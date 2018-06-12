package com.borqs.se.home3d;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout.Alignment;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import com.borqs.framework3d.home3d.DockObject;
import com.borqs.se.engine.SELoadResThread;
import com.borqs.freehdhome.R;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERect3D;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEVector.SEVector2f;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAlphaAnimation;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SEEmptyAnimation;
import com.borqs.se.engine.SETranslateAnimation;
//import com.borqs.se.home3d.ProviderUtils.AppsDrawerColumns;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SEImageView;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectData.IMAGE_TYPE;
import com.borqs.se.engine.SEObjectFactory;
import com.borqs.se.engine.SEObjectGroup;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.shortcut.AppItemInfo;
import com.borqs.se.shortcut.ItemInfo;
import com.borqs.se.shortcut.LauncherModel;
import com.borqs.se.shortcut.LauncherModel.AppCallBack;
import com.borqs.se.widget3d.AppObject;
//import com.borqs.se.widget3d.Desk;
import com.borqs.se.widget3d.ShortcutObject;

public class ApplicationMenu extends SEObjectGroup implements AppCallBack {

    private static final int CYLINDER_FACE_NUM = 4;
    public static final int SORT_BY_NAME = 0;
    public static final int SORT_BY_RECENTLY_INSTALL = 1;
    public static final int SORT_BY_USED_FREQUENCY = 2;

    private static final int MAX_INDICATOR_HEIGHT = 8;
    private static final int MIN_DRAWER_FACE_NUM = 2;

    private int mSortOption = SORT_BY_NAME;

    private List<PreViewFace> mPreViewFaces;
    private List<PreViewObject> mApps;
    private List<PreViewObject> mShortcuts;
    private float mFaceAngle;
    private float mFaceRadius;

    private int mFaceNumber;
    private int mAppFaceNum;
    private int mShortcutFaceNum;
    private VelocityTracker mVelocityTracker;
    private float mCurAngle;
    private LauncherModel mModel;
    private OnTouchListener mLongClickListener;
    private boolean mDisableTouch;
    private boolean mHasLoaded;
    private DockObject mDockObject;

    private int mIconCount;

    public static final int TYPE_SHOW_KEEP_LAST = 0;
    public static final int TYPE_SHOW_APP = 1;
    public static final int TYPE_SHOW_SHORTCUT = 2;

    private SEEmptyAnimation mVAnimation;

    private final float SCALE = 0.1f;
    private final float MENU_SCALE = 0.05f;

    private float mCylinderIndex = -1;
    private int mFaceAIndex = -1;
    private int mFaceBIndex = -1;
    private int mCylinderIndexOfFaceA = -1;
    private int mCylinderIndexOfFaceB = -1;
    private DocMenuGroup mDocMenuGroup;
    private SEObjectGroup mRotateMenu;
    private SEObject mBackground;
    private SEImageView mIndicatorA;
    private SEImageView mIndicatorB;
    private SEImageView mIndicatorEnd;
//    private AppSearchPane mAppSearchPane;

    private boolean mIsBusy = false;
    
    //moved from SESceneManager
    private float mFontScale;
    private float mScreenDensity;
    private int mAppMemuIconSize;
    private int mAppMenuIconTextSize;
    private int mAppMenuIconPaddingTop;
    private int mAppMenuIconPadding;
    private int mAppsMenuCellCountX;
    private int mAppsMenuCellCountY;
    private float mAppsMenuCellWidth;
    private float mAppsMenuCellHeight;
    private float mAppsMenuWidthGap;
    private float mAppsMenuHeightGap;
    private float mAppsMenuWallWidth;
    private float mAppsMenuWallHeight;
    private float mAppsMenuWallPaddingLeft;
    private float mAppsMenuWallPaddingRight;
    private float mAppsMenuWallPaddingTop;
    private float mAppsMenuWallPaddingBottom;

    // public SortAppListDialog mSortAppListDialog = null;
    private AlertDialog mSortAppListDialog;

    private boolean mWasPortCalculated = false;

    public ApplicationMenu(SEScene scene, String name) {
        super(scene, name);
        mModel = LauncherModel.getInstance();
        mCurAngle = 0;
        mHasLoaded = false;
        mDisableTouch = true;
        mIsBusy = false;
        setOnLongClickListener(new OnTouchListener() {
            public void run(SEObject obj) {
                hide(true, null);
                PreViewObject preViewObject = (PreViewObject) obj;
                ItemInfo item = preViewObject.getItemInfo();
                SETransParas startTranspara = new SETransParas();
                startTranspara.mTranslate = obj.getAbsoluteTranslate();
                AppObject itemObject;
                if (item instanceof AppItemInfo) {
                    itemObject = AppObject.create(getScene(), (AppItemInfo)preViewObject.getItemInfo());
                } else {
                    itemObject = ShortcutObject.create(getScene(), preViewObject);
                }
                getScene().getContentObject().addChild(itemObject, true);
                itemObject.initStatus(getScene());
                itemObject.setTouch(obj.getTouchX(), obj.getTouchY());
                itemObject.setStartTranspara(startTranspara);
                itemObject.setOnMove(true);
            }
        });

        caculateAppsMenuWallSize(SESceneManager.getInstance().getWidth(), SESceneManager.getInstance().getHeight());
    }

    private SETransParas getBackgroundTransparas() {
        SETransParas transParas = new SETransParas();
        float y = (float) (mAppsMenuWallWidth / (2 * Math.tan(getCamera().getFov() * Math.PI / 360)));
        float actualY = y + 3 * mFaceRadius;
        float scale = actualY * SCALE / y;
        transParas.mTranslate = getCamera().getScreenLocation(scale);
        transParas.mScale.set(scale, scale, scale);
        return transParas;
    }

    @Override
    public void onRenderFinish(final SECamera camera) {
        super.onRenderFinish(camera);
        
        mIconCount = mAppsMenuCellCountX * mAppsMenuCellCountY;

        setVisible(false, true);
        mRotateMenu = new SEObjectGroup(getScene(), "RotateMenu");
        addChild(mRotateMenu, true);
        mRotateMenu.setPressType(PRESS_TYPE.NONE);

        mBackground = new SEObject(getScene(), "application_background");
        SERect3D rect = new SERect3D(mAppsMenuWallWidth, mAppsMenuWallHeight);
        SEObjectFactory.createRectangle(mBackground, rect, IMAGE_TYPE.COLOR, null, null, null, new float[] { 0, 0, 0 },
                true);

        mBackground.setAlpha(0.6f, true);
        addChild(mBackground, true);
        mBackground.setBlendSortAxis(AXIS.Y);
        mBackground.setIsEntirety_JNI(false);
        mBackground.setClickable(false);

        mDocMenuGroup = new DocMenuGroup(getScene(), "DocMenuGroup");
        addChild(mDocMenuGroup, true);

        createBackground();
        LauncherModel.getInstance().addAppCallBack(this);
        readSortOptionConfiguration();
    }

    private PreViewCylinder mPreViewCylinder;

    private void createBackground() {
        mPreViewCylinder = new PreViewCylinder(getScene(), "PreViewCylinder");
        mRotateMenu.addChild(mPreViewCylinder, true);
        mPreViewCylinder.setVisible(false, true);
        mPreViewCylinder.setPressType(PRESS_TYPE.NONE);
    }

    private SETransParas getIconSlotTransParas(int left, int top) {
        SETransParas transparas = new SETransParas();
        float gridSizeX = mAppsMenuCellWidth + mAppsMenuWidthGap;
        float gridSizeY = mAppsMenuCellHeight + mAppsMenuHeightGap;
        float offsetX = (left + 1 / 2.f) * gridSizeX - mAppsMenuCellCountX * gridSizeX / 2.f
                + (mAppsMenuWallPaddingLeft - mAppsMenuWallPaddingRight) / 2;
        float offsetZ = mAppsMenuCellCountY * gridSizeY / 2.f - (top + 1 / 2.f) * gridSizeY
                + (mAppsMenuWallPaddingBottom - mAppsMenuWallPaddingTop) / 2;
        transparas.mTranslate.set(offsetX, 0, offsetZ);
        return transparas;
    }

    private SETransParas getFaceSlotTransParas(int face) {
        SETransParas transparas = new SETransParas();
        float angle = face * 90;
        SEVector2f direction = new SEVector2f((float) Math.sin(angle * Math.PI / 180), (float) -Math.cos(angle
                * Math.PI / 180));

        float y = (float) (mAppsMenuWallWidth / (2 * Math.tan(getCamera().getFov() * Math.PI / 360)));
        float faceY = y - 0.5f * mFaceRadius;
        float faceScale = faceY / y;
        float faceRadius = 1.5f * mFaceRadius;
        SEVector2f offset = direction.mul(faceRadius);
        transparas.mTranslate.set(offset.getX(), offset.getY(), 0);
        transparas.mRotate.set(angle, 0, 0, 1);
        transparas.mScale.set(faceScale, faceScale, faceScale);
        return transparas;
    }

    @Override
    public void onRelease() {
        super.onRelease();
        LauncherModel.getInstance().removeAppCallBack(this);
    }

    public void show(final int type, final DockObject dockObject) {
        if (!getScene().getStatus(SEScene.STATUS_APP_MENU) && mHasLoaded) {
            mDisableTouch = true;
            stopAllAnimation(null);
            getScene().setStatus(SEScene.STATUS_APP_MENU, true);
            if (type == TYPE_SHOW_APP) {
                mCurAngle = 0;
            } else if (type == TYPE_SHOW_SHORTCUT) {
                mCurAngle = mFaceAngle * mShortcutFaceNum;
            }

            if (mDockObject == null && getScene() instanceof HomeScene) {
                HomeScene homeScene = (HomeScene) getScene();
//                mDesk = homeScene.getDesk();
                if (null == dockObject) {
                    mDockObject = ModelInfo.getDockObject(getScene());
                } else {
                    mDockObject = dockObject;
                }
            }
            getCamera().moveToWallSight(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    if (mDockObject != null) {
                        mDockObject.hide(new SEAnimFinishListener() {
                            public void onAnimationfinish() {
                            }
                        });
                    }
                    setVisible(true, true);
                    setCubeToRightPosition();
                }
            });
        }
    }

    private void setCubeToRightPosition() {
        stopAllAnimation(null);
        if (getScene() instanceof HomeScene) {
            final SEVector3f fromScale = new SEVector3f(0, 0, 0);
            final SEVector3f toScale = new SEVector3f(SCALE, SCALE, SCALE);
            float y = (float) (mAppsMenuWallWidth / (2 * Math.tan(getCamera().getFov() * Math.PI / 360)) + mFaceRadius) * SCALE;
            final SEVector3f fromLocation = new SEVector3f(0, 0, 0);
            final SEVector3f toLocation = new SEVector3f(0, y, 0).add(getCamera().getLocation());
            mIndexEnd = -1;
            setRotate(mCurAngle);

            mRotateMenu.getUserTransParas().mTranslate = fromLocation.clone();
            mRotateMenu.getUserTransParas().mRotate.set(mCurAngle, 0, 0, 1);
            mRotateMenu.getUserTransParas().mScale = fromScale.clone();
            mRotateMenu.setUserTransParas();
            mRotateMenu.setAlpha(0, true);

            mBackground.setAlpha(0, true);
            mBackground.getUserTransParas().set(getBackgroundTransparas());
            mBackground.setUserTransParas();
            SEEmptyAnimation updateSceneAnimation = new SEEmptyAnimation(getScene(), 0, 1, 15) {
                @Override
                public void onAnimationRun(float value) {
                    SEVector3f distanceScale = toScale.subtract(fromScale);
                    SEVector3f scale = fromScale.add(distanceScale.mul(value));
                    mRotateMenu.setScale(scale, true);

                    SEVector3f distanceLocation = toLocation.subtract(fromLocation);
                    SEVector3f location = fromLocation.add(distanceLocation.mul(value));
                    mRotateMenu.setTranslate(location, true);
                    mRotateMenu.setAlpha(value, true);

                    mBackground.setAlpha(0.75f * value, true);
                    mDisableTouch = false;
                }

            };
            updateSceneAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    Resources res = getContext().getResources();
                    int dockSize = getBottomDockHeight(res);
                    SEVector3f yAxis = getCamera().getAxisY();
                    float scale = MENU_SCALE;
                    SEVector3f yTrans = yAxis.mul((dockSize - getCamera().getHeight()) / 2).selfMul(scale);
                    SEVector3f toLocation = getCamera().getScreenLocation(scale).selfAdd(yTrans);
                    SEVector3f fromLocation = toLocation.add(new SEVector3f(0, 0, -dockSize * scale));
                    mDocMenuGroup.getUserTransParas().mTranslate = fromLocation.clone();
                    mDocMenuGroup.getUserTransParas().mScale.set(scale, scale, scale);
                    mDocMenuGroup.setUserTransParas();
                    mDocMenuGroup.setAlpha(1, true);
                    SETranslateAnimation moveDockAnimation = new SETranslateAnimation(getScene(), mDocMenuGroup,
                            fromLocation, toLocation, 6);
                    moveDockAnimation.execute();
                }
            });
            AccelerateInterpolator accelerateInterpolator = new AccelerateInterpolator();
            updateSceneAnimation.setInterpolator(accelerateInterpolator);
            updateSceneAnimation.execute();

        }
    }

    public void hide(boolean fast, final SEAnimFinishListener l) {
        if (getScene().getStatus(SEScene.STATUS_APP_MENU) && !mDisableTouch) {
            stopAllAnimation(null);
            mDisableTouch = true;
            final SEVector3f fromScale = mRotateMenu.getUserTransParas().mScale.clone();
            final SEVector3f toScale = new SEVector3f(0, 0, 0);

            final SEVector3f fromLocation = mRotateMenu.getUserTransParas().mTranslate.clone();
            final SEVector3f toLocation = new SEVector3f(0, 0, 0);
            Resources res = getContext().getResources();
            int dockSize = getBottomDockHeight(res);
            final SEVector3f menuFromLocation = mDocMenuGroup.getUserTranslate().clone();
            final SEVector3f menuToLocation = menuFromLocation.add(new SEVector3f(0, 0, -dockSize * 0.1f));

            final SEEmptyAnimation updateSceneAnimation = new SEEmptyAnimation(getScene(), 0, 1, 15) {

                public void onAnimationRun(float value) {
                    SEVector3f scaleDistance = toScale.subtract(fromScale);
                    SEVector3f scale = fromScale.add(scaleDistance.mul(value));
                    mRotateMenu.setScale(scale, true);
                    SEVector3f distanceLocation = toLocation.subtract(fromLocation);
                    SEVector3f location = fromLocation.add(distanceLocation.mul(value));
                    mRotateMenu.setTranslate(location, true);
                    mRotateMenu.setAlpha(1 - value, true);

                    SEVector3f distance = menuToLocation.subtract(menuFromLocation);
                    SEVector3f nemuLocation = menuFromLocation.add(distance.mul(value));
                    mDocMenuGroup.setTranslate(nemuLocation, true);
                    mDocMenuGroup.setAlpha(1 - value, true);
                    mBackground.setAlpha(0.6f * (1 - value), true);
                }

            };
            updateSceneAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    setVisible(false, true);
                    if (mDockObject != null) {
                        mDockObject.show(new SEAnimFinishListener() {
                            public void onAnimationfinish() {
                                if (l != null) {
                                    l.onAnimationfinish();
                                }
                                mDisableTouch = false;
                                getScene().setStatus(SEScene.STATUS_APP_MENU, false);
                            }
                        });
                    } else {
                        if (l != null) {
                            l.onAnimationfinish();
                        }
                        mDisableTouch = false;
                        getScene().setStatus(SEScene.STATUS_APP_MENU, false);
                    }
                }
            });
            AccelerateInterpolator accelerateInterpolator = new AccelerateInterpolator();
            updateSceneAnimation.setInterpolator(accelerateInterpolator);

            final float fromAngle = mCurAngle;
            final float toAngle = getFaceAngle(Math.round(mCylinderIndex));
            if (fromAngle != toAngle) {
                int animationTimes = (int) (Math.abs(toAngle - fromAngle) / 3);
                if (animationTimes == 0) {
                    animationTimes = 1;
                }
                mVAnimation = new SEEmptyAnimation(getScene(), 0, 1, animationTimes) {
                    @Override
                    public void onAnimationRun(float value) {
                        float distance = toAngle - fromAngle;
                        float angle = fromAngle + distance * value;
                        setRotate(angle);

                    }
                };
                mVAnimation.setInterpolator(new DecelerateInterpolator());
                mVAnimation.setAnimFinishListener(new SEAnimFinishListener() {

                    public void onAnimationfinish() {
                        updateSceneAnimation.execute();
                        mPreViewCylinder.hide();
                        if (mIndicatorB != null) {
                            mIndicatorB.getParent().removeChild(mIndicatorB, true);
                            mIndicatorB = null;
                        }

                        if (mIndicatorA != null) {
                            mIndicatorA.getParent().removeChild(mIndicatorA, true);
                            mIndicatorA = null;
                        }

                        if (mIndicatorEnd != null) {
                            mIndicatorEnd.getParent().removeChild(mIndicatorEnd, true);
                            mIndicatorEnd = null;
                        }
                    }
                });
                mVAnimation.execute();
            } else {
                updateSceneAnimation.execute();
                if (mIndicatorB != null) {
                    mIndicatorB.getParent().removeChild(mIndicatorB, true);
                    mIndicatorB = null;
                }
                if (mIndicatorA != null) {
                    mIndicatorA.getParent().removeChild(mIndicatorA, true);
                    mIndicatorA = null;
                }

                if (mIndicatorEnd != null) {
                    mIndicatorEnd.getParent().removeChild(mIndicatorEnd, true);
                    mIndicatorEnd = null;
                }
            }

        }
    }

    @Override
    public boolean handleBackKey(SEAnimFinishListener l) {
        if (getScene().getStatus(SEScene.STATUS_APP_MENU)) {
            hide(false, l);
            return true;
        }
        return false;
    }

    private void setRotate(float angle) {
        mCurAngle = angle;
        updateFaceIndex(false);
        updateScale();
        updateIndicator(false);
        mRotateMenu.setRotate(new SERotate(mCurAngle, 0, 0, 1), true);
    }

    private void updateScale() {
        AccelerateInterpolator accelerateInterpolator = new AccelerateInterpolator();
        float input = Math.abs(Math.abs(mCylinderIndex) % 1 - 0.5f) * 2;
        float output = accelerateInterpolator.getInterpolation(input);
        float scalePara = output * 0.2f + 0.8f;
        SEVector3f toScale = new SEVector3f(scalePara * SCALE, scalePara * SCALE, scalePara * SCALE);
        mRotateMenu.setScale(toScale, true);
    }

    private void updateFaceIndex(boolean force) {
        mCylinderIndex = -mCurAngle / mFaceAngle;
        int cylinderIndexOfFaceA;
        int faceAIndex;
        int cylinderIndexOfFaceB;
        int faceBIndex;
        if (mCylinderIndex < 0) {
            cylinderIndexOfFaceA = (int) mCylinderIndex - 1;
            faceAIndex = mFaceNumber + cylinderIndexOfFaceA % mFaceNumber;
            cylinderIndexOfFaceB = (int) mCylinderIndex;
            faceBIndex = mFaceNumber + cylinderIndexOfFaceB % mFaceNumber;
            faceAIndex = faceAIndex % mFaceNumber;
            faceBIndex = faceBIndex % mFaceNumber;
        } else {
            cylinderIndexOfFaceA = (int) mCylinderIndex;
            faceAIndex = cylinderIndexOfFaceA % mFaceNumber;
            cylinderIndexOfFaceB = ((int) mCylinderIndex + 1);
            faceBIndex = cylinderIndexOfFaceB % mFaceNumber;
        }
        if (faceAIndex != mFaceAIndex || faceBIndex != mFaceBIndex || cylinderIndexOfFaceA != mCylinderIndexOfFaceA
                || cylinderIndexOfFaceB != mCylinderIndexOfFaceB || force) {
            mFaceAIndex = faceAIndex;
            mFaceBIndex = faceBIndex;
            mCylinderIndexOfFaceA = cylinderIndexOfFaceA;
            mCylinderIndexOfFaceB = cylinderIndexOfFaceB;
            for (PreViewFace preViewFace : mPreViewFaces) {
                if (preViewFace.mFaceIndex == mFaceAIndex) {
                    preViewFace.getUserTransParas().set(getFaceSlotTransParas(mCylinderIndexOfFaceA));
                    preViewFace.setUserTransParas();
                    preViewFace.setVisible(true, true);
                } else if (preViewFace.mFaceIndex == mFaceBIndex) {
                    preViewFace.getUserTransParas().set(getFaceSlotTransParas(mCylinderIndexOfFaceB));
                    preViewFace.setUserTransParas();
                    preViewFace.setVisible(true, true);
                } else {
                    preViewFace.setVisible(false, true);
                }

            }
        }
    }

    private int mIndexEnd = -1;

    private void updateIndicator(boolean force) {
        if (force) {
            if (mIndicatorB != null) {
                mIndicatorB.getParent().removeChild(mIndicatorB, true);
                mIndicatorB = null;
            }

            if (mIndicatorA != null) {
                mIndicatorA.getParent().removeChild(mIndicatorA, true);
                mIndicatorA = null;
            }

            if (mIndicatorEnd != null) {
                mIndicatorEnd.getParent().removeChild(mIndicatorEnd, true);
                mIndicatorEnd = null;
            }

        }
        if (mIndicatorA == null) {
            mIndicatorA = new SEImageView(getScene(), "application_indicator");
            mIndicatorEnd = new SEImageView(getScene(), "application_indicator_end");
            int sceneW = getCamera().getWidth();
            int sceneH = getCamera().getHeight();
            int w = sceneW / mFaceNumber;
            int h = w / 15;
            if (h > MAX_INDICATOR_HEIGHT) {
                h = MAX_INDICATOR_HEIGHT;
            }
            mIndicatorA.setBackground(R.drawable.indicator_slide_scroll);
            mIndicatorEnd.setBackground(R.drawable.indicator_slide_scroll_end);
            mIndicatorA.setSize(w, h);
            mIndicatorEnd.setSize(w, h);
            addChild(mIndicatorA, true);
            addChild(mIndicatorEnd, true);
            mIndicatorA.setLocalScale(new SEVector3f(MENU_SCALE, MENU_SCALE, MENU_SCALE));
            mIndicatorEnd.setLocalScale(new SEVector3f(MENU_SCALE, MENU_SCALE, MENU_SCALE));
            SEVector3f location = getCamera().getScreenLocation(MENU_SCALE);
            location.selfAdd(new SEVector3f((w - sceneW) / 2f, 0, (sceneH - h) / 2).mul(MENU_SCALE));
            mIndicatorA.setLocalTranslate(location);
            mIndicatorEnd.setLocalTranslate(location.add(new SEVector3f(0, MENU_SCALE * 2, 0)));
            mIndicatorA.cloneObjectNew_JNI(this, 1);
            mIndicatorB = new SEImageView(getScene(), "application_indicator", 1);
            addChild(mIndicatorB, false);
            mIndicatorA.setBlendSortAxis(AXIS.Y);
            mIndicatorB.setBlendSortAxis(AXIS.Y);
            mIndicatorEnd.setBlendSortAxis(AXIS.Y);
            mIndicatorEnd.setAlpha(0, true);
        }
        float index;

        if (mCylinderIndex < 0) {
            index = mFaceNumber + mCylinderIndex % mFaceNumber;
        } else {
            index = mCylinderIndex % mFaceNumber;
        }
        int sceneW = getCamera().getWidth();
        float indicatorW = MENU_SCALE * sceneW / mFaceNumber;
        mIndicatorA.setTranslate(new SEVector3f(indicatorW * index, 0, 0), true);
        mIndicatorB.setTranslate(new SEVector3f(indicatorW * (index - mFaceNumber), 0, 0), true);

        int indexEnd;
        if (Math.abs(mCylinderIndex % 1) <= 0.1f) {
            indexEnd = Math.round(mCylinderIndex) % mFaceNumber;
            if (indexEnd < 0) {
                indexEnd = mFaceNumber + indexEnd;
            }
            if (mIndexEnd != indexEnd || force) {
                mIndexEnd = indexEnd;
                mIndicatorEnd.setTranslate(new SEVector3f(indicatorW * mIndexEnd, 0, 0), true);
            }
        }
        if (mIndexEnd == 0) {
            if (index > mFaceNumber - 1) {
                mIndicatorEnd.setRotate(new SERotate(180, 0, 0, 1), true);
                mIndicatorEnd.setAlpha(index - mFaceNumber + 1, true);
            } else {
                mIndicatorEnd.setRotate(new SERotate(0, 0, 0, 1), true);
                mIndicatorEnd.setAlpha(1 - Math.abs(-index), true);
            }

        } else {
            if (index > mIndexEnd) {
                mIndicatorEnd.setRotate(new SERotate(0, 0, 0, 1), true);
            } else {
                mIndicatorEnd.setRotate(new SERotate(180, 0, 0, 1), true);
            }
            mIndicatorEnd.setAlpha(1 - Math.abs(mIndexEnd - index), true);
        }

    }

    private float getFaceAngle(float index) {
        float to = -index * mFaceAngle;
        return to;
    }

    private void trackVelocity(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        final VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mDisableTouch) {
            return true;
        }
        trackVelocity(event);
        return super.dispatchTouchEvent(event);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            setPreTouch();
            stopAllAnimation(null);
            if (isBusy()) {
                return true;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (Math.abs(getTouchX() - getPreTouchX()) > getTouchSlop() / 2) {
                stopAllAnimation(null);
                return true;
            }
            break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            setPreTouch();
            break;
        case MotionEvent.ACTION_MOVE:
            mIsBusy = true;
            mPreViewCylinder.show();
            int width = getCamera().getWidth();
            float ratio = mFaceAngle / width;
            float transAngle = ratio * (getTouchX() - getPreTouchX());
            float curAngle = mCurAngle + transAngle;
            setRotate(curAngle);
            setPreTouch();
            break;
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
            setPreTouch();
            if (mIsBusy) {
                float srcAngle = mCurAngle;
                float desAngle;
                if (mVelocityTracker.getXVelocity() > 500) {
                    if (mCylinderIndex < 0) {
                        desAngle = getFaceAngle((int) mCylinderIndex - 1);
                    } else {
                        desAngle = getFaceAngle((int) mCylinderIndex);
                    }
                } else if (mVelocityTracker.getXVelocity() < -500) {
                    if (mCylinderIndex < 0) {
                        desAngle = getFaceAngle((int) mCylinderIndex);
                    } else {
                        desAngle = getFaceAngle((int) mCylinderIndex + 1);
                    }
                } else {
                    desAngle = getFaceAngle(Math.round(mCylinderIndex));
                }
                final float fromAngle = srcAngle;
                final float toAngle = desAngle;
                int animationTimes = (int) (Math.sqrt(Math.abs(toAngle - fromAngle)) * 2);
                mVAnimation = new SEEmptyAnimation(getScene(), 0, 1, animationTimes) {
                    @Override
                    public void onAnimationRun(float value) {
                        float distance = toAngle - fromAngle;
                        float angle = fromAngle + distance * value;
                        setRotate(angle);

                    }
                };
                mVAnimation.setInterpolator(new DecelerateInterpolator(2));
                mVAnimation.setAnimFinishListener(new SEAnimFinishListener() {

                    public void onAnimationfinish() {
                        mPreViewCylinder.hide();
                        mIsBusy = false;
                    }
                });
                mVAnimation.execute();
            }
            break;
        }
        return true;
    }

    @Override
    public void setOnLongClickListener(OnTouchListener l) {
        mLongClickListener = l;
    }

    @Override
    public void stopAllAnimation(SEAnimFinishListener l) {
        if (mVAnimation != null) {
            mVAnimation.stop();
        }
    }

    private boolean isBusy() {
        return mIsBusy;

    }

    private class PreViewCylinder extends SEObjectGroup {
        private boolean mHasShowed;
        private SEAlphaAnimation mShowAnimation;

        public PreViewCylinder(SEScene scene, String name) {
            super(scene, name);
            mHasShowed = false;
        }

        public void show() {
            if (!mHasShowed) {
                mHasShowed = true;
                if (mShowAnimation != null) {
                    mShowAnimation.stop();
                }
                setVisible(true, true);
                setAlpha(0, true);
                float fromAlpha = 0;
                float toAlpha = 1;
                mShowAnimation = new SEAlphaAnimation(getScene(), this, fromAlpha, toAlpha, 12);
                mShowAnimation.execute();
            }
        }

        public void hide() {
            if (mHasShowed) {
                mHasShowed = false;
                if (mShowAnimation != null) {
                    mShowAnimation.stop();
                }
                float fromAlpha = getAlpha();
                float toAlpha = 0;
                mShowAnimation = new SEAlphaAnimation(getScene(), this, fromAlpha, toAlpha, 6);
                mShowAnimation.setAnimFinishListener(new SEAnimFinishListener() {
                    public void onAnimationfinish() {
                        setVisible(false, true);
                    }
                });
                mShowAnimation.execute();

            }

        }

        @Override
        public void onRenderFinish(SECamera camera) {
            SEObject faceBackground = new SEObject(getScene(), "faceBackground");
            SERect3D rect = new SERect3D();
            rect.setSize(mAppsMenuWallWidth, mAppsMenuWallHeight, 1);
            SEObjectFactory.createRectangle(faceBackground, rect, IMAGE_TYPE.IMAGE, faceBackground.mName,
                    "assets/appdrawer_background.png", null, null, true);
            float angle = 0 * 90;
            SEVector2f direction = new SEVector2f((float) Math.sin(angle * Math.PI / 180), (float) -Math.cos(angle
                    * Math.PI / 180));
            float faceRadius = (float) (mAppsMenuWallWidth / (2 * Math.tan(90 * Math.PI / 360)));
            SEVector2f offset = direction.mul(faceRadius);
            faceBackground.getUserTransParas().mTranslate.set(offset.getX(), offset.getY(), 0);
            faceBackground.getUserTransParas().mRotate.set(angle, 0, 0, 1);
            faceBackground.getUserTransParas().mScale.set(1, 1, 1);
            faceBackground.setIsEntirety_JNI(false);
            addChild(faceBackground, true);
            for (int i = 1; i < 4; i++) {
                faceBackground.cloneObjectNew_JNI(this, i);
                SEObject clone = new SEObject(getScene(), "faceBackground", i);
                angle = i * 90;
                direction = new SEVector2f((float) Math.sin(angle * Math.PI / 180), (float) -Math.cos(angle * Math.PI
                        / 180));
                faceRadius = (float) (mAppsMenuWallWidth / (2 * Math.tan(90 * Math.PI / 360)));
                offset = direction.mul(faceRadius);
                clone.getUserTransParas().mTranslate.set(offset.getX(), offset.getY(), 0);
                clone.getUserTransParas().mRotate.set(angle, 0, 0, 1);
                clone.getUserTransParas().mScale.set(1, 1, 1);
                clone.setUserTransParas();
                addChild(clone, false);
            }
            setBlendSortAxis(AXIS.Y);
            setIsEntirety_JNI(false);
        }
    }

    private class PreViewFace extends SEObjectGroup {
        public int mFaceIndex;

        @Override
        public void onRenderFinish(SECamera camera) {
        }

        public PreViewFace(SEScene scene, String name) {
            super(scene, name);
        }

    }

    public class PreViewObject extends SEObject {
        private ItemInfo mItemInfo;
        public int mFaceIndex;
        public int mLeft;
        public int mTop;

        private PreViewObject(SEScene scene, String name, ItemInfo itemInfo, int w, int h) {
            super(scene, name);
            setPressType(PRESS_TYPE.COLOR);
            String imageName = name + "_imageName";
            String imagekey = name + "_imageKey";
            SERect3D rect = new SERect3D(w, h);
            SEObjectFactory.createRectangle(this, rect, IMAGE_TYPE.BITMAP, imageName, imagekey, null, null, true);
            setNeedCullFace(true, false);
            float scale = 1;
            int imageW = w;
            int imageH = h;
            if (w > 128) {
                scale = 128f / imageW;
                imageH = (int) (imageH * scale);
                imageW = 128;

            }
            setImageSize(imageW, imageH);
            mItemInfo = itemInfo;
            if (itemInfo instanceof AppItemInfo) {
                setOnLongClickListener(new OnTouchListener() {
                    public void run(SEObject obj) {
                        obj.setPressed(false);
                        if (mLongClickListener != null) {
                            mLongClickListener.run(obj);
                        }
                    }
                });

                setOnClickListener(new OnTouchListener() {
                    public void run(SEObject obj) {
                        final PreViewObject preViewObject = (PreViewObject) obj;
                        final Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        intent.setComponent(preViewObject.getItemInfo().getComponentName());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        SESceneManager.getInstance().startActivity(intent);
//                        preViewObject.getItemInfo().mLaunchCount++;
//                        UpdateDBThread.getInstance().process(new Runnable() {
//                            public void run() {
//                                String where = AppsDrawerColumns.COMPONENTNAME + "='"
//                                        + preViewObject.getItemInfo().getComponentName().toShortString() + "'";
//                                ContentValues values = new ContentValues();
//                                values.put(AppsDrawerColumns.LAUNCHERCOUNT, preViewObject.getItemInfo().mLaunchCount);
//                                getContext().getContentResolver().update(AppsDrawerColumns.CONTENT_URI, values, where,
//                                        null);
//
//                            }
//                        });
                    }
                });
            } else {
                setOnLongClickListener(new OnTouchListener() {
                    public void run(SEObject obj) {
                        obj.setPressed(false);
                        if (mLongClickListener != null) {
                            mLongClickListener.run(obj);
                        }
                    }
                });
                setOnClickListener(new OnTouchListener() {
                    public void run(SEObject obj) {
                        SESceneManager.getInstance().runInUIThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getContext(), R.string.click_menu_shortcut, Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                });
            }
        }

        public void update() {
            SELoadResThread.getInstance().process(new Runnable() {
                public void run() {
                    Bitmap bitmap = getAppMenuIconBitmap(mItemInfo.getIcon(),
                            mItemInfo.getLabel());
                    final int imageData = loadImageData_JNI(bitmap);
                    bitmap.recycle();
                    new SECommand(getScene()) {
                        public void run() {
                            String imagekey = getName() + "_imageKey";
                            addImageData_JNI(imagekey, imageData);
                        }
                    }.execute();

                }
            });
        }

        public Bitmap onStartLoadImage() {
            Bitmap bitmap = getAppMenuIconBitmap(mItemInfo.getIcon(), mItemInfo.getLabel());
            return bitmap;
        }

        public ItemInfo getItemInfo() {
            return mItemInfo;
        }
    }

    private void forceReload(List<ItemInfo> apps, List<ItemInfo> shortcuts) {
        if (mPreViewFaces != null) {
            for (PreViewFace face : mPreViewFaces) {
                face.getParent().removeChild(face, true);
            }
        }
        mPreViewFaces = new ArrayList<PreViewFace>();
        mApps = new ArrayList<PreViewObject>();
        mShortcuts = new ArrayList<PreViewObject>();
        mAppFaceNum = (int) ((apps.size() + mIconCount - 1) / mIconCount);
        mShortcutFaceNum = (int) ((shortcuts.size() + mIconCount - 1) / mIconCount);
        mFaceNumber = mAppFaceNum + mShortcutFaceNum;
        if (mFaceNumber < MIN_DRAWER_FACE_NUM) {
            mFaceNumber = MIN_DRAWER_FACE_NUM;
        }
        mFaceAngle = 360.f / CYLINDER_FACE_NUM;
        mFaceRadius = (float) (mAppsMenuWallWidth / (2 * Math.tan(mFaceAngle * Math.PI / 360)));

        for (int i = 0; i < mFaceNumber; i++) {
            PreViewFace preViewFace = new PreViewFace(getScene(), "preViewFace_" + i);
            preViewFace.mFaceIndex = i;
            mRotateMenu.addChild(preViewFace, true);
            mPreViewFaces.add(preViewFace);
        }
        int size = apps.size();
        for (int i = 0; i < size; i++) {
            ItemInfo appInfo = apps.get(i);
            String objName = appInfo.getPreviewName();
            PreViewObject obj = new PreViewObject(getScene(), objName, appInfo, (int) mAppsMenuCellWidth,
                    (int) mAppsMenuCellHeight);
            mApps.add(obj);
        }
        size = shortcuts.size();
        for (int i = 0; i < size; i++) {
            ItemInfo shortcutInfo = shortcuts.get(i);
            String objName = shortcutInfo.getPreviewName();
            PreViewObject obj = new PreViewObject(getScene(), objName, shortcutInfo, (int) mAppsMenuCellWidth,
                    (int) mAppsMenuCellHeight);
            mShortcuts.add(obj);
        }
        updatePosition();
        updateFaceIndex(true);
        if (getScene().getStatus(SEScene.STATUS_APP_MENU)) {
            updateIndicator(true);
        }
        setBlendSortAxis(AXIS.Y);
    }

    private void updatePosition() {
        Collections.sort(mApps, new SortByType());
        Collections.sort(mShortcuts, new SortByType());
        mAppFaceNum = (int) ((mApps.size() + mIconCount - 1) / mIconCount);
        mShortcutFaceNum = (int) ((mShortcuts.size() + mIconCount - 1) / mIconCount);
        int faceNumber = mAppFaceNum + mShortcutFaceNum;
        if (faceNumber < MIN_DRAWER_FACE_NUM) faceNumber = MIN_DRAWER_FACE_NUM;
        if (faceNumber > mFaceNumber) {
            for (int i = mFaceNumber; i < faceNumber; i++) {
                PreViewFace preViewFace = new PreViewFace(getScene(), "preViewFace_" + i);
                preViewFace.mFaceIndex = i;
                mRotateMenu.addChild(preViewFace, true);
                mPreViewFaces.add(preViewFace);
            }
            mFaceNumber = faceNumber;
        }

        int size = mApps.size();
        for (int i = 0; i < size; i++) {
            PreViewObject obj = mApps.get(i);
            obj.mFaceIndex = i / mIconCount;
            int inFacePosition = i - mIconCount * obj.mFaceIndex;
            obj.mLeft = inFacePosition % mAppsMenuCellCountX;
            obj.mTop = inFacePosition / mAppsMenuCellCountX;
            obj.getUserTransParas().set(getIconSlotTransParas(obj.mLeft, obj.mTop));
            SEObject parent = mPreViewFaces.get(obj.mFaceIndex);
            if (obj.getParent() == parent) {
                obj.setUserTransParas();
            } else if (obj.getParent() != null) {
                obj.changeParent(parent);
                obj.setUserTransParas();
            } else {
                parent.addChild(obj, true);
            }
        }
        size = mShortcuts.size();
        for (int i = 0; i < size; i++) {
            PreViewObject obj = mShortcuts.get(i);
            obj.mFaceIndex = i / mIconCount;
            int inFacePosition = i - mIconCount * obj.mFaceIndex;
            obj.mLeft = inFacePosition % mAppsMenuCellCountX;
            obj.mTop = inFacePosition / mAppsMenuCellCountX;
            obj.mFaceIndex = obj.mFaceIndex + mAppFaceNum;
            obj.getUserTransParas().set(getIconSlotTransParas(obj.mLeft, obj.mTop));
            SEObject parent = mPreViewFaces.get(obj.mFaceIndex);
            if (obj.getParent() == parent) {
                obj.setUserTransParas();
            } else if (obj.getParent() != null) {
                obj.changeParent(parent);
                obj.setUserTransParas();
            } else {
                parent.addChild(obj, true);
            }

        }
        if (faceNumber < mFaceNumber) {
            for (int i = faceNumber; i < mFaceNumber; i++) {
                PreViewFace preViewFace = mPreViewFaces.get(i);
                preViewFace.getParent().removeChild(preViewFace, true);
                mPreViewFaces.remove(preViewFace);
                mFaceNumber--;
                i--;
            }
        }

    }

    public void addApps(List<ItemInfo> apps) {
        final List<ItemInfo> myApps = new ArrayList<ItemInfo>();
        myApps.addAll(apps);
//        if (mSortOption == SORT_BY_USED_FREQUENCY) {
//            getLauncherCount(myApps, new Runnable() {
//                public void run() {
//                    new SECommand(getScene()) {
//                        public void run() {
//                            if (!mHasLoaded) {
//                                return;
//                            }
//                            for (ItemInfo itemInfo : myApps) {
//                                switch (itemInfo.mItemType) {
//                                case ItemInfo.ITEM_TYPE_APP:
//                                    String objNameApp = itemInfo.getPreviewName();
//                                    PreViewObject app = new PreViewObject(getScene(), objNameApp, itemInfo,
//                                            (int) mAppsMenuCellWidth, (int) mAppsMenuCellHeight);
//                                    mApps.add(app);
//                                    break;
//
//                                case ItemInfo.ITEM_TYPE_SHORTCUT:
//                                    String objNameShortcut = itemInfo.getPreviewName();
//                                    PreViewObject shortcut = new PreViewObject(getScene(), objNameShortcut, itemInfo,
//                                            (int) mAppsMenuCellWidth, (int) mAppsMenuCellHeight);
//                                    mShortcuts.add(shortcut);
//                                    break;
//                                }
//                            }
//                            updatePosition();
//                            updateFaceIndex(true);
//                            if (getScene().getStatus(SEScene.STATUS_APP_MENU)) {
//                                updateIndicator(true);
//                            }
//                        }
//                    }.execute();
//
//                }
//
//            });
//        } else {
            new SECommand(getScene()) {
                public void run() {
                    if (!mHasLoaded) {
                        return;
                    }
                    for (ItemInfo itemInfo : myApps) {
                        switch (itemInfo.mItemType) {
                        case ItemInfo.ITEM_TYPE_APP:
                            String objNameApp = itemInfo.getPreviewName();
                            PreViewObject app = new PreViewObject(getScene(), objNameApp, itemInfo, (int) mAppsMenuCellWidth,
                                    (int) mAppsMenuCellHeight);
                            mApps.add(app);
                            break;
                        case ItemInfo.ITEM_TYPE_SHORTCUT:
                            String objNameShortcut = itemInfo.getPreviewName();
                            PreViewObject shortcut = new PreViewObject(getScene(), objNameShortcut, itemInfo,
                                    (int) mAppsMenuCellWidth, (int) mAppsMenuCellHeight);
                            mShortcuts.add(shortcut);
                            break;
                        }
                    }
                    updatePosition();
                    updateFaceIndex(true);
                    if (getScene().getStatus(SEScene.STATUS_APP_MENU)) {
                        updateIndicator(true);
                    }
                }
            }.execute();
//        }
    }

    public void removeApps(List<ItemInfo> apps) {
        final List<ItemInfo> myApps = new ArrayList<ItemInfo>();
        myApps.addAll(apps);
        new SECommand(getScene()) {
            public void run() {
                if (!mHasLoaded) {
                    return;
                }
                for (ItemInfo info : myApps) {
                    Iterator<PreViewObject> iterator;
                    if (info.mItemType == ItemInfo.ITEM_TYPE_APP) {
                        iterator = mApps.iterator();
                        while (iterator.hasNext()) {
                            final PreViewObject app = iterator.next();
                            if (app.getItemInfo().equals(info)) {
                                iterator.remove();
                                app.getParent().removeChild(app, true);
                            }
                        }
                    } else if (info.mItemType == ItemInfo.ITEM_TYPE_SHORTCUT) {
                        iterator = mShortcuts.iterator();
                        while (iterator.hasNext()) {
                            final PreViewObject shortcut = iterator.next();
                            if (shortcut.getItemInfo().equals(info)) {
                                iterator.remove();
                                shortcut.getParent().removeChild(shortcut, true);
                            }
                        }
                    }
                }
                updatePosition();
                updateFaceIndex(true);
                if (getScene().getStatus(SEScene.STATUS_APP_MENU)) {
                    updateIndicator(true);
                }
            }
        }.execute();
    }

    public void updateApps(List<ItemInfo> apps) {
        final List<ItemInfo> myApps = new ArrayList<ItemInfo>();
        myApps.addAll(apps);
        mModel.wait(new Runnable() {
            public void run() {
                new SECommand(getScene()) {
                    public void run() {
                        if (!mHasLoaded) {
                            return;
                        }
                        for (ItemInfo info : myApps) {
                            Iterator<PreViewObject> iterator;
                            if (info.mItemType == ItemInfo.ITEM_TYPE_APP) {
                                iterator = mApps.iterator();
                                while (iterator.hasNext()) {
                                    final PreViewObject app = iterator.next();
                                    if (app.getItemInfo().equals(info)) {
                                        app.getItemInfo().setResolveInfo(info.getResolveInfo());
                                        app.update();
                                    }
                                }
                            } else if (info.mItemType == ItemInfo.ITEM_TYPE_SHORTCUT) {
                                iterator = mShortcuts.iterator();
                                while (iterator.hasNext()) {
                                    final PreViewObject app = iterator.next();
                                    if (app.getItemInfo().equals(info)) {
                                        app.getItemInfo().setResolveInfo(info.getResolveInfo());
                                        app.update();
                                    }
                                }
                            }
                        }
                        updatePosition();
                        updateFaceIndex(true);
                        if (getScene().getStatus(SEScene.STATUS_APP_MENU)) {
                            updateIndicator(true);
                        }
                    }
                }.execute();
            }
        }, 500);
    }

    public void packagesUpdate() {
        final List<ItemInfo> myApps = new ArrayList<ItemInfo>();
        myApps.addAll(mModel.getApps());
        final List<ItemInfo> shortcuts = new ArrayList<ItemInfo>();
        shortcuts.addAll(mModel.getShortcuts());

//        if (mSortOption == SORT_BY_USED_FREQUENCY) {
//            getLauncherCount(myApps, new Runnable() {
//                public void run() {
//                    new SECommand(getScene()) {
//                        public void run() {
//                            forceReload(myApps, shortcuts);
//                            mHasLoaded = true;
//                        }
//                    }.execute();
//
//                }
//
//            });
//        } else {
            new SECommand(getScene()) {
                public void run() {
                    forceReload(myApps, shortcuts);
                    mHasLoaded = true;
                }
            }.execute();
//        }
    }

    @Override
    public void bindAppsAdded(List<ItemInfo> apps) {
        addApps(apps);

    }

    @Override
    public void bindAppsUpdated(List<ItemInfo> apps) {
        updateApps(apps);

    }

    @Override
    public void bindAppsRemoved(List<ItemInfo> apps) {
        removeApps(apps);

    }

    @Override
    public void bindAllPackagesUpdated() {
        packagesUpdate();

    }

    @Override
    public void bindAllPackages() {
        packagesUpdate();

    }

    @Override
    public void bindUnavailableApps(List<ItemInfo> apps) {
        removeApps(apps);
    }

    @Override
    public void bindAvailableApps(List<ItemInfo> apps) {
        addApps(apps);
    }

    private class DocMenuGroup extends SEObjectGroup {

        public DocMenuGroup(SEScene scene, String name) {
            super(scene, name);
        }

        @Override
        public void onRenderFinish(SECamera camera) {
//            Resources res = getContext().getResources();
//            int dockSize = getBottomDockHeight();
//            int screenW = camera.getWidth();
//            SEImageView menuA = new SEImageView(getScene(), "DocMenu_A");
//            menuA.setBackground(R.drawable.dock_search_normal);
//            menuA.setSize(dockSize, dockSize);
//            addChild(menuA, true);
//            menuA.setTranslate(new SEVector3f(-screenW * 3 / 8, -10, 0), true);
//            menuA.setOnClickListener(new OnTouchListener() {
//                @Override
//                public void run(SEObject obj) {
//                    SESceneManager.getInstance().runInUIThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (mAppSearchPane != null) {
//                                SESceneManager.getInstance().getWorkSpace().removeView(mAppSearchPane);
//                                mAppSearchPane = null;
//                            }
//                            mAppSearchPane = new AppSearchPane(SESceneManager.getInstance().getContext());
//                            if (mAppSearchPane != null) {
//                                SESceneManager.getInstance().getWorkSpace().addView(mAppSearchPane);
//                                SESceneManager.getInstance().setAppSearchPane(mAppSearchPane);
//                                mAppSearchPane.setVisibility(View.VISIBLE);
//                                mAppSearchPane.invalidate();
//                                mAppSearchPane.setItemLongClickListener(new AppSearchPaneItemLongClickListener());
//                            }
//                        }
//                    });
//                }
//            });
//
//            SEImageView menuB = new SEImageView(getScene(), "DocMenu_B");
//            menuB.setBackground(R.drawable.dock_home_normal);
//            menuB.setSize(dockSize, dockSize);
//            addChild(menuB, true);
//            menuB.setTranslate(new SEVector3f(0, -10, 0), true);
//            menuB.setOnClickListener(new OnTouchListener() {
//                @Override
//                public void run(SEObject obj) {
//                    hide(true, null);
//                }
//            });
//
//            SEImageView menuC = new SEImageView(getScene(), "DocMenu_C");
//            menuC.setBackground(R.drawable.dock_sort_normal);
//            menuC.setSize(dockSize, dockSize);
//            addChild(menuC, true);
//            menuC.setTranslate(new SEVector3f(screenW * 3 / 8, -10, 0), true);
//            menuC.setOnClickListener(new OnTouchListener() {
//                @Override
//                public void run(SEObject obj) {
//                    showSortTypes();
//                }
//            });
        }

    }

    private static final String PREF_KEY_SORT_OPTION = "PREF_KEY_SORT_OPTION";

    class SortByType implements Comparator<PreViewObject> {
        public int compare(PreViewObject lhs, PreViewObject rhs) {
            if (mSortOption == SORT_BY_NAME) {
                return Collator.getInstance().compare(lhs.getItemInfo().getLabel(), rhs.getItemInfo().getLabel());
//            } else if (mSortOption == SORT_BY_RECENTLY_INSTALL) {
//                if (lhs.getItemInfo().mInstallDateTime == rhs.getItemInfo().mInstallDateTime) {
//                    return 0;
//                } else if (lhs.getItemInfo().mInstallDateTime < rhs.getItemInfo().mInstallDateTime) {
//                    return 1;
//                } else {
//                    return -1;
//                }
//            } else if (mSortOption == SORT_BY_USED_FREQUENCY) {
//                if (lhs.getItemInfo().mLaunchCount == rhs.getItemInfo().mLaunchCount) {
//                    return 0;
//                } else if (lhs.getItemInfo().mLaunchCount < rhs.getItemInfo().mLaunchCount) {
//                    return 1;
//                } else {
//                    return -1;
//                }
            }
            return -1;
        }
    }

    private void readSortOptionConfiguration() {
        final String preferenceName = SettingsActivity.PREFS_SETTING_NAME;
        SharedPreferences settings = SESceneManager.getInstance().getContext().getSharedPreferences(preferenceName, 0);
        mSortOption = settings.getInt(PREF_KEY_SORT_OPTION, SORT_BY_NAME);
    }

    private void writeSortOptionConfiguration() {
        final String preferenceName = SettingsActivity.PREFS_SETTING_NAME;
        SharedPreferences settings = SESceneManager.getInstance().getContext().getSharedPreferences(preferenceName, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PREF_KEY_SORT_OPTION, mSortOption);
        editor.commit();
    }

    private void showSortTypes() {
        SESceneManager.getInstance().runInUIThread(new Runnable() {
            @Override
            public void run() {
                readSortOptionConfiguration();
                mSortAppListDialog = new AlertDialog.Builder(SESceneManager.getInstance().getGLActivity())
                        .setTitle(R.string.title_select_drawer_sort)
                        .setSingleChoiceItems(R.array.app_drawer_sort_type, mSortOption,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int position) {
                                        mSortOption = position;
                                        writeSortOptionConfiguration();
                                        new SECommand(getScene()) {
                                            public void run() {
                                                updatePosition();
                                            }
                                        }.execute();
                                        mSortAppListDialog.dismiss();
                                    }
                                }).create();

                mSortAppListDialog.show();
            }
        });
    }

//    private void getLauncherCount(final List<ItemInfo> apps, final Runnable finish) {
//        LauncherModel.getInstance().process(new Runnable() {
//            public void run() {
//                for (ItemInfo app : apps) {
//                    if (app.mItemType == ItemInfo.ITEM_TYPE_APP) {
//                        String where = AppsDrawerColumns.COMPONENTNAME + "='" + app.getComponentName().toShortString()
//                                + "'";
//                        String[] select = new String[] { AppsDrawerColumns.LAUNCHERCOUNT };
//                        Cursor cursor = getContext().getContentResolver().query(AppsDrawerColumns.CONTENT_URI, select,
//                                where, null, null);
//                        if (cursor != null) {
//                            if (cursor.moveToFirst()) {
//                                app.mLaunchCount = cursor.getInt(0);
//                            } else {
//                                ContentValues values = new ContentValues();
//                                values.put(AppsDrawerColumns.COMPONENTNAME, app.getComponentName().toShortString());
//                                values.put(AppsDrawerColumns.LAUNCHERCOUNT, 0);
//                                getContext().getContentResolver().insert(AppsDrawerColumns.CONTENT_URI, values);
//                            }
//                            cursor.close();
//                        }
//                    }
//                }
//                if (finish != null) {
//                    finish.run();
//                }
//            }
//        });
//    }
    
    //Move all application menu related code from SESceneManager to here
    private int mLastChangedWidth;
    private int mLastChangedHeight;
    public void caculateAppsMenuWallSize(int w, int h) {
        mLastChangedWidth = w;
        mLastChangedHeight = h;
        Resources res = getContext().getResources();
        mAppsMenuWallWidth = w;
        mAppsMenuWallHeight = h;
        mAppsMenuWallPaddingLeft = res.getDimension(R.dimen.apps_customize_pageLayoutPaddingLeft);
        mAppsMenuWallPaddingRight = res.getDimension(R.dimen.apps_customize_pageLayoutPaddingRight);
        mAppsMenuWallPaddingTop = res.getDimension(R.dimen.apps_customize_pageLayoutPaddingTop);
        mAppsMenuWallPaddingBottom = res.getDimension(R.dimen.apps_customize_pageLayoutPaddingBottom);
        caculateAppsMenuCellCount();
    }

    private void caculateAppsMenuCellCount() {
        Resources res = getContext().getResources();
        mAppsMenuCellCountX = 1;
        while (widthInPortraitOfAppsMenu(res, mAppsMenuCellCountX + 1) <= mAppsMenuWallWidth) {
            mAppsMenuCellCountX++;
        }
        mAppsMenuCellCountY = 1;
        while (heightInLandscapeOfAppsMenu(res, mAppsMenuCellCountY + 1) <= mAppsMenuWallHeight) {
            mAppsMenuCellCountY++;
        }
        caculateAppsMenuIconSize();
        caculateAppsMenuCellSize();
    }

    private void caculateAppsMenuIconSize() {
        mFontScale = HomeUtils.getFontSize(getContext());
        Resources res = getContext().getResources();
//        mScreenDensity = getSceneInfo().getScreenDensity();
        mScreenDensity = SESceneManager.getInstance().getPixelDensity();
        mAppMemuIconSize = res.getDimensionPixelSize(R.dimen.app_customize_icon_size);
        mAppMenuIconTextSize = (int) (mFontScale * res.getDimensionPixelSize(R.dimen.app_customize_icon_text_size));
        mAppMenuIconPaddingTop = res.getDimensionPixelSize(R.dimen.app_customize_icon_PaddingTop);
        mAppMenuIconPadding = res.getDimensionPixelSize(R.dimen.app_customize_icon_Padding);
    }

    private void caculateAppsMenuCellSize() {
        Resources res = getContext().getResources();
        mAppsMenuCellWidth = res.getDimensionPixelSize(R.dimen.apps_customize_cell_width);
        mAppsMenuCellHeight = res.getDimensionPixelSize(R.dimen.apps_customize_cell_height);
        float hSpace = mAppsMenuWallWidth - mAppsMenuWallPaddingLeft - mAppsMenuWallPaddingRight;
        float vSpace = mAppsMenuWallHeight - mAppsMenuWallPaddingTop - mAppsMenuWallPaddingBottom;
        float hFreeSpace = hSpace - (mAppsMenuCellCountX * mAppsMenuCellWidth);
        float vFreeSpace = vSpace - (mAppsMenuCellCountY * mAppsMenuCellHeight);
        int numAppsMenuWidthGaps = mAppsMenuCellCountX - 1;
        int numAppsMenuHeightGaps = mAppsMenuCellCountY - 1;
        mAppsMenuWidthGap = hFreeSpace / numAppsMenuWidthGaps;
        mAppsMenuHeightGap = vFreeSpace / numAppsMenuHeightGaps;
    }

    private int widthInPortraitOfAppsMenu(Resources r, int numCells) {
        int cellWidth = r.getDimensionPixelSize(R.dimen.apps_customize_cell_width);
        int minGap = r.getDimensionPixelSize(R.dimen.apps_customize_pageLayoutWidthGap);
        return minGap * (numCells - 1) + cellWidth * numCells;
    }

    private int heightInLandscapeOfAppsMenu(Resources r, int numCells) {
        int cellHeight = r.getDimensionPixelSize(R.dimen.apps_customize_cell_height);
        int minGap = r.getDimensionPixelSize(R.dimen.apps_customize_pageLayoutHeightGap);
        return minGap * (numCells - 1) + cellHeight * numCells;
    }

    public Bitmap getAppMenuIconBitmap(Drawable icon, String title) {
        int bitmapW = (int) mAppsMenuCellWidth;
        int bitmapH = (int) mAppsMenuCellHeight;
        float scale = 1;
        if (bitmapW > 128) {
            scale = 128f / bitmapW;
            bitmapH = (int) (bitmapH * scale);
            bitmapW = 128;

        }
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.density = mScreenDensity;
        textPaint.setTextSize(mAppMenuIconTextSize * scale);
        textPaint.setColor(Color.WHITE);
        textPaint.setShadowLayer(5, 2, 2, Color.BLACK);
        int newW = HomeUtils.higherPower2(bitmapW);
        int newH = HomeUtils.higherPower2(bitmapH);
        Bitmap preview = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(preview);
        canvas.translate((newW - bitmapW) * 0.5f, (newH - bitmapH) * 0.5f);

        int iconSize = (int) (mAppMemuIconSize * scale);
        int iconLeft = (int) ((bitmapW - iconSize) / 2);
        int iconTop = (int) (mAppMenuIconPaddingTop * scale);
        Rect oldBounds = icon.copyBounds();
        icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
        icon.draw(canvas);
        icon.setBounds(oldBounds); // Restore the bounds
        String label = title;

        StaticLayout titleLayout = new StaticLayout(label, textPaint, bitmapW, Alignment.ALIGN_CENTER, 1f, 0.0F, false);
        int lineCount = titleLayout.getLineCount();
        if (lineCount > 2) {
            int index = titleLayout.getLineEnd(1);
            if (index > 0) {
                label = label.substring(0, index);
                titleLayout = new StaticLayout(label, textPaint, bitmapW, Alignment.ALIGN_CENTER, 1f, 0.0F, false);
            }
        }
        float left = 0;
        float top = iconSize + iconTop + mAppMenuIconPadding * scale;
        canvas.save();
        canvas.translate(left, top);
        titleLayout.draw(canvas);
        canvas.restore();
        return preview;
    }

//    public class AppSearchPaneItemLongClickListener implements AppSearchPane.OnAppSearchItemLongClickListener {
//        @Override
//        public void onItemLongClick(AppSearchPane.Item holder) {
//            new SECommand(getScene()) {
//                public void run() {
//                    hide(true, null);
//                }
//            }.execute();
//            if (mAppSearchPane != null) {
//                mAppSearchPane.setVisibility(View.INVISIBLE);
//            }
//
//            int size = mApps.size();
//            String packageName = "";
//            String className = "";
//            PreViewObject obj = null;
//            for (int i = 0; i < size; i++) {
//                obj = mApps.get(i);
//                packageName = obj.mItemInfo.getComponentName().getPackageName();
//                className = obj.mItemInfo.getComponentName().getClassName();
//                if (holder.mPkgName.equals(packageName) && holder.mClsName.equals(className)) {
//                    break;
//                } else {
//                    obj = null;
//                }
//            }
////            for (int i =0; i < size; i++) {
////                PreViewObject tmpObj = mApps.get(i);
////                packageName = tmpObj.mItemInfo.getComponentName().getPackageName();
////                className = tmpObj.mItemInfo.getComponentName().getClassName();
////                if (holder.mPkgName.equals(packageName) && holder.mClsName.equals(className)) {
////                    obj = new PreViewObject(getScene(), tmpObj.getItemInfo().getPreviewName(), tmpObj.getItemInfo(), (int) mAppsMenuCellWidth,
////                            (int) mAppsMenuCellHeight);
////                    break;
////                }
////            }
//
//            SERay ray = getCamera().screenCoordinateToRay(holder.mTouchX, holder.mTouchY);
//            float alpha = (1000f - ray.getLocation().getY()) / ray.getDirection().getY();
//            float posY = 1000f;
//            float posX = ray.getLocation().getX() + alpha * ray.getDirection().getX();
//            float posZ = ray.getLocation().getZ() + alpha * ray.getDirection().getZ();
//            SEVector3f pos = new SEVector3f();
//            pos.set(posX, posY, posZ);
//            ItemInfo item = obj.getItemInfo();
//            SETransParas startTranspara = new SETransParas();
//            startTranspara.mTranslate = pos;
//
//            AppObject itemObject;
//            if (obj != null) {
//                if (item instanceof AppItemInfo) {
//                    itemObject = AppObject.create(getScene(), obj);
//                } else {
//                    itemObject = ShortcutObject.create(getScene(), obj);
//                }
//                getScene().getContentObject().addChild(itemObject, true);
//                itemObject.initStatus();
//                itemObject.setTouch(holder.mTouchX, holder.mTouchY);
//                itemObject.setStartTranspara(startTranspara);
//                itemObject.setOnMove(true);
//                long now = SystemClock.uptimeMillis();
//                MotionEvent eventDown = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, holder.mTouchX, holder.mTouchY, 0);
//                SESceneManager.getInstance().getWorkSpace().dispatchTouchEvent(eventDown);
//            }
//        }
//    }

    private int getBottomDockHeight(Resources res) {
        if (false) {
            return res.getDimensionPixelSize(R.dimen.apps_customize_dock_size);
        }

        return 0;
    }

    @Override
    public void notifySurfaceChanged(int width, int height) {
        if (mLastChangedWidth != width || mLastChangedHeight != height) {
            caculateAppsMenuWallSize(width, height);
        }
    }
}
