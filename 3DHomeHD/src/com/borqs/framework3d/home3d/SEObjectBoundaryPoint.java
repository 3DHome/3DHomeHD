package com.borqs.framework3d.home3d;

import com.borqs.se.engine.SEVector.SEVector3f;

public class SEObjectBoundaryPoint {
    public SEVector3f minPoint;
    public SEVector3f maxPoint;
    public SEVector3f center;
    public SEVector3f xyzSpan;
    public int movePlane;
    public SEMountPointChain.MatrixPoint minMatrixPoint;
    public SEMountPointChain.MatrixPoint maxMatrixPoint;
    public int wallIndex;
    public SEMountPointChain.MatrixPoint bpSize;
    public static final int MOVE_PLANE_INVALID = 0;
    public static final int MOVE_PLANE_XZ = 1;
    public static final int MOVE_PLANE_XY = 2;
    public static final int MOVE_PLANE_YZ = 3;

    public static final int CENTER_POINT_STYLE_TOP_LEFT = 0;
    public static final int CENTER_POINT_STYLE_TOP_MID = 1;
    public static final int CENTER_POINT_STYLE_TOP_RIGHT = 2;
    public static final int CENTER_POINT_STYLE_MID_LEFT = 3;
    public static final int CENTER_POINT_STYLE_MID_MID = 4;
    public static final int CENTER_POINT_STYLE_MID_RIGHT = 5;
    public static final int CENTER_POINT_STYLE_BOTTOM_LEFT = 6;
    public static final int CENTER_POINT_STYLE_BOTTOM_MID = 7;
    public static final int CENTER_POINT_STYLE_BOTTOM_RIGHT = 8;
    public SEObjectBoundaryPoint(int wallIndex) {
    	this.wallIndex = wallIndex;
    	this.center = new SEVector3f(0, 0, 0);
    	this.xyzSpan = new SEVector3f(0, 0, 0);
        this.movePlane = MOVE_PLANE_XZ;
    }
    public void setData(int wallIndex, int movePlane, SEVector3f center, SEVector3f xyzSpan, 
    		            int minRow, int minCol, int maxRow, int maxCol, int centerPointStyle) {
    	this.wallIndex = wallIndex;
    	this.movePlane = movePlane;
    	this.center = center;
    	this.xyzSpan = xyzSpan;
    	this.minMatrixPoint = new SEMountPointChain.MatrixPoint(minRow, minCol, -1);
    	this.maxMatrixPoint = new SEMountPointChain.MatrixPoint(maxRow, maxCol, -1);
    	if(xyzSpan.equals(SEVector3f.ZERO) == false) {
    	    SEObjectBoundaryPoint minMax = SEObjectBoundaryPoint.getMinMaxPointInPlane(center, xyzSpan, movePlane, centerPointStyle);
            this.minPoint = minMax.minPoint;
            this.maxPoint = minMax.maxPoint;
    	}
    }
    
    public void getBoundaryPoint(SEVector3f minPoint , SEVector3f maxPoint) {
    	    
    }
    public static SEObjectBoundaryPoint getMinMaxPointInPlane(SEVector3f point, SEVector3f xyzSpan, int movePlane, int centerPointStyle) {
	    SEVector3f minPoint = new SEVector3f();
	    SEVector3f maxPoint = new SEVector3f();
	    float xspan = xyzSpan.getX();
	    float yspan = xyzSpan.getY();
	    float zspan = xyzSpan.getZ();

		switch(movePlane) {
		case MOVE_PLANE_XZ: {
            switch(centerPointStyle) {
                case CENTER_POINT_STYLE_MID_MID:
                {
			        minPoint.mD[0] = point.getX() - xspan / 2;
			        minPoint.mD[1] = point.getY();
			        minPoint.mD[2] = point.getZ() - zspan / 2;
			        maxPoint.mD[0] = point.getX() + xspan / 2;
			        maxPoint.mD[1] = point.getY();
			        maxPoint.mD[2] = point.getZ() + zspan / 2;
                }
                break;
                case CENTER_POINT_STYLE_TOP_LEFT:
                {
                    minPoint.mD[0] = point.getX();
                    minPoint.mD[1] = point.getY();
                    minPoint.mD[2] = point.getZ() - zspan;
                    maxPoint.mD[0] = point.getX() + xspan;
                    maxPoint.mD[1] = point.getY();
                    maxPoint.mD[2] = point.getZ();
                }
                break;
                case CENTER_POINT_STYLE_TOP_RIGHT:
                {
                    minPoint.mD[0] = point.getX() - xspan;
                    minPoint.mD[1] = point.getY();
                    minPoint.mD[2] = point.getZ() - zspan;
                    maxPoint.mD[0] = point.getX();
                    maxPoint.mD[1] = point.getY();
                    maxPoint.mD[2] = point.getZ();
                }
                break;
                case CENTER_POINT_STYLE_TOP_MID:
                {
                    minPoint.mD[0] = point.getX() - xspan / 2;
                    minPoint.mD[1] = point.getY();
                    minPoint.mD[2] = point.getZ() - zspan;
                    maxPoint.mD[0] = point.getX() + xspan / 2;
                    maxPoint.mD[1] = point.getY();
                    maxPoint.mD[2] = point.getZ();
                }
                break;
                case CENTER_POINT_STYLE_MID_LEFT:
                {
                    minPoint.mD[0] = point.getX();
                    minPoint.mD[1] = point.getY();
                    minPoint.mD[2] = point.getZ() - zspan / 2;
                    maxPoint.mD[0] = point.getX() + xspan;
                    maxPoint.mD[1] = point.getY();
                    maxPoint.mD[2] = point.getZ() + zspan / 2;
                }
                break;
                case CENTER_POINT_STYLE_MID_RIGHT:
                {
                    minPoint.mD[0] = point.getX() - xspan;
                    minPoint.mD[1] = point.getY();
                    minPoint.mD[2] = point.getZ() - zspan / 2;
                    maxPoint.mD[0] = point.getX();
                    maxPoint.mD[1] = point.getY();
                    maxPoint.mD[2] = point.getZ() + zspan / 2;
                }
                break;
                case CENTER_POINT_STYLE_BOTTOM_LEFT:
                {
                    minPoint.mD[0] = point.getX();
                    minPoint.mD[1] = point.getY();
                    minPoint.mD[2] = point.getZ();
                    maxPoint.mD[0] = point.getX() + xspan;
                    maxPoint.mD[1] = point.getY();
                    maxPoint.mD[2] = point.getZ() + zspan;
                }
                break;
                case CENTER_POINT_STYLE_BOTTOM_MID:
                {
                    minPoint.mD[0] = point.getX() - xspan / 2;
                    minPoint.mD[1] = point.getY();
                    minPoint.mD[2] = point.getZ();
                    maxPoint.mD[0] = point.getX() + xspan / 2;
                    maxPoint.mD[1] = point.getY();
                    maxPoint.mD[2] = point.getZ() + zspan;
                }
                break;
                case CENTER_POINT_STYLE_BOTTOM_RIGHT:
                {
                    minPoint.mD[0] = point.getX() - xspan;
                    minPoint.mD[1] = point.getY();
                    minPoint.mD[2] = point.getZ();
                    maxPoint.mD[0] = point.getX();
                    maxPoint.mD[1] = point.getY();
                    maxPoint.mD[2] = point.getZ() + zspan;
                }
                break;
            }
		}
		    break;
		case MOVE_PLANE_XY: {
            SEDebug.myAssert(false, "not implement xy");
			minPoint.mD[0] = point.getX() - xspan / 2;
			minPoint.mD[1] = point.getY() - yspan / 2;
			minPoint.mD[2] = point.getZ();
			maxPoint.mD[0] = point.getX() + xspan / 2;
			maxPoint.mD[1] = point.getY() + yspan / 2;
			maxPoint.mD[2] = point.getZ();
		}
		break;
		case MOVE_PLANE_YZ: {
            SEDebug.myAssert(false, "not implement yz");
			minPoint.mD[0] = point.getX();
			minPoint.mD[1] = point.getY() - yspan / 2;
			minPoint.mD[2] = point.getZ() - zspan / 2;
			maxPoint.mD[0] = point.getX();
			maxPoint.mD[1] = point.getY() + yspan / 2;
			maxPoint.mD[2] = point.getZ() + zspan / 2;
		}
		break;
		}
		SEObjectBoundaryPoint bp = new SEObjectBoundaryPoint(0);
		bp.minPoint = minPoint;
		bp.maxPoint = maxPoint;
		return bp;
	}
    public static boolean isEmpty(SEObjectBoundaryPoint bp) {
        if(bp == null) {
            return true;
        }
        return bp.xyzSpan.equals(SEVector3f.ZERO);
    }
    public boolean isSetMatrixPoint() {
        return maxMatrixPoint != null && minMatrixPoint != null;
    }
    public boolean isSetMinMaxPoint() {
        return minPoint != null && maxPoint != null;
    }
    public boolean isXYZSpanEmpty() {
        return xyzSpan == null || xyzSpan.equals(SEVector3f.ZERO);
    }
    public SEObjectBoundaryPoint clone() {
    	    SEObjectBoundaryPoint bp = new SEObjectBoundaryPoint(wallIndex);
    	    bp.minMatrixPoint = minMatrixPoint;
    	    bp.maxMatrixPoint = maxMatrixPoint;
    	    bp.minPoint = minPoint;
    	    bp.maxPoint = maxPoint;
    	    bp.movePlane = movePlane;
    	    bp.xyzSpan = xyzSpan.clone();
    	    bp.center = center.clone();
    	    return bp;
    }
    public boolean equals(SEObjectBoundaryPoint bp) {
    	if(movePlane == bp.movePlane && minMatrixPoint.row == bp.minMatrixPoint.row && 
           minMatrixPoint.col == bp.minMatrixPoint.col && center.equals(bp.center) &&
           xyzSpan.equals(bp.xyzSpan) && wallIndex == bp.wallIndex) {
    		return true;
    	} else {
    		return false;
    	}
    }
}
