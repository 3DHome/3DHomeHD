package com.borqs.se.home3d;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.borqs.se.home3d.XmlUtils.AnalysisException;
import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SESceneManager;

public class ProviderUtils {
    public static final String DATABASE_NAME = "home3dhd.db";
    public static final int DATABASE_VERSION = 12;
//    public static final int RELEASE_DATABASE_VERSION = 4;

//    public static final int DATABASE_VERSION = 54;// db version < 41 -> xml upgrade use db version;
                                                  // db version = 41 -> xml version is 1; 
                                                  // db version > 41 -> xml upgrade use xml version.
//    public static final int XML_VERSION = 3; // use to upgrade / downgrade xml info.(from 0.14.3.1215)

    public static final String AUTHORITY = "com.borqs.freehdhome.settings";
    public static final String PARAMETER_NOTIFY = "notify";
    public static final String SPLIT_SYMBOL = ",";

    public interface Tables {

        public static final String SCENE_INFO = "scene_config";
        public static final String CAMERA_DATA = "scene_camera";
        public static final String SCENE_LEFT_JOIN_ALL = SCENE_INFO + " LEFT JOIN " + CAMERA_DATA + " USING(_id)";

        public static final String MODEL_INFO = "model_info";
        public static final String IMAGE_INFO = "image_info";

        public static final String OBJECTS_INFO = "objects_config";
        public static final String VESSEL = "vessel";
        public static final String OBJECT_LEFT_JOIN_ALL = "Objects_Config LEFT JOIN Vessel ON Objects_Config._id=Vessel.objectID";
        
        public static final String THEME = "theme";
        public static final String PLUGIN = "plugin";

    }

    public static final class SceneInfoColumns implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + Tables.SCENE_INFO + "?"
                + PARAMETER_NOTIFY + "=false");

        public static final Uri CAMERA_DATA_URI = Uri.parse("content://" + AUTHORITY + "/" + Tables.CAMERA_DATA + "?"
                + PARAMETER_NOTIFY + "=false");

        private static final Uri LEFT_JOIN_ALL_URI = Uri.parse("content://" + AUTHORITY + "/"
                + Tables.SCENE_LEFT_JOIN_ALL + "?" + PARAMETER_NOTIFY + "=false");

        public static final String CLASS_NAME = "className";
        public static final String SCENE_NAME = "sceneName";
        public static final String WALL_NUM = "wall_num";
        public static final String WALL_SPANX = "wall_spanX";
        public static final String WALL_SPANY = "wall_spanY";
        public static final String WALL_SIZEX = "wall_sizeX";
        public static final String WALL_SIZEY = "wall_sizeY";
        public static final String WALL_HEIGHT = "wall_height";
        public static final String WALL_RADIUS = "wall_radius";
        public static final String WALL_ANGLE = "wall_angle";

        public static final String SKY_RADIUS = "sky_radius";
        public static final String DESK_HEIGHT = "desk_height";
        public static final String DESK_RADIUS = "desk_radius";
        public static final String DESK_NUM = "desk_num";
        public static final String FOV = "fovDegree";
        public static final String NEAR = "near";
        public static final String FAR = "far";
        public static final String TYPE = "type";
        public static final String LOCATION = "cameraPos";
        public static final String TARGETPOS = "cameraTargetPos";
        public static final String ZAXIS = "zaxis";
        public static final String UP = "cameraUp";

        public static final String CELL_WIDTH = "width";
        public static final String CELL_HEIGHT = "height";
    }

    public static final class ObjectInfoColumns implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + Tables.OBJECTS_INFO + "?"
                + PARAMETER_NOTIFY + "=false");
        public static final Uri OBJECT_LEFT_JOIN_ALL = Uri.parse("content://" + AUTHORITY + "/" + Tables.OBJECT_LEFT_JOIN_ALL + "?"
                + PARAMETER_NOTIFY + "=false");
        public static final String OBJECT_ID = "_id";
        
        public static final String OBJECT_NAME = "name";

        public static final String OBJECT_TYPE = "type";

        public static final String OBJECT_INDEX = "objectIndex";

        public static final String SHADER_TYPE = "shaderType";

        public static final String SCENE_NAME = "sceneName";

        public static final String COMPONENT_NAME = "componentName";

        public static final String CLASS_NAME = "className";

        public static final String SHORTCUT_ICON = "shortcutIcon";

        public static final String SHORTCUT_URL = "shortcutUri";

        public static final String WIDGET_ID = "widgetId";

        public static final String IS_NATIVE_OBJ = "isNativeObj";

        public static final String SLOT_TYPE = "slotType";

        public static final String DISPLAY_NAME = "display_name";
        public static final String ORIENTATION_STATUS = "orientation_status";
    }

    public static final class ModelColumns implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + Tables.MODEL_INFO + "?"
                + PARAMETER_NOTIFY + "=false");
        public static final Uri IMAGE_INFO_URI = Uri.parse("content://" + AUTHORITY + "/" + Tables.IMAGE_INFO);

        public static final String OBJECT_NAME = "name";

        public static final String COMPONENT_NAME = "componentName";

        public static final String TYPE = "type";

        public static final String SCENE_FILE = "sceneFile";

        public static final String BASEDATA_FILE = "basedataFile";

        public static final String ASSETS_PATH = "assetsPath";

        public static final String PREVIEW_TRANS = "previewTrans";

        public static final String LOCAL_TRANS = "localTrans";

        public static final String CLASS_NAME = "className";

        public static final String SCENE_NAME = "sceneName";

        public static final String KEY_WORDS = "keyWords";

        public static final String CHILD_NAMES = "childNames";

        public static final String SLOT_TYPE = "slotType";

        public static final String SLOT_SPANX = "spanX";

        public static final String SLOT_SPANY = "spanY";

        public static final String PLACEMENT_STRING = "objectPlacement";

        public static final String THEME_NAME = "themeName";
        public static final String IAMGE_NAME = "imageName";
        public static final String IMAGE_PATH = "imagePath";
        public static final String IMAGE_NEW_PATH = "imageNewPath";
        
        public static final String PRODUCT_ID = "product_id";
        public static final String VERSION_NAME = "version_name";
        public static final String VERSION_CODE = "version_code";
        public static final String PLUG_IN = "plug_in";
    }

    public static final class VesselColumns implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + Tables.VESSEL + "?"
                + PARAMETER_NOTIFY + "=false");
        public static final String VESSEL_ID = "vesselID";
        public static final String OBJECT_ID = "objectID";
        public static final String SLOT_INDEX = "slot_Index";
        public static final String SLOT_StartX = "slot_StartX";
        public static final String SLOT_StartY = "slot_StartY";
        public static final String SLOT_SpanX = "slot_SpanX";
        public static final String SLOT_SpanY = "slot_SpanY";
        //add for mountPointIndex
        public static final String SLOT_MPINDEX = "slot_mountpointIndex";
        public static final String SLOT_BP_MOVEPLANE = "slot_boundarypoint_movePlane";
        public static final String SLOT_BP_WALLINDEX = "slot_boundaryPoint_wallIndex";
        public static final String SLOT_BP_MIN_MATRIXPOINT_ROW = "slot_boundarypoint_minMatrixPointRow";
        public static final String SLOT_BP_MIN_MATRIXPOINT_COL = "slot_boundarypoint_minMatrixPointCol";
        
        public static final String SLOT_BP_MAX_MATRIXPOINT_ROW = "slot_boundarypoint_maxMatrixPointRow";
        public static final String SLOT_BP_MAX_MATRIXPOINT_COL = "slot_boundarypoint_maxMatrixPointCol";
        
        public static final String SLOT_BP_CENTER_X = "slot_boundarypoint_center_x";
        public static final String SLOT_BP_CENTER_Y = "slot_boundarypoint_center_y";
        public static final String SLOT_BP_CENTER_Z = "slot_boundarypoint_center_z";
        
        public static final String SLOT_BP_XYZSPAN_X = "slot_boundarypoint_xyzSpan_x";
        public static final String SLOT_BP_XYZSPAN_Y = "slot_boundarypoint_xyzSpan_y";
        public static final String SLOT_BP_XYZSPAN_Z = "slot_boundarypoint_xyzSpan_z";
        
        
        //end
    }

//    public static final class FileURLInfoColumns implements BaseColumns {
//        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/file_url_info");
//
//        public static final String NAME = "name";
//        public static final String SERVER_PATH = "server_path";
//        public static final String LOCAL_PATH = "local_path";
//        public static final String TYPE = "type";
//        public static final String FILE_LENGTH = "fileLength";
//        public static final String THREAD_INFOS = "threadInfos";
//        public static final String DOWNLOAD_STATUS = "download_status";
//        public static final String APPLY_STATUS = "apply_status";
//
//    }

    public static final class ImageInfoColumns implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + Tables.IMAGE_INFO);
    }

    public static final class ThemeColumns implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + Tables.THEME);
        public static final String NAME = "name";
        public static final String FILE_PATH = "file_path";
        public static final String TYPE = "type";
        public static final String IS_APPLY = "is_apply";
        public static final String CONFIG = "config";
        public static final String PRODUCT_ID = "product_id";
        public static final String VERSION_NAME = "version_name";
        public static final String VERSION_CODE = "version_code";
    }
    
    public static final class PluginColumns implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + Tables.PLUGIN);
        public static final String Name = "name";
        public static final String PRODUCT_ID = "product_id";
        public static final String VERSION_NAME = "version_name";
        public static final String VERSION_CODE = "version_code";
        public static final String TYPE = "type";
        public static final String IS_APPLY = "is_apply";
    }

    public static int searchMaxIndex(SEScene scene, Uri uri, String name) {
        String where = ObjectInfoColumns.SCENE_NAME + "='" + scene.mSceneName + "' AND "
                + ObjectInfoColumns.OBJECT_NAME + "='" + name + "'";
        Context context = SESceneManager.getInstance().getContext();
        Cursor cursor = context.getContentResolver().query(uri,
                new String[] { "max(" + ObjectInfoColumns.OBJECT_INDEX + ") as max_index" }, where, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int max_index = cursor.getInt(0);
            cursor.close();
            return max_index;
        }
        return 0;
    }

    public static int searchMaxIndex(SEScene scene, String table, String name) {
        String where = ObjectInfoColumns.SCENE_NAME + "='" + scene.mSceneName + "' AND "
                + ObjectInfoColumns.OBJECT_NAME + "='" + name + "'";
        Context context = SESceneManager.getInstance().getContext();
        Cursor cursor = HomeDataBaseHelper
                .getInstance(context)
                .getReadableDatabase()
                .query(table, new String[]{"max(" + ObjectInfoColumns.OBJECT_INDEX + ") as max_index"}, where,
                        null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int max_index = cursor.getInt(0);
            cursor.close();
            return max_index;
        }
        return 0;
    }

    public static float[] getFloatArray(String str, int checking) {
        String[] strArray = str.split(ProviderUtils.SPLIT_SYMBOL);
        if (strArray.length % checking != 0) {
            throw new AnalysisException("The length of array is wrong," + str);
        } else {
            float[] floatArray = new float[strArray.length];
            int index = 0;
            for (String s : strArray) {
                floatArray[index++] = Float.parseFloat(s.trim());
            }
            return floatArray;
        }
    }

    public static int[] getIntArray(String str, int checking) {
        String[] strArray = str.split(ProviderUtils.SPLIT_SYMBOL);
        if (strArray.length % checking != 0) {
            throw new AnalysisException("The length of array is wrong," + str);
        } else {
            int[] intArray = new int[strArray.length];
            int index = 0;
            for (String s : strArray) {
                intArray[index++] = Integer.parseInt(s.trim());
            }
            return intArray;
        }
    }

    public static String[] getStringArray(String str) {
        String newStr = str;
        Pattern p = Pattern.compile("\\s*|\t|\r|\n");
        Matcher m = p.matcher(newStr);
        newStr = m.replaceAll("");
        return newStr.split(ProviderUtils.SPLIT_SYMBOL);
    }

    public static String floatArrayToString(float[] floatArray) {
        String strs = "";
        for (float s : floatArray) {
            strs = strs + s + ",";
        }
        if (strs.length() > 0) {
            strs = strs.substring(0, strs.length() - 1);
        }
        return strs;
    }

    public static String intArrayToString(int[] intArray) {
        String strs = "";
        for (int s : intArray) {
            strs = strs + s + ",";
        }
        if (strs.length() > 0) {
            strs = strs.substring(0, strs.length() - 1);
        }
        return strs;
    }

    public static String stringArrayToString(String[] strArray) {
        String strs = "";
        for (String s : strArray) {
            strs = strs + s + ",";
        }
        if (strs.length() > 0) {
            strs = strs.substring(0, strs.length() - 1);
        }
        return strs;
    }

    private static final String WHERE_SCENE_NAME = SceneInfoColumns.SCENE_NAME + "=?";
    private static Cursor queryUriCursor(Context context, String sceneName, Uri uri) {
        Cursor cursor;
        try {
            String where = WHERE_SCENE_NAME;
            String[] whereArgs = new String[] {sceneName};
            cursor = context.getContentResolver().query(uri, null, where, whereArgs, null);
        } catch (Exception e) {
            cursor = null;
        }
        return cursor;
    }

    public static Cursor querySceneInfo(Context context, String sceneName) {
        return queryUriCursor(context, sceneName, SceneInfoColumns.LEFT_JOIN_ALL_URI);
    }

    public static Cursor queryAllModelInfo(Context context, String sceneName) {
        return  queryUriCursor(context, sceneName, ModelColumns.CONTENT_URI);
    }
}
