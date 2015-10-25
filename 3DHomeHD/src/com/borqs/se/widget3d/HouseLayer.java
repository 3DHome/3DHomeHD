package com.borqs.se.widget3d;

import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEScene;
import com.borqs.framework3d.home3d.TypeManager;

import java.util.ArrayList;

public class HouseLayer extends VesselLayer {
	private static final String TAG = "HouseLayer";
    private VesselLayer mCurrentLayer;

    public HouseLayer(SEScene scene, VesselObject vesselObject) {
        super(scene, vesselObject);
        mCurrentLayer = null;
    }

    @Override
    public boolean canHandleSlot(NormalObject object) {
        /*
        int slotType = object.getObjectInfo().mSlotType;
        if (slotType == ObjectInfo.SLOT_TYPE_DESKTOP ||
            slotType == ObjectInfo.SLOT_TYPE_WALL || slotType == ObjectInfo.SLOT_TYPE_WALL_SHELF) {
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
            if(type.getType() == ObjectInfo.SLOT_TYPE_WALL ||
                    type.getType() == ObjectInfo.SLOT_TYPE_WALL_SHELF) {
                return true;
            }
        }
        return false;
    }
    @Override
    public void leaveLayer(NormalObject object) {
        int slotType = object.getObjectInfo().mSlotType;
        if(slotType == ObjectInfo.SLOT_TYPE_WALL ||
                slotType == ObjectInfo.SLOT_TYPE_WALL_SHELF) {
            VesselLayer layer = new WallLayer(getScene(), getVesselObject());
            layer.leaveLayer(object);
        }
    }
    @Override
    public boolean setOnLayerModel(NormalObject onMoveObject, boolean onLayerModel) {
        super.setOnLayerModel(onMoveObject, onLayerModel);
        if (onLayerModel) {
            mCurrentLayer = null;
            int slotType = onMoveObject.getObjectInfo().mSlotType;
            if (slotType == ObjectInfo.SLOT_TYPE_DESKTOP || slotType == ObjectInfo.SLOT_TYPE_WALL ||
            		slotType == ObjectInfo.SLOT_TYPE_WALL_SHELF) {
                mCurrentLayer = new WallLayer(getScene(), getVesselObject());
                mCurrentLayer.setBoundOfRecycle(mBoundOfRecycle);
                mCurrentLayer.setOnLayerModel(onMoveObject, true);
            } else if (slotType == ObjectInfo.SLOT_TYPE_WALL_GAP) {
                mCurrentLayer = new GroundLayer(getScene(), getVesselObject());
                mCurrentLayer.setBoundOfRecycle(mBoundOfRecycle);
                mCurrentLayer.setOnLayerModel(onMoveObject, true);
            }
        } else {
            if (mCurrentLayer != null) {
                mCurrentLayer.setOnLayerModel(onMoveObject, false);
                mCurrentLayer = null;
            }
        }
        return true;
    }

    public boolean onObjectMoveEvent(ACTION event, float x, float y) {
        if (mCurrentLayer != null) {
            mCurrentLayer.onObjectMoveEvent(event, x, y);
            if (mCurrentLayer != null) {
                mInRecycle = mCurrentLayer.mInRecycle;
            }
        }
        return true;
    }
    /*
    @Override
    public boolean restoreObjectToVessel(NormalObject normalObject, SEAnimFinishListener l) {
    	int slotType = normalObject.getObjectInfo().mSlotType;
    	boolean placeOK = false;
    	NormalObject changedObject = normalObject.getChangedToObj();
    	SEObject parent = normalObject.getParent();
        if (slotType == ObjectInfo.SLOT_TYPE_APP_WALL || slotType == ObjectInfo.SLOT_TYPE_WALL) {
            VesselLayer vesselLayer = new WallLayer(getScene(), getVesselObject());
            placeOK = vesselLayer.placeObjectToVessel(normalObject, l);
            parent = getVesselObject();
        } else if (slotType == ObjectInfo.SLOT_TYPE_WALL_GAP) {
            VesselLayer vesselLayer = new GroundLayer(getScene(), getVesselObject());
            placeOK = vesselLayer.placeObjectToVessel(normalObject, l);
            parent = getVesselObject();
        } else if(slotType == ObjectInfo.SLOT_TYPE_WALL_SHELF) {
        	int slotIndex = normalObject.getObjectSlot().mSlotIndex;
        	SEObject shelfObj = normalObject.getScene().findObject("jiazi", slotIndex);
        	WallShelf wallShelf = (WallShelf)shelfObj;
        	VesselLayer wallShelfLayer = wallShelf.getVesselLayer();
        	parent = wallShelf;
        	if(changedObject != null) {
        		placeOK = wallShelfLayer.placeObjectToVessel(changedObject, null);
        	} else {
        		placeOK = wallShelfLayer.placeObjectToVessel(normalObject, null);
        	}
        }
    	if(placeOK) {
            if (changedObject != null) {
                normalObject.setChangedToObj(null);
                normalObject.getParent().removeChild(normalObject, true);
                changedObject.setChangedToObj(null);
                changedObject.changeParent(parent);
                changedObject.setVisible(true, true);
                getScene().changeTouchDelegate(null);
            } else {
    		    normalObject.changeParent(parent);
            }
    	}
    	return placeOK;
    }
    */
    @Override
    public boolean placeObjectToVessel(NormalObject normalObject, final SEAnimFinishListener l) {
        super.placeObjectToVessel(normalObject, l);
        int slotType = normalObject.getObjectInfo().mSlotType;
        if (slotType == ObjectInfo.SLOT_TYPE_WALL_SHELF || slotType == ObjectInfo.SLOT_TYPE_WALL) {
            VesselLayer vesselLayer = new WallLayer(getScene(), getVesselObject());
            boolean placeOK = false;
            NormalObject changedObject = normalObject.getChangedToObj();
            if(changedObject != null) {
            	placeOK = vesselLayer.placeObjectToVessel(changedObject, null);
            } else {
                placeOK = vesselLayer.placeObjectToVessel(normalObject, l);
            }
        	if(placeOK) {
                if (changedObject != null) {
                    normalObject.setChangedToObj(null);
                    normalObject.getParent().removeChild(normalObject, true);
                    changedObject.setChangedToObj(null);
                    changedObject.setVisible(true, true);
                    changedObject.getRoot().getVesselLayer().setOnMoveObject(changedObject);
                    changedObject.handleSlotSuccess();
                } else {
        		    normalObject.changeParent(getVesselObject());
                }
        	}
        	return placeOK;
        } else if (slotType == ObjectInfo.SLOT_TYPE_WALL_GAP) {
            VesselLayer vesselLayer = new GroundLayer(getScene(), getVesselObject());
            return vesselLayer.placeObjectToVessel(normalObject, l);
        }
        /*
        else if(slotType == ObjectInfo.SLOT_TYPE_WALL_SHELF) {
        	int wallIndex = normalObject.getObjectSlot().mSlotIndex;
        	//SEObject shelfObj = normalObject.getScene().findObject(ModelInfo.Type.WALL_SHELF, slotIndex);
        	House house = (House)getVesselObject();
            WallShelf wallShelf = house.getWallShelfWithObject(wallIndex, normalObject);
        	VesselLayer wallShelfLayer = wallShelf.getVesselLayer();
        	NormalObject changedObject = normalObject.getChangedToObj();
        	boolean placeOK = false;
        	if(changedObject != null) {
        		placeOK = wallShelfLayer.placeObjectToVessel(changedObject, null);
        	} else {
        		placeOK = wallShelfLayer.placeObjectToVessel(normalObject, null);
        	}
        	if(placeOK) {
                if (changedObject != null) {
                    normalObject.setChangedToObj(null);
                    normalObject.getParent().removeChild(normalObject, true);
                    changedObject.setChangedToObj(null);
                    if(changedObject.getParent() != wallShelf) {
                        changedObject.changeParent(wallShelf);

                    }
                    changedObject.setVisible(true, true);
                    changedObject.getRoot().getVesselLayer().setOnMoveObject(changedObject);
                    changedObject.handleSlotSuccess();
                } else {
        		    normalObject.changeParent(wallShelf);
                }
        	}
        	return placeOK;
        }
        */
        return false;
    }
}
