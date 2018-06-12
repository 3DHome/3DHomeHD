package com.borqs.se.home3d;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class LocalService extends Service {

    @Override
    public void onCreate() {
        if(HomeUtils.DEBUG) {
            Log.d(HomeUtils.TAG, "###########################start localservice");
        }
//        Notification notification = new Notification(0, HomeUtils.TAG, System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
//        notification.setLatestEventInfo(this, HomeUtils.TAG, "Welcome to 3DHome !", pendingIntent);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(HomeUtils.TAG)
                .setContentText("Welcome to 3DHome!")
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .build();

        try {
            startForeground(12314, notification);
        } catch (Exception e) {
            Log.e(HomeUtils.TAG, e.getMessage());
        }
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
