package com.borqs.se.widget3d;

import android.database.Cursor;
import android.graphics.PointF;
import android.graphics.RectF;
import android.provider.MediaStore;
import android.text.TextUtils;

import android.util.Log;
import com.borqs.framework3d.home3d.*;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEVector;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEScene;
import com.borqs.se.shortcut.LauncherModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class WallShelf extends ConflictVesselObject {
    private static final float DEFAULT_SHELF_HEIGHT = 200f;

    public static float wallShelfBorderHeight = DEFAULT_SHELF_HEIGHT;

    private static final String TAG = "WallShelf";

    private String CURRENT_CONTAINER_NAME = "jiazi_2_point";

    private SETransParas mAttachObjectTransform;

    private int mRowIndex;// the shelf's row index at wall
    private ArrayList<ShelfObjectProperty> mObjectsOnShelf = new ArrayList<ShelfObjectProperty>();

    private boolean mIsCreateFromAddShelfObject;
    ////
    public class ShelfObjectProperty {
        public NormalObject object;
        public int mountPointIndexOnWall;
    }
    public interface ObjectHandler {
        public void run(int mountPointIndex, NormalObject obj);
    }
    ////

    public WallShelf(SEScene scene, String name, int index) {
        super(scene, name, index);
    }
    @Override
    public boolean isNeedAdjustTouch() {
        return false;
    }
    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);

        setVesselLayer(new WallShelfLayer(getScene(), this));
        LauncherModel.getInstance().addAppCallBack(this);
        setHasInit(true);
        if (mAttachObjectTransform == null) {
            mAttachObjectTransform = new SETransParas();
            float[] tData = new float[10];
            String vesselName = getMountPointGroupName();
            if (vesselName != null) {
                scene.getLocalTransformsByObjectName(vesselName, 0, tData);
                mAttachObjectTransform.init(tData);
            }
        }
        int currentshelfIndex = this.mIndex;
        String vesselName = getMountPointGroupName();
        if(vesselName != null) {
            SEObject vesselObject = new SEObject(getScene(), vesselName, currentshelfIndex);
            vesselObject.setIsEntirety_JNI(true);
            vesselObject.setIsAlphaPress(false);
            vesselObject.setOnDoubleClickListener(null);
            vesselObject.setPressedListener(null);
            vesselObject.setUnpressedListener(null);
            addChild(vesselObject, false);
        }
        setOnLongClickListener(new SEObject.OnTouchListener() {
            public void run(SEObject obj) {
                if(!canHandleLongClick()) {
                    return;
                }
                Log.i(TAG, "shelf long click");

                SETransParas startTranspara = new SETransParas();
                startTranspara.mTranslate = getAbsoluteTranslate();
                float angle = getUserRotate().getAngle();
                SEObject parent = getParent();
                while (parent != null) {
                    angle = angle + parent.getUserRotate().getAngle();
                    parent = parent.getParent();
                }
                startTranspara.mRotate.set(angle, 0, 0, 1);
                NormalObject normalObject = (NormalObject)obj;
                SEObjectBoundaryPoint bp = normalObject.getBoundaryPoint();
                if(bp != null && !bp.xyzSpan.equals(SEVector3f.ZERO)) {
                    SEVector3f realSpan = bp.xyzSpan;
                    SEVector3f origSpan = normalObject.getObjectOriginXYZSpan();
                    float scale = realSpan.mD[0] / origSpan.mD[0];
                    startTranspara.mScale = new SEVector3f(scale, 1, 1);
                }
                setStartTranspara(startTranspara);

                setOnMove(true);
            }
        });
        setShadowObjectVisibility_JNI(true);
        this.setOnClickListener(null);
    }
    //////
    public boolean isCreateByAddShelfObject() {
        return mIsCreateFromAddShelfObject;
    }
    public void setIsCreateByAddShelfObject(boolean b) {
        mIsCreateFromAddShelfObject = b;
    }
    private boolean isObjectEqual(NormalObject obj1, NormalObject obj2) {
        SEDebug.myAssert(obj2 != null, "obj2 is null error");
        if(obj1 == null) {
            return false;
        }
        return obj1.equals(obj2);
    }
    public boolean isObjectOnShelf(NormalObject obj) {
        for(ShelfObjectProperty p : mObjectsOnShelf) {
            if(isObjectEqual(p.object, obj)) {
                return true;
            }
        }
        return false;
    }
    public int getRowIndexInWall() {
        return mRowIndex;
    }
    public void createRowIndexInWall(String vesselName) {
        SEMountPointManager mpm = getScene().getMountPointManager();
        SEMountPointChain chain = mpm.getMountPointChain("app", getParent().mName, vesselName, WallLayer.getObjectContainerName());
        int rowNum = chain.getRowCount();
        int colNum = chain.getColCount();
        SEVector3f center = getBoundaryPoint().center;
        float minDist = 1000000;
        int row = -1;
        for(int i = 0 ; i < rowNum ; i++) {
            for(int j = 0 ; j < colNum ; j++) {
                int index = chain.getIndex(i, j);
                SEMountPointData mpd = chain.getMountPointData(index);
                SEVector3f t = mpd.getTranslate();
                float dist = Math.abs(center.mD[2] - t.mD[2]);
                if(dist < minDist) {
                    minDist = dist;
                    row = i;
                }
            }
        }
        mRowIndex = row;
    }
    private void sortShelfObjectProperty() {
        Collections.sort(mObjectsOnShelf, new Comparator<ShelfObjectProperty>() {
            @Override
            public int compare(ShelfObjectProperty shelfObjectProperty1, ShelfObjectProperty shelfObjectProperty2) {
                if(shelfObjectProperty1.mountPointIndexOnWall == shelfObjectProperty2.mountPointIndexOnWall) {
                    return 0;
                } else if(shelfObjectProperty1.mountPointIndexOnWall < shelfObjectProperty2.mountPointIndexOnWall) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
    }
    private int getFirstIndexHasObject() {
        int startIndex = -1;
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            if(p.object != null) {
                startIndex = i;
                break;
            }
        }
        return startIndex;
    }
    private int getLastIndexHasObject() {
        int endIndex = -1;
        for(int i = mObjectsOnShelf.size() - 1 ; i >= 0 ; i--) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            if(p.object != null) {
                endIndex = i;
                break;
            }
        }
        return endIndex;
    }
    public int getFirstMountPointHasObject() {
        int startIndex = getFirstIndexHasObject();
        if(startIndex == -1) {
            return -1;
        }
        ShelfObjectProperty p = mObjectsOnShelf.get(startIndex);
        return p.mountPointIndexOnWall;
    }
    public int getLastMountPointHasObject() {
        int endIndex = getLastIndexHasObject();
        if(endIndex == -1) {
            return -1;
        }
        ShelfObjectProperty p = mObjectsOnShelf.get(endIndex);
        return p.mountPointIndexOnWall;
    }
    @Override
    public boolean canChangeBind() {
        return false;
    }
    //return the shelf width which trim left and right empty place
    public float getWallShelfWidthByTrimLeftRight() {
        int startIndex = getFirstIndexHasObject();
        int endIndex = getLastIndexHasObject();
        if(startIndex == -1 || endIndex == -1) {
            SEDebug.myAssert(false, "shelf is empty error");
            return 0;
        }
        SEVector3f xyzSpan = getObjectOriginXYZSpan();
        int num = endIndex - startIndex + 1;
        return xyzSpan.mD[0] * num;
    }
    public void addObjectsFromShelf(WallShelf newShelf) {
        ArrayList<ShelfObjectProperty> sop = newShelf.mObjectsOnShelf;
        for(ShelfObjectProperty p : sop) {
            mObjectsOnShelf.add(p);
        }
        sortShelfObjectProperty();
    }
    public int getObjectNumOnShelf() {
        return mObjectsOnShelf.size();
    }
    public boolean isNeedExpand() {
        int count1 = getRealObjectNumOnShelf();
        int count2 = getObjectNumOnShelf();
        if(count1 == count2) {
            return true;
        } else {
            return false;
        }
    }
    public boolean moveObjectMountPointToRightFirstNullPlace(int mountPointIndex, ObjectHandler h) {
        int firstNullObjectIndex = -1;
        int startIndex = -1;
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            if(p.mountPointIndexOnWall == mountPointIndex) {
                startIndex = i;
                break;
            }
        }
        if(startIndex == -1) {
            SEDebug.myAssert(false, "startIndex is -1 error");
            return false;
        }
        for(int i = startIndex ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            if(p.object == null) {
                firstNullObjectIndex = i;
                break;
            }
        }
        if(firstNullObjectIndex != -1) {
            for(int i = firstNullObjectIndex ; i > startIndex ; i--) {
                ShelfObjectProperty p1 = mObjectsOnShelf.get(i);
                ShelfObjectProperty p2 = mObjectsOnShelf.get(i - 1);
                p1.object = p2.object;
                if(p1.object != null) {
                    p1.object.getObjectSlot().mMountPointIndex = p1.mountPointIndexOnWall;
                }
                if(h != null) {
                    h.run(p1.mountPointIndexOnWall, p1.object);
                }
            }
            ShelfObjectProperty firstP = mObjectsOnShelf.get(startIndex);
            firstP.object = null;
            return true;
        } else {
            return false;
        }
    }
    public boolean moveObjectMountPointToLeftFirstNullPlace(int mountPointIndex, ObjectHandler h) {
        int firstNullObjectIndex = -1;
        int endIndex = -1;
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            if(p.mountPointIndexOnWall == mountPointIndex) {
                endIndex = i;
                break;
            }
        }
        if(endIndex == -1) {
            SEDebug.myAssert(false, "startIndex is -1 error");
            return false;
        }

        for(int i = endIndex ; i >= 0 ; i--) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            if(p.object == null) {
                firstNullObjectIndex = i;
                break;
            }
        }
        if(firstNullObjectIndex != -1) {
            for(int i = firstNullObjectIndex ; i < endIndex ; i++) {
                ShelfObjectProperty p1 = mObjectsOnShelf.get(i);
                ShelfObjectProperty p2 = mObjectsOnShelf.get(i + 1);
                p1.object = p2.object;
                if(p2.object != null) {
                    p2.object.getObjectSlot().mMountPointIndex = p1.mountPointIndexOnWall;
                }
                if(h != null) {
                    h.run(p1.mountPointIndexOnWall, p1.object);
                }
            }
            ShelfObjectProperty lastP = mObjectsOnShelf.get(endIndex);
            lastP.object = null;
            return true;
        } else {
            return false;
        }
    }
    public void  moveObjectMountPointToLeft(int mountPointIndex, int endIndex, ObjectHandler h) {
        int startShelfPropertyIndex = -1;
        int endShelfPropertyIndex = -1;
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            if(p.mountPointIndexOnWall == mountPointIndex) {
                endShelfPropertyIndex = i;
                break;
            }
        }
        for(int i = 0 ;  i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            if(p.mountPointIndexOnWall == endIndex) {
                startShelfPropertyIndex = i;
                break;
            }
        }
        if(startShelfPropertyIndex == -1 || endShelfPropertyIndex == -1) {
            SEDebug.myAssert(false, "startShelfPropertyIndex == -1 || endShelfPropetyIndex == -1");
            return;
        }
        for(int i = startShelfPropertyIndex ; i < endShelfPropertyIndex ; i++) {
            ShelfObjectProperty p1 = mObjectsOnShelf.get(i);
            ShelfObjectProperty p2 = mObjectsOnShelf.get(i + 1);
            p1.object = p2.object;
            if(p1.object != null) {
                p1.object.getObjectSlot().mMountPointIndex = p1.mountPointIndexOnWall;
                if(h != null) {
                    h.run(p1.mountPointIndexOnWall, p1.object);
                }
            }
        }
        ShelfObjectProperty lastP = mObjectsOnShelf.get(endShelfPropertyIndex);
        lastP.object = null;
        /*
        for(ShelfObjectProperty p : mObjectsOnShelf) {

            if(p.mountPointIndexOnWall <= mountPointIndex && p.mountPointIndexOnWall >= endIndex) {
                p.mountPointIndexOnWall--;
                if(p.object != null) {
                    p.object.getObjectSlot().mMountPointIndex = p.mountPointIndexOnWall;
                    if(h != null) {
                        h.run(p.mountPointIndexOnWall, p.object);
                    }
                }
            }
        }
        */
    }
    public void moveObjectMountPointToRight(int mountPointIndex, int endIndex, ObjectHandler h) {
        int startShelfPropertyIndex = -1;
        int endShelfPropertyIndex = -1;
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            if(p.mountPointIndexOnWall == endIndex) {
                endShelfPropertyIndex = i;
                break;
            }
        }
        for(int i = 0 ;  i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            if(p.mountPointIndexOnWall == mountPointIndex) {
                startShelfPropertyIndex = i;
                break;
            }
        }
        if(startShelfPropertyIndex == -1 || endShelfPropertyIndex == -1) {
            SEDebug.myAssert(false, "startShelfPropertyIndex == -1 || endShelfPropetyIndex == -1");
            return;
        }
        for(int i = endShelfPropertyIndex ; i > startShelfPropertyIndex ; i--) {
            ShelfObjectProperty p1 = mObjectsOnShelf.get(i);
            ShelfObjectProperty p2 = mObjectsOnShelf.get(i - 1);
            p1.object = p2.object;
            if(p1.object != null) {
                p1.object.getObjectSlot().mMountPointIndex = p1.mountPointIndexOnWall;
                if(h != null) {
                    h.run(p1.mountPointIndexOnWall, p1.object);
                }
            }
        }
        ShelfObjectProperty firstP = mObjectsOnShelf.get(startShelfPropertyIndex);
        firstP.object = null;
        /*
        for(ShelfObjectProperty p : mObjectsOnShelf) {
            if(p.mountPointIndexOnWall >= mountPointIndex && p.mountPointIndexOnWall <= endIndex) {
                p.mountPointIndexOnWall++;
                if(p.object != null) {
                    p.object.getObjectSlot().mMountPointIndex = p.mountPointIndexOnWall;
                    if(h != null) {
                        h.run(p.mountPointIndexOnWall, p.object);
                    }
                }
            }
        }
        */
    }
    public void addObjectOnShelf(NormalObject obj, int mountPointIndex) {
        boolean added = false;
        for(ShelfObjectProperty p : mObjectsOnShelf) {
            if(p.mountPointIndexOnWall == mountPointIndex) {
                if(p.object == null) {
                    p.object = obj;
                    added = true;
                } else {
                    added = true;
                }
            }
        }
        if(added){
            return;
        }
        ShelfObjectProperty p = new ShelfObjectProperty();
        p.object = obj;
        p.mountPointIndexOnWall = mountPointIndex;
        mObjectsOnShelf.add(p);
        sortShelfObjectProperty();
    }
    public static final int NOT_ON_SHELF_INDEX = -1;
    //if object is not on shelf return -1;
    public int getObjectIndexOnShelf(NormalObject obj) {
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            if(isObjectEqual(p.object, obj)) {
                return i;
            }
        }
        return NOT_ON_SHELF_INDEX;
    }
    public void insertObjectAtIndexOnShelf(int index, NormalObject obj, int mountPointIndex) {
        if(index == NOT_ON_SHELF_INDEX) {
            ShelfObjectProperty p = new ShelfObjectProperty();
            p.object = obj;
            p.mountPointIndexOnWall = mountPointIndex;
            mObjectsOnShelf.add(p);
            return;
        }
        ShelfObjectProperty p = new ShelfObjectProperty();
        p.object = obj;
        p.mountPointIndexOnWall = mountPointIndex;
        mObjectsOnShelf.add(index, p);
    }
    public NormalObject getObjectOnMountPointIndex(int mountPointIndex) {
        for(ShelfObjectProperty p : mObjectsOnShelf) {
            if(p.mountPointIndexOnWall == mountPointIndex) {
                return p.object;
            }
        }
        return null;
    }
    public int getLeftMostMountPointIndex() {
        return mObjectsOnShelf.get(0).mountPointIndexOnWall;
    }
    public int getRightMostMountPointIndex() {
        return mObjectsOnShelf.get(mObjectsOnShelf.size() - 1).mountPointIndexOnWall;
    }
    public void changeObjectOnShelfToNull(NormalObject object, int mountPointIndex) {
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf. get(i);
            if(isObjectEqual(p.object, object)) {
                if(p.mountPointIndexOnWall == mountPointIndex) {
                    p.object = null;
                    break;
                }
            }
        }
    }
    public int getRealObjectNumOnShelf() {
        int count = 0;
        for(ShelfObjectProperty p : mObjectsOnShelf) {
            if(p.object != null) {
                count++;
            }
        }
        return count;
    }
    public void removeAllObjectOnShelf() {
        mObjectsOnShelf.clear();
    }
    public void updateObjectSlotDB() {
        for(ShelfObjectProperty p : mObjectsOnShelf) {
            if(p.object != null) {
                SEDebug.myAssert(p.object.getObjectSlot().mMountPointIndex == p.mountPointIndexOnWall, "mount point index not same in shelf");
                p.object.updateSlotDB();
            }
        }
    }
    public void removeObjectOnShelf(NormalObject object) {
        int index = -1;
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf. get(i);
            if(isObjectEqual(p.object, object)) {
                index = i;
                break;
            }
        }
        if(index != -1) {
            mObjectsOnShelf.remove(index);
        }
    }
    public ArrayList<Integer> getEmptyObjectMountPoint() {
        ArrayList<Integer> mpList = new ArrayList<Integer>();
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            if(p.object == null) {
                mpList.add(new Integer(p.mountPointIndexOnWall));
            }
        }
        return mpList;
    }
    public void removeAllObjectOnShelfFromParent() {
        for(ShelfObjectProperty p : mObjectsOnShelf) {
            if(p.object != null) {
                p.object.getParent().removeChild(p.object, true);
            }
        }
    }
    public int checkObjectNumOnShelf(NormalObject object) {
        int count = 0;
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            if(isObjectEqual(p.object, object)) {
                count++;
            }
        }
        return count;
    }
    public void checkAllObjectMountPoint() {
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            int count = 0;
            if(p.object != null) {
                int mountPointIndex = p.object.getObjectSlot().mMountPointIndex;
                SEDebug.myAssert(mountPointIndex == p.mountPointIndexOnWall, "mount point index not equal");
            }
        }
        HashMap<Integer, Integer> tmpMap = new HashMap<Integer, Integer>();
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            int mountPointIndex = p.mountPointIndexOnWall;
            Integer n = tmpMap.get(new Integer(mountPointIndex));
            if(n == null) {
                tmpMap.put(new Integer(mountPointIndex), new Integer(1));
            } else {
                SEDebug.myAssert(false, "object has same mount point index error");
            }
        }
    }
    public void printObjects() {
        Log.i(TAG, "################################################");
        Log.i(TAG, "shelf this = " + this);
        Log.i(TAG, "shelf index = " + getObjectInfo().mIndex + ", wallIndex = " + getObjectSlot().mSlotIndex);
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            Log.i(TAG, i + " : " + " mountpoint = " + p.mountPointIndexOnWall + " : object = " + p.object + " : name = " + ((p.object != null) ? p.object.mName : ""));
        }
        Log.i(TAG, "##################################################");
    }
    public void checkAllObjectOnShelf() {
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            int count = 0;
            if(p.object != null) {
                count = checkObjectNumOnShelf(p.object);
                SEDebug.myAssert(count == 1, "object on shelf is not 1 : num = " + count + ", name = " + p.object.mName);

            }
        }
    }
    public NormalObject getObjectByIndex(int i) {
        if(i >= mObjectsOnShelf.size() || i < 0) {
            return null;
        }
        return mObjectsOnShelf.get(i).object;
    }
    public void setMountPointByIndex(int index, int mountPointIndex) {
        if(index >= mObjectsOnShelf.size() || index < 0) {
            return ;
        }
        mObjectsOnShelf.get(index).mountPointIndexOnWall = mountPointIndex;

    }
    public void changeObject(NormalObject oldObject, NormalObject newObject) {
        for(int i = 0 ; i < mObjectsOnShelf.size() ; i++) {
            ShelfObjectProperty p = mObjectsOnShelf.get(i);
            if(p.object == oldObject) {
                p.object = newObject;
            }
        }
    }
    public ArrayList<NormalObject> getObjectsOnShelf() {
        ArrayList<NormalObject> objects = new ArrayList<NormalObject>();
        for(ShelfObjectProperty p : mObjectsOnShelf) {
            if(p.object != null) {
                objects.add(p.object);
            }
        }
        return objects;
    }
    public boolean hasObject(NormalObject object) {
        for(ShelfObjectProperty p : mObjectsOnShelf) {
            if(isObjectEqual(p.object, object)) {
                return true;
            }
        }
        return false;
    }
    public boolean hasObjectWithMountPointIndex(int mountPointIndex) {
        for(ShelfObjectProperty p : mObjectsOnShelf) {
            if(p.mountPointIndexOnWall == mountPointIndex && p.object != null) {
                return true;
            }
        }
        return false;
    }
    public boolean containMountPointIndex(int mountPointIndex) {
        for(ShelfObjectProperty p : mObjectsOnShelf) {
            if(p.mountPointIndexOnWall == mountPointIndex) {
                return true;
            }
        }
        return false;
    }
    @Override
    public void handOnClick() {
        super.handOnClick();
    }

    @Override
    public void onSlotSuccess() {
        setIsFresh(false);
        super.onSlotSuccess();
    }
    @Override
    public boolean canChildHandleLongClick(NormalObject child) {
        House parent = (House)this.getParent();
        int currentWallIndex = parent.getWallNearestIndex();
        int currentShelfSlotIndex = getObjectSlot().mSlotIndex;
        if(currentShelfSlotIndex == currentWallIndex) {
            return true;
        } else {
            return false;
        }
    }
    public void getBoundaryPointInWorldSpace(SEVector3f worldMinPoint, SEVector3f worldMaxPoint) {
    	SEVector3f minPoint = new SEVector3f();
    	SEVector3f maxPoint = new SEVector3f();
    	this.getBoundaryPointInObjectSpace(minPoint, maxPoint);
    	SEVector3f v1 = this.toWorldPoint(minPoint);
    	SEVector3f v2 = this.toWorldPoint(maxPoint);
    	for(int i = 0 ; i < 3 ; i++) {
    		worldMinPoint.mD[i] = v1.mD[i];
    		worldMaxPoint.mD[i] = v2.mD[i];
    	}
    	
    }
    private String getPath(String videoId) {
        String path = null;
        if (TextUtils.isEmpty(videoId)) {
            return path;
        }
        String[] projection = new String[]{MediaStore.Video.Media.DATA};
        String where = MediaStore.Video.Media._ID + "=" + videoId;
        Cursor cursor = getContext().getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, where, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    @Override
    public void onRelease() {
        super.onRelease();
    }


    private SEVector.SEVector3f calculateTranslate(SEMountPointData mpd) {
        SEVector.SEVector3f t = mpd.getTranslate();
//        SEVector.SEVector3f ret = t.add(mAttachObjectTransform.mTranslate);
        HouseObject house = ModelInfo.getHouseObject(getScene());
        final float zOffset = AppObject.getAppIconWidth(getContext()) / 2;
//        ret.selfAdd(new SEVector3f(0, 0, zOffset));
        SEVector.SEVector3f ret = t.add(new SEVector3f(0, 0, zOffset));
        return ret;
    }

    private SEVector.SEVector3f calculateSlotPosition(String objectName, int slotIndex) {
        SEMountPointChain mpc = this.getMountPointChain(objectName);
        int index = slotIndex;
        if (index >= 0 && index < mpc.getMountPointCount()) {
            SEMountPointData mpd = mpc.getMountPointData(index);
            SEVector.SEVector3f t = calculateTranslate(mpd);
            return t;
        } else {
            return new SEVector.SEVector3f(0, 0, 0);
        }
    }
    private int getShelfObjectMountPointIndexInWall(House house, String objName, int indexInShelf) {
        int shelfIndex = getObjectInfo().mIndex;
        int wallIndex = this.getObjectSlot().mSlotIndex;
        SEMountPointChain mpc = house.getWallObjectMountPointChain(objName, wallIndex);
        int colNum = mpc.getColCount();
        if(shelfIndex == 1 || shelfIndex == 2 || shelfIndex == 3) {
            int mountPointIndexInWall = (shelfIndex - 1) * colNum + indexInShelf;
            return mountPointIndexInWall;
        } else if(shelfIndex == 4 || shelfIndex == 5 || shelfIndex == 6) {
            shelfIndex -= 3;
            int mountPointIndexInWall = (shelfIndex - 1) * colNum + colNum - 2 + indexInShelf;
            return mountPointIndexInWall;
        } else {
            SEDebug.myAssert(false, "shelf index must <= 6");
            return 0;
        }
    }
    @Override
    public SETransParas getSlotTransParas(ObjectInfo objectInfo, NormalObject object) {

        House house = (House)getParent();
        int index = objectInfo.mObjectSlot.mMountPointIndex;
        int mountPointIndexInWall = getShelfObjectMountPointIndexInWall(house, object.mName, index);
        int wallIndex = this.getObjectSlot().mSlotIndex;
        objectInfo.mObjectSlot.mSlotIndex = wallIndex;
        objectInfo.mObjectSlot.mMountPointIndex = mountPointIndexInWall;
        return house.getSlotTransParas(objectInfo, object);
        /*
        SETransParas transParas = new SETransParas();
        int index = objectInfo.mObjectSlot.mMountPointIndex;
        if (index < 0 || index >= mpc.getMountPointCount()) {
            if (HomeUtils.DEBUG) {
                Log.e(TAG, "getSlotTransParas, set 0 replace invalid index = " + objectInfo.mObjectSlot.mMountPointIndex);
            }
            index = 0;
        }
        object.getObjectInfo().mSlotType = ObjectInfo.SLOT_TYPE_WALL_SHELF;
        SEMountPointData mpd = mpc.getMountPointData(index);
        transParas.mTranslate = calculateTranslate(mpd);
        //SEObjectBoundaryPoint bp = objectInfo.mObjectSlot.mBoundaryPoint.clone();
        //object.setBoundaryPoint(bp);
        //TODO: add rotate or scale for object
        //transParas.mTranslate = getSlotPosition(objectInfo.mObjectSlot);
        return transParas;
        */
    }

    public SEMountPointChain.ClosestMountPointData calculateNearestMountPoint(SEObject object, SEVector.SEVector3f tmpPoint) {
        SEMountPointChain mpc = this.getMountPointChain(object.mName);
        assert (mpc != null);
        float[] locationPoints = new float[4];
        float[] outLocPoints = new float[4];
        for (int i = 0; i < 3; i++) {
            locationPoints[i] = tmpPoint.mD[i];
        }
        locationPoints[3] = 1;
        SEMountPointManager mpm = getScene().getMountPointManager();
        String vesselName = mpm.getMountPointGroupName(mName);
        SEObject vesselObject = getScene().findObject(vesselName, mIndex);
        vesselObject.toObjectCoordinate(locationPoints, outLocPoints);
        SEVector.SEVector3f point = new SEVector.SEVector3f(outLocPoints[0], outLocPoints[1], outLocPoints[2]);
        ArrayList<SEMountPointData> mpList = mpc.getMountPointList();
        float minDist = 10000000;
        SEMountPointData retMPD = null;
        int index = -1;
        for (int i = 0; i < mpList.size(); i++) {
            SEMountPointData mpd = mpList.get(i);
            SEVector.SEVector3f t = mpd.getTranslate();//calculateTranslate(mpd);
            float dist = t.dist(point);
            if (dist < minDist) {
                minDist = dist;
                retMPD = mpd;
                index = i;
            }
        }
        if(index == -1) {
        	Log.i(TAG, "## index == -1 ##");
        }
        Log.i(TAG, "### index = " + index + " ####");
        Log.i(TAG, "#### mpd = " + retMPD + " ####");
        Log.i(TAG, "## mpList size = " + mpList.size() + " ###");
        SEMountPointChain.ClosestMountPointData d = new SEMountPointChain.ClosestMountPointData();
        d.mMPD = retMPD;
        d.mIndex = index;
        d.mDist = minDist;
        return d;

    }

    public ObjectInfo.ObjectSlot calculateNearestSlot(SEVector.SEVector3f location, boolean handup) {
        return null;
    }
    private boolean isMountPointSlotOccupied(int mountPointIndex) {
    	for (SEObject object : mChildObjects) {
            if (object instanceof NormalObject) {
                NormalObject objectOnShelf = (NormalObject) object;
                ObjectInfo objInfo = objectOnShelf.getObjectInfo();
                if(objInfo.getSlotIndex() == mountPointIndex) {
                	return true;
                }
            }
    	}
    	return false;
    }
    private int getAllObjectInShelf() {
    	int count = 0;
    	for (SEObject object : mChildObjects) {
            if (object instanceof NormalObject) {
                NormalObject objectOnShelf = (NormalObject) object;
                ObjectInfo objInfo = objectOnShelf.getObjectInfo();
                SEDebug.myAssert(objInfo.getSlotIndex() >= 0, "mount point index is -1");
                count++;
            }
    	}
    	return count;
    }
    public int  calculateNearestMountPointIndex(SEObject object, SEVector.SEVector3f location, boolean handup) {
        SEMountPointChain.ClosestMountPointData closestMountPoint = this.calculateNearestMountPoint(object, location);
        return closestMountPoint.mIndex;
    }

    private List<ObjectInfo.ObjectSlot> searchEmptySlot(List<ConflictObject> existentSlot) {
        boolean[] slot = new boolean[getCount()];
        for (int i = 0; i < getCount(); i++) {
            slot[i] = true;
        }
        for (ConflictObject desktopObject : existentSlot) {
            slot[desktopObject.getSlotIndex()] = false;
        }

        List<ObjectInfo.ObjectSlot> objectSlots = null;
        for (int i = 0; i < getCount(); i++) {
            if (slot[i]) {
                if (objectSlots == null) {
                    objectSlots = new ArrayList<ObjectInfo.ObjectSlot>();
                }
                ObjectInfo.ObjectSlot objectSlot = new ObjectInfo.ObjectSlot();
                objectSlot.mSlotIndex = i;
                objectSlots.add(objectSlot);
            }
        }
        return objectSlots;
    }

    public ConflictObject getConflictSlot(ObjectInfo.ObjectSlot cmpSlot) {
        return null;
    }

    public NormalObject getConflictedObject(SEObject moveObject, int mountPointIndex) {
        for (SEObject object : mChildObjects) {
            if (object instanceof NormalObject) {
                NormalObject dockObject = (NormalObject) object;
                ObjectInfo objInfo = dockObject.getObjectInfo();
                if (!object.equals(moveObject) &&
                        objInfo.mObjectSlot.mMountPointIndex >= 0) {
                    if (objInfo.mObjectSlot.mMountPointIndex == mountPointIndex) {
                        return dockObject;
                    }
                }
            }
        }
        return null;
    }

    public ConflictObject getConflictSlot(SEObject moveObject, int mountPointIndex) {

        NormalObject obj = getConflictedObject(moveObject, mountPointIndex);
        if (obj == null) {
            return null;
        }
        ConflictObject conflictObj = new ConflictObject(this, obj, null);
        //conflictObj.cloneSlot();
        //int movedIndex = this.getEmptySlotIndex(moveObject, mountPointIndex);
        //conflictObj.setMovingSlotIndex(movedIndex);
        return conflictObj;

    }
    
    public SEVector.SEVector3f getSlotPosition(String objectName, int slotIndex) {
        return calculateSlotPosition(objectName, slotIndex);
    }

    public SEVector.SEVector3f getSlotPosition(ObjectInfo.ObjectSlot objectSlot) {
                /*
            float angle = objectSlot.mSlotIndex * 360.f / getCount();
            return getAnglePosition(angle);
            */
        return null;
    }

    @Override
    public float getBorderHeight() {
        if (mAttachObjectTransform != null) {
            return Math.max(wallShelfBorderHeight, mAttachObjectTransform.mTranslate.getZ());
        } else {
            return Math.max(wallShelfBorderHeight, DEFAULT_SHELF_HEIGHT);
        }
    }


    private List<ConflictObject> mExistentSlot;

    public List<ConflictObject> getExistentSlot(SEObject movingObject) {
        if (null == mExistentSlot) {
            mExistentSlot = new ArrayList<ConflictObject>();
        } else {
            mExistentSlot.clear();
        }

        List<ConflictObject> fillSlots = mExistentSlot;
        for (SEObject object : mChildObjects) {
            if (object instanceof NormalObject) {
                NormalObject objectOnShelf = (NormalObject) object;
                ObjectInfo objInfo = objectOnShelf.getObjectInfo();
                if (!object.equals(movingObject) && objInfo.getSlotIndex() >= 0) {
                    ConflictObject conflictObject = new ConflictObject(this, objectOnShelf, null);
                    fillSlots.add(conflictObject);
                }
            }
        }

        return fillSlots;
    }

    private DockObject.ConflictAnimationTask mPlayConflictAnimationTask;

    public void playConflictAnimationTask(ConflictObject conflictObject, long delay, final SEAnimFinishListener postExecutor) {
        cancelConflictAnimationTask();
        if (conflictObject != null) {
            mPlayConflictAnimationTask = new DockNew.ConflictAnimationTask(conflictObject, postExecutor);
            if (delay == 0) {
                mPlayConflictAnimationTask.run();
            } else {
                SELoadResThread.getInstance().process(mPlayConflictAnimationTask, delay);
            }
        }
    }

    public int getCount() {
        SEMountPointChain mpc = this.getMountPointChain(mName);
        return mpc.getMountPointCount();
    }

    public void cancelConflictAnimationTask() {
        if (mPlayConflictAnimationTask != null) {
            SELoadResThread.getInstance().cancel(mPlayConflictAnimationTask);
            mPlayConflictAnimationTask = null;
        }
    }

    private SEMountPointChain getMountPointChain(String objectName) {

        SEMountPointManager mountPointManager = getScene().getMountPointManager();
        return mountPointManager.getMountPointChain(objectName, mName, null, CURRENT_CONTAINER_NAME);
    }
    //
    public int getMountPointIndexExceptDest(NormalObject needMoveObject,int destSlotIndex) {
        ArrayList<Integer> indexList = new ArrayList<Integer>();
    	for(SEObject object : mChildObjects) {
    		if(object instanceof NormalObject) {
    			NormalObject normalObj = (NormalObject)object;
				ObjectInfo objInfo = normalObj.getObjectInfo();
				if(objInfo.mObjectSlot.mMountPointIndex >= 0 && objInfo.mObjectSlot.mMountPointIndex != destSlotIndex) {
					indexList.add(Integer.valueOf(objInfo.mObjectSlot.mMountPointIndex));
				}
    		}
    	}
    	indexList.add(Integer.valueOf(destSlotIndex));
    	SEMountPointChain currentChain = this.getMountPointChain(needMoveObject.mName);
    	for(int i = 0 ; i < currentChain.getMountPointCount() ; i++) {
    		boolean b = indexList.contains(Integer.valueOf(i));
    		if(b == false) {
    			return i;
    		}
    	}
    	return -1;
    }
    private int getEmptySlotIndex(SEObject moveObject, int destSlotIndex) {
        ArrayList<ObjectInfo.ObjectSlot> objSlotList = new ArrayList<ObjectInfo.ObjectSlot>();
        for (SEObject object : mChildObjects) {
            if (object instanceof NormalObject) {
                NormalObject dockObject = (NormalObject) object;
                ObjectInfo objInfo = dockObject.getObjectInfo();
                if (objInfo.mSlotType == ObjectInfo.SLOT_TYPE_DESKTOP && !object.equals(moveObject) &&
                        objInfo.getSlotIndex() >= 0) {
                    objSlotList.add(objInfo.mObjectSlot);
                }
            }
        }
        ObjectInfo.ObjectSlot slot = new ObjectInfo.ObjectSlot();
        slot.mSlotIndex = 0;
        objSlotList.add(slot);
        ArrayList<Integer> emptySlotIndexList = new ArrayList<Integer>();
        for (int i = 1; i < getCount(); i++) {
            if (this.slotIndexInArray(i, objSlotList) == false) {
                emptySlotIndexList.add(new Integer(i));
            }
        }
        int movedObjectSlotIndex = ((NormalObject) moveObject).getObjectInfo().mObjectSlot.mSlotIndex;
        emptySlotIndexList.add(new Integer(movedObjectSlotIndex));

        SEVector.SEVector3f destPosition = getSlotPosition(moveObject.mName, destSlotIndex);
        float minDist = 1000000;
        int retIndex = 0;
        for (Integer slotIndex : emptySlotIndexList) {
            int index = slotIndex.intValue();
            SEVector.SEVector3f tmpPosition = getSlotPosition(moveObject.mName, index);
            float dist = tmpPosition.dist(destPosition);
            if (minDist > dist) {
                retIndex = index;
                minDist = dist;
            }
        }
        return retIndex;
    }

    private boolean slotIndexInArray(int slotIndex, ArrayList<ObjectInfo.ObjectSlot> array) {
        for (ObjectInfo.ObjectSlot slot : array) {
            if (slot.mSlotIndex == slotIndex) {
                return true;
            }
        }
        return false;
    }

    @Override
    public SEAnimation.CountAnimation createConflictAnimation(NormalObject conflictObject, ObjectInfo.ObjectSlot slot, int step) {
        ConflictAnimation anim = new ConflictAnimation(this, conflictObject, slot, step);
        SEMountPointManager mountPointManager = getScene().getMountPointManager();
        SEDockAnimationDefine define = mountPointManager.getDockAnimationDefine(mName);
        anim.setAnimCount(define.getAnimationCount());
        anim.setTraceType(define.getAnimationTrace());
        return anim;
    }
    private void getBoundaryPointInObjectSpace(SEVector3f minPoint , SEVector3f maxPoint) {
    	SEVector.SEVector3f minV = new SEVector.SEVector3f();
        SEVector.SEVector3f maxV = new SEVector.SEVector3f();
        createLocalBoundingVolume();
        getLocalBoundingVolume(minV, maxV);
        maxV.mD[2] += getBorderHeight();
        for(int i = 0 ; i < 3 ; i++) {
        	minPoint.mD[i] = minV.mD[i];
        	maxPoint.mD[i] = maxV.mD[i];
        }
    }
    public boolean canHandleSlot(NormalObject object, SEVector3f worldLocation) {
    	SEVector.SEVector3f minV = new SEVector.SEVector3f();
        SEVector.SEVector3f maxV = new SEVector.SEVector3f();
        this.getBoundaryPointInObjectSpace(minV, maxV);
        SEVector3f locationInObject = this.toObjectPoint(worldLocation);
        boolean b = locationInObject.getX() >= minV.getX() && locationInObject.getX() <= maxV.getX() &&
        		    locationInObject.getZ() >= minV.getZ() && locationInObject.getZ() <= maxV.getZ();
        return b;
    }
    public boolean canHandleSlot(NormalObject object, int targetIndex, float x, float y) {
        if (getVesselLayer().canHandleSlot(object)) {
            if (targetIndex == getObjectInfo().getSlotIndex()) {
                RectF bounder = calculateGeometry();
                if (bounder.contains(x, y)) {
                    if (HomeUtils.DEBUG) {
                        Log.d(TAG, "canHandleSlot, hit x = " + x + ", y = " + y
                                + ", bounder = " + bounder + ", object name " + object.getName());
                    }
                    return true;
                }
            } else {
//                if (HomeUtils.DEBUG) {
//                    Log.v(TAG, "canHandleSlot, skip mismatch slot index, target = " + targetIndex
//                            + ", current = " + getObjectInfo().getSlotIndex());
//                }
            }
        }
//        if (getVesselLayer().canHandleSlot(object)) {
//            float[] worldLocation = new float[4];
//            worldLocation[0] = x;
//            worldLocation[1] = 1;
//            worldLocation[2] = z;
//            worldLocation[3] = 1;
//
//            float[] objectLocation = new float[4];
//            toObjectCoordinate(worldLocation, objectLocation);
//            SEVector.SEVector3f minV = new SEVector.SEVector3f();
//            SEVector.SEVector3f maxV = new SEVector.SEVector3f();
//
//            createLocalBoundingVolume();
//            getLocalBoundingVolume(minV, maxV);
//
//            if (HomeUtils.DEBUG) {
//                Log.i(TAG, "canHandleSlot, x = " + x + ", z = " + z + ", newX = "
//                        + objectLocation[0] + ", newZ = " + objectLocation[2]
//                        + ", min = " + minV.toString() + ", max = " + maxV.toString());
//            }
//            if (isRange(objectLocation[0], Math.min(minV.getX(), maxV.getX()), Math.max(minV.getX(), maxV.getX())) &&
//                    isRange(objectLocation[2], Math.min(minV.getZ(), maxV.getZ()),  Math.max(minV.getZ(), maxV.getZ())+ getBorderHeight())) {
//                if(HomeUtils.DEBUG) {
//                    Log.i(TAG, "canHandleSlot got :" + getName());
//                }
//                return true;
//            }
//
//        }
        return false;
    }

    private static boolean isRange(float cur, float min, float max) {
        float dMin = Math.abs(cur - min);
        float dMax = Math.abs(cur - max);
        float dis = Math.abs(min - max);
        if (dMax + dMin <= dis) {
            return true;
        }

        return false;
    }

    private RectF mXzBounder;
    private RectF calculateGeometry() {
        if (mXzBounder == null) {
            SEVector.SEVector3f minV = new SEVector.SEVector3f();
            SEVector.SEVector3f maxV = new SEVector.SEVector3f();

            createLocalBoundingVolume();
            getLocalBoundingVolume(minV, maxV);

            PointF pMin = toWorldXzPoint(minV);
            PointF pMax = toWorldXzPoint(maxV);
            mXzBounder = new RectF(pMin.x, pMin.y, pMax.x, pMax.y + getBorderHeight());

            if (HomeUtils.DEBUG) {
                Log.i(TAG, "calculateGeometry, min = " + minV.toString() + ", max = " + maxV.toString()
                        + "bounder in world coordinate: " + mXzBounder.toString());
            }
        }
        return mXzBounder;
    }

    private PointF toWorldXzPoint(SEVector.SEVector3f spaceCoordinate) {
        float[] target = new float[4];
        float[] source = new float[4];
        source[3] = 1;
        for (int i = 0; i < 3; i++) {
            source[i] = spaceCoordinate.mD[i];
        }

        toWorldCoordinate(source, target);
        return new PointF(target[0], target[2]);
    }
}
