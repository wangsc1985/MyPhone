package com.wang17.myphone.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
//import android.support.v7.app.NotificationCompat;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.wang17.myphone.R;
import com.wang17.myphone.util._Utils;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.wang17.myphone.fragment.BuddhaFragment.NOTIFICATION_FLAG;
import static com.wang17.myphone.service.NianfoMusicService.notification_action_pause;
import static com.wang17.myphone.service.NianfoMusicService.notification_action_play;

public class TallyNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            String action = intent.getAction();

            if (notification_action_play.equals(action)) {
                // 启动服务
//                Intent i = new Intent(context, NianfoMusicService.class);
//                context.startService(i);
            } else if (notification_action_pause.equals(action)) {
//                context.stopService(new Intent(context, NianfoMusicService.class));// 停止服务
            }

//            showNotification(context, action);
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 设置通知
     */
    private void showNotification(Context context, String action) {

        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_tally);

            // 正在播放音乐
            if (notification_action_play.equals(action)) {
                Intent intentPause = new Intent();
                intentPause.setAction(notification_action_pause);
                PendingIntent pIntentPause = PendingIntent.getBroadcast(context, 2,
                        intentPause, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setImageViewResource(R.id.imageView_play, R.mipmap.pause);
                builder.setSmallIcon(R.mipmap.pause); // 设置顶部图标
                remoteViews.setOnClickPendingIntent(R.id.layout_root, pIntentPause);
            }
            // 没有播放音乐
            else {
                Intent intentPlay = new Intent();
                intentPlay.setAction(notification_action_play);
                PendingIntent pIntentPlay = PendingIntent.getBroadcast(context, 6,
                        intentPlay, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setImageViewResource(R.id.imageView_play, R.mipmap.play);
                builder.setSmallIcon(R.mipmap.play); // 设置顶部图标
                remoteViews.setOnClickPendingIntent(R.id.layout_root, pIntentPlay);
            }

            Notification notify = builder.build();
            notify.contentView = remoteViews; // 设置下拉图标
//            notify.bigContentView = remoteViews; // 防止显示不完全,需要添加apisupport
            notify.flags = Notification.FLAG_ONGOING_EVENT;
            notify.icon = R.mipmap.play;

            manager.notify(NOTIFICATION_FLAG, notify);
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
}
