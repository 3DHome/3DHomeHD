package com.borqs.se.widget3d;

import android.util.Log;
import com.borqs.framework3d.home3d.AnimationFactory;
import com.borqs.framework3d.home3d.ConflictVesselObject.ConflictObject;
import com.borqs.framework3d.home3d.SEMountPointChain;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.ToastUtils;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEScene;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;
import com.borqs.framework3d.home3d.SEDebug;
import java.util.List;

public class WallShelfLayer extends VesselLayer {
    private static final String TAG = "WallShelfLayer";
	private CountAnimation mMoveObjectAnimation;
    private CountAnimation mSetToRightPositionAnimation;
    private WallShelf mWallShelf;
    private ObjectSlot mObjectSlot;
    // the following two members are just used by environment value
    private SEVector3f mRealLocation;
    private SETransParas mObjectTransParas;


    public WallShelfLayer(SEScene scene, VesselObject vesselObject) {
        super(scene, vesselObject);
        mWallShelf = (WallShelf) getVesselObject();
    }

    @Override
    public boolean canHandleSlot(NormalObject object) {
        if (object instanceof IconBox || object instanceof AppObject) {
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
    private SEMountPointChain.ClosestMountPointData calculateNearestMountPoint(SEVector3f point)
    {
    	    NormalObject currentObject = getOnMoveObject();
    	    return mWallShelf.calculateNearestMountPoint(currentObject, point);
    }

    public boolean onObjectMoveEvent(ACTION event, SEVector3f worldLocation) {
    	NormalObject conflictObject = null;
        switch (event) {
        case BEGIN:
        {
        	//mWallShelf.getExistentSlot(getOnMoveObject());
        }
            break;
        case MOVE:
        {
        	
        }
            break;
        case UP:
        {}
            break;
        case FLY:
        {}
            break;
        case FINISH:
            cancelConflictAnimationTask();
            if (mInRecycle) {
                handleOutsideRoom();
            } else {
                int currentMountPointIndex = mWallShelf.calculateNearestMountPointIndex(getOnMoveObject() ,worldLocation, true);
                if (currentMountPointIndex == -1) {
                    handleNoMoreRoom();
                    return false;
                }
                
                conflictObject = mWallShelf.getConflictedObject(getOnMoveObject(), currentMountPointIndex);
                Log.i(TAG, "## current layer = " + mWallShelf.mIndex + " ###");
                boolean placeObjectOK = true;
                if (conflictObject == null) {
                    cancelConflictAnimationTask();
                } else {
                    //playConflictAnimationTask(conflictObject, 0);
                    placeObjectOK = this.placeObjectOnMountPoint(conflictObject, currentMountPointIndex,
                    		                     getOnMoveObject().getObjectInfo().mSlotType,
                    		                     getOnMoveObject().getObjectSlot().mSlotIndex, 
                    		                     getOnMoveObject().getObjectSlot().mMountPointIndex);
                }
                if(placeObjectOK == false)
                	return false;
                getOnMoveObject().getObjectInfo().mSlotType = ObjectInfo.SLOT_TYPE_WALL_SHELF;
                SEVector3f desLocation = mWallShelf.getSlotPosition( getOnMoveObject().mName, currentMountPointIndex);
                getOnMoveObject().getObjectInfo().mObjectSlot.mMountPointIndex = currentMountPointIndex;
                getOnMoveObject().getObjectInfo().mObjectSlot.mSlotIndex = mWallShelf.mIndex;
                SEVector3f srcLocation = mWallShelf.toObjectPoint(worldLocation);
                //desLocation.mD[1] = srcLocation.mD[1];
                getOnMoveObject().changeParent(mWallShelf);
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
            break;
        }
        return true;
    }
    private boolean placeObjectOnMountPoint(NormalObject conflictObject, 
    		                                int conflictObjectMountPointIndex, 
    		                                int moveObjectSlotType, 
    		                                int moveObjectSlotIndex, int moveObjectMountPointIndex) {
    	SEVector3f desLocation = null;
    	if(moveObjectSlotType != ObjectInfo.SLOT_TYPE_WALL_SHELF) {
    		int emptyMountPointIndex = mWallShelf.getMountPointIndexExceptDest(conflictObject, conflictObjectMountPointIndex);
    		if(emptyMountPointIndex == -1) {
    			return false;
    		}
    		desLocation = mWallShelf.getSlotPosition(conflictObject.mName, emptyMountPointIndex);
    	} else {
    	    if(moveObjectSlotIndex == mWallShelf.mIndex) {
    	        desLocation = mWallShelf.getSlotPosition( conflictObject.mName, moveObjectMountPointIndex);
    	    } else {
    		    SEObject parent = getScene().findObject(mWallShelf.mName, moveObjectSlotIndex);
    		    if((parent instanceof WallShelf) == false) {
    		    	Log.i(TAG, "## parent is not wallshelf ##");
    		    }
    		    conflictObject.changeParent(parent);
    		    WallShelf parentShelf = (WallShelf)parent;
    		    desLocation = parentShelf.getSlotPosition(conflictObject.mName, moveObjectMountPointIndex);
        	}
    	}
        conflictObject.getObjectInfo().mObjectSlot.mSlotIndex = moveObjectSlotIndex;
        conflictObject.getObjectInfo().mObjectSlot.mMountPointIndex = moveObjectMountPointIndex;
        conflictObject.getUserTransParas().mTranslate = desLocation;
        conflictObject.getUserTransParas().mRotate.set(0, 0, 0, 1);
        conflictObject.setUserTransParas();
        return true;
    }
    @Override
    public boolean placeObjectToVessel(NormalObject normalObject, SEAnimFinishListener l) {
    	SEDebug.myAssert(normalObject.getObjectInfo().mSlotType == ObjectInfo.SLOT_TYPE_WALL_SHELF, "must be WALL_SHELF");
        boolean b = this.placeObjectOnMountPoint(normalObject, -1, normalObject.getObjectInfo().mSlotType, 
        		                     normalObject.getObjectSlot().mSlotIndex, normalObject.getObjectSlot().mMountPointIndex);
        return b;
    }
    public boolean onObjectMoveEvent(ACTION event, float x, float y) {
        stopMoveAnimation();
        updateRecycleStatus(event, x, y);
        // calculate object's move location
        setMovePoint((int) x, (int) y);
        //mRealLocation is in the reference frame of Dock
        SEMountPointChain.ClosestMountPointData closestMountPoint = calculateNearestMountPoint(mRealLocation);
        SEObject myConflickedObj = mWallShelf.getConflictedObject(getOnMoveObject(), closestMountPoint.mIndex);
        Log.i(TAG, "## wall shelf conflicked object = " + myConflickedObj);
        if(closestMountPoint != null) {
            Log.i(TAG, "## wall shelf nearst slot t = " + closestMountPoint.mMPD.getTranslate());
            Log.i(TAG, "## wall shelf nearest slot index = " + closestMountPoint.mIndex);
        }
        // calculate object's nearest slot on wall
        ObjectSlot objectSlot = calculateSlot();
        ConflictObject conflictSlot = null;
        /*
        if (!cmpSlot(objectSlot, mObjectSlot)) {
            conflictSlot = mWallShelf.getConflictSlot(objectSlot);
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
                //playConflictAnimationTask(conflictSlot, 1000);
            }
            mWallShelf.getExistentSlot(getOnMoveObject());
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
                int mountPointIndex = mWallShelf.calculateNearestMountPointIndex(getOnMoveObject() ,mRealLocation, true);
                if (mObjectSlot == null) {
                    handleNoMoreRoom();
                    return true;
                }
                
                conflictSlot = mWallShelf.getConflictSlot(getOnMoveObject(), mObjectSlot.mSlotIndex);
                if (conflictSlot == null) {
                    cancelConflictAnimationTask();
                } else {
                    //playConflictAnimationTask(conflictSlot, 0);
                    
                }
                SEVector3f desLocation = mWallShelf.getSlotPosition( getOnMoveObject().mName,mObjectSlot.mSlotIndex);
                getOnMoveObject().getObjectInfo().mObjectSlot.mSlotIndex = mObjectSlot.mSlotIndex;
                SEVector3f srcLocation = worldLocationToDesk(getOnMoveObject().getUserTransParas().mTranslate);
                getOnMoveObject().changeParent(mWallShelf);
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
    	House house = (House)mWallShelf.getParent();
    	SEVector3f location = house.getFingerLocation(touchX, touchY);
    	float radius = house.getWallRadius() * 0.8f;
    	
        //SERay Ray = getScene().getCamera().screenCoordinateToRay(touchX, touchY);
        mRealLocation = location;//getTouchLocation(Ray, mWallShelf.getBorderHeight());
        //SEVector3f touchLocation = getTouchLocation(Ray, mWallShelf.getBorderHeight() + 5);
        mObjectTransParas = new SETransParas();
        mObjectTransParas.mTranslate = new SEVector3f(location.getX(), location.getY() - 60, location.getZ());
        mObjectTransParas.mRotate.set(mWallShelf.getUserRotate().getAngle(), 0, 0, 1);
    }

    private SEVector3f worldLocationToDesk(SEVector3f worldLocation) {
        float r = worldLocation.getVectorZ().getLength();
        float postitionAngle = (float) (worldLocation.getVectorZ().getAngle_II() * 180 / Math.PI);
        float preAngle = mWallShelf.getUserRotate().getAngle();
        float angle = postitionAngle - preAngle;
        float x = (float) (-r * Math.sin(angle * Math.PI / 180));
        float y = (float) (r * Math.cos(angle * Math.PI / 180));
        SEVector3f deskLocation = new SEVector3f(x, y, worldLocation.getZ());
        return deskLocation;
    }

    public SETransParas getSlotTransParas(ObjectInfo objectInfo) {
        SETransParas transParas = new SETransParas();
        if (objectInfo.mObjectSlot.mSlotIndex == -1) {
            transParas.mTranslate.set(0, 0, mWallShelf.getBorderHeight());

        } else {
            transParas.mTranslate = mWallShelf.getSlotPosition( objectInfo.mModelInfo.mName, objectInfo.mObjectSlot.mSlotIndex);
        }
        return transParas;
    }

    private List<ConflictObject> getExistentSlot() {
        return mWallShelf.getExistentSlot(getOnMoveObject());
    }

    private ObjectSlot calculateSlot() {
        if (mInRecycle) {
            return null;
        }
        return null;//mWallShelf.calculateNearest(getOnMoveObject(), mRealLocation, false);
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
        return mWallShelf.getSlotPosition(objectSlot);
    }

    void playConflictAnimationTask(ConflictObject conflictObject, long delay, SEAnimFinishListener postExecutor) {
        mWallShelf.playConflictAnimationTask(conflictObject, delay, postExecutor);
    }

    void cancelConflictAnimationTask() {
        mWallShelf.cancelConflictAnimationTask();
    }

}
