package com.borqs.se.engine;

import org.xmlpull.v1.XmlPullParser;

import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.home3d.XmlUtils;
import com.borqs.se.home3d.ProviderUtils.SceneInfoColumns;

public class SECameraData {
	public static final String CAMERA_TYPE_DEFAULT = "default";
	public static final String CAMERA_TYPE_UP = "up";
	public static final String CAMERA_TYPE_DOWN = "down";
	public static final String CAMERA_TYPE_NEAR = "near";
	public static final String CAMERA_TYPE_FAR = "far";

    public SEVector3f mLocation;
    public SEVector3f mAxisZ;
    public SEVector3f mTargetPos;
    public SEVector3f mUp;
    public float mFov;
    public float mNear;
    public float mFar;
    public int mWidth;
    public int mHeight;
    public String mType;
    private SECameraData mDataStack;

    public SECameraData() {
        mNear = 1;
        mFar = 2000;
        mFov = 60;
    }

    public void save() {
        mDataStack = clone();
    }

    public SECameraData restore() {
        return mDataStack;
    }

    public void update(int width, int height) {
        mHeight = height;
        mWidth = width;
        if (mDataStack != null) {
            mDataStack.update(width, height);
        }
    }

    public SECameraData clone() {
        SECameraData cameraData = new SECameraData();
        cameraData.mLocation = mLocation.clone();
        cameraData.mAxisZ = mAxisZ.clone();
        cameraData.mTargetPos = mTargetPos.clone();
        cameraData.mUp = mUp.clone();
        cameraData.mFov = mFov;
        cameraData.mNear = mNear;
        cameraData.mFar = mFar;
        cameraData.mWidth = mWidth;
        cameraData.mHeight = mHeight;
        cameraData.mType = mType;
        return cameraData;
    }

    public void set(SECameraData data) {
        mLocation = data.mLocation.clone();
        mAxisZ = data.mAxisZ.clone();
        mTargetPos = data.mTargetPos.clone();
        mUp = data.mUp.clone();
        mFov = data.mFov;
        mNear = data.mNear;
        mFar = data.mFar;
         mWidth = data.mWidth;
        mHeight = data.mHeight;
        mType = data.mType;
    }

//    public boolean isValid() {
//        if (mLocation == null || mAxisZ == null || mUp == null) {
//            return false;
//        }
//        return true;
//    }

//    public void init(float[] data) {
//        mLocation = new SEVector3f(data[0], data[1], data[2]);
//        mAxisZ = new SEVector3f(data[3], data[4], data[5]);
//        mUp = new SEVector3f(data[6], data[7], data[8]);
//        mFov = data[9];
//        mWidth = (int) data[10];
//        mHeight = (int) data[11];
//        mNear = data[13];
//        mFar = data[14];
//    }

    public static SECameraData parseFromXml(XmlPullParser parser) {
        if (SESceneInfo.SCENE_CONFIG_KEY_CAMERA.equalsIgnoreCase(parser.getName())) {
            SECameraData data = new SECameraData();
            data.initFromXml(parser);
            return data;
        }

        return null;
    }
    private void initFromXml(XmlPullParser parser) {
        try {
            mFov = Float.valueOf(parser.getAttributeValue(null, SceneInfoColumns.FOV));
            mNear = Float.valueOf(parser.getAttributeValue(null, SceneInfoColumns.NEAR));
            mFar = Float.valueOf(parser.getAttributeValue(null, SceneInfoColumns.FAR));
            mType = parser.getAttributeValue(null, SceneInfoColumns.TYPE);
            while (true) {
                XmlUtils.nextElement(parser);
                String item = parser.getName();
                if (SceneInfoColumns.LOCATION.equals(item)) {
                    mLocation = new SEVector3f();
                    mLocation.initFromXml(parser);
                } else if (SceneInfoColumns.TARGETPOS.equals(item)) {
                	mTargetPos = new SEVector3f();
                	mTargetPos.initFromXml(parser);
                } else if (SceneInfoColumns.UP.equals(item)) {
                    mUp = new SEVector3f();
                    mUp.initFromXml(parser);
                } else {
                	// calculate axis by subtract camera location with target location.
//                	mAxisZ = new SEVector3f(mLocation);
//                    mAxisZ.selfSubtract(mTargetPos);
                    mAxisZ = mLocation.clone().subtract(mTargetPos).normalize();
                    break;
                }
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*public float[] getFloatArray() {
        float[] array = new float[15];
        array[0] = mLocation.mD[0];
        array[1] = mLocation.mD[1];
        array[2] = mLocation.mD[2];
        array[3] = mAxisZ.mD[0];
        array[4] = mAxisZ.mD[1];
        array[5] = mAxisZ.mD[2];
        array[6] = mUp.mD[0];
        array[7] = mUp.mD[1];
        array[8] = mUp.mD[2];
        array[9] = mFov;
        array[10] = mWidth;
        array[11] = mHeight;
        array[12] = mNear;
        array[13] = mFar;
        return array;
    }*/

    public String toString() {
        return "Location = " + mLocation + " AxisZ = " + getAxisZ().toString() + " Up = " + mUp
                + " Fov = " + mFov + ", mType = " + mType
                + ", mWidth = " + mWidth + ", mHeight = " + mHeight;
    }

    public float[] getLocation() {
        return mLocation.mD;
    }

    public float[] getAxisZ() {
        return mAxisZ.mD;
    }

    public float[] getUp() {
        return mUp.mD;
    }

    /*
     public SEVector3f mLocation;
    public SEVector3f mAxisZ;
    public SEVector3f mTargetPos;
    public SEVector3f mUp;
    public float mFov;
    public float mNear;
    public float mFar;
     */
    public void selfScale(float scale) {
        if (scale != 0) {
            mLocation.selfMul(scale);
            mTargetPos.selfMul(scale);
            mAxisZ.selfMul(scale);
            mAxisZ = mAxisZ.normalize();
            mUp.selfMul(scale);
            mUp = mUp.normalize();
            mFov *= scale;
            // TODO: need to scale such factor?
            mNear *= scale;
            mFar *= scale;
        }
    }

    public void selfAdd(SECameraData delta) {
        if (delta != null) {
            mLocation.selfAdd(delta.mLocation);
            mTargetPos.selfAdd(delta.mTargetPos);
            mAxisZ.selfAdd(delta.mAxisZ);
            mAxisZ = mAxisZ.normalize();
            mUp.selfAdd(delta.mUp);
            mUp = mUp.normalize();
            mFov += delta.mFov;
            mNear += delta.mNear;
            mFar += delta.mFar;
        }
    }


    public void selfSubtract(SECameraData delta) {
        if (delta != null) {
            mLocation.selfSubtract(delta.mLocation);
            mTargetPos.selfSubtract(delta.mTargetPos);
            mAxisZ.selfSubtract(delta.mAxisZ);
            mAxisZ = mAxisZ.normalize();
            mUp.selfSubtract(delta.mUp);
            mUp = mUp.normalize();
            mFov -= delta.mFov;
            mNear -= delta.mNear;
            mFar -= delta.mFar;
        }
    }
}
