package com.borqs.se.widget3d;

import android.graphics.Rect;
import android.util.Log;

import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEVector;
import com.borqs.se.home3d.HomeScene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;

/**
 * Created by b608 on 13-11-27.
 */
public class ShelfWall extends Wall {
    private static final String TAG = "ShelfWall";

    public ShelfWall(HomeScene scene, String name, int index) {
        super(scene, name, index);
    }

    /// whenever a object is seat on the its cell on wall, wall cell layer will
    /// notify home scene, which will forwards the message here.
    public void notifyObjectOnSlotSeat(NormalObject normalObject) {
        Log.d(TAG, "notifyObjectOnSlotSeat, obj name = " + normalObject.mName);
        if (null == normalObject) return;

        final boolean isDecorator = ObjectInfo.isBottomDecoratorType(normalObject);
        final ObjectSlot currentSlot = normalObject.getObjectSlot();

        if (isDecorator) {
            HashMap<Integer, NormalObject> neighbors = queryNeighbor(this, currentSlot,
                    normalObject.getObjectInfo().mType);
            makeDecoratorOverSlot(neighbors, normalObject, currentSlot);
        } else {
            ensureDecoratorWithObjectSlot(normalObject);
        }
    }

    private void ensureDecoratorWithObjectSlot(NormalObject object) {
        if (object.isAcceptedDecoratorSlot()) {
            ObjectSlot currentSlot = object.getObjectSlot();
            HashMap<Integer, NormalObject> neighbors = queryNeighbor(this, currentSlot,
                    ObjectInfo.WALL_SHELF);
            makeDecoratorOverSlot(neighbors, null, currentSlot);
        } else {
            // skip
        }
    }

    /// make decorator cross over the slot via extends/merge neighbors and decorator, if
    /// decorator is null, create one if necessary, so there MUST be one and only one
    /// decorator run across the slot while return from this method.
    private void makeDecoratorOverSlot(HashMap<Integer, NormalObject> neighbors,
                                       NormalObject decorator,
                                       ObjectSlot slot) {
        if (neighbors.isEmpty()) {
            if (null == decorator) {
                // create new decorator with slot.
                ObjectInfo.createBottomDecorator(getHomeScene(), this, slot);
            } else {
                // unexpected case, at least 1 object (current decorator) is in the map
            }
        } else {
            ArrayList<Integer> keyList = new ArrayList<Integer>(neighbors.keySet());
            Collections.sort(keyList);
            boolean isMerged = false;
            if (null != decorator) {
                int index = keyList.indexOf(slot.mStartX);
                // extends incoming object across the possible left neighbor.
                isMerged |= mergeLeftJointNeighbor(this, decorator, neighbors, keyList, index);
                // extends incoming object across the possible right neighbor.
                isMerged |= mergeRightJointNeighbor(this, decorator, neighbors, keyList, index);
            }
            
            if (!isMerged) {
                makeDecoratorOverSlot(neighbors, slot);
            }
        }
    }

    private void makeDecoratorOverSlot(HashMap<Integer, NormalObject> neighbors, ObjectSlot slot) {
        NormalObject leftDecorator = null;
        NormalObject rightDecorator = null;

        if (null != neighbors && !neighbors.isEmpty()) {
            ObjectSlot curSlot;
            for (Integer i : neighbors.keySet()) {
                if (leftDecorator != null && rightDecorator != null) {
                    break; //
                }
                curSlot = neighbors.get(i).getObjectSlot();

                if (curSlot.contain(slot)) {
                    return; // do nothing while existing a decorator over the slot.
                }

                if (leftDecorator == null &&
                        curSlot.left() < slot.left() &&
                        curSlot.right() >= slot.left()) {
                    leftDecorator = neighbors.get(i);
                }
                if (rightDecorator == null &&
                        curSlot.left() <= slot.right() &&
                        curSlot.right() > slot.right()) {
                    rightDecorator = neighbors.get(i);
                }
            }
        }


        if (null != leftDecorator && null != rightDecorator) {
            onNeighborJoined(this, leftDecorator, rightDecorator);
        } else if (null != leftDecorator) {
            extendObject(leftDecorator, slot);
        } else if (null != rightDecorator) {
            extendObject(rightDecorator, slot);
        } else {
            // both are null, create new one
            ObjectInfo.createBottomDecorator(getHomeScene(), this, slot);
        }
    }

    private static void extendObject(NormalObject leftObject, ObjectSlot slot) {
        if (null != leftObject) {
            leftObject.getObjectSlot().union(slot);
            expandDecoratorToSlot(leftObject);
        }
    }

    private static HashMap<Integer, NormalObject> queryNeighbor(VesselObject parent,
                                                                ObjectSlot currentSlot, String type) {
        final int startY = currentSlot.mStartY;
        final int spanY = currentSlot.mSpanY;
        HashMap<Integer, NormalObject> neighbors = new HashMap<Integer, NormalObject>();
        for (SEObject object : parent.getChildObjects()) {
            if (object instanceof NormalObject) {
                NormalObject current = (NormalObject)object;
                ObjectInfo objectInfo = current.getObjectInfo();
                if (type.equalsIgnoreCase(objectInfo.mType)) {
                    ObjectSlot objectSlot = current.getObjectSlot();
                    if (startY == objectSlot.mStartY && spanY == objectSlot.mSpanY) {
                        neighbors.put(objectSlot.mStartX, current);
                    }
                }
            }
        }
        return neighbors;
    }

    private static boolean mergeLeftJointNeighbor(VesselObject parent, NormalObject normalObject,
                                                  HashMap<Integer, NormalObject> neighbors,
                                                  ArrayList<Integer> keyList, int index) {
        if (null == neighbors) return false;
        if (index < 1 || index > neighbors.size() - 1) return false;

        ObjectSlot currentSlot = normalObject.getObjectSlot();
        NormalObject leftObject = neighbors.get(keyList.get(index - 1));
        ObjectSlot leftSlot = leftObject == null ? null : leftObject.getObjectSlot();
        if (null != leftSlot && currentSlot.left() <= leftSlot.right()) {
            onNeighborJoined(parent, leftObject, normalObject);
            return true;
        }
        return false;
    }

    private static boolean mergeRightJointNeighbor(VesselObject parent, NormalObject normalObject,
                                                   HashMap<Integer, NormalObject> neighbors,
                                                   ArrayList<Integer> keyList, int index) {
        if (null == neighbors) return false;
        if (index < 0 || index >= neighbors.size() - 1) return false;

        ObjectSlot currentSlot = normalObject.getObjectSlot();
        NormalObject rightObject = neighbors.get(keyList.get(index + 1));
        ObjectSlot rightSlot = rightObject == null ?
                null : rightObject.getObjectSlot();
        if (null != rightSlot && currentSlot.right() >= rightSlot.left()) {
            onNeighborJoined(parent, normalObject, rightObject);
            return true;
        }
        return false;
    }

    // merge right object to left object
    // 1. for each child of right object, change it parent and update its slot info
    // 2. remove the right object from parent vessel
    // 3. done (as the slot info of left object has been update merged outside this methods)
    private static void onNeighborJoined(VesselObject parent, NormalObject leftObject,
                                         NormalObject rightObject) {
        if (null != leftObject && null != rightObject) {
            if (leftObject == rightObject) {
                // do nothing as it identical decorator run across the slot.
                return;
            }
            ObjectSlot leftSlot = leftObject.getObjectSlot();
            ObjectSlot rightSlot = rightObject.getObjectSlot();
            leftSlot.mSpanX = rightSlot.right() - leftSlot.left();
            leftObject.getObjectInfo().mObjectSlot.set(leftSlot);

            if (leftObject instanceof VesselObject &&
                    rightObject instanceof VesselObject) {
                VesselObject leftVessel = (VesselObject)leftObject;
                VesselObject rightVessel = (VesselObject)rightObject;
                leftVessel.appendAllChildrenToVesselObject(rightVessel);
            }

            parent.removeChild(rightObject, true);
            expandDecoratorToSlot(leftObject);
        }
    }

    private static void expandDecoratorToSlot(NormalObject decorator) {
        decorator.getObjectInfo().updateSlotDB();
        decorator.expandDecoratorToSlot();
    }

    private static ArrayList<NormalObject> queryAssociations(VesselObject parent,
                                                             NormalObject decorator) {
        ObjectSlot currentSlot = decorator.getObjectSlot();
        final int top = currentSlot.top();
        final int bottom = currentSlot.bottom();
        final int left = currentSlot.left();
        final int right = currentSlot.right();
        ArrayList<NormalObject> associations = new ArrayList<NormalObject>();
        for (SEObject object : parent.getChildObjects()) {
            if (object instanceof NormalObject) {
                NormalObject current = (NormalObject)object;
                ObjectSlot objectSlot = current.getObjectSlot();
                if (!ObjectInfo.isBottomDecoratorType(current) &&
                        current.isAcceptedDecoratorSlot()) {
                    if (top == objectSlot.top() && bottom == objectSlot.bottom()) {
                        if (left <= objectSlot.left() && right >= objectSlot.right()) {
                            associations.add(current);
                        }
                    }
                }
            }
        }
        return associations;
    }

    public Rect getAssociationFrameRect(NormalObject decorator) {
        if (ObjectInfo.isBottomDecoratorType(decorator)) {
            ObjectSlot slot = decorator.getObjectSlot();
            if (null != slot) {
                ArrayList<NormalObject> associations = queryAssociations(this, decorator);
                if (!associations.isEmpty()) {
                    int left = 0;
                    int right = 0;
                    ObjectSlot curSlot;
                    for (NormalObject object : associations) {
                        curSlot = object.getObjectSlot();
                        if (left > curSlot.left()) {
                            left = curSlot.left();
                        }
                        if (right < curSlot.right()) {
                            right = curSlot.right();
                        }
                    }
                   return new Rect(left, slot.top(), right, slot.bottom());
                }
            }
        }
        return null;
    }

    private NormalObject mLeavingDecorator;
    @Override
    public void onFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            // the current vessel becomes active
        } else {
            // the current vessel becomes inactive
            cleanEmptyDecorator();
        }
    }

    private static NormalObject queryDecorator(VesselObject parent, NormalObject normalObj) {
        if (normalObj.isAcceptedDecoratorSlot()) {
            ObjectSlot currentSlot = normalObj.getObjectSlot();
            final int top = currentSlot.top();
            final int bottom = currentSlot.bottom();
            final int left = currentSlot.left();
            final int right = currentSlot.right();
            for (SEObject object : parent.getChildObjects()) {
                if (object instanceof NormalObject) {
                    NormalObject current = (NormalObject)object;
                    ObjectSlot objectSlot = current.getObjectSlot();
                    if (ObjectInfo.isBottomDecoratorType(current)) {
                        if (top == objectSlot.top() && bottom == objectSlot.bottom()) {
                            if (left >= objectSlot.left() && right <= objectSlot.right()) {
                                return current;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public void initObjectOnDecorator(NormalObject movingObject) {
        if (ObjectInfo.isBottomDecoratorType(movingObject)) {
            ArrayList<NormalObject> list = queryAssociations(this, movingObject);
            movingObject.setMovingAssociation(list);
            mLeavingDecorator = null;
        } else if (movingObject.isAcceptedDecoratorSlot()) {
            mLeavingDecorator = queryDecorator(this, movingObject);
        }
    }

    public boolean finishObjectOnDecorator(NormalObject movingObject, ObjectInfo.ObjectSlot placeSlot) {
        if (ObjectInfo.isBottomDecoratorType(movingObject)) {
            movingObject.releaseMovingAssociation(placeSlot);
            return true;
        } else {
            if (null != mLeavingDecorator) {
                ObjectSlot slot = mLeavingDecorator.getObjectSlot();
                ObjectSlot targetSlot = movingObject.getObjectSlot();
                if (slot.mSpanX == targetSlot.mSpanX) {
                    // move the decorator to the position of moving object.
                    mLeavingDecorator.getObjectSlot().set(targetSlot.clone());
                    mLeavingDecorator.getObjectInfo().updateSlotDB();
                    SEVector.SEVector3f desLocation = mLeavingDecorator.getSlotTranslateInVessel(mLeavingDecorator.getVesselParent(), targetSlot);
                    mLeavingDecorator.setUserTranslate(desLocation);
                    notifyObjectOnSlotSeat(mLeavingDecorator);
                } else {
                    cleanEmptyDecorator();
                }
                mLeavingDecorator = null;
                return true;
            }
        }
        return false;
    }

    private void cleanEmptyDecorator() {
        if (testEmptyAndRemove(mLeavingDecorator)) {
            mLeavingDecorator = null;
        }

        VesselObject parentVessel = getVesselParent();
        if (null != parentVessel) {
            parentVessel.removeAllEmptyDecorator();
        }
    }

    private boolean isEmptyDecorator(NormalObject object) {
        if (null != object) {
            return queryAssociations(this, object).isEmpty();
        }
        return false;
    }

    private boolean testEmptyAndRemove(NormalObject object) {
        boolean result = false;
        if (isEmptyDecorator(object)) {
            object.getParent().removeChild(object, true);
            result = true;
        }

        return result;
    }

    public void removeAllEmptyDecorator() {
        List<SEObject> children = getChildObjects();
        if (null == children || children.isEmpty()) {
            return;
        }

        ArrayList<NormalObject> emptyList = new ArrayList<NormalObject>();
        for (SEObject object : children) {
            if (object instanceof NormalObject) {
                NormalObject current = (NormalObject)object;
                if (ObjectInfo.isBottomDecoratorType(current)) {
                    if (isEmptyDecorator(current)) {
                        emptyList.add(current);
                    }
                }
            }
        }
        
        for (NormalObject decorator : emptyList) {
            removeChild(decorator, true);
        }
    }

    @Override
    public SETransParas getTransParasInVessel(NormalObject needPlaceObj, ObjectSlot objectSlot) {
        SETransParas paras = super.getTransParasInVessel(needPlaceObj, objectSlot);

        if (null != needPlaceObj) {
            SEVector.SEVector3f offsetTranslate = needPlaceObj.getDecoratorTranslate();
            paras.mTranslate.selfAdd(offsetTranslate);
        }
        
        return paras;
    }

    @Override
    public void resetOverlapDecorator(NormalObject object) {
        ensureDecoratorWithObjectSlot(object);
        cleanEmptyDecorator();

        // todo: resize any overlap shelf
//        List<SEObject> children = getChildObjects();
//        if (null == children || children.isEmpty()) {
//            return;
//        }
//
//        ArrayList<NormalObject> emptyList = new ArrayList<NormalObject>();
//        for (SEObject object : children) {
//            if (object instanceof NormalObject) {
//                NormalObject current = (NormalObject)object;
//                if (ObjectInfo.isBottomDecoratorType(current)) {
//                    if (isEmptyDecorator(current)) {
//                        emptyList.add(current);
//                    }
//                }
//            }
//        }
//
//        for (NormalObject decorator : emptyList) {
//            removeChild(decorator, true);
//        }
    }

    @Override
    public void checkAndSetDecoratorVisibility(boolean show) {
        NormalObject current;
        ArrayList<NormalObject> decoratorList = new ArrayList<NormalObject>();
        ArrayList<NormalObject> otherList = new ArrayList<NormalObject>();
        for (SEObject object : getChildObjects()) {
            if (object instanceof NormalObject) {
                current = (NormalObject)object;
                if (ObjectInfo.isBottomDecoratorType(current)) {
                    decoratorList.add(current);
                } else {
                    otherList.add(current);
                }
            }
        }

        for (NormalObject decorator : decoratorList) {
            decorator.setVisible(show);
        }

        if (show) {
            for (NormalObject object : otherList) {
                notifyObjectOnSlotSeat(object);
            }
            cleanEmptyDecorator();
        }
    }

    @Override
    public boolean notifyDecoratorSlotDown(NormalObject normalObject) {
        notifyObjectOnSlotSeat(normalObject);
        return true;
    }

    @Override
    protected void ensureDecoratorForChild(SEObject child, int index) {
        if (index != 0 && child instanceof NormalObject) {
            NormalObject object = (NormalObject)child;
            ensureDecoratorWithObjectSlot(object);
        }
    }
}
