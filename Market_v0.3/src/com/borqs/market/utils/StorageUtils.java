package com.borqs.market.utils;

import static android.os.Environment.MEDIA_MOUNTED;

import java.io.File;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;

public final class StorageUtils {
    public static final String TAG = StorageUtils.class.getSimpleName();
    private static final String EXTERNAL_STORAGE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static final String INDIVIDUAL_MARKET_DIR_NAME = "market";
    private static final String INDIVIDUAL_CACHE_DIR_NAME = ".image_cache";
    private static final String INDIVIDUAL_DOWNLOAD_DIR_NAME = "download";

    private StorageUtils() {}
    
    public static File getCacheDirectory(Context context) {
        File cacheDirctory = null;
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && hasExternalStoragePermission(context)) {
            cacheDirctory = context.getExternalCacheDir();
        }
        
        if(cacheDirctory == null) {
            cacheDirctory = context.getCacheDir();
        }
        
        if(cacheDirctory == null) {
            BLog.w(TAG, "Can't define system cache directory!");
            //try agin
            cacheDirctory = context.getCacheDir(); 
        }
        return cacheDirctory;
    }
    
    private static File getIndividualMarketDirectory(Context context,String ownCacheDir) {
        File cacheDir;
        if(TextUtils.isEmpty(ownCacheDir)) {
            cacheDir = getCacheDirectory(context);
        }else {
            cacheDir = getOwnCacheDirectory(context, ownCacheDir);
        }
        File individualCacheDir = new File(cacheDir + File.separator + INDIVIDUAL_MARKET_DIR_NAME);
        if (!individualCacheDir.exists()) {
            if (!individualCacheDir.mkdirs()) {
                individualCacheDir = cacheDir;
            }
        }
        return individualCacheDir;
    }
    public static File getIndividualCacheDirectory(Context context,String ownCacheDir) {
        File cacheDir = getIndividualMarketDirectory(context,ownCacheDir);
        File individualCacheDir = new File(cacheDir, INDIVIDUAL_CACHE_DIR_NAME);
        if (!individualCacheDir.exists()) {
            if (!individualCacheDir.mkdirs()) {
                individualCacheDir = cacheDir;
            }
        }
        return individualCacheDir;
    }
    public static File getIndividualDownloadDirectory(Context context,String ownCacheDir) {
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && hasExternalStoragePermission(context)) {
            File cacheDir = getIndividualMarketDirectory(context,ownCacheDir);
            File individualCacheDir = new File(cacheDir, INDIVIDUAL_DOWNLOAD_DIR_NAME);
            if (!individualCacheDir.exists()) {
                if (!individualCacheDir.mkdirs()) {
                    individualCacheDir = cacheDir;
                }
            }
            return individualCacheDir;
        }else {
            return null;
        }
    }
    
    public static File getOwnCacheDirectory(Context context, String cacheDir) {
        File appCacheDir = null;
        if (Environment.getExternalStorageState().equals(MEDIA_MOUNTED) && hasExternalStoragePermission(context)) {
            appCacheDir = new File(cacheDir);
        }
        if (appCacheDir == null || (!appCacheDir.exists() && !appCacheDir.mkdirs())) {
            appCacheDir = context.getCacheDir();
        }
        return appCacheDir;
    }
    
    public static boolean hasExternalStoragePermission(Context context) {
        return PackageManager.PERMISSION_GRANTED == context.checkCallingOrSelfPermission(EXTERNAL_STORAGE_PERMISSION);
    }
}
