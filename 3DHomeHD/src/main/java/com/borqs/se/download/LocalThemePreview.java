package com.borqs.se.download;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.borqs.freehdhome.R;
import com.borqs.market.utils.MarketUtils;
import com.borqs.se.home3d.UpdateDBThread;
import com.borqs.se.download.GridFragment.Category;
import com.borqs.se.home3d.ProviderUtils.ModelColumns;
import com.borqs.se.home3d.ProviderUtils.ThemeColumns;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.SettingsActivity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class LocalThemePreview extends Activity implements OnClickListener{

//    protected static final String NATIVE_DEFAULT_THEME_PATH = "base/home8";
//    protected static final String NATIVE_SECOND_THEME_PATH = "base/home8/newTexture";
//    protected static final String NATIVE_THIRD_THEME_PATH = "base/home8/darkTexture";
    protected static final String NATIVE_DEFAULT_THEME_PATH = "base/home4mian";
    protected static final String NATIVE_SECOND_THEME_PATH = "base/home4mian/newTexture";
    protected static final String NATIVE_THIRD_THEME_PATH = "base/home4mian/darkTexture";

    private ProgressBar mLoadImageProgressBar;
    private GalleryFlow mGalleryFlow;
    private ArrayList<Bitmap> mImages;
    private ImageAdapter mImageAdapter;
    private LinearLayout mPage;
    private Button mDownloadOrApplyButton;
    private Button mCancelOrDeleteButton;

    private String mLocalPath;
    private Category mCategory;
    private String mThemeConfig = "";

    private LoadImageThread mLoadImageThread;

    private boolean mIsUsing;
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
                mImageAdapter = new ImageAdapter(LocalThemePreview.this, mImages);
                mGalleryFlow.setAdapter(mImageAdapter);
                mPage.removeAllViews();
                for (int i = 0; i < mGalleryFlow.getCount(); i++) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    ImageView dot = new ImageView(LocalThemePreview.this);
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
        this.setContentView(R.layout.theme_local_preview);
        Intent intent = getIntent();
        mActionBar = getActionBar();

        if (intent != null) {
            mCategory = Category.values()[intent.getIntExtra(Constant.INTENT_EXTRA_CATEGORY, 0)];
            mLocalPath = intent.getStringExtra(Constant.INTENT_EXTRA_LOCAL_PATH);
            mThemeConfig = intent.getStringExtra(Constant.INTENT_EXTRA_THEME_CONFIG);
            if (mCategory == Category.DEFAULT_THEME) {
                int i = 0;
                if (NATIVE_DEFAULT_THEME_PATH.equals(mLocalPath)) {
                    i = 0;
                } else if (NATIVE_THIRD_THEME_PATH.equals(mLocalPath)) {
                    i = 2;
                } else if (NATIVE_SECOND_THEME_PATH.equals(mLocalPath)) {
                    i = 1;
                }
                mDisplayName = getResources().getStringArray(R.array.theme_entries_8)[i];
            } else {
                mDisplayName = Utils.parseLastPathSection(mLocalPath);
            }

            Cursor cursor = null;
            try {
                String where = ThemeColumns.FILE_PATH + "='" + mLocalPath + "'";
                cursor = getContentResolver().query(ThemeColumns.CONTENT_URI, null, where,
                        null,
                        null);
                if (cursor != null && cursor.moveToFirst()) {
                    String themeName = cursor.getString(cursor.getColumnIndex(ThemeColumns.NAME));
                    if (!TextUtils.isEmpty(themeName)) {
                        mDisplayName = themeName;
                    }

                    int isApply = cursor.getInt(cursor
                            .getColumnIndexOrThrow(ThemeColumns.IS_APPLY));
                    if (isApply == 1) {
                        mIsUsing = true;
                        mActionBar.setTitle(mDisplayName + "("
                                + getResources().getString(R.string.action_using) + ")");
                    }
                    mThemeConfig = cursor.getString(cursor
                            .getColumnIndexOrThrow(ThemeColumns.CONFIG));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
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

        mDownloadOrApplyButton = (Button) findViewById(R.id.download_or_apply);
        mDownloadOrApplyButton.setOnClickListener(this);
        mCancelOrDeleteButton = (Button) findViewById(R.id.cancel_or_delete);
        mCancelOrDeleteButton.setOnClickListener(this);
        mPage = (LinearLayout) findViewById(R.id.pages);
        updateButtonStatus();
        loadPreviewImage();

        mActionBar.setTitle(mDisplayName);
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setHomeButtonEnabled(true);
    }

    private void loadPreviewImage() {
        if (mImages == null) {
            mImages = new ArrayList<Bitmap>();
        }

        mLoadImageThread = new LoadImageThread("lOAD_image_thread") {
            @Override
            public void run() {
                String[] imagePath = getImagePaths();
                for (int i = 0; i < imagePath.length; i++) {
                    if (mStop) {
                        return;
                    }
                    if (mCategory == Category.DEFAULT_THEME) {
                        mImages.add(Utils.getAssetDrawable(LocalThemePreview.this, imagePath[i]));
                    } else {
                        mImages.add(Utils.getLocalDrawable(imagePath[i]));
                    }

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
        case R.id.download_or_apply:
            mIsUsing = true;
            mActionBar.setTitle(mDisplayName + "(" + getResources().getString(R.string.action_using) + ")");
            Toast.makeText(this, getResources().getString(R.string.notify_user_apply, mDisplayName), Toast.LENGTH_SHORT)
                    .show();
            Utils.markAsApply(this, mLocalPath);
            if (mCategory == Category.DEFAULT_THEME) {
                int i = 0;
                if (NATIVE_DEFAULT_THEME_PATH.equals(mLocalPath)) {
                    i = 0;
                } else if (NATIVE_THIRD_THEME_PATH.equals(mLocalPath)) {
                    i = 2;
                } else if (NATIVE_SECOND_THEME_PATH.equals(mLocalPath)) {
                    i = 1;
                }
                String theme = getResources().getStringArray(R.array.theme_values_8)[i];
                SettingsActivity.saveThemeName(this, theme, mThemeConfig);
            } else {
                SettingsActivity.saveThemeName(this, mDisplayName, mThemeConfig);
            }
            String product_id = HomeUtils.PKG_CURRENT_NAME + "." + mDisplayName;
            MarketUtils.updatePlugIn(LocalThemePreview.this, product_id, true);
            updateButtonStatus();
            finish();
            break;
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
                    if (mIsUsing) {
                        Utils.markAsApply(LocalThemePreview.this, NATIVE_DEFAULT_THEME_PATH);
                        SettingsActivity.saveThemeName(LocalThemePreview.this,  "default", "");
                    }
                    Utils.deleteThemes(LocalThemePreview.this,mLocalPath);
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
                    R.string.confirm_delete_selected_theme, mDisplayName);
            String title = getResources().getString(R.string.delete_theme_title);
            mDeleteDialog.setTitle(title);
            mDeleteDialog.setMessage(message);
        }
        mDeleteDialog.show();
    }

    private void deleteDownload(final String name, final String path) {
        finish();
        UpdateDBThread.getInstance().process(new Runnable() {

            @Override
            public void run() {
                ContentResolver resolver = getContentResolver();
                String theme = name;
                String where = ModelColumns.THEME_NAME + "='" + theme + "'";
                resolver.delete(ModelColumns.IMAGE_INFO_URI, where, null);
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
        if (mCategory == Category.DEFAULT_THEME) {
            mCancelOrDeleteButton.setVisibility(View.GONE);
            if (mIsUsing) {
                mDownloadOrApplyButton.setVisibility(View.GONE);
            } else {
                mDownloadOrApplyButton.setText(R.string.action_apply);
                mDownloadOrApplyButton.setVisibility(View.VISIBLE);
            }
        } else {
            mCancelOrDeleteButton.setVisibility(View.VISIBLE);
            if (mIsUsing) {
                mDownloadOrApplyButton.setVisibility(View.GONE);
            } else {
                mDownloadOrApplyButton.setText(R.string.action_apply);
                mDownloadOrApplyButton.setVisibility(View.VISIBLE);
            }
        }
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

    private String[] getImagePaths() {
//        List<String> screenShot = Utils.getPaperList(mLocalPath + File.pathSeparator + "_images_");
        ArrayList<String> screenShot = new ArrayList<String>();
        Utils.getScreenShotList(mLocalPath, screenShot);
        if (null == screenShot || screenShot.isEmpty()) {
            return new String[] { mLocalPath + "/1.jpg", mLocalPath + "/2.jpg", mLocalPath + "/3.jpg",
                    mLocalPath + "/4.jpg" };
        } else {
            String[] result = new String[screenShot.size()];
            screenShot.toArray(result);
            return result;
        }
    }

}