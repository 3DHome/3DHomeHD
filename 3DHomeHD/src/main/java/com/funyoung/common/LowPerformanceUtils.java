package com.funyoung.common;

import android.util.Log;

/**
 * Created by yangfeng on 13-7-2.
 */
public class LowPerformanceUtils {
    private static final boolean LOW_PERFORMANCE = true;

    private static final String ENTER = "Enter ";
    private static final String EXIT = "Exit ";

    private LowPerformanceUtils() {
        // not instance
    }

    public static void logEnter(String tag, String method, String text) {
        logPrint(tag, ENTER, method, text);
    }

    public static void logExit(String tag, String method, String text) {
        logPrint(tag, EXIT, method, text);
    }

    private static void logPrint(String tag, String prefix, String method, String text) {

        Log.d(tag, prefix + method + ", " + text);
    }
}
