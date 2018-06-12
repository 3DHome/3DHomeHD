package com.borqs.se.widget3d;

import android.view.MotionEvent;
import android.view.VelocityTracker;

import com.borqs.framework3d.home3d.AnimationFactory;
import com.borqs.framework3d.home3d.DockObject;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEVector.SERay;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEVector.SEVector2f;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.engine.SESceneManager.UnlockScreenListener;
import com.borqs.se.shortcut.LauncherModel;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;

import java.util.ArrayList;
import java.util.List;

public class Desk extends DockObject implements UnlockScreenListener {
    private VelocityTracker mVelocityTracker;
//    private float mAngle;
//    private float mDeskTranslateZ;
    private float mPreTouchAngle;
    private float[] mRecordRotate;
    private boolean mDisableTouch;

    private CountAnimation mDeskVelocityAnimation;
    private CountAnimation mToFaceAnimation;
    private CountAnimation mRunACircleAnimation;

    private CountAnimation mHideDeskAnimation;
    private CountAnimation mShowDeskAnimation;

    public Desk(SEScene scene, String name, int index) {
        super(scene, name, index);
        setClickable(true);
//        mDeskTranslateZ = 0;
//        mAngle = 0;
        mRecordRotate = new float[] { 0, 0, 0, 0, 0 };
        mDisableTouch = false;
    }

    @Override
    public void initStatus(SEScene scene) {
        setIsEntirety_JNI(true);
        SESceneManager.getInstance().addUnlockScreenListener(this);
        setVesselLayer(new DeskLayer(getScene(), this));
        LauncherModel.getInstance().addAppCallBack(this);
        setHasInit(true);
    }

    @Override
    public SETransParas getSlotTransParas(ObjectInfo objectInfo, NormalObject object) {
        SETransParas transParas = new SETransParas();
        if (objectInfo.mObjectSlot.mSlotIndex == -1) {
            transParas.mTranslate.set(0, 0, getBorderHeight());

        } else {
            transParas.mTranslate = getSlotPosition(objectInfo.mObjectSlot);
        }
        return transParas;
    }
    @Override
    public SEVector3f getSlotPosition(ObjectSlot objectSlot) {
        float angle = objectSlot.mSlotIndex * 360.f / getCount();
        return getAnglePosition(angle);
    }

    private SEVector3f getAnglePosition(float angle) {
        float x = (float) (-getBorderRadius() * Math.sin(angle * Math.PI / 180));
        float y = (float) (getBorderRadius() * Math.cos(angle * Math.PI / 180));
        SEVector3f slotPosition = new SEVector3f(x, y, getBorderHeight());
        return slotPosition;
    }

    @Override
    public void onPressHomeKey() {
        super.onPressHomeKey();
        toFace(0, null, 10);
    }
    
    @Override
    public void onRelease() {
        super.onRelease();
        SESceneManager.getInstance().removeUnlockScreenListener(this);
        LauncherModel.getInstance().removeAppCallBack(this);
    }


    private void clearRecord() {
        for (int i = 0; i < 5; i++) {
            mRecordRotate[i] = 0;
        }
    }

    private void pushToRecord(float rotate) {
        for (int i = 0; i < 4; i++) {
            mRecordRotate[i] = mRecordRotate[i + 1];
        }
        mRecordRotate[4] = rotate;
    }

    private float getAverageRecord() {
        float average = 0;
        for (int i = 0; i < 5; i++) {
            average = average + mRecordRotate[i];
        }
        return average / 5;
    }

//    public void setRotate(SERotate rotate) {
//        mAngle = rotate.getAngle();
//        setRotate(rotate, true);
//    }
//
//    public float getAngle() {
//        return mAngle;
//    }

    private void trackVelocity(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        final VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mDisableTouch) {
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (mDeskVelocityAnimation != null && !mDeskVelocityAnimation.isFinish()) {
                stopAllAnimation(null);
                return true;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (mMotionTarget != null && mMotionTarget instanceof NormalObject) {
                NormalObject motionTarget = (NormalObject) mMotionTarget;
                if ("Laptop".equals(motionTarget.getObjectInfo().mType)) {
                    return false;
                }
            }
            mPreTouchAngle = getTouchRotate(getTouchX(), getTouchY());
            stopAllAnimation(null);
            clearRecord();
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        trackVelocity(event);
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            getCamera().moveToDeskSight(null);
            mPreTouchAngle = getTouchRotate(getTouchX(), getTouchY());
            clearRecord();
            break;
        case MotionEvent.ACTION_MOVE:
            getCamera().moveToDeskSight(null);
            float touchAngle = getTouchRotate(getTouchX(), getTouchY());
            float needRotate = touchAngle - mPreTouchAngle;
            if (needRotate > 180) {
                needRotate = needRotate - 360;
            } else if (needRotate < -180) {
                needRotate = needRotate + 360;
            }
            pushToRecord(needRotate);
            float curAngle = getUserRotate().getAngle() + needRotate;
            setRotate(new SERotate(curAngle), true);
            mPreTouchAngle = touchAngle;
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            float xV = mVelocityTracker.getXVelocity();
            float yV = mVelocityTracker.getYVelocity();
            float v = (float) Math.sqrt(xV * xV + yV * yV);
            mDeskVelocityAnimation = AnimationFactory.createVelocityRotateAnimation(this, v, getAverageRecord(), 360.0f / getCount());
            mDeskVelocityAnimation.execute();
            break;
        }
        return true;
    }

    private float getTouchRotate(int x, int y) {
        SERay Ray = getCamera().screenCoordinateToRay(getTouchX(), getTouchY());
        SEVector3f touchLoc = rayCrossZ(Ray, getBorderHeight());
        SEVector2f vectorZ = touchLoc.getVectorZ();
        return (float) (vectorZ.getAngle() * 180 / Math.PI);
    }

    private SEVector3f rayCrossZ(SERay ray, float crossZ) {
        float para = (crossZ - ray.getLocation().getZ()) / ray.getDirection().getZ();
        SEVector3f touchLoc = ray.getLocation().add(ray.getDirection().mul(para));

        return touchLoc;
    }

    public void playDeskCorrectAnimation() {
        stopAllAnimation(null);
        mDeskVelocityAnimation = AnimationFactory.createVelocityRotateAnimation(this, 0, 0, 360.0f / getCount());
        mDeskVelocityAnimation.execute();
    }

    public void toFace(int face, SEAnimFinishListener listener, float step) {
        stopAllAnimation(null);
        float desAngle = 360 - face * 30;
        if (getUserRotate().getAngle() == desAngle) {
            if (listener != null) {
                listener.onAnimationfinish();
            }
            return;
        }
        mToFaceAnimation = AnimationFactory.createSetFace(this, face, step);
        mToFaceAnimation.setAnimFinishListener(listener);
        mToFaceAnimation.execute();
    }

    @Override
    public void stopAllAnimation(SEAnimFinishListener l) {
        if (mDeskVelocityAnimation != null) {
            mDeskVelocityAnimation.stop();
        }
        if (mToFaceAnimation != null) {
            mToFaceAnimation.stop();
        }
    }

    @Override
    public boolean handleBackKey(SEAnimFinishListener l) {
        if (getScene().getStatus(SEScene.STATUS_ON_DESK_SIGHT)) {
            getCamera().moveToWallSight(l);
        }
        return false;
    }

    public void show(SEAnimFinishListener l) {
        stopDeskAnimation();
        mShowDeskAnimation = AnimationFactory.createShowHideDockAnimation(this, findGround(), false);
        mShowDeskAnimation.setAnimFinishListener(l);
        mShowDeskAnimation.execute();
    }

    public void hide(SEAnimFinishListener l) {
        stopDeskAnimation();
        mHideDeskAnimation = AnimationFactory.createShowHideDockAnimation(this, findGround(), true);
        mHideDeskAnimation.setAnimFinishListener(l);
        mHideDeskAnimation.execute();
    }

    private void stopDeskAnimation() {
        if (mHideDeskAnimation != null) {
            mHideDeskAnimation.stop();
        }
        if (mShowDeskAnimation != null) {
            mShowDeskAnimation.stop();
        }
    }

    private SEObject findGround() {
        return getScene().findObject("Ground");
    }

    public void runACircle(SEAnimFinishListener listener) {
        mDisableTouch = true;
        final float height = -180f;
        getUserTransParas().mTranslate = new SEVector3f(0, 0, height);
        setUserTransParas();
        mRunACircleAnimation = AnimationFactory.createRunCircleAnimation(this, height);
        mRunACircleAnimation.setAnimFinishListener(listener);
        mRunACircleAnimation.execute();
    }

    @Override
    public void unlockScreen() {
        boolean disable = getStatus(SEScene.STATUS_APP_MENU) | getStatus(SEScene.STATUS_HELPER_MENU)
                | getStatus(SEScene.STATUS_OPTION_MENU) | getStatus(SEScene.STATUS_OBJ_MENU)
                | getStatus(SEScene.STATUS_ON_SKY_SIGHT) | getStatus(SEScene.STATUS_ON_WIDGET_SIGHT)
                | getStatus(SEScene.STATUS_ON_UNLOCK_ANIMATION)
                | getStatus(SEScene.STATUS_ON_WALL_DIALOG);
        if (!disable) {
            getScene().setStatus(SEScene.STATUS_ON_UNLOCK_ANIMATION, true);
            SEAnimFinishListener deskFinishListener = new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    getScene().setStatus(SEScene.STATUS_ON_UNLOCK_ANIMATION, false);
                    mDisableTouch = false;
                }
            };
            runACircle(deskFinishListener);
        }
    }

    private boolean getStatus(int type) {
        return getScene().getStatus(type);
    }

    @Override
    public CountAnimation createConflictAnimation(NormalObject conflictObject, ObjectSlot slot, int step) {
        return new ConflictAnimation(this, conflictObject, slot, step);
    }

    private static float getPositionAngle(SEVector3f position) {
        return (float) (position.getVectorZ().getAngle_II() * 180 / Math.PI);
    }

    private static SEVector3f getAnglePosition(DockObject dock, float angle) {
        float x = (float) (-dock.getBorderRadius() * Math.sin(angle * Math.PI / 180));
        float y = (float) (dock.getBorderRadius() * Math.cos(angle * Math.PI / 180));
        SEVector3f slotPosition = new SEVector3f(x, y, dock.getBorderHeight());
        return slotPosition;
    }

    private static class ConflictAnimation extends CountAnimation {
        private float mCurAngle;
        private float mEndAngle;
        private float mStep;
        private boolean mIsEnbaleBlending;
        private boolean mHasGetBlending;

        private DockObject mDock;
        private SEObject mConflictObject;

        public ConflictAnimation(DockObject dockObject, SEObject conflictObject,
                                 ObjectSlot NextSlot, float step) {
            super(dockObject.getScene());

            mDock = dockObject;
            mConflictObject = conflictObject;

            mStep = step;
            mCurAngle = getPositionAngle(mConflictObject.getUserTranslate());
            mEndAngle = NextSlot.mSlotIndex * 360.f / mDock.getCount();
            if (mEndAngle - mCurAngle > 180) {
                mEndAngle = mEndAngle - 360;
            } else if (mEndAngle - mCurAngle < -180) {
                mEndAngle = 360 + mEndAngle;
            }
        }

        @Override
        public void runPatch(int count) {
            float needTranslate = mEndAngle - mCurAngle;
            float absNTX = Math.abs(needTranslate);
            if (absNTX <= mStep) {
                mCurAngle = mEndAngle;
                stop();
            } else {
                int step = (int) (mStep * Math.sqrt(absNTX));
                if (needTranslate < 0) {
                    step = -step;
                }
                mCurAngle += step;
            }
            mConflictObject.setTranslate(getAnglePosition(mDock, mCurAngle), true);
        }

        @Override
        public void onFirstly(int count) {
            if (!mHasGetBlending) {
                mIsEnbaleBlending = mConflictObject.isBlendingable();
                mHasGetBlending = true;
            }
            if (!mIsEnbaleBlending) {
                mConflictObject.setBlendingable(true, true);
            }
            mConflictObject.setAlpha(0.2f, true);
        }

        @Override
        public void onFinish() {
            if (mHasGetBlending) {
                mConflictObject.setAlpha(1, true);
                if (!mIsEnbaleBlending) {
                    mConflictObject.setBlendingable(false, true);
                } else {
                    mConflictObject.setBlendingable(true, true);
                }
            }
        }

    }

    private ConflictAnimationTask mPlayConflictAnimationTask;

    public void playConflictAnimationTask(ConflictObject conflictObject, long delay, final SEAnimFinishListener postExecutor) {
        cancelConflictAnimationTask();
        if (conflictObject != null) {
            mPlayConflictAnimationTask = new ConflictAnimationTask(conflictObject, postExecutor);
            if (delay == 0) {
                mPlayConflictAnimationTask.run();
            } else {
                SELoadResThread.getInstance().process(mPlayConflictAnimationTask, delay);
            }
        }
    }

    public void cancelConflictAnimationTask() {
        if (mPlayConflictAnimationTask != null) {
            SELoadResThread.getInstance().cancel(mPlayConflictAnimationTask);
            mPlayConflictAnimationTask = null;
        }
    }

    private List<ConflictObject> mExistentSlot;
    public List<ConflictObject> getExistentSlot(SEObject movingObject) {
        if (null == mExistentSlot) {
            mExistentSlot = new ArrayList<ConflictObject>();
        } else {
            mExistentSlot.clear();
        }

        List<ConflictObject> fillSlots = mExistentSlot;
        for (SEObject object : mChildObjects) {
            if (object instanceof NormalObject) {
                NormalObject desktopObject = (NormalObject) object;
                ObjectInfo objInfo = desktopObject.getObjectInfo();
                if (objInfo.mSlotType == ObjectInfo.SLOT_TYPE_DESKTOP && !object.equals(movingObject)
                        && objInfo.getSlotIndex() >= 0) {
                    ConflictObject conflictObject = new ConflictObject(this, desktopObject, null);
                    fillSlots.add(conflictObject);
                }
            }
        }

        return fillSlots;
    }

    public ObjectSlot calculateNearestSlot(SEVector3f location, boolean handup) {
        ObjectSlot slot = null;
        if (mExistentSlot.size() == getCount()) {
            return null;
        }

        float r = location.getVectorZ().getLength();
        final float dockRadius = getBorderRadius();
        final float minRadius = 0.5f * dockRadius;
        final float maxRadius = minRadius + dockRadius;
        if ((r > minRadius && r < maxRadius) || handup) {
            float postitionAngle = (float) (location.getVectorZ().getAngle_II() * 180 / Math.PI);
            float deskAngle = getUserRotate().getAngle();
            float angle = postitionAngle - deskAngle;
            if (angle < 0) {
                angle = 360 + angle;
            }
            float perDeskangle = 360.f / getCount();
            slot = new ObjectSlot();
            float index = angle / perDeskangle;
            slot.mSlotIndex = Math.round(index);
            if (slot.mSlotIndex == getCount()) {
                slot.mSlotIndex = 0;
            }
        }
        return slot;
    }

    private List<ObjectSlot> searchEmptySlot(List<ConflictObject> existentSlot) {
        boolean[] slot = new boolean[getCount()];
        for (int i = 0; i < getCount(); i++) {
            slot[i] = true;
        }
        for (ConflictObject desktopObject : existentSlot) {
            slot[desktopObject.getSlotIndex()] = false;
        }

        List<ObjectSlot> objectSlots = null;
        for (int i = 0; i < getCount(); i++) {
            if (slot[i]) {
                if (objectSlots == null) {
                    objectSlots = new ArrayList<ObjectSlot>();
                }
                ObjectSlot objectSlot = new ObjectSlot();
                objectSlot.mSlotIndex = i;
                objectSlots.add(objectSlot);
            }
        }
        return objectSlots;
    }

    public ConflictObject getConflictSlot(ObjectSlot cmpSlot) {
        if (cmpSlot == null) {
            return null;
        }
        for (ConflictObject desktopObject : mExistentSlot) {
            if (desktopObject.getSlotIndex() == cmpSlot.mSlotIndex) {
                desktopObject.cloneSlot();
                List<ObjectSlot> emptySlots = searchEmptySlot(mExistentSlot);
                if (emptySlots != null) {
                    float minDistance = Float.MAX_VALUE;
                    for (ObjectSlot emptySlot : emptySlots) {
                        float distance = getSlotPosition(desktopObject.getConflictSlot())
                                .subtract(getSlotPosition(emptySlot)).getLength();
                        if (distance < minDistance) {
                            minDistance = distance;
                            desktopObject.setMovingSlotIndex(emptySlot.mSlotIndex);
                        }
                    }

                } else {
                    desktopObject.clearMovingSlot();
                }
                return desktopObject;
            }
        }
        return null;
    }

    /// conflict task end
}
