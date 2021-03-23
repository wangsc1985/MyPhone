package com.wang17.myphone.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

import com.wang17.myphone.R;
import com.wang17.myphone.database.Setting;
import com.wang17.myphone.service.LocationService;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._Utils;

import static android.content.Context.ALARM_SERVICE;
import static com.wang17.myphone.util._Session.ALERT_VOLUMN;

public class LocationAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        DataContext dataContext = new DataContext(context);
        dataContext.addRunLog("定位闹钟", intent.getAction());
        if (dataContext.getSetting(Setting.KEYS.map_location_clock_alarm_is_open, true).getBoolean() == true) {
            SoundPool soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
            soundPool.load(context, R.raw.ling, 1);
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    soundPool.play(1, ALERT_VOLUMN + (1 - ALERT_VOLUMN) / 2, ALERT_VOLUMN + (1 - ALERT_VOLUMN) / 2, 0, 0, 1);
                }
            });

        }
        if (!_Utils.isServiceRunning(context, LocationService.class.getCanonicalName())) {
            dataContext.addRunLog("定位闹钟 - 重新启动定位服务", "");
            context.startService(new Intent(context, LocationService.class));
        }


        /**
         * 重复闹钟
         */
        Intent alarmIntent = new Intent(context, LocationAlarmReceiver.class);
        PendingIntent alarmPi = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 19) {
            alarm.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10 * 60000, alarmPi);
        } else {
            alarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10 * 60000, alarmPi);
        }
    }
}
