package com.borqs.se.widget3d;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.borqs.framework3d.home3d.HouseObject;
import com.borqs.framework3d.home3d.HouseSceneInfo;
import com.borqs.freehdhome.R;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEVector.SERect3D;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.ToastUtils;
import com.borqs.se.engine.SECommand;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.engine.SEBitmap;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectFactory;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.shortcut.AppItemInfo;
import com.borqs.se.shortcut.AppItemInfo.BitmapInfo;

public class AppObject extends NormalObject {
    private SEObject mBackground;
    public SEObject mIconObject;
    private float mBitmapWidth;
    private float mBitmapHeight;
    private float mBitmapContentWidth;
    private float mBitmapContentHeight;
    public AppObject(SEScene scene, String name, int index) {
        super(scene, name, index);
    }
    public float getBitmapWidth() {
        return mBitmapWidth;
    }
    public float getBitmapHeight() {
        return mBitmapHeight;
    }
    public float getBitmapContentWidth() {
        return mBitmapContentWidth;
    }
    public float getBitmapContentHeight() {
        return mBitmapContentHeight;
    }
    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        SEObject parent = getParent();
        if(parent instanceof WallShelf) {
            SEObject houseObj = parent.getParent();
            changeParent(houseObj);
        }
        se_setNeedBlendSort_JNI(new float[] { 0, 1f, 0 });
        setOnLongClickListener(new SEObject.OnTouchListener() {
            public void run(SEObject obj) {
                if(!canHandleLongClick()) {
                    return;
                }
                SETransParas startTranspara = new SETransParas();
                startTranspara.mTranslate = getAbsoluteTranslate();
                float angle = getUserRotate().getAngle();
                SEObject parent = getParent();
                while (parent != null) {
                    angle = angle + parent.getUserRotate().getAngle();
                    parent = parent.getParent();
                }
                startTranspara.mRotate.set(angle, 0, 0, 1);
                setStartTranspara(startTranspara);
                setOnMove(true);
                hideBackgroud();
            }
        });
        setBlendSortAxis(AXIS.Y);
        if (!isSysApp(getObjectInfo().mComponentName)) {
            setCanUninstall(true);
        } else {
            setCanUninstall(false);
        }
        setCanChangeBind(false);
    }

    @Override
    public void showBackgroud() {
        if (mBackground == null) {
            int index = (int) System.currentTimeMillis();
            ModelInfo modelInfo = getScene().mSceneInfo.findModelInfo("IconBackground");
            mBackground = new SEObject(getScene(), "IconBackground", index);
            modelInfo.cloneMenuItemInstance(this, index, false, modelInfo.mStatus);
            addChild(mBackground, false);
            mBackground.getUserTransParas().set(getBackgroundLocation());
            mBackground.setUserTransParas();
            mBackground.se_setNeedBlendSort_JNI(new float[] { 0, 1, 0 });
        } 
        if ("none".equals(SettingsActivity.getAppIconBackgroundName(getContext()))) {
            mBackground.setVisible(false, true);
            if (mIconObject != null) {
                mIconObject.setScale(new SEVector3f(1, 1, 1), true);
            }
        } else {
            mBackground.setVisible(true, true);
            if (mIconObject != null) {
                mIconObject.setScale(new SEVector3f(0.85f, 0.85f, 0.85f), true);
            }
        }

    }

    @Override
    public void hideBackgroud() {
        if (mBackground != null) {
            removeChild(mBackground, true);
            mBackground = null;
        }
        if (mIconObject != null) {
            mIconObject.setScale(new SEVector3f(1, 1, 1), true);
        }
    }

    private SETransParas getBackgroundLocation() {
        SETransParas transparas = new SETransParas();
        transparas.mTranslate.set(-9, HomeUtils.ICON_BACKGROUND_SPACING, 0);
        int spanX = getObjectSlot().mSpanX;
        int spanY = getObjectSlot().mSpanY;
        transparas.mScale.set(spanX, 1, spanY * 0.85f);
        return transparas;
    }

    public Folder changeToFolder() {
        ModelInfo modelInfo = getScene().mSceneInfo.findModelInfo("woodfolderclose");
        final ObjectInfo objInfo = new ObjectInfo();
        objInfo.setModelInfo(modelInfo);
        objInfo.mIndex = (int) System.currentTimeMillis();
        objInfo.mSceneName = getScene().mSceneName;
        NormalObject parent = (NormalObject) getParent();
        objInfo.mObjectSlot.set(getObjectSlot());
        Folder folder = (Folder) HomeUtils.getObjectByClassName(getScene(), objInfo);
        parent.addChild(folder, false);
        modelInfo.register(folder);
        modelInfo.cloneMenuItemInstance(parent, objInfo.mIndex, false, modelInfo.mStatus);
        folder.initStatus(getScene());
        changeParent(folder);
        mIconObject.setScale(new SEVector3f(1f, 1f, 1f), true);
        SETransParas transParas = folder.getFirstPosition();
        getUserTransParas().set(transParas);
        setUserTransParas();
        setIsEntirety_JNI(false);
        return folder;
    }

    public IconBox changeToIconBox() {

        if (getChangedToObj() == null) {
            ModelInfo modelInfo = getScene().mSceneInfo.findModelInfo("group_iconbox");
            final ObjectInfo objInfo = new ObjectInfo();
            objInfo.setModelInfo(modelInfo);
            objInfo.mIndex = (int)System.currentTimeMillis();
            objInfo.mSceneName = getScene().mSceneName;
            objInfo.mComponentName = getObjectInfo().mComponentName;
            objInfo.mDisplayName = getObjectInfo().mDisplayName;
            objInfo.mSlotType = getObjectInfo().mSlotType;
            NormalObject parent = (NormalObject) getParent();
            objInfo.mObjectSlot.set(getObjectSlot());
            final IconBox iconBox = (IconBox) HomeUtils.getObjectByClassName(getScene(), objInfo);
            parent.addChild(iconBox, false);
            modelInfo.register(this);
            modelInfo.cloneMenuItemInstance(parent, objInfo.mIndex, false, objInfo.mModelInfo.mStatus);
            iconBox.initStatus(getScene());

            copeStatusTo(iconBox);
            iconBox.setChangedToObj(this);
            iconBox.setBeginLayer(this.getBeginLayer());
            setChangedToObj(iconBox);
        }
        getChangedToObj().setTouch(getTouchX(), getTouchY());
        getChangedToObj().setVisible(true, true);
        getChangedToObj().setPressed(true);
        setVisible(false, true);
        setPressed(false);
        getScene().changeTouchDelegate(getChangedToObj());
        return (IconBox)getChangedToObj();
    }

    @Override
    public void onRenderFinish(SECamera camera) {
        super.onRenderFinish(camera);
        if (mIconObject != null) {
            addChild(mIconObject, true);
            mIconObject.setIsEntirety_JNI(false);
        }
    }
    public void setBitmapSize(final float bitmapWidth, final float bitmapHeight, final float bitmapContentWidth, final float bitmapContentHeight) {
        mBitmapWidth = bitmapWidth;
        mBitmapHeight = bitmapHeight;
        mBitmapContentHeight = bitmapContentHeight;
        mBitmapContentWidth = bitmapContentWidth;
    }
    @Override
    public boolean load(final SEObject parent, final Runnable finish) {
        SELoadResThread.getInstance().process(new Runnable() {
            public void run() {
                ResolveInfo resolveInfo = getObjectInfo().getResolveInfo();
//                HouseObject houseObject = ModelInfo.getHouseObject(getScene());
//                int w = (int) houseObject.getWallUnitSizeX() * getObjectInfo().getSpanX();
//                int h = (int) houseObject.getWallUnitSizeY() * getObjectInfo().getSpanY();
                int w = getAppIconWidth(getContext());
                int h = w;
                //Bitmap iconwithText;
                BitmapInfo bitmapInfo;
                int bitmapW = 128;
                int bitmapH = 128;
                if (resolveInfo == null) {
                	Bitmap icon ; 
                	if(getObjectInfo().mComponentName != null && HomeUtils.LOCKSCREEN_HOMEHD_PKG.equals(getObjectInfo().mComponentName.getPackageName())) {
                		//TODO
                			icon = ToastUtils.decodeLockScreenHdIcon(bitmapW, bitmapH, R.drawable.lockscreenhd_icon);
                	}else {
                		icon = ToastUtils.decodeDefaultApplicationIcon(bitmapW, bitmapH);
                	}
                    bitmapInfo = new BitmapInfo();
                    bitmapInfo.bitmap = icon;
                    bitmapInfo.bitmapWidth = icon.getWidth();
                    bitmapInfo.bitmapHeight = icon.getHeight();
                    bitmapInfo.bitmapContentWidth = icon.getWidth();
                    bitmapInfo.bitmapContentHeight = icon.getHeight();
                    /*
                    if (getObjectInfo().mDisplayName != null) {
                        iconwithText = AppItemInfo.getBitmapWithText(icon, getObjectInfo().mDisplayName, w, h);
                        icon.recycle();
                    } else {
                        iconwithText = icon;
                    }
                    */
                } else {
                    //iconwithText = AppItemInfo.getBitmap(resolveInfo, w, h);
                	bitmapInfo = AppItemInfo.getBitmap(resolveInfo);
                }

                String imageName = getObjectInfo().mName + "_imageName";
                String imageKey = getObjectInfo().mName + "_imageKey";
                SEBitmap bp = new SEBitmap(bitmapInfo.bitmap, SEBitmap.Type.normal);
                SERect3D rect = new SERect3D(w, h);
                mIconObject = new SEObject(getScene(), mName + "_icon");
                SEObjectFactory.createRectangle(mIconObject, rect, imageName, imageKey, bp);
                float scale = 1;
                if (bitmapW > 128) {
                    scale = 128f / bitmapW;
                    bitmapH = (int) (bitmapH * scale);
                    bitmapW = 128;
                }
                mIconObject.setImageSize(bitmapW, bitmapH);
                final float currentBitmapWidth = bitmapInfo.bitmapWidth;
                final float currentBitmapHeight = bitmapInfo.bitmapHeight;
                final float currentBitmapContentWidth = bitmapInfo.bitmapContentWidth;
                final float currentBitmapContentHeight = bitmapInfo.bitmapContentHeight;
                new SECommand(getScene()) {
                    public void run() {
                        render();
                        setBitmapSize(currentBitmapWidth, currentBitmapHeight, currentBitmapContentWidth, currentBitmapContentHeight);
                        initStatus(getScene());
                        setHasInit(true);
                        if (finish != null) {
                            finish.run();
                        }
                    }
                }.execute();
            }
        });
        return true;
    }

    private static final String APP_NAME_PREFIX = "app_";
    private static final String SHORTCUT_NAME_PREFIX = "shortcut_";
    public static boolean isValidAppName(String name) {
        return null != name && name.startsWith(APP_NAME_PREFIX);
    }
    public static boolean isValidShortcutName(String name) {
        return null != name && name.startsWith(SHORTCUT_NAME_PREFIX);
    }

    public static String generateAppName() {
        return APP_NAME_PREFIX + System.currentTimeMillis();
    }

    public static String generateShortcutName() {
        return SHORTCUT_NAME_PREFIX + System.currentTimeMillis();
    }

    public static AppObject create(SEScene scene, AppItemInfo itemInfo) {
//        AppItemInfo itemInfo = (AppItemInfo) preViewObject.getItemInfo();
        ObjectInfo info = new ObjectInfo();
        info.mName = generateAppName();
        info.mSceneName = scene.mSceneName;
        info.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
        info.mObjectSlot.mSpanX = itemInfo.getSpanX();
        info.mObjectSlot.mSpanY = itemInfo.getSpanY();
        info.mComponentName = itemInfo.getComponentName();
        info.mClassName = AppObject.class.getName();
        info.mType = ModelInfo.Type.APP_ICON;
        Context context = SESceneManager.getInstance().getContext();
        PackageManager pm = context.getPackageManager();
        info.mDisplayName = itemInfo.getResolveInfo().loadLabel(pm).toString();
        AppObject appObject = new AppObject(scene, info.mName, info.mIndex);
        appObject.setIsFresh(true);
//        HouseObject houseObject = ModelInfo.getHouseObject(scene);
//        int w = houseObject.getWallUnitSizeX() * info.getSpanX();
//        int h = houseObject.getWallUnitSizeY() * info.getSpanY();
        int w = getAppIconWidth(context);
        int h = w;
        
        //Bitmap image = AppItemInfo.getBitmap(itemInfo.getResolveInfo(), w, h);
        AppItemInfo.BitmapInfo bitmapInfo = AppItemInfo.getBitmap(itemInfo.getResolveInfo());
        String imageName = info.mName + "_imageName";
        String imageKey = info.mName + "_imageKey";
        SEBitmap bp = new SEBitmap(bitmapInfo.bitmap, SEBitmap.Type.normal);
        SERect3D rect = new SERect3D(w, h);
        appObject.mIconObject = new SEObject(scene, info.mName + "_icon");
        SEObjectFactory.createRectangle(appObject.mIconObject, rect, imageName, imageKey, bp);
        int bitmapW = bitmapInfo.bitmapWidth;
        int bitmapH = bitmapInfo.bitmapHeight;
        float scale = 1;
        if (bitmapW > 128) {
            scale = 128f / bitmapW;
            bitmapH = (int) (bitmapH * scale);
            bitmapW = 128;
        }
        appObject.setBitmapSize(bitmapInfo.bitmapWidth, bitmapInfo.bitmapHeight, bitmapInfo.bitmapContentWidth, bitmapInfo.bitmapContentHeight);
        appObject.mIconObject.setImageSize(bitmapW, bitmapH);
        appObject.setObjectInfo(info);
        info.saveToDB();
        return appObject;
    }

    @Override
    public void handOnClick() {
        Intent intent = getObjectInfo().getIntent();
        if (intent != null) {
            if (!SESceneManager.getInstance().startActivity(intent)) {
                if (HomeUtils.DEBUG)
                    Log.e(HomeUtils.TAG, "not found bind activity");
            }
        }
    }

    @Override
    public void handleOutsideRoom() {
        if (isFresh()) {
            if (getObjectInfo().mComponentName != null) {
                startApplicationUninstallActivity(getObjectInfo().mComponentName);
            }
            setIsFresh(false);
        }
        super.handleOutsideRoom();
    }

    @Override
    public void onSlotSuccess() {
        setIsFresh(false);
        super.onSlotSuccess();
        NormalObject parent = (NormalObject)getParent();
        if (ModelInfo.isHouseVesselObject(parent)) {
            showBackgroud();
        } else {
            hideBackgroud();
        }
    }

    @Override
    public boolean update(SEScene scene) {
        ObjectInfo info = getObjectInfo();
        ResolveInfo resolveInfo = HomeUtils.findResolveInfoByComponent(scene.getContext(), info.mComponentName);
        if (resolveInfo == null) {
            return false;
        }
//        HouseObject houseObject = ModelInfo.getHouseObject(getScene());
//        int w = getAppIconWidth(getContext());
//        int h = (int) houseObject.getWallUnitSizeY() * info.getSpanY();
//        final Bitmap icon = AppItemInfo.getBitmap(resolveInfo, w, h);
        final AppItemInfo.BitmapInfo bitmapInfo = AppItemInfo.getBitmap(resolveInfo);
        Context context = SESceneManager.getInstance().getContext();
        PackageManager pm = context.getPackageManager();
        String label = resolveInfo.loadLabel(pm).toString();
        if (!label.equals(info.mDisplayName)) {
            info.mDisplayName = label;
            info.updateToDB(true);
        }
        if (bitmapInfo.bitmap == null) {
            return false;
        }
        final String imageKey = getObjectInfo().mName + "_imageKey";
        SELoadResThread.getInstance().process(new Runnable() {
            public void run() {
                final int imageData = loadImageData_JNI(bitmapInfo.bitmap);
                bitmapInfo.bitmap.recycle();
                new SECommand(getScene()) {
                    public void run() {
                        addImageData_JNI(imageKey, imageData);
                    }
                }.execute();
            }
        });
        return true;
    }

    private void startApplicationUninstallActivity(ComponentName componentName) {
        String packageName = componentName.getPackageName();
        String className = componentName.getClassName();
        PackageManager pm = SESceneManager.getInstance().getContext().getPackageManager();
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_DISABLED_COMPONENTS);
            boolean isUpdatedSysApp = (applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
            boolean isSysApp = (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (isSysApp || isUpdatedSysApp) {
                ToastUtils.showUninstallForbidden();
            } else {
                Intent intent = new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, className));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                SESceneManager.getInstance().startActivity(intent);
            }
        } catch (NameNotFoundException e) {
            if (HomeUtils.DEBUG)
                Log.d(HomeUtils.TAG, "uninstall : " + e.getMessage());
        }
    }

    private static final int ICON_PADDING = 20;
    public static int getAppIconWidth(Context context) {
//        final float iconThreshold = context.getResources().getDimensionPixelSize(R.dimen.wall_app_icon_size);
//        float size = Math.min(ApplicationMenu.mCellWidth, ApplicationMenu.mCellHeight);
//        final float scale = iconThreshold / size;
//        if (size > iconThreshold) {
//            size = iconThreshold;
//        }
//
//        int padding = (int) (HomeUtils.ICON_PADDING * scale);
//        int iconPadding = (int) (4 * scale);
//        final float iconSize = size - 2 * (padding + iconPadding);
//        return (int)(0.5 + iconSize);
//    	return context.getResources().getDimensionPixelSize(R.dimen.wall_app_icon_size);
        HouseObject houseObject = ModelInfo.getHouseObject(SESceneManager.getInstance().getCurrentScene());
        if(houseObject == null) {
        	HouseSceneInfo info = SESceneManager.getInstance().getCurrentScene().mSceneInfo.mHouseSceneInfo;
        	return (int)Math.min(info.getWallUnitSizeX(), info.getWallUnitSizeY()) - ICON_PADDING;
        }
        return Math.min(houseObject.getWallUnitSizeX(), houseObject.getWallUnitSizeY()) - ICON_PADDING;
    }
    
    public static boolean isSysApp(ComponentName componentName) {
        if (componentName == null) {
            return true;
        }
        if(componentName.flattenToString().equals("com.borqs.freehdhome/com.borqs.se.home3d.SearchActivity")) {
        	return true;
        }
        String packageName = componentName.getPackageName();
        PackageManager pm = SESceneManager.getInstance().getContext().getPackageManager();
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_DISABLED_COMPONENTS);
            boolean isUpdatedSysApp = (applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
            boolean isSysApp = (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (isSysApp || isUpdatedSysApp) {
                return true;
            }
        } catch (NameNotFoundException e) {
            if (HomeUtils.DEBUG)
                Log.d("SEHome", "isSysApp : " + e.getMessage());
        }
        return false;
    }
}
