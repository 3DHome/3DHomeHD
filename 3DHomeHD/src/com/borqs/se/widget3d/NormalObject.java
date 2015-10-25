package com.borqs.se.widget3d;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import com.borqs.framework3d.home3d.TypeManager;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SEVector2f;
import com.borqs.se.engine.SEVector.SEVector2i;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.ToastUtils;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.home3d.HomeScene;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.SearchActivity;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectGroup;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;
import com.borqs.se.widget3d.VesselLayer.ACTION;
import com.borqs.framework3d.home3d.SEMountPointChain;
import com.borqs.framework3d.home3d.SEObjectBoundaryPoint;
public class NormalObject extends SEObjectGroup {
    private SETransParas mStartTranspara;
    private ObjectInfo mObjectInfo;
    private boolean mOnFinger;
    private boolean mIsFresh;
    private boolean mHasInit;
    private NormalObject mChangedToObj;
    private VesselObject mRoot;
    private VelocityTracker mVelocityTracker;
    private boolean mDisableTouch;
    private SEVector2i mAdjustTouch;
    private VesselLayer mVesselLayer;
    private VesselLayer mBeginLayer;
    
    // for long click object
    private boolean mCanUninstall;
    private boolean mCanChangeBind;

    protected static final SEVector3f NO_SCALE = new SEVector3f(1f, 1f, 1f);
    protected static final SEVector3f PORT_SCALE = new SEVector3f(0.6f, 0.6f, 0.6f);

    /////////// for object location begin
    /*
    private SEMountPointChain.MatrixPoint mStartMatrixPoint;
    private SEMountPointChain.MatrixPoint mEndMatrixPoint;
    private SEVector3f mCenter;// this is the coordinate in vessel
    private SEVector3f mXYZSpan;
    private int movePlane;
    */
    private SEObjectBoundaryPoint mBoundaryPoint;
    public void setBeginLayer(VesselLayer l) {
    	mBeginLayer = l;
    }
    public VesselLayer getBeginLayer() {
    	return mBeginLayer;
    }
    public SEObjectBoundaryPoint getBoundaryPoint() {
    	    return mBoundaryPoint;
    }
    public void setBoundaryPoint(SEObjectBoundaryPoint bp) {
    	    mBoundaryPoint = bp;
    }
    public SEMountPointChain.MatrixPoint getStartMatrixPoint() {
    	    return mBoundaryPoint.minMatrixPoint;
    }
    public SEMountPointChain.MatrixPoint getEndMatrixPoint() {
    	    return mBoundaryPoint.maxMatrixPoint;
    }
    /*
    public SEVector3f getXYZSpanFromMinMaxPoint(SEVector3f minPoint, SEVector3f maxPoint) {
        float xspan = Math.abs(maxPoint.getX() - minPoint.getX());
        float yspan = Math.abs(maxPoint.getY() - minPoint.getY());
        float zspan = Math.abs(maxPoint.getZ() - minPoint.getZ());
        return new SEVector3f(xspan, yspan, zspan);
    }
    */
    public SEVector3f getObjectOriginXYZSpan() {
        createLocalBoundingVolume();
        SEVector3f objectMinPoint = new SEVector3f();
        SEVector3f objectMaxPoint = new SEVector3f();
        getLocalBoundingVolume(objectMinPoint, objectMaxPoint);
        SEVector3f xyzSpan = getXYZSpanFromMinMaxPoint(objectMinPoint, objectMaxPoint);
        return xyzSpan;
    }
    public SEVector3f getObjectXYZSpan() {
        SEObjectBoundaryPoint bp = getBoundaryPoint();
        SEVector3f xyzSpan = null;
        if(bp != null) {
            xyzSpan = bp.xyzSpan;
        }
        if(xyzSpan != null && !xyzSpan.equals(SEVector3f.ZERO)) {
            return xyzSpan;
        }
        createLocalBoundingVolume();
        SEVector3f objectMinPoint = new SEVector3f();
        SEVector3f objectMaxPoint = new SEVector3f();
        getLocalBoundingVolume(objectMinPoint, objectMaxPoint);
        xyzSpan = getXYZSpanFromMinMaxPoint(objectMinPoint, objectMaxPoint);
        return xyzSpan;
    }
    /////////// for object location end
    public NormalObject(SEScene scene, String name, int index) {
        super(scene, name, index);
        mOnFinger = false;
        mIsFresh = false;
        mDisableTouch = false;
        mHasInit = false;
        mCanUninstall = false;
        mCanChangeBind = true;
    }

    public void showBackgroud() {

    }

    public void hideBackgroud() {

    }

    public boolean update(SEScene scene) {
        return true;
    }

    public void setIsFresh(boolean fresh) {
        mIsFresh = fresh;
    }

    public boolean isFresh() {
        return mIsFresh;
    }

    public void setChangedToObj(NormalObject origObj) {
        mChangedToObj = origObj;
    }

    public NormalObject getChangedToObj() {
        return mChangedToObj;
    }

    public void setHasInit(boolean hasInit) {
        mHasInit = hasInit;
    }
    
    public boolean hasInit() {
        return mHasInit;
    }

    public boolean load(final SEObject parent, final Runnable finish) {
        if (parent == null) {
            initStatus(getScene());
            if (finish != null) {
                finish.run();
            }
            return true;
        }
        if (mObjectInfo.mIsNativeObject) {
            if (!mObjectInfo.mModelInfo.hasInstance()) {
                mObjectInfo.mModelInfo.register(this);
                SELoadResThread.getInstance().process(new Runnable() {
                    public void run() {
                        mObjectInfo.mModelInfo.load3DMAXModel(getScene());
                        onLoad3DMaxModel();
                        new SECommand(getScene()) {
                            public void run() {
                                if (mObjectInfo.mIndex == 0) {
                                    mObjectInfo.mModelInfo.add3DMAXModel(getScene(), parent);
                                    initStatus(getScene());
                                    onAdd3DMaxModel();
                                    if (finish != null) {
                                        finish.run();
                                    }
                                } else {
                                    mObjectInfo.mModelInfo.createMenuInstanceForMaxModel(getScene());
                                    mObjectInfo.mModelInfo.cloneMenuItemInstance(parent, mObjectInfo.mIndex, false, mObjectInfo.mModelInfo.mStatus);
                                    initStatus(getScene());
                                    if (finish != null) {
                                        finish.run();
                                    }
                                }
                            }
                        }.execute();
                    }
                });

            } else {
                mObjectInfo.mModelInfo.register(this);
                mObjectInfo.mModelInfo.cloneMenuItemInstance(parent, mObjectInfo.mIndex, false,
                        mObjectInfo.mModelInfo.mStatus);
                initStatus(getScene());
                if (finish != null) {
                    finish.run();
                }
            }
        } else {
            render();
            initStatus(getScene());
            if (finish != null) {
                finish.run();
            }
        }
        return true;
    }

    public void onLoad3DMaxModel() {

    }

    public void onAdd3DMaxModel() {

    }
    @Override
    public void onRemoveFromParent(SEObject parent) {
        if(!(parent instanceof House)) {
            return;
        }

    }
    
    private void onSelfRelease() {
        if (mObjectInfo.mIsNativeObject) {
            mObjectInfo.mModelInfo.unRegister(this);
            if (!mObjectInfo.mModelInfo.hasInstance()
                    && !ModelInfo.isObjectShelfHidden(mObjectInfo.mModelInfo.mType)) {
//                    && !mObjectInfo.mModelInfo.mType.equals("Folder")
//                    && !mObjectInfo.mModelInfo.mType.equals("Recycle")
//                    && !mObjectInfo.mModelInfo.mType.equals("IconBox")
//                    && !mObjectInfo.mModelInfo.mType.equals("walldialog")) {
                mObjectInfo.mModelInfo.releaseMenuItem();
            }
        }
    }
    
    @Override
    public void onRelease() {
    	super.onRelease();
        onSelfRelease();
        if (mSoftRelease) {
        	mObjectInfo.doRelease(false);
        } else {
        	mObjectInfo.releaseDB();
        }
    }
    public SETransParas getAbsoluteTransParas() {
        SETransParas retTranspara = new SETransParas();
        retTranspara.mTranslate = getAbsoluteTranslate();
        float angle = getUserRotate().getAngle();
        SEObject parent = getParent();
        while (parent != null) {
            angle = angle + parent.getUserRotate().getAngle();
            parent = parent.getParent();
        }
        retTranspara.mRotate.set(angle, 0, 0, 1);
        retTranspara.mScale = getUserScale().clone();
        return retTranspara;
    }

    public final void calculateNativeObjectTransParas() {
        final VesselObject vessel = getParent() != null && (getParent() instanceof VesselObject) ? (VesselObject) getParent() : null;

        if (mObjectInfo.mIsNativeObject) {
            SETransParas origin = mObjectInfo.mModelInfo.mLocalTrans;
            if (origin != null) {
                SETransParas localTrans = origin.clone();
                applyVesselScale(vessel, localTrans.mScale);
                applyModelSelfScale(localTrans.mScale);

                setLocalTranslate(localTrans.mTranslate);
                setLocalScale(localTrans.mScale);
                setLocalRotate(localTrans.mRotate);
            }
            createLocalBoundingVolume();
        }

    }

    public void initStatus(SEScene scene) {
        setIsEntirety_JNI(true);
        setShadowObjectVisibility_JNI(isShadowNeeded());

        mVesselLayer = getRoot().getVesselLayer();

        calculateNativeObjectTransParas();
        final VesselObject vessel = getParent() != null && (getParent() instanceof VesselObject) ? (VesselObject) getParent() : null;

        if (null != vessel) {
            SETransParas transParas = vessel.getSlotTransParas(getObjectInfo(), this);
            if (transParas != null) {
                getUserTransParas().mTranslate = transParas.mTranslate.clone();
                getUserTransParas().mRotate = transParas.mRotate.clone();
                getUserTransParas().mScale = transParas.mScale.clone();
                setUserTransParas();
            }
        }
        setOnLongClickListener(new SEObject.OnTouchListener() {
            public void run(SEObject obj) {
                if(!canHandleLongClick()) {
                    return;
                }
                SETransParas startTranspara = new SETransParas();
                startTranspara.mTranslate = getAbsoluteTranslate();
                float angle = getUserRotate().getAngle();
                SEObject parent = getParent();
                while (parent != null) {
                    angle = angle + parent.getUserRotate().getAngle();
                    parent = parent.getParent();
                }
                startTranspara.mRotate.set(angle, 0, 0, 1);
                setStartTranspara(startTranspara);
                setOnMove(true);
            }
        });
        setOnClickListener(new SEObject.OnTouchListener() {
            public void run(SEObject obj) {
                if (getObjectInfo().mSlotType == ObjectInfo.SLOT_TYPE_DESKTOP) {
                    handOnClick();
                } else if (!getScene().getStatus(SEScene.STATUS_ON_DESK_SIGHT)
                        && !getScene().getStatus(SEScene.STATUS_ON_SKY_SIGHT)) {
                    handOnClick();
                }
            }
        });
        if (getObjectInfo().mSlotType == ObjectInfo.SLOT_TYPE_DESKTOP) {
            setIsMiniBox(true, true);
        }

    }

    public void copeStatusTo(NormalObject changedObject) {
        changedObject.setAdjustTouch(getAdjustTouch());
        changedObject.setShadowObjectVisibility_JNI(false);
        changedObject.setIsFresh(isFresh());
        changedObject.setOnFinger(true);
        changedObject.getUserTransParas().set(getUserTransParas());
        changedObject.setUserTransParas();
        changedObject.setLayerIndex(10, true);
        if (changedObject.mVelocityTracker != null) {
            changedObject.mVelocityTracker.clear();
        }
    }

    public SEVector2i getAdjustTouch() {
        return mAdjustTouch;
    }

    public void setAdjustTouch(SEVector2i adjustTouch) {
        mAdjustTouch = adjustTouch;
    }

    public void setOnFinger(boolean onFinger) {
        mOnFinger = onFinger;
    }

    public boolean isOnFinger() {
        return mOnFinger;
    }
    public boolean canChildHandleLongClick(NormalObject child) {
        return true;
    }
    public boolean canHandleLongClick() {
        SEObject parent = getParent();
        if(parent instanceof House) {
            House house = (House)parent;
            return house.canChildHandleLongClick(this);
        } else if(parent instanceof  WallShelf) {
            WallShelf wallShelf = (WallShelf)parent;
            return wallShelf.canChildHandleLongClick(this);
        }
        return true;
    }
    public boolean isNeedAdjustTouch() {
        return true;
    }
    public void setOnMove(boolean onFinger) {
        if (onFinger) {
            if (getStartTranspara() != null) {
                if(isNeedAdjustTouch()) {
                    SEVector3f userTranslate = getStartTranspara().mTranslate.clone();
                    mAdjustTouch = getCamera().worldToScreenCoordinate(userTranslate);
                    mAdjustTouch.selfSubtract(new SEVector2i(getTouchX(), getTouchY()));
                } else {
                    mAdjustTouch = new SEVector2i();
                }
            } else {
                mAdjustTouch = new SEVector2i();
            }
            setShadowObjectVisibility_JNI(false);
            getScene().setStatus(SEScene.STATUS_MOVE_OBJECT, true);
            getScene().setTouchDelegate(this);
            setPressed(true);
            getUserTransParas().set(getStartTranspara());
            if (getObjectInfo().mIsNativeObject) {
                SETransParas localTrans = getObjectInfo().mModelInfo.mLocalTrans;
                if (localTrans != null) {
                    getUserTransParas().mTranslate.selfSubtract(localTrans.mTranslate);
                }
            }
            if (!getRoot().equals(getParent())) {
                changeParent(getRoot());
                setUserTransParas();
            }
            mVesselLayer.setOnLayerModel(this, true);
            mVesselLayer.onObjectMoveEvent(ACTION.BEGIN, getTouchX() + mAdjustTouch.getX(),
                    getTouchY() + mAdjustTouch.getY());

            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
            setLayerIndex(10, true);
        } else {
            if(isShelfObject()) {
                if(getObjectInfo().mSlotType == ObjectInfo.SLOT_TYPE_WALL_SHELF) {
                    setShadowObjectVisibility_JNI(false);
                }
            } else {
                setShadowObjectVisibility_JNI(isShadowNeeded());
            }
            setPressed(false);
            getScene().setStatus(SEScene.STATUS_MOVE_OBJECT, false);
            getScene().removeTouchDelegate();
            mDisableTouch = false;
            mVesselLayer.setOnLayerModel(this, false);
            setLayerIndex(0, true);
        }
        mOnFinger = onFinger;
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
    public boolean dispatchTouchEvent(MotionEvent event) {
        trackVelocity(event);
        if (mDisableTouch) {
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onObjectLongClicked() {
    	super.onObjectLongClicked();
    	mPosChanged = false;
    }
    
    private float mDownX ;
    private float mDownY ;
    private boolean mPosChanged;
    public boolean onMoveEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
        	mDownX = event.getX();
        	mDownY = event.getY();
            break;
        case MotionEvent.ACTION_MOVE:
        	
        	if (!mPosChanged) {
        		float newX = event.getX();
        		float newY = event.getY();
        		mPosChanged = Math.sqrt((newX - mDownX) * (newX - mDownX) + (newY - mDownY) * (newY - mDownY)) > 30 * SESceneManager.getInstance().getPixelDensity();
        		if(mPosChanged) {
        			getScene().handleMessage(HomeScene.MSG_TYPE_DISMISS_OBJECT_LONG_CLICK_DIALOG, this);
        		}
            }
            mVesselLayer.onObjectMoveEvent(ACTION.MOVE, event.getX() + mAdjustTouch.getX(),
                    event.getY() + mAdjustTouch.getY());
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            mVesselLayer.onObjectMoveEvent(ACTION.UP, event.getX() + mAdjustTouch.getX(),
                    event.getY() + mAdjustTouch.getY());
            FlyAnimation flyAnimation = new FlyAnimation(getScene(), mVelocityTracker.getXVelocity(),
                    mVelocityTracker.getYVelocity(), 3);
            flyAnimation.execute();
            break;
        }
        return true;
    }

    protected VesselObject getRoot() {
        if (mRoot == null) {
            mRoot = (VesselObject) getScene().getContentObject();
        }
        return mRoot;
    }

    public void onSlotSuccess() {
        if (isFresh()) {
            setIsFresh(false);
            if (getChangedToObj() == null) {
            	 if (!isBindComponentName()) {
            		 showBindAppDialog();
                 }
//                getScene().handleMessage(SE3DHomeScene.MSG_TYPE_SHOW_BIND_APP_DIALOG, this);
            }
        }
        NormalObject normalObject = getChangedToObj();
        if (normalObject != null) {
            normalObject.setChangedToObj(null);
            normalObject.getParent().removeChild(normalObject, true);
            setChangedToObj(null);
        }
    }

    public void updateComponentName(ComponentName name) {
        if (mObjectInfo != null) {
            mObjectInfo.updateComponentName(name);
        }
    }

    public void handOnClick() {
    	if (mObjectInfo != null && "fangdajing".equals(mObjectInfo.mName)) {
          Intent intent = new Intent(SESceneManager.getInstance().getContext(), SearchActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          SESceneManager.getInstance().startActivity(intent);
          return;
      }
        if (!findAndStartIntent()) {
        	showBindAppDialog();
        }
    }
    private void showBindAppDialog() {
    	if (HomeUtils.DEBUG)
            Log.d(HomeUtils.TAG, "not find intent, show binding dialog");
        if (mObjectInfo.mIsNativeObject) {
            getScene().handleMessage(HomeScene.MSG_TYPE_SHOW_BIND_APP_DIALOG, this);
        }
    }
    
    private boolean isBindComponentName() {
    	Intent intent = mObjectInfo.getIntent();
        if (intent == null) {
            if (HomeUtils.DEBUG)
                Log.d("isBindComponentName", "does not have compoment");
            return false;
        }
        return true;
    }

    public boolean findAndStartIntent() {
        Intent intent = mObjectInfo.getIntent();
        if (intent == null) {
            if (HomeUtils.DEBUG)
                Log.d("SEHome", "db does not have compoment");
            return false;
        }
        if (!SESceneManager.getInstance().startActivity(intent)) {
            if (HomeUtils.DEBUG)
                Log.e("SEHome", "not found bind activity");
            return false;
        }
        return true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (mOnFinger) {
            return onMoveEvent(event);
        }
        return super.onTouchEvent(event);
    }

    public void handleNoMoreRoom() {
        setOnMove(false);
        NormalObject normalObject = getChangedToObj();
        if (normalObject != null) {
            normalObject.setChangedToObj(null);
            normalObject.getParent().removeChild(normalObject, true);
            setChangedToObj(null);
        }
        getParent().removeChild(this, true);
    }

    public void handleOutsideRoom() {
        setOnMove(false);
        NormalObject normalObject = getChangedToObj();
        if (normalObject != null) {
            normalObject.setChangedToObj(null);
            normalObject.getParent().removeChild(normalObject, true);
            setChangedToObj(null);
        }
        getParent().removeChild(this, true);
        if (isFresh()) {
            setIsFresh(false);
            if (getObjectInfo().isDownloadObj()) {
                getScene().handleMessage(HomeScene.MSG_TYPE_SHOW_DELETE_OBJECTS, this.mName);
            } else {
                ToastUtils.showDeletePresetObject();
            }
        }
    }
    private void setObjectBoundaryPointToObjectInfo() {
    	if(this.mBoundaryPoint != null) {
    	    this.getObjectInfo().mObjectSlot.mBoundaryPoint = this.mBoundaryPoint.clone();
    	}
    }
    public void updateSlotDB() {
    	setObjectBoundaryPointToObjectInfo();
    	getObjectInfo().updateSlotDB();
    }
    public void handleSlotSuccess() {
        setOnMove(false);
        if (getChangedToObj() != null) {
            getObjectInfo().saveToDB();
        } else if (getObjectInfo().mObjectSlot.mVesselID != -1) {
        	this.setObjectBoundaryPointToObjectInfo();
            getObjectInfo().updateSlotDB();
        }
        if (mObjectInfo != null && mObjectInfo.mModelInfo != null 
        		&& mObjectInfo.mModelInfo.mAssetsPath != null && !mObjectInfo.mModelInfo.mAssetsPath.startsWith("assets/base/")) {
            setCanUninstall(true);
        }
        onSlotSuccess();
    }
    public boolean canPlaceOnShelf() {
        TypeManager typeManager = TypeManager.getInstance();
        return typeManager.canPlaceOnVessel(mName, ObjectInfo.SLOT_TYPE_WALL_SHELF);
    }
    public boolean canPlaceOnWall() {
        return true;
//        TypeManager typeManager = TypeManager.getInstance();
//        return typeManager.canPlaceOnVessel(mName, ObjectInfo.SLOT_TYPE_WALL);
    }
    public boolean isShelfObject() {
        //return canPlaceOnShelf(object.mName) || (object instanceof  WallShelf);
        return canPlaceOnShelf();
    }
    public void setObjectInfo(ObjectInfo info) {
        mObjectInfo = info;
    }

    public ObjectSlot getObjectSlot() {
        return mObjectInfo.mObjectSlot;
    }

    public ObjectInfo getObjectInfo() {
        return mObjectInfo;
    }

    public void setStartTranspara(SETransParas transParas) {
        mStartTranspara = transParas;
    }

    public SETransParas getStartTranspara() {
        return mStartTranspara;
    }

    private class FlyAnimation extends CountAnimation {
        private float mStep;
        private float mVelocityX;
        private float mVelocityY;
        private SEVector2f mCurPoint;
        private SEVector2f mDirect;
        private int mCur;
        private int mEnd;

        public FlyAnimation(SEScene scene, float velocityX, float velocityY, float step) {
            super(scene);
            if (Math.abs(velocityX) <= 100) {
                mVelocityX = 0;
            } else {
                mVelocityX = velocityX * 0.05f;
            }
            if (Math.abs(velocityY) <= 100) {
                mVelocityY = 0;
            } else if (velocityY > 100) {
                mVelocityY = velocityY * 0.05f;
            } else {
                mVelocityY = velocityY * 0.1f;
            }
            mStep = step;
            mCur = 0;
            setAnimFinishListener(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    mVesselLayer.onObjectMoveEvent(ACTION.FINISH, mCurPoint.getX() + mAdjustTouch.getX(),
                            mCurPoint.getY() + mAdjustTouch.getY());
                }
            });
        }

        public void runPatch(int count) {
            int needTranslate = mEnd - mCur;
            int absNTX = Math.abs(needTranslate);
            if (absNTX <= mStep) {
                mCur = mEnd;
                stop();
            } else {
                int step = (int) (mStep * Math.sqrt(absNTX));
                if (needTranslate < 0) {
                    step = -step;
                }
                mCur = mCur + step;
                SEVector2f move = mDirect.mul(step);
                mCurPoint.selfAdd(move);
                mVesselLayer.onObjectMoveEvent(ACTION.FLY, mCurPoint.getX() + mAdjustTouch.getX(), mCurPoint.getY()
                        + mAdjustTouch.getY());
                if (mCurPoint.getY() < 0 || mCurPoint.getY() > getCamera().getHeight() || mCurPoint.getX() < 0
                        || mCurPoint.getX() > getCamera().getWidth()) {
                    stop();
                }
            }
        }

        @Override
        public void onFirstly(int count) {
            mCurPoint = new SEVector2f(getTouchX(), getTouchY());
            mDirect = new SEVector2f(mVelocityX, mVelocityY);
            mEnd = (int) mDirect.getLength();
            mDirect.normalize();

        }
    }

    protected boolean isShadowNeeded() {
        if (mObjectInfo.mSlotType == ObjectInfo.SLOT_TYPE_DESKTOP) {
            VesselObject vesselObject = ModelInfo.getDockObject(getScene());
            return vesselObject.isChildrenShadowAllowed();
        }

        return true;
    }
    
    public void setCanChangeBind(boolean canChangeBind) {
        mCanChangeBind = canChangeBind;
    }
    public boolean canChangeBind() {
        return mCanChangeBind;
    }
    
    public boolean canUninstall() {
        return mCanUninstall;
    }
    
    public void setCanUninstall(boolean canUninstall) {
        mCanUninstall = canUninstall;
    }

    private final void applyVesselScale(VesselObject vesselObject, SEVector3f currentScale) {
        if (null != vesselObject) {
            VesselObject vessel = (VesselObject) getParent();
            SEVector3f scale = vessel.getVesselScale();
            if (!NO_SCALE.equals(scale)) {
                final float scaleX = Math.abs(scale.getX() == 0f ? 1.0f : scale.getX());
                final float scaleY = Math.abs(scale.getY() == 0f ? 1.0f : scale.getY());
                final float scaleZ = Math.abs(scale.getZ() == 0f ? 1.0f : scale.getZ());
                currentScale.selfMul(new SEVector3f(scaleX, scaleY, scaleZ));
            }
        }
    }

    private final void applyModelSelfScale(SEVector3f currentScale) {
        SEVector3f scale = getModelScale();
        if (!NO_SCALE.equals(scale)) {
            currentScale.selfMul(scale);
        }
    }

    protected SEVector3f getModelScale() {
        return NO_SCALE;
    }

    public static boolean isScreenOrientationPortrait() {
        return House.IsScreenOrientationPortrait(SESceneManager.getInstance().getGLActivity());
    }
}