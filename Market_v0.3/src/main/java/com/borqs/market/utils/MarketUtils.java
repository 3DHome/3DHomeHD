package com.borqs.market.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.text.TextUtils;

import com.borqs.market.CommentListActivity;
import com.borqs.market.MarketHomeActivity;
import com.borqs.market.PartitionsListActivity;
import com.borqs.market.ProductListActivity;
import com.borqs.market.UserShareListActivity;
import com.borqs.market.WallPaperLocalProductListActivity;
import com.borqs.market.db.DownLoadHelper;
import com.borqs.market.db.DownLoadProvider;
import com.borqs.market.db.DownloadInfo;
import com.borqs.market.db.DownloadInfoColumns;
import com.borqs.market.db.PlugInColumns;
import com.borqs.market.db.PlugInInfo;
import com.borqs.market.json.Product;
import com.borqs.market.json.Product.ProductType;
import com.borqs.market.json.ProductJSONImpl;
import com.borqs.market.wallpaper.RawPaperItem;
import com.borqs.market.wallpaper.WallpaperUtils;

public class MarketUtils {
    @SuppressWarnings("unused")
    private final static String TAG = "MarketUtils";

    public static void setLogVisibility(boolean show) {
        BLog.setSHOW_LOG(show);
    }
    
    public static void showPerformanceLog(boolean show) {
        BLog.setShowPerformanceLog(show);
    }

    public static final String EXTRA_APP_VERSION = "EXTRA_APP_VERSION";
    public static final String EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME";
    public static final String EXTRA_CATEGORY = "EXTRA_CATEGORY";
    public static final String EXTRA_MOD = "EXTRA_MOD";
    public static final String EXTRA_TAB = "EXTRA_TAB";
    public static final String EXTRA_SHOW_HEADVIEW = "EXTRA_SHOW_HEADVIEW";
    public static final String EXTRA_ORDER_BY = "EXTRA_ORDER_BY";
    public static final String EXTRA_NAME = "EXTRA_NAME";
    public static final String EXTRA_ID = "EXTRA_ID";
    public static final String EXTRA_LOCAL_PATH = "EXTRA_LOCAL_PATH";

    public static final String CATEGORY_THEME = ProductType.THEME;
    public static final String CATEGORY_OBJECT = ProductType.OBJECT;
    public static final String CATEGORY_SCENE = ProductType.SCENE;
    public static final String CATEGORY_WALLPAPER = ProductType.WALL_PAPER;
    public static final String TAB_LOCAL = "TAB_LOCAL";
    public static final String TAB_REMOTE = "TAB_REMOTE";
//    public static final String DOWNLOAD_AUTHORITY = "com.borqs.freehdhome.download";//请更改为androidManifest中定义的DownloadProvider的authority
    
    public static boolean IS_ONLY_FREE = false; //是否只取免费的
    public static final String MARKET_CONFIG_FILE_NAME = "ResourceManifest.xml";

    private static final String PREFS_SETTING_NAME = "com.borqs.se_preferences";
    private static final String KEY_ORIENTATION_PREFERRED = "orientation_preferred_key";
    private final static String ORIENTATION_ROTATION = "rotation";
    private final static String ORIENTATION_LANDSCAPE = "landscape";
    private final static String ORIENTATION_PORTRAIT = "portrait";

    /**
     * 
     * @param context
     * @param isOnlyFree
     *            application version code
     * @param category
     *            MarketUtils.CATEGORY_THEME MarketUtils.CATEGORY_OBJECT,MarketUtils.CATEGORY_WALLPAPER
     *            MarketUtils.CATEGORY_SCENE
     */
    public static void startMarketIntent(Context context, String category,boolean isOnlyFree) {
        IS_ONLY_FREE = isOnlyFree;
        Intent intent = new Intent(context, MarketHomeActivity.class);
        intent.putExtra(EXTRA_CATEGORY, category);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);

    }

    /**
     * 
     * @param context
     * @param isPortMode
     *            preferred portrait mode or not.
     * @param category
     *            MarketUtils.CATEGORY_THEME MarketUtils.CATEGORY_OBJECT
     *            MarketUtils.CATEGORY_SCENE
     */
    public static void startProductListIntent(Context context, String category,boolean isOnlyFree, boolean isPortMode) {
        startProductListIntent(context, category, isOnlyFree,
                isPortMode ? Product.SupportedMod.PORTRAIT : Product.SupportedMod.LANDSCAPE);

    }
    public static void startProductListIntent(Context context, String category,boolean isOnlyFree, String supportedMode) {
        IS_ONLY_FREE = isOnlyFree;
        Intent intent = new Intent(context, ProductListActivity.class);
        intent.putExtra(EXTRA_CATEGORY, category);
        intent.putExtra(EXTRA_MOD, supportedMode);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        context.startActivity(intent);
        
    }
    
    public static void startProductListIntent(Context context, String category,boolean isOnlyFree, String supportedMod
            , boolean showHeaderView, String orderBy) {
        IS_ONLY_FREE = isOnlyFree;
        Intent intent = new Intent(context, ProductListActivity.class);
        intent.putExtra(EXTRA_CATEGORY, category);
        intent.putExtra(EXTRA_MOD, supportedMod);
        intent.putExtra(EXTRA_SHOW_HEADVIEW, showHeaderView);
        intent.putExtra(EXTRA_ORDER_BY, orderBy);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        context.startActivity(intent);
        
    }
    
    /**
     * 
     * @param context
     * @param category
     *            MarketUtils.CATEGORY_THEME MarketUtils.CATEGORY_OBJECT
     *            MarketUtils.CATEGORY_SCENE
     */
    public static void startUserShareListIntent(Context context, String category, String supportedMod) {
        Intent intent = new Intent(context, UserShareListActivity.class);
        intent.putExtra(EXTRA_CATEGORY, category);
        intent.putExtra(EXTRA_MOD, supportedMod);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        context.startActivity(intent);
        
    }
    
    public static void startPartitionseListIntent(Context context, String category, String supportedMod, String name, String partitions_id) {
        Intent intent = new Intent(context, PartitionsListActivity.class);
        intent.putExtra(EXTRA_CATEGORY, category);
        intent.putExtra(EXTRA_MOD, supportedMod);
        intent.putExtra(EXTRA_NAME, name);
        intent.putExtra(EXTRA_ID, partitions_id);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);

    }

    public static void startLocalProductListIntent(Context context, String package_name, String category,
                                                   String localPath, boolean isOnlyFree, String supported_mod) {
        startLocalProductListIntent(context, package_name, category, localPath, isOnlyFree, supported_mod, null);

    }
    public static void startLocalProductListIntent(Context context, String package_name, String category,
                                                   String localPath, boolean isOnlyFree, String supported_mod,
                                                   ArrayList<RawPaperItem> wallPaperItems) {
        MarketUtils.IS_ONLY_FREE = isOnlyFree;
        if (TextUtils.isEmpty(package_name)) {
            throw new IllegalArgumentException("package name is null");
        }
        try {
            int version_code = context.getPackageManager()
                    .getPackageInfo(package_name, PackageManager.GET_UNINSTALLED_PACKAGES).versionCode;
            
            Intent intent = new Intent(context, WallPaperLocalProductListActivity.class);
            intent.putExtra(MarketUtils.EXTRA_APP_VERSION, version_code);
            intent.putExtra(MarketUtils.EXTRA_PACKAGE_NAME, package_name);
            intent.putExtra(MarketUtils.EXTRA_CATEGORY, category);
            intent.putExtra(MarketUtils.EXTRA_LOCAL_PATH, localPath);
            intent.putExtra(MarketUtils.EXTRA_MOD, supported_mod);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (null != wallPaperItems) {
                intent.putParcelableArrayListExtra(WallpaperUtils.EXTRA_RAW_WALL_PAPERS, wallPaperItems);
            }

            context.startActivity(intent);
            
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void startLocalProductListIntent(Context context, String category, String localPath, boolean isOnlyFree, String supported_mod) {
        MarketUtils.IS_ONLY_FREE = isOnlyFree;
        Intent intent = new Intent(context, WallPaperLocalProductListActivity.class);
        intent.putExtra(MarketUtils.EXTRA_CATEGORY, category);
        intent.putExtra(MarketUtils.EXTRA_LOCAL_PATH, localPath);
        intent.putExtra(MarketUtils.EXTRA_MOD, supported_mod);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        context.startActivity(intent);
    }
    
    public static final String EXTRA_FILE_URI = "EXTRA_FILE_URI";
    public static final String EXTRA_FILE_NAME = "EXTRA_FILE_NAME";
    public static final String EXTRA_PRODUCT_ID = "EXTRA_PRODUCT_ID";
    public static final String EXTRA_VERSION_CODE = "EXTRA_VERSION_CODE";
    public static final String EXTRA_VERSION_NAME = "EXTRA_VERSION_NAME";
    public static final String ACTION_MARKET_DOWNLOAD_COMPLETE = MarketConfiguration.getPackageName() + ".market.intent.action.DOWNLOAD_COMPLETE";
    public static final String ACTION_MARKET_THEME_INSTALL = MarketConfiguration.getPackageName() + ".market.intent.action.INSTALL";
    public static final String ACTION_MARKET_THEME_APPLY = MarketConfiguration.getPackageName() + ".market.intent.action.APPLY";
    public static final String ACTION_MARKET_THEME_DELETE = MarketConfiguration.getPackageName() + ".market.intent.action.DELETE";
    public static final String ACTION_BACK_TO_SCENE = MarketConfiguration.getPackageName() + ".market.intent..action.ACTION_BACK_TO_SCENE";

    public static void insertPlugIn(Context context, PlugInInfo plugin) {
        ContentValues pluginValues = new ContentValues();
        pluginValues.put(PlugInColumns.NAME, plugin.name);
        pluginValues.put(PlugInColumns.PRODUCT_ID, plugin.product_id);
        pluginValues.put(PlugInColumns.VERSION_NAME, plugin.version_name);
        pluginValues.put(PlugInColumns.VERSION_CODE, plugin.version_code);
        pluginValues.put(PlugInColumns.TYPE, plugin.type);
        pluginValues.put(PlugInColumns.IS_APPLY,plugin.is_apply?1:0);
        pluginValues.put(PlugInColumns.LOCAL_JSON_STR, plugin.local_json_str);
        pluginValues.put(PlugInColumns.JSON_STR, plugin.json_str);
        pluginValues.put(PlugInColumns.SUPPORTED_MOD, plugin.supported_mod);
        context.getContentResolver().insert(DownLoadProvider.getContentURI(context, DownLoadProvider.TABLE_PLUGIN), pluginValues);
    }
    
    public static void insertPlugIn(Context context, DownloadInfo info, boolean isApply, File file) {
        if(file == null) {
            throw new IllegalAccessError("file can't null");
        }
        ContentValues pluginValues = new ContentValues();
        pluginValues.put(PlugInColumns.NAME, info.name);
        pluginValues.put(PlugInColumns.PRODUCT_ID, info.product_id);
        pluginValues.put(PlugInColumns.VERSION_NAME, info.version_name);
        pluginValues.put(PlugInColumns.VERSION_CODE, info.version_code);
        pluginValues.put(PlugInColumns.TYPE, info.type);
        pluginValues.put(PlugInColumns.IS_APPLY,isApply?1:0);
        pluginValues.put(PlugInColumns.FILE_PATH, file.getPath());
        Product product = ThemeXmlParser.parser(file);
        if(product != null) {
            pluginValues.put(PlugInColumns.LOCAL_JSON_STR, ProductJSONImpl.createJsonObjectString(product));
        }
        pluginValues.put(PlugInColumns.JSON_STR, info.json_str);
        pluginValues.put(PlugInColumns.SUPPORTED_MOD, info.supported_mod);
        context.getContentResolver().insert(DownLoadProvider.getContentURI(context, DownLoadProvider.TABLE_PLUGIN), pluginValues);
    }

    public static void updatePlugIn(Context context, String id, boolean isApply, String exclusionType) {
        ContentValues values = new ContentValues();
        String where = PlugInColumns.TYPE + "=?";
        if (!TextUtils.isEmpty(exclusionType)) {
            values.put(PlugInColumns.IS_APPLY, 0);
            String[] whereArgs = {exclusionType};
            context.getContentResolver().update(DownLoadProvider.getContentURI(context, DownLoadProvider.TABLE_PLUGIN), values, where, whereArgs);
        }

        where = PlugInColumns.PRODUCT_ID + " = '" + id + "'";
        values.put(PlugInColumns.IS_APPLY,isApply ? 1 : 0);
        context.getContentResolver().update(DownLoadProvider.getContentURI(context, DownLoadProvider.TABLE_PLUGIN), values, where, null);
    }

    public static void updatePlugIn(Context context, String id, boolean isApply) {
        ContentValues values = new ContentValues();
        String where = PlugInColumns.PRODUCT_ID + " = '" + id + "'";
        values.put(PlugInColumns.IS_APPLY,isApply?1:0);
        context.getContentResolver().update(DownLoadProvider.getContentURI(context, DownLoadProvider.TABLE_PLUGIN), values, where, null);
    }

    public static boolean isProductApplied(Context context, String productId) {
        String where = PlugInColumns.PRODUCT_ID + "=?";
        String[] whereArgs = new String[]{productId};
        Cursor cursor = context.getContentResolver().query(DownLoadProvider.getContentURI(context, DownLoadProvider.TABLE_PLUGIN), new String[]{PlugInColumns.IS_APPLY},
                where, whereArgs, null);
        boolean result = false;
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                result = 1 == cursor.getInt(cursor.getColumnIndex(PlugInColumns.PRODUCT_ID));
            }
            cursor.close();
        }
        return result;
    }
    
    public static void updatePluginWithDownloadInfo(Context context, DownloadInfo info, File file) {
    	 ContentValues pluginValues = new ContentValues();
         String where = PlugInColumns.PRODUCT_ID + " = '" + info.product_id + "'";
         pluginValues.put(PlugInColumns.NAME, info.name);
         pluginValues.put(PlugInColumns.VERSION_NAME, info.version_name);
         pluginValues.put(PlugInColumns.VERSION_CODE, info.version_code);
         Product product = ThemeXmlParser.parser(file);
         if(product != null) {
             pluginValues.put(PlugInColumns.LOCAL_JSON_STR, ProductJSONImpl.createJsonObjectString(product));
         }
         pluginValues.put(PlugInColumns.JSON_STR, info.json_str);
         pluginValues.put(PlugInColumns.SUPPORTED_MOD, info.supported_mod);
         
         context.getContentResolver().update(DownLoadProvider.getContentURI(context, DownLoadProvider.TABLE_PLUGIN), pluginValues, where, null);
    }
    
    public static void updatePlugInVersion(Context context, String id, int versionCode, String versionName) {
        ContentValues values = new ContentValues();
        String where = PlugInColumns.PRODUCT_ID + " = '" + id + "'";
        values.put(PlugInColumns.VERSION_CODE,versionCode);
        values.put(PlugInColumns.VERSION_NAME,versionName);
        context.getContentResolver().update(DownLoadProvider.getContentURI(context, DownLoadProvider.TABLE_PLUGIN), values, where, null);
    }
    
    public static void deletePlugIn(Context context, String id) {
        String where = PlugInColumns.PRODUCT_ID + " = '" + id + "'";
        context.getContentResolver().delete(DownLoadProvider.getContentURI(context, DownLoadProvider.TABLE_PLUGIN), where, null);
    }
    
    public static Cursor queryPlugIn(Context context, String id) {
        String where = PlugInColumns.PRODUCT_ID + " = '" + id + "'";
        return context.getContentResolver().query(DownLoadProvider.getContentURI(context, DownLoadProvider.TABLE_PLUGIN), PlugInColumns.PROJECTION, where, null, null);
    }
    
    public static DownloadInfo getDownloadInfoByProductId(Context context, String productId){
        DownloadInfo info = null;
        Cursor cursor = null;
        try {
            String where = DownloadInfoColumns.PRODUCT_ID  + " = '"  + productId + "'";
            cursor = context.getContentResolver().query(DownLoadProvider.getContentURI(context, DownLoadProvider.TABLE_DOWNLOAD), null, where, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                info = new DownloadInfo();
                info.product_id = productId;
                info.download_status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadInfoColumns.DOWNLOAD_STATUS));
                info.name = cursor.getString(cursor.getColumnIndexOrThrow(DownloadInfoColumns.NAME));
                info.version_name = cursor.getString(cursor.getColumnIndexOrThrow(DownloadInfoColumns.VERSION_NAME));
                info.version_code = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadInfoColumns.VERSION_CODE));
                info.local_path = cursor.getString(cursor.getColumnIndexOrThrow(DownloadInfoColumns.LOCAL_PATH));
                info.type = cursor.getString(cursor.getColumnIndexOrThrow(DownloadInfoColumns.TYPE));
                info.json_str = cursor.getString(cursor.getColumnIndexOrThrow(DownloadInfoColumns.JSON_STR));
                info.supported_mod = cursor.getString(cursor.getColumnIndexOrThrow(DownloadInfoColumns.SUPPORTED_MOD));
                if (info.local_path.contains("file://")){
                    info.local_path = info.local_path.replace("file://", "");
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        } 
        return info;
    }
    
    public static DownloadInfo getDownloadInfoFromPlugIn(Context context, String productId) {
        DownloadInfo info = null;
        Product product = DownLoadHelper.queryLocalProductInfo(context, productId);
        if(product != null) {
            info = new DownloadInfo();
            info.product_id = productId;
            info.download_status = DownloadInfoColumns.DOWNLOAD_STATUS_COMPLETED;
            info.name = product.name;
            info.version_name = product.version_name;
            info.version_code = product.version_code;
            info.local_path = null;
            info.type = product.type;
            info.json_str = null;
            info.supported_mod = product.supported_mod;
        }
        return info;
    }
    
    public static void bulkInsertPlugIn(Context context, List<PlugInInfo> pluginList) {
        final int size = null == pluginList ? 0 : pluginList.size();
        if (size > 0) {
            ContentValues valueList[] = new ContentValues[size];
            ContentValues pluginValues;
            PlugInInfo plugin;
            for (int i = 0; i < size; i++) {
                pluginValues = valueList[i] = new ContentValues();
                plugin = pluginList.get(i);
                pluginValues.put(PlugInColumns.NAME, plugin.name);
                pluginValues.put(PlugInColumns.PRODUCT_ID, plugin.product_id);
                pluginValues.put(PlugInColumns.VERSION_NAME, plugin.version_name);
                pluginValues.put(PlugInColumns.VERSION_CODE, plugin.version_code);
                pluginValues.put(PlugInColumns.TYPE, plugin.type);
                pluginValues.put(PlugInColumns.IS_APPLY,plugin.is_apply?1:0);
                pluginValues.put(PlugInColumns.JSON_STR, plugin.json_str);
                pluginValues.put(PlugInColumns.LOCAL_JSON_STR, plugin.local_json_str);
                pluginValues.put(PlugInColumns.SUPPORTED_MOD, plugin.supported_mod);
            }
            context.getContentResolver().bulkInsert(DownLoadProvider.getContentURI(context, DownLoadProvider.TABLE_PLUGIN), valueList);
        }
    }
    
    public static void bulkInsertPlugInFromProduct(Context context, List<Product> productList) {
        final int size = null == productList ? 0 : productList.size();
        if (size > 0) {
            ContentValues valueList[] = new ContentValues[size];
            ContentValues pluginValues;
            Product product;
            for (int i = 0; i < size; i++) {
                pluginValues = valueList[i] = new ContentValues();
                product = productList.get(i);
                pluginValues.put(PlugInColumns.NAME, product.name);
                pluginValues.put(PlugInColumns.PRODUCT_ID, product.product_id);
                pluginValues.put(PlugInColumns.VERSION_NAME, product.version_name);
                pluginValues.put(PlugInColumns.VERSION_CODE, product.version_code);
                pluginValues.put(PlugInColumns.TYPE, product.type);
                pluginValues.put(PlugInColumns.FILE_PATH, product.installed_file_path);
                pluginValues.put(PlugInColumns.IS_APPLY,0);
                if(product != null) {
                    pluginValues.put(PlugInColumns.LOCAL_JSON_STR, ProductJSONImpl.createJsonObjectString(product));
                }
                pluginValues.put(PlugInColumns.SUPPORTED_MOD, product.supported_mod);
            }
            context.getContentResolver().bulkInsert(DownLoadProvider.getContentURI(context, DownLoadProvider.TABLE_PLUGIN), valueList);
        }
    }
    
    public static int getPreferRotation(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        final String orientation = settings.getString(KEY_ORIENTATION_PREFERRED, ORIENTATION_LANDSCAPE);
        
        if (orientation.equals(ORIENTATION_PORTRAIT)) {
            return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else if (orientation.equals(ORIENTATION_LANDSCAPE)) {
            return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        } else {
            return ActivityInfo.SCREEN_ORIENTATION_USER;
        }
    }
}
