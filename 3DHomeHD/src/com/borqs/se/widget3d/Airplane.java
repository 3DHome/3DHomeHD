package com.borqs.se.widget3d;

import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEObject;

public class Airplane extends Flyer {
    private SEObject mAircrew;
    private SEObject mPlaneBody;
    private float mBodyAngle = 0;

    public Airplane(SEScene scene, String name, int index) {
        super(scene, name, index);
    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        setBanner(new SEObject(scene, getObjectInfo().mModelInfo.mChildNames[0]));
        setBannerImageKey("assets/base/airplane/ggq_01.jpg");
        mAircrew = new SEObject(scene, getObjectInfo().mModelInfo.mChildNames[1]);
        mPlaneBody = new SEObject(scene, getObjectInfo().mModelInfo.mChildNames[2]);
        setHasInit(true);
    }

    private float caculateBodyAngle(int count) {
        if (count < 151) {
            mBodyAngle = (float) count * (-0.15f);
        } else if ((count > 175 && count < 326)) {
            mBodyAngle = (float) (count - 325) * 0.15f;
        } else if ((count > 350 && count < 501)) {
            mBodyAngle = (float) (count - 350) * 0.15f;
        } else if (count > 525 && count < 676) {
            mBodyAngle = (float) (675 - count) * 0.15f;
        }
        return mBodyAngle;
    }

    public void onAnimationRun(int count, int TotalFrames) {
        if (count <= 700) {
            mPlaneBody.setRotate(new SERotate(caculateBodyAngle(count), 1.0f, 0, 0), true);
        } else {
            count = count - 700;
            mPlaneBody.setRotate(new SERotate(caculateBodyAngle(count), 1.0f, 0, 0), true);
        }
        mAircrew.setRotate(new SERotate((count * 60) % 360, 1.0f, 0, 0), true);
    }
//
//    @Override
//    protected void trimAdOff() {
////        ModelInfo model = getObjectInfo().mModelInfo;
//    }

}
