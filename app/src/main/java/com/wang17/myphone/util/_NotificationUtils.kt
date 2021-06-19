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
import com.wang17.myphone.model.ChannelName
import com.wang17.myphone.util._Utils.saveException

object _NotificationUtils {
    fun getForegroundNotification(context: Context, channelName: ChannelName, notificationId: Int, layoutId: Int, setNotificationViews: SetNotificationViews):Notification? {
        var notification:Notification?=null
        try {
//            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val remoteViews = RemoteViews(context.packageName, layoutId)
            setNotificationViews.setNotificationViews(remoteViews)
            if (Build.VERSION.SDK_INT >= 26) {
//                val channel = NotificationChannel(channelName.toString(), channelName.toString(), NotificationManager.IMPORTANCE_HIGH)
//                channel.enableLights(true)
//                channel.enableVibration(true)
//                notificationManager.createNotificationChannel(channel)
                notification = Notification.Builder(context, channelName.toString()).setSmallIcon(R.mipmap.ic_launcher) //通知的构建过程基本与默认相同
                    //                .setTicker("hello world")
                    //                .setWhen(System.currentTimeMillis())
                    .setAutoCancel(false)
                    .setWhen(System.currentTimeMillis()) //                 Notification.FLAG_ONGOING_EVENT;
                    .setContent(remoteViews) //在这里设置自定义通知的内容
                    .build()
                notification.flags = Notification.FLAG_ONGOING_EVENT
//                notificationManager.notify(notificationId, notification)
            } else {
                notification = NotificationCompat.Builder(context).setSmallIcon(R.mipmap.ic_launcher) //通知的构建过程基本与默认相同
                    //                .setTicker("hello world")
                    //                .setWhen(System.currentTimeMillis())
                    .setAutoCancel(false)
                    .setWhen(System.currentTimeMillis()) //                 Notification.FLAG_ONGOING_EVENT;
                    .setContent(remoteViews) //在这里设置自定义通知的内容
                    .build()
                notification.flags = Notification.FLAG_ONGOING_EVENT
//                notificationManager.notify(notificationId, notification)
            }
        } catch (e: NotFoundException) {
            saveException(context, e)
        }
        return notification
    }
    fun sendNotification(context: Context, channelName: ChannelName, notificationId: Int, layoutId: Int, setNotificationViews: SetNotificationViews):Notification? {
        var notification:Notification?=null
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val remoteViews = RemoteViews(context.packageName, layoutId)
            setNotificationViews.setNotificationViews(remoteViews)
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(channelName.toString(), channelName.toString(), NotificationManager.IMPORTANCE_HIGH)
                channel.enableLights(true)
                channel.enableVibration(true)
                notificationManager.createNotificationChannel(channel)
                notification = Notification.Builder(context, channelName.toString()).setSmallIcon(R.mipmap.ic_launcher) //通知的构建过程基本与默认相同
                        //                .setTicker("hello world")
                        //                .setWhen(System.currentTimeMillis())
                        .setAutoCancel(false)
                        .setWhen(System.currentTimeMillis()) //                 Notification.FLAG_ONGOING_EVENT;
                        .setContent(remoteViews) //在这里设置自定义通知的内容
                        .build()
                notification.flags = Notification.FLAG_ONGOING_EVENT
                notificationManager.notify(notificationId, notification)
            } else {
                notification = NotificationCompat.Builder(context).setSmallIcon(R.mipmap.ic_launcher) //通知的构建过程基本与默认相同
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
        return notification
    }

//    fun sendNotification(context: Context, notificationId: Int, layoutId: Int, channel: MyChannel, setNotificationViews: SetNotificationViews) {
//        sendNotification(context, notificationId, channel, layoutId, setNotificationViews)
//    }

    fun alertNotificationTop(context: Context, channelName: ChannelName, msg:String):Notification? {
        var notification:Notification?=null
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val remoteViews = RemoteViews(context.packageName, R.layout.notification_top_alert)
            remoteViews.setTextViewText(R.id.tv_msg,msg)
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(channelName.toString(), channelName.toString(), NotificationManager.IMPORTANCE_HIGH)
                channel.enableLights(true)
                channel.enableVibration(true)
                notificationManager.createNotificationChannel(channel)
                notification = Notification.Builder(context, channelName.toString()).setSmallIcon(R.mipmap.ic_launcher) //通知的构建过程基本与默认相同
//                                        .setTicker(msg+"123")
                        .setAutoCancel(false)
                        .setWhen(System.currentTimeMillis())
                        .setContent(remoteViews)
                        .setOngoing(false)
                        .build()
                notification.flags = Notification.FLAG_AUTO_CANCEL
                notificationManager.notify(0, notification)
            } else {
                notification = NotificationCompat.Builder(context).setSmallIcon(R.mipmap.ic_launcher) //通知的构建过程基本与默认相同
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
        return notification
    }

    fun closeNotification(context: Context, notificationId: Int) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
    }
}