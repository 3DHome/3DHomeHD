package com.borqs.market.fragment;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.borqs.market.R;
import com.borqs.market.adapter.ProductGridAdapter;
import com.borqs.market.api.ApiUtil;
import com.borqs.market.json.Product;
import com.borqs.market.json.Product.ProductType;
import com.borqs.market.json.ProductJSONImpl;
import com.borqs.market.net.RequestListener;
import com.borqs.market.net.WutongException;
import com.borqs.market.utils.BLog;
import com.borqs.market.utils.DataConvertUtils;
import com.borqs.market.utils.IntentUtil;
import com.support.StaticUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WallpaperLocalGridFragment extends BasicFragment implements
        OnScrollListener {

    private static final String TAG = "WallpaperLocalGridFragment";
    private static final String TAG_DATAS = "TAG_DATAS";
    private static final String TAG_PRODUCT_TYPE = "TAG_PRODUCT_TYPE";
    private static final String TAG_VERSION_CODE = "TAG_VERSION_CODE";
    private static final String TAG_PACKAGE_NAME = "TAG_PACKAGE_NAME";
    protected GridView mGridView;
    protected View layout_more;
    private ProductGridAdapter mAdapter;
    private ArrayList<Product> mDatas = new ArrayList<Product>();
//    private ApiUtil mApiUtil;
    private AsyncTask<Void, Void, List<Product>> mScanLocalProductTask;
    private int mPage = 0;
    private final int count = 10;
    private boolean hasMore = true;

    protected String product_type = ProductType.THEME;
    protected String packageName;
    private String localPath;
    protected int appVersionCode = 0;
    
    public static Bundle getArguments(String product_type, int appVersionCode, String packageName, String localPath) {
        Bundle args = new Bundle();
        args.putString(ARGUMENTS_KEY_TYPE, product_type);
        args.putInt(ARGUMENTS_KEY_VERSION, appVersionCode);
        args.putString(ARGUMENTS_KEY_PKG, packageName);
        args.putString(ARGUMENTS_KEY_PATH, localPath);
        return args;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        product_type = args.getString(ARGUMENTS_KEY_TYPE);
        appVersionCode = args.getInt(ARGUMENTS_KEY_VERSION);
        packageName = args.getString(ARGUMENTS_KEY_PKG);
        localPath = args.getString(ARGUMENTS_KEY_PATH);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        BLog.d(TAG, "onCreateView()");
        super.onCreateView(inflater, container, savedInstanceState);
        // mConvertView = inflater.inflate(R.layout.product_grid_layout,
        // container, false);
        // mLoadingLayout = mConvertView.findViewById(R.id.loading_layout);
        mGridView = (GridView) mConvertView.findViewById(R.id.grid_favorite);
        layout_more = mConvertView.findViewById(R.id.layout_more);
        mAdapter = new ProductGridAdapter(mActivity, mDatas);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnScrollListener(this);
        return mConvertView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        BLog.d(TAG, "onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mDatas = savedInstanceState.getParcelableArrayList(TAG_DATAS);
            this.appVersionCode = savedInstanceState
                    .getInt(TAG_VERSION_CODE, 0);
            this.product_type = savedInstanceState.getString(TAG_PRODUCT_TYPE);
            this.packageName = savedInstanceState.getString(TAG_PACKAGE_NAME);

            BLog.d(TAG, "savedInstanceState != null versioncode="
                    + appVersionCode);
            BLog.d(TAG, "savedInstanceState != null product_type="
                    + product_type);
            BLog.d(TAG, "savedInstanceState != null packageName=" + packageName);
        }
        if (mDatas != null && !mDatas.isEmpty()) {
            mPage = (mDatas.size()-1) / count;
            BLog.d(TAG, "notifyDataChange");
            notifyDataChange();
        } else if (hasMore) {
            BLog.d(TAG, "getLocalProductList....");
            getLocalProductList();
        }

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                if (mDatas != null && mDatas.get(position) != null) {
                    onStartProductItem(mDatas.get(position));
                }
            }

        });
    }

    private void onStartProductItem(Product product) {
        if (null == product) {
            Log.e(TAG, "onStartProductItem, error with null product.");
            return;
        }
        IntentUtil.startWallpaperProductDetailActivity(mActivity,
                product.product_id,
                product.version_code,
                product.name, product.supported_mod);
        StaticUtil.onEvent(mActivity, "WALLPAPER_VIEWED", "wallpaper viewed");
    }

    @Override
    public void onRefresh() {
        hasMore = true;
        mPage = 0;
        getLocalProductList();
    }

    public void loadMore() {
        mPage++;
        getLocalProductList();
    }

    private void notifyDataChange() {
        if (mAdapter == null) {
            mAdapter = new ProductGridAdapter(mActivity, mDatas);
            mGridView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged(mDatas);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(TAG_DATAS, mDatas);
        outState.putString(TAG_PRODUCT_TYPE, product_type);
        outState.putString(TAG_PACKAGE_NAME, packageName);
        outState.putInt(TAG_VERSION_CODE, appVersionCode);

        BLog.d(TAG, "onSaveInstanceState()   versioncode=" + appVersionCode);
        BLog.d(TAG, "onSaveInstanceState()   product_type=" + product_type);
        BLog.d(TAG, "onSaveInstanceState()   packageName=" + packageName);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        BLog.d(TAG, "onDestroyView()");
        super.onDestroyView();
    }

    private boolean isLoading;
    private final Object mLocked = new Object();

    private void getLocalProductList() {
        if (isLoading) {
            BLog.v(TAG, "is Loading ...");
            return;
        }
        
        if (mPage == 0)
            begin();
        
//        if (!DataConnectionUtils.testValidConnection(mActivity)) {
//            if (mPage == 0) {
//                mHandler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        showLoadMessage(R.string.dlg_msg_no_active_connectivity,true);
//                    }
//                }, 500);
//            }else {
//                Toast.makeText(mActivity,
//                        R.string.dlg_msg_no_active_connectivity,
//                    Toast.LENGTH_SHORT).show();
//            }
//            return;
//        }
        
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
                return DataConvertUtils.parseXML(localPath);
            }

            @Override
            protected void onPostExecute(List<Product> productList) {
                synchronized (mLocked) {
                    isLoading = false;
                }

                Message mds = mHandler.obtainMessage(LOAD_END);
                if (productList == null) {
                    mds.getData().putBoolean(RESULT, false);
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
//        mApiUtil = null;

        if (mScanLocalProductTask != null) {
            mScanLocalProductTask.cancel(true);
        }
        System.gc();
    }

    private final static int LOAD_END = 0;
    private final static String RESULT = "RESULT";

    private class MainHandler extends Handler {
        public MainHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

            case LOAD_END: {
                mPage = (mDatas.size()-1) / count;
                end();
                boolean suc = msg.getData().getBoolean(RESULT);
                if (suc) {
                    notifyDataChange();
                } else {
                    showLoadMessage(R.string.msg_loadi_failed);
                }
                break;
            }
            }
        }
    }

    @Override
    protected int getInflatelayout() {
        return R.layout.wallpaper_product_grid_layout;
    }

    // int mLastItemtPosition = 0;
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // if(scrollState == OnScrollListener.SCROLL_STATE_IDLE
        // && view.on) {
        //
        // }
        
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        if (mDatas == null || mDatas.size() == 0) {
            layout_more.setVisibility(View.GONE);
            return;
        }
        // mLastItemtPosition = firstVisibleItem + visibleItemCount -1;
        if (firstVisibleItem + visibleItemCount == totalItemCount
                && totalItemCount % count == 0 && hasMore) {
            layout_more.setVisibility(View.VISIBLE);
            loadMore();
        } else {
            layout_more.setVisibility(View.GONE);
        }

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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int column = getResources().getInteger(R.integer.gridview_number);
        mGridView.setNumColumns(column);
        mAdapter.notifyGridColumnChanged(column);
//        mAdapter.notifyDataSetChanged();
        mGridView.invalidateViews();
        mGridView.setAdapter(mAdapter);
    }

    @Override
    public void onLoging() {
        // TODO Auto-generated method stub
        
    }
}
