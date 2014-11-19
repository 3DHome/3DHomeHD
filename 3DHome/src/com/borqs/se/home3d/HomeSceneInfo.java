package com.borqs.se.home3d;

import android.content.Context;

import com.borqs.se.R;
import com.borqs.se.engine.SECameraData;
import com.borqs.se.engine.SEVector.SEVector3f;

public class HomeSceneInfo {
    // Below info should be initiated by theme xml in every theme and saved into
    // db
    // Below values are all default value
    public String mSceneName;
    public float mSkyRadius = 0;
    public float mWallWidth = 768;
    public float mWallHeight = 1013;
    public float mWallPaddingLeft = 0;
    public float mWallPaddingRight = 0;
    public float mWallPaddingTop = 0;
    public float mWallPaddingBottom = 45;
    public int mWallNum = 8;
    public float mWallRadius = 1000;
    public int mCellCountX = 4;
    public int mCellCountY = 4;
    public float mCellWidth = 189;
    public float mCellHeight = 231;
    public float mWidthGap = 4;
    public float mHeightGap = 15;
    public int mWallIndex = 0;

    public SECameraData mSECameraData;

    // moved from SESceneManager
    private int mSceneWidth = 0;
    private int mSceneHeight = 0;


    private ThemeInfo mThemeInfo;
    private Context mContext;

    // For application icon usage and some of them could be configured in the
    // scene.xml
    public float mAppsIconWidth;
    public int mAppIconTextSize;
    public int mAppIconPaddingTop = 20;
    public int mAppIconPaddingLeft = 26;
    public int mAppIconPaddingRight = 26;
    public int mAppIconPaddingBottom = 18;
    /** 下面四个值代表的意思是相机距离房间中心点的最小/大值以及相机在最小位置的张角和最大位置的张角 **/
    private float mMinCameraRadius;
    private float mMaxCameraRadius;
    private float mBestCameraRadius;
    
    private float mMinCameraFov;
    private float mMaxCameraFov;
    private float mBestCameraFov;
    
    private float mMinCameraDownUp;
    private float mMaxCameraDownUp;
    
    public HomeSceneInfo() {
        mContext = HomeManager.getInstance().getContext();
        Float fontScale = HomeUtils.getFontSize(mContext);
        mAppIconTextSize = (int) (fontScale * mContext.getResources().getDimensionPixelSize(
                R.dimen.app_customize_icon_text_size));
        mAppsIconWidth = mContext.getResources().getDimensionPixelSize(R.dimen.apps_customize_cell_width);
    }

    public void setThemeInfo(ThemeInfo themeInfo) {
        mSceneName = themeInfo.mSceneName;
        mWallIndex = themeInfo.mWallIndex;
        mSkyRadius = themeInfo.mSkyRadius;

        mWallWidth = themeInfo.mWallWidth;
        mWallHeight = themeInfo.mWallHeight;
        mWallPaddingTop = themeInfo.mWallPaddingTop;
        mWallPaddingBottom = themeInfo.mWallPaddingBottom;
        mWallPaddingLeft = themeInfo.mWallPaddingLeft;
        mWallPaddingRight = themeInfo.mWallPaddingRight;
        mWallNum = themeInfo.mWallNum;
        mWallRadius = themeInfo.mWallRadius;

        mCellCountX = themeInfo.mCellCountX;
        mCellCountY = themeInfo.mCellCountY;
        mCellWidth = themeInfo.mCellWidth;
        mCellHeight = themeInfo.mCellHeight;
        mWidthGap = themeInfo.mWidthGap;
        mHeightGap = themeInfo.mHeightGap;
        mSECameraData = themeInfo.mSECameraData;
        if (islandscape()) {
            mAppIconPaddingTop = 12;
            mAppIconPaddingLeft = 32;
            mAppIconPaddingRight = 32;
            mAppIconPaddingBottom = 12;
        }
        
        mMinCameraRadius = Math.abs(themeInfo.mNearestCameraLocation.getY());
        mMaxCameraRadius = Math.abs(themeInfo.mFarthestCameraLocation.getY());
        mBestCameraRadius= Math.abs(themeInfo.mBestCameraLocation.getY());

        mMinCameraFov = themeInfo.mNearestCameraFov;
        mMaxCameraFov = themeInfo.mFarthestCameraFov;
        mBestCameraFov = themeInfo.mBestCameraFov;

        mMinCameraDownUp = themeInfo.mBestCameraLocation.getZ() - 80;
        mMaxCameraDownUp = themeInfo.mBestCameraLocation.getZ() + 80;

        mThemeInfo = themeInfo;
    }

    public boolean islandscape() {
        return mWallWidth > mWallHeight;
    }

    /**
     * 计算出当前相机位置张角应该为多少合适,即相机位置变化时调整张角。
     */
    public float getCameraFovByRadius(float radius) {
        float curFov;
        if (radius < mBestCameraRadius) {
            curFov = (mBestCameraRadius - radius) * (mMinCameraFov - mBestCameraFov)
                    / (mBestCameraRadius - mMinCameraRadius) + mBestCameraFov;
        } else {
            curFov = (radius - mBestCameraRadius) * (mMaxCameraFov - mBestCameraFov)
                    / (mMaxCameraRadius - mBestCameraRadius) + mBestCameraFov;
        }
        return curFov;
    }

    public float getCameraMinRadius() {
        return mMinCameraRadius;
    }

    public float getCameraMaxRadius() {
        return mMaxCameraRadius;
    }

    public float getCameraMinDownUp() {
        return mMinCameraDownUp;
    }

    public float getCameraMaxDownUp() {
        return mMaxCameraDownUp;
    }

    public ThemeInfo getThemeInfo() {
        return mThemeInfo;
    }

    public void updateWallIndex(int index) {
        mThemeInfo.mWallIndex = index;
        mThemeInfo.updateWallIndex(HomeManager.getInstance().getContext());
    }

    public void updateCameraDataToDB(SEVector3f location, float fov) {
        mThemeInfo.updateCameraDataToDB(mContext, location, fov);
    }

    public void notifySurfaceChanged(int width, int height) {
        setSceneWidth(width);
        setSceneHeight(height);
    }

    // moved from SESceneManager
    public int getSceneWidth() {
        return mSceneWidth;
    }

    public int getSceneHeight() {
        return mSceneHeight;
    }

    public void setSceneWidth(int sceneWidth) {
        mSceneWidth = sceneWidth;
    }

    public void setSceneHeight(int sceneHeight) {
        mSceneHeight = sceneHeight;
    }


}
