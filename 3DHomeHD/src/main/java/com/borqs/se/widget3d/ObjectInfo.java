package com.borqs.se.widget3d;

import java.net.URISyntaxException;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;

import com.borqs.framework3d.home3d.SEMountPointChain;
import com.borqs.framework3d.home3d.SEObjectBoundaryPoint;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.home3d.UpdateDBThread;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.ProviderUtils.ObjectInfoColumns;
import com.borqs.se.home3d.ProviderUtils.Tables;
import com.borqs.se.home3d.ProviderUtils.VesselColumns;
import com.borqs.se.home3d.SearchActivity;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.engine.SEBitmap;
import com.borqs.se.engine.SEBitmap.Type;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.shortcut.AppItemInfo;
import com.borqs.se.shortcut.LauncherModel;

public class ObjectInfo {
    public static final int SLOT_TYPE_WALL = 1;
    public static final int SLOT_TYPE_DESKTOP = 2;
    @Deprecated
    public static final int SLOT_TYPE_WALL_GAP = 3;
    public static final int SLOT_TYPE_SKY = 0;
    @Deprecated
    public static final int SLOT_TYPE_APP_WALL = 4;
    public static final int SLOT_TYPE_WALL_SHELF = 5;
    private static final int MIN_SPAN_SIZE = 1;
    public int mID;

    public String mName;

    public String mType;

    public int mIndex;

    public int mShaderType;

    public String mSceneName;

    public String mVesselName;

    public int mVesselIndex;

    public ComponentName mComponentName;

    private ResolveInfo mResolveInfo;

    private Intent mIntent;

    private Context mContext;

    public int mSlotType;

    public ObjectSlot mObjectSlot;

    public int mAppWidgetId = -1;

    public String mShortcutUrl;

    public SEBitmap mShortcutIcon;

    public boolean mIsNativeObject;

    public String mClassName;

    public ModelInfo mModelInfo;

    public String mDisplayName;

    private ContentResolver mContentResolver;
    
    private int mOriStatus;

    public ObjectInfo() {
        mIndex = 0;
        mSlotType = 0;
        mShaderType = 0;
        mObjectSlot = new ObjectSlot();
        mIsNativeObject = false;
        mVesselIndex = 0;
        mContext = SESceneManager.getInstance().getContext();
        mContentResolver = mContext.getContentResolver();
        mID = -1;
        mOriStatus = SettingsActivity.getPreferRotation(mContext);
    }

    public boolean isShortcut() {

        if (mShortcutUrl != null || mShortcutIcon != null) {
            return true;
        }
        return false;
    }

    public boolean isDownloadObj() {
//        Cursor cursor = null;
//        try {
//            String localPath = mContext.getFilesDir() + File.separator + mName;
//            String where = FileURLInfoColumns.LOCAL_PATH + " = '" + localPath + "'";
//            cursor = mContentResolver.query(FileURLInfoColumns.CONTENT_URI, null, where, null, null);
//            if (cursor != null && cursor.moveToFirst()) {
//                return true;
//            }
//        }finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
        return false;
    }

    public void setModelInfo(ModelInfo modelInfo) {
        mModelInfo = modelInfo;
        if (mModelInfo != null) {
            mClassName = mModelInfo.mClassName;
            mSlotType = mModelInfo.mSlotType;
            mName = mModelInfo.mName;
            mType = mModelInfo.mType;
            if(!"jiazi".equalsIgnoreCase(mType)) {
            	mObjectSlot.mSpanX = mModelInfo.mSpanX;
            	mObjectSlot.mSpanY = mModelInfo.mSpanY;
            }
            mIsNativeObject = true;
        }
    }

    public ResolveInfo getResolveInfo() {
        if (mComponentName == null) {
            return null;
        }
        if (mResolveInfo == null) {
            mResolveInfo = HomeUtils.findResolveInfoByComponent(mContext, mComponentName);
        }
        return mResolveInfo;
    }

    public int getSlotIndex() {
        return mObjectSlot.mSlotIndex;
    }

    public int getSpanX() {
        return Math.max(MIN_SPAN_SIZE, mObjectSlot.mSpanX);
    }

    public int getSpanY() {
        return Math.max(MIN_SPAN_SIZE, mObjectSlot.mSpanY);
    }

    public int getStartX() {
        return mObjectSlot.mStartX;
    }

    public int getStartY() {
        return mObjectSlot.mStartY;
    }

    public Intent getIntent() {

    	if(mComponentName != null && HomeUtils.LOCKSCREEN_HOMEHD_PKG.equals(mComponentName.getPackageName())) {
    		// is lockScreen need switch intent 
    	}else {
    		if (mIntent != null) {
    			return mIntent;
    		}
    	}
        if (mShortcutUrl != null) {
            try {
                mIntent = Intent.parseUri(mShortcutUrl, 0);
                mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else if (mComponentName != null) {
        	String checkedAppName = mComponentName.getClassName();
        	boolean haveApp = false;
        	List<AppItemInfo> apps = LauncherModel.getInstance().getApps();
        	
        	//check objects_config componentName exists
        	for (AppItemInfo info : apps) {
        		ResolveInfo ri = info.getResolveInfo();
                String cls = ri.activityInfo.name;
                if (!TextUtils.isEmpty(cls) && cls.equals(checkedAppName)) {
                	haveApp = true;
                    break;
                }
        	}
            
        	if(!haveApp && mIsNativeObject) { // have not this package app, match with keyword
        		
        		mComponentName = null;
        		if(mModelInfo != null) {
	        		if (mModelInfo.mComponentName != null) {
	                    mComponentName = mModelInfo.mComponentName;
	                    updateComponentName(mComponentName);
	                }else {
	                	String[] keywords = mModelInfo.mKeyWords;
	                	if (keywords != null) {
	                		FindOK:
	                		for (String keyword : keywords) {
	                			for (AppItemInfo info : apps) {
	                				ResolveInfo ri = info.getResolveInfo();
	                				if (ri.activityInfo.name.toLowerCase().contains(keyword)) {
	                					mComponentName = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
	                					updateComponentName(mComponentName);
	                					break FindOK;
	                				}
	                			}
	                		}
	                	}
	                }
        		}
        	}
        	
        	if(mComponentName != null) {
        		if(mContext.getPackageName().equals(mComponentName.getPackageName())) { // hard code to start search activity
        			// start search Activity
        			mIntent = new Intent(SESceneManager.getInstance().getContext(), SearchActivity.class);
        			mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        		}else {
        			if(HomeUtils.LOCKSCREEN_HOMEHD_PKG.equals(mComponentName.getPackageName()) && !haveApp) { // hard code to recommend lockScreenHD
        				mIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://search?q=pname:" + HomeUtils.LOCKSCREEN_HOMEHD_PKG));
        				mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        			}else {
        				mIntent = new Intent(Intent.ACTION_MAIN);
        				mIntent.setComponent(mComponentName);
        				mIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        				mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        			}
        		}
        	}
            
        }else if(mComponentName == null && mIsNativeObject) {
        	if ("fangdajing".equals(mName)) {
        		mIntent = new Intent(SESceneManager.getInstance().getContext(), SearchActivity.class);
        		mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }else {
            	mComponentName = getCategoryComponentName();
            	if(mComponentName != null) {
            		updateComponentName(mComponentName);
            		mIntent = new Intent(Intent.ACTION_MAIN);
            		mIntent.setComponent(mComponentName);
            		mIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            		mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            	}
            }
        }
        return mIntent;
    }

    private String ensureVesselName() {
        if (TextUtils.isEmpty(mVesselName)) {
            Cursor cursor = null;
            try {
                cursor = null;
                String where = ObjectInfoColumns.OBJECT_ID + " = " + mObjectSlot.mVesselID;
                cursor = mContentResolver.query(ObjectInfoColumns.CONTENT_URI, null, where, null, null);
                if (null != cursor && cursor.moveToFirst()) {
                    final int index = cursor.getColumnIndexOrThrow(ObjectInfoColumns.OBJECT_NAME);
                    mVesselName = cursor.getString(index);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != cursor) {
                    cursor.close();
                }
            }
        }

        return mVesselName;
    }

    public SEBitmap getShortcutIcon(Type type) {
        if (mID < 0) {
            return null;
        }
        Cursor cursor = null;
        SEBitmap seBitmap = null;
        try {
            String where = ObjectInfoColumns.OBJECT_ID + " = " + mID ;
            cursor = mContentResolver.query(ObjectInfoColumns.CONTENT_URI, null, where, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                byte[] bytes = cursor.getBlob(cursor.getColumnIndexOrThrow(ObjectInfoColumns.SHORTCUT_ICON));
                if (bytes != null) {
                    seBitmap = new SEBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null),
                            type);
                }
            }
        }finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return seBitmap;
    }

    public ComponentName getCategoryComponentName() {
        if (mComponentName == null && mIsNativeObject) {
            if (mModelInfo.mComponentName != null) {
                mComponentName = mModelInfo.mComponentName;
                return mComponentName;
            }
            String[] keywords = mModelInfo.mKeyWords;
            if (keywords != null) {
                List<AppItemInfo> apps = LauncherModel.getInstance().getApps();
                for (String keyword : keywords) {
                    for (AppItemInfo info : apps) {
                        ResolveInfo ri = info.getResolveInfo();
                        if (ri.activityInfo.name.toLowerCase().contains(keyword)) {
                            mComponentName = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
                            return mComponentName;
                        }
                    }
                }
            }
        }
        return mComponentName;
    }

    public void updateComponentName(final ComponentName name) {
        mComponentName = name;
        UpdateDBThread.getInstance().process(new Runnable() {
            public void run() {
                String where = ObjectInfoColumns._ID + "=" + mID;
                ContentValues values = new ContentValues();
                values.put(ObjectInfoColumns.COMPONENT_NAME, name.flattenToShortString());
                mContentResolver.update(ObjectInfoColumns.CONTENT_URI, values, where, null);
            }
        });
        mIntent = null;
        mResolveInfo = null;
    }

    public void saveToDB(SQLiteDatabase db) {
        String where = ObjectInfoColumns.OBJECT_NAME + "='" + mName + "' and " + ObjectInfoColumns.SCENE_NAME + "='"
                + mSceneName + "' and " + ObjectInfoColumns.OBJECT_INDEX + "=" + mIndex + " and " + ObjectInfoColumns.ORIENTATION_STATUS + " = " + SettingsActivity.getPreferRotation(mContext);
        Cursor cursor = null;
        try {
            cursor = db.query(Tables.OBJECTS_INFO, null, where, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    return;
                }
            }
        }finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        ContentValues values = new ContentValues();
        saveToDB(values);
        mID = (int) db.insert(Tables.OBJECTS_INFO, null, values);
        mObjectSlot.mObjectID = mID;
        if (mVesselName != null) {
            where = ObjectInfoColumns.OBJECT_NAME + "='" + mVesselName + "' and " + ObjectInfoColumns.SCENE_NAME + "='"
                    + mSceneName + "' and " + ObjectInfoColumns.OBJECT_INDEX + "=" + mVesselIndex + " and " +
                    ObjectInfoColumns.ORIENTATION_STATUS + " = " + SettingsActivity.getPreferRotation(mContext);
            String[] columns = { ObjectInfoColumns.OBJECT_ID };
            cursor = db.query(Tables.OBJECTS_INFO, columns, where, null, null,
                    null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    mObjectSlot.mVesselID = cursor.getInt(0);
                }
                cursor.close();
            }
        }
        saveSlotToDB(db);
    }
    
    private void saveSlotToDB(SQLiteDatabase db) {
        String table = Tables.VESSEL;
        final String updateTable = table;
        ContentValues values = new ContentValues();
        saveSlotToDB(values);
        db.insert(updateTable, null, values);
    }

    public void saveToDB() {
        UpdateDBThread.getInstance().process(new Runnable() {
            public void run() {
                if (mID >= 0) {
                    updateSlotDB();
                    return;
                }
                ContentValues values = new ContentValues();
                saveToDB(values);
                Uri insertUri = mContentResolver.insert(ObjectInfoColumns.CONTENT_URI, values);
                if(insertUri == null) {
                    return;
                }
                long rowId = ContentUris.parseId(insertUri);
                if (rowId >= 0) {
                    mID = (int) rowId;
                    mObjectSlot.mObjectID = mID;
                    if (mVesselName != null) {
                        String where = ObjectInfoColumns.OBJECT_NAME + "='" + mVesselName + "' and "
                                + ObjectInfoColumns.SCENE_NAME + "='" + mSceneName + "' and "
                                + ObjectInfoColumns.OBJECT_INDEX + "=" + mVesselIndex;
                        String[] columns = { ObjectInfoColumns.OBJECT_ID };
                        Cursor cursor = mContentResolver.query(ObjectInfoColumns.CONTENT_URI, columns,
                                where, null, null);
                        if (cursor != null) {
                            if (cursor.moveToFirst()) {
                                mObjectSlot.mVesselID = cursor.getInt(0);
                            }
                            cursor.close();
                        }
                    }
                    saveSlotToDB();
                }
            }
        });
    }

    public void updateToDB(final boolean onlyName) {
        UpdateDBThread.getInstance().process(new Runnable() {
            public void run() {
                String where = ObjectInfoColumns.OBJECT_ID + "=" + mID;
                ContentValues values = new ContentValues();
                values.put(ObjectInfoColumns.DISPLAY_NAME, mDisplayName);
                if (!onlyName) {
                    values.put(ObjectInfoColumns.SHORTCUT_URL, mShortcutUrl);
                    if (mShortcutIcon != null) {
                        HomeUtils.writeBitmap(values, mShortcutIcon.getBitmap());
                        mShortcutIcon.recycle();
                    }
                }
                mContentResolver.update(ObjectInfoColumns.CONTENT_URI, values, where, null);
            }
        });
    }

    private void saveToDB(ContentValues values) {
        values.put(ObjectInfoColumns.OBJECT_NAME, mName);
        values.put(ObjectInfoColumns.OBJECT_TYPE, mType);
        values.put(ObjectInfoColumns.SHADER_TYPE, mShaderType);
        values.put(ObjectInfoColumns.SCENE_NAME, mSceneName);
        if (mComponentName != null) {
            values.put(ObjectInfoColumns.COMPONENT_NAME, mComponentName.flattenToShortString());
        }
        values.put(ObjectInfoColumns.CLASS_NAME, mClassName);
        values.put(ObjectInfoColumns.SLOT_TYPE, mSlotType);
        values.put(ObjectInfoColumns.WIDGET_ID, mAppWidgetId);
        values.put(ObjectInfoColumns.SHORTCUT_URL, mShortcutUrl);
        if (mIsNativeObject) {
            values.put(ObjectInfoColumns.IS_NATIVE_OBJ, 1);
        } else {
            values.put(ObjectInfoColumns.IS_NATIVE_OBJ, 0);
        }
        if (mShortcutIcon != null) {
            HomeUtils.writeBitmap(values, mShortcutIcon.getBitmap());
            mShortcutIcon.recycle();
        }
        values.put(ObjectInfoColumns.DISPLAY_NAME, mDisplayName);
        values.put(ObjectInfoColumns.OBJECT_INDEX, mIndex);
//        if("home_root".equals(mName) == false) {
        	values.put(ObjectInfoColumns.ORIENTATION_STATUS, mOriStatus);
//        }
    }

    public void doRelease(final boolean hard) {
    	UpdateDBThread.getInstance().process(new Runnable() {
            public void run() {
            	if (hard) {
            		String where = ObjectInfoColumns._ID + "=" + mID;
            		mContentResolver.delete(ObjectInfoColumns.CONTENT_URI, where, null);
            	}
                if (mShortcutIcon != null){
                    mShortcutIcon.recycle();
                }
            }
        });
    }
    
    public void releaseDB() {
        doRelease(true);
    }

    public void updateSlotDB() {
        UpdateDBThread.getInstance().process(new Runnable() {
            public void run() {
                String where = VesselColumns.OBJECT_ID + "=" + mID;
                ContentValues values = new ContentValues();
                saveSlotToDB(values);
                mContentResolver.update(VesselColumns.CONTENT_URI, values, where, null);
            }
        });

    }

    public void saveSlotToDB() {
        ContentValues values = new ContentValues();
        saveSlotToDB(values);
        mContentResolver.insert(VesselColumns.CONTENT_URI, values);
    }

    private void saveSlotToDB(ContentValues values) {
        values.put(VesselColumns.VESSEL_ID, mObjectSlot.mVesselID);
        values.put(VesselColumns.OBJECT_ID, mObjectSlot.mObjectID);
        values.put(VesselColumns.SLOT_INDEX, mObjectSlot.mSlotIndex);
        values.put(VesselColumns.SLOT_StartX, mObjectSlot.mStartX);
        values.put(VesselColumns.SLOT_StartY, mObjectSlot.mStartY);
        values.put(VesselColumns.SLOT_SpanX, mObjectSlot.mSpanX);
        values.put(VesselColumns.SLOT_SpanY, mObjectSlot.mSpanY);
        values.put(VesselColumns.SLOT_MPINDEX, mObjectSlot.mMountPointIndex);
        SEObjectBoundaryPoint bp = mObjectSlot.mBoundaryPoint;
        if(bp != null) {
        	values.put(VesselColumns.SLOT_BP_WALLINDEX, bp.wallIndex);
        	values.put(VesselColumns.SLOT_BP_MOVEPLANE, bp.movePlane);
        	values.put(VesselColumns.SLOT_BP_MAX_MATRIXPOINT_COL, bp.maxMatrixPoint.col);
        	values.put(VesselColumns.SLOT_BP_MAX_MATRIXPOINT_ROW, bp.maxMatrixPoint.row);
        	values.put(VesselColumns.SLOT_BP_MIN_MATRIXPOINT_COL, bp.minMatrixPoint.col);
        	values.put(VesselColumns.SLOT_BP_MIN_MATRIXPOINT_ROW, bp.minMatrixPoint.row);
        	values.put(VesselColumns.SLOT_BP_XYZSPAN_X, bp.xyzSpan.getX());
        	values.put(VesselColumns.SLOT_BP_XYZSPAN_Y, bp.xyzSpan.getY());
        	values.put(VesselColumns.SLOT_BP_XYZSPAN_Z, bp.xyzSpan.getZ());
        	values.put(VesselColumns.SLOT_BP_CENTER_X, bp.center.getX());
        	values.put(VesselColumns.SLOT_BP_CENTER_Y, bp.center.getY());
        	values.put(VesselColumns.SLOT_BP_CENTER_Z, bp.center.getZ());
        }
    }
    //for test
    public static ObjectInfo create(String objName, String sceneName, String className, 
    		                            String vesselName, boolean isNativeObject, int objectIndex,
    		                            //not must set
    		                            String shaderType, String componentName ,
    		                         String vesselIndex, String objectType,
    		                        String slotIndex, String slotType) {
    	    ObjectInfo info = new ObjectInfo();
    	    assert(objName != null);
        info.mName = objName;
        if (shaderType != null) {
            info.mShaderType = Integer.parseInt(shaderType);
        }
        assert(sceneName != null);
        info.mSceneName = sceneName;
        if (componentName != null) {
            info.mComponentName = ComponentName.unflattenFromString(componentName);
        }
        
        if ("wall".equals(slotType)) {
            info.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
        } else if ("desktop".equals(slotType)) {
            info.mSlotType = ObjectInfo.SLOT_TYPE_DESKTOP;
        } else if ("wallGap".equals(slotType)) {
            info.mSlotType = ObjectInfo.SLOT_TYPE_WALL_GAP;
        }
        assert(vesselName != null);
        info.mVesselName = vesselName;
        if (!TextUtils.isEmpty(vesselIndex)) {
            info.mVesselIndex = Integer.parseInt(vesselIndex);
        }

        
        if (!TextUtils.isEmpty(slotIndex)) {
            info.mObjectSlot.mSlotIndex = Integer.parseInt(slotIndex);
        }
        if (!TextUtils.isEmpty(className)) {
            info.mClassName = className;
        }
        info.mIsNativeObject = isNativeObject;
        
        

        info.mIndex = objectIndex;
        String type = objectType;
        if (!TextUtils.isEmpty(type)) {
            info.mType = type;
        }
      	return info;
    }
    //end
    public static ObjectInfo CreateFromXml(XmlPullParser parser) {
        ObjectInfo info = new ObjectInfo();
        info.mName = parser.getAttributeValue(null, ObjectInfoColumns.OBJECT_NAME);
        String shaderType = parser.getAttributeValue(null, ObjectInfoColumns.SHADER_TYPE);
        if (shaderType != null) {
            info.mShaderType = Integer.parseInt(shaderType);
        }
        info.mSceneName = parser.getAttributeValue(null, ObjectInfoColumns.SCENE_NAME);
        String componentName = parser.getAttributeValue(null, ObjectInfoColumns.COMPONENT_NAME);
        if (componentName != null) {
            info.mComponentName = ComponentName.unflattenFromString(componentName);
        }
        String slotType = parser.getAttributeValue(null, ObjectInfoColumns.SLOT_TYPE);
        info.mSlotType = fromSlotTypeString(slotType);

        info.mVesselName = parser.getAttributeValue(null, "vesselName");
        String vesselIndex = parser.getAttributeValue(null, "vesselIndex");
        if (!TextUtils.isEmpty(vesselIndex)) {
            info.mVesselIndex = Integer.parseInt(vesselIndex);
        }

        String slotIndex = parser.getAttributeValue(null, "slotIndex");
        if (!TextUtils.isEmpty(slotIndex)) {
            info.mObjectSlot.mSlotIndex = Integer.parseInt(slotIndex);
        }
        String mountPointIndexStr = parser.getAttributeValue(null, "mountPointIndex");
        if(!TextUtils.isEmpty(mountPointIndexStr)) {
        	info.mObjectSlot.mMountPointIndex = Integer.parseInt(mountPointIndexStr);
        }
        String slotStartX = parser.getAttributeValue(null, "slotStartX");
        if (!TextUtils.isEmpty(slotStartX)) {
            info.mObjectSlot.mStartX = Integer.parseInt(slotStartX);
        }
        String slotStartY = parser.getAttributeValue(null, "slotStartY");
        if (!TextUtils.isEmpty(slotStartY)) {
            info.mObjectSlot.mStartY = Integer.parseInt(slotStartY);
        }
        String slotSpanX = parser.getAttributeValue(null, "slotSpanX");
        if (!TextUtils.isEmpty(slotSpanX)) {
            info.mObjectSlot.mSpanX = Integer.parseInt(slotSpanX);
        }
        String slotSpanY = parser.getAttributeValue(null, "slotSpanY");
        if (!TextUtils.isEmpty(slotSpanY)) {
            info.mObjectSlot.mSpanY = Integer.parseInt(slotSpanY);
        }

        String className = parser.getAttributeValue(null, ObjectInfoColumns.CLASS_NAME);
        if (!TextUtils.isEmpty(className)) {
            info.mClassName = className;
        }

        String isnative = parser.getAttributeValue(null, ObjectInfoColumns.IS_NATIVE_OBJ);
        if (!TextUtils.isEmpty(isnative)) {
            if (Integer.parseInt(isnative) > 0) {
                info.mIsNativeObject = true;
            } else {
                info.mIsNativeObject = false;
            }
        }

        String index = parser.getAttributeValue(null, ObjectInfoColumns.OBJECT_INDEX);
        if (!TextUtils.isEmpty(index)) {
            info.mIndex = Integer.parseInt(index);
        }
        String type = parser.getAttributeValue(null, ObjectInfoColumns.OBJECT_TYPE);
        if (!TextUtils.isEmpty(type)) {
            info.mType = type;
        }

        return info;
    }
    private static void readBoundaryPoint(Cursor cursor, SEObjectBoundaryPoint bp) {
    	int movePlane = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_BP_MOVEPLANE));
    	int minRow = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_BP_MIN_MATRIXPOINT_ROW));
    	int minCol = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_BP_MIN_MATRIXPOINT_COL));
    	int maxRow = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_BP_MAX_MATRIXPOINT_ROW));
    	int maxCol = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_BP_MAX_MATRIXPOINT_COL));
    	float centerx = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_BP_CENTER_X));
    	float centery = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_BP_CENTER_Y));
    	float centerz = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_BP_CENTER_Z));
    	float xyzSpanX = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_BP_XYZSPAN_X));
    	float xyzSpanY = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_BP_XYZSPAN_Y));
    	float xyzSpanZ = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_BP_XYZSPAN_Z));
    	int wallIndex = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_BP_WALLINDEX));
    	bp.setData(wallIndex, movePlane, new SEVector3f(centerx, centery, centerz), 
    			new SEVector3f(xyzSpanX, xyzSpanY, xyzSpanZ), minRow, minCol, maxRow, maxCol, SEObjectBoundaryPoint.CENTER_POINT_STYLE_MID_MID);
    }
    public static ObjectInfo CreateFromDB(Cursor cursor) {
        ObjectInfo info = new ObjectInfo();
        info.mAppWidgetId = cursor.getInt(cursor.getColumnIndexOrThrow(ObjectInfoColumns.WIDGET_ID));
        info.mName = cursor.getString(cursor.getColumnIndexOrThrow(ObjectInfoColumns.OBJECT_NAME));
        info.mType = cursor.getString(cursor.getColumnIndexOrThrow(ObjectInfoColumns.OBJECT_TYPE));
        info.mID = cursor.getInt(cursor.getColumnIndexOrThrow(ObjectInfoColumns._ID));
        info.mShaderType = cursor.getInt(cursor.getColumnIndexOrThrow(ObjectInfoColumns.SHADER_TYPE));
        info.mSceneName = cursor.getString(cursor.getColumnIndexOrThrow(ObjectInfoColumns.SCENE_NAME));
        String componentName = cursor.getString(cursor.getColumnIndexOrThrow(ObjectInfoColumns.COMPONENT_NAME));
        if (componentName != null) {
            info.mComponentName = ComponentName.unflattenFromString(componentName);
        }
        info.mSlotType = cursor.getInt(cursor.getColumnIndexOrThrow(ObjectInfoColumns.SLOT_TYPE));
        info.mObjectSlot.mObjectID = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.OBJECT_ID));
        info.mObjectSlot.mVesselID = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.VESSEL_ID));
        info.mObjectSlot.mSlotIndex = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_INDEX));
        info.mObjectSlot.mStartX = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_StartX));
        info.mObjectSlot.mStartY = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_StartY));
        info.mObjectSlot.mSpanX = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_SpanX));
        info.mObjectSlot.mSpanY = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_SpanY));

        info.mObjectSlot.mMountPointIndex = cursor.getInt(cursor.getColumnIndexOrThrow(VesselColumns.SLOT_MPINDEX));
        info.mObjectSlot.mBoundaryPoint = new SEObjectBoundaryPoint(0);
        readBoundaryPoint(cursor, info.mObjectSlot.mBoundaryPoint);
        
        info.mClassName = cursor.getString(cursor.getColumnIndexOrThrow(ObjectInfoColumns.CLASS_NAME));
        byte[] bytes = cursor.getBlob(cursor.getColumnIndexOrThrow(ObjectInfoColumns.SHORTCUT_ICON));
        if (bytes != null) {
            info.mShortcutIcon = new SEBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null),
                    SEBitmap.Type.normal);
        }
        int nativeObj = cursor.getInt(cursor.getColumnIndexOrThrow(ObjectInfoColumns.IS_NATIVE_OBJ));
        if (nativeObj > 0) {
            info.mIsNativeObject = true;
        } else {
            info.mIsNativeObject = false;
        }
        info.mShortcutUrl = cursor.getString(cursor.getColumnIndexOrThrow(ObjectInfoColumns.SHORTCUT_URL));
        info.mIndex = cursor.getInt(cursor.getColumnIndexOrThrow(ObjectInfoColumns.OBJECT_INDEX));
        info.mDisplayName = cursor.getString(cursor.getColumnIndexOrThrow(ObjectInfoColumns.DISPLAY_NAME));
        info.ensureVesselName();
        return info;
    }

    public boolean isValidateMountPointData(SEMountPointChain chain) {
        if(mIsNativeObject || chain == null) {
            return true;
        }
        ObjectSlot slot = mObjectSlot;
        String vesselName = ensureVesselName();
        if("home4mian".equals(vesselName) || "home8mianshu".equals(vesselName)){
            int rowNum = chain.getRowCount();
            int colNum = chain.getColCount();
            int count = rowNum * colNum;
            int mountPointIndex = slot.mMountPointIndex;
            return mountPointIndex >= 0 && mountPointIndex < count;
        } else {
            return true;
        }
    }

    public static class ObjectSlot {
        public int mVesselID = -1;
        public int mObjectID = -1;
        public int mSlotIndex = 0;
        public int mStartX = 0;
        public int mStartY = 0;
        public int mSpanX = 0;
        public int mSpanY = 0;
        public SEObjectBoundaryPoint mBoundaryPoint;
        //for mount point index;
        public int mMountPointIndex = -1;
        public String toString() {
            return mSlotIndex + "," + mStartX + "," + mStartY + "," + mSpanX + "," + mSpanY + ", " + mMountPointIndex;
        }
        
        public void set(ObjectSlot slot) {
            mSlotIndex = slot.mSlotIndex;
            mStartX = slot.mStartX;
            mStartY = slot.mStartY;
            mSpanX = slot.mSpanX;
            mSpanY = slot.mSpanY;
            mMountPointIndex = slot.mMountPointIndex;
            if(slot.mBoundaryPoint != null) {
            	mBoundaryPoint = slot.mBoundaryPoint.clone();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            ObjectSlot newOP = (ObjectSlot) o;
            if (newOP.mSlotIndex == mSlotIndex && newOP.mStartX == mStartX && newOP.mStartY == mStartY
                    && newOP.mSpanX == mSpanX && newOP.mSpanY == mSpanY && newOP.mMountPointIndex == mMountPointIndex) {
            	if(newOP.mBoundaryPoint != null && this.mBoundaryPoint == null) {
            		return false;
            	} else if(newOP.mBoundaryPoint == null && this.mBoundaryPoint != null) {
            		return false;
            	} else if(newOP.mBoundaryPoint == null && this.mBoundaryPoint == null) {
            		return true;
            	} else {
            		return newOP.mBoundaryPoint.equals(this.mBoundaryPoint);
            	}
            }
            return false;
        }

        @Override
        public ObjectSlot clone() {
            ObjectSlot newSlot = new ObjectSlot();
            newSlot.mSlotIndex = mSlotIndex;
            newSlot.mStartX = mStartX;
            newSlot.mStartY = mStartY;
            newSlot.mSpanX = mSpanX;
            newSlot.mSpanY = mSpanY;
            if(this.mBoundaryPoint != null) {
                newSlot.mBoundaryPoint = this.mBoundaryPoint.clone();
            } else {
            	newSlot.mBoundaryPoint = null;
            }
            return newSlot;
        }

    }

    public void upgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
        case 0:
            break;
        }
    }


    public static int fromSlotTypeString(String slotType) {
        int type = 0;
        if ("wall".equals(slotType)) {
            type = ObjectInfo.SLOT_TYPE_WALL;
        } else if ("desktop".equals(slotType)) {
            type = ObjectInfo.SLOT_TYPE_DESKTOP;
        } else if ("wallGap".equals(slotType)) {
            type = ObjectInfo.SLOT_TYPE_WALL_GAP;
        } else if ("appWall".equals(slotType)) {
            type = ObjectInfo.SLOT_TYPE_APP_WALL;
        } else {
//            type = SLOT_TYPE_WALL;
        }
        return type;
    }
}
