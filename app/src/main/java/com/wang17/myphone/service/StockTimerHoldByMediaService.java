package com.wang17.myphone.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

import com.wang17.myphone.MainActivity;
import com.wang17.myphone.R;
import com.wang17.myphone.model.Commodity;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.model.database.Setting;
import com.wang17.myphone.model.database.Position;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._LogUtils;
import com.wang17.myphone.util._OkHttpUtil;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.util._Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
@Deprecated
public class StockTimerHoldByMediaService extends Service {

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

    private static final int ERROR_CODE = 807;
    private static final int ERROR_CODE_1 = 628;
    private static final int ERROR_CODE_2 = 222;
    private static final int ERROR_CODE_3 = 142;
    private static final int ERROR_CODE_4 = 667;
    private static final int ERROR_CODE_5 = 589;
    private static final int ERROR_CODE_6 = 39;

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
            positions = mDataContext.getPositions();

            preTime = System.currentTimeMillis();

            if (positions.size() > 0)
                play();

            startTimer();
        } catch (Exception e) {
            _Utils.printException(getApplicationContext(),e);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private static PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("wangsc", "stock timer music service on create...");
        wakeLock = _Utils.acquireWakeLock(getApplicationContext(), PARTIAL_WAKE_LOCK);
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
        _Utils.releaseWakeLock(getApplicationContext(), wakeLock);
        stopTimer();

        stop();
        // 解除监控
        if (mainReciver != null)
            unregisterReceiver(mainReciver);


        ((NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
        super.onDestroy();
    }


    Timer timer;

    private void startTimer() {
        if (timer != null) {
            return;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (++loopCount >= 10) {
                        long span = (System.currentTimeMillis() - preTime) / 1000;

                        /**
                         *
                         */
                        File logFile = new File(_Session.ROOT_DIR, "stock.log");
                        BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
                        String string = new DecimalFormat("#,000").format(System.currentTimeMillis() - preTime) + "\t" + new DecimalFormat("0.00%").format(preAverageTotalProfitS) + "\t" + new DateTime().toLongDateTimeString();
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

                    fillStockInfoList();


                } catch (IOException e) {
                    _Utils.printException(getApplicationContext(),e);
                }
            }
        }, 0, 10000);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void play() {
        try {
            mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.second_10);
            mPlayer.setVolume(0.01f, 0.01f);
            mPlayer.setLooping(true);
            mPlayer.start();
        } catch (Exception e) {
            _Utils.printException(getApplicationContext(),e);
        }
    }

    private void stop() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }


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

    private int sizeS = 0, sizeF = 0;

    private void log(String msg) {
        Log.e("wangsc", msg);
    }

    private void fillStockInfoList() {

        //http://hq.sinajs.cn/list=sh601555
        //var hq_str_sh601555="东吴证券,11.290,11.380,11.160,11.350,11.050,11.160,11.170,61431561,687740501.000,3500,11.160,144700,11.150,98500,11.140,78500,11.130,99200,11.120,143700,11.170,99700,11.180,28700,11.190,41500,11.200,41500,11.210,2019-04-17,15:00:00,00";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    double totalProfitS = 0;
                    double totalAmountS = 0;
                    double totalProfitF = 0;
                    sizeF = sizeS = 0;

                    if (positions.size() <= 0)
                        return;
                    for (Position position : positions) {
                        if (position.getType() == 0) {
                            sizeS++;
                            String urlS = "https://hq.sinajs.cn/list=" + position.getExchange() + position.getCode();

                            OkHttpClient clientS = _OkHttpUtil.client;
                            Request requestS = new Request.Builder().url(urlS).build();
                            Response responseS = clientS.newCall(requestS).execute();
                            if (responseS.isSuccessful()) {
                                try {
                                    String sss = responseS.body().string();
                                    String[] result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",");

                                    double price = Double.parseDouble(result[3]);
                                    final double profit = (price - position.getCost()) / position.getCost();
                                    mTimeS = result[31];
                                    totalProfitS += profit * position.getAmount() * position.getCost() * 100;
                                    totalAmountS += position.getAmount() * position.getCost() * 100;
                                } catch (Exception e) {
                                    Log.e("wangsc", "数据错误...");
                                    _LogUtils.log2file("数据错误...");
                                    return;
                                }
                            } else {
                                Log.e("wangsc", "数据错误...");
                                _LogUtils.log2file("数据错误...");
                            }
                        } else {
                            sizeF++;
                            String url = "https://hq.sinajs.cn/list=" + position.getCode();
                            Log.e("wangsc", "url：" + url);

                            OkHttpClient client = _OkHttpUtil.client;
                            Request request = new Request.Builder().url(url).build();
                            Response response = client.newCall(request).execute();
                            if (response.isSuccessful()) {
                                try {
                                    String sss = response.body().string();
                                    Log.e("wangsc", "数据：" + sss);
                                    String[] result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",");

                                    double price = Double.parseDouble(result[8]);
                                    double profit = position.getType()*( price - position.getCost());

                                    mTimeF = result[1];
                                    mTimeF = mTimeF.substring(0, 2) + ":" + mTimeF.substring(2, 4) + ":" + mTimeF.substring(4, 6);
                                    Commodity commodity = findCommodity(position.getCode());
                                    totalProfitF += profit * position.getAmount() * commodity.unit;
                                } catch (Exception e) {
                                    Log.e("wangsc", "数据错误...");
                                    _LogUtils.log2file("数据错误...");
                                    return;
                                }
                            } else {
                                Log.e("wangsc", "获取数据失败...");
                                _LogUtils.log2file("获取数据失败...");
                            }
                        }

                    }

                    //region 上证指数
                    String urlS = "https://hq.sinajs.cn/list=sh000001";

                    double szPrice = 0;
                    double szIncrease = 0;
                    OkHttpClient clientS = _OkHttpUtil.client;
                    Request requestS = new Request.Builder().url(urlS).build();
                    Response responseS = clientS.newCall(requestS).execute();
                    if (responseS.isSuccessful()) {
                        try {
                            String sss = responseS.body().string();
                            String[] result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",");

                            szPrice = Double.parseDouble(result[3]);
                            double open = Double.parseDouble(result[2]);
                            szIncrease = (szPrice - open) / open;
                            Log.e("wangsc", "open: " + open + " , price: " + szPrice + " , increase: " + szIncrease);
                            mTimeS = result[31];
                        } catch (Exception e) {
                            Log.e("wangsc", "数据错误...");
                            _LogUtils.log2file("数据错误...");
                            return;
                        }
                    } else {
                        Log.e("wangsc", "数据错误...");
                        _LogUtils.log2file("数据错误...");
                        return;
                    }
                    //endregion

                    String speakMsg = "";
                    //region 股票平均盈利
                    double averageTotalProfitS = 0;
                    String msgS = "";
                    if (sizeS > 0) {
                        averageTotalProfitS = totalProfitS / totalAmountS;
                        msgS = new DecimalFormat("0.00").format(averageTotalProfitS * 100);
                        Log.e("wangsc", "averageTotalProfitS: " + msgS);

                        if (Math.abs(averageTotalProfitS - preAverageTotalProfitS) * 100 > 1.0 / sizeS) {
                            preAverageTotalProfitS = averageTotalProfitS;
                            if (averageTotalProfitS > 0) {
                                msgS = "A" + msgS;
                            }
                            speakMsg += msgS;
                        }
                    }
                    //endregion


                    //region 期货平均盈利
                    double averageTotalProfitF = 0;
                    String msgF = "";
                    if (sizeF > 0) {
                        averageTotalProfitF = totalProfitF / sizeF;
                        msgF = new DecimalFormat("0").format(averageTotalProfitF);
                        Log.e("wangsc", "averageTotalProfit: " + msgF);
                        if (Math.abs(averageTotalProfitF - preAverageTotalProfitF) > 20 / sizeF) {
                            preAverageTotalProfitF = averageTotalProfitF;
                            if (averageTotalProfitF > 0) {
                                msgF = "C" + msgF;
                            }
                            speakMsg += msgF;
                        }
                    }
                    //endregion

                    if (!speakMsg.isEmpty() && mDataContext.getSetting(Setting.KEYS.is_stock_broadcast, true).getBoolean())
                        _Utils.speaker(getApplicationContext(), speakMsg);

                    sendNotification(new DecimalFormat("0.00").format(szPrice),
                            new DecimalFormat("0.00").format(szIncrease * 100),
                            mTimeS, new DecimalFormat("0.00").format(averageTotalProfitS * 100),
                            mTimeF, new DecimalFormat("#0,000").format(averageTotalProfitF));
                } catch (Exception e) {
                    _Utils.printException(getApplicationContext(),e);
                }
            }
        }).start();
    }


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


    private void sendNotification(String szPrice, String szIncrease, String sTime, String sIncrease, String fTime, String fIncrease) {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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

            if (sizeF == 0) {
                remoteViews.setViewVisibility(R.id.layout_f, GONE);
            } else {
                remoteViews.setViewVisibility(R.id.layout_f, VISIBLE);
            }
            if (sizeS == 0) {
                remoteViews.setViewVisibility(R.id.layout_s, GONE);
            } else {
                remoteViews.setViewVisibility(R.id.layout_s, VISIBLE);
            }


            if (Build.VERSION.SDK_INT >= 26) {
                createNotificationChannel();
                Notification notification = new Notification.Builder(this, "channel_id").setSmallIcon(R.mipmap.ic_launcher)//通知的构建过程基本与默认相同
    //                .setTicker("hello world")
    //                .setWhen(System.currentTimeMillis())
                        .setAutoCancel(false)
    //                 Notification.FLAG_ONGOING_EVENT;
                        .setContent(remoteViews)//在这里设置自定义通知的内容
                        .build();
                notification.flags = Notification.FLAG_ONGOING_EVENT;
                getManager().notify(NOTIFICATION_ID, notification);
            } else {
                Notification notification = new NotificationCompat.Builder(this).setSmallIcon(R.mipmap.ic_launcher)//通知的构建过程基本与默认相同
    //                .setTicker("hello world")
    //                .setWhen(System.currentTimeMillis())
                        .setAutoCancel(false)
    //                 Notification.FLAG_ONGOING_EVENT;
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

    public static final int NOTIFICATION_ID = 100;

//    private void sendNotification(String contentTitle, String contentText) {
//        try {
//            //获取NotificationManager实例
//            NotificationManager notifyManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
//            //实例化NotificationCompat.Builde并设置相关属性
//            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
//                    //设置小图标
//                    .setSmallIcon(R.mipmap.wan)
//                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.wan))
//                    //设置通知标题
//                    .setContentTitle(contentTitle)
//                    //设置通知内容
//                    .setContentText(contentText)
//                    .setPriority(NotificationCompat.PRIORITY_MAX)
//                    .setOngoing(true);
//
//
////        android.support.v4.app.NotificationCompat.BigPictureStyle style = new android.support.v4.app.NotificationCompat.BigPictureStyle();
////        style.setBigContentTitle("南无阿弥陀佛");
////        style.setSummaryText(contentText);
////        style.bigPicture(BitmapFactory.decodeResource(getResources(),R.drawable.amtf));
////        builder.setStyle(style);
//
//            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//            PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 1, intent, 0);
//            //设置点击大图后跳转
//            builder.setContentIntent(pIntent);
//
//            //设置通知时间，默认为系统发出通知的时间，通常不用设置
//            //.setWhen(System.currentTimeMillis());
//            //通过builder.build()方法生成Notification对象,并发送通知,id=1
//            notifyManager.notify(NOTIFICATION_ID, builder.build());
//        } catch (Exception e) {
//            _Utils.printException(getApplicationContext(), e);
//        }
//    }

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
                            _LogUtils.log2file("{" + level + "%}{" + span / 60000 + "分" + span % 60000 / 1000 + "秒}");
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
