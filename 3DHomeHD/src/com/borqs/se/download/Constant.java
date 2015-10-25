package com.borqs.se.download;

public final class Constant {
    /**
     * message
     */
    public static final int MSG_LOAD_DATA = 0x0000001;
    public static final int MSG_DOWNLOAD_SCHEDULE = 0x0000010;
    public static final int MSG_DOWNLOAD_COMPLETE = 0x0000011;
    public static final int MSG_DOWNLOAD_KEYCODE_BACK_UPDATE = 0x0000100;
    public static final int MSG_DOWNLOAD_FAILURL = 0x0000101;
    public static final int MSG_ERROR = 0x000110;
    public static final int MSG_ERROR_TYPE_NETWORK = 0x000111;
    public static final int MSG_ERROR_TYPE_RESOURCES = 0x001000;
    public static final int MSG_LOAD_IMAGES_FINISHED = 0x001001;
    public static final int MSG_DOWNLOAD_START_TASK = 0x001010;
    public static final int MSG_DOWNLOAD_CANCEL_TASK = 0x001011;
    public static final int MSG_DOWNLOAD_COMPLETE_TASK = 0x001100;
    public static final int MSG_UPDATE_PRIVEW_IMAGE = 0x010001;

    public static final int RESULT_CODE_UPDATE = 0;
    public static final int DOWNLOAD_COMPLICATION = 3;
    public static final int DOWNLOAD_BUFFER_SIZE = 4096;
    public static final int DOWNLOAD_TIME_OUT = 5000;
    public static final int DOWNLOAD_PROGRESS_RATE_TIME = 1000;
    public static final String TASK_NAME = "task_name";
    public static final String NODE_MODELS = "models";
    public static final String NODE_MODEL = "model";
    public static final String UPATE_VIEW_TAG = "update_view_tag";
    public static final String DOWNLOAD_STATUS = "download_status";

    public static final String BUNDLE_VIEW_TAG = "view_tag";
    public static final String BUNDLE_PREVIEW_URL = "preview_url";
    public static final String BUNDLE_FILE_SIZE = "file_size";
    public static final String BUNDLE_DOWNLOAD_SIZE = "download_size";
    public static final String INTENT_EXTRA_PREVIEW_IMAGE = "preview_image";
    public static final String INTENT_EXTRA_PREVIEW_IMAGES = "preview_images";
    public static final String INTENT_EXTRA_RESOURCE_URL = "resource_url";
    public static final String INTENT_EXTRA_TYPE = "type";
    public static final String INTENT_EXTRA_LOCATION = "location";
    public static final String INTENT_EXTRA_CATEGORY = "category";
    public static final String INTENT_EXTRA_LOCAL_PATH = "local_path";
    public static final String INTENT_EXTRA_PATH = "path";
    public static final String INTENT_EXTRA_THEME_CONFIG ="theme_config";

}
