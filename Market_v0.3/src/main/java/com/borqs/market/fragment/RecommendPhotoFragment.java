package com.borqs.market.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.borqs.market.R;
import com.borqs.market.json.Recommend;
import com.borqs.market.utils.ImageRun;
import com.borqs.market.utils.IntentUtil;
import com.borqs.market.utils.MarketUtils;
import com.borqs.market.utils.QiupuConfig;

public class RecommendPhotoFragment extends Fragment {
    public static final String TAG = RecommendPhotoFragment.class.getSimpleName();
    private final String KEY_DATA = "KEY_DATA";
    private final String KEY_MOD = "KEY_MOD";
    private final String KEY_CATEGORY = "KEY_CATEGORY";
    private Recommend mData;
    private String supported_mod;
    private String category;

    public interface ClickListener {
        void onclick();
    }

    public RecommendPhotoFragment() {
        super();
    }

    public RecommendPhotoFragment(Recommend data, String supported_mod, String category) {
        super();
        this.mData = data;
        this.supported_mod = supported_mod;
        this.category = category;
    }

    // MainHandler mHandler;
    View layout_process;
    public ImageView img_recommend;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.recommend_imageview, null);
        img_recommend = (ImageView) view.findViewById(R.id.img_recommend);
        if(savedInstanceState != null) {
            mData = (Recommend)savedInstanceState.getParcelable(KEY_DATA);
            supported_mod = savedInstanceState.getString(KEY_MOD);
            category = savedInstanceState.getString(KEY_CATEGORY);
        }
        if(mData != null) {
            initPad(mData, img_recommend);
        }
        return view;
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(KEY_DATA, mData);
        outState.putString(KEY_MOD, supported_mod);
        outState.putString(KEY_CATEGORY, category);
        super.onSaveInstanceState(outState);
    }

    private void initPad(Recommend recommend, ImageView img_recommend) {
        if (recommend.type == Recommend.TYPE_USER_SHARE) {
            img_recommend.setImageResource(R.drawable.picture_loading);
            img_recommend.setOnClickListener(getOnClickListener(recommend));
        }

        if (!TextUtils.isEmpty(recommend.promotion_image)) {
            img_recommend.setImageResource(R.drawable.picture_loading);
            downloadPhoto(recommend.promotion_image, img_recommend);
            img_recommend.setOnClickListener(getOnClickListener(recommend));
        }
    }

    private View.OnClickListener getOnClickListener(final Recommend recommend) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(recommend.type == Recommend.TYPE_SINGLE_PRODUCT) {
                    IntentUtil.startProductDetailActivity(getActivity(), recommend.target, 0, null, supported_mod);
                }else if(recommend.type == Recommend.TYPE_PARTITION) {
                    MarketUtils.startPartitionseListIntent(getActivity().getApplicationContext(), category,supported_mod,recommend.name,recommend.target);
                }else if(recommend.type == Recommend.TYPE_TAG) {
                    //TODO 
                }else if(recommend.type == Recommend.TYPE_USER_SHARE) {
                    MarketUtils.startUserShareListIntent(getActivity().getApplicationContext(), category,
                            supported_mod);
                }else if(recommend.type == Recommend.TYPE_ORDER_BY) {
                   MarketUtils.startProductListIntent(getActivity().getApplicationContext(), category, false, supported_mod, false, recommend.target);
                }
            }
        };
    }

    private void downloadPhoto(String url, final ImageView imageView) {
        ImageRun photo_1 = new ImageRun(null, url, 0);
        photo_1.addHostAndPath = true;
        photo_1.default_image_index = QiupuConfig.DEFAULT_IMAGE_INDEX_PHOTO;
//        final Resources resources = imageView.getResources();
//        BLog.d(TAG, "desity="+resources.getDisplayMetrics().density);
//        photo_1.width = (int)(300*resources.getDisplayMetrics().density);
//        photo_1.need_scale = true;
//        photo_1.maxNumOfPixels = 480*512;
        photo_1.noimage = true;
        photo_1.setImageView(imageView);
        photo_1.post(null);
    }
    
}
