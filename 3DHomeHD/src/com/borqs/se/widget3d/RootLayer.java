package com.borqs.se.widget3d;

import android.content.pm.ActivityInfo;
import android.graphics.Rect;

//import com.borqs.se.engine.SEVector.SERay;
import com.borqs.framework3d.home3d.DockAbstractLayer;
import com.borqs.framework3d.home3d.DockObject;
import com.borqs.se.engine.SEVector;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEVector.SEVector2i;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimation.CountAnimation;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SESceneManager;

public class RootLayer extends VesselLayer {
	private static final String TAG = "RootLayer";
    private VesselLayer mCurrentLayer;
    private MoveObjectAnimation mMoveRecycleAnimation;
    private SEObject mRecycle;
    private boolean mHasSetRecycleColor;
    private float mLineGround;

    public RootLayer(SEScene scene, VesselObject vesselObject) {
        super(scene, vesselObject);
        mCurrentLayer = null;
    }

    @Override
    public boolean setOnLayerModel(NormalObject onMoveObject, boolean onLayerModel) {
        super.setOnLayerModel(onMoveObject, onLayerModel);
        mHasSetRecycleColor = false;
        if (mMoveRecycleAnimation != null) {
            mMoveRecycleAnimation.stop();
        }
        if (onLayerModel) {
            mLineGround = getScene().getCamera()
                    .worldToScreenCoordinate(new SEVector3f(0,
                            getScene().mSceneInfo.mHouseSceneInfo.getHouseRadius(), 0)).getY();
            mCurrentLayer = null;
            for (SEObject child : getVesselObject().mChildObjects) {
                if (child instanceof VesselObject) {
                    VesselObject vessel = (VesselObject) child;
                    if (vessel.getVesselLayer().canHandleSlot(onMoveObject)) {
                        mCurrentLayer = vessel.getVesselLayer();
                        loadAndShowRecycle(mCurrentLayer);
                        mCurrentLayer.setOnLayerModel(onMoveObject, true);
                        break;
                    }
                }
            }
        } else {
            if (mCurrentLayer != null) {
                mCurrentLayer.setOnLayerModel(onMoveObject, false);
                mCurrentLayer = null;
            }
            if (mRecycle != null) {
                mRecycle.release();
                mRecycle = null;
            }
        }
        return true;
    }
    //when appObject or native object selected from appdrawer and object menu
    //and when you move your finger, this function will be invoked
    public boolean onObjectMoveEvent(ACTION event, float x, float y) {
        if (x < 0) {
            x = 0;
        } else if (x > SESceneManager.getInstance().getScreenWidth()) {
            x = SESceneManager.getInstance().getScreenWidth();
        }
        if (y < 0) {
            y = 0;
        } else if (y > SESceneManager.getInstance().getScreenHeight()) {
            y = SESceneManager.getInstance().getScreenHeight();
        }
        if (isTouchOnWall((int) x, (int) y)) {
            if (mCurrentLayer instanceof DockAbstractLayer) {
                disableCurrentLayer();
                if (getOnMoveObject() instanceof IconBox) {
                    IconBox iconBox = (IconBox) getOnMoveObject();
                    AppObject appObject = iconBox.changeToAppIcon();
                    setOnMoveObject(appObject);
                }
                for (SEObject child : getVesselObject().mChildObjects) {
                    if (child instanceof House) {
                        VesselObject vessel = (VesselObject) child;
                        if (vessel.getVesselLayer().canHandleSlot(getOnMoveObject())) {
                            mCurrentLayer = vessel.getVesselLayer();
                            loadAndShowRecycle(mCurrentLayer);
                            mCurrentLayer.setOnLayerModel(getOnMoveObject(), true);

                            break;
                        }
                    }
                }
            } else {
            }
        } else {

            if (!(mCurrentLayer instanceof DockAbstractLayer)) {
                //disableCurrentLayer();
                if (getOnMoveObject() instanceof AppObject) {
                    AppObject appObject = (AppObject) getOnMoveObject();
                    IconBox iconBox = appObject.changeToIconBox();
                    setOnMoveObject(iconBox);
                }
                //VesselLayer newLayer = null;
                for (SEObject child : getVesselObject().mChildObjects) {
                    if (child instanceof DockObject) {
                        VesselObject vessel = (VesselObject) child;
                        if (vessel.getVesselLayer().canHandleSlot(getOnMoveObject())) {
                            disableCurrentLayer();
                            mCurrentLayer = vessel.getVesselLayer();
                            loadAndShowRecycle(mCurrentLayer);
                            mCurrentLayer.setOnLayerModel(getOnMoveObject(), true);
                            break;
                        }
                    }
                }
                
            }

        }
        if (mCurrentLayer != null) {
            mCurrentLayer.onObjectMoveEvent(event, x, y);
            if(event == ACTION.BEGIN) {
            	getOnMoveObject().setBeginLayer(mCurrentLayer);
            } else if(event == ACTION.FINISH) {
            	getOnMoveObject().setBeginLayer(null);
            } else if(event == ACTION.MOVE || event == ACTION.UP) {
            	if(getOnMoveObject().getBeginLayer() != mCurrentLayer) {
            		VesselLayer beginLayer = getOnMoveObject().getBeginLayer();
            		
            	}
            }
            if (mCurrentLayer != null) {
                if (mCurrentLayer.mInRecycle) {
                    if (mRecycle != null && !mHasSetRecycleColor) {
                        mRecycle.setUseUserColor(1, 0, 0);
                        mHasSetRecycleColor = true;
                    }
                } else {
                    if (mRecycle != null && mHasSetRecycleColor) {
                        mRecycle.clearUserColor();
                        mHasSetRecycleColor = false;
                    }
                }
            }
        }
        return true;
    }

    private static SETransParas rayCrossWall(House house, SEVector.SERay ray, float wallRadius) {
        // ray cross the front wall
        SETransParas transParas = new SETransParas();
        float y = wallRadius;
        float para = (y - ray.getLocation().getY()) / ray.getDirection().getY();
        transParas.mTranslate = ray.getLocation().add(ray.getDirection().mul(para));
        float faceAngle = house.getFaceAngle();
        float tanAngle = (float) Math.tan(faceAngle * Math.PI / 360);
        float halfFaceW = wallRadius * tanAngle;
        if (transParas.mTranslate.getX() < -halfFaceW) {
            // ray cross the left wall
            float Xa = ray.getLocation().getX();
            float Ya = ray.getLocation().getY();
            float Xb = ray.getDirection().getX();
            float Yb = ray.getDirection().getY();
            para = (tanAngle * Xa + tanAngle * halfFaceW + wallRadius - Ya) / (Yb - tanAngle * Xb);
            transParas.mTranslate = ray.getLocation().add(ray.getDirection().mul(para));
            transParas.mRotate.set(faceAngle, 0, 0, 1);
        } else if (transParas.mTranslate.getX() > halfFaceW) {
            // ray cross the right wall
            float Xa = ray.getLocation().getX();
            float Ya = ray.getLocation().getY();
            float Xb = ray.getDirection().getX();
            float Yb = ray.getDirection().getY();
            para = (-tanAngle * Xa + tanAngle * halfFaceW + wallRadius - Ya) / (Yb + tanAngle * Xb);
            transParas.mTranslate = ray.getLocation().add(ray.getDirection().mul(para));
            transParas.mRotate.set(-faceAngle, 0, 0, 1);
        }
        return transParas;
    }

    public boolean isTouchOnWall(int touchX, int touchY) {
        return touchY < mLineGround;
    }

    private void disableCurrentLayer() {
        if (mCurrentLayer != null) {
            mCurrentLayer.setOnLayerModel(getOnMoveObject(), false);
            mCurrentLayer = null;
        }
    }

    private void loadAndShowRecycle(VesselLayer currentLayer) {
        SEVector3f srcTranslate;
        SEVector3f desTranslate;
        float scale;
        boolean onDeskSight = getScene().getStatus(SEScene.STATUS_ON_DESK_SIGHT);
        Rect boundOfRecycle = new Rect();
        if (onDeskSight) {
            float y = getScene().mSceneInfo.mHouseSceneInfo.getHouseRadius() * 0.66f;
            srcTranslate = new SEVector3f(0, y, 500);
            desTranslate = new SEVector3f(0, y, 0);
            SEVector2i locationTop = getScene().getCamera().worldToScreenCoordinate(
                    desTranslate.add(new SEVector3f(0, 0, 200)));
            scale = 0.66f;
            
            SEVector2i locationBottom = getScene().getCamera().worldToScreenCoordinate(desTranslate);
            boundOfRecycle.top = locationTop.getY();
            boundOfRecycle.left = (int) (getScene().getCamera().getWidth() * 0.3f);
            boundOfRecycle.right = (int) (getScene().getCamera().getWidth() * 0.7f);
            boundOfRecycle.bottom = locationBottom.getY();
            
        } else {
        	float y ;
        	float x ;
        	
        	if(SettingsActivity.getPreferRotation(SESceneManager.getInstance().getGLActivity()) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
        		y = getScene().mSceneInfo.mHouseSceneInfo.getHouseRadius() * 0.8f;
            	x = y;
            	boundOfRecycle.left = (int) (getScene().getCamera().getWidth() * 0.7f);
            	boundOfRecycle.right = (int) (getScene().getCamera().getWidth());
        	}else {
        		y = getScene().mSceneInfo.mHouseSceneInfo.getHouseRadius() * 0.66f;
        		x = 0;
        		boundOfRecycle.left = (int) (getScene().getCamera().getWidth() * 0.3f);
            	boundOfRecycle.right = (int) (getScene().getCamera().getWidth()* 0.7f);
        	}
        	
        	srcTranslate = new SEVector3f(x, y, 500);
        	desTranslate = new SEVector3f(x, y, 0);
        	SEVector2i locationTop = getScene().getCamera().worldToScreenCoordinate(
        			desTranslate.add(new SEVector3f(0, 0, 200)));
        	scale = 0.66f;
        	
        	SEVector2i locationBottom = getScene().getCamera().worldToScreenCoordinate(desTranslate);
        	boundOfRecycle.top = locationTop.getY();
        	boundOfRecycle.bottom = locationBottom.getY();
        	
//            float z = getScene().mSceneInfo.mHouseSceneInfo.getTopOffset() - 100;
//            srcTranslate = new SEVector3f(0, getScene().mSceneInfo.mSkyRadius, z + 500);
//            desTranslate = new SEVector3f(0, getScene().mSceneInfo.mSkyRadius, z);
//
//            boundOfRecycle.top = 0;
//            scale = 0.8f;
        }
        
        currentLayer.setBoundOfRecycle(boundOfRecycle);
        loadAndShowRecycle(srcTranslate, desTranslate, scale);
    }

    private void loadAndShowRecycle(SEVector3f srcTranslate, SEVector3f desTranslate, float scale) {
        if (mMoveRecycleAnimation != null) {
            mMoveRecycleAnimation.stop();
        }
        if (mRecycle != null) {
            SEVector3f mySrcTranslate = mRecycle.getUserTranslate();
            if (desTranslate.subtract(mySrcTranslate).getLength() > 1) {
                mMoveRecycleAnimation = new MoveObjectAnimation(getScene(), mRecycle, mySrcTranslate, desTranslate, 3);
                mMoveRecycleAnimation.execute();
            }
            return;
        }
        int index = (int) System.currentTimeMillis();
        mRecycle = new SEObject(getScene(), "recycle", index);
        ModelInfo modelInfo = getScene().mSceneInfo.findModelInfo("recycle");
        modelInfo.cloneMenuItemInstance(getScene().getContentObject(), index, false, modelInfo.mStatus);
        mRecycle.setTranslate(srcTranslate, true);
        mRecycle.setScale(new SEVector3f(scale, scale, scale), true);
        mMoveRecycleAnimation = new MoveObjectAnimation(getScene(), mRecycle, srcTranslate, desTranslate, 3);
        mMoveRecycleAnimation.execute();
    }

    private class MoveObjectAnimation extends CountAnimation {
        private float mStep;
        private SEVector3f mDirect;
        private SEVector3f mDLocation;
        private SEVector3f mSLocation;
        private SEObject mNormalObject;
        private int mCur;
        private int mEnd;

        public MoveObjectAnimation(SEScene scene, SEObject normalObject, SEVector3f srcLocation,
                SEVector3f desLocation, float step) {
            super(scene);
            mNormalObject = normalObject;
            mSLocation = srcLocation;
            mDLocation = desLocation;
            mStep = step;
            mCur = 0;
        }

        @Override
        public void runPatch(int count) {
            if (mCur != mEnd) {
                int needTranslate = mEnd - mCur;
                int absNTX = Math.abs(needTranslate);
                if (absNTX <= mStep) {
                    mCur = mEnd;
                } else {
                    int step = (int) (mStep * Math.sqrt(absNTX));
                    if (needTranslate < 0) {
                        step = -step;
                    }
                    mCur = mCur + step;
                }
            }
            if (mCur == mEnd) {
                stop();
            }
            mNormalObject.setTranslate(mSLocation.add(mDirect.mul(mCur)), true);
        }

        @Override
        public void onFirstly(int count) {
            mDirect = mDLocation.subtract(mSLocation);
            mEnd = (int) mDirect.getLength();
            mDirect.normalize();
            mNormalObject.setRotate(new SERotate(0, 0, 0, 1), true);
        }
    }
}
