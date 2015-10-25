package com.borqs.se.widget3d;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.text.TextUtils;
import android.view.MotionEvent;

import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.engine.SECommand;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SESceneManager;

public class WallFrame extends WallNormalObject {
    protected SwingAnimation mSwingAnimation;
    protected SEObject mPicture;
    protected SEObject mFrame;
    protected SEObject mShadow;
    public SEObject mRope;
    public SEObject mHook;

    protected int mRequestCode;
    protected int mWidth;
    protected int mHeight;

    protected int mImageSize;

    private String mImageName;
    protected String mSaveImagePath;
    private File mSdcardTempFile;

    public WallFrame(SEScene scene, String name, int index) {
        super(scene, name, index);
        setCanChangeBind(false);
        mWidth = 0;
        mHeight = 0;
        mImageSize = 0;
    }

    @Override
    public void setShadowObjectVisibility_JNI(boolean enable) {
        super.setShadowObjectVisibility_JNI(enable && isShadowNeeded());
        if (mHook != null) {
            mHook.setVisible(enable, true);
        }
        if (mRope != null) {
            mRope.setVisible(enable, true);
        }
    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        mRequestCode = (int) System.currentTimeMillis();
        if (mRequestCode < 0) {
            mRequestCode = -mRequestCode;
        }
        mSaveImagePath = HomeUtils.PKG_FILES_PATH + scene.mSceneName + mName + mIndex + ".png";
        mSdcardTempFile = HomeUtils.getTempImageFile();
        mPicture = new SEObject(getScene(), getObjectInfo().mModelInfo.mChildNames[0], mIndex);
        mFrame = new SEObject(getScene(), getObjectInfo().mModelInfo.mChildNames[1], mIndex);
        mShadow = new SEObject(getScene(), getObjectInfo().mModelInfo.mChildNames[2], mIndex);
        mRope = new SEObject(getScene(), getObjectInfo().mModelInfo.mChildNames[3], mIndex);
        mHook = new SEObject(getScene(), getObjectInfo().mModelInfo.mChildNames[4], mIndex);
        mHook.setVisible(true, true);
        mRope.setVisible(true, true);
        mImageName = mPicture.getImageName_JNI();
        setHasInit(true);
    }

    protected int getCloneID() {
        String IDStr = "";
        if (!TextUtils.isEmpty(mName) && mName.contains("_clone_")) {
            IDStr = mName.substring(mName.indexOf("_clone_") + "_clone_".length(), mName.length());
        } else {
            return 0;
        }
        return Integer.parseInt(IDStr);
    }

    @Override
    public void handOnClick() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", mWidth);
        intent.putExtra("aspectY", mHeight);
        intent.putExtra("output", Uri.fromFile(mSdcardTempFile));
        intent.putExtra("outputFormat", "JPEG");
        SESceneManager.getInstance().startActivityForResult(intent, mRequestCode);
    }

    @Override
    public void onSlotSuccess() {
        setIsFresh(false);
        super.onSlotSuccess();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == mRequestCode) {
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(mSdcardTempFile.getAbsolutePath(), options);
                    options.inSampleSize = HomeUtils.computeSampleSize(options, -1, mImageSize * mImageSize);
                    options.inJustDecodeBounds = false;
                    Bitmap bm = BitmapFactory.decodeFile(mSdcardTempFile.getAbsolutePath(), options);
                    if (bm == null) {
                        return;
                    }
                    Bitmap des = Bitmap.createBitmap(mImageSize, mImageSize, Bitmap.Config.RGB_565);
                    Rect srcRect = new Rect(0, 0, bm.getWidth(), bm.getHeight());
                    int newW;
                    int newH;
                    if (bm.getWidth() > bm.getHeight()) {
                        newW = mImageSize;
                        newH = bm.getHeight() * mImageSize / bm.getWidth();
                    } else {
                        newH = mImageSize;
                        newW = bm.getWidth() * mImageSize / bm.getHeight();
                    }
                    Rect desRect = new Rect((mImageSize - newW) / 2, (mImageSize - newH) / 2, (mImageSize + newW) / 2,
                            (mImageSize + newH) / 2);
                    Canvas canvas = new Canvas(des);
                    canvas.drawBitmap(bm, srcRect, desRect, null);
                    bm.recycle();
                    HomeUtils.saveBitmap(des, mSaveImagePath, Bitmap.CompressFormat.PNG);
                    des.recycle();

                    final int imageData = SEObject.loadImageData_JNI(mSaveImagePath);
                    new SECommand(SESceneManager.getInstance().getCurrentScene()) {
                        public void run() {
                            SEObject.applyImage_JNI(mImageName, mSaveImagePath);
                            SEObject.addImageData_JNI(mSaveImagePath, imageData);
                        }
                    }.execute();
                    System.gc();
                }
            });

        }
    }

    public void onWallRadiusChanged(int faceIndex) {
        if (isPressed()) {
            return;
        }
        if (faceIndex == getObjectInfo().getSlotIndex()) {
            if (mSwingAnimation != null) {
                mSwingAnimation.stop();
            }
            mSwingAnimation = new SwingAnimation(getScene());
            mSwingAnimation.execute();

        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mSwingAnimation != null) {
                mSwingAnimation.stop();
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private class SwingAnimation extends CountAnimation {
        private boolean mDirection_Center;
        private float mSpan_Center;

        public SwingAnimation(SEScene scene) {
            super(scene);

        }

        @Override
        public void runPatch(int count) {
            float angle = (float) ((mSpan_Center - count * mSpan_Center / 200) * Math.sin(Math.PI * count / 50));
            if (mDirection_Center) {
                angle = -angle;
            }
            mFrame.setRotate(new SERotate(angle, 0, 1, 0), true);
            mPicture.setRotate(new SERotate(angle, 0, 1, 0), true);
            mShadow.setRotate(new SERotate(angle, 0, 1, 0), true);
            mRope.setRotate(new SERotate(angle, 0, 1, 0), true);
        }

        @Override
        public void onFirstly(int count) {
            if (Math.random() > 0.5) {
                mDirection_Center = true;
            } else {
                mDirection_Center = false;
            }
            mSpan_Center = (float) (Math.random() * 15 + 1);
        }

        @Override
        public void onFinish() {
            mFrame.setRotate(new SERotate(0, 0, 1, 0), true);
            mPicture.setRotate(new SERotate(0, 0, 1, 0), true);
            mShadow.setRotate(new SERotate(0, 0, 1, 0), true);
            mRope.setRotate(new SERotate(0, 0, 1, 0), true);
        }

        @Override
        public int getAnimationCount() {
            return 200;
        }
    }
    
    @Override
    public void onRelease() {
        super.onRelease();
    }
}
