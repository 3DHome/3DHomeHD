package com.borqs.market.fragment;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.borqs.market.R;
import com.borqs.market.listener.ViewListener;
import com.borqs.market.utils.BLog;
import com.borqs.market.utils.ImageRun;
import com.borqs.market.utils.QiupuConfig;

public class PhotoFragment extends Fragment {
    public static final String TAG = "PhotoFragment";
    private static final String TAG_URL = "TAG_URL";
    
    protected static String ARGUMENTS_KEY_URL= "ARGUMENTS_KEY_URL";
    protected static String ARGUMENTS_KEY_ISLANDSCAPE = "ARGUMENTS_KEY_ISLANDSCAPE";
    private String url;
    private int width;
    private int shoot_width_landscape;
    private int shoot_height_landscape;
    private int shoot_width_portrait;
    private int shoot_height_portrait;
    private int maxNumOfPixels;
    private boolean isLandscape;

    public interface ClickListener {
        void onclick();
    }

    public static Bundle getArguments(String url, boolean isLandscape) {
        Bundle args = new Bundle();
        args.putString(ARGUMENTS_KEY_URL, url);
        args.putBoolean(ARGUMENTS_KEY_ISLANDSCAPE, isLandscape);
        return args;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        url = args.getString(ARGUMENTS_KEY_URL);
        isLandscape = args.getBoolean(ARGUMENTS_KEY_ISLANDSCAPE);
    }
    
    View layout_process;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        shoot_width_landscape = getActivity().getApplicationContext().getResources().getDimensionPixelSize(R.dimen.shoot_screen_width_landscape);
        shoot_height_landscape = getActivity().getApplicationContext().getResources().getDimensionPixelSize(R.dimen.shoot_screen_height_landscape);
        shoot_width_portrait = getActivity().getApplicationContext().getResources().getDimensionPixelSize(R.dimen.shoot_screen_width_portrait);
        shoot_height_portrait = getActivity().getApplicationContext().getResources().getDimensionPixelSize(R.dimen.shoot_screen_height_portrait);
        maxNumOfPixels = getActivity().getApplicationContext().getResources().getInteger(R.integer.shoot_screen_maxNumOfPixels);
        DisplayMetrics dm = getActivity().getApplicationContext().getResources().getDisplayMetrics();
        int h = dm.heightPixels;
        int w = dm.widthPixels;
        width = h>w?w:h;
        maxNumOfPixels = h*w;
        Log.d(TAG, "width==="+width);
    }



    public ImageView img_shoot;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            this.url = savedInstanceState.getString(TAG_URL);
        }
        View view = inflater.inflate(R.layout.photo_fragment, null);
        img_shoot = (ImageView) view.findViewById(R.id.img_shoot);
//        if(isLandscape) {
//            img_shoot.getLayoutParams().width = shoot_width_landscape;
//            img_shoot.getLayoutParams().height = shoot_height_landscape;
//        }else {
//            img_shoot.getLayoutParams().height = shoot_height_portrait;
//            img_shoot.getLayoutParams().width = shoot_width_portrait;
//        }
//        width = img_shoot.getLayoutParams().width;
        if (!TextUtils.isEmpty(url)) {
            downloadPhoto(url, img_shoot);
        }
        view.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if(getActivity() != null && ViewListener.class.isInstance(getActivity())) {
                    ((ViewListener)getActivity()).showOrHide(true);
                }
                
            }
        });
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
        photo_1.width = width;
        photo_1.need_scale = true;
        photo_1.maxNumOfPixels = maxNumOfPixels;
        photo_1.noimage = true;
        imageView.setImageResource(R.drawable.picture_loading);
        photo_1.setImageView(imageView);
        photo_1.post(null);
    }
    
}
