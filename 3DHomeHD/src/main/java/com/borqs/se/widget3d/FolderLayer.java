package com.borqs.se.widget3d;

import java.util.ArrayList;
import java.util.List;

import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SETransParas;

import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;

public class FolderLayer extends VesselLayer {
    private Folder mFolder;
    private List<NormalObject> mExistentSlot;
    private SetToRightPositionAnimation mSetToRightPositionAnimation;
    private SetScaleAnimation mSetScaleAnimation;
    private float mScale;
    private SEVector3f mPreScale;

    public FolderLayer(SEScene scene, VesselObject vesselObject) {
        super(scene, vesselObject);
        mFolder = (Folder) vesselObject;
        mScale = 1;
    }

    @Override
    public boolean canHandleSlot(NormalObject object) {
        super.canHandleSlot(object);
        if (object instanceof AppObject) {
            mExistentSlot = getExistentSlot();
            if (mExistentSlot.size() < 12) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setOnLayerModel(NormalObject onMoveObject, boolean onLayerModel) {
        super.setOnLayerModel(onMoveObject, onLayerModel);
        if (onLayerModel) {
            mExistentSlot = getExistentSlot();
            if (mSetScaleAnimation != null) {
                mSetScaleAnimation.stop();
            }
            mPreScale = mFolder.getUserScale();
            if (mScale != 1.2f) {
                mSetScaleAnimation = new SetScaleAnimation(getScene(), mFolder, 1.2f, 0.05f);
                mSetScaleAnimation.execute();
            }
        } else {
            mExistentSlot = getExistentSlot();
            if (mSetScaleAnimation != null) {
                mSetScaleAnimation.stop();
            }
            if (mScale != 1) {
                mSetScaleAnimation = new SetScaleAnimation(getScene(), mFolder, 1, 0.05f);
                mSetScaleAnimation.execute();
            }
        }
        return true;
    }

    @Override
    public boolean onObjectMoveEvent(ACTION event, SEVector3f location) {
        switch (event) {
        case FINISH:
            slotToFolder(location, null);
            break;
        }
        return true;
    }

    private void slotToFolder(SEVector3f location, final SEAnimFinishListener l) {
        ObjectSlot objectSlot = calculateSlot();
        playSlotAnimation(objectSlot, l);
    }
    public void placeObjectInSlot(SEAnimFinishListener l) {
        ObjectSlot objectSlot = calculateSlot();
        playSlotAnimation(objectSlot, l);

    }
    private void playSlotAnimation(final ObjectSlot wallSlot, final SEAnimFinishListener l) {
        getOnMoveObject().changeParent(mFolder);
        final SETransParas srcTransParas = worldToWall(getOnMoveObject().getUserTransParas());
        getOnMoveObject().getUserTransParas().set(srcTransParas);
        getOnMoveObject().setUserTransParas();
        getOnMoveObject().getObjectSlot().set(wallSlot);
        final SETransParas desTransParas = mFolder.getSlotTransParas(getOnMoveObject().getObjectInfo(), getOnMoveObject());
        mSetToRightPositionAnimation = new SetToRightPositionAnimation(getScene(), getOnMoveObject(), srcTransParas,
                desTransParas, 8);
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

    @Override
    public void handleSlotSuccess() {
        super.handleSlotSuccess();
        getOnMoveObject().handleSlotSuccess();
        getOnMoveObject().setIsEntirety_JNI(false);
        if (getOnMoveObject().getObjectSlot().mSlotIndex > 3) {
            getOnMoveObject().setVisible(false, true);
        }
        getOnMoveObject().hideBackgroud();
    }

    private SETransParas worldToWall(SETransParas worldTransParas) {
        SETransParas wallTransParas = new SETransParas();
        SEVector3f appWallLocation = mFolder.getAbsoluteTranslate();
        wallTransParas.mTranslate = worldTransParas.mTranslate.subtract(appWallLocation);
        return wallTransParas;
    }

    private ObjectSlot calculateSlot() {
        ObjectSlot slot = getOnMoveObject().getObjectSlot().clone();
        slot.mSlotIndex = mExistentSlot.size();
        return slot;
    }

    private List<NormalObject> getExistentSlot() {
        List<NormalObject> fillSlots = new ArrayList<NormalObject>();
        for (SEObject object : mFolder.mChildObjects) {
            if (object instanceof NormalObject) {
                NormalObject desktopObject = (NormalObject) object;
                if (!object.equals(getOnMoveObject())) {
                    fillSlots.add(desktopObject);
                }

            }
        }
        return fillSlots;
    }

    private class SetScaleAnimation extends CountAnimation {
        private SEObject mObject;
        private float mEndScale;
        private float mCurrentScale;
        private float mStep;

        public SetScaleAnimation(SEScene scene, SEObject obj, float end, float step) {
            super(scene);
            mObject = obj;
            mEndScale = end;
            mStep = step;
        }

        public void runPatch(int count) {
            mCurrentScale = mCurrentScale + mStep;
            if (Math.abs(mEndScale - mCurrentScale) <= Math.abs(mStep)) {
                stop();
            } else {
                mScale = mCurrentScale;
                SEVector3f scale = new SEVector3f(mScale, 1, mScale);
                mObject.setScale(scale.mul(mPreScale), true);
            }
        }

        @Override
        public void onFinish() {
            mCurrentScale = mEndScale;
            mScale = mCurrentScale;
            SEVector3f scale = new SEVector3f(mScale, 1, mScale);
            mObject.setScale(scale.mul(mPreScale), true);
        }

        @Override
        public void onFirstly(int count) {
            mCurrentScale = mScale;
            if (mEndScale < mCurrentScale) {
                mStep = -mStep;
            }
        }
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

}
