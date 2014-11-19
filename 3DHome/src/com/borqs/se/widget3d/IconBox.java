package com.borqs.se.widget3d;

import java.io.IOException;
import java.io.InputStream;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.Toast;

import com.borqs.se.R;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.home3d.HomeManager;
import com.borqs.se.home3d.HomeScene;
import com.borqs.se.home3d.HomeUtils;

public class IconBox extends NormalObject {
    private String mImageName;
    private String mImagePath;

    public IconBox(HomeScene scene, String name, int index) {
        super(scene, name, index);
    }

    public AppObject changeToAppIcon() {

        if (getChangedToObj() == null) {
            // It is the first time change, set the changed obj.
            // So let the user can change the obj quickly.
            ObjectInfo info = new ObjectInfo();
            info.mName = "app_" + System.currentTimeMillis();
            info.mSceneName = getScene().getSceneName();
            info.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
            info.mObjectSlot.mSpanX = 1;
            info.mObjectSlot.mSpanY = 1;
            info.mShortcutUrl = getObjectInfo().mShortcutUrl;
            info.mComponentName = getObjectInfo().mComponentName;
            info.mShortcutIcon = getObjectInfo().mShortcutIcon;
            info.mDisplayName = getObjectInfo().mDisplayName;
            AppObject appObject;
            if (!getObjectInfo().isShortcut()) {
                info.mType = "Shortcut";
                appObject = new ShortcutObject(getHomeScene(), info.mName, info.mIndex);
            } else {
                info.mType = "App";
                appObject = new AppObject(getHomeScene(), info.mName, info.mIndex);
            }
            appObject.setObjectInfo(info);
            // NormalObject parent = (NormalObject) getParent();
            // IconBox 在物体移动时父亲只可能是Scene
            // ContentObject,此地方获取父亲错误在DragLayer中加入Log查询（目前原因不详）
            // above issue has been fixed
            SEObject parent = getScene().getContentObject();
            parent.addChild(appObject, false);
            appObject.loadMyself(parent, null);
            appObject.initStatus();
            copeStatusTo(appObject);
            appObject.setChangedToObj(this);
            setChangedToObj(appObject);
        }
        getChangedToObj().setTouch(getTouchX(), getTouchY());
        getChangedToObj().setVisible(true);
        setVisible(false);

        return (AppObject) getChangedToObj();
    }

    @Override
    public void initStatus() {
        super.initStatus();
        SEObject face = findComponenetObjectByRegularName("face");
        mImageName = face.getImageName();
        mImagePath = face.getImageKey();
        if (getObjectInfo().mShortcutIcon != null || getObjectInfo().mComponentName != null) {
            changeImageOfFace();
        }
        if (!AppObject.isSysApp(getObjectInfo().mComponentName)) {
            setCanUninstall(true);
        } else {
            setCanUninstall(false);
        }
    }

    @Override
    public void handleOutsideRoom() {
        if (getObjectInfo().mComponentName != null) {
            if (isFresh()) {
                startApplicationUninstallActivity(getObjectInfo().mComponentName);
                setIsFresh(false);
            }
        }
        super.handleOutsideRoom();
    }

    private void startApplicationUninstallActivity(ComponentName componentName) {
        String packageName = componentName.getPackageName();
        String className = componentName.getClassName();
        if (AppObject.isSysApp(componentName)) {
            // System applications cannot be installed. For now, show a
            // toast explaining that.
            final int messageId = R.string.uninstall_system_app_text;
            SESceneManager.getInstance().runInUIThread(new Runnable() {
                public void run() {
                    Toast.makeText(SESceneManager.getInstance().getContext(), messageId, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Intent intent = new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, className));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            HomeManager.getInstance().startActivity(intent);
        }
    }

    private void changeImageOfFace() {
        SELoadResThread.getInstance().process(new Runnable() {
            public void run() {
                try {
                    String imageAssetsPath = mImagePath.substring(7);
                    InputStream is = getContext().getAssets().open(imageAssetsPath);
                    Bitmap background = BitmapFactory.decodeStream(is).copy(Config.ARGB_8888, true);
                    is.close();
                    Canvas canvas = new Canvas(background);
                    if (getObjectInfo().mShortcutIcon != null) {
                        Rect iconSrc = new Rect(0, 0, getObjectInfo().mShortcutIcon.getWidth(),
                                getObjectInfo().mShortcutIcon.getHeight());
                        Rect iconDes = new Rect(0, 0, 128, 128);
                        canvas.drawBitmap(getObjectInfo().mShortcutIcon, iconSrc, iconDes, new Paint());
                    } else if (getObjectInfo().mComponentName != null) {
                        ResolveInfo resolveInfo = getObjectInfo().getResolveInfo();
                        Drawable drawableIcon = HomeUtils.getAppIcon(getContext(), resolveInfo);
                        Rect oldBounds = drawableIcon.copyBounds();
                        drawableIcon.setBounds(0, 0, 128, 128);
                        drawableIcon.draw(canvas);
                        drawableIcon.setBounds(oldBounds);
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
    }

    @Override
    public void updateComponentName(ComponentName name) {
        super.updateComponentName(name);
        if (getObjectInfo().mComponentName != null) {
            changeImageOfFace();
            if (!AppObject.isSysApp(getObjectInfo().mComponentName)) {
                setCanUninstall(true);
            } else {
                setCanUninstall(false);
            }
        }

    }

    @Override
    public void onSlotSuccess() {
        setCanChangeBind(true);
        setCanChangeIcon(true);
        if (isFresh() && !getObjectInfo().isShortcut() && getObjectInfo().mComponentName == null) {
            if (canChangeBind()) {
                getScene().handleMessage(HomeScene.MSG_TYPE_SHOW_BIND_APP_DIALOG, this);
            }
        }
    }

    public void updateIcon(final Bitmap icon) {
        if (icon == null) {
            return;
        }
        ObjectInfo info = getObjectInfo();
        info.mShortcutIcon = icon.copy(Config.ARGB_8888, true);
        info.updateToDB(true, false, false);
        changeImageOfFace();

    }

    public void resetIcon() {
        if (getObjectInfo().isShortcut()) {
            return;
        }
        ObjectInfo info = getObjectInfo();
        info.mShortcutIcon = null;
        info.updateToDB(true, false, false);
        changeImageOfFace();
    }

}
