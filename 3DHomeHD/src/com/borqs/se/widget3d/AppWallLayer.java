package com.borqs.se.widget3d;

import java.util.ArrayList;
import java.util.List;

import com.borqs.framework3d.home3d.HouseObject;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.home3d.UpdateDBThread;
import com.borqs.se.engine.SEVector.SEVector2f;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SESceneInfo;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;

public class AppWallLayer extends VesselLayer {
    private AppWall mAppWall;
    private SESceneInfo mSceneInfo;
    private List<ConflictObject> mExistentSlot;
    private ObjectSlot mObjectSlot;
    private ConflictObject mConflictSlot;
    private SetToRightPositionAnimation mSetToRightPositionAnimation;
    private VesselLayer mCurrentLayer;

    public AppWallLayer(SEScene scene, VesselObject vesselObject) {
        super(scene, vesselObject);
        mCurrentLayer = null;
        mAppWall = (AppWall) vesselObject;
        mSceneInfo = getScene().mSceneInfo;
    }

    @Override
    public boolean canHandleSlot(NormalObject object) {
        super.canHandleSlot(object);
        if (object.getObjectInfo().mSlotType == ObjectInfo.SLOT_TYPE_APP_WALL) {
            mExistentSlot = getExistentSlot();
            if (searchEmptySlot(mExistentSlot) == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean setOnLayerModel(NormalObject onMoveObject, boolean onLayerModel) {
        super.setOnLayerModel(onMoveObject, onLayerModel);
        if (onLayerModel) {
            mExistentSlot = getExistentSlot();
            mObjectSlot = null;
            mConflictSlot = null;
            mPreSlotChangeTime = System.currentTimeMillis();
            mSlotChangeinterval = 0;
        } else {
            cancelConflictAnimationTask();
            disableCurrentLayer();
        }
        return true;
    }

    private long mSlotChangeinterval;
    private long mPreSlotChangeTime;

    public boolean onObjectMoveEvent(ACTION event, SEVector3f location) {
        ObjectSlot objectSlot = calculateSlotAndConflictObjects(location);
        if (!cmpSlot(objectSlot, mObjectSlot)) {
            long slotChangeTime = System.currentTimeMillis();
            mSlotChangeinterval = slotChangeTime - mPreSlotChangeTime;
            mPreSlotChangeTime = slotChangeTime;
            if (objectSlot != null && mConflictSlot != null) {
                if (mConflictSlot.mConflictObject instanceof VesselObject) {
                    VesselObject vesselObject = (VesselObject) mConflictSlot.mConflictObject;
                    VesselLayer vesselLayer = vesselObject.getVesselLayer();
                    if (vesselLayer.canHandleSlot(getOnMoveObject())) {
                        if (vesselLayer != mCurrentLayer) {
                            disableCurrentLayer();
                            vesselLayer.setOnLayerModel(getOnMoveObject(), true);
                            mCurrentLayer = vesselLayer;
                        }
                    } else {
                        disableCurrentLayer();
                    }
                } else if ((getOnMoveObject() instanceof AppObject)
                        && (mConflictSlot.mConflictObject instanceof AppObject)) {
                    AppObject appObject = (AppObject) mConflictSlot.mConflictObject;
                    Folder folder = appObject.changeToFolder();
                    changeExistentObject(appObject, folder);
                    disableCurrentLayer();
                    VesselLayer vesselLayer = folder.getVesselLayer();
                    vesselLayer.setOnLayerModel(getOnMoveObject(), true);
                    mCurrentLayer = vesselLayer;

                } else {
                    disableCurrentLayer();
                }
            } else {
                disableCurrentLayer();
            }
        }
        if (mCurrentLayer != null) {
            cancelConflictAnimationTask();
        }
        switch (event) {
        case BEGIN:
            if (mCurrentLayer != null) {
                mObjectSlot = objectSlot;
                return mCurrentLayer.onObjectMoveEvent(event, location);
            }
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
                updateWallStatus(1000);
            }
            break;
        case MOVE:
            if (mCurrentLayer != null) {
                mObjectSlot = objectSlot;
                return mCurrentLayer.onObjectMoveEvent(event, location);
            }
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
                updateWallStatus(250);
            }
            break;
        case UP:
            if (mCurrentLayer != null) {
                mObjectSlot = objectSlot;
                return mCurrentLayer.onObjectMoveEvent(event, location);
            }
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
                updateWallStatus(250);
            }
            break;
        case FLY:
            if (mCurrentLayer != null) {
                mObjectSlot = objectSlot;
                return mCurrentLayer.onObjectMoveEvent(event, location);
            }
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
                updateWallStatus(250);
            }
            break;
        case FINISH:
            if (mCurrentLayer != null) {
                mObjectSlot = objectSlot;
                return mCurrentLayer.onObjectMoveEvent(event, location);
            }
            slotToWall(location, null);
            break;
        }
        return true;
    }

    private void disableCurrentLayer() {
        if (mCurrentLayer != null) {
            if (mCurrentLayer instanceof FolderLayer) {
                final Folder folder = (Folder) mCurrentLayer.getVesselObject();
                if (folder.mChildObjects.size() == 1) {
                    NormalObject icon = folder.changeToAppIcon();
                    changeExistentObject(folder, icon);
                } else if (folder.mChildObjects.size() == 2 && folder.getObjectSlot().mVesselID == -1) {
                    folder.getObjectSlot().mVesselID = mAppWall.getObjectInfo().mID;
                    folder.getObjectInfo().saveToDB();
                    for (SEObject child : folder.mChildObjects) {
                        final NormalObject icon = (NormalObject) child;
                        final int index = folder.mChildObjects.indexOf(child);
                        UpdateDBThread.getInstance().process(new Runnable() {
                            public void run() {
                                icon.getObjectSlot().mVesselID = folder.getObjectInfo().mID;
                                icon.getObjectSlot().mSlotIndex = index;
                                icon.getObjectInfo().updateSlotDB();
                            }
                        });
                    }
                }
            }
            mCurrentLayer.setOnLayerModel(getOnMoveObject(), false);
            mCurrentLayer = null;
        }
    }

    private void changeExistentObject(NormalObject src, NormalObject des) {
        if (mExistentSlot != null) {
            for (ConflictObject existentObject : mExistentSlot) {
                if (existentObject.mConflictObject == src) {
                    existentObject.mConflictObject = des;
                    break;
                }
            }
        }
    }

    private void updateWallStatus(long delay) {
        cancelConflictAnimationTask();
        if (mObjectSlot != null && mConflictSlot != null) {
            searchNewSlot(mConflictSlot);
            playConflictAnimationTask(mConflictSlot, delay);
        }
    }

    private ObjectSlot calculateSlotAndConflictObjects(SEVector3f location) {
        ObjectSlot objectSlot = calculateNearestSlot(location);
        if (objectSlot != null && !objectSlot.equals(mObjectSlot)) {
            mConflictSlot = getConflictSlot(objectSlot);
        }
        return objectSlot;
    }

    private ObjectSlot calculateNearestSlot(SEVector3f location) {
        ObjectSlot slot = getOnMoveObject().getObjectSlot().clone();
        ObjectSlot wallSlot = mAppWall.getObjectSlot();
        SEVector3f wallCenter = getWallPosition();
        SEVector3f start = new SEVector3f();
        HouseObject houseObject = ModelInfo.getHouseObject(getScene());
        start.mD[0] = location.getX() - slot.mSpanX * houseObject.getWallUnitSizeX() / 2f;
        start.mD[1] = wallCenter.getY();
        start.mD[2] = location.getZ() + slot.mSpanY * houseObject.getWallUnitSizeY() / 2f;

        float convertStartX = (start.getX() - wallCenter.getX()) / houseObject.getWallUnitSizeX() +
                wallSlot.mSpanX / 2f;
        if (convertStartX < 0) {
            convertStartX = 0;
        } else if (convertStartX > wallSlot.mSpanX - slot.mSpanX) {
            convertStartX = wallSlot.mSpanX - slot.mSpanX;
        }
        float convertStartY = wallSlot.mSpanY / 2f - (start.getZ() - wallCenter.getZ()) / houseObject.getWallUnitSizeY();
        if (convertStartY < 0) {
            convertStartY = 0;
        } else if (convertStartY > wallSlot.mSpanY - slot.mSpanY) {
            convertStartY = wallSlot.mSpanY - slot.mSpanY;
        }
        slot.mStartX = Math.round(convertStartX);
        slot.mStartY = Math.round(convertStartY);
        return slot;
    }

    public void slotToWall(SEVector3f location, final SEAnimFinishListener l) {
        cancelConflictAnimationTask();
        mObjectSlot = calculateNearestSlot(location);
        mConflictSlot = getConflictSlot(mObjectSlot);
        if (mConflictSlot != null) {
            searchNewSlot(mConflictSlot);
            playConflictAnimationTask(mConflictSlot, 0);
        }
        playSlotAnimation(mObjectSlot, l);
    }

    private ConflictObject getConflictSlot(ObjectSlot cmpSlot) {
        if (cmpSlot == null) {
            return null;
        }
        for (ConflictObject desktopObject : mExistentSlot) {
            if (desktopObject.mConflictObject.getObjectInfo().getStartX() == cmpSlot.mStartX
                    && desktopObject.mConflictObject.getObjectInfo().getStartY() == cmpSlot.mStartY) {
                return desktopObject;
            }
        }
        return null;
    }

    private SEVector3f getWallPosition() {
        ObjectSlot objectSlot = mAppWall.getObjectSlot();
        SEVector2f yDirection = new SEVector2f(0, 1);
        SEVector2f xDirection = new SEVector2f(1, 0);
        HouseObject houseObject = ModelInfo.getHouseObject(getScene());
        float offsetY = houseObject.getWallRadius();
        float offsetX = (objectSlot.mStartX + objectSlot.mSpanX / 2.f) * houseObject.getWallUnitSizeX()
                - houseObject.getWallSpanX() * houseObject.getWallUnitSizeX() / 2.f;
        SEVector2f offset = yDirection.mul(offsetY).add(xDirection.mul(offsetX));
        float offsetZ = houseObject.getWallSpanY() * houseObject.getWallUnitSizeY()
                - (objectSlot.mStartY + objectSlot.mSpanY / 2.f) * houseObject.getWallUnitSizeY() + houseObject.getWallHeight();
        float z = offsetZ;
        return new SEVector3f(offset.getX(), offset.getY(), z);
    }

    private List<ConflictObject> getExistentSlot() {
        List<ConflictObject> fillSlots = new ArrayList<ConflictObject>();
        for (SEObject object : mAppWall.mChildObjects) {
            if (object instanceof NormalObject) {
                NormalObject desktopObject = (NormalObject) object;
                if (!object.equals(getOnMoveObject())) {
                    ConflictObject conflictObject = new ConflictObject();
                    conflictObject.mConflictObject = desktopObject;
                    fillSlots.add(conflictObject);
                }

            }
        }
        return fillSlots;
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

    private void playConflictAnimationTask(ConflictObject conflictObject, long delay) {
        cancelConflictAnimationTask();
        if (conflictObject != null) {
            mPlayConflictAnimationTask = new ConflictAnimationTask(conflictObject);
            if (delay == 0) {
                mPlayConflictAnimationTask.run();
            } else {
                SELoadResThread.getInstance().process(mPlayConflictAnimationTask, delay);
            }
        }
    }

    private void cancelConflictAnimationTask() {
        if (mPlayConflictAnimationTask != null) {
            SELoadResThread.getInstance().cancel(mPlayConflictAnimationTask);
            mPlayConflictAnimationTask = null;
        }
    }

    private ConflictAnimationTask mPlayConflictAnimationTask;

    private class ConflictAnimationTask implements Runnable {
        private ConflictObject mMyConflictObject;

        public ConflictAnimationTask(ConflictObject conflictObject) {
            mMyConflictObject = conflictObject;
        }

        public void run() {
            mMyConflictObject.playConflictAnimation();

        }
    }

    private void searchNewSlot(ConflictObject conflictObject) {
        final ObjectSlot NextSlot = conflictObject.mConflictObject.getObjectInfo().mObjectSlot.clone();
        List<ObjectSlot> emptySlots = searchEmptySlot(mExistentSlot);
        float minDistance = Float.MAX_VALUE;
        for (ObjectSlot emptySlot : emptySlots) {
            float distance = getSlotPosition(conflictObject.mConflictObject.getObjectInfo().mObjectSlot).subtract(
                    getSlotPosition(emptySlot)).getLength();
            if (distance < minDistance) {
                minDistance = distance;
                NextSlot.mStartX = emptySlot.mStartX;
                NextSlot.mStartY = emptySlot.mStartY;
            }
        }
        conflictObject.mMoveSlot = NextSlot;
    }

    private SEVector3f getSlotPosition(ObjectSlot objectSlot) {
        ObjectSlot vesselSlot = mAppWall.getObjectInfo().mObjectSlot;
        float offsetX = (objectSlot.mStartX + objectSlot.mSpanX / 2.f) * 195 - vesselSlot.mSpanX * 195 / 2.f;
        float offsetZ = vesselSlot.mSpanY * 234 / 2.f - (objectSlot.mStartY + objectSlot.mSpanY / 2.f) * 234;
        return new SEVector3f(offsetX, 0, offsetZ);
    }

    private List<ObjectSlot> searchEmptySlot(List<ConflictObject> existentSlot) {
        List<ObjectSlot> emptySlots = null;
        int sizeX = mAppWall.getObjectSlot().mSpanX;
        int sizeY = mAppWall.getObjectSlot().mSpanY;
        boolean[][] slot = new boolean[sizeY][sizeX];
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                slot[y][x] = true;
            }
        }
        for (ConflictObject conflictObject : existentSlot) {
            ObjectSlot objectSlot = conflictObject.mConflictObject.getObjectSlot();
            int startY = objectSlot.mStartY;
            int startX = objectSlot.mStartX;
            slot[startY][startX] = false;
        }
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                if (slot[y][x]) {
                    if (emptySlots == null) {
                        emptySlots = new ArrayList<ObjectSlot>();
                    }
                    ObjectSlot objectSlot = new ObjectSlot();
                    objectSlot.mSpanX = 1;
                    objectSlot.mSpanY = 1;
                    objectSlot.mStartX = x;
                    objectSlot.mStartY = y;
                    emptySlots.add(objectSlot);
                }
            }
        }
        return emptySlots;
    }

    private void playSlotAnimation(final ObjectSlot wallSlot, final SEAnimFinishListener l) {
        getOnMoveObject().changeParent(mAppWall);
        final SETransParas srcTransParas = worldToWall(getOnMoveObject().getUserTransParas());
        getOnMoveObject().getUserTransParas().set(srcTransParas);
        getOnMoveObject().setUserTransParas();
        getOnMoveObject().getObjectSlot().set(wallSlot);
        final SETransParas desTransParas = mAppWall.getSlotTransParas(getOnMoveObject().getObjectInfo(), getOnMoveObject());
        mSetToRightPositionAnimation = new SetToRightPositionAnimation(getScene(), getOnMoveObject(), srcTransParas,
                desTransParas, 7);
        mSetToRightPositionAnimation.setAnimFinishListener(new SEAnimFinishListener() {
            public void onAnimationfinish() {
                handleSlotSuccess();
                if (l != null) {
                    l.onAnimationfinish();
                }
            }
        });
        mSetToRightPositionAnimation.execute();

    }

    private SETransParas worldToWall(SETransParas worldTransParas) {
        SETransParas wallTransParas = new SETransParas();
        SEVector3f appWallLocation = mAppWall.getAbsoluteTranslate();
        wallTransParas.mTranslate = worldTransParas.mTranslate.subtract(appWallLocation);
        return wallTransParas;
    }

    @Override
    public void handleSlotSuccess() {
        super.handleSlotSuccess();
        getOnMoveObject().handleSlotSuccess();
    }

    private class SetToRightPositionAnimation extends CountAnimation {
        private SETransParas mSrcTransParas;
        private SETransParas mDesTransParas;
        private NormalObject mObject;
        private int mCount;
        private float mStep;

        public SetToRightPositionAnimation(SEScene scene, NormalObject obj, SETransParas srcTransParas,
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

    private class ConflictObject {
        public ConflictAnimation mConflictAnimation;
        public NormalObject mConflictObject;
        public ObjectSlot mMoveSlot;

        private void playConflictAnimation() {
            if (mConflictAnimation != null) {
                mConflictAnimation.stop();
            }
            if (mMoveSlot == null) {
                return;
            }
            mConflictObject.getObjectSlot().set(mMoveSlot);
            mConflictAnimation = new ConflictAnimation(getScene(), 3);
            mConflictAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    mConflictObject.getObjectInfo().updateSlotDB();
                }
            });
            mConflictAnimation.execute();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            ConflictObject cmp = (ConflictObject) obj;
            return (mConflictObject.equals(cmp.mConflictObject));
        }

        private class ConflictAnimation extends CountAnimation {
            private SETransParas mDesTransParas;
            private SETransParas mSrcTransParas;
            private float mStep;
            private float mCurProcess;
            private boolean mIsEnbaleBlending;
            private boolean mHasGetBlending;

            public ConflictAnimation(SEScene scene, float step) {
                super(scene);
                mDesTransParas = mAppWall.getSlotTransParas(mConflictObject.getObjectInfo(), mConflictObject);
                mSrcTransParas = mConflictObject.getUserTransParas().clone();
                mStep = step;
                mCurProcess = 0;
            }

            @Override
            public void runPatch(int count) {
                float needTranslate = 100 - mCurProcess;
                float absNTX = Math.abs(needTranslate);
                if (absNTX <= mStep) {
                    mCurProcess = 100;
                    stop();
                } else {
                    int step = (int) (mStep * Math.sqrt(absNTX));
                    if (needTranslate < 0) {
                        step = -step;
                    }
                    mCurProcess = mCurProcess + step;

                }
                float step = mCurProcess / 100;
                mConflictObject.getUserTransParas().mTranslate = mSrcTransParas.mTranslate
                        .add(mDesTransParas.mTranslate.subtract(mSrcTransParas.mTranslate).mul(step));
                mConflictObject.getUserTransParas().mScale = mSrcTransParas.mScale.add(mDesTransParas.mScale.subtract(
                        mSrcTransParas.mScale).mul(step));
                float desAngle = mDesTransParas.mRotate.getAngle();
                float srcAngle = mSrcTransParas.mRotate.getAngle();
                if (desAngle - srcAngle > 180) {
                    desAngle = desAngle - 360;
                } else if (desAngle - srcAngle < -180) {
                    desAngle = desAngle + 360;
                }
                float curAngle = srcAngle + (desAngle - srcAngle) * step;
                mConflictObject.getUserTransParas().mRotate.set(curAngle, 0, 0, 1);
                mConflictObject.setUserTransParas();
            }

            @Override
            public void onFirstly(int count) {
                if (!mHasGetBlending) {
                    mIsEnbaleBlending = mConflictObject.isBlendingable();
                    mHasGetBlending = true;
                }
                if (!mIsEnbaleBlending) {
                    mConflictObject.setBlendingable(true, true);
                }
                mConflictObject.setAlpha(0.1f, true);
            }

            @Override
            public void onFinish() {
                mConflictObject.getUserTransParas().set(mDesTransParas);
                mConflictObject.setUserTransParas();
                if (mHasGetBlending) {
                    mConflictObject.setAlpha(1, true);
                    if (!mIsEnbaleBlending) {
                        mConflictObject.setBlendingable(false, true);
                    } else {
                        mConflictObject.setBlendingable(true, true);
                    }
                }
            }
        }
    }

}
