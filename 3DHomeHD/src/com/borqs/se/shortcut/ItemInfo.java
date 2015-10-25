package com.borqs.se.shortcut;

import java.lang.reflect.Method;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;

import com.borqs.freehdhome.R;
import com.borqs.se.engine.SEVector.SERect3D;
import com.borqs.se.ToastUtils;
import com.borqs.se.home3d.ApplicationMenu.PreViewObject;
import com.borqs.se.home3d.HomeActivity;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.engine.SEObjectFactory;
import com.borqs.se.engine.SESceneManager;

public class ItemInfo {
    public static final int ITEM_TYPE_APP = 0;
    public static final int ITEM_TYPE_SHORTCUT = 1;
    public static final int ITEM_TYPE_WIDGET = 2;

    private int mSpanX;
    private int mSpanY;

    private boolean mIsDefault;
    private Context mContext;
    private ResolveInfo mResolveInfo;
    private ComponentName mComponentName;
    private String mLabel = "";
    private Drawable mIcon;
    private int mWidth;
    private int mHeight;

    public int mItemType;
    private int mIconDpi;
    private PackageManager mPackageManager;

    public ItemInfo(Context context, ResolveInfo resolveInfo, ComponentName name) {
        mItemType = -1;
        mContext = context;
        mComponentName = name;
        mPackageManager = context.getPackageManager();
        mResolveInfo = resolveInfo;
        if (mResolveInfo != null) {
            mLabel = mResolveInfo.loadLabel(context.getPackageManager()).toString();
            mIcon = getFullResIcon(mResolveInfo, mPackageManager);
            if (mIcon == null) {
                mIcon = resolveInfo.loadIcon(context.getPackageManager());
                if (mIcon == null) {
                    mIcon = ToastUtils.getDefaultApplicationIcon(context);
                }
            }
        }
        mSpanX = mSpanY = 1;
        mIsDefault = true;

        int density = context.getResources().getDisplayMetrics().densityDpi;
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
        } else {
            mIconDpi = context.getResources().getDisplayMetrics().densityDpi;
        }
        // need to set mIconDpi before getting default icon
    }

    public ComponentName getComponentName() {
        return mComponentName;
    }

    public ResolveInfo getResolveInfo() {
        return mResolveInfo;
    }

    public void setResolveInfo(ResolveInfo resolveInfo) {
        mResolveInfo = resolveInfo;
    }

    public String getPreviewName() {
        return null;
    }

    public String getLabel() {
        return mLabel;
    }
    
    public Drawable getIcon() {
        return mIcon;
    }


    @Override
    public boolean equals(Object obj) {
        try {
            if (obj != null) {
                ItemInfo other = (ItemInfo) obj;
                return mComponentName.equals(other.mComponentName);
            }
        } catch (ClassCastException e) {
        }
        return false;
    }

    public void setComponentName(ComponentName name) {
        mComponentName = name;
    }

    public boolean isDefault() {
        return mIsDefault;
    }

    public void setIsDefault(boolean isDefault) {
        mIsDefault = isDefault;
    }

    public void setWidth(int w) {
        mWidth = w;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int h) {
        mHeight = h;
    }

    public int getSpanX() {
        return mSpanX;
    }

    public void setSpanX(int spanX) {
        this.mSpanX = spanX;
    }

    public int getSpanY() {
        return mSpanY;
    }

    public void setSpanY(int spanY) {
        this.mSpanY = spanY;
    }

    public void setSpan(int spanX, int spanY) {
        this.mSpanX = spanX;
        this.mSpanY = spanY;
    }

    public void loadPreViewObject(PreViewObject obj) {
        if (isDefault()) {
            loadDefaultPreViewObject(obj);
            return;
        }
        String imageName = obj.getName() + "_imageName";
        String imagekey = obj.getName() + "_imageKey";
        SERect3D rect = new SERect3D();
        SEObjectFactory.createOpaqueRectangle(obj, rect, imageName, imagekey, null);
        int bitmapW = mWidth;
        int bitmapH = mHeight;
        if (bitmapW > 128) {
            bitmapH = bitmapH * 128 / bitmapW;
            bitmapW = 128;
        }
        obj.setImageSize(bitmapW, bitmapH);
    }

    public Bitmap getPreviewBitmap() {
        return getPreviewBitmap(mContext, mResolveInfo, mWidth, mHeight);
    }

    public Bitmap getPreviewBitmap(Context context, ResolveInfo resolveInfo, int w, int h) {
        int bitmapW = w;
        int bitmapH = h;
        float scale = 1;
        if (bitmapW > 128) {
            scale = (float) 128 / bitmapW;
            bitmapH = (int) (bitmapH * scale);
            bitmapW = 128;
        }
        Resources resources = context.getResources();
        Configuration config = new Configuration();
        android.provider.Settings.System.getConfiguration(context.getContentResolver(), config);
        float pixelDensity = SESceneManager.getInstance().getPixelDensity();
        float fontSize = context.getResources().getDimension(R.dimen.app_menu_icon_font) * config.fontScale
                * pixelDensity;
        TextPaint titlePaint = new TextPaint();
        titlePaint.setTextSize(fontSize * scale);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setAntiAlias(true);
        titlePaint.setStrokeWidth(HomeUtils.STROKE_WIDTH * pixelDensity * scale);
        titlePaint.setStrokeCap(Paint.Cap.ROUND);

        int newW = HomeUtils.higherPower2(bitmapW);
        int newH = HomeUtils.higherPower2(bitmapH);
        Bitmap preview = Bitmap.createBitmap(newW, newH, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(preview);
        canvas.translate((newW - bitmapW) * 0.5f, (newH - bitmapH) * 0.5f);
        int iconSize = (int) (resources.getDimensionPixelSize(R.dimen.app_icon_size) * scale);
        int paddingH = bitmapW - iconSize;
        int paddingV = bitmapH - iconSize;
        Rect oldBounds = mIcon.copyBounds();
        mIcon.setBounds(paddingH / 2, paddingV / 4, paddingH / 2 + iconSize, paddingV / 4 + iconSize);
        mIcon.draw(canvas);
        mIcon.setBounds(oldBounds); // Restore the bounds
        String label = getLabel();

        StaticLayout titleLayout = new StaticLayout(label, titlePaint, bitmapW - paddingH / 2, Alignment.ALIGN_CENTER,
                1f, 0.0F, false);
        int lineCount = titleLayout.getLineCount();
        if (lineCount > 2) {
            int index = titleLayout.getLineEnd(1);
            if (index > 0) {
                label = label.substring(0, index);
                titleLayout = new StaticLayout(label, titlePaint, bitmapW - paddingH / 2, Alignment.ALIGN_CENTER, 1f,
                        0.0F, false);
            }
        }
        float left = paddingH / 4;
        float top = paddingV * 5 / 16 + iconSize;
        canvas.save();
        canvas.translate(left, top);
        titleLayout.draw(canvas);
        canvas.restore();
        return preview;
    }

    private void loadDefaultPreViewObject(PreViewObject obj) {
        SERect3D rect = new SERect3D();
        SEObjectFactory.createOpaqueRectangle(obj, rect, new float[] { 0, 0, 0 });
    }

    public boolean equals(ItemInfo itemInfo) {
        if (itemInfo != null && mComponentName != null && mComponentName.equals(itemInfo.getComponentName())) {
            return true;
        } else {
            return super.equals(itemInfo);
        }
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
        } catch (Exception e) {
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
}
