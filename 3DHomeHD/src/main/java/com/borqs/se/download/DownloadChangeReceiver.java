package com.borqs.se.download;


import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.borqs.market.db.DownloadInfo;
import com.borqs.market.json.Product;
import com.borqs.market.utils.MarketUtils;
import com.borqs.market.wallpaper.WallpaperUtils;
import com.borqs.se.home3d.HomeDataBaseHelper;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.ProviderUtils.ModelColumns;
import com.borqs.se.home3d.ProviderUtils.ObjectInfoColumns;
import com.borqs.se.home3d.ProviderUtils.PluginColumns;
import com.borqs.se.home3d.ProviderUtils.Tables;
import com.borqs.se.home3d.ProviderUtils.ThemeColumns;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.widget3d.House;

public class DownloadChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        final String productId = intent.getStringExtra(MarketUtils.EXTRA_PRODUCT_ID);
        if (intent.getAction().equals(MarketUtils.ACTION_MARKET_DOWNLOAD_COMPLETE)) {
            final DownloadInfo testInfo  = WallpaperUtils.getExportInfoByProductId(context, productId,
                    House.IsScreenOrientationPortrait(context));
            if (null != testInfo) {
                new Thread() {
                    public void run() {
                        Utils.installWallpaper(context, testInfo);
                        saveToPlugTable(context, testInfo);
                        Utils.applyWallpaper(context, productId);
                    }
                }.start();
            }

            final DownloadInfo info = MarketUtils.getDownloadInfoByProductId(context, productId);
            if(info == null) return;
            if (MarketUtils.CATEGORY_THEME.equals(info.type)) {
                new Thread() {
                    public void run() {
                        Utils.installTheme(context, info);
                        saveToPlugTable(context, info);
                    }
                }.start();
            } else if (MarketUtils.CATEGORY_WALLPAPER.equalsIgnoreCase(info.type)) {
                new Thread() {
                    public void run() {
                        Utils.installWallpaper(context, info);
                        saveToPlugTable(context, info);
                    }
                }.start();
            } else if (MarketUtils.CATEGORY_OBJECT.equals(info.type)) {
                new Thread() {
                    public void run() {
                        Utils.installObject(context, info);
                        saveToPlugTable(context, info);
                    }
                }.start();
            }
        } else if (intent.getAction().equals(MarketUtils.ACTION_MARKET_THEME_APPLY)) {
            final DownloadInfo info = MarketUtils.getDownloadInfoFromPlugIn(context, productId);
            if(info == null) return;
            if (MarketUtils.CATEGORY_THEME.equals(info.type)) {
                new Thread(){
                    @Override
                    public void run() {
                        String where = ThemeColumns.PRODUCT_ID  + " = '" + productId + "'";
                        Cursor cursor = context.getContentResolver().query(ThemeColumns.CONTENT_URI, null, where, null, null);
                        if (cursor != null && cursor.getCount() == 0) {
                            Utils.installTheme(context, MarketUtils.getDownloadInfoByProductId(context, productId));
							MarketUtils.insertPlugIn(context, info, false, new File(Utils.parseTargetPath(context, info)));
                            cursor = context.getContentResolver().query(ThemeColumns.CONTENT_URI, null, where, null, null);
                        }
                        if  ( cursor != null && cursor.moveToFirst()) {
                            String themeName = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.NAME));
                            String localPath = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.FILE_PATH));
                            int type = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns.TYPE));
                            int applyStatus = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns.IS_APPLY));
                            String themeConfig = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.CONFIG));
                            SettingsActivity.saveThemeName(context, themeName, themeConfig);
                            Utils.markAsApply(context, localPath);
                            String product_id = HomeUtils.PKG_CURRENT_NAME + "." + themeName;
                            MarketUtils.updatePlugIn(context, product_id, true);
                        }
                    }
                }.start();
            } else if (MarketUtils.CATEGORY_WALLPAPER.equalsIgnoreCase(info.type)) {
                new Thread(){
                    @Override
                    public void run() {
                    	Utils.applyWallpaper(context, info.product_id);
                    }
                }.start();
            } else if (MarketUtils.CATEGORY_OBJECT.equals(info.type)) {
                new Thread(){
                    public void run(){
                    	addObjectToWall(info.product_id);
//                        Utils.installObject(context, info);
//                        MarketUtils.insertPlugIn(context, info, true, new File(Utils.parseTargetPath(context, info)));
                    	
                    }
                }.start();
            }
        } else if (intent.getAction().equals(MarketUtils.ACTION_MARKET_THEME_DELETE)) {
            final DownloadInfo info = MarketUtils.getDownloadInfoFromPlugIn(context, productId);
            if(info == null) return;
            if (MarketUtils.CATEGORY_THEME.equals(info.type)) {
                new Thread(){

                    @Override
                    public void run() {
                        String where = ThemeColumns.PRODUCT_ID  + " = '" + productId + "'";
                        Cursor cursor = context.getContentResolver().query(ThemeColumns.CONTENT_URI, null, where, null, null);
                        if  ( cursor != null && cursor.moveToFirst()) {
                            String themeName = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.NAME));
                            String localPath = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.FILE_PATH));
                            int type = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns.TYPE));
                            int applyStatus = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns.IS_APPLY));
                            String themeConfig = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.CONFIG));
                            if (applyStatus == 1) {
                                Utils.markAsApply(context, LocalThemePreview.NATIVE_DEFAULT_THEME_PATH);
                                SettingsActivity.saveThemeName(context,  "default", "");
                            }
                            Utils.deleteThemes(context, localPath);

//                            String product_id = HomeUtils.PKG_CURRENT_NAME + "." + themeName;
//                            MarketUtils.deletePlugIn(context, product_id);
                            MarketUtils.deletePlugIn(context, productId);
                        }
                    }
                    
                }.start();
            } else if (MarketUtils.CATEGORY_WALLPAPER.equalsIgnoreCase(info.type)) {
                new Thread(){

                    @Override
                    public void run() {
                        String where = ThemeColumns.PRODUCT_ID  + " = '" + productId + "'";
                        Cursor cursor = context.getContentResolver().query(ThemeColumns.CONTENT_URI, null, where, null, null);
                        if  ( cursor != null && cursor.moveToFirst()) {
                            String localPath = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.FILE_PATH));
                            int applyStatus = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns.IS_APPLY));
                            if (applyStatus == 1) {
                                Utils.markAsApply(context, LocalThemePreview.NATIVE_DEFAULT_THEME_PATH);
                                SettingsActivity.saveThemeName(context,  "default", "");
                            }
                            Utils.deleteThemes(context, localPath);
                            MarketUtils.deletePlugIn(context, productId);
                        }
                    }

                }.start();
            } else if (MarketUtils.CATEGORY_OBJECT.equals(info.type)) {
                new Thread() {
                    @Override
                    public void run() {
                        HomeDataBaseHelper help = HomeDataBaseHelper.getInstance(context);
                        SQLiteDatabase db = help.getWritableDatabase();
                        String table = Tables.MODEL_INFO;
                        SESceneManager.getInstance().removeModelFromScene(info.name.toLowerCase());
                        String where = ModelColumns.OBJECT_NAME + "='" + info.name.toLowerCase() + "'";
                        context.getApplicationContext().getContentResolver().delete(ModelColumns.CONTENT_URI, where, null);
                        table = Tables.OBJECTS_INFO;
                        where = ObjectInfoColumns.OBJECT_NAME + "='" + info.name.toLowerCase() + "'";
                        context.getApplicationContext().getContentResolver().delete(ModelColumns.CONTENT_URI, where, null);
                        db.delete(table, where, null);
                        String path = context.getFilesDir() + File.separator + info.name.toLowerCase();
                        File fileLocal = new File(path);
                        if (fileLocal.isDirectory()) {
                            for (File f : fileLocal.listFiles()) {
                                f.delete();
                            }
                        }
                        fileLocal.delete();
//                        String product_id = HomeUtils.PKG_CURRENT_NAME + "." + info.mName;
//                        MarketUtils.deletePlugIn(context, product_id);
                        MarketUtils.deletePlugIn(context, productId);
                    }

                }.start();
            }
        }
        
    }

    public void saveToPlugTable(Context context, DownloadInfo info){
        Cursor cursor = MarketUtils.queryPlugIn(context, info.product_id);
        if (cursor != null && cursor.moveToFirst()) {
            int version = cursor.getInt(cursor.getColumnIndexOrThrow(PluginColumns.VERSION_CODE));
            if (version < info.version_code) {
            	 if(Product.ProductType.WALL_PAPER.equalsIgnoreCase(info.type)) {
                     MarketUtils.updatePluginWithDownloadInfo(context, info, new File(WallpaperUtils.getWallPaperPath(info.product_id)));
                 }else if(Product.ProductType.OBJECT.equalsIgnoreCase(info.type)) {
                 	MarketUtils.updatePluginWithDownloadInfo(context, info, new File(Utils.getObjectPath(info.product_id)));
                 }else {
                     MarketUtils.updatePluginWithDownloadInfo(context, info, new File(Utils.parseTargetPath(context, info)));
                 }
            }
        } else {
            if(Product.ProductType.WALL_PAPER.equalsIgnoreCase(info.type)) {
                MarketUtils.insertPlugIn(context, info, false, new File(WallpaperUtils.getWallPaperPath(info.product_id)));
            }else if(Product.ProductType.OBJECT.equalsIgnoreCase(info.type)) {
            	MarketUtils.insertPlugIn(context, info, false, new File(Utils.getObjectPath(info.product_id)));
            }else {
                MarketUtils.insertPlugIn(context, info, false, new File(Utils.parseTargetPath(context, info)));
            }
        }
    }
    
    private static final HashMap<String,WeakReference<selectObjectListener>> selectObjectlisteners = new HashMap<String,WeakReference<selectObjectListener>>();
    
	public static void registerSelectObjectListener(String key,selectObjectListener listener){
		synchronized(selectObjectlisteners)
		{
			WeakReference<selectObjectListener> ref = selectObjectlisteners.get(key);
			if(ref != null && ref.get() != null)
			{
				ref.clear();
			}
			selectObjectlisteners.put(key, new WeakReference<selectObjectListener>(listener));
		}
	}
	
	public static void unregisterSelectObjectListener(String key){
		synchronized(selectObjectlisteners)
		{
			WeakReference<selectObjectListener> ref = selectObjectlisteners.get(key);
			if(ref != null && ref.get() != null)
			{
				ref.clear();
			}
			selectObjectlisteners.remove(key);
		}
	}
    
	private static void addObjectToWall(String proName) {
        synchronized (selectObjectlisteners) {
            Set<String> set = selectObjectlisteners.keySet();
            Iterator<String> it = set.iterator();
            while (it.hasNext()) {
                String key = it.next();
                if (selectObjectlisteners.get(key) != null) {
                	selectObjectListener listener = selectObjectlisteners.get(key).get();
                    if (listener != null) {
                        listener.selectObjectCallBack(proName);
                    }
                }
            }
        }
    }
    public interface selectObjectListener {
    	public void selectObjectCallBack(String proName);
    }
}
