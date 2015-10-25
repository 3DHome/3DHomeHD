package com.borqs.market.wallpaper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;

import com.borqs.market.api.ApiUtil;
import com.borqs.market.db.DownLoadProvider;
import com.borqs.market.db.DownloadInfo;
import com.borqs.market.db.PlugInColumns;
import com.borqs.market.db.PlugInInfo;
import com.borqs.market.json.Product;
import com.borqs.market.net.RequestListener;
import com.borqs.market.net.WutongException;
import com.borqs.market.utils.ImageRun;
import com.borqs.market.utils.IntentUtil;
import com.borqs.market.utils.MarketConfiguration;
import com.borqs.market.utils.MarketUtils;

/**
 * Created by b608 on 13-8-1.
 */
public class WallpaperUtils {

    public static final String EXTRA_RAW_WALL_PAPERS = "EXTRA_RAW_WALL_PAPERS";
//    public static final String EXTRA_RAW_GROUND_PAPERS = "EXTRA_RAW_GROUND_PAPERS";
    private static final String TAG = "WallpaperUtils";

    private WallpaperUtils() {
        // no instance.
    }
    private static void touchFolder(String path) {
        File dirRoot = new File(path);
        if (!dirRoot.exists()) {
            dirRoot.mkdirs();
        }
    }
    public static boolean ENABLE_BACKDOOR = false;
    private static String WALLPAPER_LOCAL_PATH;
    private static String WALLPAPER_EXPORT_PATH;
    public static void setApplicationRootPath(String path, int version) {
        WALLPAPER_LOCAL_PATH = getWallpaperFolder(path);
        WALLPAPER_EXPORT_PATH = getWallpaperFolder(path + File.separator + EXPORT_PATH_NAME);
        touchFolder(WALLPAPER_EXPORT_PATH);

        if (version < 1) {
            runFolderRemove(WALLPAPER_LOCAL_PATH + File.separator + "export-port");
            runFolderRemove(WALLPAPER_LOCAL_PATH + File.separator + "export-land");
            runFolderRemove(WALLPAPER_LOCAL_PATH + File.separator + "export-port.zip");
            runFolderRemove(WALLPAPER_LOCAL_PATH + File.separator + "export-land.zip");
        }
    }

    /**
     *
     * @param context
     * @param package_name
     *            application package name
     */

    public static void startProductListIntent(Context context, String package_name, String sdRoot,
                                              boolean isOnlyFree, boolean isPort,
                                              ArrayList<RawPaperItem> wallPaperItems) {
        String supported_mod = isPort ? Product.SupportedMod.PORTRAIT : Product.SupportedMod.LANDSCAPE;
        WALLPAPER_LOCAL_PATH = getWallpaperFolder(sdRoot);
        MarketUtils.startLocalProductListIntent(context, package_name, MarketUtils.CATEGORY_WALLPAPER,
                WALLPAPER_LOCAL_PATH, isOnlyFree, supported_mod, wallPaperItems);
//        MarketUtils.startLocalProductListIntent(context, MarketUtils.CATEGORY_WALLPAPER, localPath, isOnlyFree, supported_mod);
    }

    /**
     *
     * @param context
     */
    public static void startExportOrImportIntent(Context context, boolean isPort, ArrayList<RawPaperItem> wallPaperItems) {
//        WALLPAPER_EXPORT_PATH = localPath;
        final String package_name = MarketConfiguration.PACKAGE_NAME;
        if (TextUtils.isEmpty(package_name)) {
            throw new IllegalArgumentException("package name is null");
        }
        try {
            int version_code = context.getPackageManager()
                    .getPackageInfo(package_name, PackageManager.GET_UNINSTALLED_PACKAGES).versionCode;

            Intent intent = new Intent(context, WallpaperExportActivity.class);
            intent.putExtra(MarketUtils.EXTRA_APP_VERSION, version_code);
            intent.putExtra(MarketUtils.EXTRA_PACKAGE_NAME, package_name);
            intent.putExtra(MarketUtils.EXTRA_CATEGORY, MarketUtils.CATEGORY_WALLPAPER);
            intent.putExtra(MarketUtils.EXTRA_LOCAL_PATH, WALLPAPER_EXPORT_PATH);
            intent.putExtra(MarketUtils.EXTRA_MOD, isPort ? Product.SupportedMod.PORTRAIT : Product.SupportedMod.LANDSCAPE);
            intent.putParcelableArrayListExtra(WallpaperUtils.EXTRA_RAW_WALL_PAPERS, wallPaperItems);
//            intent.putParcelableArrayListExtra(WallpaperUtils.EXTRA_RAW_GROUND_PAPERS, groundPaperItems);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<PlugInInfo> convertToPluginInfo(List<Product> productList) {
        List<PlugInInfo> resultList = new ArrayList<PlugInInfo>();
        if (null == productList) {
            return resultList;
        }

        PlugInInfo info;
        for (Product product : productList) {
            info = PlugInInfo.fromProduct(product);
            info.type = Product.ProductType.WALL_PAPER;

            resultList.add(info);
        }
        return resultList;
    }


    public static int resetWallpaperPlugIn(Context context) {
        String where = PlugInColumns.TYPE + "=?" ;
        return context.getContentResolver().delete(DownLoadProvider.getContentURI(context, DownLoadProvider.TABLE_PLUGIN), where, new String[] {Product.ProductType.WALL_PAPER});
    }

    public static void bulkInsertProductList(Context context, List<Product> productList) {
        resetWallpaperPlugIn(context);
        MarketUtils.bulkInsertPlugIn(context, convertToPluginInfo(productList));
    }

    public static String getWallpaperFolder(String sdRoot) {
        return sdRoot + File.separator + "." + Product.ProductType.WALL_PAPER;

    }

    public static String getWallPaperPath(String productId) {
        String path = WALLPAPER_LOCAL_PATH;

        File dirRoot = new File(path);
        if (!dirRoot.exists()) {
            dirRoot.mkdirs();
        }

        String localPath ;
        if(dirRoot.isDirectory()) {
            localPath = path + File.separator + productId.toLowerCase();
            File tmpPathFile = new File(localPath);
            if(!tmpPathFile.exists()) {
                boolean bCreatedir = tmpPathFile.mkdir();
                if(!bCreatedir) {
                    Log.i("getWallPaperPath: ", "create tmp dir error!!");
                    return "";
                }
            }
        } else {
            Log.i("getWallPaperPath: ", "can not file path : " + path);
            return "";
        }
        return localPath;
    }

    private static final String EXPORT_PATH_NAME = "exporting";
    private static final String PORT_PAPER_PATH = "port";
    private static final String LAND_PAPER_PATH = "land";
    private static final String MANIFEST_FILE_NAME = "ResourceManifest.xml";
    private static void runFolderRemove(String path) {
        try {
            Runtime.getRuntime().exec("rm -r " + path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void ensureExportWallpaperFolder(String exportPath, boolean force) {
        File tmpPathFile = new File(exportPath);
        if (tmpPathFile.exists() && tmpPathFile.isDirectory()) {
            if (force) {
                final String tempFilePath = exportPath + "_del_" + System.currentTimeMillis();
                tmpPathFile.renameTo(new File(tempFilePath));
                tmpPathFile.mkdir();
                runFolderRemove(tempFilePath);
            }
            return;
        }

        File dirRoot = new File(getExportFolderRoot());
        if (!dirRoot.exists() || !dirRoot.isDirectory()) {
            dirRoot.deleteOnExit();
            dirRoot.mkdir();
        }

        if(!tmpPathFile.exists() || !tmpPathFile.isDirectory()) {
            tmpPathFile.deleteOnExit();
            tmpPathFile.mkdir();
        }
    }

    private static String getExportFolderRoot() {
        return WALLPAPER_EXPORT_PATH;
    }
    private static String getExportPaperFolderPath(int orientation) {
        final String folder = getExportFolderRoot();

        if (orientation == Wallpaper.PaperOrientation.PORT) {
            return folder + File.separator + PORT_PAPER_PATH;
        } else if (orientation == Wallpaper.PaperOrientation.LAND) {
            return folder + File.separator + LAND_PAPER_PATH;
        } else {
            return folder + File.separator + LAND_PAPER_PATH;
        }
    }
    private static String resetExportWallpaperFolder(boolean force, int orientation) {
        final String exportPath = getExportPaperFolderPath(orientation);
        ensureExportWallpaperFolder(exportPath, force);
        return exportPath;
    }

    private static int parseOrientationMode(String supported_mod) {
        if (Product.SupportedMod.LANDSCAPE.equalsIgnoreCase(supported_mod)) {
            return Wallpaper.PaperOrientation.LAND;
        } else if (Product.SupportedMod.PORTRAIT.equalsIgnoreCase(supported_mod)) {
            return Wallpaper.PaperOrientation.PORT;
        } else {
            return Wallpaper.PaperOrientation.NONE;
        }
    }

    public static void exportWallpaperSuite(Wallpaper.Builder builder, String supported_mod,
                                            final RequestListener listener) {

        Wallpaper.PaperOrientation orientation = new Wallpaper.PaperOrientation();
        orientation.orientation = parseOrientationMode(supported_mod);

        final String exportRoot = resetExportWallpaperFolder(false, orientation.orientation);
        final File manifestFile = new File(exportRoot + File.separator + MANIFEST_FILE_NAME);
        try {
            if (!manifestFile.exists()) {
                manifestFile.createNewFile();
            }
//            OutputStream out = new FileOutputStream(manifestFile);
//            List<Object> manifestTags = new ArrayList<Object>();
//            WallpaperXmlUtils.writeListXml(manifestTags, out);
//            out.close();
            builder.setOrientation(orientation);
            builder.buildManifest(manifestFile);
//            testCreateXml(manifestFile, builder.create());
            final String zipFileName = exportRoot + ".zip";
            ZipFolder(exportRoot, zipFileName);
//            performUpload(builder, zipFileName, listener);
            ApiUtil.getInstance().shareUpload(builder.hostPackageName, builder.appVersion,
                    Product.ProductType.WALL_PAPER, supported_mod,
                    new File(zipFileName), new RequestListener() {
                @Override
                public void onComplete(String response) {
                    listener.onComplete(response);
                    runFolderRemove(exportRoot);
//                    runFolderRemove(zipFileName);
                }

                @Override
                public void onIOException(IOException e) {
                    listener.onIOException(e);
                }

                @Override
                public void onError(WutongException e) {
                    listener.onError(e);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private static final String START_TAG = "BorqsResource";
//    private static void testCreateXml(File xmlFile, Wallpaper paper) {
//        try {
//            FileOutputStream outputStream = new FileOutputStream(xmlFile);
//            XmlSerializer serializer=Xml.newSerializer();
//            serializer.setOutput(outputStream, "utf-8");
//
//            serializer.startDocument("UTF-8", true);
//            //第一个参数为命名空间,如果不使用命名空间,可以设置为null
//            serializer.startTag(null, START_TAG);
//            paper.serializeXmlTags(serializer);
//            serializer.endTag(null, START_TAG);
//            serializer.endDocument();
//            outputStream.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.e(TAG, "testCreateXml, exception = " + e.getMessage());
//        } finally {
//        }
//    }

    public static void clearAppliedFlag(Context context) {
        MarketUtils.updatePlugIn(context, "dumb_id", false, MarketUtils.CATEGORY_WALLPAPER);
    }

    public static String getDecodedPaperPath(RawPaperItem rawPaperItem) {
        if (!TextUtils.isEmpty(rawPaperItem.mDecodedValue)) {
            return rawPaperItem.mDecodedValue;
        } else if (!TextUtils.isEmpty(rawPaperItem.mOverlayValue)) {
            return rawPaperItem.mDecodedValue;
        } else {
            Log.d(TAG, "getDecodedPaperPath, use default paper without overlay one.");
            return rawPaperItem.mDefaultValue;
        }
    }

    private static boolean isFileExisting(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File testFile = new File(path);
        return testFile.exists();
    }
    public static boolean decodePaper(Context context, ArrayList<RawPaperItem> itemList, boolean portMode) {
//        final String dstPath = resetExportWallpaperFolder(true, h2wRatio > 1.0f ?
//                Wallpaper.PaperOrientation.PORT : Wallpaper.PaperOrientation.LAND);
        final int orientation = portMode ? Wallpaper.PaperOrientation.PORT : Wallpaper.PaperOrientation.LAND;
        final String dstPath = resetExportWallpaperFolder(true, orientation);
        String fileName;
        HashMap<String, String> decodedMap = new HashMap<String, String>();
        boolean succeed = true;
        for (RawPaperItem item : itemList) {
            if (!TextUtils.isEmpty(item.mDecodedValue)) {
                Log.i(TAG, "decodePaper, skip with decoded path = " + item.mDecodedValue);
                continue;
            }

            String srcFilePath = item.mOverlayValue;
            if (!isFileExisting(srcFilePath)) {
                srcFilePath = item.mDefaultValue;
            }

            if (!TextUtils.isEmpty(item.mDecodedValue = decodedMap.get(srcFilePath))) {
                continue;
            }

            final int imageW;
            final int imageH;

            final int offsetW;
            final int offsetH;

            if (item.mRatioH2W == 1.0f || RawPaperItem.TYPE_GROUND.equals(item.mType)) {
                imageW = imageSize;
                offsetW = 0;

                imageH = imageSize;
                offsetH = 0;

            } else if (item.mRatioH2W > 1.0f) {
                // portrait
                final int rawWidth = (int)(imageSize / item.mRatioH2W + 0.5);
//                imageW = (rawWidth + ROUND_WALL_WIDTH) - (rawWidth % ROUND_WALL_WIDTH);
                imageW = rawWidth;
                offsetW = (imageSize - imageW) / 2;

                imageH = imageSize;
                offsetH = 0;
            } else if (item.mRatioH2W < 1.0f) {
                // landscape
                imageW = imageSize;
                offsetW = 0;

                final int rawHeight = (int)(imageSize * item.mRatioH2W + 0.5);
//                imageH = (rawHeight + ROUND_WALL_HEIGHT) - (rawHeight % ROUND_WALL_HEIGHT);
                imageH = rawHeight;
                offsetH = (imageSize - imageH) / 2;
            } else {
                imageW = imageSize;
                offsetW = 0;

                imageH = imageSize;
                offsetH = 0;
            }

            Bitmap bm = ImageRun.decodeImageFile(context, srcFilePath, imageSize, imageSize * imageSize);
            if (null == bm) {
                Log.e(TAG, "decodePaper, skip file = " + srcFilePath);
                continue;
            }

            fileName = srcFilePath.substring(srcFilePath.lastIndexOf(File.separator));
            final int width = bm.getWidth();
            final int height = bm.getHeight();
            final int srcOffsetWidth = (int)(0.5 + 1f * offsetW * width / imageSize);
            final int srcOffsetHeight = (int)(0.5 + 1f * offsetH * height / imageSize);

//        Rect srcRect = new Rect(0, 0, width, height);
//            final int srcOffsetW;
//            final int srcOffsetH;
//            final int relativeH = Math.round(width * h2wRatio);
//            if (relativeH < height) {
//                srcOffsetW = width;
//                srcOffsetH = Math.round(width * h2wRatio);
//            } else if (relativeH > height) {
//                srcOffsetH = height;
//                srcOffsetW = Math.round(height / h2wRatio);
//            } else {
//                srcOffsetW = width;
//                srcOffsetH = height;
//            }

//            Rect srcRect = new Rect(0, 0, srcOffsetW, srcOffsetH);
//            Rect desRect = new Rect(offsetW, offsetH, imageW + offsetW, imageH + offsetH);
//            Bitmap des = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888);

            Rect srcRect = new Rect(srcOffsetWidth, srcOffsetHeight,
                    width - srcOffsetWidth,
                    height - srcOffsetHeight);

            Rect desRect = new Rect(0, 0, imageW, imageH);
            Bitmap des = Bitmap.createBitmap(imageW, imageH, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(des);
            canvas.drawBitmap(bm, srcRect, desRect, null);
            bm.recycle();
            item.mDecodedValue = dstPath + File.separator + fileName;
            if (!saveBitmap(des, item.mDecodedValue, Bitmap.CompressFormat.JPEG)) {
                succeed = false;
            }
            des.recycle();

            decodedMap.put(item.mOverlayValue, item.mDecodedValue);
        }

        return succeed;
    }

//    private static final int ROUND_WALL_WIDTH = 6;
//    private static final int ROUND_WALL_HEIGHT = 10;

//    private static final int ROUND_WALL_WIDTH = 2;
//    private static final int ROUND_WALL_HEIGHT = 2;

    final static int imageSize = 1024;
    public static String encodeWallPaper(final String srcPath, final String targetFolder, final float h2wRatio) {
        final String dstPath = targetFolder + File.separator + new File(srcPath).getName();
        encodePaperFile(srcPath, dstPath, h2wRatio);
        return dstPath;
    }

    public static boolean encodePaperFile(final String srcPath, final String dstPath, final float h2wRatio) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(srcPath, options);
        options.inJustDecodeBounds = false;
        Bitmap bm = BitmapFactory.decodeFile(srcPath, options);
        if (bm == null) {
            return false;
        }

        final int imageW;
        final int imageH;

        final int offsetW;
        final int offsetH;

        if (h2wRatio > 1.0f) {
            // portrait
            final int rawWidth = (int)(imageSize / h2wRatio + 0.5);
//            imageW = (rawWidth + ROUND_WALL_WIDTH) - (rawWidth % ROUND_WALL_WIDTH);
            imageW = rawWidth;
            offsetW = (imageSize - imageW) / 2;

            imageH = imageSize;
            offsetH = 0;
        } else if (h2wRatio < 1.0f) {
            // landscape
            imageW = imageSize;
            offsetW = 0;

            final int rawHeight = (int)(imageSize * h2wRatio + 0.5);
//            imageH = (rawHeight + ROUND_WALL_HEIGHT) - (rawHeight % ROUND_WALL_HEIGHT);
            imageH = rawHeight;
            offsetH = (imageSize - imageH) / 2;
        } else {
            imageW = imageSize;
            offsetW = 0;

            imageH = imageSize;
            offsetH = 0;
        }

        Bitmap des = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888);

        final int width = bm.getWidth();
        final int height = bm.getHeight();
//        Rect srcRect = new Rect(0, 0, width, height);
        final int srcOffsetW;
        final int srcOffsetH;
        final int relativeH = Math.round(width * h2wRatio);
        if (relativeH < height) {
            srcOffsetW = width;
            srcOffsetH = Math.round(width * h2wRatio);
        } else if (relativeH > height) {
            srcOffsetH = height;
            srcOffsetW = Math.round(height / h2wRatio);
        } else {
            srcOffsetW = width;
            srcOffsetH = height;
        }

        Rect srcRect = new Rect(0, 0, srcOffsetW, srcOffsetH);
        Rect desRect = new Rect(offsetW, offsetH, imageW + offsetW, imageH + offsetH);

        Canvas canvas = new Canvas(des);
        canvas.drawBitmap(bm, srcRect, desRect, null);
        bm.recycle();
        saveBitmap(des, dstPath, parseFormatByExtension(srcPath));
        des.recycle();
        return true;
    }

    private static Bitmap.CompressFormat parseFormatByExtension(String path) {
        if (path.toLowerCase().endsWith(".png")) {
            return Bitmap.CompressFormat.PNG;
        } else {
            return Bitmap.CompressFormat.JPEG;
        }
    }

    public static boolean saveBitmap(Bitmap bitmap, String path, Bitmap.CompressFormat format) {
        try {
            File f = new File(path);
            f.createNewFile();
            FileOutputStream fOut = new FileOutputStream(f);
            bitmap.compress(format, 100, fOut);
            fOut.flush();
            fOut.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 压缩文件,文件夹
     * @param srcFileString 要压缩的文件/文件夹名字
     * @param zipFileString 指定压缩的目的和名字
     * @throws Exception
     */
    public static void ZipFolder(String srcFileString, String zipFileString)throws Exception {
        android.util.Log.v("XZip", "ZipFolder(String, String)");

        //创建Zip包
        java.util.zip.ZipOutputStream outZip = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zipFileString));

        //打开要输出的文件
        java.io.File file = new java.io.File(srcFileString);
        if (file.isDirectory()) {
            //压缩
//            ZipFiles(file.getParent()+java.io.File.separator, file.getName(), outZip);

            String fileList[] = file.list();
            for (int i = 0; i < fileList.length; i++) {
                ZipFiles(srcFileString, fileList[i], outZip);
            }//end of for
        }
        //完成,关闭
        outZip.finish();
        outZip.close();

    }//end of func

    /**
     * 压缩文件
     * @param folderString
     * @param fileString
     * @param zipOutputSteam
     * @throws Exception
     */
    private static void ZipFiles(String folderString, String fileString, java.util.zip.ZipOutputStream zipOutputSteam)throws Exception{
        android.util.Log.v("XZip", "ZipFiles(String, String, ZipOutputStream)");

        if(zipOutputSteam == null)
            return;

        java.io.File file = new java.io.File(folderString+java.io.File.separator+fileString);

        //判断是不是文件
        if (file.isFile()) {

            java.util.zip.ZipEntry zipEntry =  new java.util.zip.ZipEntry(fileString);
            java.io.FileInputStream inputStream = new java.io.FileInputStream(file);
            zipOutputSteam.putNextEntry(zipEntry);

            int len;
            byte[] buffer = new byte[4096];

            while((len=inputStream.read(buffer)) != -1)
            {
                zipOutputSteam.write(buffer, 0, len);
            }

            zipOutputSteam.closeEntry();
        }
        else {

            //文件夹的方式,获取文件夹下的子文件
            String fileList[] = file.list();

            //如果没有子文件, 则添加进去即可
            if (fileList.length <= 0) {
                java.util.zip.ZipEntry zipEntry =  new java.util.zip.ZipEntry(fileString+java.io.File.separator);
                zipOutputSteam.putNextEntry(zipEntry);
                zipOutputSteam.closeEntry();
            }

            //如果有子文件, 遍历子文件
            for (int i = 0; i < fileList.length; i++) {
                ZipFiles(folderString, fileString+java.io.File.separator+fileList[i], zipOutputSteam);
            }//end of for

        }//end of if

    }//end of func

    // check and import last export wallpaper
    public static void checkAndImportWallpapers(Context context, boolean isPort) {
        Wallpaper.PaperOrientation orientation = new Wallpaper.PaperOrientation();
        orientation.orientation = isPort ? Wallpaper.PaperOrientation.PORT : Wallpaper.PaperOrientation.LAND;

        final String exportRoot = resetExportWallpaperFolder(false, orientation.orientation);
        final String lastExport = exportRoot + ".zip";
        File file = new File(lastExport);
        if (file.exists()) {
            IntentUtil.sendBroadCastDownloaded(context, lastExport, DUMB_EXPORT_ID, 0, "v0");
        } else {
            Log.i(TAG, "checkAndImportWallpapers, not existing export file " + lastExport);
        }
    }

    private static final String DUMB_EXPORT_ID = "dumb_import_export_id";
    public static DownloadInfo getExportInfoByProductId(Context context, String productId, boolean isPort) {
        if (DUMB_EXPORT_ID.equalsIgnoreCase(productId)) {
            Wallpaper.PaperOrientation orientation = new Wallpaper.PaperOrientation();
            orientation.orientation = isPort ? Wallpaper.PaperOrientation.PORT : Wallpaper.PaperOrientation.LAND;

            final String exportRoot = resetExportWallpaperFolder(false, orientation.orientation);
            final String lastExport = exportRoot + ".zip";
            DownloadInfo info = new DownloadInfo();
            info.product_id = productId;
            info.download_status = 0;
            info.name = "Last export";
            info.version_name = "v0";
            info.version_code = 0;
            info.local_path = lastExport;
            info.type = Product.ProductType.WALL_PAPER;
            info.json_str = "";
            info.supported_mod = isPort ? Product.SupportedMod.PORTRAIT : Product.SupportedMod.LANDSCAPE;
            if (info.local_path.contains("file://")){
                info.local_path = info.local_path.replace("file://", "");
            }
            return info;
        }
        return null;
    }

    public static ArrayList<RawPaperItem> parseRawPaperItemList(String parentPath) {
        if (isFileExisting(parentPath)) {
            final String manifestPath = parentPath + File.separator + MANIFEST_FILE_NAME;
            if (isFileExisting(manifestPath)) {
                File file = new File(manifestPath);
                Log.d(TAG, WallpaperXmlParser.test(file));
                Wallpaper.Builder builder = Wallpaper.Builder.createPaperUnitFromFile(file);
                if (null != builder) {
                    return builder.paperUnits;
                }
//                for (Object item : paperUnits) {
//                    if (item instanceof RawPaperItem) {
//                        RawPaperItem paperItem = (RawPaperItem)item;
//                        serializer.startTag(null, TAG_PAPER_ITEM);
//                        serializeXmlTag(serializer, TAG_PAPER_NAME, paperItem.mKeyName);
//                        serializeXmlTag(serializer, TAG_PAPER_TYPE, paperItem.mType);
//                        serializeXmlTag(serializer, TAG_PAPER_PATH_DEFAULT, paperItem.mDefaultValue);
//                        serializeXmlTag(serializer, TAG_PAPER_PATH_OVERLAY, paperItem.mOverlayValue);
//                        serializeXmlTag(serializer, TAG_PAPER_PATH_DECODED, paperItem.mDecodedValue);
//                        serializer.endTag(null, TAG_PAPER_ITEM);
//                    } else {
//                        Log.i(TAG, "serializeXmlPaperItemTag, unexpected item type " + item.getClass().getName());
//                    }
//                }
            }
        }
        return null;
    }
}
