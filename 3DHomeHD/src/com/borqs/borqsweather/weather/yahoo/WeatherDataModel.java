package com.borqs.borqsweather.weather.yahoo;

import java.net.URLEncoder;
import java.util.List;

import org.w3c.dom.Document;

import com.borqs.se.home3d.HomeUtils;
import com.borqs.borqsweather.weather.State;
import com.borqs.borqsweather.weather.yahoo.YahooLocationHelper.CityEntity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class WeatherDataModel {
    /** For debugging */
    private static final String TAG = "Weather_WeatherDataModel";

    private static final boolean DEBUG = HomeUtils.DEBUG;

    /** URL for Yahoo API */
    private static final String URL_YAHOO_API_WEATHER = "http://weather.yahooapis.com/forecastrss?w=%s&u=c";
    private static final String URL_LOCAL_API_WEATHER = "http://app.borqs.com:9328/forecastrss/city?%s";

    /** Query to get location */
    private static final String AUTO_GET_LOCATION_WOEID = "http://query.yahooapis.com/v1/public/yql?q=select+woeid+,+centroid+from+geo.places+where+text+=+";
    private static final String MANUAL_GET_LOCATION_WOEID = "http://query.yahooapis.com/v1/public/yql?q=select+woeid+,+country+,+admin1+,+admin2+from+geo.places+where+text+=+";

    /** Request google map to get city name by geo coordinates */
    private static final String GET_LOCATION_CITY = "https://maps.googleapis.com/maps/api/geocode/xml?latlng=%s,%s&sensor=false";

    /** Request type */
    private static final int YAHOO_API = 1;
    private static final int LOCAL_API = 2;

    /** Connect helper for connection */
    private HttpConnectHelper m_ConnectHelper = null;

    /** Data model instance */
    private static WeatherDataModel m_Instance = null;

    private WeatherDataModel() {
        /* Create for connect helper */
        m_ConnectHelper = new HttpConnectHelper();
    }

    public static synchronized WeatherDataModel getInstance() {
        if (m_Instance == null) {
            m_Instance = new WeatherDataModel();
        }

        return m_Instance;
    }

    public WeatherInfo getWeatherData(Bundle bundle, String woeid) {
        WeatherInfo weather = null;

        /* Firstly use Yahoo API */
        Document docWeather = getWeatherDocFromYahooServer(woeid);
        if (docWeather != null) {
            weather = YahooWeatherHelper.parserYahooWeatherInfo(docWeather);
        }
        if (weather == null) {
            /* Use local API */
            docWeather = getWeatherDocFromLocalServer(bundle, woeid);
            weather = YahooWeatherHelper.parserYahooWeatherInfo(docWeather);
        }
        return weather;
    }

    private Document getWeatherDocFromYahooServer(String woeid) {
        if (woeid == null) {
            Log.e(TAG, "Invalid location");
            return null;
        }
        Document docWeather = reqWeatherDoc(YAHOO_API, woeid);
        return docWeather;
    }

    public String transCoding(String code){
        if (!TextUtils.isEmpty(code)) {
            code = URLEncoder.encode(code);
        }
        return code;
    }

    private Document getWeatherDocFromLocalServer(Bundle bundle, String woeid) {
        if (DEBUG)
            Log.d(TAG, "get weather from local server");
        Document docWeather = null;
        String country = transCoding(bundle.getString(State.BUNDLE_COUNTRY));
        String province = transCoding(bundle.getString(State.BUNDLE_PROVINCE));
        String city = transCoding(bundle.getString(State.BUNDLE_CITY));
        String place = null;
        if (!TextUtils.isEmpty(country) && !TextUtils.isEmpty(province) && !TextUtils.isEmpty(city)) {
            place = "place=" + city + "," + province + "," + country + "&w=" + woeid;
            docWeather = reqWeatherDoc(LOCAL_API, place);
        } else if (!TextUtils.isEmpty(country) && !TextUtils.isEmpty(province)) {
            place = "place=" + province + "," + country + "&w=" + woeid;
            docWeather = reqWeatherDoc(LOCAL_API, place);
        } else if (!TextUtils.isEmpty(city) && !TextUtils.isEmpty(country)) {
            place = "place=" + city + "," + country + "&w=" + woeid;
            docWeather = reqWeatherDoc(LOCAL_API, place);
        } else if(!TextUtils.isEmpty(woeid)){
            docWeather = reqWeatherDoc(LOCAL_API, "w=" + woeid);
        } else {
            Log.e(TAG, "Invalid location info, need to check the issue further...");
            docWeather = null;
        }
        return docWeather;
    }

    private Document reqWeatherDoc(int apiType, String strData) {
        Document doc = null;
        String url = createURL(apiType, strData);
        if (url == null) {
            return null;
        }

        if(DEBUG) Log.d(TAG, "Request to weather server: " + url);        

        try {
            doc = m_ConnectHelper.getDocumentFromURL(url);
        } catch (Exception e) {
            Log.e(TAG, "Api get weather error:" + apiType + " | " + e.getMessage());
        }
        return doc;
    }

    public List<CityEntity> getCityList(String city) {
        try {
            String strQueryWOEID = createQueryGetWoeid(URLEncoder.encode(city, "utf-8"), MANUAL_GET_LOCATION_WOEID);
            if (strQueryWOEID == null) {
                Log.e(TAG, "getCityList Can not create WOEID");
                return null;
            }
            Document doc = m_ConnectHelper.getDocumentFromURL(strQueryWOEID);
            List<CityEntity> citys = YahooLocationHelper.getCitysFromDocument(city, doc);
            return citys;
        } catch (Exception e) {
            Log.e(TAG, "getCityList XML Pasing error woeid:" + e);
            return null;
        }
    }

    public String getWOEIDByLocation(Bundle bundle) {
        String strWOEID = null;
        String city = bundle.getString(State.BUNDLE_CITY).toLowerCase().trim().replaceAll(" ", "_");
        String strQueryWOEID = createQueryGetWoeid(city, AUTO_GET_LOCATION_WOEID);
        if (DEBUG)
            Log.d(TAG, "model, query woeid: " + strQueryWOEID);
        if (strQueryWOEID == null) {
            Log.e(TAG, "Can not create WOEID");
            return null;
        }

        try {
            Document doc = m_ConnectHelper.getDocumentFromURL(strQueryWOEID);
            if (doc != null) {
                strWOEID = YahooLocationHelper.parserWOEIDData(doc, bundle);
            }

        } catch (Exception e) {
            Log.e(TAG, "XML Pasing error woeid:" + e);
            return null;
        }

        return strWOEID;
    }

    private String createURL(int nRequestType, String strData) {
        if (strData == null) {
            Log.e(TAG, "Invalid input data");
            return null;
        }

        String strRegURL = null;
        switch (nRequestType) {
        case YAHOO_API:
            strRegURL = String.format(URL_YAHOO_API_WEATHER, strData);
            break;
        case LOCAL_API:
            strRegURL = String.format(URL_LOCAL_API_WEATHER, strData);
            break;
        default:
            Log.e(TAG, "Not support this request:" + nRequestType);
            return null;
        }

        return strRegURL;
    }

    private String createQueryGetWoeid(String strQuerry, String url) {
        if (strQuerry == null) {
            return null;
        }

        StringBuffer strQuerryBuf = new StringBuffer(url);
        strQuerryBuf.append('"');
        strQuerryBuf.append(strQuerry);
        strQuerryBuf.append('"');
        strQuerryBuf.append("&format=xml");

        return strQuerryBuf.toString();
    }

    public String getCityByLocation(double[] location) {
        String city = null;
        String urlString = createGetCityURL(location);
        try {
            Document loctionDoc = m_ConnectHelper.getDocumentFromURL(urlString);
            city = YahooLocationHelper.parserCityName(loctionDoc);
            if (DEBUG)
                Log.d(TAG, "get city by location: " + city);
        } catch (Exception e) {
            Log.e(TAG, "get city by location error : " + e.getMessage());
        }
        return city;
    }

    private String createGetCityURL(double[] location) {
        if (location == null || location[0] == Double.MAX_VALUE || location[1] == Double.MAX_VALUE) {
            return null;
        }

        String url = String.format(GET_LOCATION_CITY, location[0], location[1]);
        return url;
    }
}
