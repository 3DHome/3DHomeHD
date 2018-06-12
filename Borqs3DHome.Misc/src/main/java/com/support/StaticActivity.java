package com.support;

import android.app.Activity;

import android.content.Context;

import com.umeng.analytics.MobclickAgent;

/**
 * Created by b608 on 13-10-17.
 */
public class StaticActivity extends Activity {
    @Override
    protected void onResume() {
        super.onResume();
        staticResumed(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        staticPaused(this);
    }

    public static void staticResumed(Context context) {
        MobclickAgent.onResume(context);
    }

    public static void staticPaused(Context context) {
        MobclickAgent.onPause(context);
    }
}
