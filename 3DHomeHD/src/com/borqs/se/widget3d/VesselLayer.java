package com.borqs.se.widget3d;

import android.graphics.Rect;

import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SESceneManager;

public class VesselLayer {
    private SEScene mScene;
    private VesselObject mVesselObject;
    private NormalObject mOnMoveObject;
    private boolean mOnLayerModel;
    protected Rect mBoundOfRecycle;
    protected boolean mInRecycle;
    private static float mSkyRadiusThreshold;

    public enum ACTION {
        BEGIN, MOVE, UP, FLY, FINISH
    };

    public VesselLayer(SEScene scene, VesselObject vesselObject) {
        mScene = scene;
        mVesselObject = vesselObject;
        mOnLayerModel = false;
        mSkyRadiusThreshold = mScene.mSceneInfo.mSkyRadius * 0.8f;
    }
    
    public SEScene getScene() {
        return mScene;
    }

    public boolean canHandleSlot(NormalObject object) {
        mOnMoveObject = object;
        return false;
    }

    public boolean setOnLayerModel(NormalObject onMoveObject, boolean onLayerModel) {
        mOnMoveObject = onMoveObject;
        mOnLayerModel = onLayerModel;
        return true;
    }

    public boolean placeObjectToVessel(NormalObject normalObject, SEAnimFinishListener l) {
        mOnMoveObject = normalObject;
        return true;
    }

    public void setOnMoveObject(NormalObject normalObject) {
        mOnMoveObject = normalObject;
    }
    public boolean onObjectMoveEvent(ACTION event, float x, float y) {
        return false;
    }

    public boolean onObjectMoveEvent(ACTION event, SEVector3f location) {
        return false;
    }

    public NormalObject getOnMoveObject() {
        return mOnMoveObject;
    }
    
    public VesselObject getVesselObject() {
        return mVesselObject;
    }

    public boolean isOnLayerModel() {
        return mOnLayerModel;
    }

   
    public void handleOutsideRoom() {
       
    }


    public void handleNoMoreRoom() {

    }

    public void handleSlotSuccess() {
        if (mOnMoveObject != null && mVesselObject != null) {
            ObjectInfo info = mOnMoveObject.getObjectInfo();
            info.mObjectSlot.mVesselID = mVesselObject.getObjectInfo().mID;
        }
    }
    public void leaveLayer(NormalObject object) {
    	
    }
    public boolean restoreObjectToVessel(NormalObject normalObject, SEAnimFinishListener l) {
    	return false;
    }
    public void setBoundOfRecycle(Rect boundOfRecycle) {
        mBoundOfRecycle = boundOfRecycle;
    }

    /// helper methods
    public static SEVector3f getTouchLocation(SEVector.SERay ray, float z) {
        SEVector3f touchLocZ = rayCrossZFace(ray, z);
        float distance = touchLocZ.getVectorZ().getLength();
        if (distance > mSkyRadiusThreshold) {
            distance = mSkyRadiusThreshold;
            if (touchLocZ.getY() > 0) {
                touchLocZ = rayCrossCylinder(ray, distance);
            }
        }
        return touchLocZ;
    }

    private static SEVector3f rayCrossZFace(SEVector.SERay ray, float z) {
        float para = (z - ray.getLocation().getZ()) / ray.getDirection().getZ();
        SEVector3f touchLoc = ray.getLocation().add(ray.getDirection().mul(para));
        if ((z < ray.getLocation().getZ() && ray.getDirection().getZ() > 0)
                || (z > ray.getLocation().getZ() && ray.getDirection().getZ() < 0)) {
            touchLoc.mD[0] = -touchLoc.getX();
            touchLoc.mD[1] = -touchLoc.getY();
        }
        return touchLoc;
    }

    private static SEVector3f rayCrossCylinder(SEVector.SERay ray, float radius) {
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

    public static boolean isScreenOrientationPortrait() {
        return House.IsScreenOrientationPortrait(SESceneManager.getInstance().getGLActivity());
    }
    /// helper methods end
}
