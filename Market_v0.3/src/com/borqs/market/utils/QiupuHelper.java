package com.borqs.market.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.conn.ssl.AbstractVerifier;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.borqs.market.listener.DownloadListener;

public class QiupuHelper {
    private static final String TAG = "QiupuHelper";


    private static ImageCacheManager cachemanager;
//    private static String tmpPath = MarketConfiguration.getCacheDirctory().getPath() + File.separator;
 
    static StatFs stat = null;

    public static int mVersionCode;
    private static HashMap<String,Long> downloadingMap;
    public static final HashMap<String,WeakReference<DownloadListener>> downloadlisteners;
    
    static {
        downloadlisteners = new HashMap<String,WeakReference<DownloadListener>>();
        downloadingMap = new HashMap<String,Long>();
        cachemanager = ImageCacheManager.instance();
    }
    

    public static String isImageExistInPhone(String url, boolean addHostAndPath) {
        String localpath = null;
        try {
            final URL imageurl = new URL(url);
            final String filepath = getImageFilePath(imageurl, addHostAndPath);
            final File file = new File(filepath);
            if (file.exists() == true && file.length() > 0) {
                localpath = filepath;
            } else if (url.endsWith(".icon.png")) {
                final String alterFilePath = getAlterLocalImageFilePath(
                        imageurl, addHostAndPath);
                final File alterFile = new File(alterFilePath);
                if (alterFile.exists() && alterFile.length() > 0) {
                    localpath = alterFilePath;
                }
            }
        } catch (java.net.MalformedURLException ne) {
            Log.d(TAG, "isImageExistInPhone exception=" + ne.getMessage()
                    + " url=" + url);
        }
        return localpath;

    }

    public static class myHostnameVerifier extends AbstractVerifier {
        public myHostnameVerifier() {

        }

        public final void verify(final String host, final String[] cns,
                final String[] subjectAlts) {
            Log.d(TAG, "host =" + host);
        }
    }

    public static String getImagePathFromURL_noFetch(String url) {
        try {
            URL imageurl = new URL(url);
            String filename = getImageFileName(imageurl.getFile());
            String filepath = MarketConfiguration.getCacheDirctory().getPath() + File.separator + new File(filename).getName();
            return filepath;
        } catch (MalformedURLException ne) {
        }
        return "";
    }

    public static String getImagePathFromURL(Context con, String url,
            boolean addHostAndPath) {
        if (url == null || url.length() == 0)
            return null;

        try {
            URL imageUrl = new URL(url);
            String filePath = getImageFilePath(imageUrl, addHostAndPath);

            File file = new File(filePath);
            if (file.exists() == false || file.length() == 0) {
                if (file.exists() == true) {
                    file.delete();
                }

                File savedFile = createTempPackageFile(con, filePath);
                if (!downloadImageFromInternet(imageUrl, savedFile)) {
                    file.delete();
                    return null;
                }
            }
            return filePath;
        } catch (java.net.MalformedURLException ne) {
            Log.e(TAG,
                    "getImageFromURL url=" + url + " exception="
                            + ne.getMessage());
            return null;
        }
    }

    private static String getImageFileName(String filename) {
        if (filename.contains("=") || filename.contains("?")
                || filename.contains("&") || filename.contains("%")) {
            filename = filename.replace("?", "");
            filename = filename.replace("=", "");
            filename = filename.replace("&", "");
            filename = filename.replace("%", "");
        }

        return filename;
    }

    /*
     * photos-a.ak.fbcdn.net api.facebook.com secure-profile.facebook.com
     * ssl.facebook.com www.facebook.com x.facebook.com api-video.facebook.com
     * developers.facebook.com iphone.facebook.com developer.facebook.com
     * m.facebook.com s-static.ak.facebook.com secure-profile.facebook.com
     * secure-media-sf2p.facebook.com ssl.facebook.com profile.ak.facebook.com
     * b.static.ak.facebook.com
     * 
     * photos-h.ak.fbcdn.net photos-f.ak.fbcdn.net
     */
    private static boolean isInTrustHost(String host) {
        if (host.contains(".fbcdn.net"))
            return true;

        if (host.contains("secure-profile.facebook.com"))
            return true;

        return false;
    }

    public static String getImageFilePath(URL imageUrl, boolean addHostAndPath) {
        return getImageFilePath(MarketConfiguration.getCacheDirctory().getPath() + File.separator, imageUrl, addHostAndPath);
    }

    public static String getImageFilePath(String path, URL imageUrl,
            boolean addHostAndPath) {
        if (addHostAndPath == false) {
            return path
                    + new File(getImageFileName(imageUrl.getFile())).getName();
        } else {
            String filename = imageUrl.getFile();
            filename = removeChar(filename);

            String host = imageUrl.getHost();
            if (isInTrustHost(host) == false) {

                filename = host + "_" + filename;
                if (filename.contains("/")) {
                    filename = filename.replace("/", "");
                }
            } else {
                Log.d(TAG, "***********   i am in trust=" + host + " filename="
                        + filename);
            }

            return path + new File(filename).getName();
        }
    }

    private static String removeChar(String filename) {
        if (filename.contains("=") || filename.contains("?")
                || filename.contains("&") || filename.contains("%")) {
            filename = filename.replace("?", "");
            filename = filename.replace("=", "");
            filename = filename.replace("&", "");
            filename = filename.replace("%", "");
            filename = filename.replace(",", "");
            filename = filename.replace(".", "");
            filename = filename.replace("-", "");

        }
        return filename;
    }

    private static Bitmap getImageFromPath(String filePath, int sampleSize, int maxNumOfPixels) {
        if (filePath != null) {
            Bitmap tmp = null;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, opts);
            
//            opts.inSampleSize = ImageRun.computeSampleSizeLarger(
//                    opts.outWidth, opts.outHeight, sampleSize);
            opts.inSampleSize = ImageRun.computeSampleSize(
                    opts, sampleSize, maxNumOfPixels);
            opts.inJustDecodeBounds = false;
            try {
                tmp = BitmapFactory.decodeFile(filePath, opts);
            } catch (OutOfMemoryError oof) {
                
            }
            return tmp;
        } else
            return null;
    }


    public static Bitmap getImageFromURL(Context con, String url,
            boolean isHighPriority, boolean addHostAndPath,
            boolean setRoundAngle, int width ,int maxNumOfPixels) {
//        ImageCacheManager.ImageCache cache = cachemanager.getCache(url);
        final Bitmap cache = ImageCacheManager.ContextCache
                .getImageFromCache(url, con.getClass()
                        .getName());
        if (cache == null || cache.isRecycled()) {
            final Bitmap image;
            String filePath = getImagePathFromURL(con, url, addHostAndPath);
            image = getImageFromPath(filePath, width, maxNumOfPixels);

            if (image != null) {
//                cachemanager.addCache(url, image);
                ImageCacheManager.ContextCache.putBitmapIntoMap(
                        con,
                        image,
                        url);
            }
            return image;
        } else {
            return cache;
        }
    }

    static File createTempPackageFile(Context con, String filePath) {
        File tmpPackageFile;
        if (filePath.startsWith(QiupuConfig.getSdcardPath()) == true) {
            tmpPackageFile = new File(filePath);
            return tmpPackageFile;
        }

        int i = filePath.lastIndexOf("/");
        String tmpFileName;
        if (i != -1) {
            tmpFileName = filePath.substring(i + 1);
        } else {
            tmpFileName = filePath;
        }
        FileOutputStream fos;
        try {
            fos = con.openFileOutput(tmpFileName, 1 | 2);
        } catch (FileNotFoundException e1) {
            Log.e(TAG, "Error opening file " + tmpFileName);
            return null;
        }
        try {
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Error opening file " + tmpFileName);
            return null;
        }
        tmpPackageFile = con.getFileStreamPath(tmpFileName);
        return tmpPackageFile;
    }

    public static void createAllWritableFolder(String pathName) {
        if (TextUtils.isEmpty(pathName)) {
            Log.d(TAG, "createAllWritableFolder, invalid path name: "
                    + pathName);
            return;
        }

        File pathFile = new File(pathName);
        if (!pathFile.exists()) {
            pathFile.mkdirs();

            Process p;
            int status;
            try {
                p = Runtime.getRuntime().exec("chmod 777 " + pathName);
                status = p.waitFor();
                if (status == 0) {
                    Log.i(TAG, "createAllWritableFolder, chmod succeed, "
                            + pathName);
                } else {
                    Log.i(TAG, "createAllWritableFolder, chmod failed, "
                            + pathName);
                }
            } catch (Exception ex) {
                Log.i(TAG, "createAllWritableFolder, chmod exception, "
                        + pathName);
                ex.printStackTrace();
            }
        }
    }

    private static String getAlterLocalImageFilePath(URL imageurl,
            boolean addHostAndPath) {
        final String alterPath;

        final String imageFilePath = imageurl.getFile();
        final int subStart = imageFilePath.lastIndexOf('/');
        final int subEnd = imageFilePath.indexOf('-', subStart);
        final String localFileName;
        if (subEnd > 0) {
            localFileName = imageFilePath.substring(subStart + 1, subEnd)
                    + ".png";
        } else {
            localFileName = imageFilePath.substring(subStart + 1) + ".png";
        }

        Log.d(TAG, "getAlterLocalImageFilePath, get localFileName:"
                + localFileName + ", from:" + imageFilePath);
        if (addHostAndPath == false) {
            alterPath = MarketConfiguration.getCacheDirctory().getPath() + File.separator
                    + new File(getImageFileName(localFileName)).getName();
        } else {
            String filename = removeChar(localFileName);
            filename = "local_" + filename;
            if (filename.contains("/")) {
                filename = filename.replace("/", "");
            }
            alterPath = MarketConfiguration.getCacheDirctory().getPath() + File.separator + new File(filename).getName();
        }

        Log.v(TAG, "getAlterLocalImageFilePath, return alter local path:"
                + alterPath);
        return alterPath;
    }



    private static boolean downloadImageFromInternet(URL imageUrl, File filep) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            TrafficStats.setThreadStatsTag(0xB0AC);
        // get bitmap
        HttpURLConnection conn = null;
        FileOutputStream fos = null;
        try {
            String filepath = filep.getAbsolutePath();

            fos = new FileOutputStream(filep);

            conn = (HttpURLConnection) imageUrl.openConnection();

            if (HttpsURLConnection.class.isInstance(conn)) {
                myHostnameVerifier passv = new myHostnameVerifier();
                ((HttpsURLConnection) conn).setHostnameVerifier(passv);
            }

            conn.setConnectTimeout(15 * 1000);
            conn.setReadTimeout(30 * 1000);
            InputStream in = conn.getInputStream();

            int retcode = conn.getResponseCode();
            if (retcode == 200) {
                final long totalLength = conn.getContentLength();

                long downlen = 0;
                int len = -1;
                byte[] buf = new byte[1024 * 4];
                while ((len = in.read(buf, 0, 1024 * 4)) > 0) {
                    downlen += len;
                    fos.write(buf, 0, len);
                }
                buf = null;

                if (totalLength == downlen) {
                    BLog.d(TAG, "downloadImageFromInternet, to file: " + filepath);
                } else {

                }
            }

            fos.close();
                BLog.d(TAG, "downloadImageFromInternet, to file: " + filepath);
        } catch (IOException ne) {
            Log.e(TAG, "fail to get image=" + ne.getMessage());
            return false;
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                TrafficStats.clearThreadStatsTag();
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ne) {
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ne) {
                }
            }
        }

        return true;
    }


    public static String getGoogleAccount(Context context) {
        String account = "test@borqs.com";
        if (context
                .checkCallingOrSelfPermission(android.Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_DENIED) {
            return account;
        }
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        if (accounts.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < accounts.length; i++) {
                Account acc = accounts[i];
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(acc.name);
            }
            account =  sb.toString();
        }
        return account;
    }

    public static byte[] toByteArray(File f) throws IOException {
        if (f.length() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(f + " is too large!");
        }
        int length = (int) f.length();
        byte[] content = new byte[length];
        int off = 0;
        int read = 0;
        InputStream in = new FileInputStream(f);
        try {
            while (read != -1 && off < length) {
                read = in.read(content, off, (length - off));
                off += read;
            }
            if (off != length) {
                // file size has shrunken since check, handle appropriately
            } else if (in.read() != -1) {
                // file size has grown since check, handle appropriately
            }
            return content;
        } finally {
            in.close();
        }
    }

    public static String getDeviceID(Context con) {
        String deviceid = "";
        WifiManager wm = (WifiManager) con
                .getSystemService(Context.WIFI_SERVICE);
        try {
            WifiInfo info = wm.getConnectionInfo();
            deviceid = info.getMacAddress().replace(":", "");
            BLog.d("DEVICE", "deviceid 1=" + deviceid);
        } catch (Exception ne) {
            ne.printStackTrace();
            BLog.d("DEVICE", "deviceid 1 exception=" + ne.getMessage());
        }

        if (con.checkCallingOrSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            // 2. imei/imsi
            TelephonyManager tm = (TelephonyManager) con
                    .getSystemService(Context.TELEPHONY_SERVICE);
            if (deviceid == null || deviceid.length() == 0) {
                String imei = tm.getDeviceId();
                String imsi = tm.getSubscriberId();

                deviceid = (imei == null ? "" : imei + imsi == null ? "" : imsi);
                BLog.d("DEVICE", "deviceid 2=" + deviceid);
            }
            // 3. phone number
            if (deviceid == null || deviceid.length() == 0) {
                deviceid = tm.getLine1Number();
                BLog.d("DEVICE", "deviceid 3=" + deviceid);
            }

        }
        if (deviceid == null) {
            deviceid = Installation.id(con);
            BLog.d("DEVICE", "deviceid 4=" + deviceid);
        }

        BLog.d("DEVICE", "deviceid=" + deviceid);
        return MD5.toMd5(deviceid.getBytes());
    }

    public static String toMD5String(File f) {
        try {
            return MD5.toMd5(toByteArray(f));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String user_agent = "os=android;client=com.borqs.market";
    public static void formatUserAgent(Context con)
    {
        PackageManager manager = con.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(con.getPackageName(), 0);
//            String versionName = info.versionName;
            String versionCode = String.valueOf(info.versionCode);
            String language = Locale.getDefault().getCountry();//TODO or getLanguage() to back 'zh'
            
            String deviceid = getDeviceID(con);
            
//            final String UA = String.format("os=android-%1$s-%2$s;client=%3$s;lang=%4$s;model=%5$s-%6$s;deviceid=%7$s",
//                    Build.VERSION.SDK, Build.CPU_ABI, versionCode, language, Build.BOARD, Build.BRAND, deviceid);
            
            final String UA = String.format("os=android-%1$s-%2$s;client=%3$s;lang=%4$s;model=%5$s-%6$s;deviceid=%7$s",
                    Build.VERSION.SDK, Build.CPU_ABI, versionCode, language, Build.MODEL, Build.BRAND, deviceid);
            Log.d(TAG, "formatUserAgent, phone UA Info : "+ UA);
            user_agent = UA;
        } catch (PackageManager.NameNotFoundException e){       
        }
    }
    
    
    public static void addDownloading(String key,Long value) {
        BLog.v(TAG,"addDownloading()");
        downloadingMap.put(key, value);
    }
    
    public static void removeDownloading(String key) {
        BLog.v(TAG,"removeDownloading()");
        downloadingMap.remove(key);
    }
    
    public static boolean hasDownloading() {
        if(downloadingMap != null || downloadingMap.size() > 0) {
            return true;
        }
        return false;
    }
    
    public static void registerDownloadListener(String key,DownloadListener listener){
        synchronized(downloadlisteners)
        {
            WeakReference<DownloadListener> ref = downloadlisteners.get(key);
            if(ref != null && ref.get() != null)
            {
                ref.clear();
            }
            downloadlisteners.put(key, new WeakReference<DownloadListener>(listener));
        }
    }
    
    public static void unregisterDownloadListener(String key){
        synchronized(downloadlisteners)
        {
            WeakReference<DownloadListener> ref = downloadlisteners.get(key);
            if(ref != null && ref.get() != null)
            {
                ref.clear();
            }
            downloadlisteners.remove(key);
        }
    }
    
    public static long queryDownloadId(String productid) {
        BLog.v(TAG,"queryDownloadId()");
        if(downloadingMap != null && downloadingMap.size() > 0) {
            if(downloadingMap.containsKey(productid)) {
                return downloadingMap.get(productid);
            }
        }
        return -1;
    }

    static long remainSize= 4*1024*1024L;
    public static boolean isEnoughSpace() {
        final String sdRoot = Environment.getExternalStorageDirectory().getPath();
        return isEnoughSpace(sdRoot);
    }
    public static boolean isEnoughSpace(final String path) {
        return isEnoughSpace(path, remainSize);
    }
    public static boolean isEnoughSpace(final String path, long reservedSize) {
        StatFs tmpstat = new StatFs(path);
        if(tmpstat != null)
        {
            int blockSize = tmpstat.getBlockSize();
            int availableBlocks = tmpstat.getAvailableBlocks();
            if (blockSize * ((long) availableBlocks - 4) <= reservedSize)
            {
                return false;
            }
        }

        return true;
    }
}
