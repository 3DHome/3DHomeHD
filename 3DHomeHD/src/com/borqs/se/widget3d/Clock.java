package com.borqs.se.widget3d;

import java.util.TimeZone;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.text.format.Time;
import android.util.Log;
import android.view.MotionEvent;

import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SECommand;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectGroup;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.engine.SESceneManager.TimeChangeCallBack;

public class Clock extends NormalObject implements TimeChangeCallBack {

    private static final String TAG = "clock";
    private SEObject mHourHand;
    private SEObject mMinuteHand;
    private SEObject mSecondHand;
    private Time mTime;
    private float mHours;
    private float mMinutes;
    private float mSeconds;
    private boolean mOnShow;
    private boolean mDisableTouch;
    private MoveAnimation mShowAnimation;
    private MoveAnimation mHideAnimation;
    private CloneClock mCloneClock;

    public Clock(SEScene scene, String name, int index) {
        super(scene, name, index);
        mTime = new Time();
    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        mHourHand = new SEObject(getScene(), getObjectInfo().mModelInfo.mChildNames[0], mIndex);
        mMinuteHand = new SEObject(getScene(), getObjectInfo().mModelInfo.mChildNames[1], mIndex);
        mSecondHand = new SEObject(getScene(), getObjectInfo().mModelInfo.mChildNames[2], mIndex);
        mSecondHand.setVisible(false, true);
        SESceneManager.getInstance().addTimeCallBack(this);
        setOnClickListener(new OnTouchListener() {
            public void run(SEObject obj) {
                show();
            }
        });
        setHasInit(true);
    }

    @Override
    public void onRelease() {
        super.onRelease();
        SESceneManager.getInstance().removeTimeCallBack(this);
    }

    private void setToCurrentTime(boolean post) {
        if (mHourHand == null || mMinuteHand == null || mSecondHand == null) {
            Log.e(TAG, "hands have not be initialized");
            return;
        }
        updateCurrentTime();
        updateClockModel(post);
    }

    private void updateCurrentTime() {
        mTime.setToNow();
        if (HomeUtils.DEBUG)
            Log.i(TAG, "set clock to current time: " + mTime.toString());

        int hour = mTime.hour;
        int minute = mTime.minute;
        int second = mTime.second;
        mMinutes = minute + second / 60.0f;
        mHours = hour + mMinutes / 60.0f;
        mMinutes = mMinutes % 60;
        mHours = mHours % 12;
        mSeconds = second % 60.0f;
    }

    private void updateClockModel(boolean post) {

        final float hourDegree = mHours / 12.0f * 360.0f;
        final float minuteDegree = mMinutes / 60.0f * 360.0f;
        final float secondDegree = mSeconds / 60.0f * 360.0f;
        final Runnable task = new Runnable() {
            public void run() {
                mHourHand.setRotate(new SERotate(hourDegree, 0, 1, 0), true);
                mMinuteHand.setRotate(new SERotate(minuteDegree, 0, 1, 0), true);
                mSecondHand.setRotate(new SERotate(secondDegree, 0, 1, 0), true);
            }
        };
        if (post) {
            new SECommand(getScene()) {
                public void run() {
                    task.run();
                }
            }.execute();
        } else {
            task.run();
        }
    }

    private void show() {
        if (!mOnShow) {
            stopAllAnimation(null);
            mOnShow = true;
            mDisableTouch = true;
            getScene().setStatus(SEScene.STATUS_ON_WIDGET_SIGHT, true);
            loadCloneClock(getAbsoluteTranslate());
            setVisible(false, true);
            getScene().setTouchDelegate(mCloneClock);
            SEVector3f cameraDir = getCamera().getAxisZ();
            SEVector3f desLocation = getCamera().getLocation().subtract(cameraDir.mul(650));
            desLocation.mD[2] = desLocation.getZ() + 50;
            mShowAnimation = new MoveAnimation(getScene(), desLocation);
            mShowAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    mDisableTouch = false;
                }
            });
            mShowAnimation.execute();
        }
    }

    private void loadCloneClock(SEVector3f location) {
        ObjectInfo objInfo = getObjectInfo();
        SEObject mother = getScene().findObject(objInfo.mName, 0);
        mother.cloneObject_JNI(getScene().getContentObject(), 1001, false, objInfo.mModelInfo.mStatus);
        mCloneClock = new CloneClock(getScene(), objInfo.mName, 1001);
        mCloneClock.setLocalScale(getObjectInfo().mModelInfo.mLocalTrans.mScale);
        mCloneClock.setTranslate(location, true);
        mCloneClock.setVisible(false, true);
        mCloneClock.setIsEntirety_JNI(true);
        getScene().getContentObject().addChild(mCloneClock, false);
    }

    private void unloadCloneClock() {
        SEObject root = getScene().getContentObject();
        root.removeChild(mCloneClock, true);
        mCloneClock.stopWork();
    }

    private void hide(boolean fast, final SEAnimFinishListener l) {
        if (mOnShow) {
            mDisableTouch = true;
            stopAllAnimation(null);
            if (fast) {
                unloadCloneClock();
                setVisible(true, true);
                mDisableTouch = false;
                mOnShow = false;
                getScene().setStatus(SEScene.STATUS_ON_WIDGET_SIGHT, false);
                getScene().removeTouchDelegate();
                if (l != null) {
                    l.onAnimationfinish();
                }
            } else {
                mHideAnimation = new MoveAnimation(getScene(), getAbsoluteTranslate());
                mHideAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                    public void onAnimationfinish() {
                        unloadCloneClock();
                        setVisible(true, true);
                        mDisableTouch = false;
                        mOnShow = false;
                        getScene().setStatus(SEScene.STATUS_ON_WIDGET_SIGHT, false);
                        getScene().removeTouchDelegate();
                        if (l != null) {
                            l.onAnimationfinish();
                        }
                    }
                });
                mHideAnimation.execute();
            }
        }
    }

    @Override
    public void stopAllAnimation(SEAnimFinishListener l) {
        if (mShowAnimation != null) {
            mShowAnimation.stop();
        }
        if (mHideAnimation != null) {
            mHideAnimation.stop();
        }
        super.stopAllAnimation(l);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mDisableTouch) {
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean handleBackKey(SEAnimFinishListener l) {
        if (mOnShow) {
            hide(false, l);
        }
        return false;
    }

    class MoveAnimation extends CountAnimation {
        private SEVector3f mEndLocation;
        private SEVector3f mCurLocation;
        private SEVector3f mStepLocation;

        public MoveAnimation(SEScene scene, SEVector3f endLocation) {
            super(scene);
            mEndLocation = endLocation;
        }

        public void runPatch(int count) {
            mCurLocation.selfAdd(mStepLocation);
            if (mCurLocation.subtract(mEndLocation).getLength() <= mStepLocation.getLength()) {
                mCurLocation = mEndLocation;
                stop();
            }
            mCloneClock.setTranslate(mCurLocation, true);

        }

        @Override
        public void onFirstly(int count) {
            if (!mCloneClock.isVisible()) {
                mCloneClock.setVisible(true, true);
            }
            mCurLocation = mCloneClock.getUserTranslate();
            mStepLocation = mEndLocation.subtract(mCurLocation).selfDiv(6);
            if (mStepLocation.getLength() < 20) {
                mStepLocation.normalize();
                mStepLocation.selfMul(20);
            }
        }
    }

    public void onTimeChanged() {
        mTime.switchTimezone(TimeZone.getDefault().getID());
        mTime.setToNow();
        setToCurrentTime(true);
    }

    public class CloneClock extends SEObjectGroup implements TimeChangeCallBack {
        private static final int RUN_SECOND = 0;
        private SEObject mHourHand;
        private SEObject mMinuteHand;
        private SEObject mSecondHand;
        private SEObject mDial;
        private Time mTime;
        private float mHours;
        private float mMinutes;
        private float mSeconds;
        private Handler mHandler;
        private HandlerThread mHandlerThread;

        public CloneClock(SEScene scene, String name, int index) {
            super(scene, name, index);
            mHourHand = new SEObject(scene, getObjectInfo().mModelInfo.mChildNames[0], index);
            mMinuteHand = new SEObject(scene, getObjectInfo().mModelInfo.mChildNames[1], index);
            mSecondHand = new SEObject(scene, getObjectInfo().mModelInfo.mChildNames[2], index);
            mDial = new SEObject(scene, getObjectInfo().mModelInfo.mChildNames[3], index);
            addChild(mDial, false);
            mDial.setIsEntirety_JNI(true);
            mSecondHand.setVisible(true, true);
            mTime = new Time();
            SESceneManager.getInstance().addTimeCallBack(this);
            mHandlerThread = new HandlerThread("Clock.update", Process.THREAD_PRIORITY_DEFAULT);
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                    case RUN_SECOND:
                        mHours += 1 / 3600f;
                        mMinutes += 1 / 60f;
                        mSeconds += 1;
                        mSeconds = mSeconds % 60;
                        updateClockModel(true);
                        removeMessages(RUN_SECOND);
                        sendEmptyMessageDelayed(RUN_SECOND, 1000);
                        break;
                    }
                }
            };
            mHandler.removeMessages(RUN_SECOND);
            mHandler.sendEmptyMessageDelayed(RUN_SECOND, 1000);
            mDial.setOnClickListener(new OnTouchListener() {
                public void run(SEObject obj) {
                    handOnClick();
                }
            });
            setOnClickListener(new OnTouchListener() {
                public void run(SEObject obj) {
                    hide(false, null);
                }
            });
        }

        @Override
        public void onRelease() {
            super.onRelease();
            SESceneManager.getInstance().removeTimeCallBack(this);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            if (mDisableTouch) {
                return true;
            }
            return super.dispatchTouchEvent(event);
        }

        public void stopWork() {
            mHandler.removeMessages(RUN_SECOND);
            mHandlerThread.quit();
        }

        public void onTimeChanged() {
            updateCurrentTime();
            updateClockModel(true);
        }

        private void updateCurrentTime() {
            mTime.switchTimezone(TimeZone.getDefault().getID());
            mTime.setToNow();
            int hour = mTime.hour;
            int minute = mTime.minute;
            int second = mTime.second;
            mMinutes = minute + second / 60.0f;
            mHours = hour + mMinutes / 60.0f;
            mMinutes = mMinutes % 60;
            mHours = mHours % 12;
            mSeconds = second % 60.0f;
        }

        private void updateClockModel(boolean post) {

            final float hourDegree = mHours / 12.0f * 360.0f;
            final float minuteDegree = mMinutes / 60.0f * 360.0f;
            final float secondDegree = mSeconds / 60.0f * 360.0f;
            final Runnable task = new Runnable() {
                public void run() {
                    mHourHand.setRotate(new SERotate(hourDegree, 0, 1, 0), true);
                    mMinuteHand.setRotate(new SERotate(minuteDegree, 0, 1, 0), true);
                    mSecondHand.setRotate(new SERotate(secondDegree, 0, 1, 0), true);

                }
            };
            if (post) {
                new SECommand(getScene()) {
                    public void run() {
                        task.run();
                    }
                }.execute();
            } else {
                task.run();
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float x = ev.getX();
                float y = ev.getY();
                if (checkOutside((int) x, (int) y)) {
                    setPressed(false);
                }
                break;
            }
            return super.onTouchEvent(ev);
        }
    }
}
