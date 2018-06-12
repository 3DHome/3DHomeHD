package com.borqs.se.home3d;

import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERect3D;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEVector.SEVector2f;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectFactory;
import com.borqs.se.engine.SESceneManager;

public class DelayFrame extends SEObject {

    public DelayFrame(SEScene scene) {
        super(scene, "DelayFrameObject", 0);
    }

    @Override
    public void onRender(SECamera camera) {
        super.onRender(camera);
        SERect3D rect = new SERect3D();
        rect.setSize(camera.getWidth(), camera.getHeight(), 0.1f);
        SEObjectFactory.createOpaqueRectangle(this, rect, mName + "_image", SESceneManager.BACKGROUND_IMAGE_KEY);
        setImageSize(camera.getWidth(), camera.getHeight());
        setTranslate(camera.getScreenLocation(0.1f), false);
        SEVector3f yAxis = getCamera().getAxisY();
        SEVector2f yAxis2f = new SEVector2f(yAxis.getZ(), yAxis.getY());
        float angle = (float) (180 * yAxis2f.getAngle() / Math.PI);
        setRotate(new SERotate(-angle, 1, 0, 0), false);
        setVisible(false, false);
    }

    protected void render() {
        onRender(getCamera());
        addUserObject_JNI();
        applyImage_JNI(mName + "_image", SESceneManager.BACKGROUND_IMAGE_KEY);
        onRenderFinish(getCamera());
    }

    public void show(SEAnimFinishListener l, boolean show) {
        if (show) {
            SEVector3f yAxis = getCamera().getAxisY();
            SEVector2f yAxis2f = new SEVector2f(yAxis.getZ(), yAxis.getY());
            float angle = (float) (180 * yAxis2f.getAngle() / Math.PI);
            setTranslate(getCamera().getScreenLocation(0.1f), false);
            setRotate(new SERotate(-angle, 1, 0, 0), false);
            setUserTransParas();
        }
        setVisible(show, true);
    }

}
