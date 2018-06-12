package com.borqs.se.widget3d;

import java.util.ArrayList;
import java.util.List;

import com.borqs.framework3d.home3d.*;
import com.borqs.framework3d.home3d.ConflictVesselObject.ConflictObject;
import com.borqs.se.engine.SEVector.SERay;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.ToastUtils;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEScene;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;
import com.borqs.se.engine.SEObject;

import android.util.Log;

public class DockLayerNew extends DockAbstractLayer {
    private static final String TAG = "DockLayerNew";
	private CountAnimation mMoveObjectAnimation;
    private CountAnimation mSetToRightPositionAnimation;
    private DockNew mDesk;
    private ObjectSlot mObjectSlot;
    // the following two members are just used by environment value 
    private SEVector3f mRealLocation;
    private SETransParas mObjectTransParas;

    
    public DockLayerNew(SEScene scene, VesselObject vesselObject) {
        super(scene, vesselObject);
        mDesk = (DockNew) getVesselObject();
    }

    @Override
    public boolean canHandleSlot(NormalObject object) {
        /*
        if (object.getObjectInfo().mSlotType == ObjectInfo.SLOT_TYPE_DESKTOP ||
                (object instanceof  TableFrame) ||
        	((object instanceof IconBox) && object.getObjectInfo().mSlotType == ObjectInfo.SLOT_TYPE_WALL_SHELF) ||
        	((object instanceof IconBox) && object.getObjectInfo().mSlotType == ObjectInfo.SLOT_TYPE_WALL)) {
            return true;
        }
        return false;
        */
        String name = object.mName;
        ArrayList<TypeManager.VesselType> types = TypeManager.getInstance().getObjectPlacements(name);
        if(types == null) {
            return false;
        }
        for(TypeManager.VesselType type : types) {
            if(type.getType() == ObjectInfo.SLOT_TYPE_DESKTOP) {
                return true;
            }
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
    private SEMountPointChain.ClosestMountPointData calculateNearestMountPoint(SEVector3f point)
    {
    	    NormalObject currentObject = getOnMoveObject();
    	    return mDesk.calculateNearestMountPoint(currentObject, point);
    }
    private NormalObject getConflickedObject(int slotIndex) {
    	    return mDesk.getConflickedObject(getOnMoveObject(), slotIndex);
    }

    private int mOriginSlot = -1;
    private void saveOriginSlot() {
        mOriginSlot = mObjectSlot.mSlotIndex;
    }
    private int getFinalSlot() {
        if (mDesk.isExchangedSlot(mObjectSlot.mSlotIndex)) {
            return mObjectSlot.mSlotIndex;
        } else {
            return mOriginSlot;
        }
    }

    public boolean onObjectMoveEvent(ACTION event, float x, float y) {
        stopMoveAnimation();
        updateRecycleStatus(event, x, y);
        // calculate object's move location
        setMovePoint((int) x, (int) y);
        //mRealLocation is in the reference frame of Dock
        SEMountPointChain.ClosestMountPointData closestMountPoint = calculateNearestMountPoint(mRealLocation);
        SEObject myConflickedObj = mDesk.getConflickedObject(getOnMoveObject(), closestMountPoint.mIndex);
        Log.i(TAG, "## conflicked object = " + myConflickedObj);
        Log.i(TAG, "## nearest slot index = " + closestMountPoint.mIndex);
        // calculate object's nearest slot on wall
        ObjectSlot objectSlot = calculateSlot();
        ConflictObject conflictSlot = null;
        /*
        if (!cmpSlot(objectSlot, mObjectSlot)) {
            conflictSlot = mDesk.getConflictSlot(objectSlot);
        }
        */
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
            SETransParas ttt = desTransParas.clone();
            ttt.mTranslate.mD[2] += 20;
            mSetToRightPositionAnimation = AnimationFactory.createSetPositionAnimation(getOnMoveObject(),
                    srcTransParas, ttt, 20);
            mSetToRightPositionAnimation.execute();
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
                saveOriginSlot();
                //playConflictAnimationTask(conflictSlot, 1000);
            }
            break;
        case MOVE:
        	    Log.i(TAG, " ## in MOVE state ##");
            getOnMoveObject().getUserTransParas().set(mObjectTransParas);
            getOnMoveObject().setUserTransParas();
            /*
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
                playConflictAnimationTask(conflictSlot, 400);
            }
            */
            break;
        case UP:
        	    Log.i(TAG, "## in UP state ##");
            cancelConflictAnimationTask();
            getOnMoveObject().getUserTransParas().set(mObjectTransParas);
            getOnMoveObject().setUserTransParas();
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
            }
            break;
        case FLY:
        	    Log.i(TAG, "## in FLY state ##");
            cancelConflictAnimationTask();
            getOnMoveObject().getUserTransParas().set(mObjectTransParas);
            getOnMoveObject().setUserTransParas();
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
            }
            break;
        case FINISH:
        	    Log.i(TAG, "## in FINISH state ##");
            cancelConflictAnimationTask();
            if (mInRecycle) {
                handleOutsideRoom();
            } else {
                mObjectSlot = mDesk.calculateNearestSlot(getOnMoveObject() ,mRealLocation, true);
                if (mObjectSlot == null) {
                    handleNoMoreRoom();
                    return true;
                }
                VesselLayer beginLayer = getOnMoveObject().getBeginLayer();
                if(beginLayer != null && beginLayer != this) {
                    beginLayer.leaveLayer(getOnMoveObject());
                }
                final int finalSlotIndex = getFinalSlot();
                conflictSlot = mDesk.getConflictSlot(getOnMoveObject(), finalSlotIndex);
                if (conflictSlot == null) {
                    cancelConflictAnimationTask();
                    onObjectMovementFinish(finalSlotIndex);
                } else {
                    playConflictAnimationTask(conflictSlot, 0, new SEAnimFinishListener() {
                        @Override
                        public void onAnimationfinish() {
                            onObjectMovementFinish(finalSlotIndex);
                        }
                    });

                }
                this.getExistentSlot();

            }
            break;
        }

        return true;
    }

    private void onObjectMovementFinish(int finalSlotIndex) {
        SEVector3f desLocation = mDesk.getSlotPosition( getOnMoveObject().mName, finalSlotIndex);
                getOnMoveObject().getObjectInfo().mObjectSlot.mSlotIndex = finalSlotIndex;
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

    //if return false the two slots are not equal
    //if return true the two slots are equal
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
            transParas.mTranslate = mDesk.getSlotPosition( objectInfo.mModelInfo.mName, objectInfo.mObjectSlot.mSlotIndex);
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
        return mDesk.calculateNearestSlot(getOnMoveObject(), mRealLocation, false);
    }

    @Override
    public void handleOutsideRoom() {
        VesselLayer beginLayer = getOnMoveObject().getBeginLayer();
        if(beginLayer != null && beginLayer != this) {
            beginLayer.leaveLayer(getOnMoveObject());
        }
        getOnMoveObject().handleOutsideRoom();
    }

    @Override
    public void handleNoMoreRoom() {
        ToastUtils.showNoDeskSpace();
        VesselLayer beginLayer = getOnMoveObject().getBeginLayer();
        if(beginLayer != null && beginLayer != getOnMoveObject().getRoot().getVesselLayer()
                && beginLayer != this) {
        	beginLayer.placeObjectToVessel(getOnMoveObject(), null);
        } else {
            getOnMoveObject().handleNoMoreRoom();
        }
    }

    @Override
    public void handleSlotSuccess() {
        super.handleSlotSuccess();
        NormalObject moveObject = getOnMoveObject();
        if (null != moveObject) {
            moveObject.getObjectInfo().mSlotType = ObjectInfo.SLOT_TYPE_DESKTOP;
            moveObject.handleSlotSuccess();
            moveObject.calculateNativeObjectTransParas();
        }
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
