package com.borqs.framework3d.home3d;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.borqs.se.home3d.ProviderUtils;

import org.xmlpull.v1.XmlPullParser;

/**
 * Created with IntelliJ IDEA.
 * User: yangfeng
 * Date: 13-5-11
 * Time: 下午7:20
 * To change this template use File | Settings | File Templates.
 */
/// Scene info of Launcher dock info, both Scene and DockObject will refer to its instance,
/// and should not be referred by others.
/// 1. parse from both xml parser and database cursor
/// 2. self fill info to database content values.
/// 3. construct default instance.
/// 4. Camera sight info
public class DockSceneInfo {
    protected int mDeskNum;
    protected float mDeskHeight;
    protected float mDeskRadius;

    public static DockSceneInfo DEFAULT_INFO;
    static {
        DEFAULT_INFO = new DockSceneInfo();
        DEFAULT_INFO.mDeskNum = 6;
        DEFAULT_INFO.mDeskHeight = 100;
        DEFAULT_INFO.mDeskRadius = 140;
    }

    public void fillInfoContent(ContentValues values) {
        values.put(ProviderUtils.SceneInfoColumns.DESK_HEIGHT, mDeskHeight);
        values.put(ProviderUtils.SceneInfoColumns.DESK_RADIUS, mDeskRadius);
        values.put(ProviderUtils.SceneInfoColumns.DESK_NUM, mDeskNum);
    }

    public void parseFromXml(XmlPullParser parser) {
        String deskHeight = parser.getAttributeValue(null, ProviderUtils.SceneInfoColumns.DESK_HEIGHT);
        if (!TextUtils.isEmpty(deskHeight)) {
            mDeskHeight = Float.valueOf(deskHeight);
        }
        String deskRadius = parser.getAttributeValue(null, ProviderUtils.SceneInfoColumns.DESK_RADIUS);
        if (!TextUtils.isEmpty(deskRadius)) {
            mDeskRadius = Float.valueOf(deskRadius);
        }
        String deskNum = parser.getAttributeValue(null, ProviderUtils.SceneInfoColumns.DESK_NUM);
        if (!TextUtils.isEmpty(deskNum)) {
            mDeskNum = Integer.valueOf(deskNum);
        }

    }
    public void parseFromCursor(Cursor cursor) {
        mDeskHeight = cursor.getFloat(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.DESK_HEIGHT));
        mDeskRadius = cursor.getFloat(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.DESK_RADIUS));
        mDeskNum = cursor.getInt(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.DESK_NUM));
    }


//    public SEVector.SEVector3f calculateDeskSightLocation(int width, int height) {
//        float h = mDeskHeight;
//        float r = mDeskRadius + 60;
//        SEVector.SEVector3f loc = getDeskCameraLocation(new SEVector.SEVector3f(0, 0, h), r);
//        SEVector.SEVector3f locOri = new SEVector.SEVector3f(0,
//                (float) Math.sin(SECamera.MAX_SIGHT_ANGLE),
//                (float) Math.cos(SECamera.MAX_SIGHT_ANGLE));
//
//        float distance = (float) (width / (2 * Math.tan(SECamera.SIGHT_FOV * Math.PI / 360)));
//        double sightFovH = Math.atan(height / (2 * distance)) + SECamera.MAX_SIGHT_ANGLE;
//
//        SEVector.SEVector3f topOri = new SEVector.SEVector3f(0, (float) Math.cos(sightFovH),
//                (float) -Math.sin(sightFovH));
//        SEVector.SEVector3f top = new SEVector.SEVector3f(0, -r, h);
//
//        float para = ((loc.getY() - top.getY()) / topOri.getY() - (loc.getZ() - top.getZ()) / topOri.getZ())
//                / (locOri.getZ() / topOri.getZ() - locOri.getY() / topOri.getY());
//
//        return new SEVector.SEVector3f(0, loc.getY() + locOri.getY() * para, loc.getZ() + locOri.getZ() * para);
//    }

//    private static SEVector.SEVector3f getDeskCameraLocation(SEVector.SEVector3f centerLocation, float r) {
//        float paras = (float) (r / Math.tan(SECamera.SIGHT_FOV * Math.PI / 360));
//        SEVector.SEVector3f screenOrientation = new SEVector.SEVector3f(0,
//                (float) Math.cos(-SECamera.MAX_SIGHT_ANGLE),
//                (float) Math.sin(-SECamera.MAX_SIGHT_ANGLE));
//        SEVector.SEVector3f loc = centerLocation.subtract(screenOrientation.mul(paras));
//        return loc;
//    }

//    public float getBestCameraFov(SEVector.SEVector3f location, int screenW, int screenH) {
//        float h = mDeskHeight;
//        float r = mDeskRadius + 60;
//        SEVector.SEVector3f top = new SEVector.SEVector3f(0, -r, h);
//        float screenToCamera = (screenH / 2) * (top.getY() - location.getY())
//                / (location.getZ() - top.getZ());
//        double fov = 2 * Math.atan((screenW / 2) / screenToCamera);
//        return (float) (fov * 180 / Math.PI);
//
//    }

    public float getSlotScale(float origin) {
        return origin / mDeskRadius;
    }
}
