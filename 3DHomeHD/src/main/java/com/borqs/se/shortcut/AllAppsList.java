package com.borqs.se.shortcut;

import java.util.ArrayList;
import java.util.List;

import com.borqs.se.home3d.HomeUtils;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

/**
 * Stores the list of all applications, shortcuts, widgets for the all apps
 * view.
 */
public class AllAppsList {
    public static final int DEFAULT_APPLICATIONS_NUMBER = 100;

    /** The list of all apps, shortcuts and widgets */
    public List<ItemInfo> data = new ArrayList<ItemInfo>(DEFAULT_APPLICATIONS_NUMBER);

    /** The list of apps that have been added since the last notify() call. */
    public List<ItemInfo> added = new ArrayList<ItemInfo>(DEFAULT_APPLICATIONS_NUMBER);

    /** The list of apps that have been removed since the last notify() call. */
    public List<ItemInfo> removed = new ArrayList<ItemInfo>();

    /** The list of apps that have been modified since the last notify() call. */
    public List<ItemInfo> modified = new ArrayList<ItemInfo>();

    public List<ItemInfo> unavailable = new ArrayList<ItemInfo>();
    public List<ItemInfo> available = new ArrayList<ItemInfo>();

    private Context mContext;
    private PackageManager mPackageManager;

    /**
     * Boring constructor.
     */
    public AllAppsList(Context context) {
        mContext = context;
        mPackageManager = mContext.getPackageManager();
    }

    public void clear() {
        data.clear();
        // TODO: do we clear these too?
        added.clear();
        removed.clear();
        modified.clear();
        unavailable.clear();
        available.clear();
    }

    public int size() {
        return data.size();
    }

    public ItemInfo get(int index) {
        return data.get(index);
    }

    /**
     * Add the data for the supplied apk called packageName.
     */
    public void addPackage(String packageName) {
        if (HomeUtils.isCurrentPackage(packageName)) {
            return;
        }
        List<ItemInfo> apps = getCurForPackage(packageName);
        for (ItemInfo aInfo : apps) {
            if (!data.contains(aInfo)) {
                data.add(aInfo);
                added.add(aInfo);
            }
        }
    }

    /**
     * Remove the data for the given apk identified by packageName.
     */
    public void removePackage(String packageName) {
        List<ItemInfo> data = this.data;
        for (int i = data.size() - 1; i >= 0; i--) {
            ItemInfo info = data.get(i);
            ComponentName component = info.getComponentName();
            if (packageName.equals(component.getPackageName())) {
                removed.add(info);
                if (HomeUtils.DEBUG)
                    Log.d("AllAppsList", "remove add : " + info.getComponentName());
                data.remove(i);
            }
        }
    }

    public void setPackageUnavailable(String packageName) {
        List<ItemInfo> data = this.data;
        for (int i = data.size() - 1; i >= 0; i--) {
            ItemInfo info = data.get(i);
            ComponentName component = info.getComponentName();
            if (packageName.equals(component.getPackageName())) {
                unavailable.add(info);
                data.remove(i);
            }
        }
    }

    public void setPackageAvailable(String packageName) {
        if (HomeUtils.isCurrentPackage(packageName)) {
            return;
        }
        List<ItemInfo> apps = getCurForPackage(packageName);
        for (ItemInfo aInfo : apps) {
            if (!data.contains(aInfo)) {
                data.add(aInfo);
                available.add(aInfo);
            }
        }
    }

    /**
     * Add and remove data for this package which has been updated.
     */
    public void updatePackage(String packageName) {
        if (HomeUtils.isCurrentPackage(packageName)) {
            return;
        }
        List<ItemInfo> curList = getCurForPackage(packageName);// from system
        List<ItemInfo> preList = findPreForPackage(packageName);// from local
        int size = preList.size();
        for (int i = 0; i < size; i++) {
            ItemInfo preItem = preList.get(i);
            if (curList.contains(preItem)) {
                int index = curList.indexOf(preItem);
                if (index >= 0 && index < curList.size()) {
                    modified.add(curList.get(index));
                }
                curList.remove(preItem);
            } else {
                removed.add(preItem);
                data.remove(preItem);
            }
        }
        for (ItemInfo info : curList) {
            added.add(info);
            data.add(info);
        }
    }

    /**
     * Load all data (applications, shortcuts, widgets), and save them to the
     * lists.
     * 
     * @param context
     */
    public void loadAll() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = mPackageManager.queryIntentActivities(mainIntent, 0);
        for (ResolveInfo info : apps) {
            ComponentName name = new ComponentName(info.activityInfo.applicationInfo.packageName,
                    info.activityInfo.name);
            if (HomeUtils.isCurrentPackage(name.getPackageName())) {
                continue;
            }
            AppItemInfo appInfo = new AppItemInfo(mContext, info, name);
            appInfo.mItemType = ItemInfo.ITEM_TYPE_APP;
            data.add(appInfo);
        }
        List<AppWidgetProviderInfo> widgets = AppWidgetManager.getInstance(mContext).getInstalledProviders();
        for (AppWidgetProviderInfo info : widgets) {
            WidgetItemInfo wInfo = new WidgetItemInfo(mContext, info, info.provider);
            wInfo.mItemType = ItemInfo.ITEM_TYPE_WIDGET;
            data.add(wInfo);
        }
        Intent shortcutsIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        List<ResolveInfo> shortcuts = mPackageManager.queryIntentActivities(shortcutsIntent, 0);
        for (ResolveInfo info : shortcuts) {
            ComponentName name = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            ShortcutItemInfo shortcurtInfo = new ShortcutItemInfo(mContext, info, name);
            shortcurtInfo.mItemType = ItemInfo.ITEM_TYPE_SHORTCUT;
            data.add(shortcurtInfo);
        }
    }

    public List<ItemInfo> findPreForPackage(String packageName) {
        List<ItemInfo> found = new ArrayList<ItemInfo>();
        for (ItemInfo item : data) {
            ComponentName component = item.getComponentName();
            if (component.getPackageName().equals(packageName)) {
                found.add(item);
            }
        }
        return found;
    }

    public List<ItemInfo> getCurForPackage(String packageName) {
        List<ItemInfo> get = new ArrayList<ItemInfo>();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(packageName);
        List<ResolveInfo> apps = mPackageManager.queryIntentActivities(mainIntent, 0);
        if (apps != null) {
            for (ResolveInfo info : apps) {
                AppItemInfo itemInfo = new AppItemInfo(mContext, info, new ComponentName(packageName,
                        info.activityInfo.name));
                get.add(itemInfo);
            }
        }
        Intent shortcutsIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        shortcutsIntent.setPackage(packageName);
        List<ResolveInfo> shortcuts = mPackageManager.queryIntentActivities(shortcutsIntent, 0);
        if (shortcuts != null) {
            for (ResolveInfo shortcut : shortcuts) {
                ShortcutItemInfo info = new ShortcutItemInfo(mContext, shortcut, new ComponentName(packageName,
                        shortcut.activityInfo.name));
                get.add(info);
            }
        }
        List<AppWidgetProviderInfo> widgets = AppWidgetManager.getInstance(mContext).getInstalledProviders();
        if (widgets != null) {
            for (AppWidgetProviderInfo widget : widgets) {
                if (widget.provider.getPackageName().equals(packageName)) {
                    WidgetItemInfo wInfo = new WidgetItemInfo(mContext, widget, widget.provider);
                    get.add(wInfo);
                }
            }
        }
        return get;
    }
}
