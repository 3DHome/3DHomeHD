package com.borqs.market.adapter;

import java.net.URI;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.borqs.market.R;
import com.borqs.market.json.Product;
import com.borqs.market.utils.BLog;
import com.borqs.market.utils.ImageRun;

public class ProductGridAdapter extends BaseAdapter {

    private ArrayList<Product> mDatas;
    private LayoutInflater inflater;
    private Context mContext;

    public ProductGridAdapter(Context context, ArrayList<Product> datas) {
        mContext = context;
        mDatas = datas;
        inflater = LayoutInflater.from(context);
        notifyGridColumnChanged(mContext.getResources().getInteger(R.integer.gridview_number));
    }

    public int getCount() {
        return mDatas != null ? mDatas.size() : 0;
    }

    public Product getItem(int position) {
        if (position >= getCount()) {
            return null;
        } else {
            return mDatas.get(position);
        }
    }

    public long getItemId(int position) {
        return position;
    }

    int image_width = 0;
    int image_height = 0;
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if(convertView == null) {
            convertView = inflater.inflate(R.layout.product_item_view, null);
            holder = new ViewHolder();
            holder.imageCover = (ImageView) convertView.findViewById(R.id.img_cover);
            holder.imageCover.setLayoutParams(new RelativeLayout.LayoutParams(image_width, image_height));
            holder.textName = (TextView) convertView.findViewById(R.id.tv_name);
            
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }
        initUI(position, holder);
        
        return convertView;
    }
    
    private void initUI(int position,final ViewHolder holder) {
        if (getItem(position) == null) {
            BLog.e("data is null");
            return;
        }
        holder.imageCover.setImageBitmap(null);
        holder.imageCover.setTag(getItem(position).logo_url);
        final String url = getItem(position).logo_url;
        holder.textName.setText(getItem(position).name);
        holder.imageCover.postDelayed(new Runnable() {
            
            @Override
            public void run() {
                Uri uri = Uri.parse(url);
                if (uri.getScheme().equalsIgnoreCase("file")) {
                    holder.imageCover.setImageURI(uri);
                } else {
                    shootImageRunner(url, holder.imageCover);
                }
            }
        }, 500);

    }

    static class ViewHolder {
        public ImageView imageCover;
        public TextView textName;
    }
    
    private void shootImageRunner(String url, ImageView imageView) {
        if(TextUtils.isEmpty(url) || !url.equals(imageView.getTag())) {
            return;
        }
        ImageRun photo_1 = new ImageRun(null, url, 0);
        photo_1.addHostAndPath = true;
        final Resources resources = imageView.getResources();

        int maxWidthDpi = (int)(100*resources.getDisplayMetrics().density);
        if(image_width > maxWidthDpi) {
            photo_1.width = maxWidthDpi;
        }else {
            photo_1.width = image_width;
        }
        
        BLog.v("widthDpi="+photo_1.width);
        BLog.v("image_width="+image_width);
        BLog.v("image_height="+image_height);
        photo_1.noimage = true;
        photo_1.need_scale = true;
        photo_1.setImageView(imageView);
        photo_1.post(null);
    }

    public void notifyDataSetChanged(ArrayList<Product> datas) {
        mDatas = datas;
        super.notifyDataSetChanged();
    }

    public void notifyGridColumnChanged(int columnNum) {
//        if (1 == columnNum) {
//            image_width = RelativeLayout.LayoutParams.MATCH_PARENT;
//            image_height = RelativeLayout.LayoutParams.WRAP_CONTENT;
//        } else {
            DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
            int width = dm.widthPixels;
            image_width = width / columnNum;
//            image_height = image_width * 512 / 300;
        image_height = image_width;
//        }
    }
}
