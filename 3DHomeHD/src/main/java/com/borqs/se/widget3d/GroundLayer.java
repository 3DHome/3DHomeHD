package com.borqs.se.widget3d;

import java.util.ArrayList;
import java.util.List;

import com.borqs.framework3d.home3d.HouseObject;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERay;
import com.borqs.se.engine.SEVector.SEVector2f;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.ToastUtils;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SESceneInfo;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;

public class GroundLayer extends VesselLayer {
    private SetToRightPositionAnimation mSetToRightPositionAnimation;
    private List<ConflictObject> mExistentSlot;
    private ObjectSlot mObjectSlot;
    private SESceneInfo mSceneInfo;
    private SECamera mCamera;
    private HouseObject mHouse;

    private boolean mNeedRotateWall;
    private SEVector3f mRealLocation;
    private SETransParas mObjectTransParas;
    private float mVirtualWallRadius;
    private float mSkyRadius;
    private ACTION mPreAction;

    public GroundLayer(SEScene scene, VesselObject vesselObject) {
        super(scene, vesselObject);
        mHouse = (House) getVesselObject();
        mSceneInfo = getScene().mSceneInfo;
        mCamera = getScene().getCamera();
        mVirtualWallRadius = mHouse.getWallRadius() * 0.9f;
        mSkyRadius = mHouse.getWallRadius() * 0.3f;
    }

    @Override
    public boolean setOnLayerModel(NormalObject onMoveObject, boolean onLayerModel) {
        super.setOnLayerModel(onMoveObject, onLayerModel);
        if (onLayerModel) {
            mObjectSlot = null;
            mExistentSlot = getExistentSlot();
            mInRecycle = false;
        } else {
            cancelRotation();
            mHouse.toNearestFace(null, 5);
        }
        return true;
    }

    public boolean onObjectMoveEvent(ACTION event, float x, float y) {
        stopMoveAnimation();
        updateRecycleStatus(event, x, y);
        setMovePoint((int) x, (int) y);
        ObjectSlot objectSlot = calculateSlot();
        ConflictObject conflictSlot = null;
        if (!cmpSlot(objectSlot, mObjectSlot)) {
            conflictSlot = getConflictSlot(objectSlot);
        }
        switch (event) {
        case BEGIN:
            mPreAction = ACTION.BEGIN;
            mNeedRotateWall = false;
            SETransParas srcTransParas = getOnMoveObject().getUserTransParas().clone();
            SETransParas desTransParas = mObjectTransParas.clone();
            if (getOnMoveObject().getObjectInfo().mIsNativeObject) {
                SETransParas localTrans = getOnMoveObject().getObjectInfo().mModelInfo.mLocalTrans;
                if (localTrans != null) {
                    desTransParas.mTranslate.selfSubtract(localTrans.mTranslate);
                }
            }
            mSetToRightPositionAnimation = new SetToRightPositionAnimation(getScene(), getOnMoveObject(),
                    srcTransParas, desTransParas, 5);
            mSetToRightPositionAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    if (!mNeedRotateWall) {
                        calculationWallRotation(800);
                    }
                }
            });
            mSetToRightPositionAnimation.execute();
            mNeedRotateWall = true;
            float toFace = mHouse.getWallNearestIndex();
            if (mRealLocation.getX() < 0) {
                toFace = toFace + 0.5f;
            } else {
                toFace = toFace - 0.5f;
            }

            mHouse.toFace(toFace, new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    ObjectSlot objectSlot = calculateSlot();
                    if (!cmpSlot(objectSlot, mObjectSlot)) {
                        mObjectSlot = objectSlot;
                        ConflictObject conflictSlot = getConflictSlot(objectSlot);
                        playConflictAnimationTask(conflictSlot, 300);
                    }
                    if (mPreAction == ACTION.UP) {
                        cancelRotation();
                    } else {
                        calculationWallRotation(800);
                    }
                }
            }, 5);
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
                playConflictAnimationTask(conflictSlot, 300);
            }
            break;
        case MOVE:
            mPreAction = ACTION.MOVE;
            getOnMoveObject().getUserTransParas().set(mObjectTransParas);
            getOnMoveObject().setUserTransParas();
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
                playConflictAnimationTask(conflictSlot, 300);
            }
            if (!mNeedRotateWall) {
                calculationWallRotation(500);
            }
            break;
        case UP:
            mPreAction = ACTION.UP;
            cancelConflictAnimationTask();
            getOnMoveObject().getUserTransParas().set(mObjectTransParas);
            getOnMoveObject().setUserTransParas();
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
            }
            cancelRotation();
            int index = mHouse.getCurrentFaceIndex();
            if (index != 0) {
                index = mHouse.getCount() - index;
            }
            float face = index - 0.5f;
            if (face < 0) {
                face = face + mHouse.getCount();
            }
            mHouse.toFace(face, null, 5);

            break;
        case FLY:
            cancelConflictAnimationTask();
            getOnMoveObject().getUserTransParas().set(mObjectTransParas);
            getOnMoveObject().setUserTransParas();
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
            }
            break;
        case FINISH:
            cancelConflictAnimationTask();
            if (mInRecycle) {
                handleOutsideRoom();
            } else {
                mObjectSlot = calculateNearestSlot(mRealLocation, true);
                if (mObjectSlot == null) {
                    handleNoMoreRoom();
                    return true;
                }
                conflictSlot = getConflictSlot(mObjectSlot);
                playConflictAnimationTask(conflictSlot, 0);
                stopMoveAnimation();
                playSlotAnimation(mObjectSlot, null);

            }
            break;
        }
        return true;
    }

    /**
     * Calculate whether object is in rubbish box and update the box's color
     */
    private void updateRecycleStatus(ACTION event, float x, float y) {
        boolean force = false;
        switch (event) {
        case BEGIN:
            force = false;
            break;
        case MOVE:
            force = false;
            break;
        case UP:
            force = true;
            break;
        case FLY:
            force = true;
            break;
        case FINISH:
            force = true;
            break;
        }

        if (x >= mBoundOfRecycle.left && x <= mBoundOfRecycle.right && y <= mBoundOfRecycle.bottom
                && y >= mBoundOfRecycle.top) {
            mInRecycle = true;
        } else {
            if (!force) {
                mInRecycle = false;
            }
        }
    }

    private boolean cmpSlot(ObjectSlot objectSlot1, ObjectSlot objectSlot2) {
        if (objectSlot1 == null && objectSlot2 == null) {
            return true;
        }
        if ((objectSlot1 != null && objectSlot2 == null) || (objectSlot1 == null && objectSlot2 != null)) {
            return false;
        }
        return objectSlot1.equals(objectSlot2);
    }

    public void stopMoveAnimation() {
        if (mSetToRightPositionAnimation != null) {
            mSetToRightPositionAnimation.stop();
        }
    }

    public void setMovePoint(int touchX, int touchY) {
        SERay ray = mCamera.screenCoordinateToRay(touchX, touchY);
        mRealLocation = rayCrossWall(ray, mHouse.getWallRadius()).mTranslate;
        mObjectTransParas = getObjectTransParas(ray);
    }

    private SETransParas getObjectTransParas(SERay ray) {
        SETransParas transParas = rayCrossWall(ray, mVirtualWallRadius);
        int minZ = 0;
        int maxZ = (int) (mHouse.getWallHeight() / 2);
        if (transParas.mTranslate.getZ() < minZ) {
            transParas.mTranslate.mD[2] = minZ;
        } else if (transParas.mTranslate.getZ() > maxZ) {
            transParas.mTranslate = rayCrossCylinder(ray, mSkyRadius);
            SEVector2f touchLocZ = transParas.mTranslate.getVectorZ();
            double angle = touchLocZ.getAngle_II();
            transParas.mRotate.set((float) (angle * 180 / Math.PI), 0, 0, 1);
            float scale = (mSkyRadius + mCamera.getRadius()) / (mVirtualWallRadius + mCamera.getRadius());
            transParas.mScale.set(scale, scale, scale);
        }
        return transParas;
    }

    private SETransParas rayCrossWall(SERay ray, float wallRadius) {
        // ray cross the front wall
        SETransParas transParas = new SETransParas();
        float y = wallRadius;
        float para = (y - ray.getLocation().getY()) / ray.getDirection().getY();
        transParas.mTranslate = ray.getLocation().add(ray.getDirection().mul(para));
        float faceAngle = mHouse.getFaceAngle();
        float tanAngle = (float) Math.tan(faceAngle * Math.PI / 360);
        float halfFaceW = wallRadius * tanAngle;
        if (transParas.mTranslate.getX() < -halfFaceW) {
            // ray cross the left wall
            float Xa = ray.getLocation().getX();
            float Ya = ray.getLocation().getY();
            float Xb = ray.getDirection().getX();
            float Yb = ray.getDirection().getY();
            para = (tanAngle * Xa + tanAngle * halfFaceW + wallRadius - Ya) / (Yb - tanAngle * Xb);
            transParas.mTranslate = ray.getLocation().add(ray.getDirection().mul(para));
            transParas.mRotate.set(faceAngle, 0, 0, 1);
        } else if (transParas.mTranslate.getX() > halfFaceW) {
            // ray cross the right wall
            float Xa = ray.getLocation().getX();
            float Ya = ray.getLocation().getY();
            float Xb = ray.getDirection().getX();
            float Yb = ray.getDirection().getY();
            para = (-tanAngle * Xa + tanAngle * halfFaceW + wallRadius - Ya) / (Yb + tanAngle * Xb);
            transParas.mTranslate = ray.getLocation().add(ray.getDirection().mul(para));
            transParas.mRotate.set(-faceAngle, 0, 0, 1);
        }
        return transParas;

    }

    private SEVector3f rayCrossCylinder(SERay ray, float radius) {
        float Xa = ray.getLocation().getX();
        float Ya = ray.getLocation().getY();
        float Xb = ray.getDirection().getX();
        float Yb = ray.getDirection().getY();
        float a = Xb * Xb + Yb * Yb;
        float b = 2 * (Xa * Xb + Ya * Yb);
        float c = Xa * Xa + Ya * Ya - radius * radius;
        float para = (float) ((-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a));
        SEVector3f touchLoc = ray.getLocation().add(ray.getDirection().mul(para));
        return touchLoc;
    }

    private ObjectSlot calculateSlot() {
        if (onRight() || onLeft() || mInRecycle) {
            return null;
        }
        return calculateNearestSlot(mRealLocation, false);
    }

    private ObjectSlot calculateNearestSlot(SEVector3f location, boolean handup) {
        ObjectSlot slot = null;
        if (mExistentSlot.size() == mHouse.getCount()) {
            return slot;
        }
        float wallH = mHouse.getHouseHeight() + mHouse.getWallHeight();
        float wallW = mHouse.getHouseWidth();
        int index = mHouse.getCurrentFaceIndex();
        if (index != 0) {
            index = mHouse.getCount() - index;
        }
        if ((location.getZ() < wallH / 5 && Math.abs(location.getX()) < wallW / 5) || handup) {
            slot = new ObjectSlot();
            slot.mSlotIndex = index - 1;
            if (slot.mSlotIndex < 0) {
                slot.mSlotIndex = slot.mSlotIndex + mHouse.getCount();
            }
        }

        return slot;
    }

    private void playConflictAnimationTask(ConflictObject conflictObject, long delay) {
        cancelConflictAnimationTask();
        if (conflictObject != null) {
            mPlayConflictAnimationTask = new ConflictAnimationTask(conflictObject);
            if (delay == 0) {
                mPlayConflictAnimationTask.run();
            } else {
                SELoadResThread.getInstance().process(mPlayConflictAnimationTask, delay);
            }
        }
    }

    private void cancelConflictAnimationTask() {
        if (mPlayConflictAnimationTask != null) {
            SELoadResThread.getInstance().cancel(mPlayConflictAnimationTask);
            mPlayConflictAnimationTask = null;
        }
    }

    private class ConflictAnimationTask implements Runnable {
        private ConflictObject mMyConflictObject;

        public ConflictAnimationTask(ConflictObject conflictObject) {
            mMyConflictObject = conflictObject;
        }

        public void run() {
            mMyConflictObject.playConflictAnimation();

        }
    }

    private ConflictAnimationTask mPlayConflictAnimationTask;

    private List<ObjectSlot> searchEmptySlot(List<ConflictObject> existentSlot) {
        final int count = mHouse.getCount();
        boolean[] slot = new boolean[count];
        for (int i = 0; i < count; i++) {
            slot[i] = true;
        }
        for (ConflictObject wallGapObject : existentSlot) {
            slot[wallGapObject.mConflictObject.getObjectInfo().getSlotIndex()] = false;
        }

        List<ObjectSlot> objectSlots = null;
        for (int i = 0; i < count; i++) {
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

    private SEVector3f getSlotPosition(ObjectSlot objectSlot) {
        float angle = (objectSlot.mSlotIndex + 0.5f) * mHouse.getFaceAngle();
        SEVector2f yDirection = new SEVector2f((float) Math.cos((angle + 90) * Math.PI / 180),
                (float) Math.sin((angle + 90) * Math.PI / 180));
        float offsetY = mHouse.getWallRadius();
        SEVector2f offset = yDirection.mul(offsetY);
        return new SEVector3f(offset.getX(), offset.getY(), 0);
    }

    private Runnable mRotateToNextFace = new Runnable() {
        public void run() {
            new SECommand(getScene()) {
                public void run() {
                    if (onLeft()) {
                        mHouse.toLeftHalfFace(new SEAnimFinishListener() {
                            public void onAnimationfinish() {
                                ObjectSlot objectSlot = calculateSlot();
                                if (!cmpSlot(objectSlot, mObjectSlot)) {
                                    mObjectSlot = objectSlot;
                                    ConflictObject conflictSlot = getConflictSlot(objectSlot);
                                    playConflictAnimationTask(conflictSlot, 1000);
                                }
                                if (mPreAction == ACTION.UP) {
                                    cancelRotation();
                                } else {
                                    calculationWallRotation(800);
                                }
                            }
                        }, 5);
                    } else if (onRight()) {
                        mHouse.toRightHalfFace(new SEAnimFinishListener() {
                            public void onAnimationfinish() {
                                ObjectSlot objectSlot = calculateSlot();
                                if (!cmpSlot(objectSlot, mObjectSlot)) {
                                    mObjectSlot = objectSlot;
                                    ConflictObject conflictSlot = getConflictSlot(objectSlot);
                                    playConflictAnimationTask(conflictSlot, 1000);
                                }
                                if (mPreAction == ACTION.UP) {
                                    cancelRotation();
                                } else {
                                    calculationWallRotation(800);
                                }
                            }
                        }, 5);
                    } else {
                        mNeedRotateWall = false;
                    }

                }
            }.execute();
        }
    };

    private void calculationWallRotation(long delayTime) {
        if (onLeft() || onRight()) {
            mNeedRotateWall = true;
            SELoadResThread.getInstance().process(mRotateToNextFace, delayTime);
        } else {
            cancelRotation();
        }
    }

    private boolean onLeft() {
        int screenW = mCamera.getWidth();
        return getOnMoveObject().getTouchX() < screenW * 0.1f;
    }

    private boolean onRight() {
        int screenW = mCamera.getWidth();
        return getOnMoveObject().getTouchX() > screenW * 0.9f;
    }

    private void cancelRotation() {
        mNeedRotateWall = false;
        SELoadResThread.getInstance().cancel(mRotateToNextFace);
        mHouse.stopAllAnimation(null);
    }

    private ConflictObject getConflictSlot(ObjectSlot cmpSlot) {
        if (cmpSlot == null) {
            return null;
        }
        for (ConflictObject wallGapObject : mExistentSlot) {
            if (wallGapObject.mConflictObject.getObjectInfo().getSlotIndex() == cmpSlot.mSlotIndex) {
                wallGapObject.mMoveSlot = wallGapObject.mConflictObject.getObjectInfo().mObjectSlot.clone();
                List<ObjectSlot> emptySlots = searchEmptySlot(mExistentSlot);
                if (emptySlots != null) {
                    float minDistance = Float.MAX_VALUE;
                    for (ObjectSlot emptySlot : emptySlots) {
                        float distance = getSlotPosition(wallGapObject.mConflictObject.getObjectInfo().mObjectSlot)
                                .subtract(getSlotPosition(emptySlot)).getLength();
                        if (distance < minDistance) {
                            minDistance = distance;
                            wallGapObject.mMoveSlot.mSlotIndex = emptySlot.mSlotIndex;
                        }
                    }
                } else {
                    wallGapObject.mMoveSlot = null;
                }
                return wallGapObject;
            }
        }
        return null;
    }

    private List<ConflictObject> getExistentSlot() {
        List<ConflictObject> fillSlots = new ArrayList<ConflictObject>();
        for (SEObject object : mHouse.mChildObjects) {
            if (object instanceof NormalObject) {
                NormalObject wallObject = (NormalObject) object;
                ObjectInfo objInfo = wallObject.getObjectInfo();
                if (objInfo.mSlotType == ObjectInfo.SLOT_TYPE_WALL_GAP && !object.equals(getOnMoveObject())) {
                    ConflictObject conflictObject = new ConflictObject();
                    conflictObject.mConflictObject = wallObject;
                    fillSlots.add(conflictObject);
                }
            }
        }
        return fillSlots;
    }

    private SETransParas worldToWall(SETransParas worldTransParas) {
        SETransParas wallTransParas = new SETransParas();
        SEVector2f touchLocZ = worldTransParas.mTranslate.getVectorZ();
        float objectToWorldAngle = (float) (touchLocZ.getAngle_II() * 180 / Math.PI);
        float wallToWorldAngle = mHouse.getWallAngle();
        float objectLocationAngle = objectToWorldAngle - wallToWorldAngle;
        float r = touchLocZ.getLength();
        float x = (float) (-r * Math.sin(objectLocationAngle * Math.PI / 180));
        float y = (float) (r * Math.cos(objectLocationAngle * Math.PI / 180));
        float z = worldTransParas.mTranslate.getZ();
        float objectToWallAngle = worldTransParas.mRotate.getAngle() - wallToWorldAngle;
        wallTransParas.mTranslate.set(x, y, z);
        wallTransParas.mRotate.set(objectToWallAngle, 0, 0, 1);
        wallTransParas.mScale = worldTransParas.mScale.clone();
        return wallTransParas;
    }

    private void playSlotAnimation(ObjectSlot wallSlot, final SEAnimFinishListener l) {
        getOnMoveObject().changeParent(mHouse);
        final SETransParas srcTransParas = worldToWall(getOnMoveObject().getUserTransParas());
        getOnMoveObject().getUserTransParas().set(srcTransParas);
        getOnMoveObject().setUserTransParas();
        getOnMoveObject().getObjectInfo().mObjectSlot.mSlotIndex = wallSlot.mSlotIndex;
        final SETransParas desTransParas = mHouse.getSlotTransParas(getOnMoveObject().getObjectInfo(), getOnMoveObject());
        mHouse.toFace(getOnMoveObject().getObjectInfo().getSlotIndex() + 0.5f, new SEAnimFinishListener() {
            public void onAnimationfinish() {
                stopMoveAnimation();
                mSetToRightPositionAnimation = new SetToRightPositionAnimation(getScene(), getOnMoveObject(),
                        srcTransParas, desTransParas, 7);
                mSetToRightPositionAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                    public void onAnimationfinish() {
                        handleSlotSuccess();
                        if (l != null) {
                            l.onAnimationfinish();
                        }
                    }
                });
                mSetToRightPositionAnimation.execute();
            }
        }, 5);
    }

    @Override
    public void handleOutsideRoom() {
        getOnMoveObject().handleOutsideRoom();
    }

    @Override
    public void handleNoMoreRoom() {
        ToastUtils.showNoWallSpace();
        getOnMoveObject().handleNoMoreRoom();
    }

    @Override
    public void handleSlotSuccess() {
        super.handleSlotSuccess();
        getOnMoveObject().handleSlotSuccess();
    }

    private class SetToRightPositionAnimation extends CountAnimation {
        private SETransParas mSrcTransParas;
        private SETransParas mDesTransParas;
        private NormalObject mObject;
        private int mCount;
        private float mStep;

        public SetToRightPositionAnimation(SEScene scene, NormalObject obj, SETransParas srcTransParas,
                SETransParas desTransParas, int count) {
            super(scene);
            mObject = obj;
            mSrcTransParas = srcTransParas;
            mDesTransParas = desTransParas;
            mCount = count;
        }

        public void runPatch(int count) {
            float step = mStep * count;
            mObject.getUserTransParas().mTranslate = mSrcTransParas.mTranslate.add(mDesTransParas.mTranslate.subtract(
                    mSrcTransParas.mTranslate).mul(step));
            mObject.getUserTransParas().mScale = mSrcTransParas.mScale.add(mDesTransParas.mScale.subtract(
                    mSrcTransParas.mScale).mul(step));
            float desAngle = mDesTransParas.mRotate.getAngle();
            float srcAngle = mSrcTransParas.mRotate.getAngle();
            if (desAngle - srcAngle > 180) {
                desAngle = desAngle - 360;
            } else if (desAngle - srcAngle < -180) {
                desAngle = desAngle + 360;
            }
            float curAngle = srcAngle + (desAngle - srcAngle) * step;
            mObject.getUserTransParas().mRotate.set(curAngle, 0, 0, 1);
            mObject.setUserTransParas();
        }

        @Override
        public void onFirstly(int count) {
            mStep = 1f / getAnimationCount();
        }

        @Override
        public int getAnimationCount() {
            return mCount;
        }
    }

    private class ConflictObject {
        public ConflictAnimation mConflictAnimation;
        public NormalObject mConflictObject;
        public ObjectSlot mMoveSlot;

        // confirm to next position
        private void playConflictAnimation() {
            if (mConflictAnimation != null) {
                mConflictAnimation.stop();
            }
            if (mMoveSlot == null) {
                return;
            }
            mConflictObject.getObjectSlot().set(mMoveSlot);
            mConflictAnimation = new ConflictAnimation(getScene(), 3);
            mConflictAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    mConflictObject.getObjectInfo().updateSlotDB();
                }
            });
            mConflictAnimation.execute();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            ConflictObject cmp = (ConflictObject) obj;
            return (mConflictObject.equals(cmp.mConflictObject));
        }

        private class ConflictAnimation extends CountAnimation {

            private SETransParas mDesTransParas;
            private SETransParas mSrcTransParas;
            private float mStep;
            private float mCurProcess;
            private boolean mIsEnbaleBlending;
            private boolean mHasGetBlending;

            public ConflictAnimation(SEScene scene, float step) {
                super(scene);
                mDesTransParas = mHouse.getSlotTransParas(mConflictObject.getObjectInfo(), mConflictObject);
                mSrcTransParas = mConflictObject.getUserTransParas().clone();
                mStep = step;
                mCurProcess = 0;
            }

            @Override
            public void runPatch(int count) {
                float needTranslate = 100 - mCurProcess;
                float absNTX = Math.abs(needTranslate);
                if (absNTX <= mStep) {
                    mCurProcess = 100;
                    stop();
                } else {
                    int step = (int) (mStep * Math.sqrt(absNTX));
                    if (needTranslate < 0) {
                        step = -step;
                    }
                    mCurProcess = mCurProcess + step;

                }
                float step = mCurProcess / 100;
                mConflictObject.getUserTransParas().mTranslate = mSrcTransParas.mTranslate
                        .add(mDesTransParas.mTranslate.subtract(mSrcTransParas.mTranslate).mul(step));
                mConflictObject.getUserTransParas().mScale = mSrcTransParas.mScale.add(mDesTransParas.mScale.subtract(
                        mSrcTransParas.mScale).mul(step));
                float desAngle = mDesTransParas.mRotate.getAngle();
                float srcAngle = mSrcTransParas.mRotate.getAngle();
                if (desAngle - srcAngle > 180) {
                    desAngle = desAngle - 360;
                } else if (desAngle - srcAngle < -180) {
                    desAngle = desAngle + 360;
                }
                float curAngle = srcAngle + (desAngle - srcAngle) * step;
                mConflictObject.getUserTransParas().mRotate.set(curAngle, 0, 0, 1);
                mConflictObject.setUserTransParas();
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
                mConflictObject.setAlpha(0.1f, true);
            }

            @Override
            public void onFinish() {
                mConflictObject.getUserTransParas().set(mDesTransParas);
                mConflictObject.setUserTransParas();
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
    }

}
