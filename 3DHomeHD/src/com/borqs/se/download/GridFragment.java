package com.borqs.se.download;

import com.borqs.freehdhome.R;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class GridFragment extends Fragment {
    public static final String KEY_DOWNLOAD_STATUS = "status_type";
    public static final String KEY_FAILURE_TYPE = "fail_type";
    public static final String KEY_FAILURE_MSG = "fail_message";
    public static final String KEY_LOCATION_TYPE = "location_type";
    public static final String KEY_CATEGORY_TYPE = "category_type";
    public static final String KEY_REQUEST_URL = "Request_url";

    private GridView mGrid;
    private TextView mStandardEmptyView;
    private View mProgressContainer;
    private View mGridContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.theme_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mStandardEmptyView = (TextView) view.findViewById(R.id.internalEmpty);
        mStandardEmptyView.setVisibility(View.GONE);

        mProgressContainer = view.findViewById(R.id.progressContainer);
        mGridContainer = view.findViewById(R.id.listContainer);
        mGrid = (GridView) view.findViewById(R.id.list);
        mGrid.setEmptyView(mStandardEmptyView);
    }

    public GridView getGridView() {
        return mGrid;
    }

    public void setGridAdapter(BaseAdapter adapter) {
        mGrid.setAdapter(adapter);
        mProgressContainer.setVisibility(View.GONE);
        mGridContainer.setVisibility(View.VISIBLE);
    }

    public void setEmptyText(CharSequence text) {
        mStandardEmptyView.setText(text);
        mProgressContainer.setVisibility(View.GONE);
        mGridContainer.setVisibility(View.VISIBLE);
    }

    public void cancelProgress() {
        mProgressContainer.setVisibility(View.GONE);
    }

    public enum Location {
        ONLINE, LOCAL
    }

    public enum Category {
        THEME, OBJECT, DEFAULT_THEME
    }

}
