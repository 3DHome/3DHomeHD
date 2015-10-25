package com.borqs.borqsweather.weather.yahoo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import com.borqs.borqsweather.weather.Utils;
import com.borqs.borqsweather.weather.WeatherController;


import android.text.TextUtils;
import android.util.Log;

public class YahooWeatherHelper {
    /** For debugging */
    private static final String TAG = "Weather_whelper";

    /** Yahoo attribute */
    private static final String PARAM_YAHOO_LOCATION = "yweather:location";
    private static final String PARAM_YAHOO_CONDITION = "yweather:condition";
    private static final String PARAM_YAHOO_FORECAST = "yweather:forecast";
    private static final String PARAM_YAHOO_ASTRONOMY = "yweather:astronomy";

    /** Attribute city */
    private static final String ATT_YAHOO_CITY = "city";
    private static final String ATT_YAHOO_COUNTRY = "country";
    private static final String ATT_YAHOO_TEXT = "text";
    private static final String ATT_YAHOO_CODE = "code";
    private static final String ATT_YAHOO_DATE = "date";
    private static final String ATT_YAHOO_TEMP = "temp";
    private static final String ATT_YAHOO_TEMP_HIGH = "high";
    private static final String ATT_YAHOO_TEMP_LOW = "low";
    private static final String ATT_YAHOO_SUN_RISE = "sunrise";
    private static final String ATT_YAHOO_SUN_SET = "sunset";

    public static WeatherInfo parserYahooWeatherInfo(Document docWeather) {
        if (docWeather == null) {
            if (WeatherController.DEBUG)
                Log.e(TAG, "Invalid doc weatehr");
            return null;
        }

        String strCity = null;
        String strCountry = null;
        String strTempUnit = null;
        String strTempValue = null;
        String strHumidity = null;
        String strText = null;

        String strCode = null;

        String strDate = null;

        String strVisi = null;
        String strTempHigh = null;
        String strTempLow = null;
        
        String strSunrise = null;
        String strSunset = null;
        
        try {
            Element root = docWeather.getDocumentElement();
            root.normalize();

            NamedNodeMap locationNode = root.getElementsByTagName(PARAM_YAHOO_LOCATION).item(0).getAttributes();

            if (locationNode != null) {
                strCity = locationNode.getNamedItem(ATT_YAHOO_CITY).getNodeValue();
                strCountry = locationNode.getNamedItem(ATT_YAHOO_COUNTRY).getNodeValue();
            }


            NamedNodeMap conditionNode = root.getElementsByTagName(PARAM_YAHOO_CONDITION).item(0).getAttributes();
            if (conditionNode != null) {
                strText = conditionNode.getNamedItem(ATT_YAHOO_TEXT).getNodeValue();
                strCode = conditionNode.getNamedItem(ATT_YAHOO_CODE).getNodeValue();
                strDate = conditionNode.getNamedItem(ATT_YAHOO_DATE).getNodeValue();
                strTempValue = conditionNode.getNamedItem(ATT_YAHOO_TEMP).getNodeValue();
            }

            NamedNodeMap temHNode = root.getElementsByTagName(PARAM_YAHOO_FORECAST).item(0).getAttributes();
            if (temHNode != null) {
                strTempHigh = temHNode.getNamedItem(ATT_YAHOO_TEMP_HIGH).getNodeValue();
                strTempLow = temHNode.getNamedItem(ATT_YAHOO_TEMP_LOW).getNodeValue();
            }
            
            NamedNodeMap astronomyNode = root.getElementsByTagName(PARAM_YAHOO_ASTRONOMY).item(0).getAttributes();

            if (astronomyNode != null) {
                strSunrise = astronomyNode.getNamedItem(ATT_YAHOO_SUN_RISE).getNodeValue();
                strSunset = astronomyNode.getNamedItem(ATT_YAHOO_SUN_SET).getNodeValue();
            }
            //Log.i(TAG, "sun rise/set get from server: " + strSunrise + " / " + strSunset);
        } catch (Exception e) {
            Log.e(TAG, "Something wrong with parser weather data");
            return null;
        }

        strSunrise = Utils.get24Time(strSunrise);
        if (TextUtils.isEmpty(strSunrise)) {
            strSunrise = "6:00";
        }
        strSunset = Utils.get24Time(strSunset);
        if (TextUtils.isEmpty(strSunset)) {
            strSunset = "19:00";
        }

        //Log.i(TAG, "sun rise/set format: " + strSunrise + " / " + strSunset);
        WeatherInfo yahooWeatherInfo = new WeatherInfo(strCity, strCountry, strTempValue, strHumidity, strText,
                strCode, strDate, strTempUnit, strVisi, strTempHigh, strTempLow, strSunrise, strSunset);


        Log.d(TAG, "Weather information: Begin");
        Log.d(TAG, "Place: " + strCity + ", " +strCountry);
        Log.d(TAG, "Weather temp: " + strTempValue + ", " + "condidtion: " + strCode + ", " + "high: " + strTempHigh + ", " + "low: " + strTempLow + ", " + "unit: " + strTempUnit);
        Log.d(TAG, "Sunrise: " + strSunrise + ", " + "Sunset: " + strSunset);
        Log.d(TAG, "Weather information: End");

        return yahooWeatherInfo;
    }
}
