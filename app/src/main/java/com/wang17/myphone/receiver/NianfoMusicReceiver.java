package com.wang17.myphone.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;

import com.wang17.myphone.R;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.model.database.Setting;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by 阿弥陀佛 on 2016/10/23.
 */

public class NianfoMusicReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        final DataContext dataContext = new DataContext(context);
        try {
            final int interval = Integer.parseInt(dataContext.getSetting(Setting.KEYS.nianfo_intervalInMillis, 60).getString());
            final Context contextFinal = context;
            SoundPool soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
            soundPool.load(context, R.raw.nianfo, 1);
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    try {
                        int playId = soundPool.play(1, 0, 0, 0, 0, 1);
                        dataContext.editSetting(Setting.KEYS.nianfo_palyId, playId);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Intent intent = new Intent(_Session.NIAN_FO_ACTION);
                                    PendingIntent pi = PendingIntent.getBroadcast(contextFinal, 0, intent, 0);
                                    AlarmManager am = (AlarmManager) contextFinal.getSystemService(ALARM_SERVICE);
                                    long endTimeMillis = System.currentTimeMillis() + interval * 1000;
                                    am.setExact(AlarmManager.RTC_WAKEUP, endTimeMillis, pi);
                                    //
                                    dataContext.editSetting(Setting.KEYS.nianfo_endInMillis, endTimeMillis);
                                    //
                                } catch (Exception e) {
                                    _Utils.printException(context,e);
                                }
                            }
                        }).start();
                    } catch (Exception e) {
                        _Utils.printException(context,e);
                    }
                }
            });
        } catch (NumberFormatException e) {
            _Utils.printException(context,e);
        }
    }
}
