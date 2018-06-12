package com.borqs.market.fragment;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.borqs.market.R;
import com.borqs.market.utils.BLog;
import com.borqs.market.utils.ImageRun;
import com.borqs.market.utils.QiupuConfig;

public class WallpaperPhotoFragment extends Fragment {
    public static final String TAG = "PhotoFragment";
    private static final String TAG_URL = "TAG_URL";
    private static final String ARGUMENTS_KEY_URL = "ARGUMENTS_KEY_URL";
    private String url;

    public interface ClickListener {
        void onclick();
    }

    public static Bundle getArguments(String url) {
        Bundle args = new Bundle();
        args.putString(ARGUMENTS_KEY_URL, url);
        return args;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        url = args.getString(ARGUMENTS_KEY_URL);
    }

    // MainHandler mHandler;
    View layout_process;
    public ImageView img_shoot;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            this.url = savedInstanceState.getString(TAG_URL);
        }
        View view = inflater.inflate(R.layout.wallpaper_photo_fragment, null);
        img_shoot = (ImageView) view.findViewById(R.id.img_shoot);
        // layout_process = view
        // .findViewById(R.id.layout_process);
        //
        // layout_process.setVisibility(View.VISIBLE);
        // mHandler = new MainHandler();
//        img_shoot.setImageResource(R.drawable.resource_preview_bg);
        if (!TextUtils.isEmpty(url)) {
            downloadPhoto(url, img_shoot);
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(TAG_URL, url);
        super.onSaveInstanceState(outState);
    }

    private void downloadPhoto(String url, final ImageView imageView) {
        ImageRun photo_1 = new ImageRun(null, url, 0);
        photo_1.addHostAndPath = true;
        photo_1.default_image_index = QiupuConfig.DEFAULT_IMAGE_INDEX_PHOTO;
        final Resources resources = imageView.getResources();
        BLog.d(TAG, "desity="+resources.getDisplayMetrics().density);
        photo_1.width = (int)(300*resources.getDisplayMetrics().density);
        photo_1.need_scale = true;
        photo_1.maxNumOfPixels = 480*512;
        photo_1.noimage = true;
        imageView.getLayoutParams().height = photo_1.width;
        photo_1.setImageView(imageView);
        photo_1.post(null);
    }
    
}
