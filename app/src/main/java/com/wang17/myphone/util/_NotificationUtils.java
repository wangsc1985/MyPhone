package com.wang17.myphone.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.wang17.myphone.R;
import com.wang17.myphone.callback.SetNotificationViews;


public class _NotificationUtils {

    public static void sendNotification(int notificationId,String channelId,String channelName, Context context, int layoutId, SetNotificationViews setNotificationViews) {
        try {
            NotificationManager notificationManager  = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), layoutId);

            setNotificationViews.setNotificationViews(remoteViews);

            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
                channel.enableLights(true);
                channel.enableVibration(true);

                notificationManager.createNotificationChannel(channel);
                Notification notification = new Notification.Builder(context, channelId).setSmallIcon(R.mipmap.ic_launcher)//通知的构建过程基本与默认相同
                        //                .setTicker("hello world")
                        //                .setWhen(System.currentTimeMillis())
                        .setAutoCancel(false)
                        //                 Notification.FLAG_ONGOING_EVENT;
                        .setContent(remoteViews)//在这里设置自定义通知的内容
                        .build();
                notification.flags = Notification.FLAG_ONGOING_EVENT;
                notificationManager.notify(notificationId, notification);
            } else {
                Notification notification = new NotificationCompat.Builder(context).setSmallIcon(R.mipmap.ic_launcher)//通知的构建过程基本与默认相同
                        //                .setTicker("hello world")
                        //                .setWhen(System.currentTimeMillis())
                        .setAutoCancel(false)
                        //                 Notification.FLAG_ONGOING_EVENT;
                        .setContent(remoteViews)//在这里设置自定义通知的内容
                        .build();
                notification.flags = Notification.FLAG_ONGOING_EVENT;
                notificationManager.notify(notificationId, notification);
            }
        } catch (Resources.NotFoundException e) {
            _Utils.saveException(context,e);
        }
    }

    public static void sendNotification(int notificationId, Context context, int layoutId, SetNotificationViews setNotificationViews) {
        sendNotification(notificationId,"channel_myphone","我的手机" ,context, layoutId, setNotificationViews);
    }

    public static void closeNotification(Context context,int notificationId){
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
    }
}
