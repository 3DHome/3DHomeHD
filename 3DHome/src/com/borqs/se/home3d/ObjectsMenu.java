package com.borqs.se.home3d;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.pm.ActivityInfo;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.animation.DecelerateInterpolator;

import com.borqs.market.json.Product;
import com.borqs.market.utils.MarketUtils;
import com.borqs.se.download.Utils;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SEEmptyAnimation;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SETransParas;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.widget3d.Desk;
import com.borqs.se.widget3d.NormalObject;
import com.borqs.se.widget3d.ObjectInfo;

public class ObjectsMenu extends SEObject {
    /**
     * 这个值决定了ObjectsMenu在相机的透射框中的位置，LAY_SCALE越小ObjectsMenu越小且距离相机越近。
     * 值越小越好，也不能太小，太小的话物体会超出相机的近平面
     */
    private static float LAY_SCALE = 0.1f;
    /**
     * 预先加载模型的屏数,如3：表示预先加载当前2列和左右4列，一共加载6列
     */
    private static final int NEED_PRE_LOADING_FACES = 3;
    private static int SHOW_BOX_LINE = 2;
    private static int SHOW_BOX_COLUMN = 4;
    private VelocityTracker mVelocityTracker;
    private SEEmptyAnimation mVelocityAnimation;
    private OnTouchListener mLongClickListener;
    private boolean mDisableTouch;
    private List<MenuItem> mMenuItems;
    /**
     * 假如一屏显示2列： mFaceIndex 为0时显示第一二列，mFaceIndex为1时显示第二三列 ，mFaceIndex为2时显示三四列
     */
    private float mFaceIndex = 0;
    private int mColumnNum;
    private float mBoxWidth;
    private float mBoxHeight;
    private OBJECTSHOWTYPE mObjectShowType;
    private boolean mNeedUpdateMenu = false;
    private HomeScene mHomeScene;

    public enum OBJECTSHOWTYPE {
        ALL, DESK
    }

    public ObjectsMenu(SEScene scene, String name) {
        super(scene, name, 0);

        if (HomeManager.getInstance().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            SHOW_BOX_LINE = 4;
            SHOW_BOX_COLUMN = 2;
        } else {
            SHOW_BOX_LINE = 2;
            SHOW_BOX_COLUMN = 4;
        }
        mHomeScene = (HomeScene) scene;
        setIsGroup(true);
        mDisableTouch = true;
        setOnLongClickListener(new OnTouchListener() {
            public void run(SEObject obj) {
                // Long click object to added it to scene.
                String objName = (String) obj.getTag();
                ObjectInfo objInfo = new ObjectInfo();
                objInfo.setModelInfo(HomeManager.getInstance().getModelManager().findModelInfo(objName));
                objInfo.mIndex = ProviderUtils.searchMaxIndex(mHomeScene, ProviderUtils.Tables.OBJECTS_INFO, objName) + 1;
                objInfo.mSceneName = mHomeScene.getSceneName();
                objInfo.saveToDB();
                SETransParas startTransParas = new SETransParas();
                startTransParas.mTranslate = obj.getAbsoluteTranslate();
                NormalObject newObject = objInfo.CreateNormalObject(mHomeScene);
                HomeManager.getInstance().getModelManager().createQuickly(mHomeScene.getContentObject(), newObject);
                mHomeScene.getContentObject().addChild(newObject, false);
                newObject.setIsFresh(true);
                newObject.initStatus();
                newObject.setHasInit(true);
                hide(true, null);
                newObject.setUserTranslate(startTransParas.mTranslate);
                newObject.setTouch(obj.getTouchX(), obj.getTouchY());
                newObject.setStartTranspara(startTransParas);
                newObject.setOnMove(true);
            }
        });
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
    public void setOnLongClickListener(OnTouchListener l) {
        mLongClickListener = l;
    }

    @Override
    public void onRenderFinish(SECamera camera) {
        super.onRenderFinish(camera);
        setVisible(false);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mDisableTouch) {
            return true;
        }

        return super.dispatchTouchEvent(event);
    }

    /**
     * 记住开始左右滑动ObjectsMenu的位置,当X速度超过5000时向右滑动一屏即mFaceIndex=mDownFaceIndex-
     * SHOW_BOX_LINE； 当X速度超过-5000时向左滑动一屏即mFaceIndex=mDownFaceIndex+SHOW_BOX_LINE
     */
    private int mDownFaceIndex;

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            setPreTouch();
            if ((mVelocityAnimation != null && !mVelocityAnimation.isFinish())) {
                mDownFaceIndex = Math.round(mFaceIndex);
                stopAllAnimation(null);
                return true;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (Math.abs(getPreTouchX() - getTouchX()) > getTouchSlop() / 2) {
                mDownFaceIndex = Math.round(mFaceIndex);
                setPreTouch();
                stopAllAnimation(null);
                return true;
            }
            break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        trackVelocity(ev);
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mDownFaceIndex = Math.round(mFaceIndex);
            setPreTouch();
            break;
        case MotionEvent.ACTION_MOVE:
            float dFaceIndex = (getTouchX() - getPreTouchX()) / mBoxWidth;
            mFaceIndex = mFaceIndex - dFaceIndex;
            float maxFaceIndex = mColumnNum - SHOW_BOX_LINE;
            if (mFaceIndex < -0.5f) {
                mFaceIndex = -0.5f;
            } else if (mFaceIndex > maxFaceIndex + 0.5f) {
                mFaceIndex = maxFaceIndex + 0.5f;
            }
            updateScrollFaceIndex();
            updateLoadingModel();
            setPreTouch();
            break;
        case MotionEvent.ACTION_UP:
            setPreTouch();
        case MotionEvent.ACTION_CANCEL:
            float fromIndex = mFaceIndex;
            int toIndex;
            float velocity = mVelocityTracker.getXVelocity() / mHomeScene.getScreenDensity();
            if (Math.abs(velocity) < 100) {
                toIndex = Math.round(fromIndex);
            } else if (Math.abs(velocity) < 500) {
                if (velocity > 0) {
                    toIndex = (int) fromIndex;
                } else {
                    toIndex = (int) fromIndex + 1;
                }
            } else if (Math.abs(velocity) < 5000) {
                if (velocity > 0) {
                    toIndex = mDownFaceIndex - SHOW_BOX_LINE;
                } else {
                    toIndex = mDownFaceIndex + SHOW_BOX_LINE;
                }
            } else {
                toIndex = Math.round(fromIndex - velocity / 2000);
            }
            if (toIndex < 0) {
                toIndex = 0;
            } else if (toIndex > mColumnNum - SHOW_BOX_LINE) {
                toIndex = mColumnNum - SHOW_BOX_LINE;
            }
            int animationTimes = (int) (Math.sqrt(Math.abs(toIndex - fromIndex)) * 30);
            mVelocityAnimation = new SEEmptyAnimation(mHomeScene, fromIndex, toIndex, animationTimes) {
                @Override
                public void onAnimationRun(float value) {
                    mFaceIndex = value;
                    updateScrollFaceIndex();
                    updateLoadingModel();
                }
            };
            mVelocityAnimation.setInterpolator(new DecelerateInterpolator(2.5f));
            mVelocityAnimation.execute();
            break;
        }
        return true;
    }

    public void show(OBJECTSHOWTYPE objectShowType) {
        if (!mHomeScene.getStatus(HomeScene.STATUS_OBJ_MENU)) {
            mLastUpdateIndex = -SHOW_BOX_LINE;
            mObjectShowType = objectShowType;
            mDisableTouch = true;
            stopAllAnimation(null);
            mHomeScene.setStatus(HomeScene.STATUS_OBJ_MENU, true);
            mHomeScene.setTouchDelegate(this);
            mHomeScene.moveToWallSight(new SEAnimFinishListener() {
                public void onAnimationfinish() {
                    mHomeScene.hideDesk(new SEAnimFinishListener() {
                        public void onAnimationfinish() {
                            setVisible(true);
                            initMenuItems();
                            updatePosition();
                            updateScrollFaceIndex();
                            updateLoadingModel();
                            mDisableTouch = false;
                        }
                    });
//                    Desk desk = mHomeScene.getDesk();
//                    if (desk != null) {
//                        desk.hide(new SEAnimFinishListener() {
//                            public void onAnimationfinish() {
//                                setVisible(true);
//                                initMenuItems();
//                                updatePosition();
//                                updateScrollFaceIndex();
//                                updateLoadingModel();
//                                mDisableTouch = false;
//                            }
//                        });
//                    } else {
//                        setVisible(true);
//                        initMenuItems();
//                        updatePosition();
//                        updateScrollFaceIndex();
//                        updateLoadingModel();
//                        mDisableTouch = false;
//                    }
                }
            });
        }
    }

    public void hide(boolean fast, final SEAnimFinishListener l) {
        if (mHomeScene.getStatus(HomeScene.STATUS_OBJ_MENU)) {
            stopAllAnimation(null);
            mDisableTouch = true;
//            final Desk desk = mHomeScene.getDesk();
            if (fast) {
                mHomeScene.removeTouchDelegate();
                setVisible(false);
//                if (desk != null) {
//                    desk.show(null);
//                }
                mHomeScene.showDesk(null);
                mHomeScene.setStatus(HomeScene.STATUS_OBJ_MENU, false);
                releaseMenuItems();
                if (l != null) {
                    l.onAnimationfinish();
                }

            } else {
                // 在做隐藏动画前摆正ObjectsMenu位置
                float fromIndex = mFaceIndex;
                int toIndex = Math.round(fromIndex);
                if (toIndex < 0) {
                    toIndex = 0;
                } else if (toIndex > mColumnNum - SHOW_BOX_LINE) {
                    toIndex = mColumnNum - SHOW_BOX_LINE;
                }
                int animationTimes = (int) (Math.sqrt(Math.abs(toIndex - fromIndex)) * 30);
                SEEmptyAnimation adjustPositionAnim = new SEEmptyAnimation(mHomeScene, fromIndex, toIndex,
                        animationTimes) {
                    @Override
                    public void onAnimationRun(float value) {
                        mFaceIndex = value;
                        updateScrollFaceIndex();
                    }
                };
                adjustPositionAnim.setInterpolator(new DecelerateInterpolator(2.5f));

                final SEEmptyAnimation cuttingAnim = new SEEmptyAnimation(mHomeScene, 0, 1, 15) {
                    @Override
                    public void onAnimationRun(float value) {
                        int moveToleftIndex = (Math.round(mFaceIndex) + SHOW_BOX_LINE / 2) * SHOW_BOX_COLUMN;
                        int index = 0;
                        float moveX = value * mBoxWidth;
                        for (MenuItem menuItem : mMenuItems) {
                            if (index < moveToleftIndex) {
                                menuItem.setUserTranslate(new SEVector3f(-moveX, 0, 0));
                            } else {
                                menuItem.setUserTranslate(new SEVector3f(moveX, 0, 0));
                            }
                            index++;
                        }
                    }
                };
                cuttingAnim.setAnimFinishListener(new SEAnimFinishListener() {
                    public void onAnimationfinish() {
                        mHomeScene.removeTouchDelegate();
                        setVisible(false);
//                        if (desk != null) {
//                            desk.show(null);
//                        }
                        mHomeScene.showDesk(null);
                        mHomeScene.setStatus(HomeScene.STATUS_OBJ_MENU, false);
                        releaseMenuItems();
                        if (l != null) {
                            l.onAnimationfinish();
                        }
                    }
                });
                adjustPositionAnim.setAnimFinishListener(new SEAnimFinishListener() {
                    public void onAnimationfinish() {
                        cuttingAnim.execute();
                    }
                });
                adjustPositionAnim.execute();

            }

        }
    }

    public boolean handleBackKey(SEAnimFinishListener l) {
        if (mHomeScene.getStatus(HomeScene.STATUS_OBJ_MENU)) {
            hide(false, l);
            return true;
        }
        return false;
    }

    @Override
    public void stopAllAnimation(SEAnimFinishListener l) {
        if (mVelocityAnimation != null) {
            mVelocityAnimation.stop();
        }
    }

    /**
     * Release the preview objects, but don't release the preloaded models
     */
    private void releaseMenuItems() {
        for (MenuItem item : mMenuItems) {
            removeChild(item, true);
        }
        mMenuItems.clear();
    }

    private int mCurMaxItemIndex = 1;

    private void initMenuItems() {
        mMenuItems = new ArrayList<MenuItem>();
        List<ModelInfo> showObjectsinfo;
        if (mObjectShowType == OBJECTSHOWTYPE.ALL) {
            showObjectsinfo = HomeManager.getInstance().getModelManager().getModelsCanBeShowedInMenu();
        } else {
            showObjectsinfo = HomeManager.getInstance().getModelManager().findModelInfoByType("Desk");
        }
        // 盒子的总数必须为SHOW_BOX_COLUMN 的倍数且最少显示一屏
        int boxSize = showObjectsinfo.size();
        if (boxSize < SHOW_BOX_COLUMN * SHOW_BOX_LINE) {
            mColumnNum = SHOW_BOX_LINE;
        } else {
            if (boxSize % SHOW_BOX_COLUMN == 0) {
                mColumnNum = boxSize / SHOW_BOX_COLUMN;
            } else {
                mColumnNum = boxSize / SHOW_BOX_COLUMN + 1;
            }
        }
        boxSize = mColumnNum * SHOW_BOX_COLUMN;
        for (int i = 0; i < boxSize; i++) {
            MenuItem menuItem = new MenuItem(mHomeScene, "menu_item_" + mCurMaxItemIndex);
            addChild(menuItem, true);
            // 创建盒子，并且把盒子挂载至menuItem节点下
            menuItem.mBoxBackground = new SEObject(mHomeScene, "group_showbox", mCurMaxItemIndex);
            HomeManager.getInstance().getModelManager().createQuickly(menuItem, menuItem.mBoxBackground);
            // 获取索引号为0的模型，所有被克隆出来的物体都是通过该物体克隆出来的
            // 把模型也挂载到menuItem节点下
            if (i < showObjectsinfo.size()) {
                menuItem.mModelInfo = showObjectsinfo.get(i);
                if (menuItem.mModelInfo.hasInstance()) {
                    menuItem.mModelObject = menuItem.mModelInfo.getInstances().get(0);
                    menuItem.mModelObject.changeParent(menuItem);
                    menuItem.mModelObject.setVisible(true);
                }
            }
            menuItem.initClickListener();
            mMenuItems.add(menuItem);
            mCurMaxItemIndex++;
        }
        // 排列MenuItem，空的盒子放在后面，物体排列方式从上到下从左到右
        Collections.sort(mMenuItems, new SortMenuItem());
    }

    /**
     * 更新ObjectsMenu在世界坐标下的位置，初始位置显示最左边的两列
     */
    private void updatePosition() {
        // 每个盒子的宽度和高度
        mBoxWidth = getCamera().getWidth() / SHOW_BOX_LINE;
        mBoxHeight = getCamera().getHeight() / SHOW_BOX_COLUMN;

        // 根据LAY_SCALE获取ObjectsMenu摆放的位置, 即相机前0.1(value of LAY_SCALE)倍屏所在的位置
        SEVector3f menuLocation = getCamera().getScreenLocation(LAY_SCALE);
        SEVector3f menuScale = new SEVector3f(LAY_SCALE, LAY_SCALE, LAY_SCALE);
        // 往左偏移半个盒子的位置，使得初始位置刚好显示2(value of SHOW_BOX_LINE)列
        menuLocation.selfAdd(new SEVector3f(-mBoxWidth * LAY_SCALE * (SHOW_BOX_LINE - 1) / 2, 0, 0));
        setLocalTranslate(menuLocation);
        setLocalScale(menuScale);
        if (mFaceIndex > mColumnNum - SHOW_BOX_LINE) {
            mFaceIndex = mColumnNum - SHOW_BOX_LINE;
        }
        int index = 0;
        for (MenuItem menuItem : mMenuItems) {
            // 获取物体摆放的列
            int column = index / SHOW_BOX_COLUMN;
            // 获取物体摆放的行
            int line = index - column * SHOW_BOX_COLUMN;
            float moveX = column * mBoxWidth;
            float moveZ = (getCamera().getHeight() - mBoxHeight) / 2 - line * mBoxHeight;
            menuItem.setLocalTranslate(new SEVector3f(moveX, 0, moveZ));
            // 更新背景盒子的大小
            menuItem.updateBoxSize(mBoxWidth, mBoxHeight);
            menuItem.upModelSize(mBoxWidth, mBoxHeight);
            index++;
        }

    }

    /**
     * 只加载当前的2(value of SHOW_BOX_LINE)列，并且释放不在当前列的且在房间中没有克隆实例的模型
     */
    private int mLastUpdateIndex = -SHOW_BOX_LINE;

    private void updateLoadingModel() {
        int loadingIndex = Math.round(mFaceIndex);
        if (mLastUpdateIndex == loadingIndex) {
            return;
        }
        mLastUpdateIndex = loadingIndex;
        int needPreLoadingLine = NEED_PRE_LOADING_FACES * SHOW_BOX_LINE;
        int preLoadingBoxNumAtLeft = ((needPreLoadingLine - SHOW_BOX_LINE) / 2) * SHOW_BOX_COLUMN;
        int preLoadingBoxNumAtRight = preLoadingBoxNumAtLeft;
        int beginIndex = loadingIndex * SHOW_BOX_COLUMN - preLoadingBoxNumAtLeft;
        int endIndex = (loadingIndex + SHOW_BOX_LINE) * SHOW_BOX_COLUMN + preLoadingBoxNumAtRight;
        int index = 0;
        List<MenuItem> loadingAtFirst = new ArrayList<MenuItem>();
        List<MenuItem> loadingAtLast = new ArrayList<MenuItem>();
        for (MenuItem menuItem : mMenuItems) {
            if (!menuItem.isEmptyBox())
                if (index >= beginIndex && index < endIndex) {
                    if (index >= loadingIndex * SHOW_BOX_COLUMN
                            && index <= (loadingIndex + SHOW_BOX_LINE) * SHOW_BOX_COLUMN) {
                        loadingAtFirst.add(menuItem);
                    } else {
                        loadingAtLast.add(menuItem);
                    }

                } else {
                    menuItem.unLoadModel();
                }
            index++;
        }
        for (MenuItem menuItem : loadingAtFirst) {
            menuItem.loadModel();
        }
        for (MenuItem menuItem : loadingAtLast) {
            menuItem.loadModel();
        }
    }

    /**
     * 更新ObjectsMenu的左右滑动的位置
     */
    private void updateScrollFaceIndex() {
        float moveX = -mFaceIndex * mBoxWidth * LAY_SCALE;
        setUserTranslate(new SEVector3f(moveX, 0, 0));
    }

    public class SortMenuItem implements Comparator<MenuItem> {
        public int compare(MenuItem lhs, MenuItem rhs) {
            int value1;
            if (lhs.isEmptyBox()) {
                value1 = 0;
            } else if (lhs.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_GROUND) {
                value1 = 0;
            } else if (lhs.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_WALL) {
                value1 = 1;
            } else if (lhs.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_DESKTOP) {
                value1 = 2;
            } else {
                value1 = 3;
            }
            int value2;
            if (rhs.isEmptyBox()) {
                value2 = 0;
            } else if (rhs.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_GROUND) {
                value2 = 0;
            } else if (rhs.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_WALL) {
                value2 = 1;
            } else if (rhs.mModelInfo.mSlotType == ObjectInfo.SLOT_TYPE_DESKTOP) {
                value2 = 2;
            } else {
                value2 = 3;
            }

            int result = value2 - value1;
            if (result == 0 && !lhs.isEmptyBox() && !rhs.isEmptyBox()) {
                if ("TouchDialer".equals(lhs.mModelInfo.mType)) {
                    return -1;
                } else if ("TouchDialer".equals(rhs.mModelInfo.mType)) {
                    return 1;
                } else {
                    return Collator.getInstance().compare(lhs.mModelInfo.mType, rhs.mModelInfo.mType);
                }
            }
            return result;
        }
    }

    private class MenuItem extends SEObject {
        public ModelInfo mModelInfo;
        public SEObject mBoxBackground;
        public SEObject mModelObject;


        public boolean isEmptyBox() {
            return mModelInfo == null ? true : false;
        }

        public MenuItem(SEScene scene, String name) {
            super(scene, name);
            setIsGroup(true);
            setPressType(PRESS_TYPE.COLOR);
        }

        public void initClickListener() {
            if (isEmptyBox()) {
                setPressType(PRESS_TYPE.NONE);
                return;
            }
            if ("shop".equals(mModelInfo.mName)) {
                setOnClickListener(new OnTouchListener() {
                    @Override
                    public void run(SEObject obj) {
                        mNeedUpdateMenu = true;
                        Utils.showOnlineObjects(getContext());
                    }
                });
            } else {

                if (mObjectShowType == OBJECTSHOWTYPE.ALL) {
                    setOnLongClickListener(new SEObject.OnTouchListener() {
                        public void run(SEObject obj) {
                            if (mLongClickListener != null && mModelObject != null) {
                                obj.setPressed(false);
                                obj.setTag(mModelInfo.mName);
                                mLongClickListener.run(obj);
                            }
                        }
                    });
                } else {
                    setOnClickListener(new OnTouchListener() {
                        @Override
                        public void run(SEObject obj) {
                            if (mModelObject == null) {
                                return;
                            }
//                            HomeScene scene = mHomeScene;
//                            Desk preDesk = scene.getDesk();
                            String objName = mModelInfo.mName;
//                            preDesk.changeDeskTo(objName);
                            mHomeScene.changeDeskTo(objName);
                            hide(true, null);

                            HomeUtils.staticUsingDesk(getContext(), objName);
                        }
                    });
                }
            }
        }

        public void updateBoxSize(float boxWidth, float boxHeight) {
            if (mBoxBackground == null) {
                return;
            }
            // 背景盒子的宽大概是18，高大概18，纵深大概20
            float scaleX = boxWidth / 18f;
            float scaleZ = boxHeight / 17.8f;
            float scaleY = Math.min(scaleX, scaleZ);
            mBoxBackground.setUserScale(new SEVector3f(scaleX, scaleY, scaleZ));
        }

        public void upModelSize(float boxWidth, float boxHeight) {
            if (!isEmptyBox() && mModelObject != null) {
                // 根据物体的大概大小调整位置，物体居中显示，且最小不得小于空间的1/4，最大不得超过空间的0.8倍
                // 先取得物体的中心点和大小
                SEVector3f size = mModelInfo.mMaxPoint.subtract(mModelInfo.mMinPoint);
                float scale = 1;
                if (size.getX() < mBoxWidth / 2 && size.getZ() < mBoxHeight / 2) {
                    // 模型太小，需要放大些
                    float scaleX = (mBoxWidth / 2) / size.getX();
                    float scaleZ = (mBoxHeight / 2) / size.getZ();
                    scale = Math.min(scaleX, scaleZ);
                } else if (size.getX() > mBoxWidth * 0.8f || size.getZ() > mBoxHeight * 0.8f) {
                    // 模型太大，需要缩小些
                    float scaleX = (mBoxWidth * 0.8f) / size.getX();
                    float scaleZ = (mBoxHeight * 0.8f) / size.getZ();
                    scale = Math.min(scaleX, scaleZ);
                }
                mModelObject.setUserScale(new SEVector3f(scale, scale, scale));

                // 获取物体的缩放后的中心点和大小
                SEVector3f maxPoint = mModelInfo.mMaxPoint.mul(scale);
                SEVector3f minPoint = mModelInfo.mMinPoint.mul(scale);
                SEVector3f center = maxPoint.add(minPoint).div(2);
                size = size.mul(scale);
                // 物体的中心点和盒子的y平面中心点对齐
                SEVector3f move = center.mul(-1);
                // 物体最外面的点不能超出盒子的范围
                float mBoxSizeY = Math.min(mBoxWidth, mBoxHeight);

                mModelObject.setUserTranslate(move);
                if ("Desk".equals(mModelInfo.mType)) {
                    move.selfAdd(new SEVector3f(0, size.getY() / 2, 0));
                    mModelObject.setNeedCullFace(false);
                } else {
                    move.selfAdd(new SEVector3f(0, size.getY() / 2 + mBoxSizeY / 4, 0));
                }
                mModelObject.setUserTranslate(move);
            }
        }

        public void loadModel() {
            if (!isEmptyBox() && mModelObject == null) {
                final SEObject modelObject = new SEObject(mHomeScene, mModelInfo.mName);
                HomeManager.getInstance().getModelManager().createBaseObject(this, modelObject, new Runnable() {
                    public void run() {
                        mModelObject = modelObject;
                        upModelSize(mBoxWidth, mBoxHeight);

                    }
                });
            }
        }

        public void unLoadModel() {
            if (!isEmptyBox()) {
                if (HomeManager.getInstance().getModelManager().releaseBaseModel(mModelInfo)) {
                    mModelObject = null;
                }
            }
        }

        @Override
        public void onRelease() {
            super.onRelease();
            if (mBoxBackground != null) {
                mBoxBackground.getParent().removeChild(mBoxBackground, true);
                HomeManager.getInstance().getModelManager().unRegister(mBoxBackground);
                mBoxBackground = null;
            }
            if (!isEmptyBox()) {
                if (!HomeManager.getInstance().getModelManager().releaseBaseModel(mModelInfo)) {
                    if (mModelInfo.hasInstance()) {
                        mModelInfo.getInstances().get(0).changeParent(mHomeScene.getContentObject());
                        mModelInfo.getInstances().get(0).clearUserColor();
                        mModelInfo.getInstances().get(0).setVisible(false);
                    }
                }
                mModelObject = null;
            }
        }
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        // 更新ObjectsMenu
        if (mNeedUpdateMenu && mHomeScene.getStatus(HomeScene.STATUS_OBJ_MENU) && mObjectShowType == OBJECTSHOWTYPE.ALL) {
            mNeedUpdateMenu = false;
            new SECommand(mHomeScene) {
                public void run() {
                    releaseMenuItems();
                    initMenuItems();
                    updatePosition();
                    mLastUpdateIndex = -SHOW_BOX_LINE;
                    updateScrollFaceIndex();
                    updateLoadingModel();
                }
            }.execute();
        }
    }

}
