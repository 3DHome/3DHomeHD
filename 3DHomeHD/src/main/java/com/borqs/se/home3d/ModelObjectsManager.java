package com.borqs.se.home3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SECommand;
import com.borqs.se.home3d.ProviderUtils.ModelColumns;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SESceneManager;

public class ModelObjectsManager {

    public HashMap<String, ModelInfo> mModels;

    public ModelObjectsManager() {
        init();
    }

    private void init() {
        Context context = SESceneManager.getInstance().getContext();
        ContentResolver resolver = context.getContentResolver();
        Cursor modelCursor = resolver.query(ModelColumns.CONTENT_URI, null, null, null, null);
        while (modelCursor.moveToNext()) {
            ModelInfo modelInfo = ModelInfo.CreateFromDB(modelCursor);
            if (mModels == null) {
                mModels = new HashMap<String, ModelInfo>();
            }
            mModels.put(modelInfo.mName, modelInfo);
        }
        modelCursor.close();
    }
    /**
     * When the 3Dscene started, will load the preload models which for
     * can be added into the scene quickly.
     * @param finish the finish behavior after preloaded.
     */
    public void loadPreLoadModel(final SEScene scene, final Runnable finish) {
        Iterator<Entry<String, ModelInfo>> iter = mModels.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, ModelInfo> entry = iter.next();
            final ModelInfo modelInfo = entry.getValue();
            if (modelInfo.mType.equals("IconBackground")) {
                if (!modelInfo.hasInstance()) {
                    SELoadResThread.getInstance().process(new Runnable() {
                        public void run() {
                            modelInfo.load3DMAXModel(scene);
                            new SECommand(scene) {
                                public void run() {
                                    modelInfo.add3DMAXModel(scene, scene.getContentObject());
                                    SEObject baseModel = new SEObject(scene, modelInfo.mName, 0);
                                    baseModel.setVisible(false, true);
                                    scene.getContentObject().addChild(baseModel, false);
                                    register(baseModel);
                                    // adjust the size of 3DMax model begin
                                    resizeModel(scene, modelInfo);
                                    // adjust the size of 3DMax model end
                                }
                            }.execute();
                        }
                    });

                }
            }
        }
        SELoadResThread.getInstance().process(new Runnable() {
            public void run() {
                new SECommand(scene) {
                    public void run() {
                        if (finish != null) {
                            finish.run();
                        }
                    }
                }.execute();
            }
        });
    }

    /**
     * When the 3Dscene started,After load all objects finished, will load the preload models which for
     * can be added into the scene quickly.
     * @param finish the finish behavior after afterloaded.
     */
    public void loadAfterLoadModel(final SEScene scene, final Runnable finish) {
        Iterator<Entry<String, ModelInfo>> iter = mModels.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, ModelInfo> entry = iter.next();
            final ModelInfo modelInfo = entry.getValue();
            if (modelInfo.mType.equals("Folder") || modelInfo.mType.equals("Recycle")
                    || modelInfo.mType.equals("IconBox") || modelInfo.mType.equals("shop")
                    || modelInfo.mType.equals("showbox")) {
                if (!modelInfo.hasInstance()) {
                    SELoadResThread.getInstance().process(new Runnable() {
                        public void run() {
                            modelInfo.load3DMAXModel(scene);
                            new SECommand(scene) {
                                public void run() {
                                    modelInfo.add3DMAXModel(scene, scene.getContentObject());
                                    SEObject baseModel = new SEObject(scene, modelInfo.mName, 0);
                                    baseModel.setVisible(false, true);
                                    scene.getContentObject().addChild(baseModel, false);
                                    register(baseModel);
                                    // adjust the size of 3DMax model begin
                                    resizeModel(scene, modelInfo);
                                    // adjust the size of 3DMax model end
                                }
                            }.execute();
                        }
                    });

                }
            }
        }

        SELoadResThread.getInstance().process(new Runnable() {
            public void run() {
                new SECommand(scene) {
                    public void run() {
                        if (finish != null) {
                            finish.run();
                        }
                    }
                }.execute();
            }
        });
    }

    public void createQuickly(SEObject parent, SEObject modelObject) {
        ModelInfo modelInfo = mModels.get(modelObject.mName);
        if (modelInfo.hasInstance()) {
            // clone from base model
            SEObject baseModel = modelInfo.getInstances().get(0);
            baseModel.cloneObjectNew_JNI(parent, modelObject.mIndex);
            parent.addChild(modelObject, false);
            register(modelObject);
        }
    }

    /**
     * If modelObject's mIndex does not equals 0, we will save a base object
     * which index is 0. All objects are cloned from the base object
     */
    public void create(final SEObject parent, final SEObject modelObject, final Runnable finish) {
        final ModelInfo modelInfo = mModels.get(modelObject.mName);
        if (modelObject.mIndex == 0) {
            throw new IllegalArgumentException(
                    "If the object is created from 3DMax model, its index should not equals 0");
        }
        if (modelInfo.hasInstance()) {
            // clone from base model
            SEObject baseModel = modelInfo.getInstances().get(0);
            baseModel.cloneObjectNew_JNI(parent, modelObject.mIndex);
            parent.addChild(modelObject, false);
            register(modelObject);
            if (finish != null) {
                finish.run();
            }
        } else {
            final SEObject baseModel = new SEObject(parent.getScene(), modelInfo.mName, 0);
            createBaseObject(parent.getScene().getContentObject(), baseModel, new Runnable() {
                public void run() {
                    baseModel.setVisible(false, true);
                    baseModel.cloneObjectNew_JNI(parent, modelObject.mIndex);
                    parent.addChild(modelObject, false);
                    register(modelObject);
                    if (finish != null) {
                        finish.run();
                    }
                }
            });
        }
    }

    private void resizeModel(SEScene scene, ModelInfo modelInfo) {
        SEObject modelOf3DMax = new SEObject(scene, modelInfo.mName + "_model");
        if (modelInfo.mLocalTrans != null) {
            modelOf3DMax.setLocalTranslate(modelInfo.mLocalTrans.mTranslate);
            modelOf3DMax.setLocalRotate(modelInfo.mLocalTrans.mRotate);
            modelOf3DMax.setLocalScale(modelInfo.mLocalTrans.mScale);
        }
    }

    public ModelInfo findModelInfo(String name) {
        return mModels.get(name);
    }

    public ArrayList<ModelInfo> findModelInfoByType(String type) {
        ArrayList<ModelInfo> flyers = new ArrayList<ModelInfo>();
        for (Map.Entry<String, ModelInfo> entry : mModels.entrySet()) {
            ModelInfo flyer = entry.getValue();
            if (!flyer.mType.equals(type)) {
                continue;
            }
            flyers.add(flyer);
        }
        return flyers;
    }

    public ArrayList<ModelInfo> getModelsCanBeShowedInMenu() {
        ArrayList<ModelInfo> flyers = new ArrayList<ModelInfo>();
        for (Map.Entry<String, ModelInfo> entry : mModels.entrySet()) {
            ModelInfo flyer = entry.getValue();
            if (flyer.mType.equals("Airship") || flyer.mType.equals("Folder") || flyer.mType.equals("Recycle")
                    || flyer.mType.equals("IconBackground") || flyer.mType.equals("showbox")
                    || flyer.mType.equals("House") || flyer.mType.equals("Laptop") || flyer.mType.equals("Sky")
                    || flyer.mType.equals("Desk")) {
                continue;
            }
            flyers.add(flyer);

        }
        return flyers;
    }

    /**
     * Objects menu can call this method, or we dose not need clone this object
     */
    public void createBaseObject(final SEObject parent, final SEObject modelObject, final Runnable finish) {
        final ModelInfo modelInfo = mModels.get(modelObject.mName);
        if (modelObject.mIndex == 0) {
            // for single instance object
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    modelInfo.load3DMAXModel(parent.getScene());
                    new SECommand(parent.getScene()) {
                        public void run() {
                            modelInfo.add3DMAXModel(parent.getScene(), parent);
                            parent.addChild(modelObject, false);
                            resizeModel(parent.getScene(), modelInfo);
                            register(modelObject);
                            if (finish != null) {
                                finish.run();
                            }
                        }
                    }.execute();
                }
            });
        } else {
            throw new IllegalArgumentException("The index of base object should equals 0");
        }
    }

    /**
     * Only object menu can call this method, release the base object if we do
     * not need it
     */
    public boolean releaseModelAfterHideMenu(ModelInfo modelInfo) {
        if (modelInfo.getInstances().size() == 1) {
            if (!modelInfo.mType.equals("shop") && !modelInfo.mType.equals("IconBox")) {
                SEObject model = modelInfo.getInstances().get(0);
                model.getParent().removeChild(model, true);
                modelInfo.getInstances().clear();
                return true;
            }
        }
        return false;
    }

    public void register(SEObject instance) {
        ModelInfo modelInfo = mModels.get(instance.mName);
        if (modelInfo == null) {
            return;
        }
        if (!modelInfo.getInstances().contains(instance)) {
            modelInfo.getInstances().add(instance);
        }
    }

    public void unRegister(SEObject instance) {
        ModelInfo modelInfo = mModels.get(instance.mName);
        if (modelInfo == null) {
            return;
        }
        if (modelInfo.getInstances().contains(instance)) {
            modelInfo.getInstances().remove(instance);
            if (modelInfo.getInstances().size() == 1) {
                releaseBaseObjectWhileNoObjectCloneFromIt(modelInfo);
            }
        }
    }

    /**
     * We will release the last object if we do not need it no longer
     */
    private void releaseBaseObjectWhileNoObjectCloneFromIt(ModelInfo modelInfo) {
        if (!modelInfo.mType.equals("Folder") && !modelInfo.mType.equals("Recycle")
                && !modelInfo.mType.equals("IconBox") && !modelInfo.mType.equals("IconBackground")
                && !modelInfo.mType.equals("Laptop") && !modelInfo.mType.equals("shop")
                && !modelInfo.mType.equals("House") && !modelInfo.mType.equals("Desk")
                && !modelInfo.mType.equals("Sky") && !modelInfo.mType.equals("showbox")) {
            SEObject theLastInstance = modelInfo.getInstances().get(0);
            theLastInstance.getParent().removeChild(theLastInstance, true);
            modelInfo.getInstances().clear();
        }
    }

}
