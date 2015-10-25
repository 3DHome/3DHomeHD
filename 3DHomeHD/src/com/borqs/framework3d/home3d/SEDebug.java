package com.borqs.framework3d.home3d;

import android.util.Log;


public class SEDebug {
    private static class MyAssertException extends RuntimeException {
	    public MyAssertException(String msg) {
	    	    super("error: " + msg + " : b050");
	    }
    }
    public static void myAssert(boolean b, String errorMsg) {
	    if(b == false) {
	    	Log.e("SEDebug", " " + errorMsg);
//	    	    throw new MyAssertException(errorMsg);
	    }
    }
}
