package com.borqs.market.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.borqs.market.BasicActivity;
import com.borqs.market.R;
import com.borqs.market.account.AccountSession;
import com.borqs.market.db.DownLoadHelper;
import com.borqs.market.json.Product;
import com.borqs.market.net.RequestListener;
import com.borqs.market.net.WutongException;
import com.borqs.market.utils.BLog;
import com.borqs.market.utils.DataConnectionUtils;
import com.borqs.market.utils.DialogUtils;
import com.borqs.market.utils.MarketUtils;
import com.borqs.market.utils.QiupuHelper;
import com.borqs.market.wallpaper.RawPaperItem;
import com.borqs.market.wallpaper.Wallpaper;
import com.borqs.market.wallpaper.WallpaperUtils;
import com.support.StaticUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WallpaperExportFragment extends BasicFragment implements View.OnClickListener {
    private static final String TAG = "WallpaperExportFragment";
    protected static int     TOTAL_COUNT = 3;

    protected View pager_layout;
    private PhotoAdapter mAdapter;

    protected ViewPager mPager;
    protected LinearLayout mPage;
    protected LinearLayout mOpertionGroupView;
    protected Button mDownloadOrDeleteButton;
    private ProgressBar downloadProgress;
    private TextView downloadSize;
    private TextView downloadPrecent;
//    private View downloadCancel;
    private View processView;
    private View content_container;

    private static final int PRODUCT_STATUS_PUBLISH = 0;
    private static final int PRODUCT_STATUS_PUBLISHING = 1;
    private static final int PRODUCT_STATUS_PUBLISH_OK = 2;
    private static final int PRODUCT_STATUS_PUBLISH_FAIL = 3;
    private static final int PRODUCT_STATUS_LOGOUT = 4;
    private static final int PRODUCT_STATUS_LOGINING = 5;
    private static final int PRODUCT_STATUS_SCAN_FAIL = 6;
    private static final int PRODUCT_STATUS_QUOTA_FULL = 7;
    private static final int PRODUCT_STATUS_EMPTY_DATA = 8;
    private int PRODUCT_STATUS = PRODUCT_STATUS_LOGOUT;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        performDecodingTask();
    }

    private void performDecodingTask() {
        if (null != mScanLocalProductTask && mScanLocalProductTask.getStatus() == AsyncTask.Status.RUNNING) {
            mScanLocalProductTask.cancel(true);
        }
        mScanLocalProductTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                if (!QiupuHelper.isEnoughSpace()) {
                    setTitle(R.string.storage_full);
                    PRODUCT_STATUS = PRODUCT_STATUS_QUOTA_FULL;
                    cancel(true);
                }
            }
            @Override
            protected Boolean doInBackground(Void... params) {
                if (isCancelled()) {
                    return false;
                }

                return WallpaperUtils.decodePaper(mContext, mWallDatas,
                        Product.SupportedMod.PORTRAIT.equalsIgnoreCase(supported_mod));
            }

            @Override
            protected void onPostExecute(Boolean succeed) {
                Message mds = mHandler.obtainMessage(LOAD_END);
                mds.getData().putBoolean(RESULT, succeed);

                if (succeed) {
                    createAdapter();
                } else {
                    // error while decode images, change status and decoding again
//                    performDecodingTask();
                    setTitle(R.string.scanning_fail);
                    PRODUCT_STATUS = PRODUCT_STATUS_SCAN_FAIL;
                }
                if (isCancelled()) {
                    Log.w(TAG, "onPostExecute, task was cancelled...");
                }
                mHandler.sendMessage(mds);
            }

            @Override
            protected void onCancelled() {
                Message mds = mHandler.obtainMessage(LOAD_END);
                mds.getData().putBoolean(RESULT, true);

                if (isCancelled()) {
                    Log.w(TAG, "onPostExecute, task was cancelled...");
                }
                mHandler.sendMessage(mds);
            }
        };

        mScanLocalProductTask.execute(null, null, null);
    }

    @Override
    protected void initView() {
        pager_layout = mConvertView.findViewById(R.id.pager_layout);
        mPager = (ViewPager) mConvertView.findViewById(R.id.mPager);
        mPager.setOffscreenPageLimit(TOTAL_COUNT);
        mPager.setPageMargin(20);
        mOpertionGroupView = (LinearLayout) mConvertView.findViewById(R.id.opertion_group_view);
        mPage = (LinearLayout) mConvertView.findViewById(R.id.pages);
        mDownloadOrDeleteButton = (Button) mConvertView.findViewById(R.id.delete_or_download);
        mDownloadOrDeleteButton.setOnClickListener(this);

//        downloadCancel = mConvertView.findViewById(R.id.download_cancel);
        downloadProgress = (ProgressBar)mConvertView.findViewById(R.id.download_progress);
        downloadSize = (TextView)mConvertView.findViewById(R.id.download_size);
        downloadPrecent = (TextView)mConvertView.findViewById(R.id.download_precent);
        processView = mConvertView.findViewById(R.id.process_view);
        content_container = mConvertView.findViewById(R.id.content_container);

        PhotoOnPageChangeListener pageChangeListener = new PhotoOnPageChangeListener();
        mPager.setOnPageChangeListener(pageChangeListener);

        pager_layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // dispatch the events to the ViewPager, to solve the problem that we can swipe only the middle view.
                return mPager.dispatchTouchEvent(event);
            }
        });
//        downloadCancel.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                if((PRODUCT_STATUS == PRODUCT_STATUS_PUBLISH)) {
//                    // todo : publish wallpaper
//                }
//
//            }
//        });
    }

    @Override
    protected void begin() {
        super.begin();
        content_container.setVisibility(View.GONE);
    }

    @Override
    protected void end() {
        super.end();
        content_container.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        parseIntent(activity.getIntent());
    }

    protected void parseIntent(Intent intent) {
        appVersionCode = intent.getIntExtra(MarketUtils.EXTRA_APP_VERSION, 0);
        packageName = intent.getStringExtra(MarketUtils.EXTRA_PACKAGE_NAME);
//        localPath = intent.getStringExtra(MarketUtils.EXTRA_LOCAL_PATH);
        supported_mod = intent.getStringExtra(MarketUtils.EXTRA_MOD);
        if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("package name is null");
        }

        mWallDatas = (ArrayList<RawPaperItem>)intent.getSerializableExtra(WallpaperUtils.EXTRA_RAW_WALL_PAPERS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if(loading_layout != null) loading_layout.setBackgroundResource(R.color.transparent);

        return mConvertView;
    }

    @Override
    public void onResume() {
        refreshUI();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    protected void refreshProductStatus(int status) {
        PRODUCT_STATUS = status;
        processView.setVisibility(View.GONE);

        if (PRODUCT_STATUS == PRODUCT_STATUS_LOGOUT) {
            mDownloadOrDeleteButton.setText(R.string.action_login);
            mDownloadOrDeleteButton.setClickable(true);
        } else if (PRODUCT_STATUS == PRODUCT_STATUS_LOGINING) {
            mDownloadOrDeleteButton.setText(R.string.lable_being_login);
            mDownloadOrDeleteButton.setClickable(true);
        } else if (PRODUCT_STATUS == PRODUCT_STATUS_PUBLISH) {
            mDownloadOrDeleteButton.setText(R.string.publish);
            mDownloadOrDeleteButton.setClickable(true);
        } else if (PRODUCT_STATUS == PRODUCT_STATUS_PUBLISHING) {
            mDownloadOrDeleteButton.setText(R.string.publishing);
//            mDownloadOrDeleteButton.setEnabled(false);
//            processView.setVisibility(View.VISIBLE);
            mDownloadOrDeleteButton.setClickable(false);
        } else if (PRODUCT_STATUS == PRODUCT_STATUS_PUBLISH_OK) {
            mDownloadOrDeleteButton.setText(R.string.published);
            mDownloadOrDeleteButton.setClickable(false);
        } else if (PRODUCT_STATUS == PRODUCT_STATUS_PUBLISH_FAIL
                || PRODUCT_STATUS == PRODUCT_STATUS_SCAN_FAIL) {
            mDownloadOrDeleteButton.setText(R.string.publish_again);
            mDownloadOrDeleteButton.setClickable(true);
        } else if (PRODUCT_STATUS == PRODUCT_STATUS_QUOTA_FULL) {
            mDownloadOrDeleteButton.setText(R.string.storage_full);
            mDownloadOrDeleteButton.setClickable(true);
        }
        mDownloadOrDeleteButton.setVisibility(View.VISIBLE);
    }

    private void createAdapter() {
        mAdapter = new PhotoAdapter(getFragmentManager(), mWallDatas, supported_mod);
        mPager.setAdapter(mAdapter);
    }
    protected void refreshUI() {
        if(isDetached()) {
            BLog.d(TAG, "fragment is detached!");
            return;
        }

        if (mAdapter == null ||
                (null == mScanLocalProductTask &&
                        mScanLocalProductTask.getStatus() == AsyncTask.Status.RUNNING)) {
            begin();
            setTitle(R.string.loading);

            return;
        } else {
            end();
            setTitle(R.string.action_export_wallpaper);
        }

        if (mAdapter == null) {
            createAdapter();
        } else {
            mAdapter.notifyDataSetChanged();
        }
        BLog.d(TAG,"page count = "+mAdapter.getCount());
        if (AccountSession.isLogin()) {
            refreshProductStatus(PRODUCT_STATUS == PRODUCT_STATUS_LOGOUT ?
                    PRODUCT_STATUS_PUBLISH : PRODUCT_STATUS);
        } else {
            refreshProductStatus(PRODUCT_STATUS_LOGOUT);
        }

        mPage.removeAllViews();
        for (int i = 0; i < mAdapter.getCount(); i++) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            ImageView dot = new ImageView(getActivity());
            if (i == 0) {
                dot.setBackgroundResource(R.drawable.indicator_focus);
            } else {
                dot.setBackgroundResource(R.drawable.indicator_normal);
            }
            mPage.addView(dot, params);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    protected void createHandler() {
        mHandler = new MainHandler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private final static int LOAD_END = 0;
    private final static int PURCHASE_END = 1;
    private final static int PUBLISH_BEGIN = 2;
    private final static int PUBLISH_END = 3;
    private final static String RESULT = "RESULT";
    private final static String URL = "URL";

    private class MainHandler extends Handler {
        public MainHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOAD_END: {
                    end();
                    if(getActivity() == null) return;
                    boolean suc = msg.getData().getBoolean(RESULT);
                    if (suc) {
                        refreshUI();
                    } else {
                        showLoadMessage(R.string.msg_loadi_failed);
                    }
                    break;
                }
                case PUBLISH_BEGIN: {
//                    showIndeterminate(R.string.publishing);
                    if(getActivity() == null) return;
                    StaticUtil.onEvent(mContext, "WALLPAPER_SHOW_LAUNCH", "" + System.currentTimeMillis());
                    refreshProductStatus(PRODUCT_STATUS_PUBLISHING);
                    break;
                }
                case PUBLISH_END: {
//                    dismissProgress();
                    if(getActivity() == null) return;
                    boolean suc = msg.getData().getBoolean(RESULT);

                    if (suc) {
                        StaticUtil.onEvent(mContext, "WALLPAPER_SHOW_SUCCESS", "" + System.currentTimeMillis());
                        refreshProductStatus(PRODUCT_STATUS_PUBLISH_OK);
                        Activity activity = getActivity();
                        if (null != activity && activity instanceof BasicActivity) {
                            ((BasicActivity)activity).goBackToScene();
                            activity.finish();
                            MarketUtils.startUserShareListIntent(activity, Product.ProductType.WALL_PAPER, supported_mod);
                        }
                    } else {
                        StaticUtil.onEvent(mContext, "WALLPAPER_SHOW_FAIL", "" + System.currentTimeMillis());
                        refreshProductStatus(PRODUCT_STATUS_PUBLISH_FAIL);
                        showLoadMessage(R.string.msg_show_paper_failed);
                    }
                    break;
                }
                case PURCHASE_END: {
                    if(getActivity() == null) return;
                    boolean suc = msg.getData().getBoolean(RESULT);
                    if (suc) {

                    } else {
//                    Toast.makeText(mActivity, R.string.purchase_failed,
//                            Toast.LENGTH_SHORT).show();

                    }
                    break;
                }
            }
        }
    }

    @Override
    protected int getInflatelayout() {
        return R.layout.paper_export_fragment;
    }

    @Override
    public void onRefresh() {
    }

    public class PhotoOnPageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageSelected(int position) {
            for (int i = 0; i < mPage.getChildCount(); i++) {
                ImageView dot = (ImageView) mPage.getChildAt(i);
                if (i == position) {
                    dot.setBackgroundResource(R.drawable.indicator_focus);
                } else {
                    dot.setBackgroundResource(R.drawable.indicator_normal);
                }
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // to refresh frameLayout
            if (pager_layout != null) {
                pager_layout.invalidate();
            }
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    }

    private void postRefreshUi(final int status) {
        Activity activity = getActivity();
        if (null != activity && !activity.isFinishing()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshUI();
                    refreshProductStatus(status);
                }
            });
        }
    }
    @Override
    public void onLogin() {
        postRefreshUi(PRODUCT_STATUS_PUBLISH);
    }

    @Override
    public void onLogout() {
        postRefreshUi(PRODUCT_STATUS_LOGOUT);
    }

    @Override
    public void onCancelLogin() {
        postRefreshUi(PRODUCT_STATUS_LOGOUT);
    }

    private static class PhotoAdapter  extends FragmentPagerAdapter {
        private ArrayList<RawPaperItem> mWallDatas = null;
        private boolean isLandscape;
        private ExportDescriptionFragment mDescription;
        public PhotoAdapter(FragmentManager fm, ArrayList<RawPaperItem> wallList, String supported_mod) {
            super(fm);
            mWallDatas = wallList;
            isLandscape = Product.SupportedMod.LANDSCAPE.equalsIgnoreCase(supported_mod);
        }

        @Override
        public int getCount() {
            int count = 1;
            count += null == mWallDatas ? 0 : mWallDatas.size();
            return count;
        }

        @Override
        public void notifyDataSetChanged() {
            if (null != mDescription) {
                mDescription.updateAuthorInfo();
            }

            super.notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                mDescription = new ExportDescriptionFragment();
                return mDescription;
            }else {
                final int wallSize = null == mWallDatas ? 0 : mWallDatas.size();
                String filePath = null;
                if (position < wallSize + 1) {
                    filePath = WallpaperUtils.getDecodedPaperPath(mWallDatas.get(position - 1));
                }

                if (TextUtils.isEmpty(filePath)) {
                    Log.e(TAG, "getItem, should not be here");
                }

                final String url = Uri.fromFile(new File(filePath)).toString();
                PhotoFragment f = new PhotoFragment();
                f.setArguments(PhotoFragment.getArguments(url,isLandscape));
                
                return f;
            }
        }
    }

    protected String product_type = Product.ProductType.THEME;
    protected String packageName;
    private String supported_mod;
    protected int appVersionCode = 0;

    private ArrayList<RawPaperItem> mWallDatas = new ArrayList<RawPaperItem>();

    public WallpaperExportFragment() {
        super();
        this.product_type = Product.ProductType.WALL_PAPER;
//        this.appVersionCode = appVersionCode;
//        this.packageName = packageName;
//        this.supported_mod = supported_mod;
//        this.mDatas.clear();
//        this.mDatas.addAll(rawPaperItems);
    }


//    @Override
//    protected void begin() {
//    }
//    
//    @Override
//    protected void end() {
//    }
    
    @Override
    protected void showLoadMessage(int resId) {
        Toast.makeText(mActivity, resId, Toast.LENGTH_SHORT).show();
    }

    private void sendMessage(int code, boolean result) {
        Message mds = mHandler.obtainMessage(code);
        mds.getData().putBoolean(RESULT, result);
        mHandler.sendMessage(mds);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.delete_or_download) {
            if (PRODUCT_STATUS == PRODUCT_STATUS_LOGOUT) {
                ((BasicActivity)getActivity()).login();
                refreshProductStatus(PRODUCT_STATUS_LOGINING);
            } else if (PRODUCT_STATUS == PRODUCT_STATUS_PUBLISH ||
                    PRODUCT_STATUS == PRODUCT_STATUS_PUBLISH_FAIL) {
                doPublish();
            } else if (PRODUCT_STATUS == PRODUCT_STATUS_SCAN_FAIL) {
                performDecodingTask();
            } else if (PRODUCT_STATUS == PRODUCT_STATUS_QUOTA_FULL) {
                // todo: show clean disk dialog
            }
        }
    }

    private void doPublish() {
        if (!checkAndPromptNetworking()) {
            return;
        }

        final String title = getSummary();
        if (TextUtils.isEmpty(title)) {
            TextView textView = (TextView)getActivity().findViewById(R.id.tv_title);
            if (null != textView) {
                textView.requestFocus();
                return;
            }
        }

        final String authorName = getAuthorName();
        if (TextUtils.isEmpty(authorName)) {
            TextView textView = (TextView)getActivity().findViewById(R.id.tv_author);
            if (null != textView) {
                textView.requestFocus();
                return;
            }
        }

        final String description = getDescription();
        if (TextUtils.isEmpty(description)) {
            TextView textView = (TextView)getActivity().findViewById(R.id.tv_desc);
            if (null != textView) {
                textView.requestFocus();
                return;
            }
        }

        Wallpaper.Builder builder = new Wallpaper.Builder(AccountSession.account_id);
        builder.setPackageName(packageName).setAppVersion(appVersionCode)
                .setEmail(AccountSession.account_email)
                .setSummary(title)
                .setTitle(title)
                .setUserName(authorName)
                .setDescription(description)
                .setPapers(mWallDatas);

        sendMessage(PUBLISH_BEGIN, true);
        WallpaperUtils.exportWallpaperSuite(builder, supported_mod, new RequestListener() {
            @Override
            public void onComplete(String response) {
                Log.i(TAG, "performUpload, wallpaper upload complete, " + response);
                sendMessage(PUBLISH_END, true);
            }

            @Override
            public void onIOException(IOException e) {
                e.printStackTrace();
                sendMessage(PUBLISH_END, false);
                Log.e(TAG, "performUpload, wallpaper upload exception, " + e.getMessage());
            }

            @Override
            public void onError(WutongException e) {
                e.printStackTrace();
                sendMessage(PUBLISH_END, false);
                Log.e(TAG, "performUpload, wallpaper upload error, " + e.getMessage());
            }
        });
    }

    private String getSummary() {
        TextView textView = (TextView)getActivity().findViewById(R.id.tv_title);
        if (null != textView) {
            String summary = textView.getText().toString();
            return null == summary ? "" : summary;
        }
        return "";
    }

    private String getAuthorName() {
        TextView textView = (TextView)getActivity().findViewById(R.id.tv_author);
        if (null != textView) {
            String summary = textView.getText().toString();
            return null == summary ? "" : summary;
        }
        return "";
    }
    private String getDescription() {
        TextView textView = (TextView)getActivity().findViewById(R.id.tv_desc);
        if (null != textView) {
            String summary = textView.getText().toString();
            return null == summary ? "" : summary;
        }
        return "";
    }

    @Override
    public void onLoging() {
        // TODO Auto-generated method stub
    }

    private AsyncTask<Void, Void, Boolean> mScanLocalProductTask;
    @Override
    public void onDetach() {
        super.onDetach();
        if (mScanLocalProductTask != null && mScanLocalProductTask.getStatus() == AsyncTask.Status.RUNNING) {
            mScanLocalProductTask.cancel(true);
        }
    }

    private boolean checkAndPromptNetworking() {
        Activity activity = getActivity();
        if (DataConnectionUtils.testValidConnection(activity)) {
            return true;
        }

        if (!activity.isFinishing()) {
            Toast.makeText(activity, R.string.no_networking, Toast.LENGTH_SHORT).show();
            PRODUCT_STATUS = PRODUCT_STATUS_PUBLISH_FAIL;
            refreshUI();
        }
        return false;
    }
}

class ExportDescriptionFragment extends Fragment {
    public interface ClickListener {
        void onclick();
    }

    public ExportDescriptionFragment() {
        super();
    }

    private TextView mAuthorView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wallpaper_export_description, null);
            ((TextView)view.findViewById(R.id.tv_title)).setText(getSharedTitle());
//            ((TextView)view.findViewById(R.id.tv_size)).setText(DownloadUtils.getAppSize(mData.size));
//            ((TextView)view.findViewById(R.id.tv_size)).setText("");
//            ((TextView)view.findViewById(R.id.tv_version)).setText("0");
        mAuthorView = ((TextView)view.findViewById(R.id.tv_author));
            ((TextView)view.findViewById(R.id.tv_desc)).setText(getSharedDescription());
        updateAuthorInfo();
//            SimpleDateFormat fmt=new SimpleDateFormat("yyyy-MM-dd");
//            ((TextView)view.findViewById(R.id.tv_update_time)).setText(fmt.format(new Date(System.currentTimeMillis())));
        return view;
    }

    protected void updateAuthorInfo() {
        if (null == mAuthorView) return;
        final String author;
        if (!TextUtils.isEmpty(AccountSession.account_email)) {
            author = AccountSession.account_email;
        } else if (!TextUtils.isEmpty(AccountSession.account_id)) {
            author = AccountSession.account_id;
        } else {
            author = "";
        }
        mAuthorView.setText(author);
    }

    private String getSharedDescription() {
        return getResources().getString(R.string.user_share_desc, getString(R.string.app_name));
    }

    private String getSharedTitle() {
        return getResources().getString(R.string.user_share_title, System.currentTimeMillis());
    }
}