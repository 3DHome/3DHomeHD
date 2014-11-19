package com.borqs.se.widget3d;

import com.borqs.se.engine.SEXMLAnimation;
import com.borqs.se.engine.SECamera.CameraChangedListener;
import com.borqs.se.home3d.HomeScene;

public class EmailBox extends NormalObject implements CameraChangedListener {
    private SEXMLAnimation mOpenEmailBox;

    public EmailBox(HomeScene scene, String name, int index) {
        super(scene, name, index);
    }

    @Override
    public void initStatus() {
        super.initStatus();
        mOpenEmailBox = new SEXMLAnimation(getScene(), "assets/base/email/animation.xml", mIndex);
        getCamera().addCameraChangedListener(this);
        setHasInit(true);
    }

    public void onCameraChanged() {
        if (getHomeScene().getSightValue() == -1) {
            if (mOpenEmailBox != null) {
                mOpenEmailBox.setIsReversed(false);
                mOpenEmailBox.execute();
            }
        } else if (getHomeScene().getSightValue() == 0) {
            if (mOpenEmailBox != null) {
                mOpenEmailBox.setIsReversed(true);
                mOpenEmailBox.execute();
            }
        }
    }

    @Override
    public void onRelease() {
        super.onRelease();
        mOpenEmailBox.pause();
        mOpenEmailBox = null;
        getCamera().removeCameraChangedListener(this);
    }

}
