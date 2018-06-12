package com.borqs.framework3d.home3d;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEVector.SERotate;
import java.util.ArrayList;
public class SEMountPointData {
    private SEVector3f mTranslate;
    private SEVector3f mScale;
    private SERotate mRotate;
    private String mName;
    private float mDistToComparedPoint;
    private ArrayList<Float> mDistToPointList ;
    public SEVector3f mMinPoint;
    public SEVector3f mMaxPoint;
    public SEMountPointData(String name, SEVector3f t, SEVector3f s, SERotate r){
    	    mTranslate = t;
    	    mScale = s;
    	    mRotate =r;
    	    mName = name;
    }
    public SEMountPointData()
    {
    	
    }
    public void setDistToComparedPoint(float d) {
    	    mDistToComparedPoint = d;
    }
    public float getDistToComparedPoint() {
    	    return mDistToComparedPoint;
    }
    public void setDistToPointListWithSize(int size) {
    	    mDistToPointList = new ArrayList<Float>();
    	    for(int i = 0 ; i < size ; i++) {
    	    	    mDistToPointList.add(Float.valueOf(0));
    	    }
    }
    public ArrayList<Float> getDistToPointList() {
    	    return mDistToPointList;
    }
    public float getDistToPoint(int index) {
    	    return mDistToPointList.get(index);
    }
    public void saveDistToPoint(int index, float dist) {
    	    mDistToPointList.set(index, Float.valueOf(dist));
    }
    public String getName()
    {
    	return mName;
    }
    
    public void setName(String name)
    {
    	mName = name;
    }
    public void setTranslate(SEVector3f t)
    {
    	mTranslate = t;
    }
    public void setRotate(SERotate r)
    {
    	mRotate = r;
    }
    public void setScale(SEVector3f s)
    {
    	mScale = s;
    }
    public SEVector3f getTranslate()
    {
    	return mTranslate;
    }
    public SEVector3f getScale()
    {
    	return mScale;
    }
    public SERotate getRotate()
    {
    	return mRotate;
    }
    
}
