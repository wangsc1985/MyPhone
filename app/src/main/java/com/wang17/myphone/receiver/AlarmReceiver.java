package com.wang17.myphone.receiver;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.wang17.myphone.R;
import com.wang17.myphone.activity.AlarmWindowActivity;
import com.wang17.myphone.activity.FuturePositionActivity;
import com.wang17.myphone.activity.StockPositionActivity;
import com.wang17.myphone.activity.FundMonitorActivity;
import com.wang17.myphone.activity.StockDarkRunActivity;
import com.wang17.myphone.event.NianfoOverEvent;
import com.wang17.myphone.service.NianfoMusicService;
import com.wang17.myphone.service.SpeakerService;
import com.wang17.myphone.util.BackupTask;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._LogUtils;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.model.database.Setting;
import com.wang17.myphone.model.database.TallyRecord;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;

import static android.content.Context.ALARM_SERVICE;
import static com.wang17.myphone.util._Session.ALERT_VOLUMN;

/**
 * Created by 阿弥陀佛 on 2016/10/23.
 */

public class AlarmReceiver extends BroadcastReceiver {

    public static final int ALARM_NIANFO_OVER = 406;
    public static final int ALARM__STOCK_SCREEN = 564;
    public static final int ALARM_NIANFO_BEFORE_OVER = 916;
    public static final int ALARM_TRADE = 134;

    private DataContext mDataContext;

    @Override
    public void onReceive(final Context context, Intent argIntent) {

        DateTime now = new DateTime();
        try {
            mDataContext = new DataContext(context);

            switch (argIntent.getAction()) {
                case _Session.ACTION_ALARM_NIANFO_OVER: {

                    _LogUtils.log2file("念佛闹钟响铃");

                    Log.e("wangsc", "ALARM_NIANFO_OVER");

                    saveSection(context);

                    EventBus.getDefault().post(new NianfoOverEvent());

                    ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);

                    /**
                     * 有对话框
                     */
                    if (Boolean.parseBoolean(mDataContext.getSetting(Setting.KEYS.tally_isShowDialog, "true").getString())) {
                        Intent intent = new Intent(context, AlarmWindowActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                    /**
                     * 无对话框
                     */
                    else {

                        Intent intent1 = new Intent(context, SpeakerService.class);
                        intent1.putExtra("msg", mDataContext.getSetting(Setting.KEYS.nianfo_over_speaker_msg,"富饶").getString());
                        context.startService(intent1);

//                        SoundPool soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
//                        soundPool.load(context, R.raw.ling3, 1);
//                        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
//                            @Override
//                            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
//                                int streamId = soundPool.play(1, ALERT_VOLUMN, ALERT_VOLUMN, 0, 0, 1);
//                                try {
//                                    float add = (1 - ALERT_VOLUMN) / 2;
//                                    float vol = ALERT_VOLUMN;
//                                    for (int i = 0; i < 2; i++) {
//                                        Thread.sleep(1000);
//
//                                        vol += add;
//                                        soundPool.setVolume(streamId, vol, vol);
//                                    }
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        });

                        /**
                         * 停止念佛音乐服务
                         */
                        if (mDataContext.getSetting(Setting.KEYS.tally_music_is_playing, false).getBoolean()) {
                            context.stopService(new Intent(context, NianfoMusicService.class));
                        }
                    }
                }
                break;

                case _Session.ACTION_ALARM_TRADE: {

                    Log.e("wangsc", "ALARM_TRADE");
                    SoundPool soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
                    soundPool.load(context, R.raw.ling3, 1);
                    soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                        @Override
                        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                            int streamId = soundPool.play(1, ALERT_VOLUMN, ALERT_VOLUMN, 0, 0, 1);
                            try {
                                float add = (1 - ALERT_VOLUMN) / 2;
                                float vol = ALERT_VOLUMN;
                                for (int i = 0; i < 1; i++) {
                                    Thread.sleep(500);
                                    vol += add;
                                    soundPool.setVolume(streamId, vol, vol);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    DateTime clock = new DateTime(now.getYear(), now.getMonth(), now.getDay(), 8, 50, 0);
                    if (now.getTimeInMillis() > clock.getTimeInMillis()) {
                        clock.set(Calendar.HOUR, 11);
                        clock.set(Calendar.MINUTE, 27);
                    }
                    if (now.getTimeInMillis() > clock.getTimeInMillis()) {
                        clock.set(Calendar.HOUR, 13);
                        clock.set(Calendar.MINUTE, 27);
                    }
                    if (now.getTimeInMillis() > clock.getTimeInMillis()) {
                        clock.set(Calendar.HOUR, 14);
                        clock.set(Calendar.MINUTE, 55);
                    }
                    if (now.getTimeInMillis() > clock.getTimeInMillis()) {
                        clock.set(Calendar.HOUR, 20);
                        clock.set(Calendar.MINUTE, 50);
                    }
                    if (now.getTimeInMillis() > clock.getTimeInMillis()) {
                        if (clock.get(Calendar.DAY_OF_WEEK) == DateTime.FRIDAY)
                            clock.addDays(3);
                        else
                            clock.addDays(1);
                        clock.set(Calendar.HOUR, 8);
                        clock.set(Calendar.MINUTE, 50);
                    }

                    Intent intent = new Intent(_Session.ACTION_ALARM_TRADE);
                    PendingIntent pi = PendingIntent.getBroadcast(context, ALARM_TRADE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);

                    if (Build.VERSION.SDK_INT >= 19) {
                        am.setExact(AlarmManager.RTC_WAKEUP, clock.getTimeInMillis(), pi);
                    } else {
                        am.set(AlarmManager.RTC_WAKEUP, clock.getTimeInMillis(), pi);


                    }
                }
                break;

                case _Session.ACTION_ALARM_STOCK_SCREEN:
                    wakeScreen(context);

                    Intent intent = new Intent(context, FundMonitorActivity.class);
                    switch (mDataContext.getSetting(Setting.KEYS.quick,1).getInt()){
                        case 1:
                            intent = new Intent(context, StockPositionActivity.class);
                            break;
                        case 2:
                            intent = new Intent(context, StockDarkRunActivity.class);
                            break;
                        case 3:
                            intent = new Intent(context, FuturePositionActivity.class);
                            break;
                        case 4:
                            intent = new Intent(context, FundMonitorActivity.class);
                            break;
                    }
                    context.startActivity(intent);
                    break;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
    public static PowerManager.WakeLock mWakeLock;

    @SuppressLint("InvalidWakeLockTag")
    public static void wakeScreen(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "bright");
        mWakeLock.acquire();
    }

    public static void saveSection(Context context) {
        DataContext mDataContext = new DataContext(context);
        if (mDataContext.getSetting(Setting.KEYS.tally_endInMillis) != null) {
            //
            final long sectionEndInMillis = Long.parseLong(mDataContext.getSetting(Setting.KEYS.tally_endInMillis).getString());
            mDataContext.deleteSetting(Setting.KEYS.tally_endInMillis);
            final long sectionStartInMillis = Long.parseLong(mDataContext.getSetting(Setting.KEYS.tally_sectionStartMillis).getString());
            mDataContext.deleteSetting(Setting.KEYS.tally_sectionStartMillis);
            mDataContext.deleteSetting(Setting.KEYS.tally_manualOverTimeInMillis);

            TallyRecord tallyRecord = new TallyRecord(new DateTime(sectionStartInMillis), (int) (sectionEndInMillis - sectionStartInMillis), mDataContext.getSetting(Setting.KEYS.tally_record_item_text, "").getString());
            mDataContext.addRecord(tallyRecord);

            new BackupTask(context).execute(BackupTask.COMMAND_BACKUP);


            mDataContext.editSetting(Setting.KEYS.tally_music_switch, false);
        }
    }
}
