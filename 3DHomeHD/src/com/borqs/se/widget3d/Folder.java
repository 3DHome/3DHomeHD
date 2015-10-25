package com.borqs.se.widget3d;

import java.util.ArrayList;
import java.util.List;

import android.view.MotionEvent;

import com.borqs.framework3d.home3d.HouseObject;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERay;
import com.borqs.se.engine.SEVector.SEVector2i;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SECommand;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectGroup;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.shortcut.ItemInfo;
import com.borqs.se.shortcut.LauncherModel;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;

public class Folder extends VesselObject {
    private boolean mOnDialogShow;
    private FolderExpandDialog mDialog;
    private List<NormalObject> mIcons;
    private boolean mDisableTouch;
    private SEVector3f mDialogCenter;
    private ShowDialogFolderAnimation mShowDialogFolderAnimation;

    public Folder(SEScene scene, String name, int index) {
        super(scene, name, index);
        mOnDialogShow = false;
        mDisableTouch = false;
        setCanChangeBind(false);
    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        se_setNeedBlendSort_JNI(new float[] { 0, 1f, 0 });
        setVesselLayer(new FolderLayer(scene, this));
        setOnClickListener(new OnTouchListener() {
            public void run(SEObject obj) {
                if (!getScene().getStatus(SEScene.STATUS_ON_DESK_SIGHT)
                        && !getScene().getStatus(SEScene.STATUS_ON_SKY_SIGHT)) {
                    showExpand();
                }
            }
        });
        LauncherModel.getInstance().addAppCallBack(this);
        setHasInit(true);
    }

    @Override
    public void onRelease() {
        super.onRelease();
        LauncherModel.getInstance().removeAppCallBack(this);
    }

    private void showExpand() {
        if (!mOnDialogShow) {
            stopAllAnimation(null);
            mOnDialogShow = true;
            mDisableTouch = true;
            getScene().setStatus(SEScene.STATUS_ON_WIDGET_SIGHT, true);
            HouseObject houseObject = ModelInfo.getHouseObject(getScene());
            float centerY = houseObject.getWallRadius() * 0.8f;
            float centerZ = houseObject.getWallHeight() + houseObject.getHouseHeight() / 2;
            mDialogCenter = new SEVector3f(0, centerY, centerZ);
            ModelInfo modelInfo = getScene().mSceneInfo.findModelInfo("woodfolderopen");
            mDialog = new FolderExpandDialog(getScene(), "woodfolderopen", 1);
            mDialog.mModelInfo = modelInfo;
            getScene().getContentObject().addChild(mDialog, false);
            getScene().setTouchDelegate(mDialog);
            mIcons = new ArrayList<NormalObject>();
            for (SEObject icon : mChildObjects) {
                mIcons.add((NormalObject) icon);
            }
            mDialog.load(getScene().getContentObject());
            SETransParas srcTransParas = new SETransParas();
            srcTransParas.mTranslate = getAbsoluteTranslate().clone();
            srcTransParas.mScale.set(0.2f, 0.2f, 0.2f);
            mDialog.getUserTransParas().set(srcTransParas);
            mDialog.setUserTransParas();
            SETransParas desTransParas = new SETransParas();
            desTransParas.mTranslate = mDialogCenter.clone();
            setVisible(false, true);
            mShowDialogFolderAnimation = new ShowDialogFolderAnimation(getScene(), mDialog, srcTransParas,
                    desTransParas, 10);
            mShowDialogFolderAnimation.setAnimFinishListener(new SEAnimFinishListener() {

                public void onAnimationfinish() {
                    mDisableTouch = false;
                }
            });
            mShowDialogFolderAnimation.execute();

        }
    }

    private void hideExpand(boolean fast, final SEAnimFinishListener l) {
        if (mOnDialogShow) {
            mDisableTouch = true;
            stopAllAnimation(null);
            if (fast) {
                mOnDialogShow = false;
                mDisableTouch = false;
                setVisible(true, true);
                for (SEObject child : mIcons) {
                    NormalObject icon = (NormalObject) child;
                    icon.changeParent(this);
                    icon.initStatus(getScene());
                    icon.setAlpha(1, true);
                    icon.setIsEntirety_JNI(false);
                    icon.getObjectInfo().updateSlotDB();
                    if (icon.getObjectSlot().mSlotIndex > 3) {
                        icon.setVisible(false, true);
                    }
                }
                if (mChildObjects.size() == 1) {
                    NormalObject icon = (NormalObject) mChildObjects.get(0);
                    changeToAppIcon();
                    icon.getObjectInfo().updateSlotDB();
                }
                getScene().getContentObject().removeChild(mDialog, true);
                getScene().setStatus(SEScene.STATUS_ON_WIDGET_SIGHT, false);
                getScene().removeTouchDelegate();
                if (l != null) {
                    l.onAnimationfinish();
                }
            } else {
                SETransParas srcTransParas = mDialog.getUserTransParas().clone();
                SETransParas desTransParas = new SETransParas();
                desTransParas.mTranslate = getAbsoluteTranslate().clone();
                desTransParas.mScale.set(0.2f, 0.2f, 0.2f);

                mShowDialogFolderAnimation = new ShowDialogFolderAnimation(getScene(), mDialog, srcTransParas,
                        desTransParas, 8);
                mShowDialogFolderAnimation.setAnimFinishListener(new SEAnimFinishListener() {

                    public void onAnimationfinish() {
                        mOnDialogShow = false;
                        mDisableTouch = false;
                        setVisible(true, true);
                        for (SEObject child : mIcons) {
                            NormalObject icon = (NormalObject) child;
                            icon.changeParent(Folder.this);
                            icon.initStatus(getScene());
                            icon.setAlpha(1, true);
                            icon.setIsEntirety_JNI(false);
                            icon.getObjectInfo().updateSlotDB();
                            icon.hideBackgroud();
                            if (icon.getObjectSlot().mSlotIndex > 3) {
                                icon.setVisible(false, true);
                            }
                        }
                        if (mChildObjects.size() == 1) {
                            NormalObject icon = (NormalObject) mChildObjects.get(0);
                            changeToAppIcon();
                            icon.getObjectInfo().updateSlotDB();
                        }
                        getScene().getContentObject().removeChild(mDialog, true);
                        getScene().setStatus(SEScene.STATUS_ON_WIDGET_SIGHT, false);
                        getScene().removeTouchDelegate();
                        if (l != null) {
                            l.onAnimationfinish();
                        }
                    }
                });
                mShowDialogFolderAnimation.execute();
            }
        }
    }

    @Override
    public void stopAllAnimation(SEAnimFinishListener l) {
        super.stopAllAnimation(l);
        if (mShowDialogFolderAnimation != null) {
            mShowDialogFolderAnimation.stop();
        }

    }

    private class ShowDialogFolderAnimation extends CountAnimation {
        private SETransParas mSrcTransParas;
        private SETransParas mDesTransParas;
        private SEObject mObject;
        private int mCount;
        private float mStep;

        public ShowDialogFolderAnimation(SEScene scene, SEObject obj, SETransParas srcTransParas,
                SETransParas desTransParas, int count) {
            super(scene);
            mObject = obj;
            mSrcTransParas = srcTransParas;
            mDesTransParas = desTransParas;
            mCount = count;
        }

        public void runPatch(int count) {
            float step = mStep * count;
            mObject.getUserTransParas().mTranslate = mSrcTransParas.mTranslate.add(mDesTransParas.mTranslate.subtract(
                    mSrcTransParas.mTranslate).mul(step));
            mObject.getUserTransParas().mScale = mSrcTransParas.mScale.add(mDesTransParas.mScale.subtract(
                    mSrcTransParas.mScale).mul(step));
            float desAngle = mDesTransParas.mRotate.getAngle();
            float srcAngle = mSrcTransParas.mRotate.getAngle();
            if (desAngle - srcAngle > 180) {
                desAngle = desAngle - 360;
            } else if (desAngle - srcAngle < -180) {
                desAngle = desAngle + 360;
            }
            float curAngle = srcAngle + (desAngle - srcAngle) * step;
            mObject.getUserTransParas().mRotate.set(curAngle, 0, 0, 1);
            mObject.setUserTransParas();
        }

        @Override
        public void onFirstly(int count) {
            mStep = 1f / getAnimationCount();
        }

        @Override
        public int getAnimationCount() {
            return mCount;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mDisableTouch) {
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean handleBackKey(SEAnimFinishListener l) {
        if (mOnDialogShow) {
            hideExpand(false, l);
            return true;
        }
        return false;

    }

    public NormalObject changeToAppIcon() {
        NormalObject icon = (NormalObject) mChildObjects.get(0);
        VesselObject parent = (VesselObject) getParent();
        House house = null;
        int wallIndex = -1;
        int mountPointIndex = -1;
        if(parent instanceof  House) {
            house = (House)parent;
            wallIndex = getObjectSlot().mSlotIndex;
            mountPointIndex = getObjectSlot().mMountPointIndex;
            WallShelf shelf = house.getWallShelfWithObject(wallIndex, this);
            if(shelf != null) {
                shelf.changeObject(this, icon);
            }
        }
        icon.getObjectSlot().set(getObjectSlot());
        icon.getObjectSlot().mSlotIndex = wallIndex;
        icon.getObjectSlot().mMountPointIndex = mountPointIndex;
        icon.getObjectSlot().mVesselID = parent.getObjectInfo().mID;
        icon.getObjectInfo().mVesselName = getObjectInfo().mVesselName;
        icon.changeParent(parent);
        icon.getObjectInfo().mType = ModelInfo.Type.APP_ICON;
        if (ModelInfo.isHouseVesselObject(parent)) {
            icon.showBackgroud();
        } else {
            icon.hideBackgroud();
        }
        icon.initStatus(getScene());
        icon.setIsEntirety_JNI(true);
        parent.removeChild(this, true);
        return icon;
    }

    public void bindAppsRemoved(List<ItemInfo> apps) {
        super.bindAppsRemoved(apps);
        new SECommand(getScene()) {
            public void run() {
                if (mChildObjects.size() == 0) {
                    getParent().removeChild(Folder.this, true);
                } else if (mChildObjects.size() == 1) {
                    NormalObject icon = changeToAppIcon();
                    icon.getObjectInfo().updateSlotDB();
                }

            }
        }.execute();
    }

    public void onLoadFinished() {
        if (mChildObjects.size() == 0) {
            getParent().removeChild(this, true);
        } else if (mChildObjects.size() == 1) {
            NormalObject icon = changeToAppIcon();
            icon.getObjectInfo().updateSlotDB();
        } else {
            for (SEObject child : mChildObjects) {
                child.setIsEntirety_JNI(false);
                if (child instanceof NormalObject) {
                    NormalObject icon = (NormalObject) child;
                    if (icon.getObjectSlot().mSlotIndex > 3) {
                        icon.setVisible(false, true);
                    }
                }
            }
        }
    }

    public SETransParas getFirstPosition() {
        SETransParas transparas = new SETransParas();
        transparas.mTranslate.set(-30, -25.0f, 100);
        transparas.mScale.set(0.28f, 0.28f, 0.28f);
        return transparas;
    }

    @Override
    public SETransParas getSlotTransParas(ObjectInfo objectInfo, NormalObject object) {
        SETransParas transparas = new SETransParas();
        ObjectSlot objectSlot = objectInfo.mObjectSlot;
        if (objectSlot.mSlotIndex == 0) {
            transparas.mTranslate.set(-30, -25.0f, 100);
            transparas.mScale.set(0.28f, 0.28f, 0.28f);
        } else if (objectSlot.mSlotIndex == 1) {
            transparas.mTranslate.set(30, -25.0f, 100);
            transparas.mScale.set(0.28f, 0.28f, 0.28f);
        } else if (objectSlot.mSlotIndex == 2) {
            transparas.mTranslate.set(-30, -25.0f, 40);
            transparas.mScale.set(0.28f, 0.28f, 0.28f);
        } else if (objectSlot.mSlotIndex == 3) {
            transparas.mTranslate.set(30, -25.0f, 40);
            transparas.mScale.set(0.28f, 0.28f, 0.28f);
        } else {
            transparas.mTranslate.set(0, 20, 25);
            transparas.mScale.set(0, 0, 0);
        }
        return transparas;
    }

    private class FolderExpandDialog extends SEObjectGroup {
        private NormalObject mOnMoveIcon;
        private SEVector2i mAdjustTouch;
        private SEVector3f mTouchLocation;
        private ObjectSlot mObjectSlot;
        private List<ConflictAnimationTask> mConflictAnimationTasks;
        private ModelInfo mModelInfo;
        private float mSizeX = 200;
        private float mSizeZ = 192f;
        private int mSpanX = 3;
        private int mSpanZ = 4;
        private float mOutY = 80;
        private boolean mStopConflictAnination;

        public FolderExpandDialog(SEScene scene, String name, int index) {
            super(scene, name, index);
            mStopConflictAnination = false;
        }

        public void load(final SEObject parent) {
            mModelInfo.cloneMenuItemInstance(parent, mIndex, false, mModelInfo.mStatus);
            init();
        }

        private void init() {
            for (NormalObject icon : mIcons) {
                icon.changeParent(this);
                icon.getUserTransParas().set(getSlotTransParas(icon.getObjectInfo()));
                icon.setUserTransParas();
                icon.setAlpha(1, true);
                icon.setIsEntirety_JNI(true);
                if (icon.getObjectSlot().mSlotIndex > 3) {
                    icon.setVisible(true, true);
                }
                icon.setOnLongClickListener(new OnTouchListener() {
                    public void run(SEObject obj) {
                        mOnMoveIcon = (NormalObject) obj;
                        SEVector3f userTranslate = mOnMoveIcon.getAbsoluteTranslate().clone();
                        mAdjustTouch = getCamera().worldToScreenCoordinate(userTranslate);
                        mAdjustTouch.selfSubtract(new SEVector2i(mOnMoveIcon.getTouchX(), mOnMoveIcon.getTouchY()));
                        setMovePoint(obj.getTouchX(), obj.getTouchY());
                        mOnMoveIcon.setTranslate(mTouchLocation, true);
                        mObjectSlot = mOnMoveIcon.getObjectSlot().clone();

                    }
                });
            }
            setOnClickListener(new OnTouchListener() {
                public void run(SEObject obj) {
                    SERay ray = getCamera().screenCoordinateToRay(getTouchX(), getTouchY());
                    SEVector3f point = rayCrossY(ray, mDialogCenter.getY() - mOutY);
                    float maxX = mSizeX * mSpanX / 2;
                    float maxZ = mSizeZ * mSpanZ / 2;
                    float centerZ = mDialogCenter.getZ();
                    if (point.getX() < -maxX || point.getX() > maxX || point.getZ() > centerZ + maxZ
                            || point.getZ() < centerZ - maxZ) {
                        hideExpand(false, null);
                    }

                }

            });
            setPressedListener(null);
            setUnpressedListener(null);
        }

        public SETransParas getSlotTransParas(ObjectInfo objectInfo) {
            int slotIndex = objectInfo.mObjectSlot.mSlotIndex;
            int startX = slotIndex % mSpanX;
            int startY = slotIndex / mSpanX;
            SETransParas transParas = new SETransParas();
            transParas.mTranslate.set(-mSizeX * (mSpanX - 1) / 2 + startX * mSizeX, -mOutY, (mSpanZ - 1) * mSizeZ / 2
                    - startY * mSizeZ);
            transParas.mScale.set(0.68f, 0.68f, 0.68f);
            return transParas;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (mOnMoveIcon != null) {
                return true;
            }
            return false;
        }

        private SEVector3f rayCrossY(SERay ray, float y) {
            float para = (y - ray.getLocation().getY()) / ray.getDirection().getY();
            SEVector3f crossPoint = ray.getLocation().add(ray.getDirection().mul(para));
            return crossPoint;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mOnMoveIcon = null;
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
                if (mOnMoveIcon != null) {
                    SELoadResThread.getInstance().cancel(mPlayConflictAnimationTask);
                    mConflictAnimationTasks = getConflictTask(mObjectSlot);
                    if (mConflictAnimationTasks != null) {
                        mPlayConflictAnimationTask.run();
                    }
                    if (mObjectSlot.mSlotIndex >= mIcons.size()) {
                        mObjectSlot.mSlotIndex = mIcons.size() - 1;
                    }
                    mOnMoveIcon.getObjectSlot().set(mObjectSlot);
                    mOnMoveIcon.setPressed(false);
                    SEVector3f src = mOnMoveIcon.getUserTransParas().mTranslate.clone();
                    SEVector3f des = getSlotTransParas(mOnMoveIcon.getObjectInfo()).mTranslate.clone();
                    MovePositionAnimation movePositionAnimation = new MovePositionAnimation(getScene(), mOnMoveIcon,
                            src, des, 8);
                    movePositionAnimation.execute();
                }
            }
            if (mDisableTouch) {
                return true;
            }
            return super.dispatchTouchEvent(event);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (mOnMoveIcon == null) {
                return super.onTouchEvent(event);
            }
            if (event.getAction() != MotionEvent.ACTION_UP) {
                setMovePoint((int) event.getX(), (int) event.getY());
                mOnMoveIcon.setTranslate(mTouchLocation, true);
                mOnMoveIcon.setPressed(true);
                ObjectSlot objectSlot = calculateSlot();
                if (!cmpSlot(objectSlot, mObjectSlot)) {
                    mObjectSlot = objectSlot;
                    if (mObjectSlot.mSlotIndex == mSpanX * mSpanZ) {
                        SELoadResThread.getInstance().cancel(mPlayConflictAnimationTask);
                        if (mConflictAnimationTasks != null) {
                            for (ConflictAnimationTask conflictAnimationTask : mConflictAnimationTasks) {
                                conflictAnimationTask.mIcon.getObjectSlot().set(conflictAnimationTask.mMoveSlot);
                            }
                        }
                        mStopConflictAnination = true;
                        mIcons.remove(mOnMoveIcon);
                        SETransParas startTranspara = new SETransParas();
                        startTranspara.mTranslate = mOnMoveIcon.getAbsoluteTranslate();
                        mOnMoveIcon.changeParent(getRoot());
                        mOnMoveIcon.initStatus(getScene());
                        hideExpand(true, null);
                        mOnMoveIcon.setStartTranspara(startTranspara);
                        mOnMoveIcon.getUserTransParas().set(startTranspara);
                        mOnMoveIcon.setUserTransParas();
                        mOnMoveIcon.setTouch(getTouchX(), getTouchY());
                        mOnMoveIcon.setOnMove(true);
                    } else {
                        updateStatus(200);
                    }
                }
            }
            return true;
        }

        private void updateStatus(long delay) {
            SELoadResThread.getInstance().cancel(mPlayConflictAnimationTask);
            if (mConflictAnimationTasks != null) {
                SELoadResThread.getInstance().process(mPlayConflictAnimationTask, delay);
            }
        }

        private Runnable mPlayConflictAnimationTask = new Runnable() {
            public void run() {
                if (mConflictAnimationTasks != null) {
                    List<ConflictAnimationTask> tasks = new ArrayList<ConflictAnimationTask>();
                    tasks.addAll(mConflictAnimationTasks);
                    for (ConflictAnimationTask task : tasks) {
                        task.mIcon.getObjectSlot().set(task.mMoveSlot);
                        task.mSrcLocation = task.mIcon.getUserTransParas().mTranslate.clone();
                        task.mDesLocation = getSlotTransParas(task.mIcon.getObjectInfo()).mTranslate.clone();
                    }
                    ConflictAnimation conflictAnimation = new ConflictAnimation(getScene(), tasks, 8);
                    conflictAnimation.execute();
                }
            }
        };

        private ObjectSlot calculateSlot() {
            float x = (mTouchLocation.getX() + (mSpanX - 1) * mSizeX / 2) / mSizeX;
            float y = ((mSpanZ - 1) * mSizeZ / 2 - mTouchLocation.getZ()) / mSizeZ;
            int startX = Math.round(x);
            int startY = Math.round(y);
            ObjectSlot objectSlot = mOnMoveIcon.getObjectSlot().clone();
            if (startX < 0 || startX >= mSpanX || startY < 0 || startY >= mSpanZ) {
                objectSlot.mSlotIndex = mSpanX * mSpanZ;
            } else {
                objectSlot.mSlotIndex = startY * mSpanX + startX;
            }
            if (!objectSlot.equals(mObjectSlot)) {
                mConflictAnimationTasks = getConflictTask(objectSlot);
            }
            return objectSlot;
        }

        private List<ConflictAnimationTask> getConflictTask(ObjectSlot slot) {
            List<ConflictAnimationTask> tasks = null;
            NormalObject conflictIcon = getConflictIcon(slot);
            if (conflictIcon != null) {
                tasks = new ArrayList<ConflictAnimationTask>();
                if (canPlaceToLeft(tasks, conflictIcon)) {
                    return tasks;
                }
                tasks = new ArrayList<ConflictAnimationTask>();
                if (canPlaceToRight(tasks, conflictIcon)) {
                    return tasks;
                }
            } else {
                if (slot.mSlotIndex >= mIcons.size()) {
                    conflictIcon = getMaxIndexIcon();
                    if (conflictIcon != null) {
                        tasks = new ArrayList<ConflictAnimationTask>();
                        if (canPlaceToLeft(tasks, conflictIcon)) {
                            return tasks;
                        }
                    }
                }
            }
            return null;
        }

        private boolean canPlaceToLeft(List<ConflictAnimationTask> tasks, NormalObject conflictIcon) {
            if (conflictIcon.getObjectSlot().mSlotIndex > 0) {
                ConflictAnimationTask conflictAnimationTask = new ConflictAnimationTask();
                conflictAnimationTask.mIcon = conflictIcon;
                ObjectSlot moveSlot = conflictIcon.getObjectSlot().clone();
                moveSlot.mSlotIndex = moveSlot.mSlotIndex - 1;
                conflictAnimationTask.mMoveSlot = moveSlot;
                tasks.add(conflictAnimationTask);
                NormalObject newConflictIcon = getConflictIcon(moveSlot);
                if (newConflictIcon != null) {
                    return (canPlaceToLeft(tasks, newConflictIcon));
                }
                return true;
            } else {
                return false;
            }

        }

        private boolean canPlaceToRight(List<ConflictAnimationTask> tasks, NormalObject conflictIcon) {
            if (conflictIcon.getObjectSlot().mSlotIndex < mSpanX * mSpanZ) {
                ConflictAnimationTask conflictAnimationTask = new ConflictAnimationTask();
                conflictAnimationTask.mIcon = conflictIcon;
                ObjectSlot moveSlot = conflictIcon.getObjectSlot().clone();
                moveSlot.mSlotIndex = moveSlot.mSlotIndex + 1;
                conflictAnimationTask.mMoveSlot = moveSlot;
                tasks.add(conflictAnimationTask);
                NormalObject newConflictIcon = getConflictIcon(moveSlot);
                if (newConflictIcon != null) {
                    return (canPlaceToRight(tasks, newConflictIcon));
                }
                return true;
            } else {
                return false;
            }
        }

        private NormalObject getMaxIndexIcon() {
            NormalObject maxIcon = null;
            for (NormalObject icon : mIcons) {
                if (icon != mOnMoveIcon) {
                    if (maxIcon == null) {
                        maxIcon = icon;
                    } else if (icon.getObjectSlot().mSlotIndex > maxIcon.getObjectSlot().mSlotIndex) {
                        maxIcon = icon;
                    }
                }
            }
            return maxIcon;
        }

        private NormalObject getConflictIcon(ObjectSlot slot) {
            for (NormalObject icon : mIcons) {
                if (icon != mOnMoveIcon) {
                    if (icon.getObjectSlot().mSlotIndex == slot.mSlotIndex) {
                        return icon;
                    }
                }
            }
            return null;
        }

        private void setMovePoint(int touchX, int touchY) {
            SERay ray = getCamera().screenCoordinateToRay(mAdjustTouch.getX() + touchX, mAdjustTouch.getY() + touchY);
            mTouchLocation = getTouchLocation(ray);
        }

        private SEVector3f getTouchLocation(SERay ray) {
            float y = mDialogCenter.getY() - mOutY;
            float para = (y - ray.getLocation().getY()) / ray.getDirection().getY();
            SEVector3f touchLocation = ray.getLocation().add(ray.getDirection().mul(para));
            touchLocation.selfSubtract(new SEVector3f(0, mDialogCenter.getY() + mOutY, mDialogCenter.getZ()));
            return touchLocation;
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

        private class MovePositionAnimation extends CountAnimation {
            private SEVector3f mSrcLocation;
            private SEVector3f mDesLocation;
            private NormalObject mObject;
            private int mCount;
            private float mStep;

            public MovePositionAnimation(SEScene scene, NormalObject obj, SEVector3f srcLocation,
                    SEVector3f desLocation, int count) {
                super(scene);
                mObject = obj;
                mSrcLocation = srcLocation;
                mDesLocation = desLocation;
                mCount = count;
            }

            public void runPatch(int count) {
                float step = mStep * count;
                SEVector3f location = mSrcLocation.add(mDesLocation.subtract(mSrcLocation).mul(step));
                mObject.setTranslate(location, true);
            }

            @Override
            public void onFirstly(int count) {
                mStep = 1f / getAnimationCount();
            }

            @Override
            public int getAnimationCount() {
                return mCount;
            }
        }

        private class ConflictAnimation extends CountAnimation {
            private List<ConflictAnimationTask> mTasks;
            private int mCount;
            private float mStep;

            public ConflictAnimation(SEScene scene, List<ConflictAnimationTask> conflictAnimationTasks, int count) {
                super(scene);
                mCount = count;
                mTasks = conflictAnimationTasks;
            }

            public void runPatch(int count) {
                if (mStopConflictAnination) {
                    stop();
                } else {
                    float step = mStep * count;
                    for (ConflictAnimationTask task : mTasks) {
                        SEVector3f location = task.mSrcLocation.add(task.mDesLocation.subtract(task.mSrcLocation).mul(
                                step));
                        task.mIcon.setTranslate(location, true);
                    }
                }
            }

            @Override
            public void onFinish() {
                for (ConflictAnimationTask task : mTasks) {
                    task.mIcon.setAlpha(1, true);
                }
            }

            @Override
            public void onFirstly(int count) {
                for (ConflictAnimationTask task : mTasks) {
                    task.mIcon.setAlpha(0.6f, true);
                }
                mStep = 1f / getAnimationCount();
            }

            @Override
            public int getAnimationCount() {
                return mCount;
            }
        }

        private class ConflictAnimationTask {
            public NormalObject mIcon;
            public ObjectSlot mMoveSlot;
            private SEVector3f mSrcLocation;
            private SEVector3f mDesLocation;
        }

    }

}
