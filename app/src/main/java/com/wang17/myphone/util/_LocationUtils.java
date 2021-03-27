package com.wang17.myphone.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.wang17.myphone.database.DataContext;
import com.wang17.myphone.database.Setting;
import com.wang17.myphone.receiver.LocationAlarmReceiver;
import com.wang17.myphone.service.LocationService;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by Administrator on 2017/7/11.
 */

public class _LocationUtils {


    /**
     * 利用闹钟监控功能，开启定位服务。
     * 定位服务，精确闹钟。
     */
    public static void startLocationWithClock(Context context) {
        Intent alarmIntent = new Intent(context, LocationAlarmReceiver.class);
        PendingIntent alarmPi = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 19) {
            alarm.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10 * 60000, alarmPi);
        } else {
            alarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10 * 60000, alarmPi);
        }

        DataContext dataContext = new DataContext(context);
        dataContext.editSetting(Setting.KEYS.map_location_gear, 1);
        context.startService(new Intent(context, LocationService.class));
    }


    public static void stopLocation(Context context) {
        Intent alarmIntent = new Intent(context, LocationAlarmReceiver.class);
        PendingIntent alarmPi = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarm.cancel(alarmPi);

        context.stopService(new Intent(context, LocationService.class));
    }


    /**
     *
     */
    public static BroadcastReceiver alarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DataContext dataContext = new DataContext(context);
            dataContext.addRunLog("定位闹钟", intent.getAction());
            if (intent.getAction().equals("LOCATION")) {
                if (!_Utils.isServiceRunning(context, LocationService.class.getCanonicalName())) {
                    dataContext.addRunLog("定位闹钟 - 启动定位服务", "");
                    context.startService(new Intent(context, LocationService.class));
                }
            }
        }
    };


    /**
     * 利用闹钟监控功能，开启定位服务。
     * 定位服务，非精确闹钟。
     */
    public static void startLocationWithClock1(Context context) {

        Intent alarmIntent = new Intent();
        alarmIntent.setAction("LOCATION");

        PendingIntent alarmPi = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) context.getSystemService(ALARM_SERVICE);
//        alarm.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, System.currentTimeMillis(), alarmPi);
        alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 10 * 60000, 10 * 60000, alarmPi);

        IntentFilter filter = new IntentFilter();
        filter.addAction("LOCATION");
        context.registerReceiver(alarmReceiver, filter);

        DataContext dataContext = new DataContext(context);
        dataContext.editSetting(Setting.KEYS.map_location_gear, 1);
        context.startService(new Intent(context, LocationService.class));
    }


}
