package com.borqs.se.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.borqs.framework3d.home3d.DockSceneInfo;
import com.borqs.framework3d.home3d.HouseSceneInfo;
import com.borqs.framework3d.home3d.ObjectShelfSceneInfo;
import com.borqs.se.home3d.UpdateDBThread;
import com.borqs.se.home3d.XmlUtils;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.ProviderUtils;
import com.borqs.se.home3d.ProviderUtils.SceneInfoColumns;
import com.borqs.se.home3d.ProviderUtils.Tables;

public class SESceneInfo {
    public static final String DEFAULT_SCENE_CONFIG_PATH = "base/scene_config_files";
    public static final String DEFAULT_SCENE_NAME = "home8";
    public static final String SCENE_CONFIG_KEY_CAMERA = "camera";

    public int mID;
    public String mSceneName;
    public float mSkyRadius = 0;
    public HouseSceneInfo mHouseSceneInfo = HouseSceneInfo.DEFAULT_INFO;
    public DockSceneInfo mDockSceneInfo = DockSceneInfo.DEFAULT_INFO;
    public ObjectShelfSceneInfo mObjectShelfSceneInfo = ObjectShelfSceneInfo.DEFAULT_INFO;

    private SECameraData mSECameraData;
    private ArrayList<SECameraData> mSECameraDataList;
    private HashMap<String, Integer> mSECameraIndexMap;
    
    public HashMap<String, ModelInfo> mModels;
    
    public int mStatus = 0;

    private ContentResolver mContentResolver;

    public SESceneInfo() {
        Context context = SESceneManager.getInstance().getContext();
        mContentResolver = context.getContentResolver();
    }

    public void updateWallAngle() {
        UpdateDBThread.getInstance().process(new Runnable() {
            public void run() {
                String where = SceneInfoColumns.SCENE_NAME + "='" + mSceneName + "'";
                ContentValues values = new ContentValues();
//                float angle = Math.round(mWallAngle * mWallNum / 360) * 360 / mWallNum;
                float angle = mHouseSceneInfo.round();
                values.put(SceneInfoColumns.WALL_ANGLE, angle);
                mContentResolver.update(SceneInfoColumns.CONTENT_URI, values, where, null);
            }
        });
    }

    public static SESceneInfo CreateFromXml(XmlPullParser parser) {
        try {
            SESceneInfo info = new SESceneInfo();
            info.mSceneName = parser.getAttributeValue(null, SceneInfoColumns.SCENE_NAME);

            String skyRadius = parser.getAttributeValue(null, SceneInfoColumns.SKY_RADIUS);
            if (!TextUtils.isEmpty(skyRadius)) {
                info.mSkyRadius = Float.valueOf(skyRadius);
            }

            info.mHouseSceneInfo.parseFromXml(parser);
            info.mDockSceneInfo.parseFromXml(parser);
            info.mObjectShelfSceneInfo.update(info.mDockSceneInfo);
            info.mSECameraDataList = new ArrayList<SECameraData>();

            XmlUtils.nextElement(parser);
            while (true) {
                SECameraData data = SECameraData.parseFromXml(parser);
                if (null != data) {
                	info.mSECameraDataList.add(data);
                	if(SECameraData.CAMERA_TYPE_DEFAULT.equals(data.mType)) {
                		info.mSECameraData = data;
                	}
                } else {
                    break;
                }
            }
            return info;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static SESceneInfo CreateFromDB(Cursor cursor) {
        SESceneInfo info = new SESceneInfo();
        info.mID = cursor.getInt(cursor.getColumnIndexOrThrow(SceneInfoColumns._ID));
        info.mSceneName = cursor.getString(cursor.getColumnIndexOrThrow(SceneInfoColumns.SCENE_NAME));
        info.mSkyRadius= cursor.getFloat(cursor.getColumnIndexOrThrow(SceneInfoColumns.SKY_RADIUS));

        info.mHouseSceneInfo.parseFromCursor(cursor);
        info.mDockSceneInfo.parseFromCursor(cursor);
        info.mObjectShelfSceneInfo.update(info.mDockSceneInfo);

        info.parseCameraData(cursor);

//        String where = ModelColumns.SCENE_NAME + "='" + info.mSceneName + "'";
//        Context context = SESceneManager.getInstance().getContext();
//        ContentResolver resolver = context.getContentResolver();
//        Cursor modelCursor = resolver.query(ModelColumns.CONTENT_URI, null, where, null, null);

//        while (modelCursor.moveToNext()) {
//            ModelInfo modelInfo = ModelInfo.CreateFromDB(modelCursor);
//            if (info.mModels == null) {
//                info.mModels = new HashMap<String, ModelInfo>();
//            }
//            info.mModels.put(modelInfo.mName, modelInfo);
//        }

        Context context = SESceneManager.getInstance().getContext();
        Cursor modelCursor = ProviderUtils.queryAllModelInfo(context, info.mSceneName);
        if (info.mModels == null) {
            info.mModels = new HashMap<String, ModelInfo>();
        }
        ModelInfo.parseAllFromCursor(modelCursor, info.mModels);

        modelCursor.close();
        return info;
    }

    public static void removeFromDB(final SQLiteDatabase db) {
    	db.delete(Tables.SCENE_INFO, null, null);
    	db.delete(Tables.CAMERA_DATA, null, null);
    }
    
    public void saveToDB(final SQLiteDatabase db) {
        String where = SceneInfoColumns.SCENE_NAME + "='" + mSceneName + "'";
        Cursor cursor = db.query(Tables.SCENE_INFO, null, where, null, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return;
            }
            cursor.close();
        }
        ContentValues values = new ContentValues();
        saveToDB(values);
        mID = (int) db.insert(Tables.SCENE_INFO, null, values);
//        values = new ContentValues();
        saveCameraDataToDB(db);
//        db.insert(Tables.CAMERA_DATA, null, values);
    }
    
   /* public void saveToDB() {
        String where = SceneInfoColumns.SCENE_NAME + "='" + mSceneName + "'";
        Cursor cursor = mContentResolver.query(SceneInfoColumns.CONTENT_URI, null, where, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return;
            }
            cursor.close();
        }
        ContentValues values = new ContentValues();
        saveToDB(values);
        Uri insertUri = mContentResolver.insert(SceneInfoColumns.CONTENT_URI, values);
        mID = (int) ContentUris.parseId(insertUri);
        values = new ContentValues();
        saveCameraDataToDB(values);
        mContentResolver.insert(SceneInfoColumns.CAMERA_DATA_URI, values);
    }*/

    private void saveToDB(ContentValues values) {
        // todo: abandon the class name, just a patch here.
        values.put(SceneInfoColumns.CLASS_NAME, "NO_SENSE");
        // todo: remove it from db later. patch end.
        values.put(SceneInfoColumns.SCENE_NAME, mSceneName);
        mHouseSceneInfo.fillInfoContent(values);
        mDockSceneInfo.fillInfoContent(values);
        values.put(SceneInfoColumns.SKY_RADIUS, mSkyRadius);
    }

    private ContentValues createCameraDataValue(SECameraData data) {
    	ContentValues values = new ContentValues();
    	values.put(SceneInfoColumns._ID, mID);
        values.put(SceneInfoColumns.FOV, data.mFov);
        values.put(SceneInfoColumns.NEAR, data.mNear);
        values.put(SceneInfoColumns.FAR, data.mFar);
        values.put(SceneInfoColumns.TYPE, data.mType);
        values.put(SceneInfoColumns.LOCATION, data.mLocation.toString());
        values.put(SceneInfoColumns.TARGETPOS, data.mTargetPos.toString());
        values.put(SceneInfoColumns.ZAXIS, data.mAxisZ.toString());
        values.put(SceneInfoColumns.UP, data.mUp.toString());
        return values;
    }
    private void saveCameraDataToDB(final SQLiteDatabase db) {
    	int cameraCount = mSECameraDataList.size();
    	if(cameraCount > 0) {
            for(int i = 0; i < cameraCount; i++){
            	ContentValues va = createCameraDataValue(mSECameraDataList.get(i));
            	db.insert(Tables.CAMERA_DATA, null, va);
            }
        }
    }

    public ModelInfo findModelInfo(String name) {
        return mModels.get(name);
    }

    public SECameraData getDefaultCameraData() {
        return mSECameraDataList.get(mSECameraIndexMap.get(SECameraData.CAMERA_TYPE_DEFAULT)).clone();
    }

    public SECameraData getFarCameraData() {
        return mSECameraDataList.get(mSECameraIndexMap.get(SECameraData.CAMERA_TYPE_FAR)).clone();
    }

    public SECameraData getNearCameraData() {
        return mSECameraDataList.get(mSECameraIndexMap.get(SECameraData.CAMERA_TYPE_NEAR)).clone();
    }

    public SECameraData getUpCameraData() {
        return mSECameraDataList.get(mSECameraIndexMap.get(SECameraData.CAMERA_TYPE_UP)).clone();
    }

    public SECameraData getDownCameraData() {
        return mSECameraDataList.get(mSECameraIndexMap.get(SECameraData.CAMERA_TYPE_DOWN)).clone();
    }

    private void parseCameraData(Cursor cursor) {
        if (null == mSECameraDataList) {
            mSECameraDataList = new ArrayList<SECameraData>();
        }
        if (null == mSECameraIndexMap) {
            mSECameraIndexMap = new HashMap<String, Integer>();
        }
        SECamera.parseCameraData(cursor, mSECameraDataList, mSECameraIndexMap);
        mSECameraData = getDefaultCameraData();
    }

    public SECameraData getCurrentData() {
        return mSECameraData;
    }

    public float getSkyHeightOffset() {
        return mHouseSceneInfo.getTopOffset() + 700;
    }

    public ArrayList<SECameraData> getAllCameraData() {
        return mSECameraDataList;
    }
}