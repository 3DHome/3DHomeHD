package com.borqs.se.shortcut;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout.Alignment;
import android.util.DisplayMetrics;
import android.util.Log;

import com.borqs.se.ToastUtils;
import com.borqs.se.home3d.HomeActivity;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.engine.SESceneManager;

public class AppItemInfo extends ItemInfo {
    
    private static Bitmap mShadowBitmap;

    public AppItemInfo(Context context, ResolveInfo resolveInfo, ComponentName name) {
        super(context, resolveInfo, name);
        setIsDefault(false);
        mItemType = ItemInfo.ITEM_TYPE_APP;
    }

    public String getPreviewName() {
        return "previewapp_" + getResolveInfo().activityInfo.packageName + "_" + getResolveInfo().activityInfo.name;
    }

    public static Bitmap getBitmap(ResolveInfo resolveInfo, int w, int h) {
        return getBitmap(resolveInfo, null, w, h);
    }
    public static class BitmapInfo {
    	public Bitmap bitmap;
    	public int bitmapWidth;
    	public int bitmapHeight;
    	public int bitmapContentWidth;
    	public int bitmapContentHeight;
    }
    public static BitmapInfo getBitmap(ResolveInfo resolveInfo) {
    	int bitmapW = 128;
    	int bitmapH = 128;
    	Bitmap res = Bitmap.createBitmap(bitmapW, bitmapH, Bitmap.Config.ARGB_8888);
    	Context context = SESceneManager.getInstance().getContext();
        Canvas canvas = new Canvas(res);
        Resources resources = context.getResources();
        PackageManager pm = context.getPackageManager();
        Drawable icon = getFullResIcon(resolveInfo, pm);
        if (icon == null) {
            icon = resolveInfo.loadIcon(pm);
        }
        if (icon == null) {
            icon = ToastUtils.getDefaultApplicationIcon(context);
        }
        int iconWidth = icon.getIntrinsicWidth();
        int iconHeight = icon.getIntrinsicHeight();
        icon.setBounds(0, 0, bitmapW, bitmapH);
        icon.draw(canvas);
        BitmapInfo bmpInfo = new BitmapInfo();
        bmpInfo.bitmap = res;
        bmpInfo.bitmapWidth = bitmapW;
        bmpInfo.bitmapHeight = bitmapH;
        bmpInfo.bitmapContentHeight = iconWidth;
        bmpInfo.bitmapContentWidth = iconHeight;
        return bmpInfo;
    }
    public static Bitmap getBitmap(ResolveInfo resolveInfo, String title, int w, int h) {
        int bitmapW = w;
        int bitmapH = h;
        float scale = 1;
        if (bitmapW > 128) {
            scale = 128f / bitmapW;
            bitmapH = (int) (bitmapH * scale);
            bitmapW = 128;

        }
        int newW = HomeUtils.higherPower2(bitmapW);
        int newH = HomeUtils.higherPower2(bitmapH);
        Context context = SESceneManager.getInstance().getContext();
        PackageManager pm = context.getPackageManager();
        TextPaint textPaint = new TextPaint();
        float fontSize = HomeUtils.FONT_SIZE;
        textPaint.setTextSize(fontSize * scale);
        textPaint.setColor(HomeUtils.FONT_COLOR);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);
        textPaint.setStrokeWidth(HomeUtils.STROKE_WIDTH * scale);
        textPaint.setStrokeCap(Paint.Cap.ROUND);
        Bitmap res = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(res);
        canvas.translate((newW - bitmapW) * 0.5f, (newH - bitmapH) * 0.5f);
        if (getShadow() != null) {
            canvas.drawBitmap(getShadow(), 0, (newH - bitmapH) * 0.15f, new Paint());
        }
        int padding = (int) (HomeUtils.ICON_PADDING * scale);
        int iconPadding = (int) (22 * scale);
        int iconLeft = iconPadding + padding;
        int iconSize = bitmapW - 2 * (padding + iconPadding);
        Resources resources = context.getResources();
        Drawable icon = getFullResIcon(resolveInfo, pm);
        if (icon == null) {
            icon = resolveInfo.loadIcon(pm);
        }
        if (icon == null) {
            icon = ToastUtils.getDefaultApplicationIcon(context);
        }
        Rect oldBounds = icon.copyBounds();
        icon.setBounds(iconLeft, padding, iconLeft + iconSize, padding + iconSize);
        icon.draw(canvas);
        icon.setBounds(oldBounds);
        String label;
        if (title == null) {
            label = resolveInfo.loadLabel(pm).toString();
        } else {
            label = title;
        }
        StaticLayout titleLayout = new StaticLayout(label, textPaint, bitmapW - padding, Alignment.ALIGN_CENTER, 1f, 0.0F,
                false);
        int lineCount = titleLayout.getLineCount();
        if (lineCount > 2) {
            int index = titleLayout.getLineEnd(1);
            if (index > 0) {
                label = label.substring(0, index);
                titleLayout = new StaticLayout(label, textPaint, bitmapW - padding, Alignment.ALIGN_CENTER, 1f, 0.0F, false);
            }
        }
        float left = padding / 2f;
        float top = 1.5f * padding + iconSize;
        canvas.save();
        canvas.translate(left, top);
        titleLayout.draw(canvas);
        canvas.restore();
        return res;
    }

    private static Drawable getFullResIcon(Resources resources, int iconId, int iconDpi) {
        Drawable d;
        try {
            Method method = HomeUtils.getMethod(Resources.class, "getDrawableForDensity", new Class[]{int.class, int.class});
            //d = resources.getDrawableForDensity(iconId, mIconDpi);
            method.setAccessible(true);
            d = (Drawable) method.invoke(resources, new Object[]{iconId, iconDpi});
        } catch (Exception e) {
            Log.d("AppItemInfo", "error : " + e.getMessage());
            d = null;
        }

        return d;
    }

    private static Drawable getFullResIcon(ResolveInfo info, PackageManager packageManager) {
        int iconDpi = -1;
        Context context = SESceneManager.getInstance().getContext();
        int density = context.getResources().getDisplayMetrics().densityDpi;
        if (HomeActivity.isScreenLarge()) {
            if (density <= DisplayMetrics.DENSITY_LOW) {
                iconDpi = DisplayMetrics.DENSITY_LOW;
            } else if (density <= DisplayMetrics.DENSITY_MEDIUM) {
                iconDpi = DisplayMetrics.DENSITY_MEDIUM;
            } else if (density <= DisplayMetrics.DENSITY_HIGH) {
                iconDpi = DisplayMetrics.DENSITY_HIGH;
            } else if (density <= DisplayMetrics.DENSITY_XHIGH) {
                iconDpi = DisplayMetrics.DENSITY_XHIGH;
            } else if (density > DisplayMetrics.DENSITY_XHIGH) {
                // We'll need to use a denser icon, or some sort of a mipmap
                iconDpi = DisplayMetrics.DENSITY_XHIGH;
            } else {
                iconDpi = DisplayMetrics.DENSITY_XHIGH;
            }
        } else {
            iconDpi = context.getResources().getDisplayMetrics().densityDpi;
        }
        
        Resources resources;
        try {
            resources = packageManager.getResourcesForApplication(
                    info.activityInfo.applicationInfo);
        } catch (Exception e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = info.activityInfo.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId, iconDpi);
            }
        }
        return null;
    }
    
    public static Bitmap getShadow() {
        if (mShadowBitmap == null) {
            try {
                InputStream is = SESceneManager.getInstance().getContext().getAssets().open("base/appwall/home_appwall01_zch.png");
                mShadowBitmap = BitmapFactory.decodeStream(is);
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mShadowBitmap;
    }

    public static BitmapInfo createBitmapInfo(Bitmap icon, int width, int height) {
        Bitmap des = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        BitmapInfo bitmapInfo = new BitmapInfo();
        Canvas canvas = new Canvas(des);
        int iconWidth = icon.getWidth();
        int iconHeight = icon.getHeight();
        float startx = (width - iconWidth) / 2;
        float starty = (height - iconHeight) / 2;
        canvas.translate(startx, starty);
        canvas.drawBitmap(icon, 0, 0, new Paint());
        bitmapInfo.bitmap = des;
        bitmapInfo.bitmapWidth = width;
        bitmapInfo.bitmapHeight = height;
        bitmapInfo.bitmapContentWidth = iconWidth;
        bitmapInfo.bitmapContentHeight = iconHeight;
        return bitmapInfo;
    }

    public static Bitmap getBitmapWithText(Bitmap icon, String showName, int w, int h) {
        if (showName == null) {
            return icon;
        }
        int bitmapW = w;
        int bitmapH = h;
        float scale = 1;
        if (bitmapW > 128) {
            scale = 128f / bitmapW;
            bitmapH = (int) (bitmapH * scale);
            bitmapW = 128;

        }
        TextPaint textPaint = new TextPaint();
        float fontSize = HomeUtils.FONT_SIZE;
        textPaint.setTextSize(fontSize * scale);
        textPaint.setColor(HomeUtils.FONT_COLOR);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);
        textPaint.setStrokeWidth(HomeUtils.STROKE_WIDTH * scale);
        textPaint.setStrokeCap(Paint.Cap.ROUND);
        int newW = HomeUtils.higherPower2(bitmapW);
        int newH = HomeUtils.higherPower2(bitmapH);
        Bitmap des = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(des);
        canvas.translate((newW - bitmapW) * 0.5f, (newH - bitmapH) * 0.5f);
        if (AppItemInfo.getShadow() != null) {
            canvas.drawBitmap(AppItemInfo.getShadow(), 0, (newH - bitmapH) * 0.15f, new Paint());
        }
        int padding = (int) (HomeUtils.ICON_PADDING * scale);
        int iconPadding = (int) (22 * scale);
        int iconLeft = iconPadding + padding;
        int iconSize = bitmapW - 2 * (padding + iconPadding);
        Rect iconSrc = new Rect(0, 0, icon.getWidth(), icon.getHeight());
        Rect iconDes = new Rect(iconLeft, padding, iconLeft + iconSize, padding + iconSize);
        canvas.drawBitmap(icon, iconSrc, iconDes, new Paint());
        String label = showName;
        StaticLayout titleLayout = new StaticLayout(label, textPaint, bitmapW - padding, Alignment.ALIGN_CENTER, 1f,
                0.0F, false);
        int lineCount = titleLayout.getLineCount();
        if (lineCount > 2) {
            int index = titleLayout.getLineEnd(1);
            if (index > 0) {
                label = label.substring(0, index);
                titleLayout = new StaticLayout(label, textPaint, bitmapW - padding, Alignment.ALIGN_CENTER, 1f, 0.0F,
                        false);
            }
        }
        float left = padding / 2f;
        float top = 1.5f * padding + iconSize;
        canvas.save();
        canvas.translate(left, top);
        titleLayout.draw(canvas);
        canvas.restore();
        return des;
    }
}
