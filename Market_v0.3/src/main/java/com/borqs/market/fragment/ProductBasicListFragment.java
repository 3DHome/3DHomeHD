package com.borqs.market.fragment;

import java.util.ArrayList;

import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.borqs.market.R;
import com.borqs.market.adapter.ProductItemView;
import com.borqs.market.adapter.ProductListAdapter;
import com.borqs.market.api.ApiUtil;
import com.borqs.market.db.DownLoadProvider;
import com.borqs.market.db.PlugInColumns;
import com.borqs.market.json.Product;
import com.borqs.market.json.Product.ProductType;
import com.borqs.market.json.Product.SupportedMod;
import com.borqs.market.listener.DownloadListener;
import com.borqs.market.utils.BLog;
import com.borqs.market.utils.DataConnectionUtils;
import com.borqs.market.utils.DownloadUtils;
import com.borqs.market.utils.IntentUtil;
import com.borqs.market.utils.MarketUtils;
import com.borqs.market.utils.QiupuHelper;

public class ProductBasicListFragment extends BasicFragment implements
        OnScrollListener, DownloadListener {

    private static final String TAG = ProductBasicListFragment.class.getSimpleName();
    private static final String TAG_DATAS = "TAG_DATAS";
    private static final String TAG_PRODUCT_TYPE = "TAG_PRODUCT_TYPE";
    protected ListView  mListView;
    protected View  load_more_item;
    protected View layout_more;
    protected ProductListAdapter mAdapter;
    protected ArrayList<Product> mDatas = new ArrayList<Product>();
    protected ApiUtil mApiUtil;
    protected int mPage = 0;
    protected final int count = 20;
    protected boolean hasMore = true;
    protected int totalCount = 0;

    protected String product_type = ProductType.THEME;
    protected String supported_mod;
    protected String orderBy;
    boolean isportrait = false;
    protected boolean showRecommend = false;

    public static Bundle getArguments(String product_type, String supported_mod, boolean showRecommend, String orderBy) {
        Bundle args = new Bundle();
        args.putString(ARGUMENTS_KEY_TYPE, product_type);
        args.putString(ARGUMENTS_KEY_MOD, supported_mod);
        args.putBoolean(ARGUMENTS_KEY_SHOW, showRecommend);
        args.putString(ARGUMENTS_KEY_ORDERBY, orderBy);
        return args;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        product_type = args.getString(ARGUMENTS_KEY_TYPE);
        supported_mod = args.getString(ARGUMENTS_KEY_MOD);
        showRecommend = args.getBoolean(ARGUMENTS_KEY_SHOW);
        orderBy = args.getString(ARGUMENTS_KEY_ORDERBY);
        if(!TextUtils.isEmpty(supported_mod)) {
            if(supported_mod.contains(SupportedMod.PORTRAIT)) {
                isportrait = true;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        QiupuHelper.registerDownloadListener(downloadListener_key, this);
        downLoadObserver = new DownloadOberserver();
        registerContentObserver(DownloadUtils.CONTENT_URI, true, downLoadObserver);
        plugInObserver = new PlugsInObserver();
        registerContentObserver(DownLoadProvider.getContentURI(getActivity().getApplicationContext(), DownLoadProvider.TABLE_PLUGIN), true, plugInObserver);
        
        mListView = (ListView) mConvertView.findViewById(R.id.listview);
        load_more_item = inflater.inflate(R.layout.load_more_item, null);
        layout_more = load_more_item.findViewById(R.id.layout_more);
        mListView.addFooterView(load_more_item);
        mAdapter = new ProductListAdapter(mActivity, mDatas, true, isportrait);
        if(showRecommend) {
            initHeaderView(inflater);
        }
        mListView.setAdapter(mAdapter);
        mListView.setOnScrollListener(this);
        return mConvertView;
    }
    
    protected void initHeaderView(LayoutInflater inflater) {
        
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        BLog.d(TAG, "onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
        
        if (savedInstanceState != null) {
            mDatas = savedInstanceState.getParcelableArrayList(TAG_DATAS);
            this.product_type = savedInstanceState.getString(TAG_PRODUCT_TYPE);
        }
        if (mDatas != null && mDatas.size() > 0) {
            mPage = (mDatas.size()-1) / count;
            BLog.d(TAG, "notifyDataChange");
            notifyDataChange();
        } else {
            BLog.d(TAG, "getProductList....");
            getDatasList();
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                if (mDatas != null && mDatas.get(position) != null) {
                    IntentUtil.startProductDetailActivity(mActivity,
                            mDatas.get(position).product_id,
                            mDatas.get(position).version_code,
                            mDatas.get(position).name,
                            mDatas.get(position).supported_mod);
                }
            }

        });
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mAdapter != null) {
            mAdapter.alertData(mDatas);
        }
    }

    public void onRefresh() {
        hasMore = true;
        mPage = 0;
        getDatasList();
    }

    public void loadMore() {
        mPage++;
        getDatasList();
    }

    protected void notifyDataChange() {
        if (mAdapter == null) {
            mAdapter = new ProductListAdapter(mActivity, mDatas, true, isportrait);
            mListView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged(mDatas);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(TAG_DATAS, mDatas);
        outState.putString(TAG_PRODUCT_TYPE, product_type);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        QiupuHelper.unregisterDownloadListener(downloadListener_key);
        if(downLoadObserver != null) {
            unregisterContentObserver(downLoadObserver);
            downLoadObserver = null; 
        }
        if(plugInObserver != null) {
            unregisterContentObserver(plugInObserver);
            plugInObserver = null; 
        }
        super.onDestroyView();
    }

    protected boolean isLoading;
    protected final Object mLocked = new Object();

    protected void getDatasList() {
        if (isLoading) {
            BLog.v(TAG, "is Loading ...");
            return;
        }
        
        
        if (mPage == 0)
            begin();
        
        if (!DataConnectionUtils.testValidConnection(mActivity)) {
            if (mPage == 0) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showLoadMessage(R.string.dlg_msg_no_active_connectivity, R.drawable.biz_pic_empty_view,true);
                    }
                }, 500);
            }else {
                Toast.makeText(mActivity, R.string.dlg_msg_no_active_connectivity,
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        synchronized (mLocked) {
            isLoading = true;
        }
        
        if(mApiUtil == null) {
            mApiUtil = new ApiUtil();
        }
//        getRecommendList();
//        getProductList();
    }
    
    
    @Override
    protected void createHandler() {
        mHandler = new MainHandler();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(mDatas != null && mDatas.size() >0) {
            for(int i=0; i<mDatas.size(); i++) {
                mDatas.get(i).despose();
            }
            mDatas.clear();
        }
        mAdapter = null;
        mApiUtil = null;
        System.gc();
    }

    protected final static int LOAD_END = 0x001;
    protected final static int MSG_ON_CHANGE = 0x002;
    protected final static int MSG_DOWNLOADING = 0x003;
    protected final static String RESULT = "RESULT";

    private class MainHandler extends Handler {
        public MainHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            if(getActivity() == null) return;
            switch (msg.what) {

            case LOAD_END: {
                mPage = (mDatas.size()-1) / count;
                end();
                layout_more.setVisibility(View.GONE);
                boolean suc = msg.getData().getBoolean(RESULT);
                if (suc) {
                    notifyDataChange();
                    if(mDatas.size() == 0) {
                        showLoadMessage(R.string.empty_list, false);
                    }
                } else {
                    showLoadMessage(R.string.msg_loadi_failed);
                }
                break;
            }
            case MSG_ON_CHANGE: {
                refreshStatusUI();
                break;
            }
            case MSG_DOWNLOADING: {
                refreshProgressUI();
                break;
            }
            }
        }
    }

    @Override
    protected int getInflatelayout() {
        return R.layout.product_list_fragment;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if(mAdapter == null) return;
        int itemsLastIndex = mAdapter.getCount() - 1; 
        if (scrollState == OnScrollListener.SCROLL_STATE_IDLE && visibleLastIndex == itemsLastIndex && hasMore) {  
            if (mDatas == null || mDatas.size() == 0) {
                layout_more.setVisibility(View.GONE);
                return;
            }
            layout_more.setVisibility(View.VISIBLE);
            if (isLoading) {
                BLog.v(TAG, "is Loading ...");
                return;
            }
            loadMore();
        } else {
            layout_more.setVisibility(View.GONE);
        } 
        
    }
    int  visibleLastIndex = 0;  
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        visibleLastIndex = firstVisibleItem + visibleItemCount - 1 - 1 - mListView.getHeaderViewsCount();  
    }

    @Override
    public void onLogin() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onLogout() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onCancelLogin() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void initView() {
        // TODO Auto-generated method stub
        
    }
    
    private final String downloadListener_key = TAG + product_type;
    
    
    private DownloadOberserver downLoadObserver;
    class DownloadOberserver extends ContentObserver {
        
        public DownloadOberserver() {
            super(mHandler);
        }

        @Override
        public void onChange(boolean selfChange) {
            mHandler.sendEmptyMessage(MSG_DOWNLOADING);
        }
        
    }
    
    private PlugsInObserver plugInObserver;
    class PlugsInObserver extends ContentObserver {
        public PlugsInObserver() {
            super(mHandler);
        }

        // 当监听到数据发生了变化就调用这个方法，并将新添加的数据查询出来
        public void onChange(boolean selfChange) {
            mHandler.sendEmptyMessage(MSG_ON_CHANGE);
        }
    }
    
    protected void refreshStatusUI() {
//        for (int j = 0; j < mListView.getChildCount(); j++) {
//            View v = mListView.getChildAt(j);
//            if(v.getTag() != null && ProductItemView.class.isInstance(v.getTag())) {
//                ProductItemView item = (ProductItemView)v.getTag();
//                item.refreshStatusUI();
//            }
//        }
    }
    protected void refreshProgressUI() {
//        for (int j = 0; j < mListView.getChildCount(); j++) {
//            View v = mListView.getChildAt(j);
//            if(v.getTag() != null && ProductItemView.class.isInstance(v.getTag())) {
//                ProductItemView item = (ProductItemView)v.getTag();
//                item.refreshProgressUI();
//            }
//        }
    }

    @Override
    public void downloadSuccess(String productID, String fileUri) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void downloadFailed(String productID) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onLoging() {
        // TODO Auto-generated method stub
        
    }
}
