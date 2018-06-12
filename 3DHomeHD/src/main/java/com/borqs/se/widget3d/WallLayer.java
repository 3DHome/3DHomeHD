package com.borqs.se.widget3d;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.sqlite.SQLiteDatabase;
import android.util.Xml;
import android.util.Log;

import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEScene;
import com.borqs.se.home3d.HomeDataBaseHelper;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.ToastUtils;
import com.borqs.se.home3d.UpdateDBThread;
import com.borqs.se.engine.SEVector.SERay;
import com.borqs.se.engine.SEVector.SEVector2f;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.home3d.XmlUtils;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectData;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SESceneInfo;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;
import com.borqs.framework3d.home3d.SEMountPointChain.MatrixPoint;
import com.borqs.framework3d.home3d.SEDebug;
import com.borqs.framework3d.home3d.SEMountPointManager;
import com.borqs.framework3d.home3d.SEMountPointChain;
import com.borqs.framework3d.home3d.SEMountPointData;
import com.borqs.framework3d.home3d.SEObjectBoundaryPoint;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * NOTE: our wall is placed by counter-clockwise, wall1 -> wall2 ->wall3 -> ...
 *
 * */
public class WallLayer extends VesselLayer {
	private final String TAG = "WallLayer";
    private SetToRightPositionAnimation mSetToRightPositionAnimation;
    private SetToRightPositionAnimation_New mSetToRightPositionAnimationNew;
    private SEObject mObjectLine;
    private SEObject mObjectLine_new;
    private List<ConflictObject> mExistentObjects;

    private ObjectSlot mObjectSlot;
    private SESceneInfo mSceneInfo;
    private SECamera mCamera;
    private House mHouse;

    private boolean mNeedRotateWall;
    private SEVector3f mRealLocation;
    private SETransParas mObjectTransParas;
    private SEVector3f mRealLocationInWall;
    private SEVector3f mLocationInWallForWidget;
    private SEVector3f mObjectTransParasTranslateInWall;
    private float mVirtualWallRadius;
    private float mSkyRadius;
    private ACTION mPreAction;
    private VesselLayer mCurrentLayer;
    private MountPointArea mMountPointArea = new MountPointArea();
    private boolean mOnCurrentWallRight;
    private boolean mOnCurrentWallLeft;
    private SEVector3f mWallMinPoint;
    private SEVector3f mWallMaxPoint;
    private SEObjectBoundaryPoint mMovedObjectOriginBP;
    private boolean mHasIntersectObject = false;
    private boolean mAllIntersectObjectMoveOK = true;
    private static String mObjectContainerName = "qiang_8_point";
    private static String mShelfContainerName = "wall_jiazi_6_point";
    /////
    private static class MountPointArea {
    	    ArrayList<SEObject> mList = new ArrayList<SEObject>();
    	    SEMountPointChain.BoundaryPoint mBoundaryPoint;
    }
    ////
    ////

    /////
    public WallLayer(SEScene scene, VesselObject vesselObject) {
        super(scene, vesselObject);
        mCurrentLayer = null;
        mHouse = (House) getVesselObject();
        mSceneInfo = getScene().mSceneInfo;
        mCamera = getScene().getCamera();
        mVirtualWallRadius = mHouse.getWallRadius() * 0.8f;
        mSkyRadius = mSceneInfo.mSkyRadius * 0.6f;
    }
    public static String getObjectContainerName() {
        return mObjectContainerName;
    }
    public static String getShelfContainerName() {
        return mShelfContainerName;
    }
    @Override
    public boolean setOnLayerModel(NormalObject onMoveObject, boolean onLayerModel) {
        super.setOnLayerModel(onMoveObject, onLayerModel);
        if (onLayerModel) {
            mInRecycle = false;
            mObjectSlot = null;
            mExistentObjects = getExistentObject();
            if(HomeUtils.DEBUG) {
                showLine(true);
            }
        } else {
            if(HomeUtils.DEBUG) {
                showLine(false);
            }
            disableCurrentLayer();
            cancelRotation();
            mHouse.toNearestFace(null, 5);
        }
        return true;
    }
    private static class MountPointInfo {
    	    public SEMountPointChain.ClosestMountPointData mountPointInfo;
    	    public ConflictObject conflictObject;
    	    public int wallIndex;
    }
    private ConflictObject getConflictObjectInMountPoint(int wallIndex, int mountPointIndex) {
        ArrayList<NormalObject> objectsInWall = mHouse.getNormalObjectInWall(wallIndex);
    	    for(NormalObject obj : objectsInWall) {
                /*
                Log.i(TAG, "##### " + i + " ###");
                Log.i(TAG, "### obj = " + obj.mConflictObject.mName + " ##");
                Log.i(TAG, "mp index = " + mountPointIndex);
                Log.i(TAG, "### obj mount point index = " + obj.mConflictObject.getObjectSlot().mMountPointIndex + " ##");
                i++;
                Log.i(TAG, "###################");
                */
                if(obj.getObjectSlot().mMountPointIndex == mountPointIndex) {
                    ConflictObject conflictObject = new ConflictObject();
                    conflictObject.mConflictObject = obj;
                    return conflictObject;
                }
    	    }
    	    return null;
    }
    //wall must be counter-clockwise : 1, 2, 3, 4
    private String getWallName(int wallIndex) {
    	
    	int index = wallIndex;//(mHouse.getCount() - wallIndex) % mHouse.getCount();
//      String vesselName = "wall" + "0" + (index + 1) + "@home8";
    	String vesselName = "";
    	if(isScreenOrientationPortrait()) {
    		vesselName = "qiang0" + (index + 1) + "@home8mianshu";
    	}else {
    		vesselName = "wall" + "0" + (index + 1) + "@" + mHouse.getName();
    	}
      return vesselName;
	    
    	    /*
    	    int index = (mSceneInfo.mWallNum - wallIndex) % mSceneInfo.mWallNum; 
    	    int num = 0;
    	    switch(wallIndex) {
    	    case 0:
    	    	    num = 1;
    	    	    break;
    	    case 1:
    	    	    num = 7;
    	    	    break;
    	    case 2:
    	    	    num = 6;
    	    	    break;
    	    case 3:
    	    	    num = 4;
    	    	    break;
    	    case 4:
    	    	    num = 5;
    	    	    break;
    	    case 5:
    	    	num = 3;
    	        break;
    	    case 6:
    	    	num = 2;
    	    	    break;
    	    case 7:
    	    	num = 8;
    	    	break;
    	    }
    	    String vesselName = "wall" + num + "@home8";
    	    return vesselName;
    	    */
    }
    private SEVector3f wallSpaceCoordinateToWorld(int wallIndex, SEVector3f spaceCoordinate) {
    	    String vesselName = this.getWallName(wallIndex);
	    float[] worldFingerLocation = new float[4];
	    float[] objectFingerLocation = new float[4];
	    objectFingerLocation[3] = 1;
	    for(int i = 0 ; i < 3 ; i++) {
	    	objectFingerLocation[i] = spaceCoordinate.mD[i];
	    }
	    SEObject object = getScene().findObject(vesselName, 0);
	    object.toWorldCoordinate(objectFingerLocation, worldFingerLocation);
	    SEVector3f newLocation = new SEVector3f(worldFingerLocation[0], worldFingerLocation[1], worldFingerLocation[2]);
	    return newLocation;
    }
    private SEVector3f toWallSpaceCoordinate(int wallIndex, SEVector3f worldCoordinate) {
	    String vesselName = this.getWallName(wallIndex);
	    float[] worldFingerLocation = new float[4];
	    float[] objectFingerLocation = new float[4];
	    worldFingerLocation[3] = 1;
	    for(int i = 0 ; i < 3 ; i++) {
	    	    worldFingerLocation[i] = worldCoordinate.mD[i];
	    }
	    SEObject object = getScene().findObject(vesselName, 0);
	    object.toObjectCoordinate(worldFingerLocation, objectFingerLocation);
	    SEVector3f newLocation = new SEVector3f(objectFingerLocation[0], objectFingerLocation[1], objectFingerLocation[2]);
	    return newLocation;
    }
    private SEMountPointChain getCurrentMountPointChain(int wallIndex) {
    	String vesselName = getWallName(wallIndex);
    	String objectName = getOnMoveObject().mName;
    	SEMountPointManager mountPointManager = getScene().getMountPointManager();
        String containerName = mObjectContainerName;
	    SEMountPointChain mpc = mountPointManager.getMountPointChain(objectName, mHouse.mName,vesselName, containerName);
	    return mpc;
    }
    //return null if your object is on left or on right margin or in recycle
    //else return the nearest mount point information and if current mount point has object
    // it will contain this object also in conflictObject.
    private MountPointInfo calculateNearestMountPoint(int wallIndex, String objectName) {
    	    if(onLeft() || onRight() || mInRecycle) {
    	    	    return null;
    	    }
    	    //for test
    	    /*
    	    float[] outData = new float[10];
        getScene().getLocalTransformsByObjectName("group_house8", 0, outData);
        outData = new float[10];
        getScene().getLocalTransformsByObjectName("wall1@home8", 0, outData);
        Log.i(TAG, "## wall1 = " + outData[0] + ", " + outData[1] + ", " + outData[2] + ", " + 
                                 outData[3] +  ", " + outData[4] +  ", " + outData[5]
                                 +  ", " + outData[6]+  ", " + outData[7]+  ", " + outData[8]
                                		 +  ", " + outData[9]);
        outData = new float[10];
        getScene().getLocalTransformsByObjectName("wall2@home8", 0, outData);
        Log.i(TAG, "## wall2 = " + outData[0] + ", " + outData[1] + ", " + outData[2] + ", " + 
                outData[3] +  ", " + outData[4] +  ", " + outData[5]
                +  ", " + outData[6]+  ", " + outData[7]+  ", " + outData[8]
               		 +  ", " + outData[9]);
        outData = new float[10];
        getScene().getLocalTransformsByObjectName("wall3@home8", 0, outData);
        Log.i(TAG, "## wall3 = " + outData[0] + ", " + outData[1] + ", " + outData[2] + ", " + 
                outData[3] +  ", " + outData[4] +  ", " + outData[5]
                +  ", " + outData[6]+  ", " + outData[7]+  ", " + outData[8]
               		 +  ", " + outData[9]);
        outData = new float[10];
        getScene().getLocalTransformsByObjectName("wall4@home8", 0, outData);
        Log.i(TAG, "## wall4 = " + outData[0] + ", " + outData[1] + ", " + outData[2] + ", " + 
                outData[3] +  ", " + outData[4] +  ", " + outData[5]
                +  ", " + outData[6]+  ", " + outData[7]+  ", " + outData[8]
               		 +  ", " + outData[9]);
        outData = new float[10];
        getScene().getLocalTransformsByObjectName("wall5@home8", 0, outData);
        Log.i(TAG, "## wall5 = " + outData[0] + ", " + outData[1] + ", " + outData[2] + ", " + 
                outData[3] +  ", " + outData[4] +  ", " + outData[5]
                +  ", " + outData[6]+  ", " + outData[7]+  ", " + outData[8]
               		 +  ", " + outData[9]);
        outData = new float[10];
        getScene().getLocalTransformsByObjectName("wall6@home8", 0, outData);
        Log.i(TAG, "## wall6 = " + outData[0] + ", " + outData[1] + ", " + outData[2] + ", " + 
                outData[3] +  ", " + outData[4] +  ", " + outData[5]
                +  ", " + outData[6]+  ", " + outData[7]+  ", " + outData[8]
               		 +  ", " + outData[9]);
        outData = new float[10];
        getScene().getLocalTransformsByObjectName("wall7@home8", 0, outData);
        Log.i(TAG, "## wall7 = " + outData[0] + ", " + outData[1] + ", " + outData[2] + ", " + 
                outData[3] +  ", " + outData[4] +  ", " + outData[5]
                +  ", " + outData[6]+  ", " + outData[7]+  ", " + outData[8]
               		 +  ", " + outData[9]);
        outData = new float[10];
        getScene().getLocalTransformsByObjectName("wall8@home8", 0, outData);
        Log.i(TAG, "## wall8 = " + outData[0] + ", " + outData[1] + ", " + outData[2] + ", " + 
                outData[3] +  ", " + outData[4] +  ", " + outData[5]
                +  ", " + outData[6]+  ", " + outData[7]+  ", " + outData[8]
               		 +  ", " + outData[9]);
        */
    	    //end
    	    //Log.i(TAG, "## current wall index = " + wallIndex + ", objname = " + objectName);
    	    
        String vesselName = this.getWallName(wallIndex);

        SEVector3f newLocation = mRealLocationInWall.clone();//toWallSpaceCoordinate(wallIndex, this.mRealLocation);
        SEMountPointManager mountPointManager = getScene().getMountPointManager();
        SEMountPointChain mpc = mountPointManager.getMountPointChain(objectName, mHouse.mName,vesselName, mObjectContainerName);
        SEMountPointChain.ClosestMountPointData cmd = mpc.getClosestMountPoint(newLocation);
        if(cmd != null) {
            //Log.i(TAG, "## closest point = " + cmd.mIndex);
        }
        MountPointInfo info = new MountPointInfo();
        info.mountPointInfo = cmd;
        info.wallIndex = wallIndex;
        info.conflictObject = getConflictObjectInMountPoint(wallIndex, cmd.mIndex);
        if(info.conflictObject != null) {
            //Log.i(TAG, "# conflictObject = " + info.conflictObject.mConflictObject.mName);
        } else {
            //Log.i(TAG, "# conflictObject = " + null);
        }
        return info;
    }

    private SEVector3f calculateRealPointInWall(int wallIndex) {
    	SEVector3f coordInWall = this.toWallSpaceCoordinate(wallIndex, mRealLocation);
    	return coordInWall;
    }
    private SETransParas setAppObjectToWall(SEMountPointData mpd,
    		                              int wallIndex, 
    		                              int mountPointIndex, 
    		                              NormalObject currentMoveObject,
                                          SEVector3f realLocationInWall) {
        SEVector3f wallSpaceCoord;
        if (null == mpd) {
            wallSpaceCoord = new SEVector3f(0, 0, 0);
        } else {
            wallSpaceCoord = mpd.getTranslate();
            if (null == wallSpaceCoord) {
                wallSpaceCoord = new SEVector3f(0, 0, 0);
            } else {
                wallSpaceCoord = wallSpaceCoord.clone();
            }
        }

        wallSpaceCoord.mD[1] = realLocationInWall.mD[1];
	    SETransParas dstTransParas = createUserTransParasFromWallTransform(wallIndex, wallSpaceCoord);
	    setAppObjectBoundaryPoint(currentMoveObject, wallIndex, mountPointIndex);
        if(currentMoveObject.getObjectInfo().mIsNativeObject) {
            dstTransParas.mScale = createNativeShelfObjectScale(currentMoveObject);
        }
	    return dstTransParas;
    }
    /*
    private SETransParas slotToWall_AppObjectNoIntersect(MountPointInfo mountPointInfo,
    		                                         NormalObject currentMoveObject,
    		                                         SEVector3f realLocationInWall) {
       return this.setAppObjectToWall(mountPointInfo.mountPointInfo.mMPD, 
    		                   mountPointInfo.wallIndex, mountPointInfo.mountPointInfo.mIndex, 
    		                   currentMoveObject, realLocationInWall);
    }
    */
    private SEMountPointChain.MatrixPoint getEmptyMountPointIndex(int wallIndex) {
        SEMountPointChain currentChain = this.getCurrentMountPointChain(wallIndex);
        int rowNum = currentChain.getRowCount();
        int colNum = currentChain.getColCount();
        List<ConflictObject> objects = this.getExistentObject(wallIndex);
        Log.i(TAG, "## current object is " + objects.size());
        for(int i = 0 ; i < rowNum ; i++) {
            for(int j = 0 ; j < colNum ; j++) {

                boolean found = false;

                for(ConflictObject object : objects) {
                    NormalObject tmpObj = object.mConflictObject;
                    found = this.isMatrixPointInObject(i, j, tmpObj);
                    if(found == true) {
                            break;
                    }
                }
                if(found == false) {
                    int mountPointIndex = currentChain.getIndex(i, j);
                    return new SEMountPointChain.MatrixPoint(i, j, mountPointIndex);
                }
            }
        }
        return null;
    }
    private void setObjectUserTransParas(int wallIndex, NormalObject object) {
    	SEObjectBoundaryPoint bp = object.getBoundaryPoint();
	    SEVector3f center = bp.center;
        SEVector3f yDist = getLocationYInWall(object, wallIndex);
	    if(object instanceof AppObject) {
            center.mD[1] = yDist.mD[1];
	    } else if(object instanceof WidgetObject) {
            center.mD[1] = yDist.mD[1];
	    }
	    //Log.i(TAG, " update obj trans = " + center);
	    SETransParas trans = this.createUserTransParasFromWallTransform(wallIndex, bp.center);
	    object.getUserTransParas().mTranslate = trans.mTranslate;
	    object.getUserTransParas().mRotate = trans.mRotate;
	    object.getUserTransParas().mScale = trans.mScale;
	    object.setUserTransParas();
    }
    /*
    private boolean placeWidgetOnWall(WidgetObject currentMoveObject, int wallIndex) {
    	currentMoveObject.createLocalBoundingVolume();
    	SEVector3f minPoint = new SEVector3f();
    	SEVector3f maxPoint = new SEVector3f();
    	currentMoveObject.getLocalBoundingVolume(minPoint, maxPoint);
    	SEVector3f xyzSpan = maxPoint.subtract(minPoint);
    	SEObjectBoundaryPoint bp = this.createProperBoundaryPoint(wallIndex, xyzSpan, currentMoveObject);
    	if(bp != null) {
    		currentMoveObject.setBoundaryPoint(bp);
    		return true;
    	} else {
    		return false;
    	}
    }
    */
    /*
    private boolean placeObjectOnWall(NormalObject currentMoveObject, int wallIndex){
    	createShelfBoundaryPoint(wallIndex);
		createObjectBoundaryPoint(wallIndex);
		if(currentMoveObject.getBoundaryPoint() != null) {
		    currentMoveObject.getBoundaryPoint().wallIndex = wallIndex;
		}
		if(currentMoveObject instanceof WidgetObject) {
		    boolean ret = placeWidgetOnWall((WidgetObject)currentMoveObject, wallIndex);
		    if(ret) {
		    	this.setObjectUserTransParas(wallIndex, currentMoveObject);
		    	((WidgetObject)currentMoveObject).requestUpdateAndroidWidget();
                currentMoveObject.getObjectSlot().mSlotIndex = wallIndex;
		    	return true;
		    } else {
		    	return false;
		    }
		    	
		} else if(currentMoveObject instanceof AppObject) {
			SEMountPointChain.MatrixPoint mp = this.getEmptyMountPointIndex(wallIndex);
			if(mp == null) {
				return false;
			}
			this.setAppObjectBoundaryPoint(currentMoveObject, wallIndex, mp.mountPointIndex);
			currentMoveObject.getObjectSlot().mMountPointIndex = mp.mountPointIndex;
			currentMoveObject.getObjectSlot().mSlotIndex = wallIndex;
			this.setObjectUserTransParas(wallIndex, currentMoveObject);
			return true;
		} else if(currentMoveObject.getObjectInfo().mIsNativeObject) {
			SEObjectBoundaryPoint bp = currentMoveObject.getBoundaryPoint();
			SEObjectBoundaryPoint newBP = this.createProperBoundaryPoint(wallIndex, bp.xyzSpan, currentMoveObject);
			if(newBP == null) {
				return false;
			}
			currentMoveObject.setBoundaryPoint(newBP);
            currentMoveObject.getObjectSlot().mSlotIndex = wallIndex;
			this.setObjectUserTransParas(wallIndex, currentMoveObject);
			return true;
		}
		return false;
    }
    */
    private int getLeftWallIndex(int wallIndex) {
    	return (wallIndex + 1) % mHouse.getCount();
    }
    /*
    private void sendMoveObjectToOtherWall() {
    	    NormalObject object = getOnMoveObject();
       	//SEObject parent = object.getParent();
     	//parent.removeChild(object, true);
     	int currentWallIndex = mHouse.getWallNearestIndex();
     	int leftWallIndex = getLeftWallIndex(currentWallIndex);
     	boolean placeOK = false;
     	//int rightWallIndex = currentWallIndex == 0 ? (mHouse.getCount() - 1) : (currentWallIndex - 1);
     	while(leftWallIndex != currentWallIndex) {
     	    placeOK = this.placeObjectOnWall(object, leftWallIndex);
     	    if(placeOK) {
     	    	    break;
     	    }
     	    leftWallIndex = getLeftWallIndex(leftWallIndex);
     	}
     	if(placeOK == false) {
     		SEObject parent = object.getParent();
         	parent.removeChild(object, true);
     	}
    }
    */
    /*
    private SETransParas slotToWall_AppObjectHasIntersect(MountPointInfo mountPointInfo,
    		                                                NormalObject currentMoveObject,
    		                                                SEVector3f realLocationInWall) {
	    SEMountPointChain.MatrixPoint mp = null;
	    if(mMovedObjectOriginBP == null) {
	        mp = this.getEmptyMountPointIndex(mountPointInfo.wallIndex);	        
	    } else {
	        mp = mMovedObjectOriginBP.minMatrixPoint;
	    }
	    if(mp == null) {
	    	    return null;
	    }
	    SEMountPointChain currentChain = this.getCurrentMountPointChain(mountPointInfo.wallIndex);
	    int mountPointIndex = currentChain.getIndex(mp.row, mp.col);
	    
	    SEMountPointData mpd = currentChain.getMountPointData(mountPointIndex);
	    SEVector3f wallSpaceCoord = mpd.getTranslate().clone();
	    wallSpaceCoord.mD[1] = realLocationInWall.mD[1];
	    SETransParas dstTransParas = this.createUserTransParasFromWallTransform(mountPointInfo.wallIndex, wallSpaceCoord);
	    
	    this.setAppObjectBoundaryPoint(currentMoveObject, mountPointInfo.wallIndex, mountPointIndex);
	    currentMoveObject.getObjectSlot().mMountPointIndex = mountPointIndex;
	    currentMoveObject.getObjectSlot().mSlotIndex = mountPointInfo.wallIndex;
	    return dstTransParas;
    }
    private SETransParas slotToWall_NativeObjectNoIntersect(MountPointInfo mountPointInfo,
                                                          NormalObject currentMoveObject,
                                                          SEVector3f realLocationInWall) {
	    SEObjectBoundaryPoint currentObjectBP = currentMoveObject.getBoundaryPoint();
	    SEVector3f center = realLocationInWall.clone();
	    if(this.isObjectFixedOnYAxis(currentMoveObject)) {
        	center.mD[1] = mRealLocationInWall.mD[1];
        } else {
        	center.mD[1] += 70;
        }
	    SEObjectBoundaryPoint newObjectBP = SEObjectBoundaryPoint.getMinMaxPointInPlane(center, currentObjectBP.xyzSpan, currentObjectBP.movePlane, SEObjectBoundaryPoint.CENTER_POINT_STYLE_MID_MID);
 	    SEMountPointChain currentChain = this.getCurrentMountPointChain(mountPointInfo.wallIndex);
	    SEMountPointChain.MatrixPoint minMatrixPoint = currentChain.getMatrixPointInPlaneXZ(newObjectBP.minPoint);
	    SEMountPointChain.MatrixPoint maxMatrixPoint = currentChain.getMatrixPointInPlaneXZ(newObjectBP.maxPoint);
	    newObjectBP.minMatrixPoint = minMatrixPoint;
	    newObjectBP.maxMatrixPoint = maxMatrixPoint;
	    newObjectBP.xyzSpan = currentObjectBP.xyzSpan;
	    newObjectBP.movePlane = currentObjectBP.movePlane;
	    newObjectBP.center = center;
	    newObjectBP.wallIndex = mountPointInfo.wallIndex;
	    currentMoveObject.setBoundaryPoint(newObjectBP);
	    SETransParas dstTransParas = this.createUserTransParasFromWallTransform(mountPointInfo.wallIndex, center);
	    return dstTransParas;
    	    
    }
    */
    /*
    private SETransParas slotToWall_NativeObjectHasIntersect(MountPointInfo mountPointInfo,
                                                           NormalObject currentMoveObject,
                                                           SEVector3f realLocationInWall) {
    	if(mMovedObjectOriginBP != null) {
    	    SEVector3f center = mMovedObjectOriginBP.center;
    	    currentMoveObject.setBoundaryPoint(mMovedObjectOriginBP);
    	    SETransParas dstTransParas = this.createUserTransParasFromWallTransform(mountPointInfo.wallIndex, center);
    	    return dstTransParas;
    	}
      	SEObjectBoundaryPoint currentBP = currentMoveObject.getBoundaryPoint();
    	SEObjectBoundaryPoint properBP = this.createProperBoundaryPoint(mountPointInfo.wallIndex, currentBP.xyzSpan, currentMoveObject);
	    if(properBP == null) {
	    	    return null;
	    }
        if(this.isObjectFixedOnYAxis(currentMoveObject)) {
        	properBP.center.mD[1] = mRealLocationInWall.mD[1];
        } else {
        	properBP.center.mD[1] += 70;
        }
	    currentMoveObject.setBoundaryPoint(properBP);
	    SETransParas dstTransParas = this.createUserTransParasFromWallTransform(mountPointInfo.wallIndex, properBP.center);
	    return dstTransParas;
    }
    */
    private SEVector3f objectToWallSpace(int wallIndex, NormalObject object) {
    	SEVector3f worldCenter = mRealLocation;//this.mObjectTransParas.mTranslate;//object.toWorldPoint(new SEVector3f(0, 0, 0));
    	SEVector3f currentCenter = this.toWallSpaceCoordinate(wallIndex, worldCenter);
    	return currentCenter;
    }

    private boolean isObjectOutOfWallBoundary(int wallIndex, NormalObject object) {

    	SEObjectBoundaryPoint currentBP = object.getBoundaryPoint();
        if(currentBP == null) {
            object.createLocalBoundingVolume();
            SEVector3f minPoint = new SEVector3f();
            SEVector3f maxPoint = new SEVector3f();
            object.getLocalBoundingVolume(minPoint, maxPoint);
            currentBP = new SEObjectBoundaryPoint(wallIndex);
            currentBP.xyzSpan = maxPoint.subtract(minPoint);
            currentBP.movePlane = SEObjectBoundaryPoint.MOVE_PLANE_XZ;
        }
    	SEVector3f currentCenter = this.objectToWallSpace(wallIndex, object);
    	SEObjectBoundaryPoint newBP = SEObjectBoundaryPoint.getMinMaxPointInPlane(currentCenter, currentBP.xyzSpan, currentBP.movePlane, SEObjectBoundaryPoint.CENTER_POINT_STYLE_MID_MID);
    	SEVector3f minPoint = newBP.minPoint;
    	SEVector3f maxPoint = newBP.maxPoint;
    	//maxPoint = maxPoint.add(currentBP.xyzSpan.mul(0.5f));
    	//minPoint = minPoint.subtract(currentBP.xyzSpan.mul(0.5f));
    	if(minPoint.getX() < mWallMinPoint.getX()) {
    		return true;
    	}
    	if(maxPoint.getX() > mWallMaxPoint.getX()) {
    		return true;
    	}
    	if(minPoint.getZ() < mWallMinPoint.getZ()) {
    		return true;
    	}
    	if(maxPoint.getZ() > mWallMaxPoint.getZ()) {
    		return true;
    	}
    	return false;
    }
    private boolean isBoundaryPointOccupied(int wallIndex, 
    		                               int mountPointIndex, 
    		                               SEObjectBoundaryPoint bp,
    		                               boolean compareWallIndex) {
    	List<ConflictObject> objects = this.getExistentObject(wallIndex);
    	for(ConflictObject cb : objects) {
    		if(cb.mConflictObject instanceof NormalObject == false) {
    			continue;
    		}
    		NormalObject normalObject = (NormalObject)cb.mConflictObject;
    		if(this.isAppObject(normalObject)) {
    			if(mountPointIndex == normalObject.getObjectSlot().mMountPointIndex) {
    				return true;
    			}
    		} else {
    			if(this.isMatrixPointOverlap(bp, normalObject.getBoundaryPoint(), compareWallIndex)) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
    /*
    private SETransParas handleAppObjectOutOfWall() {
    	NormalObject currentMoveObject = getOnMoveObject();
    	int currentObjectSlotIndex = currentMoveObject.getObjectSlot().mSlotIndex;
    	int currentWallIndex = mHouse.getWallNearestIndex();
    	SEMountPointChain currentChain = this.getCurrentMountPointChain(currentWallIndex);
    	if(currentObjectSlotIndex == currentWallIndex) {
    		SEDebug.myAssert(currentMoveObject.getBoundaryPoint() != null, "bp must be not null");
    		if(this.isBoundaryPointOccupied(currentWallIndex, 
    				                        currentMoveObject.getObjectSlot().mMountPointIndex,
    				                        currentMoveObject.getBoundaryPoint(), false)) {
    			SEMountPointChain.MatrixPoint mp = this.getEmptyMountPointIndex(currentWallIndex);
    			if(mp != null) {
    				this.setAppObjectBoundaryPoint(currentMoveObject, currentWallIndex, mp.mountPointIndex);
    				int mountPointIndex = mp.mountPointIndex;
    				SEMountPointData mpd = currentChain.getMountPointData(mountPointIndex);
    				return this.setAppObjectToWall(mpd, currentWallIndex, mountPointIndex, 
    						                currentMoveObject, mRealLocationInWall);
    			} else {
    				return null;
    			}
    		} else {
    			currentMoveObject.setBoundaryPoint(mMovedObjectOriginBP);
    			int mountPointIndex = currentChain.getIndex(mMovedObjectOriginBP.minMatrixPoint.row, mMovedObjectOriginBP.minMatrixPoint.col);
    			SEMountPointData mpd = currentChain.getMountPointData(mountPointIndex);
    			return this.setAppObjectToWall(mpd, currentWallIndex, mountPointIndex, currentMoveObject, mRealLocationInWall);
    		}
    	} else {
    		SEMountPointChain.MatrixPoint mp = this.getEmptyMountPointIndex(currentWallIndex);
			if(mp != null) {
				this.setAppObjectBoundaryPoint(currentMoveObject, currentWallIndex, mp.mountPointIndex);
				int mountPointIndex = mp.mountPointIndex;
				SEMountPointData mpd = currentChain.getMountPointData(mountPointIndex);
				return this.setAppObjectToWall(mpd, currentWallIndex, mountPointIndex, 
						                currentMoveObject, mRealLocationInWall);
			} else {
				return null;
			}
    	}
    }
    */
    /*
    private SETransParas handleNotAppObjectOutOfWall() {
    	NormalObject currentMoveObject = getOnMoveObject();
    	SEObjectBoundaryPoint currentBP = currentMoveObject.getBoundaryPoint();
    	int currentObjectSlotIndex = currentMoveObject.getObjectSlot().mSlotIndex;
    	int currentWallIndex = mHouse.getWallNearestIndex();
    	SEVector3f t = getFingerOnWallLocation(100, 100);
        SEVector3f coordInWall = this.toWallSpaceCoordinate(currentWallIndex, t);
        mLocationInWallForWidget = coordInWall;
    	if(currentObjectSlotIndex == currentWallIndex) {
    		SEDebug.myAssert(currentBP != null, "current bp is null");
    		SEDebug.myAssert(this.mMovedObjectOriginBP != null, "save bp is null");
    		if(this.isBoundaryPointOccupied(currentWallIndex, -1, mMovedObjectOriginBP, false)) {
    			SEObjectBoundaryPoint properBP = this.createProperBoundaryPoint(currentWallIndex, currentBP.xyzSpan, currentMoveObject);
    		    if(properBP == null) {
    		    	return null;
    		    } else {
    		    	if(this.isObjectFixedOnYAxis(currentMoveObject)) {
    		    		properBP.center.mD[1] = mLocationInWallForWidget.getY();
    		    	}
    		    	currentMoveObject.setBoundaryPoint(properBP);
    		    	SETransParas dstTransParas = this.createUserTransParasFromWallTransform(currentWallIndex, properBP.center);
        		    return dstTransParas;
    		    }
    		} else {
    			currentMoveObject.setBoundaryPoint(mMovedObjectOriginBP);
    			SETransParas dstTransParas = this.createUserTransParasFromWallTransform(currentWallIndex, mMovedObjectOriginBP.center);
    		    return dstTransParas;
    		}
    	} else {
    		SEObjectBoundaryPoint properBP = this.createProperBoundaryPoint(currentWallIndex, currentBP.xyzSpan, currentMoveObject);
		    if(properBP == null) {
		    	return null;
		    } else {
		    	if(this.isObjectFixedOnYAxis(currentMoveObject)) {
		    		properBP.center.mD[1] = mLocationInWallForWidget.getY();
		    	}
		    	currentMoveObject.setBoundaryPoint(properBP);
		    	SETransParas dstTransParas = this.createUserTransParasFromWallTransform(currentWallIndex, properBP.center);
    		    return dstTransParas;
		    }
    	}
    }
    */
    /*
    private boolean canPlaceOnShelf(String objectName) {
        //TypeManager typeManager = TypeManager.getInstance();
        //return typeManager.canPlaceOnVessel(objectName, ObjectInfo.SLOT_TYPE_WALL_SHELF);
    }
    private boolean canPlaceOnWall(String objectName) {
        TypeManager typeManager = TypeManager.getInstance();
        return typeManager.canPlaceOnVessel(objectName, ObjectInfo.SLOT_TYPE_WALL);
    }
    */
    private SETransParas placeObjectToOriginalPlace(NormalObject currentObject) {
        return null;
    }
    private PlacedObjectsAfterMove handleObjectOutOfWall(NormalObject currentObject, int currentWallIndex) {
        if(currentObject.isShelfObject()) {
            SEMountPointChain chain = getCurrentMountPointChain(currentWallIndex);
            return placeShelfObjectOnProperPlace(chain, currentObject, currentWallIndex);
        } else {
            return placeNativeWidgetObjectInProperPlace(currentObject, currentWallIndex);
        }
    }
    /*
    private SETransParas handleObjectOutOfWall() {
        NormalObject currentObject = getOnMoveObject();
        int currentWallIndex = mHouse.getWallNearestIndex();
        String currentObjectName = currentObject.mName;
        SETransParas destTransParas = null;
        if(canPlaceOnShelf(currentObjectName)) {
            SEMountPointChain.MatrixPoint mp = mMovedObjectOriginBP.maxMatrixPoint;
            int originWallIndex = mMovedObjectOriginBP.wallIndex;
            SEMountPointChain chain = getCurrentMountPointChain(originWallIndex);
            int mountPointIndex = chain.getIndex(mp.row, mp.col);
            destTransParas = placeObjectOnShelf(currentObject, mountPointIndex, originWallIndex);
        } else if(canPlaceOnWall(currentObjectName)) {

        }
        return destTransParas;
    }
    */
    private static class TransParasAnimationFinish implements SEAnimFinishListener {
    	public NormalObject currentObject;
    	public void onAnimationfinish() {
    		if(currentObject instanceof WidgetObject) {
    			((WidgetObject)currentObject).requestUpdateAndroidWidget();
    		}
    	}
    }
    private int getNewShelfIndex() {
        int index = 0;
        for(SEObject obj : mHouse.mChildObjects) {
            if(obj instanceof WallShelf) {
                int objIndex = ((WallShelf) obj).getObjectInfo().mIndex;
                if(objIndex > index) {
                    index = objIndex;
                }
            }
        }
        return index + 1;
    }
    private ModelInfo createFromXml(String path) {
        ModelInfo config = null;
        try {
            InputStream is = SESceneManager.getInstance().getContext().getAssets().open(path);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            XmlUtils.beginDocument(parser, "config");
            config = ModelInfo.CreateFromXml(parser);
            HomeDataBaseHelper help = HomeDataBaseHelper.getInstance(SESceneManager.getInstance()
                    .getContext());
            SQLiteDatabase db = help.getWritableDatabase();
            config.saveToDB(db);
            is.close();
            SESceneManager.getInstance().addModelToScene(config);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        return config;
    }
    private static class ShelfObjectProperty {
        public WallShelf shelf;
        public int mountPointIndex;
        public ShelfObjectProperty(WallShelf s, int index) {
            shelf = s;
            mountPointIndex = index;
        }
        public ShelfObjectProperty() {

        }
    }
    private static ArrayList<ShelfObjectProperty> mShelfObjectProperty = new ArrayList<ShelfObjectProperty>();
    private WallShelf getLeftWallShelf(int leftIndex) {
        for(int i = 0 ; i < mShelfObjectProperty.size() ; i++) {
            ShelfObjectProperty sop = mShelfObjectProperty.get(i);
            if(sop.mountPointIndex == leftIndex) {
                return sop.shelf;
            }
        }
        return null;
    }
    /*
    private WallShelf getWallShelfWithObject(int wallIndex, NormalObject object) {
        for(SEObject child : mHouse.mChildObjects) {
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
    private WallShelf getWallShelfWithMountPoint(int wallIndex, int mountPointIndex) {
        for(SEObject child : mHouse.mChildObjects) {
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
    */
    private SEVector3f getNearestShelfPosition(NormalObject currentObject , int wallIndex, float currentObjectZ) {
        String vesselName = getWallName(wallIndex);
        SEMountPointManager mpm = getScene().getMountPointManager();
        SEMountPointChain chain = mpm.getMountPointChain(currentObject.mName, getVesselObject().mName, vesselName, WallLayer.getShelfContainerName());
        int mountPointCount = chain.getMountPointCount();
        SEMountPointData nearestMPD = null;
        float minDist = 10000000f;
        for(int i = 0 ; i < mountPointCount ; i++) {
            SEMountPointData mpd = chain.getMountPointData(i);
            float distZ = Math.abs(mpd.getTranslate().mD[2] - currentObjectZ);
            if(distZ < minDist) {
                minDist = distZ;
                nearestMPD = mpd;
            }
        }
        SEDebug.myAssert(nearestMPD != null, "nearestMPD must not be null");
        return nearestMPD.getTranslate().clone();
    }
    private WallShelf createWallShelf(int wallIndex) {
    	Context pContext = SESceneManager.getInstance().getContext();
    	String newShelfName ;
    	if(SettingsActivity.getPreferRotation(pContext) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
    		newShelfName = ModelInfo.Type.WALL_SHELF;
    	}else {
    		newShelfName = "jiaziport";
    	}
        ModelInfo modelInfo = getScene().mSceneInfo.findModelInfo(newShelfName);
        if(modelInfo == null) {
            modelInfo = createFromXml("base" + File.separator + newShelfName + File.separator + "models_config.xml");
        }
        ObjectInfo objInfo = new ObjectInfo();
        objInfo.setModelInfo(modelInfo);
        objInfo.mIndex = getNewShelfIndex();
        objInfo.mSceneName = getScene().mSceneName;
        objInfo.mIsNativeObject = true;
        objInfo.mClassName = "com.borqs.se.widget3d.WallShelf";
        objInfo.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
        if(SettingsActivity.getPreferRotation(pContext) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
        	objInfo.mName = "jiazi";
    		objInfo.mVesselName = "home4mian";
    	}else {
    		objInfo.mName = "jiaziport";
    		objInfo.mVesselName = "home8mianshu";
    	}
        objInfo.mObjectSlot.mSlotIndex = wallIndex;
        objInfo.mType = ModelInfo.Type.WALL_SHELF;

        HomeDataBaseHelper help = HomeDataBaseHelper.getInstance(SESceneManager.getInstance()
                .getContext());
        SQLiteDatabase db2 = help.getWritableDatabase();
        objInfo.saveToDB(db2);

        NormalObject shelfObject = HomeUtils.getObjectByClassName(getScene(), objInfo);
        return (WallShelf)shelfObject;
    }
    private int getFartestNeiborMountPointIndex(boolean isLeft, WallShelf shelfObject, int wallIndex, int startRightIndex) {
        SEMountPointChain chain = getCurrentMountPointChain(wallIndex);
        int colNum = chain.getColCount();
        int rowNum = chain.getRowCount();
        int currentRow = startRightIndex / colNum;
        int currentCol = startRightIndex % colNum;
        int endIndex = currentRow * colNum + colNum;
        int startIndex = currentRow * colNum;
        int index = startRightIndex;
        do {
            if(isLeft) {
                index --;
            } else {
                index ++;
            }
            if(index < endIndex && index >= startIndex) {
                boolean b = shelfObject.containMountPointIndex(index);
                if(!b) {
                    break;
                }
            }
        } while(index < endIndex && index >= startIndex);
        if(isLeft) {
            return index + 1;
        } else {
            return index - 1;
        }
    }
    private SEVector3f expandShelf(WallShelf shelfObject, SEMountPointChain chain, float y, float z, boolean isToLeft) {
        int leftMostIndex = shelfObject.getLeftMostMountPointIndex();
        int rightMostIndex = shelfObject.getRightMostMountPointIndex();
        int colNum = chain.getColCount();
        SEMountPointChain.MatrixPoint leftMostMP = chain.getMatrixPointByIndex(leftMostIndex);
        SEMountPointChain.MatrixPoint rightMostMP = chain.getMatrixPointByIndex(rightMostIndex);
        if(isToLeft) {
            leftMostMP.col--;
        } else  {
            rightMostMP.col++;
        }
        if(leftMostMP.col < 0 || rightMostMP.col >= colNum) {
            SEDebug.myAssert(false, "can not expand error");
            return null;
        }
        leftMostIndex = chain.getIndex(leftMostMP.row, leftMostMP.col);
        SEMountPointData leftMostMPD = chain.getMountPointData(leftMostIndex);
        rightMostIndex = chain.getIndex(rightMostMP.row, rightMostMP.col);
        SEMountPointData rightMostMPD = chain.getMountPointData(rightMostIndex);
        float leftX = leftMostMPD.getTranslate().mD[0];
        float rightX = rightMostMPD.getTranslate().mD[0];
        float x = (leftX + rightX) / 2;
        return new SEVector3f(x, y, z);
    }
    private SEVector3f expandShelfToLeft(WallShelf shelfObject, SEMountPointChain chain, float y, float z) {
        return expandShelf(shelfObject, chain, y, z, true);
    }
    private SEVector3f expandShelfToRight(WallShelf shelfObject, SEMountPointChain chain, float y, float z) {
        return expandShelf(shelfObject, chain, y, z, false);
    }
    /*
    private void expandShelf(int currentMountPointIndex, WallShelf shelfObject, int wallIndex, int neiborIndex, float y, float z) {
        SEMountPointChain chain = getCurrentMountPointChain(wallIndex);
        SEMountPointData mpd = chain.getMountPointData(neiborIndex);
        SEVector3f neiborCenter = mpd.getTranslate();
        SEVector3f neiborScale = shelfObject.getUserTransParas().mScale;
        float leftCenterX = neiborCenter.mD[0];
        mpd = chain.getMountPointData(currentMountPointIndex);
        float currX = mpd.getTranslate().mD[0];
        float x = (currX + leftCenterX) / 2;
        SEAfterAddShelf s = new SEAfterAddShelf();
        s.center = new SEVector3f(x, y, z);
        s.wallIndex = wallIndex;
        s.object = shelfObject;
        s.scale = new SEVector3f(neiborScale.mD[0] + 1, 1, 1);
        s.run();
    }
    */
    private SEVector3f mergeShelf(SEMountPointChain chain, WallShelf leftShelf, WallShelf rightShelf, float y, float z) {
        int leftMostIndex = leftShelf.getLeftMostMountPointIndex();
        int rightMostIndex = rightShelf.getRightMostMountPointIndex();
        SEMountPointData leftMPD = chain.getMountPointData(leftMostIndex);
        SEMountPointData rightMPD = chain.getMountPointData(rightMostIndex);
        float leftX = leftMPD.getTranslate().mD[0];
        float rightX = rightMPD.getTranslate().mD[0];
        return new SEVector3f((leftX + rightX) / 2, y, z);
    }
    private void addOrChangeShelf(NormalObject currentObject, SEVector3f xyzSpan, int destMountPointIndex, int wallIndex) {
        //SEObjectBoundaryPoint bp = currentObject.getBoundaryPoint();
        int currentMountPointIndex = destMountPointIndex;
        int leftIndex = currentMountPointIndex - 1;
        int rightIndex = currentMountPointIndex + 1;
        SEMountPointChain chain = getCurrentMountPointChain(wallIndex);
        int rowNum = chain.getRowCount();
        int colNum = chain.getColCount();
        int currentRow = currentMountPointIndex / colNum;
        int currentCol = currentMountPointIndex % colNum;
        int leftCol = currentCol - 1;
        int rightCol = currentCol + 1;
        WallShelf leftShelf = null;
        if(leftCol >= 0 && leftCol < colNum) {
            leftShelf = mHouse.getWallShelfWithMountPoint(wallIndex, leftIndex);
        }

        WallShelf rightShelf = null;
        if(rightCol >= 0 && rightCol < colNum) {
            rightShelf = mHouse.getWallShelfWithMountPoint(wallIndex, rightIndex);
        }
        SEVector3f span = xyzSpan;
        SEMountPointData destMPD = chain.getMountPointData(destMountPointIndex);
        float z = null == destMPD ? 0 : destMPD.getTranslate().mD[2];
        float x = null == destMPD ? 0 : destMPD.getTranslate().mD[0];
        float y = 0;
        if(leftShelf == null && rightShelf == null) {
            SEVector3f shelfCenter = getNearestShelfPosition(currentObject, wallIndex, z);
            WallShelf shelfObject = createWallShelf(wallIndex);
            getVesselObject().addChild(shelfObject, false);
            SEAfterAddShelf s = new SEAfterAddShelf();
            s.center = new SEVector3f(x, y, shelfCenter.mD[2]);
            s.wallIndex = wallIndex;
            s.object = shelfObject;
            shelfObject.setIsCreateByAddShelfObject(true);
            shelfObject.load(getVesselObject(), s);
            shelfObject.addObjectOnShelf(currentObject, currentMountPointIndex);
        } else if(leftShelf != null && rightShelf == null) {
            SEVector3f shelfCenter = getNearestShelfPosition(currentObject, wallIndex, z);
            SEVector3f realShelfCenter = expandShelfToRight(leftShelf, chain, y, shelfCenter.mD[2]);
            leftShelf.addObjectOnShelf(currentObject, currentMountPointIndex);
            SEVector3f scale = new SEVector3f(leftShelf.getObjectNumOnShelf(), 1, 1);
            setShelfObjectUserTransParas(leftShelf, realShelfCenter, wallIndex, scale);

        } else if(leftShelf == null && rightShelf != null) {
            int rightMostIndex = getFartestNeiborMountPointIndex(false, rightShelf, wallIndex, rightIndex);
            SEVector3f shelfCenter = getNearestShelfPosition(currentObject, wallIndex, z);
            SEVector3f realShelfCenter = expandShelfToLeft(rightShelf, chain, y, shelfCenter.mD[2]);
            rightShelf.addObjectOnShelf(currentObject, currentMountPointIndex);
            SEVector3f scale = new SEVector3f(rightShelf.getObjectNumOnShelf(), 1, 1);
            setShelfObjectUserTransParas(rightShelf, realShelfCenter, wallIndex, scale);

        } else {
            SEVector3f shelfCenter = getNearestShelfPosition(currentObject, wallIndex, z);
            SEVector3f realShelfCenter = mergeShelf(chain, leftShelf, rightShelf, y, shelfCenter.mD[2]);
            leftShelf.addObjectsFromShelf(rightShelf);
            rightShelf.getParent().removeChild(rightShelf, true);
            leftShelf.addObjectOnShelf(currentObject, destMountPointIndex);
            SEVector3f scale = new SEVector3f(leftShelf.getObjectNumOnShelf(), 1, 1);
            setShelfObjectUserTransParas(leftShelf, realShelfCenter, wallIndex, scale);

        }
    }
    /*
    private void createShelf(NormalObject currentObject, int wallIndex) {
        SEObjectBoundaryPoint bp = currentObject.getBoundaryPoint();
        int currentMountPointIndex = currentObject.getObjectSlot().mMountPointIndex;
        int leftIndex = currentMountPointIndex - 1;
        WallShelf leftShelf = getLeftWallShelf(leftIndex);
        SEVector3f center = bp.center.clone();
        SEVector3f span = bp.xyzSpan;
        float z = center.mD[2] + span.mD[2] / 2;
        float x = center.mD[0];
        float y = 0;
        if(leftShelf != null) {
            SERotate rotate = new SERotate();
            leftShelf.getLocalRotate_JNI(rotate.mD);
            SEVector3f scale = new SEVector3f();
            leftShelf.getLocalScale_JNI(scale.mD);
            SEVector3f translate = new SEVector3f();
            leftShelf.getLocalTranslate_JNI(translate.mD);
            leftShelf.createLocalBoundingVolume();
            SEVector3f minPoint = new SEVector3f();
            SEVector3f maxPoint = new SEVector3f();
            leftShelf.getLocalBoundingVolume(minPoint, maxPoint);
            SEVector3f xyzSpan = maxPoint.subtract(minPoint);
            SEMountPointChain chain = getCurrentMountPointChain(wallIndex);
            SEMountPointData mpd = chain.getMountPointData(leftIndex);
            SEVector3f leftCenter = mpd.getTranslate().clone();
            x = (leftCenter.mD[0] + x) / 2;
            SEAfterAddShelf s = new SEAfterAddShelf();
            s.center = new SEVector3f(x, y, z);
            s.wallIndex = wallIndex;
            s.object = leftShelf;
            s.scale = new SEVector3f(2, 1, 1);
            s.run();
            return;
        }

        String vesselName = getWallName(wallIndex);
        SEMountPointManager mpm = getScene().getMountPointManager();
        SEMountPointChain chain = mpm.getMountPointChain(currentObject.mName, getVesselObject().mName, vesselName, WallLayer.getShelfContainerName());
        int mountPointCount = chain.getMountPointCount();
        SEMountPointData nearestMPD = null;
        float minDist = 10000000f;
        for(int i = 0 ; i < mountPointCount ; i++) {
            SEMountPointData mpd = chain.getMountPointData(i);
            float distZ = Math.abs(mpd.getTranslate().mD[2] - z);
            if(distZ < minDist) {
                minDist = distZ;
                nearestMPD = mpd;
            }
        }
        SEDebug.myAssert(nearestMPD != null, "nearestMPD must not be null");
        z = nearestMPD.getTranslate().mD[2];
        SEVector3f shelfCenter = new SEVector3f(x, y, z);
        String newShelfName = ModelInfo.Type.WALL_SHELF + "1";
        ModelInfo modelInfo = getScene().mSceneInfo.findModelInfo(newShelfName);
        if(modelInfo == null) {
            modelInfo = createFromXml("base" + File.separator + newShelfName + File.separator + "models_config.xml");
        }
        ObjectInfo objInfo = new ObjectInfo();
        objInfo.setModelInfo(modelInfo);
        objInfo.mIndex = getCurrentShelfCount();
        objInfo.mSceneName = getScene().mSceneName;
        objInfo.mIsNativeObject = true;
        objInfo.mClassName = "com.borqs.se.widget3d.WallShelf";
        objInfo.mSlotType = ObjectInfo.SLOT_TYPE_WALL;
        objInfo.mName = "jiazi1";
        objInfo.mVesselName = "home4mian";

        HomeDataBaseHelper help = HomeDataBaseHelper.getInstance(SESceneManager.getInstance()
                .getContext());
        SQLiteDatabase db2 = help.getWritableDatabase();
        objInfo.saveToDB(db2);

        NormalObject shelfObject = HomeUtils.getObjectByClassName(getScene(), objInfo);
        getVesselObject().addChild(shelfObject, false);
        SEAfterAddShelf s = new SEAfterAddShelf();
        s.center = shelfCenter;
        s.wallIndex = wallIndex;
        s.object = shelfObject;
        shelfObject.load(getVesselObject(), s);
        mShelfObjectProperty.add(new ShelfObjectProperty((WallShelf)shelfObject, currentMountPointIndex));
    }
    */
    private void setShelfObjectUserTransParas(NormalObject object, SEVector3f center, int wallIndex, SEVector3f scale) {
        SETransParas transParas = WallLayer.this.createUserTransParasFromWallTransform(wallIndex, center);
        object.getUserTransParas().mTranslate = transParas.mTranslate;
        if(scale != null) {
            SEVector3f newScale = scale.mul(transParas.mScale);
            object.getUserTransParas().mScale = newScale;
        } else {
            object.getUserTransParas().mScale = transParas.mScale;
        }
        object.getUserTransParas().mRotate = transParas.mRotate;
        object.setUserTransParas();

        SEObjectBoundaryPoint newBP = new SEObjectBoundaryPoint(wallIndex);
        newBP.center = center;
        SEVector3f xyzSpan = object.getObjectOriginXYZSpan();
        if(scale != null) {
            xyzSpan.mD[0] *= scale.mD[0];
        }
        SEObjectBoundaryPoint tmpBP = SEObjectBoundaryPoint.getMinMaxPointInPlane(center, xyzSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ, SEObjectBoundaryPoint.CENTER_POINT_STYLE_BOTTOM_MID);
        newBP.minPoint = tmpBP.minPoint;
        newBP.maxPoint = tmpBP.maxPoint;
        SEMountPointChain chain = getCurrentMountPointChain(wallIndex);
        MatrixPoint minMatrixPoint =  chain.getMatrixPointInPlaneXZ(tmpBP.minPoint);
        MatrixPoint maxMatrixPoint = chain.getMatrixPointInPlaneXZ(tmpBP.maxPoint);
        newBP.minMatrixPoint = minMatrixPoint;
        newBP.maxMatrixPoint = maxMatrixPoint;
        newBP.bpSize = calculateObjectMatrixPointSize(chain, xyzSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ);
        newBP.xyzSpan = xyzSpan;
        object.setBoundaryPoint(newBP);
        if(getScene().isShelfVisible() ) {
            object.setVisible(true, true);
        } else {
            object.setVisible(false, true);
        }
    }
    private class SEAfterAddShelf implements Runnable {
        SEVector3f center;
        int wallIndex;
        NormalObject object;
        SEVector3f scale;
        public void run() {
            setShelfObjectUserTransParas(object, center, wallIndex, scale);
            ((WallShelf)object).createRowIndexInWall(getWallName(wallIndex));
            ((WallShelf)object).setIsCreateByAddShelfObject(false);
        }
    }
    private SEObjectBoundaryPoint getMountPointMinMaxPoint(SEMountPointChain chain, int mountPointIndex) {
        SEMountPointData mpd = chain.getMountPointData(mountPointIndex);
        SEVector3f cellSize = mpd.mMaxPoint.subtract(mpd.mMinPoint);
        SEObjectBoundaryPoint minMaxPointBP = SEObjectBoundaryPoint.getMinMaxPointInPlane(mpd.getTranslate(), cellSize, SEObjectBoundaryPoint.MOVE_PLANE_XZ, SEObjectBoundaryPoint.CENTER_POINT_STYLE_BOTTOM_MID);
        return minMaxPointBP;
    }
    //this function just return BP with minMatrixPoint, maxMatrixPoint, minPoint, maxPoint, it will not calulate
    // center and xyzspan
    private SEObjectBoundaryPoint createBoundaryPointFromMatrixPoint(SEMountPointChain chain, int wallIndex, MatrixPoint mp) {
        SEObjectBoundaryPoint bp = new SEObjectBoundaryPoint(wallIndex);
        bp.minMatrixPoint = mp;
        bp.maxMatrixPoint = mp.clone();
        int mountPointIndex = chain.getIndex(mp.row, mp.col);
        SEObjectBoundaryPoint minMaxPointBP = getMountPointMinMaxPoint(chain, mountPointIndex);
        bp.minPoint = minMaxPointBP.minPoint;
        bp.maxPoint = minMaxPointBP.maxPoint;
        return bp;
    }
    private boolean canExpandToLeft(SEMountPointChain chain, int wallIndex, SEMountPointChain.MatrixPoint mp) {
        if( mp.col <= 0) {
            return false;
        }
        int leftCol = mp.col - 1;
        SEMountPointChain.MatrixPoint newMP = new SEMountPointChain.MatrixPoint();
        newMP.row = mp.row;
        newMP.col = leftCol;
        SEObjectBoundaryPoint bp = createBoundaryPointFromMatrixPoint(chain, wallIndex, newMP);
        /*
        new SEObjectBoundaryPoint(wallIndex);
        bp.minMatrixPoint = newMP;
        bp.maxMatrixPoint = newMP.clone();
        int mountPointIndex = chain.getIndex(newMP.row, newMP.col);
        SEObjectBoundaryPoint minMaxPointBP = getMountPointMinMaxPoint(chain, mountPointIndex);
        bp.minPoint = minMaxPointBP.minPoint;
        bp.maxPoint = minMaxPointBP.maxPoint;
        */
        boolean b = isBoundaryPointOverlapObjectsInWall(chain, wallIndex, bp, false);
        if(b) {
            return false;
        } else {
            return true;
        }
    }
    private boolean canExpandToRight(SEMountPointChain chain, int wallIndex, SEMountPointChain.MatrixPoint mp) {
        //return mp.col < (chain.getColCount() - 1);
        if(mp.col >= (chain.getColCount() - 1)) {
            return false;
        }
        int rightCol = mp.col + 1;
        SEMountPointChain.MatrixPoint newMP = new SEMountPointChain.MatrixPoint();
        newMP.row = mp.row;
        newMP.col = rightCol;
        SEObjectBoundaryPoint bp = new SEObjectBoundaryPoint(wallIndex);
        bp.minMatrixPoint = newMP;
        bp.maxMatrixPoint = newMP.clone();
        int mountPointIndex = chain.getIndex(newMP.row, newMP.col);
        SEObjectBoundaryPoint minMaxPointBP = getMountPointMinMaxPoint(chain, mountPointIndex);
        bp.minPoint = minMaxPointBP.minPoint;
        bp.maxPoint = minMaxPointBP.maxPoint;
        boolean b = isBoundaryPointOverlapObjectsInWall(chain, wallIndex, bp, false);
        if(b) {
            return false;
        } else {
            return true;
        }
    }
    private void testAssertObjectMountPointOnShelf() {
        for(SEObject object : mHouse.mChildObjects) {
            if(!(object instanceof  WallShelf)) {
                continue;
            }
            WallShelf wallShelf = (WallShelf)object;
            wallShelf.checkAllObjectMountPoint();
        }
    }
    private void testAssertObjectOnShelf(NormalObject currentObject) {
        boolean found = false;
        for(SEObject object : mHouse.mChildObjects) {
            if(!(object instanceof WallShelf)){
                continue;
            }
            WallShelf wallShelf = (WallShelf)object;
            int num = wallShelf.checkObjectNumOnShelf(currentObject);
            if(num != 1 && num != 0) {
                SEDebug.myAssert(false, "one object two occurence on shelf: num = " + num);
            }
            wallShelf.checkAllObjectOnShelf();
            boolean b = wallShelf.hasObject(currentObject);
            if(found == true && b == true) {
                SEDebug.myAssert(false, "one object on two shelf error");
            } else {
                if(b) {
                    found = b;
                }
            }
        }
    }
    public static float calculateAppObjectZPosition(float bottomz, SEVector3f xyzSpan, NormalObject object) {
        float z = xyzSpan.mD[2];
        float deltaz = 0;
        if(object instanceof  AppObject) {
            AppObject appObject = (AppObject)object;
            float bitmapHeight = appObject.getBitmapHeight();
            float bitmapContentHeight = appObject.getBitmapContentHeight();
            if (bitmapHeight > bitmapContentHeight) {
                deltaz = z * ((bitmapHeight - bitmapContentHeight) / 2) / bitmapHeight;
            } else {
                deltaz = 0;
            }
        }
        float ret = bottomz + z / 2 - deltaz;
        return ret;
    }
    private class ObjectMountPointHandler implements WallShelf.ObjectHandler {
        SEMountPointChain chain;
        int wallIndex;
        public void run(int mountPointIndex, NormalObject object) {
            if(object != null) {
                SEMountPointData mpd = chain.getMountPointData(mountPointIndex);
                SEVector3f xyzSpan = object.getObjectXYZSpan();
                SEMountPointData d = new SEMountPointData("dd", mpd.getTranslate().clone(), mpd.getScale().clone(), mpd.getRotate().clone());
                if(object instanceof  AppObject) {
                    d.getTranslate().mD[2] = calculateAppObjectZPosition(d.getTranslate().mD[2], xyzSpan, object);
                }
                SEVector3f locationInWall = getLocationYInWall(object, wallIndex);
                SETransParas t = WallLayer.this.setAppObjectToWall(d, wallIndex, mountPointIndex, object, locationInWall);
                object.getUserTransParas().mTranslate = t.mTranslate;
                object.getUserTransParas().mScale = t.mScale;
                object.getUserTransParas().mRotate = t.mRotate;
                object.setUserTransParas();
            }
        }
    }
    private void moveObjectOnEmptyMountPoint(NormalObject currentObject, int destMountPointIndex,
                                             WallShelf destShelf, WallShelf currentObjectShelf) {
        int objectMountPointIndex = currentObject.getObjectSlot().mMountPointIndex;
        SEDebug.myAssert(objectMountPointIndex != destMountPointIndex, "objectMountPointIndex != destMountPointIndex error");
        destShelf.addObjectOnShelf(currentObject, destMountPointIndex);
        if(currentObjectShelf != null ){
            int mpIndex = currentObject.getObjectSlot().mMountPointIndex;
            mHouse.removeObjectFromCurrentShelf(currentObject, mpIndex, currentObjectShelf);
        }
    }
    private boolean moveObjectOnSameShelf(NormalObject currentObject, WallShelf wallShelf,
                                       SEMountPointChain chain , int wallIndex,
                                       int destMountPointIndex) {
        int objectMountPointIndex = currentObject.getObjectSlot().mMountPointIndex;
        if(objectMountPointIndex == destMountPointIndex) {
            return true;
        }
        ObjectMountPointHandler h = new ObjectMountPointHandler();
        h.wallIndex = wallIndex;
        h.chain = chain;
        if(objectMountPointIndex < destMountPointIndex) {
            NormalObject objectOnDestMountPointIndex = wallShelf.getObjectOnMountPointIndex(destMountPointIndex);
            if(objectOnDestMountPointIndex != null) {
                //after invokde moveObjectMountPointToLeft currentObject on objectMountPointIndex will be null
                wallShelf.moveObjectMountPointToLeft(destMountPointIndex, objectMountPointIndex, h);
                //wallShelf.removeObjectOnShelf(currentObject);
                wallShelf.addObjectOnShelf(currentObject, destMountPointIndex);
            } else {
                moveObjectOnEmptyMountPoint(currentObject, destMountPointIndex, wallShelf, wallShelf);
            }
        } else {
            NormalObject objectOnDestMountPointIndex = wallShelf.getObjectOnMountPointIndex(destMountPointIndex);
            if(objectOnDestMountPointIndex != null ) {
                wallShelf.moveObjectMountPointToRight(destMountPointIndex, objectMountPointIndex, h);
                //wallShelf.removeObjectOnShelf(currentObject);
                wallShelf.addObjectOnShelf(currentObject, destMountPointIndex);
            } else {
                moveObjectOnEmptyMountPoint(currentObject, destMountPointIndex, wallShelf, wallShelf);
            }
        }
        //currentObject.getObjectSlot().mMountPointIndex = destMountPointIndex;
        return true;
    }
    private boolean moveObjectOnDiffShelf(NormalObject currentObject, WallShelf destShelf, WallShelf currentObjectShelf,
                                       SEMountPointChain chain, int wallIndex,
                                       int destMountPointIndex) {
        SEMountPointData destMPD = chain.getMountPointData(destMountPointIndex);
        NormalObject objectOnDestMountPointIndex = destShelf.getObjectOnMountPointIndex(destMountPointIndex);
        boolean moveFinished = true;

        if(objectOnDestMountPointIndex == null) {
            // just make object on this mount point
            int objectMountPointIndex = currentObject.getObjectSlot().mMountPointIndex;
            SEDebug.myAssert(objectMountPointIndex != destMountPointIndex, "objectMountPointIndex != destMountPointIndex error");
            destShelf.addObjectOnShelf(currentObject, destMountPointIndex);
            if(currentObjectShelf != null ){
                int mpIndex = currentObject.getObjectSlot().mMountPointIndex;
                mHouse.removeObjectFromCurrentShelf(currentObject, mpIndex, currentObjectShelf);
            }
        } else {
            boolean isShelfNeedExpand = destShelf.isNeedExpand();
            ObjectMountPointHandler h = new ObjectMountPointHandler();
            h.wallIndex = wallIndex;
            h.chain = chain;
            if(isShelfNeedExpand) {
                int leftMostMountPointIndex = destShelf.getLeftMostMountPointIndex();
                int rightMostMountPointIndex = destShelf.getRightMostMountPointIndex();
                SEMountPointChain.MatrixPoint leftMP = chain.getMatrixPointByIndex(leftMostMountPointIndex);
                SEMountPointChain.MatrixPoint rightMP = chain.getMatrixPointByIndex(rightMostMountPointIndex);
                SEDebug.myAssert(leftMP.row == rightMP.row, "not row equal error");
                int colNum = chain.getColCount();
                int endIndex = colNum * (leftMP.row + 1) - 1;
                if(canExpandToLeft(chain, wallIndex, leftMP)) {
                    SEVector3f shelfCenter = getNearestShelfPosition(currentObject,wallIndex, destMPD.getTranslate().mD[2]);
                    SEVector3f realShelfCenter = expandShelfToLeft(destShelf, chain, 0, shelfCenter.mD[2]);
                    SEVector3f scale = new SEVector3f(destShelf.getObjectNumOnShelf() + 1, 1, 1);
                    setShelfObjectUserTransParas(destShelf, realShelfCenter, wallIndex, scale);
                    destShelf.addObjectOnShelf(null, leftMostMountPointIndex - 1);
                    destShelf.moveObjectMountPointToLeft(destMountPointIndex, leftMostMountPointIndex - 1, h);
                    destShelf.addObjectOnShelf(currentObject, destMountPointIndex);
                    if(currentObjectShelf != null ){
                        int mpIndex = currentObject.getObjectSlot().mMountPointIndex;
                        mHouse.removeObjectFromCurrentShelf(currentObject, mpIndex, currentObjectShelf);
                    }
                } else if(canExpandToRight(chain, wallIndex, rightMP)) {
                    SEVector3f shelfCenter = getNearestShelfPosition(currentObject,wallIndex, destMPD.getTranslate().mD[2]);
                    SEVector3f realShelfCenter = expandShelfToRight(destShelf, chain, 0, shelfCenter.mD[2]);
                    SEVector3f scale = new SEVector3f(destShelf.getObjectNumOnShelf() + 1, 1, 1);
                    setShelfObjectUserTransParas(destShelf, realShelfCenter, wallIndex, scale);
                    destShelf.addObjectOnShelf(null, rightMostMountPointIndex + 1);
                    destShelf.moveObjectMountPointToRight(destMountPointIndex, rightMostMountPointIndex + 1, h);
                    destShelf.addObjectOnShelf(currentObject, destMountPointIndex);
                    if(currentObjectShelf != null ){
                        int mpIndex = currentObject.getObjectSlot().mMountPointIndex;
                        mHouse.removeObjectFromCurrentShelf(currentObject,mpIndex, currentObjectShelf);
                    }
                } else {
                    // can not expand , you must make the current object to its original place
                    moveFinished = false;
                }
            } else {

                boolean moveOk = destShelf.moveObjectMountPointToLeftFirstNullPlace(destMountPointIndex, h);
                if(moveOk == false) {
                    destShelf.moveObjectMountPointToRightFirstNullPlace(destMountPointIndex, h);
                }
                destShelf.addObjectOnShelf(currentObject, destMountPointIndex);
                if(currentObjectShelf != null ){
                    int mpIndex = currentObject.getObjectSlot().mMountPointIndex;
                    mHouse.removeObjectFromCurrentShelf(currentObject, mpIndex, currentObjectShelf);
                }

            }
        }
        return moveFinished;
    }
    private int getEmptyMountPointIndex(SEMountPointChain chain, int wallIndex) {
        /*
        ArrayList<WallShelf> shelfList = new ArrayList<WallShelf>();
        for(SEObject object : mHouse.mChildObjects) {
            if(!(object instanceof WallShelf)) {
                continue;
            }
            WallShelf shelf = (WallShelf) object;
            if(shelf.getObjectSlot().mSlotIndex != wallIndex) {
                continue;
            }
            shelfList.add(shelf);
        }
        */
        int colNum = chain.getColCount();
        int rowNum = chain.getRowCount();
        for(int i = 0 ; i < rowNum ; i++) {
            for(int j = 0 ; j < colNum ; j++) {
                int index = chain.getIndex(i, j);
                MatrixPoint mp = new MatrixPoint();
                mp.row = i;
                mp.col = j;
                SEObjectBoundaryPoint newBP = createBoundaryPointFromMatrixPoint(chain, wallIndex, mp);
                boolean b = isBoundaryPointOverlapObjectsInWall(chain, wallIndex, newBP, false);
                /*
                for(WallShelf shelf : shelfList) {
                    b = shelf.hasObjectWithMountPointIndex(index);
                    if(b) {
                        break;
                    }
                }
                */
                if(!b) {
                    return index;
                }
            }
        }
        return -1;
    }

    private ArrayList<NormalObject> getNativeWidgetIntersectObjects(NormalObject currentObject, int wallIndex) {
        SEDebug.myAssert(mRealLocationInWall != null, "mRealLocationInWall must not be null");
        ArrayList<NormalObject> intersectObjects = new ArrayList<NormalObject>();
        ArrayList<NormalObject> currentWallNormalObjects = getNormalObjectsOnWall(wallIndex);
        for(NormalObject normalObject : currentWallNormalObjects) {
            SEObjectBoundaryPoint normalObjectBP = normalObject.getBoundaryPoint();
            if(normalObjectBP == null) {
                Log.i(TAG, "### object bp is null : " + normalObject.mName + " ####");
                continue;
            }
            SEVector3f xyzSpan = currentObject.getObjectXYZSpan();
            SEVector3f currentCenter = mRealLocationInWall.clone();
            SEObjectBoundaryPoint currBP = SEObjectBoundaryPoint.getMinMaxPointInPlane(currentCenter, xyzSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ, SEObjectBoundaryPoint.CENTER_POINT_STYLE_MID_MID);
            boolean b = isRectIntersect(currBP.minPoint, currBP.maxPoint, normalObjectBP.minPoint, normalObjectBP.maxPoint);
            if(b) {
                intersectObjects.add(normalObject);
            }
        }
        return intersectObjects;
    }
    private SEVector3f createObjectCenterFromMatrixPoint(SEMountPointChain chain, MatrixPoint minMatrixPoint, MatrixPoint maxMatrixPoint) {
        int minMountPointIndex = chain.getIndex(minMatrixPoint.row, minMatrixPoint.col);
        int maxMountPointIndex = chain.getIndex(maxMatrixPoint.row, maxMatrixPoint.col);
        SEMountPointData minMPD = chain.getMountPointData(minMountPointIndex);
        SEMountPointData maxMPD = chain.getMountPointData(maxMountPointIndex);
        SEVector3f center = minMPD.getTranslate().add(maxMPD.getTranslate()).mul(0.5f);
        return center;
    }
    // object must be child on wall
    private SEObjectBoundaryPoint createNewBoundaryPointForObject(int wallIndex, NormalObject object, ArrayList<SEObjectBoundaryPoint> bpList) {
        SEMountPointChain chain = getCurrentMountPointChain(wallIndex);
        SEVector3f xyzSpan = object.getObjectXYZSpan();
        SEObjectBoundaryPoint objectBP = object.getBoundaryPoint();
        SEMountPointChain.MatrixPoint objectBPSize = null;
        if(objectBP != null && objectBP.bpSize != null) {
            objectBPSize = objectBP.bpSize;
        } else {
            objectBPSize = calculateObjectMatrixPointSize(chain, xyzSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ);
            if(objectBP != null) {
                objectBP.bpSize = objectBPSize;
            }
        }
        int rowNum = chain.getRowCount();
        int colNum = chain.getColCount();
        for(int i = (objectBPSize.row - 1) ; i < rowNum;  i++) {
            for(int j = 0 ; j < colNum ; j++) {
                int startRow = i;
                int startCol = j;
                int endRow = i - (objectBPSize.row - 1);
                int endCol = j + (objectBPSize.col - 1);
                if(startRow >= 0 && startRow < rowNum && startCol >= 0 && startCol < colNum &&
                        endRow >= 0 && endRow < rowNum && endCol >= 0 && endCol < colNum) {
                    SEObjectBoundaryPoint bp = new SEObjectBoundaryPoint(wallIndex);
                    bp.minMatrixPoint = new SEMountPointChain.MatrixPoint(startRow, startCol, -1);
                    bp.maxMatrixPoint = new SEMountPointChain.MatrixPoint(endRow, endCol, -1);
                    bp.xyzSpan = xyzSpan;
                    SEVector3f center = createObjectCenterFromMatrixPoint(chain, bp.minMatrixPoint, bp.maxMatrixPoint);
                    bp.center = center;
                    SEObjectBoundaryPoint tmpBP = SEObjectBoundaryPoint.getMinMaxPointInPlane(center, xyzSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ, SEObjectBoundaryPoint.CENTER_POINT_STYLE_MID_MID);
                    bp.minPoint = tmpBP.minPoint;
                    bp.maxPoint = tmpBP.maxPoint;
                    bp.bpSize = objectBPSize;
                    boolean b = isBoundaryPointOverlapObjectsInWall(chain, wallIndex, bp, true);
                    if(b == false) {
                        if(bpList != null) {
                            for(SEObjectBoundaryPoint bpInList : bpList) {
                                if(isRectIntersect(bp.minPoint, bp.maxPoint, bpInList.minPoint, bpInList.maxPoint)) {
                                    b = true;
                                    break;
                                }
                            }
                        }
                        if(b == false) {
                            return bp;
                        }
                    }
                }
            }
        }
        return null;
    }
    //currentObject is native object or widget object
    private ArrayList<SEObjectBoundaryPoint> getIntersectObjectsNewPlace(ArrayList<NormalObject> intersectObjects, int currentWallIndex) {
        ArrayList<SEObjectBoundaryPoint> objectNewPlaceList = new ArrayList<SEObjectBoundaryPoint>();
        for(NormalObject intersectObject : intersectObjects) {
            if(intersectObject instanceof WallShelf) {
                break;
            }
            if(intersectObject instanceof  AppObject) {
                break;
            }
            SEObjectBoundaryPoint newBP = createNewBoundaryPointForObject(currentWallIndex, intersectObject, objectNewPlaceList);
            if(newBP == null) {
                break;
            }
            /*
            boolean newBPOverlap = false;
            for(SEObjectBoundaryPoint bp : objectNewPlaceList) {
                if(isMatrixPointOverlap(bp, newBP, true)) {
                    newBPOverlap = true;
                    break;
                }
            }
            if(newBPOverlap) {
                break;
            }
            */
            objectNewPlaceList.add(newBP);
        }
        return objectNewPlaceList;
    }

    private PlacedObjectsAfterMove placeNativeWidgetObjectInEmptyPlaceOnCurrentWall(NormalObject currentObject, int currentWallIndex) {
        SEObjectBoundaryPoint newBP = createNewBoundaryPointForObject(currentWallIndex, currentObject, null);
        if(newBP != null) {
            currentObject.setBoundaryPoint(newBP);
            PlacedObjectsAfterMove pam = new PlacedObjectsAfterMove();
            pam.currentMovedObject = currentObject;
            return pam;
        } else {
            return null;
        }
    }
    private static class PlacedObjectsAfterMove {
        public NormalObject currentMovedObject;
        public ArrayList<NormalObject> objectsNeedMove = new ArrayList<NormalObject>();
    }
    private PlacedObjectsAfterMove placeAppObjectOnCurrentWall(NormalObject currentObject, int currentWallIndex, MountPointInfo mpi) {
        ArrayList<NormalObject> intersectObjects = getAppObjectIntersectObjects(currentObject, currentWallIndex, mpi);
        SEMountPointChain chain = getCurrentMountPointChain(currentWallIndex);
        if(mpi == null) {
            return placeShelfObjectOnProperPlace(chain, currentObject, currentWallIndex);
        } else {
            return placeShelfObjectOnWallWithFingerPoint(chain, currentObject, currentWallIndex, mpi);
        }
    }
    private ArrayList<NormalObject> getNormalObjectsOnWall(int wallIndex)  {
        ArrayList<NormalObject> objects = new ArrayList<NormalObject>();
        for(SEObject obj : mHouse.mChildObjects) {
            if(!(obj instanceof NormalObject)) {
                continue;
            }
            NormalObject normalObject = (NormalObject) obj;
            if(normalObject.getObjectSlot().mSlotIndex == wallIndex) {
                objects.add(normalObject);
            }
        }
        return objects;
    }

    private ArrayList<SEObjectBoundaryPoint> getAppObjectIntersectObjectsNewPlace(NormalObject currentObject, ArrayList<NormalObject> objects, int wallIndex, MountPointInfo mpi) {
        ArrayList<SEObjectBoundaryPoint> bpList = new ArrayList<SEObjectBoundaryPoint>();
        for(NormalObject object : objects) {
            if(object instanceof WallShelf) {
                continue;
            }
            if(object.isShelfObject()) {
                WallShelf shelf = mHouse.getWallShelfWithMountPoint(wallIndex, mpi.mountPointInfo.mIndex);
                int currentObjectMP = currentObject.getObjectSlot().mMountPointIndex;
                WallShelf currentShelf = mHouse.getWallShelfWithMountPoint(wallIndex, currentObjectMP);
                boolean canExpand = false;
                SEMountPointChain chain = getCurrentMountPointChain(wallIndex);
                if(shelf == null) {
                    canExpand = true;
                } else {
                    boolean shelfNeedExpand = shelf.isNeedExpand();
                    if(shelfNeedExpand == false || shelf == currentShelf) {
                        canExpand = true;
                    } else {
                        int leftMostIndex = shelf.getLeftMostMountPointIndex();
                        int rightMostIndex = shelf.getRightMostMountPointIndex();
                        SEMountPointChain.MatrixPoint mp = chain.getMatrixPointByIndex(leftMostIndex);
                        canExpand = canExpandToLeft(chain, wallIndex, mp);
                        if(!canExpand) {
                            mp = chain.getMatrixPointByIndex(rightMostIndex);
                            canExpand = canExpandToRight(chain, wallIndex, mp);
                        }
                    }
                }
                if(canExpand) {
                    SEObjectBoundaryPoint bp = createBoundaryPointFromMountPointIndex(object, chain, mpi.mountPointInfo.mIndex, wallIndex);
                    if (null == bp) {
                        object.getParent().removeChild(object, true);
                    } else {
                        bpList.add(bp);
                    }
                } else {
                    break;
                }
            } else {
                SEObjectBoundaryPoint bp = createNewBoundaryPointForObject(wallIndex, object, null);
                if(bp == null) {
                    break;
                }
                bpList.add(bp);
            }
        }
        return bpList;
    }
    private ArrayList<NormalObject> getAppObjectIntersectObjects(NormalObject currentObject, int wallIndex, MountPointInfo mpi) {
        ArrayList<NormalObject> normalObjects = getNormalObjectsOnWall(wallIndex);
        ArrayList<NormalObject> retObjects = new ArrayList<NormalObject>();
        for(NormalObject object : normalObjects) {
            if(object instanceof  WallShelf) {
                continue;
            }
            boolean b = isMountPointInObject(object, mpi.wallIndex, mpi.mountPointInfo.mIndex);
            if(b) {
                retObjects.add(object);
            }
        }
        return retObjects;
    }
    private SEObjectBoundaryPoint createBoundaryPointFromObjectCenter(NormalObject currentObject, SEVector3f center, int wallIndex, int centerPlace) {
        SEVector3f xyzSpan = currentObject.getObjectXYZSpan();
        SEObjectBoundaryPoint newObjectBP = SEObjectBoundaryPoint.getMinMaxPointInPlane(center, xyzSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ, centerPlace);
        SEMountPointChain currentChain = this.getCurrentMountPointChain(wallIndex);
        SEMountPointChain.MatrixPoint minMatrixPoint = currentChain.getMatrixPointInPlaneXZ(newObjectBP.minPoint);
        SEMountPointChain.MatrixPoint maxMatrixPoint = currentChain.getMatrixPointInPlaneXZ(newObjectBP.maxPoint);
        newObjectBP.minMatrixPoint = minMatrixPoint;
        newObjectBP.maxMatrixPoint = maxMatrixPoint;
        newObjectBP.xyzSpan = xyzSpan;
        newObjectBP.movePlane = SEObjectBoundaryPoint.MOVE_PLANE_XZ;
        newObjectBP.center = center;
        newObjectBP.wallIndex = wallIndex;
        newObjectBP.bpSize = calculateObjectMatrixPointSize(currentChain, xyzSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ);
        return newObjectBP;
    }
    //currentObject must be native or widget object
    private PlacedObjectsAfterMove placeNativeWidgetObjectOnCurrentWallWithFinger(NormalObject currentObject, int currentWallIndex, MountPointInfo mountPointInfo) {
        ArrayList<NormalObject> intersectObjects = getNativeWidgetIntersectObjects(currentObject, currentWallIndex);
        ArrayList<SEObjectBoundaryPoint> intersectObjectNewBP = getIntersectObjectsNewPlace(intersectObjects, currentWallIndex);
        if(intersectObjects.size() != intersectObjectNewBP.size()) {
            SEObjectBoundaryPoint bp = currentObject.getBoundaryPoint();
            if(bp != null) {
                PlacedObjectsAfterMove pam = new PlacedObjectsAfterMove();
                pam.currentMovedObject = currentObject;
                return pam;
            } else {
                return placeNativeWidgetObjectInEmptyPlaceOnCurrentWall(currentObject, currentWallIndex);
            }
        } else {
            for(int i = 0 ; i < intersectObjects.size() ; i++) {
                NormalObject normalObject = intersectObjects.get(i);
                SEObjectBoundaryPoint bp = intersectObjectNewBP.get(i);
                normalObject.setBoundaryPoint(bp);
            }
            //SEObjectBoundaryPoint bp = new SEObjectBoundaryPoint(currentWallIndex);
            SEVector3f center = mRealLocationInWall.clone();
            if(this.isObjectFixedOnYAxis(currentObject)) {
                SEVector3f yLocation = getLocationYInWall(currentObject, currentWallIndex);
                center.mD[1] = yLocation.mD[1];
            } else {
                center.mD[1] = getLocationYInWall(currentObject, currentWallIndex).mD[1];
            }

            SEObjectBoundaryPoint newObjectBP = createBoundaryPointFromObjectCenter(currentObject, center, currentWallIndex, SEObjectBoundaryPoint.CENTER_POINT_STYLE_MID_MID);
            currentObject.setBoundaryPoint(newObjectBP);
            PlacedObjectsAfterMove pam = new PlacedObjectsAfterMove();
            pam.currentMovedObject = currentObject;
            pam.objectsNeedMove = intersectObjects;
            return pam;
        }
    }
    private boolean isBoundaryPointEmpty(SEObjectBoundaryPoint bp) {
        return bp == null || bp.xyzSpan.equals(SEVector3f.ZERO);
    }
    public static SEObjectBoundaryPoint createBoundaryPointFromMountPointIndex(NormalObject currentObject,SEMountPointChain chain, int mountPointIndex, int wallIndex) {
        SEObjectBoundaryPoint bp = new SEObjectBoundaryPoint(wallIndex);
        SEMountPointChain.MatrixPoint mp = chain.getMatrixPointByIndex(mountPointIndex);
        bp.minMatrixPoint = mp;
        bp.maxMatrixPoint = mp.clone();
        bp.xyzSpan = currentObject.getObjectXYZSpan();
        SEMountPointData mpd = chain.getMountPointData(mountPointIndex);
        if (null == mpd) {
            Log.e("FUCK", "force erase this object");
            return null;
        } else {
            SEObjectBoundaryPoint minMaxPoint = SEObjectBoundaryPoint.getMinMaxPointInPlane(mpd.getTranslate(), bp.xyzSpan, bp.movePlane, SEObjectBoundaryPoint.CENTER_POINT_STYLE_BOTTOM_MID);
            bp.minPoint = minMaxPoint.minPoint;
            bp.maxPoint = minMaxPoint.maxPoint;
            bp.center = mpd.getTranslate().clone();
            return bp;
        }
    }
    private PlacedObjectsAfterMove placeNativeWidgetObjectOnOtherWall(NormalObject object, int currentWallIndex) {
        int nextWallIndex = getLeftWallIndex(currentWallIndex);
        while(nextWallIndex != currentWallIndex) {
            SEObjectBoundaryPoint bp = createNewBoundaryPointForObject(nextWallIndex, object, null);
            if(bp != null) {
                object.setBoundaryPoint(bp);
                PlacedObjectsAfterMove pam = new PlacedObjectsAfterMove();
                pam.currentMovedObject = object;
                return pam;
            }
            nextWallIndex = getLeftWallIndex(nextWallIndex);
        }
        return null;
    }
    private PlacedObjectsAfterMove placeShelfObjectOnOtherWall(NormalObject object, int currentWallIndex) {
        int nextWallIndex = getLeftWallIndex(currentWallIndex);
        while(nextWallIndex != currentWallIndex) {
            SEMountPointChain chain = getCurrentMountPointChain(nextWallIndex);
            int newMountPointIndex = getEmptyMountPointIndex(chain, nextWallIndex);
            if(newMountPointIndex != -1) {
                SEObjectBoundaryPoint bp = createBoundaryPointFromMountPointIndex(object, chain, newMountPointIndex, nextWallIndex);
                if (null == bp) {
                    object.getParent().removeChild(object, true);
                } else {
                    object.setBoundaryPoint(bp);
                    PlacedObjectsAfterMove pam = new PlacedObjectsAfterMove();
                    pam.currentMovedObject = object;
                    return pam;
                }
                return null;
            } else {
                nextWallIndex = getLeftWallIndex(nextWallIndex);
            }
        }
        return null;
    }
    private PlacedObjectsAfterMove placeShelfObjectOnEmptyPlace(SEMountPointChain chain, NormalObject currentObject, int currentWallIndex) {
        int newMountPointIndex = getEmptyMountPointIndex(chain, currentWallIndex);
        if(newMountPointIndex != -1) {
            SEObjectBoundaryPoint bp = createBoundaryPointFromMountPointIndex(currentObject, chain, newMountPointIndex, currentWallIndex);
            if (null == bp) {
                currentObject.getParent().removeChild(currentObject, true);
            } else {
                currentObject.setBoundaryPoint(bp);
                PlacedObjectsAfterMove pam = new PlacedObjectsAfterMove() ;
                pam.currentMovedObject = currentObject;
                return pam;
            }
            return null;
        } else {
            return null;
        }
    }
    private PlacedObjectsAfterMove placeNativeWidgetObjectInProperPlace(NormalObject currentObject, int currentWallIndex) {
        SEObjectBoundaryPoint currentBP = currentObject.getBoundaryPoint();
        if(isBoundaryPointEmpty(currentBP)) {
            PlacedObjectsAfterMove pam = placeNativeWidgetObjectInEmptyPlaceOnCurrentWall(currentObject, currentWallIndex);
            if(pam == null) {
                pam = placeNativeWidgetObjectOnOtherWall(currentObject, currentWallIndex);
            }
            return pam;
        } else {
            int objectOriginWallIndex = currentBP.wallIndex;
            if(objectOriginWallIndex == currentWallIndex) {
                PlacedObjectsAfterMove pam = new PlacedObjectsAfterMove();
                pam.currentMovedObject = currentObject;
                return pam;
            } else {
                PlacedObjectsAfterMove pam = placeNativeWidgetObjectInEmptyPlaceOnCurrentWall(currentObject, currentWallIndex);
                if(pam == null) {
                    pam = new PlacedObjectsAfterMove();
                    pam.currentMovedObject = currentObject;
                    return pam;
                } else {
                    return pam;
                }
            }
        }
    }
    private PlacedObjectsAfterMove placeShelfObjectOnProperPlace(SEMountPointChain chain, NormalObject currentObject,
                                                                   int currentWallIndex) {
        SEObjectBoundaryPoint currentBP = currentObject.getBoundaryPoint();
        if(isBoundaryPointEmpty(currentBP)) {
            PlacedObjectsAfterMove pam = placeShelfObjectOnEmptyPlace(chain, currentObject, currentWallIndex);
            if(pam == null) {
                pam = placeShelfObjectOnOtherWall(currentObject, currentWallIndex);
            }
            return pam;
        } else {
            int objectOriginWallIndex = currentBP.wallIndex;
            if(objectOriginWallIndex == currentWallIndex) {
                PlacedObjectsAfterMove pam = new PlacedObjectsAfterMove();
                pam.currentMovedObject = currentObject;
                return pam;
            } else {
                PlacedObjectsAfterMove pam = placeShelfObjectOnEmptyPlace(chain, currentObject, currentWallIndex);
                if(pam == null) {
                    pam = new PlacedObjectsAfterMove();
                    pam.currentMovedObject = currentObject;
                }
                return pam;
            }
        }
    }

    //this function place object which must be on shelf.
    private PlacedObjectsAfterMove placeShelfObjectOnWallWithFingerPoint(SEMountPointChain chain, NormalObject currentObject,
                                                          int currentWallIndex, MountPointInfo mpi) {
        SEDebug.myAssert(mRealLocationInWall != null, "real location in wall is not null");
        ArrayList<NormalObject> intersectObjects = getAppObjectIntersectObjects(currentObject, currentWallIndex, mpi);
        ArrayList<SEObjectBoundaryPoint> intersectObjectsBP = getAppObjectIntersectObjectsNewPlace(currentObject, intersectObjects, currentWallIndex, mpi);
        if(intersectObjects.size() != intersectObjectsBP.size()) {
            return placeShelfObjectOnProperPlace(chain, currentObject, currentWallIndex);
        } else {
            for(int i = 0 ; i < intersectObjects.size() ; i++) {
                NormalObject object = intersectObjects.get(i);
                SEObjectBoundaryPoint bp = intersectObjectsBP.get(i);
                object.setBoundaryPoint(bp);
            }
            SEObjectBoundaryPoint newBP = createBoundaryPointFromMountPointIndex(currentObject, chain, mpi.mountPointInfo.mIndex, currentWallIndex);
            if (null == newBP) {
                currentObject.getParent().removeChild(currentObject, true);
            } else {
                currentObject.setBoundaryPoint(newBP);
                PlacedObjectsAfterMove pam = new PlacedObjectsAfterMove();
                pam.currentMovedObject = currentObject;
                pam.objectsNeedMove = intersectObjects;
                return pam;
            }
            return null;
        }
    }
    private PlacedObjectsAfterMove placeObjectOnCurrentWall(NormalObject currentObject, int currentWallIndex, MountPointInfo mountPointInfo) {
        boolean canObjectOnShelf = currentObject.canPlaceOnShelf();
        boolean canObjectOnWall = currentObject.canPlaceOnWall();
        SEMountPointChain chain = getCurrentMountPointChain(currentWallIndex);
        if(canObjectOnShelf) {
            if(mountPointInfo != null) {
                return placeShelfObjectOnWallWithFingerPoint(chain, currentObject, currentWallIndex, mountPointInfo);
            } else {
                return placeShelfObjectOnProperPlace(chain, currentObject, currentWallIndex);
            }
        } else if(canObjectOnWall) {
            if(mountPointInfo != null) {
                return placeNativeWidgetObjectOnCurrentWallWithFinger(currentObject, currentWallIndex, mountPointInfo);
            } else {
                return placeNativeWidgetObjectInProperPlace(currentObject, currentWallIndex);
            }
        } else {
            return null;
        }
    }
    public static SEVector3f createNativeShelfObjectScale(NormalObject object) {
        if(object instanceof Folder) {
            return new SEVector3f(1, 1, 1);
        } else {
            return new SEVector3f(2, 2, 2);
        }
    }
    private SETransParas setShelfObjectWithMountPoint(int mountPointIndex, int wallIndex, SEMountPointChain chain, NormalObject currentObject) {
        SEMountPointData destMPD = chain.getMountPointData(mountPointIndex);
        SEVector3f xyzSpan = currentObject.getObjectXYZSpan();

        SEMountPointData d = new SEMountPointData("destpoint", destMPD.getTranslate().clone(),  destMPD.getScale().clone(), destMPD.getRotate().clone());
        if(currentObject instanceof  AppObject) {
            d.getTranslate().mD[2] = calculateAppObjectZPosition(d.getTranslate().mD[2], xyzSpan, currentObject);//xyzSpan.mD[2] / 2;
        }
        SEVector3f wallLocation = getLocationYInWall(currentObject, wallIndex);

        SEVector3f wallSpaceCoord = d.getTranslate().clone();
        wallSpaceCoord.mD[1] = wallLocation.mD[1];
        SETransParas dstTransParas = createUserTransParasFromWallTransform(wallIndex, wallSpaceCoord);
        setAppObjectBoundaryPoint(currentObject, wallIndex, mountPointIndex);

        if(currentObject.getObjectInfo().mIsNativeObject) {
            dstTransParas.mScale = createNativeShelfObjectScale(currentObject);
        }
        return dstTransParas;
    }
    private SETransParas placeObjectOnShelf(NormalObject currentObject, int destMountPointIndex, int wallIndex) {
        SEMountPointChain chain = getCurrentMountPointChain(wallIndex);
        WallShelf destShelf = mHouse.getWallShelfWithMountPoint(wallIndex, destMountPointIndex);
        int objectOriginWallIndex = currentObject.getObjectSlot().mSlotIndex;
        WallShelf currentObjectShelf = mHouse.getWallShelfWithObject(objectOriginWallIndex, currentObject);

        boolean isTheSameShelf = destShelf == currentObjectShelf;
        boolean moveOK = false;
        if(destShelf == null) {
            addOrChangeShelf(currentObject, currentObject.getObjectXYZSpan(), destMountPointIndex, wallIndex);
            if(currentObjectShelf != null) {
                int currentObjectMountPointIndex = currentObject.getObjectSlot().mMountPointIndex;
                mHouse.removeObjectFromCurrentShelf(currentObject, currentObjectMountPointIndex, currentObjectShelf);
            }
            moveOK = true;
        } else {
            if(isTheSameShelf) {
                moveOK = moveObjectOnSameShelf(currentObject, destShelf, chain, wallIndex, destMountPointIndex);
            } else {
                moveOK = moveObjectOnDiffShelf(currentObject, destShelf, currentObjectShelf, chain, wallIndex, destMountPointIndex);
            }
        }
        testAssertObjectOnShelf(currentObject);
        SEMountPointData destMPD = moveOK ? chain.getMountPointData(destMountPointIndex) : null;
        if(null != destMPD) {
            SEVector3f xyzSpan = currentObject.getObjectXYZSpan();

            SEMountPointData d = new SEMountPointData("destpoint", destMPD.getTranslate().clone(),  destMPD.getScale().clone(), destMPD.getRotate().clone());
            if(currentObject instanceof  AppObject) {
                d.getTranslate().mD[2] = calculateAppObjectZPosition(d.getTranslate().mD[2], xyzSpan, currentObject);//xyzSpan.mD[2] / 2;
            }
            SEVector3f wallLocation = getLocationYInWall(currentObject, wallIndex);
            SETransParas transParas = setAppObjectToWall(d, wallIndex, destMountPointIndex, currentObject, wallLocation);
            return transParas;
        } else {
            int originMountPointIndex = currentObject.getObjectSlot().mMountPointIndex;
            destMPD = chain.getMountPointData(originMountPointIndex);
            SEVector3f wallLocation = getLocationYInWall(currentObject, wallIndex);
            SETransParas transParas = setAppObjectToWall(destMPD, objectOriginWallIndex, originMountPointIndex, currentObject, wallLocation);
            return transParas;
        }
    }
    private void updateWidgetObjectLayout(NormalObject currentObject) {
        if(currentObject instanceof WidgetObject) {
            ((WidgetObject)currentObject).requestUpdateAndroidWidget();
        }
    }
    //inputBP is the SEObjectBoundaryPoint which has just matrix point
    private SEObjectBoundaryPoint finishBoundaryPoint(NormalObject currentObject, SEObjectBoundaryPoint inputBP) {
        if(currentObject.isShelfObject()) {
            SEMountPointChain chain = getCurrentMountPointChain(inputBP.wallIndex);
            int mountPointIndex = chain.getIndex(inputBP.minMatrixPoint.row, inputBP.minMatrixPoint.col);
            SEMountPointData mpd = chain.getMountPointData(mountPointIndex);
            SEObjectBoundaryPoint minMaxPoint = SEObjectBoundaryPoint.getMinMaxPointInPlane(mpd.getTranslate(), inputBP.xyzSpan, inputBP.movePlane, SEObjectBoundaryPoint.CENTER_POINT_STYLE_BOTTOM_MID);
            inputBP.minPoint = minMaxPoint.minPoint;
            inputBP.maxPoint = minMaxPoint.maxPoint;
            inputBP.maxMatrixPoint.mountPointIndex = mountPointIndex;
            inputBP.minMatrixPoint.mountPointIndex = mountPointIndex;
            return inputBP;
        } else {
            SEMountPointChain currentChain = this.getCurrentMountPointChain(inputBP.wallIndex);
            SEMountPointChain.MatrixPoint minMatrixPoint = currentChain.getMatrixPointInPlaneXZ(inputBP.minPoint);
            SEMountPointChain.MatrixPoint maxMatrixPoint = currentChain.getMatrixPointInPlaneXZ(inputBP.maxPoint);
            inputBP.minMatrixPoint = minMatrixPoint;
            inputBP.maxMatrixPoint = maxMatrixPoint;
            return inputBP;
        }

    }
    private void setRealPlaceForPlacedObjectsAfterMove(PlacedObjectsAfterMove pam) {
        if(pam.currentMovedObject != null) {
            setObjectPlace(pam.currentMovedObject);
        }
        for(int i = 0 ; i < pam.objectsNeedMove.size() ; i++) {
            NormalObject object = pam.objectsNeedMove.get(i);
            if(pam.currentMovedObject != null && pam.currentMovedObject.isShelfObject() && object.isShelfObject()) {
                continue;
            }
            setObjectPlace(object);
            object.updateSlotDB();
        }
    }
    private void printAllShelf() {
        for(SEObject obj : mHouse.mChildObjects) {
            if(obj instanceof  WallShelf) {
                WallShelf shelf = (WallShelf) obj;
                shelf.printObjects();
            }
        }
    }
    private void setObjectPlace(NormalObject currentObject) {
        SEObjectBoundaryPoint bp = currentObject.getBoundaryPoint();
        if(currentObject.isShelfObject()) {
            SETransParas tp = placeObjectOnShelf(currentObject, bp.minMatrixPoint.mountPointIndex, bp.wallIndex);
            currentObject.getUserTransParas().mTranslate = tp.mTranslate;
            currentObject.getUserTransParas().mScale = tp.mScale;
            currentObject.getUserTransParas().mRotate = tp.mRotate;
            currentObject.setUserTransParas();
            currentObject.getObjectSlot().mSlotIndex = bp.wallIndex;
            currentObject.getObjectSlot().mMountPointIndex = bp.minMatrixPoint.mountPointIndex;
            printAllShelf();
            testAssertObjectMountPointOnShelf();

        } else {
            currentObject.getObjectSlot().mSlotIndex = bp.wallIndex;
            setObjectUserTransParas(bp.wallIndex, currentObject);
        }
    }
    /*
    private ArrayList<NormalObject> getShelfObjectInWall(int wallIndex) {
        ArrayList<NormalObject> shelfObjectList = new ArrayList<NormalObject>();
        for(SEObject obj : mHouse.mChildObjects) {
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
    */
    /*
    private ArrayList<ArrayList<WallShelf>> getWallShelfsInWall(int num) {
        ArrayList<ArrayList<WallShelf>> shelfs = new ArrayList<ArrayList<WallShelf>>();
        for(int i = 0 ; i < num ; i++) {
            ArrayList<WallShelf> shelfList = new ArrayList<WallShelf>();
            shelfs.add(shelfList);
        }
        for(SEObject obj : mHouse.mChildObjects) {
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
    */
    /*
    private WallShelf getNearestShelf(SEMountPointChain chain, int mountPointIndex, ArrayList<WallShelf> shelfList) {
        SEMountPointData mpd = chain.getMountPointData(mountPointIndex);
        SEVector3f mpTranslate = mpd.getTranslate();
        final float ZE = 2;
        for(int i = 0 ; i < shelfList.size() ; i++) {
            WallShelf shelf = shelfList.get(i);
            SEObjectBoundaryPoint bp = shelf.getBoundaryPoint();
            SEVector3f minPoint = bp.minPoint;
            SEVector3f maxPoint = bp.maxPoint;
            SEVector3f center = bp.center;
            if(Math.abs(mpTranslate.mD[2] - center.mD[2]) <= ZE ) {
                if(mpTranslate.mD[0] >= minPoint.getX() && mpTranslate.mD[0] <= maxPoint.getX()) {
                    return shelf;
                }
            }
        }
        return null;
    }
    */

    private void realSetShelfObjectToShelf() {
        ArrayList<ArrayList<WallShelf>> shelfs = mHouse.getWallShelfsInWall();
        int num = shelfs.size();
        for(int wallIndex = 0 ; wallIndex < num ; wallIndex++) {
            ArrayList<WallShelf> shelfList = shelfs.get(wallIndex);
            ArrayList<NormalObject> shelfObjList = mHouse.getShelfObjectInWall(wallIndex);
            SEMountPointChain currentChain = getCurrentMountPointChain(wallIndex);
            for(NormalObject shelfObj : shelfObjList) {
                int mountPointIndex = shelfObj.getObjectSlot().mMountPointIndex;
                WallShelf shelf = mHouse.getNearestShelf(currentChain, mountPointIndex, shelfList);
                SEDebug.myAssert(shelf != null, "shelf must not be null");
                if (null == shelf) {
                    Log.e(TAG, "realSetShelfObjectToShelf, should not be here");
                } else {
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
                WallShelf shelf = mHouse.getNearestShelf(currentChain, i, shelfList);
                if(shelf != null) {
                    shelf.addObjectOnShelf(null, i);
                }
            }
        }

    }
    private void setShelfObjectToShelf() {
        NormalObject currentObject = getOnMoveObject();
        if(currentObject instanceof  WallShelf) {
            return;
        }
        realSetShelfObjectToShelf();
        /*
        ArrayList<ArrayList<WallShelf>> shelfs = mHouse.getWallShelfsInWall();
        int num = shelfs.size();
        for(int wallIndex = 0 ; wallIndex < num ; wallIndex++) {
            ArrayList<WallShelf> shelfList = shelfs.get(wallIndex);
            ArrayList<NormalObject> shelfObjList = mHouse.getShelfObjectInWall(wallIndex);
            SEMountPointChain currentChain = getCurrentMountPointChain(wallIndex);
            for(NormalObject shelfObj : shelfObjList) {
                int mountPointIndex = shelfObj.getObjectSlot().mMountPointIndex;
                WallShelf shelf = mHouse.getNearestShelf(currentChain, mountPointIndex, shelfList);
                SEDebug.myAssert(shelf != null, "shelf must not be null");
                shelf.addObjectOnShelf(shelfObj, mountPointIndex);
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
                WallShelf shelf = mHouse.getNearestShelf(currentChain, i, shelfList);
                if(shelf != null) {
                    shelf.addObjectOnShelf(null, i);
                }
            }
        }
        */
    }
    private ArrayList<NormalObject> getShelfIntersectObjects(ArrayList<Integer> mountPointList,  int wallIndex) {
        ArrayList<NormalObject> normalObjects = getNormalObjectsOnWall(wallIndex);
        Set<NormalObject> allIntersectObjects = new HashSet<NormalObject>();
        SEMountPointChain chain = getCurrentMountPointChain(wallIndex);
        for(Integer ii : mountPointList) {
            int mountPointIndex = ii.intValue();
            ArrayList<NormalObject> retObjects = new ArrayList<NormalObject>();
            for(NormalObject object : normalObjects) {
                if(object instanceof  WallShelf) {
                    ArrayList<Integer> mpList = ((WallShelf)object).getEmptyObjectMountPoint();
                    MatrixPoint mp = chain.getMatrixPointByIndex(mountPointIndex);
                    int shelfWallIndex = object.getObjectSlot().mSlotIndex;
                    if(shelfWallIndex == wallIndex) {
                        for(Integer emptyI : mpList) {
                            int emptyIndex = emptyI.intValue();
                            MatrixPoint tmpMP = chain.getMatrixPointByIndex(emptyIndex);
                            boolean bb = isMatrixPointInRect(tmpMP.row, tmpMP.col, mp, mp.clone());
                            if(bb) {
                                retObjects.add(object);
                            }
                        }
                    }
                } else {
                    boolean b = isMountPointInObject(object, wallIndex, mountPointIndex);
                    if(b) {
                        retObjects.add(object);
                    }
                }
            }
            allIntersectObjects.addAll(retObjects);
        }
        ArrayList<NormalObject> retObjs = new ArrayList<NormalObject>();
        retObjs.addAll(allIntersectObjects);
        return retObjs;
    }
    private PlacedObjectsAfterMove getShelfIntersectObjectNewPlace(ArrayList<Integer> mountPointList, SEMountPointChain chain, int wallIndex) {
        ArrayList< NormalObject> intersectObjects = getShelfIntersectObjects(mountPointList, wallIndex);
        for(NormalObject obj : intersectObjects) {
            if(obj.isShelfObject() || obj instanceof  WallShelf) {
                return null;
            }
        }
        ArrayList<SEObjectBoundaryPoint> bpList = new ArrayList<SEObjectBoundaryPoint>();
        for(NormalObject obj : intersectObjects) {
            SEObjectBoundaryPoint bp = createNewBoundaryPointForObject(wallIndex, obj, bpList);
            if(bp == null) {
                return null;
            } else {
                /*
                for(SEObjectBoundaryPoint tmpBP : bpList) {
                    if(isRectIntersect(bp.minPoint, bp.maxPoint, tmpBP.minPoint, tmpBP.maxPoint)) {
                        return null;
                    }
                }
                */
                bpList.add(bp);
            }
        }
        for(int i = 0 ; i < intersectObjects.size() ; i++) {
            NormalObject object = intersectObjects.get(i);
            SEObjectBoundaryPoint bp = bpList.get(i);
            object.setBoundaryPoint(bp);
        }
        PlacedObjectsAfterMove pam = new PlacedObjectsAfterMove();
        pam.currentMovedObject = null;
        pam.objectsNeedMove = intersectObjects;
        return pam;
    }
    private void handleShelfMove(int wallIndex, NormalObject object) {
        WallShelf currentShelf = (WallShelf)object;
        SEVector3f shelfWorldCenter = currentShelf.getAbsoluteTranslate();
        SEObjectBoundaryPoint shelfBP = currentShelf.getBoundaryPoint();
        SEVector3f realSpan = currentShelf.getObjectXYZSpan();
        SEVector3f origSpan = currentShelf.getObjectOriginXYZSpan();
        SEVector3f leftPoint = shelfWorldCenter.subtract(new SEVector3f(realSpan.mD[0] / 2, 0, 0));
        leftPoint = leftPoint.add(new SEVector3f(origSpan.mD[0] / 2, 0, 0));
        SEVector3f leftPointInWallCoord = toWallSpaceCoordinate(wallIndex, leftPoint);
        SEMountPointChain chain = getCurrentMountPointChain(wallIndex);
        SEMountPointChain.ClosestMountPointData cmd = chain.getClosestMountPoint(leftPointInWallCoord);
        int leftMountPointIndex = cmd.mIndex;
        int objectsNum = currentShelf.getObjectNumOnShelf();
        ArrayList<Integer> mountPointList = new ArrayList<Integer>();
        int startMountPointIndex = leftMountPointIndex;
        MatrixPoint mp = chain.getMatrixPointByIndex(startMountPointIndex);
        int colNum = chain.getColCount();
        for(int i = 0 ; i < objectsNum ; i++) {
            if(mp.col < colNum) {
                int index = chain.getIndex(mp.row, mp.col);
                mountPointList.add(new Integer(index));
            }
            mp.col++;
        }
        boolean outOfBoundary = mountPointList.size() != objectsNum;

        PlacedObjectsAfterMove pam = null;
        if(outOfBoundary == false) {
            pam = getShelfIntersectObjectNewPlace(mountPointList, chain, wallIndex);
        }
        if(pam == null) {
            for(ObjectOnShelfPosition p : mBeginObjectOnShelfPositionList) {
                NormalObject tmpObject = p.object;
                int mountPointIndex = tmpObject.getObjectSlot().mMountPointIndex;
                int tmpWallIndex = tmpObject.getObjectSlot().mSlotIndex;
                SEMountPointChain tmpChain = getCurrentMountPointChain(tmpWallIndex);
                SETransParas transParas = setShelfObjectWithMountPoint(mountPointIndex, tmpWallIndex, tmpChain, tmpObject);
                tmpObject.getUserTransParas().set(transParas);
                tmpObject.setUserTransParas();
                tmpObject.changeParent(mHouse);
            }
            SEVector3f shelfCenter = currentShelf.getBoundaryPoint().center;
            int shelfWallIndex = currentShelf.getBoundaryPoint().wallIndex;
            SETransParas shelfTransParas = createUserTransParasFromWallTransform(shelfWallIndex, shelfCenter);
            shelfTransParas.mScale = currentShelf.getUserTransParas().mScale.clone();
            currentShelf.getUserTransParas().set(shelfTransParas);
            currentShelf.setUserTransParas();
            currentShelf.changeParent(mHouse);
        } else {
            SEMountPointChain tmpChain = getCurrentMountPointChain(wallIndex);
            for(int i = 0 ; i < mountPointList.size() ; i++) {
                Integer ii = mountPointList.get(i);
                int mountPointIndex = ii.intValue();
                NormalObject tmpObject = currentShelf.getObjectByIndex(i);
                currentShelf.setMountPointByIndex(i, mountPointIndex);
                if(tmpObject != null) {
                    SETransParas transParas = setShelfObjectWithMountPoint(mountPointIndex, wallIndex, tmpChain, tmpObject);
                    tmpObject.getUserTransParas().set(transParas);
                    tmpObject.setUserTransParas();
                    tmpObject.changeParent(mHouse);
                    tmpObject.getObjectSlot().mMountPointIndex = mountPointIndex;
                    tmpObject.getObjectSlot().mSlotIndex = wallIndex;
                }
            }
            int leftIndex = mountPointList.get(0);
            int rightIndex = mountPointList.get(mountPointList.size() - 1);
            SEMountPointData leftMPD = tmpChain.getMountPointData(leftIndex);
            SEMountPointData rightMPD = tmpChain.getMountPointData(rightIndex);
            SEVector3f shelfCenter = currentShelf.getBoundaryPoint().center;
            float x = (leftMPD.getTranslate().mD[0] + rightMPD.getTranslate().mD[0]) / 2;
            shelfCenter.mD[0] = x;
            shelfCenter.mD[2] = leftMPD.getTranslate().mD[2];
            SETransParas shelfTransParas = createUserTransParasFromWallTransform(wallIndex, shelfCenter);
            shelfTransParas.mScale = currentShelf.getUserTransParas().mScale.clone();
            currentShelf.getUserTransParas().set(shelfTransParas);
            currentShelf.setUserTransParas();
            currentShelf.changeParent(mHouse);
            SEObjectBoundaryPoint newBP = createBoundaryPointFromObjectCenter(currentShelf, shelfCenter, wallIndex, SEObjectBoundaryPoint.CENTER_POINT_STYLE_TOP_MID);
            currentShelf.setBoundaryPoint(newBP);
            currentShelf.createRowIndexInWall(getWallName(wallIndex));
            currentShelf.getObjectSlot().mSlotIndex = wallIndex;
            setRealPlaceForPlacedObjectsAfterMove(pam);
        }
    }
    private void handleShelfScale(int wallIndex, NormalObject object) {
        WallShelf currentShelf = (WallShelf)object;
        SEVector3f currentScale = currentShelf.getUserTransParas().mScale.clone();
        Log.i(TAG, "finish currentscale = " + currentScale);
        if(mShelfMoveBP == null) {
            Log.i(TAG, "## mShelfMoveBP == null ####");
            object.changeParent(mHouse);
            return;
        }
        Log.i(TAG, "## handleShelfMove wallIndex = " + wallIndex + " ###");
        float scalex = currentScale.mD[0];
        int scaleIntx = (int)(scalex + 0.5);
        SEVector3f xyzSpan = object.getObjectOriginXYZSpan();
        float currentX = scalex * xyzSpan.mD[0];
        float intCurrentX = scaleIntx * xyzSpan.mD[0];
        float deltax = currentX - intCurrentX;
        float right = mShelfMoveBP.center.mD[0] + (xyzSpan.mD[0] * scalex) / 2 - deltax;
        float left = mShelfMoveBP.center.mD[0] - (xyzSpan.mD[0] * scalex) / 2;
        if(mCurrentShelfPressPosition == CLICK_SHELF_LEFT) {
            left = mShelfMoveBP.center.mD[0] - (xyzSpan.mD[0] * scalex) / 2 + deltax;
            right = mShelfMoveBP.center.mD[0] + (xyzSpan.mD[0] * scalex) / 2;
        }
        float x = (right + left) / 2;
        SEVector3f newCenter = new SEVector3f(x, mShelfMoveBP.center.mD[1], mShelfMoveBP.center.mD[2]);
        xyzSpan = xyzSpan.mul(new SEVector3f(scaleIntx, 1, 1));
        SEVector3f minPoint = new SEVector3f();
        SEVector3f maxPoint = new SEVector3f();
        minPoint.mD[0] = left;
        minPoint.mD[1] = mShelfMoveBP.minPoint.mD[1];
        minPoint.mD[2] = newCenter.mD[2] - xyzSpan.mD[2] / 2;

        maxPoint.mD[0] = right;
        maxPoint.mD[1] = mShelfMoveBP.maxPoint.mD[1];
        maxPoint.mD[2] = newCenter.mD[2] + xyzSpan.mD[2] / 2;
        SEMountPointChain chain = getCurrentMountPointChain(wallIndex);
        MatrixPoint tmpBP1 = chain.getMatrixPointInPlaneXZ(minPoint);
        MatrixPoint tmpBP2 = chain.getMatrixPointInPlaneXZ(maxPoint);

        SEObjectBoundaryPoint newBP = new SEObjectBoundaryPoint(wallIndex);
        newBP.center = newCenter;
        newBP.xyzSpan = xyzSpan;
        newBP.minMatrixPoint = tmpBP1;
        newBP.maxMatrixPoint = tmpBP2;
        newBP.minPoint = minPoint;
        newBP.maxPoint = maxPoint;
        object.setBoundaryPoint(newBP);

        SETransParas transParas = createUserTransParasFromWallTransform(wallIndex, newCenter);
        transParas.mScale = new SEVector3f(scaleIntx, 1, 1);
        object.getUserTransParas().mTranslate = transParas.mTranslate;
        object.getUserTransParas().mScale = transParas.mScale;
        object.getUserTransParas().mRotate = transParas.mRotate;
        object.setUserTransParas();
        object.changeParent(mHouse);
        currentShelf.removeAllObjectOnShelf();
        mHouse.setShelfObjectWhenAddShelf(currentShelf, chain, wallIndex);
        currentShelf.printObjects();

    }
    private void slotToWall_new(MountPointInfo mountPointInfo) {
	    NormalObject currentObject = getOnMoveObject();
        if(currentObject instanceof  WallShelf) {
            if(mCurrentShelfPressPosition == CLICK_SHELF_LEFT || mCurrentShelfPressPosition == CLICK_SHELF_RIGHT) {
                handleShelfScale(mHouse.getWallNearestIndex(), currentObject);
            } else {
                handleShelfMove(mHouse.getWallNearestIndex(), currentObject);
            }
            return;
        }
	    int wallIndex = mHouse.getWallNearestIndex();
        if(mRealLocationInWall == null) {
            mRealLocationInWall = calculateRealPointInWall(wallIndex);
        }
	    SEVector3f realPointInWall = mRealLocationInWall.clone(); //calculateRealPointInWall(mountPointInfo.wallIndex);

        if(currentObject.isShelfObject()) {
            currentObject.getObjectInfo().mSlotType = ObjectInfo.SLOT_TYPE_WALL_SHELF;
        } else {
	        currentObject.getObjectInfo().mSlotType = ObjectInfo.SLOT_TYPE_WALL;
        }
	    SETransParas srcTransParas = mObjectTransParas.clone();
	    SEVector3f srcWallSpaceCoord = toWallSpaceCoordinate(wallIndex, srcTransParas.mTranslate);
	    srcTransParas = createUserTransParasFromWallTransform(wallIndex, srcWallSpaceCoord);
	    /////////////////////////
	    PlacedObjectsAfterMove pam = null;
	    boolean isOutOfBoundary = this.isObjectOutOfWallBoundary(wallIndex, currentObject);
	    if(isOutOfBoundary == false && mountPointInfo != null) {
            //dstTransParas = placeObjectOnShelf(currentObject, mountPointInfo.mountPointInfo.mIndex, wallIndex);
            pam = placeObjectOnCurrentWall(currentObject, wallIndex, mountPointInfo);
	    } else {
	    	Log.i(TAG, "## handleobject out of wall ##");
	    	pam = handleObjectOutOfWall(currentObject, wallIndex);
	    }
	    if(pam == null) {
    	    this.hideAllLineObject();
            currentObject.getParent().removeChild(currentObject, true);
    	    return;
	    }

	    SEDebug.myAssert(pam != null , "place object after move is null");
	    /*
	    currentObject.getUserTransParas().mTranslate = srcTransParas.mTranslate;
	    currentObject.getUserTransParas().mRotate = srcTransParas.mRotate;
	    currentObject.getUserTransParas().mScale = srcTransParas.mScale;
	    currentObject.setUserTransParas();
        */

        setRealPlaceForPlacedObjectsAfterMove(pam);
        currentObject.changeParent(mHouse);
        updateWidgetObjectLayout(currentObject);
        /*
	    mSetToRightPositionAnimation = new SetToRightPositionAnimation(getScene(), currentObject, 
	    		                               srcTransParas, dstTransParas, 30);
	    TransParasAnimationFinish f = new TransParasAnimationFinish();
	    f.currentObject = currentObject;
	    mSetToRightPositionAnimation.setAnimFinishListener(f);
	    mSetToRightPositionAnimation.execute();
        */
	    //this.getExistentObject();
	    if(HomeUtils.DEBUG) {
	        showLine_new(null, false);
	        showArea(0, false);
	    }
        if(currentObject.isShelfObject()) {
            currentObject.setShadowObjectVisibility_JNI(false);
        }
	    currentObject.getObjectSlot().mSlotIndex = wallIndex;
        //createShelf(currentObject, wallIndex);
        //addObjectWithShelf(currentObject, wallIndex);
    }
    private void setAppObjectBoundaryPoint(NormalObject appObject, int wallIndex, int mountPointIndex) {
	    SEDebug.myAssert(mountPointIndex != -1, "setAppObjectBoundaryPoint mountPointIndex == -1");
	    SEMountPointChain currentChain = this.getCurrentMountPointChain(wallIndex);
	    SEMountPointData mpd = currentChain.getMountPointData(mountPointIndex);

	    SEVector3f xyzSpan = appObject.getObjectXYZSpan();
	    SEObjectBoundaryPoint bp = SEObjectBoundaryPoint.getMinMaxPointInPlane(mpd.getTranslate(), xyzSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ, SEObjectBoundaryPoint.CENTER_POINT_STYLE_BOTTOM_MID);
	    bp.center = mpd.getTranslate().clone();
	    SEMountPointChain.MatrixPoint mp = currentChain.getMatrixPointByIndex(mountPointIndex);
	    SEMountPointChain.MatrixPoint mp1 = mp.clone();
	    bp.minMatrixPoint = mp;
	    bp.maxMatrixPoint = mp1;
	    bp.movePlane = SEObjectBoundaryPoint.MOVE_PLANE_XZ;
	    bp.xyzSpan = xyzSpan;
	    bp.wallIndex = wallIndex;
	    appObject.setBoundaryPoint(bp);
    }
    private void printTrans() {
    	    SEObject object = getOnMoveObject();
	    SERotate localRotate = new SERotate();
	    object.getLocalRotate_JNI(localRotate.mD);
	    SEVector3f localTranslate = new SEVector3f();
	    object.getLocalTranslate_JNI(localTranslate.mD);
	    SEVector3f localScale = new SEVector3f();
	    object.getLocalScale_JNI(localScale.mD);
	    SEVector3f userTranslate = object.getUserTranslate();
	    SERotate userRotate = object.getUserRotate();
	    SEVector3f userScale = object.getUserScale();
	    //Log.i(TAG, "local trans = " + localTranslate + " | " + localRotate + " | " + localScale);
	    //Log.i(TAG, "user trans = " + userTranslate + " | " + localRotate + " | " + localScale);
    }
    private float getMoveObjectParentAngle() {
    	SERotate localRotate = new SERotate();
    	getOnMoveObject().getLocalRotate_JNI(localRotate.mD);
    	float angle = localRotate.mD[0] + getOnMoveObject().getUserRotate().getAngle();
        SEObject parent = getOnMoveObject().getParent();
        while (parent != null) {
            angle = angle + parent.getUserRotate().getAngle();
            parent = parent.getParent();
        }
        return angle;
    }
    private void getWallLocalRotateAndTranslateScale(String wallName, SEVector3f translate, 
    		                                             SERotate rotate, SEVector3f scale) {
	    SEObject wallObject = getScene().findObject(wallName, 0);
	    wallObject.getLocalRotate_JNI(rotate.mD);
	    wallObject.getLocalTranslate_JNI(translate.mD);
	    wallObject.getLocalScale_JNI(scale.mD);
    }
    private SEMountPointChain.ClosestMountPointData getAppObjectClosestMountPoint(int wallIndex, NormalObject appObject) {
	    SEMountPointChain mpc = this.getCurrentMountPointChain(wallIndex);
	    SEObjectBoundaryPoint appObjectBP = appObject.getBoundaryPoint();
	    SEDebug.myAssert(appObjectBP != null, "appObjectBP is null");
	    int mountPointIndex = mpc.getIndex(appObjectBP.minMatrixPoint.row, appObjectBP.minMatrixPoint.col);
	    ArrayList<Integer> indexNeighborList = mpc.getMountPointIndexNeighbor(mountPointIndex);
   	    List<ConflictObject> conflictObjects = this.getExistentObject(wallIndex);
	    for(int neighborIndex : indexNeighborList) {
    	    boolean found = false;
    	    for(ConflictObject obj : conflictObjects) {
	    	    //Log.i(TAG, "conflict obj slot index = " + obj.mConflictObject.getObjectSlot().mMountPointIndex);
	    	    if(obj.mConflictObject.getObjectSlot().mMountPointIndex == neighborIndex) {
	    	    	found = true;
	    	    	break;
	    	    } else if(isObjectFixedInMountPoint(obj.mConflictObject) == false) {
    	    	    SEMountPointChain.MatrixPoint mp = mpc.getMatrixPointByIndex(neighborIndex);
    	    	    found = isMatrixPointInObject(mp.row, mp.col, obj.mConflictObject);
    	    	    if(found) {
    	    	    	break;
    	    	    }
	    	    }
    	    }
            if(found == false) {
                SEMountPointChain.MatrixPoint mp = mpc.getMatrixPointByIndex(neighborIndex);
                found = isMatrixPointInObject(mp.row, mp.col, getOnMoveObject());
            }
    	    if(found == false) {
	    	    SEMountPointData retMPD = mpc.getMountPointData( neighborIndex);
	    	    SEMountPointChain.ClosestMountPointData closeData = new SEMountPointChain.ClosestMountPointData();
	    	    closeData.mIndex = neighborIndex;
	    	    closeData.mMPD = retMPD;
	    	    closeData.mDist = 0;
	    	    return closeData;
    	    }
	    }
	    return null;
    }
    private SEMountPointChain.ClosestMountPointData getClosestMountPointToMPI(String objectName, MountPointInfo mpi) {
	    int wallIndex = mpi.wallIndex;
	    String vesselName = getWallName(wallIndex);
	    SEMountPointManager mountPointManager = getScene().getMountPointManager();
	    SEMountPointChain mpc = mountPointManager.getMountPointChain(objectName, mHouse.mName,vesselName, mObjectContainerName);
	    ArrayList<Integer> indexNeighborList = mpc.getMountPointIndexNeighbor(mpi.mountPointInfo.mIndex);
	    assert(indexNeighborList != null);
	    List<ConflictObject> conflictObjects = this.getExistentObject(wallIndex);
	    for(int neighborIndex : indexNeighborList) {
    	    boolean found = false;
    	    for(ConflictObject obj : conflictObjects) {
	    	    //Log.i(TAG, "conflict obj slot index = " + obj.mConflictObject.getObjectSlot().mMountPointIndex);
	    	    if(obj.mConflictObject.getObjectSlot().mMountPointIndex == neighborIndex) {
	    	    	    found = true;
	    	    	    break;
	    	    } else if(obj.mConflictObject.getObjectInfo().mIsNativeObject) {
	    	    	    SEMountPointChain.MatrixPoint mp = mpc.getMatrixPointByIndex(neighborIndex);
	    	    	    found = isMatrixPointInObject(mp.row, mp.col, obj.mConflictObject);
	    	    }
    	    }
    	    if(found == false) {
	    	    if(getOnMoveObject().getObjectInfo().mIsNativeObject) {
	    	    	    if(mMovedObjectOriginBP != null) {
	    	       	    SEMountPointChain.MatrixPoint mp = mpc.getMatrixPointByIndex(neighborIndex);
	    	    	        found = isMatrixPointInRect(mp.row, mp.col, mMovedObjectOriginBP.minMatrixPoint, mMovedObjectOriginBP.maxMatrixPoint);
	    	    	    }
	    	    	    if(found == false) {
	    	    	       	SEMountPointChain.MatrixPoint mp = mpc.getMatrixPointByIndex(neighborIndex);
	    	    	       	SEObjectBoundaryPoint currentMovedObjectBP = getOnMoveObject().getBoundaryPoint();
	    	    	       	found = isMatrixPointInRect(mp.row, mp.col, currentMovedObjectBP.minMatrixPoint, currentMovedObjectBP.maxMatrixPoint);
	    	    	    }
	    	    }
    	    }
    	    if(found == false) {
	    	    SEMountPointData retMPD = mpc.getMountPointData( neighborIndex);
	    	    SEMountPointChain.ClosestMountPointData closeData = new SEMountPointChain.ClosestMountPointData();
	    	    closeData.mIndex = neighborIndex;
	    	    closeData.mMPD = retMPD;
	    	    closeData.mDist = mpi.mountPointInfo.mMPD.getDistToPoint(neighborIndex);
	    	    return closeData;
    	    }
	    }
	    return null;
    }
    private SETransParas createUserTransParasFromWallTransform(int wallIndex, SEVector3f coordInVessel) {
    	    String wallName = this.getWallName(wallIndex);
	    SEVector3f wallLocalTranslate = new SEVector3f();
	    SERotate wallLocalRotate = new SERotate();
	    SEVector3f wallLocalScale = new SEVector3f();
	    getWallLocalRotateAndTranslateScale(wallName, wallLocalTranslate, wallLocalRotate, wallLocalScale);
	    SEVector3f wallSpaceCoord = coordInVessel.clone();
	    SEVector3f rotateV = SEObject.rotateMapPoint(wallLocalRotate, wallSpaceCoord);
	    rotateV = rotateV.add(wallLocalTranslate);
	    //wallLocalScale = new SEVector3f(mLineObjectWidth, 1, mLineObjectHeight);
	    SETransParas tp = new SETransParas(rotateV, wallLocalRotate, wallLocalScale);
	    return tp;
    }
    // p1, p2, p3, p4 is clockwise 
    //p1 is the bottom left point
    //p2 is the bottom right point
    //p3 is the top right point
    //p4 is the top left point
    private static class SEXZRect {
        //private SEVector3f p1, p2, p3, p4;
        public SEVector3f minPoint, maxPoint;
        public SEXZRect() {

        }
        public SEXZRect(SEVector3f minPoint , SEVector3f maxPoint) {
                this.minPoint = minPoint;
                this.maxPoint = maxPoint;
                //p1 = minPoint.clone();
                //p2 = new SEVector3f(maxPoint.getX(), maxPoint.getY(), minPoint.getZ());
                //p3 = maxPoint.clone();
                //p4 = new SEVector3f(minPoint.getX(), minPoint.getY(), maxPoint.getZ());
        }
        public boolean isIntersectFast(SEXZRect r2) {
            if (null == r2) return false;

            SEXZRect r1 = this;
            final float EX = 2.0f;
            boolean b1 = r2.minPoint.getX() > r1.maxPoint.getX() || Math.abs(r2.minPoint.getX() - r1.maxPoint.getX()) < EX;
            boolean b2 = r2.maxPoint.getX() < r1.minPoint.getX() || Math.abs(r1.minPoint.getX() - r2.maxPoint.getX()) < EX;
            boolean b3 = r2.minPoint.getZ() > r1.maxPoint.getZ() || Math.abs(r2.minPoint.getZ() - r1.maxPoint.getZ()) < EX;
            boolean b4 = r2.maxPoint.getZ() < r1.minPoint.getZ() || Math.abs(r2.maxPoint.getZ() - r1.minPoint.getZ()) < EX;
            boolean ret = b1 || b2 || b3 || b4;
            return !ret;
        }
    }
    private boolean isRectIntersect(SEVector3f minPoint1, SEVector3f maxPoint1, SEVector3f minPoint2, SEVector3f maxPoint2) {
        SEXZRect r1 = new SEXZRect(minPoint1, maxPoint1);
        SEXZRect r2 = new SEXZRect(minPoint2, maxPoint2);

        if(r1.isIntersectFast(r2)) {
            return true;
        } else {
            return false;
        }
    }
    /*
    private boolean isObjectIntersect(NormalObject object1, NormalObject object2) {
        SEObjectBoundaryPoint bp1 = object1.getBoundaryPoint();
        SEObjectBoundaryPoint bp2 = object2.getBoundaryPoint();
        if(bp1 == null && bp2 != null) {
                return false;
        }
        if(bp1 != null && bp2 == null) {
                return false;
        }
        if(bp1 == null && bp2 == null) {
                return false;
        }
        SEVector3f center1 = bp1.center;
        SEVector3f center2 = bp2.center;
        SEVector3f xyzSpan1 = bp1.xyzSpan;
        SEVector3f xyzSpan2 = bp2.xyzSpan;

        SEObjectBoundaryPoint minMaxPoint1 = SEObjectBoundaryPoint.getMinMaxPointInPlane(center1, xyzSpan1, bp1.movePlane);
        SEObjectBoundaryPoint minMaxPoint2 = SEObjectBoundaryPoint.getMinMaxPointInPlane(center2, xyzSpan2, bp2.movePlane);

        SEXZRect r1 = new SEXZRect(minMaxPoint1.minPoint, minMaxPoint1.maxPoint);
        SEXZRect r2 = new SEXZRect(minMaxPoint2.minPoint, minMaxPoint2.maxPoint);
        //Log.i(TAG, "minMaxPoint1 min = " + minMaxPoint1.minPoint + ", max = " + minMaxPoint1.maxPoint);
        //Log.i(TAG, "minMaxPoint2 min = " + minMaxPoint2.minPoint + ", max = " + minMaxPoint2.maxPoint);
	    if(r1.isIntersectFast(r2)) {
	    	return true;
	    } else {
	    	return false;
	    }
    }
    */
    private boolean isPointInRect(SEVector3f point , SEVector3f minPoint, SEVector3f maxPoint) {
    	    boolean b = point.getX() >= minPoint.getX() && point.getX() <= maxPoint.getX() && 
    	    		point.getZ() >= minPoint.getZ() && point.getZ() <= maxPoint.getZ();
    	    	return b;
    }
    private boolean isMatrixPointInRect(int row, int col, SEMountPointChain.MatrixPoint minPoint, 
    		                                                  SEMountPointChain.MatrixPoint maxPoint) {
    	if(row > minPoint.row || row  < maxPoint.row || col > maxPoint.col || col < minPoint.col) {
    		return false;
    	}
        return true;
        /*
	    for(int i = maxPoint.row ; i <= minPoint.row ; i++) {
	    	    for(int j = minPoint.col ; j <= maxPoint.col ; j++) {
	    	    	    if(i == row && col == j) {
	    	    	    	    return true;
	    	    	    }
	    	    }
	    }
	    return false;
	    */
    }
    private boolean isMatrixPointInObject(int row, int col, NormalObject object) {
	    SEObjectBoundaryPoint bp = object.getBoundaryPoint();
	    SEDebug.myAssert(bp != null, "object: " + object.mName + " matrix point bp = null error");
	    if(bp == null) {
	    	return false;
	    }
	    return isMatrixPointInRect(row, col, bp.minMatrixPoint, bp.maxMatrixPoint);
    }
    private boolean isObjectFixedInMountPoint(NormalObject obj) {
    	return obj instanceof AppObject;
    }
    private boolean isMatrixPointOverlap(SEObjectBoundaryPoint bp1, 
    		                             SEObjectBoundaryPoint bp2,
    		                             boolean compareWallIndex) {
	    SEMountPointChain.MatrixPoint minPoint1 = bp1.minMatrixPoint;
	    SEMountPointChain.MatrixPoint maxPoint1 = bp1.maxMatrixPoint;
	    SEMountPointChain.MatrixPoint minPoint2 = bp2.minMatrixPoint;
	    SEMountPointChain.MatrixPoint maxPoint2 = bp2.maxMatrixPoint;
	    if(bp1.wallIndex != bp2.wallIndex && compareWallIndex) {
	    	    return false;
	    }
	    if(bp1.minMatrixPoint.col > bp2.maxMatrixPoint.col || 
	       bp2.minMatrixPoint.col > bp1.maxMatrixPoint.col)
	    	return false;
	    if(bp1.maxMatrixPoint.row > bp2.minMatrixPoint.row ||
	       bp1.minMatrixPoint.row < bp2.maxMatrixPoint.row)
	    	return false;
	    return true;
    }
    private boolean isWallShelfOverlapOtherOtherObject(int wallIndex, SEObjectBoundaryPoint bp, WallShelf currentShelf) {
        ArrayList<NormalObject> currentWallNormalObjects = getNormalObjectsOnWall(wallIndex);
        for(NormalObject normalObject : currentWallNormalObjects) {
            if(!normalObject.isShelfObject()) {
                if(normalObject != currentShelf) {
                    SEObjectBoundaryPoint normalObjectBP = normalObject.getBoundaryPoint();
                    boolean b = isRectIntersect(normalObjectBP.minPoint, normalObjectBP.maxPoint, bp.minPoint, bp.maxPoint);//isMatrixPointOverlap(normalObjectBP, bp, true);
                    if(b) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    // bp is the boundary point need to compare with object's boundary point in wall
    private boolean isBoundaryPointOverlapObjectsInWall(SEMountPointChain chain, int wallIndex, SEObjectBoundaryPoint bp, boolean compareShelf) {
        ArrayList<NormalObject> currentWallNormalObjects = getNormalObjectsOnWall(wallIndex);
        for(NormalObject normalObject : currentWallNormalObjects) {
            SEObjectBoundaryPoint normalObjectBP = normalObject.getBoundaryPoint();
            if(normalObjectBP == null || normalObjectBP.minPoint == null) {
                Log.i(TAG, "bp == null");
            }
            if((normalObject instanceof  WallShelf) && !compareShelf) {
                continue;
            }
            boolean b = isRectIntersect(normalObjectBP.minPoint, normalObjectBP.maxPoint, bp.minPoint, bp.maxPoint);//isMatrixPointOverlap(normalObjectBP, bp, true);
            if(b) {
                return true;
            }
        }
        return false;
    }
    private boolean calculateBoundaryPointOverlapForAllObject(int wallIndex, SEObjectBoundaryPoint bp, NormalObject object) {
    	if(wallIndex != bp.wallIndex) {
    	    return false;
    	}
    	List<ConflictObject> objects = this.getExistentObject(wallIndex);
	    for(ConflictObject otherObject : objects) {
            if(otherObject.mConflictObject != object) {
                SEObjectBoundaryPoint otherObjectBP = otherObject.mConflictObject.getBoundaryPoint();
                if(otherObjectBP == null) {
                    return true;
                }
                boolean b = this.isMatrixPointOverlap(bp, otherObjectBP, true);
                if(b) {
                    return true;
                }
            }
	    }

	    NormalObject movedObject = getOnMoveObject();
	    boolean b = false;
	    if(movedObject.getBoundaryPoint() != null) {
	    	b = this.isMatrixPointOverlap(bp, movedObject.getBoundaryPoint(), true);
	    }
	    if(b) {
	    	return true;
	    }
	    return false;
    }
    private SEMountPointChain.MatrixPoint calculateObjectMatrixPointSize(SEMountPointChain chain, SEVector3f xyzSpan, int movePlane) {
        SEMountPointData mpd = chain.getMountPointData(0);
        SEVector3f mpdBound = mpd.mMaxPoint.subtract(mpd.mMinPoint);
        float xspan = xyzSpan.getX() / mpdBound.getX();
        float zspan = xyzSpan.getZ() / mpdBound.getZ();
        int colNum = (int)Math.round(xspan);
        int rowNum = (int)Math.round(zspan);
        if (colNum < 1) colNum = 1;
        if (rowNum < 1) colNum = 1;
        return new SEMountPointChain.MatrixPoint(rowNum, colNum, -1);
    }
    private SEMountPointChain.MatrixPoint calculateObjectMatrixPointSize(int wallIndex, SEVector3f xyzSpan, int movePlane) {
    	    SEMountPointChain currentChain = this.getCurrentMountPointChain(wallIndex);
    	    SEMountPointData mpd = currentChain.getMountPointData(0);
    	    SEVector3f mpdBound = mpd.mMaxPoint.subtract(mpd.mMinPoint);
    	    float xspan = xyzSpan.getX() / mpdBound.getX();
    	    float zspan = xyzSpan.getZ() / mpdBound.getZ();
    	    int colNum = (int)Math.ceil(xspan);
    	    int rowNum = (int)Math.ceil(zspan);
    	    return new SEMountPointChain.MatrixPoint(rowNum, colNum, -1);
    }
    private SEObjectBoundaryPoint calculateProperBoundaryPoint(int wallIndex, SEVector3f objectXYZSpan, NormalObject object) {
        //SEObjectBoundaryPoint movedObjectBP = movedObject.getBoundaryPoint();
        SEMountPointChain currentChain = this.getCurrentMountPointChain(wallIndex);
        SEObjectBoundaryPoint objectBP = object.getBoundaryPoint();
        SEMountPointChain.MatrixPoint objectBPSize = null;
        if(objectBP != null && objectBP.bpSize != null) {
            objectBPSize = objectBP.bpSize;
        } else {
            objectBPSize = this.calculateObjectMatrixPointSize(wallIndex, objectXYZSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ);
            if(objectBP != null) {
                objectBP.bpSize = objectBPSize;
            }
        }
        int rowNum = currentChain.getRowCount();
        int colNum = currentChain.getColCount();
        for(int i = (objectBPSize.row - 1) ; i < rowNum;  i++) {
            for(int j = 0 ; j < colNum ; j++) {
                int startRow = i;
                int startCol = j;
                int endRow = i - (objectBPSize.row - 1);
                int endCol = j + (objectBPSize.col - 1);
                if(startRow >= 0 && startRow < rowNum && startCol >= 0 && startCol < colNum &&
                        endRow >= 0 && endRow < rowNum && endCol >= 0 && endCol < colNum) {
                    SEObjectBoundaryPoint bp = new SEObjectBoundaryPoint(wallIndex);
                    bp.minMatrixPoint = new SEMountPointChain.MatrixPoint(startRow, startCol, -1);
                    bp.maxMatrixPoint = new SEMountPointChain.MatrixPoint(endRow, endCol, -1);
                    boolean b = this.calculateBoundaryPointOverlapForAllObject(wallIndex, bp, object);
                    if(b == false && ((objectBP == null) || (!bp.minMatrixPoint.equalRowCol(objectBP.minMatrixPoint) ||
                                      !bp.maxMatrixPoint.equalRowCol(objectBP.maxMatrixPoint)))) {
                        return bp;
                    }
                }
            }
        }
        return null;
    }
    private SEObjectBoundaryPoint createProperBoundaryPoint(int wallIndex, SEVector3f xyzSpan, NormalObject object) {
    	SEObjectBoundaryPoint properBP = this.calculateProperBoundaryPoint(wallIndex, xyzSpan, object);
    	SEMountPointChain currentChain = this.getCurrentMountPointChain(wallIndex);
	    if(properBP != null) {
	    	    int minMountPointIndex = currentChain.getIndex(properBP.minMatrixPoint.row, properBP.minMatrixPoint.col);
	    	    properBP.minMatrixPoint.mountPointIndex = minMountPointIndex;
	    	    int maxMountPointIndex = currentChain.getIndex(properBP.maxMatrixPoint.row, properBP.maxMatrixPoint.col);
	    	    properBP.maxMatrixPoint.mountPointIndex = maxMountPointIndex;
	    	    SEMountPointData minMPD = currentChain.getMountPointData(minMountPointIndex);
	    	    SEMountPointData maxMPD = currentChain.getMountPointData(maxMountPointIndex);
	    	    SEVector3f center = minMPD.getTranslate().add(maxMPD.getTranslate()).mul(0.5f);
	    	    SEObjectBoundaryPoint tmpBP = SEObjectBoundaryPoint.getMinMaxPointInPlane(center, xyzSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ, SEObjectBoundaryPoint.CENTER_POINT_STYLE_MID_MID);
	    	    properBP.minPoint = tmpBP.minPoint;
	    	    properBP.maxPoint = tmpBP.maxPoint;
	    	    properBP.center = center;
	    	    properBP.xyzSpan = xyzSpan;
	    	    properBP.movePlane = SEObjectBoundaryPoint.MOVE_PLANE_XZ;
	    	    properBP.wallIndex = wallIndex;
	    	    object.setBoundaryPoint(properBP);
	    	    return properBP;
	    }
	    return null;
    }

    private void updateObjectTransParasByBoundaryPoint(int wallIndex, NormalObject object) {
    	    SEObjectBoundaryPoint bp = object.getBoundaryPoint();
    	    SEVector3f center = bp.center;
    	    //Log.i(TAG, " update obj trans = " + center);
    	    SETransParas trans = this.createUserTransParasFromWallTransform(wallIndex, bp.center);
    	    object.getUserTransParas().mTranslate = trans.mTranslate;
    	    object.setUserTransParas();
    }
    /////////
    private static class UpdateObjectBoundaryPointResult {
        public boolean updateOK;
        public SEMountPointChain.ClosestMountPointData resultCMD;
        public SEObjectBoundaryPoint resultBoundaryPoint;
        public NormalObject needMoveObject;
        UpdateObjectBoundaryPointResult() {
            updateOK = false;
        }
    }
    private UpdateObjectBoundaryPointResult updateAppObjectAndAppObjectBoundaryPoint(MountPointInfo mpi, NormalObject movedObject,
    		                                                     NormalObject needMoveObject,
    		                                                  SEObjectBoundaryPoint newMovedObjectBP) {
	    //myAssert(mpi.conflictObject != null, "conflict object is null");
	    SEDebug.myAssert(movedObject instanceof AppObject, "movedObject is not AppObject");
	    SEMountPointChain.ClosestMountPointData cmp = getClosestMountPointToMPI(needMoveObject.mName, mpi);
        UpdateObjectBoundaryPointResult result = new UpdateObjectBoundaryPointResult();
	    if(cmp != null) {
            result.updateOK = true;
            result.resultCMD = cmp;
            result.needMoveObject = needMoveObject;
            return result;

	    } else {
	    	result.updateOK = false;
	    }
        return result;
    }
    private SEVector3f mOriginPlaceInWall;
    private SEVector3f mOriginScale;
    private int mCurrentShelfPressPosition;
    private void saveMovedObjectBoundary(MountPointInfo mpi) {
	    NormalObject currentObject = getOnMoveObject();
	    mMovedObjectOriginBP = getOnMoveObject().getBoundaryPoint();
        mOriginPlaceInWall = mRealLocationInWall.clone();
        mOriginScale = currentObject.getUserTransParas().mScale.clone();
        if(currentObject instanceof  WallShelf) {
            mShelfMoveBP = currentObject.getBoundaryPoint().clone();
        }
    }
    private boolean isObjectFixedOnYAxis(NormalObject obj) {
    	return obj instanceof AppObject || obj instanceof WidgetObject;
    }
    private UpdateObjectBoundaryPointResult updateAppObjectAndNativeObjectBoundaryPoint(MountPointInfo mpi, NormalObject movedObject,
        NormalObject nativeObject, SEObjectBoundaryPoint newMovedObjectBP) {
        SEDebug.myAssert(movedObject instanceof AppObject, "movedObject is not AppObject");
        SEDebug.myAssert(mpi.conflictObject == null, "conflict app object must be null");
        SEObjectBoundaryPoint nativeObjectBP = nativeObject.getBoundaryPoint();
        UpdateObjectBoundaryPointResult result = new UpdateObjectBoundaryPointResult();
        if(nativeObjectBP == null) {
            result.updateOK = false;
            return result;
        }
        SEVector3f currentAppObjCenter = newMovedObjectBP.center;
        if(!isPointInRect(currentAppObjCenter, nativeObjectBP.minPoint, nativeObjectBP.maxPoint)) {
            result.updateOK = false;
            return result;
        }
        //SEMountPointChain.MatrixPoint matrixPointSize = this.calculateObjectMatrixPointSize(mpi.wallIndex, nativeObjectBP.xyzSpan, MOVE_PLANE_XZ);
        SEObjectBoundaryPoint properBP = this.calculateProperBoundaryPoint(mpi.wallIndex, nativeObjectBP.xyzSpan, nativeObject);
        if(properBP == null) {
            result.updateOK = false;
            return result;
        }
        result.updateOK = true;
        result.needMoveObject = nativeObject;
        result.resultBoundaryPoint = properBP;
        return result;
    }
    private UpdateObjectBoundaryPointResult updateNativeObjectAndAppObjectBoundaryPoint(MountPointInfo mpi, NormalObject movedNativeObject,
        NormalObject appObject, SEObjectBoundaryPoint newMovedObjectBP) {
        SEDebug.myAssert(movedNativeObject.getObjectInfo().mIsNativeObject, "object is not native");
        SEMountPointChain.ClosestMountPointData cpd = this.getAppObjectClosestMountPoint(mpi.wallIndex, appObject);
        UpdateObjectBoundaryPointResult result = new UpdateObjectBoundaryPointResult();
        if(cpd == null) {
            result.updateOK = false;
            return result;
        }
        result.resultCMD = cpd;
        result.needMoveObject = appObject;
        result.updateOK = true;
        return result;
    }
    private UpdateObjectBoundaryPointResult updateNativeObjectAndNativeObjectBoundaryPoint(MountPointInfo mpi, NormalObject movedNativeObject,
    		                                                           NormalObject nativeObject, SEObjectBoundaryPoint newMovedObjectBP) {
	    SEObjectBoundaryPoint nativeObjectBP = nativeObject.getBoundaryPoint();
	    SEObjectBoundaryPoint properBP = this.calculateProperBoundaryPoint(mpi.wallIndex, nativeObjectBP.xyzSpan, nativeObject);
	    UpdateObjectBoundaryPointResult result = new UpdateObjectBoundaryPointResult();
        if(properBP == null) {
            result.updateOK = false;
            return result;
	    }
        result.updateOK = true;
        result.needMoveObject = nativeObject;
        result.resultBoundaryPoint = properBP;
        return result;
    }
    ///////////
    private boolean isAppObject(NormalObject object) {
    	return object instanceof AppObject;
    }

    private boolean isObjectCenterInOtherObject(NormalObject srcObject, NormalObject dstObject) {
        SEObjectBoundaryPoint srcObjectBP = srcObject.getBoundaryPoint();
        SEObjectBoundaryPoint dstObjectBP = dstObject.getBoundaryPoint();
        return isPointInRect(srcObjectBP.center, dstObjectBP.minPoint, dstObjectBP.maxPoint);
    }

    private boolean isMountPointInObject(NormalObject testObject, int wallIndex, int mountPointIndex) {
        SEMountPointChain currentChain = getCurrentMountPointChain(wallIndex);
        SEMountPointChain.MatrixPoint mp = currentChain.getMatrixPointByIndex(mountPointIndex);
        return isMatrixPointInObject(mp.row, mp.col, testObject);
    }
    private void printObjetsInWall(int wallIndex) {
		List<ConflictObject> tmpObjects = this.getExistentObject(wallIndex);
	    for(ConflictObject object: tmpObjects) {
	    	    NormalObject tmpObject =  object.mConflictObject;
	    	    Log.i(TAG, "wall object = " + tmpObject.mName + " ###");
	    }
    }
    /*
    private boolean isObjectStatic(NormalObject object) {
    	return object.getObjectInfo().mType.equals(ModelInfo.Type.WALL_SHELF);
    }
    private void changeNeedMoveObjectBoundaryPoint(ArrayList<UpdateObjectBoundaryPointResult> updateBPResults, MountPointInfo mpi) {
        for(UpdateObjectBoundaryPointResult result : updateBPResults) {
            NormalObject needMoveObject = result.needMoveObject;
            if(isObjectFixedInMountPoint(needMoveObject)) {
                SEMountPointChain.ClosestMountPointData cmp = result.resultCMD;
                SEVector3f realPointInWall = mRealLocationInWall.clone();
                SEVector3f srcMountPoint = mpi.mountPointInfo.mMPD.getTranslate();
                SEVector3f dstMountPoint = cmp.mMPD.getTranslate();
                srcMountPoint.mD[1] = dstMountPoint.mD[1] = realPointInWall.mD[1];
                //SETransParas srcUserTransParas = this.createUserTransParasFromWallTransform(mpi.wallIndex, srcMountPoint);
                SETransParas dstUserTransParas = this.createUserTransParasFromWallTransform(mpi.wallIndex, dstMountPoint);

                needMoveObject.getUserTransParas().mTranslate = dstUserTransParas.mTranslate;
                needMoveObject.setUserTransParas();
                needMoveObject.getObjectSlot().mSlotIndex = mpi.wallIndex;
                needMoveObject.getObjectSlot().mMountPointIndex = cmp.mIndex;
                this.setAppObjectBoundaryPoint(needMoveObject, mpi.wallIndex, cmp.mIndex);
                needMoveObject.updateSlotDB();
            } else {
                SEObjectBoundaryPoint nativeObjectBP = needMoveObject.getBoundaryPoint();
                SEObjectBoundaryPoint properBP = result.resultBoundaryPoint;
                SEVector3f objectXYZSpan = nativeObjectBP.xyzSpan;
                SEMountPointChain currentChain = this.getCurrentMountPointChain(mpi.wallIndex);
                int minMountPointIndex = currentChain.getIndex(properBP.minMatrixPoint.row, properBP.minMatrixPoint.col);
                properBP.minMatrixPoint.mountPointIndex = minMountPointIndex;
                int maxMountPointIndex = currentChain.getIndex(properBP.maxMatrixPoint.row, properBP.maxMatrixPoint.col);
                properBP.maxMatrixPoint.mountPointIndex = maxMountPointIndex;
                SEMountPointData minMPD = currentChain.getMountPointData(minMountPointIndex);
                SEMountPointData maxMPD = currentChain.getMountPointData(maxMountPointIndex);
                SEVector3f center = minMPD.getTranslate().add(maxMPD.getTranslate()).mul(0.5f);
                SEObjectBoundaryPoint tmpBP = SEObjectBoundaryPoint.getMinMaxPointInPlane(center, objectXYZSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ, SEObjectBoundaryPoint.CENTER_POINT_STYLE_MID_MID);

                properBP.minPoint = tmpBP.minPoint;
                properBP.maxPoint = tmpBP.maxPoint;
                properBP.center = center;
                properBP.xyzSpan = objectXYZSpan;
                properBP.movePlane = SEObjectBoundaryPoint.MOVE_PLANE_XZ;
                properBP.wallIndex = mpi.wallIndex;
                needMoveObject.setBoundaryPoint(properBP);
                updateObjectTransParasByBoundaryPoint(mpi.wallIndex, needMoveObject);
                needMoveObject.updateSlotDB();
            }
        }
    }
    */
/*
	private void updateMoveObjectBoundary(MountPointInfo mpi) {
		if(mpi == null) {
			 return;
		}
        this.printObjetsInWall(mpi.wallIndex);
		NormalObject movedObject = (NormalObject)getOnMoveObject();
		SEObjectBoundaryPoint objectBoundaryPoint = this.getObjectBoundingVolumeInWall(mpi.wallIndex, movedObject, SEObjectBoundaryPoint.MOVE_PLANE_XZ);
		SEMountPointChain.MatrixPoint minMatrixPoint = objectBoundaryPoint.minMatrixPoint;
		SEMountPointChain.MatrixPoint maxMatrixPoint = objectBoundaryPoint.maxMatrixPoint;
		SEObjectBoundaryPoint oldMovedObjectBP = movedObject.getBoundaryPoint();
		movedObject.setBoundaryPoint(objectBoundaryPoint);
		
		
        ArrayList<NormalObject> needMoveObjectList = new ArrayList<NormalObject>();
        ArrayList<NormalObject> intersectObjectList = new ArrayList<NormalObject>();
	    List<ConflictObject> objects = this.getExistentObject(mpi.wallIndex);
	    for(ConflictObject object: objects) {
    	    NormalObject tmpObject =  object.mConflictObject;
    	    if(isObjectIntersect(movedObject, tmpObject)) {
    	    	//Log.i(TAG, "intersect object = " + tmpObject.mName);
	    	    intersectObjectList.add(tmpObject);
	    	    if(isObjectFixedInMountPoint(movedObject) && isObjectFixedInMountPoint(tmpObject)) {
    	    	    if(isNearestAppObjectSlotInObject(movedObject, tmpObject, mpi)) {
    	    	        	needMoveObjectList.add(tmpObject);
    	    	    }
	    	    } else if(isObjectFixedInMountPoint(movedObject) && !isObjectFixedInMountPoint(tmpObject)) {
    	    	    if(isNearestAppObjectSlotInObject(movedObject, tmpObject, mpi) && isObjectStatic(tmpObject) == false) {
    	    	    	    needMoveObjectList.add(tmpObject);
    	    	    }
	    	    } else if(!isObjectFixedInMountPoint(movedObject) && isObjectFixedInMountPoint(tmpObject)) {
    	    	    if(isObjectCenterInOtherObject(tmpObject, movedObject)) {
    	    	    	    needMoveObjectList.add(tmpObject);
    	    	    }
	    	    } else if(!isObjectFixedInMountPoint(movedObject) && !isObjectFixedInMountPoint(tmpObject)) {
    	    	    if(isObjectStatic(tmpObject) == false) {
    	    	        needMoveObjectList.add(tmpObject);
    	    	    }
	    	    }
    	    }
	    }
	    if(intersectObjectList.size() == 0) {
	    	mHasIntersectObject = false;
	    } else {
	        mHasIntersectObject = true;
	        mAllIntersectObjectMoveOK = false;
	    }
	    if(needMoveObjectList.size() == 0) {
	    	return;
	    }
        if(intersectObjectList.size() != needMoveObjectList.size()) {
            return;
        }
	    boolean moveOk = true;
        ArrayList<UpdateObjectBoundaryPointResult> updateBPResults = new ArrayList<UpdateObjectBoundaryPointResult>();
	    for(int i = 0 ; i < needMoveObjectList.size() ; i++) {
    	    NormalObject needMoveObject = needMoveObjectList.get(i);
            UpdateObjectBoundaryPointResult result = null;
    	    if(isObjectFixedInMountPoint(movedObject) && isObjectFixedInMountPoint(needMoveObject)) {
    	    	result = this.updateAppObjectAndAppObjectBoundaryPoint(mpi, movedObject, needMoveObject,objectBoundaryPoint);

    	    } else if(isObjectFixedInMountPoint(movedObject) && !isObjectFixedInMountPoint(needMoveObject)) {
    	    	result = this.updateAppObjectAndNativeObjectBoundaryPoint(mpi, movedObject, needMoveObject, objectBoundaryPoint);
    	    } else if(!isObjectFixedInMountPoint(movedObject) && isObjectFixedInMountPoint(needMoveObject)) {
    	    	result = this.updateNativeObjectAndAppObjectBoundaryPoint(mpi, movedObject, needMoveObject, objectBoundaryPoint);
    	    } else if(!isObjectFixedInMountPoint(movedObject) && !isObjectFixedInMountPoint(needMoveObject)) {
    	    	result = this.updateNativeObjectAndNativeObjectBoundaryPoint(mpi, movedObject, needMoveObject, objectBoundaryPoint);
    	    }
            if(result != null) {
                updateBPResults.add(result);
            }
 	    }
        for(UpdateObjectBoundaryPointResult result : updateBPResults) {
            if(result.updateOK == false) {
                moveOk = false;
                break;
            }
        }
	    if(moveOk == false) {
	    	mAllIntersectObjectMoveOK = false;
	    } else {
            changeNeedMoveObjectBoundaryPoint(updateBPResults, mpi);
            mAllIntersectObjectMoveOK = true;
	    }
		
	}
	*/
    private boolean playConflictAnimation(MountPointInfo mpi) {
    	    if(mpi == null) {
    	    	    return false;
    	    }
    	    if(mpi.conflictObject == null) {
    	    	    return false;
    	    }
    	    int wallIndex = mpi.wallIndex;
    	    SEVector3f realPointInWall = mRealLocationInWall.clone();//calculateRealPointInWall(wallIndex);
    	    SEMountPointChain.ClosestMountPointData cmp = getClosestMountPointToMPI(getOnMoveObject().mName, mpi);
    	    SEVector3f srcMountPoint = mpi.mountPointInfo.mMPD.getTranslate();
    	    SEVector3f dstMountPoint = cmp.mMPD.getTranslate();
    	    srcMountPoint.mD[1] = dstMountPoint.mD[1] = realPointInWall.mD[1];
    	    SETransParas srcUserTransParas = this.createUserTransParasFromWallTransform(wallIndex, srcMountPoint);
    	    SETransParas dstUserTransParas = this.createUserTransParasFromWallTransform(wallIndex, dstMountPoint);

    	    mpi.conflictObject.mConflictObject.getUserTransParas().mTranslate = dstUserTransParas.mTranslate;
    	    mpi.conflictObject.mConflictObject.setUserTransParas();
    	    mpi.conflictObject.mConflictObject.getObjectSlot().mSlotIndex = mpi.wallIndex;
    	    mpi.conflictObject.mConflictObject.getObjectSlot().mMountPointIndex = cmp.mIndex;
    	    return true;
    }
    //public static final int MOVE_PLANE_INVALID = 0;
    //public static final int MOVE_PLANE_XZ = 1;
    //public static final int MOVE_PLANE_XY = 2;
    //public static final int MOVE_PLANE_YZ = 3;
    private SEVector3f getXYZSpanFromMinMaxPoint(SEVector3f minPoint, SEVector3f maxPoint) {
		float xspan = Math.abs(maxPoint.getX() - minPoint.getX());
		float yspan = Math.abs(maxPoint.getY() - minPoint.getY());
		float zspan = Math.abs(maxPoint.getZ() - minPoint.getZ());
		return new SEVector3f(xspan, yspan, zspan);
    }
    private SEVector3f getNativeObjectBottom(NormalObject object) {
        SEObjectBoundaryPoint bp = object.getBoundaryPoint();
        SEVector3f xyzSpan = null;
        if(bp != null) {
            xyzSpan = bp.xyzSpan;
        }
        if(xyzSpan != null && !xyzSpan.equals(SEVector3f.ZERO)) {
            return xyzSpan;
        }
        object.createLocalBoundingVolume();
        SEVector3f objectMinPoint = new SEVector3f();
        SEVector3f objectMaxPoint = new SEVector3f();
        object.getLocalBoundingVolume(objectMinPoint, objectMaxPoint);
        return new SEVector3f(Math.abs(objectMinPoint.getX()), Math.abs(objectMaxPoint.getY()), Math.abs(objectMinPoint.getZ()));
    }
    /*
    private SEVector3f getObjectOriginXYZSpan(NormalObject object) {
        object.createLocalBoundingVolume();
        SEVector3f objectMinPoint = new SEVector3f();
        SEVector3f objectMaxPoint = new SEVector3f();
        object.getLocalBoundingVolume(objectMinPoint, objectMaxPoint);
        SEVector3f xyzSpan = getXYZSpanFromMinMaxPoint(objectMinPoint, objectMaxPoint);
        return xyzSpan;
    }
    private SEVector3f getObjectXYZSpan(NormalObject object) {
    	SEObjectBoundaryPoint bp = object.getBoundaryPoint();
        SEVector3f xyzSpan = null;
        if(bp != null) {
            xyzSpan = bp.xyzSpan;
        }
        if(xyzSpan != null && !xyzSpan.equals(SEVector3f.ZERO)) {
            return xyzSpan;
        }
        object.createLocalBoundingVolume();
		SEVector3f objectMinPoint = new SEVector3f();
		SEVector3f objectMaxPoint = new SEVector3f();
		object.getLocalBoundingVolume(objectMinPoint, objectMaxPoint);
        xyzSpan = getXYZSpanFromMinMaxPoint(objectMinPoint, objectMaxPoint);
        return xyzSpan;
    }
    */
    //
    /*
    private SEObjectBoundaryPoint getObjectBoundingVolumeInWall(int wallIndex, NormalObject object, int movePlane) {
		SEVector3f realLocationInWallSpace = mRealLocationInWall.clone();//calculateRealPointInWall(wallIndex);
        SEVector3f xyzSpan = object.getObjectXYZSpan();
        SEMountPointChain currentChain = this.getCurrentMountPointChain(wallIndex);
        SEObjectBoundaryPoint bp = SEObjectBoundaryPoint.getMinMaxPointInPlane(realLocationInWallSpace, xyzSpan, movePlane, SEObjectBoundaryPoint.CENTER_POINT_STYLE_MID_MID);
	    SEMountPointChain.MatrixPoint minMatrixPoint = null;
	    SEMountPointChain.MatrixPoint maxMatrixPoint = null;
	    minMatrixPoint = currentChain.getMatrixPointInPlaneXZ(bp.minPoint);
		maxMatrixPoint = currentChain.getMatrixPointInPlaneXZ(bp.maxPoint);
		bp.wallIndex = wallIndex;
        bp.center = realLocationInWallSpace;
        bp.xyzSpan = xyzSpan;
        bp.movePlane = SEObjectBoundaryPoint.MOVE_PLANE_XZ;
        bp.minMatrixPoint = minMatrixPoint;
        bp.maxMatrixPoint = maxMatrixPoint;
        return bp;
        
    }
    */
    /*
    private SEMountPointChain.BoundaryPoint createTestBoundaryPoint() {
    	    SEMountPointChain.BoundaryPoint bp = new SEMountPointChain.BoundaryPoint();
    	    bp.startPoint = new SEMountPointChain.MatrixPoint(0, 0, 0);
    	    bp.endPoint = new SEMountPointChain.MatrixPoint(1, 0, 4);
    	    return bp;
    }
    */
    private void setCoordInWallSpace(int wallIndex) {
    	    mRealLocationInWall = this.calculateRealPointInWall(wallIndex);
    	    SEVector3f v = this.mObjectTransParas.mTranslate;
    	    mObjectTransParasTranslateInWall = this.toWallSpaceCoordinate(wallIndex, v);
    }
    private void createObjectBoundaryPoint(int wallIndex) {
    	SEMountPointChain currentChain = this.getCurrentMountPointChain(wallIndex);
      	for (SEObject child : mHouse.mChildObjects) {
            if (child instanceof NormalObject == false) {
              	continue;
            }
            NormalObject normalObj = (NormalObject)child;
            if(normalObj instanceof WallShelf) {
            	continue;
            }
            if(this.isObjectOnWall(normalObj) == false || normalObj.getObjectSlot().mSlotIndex != wallIndex) {
            	    continue;
            }
            SEObjectBoundaryPoint bp = normalObj.getBoundaryPoint();
            if(bp != null && bp.xyzSpan.equals(SEVector3f.ZERO) == false) {
            	    continue;
            }
            if(isObjectFixedInMountPoint(normalObj)) {
            	this.setAppObjectBoundaryPoint(normalObj, wallIndex, normalObj.getObjectSlot().mMountPointIndex);
            } else {
                ObjectSlot slotInfo = normalObj.getObjectSlot();
                SEObjectBoundaryPoint savedBP = slotInfo.mBoundaryPoint;
                if(savedBP != null && savedBP.xyzSpan.equals(SEVector3f.ZERO) == false) {
                    normalObj.setBoundaryPoint(savedBP.clone());
                } else {
                    int rowIndex = slotInfo.mStartY;
                    int colIndex = slotInfo.mStartX;
                    int colNum = currentChain.getColCount();
                    int index = colIndex + rowIndex * colNum;
                    SEMountPointData mpd = currentChain.getMountPointData(index);
                    normalObj.createLocalBoundingVolume();
                    SEVector3f bvMinPoint = new SEVector3f();
                    SEVector3f bvMaxPoint = new SEVector3f();
                    normalObj.getLocalBoundingVolume(bvMinPoint, bvMaxPoint);
                    SEVector3f xyzSpan = getXYZSpanFromMinMaxPoint(bvMinPoint, bvMaxPoint);
                    SEObjectBoundaryPoint newBP = SEObjectBoundaryPoint.getMinMaxPointInPlane(mpd.getTranslate(), xyzSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ, SEObjectBoundaryPoint.CENTER_POINT_STYLE_MID_MID);
                    SEMountPointChain.MatrixPoint minMatrixPoint = currentChain.getMatrixPointInPlaneXZ(newBP.minPoint);
                    SEMountPointChain.MatrixPoint maxMatrixPoint = currentChain.getMatrixPointInPlaneXZ(newBP.maxPoint);
                    newBP.minMatrixPoint = minMatrixPoint;
                    newBP.maxMatrixPoint = maxMatrixPoint;
                    newBP.center = mpd.getTranslate().clone();
                    newBP.xyzSpan = xyzSpan;
                    newBP.movePlane = SEObjectBoundaryPoint.MOVE_PLANE_XZ;
                    newBP.wallIndex = wallIndex;
                    normalObj.setBoundaryPoint(newBP);
                }
            }
     	}
    }
    public static SEVector3f getLocationYInWall(NormalObject object, int wallIndex) {
        /*
        if(mRealLocationInWall != null) {
            return mRealLocationInWall;
        }
        if(mLocationInWallForWidget != null) {
            return mLocationInWallForWidget;
        }
        SEVector3f t = getFingerOnWallLocation(100, 100);
        SEVector3f coordInWall = this.toWallSpaceCoordinate(wallIndex, t);
        mLocationInWallForWidget = coordInWall;
        return mLocationInWallForWidget;
        */

        final boolean isNative = null == object || null == object.getObjectInfo() ? false : object.getObjectInfo().mIsNativeObject;
        if (isNative && isScreenOrientationPortrait()) {
            return new SEVector3f(0, -124, 0);
        }

        return  new SEVector3f(0, -70, 0);
    }
    private SEObjectBoundaryPoint createAppObjectBP(SEMountPointChain chain, NormalObject object,
                                                    int wallIndex, int mountPointIndex, SEVector3f deltaV) {
        SEMountPointData mpd = chain.getMountPointData(mountPointIndex);
        SEVector3f xyzSpan = object.getObjectXYZSpan();
        SEObjectBoundaryPoint bp = SEObjectBoundaryPoint.getMinMaxPointInPlane(mpd.getTranslate(), xyzSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ, SEObjectBoundaryPoint.CENTER_POINT_STYLE_BOTTOM_MID);
        bp.center = mpd.getTranslate().add(deltaV);
        SEMountPointChain.MatrixPoint mp = chain.getMatrixPointByIndex(mountPointIndex);
        SEMountPointChain.MatrixPoint mp1 = mp.clone();
        bp.minMatrixPoint = mp;
        bp.maxMatrixPoint = mp1;
        bp.movePlane = SEObjectBoundaryPoint.MOVE_PLANE_XZ;
        bp.xyzSpan = xyzSpan;
        bp.wallIndex = wallIndex;
        return bp;
    }
    private SEObjectBoundaryPoint createObjectBP(SEMountPointChain chain, NormalObject object,
                                                 int wallIndex, SEVector3f center) {
        SEVector3f xyzSpan = object.getObjectXYZSpan();
        SEObjectBoundaryPoint newBP = SEObjectBoundaryPoint.getMinMaxPointInPlane(center, xyzSpan, SEObjectBoundaryPoint.MOVE_PLANE_XZ, SEObjectBoundaryPoint.CENTER_POINT_STYLE_MID_MID);
        SEMountPointChain.MatrixPoint minMatrixPoint = chain.getMatrixPointInPlaneXZ(newBP.minPoint);
        SEMountPointChain.MatrixPoint maxMatrixPoint = chain.getMatrixPointInPlaneXZ(newBP.maxPoint);
        newBP.minMatrixPoint = minMatrixPoint;
        newBP.maxMatrixPoint = maxMatrixPoint;
        newBP.center = center;
        newBP.xyzSpan = xyzSpan;
        newBP.movePlane = SEObjectBoundaryPoint.MOVE_PLANE_XZ;
        newBP.wallIndex = wallIndex;
        return newBP;
    }
    private void createShelfBoundaryPoint(int wallIndex) {
    	SEMountPointChain currentChain = this.getCurrentMountPointChain(wallIndex);
        for (SEObject child : mHouse.mChildObjects) {
            if (child instanceof WallShelf) {
                WallShelf vesselShelf = (WallShelf) child;
                SEObjectBoundaryPoint bp = vesselShelf.getBoundaryPoint();
                if((bp != null && bp.xyzSpan.equals(SEVector3f.ZERO) == false) ||
                		(vesselShelf.getObjectSlot().mSlotIndex != wallIndex)) {
                	    continue;
                }
                bp = new SEObjectBoundaryPoint(wallIndex);
                SEVector3f worldMinPoint = new SEVector3f();
                SEVector3f worldMaxPoint = new SEVector3f();
                vesselShelf.getBoundaryPointInWorldSpace(worldMinPoint, worldMaxPoint);
                SEVector3f pointInWallMin = this.toWallSpaceCoordinate(wallIndex, worldMinPoint);
                SEVector3f pointInWallMax = this.toWallSpaceCoordinate(wallIndex, worldMaxPoint);
                SEVector3f center = pointInWallMax.add(pointInWallMin).mul(0.5f);
                SEMountPointChain.MatrixPoint minMatrixPoint = currentChain.getMatrixPointInPlaneXZ(pointInWallMin);
                SEMountPointChain.MatrixPoint maxMatrixPoint = currentChain.getMatrixPointInPlaneXZ(pointInWallMax);
                
                bp.center = center;
                bp.minPoint = pointInWallMin;
                bp.maxPoint = pointInWallMax;
                bp.minMatrixPoint = minMatrixPoint;
                bp.maxMatrixPoint = maxMatrixPoint;
                bp.xyzSpan = pointInWallMax.subtract(pointInWallMin);
                bp.movePlane = SEObjectBoundaryPoint.MOVE_PLANE_XZ;
                bp.wallIndex = wallIndex;
                vesselShelf.setBoundaryPoint(bp);
            }
        }
    }
    private boolean isWallHasShelf() {
    	int currentWallIndex = mHouse.getWallNearestIndex();
    	return currentWallIndex == 0;
    }
    private void determineShelf() {
       	VesselLayer currentLayer = null;
       	if(isWallHasShelf() == false) {
       		return;
       	}
        for (SEObject child : mHouse.mChildObjects) {
            if (child instanceof WallShelf) {
                WallShelf vessel = (WallShelf) child;
                if (vessel.getVesselLayer().canHandleSlot(getOnMoveObject())) {
                	if(vessel.canHandleSlot(getOnMoveObject(), mRealLocation)) {
                		//Log.i(TAG, "## shelf " + vessel.mName + " " + vessel.mIndex + " contain object ####");
                		currentLayer = vessel.getVesselLayer();
                        currentLayer.setOnLayerModel(getOnMoveObject(), true);
                		break;
                	}
                    //mCurrentLayer = vessel.getVesselLayer();
                    //mCurrentLayer.setOnLayerModel(getOnMoveObject(), true);
                    
                }
            }
        }
        if(currentLayer == null) {
        	    if(mCurrentLayer != null && mCurrentLayer instanceof WallShelfLayer) {
        	    	    mCurrentLayer = currentLayer;
        	    }
        } else {
        	    if(mCurrentLayer != null && (mCurrentLayer instanceof WallShelfLayer) == false) {
        	    	    disableCurrentLayer();
        	    }
        	    mCurrentLayer = currentLayer;
        }
    }
    private void hideAllLineObject() {
    	if(HomeUtils.DEBUG) {
   	        showLine_new(null, false);
            showArea(mHouse.getWallNearestIndex(), false);
    	}
    }
    private void setCurrentObjectScaleToGlobalTransparas() {
        SEVector3f s = getOnMoveObject().getUserTransParas().mScale;
        mObjectTransParas.mScale = s.clone();
    }
    private SEObjectBoundaryPoint mShelfMoveBP;
    private final int CLICK_SHELF_LEFT = 0;
    private final int CLICK_SHELF_RIGHT = 1;
    private final int CLICK_SHELF_MID = 2;
    private int calculateShelfClickPosition(float left, float right, float currentPosition, float shelfWidth) {
        float leftEnd = left + shelfWidth / 3;
        float midEnd = leftEnd + shelfWidth / 3;
        if(currentPosition >= left && currentPosition <= leftEnd) {
            return CLICK_SHELF_LEFT;
        } else if(currentPosition > leftEnd && currentPosition <= midEnd) {
            return CLICK_SHELF_MID;
        } else {
            return CLICK_SHELF_RIGHT;
        }
    }
    private void saveCurrentShelfClickPosition(WallShelf shelf) {
        SEVector3f originCenter = mOriginPlaceInWall;
        SEVector3f realSpan = shelf.getBoundaryPoint().xyzSpan;
        float originLeft = mMovedObjectOriginBP.center.mD[0] - realSpan.mD[0] / 2;
        float originRight = mMovedObjectOriginBP.center.mD[0] + realSpan.mD[0] / 2;
        int currentClickPosition = calculateShelfClickPosition(originLeft, originRight, originCenter.mD[0], realSpan.mD[0]);
        mCurrentShelfPressPosition = currentClickPosition;
    }
    private SEVector2f getShelfRowLeftRightMostPosition(SEMountPointChain chain, WallShelf shelf) {
        int shelfRow = shelf.getRowIndexInWall();
        MatrixPoint mp = new MatrixPoint();
        mp.row = shelfRow;
        int colNum = chain.getColCount();
        mp.col = colNum - 1;
        int rightIndex = chain.getIndex(mp.row, mp.col);
        SEMountPointData rightMPD = chain.getMountPointData(rightIndex);
        float width = rightMPD.mMaxPoint.subtract(rightMPD.mMinPoint).mD[0];
        float right = rightMPD.getTranslate().mD[0] + width;
        mp.col = 0;
        int leftIndex = chain.getIndex(mp.row, mp.col);
        SEMountPointData leftMPD = chain.getMountPointData(leftIndex);
        float left = leftMPD.getTranslate().mD[0] - width;
        return new SEVector2f(left, right);
    }
    private SEVector2f getShelfScopeXHasObject(SEMountPointChain chain, WallShelf shelf) {
        int leftMountPoint = shelf.getFirstMountPointHasObject();
        int rightMountPoint = shelf.getLastMountPointHasObject();
        //SEDebug.myAssert(leftMountPoint != -1 , "left mount point is -1 error");
        //SEDebug.myAssert(rightMountPoint != -1, "right mount point is -1 error");
        if(leftMountPoint != -1 && rightMountPoint != -1) {
            SEMountPointData leftMPD = chain.getMountPointData(leftMountPoint);
            SEMountPointData rightMPD = chain.getMountPointData(rightMountPoint);
            float width = leftMPD.mMaxPoint.subtract(leftMPD.mMinPoint).mD[0];
            float leftMostX = leftMPD.getTranslate().mD[0] - width / 2 - 10;
            float rightMostX = rightMPD.getTranslate().mD[0] + width / 2 + 10;
            return new SEVector2f(leftMostX, rightMostX);
        } else {
            SEVector3f realSpan = shelf.getBoundaryPoint().xyzSpan;
            float originLeft = mMovedObjectOriginBP.center.mD[0] - realSpan.mD[0] / 2;
            float originRight = mMovedObjectOriginBP.center.mD[0] + realSpan.mD[0] / 2;
            float left = originLeft;
            float right = originRight;
            SEVector3f xyzSpan = shelf.getObjectOriginXYZSpan();
            if(mCurrentShelfPressPosition == CLICK_SHELF_LEFT) {
                left = originRight - xyzSpan.mD[0] - 10;
                right = originRight;
            } else if(mCurrentShelfPressPosition == CLICK_SHELF_RIGHT) {
                left = originLeft;
                right = originLeft + xyzSpan.mD[0] + 10;
            }
            return new SEVector2f(left, right);

        }
    }
    private void changeShelfScale(int wallIndex, WallShelf shelf) {
        SEVector3f originCenter = mOriginPlaceInWall;
        float deltax = mRealLocationInWall.mD[0] - originCenter.mD[0];
        Log.i(TAG, "deltax = " + deltax);
        SEVector3f xyzSpan = shelf.getObjectOriginXYZSpan();
        SEVector3f realSpan = shelf.getBoundaryPoint().xyzSpan;
        Log.i(TAG, "shelf width = " + xyzSpan.mD[0]);
        Log.i(TAG, "real shelf width = " + realSpan.mD[0]);
        SEVector3f currentScale = mOriginScale.clone();//shelf.getUserTransParas().mScale.clone();
        float deltaScale = deltax / xyzSpan.mD[0];
        float originLeft = mMovedObjectOriginBP.center.mD[0] - realSpan.mD[0] / 2;
        float originRight = mMovedObjectOriginBP.center.mD[0] + realSpan.mD[0] / 2;
        float right = originRight;
        float left = originLeft;
        float bottom = mMovedObjectOriginBP.center.mD[2] - realSpan.mD[2] / 2;
        float top = mMovedObjectOriginBP.center.mD[2] + realSpan.mD[2] / 2;
        float y = mMovedObjectOriginBP.center.mD[1];

        //int currentClickPosition = calculateShelfClickPosition(originLeft, originRight, originCenter.mD[0], realSpan.mD[0]);
        float newCenterX = 0;
        if(mCurrentShelfPressPosition == CLICK_SHELF_RIGHT) {
            right = originRight + deltax;
            currentScale.mD[0] += deltaScale;
            Log.i(TAG, "current scale = " + currentScale);
        } else if(mCurrentShelfPressPosition == CLICK_SHELF_LEFT) {
            left = originLeft + deltax;
            currentScale.mD[0] -= deltaScale;
        }
        Log.i(TAG, "current left = " + left + " : right = " + right);
        newCenterX = (left + right) / 2;
        SEObjectBoundaryPoint newBP = new SEObjectBoundaryPoint(wallIndex);
        SEVector3f minPoint = new SEVector3f(left, y, bottom);
        SEVector3f maxPoint = new SEVector3f(right, y, top);
        newBP.minPoint = minPoint;
        newBP.maxPoint = maxPoint;
        boolean b = isWallShelfOverlapOtherOtherObject(wallIndex, newBP, shelf);
        Log.i(TAG, "## has object intersect = " + b + " ###");
        SEMountPointChain chain = getCurrentMountPointChain(wallIndex);
        SEVector2f shelfScopeX = getShelfScopeXHasObject(chain, shelf);
        Log.i(TAG, "left most x = " + shelfScopeX.mD[0] + ", right most x = " + shelfScopeX.mD[1]);
        boolean bIsInShelfScope = false;
        if(mCurrentShelfPressPosition == CLICK_SHELF_LEFT) {
            if(left > shelfScopeX.mD[0]) {
                bIsInShelfScope = true;
            } else {
                bIsInShelfScope = false;
            }
        } else if(mCurrentShelfPressPosition == CLICK_SHELF_RIGHT) {
            if(right < shelfScopeX.mD[1]) {
                bIsInShelfScope = true;
            } else {
                bIsInShelfScope = false;
            }
        }
        SEVector2f shelfRowLeftMostPosition = getShelfRowLeftRightMostPosition(chain, shelf);
        boolean bExceedWallBoundary = false;
        if(mCurrentShelfPressPosition == CLICK_SHELF_LEFT) {
            if(left < shelfRowLeftMostPosition.mD[0]) {
                bExceedWallBoundary = true;
            } else {
                bExceedWallBoundary = false;
            }
        } else if(mCurrentShelfPressPosition == CLICK_SHELF_RIGHT) {
            if(right > shelfRowLeftMostPosition.mD[1]) {
                bExceedWallBoundary = true;
            } else {
                bExceedWallBoundary = false;
            }
        }
        if(!b && !bIsInShelfScope && !bExceedWallBoundary) {
            SEVector3f newCenter = new SEVector3f(newCenterX, mMovedObjectOriginBP.center.mD[1], mMovedObjectOriginBP.center.mD[2]);
            newBP.center = newCenter;
            mShelfMoveBP = newBP;
            shelf.getUserTransParas().mTranslate = wallSpaceCoordinateToWorld(wallIndex, newCenter);
            shelf.getUserTransParas().mScale = currentScale;
            shelf.setUserTransParas();
        }
    }
    private class ObjectOnShelfPosition {
        public NormalObject object;
        public SETransParas transParas;
        public ObjectOnShelfPosition() {

        }
        public ObjectOnShelfPosition(NormalObject obj, SETransParas trans) {
            object = obj;
            transParas = trans;
        }
    }
    private float mBeginX, mBeginY;
    private SETransParas mBeginTransParas;
    private ArrayList<ObjectOnShelfPosition> mBeginObjectOnShelfPositionList;
    private void initObjectOnShelfPositionList(WallShelf shelf) {
        ArrayList<NormalObject> objectsOnShelf = shelf.getObjectsOnShelf();
        mBeginObjectOnShelfPositionList = new ArrayList<ObjectOnShelfPosition>();
        SEObject parent = shelf.getParent();
        for(NormalObject obj : objectsOnShelf) {
            SETransParas objAbsoluteTransParas = obj.getAbsoluteTransParas();
            ObjectOnShelfPosition p = new ObjectOnShelfPosition(obj, objAbsoluteTransParas);
            obj.changeParent(parent);
            obj.getUserTransParas().set(objAbsoluteTransParas);
            obj.setUserTransParas();
            mBeginObjectOnShelfPositionList.add(p);
        }
    }
    private void updateShelfPosition(float x, float y, int wallIndex, WallShelf currentShelf) {
        SEVector3f originCenter = mOriginPlaceInWall;
        SETransParas originTransParas = createUserTransParasFromWallTransform(wallIndex, originCenter);
        SETransParas currentTransParas = createUserTransParasFromWallTransform(wallIndex, mRealLocationInWall);
        float deltaxForObj = currentTransParas.mTranslate.mD[0] - originTransParas.mTranslate.mD[0];
        float deltazForObj = currentTransParas.mTranslate.mD[2] - originTransParas.mTranslate.mD[2];
        SEVector3f deltaT = currentTransParas.mTranslate.subtract(originTransParas.mTranslate);
        float deltax = mRealLocationInWall.mD[0] - originCenter.mD[0];
        float deltaz = mRealLocationInWall.mD[2] - originCenter.mD[2];
        Log.i(TAG, "updateShelfPosition deltax = " + deltax + " , deltaxForObj = " + deltaxForObj);
        SEVector3f currentT = mBeginTransParas.mTranslate.clone();
        currentT.mD[0] += deltax;
        currentT.mD[2] += deltaz;
        currentShelf.getUserTransParas().mTranslate = currentT;
        currentShelf.getUserTransParas().mRotate = mObjectTransParas.mRotate.clone();
        currentShelf.setUserTransParas();

        for(ObjectOnShelfPosition p : mBeginObjectOnShelfPositionList) {
            NormalObject obj = p.object;
            SETransParas transParas = p.transParas;
            currentT = transParas.mTranslate.clone();
            currentT = currentT.add(new SEVector3f(deltax, 0, deltaz));
            obj.getUserTransParas().mTranslate = currentT;
            obj.getUserTransParas().mRotate = mObjectTransParas.mRotate.clone();//transParas.mRotate.clone();
            obj.getUserTransParas().mScale = transParas.mScale.clone();
            obj.setUserTransParas();
        }
    }
    private void setAllShelfVisibility(boolean b) {
        ArrayList<ArrayList<WallShelf>> shelfs = mHouse.getWallShelfsInWall();
        for(ArrayList<WallShelf> shelfList : shelfs) {
            for(WallShelf shelf : shelfList) {
                shelf.setVisible(b, true);
            }
        }
    }
    private boolean mIsObjectInVesselOnWall = false;
    private boolean isObjectInVesselOnWall(NormalObject currentObject, MountPointInfo mpi) {
        if(mpi == null) {
            return false;
        }
        ConflictObject conflictObject = mpi.conflictObject;
        if(conflictObject != null && conflictObject.mConflictObject != null) {
            if(conflictObject.mConflictObject instanceof  VesselObject) {
                return true;
            } else if((currentObject instanceof AppObject) && (conflictObject.mConflictObject instanceof  AppObject)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    private boolean isAppObjectOnAppObject(NormalObject currentObject, MountPointInfo mpi) {
        if(mpi == null || mpi.conflictObject == null) {
            return false;
        }
        return (currentObject instanceof  AppObject) && (mpi.conflictObject.mConflictObject instanceof  AppObject);
    }
    private void changeShelf(int wallIndex, NormalObject oldObj, NormalObject newObj) {
        WallShelf shelf = mHouse.getWallShelfWithObject(wallIndex, oldObj);
        if(shelf != null ) {
            shelf.changeObject(oldObj, newObj);
        }
    }
    private VesselObject mCurrentVesselObject = null;
    private void changeObjectToVessel(NormalObject currentObject, MountPointInfo mpi) {
        if(!isAppObjectOnAppObject(currentObject, mpi)) {
            if(mpi != null && mpi.conflictObject != null && (mpi.conflictObject.mConflictObject instanceof Folder)) {
                mCurrentVesselObject = (Folder)mpi.conflictObject.mConflictObject;
            }
            return;
        }
        int wallIndex = mpi.wallIndex;
        NormalObject conflictObject = mpi.conflictObject.mConflictObject;
        AppObject appObject = (AppObject) conflictObject;
        Folder folder = appObject.changeToFolder();
        appObject.hideBackgroud();
        changeShelf(mpi.wallIndex, appObject, folder);
        int mountPointIndex = appObject.getObjectSlot().mMountPointIndex;
        SEMountPointChain chain = getCurrentMountPointChain(wallIndex);
        SEMountPointData mpd = chain.getMountPointData(mountPointIndex);
        SETransParas transParas = createUserTransParasFromWallTransform(wallIndex, mpd.getTranslate());
        folder.getUserTransParas().set(transParas);
        folder.setUserTransParas();
        ObjectInfo objInfo = folder.getObjectInfo();
        Context pContext = SESceneManager.getInstance().getContext();
        if(SettingsActivity.getPreferRotation(pContext) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            objInfo.mVesselName = "home4mian";
        }else {
            objInfo.mVesselName = "home8mianshu";
        }

        if(mCurrentVesselObject != folder) {
            changeVesselToObject(wallIndex, mCurrentVesselObject);
        }
        mCurrentVesselObject = folder;
        //VesselLayer vesselLayer = folder.getVesselLayer();
        //vesselLayer.setOnLayerModel(getOnMoveObject(), true);

    }
    private void changeVesselToObject(int wallIndex, VesselObject vesselObject) {
        if(vesselObject instanceof  Folder) {
            Folder folder = (Folder) vesselObject;
            if (folder.mChildObjects.size() == 1) {
                int mountPointIndex = folder.getObjectSlot().mMountPointIndex;
                NormalObject icon = folder.changeToAppIcon();
                changeShelf(wallIndex, folder, icon);
                icon.getObjectSlot().mSlotIndex = wallIndex;
                icon.getObjectSlot().mMountPointIndex = mountPointIndex;
            }
        }
    }
    private void changeVesselToObject(NormalObject currentObject, MountPointInfo mpi) {
        if(mCurrentVesselObject != null) {
            int wallIndex = mCurrentVesselObject.getObjectSlot().mSlotIndex;
            changeVesselToObject(wallIndex, mCurrentVesselObject);
        }
        mCurrentVesselObject = null;
    }
    private class FolderAnimationFinish implements  SEAnimFinishListener {
        public FolderLayer folderLayer;
        public NormalObject currentObject;
        public int wallIndex;
        public int mountPointIndex;
        @Override
        public void onAnimationfinish() {
            folderLayer.setOnLayerModel(currentObject, false);
            WallShelf shelf = mHouse.getWallShelfWithObject(wallIndex, currentObject);
            if(shelf != null) {
                mHouse.removeObjectFromCurrentShelf(currentObject, mountPointIndex, shelf);
            }
        }
    }
    private boolean placeObjectToVesselInWall(VesselObject vesselObject, NormalObject currentObject) {
        if(!(vesselObject instanceof Folder)) {
            return false;
        }
        Folder folder = (Folder)vesselObject;
        FolderLayer folderLayer = (FolderLayer)folder.getVesselLayer();
        if(folderLayer.canHandleSlot(currentObject) == false) {
            return false;
        }
        int mountPointIndex = currentObject.getObjectSlot().mMountPointIndex;
        int wallIndex = currentObject.getObjectSlot().mSlotIndex;
        folderLayer.setOnLayerModel(currentObject, true);
        FolderAnimationFinish l = new FolderAnimationFinish();
        l.folderLayer = folderLayer;
        l.currentObject = currentObject;
        l.wallIndex = wallIndex;
        l.mountPointIndex = mountPointIndex;
        folderLayer.placeObjectInSlot(l);
        return true;
    }
    private void changeObjectInFolderDB(final Folder folder) {
        if (folder.mChildObjects.size() == 2 && folder.getObjectSlot().mVesselID == -1) {
            folder.getObjectSlot().mVesselID = mHouse.getObjectInfo().mID;
            folder.getObjectInfo().saveToDB();
            for (SEObject child : folder.mChildObjects) {
                final NormalObject icon = (NormalObject) child;
                final int index = folder.mChildObjects.indexOf(child);
                UpdateDBThread.getInstance().process(new Runnable() {
                    public void run() {
                        icon.getObjectSlot().mVesselID = folder.getObjectInfo().mID;
                        icon.getObjectSlot().mSlotIndex = index;
                        icon.getObjectInfo().updateSlotDB();
                    }
                });
            }
        }
    }
    private boolean onObjectMoveEvent_new(ACTION event, float x, float y) {
        int slotType = getOnMoveObject().getObjectInfo().mSlotType;
        Log.i(TAG, "## wall move obj slotType = " + slotType);
        if(getScene().isShelfVisible()) {
            setAllShelfVisibility(true);
        } else {
            setAllShelfVisibility(false);
        }
		MountPointInfo mountPointInfo = null;
        updateRecycleStatus(event, x, y);
        // calculate object's move location
        setMovePoint((int) x, (int) y);
        // calculate object's nearest slot on wall
        ObjectSlot slot = calculateSlot();
        // set on move object scale
        setCurrentObjectScaleToGlobalTransparas();
        //set the coordinate in wall
        setCoordInWallSpace(mHouse.getWallNearestIndex());
        int currentWallIndex = mHouse.getWallNearestIndex();

		//determineShelf();
		//createShelfBoundaryPoint(mHouse.getWallNearestIndex());
		createObjectBoundaryPoint(mHouse.getWallNearestIndex());
        if(slot != null) {
            mountPointInfo = this.calculateNearestMountPoint(slot.mSlotIndex, getOnMoveObject().mName);
        }
        mIsObjectInVesselOnWall = isObjectInVesselOnWall(getOnMoveObject(), mountPointInfo);
        if(mIsObjectInVesselOnWall) {
            changeObjectToVessel(getOnMoveObject(), mountPointInfo);
        } else {
            changeVesselToObject(getOnMoveObject(), mountPointInfo);
        }
        /*
		if(mCurrentLayer == null || mCurrentLayer instanceof FolderLayer)
		{
			if(slot != null) {
		        mountPointInfo = this.calculateNearestMountPoint(slot.mSlotIndex, getOnMoveObject().mName);
			}
	        if(mountPointInfo != null) {
	            if (!hitTestAppFolder(mountPointInfo, slot)) {
	            	if(HomeUtils.DEBUG) {
	                    showLine_new(mountPointInfo.mountPointInfo.mMPD, true);
	                    this.showArea(mountPointInfo.wallIndex, true);
	                    this.changeLineObjectPosition(mountPointInfo);
	            	}
	            }
	        }
		}
		*/
        if (mCurrentLayer != null) {
            cancelConflictAnimationTask();
            //updateWallLinePostion(null);
        }
        switch (event) {
        case BEGIN:
            mBeginX = x;
            mBeginY = y;
            mBeginTransParas = getOnMoveObject().getUserTransParas().clone();
            mPreAction = ACTION.BEGIN;
            mNeedRotateWall = false;
            if(!(getOnMoveObject() instanceof  WallShelf)) {
                SETransParas srcTransParas = getOnMoveObject().getUserTransParas().clone();
                SETransParas desTransParas = mObjectTransParas.clone();
                mSetToRightPositionAnimation = new SetToRightPositionAnimation(getScene(), getOnMoveObject(),
                    srcTransParas, desTransParas, 30);
                mSetToRightPositionAnimation.execute();
            }
            if (mCurrentLayer != null) {
                return mCurrentLayer.onObjectMoveEvent(event, mRealLocation);
            }
            saveMovedObjectBoundary(mountPointInfo);
            if(getOnMoveObject() instanceof  WallShelf) {
                saveCurrentShelfClickPosition((WallShelf)getOnMoveObject());
                if(mCurrentShelfPressPosition == CLICK_SHELF_MID) {
                    initObjectOnShelfPositionList((WallShelf)getOnMoveObject());
                }
            }
            mHasIntersectObject = false;
            mAllIntersectObjectMoveOK = true;
            //setShelfObjectToShelf();
            break;
        case MOVE:
            mPreAction = ACTION.MOVE;
            if(!(getOnMoveObject() instanceof  WallShelf)) {
                getOnMoveObject().getUserTransParas().set(mObjectTransParas);
                getOnMoveObject().setUserTransParas();
            } else {
                if(mCurrentShelfPressPosition == CLICK_SHELF_LEFT || mCurrentShelfPressPosition == CLICK_SHELF_RIGHT) {
                    changeShelfScale(mHouse.getWallNearestIndex(), (WallShelf) getOnMoveObject());
                } else {
                    updateShelfPosition(x, y, currentWallIndex, (WallShelf)getOnMoveObject());
                }
            }
            //Log.i(TAG , " ### mNeedRotateWall = " + mNeedRotateWall + " ####");
            if (!mNeedRotateWall) {
                calculationWallRotation(500);
            }

            break;
        case UP:
        	//Log.i(TAG, "## wall up ##");
            mPreAction = ACTION.UP;
            if(!(getOnMoveObject() instanceof  WallShelf)) {
                getOnMoveObject().getUserTransParas().set(mObjectTransParas);
                getOnMoveObject().setUserTransParas();
            } else {
                if(mCurrentShelfPressPosition == CLICK_SHELF_LEFT || mCurrentShelfPressPosition == CLICK_SHELF_RIGHT) {
                    changeShelfScale(mHouse.getWallNearestIndex(), (WallShelf)getOnMoveObject());
                } else {
                    updateShelfPosition(x, y, currentWallIndex, (WallShelf) getOnMoveObject());
                }

            }
            cancelRotation();
            //updateMoveObjectBoundary(mountPointInfo);
            break;
        case FLY:
        	//Log.i(TAG, "### wall fly ###");
            if(!(getOnMoveObject() instanceof  WallShelf)) {
                getOnMoveObject().getUserTransParas().set(mObjectTransParas);
                getOnMoveObject().setUserTransParas();
            } else {
                if(mCurrentShelfPressPosition == CLICK_SHELF_LEFT || mCurrentShelfPressPosition == CLICK_SHELF_RIGHT) {
                    changeShelfScale(mHouse.getWallNearestIndex(), (WallShelf)getOnMoveObject());
                } else {
                    updateShelfPosition(x, y, currentWallIndex, (WallShelf) getOnMoveObject());
                }

            }

            //playConflictAnimation(mountPointInfo);
            break;
        case FINISH:
        	//Log.i(TAG, "## wall finish ##");
            if(!(getOnMoveObject() instanceof  WallShelf)) {
                getOnMoveObject().getUserTransParas().set(mObjectTransParas);
                getOnMoveObject().setUserTransParas();
            } else {
                if(mCurrentShelfPressPosition == CLICK_SHELF_LEFT || mCurrentShelfPressPosition == CLICK_SHELF_RIGHT) {
                    changeShelfScale(mHouse.getWallNearestIndex(), (WallShelf)getOnMoveObject());
                } else {
                    updateShelfPosition(x, y, currentWallIndex, (WallShelf) getOnMoveObject());
                }
            }
            if (mInRecycle) {
                disableCurrentLayer();
                cancelConflictAnimationTask();
                handleOutsideRoom();
                hideAllLineObject();
                
                return true;
            }

            if(mountPointInfo == null) {
            	if(slot != null) {
    		        setCoordInWallSpace(slot.mSlotIndex);
    		        mountPointInfo = this.calculateNearestMountPoint(slot.mSlotIndex, getOnMoveObject().mName);
    			}
            }
            setShelfObjectToShelf();
            boolean placeInVessel = false;
            if(mCurrentVesselObject != null) {
                placeInVessel = placeObjectToVesselInWall(mCurrentVesselObject, getOnMoveObject());
            }
            if(!placeInVessel) {
                slotToWall_new(mountPointInfo);
                handleSlotSuccess();
            } else {
                if(mCurrentVesselObject != null) {
                    /*
                    ObjectInfo objInfo = mCurrentVesselObject.getObjectInfo();
                    HomeDataBaseHelper help = HomeDataBaseHelper.getInstance(SESceneManager.getInstance()
                            .getContext());
                    SQLiteDatabase db2 = help.getWritableDatabase();
                    objInfo.saveToDB(db2);
                    */
                    if(mCurrentVesselObject instanceof  Folder) {
                        Folder folder = (Folder)mCurrentVesselObject;
                        changeObjectInFolderDB(folder);
                    }
                }
                updateShelfInfo();
            }
            mCurrentVesselObject = null;
            break;
        }
        return true;
    }

    private boolean hitTestAppFolder(MountPointInfo mountPointInfo, ObjectSlot objectSlot) {
        boolean result = false;
        ConflictObject conflictObjects = mountPointInfo.conflictObject;
        Log.i(TAG, "hitTest conflictObject = " + conflictObjects);
        Log.i(TAG, "## currentlayer = " + mCurrentLayer + " ###");
        if(conflictObjects != null) {
        	    Log.i(TAG, " hitTest obj = " + conflictObjects.mConflictObject.mName);
        }
        VesselLayer currentLayer = null;
        if (objectSlot != null && conflictObjects != null && null != conflictObjects.mConflictObject) {
            if (conflictObjects.mConflictObject instanceof VesselObject) {
                VesselObject vesselObject = (VesselObject) conflictObjects.mConflictObject;
                VesselLayer vesselLayer = vesselObject.getVesselLayer();
                if (vesselLayer.canHandleSlot(getOnMoveObject())) {
                    if (vesselLayer != mCurrentLayer) {
                        disableCurrentLayer();
                        vesselLayer.setOnLayerModel(getOnMoveObject(), true);
                        mCurrentLayer = vesselLayer;
                        result = true;
                    }
                } else {
                    disableCurrentLayer();
                }
            } else if ((getOnMoveObject() instanceof AppObject)
                    && (conflictObjects.mConflictObject instanceof AppObject)) {
                AppObject appObject = (AppObject) conflictObjects.mConflictObject;
                Folder folder = appObject.changeToFolder();
                appObject.hideBackgroud();
                changeExistentObject(appObject, folder);
                disableCurrentLayer();
                VesselLayer vesselLayer = folder.getVesselLayer();
                vesselLayer.setOnLayerModel(getOnMoveObject(), true);
                mCurrentLayer = vesselLayer;
                result = true;
            } else {
                disableCurrentLayer();
            }
        } else {
            disableCurrentLayer();
        }
        return result;
    }

    public boolean onObjectMoveEvent(ACTION event, float x, float y) {
        stopMoveAnimation();
        /*
        updateRecycleStatus(event, x, y);
        // calculate object's move location
        setMovePoint((int) x, (int) y);
        // calculate object's nearest slot on wall
        ObjectSlot objectSlot = calculateSlot();
        */
        ObjectSlot objectSlot = null;
        //// for test
        boolean ret = this.onObjectMoveEvent_new(event, x, y);
        if(true)
        	    return ret;
        //////////////////////////////////////////////
        List<ConflictObject> conflictObjects = null;
        if (!cmpSlot(objectSlot, mObjectSlot)) {
            // get the objects in this slot
            conflictObjects = calculateConflictObjects(objectSlot);
            if (objectSlot != null && conflictObjects != null && conflictObjects.size() == 1) {
                if (conflictObjects.get(0).mConflictObject instanceof VesselObject) {
                    VesselObject vesselObject = (VesselObject) conflictObjects.get(0).mConflictObject;
                    VesselLayer vesselLayer = vesselObject.getVesselLayer();
                    if (vesselLayer.canHandleSlot(getOnMoveObject())) {
                        if (vesselLayer != mCurrentLayer) {
                            disableCurrentLayer();
                            vesselLayer.setOnLayerModel(getOnMoveObject(), true);
                            mCurrentLayer = vesselLayer;
                        }
                    } else {
                        disableCurrentLayer();
                    }
                } else if ((getOnMoveObject() instanceof AppObject)
                        && (conflictObjects.get(0).mConflictObject instanceof AppObject)) {
                    AppObject appObject = (AppObject) conflictObjects.get(0).mConflictObject;
                    Folder folder = appObject.changeToFolder();
                    appObject.hideBackgroud();
                    changeExistentObject(appObject, folder);
                    disableCurrentLayer();
                    VesselLayer vesselLayer = folder.getVesselLayer();
                    vesselLayer.setOnLayerModel(getOnMoveObject(), true);
                    mCurrentLayer = vesselLayer;

                } else {
                    disableCurrentLayer();
                }
            } else {
                disableCurrentLayer();
            }
        }
        if (mCurrentLayer != null) {
            cancelConflictAnimationTask();
            updateWallLinePostion(null);
        }
       
        switch (event) {
        case BEGIN:
            mPreAction = ACTION.BEGIN;
            mNeedRotateWall = false;
            SETransParas srcTransParas = getOnMoveObject().getUserTransParas().clone();
            SETransParas desTransParas = mObjectTransParas.clone();
            if (getOnMoveObject().getObjectInfo().mIsNativeObject) {
                SETransParas localTrans = getOnMoveObject().getObjectInfo().mModelInfo.mLocalTrans;
                //Log.i(TAG, "obj localTrans = " + localTrans.mTranslate + " ##");
                //Log.i(TAG, "desTransparas translate = " + desTransParas.mTranslate + " ##");
                if (localTrans != null) {
                	    SEVector3f tmpTranslate = localTrans.mTranslate.clone();
                	    //tmpTranslate.mD[0] = 10;
                	    //tmpTranslate.mD[1] = -800;
                	    //tmpTranslate.mD[2] = -20;
                    desTransParas.mTranslate.selfSubtract(tmpTranslate);
                }
            }
            mSetToRightPositionAnimation = new SetToRightPositionAnimation(getScene(), getOnMoveObject(),
                    srcTransParas, desTransParas, 30);
            mSetToRightPositionAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    if (!mNeedRotateWall) {
                        calculationWallRotation(800);
                    }
                }
            });
            mSetToRightPositionAnimation.execute();
            if (mCurrentLayer != null) {
                mObjectSlot = objectSlot;
                return mCurrentLayer.onObjectMoveEvent(event, mRealLocation);
            }
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
                if (calculateAndPlayConflictAnimation(conflictObjects, 1000)) {
                    updateWallLinePostion(mObjectSlot);
                } else {
                    updateWallLinePostion(null);
                }
            }
            break;
        case MOVE:
            mPreAction = ACTION.MOVE;
            getOnMoveObject().getUserTransParas().set(mObjectTransParas);
            getOnMoveObject().setUserTransParas();
            if (mCurrentLayer != null) {
                mObjectSlot = objectSlot;
                return mCurrentLayer.onObjectMoveEvent(event, mRealLocation);
            }
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
                if (calculateAndPlayConflictAnimation(conflictObjects, 250)) {
                    updateWallLinePostion(mObjectSlot);
                } else {
                    updateWallLinePostion(null);
                }
            }
            if (!mNeedRotateWall) {
                calculationWallRotation(500);
            }
            break;
        case UP:
        	//Log.i(TAG, "## wall up ##");
            mPreAction = ACTION.UP;
            getOnMoveObject().getUserTransParas().set(mObjectTransParas);
            getOnMoveObject().setUserTransParas();
            if (mCurrentLayer != null) {
                mObjectSlot = objectSlot;
                return mCurrentLayer.onObjectMoveEvent(event, mRealLocation);
            }
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
                if (calculateAndPlayConflictAnimation(conflictObjects, 250)) {
                    updateWallLinePostion(mObjectSlot);
                } else {
                    updateWallLinePostion(null);
                }
            }

            cancelRotation();
            mHouse.toNearestFace(null, 5);
            break;
        case FLY:
            getOnMoveObject().getUserTransParas().set(mObjectTransParas);
            getOnMoveObject().setUserTransParas();
            if (mCurrentLayer != null) {
                mObjectSlot = objectSlot;
                return mCurrentLayer.onObjectMoveEvent(event, mRealLocation);
            }
            if (!cmpSlot(objectSlot, mObjectSlot)) {
                mObjectSlot = objectSlot;
                if (calculateAndPlayConflictAnimation(conflictObjects, 250)) {
                    updateWallLinePostion(mObjectSlot);
                } else {
                    updateWallLinePostion(null);
                }
            }
            break;
        case FINISH:
        	//Log.i(TAG, "## wall finish ##");
            getOnMoveObject().getUserTransParas().set(mObjectTransParas);
            getOnMoveObject().setUserTransParas();
            if (mInRecycle) {
                disableCurrentLayer();
                cancelConflictAnimationTask();
                handleOutsideRoom();
                return true;
            }
            if (mCurrentLayer != null) {
                mObjectSlot = objectSlot;
                return mCurrentLayer.onObjectMoveEvent(event, mRealLocation);
            }
            slotToWall(null);
            break;
        }
        return true;
    }

    /**
     * Calculate whether object is in rubbish box and update the box's color
     */
    private void updateRecycleStatus(ACTION event, float x, float y) {
        boolean force = false;
        switch (event) {
        case BEGIN:
            force = false;
            break;
        case MOVE:
            force = false;
            break;
        case UP:
            force = true;
            break;
        case FLY:
            force = true;
            break;
        case FINISH:
            force = true;
            break;
        }

        if (x >= mBoundOfRecycle.left && x <= mBoundOfRecycle.right && y <= mBoundOfRecycle.bottom
                && y >= mBoundOfRecycle.top) {
            mInRecycle = true;
        } else {
            if (!force) {
                mInRecycle = false;
            }
        }
    }

    private void disableCurrentLayer() {
        if (mCurrentLayer != null) {
            if (mCurrentLayer instanceof FolderLayer) {
                final Folder folder = (Folder) mCurrentLayer.getVesselObject();
                if (folder.mChildObjects.size() == 1) {
                    NormalObject icon = folder.changeToAppIcon();
                    changeExistentObject(folder, icon);
                } else if (folder.mChildObjects.size() == 2 && folder.getObjectSlot().mVesselID == -1) {
                    folder.getObjectSlot().mVesselID = mHouse.getObjectInfo().mID;
                    folder.getObjectInfo().saveToDB();
                    for (SEObject child : folder.mChildObjects) {
                        final NormalObject icon = (NormalObject) child;
                        final int index = folder.mChildObjects.indexOf(child);
                        UpdateDBThread.getInstance().process(new Runnable() {
                            public void run() {
                                icon.getObjectSlot().mVesselID = folder.getObjectInfo().mID;
                                icon.getObjectSlot().mSlotIndex = index;
                                icon.getObjectInfo().updateSlotDB();
                            }
                        });
                    }
                }
            }
            mCurrentLayer.setOnLayerModel(getOnMoveObject(), false);
            mCurrentLayer = null;
        }
    }

    private void changeExistentObject(NormalObject src, NormalObject des) {
        if (mExistentObjects != null) {
            for (ConflictObject existentObject : mExistentObjects) {
                if (existentObject.mConflictObject == src) {
                    existentObject.mConflictObject = des;
                    break;
                }
            }
        }
    }

    public void stopMoveAnimation() {
        if (mSetToRightPositionAnimation != null) {
            mSetToRightPositionAnimation.stop();
        }
    }
    public SEVector3f getFingerOnWallLocation(int touchX, int touchY) {
    	SERay ray = mCamera.screenCoordinateToRay(touchX, touchY);
        SEVector3f location = rayCrossWall(ray, mHouse.getWallRadius()).mTranslate;
        return location;
    }
    public void setMovePoint(int touchX, int touchY) {
        SERay ray = mCamera.screenCoordinateToRay(touchX, touchY);
        mRealLocation = rayCrossWall(ray, mHouse.getWallRadius()).mTranslate;
        mObjectTransParas = getObjectTransParas(ray);
    }

    private void playConflictAnimationTask(List<ConflictObject> needMoveObjects, long delay) {
        cancelConflictAnimationTask();
        if (needMoveObjects != null) {
            mPlayConflictAnimationTask = new ConflictAnimationTask(needMoveObjects);
            if (delay == 0) {
                mPlayConflictAnimationTask.run();
            } else {
                SELoadResThread.getInstance().process(mPlayConflictAnimationTask, delay);
            }
        }
    }

    private void cancelConflictAnimationTask() {
        if (mPlayConflictAnimationTask != null) {
            SELoadResThread.getInstance().cancel(mPlayConflictAnimationTask);
            mPlayConflictAnimationTask = null;
        }
    }

    private ConflictAnimationTask mPlayConflictAnimationTask;

    private class ConflictAnimationTask implements Runnable {
        private List<ConflictObject> mMyNeedMoveObject;

        public ConflictAnimationTask(List<ConflictObject> needMoveObjects) {
            mMyNeedMoveObject = needMoveObjects;
        }

        public void run() {
            if (mMyNeedMoveObject != null) {
                for (ConflictObject wallObject : mMyNeedMoveObject) {
                    wallObject.playConflictAnimation();
                }
            }
        }
    }

    private Runnable mRotateToNextFace = new Runnable() {
        public void run() {
            new SECommand(getScene()) {
                public void run() {
                    if (onLeft()) {
                        mHouse.toLeftFace(new SEAnimFinishListener() {
                            public void onAnimationfinish() {
                            	/*
                                ObjectSlot objectSlot = calculateSlot();
                                if (!cmpSlot(objectSlot, mObjectSlot)) {
                                    mObjectSlot = objectSlot;
                                    List<ConflictObject> conflictObjects = calculateConflictObjects(objectSlot);
                                    if (calculateAndPlayConflictAnimation(conflictObjects, 250)) {
                                        updateWallLinePostion(mObjectSlot);
                                    } else {
                                        updateWallLinePostion(null);
                                    }
                                }
                                */
                                if (mPreAction == ACTION.UP) {
                                    cancelRotation();
                                } else {
                                    calculationWallRotation(800);
                                }
                            }
                        }, 5);
                    } else if (onRight()) {
                        mHouse.toRightFace(new SEAnimFinishListener() {
                            public void onAnimationfinish() {
                            	/*
                                ObjectSlot objectSlot = calculateSlot();
                                if (!cmpSlot(objectSlot, mObjectSlot)) {
                                    mObjectSlot = objectSlot;
                                    List<ConflictObject> conflictObjects = calculateConflictObjects(objectSlot);
                                    if (calculateAndPlayConflictAnimation(conflictObjects, 250)) {
                                        updateWallLinePostion(mObjectSlot);
                                    } else {
                                        updateWallLinePostion(null);
                                    }
                                }
                                */
                                if (mPreAction == ACTION.UP) {
                                    cancelRotation();
                                } else {
                                    calculationWallRotation(800);
                                }
                            }
                        }, 5);
                    } else {
                        mNeedRotateWall = false;
                    }
                }
            }.execute();
        }
    };

    private void calculationWallRotation(long delayTime) {
        if((onLeft() || onRight()) && (!(getOnMoveObject() instanceof  WallShelf) || mCurrentShelfPressPosition == CLICK_SHELF_MID)) {
            mNeedRotateWall = true;
            //Log.i(TAG, "##### calculationWallRotation   ######");
            SELoadResThread.getInstance().process(mRotateToNextFace, delayTime);
        } else {
            cancelRotation();
        }
    }

    private boolean onLeft() {
        int screenW = mCamera.getWidth();
        float moveObjectTouchX = getOnMoveObject().getTouchX();
        //Log.i(TAG, "## touch x = " + getOnMoveObject().getTouchX() + ", y " + getOnMoveObject().getTouchY() );
        //Log.i(TAG, "## screeW = " + screenW + ", screenh = " + mCamera.getHeight());
        float threshold = screenW * 0.1f;
        if(threshold > 50) {
        	threshold = 50;
        }
        return moveObjectTouchX < threshold || mOnCurrentWallLeft;
    }

    private boolean onRight() {
        int screenW = mCamera.getWidth();
        float threshold = screenW * 0.1f;
        if(threshold > 50) {
        	threshold = 50;
        }
        threshold = screenW - threshold;
        return getOnMoveObject().getTouchX() > threshold || mOnCurrentWallRight;
    }

    private void cancelRotation() {
        mNeedRotateWall = false;
        SELoadResThread.getInstance().cancel(mRotateToNextFace);
        mHouse.stopAllAnimation(null);
    }
    /*
    private MountPointInfo calculateCurrentMountPoint(String objectName) {
    	   if(onRight() || onLeft() || mInRecycle) {
    		   return null;
    	   }
    	   MountPointInfo mpi = calculateNearestMountPoint(objectName);
    	   return mpi;
    }
    */
    private ObjectSlot calculateSlot() {
        if (onRight() || onLeft() || mInRecycle) {
            return null;
        }
        return calculateNearestSlot(mHouse.getWallNearestIndex(), mRealLocation);
    }

    private List<ConflictObject> calculateConflictObjects(ObjectSlot conflictSlot) {
        if (conflictSlot == null) {
            return null;
        }
        List<ConflictObject> existentObjects = getExistentObject(mHouse.getWallNearestIndex());
        return caculateConflictObjs(existentObjects, conflictSlot);
    }

    private boolean cmpSlot(ObjectSlot objectSlot1, ObjectSlot objectSlot2) {
        if (objectSlot1 == null && objectSlot2 == null) {
            return true;
        }
        if ((objectSlot1 != null && objectSlot2 == null) || (objectSlot1 == null && objectSlot2 != null)) {
            return false;
        }
        return objectSlot1.equals(objectSlot2);
    }

    private boolean calculateAndPlayConflictAnimation(List<ConflictObject> conflictObjs, long delay) {
        if (conflictObjs == null) {
            cancelConflictAnimationTask();
            return true;
        }
        List<ConflictObject> needMoveObjs = new ArrayList<ConflictObject>();
        List<ConflictObject> existentObjects = getExistentObject(mHouse.getWallNearestIndex());
        int sizeX = (int)mHouse.getWallSpanX();
        int sizeY = (int)mHouse.getWallSpanY();
        boolean[][] slot = new boolean[sizeY][sizeX];
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                slot[y][x] = true;
            }
        }
        // detect if it can place to the new position
        ConflictObject conflictObject = new ConflictObject();
        conflictObject.mConflictObject = getOnMoveObject();
        conflictObject.mMoveSlot = mObjectSlot;
        if (canPlaceToNewSlot(existentObjects, conflictObject, slot, conflictObjs, needMoveObjs, true)) {
            playConflictAnimationTask(needMoveObjs, delay);
            return true;
        } else {
            cancelConflictAnimationTask();
            return false;
        }
    }

    private List<ConflictObject> getExistentObject(int faceIndex) {
        List<ConflictObject> existentSlot = new ArrayList<ConflictObject>();
        for (ConflictObject normalObject : mExistentObjects) {
            ObjectSlot objectSlot = normalObject.mConflictObject.getObjectInfo().mObjectSlot;
            if (objectSlot.mSlotIndex == faceIndex) {
                existentSlot.add(normalObject);
            }
        }
        return existentSlot;
    }

    private boolean canPlaceToNewSlot(List<ConflictObject> existentObjects, ConflictObject conflictObj,
            boolean[][] slot, List<ConflictObject> conflictObjects, List<ConflictObject> needMoveObjs, boolean start) {
        if (start) {// has found new position
            int startY = conflictObj.mMoveSlot.mStartY;
            int startX = conflictObj.mMoveSlot.mStartX;
            float sX = conflictObj.mMoveSlot.mSpanX;
            float sY = conflictObj.mMoveSlot.mSpanY;
            for (int y = startY; y < startY + sY; y++) {
                for (int x = startX; x < startX + sX; x++) {
                    slot[y][x] = false;
                }
            }
            needMoveObjs.addAll(conflictObjects);
            if (caculateConflictObjsNewSlot(existentObjects, conflictObjects, conflictObj.mMoveSlot)) {
                // using the first method to find out the conflict Objects'
                // new position
                return true;
            }
            // using the second method to find the conflict Objects' new
            // position if the first method is failed
            for (ConflictObject myConflictObj : conflictObjects) {
                if (!canPlaceToNewSlot(existentObjects, myConflictObj, slot, null, needMoveObjs, false)) {
                    return false;
                }
            }
            return true;
        } else {
            if (existentObjects.contains(conflictObj)) {
                existentObjects.remove(conflictObj);
            }
            ObjectSlot currentSlot = conflictObj.mConflictObject.getObjectSlot().clone();
            List<ObjectSlot> emptySlot = searchEmptySlot(currentSlot.mSlotIndex, currentSlot.mSpanX,
                    currentSlot.mSpanY, slot);// search the empty position
            if (emptySlot != null) {
                // search the nearest position from its current position
                conflictObj.mMoveSlot = searchNearestSlot(emptySlot, currentSlot);
                int startY = conflictObj.mMoveSlot.mStartY;
                int startX = conflictObj.mMoveSlot.mStartX;
                float sX = conflictObj.mMoveSlot.mSpanX;
                float sY = conflictObj.mMoveSlot.mSpanY;

                for (int y = startY; y < startY + sY; y++) {
                    for (int x = startX; x < startX + sX; x++) {
                        slot[y][x] = false;
                    }
                }
                List<ConflictObject> myConflictObjs = caculateConflictObjs(existentObjects, conflictObj.mMoveSlot);
                // find out which objects have occupied its new position
                if (myConflictObjs != null) {
                    needMoveObjs.addAll(myConflictObjs);
                    for (ConflictObject myConflictObj : myConflictObjs) {
                        if (!canPlaceToNewSlot(existentObjects, myConflictObj, slot, null, needMoveObjs, false)) {
                            return false;
                        }
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    private List<ConflictObject> caculateConflictObjs(List<ConflictObject> existentObjs, ObjectSlot cmpSlot) {
        if (cmpSlot == null) {
            return null;
        }
        List<ConflictObject> conflictSlots = null;
        for (ConflictObject normalObject : existentObjs) {
            ObjectSlot objectSlot = normalObject.mConflictObject.getObjectInfo().mObjectSlot;
            boolean xConflict = !((cmpSlot.mStartX <= objectSlot.mStartX && (cmpSlot.mStartX + cmpSlot.mSpanX) <= objectSlot.mStartX) || (cmpSlot.mStartX >= (objectSlot.mStartX + objectSlot.mSpanX) && (cmpSlot.mStartX + cmpSlot.mSpanX) >= (objectSlot.mStartX + objectSlot.mSpanX)));
            boolean YConflict = !((cmpSlot.mStartY <= objectSlot.mStartY && (cmpSlot.mStartY + cmpSlot.mSpanY) <= objectSlot.mStartY) || (cmpSlot.mStartY >= (objectSlot.mStartY + objectSlot.mSpanY) && (cmpSlot.mStartY + cmpSlot.mSpanY) >= (objectSlot.mStartY + objectSlot.mSpanY)));
            if (xConflict && YConflict) {
                if (conflictSlots == null) {
                    conflictSlots = new ArrayList<ConflictObject>();
                }
                conflictSlots.add(normalObject);
            }
        }
        return conflictSlots;
    }

    private boolean caculateConflictObjsNewSlot(List<ConflictObject> existentObjects,
            List<ConflictObject> conflictObjs, ObjectSlot cmpSlot) {
        if (conflictObjs == null) {
            return true;
        }
        List<ObjectSlot> existentSlot = new ArrayList<ObjectSlot>();
        existentSlot.add(cmpSlot);
        for (ConflictObject wallObject : existentObjects) {
            boolean isConflictObj = false;
            for (ConflictObject conflictObj : conflictObjs) {
                if (conflictObj.equals(wallObject)) {
                    isConflictObj = true;
                    break;
                }
            }
            if (!isConflictObj) {
                existentSlot.add(wallObject.mConflictObject.getObjectInfo().mObjectSlot);
            }
        }
        for (ConflictObject conflictObj : conflictObjs) {
            ObjectSlot newSlot = conflictObj.mConflictObject.getObjectInfo().mObjectSlot.clone();
            List<ObjectSlot> emptySlots = searchEmptySlot(newSlot.mSlotIndex, newSlot.mSpanX, newSlot.mSpanY,
                    existentSlot);
            float minDistance = Float.MAX_VALUE;
            if (emptySlots == null) {
                return false;
            }
            for (ObjectSlot emptySlot : emptySlots) {
                float distance = getSlotPosition(conflictObj.mConflictObject.getObjectInfo().mObjectSlot).subtract(
                        getSlotPosition(emptySlot)).getLength();
                if (distance < minDistance) {
                    minDistance = distance;
                    newSlot.mSlotIndex = emptySlot.mSlotIndex;
                    newSlot.mStartX = emptySlot.mStartX;
                    newSlot.mStartY = emptySlot.mStartY;
                }
            }
            conflictObj.mMoveSlot = newSlot.clone();
            existentSlot.add(newSlot);
        }
        return true;

    }

    private ObjectSlot calculateNearestSlot(int faceIndex, SEVector3f location) {
        ObjectSlot slot = getOnMoveObject().getObjectSlot().clone();
        slot.mSlotIndex = faceIndex;
        /*
        SEVector3f wallCenter = new SEVector3f();
        wallCenter.mD[0] = 0;
        wallCenter.mD[1] = mHouse.getWallRadius();
        wallCenter.mD[2] = mHouse.getWallHeight() + mHouse.getWallSpanY()  * mHouse.getWallUnitSizeY() / 2f;

        SEVector3f start = new SEVector3f();
        start.mD[0] = location.getX() - slot.mSpanX * mHouse.getWallUnitSizeX() / 2f;
        start.mD[1] = wallCenter.getY();
        start.mD[2] = location.getZ() + slot.mSpanY * mHouse.getWallUnitSizeY() / 2f;

        float convertStartX = (start.getX() - wallCenter.getX()) / mHouse.getWallUnitSizeX()  + mHouse.getWallSpanX()
                / 2f;
        if (convertStartX < 0) {
            convertStartX = 0;
        } else if (convertStartX > mHouse.getWallSpanX() - slot.mSpanX) {
            convertStartX = mHouse.getWallSpanX() - slot.mSpanX;
        }
        float convertStartY = mHouse.getWallSpanY() / 2f - (start.getZ() - wallCenter.getZ())
                / mHouse.getWallUnitSizeY();
        if (convertStartY < 0) {
            convertStartY = 0;
        } else if (convertStartY > mHouse.getWallSpanY() - slot.mSpanY) {
            convertStartY = mHouse.getWallSpanY() - slot.mSpanY;
        }
        slot.mStartX = Math.round(convertStartX);
        slot.mStartY = Math.round(convertStartY);
        */
        return slot;
    }
    private void getWallLocalBoundaryVolume(SEVector3f minPoint , SEVector3f maxPoint) {
    	String wallName = this.getWallName(0);
    	SEObject object = getScene().findObject(wallName, 0);
    	object.createLocalBoundingVolume();
    	object.getLocalBoundingVolume(minPoint, maxPoint);
    	SEVector3f translate = object.getLocalTranslate();
    	for(int i = 0 ; i < 3 ; i++) {
    		minPoint.mD[i] -= translate.mD[i];
    		maxPoint.mD[i] -= translate.mD[i];
    	}
    }
    private float getWallRealDistance() {
    	String wallName = this.getWallName(0);
    	SEObject object = getScene().findObject(wallName, 0);
    	SEVector3f translate = object.getLocalTranslate();
    	return translate.mD[1];
    }
    private SETransParas getObjectTransParas(SERay ray) {
    	float wallRealDist = getWallRealDistance();
        SETransParas transParas = rayCrossWall(ray, mVirtualWallRadius);
    	SETransParas transParasReal = rayCrossWall(ray, wallRealDist);
        SEVector3f location = transParasReal.mTranslate;
        SEVector3f moveObjectBVMinPoint = new SEVector3f();
        SEVector3f moveObjectBVMaxPoint = new SEVector3f();
        NormalObject moveObject = getOnMoveObject();
        moveObject.createLocalBoundingVolume();
        moveObject.getLocalBoundingVolume(moveObjectBVMinPoint, moveObjectBVMaxPoint);
        SEVector3f moveObjectXYZSpan = moveObjectBVMaxPoint.subtract(moveObjectBVMinPoint);
        int wallIndex = mHouse.getWallNearestIndex();
        SEVector3f locationInWall = this.toWallSpaceCoordinate(wallIndex, location);
        SEVector3f moveObjectMaxLocation = locationInWall.add(moveObjectXYZSpan.mul(0.5f));
        SEVector3f moveObjectMinLocation = locationInWall.subtract(moveObjectXYZSpan.mul(0.5f));
        SEVector3f wallMinPoint = new SEVector3f();
        SEVector3f wallMaxPoint = new SEVector3f();
        this.getWallLocalBoundaryVolume(wallMinPoint, wallMaxPoint);
        mWallMinPoint = wallMinPoint;
        mWallMaxPoint = wallMaxPoint;
        boolean b = this.isPointInRect(locationInWall, wallMinPoint, wallMaxPoint);
        //Log.i(TAG, "### finger location = " + locationInWall + " ####");
        //Log.i(TAG, "### wall min point = " + wallMinPoint + " ####");
        //Log.i(TAG, "### wall max point = " + wallMaxPoint + " ####");
        //Log.i(TAG, "## locationIn Wall = " + locationInWall);
        //Log.i(TAG, "## xysSpan = " + moveObjectXYZSpan + " ##");
        //Log.i(TAG, "## moveobj min locatoin = " + moveObjectMinLocation + " ##" );
        //Log.i(TAG, "### in wall :  " + b + " #####");
        mOnCurrentWallRight = mOnCurrentWallLeft = false;
        if(b == false) {
        	if(locationInWall.getX() > wallMaxPoint.getX()) {
        		transParas.mRotate.set(-90, 0, 0, 1);
        		//float delta = locationInWall.getX() - wallMaxPoint.getX();
        		
        		float deltax = transParasReal.mTranslate.getX() - wallRealDist;
        		float deltay = transParasReal.mTranslate.getY();
        		float yy = deltax * deltay / locationInWall.getX();
        		transParas.mTranslate.mD[1] -= yy;
        		transParas.mTranslate.mD[0] = mHouse.getWallRadius();
        		mOnCurrentWallRight = true;
        	} else if(locationInWall.getX() < wallMinPoint.getX()) {
        		transParas.mRotate.set(90, 0, 0, 1);
        		float deltax = Math.abs(transParasReal.mTranslate.getX()) - wallRealDist;
        		float deltay = transParasReal.mTranslate.getY();
        		float yy = deltax * deltay / locationInWall.getX();
        		transParas.mTranslate.mD[1] -= yy;
        		transParas.mTranslate.mD[0] = -mHouse.getWallRadius();
        		mOnCurrentWallLeft = true;
        	}
        }
        if(moveObjectMinLocation.getZ() < wallMinPoint.getZ()) {
        	float deltaz = wallMinPoint.getZ() - moveObjectMinLocation.getZ();
        	transParas.mTranslate.mD[2] += deltaz;
            mRealLocation.mD[2] = transParas.mTranslate.mD[2];
        } else if(moveObjectMaxLocation.getZ() > wallMaxPoint.getZ()) {
        	float deltaz = moveObjectMaxLocation.getZ() - wallMaxPoint.getZ();
        	transParas.mTranslate.mD[2] -= deltaz;
            mRealLocation.mD[2] = transParas.mTranslate.mD[2];
        }
        return transParas;
    }

    private SETransParas rayCrossWall(SERay ray, float wallRadius) {
        // ray cross the front wall
        SETransParas transParas = new SETransParas();
        float y = wallRadius;
        assert(ray.getDirection().getY() == 1.0);
        float para = (y - ray.getLocation().getY()) / ray.getDirection().getY();
        transParas.mTranslate = ray.getLocation().add(ray.getDirection().mul(para));
        float faceAngle = 360 / mHouse.getCount();
        float tanAngle = (float) Math.tan(faceAngle * Math.PI / 360);
        float halfFaceW = wallRadius * tanAngle;
        /*
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
        */
        return transParas;

    }

    private SEVector3f rayCrossCylinder(SERay ray, float radius) {
        float Xa = ray.getLocation().getX();
        float Ya = ray.getLocation().getY();
        float Xb = ray.getDirection().getX();
        float Yb = ray.getDirection().getY();
        float a = Xb * Xb + Yb * Yb;
        float b = 2 * (Xa * Xb + Ya * Yb);
        float c = Xa * Xa + Ya * Ya - radius * radius;
        float para = (float) ((-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a));
        SEVector3f touchLoc = ray.getLocation().add(ray.getDirection().mul(para));
        return touchLoc;
    }
    private boolean isObjectOnWall(NormalObject object) {
    	ObjectInfo objInfo = object.getObjectInfo();
    	if(objInfo.mSlotType == ObjectInfo.SLOT_TYPE_WALL || objInfo.mSlotType == ObjectInfo.SLOT_TYPE_APP_WALL) {
    		return true;
    	} else {
    		return false;
    	}
    }
    private List<ConflictObject> getExistentObject() {
        List<ConflictObject> fillSlots = new ArrayList<ConflictObject>();
        for (SEObject object : mHouse.mChildObjects) {
            if (object instanceof NormalObject) {
                NormalObject wallObject = (NormalObject) object;
                ObjectInfo objInfo = wallObject.getObjectInfo();
                if ((objInfo.mSlotType == ObjectInfo.SLOT_TYPE_WALL || objInfo.mSlotType == ObjectInfo.SLOT_TYPE_APP_WALL)
                        && !object.equals(getOnMoveObject())) {
                    ConflictObject conflictObject = new ConflictObject();
                    conflictObject.mConflictObject = wallObject;
                    fillSlots.add(conflictObject);
                    if(wallObject instanceof WallShelf) {
                    	//Log.i(TAG, "wall object is jiazi");
                    }
                    /*
                    if(wallObject.mName.startsWith("app")) {
                    	    Log.i(TAG, "app obj = " + wallObject);
                    }
                    */
                }
            }
        }
        return fillSlots;
    }
    
     
    private SETransParas createNativeObjectBoundingVolumeTransParas(MountPointInfo mpi) {
        NormalObject object = (NormalObject)getOnMoveObject();
        boolean isNativeObject = object.getObjectInfo().mIsNativeObject;
        if(!isNativeObject) {
        	    return null;
        }
        String wallName = getWallName(mpi.wallIndex);
        SEVector3f objectMinV = new SEVector3f();
        SEVector3f objectMaxV = new SEVector3f();
        object.createLocalBoundingVolume();
        object.getLocalBoundingVolume(objectMinV, objectMaxV);
        //Log.i(TAG, "bound min = " + objectMinV + ", max = " + objectMaxV);
        //SEVector3f currentFingerWorldPoint = mRealLocation;//mObjectTransParas.mTranslate;
        SEVector3f coordWallSpace = mRealLocationInWall.clone(); //this.toWallSpaceCoordinate(mpi.wallIndex, currentFingerWorldPoint);
        float xspan = objectMaxV.getX() - objectMinV.getX();
        float zspan = objectMaxV.getZ() - objectMinV.getZ();
        float sx = xspan / lw;
        float sz = zspan / lh;
        float sy = 1;
        SEVector3f wallTranslate = new SEVector3f();
        SEVector3f wallScale = new SEVector3f();
        SERotate wallRotate = new SERotate();
        getWallLocalRotateAndTranslateScale(wallName, wallTranslate, wallRotate, wallScale);
        SEVector3f scale = new SEVector3f(sx, 1, sz);
        SEVector3f coordWallSpaceV = SEObject.rotateMapPoint(wallRotate, coordWallSpace);
        coordWallSpaceV = coordWallSpaceV.add(wallTranslate);
        for(int i = 0 ; i < 3 ; i++) {
            scale.mD[i] *= wallScale.mD[i];
        }
        scale.mD[1] = 1;
        SETransParas tp = new SETransParas(coordWallSpaceV, wallRotate, scale);
        return tp;
    }
    private void changeMountPointAreaPosition(MountPointInfo mpi) {
        if(mMountPointArea.mList.size() == 0)
                return;
        if(getOnMoveObject().getObjectInfo().mIsNativeObject == false)
                return;
    	    SEMountPointChain currentChain = this.getCurrentMountPointChain(mpi.wallIndex);
    	    SEMountPointChain.BoundaryPoint boundaryPoint = mMountPointArea.mBoundaryPoint;
        //SEMountPointChain.MatrixPoint minPoint = currentChain.getMatrixPointInPlaneXZ(minCoordWallSpace);
        //SEMountPointChain.MatrixPoint maxPoint = currentChain.getMatrixPointInPlaneXZ(maxCoordWallSpace);
    	    SEMountPointChain.MatrixPoint startPoint = boundaryPoint.startPoint;
    	    SEMountPointChain.MatrixPoint endPoint = boundaryPoint.endPoint;
    	    int k = 0;
    	    
    	    for(int i = endPoint.row ; i <= startPoint.row ; i++) {
    	    	    for(int j = startPoint.col ; j <= endPoint.col ; j++) {
                        int index = currentChain.getIndex(i, j);
                        SEMountPointData mpd = currentChain.getMountPointData(index);
                        SEVector3f wallSpaceCoord = mpd.getTranslate().clone();
                        wallSpaceCoord.mD[1] -= 10;
    	    	    	SETransParas tp = this.createUserTransParasFromWallTransform(mpi.wallIndex, wallSpaceCoord);
    	        	    SEVector3f scale = new SEVector3f(currentChain.getCellWidth(), 1, currentChain.getCellHeight());
    	        	    SEObject obj = mMountPointArea.mList.get(k);
    	        	    obj.getUserTransParas().mTranslate = tp.mTranslate;//rotateV;
    	        	    obj.getUserTransParas().mRotate = tp.mRotate;//rotate;
    	        	    obj.getUserTransParas().mScale = scale;
    	        	    obj.setUserTransParas();
    	        	    obj.setVisible(true, true);
    	        	    k++;
    	    	    }
    	    }
    }
    public void changeLineObjectPosition(MountPointInfo mpi) {
    	    mObjectLine_new.setVisible(true, true);
    	    
    	    SEMountPointData mpd = mpi.mountPointInfo.mMPD;
    	    SEVector3f wallSpaceCoord = mpd.getTranslate().clone();
    	    //Log.i(TAG, "## mount point " + mpi.mountPointInfo.mIndex + ": " + mpd.getTranslate());
    	    wallSpaceCoord.mD[1] -= 5;
    	    SETransParas tp = null;
    	    SEVector3f scale = null;
    	    SEMountPointChain currentChain = this.getCurrentMountPointChain(mpi.wallIndex);
    	    if(getOnMoveObject().getObjectInfo().mIsNativeObject) {
    	    	    tp = createNativeObjectBoundingVolumeTransParas(mpi);
    	    	    scale = tp.mScale;
    	    } else {
    	        tp = this.createUserTransParasFromWallTransform(mpi.wallIndex, wallSpaceCoord);
    	        scale = new SEVector3f(currentChain.getCellWidth(), 1, currentChain.getCellHeight());
    	    }

    	    mObjectLine_new.getUserTransParas().mTranslate = tp.mTranslate;//rotateV;
    	    mObjectLine_new.getUserTransParas().mRotate = tp.mRotate;//rotate;
    	    mObjectLine_new.getUserTransParas().mScale = scale;
    	    mObjectLine_new.setUserTransParas();
    	    
    	    changeMountPointAreaPosition(mpi);
    	    /*
    	    String objectName = this.getWallName(mpi.wallIndex);
    	    SEObject object = getScene().findObject(objectName, 0);
    	    object.setVisible(false, true);
    	    */
    	    
    	    
    }
    public void showArea(int wallIndex, boolean createOrDestroy) {
        for(int i = 0 ; i < mMountPointArea.mList.size(); i++) {
                SEObject obj = mMountPointArea.mList.get(i);
                mHouse.removeChild(obj, true);
        }
        mMountPointArea.mList.clear();
        if(createOrDestroy == false) {
            return;
        }
        if(isObjectFixedInMountPoint(getOnMoveObject()))
            return;
	    SEObject object = getOnMoveObject();
	    //SEVector3f currentFingerWorldPoint = mRealLocation;//mObjectTransParas.mTranslate;
        SEVector3f coordWallSpace = mRealLocationInWall.clone();//this.toWallSpaceCoordinate(wallIndex, currentFingerWorldPoint);
        SEVector3f objectMinV = new SEVector3f();
        SEVector3f objectMaxV = new SEVector3f();
        object.createLocalBoundingVolume();
        object.getLocalBoundingVolume(objectMinV, objectMaxV);
        float xspan = objectMaxV.getX() - objectMinV.getX();
        float zspan = objectMaxV.getZ() - objectMinV.getZ();
        SEVector3f minCoordWallSpace = new SEVector3f(coordWallSpace.getX() - xspan / 2, -10, coordWallSpace.getZ() - zspan / 2);
        SEVector3f maxCoordWallSpace = new SEVector3f(coordWallSpace.getX() + xspan / 2, -10, coordWallSpace.getZ() + zspan / 2);
    
        SEMountPointChain currentChain = this.getCurrentMountPointChain(wallIndex);
	    //SEMountPointChain.BoundaryPoint boundaryPoint = mMountPointArea.mBoundaryPoint;
        SEMountPointChain.MatrixPoint minPoint = currentChain.getMatrixPointInPlaneXZ(minCoordWallSpace);
        SEMountPointChain.MatrixPoint maxPoint = currentChain.getMatrixPointInPlaneXZ(maxCoordWallSpace);
	    //SEMountPointChain.MatrixPoint startPoint = minPoint;
	    //SEMountPointChain.MatrixPoint endPoint = maxPoint;
        //Log.i(TAG, "## bound point = " + minPoint.row + ", " + minPoint.col + ", " + minPoint.mountPointIndex + 
        //		                            ", " + maxPoint.row + ", " + maxPoint.col + ", " + maxPoint.mountPointIndex);
	    SEMountPointChain.BoundaryPoint bp = new SEMountPointChain.BoundaryPoint(minPoint, maxPoint);
        /////////////////////////////////////
        mMountPointArea.mBoundaryPoint = bp;

        SEMountPointChain.MatrixPoint startMP = bp.startPoint;
        SEMountPointChain.MatrixPoint endMP = bp.endPoint;
        for(int i = endMP.row ; i <= startMP.row ; i++) {
            for(int j = startMP.col ; j <= endMP.col ; j++) {
                int index = currentChain.getIndex(i, j);
                SEMountPointData mpd = currentChain.getMountPointData(index);
                SEObject obj = new SEObject(getScene(), getOnMoveObject().mName + "_" + i + "_" + j + "line");
                createWallLine_new(obj, mpd, new Color(1, 0, 0));
                mHouse.addChild(obj, true);
                //obj.setUserTransParas();
                obj.setVisible(false, true);
                mMountPointArea.mList.add(obj);
            }
        }

    }
    public void showLine_new(SEMountPointData mpd, boolean createOrDestroy) {
    	
        if (createOrDestroy) {
            if (mObjectLine_new == null) {
                mObjectLine_new = new SEObject(getScene(), getOnMoveObject().mName + "_line1");
                createWallLine_new(mObjectLine_new, mpd, new Color(0, 1, 0));
                mHouse.addChild(mObjectLine_new, true);
                //mObjectLine_new.setUserTransParas();
                mObjectLine_new.setVisible(false, true);
            }
        } else {
            if (mObjectLine_new != null) {
                mObjectLine_new.getParent().removeChild(mObjectLine_new, true);
                mObjectLine_new = null;
            }
        }
    }
    public void showLine(boolean createOrDestory) {
        if (createOrDestory) {
            if (mObjectLine == null) {
                mObjectLine = new SEObject(getScene(), getOnMoveObject().mName + "_line");
                createWallLine(mObjectLine, getOnMoveObject().getObjectSlot().mSpanX,
                        getOnMoveObject().getObjectSlot().mSpanY);
                mHouse.addChild(mObjectLine, true);
                mObjectLine.setVisible(false, true);
            }
        } else {
            if (mObjectLine != null) {
                mObjectLine.getParent().removeChild(mObjectLine, true);
                mObjectLine = null;
            }
        }
    }
    private float mLineObjectWidth = 196;
    private float mLineObjectHeight = 195;
    private float lw = 1;
    private float lh = 1;
    private static class Color {
    	    public float r, g, b;
    	    public Color(float r, float g, float b) {
    	    	    this.r = r;
    	    	    this.g = g;
    	    	    this.b = b;
    	    }
    }
    private void createWallLine_new(SEObject lineObject , SEMountPointData mountPointData, Color color) {
    	    SEObjectData data = new SEObjectData(lineObject.getName());
    	    int lineNumber = 4;
    	    float[] vertexArray = new float[lineNumber * 6];
    	    //SEVector3f point = mountPointData.getTranslate();
    	    float centerX = 0;//point.getX();
    	    float centerY = 0;//point.getY();
    	    float centerZ = 0;//point.getZ();
    	    //0
    	    vertexArray[0] = centerX - lw / 2;
    	    vertexArray[1] = centerY;
    	    vertexArray[2] = centerZ + lh / 2;
    	    //1
    	    vertexArray[3] = centerX + lw / 2;
    	    vertexArray[4] = centerY;
    	    vertexArray[5] = centerZ + lh / 2;
    	    
    	    //1
    	    vertexArray[6] = centerX + lw / 2;
    	    vertexArray[7] = centerY;
    	    vertexArray[8] = centerZ + lh / 2;
    	    //2
    	    vertexArray[9] = centerX + lw / 2;
    	    vertexArray[10] = centerY;
    	    vertexArray[11] = centerZ - lh / 2;
    	    
    	    //2
    	    vertexArray[12] = centerX + lw / 2;
    	    vertexArray[13] = centerY;
    	    vertexArray[14] = centerZ - lh / 2;
    	    
    	    //3
    	    vertexArray[15] = centerX - lw / 2;
    	    vertexArray[16] = centerY;
    	    vertexArray[17] = centerZ - lh / 2;
    	    
    	    //3
    	    vertexArray[18] = centerX - lw / 2;
    	    vertexArray[19] = centerY;
    	    vertexArray[20] = centerZ - lh / 2;
    	    //0
    	    vertexArray[21] = centerX - lw / 2;
    	    vertexArray[22] = centerY;
    	    vertexArray[23] = centerZ + lh / 2;
    	    data.setVertexArray(vertexArray);
    	    int[] faceArray = new int[lineNumber * 2];
        for (int i = 0; i < lineNumber * 2; i++) {
            faceArray[i] = i;
        }
        data.setFaceArray(faceArray);
        data.setColor(new float[] { color.r, color.g, color.b });
        data.setObjectType(SEObjectData.OBJECT_TYPE_LINE);
        data.setImage(SEObjectData.IMAGE_TYPE_COLOR, null, null);
        data.setLineWidth(5);
        //lineObject.getUserTransParas().mScale = (new SEVector3f(mLineObjectWidth, 1, mLineObjectHeight));
        //lineObject.setUserTransParas();
        lineObject.setObjectData(data);
    }
    private void createWallLine(SEObject line, int spanX, int spanY) {
        SEObjectData data = new SEObjectData(line.getName());
        int lineNumber = 4;
        float[] vertexArray = new float[lineNumber * 6];
        float centerX = -spanX * mHouse.getWallUnitSizeX() / 2;
        float centerZ = -spanY * mHouse.getWallUnitSizeY() / 2;
        vertexArray[0] = centerX;
        vertexArray[1] = 0;
        vertexArray[2] = centerZ;

        vertexArray[3] = centerX;
        vertexArray[4] = 0;
        vertexArray[5] = spanY * mHouse.getWallUnitSizeY() + centerZ;

        vertexArray[6] = spanX * mHouse.getWallUnitSizeX() + centerX;
        vertexArray[7] = 0;
        vertexArray[8] = centerZ;

        vertexArray[9] = spanX * mHouse.getWallUnitSizeX() + centerX;
        vertexArray[10] = 0;
        vertexArray[11] = spanY * mHouse.getWallUnitSizeY() + centerZ;

        vertexArray[12] = centerX;
        vertexArray[13] = 0;
        vertexArray[14] = centerZ;

        vertexArray[15] = spanX * mHouse.getWallUnitSizeX() + centerX;
        vertexArray[16] = 0;
        vertexArray[17] = centerZ;

        vertexArray[18] = centerX;
        vertexArray[19] = 0;
        vertexArray[20] = spanY * mHouse.getWallUnitSizeY() + centerZ;

        vertexArray[21] = spanX * mHouse.getWallUnitSizeX() + centerX;
        vertexArray[22] = 0;
        vertexArray[23] = spanY * mHouse.getWallUnitSizeY() + centerZ;

        data.setVertexArray(vertexArray);

        int[] faceArray = new int[lineNumber * 2];
        for (int i = 0; i < lineNumber * 2; i++) {
            faceArray[i] = i;
        }
        data.setFaceArray(faceArray);
        data.setColor(new float[] { 1, 0, 0 });
        data.setObjectType(SEObjectData.OBJECT_TYPE_LINE);
        data.setImage(SEObjectData.IMAGE_TYPE_COLOR, null, null);
        data.setLineWidth(5);
        line.setObjectData(data);
    }

    private void updateWallLinePostion(ObjectSlot slot) {
        if (mObjectLine != null) {
            if (slot == null) {
                if (mObjectLine.isVisible()) {
                    mObjectLine.setVisible(false, true);
                }
                return;
            }
            if (!mObjectLine.isVisible()) {
                mObjectLine.setVisible(true, true);
            }
            float angle = slot.mSlotIndex * 360.f / mHouse.getCount();
            SEVector2f orientation = new SEVector2f((float) Math.cos(angle * Math.PI / 180), (float) Math.sin(angle
                    * Math.PI / 180));
            float radius = mHouse.getWallRadius();
            float x = (float) (-radius * orientation.getY());
            float y = (float) (radius * orientation.getX());
            float offsetX = (slot.mStartX + slot.mSpanX / 2.f) * mHouse.getWallUnitSizeX() - mHouse.getWallSpanX()
                    * mHouse.getWallUnitSizeX() / 2.f;
            SEVector2f offset = orientation.mul(offsetX);
            x = x + offset.getX();
            y = y + offset.getY();
            float offsetZ = mHouse.getWallSpanY() * mHouse.getWallUnitSizeY() - (slot.mStartY + slot.mSpanY / 2.f)
                    * mHouse.getWallUnitSizeY() + mHouse.getWallHeight();
            float z = offsetZ;
            mObjectLine.getUserTransParas().mTranslate.set(x, y, z);
            mObjectLine.getUserTransParas().mRotate.set(angle, 0, 0, 1);
            mObjectLine.setUserTransParas();

        }
    }
    @Override
    public void leaveLayer(NormalObject object) {
        int wallIndex = object.getObjectSlot().mSlotIndex;
        NormalObject originObj = object;
        if(object instanceof  IconBox) {
            originObj = object.getChangedToObj();
        }
        WallShelf shelf = mHouse.getWallShelfWithObject(wallIndex, originObj);
        if(shelf != null) {
            int mpIndex = originObj.getObjectSlot().mMountPointIndex;
            mHouse.removeObjectFromCurrentShelf(originObj, mpIndex, shelf);
        }
        object.setBoundaryPoint(null);
        object.getObjectSlot().mMountPointIndex = -1;
        object.getObjectSlot().mSlotIndex = -1;
        printAllShelf();
    }
    @Override
    public void handleOutsideRoom() {
        NormalObject currentObject = getOnMoveObject();
        if(currentObject instanceof IconBox) {
            Log.i(TAG, "current object change to iconbox");
        }
        int wallIndex = currentObject.getObjectSlot().mSlotIndex;

        WallShelf shelf = mHouse.getWallShelfWithObject(wallIndex, currentObject);
        if(shelf != null) {
            int mpIndex = currentObject.getObjectSlot().mMountPointIndex;
            mHouse.removeObjectFromCurrentShelf(currentObject, mpIndex, shelf);
        }
        if((currentObject instanceof WallShelf) && mCurrentShelfPressPosition == CLICK_SHELF_MID) {
            WallShelf wallShelf =  (WallShelf)currentObject;
            wallShelf.removeAllObjectOnShelfFromParent();
        }
        currentObject.handleOutsideRoom();
        printAllShelf();
    }

    @Override
    public void handleNoMoreRoom() {
        ToastUtils.showNoWallSpace();
        getOnMoveObject().handleNoMoreRoom();
    }
    private ArrayList<WallShelf> getAllWallShelfInHouse() {
        ArrayList<WallShelf> shelfList = new ArrayList<WallShelf>();
        for(SEObject obj : mHouse.mChildObjects) {
            if(!(obj instanceof WallShelf))
                continue;
            shelfList.add((WallShelf)obj);
        }
        return shelfList;
    }
    private void updateShelfInfo() {
        ArrayList<WallShelf> shelfList = getAllWallShelfInHouse();
        for(WallShelf shelf : shelfList) {
            shelf.updateObjectSlotDB();
            shelf.updateSlotDB();
        }
    }
    @Override
    public void handleSlotSuccess() {
        super.handleSlotSuccess();
        updateShelfInfo();
        NormalObject moveObject = getOnMoveObject();
        if (null != moveObject) {
            moveObject.calculateNativeObjectTransParas();
            moveObject.handleSlotSuccess();
        }
    }

    @Override
    public boolean placeObjectToVessel(NormalObject normalObject, final SEAnimFinishListener l) {
        super.placeObjectToVessel(normalObject, l);
        int currentWallIndex = mHouse.getWallNearestIndex();
        //SEObject preParent = normalObject.getParent();
        PlacedObjectsAfterMove pam = placeObjectOnCurrentWall(normalObject, currentWallIndex, null);
        if(pam != null) {
            setRealPlaceForPlacedObjectsAfterMove(pam);
            normalObject.changeParent(mHouse);
            normalObject.handleSlotSuccess();
            updateShelfInfo();
            return true;
        } else {
            //normalObject.changeParent(preParent);
            handleNoMoreRoom();
            return false;
        }
        /*
        mExistentObjects = getExistentObject();
        SEVector3f t = getFingerOnWallLocation(100, 100);
        SEVector3f coordInWall = this.toWallSpaceCoordinate(currentWallIndex, t);
        mLocationInWallForWidget = coordInWall;
        boolean ret = this.placeObjectOnWall(normalObject, currentWallIndex);
        if(ret == true) {
        	return true;
        } else {
        	normalObject.changeParent(preParent);
        	handleNoMoreRoom();
        	return false;
        }
        */
    }

    public void slotToWall(final SEAnimFinishListener l) {
        cancelConflictAnimationTask();
        mObjectSlot = calculateNearestSlot(mHouse.getWallNearestIndex(), mRealLocation);
        ObjectSlot wallSlot = null;
        List<ConflictObject> needMoveObjs = new ArrayList<ConflictObject>();
        List<ConflictObject> existentObjects = getExistentObject(mHouse.getWallNearestIndex());
        int sizeX = (int)mHouse.getWallSpanX();
        int sizeY = (int)mHouse.getWallSpanY();
        boolean[][] slot = new boolean[sizeY][sizeX];
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                slot[y][x] = true;
            }
        }
        ConflictObject conflictObject = new ConflictObject();
        conflictObject.mConflictObject = getOnMoveObject();
        conflictObject.mMoveSlot = mObjectSlot;
        List<ConflictObject> conflictObjects = caculateConflictObjs(existentObjects, mObjectSlot);
        if (conflictObjects == null
                || canPlaceToNewSlot(existentObjects, conflictObject, slot, conflictObjects, needMoveObjs, true)) {
            updateWallLinePostion(mObjectSlot);
            playConflictAnimationTask(needMoveObjs, 0);
            wallSlot = mObjectSlot;
        } else {
            updateWallLinePostion(null);
            cancelConflictAnimationTask();
            wallSlot = getWallNearestSlot(mObjectSlot);
        }

        if (wallSlot == null) {
            stopMoveAnimation();
            handleNoMoreRoom();
        } else {
            playSlotAnimation(wallSlot, l);
        }
    }

    private ObjectSlot getWallNearestSlot(ObjectSlot cmpSlot) {
        if (cmpSlot == null) {
            return null;
        }
        int count = 0;
        while (count <= mHouse.getCount() / 2) {
            int indexRight;
            int indexLeft;
            if (count == 0) {
                indexLeft = indexRight = cmpSlot.mSlotIndex;
            } else {
                indexRight = cmpSlot.mSlotIndex - count;
                if (indexRight < 0) {
                    indexRight = mHouse.getCount() + indexRight;
                }
                indexLeft = cmpSlot.mSlotIndex + count;
                if (indexLeft >= mHouse.getCount()) {
                    indexLeft = indexLeft - mHouse.getCount();
                }
            }
            List<ObjectSlot> emptySlots;
            if (indexLeft == indexRight) {
                emptySlots = searchEmptySlot(indexLeft, cmpSlot.mSpanX, cmpSlot.mSpanY);
            } else {
                emptySlots = searchEmptySlot(indexLeft, cmpSlot.mSpanX, cmpSlot.mSpanY);
                List<ObjectSlot> emptySlots2 = searchEmptySlot(indexRight, cmpSlot.mSpanX, cmpSlot.mSpanY);
                if (emptySlots != null) {
                    if (emptySlots2 != null)
                        emptySlots.addAll(emptySlots2);
                } else {
                    emptySlots = emptySlots2;
                }
            }
            if (emptySlots != null) {
                return searchNearestSlot(emptySlots, cmpSlot);
            }
            count++;
        }
        return null;
    }

    private void playSlotAnimation(final ObjectSlot wallSlot, final SEAnimFinishListener l) {
        stopMoveAnimation();
        getOnMoveObject().changeParent(mHouse);
        final SETransParas srcTransParas = worldToWall(getOnMoveObject().getUserTransParas());
        getOnMoveObject().getUserTransParas().set(srcTransParas);
        getOnMoveObject().setUserTransParas();
        getOnMoveObject().getObjectSlot().set(wallSlot);
        final SETransParas desTransParas = mHouse.getSlotTransParas(getOnMoveObject().getObjectInfo(), getOnMoveObject());
        mHouse.toFace(wallSlot.mSlotIndex, new SEAnimFinishListener() {
            public void onAnimationfinish() {
                updateWallLinePostion(wallSlot);
                mSetToRightPositionAnimation = new SetToRightPositionAnimation(getScene(), getOnMoveObject(),
                        srcTransParas, desTransParas, 7);
                mSetToRightPositionAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                    public void onAnimationfinish() {
                        handleSlotSuccess();
                        if (l != null) {
                            l.onAnimationfinish();
                        }
                    }
                });
                mSetToRightPositionAnimation.execute();
            }
        }, 5);
    }

    private SETransParas worldToWall(SETransParas worldTransParas) {
        SETransParas wallTransParas = new SETransParas();
        SEVector2f touchLocZ = worldTransParas.mTranslate.getVectorZ();
        float objectToWorldAngle = (float) (touchLocZ.getAngle_II() * 180 / Math.PI);
        float wallToWorldAngle = mHouse.getAngle();
        float objectLocationAngle = objectToWorldAngle - wallToWorldAngle;
        float r = touchLocZ.getLength();
        float x = (float) (-r * Math.sin(objectLocationAngle * Math.PI / 180));
        float y = (float) (r * Math.cos(objectLocationAngle * Math.PI / 180));
        float z = worldTransParas.mTranslate.getZ();
        float objectToWallAngle = worldTransParas.mRotate.getAngle() - wallToWorldAngle;
        wallTransParas.mTranslate.set(x, y, z);
        wallTransParas.mRotate.set(objectToWallAngle, 0, 0, 1);
        wallTransParas.mScale = worldTransParas.mScale.clone();
        return wallTransParas;
    }
    private static final int TRANSFORM_LOCAL = 0;
    private static final int TRANSFORM_WORLD = 1;
    private static final int TRANSFORM_USER = 2;
    private static final int TRANSFORM_IN_LOCAL = 3;
    private class SetToRightPositionAnimation_New extends CountAnimation {
        private SETransParas mSrcTransParas;
        private SETransParas mDstTransParas;
        private NormalObject mObject;
        private int mCount;
        private int mTransformStyle;
        private SERotate mWallRotate;
        private SEVector3f mWallTranslate;
        public SetToRightPositionAnimation_New(SEScene scene, NormalObject obj, SETransParas srcTransParas,
        		     SETransParas dstTransParas, SERotate wallRotate, SEVector3f wallTranslate, int count) {
        	    super(scene);
        	    mSrcTransParas = srcTransParas;
        	    mDstTransParas = dstTransParas;
        	    mObject = obj;
        	    mCount = count;
        	    mWallRotate = wallRotate;
        	    mWallTranslate = wallTranslate;
        	    //mTransformStyle = transformStyle;
        }
        public void runPatch(int count) {
        	    float t = count / (float)mCount;
        	    if(count == mCount) {
        	    	    //Log.i(TAG, "count == mCOunt");
        	    }
        	    SEVector3f srcTranslate = mSrcTransParas.mTranslate;
        	    SEVector3f dstTranslate = mDstTransParas.mTranslate;
        	    SEVector3f deltaTranslate = dstTranslate.subtract(srcTranslate);
        	    SEVector3f currTranslate = srcTranslate.add(deltaTranslate.mul(t));
        	    
        	    SEVector3f rotateV = SEObject.rotateMapPoint(mWallRotate, currTranslate);
        	    rotateV = rotateV.add(mWallTranslate);
        	    mObject.getUserTransParas().mTranslate = rotateV;
        	    mObject.setUserTransParas();
        }
        @Override
        public void onFirstly(int count) {
        
        }

        @Override
        public int getAnimationCount() {
            return mCount;
        }
    }
    private class SetToRightPositionAnimationWithPlacedObjects extends CountAnimation {
        private SETransParas mSrcCurrentMoveObjectParas;
        private SETransParas mDstCurrentMoveObjectParas;
        private NormalObject mObject;
        private ArrayList<NormalObject> mPlacedObjects;
        private ArrayList<SETransParas> mSrcPlacedObjectsParas;
        private ArrayList<SETransParas> mDstPlacedObjectsParas;
        private float mStep;
        private int mCount;
        private SEVector3f mMovedObjectTranslateDelta;
        private SEVector3f mMovedObjectScaleDelta;
        public SetToRightPositionAnimationWithPlacedObjects(SEScene scene, NormalObject obj, ArrayList<NormalObject> placedObjs, SETransParas srcCurrentMoveObjectParas,
                                                            SETransParas dstCurrentMoveObjectParas,
                                                            ArrayList<SETransParas> srcPlacedObjectsParas,
                                                            ArrayList<SETransParas> dstPlacedObjectsParas, int count) {
            super(scene);
            mCount = count;
            mSrcCurrentMoveObjectParas = srcCurrentMoveObjectParas;
            mDstCurrentMoveObjectParas = dstCurrentMoveObjectParas;
            mSrcPlacedObjectsParas = srcPlacedObjectsParas;
            mDstPlacedObjectsParas = dstPlacedObjectsParas;
            mObject = obj;
            mPlacedObjects = placedObjs;
            mMovedObjectTranslateDelta = mDstCurrentMoveObjectParas.mTranslate.subtract(mSrcCurrentMoveObjectParas.mTranslate);
            mMovedObjectScaleDelta = mDstCurrentMoveObjectParas.mScale.subtract(mSrcCurrentMoveObjectParas.mScale);
        }

        public void runPatch(int count) {
            float step = mStep * count;
            mObject.getUserTransParas().mTranslate = mSrcCurrentMoveObjectParas.mTranslate.add(mMovedObjectTranslateDelta.mul(step));
            mObject.getUserTransParas().mScale = mSrcCurrentMoveObjectParas.mScale.add(mMovedObjectScaleDelta.mul(step));
            mObject.setUserTransParas();
        }

        @Override
        public void onFirstly(int count) {
            mStep = 1f / getAnimationCount();
        }

        @Override
        public int getAnimationCount() {
            return mCount;
        }
    }
    private class SetToRightPositionAnimation extends CountAnimation {
        private SETransParas mSrcTransParas;
        private SETransParas mDesTransParas;
        private NormalObject mObject;
        private int mCount;
        private float mStep;

        public SetToRightPositionAnimation(SEScene scene, NormalObject obj, SETransParas srcTransParas,
                SETransParas desTransParas, int count) {
            super(scene);
            mObject = obj;
            mSrcTransParas = srcTransParas;
            mDesTransParas = desTransParas;
            mCount = count;
        }

        public void runPatch(int count) {
            float step = mStep * count;
            mObject.getUserTransParas().mTranslate = mSrcTransParas.mTranslate.add(mDesTransParas.mTranslate.subtract(
                    mSrcTransParas.mTranslate).mul(step));
            mObject.getUserTransParas().mScale = mSrcTransParas.mScale.add(mDesTransParas.mScale.subtract(
                    mSrcTransParas.mScale).mul(step));
            float desAngle = mDesTransParas.mRotate.getAngle();
            float srcAngle = mSrcTransParas.mRotate.getAngle();
            if (desAngle - srcAngle > 180) {
                desAngle = desAngle - 360;
            } else if (desAngle - srcAngle < -180) {
                desAngle = desAngle + 360;
            }
            float curAngle = srcAngle + (desAngle - srcAngle) * step;
            mObject.getUserTransParas().mRotate.set(curAngle, 0, 0, 1);
            mObject.setUserTransParas();
        }

        @Override
        public void onFirstly(int count) {
            mStep = 1f / getAnimationCount();
        }

        @Override
        public int getAnimationCount() {
            return mCount;
        }
    }

    private List<ObjectSlot> searchEmptySlot(int faceIndex, int spanX, int spanY) {

        List<ObjectSlot> existentSlot = new ArrayList<ObjectSlot>();

        for (ConflictObject wallObject : mExistentObjects) {
            if (wallObject.mConflictObject.getObjectInfo().getSlotIndex() == faceIndex) {
                existentSlot.add(wallObject.mConflictObject.getObjectInfo().mObjectSlot);
            }
        }

        return searchEmptySlot(faceIndex, spanX, spanY, existentSlot);
    }

    private List<ObjectSlot> searchEmptySlot(int faceIndex, int spanX, int spanY, List<ObjectSlot> existentSlot) {
        int sizeX = (int)mHouse.getWallSpanX() ;
        int sizeY = (int)mHouse.getWallSpanY() ;
        boolean[][] slot = new boolean[sizeY][sizeX];

        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                slot[y][x] = true;
            }
        }
        for (ObjectSlot objectSlot : existentSlot) {
            int startY = objectSlot.mStartY;
            int startX = objectSlot.mStartX;
            float sX = objectSlot.mSpanX;
            float sY = objectSlot.mSpanY;
            for (int y = startY; y < startY + sY; y++) {
                if (y < sizeY) {
                    for (int x = startX; x < startX + sX; x++) {
                        if (x < sizeX) {
                            slot[y][x] = false;
                        }
                    }
                }
            }
        }
        return searchEmptySlot(faceIndex, spanX, spanY, slot);
    }

    private List<ObjectSlot> searchEmptySlot(int faceIndex, int spanX, int spanY, boolean[][] slot) {
        int sizeX = (int)mHouse.getWallSpanX();
        int sizeY = (int)mHouse.getWallSpanY();
        List<ObjectSlot> objectSlots = null;
        for (int j = 0; j <= sizeY - spanY; j++) {
            for (int i = 0; i <= sizeX - spanX; i++) {
                boolean hasSlot = true;
                for (int n = j; n < j + spanY; n++) {
                    for (int m = i; m < i + spanX; m++) {
                        if (!slot[n][m]) {
                            hasSlot = false;
                            break;
                        }
                    }
                    if (!hasSlot)
                        break;
                }
                if (hasSlot) {
                    ObjectSlot objectSlot = new ObjectSlot();
                    objectSlot.mSlotIndex = faceIndex;
                    objectSlot.mStartX = i;
                    objectSlot.mStartY = j;
                    objectSlot.mSpanX = spanX;
                    objectSlot.mSpanY = spanY;
                    if (objectSlots == null) {
                        objectSlots = new ArrayList<ObjectSlot>();
                    }
                    objectSlots.add(objectSlot);
                }
            }
        }
        return objectSlots;
    }

    private ObjectSlot searchNearestSlot(List<ObjectSlot> emptySlots, ObjectSlot cmpSlot) {
        ObjectSlot nearestSlot = new ObjectSlot();
        float minDistance = Float.MAX_VALUE;
        for (ObjectSlot emptySlot : emptySlots) {
            float distance = getSlotPosition(cmpSlot).subtract(getSlotPosition(emptySlot)).getLength();
            if (distance < minDistance) {
                minDistance = distance;
                nearestSlot = emptySlot.clone();
            }
        }
        return nearestSlot;
    }

    private SEVector3f getSlotPosition(ObjectSlot objectSlot) {
        float angle = objectSlot.mSlotIndex * 360.f / mHouse.getCount();
        SEVector2f yDirection = new SEVector2f((float) Math.cos((angle + 90) * Math.PI / 180),
                (float) Math.sin((angle + 90) * Math.PI / 180));
        SEVector2f xDirection = new SEVector2f((float) Math.cos(angle * Math.PI / 180), (float) Math.sin(angle
                * Math.PI / 180));
        float offsetY = mHouse.getWallRadius() ;
        float offsetX = (objectSlot.mStartX + objectSlot.mSpanX / 2.f) * mHouse.getWallUnitSizeX()
                - mHouse.getWallSpanX() * mHouse.getWallUnitSizeX() / 2.f;
        SEVector2f offset = yDirection.mul(offsetY).add(xDirection.mul(offsetX));
        float offsetZ = mHouse.getWallSpanY() * mHouse.getWallUnitSizeY()
                - (objectSlot.mStartY + objectSlot.mSpanY / 2.f) * mHouse.getWallUnitSizeY() + mHouse.getWallHeight();
        float z = offsetZ;
        return new SEVector3f(offset.getX(), offset.getY(), z);
    }

    private class ConflictObject {
        public ConflictAnimation mConflictAnimation;
        public NormalObject mConflictObject;
        public ObjectSlot mMoveSlot;

        private void playConflictAnimation() {
            if (mConflictAnimation != null) {
                mConflictAnimation.stop();
            }
            if (mMoveSlot == null) {
                return;
            }
            mConflictObject.getObjectSlot().set(mMoveSlot);
            mConflictAnimation = new ConflictAnimation(getScene(), 3);
            mConflictAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    mConflictObject.getObjectInfo().updateSlotDB();
                }
            });
            mConflictAnimation.execute();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            ConflictObject cmp = (ConflictObject) obj;
            return (mConflictObject.equals(cmp.mConflictObject));
        }

        private class ConflictAnimation extends CountAnimation {
            private SETransParas mDesTransParas;
            private SETransParas mSrcTransParas;
            private float mStep;
            private float mCurProcess;
            private boolean mIsEnbaleBlending;
            private boolean mHasGetBlending;

            public ConflictAnimation(SEScene scene, float step) {
                super(scene);
                mDesTransParas = mHouse.getSlotTransParas(mConflictObject.getObjectInfo(), mConflictObject);
                mSrcTransParas = mConflictObject.getUserTransParas().clone();
                mStep = step;
                mCurProcess = 0;
            }

            @Override
            public void runPatch(int count) {
                float needTranslate = 100 - mCurProcess;
                float absNTX = Math.abs(needTranslate);
                if (absNTX <= mStep) {
                    mCurProcess = 100;
                    stop();
                } else {
                    int step = (int) (mStep * Math.sqrt(absNTX));
                    if (needTranslate < 0) {
                        step = -step;
                    }
                    mCurProcess = mCurProcess + step;

                }
                float step = mCurProcess / 100;
                mConflictObject.getUserTransParas().mTranslate = mSrcTransParas.mTranslate
                        .add(mDesTransParas.mTranslate.subtract(mSrcTransParas.mTranslate).mul(step));
                mConflictObject.getUserTransParas().mScale = mSrcTransParas.mScale.add(mDesTransParas.mScale.subtract(
                        mSrcTransParas.mScale).mul(step));
                float desAngle = mDesTransParas.mRotate.getAngle();
                float srcAngle = mSrcTransParas.mRotate.getAngle();
                if (desAngle - srcAngle > 180) {
                    desAngle = desAngle - 360;
                } else if (desAngle - srcAngle < -180) {
                    desAngle = desAngle + 360;
                }
                float curAngle = srcAngle + (desAngle - srcAngle) * step;
                mConflictObject.getUserTransParas().mRotate.set(curAngle, 0, 0, 1);
                mConflictObject.setUserTransParas();
            }

            @Override
            public void onFirstly(int count) {
                if (!mHasGetBlending) {
                    mIsEnbaleBlending = mConflictObject.isBlendingable();
                    mHasGetBlending = true;
                }
                if (!mIsEnbaleBlending) {
                    mConflictObject.setBlendingable(true, true);
                }
                mConflictObject.setAlpha(0.1f, true);
            }

            @Override
            public void onFinish() {
                mConflictObject.getUserTransParas().set(mDesTransParas);
                mConflictObject.setUserTransParas();
                if (mHasGetBlending) {
                    mConflictObject.setAlpha(1, true);
                    if (!mIsEnbaleBlending) {
                        mConflictObject.setBlendingable(false, true);
                    } else {
                        mConflictObject.setBlendingable(true, true);
                    }
                }
            }
        }
    }

}
