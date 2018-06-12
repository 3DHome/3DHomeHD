package com.borqs.framework3d.home3d;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import com.borqs.se.engine.SEVector;
import com.borqs.se.home3d.ProviderUtils;
import org.xmlpull.v1.XmlPullParser;

/**
 * Created with IntelliJ IDEA.
 * User: yangfeng
 * Date: 13-5-19
 * Time: 上午12:39
 * To change this template use File | Settings | File Templates.
 */
public class HouseSceneInfo {
    protected int mWallSpanX;
    protected int mWallSpanY;

    protected int mWallNum;
    protected float mWallHeight;
    protected float mWallRadius;
    protected float mWallAngle;

    protected float mWallUnitSizeX;
    protected float mWallUnitSizeY;

    public static HouseSceneInfo DEFAULT_INFO;
    static {
        DEFAULT_INFO = new HouseSceneInfo();
        DEFAULT_INFO.mWallSpanX = 4;
        DEFAULT_INFO.mWallSpanY = 4;
        DEFAULT_INFO.mWallNum = 12;
        DEFAULT_INFO.mWallHeight = 45f;
        DEFAULT_INFO.mWallRadius = 1000;
        DEFAULT_INFO.mWallAngle = 0;
//        DEFAULT_INFO.mWallUnitSizeX;
//        DEFAULT_INFO.mWallUnitSizeY;
    }

    public void parseFromXml(XmlPullParser parser) {

        String wallNum = parser.getAttributeValue(null, ProviderUtils.SceneInfoColumns.WALL_NUM);
        if (!TextUtils.isEmpty(wallNum)) {
            mWallNum = Integer.parseInt(wallNum);
        }
        String wallSpanX = parser.getAttributeValue(null, ProviderUtils.SceneInfoColumns.WALL_SPANX);
        if (!TextUtils.isEmpty(wallSpanX)) {
            mWallSpanX = Integer.parseInt(wallSpanX);
        }
        String wallSpanY = parser.getAttributeValue(null, ProviderUtils.SceneInfoColumns.WALL_SPANY);
        if (!TextUtils.isEmpty(wallSpanY)) {
            mWallSpanY = Integer.parseInt(wallSpanY);
        }
        String wallSizeX = parser.getAttributeValue(null, ProviderUtils.SceneInfoColumns.WALL_SIZEX);
        if (!TextUtils.isEmpty(wallSizeX)) {
            mWallUnitSizeX = Float.valueOf(wallSizeX);
        }
        String wallSizeY = parser.getAttributeValue(null, ProviderUtils.SceneInfoColumns.WALL_SIZEY);
        if (!TextUtils.isEmpty(wallSizeY)) {
            mWallUnitSizeY = Float.valueOf(wallSizeY);
        }
        String wallHeight = parser.getAttributeValue(null, ProviderUtils.SceneInfoColumns.WALL_HEIGHT);
        if (!TextUtils.isEmpty(wallHeight)) {
            mWallHeight = Float.valueOf(wallHeight);
        }
        String wallRadius = parser.getAttributeValue(null, ProviderUtils.SceneInfoColumns.WALL_RADIUS);
        if (!TextUtils.isEmpty(wallRadius)) {
            mWallRadius = Float.valueOf(wallRadius);
        }
    }

    public void parseFromCursor(Cursor cursor) {
        mWallNum = cursor.getInt(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.WALL_NUM));
        mWallSpanX = cursor.getInt(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.WALL_SPANX));
        mWallSpanY = cursor.getInt(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.WALL_SPANY));
        mWallUnitSizeX = cursor.getFloat(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.WALL_SIZEX));
        mWallUnitSizeY = cursor.getFloat(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.WALL_SIZEY));
        mWallHeight = cursor.getFloat(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.WALL_HEIGHT));
        mWallAngle = cursor.getFloat(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.WALL_ANGLE));
        mWallRadius = cursor.getFloat(cursor.getColumnIndexOrThrow(ProviderUtils.SceneInfoColumns.WALL_RADIUS));
    }

    public void fillInfoContent(ContentValues values) {
        values.put(ProviderUtils.SceneInfoColumns.WALL_NUM, mWallNum);
        values.put(ProviderUtils.SceneInfoColumns.WALL_SPANX, mWallSpanX);
        values.put(ProviderUtils.SceneInfoColumns.WALL_SPANY, mWallSpanY);
        values.put(ProviderUtils.SceneInfoColumns.WALL_SIZEX, mWallUnitSizeX);
        values.put(ProviderUtils.SceneInfoColumns.WALL_SIZEY, mWallUnitSizeY);
        values.put(ProviderUtils.SceneInfoColumns.WALL_HEIGHT, mWallHeight);
        values.put(ProviderUtils.SceneInfoColumns.WALL_ANGLE, mWallAngle);
        values.put(ProviderUtils.SceneInfoColumns.WALL_RADIUS, mWallRadius);
    }

    public float getWallHeightOffset() {
        return mWallHeight + mWallSpanY * mWallUnitSizeY;
    }

    public float getHouseWidth() {
        return mWallSpanX * mWallUnitSizeX;
    }

    public float getWallHeight() {
        return mWallHeight;
    }

    public float getHouseHeight() {
        return mWallSpanY * mWallUnitSizeY;
    }

    public float getHouseRadius() {
        return mWallRadius;
    }

    public float getWallObjectWidth(int spanX) {
        return mWallUnitSizeX * spanX;
    }

    public float getWallObjectHeight(int spanY) {
        return mWallUnitSizeY * spanY;
    }
    public float getWallUnitSizeX() {
        return mWallUnitSizeX;
    }
    public float getWallUnitSizeY() {
        return mWallUnitSizeY;
    }

    public float round() {
        float angle = Math.round(mWallAngle * mWallNum / 360) * 360 / mWallNum;
        return angle;
    }

    public float getTopOffset() {
        return mWallHeight + mWallSpanY * mWallUnitSizeY;
    }

    public SEVector.SEVector2f slotToWall(int slotX, int slotY, float cameraHeight) {
        float x = mWallUnitSizeX * (slotX - mWallSpanX / 2f);
        float y = mWallUnitSizeY * (mWallSpanY - slotY) - cameraHeight + mWallHeight;
        SEVector.SEVector2f wallSlot = new SEVector.SEVector2f(x, y);
        return wallSlot;
    }
}
