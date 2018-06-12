package com.borqs.se.widget3d;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.database.Cursor;

import com.borqs.framework3d.home3d.SEMountPointChain;
import com.borqs.framework3d.home3d.SEMountPointManager;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SECommand;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.ProviderUtils.ObjectInfoColumns;
import com.borqs.se.home3d.ProviderUtils.VesselColumns;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.shortcut.ItemInfo;
import com.borqs.se.shortcut.LauncherModel;
import com.borqs.se.shortcut.LauncherModel.AppCallBack;

public abstract class VesselObject extends NormalObject implements AppCallBack {

    public VesselLayer mVesselLayer;

    public VesselObject(SEScene scene, String name, int index) {
        super(scene, name, index);
    }

    public VesselLayer getVesselLayer() {
        return mVesselLayer;
    }

    public void setVesselLayer(VesselLayer vesselLayer) {
        mVesselLayer = vesselLayer;
    }

    public abstract SETransParas getSlotTransParas(ObjectInfo objectInfo, NormalObject object);

    public void onLoadFinished() {

    }

    @Override
    public boolean load(SEObject parent, final Runnable finish) {
        super.load(parent, new Runnable() {
            public void run() {
                loadChild(finish);
            }
        });
        return true;
    }

    protected SEVector3f getVesselScale() {
    	return NO_SCALE;
    }

    private SEMountPointChain getObjectMountPointChain(ObjectInfo objectInfo) {
        SEMountPointManager mpm = getScene().getMountPointManager();
        String objName = objectInfo.mName;
        int slotType = objectInfo.mSlotType;
        SEMountPointChain chain = null;
        if(slotType == ObjectInfo.SLOT_TYPE_WALL_SHELF || slotType == ObjectInfo.SLOT_TYPE_WALL) {
            chain = mpm.getMountPointChain(objName, mName, null, WallLayer.getObjectContainerName());
        } else if(slotType == ObjectInfo.SLOT_TYPE_DESKTOP) {
            chain = mpm.getMountPointChain(objName, mName, null, DockNew.getContainerName());
        }
        return chain;
    }

    private boolean loadChild(final Runnable finish) {
        String where = VesselColumns.VESSEL_ID + "=" + getObjectInfo().mID + " and " + ObjectInfoColumns.ORIENTATION_STATUS +
                " = " + SettingsActivity.getPreferRotation(getContext());
        Cursor cursor = getContext().getContentResolver().query(ObjectInfoColumns.OBJECT_LEFT_JOIN_ALL, null, where, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ObjectInfo objectInfo = ObjectInfo.CreateFromDB(cursor);
                SEMountPointChain chain = getObjectMountPointChain(objectInfo);
                if (objectInfo.isValidateMountPointData(chain)) {
                    objectInfo.setModelInfo(getScene().mSceneInfo.findModelInfo(objectInfo.mName));
                    NormalObject object = HomeUtils.getObjectByClassName(getScene(), objectInfo);
                    addChild(object, false);
                } else {
                    objectInfo.releaseDB();
                }
            }
            cursor.close();
        }
        if (mChildObjects.size() > 0) {
            List<SEObject> childObjects = new ArrayList<SEObject>();
            childObjects.addAll(mChildObjects);
            loadObjectsOneByOne(childObjects, 0, finish);
        } else {
            onLoadFinished();
            if (finish != null) {
                finish.run();
            }
        }
        return true;
    }

    private void loadObjectsOneByOne(final List<SEObject> childObjects, final int count, final Runnable finish) {
        SEObject child = childObjects.get(count);
        if (child instanceof NormalObject) {
            final NormalObject object = (NormalObject) child;
            object.load(this, new Runnable() {
                public void run() {
                    int newCount = count + 1;
                    if (newCount < childObjects.size()) {
                        loadObjectsOneByOne(childObjects, newCount, finish);
                    } else {
                        onLoadFinished();
                        if (finish != null) {
                            finish.run();
                        }
                    }
                }
            });

        } else {
            int newCount = count + 1;
            if (newCount < childObjects.size()) {
                loadObjectsOneByOne(childObjects, newCount, finish);
            } else {
                onLoadFinished();
                if (finish != null) {
                    finish.run();
                }
            }
        }
    }

    public void bindAppsAdded(List<ItemInfo> apps) {
        // TODO Auto-generated method stub
    	bindAppsUpdated(apps);
    }

    public void bindAppsUpdated(List<ItemInfo> apps) {
        final List<ItemInfo> myApps = new ArrayList<ItemInfo>();
        myApps.addAll(apps);
        new SECommand(getScene()) {
            public void run() {
                for (ItemInfo info : myApps) {
                    List<NormalObject> matchApps = findAPP(info.getComponentName(), ModelInfo.Type.APP_ICON);
                    for (NormalObject object : matchApps) {
                        object.update(getScene());
                    }
                }
            }
        }.execute();
    }

    public void bindAppsRemoved(List<ItemInfo> apps) {
        final List<ItemInfo> myApps = new ArrayList<ItemInfo>();
        myApps.addAll(apps);
        new SECommand(getScene()) {
            public void run() {
                for (ItemInfo info : myApps) {
                    List<NormalObject> matchApps = findAPP(info.getComponentName(), null);
                    for (NormalObject object : matchApps) {
                        final ObjectInfo objectInfo = object.getObjectInfo();
                        objectInfo.releaseDB();
                        object.getParent().removeChild(object, true);
                    }
                }
            }
        }.execute();
    }

    public void bindAllPackagesUpdated() {
        SESceneManager.getInstance().debugOutput("VesselObject bindAllPackagesUpdated enter.");
        final List<ItemInfo> myApps = new ArrayList<ItemInfo>();
        myApps.addAll(LauncherModel.getInstance().getAllDatas());
        new SECommand(getScene()) {
            public void run() {
                List<NormalObject> matchApps = findAPP(null, null);
                for (NormalObject object : matchApps) {
                    final ObjectInfo objectInfo = object.getObjectInfo();
                    if (objectInfo.mComponentName != null && isComponentNameExist(myApps, objectInfo.mComponentName)) {
                        object.update(getScene());
                    }
                }
                SESceneManager.getInstance().debugOutput("VesselObject bindAllPackagesUpdated completed.");
            }
        }.execute();
        SESceneManager.getInstance().debugOutput("VesselObject bindAllPackagesUpdated exit.");
    }

    public void bindAllPackages() {
        // TODO Auto-generated method stub

    }

    @Override
    public void bindUnavailableApps(List<ItemInfo> apps) {

    }

    @Override
    public void bindAvailableApps(List<ItemInfo> apps) {
        final List<ItemInfo> myApps = new ArrayList<ItemInfo>();
        myApps.addAll(apps);
        new SECommand(getScene()) {
            public void run() {
                for (ItemInfo info : myApps) {
                    List<NormalObject> matchApps = findAPP(info.getComponentName(), null);
                    for (NormalObject object : matchApps) {
                        object.update(getScene());
                    }
                }
            }
        }.execute();
    }

    private boolean isComponentNameExist(List<ItemInfo> allApps, ComponentName componentName) {
        for (ItemInfo item : allApps) {
            if (componentName.equals(item.getComponentName())) {
                return true;
            }
        }
        return false;
    }

    public List<NormalObject> findAPP(ComponentName componentName, String myType) {
        List<NormalObject> newItems = new ArrayList<NormalObject>();
        for (SEObject item : mChildObjects) {
            if (item instanceof NormalObject) {
                NormalObject appObject = (NormalObject) item;
                String type = appObject.getObjectInfo().mType;
                boolean flag = false;
                if (myType == null) {
                    flag = ModelInfo.isAppObjectItemType(type);
                } else {
                    flag = myType.equals(type);
                }
                
                
                if (flag) {
                    if (componentName != null) {
                        if (componentName.equals(appObject.getObjectInfo().mComponentName)) {
                            newItems.add(appObject);
                        }
                    } else {
                        newItems.add(appObject);
                    }
                }
            }
        }
        return newItems;
    }

    /// urgent ugly patch, should be configure within model config.
    public boolean isChildrenShadowAllowed() {
        return true;
    }
}