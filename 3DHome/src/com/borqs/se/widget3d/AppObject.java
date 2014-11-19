package com.borqs.se.widget3d;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout.Alignment;
import android.util.Log;
import android.widget.Toast;

import com.borqs.se.R;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SEImageView;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEUtils;
import com.borqs.se.home3d.HomeScene;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.HomeManager;
import com.borqs.se.home3d.HomeSceneInfo;
import com.borqs.se.shortcut.ItemInfo;
import com.borqs.se.shortcut.LauncherModel;

public class AppObject extends NormalObject {
//    private SEObject mBackground;
    public SEImageView mIconObject;
    public SEObject mIconBoxObject;

    public AppObject(HomeScene scene, String name, int index) {
        super(scene, name, index);
    }

    @Override
    public boolean loadMyself(final SEObject parent, final Runnable finish) {
        render();
        initStatus();
        setHasInit(true);
        if (finish != null) {
            finish.run();
        }
        return true;
    }

    @Override
    public void onRenderFinish(SECamera camera) {
        super.onRenderFinish(camera);
        createIconObject();
    }

    @Override
    public void initStatus() {
        super.initStatus();
        setOnLongClickListener(new SEObject.OnTouchListener() {
            public void run(SEObject obj) {
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
                hideBackground();
            }
        });
        if (!isSysApp(getObjectInfo().mComponentName)) {
            setCanUninstall(true);
        } else {
            setCanUninstall(false);
        }
        setCanChangeBind(false);
        setCanChangeIcon(true);
        setCanChangeLabel(true);
    }

    @Override
    public void showBackground() {
//        if (mBackground == null && isOnWall()) {
//            int index = (int) System.currentTimeMillis();
//            mBackground = new SEObject(getScene(), "IconBackground", index);
//            HomeManager.getInstance().getModelManager().createQuickly(this, mBackground);
//            addComponenetObject(mBackground, false);
//            mBackground.getUserTransParas().set(getBackgroundLocation());
//            mBackground.setUserTransParas();
//            mBackground.setBlendSortAxis(AXIS.Y);
//        }

    }

//    private boolean isOnWall() {
//        return "wall".equals(getParent().getName());
//    }

    @Override
    public void hideBackground() {
//        if (mBackground != null) {
//            removeChild(mBackground, true);
//            HomeManager.getInstance().getModelManager().unRegister(mBackground);
//            mBackground = null;
//        }

    }

//    private SETransParas getBackgroundLocation() {
//        SETransParas transparas = new SETransParas();
//        transparas.mTranslate.set(-6, HomeUtils.ICON_BACKGROUND_SPACING, -5);
//        int spanX = getObjectSlot().mSpanX;
//        int spanY = getObjectSlot().mSpanY;
//        transparas.mScale.set(spanX, 1, spanY * 0.9f);
//        return transparas;
//    }

    public Folder changeToFolder() {
        hideBackground();
        final ObjectInfo objInfo = new ObjectInfo();
        objInfo.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
        objInfo.mType = "Folder";
        objInfo.mName = "app_folder";
        objInfo.mIndex = (int) System.currentTimeMillis();
        objInfo.mSceneName = getScene().getSceneName();
        NormalObject parent = (NormalObject) getParent();
        objInfo.mObjectSlot.set(getObjectSlot());
        Folder folder = new Folder(getHomeScene(), objInfo.mName, objInfo.mIndex);
        folder.setObjectInfo(objInfo);
        parent.addChild(folder, true);
        folder.initStatus();
        changeParent(folder);
        getObjectSlot().mSlotIndex = 0;
        setVisible(false);
        folder.updateFolderCover();
        return folder;
    }

    public IconBox changeToIconBox() {

        if (getChangedToObj() == null) {
            // It is the first time change, set the changed obj.
            // So let the user can change the obj quickly.
            final ObjectInfo objInfo = new ObjectInfo();
            objInfo.setModelInfo(HomeManager.getInstance().getModelManager().findModelInfo("group_iconbox"));
            objInfo.mIndex = (int) System.currentTimeMillis();
            objInfo.mSceneName = getScene().getSceneName();
            objInfo.mComponentName = getObjectInfo().mComponentName;
            objInfo.mShortcutUrl = getObjectInfo().mShortcutUrl;
            objInfo.mShortcutIcon = getObjectInfo().mShortcutIcon;
            objInfo.mDisplayName = getObjectInfo().mDisplayName;
            NormalObject parent = (NormalObject) getParent();
            objInfo.mObjectSlot.set(getObjectSlot());
            final IconBox iconBox = (IconBox) objInfo.CreateNormalObject(getHomeScene());
            HomeManager.getInstance().getModelManager().createQuickly(parent, iconBox);
            parent.addChild(iconBox, false);
            iconBox.initStatus();

            copeStatusTo(iconBox);
            iconBox.setChangedToObj(this);
            setChangedToObj(iconBox);
        }
        getChangedToObj().setTouch(getTouchX(), getTouchY());
        getChangedToObj().setVisible(true);
        setVisible(false);
        return (IconBox) getChangedToObj();
    }

    public static AppObject create(HomeScene scene, ItemInfo itemInfo) {
        ObjectInfo info = new ObjectInfo();
        info.mName = "app_" + itemInfo.getComponentName() + "_" + System.currentTimeMillis();
        info.mSceneName = scene.getSceneName();
        info.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
        info.mObjectSlot.mSpanX = itemInfo.getSpanX();
        info.mObjectSlot.mSpanY = itemInfo.getSpanY();
        info.mComponentName = itemInfo.getComponentName();
        info.mType = "App";
        AppObject appObject = new AppObject(scene, info.mName, info.mIndex);
        appObject.setIsFresh(true);
        appObject.setObjectInfo(info);
        info.saveToDB();
        return appObject;
    }

    public static AppObject create(HomeScene scene, ComponentName componentName, int index) {
        ObjectInfo info = new ObjectInfo();
        info.mName = "app_" + componentName + "_" + System.currentTimeMillis() + "_" + index;
        info.mSceneName = scene.getSceneName();
        info.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
        info.mObjectSlot.mSpanX = 1;
        info.mObjectSlot.mSpanY = 1;
        info.mComponentName = componentName;
        info.mType = "App";
        AppObject appObject = new AppObject(scene, info.mName, info.mIndex);
        appObject.setIsFresh(true);
        appObject.setObjectInfo(info);
        info.saveToDB();
        return appObject;
    }

    @Override
    public void handOnClick() {
        Intent intent = getObjectInfo().getIntent();
        if (intent != null) {
            if (!HomeManager.getInstance().startActivity(intent)) {
                if (HomeUtils.DEBUG)
                    Log.e("SEHome", "not found bind activity");
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
        super.onSlotSuccess();
        if (hasBeenReleased()) {
            return;
        }
//        String backgroundName = SettingsActivity.getAppIconBackgroundName(getContext());
//        if (!"none".equals(backgroundName)) {
//            showBackground();
//        } else {
//            hideBackground();
//        }
    }

    @Override
    public boolean update(SEScene scene) {
        createIconObject();
        return true;
    }

    public void updateIcon(final Bitmap icon) {
        if (icon == null) {
            return;
        }
        final ObjectInfo info = getObjectInfo();
        info.mShortcutIcon = icon.copy(Config.ARGB_8888, true);
        info.updateToDB(true, false, false);
        createIconObject();
    }

    public void updateLabel(final String name) {
        if (name == null) {
            return;
        }
        ObjectInfo info = getObjectInfo();
        info.mDisplayName = name;
        info.updateToDB(false, true, false);
        createIconObject();
    }

    public void resetIcon() {
        ObjectInfo info = getObjectInfo();
        info.mShortcutIcon = null;
        info.updateToDB(true, false, false);
        createIconObject();
    }

    private void startApplicationUninstallActivity(ComponentName componentName) {
        String packageName = componentName.getPackageName();
        String className = componentName.getClassName();
        if (isSysApp(componentName)) {
            // System applications cannot be installed. For now, show a
            // toast explaining that.
            final int messageId = R.string.uninstall_system_app_text;
            SESceneManager.getInstance().runInUIThread(new Runnable() {
                public void run() {
                    Toast.makeText(HomeManager.getInstance().getContext(), messageId, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Intent intent = new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, className));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            HomeManager.getInstance().startActivity(intent);
        }
    }

    public static boolean isSysApp(ComponentName componentName) {
        if (componentName == null) {
            return true;
        }
        ItemInfo itemInfo = LauncherModel.getInstance().findAppItem(componentName);
        if (itemInfo != null) {
            return itemInfo.mIsSysApp;
        }
        return false;
    }

    private static Drawable getBackgroundDrawable(Rect rect, int averageColor) {
        int endR = Color.red(averageColor) + (255 - Color.red(averageColor)) * 3 / 5;
        int endG = Color.green(averageColor) + (255 - Color.green(averageColor)) * 3 / 5;
        int endB = Color.blue(averageColor) + (255 - Color.blue(averageColor)) * 3 / 5;
        int endColor = Color.argb(0xff, endR, endG, endB);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {
                averageColor, endColor });
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        drawable.setBounds(rect);
        drawable.setGradientRadius((float) (Math.sqrt(2) * 60));
        drawable.setCornerRadii(new float[] { 12, 12, 12, 12, 12, 12, 12, 12 });
        return drawable;
    }

    private static int getAverageColor(Bitmap bitmap) {
        if (bitmap == null) {
            return 0xFFFFFF00;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int avgr = 0;
        int avgg = 0;
        int avgb = 0;
        int count = 0;
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                int newX = w * x / 10;
                int newY = h * y / 10;
                int color = bitmap.getPixel(newX, newY);
                float alpha = Color.alpha(color) / 255f;
                if (alpha > 0.5f) {
                    int r = 255 - (int) (Color.red(color) * alpha);
                    int g = 255 - (int) (Color.green(color) * alpha);
                    int b = 255 - (int) (Color.blue(color) * alpha);
                    avgr = avgr + r;
                    avgg = avgg + g;
                    avgb = avgb + b;
                    count++;
                }
            }
        }
        if (count == 0) {
            return 0xFFFFFF00;
        }
        avgr = avgr / count;
        avgg = avgg / count;
        avgb = avgb / count;
        return Color.argb(0xff, avgr, avgg, avgb);

    }

    private static Drawable mIconShadow;

    public static Bitmap getAppIconBitmap(HomeScene scene, Bitmap bitmapIcon, Drawable icon, String title,
            boolean needBackground) {
        int bitmapW = (int) scene.getHomeSceneInfo().mCellWidth;
        int bitmapH = (int) scene.getHomeSceneInfo().mCellHeight;
        float scale = 1;
        if (bitmapW > 128) {
            scale = 128f / bitmapW;
            bitmapH = (int) (bitmapH * scale);
            bitmapW = 128;

        }
        int newW = SEUtils.higherPower2(bitmapW);
        int newH = SEUtils.higherPower2(bitmapH);
        float screenDensity = scene.getScreenDensity();
        Bitmap res = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(res);
        canvas.translate((newW - bitmapW) * 0.5f, (newH - bitmapH) * 0.5f);
        if (icon != null && (icon instanceof BitmapDrawable)) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
            bitmapIcon = bitmapDrawable.getBitmap();
        }
        int iconLeft = (int) (scene.getHomeSceneInfo().mAppIconPaddingLeft * scale);
        int iconRight = (int) (scene.getHomeSceneInfo().mAppIconPaddingRight * scale);
        int iconTop = (int) (scene.getHomeSceneInfo().mAppIconPaddingTop * scale);
        int iconSize = bitmapW - iconRight - iconLeft;
        float labelTop = iconSize + iconTop + scene.getHomeSceneInfo().mAppIconPaddingBottom * scale;
        final float textAreaHeight = bitmapH - (iconSize + iconTop);
        if (null == title) {
            canvas.translate(0.0f, textAreaHeight);
        }
        if (mIconShadow == null) {
            mIconShadow = SESceneManager.getInstance().getContext().getResources().getDrawable(R.drawable.icon_shadow);
        }
        Rect rectShadow = new Rect(iconLeft + 6, iconTop + 6, (int) (iconLeft + iconSize * 1.12f + 6), (int) (iconTop
                + iconSize * 1.12f + 6));
        mIconShadow.setBounds(rectShadow);
        mIconShadow.draw(canvas);
        if (needBackground) {
            Rect rect = new Rect(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
            int averageColor = getAverageColor(bitmapIcon);
            Drawable background = getBackgroundDrawable(rect, averageColor);
            background.draw(canvas);
            iconLeft = iconLeft + 6;
            iconRight = iconRight + 6;
            iconTop = iconTop + 6;
            iconSize = bitmapW - iconRight - iconLeft;
        }
        if (bitmapIcon != null) {
            Bitmap scaleIcon = Bitmap.createScaledBitmap(bitmapIcon, iconSize, iconSize, true);
//            if (needBackground) {
//                int iconW = scaleIcon.getWidth();
//                int iconH = scaleIcon.getHeight();
//                for (int x = 0; x < iconW; x++) {
//                    for (int y = 0; y < iconW - x; y++) {
//                        int pix = scaleIcon.getPixel(x, y);
//                        int newRed = (int) (Color.red(pix) * 1.5f);
//                        if (newRed > 255) {
//                            newRed = 255;
//                        }
//                        int newGreen = (int) (Color.green(pix) * 1.5f);
//                        if (newGreen > 255) {
//                            newGreen = 255;
//                        }
//                        int newBlue = (int) (Color.blue(pix) * 1.5f);
//                        if (newBlue > 255) {
//                            newBlue = 255;
//                        }
//                        int newPix = Color.argb(Color.alpha(pix), newRed, newGreen, newBlue);
//                        scaleIcon.setPixel(x, y, newPix);
//                    }
//                }
//            }
            canvas.drawBitmap(scaleIcon, iconLeft, iconTop, null);
        } else if (icon != null) {
            Rect oldBounds = icon.copyBounds();
            icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
            icon.draw(canvas);
            icon.setBounds(oldBounds);
        }
        String label = title;
        if (label != null) {
            int labelW = scene.getContext().getResources().getDimensionPixelSize(R.dimen.apps_customize_cell_width);
            scale = 1;
            if (labelW > 128) {
                scale = 128f / labelW;
                labelW = 128;
            }
            TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.density = screenDensity;
            textPaint.setTextSize(scene.getHomeSceneInfo().mAppIconTextSize * scale);
            textPaint.setColor(Color.WHITE);
            textPaint.setFakeBoldText(true);
            textPaint.setShadowLayer(screenDensity * 2 * scale, screenDensity * scale, screenDensity * scale,
                    Color.BLACK);
            StaticLayout titleLayout = new StaticLayout(label, textPaint, labelW, Alignment.ALIGN_CENTER, 1f, 0.0F,
                    false);
            int lineCount = titleLayout.getLineCount();
            if (lineCount > 1) {
                float allowW = labelW;
                int index = 0;
                String newlabel = null;
                float newLableW;
                while (true) {
                    index++;
                    if (index > label.length()) {
                        break;
                    }
                    newlabel = label.substring(0, index) + "...";
                    newLableW = StaticLayout.getDesiredWidth(newlabel, textPaint);
                    if (newLableW > allowW) {
                        newlabel = label.substring(0, --index) + "...";
                        break;
                    }
                }
                titleLayout = new StaticLayout(newlabel, textPaint, labelW, Alignment.ALIGN_CENTER, 1f, 0.0F, false);
            }
            float left = 0;           
            canvas.save();
            canvas.translate(left, labelTop);
            canvas.scale((float) bitmapW / labelW, (float) bitmapW / labelW);
            titleLayout.draw(canvas);
            canvas.restore();
        }
        return res;
    }

    private void createIconObject() {
        SELoadResThread.getInstance().process(new Runnable() {
            public void run() {
                ResolveInfo resolveInfo = getObjectInfo().getResolveInfo();
                Drawable icon = null;
                if (getObjectInfo().mShortcutIcon == null) {
                    icon = HomeUtils.getAppIcon(getContext(), resolveInfo);
                }

                String label = getDisplayLabel(resolveInfo);
                final Bitmap iconWithText = getAppIconBitmap(getHomeScene(), getObjectInfo().mShortcutIcon, icon, label, true);
                new SECommand(getScene()) {
                    public void run() {
                        if (!hasBeenReleased()) {
                            if (mIconObject != null) {
                                removeComponenetObject(mIconObject, true);
                            }
                            mIconObject = new SEImageView(getScene(), mName + "_icon");
                            HomeSceneInfo sceneInfo = getHomeSceneInfo();
                            int bitmapW = (int) sceneInfo.mCellWidth;
                            int bitmapH = (int) sceneInfo.mCellHeight;
                            float scale = 1;
                            if (bitmapW > 128) {
                                scale = 128f / bitmapW;
                                bitmapH = (int) (bitmapH * scale);
                                bitmapW = 128;
                            }
                            mIconObject.setSize(sceneInfo.mCellWidth, sceneInfo.mCellHeight);
                            mIconObject.setImageValidAreaSize(bitmapW, bitmapH);
                            mIconObject.setBackgroundBitmap(iconWithText);
                            addComponenetObject(mIconObject, true);
                        }
                    }
                }.execute();
            }
        });
    }

    private String getDisplayLabel(ResolveInfo resolveInfo) {
        if (!isLabelShown()) {
            return null;
        }

        String label = getObjectInfo().mDisplayName;
        if (label == null) {
            label = HomeUtils.getAppLabel(getContext(), resolveInfo);
        }
        if (label == null && getObjectInfo().mComponentName != null) {
            label = getObjectInfo().mComponentName.getPackageName();
        }
        return label;
    }
}
