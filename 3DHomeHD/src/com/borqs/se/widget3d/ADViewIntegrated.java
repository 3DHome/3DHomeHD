package com.borqs.se.widget3d;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.LinearLayout;

import com.borqs.freehdhome.R;
import com.borqs.se.engine.SECommand;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SESceneManager;

public class ADViewIntegrated extends LinearLayout {
    private static final int MSG_UPDATE_IMAGE = 0;
    private static final int MSG_RELEASE_IMAGE = 1;
    private AdListener mAdListener;
    private int mImageW, mImageH;
    private Handler mHandler;
    private Bitmap mMyBitmap;
    private Canvas mMyCanvas;
    private String mImageKey;
    private boolean mIsEnableCatchImage = false;
    private Bitmap mCrossBitmap;

    public ADViewIntegrated(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    public ADViewIntegrated(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public ADViewIntegrated(Context context) {
        super(context);
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case MSG_UPDATE_IMAGE:
                    mUpdateBitmap.run();
                    break;
                case MSG_RELEASE_IMAGE:
                    releaseBitmap();
                    break;
                }
            }

        };
        BitmapDrawable bitmapDrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.remove_ad);
        mCrossBitmap = bitmapDrawable.getBitmap();
    }

    public void setAdListener(AdListener l) {
        mAdListener = l;
    }

    public void onFailedToReceiveAd(int index, String arg0) {
        if (mAdListener != null) {
            mAdListener.onFailedToReceiveAd(index, arg0);
        }
    }

    public void onReceiveAd(int index) {
        if (mAdListener != null) {
            mAdListener.onReceiveAd(index);
        }
    }

    public View showChild(int index) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (i == index) {
                getChildAt(i).setVisibility(View.VISIBLE);
            } else {
                getChildAt(i).setVisibility(View.INVISIBLE);
            }
        }
        requestLayout();
        return getChildAt(index);

    }
    
    public void loadAD(int index) {
        if (mAdListener != null) {
            mAdListener.onLoadADS(index);
        }
    }
    
    public void stopLoadAD(int index) {
        if (mAdListener != null) {
            mAdListener.onStopLoadAD(index);
        }
    }

    public interface AdListener {
        public void onFailedToReceiveAd(int index, String arg0);

        public void onReceiveAd(int index);
        
        public void onLoadADS(int index);
        
        public void onStopLoadAD(int index);
    }
    
    @Override
    public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
        if (mIsEnableCatchImage) {
            mHandler.removeMessages(MSG_UPDATE_IMAGE);
            mHandler.sendEmptyMessage(MSG_UPDATE_IMAGE);
        }
        return super.invalidateChildInParent(location, dirty);
    }

    public void requestCatchImage(long delay) {
        mIsEnableCatchImage = true;
        mHandler.removeMessages(MSG_UPDATE_IMAGE);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_IMAGE, delay);
    }
    
    public void stopCatchImage() {
        mIsEnableCatchImage = false;
        mHandler.removeMessages(MSG_UPDATE_IMAGE);
        mHandler.sendEmptyMessage(MSG_RELEASE_IMAGE);
    }

    public void setImageKey(String imageKey) {
        mImageKey = imageKey;
    }

    public void setImageSize(int w, int h) {
        mImageW = w;
        mImageH = h;
    }

    private Canvas tryToGetCanvas() {
        if (mMyCanvas != null && !mMyBitmap.isRecycled()) {
            return mMyCanvas;
        }
        int newW = HomeUtils.higherPower2(mImageW);
        int newH = HomeUtils.higherPower2(mImageH);
        try {
            mMyBitmap = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888);
        } catch (Exception e) {
            mMyBitmap = null;
        }
        if (mMyBitmap != null) {
            mMyCanvas = new Canvas(mMyBitmap);
            mMyCanvas.translate((newW - mImageW) * 0.5f, (newH - mImageH) * 0.5f);
            return mMyCanvas;
        }
        return null;
    }

    private int mImageData = 0;
    private Object mLock = new Object();
    
    private Runnable mUpdateBitmap = new Runnable() {
        public void run() {
            if (getChildAt(0) == null) {
                return;
            }
            Canvas canvas = tryToGetCanvas();
            if (canvas == null) {
                return;
            }
            getChildAt(0).setDrawingCacheEnabled(true);
            Bitmap catchBitmap = getChildAt(0).getDrawingCache();
            mMyBitmap.eraseColor(0);
            if (catchBitmap != null && !catchBitmap.isRecycled()) {
                Rect src = new Rect(0, 0, catchBitmap.getWidth(), catchBitmap.getHeight());
                Rect des =  new Rect(0, 0, mImageW, mImageH);
                canvas.drawBitmap(catchBitmap, src, des, null);
                getChildAt(0).setDrawingCacheEnabled(false);
                // if engine has not used the previous image, release the previous image
                synchronized (mLock) {
                    if (mImageData != 0) {
                        SEObject.releaseImageData_JNI(mImageData);
                        mImageData = 0;
                    }
                }
                if (mCrossBitmap != null && !getContext().getString(R.string.channel_amazon).equalsIgnoreCase(SettingsActivity.getChannelCode(getContext())) ){
                    canvas.drawBitmap(mCrossBitmap, 0, 0, null);
                }
                int imageData = SEObject.loadImageData_JNI(mMyBitmap);
                synchronized (mLock) {
                    mImageData = imageData;
                }
                new SECommand(SESceneManager.getInstance().getCurrentScene()) {
                    public void run() {
                        synchronized (mLock) {
                            SEObject.addImageData_JNI(mImageKey, mImageData);
                            mImageData = 0;
                        }
                    }
                }.execute();
            }
        }
    };
    
    private Runnable mOpenADTask = new Runnable() {
        public void run() {
            if ( getChildAt(0) == null) {
                return;
            }
            try {
                float pointX =  getChildAt(0).getWidth() - 50;
                float pointY =  getChildAt(0).getHeight() / 2;
                long now = SystemClock.uptimeMillis();
                MotionEvent eventDowne = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, pointX, pointY, 0);
                getChildAt(0).dispatchTouchEvent(eventDowne);
                MotionEvent eventUp = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, pointX, pointY, 0);
                getChildAt(0).dispatchTouchEvent(eventUp);
            } catch (Exception e) {
            }
        }
    };

    public void doClick() {
        mHandler.removeCallbacks(mOpenADTask);
        mHandler.post(mOpenADTask);
    }

    private void releaseBitmap() {
        if (mMyBitmap != null && !mMyBitmap.isRecycled()) {
            mMyBitmap.recycle();
        }
    }

    public void releaseCatchBitmap() {
        if (mCrossBitmap != null && !mCrossBitmap.isRecycled()) {
            mCrossBitmap.recycle();
        }
    }

}
