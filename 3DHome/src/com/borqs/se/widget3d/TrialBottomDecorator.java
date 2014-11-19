package com.borqs.se.widget3d;

import android.graphics.Rect;

import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEVector.SEVector2i;
import com.borqs.se.home3d.HomeScene;

import java.util.ArrayList;

/**
 * Created by b608 on 13-11-29.
 */
/// A simple class for those model that could be attach to the bottom of
/// other object, e.g., shelf on the wall. these model should not be occupy
/// Z-axis length more than the gap of wall.
public class TrialBottomDecorator extends NormalObject {
    public static final boolean ENABLE_RESIZE = false; // no resize feature
    public static final boolean ENABLE_MOVE = false;   // no move feature.
    public static final boolean ENABLE_DELETE = false; // enable delete via long press

    public TrialBottomDecorator(HomeScene homeScene, String name, int index) {
        super(homeScene, name, index);
    }

    // only be sensitive with x-axis change.
    @Override
    protected void updateObjectSlotRect(Rect sizeRect) {
        final Rect frameRect = getHomeScene().getAssociationFrameRect(this);
        if (null != frameRect) {
            int left = Math.min(sizeRect.left, frameRect.left);
            int right = Math.max(sizeRect.right, frameRect.right);

            getObjectSlot().mStartX = left;
            getObjectSlot().mSpanX = right - left;
            getObjectInfo().updateSlotDB();
        }
    }

    @Override
    public void onSizeAndPositionChanged(Rect sizeRect) {
        updateObjectSlotRect(sizeRect);
        expandDecoratorToSlot();
    }

    @Override
    public boolean canUninstall() {
        return false;
    }

    @Override
    public boolean canBeResized() {
        if (super.canBeResized()) {
            SEObject parent = getVesselParent();
            if (null != parent) {
                return ENABLE_RESIZE;
            }
        }

        return false;
    }

    @Override
    public void handleOutsideRoom() {
        // remove all association
        NormalObject object;
        for (ObjectOnShelfPosition association : mMovingAssociations) {
            object = association.object;
            object.getParent().removeChild(object, true);
        }
        mMovingAssociations.clear();
        mMovingAssociations = null;
        super.handleNoMoreRoom();
    }

    /// moving associations begin
    private class ObjectOnShelfPosition {
        public NormalObject object;
        public SETransParas transParas;
        public SEVector2i slotOffset;

        public ObjectOnShelfPosition(NormalObject obj, SETransParas trans,
                                     SEVector2i slotOffset) {
            object = obj;
            transParas = trans;
            this.slotOffset = slotOffset;
        }
    }
    private ArrayList<ObjectOnShelfPosition> mMovingAssociations;
    public ArrayList<NormalObject> getMovingAssociation() {
        if (null != mMovingAssociations && !mMovingAssociations.isEmpty()) {
            ArrayList<NormalObject> list = new ArrayList<NormalObject>();
            for (ObjectOnShelfPosition item : mMovingAssociations) {
                list.add(item.object);
            }
            return list;
        }
        return null;
    }
    public void setMovingAssociation(ArrayList<NormalObject> list) {
        if (null == list || list.isEmpty()) {
            mMovingAssociations = null;
        }

        ObjectInfo.ObjectSlot initSlot = getObjectSlot();
        SETransParas initTrans = getUserTransParas();
        mMovingAssociations = new ArrayList<ObjectOnShelfPosition>();
        SETransParas transParas;
        ObjectInfo.ObjectSlot slot;
        SEVector2i slotOffset;
        for (NormalObject object : list) {
            transParas = object.getUserTransParas().clone();
            transParas.mTranslate.selfSubtract(initTrans.mTranslate);
            transParas.mScale.selfSubtract(initTrans.mScale);
            transParas.mRotate.selfSubtract(initTrans.mRotate);

            slot = object.getObjectSlot();

            slotOffset = new SEVector2i(slot.left() - initSlot.left(), slot.top() - initSlot.top());
            mMovingAssociations.add(new ObjectOnShelfPosition(object, transParas, slotOffset));
        }
    }

    public void releaseMovingAssociation(ObjectInfo.ObjectSlot placeSlot) {
        if (null != mMovingAssociations && !mMovingAssociations.isEmpty()) {
            VesselObject vessel = getVesselParent();
            if (null != vessel) {
                ObjectInfo.ObjectSlot slot;
                for (ObjectOnShelfPosition association : mMovingAssociations) {
                    slot = association.object.getObjectSlot();
                    slot.mSpanX = association.slotOffset.getX() + placeSlot.left();
                    slot.mSpanY = association.slotOffset.getY() + placeSlot.top();
                    association.object.getObjectSlot().set(slot);
                    association.object.getObjectInfo().updateSlotDB();
                }
            }
            mMovingAssociations.clear();
            mMovingAssociations = null;
        }
    }

    public void setUserTransParas(SETransParas trans) {
        if (null == mMovingAssociations || mMovingAssociations.isEmpty()) {
            super.setUserTransParas(trans);
        } else {
            super.setUserTransParas(trans);
            NormalObject object;
            for (ObjectOnShelfPosition association : mMovingAssociations) {
                object = association.object;
                object.getUserTransParas().mTranslate = association.transParas.mTranslate.add(trans.mTranslate);
                object.getUserTransParas().mScale = association.transParas.mScale.add(trans.mScale);
                object.getUserTransParas().mRotate= association.transParas.mRotate.add(trans.mRotate);
                object.setUserTransParas();
            }
        }
    }

    @Override
    protected void onLongClick() {
        if (ENABLE_MOVE) {
            super.onLongClick();
        } else {
            if (ENABLE_DELETE) {
                getScene().handleMessage(HomeScene.MSG_TYPE_SHOW_OBJECT_LONG_CLICK_DIALOG, this);
            }
        }
    }

    @Override
    public void initStatus() {
        scaleNativeWithinWallCell();
        super.initStatus();
        checkAndSetVisibility();
        setCanChangeBind(false);
        expandDecoratorToSlot();
    }

    public void scaleNativeWithinWallCell() {
        final float targetLen = getWallCellWidth(1);
        final float currentLen = getObjectSize().getX();
        if (targetLen > 0 && currentLen > 0 && targetLen != currentLen) {
            SEObject modelComponent = getModelComponenet();
            if (null != modelComponent) {
                SEVector3f scale = modelComponent.getUserScale();
                float ratio = targetLen / currentLen;
                modelComponent.setUserScale(scale.mul(ratio));
            }
        }
//
//        SEVector3f translate = getSlotTranslateInVessel(getVesselParent(), getObjectSlot());
//        if (null != translate) {
//            setLocalTranslate(translate);
//        }
    }

    private static final float HEIGHT = 20;
    @Override
    public SEVector3f getDecoratorTranslate() {
        final float offsetZ = HEIGHT - 0.5f * getWallCellSize().getY();
        return new SEVector3f(0f, 0f, offsetZ);
    }

    @Override
    public void expandDecoratorToSlot() {
        SEVector3f objectSize = getObjectSize();
        final float targetLen = getExpectedNativeX();
        final float currentLen = objectSize.getX();
        if (targetLen > 0 && currentLen > 0 && targetLen != currentLen) {
            SEObject modelComponent = getModelComponenet();
            if (null != modelComponent) {
                SEVector3f scale = modelComponent.getUserScale().clone();
//                scale.set(targetLen / currentLen, HEIGHT / objectSize.getY(),
//                        HEIGHT / objectSize.getZ());
                scale.set(targetLen / currentLen, scale.getY(), scale.getZ());
                modelComponent.setUserScale(scale);
            }
        }

        SEVector3f translate = getSlotTranslateInVessel(getVesselParent(), getObjectSlot());
        if (null != translate) {
            setUserTranslate(translate);
        }
    }

    @Override
    public boolean update(SEScene scene) {
        checkAndSetVisibility();
        return true;
    }

    private void checkAndSetVisibility() {
        if (isLabelShown()) {
            setVisible(false);
        } else {
            setVisible(true);
        }
    }
}
