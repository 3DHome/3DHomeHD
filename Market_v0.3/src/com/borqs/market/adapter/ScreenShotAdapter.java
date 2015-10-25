package com.borqs.market.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.text.TextUtils;

import com.borqs.market.fragment.PhotoFragment;
import com.borqs.market.fragment.ProductDescriptionFragment;
import com.borqs.market.json.Product;
import com.borqs.market.json.Product.SupportedMod;

public class ScreenShotAdapter  extends FragmentPagerAdapter {
    
    private Product mData = null;
    private boolean isLandscape;
    private ProductDescriptionFragment desFragment;
    public ScreenShotAdapter(FragmentManager fm,Product data) {
        super(fm);
        mData = data;
        if(mData != null && !TextUtils.isEmpty(mData.supported_mod) && mData.supported_mod.contains(SupportedMod.LANDSCAPE)) {
            isLandscape = true;
        }
    }

    @Override
    public int getCount() {
        int count = 1;
        if (mData != null && mData.screenshots != null) {
            count = mData.screenshots.size();
            return count==0?1:count + 1;
        }
        return count;
    }

    @Override
    public Fragment getItem(int position) {
        if(position == 0) {
            if(desFragment == null) {
                desFragment = new ProductDescriptionFragment(mData);
            }
            return desFragment;
        }else {
            PhotoFragment f = new PhotoFragment();
            f.setArguments(PhotoFragment.getArguments(mData.screenshots.get(position - 1),isLandscape));
            return f;
        }
    }
    
    public void alertData(Product data) {
    	mData = data;
    	notifyDataSetChanged();
    	if(desFragment != null) {
    	    desFragment.notifyDataChange(data);
    	}
    }

}
