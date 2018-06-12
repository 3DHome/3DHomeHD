package com.borqs.se.home3d;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import com.borqs.framework3d.home3d.DockObject;
import com.borqs.framework3d.home3d.HouseObject;
import com.borqs.market.json.Product.SupportedMod;
import com.borqs.market.utils.MarketUtils;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SECommand;
import com.borqs.se.localloader.SDCardLoader;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectGroup;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.widget3d.NormalObject;
import com.borqs.se.widget3d.ObjectInfo;
import com.borqs.se.widget3d.ShowBox;

public class ObjectsMenu extends SEObjectGroup {

    private static final String TAG = "ObjectsMenu";
    private VelocityTracker mVelocityTracker;
    private VelocityAnimation mVelocityAnimation;
    private ShowAnimation mShowAnimation;
    private HideAnimation mHideAnimation;
    private Context mContext;
    private float mTranslateX;
    private float mTranslateX_MIN;
    private float mTranslateX_MAX;
    private OnTouchListener mLongClickListener;
    private DockObject mDockObject;
    private boolean mDisableTouch;
    private List<ObjectsMenuItem> mCloneObjects;
    private boolean mIsReBuild;

    private ModelInfo mShowBoxInfo;
    private List<ShowBox> mBackgroudList = new ArrayList<ShowBox>();
    private int SHOW_BOX_LINE ;
    private int SHOW_BOX_COLUMN ;
    private float mBoxLengthX;

    private float mShelfHeightOffset;
    public ObjectsMenu(SEScene scene, String name) {
        super(scene, name, 0);
        mShelfHeightOffset = scene.mSceneInfo.mObjectShelfSceneInfo.mDeskHeight;
        mContext = SESceneManager.getInstance().getContext();
        mDisableTouch = true;
        if(SettingsActivity.getPreferRotation(mContext) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
        	SHOW_BOX_LINE = 4;
        	SHOW_BOX_COLUMN = 2;
		}else {
			SHOW_BOX_LINE = 2;
        	SHOW_BOX_COLUMN = 4;
		}
        setOnLongClickListener(new OnTouchListener() {
            public void run(SEObject obj) {
                String objName = (String) obj.getTag();
                ModelInfo modelInfo = getScene().mSceneInfo.findModelInfo(objName);
                ObjectInfo objInfo = new ObjectInfo();
                objInfo.setModelInfo(modelInfo);
                objInfo.mIndex = ProviderUtils.searchMaxIndex(getScene(), ProviderUtils.Tables.OBJECTS_INFO, objName) + 1;
                objInfo.mSceneName = getScene().mSceneName;
                objInfo.saveToDB();
                SETransParas startTransParas = new SETransParas();
                startTransParas.mTranslate = obj.getAbsoluteTranslate();
                NormalObject newObject = HomeUtils.getObjectByClassName(getScene(), objInfo);
                SEObject mother = findObject(objInfo.mName, 0);
                newObject.setIsFresh(true);
                mother.cloneObject_JNI(getScene().getContentObject(), objInfo.mIndex, false, objInfo.mModelInfo.mStatus);
                newObject.initStatus(getScene());
                newObject.setHasInit(true);
                modelInfo.register(newObject);
                hide(true, null);
                newObject.setTranslate(startTransParas.mTranslate, true);
                newObject.setTouch(obj.getTouchX(), obj.getTouchY());
                newObject.setStartTranspara(startTransParas);
                newObject.setOnMove(true);
            }
        });
    }

    public void loadPreLoadModel(final Runnable finish) {
        Iterator<Entry<String, ModelInfo>> iter = getScene().mSceneInfo.mModels.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, ModelInfo> entry = iter.next();
            final ModelInfo modelInfo = entry.getValue();
            if (ModelInfo.isObjectShelfHidden(modelInfo.mType) || modelInfo.mType.equals("IconBox")) {
//            if (modelInfo.mType.equals("Folder") || modelInfo.mType.equals("Recycle")
//                    || modelInfo.mType.equals("IconBox") || modelInfo.mType.equals("shop")
//                    || modelInfo.mType.equals("walldialog")) {
                if (!modelInfo.hasInstance()) {
                    final ObjectsMenuItem menuItem = modelInfo.createMenuInstance(getScene(), this);
                    if (null != menuItem) {
                        menuItem.loadObject(new Runnable() {
                            public void run() {
                                menuItem.setVisible(false, true);
                            }
                        });
                    }
                } else {
                    final ObjectsMenuItem menuItem = modelInfo.createMenuInstance(getScene(), this);
                    if (null == menuItem) {
                        Log.e(TAG, "loadPreLoadModel, skip without menu instance " + modelInfo.mName);
                    } else {
                        menuItem.setVisible(false, true);
                    }
                }
            }
        }
        SELoadResThread.getInstance().process(new Runnable() {
            public void run() {
                new SECommand(getScene()) {
                    public void run() {
                        if (finish != null) {
                            finish.run();
                        }
                    }
                }.execute();
            }
        });
    }

    private void trackVelocity(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        final VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000);
    }

    @Override
    public void setOnLongClickListener(OnTouchListener l) {
        mLongClickListener = l;
    }

    @Override
    public void onRenderFinish(SECamera camera) {
        super.onRenderFinish(camera);
        setVisible(false, true);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mDisableTouch) {
            return true;
        }

        return super.dispatchTouchEvent(event);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            setPreTouch();
            if ((mShowAnimation != null && !mShowAnimation.isFinish())
                    || (mVelocityAnimation != null && !mVelocityAnimation.isFinish())
                    || (mHideAnimation != null && !mHideAnimation.isFinish())) {
                stopAllAnimation(null);
                return true;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (Math.abs(getPreTouchX() - getTouchX()) > getTouchSlop() / 2) {
                setPreTouch();
                stopAllAnimation(null);
                return true;
            }
            break;
        }
        return false;
    }

    private float mLastMove;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        trackVelocity(ev);
        switch (ev.getAction()) {
        case MotionEvent.ACTION_MOVE:
            float dx = getTouchX() - getPreTouchX();
            mTranslateX = mTranslateX + dx;
            mLastMove += dx;
            if (mTranslateX < mTranslateX_MIN - 100) {
                mTranslateX = mTranslateX_MIN - 100;
            } else if (mTranslateX > mTranslateX_MAX + 100) {
                mTranslateX = mTranslateX_MAX + 100;
            }
            setTranslate(new SEVector3f(mTranslateX, 0, mShelfHeightOffset), true);

            setPreTouch();
            break;
        case MotionEvent.ACTION_UP:
            setPreTouch();
        case MotionEvent.ACTION_CANCEL:
//            Log.v(TAG," mVelocityTracker.getXVelocity()="+mVelocityTracker.getXVelocity() +" mVelocityTracker.getYVelocity()="+mVelocityTracker.getYVelocity());
            mVelocityAnimation = new VelocityAnimation(getScene(), mVelocityTracker.getXVelocity());
            mVelocityAnimation.execute();
            break;
        }
        return true;
    }

    public void show() {
        if (!getScene().getStatus(SEScene.STATUS_OBJ_MENU)) {
            mDisableTouch = true;
            stopAllAnimation(null);
            init();
            getScene().setStatus(SEScene.STATUS_OBJ_MENU, true);
            getScene().setTouchDelegate(this);
            if (null == mDockObject) {
                mDockObject = ModelInfo.getDockObject(getScene());
            }
            performShowAction(null);
        }
    }

    public void hide(boolean fast, final SEAnimFinishListener l) {
        if (getScene().getStatus(SEScene.STATUS_OBJ_MENU)) {

            stopAllAnimation(null);
            mDisableTouch = true;
            getScene().removeTouchDelegate();
            if (fast) {
                onHidden(l);
            } else {
                mHideAnimation = new HideAnimation(getScene());
                mHideAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                    public void onAnimationfinish() {
                        onHidden(l);
                    }
                });
                mHideAnimation.execute();
            }

        }
    }

    public boolean handleBackKey(SEAnimFinishListener l) {
        if (getScene().getStatus(SEScene.STATUS_OBJ_MENU)) {
            hide(false, l);
            return true;
        }
        return false;
    }

    @Override
    public void stopAllAnimation(SEAnimFinishListener l) {
        if (mShowAnimation != null) {
            mShowAnimation.stop();
        }
        if (mHideAnimation != null) {
            mHideAnimation.stop();
        }
        if (mVelocityAnimation != null) {
            mVelocityAnimation.stop();
        }
        if (mDockObject != null) {
            mDockObject.stopAllAnimation(null);
        }
        getCamera().stopAllAnimation();
    }

    private class ShowAnimation extends CountAnimation {
//        private int mRotateX = 0;
//        private int mRotateY = 0;
//        private int mRotateZ = 0;
        public ShowAnimation(SEScene scene) {
            super(scene);
//            Random random = new Random();
//            mRotateX = random.nextInt(2);
//            mRotateY = random.nextInt(2);
//            mRotateZ = random.nextInt(2);
        }

        public void runPatch(int count) {
//            int size = mCloneObjects.size();
//            if (count == getAnimationCount()) {
//                for (int i=0; i< size; i++) {
//                    ObjectsMenuItem cloneObj = (ObjectsMenuItem) mCloneObjects.get(i);
//                    cloneObj.setUserTransParas();
//                }
//            }
//            for (int i= 0; i < SHOW_BOX_LINE * SHOW_BOX_COLUMN; i++) {
//                ShowBox showBox = mBackgroudList.get(i);
//                showBox.getUserTransParas().mRotate.set(360/getAnimationCount() * count, mRotateX, mRotateY, mRotateZ);
//                showBox.setUserTransParas();
//            }
        }

        @Override
        public void onFirstly(int count) {
        	SESceneManager ma = SESceneManager.getInstance();
        	int screenWidth = ma.getScreenWidth();
        	int screenHeight = ma.getScreenHeight();
        	SECamera camera = getCamera();
            float fov = camera.getFov();
            SEVector3f cameraLoc = camera.getLocation();
            SEVector3f defaultLoc = getScene().mSceneInfo.getDefaultCameraData().mLocation;
            float ratio = Math.abs(cameraLoc.getY() / defaultLoc.getY());
            
            if(SettingsActivity.getPreferRotation(mContext) == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            	ratio = ratio * screenWidth/screenHeight;
            }
//            Log.v(TAG," getCamera().getLocation()="+getCamera().getLocation());
//            Log.v(TAG," getCamera().getScreenOrientation()="+getCamera().getScreenOrientation() +" h="+getCamera().getHeight()+" w="+getCamera().getWidth());
//            Log.v(TAG," cameraLoc="+cameraLoc);

            float realBoxInnerL = 17f;  // from the UI team.
            float realBoxBadge = 0.5f; // from the UI team.
            float boxYOffset = 300f; // If it is 0 ,some part will be hided by the ground when the camera's fov become bigger.
            float boxLengthX = (Math.abs(cameraLoc.getY()+ boxYOffset) * (float)Math.tan(fov * Math.PI / 360)) * 2 / SHOW_BOX_LINE;
            float boxLengthZ = ((float)screenHeight / screenWidth) * (boxLengthX * SHOW_BOX_LINE) / SHOW_BOX_COLUMN;
            float boxScaleX = boxLengthX * SHOW_BOX_LINE /(realBoxInnerL * SHOW_BOX_LINE + (SHOW_BOX_LINE + 1) * realBoxBadge);
            float boxScaleZ = boxLengthZ * SHOW_BOX_COLUMN / (realBoxInnerL * SHOW_BOX_COLUMN + (SHOW_BOX_COLUMN + 1) * realBoxBadge);
            float boxScaleY = boxScaleX / 2;
            float boxLengthY = (realBoxInnerL + 2 *realBoxBadge) * boxScaleY;
            mBoxLengthX = boxLengthX;
            mTranslateX = 0;
            mTranslateX_MAX = 0;
            int size = mBackgroudList.size();
            int a = size %  SHOW_BOX_COLUMN;
            int b = 1;
            if (a == 0) {
                b = 0;
            }
            mTranslateX_MIN = -((size / SHOW_BOX_COLUMN + b - SHOW_BOX_LINE) * boxLengthX);
//            Log.v(TAG," min="+mTranslateX_MIN +" max="+mTranslateX_MAX +" a="+a+" b="+b +" boxLengthX="+boxLengthX);
//            Log.v(TAG," screeng height="+SESceneManager.getInstance().getScreenHeight() +"  w="+SESceneManager.getInstance().getScreenWidth() +"   boxLengthX=" + boxLengthX +"   "+ boxScaleX +"  "+boxLengthZ  +"  " +boxScaleZ);
//            Log.v(TAG," boxLengthX="+boxLengthX+" boxLengthZ="+boxLengthZ);
            setTranslate(new SEVector3f(0, 0, mShelfHeightOffset), true);

            HouseObject houseObject = ModelInfo.getHouseObject(getScene());
            for (int i = 0; i < size; i++ ) {
                float x = i / SHOW_BOX_COLUMN * boxLengthX - (boxLengthX * ((SHOW_BOX_LINE -1) / 2f));
                float y = 0;
                float z = cameraLoc.getZ() - mShelfHeightOffset + boxLengthZ * ((SHOW_BOX_COLUMN - 1) / 2f - i %SHOW_BOX_COLUMN);

                ShowBox showBox = mBackgroudList.get(i);
                showBox.getUserTransParas().mTranslate.set(x, y - boxYOffset, z);
                showBox.getUserTransParas().mScale = new SEVector3f(boxScaleX, boxScaleY,boxScaleZ);
                showBox.setUserTransParas();

                if (i >= mCloneObjects.size()) {
                    continue;
                }
                ObjectsMenuItem cloneObj = (ObjectsMenuItem) mCloneObjects.get(i);
                SETransParas previewTrans = cloneObj.mModelInfo.mPreviewTrans;
                //clone obj set translate
                cloneObj.getUserTransParas().mTranslate.set(x , boxLengthY/2 - boxYOffset, z);
                if (cloneObj.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_DESKTOP ) {
                	// if(previewTrans != null) {
                	// cloneObj.getUserTransParas().mTranslate = new SEVector3f(previewTrans.mTranslate);
                	// }
                	if (!"group_contact".equals(cloneObj.mModelInfo.mName)) {
                	cloneObj.getUserTransParas().mTranslate.selfAdd(new SEVector3f(0,0, -boxLengthZ/2));
                	} else {
                	cloneObj.getUserTransParas().mTranslate.selfAdd(new SEVector3f(0,0, -boxLengthZ/3));
                	}
                	if (i % SHOW_BOX_COLUMN == 0) {
                	cloneObj.getUserTransParas().mTranslate.selfAdd(new SEVector3f(0,0, boxLengthZ/20));
                	} else if (i % SHOW_BOX_COLUMN == 1) {
                	cloneObj.getUserTransParas().mTranslate.selfAdd(new SEVector3f(0,0, boxLengthZ/30));
                	}
                	} else if (cloneObj.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_WALL) {

                	} else if (cloneObj.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_WALL_GAP) {
                	cloneObj.getUserTransParas().mTranslate.selfAdd(new SEVector3f(0,0, -boxLengthZ/2));
                	}

                	//clone obj set rotate
                	// if ("group_contact".equals(cloneObj.mModelInfo.mName)) {
                	// cloneObj.getUserTransParas().mRotate = new SERotate(90, 1, 0, 0);
                	// }

                	//clone obj set scale
                	if (previewTrans != null) {
                	// float scaleX = previewTrans.mScale.getX() * ratio;
                	// float scaleZ = previewTrans.mScale.getZ() * ratio;
                	// cloneObj.getUserTransParas().mScale = new SEVector3f(scaleX, ratio, scaleZ);

                	cloneObj.getUserTransParas().mRotate = previewTrans.mRotate;
                	SEVector3f pScale = previewTrans.mScale.clone();  
                	pScale.selfMul(ratio);
                	cloneObj.getUserTransParas().mScale = pScale;

                	} else {
                	cloneObj.getUserTransParas().mScale = new SEVector3f(ratio, ratio, ratio);
                	}
//                if (cloneObj.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_DESKTOP ) {
//                    float scale = getScene().mSceneInfo.mDockSceneInfo.getSlotScale(realBoxInnerL * boxScaleX);
//                    cloneObj.getUserTransParas().mScale.selfMul(new SEVector3f(scale,scale,scale));
//                } else if (cloneObj.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_WALL) {
//                    int spanX = cloneObj.mModelInfo.mSpanX;
//                    int spanY = cloneObj.mModelInfo.mSpanY;
//                    float wallSpanX = houseObject.getWallUnitSizeX() * spanX;
//                    float wallSpanY = houseObject.getWallUnitSizeY() * spanY;
//                    float scale = Math.min(realBoxInnerL * boxScaleX / wallSpanX, realBoxInnerL * boxScaleZ / wallSpanY);
//                    cloneObj.getUserTransParas().mScale.selfMul(new SEVector3f(scale,scale,scale));
//                } else if (cloneObj.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_WALL_GAP) {
//                    int spanX = 1;
//                    int spanY = 2;
//                    float wallSpanX = houseObject.getWallUnitSizeX() * spanX;
//                    float wallSpanY = houseObject.getWallUnitSizeY() * spanY;
//                    float scale = Math.min(realBoxInnerL * boxScaleX / wallSpanX, realBoxInnerL * boxScaleZ / wallSpanY);
//                    cloneObj.getUserTransParas().mScale.selfMul(new SEVector3f(scale,scale,scale));
//                }
            }
            setVisible(true, true);
        }

        @Override
        public int getAnimationCount() {
            return 1;
        }
    }

    class SortByType implements Comparator<ObjectsMenuItem> {
        public int compare(ObjectsMenuItem lhs, ObjectsMenuItem rhs) {
            int value1;
            if (lhs.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_WALL_GAP) {
                value1 = 0;
            } else if (lhs.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_WALL) {
                value1 = 1;
            } else if (lhs.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_DESKTOP){
                value1 = 2;
            } else {
                value1 = 3;
            }
            int value2;
            if (rhs.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_WALL_GAP) {
                value2 = 0;
            } else if (rhs.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_WALL) {
                value2 = 1;
            } else if (rhs.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_DESKTOP){
                value2 = 2;
            } else {
                value2 = 3;
            }

            int result = value2 - value1;
            if (result == 0) {
                if ("TouchDialer".equals(lhs.mModelInfo.mType)) {
                    return -1;
                } else if ("TouchDialer".equals(rhs.mModelInfo.mType)){
                    return 1;
                } else {
                    return Collator.getInstance().compare(lhs.mModelInfo.mType, rhs.mModelInfo.mType);
                }
            }
            return result;
        }
    }

    private class HideAnimation extends CountAnimation {
        private int mSteps;
        private int mLeftNum;
        private int mVisibleB;
        private int mVisibleE;
        public HideAnimation(SEScene scene) {
            super(scene);
            mSteps  = (SHOW_BOX_LINE - SHOW_BOX_LINE / 2) * (int)mBoxLengthX / getAnimationCount();
            mLeftNum = SHOW_BOX_LINE / 2 * SHOW_BOX_COLUMN;
        }

        public void runPatch(int count) {

            for (int i = mVisibleB; i < mVisibleE; i++) {
                int x = mSteps;
                if ((i - mVisibleB) < mLeftNum ) {
                    x = -mSteps;
                }
                ShowBox showBox = mBackgroudList.get(i);
                showBox.getUserTranslate().selfAdd(new SEVector3f(x, 0, 0));
                showBox.setUserTransParas();
                if (i >= mCloneObjects.size()) {
                    continue;
                }
                ObjectsMenuItem cloneObj = (ObjectsMenuItem) mCloneObjects.get(i);
                cloneObj.getUserTranslate().selfAdd(new SEVector3f(x, 0, 0));
                cloneObj.setUserTransParas();
            }
        }

        @Override
        public void onFirstly(int count) {
            int size = mBackgroudList.size();
            int visibleLine = -1;
            if (size <= 3) {
                mTranslateX = mTranslateX_MIN;
                setTranslate(new SEVector3f(mTranslateX, 0, mShelfHeightOffset), true);
            } else {
//                Log.v(TAG," hide..........."+mTranslateX +"  "+mTranslateX_MIN +" " +mTranslateX_MAX +" " +(int)(mTranslateX / mBoxLengthX));
                if (mTranslateX < mTranslateX_MIN) {
                    mTranslateX = mTranslateX_MIN;
                } else if (mTranslateX > mTranslateX_MAX) {
                    mTranslateX = mTranslateX_MAX;
                } else {
                    int a = (int)(mTranslateX / mBoxLengthX);
                    visibleLine = Math.abs(a);
                    mTranslateX = a * mBoxLengthX;
                }
//                Log.v(TAG," onFirstly mTranslateX:"+mTranslateX +" " + (int)(mTranslateX / mBoxLengthX));
                setTranslate(new SEVector3f(mTranslateX, 0, mShelfHeightOffset), true);
            }

            if (visibleLine < 0) {
                visibleLine = (int)(Math.abs(mTranslateX) / mBoxLengthX);
            }

            mVisibleB = visibleLine * SHOW_BOX_COLUMN;
            mVisibleE = (visibleLine + SHOW_BOX_LINE) * SHOW_BOX_COLUMN;
            if (mVisibleE > size) {
                mVisibleE = size;
            }
//            for (int i=0; i< size; i++) {
//                ObjectsMenuItem cloneObj = (ObjectsMenuItem) mCloneObjects.get(i);
//                cloneObj.setUserTransParas();
//            }
//
//            for (int i= 0; i < SHOW_BOX_LINE * SHOW_BOX_COLUMN; i++) {
//                ShowBox showBox = mBackgroudList.get(i);
//                showBox.getUserTransParas().mRotate.set(0, 0, 0, 0);
//                showBox.setUserTransParas();
//            }
        }

        @Override
        public int getAnimationCount() {
            return 15;
        }
    }

    private void releaseRedundantModel() {
        for (int i = 0; i <  mCloneObjects.size(); i++) {
            SEObject object = mCloneObjects.get(i);
            ObjectsMenuItem previewObject = (ObjectsMenuItem) object;
            if (!previewObject.mModelInfo.hasInstance()
                    && !previewObject.mModelInfo.mType.equals("IconBox") 
                    /*&& !previewObject.mModelInfo.mType.equals("shop")*/) {
                removeChild(previewObject, true);
                previewObject.mModelInfo.releaseMenuItem();
            }
        }
        mCloneObjects.clear();
        for (int i = 0;i < mBackgroudList.size(); i++) {
            ShowBox showBox = mBackgroudList.get(i);
            removeChild(showBox, true);
            showBox = null;
        }
        mBackgroudList.clear();
    }

    private void init() {

        Iterator<Entry<String, ModelInfo>> iter = getScene().mSceneInfo.mModels.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, ModelInfo> entry = iter.next();
            final ModelInfo modelInfo = entry.getValue();
            if (!modelInfo.hasInstance()) {
                if (ModelInfo.isObjectShelfHidden(modelInfo.mType)  || modelInfo.mType.equals("IconBox")
                		|| "ipad".equalsIgnoreCase(modelInfo.mType) || "nexus10".equalsIgnoreCase(modelInfo.mType)
                		|| "house".equalsIgnoreCase(modelInfo.mType) || "desk".equalsIgnoreCase(modelInfo.mType)) {
//                if (modelInfo.mType.equals("Airship") || modelInfo.mType.equals("Folder")
//                        || modelInfo.mType.equals("Recycle") || modelInfo.mType.equals("IconBox")
//                        || modelInfo.mType.equals("shop") || modelInfo.mType.equals("walldialog")) {
                    continue;
                }

                if (modelInfo.mType.equals("showbox")) {
                    mShowBoxInfo = modelInfo;
                    continue;
                }

                modelInfo.createMenuInstance(getScene(), this);
            }
        }
        mCloneObjects = new ArrayList<ObjectsMenuItem>();
        int size = mChildObjects.size();
        int j =0;
        for (int i = 0; i < size; i++) {
            SEObject object = mChildObjects.get(i);
            if (object instanceof ObjectsMenuItem) {
                ObjectsMenuItem previewObject = (ObjectsMenuItem) object;
                if (ModelInfo.isObjectShelfHidden(previewObject.mModelInfo.mType) ||
                        ModelInfo.Type.ICON_BOX.equalsIgnoreCase(previewObject.mModelInfo.mType)) {
//                if (previewObject.mModelInfo.mType.equals("Folder")
//                        || previewObject.mModelInfo.mType.equals("Recycle")
//                        || previewObject.mModelInfo.mType.equals("IconBackground")
//                        || previewObject.mModelInfo.mType.equals("walldialog")) {
                    continue;
                }

                mCloneObjects.add(previewObject);
                //init the background box
                ShowBox background = new ShowBox(getScene(), mShowBoxInfo, j);
                mBackgroudList.add(background);
                j++;
            }
        }
        //Fill Full 
        int bSize = mBackgroudList.size();
        if (bSize % SHOW_BOX_COLUMN != 0) {
            int a = SHOW_BOX_COLUMN - bSize % SHOW_BOX_COLUMN;
            for (int i = 0;i< a; i++ ) {
                ShowBox background = new ShowBox(getScene(), mShowBoxInfo, bSize + i);
                mBackgroudList.add(background);
            }
        }
        Collections.sort(mCloneObjects, new SortByType());

        loadBackgroundOneByOne(0);
    }

    private void loadBackgroundOneByOne(final int position) {
        ShowBox background = mBackgroudList.get(position);
        addChild(background, false);
        background.load(this, new Runnable() {
            @Override
            public void run() {
                int newPos = position + 1;
                if (newPos < mBackgroudList.size()) {
                    loadBackgroundOneByOne(newPos);
                } 
            }
        });
    }

    private void initMenuObject(final ObjectsMenuItem cloneObj) {
        cloneObj.setIsMiniBox(true, true);
        cloneObj.setVisible(true, true);
        cloneObj.setIsEntirety_JNI(true);
        cloneObj.setLocalTranslate(new SEVector3f());
        cloneObj.setLocalScale(new SEVector3f(1, 1, 1));
        cloneObj.setLocalRotate(new SERotate(0, 0, 0, 1));
        cloneObj.setUserTransParas();
        cloneObj.setIsAlphaPress(false);
        cloneObj.setShadowObjectVisibility_JNI(false);
        cloneObj.setNeedGenerateMirror_JNI("preview_ground", 0);
        if (!"shop".equals(cloneObj.mName)) {
            cloneObj.setOnLongClickListener(new SEObject.OnTouchListener() {
                public void run(SEObject obj) {
                    if (mLongClickListener != null) {
                        obj.setPressed(false);
                        obj.setTag(cloneObj.mModelInfo.mName);
                        mLongClickListener.run(obj);
                    }
                }
            });
        }
        if ("shop".equals(cloneObj.mName)) {
            cloneObj.setOnClickListener(new OnTouchListener(){
                @Override
                public void run(SEObject obj) {
                    mIsReBuild = true;
                    MarketUtils.startProductListIntent(getScene().mContext, MarketUtils.CATEGORY_OBJECT,true,SupportedMod.PORTRAIT);
//                    Intent intent = new Intent(getScene().mContext, ObjectMarket.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    getScene().mContext.startActivity(intent);
                    hide(true, null);
                    SDCardLoader sdcardLoader = new SDCardLoader();
                    sdcardLoader.loadFiles();
                    sdcardLoader.createModels();
                }
            });
        }
    }


    @Override
    public void onActivityResume() {
        super.onActivityResume();
        if (mIsReBuild) {
            mIsReBuild = false;
            new SECommand(getScene()) {
                @Override
                public void run() {
                    show();
                }
            }.execute();

        }
    }

    private class VelocityAnimation extends CountAnimation {
        private float mNeedTranslate;
        private float mDesTranslateX;

        public VelocityAnimation(SEScene scene, float velocity) {
            super(scene);
            if (Math.abs(velocity) < 100) {
                mNeedTranslate = 0;
            } else if (Math.abs(velocity) < 500) {
                mNeedTranslate = mBoxLengthX * (velocity/Math.abs(velocity)) - mLastMove;
            } else if (Math.abs(velocity) < 5000) {
                mNeedTranslate = mBoxLengthX * SHOW_BOX_LINE * (velocity/Math.abs(velocity)) -mLastMove;
            } else {
                mNeedTranslate = velocity * 0.5f;
            }
            mLastMove = 0;
        }

        public void runPatch(int count) {
            float needTranslateX = mDesTranslateX - mTranslateX;
            float absNTX = Math.abs(needTranslateX);
            if (absNTX <= 1) {
                mTranslateX = mDesTranslateX;
                setTranslate(new SEVector3f(mTranslateX, 0, mShelfHeightOffset), true);
                if (mDesTranslateX < mTranslateX_MIN) {
                    mDesTranslateX = mTranslateX_MIN;
                } else if (mDesTranslateX > mTranslateX_MAX) {
                    mDesTranslateX = mTranslateX_MAX;
                } else {
                    stop();
                }
            } else {
                int step = (int) Math.sqrt(absNTX);
                if (needTranslateX < 0) {
                    step = -step;
                }
                mTranslateX = mTranslateX + step;
                setTranslate(new SEVector3f(mTranslateX, 0, mShelfHeightOffset), true);
            }

        }

        @Override
        public void onFirstly(int count) {
            mDesTranslateX = mTranslateX + mNeedTranslate;
            if (mDesTranslateX < mTranslateX_MIN - 100) {
                mDesTranslateX = mTranslateX_MIN - 100;
            } else if (mDesTranslateX > mTranslateX_MAX + 100) {
                mDesTranslateX = mTranslateX_MAX + 100;
            } else {
                mDesTranslateX = ((int)mDesTranslateX / (int)mBoxLengthX) * mBoxLengthX;
            }
        }

    }

    private void onShowFinished() {
        mShowAnimation = new ShowAnimation(getScene());
        mShowAnimation.setAnimFinishListener(new SEAnimFinishListener() {
            public void onAnimationfinish() {
                mDisableTouch = false;
                int size = mCloneObjects.size();
                for (int i = 0; i < size; i++) {
                    final ObjectsMenuItem cloneObj = mCloneObjects.get(i);
                    if (!cloneObj.mModelInfo.hasInstance()
                            /*&& !cloneObj.mModelInfo.mType.equals("IconBox")
                            && !cloneObj.mModelInfo.mType.equals("shop")*/) {
                        final ObjectsMenuItem menuItem = cloneObj.mModelInfo.createMenuInstance(getScene(), ObjectsMenu.this);
                        if (null != menuItem) {
                            menuItem.loadObject(new Runnable() {
                                public void run() {
                                    initMenuObject(menuItem);
                                }
                            });
                        }
                    } else {
                        initMenuObject(cloneObj);
                    }
                }
            }
        });
        mShowAnimation.execute();
    }

    private void performShowAction(final SEAnimFinishListener listener) {
        getCamera().moveToWallSight(new SEAnimFinishListener() {
            public void onAnimationfinish() {
                if (mDockObject != null) {
                    mDockObject.hide(new SEAnimFinishListener() {
                        public void onAnimationfinish() {
                            onShowFinished();
                        }
                    });
                } else {
                    onShowFinished();
                }
            }
        });
    }

    private void onHidden(final SEAnimFinishListener listener) {
        setVisible(false, true);
        if (mDockObject != null) {
            mDockObject.show(null);
        }
        getScene().setStatus(SEScene.STATUS_OBJ_MENU, false);
        releaseRedundantModel();
        if (listener != null) {
            listener.onAnimationfinish();
        }
    }

}
