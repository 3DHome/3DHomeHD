package com.borqs.borqsweather.weather;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.borqs.se.home3d.HomeApplication;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.borqsweather.weather.LocationState.Locate;

public class BDLocate implements Locate {
    private static final String TAG = "Weather_BDLocate";
    private static boolean DEBUG = HomeUtils.DEBUG;

    private LocationState mState;
    private LocationClient mBDLocationClient;
    private BDLocationListener mBDLocationListener;
    private LocationClientOption mBDLocationClientOption;
    private String mCountry = "China";
    private String mProvince;
    private String mCity;
    private double mLatitude;
    private double mLongitude;
    private boolean mFirstRequest;

    private static String[] citySuffixs = {"市", "盟", "地区", "自治州", "特别行政区"};
    private static String[] provinceSuffixs = {"市", "省", "自治区", "特别行政区"};

    public BDLocate(LocationState state, boolean first) {
        mState = state;
        mFirstRequest = first;
    }

    public void getCity() {
        reqCityFromBaiduServer();
    }
    
    public boolean isFirstRequest(){
        return mFirstRequest;
    }

    private void reqCityFromBaiduServer() {
        mBDLocationClient = HomeApplication.getInstance().getBDLocationClient();
        mBDLocationListener = new BDLocationListener() {

            public void onReceivePoi(BDLocation arg0) {

            }

            public void onReceiveLocation(BDLocation location) {
                mCity = location.getCity();
                mProvince = location.getProvince();
                mLatitude = location.getLatitude();
                mLongitude = location.getLongitude();
                if (!TextUtils.isEmpty(mProvince) && Utils.isChinese(mProvince.charAt(0))) {
                    for (String suffixs : provinceSuffixs) {
                        if (mProvince.endsWith(suffixs)) {
                            mProvince = mProvince.replace(suffixs, "");
                            break;
                        }
                    }
                    mProvince = Utils.changeHanZiToPinYin(mProvince).toLowerCase();
                }
                if (!TextUtils.isEmpty(mCity) && Utils.isChinese(mCity.charAt(0))) {
                    for (String suffixs : citySuffixs) {
                        if (mCity.endsWith(suffixs)) {
                            mCity = mCity.replace(suffixs, "");
                            break;
                        }
                    }
                    mCity = Utils.changeHanZiToPinYin(mCity).toLowerCase();
                }
                
                Bundle data = new Bundle();
                data.putString(State.BUNDLE_COUNTRY, mCountry);
                data.putString(State.BUNDLE_PROVINCE, mProvince);
                data.putString(State.BUNDLE_CITY, mCity);
                //data.putString(State.BUNDLE_COUNTRY, mCountry);
                //data.putString(State.BUNDLE_PROVINCE, "Hubei");
                //data.putString(State.BUNDLE_CITY, "Huanggang");
                data.putDouble(State.BUNDLE_LATITUDE, mLatitude);
                data.putDouble(State.BUNDLE_LONGITUDE, mLongitude);
                mState.onReceiveCity(true, data);
                if (mBDLocationClient.isStarted()) {
                    if (DEBUG)
                        Log.d(TAG, "## onReceiveLocation stop bd service ##");
                    mBDLocationClient.unRegisterLocationListener(this);
                    mBDLocationClient.stop();
                }
            }
        };
        mBDLocationClient.registerLocationListener(mBDLocationListener);
        mBDLocationClientOption = new LocationClientOption();
        mBDLocationClientOption.setAddrType("all");
        mBDLocationClientOption.setTimeOut(15);
        mBDLocationClientOption.setProdName(HomeUtils.TAG);
        mBDLocationClientOption.setCoorType("bd09ll");
        mBDLocationClientOption.setPriority(LocationClientOption.NetWorkFirst);
        mBDLocationClient.setLocOption(mBDLocationClientOption);
        mBDLocationClient.start();
        mBDLocationClient.requestLocation();
    }

    public void stop() {
        if (mBDLocationClient != null && mBDLocationClient.isStarted()) {
            if (DEBUG)
                Log.d(TAG, "## stop bd service ##");
            mBDLocationClient.unRegisterLocationListener(mBDLocationListener);
            mBDLocationClient.stop();
        }
    }

    // private void printOption() {
    // Log.d(TAG, "## coorType ## " + mBDLocationClientOption.getCoorType());
    // Log.d(TAG, "## addrType ## " + mBDLocationClientOption.getAddrType());
    // Log.d(TAG, "## prodName ## " + mBDLocationClientOption.getProdName());
    // Log.d(TAG, "## poiDistance ## " +
    // mBDLocationClientOption.getPoiDistance());
    // Log.d(TAG, "## poiExtranInfo ## " +
    // mBDLocationClientOption.getPoiExtranInfo());
    // Log.d(TAG, "## poiNum ## " + mBDLocationClientOption.getPoiNumber());
    // Log.d(TAG, "## priority ## " + mBDLocationClientOption.getPriority());
    // Log.d(TAG, "## scanSpan ## " + mBDLocationClientOption.getScanSpan());
    // Log.d(TAG, "## serviceName ## " +
    // mBDLocationClientOption.getServiceName());
    // Log.d(TAG, "## timeOut ## " + mBDLocationClientOption.getTimeOut());
    // Log.d(TAG, "## isDisableCache ## " +
    // mBDLocationClientOption.isDisableCache());
    // Log.d(TAG, "## isLocationNotify ## " +
    // mBDLocationClientOption.isLocationNotify());
    // Log.d(TAG, "## isOpenGps ## " + mBDLocationClientOption.isOpenGps());
    // Log.d(TAG, "## isPoiOn ## " + mBDLocationClientOption.isPoiOn());
    // }

}
