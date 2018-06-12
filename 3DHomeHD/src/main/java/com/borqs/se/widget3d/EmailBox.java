package com.borqs.se.widget3d;

import com.borqs.se.engine.SEXMLAnimation;
import com.borqs.se.engine.SECamera.CameraChangedListener;
import com.borqs.se.engine.SEScene;

public class EmailBox extends NormalObject implements CameraChangedListener {
    private SEXMLAnimation mOpenEmailBox;

    public EmailBox(SEScene scene, String name, int index) {
        super(scene, name, index);
    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        mOpenEmailBox = new SEXMLAnimation(getScene(), "assets/base/email/animation.xml", mIndex);
        getCamera().addCameraChangedListener(this);
        setHasInit(true);
    }

    public void onCameraChanged() {
        if (getCamera().isGroundSight()) {
          if (mOpenEmailBox != null) {
              mOpenEmailBox.setIsReversed(false);
              mOpenEmailBox.execute();
          }
        } else if (getCamera().isDefaultSight()) {
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
