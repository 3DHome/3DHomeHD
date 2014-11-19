package com.borqs.se.home3d;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.borqs.borqsweather.weather.WeatherConditions;
import com.borqs.market.utils.MarketConfiguration;
import com.borqs.market.utils.MarketUtils;
import com.borqs.market.wallpaper.RawPaperItem;
import com.borqs.se.R;
import com.borqs.se.engine.GLSurfaceView;
import com.borqs.se.widget3d.ADViewController;
import com.borqs.se.R;
import com.borqs.se.widget3d.House;
import org.xmlpull.v1.XmlPullParser;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.borqs.borqsweather.weather.IWeatherService;
import com.borqs.borqsweather.weather.WeatherController;

import com.borqs.se.download.Utils;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.home3d.ProviderUtils.ModelColumns;
import com.borqs.se.home3d.ProviderUtils.ObjectInfoColumns;
import com.borqs.se.home3d.ProviderUtils.ThemeColumns;
import com.borqs.se.shortcut.HomeAppWidgetHost;
import com.borqs.se.shortcut.LauncherModel;
import com.borqs.se.thumbnails.VideoThumbnailsService;

public class HomeManager {

    public static final int MSG_REMOVE_LOADING = 0;
    public static final int MSG_FINISH_LOADING = 1;
    private static final int APPWIDGET_HOST_ID = 2048;

    private static HomeManager mHomeManager;
    private WorkSpace mWorkSpace;
    private AppSearchPane mAppSearchPane;
    private TextView mFPSView;
    private HomeAppWidgetHost mAppWidgetHost;
    private AppWidgetManager mAppWidgetManager;
    private ModelObjectsManager mModelObjectsManager;
    private HomeScene mHomeScene;
    private ThemeInfo mCurrentThemeInfo;
    public List<TimeChangeCallBack> mTimeCallBacks;
    private boolean mHasStartVideoThumbTask = false;
    private boolean mWithoutAD = false;

    public interface TimeChangeCallBack {
        public void onTimeChanged();
    }

    public interface ModelChangeCallBack {
        public void onAddModelToDB(ModelInfo modelInfo);

        public void onRemoveModelFromDB(ModelInfo modelInfo);
    }

    private HomeManager() {

    }

    public void init(Context context) {
        File file = new File(HomeUtils.SDCARD_PATH + "/.debug");
        if (file.exists()) {
            HomeUtils.DEBUG = true;
            setWeatherServiceDebug(true);
        }
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        SESceneManager.getInstance().initEngine(context);
        SESceneManager.getInstance().enableLight(true);
        SESceneManager.setDebug_JNI(HomeUtils.DEBUG);
        createOrUpgradeDB();
        LauncherModel.getInstance().loadAllData(false);
        mTimeCallBacks = new ArrayList<TimeChangeCallBack>();
        mAppWidgetManager = AppWidgetManager.getInstance(context);
        mAppWidgetHost = new HomeAppWidgetHost(context, APPWIDGET_HOST_ID);
        mAppWidgetHost.startListening();
        String localFilePath = HomeUtils.getLocalFilePath(getContext(), null);
        MarketConfiguration.setSystemThemeDir(new File(localFilePath));

    }

    public void setWithoutAD(boolean withoutAD) {
        mWithoutAD = withoutAD;
    }

    public boolean withoutAD() {
        return mWithoutAD;
    }

    public void debug(boolean enable) {
        HomeUtils.DEBUG = enable;
        setWeatherServiceDebug(enable);
        File file = new File(HomeUtils.SDCARD_PATH + "/.debug");
        if (enable) {
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            Toast.makeText(HomeManager.getInstance().getContext(), "Open debug!!!", Toast.LENGTH_SHORT).show();
            SELoadResThread.getInstance().process(mDetectMemoryTask, 10000);
        } else {
            if (file.exists()) {
                file.delete();
            }
            Toast.makeText(HomeManager.getInstance().getContext(), "Close debug!!!", Toast.LENGTH_SHORT).show();
            SELoadResThread.getInstance().cancel(mDetectMemoryTask);
        }
        SESceneManager.setDebug_JNI(enable);
    }

    /**
     * 加载数据库并且启动场景
     */
    private void createOrUpgradeDB() {
        UpdateDBThread.getInstance().process(new Runnable() {
            @Override
            public void run() {
                /// firstly, trigger possible db upgrade, and verify validate object db (todo)
                ContentResolver resolver = getContext().getContentResolver();
                Cursor cursor = resolver.query(ObjectInfoColumns.CONTENT_URI, null, "_id=1", null, null);
                if (null != cursor) {
                    cursor.close();
                }

                /// secondly, load possible debug model from SDCard
                loadDebugThemeAndModelFromSDCard();

                /// thirdly, construct model object manager
                if (mModelObjectsManager == null) {
                    mModelObjectsManager = new ModelObjectsManager();
                }

                /// then, construct scene and current home scene
                HomeSceneInfo sceneInfo = new HomeSceneInfo();
                sceneInfo.setThemeInfo(getCurrentThemeInfo());
                if (HomeUtils.DEBUG) {
                    Log.d("SE3DAppManager:", "start3DScreen name :" + sceneInfo.mSceneName);
                }
                mHomeScene = new HomeScene(getContext(), sceneInfo.mSceneName);
                mHomeScene.setHomeSceneInfo(sceneInfo);
                SESceneManager.getInstance().setCurrentScene(mHomeScene);
                SESceneManager.getInstance().dataIsReady();
                moveAssetToExternal();
            }
        });
    }

    public void changeCurrentHomeScene(HomeScene homeScene) {
        mHomeScene = homeScene;
    }

    public void startVideoThumbnailsService() {
        if (!mHasStartVideoThumbTask) {
            VideoThumbnailsService task = new VideoThumbnailsService();
            task.start(getContext());
            mHasStartVideoThumbTask = true;
        }
    }

    private Runnable mDetectMemoryTask = new Runnable() {
        public void run() {
            ActivityManager activityManager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
            MemoryInfo outInfo = new MemoryInfo();
            activityManager.getMemoryInfo(outInfo);
            int myProcessID = Process.myPid();
            android.os.Debug.MemoryInfo[] myInfo = activityManager.getProcessMemoryInfo(new int[] { myProcessID });
            String value = "TotalPrivateDirty :" + (myInfo[0].getTotalPrivateDirty() / 1024f);
            Log.d(HomeUtils.TAG, "#### memory usage :" + value);
            SELoadResThread.getInstance().cancel(mDetectMemoryTask);
            SELoadResThread.getInstance().process(mDetectMemoryTask, 10000);
        }
    };

    public static HomeManager getInstance() {
        if (mHomeManager == null) {
            mHomeManager = new HomeManager();
        }
        return mHomeManager;
    }

    public void startLocalService() {
        getContext().startService(new Intent(getContext(), LocalService.class));
    }

    public void stopLocalService() {
        getContext().stopService(new Intent(getContext(), LocalService.class));
    }

    public void addTimeCallBack(final TimeChangeCallBack callBack) {
        SESceneManager.getInstance().runInUIThread(new Runnable() {
            public void run() {
                if (!mTimeCallBacks.contains(callBack)) {
                    mTimeCallBacks.add(callBack);
                    callBack.onTimeChanged();
                }
            }
        });
    }

    public void removeTimeCallBack(final TimeChangeCallBack callBack) {
        SESceneManager.getInstance().runInUIThread(new Runnable() {
            public void run() {
                if (callBack != null) {
                    mTimeCallBacks.remove(callBack);
                }
            }
        });
    }

    public HomeAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public AppWidgetManager getAppWidgetManager() {
        return mAppWidgetManager;
    }

    public void setScaleListener(ScaleGestureDetector.OnScaleGestureListener l) {
        mWorkSpace.setScaleListener(l);
    }

    public void setWorkSpace(WorkSpace workSpace) {
        mWorkSpace = workSpace;
    }

    public void setAppSearchPane(AppSearchPane appSearchPane) {
        mAppSearchPane = appSearchPane;
    }

    public WorkSpace getWorkSpace() {
        return mWorkSpace;
    }

    public AppSearchPane getAppSearchPane() {
        return mAppSearchPane;
    }

    public void clearFPSView() {
        if (mFPSView != null) {
            mWorkSpace.removeView(mFPSView);
            SESceneManager.getInstance().clearFPSView();
            mFPSView = null;
        }
    }

    public void showFPSView() {
        if (mFPSView == null) {
            mFPSView = new TextView(getContext());
            mFPSView.setTextColor(Color.RED);
            mWorkSpace.addView(mFPSView);
            SESceneManager.getInstance().setFPSView(mFPSView);
        }
    }

    public Context getContext() {
        return HomeApplication.getInstance().getApplicationContext();
    }

    public boolean startActivityForResult(Intent intent, int requestCode) {
        boolean result = true;
        try {
            SESceneManager.getInstance().getGLActivity().startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            result = false;
        } catch (SecurityException e) {
            result = false;
        }
        if (result & mHomeScene != null) {
            mHomeScene.setStatus(HomeScene.STATUS_DISALLOW_TOUCH, true);
        }
        return result;
    }

    public boolean startActivity(Intent intent) {
        boolean result = true;
        try {
            getContext().startActivity(intent);
        } catch (Exception e) {
            result = false;
        }
        if (result & mHomeScene != null) {
            mHomeScene.setStatus(HomeScene.STATUS_DISALLOW_TOUCH, true);
        }
        return result;
    }

    public void setRequestedOrientation(int requestedOrientation) {
        SESceneManager.getInstance().getGLActivity().setRequestedOrientation(requestedOrientation);
    }

    public int getRequestedOrientation() {
        if (SESceneManager.getInstance().getGLActivity() == null) {
            return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
        return SESceneManager.getInstance().getGLActivity().getRequestedOrientation();
    }

    public HomeScene getHomeScene() {
        return mHomeScene;
    }

    public void onActivityResume() {
        checkSceneRotation(getCurrentThemeInfo());
        for (TimeChangeCallBack callBack : mTimeCallBacks) {
            if (callBack != null) {
                callBack.onTimeChanged();
            }
        }
    }

    public ModelObjectsManager getModelManager() {
        return mModelObjectsManager;
    }

    public void addModelToScene(ModelInfo modelInfo) {
        if (mModelObjectsManager != null) {
            mModelObjectsManager.mModels.put(modelInfo.mName, modelInfo);
            ModelChangeCallBack modelChangeCallBack = (ModelChangeCallBack) mHomeScene;
            if (modelChangeCallBack != null) {
                modelChangeCallBack.onAddModelToDB(modelInfo);
            }
        }
    }

    public void removeModelFromScene(String name) {
        if (mModelObjectsManager != null) {
            ModelInfo modelInfo = mModelObjectsManager.mModels.remove(name);
            if (modelInfo == null) {
                return;
            }
            ModelChangeCallBack modelChangeCallBack = (ModelChangeCallBack) mHomeScene;
            if (modelChangeCallBack != null) {
                modelChangeCallBack.onRemoveModelFromDB(modelInfo);
            }
        }

    }

    public void changeTheme(ThemeInfo themeInfo) {
        if (mCurrentThemeInfo != null && mModelObjectsManager != null) {
            mCurrentThemeInfo = themeInfo;
            HomeUtils.staticUsingTheme(getContext(), themeInfo.mThemeName);
            SESceneManager.getInstance().removeMessage(HomeScene.MSG_TYPE_UPDATE_SCENE);
            SESceneManager.getInstance().handleMessage(HomeScene.MSG_TYPE_UPDATE_SCENE, themeInfo);
        }
        checkSceneRotation(themeInfo);
    }

    public ThemeInfo getCurrentThemeInfo() {
        ContentResolver contentResolver = getContext().getContentResolver();
        if (mCurrentThemeInfo == null) {
            String where = ThemeColumns.IS_APPLY + "=" + 1;
            Cursor cursor = contentResolver.query(ThemeColumns.THEME_LEFT_JOIN_ALL, null, where, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    mCurrentThemeInfo = ThemeInfo.CreateFromDB(cursor);
                    if (!checkThemeInfo(mCurrentThemeInfo)) {
                        mCurrentThemeInfo = null;
                    }
                }
                cursor.close();
            }
            if (mCurrentThemeInfo == null) {
                HomeUtils.markThemeAsApply(getContext(), HomeDataBaseHelper.getInstance(getContext())
                        .getDefaultThemeID());
                cursor = contentResolver.query(ThemeColumns.THEME_LEFT_JOIN_ALL, null, where, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        mCurrentThemeInfo = ThemeInfo.CreateFromDB(cursor);
                    }
                    cursor.close();
                }
            }
            if (mCurrentThemeInfo == null) {
                // 数据库错误，删除数据库
                HomeUtils.deleteFile("/data/data/com.borqs.se/databases");
                Process.killProcess(Process.myPid());
            }
        }
        return mCurrentThemeInfo;
    }
    
    private boolean checkThemeInfo(ThemeInfo themeInfo) {
        boolean modelExist = true;
        if (themeInfo.mFilePath.startsWith("assets/")) {
            try {
                InputStream is = getContext().getAssets().open(themeInfo.mFilePath.substring(7) + "/models_config.xml");
                if (is== null) {
                    modelExist = false;
                } else {
                    is.close();
                }
            } catch (IOException e) {
                modelExist = false;
            }

        } else {
            File modelFile = new File(themeInfo.mFilePath);
            if (!modelFile.exists()) {
                modelExist = false;
            }
        }
        return modelExist;
    }

    private void loadDebugThemeAndModelFromSDCard() {
        // 假如为调试状态可从sdcard/3DHome目录下加载模型包以及主题包， 3DHome每次启动重新加载
        if (HomeUtils.DEBUG) {
            String where = ModelColumns.ASSETS_PATH + " LIKE '" + HomeUtils.SDCARD_PATH + "%'";
            getContext().getContentResolver().delete(ModelColumns.CONTENT_URI, where, null);
            where = ThemeColumns.FILE_PATH + " LIKE '" + HomeUtils.SDCARD_PATH + "%'";
            getContext().getContentResolver().delete(ThemeColumns.CONTENT_URI, where, null);

            File file = new File(HomeUtils.SDCARD_PATH);
            if (file.exists()) {
                File[] allZipFile = file.listFiles();
                for (File zipFile : allZipFile) {
                    if (zipFile.isFile() && (zipFile.getName().endsWith(".zip") || zipFile.getName().endsWith(".apk"))) {
                        try {
                            String unzipFile = HomeUtils.SDCARD_PATH + "/" + zipFile.getName() + "_unzip";
                            HomeUtils.deleteFile(unzipFile);
                            Utils.unzipDataPartition(zipFile.getAbsolutePath(), unzipFile);
                            File themeFile = new File(unzipFile + "/theme_config.xml");
                            if (themeFile.exists()) {
                                // load debug theme begin
                                FileInputStream fis = new FileInputStream(themeFile);
                                XmlPullParser parser = Xml.newPullParser();
                                parser.setInput(fis, "utf-8");
                                XmlUtils.beginDocument(parser, "config");
                                ThemeInfo config = ThemeInfo.CreateFromXml(getContext(), parser, null, unzipFile);
                                fis.close();
                                config.saveToDB(getContext());
                                // load debug theme end
                            } else {
                                // load debug model begin
                                FileInputStream fis = new FileInputStream(unzipFile + "/models_config.xml");
                                XmlPullParser parser = Xml.newPullParser();
                                parser.setInput(fis, "utf-8");
                                XmlUtils.beginDocument(parser, "config");
                                ModelInfo config = ModelInfo.CreateFromXml(parser, null, unzipFile);
                                fis.close();
                                config.saveToDB();
                                // load debug model end
                            }
                        } catch (Exception e) {

                        }
                    }
                }
            }
        }

    }

    private void checkSceneRotation(ThemeInfo themeInfo) {
        if (null == themeInfo) {
            // skip
            return;
        }

        final int orientation;
        if (themeInfo.mWallHeight > themeInfo.mWallWidth) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else {
            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }

        if (orientation != getRequestedOrientation()) {
            setRequestedOrientation(orientation);
        }
    }

    /******************** move assets to external storage ************************************/
    private boolean mHasBeenExecuted = false;

    public void moveAssetToExternal() {
        if (!mHasBeenExecuted && !SettingsActivity.hasUpdatedLocalRes(getContext())) {
            mHasBeenExecuted = true;
            Thread thread = new MoveAssetThread();
            thread.start();
        }
    }

    private class MoveAssetThread extends Thread {
        public MoveAssetThread() {
            super("MoveAssetThread");
            setPriority(MIN_PRIORITY);
        }

        @Override
        public void run() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            Iterator<Entry<String, String>> iter = HomeDataBaseHelper.MODEL_XMLS.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                String name = entry.getKey();
                String path = entry.getValue();
                if (isAssetFileExist(path + File.separator + MarketUtils.MARKET_CONFIG_FILE_NAME)) {
                    String localFilePath = HomeUtils.getLocalFilePath(getContext(), name);
                    HomeUtils.deleteFile(localFilePath);
                    HomeUtils.moveAssetFilesToExternal(getContext(), path, name);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            iter = HomeDataBaseHelper.THEME_XMLS.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                String name = entry.getKey();
                String path = entry.getValue();
                if (isAssetFileExist(path + File.separator + MarketUtils.MARKET_CONFIG_FILE_NAME)) {
                    String localFilePath = HomeUtils.getLocalFilePath(getContext(), name);
                    HomeUtils.deleteFile(localFilePath);
                    HomeUtils.moveAssetFilesToExternal(getContext(), path, name);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            Intent intent = new Intent("com.borqs.market.intent.action.Init");
            getContext().sendBroadcast(intent);           
        }

        private boolean isAssetFileExist(String fileName) {
            InputStream is = null;
            try {
                is = getContext().getAssets().open(fileName);
            } catch (IOException e1) {
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
                return true;
            }
            return false;
        }
    }

    /******************** for wall paper ************************************/
    public static void updateWallPaperBundle(ArrayList<String> wallBundle, ArrayList<String> groundBundle) {
        if (null != mHomeManager) {
            HomeScene scene = mHomeManager.getHomeScene();
            if (null != scene) {
                scene.updateWallpaperBundle(wallBundle, groundBundle);
            }
        }
    }

    public static ArrayList<RawPaperItem> queryRawPaperItems() {
        if (null != mHomeManager) {
            HomeScene scene = mHomeManager.getHomeScene();
            if (null != scene) {
                return scene.queryRawPaperItems();
            }
        }
        return new ArrayList<RawPaperItem>();
    }

    // code reform begin, migrating SESceneManager from Activity to here
    private ADViewController mADViewController;

    public void createSceneManager(HomeActivity activity, Handler handler, GLSurfaceView surfaceView) {
        SESceneManager.getInstance().setGLActivity(activity);
        SESceneManager.getInstance().setHandler(handler);
        SESceneManager.getInstance().setGLSurfaceView(surfaceView);
//        SESceneManager.getInstance().setGLActivity(activity);
//        boolean hasScene = mHomeManager.getHomeScene() != null;
        if (getHomeScene() != null) {
            SESceneManager.getInstance().onActivityRestart();
        }
        mADViewController = ADViewController.getInstance();
    }

    public void resumeSceneManager(HomeActivity activity, Handler handler, GLSurfaceView surfaceView) {
        SESceneManager.getInstance().setGLActivity(activity);
        SESceneManager.getInstance().setHandler(handler);
        SESceneManager.getInstance().setGLSurfaceView(surfaceView);
        SESceneManager.getInstance().onActivityResume();
        mADViewController.onResume();
    }

    public void pauseSceneManager(boolean leaveHome) {
        SESceneManager.getInstance().setNeedDestoryHardware(leaveHome);
        SESceneManager.getInstance().onActivityPause();
        mADViewController.onPause();
    }

    public boolean onMenuOpened(int featureId, Menu menu) {
        return SESceneManager.getInstance().onMenuOpened(featureId, menu);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return SESceneManager.getInstance().onCreateOptionsMenu(menu);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return SESceneManager.getInstance().onPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return SESceneManager.getInstance().onOptionsItemSelected(item);
    }

    protected Dialog onCreateDialog(int id, Bundle bundle) {
        return SESceneManager.getInstance().onCreateDialog(id, bundle);
    }

    protected void onPrepareDialog(int id, Dialog dialog, Bundle bundle) {
        SESceneManager.getInstance().onPrepareDialog(id, dialog, bundle);
    }

    public void onActivityDestroy() {
        SESceneManager.getInstance().onActivityDestroy();
        mADViewController.onDestory();
    }

    public void handleBackKey() {
        SESceneManager.getInstance().handleBackKey();
    }

    public void handleMenuKey() {
        SESceneManager.getInstance().handleMenuKey();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        SESceneManager.getInstance().onActivityResult(requestCode, resultCode, data);
    }

    protected void onNewIntent(Intent intent) {
        SESceneManager.getInstance().onNewIntent(intent);
    }
    // code reform end.

    public static void notifyWallLabelShow(int index) {
        if (null != mHomeManager) {
            HomeScene scene = mHomeManager.getHomeScene();
            if (null != scene) {
                scene.onWallLabelShow(index);
            }
        }
    }

    public static int getWallpaperMaxSize() {
        Context context = mHomeManager.getContext();
        int result = SettingsActivity.getPreferredWallpaperSize(context, -1);

        if (result < 0) {
            result = context.getResources().getInteger(R.integer.wallpaper_max_size);
            int sizeByResolution = getPaperSizeByResolution();
            if (result < sizeByResolution) {
                result = sizeByResolution;
            }
            SettingsActivity.setPreferredWallpaperSize(context, result);
        }

        if (HomeUtils.DEBUG) {
            Log.v(HomeUtils.TAG, "getWallpaperMaxSize maxSize = " + result +
                    ", result =" + result);
        }
        return result;
    }

    /// weather service interface begin

    public boolean isFoggyWeather() {
        boolean needed = false;
        if (mWeatherService != null) {
            int currentType;
            try {
                currentType = mWeatherService.getConditionType();
                if (currentType == WeatherConditions.CONDITION_TYPE_FOG) {
                    needed = true;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return needed;
    }

    private void setWeatherServiceDebug(boolean debug) {
        WeatherController.DEBUG = debug;
    }

    public WeatherBindedCallBack mWeatherBindedCallBack;

    public interface WeatherBindedCallBack {
        public void onWeatherServiceBinded(IWeatherService service);
    }

    private IWeatherService mWeatherService;

    public void bindWeatherService() {
        if (mWeatherService == null) {
            Context context = getContext();
            Intent intent = new Intent("com.borqs.borqsweather.weatherservice");
            context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void unBindWeatherService() {
        getContext().unbindService(mConnection);
    }

    public void setWeatherBindedCallBack(WeatherBindedCallBack callBack) {
        mWeatherBindedCallBack = callBack;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            if (service == null) {
                return;
            }

            mWeatherService = IWeatherService.Stub.asInterface(service);
            if (mWeatherBindedCallBack != null) {
                mWeatherBindedCallBack.onWeatherServiceBinded(mWeatherService);
            }
        }

        public void onServiceDisconnected(ComponentName arg0) {
            mWeatherService = null;
            if (mWeatherBindedCallBack != null) {
                mWeatherBindedCallBack.onWeatherServiceBinded(null);
            }
        }
    };

    public IWeatherService getWeatherService() {
        return mWeatherService;
    }

    // todo: calculate lunar day
    public int getLunarDay() {
        int lunarDay = -1;
        if (mWeatherService != null) {
            try {
                lunarDay = mWeatherService.getlunarDay();
            } catch (RemoteException e) {
                e.printStackTrace();
                lunarDay = -1;
            }
        }
        return lunarDay;
    }
    /// weather service interface end

    private static final int PAPER_LOW_SIZE = 512;
    private static final int PAPER_HD_SIZE = 1024;
    private static final int PAPER_TV_HIGH_SIZE = 2048;

    private static final int PAPER_SIZE_THRESHOLD = 1080;
    private static final int PAPER_SIZE_THRESHOLD_TV = 2160;
    private static int getPaperSizeByResolution() {
        DisplayMetrics metric = new DisplayMetrics();
        SESceneManager.getInstance().getGLActivity().getWindowManager().getDefaultDisplay().getMetrics(metric);
        final int minPixels = Math.min(metric.widthPixels, metric.heightPixels);
        if (minPixels < PAPER_SIZE_THRESHOLD) {
            return PAPER_LOW_SIZE;
        } else if (minPixels >= PAPER_SIZE_THRESHOLD_TV) {
            return PAPER_TV_HIGH_SIZE;
        }
        return PAPER_HD_SIZE;
    }
}
