package com.borqs.se.widget3d;

import java.util.List;

import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectFactory;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEVector.SERect3D;
import com.borqs.se.home3d.HomeScene;
import com.borqs.se.shortcut.LauncherModel;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;

public class Wall extends VesselObject {

    public Wall(HomeScene scene, String name, int index) {
        super(scene, name, index);
        setPressType(PRESS_TYPE.NONE);
    }

    @Override
    public void initStatus() {
        super.initStatus();
        setVesselLayer(new WallCellLayer(getHomeScene(), this));
        LauncherModel.getInstance().addAppCallBack(this);
        setOnClickListener(null);
        setHasInit(true);
//        test();
    }

    private void test() {
        for (int x = 0; x < getHomeSceneInfo().mCellCountX; x++) {
            for (int y = 0; y < getHomeSceneInfo().mCellCountY; y++) {
                SEObject line = new SEObject(getScene(), "line_" + x + y + System.currentTimeMillis());
                SERect3D rect = new SERect3D(getHomeSceneInfo().mCellWidth, getHomeSceneInfo().mCellHeight);
                SEObjectFactory.createRectLine(line, rect, new float[] { 1, 0, 0 });
                addChild(line, true);
                ObjectSlot slot = new ObjectSlot();
                slot.mStartX = x;
                slot.mStartY = y;
                slot.mSpanX = 1;
                slot.mSpanY = 1;
                line.getUserTransParas().set(getTransParasInVessel(null, slot));
                line.setUserTransParas();
            }
        }
    }

    @Override
    public SETransParas getTransParasInVessel(NormalObject needPlaceObj, ObjectSlot objectSlot) {
        SETransParas transparas = new SETransParas();
        float gridSizeX = getHomeSceneInfo().mCellWidth + getHomeSceneInfo().mWidthGap;
        float gridSizeY = getHomeSceneInfo().mCellHeight + getHomeSceneInfo().mHeightGap;
        float offsetX = (objectSlot.mStartX + objectSlot.mSpanX / 2.f) * gridSizeX - getHomeSceneInfo().mCellCountX
                * gridSizeX / 2.f + (getHomeSceneInfo().mWallPaddingLeft - getHomeSceneInfo().mWallPaddingRight) / 2;
        float offsetZ = getHomeSceneInfo().mCellCountY * gridSizeY / 2.f
                - (objectSlot.mStartY + objectSlot.mSpanY / 2.f) * gridSizeY
                + (getHomeSceneInfo().mWallPaddingBottom - getHomeSceneInfo().mWallPaddingTop) / 2;
        transparas.mTranslate.set(offsetX, 0, offsetZ);
        return transparas;
    }

    @Override
    public void onRelease() {
        super.onRelease();
        LauncherModel.getInstance().removeAppCallBack(this);
    }
    
    
    @Override
    public void onActivityRestart() {
        super.onActivityRestart();
        forceReloadWidget();
    }
    
    private void forceReloadWidget() {
        new SECommand(getScene()) {
            public void run() {
                List<NormalObject> matchApps = findAPP(null, "Widget");
                for (NormalObject widget : matchApps) {
                    WidgetObject myWidget = (WidgetObject) widget;
                    myWidget.bind();
                }
            }
        }.execute();
    }

    /// create Wall object or ShelfObject that will auto add a
    /// decorator underneath of all 1x1 child object.
    /// todo: configure these via settings, pre-load configuration, etc.
    private static final boolean ENABLED_SHELF_DECORATOR = true;
    public static NormalObject instance(HomeScene scene, String name, int index) {
        if (ENABLED_SHELF_DECORATOR) {
            return new ShelfWall(scene, name, index);
        } else {
            return new Wall(scene, name, index);
        }
    }
}
