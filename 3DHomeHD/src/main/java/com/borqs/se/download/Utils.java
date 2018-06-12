package com.borqs.se.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.borqs.market.db.DownloadInfo;
import com.borqs.market.json.Product;
import com.borqs.market.utils.MarketUtils;
import com.borqs.market.wallpaper.RawPaperItem;
import com.borqs.market.wallpaper.WallpaperUtils;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.home3d.XmlUtils;
import com.borqs.se.download.GridFragment.Category;
import com.borqs.se.engine.SECommand;
import com.borqs.se.home3d.BlankActivity;
import com.borqs.se.home3d.HomeDataBaseHelper;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.ModelInfo.ImageInfo;
import com.borqs.se.home3d.ModelInfo.ImageItem;
import com.borqs.se.home3d.ProviderUtils.ModelColumns;
import com.borqs.se.home3d.ProviderUtils.Tables;
import com.borqs.se.home3d.ProviderUtils.ThemeColumns;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SESceneInfo;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.home3d.ProviderUtils.ImageInfoColumns;

public class Utils {
    private static final String TAG = "Utils";

    public static String converPercentage(float percentage) {
        DecimalFormat format = new DecimalFormat("##%");
        return format.format(percentage);
    }

    public static float percentage(long total, long complete) {
        float result = 0;
        if (total > 0 && complete > 0) {
            if (complete > total) {
                complete = total;
            }
            result = (float) complete / (float) total;
        }
        return result;
    }

    public static Bitmap getLocalDrawable(String imagePath) {
        FileInputStream fis =  null;
        try {
            fis = new FileInputStream(imagePath);
            return BitmapFactory.decodeStream(fis);
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public static Bitmap getAssetDrawable(Context context, String imagePath) {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(imagePath);
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            return null;
        } finally {
            if (inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public static void deleteAll(String path) {
        File f = new File(path);
        if (!f.exists())
            return;
        boolean rslt = true;
        if  (!(rslt = f.delete())) {
            File subs[] = f.listFiles();
            for (int i = 0; i <= subs.length - 1; i++) {
                if (subs[i].isDirectory())
                    deleteAll(subs[i].getAbsolutePath());
                rslt = subs[i].delete();
            }
            f.delete();
        }
    }

    public static void insertThemeTable(Context context, ContentValues values) {
        context.getContentResolver().insert(ThemeColumns.CONTENT_URI, values);
    }

    public static void deleteThemes(Context context, String productId) {
//        context.getContentResolver().delete(ThemeColumns.CONTENT_URI,
//                ThemeColumns.FILE_PATH + " = '" + localPath + "'", null);
//        context.getContentResolver().delete(FileURLInfoColumns.CONTENT_URI,
//                FileURLInfoColumns.LOCAL_PATH + " = '" + localPath + "'", null);
    }

    public static void deleteObjects(Context context, String localPath){
//        context.getContentResolver().delete(FileURLInfoColumns.CONTENT_URI,
//                FileURLInfoColumns.LOCAL_PATH + " = '" + localPath + "'", null);
    }

    public static void markAsApply(Context context,String localPath) {
        ContentValues values = new ContentValues();
        String where = ThemeColumns.FILE_PATH + "='" + localPath + "'";
        values.put(ThemeColumns.IS_APPLY, 1);
        context.getContentResolver().update(ThemeColumns.CONTENT_URI, values, where, null);
        String where2 = ThemeColumns.FILE_PATH + "!='" + localPath + "'";
        values.put(ThemeColumns.IS_APPLY, 0);
        context.getContentResolver().update(ThemeColumns.CONTENT_URI, values, where2, null);
    }

    public static int queryApplyStatus(Context context,String name) {
        int result = -1;
        String where = ThemeColumns.FILE_PATH + "='" + name + "'";
        Cursor cursor = context.getContentResolver().query(ThemeColumns.CONTENT_URI, null, where, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            result = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns.IS_APPLY));
        }
        cursor.close();
        return result;
    }

    public static boolean hasNetWork(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isAvailable()) == true ? true : false;
    }

    public static void installTheme(Context context, DownloadInfo info){
        String localPath = parseTargetPath(context, info);
        try{
            deleteAll(localPath);
            if (!ZipUtil.unzipDataPartition(info.local_path, context.getFilesDir() + File.separator)){
                return;
            }
            FileInputStream fis = new FileInputStream(localPath + "/models_config.xml");
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, "utf-8");
            XmlUtils.beginDocument(parser, "imageInfos");
            ImageInfo imageInfo = new ImageInfo();
            imageInfo.mThemeName = parser.getAttributeValue(null, ModelColumns.THEME_NAME);
            StringBuilder themeConfig = new StringBuilder();
            while (true) {
                XmlUtils.nextElement(parser);
                if ("imageInfo".equals(parser.getName())) {
                    ImageItem imageItem = new ImageItem();
                    imageItem.mImageName = parser.getAttributeValue(null, ModelColumns.IAMGE_NAME);
                    imageItem.mPath = parser.getAttributeValue(null, ModelColumns.IMAGE_PATH);
                    imageItem.mNewPath = imageItem.mPath;
                    if (!imageInfo.mImageItems.contains(imageItem)) {
                        imageInfo.mImageItems.add(imageItem);
                    }

                } else if ("themeConfig".equals(parser.getName())){
                    if (!TextUtils.isEmpty(themeConfig)) {
                        themeConfig.append(";");
                    }
                    String modelName = parser.getAttributeValue(null, "modelName");
                    String enabled = parser.getAttributeValue(null, "enable");
                    themeConfig.append(modelName +":" + enabled);
                } else {
                    break;
                }
            }
            fis.close();
            ContentValues themeValues = new ContentValues();
            themeValues.put(ThemeColumns.NAME, imageInfo.mThemeName);
            themeValues.put(ThemeColumns.TYPE, Category.THEME.ordinal());
            themeValues.put(ThemeColumns.FILE_PATH, localPath);
            themeValues.put(ThemeColumns.IS_APPLY, 0);
            themeValues.put(ThemeColumns.CONFIG, themeConfig.toString());
            themeValues.put(ThemeColumns.PRODUCT_ID, info.product_id);
            themeValues.put(ThemeColumns.VERSION_CODE, info.version_code);
            themeValues.put(ThemeColumns.VERSION_NAME, info.version_name);
            context.getContentResolver().insert(ThemeColumns.CONTENT_URI, themeValues);

            HomeDataBaseHelper help = HomeDataBaseHelper.getInstance(SESceneManager.getInstance().getContext());
            SQLiteDatabase db = help.getWritableDatabase();
//            String where = ModelColumns.TYPE + "='House' AND " + ModelColumns.SCENE_NAME +"='home8'" ;
            String where = ModelColumns.TYPE + "=? AND " + ModelColumns.SCENE_NAME + "=?";
            String[] whereArgs = new String[] {getHouseType(), SESceneInfo.DEFAULT_SCENE_NAME};
            Cursor cursor = db.query(Tables.MODEL_INFO, null, where, whereArgs, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String table = Tables.IMAGE_INFO;
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(ModelColumns._ID));
                    for (ImageItem imageItem : imageInfo.mImageItems) {
                        ContentValues values = new ContentValues();
                        values.put(ModelColumns._ID, id);
                        values.put(ModelColumns.THEME_NAME, imageInfo.mThemeName);
                        values.put(ModelColumns.IMAGE_PATH, imageItem.mPath);
                        values.put(ModelColumns.IMAGE_NEW_PATH, imageItem.mNewPath);
                        values.put(ModelColumns.IAMGE_NAME, imageItem.mImageName);
                        db.insert(table, null, values);
                    }
                }
                cursor.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    public static String parseTargetPath(Context context, DownloadInfo info) {
        return context.getFilesDir() + File.separator + info.product_id.toLowerCase();
    }

    public static String getObjectRootPath() {
    	return HomeUtils.SDCARD_PATH + File.separator + "." + Product.ProductType.OBJECT;
    }
    public static String getObjectPath(String productId) {
        String path = getObjectRootPath();
        File dirRoot = new File(path);
        if (!dirRoot.exists()) {
            dirRoot.mkdirs();
        }

        String localPath ;
        if(dirRoot.isDirectory()) {
            localPath = path + File.separator + productId.toLowerCase();
            File tmpPathFile = new File(localPath);
            if(!tmpPathFile.exists()) {
                boolean bCreatedir = tmpPathFile.mkdir();
                if(!bCreatedir) {
                    Log.i("getObjectPath: ", "create tmp dir error!!");
                    return "";
                }
            }
        } else {
            Log.i("getObjectPath: ", "can not file path : " + path);
            return "";
        }
        return localPath;
    }
    
    public static void installObject(Context context, DownloadInfo info){
        try {
            String localPath = getObjectPath(info.product_id);
            deleteAll(localPath);
            if (!ZipUtil.unzipDataPartition(info.local_path, localPath + File.separator)) {
                return;
            }
            FileInputStream fis = new FileInputStream(localPath + "/models_config.xml");
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, "utf-8");
            XmlUtils.beginDocument(parser, "config");
            ModelInfo config = ModelInfo.CreateFromXml(parser,localPath);
            config.mProductId = info.product_id;
            config.mVersionCode = info.version_code;
            config.mVersionName = info.version_name;
            config.mPlugin = 1;
            HomeDataBaseHelper help = HomeDataBaseHelper.getInstance(SESceneManager.getInstance()
                    .getContext());
            SQLiteDatabase db2 = help.getWritableDatabase();
            config.saveToDB(db2);
            fis.close();
            SESceneManager.getInstance().addModelToScene(config);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }
    
    public static void parseSdcardObjectModelInfo(String localPath, Product info) {
    	try {
    		File targetFile = new File(localPath + "/models_config.xml");
    		if(!targetFile.exists()) {
    			return ;
    		}
    		
            FileInputStream fis = new FileInputStream(localPath + "/models_config.xml");
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, "utf-8");
            XmlUtils.beginDocument(parser, "config");
            ModelInfo config = ModelInfo.CreateFromXml(parser,localPath);
            config.mProductId = info.product_id;
            config.mVersionCode = info.version_code;
            config.mVersionName = info.version_name;
            config.mPlugin = 1;
            HomeDataBaseHelper help = HomeDataBaseHelper.getInstance(SESceneManager.getInstance()
                    .getContext());
            SQLiteDatabase db2 = help.getWritableDatabase();
            config.saveToDB(db2);
            fis.close();
            SESceneManager.getInstance().addModelToScene(config);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    public static void reportMonitoredApps(final Context context) {
        if (!hasNetWork(context)) {
            if (HomeUtils.DEBUG) {
                Log.d("SETTINGS_THREAD", "reportMonitoredApps, skip while set already.");
            }
            return;
        }

        final String preferenceName = SettingsActivity.PREFS_SETTING_NAME;
        SharedPreferences settings = context.getSharedPreferences(preferenceName, 0);
        final String pkgName = context.getPackageName();
        if (settings.getBoolean(pkgName, false)) {
            if (HomeUtils.DEBUG) {
                Log.d("SETTINGS_THREAD", "reportMonitoredApps, skip while set already.");
            }
            return;
        }

        final String REPORT_URL = "http://api.borqs.com/internal/keyvalue?device=%1$s&key=%2$s&value=%3$s";
        final String[] REPORT_LIST = {"com.facebook.katana", "com.google.android.apps.plus", "com.android.vending"};
        new Thread(new Runnable() {
            public void run() {
                try {
                    final PackageManager packageManager = context.getPackageManager();
                    ArrayList<String> reportList = new ArrayList<String>(Arrays.asList(REPORT_LIST));
                    for (String testPkgName : REPORT_LIST) {
                        try {
                            packageManager.getPackageInfo(testPkgName, 0);
                        } catch (PackageManager.NameNotFoundException ne) {
                            reportList.remove(testPkgName);
                        }
                    }

                    if (reportList.isEmpty()) {
                        if (HomeUtils.DEBUG) {
                            Log.d("SETTINGS_THREAD", "skip without target apps");
                        }
                        return;
                    }

                    final String reportValues = TextUtils.join(" ", reportList);

                    String packageName = context.getPackageName();
                    int version = 0;
                    try {
                        version = context.getPackageManager().getPackageInfo(packageName, 0).versionCode;
                    } catch (PackageManager.NameNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    String macId = "";
                    WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    try {
                        WifiInfo info = wm.getConnectionInfo();
                        macId = info.getMacAddress().replace(":", "");
                        if (HomeUtils.DEBUG) {
                            Log.d("SETTINGS_THREAD", "macId=" + macId);
                        }
                    } catch (Exception ne) {
                        ne.printStackTrace();
                        Log.d("SETTINGS_THREAD", "macId exception=" + ne.getMessage());
                    }

                    String device = "mac-" + macId
                            + "-manufacture-" + Build.MANUFACTURER
                            + "-locale-" + Locale.getDefault().toString()
                            + "-model-" + Build.MODEL
                            + "-dev-" + Build.DEVICE
                            + "-sdk-" + Build.VERSION.SDK_INT
                            + "-android-" + Build.VERSION.RELEASE
                            + "-package-" + context.getPackageName()
                            + "-version-" + version;

                    String url = String.format(REPORT_URL,
                            URLEncoder.encode(device, "UTF-8"),
                            URLEncoder.encode(pkgName, "UTF-8"),
                            URLEncoder.encode(reportValues, "UTF-8"));
                    if (HomeUtils.DEBUG) {
                        Log.d("SETTINGS_THREAD", "statics device = " + device + ", url = " + url);
                    }
                    HttpURLConnection con = (HttpURLConnection) (new URL(url).openConnection());
                    int code = con.getResponseCode();
                    if (HttpURLConnection.HTTP_OK == code) {
                        SharedPreferences settings = context.getSharedPreferences(preferenceName, 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean(pkgName, true);
                        editor.commit();
                    } else {
                        if (HomeUtils.DEBUG) {
                            Log.d("SETTINGS_THREAD", "http code = " + code);
                        }
                    }
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "SETTINGS_THREAD").start();
    }

    public static void installWallpaper(Context context, DownloadInfo info) {
        String localPath = WallpaperUtils.getWallPaperPath(info.product_id);
        try {
            clearUsingWallpaper(context, info.product_id);
            deleteAll(localPath);
            if (!ZipUtil.unzipDataPartition(info.local_path, localPath + File.separator)){
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //    private static final String[] paperPortKey = {
//            "hd_diban_zch.jpg@home8mianshu_basedata.cbf",
//            "home_8mian_shuping_zch.jpg@home8mianshu_basedata.cbf",
//            "home_8mian_shuping01_zch.jpg@home8mianshu_basedata.cbf",
//            "home_8mian_shuping01_zch.jpg@home8mianshu_basedata.cbf_0",
//            "home_8mian_shuping_zch.jpg@home8mianshu_basedata.cbf_0",
//            "home_8mian_shuping_zch.jpg@home8mianshu_basedata.cbf_1",
//            "home_8mian_shuping01_zch.jpg@home8mianshu_basedata.cbf_1",
//            "home_8mian_shuping_zch.jpg@home8mianshu_basedata.cbf_2",
//            "home_8mian_shuping01_zch.jpg@home8mianshu_basedata.cbf_2"
//    };
//
//    private static final String[] paperLandKey = {
//            "hd_diban_zch.jpg@home4mian_basedata.cbf",
//            "hd_home_4mianqiang01_zch.jpg@home4mian_basedata.cbf",
//            "hd_home_4mianqiang_zch.jpg@home4mian_basedata.cbf",
//            "hd_home_4mianqiang01_zch.jpg@home4mian_basedata.cbf_0",
//            "hd_home_4mianqiang_zch.jpg@home4mian_basedata.cbf_0"
//    };
//
    private static List<String> getFiles(String path, String extension,boolean isIterative) //搜索目录，扩展名，是否进入子文件夹
    {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        File root = new File(path);
        if (!root.exists()) {
            return null;
        }

        List<String> lstFile = new ArrayList<String>();
        File[] files = root.listFiles();
        for (int i =0; i < files.length; i++)
        {
            File f = files[i];
            if (f.isFile())
            {
                if (f.getPath().substring(f.getPath().length() - extension.length()).equals(extension)) //判断扩展名
                    lstFile.add(f.getPath());

                if (!isIterative)
                    break;
            }
            else if (f.isDirectory() && f.getPath().indexOf("/.") == -1) //忽略点文件（隐藏文件/文件夹）
                getFiles(f.getPath(), extension, isIterative);
        }
        return lstFile;
    }

    private static boolean isGroundPaperFile(File file) {
        final String name = file.getName().toLowerCase();
        return name.startsWith("background");
    }

    private static boolean isWallpaperFile(File file) {
        final String name = file.getName().toLowerCase();
        if (name.startsWith("wallpaper")) {
            return true;
        }
        return false;
    }

//    public static boolean isExistingFilePath(String path) {
//
//        if (TextUtils.isEmpty(path)) {
//            return false;
//        }
//        File root = new File(path);
//        return root.exists();
//    }

    //TODO
    /**
     *if need read Rules from the configuration file,i recommend use 'ResourceManifest.xml',
     * but need defining the rules;
     */

    private static final String SUFFIX_LAND = "land";
    private static final String SUFFIX_PORT = "port";

//    private static void prepareWallpaper(String parent, String targetPath, SEVector.SEVector2f wallXz) {
//        if (TextUtils.isEmpty(parent) || TextUtils.isEmpty(targetPath)) {
//            Log.e(TAG, "prepareWallpaper, invalid path.");
//            return;
//        }
//        if (0f == wallXz.getX() || 0f == wallXz.getY()) {
//            Log.e(TAG, "prepareWallpaper, invalid h2wRatio " + wallXz);
//            return;
//        }
//
//        ArrayList<String> srcWallPapers = new ArrayList<String>();
//        ArrayList<String> srcGroundPapers = new ArrayList<String>();
//        getPaperList(parent, srcWallPapers, srcGroundPapers);
//        if (srcWallPapers.isEmpty() && srcGroundPapers.isEmpty()) {
//            Log.e(TAG, "prepareWallpaper, no valid paper found, skip.");
//            return;
//        }
//
//        /// force apply
////        ArrayList<String> dstWallPaper = new ArrayList<String>();
////        ArrayList<String> dstGroundPaper = new ArrayList<String>();
////        getPaperList(targetPath, dstWallPaper, dstGroundPaper);
////        if (true || (dstWallPaper.isEmpty() && dstGroundPaper.isEmpty())) {
//            performPaperConvert(targetPath, srcWallPapers, wallXz.getY() / wallXz.getX());
//            performPaperConvert(targetPath, srcGroundPapers, 1.0f);
//
//            System.gc();
////        } else {
////            if (HomeUtils.DEBUG) {
////                Log.d(TAG, "prepareWallpaper, existing paper list " + dstGroundPaper.size() + dstWallPaper.size()
////                        + ", origin size " + srcGroundPapers.size() + srcWallPapers.size());
////            }
////        }
//    }

    private static ArrayList<String> performPaperConvert(String targetPath, ArrayList<String> sourcePapers, float h2wRatio) {
        File targetFile = new File(targetPath);
        if (targetFile.exists()) {
            targetFile.delete();
            targetFile.mkdir();
        } else {
            targetFile.mkdir();
        }

        ArrayList<String> pathList = new ArrayList<String>();
        // do converting task
        String convertedPath;
        for(String filePath : sourcePapers) {
            pathList.add(WallpaperUtils.encodeWallPaper(filePath, targetPath, h2wRatio));
        }
        return pathList;
    }

    private static String getCurrentThemeName(Context context) {
        String pUseThemeName = "";
        String querythemeWhere = ThemeColumns.IS_APPLY + " = " + 1;
        Cursor cursor = context.getContentResolver().query(ThemeColumns.CONTENT_URI, new String[]{ThemeColumns.NAME},
                querythemeWhere, null, null);
        if(cursor != null) {
            if(cursor.moveToFirst()) {
                pUseThemeName = cursor.getString(cursor.getColumnIndex(ThemeColumns.NAME));
            }
            cursor.close();
        }
        return pUseThemeName;
    }

//    public static void getRawPaperItems(Context context, ArrayList<RawPaperItem> rawWallPaper) {
//        queryRawPaperItems(context, rawWallPaper);
////        SEVector.SEVector2f wallXz = ModelInfo.getHouseObject(SESceneManager.getInstance().getCurrentScene()).getWallXZBounder();
////        WallpaperUtils.decodePaper(context, rawWallPaper, wallXz.getY() / wallXz.getX());
//        float h2wRatio = SESceneManager.getWallRatioH2W();
//        WallpaperUtils.decodePaper(context, rawWallPaper, h2wRatio);
//    }

    public static ArrayList<RawPaperItem> queryRawPaperItems(Context context) {
        ArrayList<RawPaperItem> rawPaperItems = new ArrayList<RawPaperItem>();

        final float h2wWall = SESceneManager.getWallRatioH2W();
        final float h2wGround = 1.0f;

        List<String> wallKeyList = getWallPaperKeySet();
        List<String> groundKeyList = getGroundPaperKeySet();

        rawPaperItems.clear();
        HashMap<String, RawPaperItem> cachedMap = new HashMap<String, RawPaperItem>();
        RawPaperItem item;
        for (String name : wallKeyList) {
            item = new RawPaperItem("", name, RawPaperItem.TYPE_WALL, h2wWall);
//            item.mKeyName = name;
//            item.mType = RawPaperItem.TYPE_WALL;
//            item.mRatioH2W = h2wWall;
            rawPaperItems.add(item);
            cachedMap.put(name, item);
        }
        for (String name : groundKeyList) {
            item = new RawPaperItem("", name, RawPaperItem.TYPE_GROUND, h2wGround);
//            item.mKeyName = name;
//            item.mType = RawPaperItem.TYPE_GROUND;
//            item.mRatioH2W = h2wGround;
            rawPaperItems.add(item);
            cachedMap.put(name, item);
        }

        if (cachedMap.isEmpty()) {
            return rawPaperItems;
        }

        final String pUseThemeName = getCurrentThemeName(context);
        if(!TextUtils.isEmpty(pUseThemeName)) {
            final String where = ModelColumns.THEME_NAME + "=?";
            final String[] whereArgs = new String[] {pUseThemeName};
            Cursor imageCursor = context.getContentResolver().query(ImageInfoColumns.CONTENT_URI,null, where, whereArgs, null);
            if (null != imageCursor) {
                if (imageCursor.moveToFirst()) {
                    String imgName;
                    RawPaperItem imageItem;
                    do {
                        imgName = imageCursor.getString(imageCursor.getColumnIndexOrThrow(ModelColumns.IAMGE_NAME));
                        if (cachedMap.containsKey(imgName)) {
                            imageItem = cachedMap.get(imgName);
                            imageItem.mDefaultValue = imageCursor.getString(imageCursor.getColumnIndexOrThrow(ModelColumns.IMAGE_PATH));
                            imageItem.mOverlayValue = imageCursor.getString(imageCursor.getColumnIndexOrThrow(ModelColumns.IMAGE_NEW_PATH));
                        }
                    } while (imageCursor.moveToNext());
                }
                imageCursor.close();
            }
        }
        return rawPaperItems;
    }

    private static boolean performPaperCycleApply(Context context, String pUseThemeName,
                                             List<String> paperKey, ArrayList<String> paperList) {
        boolean usedNewWallPaper = null != paperKey && !paperKey.isEmpty();

        if (null == paperList || paperList.isEmpty()) {
            // reset all
            for (String imageName : paperKey) {
                resetWallpaper(context, SESceneManager.getInstance().getCurrentScene(), pUseThemeName, imageName);
            }
        } else {
            final int keySize = paperKey.size();
            final int valueSize = paperList.size();
            String targetPath;
            String imageName;
            for (int i = 0; i < keySize; i++) {
                imageName = paperKey.get(i);
                targetPath = paperList.get(i % valueSize);
                if (TextUtils.isEmpty(targetPath)) {
                    // reset to default
                    resetWallpaper(context, SESceneManager.getInstance().getCurrentScene(), pUseThemeName, imageName);
                } else {
                    applyWallpaper(pUseThemeName, imageName, targetPath);
                }
            }
        }

        return usedNewWallPaper;
    }

    private static boolean performPaperMinReplace(Context context, String pUseThemeName,
                                             List<String> paperKey, ArrayList<String> paperList) {
        boolean usedNewWallPaper = false;

        String targetPath;
        String imageName;

        final int valueSize = paperList.size();
        final int keySize = paperKey.size();
        final int matchSize = Math.min(keySize, valueSize);
        for (int i = 0; i < matchSize; ++i) {
            targetPath = paperList.get(i);
            imageName = paperKey.get(i);
            if (TextUtils.isEmpty(targetPath)) {
                // reset to default
                resetWallpaper(context, SESceneManager.getInstance().getCurrentScene(), pUseThemeName, imageName);
            } else {
                applyWallpaper(pUseThemeName, imageName, targetPath);
            }
            usedNewWallPaper = true;
        }

        return usedNewWallPaper;
    }

    private static List<String> getWallPaperKeySet() {
        return ModelInfo.getWallpaperKeySet();
    }

    private static List<String> getGroundPaperKeySet() {
        return ModelInfo.getGroundpaperKeySet();
    }

    private static String getHouseType() {
        return ModelInfo.getHouseType();
    }

    public static String parseLastPathSection(String localPath) {
        String[] paths = localPath.split("/");
        return paths[paths.length - 1].replace(".zip", "");
    }

    public static void applyWallpaper(String currentTheme, final String imageName, final String imagePath) {
        HomeDataBaseHelper help = HomeDataBaseHelper.getInstance(SESceneManager.getInstance().getContext());
        SQLiteDatabase db = help.getWritableDatabase();

        String where = ModelColumns.THEME_NAME + "=? AND "  + ModelColumns.IAMGE_NAME + "=?";
        String[] whereArgs = new String[] {currentTheme, imageName};

        ContentValues imageInfoValues = new ContentValues();
        imageInfoValues.put(ModelColumns.IMAGE_NEW_PATH, imagePath);
        db.update(Tables.IMAGE_INFO, imageInfoValues, where, whereArgs);
        db.close();
    }

    public static void resetWallpaper(Context context, final SEScene scene, String currentTheme, final String imageName) {
        String where = ModelColumns.THEME_NAME + "=? AND "  + ModelColumns.IAMGE_NAME + "=?";
        String[] whereArgs = new String[] {currentTheme, imageName};
        ContentResolver resolver = context.getContentResolver();
        Cursor imageCursor = resolver.query(ModelColumns.IMAGE_INFO_URI, null, where, whereArgs, null);
        final ImageItem imageItem = new ImageItem();
        imageItem.mImageName = imageName;
        if (imageCursor.moveToFirst()) {
            imageItem.mPath = imageCursor.getString(imageCursor
                    .getColumnIndexOrThrow(ModelColumns.IMAGE_PATH));
            imageItem.mNewPath = imageCursor.getString(imageCursor
                    .getColumnIndexOrThrow(ModelColumns.IMAGE_NEW_PATH));
            imageCursor.close();
        }

        imageItem.mNewPath = imageItem.mPath;
        new SECommand(scene) {
            public void run() {
                boolean exist = SEObject.isImageExist_JNI(imageItem.mNewPath);
                if (exist) {
                    SEObject.applyImage_JNI(imageItem.mImageName, imageItem.mNewPath);
                } else {
                    SELoadResThread.getInstance().process(new Runnable() {
                        public void run() {
                            final int image = SEObject.loadImageData_JNI(imageItem.mNewPath);
                            new SECommand(scene) {
                                public void run() {
                                    SEObject.applyImage_JNI(imageName, imageItem.mNewPath);
                                    SEObject.addImageData_JNI(imageItem.mNewPath, image);
                                }
                            }.execute();
                        }
                    });
                }
            }
        }.execute();
        ModelInfo.updateWallPaperDB(context, imageItem.mNewPath, imageName, currentTheme);
    }

    public static void applyWallpaper(Context context, String productId) {
//        SEVector.SEVector2f wallXz = ModelInfo.getHouseObject(SESceneManager.getInstance().getCurrentScene()).getWallXZBounder();
//        final boolean isPort = wallXz.getX() < wallXz.getY();
        float h2wRatio = SESceneManager.getWallRatioH2W();
        final boolean isPort = h2wRatio > 1.0f;
        final String parentPath = WallpaperUtils.getWallPaperPath(productId);
        final String targetPath = parentPath + File.separator + (isPort ? SUFFIX_PORT : SUFFIX_LAND);

        if(TextUtils.isEmpty(targetPath)) {
            return ;
        }

//        prepareWallpaper(parentPath, targetPath, wallXz);
        if (TextUtils.isEmpty(parentPath) || TextUtils.isEmpty(targetPath)) {
            Log.e(TAG, "applyWallpaper, invalid path.");
            return;
        }
//        if (0f == wallXz.getX() || 0f == wallXz.getY()) {
//            Log.e(TAG, "applyWallpaper, invalid h2wRatio " + wallXz);
//            return;
//        }

        if (TextUtils.isEmpty(parentPath)) {
            return;
        }

        File root = new File(parentPath);
        if (!root.exists()) {
            return;
        }

        ArrayList<RawPaperItem> rawPaperItems = WallpaperUtils.parseRawPaperItemList(parentPath);
        ArrayList<String> srcWallPapers = new ArrayList<String>();
        ArrayList<String> srcGroundPapers = new ArrayList<String>();
//        getPaperList(parentPath, srcWallPapers, srcGroundPapers);
        if (null == rawPaperItems || rawPaperItems.isEmpty()) {
            File[] files = root.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                File f = files[i];
                if (f.isFile())
                {
                    if (isWallpaperFile(f)) {
                        srcWallPapers.add(f.getPath());
                    } else if (isGroundPaperFile(f)) {
                        srcGroundPapers.add(f.getPath());
                    }
                }
            }
            Collections.sort(srcWallPapers);
            Collections.sort(srcGroundPapers);
        } else {
            String filePath;
            for (RawPaperItem item : rawPaperItems) {
                filePath = item.mDecodedValue;
                if (TextUtils.isEmpty(filePath)) continue;
                filePath = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
                filePath = parentPath + File.separator + filePath;
                File file = new File(filePath);
                if (!file.exists()) {
                    Log.e(TAG, "applyWallpaper, skip not existing file " + filePath);
                    continue;
                }
                if (RawPaperItem.TYPE_WALL.equalsIgnoreCase(item.mType)) {
                    srcWallPapers.add(filePath);
                } else if (RawPaperItem.TYPE_GROUND.equalsIgnoreCase(item.mType)) {
                    srcGroundPapers.add(filePath);
                } else {
                    Log.w(TAG, "applyWallpaper, unknown item type " + item.mType);
                }
            }
        }

        if (srcWallPapers.isEmpty() && srcGroundPapers.isEmpty()) {
            Log.e(TAG, "applyWallpaper, no valid paper found, skip.");
            return;
        }
        ArrayList<String> wallPaperList = performPaperConvert(targetPath, srcWallPapers, h2wRatio);
        ArrayList<String> groundPaperList = performPaperConvert(targetPath, srcGroundPapers, 1.0f);

        System.gc();

        if (wallPaperList.isEmpty() && groundPaperList.isEmpty()) {
            Log.e(TAG, "applyWallpaper, no any wallpaper in the folder " + targetPath);
            return;
        }

        final String pUseThemeName = getCurrentThemeName(context);
        if(!TextUtils.isEmpty(pUseThemeName)) {
            boolean usedNewWallPaper = false;
            if (performPaperCycleApply(context, pUseThemeName, getWallPaperKeySet(), wallPaperList)) {
                usedNewWallPaper = true;
            }
            if (performPaperCycleApply(context, pUseThemeName, getGroundPaperKeySet(), groundPaperList)) {
                usedNewWallPaper = true;
            }

            if (usedNewWallPaper) {
                SettingsActivity.saveApplyWallpaperSuite(context, productId);
                SettingsActivity.saveChangeWallPaper(context, true);
                SettingsActivity.saveWallpaperCustomizedTimeStamp(context, false);
                MarketUtils.updatePlugIn(context, productId, true, MarketUtils.CATEGORY_WALLPAPER);

                Intent intent = new Intent(context, BlankActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    private static void clearUsingWallpaper(Context context, String productId) {
        if (MarketUtils.isProductApplied(context, productId)) {
            final String pUseThemeName = getCurrentThemeName(context);
            performPaperCycleApply(context, pUseThemeName, getWallPaperKeySet(), null);
            performPaperCycleApply(context, pUseThemeName, getGroundPaperKeySet(), null);
            MarketUtils.updatePlugIn(context, productId, false);
        }else {
            MarketUtils.deletePlugIn(context, productId);
        }
        MarketUtils.deletePlugIn(context, productId);
    }

    // dumb method, to intercept screen shot.
    public static void getScreenShotList(String localPath, ArrayList<String> screenShot) {
    }
}
