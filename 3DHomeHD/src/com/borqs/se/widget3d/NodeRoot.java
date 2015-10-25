package com.borqs.se.widget3d;

import android.view.MotionEvent;
import android.view.VelocityTracker;

import com.borqs.se.engine.SEScene;
import com.borqs.se.home3d.ScaleGestureDetector;
//import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SESceneManager;

public class NodeRoot extends VesselObject implements ScaleGestureDetector.OnScaleGestureListener {
    //    private float mMinCameraRadius;
//    private float mMaxCameraRadius;
    private int mCheckSceneStatus;
    private VelocityTracker mVelocityTracker;

    public NodeRoot(SEScene scene, String name, int index) {
        super(scene, name, index);
        mCheckSceneStatus = SEScene.STATUS_APP_MENU + SEScene.STATUS_DISALLOW_TOUCH + SEScene.STATUS_HELPER_MENU
                + SEScene.STATUS_MOVE_OBJECT + SEScene.STATUS_OBJ_MENU
                + SEScene.STATUS_ON_DESK_SIGHT + SEScene.STATUS_ON_SKY_SIGHT + SEScene.STATUS_ON_WIDGET_SIGHT
                + SEScene.STATUS_OPTION_MENU + SEScene.STATUS_ON_WALL_DIALOG;

    }

    @Override
    public void initStatus(SEScene scene) {
        setIsEntirety_JNI(true);
        SESceneManager.getInstance().setScaleListener(this);
        setVesselLayer(new RootLayer(scene, this));
        setHasInit(true);
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        SESceneManager.getInstance().setScaleListener(this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (getScene().getStatus(SEScene.STATUS_ON_SCALL)) {
            return true;
        }
        return false;
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
        trackVelocity(event);
        return super.onTouchEvent(event);
    }

    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (!canScall()) {
            return false;
        }
        getScene().setStatus(SEScene.STATUS_ON_SCALL, true);
//        float cameraToWallDistance = getCamera().getRadius() + getScene().mSceneInfo.mWallRadius;
//        float wallSpan = getScene().mSceneInfo.mWallUnitSizeX * getScene().mSceneInfo.mWallSpanX;
//        mMinCameraRadius = (float) (Math.atan(wallSpan * 0.5 / cameraToWallDistance) * 360 / Math.PI + 5);
//        mMaxCameraRadius = (float) (Math.atan(wallSpan / cameraToWallDistance) * 360 / Math.PI);
//        getScene().calculateCameraRadiusScope();

        //// TODO: correct
//        mMinCameraRadius = SECameraData.mMinCameraRadius;
//        mMaxCameraRadius = SECameraData.mMaxCameraRadius;
        getCamera().onScalePrepare();
        return true;
    }

    public void onScaleEnd(ScaleGestureDetector detector) {
        getScene().setStatus(SEScene.STATUS_ON_SCALL, false);
//        float curCameraRadius = getCamera().getFov();
//        float endCameraRadius = curCameraRadius;
//        if (curCameraRadius < mMinCameraRadius) {
//            endCameraRadius = mMinCameraRadius;
//        } else if (curCameraRadius > mMaxCameraRadius) {
//            endCameraRadius = mMaxCameraRadius;
//        }
//        if (canScall()) {
//            getCamera().playSetFovAnimation(endCameraRadius, 1, null);
//        }
        if (canScall()) {
            getCamera().onScaleChecked(0);
        }

    }

    public boolean onScale(ScaleGestureDetector detector) {
        if (canScall()) {
            getCamera().onScaleChecked(detector.getScaleFactor());
            return true;
        }
        return false;
//        float scale = detector.getScaleFactor();
//        float currentCameraRadius = getCamera().getFov();
//        float targetCameraRadius = currentCameraRadius + (1 - scale) * 25;
//        if (targetCameraRadius < mMinCameraRadius - 2.5f) {
//            targetCameraRadius = mMinCameraRadius - 2.5f;
//        } else if (targetCameraRadius > mMaxCameraRadius + 2.5f) {
//            targetCameraRadius = mMaxCameraRadius + 2.5f;
//        }
//        if (!canScall()) {
//            return false;
//        }
//        final float r = targetCameraRadius;
//        new SECommand(getScene()) {
//            public void run() {
//                if (canScall()) {
//                    getCamera().setFov(r);
//                }
//            }
//        }.execute();
//        return true;
    }

    private boolean canScall() {
        if ((getScene().getStatus() & mCheckSceneStatus) > 0) {
            return false;
        }
        return true;
    }

    @Override
    public SETransParas getSlotTransParas(ObjectInfo objectInfo, NormalObject object) {
        return null;
    }
}
