package com.borqs.se.widget3d;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.animation.DecelerateInterpolator;

import com.borqs.market.wallpaper.RawPaperItem;
import com.borqs.market.wallpaper.WallpaperUtils;
import com.borqs.se.R;
import com.borqs.se.download.Utils;
import com.borqs.se.engine.*;
import com.borqs.se.engine.SEScene.SCENE_CHANGED_TYPE;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEVector.SEVector2f;
import com.borqs.se.home3d.ProviderUtils.ThemeColumns;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.HomeScene;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.HomeManager;
import com.borqs.se.home3d.ThemeInfo;
import com.borqs.se.home3d.UpdateDBThread;
import com.borqs.se.shortcut.LauncherModel;
import com.borqs.se.shortcut.LauncherModel.ShortcutCallBack;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;
import com.support.StaticUtil;

public class House extends VesselObject implements ShortcutCallBack {
    private static final String TAG = "HOUSE";
    public String mCurrentTheme;

    private VelocityTracker mVelocityTracker;
    private SEEmptyAnimation mToFaceAnimation;
    private float mCylinderIndex;
    private int mWallNum;
    private float mPerFaceAngle;
    private float mCurrentAngle;
    private ArrayList<WallRadiusChangedListener> mWallRadiusChangedListeners;

    private boolean mOnMoveSight;
    private boolean mCancelClick;
    private HomeManager mHomeManager;

    // for wallpaper
    private List<String> mImageNamesOfAllWall;
    private String mImageNamesOfGround;
    private int mImgSizeX, mImgSizeY;
    private String mCurrentImage;
    private File mSdcardTempFile;
    private String mSaveImagePath;

    private int mPreAction;
    private Ground mGround;
    private Map<Integer, Wall> mAllWall;
    private boolean mHasBeenLoadedFinish = false;

    public interface WallRadiusChangedListener {
        public void onWallRadiusChanged(int faceIndex);
    }

    public House(HomeScene scene, String name, int index) {
        super(scene, name, index);
        mHomeManager = HomeManager.getInstance();
        mWallRadiusChangedListeners = new ArrayList<WallRadiusChangedListener>();
        setPressType(PRESS_TYPE.NONE);
    }

    @Override
    public void initStatus() {
        super.initStatus();
        setOnClickListener(null);
        setOnLongClickListener(null);
        mCurrentTheme = HomeManager.getInstance().getCurrentThemeInfo().mThemeName;
        mWallNum = getHomeSceneInfo().mWallNum;
        mPerFaceAngle = 360.0f / mWallNum;
        mOnMoveSight = false;
        mCancelClick = false;
        mCurrentAngle = -getHomeSceneInfo().mWallIndex * mPerFaceAngle;
        this.setUserRotate(new SERotate(getAngle(), 0, 0, 1));
        updateFaceIndex(true);
        LauncherModel.getInstance().addAppCallBack(this);
        LauncherModel.getInstance().setShortcutCallBack(this);
        setVesselLayer(new HouseLayer(getHomeScene(), this));
        initComponent();
        initCurrentThemeFeature();
        getHomeScene().setHouse(this);
        updateCurrentDisplayedWall();
        updateIndicater();
        setHasInit(true);
    }

    private void updateIndicater() {
        float wallIndex;
        if (mCylinderIndex < 0) {
            wallIndex = (mWallNum + mCylinderIndex % mWallNum) % mWallNum;
        } else {
            wallIndex = mCylinderIndex % mWallNum;
        }
        getHomeScene().updateWallIndicater(wallIndex, mWallNum);
    }

    private void initComponent() {
        if (mSdcardTempFile == null) {
            File dir = new File(HomeUtils.SDCARD_PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            mSdcardTempFile = new File(HomeUtils.SDCARD_PATH + "/" + ".tempimage");
        }
        String path = getCurrentThemeFolderPath();
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String regularName = "ground";
        final SEObject ground = findComponenetObjectByRegularName(regularName);
        if (ground != null) {
            mImageNamesOfGround = ground.getImageName();
            ground.setClickable(true);
            ground.setIsEntirety(true);
            ground.setPressType(PRESS_TYPE.COLOR);
            ground.setOnLongClickListener(new OnTouchListener() {
                public void run(SEObject obj) {
                    mImgSizeX = 1;
                    mImgSizeY = 1;
                    mCurrentImage = mImageNamesOfGround;
                    mSaveImagePath = getGroundPaperSavedPath(0);
                    getScene().handleMessage(HomeScene.MSG_TYPE_SHOW_SELECT_WALLPAPER_DIALOG, null);
                }
            });
        }

        SEObject roof = findComponenetObjectByRegularName("roof");
        if (roof != null) {
            roof.setClickable(true);
            roof.setIsEntirety(true);
            roof.setPressType(PRESS_TYPE.NONE);
            roof.setOnDoubleClickListener(new OnTouchListener() {
                @Override
                public void run(SEObject obj) {
                    performDoubleTap();
                }
            });
        }

        mImageNamesOfAllWall = new ArrayList<String>();
        for (int i = 0; i < mWallNum; i++) {
            regularName = "wall_face_" + i;
            SEObject obj = findComponenetObjectByRegularName(regularName);
            final int wallIndex = i;
            if (obj != null) {
                final String imageName = obj.getImageName();
                mImageNamesOfAllWall.add(imageName);
                obj.setClickable(true);
                obj.setIsEntirety(true);
                obj.setPressType(PRESS_TYPE.NONE);
                obj.setOnLongClickListener(new OnTouchListener() {
                    public void run(SEObject obj) {
                        setWallRatio();
                        mCurrentImage = imageName;
                        mSaveImagePath = getWallPaperSavePath(wallIndex);
                        getScene().handleMessage(HomeScene.MSG_TYPE_SHOW_WALL_LONG_CLICK_DIALOG, null);
                    }
                });

                obj.setOnDoubleClickListener(new OnTouchListener() {
                    @Override
                    public void run(SEObject obj) {
                        performDoubleTap();
                    }
                });
            }
        }

        rebuildVirtualGroundAndWalls();
    }

    @Override
    public void onLoadFinished() {
        rebuildVirtualGroundAndWalls();

        onFirstWallObjectReady();

        mHasBeenLoadedFinish = true;
    }

    public Ground getGround() {
        return mGround;
    }

    public Wall getWall(int index) {
        Wall wall = null;
        if (mAllWall != null) {
            wall = mAllWall.get(index);
        }
        if (wall == null) {
            String errorLog = "get wall equals null:\n";
            if (mAllWall == null) {
                errorLog = errorLog + "mAllWall equals null;\n";
            } else {
                errorLog = errorLog + "mAllWall size equal " + mAllWall.size() + ";\n";
                errorLog = errorLog + " get index equal " + index + ";\n";
                errorLog = errorLog + " wall face number equal " + getHomeSceneInfo().mWallNum + ";\n";
            }
            StaticUtil.reportError(getContext(), errorLog);
        }
        return wall;
    }

    @Override
    public SETransParas getTransParasInVessel(NormalObject needPlaceObj, ObjectSlot objectSlot) {
        if (objectSlot.mSlotIndex >= 0) {
            SETransParas transparas = new SETransParas();
            float angle = objectSlot.mSlotIndex * 360.f / getHomeSceneInfo().mWallNum;
            SEVector2f yDirection = new SEVector2f((float) Math.cos((angle + 90) * Math.PI / 180),
                    (float) Math.sin((angle + 90) * Math.PI / 180));
            SEVector2f xDirection = new SEVector2f((float) Math.cos(angle * Math.PI / 180), (float) Math.sin(angle
                    * Math.PI / 180));
            float offsetY = getHomeSceneInfo().mWallRadius;
            float offsetX = 0;
            SEVector2f offset = yDirection.mul(offsetY).add(xDirection.mul(offsetX));
            float offsetZ = getHomeSceneInfo().mWallHeight / 2;
            float z = offsetZ;
            transparas.mTranslate.set(offset.getX(), offset.getY(), z);
            transparas.mRotate.set(angle, 0, 0, 1);
            return transparas;
        }
        return null;
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
//        new SECommand(getScene()) {
//            public void run() {
//                final String iconName = SettingsActivity.getAppIconBackgroundName(getContext());
//                if (iconName.equals(mBackgroundName)) {
//                    return;
//                }
//                if (!"none".equals(iconName)) {
//                    mBackgroundName = iconName;
//                    SELoadResThread.getInstance().process(new Runnable() {
//                        @Override
//                        public void run() {
//                            final String imagePath = "assets/base/appwall/home_appwall_" + iconName + ".png";
//                            final int imageData = SEObject.loadImageData_JNI(imagePath);
//                            new SECommand(getScene()) {
//                                public void run() {
//                                    SEObject.addImageData_JNI(mBackgroundKey, imageData);
//                                    showBackground();
//                                }
//                            }.execute();
//                        }
//                    });
//                } else {
//                    mBackgroundName = "none";
//                    hideBackground();
//                }
//            }
//        }.execute();

    }
    
    /**
     * 当主题改变时我们需要做：
     * 第一，找到主题下面的房子，且更新当前房子ObjectInfo表的物体名字
     * 第二，找到当前场景中的房子，删除房子节点下的名称为 house.mName +“_model”的物体（该物体为模型节点）
     * 第三，更改房子名称，并更改ModelInfo
     * 第四，从新加载房子的模型节点。
     * 第五，初始化房子
     */
    @Override
    public void onSceneChanged(SCENE_CHANGED_TYPE changeType) {
        super.onSceneChanged(changeType);
        if (changeType == SCENE_CHANGED_TYPE.NEW_CONFIG) {
            // 第一，找到主题下面的房子，且更新当前房子ObjectInfo表的物体名字
            ThemeInfo themeInfo = mHomeManager.getCurrentThemeInfo();
            ModelInfo modelInfo = mHomeManager.getModelManager().findModelInfo(themeInfo.mHouseName);
            if (modelInfo == null) {
                modelInfo = HomeUtils.getModelInfoFromDB(getContext(), themeInfo.mHouseName);
                mHomeManager.getModelManager().mModels.put(themeInfo.mHouseName, modelInfo);
            } else {
                ModelInfo newModelInfo = HomeUtils.getModelInfoFromDB(getContext(), themeInfo.mHouseName);
                if (newModelInfo != null) {
                    modelInfo.updateFromNewModelInfo(newModelInfo);
                }
            }
            if (modelInfo == null) {
                return;
            }

            // 第二，找到当前场景中的房子，删除房子节点下的名称为 house.mName +“_model”的物体（该物体为模型节点）
            SEObject modelOfHouse = new SEObject(getScene(), mName + "_model");
            modelOfHouse.release();
            // 第三，更改房子名称，并更改ModelInfo
            getObjectInfo().mModelInfo.moveInstancesTo(modelInfo);
            changeModelInfo(modelInfo);
            setName(themeInfo.mHouseName);
            // 第四，从新加载房子的模型节点。
            SEObject newModelOfHouse = new SEObject(getScene(), themeInfo.mHouseName + "_model");
            mHomeManager.getModelManager().loadModelOnly(modelInfo, House.this, newModelOfHouse, new Runnable() {
                public void run() {
                    // 第五，初始化房子
                    initStatus();
                }
            });
        }

    }

    private void initCurrentThemeFeature() {
        setupThemeFeature(getThemeFeatureMap());
    }

    private String getThemeFeature() {
        String where = ThemeColumns.NAME + "='" + mCurrentTheme + "'";
        String[] selection = { ThemeColumns.CONFIG };
        Cursor cursor = getContext().getContentResolver().query(ThemeColumns.CONTENT_URI, selection, where, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getString(0);
        }
        if (cursor != null) {
            cursor.close();
            return null;
        }
        return null;
    }

    public void addWallRadiusChangedListener(WallRadiusChangedListener l) {
        if (!mWallRadiusChangedListeners.contains(l)) {
            mWallRadiusChangedListeners.add(l);
            l.onWallRadiusChanged(getWallNearestIndex());
        }
    }

    public void removeWallRadiusChangedListener(WallRadiusChangedListener l) {
        if (l != null) {
            mWallRadiusChangedListeners.remove(l);
        }
    }

    public void setRotate(float angle) {
        if (mCurrentAngle == angle) {
            return;
        }
        SERotate rotate = new SERotate(angle);
        mCurrentAngle = angle;
        int preIndex = getWallNearestIndex();
        updateFaceIndex(false);
        int curIndex = getWallNearestIndex();
        if (preIndex != curIndex) {
            for (WallRadiusChangedListener l : mWallRadiusChangedListeners) {
                l.onWallRadiusChanged(curIndex);
            }
            updateCurrentDisplayedWall();
        }

        setUserRotate(rotate);
    }

    private void updateFaceIndex(boolean force) {
        mCylinderIndex = -mCurrentAngle / mPerFaceAngle;
        updateIndicater();
    }

    private void updateCurrentDisplayedWall() {
        int curIndex = getWallNearestIndex();
        int showFaceIndexA = curIndex - 1;
        if (showFaceIndexA < 0) {
            showFaceIndexA = mWallNum - 1;
        }
        int showFaceIndexB = curIndex;
        int showFaceIndexC = curIndex + 1;
        if (showFaceIndexC > mWallNum - 1) {
            showFaceIndexC = 0;
        }
        if (mAllWall != null) {
            Iterator<Entry<Integer, Wall>> iter = mAllWall.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Integer, Wall> entry = iter.next();
                Wall wall = entry.getValue();
                if (entry.getKey() == showFaceIndexA || entry.getKey() == showFaceIndexB
                        || entry.getKey() == showFaceIndexC) {
                    wall.setVisible(true);
                } else {
                    wall.setVisible(false);
                }

            }
        }
    }

    private float getFaceAngle(float index) {
        float to = -index * mPerFaceAngle;
        return to;
    }

    public float getWallIndex() {
        return mCylinderIndex;
    }

    public float getAngle() {
        return mCurrentAngle;
    }

    public int getWallNearestIndex() {
        float index;
        if (mCylinderIndex < 0) {
            index = mWallNum + mCylinderIndex % mWallNum;
        } else {
            index = mCylinderIndex % mWallNum;
        }
        int nearestIndex = Math.round(index);
        if (nearestIndex == mWallNum) {
            nearestIndex = 0;
        }
        return nearestIndex;
    }

    private void trackVelocity(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        final VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000);
    }

    public void toNearestFace(SEAnimFinishListener l, float interpolatorfactor) {
        stopAllAnimation(null);
        toFace(Math.round(mCylinderIndex), l, interpolatorfactor);
    }

    public void toLeftFace(SEAnimFinishListener listener, float interpolatorfactor) {
        stopAllAnimation(null);
        toFace(Math.round(mCylinderIndex) + 1, listener, interpolatorfactor);
    }

    public void toLeftHalfFace(SEAnimFinishListener listener, float interpolatorfactor) {
        stopAllAnimation(null);
        float toFace = mCylinderIndex + 1;
        toFace(Math.round(toFace + 0.5f) - 0.5f, listener, interpolatorfactor);
    }

    public void toRightFace(SEAnimFinishListener listener, float interpolatorfactor) {
        stopAllAnimation(null);
        toFace(Math.round(mCylinderIndex) - 1, listener, interpolatorfactor);
    }

    public void toRightHalfFace(SEAnimFinishListener listener, float interpolatorfactor) {
        stopAllAnimation(null);
        float toFace = mCylinderIndex - 1;
        toFace(Math.round(toFace + 0.5f) - 0.5f, listener, interpolatorfactor);
    }

    public void toFace(float face, final SEAnimFinishListener listener, float interpolatorfactor) {
        stopAllAnimation(null);
        float srcAngle = mCurrentAngle;
        float desAngle = getFaceAngle(face);
        if (mCurrentAngle == desAngle) {
            if (listener != null) {
                listener.onAnimationfinish();
            }
            return;
        }
        int animationTimes = (int) (Math.sqrt(Math.abs(desAngle - srcAngle)) * interpolatorfactor * 3);
        mToFaceAnimation = new SEEmptyAnimation(getScene(), srcAngle, desAngle, animationTimes) {

            @Override
            public void onAnimationRun(float value) {
                setRotate(value);
            }

        };
        mToFaceAnimation.setInterpolator(new DecelerateInterpolator(interpolatorfactor));
        mToFaceAnimation.setAnimFinishListener(new SEAnimFinishListener() {
            public void onAnimationfinish() {
                getHomeSceneInfo().updateWallIndex(getWallNearestIndex());
                if (listener != null) {
                    listener.onAnimationfinish();
                }
            }
        });
        mToFaceAnimation.execute();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        trackVelocity(event);
        return super.dispatchTouchEvent(event);
    }

    private boolean mHasGotAction = false;
    private boolean mOnMoveSightToWall = false;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            setPreTouch();
            mOnMoveSightToWall = false;
            mPreAction = ev.getAction();
            if (isBusy() || getHomeScene().getStatus(HomeScene.STATUS_ON_SKY_SIGHT)
                    || getHomeScene().getStatus(HomeScene.STATUS_ON_DESK_SIGHT)) {
                if (getHomeScene().getStatus(HomeScene.STATUS_ON_DESK_SIGHT)) {
                    getHomeScene().moveToWallSight(null);
                    mOnMoveSight = false;
                    mHasGotAction = false;
                    mOnMoveSightToWall = true;
                    return true;
                } else if (getHomeScene().getStatus(HomeScene.STATUS_ON_SKY_SIGHT)) {
                    mOnMoveSight = true;
                    mHasGotAction = true;
                }
                stopAllAnimation(null);
                return true;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (getHomeScene().getStatus(HomeScene.STATUS_ON_WIDGET_TOUCH)) {
                if (mPreAction == MotionEvent.ACTION_DOWN
                        && Math.abs(getTouchX() - getPreTouchX()) > Math.abs(getTouchY() - getPreTouchY())) {
                    setPreTouch();
                    mHasGotAction = true;
                    mOnMoveSight = false;
                    mPreAction = ev.getAction();
                    return true;
                } else {
                    setPreTouch();
                    mHasGotAction = true;
                    mOnMoveSight = false;
                    mPreAction = ev.getAction();
                    return false;
                }
            }
            setPreTouch();
            mHasGotAction = false;
            stopAllAnimation(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mOnMoveSightToWall) {
            return true;
        }
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            stopAllAnimation(null);
            setPreTouch();
            mCancelClick = false;
            break;
        case MotionEvent.ACTION_MOVE:
            float dY = getTouchY() - getPreTouchY();
            float dX = getTouchX() - getPreTouchX();
            if (!mHasGotAction) {
                if (Math.abs(dY) > 2.2f * Math.abs(dX)) {
                    mOnMoveSight = true;
                }
            }
            mHasGotAction = true;
            if (mOnMoveSight) {
                float skyY = (dY) * 2f / getCamera().getHeight() + getHomeScene().getSightValue();
                if (skyY < -1) {
                    skyY = -1;
                } else if (skyY > 1) {
                    skyY = 1;
                }
                getHomeScene().changeSight(skyY, true);
                mCancelClick = true;
            } else {
                int width = getCamera().getWidth();
                float ratio = (float) (mPerFaceAngle * 2 / width);
                float transAngle = ratio * (getTouchX() - getPreTouchX());
                float curAngle = mCurrentAngle - transAngle;
                setRotate(curAngle);
            }
            setPreTouch();
            break;
        case MotionEvent.ACTION_UP:
            setPreTouch();
            if (mOnMoveSight) {
                if (!mCancelClick) {
                    mOnMoveSight = false;
                    mHasGotAction = false;
                    getHomeScene().moveToWallSight(null);
                    stopAllAnimation(null);
                    toNearestFace(null, 2);
                    return true;
                }
            }
        case MotionEvent.ACTION_CANCEL:
            if (mOnMoveSight) {
                mOnMoveSight = false;
                if (mVelocityTracker.getYVelocity() > 500) {
                    if (getHomeScene().getSightValue() > 0) {
                        getHomeScene().moveToSkySight(null);
                    } else {
                        getHomeScene().moveToWallSight(null);
                    }
                } else if (mVelocityTracker.getYVelocity() < -500) {
                    if (getHomeScene().getSightValue() < 0) {
                        getHomeScene().moveToDeskSight(null);
                    } else {
                        getHomeScene().moveToWallSight(null);
                    }
                } else {
                    if (getHomeScene().getSightValue() > 0.5f) {
                        getHomeScene().moveToSkySight(null);
                    } else if (getHomeScene().getSightValue() < -0.5f) {
                        getHomeScene().moveToDeskSight(null);
                    } else {
                        getHomeScene().moveToWallSight(null);
                    }
                }
                stopAllAnimation(null);
                toNearestFace(null, 2);
            } else {
                stopAllAnimation(null);
                playVelocityAnimation(mVelocityTracker.getXVelocity() / getHomeScene().getScreenDensity());
            }
            mHasGotAction = false;
            break;
        }
        return true;
    }

    private void playVelocityAnimation(float vX) {
        int toFace;
        if (Math.abs(vX) < 5000) {
            if (vX > 150) {
                if (mCylinderIndex < 0) {
                    toFace = (int) mCylinderIndex;
                } else {
                    toFace = (int) mCylinderIndex + 1;
                }
            } else if (vX < -150) {
                if (mCylinderIndex < 0) {
                    toFace = (int) mCylinderIndex - 1;
                } else {
                    toFace = (int) mCylinderIndex;
                }
            } else {
                toFace = Math.round(mCylinderIndex);
            }
        } else {
            toFace = Math.round(mCylinderIndex + vX / 1666);
        }
        toFace(toFace, null, 3);
    }

    private boolean isBusy() {
        if (mToFaceAnimation != null && !mToFaceAnimation.isFinish()) {
            return true;
        }
        return false;
    }

    @Override
    public void stopAllAnimation(SEAnimFinishListener l) {
        if (mToFaceAnimation != null) {
            mToFaceAnimation.stop();
        }
    }

    private boolean performDoubleTap() {
        if (mOnMoveSight) {
            return false;
        }
        float curCameraRadius = getCamera().getRadius();
        float minCameraRadius = getHomeSceneInfo().getCameraMinRadius();
        float maxCameraRadius = getHomeSceneInfo().getCameraMaxRadius();
        float distanceMax = maxCameraRadius - curCameraRadius;
        float distanceMin = curCameraRadius - minCameraRadius;
        if (distanceMax > distanceMin) {
            float endFov = getHomeSceneInfo().getCameraFovByRadius(maxCameraRadius);
            getHomeScene().playSetRadiusAndFovAnim(maxCameraRadius, endFov, null);
        } else {
            float endFov = getHomeSceneInfo().getCameraFovByRadius(minCameraRadius);
            getHomeScene().playSetRadiusAndFovAnim(minCameraRadius, endFov, null);
        }
        return true;
    }

    @Override
    public void onActivityRestart() {
        super.onActivityRestart();
        forceReloadWidget();
    }

    private void forceReloadWidget() {
        new SECommand(getScene()) {
            public void run() {
                List<NormalObject> matchApps = findAPP(null, "Widget");
                for (NormalObject widget : matchApps) {
                    WidgetObject myWidget = (WidgetObject) widget;
                    myWidget.bind();
                }
            }
        }.execute();
    }

    @Override
    public void onActivityDestory() {
        super.onActivityDestory();

    }

    @Override
    public void onPressHomeKey() {
        super.onPressHomeKey();
        if (hasInit()) {
            int toFace = Math.round(mCylinderIndex / mWallNum) * mWallNum;
            toFace(toFace, null, 3);
        }
        setWallRatio();
    }

    @Override
    public void onRelease() {
        super.onRelease();
        LauncherModel.getInstance().removeAppCallBack(this);
        LauncherModel.getInstance().setShortcutCallBack(null);
    }

    @Override
    public void shortcutAction(Context context, Intent data) {
        final Intent shortIntent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);

        if (!mHasBeenLoadedFinish || shortIntent == null) {
            return;
        }
        String action = shortIntent.getAction();
        if (!TextUtils.isEmpty(action) && action.equals("android.intent.action.CALL_PRIVILEGED")) {
            action = "android.intent.action.CALL";
            shortIntent.setAction(action);
        }

        final String shortUri = shortIntent.toURI();
        final String title = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Bitmap icon = null;
        Parcelable bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
        if (bitmap != null && bitmap instanceof Bitmap) {
            icon = (Bitmap) bitmap;
        } else {
            Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (extra != null && extra instanceof ShortcutIconResource) {
                try {
                    ShortcutIconResource iconResource = (ShortcutIconResource) extra;
                    Resources resources = getContext().getPackageManager().getResourcesForApplication(
                            iconResource.packageName);
                    int id = resources.getIdentifier(iconResource.resourceName, null, null);
                    icon = BitmapFactory.decodeResource(resources, id);
                } catch (Exception e) {
                }
            }
        }
        if (icon == null || TextUtils.isEmpty(title) || TextUtils.isEmpty(shortUri)) {
            return;
        }
        final Bitmap shortIcon = icon;
        boolean duplicate = data.getBooleanExtra("duplicate", true);
        if (duplicate || !shortcutExists(shortUri)) {
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    final ObjectInfo info = new ObjectInfo();
                    info.mName = "shortcut_" + System.currentTimeMillis();
                    info.mSceneName = getScene().getSceneName();
                    info.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
                    info.mType = "Shortcut";
                    info.mObjectSlot.mSpanX = 1;
                    info.mObjectSlot.mSpanY = 1;
                    info.mShortcutUrl = shortUri;
                    info.mDisplayName = title;
                    info.mShortcutIcon = shortIcon;
                    final ShortcutObject shortcutObject = new ShortcutObject(getHomeScene(), info.mName, info.mIndex);
                    shortcutObject.setObjectInfo(info);
                    info.saveToDB(new Runnable() {
                        public void run() {
                            new SECommand(getScene()) {
                                public void run() {
                                    getScene().getContentObject().addChild(shortcutObject, true);
                                    shortcutObject.initStatus();
                                    getVesselLayer().placeObjectToVessel(shortcutObject);
                                }
                            }.execute();

                        }
                    });
                }
            });
        } else {
            shortIcon.recycle();
            RemoveExistsShortcut(shortUri);
        }
    }

    private void RemoveExistsShortcut(final String uri) {
        new SECommand(getScene()) {
            public void run() {
                List<NormalObject> newItems = findShortcut(uri);
                for (NormalObject obj : newItems) {
                    removeChild(obj, true);
                }
            }
        }.execute();
    }

    private List<NormalObject> findShortcut(String uri) {
        List<NormalObject> newItems = new ArrayList<NormalObject>();
        for (SEObject item : getChildObjects()) {
            if (item instanceof ShortcutObject) {
                NormalObject appObject = (NormalObject) item;
                if (uri.equals(appObject.getObjectInfo().mShortcutUrl)) {
                    newItems.add(appObject);
                }
            }
        }
        return newItems;
    }

    private boolean shortcutExists(String uri) {
        for (SEObject item : getChildObjects()) {
            if (item instanceof ShortcutObject) {
                NormalObject appObject = (NormalObject) item;
                if (uri.equals(appObject.getObjectInfo().mShortcutUrl)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case HomeScene.REQUEST_CODE_BIND_WIDGET:
            if (data != null) {
                int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
                if (data.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
                    appWidgetId = data.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
                }
                if (resultCode == Activity.RESULT_OK) {
                    setAppWidget(appWidgetId);
                } else {
                    mHomeManager.getAppWidgetHost().deleteAppWidgetId(appWidgetId);
                }
            }
            break;
        case HomeScene.REQUEST_CODE_SELECT_WIDGET:
            if (data != null) {
                int appWidgetId = data.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
                if (resultCode == Activity.RESULT_OK) {
                    finishSetAppWidget(appWidgetId);
                } else {
                    mHomeManager.getAppWidgetHost().deleteAppWidgetId(appWidgetId);
                }
            }
            break;
        case HomeScene.REQUEST_CODE_SELECT_SHORTCUT:
            if (resultCode == Activity.RESULT_OK) {
                shortcutAction(getContext(), data);
            }
            break;
        case HomeScene.REQUEST_CODE_SELECT_WALLPAPER_IMAGE:
            if (resultCode == Activity.RESULT_OK) {
                Uri url = null;
                Bitmap pic = null;
                if (data != null) {
                    url = data.getData();
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        pic = extras.getParcelable("data");
                    }
                }
                final Uri imageFileUri = url;
                final Bitmap imageData = pic;
                final String imageName = mCurrentImage;
                final String imageKey = mSaveImagePath;
                if (imageName == null) {
                    return;
                }
                SELoadResThread.getInstance().process(new Runnable() {
                    public void run() {
                        int imageMaxSize = 512;
                        if (HomeUtils.getPhoneMemory() > 1200 || imageName.equals(mImageNamesOfGround)) {
                            imageMaxSize = 1024;
                        }
                        Bitmap bm = null;
                        if (imageFileUri != null) {
                            bm = HomeUtils.decodeSampledBitmapFromResource(getContext(), imageFileUri, imageMaxSize,
                                    imageMaxSize);
                        } else {
                            bm = HomeUtils.decodeSampledBitmapFromResource(mSdcardTempFile.getAbsolutePath(),
                                    imageMaxSize, imageMaxSize);
                        }
                        if (bm == null) {
                            bm = imageData;
                        }
                        if (bm == null) {
                            return;
                        }
                        int size = SEUtils.higherPower2(bm.getHeight());
                        if (size > imageMaxSize) {
                            size = imageMaxSize;
                        }
                        bm = Bitmap.createScaledBitmap(bm, size * bm.getWidth() / bm.getHeight(), size, true);
                        Bitmap des = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
                        Canvas canvas = new Canvas(des);
                        Paint paint = new Paint();
                        paint.setAntiAlias(true);
                        canvas.drawBitmap(bm, (size - bm.getWidth()) / 2, 0, paint);
                        bm.recycle();
                        HomeUtils.saveBitmap(des, imageKey, Bitmap.CompressFormat.JPEG);
                        des.recycle();
                        final int image = SEObject.loadImageData_JNI(imageKey);
                        if (image != 0) {
                            new SECommand(getScene()) {
                                public void run() {
                                    SEObject.applyImage_JNI(imageName, imageKey);
                                    SEObject.addImageData_JNI(imageKey, image);
                                }
                            }.execute();
                            updateWallpaper(imageName, imageKey);
                        }
                        System.gc();
                    }
                });
            }
            break;
        case HomeScene.REQUEST_CODE_SELECT_WALLPAPER_CAMERA:
            if (resultCode == Activity.RESULT_OK) {
                Intent intent2 = new Intent("com.android.camera.action.CROP");
                Uri u = Uri.fromFile(mSdcardTempFile);
                intent2.setDataAndType(u, "image/*");
                intent2.putExtra("output", Uri.fromFile(mSdcardTempFile));
                intent2.putExtra("crop", "true");
                intent2.putExtra("aspectX", mImgSizeX);
                intent2.putExtra("aspectY", mImgSizeY);
                intent2.putExtra("outputFormat", "JPEG");
                mHomeManager.startActivityForResult(intent2, HomeScene.REQUEST_CODE_SELECT_WALLPAPER_IMAGE);
            }
            break;
        }
    }

    public void mayChangeWallPaper() {
        setWallRatio();
        int wallIndex = getWallNearestIndex();
        mCurrentImage = mImageNamesOfAllWall.get(wallIndex);
        mSaveImagePath = getWallPaperSavePath(wallIndex);
    }


    private static final boolean USE_NEW_WALL_MENU = true;
    public Dialog createWallPaperDialog() {
        final int itemRes = USE_NEW_WALL_MENU ? R.array.edit_wall_menu_item_new : R.array.edit_wall_menu_item;
        Dialog dialog = new AlertDialog.Builder(SESceneManager.getInstance().getGLActivity())
                .setTitle(R.string.edit_wall_menu_title)
                .setItems(itemRes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, final int itemIndex) {
                        dialog.dismiss();
                        final int whichButton;
                        if (USE_NEW_WALL_MENU) {
                            if (itemIndex == 0) {
                                Utils.showWallpapers(SESceneManager.getInstance().getGLActivity());
                            }
                            whichButton = itemIndex - 1;
                        }
                        switch (whichButton) {
                            case 0:
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("image/*");
                                intent.putExtra("crop", "true");
                                intent.putExtra("aspectX", mImgSizeX);
                                intent.putExtra("aspectY", mImgSizeY);
                                intent.putExtra("output", Uri.fromFile(mSdcardTempFile));
                                intent.putExtra("outputFormat", "JPEG");
                                mHomeManager.startActivityForResult(intent, HomeScene.REQUEST_CODE_SELECT_WALLPAPER_IMAGE);
                                break;
                            case 1:
                                Uri u = Uri.fromFile(mSdcardTempFile);
                                Intent intent2 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
                                intent2.putExtra(MediaStore.EXTRA_OUTPUT, u);
                                mHomeManager
                                        .startActivityForResult(intent2, HomeScene.REQUEST_CODE_SELECT_WALLPAPER_CAMERA);
                                break;
                            case 2:
                                resetImage(mCurrentImage);
                                break;
                            case 3:
                                resetAllImage();
                                break;
                            case 4:
                                Utils.exportOrImportWallpaper(getContext());
                                break;
                        }
                    }
                }).create();
        return dialog;
    }

    private void resetImage(final String imageName) {
        // get the image path has been set, imagekey equals imagepath;
        File f = new File(getObjectInfo().mModelInfo.getImageNewKey(imageName));
        if (f.exists()) {
            f.delete();
        }
        final String imageOldKey = getObjectInfo().mModelInfo.getImageOldKey(imageName);
        new SECommand(getScene()) {
            public void run() {
                boolean exist = SEObject.isImageExist_JNI(imageOldKey);
                if (exist) {
                    SEObject.applyImage_JNI(imageName, imageOldKey);
                } else {
                    SELoadResThread.getInstance().process(new Runnable() {
                        public void run() {
                            final int image = SEObject.loadImageData_JNI(imageOldKey);
                            new SECommand(getScene()) {
                                public void run() {
                                    SEObject.applyImage_JNI(imageName, imageOldKey);
                                    SEObject.addImageData_JNI(imageOldKey, image);
                                }
                            }.execute();
                        }
                    });
                }
            }
        }.execute();
        updateWallpaper(imageName, imageOldKey);
    }

    private void setAppWidget(int appWidgetId) {
        /* Check for configuration */
        AppWidgetProviderInfo providerInfo = mHomeManager.getAppWidgetManager().getAppWidgetInfo(appWidgetId);
        if (providerInfo == null) {
            mHomeManager.getAppWidgetHost().deleteAppWidgetId(appWidgetId);
            return;
        }
        if (providerInfo.configure != null) {
            Intent configureIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            configureIntent.setComponent(providerInfo.configure);
            configureIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            if (configureIntent != null) {
                mHomeManager.startActivityForResult(configureIntent, HomeScene.REQUEST_CODE_SELECT_WIDGET);
            }
        } else {
            finishSetAppWidget(appWidgetId);
        }
    }

    private void finishSetAppWidget(int appWidgetId) {
        final AppWidgetProviderInfo providerInfo = mHomeManager.getAppWidgetManager().getAppWidgetInfo(appWidgetId);
        if (providerInfo != null) {
            final ObjectInfo info = new ObjectInfo();
            info.mAppWidgetId = appWidgetId;
            info.mName = "widget_" + System.currentTimeMillis();
            info.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
            info.mSceneName = getScene().getSceneName();
            info.mType = "Widget";
            final int[] span = HomeUtils.getSpanForWidget(getContext(), providerInfo);
            info.mObjectSlot.mSpanX = span[0];
            info.mObjectSlot.mSpanY = span[1];
            info.mComponentName = providerInfo.provider;
            final WidgetObject widget = new WidgetObject(getHomeScene(), info.mName, info.mIndex);
            widget.setObjectInfo(info);
            info.saveToDB();
            new SECommand(getScene()) {
                public void run() {
                    final SEObject root = getScene().getContentObject();
                    root.addChild(widget, false);
                    widget.loadMyself(getScene().getContentObject(), new Runnable() {
                        public void run() {
                            if (root.getChildObjects().contains(widget)) {
                                getVesselLayer().placeObjectToVessel(widget);
                            }
                        }
                    });
                }
            }.execute();

        }
    }

    // / reform duplicate code
    private void rebuildVirtualGroundAndWalls() {
        if (null == mAllWall) {
            mAllWall = new HashMap<Integer, Wall>();
        } else {
            mAllWall.clear();
        }

        for (SEObject child : getChildObjects()) {
            if (child instanceof Ground) {
                mGround = (Ground) child;
            } else if (child instanceof Wall) {
                Wall wall = (Wall) child;
                mAllWall.put(wall.getObjectSlot().mSlotIndex, wall);
            }
        }
        updateCurrentDisplayedWall();
    }

    // / reform duplicate code end
    /// reform saved paper path of wall image for generic object and use later
    private String getCurrentThemeFolderPath() {
        return getContext().getFilesDir() + File.separator + mCurrentTheme;
    }

    private String getWallPaperSavePath(int wallIndex) {
        return getCurrentThemeFolderPath() + "/wall_paper_" + wallIndex + ".jpg";
    }

    private String getGroundPaperSavedPath(int groundIndex) {
        return getCurrentThemeFolderPath() + "/ground_paper_" + groundIndex + ".jpg";
    }
    /// reform save paper path end

    /// wallpaper and share begin
    private List<String> getWallPaperList() {
        ArrayList<String> list = new ArrayList<String>();
        list.addAll(mImageNamesOfAllWall);
        return list;
    }

    private List<String> getGroundPaperList() {
        ArrayList<String> list = new ArrayList<String>();
        list.add(mImageNamesOfGround);
        return list;
    }

    private float mPhysicalWallHeight2WidthRatio = -1f;

    private void onFirstWallObjectReady() {
        // todo: enhance me
        if (Utils.isScreenOrientationPortrait()) {
            mPhysicalWallHeight2WidthRatio = 4f / 3;
            mImgSizeY = 4;
            mImgSizeX = 3;
        } else {
            mPhysicalWallHeight2WidthRatio = 3f / 4;
            mImgSizeY = 3;
            mImgSizeX = 4;
        }
//        float[] targetGoe = null;
//        float dyMin = 0;
//        float[] currentGoe;
//        if (mAllWall != null) {
//            Iterator<Entry<Integer, Wall>> iter = mAllWall.entrySet().iterator();
//            while (iter.hasNext()) {
//                Map.Entry<Integer, Wall> entry = iter.next();
//                Wall wall = entry.getValue();
//                currentGoe = wall.getSizeOfObject_JNI();
//                if (null == targetGoe) {
//                    dyMin = currentGoe[4] - currentGoe[1];
//                    targetGoe = currentGoe;
//                } else {
//                    if (currentGoe[4] - currentGoe[1] < dyMin && currentGoe[4] - currentGoe[1] > 0) {
//                        dyMin = currentGoe[4] - currentGoe[1];
//                        targetGoe = currentGoe;
//                    }
//                }
//            }
//        }
//
//        final float height = targetGoe[5] - targetGoe[2];
//        final float width = targetGoe[3] - targetGoe[0];
//        mPhysicalWallHeight2WidthRatio = height / width;
    }

    private float getWallRatioH2W() {
        return mPhysicalWallHeight2WidthRatio;
    }

    public void updateWallpaper(String imageName, String imageKey) {
        ObjectInfo info = getObjectInfo();
        if (null != info) {
            info.mModelInfo.updateImageKey(imageName, imageKey);
            WallpaperUtils.clearAppliedFlag(getContext());
        }
    }

//    private static void performPaperCycleApply(ModelInfo modelInfo, List<String> paperKey,
//                                                  ArrayList<String> paperList) {
//        if (null == paperList || paperList.isEmpty()) {
//            // reset all by set value of all key as null
//            for (String item : paperKey) {
//                modelInfo.updateImageKeyInForeground(item, null);
//            }
//        } else {
//            final int keySize = paperKey.size();
//            final int valueSize = paperList.size();
//            String targetPath;
//            String imageName;
//            for (int i = 0; i < keySize; i++) {
//                imageName = paperKey.get(i);
//                targetPath = paperList.get(i % valueSize);
//                // todo: convert origin image with h2wRatio, and crop bitmap into
//                // expected files.
//                modelInfo.updateImageKeyInForeground(imageName, targetPath);
//            }
//        }
//    }

    private void performWallCycleApply(ModelInfo modelInfo, ArrayList<String> wallBundle) {
        List<String> keyList = getWallPaperList();
        if (null == keyList || keyList.isEmpty()) {
            return;
        }

        if (null == wallBundle || wallBundle.isEmpty()) {
            for (String key : keyList) {
                modelInfo.updateImageKeyInForeground(key, null);
            }
        } else {
            final int keySize = keyList.size();
            final int valSize = wallBundle.size();
            String srcPath;
            String dstPath;

            final int wallpaperSize = calculateBestSize(wallBundle);
            final float ratio = getWallRatioH2W();
            for (int i = 0; i < keySize; i++) {
                srcPath = wallBundle.get(i % valSize);
                dstPath = getWallPaperSavePath(i);
                encodePaperFile(srcPath, dstPath, wallpaperSize, ratio);
                modelInfo.updateImageKeyInForeground(keyList.get(i), dstPath);
            }
        }
    }

    private void encodePaperFile(String srcPath, String dstPath, int wallpaperSize, float wallRatioH2W) {
        WallpaperUtils.encodePaperFile(srcPath, dstPath, wallpaperSize, wallRatioH2W);
    }

    private void performGroundCycleApply(ModelInfo modelInfo, ArrayList<String> groundBundle) {
        List<String> keyList = getGroundPaperList();
        if (null == keyList || keyList.isEmpty()) {
            return;
        }

        if (null == groundBundle || groundBundle.isEmpty()) {
            for (String key : keyList) {
                modelInfo.updateImageKeyInForeground(key, null);
            }
        } else {
            final int keySize = keyList.size();
            final int valSize = groundBundle.size();
            String srcPath;
            String dstPath;
            final int wallpaperSize = calculateBestSize(groundBundle);
            final float ratio = 1.0f;
            for (int i = 0; i < keySize; i++) {
                srcPath = groundBundle.get(i % valSize);
                dstPath = getGroundPaperSavedPath(i);
                encodePaperFile(srcPath, dstPath, wallpaperSize, ratio);
                modelInfo.updateImageKeyInForeground(keyList.get(i), dstPath);
            }
        }
    }

    public void updateWallpaperBundle(final ArrayList<String> wallBundle,
                                      final ArrayList<String> groundBundle) {
        UpdateDBThread.getInstance().process(new Runnable() {
            public void run() {
                ObjectInfo info = getObjectInfo();
                ModelInfo modelInfo = null == info ? null : info.mModelInfo;
                if (null == modelInfo) {
                    Log.e(TAG, "updateWallpaperBundle, skip without valid model object info.");
                    return;
                }

                performWallCycleApply(modelInfo, wallBundle);
                performGroundCycleApply(modelInfo, groundBundle);
                getHomeScene().notifyWallpaperChanged();
            }
        });
    }

    /**
     * 必需每次都执行，因为更换地板的时候 mImgSizeY和mImgSizeX会变 mImgSizeX = 4;
     */
    private void setWallRatio() {
        onFirstWallObjectReady();
    }

    public void onWallPaperChanged() {
        ArrayList<String> list = new ArrayList<String>();
        list.addAll(mImageNamesOfAllWall);
        list.add(mImageNamesOfGround);
        ModelInfo info = getObjectInfo().mModelInfo;
        for (String item : list) {
            loadImageItem(info.mImageInfo.getImageItem(item));
        }
    }

    private void loadImageItem(final ModelInfo.ImageItem imageItem) {
//        boolean exist = SEObject.isImageExist_JNI(imageItem.mNewPath);
//        if (!exist) {
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    final int imageData = SEObject.loadImageData_JNI(imageItem.mNewPath);
                    new SECommand(getHomeScene()) {
                        public void run() {
                            SEObject.addImageData_JNI(imageItem.mNewPath, imageData);
                            SEObject.applyImage_JNI(imageItem.mImageName, imageItem.mNewPath);
                        }
                    }.execute();
                }
            });
//        } else {
//            SEObject.applyImage_JNI(imageItem.mImageName, imageItem.mNewPath);
//        }
    }


    public ArrayList<RawPaperItem> queryRawPaperItems() {
        ArrayList<RawPaperItem> rawPaperItems = new ArrayList<RawPaperItem>();

        ModelInfo modelInfo = getObjectInfo().mModelInfo;

        RawPaperItem item;
        List<String> groundKeyList = getGroundPaperList();
        for (String ground : groundKeyList) {
            item = new RawPaperItem(ground, RawPaperItem.TYPE_GROUND, 1.0f);
            item.mDecodedValue = getPaperPath(modelInfo, ground);
            rawPaperItems.add(item);
        }

        List<String> wallKeyList = getWallPaperList();
        for (String wall : wallKeyList) {
            item = new RawPaperItem(wall, RawPaperItem.TYPE_WALL, getWallRatioH2W());
            item.mDecodedValue = getPaperPath(modelInfo, wall);
            rawPaperItems.add(item);
        }

        return rawPaperItems;
    }

    private static String getPaperPath(ModelInfo modelInfo, String imageName) {
        if (null == modelInfo || TextUtils.isEmpty(imageName)) {
            return null;
        }
        String path = modelInfo.getImageNewKey(imageName);
        if (TextUtils.isEmpty(path)) {
            path = modelInfo.getImageOldKey(imageName);
        }

        return path;
    }

    @Override
    public void removeAllEmptyDecorator() {
        for (Wall wall : mAllWall.values()) {
            wall.removeAllEmptyDecorator();
        }
    }

    @Override
    public void checkAndSetDecoratorVisibility(boolean show) {
        for (Wall wall : mAllWall.values()) {
            wall.checkAndSetDecoratorVisibility(show);
        }
    }

    /// create House object or HouseSimple that will show all app on the ground,
    /// and use icon box for app and excluded all other 3d objects, unify the scene
    /// management via option menu, long click.
    /// todo: configure these via settings, pre-load configuration, etc.
    private static final boolean ENABLED_SIMPLE_HOUSE = true;
    public static NormalObject instance(HomeScene scene, String name, int index) {
        if (ENABLED_SIMPLE_HOUSE) {
            return new House(scene, name, index);
        } else {
            return new House(scene, name, index);
        }
    }

    // light feature in the house
//    private SEObject mLight;
    private void turnOnFeature(String objName, String featureName,
                                 HashMap<String, Boolean> featureMap) {
        if (null == featureMap || TextUtils.isEmpty(objName)
                || TextUtils.isEmpty(featureName)) {
            return;
        }

        Boolean enabled = featureMap.get(featureName);
        SEObject object = findComponenetObjectByRegularName(objName);
        if (object != null) {
            boolean on = null == enabled || enabled.booleanValue();
            object.setVisible(on);
        }
    }

    // light feature in the house end.
    private void setupThemeFeature(HashMap<String, Boolean> featureMap) {
        if (null != featureMap && !featureMap.isEmpty()) {
//            turnOnFeature(OBJECT_LIGHT, FEATURE_LIGHT, featureMap);
//            turnOnFeature(OBJECT_ROOF_TOP, FEATURE_ROOF_TOP, featureMap);
            HashMap<String, String> objectMap = new HashMap<String, String>();
            objectMap.put(OBJECT_LIGHT, FEATURE_LIGHT);
            objectMap.put(OBJECT_ROOF_TOP, FEATURE_ROOF_TOP);
            turnOnFeature(objectMap, featureMap);
        }
    }

    protected static final String FEATURE_LIGHT = "light";
    protected static final String FEATURE_ROOF_TOP = "roof_top";

    protected static final String OBJECT_LIGHT = "light";
    protected static final String OBJECT_ROOF_TOP = "roof_top";
    protected HashMap<String, Boolean> getThemeFeatureMap() {
        HashMap<String, Boolean> featureMap = new HashMap<String, Boolean>();

        String themeConfig = getThemeFeature();
        if (!TextUtils.isEmpty(themeConfig)) {
            String[] featureList = themeConfig.split(";");
            if (featureList != null && featureList.length > 0) {
                for (String item : featureList) {
                    if (TextUtils.isEmpty(item)) {
                        continue;
                    }

                    String[] feature = item.split(":");
                    if (feature != null && feature.length == 2) {
                        String featureName = feature[0];
                        try {
                            featureMap.put(featureName, Boolean.getBoolean(feature[1]));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return featureMap;
    }

    public void onWallLabelShow(int index) {
        if (mAllWall.isEmpty()) return;

        final int wallSize = mAllWall.size();
        final int offset = getWallNearestIndex();
        Wall left = mAllWall.get((offset + 1) % wallSize);
        Wall mid = mAllWall.get(offset);
        Wall right = mAllWall.get((offset + wallSize - 1) % wallSize);

        left.onLabelShown(index);
        if (mid != left) {
            mid.onLabelShown(index);
        }
        if (right != mid) {
            right.onLabelShown(index);
        }

        final ArrayList<Wall> walls = new ArrayList<Wall>();
        walls.addAll(mAllWall.values());
        walls.remove(left);
        walls.remove(mid);
        walls.remove(right);

//        SELoadResThread.getInstance().process(new Runnable() {
//            public void run() {
                for (Wall item : walls) {
                    item.onLabelShown(index);
                }
//            }
//        });
    }

    private int calculateBestSize(ArrayList<String> pathBundle) {
        int target = HomeManager.getWallpaperMaxSize();
        int maxSize = 0;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        int itemLen;
        for (String path : pathBundle) {
            BitmapFactory.decodeFile(path, options);
            itemLen = Math.max(options.outHeight, options.outWidth);
            if (maxSize < itemLen) {
                maxSize = itemLen;
            }
        }
        if (HomeUtils.DEBUG) {
            Log.v(TAG, "calculateBestSize, max len of paper bundle is " + maxSize);
        }
        if (maxSize < target) {
            target = maxSize;
        }

        target = SEUtils.higherPower2(target);

        Log.v(TAG, "calculateBestSize, convert to power of 2: " + target);
        return target;
    }

    private void resetAllImage() {
        for (String imageName : mImageNamesOfAllWall) {
            resetImage(imageName);
        }
        resetImage(mImageNamesOfGround);
    }
}
