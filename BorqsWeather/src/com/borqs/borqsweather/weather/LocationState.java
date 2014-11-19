package com.borqs.borqsweather.weather;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class LocationState extends State {

    private static final String TAG = "Weather_LocationState";

    public interface Locate {
        public void requestLocationAndCity(Bundle data);

        void stop();
    }

    private Locate mLocate;
    private final static int MAX_TRY_COUNT = 4; 
    private int mTryCount;
    private boolean mHasStoped = false;

    public LocationState(WeatherController controller) {
        super(controller);
        mTryCount = 0;
    }

    @Override
    public void run(Bundle data) {
        requestCity(null, true, true);
    }

    /**
     * 在中国使用百度SDK获取位置以及城市信息，在中国以外百度SDK不可用所以只能使用ANDROID自带接口获取位置以及城市信息
     */
    private void requestCity(Bundle data, boolean auto, boolean usingBaidu) {
        if (mHasStoped) {
            return;
        }
        mTryCount++;
        if (mTryCount > MAX_TRY_COUNT) {
            mTryCount--;
            onFailed(null);
            return;
        }
        int countryCode = Utils.getCountryCode(mContext);
        if (auto) {           
            if (countryCode == 460) {
                if (WeatherController.DEBUG)
                    Log.i(TAG, "### request location ### using baidu server for location, mTryCount = " + mTryCount);
                mLocate = new BDLocate(this);
            } else {
                if (WeatherController.DEBUG)
                    Log.i(TAG, "### request location ### using google server for location, mTryCount = " + mTryCount);
                mLocate = new GLocate(this);
            }
        } else {
            if (usingBaidu && countryCode == 460) {
                if (WeatherController.DEBUG)
                    Log.i(TAG, "### request location ### using baidu server for location, mTryCount = " + mTryCount);
                mLocate = new BDLocate(this);
            } else {
                if (WeatherController.DEBUG)
                    Log.i(TAG, "### request location ### using google server for location, mTryCount = " + mTryCount);
                mLocate = new GLocate(this);
            }
        }
        mLocate.requestLocationAndCity(data);
    }

    public void onReceiveCity(final Boolean isBaiDu, final Bundle data) {
        if (data.getBoolean(State.BUNDLE_HAS_LOCATION, false)) {
            onSuccess(data);
        } else {
            processTask(new Runnable() {
                public void run() {
                    requestCity(data, false, !isBaiDu);
                }
            }, 0);

        }
    }

    void stop() {
        if (WeatherController.DEBUG)
            Log.i(TAG, "########## stop request location ##### ");
        mHasStoped = true;
        if (mLocate != null) {
            mLocate.stop();
        }
    }

    @Override
    void onSuccess(Bundle data) {
        if (mHasStoped) {
            return;
        }
        if (WeatherController.DEBUG)
            Log.i(TAG, "###request location successfully### latitude= " + data.getString(State.BUNDLE_LATITUDE)
                    + ",longtude=" + data.getString(State.BUNDLE_LONGITUDE));
        Message msg = Message.obtain(mHandler, WeatherController.EVENT_REQUEST_LOCATION_SUCCESS);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    @Override
    void onFailed(Bundle data) {
        if (mHasStoped) {
            return;
        }
        Log.i(TAG, "##########request location failed after try " + mTryCount + " times");
        Message msg = Message.obtain(mHandler, WeatherController.EVENT_REQUEST_FAILE,
                WeatherController.RESULT_ERROR_LOCATION);
        mHandler.sendMessage(msg);

    }
}
