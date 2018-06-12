package com.borqs.se.engine;

import java.util.ArrayList;
import java.util.List;

import com.borqs.se.home3d.HomeActivity;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.engine.SEObject.SEObjectTravel;
import com.borqs.se.widget3d.NormalObject;
import com.borqs.framework3d.home3d.SEMountPointManager;
import com.borqs.se.widget3d.WallShelf;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

public abstract class SEScene {
    private static final boolean DEBUG = true || HomeUtils.DEBUG;

    public final static int LIGHT_TYPE_POINT_LIGHT = 0;
    public final static int LIGHT_TYPE_DIRECT_LIGHT = 1;
    public final static int LIGHT_TYPE_SPOT_LIGHT = 2;
    public static final int STATUS_DISALLOW_TOUCH = 0x00000001;
    public static final int STATUS_OPTION_MENU = STATUS_DISALLOW_TOUCH << 1;
    public static final int STATUS_APP_MENU = STATUS_OPTION_MENU << 1;
    public static final int STATUS_OBJ_MENU = STATUS_APP_MENU << 1;
    public static final int STATUS_HELPER_MENU = STATUS_OBJ_MENU << 1;
    public static final int STATUS_MOVE_OBJECT = STATUS_HELPER_MENU << 1;
    public static final int STATUS_ON_DESK_SIGHT = STATUS_MOVE_OBJECT << 1;
    public static final int STATUS_ON_SKY_SIGHT = STATUS_ON_DESK_SIGHT << 1;
    public static final int STATUS_ON_WIDGET_SIGHT = STATUS_ON_SKY_SIGHT << 1;
    public static final int STATUS_ON_SCALL = STATUS_ON_WIDGET_SIGHT << 1;
    public static final int STATUS_ON_UNLOCK_ANIMATION = STATUS_ON_SCALL << 1;
    public static final int STATUS_ON_WALL_DIALOG = STATUS_ON_UNLOCK_ANIMATION << 1;

    private boolean mIsShowShelf = true;

    public interface SESensorEventListener {
        public void onAccuracyChanged(Sensor sensor, int accuracy);

        public void onSensorChanged(SensorEvent event);
    }

    public native void create_JNI();

    private native void release_JNI();

    public native void setIsTranslucent_JNI(boolean translucent);

    // It will be drawed at first, if a translucent scene

    public native void setNeedDraw_JNI(boolean needDraw);

    public native void setVisibility_JNI(boolean visibility);

    private native void setRoot_JNI(String rootName, int index);

    /**
     * @param object
     *            object's pointer
     */
    public native void setRoot_JNI(int object);

    public void setRoot(SEObject object) {
        setRoot_JNI(object.mName, object.mIndex);
    }
    //loadResource will create SE_Spatial for object with data path
    public native static int loadResource_JNI(String scenePath, String dataPath);
    //inflateResource will create mesh data from object cbf file
    public native String inflateResource_JNI(int resource, String Objname, int ObjIndex, String nodeName, int nodeIndex, String vesselMountPointGroupName);

    public native void setShadowMapCamera_JNI(float[] location, float[] axisZ, float[] up, float fov, float ratio,
            float near, float far);

    /**
     * @param type
     *            is 0,1 or 2, 2 is spot light, 0 is point light, 1 is direction
     *            light.
     * @param pos
     *            point light has position(world coordinate) only,pos[3] is
     *            attenuation parameter,from 0 to 1.0,0 means no attenuation.
     *            (no need set param dir,just set it to (0,0,0))
     * @param dir
     *            direction light has direction(world coordinate) only. (no need
     *            set param pos,just set it to (0,0,0)) if the light direction
     *            is from (0,0,1) to (0,0,0),the dir should be (0,0,-1);
     * @param spotdata
     *            spotdata is a vec4 struct, x is cutoff value from 0.1 to 0.9,
     *            0.1 means a little spot light angle,0.9 means a larger, y is
     *            exp value(a attenuation) min-exp = 0.0, no attenuation. z,w is
     *            not use now.
     */
    private native void addLightToScene_JNI(String lightName, float[] pos, float[] dir, float[] spotdata, int type);

    public native void removeLightFromScene_JNI(String lightName);

    public void addPointLightToScene(String lightName, float[] pos, float para) {
        addLightToScene_JNI(lightName, new float[] { pos[0], pos[1], pos[2], para }, new float[] { 0, 0, 0 },
                new float[] { 0, 0, 0, 0 }, 0);
    }

    public void addDirectLightToScene(String lightName, float[] dir) {
        addLightToScene_JNI(lightName, new float[] { 0, 0, 0 }, dir, new float[] { 0, 0, 0, 0 }, 1);
    }

    public void addSpotLightToScene(String lightName, float[] dir, float[] pos, float[] vec4spotdata) {
        addLightToScene_JNI(lightName, pos, dir, vec4spotdata, 2);
    }

    /**
     * @param lowestEnvBrightness
     *            is lowest environment brightness that there is no light reach
     *            0.0 is black, 1.0 is texture color;
     */
    public native void setLowestBrightness_JNI(float lowestEnvBrightness);

    public native void changeSceneShader_JNI(String shaderName, String renderName);
    public static native void getLocalTransformByObjectName_JNI(String sceneName, String objName, int objIndex, float[] outData);
    public native static void removeAllLight_JNI(int type);

    public static void removeAllLight() {
        removeAllLight_JNI(LIGHT_TYPE_POINT_LIGHT);
        removeAllLight_JNI(LIGHT_TYPE_DIRECT_LIGHT);
        removeAllLight_JNI(LIGHT_TYPE_SPOT_LIGHT);
    }

    public native void updateSceneLightPos_JNI(String lightName, float[] pos);

    public native void updateSceneLightDir_JNI(String lightName, float[] dir);

    public native void updateSceneLightSpotData_JNI(String lightName, float[] vec4spotdata);

    /**
     * when occur geometry change it's scene,new scene should invoke this
     * function to reset the geometry use new scene light status
     */
    public native void refreshSceneLightStatus_JNI();

    /* SceneManagerType */
    public native void setSceneType_JNI(int type);

    public native void setSceneDepthType_JNI(int max_depth, int type);

    /* Particle */
    public native void createParticleObject_JNI(int effectIndex, float[] cameraPos, String mainImagePath,
            String helpImagePath);

    public native void deleteParticleObject_JNI(int effectIndex);

    public String mSceneName;
    public SECamera mSECamera;
    public SESceneInfo mSceneInfo;
    public List<SESensorEventListener> mSESensorEventListeners;
    private SEObject mContectObject;
    public Context mContext;
    public SESceneManager mSESceneManager;
    public SEEventQueue mEventQueue;
    private SEScene mStartScene;
    private List<SEScene> mChildScenes;
    private SEObject mDownHitObject;
    private SEObject mMenuObject;
    private SEMountPointManager mSceneMountPointManager;
    /**
     * The delegate to handle touch events that are physically in this view but
     * should be handled by another object.
     */
    private SEObject mTouchDelegate = null;
    public boolean mHasBeenReleased;

    public SEScene(String name) {
        mSceneName = name;
        mSECamera = new SECamera(this);
        mSESensorEventListeners = new ArrayList<SESensorEventListener>();
        mChildScenes = new ArrayList<SEScene>();
        mSESceneManager = SESceneManager.getInstance();
        mContext = mSESceneManager.getContext();
        mEventQueue = new SEEventQueue();
        mHasBeenReleased = true;
        mIsShowShelf = SettingsActivity.isShowShelf(mContext);
    }
    public void createMountPointManager() {
    	    mSceneMountPointManager = new SEMountPointManager(this);
    	
    	    mSceneMountPointManager.loadFromXml("assets", "base", mContext);
    }
    public SEMountPointManager getMountPointManager(){
    	    return mSceneMountPointManager;
    }
    //outData must be float array which has 10 elements
    public void getLocalTransformsByObjectName(String objectName, int objectIndex, float[] outData) {
    	    getLocalTransformByObjectName_JNI(mSceneName, objectName, objectIndex, outData);
    }
    public void removeMessage(int id) {
        mEventQueue.removeEvent(id);
    }
    public abstract void handleMessage(int type, Object message);

    public void setStatus(int type, boolean status) {
        if (status) {
            mSceneInfo.mStatus |= type;
        } else {
            mSceneInfo.mStatus &= ~type;
        }
    }

    public boolean getStatus(int type) {
        return (mSceneInfo.mStatus & type) != 0;
    }

    public int getStatus() {
        return mSceneInfo.mStatus;
    }

    public void addSESensorEventListener(SESensorEventListener listener) {
        if (listener != null && !mSESensorEventListeners.contains(listener)) {
            mSESensorEventListeners.add(listener);
        }
    }

    public void removeSESensorEventListener(SESensorEventListener listener) {
        if (listener != null && !mSESensorEventListeners.contains(listener)) {
            mSESensorEventListeners.remove(listener);
        }
    }

    public void clearSESensorEventListener() {
        mSESensorEventListeners.clear();
    }

    public Context getContext() {
        return mContext;
    }

    public void startScene(SEScene scene) {
        mStartScene = scene;
        create_JNI();
        notifySurfaceChanged(mSESceneManager.getWidth(), mSESceneManager.getHeight());
        onSceneStart();
    }

    public void onSceneStart() {

    }

    public void release() {
        onRelease(false);
        release_JNI();
        mHasBeenReleased = true;
    }
    
    public void softRelease() {
    	onRelease(true);
        release_JNI();
        mHasBeenReleased = true;
    }
    
    
    public void delay() {

    }

    public void addChildScene(SEScene scene) {
        if (!mChildScenes.contains(scene)) {
            mChildScenes.add(scene);
            scene.startScene(null);
            scene.setIsTranslucent_JNI(true);
        }
    }

    public void removeChildScene(SEScene scene) {
        if (mChildScenes.contains(scene)) {
            mChildScenes.remove(scene);
            scene.release();
        }
    }

    public SEEventQueue getEventQuene() {
        return mEventQueue;
    }

    public SEObject getContentObject() {
        return mContectObject;
    }

    public SEObject findObject(String name, int index) {
        if (getContentObject() == null) {
            return null;
        }
        if (index == getContentObject().mIndex && getContentObject().getName().equals(name)) {
            return getContentObject();
        }
        return getContentObject().findObject(name, index);
    }

    public SEObject findObject(String type) {
        final String objectType = type;
        SEObjectTravel travel = new SEObjectTravel() {
            public boolean travel(SEObject obj) {
                if (obj instanceof NormalObject) {
                    NormalObject normalObject = (NormalObject) obj;
                    if (normalObject.getObjectInfo() == null) {
                        return false;
                    }
                    if (objectType.equals(normalObject.getObjectInfo().mType)) {
                        return true;
                    }
                }
                return false;
            }
        };
        return getContentObject().travelObject(travel);
    }

    public SEObject findFirstObjectByClass(final String className) {
            SEObjectTravel travel = new SEObjectTravel() {
                public boolean travel(SEObject obj) {
                    if (ModelInfo.isObjectInstance(className, obj)) {
                        return true;
                    }
                    return false;
                }
            };
            return getContentObject().travelObject(travel);
    }

    public SECamera getCamera() {
        return mSECamera;
    }
    public boolean isShelfVisible() {
        return mIsShowShelf;
    }
    public void setShelfVisibility(boolean b) {
        mIsShowShelf = b;
    }
    //this fucntion must run in render thread
    private class ShelfVisibilityTravel implements SEObjectTravel {
        public ArrayList<WallShelf> shelfList = new ArrayList<WallShelf>();
        public boolean travel(SEObject obj) {
            if(obj instanceof WallShelf) {
                WallShelf shelf = (WallShelf)obj;
                shelf.setVisible(mIsShowShelf, true);
                if(mIsShowShelf == false) {
                    int realNum = shelf.getRealObjectNumOnShelf();

                        if(realNum == 0) {
                            //shelf.getParent().removeChild(shelf, true);
                            shelfList.add(shelf);
                        }

                }
            }
            return false;
        }
    }
    protected void setShelfVisibility() {
        if (null == getContentObject()) return;
        
        ShelfVisibilityTravel travel = new ShelfVisibilityTravel();
        getContentObject().travelObject(travel);
        for(WallShelf shelf : travel.shelfList) {
            shelf.getParent().removeChild(shelf, true);
        }
    }
    public void setContentObject(SEObject contentObject) {
        mContectObject = contentObject;
        mContectObject.setCamera(mSECamera);
        mContectObject.setScene(this);
        setRoot(contentObject);
    }
    public void setObjectsMenu(SEObject menu) {
        mMenuObject = menu;
        mContectObject.addChild(mMenuObject, true);
    }
    
    public SEObject getObjectsMenu() {
        return mMenuObject;
    }
    

    public boolean update() {
        return mEventQueue.render();
    }

    public void notifySurfaceChanged(int width, int height) {
        if (mSECamera != null) {
            mSECamera.notifySurfaceChanged(width, height);
        }
        if (mContectObject != null) {
            mContectObject.notifySurfaceChanged(width, height);
        }

//        calculateCameraRadiusScope();
//        mSECamera.setFov(SECameraData.mBestCameraFov);
        mSECamera.setCamera();
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (getStatus(STATUS_DISALLOW_TOUCH)) {
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mDownHitObject = getCamera().getSelectedObject_JNI((int) event.getX(), (int) event.getY());
            if (HomeUtils.DEBUG && mDownHitObject != null) {
                Log.d(HomeUtils.TAG, "Hit object name = " + mDownHitObject.mName + "; index = "
                        + mDownHitObject.mIndex);
            }
        }
        if (dispatchEventToOptionMenu(event)) {
            return true;
        }
        if (mTouchDelegate != null) {
            mTouchDelegate.dispatchTouchEvent(event);
            return true;
        }
        if (mContectObject != null) {
            mContectObject.dispatchTouchEvent(event);
            return true;
        }
        return true;
    }

    abstract public boolean dispatchEventToOptionMenu(MotionEvent event);

    public SEObject getDownHitObject() {
        return mDownHitObject;
    }

    public void setDownHitObject(SEObject obj) {
        mDownHitObject = obj;
    }

    public boolean handleBackKey(SEAnimFinishListener l) {
        if (getStatus(STATUS_DISALLOW_TOUCH)) {
            return false;
        }
        boolean result = false;
        if (mContectObject != null) {
            result = mContectObject.handleBackKey(l);
        }
        return result;
    }

    public void handleMenuKey() {

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mContectObject != null) {
            mContectObject.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onActivityPause() {
        if (mContectObject != null) {
            mContectObject.onActivityPause();
        }
    }

    public void onActivityRestart() {
        if (mContectObject != null) {
            mContectObject.onActivityRestart();
        }
    }

    public void onActivityResume() {
        if (mContectObject != null) {
            mContectObject.onActivityResume();
        }
    }

    public void onNewIntent(Intent intent) {

    }

    public void onActivityDestory() {
        if (mContectObject != null) {
            mContectObject.onActivityDestory();
        }
    }

    public void onRelease(boolean softReset) {
        if (mContectObject != null) {
        	mContectObject.setSoftRelease(softReset);
            mContectObject.onRelease();
        }
        mEventQueue.clear();
    }

    public boolean onMenuOpened(int featureId, Menu menu) {
        return false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    public Dialog onCreateDialog(int id) {
        return null;
    }

    public void onPrepareDialog(int id, Dialog dialog, Bundle bundles) {

    }

    public final void showDialog(final int id) {
        mSESceneManager.runInUIThread(new Runnable() {
            public void run() {
            	if (((HomeActivity)mSESceneManager.getGLActivity()).isLiving()) {
                    mSESceneManager.getGLActivity().showDialog(id);
                }
            }
        });
    }

    public final void showDialog(final int id, final Bundle bundle) {
        mSESceneManager.runInUIThread(new Runnable() {
            public void run() {
                //if activity have been destroyed, no need pop up the dialog.
                if (((HomeActivity)mSESceneManager.getGLActivity()).isLiving()) {
                    try {
                        mSESceneManager.getGLActivity().showDialog(id, bundle);
                    } catch (Exception e) {
                        //still get BadTokenException sometimes.
                    }

                }
            }
        });
    }
    public final void removeDialog(final int id) {
        mSESceneManager.runInUIThread(new Runnable() {
            public void run() {
            	try {
                    mSESceneManager.getGLActivity().removeDialog(id);
                } catch (Exception e) {
                }
            }
        });
    }

    public final void dissMissDialog(final int id) {
        mSESceneManager.runInUIThread(new Runnable() {
            public void run() {
            	try {
                    mSESceneManager.getGLActivity().dismissDialog(id);
                } catch (Exception e) {
                }
            }
        });
    }

    /**
     * Sets the TouchDelegate for this Object.
     */
    public void setTouchDelegate(SEObject delegate) {
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
        dispatchTouchEvent(event);
        mTouchDelegate = delegate;
    }

    public void changeTouchDelegate(SEObject delegate) {
        mTouchDelegate = delegate;
    }

    public void removeTouchDelegate() {
        mTouchDelegate = null;
    }

    /**
     * Gets the TouchDelegate for this View.
     */
    public SEObject getTouchDelegate() {
        return mTouchDelegate;
    }

//    public void calculateCameraRadiusScope() {
//    }
}
