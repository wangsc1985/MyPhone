package com.wang17.myphone.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Resources.NotFoundException
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.widget.RemoteViews
import com.wang17.myphone.R
import com.wang17.myphone.callback.SetNotificationViews
import com.wang17.myphone.util._Utils.saveException

object _NotificationUtils {
    fun sendNotification(notificationId: Int, channelId: String?, channelName: String?, context: Context, layoutId: Int, setNotificationViews: SetNotificationViews) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val remoteViews = RemoteViews(context.packageName, layoutId)
            setNotificationViews.setNotificationViews(remoteViews)
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                channel.enableLights(true)
                channel.enableVibration(true)
                notificationManager.createNotificationChannel(channel)
                val notification = Notification.Builder(context, channelId).setSmallIcon(R.mipmap.ic_launcher) //通知的构建过程基本与默认相同
                        //                .setTicker("hello world")
                        //                .setWhen(System.currentTimeMillis())
                        .setAutoCancel(false)
                        .setWhen(System.currentTimeMillis()) //                 Notification.FLAG_ONGOING_EVENT;
                        .setContent(remoteViews) //在这里设置自定义通知的内容
                        .build()
                notification.flags = Notification.FLAG_ONGOING_EVENT
                notificationManager.notify(notificationId, notification)
            } else {
                val notification = NotificationCompat.Builder(context).setSmallIcon(R.mipmap.ic_launcher) //通知的构建过程基本与默认相同
                        //                .setTicker("hello world")
                        //                .setWhen(System.currentTimeMillis())
                        .setAutoCancel(false)
                        .setWhen(System.currentTimeMillis()) //                 Notification.FLAG_ONGOING_EVENT;
                        .setContent(remoteViews) //在这里设置自定义通知的内容
                        .build()
                notification.flags = Notification.FLAG_ONGOING_EVENT
                notificationManager.notify(notificationId, notification)
            }
        } catch (e: NotFoundException) {
            saveException(context, e)
        }
    }

    fun sendNotification(notificationId: Int, context: Context, layoutId: Int, channelName: String, setNotificationViews: SetNotificationViews) {
        sendNotification(notificationId, channelName, channelName, context, layoutId, setNotificationViews)
    }

    fun alertNotificationTop(context: Context, msg:String) {

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val remoteViews = RemoteViews(context.packageName, R.layout.notification_top_alert)
            remoteViews.setTextViewText(R.id.tv_msg,msg)
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(CHANNEL_TOP_ALERT, CHANNEL_TOP_ALERT, NotificationManager.IMPORTANCE_HIGH)
                channel.enableLights(true)
                channel.enableVibration(true)
                notificationManager.createNotificationChannel(channel)
                val notification = Notification.Builder(context, CHANNEL_TOP_ALERT).setSmallIcon(R.mipmap.ic_launcher) //通知的构建过程基本与默认相同
//                                        .setTicker(msg+"123")
                        .setAutoCancel(false)
                        .setWhen(System.currentTimeMillis())
                        .setContent(remoteViews)
                        .setOngoing(false)
                        .build()
                notification.flags = Notification.FLAG_AUTO_CANCEL
                notificationManager.notify(0, notification)
            } else {
                val notification = NotificationCompat.Builder(context).setSmallIcon(R.mipmap.ic_launcher) //通知的构建过程基本与默认相同
//                        .setTicker(msg+"123")
                        .setAutoCancel(false)
                        .setWhen(System.currentTimeMillis())
                        .setContent(remoteViews)
                        .setOngoing(false)
                        .build()
                notification.flags = Notification.FLAG_AUTO_CANCEL
                notificationManager.notify(0, notification)
            }
        } catch (e: NotFoundException) {
            saveException(context, e)
        }
    }

    fun closeNotification(context: Context, notificationId: Int) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
    }

    var CHANNEL_BUDDHA="老实念佛"
    var CHANNEL_STOCK="财经频道"
    var CHANNEL_TOP_ALERT="顶部弹窗"
}