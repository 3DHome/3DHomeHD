package com.borqs.borqsweather.weather;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.borqs.se.home3d.HomeUtils;
import com.borqs.borqsweather.weather.LocationState.Locate;

public class GLocate implements Locate {

    private static final String TAG = "Weather_LocationState";
    private static boolean DEBUG = HomeUtils.DEBUG;

    private LocationState mState;
    private String mCity;
    private double mLatitude;
    private double mLongitude;
    private Timer mTimer;
    private LocationManager mLocationManager = null;
    private boolean mFirstRequest;
    
    private final LocationListener mLocationListener = new LocationListener() {

        public void onLocationChanged(Location location) {
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }
            mLocationManager.removeUpdates(mLocationListener);
            mCity = getCityNameFromLocation(location);
            mLatitude = location.getLatitude();
            mLongitude = location.getLongitude();
            mState.onReceiveCity(false, getBundle());
        }

        public void onProviderDisabled(String provider) {
            if (DEBUG)
                Log.i(TAG, "onProviderDisabled");
        }

        public void onProviderEnabled(String provider) {
            if (DEBUG)
                Log.i(TAG, "onProviderEnabled");
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (DEBUG)
                Log.i(TAG, "onStatusChanged");
        }
    };

    public GLocate(LocationState state, boolean first) {
        mState = state;
        mFirstRequest = first;
        mLocationManager = (LocationManager) mState.mContext.getSystemService(Context.LOCATION_SERVICE);
    }
    
    public boolean isFirstRequest(){
        return mFirstRequest;
    }

    private void requestCurrentLocation() {
        if (!Utils.isLocationServicesEnabled(mState.mContext)) {
            mState.onReceiveCity(false, getBundle());
            return;
        }

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        mTimer = new Timer();
        mTimer.schedule(new CancelLocateTask(), 1000 * 35);
        if (DEBUG)
            Log.i(TAG, "request location");
        if (isLocationProviderEnabled()) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
        } else {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        }
    }

    private class CancelLocateTask extends TimerTask {
        @Override
        public void run() {
            if (DEBUG)
                Log.d(TAG, "##############################  timer cancel locate");
            mLocationManager.removeUpdates(mLocationListener);
            mState.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mState.onReceiveCity(false, getBundle());
                }
            });
        }
    }

    private String getCityNameFromLocation(Location location) {
        double[] coordinates = getGeoCoordinates(location);
        return mState.mDataModel.getCityByLocation(coordinates);
    }

    private double[] getGeoCoordinates(Location location) {
        double[] geo_coordinates = { Double.MAX_VALUE, Double.MAX_VALUE };
        if (location != null) {
            geo_coordinates[0] = location.getLatitude();
            geo_coordinates[1] = location.getLongitude();
            if (DEBUG)
                Log.d(TAG, "############### " + geo_coordinates[0] + ", " + geo_coordinates[1] + "###############");
        }
        return geo_coordinates;
    }

    private boolean isLocationProviderEnabled() {
        return mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public Bundle getBundle() {
        Bundle data = new Bundle();
        data.putString(State.BUNDLE_CITY, mCity);
        data.putDouble(State.BUNDLE_LATITUDE, mLatitude);
        data.putDouble(State.BUNDLE_LONGITUDE, mLongitude);
        return data;
    }

    public void getCity() {
        // TODO now we only get city from Google's location service
        // if (Utils.isWifiConnected(mState.mContext)) {
        // mCity = mState.mDataModel.getCityFromWifiInfo(mState.mContext);
        // Log.i(TAG, "get city wifi : " + mCity);
        // if (!TextUtils.isEmpty(mCity)) {
        // mState.onReceiveCity(false, mCity);
        // }
        // }
        // mCity = mState.mDataModel.getCityFromCellLocation(mState.mContext);
        // Log.i(TAG, "get city cell : " + mCity);
        // if (TextUtils.isEmpty(mCity)) {
        requestCurrentLocation();
        // }
    }

    public void stop() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
    }
}
