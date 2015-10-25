package com.borqs.se.shortcut;


import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ResolveInfo;

public class ShortcutItemInfo extends ItemInfo {

    public ShortcutItemInfo(Context context, ResolveInfo resolveInfo, ComponentName name) {
        super(context, resolveInfo, name);
        setIsDefault(false);
        mItemType = ItemInfo.ITEM_TYPE_SHORTCUT;
    }

    public String getPreviewName() {
        return "previewshortcut_" + getResolveInfo().activityInfo.packageName + "_"
                + getResolveInfo().activityInfo.name;
    }
}
