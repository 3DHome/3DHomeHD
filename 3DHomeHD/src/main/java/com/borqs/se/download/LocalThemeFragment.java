package com.borqs.se.download;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import android.widget.RelativeLayout;

import com.borqs.freehdhome.R;
import com.borqs.se.home3d.ProviderUtils.ThemeColumns;

public class LocalThemeFragment extends GridFragment {

    private LocalCusorAdapter mLocalCusorAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Cursor cursor =  getActivity().getContentResolver().query(ThemeColumns.CONTENT_URI, null, null, null, null);
        if (cursor != null && cursor.getCount() == 0) {
            setEmptyText(getString(R.string.no_local_theme));
        }
        mLocalCusorAdapter = new LocalCusorAdapter(getActivity(), cursor);
        setGridAdapter(mLocalCusorAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        Cursor cursor = getActivity().getContentResolver().query(ThemeColumns.CONTENT_URI, null, null, null, null);
        if (cursor != null && cursor.getCount() == 0) {
            setEmptyText(getString(R.string.no_local_theme));
        }
        if (cursor != null) {
            cursor.close();
        }
        if (mLocalCusorAdapter != null) {
            mLocalCusorAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocalCusorAdapter.changeCursor(null);
    }

    public class LocalCusorAdapter extends CursorAdapter {

        public LocalCusorAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            return li.inflate(R.layout.theme_adapter, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            final String name = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.NAME));
            final String localPath = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.FILE_PATH));
            final int type = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns.TYPE));
            final int applyStatus = cursor.getInt(cursor.getColumnIndexOrThrow(ThemeColumns.IS_APPLY));
            final String themeConfig = cursor.getString(cursor.getColumnIndexOrThrow(ThemeColumns.CONFIG));
            View scheduleGroupView = (RelativeLayout) view.findViewById(R.id.bottom);
            scheduleGroupView.setVisibility(View.GONE);
            Button action = (Button) view.findViewById(R.id.resource_action);
            ImageView preview = (ImageView) view.findViewById(R.id.resource_preview);
            ImageView isUsingView = (ImageView) view.findViewById(R.id.resource_status);
            if (applyStatus == 1) {
                isUsingView.setVisibility(View.VISIBLE);
            } else {
                isUsingView.setVisibility(View.GONE);
            }

            Drawable bd = null;
            String imagePath = localPath + "/1.jpg";
            if (Category.DEFAULT_THEME.ordinal() == type) {
                action.setVisibility(View.GONE);
                bd = new BitmapDrawable(Utils.getAssetDrawable(context, imagePath));
            } else {
                action.setVisibility(View.VISIBLE);
                action.setText(R.string.action_downloaded);
                bd = new BitmapDrawable(Utils.getLocalDrawable(imagePath));
            }
            preview.setBackgroundDrawable(bd);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), LocalThemePreview.class);
                    intent.putExtra(Constant.INTENT_EXTRA_LOCAL_PATH, localPath);
                    intent.putExtra(Constant.INTENT_EXTRA_CATEGORY, type);
                    intent.putExtra(Constant.INTENT_EXTRA_THEME_CONFIG, themeConfig);
                    startActivity(intent);
                }

            });
        }

    }

}
