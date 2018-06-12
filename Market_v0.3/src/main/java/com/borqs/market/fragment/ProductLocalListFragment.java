package com.borqs.market.fragment;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.borqs.market.R;
import com.borqs.market.adapter.ProductListAdapter;
import com.borqs.market.db.DownLoadHelper;
import com.borqs.market.db.DownLoadProvider;
import com.borqs.market.json.Product;
import com.borqs.market.json.Product.ProductType;
import com.borqs.market.json.Product.SupportedMod;
import com.borqs.market.listener.DownloadListener;
import com.borqs.market.utils.BLog;
import com.borqs.market.utils.DownloadUtils;
import com.borqs.market.utils.QiupuHelper;

public class ProductLocalListFragment extends BasicFragment implements
        OnScrollListener, DownloadListener {

    private static final String TAG = ProductLocalListFragment.class.getSimpleName();
    private static final String TAG_DATAS = "TAG_DATAS";
    private static final String TAG_PRODUCT_TYPE = "TAG_PRODUCT_TYPE";
    protected ListView  mListView;
    protected View load_more_item;
    protected View layout_more;
    private ProductListAdapter mAdapter;
    private ArrayList<Product> mDatas = new ArrayList<Product>();
    private AsyncTask<Void, Void, List<Product>> mScanLocalProductTask;
    private int mPage = 0;
    private final int count = 100;
    private boolean hasMore = true;

    protected String product_type = ProductType.THEME;
    protected String supported_mod;
    boolean isportrait = false;

    public static Bundle getArguments(String product_type, String supported_mod) {
        Bundle args = new Bundle();
        args.putString(ARGUMENTS_KEY_TYPE, product_type);
        args.putString(ARGUMENTS_KEY_MOD, supported_mod);
        return args;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        product_type = args.getString(ARGUMENTS_KEY_TYPE);
        supported_mod = args.getString(ARGUMENTS_KEY_MOD);
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
        mAdapter = new ProductListAdapter(mActivity, mDatas, false, isportrait);
        mListView.setAdapter(mAdapter);
        mListView.setOnScrollListener(this);
        
        return mConvertView;
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	if(mAdapter != null) {
    		mAdapter.alertData(mDatas);
    	}
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
        } else if (hasMore) {
            BLog.d(TAG, "getProductList....");
            getProductList();
        }

//        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view,
//                    int position, long id) {
//                if (mDatas != null && mDatas.get(position) != null) {
//                    IntentUtil.startProductDetailActivity(mActivity,
//                            mDatas.get(position).product_id,
//                            mDatas.get(position).version_code,
//                            mDatas.get(position).name);
//                }
//            }
//
//        });
    }

    public void onRefresh() {
        hasMore = true;
        mPage = 0;
        getProductList();
    }

    public void loadMore() {
        mPage++;
        getProductList();
    }

    private void notifyDataChange() {
        if (mAdapter == null) {
            mAdapter = new ProductListAdapter(mActivity, mDatas, false, isportrait);
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

    private boolean isLoading;
    private final Object mLocked = new Object();

    private void getProductList() {
        if (isLoading) {
            BLog.v(TAG, "is Loading ...");
            return;
        }
        
        
        if (mPage == 0)
            begin();
        
        synchronized (mLocked) {
            isLoading = true;
        }

        BLog.v(TAG, "begin getLocalProductList");
//        mApiUtil = ApiUtil.getInstance();
        // Try to scan the local product info, but not in the UI thread.
        // It's also necessary to cancel the thread onDetach(),
        // hence the use of AsyncTask instead of a raw thread.
        mScanLocalProductTask = new AsyncTask<Void, Void, List<Product>>() {
            @Override
            protected List<Product> doInBackground(Void... params) {
//                return DataConvertUtils.parseXML(localPath);
                return DownLoadHelper.queryLocalProductList(mActivity.getApplicationContext(),supported_mod, product_type);
            }

            @Override
            protected void onPostExecute(List<Product> productList) {
                synchronized (mLocked) {
                    isLoading = false;
                }

                Message mds = mHandler.obtainMessage(LOAD_END);
                if (productList == null) {
                    mds.getData().putBoolean(RESULT, true);
                } else {
                    mds.getData().putBoolean(RESULT, true);

                    if (mPage == 0 && mDatas != null) {
                        mDatas.clear();
                    }

                    if (productList != null && productList.size() > 0) {
                        mDatas.addAll(productList);
                    } else {
                        hasMore = false;

                    }
                    //现在为一次性取所有数据
                    hasMore = false;

                }
                if (isCancelled()) {
                    Log.w(TAG, "onPostExecute, task was cancelled...");
                }
                mHandler.sendMessage(mds);
            }
        };
        mScanLocalProductTask.execute(null, null, null);
        
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
        if (mScanLocalProductTask != null) {
            mScanLocalProductTask.cancel(true);
        }
        System.gc();
    }

    private final static int LOAD_END = 0;
    private final static int MSG_ON_CHANGE = 2;
    private final static int MSG_DOWNLOADING = 3;
    private final static String RESULT = "RESULT";

    private class MainHandler extends Handler {
        public MainHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

            case LOAD_END: {
                if(mPage == 0) {
                    if(!isLoading) {
                        mPage = (mDatas.size()-1) / count;
                        end();
                        boolean suc = msg.getData().getBoolean(RESULT);
                        if (suc) {
                            notifyDataChange();
                            if(mDatas.size() == 0) {
                                showLoadMessage(R.string.empty_list, false);
                            }
                        } else {
                            showLoadMessage(R.string.msg_loadi_failed);
                        }
                    }
                }else {
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

    // int mLastItemtPosition = 0;
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
//        if (mDatas == null || mDatas.size() == 0) {
//            layout_more.setVisibility(View.GONE);
//            return;
//        }
//        // mLastItemtPosition = firstVisibleItem + visibleItemCount -1;
//        if (firstVisibleItem + visibleItemCount == totalItemCount
//                && mDatas.size() % count == 0 && hasMore) {
//            if (isLoading || isLoadingRecommend) {
//                BLog.v(TAG, "is Loading ...");
//                return;
//            }
//            layout_more.setVisibility(View.VISIBLE);
//            loadMore();
//        } else {
//            layout_more.setVisibility(View.GONE);
//        }
//        
        //ListView 的FooterView也会算到visibleItemCount中去，所以要再减去一  
        visibleLastIndex = firstVisibleItem + visibleItemCount - 1 - 1;  
        
    }
//    @Override
//    public void onScroll(AbsListView view, int firstVisibleItem,
//            int visibleItemCount, int totalItemCount) {
//        if (mDatas == null || mDatas.size() == 0) {
//            layout_more.setVisibility(View.GONE);
//            return;
//        }
//        // mLastItemtPosition = firstVisibleItem + visibleItemCount -1;
//        if (firstVisibleItem + visibleItemCount == totalItemCount
//                && mDatas.size() % count == 0 && hasMore) {
//            if (isLoading || isLoadingRecommend) {
//                BLog.v(TAG, "is Loading ...");
//                return;
//            }
//            layout_more.setVisibility(View.VISIBLE);
//            loadMore();
//        } else {
//            layout_more.setVisibility(View.GONE);
//        }
//
//    }

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
    
    private void refreshStatusUI() {
//        for (int j = 0; j < mListView.getChildCount(); j++) {
//            View v = mListView.getChildAt(j);
//            if(v.getTag() != null && ProductItemView.class.isInstance(v.getTag())) {
//                ProductItemView item = (ProductItemView)v.getTag();
//                item.refreshStatusUI();
//            }
//        }
        getProductList();
    }
    private void refreshProgressUI() {
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
