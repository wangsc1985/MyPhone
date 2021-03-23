package com.wang17.myphone.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.wang17.myphone.R;
import com.wang17.myphone.model.Commodity;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.database.Setting;
import com.wang17.myphone.database.Position;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.util._Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

/**
 * 用MediaPlayer的循环播放10秒音乐，每次结束事件，来模拟Timer的功能。需要获取WakeLock电源锁， 并且发送Notification。
 *
 *      测试结果：MediaPlayer运行正常，没有被杀死，成功的模拟了TImer功能。
 */
@Deprecated
public class MediaLoopTimerService extends Service {

    private DataContext mDataContext;
    private MediaPlayer mPlayer;
    private List<Position> positions;
    private boolean isSoundLoaded;
    private SoundPool mSoundPool;
    private int soundId;

    private MainReciver mainReciver;

    private int loadStockCount = 1, loopCount = 1;
    private double preAverageTotalProfitS = 0;
    private double preAverageTotalProfitF = 0;

    private long preTime = 0;
    String mTimeS, mTimeF;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            mDataContext = new DataContext(this);
            // TODO: 2019/4/26 test
            //region test

            /**
             * 初始化声音
             */
            mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 5);
            soundId = mSoundPool.load(getApplicationContext(), R.raw.clock, 1);
            mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    isSoundLoaded = true;
                }
            });

            /**
             *
             */
            preAverageTotalProfitS = 0;

            preTime = System.currentTimeMillis();

            play();
        } catch (Exception e) {
            _Utils.printException(getApplicationContext(),e);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private static final int ERROR_CODE_5 = 589;
    private static PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("wangsc", "stock timer music service on create...");
//        wakeLock = _Utils.acquireWakeLock(getApplicationContext(), PARTIAL_WAKE_LOCK);
        Log.e("wangsc", "wakeLock:" + wakeLock);
        /**
         * 监听
         */
        mainReciver = new MainReciver();
        IntentFilter filter = new IntentFilter();
        // 监控电量变化
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mainReciver, filter);

//        showNotification();
    }

    @Override
    public void onDestroy() {
//        _Utils.releaseWakeLock(getApplicationContext(), wakeLock);

        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
        // 解除监控
        if (mainReciver != null)
            unregisterReceiver(mainReciver);


        ((NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
        super.onDestroy();
    }

    public void play() {
        try {
            mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.second_10);
            mPlayer.setVolume(0.0f, 0.0f);
            mPlayer.setLooping(false);
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    try {
                        Log.e("wangsc", "wakeLock:" + wakeLock);
                        if (++loopCount > 10) {
                            long span = (System.currentTimeMillis() - preTime) / 1000;
                            mDataContext.addLog("timer","span",span+"秒");
//                            Log.e("wangsc", "span:" + span);

                            /**
                             *
                             */
                            File logFile = new File(_Session.ROOT_DIR, "stock.log");
                            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
                            String string = new DecimalFormat("#,##0").format(System.currentTimeMillis() - preTime) + "\t" + new DecimalFormat("0.00%").format(preAverageTotalProfitS) + "\t" + new DateTime().toLongDateTimeString();
                            writer.write(string);
                            writer.newLine();
                            writer.flush();
                            writer.close();

                            preTime = System.currentTimeMillis();
                            if (span > 110) {
                                _Utils.speaker(getApplicationContext(), "计时警告，此次间隔：" + span + "秒");
                            }
                            loopCount = 1;
                        }

                        if (++loadStockCount >= 10 && mDataContext.getSetting(Setting.KEYS.is_stock_load_noke, false).getBoolean()) {
                            playknock();
                            loadStockCount = 1;
                        }

                        sendNotification("LoopCount", loopCount + "", "LoadStockCound", loadStockCount + "", "Time", new DateTime().toTimeString());

                        mPlayer.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            mPlayer.start();
        } catch (Exception e) {
            _Utils.printException(getApplicationContext(),e);
        }
    }

    private static final int ERROR_CODE_4 = 667;

    private void playknock() {
        if (!isSoundLoaded) {
            mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    isSoundLoaded = true;
                    soundPool.play(soundId, 1f, 1f, 0, 0, 1);
                }
            });
        } else {
            mSoundPool.play(soundId, 1f, 1f, 0, 0, 1);
        }
    }

    private static final int ERROR_CODE_3 = 142;

    public static Commodity findCommodity(String code) {
        for (Commodity commodity : _Session.commoditys) {
            if (commodity.item.toLowerCase().equals(parseItem(code).toLowerCase())) {
                return commodity;
            }
        }
        return null;
    }

    private static String parseItem(String code) {
        StringBuffer item = new StringBuffer();
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (!Character.isDigit(c)) {
                item.append(c);
            } else {
                break;
            }
        }
        return item.toString();
    }

    private long preBatteryTime;

    //region 通知
    private NotificationManager notificationManager;

    private NotificationManager getManager() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createNotificationChannel() {
        try {
            NotificationChannel channel = new NotificationChannel("channel_id", "我的手机", NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.enableVibration(true);

            getManager().createNotificationChannel(channel);
        } catch (Exception e) {
            _Utils.printException(getApplicationContext(),e);
        }
    }

    private static final int ERROR_CODE_2 = 222;

    private void sendNotification(String szPrice, String szIncrease, String sTime, String sIncrease, String fTime, String fIncrease) {
        try {
            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification2);

            remoteViews.setTextViewText(R.id.textView_price_sz, szPrice);
            remoteViews.setTextViewText(R.id.textView_increase_sz, szIncrease);
            remoteViews.setTextColor(R.id.textView_increase_sz, szIncrease.contains("-") ? getResources().getColor(R.color.DARK_GREEN) : getResources().getColor(R.color.month_text_color));

            remoteViews.setTextViewText(R.id.textView_time_s, sTime);
            remoteViews.setTextViewText(R.id.textView_increase_s, sIncrease);
            remoteViews.setTextColor(R.id.textView_increase_s, sIncrease.contains("-") ? getResources().getColor(R.color.DARK_GREEN) : getResources().getColor(R.color.month_text_color));

            remoteViews.setTextViewText(R.id.textView_time_f, fTime);
            remoteViews.setTextViewText(R.id.textView_increase_f, fIncrease);
            remoteViews.setTextColor(R.id.textView_increase_f, fIncrease.contains("-") ? getResources().getColor(R.color.DARK_GREEN) : getResources().getColor(R.color.month_text_color));


            if (Build.VERSION.SDK_INT >= 26) {
                createNotificationChannel();
                Notification notification = new Notification.Builder(this, "channel_id").setSmallIcon(R.mipmap.ic_launcher)//通知的构建过程基本与默认相同
                        //                .setTicker("hello world")
                        //                .setWhen(System.currentTimeMillis())
                        .setAutoCancel(false)
                        .setContent(remoteViews)//在这里设置自定义通知的内容
                        .build();
                notification.flags = Notification.FLAG_ONGOING_EVENT;
                getManager().notify(NOTIFICATION_ID, notification);
            } else {
                Notification notification = new NotificationCompat.Builder(this).setSmallIcon(R.mipmap.ic_launcher)//通知的构建过程基本与默认相同
                        //                .setTicker("hello world")
                        //                .setWhen(System.currentTimeMillis())
                        .setAutoCancel(false)
                        .setContent(remoteViews)//在这里设置自定义通知的内容
                        .build();
                notification.flags = Notification.FLAG_ONGOING_EVENT;
                getManager().notify(NOTIFICATION_ID, notification);
            }
        } catch (Resources.NotFoundException e) {
            _Utils.printException(getApplicationContext(),e);
        }
    }
    //endregion

    private static final int ERROR_CODE_1 = 628;
    public static final int NOTIFICATION_ID = 100;
    private static final int ERROR_CODE = 807;

    public class MainReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                // TODO Auto-generated method stub
                switch (intent.getAction()) {
                    case Intent.ACTION_BATTERY_CHANGED:
                        int level = intent.getIntExtra("level", 0);
                        int dataLevel = mDataContext.getSetting(Setting.KEYS.battery, 0).getInt();
                        if (dataLevel != level) {
                            long span = System.currentTimeMillis() - (preBatteryTime == 0 ? System.currentTimeMillis() : preBatteryTime);
                            preBatteryTime = System.currentTimeMillis();
                            mDataContext.editSetting(Setting.KEYS.battery, level);
                        }
                        break;
                }
            } catch (Exception e) {
                _Utils.printException(getApplicationContext(),e);
            }
        }
    }
}
