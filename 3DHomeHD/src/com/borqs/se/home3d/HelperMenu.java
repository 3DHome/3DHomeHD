package com.borqs.se.home3d;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import com.borqs.framework3d.home3d.DockObject;
import com.borqs.freehdhome.R;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEVector.SERect3D;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SEBitmap;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectFactory;
import com.borqs.se.engine.SEObjectGroup;
import com.borqs.se.engine.SESceneManager;

public class HelperMenu extends SEObjectGroup {
    private static final int FACE_NUMBER = 7;
    private Context mContext;
    private int mSceneWidth;
    private int mSceneHeight;
    private static final float SCALE = 0.1f;
    private int mX;
    private boolean mDisableTouch;
    private DockObject mDockObject;
    private VelocityTracker mVelocityTracker;
    private SetFaceAnimation mSetFaceAnimation;
    private VelocityAnimation mVelocityAnimation;
    private CreateFaceTask mCreateFaceTask;
    private boolean mNeedWait;

    public HelperMenu(SEScene scene, String name) {
        super(scene, name, 0);
        mContext = getContext();
        mDisableTouch = true;
    }

    @Override
    public void onRenderFinish(final SECamera camera) {
        super.onRenderFinish(camera);
        mSceneWidth = SESceneManager.getInstance().getWidth();
        mSceneHeight = SESceneManager.getInstance().getHeight();
        mX = 0;
        setVisible(false, true);
    }

    public void show(final DockObject dockObject) {
        if (!getScene().getStatus(SEScene.STATUS_HELPER_MENU)) {
            mDisableTouch = true;
            stopAllAnimation(null);
            getScene().setStatus(SEScene.STATUS_HELPER_MENU, true);
            mNeedWait = true;
            mX = 0;
            loadAllFace();
            getScene().setTouchDelegate(this);
            getCamera().moveToWallSight(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    mDockObject = dockObject;
                    performShowAction(null);
                }
            });

        }
    }

    private void loadAllFace() {
        mCreateFaceTask = new CreateFaceTask();
        new Thread(mCreateFaceTask).start();
    }

    private void setCubeToRightPosition() {
        mDisableTouch = true;
        SEVector3f location = getCamera().getScreenLocation(SCALE);
        mSetFaceAnimation = new SetFaceAnimation(getScene(), location);
        mSetFaceAnimation.setAnimFinishListener(new SEAnimFinishListener() {
            public void onAnimationfinish() {
                mCreateFaceTask.go();
            }
        });
        mSetFaceAnimation.execute();
    }

    public void hide(boolean fast, final SEAnimFinishListener l) {
        if (getScene().getStatus(SEScene.STATUS_HELPER_MENU)) {
            stopAllAnimation(null);
            mDisableTouch = true;
            getScene().removeTouchDelegate();
            mCreateFaceTask.stop();
            if (fast) {
                performHideAction(l);
                return;
            }
            payVelocityAnimation(0, 1, new SEAnimFinishListener() {

                public void onAnimationfinish() {
                    resetPosition();
                    float locationY = 280;
                    SEVector3f location = new SEVector3f(0, locationY, -30);
                    mSetFaceAnimation = new SetFaceAnimation(getScene(), location);
                    mSetFaceAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                        public void onAnimationfinish() {
                            performHideAction(null);
                        }
                    });
                    mSetFaceAnimation.execute();

                }

            });
        }
    }

    @Override
    public void stopAllAnimation(SEAnimFinishListener l) {
        if (mSetFaceAnimation != null) {
            mSetFaceAnimation.stop();
        }
        if (mVelocityAnimation != null) {
            mVelocityAnimation.stop();
        }
    }

    @Override
    public boolean handleBackKey(SEAnimFinishListener l) {
        if (getScene().getStatus(SEScene.STATUS_HELPER_MENU)) {
            hide(false, l);
            return true;
        }
        return false;
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
        if (mDisableTouch) {
            return true;
        }
        trackVelocity(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
        case MotionEvent.ACTION_MOVE:
            int dX = getTouchX() - getPreTouchX();
            if (Math.abs(dX) > getTouchSlop() / 2) {
                setPreTouch();
                mX = (int) (mX + dX * 0.2f);
                updatePosition();
            }
            break;
        case MotionEvent.ACTION_DOWN:
            setPreTouch();
            if (mVelocityAnimation != null) {
                mVelocityAnimation.stop();
            }
            break;
        case MotionEvent.ACTION_UP:
            setPreTouch();
        case MotionEvent.ACTION_CANCEL:
            payVelocityAnimation(mVelocityTracker.getXVelocity(), 1, null);
            break;
        }
        return true;
    }

    private void payVelocityAnimation(float vX, float step, SEAnimFinishListener l) {
        if (mVelocityAnimation != null) {
            mVelocityAnimation.stop();
        }
        mVelocityAnimation = new VelocityAnimation(getScene(), vX, step);
        mVelocityAnimation.setAnimFinishListener(l);
        mVelocityAnimation.execute();
    }

    private void resetPosition() {
        int size = mChildObjects.size();
        for (int i = 0; i < size; i++) {
            SEObject child = mChildObjects.get(i);
            child.setTranslate(new SEVector3f(0, 0, 0), true);
        }
    }

    private void updatePosition() {
        int size = mChildObjects.size();
        int step = (int) (mSceneWidth * 0.12f);
        for (int i = 0; i < size; i++) {
            SEObject child = mChildObjects.get(i);
            float x = i * step + mX;
            float y = Math.abs(2 * x);
            child.setTranslate(new SEVector3f(x, y, 0), true);
        }
    }

    private class CreateFaceTask implements Runnable {
        private float mPixelDensity;
        private float mFontScale;
        private boolean mNeedStop;

        public CreateFaceTask() {
            Configuration config = new Configuration();
            android.provider.Settings.System.getConfiguration(mContext.getContentResolver(), config);
            mPixelDensity = SESceneManager.getInstance().getPixelDensity();
            mFontScale = config.fontScale;
            mNeedStop = false;
        }

        public synchronized void go() {
            mNeedWait = false;
            notifyAll();
        }

        public synchronized void stop() {
            mNeedStop = true;
            mNeedWait = false;
            notifyAll();
        }

        public void run() {
            for (int index = 0; index < FACE_NUMBER; index++) {
                if (mNeedStop) {
                    return;
                }
                final SEObject obj = new SEObject(getScene(), "HelperPreview_" + index, 0);
                SEBitmap bp = new SEBitmap(getBitmap(index), SEBitmap.Type.normal);
                SERect3D rect = new SERect3D();
                rect.setSize(mSceneWidth, mSceneHeight, SCALE);
                SEObjectFactory.createOpaqueRectangle(obj, rect, obj.mName + "_imageName", obj.mName + "_imageKey", bp);
                obj.setImageSize(mSceneWidth, mSceneHeight);
                final int i = index;
                new SECommand(getScene()) {
                    public void run() {
                        if (mNeedStop) {
                            return;
                        }
                        int step = (int) (mSceneWidth * 0.12f);
                        float x = i * step + mX;
                        float y = Math.abs(2 * x);
                        obj.getUserTransParas().mTranslate.set(x, y, 0);
                        addChild(obj, true);
                        obj.setClickable(false);
                        if (i == FACE_NUMBER - 1) {
                            mDisableTouch = false;
                        }
                    }
                }.execute();
                synchronized (this) {
                    while (mNeedWait) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        private Bitmap getBitmap(int index) {
            TextPaint titlePaint = new TextPaint();
            float fontSize = mFontScale * mPixelDensity
                    * getContext().getResources().getDimension(R.dimen.helper_title_font);
            titlePaint.setTextSize(fontSize);
            titlePaint.setColor(mContext.getResources().getColor(R.color.help_blue));
            titlePaint.setAntiAlias(true);
            int newW = HomeUtils.higherPower2(mSceneWidth);
            int newH = HomeUtils.higherPower2(mSceneHeight);
            Bitmap bitmap = Bitmap.createBitmap(newW, newH, Bitmap.Config.RGB_565);            
            Canvas canvas = new Canvas(bitmap);
            canvas.translate((newW - mSceneWidth) * 0.5f, (newH - mSceneHeight) * 0.5f);
            canvas.save();

            String title = "";
            switch (index) {
            case 0:
                title = mContext.getResources().getString(R.string.help_menu_0);
                break;
            case 1:
                title = mContext.getResources().getString(R.string.help_menu_1);
                break;
            case 2:
                title = mContext.getResources().getString(R.string.help_menu_2);
                break;
            case 3:
                title = mContext.getResources().getString(R.string.help_menu_3);
                break;
            case 4:
                title = mContext.getResources().getString(R.string.help_menu_4);
                break;
            case 5:
                title = mContext.getResources().getString(R.string.help_menu_5);
                break;
            case 6:
                title = mContext.getResources().getString(R.string.help_menu_6);
                break;
            }

            int w = mSceneWidth ;
            int h = mSceneHeight;
            try {
                drawImage(canvas, index, w, h);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            canvas.translate(0, h * 85/100);
            StaticLayout titleLayout = new StaticLayout(title, titlePaint, mSceneWidth, Alignment.ALIGN_CENTER, 1.1f,
                    0.0F, true);
            titleLayout.draw(canvas);
            canvas.restore();
            
            drawText(canvas, index, mSceneWidth);
            return bitmap;
        }

        private void drawImage(Canvas canvas, int index, int w, int h) throws IOException {
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(0);
            paint.setColor(0xff008000);
            paint.setAntiAlias(true);
            int imageIndex = index;
            InputStream is_one = mContext.getAssets().open("base/help/" + imageIndex + ".jpg");
            Bitmap bitmap_one = BitmapFactory.decodeStream(is_one);
            is_one.close();
            Rect src = new Rect(0, 0, bitmap_one.getWidth(), bitmap_one.getHeight());
            int left = 0;
            int right = w;
            int top = 0;
            int bottom = h;
            Rect dst = new Rect(left, top, right, bottom);
            canvas.drawBitmap(bitmap_one, src, dst, paint);
            bitmap_one.recycle();
            canvas.drawRect(dst, paint);
        }

        private void drawText(Canvas canvas, int index, int w) {
            TextPaint titlePaint = new TextPaint();
            float fontSize = getContext().getResources().getDimension(R.dimen.helper_content_font) * mFontScale
                    * mPixelDensity;
            titlePaint.setTextSize(fontSize);
            titlePaint.setColor(mContext.getResources().getColor(R.color.help_black));
            titlePaint.setAntiAlias(true);
            String title = null;
            switch (index) {
            case 0:
            	canvas.save();
            	canvas.translate(mSceneWidth * 1/10, mSceneHeight * 1/8);
                title = mContext.getResources().getString(R.string.help_0);
                break;
            case 1:
            	canvas.save();
            	canvas.translate(mSceneWidth * 3/5, mSceneHeight * 1/6);
                title = mContext.getResources().getString(R.string.help_1);
                w = mSceneWidth * 2/5;
                break;
            case 2:
            	canvas.save();
            	canvas.translate(mSceneWidth * 55/100, mSceneHeight * 1/7);
                title = mContext.getResources().getString(R.string.help_2);
                w = mSceneWidth * 2/5;
                break;
            case 3:
            	canvas.save();
            	canvas.translate(mSceneWidth * 55/100, mSceneHeight * 1/6);
                title = mContext.getResources().getString(R.string.help_3);
                w = mSceneWidth * 2/5;
                break;
            case 4:
            	canvas.save();
            	canvas.translate(mSceneWidth * 1/10, mSceneHeight * 1/6);
                title = mContext.getResources().getString(R.string.help_4);
                w = mSceneWidth * 1/3;
                break;
            case 5:
            	canvas.save();
            	canvas.translate(mSceneWidth * 1/2, mSceneHeight * 2/3);
                title = mContext.getResources().getString(R.string.help_5);
                break;
            case 6:
            	canvas.save();
            	canvas.translate(mSceneWidth * 55/100, mSceneHeight * 3/5);
                title = mContext.getResources().getString(R.string.help_6);
                break;
            }
            if (title != null) {
                StaticLayout layout = new StaticLayout(title, titlePaint, w, Alignment.ALIGN_NORMAL, 1.f, 0.0F, false);
                layout.draw(canvas);
                canvas.restore();
            }
        }
    }

    private class SetFaceAnimation extends CountAnimation {
        private SEVector3f mEndLocation;
        private SEVector3f mCurLocation;
        private SEVector3f mStepLocation;

        public SetFaceAnimation(SEScene scene, SEVector3f endLocation) {
            super(scene);
            mEndLocation = endLocation;
        }

        public void runPatch(int count) {
            if (mStepLocation != null) {
                mCurLocation.selfAdd(mStepLocation);
                if (mEndLocation.subtract(mCurLocation).getLength() <= mStepLocation.getLength()) {
                    mCurLocation = mEndLocation;
                    mStepLocation = null;
                }
                setTranslate(mCurLocation, true);
            }

            if (mStepLocation == null) {
                stop();
            }
        }

        @Override
        public void onFirstly(int count) {
            mCurLocation = getUserTranslate();
            if (!mCurLocation.equals(mEndLocation)) {
                mStepLocation = mEndLocation.subtract(mCurLocation).selfDiv(6);
                if (mStepLocation.getLength() < 20) {
                    mStepLocation.normalize();
                    mStepLocation.selfMul(20);
                }
            }
        }
    }

    private class VelocityAnimation extends CountAnimation {
        private float mVelocity;
        private int mDesTranslateX;
        private float mStep;
        private boolean mNeedExit;

        public VelocityAnimation(SEScene scene, float velocity, float step) {
            super(scene);
            if (Math.abs(velocity) < 100) {
                mVelocity = 0;
            } else {
                mVelocity = velocity;
            }
            mStep = step;
            mNeedExit = false;
        }

        public void runPatch(int count) {
            int needTranslateX = mDesTranslateX - mX;
            int absNTX = Math.abs(needTranslateX);
            if (absNTX <= mStep) {
                mX = mDesTranslateX;
                if (mNeedExit) {
                    hide(true, null);
                }
                stop();
            } else {
                int step = (int) (mStep * Math.sqrt(absNTX));
                if (needTranslateX < 0) {
                    step = -step;
                }
                mX = mX + step;
            }
            updatePosition();

        }

        @Override
        public void onFirstly(int count) {
            int needTranslateX = (int) (mVelocity * 0.012f);
            mDesTranslateX = mX + needTranslateX;
            int size = mChildObjects.size();
            int step = (int) (mSceneWidth * 0.12f);
            int minX = (int) (step * (1 - size));
            int maxX = 0;
            if (mDesTranslateX < minX) {
                if (mDesTranslateX < minX - step) {
                    mDesTranslateX = minX - step;
                    mNeedExit = true;
                } else {
                    mDesTranslateX = minX;
                }
            } else if (mDesTranslateX > maxX) {
                mDesTranslateX = maxX;
            } else {
                float flyX = (int) ((mDesTranslateX - step * 0.5f) / step);
                if (flyX > 0.2f * step) {
                    mDesTranslateX = step;
                } else if (flyX < -0.2f * step) {
                    mDesTranslateX = -step;
                } else {
                    mDesTranslateX = ((int) flyX) * step;
                }
            }
        }

    }

    private void onShown(SEAnimFinishListener listener) {
        float locationY = 280;
        setTranslate(new SEVector3f(0, locationY, -30), true);
        setVisible(true, true);
        setCubeToRightPosition();
    }
    private void onHidden(SEAnimFinishListener listener) {
        getScene().setStatus(SEScene.STATUS_HELPER_MENU, false);
        removeAllChild(true);
        if (listener != null) {
            listener.onAnimationfinish();
        }
    }
    private void performShowAction(final SEAnimFinishListener listener) {
        if (mDockObject != null) {
            mDockObject.hide(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    onShown(listener);
                }
            });
        } else {
            onShown(listener);
        }
    }
    private void performHideAction(final SEAnimFinishListener listener) {
        setVisible(false, true);
        if (mDockObject != null) {
            mDockObject.show(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    onHidden(listener);
                }
            });
        } else {
            onHidden(listener);
        }
    }
}
