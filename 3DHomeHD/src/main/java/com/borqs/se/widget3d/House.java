package com.borqs.se.widget3d;


import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import com.borqs.framework3d.home3d.HouseObject;
import com.borqs.framework3d.home3d.SEDebug;
import com.borqs.framework3d.home3d.SEMountPointChain;
import com.borqs.framework3d.home3d.SEMountPointData;
import com.borqs.framework3d.home3d.SEMountPointManager;
import com.borqs.framework3d.home3d.SEObjectBoundaryPoint;
import com.borqs.market.wallpaper.WallpaperUtils;
import com.borqs.se.download.DownloadChangeReceiver;
import com.borqs.se.download.DownloadChangeReceiver.selectObjectListener;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SEBitmap;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectFactory;
import com.borqs.se.engine.SEObjectGroup;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SESceneInfo;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.engine.SESceneManager.UnlockScreenListener;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEVector;
import com.borqs.se.engine.SEVector.SERay;
import com.borqs.se.engine.SEVector.SERect3D;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEVector.SEVector2f;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.home3d.HomeApplication;
import com.borqs.se.home3d.HomeDataBaseHelper;
import com.borqs.se.home3d.HomeScene;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.ModelInfo.ImageInfo;
import com.borqs.se.home3d.ModelInfo.ImageItem;
import com.borqs.se.home3d.ProviderUtils;
import com.borqs.se.home3d.ProviderUtils.ModelColumns;
import com.borqs.se.home3d.ProviderUtils.Tables;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.shortcut.AppItemInfo;
import com.borqs.se.shortcut.LauncherModel;
import com.borqs.se.shortcut.LauncherModel.ShortcutCallBack;
import com.borqs.se.shortcut.WidgetWorkSpace;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class House extends HouseObject implements UnlockScreenListener, ShortcutCallBack, selectObjectListener {

	private static final String TAG = "House";
    private static String FEATURE_LIGHT = "light";

    private boolean mFeatureLight = true;

    private SEObject mLight;

    public String mCurrentTheme;

    private VelocityTracker mVelocityTracker;
    private WallVelocityAnimation mWallVelocityAnimation;
    private ToFaceAnimation mToFaceAnimation;
    private int mPreFaceIndex;
    private int mWallNum;
    private ArrayList<WallRadiusChangedListener> mWallRadiusChangedListeners;

    private RunACircleAnimation mRunACircleAnimation;
    private boolean mIsRunningCircleAnimation;
    private SESceneInfo mSceneInfo;
    private boolean mOnMoveSight;
    private boolean mCancelClick;
    private SESceneManager mSESceneManager;

    private WallDialog mWallDialog;
    //for wallpaper
    private int mImgSizeX, mImgSizeY;
    private String mCurrentImage;
    private File mSdcardTempFile;
    private static final int TYPE_FLOOR = 0;
    private static final int TYPE_WALL  = 1;

    //TODO: Becuase that the XML has not define the housetop obj name. So we only define them by hardcode.
    // need resign the XML in future.
//    private static final String WU_DING_OBJ_NAME_1 = "wuding01@group_house8@home8_basedata.cbf";
//    private static final String WU_DING_OBJ_NAME_2 = "wuding02@group_house8@home8_basedata.cbf";

    private ArrayList<String> WALL_OBJNAMES = new ArrayList<String>();
    private ArrayList<String> CEIL_OBJNAMES = new ArrayList<String>();
    private String CEIL_LIGHT_OBJ_NAME;
    private String GROUND_OBJ_NAME;
    private ArrayList<WallChangedListener> mWallChangedListeners;
    public House(SEScene scene, String name, int index) {
        super(scene, name, index);
        DownloadChangeReceiver.registerSelectObjectListener(getClass().getName(), this);
        mSESceneManager = SESceneManager.getInstance();
        mWallRadiusChangedListeners = new ArrayList<WallRadiusChangedListener>();
        mWallChangedListeners = new ArrayList<WallChangedListener>();
    }
    public ArrayList<ArrayList<WallShelf>> getWallShelfsInWall() {
        int num = WALL_OBJNAMES.size();
        ArrayList<ArrayList<WallShelf>> shelfs = new ArrayList<ArrayList<WallShelf>>();
        for(int i = 0 ; i < num ; i++) {
            ArrayList<WallShelf> shelfList = new ArrayList<WallShelf>();
            shelfs.add(shelfList);
        }
        for(SEObject obj : mChildObjects) {
            if(!(obj instanceof  WallShelf)) {
                continue;
            }
            WallShelf shelf = (WallShelf) obj;
            for(int wallIndex = 0 ; wallIndex < num ; wallIndex++) {
                if(shelf.getObjectSlot().mSlotIndex == wallIndex) {
                    ArrayList<WallShelf> shelfList = shelfs.get(wallIndex);
                    shelfList.add(shelf);
                }
            }
        }
        return shelfs;
    }
    public ArrayList<NormalObject> getNormalObjectInWall(int wallIndex) {
        ArrayList<NormalObject> objects = new ArrayList<NormalObject>();
        for(SEObject obj : mChildObjects) {
            if(obj instanceof NormalObject) {
                if(((NormalObject) obj).getObjectSlot().mSlotIndex == wallIndex) {
                    objects.add((NormalObject)obj);
                }
            }
        }
        return objects;
    }
    public ArrayList<NormalObject> getShelfObjectInWall(int wallIndex) {
        ArrayList<NormalObject> shelfObjectList = new ArrayList<NormalObject>();
        for(SEObject obj : mChildObjects) {
            if(!(obj instanceof NormalObject)) {
                continue;
            }
            if(obj instanceof  WallShelf) {
                continue;
            }
            NormalObject normalObject = (NormalObject) obj;
            if(normalObject.isShelfObject() && normalObject.getObjectSlot().mSlotIndex == wallIndex) {
                shelfObjectList.add(normalObject);
            }
        }
        return shelfObjectList;
    }
    @Override
    public void onLoad3DMaxModel() {
        ModelInfo modelInfo = getScene().mSceneInfo.findModelInfo("IconBackground");
        modelInfo.load3DMAXModel(getScene());
    }
    public void removeObjectFromCurrentShelf(NormalObject currentObject, int mountPointIndex, WallShelf currentShelf) {
        if(currentShelf == null) {
            return;
        }
        currentShelf.changeObjectOnShelfToNull(currentObject, mountPointIndex);
        int realNum = currentShelf.getRealObjectNumOnShelf();
        if(realNum == 0) {
            currentShelf.getParent().removeChild(currentShelf, true);
        }
    }
    public WallShelf getNearestShelf(SEMountPointChain chain, int mountPointIndex, ArrayList<WallShelf> shelfList) {
        SEMountPointData mpd = chain.getMountPointData(mountPointIndex);
        if (null != mpd) {
            SEVector3f mpTranslate = mpd.getTranslate();
            final float ZE = 2;
            for(int i = 0 ; i < shelfList.size() ; i++) {
                WallShelf shelf = shelfList.get(i);
                SEObjectBoundaryPoint bp = shelf.getBoundaryPoint();
                if(bp == null || bp.minPoint == null) {
                    continue;
                }
                SEVector3f minPoint = bp.minPoint;
                SEVector3f maxPoint = bp.maxPoint;
                SEVector3f center = bp.center;

                if(Math.abs(mpTranslate.mD[2] - center.mD[2]) <= ZE ) {
                    if(mpTranslate.mD[0] >= minPoint.getX() && mpTranslate.mD[0] <= maxPoint.getX()) {
                        return shelf;
                    }
                }
            }
        } else {
            // for native object, no mount point sensitive
        }
        return null;
    }
    /*
    private void removeNormalObject(NormalObject normalObject) {
        ArrayList<ArrayList<WallShelf>> shelfs = getWallShelfsInWall();
        for(ArrayList<WallShelf> wallShelfList : shelfs) {
            for(WallShelf shelf : wallShelfList) {
                removeObjectFromCurrentShelf(normalObject, shelf);
            }
        }
    }
    */
    public void onLoadFinished() {
        if (mChildObjects != null) {
            for (SEObject c : mChildObjects) {
                if (c instanceof NormalObject) {
                    NormalObject child = (NormalObject)c;
                    NormalObject parent = (NormalObject)c.getParent();
                    String type = child.getObjectInfo().mType;
                    if (ModelInfo.isAppItemWithBackground(type)) {
                        child.showBackgroud();
                    }
                }
            }
            /*
            SEMountPointManager mountPointManager = getScene().getMountPointManager();
            SEMountPointChain mpc = mountPointManager.getMountPointChain(ModelInfo.Type.WALL_SHELF, mName, WALL_OBJNAMES.get(0));
            SEVector3f t0 = mpc.getMountPointData(0).getTranslate().clone();
            SEVector3f t1 = mpc.getMountPointData(1).getTranslate().clone();
            WallShelf.wallShelfBorderHeight = t0.getZ() - t1.getZ();
            */
        }
    }
    @Override
    public void addChild(SEObject obj, boolean create) {
        super.addChild(obj, create);
        if((obj instanceof  NormalObject) && ((NormalObject)obj).isShelfObject() && (!(obj instanceof WallShelf))) {
            NormalObject normalObject = (NormalObject) obj;
            int wallIndex = normalObject.getObjectSlot().mSlotIndex;
            SEMountPointManager mountPointManager = getScene().getMountPointManager();
            final String childName = WALL_OBJNAMES.get(wallIndex);
            SEMountPointChain mpc = mountPointManager.getMountPointChain(normalObject.mName, mName, childName, getWallObjectContainerName());

            setShelfObjectWhenAddObject(normalObject, mpc, wallIndex);
        }
    }
    @Override
    public void onAdd3DMaxModel() {
        ModelInfo modelInfo = getScene().mSceneInfo.findModelInfo("IconBackground");
        modelInfo.createMenuInstanceForMaxModel(getScene());
    }

    private void changeImage() {
        String where = ModelColumns._ID + "=" + getObjectInfo().mModelInfo.mID + " and " + ModelColumns.THEME_NAME
                + "='" + mCurrentTheme + "'";
        Context context = SESceneManager.getInstance().getContext();
        ContentResolver resolver = context.getContentResolver();
        Cursor imageCursor = resolver.query(ModelColumns.IMAGE_INFO_URI, null, where, null, null);
        ImageInfo imageInfo = new ImageInfo();
        imageInfo.mThemeName = mCurrentTheme;
        if (imageCursor != null) {
            while (imageCursor.moveToNext()) {
                ImageItem imageItem = new ImageItem();
                imageItem.mImageName = imageCursor
                        .getString(imageCursor.getColumnIndexOrThrow(ModelColumns.IAMGE_NAME));
                imageItem.mPath = imageCursor.getString(imageCursor.getColumnIndexOrThrow(ModelColumns.IMAGE_PATH));
                imageItem.mNewPath = imageCursor.getString(imageCursor
                        .getColumnIndexOrThrow(ModelColumns.IMAGE_NEW_PATH));
                if(!TextUtils.isEmpty(imageItem.mNewPath) && imageItem.mNewPath.startsWith(HomeUtils.SDCARD_PATH)) {
                	if(imageNewPathUnExit(imageItem.mNewPath)) {
                		imageItem.mNewPath = imageItem.mPath;
                	}
                }
                if (!imageInfo.mImageItems.contains(imageItem)) {
                    imageInfo.mImageItems.add(imageItem);
                }
            }
            imageCursor.close();

            loadImageItemOneByOne(getScene(), 0, imageInfo.mImageItems);
            initCurrentThemeFeature();
        }
    }
    
    private boolean imageNewPathUnExit(String imagePath) {
    	File tmpPathFile = new File(imagePath);
    	if(tmpPathFile.exists()) {
    		return false;
    	}else {
    		return true;
    	}
    }

    private void loadImageItemOneByOne(final SEScene scene, final int index, final List<ImageItem> imageItems) {
        if (index < imageItems.size()) {
            final ImageItem imageItem = imageItems.get(index);
            boolean exist = SEObject.isImageExist_JNI(imageItem.mNewPath);
            if (!exist) {
                SELoadResThread.getInstance().process(new Runnable() {
                    public void run() {
                        final int imageData = SEObject.loadImageData_JNI(imageItem.mNewPath);
                        new SECommand(scene) {
                            public void run() {
                                SEObject.addImageData_JNI(imageItem.mNewPath, imageData);
                                SEObject.applyImage_JNI(imageItem.mImageName, imageItem.mNewPath);
                                loadImageItemOneByOne(scene, index + 1, imageItems);
                            }
                        }.execute();
                    }
                });
            } else {
                SEObject.applyImage_JNI(imageItem.mImageName, imageItem.mNewPath);
                loadImageItemOneByOne(scene, index + 1, imageItems);
            }
        }
    }

    private void initObjectNameInHome() {
        String[] names = getObjectInfo().mModelInfo.mChildNames;
        final String[] wallPrefix = {"wall", "qiang"};
        final String[] ceilPrefix = {"roof", "wuding"};
        final String[] lightPrefix = {"light", "deng"};
        final String[] groundPrefix = {"floor", "home_ground"};
        final String[][] allPrefix = {wallPrefix, ceilPrefix, lightPrefix, groundPrefix};
    	    for(int i = 0 ; i < names.length ; i++) {
    	    	    String objName = names[i];
    	    	    boolean added = false;
    	    	    for(int j = 0 ; j < allPrefix.length ; j++) {
    	    	    	    String[] prefixs = allPrefix[j];
    	    	    	    for(int k = 0 ; k < prefixs.length ; k++) {
    	    	    	    	    String prefix = prefixs[k];
    	    	    	    	    if(objName.startsWith(prefix)) {
        	    	    	    	    added = true;
        	    	    	    	    if(j == 0) {
        	    	    	    	        this.WALL_OBJNAMES.add(objName);
        	    	    	    	    } else if (j == 1) {
        	    	    	    	    	    this.CEIL_OBJNAMES.add(objName);
        	    	    	    	    } else if(j == 2) {
        	    	    	    	    	    this.CEIL_LIGHT_OBJ_NAME = objName;
        	    	    	    	    } else if(j == 3) {
        	    	    	    	    	    this.GROUND_OBJ_NAME = objName;
        	    	    	    	    }
        	    	    	    	    break;
        	    	    	    }
    	    	    	    }
    	    	    	    if(added) {
    	    	    	    	    break;
    	    	    	    }
    	    	    }   
    	    }
    	    Collections.sort(this.WALL_OBJNAMES);
    	    Collections.sort(this.CEIL_OBJNAMES);
    	    assert(WALL_OBJNAMES.size() == 8);
    	    assert(CEIL_OBJNAMES.size() == 2);
//    	    assert(CEIL_LIGHT_OBJ_NAME != null);
    	    assert(this.GROUND_OBJ_NAME != null);
    }
    @Override
    public void initStatus(SEScene scene) {
        setIsEntirety_JNI(true);
        this.initObjectNameInHome();
        if (null != CEIL_LIGHT_OBJ_NAME) {
            mLight = new SEObject(getScene(), this.CEIL_LIGHT_OBJ_NAME, mIndex);
            mLight.setBlendingable(true, true);

            mLight.setOnLongClickListener(new OnTouchListener() {
                public void run(SEObject obj) {
                    getScene().handleMessage(HomeScene.MSG_TYPE_SHOW_EDIT_SCENE_DIALOG, null);
                }
            });
        }
        mCurrentTheme = SettingsActivity.getThemeName(getContext());
        initCurrentThemeFeature();
        mSceneInfo = scene.mSceneInfo;
        mWallNum = getCount();
        mWallRadiusChangedListeners = new ArrayList<WallRadiusChangedListener>();
        mOnMoveSight = false;
        mCancelClick = false;
        this.setRotate(new SERotate(getAngle(), 0, 0, 1), true);
        mPreFaceIndex = getFaceIndex(getAngle());
        initAndroidWidget();
        SESceneManager.getInstance().addUnlockScreenListener(this);
        LauncherModel.getInstance().addAppCallBack(this);
        LauncherModel.getInstance().setShortcutCallBack(this);
        setVesselLayer(new HouseLayer(scene, this));
        setHasInit(true);


        for(String name : CEIL_OBJNAMES) {
        	SEObject houseTop = new SEObject(getScene(), name, 0);
        	houseTop.setIsEntirety_JNI(true);
        	houseTop.setIsAlphaPress(false);
        	houseTop.setOnDoubleClickListener(new OnTouchListener() {
        		@Override
        		public void run(SEObject obj) {
        			performDoubleTap();
        		}
        	});
        	houseTop.setPressedListener(null);
        	houseTop.setUnpressedListener(null);
        	addChild(houseTop, false);
        }
        addWallObjects();
    }

    private void addWallObjects() {
        SEObject ground = new SEObject(getScene(), this.GROUND_OBJ_NAME, 0);
        groundpaperKeys.add(ground.getImageName_JNI());
        ground.setIsEntirety_JNI(true);
        ground.setIsAlphaPress(false);
        ground.setOnClickListener(new OnTouchListener() {
            @Override
            public void run(SEObject obj) {
                getScene().handleMenuKey();
            }
        });
        ground.setOnLongClickListener(new OnTouchListener() {
            public void run(SEObject obj) {
                Bundle msg = prepareWallpaper(obj,TYPE_FLOOR);
                getScene().handleMessage(HomeScene.MSG_TYPE_SHOW_SELECT_WALLPAPER_DIALOG, msg);
            }
        });
        addChild(ground, false);

        for (String wallName : WALL_OBJNAMES) {
    		SEObject obj = new SEObject(getScene(), wallName, 0);
            wallpaperKeys.add(obj.getImageName_JNI());
    		obj.setIsEntirety_JNI(true);
    		obj.setIsAlphaPress(false);
            obj.setOnClickListener(new OnTouchListener() {
                @Override
                public void run(SEObject obj) {
                    getScene().handleMenuKey();
                }
            });
    		obj.setOnLongClickListener(new OnTouchListener() {
    			public void run(SEObject obj) {
    				Bundle msg = prepareWallpaper(obj,TYPE_WALL);
    				getScene().handleMessage(HomeScene.MSG_TYPE_SHOW_WALL_LONG_CLICK_DIALOG, msg);
    			}
    		});
    		
    		obj.setOnDoubleClickListener(new OnTouchListener() {
    			@Override
    			public void run(SEObject obj) {
    				performDoubleTap();
    			}
    		});
    		obj.setPressedListener(null);
    		obj.setUnpressedListener(null);
    		addChild(obj, false);
    	}
    }

    public static String getWallObjectContainerName() {
        return WallLayer.getObjectContainerName();
    }
    public static String getWallShelfContainerName() {
        return WallLayer.getShelfContainerName();
    }
    /*
    private void changeWallLocation(SEObject wall0, SEObject wallObj, int wallIndex) {
        wallObj.setLocalRotate(new SERotate(0, 0, 0, 0));
        wall0.createLocalBoundingVolume();
        SEVector3f minPoint = new SEVector3f();
        SEVector3f maxPoint = new SEVector3f();
        wall0.getLocalBoundingVolume(minPoint, maxPoint);
        SEVector3f xyzSpan = maxPoint.subtract(minPoint);
        SEVector3f wall0LocalTranslate = wall0.getLocalTranslate();
        SEVector3f wallTranslate = null;
        if(wallIndex == 1) {
            wallTranslate = new SEVector3f(wall0LocalTranslate.mD[0] - xyzSpan.mD[0],
                                           wall0LocalTranslate.mD[1], wall0LocalTranslate.mD[2]);
        } else if(wallIndex == 2) {
            wallTranslate = new SEVector3f(wall0LocalTranslate.mD[0] + xyzSpan.mD[0],
                    wall0LocalTranslate.mD[1], wall0LocalTranslate.mD[2]);
        } else {
            wallTranslate = new SEVector3f(wall0LocalTranslate.mD[0] + 2 * xyzSpan.mD[0],
                    wall0LocalTranslate.mD[1], wall0LocalTranslate.mD[2]);
        }
        wallObj.setLocalTranslate(wallTranslate);
    }
    */
    private SETransParas rayCrossWall(SERay ray, float wallRadius) {
        // ray cross the front wall
        SETransParas transParas = new SETransParas();
        float y = wallRadius;
        assert(ray.getDirection().getY() == 1.0);
        float para = (y - ray.getLocation().getY()) / ray.getDirection().getY();
        transParas.mTranslate = ray.getLocation().add(ray.getDirection().mul(para));
        float faceAngle = 360 / this.getCount();
        float tanAngle = (float) Math.tan(faceAngle * Math.PI / 360);
        float halfFaceW = wallRadius * tanAngle;
        if (transParas.mTranslate.getX() < -halfFaceW) {
            // ray cross the left wall
            float Xa = ray.getLocation().getX();
            float Ya = ray.getLocation().getY();
            float Xb = ray.getDirection().getX();
            float Yb = ray.getDirection().getY();
            para = (tanAngle * Xa + tanAngle * halfFaceW + wallRadius - Ya) / (Yb - tanAngle * Xb);
            transParas.mTranslate = ray.getLocation().add(ray.getDirection().mul(para));
            transParas.mRotate.set(faceAngle, 0, 0, 1);
        } else if (transParas.mTranslate.getX() > halfFaceW) {
            // ray cross the right wall
            float Xa = ray.getLocation().getX();
            float Ya = ray.getLocation().getY();
            float Xb = ray.getDirection().getX();
            float Yb = ray.getDirection().getY();
            para = (-tanAngle * Xa + tanAngle * halfFaceW + wallRadius - Ya) / (Yb + tanAngle * Xb);
            transParas.mTranslate = ray.getLocation().add(ray.getDirection().mul(para));
            transParas.mRotate.set(-faceAngle, 0, 0, 1);
        }
        return transParas;

    }
    /*
    private SETransParas getObjectTransParas(SERay ray, float radius, House house) {
        float skyRadius = getScene().mSceneInfo.mSkyRadius;
        SETransParas transParas = rayCrossWall(ray, radius);
        int minZ = (int) (getOnMoveObject().getObjectSlot().mSpanY * house.getWallUnitSizeY() / 2);
        int maxZ = (int) (house.getWallSpanY() * house.getWallUnitSizeY() + house.getWallHeight() - minZ);
        if (transParas.mTranslate.getZ() < minZ) {
            transParas.mTranslate.mD[2] = minZ;
        } else if (transParas.mTranslate.getZ() > maxZ) {
            transParas.mTranslate = rayCrossCylinder(ray, skyRadius);
            SEVector2f touchLocZ = transParas.mTranslate.getVectorZ();
            double angle = touchLocZ.getAngle_II();
            transParas.mRotate.set((float) (angle * 180 / Math.PI), 0, 0, 1);
            float scale = (skyRadius + getScene().getCamera().getRadius()) / (radius + getScene().getCamera().getRadius());
            transParas.mScale.set(scale, scale, scale);
        }
        return transParas;
    }
    */
    public SEVector3f getFingerLocation(int touchX, int touchY) {
    	SERay ray = getScene().getCamera().screenCoordinateToRay(touchX, touchY);
        SEVector3f location = rayCrossWall(ray, this.getWallRadius()).mTranslate;
        return location;
    }
    private SEObject firstWall;
    private SEObject getFirstWallObject() {
        if (firstWall == null) {
            firstWall = findChild(WALL_OBJNAMES.get(0), 0);
        }
        return firstWall;
    }
    private int getScaleFactor(NormalObject shelfObject) {
    	return shelfObject.getObjectInfo().getSpanX();
    	
    }
    private SEVector3f getPortShelfLocation(NormalObject shelfObject, String childName, SEMountPointManager mountPointManager) {
    	int startx = shelfObject.getObjectInfo().getStartX();
    	int spanx = shelfObject.getObjectInfo().getSpanX();
    	int starty = shelfObject.getObjectInfo().getStartY();
    	SEMountPointChain mpc = mountPointManager.getMountPointChain("app", mName, childName, getWallObjectContainerName());
        SEMountPointData mpd1 = null, mpd2 = null;
        int endx = startx + spanx - 1;
        int startIndex = mpc.getIndex(starty, startx);
        int endIndex = mpc.getIndex(starty, endx);
        mpd1 = mpc.getMountPointData(startIndex);
        mpd2 = mpc.getMountPointData(endIndex);
        SEVector3f t1 = new SEVector3f();
        if(mpd1 != null) {
            t1 = mpd1.getTranslate();
        }
        SEVector3f t2 = new SEVector3f();
        if(mpd2 != null) {
            t2 = mpd2.getTranslate();
        }
        SEVector3f t = t2.add(t1).mul(0.5f);
        return t;
    }
    private SEVector3f getShelfLocation(NormalObject shelfObject, String childName, SEMountPointManager mountPointManager, int shelfIndex) {
        SEMountPointChain mpc = mountPointManager.getMountPointChain("app", mName, childName, getWallObjectContainerName());
        SEMountPointData mpd1 = null, mpd2 = null;
        if(shelfIndex == 1) {
            mpd1 = mpc.getMountPointData(0);
            mpd2 = mpc.getMountPointData(1);
        } else if(shelfIndex == 2) {
            mpd1 = mpc.getMountPointData(8);
            mpd2 = mpc.getMountPointData(9);
        } else if(shelfIndex == 3) {
            mpd1 = mpc.getMountPointData(16);
            mpd2 = mpc.getMountPointData(17);
        } else if(shelfIndex == 4) {
            mpd1 = mpc.getMountPointData(6);
            mpd2 = mpc.getMountPointData(7);
        } else if(shelfIndex == 5) {
            mpd1 = mpc.getMountPointData(14);
            mpd2 = mpc.getMountPointData(15);
        } else if(shelfIndex == 6) {
            mpd1 = mpc.getMountPointData(22);
            mpd2 = mpc.getMountPointData(23);
        }
        SEVector3f t1 = new SEVector3f();
        if(mpd1 != null) {
            t1 = mpd1.getTranslate();
        }
        SEVector3f t2 = new SEVector3f();
        if(mpd2 != null) {
            t2 = mpd2.getTranslate();
        }
        SEVector3f t = t2.add(t1).mul(0.5f);
        return t;
    }
    private void setShelfObjectWhenAddObject(NormalObject shelfObj, SEMountPointChain currentChain, int wallIndex) {
        ArrayList<ArrayList<WallShelf>> shelfs = getWallShelfsInWall();
        ArrayList<WallShelf> shelfList = shelfs.get(wallIndex);
        int mountPointIndex = shelfObj.getObjectSlot().mMountPointIndex;
        WallShelf shelf = getNearestShelf(currentChain, mountPointIndex, shelfList);
        if(shelf != null) {
            shelf.addObjectOnShelf(shelfObj, mountPointIndex);
        }
    }
    public void setShelfObjectWhenAddShelf(WallShelf currentShelf, SEMountPointChain chain, int wallIndex) {
        ArrayList<WallShelf> shelfList = new ArrayList<WallShelf>();
        shelfList.add(currentShelf);
        ArrayList<NormalObject> shelfObjList = getShelfObjectInWall(wallIndex);
        for(NormalObject shelfObj : shelfObjList) {
            int mountPointIndex = shelfObj.getObjectSlot().mMountPointIndex;
            WallShelf shelf = getNearestShelf(chain, mountPointIndex, shelfList);
            if(shelf != null) {
                SEDebug.myAssert(shelf == currentShelf, "shelf is not the same");
                shelf.addObjectOnShelf(shelfObj, mountPointIndex);
            }
        }
        ArrayList<Integer> leftMountPointList = new ArrayList<Integer>();
        int rowNum = chain.getRowCount();
        int colNum = chain.getColCount();
        int mountPointCount = rowNum * colNum;
        for(int i = 0 ; i < mountPointCount ; i++) {
            boolean found = false;
            for(NormalObject shelfObj : shelfObjList) {
                if(shelfObj.getObjectSlot().mMountPointIndex == i) {
                    found = true;
                    break;
                }
            }
            if(found == false) {
                leftMountPointList.add(new Integer(i));
            }
        }
        for(Integer ii : leftMountPointList) {
            int i = ii.intValue();
            WallShelf shelf = getNearestShelf(chain, i, shelfList);
            if(shelf != null) {
                shelf.addObjectOnShelf(null, i);
            }
        }
    }
    private void setShelfObjectToShelf(ArrayList<WallShelf> shelfList, SEMountPointChain currentChain, int wallIndex) {
        ArrayList<NormalObject> shelfObjList = getShelfObjectInWall(wallIndex);
        for(NormalObject shelfObj : shelfObjList) {
            int mountPointIndex = shelfObj.getObjectSlot().mMountPointIndex;
            WallShelf shelf = getNearestShelf(currentChain, mountPointIndex, shelfList);
            if(shelf != null) {
                shelf.addObjectOnShelf(shelfObj, mountPointIndex);
            }
        }
        ArrayList<Integer> leftMountPointList = new ArrayList<Integer>();
        int rowNum = currentChain.getRowCount();
        int colNum = currentChain.getColCount();
        int mountPointCount = rowNum * colNum;
        for(int i = 0 ; i < mountPointCount ; i++) {
            boolean found = false;
            for(NormalObject shelfObj : shelfObjList) {
                if(shelfObj.getObjectSlot().mMountPointIndex == i) {
                    found = true;
                    break;
                }
            }
            if(found == false) {
                leftMountPointList.add(new Integer(i));
            }
        }
        for(Integer ii : leftMountPointList) {
            int i = ii.intValue();
            WallShelf shelf = getNearestShelf(currentChain, i, shelfList);
            if(shelf != null) {
                shelf.addObjectOnShelf(null, i);
            }
        }
    }
    public WallShelf getWallShelfWithObject(int wallIndex, NormalObject object) {
        for(SEObject child : mChildObjects) {
            if(!(child instanceof WallShelf)) {
                continue;
            }
            WallShelf shelf = (WallShelf) child;
            if(shelf.getObjectSlot().mSlotIndex == wallIndex && shelf.hasObject(object)) {
                return shelf;
            }
        }
        return null;
    }
    public WallShelf getWallShelfWithMountPoint(int wallIndex, int mountPointIndex) {
        for(SEObject child : mChildObjects) {
            if(!(child instanceof NormalObject)){
                continue;
            }
            if(!(child instanceof WallShelf)) {
                continue;
            }
            WallShelf shelf = (WallShelf) child;
            if(shelf.getObjectSlot().mSlotIndex == wallIndex && shelf.containMountPointIndex(mountPointIndex)) {
                return shelf;
            }
        }
        return null;
    }
    public SEMountPointChain getWallObjectMountPointChain(String objName, int wallIndex) {
        SEMountPointManager mountPointManager = getScene().getMountPointManager();
        String childName = WALL_OBJNAMES.get(wallIndex);
        SEMountPointChain mpc = mountPointManager.getMountPointChain(objName, mName, childName, getWallObjectContainerName());
        return mpc;
    }
    private SETransParas getMountPointTransParas(ObjectInfo objectInfo, NormalObject currentObject) {
        if (HomeUtils.DEBUG) {
            Log.d(TAG, "getMountPointTransParas enter, objectInfo = " + objectInfo);
        }


        final int childIndex = objectInfo.getSlotIndex();
        final String childName = WALL_OBJNAMES.get(childIndex);
        SEObject wall = findChild(childName, 0);

        if (HomeUtils.DEBUG) {
            Log.d(TAG, "getMountPointTransParas, childIndex = " + childIndex +
                    ", childName = " + childName +
                    ", wall object: " + wall.getName() + ", " + wall.getClass().getName());
        }

        SETransParas transparas = new SETransParas();

        int mountPointIndex = ModelInfo.getWallMountPointIndex(objectInfo);
        SEVector3f retV = null;
        if(currentObject.mName.equals("group_gplay")) {
            Log.i(TAG, "## google playe @@");
        }
        SEVector3f tmpTranslate = new SEVector3f();
        SEVector3f tmpScale = new SEVector3f();
        SERotate tmpRotate = new SERotate();
        wall.getLocalRotate_JNI(tmpRotate.mD);
        wall.getLocalTranslate_JNI(tmpTranslate.mD);
        wall.getLocalScale_JNI(tmpScale.mD);
        if(mountPointIndex == -1 && ( objectInfo.mIsNativeObject || ModelInfo.isWidgetObjectType(objectInfo.mType))) {
            SEObjectBoundaryPoint bp = null;
            if(objectInfo.mObjectSlot.mBoundaryPoint != null) {
                bp = objectInfo.mObjectSlot.mBoundaryPoint.clone();
            }
            SEVector3f center = null;
            if(bp == null || bp.center.equals(SEVector3f.ZERO)) {
                int startx = objectInfo.getStartX();
                int starty = objectInfo.getStartY();
                int spanx = objectInfo.getSpanX();
                int spany = objectInfo.getSpanY();
                int endx = startx + spanx - 1;
                int endy = starty + spany - 1;

                SEMountPointChain.MatrixPoint minMP = new SEMountPointChain.MatrixPoint();
                minMP.row = endy;
                minMP.col = startx;
                SEMountPointChain.MatrixPoint maxMP = new SEMountPointChain.MatrixPoint();
                maxMP.row = starty;
                maxMP.col = endx;
                SEVector3f minPoint = new SEVector3f();
                SEVector3f maxPoint = new SEVector3f();
                currentObject.createLocalBoundingVolume();
                currentObject.getLocalBoundingVolume(minPoint, maxPoint);
                SEVector3f xyzSpan = maxPoint.subtract(minPoint);
                SEMountPointManager mountPointManager = getScene().getMountPointManager();
                SEMountPointChain mpc = null;
                mpc = mountPointManager.getMountPointChain(objectInfo.mName, mName, childName, getWallObjectContainerName());
                int minIndex = mpc.getIndex(minMP.row, minMP.col);
                int maxIndex = mpc.getIndex(maxMP.row, maxMP.col);
                minPoint = mpc.getMountPointData(minIndex).getTranslate();
                maxPoint = mpc.getMountPointData(maxIndex).getTranslate();
                center = maxPoint.add(minPoint).mul(0.5f);
                SEObjectBoundaryPoint newBP = new SEObjectBoundaryPoint(childIndex);
                maxPoint = center.add(new SEVector3f(xyzSpan.getX() / 2, 0, xyzSpan.getZ() / 2));
                minPoint = center.subtract(new SEVector3f(xyzSpan.getX() / 2, 0, xyzSpan.getZ() / 2));
                newBP.center = center;
                newBP.xyzSpan = xyzSpan;
                newBP.minPoint = minPoint;
                newBP.maxPoint = maxPoint;
                newBP.minMatrixPoint = minMP;
                newBP.maxMatrixPoint = maxMP;
                currentObject.setBoundaryPoint(newBP);
            } else {
                currentObject.setBoundaryPoint(bp);
                center = bp.center;
                if(ModelInfo.isWidgetObjectType(objectInfo.mType)) {
            	    SEVector3f realLocationY  = WallLayer.getLocationYInWall(currentObject, childIndex);
            	    center.mD[1] = realLocationY.getY();
                }
            }
            retV = SEObject.rotateMapPoint(tmpRotate, center);
            retV = retV.add(tmpTranslate);
        } else {
        	if(mountPointIndex >= 0) {
                SEMountPointManager mountPointManager = getScene().getMountPointManager();
                final SEMountPointChain mpc;
                final SEVector3f t;
                if(objectInfo.mType != null && objectInfo.mType.equals(ModelInfo.Type.WALL_SHELF)) {
                    final String containerName = getWallShelfContainerName();
                    mpc = mountPointManager.getMountPointChain(objectInfo.mName, mName, childName, containerName);

                    SEObjectBoundaryPoint bp = objectInfo.mObjectSlot.mBoundaryPoint;
                    if(bp == null || bp.center.equals(SEVector3f.ZERO)) {
                    	if(isScreenOrientationPortrait()) {
                    	    t = getPortShelfLocation(currentObject, childName, mountPointManager);    	
                    	} else {
                            t = getShelfLocation(currentObject, childName, mountPointManager, objectInfo.mIndex);
                    	}

                    	if(isScreenOrientationPortrait()) {
                    		tmpScale.mD[0] = getScaleFactor(currentObject);
                    	} else {
                            tmpScale.mD[0] = 2;
                    	}
                        //t.mD[1] -= 200;
                        SEVector3f minPoint = new SEVector3f();
                        SEVector3f maxPoint = new SEVector3f();
                        currentObject.createLocalBoundingVolume();
                        currentObject.getLocalBoundingVolume(minPoint, maxPoint);
                        SEVector3f xyzSpan = maxPoint.subtract(minPoint);
                        if(!t.equals(SEVector3f.ZERO)) {
                        	if(isScreenOrientationPortrait()) {
                        		xyzSpan.mD[0] *= getScaleFactor(currentObject);
                        	}else {
                        		xyzSpan.mD[0] *= 2;
                        	}
                        }
                        SEObjectBoundaryPoint tmpBP = SEObjectBoundaryPoint.getMinMaxPointInPlane(t, xyzSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ, SEObjectBoundaryPoint.CENTER_POINT_STYLE_TOP_MID);
                        SEObjectBoundaryPoint newBP = new SEObjectBoundaryPoint(childIndex);
                        newBP.minPoint = tmpBP.minPoint;
                        newBP.maxPoint = tmpBP.maxPoint;

                        SEMountPointChain.MatrixPoint minMP = mpc.getMatrixPointInPlaneXZ(tmpBP.minPoint);
                        SEMountPointChain.MatrixPoint maxMP = mpc.getMatrixPointInPlaneXZ(tmpBP.maxPoint);

                        newBP.minMatrixPoint = minMP;
                        newBP.maxMatrixPoint = maxMP;
                        newBP.center = t;
                        newBP.xyzSpan = xyzSpan;
                        currentObject.setBoundaryPoint(newBP);
                    } else {
                        currentObject.setBoundaryPoint(bp);
                        t = bp.center;
                        SEVector3f xyzSpan = bp.xyzSpan;
                        SEVector3f minPoint = new SEVector3f();
                        SEVector3f maxPoint = new SEVector3f();
                        currentObject.createLocalBoundingVolume();
                        currentObject.getLocalBoundingVolume(minPoint, maxPoint);
                        SEVector3f origSpan = maxPoint.subtract(minPoint);
                        float s = (xyzSpan.mD[0] / origSpan.mD[0]) + 0.5f;
                        int scaleX = (int)s;
                        if(scaleX < 1) {
                            scaleX = 1;
                        }
                        tmpScale.mD[0] = scaleX;
                    }
                    WallShelf currentShelf = (WallShelf)currentObject;
                    if(currentShelf.isCreateByAddShelfObject() == false) {
                        currentShelf.createRowIndexInWall(childName);
                        SEMountPointChain shelfObjectMPC = mountPointManager.getMountPointChain("app", mName, childName, getWallObjectContainerName());
                        setShelfObjectWhenAddShelf((WallShelf)currentObject, shelfObjectMPC, childIndex);
                    }
                } else {
                    final String containerName = getWallObjectContainerName();
                    mpc = mountPointManager.getMountPointChain(objectInfo.mName, mName, childName, containerName);

                    SEMountPointData mpd = mpc.getMountPointData(mountPointIndex);
                    if (null == mpd) {
                        StringBuilder errorMsg = new StringBuilder();
                        errorMsg.append("getMountPointTransParas, Should not be here!!! null mpd, mountPointIndex = ").append(mountPointIndex)
                                .append(", objType = ").append(objectInfo.mType).append(", childName = ").append(childName).append(", objName")
                                .append(objectInfo.mName).append(", vesselName = ").append(mName).append(", containerName = ").append(containerName);
                        HomeUtils.reportError(errorMsg.toString());
                        Log.e(TAG, errorMsg.toString());
                        t = new SEVector3f();
                    } else {
                        if (null == mpd.getTranslate()) {
                            StringBuilder errorMsg = new StringBuilder();
                            errorMsg.append("getMountPointTransParas, Should not be here!!! null translate, mountPointIndex = ").append(mountPointIndex)
                                    .append(", objType = ").append(objectInfo.mType).append(", childName = ").append(childName).append(", objName")
                                    .append(objectInfo.mName).append(", vesselName = ").append(mName).append(", containerName = ").append(containerName);
                            HomeUtils.reportError(errorMsg.toString());
                            Log.e(TAG, errorMsg.toString());
                            t = new SEVector3f();
                        } else {
                            t = mpd.getTranslate().clone();
                        }
                    }
                }

                if (currentObject.canPlaceOnShelf()) {
                	SEVector3f realLocationY  = WallLayer.getLocationYInWall(currentObject, childIndex);
                	t.mD[1] = realLocationY.getY();
                    SEVector3f minPoint = new SEVector3f();
                    SEVector3f maxPoint = new SEVector3f();
                    currentObject.createLocalBoundingVolume();
                    currentObject.getLocalBoundingVolume(minPoint, maxPoint);
                    SEVector3f xyzSpan = maxPoint.subtract(minPoint);
                    if(currentObject.getObjectInfo().mIsNativeObject) {
                        tmpScale = WallLayer.createNativeShelfObjectScale(currentObject);
                    } else {
                        t.mD[2] = WallLayer.calculateAppObjectZPosition(t.mD[2], xyzSpan, currentObject);//xyzSpan.mD[2] / 2;
                    }
                    SEObjectBoundaryPoint newBP = WallLayer.createBoundaryPointFromMountPointIndex(currentObject, mpc, mountPointIndex, childIndex);
                    if (null == newBP) {
                        currentObject.getParent().removeChild(currentObject, true);
                    } else {
                        currentObject.setBoundaryPoint(newBP);
                        setShelfObjectWhenAddObject(currentObject, mpc, childIndex);
                    }
                } else {

                }
                retV = SEObject.rotateMapPoint(tmpRotate, t);
                retV = retV.add(tmpTranslate);
        	} else {
        		Log.i(TAG, "## object " + currentObject.mName + " mountPointIndex = " + mountPointIndex);
        	}
        }
        transparas.mTranslate = retV;
        transparas.mRotate = tmpRotate;
        transparas.mScale = tmpScale;
        currentObject.getObjectInfo().mSlotType = ObjectInfo.SLOT_TYPE_WALL;
        if(currentObject instanceof WallShelf) {
            if(getScene().isShelfVisible()) {
                currentObject.setVisible(true, true);
            } else {
                currentObject.setVisible(false, true);
            }
        }
        return transparas;
    }

    @Override
    public SETransParas getSlotTransParas(ObjectInfo objectInfo, NormalObject object) {
        if (ModelInfo.isUsingMountPointSlot(objectInfo)) {
            Log.d(HomeUtils.TAG, "initStatus, object type: " + objectInfo.mName);
            final SETransParas transParas = getMountPointTransParas(objectInfo, object);
            if (null != transParas) {
                return transParas;
            } else {
                Log.e(TAG, "getSlotTransParas, should not be here, getMountPointTransParas return null!!!");
            }
        }

        if (objectInfo.mObjectSlot.mSpanX > 0 && objectInfo.mObjectSlot.mSpanY > 0) {
            SEVector3f localTranslate = new SEVector3f();
            if (objectInfo.mIsNativeObject) {
                SETransParas localTrans = objectInfo.mModelInfo.mLocalTrans;
                if (localTrans != null)
                    localTranslate = localTrans.mTranslate;
            }
            SETransParas transparas = new SETransParas();
            ObjectSlot objectSlot = objectInfo.mObjectSlot;
            float angle = objectSlot.mSlotIndex * mPerFaceAngle;
            SEVector2f yDirection = new SEVector2f((float) Math.cos((angle + 90) * Math.PI / 180),
                    (float) Math.sin((angle + 90) * Math.PI / 180));
            SEVector2f xDirection = new SEVector2f((float) Math.cos(angle * Math.PI / 180), (float) Math.sin(angle
                    * Math.PI / 180));
            float offsetY = getWallRadius() + localTranslate.getY();
            float offsetX = (objectSlot.mStartX + objectSlot.mSpanX / 2.f) * getWallUnitSizeX()
                    - getWallSpanX() * getWallUnitSizeX() / 2.f + localTranslate.getX();
            SEVector2f offset = yDirection.mul(offsetY).add(xDirection.mul(offsetX));
            float offsetZ = getWallSpanY() * getWallUnitSizeY()
                    - (objectSlot.mStartY + objectSlot.mSpanY / 2.f) * getWallUnitSizeY()
                    + getWallHeight() + localTranslate.getZ();
            float z = offsetZ;
            transparas.mTranslate.set(offset.getX(), offset.getY(), z);
            transparas.mRotate.set(angle, 0, 0, 1);
            transparas.mTranslate.selfSubtract(localTranslate);
            return transparas;
        } else {
            SEVector3f localTranslate = new SEVector3f();
            if (objectInfo.mIsNativeObject) {
                SETransParas localTrans = objectInfo.mModelInfo.mLocalTrans;
                if (localTrans != null)
                    localTranslate = localTrans.mTranslate;
            }
            SETransParas transparas = new SETransParas();
            ObjectSlot objectSlot = objectInfo.mObjectSlot;
            float angle = (objectSlot.mSlotIndex + 0.5f) * mPerFaceAngle;
            SEVector2f yDirection = new SEVector2f((float) Math.cos((angle + 90) * Math.PI / 180),
                    (float) Math.sin((angle + 90) * Math.PI / 180));
            SEVector2f xDirection = new SEVector2f((float) Math.cos(angle * Math.PI / 180), (float) Math.sin(angle
                    * Math.PI / 180));
            float offsetY = getWallRadius() + localTranslate.getY();
            float offsetX = localTranslate.getX();
            SEVector2f offset = yDirection.mul(offsetY).add(xDirection.mul(offsetX));
            float offsetZ = localTranslate.getZ();
            float z = offsetZ;
            transparas.mTranslate.set(offset.getX(), offset.getY(), z);
            transparas.mRotate.set(angle, 0, 0, 1);
            transparas.mTranslate.selfSubtract(localTranslate);
            return transparas;
        }
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        new SECommand(getScene()) {
            public void run() {
            	Context pContext = getContext(); 
                if (hasInit()) {
                    String theme = SettingsActivity.getThemeName(pContext);
                    if (!mCurrentTheme.equals(theme)) {
                        mCurrentTheme = theme;
                        changeImage();
                    }
                    if(SettingsActivity.isChangeWallPaper(pContext)) {
                    	changeImage();
                    	SettingsActivity.saveChangeWallPaper(pContext, false);
                        SettingsActivity.saveWallpaperCustomizedTimeStamp(pContext, true);
                    }

                    mWallNum = getCount();
                    setRotate(new SERotate(getAngle(), 0, 0, 1), true);
                    mPreFaceIndex = getFaceIndex(getAngle());
                }
                final String iconName = SettingsActivity.getAppIconBackgroundName(pContext);
                if (!"none".equals(iconName)) {
                    SELoadResThread.getInstance().process(new Runnable() {
                        @Override
                        public void run() {
                            String imagePath = "assets/base/appwall/home_appwall_" + iconName + ".png";
                            final int imageData = SEObject.loadImageData_JNI(imagePath);
                            SEObject.applyImage_JNI("home_appwall05_zch.png@appwall1_basedata.cbf", imagePath);
                            SEObject.addImageData_JNI(imagePath, imageData);
                        }
                    });
                }
                if (mChildObjects != null) {
                    for (SEObject c : mChildObjects) {
                        if (c instanceof NormalObject) {
                            NormalObject child = (NormalObject)c;
                            NormalObject parent = (NormalObject)c.getParent();
                            String type = child.getObjectInfo().mType;
                            if (ModelInfo.isAppItemWithBackground(type)) {
                                child.showBackgroud();
                                if (HomeUtils.DEBUG) {
                                    Log.d(TAG, "App item info: " + child.mName + ", t = " + child.getUserTranslate());
                                }
                            }
                        }
                    }
                }
            }
        }.execute();

    }

    private void initCurrentThemeFeature() {
        String themeConfig = SettingsActivity.getThemeConfig(getContext());
        if (!TextUtils.isEmpty(themeConfig)) {
            String[] featureList = themeConfig.split(";");
            if (featureList != null && featureList.length > 0) {
                for (String item : featureList) {
                    String[] feature = item.split(":");
                    if (feature != null && feature.length == 2) {
                        String featureName = feature[0];
                        try {
                            if (FEATURE_LIGHT.equals(featureName)) {
                                mFeatureLight = Boolean.getBoolean(feature[1]);
                                if (null != mLight) {
                                    mLight.setVisible(mFeatureLight, true);
                                }
                            }
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        // if there is no feature config, set default value.
        if (null != mLight) {
            mLight.setVisible(true, true);
        }
    }

    @Override
    public void onRenderFinish(SECamera camera) {
        super.onRenderFinish(camera);
        this.setRotate(new SERotate(getAngle(), 0, 0, 1), true);
        mPreFaceIndex = getFaceIndex(getAngle());
    }

    public void addWallRadiusChangedListener(WallRadiusChangedListener l) {
        if (!mWallRadiusChangedListeners.contains(l)) {
            mWallRadiusChangedListeners.add(l);
            l.onWallRadiusChanged(mPreFaceIndex);
        }
    }

    public void removeWallRadiusChangedListener(WallRadiusChangedListener l) {
        if (l != null) {
            mWallRadiusChangedListeners.remove(l);
        }
    }
    public int getWallNum() {
        return WALL_OBJNAMES.size();
    }
    @Override
    public boolean canChildHandleLongClick(NormalObject child) {
        int currentWallIndex = getWallNearestIndex();
        int currentChildSlotIndex = child.getObjectSlot().mSlotIndex;
        if(currentWallIndex != currentChildSlotIndex) {
            return false;
        }
        return true;
    }
    public void setRotate(float angle) {
        if (getAngle() == angle) {
            return;
        }
        
        SERotate rotate = new SERotate(angle);
        setWallAngle(rotate.getAngle());
        int faceIndex = getFaceIndex(getAngle());
        if (mPreFaceIndex != faceIndex) {
            mSceneInfo.updateWallAngle();
            mPreFaceIndex = faceIndex;
            for (WallRadiusChangedListener l : mWallRadiusChangedListeners) {
                l.onWallRadiusChanged(mPreFaceIndex);
            }
            notifyWallChanged(mPreFaceIndex, getCount());
        }

        setRotate(rotate, true);
    }

    public int getWallNearestIndex() {
        int index = (int) ((getAngle() + mPerFaceAngle / 2.f) / mPerFaceAngle);
        if (index != 0) {
            index = mWallNum - index;
        }
        return index;
    }

    private int getFaceIndex(float angle) {
        if (angle % mPerFaceAngle == 0) {
            int index = (int) (angle / mPerFaceAngle);
            if (index != 0) {
                index = mWallNum - index;
            }
            return index;
        }
        return -1;
    }

    public float getAngle() {
        return getWallAngle();
    }

    private void trackVelocity(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        final VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000);
    }

    public void toNearestFace(SEAnimFinishListener l, float step) {
        int index = (int) ((getAngle() + mPerFaceAngle / 2) / mPerFaceAngle);
        if (index != 0) {
            index = mWallNum - index;
        }
        stopAllAnimation(null);
        toFace(index, l, step);
    }

    public void toLeftFace(SEAnimFinishListener listener, float step) {
        int index = (int) ((getAngle() + mPerFaceAngle / 2) / mPerFaceAngle);
        if (index != 0) {
            index = mWallNum - index;
        }
        int face = index + 1;
        if (face >= mWallNum) {
            face = face - mWallNum;
        }
        stopAllAnimation(null);
        toFace(face, listener, step);
    }

    public void toLeftHalfFace(SEAnimFinishListener listener, float step) {
        int index = (int) (getAngle() / mPerFaceAngle);
        if (index != 0) {
            index = mWallNum - index;
        }
        float face = index + 0.5f;
        if (face >= mWallNum) {
            face = face - mWallNum;
        }
        stopAllAnimation(null);
        toFace(face, listener, step);
    }

    public void toRightFace(SEAnimFinishListener listener, float step) {
        int index = (int) ((getAngle() + mPerFaceAngle / 2) / mPerFaceAngle);
        if (index != 0) {
            index = mWallNum - index;
        }
        int face = index - 1;
        if (face < 0) {
            face = mWallNum + face;
        }
        stopAllAnimation(null);
        toFace(face, listener, step);
    }

    public void toRightHalfFace(SEAnimFinishListener listener, float step) {
        int index = (int) (getAngle() / mPerFaceAngle);
        if (index != 0) {
            index = mWallNum - index;
        }
        float face = index - 1.5f;
        if (face < 0) {
            face = mWallNum + face;
        }
        stopAllAnimation(null);
        toFace(face, listener, step);
    }

    public void toFace(float face, SEAnimFinishListener listener, float step) {
        stopAllAnimation(null);
        float desAngle = 360 - face * mPerFaceAngle;
        if (getAngle() == desAngle) {
            if (listener != null) {
                listener.onAnimationfinish();
            }
            return;
        }
        mToFaceAnimation = new ToFaceAnimation(getScene(), face, step, false);
        mToFaceAnimation.setAnimFinishListener(listener);
        mToFaceAnimation.execute();
    }

    private boolean mHasGotAction = false;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            SettingsActivity.updateScreenIndicatorStatus(Boolean.valueOf(true));
            if (isBusy() || getScene().getStatus(SEScene.STATUS_ON_SKY_SIGHT)
                    || getScene().getStatus(SEScene.STATUS_ON_DESK_SIGHT)) {
                if (getScene().getStatus(SEScene.STATUS_ON_DESK_SIGHT)) {
                    getCamera().moveToWallSight(null);
                    mHasGotAction = true;
                } else if (getScene().getStatus(SEScene.STATUS_ON_SKY_SIGHT)) {
                    mOnMoveSight = true;
                    mHasGotAction = true;
                }
                return true;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            setPreTouch();
            mHasGotAction = false;
            stopAllAnimation(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        trackVelocity(ev);
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            stopAllAnimation(null);
            setPreTouch();
            mCancelClick = false;
            SettingsActivity.updateScreenIndicatorStatus(Boolean.valueOf(true));
            break;
        case MotionEvent.ACTION_MOVE:
            int dY = getTouchY() - getPreTouchY();
            int dX = getTouchX() - getPreTouchX();
            if (!mHasGotAction) {
                if ((Math.abs(dY) > Math.abs(dX) /*&& dY > 0*/)) {
                    mOnMoveSight = true;
                }
            }
            mHasGotAction = true;
            if (mOnMoveSight) {
                getCamera().dragSight(dY);
                mCancelClick = true;
            } else {
            	int width = getCamera().getWidth();
            	float ratio = (float) (mPerFaceAngle * 2 / width);
            	float transAngle = ratio * (getTouchX() - getPreTouchX());
            	float curAngle = getAngle() - transAngle;
            	setRotate(curAngle);
            }
            setPreTouch();
            break;
        case MotionEvent.ACTION_UP:
            setPreTouch();
            if (mOnMoveSight) {
                if (!mCancelClick) {
                    mOnMoveSight = false;
                    mHasGotAction = false;
                    getCamera().moveToWallSight(null);
                    stopAllAnimation(null);
                    mWallVelocityAnimation = new WallVelocityAnimation(getScene(), 0);
                    mWallVelocityAnimation.execute();
                    return true;
                }
                
                mOnMoveSight = false;
                getCamera().onDragEnd(mVelocityTracker.getYVelocity());
            }
            
            mHasGotAction = false;
            stopAllAnimation(null);
            if(getCamera().isDefaultSight()) {
            	mWallVelocityAnimation = new WallVelocityAnimation(getScene(), mVelocityTracker.getXVelocity());
            }else {
            	mWallVelocityAnimation = new WallVelocityAnimation(getScene(), 0);
            }
            mWallVelocityAnimation.execute();
            break;
        case MotionEvent.ACTION_CANCEL:
            if (mOnMoveSight) {
                mOnMoveSight = false;
                getCamera().onDragEnd(mVelocityTracker.getYVelocity());
//                if (getCamera().wasLeftDownSight()) {
//                    if (mVelocityTracker.getYVelocity() > 200) {
//                        getCamera().moveToSkySight(null);
//                    } else if (mVelocityTracker.getYVelocity() < -200) {
//                        getCamera().moveToWallSight(null);
//                    } else {
//                        if (getCamera().wasSkySightRange()) {
//                            getCamera().moveToSkySight(null);
//                        } else {
//                            getCamera().moveToWallSight(null);
//                        }
//                    }
//                }
            }
            
            mHasGotAction = false;
            stopAllAnimation(null);
            mWallVelocityAnimation = new WallVelocityAnimation(getScene(), mVelocityTracker.getXVelocity());
            mWallVelocityAnimation.execute();
            break;
        }
        return true;
    }

    private boolean isBusy() {
        if (mWallVelocityAnimation != null && !mWallVelocityAnimation.isFinish()) {
            stopAllAnimation(null);
            return true;
        }
        if (mToFaceAnimation != null && !mToFaceAnimation.isFinish()) {
            stopAllAnimation(null);
            return true;
        }
        return false;
    }

    @Override
    public void stopAllAnimation(SEAnimFinishListener l) {
        if (mWallVelocityAnimation != null) {
            mWallVelocityAnimation.stop();
        }
        if (mToFaceAnimation != null) {
            mToFaceAnimation.stop();
        }
        if (mRunACircleAnimation != null) {
            mRunACircleAnimation.stop();
        }
    }

    private class WallVelocityAnimation extends CountAnimation {
        private float mVelocity;
        private float mCurrentAngle;
        private float mDesAngle;

        public WallVelocityAnimation(SEScene scene, float velocity) {
            super(scene);
            if (Math.abs(velocity) < 100) {
                mVelocity = 0;
            } else {
                mVelocity = -velocity;
            }
        }

        @Override
        public void runPatch(int count) {
            float needRotateAngle = mDesAngle - mCurrentAngle;
            float absNRA = Math.abs(needRotateAngle);
            if (absNRA < 1) {
                mCurrentAngle = mDesAngle;
                setRotate(mCurrentAngle);
                stop();
            } else {
                float step = (float) Math.sqrt(absNRA);
                if (needRotateAngle < 0) {
                    step = -step;
                }
                mCurrentAngle = mCurrentAngle + step;
                setRotate(mCurrentAngle);
            }
        }

        @Override
        public void onFirstly(int count) {
            mCurrentAngle = getAngle();
            float needRotateAngle = (float) (mVelocity * mPerFaceAngle * 0.0006f);
            float desAngle = mCurrentAngle + needRotateAngle;
            if (desAngle >= 0) {
                int index = (int) ((desAngle + mPerFaceAngle / 2) / mPerFaceAngle);
                mDesAngle = mPerFaceAngle * index;
            } else {
                int index = (int) ((desAngle - mPerFaceAngle / 2) / mPerFaceAngle);
                mDesAngle = mPerFaceAngle * index;
            }
            if (mDesAngle == mCurrentAngle) {
                stop();
            }
        }
        @Override
        public void onFinish() {
            super.onFinish();
            SettingsActivity.updateScreenIndicatorStatus(Boolean.valueOf(false));
        }
    }

    private class ToFaceAnimation extends CountAnimation {
        private float mStep;
        private float mDesAngle;
        private float mCurAngle;
        private float mFace;
        private boolean mCircle;

        public ToFaceAnimation(SEScene scene, float face, float step, boolean circle) {
            super(scene);
            mStep = step;
            mFace = face;
            mCircle = circle;
        }

        public void runPatch(int count) {
            float needRotate = mDesAngle - mCurAngle;
            float absNTR = Math.abs(needRotate);
            if (absNTR <= mStep) {
                mCurAngle = mDesAngle;
                setRotate(mCurAngle);
                stop();
            } else {
                float step = mStep;
                if (needRotate < 0) {
                    step = -mStep;
                }
                mCurAngle = mCurAngle + step;
                setRotate(mCurAngle);
            }
        }

        public void onFirstly(int count) {
            mCurAngle = getAngle();
            if (mCircle) {
                mDesAngle = 360 + mCurAngle;
            } else {
                mDesAngle = 360 - mFace * mPerFaceAngle;
                float needRotateAngle = Math.abs(mDesAngle - mCurAngle);
                if (needRotateAngle > 180) {
                    needRotateAngle = 360 - needRotateAngle;
                    if (mDesAngle - mCurAngle > 180) {
                        mDesAngle = mCurAngle - needRotateAngle;
                    } else if (mDesAngle - mCurAngle < -180) {
                        mDesAngle = mCurAngle + needRotateAngle;
                    }
                }
            }
        }
    }

    private boolean performDoubleTap() {
        if (getScene().getStatus(SEScene.STATUS_ON_UNLOCK_ANIMATION)
                || mIsRunningCircleAnimation || mOnMoveSight) {
            return false;
        }
        getCamera().zoomInOut();
        return true;
    }

    public void runACircle(SEAnimFinishListener listener) {
        stopAllAnimation(null);
        mIsRunningCircleAnimation = true;
        mRunACircleAnimation = new RunACircleAnimation(getScene());
        mRunACircleAnimation.setAnimFinishListener(listener);
        mRunACircleAnimation.execute();
    }

    class RunACircleAnimation extends CountAnimation {
        private float mDesAngle;
        private float mCurAngle;

        public RunACircleAnimation(SEScene scene) {
            super(scene);
        }

        @Override
        public void runPatch(int count) {
            float needRotateAngle = mDesAngle - mCurAngle;
            float absNRA = Math.abs(needRotateAngle);
            if (absNRA < 1) {
                mCurAngle = mDesAngle;
                setRotate(mCurAngle);
                stop();
            } else {
                float step = (float) Math.sqrt(absNRA) * 0.6f;/*
                                                            * (float)Math.pow(absNRA
                                                            * , 1 / 3.0);
                                                            */
                if (needRotateAngle < 0) {
                    step = -step;
                }
                mCurAngle = mCurAngle + step;
                setRotate(mCurAngle);
            }
        }

        @Override
        public void onFirstly(int count) {
            mCurAngle = getAngle();
            mDesAngle = 360f + mCurAngle;
        }

        @Override
        public void onFinish() {
            mIsRunningCircleAnimation = false;
        }
    }

    @Override
    public void onActivityRestart() {
        super.onActivityRestart();
        initAndroidWidget();
        forceReloadWidget();
    }

    private void forceReloadWidget() {
        mSESceneManager.getWidgetView().clearAll();
        new SECommand(getScene()) {
            public void run() {
                List<NormalObject> matchApps = findAPP(null, "Widget");
                for (NormalObject widget : matchApps) {
                    WidgetObject myWidget = (WidgetObject) widget;
                    myWidget.bind();
                }
            }
        }.execute();
    }

    @Override
    public void onActivityDestory() {
        super.onActivityDestory();
        DownloadChangeReceiver.unregisterSelectObjectListener(getClass().getName());
        final WidgetWorkSpace widgetWorkSpace = mSESceneManager.getWidgetView();
        widgetWorkSpace.clearAll();
        new SECommand(getScene()) {
            public void run() {
                removeWallRadiusChangedListener(widgetWorkSpace);
                getCamera().removeCameraChangedListener(widgetWorkSpace);
            }
        }.execute();

    }

    @Override
    public void onPressHomeKey() {
        super.onPressHomeKey();
        if (hasInit()) {
            toFace(0, null, 10);
        }
    }

    @Override
    public void onRelease() {
        super.onRelease();
        getCamera().removeCameraChangedListener(mSESceneManager.getWidgetView());
        SESceneManager.getInstance().removeUnlockScreenListener(this);
        LauncherModel.getInstance().removeAppCallBack(this);
        LauncherModel.getInstance().setShortcutCallBack(null);
        mSESceneManager.runInUIThread(new Runnable() {
            public void run() {
                mSESceneManager.getWidgetView().clearAll();
            }
        });
    }

    private void initAndroidWidget() {
        final WidgetWorkSpace widgetWorkSpace = mSESceneManager.getWidgetView();
        new SECommand(getScene()) {
            public void run() {
                addWallRadiusChangedListener(widgetWorkSpace);
                getCamera().addCameraChangedListener(widgetWorkSpace);
            }
        }.execute();
        widgetWorkSpace.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (v != null && v.getTag() != null) {
                    final WidgetObject itemObject = (WidgetObject) v.getTag();
                    View child = itemObject.getWidgetHostView();
                    child.clearFocus();
                    child.setPressed(false);
                    mSESceneManager.getWidgetView().startDrag();
                    new SECommand(getScene()) {
                        public void run() {
                            itemObject.hideBackgroud();
                            SETransParas startTranspara = new SETransParas();
                            startTranspara.mTranslate = itemObject.getAbsoluteTranslate();
                            float angle = itemObject.getUserRotate().getAngle() + getAngle();
                            startTranspara.mRotate.set(angle, 0, 0, 1);
                            int touchX = mSESceneManager.getWidgetView().getTouchX();
                            int touchY = mSESceneManager.getWidgetView().getTouchY();
                            itemObject.setTouch(touchX, touchY);
                            itemObject.setStartTranspara(startTranspara);
                            itemObject.setOnMove(true);
                        }
                    }.execute();
                }
                return true;
            }
        });
    }

    @Override
    public void unlockScreen() {
        boolean disable = getStatus(SEScene.STATUS_APP_MENU) | getStatus(SEScene.STATUS_HELPER_MENU)
                | getStatus(SEScene.STATUS_OPTION_MENU) | getStatus(SEScene.STATUS_OBJ_MENU)
                | getStatus(SEScene.STATUS_ON_SKY_SIGHT) | getStatus(SEScene.STATUS_ON_WIDGET_SIGHT)
                | getStatus(SEScene.STATUS_ON_WALL_DIALOG);
        if (!disable) {
            if (!getStatus(SEScene.STATUS_ON_DESK_SIGHT)) {
                new SECommand(getScene()) {
                    public void run() {
                        runACircle(null);
                    }
                }.execute();
            }
        }
    }

    private boolean getStatus(int type) {
        return getScene().getStatus(type);
    }

    @Override
    public void shortcutAction(Context context, Intent data) {
        final Intent shortIntent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        if (shortIntent == null) {
            return;
        }
        final String shortUri = shortIntent.toURI();
        if (HomeApplication.ACTION_INSTALL_SHORTCUT.equals(data.getAction())) {
            final String title = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
            Bitmap icon = null;
            final ComponentName componentName = shortIntent.getComponent();
            Parcelable bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
            if (bitmap != null && bitmap instanceof Bitmap) {
                icon = (Bitmap) bitmap;
            } else {
                Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                if (extra != null && extra instanceof ShortcutIconResource) {
                    try {
                        ShortcutIconResource iconResource = (ShortcutIconResource) extra;
                        Resources resources = getContext().getPackageManager().getResourcesForApplication(
                                iconResource.packageName);
                        int id = resources.getIdentifier(iconResource.resourceName, null, null);
                        icon = BitmapFactory.decodeResource(resources, id);
                    } catch (Exception e) {
                    }
                }
            }
            final Bitmap shortIcon = icon;
            boolean duplicate = data.getBooleanExtra("duplicate", true);
            if (duplicate || !shortcutExists(shortUri)) {
                SELoadResThread.getInstance().process(new Runnable() {
                    public void run() {
                        final ObjectInfo info = new ObjectInfo();
                        info.mName = AppObject.generateShortcutName();
                        info.mSceneName = getScene().mSceneName;
                        info.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
                        info.mType = ModelInfo.Type.SHORTCUT_ICON;
                        info.mObjectSlot.mSpanX = 1;
                        info.mObjectSlot.mSpanY = 1;
                        info.mShortcutUrl = shortUri;
                        info.mComponentName = componentName;
                        info.mClassName = ShortcutObject.class.getName();
                        final ShortcutObject shortcutObject = new ShortcutObject(getScene(), info.mName, info.mIndex);
                        shortcutObject.setObjectInfo(info);
                        SERect3D rect = new SERect3D(new SEVector3f(1, 0, 0), new SEVector3f(0, 0, 1));
                        int w = (int) getWallUnitSizeX() * info.getSpanX();
                        int h = (int) getWallUnitSizeY() * info.getSpanY();
                        rect.setSize(w, h, 1);
                        ResolveInfo resolveInfo = HomeUtils.findResolveInfoByComponent(getContext(),
                                info.mComponentName);
                        if (resolveInfo == null) {
                            return;
                        }
                        String label = title;
                        if (TextUtils.isEmpty(label)) {
                            PackageManager pm = getContext().getPackageManager();
                            label = resolveInfo.loadLabel(pm).toString();
                        }
                        info.mDisplayName = label;
                        Bitmap bitmapWithText;
                        if (shortIcon == null) {
                            info.mDisplayName = title;
                            bitmapWithText = AppItemInfo.getBitmap(resolveInfo, label, w, h);
                        } else {
                            bitmapWithText = AppItemInfo.getBitmapWithText(shortIcon, label, w, h);
                            info.mShortcutIcon = new SEBitmap(shortIcon, SEBitmap.Type.normal);
                        }
                        String imageName = info.mName + "_imageName";
                        String imageKey = info.mName + "_imageKey";
                        SEBitmap seBitmap = new SEBitmap(bitmapWithText, SEBitmap.Type.normal);
                        shortcutObject.mIconObject = new SEObject(getScene(), info.mName + "_icon");
                        SEObjectFactory.createRectangle(shortcutObject.mIconObject, rect, imageName, imageKey, seBitmap);
                        int bitmapW = w;
                        int bitmapH = h;
                        float scale = 1;
                        if (bitmapW > 128) {
                            scale = 128f / bitmapW;
                            bitmapH = (int) (bitmapH * scale);
                            bitmapW = 128;

                        }
                        shortcutObject.mIconObject.setImageSize(bitmapW, bitmapH);
                        shortcutObject.setObjectInfo(info);
                        new SECommand(getScene()) {
                            public void run() {
                                getScene().getContentObject().addChild(shortcutObject, true);
                                shortcutObject.getObjectInfo().saveToDB();
                                shortcutObject.initStatus(getScene());
                                getVesselLayer().placeObjectToVessel(shortcutObject, null);
                            }
                        }.execute();
                    }
                });
            } else {
                shortIcon.recycle();
                RemoveExistsShortcut(shortUri);
            }
        }

    }

    private void RemoveExistsShortcut(final String uri) {
        new SECommand(getScene()) {
            public void run() {
                List<NormalObject> newItems = findShortcut(uri);
                for (NormalObject obj : newItems) {
                    removeChild(obj, true);
                }
            }
        }.execute();
    }

    private List<NormalObject> findShortcut(String uri) {
        List<NormalObject> newItems = new ArrayList<NormalObject>();
        for (SEObject item : mChildObjects) {
            if (item instanceof ShortcutObject) {
                NormalObject appObject = (NormalObject) item;
                if (uri.equals(appObject.getObjectInfo().mShortcutUrl)) {
                    newItems.add(appObject);
                }
            }
        }
        return newItems;
    }

    private boolean shortcutExists(String uri) {
        for (SEObject item : mChildObjects) {
            if (item instanceof ShortcutObject) {
                NormalObject appObject = (NormalObject) item;
                if (uri.equals(appObject.getObjectInfo().mShortcutUrl)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case HomeScene.REQUEST_CODE_BIND_WIDGET:
            if (data != null) {
                int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
                if (data.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
                    appWidgetId = data.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
                }
                if (resultCode == Activity.RESULT_OK) {
                    setAppWidget(appWidgetId);
                } else {
                    mSESceneManager.getAppWidgetHost().deleteAppWidgetId(appWidgetId);
                }
            }
            break;
        case HomeScene.REQUEST_CODE_SELECT_WIDGET:
            if (data != null) {
                int appWidgetId = data.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
                if (resultCode == Activity.RESULT_OK) {
                    finishSetAppWidget(appWidgetId);
                } else {
                    mSESceneManager.getAppWidgetHost().deleteAppWidgetId(appWidgetId);
                }
            }
            break;
        case HomeScene.REQUEST_CODE_SELECT_SHORTCUT:
            SEObject object =  getScene().getTouchDelegate(); 
            if (object == null || !(object instanceof NormalObject)) {
                return;
            }
            final NormalObject normalObject = (NormalObject)object;
            if (resultCode == Activity.RESULT_OK) {
                if (normalObject instanceof ShortcutObject) {
                    ((ShortcutObject)normalObject).updateShortcut(data);
                } else {
                    ((IconBox)normalObject).updateShortcut(data);
                }
            } else {
                new SECommand(getScene()) {
                    public void run() {
                        SEObjectGroup group = (SEObjectGroup) normalObject.getParent();
                        group.removeChild(normalObject, true);
                        if(group instanceof House) {
                            WallShelf shelf = getWallShelfWithObject(normalObject.getObjectSlot().mSlotIndex, normalObject);
                            int mountPointIndex = normalObject.getObjectSlot().mMountPointIndex;
                            removeObjectFromCurrentShelf(normalObject, mountPointIndex, shelf);
                        }
                        normalObject.getObjectInfo().releaseDB();
                        if (group instanceof Folder) {
                            Folder floder = (Folder) group;
                            if (floder.mChildObjects.size() == 1) {
                                NormalObject icon = (NormalObject) floder.mChildObjects.get(0);
                                floder.changeToAppIcon();
                                icon.getObjectInfo().updateSlotDB();
                            }
                        }
                    }
                }.execute();
            }
            getScene().removeTouchDelegate();
            break;
        case HomeScene.REQUEST_CODE_SELECT_WALLPAPER_IMAGE:
            if (resultCode == Activity.RESULT_OK) {
                final ImageItem imageItem = new ImageItem();
                imageItem.mImageName = mCurrentImage;
                String path = HomeUtils.PKG_FILES_PATH + mCurrentTheme;
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                final float h2wRatio = groundpaperKeys.contains(mCurrentImage) ? 1.0f : getWallHeight2WidthRatio();
                imageItem.mNewPath = path + "/" + mCurrentImage;
                SELoadResThread.getInstance().process(new Runnable() {
                    public void run() {
                        if (!WallpaperUtils.encodePaperFile(mSdcardTempFile.getAbsolutePath(),
                                imageItem.mNewPath, h2wRatio)) {
                            return;
                        }

//                        BitmapFactory.Options options = new BitmapFactory.Options();
//                        options.inJustDecodeBounds = true;
//                        BitmapFactory.decodeFile(mSdcardTempFile.getAbsolutePath(), options);
//                        options.inSampleSize = HomeUtils.computeSampleSize(options, -1,  1024 * 1024);
//                        options.inJustDecodeBounds = false;
//                        Bitmap bm = BitmapFactory.decodeFile(mSdcardTempFile.getAbsolutePath(), options);
//                        if (bm == null) {
//                            return;
//                        }
//                        final int w = bm.getWidth();
//                        final int h = bm.getHeight();
//
//                        int size = HomeUtils.higherPower2(w);
//                        if (size > 1024) {
//                            size = 1024;
//                        }
//                        Bitmap des = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
//                        Rect srcRect = new Rect(0, 0, w, h);
//                        int newH = h * size / w;
////                        Rect desRect = new Rect((size - newW) / 2, 0, (size + newW) / 2, size);
//                        Rect desRect = new Rect(0, (size - newH)/2 , size, (size + newH) / 2);
//                        Canvas canvas = new Canvas(des);
//                        canvas.drawBitmap(bm, srcRect, desRect, null);
//                        bm.recycle();
//                        HomeUtils.saveBitmap(des, imageItem.mNewPath, Bitmap.CompressFormat.JPEG);
//                        des.recycle();
                        final int image = SEObject.loadImageData_JNI(imageItem.mNewPath);
                        if (image != 0) {
                            ModelInfo.updateWallPaperDB(getContext(), imageItem.mNewPath, mCurrentImage, mCurrentTheme);
                            new SECommand(getScene()) {
                                public void run() {
                                    SEObject.applyImage_JNI(mCurrentImage, imageItem.mNewPath);
                                    SEObject.addImageData_JNI(imageItem.mNewPath, image);
                                }
                            }.execute();
                        }
                        System.gc();
                    }
                });
            }
            break;
        case HomeScene.REQUEST_CODE_SELECT_WALLPAPER_CAMERA:
            if (resultCode == Activity.RESULT_OK) {
                Intent intent2 = new Intent("com.android.camera.action.CROP");
                Uri u = Uri.fromFile(mSdcardTempFile);
                intent2.setDataAndType(u, "image/*");
                intent2.putExtra("output", Uri.fromFile(mSdcardTempFile));
                intent2.putExtra("crop", "true");
                intent2.putExtra("aspectX", mImgSizeX);
                intent2.putExtra("aspectY", mImgSizeY);
                intent2.putExtra("outputFormat", "JPEG");
                mSESceneManager.startActivityForResult(intent2, HomeScene.REQUEST_CODE_SELECT_WALLPAPER_IMAGE);
            }
            break;
        }
    }

    private void setAppWidget(int appWidgetId) {
        /* Check for configuration */
        AppWidgetProviderInfo providerInfo = mSESceneManager.getAppWidgetManager().getAppWidgetInfo(appWidgetId);
        if (providerInfo.configure != null) {
            Intent configureIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            configureIntent.setComponent(providerInfo.configure);
            configureIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            if (configureIntent != null) {
                mSESceneManager.startActivityForResult(configureIntent, HomeScene.REQUEST_CODE_SELECT_WIDGET);
            }
        } else {
            finishSetAppWidget(appWidgetId);
        }
    }

    private void finishSetAppWidget(int appWidgetId) {
        final AppWidgetProviderInfo providerInfo = mSESceneManager.getAppWidgetManager().getAppWidgetInfo(appWidgetId);
        if (providerInfo != null) {
            final ObjectInfo info = new ObjectInfo();
            info.mAppWidgetId = appWidgetId;
            info.mName = "widget_" + System.currentTimeMillis();
            info.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
            info.mClassName = WidgetObject.class.getName();
            info.mSceneName = getScene().mSceneName;
            info.mType = "Widget";
            final int[] span = HomeUtils.getSpanForWidget(getContext(), providerInfo);
            info.mObjectSlot.mSpanX = span[0];
            info.mObjectSlot.mSpanY = span[1];
            info.mComponentName = providerInfo.provider;
            final WidgetObject widget = new WidgetObject(getScene(), info.mName, info.mIndex);
            widget.setObjectInfo(info);
            info.saveToDB();
            new SECommand(getScene()) {
                public void run() {
                    final SEObjectGroup root = (SEObjectGroup) getScene().getContentObject();
                    root.addChild(widget, false);
                    widget.load(getScene().getContentObject(), new Runnable() {
                        public void run() {
                            if (root.mChildObjects.contains(widget)) {
                                getVesselLayer().placeObjectToVessel(widget, null);
                            }
                        }
                    });
                }
            }.execute();

        }
    }

    private Bundle prepareWallpaper(SEObject obj, int type) {
        switch (type) {
            case TYPE_WALL:
                SEVector.SEVector2f bounder = getWallXZBounder();
                mImgSizeX = (int)(bounder.getX() + 0.5);
                mImgSizeY = (int)(bounder.getY() + 0.5);
                break;
            case TYPE_FLOOR:
                mImgSizeX = 256;
                mImgSizeY = 256;
                break;
        }
        if (mSdcardTempFile == null) {
            File dir = new File(HomeUtils.SDCARD_PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            mSdcardTempFile = new File(HomeUtils.SDCARD_PATH + "/" + ".tempimage");
        }
        mCurrentImage = obj.getImageName_JNI();

        Bundle msg = new Bundle();
        msg.putString(HomeScene.MSG_CONTENT_IMAGE, mCurrentImage);
        msg.putString(HomeScene.MSG_CONTENT_IMAGE_OUTPUT, Uri.fromFile(mSdcardTempFile).toString());
        msg.putInt(HomeScene.MSG_CONTENT_IMAGE_SIZE_X, mImgSizeX);
        msg.putInt(HomeScene.MSG_CONTENT_IMAGE_SIZE_Y, mImgSizeY);
        return msg;
    }


    public int getWallIndex() {
        return getWallNearestIndex();
    }

    public void addWallChangedListener(WallChangedListener l) {
        if (!mWallChangedListeners.contains(l)) {
            mWallChangedListeners.add(l);
        }
    }

    public void removeWallChangedListener(WallChangedListener l) {
        if (l != null) {
            mWallChangedListeners.remove(l);
        }
    }

    public void notifyWallChanged(int index, int num) {
        for (WallChangedListener l : mWallChangedListeners) {
            l.onWallChanged(index, num);
        }
    }

    public void notifyWallPositionUpdated(float index, int num) {
        for (WallChangedListener l : mWallChangedListeners) {
            l.onWallPositionUpdated(index, num);
        }
    }

    public SEVector.SEVector2f getWallXZBounder() {
        SEVector3f xyzBounder = getFirstWallObject().getObjectXYZSpan();
        return new SEVector2f(xyzBounder.getX(), xyzBounder.getZ());
    }

    private float getWallHeight2WidthRatio() {
        SEVector3f xyzBounder = getFirstWallObject().getObjectXYZSpan();
        return xyzBounder.getZ() / xyzBounder.getX();
    }
    
    public static boolean IsScreenOrientationPortrait(Context context) {
    	 if(getOrientation(context) == Configuration.ORIENTATION_PORTRAIT){
    		 return true;
    	 }else {
    		 return false;
    	 }
    }

    ArrayList<String> groundpaperKeys = new ArrayList<String>();
    ArrayList<String> wallpaperKeys = new ArrayList<String>();

    @Override
    public ArrayList<String> getWallpaperKeySet() {
        return wallpaperKeys;
    }

    @Override
    public ArrayList<String> getGroundpaperKeySet() {
        return groundpaperKeys;
    }

    private static int getOrientation(Context context) {
        return SettingsActivity.getPreferRotation(context);
    }
    
	@Override
	public void selectObjectCallBack(String proName) {
		SEScene scene = getScene();
    	//load all model info
		HomeDataBaseHelper help = HomeDataBaseHelper.getInstance(SESceneManager.getInstance()
                .getContext());
        SQLiteDatabase db = help.getWritableDatabase();
//        ModelInfo modelInfo = scene.mSceneInfo.findModelInfo(objectName);
        ModelInfo modelInfo = null ;
        String where = ModelColumns.PRODUCT_ID + " = '" + proName + "'" ;
        Cursor cursor = db.query(Tables.MODEL_INFO, null, where, null, null, null, null);
        if(cursor != null) {
        	if(cursor.moveToFirst()) {
        		modelInfo = ModelInfo.CreateFromDB(cursor);
        	}
        	cursor.close();
        	cursor = null;
        }
        
        if(modelInfo == null) {
        	Log.e(TAG, "object modelinfo is null");
        	return ;
        }
		ObjectInfo objInfo = new ObjectInfo();
		objInfo.setModelInfo(modelInfo);
		objInfo.mIndex = ProviderUtils.searchMaxIndex(scene, ProviderUtils.Tables.OBJECTS_INFO, objInfo.mName) + 1;
		objInfo.mSceneName = scene.mSceneName;
		objInfo.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
		objInfo.mObjectSlot.mSpanX = 1;
		objInfo.mObjectSlot.mSpanY = 1;
		if(SettingsActivity.getPreferRotation(getContext()) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
			objInfo.mVesselName = "home4mian";
    	}else {
    		objInfo.mVesselName = "home8mianshu";
    	}
		
        objInfo.saveToDB(db);
        
		final NormalObject newObject = HomeUtils.getObjectByClassName(scene, objInfo);
        if (null != newObject) {
            newObject.changeParent(getRoot());
            MyAnimEnd anim = new MyAnimEnd();
            anim.newObject = newObject;
            newObject.load(this, anim);
        }
	}
	
	private class MyAnimEnd implements Runnable {
    	public NormalObject newObject;
        public void run() {
        	new SECommand(getScene()) {
                public void run() {
                    getVesselLayer().placeObjectToVessel(newObject, null);
                    if(newObject.getBoundaryPoint() != null) {
                    	int lastWallIndex = newObject.getBoundaryPoint().wallIndex;
                    	toFace(lastWallIndex, null, 5);
                    }
                }
            }.execute();
        }
    }
}
