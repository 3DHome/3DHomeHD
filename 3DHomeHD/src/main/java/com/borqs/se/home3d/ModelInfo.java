package com.borqs.se.home3d;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.borqs.framework3d.home3d.DockObject;
import com.borqs.framework3d.home3d.HouseObject;
import com.borqs.framework3d.home3d.TypeManager;
import com.borqs.market.wallpaper.WallpaperUtils;
import com.borqs.se.engine.SEObjectGroup;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.widget3d.NormalObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.text.TextUtils;

import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SECommand;
import com.borqs.se.home3d.ProviderUtils.ModelColumns;
import com.borqs.se.home3d.ProviderUtils.Tables;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.widget3d.ObjectInfo;

public class ModelInfo {
    private static final String TAG = "ModelInfo";

    public void cloneMenuItemInstance(SEObject parent, int index, boolean copy, String status) {
        if (null == mMenuInstance) {
            Log.e(TAG, "cloneMenuItemInstance without instance " + mName);
        } else {
            mMenuInstance.cloneObject_JNI(parent, index, copy, status);
        }
    }

    public void releaseMenuItem() {
        if (mMenuInstance != null) {
            mMenuInstance.getParent().removeChild(mMenuInstance, true);
            mMenuInstance = null;
        } else {
            Log.i(TAG, "releaseMenuItem has been released already " + mName);
        }
    }

    public void createMenuInstanceForMaxModel(SEScene scene) {
        if (null == mMenuInstance) {
            mMenuInstance = new ObjectsMenuItem(scene, this);
            if (Type.ICON_BOX.equalsIgnoreCase(mType)) {
                Log.d(TAG, "createMenuInstanceForMaxModel skip object " + mName + ", " + mType);
            }

            scene.getObjectsMenu().addChild(mMenuInstance, false);
            add3DMAXModel(scene, scene.getObjectsMenu());
        } else {
            Log.i(TAG, "createMenuInstanceForMaxModel has already been created and added  " + mName);
        }
    }

    public ObjectsMenuItem createMenuInstance(SEScene scene, SEObjectGroup parent) {
        if (null == mMenuInstance) {
            mMenuInstance = new ObjectsMenuItem(scene, this);
            if (Type.ICON_BOX.equalsIgnoreCase(mType)) {
                Log.d(TAG, "createMenuInstance skip object " + mName + ", " + mType);
            }

            parent.addChild(mMenuInstance, false);
        } else {
            Log.i(TAG, "createMenuInstance has already been created " + mName);
        }
        return mMenuInstance;
    }

    public static final class Type {
        // integer type id
        public static final int SLOT_TYPE_SKY = 0;
        public static final int SLOT_TYPE_WALL = 1;
        public static final int SLOT_TYPE_DESKTOP = 2;

        // String type id
        public static String SKY = "Sky";
        private static String HOUSE = "House";
        public static String DESKTOP = "Desk";

        public static String WALL_SHELF = "jiazi";
        public static String TV = "tv";
        public static String FOLDER = "Folder";

        public static String APP_ICON = "App";
        public static String SHORTCUT_ICON = "Shortcut";
        public static String APP_WIDGET = "Widget";
        public static String ICON_BOX = "IconBox";

        public static boolean isKnownType(String type) {
            final boolean result;
            if (SKY.equalsIgnoreCase(type)) {
                result = true;
            } else if (SKY.equalsIgnoreCase(type)) {
                result = true;
            } else if (HOUSE.equalsIgnoreCase(type)) {
                result = true;
            } else if (DESKTOP.equalsIgnoreCase(type)) {
                result = true;
            } else if (WALL_SHELF.equalsIgnoreCase(type)) {
                result = true;
            } else if (TV.equalsIgnoreCase(type)) {
                result = true;
            } else if (FOLDER.equalsIgnoreCase(type)) {
                result = true;
            } else if (APP_ICON.equalsIgnoreCase(type)) {
                result = true;
            } else if (SHORTCUT_ICON.equalsIgnoreCase(type)) {
                result = true;
            } else if (APP_WIDGET.equalsIgnoreCase(type)) {
                result = true;
            } else if (ICON_BOX.equalsIgnoreCase(type)) {
                result = true;
            } else {
                result = false;
            }
            return result;
        }
    }

    public static final class ClassName {
        // String type id
        public static String SkyObject = "com.borqs.framework3d.home3d.SkyObject";
        public static String HouseObject = "com.borqs.framework3d.home3d.HouseObject";
        public static String DockObject = "com.borqs.framework3d.home3d.DockObject";
    }

    public int mID;
    public String mClassName;
    public int mSlotType;
    public String mName;
    public String mSceneName;
    public String mType;
    public String mSceneFile;
    public String mBasedataFile;
    public String mAssetsPath;
    public String[] mChildNames;
    public String[] mKeyWords;
    public SETransParas mPreviewTrans;
    public SETransParas mLocalTrans;
    public int mSpanX;
    public int mSpanY;
    public ComponentName mComponentName;
    private int mNativeResource;
    public String mStatus;
    private List<ImageInfo> mImageInfos;
    private ImageInfo mCurImageInfo;

    public String mProductId;
    public int mVersionCode;
    public String mVersionName;
    public int mPlugin;

    private String mPlacementString = new String(" ");
    private List<SEObject> mInstances;
    private ObjectsMenuItem mMenuInstance;

    private ContentResolver mContentResolver;
    private ContentResolver getContentResolver() {
        if (null == mContentResolver) {
            Context context = SESceneManager.getInstance().getContext();
            mContentResolver = context.getContentResolver();
        }
        return mContentResolver;
    }

    public ModelInfo() {
        mNativeResource = 0;
        mSpanX = 0;
        mSpanY = 0;
        mInstances = new ArrayList<SEObject>();
    }

    public void register(SEObject instance) {
        if (!mInstances.contains(instance)) {
            mInstances.add(instance);
        }
    }
    

    public void unRegister(SEObject instance) {
        if (mInstances.contains(instance)) {
            mInstances.remove(instance);
        }
    }
    public List<SEObject> getInstances() {
        return mInstances;
    }
    public boolean hasInstance() {
        if (mInstances.size() > 0) {
            return true;
        }
        return false;
    }
    public void changeImageInfoPath(String newParentPath) {
        for(ImageInfo imageInfo : mImageInfos) {
            imageInfo.changeImageItemPath(newParentPath);
        }
    }
    public ArrayList<String> getPlacement() {
        if(mPlacementString == null) {
            return new ArrayList<String>();
        }
        String[] places = mPlacementString.split(" ");
        ArrayList<String> placeArray = new ArrayList<String>();
        for(int i = 0 ; i < places.length ; i++) {
            if(!places[i].equals(" ") && !places[i].equals("")) {
                placeArray.add(places[i]);
            }
        }
        return placeArray;
    }
    public static ModelInfo CreateFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        ModelInfo info = new ModelInfo();
        info.mName = parser.getAttributeValue(null, ModelColumns.OBJECT_NAME);
        info.mSceneName = parser.getAttributeValue(null, ModelColumns.SCENE_NAME);
        info.mType = parser.getAttributeValue(null, ModelColumns.TYPE);
        info.mSceneFile = parser.getAttributeValue(null, ModelColumns.SCENE_FILE);
        info.mBasedataFile = parser.getAttributeValue(null, ModelColumns.BASEDATA_FILE);
        info.mAssetsPath = parser.getAttributeValue(null, ModelColumns.ASSETS_PATH);
        String slotType = parser.getAttributeValue(null, ModelColumns.SLOT_TYPE);
        info.mSlotType = ObjectInfo.fromSlotTypeString(slotType);
        String spanX = parser.getAttributeValue(null, ModelColumns.SLOT_SPANX);
        if (!TextUtils.isEmpty(spanX)) {
            info.mSpanX = Integer.parseInt(spanX);
        }
        
        String spanY = parser.getAttributeValue(null, ModelColumns.SLOT_SPANY);
        if (!TextUtils.isEmpty(spanY)) {
            info.mSpanY = Integer.parseInt(spanY);
        }
        String previewTrans = parser.getAttributeValue(null, ModelColumns.PREVIEW_TRANS);
        if (previewTrans != null) {
            info.mPreviewTrans = new SETransParas();
            info.mPreviewTrans.init(ProviderUtils.getFloatArray(previewTrans, 10));
        }
        String localTrans = parser.getAttributeValue(null, ModelColumns.LOCAL_TRANS);
        if (localTrans != null) {
            info.mLocalTrans = new SETransParas();
            info.mLocalTrans.init(ProviderUtils.getFloatArray(localTrans, 10));
        }
        String className = parser.getAttributeValue(null, ModelColumns.CLASS_NAME);
        if (!TextUtils.isEmpty(className)) {
            info.mClassName = className;
        }
        String keyWords = parser.getAttributeValue(null, ModelColumns.KEY_WORDS);
        if (!TextUtils.isEmpty(keyWords)) {
            info.mKeyWords = ProviderUtils.getStringArray(keyWords);
        }
        String childNames = parser.getAttributeValue(null, ModelColumns.CHILD_NAMES);
        if (!TextUtils.isEmpty(childNames)) {
            info.mChildNames = ProviderUtils.getStringArray(childNames);
        }
        
        String componentName = parser.getAttributeValue(null, ModelColumns.COMPONENT_NAME);
        if (componentName != null) {
            info.mComponentName = ComponentName.unflattenFromString(componentName);
        }

        XmlUtils.nextElement(parser);
        int eventType = parser.getEventType();
        ImageInfo currentImageInfo = null;
        while (eventType != parser.END_DOCUMENT) {
            String tag = parser.getName();
            switch(eventType)
            {
                case XmlPullParser.START_TAG:
                {
                    if ("imageInfos".equals(tag)) {
                        if (info.mImageInfos == null) {
                            info.mImageInfos = new ArrayList<ImageInfo>();
                        }
                        ImageInfo imageInfo = new ImageInfo();
                        info.mImageInfos.add(imageInfo);
                        imageInfo.mThemeName = parser.getAttributeValue(null, ModelColumns.THEME_NAME);
                        currentImageInfo = imageInfo;
                    } else if("imageInfo".equals(tag)) {
                        ImageItem imageItem = new ImageItem();
                        imageItem.mImageName = parser.getAttributeValue(null, ModelColumns.IAMGE_NAME);
                        imageItem.mPath = parser.getAttributeValue(null, ModelColumns.IMAGE_PATH);
                        imageItem.mNewPath = imageItem.mPath;
                        if (!currentImageInfo.mImageItems.contains(imageItem)) {
                            currentImageInfo.mImageItems.add(imageItem);
                        }
                    } else if("objectPlacement".equals(tag)) {
                        String objPlacementString = parser.getAttributeValue(null, "placement");
                        if(objPlacementString != null)
                        {
                            info.mPlacementString = info.mPlacementString + " " + objPlacementString;
                        }
                    }
                }
            }
            eventType = parser.next();
        }
        return info;
    }
    public static ModelInfo CreateFromXml(XmlPullParser parser, String localPath) throws XmlPullParserException, IOException {
        ModelInfo info = new ModelInfo();
        info.mName = parser.getAttributeValue(null, ModelColumns.OBJECT_NAME);
        info.mSceneName = parser.getAttributeValue(null, ModelColumns.SCENE_NAME);
        info.mType = parser.getAttributeValue(null, ModelColumns.TYPE);
        info.mSceneFile = parser.getAttributeValue(null, ModelColumns.SCENE_FILE);
        info.mBasedataFile = parser.getAttributeValue(null, ModelColumns.BASEDATA_FILE);
//        info.mAssetsPath = parser.getAttributeValue(null, ModelColumns.ASSETS_PATH);
        info.mAssetsPath = localPath;
        String slotType = parser.getAttributeValue(null, ModelColumns.SLOT_TYPE);
        info.mSlotType = ObjectInfo.fromSlotTypeString(slotType);
        String spanX = parser.getAttributeValue(null, ModelColumns.SLOT_SPANX);
        if (!TextUtils.isEmpty(spanX)) {
            info.mSpanX = Integer.parseInt(spanX);
        }

        String spanY = parser.getAttributeValue(null, ModelColumns.SLOT_SPANY);
        if (!TextUtils.isEmpty(spanY)) {
            info.mSpanY = Integer.parseInt(spanY);
        }
        String previewTrans = parser.getAttributeValue(null, ModelColumns.PREVIEW_TRANS);
        if (previewTrans != null) {
            info.mPreviewTrans = new SETransParas();
            info.mPreviewTrans.init(ProviderUtils.getFloatArray(previewTrans, 10));
        }
        String localTrans = parser.getAttributeValue(null, ModelColumns.LOCAL_TRANS);
        if (localTrans != null) {
            info.mLocalTrans = new SETransParas();
            info.mLocalTrans.init(ProviderUtils.getFloatArray(localTrans, 10));
        }
        String className = parser.getAttributeValue(null, ModelColumns.CLASS_NAME);
        if (!TextUtils.isEmpty(className)) {
            info.mClassName = className;
        }
        String keyWords = parser.getAttributeValue(null, ModelColumns.KEY_WORDS);
        if (!TextUtils.isEmpty(keyWords)) {
            info.mKeyWords = ProviderUtils.getStringArray(keyWords);
        }
        String childNames = parser.getAttributeValue(null, ModelColumns.CHILD_NAMES);
        if (!TextUtils.isEmpty(childNames)) {
            info.mChildNames = ProviderUtils.getStringArray(childNames);
        }

        String componentName = parser.getAttributeValue(null, ModelColumns.COMPONENT_NAME);
        if (componentName != null) {
            info.mComponentName = ComponentName.unflattenFromString(componentName);
        }
        XmlUtils.nextElement(parser);
        int eventType = parser.getEventType();
        ImageInfo currentImageInfo = null;
        while (eventType != parser.END_DOCUMENT) {
            String tag = parser.getName();
            switch(eventType)
            {
                case XmlPullParser.START_TAG:
                {
                    if ("imageInfos".equals(tag)) {
                        if (info.mImageInfos == null) {
                            info.mImageInfos = new ArrayList<ImageInfo>();
                        }
                        ImageInfo imageInfo = new ImageInfo();
                        info.mImageInfos.add(imageInfo);
                        imageInfo.mThemeName = parser.getAttributeValue(null, ModelColumns.THEME_NAME);
                        currentImageInfo = imageInfo;
                    } else if("imageInfo".equals(tag)) {
                        ImageItem imageItem = new ImageItem();
                        imageItem.mImageName = parser.getAttributeValue(null, ModelColumns.IAMGE_NAME);
                        String tmpePath = parser.getAttributeValue(null, ModelColumns.IMAGE_PATH);
                        imageItem.mPath =localPath + File.separator + tmpePath;
                        imageItem.mNewPath = imageItem.mPath;
                        if (!currentImageInfo.mImageItems.contains(imageItem)) {
                            currentImageInfo.mImageItems.add(imageItem);
                        }
                    } else if("objectPlacement".equals(parser.getName())) {
                        String objPlacementString = parser.getAttributeValue(null, "placement");
                        if(objPlacementString != null)
                        {
                            info.mPlacementString = info.mPlacementString + " " + objPlacementString;
                        }
                    }
                }
            }
            eventType = parser.next();
        }
        return info;
    }

    public static ModelInfo CreateFromDB(Cursor cursor) {
        ModelInfo info = new ModelInfo();
        info.mID = cursor.getInt(cursor.getColumnIndexOrThrow(ModelColumns._ID));
        info.mName = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.OBJECT_NAME));
        info.mSceneName = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.SCENE_NAME));
        info.mType = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.TYPE));
        info.mSceneFile = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.SCENE_FILE));
        info.mBasedataFile = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.BASEDATA_FILE));
        info.mAssetsPath = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.ASSETS_PATH));
        info.mSlotType = cursor.getInt(cursor.getColumnIndexOrThrow(ModelColumns.SLOT_TYPE));

        info.mSpanX = cursor.getInt(cursor.getColumnIndexOrThrow(ModelColumns.SLOT_SPANX));
        info.mSpanY = cursor.getInt(cursor.getColumnIndexOrThrow(ModelColumns.SLOT_SPANY));
        info.mPlugin = cursor.getInt(cursor.getColumnIndexOrThrow(ModelColumns.PLUG_IN));
        info.mVersionCode = cursor.getInt(cursor.getColumnIndexOrThrow(ModelColumns.VERSION_CODE));
        info.mVersionName = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.VERSION_NAME));
        info.mProductId = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.PRODUCT_ID));
        
        String previewTrans = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.PREVIEW_TRANS));
        if (previewTrans != null) {
            info.mPreviewTrans = SETransParas.parseFrom(previewTrans);
        }
        String localTrans = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.LOCAL_TRANS));
        if (localTrans != null) {
            info.mLocalTrans = SETransParas.parseFrom(localTrans);
        }

        info.mClassName = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.CLASS_NAME));

        String keyWords = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.KEY_WORDS));
        if (!TextUtils.isEmpty(keyWords)) {
            info.mKeyWords = ProviderUtils.getStringArray(keyWords);
        }
        String childNames = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.CHILD_NAMES));
        if (!TextUtils.isEmpty(childNames)) {
            info.mChildNames = ProviderUtils.getStringArray(childNames);
        }
        String componentName = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.COMPONENT_NAME));
        if (componentName != null) {
            info.mComponentName = ComponentName.unflattenFromString(componentName);
        }
        String placementString = cursor.getString(cursor.getColumnIndexOrThrow(ModelColumns.PLACEMENT_STRING));
        info.mPlacementString = placementString;
        String where = ModelColumns._ID + "=" + info.mID;
        Context context = SESceneManager.getInstance().getContext();
        ContentResolver resolver = context.getContentResolver();
        Cursor imageCursor = resolver.query(ModelColumns.IMAGE_INFO_URI, null, where, null, null);
        if (info.mImageInfos == null) {
            info.mImageInfos = new ArrayList<ImageInfo>();
        }
        while (imageCursor.moveToNext()) {
            ImageItem imageItem = new ImageItem();
            String themeName = imageCursor.getString(imageCursor.getColumnIndexOrThrow(ModelColumns.THEME_NAME));
            imageItem.mImageName = imageCursor.getString(imageCursor.getColumnIndexOrThrow(ModelColumns.IAMGE_NAME));
            imageItem.mPath = imageCursor.getString(imageCursor.getColumnIndexOrThrow(ModelColumns.IMAGE_PATH));
            imageItem.mNewPath = imageCursor.getString(imageCursor.getColumnIndexOrThrow(ModelColumns.IMAGE_NEW_PATH));
            boolean hasImageInfo = false;
            for (ImageInfo imageInfo : info.mImageInfos) {
                if (imageInfo.mThemeName.equals(themeName)) {
                    hasImageInfo = true;
                    if (!imageInfo.mImageItems.contains(imageItem)) {
                        imageInfo.mImageItems.add(imageItem);
                    }
                    break;
                }
            }
            if (!hasImageInfo) {
                ImageInfo imageInfo = new ImageInfo();
                info.mImageInfos.add(imageInfo);
                imageInfo.mThemeName = themeName;
                if (!imageInfo.mImageItems.contains(imageItem)) {
                    imageInfo.mImageItems.add(imageItem);
                }
            }
        }
        imageCursor.close();
        return info;
    }

    public void saveToDB(final SQLiteDatabase db) {
        String where = ModelColumns.OBJECT_NAME + "='" + mName + "' and " + ModelColumns.SCENE_NAME + "='" + mSceneName
                + "'";
        Cursor cursor = db.query(Tables.MODEL_INFO, null, where, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                mID = cursor.getInt(cursor.getColumnIndexOrThrow(ModelColumns._ID));
                ContentValues values = new ContentValues();
                updateDB(values);
                db.update(Tables.MODEL_INFO, values, where, null);
                cursor.close();
                if (mImageInfos != null) {
                    updateImageInfo(db);
                }
                return;
            }
            cursor.close();
        }
        ContentValues values = new ContentValues();
        if (mClassName != null
                && (mClassName.endsWith("WallObject") || mClassName.endsWith("DesktopObject") || mClassName
                        .endsWith("WallGapObject"))) {
            mClassName = "com.borqs.se.widget3d.NormalObject";
        }
        saveToDB(values);
        mID = (int) db.insert(Tables.MODEL_INFO, null, values);
        if (mImageInfos != null) {
            saveImageInfo(db);
        }

    }
    
    public void removeImageInfoWithName(String name, SQLiteDatabase db) {
    	String table = Tables.IMAGE_INFO;
    	String where = ModelColumns.IAMGE_NAME + " = '" + name + "'"; 
    	db.delete(table, where, null);
    }

    private void updateImageInfo(SQLiteDatabase db) {
        String table = Tables.IMAGE_INFO;
        for (ImageInfo imageInfo : mImageInfos) {
            for (ImageItem imageItem : imageInfo.mImageItems) {
                String where = ModelColumns.THEME_NAME + "='" + imageInfo.mThemeName + "' and "
                        + ModelColumns.IAMGE_NAME + "='" + imageItem.mImageName + "'";
                Cursor cursor = db.query(table, null, where, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        ContentValues values = new ContentValues();
                        values.put(ModelColumns.IMAGE_PATH, imageItem.mPath);
                        db.update(table, values, where, null);
                        cursor.close();
                        continue ;
                    }
                    cursor.close();
                }
            	
                ContentValues values = new ContentValues();
                values.put(ModelColumns._ID, mID);
                values.put(ModelColumns.THEME_NAME, imageInfo.mThemeName);
                values.put(ModelColumns.IMAGE_PATH, imageItem.mPath);
                values.put(ModelColumns.IMAGE_NEW_PATH, imageItem.mNewPath);
                values.put(ModelColumns.IAMGE_NAME, imageItem.mImageName);
                db.insert(table, null, values);

            }
        }
    }
    
    private void saveImageInfo(SQLiteDatabase db) {
        String table = Tables.IMAGE_INFO;
        for (ImageInfo imageInfo : mImageInfos) {
            for (ImageItem imageItem : imageInfo.mImageItems) {
                ContentValues values = new ContentValues();
                values.put(ModelColumns._ID, mID);
                values.put(ModelColumns.THEME_NAME, imageInfo.mThemeName);
                values.put(ModelColumns.IMAGE_PATH, imageItem.mPath);
                values.put(ModelColumns.IMAGE_NEW_PATH, imageItem.mNewPath);
                values.put(ModelColumns.IAMGE_NAME, imageItem.mImageName);
                db.insert(table, null, values);
            }
        }

    }

    public void saveToDB() {
        String where = ModelColumns.OBJECT_NAME + "='" + mName + "' and " + ModelColumns.SCENE_NAME + "='" + mSceneName
                + "'";
        Cursor cursor = getContentResolver().query(ModelColumns.CONTENT_URI, null, where, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                mID = cursor.getInt(cursor.getColumnIndexOrThrow(ModelColumns._ID));
                ContentValues values = new ContentValues();
                updateDB(values);
                getContentResolver().update(ModelColumns.CONTENT_URI, values, where, null);
                cursor.close();
                if (mImageInfos != null) {
                    updateImageInfo();
                }
                return;
            }
            cursor.close();
        }
        ContentValues values = new ContentValues();
        if (mClassName != null
                && (mClassName.endsWith("WallObject") || mClassName.endsWith("DesktopObject") || mClassName
                        .endsWith("WallGapObject"))) {
            mClassName = "com.borqs.se.widget3d.NormalObject";
        }
        saveToDB(values);
        Uri insertUri = getContentResolver().insert(ModelColumns.CONTENT_URI, values);
        mID = (int) ContentUris.parseId(insertUri);
        if (mImageInfos != null) {
            saveImageInfo();
        }

    }

    public static void updateWallPaperDB(final Context context, final String newPath,
            final String currentImage, final String currentTheme) {
        UpdateDBThread.getInstance().process(new Runnable() {
            public void run() {
                ContentValues values = new ContentValues();
                values.put(ModelColumns.IMAGE_NEW_PATH, newPath);
                String where = ModelColumns.IAMGE_NAME + "='" + currentImage + "' and "
                        + ModelColumns.THEME_NAME
                        + "='" + currentTheme + "'";
                ContentResolver resolver = context.getContentResolver();
                resolver.update(ModelColumns.IMAGE_INFO_URI, values, where, null);
                WallpaperUtils.clearAppliedFlag(context);
            }
        });
    }

    public void updateImageInfo() {
        for (ImageInfo imageInfo : mImageInfos) {
            for (ImageItem imageItem : imageInfo.mImageItems) {
                String where = ModelColumns.THEME_NAME + "='" + imageInfo.mThemeName + "' and "
                        + ModelColumns.IAMGE_NAME + "='" + imageItem.mImageName + "'";
                Cursor cursor = getContentResolver().query(ModelColumns.IMAGE_INFO_URI, null, where, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        ContentValues values = new ContentValues();
                        values.put(ModelColumns.IMAGE_PATH, imageItem.mPath);
                        getContentResolver().update(ModelColumns.IMAGE_INFO_URI, values, where, null);
                        cursor.close();
                        return;
                    }
                    cursor.close();
                }
                ContentValues values = new ContentValues();
                values.put(ModelColumns._ID, mID);
                values.put(ModelColumns.THEME_NAME, imageInfo.mThemeName);
                values.put(ModelColumns.IMAGE_PATH, imageItem.mPath);
                values.put(ModelColumns.IMAGE_NEW_PATH, imageItem.mNewPath);
                values.put(ModelColumns.IAMGE_NAME, imageItem.mImageName);
                getContentResolver().insert(ModelColumns.IMAGE_INFO_URI, values);

            }
        }
    }

    public void saveImageInfo() {
        for (ImageInfo imageInfo : mImageInfos) {
            for (ImageItem imageItem : imageInfo.mImageItems) {
                ContentValues values = new ContentValues();
                values.put(ModelColumns._ID, mID);
                values.put(ModelColumns.THEME_NAME, imageInfo.mThemeName);
                values.put(ModelColumns.IMAGE_PATH, imageItem.mPath);
                values.put(ModelColumns.IMAGE_NEW_PATH, imageItem.mNewPath);
                values.put(ModelColumns.IAMGE_NAME, imageItem.mImageName);
                getContentResolver().insert(ModelColumns.IMAGE_INFO_URI, values);
            }
        }

    }

    private void updateDB(ContentValues values) {
        values.put(ModelColumns.SCENE_FILE, mSceneFile);
        values.put(ModelColumns.BASEDATA_FILE, mBasedataFile);
        values.put(ModelColumns.ASSETS_PATH, mAssetsPath);
        values.put(ModelColumns.SLOT_SPANX, mSpanX);
        values.put(ModelColumns.SLOT_SPANY, mSpanY);
        if (mPreviewTrans != null) {
            values.put(ModelColumns.PREVIEW_TRANS, mPreviewTrans.toString());
        }
        if (mLocalTrans != null) {
            values.put(ModelColumns.LOCAL_TRANS, mLocalTrans.toString());
        }
        values.put(ModelColumns.CLASS_NAME, mClassName);
        if (mKeyWords != null) {
            values.put(ModelColumns.KEY_WORDS, ProviderUtils.stringArrayToString(mKeyWords));
        }
        if (mChildNames != null) {
            values.put(ModelColumns.CHILD_NAMES, ProviderUtils.stringArrayToString(mChildNames));
        }
        if(mType != null) {
        	values.put(ModelColumns.TYPE, mType);
        }
        if(mPlacementString != null)
        {
            values.put(ModelColumns.PLACEMENT_STRING, mPlacementString);
        }
    }

    private void saveToDB(ContentValues values) {
        values.put(ModelColumns.OBJECT_NAME, mName);
        values.put(ModelColumns.SCENE_NAME, mSceneName);
        values.put(ModelColumns.TYPE, mType);
        values.put(ModelColumns.SCENE_FILE, mSceneFile);
        values.put(ModelColumns.BASEDATA_FILE, mBasedataFile);
        values.put(ModelColumns.ASSETS_PATH, mAssetsPath);
        values.put(ModelColumns.SLOT_TYPE, mSlotType);
        values.put(ModelColumns.SLOT_SPANX, mSpanX);
        values.put(ModelColumns.SLOT_SPANY, mSpanY);
        if (mPreviewTrans != null) {
            values.put(ModelColumns.PREVIEW_TRANS, mPreviewTrans.toString());
        }
        if (mLocalTrans != null) {
            values.put(ModelColumns.LOCAL_TRANS, mLocalTrans.toString());
        }
        values.put(ModelColumns.CLASS_NAME, mClassName);

        if (mKeyWords != null) {
            values.put(ModelColumns.KEY_WORDS, ProviderUtils.stringArrayToString(mKeyWords));
        }
        if (mChildNames != null) {
            values.put(ModelColumns.CHILD_NAMES, ProviderUtils.stringArrayToString(mChildNames));
        }
        if (mComponentName != null) {
            values.put(ModelColumns.COMPONENT_NAME, mComponentName.flattenToShortString());
        }
        values.put(ModelColumns.PRODUCT_ID, mProductId);
        values.put(ModelColumns.VERSION_CODE, mVersionCode);
        values.put(ModelColumns.VERSION_NAME, mVersionName);
        values.put(ModelColumns.PLUG_IN, mPlugin);
        if(mPlacementString != null)
        {
            values.put(ModelColumns.PLACEMENT_STRING, mPlacementString);
        }
    }
    //this function will load the Texture used by this 3D model and 
    //the load SE_Spatial of this model
    public void load3DMAXModel(final SEScene scene) {
        mCurImageInfo = getImageInfo(SettingsActivity.getThemeName(scene.getContext()));
        new SECommand(scene) {
            public void run() {
                loadImageItemOneByOne(scene, 0, mCurImageInfo.mImageItems);
                //add object placement
                TypeManager t = TypeManager.getInstance();
                ArrayList<String> vesselTypes = getPlacement();
                t.setObjectPlacementFromModelInfo(mName, vesselTypes);
                //
            }
        }.execute();
        mNativeResource = SEScene.loadResource_JNI(mAssetsPath + "/" + mSceneFile, mAssetsPath + "/" + mBasedataFile);
    }

    private void loadImageItemOneByOne(final SEScene scene, final int index, final List<ImageItem> imageItems) {
        if (index < imageItems.size()) {
            final ImageItem imageItem = imageItems.get(index);
            boolean exist = SEObject.isImageExist_JNI(imageItem.mNewPath);
            if (!exist) {
                SELoadResThread.getInstance().process(new Runnable() {
                    public void run() {
                        final int imageData = SEObject.loadImageData_JNI(imageItem.mNewPath);
                        if (imageData != 0) {
                            new SECommand(scene) {
                                public void run() {
                                    SEObject.addImageData_JNI(imageItem.mNewPath, imageData);
                                    loadImageItemOneByOne(scene, index + 1, imageItems);
                                }
                            }.execute();
                        } else {
                            if (!imageItem.mNewPath.equals(imageItem.mPath)) {
                                imageItem.mNewPath = imageItem.mPath;
                                loadImageItemOneByOne(scene, index, imageItems);
                                new SECommand(scene) {
                                    public void run() {
                                        SEObject.applyImage_JNI(imageItem.mImageName, imageItem.mNewPath);
                                    }
                                }.execute();
                            } else {
                                new SECommand(scene) {
                                    public void run() {
                                        loadImageItemOneByOne(scene, index + 1, imageItems);
                                    }
                                }.execute();
                            }
                        }
                    }
                });
            } else {
                loadImageItemOneByOne(scene, index + 1, imageItems);
            }
        }
    }

    private ImageInfo getImageInfo(String themeName) {
        for (ImageInfo imageInfo : mImageInfos) {
            if (imageInfo.mThemeName.equals(themeName)) {
                return imageInfo;
            }
        }
        return mImageInfos.get(0);
    }
    //mNativeResource is the pointer of SE_Spatial for object
    //add3DMaxModel will add this SE_Spatial to parent
    //and at the same time it will use inflateResource_JNI to create mesh data about it.
    public void add3DMAXModel(SEScene scene, SEObject parent) {
        if (mNativeResource != 0) {
            for (final ImageItem imageItem : mCurImageInfo.mImageItems) {
                SEObject.applyImage_JNI(imageItem.mImageName, imageItem.mNewPath);
            }
             //for vessel mount point
//            SEObject parentObj = scene.findObject(parent.mName, 0);
            String vesselGroupName = null;
            /*
            if(parentObj == null)
            {
            	    Log.i(TAG, "null object");
            }
            if(parentObj != null && parentObj.isVesselWithMountPoint())
            {
            	    Log.i(TAG, "has mount point");
            	    vesselGroupName = parentObj.getMountPointGroupName();
            }
            //end
            */
            Log.i(TAG, "add3DMAXModel, mName = " + mName);
            mStatus = scene.inflateResource_JNI(mNativeResource, mName, 0, parent.mName, parent.mIndex, vesselGroupName);
        }
    }

    public static class ImageInfo {
        public String mThemeName;
        public List<ImageItem> mImageItems = new ArrayList<ImageItem>();

        public ImageItem getImageItem(String imageName) {
            for (ImageItem item : mImageItems) {
                if (item.mImageName.endsWith(imageName)) {
                    return item;
                }
            }
            return null;
        }
        public void changeImageItemPath(String newParentPath) {
            for(ImageItem imageItem : mImageItems) {
                String imagePath = imageItem.mPath;
                String[] namePathArray = imagePath.split("/");
                String lastName = namePathArray[namePathArray.length - 1];
                String newPath = newParentPath + File.separator + lastName;
                imageItem.mPath = newPath;
                imageItem.mNewPath = newPath;
            }
        }
    }

    public static class ImageItem {

        @Override
        public boolean equals(Object o) {
            ImageItem newItem = (ImageItem) o;
            if (mImageName.equals(newItem.mImageName)) {
                return true;
            }
            return false;
        }

        public String mImageName;
        public String mPath;
        public String mNewPath;
    }

    /// Abstract for query Sky, House, and Desktop container objects, all follow the
    /// similar algorithm:
    /// 1. get weak reference object map for scene
    /// 2. query and return cached vessel object if existing in the map
    /// 3. query the object from the scene and put it into weak reference map of the scene
    /// 4. return null if not found.
    public static SEObject getSkyObject(SEScene scene) {
        SEObject object = getVesselObject(scene, ClassName.SkyObject);
        return object;
    }
    public static HouseObject getHouseObject(SEScene scene) {
        SEObject object = getVesselObject(scene, ClassName.HouseObject);
        return (HouseObject)object;
    }
    public static DockObject getDockObject(SEScene scene) {
        SEObject object = getVesselObject(scene, ClassName.DockObject);
        if (null != object) {
            return (DockObject)object;
        }
        return null;
    }

    private static final class VesselMap extends HashMap<String, WeakReference<SEObject>> {}
    private static Map<SEScene, VesselMap> sceneVesselObjectReference =
            new HashMap<SEScene, VesselMap>();
    private static SEObject getVesselObject(SEScene scene, String className) {
        VesselMap vesselMap = sceneVesselObjectReference.get(scene);
        if (null != vesselMap) {
            WeakReference<SEObject> objectReference = vesselMap.get(className);
            if (null != objectReference) {
                SEObject object = objectReference.get();
                if (null != object) {
                    return object;
                }
            }
        }

        SEObject object = scene.findFirstObjectByClass(className);
        if (null != object) {
            if (null == vesselMap) {
                vesselMap = new VesselMap();
                sceneVesselObjectReference.put(scene, vesselMap);
            }
            vesselMap.put(className, new WeakReference<SEObject>(object));
            return object;
        }

        return null;
    }
    public static boolean isSkyVesselObject(NormalObject object) {
        return isObjectInstance(ClassName.SkyObject, object);
    }

    public static boolean isHouseVesselObject(NormalObject object) {
        return isObjectInstance(ClassName.HouseObject, object);
    }

    public static boolean isDockVesselObject(NormalObject object) {
        return isObjectInstance(ClassName.DockObject, object);
    }

    public static boolean isObjectInstance(String className, SEObject object) {
        try {
            final Class<?> objectClass = Class.forName(className);
            return objectClass.isInstance(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    /// Abstract for query Sky, House, and Desktop container objects end

    // ugly implement, just pending for design reform.
    public static boolean isObjectShelfHidden(String type) {
        return  type.equals("Airship")
                || type.equals("jiazi")
                || type.equals("Folder")
                || type.equals("Recycle")
//                || type.equals("IconBox")
//                || type.equals("shop")
                || type.equals("IconBackground")
                || type.equals("walldialog");
    }

    public static boolean isUsingMountPointSlot(ObjectInfo objectInfo) {
    	return true;
    	/*
        final String type = null == objectInfo ? null: objectInfo.mType;
        final boolean result;
        if (Type.WALL_SHELF.equalsIgnoreCase(type)) {
            result = true;
        } else if (Type.TV.equalsIgnoreCase(type)) {
            result = true;
        } else if (Type.FOLDER.equalsIgnoreCase(type)) {
            result = true;
        } else if (isAppObjectItemType(type)) {
            result = true;
        } else {
            result = false;
        }

        return result;
        */
    }

    public static int getWallMountPointIndex(ObjectInfo objectInfo) {
        if (null == objectInfo || null == objectInfo.mObjectSlot) {
            if (HomeUtils.DEBUG) {
                Log.d(TAG, "getWallMountPointIndex, invalid object or slot info.");
            }
            return 0;
        }

        if (objectInfo.mObjectSlot.mMountPointIndex > 0) {
            return objectInfo.mObjectSlot.mMountPointIndex;
        }

        final String modelType = objectInfo.mType;
        //this is a HACK about mountp point row and col
        if (Type.WALL_SHELF.equalsIgnoreCase(modelType)) {
            return objectInfo.mObjectSlot.mStartX * 3 + objectInfo.mObjectSlot.mStartY;
        } else {
            return objectInfo.mObjectSlot.mMountPointIndex;
            /*
        	    if(objectInfo.mIsNativeObject || ModelInfo.isWidgetObjectType(objectInfo.mType)) {
        	    	SEObjectBoundaryPoint bp = objectInfo.mObjectSlot.mBoundaryPoint;
        	    	if(bp == null || bp.xyzSpan.equals(SEVector3f.ZERO)) {

        	    	    return objectInfo.mObjectSlot.mStartX + objectInfo.mObjectSlot.mStartY * 7;
        	    	} else {
        	    		return -1;
        	    	}
        	    } else {
                    return objectInfo.mObjectSlot.mMountPointIndex;
        	    }
        	    */
        }
    }

    public static boolean isAppItemWithBackground(String type) {
//        return "App".equals(type) || "Shortcut".equals(type) || "Widget".equals(type);
        final boolean result;
        if (Type.APP_ICON.equalsIgnoreCase(type)) {
            result = true;
        } else if (Type.SHORTCUT_ICON.equalsIgnoreCase(type)) {
            result = true;
        } else if (Type.APP_WIDGET.equalsIgnoreCase(type)) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }
    public static boolean isWidgetObjectType(String type) {
    	return Type.APP_WIDGET.equalsIgnoreCase(type);
    }
    public static boolean isAppObjectItemType(String type) {
//        return "App".equals(type) || "Shortcut".equals(type) || "Widget".equals(type)
//                                    || "IconBox".equals(type);
        final boolean result;
        if (isAppItemWithBackground(type)) {
            result = true;
        } else if (Type.ICON_BOX.equalsIgnoreCase(type)) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }

    public static void parseAllFromCursor(Cursor cursor, HashMap<String, ModelInfo> allModelInfo) {
        allModelInfo.clear();

        if (null != cursor && cursor.moveToFirst()) {
            ModelInfo info;
            do {
                info = ModelInfo.CreateFromDB(cursor);
                if (null != info) {
                    allModelInfo.put(info.mName, info);
                }
            } while (cursor.moveToNext());
        }
    }

    public static boolean isKnownType(String type) {
        return Type.isKnownType(type);
    }

    // TODO
    public static boolean isKnownSlotType(int slotType) {
        return false;
    }

    // TODO
    public static boolean isKnownPrimitiveType(int primitiveType) {
        return false;
    }

    // TODO
    public static boolean isKnownTargetVesselType(int vesselType) {
        return false;
    }

    // TODO
    public static boolean isKnownKeyword(String[] keyword) {
        return false;
    }

    public static String getHouseType() {
        SEScene scene = SESceneManager.getInstance().getCurrentScene();
        if (null == scene) {
            Log.e(TAG, "getHouseType, failed to get current scene.");
            return Type.HOUSE;
        }
        return getHouseObject(scene).getObjectInfo().mType;
    }

    public static ArrayList<String> getWallpaperKeySet() {
        HouseObject object = getHouseObject(SESceneManager.getInstance().getCurrentScene());
        if (null == object) {
            Log.e(TAG, "getWallpaperKeySet, could not find house object yet.");
            return new ArrayList<String>();
        } else {
            return object.getWallpaperKeySet();
        }
    }
    public static ArrayList<String> getGroundpaperKeySet() {
        HouseObject object = getHouseObject(SESceneManager.getInstance().getCurrentScene());
        if (null == object) {
            Log.e(TAG, "getGroundpaperKeySet, could not find house object yet.");
            return new ArrayList<String>();
        } else {
            return object.getGroundpaperKeySet();
        }
    }
}
