package com.borqs.se.widget3d;

import java.io.File;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SEVector2f;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.engine.SEObject;
import com.borqs.se.shortcut.LauncherModel;

public class Television extends WallNormalObject {
    private SEObject mGUANG;
    private UpdateAnimation mUpdateAnimation;
    private PlayVideoThumbnails mPlayAnimation;
    private String mImageName;
    private int mFaceIndex = -1;
    private String mCurrentImageName;
    private int mCurrentImage;

    public Television(SEScene scene, String name, int index) {
        super(scene, name, index);
        setCanChangeBind(false);
    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        mGUANG = new SEObject(getScene(), getObjectInfo().mModelInfo.mChildNames[0], mIndex);
        mGUANG.setScale(new SEVector3f(0, 0, 0), true);
        mImageName = mGUANG.getImageName_JNI();
        mCurrentImage = -1;
        setHasInit(true);
    }

    @Override
    public void handOnClick() {
        if (mCurrentImageName == null || !startToPlayVideo()) {
            super.handOnClick();
        }
    }

    @Override
    public void onSlotSuccess() {
        setIsFresh(false);
        super.onSlotSuccess();
    }
    public void onWallRadiusChanged(int faceIndex) {
        mFaceIndex = faceIndex;
        if (isPressed()) {
            return;
        }
        if (mFaceIndex == getObjectInfo().getSlotIndex()) {
            if (mCurrentImageName == null) {
                String[] thumbList = getThumbNameList();
                if (thumbList != null && thumbList.length > 0) {
                    mCurrentImage = 0;
                    mCurrentImageName = thumbList[mCurrentImage];
                    LauncherModel.getInstance().removeTask(mPlayCycle);
                    mPlayAnimation = new PlayVideoThumbnails(getScene());
                    mPlayAnimation.execute();
                    return;
                }
            }
            if (mCurrentImageName == null) {
                if (mUpdateAnimation == null || mUpdateAnimation.isFinish()) {
                    mUpdateAnimation = new UpdateAnimation(getScene());
                    mUpdateAnimation.execute();
                }
            } else {
                LauncherModel.getInstance().removeTask(mPlayCycle);
                LauncherModel.getInstance().process(mPlayCycle, 2500);
            }
        }
    }

    private void playUVAnimation() {
        if (mPlayAnimation == null || mPlayAnimation.isFinish()) {
            String[] thumbList = getThumbNameList();
            if (thumbList != null && thumbList.length > 0) {
                mCurrentImage++;
                if (mCurrentImage >= thumbList.length) {
                    mCurrentImage = 0;
                }
                mCurrentImageName = thumbList[mCurrentImage];
                LauncherModel.getInstance().removeTask(mPlayCycle);
                mPlayAnimation = new PlayVideoThumbnails(getScene());
                mPlayAnimation.execute();
            }

        }
    }

    private class UpdateAnimation extends CountAnimation {
        private float mStep;

        public UpdateAnimation(SEScene scene) {
            super(scene);
        }

        public void runPatch(int count) {
            if (count <= 15) {
                mStep = mStep + 0.05f;
                SEVector3f scale = new SEVector3f(1, 1, mStep);
                mGUANG.setScale(scale, true);
            } else if (count <= 25) {
                mStep = mStep - 0.05f;
                SEVector3f scale = new SEVector3f(1, 1, mStep);
                mGUANG.setScale(scale, true);
            } else if (count <= 37) {
                mStep = mStep + 0.075f;
                SEVector3f scale = new SEVector3f(1, 1, mStep);
                mGUANG.setScale(scale, true);
            } else if (count <= 50) {
                mStep = mStep - 0.1f;
                SEVector3f scale = new SEVector3f(1, 1, mStep);
                mGUANG.setScale(scale, true);
            } else if (count <= 100) {
                if (count == 51) {
                    mStep = -0.5f;
                    mGUANG.setScale(new SEVector3f(0, 0, 0), true);
                }
                mStep = mStep + 0.01f;
                if (count == 100) {
                    mStep = 1;
                }
            } else {
                mStep = mStep - 0.1f;
            }
        }

        @Override
        public void onFinish() {
            super.onFinish();
            mGUANG.setScale(new SEVector3f(0, 0, 0), true);
        }

        @Override
        public void onFirstly(int count) {
            super.onFirstly(count);
            mStep = 0;
            mGUANG.playUVAnimation(new SEVector2f(0, 0));
        }

        @Override
        public int getAnimationCount() {
            return 50;
        }
    }

    // ///////////////////////////////////////////////////////////////////////////////

    private class PlayVideoThumbnails extends CountAnimation {
        private int mImageData = 0;
        private float mStep = 0;
        private boolean mHasLoadImage = false;
        private boolean mCanUpdate = false;

        public PlayVideoThumbnails(SEScene scene) {
            super(scene);
        }

        @Override
        public void runPatch(int count) {
            if (mHasLoadImage && mImageData <= 0) {
                stop();
            } else if (mHasLoadImage && !mCanUpdate) {
                SEObject.applyImage_JNI(mImageName, mCurrentImageName);
                SEObject.addImageData_JNI(mCurrentImageName, mImageData);
                mCanUpdate = true;
            }

            if (mCanUpdate) {
                mStep = mStep + 0.025f;
                if (mStep >= 0.5f) {
                    stop();
                    if (mFaceIndex == getObjectInfo().getSlotIndex()) {
                        LauncherModel.getInstance().removeTask(mPlayCycle);
                        LauncherModel.getInstance().process(mPlayCycle, 5000);
                    }
                }
                SEVector2f dirCloud = new SEVector2f(0, -mStep);
                mGUANG.playUVAnimation(dirCloud);
            }

        }

        @Override
        public void onFirstly(int count) {
            changeImage(mCurrentImageName);
            mGUANG.setScale(new SEVector3f(1, 1, 1), true);
        }

        @Override
        public void onFinish() {

        }

        private void changeImage(final String name) {
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    mImageData = SEObject.loadImageData_JNI(HomeUtils.THUMB_PATH + "/" + name);
                    mHasLoadImage = true;
                }
            });
        }
    }

    Runnable mPlayCycle = new Runnable() {
        public void run() {
            if (mFaceIndex == getObjectInfo().getSlotIndex()) {
                playUVAnimation();
            }
        }

    };

    private String[] getThumbNameList() {
        File dir = new File(HomeUtils.THUMB_PATH);
        if (dir.exists() && dir.isDirectory()) {
            return dir.list();
        }
        return null;
    }

    private boolean startToPlayVideo() {
        if (!TextUtils.isEmpty(mCurrentImageName)) {
            String path = getPath(mCurrentImageName);
            if (!TextUtils.isEmpty(path)) {
                Uri fileUri = Uri.fromFile(new File(path));
                final Intent fileIntent = new Intent();
                fileIntent.setAction(Intent.ACTION_VIEW);
                fileIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                fileIntent.setDataAndType(fileUri, "video/*");
                try {
                    getContext().startActivity(fileIntent);
                    return true;
                } catch (ActivityNotFoundException e) {
                    Log.e(HomeUtils.TAG, "Thumbnails can not find the activity to play video.");
                }
            }
        }
        return false;
    }
    
    private String getPath(String videoId) {
        String path = null;
        if (TextUtils.isEmpty(videoId)) {
            return path;
        }
        String[] projection = new String[] {MediaStore.Video.Media.DATA};
        String where = MediaStore.Video.Media._ID + "=" + videoId;
        Cursor cursor = getContext().getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, 
                projection, where, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }
    
    @Override
    public void onRelease() {
        super.onRelease();
    }

    protected SEVector3f getModelScale() {
        if (isScreenOrientationPortrait()) {
            return PORT_SCALE;
        } else {
            return super.getModelScale();
        }
    }
}
