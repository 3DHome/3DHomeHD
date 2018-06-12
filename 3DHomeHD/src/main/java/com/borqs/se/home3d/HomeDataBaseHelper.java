package com.borqs.se.home3d;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Xml;

import com.borqs.freehdhome.R;
import com.borqs.market.db.PlugInInfo;
import com.borqs.market.utils.MarketUtils;
import com.borqs.se.download.GridFragment.Category;
import com.borqs.se.download.Utils;
import com.borqs.se.home3d.ProviderUtils.ModelColumns;
import com.borqs.se.home3d.ProviderUtils.ObjectInfoColumns;
import com.borqs.se.home3d.ProviderUtils.PluginColumns;
import com.borqs.se.home3d.ProviderUtils.SceneInfoColumns;
import com.borqs.se.home3d.ProviderUtils.Tables;
import com.borqs.se.home3d.ProviderUtils.ThemeColumns;
import com.borqs.se.home3d.ProviderUtils.VesselColumns;
import com.borqs.se.engine.SESceneInfo;
import com.borqs.se.widget3d.AppObject;
import com.borqs.se.widget3d.ObjectInfo;

public class HomeDataBaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "HomeDataBaseHelper";
    private Context mContext;

    private static final String EVENTS_CLEANUP_TRIGGER_SQL_OBJECTS = 
            "DELETE FROM " + Tables.VESSEL + " WHERE objectID=old._id;";
    
    private static final String EVENTS_CLEANUP_TRIGGER_SQL_SCENES = 
            "DELETE FROM " + Tables.CAMERA_DATA + " WHERE _id= old._id;";

    private static final String EVENTS_CLEANUP_TRIGGER_SQL_MODELS = 
            "DELETE FROM " + Tables.IMAGE_INFO + " WHERE _id=old._id;";

    private static HashMap<String, String> MODEL_XMLS = new HashMap<String, String>();
    static {
        MODEL_XMLS.put("group_airplane", "base/airplane/models_config.xml");
//        MODEL_XMLS.put("group_bookshelf01", "base/bookshelf/models_config.xml");
//        MODEL_XMLS.put("group_calendar", "base/calendar/models_config.xml");
        MODEL_XMLS.put("group_camera", "base/camera/models_config.xml");
        MODEL_XMLS.put("group_clock", "base/clock/models_config.xml");
        MODEL_XMLS.put("group_contact", "base/contact/models_config.xml");
//        MODEL_XMLS.put("group_desk", "base/desk/models_config.xml");
        MODEL_XMLS.put("group_email", "base/email/models_config.xml");
        MODEL_XMLS.put("group_feiting", "base/feiting/models_config.xml");
        MODEL_XMLS.put("group_globe", "base/globe/models_config.xml");
        MODEL_XMLS.put("group_gplay", "base/gplay/models_config.xml");
//        MODEL_XMLS.put("group_heart", "base/heart/models_config.xml");
//        MODEL_XMLS.put("group_house8", "base/home8/models_config.xml");
        MODEL_XMLS.put("home4mian", "base/home4mian/models_config.xml");
        MODEL_XMLS.put("home8mianshu", "base/home8mianshu/models_config.xml");
        MODEL_XMLS.put("group_iconbox", "base/iconbox/models_config.xml");
//        MODEL_XMLS.put("group_navigation", "base/navigation/models_config.xml");
//        MODEL_XMLS.put("group_pc", "base/pc/models_config.xml");
        MODEL_XMLS.put("group_phone", "base/phone/models_config.xml");
        MODEL_XMLS.put("group_largewallpicframe", "base/picframe/horizontal_large/models_config.xml");
        MODEL_XMLS.put("group_hengwallpicframe", "base/picframe/horizontal_small/models_config.xml");
        MODEL_XMLS.put("group_tablepicframe", "base/picframe/table_frame/models_config.xml");
//        MODEL_XMLS.put("group_s3", "base/s3/models_config.xml");
        MODEL_XMLS.put("group_sky", "base/sky/models_config.xml");
        MODEL_XMLS.put("group_sms", "base/sms/models_config.xml");
//        MODEL_XMLS.put("group_speaker", "base/speaker/models_config.xml");
        MODEL_XMLS.put("group_tv", "base/tv/models_config.xml");
//        MODEL_XMLS.put("group_appwall", "base/appwall/appwall4/models_config.xml");
//        MODEL_XMLS.put("group_appwallheng", "base/appwall/appwallheng/models_config.xml");
        MODEL_XMLS.put("group_showbox", "base/showbox/models_config.xml");
        MODEL_XMLS.put("woodfolderclose", "base/woodfolderclose/models_config.xml");
        MODEL_XMLS.put("woodfolderopen", "base/woodfolderopen/models_config.xml");
        MODEL_XMLS.put("recycle", "base/recycle/models_config.xml");
        MODEL_XMLS.put("ipad", "base/ipad/models_config.xml");
        MODEL_XMLS.put("shop", "base/shop/models_config.xml");
        MODEL_XMLS.put("IconBackground","base/appwall/background/models_config.xml");
//        MODEL_XMLS.put("yuanzhuo", "base/yuanzhuo/models_config.xml");
        MODEL_XMLS.put("desk_fang", "base/desk_fang/models_config.xml");
        MODEL_XMLS.put("jiazi", "base/jiazi/models_config.xml");
        MODEL_XMLS.put("fangdajing", "base/fangdajing/models_config.xml");
        MODEL_XMLS.put("nexus4", "base/nexus4/models_config.xml");
        MODEL_XMLS.put("nexus10", "base/nexus10/models_config.xml");
        MODEL_XMLS.put("desk_fang_port", "base/desk_fang_port/models_config.xml");
        MODEL_XMLS.put("jiaziport", "base/jiaziport/models_config.xml");
        
       //TODO: Wait for the UI team resign the wall_dialog models has checked into SVN.
//        MODEL_XMLS.put("walldialogapp", "base/walldialog/app/models_config.xml");
//        MODEL_XMLS.put("walldialogdock", "base/walldialog/dock/models_config.xml");
//        MODEL_XMLS.put("walldialogwallpaper", "base/walldialog/wallpaper/models_config.xml");
//        MODEL_XMLS.put("walldialogwidget", "base/walldialog/widget/models_config.xml");
    }

    private HomeDataBaseHelper(Context context) {
        super(context, ProviderUtils.DATABASE_NAME, null, ProviderUtils.DATABASE_VERSION);
        mContext = context;
    }

    private static HomeDataBaseHelper mSEHomeDataBaseHelper;

    public static HomeDataBaseHelper getInstance(Context context) {
        if (mSEHomeDataBaseHelper == null) {
            mSEHomeDataBaseHelper = new HomeDataBaseHelper(context);
        }
        return mSEHomeDataBaseHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.SCENE_INFO + " (" + SceneInfoColumns._ID
                + " INTEGER PRIMARY KEY," + SceneInfoColumns.CLASS_NAME + " TEXT NOT NULL,"
                + SceneInfoColumns.SCENE_NAME + " TEXT NOT NULL," + SceneInfoColumns.WALL_NUM
                + " INTEGER NOT NULL DEFAULT 12," + SceneInfoColumns.WALL_SPANX + " INTEGER NOT NULL DEFAULT 4,"
                + SceneInfoColumns.WALL_SPANY + " INTEGER NOT NULL DEFAULT 4," + SceneInfoColumns.WALL_SIZEX
                + " FLOAT," + SceneInfoColumns.WALL_SIZEY + " FLOAT," + SceneInfoColumns.WALL_HEIGHT
                + " FLOAT NOT NULL DEFAULT 50," + SceneInfoColumns.WALL_RADIUS + " FLOAT NOT NULL DEFAULT 1000,"
                + SceneInfoColumns.SKY_RADIUS + " FLOAT NOT NULL DEFAULT 750," + SceneInfoColumns.DESK_HEIGHT
                + " FLOAT NOT NULL DEFAULT 100," + SceneInfoColumns.DESK_RADIUS + " FLOAT NOT NULL DEFAULT 140,"
                + SceneInfoColumns.DESK_NUM + " INTEGER NOT NULL DEFAULT 6," + SceneInfoColumns.WALL_ANGLE
                + " FLOAT NOT NULL DEFAULT 0" + ");");
        db.execSQL("CREATE TABLE " + Tables.CAMERA_DATA + " (" + "camera_id INTEGER PRIMARY KEY,"
                + SceneInfoColumns._ID + " INTEGER," + SceneInfoColumns.TYPE + " TEXT," + SceneInfoColumns.FOV + " FLOAT NOT NULL DEFAULT 60,"
                + SceneInfoColumns.NEAR + " FLOAT NOT NULL DEFAULT 1," + SceneInfoColumns.FAR
                + " FLOAT NOT NULL DEFAULT 2000," + SceneInfoColumns.LOCATION + " TEXT NOT NULL,"
                + SceneInfoColumns.TARGETPOS + " TEXT NOT NULL," + SceneInfoColumns.ZAXIS + " TEXT NOT NULL," 
                + SceneInfoColumns.UP + " TEXT NOT NULL" + ");");

        // ////////////////////////////////////////////////////////////////////////////////////////begin
        db.execSQL("CREATE TABLE " + Tables.OBJECTS_INFO + " (" 
                + ObjectInfoColumns.OBJECT_ID + " INTEGER PRIMARY KEY,"
                + ObjectInfoColumns.OBJECT_NAME + " TEXT NOT NULL,"
                + ObjectInfoColumns.OBJECT_TYPE + " TEXT," 
                + ObjectInfoColumns.SHADER_TYPE + " INTEGER NOT NULL DEFAULT 0," 
                + ObjectInfoColumns.SCENE_NAME + " TEXT,"
                + ObjectInfoColumns.COMPONENT_NAME + " TEXT,"
                + ObjectInfoColumns.SLOT_TYPE + " INTEGER NOT NULL DEFAULT 0,"
                + ObjectInfoColumns.CLASS_NAME + " TEXT,"
                + ObjectInfoColumns.SHORTCUT_ICON + " BLOB,"
                + ObjectInfoColumns.SHORTCUT_URL + " TEXT,"
                + ObjectInfoColumns.IS_NATIVE_OBJ + " INTEGER NOT NULL DEFAULT 0,"
                + ObjectInfoColumns.WIDGET_ID  + " INTEGER NOT NULL DEFAULT -1,"
                + ObjectInfoColumns.OBJECT_INDEX + " INTEGER NOT NULL DEFAULT 0,"
                + ObjectInfoColumns.DISPLAY_NAME + " TEXT,"
                + ObjectInfoColumns.ORIENTATION_STATUS + " INTEGER NOT NULL DEFAULT 0"
                + ");");

        db.execSQL("CREATE TABLE " + Tables.VESSEL + " (" 
                + VesselColumns.VESSEL_ID + " INTEGER,"
                + VesselColumns.OBJECT_ID + " INTEGER,"
                + VesselColumns.SLOT_INDEX + " INTEGER NOT NULL DEFAULT 0," 
                + VesselColumns.SLOT_StartX + " INTEGER NOT NULL DEFAULT 0,"
                + VesselColumns.SLOT_StartY + " INTEGER NOT NULL DEFAULT 0," 
                + VesselColumns.SLOT_SpanX + " INTEGER NOT NULL DEFAULT 0," 
                + VesselColumns.SLOT_SpanY + " INTEGER NOT NULL DEFAULT 0,"
                + VesselColumns.SLOT_MPINDEX + " INTEGER NOT NULL DEFAULT -1,"
                + VesselColumns.SLOT_BP_MOVEPLANE + " INTEGER NOT NULL DEFAULT 0, "
                + VesselColumns.SLOT_BP_WALLINDEX + " INTEGER NOT NULL DEFAULT -1, "
                + VesselColumns.SLOT_BP_MIN_MATRIXPOINT_ROW + " INTEGER NOT NULL DEFAULT -1, "
                + VesselColumns.SLOT_BP_MIN_MATRIXPOINT_COL + " INTEGER NOT NULL DEFAULT -1, "
                + VesselColumns.SLOT_BP_MAX_MATRIXPOINT_ROW + " INTEGER NOT NULL DEFAULT -1, "
                + VesselColumns.SLOT_BP_MAX_MATRIXPOINT_COL + " INTEGER NOT NULL DEFAULT -1, "
                + VesselColumns.SLOT_BP_CENTER_X + " FLOAT NOT NULL DEFAULT 0, "
                + VesselColumns.SLOT_BP_CENTER_Y + " FLOAT NOT NULL DEFAULT 0, "
                + VesselColumns.SLOT_BP_CENTER_Z + " FLOAT NOT NULL DEFAULT 0, "
                + VesselColumns.SLOT_BP_XYZSPAN_X + " FLOAT NOT NULL DEFAULT 0, "
                + VesselColumns.SLOT_BP_XYZSPAN_Y + " FLOAT NOT NULL DEFAULT 0, "
                + VesselColumns.SLOT_BP_XYZSPAN_Z + " FLOAT NOT NULL DEFAULT 0"
                + ");");

        createTableModelInfo(db);

        db.execSQL("CREATE TABLE " + Tables.IMAGE_INFO + " (" + "image_id INTEGER PRIMARY KEY,"
                + ModelColumns._ID + " INTEGER," + ModelColumns.THEME_NAME + " TEXT," + ModelColumns.IAMGE_NAME
                + " TEXT," + ModelColumns.IMAGE_PATH + " TEXT," + ModelColumns.IMAGE_NEW_PATH + " TEXT" + ");");

        // Trigger to remove data tied to an event when we delete that event.
        db.execSQL("CREATE TRIGGER objects_cleanup_delete DELETE ON " + Tables.OBJECTS_INFO + " "
                + "BEGIN " + EVENTS_CLEANUP_TRIGGER_SQL_OBJECTS + "END");
        // Trigger to remove data tied to an event when we delete that event.
        db.execSQL("CREATE TRIGGER scenes_cleanup_delete DELETE ON " + Tables.SCENE_INFO + " "
                + "BEGIN " + EVENTS_CLEANUP_TRIGGER_SQL_SCENES + "END");

        db.execSQL("CREATE TRIGGER models_cleanup_delete DELETE ON " + Tables.MODEL_INFO + " " + "BEGIN "
                + EVENTS_CLEANUP_TRIGGER_SQL_MODELS + "END");
        
//        db.execSQL("CREATE TABLE " + Tables.FILE_URL_INFO + " (" + FileURLInfoColumns._ID
//                + " INTEGER PRIMARY KEY,"
//                + FileURLInfoColumns.NAME + " TEXT NOT NULL,"
//                + FileURLInfoColumns.SERVER_PATH + " TEXT,"
//                + FileURLInfoColumns.LOCAL_PATH + " TEXT,"
//                + FileURLInfoColumns.TYPE + " INTEGER DEFAULT 0,"
//                + FileURLInfoColumns.APPLY_STATUS + " INTEGER DEFAULT 0,"
//                + FileURLInfoColumns.DOWNLOAD_STATUS + " INTEGER DEFAULT 0,"
//                + FileURLInfoColumns.FILE_LENGTH + " LONG NOT NULL DEFAULT 0,"
//                + FileURLInfoColumns.THREAD_INFOS + " TEXT, "
//                + " UNIQUE (" + FileURLInfoColumns.NAME + ")" + ");");

//        db.execSQL("CREATE TABLE " + ProviderUtils.Tables.THEME + " (" + ThemeColumns._ID
//                + " INTEGER PRIMARY KEY,"
//                + ThemeColumns.NAME + " TEXT NOT NULL,"
//                + ThemeColumns.FILE_PATH + " TEXT,"
//                + ThemeColumns.TYPE + " INTEGER DEFAULT 0,"
//                + ThemeColumns.IS_APPLY + " INTEGER DEFAULT 0,"
//                + ThemeColumns.CONFIG + " TEXT,"
//                + " UNIQUE (" + ThemeColumns.NAME + ")" + ");");

        createTableTheme(db);        
        ContentValues wood = new ContentValues();
        wood.put(ThemeColumns.NAME, "default");
        wood.put(ThemeColumns.TYPE, Category.DEFAULT_THEME.ordinal());
        wood.put(ThemeColumns.FILE_PATH, "base/home4mian");
        wood.put(ThemeColumns.IS_APPLY, 1);
        db.insert(Tables.THEME, null, wood);

//        ContentValues white = new ContentValues();
//        white.put(ThemeColumns.NAME, "white");
//        white.put(ThemeColumns.TYPE, Category.DEFAULT_THEME.ordinal());
//        white.put(ThemeColumns.FILE_PATH, "base/home8/newTexture");
//        white.put(ThemeColumns.IS_APPLY, 0);
//        white.put(ThemeColumns.CONFIG, "light:false");
//        db.insert(Tables.THEME, null, white);
//
//        ContentValues dark = new ContentValues();
//        dark.put(ThemeColumns.NAME, "dark");
//        dark.put(ThemeColumns.TYPE, Category.DEFAULT_THEME.ordinal());
//        dark.put(ThemeColumns.FILE_PATH, "base/home8/darkTexture");
//        dark.put(ThemeColumns.IS_APPLY, 0);
//        db.insert(Tables.THEME, null, dark);
        
        loadDefaultData(db);
//        createTablePlugIn(db);
    }
    
    public void createTableModelInfo(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.MODEL_INFO 
                + " (" + ModelColumns._ID + " INTEGER PRIMARY KEY," 
                + ModelColumns.OBJECT_NAME + " TEXT NOT NULL," 
                + ModelColumns.COMPONENT_NAME + " TEXT," 
                + ModelColumns.TYPE + " TEXT NOT NULL," 
                + ModelColumns.SCENE_FILE + " TEXT,"
                + ModelColumns.BASEDATA_FILE + " TEXT," 
                + ModelColumns.ASSETS_PATH + " TEXT,"
                + ModelColumns.PREVIEW_TRANS + " TEXT," 
                + ModelColumns.LOCAL_TRANS + " TEXT," 
                + ModelColumns.CLASS_NAME + " TEXT," 
                + ModelColumns.SCENE_NAME + " TEXT," 
                + ModelColumns.KEY_WORDS + " TEXT,"
                + ModelColumns.CHILD_NAMES + " TEXT," 
                + ModelColumns.SLOT_TYPE + " INTEGER NOT NULL DEFAULT 0,"
                + ModelColumns.SLOT_SPANX + " INTEGER NOT NULL DEFAULT 0," 
                + ModelColumns.PLUG_IN + " INTEGER NOT NULL DEFAULT 0,"
                + ModelColumns.VERSION_NAME + " TEXT,"
                + ModelColumns.VERSION_CODE + " INTEGER,"
                + ModelColumns.PRODUCT_ID + " TEXT,"
                + ModelColumns.PLACEMENT_STRING + " TEXT,"
                + ModelColumns.SLOT_SPANY + " INTEGER NOT NULL DEFAULT 0" + ");");
    }
    public void createTableTheme(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.THEME + " (" + ThemeColumns._ID
                + " INTEGER PRIMARY KEY,"
                + ThemeColumns.NAME + " TEXT NOT NULL,"
                + ThemeColumns.FILE_PATH + " TEXT,"
                + ThemeColumns.TYPE + " INTEGER DEFAULT 0,"
                + ThemeColumns.IS_APPLY + " INTEGER DEFAULT 0,"
                + ThemeColumns.CONFIG + " TEXT,"
                + ThemeColumns.VERSION_NAME + " TEXT,"
                + ThemeColumns.VERSION_CODE + " INTEGER,"
                + ThemeColumns.PRODUCT_ID + " TEXT,"
                + " UNIQUE (" + ThemeColumns.NAME + ")" + ");");
    }
    
    public void createTablePlugIn(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.PLUGIN + " (" + PluginColumns._ID
                + " INTEGER PRIMARY KEY,"
                + PluginColumns.PRODUCT_ID + " TEXT,"
                + PluginColumns.Name + " TEXT,"
                + PluginColumns.VERSION_CODE + " INTEGER,"
                + PluginColumns.VERSION_NAME + " TEXT,"
                + PluginColumns.TYPE + " TEXT,"
                + PluginColumns.IS_APPLY + " INTEGER DEFAULT 0 );");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        ensureAddModelInfo12(db, oldVersion);
        switch (oldVersion) {
//        case 0:
          case 1:
//            upgradeDatabaseToVersion2(db);
        case 2:
        case 3:
            dropAllTables(db);
            onCreate(db);
        case 4:
        	upgradeDatabaseToVersion5(db);
		case 5:
        	upgradeDatabaseToVersion6(db);
        case 6:
        	upgradeDatabaseToVersion7(db);
        case 7:
        	upgradeDatabaseToVersion8(db);
        case 8:
        	upgradeDatabaseToVersion9(db);
        case 9:
        	upgradeDatabaseToVersion10(db);
        case 10:
        	upgradeDatabaseToVersion11(db);
        case 11:
            upgradeDatabaseToVersion12(db);
//        case 7:
//        case 8:
//        case 9:
//        case 10:
//        case 11:
//        case 12:
//        case 13:
//        case 14:
//            upgradeDatabaseToVersion15(db);
//        case 15:
//            upgradeDatabaseToVersion16(db);
//        case 16:
//            upgradeDatabaseToVersion17(db);
//        case 17:
//            upgradeDatabaseToVersion18(db);
//        case 18:
//            upgradeDatabaseToVersion19(db);
//        case 19:
//            upgradeDatabaseToVersion20(db);
//        case 20:
//            upgradeDatabaseToVersion21(db);
//        case 21:
//            upgradeDatabaseToVersion22(db);
//        case 22:
//        case 23:
//            upgradeDatabaseToVersion24(db);
//        case 24:
//            upgradeDatabaseToVersion25(db);
//        case 25:
//            upgradeDatabaseToVersion26(db);
//        case 26:
//            upgradeDatabaseToVersion27(db);
//        case 27:
//        case 28:
//        case 29:
//            upgradeDatabaseToVersion30(db);
//        case 30:
//            upgradeDatabaseToVersion31(db);
//        case 31:
//        case 32:
//            upgradeDatabaseToVersion33(db);
//        case 33:
//        case 34:
//            upgradeDatabaseToVersion35(db);
//        case 35:
//        case 36:
//        case 37:
//            upgradeDatabaseToVersion38(db);
//        case 38:
//            upgradeDatabaseToVersion39(db);
//        case 39:
//            upgradeDatabaseToVersion40(db);
//        case 40:
//            upgradeDatabaseToVersion41(db);
//        case 41:
//            upgradeDatabaseToVersion42(db);
//        case 42:
//            upgradeDatabaseToVersion43(db);
//        case 43:
//            upgradeDatabaseToVersion44(db);
//        case 44:
//            upgradeDatabaseToVersion45(db);
//        case 45:
//            upgradeDatabaseToVersion46(db);
//        case 46:
//            upgradeDatabaseToVersion47(db);
//        case 47:
//            upgradeDatabaseToVersion48(db);
//        case 48:
//            upgradeDatabaseToVersion49(db);
//        case 49:
//            upgradeDatabaseToVersion50(db);
//        case 50:
//            upgradeDatabaseToAddNewModel(db);
//        case 51:
//            upgradeDatabaseToVersion52(db);
//        case 52:
//            upgradeDatabaseToAddNewModel(db);
//        case 53:
//            upgradeDatabaseToAddNewModel(db);
        default:
            break;
        }
//        loadDefaultData(db, oldVersion);
    }

    private void upgradeDatabaseToVersion11(SQLiteDatabase db) {
    	loadContactModelInfo(db);
	}

    private boolean mAddModelInfo12 = false;
    private void ensureAddModelInfo12(SQLiteDatabase db, int oldVersion) {
        if (mAddModelInfo12 || oldVersion > 12) {
            return;
        }
        try {
            db.execSQL("ALTER TABLE " + Tables.MODEL_INFO + " ADD " + ModelColumns.PLACEMENT_STRING + " TEXT;");
            mAddModelInfo12 = true;
        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
        }
    }
    private void upgradeDatabaseToVersion12(SQLiteDatabase db)  {
        ensureAddModelInfo12(db, 12);
    }
    private void loadContactModelInfo(SQLiteDatabase db) {
    	try {
    		InputStream is = mContext.getAssets().open("base/contact/models_config.xml");
    		XmlPullParser parser = Xml.newPullParser();
    		parser.setInput(is, "utf-8");
    		XmlUtils.beginDocument(parser, "config");
    		ModelInfo config = ModelInfo.CreateFromXml(parser);
    		config.saveToDB(db);
    		is.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (XmlPullParserException e) {
    		e.printStackTrace();
    	}
    }
    
	private void upgradeDatabaseToVersion10(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + Tables.OBJECTS_INFO + " ADD COLUMN " + ObjectInfoColumns.ORIENTATION_STATUS + " INTEGER NOT NULL DEFAULT 0;");
		loadHome8MianShuModelInfo(db);
		loadDeskFangPortMianShuModelInfo(db);
		loadHome4MianModelInfo(db);
		loadJiaziPortModelInfo(db);
	}

	private void upgradeDatabaseToVersion9(SQLiteDatabase db) {
    	loadIconBoxModelInfo(db);
	}

	private void upgradeDatabaseToVersion8(SQLiteDatabase db) {
    	updateAppObjectType(db);
    }
    
    private void updateAppObjectType(SQLiteDatabase db) {
        String where = ObjectInfoColumns.OBJECT_NAME + " like 'app_%' and (" + ObjectInfoColumns.OBJECT_TYPE + " is null or " + ObjectInfoColumns.OBJECT_TYPE + "='')";
        
        ContentValues values = new ContentValues();
    	values.put(ObjectInfoColumns.OBJECT_TYPE, ModelInfo.Type.APP_ICON);
    	db.update(Tables.OBJECTS_INFO, values, where, null);
    }
    
//    public void upgradeDatabaseToVersion2(SQLiteDatabase db){
//        db.beginTransaction();
//        try {
//            //theme
//            db.execSQL("ALTER TABLE " + Tables.THEME + " ADD " + ThemeColumns.PRODUCT_ID +  " TEXT;");
//            db.execSQL("ALTER TABLE "  + Tables.THEME  + " ADD " + ThemeColumns.VERSION_CODE + " INTEGER;");
//            db.execSQL("ALTER TABLE " + Tables.THEME + " ADD " + ThemeColumns.VERSION_NAME +  " TEXT;");
//            //object
//            db.execSQL("ALTER TABLE " + Tables.MODEL_INFO + " ADD " + ModelColumns.PRODUCT_ID +  " TEXT;");
//            db.execSQL("ALTER TABLE "  + Tables.MODEL_INFO  + " ADD " + ModelColumns.VERSION_CODE + " INTEGER;");
//            db.execSQL("ALTER TABLE " + Tables.MODEL_INFO+ " ADD " + ModelColumns.VERSION_NAME +  " TEXT;");
//            db.execSQL("ALTER TABLE " + Tables.MODEL_INFO+ " ADD " + ModelColumns.PLUG_IN +  " INTEGER;");
//            //plugin
//            createTablePlugIn(db);
//            db.setTransactionSuccessful();
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//            db.endTransaction();
//        }
//
//    }

    private void upgradeDatabaseToVersion7(SQLiteDatabase db) {
    	loadMagnifierModelInfo(db);
    	addMagnifierAppObject(db);
	}

	private void upgradeDatabaseToVersion5(SQLiteDatabase db) {
    	loadHome4MianModelInfo(db);
	}
	
	
    /** match objects_config.xml 
     * <config name="App" sceneName="home8" vesselName="jiazi"
            slotType="appWall" slotIndex="5" mountPointIndex="0" isNativeObj="0" objectIndex="1" vesselIndex="5"
            slotStartX="0"
            slotStartY="0"
            slotSpanX="1"
            slotSpanY="1"
            componentName = "com.borqs.freehdhome/com.borqs.se.home3d.SearchActivity" />
            
     * @param db
     */
	private void addMagnifierAppObject(SQLiteDatabase db) {
		ObjectInfo info = new ObjectInfo();
        info.mName = AppObject.generateAppName();
        info.mSceneName = "home8";
        info.mComponentName = ComponentName.unflattenFromString("com.borqs.freehdhome/com.borqs.se.home3d.SearchActivity");
        
        info.mSlotType = ObjectInfo.fromSlotTypeString("appWall");
        info.mVesselName = "jiazi";
        info.mVesselIndex = 5;
        info.mObjectSlot.mSlotIndex = 5;
        info.mObjectSlot.mMountPointIndex = 0;
        info.mObjectSlot.mStartX = 0;
        info.mObjectSlot.mStartY = 0;
        info.mObjectSlot.mSpanX = 1;
        info.mObjectSlot.mSpanY = 1;
        info.mIndex = 1;

        info.mIsNativeObject = false;
        info.mClassName = "com.borqs.se.widget3d.AppObject";
		try {
			final PackageManager packageManager = mContext.getPackageManager();
			final String pkgName = info.mComponentName.getPackageName();
			PackageInfo pinfo;
			pinfo = packageManager.getPackageInfo(pkgName, 0);
			info.mDisplayName = pinfo.applicationInfo.loadLabel(packageManager).toString();
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		info.saveToDB(db);
	}
	
	private void loadIconBoxModelInfo(SQLiteDatabase db) {
    	try {
    		InputStream is = mContext.getAssets().open("base/iconbox/models_config.xml");
    		XmlPullParser parser = Xml.newPullParser();
    		parser.setInput(is, "utf-8");
    		XmlUtils.beginDocument(parser, "config");
    		ModelInfo config = ModelInfo.CreateFromXml(parser);
    		config.removeImageInfoWithName("iconbox02_fhq.jpg@iconbox_basedata.cbf_0", db);
    		config.removeImageInfoWithName("iconbox01_fhq.jpg@iconbox_basedata.cbf_1", db);
    		config.removeImageInfoWithName("yinying.png@iconbox_basedata.cbf_2", db);
    		config.saveToDB(db);
    		is.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (XmlPullParserException e) {
    		e.printStackTrace();
    	}
    }
	
	private void loadMagnifierModelInfo(SQLiteDatabase db) {
    	try {
    		InputStream is = mContext.getAssets().open("base/fangdajing/models_config.xml");
    		XmlPullParser parser = Xml.newPullParser();
    		parser.setInput(is, "utf-8");
    		XmlUtils.beginDocument(parser, "config");
    		ModelInfo config = ModelInfo.CreateFromXml(parser);
    		config.saveToDB(db);
    		is.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (XmlPullParserException e) {
    		e.printStackTrace();
    	}
    }
	
    private void loadHome4MianModelInfo(SQLiteDatabase db) {
    	try {
    		InputStream is = mContext.getAssets().open("base/home4mian/models_config.xml");
    		XmlPullParser parser = Xml.newPullParser();
    		parser.setInput(is, "utf-8");
    		XmlUtils.beginDocument(parser, "config");
    		ModelInfo config = ModelInfo.CreateFromXml(parser);
    		config.saveToDB(db);
    		is.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (XmlPullParserException e) {
    		e.printStackTrace();
    	}
    }
    
    private void loadHome8MianShuModelInfo(SQLiteDatabase db) {
    	try {
    		InputStream is = mContext.getAssets().open("base/home8mianshu/models_config.xml");
    		XmlPullParser parser = Xml.newPullParser();
    		parser.setInput(is, "utf-8");
    		XmlUtils.beginDocument(parser, "config");
    		ModelInfo config = ModelInfo.CreateFromXml(parser);
    		config.saveToDB(db);
    		is.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (XmlPullParserException e) {
    		e.printStackTrace();
    	}
    }
    
    private void loadJiaziPortModelInfo(SQLiteDatabase db) {
    	try {
    		InputStream is = mContext.getAssets().open("base/jiaziport/models_config.xml");
    		XmlPullParser parser = Xml.newPullParser();
    		parser.setInput(is, "utf-8");
    		XmlUtils.beginDocument(parser, "config");
    		ModelInfo config = ModelInfo.CreateFromXml(parser);
    		config.saveToDB(db);
    		is.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (XmlPullParserException e) {
    		e.printStackTrace();
    	}
    }
    
    private void loadDeskFangPortMianShuModelInfo(SQLiteDatabase db) {
    	try {
    		InputStream is = mContext.getAssets().open("base/desk_fang_port/models_config.xml");
    		XmlPullParser parser = Xml.newPullParser();
    		parser.setInput(is, "utf-8");
    		XmlUtils.beginDocument(parser, "config");
    		ModelInfo config = ModelInfo.CreateFromXml(parser);
    		config.saveToDB(db);
    		is.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (XmlPullParserException e) {
    		e.printStackTrace();
    	}
    }

	public void upgradeDatabaseToVersion6(SQLiteDatabase db){
        db.beginTransaction();
        try {
            Cursor cursor = db.query(Tables.PLUGIN, new String[] {
                    PluginColumns.Name,PluginColumns.PRODUCT_ID,PluginColumns.VERSION_CODE,
                    PluginColumns.VERSION_NAME,PluginColumns.IS_APPLY,PluginColumns.TYPE},
                    null, null, null, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                while(cursor.moveToNext()) {
                    PlugInInfo info = new PlugInInfo();
                    info.name = cursor.getString(cursor.getColumnIndexOrThrow(PluginColumns.Name));
                    info.product_id = cursor.getString(cursor.getColumnIndexOrThrow(PluginColumns.PRODUCT_ID));
                    info.version_code = cursor.getInt(cursor.getColumnIndexOrThrow(PluginColumns.VERSION_CODE));
                    info.version_name = cursor.getString(cursor.getColumnIndexOrThrow(PluginColumns.VERSION_NAME));
                    info.type = cursor.getString(cursor.getColumnIndexOrThrow(PluginColumns.TYPE));
                    info.is_apply = cursor.getInt(cursor.getColumnIndexOrThrow(PluginColumns.PRODUCT_ID))==1?true:false;
                    MarketUtils.insertPlugIn(mContext, info);
                }
            }
            db.execSQL("DROP TABLE IF EXISTS " + Tables.PLUGIN);
            db.setTransactionSuccessful();
        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }

    }

	@Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropAllTables(db);
        dropAllTriggers(db);

        new Thread () {
            public void run(){
                Utils.deleteAll(mContext.getFilesDir().getAbsolutePath());
                Utils.deleteAll(HomeUtils.SDCARD_PATH);
            }
        }.start();
        onCreate(db);
    }

    private void dropAllTriggers(SQLiteDatabase db) {
        db.execSQL("DROP TRIGGER IF EXISTS objects_cleanup_delete;");
        db.execSQL("DROP TRIGGER IF EXISTS scenes_cleanup_delete;");
        db.execSQL("DROP TRIGGER IF EXISTS models_cleanup_delete;");
    }
    
    private void dropAllTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.SCENE_INFO);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.CAMERA_DATA);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.OBJECTS_INFO);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.MODEL_INFO);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.IMAGE_INFO);
//        db.execSQL("DROP TABLE IF EXISTS " + Tables.FILE_URL_INFO);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.THEME);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.VESSEL);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.PLUGIN);

    }

//    private void upgradeDatabaseToVersion15(SQLiteDatabase db) {
//        try {
//            db.execSQL("DROP TABLE IF EXISTS Objects_Config");
//            db.execSQL("DROP TABLE IF EXISTS Wall_Slot");
//            db.execSQL("DROP TABLE IF EXISTS " + ProviderUtils.Tables.DESK_SLOT);
//            db.execSQL("CREATE TABLE Scene_Config ("
//                    + "_id INTEGER PRIMARY KEY,"
//                    + "className TEXT NOT NULL,"
//                    + "sceneName TEXT NOT NULL,"
//                    + "wall_num INTEGER NOT NULL DEFAULT 12," 
//                    + "wall_spanX INTEGER NOT NULL DEFAULT 4,"
//                    + "wall_spanY INTEGER NOT NULL DEFAULT 4,"
//                    + "wall_sizeX FLOAT,"
//                    + "wall_sizeY FLOAT,"
//                    + "wall_height FLOAT NOT NULL DEFAULT 50,"
//                    + "wall_radius FLOAT NOT NULL DEFAULT 1000,"
//                    + "font_size FLOAT NOT NULL DEFAULT 18,"
//                    + "stroke_width FLOAT NOT NULL DEFAULT 8,"
//                    + "app_padding INTEGER NOT NULL DEFAULT 3,"
//                    + "desk_height FLOAT NOT NULL DEFAULT 100,"
//                    + "desk_radius FLOAT NOT NULL DEFAULT 140,"
//                    + "desk_num INTEGER NOT NULL DEFAULT 6"
//                    + ");");
//            db.execSQL("CREATE TABLE scene_camera ("
//                    + "camera_id INTEGER PRIMARY KEY,"
//                    + "_id INTEGER,"
//                    + "fov FLOAT NOT NULL DEFAULT 60,"
//                    + "near FLOAT NOT NULL DEFAULT 1," 
//                    + "far FLOAT NOT NULL DEFAULT 2000,"
//                    + "location TEXT NOT NULL,"
//                    + "zaxis TEXT NOT NULL,"
//                    + "up TEXT NOT NULL" + ");");
//
//            // ////////////////////////////////////////////////////////////////////////////////////////begin
//            db.execSQL("CREATE TABLE Objects_Config (" 
//                    + "_id INTEGER PRIMARY KEY,"
//                    + "name TEXT NOT NULL,"
//                    + "shaderType INTEGER NOT NULL DEFAULT 0,"
//                    + "sceneName TEXT,"
//                    + "rootName TEXT," 
//                    + "componentName TEXT,"
//                    + "showName TEXT,"
//                    + "sceneFile TEXT,"
//                    + "basedataFile TEXT,"
//                    + "assetsPath TEXT,"
//                    + "previewTrans TEXT,"
//                    + "slotType INTEGER NOT NULL DEFAULT 0,"
//                    + "status INTEGER NOT NULL DEFAULT 0,"
//                    + "viewLocation TEXT,"
//                    + "className TEXT,"
//                    + "shortcutIcon BLOB,"
//                    + "shortcutUri TEXT,"
//                    + "isNativeObj INTEGER NOT NULL DEFAULT 0,"
//                    + "widgetId INTEGER NOT NULL DEFAULT -1,"
//                    + "keyWords TEXT" + ");");
//
//            db.execSQL("CREATE TABLE Wall_Slot ("
//                    + "slot_id INTEGER PRIMARY KEY,"
//                    + "_id INTEGER,"
//                    + "slot_Index INTEGER NOT NULL DEFAULT 0," 
//                    + "slot_StartX INTEGER NOT NULL DEFAULT 0,"
//                    + "slot_StartY INTEGER NOT NULL DEFAULT 0,"
//                    + "slot_SpanX INTEGER NOT NULL DEFAULT 0," 
//                    + "slot_SpanY INTEGER NOT NULL DEFAULT 0"
//                    + ");");
//
//            db.execSQL("CREATE TABLE " + ProviderUtils.Tables.DESK_SLOT +" ("
//                    + "slot_id INTEGER PRIMARY KEY,"
//                    + "_id INTEGER," 
//                    + "D_slot_Index INTEGER NOT NULL DEFAULT -2" + ");");
//
//            // Trigger to remove data tied to an event when we delete that
//            // event.
//
//            String trigger = "DELETE FROM Wall_Slot WHERE _id=old._id;" 
//            + "DELETE FROM Desk_Slo WHERE _id=old._id;";
//            
//            db.execSQL("CREATE TRIGGER objects_cleanup_delete DELETE ON Objects_Config "
//                    + "BEGIN " + trigger + "END");
//            // Trigger to remove data tied to an event when we delete that
//            // event.
//            trigger = "DELETE FROM scene_camera WHERE _id=old._id;";
//            db.execSQL("CREATE TRIGGER scenes_cleanup_delete DELETE ON Scene_Config "
//                    + "BEGIN " + trigger + "END");
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion16(SQLiteDatabase db) {
//        try {
//            db.execSQL("DROP TRIGGER  objects_cleanup_delete;");
//            db.execSQL("DROP TRIGGER  scenes_cleanup_delete;");
//            db.execSQL("DROP TABLE IF EXISTS Objects_Config");
//            db.execSQL("DROP TABLE IF EXISTS Wall_Slot");
//            db.execSQL("DROP TABLE IF EXISTS " + ProviderUtils.Tables.DESK_SLOT);
//            db.execSQL("DROP TABLE IF EXISTS Scene_Config");
//            db.execSQL("DROP TABLE IF EXISTS scene_camera");
//            db.execSQL("CREATE TABLE Scene_Config ("
//                    + "_id INTEGER PRIMARY KEY,"
//                    + "className TEXT NOT NULL,"
//                    + "sceneName TEXT NOT NULL,"
//                    + "wall_num INTEGER NOT NULL DEFAULT 12," 
//                    + "wall_spanX INTEGER NOT NULL DEFAULT 4,"
//                    + "wall_spanY INTEGER NOT NULL DEFAULT 4,"
//                    + "wall_sizeX FLOAT,"
//                    + "wall_sizeY FLOAT,"
//                    + "wall_height FLOAT NOT NULL DEFAULT 50,"
//                    + "wall_radius FLOAT NOT NULL DEFAULT 1000,"
//                    + "font_size FLOAT NOT NULL DEFAULT 18,"
//                    + "stroke_width FLOAT NOT NULL DEFAULT 8,"
//                    + "app_padding INTEGER NOT NULL DEFAULT 3,"
//                    + "desk_height FLOAT NOT NULL DEFAULT 100,"
//                    + "desk_radius FLOAT NOT NULL DEFAULT 140,"
//                    + "desk_num INTEGER NOT NULL DEFAULT 6"
//                    + ");");
//            db.execSQL("CREATE TABLE scene_camera ("
//                    + "camera_id INTEGER PRIMARY KEY,"
//                    + "_id INTEGER,"
//                    + "fov FLOAT NOT NULL DEFAULT 60,"
//                    + "near FLOAT NOT NULL DEFAULT 1," 
//                    + "far FLOAT NOT NULL DEFAULT 2000,"
//                    + "location TEXT NOT NULL,"
//                    + "zaxis TEXT NOT NULL,"
//                    + "up TEXT NOT NULL" + ");");
//
//            // ////////////////////////////////////////////////////////////////////////////////////////begin
//            db.execSQL("CREATE TABLE Objects_Config (" 
//                    + "_id INTEGER PRIMARY KEY,"
//                    + "name TEXT NOT NULL,"
//                    + "shaderType INTEGER NOT NULL DEFAULT 0,"
//                    + "sceneName TEXT,"
//                    + "rootName TEXT," 
//                    + "componentName TEXT,"
//                    + "showName TEXT,"
//                    + "sceneFile TEXT,"
//                    + "basedataFile TEXT,"
//                    + "assetsPath TEXT,"
//                    + "previewTrans TEXT,"
//                    + "slotType INTEGER NOT NULL DEFAULT 0,"
//                    + "status INTEGER NOT NULL DEFAULT 0,"
//                    + "viewLocation TEXT,"
//                    + "className TEXT,"
//                    + "shortcutIcon BLOB,"
//                    + "shortcutUri TEXT,"
//                    + "isNativeObj INTEGER NOT NULL DEFAULT 0,"
//                    + "widgetId INTEGER NOT NULL DEFAULT -1,"
//                    + "keyWords TEXT" + ");");
//
//            db.execSQL("CREATE TABLE Wall_Slot ("
//                    + "slot_id INTEGER PRIMARY KEY,"
//                    + "_id INTEGER,"
//                    + "slot_Index INTEGER NOT NULL DEFAULT 0," 
//                    + "slot_StartX INTEGER NOT NULL DEFAULT 0,"
//                    + "slot_StartY INTEGER NOT NULL DEFAULT 0,"
//                    + "slot_SpanX INTEGER NOT NULL DEFAULT 0," 
//                    + "slot_SpanY INTEGER NOT NULL DEFAULT 0"
//                    + ");");
//
//            db.execSQL("CREATE TABLE " + ProviderUtils.Tables.DESK_SLOT + " ("
//                    + "slot_id INTEGER PRIMARY KEY,"
//                    + "_id INTEGER," 
//                    + "D_slot_Index INTEGER NOT NULL DEFAULT -2" + ");");
//
//            String trigger = "DELETE FROM Wall_Slot WHERE _id=old._id;" 
//            + "DELETE FROM Desk_Slo WHERE _id=old._id;";
//            
//            db.execSQL("CREATE TRIGGER objects_cleanup_delete DELETE ON Objects_Config "
//                    + "BEGIN " + trigger + "END");
//            // Trigger to remove data tied to an event when we delete that
//            // event.
//            trigger = "DELETE FROM scene_camera WHERE _id=old._id;";
//            db.execSQL("CREATE TRIGGER scenes_cleanup_delete DELETE ON Scene_Config "
//                    + "BEGIN " + trigger + "END");
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion17(SQLiteDatabase db) {
//        try {
//            db.execSQL("DROP TRIGGER  scenes_cleanup_delete;");
//            db.execSQL("DROP TRIGGER  objects_cleanup_delete;");
//            db.execSQL("DROP TABLE IF EXISTS Wall_Slot");
//            db.execSQL("DROP TABLE IF EXISTS Scene_Config");
//            db.execSQL("DROP TABLE IF EXISTS scene_camera");
//            db.execSQL("CREATE TABLE Scene_Config ("
//                    + "_id INTEGER PRIMARY KEY,"
//                    + "className TEXT NOT NULL,"
//                    + "sceneName TEXT NOT NULL,"
//                    + "wall_num INTEGER NOT NULL DEFAULT 12," 
//                    + "wall_spanX INTEGER NOT NULL DEFAULT 4,"
//                    + "wall_spanY INTEGER NOT NULL DEFAULT 4,"
//                    + "wall_sizeX FLOAT,"
//                    + "wall_sizeY FLOAT,"
//                    + "wall_height FLOAT NOT NULL DEFAULT 50,"
//                    + "wall_radius FLOAT NOT NULL DEFAULT 1000,"
//                    + "font_size FLOAT NOT NULL DEFAULT 18,"
//                    + "stroke_width FLOAT NOT NULL DEFAULT 8,"
//                    + "app_padding INTEGER NOT NULL DEFAULT 3,"
//                    + "desk_height FLOAT NOT NULL DEFAULT 100,"
//                    + "desk_radius FLOAT NOT NULL DEFAULT 140,"
//                    + "desk_num INTEGER NOT NULL DEFAULT 6"
//                    + ");");
//            db.execSQL("CREATE TABLE scene_camera ("
//                    + "camera_id INTEGER PRIMARY KEY,"
//                    + "_id INTEGER,"
//                    + "fov FLOAT NOT NULL DEFAULT 60,"
//                    + "near FLOAT NOT NULL DEFAULT 1," 
//                    + "far FLOAT NOT NULL DEFAULT 2000,"
//                    + "location TEXT NOT NULL,"
//                    + "zaxis TEXT NOT NULL,"
//                    + "up TEXT NOT NULL" + ");");
//            db.execSQL("CREATE TABLE Wall_Slot ("
//                    + "slot_id INTEGER PRIMARY KEY,"
//                    + "_id INTEGER,"
//                    + "slot_Index INTEGER NOT NULL DEFAULT 0," 
//                    + "slot_StartX INTEGER NOT NULL DEFAULT 0,"
//                    + "slot_StartY INTEGER NOT NULL DEFAULT 0,"
//                    + "slot_SpanX INTEGER NOT NULL DEFAULT 0," 
//                    + "slot_SpanY INTEGER NOT NULL DEFAULT 0,"
//                    + "slot_CenterX INTEGER NOT NULL DEFAULT 0,"
//                    + "slot_CenterY INTEGER NOT NULL DEFAULT 0,"
//                    + "SLOT_CenterZ INTEGER NOT NULL DEFAULT 0" + ");");
//            
//            String trigger = "DELETE FROM Wall_Slot WHERE _id=old._id;" 
//                    + "DELETE FROM Desk_Slo WHERE _id=old._id;";
//            db.execSQL("CREATE TRIGGER objects_cleanup_delete DELETE ON Objects_Config "
//                    + "BEGIN " + trigger + "END");
//            // Trigger to remove data tied to an event when we delete that
//            // event.
//            trigger = "DELETE FROM scene_camera WHERE _id=old._id;";
//            db.execSQL("CREATE TRIGGER scenes_cleanup_delete DELETE ON Scene_Config "
//                    + "BEGIN " + trigger + "END");
//            db.execSQL("DELETE FROM Objects_Config WHERE slotType!=2;");
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion18(SQLiteDatabase db) {
//        try {
//            db.execSQL("DROP TRIGGER  scenes_cleanup_delete;");
//            db.execSQL("DROP TABLE IF EXISTS Scene_Config");
//            db.execSQL("DROP TABLE IF EXISTS scene_camera");
//            db.execSQL("CREATE TABLE Scene_Config ("
//                    + "_id INTEGER PRIMARY KEY,"
//                    + "className TEXT NOT NULL,"
//                    + "sceneName TEXT NOT NULL,"
//                    + "wall_num INTEGER NOT NULL DEFAULT 12," 
//                    + "wall_spanX INTEGER NOT NULL DEFAULT 4,"
//                    + "wall_spanY INTEGER NOT NULL DEFAULT 4,"
//                    + "wall_sizeX FLOAT,"
//                    + "wall_sizeY FLOAT,"
//                    + "wall_height FLOAT NOT NULL DEFAULT 50,"
//                    + "wall_radius FLOAT NOT NULL DEFAULT 1000,"
//                    + "font_size FLOAT NOT NULL DEFAULT 18,"
//                    + "stroke_width FLOAT NOT NULL DEFAULT 8,"
//                    + "app_padding INTEGER NOT NULL DEFAULT 3,"
//                    + "desk_height FLOAT NOT NULL DEFAULT 100,"
//                    + "desk_radius FLOAT NOT NULL DEFAULT 140,"
//                    + "desk_num INTEGER NOT NULL DEFAULT 6,"
//                    + "width FLOAT NOT NULL DEFAULT 0,"
//                    + "height FLOAT NOT NULL DEFAULT 0" + ");");
//            db.execSQL("CREATE TABLE scene_camera ("
//                    + "camera_id INTEGER PRIMARY KEY,"
//                    + "_id INTEGER,"
//                    + "fov FLOAT NOT NULL DEFAULT 60,"
//                    + "near FLOAT NOT NULL DEFAULT 1," 
//                    + "far FLOAT NOT NULL DEFAULT 2000,"
//                    + "location TEXT NOT NULL,"
//                    + "zaxis TEXT NOT NULL,"
//                    + "up TEXT NOT NULL" + ");");
//            // Trigger to remove data tied to an event when we delete
//            // that event.
//            String trigger = "DELETE FROM scene_camera WHERE _id=old._id;";
//            db.execSQL("CREATE TRIGGER scenes_cleanup_delete DELETE ON Scene_Config "
//                    + "BEGIN " + trigger + "END");
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion19(SQLiteDatabase db) {
//        try {
//            db.execSQL("DROP TRIGGER  scenes_cleanup_delete;");
//            db.execSQL("DROP TABLE IF EXISTS Scene_Config");
//            db.execSQL("DROP TABLE IF EXISTS scene_camera");
//            db.execSQL("CREATE TABLE Scene_Config ("
//                    + "_id INTEGER PRIMARY KEY,"
//                    + "className TEXT NOT NULL,"
//                    + "sceneName TEXT NOT NULL,"
//                    + "wall_num INTEGER NOT NULL DEFAULT 12," 
//                    + "wall_spanX INTEGER NOT NULL DEFAULT 4,"
//                    + "wall_spanY INTEGER NOT NULL DEFAULT 4,"
//                    + "wall_sizeX FLOAT,"
//                    + "wall_sizeY FLOAT,"
//                    + "wall_height FLOAT NOT NULL DEFAULT 50,"
//                    + "wall_radius FLOAT NOT NULL DEFAULT 1000,"
//                    + "font_size FLOAT NOT NULL DEFAULT 18,"
//                    + "stroke_width FLOAT NOT NULL DEFAULT 8,"
//                    + "app_padding INTEGER NOT NULL DEFAULT 3,"
//                    + "desk_height FLOAT NOT NULL DEFAULT 100,"
//                    + "desk_radius FLOAT NOT NULL DEFAULT 140,"
//                    + "desk_num INTEGER NOT NULL DEFAULT 6"
//                    + ");");
//            db.execSQL("CREATE TABLE scene_camera ("
//                    + "camera_id INTEGER PRIMARY KEY,"
//                    + "_id INTEGER,"
//                    + "fov FLOAT NOT NULL DEFAULT 60,"
//                    + "near FLOAT NOT NULL DEFAULT 1," 
//                    + "far FLOAT NOT NULL DEFAULT 2000,"
//                    + "location TEXT NOT NULL,"
//                    + "zaxis TEXT NOT NULL,"
//                    + "up TEXT NOT NULL" + ");");
//            // Trigger to remove data tied to an event when we delete
//            // that event.
//            String trigger = "DELETE FROM scene_camera WHERE _id=old._id;";
//            db.execSQL("CREATE TRIGGER scenes_cleanup_delete DELETE ON Scene_Config " + "BEGIN " + trigger + "END");
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion20(SQLiteDatabase db) {
//        try {
//            db.execSQL("DELETE FROM Scene_Config WHERE name LIKE '%group_wallpicframe%'");
//            db.execSQL("ALTER TABLE Objects_Config ADD " + "childNames TEXT;");
//            db.execSQL("ALTER TABLE Objects_Config ADD " + "localTrans TEXT;");
//            db.execSQL("ALTER TABLE Scene_Config ADD " + "wall_angle FLOAT NOT NULL DEFAULT 0;");
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion21(SQLiteDatabase db) {
//        try {
//            db.execSQL("ALTER TABLE Objects_Config ADD " + "objectIndex FLOAT NOT NULL DEFAULT 0;");
//            db.execSQL("ALTER TABLE Objects_Config ADD " + "rootIndex FLOAT NOT NULL DEFAULT 0;");
//            String table = "Objects_Config";
//            String[] column = new String[] { "_id", "name" };
//            Cursor cursor = db.query(table, column, null, null, null, null, null);
//            if (cursor != null) {
//                while (cursor.moveToNext()) {
//                    int id = cursor.getInt(0);
//                    String name = cursor.getString(1);
//                    if (isClone(name)) {
//                        int objectIndex = getCloneIndex(name);
//                        String newName = getCloneParentName(name);
//                        String where = "_id=" + id;
//                        ContentValues values = new ContentValues();
//                        values.put("name", newName);
//                        values.put("objectIndex", objectIndex);
//                        db.update(table, values, where, null);
//                    }
//                }
//                cursor.close();
//            }
//
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private boolean isClone(String name) {
//        return name.contains("_clone_");
//    }
//
//    private int getCloneIndex(String name) {
//        int i = name.indexOf("_clone_");
//        String index = name.substring(i + 7);
//        return Integer.parseInt(index);
//    }
//
//    private String getCloneParentName(String name) {
//        int index = name.indexOf("_clone_");
//        if (index > 0) {
//            return name.substring(0, index);
//        }
//        return name;
//    }
//
//    private void upgradeDatabaseToVersion22(SQLiteDatabase db) {
//        try {
//            db.execSQL("CREATE TABLE model_info (" 
//                    + "_id INTEGER PRIMARY KEY," 
//                    + "name TEXT NOT NULL,"
//                    + "componentName TEXT,"
//                    + "type TEXT NOT NULL,"
//                    + "sceneFile TEXT," 
//                    + "basedataFile TEXT,"
//                    + "assetsPath TEXT," 
//                    + "previewTrans TEXT,"
//                    + "localTrans TEXT,"
//                    + "className TEXT,"
//                    + "sceneName TEXT,"
//                    + "keyWords TEXT,"
//                    + "childNames TEXT,"
//                    + "slotType INTEGER NOT NULL DEFAULT 0,"
//                    + "spanX INTEGER NOT NULL DEFAULT 0,"
//                    + "spanY INTEGER NOT NULL DEFAULT 0" + ");");
//
//            db.execSQL("ALTER TABLE Objects_Config ADD type TEXT;");
//            String table = "Objects_Config";
//            String where = "(status&2)=0";
//            db.delete(table, where, null);
//            String[] column = new String[] { "_id", "name", "isNativeObj" };
//            Cursor cursor = db.query(table, column, null, null, null, null, null);
//            if (cursor != null) {
//                while (cursor.moveToNext()) {
//                    int id = cursor.getInt(0);
//                    String name = cursor.getString(1);
//                    int isNativeObj = cursor.getInt(2);
//                    if (isNativeObj > 0) {
//                        int objectIndex = 0;
//                        if (name.equals("group_house") || name.equals("group_desk") || name.equals("group_pc")
//                                || name.equals("group_feiting") || name.equals("group_house8")) {
//                            objectIndex = 0;
//                        } else {
//                            objectIndex = 1;
//                        }
//                        where = "_id=" + id;
//                        ContentValues values = new ContentValues();
//                        values.put("objectIndex", objectIndex);
//                        db.update(table, values, where, null);
//                    }
//                }
//                cursor.close();
//            }
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion24(SQLiteDatabase db) {
//        try {
//            db.execSQL("ALTER TABLE " + ProviderUtils.Tables.MODEL_INFO + " ADD imageInfo TEXT;");
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion25(SQLiteDatabase db) {
//        try {
//            String where = ObjectInfoColumns.OBJECT_NAME + "='group_wallglobe'";
//            String table = ProviderUtils.Tables.OBJECTS_INFO;
//            db.delete(table, where, null);
//
//            where = ModelColumns.CLASS_NAME + "='com.borqs.se.home3d.AppObject'";
//            ContentValues values = new ContentValues();
//            values.put(ModelColumns.CLASS_NAME, AppObject.class.getName());
//            db.update(table, values, where, null);
//
//            where = ModelColumns.CLASS_NAME + "='com.borqs.se.home3d.ShortcutObject'";
//            values = new ContentValues();
//            values.put(ModelColumns.CLASS_NAME, ShortcutObject.class.getName());
//            db.update(table, values, where, null);
//
//            where = ModelColumns.CLASS_NAME + "='com.borqs.se.home3d.WidgetObject'";
//            values = new ContentValues();
//            values.put(ModelColumns.CLASS_NAME, WidgetObject.class.getName());
//            db.update(table, values, where, null);
//
//            where = ModelColumns.OBJECT_NAME + "='group_wallglobe'";
//            table = ProviderUtils.Tables.MODEL_INFO;
//            db.delete(table, where, null);
//
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion26(SQLiteDatabase db) {
//        try {
//            db.execSQL("DROP TRIGGER  scenes_cleanup_delete;");
//            db.execSQL("DROP TABLE IF EXISTS " + ProviderUtils.Tables.SCENE_INFO);
//            db.execSQL("DROP TABLE IF EXISTS " + ProviderUtils.Tables.CAMERA_DATA);
//            db.execSQL("CREATE TABLE " + ProviderUtils.Tables.SCENE_INFO + " (" + SceneInfoColumns._ID
//                    + " INTEGER PRIMARY KEY," + SceneInfoColumns.CLASS_NAME + " TEXT NOT NULL,"
//                    + SceneInfoColumns.SCENE_NAME + " TEXT NOT NULL," + SceneInfoColumns.WALL_NUM
//                    + " INTEGER NOT NULL DEFAULT 12," + SceneInfoColumns.WALL_SPANX + " INTEGER NOT NULL DEFAULT 4,"
//                    + SceneInfoColumns.WALL_SPANY + " INTEGER NOT NULL DEFAULT 4," + SceneInfoColumns.WALL_SIZEX
//                    + " FLOAT," + SceneInfoColumns.WALL_SIZEY + " FLOAT," + SceneInfoColumns.WALL_HEIGHT
//                    + " FLOAT NOT NULL DEFAULT 50," + SceneInfoColumns.WALL_RADIUS + " FLOAT NOT NULL DEFAULT 1000,"
//                    + SceneInfoColumns.SKY_RADIUS + " FLOAT NOT NULL DEFAULT 750," + SceneInfoColumns.DESK_HEIGHT
//                    + " FLOAT NOT NULL DEFAULT 100," + SceneInfoColumns.DESK_RADIUS + " FLOAT NOT NULL DEFAULT 140,"
//                    + SceneInfoColumns.DESK_NUM + " INTEGER NOT NULL DEFAULT 6," + SceneInfoColumns.WALL_ANGLE
//                    + " FLOAT NOT NULL DEFAULT 0" + ");");
//            db.execSQL("CREATE TABLE scene_camera ("
//                    + "camera_id INTEGER PRIMARY KEY,"
//                    + "_id INTEGER,"
//                    + "fov FLOAT NOT NULL DEFAULT 60,"
//                    + "near FLOAT NOT NULL DEFAULT 1," 
//                    + "far FLOAT NOT NULL DEFAULT 2000,"
//                    + "location TEXT NOT NULL,"
//                    + "zaxis TEXT NOT NULL,"
//                    + "up TEXT NOT NULL" + ");");
//            // Trigger to remove data tied to an event when we delete
//            // that event.
//           String trigger= "DELETE FROM scene_camera WHERE _id=old._id;";
//
//            db.execSQL("CREATE TRIGGER scenes_cleanup_delete DELETE ON " + ProviderUtils.Tables.SCENE_INFO + " "
//                    + "BEGIN " + trigger + "END");
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion27(SQLiteDatabase db) {
//        try {
//            db.execSQL("DROP TRIGGER  scenes_cleanup_delete;");
//            db.execSQL("DROP TABLE IF EXISTS " + ProviderUtils.Tables.SCENE_INFO);
//            db.execSQL("DROP TABLE IF EXISTS " + ProviderUtils.Tables.CAMERA_DATA);
//            db.execSQL("CREATE TABLE " + ProviderUtils.Tables.SCENE_INFO + " (" + SceneInfoColumns._ID
//                    + " INTEGER PRIMARY KEY," + SceneInfoColumns.CLASS_NAME + " TEXT NOT NULL,"
//                    + SceneInfoColumns.SCENE_NAME + " TEXT NOT NULL," + SceneInfoColumns.WALL_NUM
//                    + " INTEGER NOT NULL DEFAULT 12," + SceneInfoColumns.WALL_SPANX + " INTEGER NOT NULL DEFAULT 4,"
//                    + SceneInfoColumns.WALL_SPANY + " INTEGER NOT NULL DEFAULT 4," + SceneInfoColumns.WALL_SIZEX
//                    + " FLOAT," + SceneInfoColumns.WALL_SIZEY + " FLOAT," + SceneInfoColumns.WALL_HEIGHT
//                    + " FLOAT NOT NULL DEFAULT 50," + SceneInfoColumns.WALL_RADIUS + " FLOAT NOT NULL DEFAULT 1000,"
//                    + SceneInfoColumns.SKY_RADIUS + " FLOAT NOT NULL DEFAULT 750," + SceneInfoColumns.DESK_HEIGHT
//                    + " FLOAT NOT NULL DEFAULT 100," + SceneInfoColumns.DESK_RADIUS + " FLOAT NOT NULL DEFAULT 140,"
//                    + SceneInfoColumns.DESK_NUM + " INTEGER NOT NULL DEFAULT 6," + SceneInfoColumns.WALL_ANGLE
//                    + " FLOAT NOT NULL DEFAULT 0" + ");");
//            db.execSQL("CREATE TABLE scene_camera ("
//                    + "camera_id INTEGER PRIMARY KEY,"
//                    + "_id INTEGER,"
//                    + "fov FLOAT NOT NULL DEFAULT 60,"
//                    + "near FLOAT NOT NULL DEFAULT 1," 
//                    + "far FLOAT NOT NULL DEFAULT 2000,"
//                    + "location TEXT NOT NULL,"
//                    + "zaxis TEXT NOT NULL,"
//                    + "up TEXT NOT NULL" + ");");
//            // Trigger to remove data tied to an event when we delete
//            // that event.
//            String trigger = "DELETE FROM scene_camera WHERE _id=old._id;";
//            db.execSQL("CREATE TRIGGER scenes_cleanup_delete DELETE ON " + ProviderUtils.Tables.SCENE_INFO + " "
//                    + "BEGIN " + trigger + "END");
//
//            String where = ModelColumns.CLASS_NAME + "='com.borqs.se.widget3d.AppObject' or " + ModelColumns.CLASS_NAME
//                    + "='com.borqs.se.widget3d.ShortcutObject' or " + ModelColumns.CLASS_NAME
//                    + "='com.borqs.se.widget3d.WidgetObject'";
//            String table = ProviderUtils.Tables.OBJECTS_INFO;
//            db.delete(table, where, null);
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion30(SQLiteDatabase db) {
//        try {
//            db.execSQL("DROP TRIGGER  objects_cleanup_delete;");
//            db.execSQL("CREATE TABLE Wall_Gap_Slot (" + "slot_id INTEGER PRIMARY KEY," + "_id INTEGER,"
//                    + "G_slot_Index INTEGER NOT NULL DEFAULT -2" + ");");
//            // Trigger to remove data tied to an event when we delete that
//            // event.
//            String trigger = "DELETE FROM Wall_Slot WHERE _id=old._id;" + "DELETE FROM " + Tables.DESK_SLOT + " WHERE _id=old._id;"
//                    + "DELETE FROM Wall_Gap_Slot WHERE _id=old._id;";
//            db.execSQL("CREATE TRIGGER objects_cleanup_delete DELETE ON " + ProviderUtils.Tables.OBJECTS_INFO + " "
//                    + "BEGIN " + trigger + "END");
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion31(SQLiteDatabase db) {
//        try {
//            db.execSQL("CREATE TABLE " + ProviderUtils.Tables.IMAGE_INFO + " (" + "image_id INTEGER PRIMARY KEY,"
//                    + ModelColumns._ID + " INTEGER," + ModelColumns.THEME_NAME + " TEXT," + ModelColumns.IAMGE_NAME
//                    + " TEXT," + ModelColumns.IMAGE_PATH + " TEXT," + ModelColumns.IMAGE_NEW_PATH + " TEXT" + ");");
//            String trigger = "DELETE FROM image_info WHERE _id=old._id;";
//            db.execSQL("CREATE TRIGGER models_cleanup_delete DELETE ON " + ProviderUtils.Tables.MODEL_INFO + " "
//                    + "BEGIN " + trigger + "END");
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion33(SQLiteDatabase db) {
//        try {
//            String table = ProviderUtils.Tables.OBJECTS_INFO;
//            String where = ModelColumns.OBJECT_NAME + "='group_house8_3'";
//            db.delete(table, where, null);
//            table = ProviderUtils.Tables.MODEL_INFO;
//            where = ObjectInfoColumns.OBJECT_NAME + "='group_house8_3'";
//            db.delete(table, where, null);
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion35(SQLiteDatabase db) {
//        try {
//            String table = ProviderUtils.Tables.SCENE_INFO;
//            String where = SceneInfoColumns.SCENE_NAME + "='home8'";
//            ContentValues values = new ContentValues();
//            values.put(ModelColumns.CLASS_NAME, "com.borqs.se.home3d.SE3DHomeScene");
//            db.update(table, values, where, null);
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//    
//    private void upgradeDatabaseToVersion38(SQLiteDatabase db) {
//        try {
//            db.execSQL("DROP TABLE IF EXISTS " + ProviderUtils.Tables.FILE_URL_INFO);//for up grade and down grade
//            db.execSQL("CREATE TABLE " + ProviderUtils.Tables.FILE_URL_INFO + " (" + FileURLInfoColumns._ID
//                    + " INTEGER PRIMARY KEY,"
//                    + FileURLInfoColumns.NAME + " TEXT NOT NULL,"
//                    + FileURLInfoColumns.SERVER_PATH + " TEXT,"
//                    + FileURLInfoColumns.LOCAL_PATH + " TEXT,"
//                    + FileURLInfoColumns.TYPE + " INTEGER DEFAULT 0,"
//                    + FileURLInfoColumns.APPLY_STATUS + " INTEGER DEFAULT 0,"
//                    + FileURLInfoColumns.DOWNLOAD_STATUS + " INTEGER DEFAULT 0,"
//                    + FileURLInfoColumns.FILE_LENGTH + " LONG NOT NULL DEFAULT 0,"
//                    + FileURLInfoColumns.THREAD_INFOS + " TEXT, "
//                    + " UNIQUE (" + FileURLInfoColumns.NAME + ")" + ");");
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion39(SQLiteDatabase db) {
//        try {
//            String table = ProviderUtils.Tables.IMAGE_INFO;
//            String where = ModelColumns.IAMGE_NAME + "='camera.png@camera_basedata.cbf_0'";
//            ContentValues values = new ContentValues();
//            values.put(ModelColumns.IMAGE_PATH, "assets/base/camera/camera.jpg");
//            values.put(ModelColumns.IMAGE_NEW_PATH, "assets/base/camera/camera.jpg");
//            db.update(table, values, where, null);
//
//            where = ModelColumns.IAMGE_NAME + "='deskvray.png@desk_basedata.cbf'";
//            values = new ContentValues();
//            values.put(ModelColumns.IMAGE_PATH, "assets/base/desk/deskvray.jpg");
//            values.put(ModelColumns.IMAGE_NEW_PATH, "assets/base/desk/deskvray.jpg");
//            db.update(table, values, where, null);
//
//            where = ModelColumns.IAMGE_NAME + "='dituyiqiong.png@globe_basedata.cbf_0'";
//            values = new ContentValues();
//            values.put(ModelColumns.IMAGE_PATH, "assets/base/globe/dituyiqiong.jpg");
//            values.put(ModelColumns.IMAGE_NEW_PATH, "assets/base/globe/dituyiqiong.jpg");
//            db.update(table, values, where, null);
//
//            where = ModelColumns.IAMGE_NAME + "='diqiuyiyiqiong.png@globe_basedata.cbf_1'";
//            values = new ContentValues();
//            values.put(ModelColumns.IMAGE_PATH, "assets/base/globe/diqiuyiyiqiong.jpg");
//            values.put(ModelColumns.IMAGE_NEW_PATH, "assets/base/globe/diqiuyiyiqiong.jpg");
//            db.update(table, values, where, null);
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//    
//    private void upgradeDatabaseToVersion40(SQLiteDatabase db) {
//        try {
//            String table = Tables.OBJECTS_INFO;
//            String where = ObjectInfoColumns.OBJECT_NAME + "='group_floor'";
//            db.delete(table, where, null);
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void upgradeDatabaseToVersion41(SQLiteDatabase db) {
//        SettingsActivity.saveXMLVersion(mContext, 0);
//        
//    }
//
//    private void upgradeDatabaseToVersion42(SQLiteDatabase db) {
//        db.execSQL("DROP TABLE IF EXISTS " + ProviderUtils.Tables.THEME);
//        db.execSQL("CREATE TABLE " + ProviderUtils.Tables.THEME + " (" + ThemeColumns._ID
//                + " INTEGER PRIMARY KEY,"
//                + ThemeColumns.NAME + " TEXT NOT NULL,"
//                + ThemeColumns.FILE_PATH + " TEXT,"
//                + ThemeColumns.TYPE + " INTEGER DEFAULT 0,"
//                + ThemeColumns.IS_APPLY + " INTEGER DEFAULT 0,"
//                + ThemeColumns.CONFIG + " TEXT,"
//                + " UNIQUE (" + ThemeColumns.NAME + ")" + ");");
//
//        try {
//            populateThemeData(db);
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//
//    private void populateThemeData(SQLiteDatabase db) {
//        String currentTheme = SettingsActivity.getThemeName(mContext);
//
//        ContentValues wood = new ContentValues();
//        wood.put(ThemeColumns.NAME, "default");
//        wood.put(ThemeColumns.TYPE, Category.DEFAULT_THEME.ordinal());
//        wood.put(ThemeColumns.FILE_PATH, "base/home8");
//        if ("default".equals(currentTheme)) {
//            wood.put(ThemeColumns.IS_APPLY, 1);
//        } else {
//            wood.put(ThemeColumns.IS_APPLY, 0);
//        }
//        db.insert(Tables.THEME, null, wood);
//
//        ContentValues white = new ContentValues();
//        white.put(ThemeColumns.NAME, "white");
//        white.put(ThemeColumns.TYPE, Category.DEFAULT_THEME.ordinal());
//        white.put(ThemeColumns.FILE_PATH, "base/home8/newTexture");
//        white.put(ThemeColumns.CONFIG, "light:false");
//        if ("white".equals(currentTheme)) {
//            white.put(ThemeColumns.IS_APPLY, 1);
//        } else {
//            white.put(ThemeColumns.IS_APPLY, 0);
//        }
//        db.insert(Tables.THEME, null, white);
//
//        ContentValues dark = new ContentValues();
//        dark.put(ThemeColumns.NAME, "dark");
//        dark.put(ThemeColumns.TYPE, Category.DEFAULT_THEME.ordinal());
//        dark.put(ThemeColumns.FILE_PATH, "base/home8/darkTexture");
//        if ("dark".equals(currentTheme)) {
//            dark.put(ThemeColumns.IS_APPLY, 1);
//        } else {
//            dark.put(ThemeColumns.IS_APPLY, 0);
//        }
//        db.insert(Tables.THEME, null, dark);
//
//        Cursor cursor = null;
//        try {
//            String where = FileURLInfoColumns.TYPE + "=" + Category.THEME.ordinal() + " AND " + FileURLInfoColumns.DOWNLOAD_STATUS + "=1";
//            cursor = db.query(Tables.FILE_URL_INFO, null,where , null, null, null, null);
//            if (cursor != null) {
//                while (cursor.moveToNext()) {
//                    ContentValues values = new ContentValues();
//                    String localPath = cursor.getString(cursor.getColumnIndexOrThrow(FileURLInfoColumns.LOCAL_PATH));
//                    String[] paths = localPath.split("/");
//                    if (paths.length > 0) {
//                        String name = paths[paths.length - 1];
//                        if (name.contains(".zip")) {
//                            name = name.replace(".zip", "");
//                        }
//                        values.put(ThemeColumns.NAME, name);
//                        values.put(ThemeColumns.FILE_PATH, localPath);
//                        values.put(ThemeColumns.TYPE, cursor.getInt(cursor.getColumnIndexOrThrow(FileURLInfoColumns.TYPE)));
//                        if (name.equals(currentTheme)) {
//                            values.put(ThemeColumns.IS_APPLY, 1);
//                        } else {
//                            values.put(ThemeColumns.IS_APPLY, 0);
//                        }
//                        db.insert(Tables.THEME, null, values);
//                    }
//                }
//            }
//        }finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
//    }
//
//    private void upgradeDatabaseToVersion43(SQLiteDatabase db) {
//        Cursor cursor = null;
//        try {
//            String selection = BaseColumns._ID + "=1";
//            cursor = db.query(Tables.THEME, null, selection, null, null, null, null);
//            if (cursor == null || !cursor.moveToNext()) {
//                // To fix the bug of 59879..In original design, the upgradeDatabaseToVersion42 will catch
//                // the exception. So maybe create table "theme" failed but has upgraded db successful. 
//                //(now I has changed it. will create table first, 
//                // but we also need consider the users who has upgraded but create "theme" table failed.
//                db.execSQL("DROP TABLE IF EXISTS " + ProviderUtils.Tables.THEME);
//                db.execSQL("CREATE TABLE " + ProviderUtils.Tables.THEME + " (" + ThemeColumns._ID
//                        + " INTEGER PRIMARY KEY,"
//                        + ThemeColumns.NAME + " TEXT NOT NULL,"
//                        + ThemeColumns.FILE_PATH + " TEXT,"
//                        + ThemeColumns.TYPE + " INTEGER DEFAULT 0,"
//                        + ThemeColumns.IS_APPLY + " INTEGER DEFAULT 0,"
//                        + ThemeColumns.CONFIG + " TEXT,"
//                        + " UNIQUE (" + ThemeColumns.NAME + ")" + ");");
//                try {
//                    populateThemeData(db);
//                } catch (Throwable ex) {
//                    Log.e(TAG, ex.getMessage(), ex);
//                } finally {
//                   
//                }
//            }
//        } catch (Exception e) {
//            // To fix the bug of 59879..In original design, the upgradeDatabaseToVersion42 will catch
//            // the exception. So maybe create table "theme" failed but has upgraded db successful. 
//            //(now I has changed it. will create table first, 
//            // but we also need consider the users who has upgraded but create "theme" table failed.
//            db.execSQL("DROP TABLE IF EXISTS " + ProviderUtils.Tables.THEME);
//            db.execSQL("CREATE TABLE " + ProviderUtils.Tables.THEME + " (" + ThemeColumns._ID
//                    + " INTEGER PRIMARY KEY,"
//                    + ThemeColumns.NAME + " TEXT NOT NULL,"
//                    + ThemeColumns.FILE_PATH + " TEXT,"
//                    + ThemeColumns.TYPE + " INTEGER DEFAULT 0,"
//                    + ThemeColumns.IS_APPLY + " INTEGER DEFAULT 0,"
//                    + ThemeColumns.CONFIG + " TEXT,"
//                    + " UNIQUE (" + ThemeColumns.NAME + ")" + ");");
//
//            try {
//                populateThemeData(db);
//            } catch (Throwable ex) {
//                Log.e(TAG, ex.getMessage(), ex);
//            } finally {
//            }
//        } finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
//    }
//
//    private void upgradeDatabaseToVersion44(SQLiteDatabase db) {
//        try{
//            //To support pre-loaded, airships and airplane records need to be removed from Object_Config table
//            String where = ObjectInfoColumns.OBJECT_NAME + "='group_feiting' or " + ObjectInfoColumns.OBJECT_NAME + "='group_airplane'";
//            db.delete(ProviderUtils.Tables.OBJECTS_INFO, where, null);
//        } catch(Throwable ex){
//            Log.e(TAG, ex.getMessage(), ex);
//        } finally {
//        }
//    }
//    private void upgradeDatabaseToVersion45(SQLiteDatabase db) {
//        XMLVersionUtils.upgradeXMLToVersion44(mContext, db);
//    }
//
//    private void upgradeDatabaseToVersion46(SQLiteDatabase db) {
//        SettingsActivity.saveXMLVersion(mContext, 0);
//        String where = "name='wall_group'";
//        String table = "Objects_Config";
//        db.delete(table, where, null);
//        where = "name='desk_group'";
//        db.delete(table, where, null);
//        ArrayList<XMLVersionUtils.OldObjectInfo> oldObjInfos = new ArrayList<XMLVersionUtils.OldObjectInfo>();
//        table = "Objects_Config LEFT JOIN Wall_Slot USING(_id)"
//                + " LEFT JOIN Desk_Slot USING(_id) LEFT JOIN Wall_Gap_Slot USING(_id)";
//        Cursor cursor = db.query(table, null, null, null, null, null, null);
//        if (cursor != null) {
//            while (cursor.moveToNext()) {
//                XMLVersionUtils.OldObjectInfo objInfo = XMLVersionUtils.OldObjectInfo.CreateFromDB(cursor);
//                if ("group_desk".equals(objInfo.mName)) {
//                    objInfo.mRootName = "home_root";
//                } else if ("group_house8".equals(objInfo.mName)) {
//                    objInfo.mRootName = "home_root";
//                } else if ("wall_group".equals(objInfo.mRootName)) {
//                    objInfo.mRootName = "group_house8";
//                } else if ("desk_group".equals(objInfo.mRootName)) {
//                    objInfo.mRootName = "group_desk";
//                }
//                oldObjInfos.add(objInfo);
//            }
//            cursor.close();
//        }
//        db.execSQL("DROP TRIGGER  objects_cleanup_delete;");
//        db.execSQL("DROP TABLE IF EXISTS Desk_Slot");
//        db.execSQL("DROP TABLE IF EXISTS Wall_Slot");
//        db.execSQL("DROP TABLE IF EXISTS Wall_Gap_Slot");
//        db.execSQL("DROP TABLE IF EXISTS Vessel");
//        db.execSQL("CREATE TABLE " + ProviderUtils.Tables.VESSEL + " (" 
//                + VesselColumns.VESSEL_ID + " INTEGER,"
//                + VesselColumns.OBJECT_ID + " INTEGER,"
//                + VesselColumns.SLOT_INDEX + " INTEGER NOT NULL DEFAULT 0," 
//                + VesselColumns.SLOT_StartX + " INTEGER NOT NULL DEFAULT 0,"
//                + VesselColumns.SLOT_StartY + " INTEGER NOT NULL DEFAULT 0," 
//                + VesselColumns.SLOT_SpanX + " INTEGER NOT NULL DEFAULT 0," 
//                + VesselColumns.SLOT_SpanY + " INTEGER NOT NULL DEFAULT 0"
//                + ");");
//        // Trigger to remove data tied to an event when we delete that event.
//        String trigger = "DELETE FROM " + Tables.VESSEL + " WHERE objectID=old._id;";
//        db.execSQL("CREATE TRIGGER objects_cleanup_delete DELETE ON " + ProviderUtils.Tables.OBJECTS_INFO + " "
//                + "BEGIN " + trigger + "END");
//
//        for (XMLVersionUtils.OldObjectInfo oldInfo : oldObjInfos) {
//            ContentValues values = new ContentValues();
//            values.put(VesselColumns.OBJECT_ID, oldInfo.mID);
//            values.put(VesselColumns.SLOT_INDEX, oldInfo.mObjectSlot.mSlotIndex);
//            values.put(VesselColumns.SLOT_StartX, oldInfo.mObjectSlot.mStartX);
//            values.put(VesselColumns.SLOT_StartY, oldInfo.mObjectSlot.mStartY);
//            values.put(VesselColumns.SLOT_SpanX, oldInfo.mObjectSlot.mSpanX);
//            values.put(VesselColumns.SLOT_SpanY, oldInfo.mObjectSlot.mSpanY);
//            if (oldInfo.mRootName != null) {
//                for (XMLVersionUtils.OldObjectInfo oldInfo1 : oldObjInfos) {
//                    if (oldInfo.mRootName.equals(oldInfo1.mName) && oldInfo.mRootIndex == oldInfo1.mIndex) {
//                        values.put(VesselColumns.VESSEL_ID, oldInfo1.mID);
//                        break;
//                    }
//                }
//            } else {
//                values.put(VesselColumns.VESSEL_ID, -1);
//            }
//            table = Tables.VESSEL;
//            db.insert(table, null, values);
//        }
//        
//        table = "model_info";
//        where = "name='group_desk'";
//        ContentValues values = new ContentValues();
//        values.put(ModelColumns.LOCAL_TRANS, "0,0,0,0,0,0,1,1,1,1");
//        db.update(table, values, where, null);
//        String[] columns = { "_id" };
//        cursor = db.query(table, columns, where, null, null, null, null);
//        if (cursor != null) {
//            if (cursor.moveToFirst()) {
//                int id = cursor.getInt(0);
//                table = Tables.IMAGE_INFO;
//                values = new ContentValues();
//                values.put(ModelColumns._ID, id);
//                values.put(ModelColumns.THEME_NAME, "default");
//                values.put(ModelColumns.IMAGE_PATH, "assets/base/desk/long.png");
//                values.put(ModelColumns.IMAGE_NEW_PATH, "assets/base/desk/long.png");
//                values.put(ModelColumns.IAMGE_NAME, "long.png@desk_basedata.cbf_0");
//                db.insert(table, null, values);
//            }
//            cursor.close();
//        }
//        
//        table = "Scene_Config";
//        where = "sceneName='home8'";
//        values = new ContentValues();
//        values.put(SceneInfoColumns.WALL_SIZEX, 196.0f);
//        values.put(SceneInfoColumns.WALL_SIZEY, 242.0f);
//        values.put(SceneInfoColumns.WALL_HEIGHT, 45.0f);
//        db.update(table, values, where, null);
//    }
//
//    private void upgradeDatabaseToVersion47(SQLiteDatabase db) {
//        String where = "className='com.borqs.se.widget3d.WallObject'"
//                + " OR className='com.borqs.se.widget3d.DesktopObject'"
//                + " OR className='com.borqs.se.widget3d.WallGapObject'";
//        String table = "model_info";
//        ContentValues values = new ContentValues();
//        values.put(ModelColumns.CLASS_NAME, "com.borqs.se.widget3d.NormalObject");
//        db.update(table, values, where, null);
//        where = "className='com.borqs.se.widget3d.AppObject'"
//                + " OR className='com.borqs.se.widget3d.ShortcutObject'";
//        table = "Objects_Config";
//        values = new ContentValues();
//        values.put(ObjectInfoColumns.SLOT_TYPE, ObjectInfo.SLOT_TYPE_APP_WALL);
//        db.update(table, values, where, null);
//        
//        where = "name='group_appwall'";
//        table = "Objects_Config";
//        String[] columns = {"_id"};
//        Cursor cursor = db.query(table, columns, where, null, null, null, null);
//        if (cursor != null) {
//            while (cursor.moveToNext()) {
//                int id = cursor.getInt(0);
//                where = "vesselID=" + id;
//                table = "Vessel";
//                String[] vesselColumns = { "objectID", "slot_Index"};
//                Cursor vesselCursor = db.query(table, vesselColumns, where, null, null, null, null);
//                if (vesselCursor != null) {
//                    while (vesselCursor.moveToNext()) {
//                        int objectID = vesselCursor.getInt(0);
//                        int slotIndex = vesselCursor.getInt(1);
//                        int startX = slotIndex % 2;
//                        int startY = slotIndex / 2;
//                        where = "objectID=" + objectID;
//                        table = "Vessel";
//                        values = new ContentValues();
//                        values.put(VesselColumns.SLOT_StartX, startX);
//                        values.put(VesselColumns.SLOT_StartY, startY);
//                        db.update(table, values, where, null);
//                    }
//                    vesselCursor.close();
//                }
//
//            }
//            cursor.close();
//        }
//        
//        where = "name='group_appwallheng'";
//        table = "Objects_Config";
//        cursor = db.query(table, columns, where, null, null, null, null);
//        if (cursor != null) {
//            while (cursor.moveToNext()) {
//                int id = cursor.getInt(0);
//                where = "vesselID=" + id;
//                table = "Vessel";
//                String[] vesselColumns = { "objectID", "slot_Index"};
//                Cursor vesselCursor = db.query(table, vesselColumns, where, null, null, null, null);
//                if (vesselCursor != null) {
//                    while (vesselCursor.moveToNext()) {
//                        int objectID = vesselCursor.getInt(0);
//                        int slotIndex = vesselCursor.getInt(1);
//                        int startX = slotIndex % 4;
//                        int startY = slotIndex / 4;
//                        where = "objectID=" + objectID;
//                        table = "Vessel";
//                        values = new ContentValues();
//                        values.put(VesselColumns.SLOT_StartX, startX);
//                        values.put(VesselColumns.SLOT_StartY, startY);
//                        db.update(table, values, where, null);
//                    }
//                    vesselCursor.close();
//                }
//
//            }
//            cursor.close();
//        }
//    }
//
//    private void upgradeDatabaseToVersion48(SQLiteDatabase db) {
//        String table = "model_info";
//        String where = "name='group_appwall'";
//        ContentValues values = new ContentValues();
//        values.put(ModelColumns.ASSETS_PATH, "assets/base/appwall/appwall4");
//        db.update(table, values, where, null);
//        
//        String[] columns = {"_id"};
//        Cursor cursor = db.query(table, columns, where, null, null, null, null);
//        if (cursor != null) {
//            while (cursor.moveToNext()) {
//                int id = cursor.getInt(0);
//                where = "_id=" + id + " AND imageName='home_appwall04_zch.png@appwall4_basedata.cbf'";
//                table = "image_info";
//                values = new ContentValues();
//                values.put(ModelColumns.IMAGE_PATH, "assets/base/appwall/home_appwall04_zch.png");
//                values.put(ModelColumns.IMAGE_NEW_PATH, "assets/base/appwall/home_appwall04_zch.png");
//                db.update(table, values, where, null);
//                
//                where = "_id=" + id + " AND imageName='home_appwall02_zch.png@appwall4_basedata.cbf'";
//                values = new ContentValues();
//                values.put(ModelColumns.IMAGE_PATH, "assets/base/appwall/home_appwall02_zch.png");
//                values.put(ModelColumns.IMAGE_NEW_PATH, "assets/base/appwall/home_appwall02_zch.png");
//                db.update(table, values, where, null);
//            }
//            cursor.close();
//        }
//        
//        table = "model_info";
//        where = "name='group_appwallheng'";
//        values = new ContentValues();
//        values.put(ModelColumns.ASSETS_PATH, "assets/base/appwall/appwallheng");
//        db.update(table, values, where, null);
//        
//        cursor = db.query(table, columns, where, null, null, null, null);
//        if (cursor != null) {
//            while (cursor.moveToNext()) {
//                int id = cursor.getInt(0);
//                where = "_id=" + id + " AND imageName='home_appwall02_zch.png@appwallheng_basedata.cbf'";
//                table = "image_info";
//                values = new ContentValues();
//                values.put(ModelColumns.IMAGE_PATH, "assets/base/appwall/home_appwall02_zch.png");
//                values.put(ModelColumns.IMAGE_NEW_PATH, "assets/base/appwall/home_appwall02_zch.png");
//                db.update(table, values, where, null);
//                
//                where = "_id=" + id + " AND imageName='home_appwall04_zch.png@appwallheng_basedata.cbf'";
//                values = new ContentValues();
//                values.put(ModelColumns.IMAGE_PATH, "assets/base/appwall/home_appwall04_zch.png");
//                values.put(ModelColumns.IMAGE_NEW_PATH, "assets/base/appwall/home_appwall04_zch.png");
//                db.update(table, values, where, null);
//            }
//            cursor.close();
//        }
//        
//        table = "model_info";
//        where = "name='group_email'";
//        cursor = db.query(table, columns, where, null, null, null, null);
//        if (cursor != null) {
//            while (cursor.moveToNext()) {
//                int id = cursor.getInt(0);
//                table = "image_info";
//                values = new ContentValues();
//                values.put(ModelColumns._ID, id);
//                values.put(ModelColumns.THEME_NAME, "default");
//                values.put(ModelColumns.IAMGE_NAME, "xinxiang.jpg@email_basedata.cbf_0");
//                values.put(ModelColumns.IMAGE_PATH, "assets/base/email/xinxiang.jpg");
//                values.put(ModelColumns.IMAGE_NEW_PATH, "assets/base/email/xinxiang.jpg");
//                db.insert(table, null, values);
//                
//                values = new ContentValues();
//                values.put(ModelColumns._ID, id);
//                values.put(ModelColumns.THEME_NAME, "default");
//                values.put(ModelColumns.IAMGE_NAME, "xinxiang.jpg@email_basedata.cbf_1");
//                values.put(ModelColumns.IMAGE_PATH, "assets/base/email/xinxiang.jpg");
//                values.put(ModelColumns.IMAGE_NEW_PATH, "assets/base/email/xinxiang.jpg");
//                db.insert(table, null, values);
//                
//                values = new ContentValues();
//                values.put(ModelColumns._ID, id);
//                values.put(ModelColumns.THEME_NAME, "default");
//                values.put(ModelColumns.IAMGE_NAME, "xinxiang.jpg@email_basedata.cbf_2");
//                values.put(ModelColumns.IMAGE_PATH, "assets/base/email/xinxiang.jpg");
//                values.put(ModelColumns.IMAGE_NEW_PATH, "assets/base/email/xinxiang.jpg");
//                db.insert(table, null, values);
//                
//                values = new ContentValues();
//                values.put(ModelColumns._ID, id);
//                values.put(ModelColumns.THEME_NAME, "default");
//                values.put(ModelColumns.IAMGE_NAME, "xinxiang.jpg@email_basedata.cbf_3");
//                values.put(ModelColumns.IMAGE_PATH, "assets/base/email/xinxiang.jpg");
//                values.put(ModelColumns.IMAGE_NEW_PATH, "assets/base/email/xinxiang.jpg");
//                db.insert(table, null, values);
//            }
//            cursor.close();
//        }
//    }
//
//    private void upgradeDatabaseToVersion49(SQLiteDatabase db) {
//        List<Vessel> vessels = new ArrayList<Vessel>();
//        String table = "Vessel";
//        String[] columns = { "objectID", "vesselID" };
//        Cursor cursor = db.query(table, columns, null, null, null, null, null);
//        if (cursor != null) {
//            while (cursor.moveToNext()) {
//                int objectID = cursor.getInt(0);
//                int vesselID = cursor.getInt(1);
//                Vessel vessel = null;
//                for (Vessel v : vessels) {
//                    if (v.mVessel == vesselID) {
//                        vessel = v;
//                        break;
//                    }
//                }
//                if (vessel == null) {
//                    vessel = new Vessel();
//                    vessel.mVessel = vesselID;
//                }
//                vessel.mObjects.add(objectID);
//            }
//            cursor.close();
//        }
//        table = "Objects_Config";
//        for (Vessel v : vessels) {
//            String where = "_id=" + v.mVessel;
//            cursor = db.query(table, null, where, null, null, null, null);
//            if (cursor == null || cursor.getCount() == 0) {
//                for (int objectID : v.mObjects) {
//                    where = "_id=" + objectID + " AND name!='home_root'";
//                    db.delete(table, where, null);
//                }
//            }
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
//    }
//
//    private class Vessel {
//        public int mVessel;
//        public List<Integer> mObjects = new ArrayList<Integer>();
//    }
//
//    private void upgradeDatabaseToVersion50(SQLiteDatabase db) {
//        String table = "Objects_Config";
//        String where = "className='com.borqs.se.widget3d.AppObject'";
//        ContentValues values = new ContentValues();
//        values.put("type", "App");
//        db.update(table, values, where, null);
//        values = new ContentValues();
//        values.put("type", "Shortcut");
//        where = "className='com.borqs.se.widget3d.ShortcutObject'";
//        db.update(table, values, where, null);
//        values = new ContentValues();
//        values.put("type", "Widget");
//        where = "className='com.borqs.se.widget3d.WidgetObject'";
//        db.update(table, values, where, null);        
//        addShadowToShortcut(db);
//    }
//
//    private void upgradeDatabaseToVersion52(SQLiteDatabase db) {
//        db.execSQL("ALTER TABLE " + ProviderUtils.Tables.OBJECTS_INFO + " ADD display_name TEXT;");
//        String where = "name='group_desk'";
//        ContentValues values = new ContentValues();
//        values.put(ModelColumns.BASEDATA_FILE, "newdesk_basedata.cbf");
//        values.put(ModelColumns.SCENE_FILE, "newdesk_scene.cbf");
//        db.update(Tables.MODEL_INFO, values, where, null);
//
//        values.clear();
//        where ="imageName ='deskvray.png@desk_basedata.cbf'";
//        values.put(ModelColumns.IAMGE_NAME, "object_table_fhq.png@newdesk_basedata.cbf");
//        values.put(ModelColumns.IMAGE_PATH, "assets/base/desk/object_table_fhq.png");
//        values.put(ModelColumns.IMAGE_NEW_PATH, "assets/base/desk/object_table_fhq.png");
//        db.update(Tables.IMAGE_INFO, values, where, null);
//
//        where ="imageName ='long.png@desk_basedata.cbf'";
//        values.put(ModelColumns.IAMGE_NAME, "object_table_fhq.png@newdesk_basedata.cbf_0");
//        db.update(Tables.IMAGE_INFO, values, where, null);
//
//        where ="imageName ='long.png@desk_basedata.cbf_0'";
//        values.put(ModelColumns.IAMGE_NAME, "object_table_fhq.png@newdesk_basedata.cbf_1");
//        db.update(Tables.IMAGE_INFO, values, where, null);
//    }

//    public void loadDefaultData(SQLiteDatabase db, int oldVersion) {
//        String[] columns = { ObjectInfoColumns.OBJECT_ID };
//        Cursor cursor = db.query(Tables.OBJECTS_INFO, columns, null, null, null, null, null);
//        if (cursor != null) {
//            if (cursor.getCount() > 0) {
//                if (oldVersion <= ProviderUtils.RELEASE_DATABASE_VERSION) {
//                    loadSceneInfo(db);
//                    loadModelInfo(db, false);
//                    loadObjectInfo(db);
//                } else {
//                    loadModelInfo(db, true);
//                }
//            } else {
//                loadDefaultData(db);
//            }
//            cursor.close();
//        }
//    }

    private void loadDefaultData(SQLiteDatabase db) {
        loadSceneInfo(db);
        loadModelInfo(db, false);
        loadObjectInfo(db);
    }

    private void loadSceneInfo(SQLiteDatabase db) {
    	XmlPullParser parser ;
    	if(SettingsActivity.getPreferRotation(mContext) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
    		parser = mContext.getResources().getXml(R.xml.octa_scene);
		}else {
			parser = mContext.getResources().getXml(R.xml.octa_scene_port);
		}
    	
    	if (null != parser) {
    		try {
				XmlUtils.beginDocument(parser, "scene");
				SESceneInfo config = SESceneInfo.CreateFromXml(parser);
				config.saveToDB(db);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
    }

    private void loadModelInfo(SQLiteDatabase db, boolean check) {
        Iterator<Entry<String, String>> iter = MODEL_XMLS.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            String name = entry.getKey();
            String path = entry.getValue();
            if (check) {
                String where = ModelColumns.OBJECT_NAME + "='" + name + "'";
                String[] columns = { ObjectInfoColumns.OBJECT_ID };
                Cursor cursor = db.query(Tables.MODEL_INFO, columns, where, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        cursor.close();
                        continue;
                    }
                    cursor.close();
                }
            }
            try {
                InputStream is = mContext.getAssets().open(path);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(is, "utf-8");
                XmlUtils.beginDocument(parser, "config");
                ModelInfo config = ModelInfo.CreateFromXml(parser);
                config.saveToDB(db);
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadObjectInfo(SQLiteDatabase db) {
        try {
        	XmlPullParser parser ;
        	if(SettingsActivity.getPreferRotation(mContext) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
        		String selection = ObjectInfoColumns.ORIENTATION_STATUS + " = " + ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        		Cursor cursor = db.query(Tables.OBJECTS_INFO, null, selection, null, null, null, null);
        		if(cursor != null && cursor.getCount() > 0) {
        			return ;
        		}
        		parser = mContext.getResources().getXml(R.xml.objects_config);
    		}else {
    			String selection = ObjectInfoColumns.ORIENTATION_STATUS + " != " + ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        		Cursor cursor = db.query(Tables.OBJECTS_INFO, null, selection, null, null, null, null);
        		if(cursor != null && cursor.getCount() > 0) {
        			return ;
        		}
    			parser = mContext.getResources().getXml(R.xml.objects_config_port);
    		}
            XmlUtils.beginDocument(parser, "configs");
            while (true) {
                XmlUtils.nextElement(parser);
                String name = parser.getName();
                if (!"config".equals(name)) {
                    break;
                }
                ObjectInfo config = ObjectInfo.CreateFromXml(parser);
                if (ModelInfo.Type.APP_ICON.equalsIgnoreCase(config.mName)) {
                	if (null == config.mComponentName) {
                		Log.w(TAG, "loadObjectInfo, App componentName is null");
                		return;
                	}
                	
                	final PackageManager packageManager = mContext.getPackageManager();
                	final String pkgName = config.mComponentName.getPackageName();
                	try {
                        if(pkgName != null && pkgName.equals(mContext.getPackageName())) {
                        	Log.d(TAG, "is search object.");
                        }else {
                        	final Intent intent =packageManager.getLaunchIntentForPackage(pkgName);
                        	config.mComponentName = intent.resolveActivity(packageManager);
                        }

                		PackageInfo info = packageManager.getPackageInfo(pkgName, 0);

                		config.mDisplayName = info.applicationInfo.loadLabel(packageManager).toString();
                		config.mClassName = "com.borqs.se.widget3d.AppObject";
                	} catch (Exception ne) {
                		Log.d(TAG, "loadObjectInfo, failed to query app " + config + " " + pkgName);
                		if(HomeUtils.LOCKSCREEN_HOMEHD_PKG.equals(pkgName)) {
                			config.mClassName = "com.borqs.se.widget3d.AppObject";
                			config.mName = AppObject.generateAppName();
                			config.mType = ModelInfo.Type.APP_ICON;
                			config.saveToDB(db);
                		}
                		ne.printStackTrace();
                        continue ;
                	}
                	config.mName = AppObject.generateAppName();
                	config.mType = ModelInfo.Type.APP_ICON;
                }

                config.saveToDB(db);

            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private void addShadowToShortcut(SQLiteDatabase db) {
//        String table = "Objects_Config";
//        String where = "className='com.borqs.se.widget3d.ShortcutObject'";
//        String[] columns = { "_id", "shortcutIcon" };
//        Cursor cursor = db.query(table, columns, where, null, null, null, null);
//        if (cursor != null) {
//            while(cursor.moveToNext()) {
//                int id = cursor.getInt(0);
//                byte[] bytes = cursor.getBlob(1);
//                if (bytes != null) {
//                    Bitmap icon= BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
//                    Bitmap des = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), Bitmap.Config.ARGB_4444);
//                    Canvas canvas = new Canvas(des);
//                    if (AppItemInfo.getShadow() != null) {
//                        canvas.drawBitmap(AppItemInfo.getShadow(), 3, icon.getHeight() * 0.25f, new Paint());
//                    }
//                    canvas.drawBitmap(icon, 0, 0, new Paint());
//                    ContentValues values = new ContentValues();
//                    HomeUtils.writeBitmap(values, des);
//                    db.update(table, values, "_id=" + id, null);
//                    des.recycle();
//                    icon.recycle();
//                }
//            }
//            cursor.close();
//        }
//    }
//
//    private void upgradeDatabaseToAddNewModel(SQLiteDatabase db) {
//        // do nothing, we need reload the db to add the new model.
//    }

	public void reloadCameraSceneData(SQLiteDatabase db) {
		SESceneInfo.removeFromDB(db);
		loadSceneInfo(db);
	}
}
