package com.borqs.framework3d.home3d;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.borqs.se.engine.SEVector;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.engine.SEScene;

import android.util.Log;
public class SEMountPointChain {
	private static final String TAG = "SEMountPointChain";
    private String mObjectName;// this mount point chain is for object which has this name
                               // if mObjectName is not null, this mount chain is defined in vessel
    private String mVesselName;// this mount point chain is for vessel which has this name
                               // if mVesselName is not null, this mount chain is defined in object
    private String mContainerName;// the type of object which the mount point is used for
    private ArrayList<SEMountPointData> mMountPointDataList = new ArrayList<SEMountPointData>();
    private ArrayList<ArrayList<Integer>> mMountPointNeighborIndexList = new ArrayList<ArrayList<Integer>>();
    
    public static final String DEFAULT_VESSEL_NAME = "**DEFAULT**";
    public static final int ROW_COL_MATRIX = 1;
    public static final int ROW_COL_FREE = 2;
    ///// these value must read from xml file
    private int mRowColType;
    private float mCellWidth = 196;
    private float mCellHeight = 195;
    private int mRowCount = 5;
    private int mColCount = 4;
    private SEVector3f mMinPoint;
    private SEVector3f mMaxPoint;
    private QuatTree mQuatTree = new QuatTree();
    /////// end
    private static class QuatTreeNode {
    	    public SEVector3f minPoint;
    	    public SEVector3f maxPoint;
    	    public ArrayList<Integer> indexList = new ArrayList<Integer>();
    	    public QuatTreeNode[] children = new QuatTreeNode[4];
    }
    private class QuatTree {
    	    private QuatTreeNode rootNode;
    	    public void create() {
    	    	    int[] indexArray1 = {0, 1, 4, 5};
    	    	    int[] indexArray2 = {2, 3, 6, 7};
    	    	    int[] indexArray3 = {8, 9, 12, 13, 16, 17};
    	    	    int[] indexArray4 = {10, 11, 14, 15, 18, 19};
    	    	    QuatTreeNode c1 = new QuatTreeNode();
    	    	    SEMountPointData mpdMax = mMountPointDataList.get(1);
    	    	    SEMountPointData mpdMin = mMountPointDataList.get(4);
    	    	    c1.minPoint = mpdMin.mMinPoint;
    	    	    c1.maxPoint = mpdMax.mMaxPoint;
    	    	    for(int i = 0 ; i < indexArray1.length ; i++) {
    	    	    	    c1.indexList.add(Integer.valueOf(indexArray1[i]));
    	    	    }
    	    	    ///////
    	    	    QuatTreeNode c2 = new QuatTreeNode();
    	    	    mpdMax = mMountPointDataList.get(3);
    	    	    mpdMin = mMountPointDataList.get(6);
    	    	    c2.minPoint = mpdMin.mMinPoint;
    	    	    c2.maxPoint = mpdMax.mMaxPoint;
    	    	    for(int i = 0 ; i < indexArray2.length ; i++) {
    	    	    	    c2.indexList.add(Integer.valueOf(indexArray2[i]));
    	    	    }
    	    	    //////
    	    	    QuatTreeNode c3 = new QuatTreeNode();
    	    	    mpdMax = mMountPointDataList.get(9);
    	    	    mpdMin = mMountPointDataList.get(16);
    	    	    c3.minPoint = mpdMin.mMinPoint;
    	    	    c3.maxPoint = mpdMax.mMaxPoint;
    	    	    for(int i = 0 ; i < indexArray3.length ; i++) {
    	    	    	    c3.indexList.add(Integer.valueOf(indexArray3[i]));
    	    	    }
    	    	    /////
    	    	    QuatTreeNode c4 = new QuatTreeNode();
    	    	    mpdMax = mMountPointDataList.get(11);
    	    	    mpdMin = mMountPointDataList.get(18);
    	    	    c4.minPoint = mpdMin.mMinPoint;
    	    	    c4.maxPoint = mpdMax.mMaxPoint;
    	    	    for(int i = 0 ; i < indexArray4.length; i++) {
    	    	    	    c4.indexList.add(Integer.valueOf(indexArray4[i]));
    	    	    }
    	    	    rootNode = new QuatTreeNode();
    	    	    rootNode.children[0] = c1;
    	    	    rootNode.children[1] = c2;
    	    	    rootNode.children[2] = c3;
    	    	    rootNode.children[3] = c4;
    	    	    
    	    }
    	    public int getIndex(SEVector3f point) {
    	    	    for(int i = 0 ; i < 4 ; i++) {
    	    	    	    QuatTreeNode c = rootNode.children[i];
    	    	    	    if(SEMountPointChain.isInRect(point, c.minPoint, c.maxPoint)) {
    	    	    	    	    for(int j = 0 ; j < c.indexList.size() ; j++) {
    	    	    	    	    	    int index = c.indexList.get(j).intValue();
    	    	    	    	    	    SEMountPointData mpd = mMountPointDataList.get(index);
    	    	    	    	    	    if(SEMountPointChain.isInRect(point, mpd.mMinPoint, mpd.mMaxPoint)) {
    	    	    	    	    	    	    return index;
    	    	    	    	    	    }
    	    	    	    	    }
    	    	    	    }
    	    	    }
    	    	    return -1;
    	    }
    }
    private static boolean isInRect(SEVector3f point, SEVector3f min, SEVector3f max) {
    	    if(point.getX() <= max.getX() && point.getX() >= min.getX() &&
    	    	   point.getZ() <= max.getZ() && point.getZ() >= min.getZ()) {
    	    	    return true;
    	    } else  {
    	    	    return false;
    	    }
    }
    public SEMountPointChain(String objectName, String vesselName, String forObjectType)
    {
    	    mObjectName = objectName;
    	    mVesselName = vesselName;
            mContainerName = forObjectType;
    	    mRowColType = ROW_COL_FREE;
    }
    public int getColCount() {
    	    return mColCount;
    }
    public int getRowCount() {
    	    return mRowCount;
    }
    public void setRowColType(int type) {
    	    mRowColType = type;
    }
    public int getRowColType() {
    	    return mRowColType;
    }
    void addMountPointData(SEMountPointData mpd)
    {
    	    mMountPointDataList.add(mpd);
    }
    void addMountPointData(String name, SEVector3f t, SEVector3f s, SERotate r)
    {
    	    SEMountPointData mpd = new SEMountPointData(name, t, s, r);
    	    mMountPointDataList.add(mpd);
    }
    public int getMountPointCount()
    {
    	    return mMountPointDataList.size();
    }
    public SEMountPointData getMountPointData(int index) {
        if(index < 0 || index >= mMountPointDataList.size()) {
            final StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("getMountPointData, met error with index = ").append(index).append(", size = ").
                    append(mMountPointDataList.size()).append(", objName = ").append(mObjectName).
                    append(", vessel name = ").append(mVesselName).append(", rowNum = ").append(mRowCount).
                    append(", colNum = ").append(mColCount);
            HomeUtils.reportError(errorMsg.toString());
            Log.e(TAG, errorMsg.toString());
            return null;
        }

        return mMountPointDataList.get(index);
    }

    public String getVesselName(){
    	    return mVesselName;
    }
    public String getContainerName() {
        return mContainerName;
    }
    public String getObjectName() {
    	    return mObjectName;
    }
    public float getCellWidth() {
    	    return mCellWidth;
    }
    public float getCellHeight() {
    	    return mCellHeight;
    }
    public static class ClosestMountPointData {
       	public SEMountPointData mMPD;
		public int mIndex;
		public float mDist;
    }
    public static class MatrixPoint {
        public int row;
        public int col;
        public int mountPointIndex;
        public MatrixPoint(int r, int c, int index) {
                row = r;
                col = c;
                mountPointIndex = index;
        }
        public MatrixPoint() {
                row = col = -1;
                mountPointIndex = -1;
        }
        @Override
        public MatrixPoint clone() {
                MatrixPoint mp = new MatrixPoint(row, col, mountPointIndex);
                return mp;
        }
        public boolean equalRowCol(MatrixPoint p) {
            if(row == p.row && col == p.col) {
                return true;
            }
            return false;
        }
        @Override
        public boolean equals(Object o) {
                if(o == null) {
                        return false;
                }
                MatrixPoint p = (MatrixPoint)o;
                if(row == p.row && col == p.col && mountPointIndex == p.mountPointIndex) {
                        return true;
                } else {
                        return false;
                }
        }
        public static MatrixPoint getSize(MatrixPoint p1, MatrixPoint p2) {
                int rowCount = Math.abs(p1.row - p2.row) + 1;
                int colCount = Math.abs(p1.col - p2.col) + 1;
                return new MatrixPoint(rowCount, colCount, -1);
        }
    }
    public static class BoundaryPoint {
    	    public MatrixPoint startPoint;
    	    public MatrixPoint endPoint;
    	    public BoundaryPoint() {
    	    	    startPoint = new MatrixPoint(-1, -1, -1);
    	    	    endPoint = new MatrixPoint(-1, -1, -1);
    	    }
    	    public BoundaryPoint(MatrixPoint min, MatrixPoint max) {
    	    	    startPoint = min.clone();
    	    	    endPoint = max.clone();
    	    }
    	    @Override
    	    public BoundaryPoint clone() {
    	    	    return new BoundaryPoint(startPoint, endPoint);
    	    }
    }
    private static class MountPointDistComparator implements Comparator<SEMountPointData> {
    	    private SEMountPointData comparedPoint;
    	    private final static float ELLIP = 1.0f;
    	    public MountPointDistComparator(SEMountPointData p) {
    	    	    comparedPoint = p;
    	    }
    	    public int compare(SEMountPointData p1, SEMountPointData p2) {
        	    float dist1 = p1.getTranslate().dist(comparedPoint.getTranslate());
        	    float dist2 = p2.getTranslate().dist(comparedPoint.getTranslate());
        	    p1.setDistToComparedPoint(dist1);
        	    p2.setDistToComparedPoint(dist2);
        	    float delta = dist1 - dist2;
        	    float absD = Math.abs(delta);
        	    if(absD < ELLIP) {
        	    	    if(p1.getTranslate().getX() < p2.getTranslate().getX()) {
        	    	    	    return -1;
        	    	    } else {
        	    	    	    absD = Math.abs(p1.getTranslate().getX()) - Math.abs(p2.getTranslate().getX());
        	    	    	    if(Math.abs(absD) < ELLIP) {
        	    	    	    	    //p1 and p2 has equal x
        	    	    	    	    if(p1.getTranslate().getZ() > p2.getTranslate().getZ()) {
        	    	    	    	    	    return -1;
        	    	    	    	    } else {
        	    	    	    	    	    absD = Math.abs(p1.getTranslate().getZ()) - Math.abs(p2.getTranslate().getZ());
        	    	    	    	    	    if(Math.abs(absD) < ELLIP) {
        	    	    	    	    	    	    return 0;
        	    	    	    	    	    } else {
        	    	    	    	    	    	    return 1;
        	    	    	    	    	    }
        	    	    	    	    	    
        	    	    	    	    }
        	    	    	    } else {
        	    	    	    	    return 1;
        	    	    	    }
        	    	    }
        	    } else {
        	    	    if(delta < 0) {
        	    	    	    return -1;
        	    	    } else {
        	    	    	    return 1;
        	    	    }
        	    }
        }
    	  
    	    public boolean equals(SEMountPointData p){
    	    	    float dist1 = p.getTranslate().dist(comparedPoint.getTranslate());
    	    	    if(Math.abs(dist1) < 0.01) {
    	    	    	    return true;
    	    	    } else {
    	    	    	    return false;
    	    	    }
    	    }
    }
    public ArrayList<SEMountPointData> getMountPointList() {
    	    return mMountPointDataList;
    }
    //return the closet sequence: right, top, left, bottom
    private static final int AT_RIGHT = 0;
    private static final int AT_TOP = 1;
    private static final int AT_LEFT = 2;
    private static final int AT_BOTTOM = 3;
    private int getIndex(SEMountPointData mpd) {
    	    for(int i = 0 ; i < mMountPointDataList.size() ; i++) {
    	    	    SEMountPointData p = mMountPointDataList.get(i);
    	    	    if(p == mpd) {
    	    	    	    return i;
    	    	    }
    	    }
    	    return -1;
    }
    private ArrayList<Integer> createNeighborIndexList(SEMountPointData mpd , int index) {
    	    for(int i = 0 ; i < mMountPointDataList.size() ; i++) {
    	    	    mMountPointDataList.get(i).setDistToComparedPoint(0);
    	    }
    	    ArrayList<SEMountPointData> mountPointList = (ArrayList<SEMountPointData>)mMountPointDataList.clone();
    	    MountPointDistComparator comp = new MountPointDistComparator(mpd);

    	    Collections.sort(mountPointList, comp);
    	    SEMountPointData first = mountPointList.get(0);
    	    assert(first == mpd);
    	    int firstIndex = getIndex(mpd);
    	    mpd.saveDistToPoint(firstIndex, 0);
    	    ArrayList<Integer> indexArrayList = new ArrayList<Integer>();
    	    for(int i = 1; i < mountPointList.size() ; i++) {
    	    	    SEMountPointData tmpMPD = mountPointList.get(i);
    	    	    int tmpIndex = getIndex(tmpMPD);
    	    	    indexArrayList.add(Integer.valueOf(tmpIndex));
    	    	    mpd.saveDistToPoint(tmpIndex, tmpMPD.getDistToComparedPoint());
    	    }
        return indexArrayList;    	    
    }
    public void createMountPointBoundary() {
    	if(mRowColType != ROW_COL_MATRIX) {
            return;
    	}
    	for(int i = 0 ; i < mMountPointDataList.size() ; i++) {
    	        SEMountPointData mpd = mMountPointDataList.get(i);
    	        SEVector3f min = new SEVector3f();
    	        SEVector3f max = new SEVector3f();
    	        getMountPointBoundaryInPlaneXZ(i, min, max);
    	        mpd.mMinPoint = min;
    	        mpd.mMaxPoint = max;
        }
    	    //TODO: fix the magic number 20
    	    if(mMountPointDataList.size() == 20) {
    	        mQuatTree.create();
    	    }
    }
    public void createMountPointIndexNeighbor() {
    	if(mRowColType != ROW_COL_MATRIX) {
    	    return;
    	}
	    for(int i = 0 ; i < mMountPointDataList.size() ; i++) {
	    	    SEMountPointData mpd = mMountPointDataList.get(i);
	    	    mpd.setDistToPointListWithSize(mMountPointDataList.size());
	    }
	    for(int i = 0 ; i < mMountPointDataList.size() ; i++){
	    	    SEMountPointData mpd = mMountPointDataList.get(i);
	    	    ArrayList<Integer> indexArray = createNeighborIndexList(mpd, i);
	    	    mMountPointNeighborIndexList.add(indexArray);
	    }
    }
    private static class MountPointLeftRightTopBottomComparator implements Comparator<SEMountPointData> {
	    private final static float ELLIP = 2.0f;
        private int planeType;
        public MountPointLeftRightTopBottomComparator(int planeType) {
        	    this.planeType = planeType;
        }
        private float getFirstDelta(SEVector3f t1, SEVector3f t2) {
        	    switch(planeType) {
        	    case SEVector.XZ_PLANE:
        	    	    return t1.getZ() - t2.getZ();
        	    default:
        	    	    SEDebug.myAssert(false, "not implement");
        	    }
        	    return 0;
        }
        private float getSecondDelta(SEVector3f t1 , SEVector3f t2) {
        	    switch(planeType) {
    	        case SEVector.XZ_PLANE:
    	    	        return t1.getX() - t2.getX();
    	        default:
    	    	        SEDebug.myAssert(false, "not implement");
    	        }
    	        return 0;
        }
	    public int compare(SEMountPointData p1, SEMountPointData p2) {
	    	    SEVector3f t1 = p1.getTranslate();
	    	    SEVector3f t2 = p2.getTranslate();
	    	    float deltaFirst = getFirstDelta(t1, t2);
	    	    if(Math.abs(deltaFirst) < ELLIP) {
	    	    	    float deltaSecond = getSecondDelta(t1, t2);
	    	    	    if(Math.abs(deltaSecond) < ELLIP) {
	    	    	    	    SEDebug.myAssert(false, "two x equal");
	    	    	    	    return 0;
	    	    	    } else {
	    	    	    	    if(deltaSecond < 0 ) {
	    	    	    	    	    return -1; 
	    	    	    	    } else {
	    	    	    	    	    return 1;
	    	    	    	    }
	    	    	    }
	    	    } else {
	    	    	    if(deltaFirst < 0) {
	    	    	        return 1;	 
	    	    	    } else {
	    	    	    	    return -1;
	    	    	    }
	    	    }
	    }
	    
	}
    private float getHorzDelta(SEVector3f t1, SEVector3f t2, int planeType) {
   	    switch(planeType) {
	    case SEVector.XZ_PLANE:
	    	    return t1.getX() - t2.getX();
	    }
	    SEDebug.myAssert(false, "not implement");
	    return 0; 
    }
    private float getVerticalDelta(SEVector3f t1, SEVector3f t2, int planeType) {
   	    switch(planeType) {
	    case SEVector.XZ_PLANE:
	    	    return t1.getZ() - t2.getZ();
	    }
	    SEDebug.myAssert(false, "not implement");
	    return 0;
    }
    //the sequence is from left to right and from top to bottom
    public void createMountPointWithSequence(SEScene scene) {
    	    if(this.mRowColType != ROW_COL_MATRIX) {
    	        return;
    	    }
    	    int planeType = SEVector.XZ_PLANE;
        	MountPointLeftRightTopBottomComparator c = new MountPointLeftRightTopBottomComparator(planeType);
        	Collections.sort(mMountPointDataList, c);
        	SEMountPointData firstData = mMountPointDataList.get(0);
        	SEVector3f firstT = firstData.getTranslate();
        	int ellip = 2;
        	int currentIndex = 0;
        	for(int i = 1 ;i < mMountPointDataList.size() ; i++) {
        		SEMountPointData mpd = mMountPointDataList.get(i);
        		SEVector3f tmpT = mpd.getTranslate();
        		float delta = getVerticalDelta(tmpT, firstT, planeType);
        		if(Math.abs(delta) > ellip) {
        			currentIndex = i;
        			break;
        		}
        	}
        	int colCount = currentIndex;
        	int rowCount = mMountPointDataList.size() / colCount;
        	mRowCount = rowCount;
        	mColCount = colCount;
        	SEMountPointData horzSecondData = mMountPointDataList.get(1);
        	SEMountPointData vertSecondData = mMountPointDataList.get(mColCount);
        	/*
        	SESceneInfo sceneInfo = scene.mSceneInfo;
        	HouseSceneInfo houseInfo = sceneInfo.mHouseSceneInfo;
        	float ux = houseInfo.getWallUnitSizeX();
        	float uy = houseInfo.getWallUnitSizeY();
        	*/
        	mCellWidth = this.getHorzDelta(horzSecondData.getTranslate(), firstData.getTranslate(), SEVector.XZ_PLANE);
        	mCellHeight = this.getVerticalDelta(firstData.getTranslate(), vertSecondData.getTranslate(), SEVector.XZ_PLANE);
    	    
    }
    public ArrayList<Integer> getMountPointIndexNeighbor(int index) {
    	    if(index < 0 || index >= mMountPointNeighborIndexList.size())
    	        return null;
    	    return mMountPointNeighborIndexList.get(index);
    }
    private void getMountPointBoundaryInPlaneXZ(int index, SEVector3f min, SEVector3f max) {
	    SEMountPointData mpd = mMountPointDataList.get(index);
	    SEVector3f t = mpd.getTranslate();
	    min.mD[0] = t.getX() - mCellWidth / 2;
	    min.mD[1] = t.getY();
	    min.mD[2] = t.getZ() - mCellHeight / 2;
	    
	    max.mD[0] = t.getX() + mCellWidth / 2;
	    max.mD[1] = t.getY();
	    max.mD[2] = t.getZ() + mCellHeight / 2;
    }
    public int getIndex(int row, int col) {
    	    return row * mColCount + col;
    }
    public MatrixPoint getMatrixPointByIndex(int index) {
    	    if(index == -1) {
    	    	    return new MatrixPoint();
    	    }
    	    int row = index / mColCount;
    	    int col = index % mColCount;
    	    return new MatrixPoint(row, col,index);
    }
    public int getCoordColInPlaneXZ(SEVector3f coord) {
   	    int[] indexArray = new int[mColCount];
	    for(int i = 0 ; i < mColCount ; i++) {
	    	    indexArray[i] = i;
	    }
	    SEVector3f min = new SEVector3f();
	    SEVector3f max = new SEVector3f();
	    for(int i = 0 ;i  < indexArray.length ; i++) {
	    	    int index = indexArray[i];

	    	    this.getMountPointBoundaryInPlaneXZ(index, min, max);
	    	    if(coord.getX() >= min.getX() && coord.getX() <= max.getX()) {
	    	    	    MatrixPoint mp = getMatrixPointByIndex(index);
	    	    	    return mp.col;
	    	    }
	    }
	    int minIndex = indexArray[0];
	    this.getMountPointBoundaryInPlaneXZ(minIndex, min, max);
	    if(coord.getX() > max.getX()) {
	    	    return mColCount - 1;
	    }
	    int maxIndex = indexArray[mColCount - 1];
	    this.getMountPointBoundaryInPlaneXZ(maxIndex, min, max);
	    if(coord.getX() < min.getX()) {
	    	    return 0;
	    }
	    return -1;
    }
    public int getCoordRowInPlaneXZ(SEVector3f coord) {
    	    int[] indexArray = new int[mRowCount];
    	    for(int i = 0 ; i < mRowCount ; i++) {
    	    	    indexArray[i] = i * mColCount;
    	    }
    	    SEVector3f min = new SEVector3f();
    	    SEVector3f max = new SEVector3f();
    	    for(int i = 0 ;i  < indexArray.length ; i++) {
    	    	    int index = indexArray[i];
    	    	    this.getMountPointBoundaryInPlaneXZ(index, min, max);
    	    	    if(coord.getZ() >= min.getZ() && coord.getZ() <= max.getZ()) {
    	    	    	    MatrixPoint mp = getMatrixPointByIndex(index);
    	    	    	    return mp.row;
    	    	    }
    	    }
    	    int minIndex = indexArray[0];
    	    this.getMountPointBoundaryInPlaneXZ(minIndex, min, max);
    	    if(coord.getZ() > max.getZ()) {
    	    	    return 0;
    	    }
    	    int maxIndex = indexArray[mRowCount - 1];
    	    this.getMountPointBoundaryInPlaneXZ(maxIndex, min, max);
    	    if(coord.getZ() < min.getZ()) {
    	    	    return mRowCount - 1;
    	    }
    	    return -1;
    }
    public MatrixPoint getMatrixPointInPlaneXZ(SEVector3f coord) {
    	
    	    for(int i = 0 ; i < mMountPointDataList.size() ; i++) {
    	    	    SEMountPointData mpd = mMountPointDataList.get(i);
    	    	    //SEVector3f center = mpd.getTranslate();
    	    	    SEVector3f startPoint = mpd.mMinPoint;//new SEVector3f(center.getX() - mCellWidth / 2, 
    	    	    		                                  // center.getY(), 
    	    	    		                                  // center.getZ() - mCellHeight / 2);
    	    	    SEVector3f endPoint = mpd.mMaxPoint;//new SEVector3f(center.getX() + mCellWidth / 2, center.getY(),
    	    	    		                                // center.getZ() + mCellHeight / 2);
    	    	    if(coord.getX() >= startPoint.getX() && coord.getX() <= endPoint.getX() &&
    	    	    		coord.getZ() >= startPoint.getZ() && coord.getZ() <= endPoint.getZ()) {
    	    	    	    int row = i / mColCount;
    	    	    	    int col = i % mColCount;
    	    	    	    MatrixPoint mp = new MatrixPoint();
    	    	    	    mp.row = row;
    	    	    	    mp.col = col;
    	    	    	    mp.mountPointIndex = i;
    	    	    	    return mp;
    	    	    } 
    	    }
    	    
    	/*
        int retIndex = mQuatTree.getIndex(coord);
        if(retIndex != -1) {
        	    int row = retIndex / mColCount;
    	        int col = retIndex % mColCount;
    	        MatrixPoint mp = new MatrixPoint();
    	        mp.row = row;
    	        mp.col = col;
    	        mp.mountPointIndex = retIndex;
    	        return mp;
        }
        */
    	    SEVector3f minPoint = new SEVector3f();
    	    SEVector3f maxPoint = new SEVector3f();
    	    this.getBoundaryCoordInPlaneXZ(minPoint, maxPoint);
    	    MatrixPoint mp = new MatrixPoint();
    	    mp.mountPointIndex = -1;
    	    if(coord.getX() > maxPoint.getX()) {
    	    	    mp.col = mColCount - 1;
    	    } else if(coord.getX() < minPoint.getX()) {
    	    	    mp.col = 0;
    	    } else {
    	    	    mp.col = this.getCoordColInPlaneXZ(coord);
    	    }
    	    if(coord.getZ() < minPoint.getZ()) {
    	    	    mp.row = mRowCount - 1;
    	    } else if(coord.getZ() > maxPoint.getZ()) {
    	    	    mp.row = 0;
    	    	    
    	    } else {
    	        mp.row = this.getCoordRowInPlaneXZ(coord);
    	    }
    	    return mp;
    }
    public static boolean isMatrixPointInBoundary(MatrixPoint mp) {
    	    return mp.mountPointIndex != -1;
    }
    /*
    //coordMin must be left bottom point
    //coordMax must be right top point
    public BoundaryPoint getBoundaryPointByPlaneXZ(SEVector3f coordMin, SEVector3f coordMax) {
    	    MatrixPoint minMatrixPoint = this.getMatrixPointInPlaneXZ(coordMin);
    	    MatrixPoint maxMatrixPoint = this.getMatrixPointInPlaneXZ(coordMax);
    	    BoundaryPoint bp = new BoundaryPoint(minMatrixPoint, maxMatrixPoint);
    	    return bp;
    }
    */
    public void getBoundaryCoordInPlaneXZ(SEVector3f minPoint, SEVector3f maxPoint) {
    	    if(mMinPoint != null && mMaxPoint != null) {
    	    	    for(int i = 0 ; i < 3 ; i++) {
    	    	    	    minPoint.mD[i] = mMinPoint.mD[i];
    	    	    	    maxPoint.mD[i] = mMaxPoint.mD[i];
    	    	    }
    	    	    return;
    	    }
    	    float xMin = 9999999;
    	    float zMin = 9999999;
    	    float yMin = 9999999;
    	    float xMax = -9999999;
    	    float zMax = -9999999;
    	    float yMax = -9999999;
    	    for(int i = 0 ; i < mMountPointDataList.size() ; i++) {
    	    	    SEMountPointData mpd = mMountPointDataList.get(i);
    	    	    SEVector3f t = mpd.getTranslate();
    	    	    SEVector3f tMinPoint = mpd.mMinPoint;//new SEVector3f(t.getX() - mCellWidth / 2, t.getY(),
    	    	    		                                 // t.getZ() - mCellHeight / 2);
    	    	    SEVector3f tMaxPoint = mpd.mMaxPoint;//new SEVector3f(t.getX() + mCellWidth / 2, t.getY(),
    	    	    		                                 // t.getZ() + mCellHeight / 2);
    	    	    if(tMinPoint.getX() < xMin) {
    	    	    	    xMin = tMinPoint.getX();
    	    	    }
    	    	    if(tMaxPoint.getX() > xMax) {
    	    	    	    xMax = tMaxPoint.getX();
    	    	    }
    	    	    if(tMinPoint.getZ() < zMin) {
    	    	    	    zMin = tMinPoint.getZ();
    	    	    }
    	    	    if(tMaxPoint.getZ() > zMax) {
    	    	    	    zMax = tMaxPoint.getZ();
    	    	    }
    	    	    if(t.getY() < yMin) {
    	    	    	    yMin = t.getY();
    	    	    }
    	    	    if(t.getY() > yMax) {
    	    	    	    yMax = t.getY();
    	    	    }
    	    }
    	    minPoint.mD[0] = xMin;
    	    minPoint.mD[1] = yMin;
    	    minPoint.mD[2] = zMin;
    	    maxPoint.mD[0] = xMax;
    	    maxPoint.mD[1] = yMax;
    	    maxPoint.mD[2] = zMax;
    	    mMinPoint = new SEVector3f();
    	    mMaxPoint = new SEVector3f();
    	    for(int i = 0 ; i < 3 ; i++) {
	    	    mMinPoint.mD[i] = minPoint.mD[i];
	    	    mMaxPoint.mD[i] = maxPoint.mD[i];
	    }
    	Log.i(TAG, "### chain boundary = " + mMinPoint + ", " + mMaxPoint);
    }
    /*
    public ArrayList<ClosestMountPointData> getClosestMountPointTo(int mountPointIndex) {
    	    SEMountPointData dstMPD = mMountPointDataList.get(mountPointIndex);
    	    SEVector3f point = dstMPD.getTranslate();
    	    int outIndex = -1;
    	    SEMountPointData outMPD = null;
    	    float minDist = 9999999;
    	    ArrayList<ClosestMountPointData> closestMountPointList = new ArrayList<ClosestMountPointData>();
    	    for(int i = 0 ; i < mMountPointDataList.size() ; i++) {
    	    	    if(i == mountPointIndex) {
    	    	    	    continue;
    	    	    }
    	    	    SEMountPointData tmpData = mMountPointDataList.get(i);
    	    	    SEVector3f t = tmpData.getTranslate();
    	    	    float dist = t.dist(point);
    	    	    if(dist < minDist) {
    	    	    	    minDist = dist;
    	    	    	    outMPD = tmpData;
    	    	    	    outIndex = i;
    	    	    }
    	    }
    	    assert(outIndex != -1);
    	    assert(outMPD != null);
    	    for(int i = 0 ; i < mMountPointDataList.size() ; i++) {
    	    	    if(i == mountPointIndex) {
    	    	    	    continue;
    	    	    }
    	    	    SEMountPointData tmpData = mMountPointDataList.get(i);
    	    	    SEVector3f t = tmpData.getTranslate();
    	    	    float dist = t.dist(point);
    	    	    if(Math.abs(dist - minDist) < 0.001) {
    	    	    	    ClosestMountPointData d = new ClosestMountPointData();
    	    	    	    d.mMPD = tmpData;
    	    	    	    d.mIndex = i;
    	    	    	    d.mDist = dist;
    	    	    	    closestMountPointList.add(d);
    	    	    }
    	    }
    	    assert(closestMountPointList.size() > 0);
    	    ClosestMountPointData[] sequenceMountPointList = new ClosestMountPointData[4];
    	    
    	    for(int i = 0 ; i < closestMountPointList.size() ; i++) {
    	    	    ClosestMountPointData d = closestMountPointList.get(i);
    	    	    SEVector3f tmpV = d.mMPD.getTranslate();
    	    	    if(tmpV.getX() > point.getX()) {
    	    	    	    sequenceMountPointList[AT_RIGHT] = d;
    	    	    } else if(tmpV.getZ() > point.getZ()) {
    	    	    	    sequenceMountPointList[AT_TOP] = d;
    	    	    } else if(tmpV.getX() < point.getX()) {
    	    	    	    sequenceMountPointList[AT_LEFT] = d;
    	    	    } else if(tmpV.getZ() < point.getZ()) {
    	    	    	    sequenceMountPointList[AT_BOTTOM] = d;
    	    	    }
    	    }
    	    ArrayList<ClosestMountPointData> retCMP = new ArrayList<ClosestMountPointData>();
    	    for(int i = 0 ; i < 4 ; i++) {
    	    	    if(sequenceMountPointList[i] != null) {
    	    	        retCMP.add(sequenceMountPointList[i]);
    	    	    }
    	    }
    	    return retCMP;
    	    
    	    for(int i = 0 ; i < 4 ; i++) {
    	    	    if(sequenceMountPointList[i] != null) {
    	    	    	    return sequenceMountPointList[i];
    	    	    }
    	    }
    	    assert(false);
    	    return null;
    	    
    	    
    	    ClosestMountPointData d = new ClosestMountPointData();
    		d.mMPD = outMPD;
    		d.mIndex = outIndex;
    		d.mDist = minDist;
    		return d;
    		
    }
*/
    public ClosestMountPointData getClosestMountPoint(SEVector3f point)
    {
		float minDist = 100000;
		SEMountPointData retMPD = null;
		int index = -1;
		for(int i = 0 ; i < mMountPointDataList.size(); i++)
		{
			SEMountPointData mpd = mMountPointDataList.get(i);
			SEVector3f t = mpd.getTranslate();
			float dist = t.dist(point);
			if(dist < minDist)
			{
				minDist = dist;
				retMPD = mpd;
				index = i;
			}
		}
		ClosestMountPointData d = new ClosestMountPointData();
		d.mMPD = retMPD;
		d.mIndex = index;
		d.mDist = minDist;
		return d;
    }
}
