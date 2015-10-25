package com.borqs.borqsweather.weather;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.borqs.freehdhome.R;
import com.borqs.se.engine.SESceneManager;
import com.borqs.borqsweather.weather.yahoo.WeatherInfo;
import com.borqs.borqsweather.weather.yahoo.WeatherPreferences;

public class LoopForInfoService extends Service {

    public static final String TAG = "Weather_LoopForInfoService";
    private static final boolean DEBUG = false;

    public static final String INTENT_ACTION_WEATHER_LOOP_UPDATE = "3dhome.action.intent.WEATHER_LOOP_UPDATE";
    public static final String INTENT_ACTION_WEATHER_DAY_NIGHT_CHANGED = "3dhome.action.intent.WEATHER_DAY_NIGHT_CHANGED";
    public int[] mSunriseTime;
    public int[] mSunsetTime;

    private AlarmManager mAlarmMgr;
    private boolean mForceUpdate = false;
    private boolean mIsNight;
    private WeatherConditions mConditions;
    private WeatherPreferences mPreferences;
    private long mLastUpdateTime = 0;
    private boolean mIsFirst = true;

    private ArrayList<WeatherUpdateCallBack> mCallBacks = new ArrayList<WeatherUpdateCallBack>();
    private final IBinder mBinder = new LocalBinder();

    private WeatherController mController;

    private GLSObserver mGLSObserver;

    public interface WeatherUpdateCallBack {
        public void onWeatherUpdate(boolean forceUpdate);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mConditions = WeatherConditions.getInstance(this);
        mPreferences = WeatherPreferences.getInstance(this);
        updateSunriseSunsetTime();
        cancelDayNightChangedAlarm();
        setDayNightChangedAlarm();
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public LoopForInfoService getService() {
            // Return this instance of LoopForInfoService so clients can call
            // public methods
            return LoopForInfoService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG)
            Log.d(TAG, "onBind");
        if (mPreferences.needSetAlarmStartTime()) {
            checkToSetFirstStartTime();
            mPreferences.setAlarmStartState(false);
        }
        mIsNight = isNight();
        if (mGLSObserver == null) {
            mGLSObserver = new GLSObserver(new Handler(), this);
            mGLSObserver.startObserving();
        }
        IntentFilter filter = new IntentFilter(INTENT_ACTION_WEATHER_LOOP_UPDATE);
        filter.addAction(INTENT_ACTION_WEATHER_DAY_NIGHT_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(WeatherController.INTENT_ACTION_WEATHER_UPDATE);
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(mReceiver, filter);
        removeAlarm();
        if (WeatherSettings.isAutoUpdate(this)) {
            setAlarm();
        }
        firstUpdate();
        return mBinder;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (INTENT_ACTION_WEATHER_LOOP_UPDATE.equals(action)) {
                /*
                 * if (WeatherSettings.isAutoLocation(LoopForInfoService.this))
                 * { requestWeatherByLocation(); } else {
                 * requestWeatherByCity(mPreferences
                 * .getManualLocationCityName()); }
                 */
                requestWeather();
                removeAlarm();
                if (WeatherSettings.isAutoUpdate(LoopForInfoService.this)) {
                    setAlarm();
                }
            } else if (Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                saveUpdateTime(-1);
                removeAlarm();
                if (WeatherSettings.isAutoUpdate(LoopForInfoService.this)) {
                    mLastUpdateTime = 0;
                    setAlarm();
                }
                if (mIsNight != isNight()) {
                    mIsNight = !mIsNight;
                    forceRefresh();
                }
                cancelDayNightChangedAlarm();
                setDayNightChangedAlarm();
            } else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
                mConditions.reload();
                forceRefresh();
            } else if (WeatherController.INTENT_ACTION_WEATHER_UPDATE.equals(action)) {
                int resultCode = intent.getIntExtra(String.valueOf("state"), -1);
                if (resultCode >= 0) {
                    updateSunriseSunsetTime();
                    WeatherInfo info = getWeather();
                    if (checkWeatherInfoIsOld(info)) {
                        Log.e(TAG, "weather info is out of date");
                        mPreferences.clearWeatherInfo();
                    }
                    onWeatherUpdate();
                    cancelDayNightChangedAlarm();
                    setDayNightChangedAlarm();
                }
            } else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                if (DEBUG)
                    Log.d(TAG, "################### connectivity change###################");
                if (Utils.checkNetworkIsAvailable(LoopForInfoService.this)) {
                    checkToUpdateWeather(1000 * 60 * 60 * 3);
                }
            } else if (INTENT_ACTION_WEATHER_DAY_NIGHT_CHANGED.equals(action)) {
                Log.d(TAG, "################### Alarm to trigger weather update of daytime and night ###################");
                onWeatherUpdate();
                cancelDayNightChangedAlarm();
                setDayNightChangedAlarm();
            }
        }
    };

    @Override
    public void onDestroy() {
        if (DEBUG)
            Log.d(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        removeAlarm();
        clearWeatherUpdateCallBacks();
        if (mGLSObserver != null) {
            mGLSObserver.stopObserving();
            mGLSObserver = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (DEBUG)
            Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    private void setAlarm() {
        long time = calculateNextAlarm();
        if (time == -1) {
            if (DEBUG)
                Log.d(TAG, "not get next alarm time, return.");
            return;
        }
        mLastUpdateTime = time;

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        if (DEBUG) {
            Log.d(TAG, "next alarm : " + DateFormat.format("MM/dd/yy kk:mm:ss", c));
            Log.d(TAG, "lastupdatetime: " + mLastUpdateTime);
        }
        if (mAlarmMgr == null) {
            mAlarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        }
        Intent intent = new Intent(INTENT_ACTION_WEATHER_LOOP_UPDATE);
        PendingIntent sender = PendingIntent.getBroadcast(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        mAlarmMgr.set(AlarmManager.RTC_WAKEUP, time, sender);
    }

    public void removeAlarm() {
        if (mAlarmMgr == null) {
            mAlarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        }
        if (DEBUG)
            Log.d(TAG, "remove alarm");
        Intent intent = new Intent(INTENT_ACTION_WEATHER_LOOP_UPDATE);
        PendingIntent sender = PendingIntent.getBroadcast(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        mAlarmMgr.cancel(sender);
    }

    private long calculateNextAlarm() {
        Context context = getApplicationContext();
        long nextTime = -1;

        long nowTimeInMillis = System.currentTimeMillis();
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowTimeInMillis);

        int nowHour = now.get(Calendar.HOUR_OF_DAY);
        int nowMinute = now.get(Calendar.MINUTE);

        int[] start = WeatherSettings.getRealStartTime(context);
        int startHour = start[0];
        int startMinute = start[1];

        int[] end = WeatherSettings.getTime(context, false);
        int endHour = end[0];
        int endMinute = end[1];

        long intervalTime = WeatherSettings.getCurrentIntervalValue(context);

        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR));
        startCal.set(Calendar.HOUR_OF_DAY, startHour);
        startCal.set(Calendar.MINUTE, startMinute);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        if (DEBUG) {
            Log.d(TAG, "start calendar : " + DateFormat.format("MM/dd/yy kk:mm:ss", startCal));
            Log.d(TAG, "now calendar : " + DateFormat.format("MM/dd/yy kk:mm:ss", now));
            Log.d(TAG, "lastupdatetime 444: " + mLastUpdateTime);
        }

        int count = 0;
        Calendar nextCal = Calendar.getInstance();
        while (true) {
            if (count > 48) {
                nextTime = -1;
                break;
            }
            nextTime = startCal.getTimeInMillis() + count * intervalTime;
            if (nextTime == mLastUpdateTime) {
                count++;
                continue;
            }
            nextCal.setTimeInMillis(nextTime);
            if (nextCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                int nextHour = nextCal.get(Calendar.HOUR_OF_DAY);
                int nextMinute = nextCal.get(Calendar.MINUTE);
                if (nextHour > nowHour || nextHour == nowHour && nextMinute >= nowMinute) {
                    if (nextHour < endHour || nextHour == endHour && nextMinute <= endMinute) {
                        break;
                    } else {
                        startCal.add(Calendar.DAY_OF_YEAR, 1);
                        nextTime = startCal.getTimeInMillis();
                        break;
                    }
                }
            } else {
                startCal.add(Calendar.DAY_OF_YEAR, 1);
                nextTime = startCal.getTimeInMillis();
                break;
            }
            count++;
        }
        if (DEBUG)
            Log.d(TAG, "count = " + count);

        return nextTime;
    }

    public void requestWeather() {
        if (mController != null) {
            mController.stopController();
        }
        mController = new WeatherController(this);
        mController.request();
    }

    public void requestWeatherByWoeid(Bundle bundle) {
        if (mController != null) {
            mController.stopController();
        }
        mController = new WeatherController(this);
        mController.request(bundle);
    }

    public void checkToUpdateWeather(long interval) {
        long currentTime = System.currentTimeMillis();
        long lastUpdateTime = mPreferences.getUpdateTime();
        if ((currentTime - lastUpdateTime) > interval) {
            requestWeather();
        }
    }

    public boolean checkWeatherInfoIsOld(WeatherInfo info) {
        if (info == null || !WeatherSettings.isAutoLocation(this)) {
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date = null;
        try {
            date = (Date) sdf.parseObject(info.getDate());
            if (date != null) {
                if (!Utils.isInvalidWeather(System.currentTimeMillis(), date.getTime())) {
                    return true;
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "check weather date : " + e.getMessage());
        }
        return false;
    }

    public WeatherInfo getWeather() {
        return mPreferences.getWeatherInfo();
    }

    public void onWeatherUpdate() {
        if (mCallBacks != null) {
            for (WeatherUpdateCallBack callBack : mCallBacks) {
                if (DEBUG)
                    Log.d(TAG, "onWeatherUpdate, force refresh: " + mForceUpdate);
                callBack.onWeatherUpdate(mForceUpdate);
            }
        }
        mForceUpdate = false;
    }

    public void forceRefresh() {
        mForceUpdate = true;
        onWeatherUpdate();
    }

    public void setWeatherUpdateCallBack(WeatherUpdateCallBack callBack) {
        if (mCallBacks != null && !mCallBacks.contains(callBack)) {
            mCallBacks.add(callBack);
        }
    }

    private void clearWeatherUpdateCallBacks() {
        mCallBacks.clear();
    }

    public boolean isNight() {
        int riseHour = mSunriseTime[0];
        int riseMin = mSunriseTime[1];
        int setHour = mSunsetTime[0];
        int setMin = mSunsetTime[1];
        Time time = new Time();
        time.setToNow();
        int hour = time.hour;
        int min = time.minute;
        if (DEBUG)
            Log.i(TAG, "sun rise/set: " + riseHour + ":" + riseMin + " / " + setHour + ":" + setMin);
        if ((hour > setHour || hour < riseHour) || (hour == riseHour && min < riseMin)
                || (hour == setHour && min >= setMin)) {
            return true;
        } else {
            return false;
        }
    }

    public void saveUpdateTime(long time) {
        mPreferences.setUpdateTime(time);
    }

    private void firstUpdate() {
        if (mIsFirst) {
            /*
             * if (WeatherSettings.isAutoLocation(this)) {
             * requestWeatherByLocation(); } else { String city =
             * mPreferences.getManualLocationCityName();
             * requestWeatherByCity(city); }
             */
            requestWeather();
            mIsFirst = false;
        }
    }

    public void setAlarm(boolean clearLastUpdateTime) {
        if (clearLastUpdateTime) {
            mLastUpdateTime = 0;
        }
        setAlarm();
    }

    /**
     * Get weather type according to the weather code.
     * 
     * @return
     */
    public int getConditionType() {
        String code = mPreferences.getWeatherInfo().getCode();
        int type = mConditions.getConditionType(code);
        return type;
    }

    /**
     * Get weather condition string according to the weather code.
     * 
     * @return
     */
    public String getDisplayCondition() {
        String code = mPreferences.getWeatherInfo().getCode();
        String condition = mConditions.getConditionDisplayName(code);
        return condition;
    }

    public String getCity() {
        return mPreferences.getCityName();
    }

    public void checkLocationServices() {
        if (WeatherSettings.isAutoLocation(this) // auto-locate
                && ((460 != Utils.getCountryCode(this) && -1 != Utils.getCountryCode(this)) 
                || (-1 == Utils.getCountryCode(this) && Utils.isWifiConnected(this)))// abroad or wifi only
                && !Utils.isLocationServicesEnabled(this) // GLS disabled
                && !WeatherSettings.getGLSPromptStatus(this)) {// never show the
                                                               // dialog
            WeatherSettings.setGLSPromptStatus(this, true);
            SESceneManager.getInstance().runInUIThread(new Runnable() {

                @Override
                public void run() {
                    new AlertDialog.Builder(SESceneManager.getInstance().getGLActivity(),
                            AlertDialog.THEME_DEVICE_DEFAULT_DARK).setMessage(R.string.dialog_gls_msg)
                            .setPositiveButton(R.string.enable, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    try {
                                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        SESceneManager.getInstance().getGLActivity().startActivityForResult(intent, 0);
                                    } catch (ActivityNotFoundException e) {
                                        Toast.makeText(SESceneManager.getInstance().getContext(), R.string.activity_not_found,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }).setNegativeButton(R.string.disable, null).show();
                }
            });
        }
    }

    class GLSObserver extends ContentObserver {

        private Context mContext;

        GLSObserver(Handler handler, Context context) {
            super(handler);
            mContext = context;
        }

        void startObserving() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.LOCATION_PROVIDERS_ALLOWED),
                    false, this);
        }

        void stopObserving() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (WeatherSettings.isAutoLocation(LoopForInfoService.this) // auto-locate
                    && 460 != Utils.getCountryCode(LoopForInfoService.this) // abroad
                    && Utils.isLocationServicesEnabled(LoopForInfoService.this)) { // GLS
                                                                                   // enabled
                checkToUpdateWeather(1000 * 60 * 60 * 3);
            }
        }
    }

    private void checkToSetFirstStartTime() {
        SharedPreferences settings = getSharedPreferences(WeatherSettings.PREFS_AUTO_UPDATE_SETTING, 0);
        int hour = settings.getInt(WeatherSettings.TIME_START_HOUR_OF_DAY, 7);
        int min = settings.getInt(WeatherSettings.TIME_START_MINUTE, 0);
        WeatherSettings.saveRealStartTime(this, hour, min);
    }

    private void setDayNightChangedAlarm() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        int nowHour = cal.get(Calendar.HOUR_OF_DAY);
        int nowMin = cal.get(Calendar.MINUTE);
        if (nowHour < mSunriseTime[0] || ((nowHour == mSunriseTime[0]) && (nowMin < mSunriseTime[1]))) {
            cal.set(Calendar.HOUR_OF_DAY, mSunriseTime[0]);
            cal.set(Calendar.MINUTE, mSunriseTime[1]);
            cal.set(Calendar.SECOND, 0);
        } else if (nowHour < mSunsetTime[0] || ((nowHour == mSunsetTime[0]) && (nowMin < mSunsetTime[1]))) {
            cal.set(Calendar.HOUR_OF_DAY, mSunsetTime[0]);
            cal.set(Calendar.MINUTE, mSunsetTime[1]);
            cal.set(Calendar.SECOND, 0);
        } else {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, mSunriseTime[0]);
            cal.set(Calendar.MINUTE, mSunriseTime[1]);
            cal.set(Calendar.SECOND, 0);
        }
        if(DEBUG) Log.i(TAG, "sun : " + DateFormat.format("MM/dd/yy kk:mm:ss", cal));
        if (mAlarmMgr == null) {
            mAlarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        }
        Intent intent = new Intent(INTENT_ACTION_WEATHER_DAY_NIGHT_CHANGED);
        PendingIntent sender = PendingIntent.getBroadcast(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        mAlarmMgr.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
    }

    private void cancelDayNightChangedAlarm() {
        if(DEBUG) Log.i(TAG, "sun cancel alarm");
        if (mAlarmMgr == null) {
            mAlarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        }
        Intent intent = new Intent(INTENT_ACTION_WEATHER_DAY_NIGHT_CHANGED);
        PendingIntent sender = PendingIntent.getBroadcast(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        mAlarmMgr.cancel(sender);
    }

    private void updateSunriseSunsetTime() {
        if (mSunriseTime == null) {
            mSunriseTime = new int[] { 6, 0 };
        }
        if (mSunsetTime == null) {
            mSunsetTime = new int[] { 19, 0 };
        }
        String sunset = mPreferences.getSunsetTime();
        if (sunset != null) {
            String[] setTime = sunset.split(":");
            try {
                mSunsetTime[0] = Integer.parseInt(setTime[0]);
                mSunsetTime[1] = Integer.parseInt(setTime[1]);
            } catch (Exception e) {
                Log.e(TAG, "sun set error: " + sunset);
            }
        }
        String sunrise = mPreferences.getSunriseTime();
        if (sunrise != null) {
            String[] riseTime = sunrise.split(":");
            try {
                mSunriseTime[0] = Integer.parseInt(riseTime[0]);
                mSunriseTime[1] = Integer.parseInt(riseTime[1]);
            } catch (Exception e) {
                Log.e(TAG, "sun rise error: " + sunrise);
            }
        }
    }
}
