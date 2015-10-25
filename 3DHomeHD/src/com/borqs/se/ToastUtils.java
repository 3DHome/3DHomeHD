package com.borqs.se;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.widget.Toast;
import com.borqs.freehdhome.R;
import com.borqs.se.engine.SESceneManager;
import org.xmlpull.v1.XmlPullParser;

/**
 * Created with IntelliJ IDEA.
 * User: b608
 * Date: 13-4-19
 * Time: 下午2:39
 * To change this template use File | Settings | File Templates.
 */
public class ToastUtils {
    // System applications cannot be installed. For now, show a
    // toast explaining that.
    public static void showUninstallForbidden() {
        final int messageId = R.string.uninstall_system_app_text;
        SESceneManager.getInstance().runInUIThread(new Runnable() {
            public void run() {
                Toast.makeText(SESceneManager.getInstance().getContext(), messageId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showActivityNotFound() {
        SESceneManager.getInstance().runInUIThread(new Runnable() {
            public void run() {
                Toast.makeText(SESceneManager.getInstance().getContext(), R.string.activity_not_found,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showNoWallSpace() {
        SESceneManager.getInstance().runInUIThread(new Runnable() {
            public void run() {
                Toast.makeText(SESceneManager.getInstance().getContext(), R.string.no_room, Toast.LENGTH_SHORT).show();
            }
        });
    }
    public static void showNoDeskSpace() {
        SESceneManager.getInstance().runInUIThread(new Runnable() {
            public void run() {
                Toast.makeText(SESceneManager.getInstance().getContext(), R.string.no_room_desk, Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    public static String[] getMonthStringArray(Context context) {
        return context.getResources().getStringArray(R.array.weeks);
    }

    public static String[] getWeekStringArray(Context context) {
        return context.getResources().getStringArray(R.array.weeks);
    }

    public static Drawable getDefaultApplicationIcon(Context context) {
        return context.getResources().getDrawable(R.drawable.ic_launcher_application);
    }

    public static Bitmap decodeDefaultApplicationIcon(int w, int h) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.outWidth = w;
        opts.outHeight = h;
        return BitmapFactory.decodeResource(SESceneManager.getInstance().getContext().getResources()
                , R.drawable.ic_launcher_application, opts);
    }
    
    public static Bitmap decodeLockScreenHdIcon(int w, int h, int res) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.outWidth = w;
        opts.outHeight = h;
        return BitmapFactory.decodeResource(SESceneManager.getInstance().getContext().getResources()
                , res, opts);
    }

    public static XmlPullParser getObjectConfigParser(Context context) {
        return context.getResources().getXml(R.xml.objects_config);
    }

    public static void showDeletePresetObject() {
        final int messageId = R.string.delete_preset_object_text;
        SESceneManager.getInstance().runInUIThread(new Runnable() {
            public void run() {
                Toast.makeText(SESceneManager.getInstance().getContext(), messageId, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
