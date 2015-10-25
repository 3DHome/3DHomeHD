package com.borqs.se.home3d;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;

import com.borqs.freehdhome.R;
import com.borqs.se.engine.SERenderView;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.shortcut.WidgetWorkSpace;
import com.borqs.se.widget3d.ADViewController;
import com.borqs.se.widget3d.House;
import com.support.StaticActivity;
import com.support.StaticUtil;

public class HomeActivity extends StaticActivity implements View.OnCreateContextMenuListener, SensorEventListener {
    private final String TAG = HomeUtils.TAG;

    private WorkSpace mWorkSpace;
    private SERenderView mRenderView;
    private SESceneManager mSESceneManager;
    private ADViewController mADViewController;

    private static boolean sIsScreenLarge;
    private HomeReceiver mHomeReceiver;
    private Handler mHandler;
    private boolean mHasLeave3DHome;
    private boolean mUserLeaveHint;
    private boolean mFirstLoad;
    private PowerManager mPowerManager;
    private boolean isLiving;
    private Sensor mASensor;
    private Sensor mMSensor;

    @Override
    protected void onCreate(Bundle icicle) {
        HomeUtils.startTracing();

        HomeUtils.ensureDataFilesPath(getApplicationContext());
        if (HomeUtils.DEBUG)
            Log.d(HomeUtils.TAG, "HomeActivity onCreate time : " + System.currentTimeMillis());
        super.onCreate(icicle);
        StaticUtil.updateOnlineConfig(this);

        setContentView(R.layout.main);

        final int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
                sIsScreenLarge = screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE
                        || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE;
        StaticUtil.forceShowOptionMenu(this);

		wm = (WindowManager) getSystemService("window");
		mSensorManager =(SensorManager)getSystemService(SENSOR_SERVICE);
		mASensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		if(mASensor == null) {
			Log.d(TAG, "mAsensor is null");
		}
        
		mMSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		
		
        
        mSESceneManager = SESceneManager.getInstance(getApplicationContext());
        mSESceneManager.setGLActivity(this);
        mHandler = new Handler();
        mSESceneManager.setHandler(mHandler);
        mSESceneManager.bindWeatherService();
        mSESceneManager.setDefaultScene();
        mSESceneManager.startVideoThumbnailsService();
        initView();

        mHomeReceiver = new HomeReceiver();
        mADViewController = ADViewController.getInstance();
        mADViewController.initIab();
        mHasLeave3DHome = true;
        mUserLeaveHint = false;
        mFirstLoad = true;
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        isLiving = true;
        
    }

    public boolean isLiving() {
        return isLiving;
    }
    
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        super.onMenuOpened(featureId, menu);
        return mSESceneManager.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return mSESceneManager.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return mSESceneManager.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        return mSESceneManager.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        return mSESceneManager.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle bundle) {
        mSESceneManager.onPrepareDialog(id, dialog, bundle);
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        mUserLeaveHint = true;
    }

    private void initView() {
        boolean hasScene = mSESceneManager.getCurrentScene() != null;
        mWorkSpace = (WorkSpace) findViewById(R.id.workspace);
        mSESceneManager.setWorkSpace(mWorkSpace);
        mRenderView = new SERenderView(this, false);
        mWorkSpace.addView(mRenderView);
        mSESceneManager.setGLSurfaceView(mRenderView);
        mSESceneManager.setGLActivity(this);
        WidgetWorkSpace widgetWorpSpace = new WidgetWorkSpace(this);
        mWorkSpace.addView(widgetWorpSpace);
        mSESceneManager.setWidgetWorkSpace(widgetWorpSpace);
        if (SettingsActivity.getFPSSetting(this)) {
            mSESceneManager.showFPSView();
            mRenderView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        } else {
            mSESceneManager.clearFPSView();
            mRenderView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
        if (hasScene) {
            mSESceneManager.onActivityRestart();
            widgetWorpSpace.updateScroll();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, mASensor, SensorManager.SENSOR_DELAY_NORMAL); 
        
        SettingsActivity.checkOrientationSettings(this);

        if (SettingsActivity.isEnableFullScreen(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(mHomeReceiver, filter);
        mSESceneManager.setHandler(mHandler);
        mSESceneManager.setDefaultScene();
        mSESceneManager.setGLSurfaceView(mRenderView);
        mSESceneManager.setGLActivity(this);
        mSESceneManager.onActivityResume();
        mSESceneManager.stopLocalService();
        if (mHasLeave3DHome) {
            if (mRenderView != null) {
                Log.i(HomeUtils.TAG, "########## surface onResume");
                mRenderView.onResume();
            }
//            if (!mFirstLoad) {
//                mSESceneManager.delay();
//            }
            mHasLeave3DHome = false;
            mFirstLoad = false;
        }
        mADViewController.onResume();
        if (HomeUtils.DEBUG)
            Log.d(HomeUtils.TAG, "HomeActivity onResume time : " + System.currentTimeMillis());
        HomeUtils.stopTracing();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mSensorManager != null) {
        	mSensorManager.unregisterListener(this);
        }

        closeFloatViews();

        if (HomeUtils.DEBUG)
            Log.d(HomeUtils.TAG, "HomeActivity onPause time : " + System.currentTimeMillis());
        try {
            unregisterReceiver(mHomeReceiver);
        } catch (IllegalArgumentException e) {
//            ACRA.getErrorReporter().handleSilentException(new Exception(HomeUtils.ACRA_TAG + "unregister HomeReceiver failed"));
        }
        mSESceneManager.onActivityPause();
        mSESceneManager.startLocalService();
        mHasLeave3DHome = mUserLeaveHint || !isTopActivity() || !mPowerManager.isScreenOn();
        mUserLeaveHint = false;
        if (mHasLeave3DHome) {
            if (mRenderView != null) {
//                mSESceneManager.setNeedcatchBackground(true);
                Log.i(HomeUtils.TAG, "########## surface onPause");
                mRenderView.onPause();

            }
        }
        mADViewController.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (HomeUtils.DEBUG)
            Log.d(HomeUtils.TAG, "HomeActivity onStop time : " + System.currentTimeMillis());
        System.gc();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (HomeUtils.DEBUG)
            Log.d(HomeUtils.TAG, "HomeActivity onStart time : " + System.currentTimeMillis());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isLiving = false;
        mSESceneManager.onActivityDestroy();
        mADViewController.onDestory();

    }

    @Override
    public void onBackPressed() {
        mSESceneManager.handleBackKey();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (HomeUtils.DEBUG)
            Log.d("BORQS", "keyCode = " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            mSESceneManager.handleMenuKey();
        }
        return super.onKeyDown(keyCode, event);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mSESceneManager.onActivityResult(requestCode, resultCode, data);
        mADViewController.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            boolean alreadyOnHome = ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            if (alreadyOnHome) {
                mSESceneManager.onNewIntent(intent);
            }
        }
    }

    public static boolean isScreenLarge() {
        return sIsScreenLarge;
    }

    private boolean isTopActivity() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (topActivity.equals(getComponentName())) {
                return true;
            }
        }
        return false;
    }

    private class HomeReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                if (SettingsActivity.isEnableUnlockScreenAnim(HomeActivity.this)) {
                    for (SESceneManager.UnlockScreenListener callBack : mSESceneManager.mUnlockScreenListeners) {
                        if (callBack != null) {
                            callBack.unlockScreen();
                        }
                    }
                }
            } else if (Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())
                    || Intent.ACTION_TIME_CHANGED.equals(intent.getAction())
                    || Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
                for (SESceneManager.TimeChangeCallBack callBack : mSESceneManager.mTimeCallBacks) {
                    if (callBack != null) {
                        callBack.onTimeChanged();
                    }
                }
            }
        }
    }


    
    
    private SensorManager mSensorManager;
    private static ScreenTraFloatView floatView;
    private boolean show = false;
    private WindowManager wm;
    float[] accelerometerValues = new float[3];  
    float[] magneticFieldValues = new float[3];
    float[] values=new float[3];  
    float[] rotate=new float[9];  
    
    public void closeFloatViews()
    {
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if (null != floatView && show) {
					wm.removeView(floatView);
					floatView = null;
					show = false;
				}
				
			}
		});
    }
    
    //show one dialog, can dismiss from outside
    private void showFloatViews(final float rotateAngle)
    {
    	final Context context = this;
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if(show) {
					if(floatView != null ) {		
						wm.updateViewLayout(floatView, floatView.wmParams);
					}
					return;
				}
				
				floatView = new ScreenTraFloatView(context, rotateAngle);		
				
				LayoutInflater factory = LayoutInflater.from(context);
				//child 1
				View convertView = factory.inflate(R.layout.screen_tra_view, null);	
				floatView.addView(convertView);
				convertView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if(House.IsScreenOrientationPortrait(context)) {
							SettingsActivity.saveOrientationName(HomeActivity.this, SettingsActivity.ORIENTATION_LANDSCAPE);
						}else {
							SettingsActivity.saveOrientationName(HomeActivity.this, SettingsActivity.ORIENTATION_PORTRAIT);
						}
						
						Intent intent = new Intent(HomeActivity.this, BlankActivity.class);
						startActivity(intent);
					}
				});
				
				wm.addView(floatView, floatView.wmParams);
				show = true;
			}
		});
    }

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		
		if (sensorEvent.sensor.getType() != Sensor.TYPE_ORIENTATION) {
			return ;
		}
		
		magneticFieldValues = sensorEvent.values;
		
		float tmpvalues = magneticFieldValues[0];
		if((tmpvalues >= 60 && tmpvalues < 120) || (tmpvalues >=240 && tmpvalues < 300)){  
			if(isScreenLarge()) {
				if(House.IsScreenOrientationPortrait(this)) {
					closeFloatViews();
				}else {
					showFloatViews(tmpvalues);
				}
			}else {
				if(House.IsScreenOrientationPortrait(this)) {
					showFloatViews(tmpvalues);
				}else {
					closeFloatViews();
				}
			}
		} else if((tmpvalues >= 0 && tmpvalues < 30) || (tmpvalues >= 150 && tmpvalues < 210) || (tmpvalues >= 330 && tmpvalues <= 360)){  
			if(isScreenLarge()) {
				if(House.IsScreenOrientationPortrait(this)) {
					showFloatViews(tmpvalues);
				}else {
					closeFloatViews();
				}
			}else {
				if(House.IsScreenOrientationPortrait(this)) {
					closeFloatViews();
				}else {
					showFloatViews(tmpvalues);
				}
			}
		}else {
			closeFloatViews();
		}
	}

}
