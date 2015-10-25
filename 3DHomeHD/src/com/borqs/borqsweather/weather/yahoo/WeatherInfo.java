package com.borqs.borqsweather.weather.yahoo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.text.TextUtils;
import android.text.format.DateFormat;

public class WeatherInfo {

    private static final String TAG = "Weather_weather_info";

    /** Initialize description */
    private static final String DEFAULT_DATA = "No data";

    /** Celsius temperature */
    public static final int TEMPERATURE_FMT_CELSIUS = 1;

    /** Fahrenheit temperature */
    public static final int TEMPERATURE_FMT_FAHRENHEIT = 2;

    /** City */
    private String m_City;

    /** Country */
    private String m_Country;

    /** Weather Temperature, in Celsius */
    private String m_Temperature;

    /** Humidity */
    private String m_Humidity;

    /** Weather Text */
    private String m_Text;

    /** Weather Wind */
    private String m_Code;

    /** Date */
    private String m_Date;

    /** Temp unit */
    private String m_TemperatureUnit;

    /** Visibility */
    private String m_Visi;

    /** High temperature */
    private String m_TempHigh;

    /** Low temperature */
    private String m_TempLow;

    /** Sun rise, sun set **/
    private String m_SunriseTime;
    private String m_SunsetTime;

    public WeatherInfo() {
        this(DEFAULT_DATA, DEFAULT_DATA, DEFAULT_DATA, DEFAULT_DATA, DEFAULT_DATA, "3200", DEFAULT_DATA, DEFAULT_DATA,
                DEFAULT_DATA, DEFAULT_DATA, DEFAULT_DATA, null, null);
    }

    public WeatherInfo(String city, String temp, String tempH, String tempL, String condition, String date,
            String code, String sunrise, String sunset) {
        this(city, DEFAULT_DATA, temp, DEFAULT_DATA, condition, "3200", date, DEFAULT_DATA, DEFAULT_DATA, tempH, tempL,
                sunrise, sunset);
    }

    public WeatherInfo(String strCity, String strCountry, String strTem, String strHum, String strText, String strCode,
            String strDate, String strTempUnit, String strVisi, String strTempHigh, String strTempLow,
            String strSunrise, String strSunset) {
        m_City = (strCity == null ? DEFAULT_DATA : strCity);
        m_Country = (strCountry == null ? DEFAULT_DATA : strCountry);
        m_Temperature = (strTem == null ? DEFAULT_DATA : strTem);
        m_Humidity = (strHum == null ? DEFAULT_DATA : strHum);
        m_Text = (strText == null ? DEFAULT_DATA : strText);
        m_Code = (strCode == null ? "3200" : strCode);
        m_Date = (strDate == null ? DEFAULT_DATA : strDate);
        m_TemperatureUnit = (strTempUnit == null ? DEFAULT_DATA : strTempUnit);
        m_Visi = (strVisi == null ? DEFAULT_DATA : strVisi);
        m_TempHigh = (strTempHigh == null ? DEFAULT_DATA : strTempHigh);
        m_TempLow = (strTempLow == null ? DEFAULT_DATA : strTempLow);
        m_SunriseTime = strSunrise;
        m_SunsetTime = strSunset;
    }

    public void setTemperature(String nValue) {
        m_Temperature = nValue;
    }

    public String getTemperature(int nTempFmt) {
        if (nTempFmt == TEMPERATURE_FMT_FAHRENHEIT) {
            return celsiusToFahrenheit(m_Temperature);
        }

        return m_Temperature;
    }

    public void setTempUnit(String nValue) {
        m_TemperatureUnit = nValue;
    }

    public String getTempUnit() {
        return m_TemperatureUnit;
    }

    public void setCode(String nValue) {
        m_Code = nValue;
    }

    public String getCode() {
        return m_Code;
    }

    public String getCity() {
        return m_City;
    }

    public void setCity(String city) {
        m_City = city;
    }

    public String getDate() {
        if (m_Date != null) {
            m_Date = formatDate(m_Date);
        }
        return m_Date;
    }

    public void setDate(String strValue) {
        m_Date = strValue;
    }

    public String getCountry() {
        return m_Country;
    }

    public void setCountry(String country) {
        m_Country = country;
    }

    public String getHumidity() {
        return m_Humidity;
    }

    public void setHumidity(String humidity) {
        m_Humidity = humidity;
    }

    public String getText() {
        return m_Text;
    }

    public void setText(String strText) {
        m_Text = strText;
    }

    public String getVisibility() {
        return m_Visi;
    }

    public void setVisibility(String strText) {
        m_Visi = strText;
    }

    public String getTempHigh(int nTempFmt) {
        if (nTempFmt == TEMPERATURE_FMT_FAHRENHEIT) {
            return celsiusToFahrenheit(m_TempHigh);
        }
        return m_TempHigh;
    }

    public void setTempHigh(String strTempHigh) {
        this.m_TempHigh = strTempHigh;
    }

    public String getTempLow(int nTempFmt) {
        if (nTempFmt == TEMPERATURE_FMT_FAHRENHEIT) {
            return celsiusToFahrenheit(m_TempLow);
        }
        return m_TempLow;
    }

    public void setTempLow(String strTempLow) {
        this.m_TempLow = strTempLow;
    }

    public String getSunriseTime() {
        return m_SunriseTime;
    }

    public void setSunriseTime(String strSunriseTime) {
        m_SunriseTime = strSunriseTime;
    }

    public String getSunsetTime() {
        return m_SunsetTime;
    }

    public void setSunsetTime(String strSunsetTime) {
        m_SunsetTime = strSunsetTime;
    }

    public boolean isNoData() {
        if (TextUtils.isEmpty(m_City) || TextUtils.isEmpty(m_Date) || TextUtils.isEmpty(m_Text)
                || TextUtils.isEmpty(m_Temperature) || TextUtils.isEmpty(m_TempHigh) || TextUtils.isEmpty(m_TempLow)
                || TextUtils.isEmpty(m_Code) || DEFAULT_DATA.equals(m_City) || DEFAULT_DATA.equals(m_Date)
                || DEFAULT_DATA.equals(m_Text) || DEFAULT_DATA.equals(m_Temperature) || DEFAULT_DATA.equals(m_TempHigh)
                || DEFAULT_DATA.equals(m_TempLow) || "3200".equals(m_Code)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof WeatherInfo)) {
            return false;
        }

        WeatherInfo info = (WeatherInfo) object;
        if (info.getCity().equals(m_City) && info.getDate().equals(m_Date) && info.getText().equals(m_Text)
                && info.getTemperature(0).equals(m_Temperature) && info.getTempHigh(0).equals(m_TempHigh)
                && info.getTempLow(0).equals(m_TempLow) && info.getCode().equals(m_Code)) {
            return true;
        } else {
            return false;
        }
    }

    public Date getFormatDate() {
        if (!TextUtils.isEmpty(m_Date) && !DEFAULT_DATA.equals(m_Date)) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm aa", Locale.ENGLISH);
                Date date = (Date) sdf.parseObject(m_Date);
                return date;
            } catch (Exception e) {
                // Log.e(TAG, "formate date has error: " + e.getMessage());
            }
        }
        return null;
    }

    private String formatDate(String strDateToParse) {
        StringBuilder sBuilder = new StringBuilder(strDateToParse);
        String strFormatDate = strDateToParse;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm aa", Locale.ENGLISH);
            Date date = (Date) sdf.parseObject(sBuilder.toString());
            strFormatDate = DateFormat.format("yyyy-MM-dd", date).toString();
        } catch (Exception e) {
            // Log.e(TAG, "formate date has error: " + e.getMessage());
        }
        return strFormatDate;
    }

    private String celsiusToFahrenheit(String cTemp) {
        if (TextUtils.isEmpty(cTemp) || DEFAULT_DATA.equals(cTemp)) {
            return "";
        }
        int c = Integer.parseInt(cTemp);
        float f = 9.0f * (float) c / 5.0f + 32.0f;
        return Integer.toString(Math.round(f));
    }
}
