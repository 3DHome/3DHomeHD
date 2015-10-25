package com.borqs.se.widget3d;

import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEScene;

public class Airship extends Flyer {

    public Airship(SEScene scene, String name, int index) {
        super(scene, name, index);
    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        setBanner(new SEObject(scene, getObjectInfo().mModelInfo.mChildNames[0]));
        setBannerImageKey("assets/base/feiting/ggq_01.jpg");
        setHasInit(true);
    }

    @Override
    public void onAnimationRun(int count, int TotalFrames) {

    }

//    @Override
//    protected void trimAdOff() {
//
//    }

}
