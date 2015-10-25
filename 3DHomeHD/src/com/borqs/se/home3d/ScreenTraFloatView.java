package com.borqs.se.home3d;

import com.borqs.freehdhome.R;
import com.borqs.se.home3d.HomeActivity;
import com.borqs.se.widget3d.House;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class ScreenTraFloatView extends FrameLayout {
	private WindowManager wm;
	
	private float mStartX;
	private float mStartY;
	private float x;
	private float y;
	private Context mContext;
	
	public ScreenTraFloatView(Context context, float rotAngle) {
		super(context);
		mContext = context;
		wm = (WindowManager)getContext().getApplicationContext().getSystemService("window");		
		
		configParams(rotAngle);
	}
	
	private void configParams(float rotAngle) {
		wmParams.width = mContext.getResources().getDimensionPixelSize(R.dimen.screen_orientation_view_width);
		wmParams.format = PixelFormat.TRANSPARENT;
		
		wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		wmParams.gravity = Gravity.LEFT | Gravity.TOP;
		if(House.IsScreenOrientationPortrait(mContext)) {
			if(HomeActivity.isScreenLarge()) {
				if(rotAngle >= 0 && rotAngle < 30) {
					wmParams.gravity = Gravity.RIGHT | Gravity.TOP;
				}else if(rotAngle >= 150 && rotAngle < 210) {
					wmParams.gravity = Gravity.LEFT | Gravity.TOP;
				}
			}else {
				if(rotAngle >= 60 && rotAngle < 120) {
					wmParams.gravity = Gravity.LEFT | Gravity.TOP;
				}else if(rotAngle >=240 && rotAngle < 300) {
					wmParams.gravity = Gravity.RIGHT | Gravity.TOP;
				}
			}
		}else {
			if(HomeActivity.isScreenLarge()) {
				if(rotAngle >= 60 && rotAngle < 120) {
					wmParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
				}else if(rotAngle >=240 && rotAngle < 300) {
					wmParams.gravity = Gravity.RIGHT | Gravity.TOP;
				}
			}else {
				if(rotAngle >= 0 && rotAngle < 30) {
					wmParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
				}else if(rotAngle >= 150 && rotAngle < 210) {
					wmParams.gravity = Gravity.RIGHT | Gravity.TOP;
				}
			}
		}
				
		wmParams.x = 10;
//		wmParams.y = 100;		
		wmParams.height = wmParams.width;
	}
	
	public WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		x = event.getRawX();
		y = event.getRawY();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mStartX = event.getX();
			mStartY = event.getY();
			//DialerSync.changeBtnVisibility();
			break;
		case MotionEvent.ACTION_MOVE:
			updateViewPosition();
			break;
		case MotionEvent.ACTION_UP:
			updateViewPosition();
//			setLocation();
			mStartX = mStartY = 0;
			break;
		default:
			break;
		}
		return true;
	}
	
	private void updateViewPosition(){
		wmParams.x = (int)(x - mStartX );
		wmParams.y = (int)(y - mStartY - 38);
		//if(null == DialerSync.floatView )
		//	return;
		wm.updateViewLayout(this, wmParams);
	}
	
	private void setLocation() {
//		SharedPreferences share=  mContext.getSharedPreferences("location", 0);
//		Editor editor = share.edit();
//		editor.putInt("x", wmParams.x).commit();
//		editor.putInt("y", wmParams.y).commit();
	}
}
