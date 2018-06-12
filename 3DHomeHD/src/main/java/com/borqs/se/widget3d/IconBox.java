package com.borqs.se.widget3d;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.ToastUtils;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SEScene;
import com.borqs.se.home3d.HomeActivity;
import com.borqs.se.home3d.HomeScene;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.engine.SEBitmap;
import com.borqs.se.engine.SEBitmap.Type;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SESceneManager;

public class IconBox extends NormalObject {
    private String mImageName = "iconbox01_fhq.jpg@iconbox_basedata.cbf";
    private int mIconDpi;
    private PackageManager mPackageManager;

    public IconBox(SEScene scene, String name, int index) {
        super(scene, name, index);
        mPackageManager = getContext().getPackageManager();
        int density = getContext().getResources().getDisplayMetrics().densityDpi;
        if (HomeActivity.isScreenLarge()) {
            if (density == DisplayMetrics.DENSITY_LOW) {
                mIconDpi = DisplayMetrics.DENSITY_MEDIUM;
            } else if (density == DisplayMetrics.DENSITY_MEDIUM) {
                mIconDpi = DisplayMetrics.DENSITY_HIGH;
            } else if (density == DisplayMetrics.DENSITY_HIGH) {
                mIconDpi = DisplayMetrics.DENSITY_XHIGH;
            } else if (density == DisplayMetrics.DENSITY_XHIGH) {
                // We'll need to use a denser icon, or some sort of a mipmap
                mIconDpi = DisplayMetrics.DENSITY_XHIGH;
            }
        }
    }

    public AppObject changeToAppIcon() {

        if (getChangedToObj() == null) {
            if (getObjectInfo().isShortcut()) {
                ObjectInfo info = new ObjectInfo();
                info.mName = AppObject.generateShortcutName();
                info.mSceneName = getScene().mSceneName;
                info.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
                info.mObjectSlot.mSpanX = 1;
                info.mObjectSlot.mSpanY = 1;
                info.mComponentName = getObjectInfo().mComponentName;
                info.mClassName = ShortcutObject.class.getName();
                info.mShortcutUrl = getObjectInfo().mShortcutUrl;
                info.mShortcutIcon = getObjectInfo().getShortcutIcon(Type.needSaveToDB);
                info.mDisplayName = getObjectInfo().mDisplayName;
                info.mType = ModelInfo.Type.SHORTCUT_ICON;
                final ShortcutObject shortcutObj = new ShortcutObject(getScene(), info.mName, info.mIndex);
                shortcutObj.setObjectInfo(info);
                NormalObject parent = (NormalObject) getParent();
                parent.addChild(shortcutObj, false);
                shortcutObj.load(parent, null);
                shortcutObj.initStatus(getScene());

                copeStatusTo(shortcutObj);
                shortcutObj.setChangedToObj(this);
                setChangedToObj(shortcutObj);
            } else {
                ObjectInfo info = new ObjectInfo();
                info.mName = AppObject.generateAppName();
                info.mSceneName = getScene().mSceneName;
                info.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
                info.mObjectSlot.mSpanX = 1;
                info.mObjectSlot.mSpanY = 1;
                info.mComponentName = getObjectInfo().mComponentName;
                info.mClassName = AppObject.class.getName();
                info.mType = ModelInfo.Type.APP_ICON;
                final AppObject appObject = new AppObject(getScene(), info.mName, info.mIndex);
                appObject.setObjectInfo(info);
                NormalObject parent = (NormalObject) getParent();
                parent.addChild(appObject, false);
                appObject.load(parent, null);
                appObject.initStatus(getScene());

                copeStatusTo(appObject);
                appObject.setChangedToObj(this);
                setChangedToObj(appObject);
            }
        }
        getChangedToObj().setTouch(getTouchX(), getTouchY());
        getChangedToObj().setVisible(true, true);
        getChangedToObj().setPressed(true);
        setVisible(false, true);
        setPressed(false);
        getScene().changeTouchDelegate(getChangedToObj());

        return (AppObject)getChangedToObj();
    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        mImageName = "iconbox01_fhq.jpg@iconbox_basedata.cbf#" + mIndex;
        if (getObjectInfo().mComponentName != null) {
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    try {
                        InputStream is = getContext().getAssets().open("base/iconbox/iconbox01_fhq.jpg");
                        Bitmap background = BitmapFactory.decodeStream(is).copy(Config.RGB_565, true);
                        is.close();
                        Canvas canvas = new Canvas(background);
                        if (getObjectInfo().mShortcutIcon != null) {
                            Rect iconSrc = new Rect(0, 0, getObjectInfo().mShortcutIcon.getWidth(), getObjectInfo().mShortcutIcon.getHeight());
                            Rect iconDes = new Rect(28, 28, 228, 228);
                            canvas.drawBitmap(getObjectInfo().mShortcutIcon.getBitmap(), iconSrc, iconDes, new Paint());
                        } else {
                            ResolveInfo resolveInfo = HomeUtils.findResolveInfoByComponent(getContext(),
                                    getObjectInfo().mComponentName);
                            if (resolveInfo == null) {
                                getObjectInfo().mComponentName = null;
                                new SECommand(getScene()) {
                                    public void run() {
                                        getParent().removeChild(IconBox.this, true);
                                    }
                                }.execute();
                                return;
                            }
                            Drawable icon = getFullResIcon(resolveInfo, mPackageManager);
                            if (icon == null) {
                                icon = getIcon(getContext(), resolveInfo);
                            }
                            Rect oldBounds = icon.copyBounds();
                            icon.setBounds(28, 28, 228, 228);
                            icon.draw(canvas);
                            icon.setBounds(oldBounds);
                        }
                        final int imageData = SEObject.loadImageData_JNI(background);
                        background.recycle();
                        new SECommand(getScene()) {
                            public void run() {
                                SEObject.applyImage_JNI(mImageName, mImageName);
                                SEObject.addImageData_JNI(mImageName, imageData);
                            }
                        }.execute();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
            
            if (!AppObject.isSysApp(getObjectInfo().mComponentName)) {
                setCanUninstall(true);
            } else {
                setCanUninstall(false);
            }
        }
    }

    @Override
    public void updateComponentName(ComponentName name) {
        super.updateComponentName(name);
        if (getObjectInfo().mComponentName != null) {
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    try {
                        InputStream is = getContext().getAssets().open("base/iconbox/iconbox01_fhq.jpg");
                        final Bitmap background = BitmapFactory.decodeStream(is).copy(Config.RGB_565, true);
                        is.close();
                        Canvas canvas = new Canvas(background);
                        ResolveInfo resolveInfo = HomeUtils.findResolveInfoByComponent(getContext(),
                                getObjectInfo().mComponentName);
                        if (resolveInfo == null) {
                            getObjectInfo().mComponentName = null;
                            return;
                        }
                        Drawable icon = getFullResIcon(resolveInfo, mPackageManager);
                        if (icon == null) {
                            icon = getIcon(getContext(), resolveInfo);
                        }
                        Rect oldBounds = icon.copyBounds();
                        icon.setBounds(28, 28, 228, 228);
                        icon.draw(canvas);
                        icon.setBounds(oldBounds);
                        final int imageData = SEObject.loadImageData_JNI(background);
                        new SECommand(SESceneManager.getInstance().getCurrentScene()) {
                            public void run() {
                                SEObject.applyImage_JNI(mImageName, mImageName);
                                SEObject.addImageData_JNI(mImageName, imageData);
                                background.recycle();
                            }
                        }.execute();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
            
            if (!AppObject.isSysApp(getObjectInfo().mComponentName)) {
                setCanUninstall(true);
            } else {
                setCanUninstall(false);
            }
        }

    }

    private static Drawable getIcon(Context context, ResolveInfo resolveInfo) {
        Drawable icon = resolveInfo.loadIcon(context.getPackageManager());
        if (icon == null) {
            icon = ToastUtils.getDefaultApplicationIcon(context);
        }
        return icon;
    }

    private Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable d;
        try {
            Method method = HomeUtils.getMethod(Resources.class, "getDrawableForDensity", new Class[]{int.class,
                    int.class});
            method.setAccessible(true);
            d = (Drawable) method.invoke(resources, new Object[] { iconId, mIconDpi });
        } catch (Exception e) {
            Log.d("ItemInfo", "error : " + e.getMessage());
            d = null;
        }

        return d;
    }

    private Drawable getFullResIcon(ResolveInfo info, PackageManager packageManager) {
        Resources resources;
        try {
            resources = packageManager.getResourcesForApplication(info.activityInfo.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = info.activityInfo.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return null;
    }

    @Override
    public void onSlotSuccess() {
        NormalObject normalObject = getChangedToObj();
        if (isFresh() && normalObject != null && ModelInfo.Type.SHORTCUT_ICON.equals(normalObject.getObjectInfo().mType)) {
            setIsFresh(false);
            normalObject.setChangedToObj(null);
            normalObject.getParent().removeChild(normalObject, true);
            setChangedToObj(null);
            handleSelectShortcut();
        } else {
            super.onSlotSuccess();
        }
    }

    private void handleSelectShortcut() {
        getScene().setTouchDelegate(this);
        Intent intent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        intent.setComponent(getObjectInfo().mComponentName);
        if (!SESceneManager.getInstance().startActivityForResult(intent, HomeScene.REQUEST_CODE_SELECT_SHORTCUT)) {
            getScene().removeTouchDelegate();
            handleActivityNotFound();
        }
    }

    private void handleActivityNotFound() {
        ToastUtils.showActivityNotFound();
    }

    public void updateShortcut(Intent data) {
        final String displayName = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Intent shortIntent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        Bitmap icon = null;
        Parcelable bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
        if (bitmap != null && bitmap instanceof Bitmap) {
            icon = (Bitmap) bitmap;
        } else {
            Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (extra != null && extra instanceof ShortcutIconResource) {
                try {
                    ShortcutIconResource iconResource = (ShortcutIconResource) extra;
                    Resources resources = getContext().getPackageManager().getResourcesForApplication(
                            iconResource.packageName);
                    int id = resources.getIdentifier(iconResource.resourceName, null, null);
                    icon = BitmapFactory.decodeResource(resources, id);
                } catch (Exception e) {
                }
            }
        }
        getObjectInfo().mShortcutUrl = shortIntent.toURI();
        final Bitmap shortcutIcon = icon;
        SELoadResThread.getInstance().process(new Runnable() {
            public void run() {
                ResolveInfo resolveInfo = HomeUtils.findResolveInfoByComponent(getContext(),
                        getObjectInfo().mComponentName);
                if (resolveInfo == null) {
                    getObjectInfo().mComponentName = null;
                    new SECommand(getScene()) {
                        public void run() {
                            getParent().removeChild(IconBox.this, true);
                        }
                    }.execute();
                    return;
                }
                String label = displayName;
                if (TextUtils.isEmpty(label)) {
                    PackageManager pm = getContext().getPackageManager();
                    label = resolveInfo.loadLabel(pm).toString();
                }
                getObjectInfo().mDisplayName = label;

                try {
                    InputStream is = getContext().getAssets().open("base/iconbox/iconbox01_fhq.jpg");
                    Bitmap background = BitmapFactory.decodeStream(is).copy(Config.RGB_565, true);
                    is.close();
                    Canvas canvas = new Canvas(background);
                    if (shortcutIcon != null) {
                        Rect iconSrc = new Rect(0, 0, shortcutIcon.getWidth(), shortcutIcon.getHeight());
                        Rect iconDes = new Rect(28, 28, 228, 228);
                        canvas.drawBitmap(shortcutIcon, iconSrc, iconDes, new Paint());
                        getObjectInfo().mShortcutIcon = new SEBitmap(shortcutIcon, SEBitmap.Type.normal);
                    } else {

                        Drawable icon = getFullResIcon(resolveInfo, mPackageManager);
                        if (icon == null) {
                            icon = getIcon(getContext(), resolveInfo);
                        }
                        Rect oldBounds = icon.copyBounds();
                        icon.setBounds(28, 28, 228, 228);
                        icon.draw(canvas);
                        icon.setBounds(oldBounds);
                    }
                    getObjectInfo().updateToDB(false);
                    final int imageData = SEObject.loadImageData_JNI(background);
                    background.recycle();
                    new SECommand(getScene()) {
                        public void run() {
                            SEObject.applyImage_JNI(mImageName, mImageName);
                            SEObject.addImageData_JNI(mImageName, imageData);
                        }
                    }.execute();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }
}
