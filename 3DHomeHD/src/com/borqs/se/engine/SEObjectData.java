package com.borqs.se.engine;

import com.borqs.se.home3d.HomeUtils;

import android.graphics.Bitmap;

public class SEObjectData {
    public static final int SE_DEFAULT_SHADER = 0;
    public static final int SE_LIGHTMAP_SHADER = 1;
    public static final int SE_PLUGINLIGHT_SHADER = 2;

    public static final int OBJECT_TYPE_TRIANGLE = 0;
    public static final int OBJECT_TYPE_LINE = 1;

    public static final int IMAGE_TYPE_IMAGE = 0;
    public static final int IMAGE_TYPE_BITMAP = 1;
    public static final int IMAGE_TYPE_COLOR = 2;
    public enum IMAGE_TYPE {
        IMAGE, BITMAP, COLOR
    }

    public static final int NEED_BLENDING = 0x00000001;
    public static final int NEED_DEPTH_TEST = 0x00000002;
    public static final int NEED_CULL_FACE = 0x00000004;
    public static final int NEED_ALPHA_TEST = 0x00000008;
    public static final int VISIBLE = 0x00000010;
    public static final int IS_MINI_BOX = 0x00000020;
    public static final int NEED_FOREVER_BLEND = 0x00000040;
    public String mObjectName;
    private String mTextureImageKey;
    private String mTextureImageName;
    private int mShaderIndex;
    private float[] mVertexArray;
    private float[] mTexVertexArray;
    private int[] mFaces;
    private float[] mColor = { 0, 0, 0 };
    private float mAlpha;
    private int mBVType;
    private SEBitmap mBitmap;
    private int mRenderState;
    public SETransParas mLocalTransParas;
    public SETransParas mUserTransParas;
    private int mImageWidth;
    private int mImageHeight;
    private boolean mHasResizeImage;
    private int mObjectType;
    public IMAGE_TYPE mImageType;
    private float mLineWidth;
    private int mLayerIndex;

    public SEObjectData(String name) {
        mObjectName = name;
        mAlpha = 1.0f;
        mShaderIndex = SE_DEFAULT_SHADER;
        mRenderState = VISIBLE + NEED_DEPTH_TEST;
        mLocalTransParas = new SETransParas();
        mUserTransParas = new SETransParas();
        mBVType = 1;
        mObjectType = OBJECT_TYPE_TRIANGLE;
        mLineWidth = 0;
        mLayerIndex = 0;
        mHasResizeImage = true;
    }

    public void setObjectType(int type) {
        mObjectType = type;
    }

    public int getObjectType() {
        return mObjectType;
    }

    public void setLineWidth(int lineWidth) {
        mLineWidth = lineWidth;
    }

    public float getLineWidth() {
        return mLineWidth;
    }

    public void setLayerIndex(int layerIndex) {
        mLayerIndex = layerIndex;
    }

    public void setImageSize(int w, int h) {
        mImageWidth = w;
        mImageHeight = h;
        if (HomeUtils.isPower2(mImageWidth) && HomeUtils.isPower2(mImageHeight)) {
            mHasResizeImage = true;
        } else {
            mHasResizeImage = false;
        }
    }

    public void setBlendingable(boolean blend) {
        if (blend) {
            mRenderState |= NEED_BLENDING;
        } else {
            mRenderState &= ~NEED_BLENDING;
        }
    }

    public void setNeedForeverBlend(boolean need) {
        if (need) {
            mRenderState |= NEED_FOREVER_BLEND;
        } else {
            mRenderState &= ~NEED_FOREVER_BLEND;
        }
    }

    public void setNeedDepthTest(boolean depthTest) {
        if (depthTest) {
            mRenderState |= NEED_DEPTH_TEST;
        } else {
            mRenderState &= ~NEED_DEPTH_TEST;
        }
    }

    public void setNeedCullFace(boolean cullFace) {
        if (cullFace) {
            mRenderState |= NEED_CULL_FACE;
        } else {
            mRenderState &= ~NEED_CULL_FACE;
        }
    }

    public void setNeedAlphaTest(boolean alphaTest) {
        if (alphaTest) {
            mRenderState |= NEED_ALPHA_TEST;
        } else {
            mRenderState &= ~NEED_ALPHA_TEST;
        }
    }

    public void setVisible(boolean visible) {
        if (visible) {
            mRenderState |= VISIBLE;
        } else {
            mRenderState &= ~VISIBLE;
        }
    }

    public void setIsMiniBox(boolean miniBox) {
        if (miniBox) {
            mRenderState |= IS_MINI_BOX;
        } else {
            mRenderState &= ~IS_MINI_BOX;
        }
    }

    public boolean isCullFaceOpend() {
        return (mRenderState & NEED_CULL_FACE) != 0;
    }

    public boolean isBlendingable() {
        return (mRenderState & NEED_BLENDING) != 0;
    }

    public boolean isDepthTestOpened() {
        return (mRenderState & NEED_DEPTH_TEST) != 0;
    }

    public boolean isNeedAlphaTest() {
        return (mRenderState & NEED_ALPHA_TEST) != 0;
    }

    public boolean isVisible() {
        return (mRenderState & VISIBLE) != 0;
    }

    public void setBVType(int type) {
        mBVType = type;
    }

    public int getBVType() {
        return mBVType;
    }

    public void setRenderState(int renderState) {
        mRenderState = renderState;
    }

    public int getRenderState() {
        return mRenderState;
    }

    public void setSEBitmap(SEBitmap bitmap) {
        mBitmap = bitmap;
    }

    public SEBitmap getSEBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = new SEBitmap(bitmap, SEBitmap.Type.normal);
    }

    public Bitmap getBitmap() {
        if (mBitmap != null) {
            return mBitmap.getBitmap();
        }
        return null;
    }

    public void setAlpha(float alpha) {
        mAlpha = alpha;
    }

    public float getAlpha() {
        return mAlpha;
    }

    public void setShaderIndex(int index) {
        mShaderIndex = index;
    }

    public int getShaderIndex() {
        return mShaderIndex;
    }

    private String getShaderType() {
        return getShaderType(mShaderIndex);
    }

    public static String getShaderType(int type) {
        switch (type) {
        case 1:
            return "lightmap_shader";
        case 2:
            return "SE_PluginLightShader";
        default:
            return "default_shader";
        }
    }

    public static int getShaderIndex(String Shader) {
        if (Shader.equals("lightmap_shader")) {
            return 1;
        } else if (Shader.equals("SE_PluginLightShader")) {
            return 2;
        } else {
            return 0;
        }
    }

    private String getRenderType() {
        return getRenderType(mShaderIndex);
    }

    public static String getRenderType(int type) {
        switch (type) {
        case 1:
            return "lightmap_renderer";
        case 2:
            return "SE_PluginLightRender";
        default:
            return "default_renderer";

        }
    }

    public void setColor(float[] color) {
        mColor = color;
    }

    public float[] getColor() {
        return mColor;
    }

    public void setVertexArray(float[] vertex) {
        mVertexArray = vertex;
    }

    public float[] getVertexArray() {
        return mVertexArray;
    }

    public void setTexVertexArray(float[] texVertex) {
        mTexVertexArray = texVertex;
    }

    public float[] getTexVertexArray() {
        if (!mHasResizeImage) {
            mHasResizeImage = true;
            int power2Width = HomeUtils.higherPower2(mImageWidth);
            int power2Height = HomeUtils.higherPower2(mImageHeight);
            float starty = (power2Height - mImageHeight) * 0.5f;
            float startx = (power2Width - mImageWidth) * 0.5f;
            int textureCoorNum = mTexVertexArray.length / 2;
            for (int i = 0; i < textureCoorNum; i++) {
                mTexVertexArray[2 * i] = (startx + mTexVertexArray[2 * i] * mImageWidth) / power2Width;
                mTexVertexArray[2 * i + 1] = (starty + mTexVertexArray[2 * i + 1] * mImageHeight) / power2Height;
            }
        }
        return mTexVertexArray;
    }

    public void setFaceArray(int[] faces) {
        mFaces = faces;
    }

    public int[] getFaceArray() {
        return mFaces;
    }

    public void setImage(int imageType, String imageName, String ImageKey) {
        mTextureImageName = imageName;
        mTextureImageKey = ImageKey;       
        mImageType = IMAGE_TYPE.values()[imageType];
    }

    public void setImage(IMAGE_TYPE imageType, String imageName, String ImageKey) {
        mTextureImageName = imageName;
        mTextureImageKey = ImageKey;
        mImageType = imageType;
    }

    public String getImageKey() {
        return mTextureImageKey;
    }

    public String getImageName() {
        return mTextureImageName;
    }

    public int getImageType() {
        return mImageType.ordinal();
//        switch (mImageType) {
//        case IMAGE:
//            return 0;
//        case BITMAP:
//            return 1;
//        case COLOR:
//            return 2;
//        }
//        return 0;
    }
}
