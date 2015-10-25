package com.borqs.borqsweather.weather;

import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.borqs.borqsweather.weather.yahoo.WeatherInfo;

public class WeatherState extends State {
    private static final String TAG = "Weather_WeatherState";
    private Bundle mBundle;

    private static final long RETRY_TIME = 2 * 60 * 1000;
    private long mNextReqTime = RETRY_TIME;
    private int mResultCode;

    public WeatherState(WeatherController controller) {
        super(controller);
    }

    @Override
    public void run(Object data) {
        // get weather
        if (DEBUG)
            Log.i(TAG, "weather state run");
        mBundle = (Bundle) data;

        if(DEBUG) Log.d(TAG, "Start to get weather " + mBundle);

        String woeid = mBundle.getString(State.BUNDLE_WOEID);
        String country = mBundle.getString(State.BUNDLE_COUNTRY);
        String province = mBundle.getString(State.BUNDLE_PROVINCE);
        String city = mBundle.getString(State.BUNDLE_CITY);
        WeatherInfo weatherInfo = mDataModel.getWeatherData(mBundle, woeid);
        if (weatherInfo != null && !weatherInfo.isNoData()) {
            boolean isRight = true;
            if (mController.isAutoLocate()) {
                isRight = checkDate(weatherInfo.getFormatDate());
            }
            if (isRight) {
                checkTemp(weatherInfo);
                mPreferences.setWeatherInfo(weatherInfo);
                mPreferences.setUpdateTime(System.currentTimeMillis());
                if (mController.isAutoLocate()) {
                    mPreferences.setAutoWoeid(woeid);
                    mPreferences.setAutoWoeidUpdateTime(System.currentTimeMillis());
                    mPreferences.setAutoLocationCityName(weatherInfo.getCity());
                } else {
                    mPreferences.setManualWoeid(woeid);
                    mPreferences.setManualLocationCountryName(country);
                    mPreferences.setManualLocationProvinceName(province);
                    mPreferences.setManualWoeidUpdateTime(System.currentTimeMillis());
                    if (!TextUtils.isEmpty(city)) {
                        mPreferences.setManualLocationCityName(city);
                    } else {
                        mPreferences.setManualLocationCityName(province);
                    }
                }
                mHandler.sendEmptyMessage(WeatherController.EVENT_REQUEST_WEATHER_SUCCESS);
                cancelRetry();
                return;
            }
            Log.e(TAG, "weather date has error : " + weatherInfo.getDate());
        } else {
            mResultCode = WeatherController.RESULT_ERROR_GET_WEATHER;
        }
        Message msg = Message.obtain(mHandler, WeatherController.EVENT_REQUEST_FAILE, mResultCode);
        mHandler.sendMessage(msg);
        setRetry();
    }

    @Override
    void setRetry() {
        // retry after 2min, 2+4min, 2+4+8min, 2+4+8+16min, then cancel retry
        if (mNextReqTime > 16 * 60 * 1000 || mController.isStop()) {
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
        if(DEBUG) Log.i(TAG, "start to get weather after: " + mNextReqTime + "ms");
        mAlarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextReq, sender);
        mNextReqTime = mNextReqTime * 2;
    }

    @Override
    void startRetry() {
        if (mCurRetryState == RetryState.RUNNING) {
            if (DEBUG)
                Log.i(TAG, "################## startRetry");
            run(mBundle);
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
            if(DEBUG) Log.i(TAG, "stop to get weather");
        }
        mCurRetryState = RetryState.IDLE;
        mNextReqTime = RETRY_TIME;
    }

    private boolean checkDate(Date date) {
        if (date == null) {
            mResultCode = WeatherController.RESULT_ERROR_GET_WEATHER;
            return false;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        long time = cal.getTimeInMillis();

        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(System.currentTimeMillis());
        int nowYear = now.get(Calendar.YEAR);
        int nowMonth = now.get(Calendar.MONTH);
        int nowDay = now.get(Calendar.DAY_OF_MONTH);
        long nowTime = now.getTimeInMillis();

        /*Fix issue: can't update time for a peroid of time after "New year day", "1st day of new month or 12:00AM". 
        **Don't check year, month and day. Check time difference only. Set 48 hours to adapt with China Weather Network.
        */
        if (!Utils.isInvalidWeather(nowTime, time)) {
            mResultCode = WeatherController.RESULT_ERROR_WEATHER_INFO;
            return false;
        }
        return true;
    }

    private void checkTemp(WeatherInfo info) {
        try {
            int temp = Integer.parseInt(info.getTemperature(WeatherInfo.TEMPERATURE_FMT_CELSIUS));
            int tempH = Integer.parseInt(info.getTempHigh(WeatherInfo.TEMPERATURE_FMT_CELSIUS));
            int tempL = Integer.parseInt(info.getTempLow(WeatherInfo.TEMPERATURE_FMT_CELSIUS));
            if (temp < tempL) {
                temp = tempL;
            } else if (temp > tempH) {
                temp = tempH;
            }
            info.setTemperature(String.valueOf(temp));
        } catch (NumberFormatException e){
            Log.e(TAG, "WeaherState checkTemp NumberformatException");
        }
    }
}
