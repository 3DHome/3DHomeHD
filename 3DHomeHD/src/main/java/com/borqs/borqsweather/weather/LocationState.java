package com.borqs.borqsweather.weather;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

public class LocationState extends State {

    private static final String TAG = "Weather_LocationState";

    public interface Locate {
        void getCity();

        void stop();
        boolean isFirstRequest();
    }

    private static final long RETRY_TIME = 2 * 60 * 1000;
    private long mNextReqTime = RETRY_TIME;
    private Locate mLocate;

    public LocationState(WeatherController controller) {
        super(controller);
    }

    @Override
    public void run(Object data) {
        int countryCode = Utils.getCountryCode(mContext);
        if (DEBUG) Log.i(TAG, "CountryCode : " + countryCode);
        if (countryCode == 460) {
            if (DEBUG) Log.i(TAG, "to use baidu server for location");
            mLocate = new BDLocate(this, true);
        } else {
            if (DEBUG) Log.i(TAG, "to use google server for location");
            mLocate = new GLocate(this, true);
        }
        mLocate.getCity();
    }

    public void onReceiveCity(Boolean isBaiDu, Bundle data) {
        if (!TextUtils.isEmpty(data.getString(State.BUNDLE_CITY))) {
            //Log.d(TAG, "OK to get city name + " + data);
            if (DEBUG) Log.i(TAG, "location city: " + data.getString(State.BUNDLE_CITY));
            mPreferences.setCityName(data.getString(State.BUNDLE_CITY));
            Message msg = Message.obtain(mHandler, WeatherController.EVENT_REQUEST_LOCATION_SUCCESS, data);
            mHandler.sendMessage(msg);
            cancelRetry();
        } else if (isBaiDu) {
            if (mLocate.isFirstRequest()) {
                if (DEBUG) Log.i(TAG, "The first use of Baidu positioning failure, to enable Google");
                mLocate = new GLocate(this, false);
                mLocate.getCity();
            } else {
                if (DEBUG) Log.i(TAG, "The use of Baidu positioning again");
                mPreferences.setCityName("");
                Message msg = Message.obtain(mHandler, WeatherController.EVENT_REQUEST_FAILE,
                      WeatherController.RESULT_ERROR_LOCATION);
                mHandler.sendMessage(msg);
                setRetry();
            }
        } else {
            int countryCode = Utils.getCountryCode(mContext);
            if (mLocate.isFirstRequest()) {
                if (countryCode == -1 && Utils.isWifiConnected(mContext)) {
                    if (DEBUG) Log.i(TAG, "The first use of Google positioning failure, to enable Baidu positioning");
                    mLocate = new BDLocate(this, false);
                    mLocate.getCity();
                } else {
                    mPreferences.setCityName("");
                    Message msg = Message.obtain(mHandler, WeatherController.EVENT_REQUEST_FAILE,
                          WeatherController.RESULT_ERROR_LOCATION);
                    mHandler.sendMessage(msg);
                    setRetry();
                }
            } else {
                if (DEBUG) Log.i(TAG, "The use of google positioning again");
                mPreferences.setCityName("");
                Message msg = Message.obtain(mHandler, WeatherController.EVENT_REQUEST_FAILE,
                      WeatherController.RESULT_ERROR_LOCATION);
                mHandler.sendMessage(msg);
                setRetry();
            }
        }
    }

    @Override
    void setRetry() {
        // retry after 2min, 2+4min, 2+4+8min, 2+4+8+16min, 2+4+8+16+32min, then
        // cancel retry
        if (mNextReqTime > 32 * 60 * 1000 || mController.isStop()) {
            if (DEBUG)
                Log.d(TAG, "################ setRetry timeout, cancel retry : " + mController.isStop());
            cancelRetry();
            return;
        }
        if (mCurRetryState == RetryState.IDLE) {
            mCurRetryState = RetryState.RUNNING;
            if (mReceiver == null) {
                IntentFilter filter = new IntentFilter(INTENT_ACTION_RETRY);
                mReceiver = new stateReceiver();
                mContext.registerReceiver(mReceiver, filter);
            }

        }
        Intent intent = new Intent(INTENT_ACTION_RETRY);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        long nextReq = SystemClock.elapsedRealtime() + mNextReqTime;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nextReq);
        if(DEBUG) Log.i(TAG, "start to get location after: " + mNextReqTime + "ms");
        mAlarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextReq, sender);
        mNextReqTime = mNextReqTime * 2;
    }

    @Override
    void cancelRetry() {
        if (mCurRetryState == RetryState.RUNNING) {
            mCurRetryState = RetryState.IDLE;
            if (mReceiver != null) {
                mContext.unregisterReceiver(mReceiver);
                mReceiver = null;
            }
            Intent intent = new Intent(INTENT_ACTION_RETRY);
            PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            mAlarmMgr.cancel(sender);
            if(DEBUG) Log.i(TAG, "stop to get location");
        }
        mNextReqTime = RETRY_TIME;
        if (mLocate != null) {
            mLocate.stop();
        }
    }
}
