package com.borqs.se.engine;

import android.graphics.Bitmap;
import com.borqs.se.engine.SEVector.SERect3D;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEObjectData.IMAGE_TYPE;

public class SEObjectFactory {

    public static void createRectangle(SEObject obj, SERect3D rect, IMAGE_TYPE imageType, String imageName,
            String imagePath, Bitmap bitmap, float[] color, boolean blendingable) {
        SEObjectData data = new SEObjectData(obj.getName());
        data.setVertexArray(rect.getVertexArray());
        data.setFaceArray(rect.getFaceArray());
        data.setTexVertexArray(rect.getTexVertexArray());
        data.setImage(imageType, imageName, imagePath);
        data.setBitmap(bitmap);
        data.setColor(color);
        data.setBlendingable(blendingable);
        if (blendingable) {
            data.setNeedForeverBlend(true);
        }
        obj.setObjectData(data);
    }

    public static void createOpaqueRectangle(SEObject obj, SERect3D rect, String imageName, String imagePath) {
        SEObjectData data = new SEObjectData(obj.getName());
        data.setVertexArray(rect.getVertexArray());
        data.setFaceArray(rect.getFaceArray());
        data.setTexVertexArray(rect.getTexVertexArray());
        data.setImage(SEObjectData.IMAGE_TYPE_IMAGE, imageName, imagePath);
        data.setBlendingable(false);
        obj.setObjectData(data);
    }

    public static void createOpaqueRectangle(SEObject obj, SERect3D rect, float[] color) {
        SEObjectData data = new SEObjectData(obj.getName());
        data.setVertexArray(rect.getVertexArray());
        data.setFaceArray(rect.getFaceArray());
        data.setImage(SEObjectData.IMAGE_TYPE_COLOR, null, null);
        data.setColor(color);
        data.setBlendingable(false);
        obj.setObjectData(data);
    }

    public static void createOpaqueRectangle(SEObject obj, SERect3D rect, String imageName, String imageKey,
            SEBitmap bitmap) {
        SEObjectData data = new SEObjectData(obj.getName());
        data.setVertexArray(rect.getVertexArray());
        data.setFaceArray(rect.getFaceArray());
        data.setTexVertexArray(rect.getTexVertexArray());
        data.setImage(SEObjectData.IMAGE_TYPE_BITMAP, imageName, imageKey);
        data.setSEBitmap(bitmap);
        data.setBlendingable(false);
        obj.setObjectData(data);
    }

    public static void createRectangle(SEObject obj, SERect3D rect, String imageName, String imagePath) {
        SEObjectData data = new SEObjectData(obj.getName());
        data.setVertexArray(rect.getVertexArray());
        data.setFaceArray(rect.getFaceArray());
        data.setTexVertexArray(rect.getTexVertexArray());
        data.setImage(SEObjectData.IMAGE_TYPE_IMAGE, imageName, imagePath);
        data.setBlendingable(true);
        data.setNeedForeverBlend(true);
        obj.setObjectData(data);
    }

    public static void createRectangle(SEObject obj, SERect3D rect, String imageName, String imageKey, SEBitmap bitmap) {
        SEObjectData data = new SEObjectData(obj.getName());
        data.setVertexArray(rect.getVertexArray());
        data.setFaceArray(rect.getFaceArray());
        data.setTexVertexArray(rect.getTexVertexArray());
        data.setSEBitmap(bitmap);
        data.setImage(SEObjectData.IMAGE_TYPE_BITMAP, imageName, imageKey);
        data.setBlendingable(true);
        data.setNeedForeverBlend(true);
        obj.setObjectData(data);

    }

    public static void createWallObject(SEObject obj, String imageName, String imageKey, SEBitmap bitmap, int w, int h) {
        SEObjectData data = new SEObjectData(obj.getName());
        int rectNum = 16;
        float[] vertexArray = new float[rectNum * 12];
        for (int i = 0; i < rectNum; i++) {
            int column = i % 4;
            int row = i / 4;
            vertexArray[12 * i + 0] = column * w * 0.25f - w * 0.5f;
            vertexArray[12 * i + 1] = 0;
            vertexArray[12 * i + 2] = row * h * 0.25f - h * 0.5f;
            vertexArray[12 * i + 3] = (column + 1) * w * 0.25f - w * 0.5f;
            vertexArray[12 * i + 4] = 0;
            vertexArray[12 * i + 5] = row * h * 0.25f - h * 0.5f;
            vertexArray[12 * i + 6] = (column + 1) * w * 0.25f - w * 0.5f;
            vertexArray[12 * i + 7] = 0;
            vertexArray[12 * i + 8] = (row + 1) * h * 0.25f - h * 0.5f;
            vertexArray[12 * i + 9] = column * w * 0.25f - w * 0.5f;
            vertexArray[12 * i + 10] = 0;
            vertexArray[12 * i + 11] = (row + 1) * h * 0.25f - h * 0.5f;
        }
        data.setVertexArray(vertexArray);
        int[] faceArray = new int[rectNum * 6];
        for (int i = 0; i < rectNum; i++) {
            faceArray[6 * i + 0] = 4 * i;
            faceArray[6 * i + 1] = 4 * i + 1;
            faceArray[6 * i + 2] = 4 * i + 2;
            faceArray[6 * i + 3] = 4 * i + 2;
            faceArray[6 * i + 4] = 4 * i + 3;
            faceArray[6 * i + 5] = 4 * i;
        }
        data.setFaceArray(faceArray);
        float[] texVertexArray = new float[rectNum * 8];
        for (int i = 0; i < rectNum; i++) {
            int column = i % 4;
            int row = i / 4;
            texVertexArray[8 * i + 0] = column * 0.25f;
            texVertexArray[8 * i + 1] = row * 0.25f;
            texVertexArray[8 * i + 2] = (column + 1) * 0.25f;
            texVertexArray[8 * i + 3] = row * 0.25f;
            texVertexArray[8 * i + 4] = (column + 1) * 0.25f;
            texVertexArray[8 * i + 5] = (row + 1) * 0.25f;
            texVertexArray[8 * i + 6] = column * 0.25f;
            texVertexArray[8 * i + 7] = (row + 1) * 0.25f;
        }
        data.setTexVertexArray(texVertexArray);
        data.setSEBitmap(bitmap);
        data.setSEBitmap(bitmap);
        data.setImage(SEObjectData.IMAGE_TYPE_BITMAP, imageName, imageKey);
        data.setBlendingable(true);
        obj.setObjectData(data);
    }

    public static void createRectangle(SECamera camera, SEObject obj, String imageName, String imagePath, float scale) {
        SEVector3f xAxis = camera.getAxisX();
        SEVector3f yAxis = camera.getAxisY();
        SERect3D rect = new SERect3D(xAxis, yAxis);
        rect.setSize(camera.getWidth(), camera.getHeight(), scale);
        createRectangle(obj, rect, imageName, imagePath);
        SETransParas paras = new SETransParas();
        paras.mTranslate = camera.getScreenLocation(scale);
        obj.setLocalTransParas(paras);
    }

    public static void createRectangle(SECamera camera, SEObject obj, String imageName, String imageKey,
            SEBitmap bitmap, float scale) {
        SEVector3f xAxis = camera.getAxisX();
        SEVector3f yAxis = camera.getAxisY();
        SERect3D rect = new SERect3D(xAxis, yAxis);
        rect.setSize(camera.getWidth(), camera.getHeight(), scale);
        createRectangle(obj, rect, imageName, imageKey, bitmap);
        SETransParas paras = new SETransParas();
        paras.mTranslate = camera.getScreenLocation(scale);
        obj.setLocalTransParas(paras);
    }

    public static void createOpaqueRectangle(SECamera camera, SEObject obj, String imageName, String imagePath,
            float scale) {
        SEVector3f xAxis = camera.getAxisX();
        SEVector3f yAxis = camera.getAxisY();
        SERect3D rect = new SERect3D(xAxis, yAxis);
        rect.setSize(camera.getWidth(), camera.getHeight(), scale);
        createOpaqueRectangle(obj, rect, imageName, imagePath);
        SETransParas paras = new SETransParas();
        paras.mTranslate = camera.getScreenLocation(scale);
        obj.setLocalTransParas(paras);
    }

    public static void createCylinder(SEObject obj, int faceNum, float r, float h, float[] color) {
        createCylinder(obj, faceNum, r, h, SEObjectData.IMAGE_TYPE_COLOR, null, null, null, color);
    }

    public static void createCylinder(SEObject obj, int faceNum, float r, float h, String imageName, String imagePath) {
        createCylinder(obj, faceNum, r, h, SEObjectData.IMAGE_TYPE_IMAGE, imageName, imagePath, null, null);
    }

    public static void createCylinder(SEObject obj, int faceNum, float r, float h, String imageName, String imageKey,
            SEBitmap bitmap) {
        createCylinder(obj, faceNum, r, h, SEObjectData.IMAGE_TYPE_BITMAP, imageName, imageKey, bitmap, null);
    }

    public static void createCylinderII(SEObject obj, int faceNum, float r, float h, String imageName, String imageKey,
            SEBitmap bitmap) {
        createCylinderII(obj, faceNum, r, h, SEObjectData.IMAGE_TYPE_BITMAP, imageName, imageKey, bitmap, null);
    }

    private static void createCylinder(SEObject obj, int faceNum, float r, float h, int imageType, String imageName,
            String imageKey, SEBitmap bitmap, float[] color) {
        SEObjectData data = new SEObjectData(obj.getName());
        double perAngle = 2 * Math.PI / (faceNum - 1);
        float[] vertexArray = new float[faceNum * 2 * 3];
        for (int i = 0; i < faceNum; i++) {
            double angle = perAngle * i;
            vertexArray[3 * i] = (float) (r * Math.cos(angle));
            vertexArray[3 * i + 1] = (float) (r * Math.sin(angle));
            vertexArray[3 * i + 2] = -h / 2;

            vertexArray[3 * (faceNum + i)] = (float) (r * Math.cos(angle));
            vertexArray[3 * (faceNum + i) + 1] = (float) (r * Math.sin(angle));
            vertexArray[3 * (faceNum + i) + 2] = h / 2;
        }
        data.setVertexArray(vertexArray);
        int[] faceArray = new int[(faceNum - 1) * 2 * 3];
        for (int i = 0; i < faceNum - 1; i++) {
            faceArray[6 * i] = i;
            faceArray[6 * i + 1] = i + 1;
            faceArray[6 * i + 2] = i + 1 + faceNum;

            faceArray[6 * i + 3] = i + 1 + faceNum;
            faceArray[6 * i + 4] = i + faceNum;
            faceArray[6 * i + 5] = i;
        }
        data.setFaceArray(faceArray);
        float[] texVertexArray = new float[faceNum * 2 * 2];
        float perTex = 1.0f / (faceNum - 1);
        for (int i = 0; i < faceNum; i++) {
            float tex = perTex * i;
            texVertexArray[i * 2] = tex;
            texVertexArray[i * 2 + 1] = 0;

            texVertexArray[(faceNum + i) * 2] = tex;
            texVertexArray[(faceNum + i) * 2 + 1] = 1;
        }
        data.setTexVertexArray(texVertexArray);
        data.setImage(imageType, imageName, imageKey);
        data.setSEBitmap(bitmap);
        data.setColor(color);
        obj.setObjectData(data);
    }

    private static void createCylinderII(SEObject obj, int faceNum, float r, float h, int imageType, String imageName,
            String imageKey, SEBitmap bitmap, float[] color) {
        SEObjectData data = new SEObjectData(obj.getName());
        double perAngle = 2 * Math.PI / (faceNum - 1);
        float[] vertexArray = new float[faceNum * 2 * 3];
        for (int i = 0; i < faceNum; i++) {
            double angle = perAngle * i;
            vertexArray[3 * i] = (float) (r * Math.cos(angle));
            vertexArray[3 * i + 1] = (float) (r * Math.sin(angle));
            vertexArray[3 * i + 2] = -h / 2;

            vertexArray[3 * (faceNum + i)] = (float) (r * Math.cos(angle));
            vertexArray[3 * (faceNum + i) + 1] = (float) (r * Math.sin(angle));
            vertexArray[3 * (faceNum + i) + 2] = h / 2;
        }
        data.setVertexArray(vertexArray);
        int[] faceArray = new int[(faceNum - 1) * 2 * 3];
        for (int i = 0; i < faceNum - 1; i++) {
            faceArray[6 * i] = i;
            faceArray[6 * i + 1] = i + 1;
            faceArray[6 * i + 2] = i + 1 + faceNum;

            faceArray[6 * i + 3] = i + 1 + faceNum;
            faceArray[6 * i + 4] = i + faceNum;
            faceArray[6 * i + 5] = i;
        }
        data.setFaceArray(faceArray);
        float[] texVertexArray = new float[faceNum * 2 * 2];
        float perTex = 0.25f / (faceNum - 1);
        for (int i = 0; i < faceNum; i++) {
            float tex = perTex * i;
            texVertexArray[i * 2] = 0;
            texVertexArray[i * 2 + 1] = 0.75f + tex;

            texVertexArray[(faceNum + i) * 2] = 1;
            texVertexArray[(faceNum + i) * 2 + 1] = 0.75f + tex;
        }
        data.setTexVertexArray(texVertexArray);
        data.setImage(imageType, imageName, imageKey);
        data.setSEBitmap(bitmap);
        data.setColor(color);
        obj.setObjectData(data);
    }

    public static void createRibbon(SEObject obj, int length, float r, float h, float step, float[] color) {
        createRibbon(obj, r, h, length, step, SEObjectData.IMAGE_TYPE_COLOR, null, null, null, color);
    }

    public static void createRibbon(SEObject obj, int length, float r, float h, float step, String imageKey,
            String imageName, String imagePath) {
        createRibbon(obj, r, h, length, step, SEObjectData.IMAGE_TYPE_IMAGE, imageName, imagePath, null, null);
    }

    public static void createRibbon(SEObject obj, int length, float r, float h, float step, String imageName,
            String imageKey, SEBitmap bitmap) {
        createRibbon(obj, r, h, length, step, SEObjectData.IMAGE_TYPE_BITMAP, imageName, imageKey, bitmap, null);
    }

    private static void createRibbon(SEObject obj, float r, float h, int length, float step, int imageType,
            String imageName, String imageKey, SEBitmap bitmap, float[] color) {
        SEObjectData data = new SEObjectData(obj.getName());
        double perAngle = Math.PI / 180;
        float[] vertexArray = new float[length * 2 * 3];
        for (int i = 0; i < length; i++) {
            double angle = perAngle * i;
            vertexArray[3 * i] = (float) (r * Math.cos(angle));
            vertexArray[3 * i + 1] = (float) (r * Math.sin(angle));
            vertexArray[3 * i + 2] = -h / 2 - i * step;

            vertexArray[3 * (length + i)] = (float) (r * Math.cos(angle));
            vertexArray[3 * (length + i) + 1] = (float) (r * Math.sin(angle));
            vertexArray[3 * (length + i) + 2] = h / 2 - i * step;
        }
        data.setVertexArray(vertexArray);
        int[] faceArray = new int[(length - 1) * 2 * 3];
        for (int i = 0; i < length - 1; i++) {
            faceArray[6 * i] = i;
            faceArray[6 * i + 1] = i + 1;
            faceArray[6 * i + 2] = i + 1 + length;

            faceArray[6 * i + 3] = i + 1 + length;
            faceArray[6 * i + 4] = i + length;
            faceArray[6 * i + 5] = i;
        }
        data.setFaceArray(faceArray);
        float[] texVertexArray = new float[length * 2 * 2];
        float perTex = 1.0f / (length - 1);
        for (int i = 0; i < length; i++) {
            float tex = perTex * i;
            texVertexArray[i * 2] = tex;
            texVertexArray[i * 2 + 1] = 0;

            texVertexArray[(length + i) * 2] = tex;
            texVertexArray[(length + i) * 2 + 1] = 1;
        }
        data.setTexVertexArray(texVertexArray);
        data.setImage(imageType, imageName, imageKey);
        data.setSEBitmap(bitmap);
        data.setColor(color);
        obj.setObjectData(data);
    }

    public static void createTest(SEObject obj, float r, float[] color) {
        SEObjectData data = new SEObjectData(obj.getName());
        float[] vertexArray = new float[13 * 3];
        vertexArray[0] = 0;
        vertexArray[1] = 0;
        vertexArray[2] = 20;
        for (int i = 1; i < 13; i++) {
            double angle = (i - 1) * Math.PI / 6 + Math.PI / 12;
            vertexArray[3 * i] = (float) (r * Math.cos(angle));
            vertexArray[3 * i + 1] = (float) (r * Math.sin(angle));
            vertexArray[3 * i + 2] = 20;
        }
        data.setVertexArray(vertexArray);
        int[] faceArray = new int[6 * 3];
        for (int i = 0; i < 6; i++) {
            faceArray[3 * i] = 0;
            faceArray[3 * i + 1] = 2 * i + 1;
            faceArray[3 * i + 2] = 2 * i + 2;
        }
        data.setFaceArray(faceArray);
        data.setColor(color);
        obj.setObjectData(data);
    }

    public static void createCircle(SEObject obj, int faceNum, float r, String imageName, String imagePath) {
        createCircle(obj, faceNum, r, SEObjectData.IMAGE_TYPE_IMAGE, imageName, imagePath, null);
    }

    public static void createCircle(SEObject obj, int faceNum, float r, String imageName, String imageKey,
            SEBitmap bitmap) {
        createCircle(obj, faceNum, r, SEObjectData.IMAGE_TYPE_BITMAP, imageName, imageKey, bitmap);
    }

    private static void createCircle(SEObject obj, int faceNum, float r, int imageType, String imageName,
            String imageKey, SEBitmap bitmap) {
        SEObjectData data = new SEObjectData(obj.getName());
        double perAngle = 2 * Math.PI / (faceNum - 1);
        float[] vertexArray = new float[faceNum * 3];
        for (int i = 0; i < faceNum; i++) {
            double angle = perAngle * i;
            if (i == faceNum - 1) {
                vertexArray[3 * i] = 0;
                vertexArray[3 * i + 1] = 0;
                vertexArray[3 * i + 2] = 0;
                break;
            }
            vertexArray[3 * i] = (float) (r * Math.cos(angle));
            vertexArray[3 * i + 1] = (float) (r * Math.sin(angle));
            vertexArray[3 * i + 2] = 0;
        }
        data.setVertexArray(vertexArray);
        int[] faceArray = new int[(faceNum - 1) * 3];
        for (int i = 0; i < faceNum - 1; i++) {
            faceArray[3 * i] = i;
            faceArray[3 * i + 1] = i + 1;
            faceArray[3 * i + 2] = faceNum - 1;
        }
        data.setFaceArray(faceArray);
        float[] texVertexArray = new float[faceNum * 2];
        for (int i = 0; i < faceNum; i++) {
            double angle = perAngle * i;
            if (i == faceNum - 1) {
                texVertexArray[2 * i] = 0.5f;
                texVertexArray[2 * i + 1] = 0.5f;
                break;
            }

            texVertexArray[2 * i] = (float) (0.5f * Math.cos(angle)) + 0.5f;
            texVertexArray[2 * i + 1] = (float) (0.5f * Math.sin(angle)) + 0.5f;
        }
        data.setTexVertexArray(texVertexArray);
        data.setImage(imageType, imageName, imageKey);
        data.setSEBitmap(bitmap);
        obj.setObjectData(data);
    }

    public static void createUnitSphere(SEObject obj, int spanXY, int spanZ, float r, String imageName, String imagePath) {
        createUnitSphere(obj, spanXY, spanZ, r, SEObjectData.IMAGE_TYPE_IMAGE, imageName, imagePath, null);
    }

    public static void createUnitSphere(SEObject obj, int spanXY, int spanZ, float r, String imageName,
            String imageKey, SEBitmap bitmap) {
        createUnitSphere(obj, spanXY, spanZ, r, SEObjectData.IMAGE_TYPE_BITMAP, imageName, imageKey, bitmap);
    }

    private static void createUnitSphere(SEObject obj, int spanXY, int spanZ, float r, int imageType, String imageName,
            String imageKey, SEBitmap bitmap) {
        double DTOR = Math.PI / 180;
        int faceSizeXY = 360 / spanXY;
        int faceSizeZ = 180 / spanZ;
        int faceSize = faceSizeXY * faceSizeZ;
        float[] vertexArray = new float[faceSize * 12];
        float[] texVertexArray = new float[faceSize * 8];
        int[] faceArray = new int[faceSize * 6];
        for (int z = 0; z < faceSizeZ; z++) {
            for (int xy = 0; xy < faceSizeXY; xy++) {
                int n = 12 * (z * faceSizeXY + xy);
                double theta = -90 + z * spanZ;
                double phi = xy * spanXY;
                vertexArray[n] = (float) (r * Math.cos(theta * DTOR) * Math.cos(phi * DTOR));
                vertexArray[n + 1] = (float) (r * Math.cos(theta * DTOR) * Math.sin(phi * DTOR));
                vertexArray[n + 2] = (float) (r * Math.sin(theta * DTOR));

                vertexArray[n + 3] = (float) (r * Math.cos(theta * DTOR) * Math.cos((phi + spanXY) * DTOR));
                vertexArray[n + 4] = (float) (r * Math.cos(theta * DTOR) * Math.sin((phi + spanXY) * DTOR));
                vertexArray[n + 5] = (float) (r * Math.sin(theta * DTOR));

                vertexArray[n + 6] = (float) (r * Math.cos((theta + spanZ) * DTOR) * Math.cos((phi + spanXY) * DTOR));
                vertexArray[n + 7] = (float) (r * Math.cos((theta + spanZ) * DTOR) * Math.sin((phi + spanXY) * DTOR));
                vertexArray[n + 8] = (float) (r * Math.sin((theta + spanZ) * DTOR));

                vertexArray[n + 9] = (float) (r * Math.cos((theta + spanZ) * DTOR) * Math.cos(phi * DTOR));
                vertexArray[n + 10] = (float) (r * Math.cos((theta + spanZ) * DTOR) * Math.sin(phi * DTOR));
                vertexArray[n + 11] = (float) (r * Math.sin((theta + spanZ) * DTOR));

                n = 8 * (z * faceSizeXY + xy);
                // texVertexArray[n] = (float) xy / faceSizeXY;
                // texVertexArray[n + 1] = (float) z / faceSizeZ;
                //
                // texVertexArray[n + 2] = (float) (xy + 1) / faceSizeXY;
                // texVertexArray[n + 3] = (float) z / faceSizeZ;
                //
                // texVertexArray[n + 4] = (float) (xy + 1) / faceSizeXY;
                // texVertexArray[n + 5] = (float) (z + 1) / faceSizeZ;
                //
                // texVertexArray[n + 6] = (float) xy / faceSizeXY;
                // texVertexArray[n + 7] = (float) (z + 1) / faceSizeZ;
                if (z >= faceSizeZ / 2) {
                    texVertexArray[n] = (float) (0.5 + Math.cos(theta * DTOR) * Math.cos(phi * DTOR) * 0.28);
                    texVertexArray[n + 1] = (float) (0.5 + Math.cos(theta * DTOR) * Math.sin(phi * DTOR) * 0.28);

                    texVertexArray[n + 2] = (float) (0.5 + Math.cos(theta * DTOR) * Math.cos((phi + spanXY) * DTOR)
                            * 0.28);
                    texVertexArray[n + 3] = (float) (0.5 + Math.cos(theta * DTOR) * Math.sin((phi + spanXY) * DTOR)
                            * 0.28);

                    texVertexArray[n + 4] = (float) (0.5 + Math.cos((theta + spanZ) * DTOR)
                            * Math.cos((phi + spanXY) * DTOR) * 0.28);
                    texVertexArray[n + 5] = (float) (0.5 + Math.cos((theta + spanZ) * DTOR)
                            * Math.sin((phi + spanXY) * DTOR) * 0.28);

                    texVertexArray[n + 6] = (float) (0.5 + Math.cos((theta + spanZ) * DTOR) * Math.cos(phi * DTOR)
                            * 0.28);
                    texVertexArray[n + 7] = (float) (0.5 + Math.cos((theta + spanZ) * DTOR) * Math.sin(phi * DTOR)
                            * 0.28);
                } else {
                    texVertexArray[n] = (float) (0.5 - Math.cos(theta * DTOR) * Math.cos(phi * DTOR) * 0.28);
                    texVertexArray[n + 1] = (float) (0.5 + Math.cos(theta * DTOR) * Math.sin(phi * DTOR) * 0.28);

                    texVertexArray[n + 2] = (float) (0.5 - Math.cos(theta * DTOR) * Math.cos((phi + spanXY) * DTOR)
                            * 0.28);
                    texVertexArray[n + 3] = (float) (0.5 + Math.cos(theta * DTOR) * Math.sin((phi + spanXY) * DTOR)
                            * 0.28);

                    texVertexArray[n + 4] = (float) (0.5 - Math.cos((theta + spanZ) * DTOR)
                            * Math.cos((phi + spanXY) * DTOR) * 0.28);
                    texVertexArray[n + 5] = (float) (0.5 + Math.cos((theta + spanZ) * DTOR)
                            * Math.sin((phi + spanXY) * DTOR) * 0.28);

                    texVertexArray[n + 6] = (float) (0.5 - Math.cos((theta + spanZ) * DTOR) * Math.cos(phi * DTOR)
                            * 0.28);
                    texVertexArray[n + 7] = (float) (0.5 + Math.cos((theta + spanZ) * DTOR) * Math.sin(phi * DTOR)
                            * 0.28);
                }

                n = 6 * (z * faceSizeXY + xy);
                int index = 4 * (z * faceSizeXY + xy);
                faceArray[n] = index;
                faceArray[n + 1] = index + 1;
                faceArray[n + 2] = index + 2;
                faceArray[n + 3] = index + 2;
                faceArray[n + 4] = index + 3;
                faceArray[n + 5] = index;
            }
        }
        SEObjectData data = new SEObjectData(obj.getName());
        data.setVertexArray(vertexArray);
        data.setFaceArray(faceArray);
        data.setTexVertexArray(texVertexArray);
        data.setImage(imageType, imageName, imageKey);
        data.setSEBitmap(bitmap);
        obj.setObjectData(data);
    }
}
