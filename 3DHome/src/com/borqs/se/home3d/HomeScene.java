package com.borqs.se.home3d;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;

import com.borqs.borqsweather.weather.IWeatherService;
import com.borqs.market.utils.MarketUtils;
import com.borqs.market.wallpaper.RawPaperItem;
import com.borqs.se.R;
import com.borqs.se.addobject.AddAppDialog;
import com.borqs.se.addobject.AddAppDialog.OnAppSelectedListener;
import com.borqs.se.addobject.AddFolderDialog;
import com.borqs.se.addobject.AddFolderDialog.OnFolderCreatedListener;
import com.borqs.se.addobject.AddObjectItemInfo;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SECameraData;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SEEmptyAnimation;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.engine.SEVector.SEVector2i;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.home3d.HomeManager.ModelChangeCallBack;
import com.borqs.se.home3d.ObjectsMenu.OBJECTSHOWTYPE;
import com.borqs.se.home3d.ProviderUtils.ObjectInfoColumns;
import com.borqs.se.home3d.ProviderUtils.ThemeColumns;
import com.borqs.se.home3d.ProviderUtils.VesselColumns;
import com.borqs.se.shortcut.AppItemInfo;
import com.borqs.se.shortcut.ItemInfo;
import com.borqs.se.shortcut.LauncherModel;
import com.borqs.se.shortcut.LauncherModel.LanguageChangeCallBack;
import com.borqs.se.shortcut.LauncherModel.LoadAppFinishedListener;
import com.borqs.se.widget3d.ADViewController;
import com.borqs.se.widget3d.AppObject;
import com.borqs.se.widget3d.Cloud;
import com.borqs.se.widget3d.Desk;
import com.borqs.se.widget3d.DragLayer;
import com.borqs.se.widget3d.Folder;
import com.borqs.se.widget3d.House;
import com.borqs.se.widget3d.NormalObject;
import com.borqs.se.widget3d.ObjectInfo;
import com.borqs.se.widget3d.VesselLayer;
import com.borqs.se.widget3d.VesselObject;
import com.borqs.se.widget3d.WallCellLayer;
import com.borqs.se.widget3d.WallLayer;

import java.util.ArrayList;
import java.util.List;

public class HomeScene extends SEScene implements ModelChangeCallBack, LanguageChangeCallBack, OnAppSelectedListener,
                                     OnFolderCreatedListener  {

    public static final String TAG = "HomeScene";

    public static final int STATUS_DISALLOW_TOUCH = 0x00000001;
    public static final int STATUS_APP_MENU = STATUS_DISALLOW_TOUCH << 1;
    public static final int STATUS_OBJ_MENU = STATUS_APP_MENU << 1;
    public static final int STATUS_HELPER_MENU = STATUS_OBJ_MENU << 1;
    public static final int STATUS_MOVE_OBJECT = STATUS_HELPER_MENU << 1;
    public static final int STATUS_ON_DESK_SIGHT = STATUS_MOVE_OBJECT << 1;
    public static final int STATUS_ON_SKY_SIGHT = STATUS_ON_DESK_SIGHT << 1;
    public static final int STATUS_ON_WIDGET_SIGHT = STATUS_ON_SKY_SIGHT << 1;
    public static final int STATUS_ON_SCALL = STATUS_ON_WIDGET_SIGHT << 1;
    public static final int STATUS_ON_WIDGET_TOUCH = STATUS_ON_SCALL << 1;
    public static final int STATUS_ON_WIDGET_RESIZE = STATUS_ON_WIDGET_TOUCH << 1;

    public static final int REQUEST_CODE_SELECT_WIDGET = 0;
    public static final int REQUEST_CODE_SELECT_SHORTCUT = REQUEST_CODE_SELECT_WIDGET + 1;
    public static final int REQUEST_CODE_BIND_WIDGET = REQUEST_CODE_SELECT_SHORTCUT + 1;
    public static final int REQUEST_CODE_SELECT_WALLPAPER_IMAGE = REQUEST_CODE_BIND_WIDGET + 1;
    public static final int REQUEST_CODE_SELECT_WALLPAPER_CAMERA = REQUEST_CODE_SELECT_WALLPAPER_IMAGE + 1;
    public static final int REQUEST_CODE_BIND_SHORTCUT = REQUEST_CODE_SELECT_WALLPAPER_CAMERA + 1;

    public static final int MSG_TYPE_SHOW_BIND_APP_DIALOG = 0;
    public static final int MSG_TYPE_SHOW_OBJECT_VIEW = MSG_TYPE_SHOW_BIND_APP_DIALOG + 1;
    private static final int MSG_TYPE_SHOW_APP_VIEW = MSG_TYPE_SHOW_OBJECT_VIEW + 1;
    public static final int MSG_TYPE_SHOW_DELETE_OBJECTS = MSG_TYPE_SHOW_APP_VIEW + 1;
    public static final int MSG_TYPE_SHOW_SELECT_WALLPAPER_DIALOG = MSG_TYPE_SHOW_DELETE_OBJECTS + 1;
    public static final int MSG_TYPE_SHOW_GROUND_WALLPAPER_DIALOG = MSG_TYPE_SHOW_SELECT_WALLPAPER_DIALOG + 1;
    public static final int MSG_TYPE_SHOW_WALL_LONG_CLICK_DIALOG = MSG_TYPE_SHOW_GROUND_WALLPAPER_DIALOG + 1;
    public static final int MSG_TYPE_SHOW_OBJECT_LONG_CLICK_DIALOG = MSG_TYPE_SHOW_WALL_LONG_CLICK_DIALOG + 1;
    public static final int MSG_TYPE_SHOW_OPTION_MENU_DIALOG = MSG_TYPE_SHOW_OBJECT_LONG_CLICK_DIALOG + 1;
    public static final int MSG_TYPE_SHOW_CAMERA_ADJUST_DIALOG = MSG_TYPE_SHOW_OPTION_MENU_DIALOG + 1;
    public static final int MSG_TYPE_SHOW_WEATHER_DIALOG = MSG_TYPE_SHOW_CAMERA_ADJUST_DIALOG + 1;
    public static final int MSG_TYPE_DISMIS_WEATHER_DIALOG = MSG_TYPE_SHOW_WEATHER_DIALOG + 1;
    public static final int MSG_TYPE_SHOW_EDITFOLDER_DIALOG = MSG_TYPE_DISMIS_WEATHER_DIALOG + 1;
    public static final int MSG_TYPE_SHOW_CREATEFOLDER_DIALOG = MSG_TYPE_SHOW_EDITFOLDER_DIALOG + 1;
    public static final int MSG_TYPE_SHOW_ADDAPP_DIALOG = MSG_TYPE_SHOW_CREATEFOLDER_DIALOG + 1;
    public static final int MSG_TYPE_SHOW_CHANGE_COLOR_DIALOG = MSG_TYPE_SHOW_ADDAPP_DIALOG + 1;

    public static final int MSG_TYPE_UPDATE_SCENE = 1000;
    public static final int MSG_TYPE_UPDATE_MODEL = MSG_TYPE_UPDATE_SCENE + 1;
    private static final int MSG_TYPE_UPDATE_WALLPAPER = MSG_TYPE_UPDATE_MODEL + 1;

    private static final int DIALOG_DELETE_OBJECTS = 1;
    private static final int DIALOG_SELECT_WALLPAPER = DIALOG_DELETE_OBJECTS + 1;
    private static final int DIALOG_WALL_LONG_CLICK = DIALOG_SELECT_WALLPAPER + 1;
    private static final int DIALOG_OBJECT_LONG_CLICK = DIALOG_WALL_LONG_CLICK + 1;
    public static final int DIALOG_OBJECT_CHANGE_LABLE = DIALOG_OBJECT_LONG_CLICK + 1;
    public static final int DIALOG_OBJECT_CHANGE_ICON = DIALOG_OBJECT_CHANGE_LABLE + 1;
    private static final int DIALOG_HELPER = DIALOG_OBJECT_CHANGE_ICON + 1;
    public static final int DIALOG_ADJUST_CAMERA = DIALOG_HELPER + 1;
    private static final int DIALOG_WEATHER = DIALOG_ADJUST_CAMERA + 1;
    private static final int DIALOG_WALL_LONG_CLICK_ADD_APP = DIALOG_WEATHER + 1;
    private static final int DIALOG_WALL_LONG_CLICK_ADD_FOLDER = DIALOG_WALL_LONG_CLICK_ADD_APP + 1;
    private static final int DIALOG_EDITFOLDER = DIALOG_WALL_LONG_CLICK_ADD_FOLDER + 1;
    private static final int DIALOG_CHANGE_COLOR = DIALOG_EDITFOLDER + 1;
    private static final int DIALOG_WALL_INDICATOR = DIALOG_CHANGE_COLOR + 1;

    private Configuration mPreviousConfig;
    private ObjectsMenu mObjectsMenu;
    private ApplicationMenu mWidgetsPreview;
    private NormalObject mSetBindObject;
    private int mMenuCheckStatus;
    private boolean mLoadCompleted;
    private String mDeleteObjName;
    private NormalObject mSelectedObject;
    private OptionMenu mOptionMenu;
    private Folder mEditFolder;

    public static final String MSG_CONTENT_IMAGE = "image";
    public static final String MSG_CONTENT_IMAGE_SIZE_X = "imgSizeX";
    public static final String MSG_CONTENT_IMAGE_SIZE_Y = "imgSizeY";
    public static final String MSG_CONTENT_IMAGE_OUTPUT = "output";
    private DragLayer mDraglayer;
    private HomeSceneInfo mSceneInfo;
    private boolean mShowWallIndicator;
    public int mStatus = 0;

    public HomeScene(Context context, String sceneName) {
        super(context, sceneName);
        mMenuCheckStatus = STATUS_APP_MENU + STATUS_HELPER_MENU + STATUS_OBJ_MENU + STATUS_ON_WIDGET_SIGHT
                + STATUS_MOVE_OBJECT + STATUS_DISALLOW_TOUCH + STATUS_ON_WIDGET_RESIZE;
        mShowWallIndicator = SettingsActivity.isEnableWallIndicator(getContext());
        mDraglayer = new DragLayer(this);
    }

    @Override
    public void onSceneStart() {
        mLoadCompleted = false;
        setStatus(STATUS_DISALLOW_TOUCH, true);
        mPreviousConfig = new Configuration();
        mPreviousConfig.setTo(getContext().getResources().getConfiguration());

        ADViewController adController = ADViewController.getInstance();
        adController.setHomeScene(this);
        getCamera().addCameraChangedListener(adController);
        LauncherModel.getInstance().setLoadAppFinishedListener(new LoadAppFinishedListener() {
            public void onFinished() {
                new SECommand(HomeScene.this) {
                    public void run() {
                        loading();
                    }
                }.execute();

            }
        });
    }

    public void setStatus(int type, boolean status) {
        if (status) {
            mStatus |= type;
        } else {
            mStatus &= ~type;
        }
    }

    public boolean getStatus(int type) {
        return (mStatus & type) != 0;
    }

    public int getStatus() {
        return mStatus;
    }

    public DragLayer getDragLayer() {
        return mDraglayer;
    }

    public void setHomeSceneInfo(HomeSceneInfo sceneInfo) {
        mSceneInfo = sceneInfo;
    }

    public HomeSceneInfo getHomeSceneInfo() {
        return mSceneInfo;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (getStatus(STATUS_DISALLOW_TOUCH)) {
            return true;
        }
        if (!super.dispatchTouchEvent(event)) {
            return mDraglayer.dispatchTouchEvent(event);
        }
        return true;
    }

    @Override
    public boolean handleBackKey(SEAnimFinishListener l) {
        if (getStatus(STATUS_DISALLOW_TOUCH)) {
            return false;
        }
        return super.handleBackKey(l);
    }

    @Override
    public void notifySurfaceChanged(int width, int height) {
        mSceneInfo.notifySurfaceChanged(width, height);
        super.notifySurfaceChanged(width, height);
    }

    @Override
    public void onRelease() {
        super.onRelease();
        LauncherModel.getInstance().removeLanguageChangeCallBack(this);
    }

    private void loading() {
        /**
         * 纠错处理， 有可能当前主题的房间和主题不对应
         */
        ThemeInfo themeInfo = HomeManager.getInstance().getCurrentThemeInfo();
        HomeUtils.updateHouseName(getContext(), themeInfo.mSceneName, themeInfo.mHouseName);
        ObjectInfo rootVesselInfo = findRootVessel();
        if (rootVesselInfo == null) {
            return;
        }
        final NormalObject root = rootVesselInfo.CreateNormalObject(this);
        setContentObject(root);// set root object of scene
        mObjectsMenu = new ObjectsMenu(HomeScene.this, "ObjectsMenu");
        setObjectsMenu(mObjectsMenu);// add objects menu
        HomeManager.getInstance().getModelManager().loadPreLoadModel(this, new Runnable() {
            @Override
            public void run() {
                root.loadAll(null, new Runnable() {
                    public void run() {
                        HomeManager.getInstance().getModelManager().loadAfterLoadModel(HomeScene.this, new Runnable() {
                            public void run() {
                                setStatus(STATUS_DISALLOW_TOUCH, false);
                                createAndLoadMenu();
                                LauncherModel.getInstance().addLanguageChangeCallBack(HomeScene.this);
                                mLoadCompleted = true;
                            }
                        });
                    }
                });
            }
        });
    }

    private ObjectInfo findRootVessel() {
        String where = ObjectInfoColumns.SCENE_NAME + "='" + getSceneName() + "' AND " + VesselColumns.VESSEL_ID
                + "=-1";
        ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = resolver.query(ObjectInfoColumns.OBJECT_LEFT_JOIN_ALL, null, where, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                ObjectInfo objectInfo = ObjectInfo.CreateFromDB(cursor);
                cursor.close();
                return objectInfo;
            }
            cursor.close();
        }
        return null;
    }

    private void createAndLoadMenu() {
        initApplicationMenu();
        initOptionMenu();
        if (!SettingsActivity.getHelpStatus(getContext())) {
            showDialog(DIALOG_HELPER);
            SettingsActivity.setHelpStatus(getContext(), true);
        }
    }

    @Override
    public void handleMenuKey() {
        if (mOptionMenu != null) {
            if ((mMenuCheckStatus & getStatus()) == 0) {
                handleMessage(HomeScene.MSG_TYPE_SHOW_OPTION_MENU_DIALOG, null);
            }
        }
    }

    @Override
    public void handleMessage(int type, Object message) {
        switch (type) {
        case MSG_TYPE_SHOW_OBJECT_VIEW:
            final OBJECTSHOWTYPE msg = (OBJECTSHOWTYPE) message;
            new SECommand(this) {
                @Override
                public void run() {
                    if (mObjectsMenu != null) {
                        mObjectsMenu.show(msg);
                    }
                }
            }.execute();
            break;
        case MSG_TYPE_SHOW_APP_VIEW:
            final int showType = (Integer) message;
            new SECommand(this) {
                @Override
                public void run() {
                    if (mWidgetsPreview != null) {
                        mWidgetsPreview.show(showType, null);
                    }
                }
            }.execute();
            break;
        case MSG_TYPE_SHOW_BIND_APP_DIALOG:
            if (message instanceof NormalObject) {
                mSetBindObject = (NormalObject) message;
                new SECommand(this) {
                    @Override
                    public void run() {
                        if (mWidgetsPreview != null) {
                            mWidgetsPreview.show(ApplicationMenu.TYPE_BIND_APP, mSetBindObject);
                        }
                    }
                }.execute();
            }
            break;
        case MSG_TYPE_SHOW_DELETE_OBJECTS:
            mDeleteObjName = (String) message;
            showDialog(DIALOG_DELETE_OBJECTS);
            break;
        case MSG_TYPE_SHOW_SELECT_WALLPAPER_DIALOG:
            showDialog(DIALOG_SELECT_WALLPAPER);
            break;
        case MSG_TYPE_SHOW_WALL_LONG_CLICK_DIALOG:
            // mWallPaperMsg = (Bundle)message;
            showDialog(DIALOG_WALL_LONG_CLICK);
            break;

        case MSG_TYPE_SHOW_OBJECT_LONG_CLICK_DIALOG:
            if (message instanceof NormalObject) {
                mSelectedObject = (NormalObject) message;
                new SECommand(HomeScene.this) {
                    public void run() {
                        if (mSelectedObject != null) {
                            Bundle bundle = setObjectLongClickDialogContent();
                            showDialog(DIALOG_OBJECT_LONG_CLICK, bundle);
                        }
                    }
                }.execute();
            }
            break;
        case MSG_TYPE_SHOW_OPTION_MENU_DIALOG:
            showOptionMenu();
            break;
        case MSG_TYPE_UPDATE_SCENE:
            onSceneChanged();
            break;
        case MSG_TYPE_SHOW_CAMERA_ADJUST_DIALOG:
            moveToWallSight(new SEAnimFinishListener() {
                @Override
                public void onAnimationfinish() {
                    showDialog(DIALOG_ADJUST_CAMERA);
                }
            });
            break;
        case MSG_TYPE_SHOW_WEATHER_DIALOG:
            showWeatherDialog(message);
            break;
        case MSG_TYPE_DISMIS_WEATHER_DIALOG:
            dismissDialog(DIALOG_WEATHER);
            break;
        case MSG_TYPE_UPDATE_WALLPAPER:
            onWallpaperChanged();
            break;
        case MSG_TYPE_SHOW_ADDAPP_DIALOG:
            removeDialog(DIALOG_WALL_LONG_CLICK_ADD_APP);
            showDialog(DIALOG_WALL_LONG_CLICK_ADD_APP);
            break;
        case MSG_TYPE_SHOW_CREATEFOLDER_DIALOG:
            removeDialog(DIALOG_WALL_LONG_CLICK_ADD_FOLDER);
            showDialog(DIALOG_WALL_LONG_CLICK_ADD_FOLDER);
            break;
        case MSG_TYPE_SHOW_EDITFOLDER_DIALOG:
            removeDialog(DIALOG_EDITFOLDER);
            mEditFolder = (Folder)message;
            showDialog(DIALOG_EDITFOLDER);
            break;
        case MSG_TYPE_SHOW_CHANGE_COLOR_DIALOG:
            showDialog(DIALOG_CHANGE_COLOR);
            break;
        }
    }

    private void showOptionMenu() {
        SESceneManager.getInstance().runInUIThread(new Runnable() {
            public void run() {
                mOptionMenu.showAtLocation(HomeManager.getInstance().getWorkSpace(), Gravity.BOTTOM, 0, 0);
                mOptionMenu.playAnimation();
            }
        });

    }

    private void initApplicationMenu() {
        mWidgetsPreview = new ApplicationMenu(HomeScene.this, "WidgetsPreview");
        getContentObject().addChild(mWidgetsPreview, true);
    }

    private void initOptionMenu() {
        if (mOptionMenu == null) {
            mOptionMenu = new OptionMenu(getContext());
        }
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        mShowWallIndicator = SettingsActivity.isEnableWallIndicator(getContext());
        if (!mShowWallIndicator) {
            removeDialog(DIALOG_WALL_INDICATOR);
        }
        new SECommand(this) {
            public void run() {
                if (mLoadCompleted) {
                    setStatus(STATUS_DISALLOW_TOUCH, false);
                }
                if (!SettingsActivity.getHelpStatus(getContext())) {
                    showDialog(DIALOG_HELPER);
                    SettingsActivity.setHelpStatus(getContext(), true);
                }
            }
        }.execute();

    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!handleBackKey(null)) {
            if (getContentObject() != null) {
                getContentObject().onPressHomeKey();
            }
            if (getCamera() != null) {
                moveToWallSight(new SEAnimFinishListener() {

                    public void onAnimationfinish() {
                        String where = ThemeColumns._ID + "=" + getHomeSceneInfo().getThemeInfo().mID;
                        Cursor cursor = getContext().getContentResolver().query(ThemeColumns.CAMERA_INFO_URI,
                                new String[] { ThemeColumns.LOCATION }, where, null, null);
                        if (cursor != null) {
                            if (cursor.moveToFirst()) {
                                String dbCameraLoc = cursor.getString(cursor
                                        .getColumnIndexOrThrow(ThemeColumns.LOCATION));
                                SEVector3f loc = new SEVector3f(ProviderUtils.getFloatArray(dbCameraLoc, 3));
                                if (loc == getCamera().getLocation()) {
                                    return;
                                }
                                float endRadius = -loc.getY();
                                float endFov = getHomeSceneInfo().getCameraFovByRadius(endRadius);
                                playSetRadiusAndFovAnim(endRadius, endFov, null);
                            }
                            cursor.close();
                        }

                    }
                });
            }
        }
        dismissAllDialog();
    }

    private boolean mHadInitedDialog = false;

    @Override
    public Dialog onCreateDialog(int id, Bundle bundle) {
        switch (id) {
        case DIALOG_DELETE_OBJECTS:
            return createDeleteDialog();
        case DIALOG_SELECT_WALLPAPER:
            if (mHouse != null) {
                return mHouse.createWallPaperDialog();
            } 
            return null;
        case DIALOG_WALL_LONG_CLICK:
            return createWallDialog();
        case DIALOG_OBJECT_LONG_CLICK:
            return createObjectDialog();
        case DIALOG_OBJECT_CHANGE_LABLE:
            return createChangeLableDialog();
        case DIALOG_OBJECT_CHANGE_ICON:
            return createChangeIconDialog();
        case DIALOG_HELPER:
            return createHelperDialog();
        case DIALOG_ADJUST_CAMERA:
            return createAdjustCameraDialog();
        case DIALOG_WEATHER:
            return createWeatherDialog();
        case DIALOG_WALL_LONG_CLICK_ADD_APP:
            return createWallDialogAddApp();
        case DIALOG_WALL_LONG_CLICK_ADD_FOLDER:
            return createWallDialogAddFolder();
        case DIALOG_EDITFOLDER:
            if (mEditFolder != null) {
                return createEditFolderDialog(mEditFolder);
            }
            return null;
        case DIALOG_CHANGE_COLOR:
            return createChangeColorDialog();
        case DIALOG_WALL_INDICATOR:
            return createWallIndicatorDialog();
        }
        return null;
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog, Bundle bundle) {
        if (mHadInitedDialog) {
            mHadInitedDialog = false;
            return;
        }
        switch (id) {
        case DIALOG_DELETE_OBJECTS:
            AlertDialog deleteDialog = (AlertDialog) dialog;
            String message = getContext().getResources().getString(R.string.confirm_delete_selected_objects,
                    mDeleteObjName);
            deleteDialog.setMessage(message);
            break;
        case DIALOG_OBJECT_LONG_CLICK:
            prepareObjectDialog(dialog, bundle);
            break;
        case DIALOG_OBJECT_CHANGE_LABLE:
            prepareChangeLabelDialog(dialog);
            break;
        case DIALOG_OBJECT_CHANGE_ICON:
            prepareChangeIconDialog(dialog);
            break;
        case DIALOG_CHANGE_COLOR:
            prepareChangeColorDialog(dialog);
            break;
        }
    }

    private Dialog createDeleteDialog() {
        Activity activity = SESceneManager.getInstance().getGLActivity();
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = mDeleteObjName;
                String productId = HomeUtils.deleteModelAndObjectDBByName(getContext(), name);
                if (productId != null) {
                    MarketUtils.deletePlugIn(getContext(), productId);
                }
                ModelInfo modelInfo = HomeManager.getInstance().getModelManager().mModels.remove(name);
                if (modelInfo != null) {
                    onRemoveModelFromDB(modelInfo);
                }
            }
        };
        AlertDialog dialog = new AlertDialog.Builder(activity).setIcon(R.drawable.art_dialog_notice)
                .setPositiveButton(android.R.string.yes, listener).setNegativeButton(android.R.string.no, null)
                .create();
        String message = activity.getResources().getString(R.string.confirm_delete_selected_objects, mDeleteObjName);
        String title = activity.getResources().getString(R.string.delete_objects_title);
        dialog.setTitle(title);
        dialog.setMessage(message);
        return dialog;
    }

    /**/
    private Dialog createWallDialog() {
        return new LongPressWallDialog(SESceneManager.getInstance().getGLActivity());
    }

    private Dialog createWallDialogAddApp() {
        if (getEmptySlotNumOfCurrentWall() <= 0) {
            Toast.makeText(getContext(), R.string.addobject_select_to_add_app_no_room, Toast.LENGTH_SHORT).show();
            return null;
        }
        return (new AddAppDialog(SESceneManager.getInstance().getGLActivity(), 
                SESceneManager.getInstance().getGLActivity().getLayoutInflater(),
                HomeUtils.getSortedAppsByName(), this, getEmptySlotNumOfCurrentWall()));
    }

    private Dialog createWallDialogAddFolder() {
        if (getEmptySlotNumOfCurrentWall() <= 0) {
            Toast.makeText(getContext(), R.string.addobject_select_to_add_app_no_room, Toast.LENGTH_SHORT).show();
            return null;
        }
        return (new AddFolderDialog(SESceneManager.getInstance().getGLActivity(), 
                             SESceneManager.getInstance().getGLActivity().getLayoutInflater(),
                             HomeUtils.getSortedAppsByName(), this, getEmptySlotNumOfCurrentWall(),
                             getChildMaxNumInFolder()));
    }

    private Dialog createEditFolderDialog(Folder folder) {
        if (folder == null) { return null; }
        return (new AddFolderDialog(SESceneManager.getInstance().getGLActivity(), 
                             SESceneManager.getInstance().getGLActivity().getLayoutInflater(),
                             HomeUtils.getSortedAppsByName(), folder, getEmptySlotNumOfCurrentWall(),
                             getChildMaxNumInFolder(), folder.getEditBundle()));
    }

    private Dialog createObjectDialog() {
        return new LongClickObjectDialog(SESceneManager.getInstance().getGLActivity());
    }

    private Dialog createChangeLableDialog() {
        return new ChangeLabelDialog(SESceneManager.getInstance().getGLActivity());
    }

    private Dialog createChangeIconDialog() {
        return new ChangeIconDialog(SESceneManager.getInstance().getGLActivity());
    }

    private Dialog createHelperDialog() {
        return new HelperDialog(SESceneManager.getInstance().getGLActivity());
    }

    private Dialog createAdjustCameraDialog() {
        return new CameraAdjustDialog(SESceneManager.getInstance().getGLActivity());
    }

    private Dialog createChangeColorDialog() {
        return new ChangeColorDialog(SESceneManager.getInstance().getGLActivity());
    }

    private WallIndicatorDialog mWallIndicatorDialog;

    private Dialog createWallIndicatorDialog() {
        mWallIndicatorDialog = new WallIndicatorDialog(SESceneManager.getInstance().getGLActivity());
        return mWallIndicatorDialog;
    }

    public void updateWallIndicater(float wallIndex, int wallNum) {
        if (mShowWallIndicator) {
            if (mWallIndicatorDialog != null) {
                mWallIndicatorDialog.updateWallIndex(wallIndex, wallNum);
            }
            if (wallIndex % 1 != 0) {
                showDialog(DIALOG_WALL_INDICATOR);
            } else {
                dismissDialog(DIALOG_WALL_INDICATOR);
            }
        }
    }

    private void prepareObjectDialog(Dialog dialog, Bundle bundle) {
        if (mSelectedObject != null) {
            LongClickObjectDialog d = (LongClickObjectDialog) dialog;
            d.prepareContent(mSelectedObject, bundle);
        }
    }

    private void prepareChangeIconDialog(Dialog dialog) {
        final NormalObject selectObject = mSelectedObject;
        if (selectObject != null) {
            ChangeIconDialog changeIconDialog = (ChangeIconDialog) dialog;
            if (selectObject.getObjectInfo().mDisplayName != null) {
                changeIconDialog.setCustomName(selectObject.getObjectInfo().mDisplayName);
            } else {
                changeIconDialog.setCustomName(HomeUtils.getAppLabel(getContext(), selectObject.getObjectInfo()
                        .getResolveInfo()));
            }
            Bitmap icon = selectObject.getObjectInfo().mShortcutIcon;
            if (icon == null) {
                Drawable drawable = HomeUtils.getAppIcon(getContext(), selectObject.getObjectInfo().getResolveInfo());
                changeIconDialog.setAppIcon(drawable);
            } else {
                changeIconDialog.setAppIcon(icon);
            }

            changeIconDialog.setOnDialogFinished(new ChangeIconDialog.OnDialogFinished() {
                @Override
                public void onFinish(Bitmap icon, boolean changed) {
                    if (changed) {
                        selectObject.updateIcon(icon);
                    } else {
                        selectObject.resetIcon();
                    }
                }
            });
        }
    }

    private void prepareChangeLabelDialog(Dialog dialog) {
        final NormalObject selectObject = mSelectedObject;
        if (selectObject != null) {
            ChangeLabelDialog changeLabelDialog = (ChangeLabelDialog) dialog;

            if (selectObject.getObjectInfo().mDisplayName != null) {
                changeLabelDialog.setCustomName(selectObject.getObjectInfo().mDisplayName);
            } else {
                changeLabelDialog.setCustomName(HomeUtils.getAppLabel(getContext(), selectObject.getObjectInfo()
                        .getResolveInfo()));
            }
            changeLabelDialog.setOnDialogFinished(new ChangeLabelDialog.OnDialogFinished() {
                @Override
                public void onFinish(String displayName, boolean changed) {
                    if (changed) {
                        selectObject.updateLabel(displayName);
                    }
                }

            });
        }
    }

    private void prepareChangeColorDialog(Dialog dialog) {
        ChangeColorDialog changecolorDialog = (ChangeColorDialog) dialog;
        changecolorDialog.setOperateObject(mSelectedObject);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case ChangeIconDialog.REQUESTCODE_APPOPTION_GETIMAGE2:
            if (data != null) {
                final Uri imageFileUri = data.getData();
                int iconSize = (int) (getHomeSceneInfo().mCellWidth - getHomeSceneInfo().mAppIconPaddingLeft
                        - getHomeSceneInfo().mAppIconPaddingRight);
                Bitmap bitmap = null;
                if (imageFileUri != null) {
                    bitmap = HomeUtils.decodeSampledBitmapFromResource(getContext(), imageFileUri, iconSize, iconSize);
                } else {
                    bitmap = HomeUtils
                            .decodeSampledBitmapFromResource(HomeUtils.TMPDATA_IMAGE_PATH, iconSize, iconSize);
                }
                if (bitmap != null) {
                    dismissDialog(DIALOG_OBJECT_CHANGE_ICON);
                    if (mSelectedObject != null) {
                        mSelectedObject.updateIcon(bitmap);
                    }
                }
                break;
            }
            break;
        case REQUEST_CODE_BIND_SHORTCUT:
            if (resultCode == Activity.RESULT_OK) {
                HomeManager.getInstance().startActivityForResult(data, REQUEST_CODE_SELECT_SHORTCUT);
            }
            break;
        default:
            break;
        }
    }

    private Bundle setObjectLongClickDialogContent() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        SESceneManager.getInstance().getGLActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int screenHeight = displaymetrics.heightPixels;
        if (mSelectedObject == null) {
            return null;
        }

        int slotType = mSelectedObject.getObjectInfo().mSlotType;
        int x = 0;
        int y = 0;
        if (slotType == ObjectInfo.SLOT_TYPE_WALL) {
            float top = mSelectedObject.getObjectSlot().mSpanY / 2f * getHomeSceneInfo().mCellHeight;
            SEVector3f vector3f = mSelectedObject.getAbsoluteTranslate().clone();
            vector3f.selfAdd(new SEVector3f(0, 0, top));
            SEVector2i position = getCamera().worldToScreenCoordinate(vector3f);
            // value 1024 is screen height of BKB phone (600X1024), hard code 55
            // is base on BKB phone.
            y = position.getY() - 55 * screenHeight / 1024;
            x = position.getX();
            if (mSelectedObject.getObjectSlot().mSpanY == 1) {
                y -= 55 * screenHeight / 1024;
            }
        } else if (slotType == ObjectInfo.SLOT_TYPE_DESKTOP) {
            SEVector3f vector3f = mSelectedObject.getAbsoluteTranslate().clone();
            vector3f.selfAdd(new SEVector3f(0, 0, 90));
            SEVector2i position = getCamera().worldToScreenCoordinate(vector3f);
            x = position.getX();
            y = position.getY() - 70 * screenHeight / 1024;
        } else if (slotType == ObjectInfo.SLOT_TYPE_GROUND) {
            float top = 2 * getHomeSceneInfo().mCellHeight;
            SEVector3f vector3f = mSelectedObject.getAbsoluteTranslate().clone();
            vector3f.selfAdd(new SEVector3f(0, 0, top));
            SEVector2i position = getCamera().worldToScreenCoordinate(vector3f);
            x = position.getX();
            y = position.getY() - 55 * screenHeight / 1024;
        } else {
            x = mSelectedObject.getTouchX();
            y = (int) (mSelectedObject.getTouchY() - 120 * getScreenDensity());
        }

        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        boolean reversal;
        if (x > mSceneInfo.getSceneWidth() / 2) {
            reversal = true;
        } else {
            reversal = false;
        }
        Bundle bundle = new Bundle();
        bundle.putInt("dlgx", x);
        bundle.putInt("dlgy", y);
        bundle.putBoolean("reversal", reversal);
        return bundle;
    }

    public class Item {
        public CharSequence mLabel;
        public Drawable mIcon;
        public String mPkgName;
        public String mClsName;

        public Item(CharSequence label, Drawable icon, String pkg, String cls) {
            mLabel = label;
            mIcon = icon;
            mPkgName = pkg;
            mClsName = cls;
        }

        @Override
        public String toString() {
            return mLabel.toString();
        }
    }

    public void updateComponentInDB(Item item) {
        String pkgName = item.mPkgName;
        String clsName = item.mClsName;
        if (TextUtils.isEmpty(pkgName) || TextUtils.isEmpty(clsName)) {
            return;
        }
        if (mSetBindObject != null) {
            mSetBindObject.updateComponentName(new ComponentName(pkgName, clsName));
        }
    }

    public void onAddModelToDB(final ModelInfo modelInfo) {

    }

    @Override
    public void onRemoveModelFromDB(final ModelInfo modelInfo) {
        new SECommand(this) {
            public void run() {
                int size = modelInfo.getInstances().size();
                /*
                 * remove all instances from house
                 */
                for (int i = 0; i < size; i++) {
                    SEObject delObj = modelInfo.getInstances().get(i);
                    SEObject parent = delObj.getParent();
                    parent.removeChild(delObj, true);
                }
            }
        }.execute();
    }

    /**
     * 主题切换前，矫正相机位置。 主题切换有两种模式： 一种是只换房间，以及微调物体摆放的位置和相机位置 第二种，重新加载整个场景
     */
    private void onSceneChanged() {
        if (!mLoadCompleted) {
            return;
        }
        moveToWallSight(new SEAnimFinishListener() {
            public void onAnimationfinish() {
                ThemeInfo themeInfo = HomeManager.getInstance().getCurrentThemeInfo();
                HomeUtils.updateHouseName(getContext(), themeInfo.mSceneName, themeInfo.mHouseName);
                if (themeInfo.mSceneName.equals(getSceneName())) {
                    getHomeSceneInfo().setThemeInfo(themeInfo);
                    getCamera().notifySurfaceChanged(getHomeSceneInfo().getSceneWidth(),
                            getHomeSceneInfo().getSceneHeight());
                    // 通知每一个物体场景已经变了，false代表第一种情况
                    getContentObject().onSceneChanged(SCENE_CHANGED_TYPE.NEW_CONFIG);
                } else {
                    removeAllDialog();
                    // 通知每一个物体场景已经变了，true代表第二种情况
                    getContentObject().onSceneChanged(SCENE_CHANGED_TYPE.NEW_SCENE);
                    // 清除ModelInfo关于场景的信息
                    HomeManager.getInstance().getModelManager().clearModelStatus();
                    // 释放老场景
                    release();
                    // 加载新场景
                    HomeSceneInfo sceneInfo = new HomeSceneInfo();
                    sceneInfo.setThemeInfo(themeInfo);
                    HomeScene screen = new HomeScene(getContext(), sceneInfo.mSceneName);
                    screen.setHomeSceneInfo(sceneInfo);
                    SESceneManager.getInstance().setCurrentScene(screen);
                    HomeManager.getInstance().changeCurrentHomeScene(screen);
                    SESceneManager.getInstance().start3DScene(getHomeSceneInfo().getSceneWidth(),
                            getHomeSceneInfo().getSceneHeight());
                }
            }
        });
    }

    private void removeAllDialog() {
        removeDialog(DIALOG_DELETE_OBJECTS);
        removeDialog(DIALOG_SELECT_WALLPAPER);
        removeDialog(DIALOG_WALL_LONG_CLICK);
        removeDialog(DIALOG_OBJECT_LONG_CLICK);
        removeDialog(DIALOG_OBJECT_CHANGE_LABLE);
        removeDialog(DIALOG_OBJECT_CHANGE_ICON);
        removeDialog(DIALOG_HELPER);
        removeDialog(DIALOG_ADJUST_CAMERA);
        removeDialog(DIALOG_WEATHER);
        removeDialog(DIALOG_WALL_LONG_CLICK_ADD_APP);
        removeDialog(DIALOG_WALL_LONG_CLICK_ADD_FOLDER);
        removeDialog(DIALOG_EDITFOLDER);
        removeDialog(DIALOG_CHANGE_COLOR);
        removeDialog(DIALOG_WALL_INDICATOR);
    }

    private void dismissAllDialog() {
        dismissDialog(DIALOG_DELETE_OBJECTS);
        dismissDialog(DIALOG_SELECT_WALLPAPER);
        dismissDialog(DIALOG_WALL_LONG_CLICK);
        dismissDialog(DIALOG_OBJECT_LONG_CLICK);
        dismissDialog(DIALOG_OBJECT_CHANGE_LABLE);
        dismissDialog(DIALOG_OBJECT_CHANGE_ICON);
        dismissDialog(DIALOG_HELPER);
        dismissDialog(DIALOG_ADJUST_CAMERA);
        dismissDialog(DIALOG_WEATHER);
        dismissDialog(DIALOG_WALL_LONG_CLICK_ADD_APP);
        dismissDialog(DIALOG_WALL_LONG_CLICK_ADD_FOLDER);
        dismissDialog(DIALOG_EDITFOLDER);
        dismissDialog(DIALOG_CHANGE_COLOR);
        dismissDialog(DIALOG_WALL_INDICATOR);
    }

    private House mHouse;

    public void setHouse(House house) {
        mHouse = house;
    }

    private Desk mDesk;

    public void setDesk(Desk desk) {
        mDesk = desk;
        SEVector3f newSize = desk.getDeskSize();
        if (!newSize.equals(mDownsideCameraBoundary)) {
            // reset and calculate it later while necessary
            mDeskObserverSight = null;
        }
    }


    private Cloud mSky;

    public void setSky(Cloud cloud) {
        mSky = cloud;
    }

    public Cloud getSky() {
        return mSky;
    }

    /**
     * Get empty slots of current wall.
     */
    private int getEmptySlotNumOfCurrentWall() {
        if ( mHouse == null) {
            return 0;
        }

        NormalObject wall = mHouse.getWall(getWallNearestIndex());
        if (wall == null) {
            return 0;
        }
        int wallc = getHomeSceneInfo().mCellCountX * getHomeSceneInfo().mCellCountY;
        int c = wall.getChildObjects() == null ? 0 : wall.getChildObjects().size();
        if (c == 0) {
            return wallc;
        }
        int objc = 0;
        for (SEObject child : wall.getChildObjects()) {
            objc += ObjectInfo.calculateUsedSlotCount(child);
        }
        if (objc >= wallc) {
            return 0;
        }
        return (wallc - objc);
    }

    /**
     * Get max-num a floder can contains.
     */
    public static int getChildMaxNumInFolder() {
        // TODO
        return 12;
    }

    @Override
    public void onLanguageChanged() {
        if (mOptionMenu != null) {
            mOptionMenu.dismiss();
            mOptionMenu = null;

            mOptionMenu = new OptionMenu(getContext());
        }
    }

    @Override
    public void onAppSelected(ArrayList<AddObjectItemInfo> selectedList, int availableNum) {
        if ((selectedList == null) || (selectedList.size() <= 0)) {
            return;
        }
        if (availableNum <= 0) {
            Toast.makeText(getContext(), R.string.no_room, Toast.LENGTH_SHORT).show();
            return;
        }

        final VesselLayer vesselLayer = mHouse.getVesselLayer();
        if (vesselLayer == null) {
            return;
        }

        int c = 0;
        for (ItemInfo iia : selectedList) {
            c++;
            if (c > availableNum) {
                if (selectedList.size() > availableNum) {
                    Toast.makeText(getContext(), R.string.no_room, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            if (iia == null) {
                continue;
            }
            ItemInfo ii = new AppItemInfo(getContext(), iia.getResolveInfo(), iia.getComponentName());
            ii.mItemType = ItemInfo.ITEM_TYPE_APP;
            final AppObject itemObject = AppObject.create(this, ii);
            itemObject.setIsFresh(false);
            new SECommand(this) {
                public void run() {
                    getContentObject().addChild(itemObject, true);
                    itemObject.initStatus();
                    boolean bl = vesselLayer.placeObjectToVessel(itemObject);
                    if (bl == false) {
                        // TODO
                    }
                }
            }.execute();
        }
    }

    // #########################################dialog relates##########
    public void showDialog(final int id) {
        SESceneManager.getInstance().runInUIThread(new Runnable() {
            public void run() {
                if (((HomeActivity) SESceneManager.getInstance().getGLActivity()).isLiving()) {
                    SESceneManager.getInstance().getGLActivity().showDialog(id);
                }
            }
        });
    }

    public void showDialog(final int id, final Bundle bundle) {
        SESceneManager.getInstance().runInUIThread(new Runnable() {
            public void run() {
                // if activity have been destroyed, no need pop up the dialog.
                if (((HomeActivity) SESceneManager.getInstance().getGLActivity()).isLiving()) {
                    try {
                        SESceneManager.getInstance().getGLActivity().showDialog(id, bundle);
                    } catch (Exception e) {
                        Log.i("test", "error is " + e.toString());
                    }

                }
            }
        });
    }

    public void removeDialog(final int id) {
        SESceneManager.getInstance().runInUIThread(new Runnable() {
            public void run() {
                try {
                    SESceneManager.getInstance().getGLActivity().removeDialog(id);
                } catch (Exception e) {
                }
            }
        });
    }

    public void dismissDialog(final int id) {
        SESceneManager.getInstance().runInUIThread(new Runnable() {
            public void run() {
                try {
                    SESceneManager.getInstance().getGLActivity().dismissDialog(id);
                } catch (Exception e) {
                }
            }
        });
    }

    // #########################################below are the action of camera
    private SECameraData mDeskObserverSight;
    private SECameraData mSkyObserverSight;
    private SEEmptyAnimation mMoveSightAnimation;
    private SEEmptyAnimation mSetRadiusAndFovAnim;
    private float mSkyY = 0;

    public void playSetRadiusAndFovAnim(final float endRadius, final float endFov, SEAnimFinishListener listener) {
        stopAllAnimation();
        final float curRadius = getCamera().getRadius();
        final float curFov = getCamera().getFov();
        if (endRadius == curRadius) {
            if (listener != null) {
                listener.onAnimationfinish();
            }
            return;
        }
        int animTimes = (int) (Math.abs(endRadius - curRadius) / 20);
        mSetRadiusAndFovAnim = new SEEmptyAnimation(this, 0, 1, animTimes) {
            @Override
            public void onAnimationRun(float value) {
                float radius = (endRadius - curRadius) * value + curRadius;
                float fov = (endFov - curFov) * value + curFov;
                getCamera().setRadiusAndFov(radius, fov);
            }
        };
        mSetRadiusAndFovAnim.setAnimFinishListener(listener);
        mSetRadiusAndFovAnim.execute();
    }


    public void changeSight(float skyY, boolean breakAnimation) {
        if (breakAnimation) {
            stopAllAnimation();
        }
        if (mSkyY == 0) {
            getCamera().save();
            if (null == mDesk) {
                mDeskObserverSight = getDownsideObserverCamera();
            } else {
                mDeskObserverSight = mDesk.getObserveCamera();
            }
            mSkyObserverSight = mSky.getObserveCamera();
        }
        if (skyY > 1) {
            mSkyY = 1;
        } else if (skyY < -1) {
            mSkyY = -1;
        } else {
            mSkyY = skyY;
        }
        if (mSkyY == 0) {
            setStatus(HomeScene.STATUS_ON_DESK_SIGHT, false);
            setStatus(HomeScene.STATUS_ON_SKY_SIGHT, false);
        } else if (mSkyY < 0) {
            setStatus(HomeScene.STATUS_ON_DESK_SIGHT, true);
            setStatus(HomeScene.STATUS_ON_SKY_SIGHT, false);
        } else {
            setStatus(HomeScene.STATUS_ON_DESK_SIGHT, false);
            setStatus(HomeScene.STATUS_ON_SKY_SIGHT, true);
        }
        SEVector3f srcLoc = getCamera().restore().mLocation;
        SEVector3f srcAxisZ = getCamera().restore().mAxisZ;
        float srcFov = getCamera().restore().mFov;
        float paras = Math.abs(mSkyY);
        SEVector3f desAxisZ;
        SEVector3f desLoc;
        float desFov;
        if (mSkyY >= 0) {
            desAxisZ = mSkyObserverSight.mAxisZ;
            desLoc = mSkyObserverSight.mLocation;
            desFov = mSkyObserverSight.mFov;
        } else {
            desAxisZ = mDeskObserverSight.mAxisZ;
            desLoc = mDeskObserverSight.mLocation;
            desFov = mDeskObserverSight.mFov;
        }
        getCamera().getCurrentData().mLocation = srcLoc.add(desLoc.subtract(srcLoc).mul(paras));
        getCamera().getCurrentData().mAxisZ = srcAxisZ.add(desAxisZ.subtract(srcAxisZ).mul(paras));
        getCamera().getCurrentData().mFov = srcFov + (desFov - srcFov) * paras;
        getCamera().setCamera();
    }

    public float getSightValue() {
        return mSkyY;
    }

    public void moveToSkySight(final SEAnimFinishListener l) {
        moveSightTo(mSkyY, 1, l);
    }

    public void moveToDeskSight(SEAnimFinishListener l) {
        moveSightTo(mSkyY, -1, l);
    }

    public void moveSightTo(float from, float to, SEAnimFinishListener l) {
        if (from == to) {
            if (l != null) {
                l.onAnimationfinish();
            }
            return;
        }
        int animTimes = (int) (Math.abs(to - from) * 10);
        mMoveSightAnimation = new SEEmptyAnimation(this, from, to, animTimes) {

            @Override
            public void onAnimationRun(float value) {
                changeSight(value, false);
            }

        };
        mMoveSightAnimation.setAnimFinishListener(l);
        mMoveSightAnimation.execute();

    }

    public void moveToWallSight(final SEAnimFinishListener l) {
        moveSightTo(mSkyY, 0, l);
    }

    public boolean isBusy() {
        if (mMoveSightAnimation != null) {
            if (!mMoveSightAnimation.isFinish()) {
                return true;
            }
        }

        if (mSetRadiusAndFovAnim != null) {
            if (!mSetRadiusAndFovAnim.isFinish()) {
                return true;
            }
        }
        return false;
    }

    public void stopAllAnimation() {
        if (mMoveSightAnimation != null) {
            mMoveSightAnimation.stop();
        }
        if (mSetRadiusAndFovAnim != null) {
            mSetRadiusAndFovAnim.stop();
        }
    }

    @Override
    public SECameraData getCameraData() {
        return getHomeSceneInfo().mSECameraData;
    }

    private void onWallpaperChanged() {
        if (null != mHouse) {
            mHouse.onWallPaperChanged();
        }
    }

    @Override
    public void onFolderCreated(final String folderName, final ArrayList<AddObjectItemInfo> selectedList) {
        if ((selectedList == null) || (selectedList.size() <= 0)) {
            return;
        }
        new SECommand(this) {
            public void run() {
                VesselLayer vesselLayer = mHouse.getVesselLayer();
                Folder folder = Folder.create(HomeScene.this, folderName);
                folder.setVisible(false);
                getContentObject().addChild(folder, true);
                folder.initStatus();
                int c = 0;
                int max = getChildMaxNumInFolder();
                for (ItemInfo iia : selectedList) {
                    ItemInfo ii = new AppItemInfo(getContext(), iia.getResolveInfo(), iia.getComponentName());
                    ii.mItemType = ItemInfo.ITEM_TYPE_APP;
                    AppObject itemObject = AppObject.create(HomeScene.this,
                            (AppItemInfo) ii);
                    itemObject.getObjectInfo().mVesselName = folder.mName;
                    itemObject.getObjectInfo().mVesselIndex = folder.getObjectInfo().mIndex;
                    itemObject.getObjectInfo().mObjectSlot.mSlotIndex = c;
                    itemObject.setIsFresh(false);
                    itemObject.initStatus();
                    itemObject.setVisible(false);
                    folder.addChild(itemObject, true);
                    c++;
                    if (c >= max) {
                        break;
                    }
                }
                folder.updateFolderCover();
                folder.setVisible(true);
                vesselLayer.placeObjectToVessel(folder);
            }
        }.execute();
    }

    public List<WallCellLayer> getAllWallCellLayerList() {
        ArrayList<WallCellLayer> mAllLayer = new ArrayList<WallCellLayer>();
        if (null != mHouse) {
            for (int i = 0; i < mSceneInfo.mWallNum; i++) {
                WallCellLayer wallCellLayer = (WallCellLayer)
                        mHouse.getWall(i).getVesselLayer();
                mAllLayer.add(wallCellLayer);
            }
        }
        return mAllLayer;
    }

    /// invocation by normal object
    public void startResize(NormalObject object) {
        if (null != object && object.canBeResized()) {
            getDragLayer().startResize(object);
        }
    }

    // a normal object sit on some wall slot.
    public void onObjectSlotChanged(NormalObject object) {
        if (null == object) return;

        VesselObject vessel = object.getVesselParent();
        if (null != vessel) {
            vessel.notifyObjectOnSlotSeat(object);
        }
    }

    public Rect getAssociationFrameRect(NormalObject decorator) {
        if (null != decorator) {
            VesselObject vessel = decorator.getVesselParent();
            if (null != vessel) {
                return vessel.getAssociationFrameRect(decorator);
            }
        }

        return null;
    }
    /// code for shelf decorator end

    public void showAllApps() {
        handleMessage(MSG_TYPE_SHOW_APP_VIEW, ApplicationMenu.TYPE_SHOW_KEEP_LAST);
    }

    public float getGroundLine() {
        SEVector3f testPoint = new SEVector3f(0, mSceneInfo.mWallRadius, 0);
        return getCamera().worldToScreenCoordinate(testPoint).getY();
    }

    private final static double MAX_SIGHT_ANGLE = Math.PI / 6;
    private final static float SIGHT_FOV = 30;
    private SEVector3f mDownsideCameraBoundary = new SEVector3f(500, 360, 160);
    private SECameraData getDownsideObserverCamera() {
        if (mDeskObserverSight!= null) {
            return mDeskObserverSight;
        }

        SECameraData cameraData = new SECameraData();
        cameraData.mAxisZ = new SEVector3f(0, -(float) Math.cos(MAX_SIGHT_ANGLE),
                (float) Math.sin(MAX_SIGHT_ANGLE));
        cameraData.mFov = SIGHT_FOV;
        SEVector3f centerLocation = new SEVector3f(0, 0, mDownsideCameraBoundary.getZ());
        float paras = (float) (mDownsideCameraBoundary.getX() * 0.5f / Math.tan(SIGHT_FOV * Math.PI / 360));
        SEVector3f screenOrientation = new SEVector3f(0, (float) Math.cos(-MAX_SIGHT_ANGLE),
                (float) Math.sin(-MAX_SIGHT_ANGLE));
        SEVector3f loc = centerLocation.subtract(screenOrientation.mul(paras));
        SEVector3f locOri = new SEVector3f(0, (float) Math.sin(MAX_SIGHT_ANGLE), (float) Math.cos(MAX_SIGHT_ANGLE));
        float distance = (float) (getCamera().getWidth() / (2 * Math.tan(SIGHT_FOV * Math.PI / 360)));
        double sightFovH = Math.atan(getCamera().getHeight() / (2 * distance)) + MAX_SIGHT_ANGLE;
        SEVector3f topOri = new SEVector3f(0, (float) Math.cos(sightFovH), (float) -Math.sin(sightFovH));
        SEVector3f top = new SEVector3f(0, -mDownsideCameraBoundary.getY() / 2, mDownsideCameraBoundary.getZ());
        float para = ((loc.getY() - top.getY()) / topOri.getY() - (loc.getZ() - top.getZ()) / topOri.getZ())
                / (locOri.getZ() / topOri.getZ() - locOri.getY() / topOri.getY());
        cameraData.mLocation = new SEVector3f(0, loc.getY() + locOri.getY() * para, loc.getZ() + locOri.getZ() * para);
        return cameraData;
    }

    public void changeDeskTo(String objName) {
        if (null != mDesk) {
            mDesk.changeDeskTo(objName);
        }
    }

    public void showDesk(SEAnimFinishListener listener) {
        if (null != mDesk) {
            mDesk.show(listener);
        } else if (null != listener) {
            listener.onAnimationfinish();
        }
    }

    public void hideDesk(SEAnimFinishListener listener) {
        if (null != mDesk) {
            mDesk.hide(listener);
        } else if (null != listener) {
            listener.onAnimationfinish();
        }
    }

    public List<VesselLayer> getDragDropLayerList() {
        List<VesselLayer> targetList = new ArrayList<VesselLayer>();
        if (null != mDesk) {
            targetList.add(mDesk.getVesselLayer());
        }
        if (null != mHouse) {
            targetList.add(mHouse.getVesselLayer());
        }
//        targetList.add(mHomeScene.getDesk().getVesselLayer());
//        targetList.add(mHomeScene.getHouse().getVesselLayer());
        return targetList;
    }

    public List<VesselLayer> getDragDropLayerInHouse() {
        List<VesselLayer> mAllLayer = new ArrayList<VesselLayer>();
        if (null != mHouse) {
            mAllLayer.add(new WallLayer(this, mHouse));

            VesselObject vessel = mHouse.getGround();
            if (null != vessel) {
                mAllLayer.add(vessel.getVesselLayer());
            }
        }
        return mAllLayer;
    }

    public void addWallRadiusChangedListener(House.WallRadiusChangedListener listener) {
        if (null != mHouse && null != listener) {
            mHouse.addWallRadiusChangedListener(listener);
        }
    }

    public void removeWallRadiusChangedListener(House.WallRadiusChangedListener listener) {
        if (null != mHouse && null != listener) {
            mHouse.removeWallRadiusChangedListener(listener);
        }
    }

    public void toNearestFace() {
        mHouse.toNearestFace(null, 2);
    }

    public boolean placeToNearestWallFace(NormalObject normalObject) {
        if (null == mHouse) {
            return false;
        }

        VesselLayer vesselLayer = mHouse.getWall(getWallNearestIndex()).getVesselLayer();
        return vesselLayer.placeObjectToVessel(normalObject);
    }

    public int getWallNearestIndex() {
        return null == mHouse ? 0 : mHouse.getWallNearestIndex();
    }

    public void stopHouseAnimation() {
        if (null != mHouse) {
            mHouse.stopAllAnimation(null);
        }
    }

    public void toLeftWallFace(SEAnimFinishListener listener) {
        if (null != mHouse) {
            mHouse.toLeftFace(listener, 1.5f);
        }
    }

    public void toRightWallFace(SEAnimFinishListener listener) {
        mHouse.toRightFace(listener, 1.5f);
    }

    /// wallpaper operation begin

    public void mayChangeWallPaper() {
        if (null != mHouse) {
            mHouse.mayChangeWallPaper();
        }
    }

    public void updateWallpaperBundle(ArrayList<String> wallBundle, ArrayList<String> groundBundle) {
        if (null != mHouse) {
            mHouse.updateWallpaperBundle(wallBundle, groundBundle);
        }
    }
    public ArrayList<RawPaperItem> queryRawPaperItems() {
        if (null != mHouse) {
            return mHouse.queryRawPaperItems();
        }
        return new ArrayList<RawPaperItem>();
    }

    /// wallpaper operation end
    public void notifyWallpaperChanged() {
        SESceneManager.getInstance().removeMessage(HomeScene.MSG_TYPE_UPDATE_WALLPAPER);
        SESceneManager.getInstance().handleMessage(HomeScene.MSG_TYPE_UPDATE_WALLPAPER, null);
    }

    public void onWallLabelShow(int index) {
        if (null != mHouse) {
            mHouse.onWallLabelShow(index);
        }
    }

    public int getLabelShownPreference() {
        return HomeUtils.getLabelShownPreference(getContext());
    }

    /// weather service begin
    private IWeatherService mWeatherSevice;

    private Dialog createWeatherDialog() {
        if (mWeatherSevice == null) {
            return null;
        }

        final WeatherDialog dialog = new WeatherDialog(SESceneManager.getInstance().getGLActivity(), mWeatherSevice);
        dialog.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_BACK:
                        moveToWallSight(null);
                        dialog.dismiss();
                        break;
                }
                return true;
            }

        });
        return dialog;
//        return null;
    }

    private void showWeatherDialog(Object message) {
        if (message instanceof IWeatherService) {
            mWeatherSevice = (IWeatherService) message;
            removeDialog(DIALOG_WEATHER);
            showDialog(DIALOG_WEATHER);
        }
    }

    /// weather service end
}
