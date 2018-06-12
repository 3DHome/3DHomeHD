package com.borqs.borqsweather.weather;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import com.borqs.se.home3d.HomeUtils;
import com.borqs.borqsweather.weather.yahoo.WeatherDataModel;
import com.borqs.borqsweather.weather.yahoo.WeatherPreferences;

public abstract class State {
    public static final String INTENT_ACTION_RETRY = "3dhome.action.intent.RETRY";
    public static final String BUNDLE_COUNTRY = "country";
    public static final String BUNDLE_PROVINCE = "province";
    public static final String BUNDLE_CITY = "city";
    public static final String BUNDLE_WOEID = "woeid";
    public static final String BUNDLE_LATITUDE = "latitude";
    public static final String BUNDLE_LONGITUDE = "longitude";

    public static boolean DEBUG = HomeUtils.DEBUG;

    protected enum RetryState {
        IDLE, RUNNING
    }

    protected WeatherController mController;
    protected Context mContext;
    protected Handler mHandler;
    protected WeatherPreferences mPreferences;
    protected WeatherDataModel mDataModel;
    protected AlarmManager mAlarmMgr;
    protected RetryState mCurRetryState;
    protected BroadcastReceiver mReceiver;

    State(WeatherController controller) {
        mController = controller;
        mContext = controller.getContext();
        mHandler = controller.getEventHandler();
        mPreferences = WeatherPreferences.getInstance(mContext);
        mDataModel = WeatherDataModel.getInstance();
        mAlarmMgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mCurRetryState = RetryState.IDLE;
    }

    abstract void run(Object data);

    void setRetry() {
        if (mCurRetryState == RetryState.IDLE) {
            if (mReceiver == null) {
                mReceiver = new stateReceiver();
            }
            mContext.registerReceiver(mReceiver, new IntentFilter(INTENT_ACTION_RETRY));
        }
        mCurRetryState = RetryState.RUNNING;
    }

    void startRetry() {
        if (mCurRetryState == RetryState.RUNNING) {
            run(null);
        }
    }

    void cancelRetry() {
        if (mCurRetryState == RetryState.RUNNING) {
            if (mReceiver != null) {
                mContext.unregisterReceiver(mReceiver);
                mReceiver = null;
            }
        }
        mCurRetryState = RetryState.IDLE;
    }

    public class stateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (INTENT_ACTION_RETRY.equals(action)) {
                try {
                         mHandler.post(new Runnable() {
                          public void run() {
                             startRetry();
                         }
                    });                
                } catch(Exception e) {
                
                }
            }
        }

    };
}
