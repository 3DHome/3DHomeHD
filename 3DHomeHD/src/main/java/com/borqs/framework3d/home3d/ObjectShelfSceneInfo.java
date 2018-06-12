package com.borqs.framework3d.home3d;

/**
 * Created with IntelliJ IDEA.
 * User: yangfeng
 * Date: 13-5-11
 * Time: 下午9:17
 * To change this template use File | Settings | File Templates.
 */

/// clone from DockSceneInfo, which was used by ObjectsMenu origin depend.
public class ObjectShelfSceneInfo {
    public float mDeskHeight;

    public static ObjectShelfSceneInfo DEFAULT_INFO;
    static {
        DEFAULT_INFO = new ObjectShelfSceneInfo();
        DEFAULT_INFO.mDeskHeight = 100;
    }

    public void update(DockSceneInfo dockSceneInfo) {
        mDeskHeight = dockSceneInfo.mDeskHeight;
    }
}
