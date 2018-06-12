package com.borqs.se.shortcut;


import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;

public class WidgetItemInfo extends ItemInfo {

    public WidgetItemInfo(Context context, AppWidgetProviderInfo providerInfo, ComponentName name) {
        super(context, null, name);
        setIsDefault(false);
        mItemType = ItemInfo.ITEM_TYPE_WIDGET;
    }

}
