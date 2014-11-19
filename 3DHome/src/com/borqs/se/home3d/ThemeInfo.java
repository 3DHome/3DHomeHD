package com.borqs.se.home3d;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.borqs.se.engine.SEComponentAttribute;
import com.borqs.se.engine.SECameraData;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.home3d.ProviderUtils.ObjectInfoColumns;
import com.borqs.se.home3d.ProviderUtils.Tables;
import com.borqs.se.home3d.ProviderUtils.ThemeColumns;
import com.borqs.se.widget3d.ObjectInfo;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Xml;

public class ThemeInfo {
    public static final String DEFAULT_SCENE_NAME = "home8";
    public int mID;
    public String mThemeName;
    public String mSceneName = DEFAULT_SCENE_NAME;
    public String mFilePath;
    public boolean mIsDownloaded = false;
    public boolean mIsApplyed = false;
    public String mConfig;
    public String mProductID;

    // house info
    public String mHouseName;
    public float mSkyRadius = 488;
    public int mWallIndex = 0;
    public float mWallWidth;
    public float mWallHeight;
    public float mWallPaddingLeft = 0;
    public float mWallPaddingRight = 0;
    public float mWallPaddingTop = 0;
    public float mWallPaddingBottom = 45;
    public int mWallNum = 8;
    public float mWallRadius = 1000;
    public int mCellCountX = 4;
    public int mCellCountY = 4;
    public float mCellWidth = 191;
    public float mCellHeight = 231;
    public float mWidthGap = 6;
    public float mHeightGap = 15;

    // camera info
    public SECameraData mSECameraData;
    public float mBestCameraFov;
    public SEVector3f mBestCameraLocation;
    public float mNearestCameraFov = 26.274373f;
    public SEVector3f mNearestCameraLocation = new SEVector3f(0, -753, 430);
    public float mFarthestCameraFov = 39.73937f;
    public SEVector3f mFarthestCameraLocation = new SEVector3f(0, -1000, 430);
    // 绑定在该主题下的模型，默认绑定了一个房间
    private List<ModelInfo> mBoundingModel;

    private DefaultObjectsInfo mDefaultObjectsInfo;

    // // object in scene
    private static class DefaultObjectsInfo {
        public String mSceneName;
        public ObjectInfo mHouse;
        public ObjectInfo mDesk;
        public ObjectInfo mSky;
        public List<ObjectInfo> mObjectsOnGround;
        public List<ObjectInfo> mObjectsOnDesk;
        public List<ObjectInfo> mObjectsOnWall;
    }

    public static ThemeInfo CreateFromXml(Context context, XmlPullParser parser, String id, String filePath)
            throws XmlPullParserException, IOException {
        ThemeInfo info = new ThemeInfo();
        info.mProductID = id;
        info.mThemeName = parser.getAttributeValue(null, "themeName");
        if (filePath == null) {
            info.mFilePath = parser.getAttributeValue(null, "filePath");
        } else {
            info.mFilePath = filePath;
        }
        info.mConfig = parser.getAttributeValue(null, "config");
        String isApplyed = parser.getAttributeValue(null, "isApplyed");
        if (!TextUtils.isEmpty(isApplyed)) {
            info.mIsApplyed = Integer.parseInt(isApplyed) == 1 ? true : false;
        }
        XmlUtils.nextElement(parser);
        info.mHouseName = "house_" + info.mThemeName + "_theme";
        while (true) {
            if ("houseConfig".equals(parser.getName())) {                
                String skyRadius = parser.getAttributeValue(null, ThemeColumns.SKY_RADIUS);
                if (!TextUtils.isEmpty(skyRadius)) {
                    info.mSkyRadius = Float.valueOf(skyRadius);
                }

                // house related info
                String wallIndex = parser.getAttributeValue(null, ThemeColumns.WALL_INDEX);
                if (!TextUtils.isEmpty(wallIndex)) {
                    info.mWallIndex = Integer.valueOf(wallIndex);
                }
                String wallPaddingTop = parser.getAttributeValue(null, ThemeColumns.WALL_PADDINGTOP);
                if (!TextUtils.isEmpty(wallPaddingTop)) {
                    info.mWallPaddingTop = Float.valueOf(wallPaddingTop);
                }
                String wallPaddingBottom = parser.getAttributeValue(null, ThemeColumns.WALL_PADDINGBOTTOM);
                if (!TextUtils.isEmpty(wallPaddingBottom)) {
                    info.mWallPaddingBottom = Float.valueOf(wallPaddingBottom);
                }
                String wallPaddingLeft = parser.getAttributeValue(null, ThemeColumns.WALL_PADDINGLEFT);
                if (!TextUtils.isEmpty(wallPaddingLeft)) {
                    info.mWallPaddingLeft = Float.valueOf(wallPaddingLeft);
                }
                String wallPaddingRight = parser.getAttributeValue(null, ThemeColumns.WALL_PADDINGRIGHT);
                if (!TextUtils.isEmpty(wallPaddingRight)) {
                    info.mWallPaddingRight = Float.valueOf(wallPaddingRight);
                }
                String wallNum = parser.getAttributeValue(null, ThemeColumns.WALL_NUM);
                if (!TextUtils.isEmpty(wallNum)) {
                    info.mWallNum = Integer.valueOf(wallNum);
                }
                String wallRadius = parser.getAttributeValue(null, ThemeColumns.WALL_RADIUS);
                if (!TextUtils.isEmpty(wallRadius)) {
                    info.mWallRadius = Float.valueOf(wallRadius);
                }

                // wall related info
                String cellCountX = parser.getAttributeValue(null, ThemeColumns.WALL_SPANX);
                if (!TextUtils.isEmpty(cellCountX)) {
                    info.mCellCountX = Integer.valueOf(cellCountX);
                }
                String cellCountY = parser.getAttributeValue(null, ThemeColumns.WALL_SPANY);
                if (!TextUtils.isEmpty(cellCountY)) {
                    info.mCellCountY = Integer.valueOf(cellCountY);
                }
                String cellWidth = parser.getAttributeValue(null, ThemeColumns.CELL_WIDTH);
                if (!TextUtils.isEmpty(cellWidth)) {
                    info.mCellWidth = Float.valueOf(cellWidth);
                }
                String cellHeight = parser.getAttributeValue(null, ThemeColumns.CELL_HEIGHT);
                if (!TextUtils.isEmpty(cellHeight)) {
                    info.mCellHeight = Float.valueOf(cellHeight);
                }
                String gapWidth = parser.getAttributeValue(null, ThemeColumns.CELL_GAP_WIDTH);
                if (!TextUtils.isEmpty(gapWidth)) {
                    info.mWidthGap = Float.valueOf(gapWidth);
                }
                String gapHeight = parser.getAttributeValue(null, ThemeColumns.CELL_GAP_HEIGHT);
                if (!TextUtils.isEmpty(gapHeight)) {
                    info.mHeightGap = Float.valueOf(gapHeight);
                }
                XmlUtils.nextElement(parser);
            } else if ("cameraConfig".equals(parser.getName()) || "bestCamera".equals(parser.getName())) {
                info.mSECameraData = HomeUtils.loadCameraDataFromXml(parser);
            } else if ("nearestCamera".equals(parser.getName())) {
                SECameraData nearestCamera = HomeUtils.loadCameraDataFromXml(parser);
                info.mNearestCameraFov = nearestCamera.mFov;
                info.mNearestCameraLocation = nearestCamera.mLocation;
            } else if ("farthestCamera".equals(parser.getName())) {
                SECameraData farthestCamera = HomeUtils.loadCameraDataFromXml(parser);
                info.mFarthestCameraFov = farthestCamera.mFov;
                info.mFarthestCameraLocation = farthestCamera.mLocation;
            } else {
                break;
            }
        }
        if (info.mSECameraData == null) {
            info.mSECameraData = new SECameraData();
            info.mSECameraData.mAxisZ = new SEVector3f(0, -1, 0);
            info.mSECameraData.mLocation = new SEVector3f(0,-803,460);
            info.mSECameraData.mUp = new SEVector3f(0, 0, 1);
            info.mSECameraData.mFov = 29;
            info.mSECameraData.mNear = 10;
            info.mSECameraData.mFar = 5000;
        }

        info.mWallWidth = info.mWallPaddingLeft + info.mWallPaddingRight + info.mCellWidth * info.mCellCountX
                + info.mWidthGap * (info.mCellCountX - 1);
        info.mWallHeight = info.mWallPaddingTop + info.mWallPaddingBottom + info.mCellHeight * info.mCellCountY
                + info.mHeightGap * (info.mCellCountY - 1);
        // 开始解析场景包中自带的物体
        boolean isAsset = info.mFilePath.startsWith("assets") ? true : false;
        info.mBoundingModel = loadAllBoundingModelInfo(context, isAsset, info.mThemeName, info.mFilePath);
        // 开始解析房间中摆放了什么物体，假如default_objects_in_scene.xml文件没有那么使用默认配置，有的话切换场景时物体的摆放会更改成自己的配置
        if (!isAsset) {
            try {
                FileInputStream fis = new FileInputStream(info.mFilePath + "/default_objects_in_scene.xml");
                parser = Xml.newPullParser();
                parser.setInput(fis, "utf-8");
                XmlUtils.beginDocument(parser, "sceneConfig");
                info.mDefaultObjectsInfo = loadObjectsInScene(parser);
                fis.close();
            } catch (Exception e) {

            }
        } else {
            try {
                InputStream is = context.getAssets().open(info.mFilePath.substring(7) + "/default_objects_in_scene.xml");
                parser = Xml.newPullParser();
                parser.setInput(is, "utf-8");
                XmlUtils.beginDocument(parser, "sceneConfig");
                info.mDefaultObjectsInfo = loadObjectsInScene(parser);
                is.close();
            } catch (Exception e) {

            }
        }
        if (info.mDefaultObjectsInfo != null) {
            if (TextUtils.isEmpty(info.mDefaultObjectsInfo.mSceneName)) {
                info.mSceneName = "scene_of_" + info.mThemeName;
                info.mDefaultObjectsInfo.mSceneName = info.mSceneName;
            } else {
                info.mSceneName = info.mDefaultObjectsInfo.mSceneName;
            }
        }
        return info;
    }

    public static ThemeInfo CreateFromDB(Cursor cursor) {
        ThemeInfo info = new ThemeInfo();
        info.mID = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns._ID));
        info.mThemeName = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.NAME));
        info.mFilePath = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.FILE_PATH));
        info.mConfig = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.CONFIG));
        info.mIsApplyed = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns.IS_APPLY)) == 1 ? true : false;
        info.mProductID = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.PRODUCT_ID));
        info.mSceneName = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.SCENE_NAME));
        info.mIsDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns.IS_DOWNLOADED)) == 1 ? true
                : false;

        info.mHouseName = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.HOUSE_NAME));
        info.mSkyRadius = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.SKY_RADIUS));
        info.mWallPaddingTop = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.WALL_PADDINGTOP));
        info.mWallPaddingBottom = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.WALL_PADDINGBOTTOM));
        info.mWallPaddingLeft = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.WALL_PADDINGLEFT));
        info.mWallPaddingRight = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.WALL_PADDINGRIGHT));
        info.mWallNum = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns.WALL_NUM));
        info.mWallRadius = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.WALL_RADIUS));
        info.mCellCountX = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns.WALL_SPANX));
        info.mCellCountY = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns.WALL_SPANY));
        info.mCellWidth = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.CELL_WIDTH));
        info.mCellHeight = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.CELL_HEIGHT));
        info.mWidthGap = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.CELL_GAP_WIDTH));
        info.mHeightGap = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.CELL_GAP_HEIGHT));
        info.mWallIndex = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns.WALL_INDEX));

        info.mSECameraData = new SECameraData();
        info.mSECameraData.mFov = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.FOV));
        info.mSECameraData.mNear = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.NEAR));
        info.mSECameraData.mFar = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.FAR));
        String cameraLoc = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.LOCATION));
        info.mSECameraData.mLocation = new SEVector3f(ProviderUtils.getFloatArray(cameraLoc, 3));
        String cameraZ = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.ZAXIS));
        info.mSECameraData.mAxisZ = new SEVector3f(ProviderUtils.getFloatArray(cameraZ, 3));
        String cameraUp = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.UP));
        info.mSECameraData.mUp = new SEVector3f(ProviderUtils.getFloatArray(cameraUp, 3));
        info.mWallWidth = info.mWallPaddingLeft + info.mWallPaddingRight + info.mCellWidth * info.mCellCountX
                + info.mWidthGap * (info.mCellCountX - 1);
        info.mWallHeight = info.mWallPaddingTop + info.mWallPaddingBottom + info.mCellHeight * info.mCellCountY
                + info.mHeightGap * (info.mCellCountY - 1);

        info.mBestCameraFov = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.BEST_FOV));
        String bestCameraLoc = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.BEST_LOCATION));
        info.mBestCameraLocation = new SEVector3f(ProviderUtils.getFloatArray(bestCameraLoc, 3));

        info.mNearestCameraFov = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.NEAREST_FOV));
        String nearestCameraLoc = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.NEAREST_LOCATION));
        info.mNearestCameraLocation = new SEVector3f(ProviderUtils.getFloatArray(nearestCameraLoc, 3));

        info.mFarthestCameraFov = cursor.getFloat(cursor.getColumnIndexOrThrow(ThemeColumns.FARTHEST_FOV));
        String farthestCameraLoc = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.FARTHEST_LOCATION));
        info.mFarthestCameraLocation = new SEVector3f(ProviderUtils.getFloatArray(farthestCameraLoc, 3));

        return info;
    }

    public void saveToDB(Context context) {
        ContentValues values = new ContentValues();
        values.put(ThemeColumns.NAME, mThemeName);
        values.put(ThemeColumns.FILE_PATH, mFilePath);
        values.put(ThemeColumns.IS_DOWNLOADED, mIsDownloaded);
        values.put(ThemeColumns.IS_APPLY, mIsApplyed);
        values.put(ThemeColumns.CONFIG, mConfig);
        values.put(ThemeColumns.PRODUCT_ID, mProductID);
        values.put(ThemeColumns.SCENE_NAME, mSceneName);
        Uri insertUri = context.getContentResolver().insert(ThemeColumns.CONTENT_URI, values);
        mID = (int) ContentUris.parseId(insertUri);

        values = new ContentValues();
        values.put(ThemeColumns._ID, mID);
        values.put(ThemeColumns.HOUSE_NAME, mHouseName);
        values.put(ThemeColumns.SKY_RADIUS, mSkyRadius);
        values.put(ThemeColumns.WALL_PADDINGTOP, mWallPaddingTop);
        values.put(ThemeColumns.WALL_PADDINGBOTTOM, mWallPaddingBottom);
        values.put(ThemeColumns.WALL_PADDINGLEFT, mWallPaddingLeft);
        values.put(ThemeColumns.WALL_PADDINGRIGHT, mWallPaddingRight);
        values.put(ThemeColumns.WALL_NUM, mWallNum);
        values.put(ThemeColumns.WALL_RADIUS, mWallRadius);
        values.put(ThemeColumns.WALL_SPANX, mCellCountX);
        values.put(ThemeColumns.WALL_SPANY, mCellCountY);
        values.put(ThemeColumns.CELL_WIDTH, mCellWidth);
        values.put(ThemeColumns.CELL_HEIGHT, mCellHeight);
        values.put(ThemeColumns.CELL_GAP_WIDTH, mWidthGap);
        values.put(ThemeColumns.CELL_GAP_HEIGHT, mHeightGap);
        values.put(ThemeColumns.WALL_INDEX, mWallIndex);
        context.getContentResolver().insert(ThemeColumns.HOUSE_INFO_URI, values);

        values = new ContentValues();
        values.put(ThemeColumns._ID, mID);
        values.put(ThemeColumns.FOV, mSECameraData.mFov);
        values.put(ThemeColumns.BEST_FOV, mSECameraData.mFov);
        values.put(ThemeColumns.NEAREST_FOV, mNearestCameraFov);
        values.put(ThemeColumns.FARTHEST_FOV, mFarthestCameraFov);
        values.put(ThemeColumns.NEAR, mSECameraData.mNear);
        values.put(ThemeColumns.FAR, mSECameraData.mFar);
        values.put(ThemeColumns.LOCATION, mSECameraData.mLocation.toString());
        values.put(ThemeColumns.BEST_LOCATION, mSECameraData.mLocation.toString());
        values.put(ThemeColumns.NEAREST_LOCATION, mNearestCameraLocation.toString());
        values.put(ThemeColumns.FARTHEST_LOCATION, mFarthestCameraLocation.toString());
        values.put(ThemeColumns.ZAXIS, mSECameraData.mAxisZ.toString());
        values.put(ThemeColumns.UP, mSECameraData.mUp.toString());
        context.getContentResolver().insert(ThemeColumns.CAMERA_INFO_URI, values);

        if (mBoundingModel != null) {
            for (ModelInfo modelInfo : mBoundingModel) {
                modelInfo.saveToDB();
            }
        }
        if (mDefaultObjectsInfo != null) {
            String where = ObjectInfoColumns.SCENE_NAME + "='" + mSceneName + "'";
            String[] columns = { ObjectInfoColumns.OBJECT_ID };
            Cursor cursor = context.getContentResolver().query(ObjectInfoColumns.CONTENT_URI, columns, where, null,
                    null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.close();
                    return;
                }
                cursor.close();
            }
            mDefaultObjectsInfo.mHouse = new ObjectInfo();
            mDefaultObjectsInfo.mHouse.mName = mHouseName;
            mDefaultObjectsInfo.mHouse.mType = "House";
            saveObjectsOfSceneToDB(mDefaultObjectsInfo, mWallNum);
        } else {
            String where = ObjectInfoColumns.SCENE_NAME + "='" + ThemeInfo.DEFAULT_SCENE_NAME + "'";
            String[] columns = { ObjectInfoColumns.OBJECT_ID };
            Cursor cursor = context.getContentResolver().query(ObjectInfoColumns.CONTENT_URI, columns, where, null,
                    null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.close();
                    return;
                }
                cursor.close();
            }

            try {
                InputStream is = context.getAssets().open("base/default_objects_in_scene.xml");
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(is, "utf-8");
                XmlUtils.beginDocument(parser, "sceneConfig");
                DefaultObjectsInfo defaultObjectsInfo = loadObjectsInScene(parser);
                defaultObjectsInfo.mHouse = new ObjectInfo();
                defaultObjectsInfo.mHouse.mName = mHouseName;
                defaultObjectsInfo.mHouse.mType = "House";
                saveObjectsOfSceneToDB(defaultObjectsInfo, 8);
                is.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void saveToDB(Context context, SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(ThemeColumns.NAME, mThemeName);
        values.put(ThemeColumns.FILE_PATH, mFilePath);
        values.put(ThemeColumns.IS_DOWNLOADED, mIsDownloaded);
        values.put(ThemeColumns.IS_APPLY, mIsApplyed);
        values.put(ThemeColumns.CONFIG, mConfig);
        values.put(ThemeColumns.PRODUCT_ID, mProductID);
        values.put(ThemeColumns.SCENE_NAME, mSceneName);
        mID = (int) db.insert(Tables.THEME, null, values);

        values = new ContentValues();
        values.put(ThemeColumns._ID, mID);
        values.put(ThemeColumns.HOUSE_NAME, mHouseName);
        values.put(ThemeColumns.SKY_RADIUS, mSkyRadius);
        values.put(ThemeColumns.WALL_PADDINGTOP, mWallPaddingTop);
        values.put(ThemeColumns.WALL_PADDINGBOTTOM, mWallPaddingBottom);
        values.put(ThemeColumns.WALL_PADDINGLEFT, mWallPaddingLeft);
        values.put(ThemeColumns.WALL_PADDINGRIGHT, mWallPaddingRight);
        values.put(ThemeColumns.WALL_NUM, mWallNum);
        values.put(ThemeColumns.WALL_RADIUS, mWallRadius);
        values.put(ThemeColumns.WALL_SPANX, mCellCountX);
        values.put(ThemeColumns.WALL_SPANY, mCellCountY);
        values.put(ThemeColumns.CELL_WIDTH, mCellWidth);
        values.put(ThemeColumns.CELL_HEIGHT, mCellHeight);
        values.put(ThemeColumns.CELL_GAP_WIDTH, mWidthGap);
        values.put(ThemeColumns.CELL_GAP_HEIGHT, mHeightGap);
        values.put(ThemeColumns.WALL_INDEX, mWallIndex);
        db.insert(Tables.HOUSE_INFO, null, values);

        values = new ContentValues();
        values.put(ThemeColumns._ID, mID);
        values.put(ThemeColumns.FOV, mSECameraData.mFov);
        values.put(ThemeColumns.BEST_FOV, mSECameraData.mFov);
        values.put(ThemeColumns.NEAREST_FOV, mNearestCameraFov);
        values.put(ThemeColumns.FARTHEST_FOV, mFarthestCameraFov);
        values.put(ThemeColumns.NEAR, mSECameraData.mNear);
        values.put(ThemeColumns.FAR, mSECameraData.mFar);
        values.put(ThemeColumns.LOCATION, mSECameraData.mLocation.toString());
        values.put(ThemeColumns.BEST_LOCATION, mSECameraData.mLocation.toString());
        values.put(ThemeColumns.NEAREST_LOCATION, mNearestCameraLocation.toString());
        values.put(ThemeColumns.FARTHEST_LOCATION, mFarthestCameraLocation.toString());
        values.put(ThemeColumns.ZAXIS, mSECameraData.mAxisZ.toString());
        values.put(ThemeColumns.UP, mSECameraData.mUp.toString());
        db.insert(Tables.CAMERA_INFO, null, values);
        if (mBoundingModel != null) {
            for (ModelInfo modelInfo : mBoundingModel) {
                modelInfo.saveToDB(db);
            }
        }
        if (mDefaultObjectsInfo != null) {
            String where = ObjectInfoColumns.SCENE_NAME + "='" + mSceneName + "'";
            String[] columns = { ObjectInfoColumns.OBJECT_ID };
            Cursor cursor = db.query(Tables.OBJECTS_INFO, columns, where, null, null, null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.close();
                    return;
                }
                cursor.close();
            }
            mDefaultObjectsInfo.mHouse = new ObjectInfo();
            mDefaultObjectsInfo.mHouse.mName = mHouseName;
            mDefaultObjectsInfo.mHouse.mType = "House";
            saveObjectsOfSceneToDB(db, mDefaultObjectsInfo, mWallNum);
        } else {
            String where = ObjectInfoColumns.SCENE_NAME + "='" + ThemeInfo.DEFAULT_SCENE_NAME + "'";
            String[] columns = { ObjectInfoColumns.OBJECT_ID };
            Cursor cursor = db.query(Tables.OBJECTS_INFO, columns, where, null, null, null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.close();
                    return;
                }
                cursor.close();
            }

            try {
                InputStream is = context.getAssets().open("base/default_objects_in_scene.xml");
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(is, "utf-8");
                XmlUtils.beginDocument(parser, "sceneConfig");
                DefaultObjectsInfo defaultObjectsInfo = loadObjectsInScene(parser);
                defaultObjectsInfo.mHouse = new ObjectInfo();
                defaultObjectsInfo.mHouse.mName = mHouseName;
                defaultObjectsInfo.mHouse.mType = "House";
                saveObjectsOfSceneToDB(db, defaultObjectsInfo, 8);
                is.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void updateWallIndex(final Context context) {
        UpdateDBThread.getInstance().process(new Runnable() {
            public void run() {
                String where = ThemeColumns._ID + "=" + mID;
                ContentValues values = new ContentValues();
                values.put(ThemeColumns.WALL_INDEX, mWallIndex);
                context.getContentResolver().update(ThemeColumns.HOUSE_INFO_URI, values, where, null);
            }
        });
    }

    public void updateCameraDataToDB(final Context context, final SEVector3f location, final float fov) {
        UpdateDBThread.getInstance().process(new Runnable() {
            public void run() {
                String where = ThemeColumns._ID + "=" + mID;
                ContentValues values = new ContentValues();
                values.put(ThemeColumns.FOV, fov);
                values.put(ThemeColumns.LOCATION, location.toString());
                context.getContentResolver().update(ThemeColumns.CAMERA_INFO_URI, values, where, null);
            }
        });
    }

    private static List<ModelInfo> loadAllBoundingModelInfo(Context context, boolean isAsset, String themeName,
            String path) {
        // 加载主题包下的房间模型
        List<ModelInfo> allBoundingModelInfo = new ArrayList<ModelInfo>();
        try {
            InputStream is;
            if (isAsset) {
                is = context.getAssets().open(path.substring(7) + "/models_config.xml");
            } else {
                is = new FileInputStream(path + "/models_config.xml");
            }
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            XmlUtils.beginDocument(parser, "config");
            ModelInfo config = ModelInfo.CreateFromXml(parser,null, path);
            config.mName = "house_" + themeName + "_theme";
            // 解决在老的主题中灯光在ApplicationMenu之上显示的问题----begin
            SEComponentAttribute componentAttr = config.findComponentAttrByRegularName("light");
            if (componentAttr != null && componentAttr.mComponentName.equals("deng@group_house8")) {
                componentAttr.mStatusValue = 1064965;
            }
            // 解决在老的主题中灯光在ApplicationMenu之上显示的问题----end
            config.mIsDownloaded = false;
            config.mName = "house_" + themeName + "_theme";
            is.close();
            allBoundingModelInfo.add(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (!isAsset) {
                File file = new File(path);
                if (file.exists()) {
                    File[] files = file.listFiles();
                    for (File item : files) {
                        if (item.isDirectory()) {
                            InputStream is = new FileInputStream(item.getPath() + "/models_config.xml");
                            XmlPullParser parser = Xml.newPullParser();
                            parser.setInput(is, "utf-8");
                            XmlUtils.beginDocument(parser, "config");
                            ModelInfo config = ModelInfo.CreateFromXml(parser, null, item.getPath());
                            config.mIsDownloaded = false;
                            is.close();
                            allBoundingModelInfo.add(config);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allBoundingModelInfo;
    }

    private static DefaultObjectsInfo loadObjectsInScene(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        DefaultObjectsInfo defaultObjectInfo = new DefaultObjectsInfo();
        defaultObjectInfo.mHouse = null;
        defaultObjectInfo.mDesk = null;
        defaultObjectInfo.mSky = null;
        defaultObjectInfo.mSceneName = parser.getAttributeValue(null, "sceneName");
        XmlUtils.nextElement(parser);
        while (true) {
            if ("skyInScene".equals(parser.getName())) {
                XmlUtils.nextElement(parser);
                while (true) {
                    if ("Object".equals(parser.getName())) {
                        defaultObjectInfo.mSky = ObjectInfo.CreateFromXml(parser);
                        XmlUtils.nextElement(parser);
                    } else {
                        break;
                    }
                }
            } else if ("deskInScene".equals(parser.getName())) {
                XmlUtils.nextElement(parser);
                while (true) {
                    if ("Object".equals(parser.getName())) {
                        defaultObjectInfo.mDesk = ObjectInfo.CreateFromXml(parser);
                        XmlUtils.nextElement(parser);
                    } else {
                        break;
                    }
                }
            } else if ("objectsOnwall".equals(parser.getName())) {
                XmlUtils.nextElement(parser);
                while (true) {
                    if ("Object".equals(parser.getName())) {
                        if (defaultObjectInfo.mObjectsOnWall == null) {
                            defaultObjectInfo.mObjectsOnWall = new ArrayList<ObjectInfo>();
                        }
                        defaultObjectInfo.mObjectsOnWall.add(ObjectInfo.CreateFromXml(parser));
                        XmlUtils.nextElement(parser);
                    } else {
                        break;
                    }
                }
            } else if ("objectsOnGround".equals(parser.getName())) {
                XmlUtils.nextElement(parser);
                while (true) {
                    if ("Object".equals(parser.getName())) {
                        if (defaultObjectInfo.mObjectsOnGround == null) {
                            defaultObjectInfo.mObjectsOnGround = new ArrayList<ObjectInfo>();
                        }
                        defaultObjectInfo.mObjectsOnGround.add(ObjectInfo.CreateFromXml(parser));
                        XmlUtils.nextElement(parser);
                    } else {
                        break;
                    }
                }
            } else if ("objectsOnDesktop".equals(parser.getName())) {
                XmlUtils.nextElement(parser);
                while (true) {
                    if ("Object".equals(parser.getName())) {
                        if (defaultObjectInfo.mObjectsOnDesk == null) {
                            defaultObjectInfo.mObjectsOnDesk = new ArrayList<ObjectInfo>();
                        }
                        defaultObjectInfo.mObjectsOnDesk.add(ObjectInfo.CreateFromXml(parser));
                        XmlUtils.nextElement(parser);
                    } else {
                        break;
                    }
                }
            } else {
                break;
            }
        }
        return defaultObjectInfo;
    }

    public static void saveObjectsOfSceneToDB(DefaultObjectsInfo defaultObjectInfo, int wallNum) {
        ObjectInfo root = new ObjectInfo();
        root.mName = "home_root";
        root.mType = "RootNode";
        root.mSceneName = defaultObjectInfo.mSceneName;
        root.saveToDB();

        defaultObjectInfo.mDesk.mVesselIndex = root.mIndex;
        defaultObjectInfo.mDesk.mVesselName = root.mName;
        defaultObjectInfo.mDesk.mSceneName = defaultObjectInfo.mSceneName;
        // can be cloned if index > 0
        defaultObjectInfo.mDesk.mIndex = 1;
        defaultObjectInfo.mDesk.saveToDB();
        if (defaultObjectInfo.mObjectsOnDesk != null) {
            for (ObjectInfo objectOnDesk : defaultObjectInfo.mObjectsOnDesk) {
                objectOnDesk.mVesselIndex = defaultObjectInfo.mDesk.mIndex;
                objectOnDesk.mVesselName = defaultObjectInfo.mDesk.mName;
                objectOnDesk.mSceneName = defaultObjectInfo.mSceneName;
                if (objectOnDesk.mIndex == 0) {
                    objectOnDesk.mIndex = 1;
                }
                objectOnDesk.saveToDB();
            }
        }

        defaultObjectInfo.mHouse.mVesselIndex = root.mIndex;
        defaultObjectInfo.mHouse.mVesselName = root.mName;
        defaultObjectInfo.mHouse.mSceneName = defaultObjectInfo.mSceneName;
        defaultObjectInfo.mHouse.saveToDB();

        for (int index = 0; index < wallNum; index++) {
            ObjectInfo wall = new ObjectInfo();
            wall.mIndex = index;
            wall.mName = "wall";
            wall.mType = "Ground";
            wall.mObjectSlot.mSlotIndex = index;
            wall.mSceneName = defaultObjectInfo.mSceneName;
            wall.mType = "Wall";
            wall.mVesselIndex = defaultObjectInfo.mHouse.mIndex;
            wall.mVesselName = defaultObjectInfo.mHouse.mName;
            wall.saveToDB();
            if (defaultObjectInfo.mObjectsOnWall != null) {
                for (ObjectInfo objectOnWall : defaultObjectInfo.mObjectsOnWall) {
                    if (objectOnWall.getSlotIndex() == index) {
                        objectOnWall.mVesselIndex = wall.mIndex;
                        objectOnWall.mVesselName = wall.mName;
                        objectOnWall.mSceneName = defaultObjectInfo.mSceneName;
                        if (objectOnWall.mIndex == 0) {
                            objectOnWall.mIndex = 1;
                        }
                        objectOnWall.saveToDB();
                    }
                }
            }

        }

        ObjectInfo ground = new ObjectInfo();
        ground.mName = "ground";
        ground.mType = "Ground";
        ground.mObjectSlot.mSlotIndex = -1;
        ground.mSceneName = defaultObjectInfo.mSceneName;
        ground.mVesselIndex = defaultObjectInfo.mHouse.mIndex;
        ground.mVesselName = defaultObjectInfo.mHouse.mName;
        ground.saveToDB();
        if (defaultObjectInfo.mObjectsOnGround != null) {
            for (ObjectInfo objectOnGround : defaultObjectInfo.mObjectsOnGround) {
                objectOnGround.mVesselIndex = ground.mIndex;
                objectOnGround.mVesselName = ground.mName;
                objectOnGround.mSceneName = defaultObjectInfo.mSceneName;
                if (objectOnGround.mIndex == 0) {
                    objectOnGround.mIndex = 1;
                }
                objectOnGround.saveToDB();
            }
        }
        if (defaultObjectInfo.mSky != null) {
            defaultObjectInfo.mSky.mVesselIndex = root.mIndex;
            defaultObjectInfo.mSky.mVesselName = root.mName;
            defaultObjectInfo.mSky.mSceneName = defaultObjectInfo.mSceneName;
            defaultObjectInfo.mSky.saveToDB();
        }
    }

    public static void saveObjectsOfSceneToDB(SQLiteDatabase db, DefaultObjectsInfo defaultObjectInfo, int wallNum) {
        ObjectInfo root = new ObjectInfo();
        root.mName = "home_root";
        root.mType = "RootNode";
        root.mSceneName = defaultObjectInfo.mSceneName;
        root.saveToDB(db);

        defaultObjectInfo.mDesk.mVesselIndex = root.mIndex;
        defaultObjectInfo.mDesk.mVesselName = root.mName;
        defaultObjectInfo.mDesk.mSceneName = defaultObjectInfo.mSceneName;
        // can be cloned if index > 0
        defaultObjectInfo.mDesk.mIndex = 1;
        defaultObjectInfo.mDesk.saveToDB(db);
        if (defaultObjectInfo.mObjectsOnDesk != null) {
            for (ObjectInfo objectOnDesk : defaultObjectInfo.mObjectsOnDesk) {
                objectOnDesk.mVesselIndex = defaultObjectInfo.mDesk.mIndex;
                objectOnDesk.mVesselName = defaultObjectInfo.mDesk.mName;
                objectOnDesk.mSceneName = defaultObjectInfo.mSceneName;
                if (objectOnDesk.mIndex == 0) {
                    objectOnDesk.mIndex = 1;
                }
                objectOnDesk.saveToDB(db);
            }
        }

        defaultObjectInfo.mHouse.mVesselIndex = root.mIndex;
        defaultObjectInfo.mHouse.mVesselName = root.mName;
        defaultObjectInfo.mHouse.mSceneName = defaultObjectInfo.mSceneName;
        defaultObjectInfo.mHouse.saveToDB(db);

        for (int index = 0; index < wallNum; index++) {
            ObjectInfo wall = new ObjectInfo();
            wall.mIndex = index;
            wall.mName = "wall";
            wall.mType = "Ground";
            wall.mObjectSlot.mSlotIndex = index;
            wall.mSceneName = defaultObjectInfo.mSceneName;
            wall.mType = "Wall";
            wall.mVesselIndex = defaultObjectInfo.mHouse.mIndex;
            wall.mVesselName = defaultObjectInfo.mHouse.mName;
            wall.saveToDB(db);
            if (defaultObjectInfo.mObjectsOnWall != null) {
                for (ObjectInfo objectOnWall : defaultObjectInfo.mObjectsOnWall) {
                    if (objectOnWall.getSlotIndex() == index) {
                        objectOnWall.mVesselIndex = wall.mIndex;
                        objectOnWall.mVesselName = wall.mName;
                        objectOnWall.mSceneName = defaultObjectInfo.mSceneName;
                        if (objectOnWall.mIndex == 0) {
                            objectOnWall.mIndex = 1;
                        }
                        objectOnWall.saveToDB(db);
                    }
                }
            }

        }

        ObjectInfo ground = new ObjectInfo();
        ground.mName = "ground";
        ground.mType = "Ground";
        ground.mObjectSlot.mSlotIndex = -1;
        ground.mSceneName = defaultObjectInfo.mSceneName;
        ground.mVesselIndex = defaultObjectInfo.mHouse.mIndex;
        ground.mVesselName = defaultObjectInfo.mHouse.mName;
        ground.saveToDB(db);
        if (defaultObjectInfo.mObjectsOnGround != null) {
            for (ObjectInfo objectOnGround : defaultObjectInfo.mObjectsOnGround) {
                objectOnGround.mVesselIndex = ground.mIndex;
                objectOnGround.mVesselName = ground.mName;
                objectOnGround.mSceneName = defaultObjectInfo.mSceneName;
                if (objectOnGround.mIndex == 0) {
                    objectOnGround.mIndex = 1;
                }
                objectOnGround.saveToDB(db);
            }
        }
        if (defaultObjectInfo.mSky != null) {
            defaultObjectInfo.mSky.mVesselIndex = root.mIndex;
            defaultObjectInfo.mSky.mVesselName = root.mName;
            defaultObjectInfo.mSky.mSceneName = defaultObjectInfo.mSceneName;
            defaultObjectInfo.mSky.saveToDB(db);
        }
    }

    public List<ModelInfo> getBoundingModel() {
        return mBoundingModel;
    }
}
