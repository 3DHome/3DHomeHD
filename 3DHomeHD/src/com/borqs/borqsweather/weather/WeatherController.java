package com.borqs.borqsweather.weather;

import java.util.HashMap;
import java.util.Iterator;

import com.borqs.se.home3d.HomeUtils;
import com.borqs.borqsweather.weather.yahoo.WeatherPreferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

public class WeatherController {
    private static final String TAG = "Weather_Controller";
    public static boolean DEBUG = HomeUtils.DEBUG;
    public static final String INTENT_ACTION_WEATHER_UPDATE = "3dhome.action.intent.UPDATE_WEATHER";

    enum StateType {
        IDLE, LOCATION, WOEID, WEATHER
    }

    public static final int EVENT_START_REQUEST = 0;
    public static final int EVENT_REQUEST_LOCATION_SUCCESS = EVENT_START_REQUEST + 1;
    public static final int EVENT_REQUEST_WOEID_SUCCESS = EVENT_REQUEST_LOCATION_SUCCESS + 1;
    public static final int EVENT_REQUEST_WEATHER_SUCCESS = EVENT_REQUEST_WOEID_SUCCESS + 1;
    public static final int EVENT_REQUEST_FAILE = EVENT_REQUEST_WEATHER_SUCCESS + 1;

    public static final int RESULT_NONE = -1;
    public static final int RESULT_ERROR_LOCATION = RESULT_NONE + 1;
    public static final int RESULT_ERROR_GET_WEATHER = RESULT_ERROR_LOCATION + 1;
    public static final int RESULT_ERROR_WEATHER_INFO = RESULT_ERROR_GET_WEATHER + 1;
    public static final int RESULT_SUCCESS = RESULT_ERROR_WEATHER_INFO + 1;
    public static final int RESULT_LOCATION_SUCCESS = RESULT_SUCCESS + 1;

    private StateType mCurrentState;
    private HashMap<StateType, State> mStateMap;

    private EventHandler mEventHandler;
    private HandlerThread mHandlerThread;

    private boolean mIsAutoLocate;
    private boolean mIsStoped;
    private Context mContext;

    public WeatherController(Context context) {
        mContext = context;
        mCurrentState = StateType.IDLE;
        mHandlerThread = new HandlerThread("Weather_request_" + System.currentTimeMillis());
        mHandlerThread.start();
        mEventHandler = new EventHandler(mHandlerThread.getLooper());
        initStateMap();
    }

    public void request() {
        if (DEBUG)
            Log.d(TAG, "################################# Controller request");
        if (!Utils.checkNetworkIsAvailable(mContext)) {
            Log.e(TAG, "Network is not available...");
            return;
        }

        mIsStoped = false;
        mIsAutoLocate = WeatherSettings.isAutoLocation(mContext);

        if (mIsAutoLocate) {
            transferToTargetState(StateType.LOCATION);
            mEventHandler.sendEmptyMessage(EVENT_START_REQUEST);
        } else {
            String woeid = WeatherPreferences.getInstance(mContext).getManualWoeid();
            String localcountry = WeatherPreferences.getInstance(mContext).getManualLocationCountryName();
            String localProvicne = WeatherPreferences.getInstance(mContext).getManualLocationProvinceName();
            String localCity = WeatherPreferences.getInstance(mContext).getManualLocationCityName();
            // if woeid is not null, provicne is null , city is null, to auto
            // get weather
            if (!TextUtils.isEmpty(woeid) && TextUtils.isEmpty(localProvicne) && TextUtils.isEmpty(localCity)) {
                transferToTargetState(StateType.LOCATION);
                mEventHandler.sendEmptyMessage(EVENT_START_REQUEST);
                return;
            }
            transferToTargetState(StateType.WEATHER);
            Bundle bundle = new Bundle();
            bundle.putString(State.BUNDLE_COUNTRY, localcountry);
            bundle.putString(State.BUNDLE_PROVINCE, localProvicne);
            bundle.putString(State.BUNDLE_CITY, localCity);
            bundle.putString(State.BUNDLE_WOEID, woeid);
            Message msg = mEventHandler.obtainMessage(EVENT_START_REQUEST, bundle);
            mEventHandler.sendMessage(msg);
        }

    }

    public void request(Bundle bundle) {
        if (DEBUG)
            Log.d(TAG, "################################# Controller request");
        if (!Utils.checkNetworkIsAvailable(mContext)) {
            Log.e(TAG, "Network is not available...");
            return;
        }

        mIsStoped = false;
        transferToTargetState(StateType.WEATHER);
        Message msg = mEventHandler.obtainMessage(EVENT_START_REQUEST, bundle);
        mEventHandler.sendMessage(msg);
    }

    StateType getCurrentStateType() {
        return mCurrentState;
    }

    public Context getContext() {
        return mContext;
    }

    public EventHandler getEventHandler() {
        return mEventHandler;
    }

    public boolean isAutoLocate() {
        return mIsAutoLocate;
    }

    public boolean isControllerRunning() {
        if (mCurrentState == StateType.IDLE) {
            return false;
        } else {
            return true;
        }
    }

    public void stopController() {
        mIsStoped = true;
        if (mStateMap != null) {
            mEventHandler.post(new Runnable() {
                @Override
                public void run() {
                    Iterator<State> states = mStateMap.values().iterator();
                    while (states.hasNext()) {
                        State state = states.next();
                        state.cancelRetry();
                    }
                    mStateMap.clear();
                    mEventHandler.removeCallbacksAndMessages(null);
                    // Quit event loop to avoid thread leak
                    mHandlerThread.quit();
                }
            });
        }
    }

    public boolean isStop() {
        return mIsStoped;
    }

    private void initStateMap() {
        mStateMap = new HashMap<StateType, State>();
        mStateMap.put(StateType.IDLE, new Idle(this));
        mStateMap.put(StateType.LOCATION, new LocationState(this));
        mStateMap.put(StateType.WOEID, new WoeidState(this));
        mStateMap.put(StateType.WEATHER, new WeatherState(this));
    }

    private void transferToNextState() {
        switch (mCurrentState) {
        case IDLE:
            mCurrentState = StateType.LOCATION;
            break;
        case LOCATION:
            mCurrentState = StateType.WOEID;
            break;
        case WOEID:
            mCurrentState = StateType.WEATHER;
            break;
        case WEATHER:
            mCurrentState = StateType.IDLE;
            break;
        default:
            break;
        }
        if (DEBUG)
            Log.i(TAG, "weather to get: " + mCurrentState);
    }

    private void transferToTargetState(StateType targetState) {
        mCurrentState = targetState;
    }

    private boolean hasNextState() {
        boolean hasNextState = true;
        if (mCurrentState == StateType.WEATHER) {
            hasNextState = false;
        }
        return hasNextState;
    }

    private void transferToIdleState() {
        mCurrentState = StateType.IDLE;
    }

    private State getState(StateType type) {
        switch (type) {
        case IDLE:
            return mStateMap.get(StateType.IDLE);

        case LOCATION:
            return mStateMap.get(StateType.LOCATION);

        case WOEID:
            return mStateMap.get(StateType.WOEID);

        case WEATHER:
            return mStateMap.get(StateType.WEATHER);

        default:
            return null;
        }
    }

    private void sendResult(int resultCode) {
        if (mIsStoped) {
            return;
        }
        Intent result = new Intent(INTENT_ACTION_WEATHER_UPDATE);
        result.putExtra("state", resultCode);
        mContext.sendBroadcast(result);
    }

    private class EventHandler extends Handler {
        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            State state;
            // String data = null;
            if (mIsStoped) {
                if (DEBUG)
                    Log.i(TAG, "stop to request weather");
                return;
            }
            switch (msg.what) {
            case EVENT_START_REQUEST:
                // data = () msg.obj;
                // if (DEBUG) Log.d(TAG,
                // "##### Controller EVENT_START_REQUEST : " + mCurrentState +
                // " | " + data);
                state = getState(mCurrentState);
                if (state != null) {
                    state.run(msg.obj);
                }
                break;
            case EVENT_REQUEST_LOCATION_SUCCESS:
                transferToNextState();
                state = getState(mCurrentState);
                if (state != null) {
                    state.run(msg.obj);
                }
                break;
            case EVENT_REQUEST_WOEID_SUCCESS:
                transferToNextState();
                state = getState(mCurrentState);
                if (state != null) {
                    state.run(msg.obj);
                }
                break;
            case EVENT_REQUEST_WEATHER_SUCCESS:
                sendResult(RESULT_SUCCESS);
                transferToIdleState();
                break;
            case EVENT_REQUEST_FAILE:
                int resultCode = (Integer) msg.obj;
                sendResult(resultCode);
                transferToIdleState();
                break;
            default:
                break;
            }
        }

    }

    class Idle extends State {
        public Idle(WeatherController controller) {
            super(controller);
        }

        @Override
        void run(Object data) {
        }
    }
}
