package com.borqs.market;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.borqs.market.account.AccountSession;
import com.borqs.market.fragment.ProductListFragment;
import com.borqs.market.fragment.ProductLocalListFragment;
import com.borqs.market.json.Product;
import com.borqs.market.json.Product.ProductType;
import com.borqs.market.utils.MarketConfiguration;
import com.borqs.market.utils.MarketUtils;
import com.borqs.market.wallpaper.RawPaperItem;
import com.borqs.market.wallpaper.WallpaperUtils;

public class WallPaperLocalProductListActivity extends BasicActivity implements
ActionBar.TabListener {
    private final String TAG = "WallPaperLocalProductListActivity";

    private FragmentManager mFragmentManager;
//    private ProductLocalListFragment productFragment;
    private final String TAG_FRAGMENT_ONLINE = "TAG_FRAGMENT_ONLINE";
    private final String TAG_FRAGMENT_LOCAL = "TAG_FRAGMENT_LOCAL";
    private String supported_mod;
    private ArrayList<RawPaperItem> wallPaperItems;
    
    private ActionBar.Tab tabLocal = null;
    private ActionBar.Tab tabOnline = null;
    private MyAdapter mAdapter;
    private ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        parseIntent();
        
        if(MarketUtils.CATEGORY_THEME.equals(categoryStr)) {
            getActionBar().setTitle(R.string.top_navigation_theme);
        }else if(MarketUtils.CATEGORY_OBJECT.equals(categoryStr)){
            getActionBar().setTitle(R.string.top_navigation_object);
        }else if(MarketUtils.CATEGORY_WALLPAPER.equals(categoryStr)){
            getActionBar().setTitle(R.string.top_navigation_wallpaper);
        }
        
//        productFragment = new ProductLocalListFragment(categoryStr, localPath, supported_mod);
        
        
        tabOnline = getActionBar().newTab();
        tabOnline.setText(R.string.tab_online);
        tabOnline.setTabListener(this);
        getActionBar().addTab(tabOnline , 0);
        
        tabLocal = getActionBar().newTab();
        tabLocal.setText(R.string.tab_local);
        tabLocal.setTabListener(this);
        getActionBar().addTab(tabLocal , 1);
        
//        getActionBar().setSelectedNavigationItem(tab_index);
        
        
        mFragmentManager = getSupportFragmentManager();
        if (savedInstanceState != null) {
            onLineFragment = (ProductListFragment)mFragmentManager.getFragment(savedInstanceState, TAG_FRAGMENT_ONLINE);
            localFragment = (ProductLocalListFragment)mFragmentManager.getFragment(savedInstanceState, TAG_FRAGMENT_LOCAL);
            tab_index = savedInstanceState.getInt("tab_index");
        }
//        mFragmentManager.beginTransaction()
//                .add(R.id.fragment_container, productFragment, TAG_FRAGMENT)
//                .commit();

        mAdapter = new MyAdapter(getSupportFragmentManager());

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
//        mPager.setCurrentItem(tab_index);

        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                tab_index = position;
                // if (position == Category.THEME.ordinal()) {
                // getSupportActionBar().selectTab(tabTheme);
                // } else {
                // getSupportActionBar().selectTab(tabObj);
                // }
                getActionBar().setSelectedNavigationItem(position);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
                // TODO Auto-generated method stub
            }
        });
    }
    
    private ProductLocalListFragment localFragment = null;
    private ProductListFragment onLineFragment = null;

    class MyAdapter extends FragmentPagerAdapter {
        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int position) {
             if (position == 0) {
                 if (onLineFragment == null) {
                     onLineFragment = new ProductListFragment();
                     onLineFragment.setArguments(ProductListFragment.getArguments(categoryStr, supported_mod, true, orderBy));
                 }
                 return onLineFragment;
             } else {
                 if (localFragment == null) {
                     localFragment = new ProductLocalListFragment();
                     localFragment.setArguments(ProductLocalListFragment.getArguments(categoryStr, supported_mod));
                 }
                 return localFragment;
             }
        }
    }

    private String categoryStr;
    private String orderBy;
    private void parseIntent() {
//        localPath = getIntent().getStringExtra(MarketUtils.EXTRA_LOCAL_PATH);
        supported_mod = getIntent().getStringExtra(MarketUtils.EXTRA_MOD);
        if (TextUtils.isEmpty(MarketConfiguration.PACKAGE_NAME)) {
            throw new IllegalArgumentException("package name is null");
        }
        categoryStr = getIntent().getStringExtra(MarketUtils.EXTRA_CATEGORY);
        orderBy = getIntent().getStringExtra(MarketUtils.EXTRA_ORDER_BY);
        wallPaperItems = (ArrayList<RawPaperItem>)getIntent().getSerializableExtra(WallpaperUtils.EXTRA_RAW_WALL_PAPERS);
        //categoryStr = getIntent().getStringExtra(MarketUtils.EXTRA_CATEGORY);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mFragmentManager.putFragment(outState, TAG_FRAGMENT_ONLINE, onLineFragment);
        mFragmentManager.putFragment(outState, TAG_FRAGMENT_LOCAL, localFragment);
        outState.putInt("tab_index", tab_index);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            onLineFragment = (ProductListFragment)mFragmentManager.getFragment(savedInstanceState, TAG_FRAGMENT_ONLINE);
            localFragment = (ProductLocalListFragment)mFragmentManager.getFragment(savedInstanceState, TAG_FRAGMENT_LOCAL);
            tab_index = savedInstanceState.getInt("tab_index");
        }
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean ret = super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.menu_export_wallpaper).setVisible(true);
        if(AccountSession.isLogin()) {
            menu.findItem(R.id.menu_export_wallpaper).setEnabled(true);
        }else {
            menu.findItem(R.id.menu_export_wallpaper).setEnabled(false);
        }

        return ret;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_export_wallpaper) {
            if (AccountSession.isLogin) {
                WallpaperUtils.startExportOrImportIntent(this, Product.SupportedMod.PORTRAIT.equalsIgnoreCase(supported_mod), wallPaperItems);
            } else {
                login();
            }
        }else if (itemId == R.id.menu_refresh) {
            if(tab_index == 0) {
                if(onLineFragment != null) {
                    onLineFragment.onRefresh();
                }
            }else if(tab_index == 1) {
                if(localFragment != null) {
                    localFragment.onRefresh();
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public interface ActionListener {
        void onrefresh();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        onLineFragment = null;
        localFragment = null;
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        // TODO Auto-generated method stub
        
    }

    int tab_index = 0;
    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        if (mPager != null) {
            tab_index = tab.getPosition();
            mPager.setCurrentItem(tab_index, true);
        }
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        // TODO Auto-generated method stub
        
    }
}
