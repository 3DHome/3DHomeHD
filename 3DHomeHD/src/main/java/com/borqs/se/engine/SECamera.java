package com.borqs.se.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.database.Cursor;
import android.util.Log;
import android.view.MotionEvent;

import com.borqs.se.engine.SEVector.SERay;
import com.borqs.se.engine.SEVector.SEVector2i;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.ProviderUtils;

public class SECamera {
    private final String TAG = "SECamera";
    /******************************* native interface begin ************************/

    public native void getLocation_JNI(float[] location);

    public native void getAxisZ_JNI(float[] axisZ);

    public native void getAxisY_JNI(float[] axisY);

    public native void getAxisX_JNI(float[] axisX);

    private native void screenCoordinateToRay_JNI(int x, int y, float[] ray);

    private native void setCamera_JNI(float[] location, float[] axisZ, float[] up, float fov, float ratio, float near,
            float far);

    public native void setCamera_JNI(float[] location, float[] target, float fov, float ratio, float near, float far);

    public native void setFrustum_JNI(float fov, float ratio, float near, float far);

    private native void rotateLocal_JNI(float[] rotate);

    private native void rotateLocal_JNI(float angle, int axis);

    private native void translateLocal_JNI(float[] translate);

    private native void setLocation_JNI(float[] loc);

    private native void setViewport_JNI(int x, int y, int w, int h);

    public native void operateCamera_JNI(float[] location, float[] rotate, boolean transLocal);

    public native static String getBestPosition_JNI(float[] pos, float[] targetPos);

    public native SEObject getSelectedObject_JNI(int x, int y);

    private native float[] worldToScreenCoordinate_JNI(float[] location);

    public SEVector2i worldToScreenCoordinate(SEVector3f location) {
        float[] out = worldToScreenCoordinate_JNI(location.mD);
        SEVector2i screenCoordinate = new SEVector2i(Math.round(out[0]), Math.round(out[1]));
        return screenCoordinate;
    }

    /******************************* native interface end ************************/
    private SEScene mScene;
    private String mSceneName;
    private CountAnimation mMoveSightAnimation;
    private CountAnimation mSetFovAnimation;
    private List<CameraChangedListener> mCameraChangedListeners;
    private float mSkyY = SIGHT_DEFAULT;

    public interface CameraChangedListener {
        public void onCameraChanged();
    }

    public SECamera(SEScene scene) {
        mCameraChangedListeners = new ArrayList<CameraChangedListener>();
        mScene = scene;
        mSceneName = mScene.mSceneName;
    }

    public synchronized void addCameraChangedListener(CameraChangedListener l) {
        if (!mCameraChangedListeners.contains(l)) {
            mCameraChangedListeners.add(l);
            l.onCameraChanged();
        }
    }

    public synchronized void removeCameraChangedListener(CameraChangedListener l) {
        if (mCameraChangedListeners.contains(l)) {
            mCameraChangedListeners.remove(l);
        }
    }

    public synchronized void clearCameraChangedListener() {
        mCameraChangedListeners.clear();
    }

    protected void setSceneName(String sceneName) {
        mSceneName = sceneName;
    }

    public int getWidth() {
        return getCurrentData().mWidth;
    }

    public int getHeight() {
        return getCurrentData().mHeight;
    }

    public float getFov() {
        return getCurrentData().mFov;
    }

    public SEVector3f getLocation() {
        return getCurrentData().mLocation;
    }

    public SEVector3f getAxisZ() {
        return getCurrentData().mAxisZ.normalize();
    }

    public SEVector3f getAxisX() {
        return getCurrentData().mUp.cross(getAxisZ()).normalize();
    }

    public SEVector3f getAxisY() {
        return getCurrentData().mAxisZ.cross(getAxisX()).normalize();
    }

    public SEVector3f getUp() {
        return getCurrentData().mUp;
    }

    public float getNear() {
        return getCurrentData().mNear;
    }

    public float getFar() {
        return getCurrentData().mFar;
    }

    public float getRatio() {
        return ((float) getHeight()) / getWidth();
    }

    public void save() {
        getCurrentData().save();
    }

    public SECameraData restore() {
        return getCurrentData().restore();
    }

    public synchronized void notifySurfaceChanged(int width, int height) {
        getCurrentData().update(width, height);
        setCamera();
        setViewport_JNI(0, 0, width, height);
        for (CameraChangedListener l : mCameraChangedListeners) {
            l.onCameraChanged();
        }
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        return false;
    }

    public SECameraData getCurrentData() {
        return mScene.mSceneInfo.getCurrentData();
    }

    // won't change fov anymore, change location instead.
//    public void setFov(float angle) {
//        getCurrentData().mFov = angle;
//        setCamera();
//    }
//
//    private void setLocation(SEVector3f location) {
//        getCurrentData().mLocation = location;
//        setCamera();
//    }

    public float getRadius() {
        return getLocation().getVectorZ().getLength();
    }

    public SEVector3f getScreenLocation(float scale) {
        float paras = (float) (getWidth() / (2 * Math.tan(getFov() * Math.PI / 360)));
        SEVector3f loc = getLocation().add(getScreenOrientation().mul(paras * scale));
        return loc;
    }

    public SEVector3f getScreenOrientation() {
        return getAxisZ().mul(-1);
    }

    public SERay screenCoordinateToRay(int x, int y) {
        float[] data = new float[6];
        screenCoordinateToRay_JNI(x, y, data);
        return new SERay(data);
    }

    public void setCamera() {
        for (CameraChangedListener l : mCameraChangedListeners) {
            l.onCameraChanged();
        }
        if (HomeUtils.DEBUG) {
            Log.d(TAG, "setCamera, location = " + getLocation()
                    + ", axis = " + getAxisZ() + ", fov = " + getFov()
                    + ", up = " + getUp() + ", near = " + getNear() + ", far = " + getFar());
        }
        setCamera_JNI(getLocation().mD, getAxisZ().mD, getUp().mD, getFov(), getRatio(), getNear(), getFar());
    }

    private static CountAnimation playNavigationAnimation(SEScene scene, SECamera camera,
                                         SECameraData src, SECameraData dst,
                                         float currentRatio,
                                         final SEAnimFinishListener l) {
        camera.stopAllAnimation();
//        mSetFovAnimation = new SetFovAnimation(mScene, endFov, ZOOM_STEP);
//        mSetFovAnimation.setAnimFinishListener(listener);
//        mSetFovAnimation.execute();
        CountAnimation animation = new NavigationAnimation(scene, camera, src, dst, currentRatio);
        animation.setAnimFinishListener(l);
//        animation.setAnimFinishListener(new SEAnimFinishListener() {
//            public void onAnimationfinish() {
//                if (l != null) {
//                    l.onAnimationfinish();
//                }
//            }
//        });
        animation.execute();
        return animation;
    }

    public static class NavigationAnimation extends CountAnimation {
        private static final float STEP = 0.03f;
        private SECamera mCamera;
        private SECameraData mSrcData;
        private SECameraData mDstData;
        private float mCurrentRatio;
        private int mCount;

        public NavigationAnimation(SEScene scene, SECamera camera,
                                   SECameraData src, SECameraData dst,
                                   float ratio) {
            super(scene);

            mCamera = camera;
            mSrcData = src.clone();
            mDstData = dst.clone();
            mCurrentRatio = ratio;
            mCount = 100 - (int)(ratio / STEP);
        }

        public void runPatch(int count) {
            float ratio = mCurrentRatio + count * STEP;
            if (count >= mCount) {
                ratio = 1;
                stop();
            } else if (count == mCount) {
            }

            navigateCurrentCamera(mCamera, mSrcData, mDstData, ratio);
        }

        @Override
        public void onFirstly(int count) {
        }

        @Override
        public int getAnimationCount() {
            return mCount;
        }
    }


    public final static double MAX_SIGHT_ANGLE = Math.PI * 0.23;
//    public final static float SIGHT_FOV = 45;

//    private SEVector3f getSkyCameraLocation(SEVector3f centerLocation, float r) {
//        float paras = (float) (r / Math.tan(SIGHT_FOV * Math.PI / 360));
//        SEVector3f screenOrientation = new SEVector3f(0, (float) Math.cos(MAX_SIGHT_ANGLE),
//                (float) Math.sin(MAX_SIGHT_ANGLE));
//        SEVector3f loc = centerLocation.subtract(screenOrientation.mul(paras));
//        return loc;
//    }

//    public void changeSight(float skyY, boolean breakAnimation) {
//        if (mScene.getStatus(SEScene.STATUS_ON_UNLOCK_ANIMATION)) {
//            return;
//        }
//        if (breakAnimation) {
//            stopAllAnimation();
//        }
//        if (mSkyY == 0) {
//            save();
//            calculateSkySightLocation();
//            mDeskSightLocation = mScene.mSceneInfo.mDockSceneInfo.calculateDeskSightLocation(getWidth(), getHeight());
//        }
//        if (skyY > 1) {
//            mSkyY = 1;
//        } else if (skyY < -1) {
//            mSkyY = -1;
//        } else {
//            mSkyY = skyY;
//        }
//        if (mSkyY == 0) {
//            mScene.setStatus(SEScene.STATUS_ON_DESK_SIGHT, false);
//            mScene.setStatus(SEScene.STATUS_ON_SKY_SIGHT, false);
//        } else if (mSkyY < 0) {
//            mScene.setStatus(SEScene.STATUS_ON_DESK_SIGHT, true);
//            mScene.setStatus(SEScene.STATUS_ON_SKY_SIGHT, false);
//        } else {
//            mScene.setStatus(SEScene.STATUS_ON_DESK_SIGHT, false);
//            mScene.setStatus(SEScene.STATUS_ON_SKY_SIGHT, true);
//        }
//        SEVector3f srcLoc = restore().mLocation;
//        SEVector3f srcAxisZ = restore().mAxisZ;
//        float srcFov = restore().mFov;
//        float paras = Math.abs(mSkyY);
//        SEVector3f desAxisZ;
//        if (mSkyY >= 0) {
//            desAxisZ = new SEVector3f(0, -(float) Math.cos(MAX_SIGHT_ANGLE), -(float) Math.sin(MAX_SIGHT_ANGLE));
//            getCurrentData().mLocation = srcLoc.add(mSkySightLocation.subtract(srcLoc).mul(paras));
//        } else {
//            desAxisZ = new SEVector3f(0, -(float) Math.cos(-MAX_SIGHT_ANGLE), -(float) Math.sin(-MAX_SIGHT_ANGLE));
//            getCurrentData().mLocation = srcLoc.add(mDeskSightLocation.subtract(srcLoc).mul(paras));
//        }
//        getCurrentData().mAxisZ = srcAxisZ.add(desAxisZ.subtract(srcAxisZ).mul(paras));
//        getCurrentData().mFov = srcFov + (SIGHT_FOV - srcFov) * paras;
//        setCamera();
//    }

    public void changeSight(float skyY, boolean breakAnimation) {
        if (mScene.getStatus(SEScene.STATUS_ON_UNLOCK_ANIMATION)) {
            return;
        }
        if (breakAnimation) {
            stopAllAnimation();
        }

        final float originRatio = Math.abs(mSkyY);
        if (isDefaultSight()) {
            save();
        }

        if (skyY > SIGHT_UP) {
            mSkyY = SIGHT_UP;
        } else if (skyY < SIGHT_DOWN) {
            mSkyY = SIGHT_DOWN;
        } else {
            mSkyY = skyY;
        }

        if (isDefaultSight()) {
            mScene.setStatus(SEScene.STATUS_ON_DESK_SIGHT, false);
            mScene.setStatus(SEScene.STATUS_ON_SKY_SIGHT, false);
        } else if (wasDeskSightRange()) {
            mScene.setStatus(SEScene.STATUS_ON_DESK_SIGHT, true);
            mScene.setStatus(SEScene.STATUS_ON_SKY_SIGHT, false);
        } else {
            mScene.setStatus(SEScene.STATUS_ON_DESK_SIGHT, false);
            mScene.setStatus(SEScene.STATUS_ON_SKY_SIGHT, true);
        }
//        SEVector3f srcLoc = restore().mLocation;
//        SEVector3f srcAxisZ = restore().mAxisZ;
//        float srcFov = restore().mFov;
//        float paras = Math.abs(mSkyY);
//        SEVector3f desAxisZ;
//        if (mSkyY >= 0) {
//            desAxisZ = new SEVector3f(0, -(float) Math.cos(MAX_SIGHT_ANGLE), -(float) Math.sin(MAX_SIGHT_ANGLE));
//            getCurrentData().mLocation = srcLoc.add(mSkySightLocation.subtract(srcLoc).mul(paras));
//        } else {
//            desAxisZ = new SEVector3f(0, -(float) Math.cos(-MAX_SIGHT_ANGLE), -(float) Math.sin(-MAX_SIGHT_ANGLE));
//            getCurrentData().mLocation = srcLoc.add(mDeskSightLocation.subtract(srcLoc).mul(paras));
//        }
//        getCurrentData().mAxisZ = srcAxisZ.add(desAxisZ.subtract(srcAxisZ).mul(paras));
//        getCurrentData().mFov = srcFov + (SIGHT_FOV - srcFov) * paras;
//        setCamera();

        final float currentRatio = Math.abs(mSkyY);
        final boolean ascOrder = originRatio < currentRatio;

        final SECameraData srcData;
        final SECameraData targetData;
        final float ratio;
        if (mSkyY < SIGHT_DEFAULT) {
            final SECameraData downData = mScene.mSceneInfo.getDownCameraData();
            if (ascOrder) {
                ratio = currentRatio;
                srcData  = restore().clone();
                targetData = downData;
                if (HomeUtils.DEBUG) {
                    Log.v(TAG, "changeSight, base -> down, ratio = " + ratio + ", mSkyY = " + mSkyY);
                }
            } else {
                ratio = 1 - currentRatio;
                srcData = downData;
                targetData = restore().clone();
                if (HomeUtils.DEBUG) {
                    Log.v(TAG, "changeSight, down -> base, ratio = " + ratio + ", mSkyY = " + mSkyY);
                }
            }
        } else {
            final SECameraData upData = mScene.mSceneInfo.getUpCameraData();
            if (ascOrder) {
                ratio = currentRatio;
                srcData  = restore().clone();
                targetData = upData;
                if (HomeUtils.DEBUG) {
                    Log.v(TAG, "changeSight, base -> up, ratio = " + ratio + ", mSkyY = " + mSkyY);
                }
            } else {
                ratio = 1 - currentRatio;
                srcData  = upData;
                targetData = restore().clone();
                if (HomeUtils.DEBUG) {
                    Log.v(TAG, "changeSight, up -> base, ratio = " + ratio + ", mSkyY = " + mSkyY);
                }
            }
        }
        navigateCurrentCamera(this, srcData, targetData, ratio);
    }

    public float getSightValue() {
        return mSkyY;
    }

    public void moveToSkySight(final SEAnimFinishListener l) {
        if (mScene.getStatus(SEScene.STATUS_ON_UNLOCK_ANIMATION)) {
            return;
        }
        if (mSkyY == 1) {
            if (l != null) {
                l.onAnimationfinish();
            }
            return;
        }
        mMoveSightAnimation = performMovieSightAnimation(mScene, 1, 0.03f, l);
    }

    public void moveToDeskSight(SEAnimFinishListener l) {
        if (mScene.getStatus(SEScene.STATUS_ON_UNLOCK_ANIMATION)) {
            return;
        }
        if (mSkyY == -1) {
            if (l != null) {
                l.onAnimationfinish();
            }
            return;
        }
        mMoveSightAnimation = performMovieSightAnimation(mScene, -1, 0.05f, l);
    }

    public void moveToWallSight(final SEAnimFinishListener l) {
        if (mScene.getStatus(SEScene.STATUS_ON_UNLOCK_ANIMATION)) {
            return;
        }
        if (mSkyY == 0) {
            if (l != null) {
                l.onAnimationfinish();
            }
            return;
        }
        mMoveSightAnimation = performMovieSightAnimation(mScene, 0, 0.05f, l);
    }

//    public float getBestCameraFov(int screenW, int screenH) {
//        return mScene.mSceneInfo.mDockSceneInfo.getBestCameraFov(getLocation(), screenW, screenH);
//
//    }

    public boolean isBusy() {
        if (mMoveSightAnimation != null) {
            if (!mMoveSightAnimation.isFinish()) {
                return true;
            }
        }

        if (mSetFovAnimation != null) {
            if (!mSetFovAnimation.isFinish()) {
                return true;
            }
        }
        return false;
    }

    public void stopAllAnimation() {
        if (mMoveSightAnimation != null) {
            mMoveSightAnimation.stop();
        }
        if (mSetFovAnimation != null) {
            mSetFovAnimation.stop();
        }
    }

//    private class SetFovAnimation extends CountAnimation {
//        private SECameraData mEndFov;
//        private float mStep;
//
//        public SetFovAnimation(SEScene scene, SECameraData endFov, float step) {
//            super(scene);
//            mEndFov = endFov.clone();
//            mStep = step;
//        }
//
//        public void runPatch(int count) {
//            float needRun = mEndFov.mLocation.getY() - getLocation().getY();
//            if (Math.abs(needRun) < mStep) {
////                currentFov = mEndFov;
////                setFov(currentFov);
//                setLocation(mEndFov.mLocation);
//
//                stop();
//            } else {
////                if (needRun > 0) {
////                    currentFov = getFov() + mStep;
////                } else {
////                    currentFov = getFov() - mStep;
////                }
////                setFov(currentFov);
//                SEVector3f location = getLocation();
//                location.mD[1] += needRun > 0 ? mStep : -mStep;
//                setLocation(location);
//            }
//        }
//
//    }

    private CountAnimation performMovieSightAnimation(SEScene scene, float dstY,
                                                             float step, final SEAnimFinishListener l) {
        stopAllAnimation();
        CountAnimation animation = new MoveSightAnimation(scene, dstY, step);
        animation.setAnimFinishListener(new SEAnimFinishListener() {
            public void onAnimationfinish() {
                if (l != null) {
                    l.onAnimationfinish();
                }
            }
        });
        animation.execute();
        return animation;
    }
    public class MoveSightAnimation extends CountAnimation {
        private float mDesSkyY;
        private float mStep;

        public MoveSightAnimation(SEScene scene, float desSight, float step) {
            super(scene);
            mDesSkyY = desSight;
            mStep = step;
        }

        public void runPatch(int count) {
            float skyY = mSkyY + mStep;
            if (Math.abs(skyY - mDesSkyY) <= Math.abs(mStep)) {
                skyY = mDesSkyY;
                stop();
            }

            changeSight(skyY, false);
        }

        @Override
        public void onFirstly(int count) {
            if (mDesSkyY > mSkyY) {
                mStep = Math.abs(mStep);
            } else {
                mStep = -Math.abs(mStep);
            }
        }

    }

    private float getLocationDistance(SEVector3f from, SEVector3f to) {
        return to.subtract(from).getLength();
    }

    public void playUnlockScreenAnimation(int count, SEAnimFinishListener listener) {
        UnlockScreenAnimation anim = new UnlockScreenAnimation(mScene, count);
        anim.setAnimFinishListener(listener);
        anim.execute();
    }

    class UnlockScreenAnimation extends CountAnimation {

        private int mCount;
        private float mStep;
        private float mStartFov;
        private float mEndFov;
        private float mNeedFov;

        public UnlockScreenAnimation(SEScene scene, int count) {
            super(scene);
            mCount = count;
            mStartFov = 45f;
            mEndFov = getCurrentData().mFov;
        }

        @Override
        public void runPatch(int count) {
            float fov = mStartFov - mStep * count;
            if (fov < mEndFov) {
                fov = mEndFov;
            }
            updateFov(fov);
        }

        @Override
        public void onFirstly(int count) {
            mNeedFov = mStartFov - mEndFov;
            mStep = mNeedFov / mCount;
            updateFov(mStartFov);
        }

        @Override
        public int getAnimationCount() {
            return mCount;
        }

        private void updateFov(float angle) {
            getCurrentData().mFov = angle;
            setCamera();
        }
    }

    public void printCameraStatus() {
        Log.d("BORQS###############", "location:" + getLocation().toString());
        Log.d("BORQS###############", "axisZ:" + getAxisZ().toString());
        Log.d("BORQS###############", "up:" + getUp());
        Log.d("BORQS###############", "fov:" + getFov());
        Log.d("BORQS###############", "height:" + getHeight());
        Log.d("BORQS###############", "width:" + getWidth());
        Log.d("BORQS###############", "near:" + getNear());
        Log.d("BORQS###############", "far:" + getFar());
    }

    public static void parseCameraData(Cursor cursor,
                                               ArrayList<SECameraData> cameraList,
                                               HashMap<String, Integer> indexMap) {
        cameraList.clear();
        indexMap.clear();
        final int cursorPos = cursor.getPosition();

        SECameraData cameraData;
        int index = 0;
        do {
            cameraData = new SECameraData();
            cameraData.mFov = cursor.getFloat(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.FOV));
            cameraData.mNear = cursor.getFloat(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.NEAR));
            cameraData.mFar = cursor.getFloat(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.FAR));
            cameraData.mType = cursor.getString(cursor.getColumnIndex(ProviderUtils.SceneInfoColumns.TYPE));
            String cameraLoc = cursor.getString(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.LOCATION));
            cameraData.mLocation = SEVector3f.parseFrom(cameraLoc);
            String cameraZ = cursor.getString(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.ZAXIS));
            cameraData.mAxisZ = SEVector3f.parseFrom(cameraZ);
            String targetPos = cursor.getString(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.TARGETPOS));
            cameraData.mTargetPos = SEVector3f.parseFrom(targetPos);
            String cameraUp = cursor.getString(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.UP));
            cameraData.mUp = SEVector3f.parseFrom(cameraUp);

            cameraList.add(cameraData);
            indexMap.put(cameraData.mType, index);
            index++;

        } while (cursor.moveToNext());

        cursor.moveToPosition(cursorPos);
    }

    private SECameraData mScaleNearData;
    private SECameraData mScaleFarData;
    private SECameraData mScaleStep;
    public void onScalePrepare() {
        stopAllAnimation();
        mScaleNearData = mScene.mSceneInfo.getNearCameraData();
        mScaleFarData = mScene.mSceneInfo.getFarCameraData();

        mScaleStep = mScaleFarData.clone();
        mScaleStep.selfSubtract(mScaleNearData);
        mScaleStep.selfScale(0.7f);

        SECameraData extraData = mScaleStep.clone();
        extraData.selfScale(10f/mScaleStep.mLocation.getLength());
        mScaleNearData.selfSubtract(extraData);
        mScaleFarData.selfAdd(extraData);

    }

    public void onScaleChecked(final float scaleFactor) {
        if (scaleFactor == 0) {
            SECameraData current = getCurrentData();
            final SECameraData finalData;
            SECameraData near = mScene.mSceneInfo.getNearCameraData();
            SECameraData far = mScene.mSceneInfo.getFarCameraData();

            if (current.mLocation.getY() < far.mLocation.getY()) {
                finalData = far;
            } else if (current.mLocation.getY() > near.mLocation.getY()) {
                finalData = near;
            } else {
                setCamera();
                return;
            }

            final float ratio = 0;
            final SECameraData srcData = current.clone();
            if (HomeUtils.DEBUG) {
                Log.v(TAG, "onScaleChecked, from " + srcData.toString() + ", to " + finalData.toString());
            }
            mSetFovAnimation = playNavigationAnimation(mScene, this, srcData, finalData, ratio, null);
        } else if (scaleFactor != 1.0f) {
            new SECommand(mScene) {
                public void run() {
                    SECameraData near = mScaleNearData.clone();
                    SECameraData far = mScaleFarData.clone();
                    SECameraData target = mScaleStep.clone();

                    target.selfScale(1 - scaleFactor);
                    target.selfAdd(getCurrentData());

                    if (target.mLocation.getY() < far.mLocation.getY()) {
                        target = far;
                    } else if (target.mLocation.getY() > near.mLocation.getY()) {
                        target = near;
                    } else {
                        // keep current target.
                    }
                    if (HomeUtils.DEBUG) {
                        Log.i(TAG, "onScaleChecked, scaleFactor = " + scaleFactor
                                + ", target = " + target.toString());
                    }

                    target.mWidth = getWidth();
                    target.mHeight = getHeight();
                    getCurrentData().set(target);
                    setCamera();
                }
            }.execute();
        }
    }

//    private int doubleClickcount = 0;
    public void zoomInOut() {
//        float cameraToWallDistance = mSECamera.getRadius() + mSceneInfo.mWallRadius;
//        float wallSpan = mSceneInfo.mWallUnitSizeX * mSceneInfo.mWallSpanX;
//        float curCameraRadius = mSECamera.getFov();
//        float minCameraRadius = (float) (Math.atan(wallSpan * 0.5 / cameraToWallDistance) * 360 / Math.PI + 5);
//        float maxCameraRadius = (float) (Math.atan(wallSpan / cameraToWallDistance) * 360 / Math.PI);
//        float curCameraRadius = mSECamera.getFov();
//        float distanceMax = SECameraData.mMaxCameraRadius - curCameraRadius;
//        float distanceMin = curCameraRadius - SECameraData.mMinCameraRadius;
//        if (distanceMax > distanceMin) {
//            mSECamera.playSetFovAnimation(SECameraData.mMaxCameraRadius, 1, null);
//        } else {
//            mSECamera.playSetFovAnimation(SECameraData.mMinCameraRadius, 1, null);
//        }
//    	if(doubleClickcount == 2) {
    		moveToDefaultCamera(null);
//    		doubleClickcount = 0;
//    	}else {
//    		SECameraData current = getCurrentData();
//    		SECameraData far = mScene.mSceneInfo.getFarCameraData();
//    		SECameraData near = mScene.mSceneInfo.getNearCameraData();
//    		float distanceFar = Math.abs(current.mLocation.getY() - far.mLocation.getY());
//    		float distanceNear = Math.abs(current.mLocation.getY() - near.mLocation.getY());
//    		if (HomeUtils.DEBUG) {
//    			Log.i(TAG, "zoomInOut, distanceFar " + distanceFar + ", distanceNear " + distanceNear);
//    		}
//    		
//    		final float ratio;
//    		final SECameraData dstData;
//    		
//    		if (distanceFar > distanceNear) {
//    			dstData = far;
//    			ratio = distanceNear / (distanceFar + distanceNear);
//    		} else {
//    			dstData = near;
//    			ratio = distanceFar / (distanceFar + distanceNear);
//    		}
//    		
//    		if (HomeUtils.DEBUG) {
//    			Log.v(TAG, "zoomInOut, animating from " + current.toString() + ", to " + dstData.toString() + ", ratio = " + ratio);
//    		}
//    		doubleClickcount ++;
//    		mSetFovAnimation = playNavigationAnimation(mScene, this, current, dstData, ratio, null);
//    	}
    }
    
    public boolean equalCameraData(SECameraData data1, SECameraData data2) {
    	if(data1 != null && data2 != null 
    			&& data1.mLocation.equals(data2.mLocation)
    			&& data1.mAxisZ.equals(data2.mAxisZ)
    			&& data1.mFov == data2.mFov) {
    		return true;
    	}
    	return false;
    }

    public boolean moveToDefaultCamera(final SEAnimFinishListener l) {
    	SECameraData current = getCurrentData();
        SECameraData defaultCamera = mScene.mSceneInfo.getDefaultCameraData();
        if (equalCameraData(current, defaultCamera)) {
            return true;
        }

        SECameraData far = mScene.mSceneInfo.getFarCameraData();
        SECameraData near = mScene.mSceneInfo.getNearCameraData();
        float distanceFar = Math.abs(current.mLocation.getY() - far.mLocation.getY());
        float distanceNear = Math.abs(current.mLocation.getY() - near.mLocation.getY());

        final float ratio;
        if (distanceFar > distanceNear) {
            ratio = distanceNear / (distanceFar + distanceNear);
        } else {
            ratio = distanceFar / (distanceFar + distanceNear);
        }
        
        mSetFovAnimation = playNavigationAnimation(mScene, this, current, defaultCamera, ratio, l);
    	return false;
    }
    
    /// camera side reform beginning
    private static final float SIGHT_DEFAULT = 0;
    private static final float SIGHT_UP = 1;
    private static final float SIGHT_DOWN = -1;

    // Navigate camera between originData and finalData by in coming ratio, which
    // clone data from finalData, and then scale bellow element counting in given ratio:
    // 1. location
    // 2. fov
    // 3. axis
    // (optional item in the future)
    // 4. near
    // 5. far
    // 6. up direction
    // Each item was calculated in the same algorithm, first clone the origin data, secondly
    // self subtract the final data, and then self multiply with the ratio,
    // in the end add with origin one.
    private static void navigateCurrentCamera(SECamera camera, SECameraData originData, SECameraData finalData, float ratio){
        SECameraData target = calculateTargetData(originData, finalData, ratio);
        if (null != target) {
            // set width and height
            target.mWidth = camera.getWidth();
            target.mHeight = camera.getHeight();
            camera.getCurrentData().set(target);
            camera.setCamera();
        }
    }

    private static SECameraData calculateTargetData(SECameraData originData, SECameraData finalData, float ratio) {
        if (ratio == Float.NaN) {
            Log.w(HomeUtils.TAG, "calculateTargetData, skip with Float.NaN: ratio = " + ratio);
            return null;
        } else if (originData.equals(finalData)) {
            Log.w(HomeUtils.TAG, "calculateTargetData, skip with identical origin and target.");
            return null;
        } else if (ratio >= 1 || ratio <= -1) {
            return finalData.clone();
        } else if (ratio == 0) {
            return originData.clone();
        }

        // ratio should be between -1 to 1 excluding 0, and
        // reverse it to -ratio for calculating delta by
        // subtract origin data with final data
        SECameraData target = finalData.clone();
        if (HomeUtils.DEBUG) {
            Log.d(HomeUtils.TAG, "calculateTargetData, enter data = " + target.toString()
                    + ", originData = " + originData.toString()
                    + ", finalData = " + finalData.toString()
                    + ", ratio = " + ratio);
        }

        if (!finalData.mLocation.equals(originData.mLocation)) {
            target.mLocation.selfSubtract(originData.mLocation);
            target.mLocation.selfMul(ratio);
            target.mLocation.selfAdd(originData.mLocation);
            SEVector.roundVector(target.mLocation, originData.mLocation, finalData.mLocation);
        }

        if (!finalData.mTargetPos.equals(originData.mTargetPos)) {
            target.mTargetPos.selfSubtract(originData.mTargetPos);
            target.mTargetPos.selfMul(ratio);
            target.mTargetPos.selfAdd(originData.mTargetPos);
            SEVector.roundVector(target.mTargetPos, originData.mTargetPos, finalData.mTargetPos);
        }

        if (finalData.mFov != originData.mFov) {
            target.mFov -= originData.mFov;
            target.mFov *= ratio;
            target.mFov += originData.mFov;
            target.mFov = SEVector.roundFloat(target.mFov, originData.mFov, finalData.mFov);
        }

        if (!finalData.mAxisZ.equals(originData.mAxisZ)) {
            target.mAxisZ.selfSubtract(originData.mAxisZ);
            target.mAxisZ.selfMul(ratio);
            target.mAxisZ.selfAdd(originData.mAxisZ);
            SEVector.roundVector(target.mAxisZ, originData.mAxisZ, finalData.mAxisZ);
        }

        if (finalData.mNear != originData.mNear) {
            target.mNear -= originData.mNear;
            target.mNear *= ratio;
            target.mNear += originData.mNear;
            target.mNear = SEVector.roundFloat(target.mNear, originData.mNear, finalData.mNear);
        }

        if (finalData.mFar != originData.mFar) {
            target.mFar -= originData.mFar;
            target.mFar *= ratio;
            target.mFar += originData.mFar;
            target.mFar = SEVector.roundFloat(target.mFar, originData.mFar, finalData.mFar);
        }

        if (!finalData.mUp.equals(originData.mUp)) {
            target.mUp.selfSubtract(originData.mUp);
            target.mUp.selfMul(ratio);
            target.mUp.selfAdd(originData.mUp);
            SEVector.roundVector(target.mUp, originData.mUp, finalData.mUp);
        }

        if (HomeUtils.DEBUG) {
            Log.d(HomeUtils.TAG, "calculateTargetData, calculated target = " + target.toString()
                    + ", ratio = " + ratio);
        }
        return target;
    }

//    public float getSightValue() {
//        return mSkyY;
//    }

    public boolean isSkySight() {
        return mSkyY == SIGHT_UP;
    }

    public boolean isGroundSight() {
        return mSkyY == SIGHT_DOWN;
    }

    public boolean isDefaultSight() {
        return mSkyY == SIGHT_DEFAULT;
    }

//    public boolean wasLeftDownSight() {
//        return !wasDeskSightRange();
//    }

    public boolean wasDeskSightRange() {
        return mSkyY < SIGHT_DEFAULT;
    }

    public boolean wasSkySightRange() {
        return  mSkyY > SIGHT_UP / 2;
    }
    
    public boolean shouldMoveDeskSightRange() {
        return  mSkyY < SIGHT_DOWN / 2;
    }

    /// drag  up and down to raise up or lay down the camera sight if it
    //  is not unlocking screen.
    // 1. stop all animation
    // 2. detect current sight and save data if it was default sight.
    // 3. calculate the target ratio (mSkyY) value to locate camera
    // 4. round the target ratio within a valid range (from down sight to up sight)
    // 5. set the origin point for camera to the default sight point
    // 6. set the final point for camera to the up or down sight depend on current
    //    sight range saved in step 1, and round the final target ratio if necessary.
    // 7. set scene status and then navigate camera.
    public void dragSight(int elevation) {
//        if (!wasDeskSightRange()) {
            float skyY = (elevation) * 2f / getHeight() + getSightValue();
//            if (skyY < SIGHT_DEFAULT) {
//                skyY = SIGHT_DEFAULT;
//            }
            changeSight(skyY, true);
//        }
    }

    public void onDragEnd(float velocity) {
        if (!wasDeskSightRange()) {
            if (velocity > 200) {
                moveToSkySight(null);
            } else if (velocity < -200) {
                moveToWallSight(null);
            } else {
                if (wasSkySightRange()) {
                    moveToSkySight(null);
                } else {
                    moveToWallSight(null);
                }
            }
        }else {
        	if (velocity < -200) {
        		moveToDeskSight(null);
            } else {
            	if(shouldMoveDeskSightRange()) {
            		moveToDeskSight(null);	
            	}else {
            		moveToWallSight(null);
            	}
            }
        }
    }
    /// camera side reform end.
}
