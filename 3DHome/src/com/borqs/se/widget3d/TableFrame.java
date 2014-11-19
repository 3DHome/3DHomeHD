package com.borqs.se.widget3d;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;

import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEObject;
import com.borqs.se.home3d.HomeScene;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.HomeManager;

public class TableFrame extends NormalObject {
    private SEObject mPhotoObject;
    private int mRequestCode;

    private String mImageName;
    protected String mSaveImagePath;
    protected int mWidth;
    protected int mHeight;

    protected int mImageSize;

    public TableFrame(HomeScene scene, String name, int index) {
        super(scene, name, index);
        mWidth = 43;
        mHeight = 64;
        mImageSize = 128;
    }

    @Override
    public void initStatus() {
        super.initStatus();
        setCanChangeBind(false);
        mRequestCode = (int) System.currentTimeMillis();
        if (mRequestCode < 0) {
            mRequestCode = -mRequestCode;
        }
        mSaveImagePath = getContext().getFilesDir() + File.separator + getScene().getSceneName() + mName + mIndex + ".png";
        File dir = new File(HomeUtils.SDCARD_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        mPhotoObject = findComponenetObjectByRegularName("picture");
        mImageName = mPhotoObject.getImageName();
        initImage();
        setHasInit(true);
    }

    private void initImage() {
        File f = new File(mSaveImagePath);
        if (f.exists()) {
            SEObject.applyImage_JNI(mImageName, mSaveImagePath);
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    final int imageData = SEObject.loadImageData_JNI(mSaveImagePath);
                    if (imageData != 0)
                        new SECommand(getHomeScene()) {
                            public void run() {
                                SEObject.addImageData_JNI(mSaveImagePath, imageData);
                            }
                        }.execute();
                }
            });
        }
    }

    @Override
    public void onRelease() {
        super.onRelease();
        File f = new File(mSaveImagePath);
        if (f.exists()) {
            f.delete();
        }
    }

    @Override
    public void handOnClick() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", mWidth);
        intent.putExtra("aspectY", mHeight);
        intent.putExtra("output", Uri.fromFile(new File(HomeUtils.createImageTmpFile())));
        intent.putExtra("outputFormat", "JPEG");
        HomeManager.getInstance().startActivityForResult(intent, mRequestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == mRequestCode) {
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    Uri url = null;
                    if (data != null) {
                        url = data.getData();
                    }
                    final Uri imageFileUri = url;
                    Bitmap bm = null;
                    if (imageFileUri != null) {
                        bm = HomeUtils.decodeSampledBitmapFromResource(getContext(), imageFileUri, mImageSize, mImageSize);
                    } else {
                        bm = HomeUtils.decodeSampledBitmapFromResource(HomeUtils.TMPDATA_IMAGE_PATH, mImageSize,
                            mImageSize);
                    }
                    if (bm == null) {
                        return;
                    }                    
                    if (bm.getWidth() > bm.getHeight()) {
                        bm = Bitmap.createScaledBitmap(bm, mImageSize, mImageSize * bm.getHeight() / bm.getWidth(), true);
                    } else {
                        bm = Bitmap.createScaledBitmap(bm, mImageSize * bm.getWidth() / bm.getHeight(), mImageSize, true);
                    }
                    
                    Bitmap des = Bitmap.createBitmap(mImageSize, mImageSize, Bitmap.Config.RGB_565);
                    int left;
                    int top;
                    if (bm.getWidth() > bm.getHeight()) {
                        left = 0;
                        top = (mImageSize - bm.getHeight())/ 2;
                    } else {
                        left = (mImageSize- bm.getWidth())/ 2;
                        top = 0;
                    }
                    Canvas canvas = new Canvas(des);
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    canvas.drawBitmap(bm, left, top, paint);
                    bm.recycle();
                    HomeUtils.saveBitmap(des, mSaveImagePath, Bitmap.CompressFormat.PNG);
                    des.recycle();
                    final int imageData = SEObject.loadImageData_JNI(mSaveImagePath);
                    new SECommand(getHomeScene()) {
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

}
