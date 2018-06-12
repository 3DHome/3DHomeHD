package com.borqs.se.widget3d;

import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEXMLAnimation;

public class Navigation extends WallNormalObject {
    private SEXMLAnimation mNavigationAnim;
    private boolean mIsOpen;

    public Navigation(SEScene scene, String name, int index) {
        super(scene, name, index);
    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        mNavigationAnim = new SEXMLAnimation(getScene(), getObjectInfo().mModelInfo.mAssetsPath + "/animation.xml",
                mIndex);
        mIsOpen = false;
    }

    public void onWallRadiusChanged(int faceIndex) {
        if (isPressed()) {
            return;
        }
        if (faceIndex == getObjectInfo().getSlotIndex()) {
            if (mNavigationAnim != null && !mIsOpen) {
                mIsOpen = true;
                mNavigationAnim.setIsReversed(false);
                mNavigationAnim.execute();
            }
        } else {
            if (mNavigationAnim != null && mIsOpen) {
                mIsOpen = false;
                mNavigationAnim.setIsReversed(true);
                mNavigationAnim.execute();
            }
        }

    }

    @Override
    public void onRelease() {
        super.onRelease();
        mNavigationAnim.pause();
        mNavigationAnim = null;
    }
}
