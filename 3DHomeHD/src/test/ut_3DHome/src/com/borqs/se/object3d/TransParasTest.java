package com.borqs.se.object3d;

import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SETransParas;

import android.test.AndroidTestCase;

public class TransParasTest extends AndroidTestCase {

    private float[] mTestData;
    private SEVector3f mTestTranslate;
    private SERotate mTestRotate;
    private SEVector3f mTestScale;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTestData = new float [] {1.0f, 1.0f, 1.0f, 2.0f, 2.0f, 2.0f, 2.0f, 3.0f, 3.0f, 3.0f};
        mTestTranslate = new SEVector3f(mTestData[0], mTestData[1], mTestData[2]);
        mTestRotate = new SERotate(mTestData[3], mTestData[4], mTestData[5], mTestData[6]);
        mTestScale = new SEVector3f(mTestData[7], mTestData[8], mTestData[9]);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testInit() {
        SETransParas paras = new SETransParas();

        paras.init(mTestData);

        assertTrue("init translate error", mTestTranslate.equals(paras.mTranslate));
        assertTrue("init rotate error", mTestRotate.equals(paras.mRotate));
        assertTrue("init rotate error", mTestScale.equals(paras.mScale));
    }

    public void testGetTranslate() {
        SETransParas paras = new SETransParas(mTestTranslate, mTestRotate, mTestScale);

        assertEquals("test get translate x failed", mTestTranslate.getX(), paras.getTranslate()[0]);
        assertEquals("test get translate y failed", mTestTranslate.getY(), paras.getTranslate()[1]);
        assertEquals("test get translate z failed", mTestTranslate.getZ(), paras.getTranslate()[2]);
    }

    public void getGetRotate() {
        SETransParas paras = new SETransParas(mTestTranslate, mTestRotate, mTestScale);

        assertEquals("test get rotate angle failed", mTestRotate.getAngle(), paras.getTranslate()[0]);
        assertEquals("test get rotate x failed", mTestRotate.getX(), paras.getTranslate()[1]);
        assertEquals("test get rotate y failed", mTestRotate.getY(), paras.getTranslate()[2]);
        assertEquals("test get rotate z failed", mTestRotate.getZ(), paras.getTranslate()[3]);
    }

    public void testGetScale() {
        SETransParas paras = new SETransParas(mTestTranslate, mTestRotate, mTestScale);

        assertEquals("test get scale x failed", mTestScale.getX(), paras.geScale()[0]);
        assertEquals("test get scale y failed", mTestScale.getY(), paras.geScale()[1]);
        assertEquals("test get scale z failed", mTestScale.getZ(), paras.geScale()[2]);
    }
}
