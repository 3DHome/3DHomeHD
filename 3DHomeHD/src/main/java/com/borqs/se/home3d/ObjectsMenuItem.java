package com.borqs.se.home3d;

import java.io.File;

import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectGroup;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SESceneManager;

public class ObjectsMenuItem extends SEObjectGroup {
    public SETransParas mPreviewTrans;
    public ModelInfo mModelInfo;

    public ObjectsMenuItem(SEScene scene, ModelInfo modelInfo) {
        super(scene, modelInfo.mName, 0);
        mModelInfo = modelInfo;
        if ("HorizontalWallFrame".equals(mModelInfo.mType) || "VerticalWallFrame".equals(mModelInfo.mType)) {
            SEObject rope = new SEObject(getScene(), mModelInfo.mChildNames[3], 0);
            SEObject hook = new SEObject(getScene(), mModelInfo.mChildNames[4], 0);
            rope.setVisible(false, true);
            hook.setVisible(false, true);
        }
    }

    public void loadObject(final Runnable finish) {
        SELoadResThread.getInstance().process(new Runnable() {
            public void run() {
                if (hasBeenReleased()) {
                    return;
                }
                mModelInfo.load3DMAXModel(getScene()); // SELoadResThread
                new SECommand(getScene()) {
                    public void run() {
                        mModelInfo.add3DMAXModel(getScene(), getParent());
                        if (hasBeenReleased()) {
                            release();
                        } else {
                            setUserTransParas();
                            if (finish != null) {
                                finish.run();
                            }
                        }
                    }
                }.execute();
            }
        });
    }

    @Override
    public boolean cloneObject_JNI(SEObject parent, int index, boolean copy, String status) {
        if ("IconBox".equals(mModelInfo.mType) || "TV".equals(mModelInfo.mType)) {
            return super.cloneObject_JNI(parent, index, true, status);
        }
        if ("TableFrame".equals(mModelInfo.mType) || "HorizontalWallFrame".equals(mModelInfo.mType)
                || "VerticalWallFrame".equals(mModelInfo.mType)) {
            copy = true;
        }
        boolean result = super.cloneObject_JNI(parent, index, copy, status);
        if (!result) {
            return result;
        }
        if (copy) {
            final String imageFileName = HomeUtils.PKG_FILES_PATH + getScene().mSceneName + mName + index
                    + ".png";
            File f = new File(imageFileName);
            SEObject photoObject = new SEObject(getScene(), mModelInfo.mChildNames[0], index);
            if (f.exists()) {
                final String newImageKey = photoObject.getImageName_JNI();
                SEObject.applyImage_JNI(newImageKey, imageFileName);
                SELoadResThread.getInstance().process(new Runnable() {
                    public void run() {
                        final int imageData = SEObject.loadImageData_JNI(imageFileName);
                        if (imageData != 0)
                            new SECommand(SESceneManager.getInstance().getCurrentScene()) {
                                public void run() {
                                    SEObject.addImageData_JNI(imageFileName, imageData);
                                }
                            }.execute();
                    }
                });
            }
        }
        return result;
    }
}
