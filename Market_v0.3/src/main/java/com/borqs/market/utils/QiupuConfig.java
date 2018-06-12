package com.borqs.market.utils;

import static android.os.Environment.getExternalStorageDirectory;

public class QiupuConfig {
    public static final boolean SEVER_DEBUG = false;

    private static final String SDCARD_ROOT = getExternalStorageDirectory()
            .getPath();
    
    /** used for imagerun set default image */
    public final static int DEFAULT_IMAGE_INDEX_PHOTO = 0;

    public static String getSdcardPath() {
        return QiupuConfig.SDCARD_ROOT + "/";
    }
}
