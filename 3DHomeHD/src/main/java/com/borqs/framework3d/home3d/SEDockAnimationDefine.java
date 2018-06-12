package com.borqs.framework3d.home3d;

public class SEDockAnimationDefine {
    private String mObjectName;
    private int mAnimationCount;
    private String mAnimationType; //"translate"
    private String mTraceType; //"linear" "circle"
    public SEDockAnimationDefine(String objectName, int animationCount, String animationType, String traceType) {
    	    mObjectName = objectName;
    	    mAnimationCount = animationCount;
    	    mAnimationType = animationType;
    	    mTraceType = traceType;
    }
    public SEDockAnimationDefine() {
    	
    }
    public void setObjectName(String name) {
    	    mObjectName = name;
    }
    public String getObjectName() {
    	    return mObjectName;
    }
    public void setAnimationCount(int n) {
    	    mAnimationCount = n;
    }
    public int getAnimationCount() {
    	    return mAnimationCount;
    }
    public void setAnimationType(String t) {
    	    mAnimationType = t;
    }
    public String getAnimationType() {
    	    return mAnimationType;
    }
    public void setAnimationTrace(String t) {
    	    mTraceType = t;
    }
    public String getAnimationTrace() {
    	    return mTraceType;
    }
    
}
