package com.borqs.se.download;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.borqs.freehdhome.R;
import com.borqs.se.home3d.UpdateDBThread;
import com.borqs.se.home3d.ProviderUtils.ModelColumns;
import com.borqs.se.home3d.ProviderUtils.ObjectInfoColumns;
import com.borqs.se.engine.SESceneManager;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class LocalObjectPreview extends Activity implements OnClickListener {

    private ProgressBar mLoadImageProgressBar;
    private GalleryFlow mGalleryFlow;
    private ArrayList<Bitmap> mImages;
    private ImageAdapter mImageAdapter;
    private LinearLayout mPage;
    private Button mCancelOrDeleteButton;

    private String mLocalPath;
    private LoadImageThread mLoadImageThread;

    private String mDisplayName;
    private AlertDialog mDeleteDialog;
    private ActionBar mActionBar;

    private class LoadImageThread extends Thread {
        public LoadImageThread(String name) {
            super(name);
        }

        public boolean mStop = false;
    };

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case Constant.MSG_LOAD_IMAGES_FINISHED:
                if (mLoadImageProgressBar.getVisibility() == View.VISIBLE) {
                    mLoadImageProgressBar.setVisibility(View.GONE);
                }
                mImageAdapter = new ImageAdapter(LocalObjectPreview.this, mImages);
                mGalleryFlow.setAdapter(mImageAdapter);
                mPage.removeAllViews();
                for (int i = 0; i < mGalleryFlow.getCount(); i++) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    ImageView dot = new ImageView(LocalObjectPreview.this);
                    if (i == 0) {
                        dot.setBackgroundResource(R.drawable.dot_visible);
                    } else {
                        dot.setBackgroundResource(R.drawable.dot_invisible);
                    }
                    mPage.addView(dot, params);
                }
                break;
            default:
                break;
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.object_local_preview);
        Intent intent = getIntent();
        if (intent != null) {
            mLocalPath = intent.getStringExtra(Constant.INTENT_EXTRA_LOCAL_PATH);
            String[] paths = mLocalPath.split("/");
            mDisplayName = paths[paths.length - 1].replace(".zip", "");
            mActionBar = getActionBar();
            mActionBar.setTitle(mDisplayName);
            mActionBar.setDisplayShowTitleEnabled(true);
            mActionBar.setHomeButtonEnabled(true);
        } else {
            finish();
            return;
        }
        mLoadImageProgressBar = (ProgressBar) findViewById(R.id.load_preview);
        mGalleryFlow = (GalleryFlow) findViewById(R.id.gallery);
        mGalleryFlow.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View view, int position, long arg3) {
                for (int i = 0; i < mPage.getChildCount(); i++) {
                    ImageView dot = (ImageView) mPage.getChildAt(i);
                    if (i == position) {
                        dot.setBackgroundResource(R.drawable.dot_visible);
                    } else {
                        dot.setBackgroundResource(R.drawable.dot_invisible);
                    }
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {
            }

        });

        mCancelOrDeleteButton = (Button) findViewById(R.id.cancel_or_delete);
        mCancelOrDeleteButton.setOnClickListener(this);
        mPage = (LinearLayout) findViewById(R.id.pages);
        updateButtonStatus();
        loadPreviewImage();
    }

    private void loadPreviewImage() {
        if (mImages == null) {
            mImages = new ArrayList<Bitmap>();
        }
        mLoadImageThread = new LoadImageThread("lOAD_image_thread") {
            @Override
            public void run() {
                String[] imagePath = new String[] { mLocalPath + "/1.jpg", mLocalPath + "/2.jpg", mLocalPath + "/3.jpg" };
                for (int i = 0; i < imagePath.length; i++) {
                    if (mStop) {
                        return;
                    }
                    mImages.add(Utils.getLocalDrawable(imagePath[i]));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Message message = mHandler.obtainMessage(Constant.MSG_LOAD_IMAGES_FINISHED);
                mHandler.sendMessage(message);
            }

        };
        mLoadImageThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLoadImageThread != null) {
            mLoadImageThread.mStop = true;
        }
        if (mImageAdapter != null) {
            mImageAdapter.clear();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.cancel_or_delete:
            showDeleteDialog();
            break;
        }
    }

    private void showDeleteDialog() {
        if (mDeleteDialog == null) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Utils.deleteObjects(LocalObjectPreview.this,mLocalPath);
                    if (mLocalPath != null) {
                        String[] config_paths = mLocalPath.split("/");
                        String name = config_paths[config_paths.length - 1];
                        deleteDownload(name, mLocalPath);
                    }
                    updateButtonStatus();
                }
            };
            mDeleteDialog = new AlertDialog.Builder(this)
                    .setIcon(R.drawable.art_dialog_notice)
                    .setPositiveButton(android.R.string.yes, listener)
                    .setNegativeButton(android.R.string.no, null).create();
            String message = getResources().getString(
                    R.string.confirm_delete_selected_objects, mDisplayName);
            String title = getResources().getString(R.string.delete_objects_title);
            mDeleteDialog.setTitle(title);
            mDeleteDialog.setMessage(message);
        }

        mDeleteDialog.show();
    }

    private void deleteDownload(final String name, final String path) {
        finish();
        SESceneManager.getInstance().removeModelFromScene(name);
        UpdateDBThread.getInstance().process(new Runnable() {
            
            @Override
            public void run() {
                ContentResolver resolver = getContentResolver();
                String where = ModelColumns.OBJECT_NAME + "='" + name + "'";
                resolver.delete(ModelColumns.CONTENT_URI, where, null);
                where = ObjectInfoColumns.OBJECT_NAME + "='" + name + "'";
                resolver.delete(ObjectInfoColumns.CONTENT_URI, where, null);
                File fileLocal = new File(path);
                if (fileLocal.isDirectory()) {
                    for (File f : fileLocal.listFiles()) {
                        f.delete();
                    }
                }
                fileLocal.delete();
            }
        });
    }


    private void updateButtonStatus() {
        mCancelOrDeleteButton.setVisibility(View.VISIBLE);
    }

    public class ImageAdapter extends BaseAdapter {

        private Context mContext;
        private List<Bitmap> mGalleryBitmaps;
        private List<ImageView> mImageViews;

        public ImageAdapter(Context c, List<Bitmap> bitmaps) {
            mContext = c;
            mGalleryBitmaps = bitmaps;
            mImageViews = new ArrayList<ImageView>();
            fillImages();
        }

        private boolean fillImages() {
            final int reflectionGap = 4;
            for (Bitmap originalImage : mGalleryBitmaps) {
                if (originalImage == null) {
                    continue;
                }
                int width = originalImage.getWidth();
                int height = originalImage.getHeight();
                Matrix matrix = new Matrix();

                matrix.preScale(1, -1);

                Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0,
                        height / 6 * 5, width, height / 6, matrix, false);

                Bitmap bitmapWithReflection = Bitmap.createBitmap(width,
                        (height + height / 6), Config.ARGB_8888);

                Canvas canvas = new Canvas(bitmapWithReflection);
                canvas.drawBitmap(originalImage, 0, 0, null);

                Paint deafaultPaint = new Paint();
                deafaultPaint.setAntiAlias(false);

                canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);
                reflectionImage.recycle();
                Paint paint = new Paint();
                paint.setAntiAlias(false);

                LinearGradient shader = new LinearGradient(0, originalImage.getHeight(), 0,
                        bitmapWithReflection.getHeight() + reflectionGap, 0x35ffffff, 0x00ffffff,
                        TileMode.MIRROR);
                paint.setShader(shader);
                originalImage.recycle();
                paint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN));
                canvas.drawRect(0, height, width, bitmapWithReflection.getHeight() + reflectionGap,
                        paint);

                ImageView imageView = new ImageView(mContext);
                imageView.setImageBitmap(bitmapWithReflection);

                imageView.setLayoutParams(new GalleryFlow.LayoutParams(mContext.getResources().getDisplayMetrics().widthPixels / 2, LayoutParams.MATCH_PARENT));
                imageView.setScaleType(ScaleType.FIT_CENTER);
                imageView.setPadding(0, 15, 0, 0);
                mImageViews.add(imageView);
            }
            return true;
        }

        public int getCount() {
            return mImageViews.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return mImageViews.get(position);
        }

        public void clear() {
            if (mGalleryBitmaps != null) {
                mGalleryBitmaps.clear();
            }
            if (mImageViews != null) {
                mImageViews.clear();
            }
        }
    }

}