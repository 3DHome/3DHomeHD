package com.borqs.se.engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


import android.app.Activity;
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
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Xml;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

import com.borqs.se.home3d.HomeApplication;
import com.borqs.se.home3d.HomeScene;
import com.borqs.se.home3d.LocalService;
import com.borqs.se.home3d.UpdateDBThread;
import com.borqs.se.home3d.XmlUtils;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.ModelObjectsManager;
import com.borqs.se.home3d.ProviderUtils;
import com.borqs.se.home3d.ProviderUtils.ObjectInfoColumns;
import com.borqs.se.home3d.HomeDataBaseHelper;
import com.borqs.se.home3d.ScaleGestureDetector;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.home3d.WorkSpace;
import com.borqs.se.shortcut.HomeAppWidgetHost;
import com.borqs.se.shortcut.WidgetWorkSpace;
import com.borqs.se.thumbnails.VideoThumbnailsService;
import com.borqs.borqsweather.weather.LoopForInfoService;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import com.borqs.framework3d.home3d.TypeManager;
public class SESceneManager {

    private native void instance_JNI(int userid0, int userid1);

    public native void addAssetPath_JNI(String assetPath);

    public native void rmAssetPath_JNI(String assetPath);

    public native void loadPlugins_JNI(String filePath);

    public native void runOneFrame_JNI();

    public native void releaseResource_JNI();

    public native void destroy_JNI();

    public native void initRenderCapabilities_JNI();

    // 0 for disable, 1 enable
    public native void enableShadow_JNI(int status);

    /*
     * createCBF should be first step before loadScenedatapath: your *.ASE
     * absolute path.May be /sdcard/xxx/xxxfileNameId: your ase file's prefix.if
     * your ase file name is abc.ase the fileNameId should be abc(do NOT add
     * .ase).
     */
    public native void createCBF_JNI(String datapath, String fileNameId);

    public static final int X_AXIS = 0;
    public static final int Y_AXIS = 1;
    public static final int Z_AXIS = 2;
    public static final int MSG_REMOVE_LOADING = 0;
    public static final int MSG_FINISH_LOADING = 1;

    public void javaCallback(int msgType, String msgName) {
        handleMessage(msgType, msgName);
    }

    public native static void setMainScene_JNI(String sceneName);

    public native static void glReadPixels_JNI(String imageKey, int x, int y, int width, int height);

    public native static void savePixelsData_JNI(String savePath, String imageKey);

    public native static boolean loadPixelsData_JNI(String savePath, String imageKey, int width, int height);

    public native static void setDebug_JNI(boolean debug);

    private static final int FORCE_THRESHOLD = 18;
    private static final int SHAKE_DURATION = 1500;
    private static final int SHAKE_TIMEOUT = 2000;
    private static final int SHAKE_COUNT = 3;
    private float mLastX, mLastY, mLastZ;
    private int mShakeCount;
    private long mLastShakeTime;
    private long mFirstShakeTime;
    private boolean mHasShaked;
    private long mLastChangeTime = -1;
    public static final String BACKGROUND_IMAGE_KEY = "scene_catched_image";
    public static final String BACKGROUND_IMAGE_PATH = HomeUtils.PKG_FILES_PATH + "/catch_scene.data";

    public static long TIME_ONE_FRAME = 20;
    public static final boolean USING_TIME_ANIMATION = false;

    private static SESceneManager mSESceneManager;
    private int mWidth;
    private int mHeight;
    private float mPixelDensity;
    private Handler mHandler;
    private WorkSpace mWorkSpace;
    private SERenderView mGLSurfaceView;
    private Activity mGLActivity;
    private WidgetWorkSpace mWidgetWorkSpace;
    private TextView mFPSView;
    private int mTouchSlop;
    private boolean[] mLocks;
    private boolean mNeedCatchBackground;
    private HomeAppWidgetHost mAppWidgetHost;
    private AppWidgetManager mAppWidgetManager;
    private ModelObjectsManager mModelObjectsManager;
    private static final int APPWIDGET_HOST_ID = 2048;

    private List<SEScene> mScenes;
    private String mCurrentSceneName;
    private SEScene mCurrentScreen;
    public List<TimeChangeCallBack> mTimeCallBacks;
    public List<UnlockScreenListener> mUnlockScreenListeners;
    private boolean mHasStartVideoThumbTask = false;

    private boolean mShouldWait = true;
    
    public float mCellHeight = 231;
    public int mCellCountX = 4;

    public interface TimeChangeCallBack {
        public void onTimeChanged();
    }

    public interface UnlockScreenListener {
        public void unlockScreen();
    }

    public interface ModelChangeCallBack {
        public void onAddModelToDB(ModelInfo modelInfo);

        public void onRemoveModelToDB(String modelName);
    }

    private SESceneManager(Context context) {
        debugOutput("SESceneManager construct enter.");
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        instance_JNI(137, 18215879);
        loadPlugins_JNI(HomeUtils.PKG_LIB_PATH);
        addAssetPath_JNI(context.getApplicationInfo().sourceDir);
        addAssetPath_JNI("/");
        createOrUpgradeDB();
        mWidth = 0;
        mHeight = 0;
        mPixelDensity = 0;
        mNeedCatchBackground = false;
        mScenes = new ArrayList<SEScene>();
        mTimeCallBacks = new ArrayList<TimeChangeCallBack>();
        mUnlockScreenListeners = new ArrayList<UnlockScreenListener>();
        if (HomeUtils.DEBUG) {
            SELoadResThread.getInstance().process(mDetectMemoryTask, 10000);
            setDebug_JNI(true);
        }
        mAppWidgetManager = AppWidgetManager.getInstance(getContext());
        mAppWidgetHost = new HomeAppWidgetHost(getContext(), APPWIDGET_HOST_ID);
        mAppWidgetHost.startListening();
        debugOutput("SESceneManager construct exit.");
        onCurrentSceneCreated();
    }
    
    private void createOrUpgradeDB() {
        debugOutput("SESceneManager createOrUpgradeDB enter.");
        UpdateDBThread.getInstance().process(new Runnable() {
            @Override
            public void run() {
                ContentResolver resolver = getContext().getContentResolver();
                resolver.query(ObjectInfoColumns.CONTENT_URI, null, "_id=1", null, null);
                if (mModelObjectsManager == null) {
                    mModelObjectsManager = new ModelObjectsManager();
                }
                dataBaseIsReady();
                debugOutput("SESceneManager createOrUpgradeDB final result.");
            }
        });
        debugOutput("SESceneManager createOrUpgradeDB exit.");
    }

    public void startVideoThumbnailsService() {
        if (!mHasStartVideoThumbTask) {
            debugOutput("SESceneManager startVideoThumbnailsService enter.");
            VideoThumbnailsService task = new VideoThumbnailsService();
            task.start(getContext());
            mHasStartVideoThumbTask = true;
            debugOutput("SESceneManager startVideoThumbnailsService exit.");
        }
    }

    private static class MemoryWatchDog {
        private ActivityManager mActivityManager;
        MemoryInfo mOutInfo;
        android.os.Debug.MemoryInfo mMyInfo;
        public MemoryWatchDog(Context context) {
            mActivityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            mOutInfo = new MemoryInfo();
        }
        public android.os.Debug.MemoryInfo getMyMemoryInfo() {
            mActivityManager.getMemoryInfo(mOutInfo);
            mMyInfo = mActivityManager.getProcessMemoryInfo(new int[] { Process.myPid() })[0];
            return mMyInfo;
        }
        public void debugOutput() {
            final float privateDirty = getMyMemoryInfo().getTotalPrivateDirty() / 1024f;
            String value = "TotalPrivateDirty :" + privateDirty;
            Log.d(HomeUtils.TAG, "#### memory usage :" + value);
        }

        public void debugOutput(String msg) {
            final float privateDirty = getMyMemoryInfo().getTotalPrivateDirty() / 1024f;
            String value = "TotalPrivateDirty :" + privateDirty;
            Log.d(HomeUtils.TAG, msg + " #### memory usage :" + value);
        }

        public void verboseOutput(String msg) {

        }
    }

    public void debugOutput(String msg) {
        if (HomeUtils.DEBUG) {
            mMemoryWatchDog.debugOutput(msg);
        }
    }

    private MemoryWatchDog mMemoryWatchDog = new MemoryWatchDog(getContext());
    private Runnable mDetectMemoryTask = new Runnable() {
        public void run() {
            mMemoryWatchDog.debugOutput();
            SELoadResThread.getInstance().cancel(mDetectMemoryTask);
            SELoadResThread.getInstance().process(mDetectMemoryTask, 10000);
        }
    };
    private static Object mLock = new Object();

    public static SESceneManager getInstance() {
        return getInstance(HomeApplication.getInstance().getApplicationContext());
    }
    public static SESceneManager getInstance(Context context) {
        if (mSESceneManager == null) {
            synchronized (mLock) {
                if (mSESceneManager == null) {
                    mSESceneManager = new SESceneManager(context);
                }
            }
        }
        return mSESceneManager;
    }

    public void startLocalService() {
        getContext().startService(new Intent(getContext(), LocalService.class));
    }

    public void stopLocalService() {
        getContext().stopService(new Intent(getContext(), LocalService.class));
    }

    public void addTimeCallBack(final TimeChangeCallBack callBack) {
        runInUIThread(new Runnable() {
            public void run() {
                if (!mTimeCallBacks.contains(callBack)) {
                    mTimeCallBacks.add(callBack);
                    callBack.onTimeChanged();
                }
            }
        });
    }

    public void removeTimeCallBack(final TimeChangeCallBack callBack) {
        runInUIThread(new Runnable() {
            public void run() {
                if (callBack != null) {
                    mTimeCallBacks.remove(callBack);
                }
            }
        });
    }

    public void addScene(SEScene scene) {
        if (!mScenes.contains(scene)) {
            mScenes.add(scene);
        }
    }

    public void removeScene(SEScene scene) {
        mScenes.remove(scene);
        if (scene != null) {
            scene.release();
        }
    }
    
    private void softRemoveScene(SEScene scene) {
        mScenes.remove(scene);
        if (scene != null) {
            scene.softRelease();
        }
    }

    public SEScene findScene(String name) {
        for (SEScene scene : mScenes) {
            if (scene.mSceneName.endsWith(name)) {
                return scene;
            }
        }
        return null;
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

    public void setPixelDensity(float pixelDensity) {
        mPixelDensity = pixelDensity;
    }

    public float getPixelDensity() {
        return mPixelDensity;
    }

    public float getFontScale() {
        Configuration config = new Configuration();
        android.provider.Settings.System.getConfiguration(getContext().getContentResolver(), config);
        return config.fontScale;
    }

    public int getScreenWidth() {
        return mWidth;
    }

    public int getScreenHeight() {
        return mHeight;
    }

    public void setNeedcatchBackground(boolean need) {
        mNeedCatchBackground = need;
    }

    public boolean needcatchBackground() {
        return mNeedCatchBackground;
    }

    public void catchBackground() {
        int newW = HomeUtils.higherPower2(mWidth);
        int newH = HomeUtils.higherPower2(mHeight);
        int x = -((newW - mWidth) >> 1);
        int y = -((newH - mHeight) >> 1);
        glReadPixels_JNI(BACKGROUND_IMAGE_KEY, x, y, HomeUtils.higherPower2(mWidth),
                HomeUtils.higherPower2(mHeight));
    }

    public void setWidgetWorkSpace(WidgetWorkSpace ws) {
        mWidgetWorkSpace = ws;
    }

    public WidgetWorkSpace getWidgetView() {
        return mWidgetWorkSpace;
    }

    public void setWorkSpace(WorkSpace workSpace) {
        mWorkSpace = workSpace;
    }

    public WorkSpace getWorkSpace() {
        return mWorkSpace;
    }

    public void clearFPSView() {
        if (mFPSView != null) {
            mWorkSpace.removeView(mFPSView);
            mFPSView = null;
        }
    }

    public void showFPSView() {
        if (mFPSView == null) {
            mFPSView = new TextView(getContext());
            mFPSView.setTextColor(Color.RED);
            mWorkSpace.addView(mFPSView);
        }
    }

    public void setGLSurfaceView(SERenderView view) {
        mGLSurfaceView = view;
    }

    public SERenderView getGLSurfaceView() {
        return mGLSurfaceView;
    }

    public Activity getGLActivity() {
        return mGLActivity;
    }

    public void setGLActivity(Activity activity) {
        mGLActivity = activity;
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = mGLActivity.getWindowManager().getDefaultDisplay();
        display.getMetrics(metrics);
        setPixelDensity(metrics.density);
        mTouchSlop = ViewConfiguration.get(mGLActivity).getScaledTouchSlop();

    }

    public Context getContext() {
        return HomeApplication.getInstance().getApplicationContext();
    }

    public boolean startActivityForResult(Intent intent, int requestCode) {
        boolean result = true;
        try {
            mGLActivity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            result = false;
        } catch (SecurityException e) {
            result = false;
        }
        if (result & mCurrentScreen != null) {
            mCurrentScreen.setStatus(SEScene.STATUS_DISALLOW_TOUCH, true);
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
        if (result & mCurrentScreen != null) {
            mCurrentScreen.setStatus(SEScene.STATUS_DISALLOW_TOUCH, true);
        }
        return result;
    }

    public void setRequestedOrientation(int requestedOrientation) {
        mGLActivity.setRequestedOrientation(requestedOrientation);
    }

    public int getRequestedOrientation() {
        return mGLActivity.getRequestedOrientation();
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public void runInUIThread(Runnable runnable, long delayMillis) {
        if (mHandler == null) {
            return;
        }
        if (delayMillis <= 0) {
            mHandler.post(runnable);
        } else {
            mHandler.postDelayed(runnable, delayMillis);
        }
    }

    public void runInUIThread(Runnable runnable) {
        runInUIThread(runnable, 0);
    }

    public void finish() {
        if (!mGLActivity.isFinishing()) {
            mGLActivity.finish();
        }
    }

    public void removeMessage(int msgType) {
        if (mCurrentScreen != null) {
            mCurrentScreen.removeMessage(msgType);
        }
    }
    public void handleMessage(int msgType, Object message) {
        if (mCurrentScreen != null) {
            mCurrentScreen.handleMessage(msgType, message);
        }
    }

    public SEScene getCurrentScene() {
        return mCurrentScreen;
    }

    private SEScene getDefaultScene() {
        return findScene(SESceneInfo.DEFAULT_SCENE_NAME);
    }

    public void setDefaultScene() {
        setCurrentScene(SESceneInfo.DEFAULT_SCENE_NAME);
    }

    public void setCurrentScene(String name) {
        debugOutput("SESceneManager setCurrentScene enter.");
        if (mCurrentScreen != null && mCurrentScreen.mSceneName.endsWith(name)) {
            return;
        }
        mCurrentSceneName = name;
        if (mCurrentScreen != null) {
            if (mCurrentScreen.mSceneName.equals(SESceneInfo.DEFAULT_SCENE_NAME)) {
                mCurrentScreen.setVisibility_JNI(false);
            } else {
                removeScene(mCurrentScreen);
            }
        }
        mCurrentScreen = findScene(mCurrentSceneName);
        if (mCurrentScreen != null) {
            mCurrentScreen.setVisibility_JNI(true);
            SESceneManager.setMainScene_JNI(mCurrentSceneName);
        }
        debugOutput("SESceneManager setCurrentScene exit.");
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void notifySurfaceChanged(int width, int height) {
        debugOutput("SESceneManager notifySurfaceChanged enter.");
//        if (width > height || width == 0) {
//            return;
//        }
        mWidth = width;
        mHeight = height;
        if (mCurrentScreen == null) {
            start3DScene(mCurrentSceneName);
        } else {
            mCurrentScreen.notifySurfaceChanged(mWidth, mHeight);
        }
        debugOutput("SESceneManager notifySurfaceChanged exit.");
    }

    public void releaseCurrentScene() {
        if (mCurrentScreen != null) {
            mCurrentScreen.release();
        }
    }

    ////////
	private InputStream getAssetInputStream(String filePath)
	{
		InputStream is = null;
		try
		{
			is = SESceneManager.getInstance().getContext().getAssets().open(filePath);
		}
		catch(IOException e)
		{}
		return is;
	}
	private InputStream getInputStreamByPackagePath(String packageName, String path)
	{
		InputStream is = null;
		if(packageName.equals("assets"))
		{
		    is = getAssetInputStream(path);
		}
		else
		{
			Log.i("SESceneManager", "can not support package : | " + packageName + " , path: " + path + "| mount point config");
		}
		return is;
	}
	///////
	private void createModelInfoToDB(String packageName, String base, String objectName)
	{
		InputStream is = getInputStreamByPackagePath(packageName, base + "/" + objectName + "/models_config.xml");
        XmlPullParser parser2 = Xml.newPullParser();
        try {
            parser2.setInput(is, "utf-8");
            XmlUtils.beginDocument(parser2, "config");
            ModelInfo config = ModelInfo.CreateFromXml(parser2);
            config.saveToDB();
        }
        catch(XmlPullParserException e)
        {}
        catch(IOException e)
        {}
	}
    private void createTypeManager() {
        TypeManager t = TypeManager.getInstance();
        t.createFromAssetXml("object_placement_property.xml");
    }
    public synchronized void start3DScene(String sceneName) {
        debugOutput("SESceneManager start3DScene enter.");
        while (mShouldWait) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (HomeUtils.DEBUG) {
            Log.d("SE3DAppManager:", "start3DScreen name :" + sceneName);
        }
        createTypeManager();
        Cursor cursor = ProviderUtils.querySceneInfo(getContext(), sceneName);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                SESceneInfo sceneInfo = SESceneInfo.CreateFromDB(cursor);
                SEScene screen = HomeUtils.getSceneByClassName(HomeScene.class.getName(), sceneInfo.mSceneName);
                screen.mSceneInfo = sceneInfo;
                addScene(screen);
                mCurrentScreen = screen;
                debugOutput("SESceneManager start3DScene to load mount point.");
                mCurrentScreen.createMountPointManager();
                debugOutput("SESceneManager start3DScene scene ready.");
                mCurrentScreen.startScene(null);
            }
        }finally {
            if (cursor  != null) {
                cursor.close();
                debugOutput("SESceneManager start3DScene exit.");
            }
        }
    }

    public void onActivityDestroy() {
        debugOutput("SESceneManager onActivityDestroy enter.");
        if (mCurrentScreen != null) {
            mCurrentScreen.onActivityDestory();
        }
        debugOutput("SESceneManager onActivityDestroy exit.");
    }

    public void onActivityPause() {
        debugOutput("SESceneManager onActivityPause enter.");
        if (mCurrentScreen != null) {
            mCurrentScreen.onActivityPause();
        }
        debugOutput("SESceneManager onActivityPause exit.");
    }

    public void onActivityResume() {
        debugOutput("SESceneManager onActivityResume enter.");
        for (SESceneManager.TimeChangeCallBack callBack : mSESceneManager.mTimeCallBacks) {
            if (callBack != null) {
                callBack.onTimeChanged();
            }
        }
        if (mCurrentScreen != null) {
        	if(isOrientationChanged()) {
        		onCurrentSceneCreated();
        		resetCurrentScene();
        	}else {
                mCurrentScreen.onActivityResume();
        	}
        }
        debugOutput("SESceneManager onActivityResume exit.");
    }

    public void onActivityRestart() {
        debugOutput("SESceneManager onActivityRestart enter.");
        if (mCurrentScreen != null) {
            mCurrentScreen.onActivityRestart();
        }
        debugOutput("SESceneManager onActivityRestart exit.");
    }

    private long mPreTime = 0;
    int count = 0;

    public boolean update() {
        if (mFPSView != null) {
            long curTime = System.currentTimeMillis();
            count++;
            if (curTime - mPreTime >= 1000) {
                final float fps = count * 1000.0f / (curTime - mPreTime);
                runInUIThread(new Runnable() {
                    public void run() {
                        mFPSView.setText(fps + " fps");
                    }
                });
                mPreTime = curTime;
                count = 0;
            }
        }
        if (mCurrentScreen != null) {
            return mCurrentScreen.update();
        }
        return false;
    }

    private int mPreX;
    private int mPreY;

    private boolean mMoveStarted = false;
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        if (mCurrentScreen != null) {
            final MotionEvent event = MotionEvent.obtain(ev);
            switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_CANCEL:
                mPreX = x;
                mPreY = y;
                new SECommand(mCurrentScreen) {
                    public void run() {
                        mCurrentScreen.dispatchTouchEvent(event);
                    }
                }.execute();
                mMoveStarted = false;
                break;
            case MotionEvent.ACTION_MOVE:
                int slop = mTouchSlop;
                boolean shouldMove = false;
                if (!mMoveStarted) {
                    shouldMove = Math.pow((x - mPreX), 2) + Math.pow((y - mPreY), 2) > Math.pow(slop, 2);
                } else {
                    shouldMove = true;
                }
                if (shouldMove) {
                    new SECommand(mCurrentScreen) {
                        public void run() {
                            mCurrentScreen.dispatchTouchEvent(event);
                        }
                    }.execute();
                    mPreX = x;
                    mPreY = y;
                    mMoveStarted = true;
                }
                break;
            default:
                mMoveStarted = false;
                break;
            }
        }
        return true;
    }

    public void handleMenuKey() {
        if (mCurrentScreen != null) {
            new SECommand(mCurrentScreen) {
                public void run() {
                    mCurrentScreen.handleMenuKey();
                }
            }.execute();
        }
    }

    public void handleBackKey() {
        if (mCurrentScreen != null) {
            new SECommand(mCurrentScreen) {
                public void run() {
                    mCurrentScreen.handleBackKey(null);
                }
            }.execute();
        }
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        debugOutput("SESceneManager onActivityResult enter.");
        if (mCurrentScreen != null) {
            mCurrentScreen.onActivityResult(requestCode, resultCode, data);
        }
        debugOutput("SESceneManager onActivityResult exit.");
    }

    public void onNewIntent(final Intent intent) {
        debugOutput("SESceneManager onNewIntent enter.");
        if (mCurrentScreen != null) {
            new SECommand(mCurrentScreen) {
                public void run() {
                    mCurrentScreen.onNewIntent(intent);
                    debugOutput("SESceneManager onNewIntent final result.");
                }
            }.execute();
        }
        debugOutput("SESceneManager onNewIntent exit.");
    }

    public boolean onMenuOpened(int featureId, Menu menu) {
        if (mCurrentScreen != null) {
            return mCurrentScreen.onCreateOptionsMenu(menu);
        }
        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        if (mCurrentScreen != null) {
            return mCurrentScreen.onCreateOptionsMenu(menu);
        }
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mCurrentScreen != null) {
            return mCurrentScreen.onPrepareOptionsMenu(menu);
        }
        return true;
    }

    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mCurrentScreen != null) {
            new SECommand(mCurrentScreen) {
                public void run() {
                    mCurrentScreen.onOptionsItemSelected(item);
                }
            }.execute();
        }
        return true;
    }

    public Dialog onCreateDialog(int id) {
        if (mCurrentScreen != null) {
            return mCurrentScreen.onCreateDialog(id);
        }
        return null;
    }

    public void onPrepareDialog(int id, Dialog dialog, Bundle bundles) {
        if (mCurrentScreen != null) {
            mCurrentScreen.onPrepareDialog(id, dialog, bundles);
        }
    }

    public SECamera getCurrentScreenCamera() {
        if (mCurrentScreen != null) {
            return mCurrentScreen.getCamera();
        }
        return null;
    }

    

    private LoopForInfoService mWeatherService;

    public void bindWeatherService() {
        if (mWeatherService == null) {
            debugOutput("SESceneManager bindWeatherService enter.");
            Context context = getContext();
            Intent intent = new Intent();
            intent.setClass(context, LoopForInfoService.class);
            context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            debugOutput("SESceneManager bindWeatherService exit.");
        }
    }

    public void unBindWeatherService() {
        getContext().unbindService(mConnection);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            if (service == null) {
                return;
            }
            mWeatherService = ((LoopForInfoService.LocalBinder) service).getService();
            debugOutput("SESceneManager weather service connected.");
        }

        public void onServiceDisconnected(ComponentName arg0) {
            mWeatherService = null;
        }
    };

    public LoopForInfoService getWeatherService() {
        return mWeatherService;
    }

    public void addUnlockScreenListener(final UnlockScreenListener callBack) {
        runInUIThread(new Runnable() {
            public void run() {
                if (!mUnlockScreenListeners.contains(callBack)) {
                    mUnlockScreenListeners.add(callBack);
                }
            }
        });
    }

    public void removeUnlockScreenListener(final UnlockScreenListener callBack) {
        runInUIThread(new Runnable() {
            public void run() {
                if (callBack != null) {
                    mUnlockScreenListeners.remove(callBack);
                }
            }
        });
    }

    public ModelObjectsManager getModelManager() {
        return mModelObjectsManager;
    }

    public void addModelToScene(ModelInfo modelInfo) {
        if (mModelObjectsManager != null) {
            mModelObjectsManager.mModels.put(modelInfo.mName, modelInfo);
            ModelChangeCallBack modelChangeCallBack = (ModelChangeCallBack) getDefaultScene();
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
            ModelChangeCallBack modelChangeCallBack = (ModelChangeCallBack) getDefaultScene();
            if (modelChangeCallBack != null) {
                modelChangeCallBack.onRemoveModelToDB(name);
            }
        }
    }

    public void delay() {
        if (mCurrentScreen != null) {
            mCurrentScreen.delay();
        }
    }

    private synchronized void dataBaseIsReady() {
        mShouldWait = false;
        this.notifyAll();
    }
    
    private int mCurrentOrientation;
    private void onCurrentSceneCreated() {
    	// read and set current orientation from settings.
    	mCurrentOrientation = SettingsActivity.getPreferRotation(getContext());
    }
    private boolean isOrientationChanged() {
    	return mCurrentOrientation != SettingsActivity.getPreferRotation(getContext());
    }
    
    private void resetCurrentScene() {
    	// destroy current scene and reload
    	mCurrentScreen.onActivityDestory();
    	softRemoveScene(mCurrentScreen);
//    	removeScene(mCurrentScreen);
    	mCurrentScreen = null;

    	HomeDataBaseHelper help = HomeDataBaseHelper.getInstance(mGLActivity);
        SQLiteDatabase db = help.getWritableDatabase();
        help.loadObjectInfo(db);
        help.reloadCameraSceneData(db);
    	setDefaultScene();
    }
    public static float getWallRatioH2W() {
        SEVector.SEVector2f wallXz = ModelInfo.getHouseObject(SESceneManager.getInstance().getCurrentScene()).getWallXZBounder();
        return wallXz.getY() / wallXz.getX();
    }
}