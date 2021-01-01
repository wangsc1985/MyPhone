package com.wang17.myphone.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.util.AMapUtil;

import java.util.Calendar;

import static android.content.Context.ALARM_SERVICE;
import static com.wang17.myphone.MainActivity.LOCATION_RECORD;

public class LocationRecordAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent i) {
        DateTime now = new DateTime();
        DateTime targetTime = now.addHours(1);
        targetTime.set(Calendar.MINUTE,0);
        targetTime.set(Calendar.SECOND,0);
        targetTime.set(Calendar.MILLISECOND,0);

        AMapUtil.getCurrentLocation(context,"闹钟定位");

        Intent intent = new Intent(context,LocationRecordAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context,LOCATION_RECORD,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am =(AlarmManager)context.getSystemService(ALARM_SERVICE);
        if(Build.VERSION.SDK_INT>=19){
            am.setExact(AlarmManager.RTC_WAKEUP,targetTime.getTimeInMillis(),pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, targetTime.getTimeInMillis(), pi);
        }
    }
}
