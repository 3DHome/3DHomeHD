package com.borqs.se.home3d;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.borqs.framework3d.home3d.DockObject;
import com.borqs.framework3d.home3d.HouseObject;
import com.borqs.freehdhome.R;
import com.borqs.market.utils.MarketUtils;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SEVector2i;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.download.Utils;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SECommand;
import com.borqs.se.home3d.ModelInfo.ImageItem;
import com.borqs.se.home3d.ProviderUtils.ModelColumns;
import com.borqs.se.home3d.ProviderUtils.ObjectInfoColumns;
import com.borqs.se.home3d.ProviderUtils.VesselColumns;
import com.borqs.se.engine.SECameraData;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectGroup;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.engine.SESceneManager.ModelChangeCallBack;
import com.borqs.se.engine.SESceneManager.UnlockScreenListener;
import com.borqs.se.shortcut.AppItemInfo;
import com.borqs.se.shortcut.ItemInfo;
import com.borqs.se.shortcut.LauncherModel;
import com.borqs.se.shortcut.LauncherModel.LanguageChangeCallBack;
import com.borqs.se.widget3d.ADViewController;
import com.borqs.se.widget3d.AppObject;
import com.borqs.se.widget3d.House;
import com.borqs.se.widget3d.NormalObject;
import com.borqs.se.widget3d.ObjectInfo;
import com.borqs.se.widget3d.WallShelf;

public class HomeScene extends SEScene implements UnlockScreenListener, ModelChangeCallBack, LanguageChangeCallBack {

    public static final int REQUEST_CODE_SELECT_WIDGET = 0;
    public static final int REQUEST_CODE_SELECT_SHORTCUT = REQUEST_CODE_SELECT_WIDGET + 1;
    public static final int REQUEST_CODE_BIND_WIDGET = REQUEST_CODE_SELECT_SHORTCUT + 1;
    public static final int REQUEST_CODE_SELECT_WALLPAPER_IMAGE = REQUEST_CODE_BIND_WIDGET + 1;
    public static final int REQUEST_CODE_SELECT_WALLPAPER_CAMERA = REQUEST_CODE_SELECT_WALLPAPER_IMAGE + 1;

    public static final int MSG_TYPE_SHOW_BIND_APP_DIALOG = 0;
    public static final int MSG_TYPE_SHOW_OBJECT_VIEW = MSG_TYPE_SHOW_BIND_APP_DIALOG + 1;
    public static final int MSG_TYPE_SHOW_APP_VIEW = MSG_TYPE_SHOW_OBJECT_VIEW + 1;
    public static final int MSG_TYPE_SHOW_DELETE_OBJECTS  = MSG_TYPE_SHOW_APP_VIEW +1;
    public static final int MSG_TYPE_SHOW_SELECT_WALLPAPER_DIALOG = MSG_TYPE_SHOW_DELETE_OBJECTS + 1;
    public static final int MSG_TYPE_SHOW_WALL_LONG_CLICK_DIALOG  = MSG_TYPE_SHOW_SELECT_WALLPAPER_DIALOG + 1;
    public static final int MSG_TYPE_SHOW_EDIT_SCENE_DIALOG = MSG_TYPE_SHOW_WALL_LONG_CLICK_DIALOG + 1;
    public static final int MSG_TYPE_SHOW_SCREEN_INDICATOR = MSG_TYPE_SHOW_EDIT_SCENE_DIALOG + 1;
    
    public static final int MSG_TYPE_SHOW_OBJECT_LONG_CLICK_DIALOG = MSG_TYPE_SHOW_SCREEN_INDICATOR + 1;
    public static final int MSG_TYPE_DISMISS_OBJECT_LONG_CLICK_DIALOG = MSG_TYPE_SHOW_OBJECT_LONG_CLICK_DIALOG + 1;
    
    public static final int MSG_TYPE_SHOW_OPTION_MENU_DIALOG = MSG_TYPE_DISMISS_OBJECT_LONG_CLICK_DIALOG + 1;

    private static final int DIALOG_SELECT_APP = 0;
    private static final int DIALOG_DELETE_OBJECTS = 1;
//    private static final int DIALOG_SELECT_WALLPAPER = 2;
//    private static final int DIALOG_WALL_LONG_CLICK = 3;
    private static final int DIALOG_OBJECT_LONG_CLICK = 4;
    
    private Configuration mPreviousConfig;
    private ObjectsMenu mObjectsPreview;
    private ApplicationMenu mWidgetsPreview;
    private HelperMenu mHelperPreview;
    private OptionMenu mOptionMenu;
    private NormalObject mSetBindObject;
    private int mMenuCheckStatus;
    private DelayFrame mDelayFrame;
    private boolean mLoadCompleted;
    private String mDeleteObjName;
    
    private NormalObject mSelectedObject;

    //for wallpaper
    private Bundle mWallPaperMsg;
    public static final String MSG_CONTENT_IMAGE = "image";
    public static final String MSG_CONTENT_IMAGE_SIZE_X = "imgSizeX";
    public static final String MSG_CONTENT_IMAGE_SIZE_Y = "imgSizeY";
    public static final String MSG_CONTENT_IMAGE_OUTPUT = "output";
    private int mImgSizeX, mImgSizeY;
    private String mCurrentImage;
    private String mCurrentTheme;
    private String mOutPutPath;

    private DockObject mDockObject;
    private HouseObject mHouseObject;

//    private IconPageIndicator mIndicator;
    public HomeScene(String sceneName) {
        super(sceneName);
        mMenuCheckStatus = SEScene.STATUS_APP_MENU + SEScene.STATUS_HELPER_MENU + SEScene.STATUS_OBJ_MENU
                + SEScene.STATUS_ON_WIDGET_SIGHT + SEScene.STATUS_MOVE_OBJECT + SEScene.STATUS_DISALLOW_TOUCH
                + SEScene.STATUS_ON_WALL_DIALOG;
    }

    @Override
    public void onSceneStart() {
        mLoadCompleted = false;
        setStatus(STATUS_DISALLOW_TOUCH, true);
        mPreviousConfig = new Configuration();
        mPreviousConfig.setTo(mContext.getResources().getConfiguration());
        SESceneManager.setMainScene_JNI(mSceneName);
        loading();
        SESceneManager.getInstance().addUnlockScreenListener(this);
        ADViewController adController = ADViewController.getInstance();
        adController.setSEScene(this);
        getCamera().addCameraChangedListener(adController);
    }

    private void loading() {
        SESceneManager.getInstance().debugOutput("SE3DHomeScene loading enter.");
        ObjectInfo rootVesselInfo = findRootVessel();
        if (rootVesselInfo != null) {
            NormalObject root = HomeUtils.getObjectByClassName(HomeScene.this, rootVesselInfo);
            setContentObject(root);// set root object of scene
//            mDelayFrame = new DelayFrame(SE3DHomeScene.this);
//            root.addChild(mDelayFrame, true);
            SESceneManager.getInstance().debugOutput("SE3DHomeScene loading root ready.");
            mObjectsPreview = new ObjectsMenu(this, "ObjectsMenu");
            setObjectsMenu(mObjectsPreview);// add objects menu
            SESceneManager.getInstance().debugOutput("SE3DHomeScene loading object shelf ready.");
            root.load(null, new Runnable() {
                public void run() {
                    SESceneManager.getInstance().debugOutput("SE3DHomeScene loading root loaded.");
                    onObjectLoaded();
                    SESceneManager.getInstance().debugOutput("SE3DHomeScene loading object emitted.");
                }
            });
        }
        SESceneManager.getInstance().debugOutput("SE3DHomeScene loading exit.");
    }

    private void onObjectLoaded() {
        mDockObject = ModelInfo.getDockObject(this);
        mHouseObject = ModelInfo.getHouseObject(this);
        mObjectsPreview.loadPreLoadModel(new Runnable() {
            public void run() {
                SESceneManager.getInstance().debugOutput("SE3DHomeScene onObjectLoaded object shelf loaded.");
                onObjectShelfLoaded();
            }
        });
//        calculateCameraRadiusScope();
    }

    private void onObjectShelfLoaded() {
        setStatus(STATUS_DISALLOW_TOUCH, false);
        initScreenIndicator();
        createAndLoadMenu();
        LauncherModel.getInstance().addLanguageChangeCallBack(HomeScene.this);
        mLoadCompleted = true;
    }

    private ObjectInfo findRootVessel() {
        String where = ObjectInfoColumns.SCENE_NAME + "='" + mSceneName 
        		+ "' AND " + VesselColumns.VESSEL_ID + "=-1" + " and " + ObjectInfoColumns.ORIENTATION_STATUS + " = " +
                SettingsActivity.getPreferRotation(mContext);
        ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = resolver.query(ObjectInfoColumns.OBJECT_LEFT_JOIN_ALL, null, where, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                ObjectInfo objectInfo = ObjectInfo.CreateFromDB(cursor);
                return objectInfo;
            }
            cursor.close();
        }
        return null;
    }

    private void createAndLoadMenu() {
        SESceneManager.getInstance().debugOutput("SE3DHomeScene createAndLoadMenu enter.");
        initApplicationMenu();
        initHelperPreview();
        initOptionMenu();
        LauncherModel.getInstance().loadAllData();
        if (!SettingsActivity.getHelpStatus(mContext)) {
            if (mHelperPreview != null) {
                mHelperPreview.show(mDockObject);
            }
            SettingsActivity.setHelpStatus(mContext, true);
        }
        SESceneManager.getInstance().debugOutput("SE3DHomeScene createAndLoadMenu exit.");
    }

    @Override
    public void handleMenuKey() {
        if (BackDoorSettingsActivity.isEnableBackdoor(mContext) &&
                BackDoorSettingsActivity.isOptionMenuAlt(mContext)) {
//            finalizeOptionMenu();
            if ((mMenuCheckStatus & getStatus()) == 0) {
                SESceneManager.getInstance().runInUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (getStatus(SEScene.STATUS_OPTION_MENU)) {
                            BackDoorSettingsActivity.showOptionMenu(mContext, false);
                            setStatus(SEScene.STATUS_OPTION_MENU, false);
                        } else {
                            BackDoorSettingsActivity.showOptionMenu(mContext, true);
                            setStatus(SEScene.STATUS_OPTION_MENU, true);
                        }
                    }
                });
            }
            return;
        }
        
        
        if (mOptionMenu != null) {
            if ((mMenuCheckStatus & getStatus()) == 0) {
                handleMessage(HomeScene.MSG_TYPE_SHOW_OPTION_MENU_DIALOG, null);
            }
        }

//        initOptionMenu();
//
//        if (mOptionMenu != null) {
//            SettingsActivity.saveNeverShowMenuBtn(mContext, true);
//            if ((mMenuCheckStatus & getStatus()) == 0) {
//                if (getStatus(SEScene.STATUS_OPTION_MENU)) {
//                    mOptionMenu.hide(false, null);
//                } else {
//                    mOptionMenu.show();
//                }
//                mOptionMenu.checkButton(false);
//            }
//        }
    }

    private void showOptionMenu() {
        mSESceneManager.runInUIThread(new Runnable() {
            public void run() {
                mOptionMenu.showAtLocation(SESceneManager.getInstance()
                        .getWorkSpace(), Gravity.BOTTOM, 0, 0);
                mOptionMenu.playAnimation();
            }
        });

    }
    
    @Override
    public void handleMessage(int type, Object message) {
        switch (type) {
        case MSG_TYPE_SHOW_OBJECT_VIEW:
            if (mObjectsPreview != null) {
                SEAnimFinishListener l = new SEAnimFinishListener() {
					@Override
					public void onAnimationfinish() {
						mObjectsPreview.show();
					}
				};
				
				// if current camera is default camera,  directly show object menu.
                if(mSECamera.moveToDefaultCamera(l)) {
                	mObjectsPreview.show();
                }
            }
            break;
        case MSG_TYPE_SHOW_APP_VIEW:
            int showType = (Integer)message;
            if (mWidgetsPreview != null) {
                mWidgetsPreview.show(showType, mDockObject);
            }
            break;
        case MSG_TYPE_SHOW_BIND_APP_DIALOG:
            if (message instanceof NormalObject) {
                mSetBindObject = (NormalObject) message;
            }
            showDialog(DIALOG_SELECT_APP);
            break;
        case MSG_TYPE_SHOW_DELETE_OBJECTS:
            mDeleteObjName = (String)message;
            showDialog(DIALOG_DELETE_OBJECTS);
            break;
        case MSG_TYPE_SHOW_SELECT_WALLPAPER_DIALOG:
            Bundle msg = (Bundle) message;
            mCurrentImage = msg.getString(MSG_CONTENT_IMAGE);
            mImgSizeX = msg.getInt(MSG_CONTENT_IMAGE_SIZE_X);
            mImgSizeY = msg.getInt(MSG_CONTENT_IMAGE_SIZE_Y);
            mOutPutPath = msg.getString(MSG_CONTENT_IMAGE_OUTPUT);
            mCurrentTheme = SettingsActivity.getThemeName(mContext);
            closeFloatView();
            getSelectWallPaperDialog();
//            showDialog(DIALOG_SELECT_WALLPAPER);
            break;
        case MSG_TYPE_SHOW_WALL_LONG_CLICK_DIALOG:
            mWallPaperMsg = (Bundle)message;
            closeFloatView();
            getWallLongClickDialog();
//            showDialog(DIALOG_WALL_LONG_CLICK);
            break;
         case MSG_TYPE_SHOW_EDIT_SCENE_DIALOG:
             BackDoorSettingsActivity.showSceneEditDialog(mSESceneManager);
             break;
            case MSG_TYPE_SHOW_SCREEN_INDICATOR:
                final boolean show = (Boolean) message;
                SESceneManager.getInstance().runInUIThread(new Runnable() {
                    @Override
                    public void run() {
//                        if (mIndicator == null && show) {
//                            initScreenIndicator();
//                        }
//
//                        if (mIndicator != null && !show) {
//                            fadeOutAnimate(mIndicator);
//                            mIndicator.setVisibility(View.GONE);
//                        } else if (mIndicator == null && show) {
//                            initScreenIndicator();
//                        } else if (mIndicator != null && show) {
//                            fadeInAnimate(mIndicator);
//                            mIndicator.setVisibility(View.VISIBLE);
//                        }
                    }
                });
                break;
                
            case MSG_TYPE_SHOW_OBJECT_LONG_CLICK_DIALOG:
            	if (message instanceof NormalObject) {
            		mSelectedObject = (NormalObject) message;
            		new SECommand(HomeScene.this) {
            			public void run() {
            				Bundle bundle = setObjectLongClickDialogContent();
            				showDialog(DIALOG_OBJECT_LONG_CLICK, bundle);
            			}
            		}.execute();
            	}
            	break;
            	
            case MSG_TYPE_DISMISS_OBJECT_LONG_CLICK_DIALOG:
            	dissMissDialog(DIALOG_OBJECT_LONG_CLICK);
            	break;
            	
            case MSG_TYPE_SHOW_OPTION_MENU_DIALOG:
                showOptionMenu();
                break;
        }
    }

    private Bundle setObjectLongClickDialogContent() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        mSESceneManager.getGLActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int screenHeight = displaymetrics.heightPixels;
        if (mSelectedObject == null) {
            return null;
        }

        int slotType = mSelectedObject.getObjectInfo().mSlotType;
        int x = 0;
        int y = 0;
        if (slotType == ObjectInfo.SLOT_TYPE_WALL_SHELF || slotType == ObjectInfo.SLOT_TYPE_WALL) {
            float top = mSelectedObject.getObjectSlot().mSpanY / 2f * mSESceneManager.mCellHeight;
            SEVector3f vector3f = mSelectedObject.getAbsoluteTranslate().clone();
            vector3f.selfAdd(new SEVector3f(0, 0, top));
            SEVector2i position = getCamera().worldToScreenCoordinate(vector3f);
            //value 1024 is screen height of BKB phone (600X1024), hard code 55 is base on BKB phone.
            y = position.getY() - AppObject.getAppIconWidth(mContext)/2;
            x = position.getX();
//            if (mSelectedObject.getObjectSlot().mSpanY == 1) {
//                y -= 55 *screenHeight / 1024;
//            }
        } else if (slotType == ObjectInfo.SLOT_TYPE_DESKTOP) {
            SEVector3f vector3f = mSelectedObject.getAbsoluteTranslate().clone();
            vector3f.selfAdd(new SEVector3f(0, 0, 90));
            SEVector2i position = getCamera().worldToScreenCoordinate(vector3f);
            x = position.getX();
            y = position.getY() - AppObject.getAppIconWidth(mContext)/2;
        } else if (slotType == ObjectInfo.SLOT_TYPE_WALL_GAP) {
            float top = 2 * mSESceneManager.mCellHeight;
            SEVector3f vector3f = mSelectedObject.getAbsoluteTranslate().clone();
            vector3f.selfAdd(new SEVector3f(0, 0, top));
            SEVector2i position = getCamera().worldToScreenCoordinate(vector3f);
            x = position.getX();
            y = position.getY() - AppObject.getAppIconWidth(mContext)/2;
        }

        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        boolean reversal;
        if (x > mSESceneManager.getWidth() / 2) {
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
    public static void fadeOutAnimate(View view) {
        view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), android.R.anim.fade_out));
    }

    public static void fadeInAnimate(View view) {
        view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), android.R.anim.fade_in));
    }

    @Override
    public void onRelease(boolean softReset) {
        super.onRelease(softReset);
        if(mOptionMenu != null) {
        	mOptionMenu.dismiss();
            mOptionMenu = null;
        }
        SESceneManager.getInstance().removeUnlockScreenListener(this);
        LauncherModel.getInstance().removeLanguageChangeCallBack(this);

    }

    private void initHelperPreview() {
        mHelperPreview = new HelperMenu(HomeScene.this, "helper_preview");
        getContentObject().addChild(mHelperPreview, true);
    }

    private void initApplicationMenu() {
        mWidgetsPreview = new ApplicationMenu(HomeScene.this, "WidgetsPreview");
        getContentObject().addChild(mWidgetsPreview, true);
    }

    private void initOptionMenu() {
//        if (null == mOptionMenu) {
//            mOptionMenu = new OptionMenu(this, "OptionMenu_group");
//            getContentObject().addChild(mOptionMenu, true);
//            mSECamera.addCameraChangedListener(mOptionMenu);
//        }
    	if(null == mOptionMenu) {
    		mOptionMenu = new OptionMenu(getContext());
    	}
    }

//    private void finalizeOptionMenu() {
//        if (null != mOptionMenu) {
//            mOptionMenu.hide(true, null);
//            getContentObject().removeChild(mOptionMenu, true);
//            mSECamera.removeCameraChangedListener(mOptionMenu);
//            mOptionMenu.release();
//            mOptionMenu = null;
//        }
//    }

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_SELECT_APP:
            return getBindingAppDialog();
        case DIALOG_DELETE_OBJECTS:
            return getDeleteDialog();
//        case DIALOG_SELECT_WALLPAPER:
//            return getSelectWallPaperDialog();
//        case DIALOG_WALL_LONG_CLICK:
//            return getWallLongClickDialog();
        case DIALOG_OBJECT_LONG_CLICK:
            return new Dialog(mSESceneManager.getGLActivity(), R.style.HomeDialogDimUnableStyle);
        }
        return null;
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        new SECommand(this) {
            public void run() {
                if (mLoadCompleted) {
                    setStatus(STATUS_DISALLOW_TOUCH, false);
                }
                if (!SettingsActivity.getHelpStatus(mContext)) {
                    if (mHelperPreview != null) {
                        mHelperPreview.show(mDockObject);
                    }
                    SettingsActivity.setHelpStatus(mContext, true);
                }
                mSESceneManager.runInUIThread(new Runnable() {
                    @Override
                    public void run() {
                        setShelfVisibility();
                    }
                });
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
            if (mSECamera != null) {
                mSECamera.moveToWallSight(new SEAnimFinishListener() {

                    public void onAnimationfinish() {
                        SECameraData current = mSECamera.getCurrentData();
                        SECameraData end = mSceneInfo.getDefaultCameraData();
                        if (current.equals(end)) {
                            return;
                        }
                        mSECamera.zoomInOut();
                    }
                });
            }
        }
        removeDialog(DIALOG_SELECT_APP);
        removeDialog(DIALOG_DELETE_OBJECTS);
        removeDialog(DIALOG_OBJECT_LONG_CLICK);
//        removeDialog(DIALOG_SELECT_WALLPAPER);
    }

    private boolean mHadInitedDialog = false;
    private List<Item> mAppList;

    private Dialog getBindingAppDialog() {
        mHadInitedDialog = true;
        Activity activity = mSESceneManager.getGLActivity();
        String checkedAppName = null;
        if (mSetBindObject != null) {
            if (mSetBindObject.getObjectInfo() != null) {
                if (mSetBindObject.getObjectInfo().getCategoryComponentName() != null) {
                    checkedAppName = mSetBindObject.getObjectInfo().getCategoryComponentName().getClassName();
                }
            }
        }
        mAppList = getAppListSortByName();
        int checkedIndex = -1;
        for (int i = 0; i < mAppList.size(); i++) {
            Item item = mAppList.get(i);
            String clsName = item.mClsName;
            if (!TextUtils.isEmpty(clsName) && clsName.equals(checkedAppName)) {
                checkedIndex = i;
                break;
            }
        }
        ArrayAdapter<Item> adapter = new ArrayAdapter<Item>(activity, R.layout.select_dialog_single_holo,
                android.R.id.text1, mAppList) {
            public View getView(int position, View convertView, ViewGroup parent) {
                // User super class to create the View
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView) v.findViewById(android.R.id.text1);
                // Put the image on the TextView
                Drawable icon = mAppList.get(position).mIcon;
                icon.setBounds(0, 0, 48, 48);
                tv.setCompoundDrawables(icon, null, null, null);
                // Add margin between image and text (support various screen
                // densities)
                int dp5 = (int) (20 * mContext.getResources().getDisplayMetrics().density + 0.5f);
                tv.setCompoundDrawablePadding(dp5);
                return v;
            }
        };
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(mContext.getString(R.string.select_app))
                .setSingleChoiceItems(adapter, checkedIndex, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final Item item = getAppListSortByName().get(whichButton);
                        updateComponentInDB(item);
                        dialog.dismiss();
                    }
                }).create();
//        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(new OnCancelListener() {
            
            @Override
            public void onCancel(DialogInterface arg0) {
                if (mSetBindObject != null && mSetBindObject.getObjectInfo().mComponentName != null) {
                    mSetBindObject.updateComponentName(mSetBindObject.getObjectInfo().mComponentName);
                }
            }
        });
        dialog.setOnDismissListener(new OnDismissListener() {

            public void onDismiss(DialogInterface dialog) {
                if (mSetBindObject != null && mSetBindObject.getObjectInfo().mComponentName == null) {
                    mSetBindObject.getObjectInfo().releaseDB();
                    new SECommand(HomeScene.this) {
                        @Override
                        public void run() {
                            if (mSetBindObject != null) {
//                                mSetBindObject.getParent().removeChild(mSetBindObject, true);
                            }
                        }
                    }.execute();
                }
            }
        });

        return dialog;
    }

    private Dialog getDeleteDialog() {
        Activity activity = mSESceneManager.getGLActivity();
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteDownload(mDeleteObjName);
            }
        };
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setIcon(R.drawable.art_dialog_notice)
                .setPositiveButton(android.R.string.yes, listener)
                .setNegativeButton(android.R.string.no, null).create();
        String message = activity.getResources().getString(
                R.string.confirm_delete_selected_objects, mDeleteObjName);
        String title = activity.getResources().getString(R.string.delete_objects_title);
        dialog.setTitle(title);
        dialog.setMessage(message);
        return dialog;
    }

    private boolean needShowRestoreWallpaperItem() {
        boolean flag = true;
        String where = ModelColumns.THEME_NAME + "='" + mCurrentTheme + "' and "
                + ModelColumns.IAMGE_NAME + "='" + mCurrentImage + "'";
        Context context = SESceneManager.getInstance().getContext();
        ContentResolver resolver = context.getContentResolver();
        Cursor imageCursor = resolver.query(ModelColumns.IMAGE_INFO_URI, null, where, null, null);
        final ImageItem imageItem = new ImageItem();
        imageItem.mImageName = mCurrentImage;
        if (imageCursor.moveToFirst()) {
            imageItem.mPath = imageCursor.getString(imageCursor
                    .getColumnIndexOrThrow(ModelColumns.IMAGE_PATH));
            imageItem.mNewPath = imageCursor.getString(imageCursor
                    .getColumnIndexOrThrow(ModelColumns.IMAGE_NEW_PATH));
            imageCursor.close();

            if(TextUtils.isEmpty(imageItem.mNewPath)
                    || imageItem.mNewPath.equals(imageItem.mPath)) {
                flag = false;
            }
        }
        return flag;
    }



    private void getSelectWallPaperDialog() {
        mSESceneManager.runInUIThread(new Runnable() {
            public void run() {
                boolean showRestoreItem = needShowRestoreWallpaperItem();
                if(mWallPaperContextMenu == null) {
                    mWallPaperContextMenu = new SelectWallPaperDialog(mSESceneManager.getGLActivity(), wallPaperSelectedListener);
                }
                mWallPaperContextMenu.show();
                mWallPaperContextMenu.setRestoreItemVisibily(showRestoreItem);
            }
        });
    }

    private View.OnClickListener wallOptionItemClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(mWallContextMenu != null) {
				mWallContextMenu.dismiss();
			}
			
			if(v.getId() == R.id.option_menu1) {
                new SECommand(HomeScene.this) {
                    public void run() {
                    	HomeUtils.initLocalObject(mContext);
                    	MarketUtils.startLocalProductListIntent(mContext, MarketUtils.CATEGORY_OBJECT, "", false, "");
//                    	handleMessage(SE3DHomeScene.MSG_TYPE_SHOW_OBJECT_VIEW, null);
                    }
                }.execute();
				//handleMessage(SE3DHomeScene.MSG_TYPE_SHOW_OBJECT_VIEW, null);
			}else if(v.getId() == R.id.option_menu2) {
                new SECommand(HomeScene.this) {
                    public void run() {
                        handleMessage(HomeScene.MSG_TYPE_SHOW_APP_VIEW, ApplicationMenu.TYPE_SHOW_APP);
                    }
                }.execute();
				//handleMessage(SE3DHomeScene.MSG_TYPE_SHOW_APP_VIEW, ApplicationMenu.TYPE_SHOW_APP);
			}else if(v.getId() == R.id.option_menu3) {
				handleMessage(HomeScene.MSG_TYPE_SHOW_APP_VIEW, ApplicationMenu.TYPE_SHOW_SHORTCUT);
			}else if(v.getId() == R.id.option_menu4) {
				int id = SESceneManager.getInstance().getAppWidgetHost().allocateAppWidgetId();
                Intent selectIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
                selectIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
                ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<AppWidgetProviderInfo>();
                selectIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
                ArrayList<Bundle> customExtras = new ArrayList<Bundle>();
                selectIntent
                        .putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
                SESceneManager.getInstance().startActivityForResult(selectIntent,
                        HomeScene.REQUEST_CODE_BIND_WIDGET);
			}else if(v.getId() == R.id.option_menu5) {
				handleMessage(HomeScene.MSG_TYPE_SHOW_SELECT_WALLPAPER_DIALOG, mWallPaperMsg);
			}
			
		}
	};
	
	 private View.OnClickListener wallPaperSelectedListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mWallPaperContextMenu != null) {
					mWallPaperContextMenu.dismiss();
				}
				
				if(v.getId() == R.id.option_menu1) {
					Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    intent.putExtra("crop", "true");
                    intent.putExtra("aspectX", mImgSizeX);
                    intent.putExtra("aspectY", mImgSizeY);
                    intent.putExtra("output", Uri.parse(mOutPutPath));
                    intent.putExtra("outputFormat", "JPEG");
                    mSESceneManager.startActivityForResult(intent, REQUEST_CODE_SELECT_WALLPAPER_IMAGE);
				}else if(v.getId() == R.id.option_menu2) {
					Uri u = Uri.parse(mOutPutPath);
                    Intent intent2 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
                    intent2.putExtra(MediaStore.EXTRA_OUTPUT, u);
                    mSESceneManager.startActivityForResult(intent2, REQUEST_CODE_SELECT_WALLPAPER_CAMERA);
				}else if(v.getId() == R.id.option_menu3) {
					Utils.resetWallpaper(getContext(), HomeScene.this, mCurrentTheme, mCurrentImage);
				}
			}
		};

	private LongPressWallDialog mWallContextMenu;
	private SelectWallPaperDialog mWallPaperContextMenu;
    private void getWallLongClickDialog() {
    	mSESceneManager.runInUIThread(new Runnable() {
            public void run() {
                if(mSESceneManager.getGLActivity().isFinishing() == false)
                {
                	if(mWallContextMenu == null) {
                		mWallContextMenu = new LongPressWallDialog(mSESceneManager.getGLActivity(), wallOptionItemClickListener);
                	}            	            	
                	mWallContextMenu.show();
                }
            }
        });
    }

    private void deleteDownload(final String name) {
        SESceneManager.getInstance().removeModelFromScene(name);
        UpdateDBThread.getInstance().process(new Runnable() {

            @Override
            public void run() {
                String localPath = mContext.getFilesDir() + File.separator + name;
                ContentResolver resolver = mSESceneManager.getGLActivity().getContentResolver();
//                resolver.delete(FileURLInfoColumns.CONTENT_URI,
//                        FileURLInfoColumns.LOCAL_PATH + " = '" + localPath + "'", null);
                String where = ModelColumns.OBJECT_NAME + "='" + name + "'";
                resolver.delete(ModelColumns.CONTENT_URI, where, null);
                where = ObjectInfoColumns.OBJECT_NAME + "='" + name + "'";
                resolver.delete(ObjectInfoColumns.CONTENT_URI, where, null);
                File fileLocal = new File(localPath);
                if (fileLocal.isDirectory()) {
                    for (File f : fileLocal.listFiles()) {
                        f.delete();
                    }
                }
                fileLocal.delete();
            }
        });
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog, Bundle bundle) {
        if (mHadInitedDialog) {
            mHadInitedDialog = false;
            return;
        }
        Activity activity = mSESceneManager.getGLActivity();
        switch (id) {
        case DIALOG_SELECT_APP:
            AlertDialog alertDialog = (AlertDialog) dialog;
            String checkedAppName = null;
            if (mSetBindObject != null) {
                if (mSetBindObject.getObjectInfo() != null) {
                    if (mSetBindObject.getObjectInfo().getCategoryComponentName() != null) {
                        checkedAppName = mSetBindObject.getObjectInfo().getCategoryComponentName().getClassName();
                    }
                }
            }
            mAppList = getAppListSortByName();
            int checkedIndex = -1;
            for (int i = 0; i < mAppList.size(); i++) {
                Item item = mAppList.get(i);
                String clsName = item.mClsName;
                if (!TextUtils.isEmpty(clsName) && clsName.equals(checkedAppName)) {
                    checkedIndex = i;
                    break;
                }
            }

            ArrayAdapter<Item> adapter = new ArrayAdapter<Item>(activity, R.layout.select_dialog_single_holo,
                    android.R.id.text1, mAppList) {
                public View getView(int position, View convertView, ViewGroup parent) {
                    // User super class to create the View
                    View v = super.getView(position, convertView, parent);
                    TextView tv = (TextView) v.findViewById(android.R.id.text1);
                    // Put the image on the TextView
                    Drawable icon = mAppList.get(position).mIcon;
                    icon.setBounds(0, 0, 48, 48);
                    tv.setCompoundDrawables(icon, null, null, null);
                    // Add margin between image and text (support various screen
                    // densities)
                    int dp5 = (int) (20 * mContext.getResources().getDisplayMetrics().density + 0.5f);
                    tv.setCompoundDrawablePadding(dp5);
                    return v;
                }
            };
            alertDialog.getListView().setAdapter(adapter);
            alertDialog.getListView().setItemChecked(checkedIndex, true);
            alertDialog.getListView().setSelection(checkedIndex);
            break;
        case DIALOG_DELETE_OBJECTS:
            AlertDialog deleteDialog = (AlertDialog) dialog;
            String message = activity.getResources().getString(
                    R.string.confirm_delete_selected_objects, mDeleteObjName);
            deleteDialog.setMessage(message);
            break;
        case DIALOG_OBJECT_LONG_CLICK:
        	closeFloatView();
            prepareObjectDlg(dialog, bundle);
            break;
        }
    }

    private void closeFloatView() {
    	Activity activity = SESceneManager.getInstance().getGLActivity();
        if(activity instanceof HomeActivity) {
        	((HomeActivity)activity).closeFloatViews();
        }
    }
    private void setButtonProperties(Context context, Button button, Drawable icon, int textId) {
        button.setEllipsize(TruncateAt.END);
        button.setWidth(LayoutParams.MATCH_PARENT);
        button.setHeight(LayoutParams.MATCH_PARENT);
        button.setSingleLine();
        button.setEllipsize(TruncateAt.END);
        button.setBackgroundResource(R.drawable.long_click_object_bg);
        button.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
        button.setText(textId);
        button.setTextAppearance(context, R.style.object_dialog_button_style);
    }
    
    private void prepareObjectDlg(final Dialog dialog, Bundle bundle) {
        Context context = mSESceneManager.getGLActivity();
        boolean reversal = bundle.getBoolean("reversal");
        int x = bundle.getInt("dlgx");
        int y = bundle.getInt("dlgy");
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);

        Button deleteBtn = new Button(context);
        Drawable iconDelete = context.getResources().getDrawable(R.drawable.long_click_object_delete);
        setButtonProperties(context, deleteBtn, iconDelete, R.string.long_click_object_menu_delete);
        deleteBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                new SECommand(HomeScene.this) {
                    @Override
                    public void run() {
                        if((mSelectedObject instanceof  NormalObject) && (mSelectedObject.getParent() instanceof House)) {
                            NormalObject normalObject = (NormalObject) mSelectedObject;
                            int wallIndex = normalObject.getObjectSlot().mSlotIndex;
                            House house = (House ) mSelectedObject.getParent();
                            WallShelf shelf = house.getWallShelfWithObject(wallIndex, normalObject);
                            if(shelf != null) {
                                int mpIndex = normalObject.getObjectSlot().mMountPointIndex;
                                house.removeObjectFromCurrentShelf(normalObject, mpIndex, shelf);
                            }
                        }
                        if(mSelectedObject instanceof  WallShelf) {
                            WallShelf shelf = (WallShelf)mSelectedObject;
                            shelf.removeAllObjectOnShelfFromParent();

                        }
                        mSelectedObject.getParent().removeChild(mSelectedObject, true);

                    }
                }.execute();
            }
        });
        layout.addView(deleteBtn);
        if (!"fangdajing".equals(mSelectedObject.mName)) {
            if (mSelectedObject.canChangeBind()) {
                Button changeBindBtn = new Button(context);
                Drawable iconBind = context.getResources().getDrawable(R.drawable.long_click_object_changebind);
                setButtonProperties(context, changeBindBtn, iconBind, R.string.long_click_object_menu_change_bind);
                changeBindBtn.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        handleMessage(HomeScene.MSG_TYPE_SHOW_BIND_APP_DIALOG, mSelectedObject);
                    }
                });
                layout.addView(changeBindBtn);

            }
        }
//        if (mSelectedObject.canChangeIcon()) {
//            Button changeIconBtn = new Button(context);
//            Drawable iconChangeIcon = context.getResources().getDrawable(R.drawable.long_click_object_changeicon);
//            setButtonProperties(context, changeIconBtn, iconChangeIcon, R.string.long_click_object_menu_change_icon);
//            changeIconBtn.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    dialog.dismiss();
//                    showDialog(DIALOG_OBJECT_CHANGE_ICON);
//                }
//            });
//            layout.addView(changeIconBtn);
//        }
//        if (mSelectedObject.canChangeLable()) {
//            Button changeLabelBtn = new Button(context);
//            Drawable iconBind = context.getResources().getDrawable(R.drawable.long_click_object_changelabel);
//            setButtonProperties(context, changeLabelBtn, iconBind, R.string.long_click_object_menu_change_label);
//            changeLabelBtn.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    dialog.dismiss();
//                    showDialog(DIALOG_OBJECT_CHANGE_LABLE);
//                }
//            });
//            layout.addView(changeLabelBtn);
//        }
        
        if (mSelectedObject.canUninstall()) {
            Button uninstallBtn = new Button(context);
            Drawable iconUninstall = context.getResources().getDrawable(R.drawable.long_click_object_uninstall);
            setButtonProperties(context, uninstallBtn, iconUninstall, R.string.long_click_object_menu_uninstall);
            uninstallBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    if (mSelectedObject.getObjectInfo().isDownloadObj()) {
                        handleMessage(HomeScene.MSG_TYPE_SHOW_DELETE_OBJECTS, mSelectedObject.mName);
                    } else {
                        ComponentName componentName = mSelectedObject.getObjectInfo().mComponentName;
                        if (componentName == null) {
                            return;
                        }
                        String packageName = componentName.getPackageName();
                        String className = componentName.getClassName();
                        Intent intent = new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName,
                                className));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        SESceneManager.getInstance().startActivity(intent);
                    }
                }
            });
            layout.addView(uninstallBtn);

        }
//        if (mSelectedObject.canBeResized()) {
//            Button resizeBtn = new Button(context);
//            Drawable iconResize = context.getResources().getDrawable(R.drawable.long_click_object_resize);
//            setButtonProperties(context, resizeBtn, iconResize, R.string.long_click_object_menu_resize);
//            resizeBtn.setOnClickListener(new OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    dialog.dismiss();
//                    new SECommand(SE3DHomeScene.this) {
//                        @Override
//                        public void run() {
//                            mSelectedObject.startResize();
//                        }
//                    }.execute();
//                }
//            });
//            layout.addView(resizeBtn);
//        }
        layout.measure(0, 0);
        dialog.setContentView(layout);
        Window window = dialog.getWindow();
        window.setGravity(Gravity.LEFT | Gravity.TOP);
        LayoutParams params = window.getAttributes();

        int startX = mSelectedObject.getObjectSlot().mStartX;
        int btnWidth = deleteBtn.getMeasuredWidth();
        int childCount = layout.getChildCount();
        int cellCountX = mSESceneManager.mCellCountX;
        if (mSelectedObject.getObjectInfo().mSlotType == ObjectInfo.SLOT_TYPE_APP_WALL ||
                mSelectedObject.getObjectInfo().mSlotType == ObjectInfo.SLOT_TYPE_WALL ||
                mSelectedObject.getObjectInfo().mSlotType == ObjectInfo.SLOT_TYPE_WALL_SHELF) {
            if (startX == 0) {
                x -= btnWidth * 0.5;
                layout.getChildAt(0).setBackgroundResource(R.drawable.long_click_object_bg_down);
            } else if (startX < cellCountX / 2) {
                if (childCount == 1) {
                    x -= btnWidth * 0.5;
                    layout.getChildAt(0).setBackgroundResource(R.drawable.long_click_object_bg_down); 
                } else {
                    x -= btnWidth * 1.5;
                    layout.getChildAt(1).setBackgroundResource(R.drawable.long_click_object_bg_down);
                }
            } else if ( startX >= cellCountX / 2 && startX != (cellCountX -1)) {
                if (childCount == 1) {
                    x -= btnWidth * 0.5;
                    layout.getChildAt(0).setBackgroundResource(R.drawable.long_click_object_bg_down);
                } else {
                    x -= (childCount - 1.5) * btnWidth;
                    layout.getChildAt(childCount - 2).setBackgroundResource(R.drawable.long_click_object_bg_down);
                }
            } else if (startX == cellCountX - 1) {
                x -= (childCount - 0.5) * btnWidth;
                layout.getChildAt(childCount - 1).setBackgroundResource(R.drawable.long_click_object_bg_down);
            }
        } else if (mSelectedObject.getObjectInfo().mSlotType == ObjectInfo.SLOT_TYPE_DESKTOP) {
            if (x > mSESceneManager.getWidth() / 2 + 30 ) {
                x -= (childCount - 0.5) * btnWidth;
                layout.getChildAt(childCount - 1).setBackgroundResource(R.drawable.long_click_object_bg_down);
            } else if ((x > mSESceneManager.getWidth() / 2 - 30) && (x < mSESceneManager.getWidth() / 2 + 30)){
                //Icon's location is almost center.
                if (childCount == 1) {
                    x -= btnWidth * 0.5;
                    layout.getChildAt(0).setBackgroundResource(R.drawable.long_click_object_bg_down);
                } else {
                    x -= btnWidth * 1.5;
                    layout.getChildAt(1).setBackgroundResource(R.drawable.long_click_object_bg_down);
                }
            } else {
                x -= btnWidth * 0.5;
                layout.getChildAt(0).setBackgroundResource(R.drawable.long_click_object_bg_down);
            }
        } else if (mSelectedObject.getObjectInfo().mSlotType == ObjectInfo.SLOT_TYPE_WALL_GAP) {
            if (reversal) {
                x -= (childCount - 0.5) * btnWidth;
                layout.getChildAt(childCount - 1).setBackgroundResource(R.drawable.long_click_object_bg_down);
            } else {
                x -= btnWidth * 0.5;
                layout.getChildAt(0).setBackgroundResource(R.drawable.long_click_object_bg_down);
            }
        }
        params.x = x;
        params.y = y;
        window.setAttributes(params);
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

    private List<Item> getAppListSortByName() {
        List<AppItemInfo> apps = LauncherModel.getInstance().getApps();
        List<Item> items = new ArrayList<Item>();
        for (ItemInfo info : apps) {
            ResolveInfo resolveInfo = info.getResolveInfo();
            if (resolveInfo != null) {
                String pkg = resolveInfo.activityInfo.packageName;
                String cls = resolveInfo.activityInfo.name;
                Item item = new Item(info.getLabel(), info.getIcon(), pkg, cls);
                items.add(item);
            }
        }

        return items;
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

    public boolean dispatchEventToOptionMenu(MotionEvent event) {
//        if (mOptionMenu != null) {
//            return mOptionMenu.dispatchTouchEvent(event);
//        }
        return false;
    }

    public void onAddModelToDB(final ModelInfo modelInfo) {
        mSceneInfo.mModels.put(modelInfo.mName, modelInfo);
    }

    @Override
    public void onRemoveModelToDB(final String modelName) {
        final ModelInfo modelInfo = mSceneInfo.mModels.get(modelName);
        if (modelInfo == null) {
            return;
        }
        mSceneInfo.mModels.remove(modelName);
        new SECommand(this) {
            public void run() {
                int size = modelInfo.getInstances().size();
                /*
                 * remove all instances from house
                 */
                for (int i = 0; i < size; i++) {
                    NormalObject delObj = (NormalObject) modelInfo.getInstances().get(i);
                    SEObject parent = delObj.getParent();
                    parent.removeChild(delObj, true);
                    size--;
                    i--;
                }
            }
        }.execute();
    }

    @Override
    public void unlockScreen() {
        boolean disable = getStatus(SEScene.STATUS_APP_MENU) | getStatus(SEScene.STATUS_HELPER_MENU)
                | getStatus(SEScene.STATUS_OPTION_MENU) | getStatus(SEScene.STATUS_OBJ_MENU)
                | getStatus(SEScene.STATUS_ON_SKY_SIGHT) | getStatus(SEScene.STATUS_ON_WIDGET_SIGHT)
                | getStatus(SEScene.STATUS_ON_WALL_DIALOG);
        if (!disable) {
            if (!getStatus(SEScene.STATUS_ON_DESK_SIGHT)) {
                new SECommand(this) {
                    public void run() {
                        getCamera().playUnlockScreenAnimation(40, null);
                    }
                }.execute();
            }
        }
    }

    @Override
    public void delay() {
        new SECommand(this) {
            public void run() {
                if (mDelayFrame != null) {
                    final ArrayList<SEObject> needHideObjs = new ArrayList<SEObject>();
                    setStatus(STATUS_DISALLOW_TOUCH, true);
                    SEObjectGroup root = (SEObjectGroup) getContentObject();
                    for (SEObject child : root.mChildObjects) {
                        if (child.isVisible() && child != mDelayFrame) {
                            needHideObjs.add(child);
                            child.setVisible(false, true);
                        }
                    }
                    mDelayFrame.show(null, true);
                    LauncherModel.getInstance().wait(new Runnable() {
                        public void run() {
                            new SECommand(HomeScene.this) {
                                public void run() {
                                    setStatus(STATUS_DISALLOW_TOUCH, false);
                                    for (SEObject child : needHideObjs) {
                                        child.setVisible(true, true);
                                    }
                                    mDelayFrame.show(null, false);
                                }
                            }.execute();
                        }
                    }, 400);
                }
            }
        }.execute();
    }


    /**
     * Create screen indicator and add it to the root node.
     */
    private void initScreenIndicator() {
//        if (SettingsActivity.isScreenIndicatorEnabled(getContext())) {
//            SESceneManager.getInstance().runInUIThread(new Runnable() {
//                @Override
//                public void run() {
//                    mIndicator = new IconPageIndicator(getContext());
//                    mIndicator.setCount(mHouseObject.getCount());
//                    mIndicator.setCurrentItem(mHouseObject.getCurrentFaceIndex());
//                    mHouseObject.addWallChangedListener(mIndicator);
//                    SESceneManager.getInstance().getWorkSpace().addView(mIndicator);
//
//                    final int size = getContext().getResources().getDimensionPixelSize(R.dimen.screen_indicator_size);
//                    mIndicator.getLayoutParams().height = size;
//                    ((FrameLayout.LayoutParams)mIndicator.getLayoutParams()).gravity = Gravity.BOTTOM;
//                    mIndicator.setVisibility(View.GONE);
//                }
//            });
//        }
    }

	@Override
	public void onLanguageChanged() {
		if (mOptionMenu != null) {
            mOptionMenu.dismiss();
            mOptionMenu = null;

            mOptionMenu = new OptionMenu(getContext());
        }		
	}

//    public void calculateCameraRadiusScope() {
//        final float bestRadius = mSECamera.getBestCameraFov(mSESceneManager.getWidth(), mSESceneManager.getHeight());
//        mSceneInfo.mSECameraData.mBestCameraFov = bestRadius;
//
//        if (null != mHouseObject) {
//            float cameraToWallDistance = mSECamera.getRadius() + mHouseObject.getWallRadius();
//            float wallSpan = mHouseObject.getWallUnitSizeX() * mHouseObject.getWallSpanX();
//            final float minRadius = (float) (Math.atan(wallSpan * 0.5 / cameraToWallDistance) * 360 / Math.PI + 5);
//            final float maxRadius = (float) (Math.atan(wallSpan / cameraToWallDistance) * 360 / Math.PI);
//            if (HomeUtils.DEBUG) {
//                Log.d(HomeUtils.TAG, "calculateCameraRadiusScope, minRadius = " + minRadius +
//                        ", maxRadius = " + maxRadius + ", bestRadius = " + bestRadius);
//            }
//            mSceneInfo.mSECameraData.mMinCameraRadius = Math.min(minRadius, bestRadius);
//            mSceneInfo.mSECameraData.mMaxCameraRadius = Math.max(maxRadius, bestRadius);
//        }
//    }
}
