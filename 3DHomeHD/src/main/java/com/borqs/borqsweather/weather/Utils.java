package com.borqs.borqsweather.weather;

import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.borqs.borqsweather.weather.yahoo.PinYinUtil;

public class Utils {

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager != null) {
            NetworkInfo networkinfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkinfo != null && networkinfo.isConnectedOrConnecting()) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkNetworkIsAvailable(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager != null) {
            NetworkInfo networkinfo = connManager.getActiveNetworkInfo();
            if (networkinfo == null || !networkinfo.isAvailable()) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    public static boolean isChinese(char ch) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(ch);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
            return true;
        }
        return false;
    }

    public static String changeHanZiToPinYin(String hanZi) {
        List<String> strList = PinYinUtil.getPinYin(hanZi);
        StringBuilder strB = new StringBuilder();
        for (String token : strList) {
            strB.append(token);
        }
        return strB.toString();
    }

    public static int getCountryCode(Context context) {
        int mcc = -1;
        TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        CellLocation cl = telManager.getCellLocation();
        if (cl != null && telManager.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE) {
            String strOperator = telManager.getNetworkOperator();
            if (!TextUtils.isEmpty(strOperator)) {
                try {
                    mcc = Integer.valueOf(strOperator.substring(0, 3));
                } catch (Exception e) {
                    Log.e("Test_Utils", "Get country code error. " + e.getMessage());
                }
            }
        }
        return mcc;
    }

    public static boolean isSameDate(long time1, long time2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(time2);

        if ((cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR))
                && (cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH))
                && (cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH))) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isInvalidWeather(long CurrentTime, long SpecifiedTime) {
        //Set 48 hours to adapt with China Weather Network.  
        //If weather you get is not elder than 48 hours, we think it is invalid.

        if ((CurrentTime - SpecifiedTime) > 48 * 1000 * 60 * 60) {
            return false;
        } else {
            return true;
        }
    }
    
    public static boolean isLocationServicesEnabled(Context context) {
        LocationManager lm= (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return true;
        }
        return false;
    }

    public static String get24Time(String time){
        String result = null;
        if (!TextUtils.isEmpty(time)) {
            result = time.toLowerCase();
            if (time.contains("am")) {
                result = time.replace("am", "").trim();
            } else if (time.contains("pm")) {
                result = time.replace("pm", "").trim();
                String[] hm = result.split(":");
                int h = 0;
                try {
                    h = Integer.parseInt(hm[0]);
                    if (h < 12)
                        h = h + 12;
                } catch (Exception e) {
                    return null;
                }
                result = "" + h + ":" + hm[1];
            } else {
                result = time.trim();
            }
        }
        return result;
    }
}
