package com.borqs.se.engine;

import java.util.ArrayList;
import java.util.List;

import com.borqs.se.home3d.HomeUtils;

import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;

public class SEObjectGroup extends SEObject {
    public List<SEObject> mChildObjects = new ArrayList<SEObject>();
    public SEObject mMotionTarget;

    public SEObjectGroup(SEScene scene, String name, int index) {
        super(scene, name, index);
        mIsNode = true;
    }

    public SEObjectGroup(SEScene scene, String name) {
        super(scene, name);
        mIsNode = true;
    }
    @Override
    public void addChild(SEObject obj, boolean create) {
    	if(obj == null) {
    		Log.e(HomeUtils.TAG, "addChild, obj is null");
    		return ;
    	}
        obj.setParent(this);
        if (!mChildObjects.contains(obj)) {
            mChildObjects.add(obj);
            if (getScene() != null) {
                obj.setScene(getScene());
                if (create) {
                    obj.render();
                }
            }
        }
    }

    @Override
    public void setScene(SEScene scene) {
        super.setScene(scene);
        for (SEObject obj : mChildObjects) {
            obj.setScene(scene);
        }
    }

    @Override
    public void removeChild(SEObject child, boolean release) {
        if (mChildObjects.contains(child)) {
            mChildObjects.remove(child);
            child.onRemoveFromParent(this);
            if (release) {
                child.release();
            }
        }
    }

    @Override
    public void removeAllChild(boolean release) {
        if (release) {
            for (SEObject child : mChildObjects) {
                child.release();
            }
        }
        mChildObjects.clear();
    }

    @Override
    public SEObject findChild(String name, int index) {
        for (SEObject obj : mChildObjects) {
            if (index == obj.mIndex && obj.getName().equals(name)) {
                return obj;
            }
        }

        for (SEObject obj : mChildObjects) {
            if (obj.mIsNode) {
                SEObject found = obj.findChild(name, index);
                if (found != null) {
                    return obj;
                }
            }
        }
        return null;
    }

    @Override
    public SEObject findObject(String name, int index) {
        for (SEObject obj : mChildObjects) {
            if (index == obj.mIndex && obj.getName().equals(name)) {
                return obj;
            }
            if (obj.mIsNode) {
                SEObject found = obj.findObject(name, index);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public void notifySurfaceChanged(int width, int height) {
        super.notifySurfaceChanged(width, height);
        List<SEObject> objects = new ArrayList<SEObject>();
        objects.addAll(mChildObjects);
        for (SEObject object : objects) {
            object.notifySurfaceChanged(width, height);
        }
    }

    @Override
    public boolean handleBackKey(SEAnimFinishListener l) {
        boolean result = false;
        List<SEObject> objects = new ArrayList<SEObject>();
        objects.addAll(mChildObjects);
        for (SEObject object : objects) {
            if (object.handleBackKey(l)) {
                result = true;
            }
        }
        return result;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_CANCEL) {
            setTouch((int) event.getX(), (int) event.getY());
        }
        if (mDepriveTouchListener != null && mDepriveTouchListener.onTouch(this, event)) {
            return true;
        }
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            if (mMotionTarget != null) {
                // this is weird, we got a pen down, but we thought it was
                // already down!
                // XXX: We should probably send an ACTION_UP to the current
                // target.
                mMotionTarget = null;
            }
            if (!onInterceptTouchEvent(event)) {
                if (mChildObjects.size() > 0) {
                    SEObject child = getHitObject(getTouchX(), getTouchY());
                    if (child != null) {
                        if (child.dispatchTouchEvent(event)) {
                            // Event handled, we have a target now.
                            mMotionTarget = child;
                            return true;
                        }
                    }
                }
            }
        }

        final SEObject target = mMotionTarget;
        if (target == null) {
            return super.dispatchTouchEvent(event);
        }
        boolean isUpOrCancel = (action == MotionEvent.ACTION_UP) || (action == MotionEvent.ACTION_CANCEL);
        if (!isUpOrCancel && onInterceptTouchEvent(event)) {
            event.setAction(MotionEvent.ACTION_CANCEL);
            if (!target.dispatchTouchEvent(event)) {
                // target didn't handle ACTION_CANCEL. not much we can do
                // but they should have.
            }
            mMotionTarget = null;
            return true;
        }
        if (isUpOrCancel) {
            mMotionTarget = null;
        }

        return target.dispatchTouchEvent(event);
    }

    public SEObject getHitObject(int x, int y) {
        SEObject hitObj = getScene().getDownHitObject();
        if (hitObj == null) {
            return null;
        }
        SEObject obj = findChild(hitObj.mName, hitObj.mIndex);
        if (obj != null && obj.isClickable()) {
            return obj;
        }
        return null;
    }

    @Override
    public void stopAllAnimation(SEAnimFinishListener l) {
        super.stopAllAnimation(l);
        List<SEObject> objects = new ArrayList<SEObject>();
        objects.addAll(mChildObjects);
        for (SEObject object : objects) {
            object.stopAllAnimation(l);
        }
    }
    @Override
    public void onActivityRestart() {
        List<SEObject> objects = new ArrayList<SEObject>();
        objects.addAll(mChildObjects);
        for (SEObject object : objects) {
            if (object != null) {
                object.onActivityRestart();
            }
        }
    }

    @Override
    public void onActivityPause() {
        List<SEObject> objects = new ArrayList<SEObject>();
        objects.addAll(mChildObjects);
        for (SEObject object : objects) {
            if (object != null) {
                object.onActivityPause();
            }
        }
    }

    @Override
    public void onActivityDestory() {
        List<SEObject> objects = new ArrayList<SEObject>();
        objects.addAll(mChildObjects);
        for (SEObject object : objects) {
            if (object != null) {
                object.onActivityDestory();
            }
        }
    }

    @Override
    public void onActivityResume() {
        List<SEObject> objects = new ArrayList<SEObject>();
        objects.addAll(mChildObjects);
        for (SEObject object : objects) {
            if (object != null) {
                object.onActivityResume();
            }
        }
    }

    @Override
    public void onPressHomeKey() {
        List<SEObject> objects = new ArrayList<SEObject>();
        objects.addAll(mChildObjects);
        for (SEObject object : objects) {
            if (object != null) {
                object.onPressHomeKey();
            }
        }
    }

    @Override
    public void onRelease() {
        super.onRelease();
        List<SEObject> objects = new ArrayList<SEObject>();
        objects.addAll(mChildObjects);
        for (SEObject object : objects) {
            if (object != null) {
                object.onRelease();
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        List<SEObject> objects = new ArrayList<SEObject>();
        objects.addAll(mChildObjects);
        for (SEObject object : objects) {
            if (object != null) {
                object.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
    
    public SEObject travelObject(SEObjectTravel travel) {
        for (SEObject obj : mChildObjects) {
          if (travel.travel(obj)) {
              return obj;
          }
            if (obj.mIsNode) {
                SEObject found = obj.travelObject(travel);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

}
