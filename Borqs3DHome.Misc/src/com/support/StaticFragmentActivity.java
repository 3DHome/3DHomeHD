package com.support;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.umeng.analytics.MobclickAgent;

/**
 * Created by b608 on 13-10-17.
 */
public class StaticFragmentActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        MobclickAgent.updateOnlineConfig(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
