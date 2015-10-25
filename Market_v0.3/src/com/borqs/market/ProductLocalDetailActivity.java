package com.borqs.market;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.borqs.market.fragment.ProductLocalDetailFragment;
import com.borqs.market.listener.ViewListener;
import com.borqs.market.utils.IntentUtil;

public class ProductLocalDetailActivity extends BasicActivity implements ViewListener{
    ProductLocalDetailFragment detailFragment = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.product_detail_activity);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
        String product_name = getIntent().getStringExtra(IntentUtil.EXTRA_KEY_NAME);
        String supportMod = getIntent().getStringExtra(IntentUtil.EXTRA_MOD);
        setTitle(product_name);
        
        if(savedInstanceState == null) {
            detailFragment = new ProductLocalDetailFragment();
            detailFragment.setArguments(ProductLocalDetailFragment.getArguments(supportMod));
            getSupportFragmentManager().beginTransaction()
            .add(R.id.fragment_container, detailFragment).commit();
        }else {
            detailFragment = (ProductLocalDetailFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            if (detailFragment != null) {
                detailFragment.onRefresh();
            }
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void show(boolean showAnimation) {
        if(title_container != null && title_container.getVisibility() != View.VISIBLE) {
            if(showAnimation) {
                Animation am = AnimationUtils.loadAnimation(thiz,R.anim.slide_in_down_self);
                title_container.startAnimation(am);
            }
            title_container.setVisibility(View.VISIBLE);
        }
        
    }
    @Override
    public void hide(boolean showAnimation) {
        if(title_container != null && title_container.getVisibility() == View.VISIBLE) {
            if(showAnimation) {
                Animation am = AnimationUtils.loadAnimation(thiz,R.anim.slide_out_up_self);
                title_container.startAnimation(am);
            }
              title_container.setVisibility(View.GONE);
          }
    }
    @Override
    public void showOrHide(boolean showAnimation) {
        if(title_container != null) {
            if(title_container.getVisibility() == View.VISIBLE) {
                if(showAnimation) {
                    Animation am = AnimationUtils.loadAnimation(thiz,R.anim.slide_out_up_self);
                    title_container.startAnimation(am);
                }
              title_container.setVisibility(View.GONE);
              if(detailFragment != null) {
                  detailFragment.hideBottom();
              }
            }else {
                if(showAnimation) {
                    Animation am = AnimationUtils.loadAnimation(thiz,R.anim.slide_in_down_self);
                    title_container.startAnimation(am);
                }
              title_container.setVisibility(View.VISIBLE);
              if(detailFragment != null) {
                  detailFragment.showBottom();
              }
            }
       }
    }
}