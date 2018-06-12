package com.borqs.se.shortcut;

import java.util.ArrayList;
import java.util.List;

import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SEVector2f;
import com.borqs.se.engine.SEVector.SEVector2i;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SECommand;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SECamera.CameraChangedListener;
import com.borqs.se.engine.SESceneInfo;
import com.borqs.se.engine.SESceneManager;
import com.borqs.framework3d.home3d.HouseObject.WallRadiusChangedListener;
import com.borqs.se.widget3d.ObjectInfo;
import com.borqs.se.widget3d.WidgetObject;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.TextView;

public class WidgetWorkSpace extends FrameLayout implements CameraChangedListener, WallRadiusChangedListener {
	private static final String TAG = "WidgetWorkSpace";
    private static final int MSG_UPDATE_ALL_WIDGET = 0;
    private List<WidgetObject> mWidgets;
    private OnLongClickListener mOnLongClickListener;
    private boolean mOnDrag;
    private int mStartX;
    private int mStartY;
    private boolean mFirst;
    private int mTouchSlop;    
    private int mFaceIndex = 0;
    private boolean mScrollXHasChanged = false;
    private boolean mCameraHasChanged = false;
    private SECommand mSceneStatusDetector;
    private int mCheckStatus;
    
    private Handler mMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_UPDATE_ALL_WIDGET:
                if (msg.arg1 == View.VISIBLE) {
                    if (mCameraHasChanged) {
                        updateWidgetSize(true);
                        mCameraHasChanged = false;
                    } else {
                        updateWidgetSize(false);
                    }

                    if (mScrollXHasChanged) {
                        mScrollXHasChanged = false;
                        updateScroll();
                    }
                }
                setVisibility(msg.arg1);
                break;             
            }
        }
    };

    public void updateScroll() {
    	
        int scrollX = -mFaceIndex * getCamera().getWidth();
        setScrollX(scrollX);
        
    }
    
    public void requestUpdateWidget(final WidgetObject widget) {
        mMsgHandler.post(new Runnable() {
            public void run() {
                updateWidgetSize(widget);                
            }
            
        });
    }

    private void updateWidgetSize(boolean all) {
        int size = getChildCount();
        for (int i = 0; i < size; i++) {
        	View v = getChildAt(i);
        	if(v instanceof HomeWidgetHostView) {
	            HomeWidgetHostView view = (HomeWidgetHostView) v;
	            WidgetObject widget = view.getWidgetObject();
	            ObjectInfo info = widget.getObjectInfo();
	            ObjectSlot slot = info.mObjectSlot;
	            int wallIndex = slot.mSlotIndex;
	            if (all || wallIndex == mFaceIndex) {
	                updateWidgetSize(widget);
	            }
        	}
        }
    }
    private TextView bgView = null;
    private void updateWidgetSize(WidgetObject widget) {
        //float cameraHeight = getCamera().getLocation().getZ();
        float radius = getScene().mSceneInfo.mHouseSceneInfo.getHouseRadius();
        float ratio = getWallToScreenRatio(radius);
        ObjectInfo info = widget.getObjectInfo();
        ObjectSlot slot = info.mObjectSlot;
        AppWidgetHostView hostView = widget.getWidgetHostView();
        //SEObjectBoundaryPoint bp = widget.getBoundaryPoint();
        SEVector3f worldCenter = widget.toWorldPoint(new SEVector3f(0, 0, 0));
        SEVector2i screenPoint = getCamera().worldToScreenCoordinate(worldCenter);
        //SEVector2f slotToWall = slotToWall(slot.mStartX, slot.mStartY, cameraHeight);
        
        //SEVector2i wallToScreen = wallToScreen(slotToWall, ratio, getCamera().getWidth(), getCamera().getHeight());
        int left =  screenPoint.getX(); //wallToScreen.getX();
        int top =  screenPoint.getY();//wallToScreen.getY();
        /*
        if(bgView == null) {
        	bgView = new TextView(SESceneManager.getInstance().getContext());
        	FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(100, 100);
        	bgView.setLayoutParams(lp);
        	bgView.setText("ttttttt");
        	bgView.setBackgroundColor(0xFF00FFFF);
        	bgView.setTranslationX(left);
        	bgView.setTranslationY(top);
        	this.addView(bgView);
        }
        */
        float delta = slot.mSlotIndex * getCamera().getWidth();
        int hostViewWidth = hostView.getWidth();
        int hostViewHeight = hostView.getHeight();
        //hostView.setPivotX(0);
        //hostView.setPivotY(0);
        
        //hostView.setScaleX(1);
        //hostView.setScaleY(1);
        hostView.setScaleX(ratio / widget.mScale);
        hostView.setScaleY(ratio / widget.mScale);
        hostView.setTranslationX(left - hostViewWidth / 2 - delta);
        hostView.setTranslationY(top - hostViewHeight / 2);
        hostView.requestLayout();
        //hostView.setBackground(new ColorDrawable(0xFFFFFFFF));
        int viewLeft = hostView.getLeft();
        int viewTop = hostView.getTop();
        int spaceWidth = this.getWidth();
        int spaceHeight = this.getHeight();
        int spaceLeft = this.getLeft();
        int spaceTop = this.getTop();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)hostView.getLayoutParams();
        
        Log.i(TAG, "### host view left = " + viewLeft + ", view top = " + viewTop);
        /*
        int width = hostView.getWidth();
        int height = hostView.getHeight();
        int viewLeft = hostView.getLeft();
        int viewTop = hostView.getTop();
        hostView.setPivotX(0);
        hostView.setPivotY(0);
        hostView.setTranslationX(left);
        hostView.setTranslationY(top);
        hostView.setScaleX(1);
        hostView.setScaleY(1);
        //hostView.layout(left, top, left + width, top + height);
        hostView.requestLayout();
        */
        widget.mPreSlot = slot.clone();
    }

    public WidgetWorkSpace(Context context) {
        super(context);
        mOnDrag = false;
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        //LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        setLayoutParams(params);
        mWidgets = new ArrayList<WidgetObject>();
        mCheckStatus = SEScene.STATUS_APP_MENU + SEScene.STATUS_DISALLOW_TOUCH + SEScene.STATUS_HELPER_MENU
                + SEScene.STATUS_MOVE_OBJECT + SEScene.STATUS_OBJ_MENU
                + SEScene.STATUS_ON_DESK_SIGHT + SEScene.STATUS_ON_SKY_SIGHT + SEScene.STATUS_ON_WIDGET_SIGHT
                + SEScene.STATUS_OPTION_MENU + SEScene.STATUS_ON_WALL_DIALOG;
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        mOnLongClickListener = l;
        int size = getChildCount();
        for (int i = 0; i < size; i++) {
            HomeWidgetHostView view = (HomeWidgetHostView) getChildAt(i);
            view.setOnLongClickListener(mOnLongClickListener);
        }
    }

    public void startDrag() {
        mOnDrag = true;
    }

    public int getTouchX() {
        return mStartX;
    }

    public int getTouchY() {
        return mStartY;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mStartX = (int) ev.getX();
            mStartY = (int) ev.getY();
            mFirst = true;
            break;

        case MotionEvent.ACTION_MOVE:
            int x = (int) ev.getX();
            int y = (int) ev.getY();
            if (mFirst) {
                int stepX = Math.abs(x - mStartX);
                int stepY = Math.abs(y - mStartY);
                int slop = mTouchSlop;
                if (Math.pow(stepX, 2) + Math.pow(stepY, 2) > Math.pow(slop, 2)) {
                    mFirst = false;
                    if (stepY < stepX) {
                        MotionEvent newEvent = MotionEvent.obtain(ev);
                        newEvent.setAction(MotionEvent.ACTION_DOWN);
                        SESceneManager.getInstance().dispatchTouchEvent(newEvent);
                        return true;
                    }
                }
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            if (mOnDrag) {
                SESceneManager.getInstance().dispatchTouchEvent(ev);
            }
        }
        if (mOnDrag) {
            mOnDrag = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return SESceneManager.getInstance().dispatchTouchEvent(event);
    }

    public void clearAll() {
        synchronized (mWidgets) {
            mWidgets.clear();
        }
        if (mSceneStatusDetector != null) {
            mSceneStatusDetector.setIsLazy(false);
            mSceneStatusDetector = null;
        }
        removeAllViews();
    }

    public boolean bindObject(WidgetObject widget) {
        if (mSceneStatusDetector == null) {
            mSceneStatusDetector = new SECommand(getScene()) {
                public void run() {
                    update();
                }
            };
            mSceneStatusDetector.setIsLazy(true);
//            mSceneStatusDetector.execute();
        }
        try {
            HomeWidgetHostView hostView = widget.getWidgetHostView();
            hostView.setOnLongClickListener(mOnLongClickListener);
            addView(hostView);
            updateWidgetSize(widget);
        } catch (Exception e) {
            return false;
        } finally {
            mSceneStatusDetector.execute();
        }

        synchronized (mWidgets) {
            mWidgets.add(widget);
        }
        return true;
    }

    public void unbindObject(WidgetObject widget) {
        AppWidgetHostView hostView = widget.getWidgetHostView();
        if (hostView == null) {
            return;
        }
        synchronized (mWidgets) {
            mWidgets.remove(widget);
        }
        removeView(hostView);
    }

    private SEVector2f slotToWall(int slotX, int slotY, float cameraHeight) {
        SESceneInfo sceneInfo = getScene().mSceneInfo;
        return sceneInfo.mHouseSceneInfo.slotToWall(slotX, slotY, cameraHeight);
    }

    public SEVector2i wallToScreen(SEVector2f slotToWall, float ratio, float screenW, float screenH) {
        slotToWall.selfMul(ratio);
        int x = (int) (slotToWall.getX() + screenW / 2);
        int y = (int) (screenH / 2 - slotToWall.getY());
        return new SEVector2i(x, y);
    }

    private float getWallToScreenRatio(float radius) {
        float cameraDistance = -getCamera().getLocation().getY() + radius;
        float screenWidth = getCamera().getWidth();
        float screenFov = getCamera().getFov();
        float screenDistance = (float) (screenWidth / (2 * Math.tan(screenFov * Math.PI / 360)));
        float paras = screenDistance / cameraDistance;
        return paras;
    }

    private boolean mEnable = true;

    private void enable(boolean enable, boolean force) {
        if (enable == mEnable && !force) {
            return;
        }
        mEnable = enable;
        synchronized (mWidgets) {
            if (enable) {
                for (WidgetObject widget : mWidgets) {
                    ObjectInfo info = widget.getObjectInfo();
                    ObjectSlot slot = info.mObjectSlot;
                    if (slot.mSlotIndex == mFaceIndex) {
                        if (widget.isVisible()) {
                            if (!"none".equals(SettingsActivity.getAppIconBackgroundName(getContext()))) {
                                widget.setVisible(true, true);
                            } else {
                                widget.setVisible(false, true);
                            }
                        }
                    } else {
//                        if (!widget.isVisible()) {
                            widget.setVisible(true, true);
//                        }
                    }
                }
            } else {
                for (WidgetObject widget : mWidgets) {
//                    if (!widget.isVisible()) {
                        widget.setVisible(true, true);
//                    }
                }
            }
        }
        mMsgHandler.removeMessages(MSG_UPDATE_ALL_WIDGET);
        Message msg = mMsgHandler.obtainMessage(MSG_UPDATE_ALL_WIDGET);
        if (enable) {
            msg.arg1 = View.VISIBLE;
            mMsgHandler.sendMessage(msg);
        } else {
            msg.arg1 = View.GONE;
            mMsgHandler.sendMessageDelayed(msg, 50);
        }
    }

    private void update() {
        if ((getScene().getStatus() & mCheckStatus) > 0 || mFaceIndex < 0) {
            enable(false, false);
        } else {
            if (mCameraHasChanged) {
                enable(true, true);
            } else {
                enable(true, false);
            }
        }
    }

    public void onWallRadiusChanged(int faceIndex) {
        mFaceIndex = faceIndex;
        if (mFaceIndex >= 0) {
            mScrollXHasChanged = true;
        }
    }

    public void onCameraChanged() {
        mCameraHasChanged = true;
    }

    private SECamera getCamera() {
        return SESceneManager.getInstance().getCurrentScreenCamera();
    }

    private SEScene getScene() {
        return SESceneManager.getInstance().getCurrentScene();
    }

}
