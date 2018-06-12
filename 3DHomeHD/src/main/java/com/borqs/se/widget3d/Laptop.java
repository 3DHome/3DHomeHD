package com.borqs.se.widget3d;

import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.animation.BounceInterpolator;

import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.home3d.ApplicationMenu;
import com.borqs.se.home3d.HomeScene;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.engine.SESceneManager.UnlockScreenListener;

public class Laptop extends NormalObject implements UnlockScreenListener {
    private final static float CLOSE_SHELL_ANGLE = 115;
    private SEObject mStand;
    private SEObject mScreen;
    private float mCurrentShellAngle;
    private boolean mCancelClick;
    private OpenOrCloseAnimation mOpenOrCloseAnimation;
    private VelocityTracker mVelocityTracker;

    public Laptop(SEScene scene, String name, int index) {
        super(scene, name, index);
        mCurrentShellAngle = 0;
        mCancelClick = false;
    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        mScreen = new SEObject(scene, getObjectInfo().mModelInfo.mChildNames[0], mIndex);
        mStand = new SEObject(scene, getObjectInfo().mModelInfo.mChildNames[1], mIndex);
        mStand.setLeader(mScreen);
        SESceneManager.getInstance().addUnlockScreenListener(this);
        setOnClickListener(new SEObject.OnTouchListener() {
            public void run(SEObject obj) {
                if (mCurrentShellAngle < 5) {
                    getScene().handleMessage(HomeScene.MSG_TYPE_SHOW_APP_VIEW, ApplicationMenu.TYPE_SHOW_KEEP_LAST);
                } else {
                    runOpenOrCloseAnimation(new SEAnimFinishListener() {
                        public void onAnimationfinish() {
                            getScene().handleMessage(HomeScene.MSG_TYPE_SHOW_APP_VIEW, ApplicationMenu.TYPE_SHOW_KEEP_LAST);
                        }
                    }, 5, true);
                }
            }
        });
        setOnLongClickListener(null);
        setHasInit(true);
    }

    @Override
    public void onRelease() {
        super.onRelease();
        SESceneManager.getInstance().removeUnlockScreenListener(this);
    }

    private void trackVelocity(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        final VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (getScene().getStatus(SEScene.STATUS_ON_UNLOCK_ANIMATION)) {
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        trackVelocity(event);
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            setPreTouch();
            mCancelClick = false;
            setPressed(true);
            break;
        case MotionEvent.ACTION_MOVE:
            if (mOpenOrCloseAnimation != null) {
                mOpenOrCloseAnimation.stop();
            }
            mCancelClick = true;
            float changeY = -getTouchY() + getPreTouchY();
            float changeX = getTouchX() - getPreTouchX();
            double angle = (getParent().getUserRotate().getAngle() - 90) * Math.PI / 180;
            float change = (float) (changeX * Math.cos(angle) + changeY * Math.sin(angle));
            mCurrentShellAngle = mCurrentShellAngle + change / 2;
            if (mCurrentShellAngle < 0) {
                mCurrentShellAngle = 0;
            } else if (mCurrentShellAngle > CLOSE_SHELL_ANGLE) {
                mCurrentShellAngle = CLOSE_SHELL_ANGLE;
            }
            setShellAngle(mCurrentShellAngle);
            setPreTouch();
            break;
        case MotionEvent.ACTION_UP:
            setPreTouch();
            setPressed(false);
            if (!mCancelClick) {
                performClick();
                return true;
            }

        case MotionEvent.ACTION_CANCEL:
            setPressed(false);
            boolean isOpen = true;
            float vY = -mVelocityTracker.getYVelocity();
            float vX = mVelocityTracker.getXVelocity();
            double deskAngle = (getParent().getUserRotate().getAngle() - 90) * Math.PI / 180;
            float changeV = (float) (vX * Math.cos(deskAngle) + vY * Math.sin(deskAngle));
            if (changeV < -500) {
                isOpen = true;
            } else if (changeV > 500) {
                isOpen = false;
            } else {
                if (mCurrentShellAngle < CLOSE_SHELL_ANGLE / 2) {
                    isOpen = true;
                } else {
                    isOpen = false;
                }
            }
            runOpenOrCloseAnimation(null, 5, isOpen);
            break;
        }
        return true;
    }

    public void setClose() {
        if (mCurrentShellAngle != CLOSE_SHELL_ANGLE) {
            if (mOpenOrCloseAnimation != null) {
                mOpenOrCloseAnimation.stop();
            }
            setShellAngle(CLOSE_SHELL_ANGLE);
        }
    }

    private void setShellAngle(float angle) {
        mCurrentShellAngle = angle;
        mScreen.setRotate(new SERotate(angle, 1, 0, 0), true);
        mStand.setRotate(new SERotate(-angle / 2.6f, 1, 0, 0), true);
    }

    private void runOpenOrCloseAnimation(SEAnimFinishListener listener, float step, boolean openOrClose) {
        if (mOpenOrCloseAnimation != null) {
            mOpenOrCloseAnimation.stop();
        }
        mOpenOrCloseAnimation = new OpenOrCloseAnimation(getScene(), step, openOrClose);
        mOpenOrCloseAnimation.setAnimFinishListener(listener);
        mOpenOrCloseAnimation.execute();
    }

    class OpenOrCloseAnimation extends CountAnimation {
        private float mShellStartAngle;
        private float mShellRotateStep;
        private int mAnimCount;
        private BounceInterpolator mBounceInterpolator;
        private boolean mIsOpen;

        public OpenOrCloseAnimation(SEScene scene, float step, boolean openOrClose) {
            super(scene);
            mIsOpen = openOrClose;
            if (mIsOpen) {
                mAnimCount = (int) (mCurrentShellAngle / step);
                if (mAnimCount != 0) {
                    mBounceInterpolator = new BounceInterpolator();
                    mShellStartAngle = mCurrentShellAngle;
                    mShellRotateStep = -mCurrentShellAngle / mAnimCount;
                }
            } else {
                mAnimCount = (int) ((CLOSE_SHELL_ANGLE - mCurrentShellAngle) / step);
                if (mAnimCount != 0) {
                    mShellStartAngle = mCurrentShellAngle;
                    mShellRotateStep = (CLOSE_SHELL_ANGLE - mCurrentShellAngle) / mAnimCount;
                }
            }
        }

        @Override
        public void runPatch(int count) {
            if (mIsOpen) {
                int newCount = (int) (mAnimCount * mBounceInterpolator.getInterpolation((float) count / mAnimCount));
                if (count == mAnimCount) {
                    mCurrentShellAngle = 0;
                    stop();
                } else {
                    mCurrentShellAngle = mShellStartAngle + newCount * mShellRotateStep;
                }
            } else {
                if (count == mAnimCount) {
                    mCurrentShellAngle = CLOSE_SHELL_ANGLE;
                    stop();
                } else {
                    mCurrentShellAngle = mShellStartAngle + count * mShellRotateStep;
                }
            }
            setShellAngle(mCurrentShellAngle);
        }

        @Override
        public void onFirstly(int count) {
            if (mAnimCount == 0) {
                stop();
            }
        }

        @Override
        public int getAnimationCount() {
            return mAnimCount;
        }

    }

    @Override
    public void unlockScreen() {
        boolean disable = getStatus(SEScene.STATUS_APP_MENU) | getStatus(SEScene.STATUS_HELPER_MENU)
                | getStatus(SEScene.STATUS_OPTION_MENU) | getStatus(SEScene.STATUS_OBJ_MENU)
                | getStatus(SEScene.STATUS_ON_SKY_SIGHT) | getStatus(SEScene.STATUS_ON_WIDGET_SIGHT)
                | getStatus(SEScene.STATUS_ON_WALL_DIALOG);
        if (!disable) {
            new SECommand(getScene()) {
                public void run() {
                    setClose();
                    runOpenOrCloseAnimation(null, 2f, true);
                }
            }.execute();
        }
    }

    private boolean getStatus(int type) {
        return getScene().getStatus(type);
    }

}
