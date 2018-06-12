package com.borqs.se.widget3d;

import java.util.List;

import com.borqs.framework3d.home3d.AnimationFactory;
import com.borqs.framework3d.home3d.DockAbstractLayer;
import com.borqs.framework3d.home3d.DockObject;
import com.borqs.framework3d.home3d.ConflictVesselObject.ConflictObject;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERay;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.ToastUtils;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;

public class DeskLayer extends DockAbstractLayer {
    private CountAnimation mMoveObjectAnimation;
    private CountAnimation mSetToRightPositionAnimation;
    private DockObject mDesk;
    private ObjectSlot mObjectSlot;
    private SEVector3f mRealLocation;
    private SETransParas mObjectTransParas;

    public DeskLayer(SEScene scene, VesselObject vesselObject) {
        super(scene, vesselObject);
        mDesk = (Desk) getVesselObject();
    }

    @Override
    public boolean canHandleSlot(NormalObject object) {
        if (object.getObjectInfo().mSlotType == ObjectInfo.SLOT_TYPE_DESKTOP) {
            return true;
        }
        return false;
    }

    @Override
    public boolean setOnLayerModel(NormalObject onMoveObject, boolean onLayerModel) {
        super.setOnLayerModel(onMoveObject, onLayerModel);
        if (onLayerModel) {
            mObjectSlot = null;
            mInRecycle = false;
            getExistentSlot();
        }
        return true;
    }

    public boolean onObjectMoveEvent(ACTION event, float x, float y) {
        stopMoveAnimation();
        updateRecycleStatus(event, x, y);
        // calculate object's move location
        setMovePoint((int) x, (int) y);
        // calculate object's nearest slot on wall
        ObjectSlot objectSlot = calculateSlot();
        ConflictObject conflictSlot = null;
        if (!cmpSlot(objectSlot, mObjectSlot)) {
            conflictSlot = mDesk.getConflictSlot(objectSlot);
        }
        switch (event) {
        case BEGIN:
            SETransParas srcTransParas = getOnMoveObject().getUserTransParas().clone();
            SETransParas desTransParas = mObjectTransParas.clone();
            if (getOnMoveObject().getObjectInfo().mIsNativeObject) {
                SETransParas localTrans = getOnMoveObject().getObjectInfo().mModelInfo.mLocalTrans;
                if (localTrans != null) {
                    desTransParas.mTranslate.selfSubtract(localTrans.mTranslate);
                }
            }
            mSetToRightPositionAnimation = AnimationFactory.createSetPositionAnimation(getOnMoveObject(),
                    srcTransParas, desTransParas, 5);
            mSetToRightPositionAnimation.execute();
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
                playConflictAnimationTask(conflictSlot, 1000, null);
            }
            break;
        case MOVE:
            getOnMoveObject().getUserTransParas().set(mObjectTransParas);
            getOnMoveObject().setUserTransParas();
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
                playConflictAnimationTask(conflictSlot, 400, null);
            }
            break;
        case UP:
            cancelConflictAnimationTask();
            getOnMoveObject().getUserTransParas().set(mObjectTransParas);
            getOnMoveObject().setUserTransParas();
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
            }
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
                mObjectSlot = mDesk.calculateNearestSlot(mRealLocation, true);
                if (mObjectSlot == null) {
                    handleNoMoreRoom();
                    return true;
                }
                conflictSlot = mDesk.getConflictSlot(mObjectSlot);
                if (conflictSlot == null) {
                    cancelConflictAnimationTask();
                    onObjectMovementFinish();
                } else {
                    playConflictAnimationTask(conflictSlot, 0, new SEAnimFinishListener() {
                        @Override

                        public void onAnimationfinish() {
                            onObjectMovementFinish();
                        }
                    });
                }

            }
            break;
        }

        return true;
    }

    private void onObjectMovementFinish() {
        SEVector3f desLocation = getSlotPosition(mObjectSlot);
        getOnMoveObject().getObjectInfo().mObjectSlot.mSlotIndex = mObjectSlot.mSlotIndex;
        SEVector3f srcLocation = wordLocationToDesk(getOnMoveObject().getUserTransParas().mTranslate);
        getOnMoveObject().changeParent(mDesk);
        getOnMoveObject().getUserTransParas().mTranslate = srcLocation;
        getOnMoveObject().getUserTransParas().mRotate.set(0, 0, 0, 1);
        getOnMoveObject().setUserTransParas();
        if (srcLocation.subtract(desLocation).getLength() > 1) {
            mMoveObjectAnimation = AnimationFactory.createMoveObjectAnimation(getOnMoveObject(), srcLocation,
                    desLocation, 3);
            mMoveObjectAnimation.setAnimFinishListener(new SEAnimFinishListener() {

                public void onAnimationfinish() {
                    handleSlotSuccess();
                }
            });
            mMoveObjectAnimation.execute();
        } else {
            handleSlotSuccess();
        }
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
        if (mMoveObjectAnimation != null) {
            mMoveObjectAnimation.stop();
        }
        if (mSetToRightPositionAnimation != null) {
            mSetToRightPositionAnimation.stop();
        }
    }

    private void setMovePoint(int touchX, int touchY) {
        SERay Ray = getScene().getCamera().screenCoordinateToRay(touchX, touchY);
//        mRealLocation = mDesk.getTouchLocation(ray);
//        mObjectTransParas = mDesk.getObjectTransParams(ray);
        mRealLocation = getTouchLocation(Ray, mDesk.getBorderHeight());
        SEVector3f touchLocation = getTouchLocation(Ray, mDesk.getBorderHeight() + 5);
        mObjectTransParas = new SETransParas();
        mObjectTransParas.mTranslate = touchLocation;
        mObjectTransParas.mRotate.set(mDesk.getUserRotate().getAngle(), 0, 0, 1);
    }

    private SEVector3f wordLocationToDesk(SEVector3f worldLocation) {
        float r = worldLocation.getVectorZ().getLength();
        float postitionAngle = (float) (worldLocation.getVectorZ().getAngle_II() * 180 / Math.PI);
        float preAngle = mDesk.getUserRotate().getAngle();
        float angle = postitionAngle - preAngle;
        float x = (float) (-r * Math.sin(angle * Math.PI / 180));
        float y = (float) (r * Math.cos(angle * Math.PI / 180));
        SEVector3f deskLocation = new SEVector3f(x, y, worldLocation.getZ());
        return deskLocation;
    }

    public SETransParas getSlotTransParas(ObjectInfo objectInfo) {
        SETransParas transParas = new SETransParas();
        if (objectInfo.mObjectSlot.mSlotIndex == -1) {
            transParas.mTranslate.set(0, 0, mDesk.getBorderHeight());

        } else {
            transParas.mTranslate = getSlotPosition(objectInfo.mObjectSlot);
        }
        return transParas;
    }

    private List<ConflictObject> getExistentSlot() {
        return mDesk.getExistentSlot(getOnMoveObject());
    }

    private ObjectSlot calculateSlot() {
        if (mInRecycle) {
            return null;
        }
        return mDesk.calculateNearestSlot(mRealLocation, false);
    }

    @Override
    public void handleOutsideRoom() {
        getOnMoveObject().handleOutsideRoom();
    }

    @Override
    public void handleNoMoreRoom() {
        ToastUtils.showNoDeskSpace();
        getOnMoveObject().handleNoMoreRoom();
    }

    @Override
    public void handleSlotSuccess() {
        super.handleSlotSuccess();
        getOnMoveObject().handleSlotSuccess();
    }

    private SEVector3f getSlotPosition(ObjectSlot objectSlot) {
        return mDesk.getSlotPosition(objectSlot);
    }

    void playConflictAnimationTask(ConflictObject conflictObject, long delay, final SEAnimFinishListener postExecutor) {
        mDesk.playConflictAnimationTask(conflictObject, delay, postExecutor);
    }

    void cancelConflictAnimationTask() {
        mDesk.cancelConflictAnimationTask();
    }

}
