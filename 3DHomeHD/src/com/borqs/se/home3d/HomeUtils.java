package com.borqs.se.home3d;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.borqs.se.engine.SEScene;
import com.support.StaticUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Xml;
import android.view.MotionEvent;

import com.borqs.freehdhome.R;
import com.borqs.market.json.Product;
import com.borqs.market.utils.DataConvertUtils;
import com.borqs.market.utils.MarketConfiguration;
import com.borqs.market.utils.ThemeXmlParser;
import com.borqs.market.wallpaper.WallpaperUtils;
import com.borqs.se.download.Utils;
import com.borqs.se.home3d.ProviderUtils.ModelColumns;
import com.borqs.se.home3d.ProviderUtils.ObjectInfoColumns;
import com.borqs.se.home3d.ProviderUtils.Tables;
import com.borqs.se.engine.SESceneInfo;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.widget3d.AppObject;
import com.borqs.se.widget3d.House;
import com.borqs.se.widget3d.NormalObject;
import com.borqs.se.widget3d.ObjectInfo;

//import org.acra.ACRA;

public class HomeUtils {
    public static final String TAG = "3DHomeHD";
    public static final String ACRA_TAG = "Send by ourself:";
    public static boolean DEBUG = false;
    public final static int FONT_SIZE = 38;
    public final static int FONT_COLOR = 0xfff8f8ff;
    public final static int STROKE_WIDTH = 18;
    public final static int ICON_PADDING = 3;
    //A combination of pictures and background together, so the spacing is greater than 20.
    public final static int ICON_BACKGROUND_SPACING = 20;

    public final static String SDCARD_PATH = Environment.getExternalStorageDirectory().getPath() + "/borqs/apps/freehdhome";

    public final static String THUMB_PATH = SDCARD_PATH + "/.thumb";
    
    private static final int UNCONSTRAINED = -1;
    
    public static final String LOCKSCREEN_HOMEHD_PKG = "com.borqs.lockscreenhd";
    
    public static boolean hasSDCard() {
        String t = android.os.Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(t);
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
        File f = new File("/sdcard/" + path);
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
        WallpaperUtils.saveBitmap(bitmap, path, format);
    }

    public static void saveBitmap(Bitmap bitmap, String path, String name) {

        try {
            File dir = new File(PKG_FILES_PATH + "/" + path);
            File f = new File(dir, name);
            if (!dir.exists()) {
                dir.mkdir();
            }
            f.createNewFile();
            FileOutputStream fOut = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
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

    /**
     * Query the package manager for MAIN/LAUNCHER activities in the supplied
     * package.
     */
    public static ResolveInfo findResolveInfoByComponent(Context context, ComponentName component) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent();
        intent.setComponent(component);
        List<ResolveInfo> apps = packageManager.queryIntentActivities(intent, 0);
        if (apps.size() > 0) {
            return apps.get(0);
        }
        return null;
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
        int newW = HomeUtils.higherPower2(w);
        int newH = HomeUtils.higherPower2(h);
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
        Rect padding = getDefaultPaddingForWidget(context,component,null);
        int requiredWidth = minWidth + padding.left + padding.right;
        int requiredHeight = minHeight + padding.top + padding.bottom;
        return rectToCell(resources, requiredWidth, requiredHeight);
    }
    
    public static Rect getDefaultPaddingForWidget(Context context, ComponentName component,
            Rect padding) {
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

    public static NormalObject getObjectByClassName(SEScene scene, ObjectInfo info) {
        NormalObject obj = null;
        try {
        	Class<NormalObject> clazz;
        	if(info.mModelInfo == null && AppObject.isValidAppName(info.mName)) {
        		clazz = (Class<NormalObject>) Class.forName("com.borqs.se.widget3d.AppObject");
        	}else {
        		clazz = (Class<NormalObject>) Class.forName(info.mClassName);
        	}
        	Class[] paratype = { SEScene.class, String.class, int.class };
        	Constructor constructor = clazz.getConstructor(paratype);
        	Object[] para = { scene, info.mName, info.mIndex };
        	obj = (NormalObject) constructor.newInstance(para);
        	obj.setObjectInfo(info);
        } catch (Exception e) {
            return new NormalObject(scene, info.mName, info.mIndex);
        }
        return obj;
    }

    public static boolean isPower2(int v) {
        if (v >= 0)
            return (v & (v - 1)) == 0;
        else
            return false;
    }

    public static int higherPower2(int v) {
        if (isPower2(v))
            return v;
        return 1 << (findHighBit(v) + 1);
    }

    public static int lowerPower2(int v) {
        if (isPower2(v))
            return v;
        return 1 << (findHighBit(v));
    }

    public static int findHighBit(int v) {
        int bit = 0;
        while (v > 1) {
            bit++;
            v >>= 1;
        }
        return bit;
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
    
    public static int computeSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength,
                maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8 ) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 :
                (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == UNCONSTRAINED) ? 128 :
                (int) Math.min(Math.floor(w / minSideLength),
                Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }

        if ((maxNumOfPixels == UNCONSTRAINED) &&
                (minSideLength == UNCONSTRAINED)) {
            return 1;
        } else if (minSideLength == UNCONSTRAINED) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    public static final String HOME_SETTING_ACTION = "com.borqs.se.freehdhome.settings";

    public static String getAppLable(Context context, ResolveInfo resolveInfo) {
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
        int iconDpi = -1;
        PackageManager pm = context.getPackageManager();
        int density = context.getResources().getDisplayMetrics().densityDpi;
        final int screenSize = context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK;
        boolean isScreenLarge = screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE
                || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE;
        if (isScreenLarge) {
            if (density == DisplayMetrics.DENSITY_LOW) {
                iconDpi = DisplayMetrics.DENSITY_MEDIUM;
            } else if (density == DisplayMetrics.DENSITY_MEDIUM) {
                iconDpi = DisplayMetrics.DENSITY_HIGH;
            } else if (density == DisplayMetrics.DENSITY_HIGH) {
                iconDpi = DisplayMetrics.DENSITY_XHIGH;
            } else if (density == DisplayMetrics.DENSITY_XHIGH) {
                // We'll need to use a denser icon, or some sort of a mipmap
                iconDpi = DisplayMetrics.DENSITY_XHIGH;
            }
        } else {
            iconDpi = context.getResources().getDisplayMetrics().densityDpi;
        }

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
            Method method = HomeUtils.getMethod(Resources.class, "getDrawableForDensity", new Class[]{int.class,
                    int.class});
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
        return BitmapFactory.decodeFile(bf, options);
    }

    public static Bitmap decodeSampledBitmapFromResource(Context context, Uri uri, int outW, int outH) {
        try {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
        options.inSampleSize = generateInSampleSize(options, outW, outH);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
        } catch (Exception e) {
        }
        return null;
    }

//    public static String createImageTmpFile() {
//        File d = new File(TMPDATA_PATH);
//        if (!d.exists()) {
//            d.mkdirs();
//        }
//        String path = TMPDATA_IMAGE_PATH;
//        File f = new File(path);
//        if (f.exists()) {
//            f.delete();
//        }
//        try {
//            f.createNewFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return path;
//    }

    private static final int EXPORT_VERSION = 1;
    public static String PKG_CURRENT_NAME = "com.borqs.freehdhome";
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
        final int exportVersion = SettingsActivity.getExportVersion(context);
        WallpaperUtils.setApplicationRootPath(SDCARD_PATH, exportVersion);
        SettingsActivity.setExportVersion(context, EXPORT_VERSION);
        DataConvertUtils.scanBuildingFromParentPath(WallpaperUtils.getWallpaperFolder(SDCARD_PATH));
    }

    public static boolean isCurrentPackage(String packageName) {
        return PKG_CURRENT_NAME.equals(packageName);
    }

    public static void ensureDataFilesPath(Context context) {
        if (TextUtils.isEmpty(PKG_FILES_PATH)) {
            attachApplication(context.getApplicationContext());
        }

        File file = new File(HomeUtils.PKG_FILES_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
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

    /// reference count that use for stack calls pairs of start/stop tracing, which
    /// ensure only one call to Debug.startMethodTracing, and then call Debug.stopMethodTracing if
    /// and only if the count decrease to 0.
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
                android.os.Debug.startMethodTracing(traceDir + "/"
                        + tag + "-" + System.currentTimeMillis());
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
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
    }

    private static String TEMP_IMAGE_PATH = SDCARD_PATH + "/" + ".tempimage";
    public static File getTempImageFile() {
        File dir = new File(SDCARD_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return new File(TEMP_IMAGE_PATH);
    }

    public static String[] queryNativeSceneFiles(Context context) {
        String[] fileNames;
        try {
            Context targetContext = context.createPackageContext(PKG_CURRENT_NAME, Context.CONTEXT_IGNORE_SECURITY);

            fileNames = targetContext.getAssets().list(SESceneInfo.DEFAULT_SCENE_CONFIG_PATH);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            fileNames = null;
        } catch (Exception ex) {
            ex.printStackTrace();
            fileNames = null;
        }
        return fileNames;
    }

    public static InputStream openNativeSceneXmlStream(Context context, String fileName) {
        InputStream is;
        try {
            Context targetContext = context.createPackageContext(PKG_CURRENT_NAME, Context.CONTEXT_IGNORE_SECURITY);
            is = targetContext.getAssets().open(SESceneInfo.DEFAULT_SCENE_CONFIG_PATH + "/" + fileName);
        } catch (IOException e) {
            e.printStackTrace();
            is = null;
        } catch (Exception e) {
            e.printStackTrace();
            is = null;
        }
        return is;
    }

    public static XmlPullParser getNativeSceneXmlParser(InputStream is) {
        XmlPullParser parser;
        try {
            parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            XmlUtils.beginDocument(parser, "scene");
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            parser = null;
        } catch (IOException e) {
            e.printStackTrace();
            parser = null;
        }
        return parser;
    }

    public static void showWallpapers(Context context) {
        WallpaperUtils.ENABLE_BACKDOOR = BackDoorSettingsActivity.isEnableBackdoor(context);

//        final ArrayList<RawPaperItem> wallPaperItems = new ArrayList<RawPaperItem>();
//        Utils.getRawPaperItems(context, wallPaperItems);

        WallpaperUtils.startProductListIntent(context, PKG_CURRENT_NAME,
                SDCARD_PATH, false,
                House.IsScreenOrientationPortrait(context),
                Utils.queryRawPaperItems(context));
    }

    public static void exportOrImportWallpaper(Context context) {
//        final ArrayList<RawPaperItem> wallPaperItems = new ArrayList<RawPaperItem>();
//        Utils.getRawPaperItems(context, wallPaperItems);
        WallpaperUtils.startExportOrImportIntent(context,
                House.IsScreenOrientationPortrait(context),
                Utils.queryRawPaperItems(context));
    }

    public static void reportError(String errorMsg) {
        final Context context = SESceneManager.getInstance().getContext();
        if (null != context) {
            StaticUtil.reportError(context, errorMsg);
        }
    }
    
    public static void initLocalObject(Context context) {
    	SharedPreferences sp = MarketConfiguration.getMarketConfigPreferences(context);
        boolean isInited = sp.getBoolean(MarketConfiguration.SP_EXTRAS_INIT_ASSERT_OBJECT, false);
        if(!isInited) {
        	MarketConfiguration.initLocalObjects(context, loadProductList(context));
        }
    }
    private static List<Product> loadProductList(Context context) {
    	List<Product> productList = new ArrayList<Product>();
    	//load all model info , add local assert data
    	HomeDataBaseHelper help = HomeDataBaseHelper.getInstance(context);
    	SQLiteDatabase db = help.getWritableDatabase();
    	Cursor cursor = db.query(Tables.MODEL_INFO, null, null, null, null, null, null);
    	
    	if(cursor != null) {
    		if(cursor.moveToFirst()) {
    			do {
    				String type = cursor.getString(cursor.getColumnIndex(ModelColumns.TYPE));
    				if (ModelInfo.isObjectShelfHidden(type)  || type.equalsIgnoreCase("IconBox")
    						|| "ipad".equalsIgnoreCase(type) || "nexus10".equalsIgnoreCase(type)
    						|| "house".equalsIgnoreCase(type) || "desk".equalsIgnoreCase(type)) {
    				}else {
    					String assertPath = cursor.getString(cursor.getColumnIndex(ModelColumns.ASSETS_PATH));
    					String objectName = cursor.getString(cursor.getColumnIndex(ModelColumns.OBJECT_NAME));
    					if(!TextUtils.isEmpty(assertPath)) {
    						int index = assertPath.indexOf("/");
    						assertPath = assertPath.substring(index + 1, assertPath.length());
    						Product p = ThemeXmlParser.parserAssert(context, assertPath);
    						if(p != null) {
    							productList.add(p);
    							String where = ModelColumns.OBJECT_NAME + " = '" + objectName + "'";
    							updateLocalObjectProInfo(where, p, db);
    						}
    					}
    				}
    				
    			} while (cursor.moveToNext());
    		}
    		cursor.close();
    		cursor = null;
    	}
    	//load sdcard object data
    	initSdcardObject(context, Utils.getObjectRootPath(), productList);
    	return productList;
    }
    
    private static void updateLocalObjectProInfo(String where, Product p, SQLiteDatabase db) {
    	ContentValues cv = new ContentValues();
    	cv.put(ModelColumns.PRODUCT_ID, p.product_id);
    	cv.put(ModelColumns.VERSION_CODE, p.version_code);
    	cv.put(ModelColumns.VERSION_NAME, p.version_name);
    	cv.put(ModelColumns.PLUG_IN, 1);
    	db.update(Tables.MODEL_INFO, cv, where, null);
    }
    
    private static void parseSdcardObject(List<Product> resultList, File file) {
        if (file != null && resultList != null) {
            if (file.exists() && file.isDirectory()) {
            	//parse ResourceManifest.xml
                Product product = ThemeXmlParser.parser(file);
                if(product != null) {
                    resultList.add(product);
                }
                
                //parse Model_config.xml
                Utils.parseSdcardObjectModelInfo(file.getPath(), product);
            }
        }
    }
    
    private static void initSdcardObject(final Context context, final String parentPath, final List<Product> productList) {
//        new Thread(new Runnable() {
//            
//            @Override
//            public void run() {
                if (ThemeXmlParser.isExistingFolderPath(parentPath)) {
                    File folder = new File(parentPath);
                    File[] files = folder.listFiles();
                    for (File f : files) {
                    	parseSdcardObject(productList, f);
                    }
                }
//            }
//        }).start();
    }

    public static void tryImportWallpaper(Context context) {
        WallpaperUtils.checkAndImportWallpapers(context, House.IsScreenOrientationPortrait(context));
    }

    public static void showFeedbackActivity(Context context) {
        StaticUtil.showFeedbackActivity(context);
    }
}
