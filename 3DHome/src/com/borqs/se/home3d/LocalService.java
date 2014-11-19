package com.borqs.se.home3d;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class LocalService extends Service {

    @Override
    public void onCreate() {
        if(HomeUtils.DEBUG) {
            Log.d(HomeUtils.TAG, "###########################start localservice");
        }
        Notification notification = new Notification(0, "3DHome", System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, "3DHome", "Welcome to 3DHome !", pendingIntent);
       
        startForeground(12314, notification);
    }

    @Override
    public void onDestroy() {
        if(HomeUtils.DEBUG) {
            Log.d(HomeUtils.TAG, "###########################stop localservice");
        }
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

}
