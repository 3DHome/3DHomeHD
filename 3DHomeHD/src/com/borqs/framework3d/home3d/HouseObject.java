package com.borqs.framework3d.home3d;

import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.widget3d.VesselObject;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: b608
 * Date: 13-5-22
 * Time: 下午3:35
 * To change this template use File | Settings | File Templates.
 */
public abstract class HouseObject extends VesselObject {
    private HouseSceneInfo mInfo;
    protected float mPerFaceAngle;

    public abstract int getWallIndex();

    public abstract void addWallChangedListener(WallChangedListener screenIndicator);

    public abstract void removeWallChangedListener(WallChangedListener screenIndicator);


    public interface WallChangedListener {
        public void onWallChanged(int faceIndex, int wallNum);
        public void onWallPositionUpdated(float index, int wallNum);
    }
    public interface WallRadiusChangedListener {
        public void onWallRadiusChanged(int faceIndex);
    }

    public HouseObject(SEScene scene, String name, int index) {
        super(scene, name, index);
        mInfo = scene.mSceneInfo.mHouseSceneInfo;
        mPerFaceAngle = 360f / getCount();
        setWallAngle(0);
    }

    public int getCount() {
        return mInfo.mWallNum;
    }

    public float getWallRadius() {
        return mInfo.mWallRadius;
    }
    public float getWallHeight() {
        return mInfo.mWallHeight;
    }

    public int getWallUnitSizeX() {
        return (int)mInfo.mWallUnitSizeX;
    }

    public int getWallUnitSizeY() {
        return (int)mInfo.mWallUnitSizeY;
    }

    protected void setWallAngle(float angle) {
        mInfo.mWallAngle = angle;
    }

    public float getWallAngle() {
        return mInfo.mWallAngle;
    }

    public float getWallSpanX() {
        return mInfo.mWallSpanX;
    }
    public float getWallSpanY() {
        return mInfo.mWallSpanY;
    }


    public float getFaceAngle() {
        return mPerFaceAngle;
    }


    public float getHouseWidth() {
        return mInfo.getHouseWidth();
    }

    public float getHouseHeight() {
        return mInfo.getHouseHeight();
    }

    public int getCurrentFaceIndex() {
        return (int)(getWallAngle() / mPerFaceAngle);
    }

    abstract public void addWallRadiusChangedListener(WallRadiusChangedListener l);
    abstract public void removeWallRadiusChangedListener(WallRadiusChangedListener l);

    abstract public void toFace(float face, SEAnimFinishListener listener, float step);
    abstract public void toNearestFace(SEAnimFinishListener l, float step);
    abstract public int getWallNearestIndex();
    abstract public void toLeftHalfFace(SEAnimFinishListener listener, float step);
    abstract public void toRightHalfFace(SEAnimFinishListener listener, float step);
    abstract public SEVector.SEVector2f getWallXZBounder();
    public abstract ArrayList<String> getWallpaperKeySet();
    public abstract ArrayList<String> getGroundpaperKeySet();
}
