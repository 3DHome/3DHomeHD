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

public class WoeidState extends State {
    private static final String TAG = "Weather_WoeidState";
    // private String mCity;
    private Bundle mData;
    private boolean mIsAuto;

    private static final long RETRY_TIME = 2 * 60 * 1000;
    private long mNextReqTime = RETRY_TIME;

    public WoeidState(WeatherController controller) {
        super(controller);
    }

    @Override
    public void run(Object data) {
        mData = (Bundle) data;
        mIsAuto = mController.isAutoLocate();
        String woeid = getWoeid(mData);

        if (DEBUG) Log.d(TAG, "Start to get WOEID " + mData);
        if (DEBUG) Log.d(TAG, "woeid : " + woeid);
        if (DEBUG) Log.d(TAG, "OK to get woeid : " + woeid);

        if (TextUtils.isEmpty(woeid)) {
            Message msg = Message.obtain(mHandler, WeatherController.EVENT_REQUEST_FAILE,
                    WeatherController.RESULT_ERROR_GET_WEATHER);
            mHandler.sendMessage(msg);
            setRetry();
        } else {
            mData.putString(State.BUNDLE_WOEID, woeid);
            Message msg = Message.obtain(mHandler, WeatherController.EVENT_REQUEST_WOEID_SUCCESS, mData);
            mHandler.sendMessage(msg);
            cancelRetry();
        }
    }

    private String getWoeid(Bundle bundle) {
        String woeid = null;
        String savedCity = mPreferences.getManualLocationCityName();
        if (mIsAuto) {
            try {
                savedCity = mPreferences.getAutoLocationCityName();
            } catch (Exception e) {
                savedCity = "";
                mPreferences.setAutoLocationCityName("");
            }
        }
        if (!TextUtils.isEmpty(savedCity)) {
            savedCity = savedCity.toLowerCase().trim().replaceAll(" ", "_");
        }
        long time = mIsAuto ? mPreferences.getAutoWoeidUpdateTime() : mPreferences.getManualWoeidUpdateTime();
        if (bundle.getString(State.BUNDLE_CITY).equals(savedCity)
                && !isWoeidOutOfDate(time)) {
            woeid = mIsAuto ? mPreferences.getAutoWoeid() : mPreferences.getManualWoeid();
        }
        if (TextUtils.isEmpty(woeid)) {
            woeid = mDataModel.getWOEIDByLocation(bundle);
        }
        return woeid;
    }

    private boolean isWoeidOutOfDate(long time) {
        if (time < 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTimeInMillis(now);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        if ((nowCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR))
                && (nowCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH))
                && (nowCal.get(Calendar.DAY_OF_MONTH) == cal.get(Calendar.DAY_OF_MONTH))) {
            return false;
        }
        return true;
    }

    @Override
    void setRetry() {
        // retry after 2min, 2+4min, 2+4+8min, then cancel retry
        if (mNextReqTime > 10 * 60 * 1000 || mController.isStop()) {
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
        mCurRetryState = RetryState.RUNNING;
        Intent intent = new Intent(INTENT_ACTION_RETRY);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        long nextReq = SystemClock.elapsedRealtime() + mNextReqTime;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nextReq);
        Log.i(TAG, "start to get woeid after: " + mNextReqTime + "ms");
        mAlarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextReq, sender);
        mNextReqTime = mNextReqTime * 2;
    }

    @Override
    void startRetry() {
        if (mCurRetryState == RetryState.RUNNING) {
            if (DEBUG)
                Log.i(TAG, "################## startRetry");
            run(mData);
        }
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
            Log.i(TAG, "stop to get woeid");
        }
        mCurRetryState = RetryState.IDLE;
        mNextReqTime = RETRY_TIME;
    }
}
