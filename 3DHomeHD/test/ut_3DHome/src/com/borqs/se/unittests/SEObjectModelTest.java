package com.borqs.se.unittests;

import android.database.Cursor;
import android.test.AndroidTestCase;

import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.ProviderUtils;
import com.borqs.se.engine.SESceneInfo;

import java.util.Collection;
import java.util.HashMap;

/// test camera data from database
public class SEObjectModelTest extends AndroidTestCase {
    private static float MIN_THRESHOLD = 0.000005f;
    HashMap<String, ModelInfo> mAllModelInfoMap = new HashMap<String, ModelInfo>();
    private int mDbCount;

    protected void setupData() {
        Cursor cursor = ProviderUtils.queryAllModelInfo(getContext(), SESceneInfo.DEFAULT_SCENE_NAME);
        if (null != cursor) {
            mDbCount = cursor.getCount();
            ModelInfo.parseAllFromCursor(cursor, mAllModelInfoMap);
            cursor.close();
        } else {
            mDbCount = 0;
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
        mAllModelInfoMap.clear();
        mAllModelInfoMap = null;
    }

    /// all camera data should be existing.
    public void testExisting() {
        assertNotNull("test model info not null", mAllModelInfoMap);
        assertFalse("test model info not empty", mAllModelInfoMap.isEmpty());
    }

    /// all camera should have been located in the y-z face with x = 0
    public void testIdenticalName() {
        assertEquals("test every model has identical name", mDbCount, mAllModelInfoMap.size());
    }

    /// all camera should have been target in the y-z face with x = 0
    public void testModelType() {
        Collection<ModelInfo> allInfo = mAllModelInfoMap.values();
        for (ModelInfo info : allInfo) {
            assertTrue("test known model type of " + info.mName, ModelInfo.isKnownType(info.mType));
        }
    }

    /// all the axis should have been normalized, so each length is 1.
    public void testSlotType() {
        Collection<ModelInfo> allInfo = mAllModelInfoMap.values();
        for (ModelInfo info : allInfo) {
            assertTrue("TODO: test known slot type of " + info.mName, ModelInfo.isKnownSlotType(info.mSlotType));
        }
    }

    /// all the axis should have been normalized, so each length is 1.
    public void testPrimitiveType() {
        Collection<ModelInfo> allInfo = mAllModelInfoMap.values();
        for (ModelInfo info : allInfo) {
            assertTrue("TODO: test known slot type of " + info.mName, ModelInfo.isKnownPrimitiveType(info.mSlotType));
        }
    }

    /// all the axis should have been normalized, so each length is 1.
    public void testTargetVesselType() {
        Collection<ModelInfo> allInfo = mAllModelInfoMap.values();
        for (ModelInfo info : allInfo) {
            assertTrue("TODO: test known slot type of " + info.mName, ModelInfo.isKnownTargetVesselType(info.mSlotType));
        }
    }

    /// all the axis should have been normalized, so each length is 1.
    public void testKeyword() {
        Collection<ModelInfo> allInfo = mAllModelInfoMap.values();
        for (ModelInfo info : allInfo) {
            assertTrue("TODO: test known slot type of " + info.mName, ModelInfo.isKnownKeyword(info.mKeyWords));
        }
    }

}
