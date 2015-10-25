package com.borqs.se.engine;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.ViewConfiguration;

import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEVector.SEVector2f;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEVector.SEQuat;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.home3d.HomeScene;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.framework3d.home3d.SEMountPointManager;

public class SEObject {
    public static class AXIS {
        private AXIS() {}
        public static int X = 1;
        public static int Y = 2;
        public static int Z = 3;
    };


    /**
     * object will generate a duplicate image relative a plane.
     * 
     * @param plane
     *            which plane the duplicate image will relative to, 0 is xy, 1
     *            is zx, 2 is yz
     */
    public native void setNeedGenerateMirror_JNI(String mirrorName, int plane);

    public native void setNeedRenderMirror_JNI();

    public native void setNeedGenerateShadow_JNI();

    public native void setNeedRenderShadow_JNI();

    private native void setNeedAlphaTest_JNI(boolean enable);

    public native static int loadImageData_JNI(String imagepath);

    public native static int loadImageData_JNI(Bitmap bitmap);

//    public native static void releaseImageData_JNI(int imageDataObj);
    public static void releaseImageData_JNI(int imageDataObj) {
    // todo: merge this jni method.
    }

    // new image will replace old image if the image key has existed
    public native static void addImageData_JNI(String key, int imageDataObj);

    public native static void removeImageData_JNI(String imageKey);

    public native String getImageName_JNI();
    
    public native static String getImageKey_JNI(String imageName);
    
    public native static void applyImage_JNI(String imageName, String imageKey);

    public native static void showAllNode_JNI(boolean show);

    public native void applyLight_JNI(String lightName);

    public native void unApplyLight_JNI(String lightName);

    protected native void addUserObject_JNI();
    private native void createLocalBoundingVolume_JNI();
    private native void clearLocalBoundingVolume_JNI();
    public void createLocalBoundingVolume() {
    	    createLocalBoundingVolume_JNI();
    }
    public void clearLocalBoundingVolume() {
        clearLocalBoundingVolume_JNI();
    }
    //local bounding volume must be AABB bounding volume
    //outData is 6 float element array
    private native void getLocalBoundingVolume_JNI(float[] outData);
    public void getLocalBoundingVolume(SEVector3f minPoint, SEVector3f maxPoint) {
    	    float[] outData = new float[6];
    	    getLocalBoundingVolume_JNI(outData);
    	    for(int i = 0 ; i < 3 ; i++) {
    	    	    minPoint.mD[i] = outData[i];
    	    }
    	    for(int i = 0 ; i < 3 ; i++) {
    	    	    maxPoint.mD[i] = outData[i + 3];
    	    }
    }
    //quat and vector are 4 elements
    private static native void rotateMap_JNI(float[] quat, float[] vector, float[] outData);
    private static SEVector3f rotateMap(SERotate rotate, SEVector3f vector, int vectorType) {
    	//this is a hack about z rotate
	    if(rotate.mD[3] < 0) {
    	    rotate.mD[3] = - rotate.mD[3];
    	    rotate.mD[0] = - rotate.mD[0];
        }
	    //
	    SEQuat q = rotate.toQuat();
	    float[] qData = new float[4];
	    for(int i = 0 ; i < 4 ; i++) {
	    	    qData[i] = q.mD[i];
	    }
	    float[] vectorData = new float[4];
	    if(vectorType == VECTOR_TYPE_POINT) {
	    	    vectorData[3] = 1;
	    }
	    for(int i = 0 ; i < 3 ; i++) {
	    	    vectorData[i] = vector.mD[i];
	    }
	    float[] outData = new float[4];
	    rotateMap_JNI(qData, vectorData, outData);
	    SEVector3f outVector = new SEVector3f();
	    for(int i = 0 ; i < 3 ; i++) {
	    	    outVector.mD[i] = outData[i];
	    }
	    return outVector;
    }

    public static SEVector3f rotateMapVector(SERotate rotate, SEVector3f vector) {
    	    return rotateMap(rotate, vector, VECTOR_TYPE_VECTOR);
    }
    public static SEVector3f rotateMapPoint(SERotate rotate, SEVector3f point) {
    	return rotateMap(rotate, point , VECTOR_TYPE_POINT);
    }
    //outP must be float  array with 4 elements
    private native void toWorldCoordinate_JNI(float[] p, float[] outP);
    
    public void toWorldCoordinate(float[] p, float[] outp) {
    	    toWorldCoordinate_JNI(p, outp);
    }
    private static final int VECTOR_TYPE_POINT = 0;
    private static final int VECTOR_TYPE_VECTOR = 1;
    private static final int VECTOR_TO_WORLD_COORD = 0;
    private static final int VECTOR_TO_OBJECT_COORD = 1;
    private SEVector3f changeVectorPoint(SEVector3f vector, int vectorType, int coordType) {
   	    float[] vectorData = new float[4];
	    for(int i = 0 ; i < 3 ; i++) {
	    	    vectorData[i] = vector.mD[i];
	    }
	    if(vectorType == VECTOR_TYPE_POINT) {
	        vectorData[3] = 1;
	    }
	    float[] outData = new float[4];
	    if(coordType == VECTOR_TO_WORLD_COORD) {
	        toWorldCoordinate(vectorData, outData);
	    } else {
	    	    toObjectCoordinate(vectorData, outData);
	    }
	    SEVector3f outPoint = new SEVector3f();
	    for(int i = 0 ; i < 3 ; i++) {
	        outPoint.mD[i] = outData[i];
	    }
	    return outPoint;
    }
    public SEVector3f toWorldPoint(SEVector3f objectPoint) {
        return changeVectorPoint(objectPoint, VECTOR_TYPE_POINT, VECTOR_TO_WORLD_COORD);
    }
    public SEVector3f toWorldVector(SEVector3f objectVector) {
    	    return changeVectorPoint(objectVector, VECTOR_TYPE_VECTOR, VECTOR_TO_OBJECT_COORD);
    }
    //outP must be float array with 4 elements
    private native void toObjectCoordinate_JNI(float[] p, float[] outP);
    public void toObjectCoordinate(float[] p, float[] outp) {
    	    toObjectCoordinate_JNI(p, outp);
    }
    public SEVector3f toObjectPoint(SEVector3f worldPoint) {
    	    return changeVectorPoint(worldPoint, VECTOR_TYPE_POINT, VECTOR_TO_OBJECT_COORD);
    }
    public SEVector3f toObjectVector(SEVector3f worldVector) {
    	    return changeVectorPoint(worldVector, VECTOR_TYPE_VECTOR, VECTOR_TO_OBJECT_COORD);
    }
    private native void getAbsoluteTranslate_JNI(float[] center);

    private native void getWorldBoundingVolumeCenter_JNI(float[] center);

    private native SEObject getLeader_JNI();

    private native void setLeader_JNI(String lname, int lindex);

    private native void addFollower_JNI(String fname, int findex);

    private native void removeFollower_JNI(String fname, int findex);

    private native void setSelected_JNI(boolean selected);// The object will be
                                                          // draw by engine at
                                                          // lastly if being
                                                          // selected

    public native void setTouchable_JNI(boolean enable); // Do not doing ray
                                                         // detect if disable

    // Using for the method "getSelectedObject_JNI" of camera, camera will
    // return its parent, if it not an entirety object
    public native void setIsEntirety_JNI(boolean isEntirety);

    public native void setTexCoordXYReverse_JNI(boolean x, boolean y);

    public void setLeader(SEObject leader) {
        if (leader != null) {
            setLeader_JNI(leader.getName(), leader.mIndex);
            leader.addFollower_JNI(mName, mIndex);
        }
    }

    public void removeFollower(String followerName, int index) {
        if (followerName != null) {
            removeFollower_JNI(followerName, index);
        }
    }

    public SEObject getLeaderName() {
        return getLeader_JNI();
    }

    public void changeParent(SEObject newParent) {
        if (newParent != null) {
            if (mParent != null) {
                mParent.removeChild(this, false);
            }
            changeParent_JNI(newParent.mName, newParent.mIndex);
            newParent.addChild(this, false);
        }
    }

    private native void changeParent_JNI(String newParent, int parentIndex);

    /**
     * object will be removed from scene, but will not be released. return its
     * pointer.
     * 
     */
    public native int remove_JNI();

    private native void release_JNI();

    public void release() {
        onRelease();
        release_JNI();
    }

    private native void setVisible_JNI(boolean visible);

    private native void setAlpha_JNI(float alpha);

    public void setAlpha(float alpha, boolean submit) {
        mAlpha = alpha;
        if (submit) {
            setAlpha_JNI(alpha);
        }
        if (mObjectData != null) {
            mObjectData.setAlpha(alpha);
        }
    }
    
    public void setNeedForeverBlend(boolean need) {
        if (need) {
            mRenderState |= SEObjectData.NEED_FOREVER_BLEND;
        } else {
            mRenderState &= ~SEObjectData.NEED_FOREVER_BLEND;
        }        
        
        if (mObjectData != null) {
            mObjectData.setNeedForeverBlend(need);
        }
    }

    public float getAlpha() {
        return mAlpha;
    }

    public native void updateVertex_JNI(float[] vertex);

    public native void updateTexture_JNI(float[] texVertex);

    public native boolean cloneObject_JNI(SEObject parent, int index, boolean copy, String status);
    public native boolean cloneObjectNew_JNI(SEObject parent, int index);

    public boolean cloneObject(int index) {
        return cloneObjectNew_JNI(getParent(), index);
    }
    public boolean cloneObject(int index, boolean copy, String status) {
        return cloneObject_JNI(getParent(), index, copy, status);
    }
    public void setShader(int type) {
        setShaderType_JNI(SEObjectData.getShaderType(type));
        setRenderType_JNI(SEObjectData.getRenderType(type));
    }

    public int getShader() {
        return SEObjectData.getShaderIndex(getShaderType_JNI());
    }

    private native void setShaderType_JNI(String type);

    private native void setRenderType_JNI(String type);

    private native String getShaderType_JNI();

    private native String getRenderType_JNI();

    // public native void setRenderState(int state);

    // trans object begin.
    private native void operateObject_JNI(float[] translate, float[] rotate, float[] scale);

    private native void rotateObject_JNI(float[] rotate);

    public void rotateObject(SERotate rotate) {
        rotateObject_JNI(rotate.mD);
    }

    private native void rotatePoint_JNI(float[] rotate);

    public void rotatePoint(SERotate rotate) {
        rotatePoint_JNI(rotate.mD);
    }
    //col0, col1, col2, col3 are 4 element array
    private native void setReferenceFrameMatrix_JNI(float[] col0, float[] col1, float[] col2, float[] col3);
    public void setReferenceFrameMatrix(SEVector3f col0, SEVector3f col1, SEVector3f col2, SEVector3f col3) {
    	    float[] c0 = new float[4];
    	    float[] c1 = new float[4];
    	    float[] c2 = new float[4];
    	    float[] c3 = new float[4];
    	    float[][] srcCols = {col0.mD, col1.mD, col2.mD, col3.mD};
    	    float[][] dstCols = {c0, c1, c2, c3};
    	    for(int i = 0 ; i < 4 ; i++) {
    	    	    float[] srcCol = srcCols[i];
	    	    float[] dstCol = dstCols[i];  
    	    	    for(int j = 0 ; j < 3 ; j++ ) {
    	    	    	    dstCol[j] = srcCol[j];  
    	    	    }
    	    }
    	    c3[3] = 1;
    	    setReferenceFrameMatrix_JNI(c0, c1, c2, c3);
    }
    public void setReferenceFrameMatrixTranslate(SEVector3f translate) {
    	    setReferenceFrameMatrix(new SEVector3f(1, 0, 1), new SEVector3f(0, 1, 0),
    	    		                    new SEVector3f(0, 0, 1), translate);
    }
    //rotate is the 4 element array
    //scale is the 3 element array
    //translate is the 3 element array
    private native void setReferenceFrameMatrix_JNI(float[] rotate, float[] scale, float[] translate);
    public void setReferenceFrameMatrix(SERotate rotate, SEVector3f scale, SEVector3f translate) {
    	    setReferenceFrameMatrix_JNI(rotate.mD, scale.mD, translate.mD);
    }
/////////////////////
    private native void translateObject_JNI(float[] translate, boolean collisionDetect);

    private native void scaleObject_JNI(float[] scale);

    private native void setRotate_JNI(float[] rotate);

    public void setRotate(SERotate rotate, boolean submit) {
        mUserTransParas.mRotate = rotate;
        if (submit) {
            setRotate_JNI(rotate.mD);
        }
        if (mObjectData != null) {
            mObjectData.mUserTransParas.mRotate = rotate;
        }
    }

    private native void setTranslate_JNI(float[] translate);

    public void setTranslate(SEVector3f translate, boolean submit) {
        mUserTransParas.mTranslate = translate;
        if (submit) {
            setTranslate_JNI(translate.mD);
        }
        if (mObjectData != null) {
            mObjectData.mUserTransParas.mTranslate = translate;
        }
    }

    private native void setScale_JNI(float[] scale);

    public void setScale(SEVector3f scale, boolean submit) {
        mUserTransParas.mScale = scale;
        if (submit) {
            setScale_JNI(scale.mD);
        }
        if (mObjectData != null) {
            mObjectData.mUserTransParas.mScale = scale;
        }
    }

    private native void getUserRotate_JNI(float[] rotate);

    public SERotate getUserRotate() {
        return mUserTransParas.mRotate;
    }

    private native void getUserTranslate_JNI(float[] translate);

    public SEVector3f getUserTranslate() {
        return mUserTransParas.mTranslate;
    }

    private native void getUserScale_JNI(float[] scale);

    public SEVector3f getUserScale() {
        return mUserTransParas.mScale;
    }

    public native void getLocalRotate_JNI(float[] rotate);

    public native void getLocalTranslate_JNI(float[] translate);

    public native void getLocalScale_JNI(float[] scale);

    private native void rotateLocal_JNI(float[] rotate);

    public void rotateLocal(SERotate rotate) {
        rotateLocal_JNI(rotate.mD);
    }

    private native void translateLocal_JNI(float[] translate);

    public void translateLocal(SEVector3f translate) {
        translateLocal_JNI(translate.mD);
    }

    private native void scaleLocal_JNI(float[] translate);

    public void scaleLocal(SEVector3f scale) {
        scaleLocal_JNI(scale.mD);
    }

    private native void setLocalRotate_JNI(float[] rotate);

    public void setLocalRotate(SERotate rotate) {
        setLocalRotate_JNI(rotate.mD);
    }

    private native void setLocalTranslate_JNI(float[] translate);

    public void setLocalTranslate(SEVector3f translate) {
        setLocalTranslate_JNI(translate.mD);
    }

    private native void setLocalScale_JNI(float[] translate);

    public void setLocalScale(SEVector3f scale) {
        setLocalScale_JNI(scale.mD);
    }

    private native boolean isEnableDepth_JNI();

    private native boolean isEnableBlend_JNI();

    public native void changeRenderState_JNI(int i, boolean enable);

    public native void setShadowObjectVisibility_JNI(boolean enable);

    /**
     * child Object will be be hanged in this node
     * 
     * @param childObject
     *            object's pointer
     */
    public native void addChild_JNI(int childObject);

    public void addChild(SEObject obj, boolean create) {

    }

    public void removeChild(SEObject child, boolean release) {

    }

    public void removeAllChild(boolean release) {

    }

    /**
     * 
     * @param vect4f
     *            , vect4f[0] = R, vect4f[1] = G, vect4f[2] = B, vect4f[3] =
     *            1(use) or 0(not use) user color
     */
    private native void setUseUserColor_JNI(float[] vect4f);

    public void setUseUserColor(float r, float g, float b) {
        setUseUserColor_JNI(new float[] { r, g, b, 1 });
    }

    public void clearUserColor() {
        setUseUserColor_JNI(new float[] { 1, 1, 1, 0 });
    }

    private native void setNeedCullFace_JNI(boolean enable);

    private native void setIsMiniBox_JNI(boolean miniBox);

    /**
     * 
     * @param offset
     *            , offset.xy should be form -1.0 to 1.0
     * 
     */
    private native void uvAnimation_JNI(float[] offset, boolean enableUVAnimation);

    public void playUVAnimation(SEVector2f direction) {
        uvAnimation_JNI(direction.mD, true);
    }

    public void stopUVAnimation() {
        uvAnimation_JNI(null, false);
    }

    /**
     * @param x: screen point x
     * @param y: screen point y
     * @param selPoint: get the crossed point if screen point crossed this object
     */
    public native boolean isObjectSelected_JNI(int x, int y, float[] selPoint);
    /**
     * set draw order direction of this object, the far object will draw in
     * front of the near object at the direction
     */
    public void setBlendSortAxis(int axis) {
        if (axis == AXIS.X) {
            se_setNeedBlendSort_JNI(new float[] { 1, 0, 0 });
        } else if (axis == AXIS.Y) {
            se_setNeedBlendSort_JNI(new float[] { 0, 1, 0 });
        } else if (axis == AXIS.Z) {
            se_setNeedBlendSort_JNI(new float[] { 0, 0, 1 });
        }
    }

    public native void se_setNeedBlendSort_JNI(float[] vector3fxyz);
    public SEVector3f getSelectPoint(int x, int y) {
        SEVector3f result = new SEVector3f();
        if (isObjectSelected_JNI(x, y, result.mD)) {
            return result;
        }
        return null;
    }

    private native void setLayerIndex_JNI(int index);

    public native void showRBColor_JNI(boolean show);

    public native static boolean isImageExist_JNI(String imagePath);

    public native static void setTexCoordXYTile_JNI(float[] tile2fxy);

    // -----------------------------------finish------------------jni
    // trans object end.

    private static final int PRESSED = 0x00000001;
    private static final int CLICKABLE = 0x00000002;
    private int mPrivateFlags;
    private boolean mHasPerformedLongPress;
    private Timer mTimer;
    private OnTouchListener mOnClickListener;
    private OnTouchListener mOnLongClickListener;
    private OnTouchListener mOnPressedListener;
    private OnTouchListener mOnUnpressedListener;
    private OnTouchListener mOnDoubleClickListener;
    protected DepriveTouchListener mDepriveTouchListener;
    private CheckForLongPress mCheckForLongPress;
    private int mPreTouchX;
    private int mPreTouchY;
    private int mTouchX;
    private int mTouchY;
    // private MediaPlayer mp;
    public String mName;
    public int mIndex;
    private SEObject mParent;
    private SEScene mScene;
    private String mSceneName;
    private String mParentName;
    private SECamera mSECamera;
    private SEObjectData mObjectData;
    private SETransParas mUserTransParas;
    private int mRenderState;
    private int mShaderType;
    private float mAlpha;
    private float mPressValue;
    private PRESS_TYPE mPressType;
    public boolean mIsNode;
    public String mDataPath;
    private AudioManager mAudioManager;
    /**
     * Cache the touch slop from the context that created the view.
     */
    private int mTouchSlop;
    private Object mTag;

    private Context mContext;
    private boolean mIsEnbaleBlending;
    private boolean mHasGetBlending;
    private ObjectPressedAnimation mObjectPressedAnimation;
    private ObjectUnpressedAnimation mObjectUnpressedAnimation;
    private boolean mHasBeenReleased;

    private Timer mDoublieClickTimer;
    private CheckForDoubleClick mCheckForDoubleClick;
    private int mClickCount;

    public interface OnTouchListener {
        public void run(SEObject obj);
    }

    public interface DepriveTouchListener {
        boolean onTouch(SEObject obj, MotionEvent event);
    }

    public SEObject(SEScene scene, String name) {
        init();
        mIndex = 0;
        mName = name;
        mHasBeenReleased = false;
        if (scene != null) {
            mScene = scene;
            mSceneName = mScene.mSceneName;
            mContext = scene.getContext();
            mTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
        }
    }

    public SEObject(SEScene scene, String name, int index) {
        init();
        mIndex = index;
        mName = name;
        if (scene != null) {
            mScene = scene;
            mSceneName = mScene.mSceneName;
            mContext = scene.getContext();
            mTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
        }
    }

    private SEObject(String name, int index) {
        init();
        mIndex = index;
        mName = name;
    }

    public int getTouchSlop() {
        return mTouchSlop;
    }

    public void setObjectData(SEObjectData data) {
        mObjectData = data;
        mUserTransParas = mObjectData.mUserTransParas;
        mRenderState = mObjectData.getRenderState();
        mAlpha = mObjectData.getAlpha();
        mShaderType = mObjectData.getShaderIndex();
        mHasSyncBlending = true;
        mHasSyncDepthTest = true;
    }

    public void setImageSize(int w, int h) {
        if (mObjectData != null) {
            mObjectData.setImageSize(w, h);
        }
    }

    public void setLayerIndex(int layerIndex, boolean sumbit) {
        if (sumbit) {
            setLayerIndex_JNI(layerIndex);
        }
        if (mObjectData != null) {
            mObjectData.setLayerIndex(layerIndex);
        }
    }

    public SEObject findChild(String name, int index) {
        return null;
    }

    public SEObject findObject(String name, int index) {
        return null;
    }

    public SEObject travelObject(SEObjectTravel travel) {
        return null;
    }

    public boolean hasChild(SEObject obj) {
        return false;
    }

    private void init() {
    	mSoftRelease = false;
        mHasGetBlending = false;
        mAlpha = 1;
        mPressValue = 1;
        mPressType = PRESS_TYPE.ALPHA;
        mDataPath = "db";
        mIsNode = false;
        mUserTransParas = new SETransParas();
        mPrivateFlags = 2;
        mSceneName = "home";
        mParentName = "home_root";
        mRenderState = SEObjectData.VISIBLE + SEObjectData.NEED_DEPTH_TEST;

    }

    public Context getContext() {
        return mContext;
    }

    public SEScene getScene() {
        return mScene;
    }

    public void setTag(Object tag) {
        mTag = tag;
    }

    public Object getTag() {
        return mTag;
    }

    public void setLocalTransParas(SETransParas trans) {
        if (mObjectData != null) {
            mObjectData.mLocalTransParas = trans;
        }

    }

    public SETransParas getLocalTransParas() {
        if (mObjectData != null) {
            return mObjectData.mLocalTransParas;
        }
        return null;
    }

    // public void setUserTransParas(SETransParas trans) {
    // mUserTransParas = trans;
    // mHasSyncRotate = true;
    // mHasSyncTranslate = true;
    // mHasSyncScale = true;
    // if (mObjectData != null) {
    // mObjectData.mUserTransParas = trans;
    // } else {
    // operateObject_JNI(mUserTransParas.getTranslate(),
    // mUserTransParas.getRotate(), mUserTransParas.geScale());
    // }
    // }

    public SETransParas getUserTransParas() {
        return mUserTransParas;
    }

    private native void setBlendingable_JNI(boolean enable);

    public void setBlendingable(boolean blend, boolean sumbit) {
        if (blend) {
            mRenderState |= SEObjectData.NEED_BLENDING;
        } else {
            mRenderState &= ~SEObjectData.NEED_BLENDING;
        }
        if (sumbit) {
            setBlendingable_JNI(blend);
        }
        if (mObjectData != null) {
            mObjectData.setBlendingable(blend);
        }
    }

    public void setNeedDepthTest(boolean depthTest, boolean sumbit) {
        if (depthTest) {
            mRenderState |= SEObjectData.NEED_DEPTH_TEST;
        } else {
            mRenderState &= ~SEObjectData.NEED_DEPTH_TEST;
        }
        if (sumbit) {
            changeRenderState_JNI(0, depthTest);
        }
        if (mObjectData != null) {
            mObjectData.setNeedDepthTest(depthTest);
        }
    }

    public void setNeedCullFace(boolean cullFace, boolean sumbit) {
        if (cullFace) {
            mRenderState |= SEObjectData.NEED_CULL_FACE;
        } else {
            mRenderState &= ~SEObjectData.NEED_CULL_FACE;
        }
        if (sumbit) {
            setNeedCullFace_JNI(cullFace);
        }
        if (mObjectData != null) {
            mObjectData.setNeedCullFace(true);
        }
    }

    public void setNeedAlphaTest(boolean alphaTest, boolean sumbit) {
        if (alphaTest) {
            mRenderState |= SEObjectData.NEED_ALPHA_TEST;
        } else {
            mRenderState &= ~SEObjectData.NEED_ALPHA_TEST;
        }
        if (sumbit) {
            setNeedAlphaTest_JNI(alphaTest);
        }
        if (mObjectData != null) {
            mObjectData.setNeedAlphaTest(alphaTest);
        }
    }

    public void setVisible(boolean visible, boolean sumbit) {
        if (visible) {
            mRenderState |= SEObjectData.VISIBLE;
        } else {
            mRenderState &= ~SEObjectData.VISIBLE;
        }
        if (sumbit) {
            setVisible_JNI(visible);
        }
        if (mObjectData != null) {
            mObjectData.setVisible(visible);
        }
    }

    public void setIsMiniBox(boolean miniBox, boolean sumbit) {
        if (miniBox) {
            mRenderState |= SEObjectData.IS_MINI_BOX;
        } else {
            mRenderState &= ~SEObjectData.IS_MINI_BOX;
        }
        if (sumbit) {
            setIsMiniBox_JNI(miniBox);
        }
        if (mObjectData != null) {
            mObjectData.setIsMiniBox(miniBox);
        }
    }

    public boolean isCullFaceOpend() {
        return (mRenderState & SEObjectData.NEED_CULL_FACE) != 0;
    }

    private boolean mHasSyncBlending = false;

    public boolean isBlendingable() {
        if (!mHasSyncBlending) {
            boolean enable = isEnableBlend_JNI();
            if (enable) {
                mRenderState |= SEObjectData.NEED_BLENDING;
            } else {
                mRenderState &= ~SEObjectData.NEED_BLENDING;
            }
            return enable;
        }
        mHasSyncBlending = true;
        return (mRenderState & SEObjectData.NEED_BLENDING) != 0;
    }

    private boolean mHasSyncDepthTest = false;

    public boolean isDepthTestOpened() {
        if (!mHasSyncDepthTest) {
            boolean enable = isEnableDepth_JNI();
            if (enable) {
                mRenderState |= SEObjectData.NEED_DEPTH_TEST;
            } else {
                mRenderState &= ~SEObjectData.NEED_DEPTH_TEST;
            }
            return enable;
        }
        mHasSyncDepthTest = true;
        return (mRenderState & SEObjectData.NEED_DEPTH_TEST) != 0;
    }

    public boolean isNeedAlphaTest() {
        return (mRenderState & SEObjectData.NEED_ALPHA_TEST) != 0;
    }

    public boolean isVisible() {
        return (mRenderState & SEObjectData.VISIBLE) != 0;
    }

    public SEVector3f getAbsoluteTranslate() {
        SEVector3f center = new SEVector3f();
        getAbsoluteTranslate_JNI(center.mD);
        return center;
    }

    public SEVector3f getWorldBoundingVolumeCenter_JNI() {
        SEVector3f center = new SEVector3f();
        getWorldBoundingVolumeCenter_JNI(center.mD);
        return center;
    }

    public SEVector3f getLocalTranslate() {
        SEVector3f translate = new SEVector3f();
        getLocalTranslate_JNI(translate.mD);
        return translate;
    }

    public void setUserTransParas() {
        operateObject_JNI(mUserTransParas.getTranslate(), mUserTransParas.getRotate(), mUserTransParas.geScale());
    }

    protected void render() {
        if (mHasBeenReleased) {
            return;
        }
        onRender(getCamera());
        addUserObject_JNI();
        if (!mIsNode && mObjectData != null) {
            if (mObjectData.getImageType() == SEObjectData.IMAGE_TYPE_IMAGE) {
                SELoadResThread.getInstance().process(new Runnable() {
                    public void run() {
                        if (mHasBeenReleased) {
                            return;
                        }
                        final int image = loadImageData_JNI(mObjectData.getImageKey());
                        new SECommand(getScene()) {
                            public void run() {
                                if (mHasBeenReleased) {
                                    return;
                                }
                                addImageData_JNI(mObjectData.getImageKey(), image);                   
                                applyImage_JNI(mObjectData.getImageName(), mObjectData.getImageKey());
                                mObjectData = null;
                            }
                        }.execute();
                    }
                });
            } else if (mObjectData.getImageType() == SEObjectData.IMAGE_TYPE_BITMAP) {
                if (mObjectData.getBitmap() != null) {
                    SELoadResThread.getInstance().process(new Runnable() {
                        public void run() {
                            if (mHasBeenReleased) {
                                return;
                            }
                            final int image = loadImageData_JNI(mObjectData.getBitmap());
                            mObjectData.getSEBitmap().recycle();
                            new SECommand(getScene()) {
                                public void run() {
                                    addImageData_JNI(mObjectData.getImageKey(), image);
                                    applyImage_JNI(mObjectData.getImageName(), mObjectData.getImageKey());
                                    mObjectData = null;
                                }
                            }.execute();
                        }
                    });
                } else {
                    SELoadResThread.getInstance().process(new Runnable() {
                        public void run() {
                            if (mHasBeenReleased) {
                                return;
                            }
                            Bitmap bitmap = onStartLoadImage();
                            if (bitmap != null) {
                                final int image = loadImageData_JNI(bitmap);
                                bitmap.recycle();
                                new SECommand(getScene()) {
                                    public void run() {
                                        addImageData_JNI(mObjectData.getImageKey(), image);
                                        applyImage_JNI(mObjectData.getImageName(), mObjectData.getImageKey());
                                        mObjectData = null;
                                    }
                                }.execute();
                            }
                        }
                    });
                }
            }

        }
        onRenderFinish(getCamera());
    }

    public void onRender(SECamera camera) {

    }

    public void onRenderFinish(SECamera camera) {
    }

    public Bitmap onStartLoadImage() {
        return null;
    }

    public void stopAllAnimation(SEAnimFinishListener l) {

    }

    public String getName() {
        return mName;
    }

    public SEObject getParent() {
        return mParent;
    }

    public void setParent(SEObject parent) {
        mParent = parent;
    }

    public void setScene(SEScene scene) {
    	if (mScene != scene) {
    		mScene = scene;
    	}
    }

    private String getSceneName() {
        if (mScene != null) {
            return mScene.mSceneName;
        }
        return null;
    }

    public String getParentName() {
        if (mParent != null) {
            return mParent.getName();
        }
        return null;
    }

    public int getParentIndex() {
        if (mParent != null) {
            return mParent.mIndex;
        }
        return 0;
    }

    public void setPressed(boolean pressed) {
        if (pressed && !isPressed()) {
            mPrivateFlags |= PRESSED;
            setSelected_JNI(true);
            performPressed();
        } else if (!pressed && isPressed()) {
            mPrivateFlags &= ~PRESSED;
            setSelected_JNI(false);
            performUnpressed();
        }
    }

    public boolean isPressed() {
        return (mPrivateFlags & PRESSED) == PRESSED;
    }

    public void setClickable(boolean clickable) {
        if (clickable) {
            mPrivateFlags |= CLICKABLE;
        } else if (!clickable) {
            mPrivateFlags &= ~CLICKABLE;
        }
    }

    public boolean isClickable() {
        return (mPrivateFlags & CLICKABLE) == CLICKABLE;
    }

    public void notifySurfaceChanged(int width, int height) {

    }

    /**
     * Register a callback to be invoked when this view is clicked. If this view
     * is not clickable, it becomes clickable.
     * 
     * @param l
     *            The callback that will run
     * 
     * @see #setClickable(boolean)
     */
    public void setOnClickListener(OnTouchListener l) {
        mOnClickListener = l;
        ensurePressedAction();
    }

    public void setOnDoubleClickListener(OnTouchListener l) {
        mOnDoubleClickListener = l;
        ensurePressedAction();
    }
    /**
     * Register a callback to be invoked when this view is clicked and held. If
     * this view is not long clickable, it becomes long clickable.
     * 
     * @param l
     *            The callback that will run
     * 
     */
    public void setOnLongClickListener(OnTouchListener l) {
        mOnLongClickListener = l;
        ensurePressedAction();
    }

    public void setIsAlphaPress(boolean alphaPress) {
        mPressType = PRESS_TYPE.ALPHA;
    }

    private void ensurePressedAction() {
        if (mOnPressedListener == null) {
            mOnPressedListener = new OnTouchListener() {

                public void run(SEObject obj) {
                    if (mObjectPressedAnimation != null) {
                        mObjectPressedAnimation.stop();
                    }
                    if (mObjectUnpressedAnimation != null) {
                        mObjectUnpressedAnimation.stop();
                    }
                    mObjectPressedAnimation = new ObjectPressedAnimation(getScene());
                    mObjectPressedAnimation.execute();

                }
            };
        }
        if (mOnUnpressedListener == null) {
            mOnUnpressedListener = new OnTouchListener() {

                public void run(SEObject obj) {
                    if (mObjectPressedAnimation != null) {
                        mObjectPressedAnimation.stop();
                    }
                    if (mObjectUnpressedAnimation != null) {
                        mObjectUnpressedAnimation.stop();
                    }
                    mObjectUnpressedAnimation = new ObjectUnpressedAnimation(getScene());
                    mObjectUnpressedAnimation.execute();

                }
            };
        }
    }


    public enum PRESS_TYPE {
        ALPHA, COLOR, NONE
    }

    public void setPressType(PRESS_TYPE type) {
        mPressType = type;
    }

    public void setPressedListener(OnTouchListener l) {
        mOnPressedListener = l;
    }

    public void setUnpressedListener(OnTouchListener l) {
        mOnUnpressedListener = l;
    }

    public void setDepriveTouchListener(DepriveTouchListener l) {
        mDepriveTouchListener = l;
    }

    public int getTouchX() {
        return mTouchX;
    }

    public int getTouchY() {
        return mTouchY;
    }

    public void setTouch(int x, int y) {
        mTouchX = x;
        mTouchY = y;
    }

    public int getPreTouchX() {
        return mPreTouchX;
    }

    public int getPreTouchY() {
        return mPreTouchY;
    }

    public void setPreTouch() {
        mPreTouchX = mTouchX;
        mPreTouchY = mTouchY;
    }

    public void setPreTouch(int x, int y) {
        mPreTouchX = x;
        mPreTouchY = y;
    }

    public boolean handleBackKey(SEAnimFinishListener l) {
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_CANCEL) {
            setTouch((int) event.getX(), (int) event.getY());
        }
        if (mDepriveTouchListener != null && mDepriveTouchListener.onTouch(this, event)) {
            return true;
        }
        return onTouchEvent(event);
    }

    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
        case MotionEvent.ACTION_UP:
            if (isPressed()) {
                setPressed(false);
                if (!mHasPerformedLongPress) {
                    // This is a tap, so remove the longpress check
                    removeCallbacks();
                    if (mClickCount > 1) {
                        removeDoublieClick();
                        mClickCount = 0;
                        performDoubleClick();
                    } else {
                        performClick();
                    }
                }
            }
            break;
        case MotionEvent.ACTION_DOWN:
            mClickCount++;
            postCheckForLongClick();
            removeDoublieClick();
            postCheckForDoubleClick();
            setPressed(true);
            break;

        case MotionEvent.ACTION_CANCEL:
            if (!mHasPerformedLongPress) {
                removeCallbacks();
            }
            setPressed(false);
            if (mObjectPressedAnimation != null) {
                mObjectPressedAnimation.stop();
            }
            if (mObjectUnpressedAnimation != null) {
                mObjectUnpressedAnimation.stop();
            }
            break;

        case MotionEvent.ACTION_MOVE:
            break;
        }
        return true;
    }

    public boolean checkOutside(int x, int y) {
        if (getCamera() == null) {
            return false;
        }
        SEObject selObj = getCamera().getSelectedObject_JNI(x, y);
        if (selObj != null) {
            if (selObj.equals(this))
                return false;
        }
        return true;
    }

    public void setCamera(SECamera camera) {
        mSECamera = camera;
    }

    public SECamera getCamera() {
        if (mSECamera == null) {
            if (mScene != null) {
                mSECamera = mScene.getCamera();
            }
        }
        return mSECamera;
    }

    public void performPressed() {
        if (HomeUtils.DEBUG) {
            Log.d(HomeUtils.TAG, mName + "^_^ ^_^ ^_^ performPressed()");
        }
        if (mOnPressedListener != null) {
            mOnPressedListener.run(this);
        }
    }

    private AudioManager getAudioManager() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        }
        return mAudioManager;
    }

    public void performUnpressed() {
        if (HomeUtils.DEBUG) {
            Log.d(HomeUtils.TAG, mName + "^_^ ^_^ ^_^ performUnpressed()");
        }
        if (mOnUnpressedListener != null) {
            mOnUnpressedListener.run(this);
        }
    }

    public boolean performLongClick() {
        if (HomeUtils.DEBUG) {
            Log.d(HomeUtils.TAG, mName + "^_^ ^_^ ^_^ performLongClick()");
        }
        if (mOnLongClickListener != null) {
            Vibrator vibrator = (Vibrator) mContext.getSystemService(Service.VIBRATOR_SERVICE);
            vibrator.vibrate(60);
            
            getScene().handleMessage(HomeScene.MSG_TYPE_SHOW_OBJECT_LONG_CLICK_DIALOG, this);
            onObjectLongClicked();
            
            mOnLongClickListener.run(SEObject.this);
            return true;

        }
        return false;
    }

    protected void onObjectLongClicked() {
	}

	public void performClick() {
        if (HomeUtils.DEBUG) {
            Log.d(HomeUtils.TAG, "SEObject ^_^ ^_^ ^_^ performClick()");
        }
        if (mOnClickListener != null) {
            getAudioManager().playSoundEffect(SoundEffectConstants.CLICK);
            mOnClickListener.run(this);
        }
    }

    public void performDoubleClick() {
        if (HomeUtils.DEBUG) {
            Log.d(HomeUtils.TAG, "SEObject ^_^ ^_^ ^_^ performDoubleClick()");
        }
        if (mOnDoubleClickListener != null) {
            getAudioManager().playSoundEffect(SoundEffectConstants.CLICK);
            mOnDoubleClickListener.run(this);
        }
    }

    public void removeLongClick() {
        removeCallbacks();
    }

    private void removeCallbacks() {
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    private void removeDoublieClick() {
        if (mDoublieClickTimer !=  null) {
            mDoublieClickTimer.cancel();
        }
    }

    private void postCheckForLongClick() {
        mHasPerformedLongPress = false;
        mCheckForLongPress = new CheckForLongPress();
        postDelayed(mCheckForLongPress, ViewConfiguration.getLongPressTimeout());
    }

    private void postCheckForDoubleClick() {
        mCheckForDoubleClick = new CheckForDoubleClick();
        mDoublieClickTimer = new Timer();
        mDoublieClickTimer.schedule(mCheckForDoubleClick, ViewConfiguration.getDoubleTapTimeout());
    }

    private void postDelayed(TimerTask task, long delayMillis) {
        mTimer = new Timer();
        mTimer.schedule(task, delayMillis);
    }

    public boolean hasPerformedLongPress() {
        return mHasPerformedLongPress;
    }

    class CheckForLongPress extends TimerTask {
        public void run() {
            new SECommand(getScene()) {
                public void run() {
                    if (isPressed()) {
                        if (performLongClick()) {
                            mHasPerformedLongPress = true;
                        }
                    }
                }
            }.execute();
        }
    }

    class CheckForDoubleClick extends TimerTask {
        public void run() {
            new SECommand(getScene()) {
                public void run() {
                    mClickCount = 0;
                }
            }.execute();
        }
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (!(obj instanceof SEObject)) {
            return false;
        }
        SEObject newobj = (SEObject) obj;
        if (mName.equals(newobj.mName) && mIndex == newobj.mIndex) {
            return true;
        } else {
            return false;
        }

    }
    public void onActivityRestart() {

    }
    
    public void onActivityResume() {

    }

    public void onActivityPause() {

    }

    public void onActivityDestory() {

    }

    public boolean hasBeenReleased() {
        return mHasBeenReleased;
    }

    protected static boolean mSoftRelease;
    
    public void onRelease() {
        mHasBeenReleased = true;
    }
    
    public final void setSoftRelease(boolean soft) {
    	mSoftRelease = soft;
    }
    
    public void onRemoveFromParent(SEObject parent) {

    }
    public void onPressHomeKey() {

    }
    
    private class ObjectPressedAnimation extends CountAnimation {

        public ObjectPressedAnimation(SEScene scene) {
            super(scene);
        }

        public void runPatch(int times) {
            mPressValue = mPressValue - 0.05f;
            if (mPressValue <= 0.5f) {
                mPressValue = 0.5f;
                stop();
            } else {
                if (mPressType == PRESS_TYPE.ALPHA) {
                    setAlpha(mPressValue, true);
                } else {
                    setUseUserColor(mPressValue, mPressValue, mPressValue);
                }
            }
        }

        @Override
        public void onFinish() {
            mPressValue = 0.5f;
            if (mPressType == PRESS_TYPE.ALPHA) {
                setAlpha(mPressValue, true);
            } else {
                setUseUserColor(mPressValue, mPressValue, mPressValue);
            }
        }

        @Override
        public void onFirstly(int count) {
            if (mPressType == PRESS_TYPE.ALPHA) {
                if (!mHasGetBlending) {
                    mIsEnbaleBlending = isBlendingable();
                    mHasGetBlending = true;
                }
                if (!mIsEnbaleBlending) {
                    setBlendingable(true, true);
                }
            }
            if (!isVisible()) {
                stop();
            }
        }

    }

    private class ObjectUnpressedAnimation extends CountAnimation {

        public ObjectUnpressedAnimation(SEScene scene) {
            super(scene);
        }

        public void runPatch(int times) {
            mPressValue = mPressValue + 0.05f;
            if (mPressValue >= 1) {
                mPressValue = 1;
                stop();
            } else {
                if (mPressType == PRESS_TYPE.ALPHA) {
                    setAlpha(mPressValue, true);
                } else {
                    setUseUserColor(mPressValue, mPressValue, mPressValue);
                }
            }
        }

        @Override
        public void onFirstly(int count) {
            if (mPressType == PRESS_TYPE.ALPHA) {
                if (!mHasGetBlending) {
                    mIsEnbaleBlending = isBlendingable();
                    mHasGetBlending = true;
                }
            }
            if (!isVisible()) {
                stop();
            }
        }

        public void onFinish() {
            mPressValue = 1;
            if (mPressType == PRESS_TYPE.ALPHA) {
                setAlpha(mPressValue, true);
                if (mHasGetBlending) {
                    if (!mIsEnbaleBlending) {
                        setBlendingable(false, true);
                    } else {
                        setBlendingable(true, true);
                    }
                }
            } else {
                clearUserColor();
            }
        }
    }
    /*
    public boolean isVesselWithMountPoint() {
    	    SEMountPointManager mountPointManager = mScene.getMountPointManager();
    	    SEMountPointChain mpc = mountPointManager.getVesselDefaulMountPointChain(mName, null);
    	    if(mpc == null)
    		    return false;
    	    else
    		    return true;
    }
    */
    public String getMountPointGroupName() {
        	SEMountPointManager mountPointManager = mScene.getMountPointManager();
    	    String groupName = mountPointManager.getMountPointGroupName(mName);
    	    return groupName;
    }
    public interface SEObjectTravel {
        public abstract boolean travel(SEObject obj);
    }


    public SEVector3f getObjectXYZSpan() {
        createLocalBoundingVolume();
        SEVector3f objectMinPoint = new SEVector3f();
        SEVector3f objectMaxPoint = new SEVector3f();
        getLocalBoundingVolume(objectMinPoint, objectMaxPoint);
        SEVector3f xyzSpan = getXYZSpanFromMinMaxPoint(objectMinPoint, objectMaxPoint);
        return xyzSpan;
    }

    public static SEVector3f getXYZSpanFromMinMaxPoint(SEVector3f minPoint, SEVector3f maxPoint) {
        float xspan = Math.abs(maxPoint.getX() - minPoint.getX());
        float yspan = Math.abs(maxPoint.getY() - minPoint.getY());
        float zspan = Math.abs(maxPoint.getZ() - minPoint.getZ());
        return new SEVector3f(xspan, yspan, zspan);
    }
}
