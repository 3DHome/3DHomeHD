package com.borqs.se.home3d;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.location.LocationClient;
import com.borqs.market.utils.MarketUtils;
import com.borqs.freehdhome.R;
import com.borqs.market.utils.MarketConfiguration;
import com.borqs.market.wallpaper.WallpaperUtils;
import com.borqs.se.FileUtils;
import com.borqs.se.download.Utils;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.shortcut.LauncherModel;
import com.borqs.borqsweather.weather.WeatherSettings;
import com.borqs.borqsweather.weather.yahoo.WeatherPreferences;
//import org.acra.ACRA;
//import org.acra.ReportField;
//import org.acra.ReportingInteractionMode;
//import org.acra.annotation.ReportsCrashes;
import com.iab.engine.MarketBillingResult;

//@ReportsCrashes(formKey = "",
//        customReportContent = { ReportField.PACKAGE_NAME,
//        ReportField.ANDROID_VERSION,
//        ReportField.APP_VERSION_CODE,
//        ReportField.APP_VERSION_NAME,
//        ReportField.PHONE_MODEL,
//        ReportField.IS_SILENT,
//        ReportField.USER_COMMENT,
//        ReportField.USER_EMAIL,
//        ReportField.STACK_TRACE
//        },
//        additionalSharedPreferences={"acra.enable", "acra.user.email", "acra.alwaysaccept"},
//        mode = ReportingInteractionMode.SILENT
//)
public class HomeApplication extends Application {
    public static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    public static final String ACTION_UNINSTALL_SHORTCUT = "com.android.launcher.action.UNINSTALL_SHORTCUT";
    LauncherModel mModel;
    private int mOldVersion;
    private int mCurVersion;
    private LocationClient mBDLocationClient;
    private WeatherPreferences mPreferences;

    private static HomeApplication mLauncherApplication;

    public static HomeApplication getInstance() {
        return mLauncherApplication;

    }

    @Override
    public void onCreate() {

        HomeUtils.attachApplication(this);
        super.onCreate();
        if (HomeUtils.isCurrentPackage(getCurProcessName())) {
            mLauncherApplication = this;
//            try {
//                ACRA.init(this);
//            } catch (Exception e) {
//
//            }
//            String formKey = getResources().getString(R.string.google_form_key);
//            ACRA.getErrorReporter().setReportSender(new AcraSender(this, formKey));
            // TODO: send to report to umeng

            mOldVersion = SettingsActivity.getVersionCode(this);
            try {
                mCurVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            initDynamicLibrary("libse.so");
            initDynamicLibrary("libsefactory.so");
            initDynamicLibrary("libmyplugin.so");
            System.load(HomeUtils.PKG_LIB_PATH + "/libsefactory.so");
            System.load(HomeUtils.PKG_LIB_PATH +"/libse.so");
            if (mOldVersion != mCurVersion) {
                WeatherSettings.setGLSPromptStatus(this, false);
            }
            SettingsActivity.saveVersionCode(this, mCurVersion);
//            File file = new File(HomeUtils.SDCARD_PATH + "/.debug");
//            if (file.exists()) {
//                HomeUtils.DEBUG = true;
//            }
            SESceneManager.getInstance();
            mModel = LauncherModel.getInstance();
            mModel.init(this);
            // Register intent receivers
            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            registerReceiver(mModel, filter);
            filter = new IntentFilter();
            filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
            filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
            filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
            registerReceiver(mModel, filter);
            filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            registerReceiver(mModel, filter);
            filter = new IntentFilter(ACTION_INSTALL_SHORTCUT);
            filter.addAction(ACTION_UNINSTALL_SHORTCUT);
            registerReceiver(mModel, filter);
            mBDLocationClient = new LocationClient(this);
            Utils.reportMonitoredApps(getApplicationContext());

            mPreferences = WeatherPreferences.getInstance(this);
            String country = mPreferences.getManualLocationCountryName();
            String province = mPreferences.getManualLocationProvinceName();
            String city = mPreferences.getManualLocationCityName();
            String woeid = mPreferences.getManualWoeid();
            if (!TextUtils.isEmpty(woeid) && TextUtils.isEmpty(country) && TextUtils.isEmpty(province) && TextUtils.isEmpty(city)) {
                mPreferences.setManualWoeid("");
                mPreferences.setManualLocationCountryName("");
                mPreferences.setManualLocationProvinceName("");
                mPreferences.setManualLocationCityName("");
                WeatherSettings.setAutoLocation(this, true);
            }
        }
        MarketConfiguration.init(getApplicationContext());
        MarketConfiguration.setPayType(MarketBillingResult.TYPE_IAB);
        MarketConfiguration.setIabKey(getString(R.string.iab_google_public));
        MarketConfiguration.initLocalWallPaper(getApplicationContext(), WallpaperUtils.getWallpaperFolder(HomeUtils.SDCARD_PATH));
        MarketConfiguration.setIS_DEBUG_BETA_REQUEST(BackDoorSettingsActivity.isDebugBetaRequest(getApplicationContext()));
        MarketConfiguration.setIS_DEBUG_TESTSERVER(BackDoorSettingsActivity.isDebugTestServer(getApplicationContext()));
        MarketConfiguration.setIS_DEBUG_SUGGESTION(BackDoorSettingsActivity.isDebugSuggestion(getApplicationContext()));
        MarketUtils.setLogVisibility(BackDoorSettingsActivity.isMarketLogEnable(getApplicationContext()));
    }
    
    private String getCurProcessName() {
        int pid = android.os.Process.myPid();
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : activityManager.getRunningAppProcesses()) {
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (mModel != null) {
            unregisterReceiver(mModel);
        }
    }

    private void initDynamicLibrary(String lib) {
        String strCpu = Build.CPU_ABI;
        String path = "armeabi/" + lib;
        if (strCpu != null && strCpu.startsWith("x86")) {
            path = "x86/" + lib;
        }
        File dir = getDir("lib", 0);
        File file = new File(dir, lib);
        Log.d("test", "file path : " + file.getPath());
        if (!file.exists() || mOldVersion == 0 || mCurVersion != mOldVersion) {
            try {
                file.createNewFile();
                InputStream is = getAssets().open(path);
                boolean rt = FileUtils.copyToFile(is, file);
                if (HomeUtils.DEBUG)
                    Log.d(HomeUtils.TAG, "initDictionary: copy " + path + rt);
                is.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public LocationClient getBDLocationClient() {
        if (mBDLocationClient == null) {
            mBDLocationClient = new LocationClient(this);
        }
        return mBDLocationClient;
    }
}
