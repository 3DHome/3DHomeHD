package com.borqs.se.home3d;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.borqs.freehdhome.R;

public class LongPressWallDialog extends Dialog {

    private android.view.View.OnClickListener mOnItemClickListener;
    
    private View mRootView;
    private Context mContext;
    public LongPressWallDialog(Context context, android.view.View.OnClickListener listener) {
        super(context, R.style.dialog);
        mContext = context;
        mOnItemClickListener = listener;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	setContentView(R.layout.wall_long_click_view);
    	mRootView = findViewById(R.id.wall_dialog_ll);
    	if(SettingsActivity.getPreferRotation(mContext) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
    		mRootView.setBackgroundResource(R.drawable.home_option_menu_bg);
    	}else {
    		mRootView.setBackgroundResource(R.drawable.home_option_menu_bg_port);
    	}
    	View option1 = findViewById(R.id.option_menu1);
    	option1.setOnClickListener(mOnItemClickListener);
    	
    	View option2 = findViewById(R.id.option_menu2);
    	option2.setOnClickListener(mOnItemClickListener);
    	
    	View option3 = findViewById(R.id.option_menu3);
    	option3.setOnClickListener(mOnItemClickListener);
    	
    	View option4 = findViewById(R.id.option_menu4);
    	option4.setOnClickListener(mOnItemClickListener);
    	
    	View option5 = findViewById(R.id.option_menu5);
    	option5.setOnClickListener(mOnItemClickListener);
    	mRootView.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
		            cancel();
		            return true;
		        }
		        return false;
			}
		});
    } 

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	super.onWindowFocusChanged(hasFocus);
    	DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
    	Log.d("LongPressWallDialog: ", "dm.density: " + dm.density) ;
    	int screenWidth  = dm.widthPixels;  
    	int screenHeight = dm.heightPixels; 
    	mRootView.setLayoutParams(new FrameLayout.LayoutParams(screenWidth, screenHeight));
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);        
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            cancel();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {       
        cancel();
    }   
}
