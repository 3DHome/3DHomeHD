package com.borqs.framework3d.home3d;

import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEAnimFinishListener;
import com.borqs.se.engine.SEObject;
import com.borqs.se.widget3d.ObjectInfo.ObjectSlot;

import java.util.List;

/// Abstract vessel object of Launcher dock function, e.g., round desk or
/// box with rectangle face.
/// DockObject present
/// 1. properties for its slot, e.g., slot number, z-height, radius of
/// its face, which could be used to calculate location of each slot.
/// 2. geometry properties
/// 3. shape properties
/// 4. scene info identified as DockInfo, a partition of SceneInfo.

public abstract class DockObject extends ConflictVesselObject {
    private DockSceneInfo mInfo;
    public DockObject(SEScene scene, String name, int index) {
        super(scene, name, index);
        mInfo = scene.mSceneInfo.mDockSceneInfo;
    }

    public int getCount() {
        return mInfo.mDeskNum;
    }
    public float getBorderRadius() {
        return mInfo.mDeskRadius;
    }
    public float getBorderHeight() {
        return mInfo.mDeskHeight;
    }

    public SEVector.SEVector3f getSlotPosition(String objectName, int slotIndex) {
        return null;
    }

    public abstract ConflictObject getConflictSlot(ObjectSlot cmpSlot);
    public abstract ObjectSlot calculateNearestSlot(SEVector3f location, boolean handup);
    public abstract List<ConflictObject> getExistentSlot(SEObject movingObject);

    public abstract SEVector3f getSlotPosition(ObjectSlot objectSlot);

    public abstract void playConflictAnimationTask(ConflictObject conflictObject, long delay, final SEAnimFinishListener postExecutor);
    public abstract void cancelConflictAnimationTask();

    public void show(SEAnimFinishListener l) {
    }

    public void hide(SEAnimFinishListener l) {
    }

    /// conflict task begin
    public static class ConflictAnimationTask implements Runnable {
        private ConflictObject mMyConflictObject;
        private SEAnimFinishListener mPostExecutor;

        public ConflictAnimationTask(ConflictObject conflictObject, final SEAnimFinishListener postExecutor) {
            mMyConflictObject = conflictObject;
            mPostExecutor = postExecutor;
        }

        public void run() {
            mMyConflictObject.playConflictAnimation(mPostExecutor);

        }
    }
}
