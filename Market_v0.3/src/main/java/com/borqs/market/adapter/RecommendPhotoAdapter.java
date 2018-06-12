package com.borqs.market.adapter;

import java.util.ArrayList;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.borqs.market.fragment.RecommendPhotoFragment;
import com.borqs.market.json.Recommend;

public class RecommendPhotoAdapter  extends FragmentPagerAdapter {
    
    private ArrayList<Recommend> mDatas = null;
    private String supported_mod;
    private String category;
    public RecommendPhotoAdapter(FragmentManager fm,ArrayList<Recommend> datas, String supported_mod, String category) {
        super(fm);
        mDatas = datas;
        this.supported_mod = supported_mod;
        this.category = category;
    }

    @Override
    public int getCount() {
        return mDatas == null? 0 : mDatas.size();
    }
    
    @Override
    public Fragment getItem(int position) {
        if(mDatas != null && mDatas.size() > position) {
            RecommendPhotoFragment f = new RecommendPhotoFragment(mDatas.get(position),supported_mod, category);
            return f;
        }
        return null;
    }
    
    public void notifyDataSetChanged(ArrayList<Recommend> mDatas) {
        this.mDatas = mDatas;
        super.notifyDataSetChanged();
    }

}
