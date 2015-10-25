package com.borqs.market.wallpaper;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Created by b608 on 13-8-20.
 */
public class RawPaperItem implements Parcelable {
    public static final String TYPE_WALL = "TYPE_WALL";
    public static final String TYPE_GROUND = "TYPE_GROUND";

    public RawPaperItem(String hostName, String imageName, String type, float h2w) {
        mHostObjectName = hostName;
        mKeyName = imageName;
        mType = type;
        mRatioH2W = h2w;
    }

    public String mHostObjectName;
    public String mKeyName;
    public String mType; // the type of host object, e.g. wall/ground
    public float mRatioH2W = 1.0f;

    public String mDefaultValue;
    public String mOverlayValue;
    public String mDecodedValue;

    //静态的Parcelable.Creator接口
    public static final Parcelable.Creator<RawPaperItem> CREATOR = new Creator<RawPaperItem>() {

        //创建出类的实例，并从Parcel中获取数据进行实例化
        public RawPaperItem createFromParcel(Parcel source) {
            RawPaperItem item = new RawPaperItem(source.readString(), source.readString(),
                    source.readString(), source.readFloat());
//            item.mKeyName = source.readString();
//            item.mType = source.readString();
//            item.mRatioH2W = source.readFloat();

            item.mDefaultValue = source.readString();
            item.mOverlayValue = source.readString();
            item.mDecodedValue = source.readString();
            return item;
        }

        public RawPaperItem[] newArray(int size) {
            // TODO Auto-generated method stub
            return new RawPaperItem[size];
        }

    };

    //
    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    //将数据写入外部提供的Parcel中
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mHostObjectName);
        dest.writeString(mKeyName);
        dest.writeString(mType);
        dest.writeFloat(mRatioH2W);

        dest.writeString(mDefaultValue);
        dest.writeString(mOverlayValue);
        dest.writeString(mDecodedValue);
    }
}