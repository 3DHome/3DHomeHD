package com.borqs.framework3d.home3d;

import com.borqs.se.engine.SEVector;
import com.borqs.se.engine.SEAnimation;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.widget3d.NormalObject;

/**
 * Created with IntelliJ IDEA.
 * User: yangfeng
 * Date: 13-5-12
 * Time: 上午12:10
 * To change this template use File | Settings | File Templates.
 */
public class AnimationFactory {
    private AnimationFactory() {
        // no instance
    }

    public static CountAnimation createSetPositionAnimation(NormalObject object,
                                                            SETransParas srcTransParas,
                                                            SETransParas desTransParas, int count) {
        return new SetToRightPositionAnimation(object, srcTransParas, desTransParas, count);
    }

    public static CountAnimation createMoveObjectAnimation(SEObject object,
                                                           SEVector.SEVector3f srcLocation,
                                                           SEVector.SEVector3f desLocation,
                                                           float step) {
        return new MoveObjectAnimation(object, srcLocation, desLocation, step);
    }

    public static CountAnimation createRunCircleAnimation(SEObject object, float height) {
        return new RunACircleAnimation(object, height);
    }

    public static CountAnimation createSetFace(SEObject object, int face, float step) {
        return new ToFaceAnimation(object, face, step);
    }

    public static CountAnimation createVelocityRotateAnimation(SEObject object, float velocity,
                                                               float direct, float aFaceAngle) {
        return new DeskVelocityAnimation(object, velocity, direct, aFaceAngle);
    }

    private static class MoveObjectAnimation extends CountAnimation {
        private float mStep;
        private SEVector.SEVector3f mDirect;
        private SEVector.SEVector3f mDLocation;
        private SEVector.SEVector3f mSLocation;
        private SEObject mNormalObject;
        private int mCur;
        private int mEnd;

        public MoveObjectAnimation(SEObject object, SEVector.SEVector3f srcLocation,
                                   SEVector.SEVector3f desLocation, float step) {
            super(object.getScene());
            mNormalObject = object;
            mSLocation = srcLocation;
            mDLocation = desLocation;
            mStep = step;
            mCur = 0;
        }

        @Override
        public void runPatch(int count) {
            if (mCur != mEnd) {
                int needTranslate = mEnd - mCur;
                int absNTX = Math.abs(needTranslate);
                if (absNTX <= mStep) {
                    mCur = mEnd;
                } else {
                    int step = (int) (mStep * Math.sqrt(absNTX));
                    if (needTranslate < 0) {
                        step = -step;
                    }
                    mCur = mCur + step;
                }
            }
            if (mCur == mEnd) {
                stop();
            }
            mNormalObject.setTranslate(mSLocation.add(mDirect.mul(mCur)), true);
        }

        @Override
        public void onFirstly(int count) {
            mDirect = mDLocation.subtract(mSLocation);
            mEnd = (int) mDirect.getLength();
            mDirect.normalize();
            mNormalObject.setRotate(new SEVector.SERotate(0, 0, 0, 1), true);
        }
    }

    private static class SetToRightPositionAnimation extends SEAnimation.CountAnimation {
        private SETransParas mSrcTransParas;
        private SETransParas mDesTransParas;
        private NormalObject mObject;
        private int mCount;
        private float mStep;

        public SetToRightPositionAnimation(NormalObject object, SETransParas srcTransParas,
                                           SETransParas desTransParas, int count) {
            super(object.getScene());
            mObject = object;
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

    private static class RunACircleAnimation extends CountAnimation {
        private float mDesAngle;
        private float mCurAngle;
        private float mHeight;

        private SEObject mObject;

        public RunACircleAnimation(SEObject object, float height) {
            super(object.getScene());

            mObject = object;
            mHeight = height;
        }

        @Override
        public void runPatch(int count) {
            float needRotateAngle = mDesAngle - mCurAngle;
            float absNRA = Math.abs(needRotateAngle);
            float step = (float) Math.sqrt(absNRA);
            float height = mHeight - (float) count * mHeight / 36f;
            if (absNRA < 1) {
                mCurAngle = mDesAngle;
                height = 0;
                stop();
            } else {
                if (needRotateAngle < 0) {
                    step = -step;
                }
                mCurAngle = mCurAngle + step;
            }
            if (Math.abs(height) > Math.abs(mHeight)) {
                height = 0;
            }
            mObject.setRotate(new SEVector.SERotate(mCurAngle), true);
            mObject.setTranslate(new SEVector.SEVector3f(0, 0, height), true);
        }

        @Override
        public void onFirstly(int count) {
            mCurAngle = mObject.getUserRotate().getAngle();
            if (mCurAngle > 180) {
                mDesAngle = 0;
            } else {
                mDesAngle = 360;
            }
        }
    }

    private static class ToFaceAnimation extends CountAnimation {
        private float mStep;
        private float mDesAngle;
        private float mCurAngle;
        private int mFace;

        private SEObject mObject;

        public ToFaceAnimation(SEObject object, int face, float step) {
            super(object.getScene());
            mStep = step;
            mFace = face;

            mObject = object;
        }

        public void runPatch(int count) {
            float needRotate = mDesAngle - mCurAngle;
            float absNTR = Math.abs(needRotate);
            if (absNTR <= mStep) {
                mCurAngle = mDesAngle;
                stop();
            } else {
                float step = mStep;
                if (needRotate < 0) {
                    step = -mStep;
                }
                mCurAngle = mCurAngle + step;
            }
            mObject.setRotate(new SEVector.SERotate(mCurAngle), true);
        }

        public void onFirstly(int count) {
            mCurAngle = mObject.getUserRotate().getAngle();
            mDesAngle = 360 - mFace * 30;
            float needRotateAngle = Math.abs(mDesAngle - mCurAngle);
            if (needRotateAngle > 180) {
                needRotateAngle = 360 - needRotateAngle;
                if (mDesAngle - mCurAngle > 180) {
                    mDesAngle = mCurAngle - needRotateAngle;
                } else if (mDesAngle - mCurAngle < -180) {
                    mDesAngle = mCurAngle + needRotateAngle;
                }
            }
        }
    }

    private static class DeskVelocityAnimation extends CountAnimation {
        private float mVelocity;
        private float mCurrentAngle;
        private float mDesAngle;
        private float mPerFaceAngle;

        private SEObject mObject;

        public DeskVelocityAnimation(SEObject object, float velocity,
                                     float direct, float aFaceAngle) {
            super(object.getScene());
            mVelocity = velocity;
            if (Math.abs(mVelocity) < 100) {
                mVelocity = 0;
            }
            if (direct < 0) {
                mVelocity = -mVelocity;
            }
            mPerFaceAngle = aFaceAngle;;

            mObject = object;
        }

        @Override
        public void runPatch(int count) {
            float needRotateAngle = mDesAngle - mCurrentAngle;
            float absNRA = Math.abs(needRotateAngle);
            if (absNRA < 1) {
                mCurrentAngle = mDesAngle;
                stop();
            } else {
                float step = (float) Math.sqrt(absNRA);
                if (needRotateAngle < 0) {
                    step = -step;
                }
                mCurrentAngle = mCurrentAngle + step;
            }
            mObject.setRotate(new SEVector.SERotate(mCurrentAngle), true);
        }

        @Override
        public void onFirstly(int count) {
            mCurrentAngle = mObject.getUserRotate().getAngle();
            float needRotateAngle = mVelocity * 0.05f;
            float desAngle = mCurrentAngle + needRotateAngle;
            if (desAngle >= 0) {
                int index = (int) ((desAngle + mPerFaceAngle / 2) / mPerFaceAngle);
                mDesAngle = mPerFaceAngle * index;
            } else {
                int index = (int) ((desAngle - mPerFaceAngle / 2) / mPerFaceAngle);
                mDesAngle = mPerFaceAngle * index;
            }
        }
    }

    /// show/hide dock animation begin
    /// TODO: merge ShowDeskAnimation with HideDeskAnimation
    public static CountAnimation createShowHideDockAnimation(SEObject object, SEObject ground, boolean hide) {
        if (hide) {
            return new HideDeskAnimation(object, ground);
        } else {
            return new ShowDeskAnimation(object, ground);
        }
    }

    private static class HideDeskAnimation extends CountAnimation {
        private SEObject mGround;
        private SEObject mObject;

        private float mDeskTranslateZ;

        public HideDeskAnimation(SEObject object, SEObject ground) {
            super(object.getScene());

            mObject = object;
            mGround = ground;
//            mGround = findGround();
            mDeskTranslateZ = object.getUserTranslate().getZ();
        }

        public void runPatch(int count) {
            mDeskTranslateZ = mDeskTranslateZ - 14;
            if (mDeskTranslateZ <= -140) {
                mDeskTranslateZ = -140;
                stop();
            }
            float alpha;
            if (mDeskTranslateZ >= -70) {
                alpha = 1 + mDeskTranslateZ / 70;
            } else {
                alpha = -1 - mDeskTranslateZ / 70;
            }
            if (mGround != null) {
                mGround.setAlpha(alpha, true);
            }
            float scale = 1 + mDeskTranslateZ / 300;
            mObject.getUserTransParas().mScale.set(scale, scale, scale);
            mObject.getUserTransParas().mTranslate.set(0, 0, mDeskTranslateZ);
            mObject.setUserTransParas();
        }

        @Override
        public void onFinish() {
            if (mGround != null) {
                mGround.setBlendingable(false, true);
            }
            mObject.setVisible(false, true);
        }

        @Override
        public void onFirstly(int count) {
            if (mGround != null) {
                mGround.setBlendingable(true, true);
            }
        }

    }
    private static class ShowDeskAnimation extends CountAnimation {
        private SEObject mGround;
        private SEObject mObject;

        private float mDeskTranslateZ;

        public ShowDeskAnimation(SEObject object, SEObject ground) {
            super(object.getScene());

            mObject = object;
            mGround = ground;
//            mGround = findGround();
            mDeskTranslateZ = object.getUserTranslate().getZ();
        }

        public void runPatch(int count) {
            mDeskTranslateZ = mDeskTranslateZ + 14;
            if (mDeskTranslateZ >= 0) {
                mDeskTranslateZ = 0;
                stop();
            }
            float alpha;
            if (mDeskTranslateZ >= -70) {
                alpha = 1 + mDeskTranslateZ / 70;
            } else {
                alpha = -1 - mDeskTranslateZ / 70;
            }
            if (mGround != null) {
                mGround.setAlpha(alpha, true);
            }
            float scale = 1 + mDeskTranslateZ / 300;
            mObject.getUserTransParas().mScale.set(scale, scale, scale);
            mObject.getUserTransParas().mTranslate.set(0, 0, mDeskTranslateZ);
            mObject.setUserTransParas();
        }

        @Override
        public void onFinish() {
            if (mGround != null) {
                mGround.setBlendingable(false, true);
            }

        }

        @Override
        public void onFirstly(int count) {
            mObject.setVisible(true, true);
            if (mGround != null) {
                mGround.setBlendingable(true, true);
            }
        }

    }
    /// show/hide dock animation end
}
