package com.borqs.framework3d.home3d;

import com.borqs.se.engine.SEVector;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEScene;
import com.borqs.se.widget3d.NormalObject;
import com.borqs.se.widget3d.ObjectInfo;
import com.borqs.se.widget3d.VesselObject;

public abstract class ConflictVesselObject extends VesselObject {
    public ConflictVesselObject(SEScene scene, String name, int index) {
        super(scene, name, index);
    }


    protected static class ConflictAnimation extends SEAnimation.CountAnimation {
        //        private float mCurAngle;
//        private float mEndAngle;
        private float mStep;
        private boolean mIsEnbaleBlending;
        private boolean mHasGetBlending;

        private SEVector.SEVector3f mEndPosition;
        private SEVector.SEVector3f mStartPosition;
        private ConflictVesselObject mDock;
        private SEObject mConflictObject;

        private SEVector.SEVector3f mMidPoint;
        private String mTraceType;
        private int mAnimCount;

        public void setTraceType(String t) {
            mTraceType = t;
        }

        public void setAnimCount(int c) {
            mAnimCount = c;
        }

        public ConflictAnimation(ConflictVesselObject dockObject, SEObject conflictObject,
                                 ObjectInfo.ObjectSlot NextSlot, float step) {
            super(dockObject.getScene());

            mDock = dockObject;
            mConflictObject = conflictObject;

            mStep = step;

            mStartPosition = mConflictObject.getUserTranslate();
            mEndPosition = mDock.getSlotPosition(mConflictObject.mName, NextSlot.mSlotIndex);
            mMidPoint = new SEVector.SEVector3f(0, 0, (mStartPosition.getZ() + mEndPosition.getZ()) / 2);
//            mCurAngle = getPositionAngle(mConflictObject.getUserTranslate());
//            mEndAngle = NextSlot.mSlotIndex * 360.f / mDock.getCount();
//            if (mEndAngle - mCurAngle > 180) {
//                mEndAngle = mEndAngle - 360;
//            } else if (mEndAngle - mCurAngle < -180) {
//                mEndAngle = 360 + mEndAngle;
//            }
        }

        private SEVector.SEVector3f getCurrentTranslate(float t) {
            SEVector.SEVector3f v1 = mStartPosition.subtract(mMidPoint);
            SEVector.SEVector3f v2 = mEndPosition.subtract(mMidPoint);
            SEVector.SEVector3f translate = vectorInterpolate(v1, v2, t);
            return mMidPoint.add(translate);
        }

        @Override
        public int getAnimationCount() {
            return mAnimCount;
        }

        @Override
        public void runPatch(int count) {
               /*
            float needTranslate = mEndAngle - mCurAngle;
            float absNTX = Math.abs(needTranslate);
            if (absNTX <= mStep) {
                mCurAngle = mEndAngle;
                stop();
            } else {
                int step = (int) (mStep * Math.sqrt(absNTX));
                if (needTranslate < 0) {
                    step = -step;
                }
                mCurAngle += step;
            }
            mConflictObject.setTranslate(getAnglePosition(mDock, mCurAngle), true);
            */
            float t = count / (float) (getAnimationCount());
            SEVector.SEVector3f translate = null;
            if (mTraceType.equals("circle")) {
                translate = this.getCurrentTranslate(t);
            } else {
                translate = mEndPosition.subtract(mStartPosition).mul(t).add(mStartPosition);
            }
            mConflictObject.setTranslate(translate, true);
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
            mConflictObject.setAlpha(0.2f, true);
        }

        @Override
        public void onFinish() {
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

    public static class ConflictObject {
        private SEAnimation.CountAnimation mConflictAnimation;
        private NormalObject mConflictObject;
        private ObjectInfo.ObjectSlot mMoveSlot;

        private ConflictVesselObject mDock;

        public ConflictObject(ConflictVesselObject dockObject, NormalObject object, ObjectInfo.ObjectSlot slot) {
            mDock = dockObject;
            mConflictObject = object;
            mMoveSlot = slot;
        }
        public NormalObject getObject() {
        	return mConflictObject;
        }
        // confirm to next position
        public void playConflictAnimation(final SEAnimFinishListener postExecutor) {
            if (mConflictAnimation != null) {
                mConflictAnimation.stop();
            }
            if (mMoveSlot == null) {
                return;
            }
            mConflictObject.getObjectSlot().set(mMoveSlot);
            mConflictAnimation = mDock.createConflictAnimation(mConflictObject, mMoveSlot, 3);
//            mConflictAnimation = new ConflictAnimation(mDock, mConflictObject, mMoveSlot, 3);
            mConflictAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    mConflictObject.getObjectInfo().updateSlotDB();
                    if (null != postExecutor) {
                        postExecutor.onAnimationfinish();
                    }
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

        public int getSlotIndex() {
            return mConflictObject.getObjectInfo().getSlotIndex();
        }

        public void cloneSlot() {
            mMoveSlot = mConflictObject.getObjectInfo().mObjectSlot.clone();
        }

        public ObjectInfo.ObjectSlot getConflictSlot() {
            return mConflictObject.getObjectInfo().mObjectSlot;
        }

        public void setMovingSlotIndex(int slotIndex) {
            mMoveSlot.mSlotIndex = slotIndex;
        }

        public void clearMovingSlot() {
            mMoveSlot = null;
        }
    }


    public static SEVector.SEVector3f vectorInterpolate(SEVector.SEVector3f v1, SEVector.SEVector3f v2, float t) {
        double theta = v1.thetaBetween(v2);
        double sinTheta = Math.sin(theta);
        double sinTTheta = Math.sin(t * theta);
        double sinATheta = Math.sin((1 - t) * theta);
        SEVector.SEVector3f t1 = v1.mul((float) sinATheta).div((float) sinTheta);
        SEVector.SEVector3f t2 = v2.mul((float) sinTTheta).div((float) sinTheta);
        return t1.add(t2);
    }

    abstract public float getBorderHeight();
    abstract public SEVector.SEVector3f getSlotPosition(String objectName, int slotIndex);
    abstract protected SEAnimation.CountAnimation createConflictAnimation(NormalObject conflictObject, ObjectInfo.ObjectSlot slot, int step);
}
