package com.borqs.se.shortcut;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.borqs.se.engine.SELoadResThread;
import com.borqs.freehdhome.R;
import com.borqs.se.engine.SECommand;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.widget3d.WidgetObject;

public class HomeWidgetHostView extends AppWidgetHostView {
    private LayoutInflater mInflater;
    private boolean mHasPerformedLongPress;
    private CheckForLongPress mPendingCheckForLongPress;
    private int mStartX;
    private int mStartY;
    private int mTouchSlop;
    private WidgetObject mWidgetObject;
    private Handler mHandler;

    public HomeWidgetHostView(Context context) {
        super(context);
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mHandler = new Handler();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Consume any touch events for ourselves after longpress is triggered
        if (mHasPerformedLongPress) {
            mHasPerformedLongPress = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN: {
            mStartX = (int) ev.getX();
            mStartY = (int) ev.getY();
            postCheckForLongClick();
            break;
        }
        case MotionEvent.ACTION_MOVE:
            int x = (int) ev.getX();
            int y = (int) ev.getY();
            int slop = mTouchSlop;
            if (Math.pow((x - mStartX), 2) > slop && Math.pow((y - mStartY), 2) > slop) {
                mHasPerformedLongPress = false;
                if (mPendingCheckForLongPress != null) {
                    removeCallbacks(mPendingCheckForLongPress);
                }
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            mHasPerformedLongPress = false;
            if (mPendingCheckForLongPress != null) {
                removeCallbacks(mPendingCheckForLongPress);
            }
            break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void postCheckForLongClick() {
        mHasPerformedLongPress = false;

        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mPendingCheckForLongPress.rememberWindowAttachCount();
        postDelayed(mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mHasPerformedLongPress = false;
        if (mPendingCheckForLongPress != null) {
            removeCallbacks(mPendingCheckForLongPress);
        }
    }

    class CheckForLongPress implements Runnable {
        private int mOriginalWindowAttachCount;

        public void run() {
            if ((getParent() != null) && hasWindowFocus() && mOriginalWindowAttachCount == getWindowAttachCount()
                    && !mHasPerformedLongPress) {
                if (performLongClick()) {
                    mHasPerformedLongPress = true;
                }
            }
        }

        public void rememberWindowAttachCount() {
            mOriginalWindowAttachCount = getWindowAttachCount();
        }
    }

    public void setWidgetObject(WidgetObject obj) {
        mWidgetObject = obj;
    }

    public WidgetObject getWidgetObject() {
        return mWidgetObject;
    }

    public void updateBitmap() {
        mHandler.removeCallbacks(mUpdateBitmap);
        mHandler.postDelayed(mUpdateBitmap, 100);
    }

    private Runnable mUpdateBitmap = new Runnable() {
        public void run() {
            boolean willNotCache = willNotCacheDrawing();
            setWillNotCacheDrawing(false);
            try {
                buildDrawingCache();
            } catch (Exception e) {
                e.printStackTrace();
                setWillNotCacheDrawing(willNotCache);
                destroyDrawingCache();
                return;
            }
            setWillNotCacheDrawing(willNotCache);
            Bitmap catchBitmap = getDrawingCache();
            if (catchBitmap == null) {
                destroyDrawingCache();
                return;
            }
            int bitmapW = catchBitmap.getWidth();
            int bitmapH = catchBitmap.getHeight();
            float scale = 1;
            if (bitmapW >= bitmapH && bitmapW > 512) {
                scale = (float) 512 / bitmapW;
                bitmapH = (int) (bitmapH * scale);
                bitmapW = 512;

            } else if (bitmapH > bitmapW && bitmapH > 512) {
                scale = (float) 512 / bitmapH;
                bitmapW = (int) (bitmapW * scale);
                bitmapH = 512;
            }

            int newW = HomeUtils.higherPower2(bitmapW);
            int newH = HomeUtils.higherPower2(bitmapH);

            final Bitmap viewBitmap = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(viewBitmap);
            canvas.translate((newW - bitmapW) * 0.5f, (newH - bitmapH) * 0.5f);
            Rect src = new Rect(0, 0, catchBitmap.getWidth(), catchBitmap.getHeight());
            Rect des = new Rect(0, 0, bitmapW, bitmapH);
            canvas.drawBitmap(catchBitmap, src, des, null);
            destroyDrawingCache();
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    final int imageData = SEObject.loadImageData_JNI(viewBitmap);
                    new SECommand(SESceneManager.getInstance().getCurrentScene()) {
                        public void run() {
                            String imageKey = mWidgetObject.getName() + "_imageKey";
                            SEObject.addImageData_JNI(imageKey, imageData);
                            viewBitmap.recycle();
                        }
                    }.execute();
                }
            });
        }
    };

    @Override
    protected View getErrorView() {
        return mInflater.inflate(R.layout.appwidget_error, this, false);
    }

    @Override
    public int getDescendantFocusability() {
        return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
    }

}
