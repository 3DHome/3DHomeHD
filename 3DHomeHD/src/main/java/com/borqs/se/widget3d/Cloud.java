package com.borqs.se.widget3d;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import com.borqs.se.engine.SELoadResThread;
import com.borqs.freehdhome.R;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERect3D;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEVector.SEVector2f;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SECommand;
import com.borqs.borqsweather.LunarCalendar;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.engine.SEBitmap;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SECamera.CameraChangedListener;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectFactory;
import com.borqs.se.engine.SEParticleSystem;
import com.borqs.se.engine.SESceneManager;
import com.borqs.borqsweather.weather.LoopForInfoService;
import com.borqs.borqsweather.weather.LoopForInfoService.WeatherUpdateCallBack;
import com.borqs.borqsweather.weather.WeatherConditions;
import com.borqs.borqsweather.weather.WeatherSettings;
import com.borqs.borqsweather.weather.yahoo.WeatherInfo;

public class Cloud extends NormalObject implements WeatherUpdateCallBack, CameraChangedListener {
    private SkyObject mWeather;

    private String mSkyImgKey;
    private SkyObject mThunderObject;
    private SkyObject mSunObject;
    private SkyObject mSunLayer;
    private SkyObject mSunShine;
    private SkyObject mMoon;
    private SkyObject mMoonLayer;
    private SkyObject mBlank;
    private SkyObject mFogObject;
    private List<SkyObject> mSkyObjects;
    private SkyObject mCloudLayer1;
    private SkyObject mCloudLayer2;
    private int mThunderRandomTime;
    private UpdateSkyAnimation mUpdateSkyAnimation;
    private int mCurrentType;
    private boolean mIsNight;
    private boolean mOnShow;
    private WeatherInfo mCurWeather;
    private VelocityTracker mVelocityTracker;
    private Context mContext;
    private int mWeatherImageHeight;
    private int mFontColorID;
    private LoopForInfoService mService;
    private boolean mCancelClick;
    private int mSkyAnimationCount;

    private SEParticleSystem mLargeSnowflakes;
    private SEParticleSystem mLittleSnowflakes;
    private SEParticleSystem mBigRaindrops;
    private SEParticleSystem mSmallRaindrops;
    private SEParticleSystem mStars;
    private SEParticleSystem mFlashStars;
    private SEParticleSystem mShootingStar;

    public Cloud(SEScene scene, String name, int index) {
        super(scene, name, index);
        setClickable(true);
        mCancelClick = false;
        mIsNode = true;
        mContext = getContext();
        mOnShow = false;
        mWeatherImageHeight = 256;
        mFontColorID = R.color.yellow;
        mSkyObjects = new ArrayList<SkyObject>();

    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        mSkyImgKey = "assets/base/sky/qingtianbogyun.jpg";
        float skyHeight = scene.mSceneInfo.getSkyHeightOffset();
        setTranslate(new SEVector3f(0, 0, skyHeight), true);
        getCamera().addCameraChangedListener(this);
        setOnLongClickListener(null);
        setOnClickListener(new OnTouchListener() {
            public void run(SEObject obj) {
                if (!getCamera().wasDeskSightRange()) {
                    getCamera().moveToSkySight(null);
                }
            }
        });
        setHasInit(true);
        LoopForInfoService service = SESceneManager.getInstance().getWeatherService();
        if (service != null) {
            mService = service;
            mService.setWeatherUpdateCallBack(this);
            mCurrentType = mService.getConditionType();
            mCurWeather = mService.getWeather();
            mIsNight = mService.isNight();
        } else {
            SESceneManager.getInstance().bindWeatherService();
            mCurrentType = WeatherConditions.CONDITION_TYPE_NONE;
            mCurWeather = new WeatherInfo();
            mIsNight = false;
        }
        processWeather_changed(mCurrentType, mIsNight);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (getScene().getStatus(SEScene.STATUS_ON_UNLOCK_ANIMATION)) {
            return false;
        }
        trackVelocity(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        new SECommand(getScene()) {
            public void run() {
                if (mService == null) {
                    mService = SESceneManager.getInstance().getWeatherService();
                    if (mService != null) {
                        mService.setWeatherUpdateCallBack(Cloud.this);
                    }
                } else {
                    boolean isNight = mService.isNight();
                    if (mIsNight != isNight) {
                        processWeather_changed(mCurrentType, isNight);
                    }
                }
            }
        }.execute();
    }

    private void trackVelocity(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        final VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            setPreTouch();
            stopAllAnimation(null);
            mCancelClick = false;
            break;
        case MotionEvent.ACTION_MOVE:
//            if (getCamera().wasLeftDownSight()) {
//                float skyY = (getTouchY() - getPreTouchY()) * 2f / getCamera().getHeight()
//                        + getCamera().getSightValue();
//                if (skyY < 0) {
//                    skyY = 0;
//                }
//                getCamera().changeSight(skyY, true);
//            }
            getCamera().dragSight(getTouchY() - getPreTouchY());
            setPreTouch();
            mCancelClick = true;
            break;
        case MotionEvent.ACTION_UP:
            setPreTouch();
            if (!mCancelClick) {
                performClick();
                return true;
            }

        case MotionEvent.ACTION_CANCEL:
            getCamera().onDragEnd(mVelocityTracker.getYVelocity());
//            if (getCamera().wasLeftDownSight()) {
//                if (mVelocityTracker.getYVelocity() > 200) {
//                    getCamera().moveToSkySight(null);
//                } else if (mVelocityTracker.getYVelocity() < -200) {
//                    getCamera().moveToWallSight(null);
//                } else {
//                    if (getCamera().wasSkySightRange()) {
//                        getCamera().moveToSkySight(null);
//                    } else {
//                        getCamera().moveToWallSight(null);
//                    }
//                }
//            }
            break;
        }
        return true;
    }

    private void show() {
        if (!mOnShow) {
            if (mService != null) {
                mService.checkLocationServices();
            }
            mOnShow = true;
            stopAllAnimation(null);
            if (mService == null || mService.checkWeatherInfoIsOld(mCurWeather)) {
                mCurWeather = new WeatherInfo();
                processWeather_changed(WeatherConditions.CONDITION_TYPE_NONE, mIsNight);
            } else {
                processWeather_LoadSkyObject(mCurrentType, mIsNight);
                processWeather_ShowWeatherImage();
                processWeather_CreateParticle(mCurrentType, mIsNight);
                processWeather_PlaySkyAnimation(mCurrentType, mIsNight);
            }
            if (mService != null) {
                mService.checkToUpdateWeather(1000 * 60 * 30);
            }
        }
    }

    private void hide(final SEAnimFinishListener l) {
        if (mOnShow) {
            mOnShow = false;
            stopAllAnimation(null);
            processWeather_StopSkyAnimation(mCurrentType, mIsNight);
            processWeather_Unload(mCurrentType, mIsNight);
            processWeather_DestroyParticle(mCurrentType, mIsNight);
            if (l != null) {
                l.onAnimationfinish();
            }
        }
    }

    @Override
    public boolean handleBackKey(SEAnimFinishListener l) {
        if (mOnShow) {
            getCamera().moveToWallSight(null);
        }
        return false;
    }

    private void processWeather_changed(int type, boolean isNight) {
        if (!hasInit()) {
            return;
        }
        if (HomeUtils.DEBUG)
            Log.d(HomeUtils.TAG, "####sky processWeather_changed ####");
        mSkyAnimationCount = 0;
        processWeather_UpdateSkyImage(type, isNight);
        if (mOnShow) {
            processWeather_StopSkyAnimation(mCurrentType, mIsNight);
            processWeather_Unload(mCurrentType, mIsNight);
            processWeather_DestroyParticle(mCurrentType, mIsNight);

            processWeather_LoadSkyObject(type, isNight);
            processWeather_ShowWeatherImage();
            processWeather_CreateParticle(type, isNight);
            processWeather_PlaySkyAnimation(type, isNight);
        }
        processWeather_LoadCloudObject();
        processWeather_UpdateClouds(type, isNight);
        mCurrentType = type;
        mIsNight = isNight;
    }

    private void processWeather_UpdateSkyImage(int type, boolean isNight) {
        String img = null;
        switch (type) {
        case WeatherConditions.CONDITION_TYPE_FOG:
        case WeatherConditions.CONDITION_TYPE_SNOW:
            img = "assets/base/sky/bg_fog.jpg";
            mFontColorID = R.color.blue;
            break;
        case WeatherConditions.CONDITION_TYPE_DUST:
        case WeatherConditions.CONDITION_TYPE_SLEET:
        case WeatherConditions.CONDITION_TYPE_HAIL:
        case WeatherConditions.CONDITION_TYPE_RAIN_SNOW:
        case WeatherConditions.CONDITION_TYPE_RAIN:
        case WeatherConditions.CONDITION_TYPE_OVERCAST:
        case WeatherConditions.CONDITION_TYPE_THUNDER:
            img = "assets/base/sky/dark_cloud.jpg";
            mFontColorID = R.color.yellow;
            break;
        case WeatherConditions.CONDITION_TYPE_SUN:
            if (isNight) {
                img = "assets/base/sky/bg_night_sun_cloudy.jpg";
            } else {
                img = "assets/base/sky/qingtianbogyun.jpg";
            }
            mFontColorID = R.color.yellow;
            break;
        case WeatherConditions.CONDITION_TYPE_CLOUDY:
        case WeatherConditions.CONDITION_TYPE_NONE:
            if (isNight) {
                img = "assets/base/sky/bg_night_sun_cloudy.jpg";
            } else {
                img = "assets/base/sky/qingtianbogyun.jpg";
            }
            mFontColorID = R.color.yellow;
            break;
        }
        if (!TextUtils.isEmpty(img)) {
            final String imgPath = img;
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    final int imageData = loadImageData_JNI(imgPath);
                    new SECommand(getScene()) {
                        public void run() {
                            addImageData_JNI(mSkyImgKey, imageData);
                        }
                    }.execute();

                }
            });

        }
    }

    private void processWeather_PlaySkyAnimation(int type, boolean isNight) {
        switch (type) {
        case WeatherConditions.CONDITION_TYPE_SLEET:
        case WeatherConditions.CONDITION_TYPE_DUST:
        case WeatherConditions.CONDITION_TYPE_FOG:
        case WeatherConditions.CONDITION_TYPE_HAIL:
        case WeatherConditions.CONDITION_TYPE_OVERCAST:
        case WeatherConditions.CONDITION_TYPE_RAIN_SNOW:
        case WeatherConditions.CONDITION_TYPE_CLOUDY:
        case WeatherConditions.CONDITION_TYPE_RAIN:
        case WeatherConditions.CONDITION_TYPE_SNOW:
        case WeatherConditions.CONDITION_TYPE_THUNDER:
        case WeatherConditions.CONDITION_TYPE_SUN:
            if (mUpdateSkyAnimation != null) {
                mUpdateSkyAnimation.stop();
            }
            mUpdateSkyAnimation = new UpdateSkyAnimation(getScene(), isNight);
            mUpdateSkyAnimation.execute();
            break;
        default:
            break;
        }
    }

    private void processWeather_OnAnimation(int type, boolean isNight, int count) {
        switch (type) {
        case WeatherConditions.CONDITION_TYPE_DUST:
        case WeatherConditions.CONDITION_TYPE_SLEET:
        case WeatherConditions.CONDITION_TYPE_HAIL:
        case WeatherConditions.CONDITION_TYPE_RAIN_SNOW:
        case WeatherConditions.CONDITION_TYPE_RAIN:
        case WeatherConditions.CONDITION_TYPE_SNOW:
            float step = (count % 2000) / 2000f;
            SEVector2f dir = new SEVector2f(step, 0);
            playUVAnimation(dir);
            break;
        case WeatherConditions.CONDITION_TYPE_THUNDER:
            float stepT = (count % 2000) / 2000f;
            SEVector2f dirT = new SEVector2f(stepT, 0);
            playUVAnimation(dirT);

            int frameT = (count - 1) % mThunderRandomTime;
            if (mThunderObject != null && frameT <= 25) {
                if (frameT <= 5) {
                    if (frameT == 0) {
                        mThunderObject.setVisible(true, true);
                    }
                    if (frameT == 2) {
                        mBlank.setVisible(true, true);
                    }
                    mThunderObject.setAlpha(1f - frameT * 0.2f, true);
                    if (frameT > 2) {
                        mBlank.setAlpha(1f - (float) frameT * 0.1f, true);
                    }
                } else if (frameT <= 10) {
                    mThunderObject.setAlpha((frameT - 5) * 0.2f, true);
                    mBlank.setAlpha(1f - (float) frameT * 0.1f, true);
                } else if (frameT <= 20) {
                    mThunderObject.setAlpha(1f - (frameT - 10) * 0.1f, true);
                    if (frameT == 20) {
                        mBlank.setVisible(false, true);
                        mThunderObject.setVisible(false, true);
                    }
                } else if (frameT == 25) {
                    mThunderRandomTime = getRandom(100, 350, 50);
                    final String img = "assets/base/sky/thunder" + getRandom(0, 3, 1) + ".png";
                    SELoadResThread.getInstance().process(new Runnable() {
                        public void run() {
                            final int imageData = loadImageData_JNI(img);
                            new SECommand(getScene()) {
                                public void run() {
                                    addImageData_JNI(mThunderObject.mImgKey, imageData);
                                }
                            }.execute();

                        }
                    });
                }
            }
            break;
        case WeatherConditions.CONDITION_TYPE_SUN:
            if (isNight) {
                if (mMoonLayer != null) {
                    count = mSkyAnimationCount + count;
                    float stepS = count % 50f / 83f;
                    if (stepS <= 0.3f) {
                        mMoonLayer.setAlpha(1f - stepS, true);
                    } else {
                        mMoonLayer.setAlpha(0.4f + stepS, true);
                    }
                }
            } else {
                if (mSunObject != null) {
                    int frameS = count % 360;
                    mSunLayer.rotateObject(new SERotate(0.5f, 0, 1, 0));
                    if (frameS == 0) {
                        mSunShine.setVisible(true, true);
                    } else if (frameS < 90) {
                        mSunShine.setRotate(new SERotate(-frameS, 0, 1, 0), true);
                    } else if (frameS == 90) {
                        mSunShine.setVisible(false, true);
                    }
                }
                float stepBg = ((mSkyAnimationCount + count) % 2000f) / 2000f;
                SEVector2f dirBg = new SEVector2f(stepBg, 0);
                playUVAnimation(dirBg);
            }
            playCloudUVAnimation(count);
            break;
        case WeatherConditions.CONDITION_TYPE_CLOUDY:
            count = mSkyAnimationCount + count;
            playCloudUVAnimation(count);
            if (!isNight) {
                float stepBg = (count % 2000f) / 2000f;
                SEVector2f dirBg = new SEVector2f(stepBg, 0);
                playUVAnimation(dirBg);
            }
            break;
        case WeatherConditions.CONDITION_TYPE_OVERCAST:
        case WeatherConditions.CONDITION_TYPE_FOG:// TODO
            count = mSkyAnimationCount + count;
            float stepBg = (count % 2000f) / 2000f;
            SEVector2f dirBg = new SEVector2f(stepBg, 0);
            playUVAnimation(dirBg);
            playCloudUVAnimation(count);
            break;
        default:
            break;
        }
    }

    private void processWeather_StopSkyAnimation(int type, boolean isNight) {
        if (mUpdateSkyAnimation != null) {
            mUpdateSkyAnimation.stop();
        }
        switch (type) {
        case WeatherConditions.CONDITION_TYPE_SLEET:
        case WeatherConditions.CONDITION_TYPE_DUST:
        case WeatherConditions.CONDITION_TYPE_FOG:
        case WeatherConditions.CONDITION_TYPE_HAIL:
        case WeatherConditions.CONDITION_TYPE_RAIN_SNOW:
        case WeatherConditions.CONDITION_TYPE_RAIN:
        case WeatherConditions.CONDITION_TYPE_SNOW:
            break;
        case WeatherConditions.CONDITION_TYPE_THUNDER:
            if (mThunderObject != null) {
                mThunderObject.stopUVAnimation();
            }
            break;
        case WeatherConditions.CONDITION_TYPE_SUN:
            break;
        case WeatherConditions.CONDITION_TYPE_OVERCAST:
        case WeatherConditions.CONDITION_TYPE_CLOUDY:
            break;
        default:
            break;
        }
    }

    private void processWeather_CreateParticle(int type, boolean isNight) {
        switch (type) {
        case WeatherConditions.CONDITION_TYPE_SLEET:
        case WeatherConditions.CONDITION_TYPE_RAIN:
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    final int imageData1 = loadImageData_JNI("assets/base/particle/mainrain.png");
                    final int imageData2 = loadImageData_JNI("assets/base/particle/helprain.png");
                    new SECommand(getScene()) {
                        public void run() {
                            addImageData_JNI("assets/base/particle/mainrain.png", imageData1);
                            addImageData_JNI("assets/base/particle/helprain.png", imageData2);
                            applyImage_JNI("assets/base/particle/mainrain.png", "assets/base/particle/mainrain.png");
                            applyImage_JNI("assets/base/particle/helprain.png", "assets/base/particle/helprain.png");
                        }
                    }.execute();
                }
            });
            mBigRaindrops = new SEParticleSystem("BigRaindrops");
            mBigRaindrops.setParticleSystemAttribute(new float[] { 10, 40 }, 1000);
            mBigRaindrops.setEmitterAngle(10);
            mBigRaindrops.setBoxEmitterSize(4000f, 4000f, 2000f);
            mBigRaindrops.setEmitterPosition(-1000f, 500f, 1800f);
            mBigRaindrops.setEmitterDirection(new SEVector3f(0, 0, -1));
            mBigRaindrops.setEmitterParticleVelocity(600f, 800f);
            mBigRaindrops.setEmitterTimeToLive(2, 4);
            mBigRaindrops.setEmitterEmissionRate(120);
            mBigRaindrops.setLinearForceAffectorEnable(true);
            mBigRaindrops.setForceVector(0, 0, -1);
            mBigRaindrops.setColourInterpolatorAffectorEnable(true);
            mBigRaindrops.setTimeAdjust(0, 0);
            mBigRaindrops.setColorAdjust(0, mBigRaindrops.new ColorValue(1, 1, 1, 1.0f));
            mBigRaindrops.setTimeAdjust(1, 2.5f);
            mBigRaindrops.setColorAdjust(1, mBigRaindrops.new ColorValue(1, 1, 1, 0.5f));
            mBigRaindrops.setTimeAdjust(2, 5.0f);
            mBigRaindrops.setColorAdjust(2, mBigRaindrops.new ColorValue(1, 1, 1, 0.0f));
            mBigRaindrops.setImagePath("assets/base/particle/mainrain.png");
            mBigRaindrops.addParticle_JNI();

            mSmallRaindrops = new SEParticleSystem("SmallRaindrops");
            mSmallRaindrops.setParticleSystemAttribute(new float[] { 10, 20 }, 1000);
            mSmallRaindrops.setBoxEmitterSize(4000f, 4000f, 2000f);
            mSmallRaindrops.setEmitterPosition(-1000f, 1100f, 1800f);
            mSmallRaindrops.setEmitterDirection(new SEVector3f(0.2f, 0, -1));
            mSmallRaindrops.setEmitterParticleVelocity(800f, 900f);
            mSmallRaindrops.setEmitterTimeToLive(2, 4);
            mSmallRaindrops.setEmitterEmissionRate(90);
            mSmallRaindrops.setLinearForceAffectorEnable(true);
            mSmallRaindrops.setForceVector(0, 0, -1);
            mSmallRaindrops.setImagePath("assets/base/particle/helprain.png");
            mSmallRaindrops.addParticle_JNI();
            break;
        case WeatherConditions.CONDITION_TYPE_SNOW:
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    final int imageData1 = loadImageData_JNI("assets/base/particle/mainsnow.png");
                    final int imageData2 = loadImageData_JNI("assets/base/particle/helpsnow.png");
                    new SECommand(getScene()) {
                        public void run() {
                            addImageData_JNI("assets/base/particle/mainsnow.png", imageData1);
                            addImageData_JNI("assets/base/particle/helpsnow.png", imageData2);

                            applyImage_JNI("assets/base/particle/mainsnow.png", "assets/base/particle/mainsnow.png");
                            applyImage_JNI("assets/base/particle/helpsnow.png", "assets/base/particle/helpsnow.png");
                        }
                    }.execute();
                }
            });
            mLargeSnowflakes = new SEParticleSystem("LargeSnowflakes");
            mLargeSnowflakes.setParticleSystemAttribute(new float[] { 40, 40 }, 200);
            mLargeSnowflakes.setEmitterAngle(10);
            mLargeSnowflakes.setBoxEmitterSize(4000f, 4000f, 2000f);
            mLargeSnowflakes.setEmitterPosition(-1000f, 1100f, 1800f);
            mLargeSnowflakes.setEmitterDirection(new SEVector3f(0, 0, -1));
            mLargeSnowflakes.setEmitterParticleVelocity(50, 200);
            mLargeSnowflakes.setEmitterTimeToLive(5, 10);
            mLargeSnowflakes.setEmitterEmissionRate(20);
            mLargeSnowflakes.setLinearForceAffectorEnable(true);
            mLargeSnowflakes.setForceVector(0, 0, -1);
            mLargeSnowflakes.setColourInterpolatorAffectorEnable(true);
            mLargeSnowflakes.setTimeAdjust(0, 0);
            mLargeSnowflakes.setColorAdjust(0, mLargeSnowflakes.new ColorValue(1, 1, 1, 1.0f));
            mLargeSnowflakes.setTimeAdjust(1, 2.5f);
            mLargeSnowflakes.setColorAdjust(1, mLargeSnowflakes.new ColorValue(1, 1, 1, 0.5f));
            mLargeSnowflakes.setTimeAdjust(2, 5.0f);
            mLargeSnowflakes.setColorAdjust(2, mLargeSnowflakes.new ColorValue(1, 1, 1, 0.0f));
            mLargeSnowflakes.setImagePath("assets/base/particle/mainsnow.png");
            mLargeSnowflakes.addParticle_JNI();

            mLittleSnowflakes = new SEParticleSystem("LittleSnowflakes");
            mLittleSnowflakes.setParticleSystemAttribute(new float[] { 20, 20 }, 200);
            mLittleSnowflakes.setBoxEmitterSize(4000f, 4000f, 2000f);
            mLittleSnowflakes.setEmitterPosition(-1000f, 1100f, 1800f);
            mLittleSnowflakes.setEmitterDirection(new SEVector3f(0.5f, 0, -1));
            mLittleSnowflakes.setEmitterParticleVelocity(300, 500);
            mLittleSnowflakes.setEmitterTimeToLive(5, 10);
            mLittleSnowflakes.setEmitterEmissionRate(20);
            mLittleSnowflakes.setLinearForceAffectorEnable(true);
            mLittleSnowflakes.setForceVector(0, 0, -1);
            mLittleSnowflakes.setImagePath("assets/base/particle/helpsnow.png");
            mLittleSnowflakes.addParticle_JNI();
            break;
        case WeatherConditions.CONDITION_TYPE_THUNDER:
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    final int imageData1 = loadImageData_JNI("assets/base/particle/mainrain.png");
                    final int imageData2 = loadImageData_JNI("assets/base/particle/helprain.png");
                    new SECommand(getScene()) {
                        public void run() {
                            addImageData_JNI("assets/base/particle/mainrain.png", imageData1);
                            addImageData_JNI("assets/base/particle/helprain.png", imageData2);

                            applyImage_JNI("assets/base/particle/mainrain.png", "assets/base/particle/mainrain.png");
                            applyImage_JNI("assets/base/particle/helprain.png", "assets/base/particle/helprain.png");
                        }
                    }.execute();
                }
            });
            mBigRaindrops = new SEParticleSystem("BigRaindrops");
            mBigRaindrops.setParticleSystemAttribute(new float[] { 10, 40 }, 1000);
            mBigRaindrops.setEmitterAngle(10);
            mBigRaindrops.setBoxEmitterSize(4000f, 4000f, 2000f);
            mBigRaindrops.setEmitterPosition(-1000f, 500f, 1800f);
            mBigRaindrops.setEmitterDirection(new SEVector3f(0, 0, -1));
            mBigRaindrops.setEmitterParticleVelocity(600f, 800f);
            mBigRaindrops.setEmitterTimeToLive(2, 4);
            mBigRaindrops.setEmitterEmissionRate(120);
            mBigRaindrops.setLinearForceAffectorEnable(true);
            mBigRaindrops.setForceVector(0, 0, -1);
            mBigRaindrops.setColourInterpolatorAffectorEnable(true);
            mBigRaindrops.setTimeAdjust(0, 0);
            mBigRaindrops.setColorAdjust(0, mBigRaindrops.new ColorValue(1, 1, 1, 1.0f));
            mBigRaindrops.setTimeAdjust(1, 2.5f);
            mBigRaindrops.setColorAdjust(1, mBigRaindrops.new ColorValue(1, 1, 1, 0.5f));
            mBigRaindrops.setTimeAdjust(2, 5.0f);
            mBigRaindrops.setColorAdjust(2, mBigRaindrops.new ColorValue(1, 1, 1, 0.0f));
            mBigRaindrops.setImagePath("assets/base/particle/mainrain.png");
            mBigRaindrops.addParticle_JNI();

            mSmallRaindrops = new SEParticleSystem("SmallRaindrops");
            mSmallRaindrops.setParticleSystemAttribute(new float[] { 10, 20 }, 1000);
            mSmallRaindrops.setBoxEmitterSize(4000f, 4000f, 2000f);
            mSmallRaindrops.setEmitterPosition(-1000f, 1100f, 1800f);
            mSmallRaindrops.setEmitterDirection(new SEVector3f(0.2f, 0, -1));
            mSmallRaindrops.setEmitterParticleVelocity(800f, 900f);
            mSmallRaindrops.setEmitterTimeToLive(2, 4);
            mSmallRaindrops.setEmitterEmissionRate(90);
            mSmallRaindrops.setLinearForceAffectorEnable(true);
            mSmallRaindrops.setForceVector(0, 0, -1);
            mSmallRaindrops.setImagePath("assets/base/particle/helprain.png");
            mSmallRaindrops.addParticle_JNI();
            break;
        case WeatherConditions.CONDITION_TYPE_HAIL:
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    final int imageData1 = loadImageData_JNI("assets/base/particle/mainhaily.png");
                    final int imageData2 = loadImageData_JNI("assets/base/particle/helphaily.png");
                    new SECommand(getScene()) {
                        public void run() {
                            addImageData_JNI("assets/base/particle/mainhaily.png", imageData1);
                            addImageData_JNI("assets/base/particle/helphaily.png", imageData2);

                            applyImage_JNI("assets/base/particle/mainhaily.png", "assets/base/particle/mainhaily.png");
                            applyImage_JNI("assets/base/particle/helphaily.png", "assets/base/particle/helphaily.png");

                        }
                    }.execute();
                }
            });
            mBigRaindrops = new SEParticleSystem("BigRaindrops");
            mBigRaindrops.setParticleSystemAttribute(new float[] { 10, 40 }, 1000);
            mBigRaindrops.setEmitterAngle(10);
            mBigRaindrops.setBoxEmitterSize(4000f, 4000f, 2000f);
            mBigRaindrops.setEmitterPosition(-1000f, 500f, 1800f);
            mBigRaindrops.setEmitterDirection(new SEVector3f(0, 0, -1));
            mBigRaindrops.setEmitterParticleVelocity(600f, 800f);
            mBigRaindrops.setEmitterTimeToLive(2, 4);
            mBigRaindrops.setEmitterEmissionRate(120);
            mBigRaindrops.setLinearForceAffectorEnable(true);
            mBigRaindrops.setForceVector(0, 0, -1);
            mBigRaindrops.setColourInterpolatorAffectorEnable(true);
            mBigRaindrops.setTimeAdjust(0, 0);
            mBigRaindrops.setColorAdjust(0, mBigRaindrops.new ColorValue(1, 1, 1, 1.0f));
            mBigRaindrops.setTimeAdjust(1, 2.5f);
            mBigRaindrops.setColorAdjust(1, mBigRaindrops.new ColorValue(1, 1, 1, 0.5f));
            mBigRaindrops.setTimeAdjust(2, 5.0f);
            mBigRaindrops.setColorAdjust(2, mBigRaindrops.new ColorValue(1, 1, 1, 0.0f));
            mBigRaindrops.setImagePath("assets/base/particle/mainhaily.png");
            mBigRaindrops.addParticle_JNI();

            mSmallRaindrops = new SEParticleSystem("SmallRaindrops");
            mSmallRaindrops.setParticleSystemAttribute(new float[] { 10, 20 }, 1000);
            mSmallRaindrops.setBoxEmitterSize(4000f, 4000f, 2000f);
            mSmallRaindrops.setEmitterPosition(-1000f, 1100f, 1800f);
            mSmallRaindrops.setEmitterDirection(new SEVector3f(0.2f, 0, -1));
            mSmallRaindrops.setEmitterParticleVelocity(800f, 900f);
            mSmallRaindrops.setEmitterTimeToLive(2, 4);
            mSmallRaindrops.setEmitterEmissionRate(90);
            mSmallRaindrops.setLinearForceAffectorEnable(true);
            mSmallRaindrops.setForceVector(0, 0, -1);
            mSmallRaindrops.setImagePath("assets/base/particle/helphaily.png");
            mSmallRaindrops.addParticle_JNI();
            break;
        case WeatherConditions.CONDITION_TYPE_SUN:
            if (isNight) {
                SELoadResThread.getInstance().process(new Runnable() {
                    public void run() {
                        final int imageData1 = loadImageData_JNI("assets/base/particle/mainstar.png");
                        final int imageData2 = loadImageData_JNI("assets/base/particle/helpstar.png");
                        new SECommand(getScene()) {
                            public void run() {
                                addImageData_JNI("assets/base/particle/mainstar.png", imageData1);
                                addImageData_JNI("assets/base/particle/helpstar.png", imageData2);

                                applyImage_JNI("assets/base/particle/mainstar.png", "assets/base/particle/mainstar.png");
                                applyImage_JNI("assets/base/particle/helpstar.png", "assets/base/particle/helpstar.png");
                            }
                        }.execute();
                    }
                });
                mStars = new SEParticleSystem("Stars");
                mStars.setParticleSystemAttribute(new float[] { 10, 10 }, 60);
                mStars.setBoxEmitterSize(2000, 200, 600);
                mStars.setEmitterPosition(0, 1600, 1700);
                mStars.setEmitterDirection(new SEVector3f(0, 0, -1));
                mStars.setEmitterParticleVelocity(0, 0);
                mStars.setEmitterTimeToLive(10000, 10000);
                mStars.setEmitterAngle(5);
                mStars.setEmitterEmissionRate(2000);
                mStars.setImagePath("assets/base/particle/mainstar.png");
                mStars.addParticle_JNI();

                mFlashStars = new SEParticleSystem("FlashStars");
                mFlashStars.setParticleSystemAttribute(new float[] { 15, 15 }, 20);
                mFlashStars.setBoxEmitterSize(2000, 200, 600);
                mFlashStars.setEmitterPosition(0, 1600, 1700);
                mFlashStars.setEmitterDirection(new SEVector3f(0, 0, -1));
                mFlashStars.setEmitterParticleVelocity(0, 0);
                mFlashStars.setEmitterTimeToLive(5, 10);
                mFlashStars.setEmitterAngle(5);
                mFlashStars.setEmitterEmissionRate(2000);
                mFlashStars.setEmitterColorValue(1, 1, 1, 0);
                mFlashStars.setColourInterpolatorAffectorEnable(true);
                mFlashStars.setTimeAdjust(0, 0);
                mFlashStars.setColorAdjust(0, mFlashStars.new ColorValue(1, 1, 1, 0));
                mFlashStars.setTimeAdjust(1, 0.2f);
                mFlashStars.setColorAdjust(1, mFlashStars.new ColorValue(1, 1, 1, 1));
                mFlashStars.setTimeAdjust(2, 0.4f);
                mFlashStars.setColorAdjust(2, mFlashStars.new ColorValue(1, 1, 1, 0));
                mFlashStars.setTimeAdjust(3, 0.6f);
                mFlashStars.setColorAdjust(3, mFlashStars.new ColorValue(1, 1, 1, 1));
                mFlashStars.setTimeAdjust(4, 0.8f);
                mFlashStars.setColorAdjust(4, mFlashStars.new ColorValue(1, 1, 1, 0));
                mFlashStars.setTimeAdjust(5, 1.0f);
                mFlashStars.setColorAdjust(5, mFlashStars.new ColorValue(1, 1, 1, 1));
                mFlashStars.setNeedDepthTest(true);
                mFlashStars.setLayerIndex(0);
                mFlashStars.setImagePath("assets/base/particle/mainstar.png");
                mFlashStars.addParticle_JNI();

                mShootingStar = new SEParticleSystem("ShootingStar");
                mShootingStar.setParticleSystemAttribute(new float[] { 100, 100 }, 1);
                mShootingStar.setBoxEmitterSize(700, 200, 500);
                mShootingStar.setEmitterPosition(700, 1250, 1600);
                mShootingStar.setEmitterDirection(new SEVector3f(-1, 1, 0.1f));
                mShootingStar.setEmitterParticleVelocity(300, 500);
                mShootingStar.setEmitterTimeToLive(2, 2);
                mShootingStar.setEmitterAngle(10);
                mShootingStar.setEmitterEmissionRate(2);
                mShootingStar.setColourFaderAffectorEnable(true);
                mShootingStar.setColorFaderAdjust(-0.0f, -0.0f, -0.0f, -0.5f);
                mShootingStar.setNeedDepthTest(true);
                mShootingStar.setLayerIndex(1);
                mShootingStar.setImagePath("assets/base/particle/helpstar.png");
                mShootingStar.addParticle_JNI();
            }
            break;
        case WeatherConditions.CONDITION_TYPE_RAIN_SNOW:
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    final int imageData1 = loadImageData_JNI("assets/base/particle/mainrain.png");
                    final int imageData2 = loadImageData_JNI("assets/base/particle/helpsnow.png");
                    new SECommand(getScene()) {
                        public void run() {
                            addImageData_JNI("assets/base/particle/mainrain.png", imageData1);
                            addImageData_JNI("assets/base/particle/helpsnow.png", imageData2);

                            applyImage_JNI("assets/base/particle/mainrain.png", "assets/base/particle/mainrain.png");
                            applyImage_JNI("assets/base/particle/helpsnow.png", "assets/base/particle/helpsnow.png");
                        }
                    }.execute();
                }
            });
            mBigRaindrops = new SEParticleSystem("BigRaindrops");
            mBigRaindrops.setParticleSystemAttribute(new float[] { 10, 40 }, 1000);
            mBigRaindrops.setEmitterAngle(10);
            mBigRaindrops.setBoxEmitterSize(4000f, 4000f, 2000f);
            mBigRaindrops.setEmitterPosition(-1000f, 500f, 1800f);
            mBigRaindrops.setEmitterDirection(new SEVector3f(0, 0, -1));
            mBigRaindrops.setEmitterParticleVelocity(600f, 800f);
            mBigRaindrops.setEmitterTimeToLive(2, 4);
            mBigRaindrops.setEmitterEmissionRate(120);
            mBigRaindrops.setLinearForceAffectorEnable(true);
            mBigRaindrops.setForceVector(0, 0, -1);
            mBigRaindrops.setColourInterpolatorAffectorEnable(true);
            mBigRaindrops.setTimeAdjust(0, 0);
            mBigRaindrops.setColorAdjust(0, mBigRaindrops.new ColorValue(1, 1, 1, 1.0f));
            mBigRaindrops.setTimeAdjust(1, 2.5f);
            mBigRaindrops.setColorAdjust(1, mBigRaindrops.new ColorValue(1, 1, 1, 0.5f));
            mBigRaindrops.setTimeAdjust(2, 5.0f);
            mBigRaindrops.setColorAdjust(2, mBigRaindrops.new ColorValue(1, 1, 1, 0.0f));
            mBigRaindrops.setImagePath("assets/base/particle/mainrain.png");
            mBigRaindrops.addParticle_JNI();

            mLittleSnowflakes = new SEParticleSystem("LittleSnowflakes");
            mLittleSnowflakes.setParticleSystemAttribute(new float[] { 20, 20 }, 200);
            mLittleSnowflakes.setBoxEmitterSize(4000f, 4000f, 2000f);
            mLittleSnowflakes.setEmitterPosition(-1000f, 1100f, 1800f);
            mLittleSnowflakes.setEmitterDirection(new SEVector3f(0.5f, 0, -1));
            mLittleSnowflakes.setEmitterParticleVelocity(300, 500);
            mLittleSnowflakes.setEmitterTimeToLive(5, 10);
            mLittleSnowflakes.setEmitterEmissionRate(20);
            mLittleSnowflakes.setLinearForceAffectorEnable(true);
            mLittleSnowflakes.setForceVector(0, 0, -1);
            mLittleSnowflakes.setImagePath("assets/base/particle/helpsnow.png");
            mLittleSnowflakes.addParticle_JNI();
            break;
        case WeatherConditions.CONDITION_TYPE_DUST:
        case WeatherConditions.CONDITION_TYPE_FOG:
        case WeatherConditions.CONDITION_TYPE_CLOUDY:
        case WeatherConditions.CONDITION_TYPE_OVERCAST:
            break;
        default:
            break;
        }
    }

    private void processWeather_DestroyParticle(int type, boolean isNight) {
        if (mLargeSnowflakes != null) {
            mLargeSnowflakes.deleteParticleObject_JNI();
            mLargeSnowflakes = null;
        }
        if (mLittleSnowflakes != null) {
            mLittleSnowflakes.deleteParticleObject_JNI();
            mLittleSnowflakes = null;
        }
        if (mBigRaindrops != null) {
            mBigRaindrops.deleteParticleObject_JNI();
            mBigRaindrops = null;
        }
        if (mSmallRaindrops != null) {
            mSmallRaindrops.deleteParticleObject_JNI();
            mSmallRaindrops = null;
        }
        if (mStars != null) {
            mStars.deleteParticleObject_JNI();
            mStars = null;
        }
        if (mFlashStars != null) {
            mFlashStars.deleteParticleObject_JNI();
            mFlashStars = null;
        }
        if (mShootingStar != null) {
            mShootingStar.deleteParticleObject_JNI();
            mShootingStar = null;
        }
    }

    private void processWeather_LoadSkyObject(int type, boolean isNight) {

        switch (type) {
        case WeatherConditions.CONDITION_TYPE_SUN:
            if (isNight) {

                int lunarDay = (int)LunarCalendar.today()[2];

                if (lunarDay > 1 && lunarDay < 30) {
                    if (lunarDay > 3 && lunarDay < 28) {
                        mMoonLayer = new SkyObject(getScene(), "moon_layer");
                        mMoonLayer.createMoon("assets/base/sky/moon_layer.png", SkyObject.LAYER_INDEX_0, lunarDay);
                        getScene().getContentObject().addChild(mMoonLayer, true);
                        mSkyObjects.add(mMoonLayer);
                    }
                    mMoon = new SkyObject(getScene(), "moon");
                    mMoon.createMoon("assets/base/sky/moon" + lunarDay +".png", SkyObject.LAYER_INDEX_1,lunarDay);
                    getScene().getContentObject().addChild(mMoon, true);
                    mSkyObjects.add(mMoon);
                }
            } else {
                mSunShine = new SkyObject(getScene(), "sun_shine");
                mSunShine.createSun("assets/base/sky/sun_shine.png", SkyObject.LAYER_INDEX_0);
                getScene().getContentObject().addChild(mSunShine, true);
                mSkyObjects.add(mSunShine);
                mSunLayer = new SkyObject(getScene(), "sun_layer");
                mSunLayer.createSun("assets/base/sky/sun_layer.png", SkyObject.LAYER_INDEX_1);
                getScene().getContentObject().addChild(mSunLayer, true);
                mSkyObjects.add(mSunLayer);
                mSunObject = new SkyObject(getScene(), "sun");
                mSunObject.createSun("assets/base/sky/sun.png", SkyObject.LAYER_INDEX_2);
                getScene().getContentObject().addChild(mSunObject, true);
                mSkyObjects.add(mSunObject);
            }
            break;
        case WeatherConditions.CONDITION_TYPE_THUNDER:
            mThunderRandomTime = 100;
            mThunderObject = new SkyObject(getScene(), "thunder");
            mThunderObject.createThunder(SkyObject.LAYER_INDEX_0);
            mSkyObjects.add(mThunderObject);
            getScene().getContentObject().addChild(mThunderObject, true);
            mBlank = new SkyObject(getScene(), "thunder_blank");
            mBlank.createFlash("assets/base/sky/blank.png", 6);
            getScene().getContentObject().addChild(mBlank, true);
            mSkyObjects.add(mBlank);
            break;
        case WeatherConditions.CONDITION_TYPE_DUST:
        case WeatherConditions.CONDITION_TYPE_SLEET:
        case WeatherConditions.CONDITION_TYPE_RAIN:
        case WeatherConditions.CONDITION_TYPE_SNOW:
        case WeatherConditions.CONDITION_TYPE_RAIN_SNOW:
        case WeatherConditions.CONDITION_TYPE_HAIL:
            break;
        case WeatherConditions.CONDITION_TYPE_FOG:
            mFogObject = new SkyObject(getScene(), "fog");
            mFogObject.createFog("assets/base/sky/fog.png", 0);
            getScene().getContentObject().addChild(mFogObject, true);
            mSkyObjects.add(mFogObject);
            break;
        case WeatherConditions.CONDITION_TYPE_OVERCAST:
            break;
        case WeatherConditions.CONDITION_TYPE_CLOUDY:
            if (isNight) {
                int lunarDay = (int)LunarCalendar.today()[2];
                if (lunarDay > 1 && lunarDay < 30) {
                    mMoon = new SkyObject(getScene(), "moon");
                    mMoon.createMoon("assets/base/sky/moon" + lunarDay +".png", SkyObject.LAYER_INDEX_0, lunarDay);
                    mMoon.setAlpha(0.2f, false);
                    getScene().getContentObject().addChild(mMoon, true);
                    mSkyObjects.add(mMoon);
                }
            }
            break;
        default:
            break;
        }
    }

    private void processWeather_UpdateClouds(int weatherType, boolean isNight) {
        mCloudLayer1.setVisible(false, true);
        mCloudLayer2.setVisible(false, true);
        switch (weatherType) {
        case WeatherConditions.CONDITION_TYPE_SUN:
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    final int imageData1 = loadImageData_JNI("assets/base/sky/fog.png");
                    new SECommand(getScene()) {
                        public void run() {
                            addImageData_JNI(mCloudLayer1.mImgKey, imageData1);
                            mCloudLayer1.setVisible(true, true);
                        }
                    }.execute();
                }
            });
            break;
        case WeatherConditions.CONDITION_TYPE_THUNDER:
        case WeatherConditions.CONDITION_TYPE_DUST:
        case WeatherConditions.CONDITION_TYPE_SLEET:
        case WeatherConditions.CONDITION_TYPE_RAIN:
        case WeatherConditions.CONDITION_TYPE_SNOW:
        case WeatherConditions.CONDITION_TYPE_RAIN_SNOW:
        case WeatherConditions.CONDITION_TYPE_HAIL:
            break;
        case WeatherConditions.CONDITION_TYPE_FOG:
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    final int imageData1 = loadImageData_JNI("assets/base/sky/cloud_dn_small_overcast.png");
                    new SECommand(getScene()) {
                        public void run() {
                            addImageData_JNI(mCloudLayer1.mImgKey, imageData1);
                            mCloudLayer1.setVisible(true, true);
                        }
                    }.execute();
                }
            });
            break;
        case WeatherConditions.CONDITION_TYPE_OVERCAST:
            if (isNight) {
                SELoadResThread.getInstance().process(new Runnable() {
                    public void run() {
                        final int imageData1 = loadImageData_JNI("assets/base/sky/cloud_dn_small_overcast.png");
                        new SECommand(getScene()) {
                            public void run() {
                                addImageData_JNI(mCloudLayer1.mImgKey, imageData1);
                                mCloudLayer1.setVisible(true, true);
                            }
                        }.execute();
                    }
                });
            } else {
                SELoadResThread.getInstance().process(new Runnable() {
                    public void run() {
                        final int imageData1 = loadImageData_JNI("assets/base/sky/cloud_dn_small_overcast.png");
                        final int imageData2 = loadImageData_JNI("assets/base/sky/cloud_day_large_overcast.png");
                        new SECommand(getScene()) {
                            public void run() {
                                addImageData_JNI(mCloudLayer1.mImgKey, imageData1);
                                addImageData_JNI(mCloudLayer2.mImgKey, imageData2);
                                mCloudLayer1.setVisible(true, true);
                                mCloudLayer2.setVisible(true, true);
                            }
                        }.execute();
                    }
                });
            }
            break;
        case WeatherConditions.CONDITION_TYPE_CLOUDY:
            if (isNight) {
                SELoadResThread.getInstance().process(new Runnable() {
                    public void run() {
                        final int imageData1 = loadImageData_JNI("assets/base/sky/cloud_night_large_cloudy.png");
                        final int imageData2 = loadImageData_JNI("assets/base/sky/fog.png");
                        new SECommand(getScene()) {
                            public void run() {
                                addImageData_JNI(mCloudLayer1.mImgKey, imageData1);
                                addImageData_JNI(mCloudLayer1.mImgKey, imageData2);
                                mCloudLayer1.setVisible(true, true);
                                mCloudLayer2.setVisible(true, true);
                            }
                        }.execute();
                    }
                });
            } else {
                SELoadResThread.getInstance().process(new Runnable() {
                    public void run() {
                        final int imageData1 = loadImageData_JNI("assets/base/sky/fog.png");
                        final int imageData2 = loadImageData_JNI("assets/base/sky/cloud_day_large_cloudy.png");
                        new SECommand(getScene()) {
                            public void run() {
                                addImageData_JNI(mCloudLayer1.mImgKey, imageData1);
                                addImageData_JNI(mCloudLayer2.mImgKey, imageData2);
                                mCloudLayer1.setVisible(true, true);
                                mCloudLayer2.setVisible(true, true);
                            }
                        }.execute();
                    }
                });
            }
            break;
        default:
            break;
        }
    }

    private void processWeather_LoadCloudObject() {
        if (mCloudLayer1 == null) {
            mCloudLayer1 = new SkyObject(getScene(), "cloudy_layer1");
            mCloudLayer1.createCloudy("assets/base/sky/fog.png", SkyObject.LAYER_INDEX_3);
            getScene().getContentObject().addChild(mCloudLayer1, true);
        }

        if (mCloudLayer2 == null) {
            mCloudLayer2 = new SkyObject(getScene(), "cloudy_layer2");
            mCloudLayer2.createCloudy("assets/base/sky/fog.png", SkyObject.LAYER_INDEX_4);
            getScene().getContentObject().addChild(mCloudLayer2, true);
        }
    }

    private void processWeather_ShowWeatherImage() {
        mWeather = new SkyObject(getScene(), "show_weather_image");
        mWeather.createWeahter(10);
        getScene().getContentObject().addChild(mWeather, true);
        mSkyObjects.add(mWeather);
    }

    private void processWeather_Unload(int type, boolean isNight) {
        if (mSkyObjects != null) {
            for (SkyObject obj : mSkyObjects) {
                getScene().getContentObject().removeChild(obj, true);
            }
            mSkyObjects.clear();
        }
    }

    public void onWeatherUpdate(final boolean forceUpdate) {
        final boolean isNight = mService.isNight();
        new SECommand(getScene()) {
            @Override
            public void run() {
                if (mService != null) {
                    WeatherInfo weather = mService.getWeather();
                    int type = mService.getConditionType();
                    if (mCurrentType != type || !weather.equals(mCurWeather) || forceUpdate || isNight != mIsNight) {
                        mCurWeather = weather;
                        processWeather_changed(type, isNight);
                    }
                }
            }
        }.execute();
    }

    private class UpdateSkyAnimation extends CountAnimation {
        private boolean mNight;
        private int mCount;

        public UpdateSkyAnimation(SEScene scene, boolean isNight) {
            super(scene);
            mNight = isNight;
            mCount = 0;
        }

        public void runPatch(int count) {
            processWeather_OnAnimation(mCurrentType, mNight, count);
            mCount = count;
        }

        public void onFinish() {
            super.onFinish();
            mSkyAnimationCount = mSkyAnimationCount + mCount;
        }

    }

    private void playCloudUVAnimation(int count) {
        float step = ((float) count % 1000f) / 800f;
        if (mCloudLayer1 != null && mCloudLayer1.isVisible()) {
            SEVector2f dirCloud = new SEVector2f(step, 0);
            mCloudLayer1.playUVAnimation(dirCloud);
        }
        if (mCloudLayer2 != null && mCloudLayer1.isVisible()) {
            step = ((float) count % 1500f) / 1300f;
            SEVector2f dirCloud = new SEVector2f(-step, 0);
            mCloudLayer2.playUVAnimation(dirCloud);
        }
    }

    private int getRandom(int min, int max, int step) {
        int count = (max - min) / step;
        double x = Math.random() * count;
        return ((int) (x) * step + min);
    }

    private enum ObjectType {
        WEATHER, CLOUDY, SUN, MOON, THUNDER, BLANK, FOG
    }

    private class SkyObject extends SEObject {

        protected static final int LAYER_INDEX_0 = 0;
        protected static final int LAYER_INDEX_1 = 1;
        protected static final int LAYER_INDEX_2 = 2;
        protected static final int LAYER_INDEX_3 = 3;
        protected static final int LAYER_INDEX_4 = 4;

        protected double mLayerAngle;
        protected ObjectType mObjectType;
        protected String mImgKey;
        protected float mSkyHeight;
        protected float mWallRadius;
        protected SEVector3f mMoveDirection;

        public SkyObject(SEScene scene, String name) {
            super(scene, name);
            mLayerAngle = SECamera.MAX_SIGHT_ANGLE;
            mSkyHeight = scene.mSceneInfo.getSkyHeightOffset();
            mWallRadius = scene.mSceneInfo.mHouseSceneInfo.getHouseRadius();
            mMoveDirection = new SEVector3f(0, (float) -Math.sin(mLayerAngle), (float) Math.cos(mLayerAngle));
        }

        @Override
        public void onRenderFinish(SECamera camera) {
            super.onRenderFinish(camera);
            setTouchable_JNI(false);
            mImgKey = getImageName_JNI();

            if (mSkyObjects != null) {
                for (SkyObject obj : mSkyObjects) {
                    obj.setBlendSortAxis(AXIS.Y);
                }
            }
        }

        @Override
        public Bitmap onStartLoadImage() {
            switch (mObjectType) {
            case WEATHER:
                break;
            case SUN:
                break;
            case MOON:
                break;
            case THUNDER:
                break;
            case BLANK:
                break;
            case FOG:
            }
            return null;
        }

        public void onCameraChanged() {
            switch (mObjectType) {
            case WEATHER:
                SEVector3f cameraYAxis = getCamera().getAxisY();
                SEVector3f weatherYTrans = cameraYAxis.mul(
                        (SESceneManager.getInstance().getHeight() - mWeatherImageHeight) / 2).selfMul(0.15f);
                SEVector2f yAxis2f = new SEVector2f(cameraYAxis.getZ(), cameraYAxis.getY());
                float angle = (float) (180 * yAxis2f.getAngle() / Math.PI);
                getUserTransParas().mTranslate = getCamera().getScreenLocation(0.15f);
                getUserTransParas().mTranslate.selfAdd(weatherYTrans);
                getUserTransParas().mRotate = new SERotate(-angle, 1, 0, 0);
                setUserTransParas();
                break;
            case SUN:
                break;
            case MOON:
                break;
            case THUNDER:
                break;
            case BLANK:
            case FOG:
                SEVector3f cameraYAxis2 = getCamera().getAxisY();
                SEVector2f yAxis2f2 = new SEVector2f(cameraYAxis2.getZ(), cameraYAxis2.getY());
                float angle2 = (float) (180 * yAxis2f2.getAngle() / Math.PI);
                SEVector3f loc = getCamera().getScreenLocation(0.2f);
                getUserTransParas().mTranslate = loc;
                loc.mD[1] = Math.max(loc.mD[1], mWallRadius);
                loc.mD[2] = Math.max(loc.mD[2], mSkyHeight);
                getUserTransParas().mRotate = new SERotate(-angle2, 1, 0, 0);
                setUserTransParas();
                break;
            }
        }

        public void createWeahter(int layerIndex) {
            mObjectType = ObjectType.WEATHER;
            float scale = 0.15f;
            int w = getCamera().getWidth() >= 720 ? 1024 : 512;
            Bitmap bitmap = getWeatherBitmap(w);
            SERect3D rect = new SERect3D();
            rect.setSize(w, mWeatherImageHeight, scale);
            String imageName = mName + "_imageName";
            String imageKey = mName + "_imageKey";
            SEObjectFactory.createOpaqueRectangle(this, rect, imageName, imageKey, new SEBitmap(bitmap,
                    SEBitmap.Type.normal));
            setImageSize(w, mWeatherImageHeight);
            setBlendingable(true, false);
            setNeedForeverBlend(true);
            setLayerIndex(layerIndex, false);
            onCameraChanged();
        }

        public void createSun(String imagePath, int layerIndex) {
            mObjectType = ObjectType.SUN;
            SERect3D rect = new SERect3D();
            rect.setSize(-65f, -87f, 512, 512, 2);
            SEObjectFactory.createRectangle(this, rect, imagePath, imagePath);

            float timeValue = getTimeValue();
            getLocalTransParas().mTranslate = new SEVector3f(timeValue * 500, mWallRadius, mSkyHeight);
            getLocalTransParas().mRotate = new SERotate((float) (mLayerAngle * 180 / Math.PI), 1, 0, 0);
            getLocalTransParas().mTranslate.selfAdd(getDistance(layerIndex));
            // set sun to right position
            getLocalTransParas().mTranslate.selfAdd(mMoveDirection.mul(900 - Math.abs(timeValue) * 250));
            setLayerIndex(layerIndex, false);
        }

        public void createMoon(String imagePath, int layerIndex, int lunarDay) {
            mObjectType = ObjectType.MOON;
            SERect3D rect = new SERect3D();
            rect.setSize(512,512,2);
            SEObjectFactory.createRectangle(this, rect, imagePath, imagePath);
            getLocalTransParas().mTranslate = new SEVector3f(30f * ( lunarDay - 15), mWallRadius, mSkyHeight);
            getLocalTransParas().mRotate = new SERotate((float) (mLayerAngle * 180 / Math.PI), 1, 0, 0);
            getLocalTransParas().mTranslate.selfAdd(getDistance(layerIndex));
            // set moon to right position
            getLocalTransParas().mTranslate.selfAdd(mMoveDirection.mul(900));

            setLayerIndex(layerIndex, false);
        }

        public void createThunder(int layerIndex) {
            mObjectType = ObjectType.THUNDER;
            SERect3D rect = new SERect3D();
            rect.setSize(512, 512, 3);
            String imagePath = "assets/base/sky/thunder" + getRandom(0, 3, 1) + ".png";
            SEObjectFactory.createRectangle(this, rect, imagePath, imagePath);
            getLocalTransParas().mTranslate = new SEVector3f(0, mWallRadius, mSkyHeight);
            getLocalTransParas().mRotate = new SERotate((float) (mLayerAngle * 180 / Math.PI), 1, 0, 0);
            getLocalTransParas().mTranslate.selfAdd(getDistance(layerIndex));
            getLocalTransParas().mTranslate.selfAdd(mMoveDirection.mul(768));
            setLayerIndex(layerIndex, false);
        }

        public void createCloudy(String imagePath, int layerIndex) {
            mObjectType = ObjectType.CLOUDY;
            SERect3D rect = new SERect3D();
            rect.setSize(712, 512, 4);
            SEObjectFactory.createRectangle(this, rect, imagePath, imagePath);
            getLocalTransParas().mTranslate = new SEVector3f(0, mWallRadius, mSkyHeight);
            getLocalTransParas().mRotate = new SERotate((float) (mLayerAngle * 180 / Math.PI), 1, 0, 0);
            getLocalTransParas().mTranslate.selfAdd(getDistance(layerIndex));
            getLocalTransParas().mTranslate.selfAdd(mMoveDirection.mul(1024));
            setBlendingable(true, false);
            setNeedForeverBlend(true);
            setLayerIndex(layerIndex, false);
            setVisible(false, false);
        }

        public void createFlash(String imagePath, int layerIndex) {
            mObjectType = ObjectType.BLANK;
            float scale = 0.2f;
            int w = getCamera().getWidth();
            int h = getCamera().getHeight();
            SERect3D rect = new SERect3D();
            rect.setSize(w, h, scale);
            SEObjectFactory.createRectangle(this, rect, imagePath, imagePath);
            setBlendingable(true, false);
            setLayerIndex(layerIndex, false);
            onCameraChanged();
        }

        public void createFog(String imagePath, int layerIndex) {
            mObjectType = ObjectType.FOG;
            float scale = 0.2f;
            int w = getCamera().getWidth();
            int h = getCamera().getHeight();
            SERect3D rect = new SERect3D();
            rect.setSize(w, h, scale);
            SEObjectFactory.createRectangle(this, rect, imagePath, imagePath);
            setBlendingable(true, false);
            setLayerIndex(layerIndex, false);
            onCameraChanged();
        }

        private Bitmap getWeatherBitmap(int w) {
            int bitmapW = w > getCamera().getWidth() ? getCamera().getWidth() : w;
            boolean isNoInfo = mCurWeather.isNoData();
            boolean isCelsius = WeatherSettings.isCelsius(mContext);
            int symbolTempResId = isCelsius ? R.string.temp_symbol_c : R.string.temp_symbol_f;
            String symbolTemp = mContext.getString(symbolTempResId);
            int tempType = isCelsius ? WeatherInfo.TEMPERATURE_FMT_CELSIUS : WeatherInfo.TEMPERATURE_FMT_FAHRENHEIT;
            String cTemp;
            String low = "";
            String hight = "";
            String condition = "";
            long curTime = System.currentTimeMillis();
            String date = DateFormat.getDateFormat(mContext).format(curTime);// with settings config.
//            String date = DateFormat.format("MM-dd-yyyy", curTime).toString();
            String city = "";
            String symbolTo = mContext.getString(R.string.symbol_to);
            if (isNoInfo) {
                cTemp = mContext.getString(R.string.no_info);
                if (mService != null) {
                    city = mService.getCity();
                }
            } else {
                cTemp = mCurWeather.getTemperature(tempType) + symbolTemp;
                low = mCurWeather.getTempLow(tempType) + symbolTemp;
                hight = mCurWeather.getTempHigh(tempType) + symbolTemp;
                condition = mService.getDisplayCondition();
                city = mCurWeather.getCity();
            }
            StringBuilder tempRange = new StringBuilder();
            tempRange.append(low);
            tempRange.append(" ");
            tempRange.append(symbolTo);
            tempRange.append(" ");
            tempRange.append(hight);

            float pixel = SESceneManager.getInstance().getPixelDensity();

            float padding = 5 * pixel;

            TextPaint tpLarge = new TextPaint();
            tpLarge.setAntiAlias(true);
            tpLarge.setFakeBoldText(true);
            tpLarge.setTextSize(32 * pixel);
            tpLarge.setColor(mContext.getResources().getColor(mFontColorID));

            Layout.Alignment tempAlign = Alignment.ALIGN_CENTER;
            if (w < getCamera().getWidth()) {
                tempAlign = Alignment.ALIGN_NORMAL;
            }
            StaticLayout tempLayout = new StaticLayout(cTemp, tpLarge, bitmapW / 4, tempAlign, 1.1f, 0.0f, true);

            TextPaint tpNormal = new TextPaint();
            tpNormal.setAntiAlias(true);
            tpNormal.setStyle(Style.FILL_AND_STROKE);
            tpNormal.setStrokeWidth(0.5f);
            tpNormal.setStrokeCap(Paint.Cap.ROUND);
            tpNormal.setTextSize(18 * pixel);
            tpNormal.setColor(mContext.getResources().getColor(mFontColorID));

            Bitmap des;
            if (isNoInfo) {

                StaticLayout promptLayout = new StaticLayout(mContext.getString(R.string.no_info_msg), tpNormal,
                        bitmapW * 3 / 4, Alignment.ALIGN_NORMAL, 1.1f, 0.0f, true);
                StaticLayout dateLayout = new StaticLayout(date, tpNormal, bitmapW * 3 / 8, Alignment.ALIGN_NORMAL,
                        1.1f, 0.0f, true);
                StaticLayout cityLayout = null;
                if (!TextUtils.isEmpty(city)) {
                    cityLayout = new StaticLayout(city, tpNormal, bitmapW * 3 / 8, Alignment.ALIGN_NORMAL, 1.1f, 0.0f,
                            true);
                }

                des = Bitmap.createBitmap(w, mWeatherImageHeight, Bitmap.Config.ARGB_4444);
                Canvas canvas = new Canvas(des);
                float transX = padding;
                if (w > bitmapW) {
                    transX = (w - bitmapW) / 2.0f + padding;
                }
                canvas.translate(transX, padding * 1.5f);
                tempLayout.draw(canvas);
                canvas.translate(tempLayout.getWidth() + padding, 0);
                promptLayout.draw(canvas);
                canvas.translate(0, promptLayout.getHeight() + padding);
                dateLayout.draw(canvas);
                if (!TextUtils.isEmpty(city) && cityLayout != null) {
                    canvas.translate(dateLayout.getWidth(), 0);
                    cityLayout.draw(canvas);
                }

            } else {
                int index = tpNormal.breakText(city, true, bitmapW / 2, null);
                if (index != city.length()) {
                    city = city.substring(0, index - 1) + ".";
                }
                StaticLayout cityLayout = new StaticLayout(city, tpNormal, bitmapW * 1 / 4, Alignment.ALIGN_NORMAL,
                        1.1f, 0.0f, true);
                StaticLayout conditionLayout = new StaticLayout(condition, tpNormal, bitmapW * 3 / 8,
                        Alignment.ALIGN_CENTER, 1.1f, 0.0f, true);
                StaticLayout tempRangeLayout = new StaticLayout(tempRange, tpNormal, bitmapW * 3 / 8,
                        Alignment.ALIGN_NORMAL, 1.1f, 0.0f, true);
                StaticLayout dateLayout = new StaticLayout(date, tpNormal, bitmapW * 3 / 8, Alignment.ALIGN_NORMAL,
                        1.1f, 0.0f, true);

                des = Bitmap.createBitmap(w, mWeatherImageHeight, Bitmap.Config.ARGB_4444);
                Canvas canvas = new Canvas(des);
                float transX = padding;
                if (w > bitmapW) {
                    transX = (w - bitmapW) / 2.0f + padding;
                }
                canvas.translate(transX, padding * 2.0f);
                tempLayout.draw(canvas);
                canvas.translate(tempLayout.getWidth() + padding, 0);
                cityLayout.draw(canvas);
                canvas.translate(cityLayout.getWidth(), 0);
                conditionLayout.draw(canvas);
                canvas.translate(-cityLayout.getWidth(),
                        (cityLayout.getHeight() > conditionLayout.getHeight() ? cityLayout.getHeight()
                                : conditionLayout.getHeight()) + padding);
                dateLayout.draw(canvas);
                canvas.translate(dateLayout.getWidth(), 0);
                tempRangeLayout.draw(canvas);
            }
            return des;
        }

        protected SEVector3f getDistance(int layerIndex) {
            SEVector3f moveDirection = new SEVector3f(0, (float) Math.cos(mLayerAngle), (float) Math.sin(mLayerAngle));
            if (layerIndex < 3) {
                moveDirection.selfMul(100 + (2 - layerIndex) * 10);
            } else {
                moveDirection.selfMul(50 + (5 - layerIndex) * 10);
            }
            return moveDirection;
        }

        private float getTimeValue() {
            Time time = new Time();
            time.setToNow();
            int hour = time.hour;
            if (hour < 12) {
                return (12f - hour) / 4;
            } else {
                return (12f - hour) / 6;
            }
        }
    }

    public void onCameraChanged() {
        if (getCamera().wasSkySightRange()) {
            show();
        } else {
            hide(null);
        }
        if (mOnShow) {
            if (mSkyObjects != null) {
                for (SkyObject obj : mSkyObjects) {
                    obj.onCameraChanged();
                }
            }
        }
    }

    @Override
    public void onRelease() {
        super.onRelease();
        getCamera().removeCameraChangedListener(this);
    }

}
