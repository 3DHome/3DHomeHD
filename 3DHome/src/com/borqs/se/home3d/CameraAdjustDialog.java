package com.borqs.se.home3d;

import com.borqs.se.R;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SECamera;
import com.borqs.se.engine.SECommand;
import com.borqs.se.engine.SEVector.SEVector3f;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class CameraAdjustDialog extends Dialog implements OnSeekBarChangeListener, Button.OnClickListener {
    public static final String SHOW_CAMERA_ADJUST_PANE_INTENT = "borqs.intent.action.camera.adjust";
    private SeekBar mDownupSeekBar;
    private SeekBar mInoutSeekBar;
    private Button mBtnReset;
    private TextView mCameraLocText;
    private TextView mCameraFovText;
    private float mMinCameraRadius;
    private float mMaxCameraRadius;
    private float mMinCameraDownUp;
    private float mMaxCameraDownUp;
    private SEScene mScene;
    private HomeSceneInfo mSceneInfo;
    private SECamera mCamera;
    private Handler mHandler;

    @Override
    public void show() {
        super.show();
        init();
    }

    public CameraAdjustDialog(Context context) {
        super(context, R.style.AdjustCameraDialog);
        setContentView(R.layout.camera_adjust_pane);
        getWindow().setWindowAnimations(R.style.adjust_camera_menu_anim_style);
        init();
    }

    private void init() {
        mScene = HomeManager.getInstance().getHomeScene();
        mSceneInfo = HomeManager.getInstance().getHomeScene().getHomeSceneInfo();
        mCamera = HomeManager.getInstance().getHomeScene().getCamera();
        mMinCameraRadius = mSceneInfo.getCameraMinRadius();
        mMaxCameraRadius = mSceneInfo.getCameraMaxRadius();
        mMinCameraDownUp = mSceneInfo.getCameraMinDownUp();
        mMaxCameraDownUp = mSceneInfo.getCameraMaxDownUp();
        float curRadius = mCamera.getRadius();
        float curDownUp = mCamera.getLocation().getZ();
        mDownupSeekBar = (SeekBar) findViewById(R.id.downup);
        mDownupSeekBar.setMax((int) (mMaxCameraDownUp - mMinCameraDownUp));
        mDownupSeekBar.setProgress((int) (curDownUp - mMinCameraDownUp));
        mDownupSeekBar.setOnSeekBarChangeListener(this);

        mInoutSeekBar = (SeekBar) findViewById(R.id.inout);
        mInoutSeekBar.setMax((int) (mMaxCameraRadius - mMinCameraRadius));
        mInoutSeekBar.setProgress((int) (mMaxCameraRadius - curRadius));
        mInoutSeekBar.setOnSeekBarChangeListener(this);

        mBtnReset = (Button) findViewById(R.id.btnReset);
        mBtnReset.setOnClickListener(this);
        setOnDismissListener(new OnDismissListener() {

            public void onDismiss(DialogInterface dialog) {
                if (mLastLocation != null) {
                    mSceneInfo.updateCameraDataToDB(mLastLocation, mLastFov);
                }

            }
        });
        mHandler = new Handler();
        mCameraLocText = (TextView) findViewById(R.id.camera_location_text);
        mCameraFovText = (TextView) findViewById(R.id.camera_fov_text);
        if (mCameraLocText != null) {
            mCameraLocText.setText("x = " + mCamera.getLocation().getX() + ", y = " + mCamera.getLocation().getY()
                    + " , z = " + mCamera.getLocation().getZ());
        }
        if (mCameraFovText != null) {
            mCameraFovText.setText(mCamera.getFov() + "°");
        }
    }

    @Override
    public void onClick(View v) {
        final float curRadius = -mSceneInfo.getThemeInfo().mBestCameraLocation.getY();
        final float curDownUp = mSceneInfo.getThemeInfo().mBestCameraLocation.getZ();
        mDownupSeekBar.setProgress((int) (curDownUp - mMinCameraDownUp));
        mInoutSeekBar.setProgress((int) (mMaxCameraRadius - curRadius));
    }

    private SEVector3f mLastLocation;
    private float mLastFov;

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, boolean fromUser) {
        new SECommand(mScene) {
            public void run() {
                float curDownUp;
                float curRadius;
                if (seekBar.equals(mDownupSeekBar)) {
                    curDownUp = progress + mMinCameraDownUp;
                    curRadius = mCamera.getRadius();
                } else {
                    curRadius = mMaxCameraRadius - progress;
                    curDownUp = mCamera.getLocation().getZ();
                }
                mCamera.getCurrentData().mLocation.set(0, -curRadius, curDownUp);
                mCamera.getCurrentData().mFov = mSceneInfo.getCameraFovByRadius(curRadius);
                mLastLocation = mCamera.getCurrentData().mLocation.clone();
                mLastFov = mCamera.getCurrentData().mFov;
                mCamera.setCamera();
                mHandler.post(new Runnable() {
                    public void run() {
                        if (mCameraLocText != null) {
                            mCameraLocText.setText("x = " + mCamera.getLocation().getX() + ", y = "
                                    + mCamera.getLocation().getY() + " , z = " + mCamera.getLocation().getZ());
                        }
                        if (mCameraFovText != null) {
                            mCameraFovText.setText(mCamera.getFov() + "°");
                        }
                    }
                });

            }
        }.execute();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

}
