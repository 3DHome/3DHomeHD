package com.support;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import com.umeng.analytics.MobclickAgent;
import com.umeng.fb.FeedbackAgent;

/**
 * Created by b608 on 13-10-16.
 */
public class StaticUtil {
    private static final int FLAG_NEEDS_MENU_KEY = 0x08000000;
    public static void forceShowOptionMenu(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            activity.getWindow().addFlags(FLAG_NEEDS_MENU_KEY);
        }
    }

    public static void reportError(Context context, String message) {
        MobclickAgent.reportError(context, message);
    }

    public static void onEvent(Context context, String s, String ex) {
        MobclickAgent.onEvent(context, s, ex);
    }

    public static void onEvent(Context context, String s) {
        MobclickAgent.onEvent(context, s);
    }

    public static void startFeedback(Context context) {
        FeedbackAgent agent = new FeedbackAgent(context);
        agent.startFeedbackActivity();
    }

    public static void updateOnlineConfig(Context context) {
        MobclickAgent.updateOnlineConfig(context);
    }

    public static void onResume(Context context) {
        MobclickAgent.onResume(context);
    }

    public static void onPause(Context context) {
        MobclickAgent.onPause(context);
    }

    public static void showFeedbackActivity(Context context) {
        FeedbackAgent agent = new FeedbackAgent(context);
        agent.startFeedbackActivity();
    }
}
