package com.borqs.se.home3d;

import java.math.RoundingMode;
import java.text.DecimalFormat;

import android.app.*;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.widget.Toast;

import com.borqs.freehdhome.R;
import com.borqs.market.json.Product;
import com.borqs.market.utils.MarketUtils;
import com.borqs.se.home3d.ProviderUtils.ModelColumns;
import com.borqs.se.home3d.ProviderUtils.ThemeColumns;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.upgrade.UpgradeTest;
import com.borqs.se.widget3d.ADViewController;
import com.support.StaticUtil;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener {
    private static final String KEY_THEME_SETTING = "switch_theme_key_8";
    private static final String KEY_FPS = "fps_key";
    private static final String KEY_DISPLAYSETTING = "DisplaySetting";
    private static final String KEY_HELP = "key_help";
    private static final String KEY_TEST_OR_RELEASE = "key_test_or_release";
//    private static final String KEY_UPGRADE_VIEW = "upgrade_key";
    public static final String PREFS_SETTING_NAME = "com.borqs.se_preferences";
    private static final String KEY_HELP_MENU = "help";
    private static final String key_DB_VERSION = "DB_VERSION";
    private static final String KEY_NEVER_SHOW_MENU_BTN = "never_show_menu_btn";
    private static final String KEY_FEED_BACK = "feedback";
    private static final String KEY_XML_VERSION = "XML_VERSION";
    private static final String KEY_THEME_CONFIG = "theme_config";
    private static final String KEY_SCORE = "score";
    private static final String KEY_SHARE = "share";
    private static final String KEY_APPICON_BACKGROUND = "objects_background_key";
    private static final String KEY_USE_NEW_WALLPAPER = "user_new_wallpaper";
    private static final String KEY_CUSTOMIZE_WALLPAPER_TIME_STAMP = "KEY_CUSTOMIZE_WALLPAPER_TIME_STAMP";
    private static final String KEY_APPLY_WALLPAPER_SUITE_PREFIX = "KEY_APPLY_WALLPAPER_SUITE_PREFIX";
    private static final String KEY_WALLPAPER = "wallpaper_key";
    private static final String KEY_SHOW_SHELF = "show_shelf_key";
    private static final String KEY_USER_SHARE = "user_share_key";

    private static final String KEY_AD_DISMISS = "ad_dismiss_flyer";
    private static final String KEY_AD_CONTENT = "ad_content";
    private static final String KEY_AD_REMOVAL = "ad_customization_key";


//    private static final String ACRA_REPORT_TIME = "acra_report_time";
//    private static final String ACRA_REPORT_COUNT = "acra_report_count";
    private static final String UNLOCK_SCREEN_KEY = "unlock_screen_key";
    private static final String FULL_SCREEN_KEY = "full_screen_key";

    private static final String KEY_ORIENTATION_PREFERRED = "orientation_preferred_key";
//    private static final String PREFER_ORIENTATION_KEY = "prefer_orientation_key";
    public static final String OPTIONMENU_KEY_PREFERENCES_SCREENINDICATOR = "preferences_screenindicator";
    private static final String BACKDOOR_OPEN = "BACKDOOR_OPEN";

    public static final int ALERT_DIALOG_UPDATE_SW = 0;
    public static final int PROGRESS_DIALOG_UPDATE_SW = 1;

    public static final int MSG_SHOW_DIALOG_UPGRADE = 0;
    public static final int MSG_START_UPGRADE = MSG_SHOW_DIALOG_UPGRADE + 1;
    public static final int MSG_ERROR_REMOVE_UPGRADE_PD = MSG_START_UPGRADE + 1;
    public static final int MSG_LATEST_REMOVE_UPGRADE_PD = MSG_ERROR_REMOVE_UPGRADE_PD + 1;

    private CheckBoxPreference mPrefFPS;
//    private PreferenceScreen mPrefUpgrade;
//    private ListPreference mIconBackgroudListPreference;
    private ListPreference mOrientationListPreference;
    
    private final static String ORIENTATION_ROTATION = "rotation";
    public final static String ORIENTATION_LANDSCAPE = "landscape";
    public final static String ORIENTATION_PORTRAIT = "portrait";

    private PreferenceGroup mDisplay;
    private PreferenceScreen mAdRemoval;
    private EditTextPreference mADContent;
    private CheckBoxPreference mADDismiss;

    private ProgressDialog mUpdateSwPD;
    private String mUpgradeUrl;
    private StringBuilder mReleaseNode;
    private UpgradeTest mUpgradeDetector;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_SHOW_DIALOG_UPGRADE:
                removeDialog(PROGRESS_DIALOG_UPDATE_SW);
                Bundle data = msg.getData();
                int curVersion = data.getInt("cur_version");
                int latestVersion = data.getInt("latest_version");
                long size = data.getLong("size");
                DecimalFormat formater = new DecimalFormat();
                formater.setMaximumFractionDigits(2);
                formater.setGroupingSize(0);
                formater.setRoundingMode(RoundingMode.FLOOR);
                String fileSize = formater.format(size / (1024f * 1024f)) + "MB";
                mUpgradeUrl = data.getString("url");
                String releaseNotes = data.getString("release_note");
                mReleaseNode = new StringBuilder();
                Resources res = getResources();
                mReleaseNode.append(res.getString(R.string.upgrade_dialog_msg));
                mReleaseNode.append("\r\n\r\n");
                mReleaseNode.append(res.getString(R.string.upgrade_dialog_current_version));
                mReleaseNode.append(curVersion);
                mReleaseNode.append("\r\n");
                mReleaseNode.append(res.getString(R.string.upgrade_dialog_latest_version));
                mReleaseNode.append(latestVersion);
                mReleaseNode.append("\r\n");
                mReleaseNode.append(res.getString(R.string.upgrade_dialog_file_size));
                mReleaseNode.append(fileSize);
                mReleaseNode.append("\r\n");
                mReleaseNode.append(res.getString(R.string.upgrade_dialog_update_changes));
                mReleaseNode.append("\r\n");
                mReleaseNode.append(releaseNotes);
                showDialog(ALERT_DIALOG_UPDATE_SW);
                break;
            case MSG_START_UPGRADE:
                showDialog(PROGRESS_DIALOG_UPDATE_SW);
                if (mUpgradeDetector != null) {
                    mUpgradeDetector.stopUpgrade();
                }
                mUpgradeDetector = new UpgradeTest(SettingsActivity.this, this);
                mUpgradeDetector.start();
                break;
            case MSG_LATEST_REMOVE_UPGRADE_PD:
                removeDialog(PROGRESS_DIALOG_UPDATE_SW);
                Toast.makeText(SESceneManager.getInstance().getContext(), R.string.no_update, Toast.LENGTH_SHORT)
                        .show();
                break;
            case MSG_ERROR_REMOVE_UPGRADE_PD:
                removeDialog(PROGRESS_DIALOG_UPDATE_SW);
                Toast.makeText(SESceneManager.getInstance().getContext(), R.string.check_update_error,
                        Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.menu_preference);
            actionBar.setDisplayShowTitleEnabled(true);
        }
        addPreferencesFromResource(R.xml.settings);
        initSetting();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelListenBackToScene();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!ADViewController.isRemoveAD()) {
            mDisplay.removePreference(mADContent);
            mDisplay.removePreference(mADDismiss);
            mDisplay.addPreference(mAdRemoval);
        } else {
            mDisplay.addPreference(mADDismiss);
            mDisplay.addPreference(mADContent);
            mDisplay.removePreference(mAdRemoval);
            if (isAdDismissed()) {
                mADContent.setEnabled(false);
            } else {
                mADContent.setEnabled(true);
            }
        }

        cancelListenBackToScene();
    }

    @Override
    protected void onPause() {
        mHandler.removeMessages(MSG_START_UPGRADE);
        mHandler.removeMessages(MSG_SHOW_DIALOG_UPGRADE);
        removeDialog(ALERT_DIALOG_UPDATE_SW);
        removeDialog(PROGRESS_DIALOG_UPDATE_SW);
        if (mUpgradeDetector != null) {
            mUpgradeDetector.stopUpgrade();

        }
        super.onPause();

        listenBackToScene();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case ALERT_DIALOG_UPDATE_SW:
            AlertDialog alertDialog = new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.upgrade_dialog_title)
                    .setPositiveButton(R.string.upgrade_dialog_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, final int whichButton) {
                            dialog.dismiss();
                            if (mUpgradeUrl != null) {
                                try {
                                    Uri uri = Uri.parse(mUpgradeUrl);
                                    Intent it = new Intent(Intent.ACTION_VIEW, uri);
                                    startActivity(it);
                                } catch (ActivityNotFoundException e) {
                                    Toast.makeText(SESceneManager.getInstance().getContext(), R.string.activity_not_found,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }).setNegativeButton(R.string.upgrade_dialog_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, final int whichButton) {
                            dialog.dismiss();
                        }
                    }).create();
            if (mReleaseNode != null) {
                alertDialog.setMessage(mReleaseNode);
            }
            return alertDialog;
        case PROGRESS_DIALOG_UPDATE_SW:
            if (mUpdateSwPD == null) {
                mUpdateSwPD = new ProgressDialog(this);
                mUpdateSwPD.setTitle(R.string.wait_dialog_title);
                mUpdateSwPD.setMessage(getString(R.string.wait_dialog_msg_sw));
                mUpdateSwPD.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        mHandler.removeMessages(MSG_START_UPGRADE);
                        mHandler.removeMessages(MSG_SHOW_DIALOG_UPGRADE);
                        if (mUpgradeDetector != null) {
                            mUpgradeDetector.stopUpgrade();

                        }
                    }
                });
            }
            if (!mUpdateSwPD.isShowing()) {
                return mUpdateSwPD;
            }
            break;
        default:
            break;
        }
        return null;
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
        case ALERT_DIALOG_UPDATE_SW:
            AlertDialog alertDialog = (AlertDialog) dialog;
            if (mReleaseNode != null) {
                alertDialog.setMessage(mReleaseNode.toString());
            }
            break;
        }
    }

    private void initSetting() {
        mDisplay = (PreferenceGroup) getPreferenceScreen().findPreference(KEY_DISPLAYSETTING);
        mPrefFPS = (CheckBoxPreference) findPreference(KEY_FPS);
        mPrefFPS.setChecked(getFPSSetting(this));
        mPrefFPS.setOnPreferenceChangeListener(this);
        if (!HomeUtils.DEBUG) {
            mDisplay.removePreference(mPrefFPS);
        }

        CheckBoxPreference unlockScreenAnim = (CheckBoxPreference) findPreference(UNLOCK_SCREEN_KEY);
        unlockScreenAnim.setChecked(isEnableUnlockScreenAnim(this));
        unlockScreenAnim.setOnPreferenceChangeListener(this);

        CheckBoxPreference fullScreen = (CheckBoxPreference) findPreference(FULL_SCREEN_KEY);
        fullScreen.setChecked(isEnableFullScreen(this));
        fullScreen.setOnPreferenceChangeListener(this);

        CheckBoxPreference showShelf = (CheckBoxPreference)findPreference(KEY_SHOW_SHELF);
        showShelf.setChecked(isShowShelf(this));
        showShelf.setOnPreferenceChangeListener(this);

//        mPrefUpgrade = (PreferenceScreen) findPreference(KEY_UPGRADE_VIEW);
//        String summary = this.getResources().getString(R.string.upgrade_dialog_current_version);
//        String packageName = getPackageName();
//        String version = "";
//        try {
//            version = getPackageManager().getPackageInfo(packageName, 0).versionName;
//        } catch (NameNotFoundException e) {
//            e.printStackTrace();
//        }
//        mPrefUpgrade.setSummary(summary + version);
//        mPrefUpgrade.setOnPreferenceClickListener(this);
        Preference help = findPreference(KEY_HELP_MENU);
        help.setOnPreferenceClickListener(this);

        Preference score = findPreference(KEY_SCORE);
        score.setOnPreferenceClickListener(this);

        Preference share = findPreference(KEY_SHARE);
        share.setOnPreferenceClickListener(this);

        PreferenceScreen feedback = (PreferenceScreen) findPreference(KEY_FEED_BACK);
        feedback.setOnPreferenceClickListener(this);

        Preference wallpaper = findPreference(KEY_WALLPAPER);
        if (null != wallpaper) {
            wallpaper.setOnPreferenceClickListener(this);
        }

        Preference userShare = findPreference(KEY_USER_SHARE);
        if (null != userShare) {
            userShare.setOnPreferenceClickListener(this);
        }

//        Preference objectBackground = findPreference(KEY_APPICON_BACKGROUND);
//        objectBackground.setOnPreferenceChangeListener(this);

        mOrientationListPreference = (ListPreference)findPreference(KEY_ORIENTATION_PREFERRED);
        mOrientationListPreference.setOnPreferenceChangeListener(this);
        if(getPreferRotation(this) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
        	mOrientationListPreference.setValueIndex(0);
        }else {
        	mOrientationListPreference.setValueIndex(1);
        }
        mOrientationListPreference.setSummary(mOrientationListPreference.getEntry());

        mADContent = (EditTextPreference) findPreference(KEY_AD_CONTENT);
        mADContent.setOnPreferenceChangeListener(this);

        mADDismiss = (CheckBoxPreference)findPreference(KEY_AD_DISMISS);
        mADDismiss.setChecked(isAdDismissed());
        mADDismiss.setOnPreferenceChangeListener(this);

        mAdRemoval = (PreferenceScreen)findPreference(KEY_AD_REMOVAL);
        mAdRemoval.setOnPreferenceClickListener(this);
        setupBackdoor(mDisplay);
    }

    private boolean isAdDismissed() {
        return isAdDismissed(this);
    }

    public static boolean isAdDismissed(Context context) {
        return getBooleanPreference(context, KEY_AD_DISMISS);
    }

    private boolean isBackdoorOpen() {
        if (HomeUtils.DEBUG) {
            return true;
        }
        return false;
    }
    private void setupBackdoor(PreferenceGroup display) {
        if (!isBackdoorOpen()) {
//            display.removePreference(mOrientationListPreference);

            Preference preference = findPreference("theme_key");
            if (null != preference) {
                display.removePreference(preference);
            }

            preference = findPreference(KEY_WALLPAPER);
            if (null != preference) {
                display.removePreference(preference);
            }
        }


        CheckBoxPreference pref_screenindicator = (CheckBoxPreference)findPreference(OPTIONMENU_KEY_PREFERENCES_SCREENINDICATOR);
        if (pref_screenindicator != null) {
            pref_screenindicator.setOnPreferenceChangeListener(this);
        }
    }

    public boolean onPreferenceClick(Preference preference) {
        /*if (preference.getKey().equals(KEY_UPGRADE_VIEW)) {
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isAvailable()) {
                mHandler.removeMessages(MSG_START_UPGRADE);
                mHandler.sendEmptyMessageDelayed(MSG_START_UPGRADE, 500);
            } else {
                Toast.makeText(SESceneManager.getInstance().getContext(), R.string.network_error, Toast.LENGTH_SHORT)
                        .show();
            }
            return true;
        } else */if (KEY_HELP_MENU.equals(preference.getKey())) {
            finish();
            setHelpStatus(this, false);
        } else if (KEY_FEED_BACK.equals(preference.getKey())) {
            HomeUtils.showFeedbackActivity(this);
        } else if (KEY_SCORE.equals(preference.getKey())) {
            Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(getScoreContent()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            }catch (ActivityNotFoundException e) {
                Toast.makeText(SESceneManager.getInstance().getContext(), R.string.activity_not_found,
                        Toast.LENGTH_SHORT).show();
            }
        } else if (KEY_SHARE.equals(preference.getKey())) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_content, getChannelSharedContent(), HomeUtils.PKG_CURRENT_NAME));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            }catch (ActivityNotFoundException e) {
                Toast.makeText(SESceneManager.getInstance().getContext(), R.string.activity_not_found,
                        Toast.LENGTH_SHORT).show();
            }
        } else if (KEY_WALLPAPER.equalsIgnoreCase(preference.getKey())) {
            HomeUtils.showWallpapers(this);
        } else if (KEY_AD_REMOVAL.equals(preference.getKey())) {
            if (ADViewController.getInstance().onUpgradeAppIntent()) {
                finish();
            } else {

            }
        } else if (KEY_USER_SHARE.equalsIgnoreCase(preference.getKey())) {
            final boolean isPort = getPreferRotation(this) == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            final String supportMode = isPort ? Product.SupportedMod.PORTRAIT : Product.SupportedMod.LANDSCAPE;
//            MarketUtils.startUserShareListIntent(this, Product.ProductType.WALL_PAPER, supportMode);
            HomeUtils.exportOrImportWallpaper(this);
        }
        return false;
    }

    private String getScoreContent() {
        final String channelCode = getChannelCode(this);
        if (getString(R.string.channel_amazon).equalsIgnoreCase(channelCode)) {
            return getString(R.string.share_content_link_amazon) + "=" + HomeUtils.PKG_CURRENT_NAME + "&showAll=1";
        }

        return "market://details?id=" + getPackageName();
    }
    private String getChannelSharedContent() {
        final String channelCode = getChannelCode(this);
        if (getString(R.string.channel_amazon).equalsIgnoreCase(channelCode)) {
            return getString(R.string.share_content_link_amazon);
        }

        return getString(R.string.share_content_link_google);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference.getKey().equals(KEY_FPS)) {
            boolean state = (Boolean) objValue;
            SharedPreferences settings = getSharedPreferences(PREFS_SETTING_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(KEY_FPS, state);
            editor.commit();
            getFPSSetting(this);
            if (state) {
                SESceneManager.getInstance().showFPSView();
                SESceneManager.getInstance().getGLSurfaceView().setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            } else {
                SESceneManager.getInstance().clearFPSView();
                SESceneManager.getInstance().getGLSurfaceView().setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            }
        } else if (UNLOCK_SCREEN_KEY.equals(preference.getKey())) {
            boolean state = (Boolean) objValue;
            SharedPreferences settings = getSharedPreferences(PREFS_SETTING_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(UNLOCK_SCREEN_KEY, state);
            editor.commit();
        } else if (FULL_SCREEN_KEY.equals(preference.getKey())) {
            boolean state = (Boolean) objValue;
            SharedPreferences settings = getSharedPreferences(PREFS_SETTING_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(FULL_SCREEN_KEY, state);
            editor.commit();
        } else if (KEY_APPICON_BACKGROUND.equals(preference.getKey())) {
            saveAppIconBackgroundName(this, (String)objValue);
        } else if (KEY_ORIENTATION_PREFERRED.equals(preference.getKey())) {
            Object currentValue = getPreferRotation(this);
            if (!currentValue.equals(objValue)) {
                saveOrientationName(this, (String)objValue);
                final int index = mOrientationListPreference.findIndexOfValue((String)objValue);
                final CharSequence summary = mOrientationListPreference.getEntries()[index];
                mOrientationListPreference.setSummary(summary);

                MarketUtils.updatePlugIn(this, getApplyWallpaperSuite(this),
                        true, MarketUtils.CATEGORY_WALLPAPER);
                finish();
            }
        } else if (preference.getKey().equals(OPTIONMENU_KEY_PREFERENCES_SCREENINDICATOR)) {
            updateScreenIndicatorStatus(objValue);
        } else if(KEY_SHOW_SHELF.equalsIgnoreCase(preference.getKey())) {
            final boolean state = (Boolean) objValue;
            SharedPreferences settings = getSharedPreferences(PREFS_SETTING_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(KEY_SHOW_SHELF, state);
            editor.commit();
            SESceneManager sceneManager = SESceneManager.getInstance();
            final SEScene scene =  sceneManager.getCurrentScene();
            scene.setShelfVisibility(state);
            /*
            new SECommand(scene) {
                public void run() {


                }
            }.execute();
            */
        }  else if (KEY_AD_CONTENT.equals(preference.getKey())) {
            if (!TextUtils.isEmpty((String) objValue)) {
                saveAdContent(this, (String) objValue);
                preference.setSummary((String) objValue);
            }
        } else if (KEY_AD_DISMISS.equals(preference.getKey())) {
            final boolean dismiss = (Boolean)objValue;
            saveBooleanPreference(this, KEY_AD_DISMISS, dismiss);
            ADViewController.getInstance().dismissFlyer(dismiss);
            if (dismiss) {
                mADContent.setEnabled(false);
            } else {
                mADContent.setEnabled(true);
            }
        }

        return true;
    }

    public static boolean getHelpStatus(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getBoolean(KEY_HELP, false);
    }

    public static void setHelpStatus(Context context, boolean flag) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(KEY_HELP, flag);
        editor.commit();
    }

    public static boolean getFPSSetting(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getBoolean(KEY_FPS, false);

    }

    public static boolean getIsTestOrRelease(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getBoolean(KEY_TEST_OR_RELEASE, false);
    }

    public static void saveNeverShowMenuBtn(Context context, boolean isNever) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(KEY_NEVER_SHOW_MENU_BTN, isNever);
        editor.commit();
    }

    public static boolean getNeverShowMenuBtn(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getBoolean(KEY_NEVER_SHOW_MENU_BTN, false);
    }

    public static void saveIsTestOrRelease(Context context, boolean isTest) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(KEY_TEST_OR_RELEASE, isTest);
        editor.commit();
    }

    public static int getVersionCode(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getInt("version_code", 0);
    }

    public static void saveVersionCode(Context context, int versionCode) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("version_code", versionCode);
        editor.commit();
    }

    public static int getDBVersion(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getInt(key_DB_VERSION, 0);
    }

    public static void saveDBVersion(Context context, int value) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key_DB_VERSION, value);
        editor.commit();
    }

    public static int getXMLVersion(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getInt(KEY_XML_VERSION, 0);
    }

    public static void saveXMLVersion(Context context, int value) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(KEY_XML_VERSION, value);
        editor.commit();
    }

    public static String getAppIconBackgroundName(Context context){
        SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_SETTING_NAME, 0);
        return settings.getString(KEY_APPICON_BACKGROUND, "0");
    }

    public static void saveAppIconBackgroundName(Context context, String value){

        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(KEY_APPICON_BACKGROUND, value);
        editor.commit();
        String  where = ModelColumns.IAMGE_NAME + " = 'home_appwall05_zch.png@appwall1_basedata.cbf'";
        String newPath = null;
        if ("black".equals(value)) {
            newPath = "assets/base/appwall/home_appwall_black.png";
        } else if ("brown".equals(value)) {
            newPath = "assets/base/appwall/home_appwall_brown.png";
        } else if ("gray".equals(value)) {
            newPath = "assets/base/appwall/home_appwall_gray.png";
        } else {
            newPath = "assets/base/appwall/home_appwall06_zch.png";
        }
        ContentValues iconValues = new ContentValues();
        iconValues.put(ModelColumns.IMAGE_NEW_PATH, newPath);
        context.getContentResolver().update(ModelColumns.IMAGE_INFO_URI, iconValues, where, null);
    }

    public static String getThemeName(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_SETTING_NAME, 0);
        String themeName = settings.getString(KEY_THEME_SETTING, "default");
        if ("0".equals(themeName)) {
            themeName = "default";
        } else if ("1".equals(themeName)) {
            themeName = "white";
        } else if ("2".equals(themeName)) {
            themeName = "dark";
        } else {
            return themeName;
        }
        String where = ThemeColumns.NAME + "='" + themeName + "'";
        String config = null;
        Cursor cursor = context.getContentResolver().query(ThemeColumns.CONTENT_URI, new String[]{ThemeColumns.CONFIG}, where, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            config = cursor.getString(0);
            saveThemeName(context, themeName, config);
        }
        if (cursor != null) {
            cursor.close();
        }
        return themeName;
    }

    public static void saveThemeName(Context context, String themeName, String themeCongig) {
        SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(KEY_THEME_SETTING, themeName);
        editor.putString(KEY_THEME_CONFIG, themeCongig);
        editor.commit();
    }
    
    public static boolean isChangeWallPaper(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_SETTING_NAME, 0);
        return settings.getBoolean(getNewWallpaperKey(), false);
    }

    public static long getWallpaperCustomizedTimeStamp(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_SETTING_NAME, 0);
        return settings.getLong(getCustomizedWallpaperKey(), -1);
    }
    
    public static void saveChangeWallPaper(Context context, boolean flag) {
        SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(getNewWallpaperKey(), flag);
        editor.commit();
    }

    public static void saveWallpaperCustomizedTimeStamp(Context context, boolean customized) {
        SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(getCustomizedWallpaperKey(), customized ? System.currentTimeMillis() : -1);
        editor.commit();
    }


    public static String getApplyWallpaperSuite(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_SETTING_NAME, 0);
        return settings.getString(getApplyWallpaperSuiteKey(), "0");
    }

    public static void saveApplyWallpaperSuite(Context context, final String productId) {
        SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(getApplyWallpaperSuiteKey(), productId);
        editor.commit();
    }

//    public static void applyWallpaperSuite(Context context, final String productId) {
//        saveApplyWallpaperSuite(context, productId);
//        saveChangeWallPaper(context, true);
//        MarketUtils.updatePlugIn(context, productId, true, MarketUtils.CATEGORY_WALLPAPER);
//    }

    private static String getApplyWallpaperSuiteKey() {
        return KEY_APPLY_WALLPAPER_SUITE_PREFIX + getPreferRotation(SESceneManager.getInstance().getGLActivity());
    }
    private static String getNewWallpaperKey() {
        return KEY_USE_NEW_WALLPAPER + getPreferRotation(SESceneManager.getInstance().getGLActivity());
    }

    private static String getCustomizedWallpaperKey() {
        return KEY_CUSTOMIZE_WALLPAPER_TIME_STAMP + getPreferRotation(SESceneManager.getInstance().getGLActivity());
    }

    public static String getThemeConfig(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_SETTING_NAME, 0);
        return settings.getString(KEY_THEME_CONFIG, null);
    }

    public static boolean isEnableUnlockScreenAnim(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getBoolean(UNLOCK_SCREEN_KEY, false);

    }
    public static boolean isShowShelf(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getBoolean(KEY_SHOW_SHELF, true);
    }
    public static boolean isEnableFullScreen(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getBoolean(FULL_SCREEN_KEY, false);
    }

    public static int getPreferRotation(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        final String orientation = settings.getString(KEY_ORIENTATION_PREFERRED, ORIENTATION_LANDSCAPE);
        /*if (orientation.equals(ORIENTATION_ROTATION)) {
            return ActivityInfo.SCREEN_ORIENTATION_SENSOR;
        } else if (orientation.equals(ORIENTATION_LANDSCAPE)) {
            return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        } else if (orientation.equals(ORIENTATION_PORTRAIT)) {
            return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else {
            return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        }*/
        
        if (orientation.equals(ORIENTATION_PORTRAIT)) {
            return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else {
            return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
    }

    public static void checkOrientationSettings(HomeActivity activity) {
        int rotation = getPreferRotation(activity);
//        String orientationName = "";
//        if (ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED == rotation) {
//        	if(activity.isScreenLarge()) {
//        		rotation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ;
//        		orientationName = ORIENTATION_LANDSCAPE;
//        	}else {
//        		rotation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ;
//        		orientationName = ORIENTATION_PORTRAIT;
//        	}
//        	saveOrientationName(activity, orientationName);
//        }
        activity.setRequestedOrientation(rotation);
    }

    public static void saveOrientationName(Context context, String value){
        saveStringPreference(context, KEY_ORIENTATION_PREFERRED, value);
    }

    public static void saveBackdoorState(Context context, boolean open) {
        saveBooleanPreference(context, BACKDOOR_OPEN, open);
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFS_SETTING_NAME, 0);
    }
    private static int getIntPreference(Context context, String key, int defaultValue) {
        SharedPreferences settings = getSharedPreferences(context);
        return settings.getInt(key, defaultValue);
    }
    private static void saveIntPreference(Context context, String key, int value) {
        SharedPreferences settings = getSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.commit();
    }
    private static void saveStringPreference(Context context, String key, String value) {
        SharedPreferences settings = getSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }
    private static boolean getBooleanPreference(Context context, String key) {
        SharedPreferences settings = getSharedPreferences(context);
        return settings.getBoolean(key, false);
    }
    private static void saveBooleanPreference(Context context, String key, boolean value) {
        SharedPreferences settings = getSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }


    /**/
    public static boolean isScreenIndicatorEnabled(Context context) {
        if (context == null) { return false; }
        return (PreferenceManager.getDefaultSharedPreferences(context)).getBoolean(OPTIONMENU_KEY_PREFERENCES_SCREENINDICATOR, true);
    }

    /**/
    public static void updateScreenIndicatorStatus(Object objValue) {
        // can only do this in gl thread
        SESceneManager.getInstance().removeMessage(HomeScene.MSG_TYPE_SHOW_SCREEN_INDICATOR);
        SESceneManager.getInstance().handleMessage(HomeScene.MSG_TYPE_SHOW_SCREEN_INDICATOR, objValue);
    }

    private BroadcastReceiver mReceiver;
    private void cancelListenBackToScene() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    private void listenBackToScene() {
        if (mReceiver == null) {
            mReceiver = new BackToSceneReceiver();
        }
        registerReceiver(mReceiver, new IntentFilter(MarketUtils.ACTION_MARKET_THEME_APPLY));
        registerReceiver(mReceiver, new IntentFilter(MarketUtils.ACTION_BACK_TO_SCENE));
    }

    public static int getExportVersion(Context context) {
        return getIntPreference(context, "getExportVersion", 0);
    }

    public static int setExportVersion(Context context, int version) {
        return getIntPreference(context, "getExportVersion", version);
    }

    private class BackToSceneReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MarketUtils.ACTION_MARKET_THEME_APPLY.equals(action) ||
                    MarketUtils.ACTION_BACK_TO_SCENE.equalsIgnoreCase(action)) {
                try {
                    mHandler.post(new Runnable() {
                        public void run() {
                            finish();
                        }
                    });
                } catch(Exception e) {

                }
            }
        }

    };
    public static String getChannelCode(Context context) {
        String code = getMetaData(context, context.getString(R.string.key_channel));
        if (code != null) {
            return code;
        }
        return context.getString(R.string.channel_default);
    }

    private static String getMetaData(Context context, String key) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            Object value = ai.metaData.get(key);
            if (value != null) {
                return value.toString();
            }
        } catch (Exception e) {
            //
        }
        return null;
    }

    public static String getAdContent(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getString(KEY_AD_CONTENT, "");
    }

    private void saveAdContent(Context context, String content) {
        SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(KEY_AD_CONTENT, content);
        editor.commit();
        StaticUtil.onEvent(context, "AD_REMOVAL_TEXT", "" + System.currentTimeMillis());
    }

}
