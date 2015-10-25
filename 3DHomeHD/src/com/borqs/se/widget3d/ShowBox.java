package com.borqs.se.widget3d;

import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SEScene;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectGroup;
import com.borqs.se.engine.SETransParas;

public class ShowBox extends SEObjectGroup {
    private ModelInfo mShowBoxInfo;

    public ShowBox(SEScene scene, ModelInfo showBoxInfo , int index) {
        super(scene, showBoxInfo.mName, index);
        mShowBoxInfo = showBoxInfo;
        setClickable(false);
    }

    public boolean load(final SEObject parent, final Runnable finish) {
        if (!mShowBoxInfo.hasInstance()) {
            if (hasBeenReleased()) {
                return false;
            }
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    mShowBoxInfo.load3DMAXModel(getScene());
                    new SECommand(getScene()) {
                        public void run() {
                            mShowBoxInfo.add3DMAXModel(getScene(), parent);
                            if (hasBeenReleased()) {
                                release();
                            } else {
                                mShowBoxInfo.register(ShowBox.this);
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
            if (hasBeenReleased()) {
                return false;
            }
            boolean result = mShowBoxInfo.getInstances().get(0).cloneObject_JNI(parent, mIndex, false, mShowBoxInfo.mStatus);
            if (result) {
                mShowBoxInfo.register(this);
                initStatus(getScene());
            }
            if (finish != null) {
                finish.run();
            }
        }
        return true;
    }

    @Override
    public void onRelease() {
        super.onRelease();
        mShowBoxInfo.unRegister(this);
    }

    public void initStatus(SEScene scene) {
        setIsEntirety_JNI(true);
        SETransParas localTrans = mShowBoxInfo.mLocalTrans;
        if (localTrans != null) {
            setLocalTranslate(localTrans.mTranslate);
            setLocalScale(localTrans.mScale);
            setLocalRotate(localTrans.mRotate);
        }
        setUserTransParas();
    }

}