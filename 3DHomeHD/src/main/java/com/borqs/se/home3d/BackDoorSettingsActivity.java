package com.borqs.se.home3d;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.borqs.freehdhome.R;
import com.borqs.market.utils.IntentUtil;
import com.borqs.market.utils.MarketConfiguration;
import com.borqs.market.utils.MarketUtils;
import com.borqs.se.engine.SECameraData;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.upgrade.UpgradeTest;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BackDoorSettingsActivity extends PreferenceActivity implements OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener {
    private static final String KEY_DISPLAYSETTING = "DisplaySetting";
    public static final String PREFS_SETTING_NAME = "com.borqs.se_preferences";

    private static final String BACKDOOR_ENABLE = "backdoor_enable";
    private static final String OPTION_MENU_ALT = "option_menu_alt";

    private static final String KEY_DEBUG_TEST_SERVER = "http_test_server_enable";
    private static final String KEY_DEBUG_SUGGESTION = "market_test_suggestion_enable";
    private static final String KEY_DEBUG_BETA_DATA = "market_test_beta_data_enable";

    private static final String KEY_MARKET_DEBUG_LOG_DATA = "http_market_log_enable";
    private static final String KEY_SCENE_PUBLISH = "publish_scene";
    private static final String KEY_SCENE_EDITOR = "scene_editor";

    public static final int ALERT_DIALOG_UPDATE_SW = 0;
    public static final int PROGRESS_DIALOG_UPDATE_SW = 1;

    public static final int MSG_SHOW_DIALOG_UPGRADE = 0;
    public static final int MSG_START_UPGRADE = MSG_SHOW_DIALOG_UPGRADE + 1;
    public static final int MSG_ERROR_REMOVE_UPGRADE_PD = MSG_START_UPGRADE + 1;
    public static final int MSG_LATEST_REMOVE_UPGRADE_PD = MSG_ERROR_REMOVE_UPGRADE_PD + 1;

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
                mUpgradeDetector = new UpgradeTest(BackDoorSettingsActivity.this, this);
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
        addPreferencesFromResource(R.xml.backdoor_settings);
        initSetting();
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
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case ALERT_DIALOG_UPDATE_SW:
            AlertDialog alertDialog = new AlertDialog.Builder(BackDoorSettingsActivity.this).setTitle(R.string.upgrade_dialog_title)
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
        CheckBoxPreference enableBackdoor = (CheckBoxPreference) findPreference(BACKDOOR_ENABLE);
        enableBackdoor.setChecked(isEnableBackdoor(this));
        enableBackdoor.setOnPreferenceChangeListener(this);

        CheckBoxPreference option_menu_alt = (CheckBoxPreference) findPreference(OPTION_MENU_ALT);
        option_menu_alt.setChecked(isOptionMenuAlt(this));
        option_menu_alt.setOnPreferenceChangeListener(this);

        CheckBoxPreference testServer = (CheckBoxPreference)findPreference(KEY_DEBUG_TEST_SERVER);
        testServer.setChecked(isDebugTestServer(this));
        testServer.setOnPreferenceChangeListener(this);

        CheckBoxPreference debugSuggestion = (CheckBoxPreference)findPreference(KEY_DEBUG_SUGGESTION);
        debugSuggestion.setChecked(isDebugSuggestion(this));
        debugSuggestion.setOnPreferenceChangeListener(this);

        CheckBoxPreference debugBeta = (CheckBoxPreference)findPreference(KEY_DEBUG_BETA_DATA);
        debugBeta.setChecked(isDebugBetaRequest(this));
        debugBeta.setOnPreferenceChangeListener(this);
        CheckBoxPreference marketLog = (CheckBoxPreference)findPreference(KEY_MARKET_DEBUG_LOG_DATA);
        marketLog.setChecked(isMarketLogEnable(this));
        marketLog.setOnPreferenceChangeListener(this);

        Preference publish = findPreference(KEY_SCENE_PUBLISH);
        publish.setOnPreferenceClickListener(this);

        Preference sceneEditor = findPreference(KEY_SCENE_EDITOR);
        sceneEditor.setOnPreferenceClickListener(this);
    }

    public boolean onPreferenceClick(Preference preference) {
        if (KEY_SCENE_PUBLISH.equals(preference.getKey())) {
            Intent data = new Intent();
            data.putExtra("COMMENT", "Hi, here is the theme of my home to you, enjoy yourself.");

            // todo : zip the theme to a path and set the path to share.
            String attachPath = "";
            data.putExtra("ATTACHMENT", attachPath);
            ArrayList<String> screenPathList = new ArrayList<String>();
            // todo : add screen path list of theme
            data.putStringArrayListExtra("SCREENS", screenPathList);
            setResult(RESULT_OK, data);

            backToScene();
        } else if (KEY_SCENE_EDITOR.equalsIgnoreCase(preference.getKey())) {
            backToScene();
            showSceneEditDialog(SESceneManager.getInstance());
        }
        return false;
    }

    public static void showSceneEditDialog(final SESceneManager sceneManager) {
        final SEScene scene = sceneManager.getCurrentScene();
        final ArrayList<SECameraData> dataList = scene.mSceneInfo.getAllCameraData();
        final int width = scene.getCamera().getWidth();
        final int height = scene.getCamera().getHeight();
        List<String> itemList = new ArrayList<String>(dataList.size());
        for (SECameraData item : dataList) {
            item.mWidth = width;
            item.mHeight = height;
            itemList.add(item.mType);
        }

        showSceneEditDialog(sceneManager, itemList, new EditSceneDialog.OnItemClickListener() {
            @Override
            public void onItemClick(int index) {
                scene.getCamera().getCurrentData().set(dataList.get(index));
                scene.mSECamera.setCamera();
            }
        }, mCloseSceneEditor);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (BACKDOOR_ENABLE.equals(preference.getKey())) {
            boolean state = (Boolean) objValue;
            setBackdoorEnabled(this, state);
        } else if (OPTION_MENU_ALT.equalsIgnoreCase(preference.getKey())) {
            boolean state = (Boolean) objValue;
            setOptionMenuAlt(this, state);
        } else if (KEY_DEBUG_TEST_SERVER.equalsIgnoreCase(preference.getKey())) {
            setDebugTestServer(this, (Boolean)objValue);
        } else if (KEY_DEBUG_SUGGESTION.equalsIgnoreCase(preference.getKey())) {
            setDebugSuggestion(this, (Boolean)objValue);
        } else if (KEY_DEBUG_BETA_DATA.equalsIgnoreCase(preference.getKey())) {
            setDebugBetaRequest(this, (Boolean)objValue);
        } else if (KEY_MARKET_DEBUG_LOG_DATA.equalsIgnoreCase(preference.getKey())) {
            setMarketLogEnable(this, (Boolean)objValue);
        }
        return true;
    }

    public static boolean isOptionMenuAlt(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getBoolean(OPTION_MENU_ALT, false);
    }

    private static void setOptionMenuAlt(Context context, boolean state) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(OPTION_MENU_ALT, state);
        editor.commit();
    }

    public static boolean isDebugTestServer(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getBoolean(KEY_DEBUG_TEST_SERVER, false);
    }

    private static void setDebugTestServer(Context context, boolean state) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(KEY_DEBUG_TEST_SERVER, state);
        editor.commit();
        MarketConfiguration.setIS_DEBUG_TESTSERVER(state);
    }

    public static boolean isDebugSuggestion(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getBoolean(KEY_DEBUG_SUGGESTION, false);
    }

    private static void setDebugSuggestion(Context context, boolean state) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(KEY_DEBUG_SUGGESTION, state);
        editor.commit();
        MarketConfiguration.setIS_DEBUG_SUGGESTION(state);
    }

    public static boolean isDebugBetaRequest(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getBoolean(KEY_DEBUG_BETA_DATA, false);
    }

    private static void setDebugBetaRequest(Context context, boolean state) {
        SharedPreferences settings = context.getSharedPreferences(KEY_DEBUG_BETA_DATA, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(KEY_DEBUG_BETA_DATA, state);
        editor.commit();
        MarketConfiguration.setIS_DEBUG_BETA_REQUEST(state);
    }

    public static boolean isMarketLogEnable(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getBoolean(KEY_MARKET_DEBUG_LOG_DATA, false);
    }

    private static void setMarketLogEnable(Context context, boolean state) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(KEY_MARKET_DEBUG_LOG_DATA, state);
        editor.commit();
        MarketUtils.setLogVisibility(state);
    }

    public static boolean isEnableBackdoor(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        return settings.getBoolean(BACKDOOR_ENABLE, false);
    }

    private static void setBackdoorEnabled(Context context, boolean state) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SETTING_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(BACKDOOR_ENABLE, state);
        editor.commit();
    }

    private static void enableBackDoorSetting(Context context) {
        final PackageManager pm = context.getPackageManager();
        final ComponentName compName = new ComponentName(context, BackDoorSettingsActivity.class);
        pm.setComponentEnabledSetting(compName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }
    public static void installBackDoor(Context context) {
        if (null == handler) {
            enableBackDoorSetting(context);
            mBackDoorContext = context.getApplicationContext();
            handler = new Handler(context.getMainLooper());
            resetKnockingCount();
        }
    }

    // Continue knocking more than THRESHOLD time to open the back door setting, and
    // reset the knock after waiting or INTERVAL.
    private static final int BACK_DOOR_THRESHOLD = 7;
    private static final int WAITING_INTERVAL = 350; // 0.35s
    private static int mKnockCount = 0;

    private static void resetKnockingCount() {
        mKnockCount = 0;
    }

    private static Context mBackDoorContext;
    private static Handler handler;
    private  static Runnable runnable = new Runnable(){
        @Override
        public void run() {
            if (HomeUtils.DEBUG) {
                Log.d(HomeUtils.TAG, "Enn, it might be a cat, so ignore it ......." + mKnockCount);
            }
            resetKnockingCount();
        }
    };

    public static boolean knockAtBackDoor(Context context) {
        if (HomeUtils.DEBUG) {
            Log.d(HomeUtils.TAG, "Hei, someone is knocking at the back door......." + mKnockCount);
        }
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, WAITING_INTERVAL);

        if (mKnockCount++ > BACK_DOOR_THRESHOLD) {
            resetKnockingCount();
            setBackdoorEnabled(context, true);
            openBackDoor();
            return true;
        } else if (BackDoorSettingsActivity.isEnableBackdoor(context)) {
            openBackDoor();
            return true;
        } else {
            return false;
        }
    }

    private static void openBackDoor() {
        if (HomeUtils.DEBUG) {
            Log.d(HomeUtils.TAG, "Hello, the back door is opening..." + mKnockCount);
        }
        try {
            Intent intent = new Intent(mBackDoorContext, BackDoorSettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mBackDoorContext.startActivity(intent);
            SettingsActivity.saveBackdoorState(mBackDoorContext, true);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(HomeUtils.TAG, "openBackDoor exception.");
        }
    }

    private static Dialog mSceneEditor = null;
    private static View.OnClickListener mCloseSceneEditor = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (null != mSceneEditor) {
                try {
                    mSceneEditor.dismiss();
                    mSceneEditor = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };
    public static void showSceneEditDialog(final SESceneManager sceneManager, final List dataList,
                                           final EditSceneDialog.OnItemClickListener listener,
                                           final View.OnClickListener closeListener) {
        sceneManager.runInUIThread(new Runnable() {
            public void run() {
                if(mSceneEditor == null) {
                    mSceneEditor = new EditSceneDialog(sceneManager.getGLActivity(),
                            dataList, listener,
                            closeListener);
                }
                mSceneEditor.show();
            }
        });
    }

    public static void showOptionMenu(final Context context, boolean show) {
        final SESceneManager sceneManager = SESceneManager.getInstance();
        final View.OnClickListener closeSceneEditor = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mSceneEditor) {
                    try {
                        mSceneEditor.dismiss();
                        mSceneEditor = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sceneManager.getCurrentScene().setStatus(SEScene.STATUS_OPTION_MENU, false);
                }
            }
        };

        if (show && (null == mSceneEditor || !mSceneEditor.isShowing())) {
            final String[] extraLabels = context.getResources().getStringArray(R.array.backdoor_menus);
            final String[] labels = context.getResources().getStringArray(R.array.menus);
            ArrayList<String> itemList = new ArrayList<String>();
            itemList.addAll(Arrays.asList(extraLabels));
            itemList.addAll(Arrays.asList(labels));
            showSceneEditDialog(sceneManager, itemList, new EditSceneDialog.OnItemClickListener() {
                @Override
                public void onItemClick(int index) {
                    if (index >= extraLabels.length) {
                        if (OptionMenu.onMenuItemClicked(index - extraLabels.length)) {
                            closeSceneEditor.onClick(null);
                        }
                    } else {
                        if (onExtraMenuItemClick(context, index)) {
                            closeSceneEditor.onClick(null);
                        }
                    }
                }
            }, closeSceneEditor);
        } else if (!show && null != mSceneEditor && mSceneEditor.isShowing()) {
            closeSceneEditor.onClick(null);
        }
    }

    private static boolean onExtraMenuItemClick(Context context, int index) {
        switch (index) {
            case 0:
                HomeUtils.exportOrImportWallpaper(context);
                break;
            case 1:
                HomeUtils.tryImportWallpaper(context);
            default:
                break;
        }
        return false;
    }

    private static class EditSceneDialog extends Dialog {
        public static interface OnItemClickListener {
            public void onItemClick(int index);
        }

        private android.view.View.OnClickListener mCloseClickListener;

        private List mDataList;
        private OnItemClickListener mItemClickListener;

        private View mRootView;
        private Context mContext;
        public EditSceneDialog(Context context,
                               List dataList, OnItemClickListener itemClickListener,
                               android.view.View.OnClickListener listener) {
            super(context, R.style.dialog);
            mContext = context;
            mDataList = dataList;
            mItemClickListener = itemClickListener;
            mCloseClickListener = listener;
        }

        @Override
        public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                SESceneManager.getInstance().handleMenuKey();
            }
            return super.onKeyDown(keyCode, event);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            setContentView(R.layout.scene);
            mRootView = findViewById(R.id.rootView);

            ViewGroup container = (ViewGroup)((ViewGroup)mRootView).getChildAt(0);
            LayoutInflater inflater = getLayoutInflater();
            TextView textView;
            int index = 0;
            for (final Object data : mDataList) {
                textView = (TextView)inflater.inflate(R.layout.edit_scene_item, container, false);
                textView.setText((String)data);
                container.addView(textView);
                final int i = index++;
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (null != mItemClickListener) {
                            mItemClickListener.onItemClick(i);
                            if (null != mCloseSceneEditor) {
                                mCloseSceneEditor.onClick(null);
                            }
                        }
                    }
                });
            }

            mRootView.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                        cancel();
//                        mCloseClickListener.onClick(v);
//                        return true;
                    }
                    return false;
                }
            });
        }

        @Override
        protected void onStart() {
            super.onStart();
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            super.onWindowFocusChanged(hasFocus);
            DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
            Log.d("EditSceneDialog: ", "dm.density: " + dm.density) ;
            int screenWidth  = dm.widthPixels;
            int screenHeight = dm.heightPixels;
            mRootView.setLayoutParams(new FrameLayout.LayoutParams(screenWidth, screenHeight));
        }

        @Override
        protected void onStop() {
            super.onStop();
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            super.onCreateOptionsMenu(menu);
            return true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                cancel();
                mCloseClickListener.onClick(mRootView);
                return true;
            }
            return false;
        }

        @Override
        public void onBackPressed() {
            cancel();
            mCloseClickListener.onClick(mRootView);
        }
    }

    private void backToScene() {
        IntentUtil.sendBroadBackToScene(getApplicationContext());
        finish();
    }
}
