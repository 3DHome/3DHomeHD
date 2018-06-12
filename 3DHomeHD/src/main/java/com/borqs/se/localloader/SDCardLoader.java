package com.borqs.se.localloader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Xml;

import com.borqs.se.home3d.XmlUtils;
import com.borqs.se.download.ZipUtil;
import com.borqs.se.home3d.HomeDataBaseHelper;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.engine.SESceneManager;

/**
 * Created by b050 on 7/8/13.
 */
public class SDCardLoader {
    private static final String TAG = "SDCardLoader";
    private String[] mObjectFileNames = new String[]{};
    private String mRootPath = HomeUtils.SDCARD_PATH;
    private HashMap<String, ModelInfo> mModels = new HashMap<String, ModelInfo>();
    public SDCardLoader() {
        String path = mRootPath;
        File dirRoot = new File(path);
        if (!dirRoot.exists()) {
        	dirRoot.mkdirs();
        }
        if(dirRoot.isDirectory()) {
            String[] fileNames = dirRoot.list();
            mObjectFileNames = fileNames;
            String tmpPath = path + "/tmp";
            File tmpPathFile = new File(tmpPath);
            if(!tmpPathFile.exists()) {
                boolean bCreatedir = tmpPathFile.mkdir();
                if(!bCreatedir) {
                    Log.i(TAG, "create tmp dir error!!");
                }
            }
        } else {
            Log.i(TAG, "can not file path : " + path);
        }
    }
    private void handleModelsConfig(File file, String parentPath, String fileName) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            XmlUtils.beginDocument(parser, "config");
            ModelInfo config = ModelInfo.CreateFromXml(parser);
            config.mAssetsPath = parentPath;
            config.changeImageInfoPath(parentPath);
            config.mName = config.mName + "_sdcard";
            String name = config.mName;
            mModels.put(name, config);
            Log.i(TAG, "## model name = " + name + " ##");

            HomeDataBaseHelper help = HomeDataBaseHelper.getInstance(SESceneManager.getInstance()
                    .getContext());
            SQLiteDatabase db2 = help.getWritableDatabase();
            config.saveToDB(db2);
            SESceneManager.getInstance().addModelToScene(config);
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }
    private void showFiles(File resourceDir, String resourceDirPath) {
        if(!resourceDir.isDirectory()) {
            return;
        }
        String[] fileNames = resourceDir.list();
        for(String fileName : fileNames) {
            if(fileName.equals(".svn")) {
                continue;
            }
            String filePath = resourceDirPath + File.separator + fileName;
            File file = new File(filePath);
            if(file.isDirectory()) {
                showFiles(file, filePath);
            } else {
                if(fileName.equalsIgnoreCase("models_config.xml")) {
                    handleModelsConfig(file, resourceDirPath, fileName);
                }
            }
        }
    }
    public void createModels() {
        for(String fileName : mObjectFileNames) {
            Log.i(TAG, "file name = " + fileName);
            String filePath = mRootPath + File.separator + fileName;
            if(!fileName.endsWith(".zip")) {
                continue;
            }
            String filePrefix = fileName.substring(0, fileName.length() - 4);
            String unzipDir =  mRootPath + File.separator + "tmp" + File.separator + filePrefix;
            File unzipFile = new File(unzipDir);
            if(!unzipFile.exists()) {
                continue;
            }
            showFiles(unzipFile, unzipDir);
        }

    }
    public void loadFiles() {
        for(String fileName : mObjectFileNames) {
            Log.i(TAG, "file name = " + fileName);
            String filePath = mRootPath + File.separator + fileName;
            if(!fileName.endsWith(".zip")) {
                continue;
            }
            String filePrefix = fileName.substring(0, fileName.length() - 4);
            String unzipDir =  mRootPath + File.separator + "tmp" + File.separator + filePrefix;
            File unzipFile = new File(unzipDir);
            if(!unzipFile.exists()) {
                try {
                    ZipUtil.unzip(filePath, unzipDir);
                } catch (IOException e) {
                    Log.i(TAG, "unzip file error");
                }
            }
        }

    }
}
