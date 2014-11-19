package com.borqs.se.home3d;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.os.StrictMode;

import com.borqs.market.db.DownloadInfo;
import com.borqs.market.utils.DataConvertUtils;
import com.borqs.market.wallpaper.WallpaperUtils;
import com.borqs.se.download.Utils;
import com.borqs.se.widget3d.House;
import com.support.StaticUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.borqs.market.utils.IntentUtil;
import com.borqs.market.utils.MarketUtils;
import com.borqs.se.engine.SECameraData;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.engine.SEUtils;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.home3d.ProviderUtils.ModelColumns;
import com.borqs.se.home3d.ProviderUtils.ObjectInfoColumns;
import com.borqs.se.home3d.ProviderUtils.ThemeColumns;
import com.borqs.se.shortcut.AppItemInfo;
import com.borqs.se.shortcut.LauncherModel;
import com.borqs.se.R;

import android.app.ActivityManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.MotionEvent;

public class HomeUtils {
    public static boolean DEBUG = false;
    public static final String TAG = "3DHome";
    public final static String SDCARD_PATH = Environment.getExternalStorageDirectory().getPath() +
            File.separator + TAG;
    public final static String THUMB_PATH = SDCARD_PATH + "/.thumb";
    public final static String TMPDATA_PATH = SDCARD_PATH + "/tmp";
    public final static String TMPDATA_IMAGE_PATH = TMPDATA_PATH + "/.tmp_icon";

    private static final int UNCONSTRAINED = -1;

    public static boolean hasSDCard() {
        String t = android.os.Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(t);
    }

    public static String getLocalFilePath(Context context, String name) {
        if (TextUtils.isEmpty(name)) {
            return context.getFilesDir() + File.separator + "local_resource";
        } else {
            return context.getFilesDir() + File.separator + "local_resource" + File.separator + name;
        }
    }

    public static String getDownloadedFilePath(Context context, DownloadInfo info) {
        if (WallpaperUtils.isWallpaper(info)) {
            return WallpaperUtils.getWallPaperPath(info.product_id);
        }

        String id = null == info ? null : info.product_id;
        if (TextUtils.isEmpty(id)) {
            return context.getFilesDir() + File.separator + "downloaded_resource";
        } else {
            return context.getFilesDir() + File.separator + "downloaded_resource" + File.separator + id;
        }
    }

    public static void moveAssetFilesToExternal(Context context, String src, String name) {
        try {
            String localFilePath = getLocalFilePath(context, name);
            String[] filePaths = context.getResources().getAssets().list(src);
            if (filePaths == null) {
                return;
            }
            for (String file : filePaths) {
                InputStream is = context.getResources().getAssets().open(src + File.separator + file);
                File parentFile = new File(localFilePath);
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
                File destFile = new File(localFilePath + File.separator + file);
                destFile.createNewFile();
                SEUtils.copyToFile(is, destFile);
                is.close();
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static Object invokeMethod(Object object, Method method, Object[] paras) {
        try {
            return method.invoke(object, paras);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Method getMethod(Class clazz, String name, Class[] paratype) {
        Method method = null;
        try {
            method = clazz.getMethod(name, paratype);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return method;
    }

    @SuppressWarnings("unchecked")
    public static Method getDeclaredMethod(Class clazz, String name, Class[] paratype) {
        Method method = null;
        try {
            method = clazz.getDeclaredMethod(name, paratype);
            method.setAccessible(true);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return method;
    }

    public static int getStatusbarHeight(Context context) {
        int height = 0;
        try {
            Class c = Class.forName("com.android.internal.R$dimen");
            Field field = c.getField("status_bar_height");
            field.setAccessible(true);
            int id = field.getInt(c);
            height = context.getResources().getDimensionPixelSize(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return height;
    }

    public static void saveBitmapToSdcard(Bitmap bitmap, String path) {
        File f = new File(Environment.getExternalStorageDirectory() + File.separator + path);
        try {
            f.createNewFile();
            FileOutputStream fOut = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveBitmap(Bitmap bitmap, String path, Bitmap.CompressFormat format) {
        try {
            File f = new File(path);
            f.createNewFile();
            FileOutputStream fOut = new FileOutputStream(f);
            bitmap.compress(format, 100, fOut);
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeBitmap(ContentValues values, Bitmap bitmap) {
        if (bitmap != null) {
            byte[] data = flattenBitmap(bitmap);
            values.put(ObjectInfoColumns.SHORTCUT_ICON, data);
        }
    }

    private static byte[] flattenBitmap(Bitmap bitmap) {
        // Try go guesstimate how much space the icon will take when serialized
        // to avoid unnecessary allocations/copies during the write.
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            if (DEBUG)
                Log.d("Favorite", "Could not write icon");
            return null;
        }
    }

    public static ComponentName getCategoryComponentName(Context context, String[] keywords) {
        ComponentName componentName = null;
        if (keywords != null) {
            List<AppItemInfo> apps = LauncherModel.getInstance().getApps();
            for (String keyword : keywords) {
                for (AppItemInfo info : apps) {
                    ResolveInfo ri = info.getResolveInfo();
                    if (ri.activityInfo.name.toLowerCase().contains(keyword)) {
                        componentName = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
                        break;
                    }
                    if (componentName != null) {
                        break;
                    }
                }
            }
        }

        return componentName;
    }

    public static String getAction(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            return "ACTION_DOWN";
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            return "ACTION_UP";
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            return "ACTION_MOVE";
        } else {
            return "ACTION_CANCEL";
        }
    }

    public static Bitmap createEmptyBitmap(int w, int h) {
        int newW = SEUtils.higherPower2(w);
        int newH = SEUtils.higherPower2(h);
        Bitmap des = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(des);
        canvas.drawColor(Color.TRANSPARENT);
        return des;
    }

    public static int[] getSpanForWidget(Context context, AppWidgetProviderInfo info) {
        return getSpanForWidget(context, info.provider, info.minWidth, info.minHeight);
    }

    private static int[] getSpanForWidget(Context context, ComponentName component, int minWidth, int minHeight) {
        Resources resources = context.getResources();
        Rect padding = getDefaultPaddingForWidget(context, component, null);
        int requiredWidth = minWidth + padding.left + padding.right;
        int requiredHeight = minHeight + padding.top + padding.bottom;
        return rectToCell(resources, requiredWidth, requiredHeight);
    }

    public static Rect getDefaultPaddingForWidget(Context context, ComponentName component, Rect padding) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo appInfo;

        if (padding == null) {
            padding = new Rect(0, 0, 0, 0);
        } else {
            padding.set(0, 0, 0, 0);
        }

        try {
            appInfo = packageManager.getApplicationInfo(component.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            // if we can't find the package, return 0 padding
            return padding;
        }

        if (appInfo.targetSdkVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Resources r = context.getResources();
            padding.left = r.getDimensionPixelSize(R.dimen.default_app_widget_padding_left);
            padding.right = r.getDimensionPixelSize(R.dimen.default_app_widget_padding_right);
            padding.top = r.getDimensionPixelSize(R.dimen.default_app_widget_padding_top);
            padding.bottom = r.getDimensionPixelSize(R.dimen.default_app_widget_padding_bottom);
        }
        return padding;
    }

    private static int[] rectToCell(Resources resources, int width, int height) {
        int actualWidth = resources.getDimensionPixelSize(R.dimen.workspace_cell_width);
        int actualHeight = resources.getDimensionPixelSize(R.dimen.workspace_cell_height);
        int smallerSize = Math.min(actualWidth, actualHeight);
        int spanX = (int) Math.ceil(width / (float) smallerSize);
        int spanY = (int) Math.ceil(height / (float) smallerSize);
        if (spanX < 1) {
            spanX = 1;
        } else if (spanX > 4) {
            spanX = 4;
        }
        if (spanY < 1) {
            spanY = 1;
        } else if (spanY > 4) {
            spanY = 4;
        }
        return new int[] { spanX, spanY };
    }

    @SuppressWarnings("unchecked")
    public static SEScene getSceneByClassName(String className, String sceneName) {
        try {
            Class<SEScene> clazz = (Class<SEScene>) Class.forName(className);
            Class[] paratype = { String.class };
            Constructor constructor = clazz.getConstructor(paratype);
            Object[] para = { sceneName };
            SEScene object = (SEScene) constructor.newInstance(para);
            return object;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getRomeNumber(int n) {
        String[] a = new String[] { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "", "X", "XX", "XXX",
                "XL", "L", "LX", "LXX", "LXXX", "XCC", "", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM" };
        int t, i, m;
        String result = "";
        for (m = 0, i = 1000; m < 3; m++, i /= 10) {
            t = (n % i) / (i / 10);
            result = result + a[(2 - m) * 10 + t];
        }
        return result;
    }

    public static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == UNCONSTRAINED) ? 128 : (int) Math.min(Math.floor(w / minSideLength),
                Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }

        if ((maxNumOfPixels == UNCONSTRAINED) && (minSideLength == UNCONSTRAINED)) {
            return 1;
        } else if (minSideLength == UNCONSTRAINED) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    public static boolean isPad(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static String getAppLabel(Context context, ResolveInfo resolveInfo) {
        if (resolveInfo == null) {
            return null;
        }
        PackageManager pm = context.getPackageManager();
        String label = resolveInfo.loadLabel(pm).toString();
        if (label == null) {
            label = resolveInfo.activityInfo.packageName;
        }
        return label;
    }

    private static Drawable getFullResIcon(Context context, ResolveInfo info) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        int iconDpi = activityManager.getLauncherLargeIconDensity();

        PackageManager pm = context.getPackageManager();
        Resources resources;
        try {
            resources = pm.getResourcesForApplication(info.activityInfo.applicationInfo);
        } catch (Exception e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = info.activityInfo.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId, iconDpi);
            }
        }
        return null;
    }

    private static Drawable getFullResIcon(Resources resources, int iconId, int iconDpi) {
        Drawable d;
        try {
            Method method = HomeUtils.getMethod(Resources.class, "getDrawableForDensity", new Class[] { int.class,
                    int.class });
            // d = resources.getDrawableForDensity(iconId, mIconDpi);
            method.setAccessible(true);
            d = (Drawable) method.invoke(resources, new Object[] { iconId, iconDpi });
        } catch (Exception e) {
            Log.d("AppItemInfo", "error : " + e.getMessage());
            d = null;
        }

        return d;
    }

    public static Drawable getAppIcon(Context context, ResolveInfo resolveInfo) {
        Resources resources = context.getResources();
        if (resolveInfo == null) {
            return resources.getDrawable(R.drawable.ic_launcher_application);
        }
        Drawable icon = getFullResIcon(context, resolveInfo);
        if (icon == null) {
            PackageManager pm = context.getPackageManager();
            icon = resolveInfo.loadIcon(pm);
        }
        if (icon == null) {
            icon = resources.getDrawable(R.drawable.ic_launcher_application);
        }
        return icon;
    }

    public static float getFontSize(Context context) {
        Configuration mCurConfig = new Configuration();
        Configuration config = new Configuration();
        android.provider.Settings.System.getConfiguration(context.getContentResolver(), config);
        Log.w(TAG, "getFontSize(), Font size is " + mCurConfig.fontScale);
        return mCurConfig.fontScale;
    }

    /**
     * Get sorted apps by name
     */
    public static ArrayList<AppItemInfo> getSortedAppsByName() {
        ArrayList<AppItemInfo> apps = LauncherModel.getInstance().getApps();
        try {
            Collections.sort(apps, new AppComparator());
        } catch (Exception e) {
        }
        return apps;
    }

    static class AppComparator implements Comparator<AppItemInfo> {
        public int compare(AppItemInfo o1, AppItemInfo o2) {
            return Collator.getInstance().compare(o1.getLabel(), o2.getLabel());
        }
    }

    private static int generateInSampleSize(BitmapFactory.Options options, int outW, int outH) {
        int height = options.outHeight;
        int width = options.outWidth;
        int size = 1;
        if (height > outH || width > outW) {
            if (width > height) {
                size = Math.round((float) height / (float) outH);
            } else {
                size = Math.round((float) width / (float) outW);
            }
        }
        return size;
    }

    public static Bitmap decodeSampledBitmapFromResource(String bf, int outW, int outH) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(bf, options);
        options.inSampleSize = generateInSampleSize(options, outW, outH);
        options.inJustDecodeBounds = false;
        options.inPreferQualityOverSpeed = true;
        return BitmapFactory.decodeFile(bf, options);
    }

    public static Bitmap decodeSampledBitmapFromResource(Context context, Uri uri, int outW, int outH) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
            options.inSampleSize = generateInSampleSize(options, outW, outH);
            options.inJustDecodeBounds = false;
            options.inPreferQualityOverSpeed = true;
            return BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
        } catch (Exception e) {
        }
        return null;
    }

    public static String createImageTmpFile() {
        File d = new File(TMPDATA_PATH);
        if (!d.exists()) {
            d.mkdirs();
        }
        String path = TMPDATA_IMAGE_PATH;
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    public static void deleteFile(String path) {
        File f = new File(path);
        if (!f.exists())
            return;
        if (!(f.delete())) {
            File subs[] = f.listFiles();
            for (int i = 0; i <= subs.length - 1; i++) {
                if (subs[i].isDirectory())
                    deleteFile(subs[i].getAbsolutePath());
                subs[i].delete();
            }
            f.delete();
        }
    }

    public static ModelInfo getModelInfoFromDB(Context context, String name) {
        String where = ModelColumns.OBJECT_NAME + "='" + name + "'";
        Cursor cursor = context.getContentResolver().query(ModelColumns.CONTENT_URI, null, where, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                ModelInfo modelInfo = ModelInfo.CreateFromDB(cursor);
                cursor.close();
                return modelInfo;
            }
            cursor.close();
        }
        return null;
    }

    /// 1. clear current possible applied theme id
    /// 2. set new applying theme id
    /// 3. set last used paper id for new theme
    public static void markThemeAsApply(Context context, String productId) {
        String where = ThemeColumns.IS_APPLY + "=" + 1;
        ContentValues values = new ContentValues();
        values.put(ThemeColumns.IS_APPLY, 0);
        context.getContentResolver().update(ThemeColumns.CONTENT_URI, values, where, null);

        values = new ContentValues();
        where = ThemeColumns.PRODUCT_ID + "='" + productId + "'";
        int status = 1;
        values.put(ThemeColumns.IS_APPLY, status);
        context.getContentResolver().update(ThemeColumns.CONTENT_URI, values, where, null);
        if (productId != null) {
            MarketUtils.updatePlugIn(context, productId, true, MarketUtils.CATEGORY_THEME);
            WallpaperUtils.clearAppliedFlag(context);
            String themePaperId = SettingsActivity.getWallpaperId(context, productId);
            if (!TextUtils.isEmpty(themePaperId)) {
                MarketUtils.updatePlugIn(context, themePaperId, true, MarketUtils.CATEGORY_WALLPAPER);
            }
        }
    }

    /// 1. set applying paper id
    /// 2. save using paper id for current theme, which restore while
    /// come back to the theme later, see to: markThemeAsApply()
    public static void markPaperAsApply(Context context, String productId) {
        MarketUtils.updatePlugIn(context, productId, true, MarketUtils.CATEGORY_WALLPAPER);
        ThemeInfo themeInfo = HomeManager.getInstance().getCurrentThemeInfo();
        if (null != themeInfo) {
            SettingsActivity.setWallpaperId(context, themeInfo.mProductID, productId);
        }
    }

    public static String getHouseOfTheme(Context context, String theme) {
        String where = ThemeColumns.NAME + "='" + theme + "'";
        String[] selection = { ThemeColumns.HOUSE_NAME };
        Cursor cursor = context.getContentResolver().query(ThemeColumns.CONTENT_URI, selection, where, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getString(0);
        }
        return null;
    }

    public static String getThemeNameByProductID(Context context, String productID) {
        String where = ThemeColumns.PRODUCT_ID + "='" + productID + "'";
        String[] selection = { ThemeColumns.NAME };
        Cursor cursor = context.getContentResolver().query(ThemeColumns.CONTENT_URI, selection, where, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getString(0);
        }
        return null;
    }

    public static void updateHouseName(Context context, String sceneName, String name) {
        String where = ObjectInfoColumns.SCENE_NAME + "='" + sceneName + "' AND " + ObjectInfoColumns.OBJECT_NAME
                + " LIKE 'house%'";
        ContentValues values = new ContentValues();
        values.put(ObjectInfoColumns.OBJECT_NAME, name);
        context.getContentResolver().update(ObjectInfoColumns.CONTENT_URI, values, where, null);
    }

    public static String deleteThemeDBByProductID(Context context, String productID) {
        String where = ThemeColumns.PRODUCT_ID + " = '" + productID + "'";
        String assetPath;
        String name;
        String sceneName;
        Cursor cursor = context.getContentResolver().query(ThemeColumns.CONTENT_URI, null, where, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            assetPath = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.FILE_PATH));
            name = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.NAME));
            sceneName = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.SCENE_NAME));
        } else {
            if (cursor != null) {
                cursor.close();
            }
            return null;
        }
        if (cursor != null) {
            cursor.close();
        }
        context.getContentResolver().delete(ThemeColumns.CONTENT_URI, where, null);
        if (!"home8".equals(sceneName)) {
            where = ObjectInfoColumns.SCENE_NAME + " = '" + sceneName + "'";
            context.getContentResolver().delete(ObjectInfoColumns.CONTENT_URI, where, null);
        }
        // 在老版本中资源包和wallpaper存在一个路径，假如是老版本上来的不删除这个文件夹
        if (!name.equals(new File(assetPath).getName()))
            deleteFile(assetPath);
        return name;
    }

    public static String deleteModelDBByProductID(Context context, String productID) {
        ContentResolver resolver = context.getContentResolver();
        String where = ModelColumns.PRODUCT_ID + " = '" + productID + "'";
        String[] selection = { ModelColumns.OBJECT_NAME, ModelColumns.ASSETS_PATH };
        Cursor cursor = resolver.query(ModelColumns.CONTENT_URI, selection, where, null, null);
        String name = null;
        String localPath = null;
        if (cursor != null && cursor.moveToFirst()) {
            name = cursor.getString(0);
            localPath = cursor.getString(1);
        } else {
            if (cursor != null) {
                cursor.close();
            }
            return null;
        }
        if (cursor != null) {
            cursor.close();
        }
        where = ModelColumns.OBJECT_NAME + "='" + name + "'";
        resolver.delete(ModelColumns.CONTENT_URI, where, null);
        deleteFile(localPath);
        return name;
    }

    public static String deleteModelAndObjectDBByProductID(Context context, String productID) {
        ContentResolver resolver = context.getContentResolver();
        String where = ModelColumns.PRODUCT_ID + " = '" + productID + "'";
        String[] selection = { ModelColumns.OBJECT_NAME, ModelColumns.ASSETS_PATH };
        Cursor cursor = resolver.query(ModelColumns.CONTENT_URI, selection, where, null, null);
        String name = null;
        String localPath = null;
        if (cursor != null && cursor.moveToFirst()) {
            name = cursor.getString(0);
            localPath = cursor.getString(1);
        } else {
            if (cursor != null) {
                cursor.close();
            }
            return null;
        }
        if (cursor != null) {
            cursor.close();
        }
        where = ModelColumns.OBJECT_NAME + "='" + name + "'";
        resolver.delete(ModelColumns.CONTENT_URI, where, null);
        where = ObjectInfoColumns.OBJECT_NAME + "='" + name + "'";
        resolver.delete(ObjectInfoColumns.CONTENT_URI, where, null);
        deleteFile(localPath);
        return name;
    }

    public static String deleteModelAndObjectDBByName(Context context, String name) {
        ContentResolver resolver = context.getContentResolver();
        String where = ModelColumns.OBJECT_NAME + " = '" + name + "'";
        String[] selection = { ModelColumns.PRODUCT_ID, ModelColumns.ASSETS_PATH };
        Cursor cursor = resolver.query(ModelColumns.CONTENT_URI, selection, where, null, null);
        String localPath = null;
        String productId = null;
        if (cursor != null && cursor.moveToNext()) {
            productId = cursor.getString(0);
            localPath = cursor.getString(1);
        } else {
            if (cursor != null) {
                cursor.close();
            }
            return null;
        }
        if (cursor != null) {
            cursor.close();
        }
        where = ModelColumns.OBJECT_NAME + "='" + name + "'";
        resolver.delete(ModelColumns.CONTENT_URI, where, null);
        where = ObjectInfoColumns.OBJECT_NAME + "='" + name + "'";
        resolver.delete(ObjectInfoColumns.CONTENT_URI, where, null);
        deleteFile(localPath);
        return productId;
    }

    public static long mPhoneMemory = 0;

    /**
     * @return total memory of phone (M)
     */
    public static long getPhoneMemory() {
        if (mPhoneMemory > 0) {
            return mPhoneMemory;
        }
        String path = "/proc/meminfo";
        String content = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path), 8);
            String line;
            if ((line = br.readLine()) != null) {
                content = line;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        int begin = content.indexOf(':');
        int end = content.indexOf('k');
        content = content.substring(begin + 1, end).trim();
        mPhoneMemory = Integer.parseInt(content) / 1024;
        return mPhoneMemory;
    }

    public static HashMap<String, String> loadSpecialAttributeForModel(Context context, ModelInfo modelInfo) {
        InputStream is = null;
        try {
            HashMap<String, String> values = new HashMap<String, String>();
            String assetPath = modelInfo.mAssetsPath;
            if (assetPath.contains("assets")) {
                // length of "assets/" is 7
                String filePath = assetPath.substring(7) + "/special_config.xml";
                is = context.getAssets().open(filePath);
            } else {
                is = new FileInputStream(assetPath + "/special_config.xml");
            }
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            XmlUtils.beginDocument(parser, "config");
            XmlUtils.nextElement(parser);
            while (true) {
                String name = parser.getName();
                if (TextUtils.isEmpty(name)) {
                    break;
                }
                values.put(name, parser.getAttributeValue(null, "value"));
                XmlUtils.nextElement(parser);
            }
            return values;
        } catch (XmlPullParserException e) {
        } catch (IOException e) {
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    public static SECameraData loadCameraDataFromXml(XmlPullParser parser) {
        SECameraData cameraData = new SECameraData();
        try {
            cameraData.mFov = Float.valueOf(parser.getAttributeValue(null, ThemeColumns.FOV));
            cameraData.mNear = Float.valueOf(parser.getAttributeValue(null, ThemeColumns.NEAR));
            cameraData.mFar = Float.valueOf(parser.getAttributeValue(null, ThemeColumns.FAR));
            XmlUtils.nextElement(parser);
            while (true) {
                String item = parser.getName();
                if (ThemeColumns.LOCATION.equals(item)) {
                    cameraData.mLocation = new SEVector3f();
                    cameraData.mLocation.initFromXml(parser);
                    XmlUtils.nextElement(parser);
                } else if (ThemeColumns.ZAXIS.equals(item)) {
                    cameraData.mAxisZ = new SEVector3f();
                    cameraData.mAxisZ.initFromXml(parser);
                    XmlUtils.nextElement(parser);
                } else if (ThemeColumns.UP.equals(item)) {
                    cameraData.mUp = new SEVector3f();
                    cameraData.mUp.initFromXml(parser);
                    XmlUtils.nextElement(parser);
                } else {
                    break;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cameraData;
    }

    public static void gotoAdRemovalActivity() {
        gotoProductDetailActivity(PID_AD_FIGHTER);
    }

    private static void gotoProductDetailActivity(String productId) {
        IntentUtil.startProductDetailActivity(SESceneManager.getInstance().getGLActivity(),
                productId, 0, null, Utils.isScreenOrientationPortrait());
    }

    // / wallpaper and user share begin
    private static final int EXPORT_VERSION = 1;
    public static String PKG_CURRENT_NAME = "com.borqs.se";
    private static String PKG_DATA_PATH;
    public static String PKG_FILES_PATH;
    public static String PKG_LIB_PATH;

    public static void attachApplication(Context context) {
        PKG_CURRENT_NAME = context.getPackageName();
        PKG_DATA_PATH = "/data/data/" + PKG_CURRENT_NAME;
        PKG_FILES_PATH = PKG_DATA_PATH + "/files";
        PKG_LIB_PATH = PKG_DATA_PATH + "/app_lib";

        testDebugFlags();
        testStrictMode();
        // final int exportVersion = SettingsActivity.getExportVersion(context);
        // SettingsActivity.setExportVersion(context, EXPORT_VERSION);
        final int exportVersion = EXPORT_VERSION;
        WallpaperUtils.setApplicationRootPath(SDCARD_PATH, exportVersion);
        DataConvertUtils.scanBuildingFromParentPath(WallpaperUtils.getWallpaperFolder(SDCARD_PATH));
    }

    private static boolean DEBUG_STRICT_MODE = false;
    private static boolean PROFILE_STARTUP = false;

    private static void testDebugFlags() {
        DEBUG = checkDebugFlag("/.debug");
        DEBUG_STRICT_MODE = checkDebugFlag("/.strict_mode");
        PROFILE_STARTUP = checkDebugFlag("/.startup");
    }

    private static boolean checkDebugFlag(String flag) {
        File file = new File(SDCARD_PATH + flag);
        return file.exists();
    }

    // / reference count that use for stack calls pairs of start/stop tracing,
    // which
    // / ensure only one call to Debug.startMethodTracing, and then call
    // Debug.stopMethodTracing if
    // / and only if the count decrease to 0.
    private static int tracingReference = 0;

    public static void startTracing() {
        startTracing(null);
    }

    public static void startTracing(String tag) {
        if (PROFILE_STARTUP) {
            if (tracingReference <= 0) {
                if (TextUtils.isEmpty(tag)) {
                    tag = "launcher";
                }
                final String traceDir = SDCARD_PATH + "/trace";
                final File dir = new File(traceDir);
                if (!dir.exists()) {
                    dir.mkdir();
                }
                android.os.Debug.startMethodTracing(traceDir + "/" + tag + "-" + System.currentTimeMillis());
                tracingReference = 1;
            } else {
                tracingReference++;
            }
        }
    }

    public static void stopTracing() {
        if (PROFILE_STARTUP && tracingReference > 0) {
            tracingReference--;
            if (tracingReference == 0) {
                android.os.Debug.stopMethodTracing();
            }
        }
    }

    private static void testStrictMode() {
        if (DEBUG_STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites()
                    .detectNetwork() // or .detectAll() for all detectable
                                     // problems
                    .penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects().penaltyLog().penaltyDeath().build());
        }
    }

    // / wallpaper and user share code end

    // / static stub begin

    public static void staticUsingDesk(Context context, String objName) {
        // 添加统计用户使用桌子的类型
        if (isKnownSquareDesk(objName)) {
            StaticUtil.onEvent(context, "use_square_desk_event");
        } else if (isKnownCircularDesk(objName)) {
            StaticUtil.onEvent(context, "use_circular_desk_enent");
        } else {
            StaticUtil.onEvent(context, "use_extra_desk_event");
        }
    }

    private static boolean isKnownSquareDesk(String objName) {
        if ("desk_fang".equalsIgnoreCase(objName)) {
            return true;
        }
        if ("desk_fang_land".equalsIgnoreCase(objName)) {
            return true;
        }
        return false;
    }

    private static boolean isKnownCircularDesk(String objectName) {
        if ("group_desk".equalsIgnoreCase(objectName)) {
            return true;
        }

        if ("group_desk_2".equalsIgnoreCase(objectName)) {
            return true;
        }
        return false;
    }

    public static void staticUsingTheme(Context context, String themeName) {
        // 添加用户使用主题的事件统计
        StaticUtil.onEvent(context, "use_" + themeName + "_theme_event");
        // todo: exclude count for switching to default theme.
        StaticUtil.onEvent(context, "use_extra_theme_event"); // count for theme
                                                              // switching
    }

    public static void showFeedbackActivity(Context context) {
        StaticUtil.showFeedbackActivity(context);
    }

    public static boolean staticPurchaseEvent(Context context, String event) {
        if ((HomeUtils.PKG_CURRENT_NAME + ".intent.action.PURCHASE").equals(event)) {
            StaticUtil.onEvent(context, "purchase_event");
            return true;
        }
        return false;
    }
    /// static stub end
    private static final String PID_WALL_SHELF = "com.borqs.se.auto_73m8ef8u45r9kbi1jom";
    private static final String PID_AD_FIGHTER = "com.borqs.se.object.fighter";
    public static boolean checkAndDownloadDecorator(boolean download) {
        if (isExistingProductId(PID_WALL_SHELF)) {
            return true;
        }

        if (download) {
            gotoProductDetailActivity(PID_WALL_SHELF);
        }

        return false;
    }

    public static boolean isExistingProductId(String productId) {
        boolean existing = false;
        final String whereProductId = ModelColumns.PRODUCT_ID + "=?";
        Cursor cursor = SESceneManager.getInstance().getGLActivity().getContentResolver().
                query(ModelColumns.CONTENT_URI,
                        new String[]{ModelColumns.OBJECT_NAME},
                        whereProductId, new String[]{productId},
                        null);
        if (cursor != null && cursor.getCount() > 0) {
            existing = true;
        }
        if (cursor != null) {
            cursor.close();
        }
        return existing;
    }

    public static String getDefaultThemeId(String themeName) {
        return HomeUtils.PKG_CURRENT_NAME + ".theme." + themeName;
    }

    public static String getDefaultObjectId(String name) {
        return HomeUtils.PKG_CURRENT_NAME + ".object." + name;
    }

    /// hard code component name, identical to that in AndroidManifest
    public static String encodeSearchComponentName() {
        return HomeUtils.PKG_CURRENT_NAME + "/.home3d.SearchActivity";
    }

    public static String encodeProviderAuthority() {
        return HomeUtils.PKG_CURRENT_NAME + ".settings";
    }

    public static void gotoSettingsActivity() {
        Intent intentHome = new Intent();
        intentHome.setAction(HomeUtils.PKG_CURRENT_NAME + ".settings");
        intentHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        HomeManager.getInstance().startActivity(intentHome);
    }

    /// hard code component name end.

    public static void deleteWallpaperByLocalPath(Context context, String localPath) {
        deleteFile(localPath);
    }

    public static int getLabelShownPreference(Context context) {
        return SettingsActivity.getItemLabelShowOnWall(context);
    }
}
