package com.borqs.se.widget3d;

import android.view.MotionEvent;

import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEAnimation.CountAnimation;

import com.borqs.se.engine.SEObject;

public class WallGlobe extends WallNormalObject {
    private SEObject mSEObject_0;
    private SEObject mSEObject_1;
    private SEObject mSEObject_3;
    private float mAngle;
    private UpdateAnimation mUpdateAnimation;

    public WallGlobe(SEScene scene, String name, int index) {
        super(scene, name, index);
    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        mSEObject_0 = new SEObject(scene, getObjectInfo().mModelInfo.mChildNames[0], mIndex);
        mSEObject_1 = new SEObject(scene, getObjectInfo().mModelInfo.mChildNames[1], mIndex);
        mSEObject_3 = new SEObject(scene, getObjectInfo().mModelInfo.mChildNames[2], mIndex);
        mAngle = 0;
        setHasInit(true);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mUpdateAnimation != null) {
                mUpdateAnimation.stop();
            }
        }
        return super.dispatchTouchEvent(event);
    }

    public void onWallRadiusChanged(int faceIndex) {
        if (isPressed()) {
            return;
        }
        if (faceIndex == getObjectInfo().getSlotIndex()) {
            if (mUpdateAnimation == null || mUpdateAnimation.isFinish()) {
                mUpdateAnimation = new UpdateAnimation(getScene());
                mUpdateAnimation.execute();
            }
        }

    }

    private class UpdateAnimation extends CountAnimation {

        public UpdateAnimation(SEScene scene) {
            super(scene);
        }

        public void runPatch(int count) {
            mAngle += 1;
            mAngle = mAngle % 360;
            boolean stopStatus = false;
            if (mAngle % 120 == 0) {
                stopStatus = true;
            }
            mSEObject_1.setRotate(new SERotate(mAngle, 0, 1, 0), true);
            if (mAngle < 60 || (mAngle > 120 && mAngle < 180) || (mAngle > 240 && mAngle < 300)) {
                mSEObject_3.setRotate(new SERotate(mAngle, 0, 1, 0), true);
                mSEObject_0.rotateObject(new SERotate(-1, 0, 1, 0));
            } else {
                if (stopStatus) {
                    mSEObject_3.setRotate(new SERotate(mAngle, 0, 1, 0), true);
                } else {
                    mSEObject_3.setRotate(new SERotate(-mAngle, 0, 1, 0), true);
                }
                mSEObject_0.rotateObject(new SERotate(1, 0, 1, 0));
            }
            if (stopStatus) {
                stop();
            }
        }
    }
    
    @Override
    public void onRelease() {
        super.onRelease();
    }
}
