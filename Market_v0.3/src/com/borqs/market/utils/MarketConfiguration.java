package com.borqs.market.utils;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;

import com.borqs.market.account.AccountSession;
import com.borqs.market.db.DownLoadHelper;
import com.borqs.market.db.DownloadInfoColumns;
import com.borqs.market.json.Product;
import com.iab.engine.MarketBillingResult;

public final class MarketConfiguration {
    
    public static final String SHARE_PREFERENCES_MARKET_CONFIG = "market_config";
    public static final String SP_EXTRAS_INIT_LOCAL_DATA = "SP_EXTRAS_INIT_LOCAL_DATA";
    
    public static final String SP_EXTRAS_INIT_ASSERT_OBJECT = "SP_EXTRAS_INIT_ASSERT_OBJECT";
    
    private static Context context;
    private static File cacheDirctory;
    private static File downloadDirctory;
    private static File externalDir;
    public static String PACKAGE_NAME = null;
    public static int VERSION_CODE = 0;
    
    private static boolean IS_DEBUG_TESTSERVER = false;
    private static boolean IS_DEBUG_SUGGESTION = false;
    private static boolean IS_DEBUG_BETA_REQUEST = false;
    
    public static boolean isIS_DEBUG_TESTSERVER() {
        return IS_DEBUG_TESTSERVER;
    }
    public static void setIS_DEBUG_TESTSERVER(boolean iS_DEBUG_TESTSERVER) {
        IS_DEBUG_TESTSERVER = iS_DEBUG_TESTSERVER;
    }
    public static boolean isIS_DEBUG_SUGGESTION() {
        return IS_DEBUG_SUGGESTION;
    }
    public static void setIS_DEBUG_SUGGESTION(boolean iS_DEBUG_SUGGESTION) {
        IS_DEBUG_SUGGESTION = iS_DEBUG_SUGGESTION;
    }
    public static boolean isIS_DEBUG_BETA_REQUEST() {
        return IS_DEBUG_BETA_REQUEST;
    }
    public static void setIS_DEBUG_BETA_REQUEST(boolean iS_DEBUG_BETA_REQUEST) {
        IS_DEBUG_BETA_REQUEST = iS_DEBUG_BETA_REQUEST;
    }
    public static void init(Context mContext) {
        context = mContext.getApplicationContext();
        PACKAGE_NAME = context.getApplicationInfo().packageName;
        
        try {
            VERSION_CODE = context.getPackageManager()
                .getPackageInfo(PACKAGE_NAME,PackageManager.GET_UNINSTALLED_PACKAGES).versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
//        MarketConfiguration.cacheDirctory = StorageUtils.getIndividualCacheDirectory(context,null);
//        MarketConfiguration.downloadDirctory = StorageUtils.getIndividualDownloadDirectory(context,null);
        QiupuHelper.formatUserAgent(context);
        if(!AccountSession.isLogin) {
            AccountSession.loadAccount(context);
        }
		  initDownloadingMap();
    }
    
    public static int billingType = MarketBillingResult.TYPE_IAB;
    public static void setPayType(int billType) {
        billingType = billType;
    }
    
    public static String IABKEY = null;
    public static void setIabKey(String iabKey) {
        IABKEY = iabKey;
    }
    
    public static void setExternalFilesDir(File externalFilesDir) {
        if(context == null) {
            throw new IllegalAccessError("context is null,plase call init()");
        }else {
            if(externalFilesDir != null) {
//                MarketConfiguration.cacheDirctory = StorageUtils.getIndividualCacheDirectory(context,externalFilesDir.getPath());
//                MarketConfiguration.downloadDirctory = StorageUtils.getIndividualDownloadDirectory(context,externalFilesDir.getPath());
                externalDir = externalFilesDir;
            }
        }
    }
    
    public static void initLocalWallPaper(final Context context, final String parentPath) {
//        SharedPreferences sp = getMarketConfigPreferences(context);
//        boolean isInited = sp.getBoolean(SP_EXTRAS_INIT_LOCAL_DATA, false);
//        if(!isInited) {
            ThemeXmlParser.initLocalData(context, parentPath);
//        }
    }
    
    public static void initLocalObjects(final Context context, final List<Product> productList) {
        SharedPreferences sp = getMarketConfigPreferences(context);
        boolean isInited = sp.getBoolean(SP_EXTRAS_INIT_ASSERT_OBJECT, false);
        if(!isInited) {
        	MarketUtils.bulkInsertPlugInFromProduct(context, productList);
        	saveBooleanMarketConfigPreferences(context, SP_EXTRAS_INIT_ASSERT_OBJECT, true);
        }
    }
    
    private static void saveBooleanMarketConfigPreferences(Context context, String key, boolean value) {
        SharedPreferences settings = getMarketConfigPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }
    
    public static SharedPreferences getMarketConfigPreferences(Context context) {
        return context.getSharedPreferences(SHARE_PREFERENCES_MARKET_CONFIG, Context.MODE_PRIVATE);
    }
    
    public static File getCacheDirctory() {
        if(cacheDirctory == null) {
            if(externalDir != null) {
                cacheDirctory = StorageUtils.getIndividualCacheDirectory(context,externalDir.getPath());
            }else {
                cacheDirctory = StorageUtils.getIndividualCacheDirectory(context,null);
            }
        }else {
            if(!cacheDirctory.exists()) {
                cacheDirctory.mkdirs();
            }
        }
        return cacheDirctory;
    }
    
    public static File getDownloadDirctory() {
        if(downloadDirctory == null) {
            if(externalDir != null) {
                downloadDirctory = StorageUtils.getIndividualDownloadDirectory(context,externalDir.getPath());
            }else {
                downloadDirctory = StorageUtils.getIndividualDownloadDirectory(context,null);
            }
        }else {
            if(!downloadDirctory.exists()) {
                downloadDirctory.mkdirs();
            }
        }
        return downloadDirctory;
    }
    public static String getPackageName() {
        return PACKAGE_NAME;
    }
    
    private static void initDownloadingMap() {
        Cursor cursor = DownLoadHelper.queryAllDownloading(context);
        if(cursor != null && cursor.getCount() > 0) {
            while(cursor.moveToNext()) {
                long downloadId = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadInfoColumns.DOWNLOAD_ID));
                String productId = cursor.getString(cursor.getColumnIndexOrThrow(DownloadInfoColumns.PRODUCT_ID));
                QiupuHelper.addDownloading(productId, downloadId);
            }
        }
    }
    
}
