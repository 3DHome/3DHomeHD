package com.borqs.se.unittests;

import android.test.AndroidTestCase;

import com.borqs.se.engine.SEVector.SEVector2f;

public class SEVectorTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSEVector2f() {
        float x = 0.0f;
        float y = 1.0f;
        SEVector2f vector2f = new SEVector2f();
        vector2f.set(x, y);
        assertEquals("test set and get x error", x, vector2f.getX());
        assertEquals("test set and get y error", y, vector2f.getY());

        SEVector2f vector2f2 = new SEVector2f(x, y);
        assertEquals("test equals error", true, vector2f.equals(vector2f2));
    }

    public void testSEVector2f_subtract() {
        float x = 1.0f;
        float y = 1.0f;
        SEVector2f vector2f = new SEVector2f(x, y);
        SEVector2f result = vector2f.subtract(vector2f);
        assertEquals("substrac, x error", x, vector2f.getX());
        assertEquals("substrac, y error", y, vector2f.getY());
        assertEquals("substrac, x result error", 0.0f, result.getX());
        assertEquals("substrac, x result error", 0.0f, result.getY());
    }

    public void testSEVector2f_selfSubtract() {
        float x = 1.0f;
        float y = 1.0f;
        SEVector2f vector2f = new SEVector2f(x, y);
        SEVector2f result = vector2f.selfSubtract(vector2f);
        assertEquals("self substrac, x error", 0.0f, vector2f.getX());
        assertEquals("self substrac, y error", 0.0f, vector2f.getY());
        assertEquals("self substrac, x result error", 0.0f, result.getX());
        assertEquals("self substrac, x result error", 0.0f, result.getY());
    }

    public void testSEVector2f_add() {
        float x = 1.0f;
        float y = 1.0f;
        SEVector2f vector2f = new SEVector2f(x, y);
        SEVector2f result = vector2f.add(vector2f);
        assertEquals("add, x error", x, vector2f.getX());
        assertEquals("add, y error", y, vector2f.getY());
        assertEquals("add, x result error", 2.0f, result.getX());
        assertEquals("add, x result error", 2.0f, result.getY());
    }

    public void testSEVector2f_selfAdd() {
        float x = 1.0f;
        float y = 1.0f;
        SEVector2f vector2f = new SEVector2f(x, y);
        SEVector2f result = vector2f.selfAdd(vector2f);
        assertEquals("self add, x error", 2.0f, vector2f.getX());
        assertEquals("self add, y error", 2.0f, vector2f.getY());
        assertEquals("self add, x result error", 2.0f, result.getX());
        assertEquals("self add, x result error", 2.0f, result.getY());
    }

    public void testSEVector2f_mulFloat() {
        float x = 1.0f;
        float y = 2.0f;
        float z = 2.0f;
        SEVector2f vector2f = new SEVector2f(x, y);
        SEVector2f result = vector2f.mul(z);
        assertEquals("mul float, x error", x, vector2f.getX());
        assertEquals("mul float, y error", y, vector2f.getY());
        assertEquals("mul float, x result error", x * z, result.getX());
        assertEquals("mul float, x result error", y * z, result.getY());
    }

    public void testSEVector2f_mulSEVetor2f() {
        float x = 1.0f;
        float y = 2.0f;
        SEVector2f z = new SEVector2f(y, y);
        SEVector2f vector2f = new SEVector2f(x, y);
        float result = vector2f.mul(z);
        assertEquals("mul SEVetor2f, x error", x, vector2f.getX());
        assertEquals("mul SEVetor2f, y error", y, vector2f.getY());
        assertEquals("mul SEVetor2f, result error", x * z.getX() + y * z.getY(), result);
    }

    public void testSEVector2f_selfMulFloat() {
        float x = 1.0f;
        float y = 2.0f;
        float z = 2.0f;
        SEVector2f vector2f = new SEVector2f(x, y);
        SEVector2f result = vector2f.selfMul(z);
        assertEquals("mul float, x error", x * z, vector2f.getX());
        assertEquals("mul float, y error", y * z, vector2f.getY());
        assertEquals("mul float, x result error", x * z, result.getX());
        assertEquals("mul float, x result error", y * z, result.getY());
    }

}
