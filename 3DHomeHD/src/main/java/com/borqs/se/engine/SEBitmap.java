package com.borqs.se.engine;

import android.graphics.Bitmap;

public class SEBitmap {
    public enum Type {
        normal, needSaveToDB
    }

    private Bitmap mBitmap;
    private int mRef;

    /**
     * @param type
     *            Need to use the bitmap once(load it to engine), if type ==
     *            normal; Need to use the bitmap twice(save it to DB and load it
     *            to engine), if type == needSaveToDB
     */
    public SEBitmap(Bitmap bitmap, Type type) {
        mBitmap = bitmap;
        if (type == Type.normal) {
            mRef = 1;
        } else if (type == Type.needSaveToDB) {
            mRef = 2;
        }
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public int getWidth() {
        return mBitmap.getWidth();
    }

    public int getHeight() {
        return mBitmap.getHeight();
    }

    public void recycle() {
        mRef--;
        if (mRef == 0 && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
    }

}
