package com.borqs.borqsweather.weather.yahoo;

import java.util.Calendar;

import com.borqs.borqsweather.weather.WeatherController;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.util.Log;

public class WeatherPreferences {
    /** TAG for debugging */
    private static final String TAG = "Weather_WeatherPreferences";
    /** Preferences name */
    private static final String WEATHER_PREFERENCE = "weather_preference";

    /** Key for last auto location city */
    private static final String KEY_AUTO_LOCATION_CITY_NAME = "auto_location";

    /** Key for last manual location city */
    private static final String KEY_MANUAL_LOCATION_CITY_NAME = "manual_location";

    /** Key for last manual location country */
    private static final String KEY_MANUAL_LOCATION_COUNTRY_NAME = "manual_country";

    /** Key for last manual location province */
    private static final String KEY_MANUAL_LOCATION_PROVINCE_NAME = "manual_province";

    /** Key for the last update time */
    private static final String KEY_TIME_UPDATE = "time_update";

    /** Key for the weather info */
    private static final String KEY_WEATHER_CITY = "weather_city";
    private static final String KEY_WEATHER_TEMP = "weather_temp";
    private static final String KEY_WEATHER_TEMP_HIGH = "weather_tempH";
    private static final String KEY_WEATHER_TEMP_LOW = "weather_tempL";
    private static final String KEY_WEATHER_CONDITION = "weather_condition";
    private static final String KEY_WEATHER_DATE = "weather_date";
    private static final String KEY_WEATHER_CODE = "weather_code";
    private static final String KEY_WEATHER_SUNRISE_TIME = "weather_sunrise_time";
    private static final String KEY_WEATHER_SUNSET_TIME = "weather_sunset_time";
    private static final String KEY_UPDATE_TIME = "update_time";

    private static final String KEY_AUTO_WOEID = "auto_woeid";
    private static final String KEY_AUTO_WOEID_TIME = "auto_woeid_time";
    private static final String KEY_MANUAL_WOEID = "manual_woeid";
    private static final String KEY_MANUAL_WOEID_TIME = "manual_woeid_time";

    /** Instance */
    private static WeatherPreferences m_Instance;

    /** Context application */
    private Context m_Context;

    private WeatherPreferences(Context context) {
        this.m_Context = context;
    }

    public synchronized static WeatherPreferences getInstance(Context context) {
        if (m_Instance == null) {
            m_Instance = new WeatherPreferences(context);
        }

        return m_Instance;
    }

    public void setAlarmStartState(boolean need) {
        SharedPreferences.Editor editor = m_Context.getSharedPreferences(WEATHER_PREFERENCE, Context.MODE_PRIVATE)
                .edit();
        editor.putBoolean("alarm_start_time", need);
        editor.commit();
    }

    public boolean needSetAlarmStartTime() {
        SharedPreferences spf = m_Context.getSharedPreferences(WEATHER_PREFERENCE, Context.MODE_PRIVATE);
        boolean need = spf.getBoolean("alarm_start_time", true);
        return need;
    }

    public String getAutoLocationCityName() {
        return _getStringPreferences(KEY_AUTO_LOCATION_CITY_NAME, "");
    }

    public boolean setAutoLocationCityName(String cityName) {
        return _setStringPreferences(KEY_AUTO_LOCATION_CITY_NAME, cityName);
    }

    public String getManualLocationCityName() {
        return _getStringPreferences(KEY_MANUAL_LOCATION_CITY_NAME, "");
    }

    public boolean setManualLocationCityName(String cityName) {
        return _setStringPreferences(KEY_MANUAL_LOCATION_CITY_NAME, cityName);
    }

    public String getManualLocationCountryName() {
        return _getStringPreferences(KEY_MANUAL_LOCATION_COUNTRY_NAME, "");
    }

    public boolean setManualLocationCountryName(String countryName) {
        return _setStringPreferences(KEY_MANUAL_LOCATION_COUNTRY_NAME, countryName);
    }

    public String getManualLocationProvinceName() {
        return _getStringPreferences(KEY_MANUAL_LOCATION_PROVINCE_NAME, "");
    }

    public boolean setManualLocationProvinceName(String provinceName) {
        return _setStringPreferences(KEY_MANUAL_LOCATION_PROVINCE_NAME, provinceName);
    }

    public boolean setAutoWoeid(String woeid) {
        return _setStringPreferences(KEY_AUTO_WOEID, woeid);
    }

    public String getAutoWoeid() {
        return _getStringPreferences(KEY_AUTO_WOEID, "");
    }

    public void setAutoWoeidUpdateTime(long time) {
        SharedPreferences.Editor editor = m_Context.getSharedPreferences(WEATHER_PREFERENCE, Context.MODE_PRIVATE)
                .edit();
        editor.putLong(KEY_AUTO_WOEID_TIME, time);
        editor.commit();
    }

    public long getAutoWoeidUpdateTime() {
        SharedPreferences spf = m_Context.getSharedPreferences(WEATHER_PREFERENCE, Context.MODE_PRIVATE);
        long time = spf.getLong(KEY_AUTO_WOEID_TIME, -1);
        return time;
    }

    public boolean setManualWoeid(String woeid) {
        return _setStringPreferences(KEY_MANUAL_WOEID, woeid);
    }

    public String getManualWoeid() {
        return _getStringPreferences(KEY_MANUAL_WOEID, "");
    }

    public void setManualWoeidUpdateTime(long time) {
        SharedPreferences.Editor editor = m_Context.getSharedPreferences(WEATHER_PREFERENCE, Context.MODE_PRIVATE)
                .edit();
        editor.putLong(KEY_MANUAL_WOEID_TIME, time);
        editor.commit();
    }

    public long getManualWoeidUpdateTime() {
        SharedPreferences spf = m_Context.getSharedPreferences(WEATHER_PREFERENCE, Context.MODE_PRIVATE);
        long time = spf.getLong(KEY_MANUAL_WOEID_TIME, -1);
        return time;
    }

    public String getUpdateTimeString() {
        return _getStringPreferences(KEY_UPDATE_TIME, "");
    }

    public void setWeatherInfo(WeatherInfo info) {
        if (info == null) {
            return;
        }
        _setStringPreferences(KEY_WEATHER_CITY, info.getCity());
        _setStringPreferences(KEY_WEATHER_CONDITION, info.getText());
        _setStringPreferences(KEY_WEATHER_DATE, info.getDate());
        _setStringPreferences(KEY_WEATHER_TEMP, info.getTemperature(0));
        _setStringPreferences(KEY_WEATHER_TEMP_HIGH, info.getTempHigh(0));
        _setStringPreferences(KEY_WEATHER_TEMP_LOW, info.getTempLow(0));
        _setStringPreferences(KEY_WEATHER_CODE, info.getCode());
        _setStringPreferences(KEY_WEATHER_SUNRISE_TIME, info.getSunriseTime());
        _setStringPreferences(KEY_WEATHER_SUNSET_TIME, info.getSunsetTime());
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        _setStringPreferences(KEY_UPDATE_TIME, DateFormat.format("MM/dd/yy kk:mm", cal).toString());
        if (WeatherController.DEBUG)
            Log.i(TAG, "save weather time: " + DateFormat.format("MM/dd/yy kk:mm", cal).toString());
    }

    public WeatherInfo getWeatherInfo() {
        WeatherInfo info = new WeatherInfo();
        info.setCity(_getStringPreferences(KEY_WEATHER_CITY, ""));
        info.setDate(_getStringPreferences(KEY_WEATHER_DATE, ""));
        info.setTemperature(_getStringPreferences(KEY_WEATHER_TEMP, ""));
        info.setText(_getStringPreferences(KEY_WEATHER_CONDITION, ""));
        info.setTempHigh(_getStringPreferences(KEY_WEATHER_TEMP_HIGH, ""));
        info.setTempLow(_getStringPreferences(KEY_WEATHER_TEMP_LOW, ""));
        info.setCode(_getStringPreferences(KEY_WEATHER_CODE, "3200"));
        info.setSunriseTime(_getStringPreferences(KEY_WEATHER_SUNRISE_TIME, ""));
        info.setSunsetTime(_getStringPreferences(KEY_WEATHER_SUNSET_TIME, ""));
        return info;
    }

    public String getSunriseTime() {
        return _getStringPreferences(KEY_WEATHER_SUNRISE_TIME, "");
    }

    public String getSunsetTime() {
        return _getStringPreferences(KEY_WEATHER_SUNSET_TIME, "");
    }

    public void clearWeatherInfo() {
        _setStringPreferences(KEY_WEATHER_CITY, "");
        _setStringPreferences(KEY_WEATHER_CONDITION, "");
        _setStringPreferences(KEY_WEATHER_DATE, "");
        _setStringPreferences(KEY_WEATHER_TEMP, "");
        _setStringPreferences(KEY_WEATHER_TEMP_HIGH, "");
        _setStringPreferences(KEY_WEATHER_TEMP_LOW, "");
        _setStringPreferences(KEY_WEATHER_CODE, "3200");
    }

    public void setUpdateTime(long time) {
        SharedPreferences.Editor editor = m_Context.getSharedPreferences(WEATHER_PREFERENCE, Context.MODE_PRIVATE)
                .edit();
        editor.putLong(KEY_TIME_UPDATE, time);
        editor.commit();
    }

    public long getUpdateTime() {
        SharedPreferences spf = m_Context.getSharedPreferences(WEATHER_PREFERENCE, Context.MODE_PRIVATE);
        long time = spf.getLong(KEY_TIME_UPDATE, -1);
        return time;
    }

    public void setCityName(String city) {
        _setStringPreferences("location", city);
    }

    public String getCityName() {
        return _getStringPreferences("location", "");
    }

    private String _getStringPreferences(String strKey, String strDefaultValue) {
        /* Verify input */
        if (strKey == null) {
            if (WeatherController.DEBUG)
                Log.e(TAG, "Invalid input parameter");
            return strDefaultValue;
        }

        /* Get value of setting from preference */
        SharedPreferences preferences = m_Context.getSharedPreferences(WEATHER_PREFERENCE, Context.MODE_PRIVATE);

        return preferences.getString(strKey, strDefaultValue);
    }

    private boolean _setStringPreferences(String strKey, String strValue) {
        /* Verify input parameter */
        if ((strKey == null) || (strValue == null)) {
            if (WeatherController.DEBUG)
                Log.e(TAG, "Invalid input parameter");
            return false;
        }

        SharedPreferences preferences = m_Context.getSharedPreferences(WEATHER_PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor settingEditor = preferences.edit();
        settingEditor.putString(strKey, strValue);
        boolean bResult = settingEditor.commit();

        return bResult;
    }
}
