package com.borqs.se.widget3d;

import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEScene;
import com.borqs.se.shortcut.LauncherModel;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;

public class AppWall extends VesselObject {

    public int mSpanX;
    public int mSpanY;
    public float mSizeX = 375f;
    public float mSizeY = 476f;

    public AppWall(SEScene scene, String name, int index) {
        super(scene, name, index);
        setCanChangeBind(false);
    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        setVesselLayer(new AppWallLayer(scene, this));
        se_setNeedBlendSort_JNI(new float[] { 0, 1f, 0 });
        mSpanX = getObjectInfo().mObjectSlot.mSpanX;
        mSpanY = getObjectInfo().mObjectSlot.mSpanY;
        if (mSpanX == 4 && mSpanY == 2) {
            mSizeX = 751;
            mSizeY = 471;
        }
        LauncherModel.getInstance().addAppCallBack(this);
        setOnClickListener(null);
        setHasInit(true);
    }

    @Override
    public void onSlotSuccess() {
        setIsFresh(false);
        super.onSlotSuccess();
    }

    @Override
    public SETransParas getSlotTransParas(ObjectInfo objectInfo, NormalObject object) {
        SETransParas transparas = new SETransParas();
        ObjectSlot vesselSlot = getObjectInfo().mObjectSlot;
        ObjectSlot objectSlot = objectInfo.mObjectSlot;
        float offsetX = (objectSlot.mStartX + objectSlot.mSpanX / 2.f) * (mSizeX / (float) mSpanX) - vesselSlot.mSpanX
                * (mSizeX / (float) mSpanX) / 2.f;
        float offsetZ = vesselSlot.mSpanY * (mSizeY / (float) mSpanY) / 2.f
                - (objectSlot.mStartY + objectSlot.mSpanY / 2.f) * (mSizeY / (float) mSpanY);
        transparas.mTranslate.set(offsetX, -40.0f, offsetZ);
        transparas.mScale.set(0.85f, 0.85f, 0.85f);
        return transparas;
    }

    @Override
    public void onRelease() {
        super.onRelease();
        LauncherModel.getInstance().removeAppCallBack(this);
    }
}
