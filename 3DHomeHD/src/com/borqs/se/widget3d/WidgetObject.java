package com.borqs.se.widget3d;

import java.util.List;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.FrameLayout.LayoutParams;

import com.borqs.se.engine.SELoadResThread;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERect3D;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SECommand;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.engine.SEBitmap;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEObjectFactory;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.shortcut.HomeWidgetHostView;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;

public class WidgetObject extends NormalObject {
    private HomeWidgetHostView mWidget;
    private SEObject mBackground;
    protected SEObject mIconObject;
    public ObjectSlot mPreSlot;
    private SESceneManager mSESceneManager;
    private boolean mHasBeenReleased = false;

    @Override
    public boolean load(final SEObject parent, final Runnable finish) {
        SELoadResThread.getInstance().process(new Runnable() {
            public void run() {
                AppWidgetProviderInfo widget = findWidgetByComponent(getContext(), getObjectInfo().mComponentName);
                if (mHasBeenReleased || widget == null) {
                    new SECommand(getScene()) {
                        public void run() {
                            parent.removeChild(WidgetObject.this, true);
                            if (finish != null) {
                                finish.run();
                            }
                        }
                    }.execute();
                    return;
                }
                String imageKey = mName + "_imageKey";
                String imageName = mName + "_imageName";
                int w = (int) (getObjectInfo().mObjectSlot.mSpanX * getScene().mSceneInfo.mHouseSceneInfo.getWallUnitSizeX());
                int h = (int) (getObjectInfo().mObjectSlot.mSpanY * getScene().mSceneInfo.mHouseSceneInfo.getWallUnitSizeY());
                float wallX = getScene().mSceneInfo.mHouseSceneInfo.getHouseWidth();
                float wallY = getScene().mSceneInfo.mHouseSceneInfo.getHouseHeight();
                int realW;
                int realH;
                if (getCamera().getWidth() / wallX < getCamera().getHeight() / wallY) {
                    mScale = getCamera().getWidth() / wallX;
                    realW = (int) (w * getCamera().getWidth() / wallX);
                    realH = (int) (h * getCamera().getWidth() / wallX);
                } else {
                    mScale = getCamera().getHeight() / wallY;
                    realW = (int) (w * getCamera().getHeight() / wallY);
                    realH = (int) (h * getCamera().getHeight() / wallY);
                }

                if (realW >= realH && realW > 512) {
                    realH = (int) (realH * 512 / realW);
                    realW = 512;

                } else if (realH > realW && realH > 512) {
                    realW = (int) (realW * 512 / realH);
                    realH = 512;
                }
                Bitmap icon = HomeUtils.createEmptyBitmap(HomeUtils.higherPower2(realW),
                        HomeUtils.higherPower2(realH));
                SERect3D rect = new SERect3D(new SEVector3f(1, 0, 0), new SEVector3f(0, 0, 1));
                rect.setSize(w, h, 1);
                SEBitmap bp = new SEBitmap(icon, SEBitmap.Type.normal);
                mIconObject = new SEObject(getScene(), mName + "_icon");
                SEObjectFactory.createRectangle(mIconObject, rect, imageName, imageKey, bp);
                mIconObject.setImageSize(realW, realH);
                new SECommand(getScene()) {
                    public void run() {
                        if (mHasBeenReleased) {
                            return;
                        }
                        render();
                        initStatus(getScene());
                        if (finish != null) {
                            finish.run();
                        }
                    }
                }.execute();
            }
        });
        return true;
    }

    @Override
    public void initStatus(SEScene scene) {
        super.initStatus(scene);
        se_setNeedBlendSort_JNI(new float[] {0, 1f, 0});
        setOnLongClickListener(new SEObject.OnTouchListener() {
            public void run(SEObject obj) {
                if(!canHandleLongClick()) {
                    return;
                }
                SETransParas startTranspara = new SETransParas();
                startTranspara.mTranslate = getAbsoluteTranslate();
                float angle = getUserRotate().getAngle();
                SEObject parent = getParent();
                while (parent != null) {
                    angle = angle + parent.getUserRotate().getAngle();
                    parent = parent.getParent();
                }
                startTranspara.mRotate.set(angle, 0, 0, 1);
                setStartTranspara(startTranspara);
                setOnMove(true);
            }
        });
        mSESceneManager = SESceneManager.getInstance();
        setHasInit(true);
        bind();
    }

    @Override
    public void showBackgroud() {
        if (mBackground == null) {
            int index = (int) System.currentTimeMillis();
            ModelInfo modelInfo = getScene().mSceneInfo.findModelInfo("IconBackground");
            mBackground = new SEObject(getScene(), "IconBackground", index);
            modelInfo.cloneMenuItemInstance(this, index, false, modelInfo.mStatus);
            addChild(mBackground, false);
            mBackground.getUserTransParas().set(getBackgroundLocation());
            mBackground.setUserTransParas();
            mBackground.se_setNeedBlendSort_JNI(new float[] { 0, 1, 0 });
        } 
        if ("none".equals(SettingsActivity.getAppIconBackgroundName(getContext()))) {
            mBackground.setVisible(false, true);
        } else {
            mBackground.setVisible(true, true);
        }

    }

    @Override
    public void hideBackgroud() {
        if (mBackground != null) {
            removeChild(mBackground, true);
            mBackground = null;
        }
    }

    private SETransParas getBackgroundLocation() {
        SETransParas transparas = new SETransParas();
        transparas.mTranslate.set(-25, HomeUtils.ICON_BACKGROUND_SPACING, -5);
        int spanX = getObjectSlot().mSpanX;
        int spanY = getObjectSlot().mSpanY;
        transparas.mScale.set(spanX * 1.05f, 1, spanY);
        return transparas;
    }

    @Override
    public void onSlotSuccess() {
        super.onSlotSuccess();
        NormalObject parent = (NormalObject)getParent();
        if (ModelInfo.isHouseVesselObject(parent)) {
            showBackgroud();
        } else {
            hideBackgroud();
        }
    }

    @Override
    public void setVisible(boolean visible, boolean sumbit) {
        if (visible) {
            if (mWidget != null) {
                mWidget.updateBitmap();
            }
        }
        if (isVisible()) {
            return;
        }
        super.setVisible(visible, sumbit);
        if (mIconObject != null) {
            mIconObject.setVisible(true, true);
        }
    }

    public float mScale;

    public void bind() {
        if (!hasInit()) {
            return;
        }
        mSESceneManager.runInUIThread(new Runnable() {
            public void run() {
                AppWidgetProviderInfo providerInfo = mSESceneManager.getAppWidgetManager().getAppWidgetInfo(
                        getObjectInfo().mAppWidgetId);
                HomeWidgetHostView hostView = (HomeWidgetHostView) mSESceneManager.getAppWidgetHost().createView(
                        getContext(), getObjectInfo().mAppWidgetId, providerInfo);
                int w = (int) (getObjectInfo().getSpanX() * getScene().mSceneInfo.mHouseSceneInfo.getWallUnitSizeX());
                int h = (int) (getObjectInfo().getSpanY() * getScene().mSceneInfo.mHouseSceneInfo.getWallUnitSizeY());

                float wallX = getScene().mSceneInfo.mHouseSceneInfo.getHouseWidth();
                float wallY = getScene().mSceneInfo.mHouseSceneInfo.getHouseHeight();
                int realW;
                int realH;
                if (getCamera().getWidth() / wallX < getCamera().getHeight() / wallY) {
                    mScale = getCamera().getWidth() / wallX;
                    realW = (int) (w * getCamera().getWidth() / wallX);
                    realH = (int) (h * getCamera().getWidth() / wallX);
                } else {
                    mScale = getCamera().getHeight() / wallY;
                    realW = (int) (w * getCamera().getHeight() / wallY);
                    realH = (int) (h * getCamera().getHeight() / wallY);
                }

                LayoutParams params = new LayoutParams(realW, realH, 0x33);
                hostView.setLayoutParams(params);
                //hostView.layout(0, 0, params.width, params.height);
                setWidgetHostView(hostView);
                hostView.setTag(WidgetObject.this);
                if (mSESceneManager.getWidgetView().bindObject(WidgetObject.this)) {
                    hostView.updateBitmap();
                } else {
                    new SECommand(getScene()) {
                        public void run() {
                            getParent().removeChild(WidgetObject.this, true);
                        }
                    }.execute();
                }
            }
        });
    }

    public void requestUpdateAndroidWidget() {
        mSESceneManager.getWidgetView().requestUpdateWidget(this);
    }

    @Override
    public void onRenderFinish(SECamera camera) {
        super.onRenderFinish(camera);
        if (mIconObject != null) {
            addChild(mIconObject, true);
            mIconObject.setIsEntirety_JNI(false);
        }
    }

    @Override
    public void onRelease() {
        super.onRelease();
        unbind();
        mHasBeenReleased = true;
    }

    private void unbind() {
        if (!hasInit()) {
            return;
        }
        mSESceneManager.runInUIThread(new Runnable() {
            public void run() {
                mSESceneManager.getWidgetView().unbindObject(WidgetObject.this);
                mSESceneManager.getAppWidgetHost().deleteAppWidgetId(getObjectInfo().mAppWidgetId);
            }
        });
    }

    public static AppWidgetProviderInfo findWidgetByComponent(Context context, ComponentName compont) {
        List<AppWidgetProviderInfo> widgets = AppWidgetManager.getInstance(context).getInstalledProviders();
        for (AppWidgetProviderInfo info : widgets) {
            if (info.provider.equals(compont)) {
                return info;
            }
        }
        return null;
    }

    public WidgetObject(SEScene scene, String name, int index) {
        super(scene, name, index);
        setOnClickListener(null);
        setCanChangeBind(false);
    }

    public void setWidgetHostView(HomeWidgetHostView view) {
        mWidget = view;
        mWidget.setWidgetObject(this);
    }

    public HomeWidgetHostView getWidgetHostView() {
        return mWidget;
    }
}
