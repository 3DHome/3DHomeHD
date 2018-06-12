package com.borqs.se.widget3d;

import java.util.ArrayList;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERay;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.home3d.ApplicationMenu;
import com.borqs.se.home3d.HomeScene;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectGroup;
import com.borqs.se.engine.SESceneManager;

public class WallDialog extends SEObjectGroup {

    private SEObject mDockObj;
    private SEObject mAppObj;
    private SEObject mWallPaperObj;
    private SEObject mWidgetObj;

    private boolean mDisableTouch;
    private ShowAnimation mShowAnimation;
    private HideAnimation mHideAnimation;

    public WallDialog(SEScene scene, String name) {
        super(scene, name, 0);
        mDisableTouch = true;
        setIsEntirety_JNI(false);
        setOnClickListener(new OnTouchListener() {
            @Override
            public void run(SEObject obj) {
                hide(false, null);
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mDisableTouch) {
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    public void show(Bundle wallpaperMsg, int touchX , int touchY) {
        if (!getScene().getStatus(SEScene.STATUS_ON_WALL_DIALOG)) {
            mDisableTouch = true;
            getParent().setUseUserColor(0.3f, 0.3f, 0.3f);
            stopAllAnimation(null);
            init(wallpaperMsg);
            getScene().setStatus(SEScene.STATUS_ON_WALL_DIALOG, true);
            getScene().setTouchDelegate(this);
            mShowAnimation = new ShowAnimation(getScene(), touchX, touchY);
            mShowAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    mDisableTouch = false;
                }
            });
            mShowAnimation.execute();
        }
    }

    private void init(final Bundle wallpaperMsg) {

        int index = 1;
        if (mDockObj == null) {
            mDockObj = new SEObject(getScene(), "walldialogdock", index);
            ModelInfo dockModelInfo = getScene().mSceneInfo.findModelInfo("walldialogdock");
            dockModelInfo.cloneMenuItemInstance(this, index, false, dockModelInfo.mStatus);
            addChild(mDockObj, false);
            mDockObj.setClickable(false);

            mDockObj.getUserTransParas().mScale = new SEVector3f(4, 4, 4);
            mDockObj.getUserTransParas().mTranslate = new SEVector3f(0, 20, -10);
            mDockObj.setUserTransParas();
        }
        if (mAppObj == null) {
            mAppObj = new SEObject(getScene(), "walldialogapp", index);
            ModelInfo appModelInfo = getScene().mSceneInfo.findModelInfo("walldialogapp");
            appModelInfo.cloneMenuItemInstance(this, index, false, appModelInfo.mStatus);
            addChild(mAppObj, false);
            mAppObj.setIsEntirety_JNI(true);
            mAppObj.setOnClickListener(new OnTouchListener() {
                @Override
                public void run(SEObject obj) {
                    selectApp();
                    hide(true, null);
                }
            });

            mAppObj.getUserTransParas().mTranslate = new SEVector3f(-120, 0, 0);
            mAppObj.getUserTransParas().mScale = new SEVector3f(4, 4, 4);
            mAppObj.setUserTransParas();
        }
        if (mWallPaperObj == null) {
            mWallPaperObj = new SEObject(getScene(), "walldialogwallpaper", index);
            ModelInfo picModelInfo = getScene().mSceneInfo.findModelInfo("walldialogwallpaper");
            picModelInfo.cloneMenuItemInstance(this, index, false, picModelInfo.mStatus);
            addChild(mWallPaperObj, false);
            mWallPaperObj.setIsEntirety_JNI(true);
            mWallPaperObj.setOnClickListener(new OnTouchListener() {
                @Override
                public void run(SEObject obj) {
                    selectWallpaper(wallpaperMsg);
                    hide(true, null);
                }
            });

            mWallPaperObj.getUserTransParas().mTranslate = new SEVector3f(120, 0, 0);
            mWallPaperObj.getUserTransParas().mScale = new SEVector3f(4, 4, 4);
            mWallPaperObj.setUserTransParas();
        }
        
        if (mWidgetObj == null) {
            mWidgetObj = new SEObject(getScene(), "walldialogwidget", index);
            ModelInfo widgetModelInfo = getScene().mSceneInfo.findModelInfo("walldialogwidget");
            widgetModelInfo.cloneMenuItemInstance(this, index, false, widgetModelInfo.mStatus);
            addChild(mWidgetObj, false);
            mWidgetObj.setIsEntirety_JNI(true);
            mWidgetObj.setOnClickListener(new OnTouchListener() {
                @Override
                public void run(SEObject obj) {
                    selectWidget();
                    hide(true, null);
                }
            });

            mWidgetObj.getUserTransParas().mTranslate = new SEVector3f(0, 0, 0);
            mWidgetObj.getUserTransParas().mScale = new SEVector3f(4, 4, 4);
            mWidgetObj.setUserTransParas();
        }
    }

    public void hide(boolean fast, final SEAnimFinishListener l) {
        if (getScene().getStatus(SEScene.STATUS_ON_WALL_DIALOG)) {
            stopAllAnimation(null);
            mDisableTouch = true;
            getScene().removeTouchDelegate();
            if (fast) {
                setVisible(false, true);
                getParent().clearUserColor();
                getScene().setStatus(SEScene.STATUS_ON_WALL_DIALOG, false);
                releaseChilds();
                if (l != null) {
                    l.onAnimationfinish();
                }
            } else {
                mHideAnimation = new HideAnimation(getScene());
                mHideAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                    public void onAnimationfinish() {

                        setVisible(false, true);
                        getParent().clearUserColor();
                        getScene().setStatus(SEScene.STATUS_ON_WALL_DIALOG, false);
                        releaseChilds();
                        if (l != null) {
                            l.onAnimationfinish();
                        }
                    }
                });
                mHideAnimation.execute();
            }
        }
    }

    public void releaseChilds() {
        removeAllChild(true);
        mDockObj = null;
        mAppObj = null;
        mWallPaperObj = null;
        mWidgetObj = null;
    }

    @Override
    public boolean handleBackKey(SEAnimFinishListener l) {
        if (getScene().getStatus(SEScene.STATUS_ON_WALL_DIALOG)) {
            hide(false, l);
            return true;
        }
        return false;
    }

    @Override
    public void stopAllAnimation(SEAnimFinishListener l) {
        if (mShowAnimation != null) {
            mShowAnimation.stop();
        }
        if (mHideAnimation != null) {
            mHideAnimation.stop();
        }
    }

    private class ShowAnimation extends CountAnimation {
        
        private int mTouchX;
        private int mTouchY;
        public ShowAnimation(SEScene scene, int touchX, int touchY) {
            super(scene);
            mTouchX = touchX;
            mTouchY = touchY;
        }

        @Override
        public void runPatch(int count) {
            getUserTransParas().mScale = new SEVector3f(count * 0.1f, count * 0.1f, count * 0.1f);
            setUserTransParas();
        }

        @Override
        public void onFirstly(int count) {
            getUserTransParas().mTranslate = new SEVector3f(0, 0, getPositionZ(mTouchX,mTouchY)+40);
            getUserTransParas().mRotate = new SERotate(10, 1, 0, 0);
            getUserTransParas().mScale = new SEVector3f(0.1f, 0.1f, 0.1f);
            setUserTransParas();
            setVisible(true, true);
        }

        @Override
        public int getAnimationCount() {
            return 10;
        }
    }

    private float getPositionZ(int touchX, int touchY) {
        SERay ray = getScene().getCamera().screenCoordinateToRay(touchX, touchY);
        float y = 0;
        float para = (y - ray.getLocation().getY()) / ray.getDirection().getY();
        float desZ = ray.getLocation().getZ() + para * ray.getDirection().getZ();
        return desZ;
    }

    private class HideAnimation extends CountAnimation {

        public HideAnimation(SEScene scene) {
            super(scene);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void runPatch(int count) {
            getUserTransParas().mScale = new SEVector3f(1f/count, 1f/count, 1f/count);
            setUserTransParas();
        }

        @Override
        public void onFirstly(int count) {
            getUserTransParas().mScale = new SEVector3f(1f, 1f, 1f);
            setUserTransParas();
        }

        @Override
        public int getAnimationCount() {
            return 10;
        }
    }


    private void selectWidget() {
        int id = SESceneManager.getInstance().getAppWidgetHost().allocateAppWidgetId();
        Intent selectIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        selectIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<AppWidgetProviderInfo>();
        selectIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
        ArrayList<Bundle> customExtras = new ArrayList<Bundle>();
        selectIntent
                .putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
        SESceneManager.getInstance().startActivityForResult(selectIntent,
                HomeScene.REQUEST_CODE_BIND_WIDGET);
    }

    private void selectApp() {
        getScene().handleMessage(HomeScene.MSG_TYPE_SHOW_APP_VIEW, ApplicationMenu.TYPE_SHOW_APP);
    }

    private void selectWallpaper(Bundle wallpaperMsg) {
        getScene().handleMessage(HomeScene.MSG_TYPE_SHOW_SELECT_WALLPAPER_DIALOG, wallpaperMsg);
    }
}
