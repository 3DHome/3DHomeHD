package com.borqs.se.widget3d;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.MotionEvent;

import com.borqs.freehdhome.R;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SECommand;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.engine.SEObject;
//import com.borqs.se.engine.SEObject.PRESS_TYPE;
import com.borqs.se.engine.SESceneManager;

public abstract class Flyer extends NormalObject {
    private static final String TAG = "Flyer";
    private MoveAnimation mShowAnimation;
    private SESceneManager mSESceneManager;
    private String mBannerImageKey;
    private SEObject mBanner;
    private ADViewController mController;
    private boolean mADOnShow = false;

    private String getRecommendationPackageName() {
//        return "com.borqs.richmessage";
//        return "com.borqs.selockscreen";
        return HomeUtils.LOCKSCREEN_HOMEHD_PKG;
    }

    public Flyer(SEScene scene, String name, int index) {
        super(scene, name, index);
        mSESceneManager = SESceneManager.getInstance();
        mController = ADViewController.getInstance();
        setClickable(true);
        setIsAlphaPress(false);
    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
//        setOnLongClickListener(new OnTouchListener() {
//            public void run(SEObject obj) {
//                getScene().handleMessage(SE3DHomeScene.MSG_TYPE_SHOW_EDIT_SCENE_DIALOG, null);
//            }
//        });
        setOnLongClickListener(null);

        setIsEntirety_JNI(true);
        setVisible(false, true);

        setOnClickListener(new OnTouchListener() {
            public void run(SEObject obj) {
                if (mController.isRemoveAD()) {
                    // todo: show option to customize ad
                    return;
                }
                
                if (!mADOnShow) {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://search?q=pname:" + getRecommendationPackageName()));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    mSESceneManager.startActivity(intent);
                    return;
                }

                boolean handled = false;

                AnimationInfo info = mShowAnimation.getAnimationInfo();
                SEVector3f vector = getSelectPoint(getTouchX(), getTouchY());
                if (info != null && vector != null) {
                    float x = vector.getX();
                    float z = vector.getZ();
//                    Log.d(TAG, "clicking x = " + x + ", z = " + z);
                    if (info.mReversed) {
                        if ((x < 440f && x > 360f) && (z < 100f && z > 20f)) {
                            handled = ADViewController.getInstance().onUpgradeAppIntent();
                        }
                    } else {
                        if ((x < -60f && x > -140f) && (z < 100f && z > 20f)) {
                            handled = ADViewController.getInstance().onUpgradeAppIntent();
                        }
                    }
                }
                if (!handled) {
                    mController.doClick();
                }
            }
        });
    }

    @Override
    public void onActivityPause() {
        mController.stopCatchImage();
    }

    @Override
    public void onActivityResume() {
        if (mShowAnimation == null || mShowAnimation.isFinish() || getCamera().getSightValue() != 1) {
            return;
        }
        mController.requestCatchImage(500);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (MotionEvent.ACTION_MOVE == event.getAction()) {
            if (checkOutside((int) event.getX(), (int) event.getY())) {
                setPressed(false);
            }
        }
        return super.onTouchEvent(event);
    }

    private void onStartAnimation() {
//        LoopForInfoService service = SESceneManager.getInstance().getWeatherService();
//        if (service != null) {
//            int currentType = service.getConditionType();
//            if (currentType == WeatherConditions.CONDITION_TYPE_FOG) {
//                setNeedFog_JNI(true);
//            }
//        }
        mSESceneManager.runInUIThread(new Runnable() {
            public void run() {
                if (mController.hasLoadedADMod()) {
                    mADOnShow = true;
                    mController.setImageKey(mBannerImageKey);
                    mController.setImageSize(492, 80);
                    mController.requestCatchImage(200);
                }
            }
        });
    }

    
    private void onAnimationFinished() {
        mController.stopCatchImage();
        mController.notifyFinish();
        mSESceneManager.runInUIThread(new Runnable() {

            public void run() {
                mController.loadAD();
            }

        });
    }

    private static int startPointRight;
    private static int startPointLeft;
    private static int totalLength;
    private static int reverseLength;
    private static final int frequency = 4;
    public void playAnimation(float speed) {
        if (mShowAnimation == null || mShowAnimation.isFinish()) {
            if (startPointRight <= 0) {
                final int width = SESceneManager.getInstance().getScreenWidth();
                startPointLeft = (int)(width/2);
                startPointRight = (int)(width/2);
                reverseLength = width + 1024;
                totalLength = 2 * (width + 1024);
            }
            AnimationInfo info = new AnimationInfo();
            info.mSpeed = speed;
            info.mAnimCount = (int) (totalLength / info.mSpeed + 1);
            info.mReversedCount = (int) (reverseLength / info.mSpeed + 1);
            info.mChangeDirection = new int[] { startPointLeft, startPointRight };
            mShowAnimation = new MoveAnimation(getScene(), info);
            mShowAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    onAnimationFinished();
                }
            });
            mShowAnimation.execute();
        }
    }

    public void fly() {
        initStatus(getScene());
        applyAdPolicy();
        playAnimation(frequency);
    }

    public void stopFlyDelayed(long delayed) {
        SELoadResThread.getInstance().cancel(mStopFlyTask);
        SELoadResThread.getInstance().process(mStopFlyTask, delayed);
    }

    public void cancelStopFly() {
        SELoadResThread.getInstance().cancel(mStopFlyTask);
    }

    private Runnable mStopFlyTask = new Runnable() {
        public void run() {
            new SECommand(getScene()) {
                public void run() {
                    if (mShowAnimation != null && !mShowAnimation.isFinish()) {
                        mShowAnimation.stop();
                        onAnimationFinished();
                    }
                }
            }.execute();
        }
    };

    void setBannerImageKey(String imageKey) {
        mBannerImageKey = imageKey;
    }

    void setBanner(SEObject object) {
        mBanner = object;
    }

    public class MoveAnimation extends CountAnimation {
        private AnimationInfo mAnimationInfo;
        private SEVector3f mBeginLoc;
        private SEVector3f mStep;

        public MoveAnimation(SEScene scene) {
            super(scene);
        }

        public MoveAnimation(SEScene scene, AnimationInfo info) {
            this(scene);
            mAnimationInfo = info;
        }

        void setAnimationInfo(AnimationInfo info) {
            mAnimationInfo = info;
        }

        AnimationInfo getAnimationInfo() {
            return mAnimationInfo;
        }

        @Override
        public void runPatch(int count) {
            onAnimationRun(count, getAnimationCount());
            int stepCount;
            if (count < mAnimationInfo.mReversedCount) {
                if (count == 1) {
                    stepCount = 0;
                    changeDirection(mAnimationInfo.mChangeDirection[0]);
                }
                stepCount = count;
            } else {
                if (count == mAnimationInfo.mReversedCount) {
                    mAnimationInfo.mReversed = !mAnimationInfo.mReversed;
                    changeDirection(mAnimationInfo.mChangeDirection[1]);
                }
                stepCount = count - mAnimationInfo.mReversedCount;
            }
            setTranslate(mBeginLoc.add(mStep.mul(stepCount)), true);
        }

        private void changeDirection(float beginLocation) {
            float skyHeight = getScene().mSceneInfo.getSkyHeightOffset();
            float y = (float) (Math.random() * 1000);
            if(y > 600) {  // max Y is 600 
            	 y = 600;
            }
            if(isScreenOrientationPortrait() && y < 250) {
            	y = 250;
            }
            if (mAnimationInfo.mReversed) {
                mBeginLoc = new SEVector3f(-beginLocation, y, skyHeight);
                mStep = new SEVector3f(mAnimationInfo.mSpeed, 0, 0);
                setRotate(new SERotate(180, 0, 0, 1), true);
                mBanner.setTexCoordXYReverse_JNI(true, false);
            } else {
                mBeginLoc = new SEVector3f(beginLocation, y, skyHeight);
                mStep = new SEVector3f(-mAnimationInfo.mSpeed, 0, 0);
                setRotate(new SERotate(0, 0, 0, 1), true);
                mBanner.setTexCoordXYReverse_JNI(false, false);
            }
        }

        @Override
        public void onFirstly(int count) {
            onStartAnimation();
            setVisible(true, true);
            if (Math.random() > 0.5) {
                mAnimationInfo.mReversed = false;
            } else {
                mAnimationInfo.mReversed = true;
            }
        }

        @Override
        public int getAnimationCount() {
            return mAnimationInfo.mAnimCount;
        }

        @Override
        public void onFinish() {
            setVisible(false, true);
        }

    }

    public abstract void onAnimationRun(int count, int TotalFrames);

    public static class AnimationInfo {
        public boolean mReversed;
        public float mSpeed;
        public int mAnimCount;
        public int mReversedCount;
        public int[] mChangeDirection;
    }


    protected void applyAdPolicy() {
        if (mController.isRemoveAD()) {
            mController.stopCatchImage();
            trimAdOff();
        }
    }

    protected void trimAdOff() {
        SELoadResThread.getInstance().process(new Runnable() {

            @Override
            public void run() {
                Bitmap myCustomBitmp = Bitmap.createBitmap(512, 128, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(myCustomBitmp);
                canvas.drawColor(Color.BLUE);
                TextPaint paint = new TextPaint();
                paint.setFakeBoldText(true);
                paint.setColor(Color.WHITE);
                paint.setTextSize(30f);
                String text = SettingsActivity.getAdContent(getContext());
                if (TextUtils.isEmpty(text)) {
                    text = getContext().getString(R.string.ad_default_blessings);
                }
                StaticLayout titleLayout = new StaticLayout(text, paint, 512, Layout.Alignment.ALIGN_CENTER, 1f, 0.1f,
                        false);
                canvas.translate(0, (128 -titleLayout.getHeight()) / 2 );
                titleLayout.draw(canvas);
                final int imageData = SEObject.loadImageData_JNI(myCustomBitmp);
                myCustomBitmp.recycle();
                new SECommand(SESceneManager.getInstance().getCurrentScene()) {
                    public void run() {
                        SEObject.addImageData_JNI(mBannerImageKey, imageData);
                    }
                }.execute();
            }

        }, 250);

    }
}
