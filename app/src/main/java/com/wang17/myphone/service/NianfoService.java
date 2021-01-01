package com.wang17.myphone.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.IBinder;

import com.wang17.myphone.R;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.model.database.RunLog;
import com.wang17.myphone.model.database.Setting;

import java.util.Timer;
import java.util.TimerTask;

@Deprecated
public class NianfoService extends Service {
    private Timer timer ;
    private SoundPool soundPool;
    private boolean isSoundLoaded;
    private int playId;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            long span = intent.getIntExtra("timer",60000);
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        final DataContext dataContext = new DataContext(getApplicationContext());
                        SoundPool soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
                        soundPool.load(getApplicationContext(), R.raw.nianfo, 1);
                        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                            @Override
                            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                                try {
                                    int playId = soundPool.play(1, 1, 1, 0, 0, 1);
                                    dataContext.editSetting(Setting.KEYS.nianfo_palyId, playId);
                                } catch (Exception e) {
                                    _Utils.printException(getApplicationContext(),e);
                                }
                            }
                        });

                    } catch (Exception e) {
                        _Utils.printException(getApplicationContext(),e);
                    }
                }
            },0,span);
        } catch (Exception e) {
            _Utils.printException(getApplicationContext(),e);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /**
         *
         */

        final DataContext dataContext = new DataContext(getApplicationContext());
        soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
        soundPool.load(getApplicationContext(), R.raw.nianfo, 1);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                try {
                    isSoundLoaded=true;
                    int playId = soundPool.play(1, 1, 1, 0, 0, 1);
                    dataContext.editSetting(Setting.KEYS.nianfo_palyId, playId);
                } catch (Exception e) {
                    _Utils.printException(getApplicationContext(),e);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        //
        soundPool.stop(playId);
        new SoundPool(10, AudioManager.STREAM_SYSTEM, 5).stop(Integer.parseInt(new DataContext(getApplicationContext()).getSetting(Setting.KEYS.nianfo_palyId,0).getString()));
        timer.cancel();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
