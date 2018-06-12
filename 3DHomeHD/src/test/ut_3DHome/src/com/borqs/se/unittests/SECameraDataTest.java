package com.borqs.se.unittests;

import android.database.Cursor;
import android.test.AndroidTestCase;

import com.borqs.framework3d.home3d.HouseSceneInfo;
import com.borqs.se.home3d.ProviderUtils;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SECameraData;
import com.borqs.se.engine.SESceneInfo;

import java.util.ArrayList;
import java.util.HashMap;

/// test camera data from database
public class SECameraDataTest extends AndroidTestCase {
    private static float MIN_THRESHOLD = 0.000005f;

    HouseSceneInfo mHouseInfo;

    SECameraData mDefault;
    SECameraData mUp;
    SECameraData mDown;
    SECameraData mNear;
    SECameraData mFar;

    protected void setupData() {
        Cursor cursor = ProviderUtils.querySceneInfo(getContext(), SESceneInfo.DEFAULT_SCENE_NAME);
        if (null != cursor) {
            ArrayList<SECameraData> dataList = new ArrayList<SECameraData>();
            HashMap<String, Integer> indexMap = new HashMap<String, Integer>();

            cursor.moveToFirst();
            SECamera.parseCameraData(cursor, dataList, indexMap);

            mHouseInfo = new HouseSceneInfo();
            mHouseInfo.parseFromCursor(cursor);
            cursor.close();

            mDefault = dataList.get(indexMap.get(SECameraData.CAMERA_TYPE_DEFAULT));
            mUp = dataList.get(indexMap.get(SECameraData.CAMERA_TYPE_UP));
            mDown = dataList.get(indexMap.get(SECameraData.CAMERA_TYPE_DOWN));
            mNear = dataList.get(indexMap.get(SECameraData.CAMERA_TYPE_NEAR));
            mFar = dataList.get(indexMap.get(SECameraData.CAMERA_TYPE_FAR));
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setupData();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mDefault = null;
        mUp = null;
        mDown = null;
        mNear = null;
        mFar = null;
    }

    /// all camera data should be existing.
    public void testExisting() {
        assertNotNull("test DEFAULT camera not null", mDefault);
        assertNotNull("test UP camera not null", mUp);
        assertNotNull("test DOWN camera not null", mDown);
        assertNotNull("test NEAR camera not null", mNear);
        assertNotNull("test FAR camera not null", mFar);
    }

    /// all camera should have been located in the y-z face with x = 0
    public void testLocationFace() {
        assertEquals("test x of DEFAULT camera location", 0f, mDefault.mLocation.getX());
        assertEquals("test x of UP camera location", 0f, mUp.mLocation.getX());
        assertEquals("test x of DOWN camera location", 0f, mDown.mLocation.getX());
        assertEquals("test x of NEAR camera location", 0f, mNear.mLocation.getX());
        assertEquals("test x of FAR camera location", 0f, mFar.mLocation.getX());
    }

    /// all camera should have been target in the y-z face with x = 0
    public void testTargetPositionFace() {
        assertEquals("test x of DEFAULT camera target", 0f, mDefault.mTargetPos.getX());
        assertEquals("test x of UP camera target", 0f, mUp.mTargetPos.getX());
        assertEquals("test x of DOWN camera target", 0f, mDown.mTargetPos.getX());
        assertEquals("test x of NEAR camera target", 0f, mNear.mTargetPos.getX());
        assertEquals("test x of FAR camera target", 0f, mFar.mTargetPos.getX());
    }

    /// all the axis should have been normalized, so each length is 1.
    public void testAxisNormalized() {
        assertTrue("test normalized axis of DEFAULT camera", Math.abs(mDefault.mAxisZ.getLength() - 1) < MIN_THRESHOLD);
        assertTrue("test normalized axis of UP camera", Math.abs(mUp.mAxisZ.getLength() - 1) < MIN_THRESHOLD);
        assertTrue("test normalized axis of DOWN camera", Math.abs(mDown.mAxisZ.getLength() - 1) < MIN_THRESHOLD);
        assertTrue("test normalized axis of NEAR camera", Math.abs(mNear.mAxisZ.getLength() - 1) < MIN_THRESHOLD);
        assertTrue("test normalized axis of FAR camera", Math.abs(mFar.mAxisZ.getLength() - 1) < MIN_THRESHOLD);
    }

    /// DEFAULT， NEAR， FAR are horizontal
    public void testHorizontal() {
        assertEquals("test horizontal axis of DEFAULT camera", 0f, mDefault.mAxisZ.getZ());
        assertEquals("test horizontal axis of NEAR camera", 0f, mNear.mAxisZ.getZ());
        assertEquals("test horizontal axis of FAR camera", 0f, mFar.mAxisZ.getZ());
        assertEquals("test horizontal position of DEFAULT camera", mDefault.mTargetPos.getZ(), mDefault.mLocation.getZ());
        assertEquals("test horizontal position of NEAR camera", mNear.mTargetPos.getZ(), mNear.mLocation.getZ());
        assertEquals("test horizontal position of FAR camera", mFar.mTargetPos.getZ(), mFar.mLocation.getZ());
    }

    public void testHorizontalRelative() {
        assertTrue("test DEFAULT is far than NEAR", mDefault.mLocation.getY() < mNear.mLocation.getY());
        assertTrue("test DEFAULT is near than FAR", mDefault.mLocation.getY() > mFar.mLocation.getY());
    }

    /// all camera locate at the same side of the x-z side with y = 0
    public void testSameYSide() {
        assertTrue("test NEAR locates at the same Y side of DEFAULT.", mDefault.mLocation.getY() * mNear.mLocation.getY() > 0);
        assertTrue("test FAR locates at the same Y side of DEFAULT.", mDefault.mLocation.getY() * mFar.mLocation.getY() > 0);
        assertTrue("test UP locates at the same Y side of DEFAULT.", mDefault.mLocation.getY() * mUp.mLocation.getY() > 0);
        assertTrue("test DOWN locates at the same Y side of DEFAULT.", mDefault.mLocation.getY() * mDefault.mLocation.getY() > 0);
    }

    /// The near threshold of camera must cut the near wall side.
    private static final int DEPTH_RANGE = 5000;
    public void testValidViewDepth() {
        assertTrue("test view depth of DEFAULT.", DEPTH_RANGE > mDefault.mFar - mDefault.mNear);
        assertTrue("test view depth of NEAR.", DEPTH_RANGE > mNear.mFar - mNear.mNear);
        assertTrue("test view depth of FAR.", DEPTH_RANGE > mFar.mFar - mFar.mNear);
        assertTrue("test view depth of UP.", DEPTH_RANGE > mUp.mFar - mUp.mNear);
        assertTrue("test view depth of DOWN.", DEPTH_RANGE > mDown.mFar - mDown.mNear);
    }

    /// The near wall should not locate in any camera.
    public void testNearWallLimit() {
        final float radius = -mHouseInfo.getHouseRadius();
        assertTrue("test near wall limit of DEFAULT.", radius < mDefault.mLocation.getY() + mDefault.mNear);
        assertTrue("test near wall limit NEAR.", radius < mNear.mLocation.getY() + mNear.mNear);
        assertTrue("test near wall limit of FAR.", radius < mFar.mLocation.getY() + mFar.mNear);
        assertTrue("test near wall limit of UP.", radius < mUp.mLocation.getY() + mUp.mNear);
        assertTrue("test near wall limit of DOWN.", radius < mDown.mLocation.getY() + mDown.mNear);
    }

    /// The near wall should not locate in any camera.
    public void testFarWallLimit() {
        final float radius = mHouseInfo.getHouseRadius();
        assertTrue("test near wall limit of DEFAULT.", radius < mDefault.mLocation.getY() + mDefault.mFar);
        assertTrue("test near wall limit NEAR.", radius < mNear.mLocation.getY() + mNear.mFar);
        assertTrue("test near wall limit of FAR.", radius < mFar.mLocation.getY() + mFar.mFar);
        assertTrue("test near wall limit of UP.", radius < mUp.mLocation.getY() + mUp.mFar);
        assertTrue("test near wall limit of DOWN.", radius < mDown.mLocation.getY() + mDown.mFar);
    }
}
