package com.wang17.myphone.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.wang17.myphone.util._Utils;

public class SpeakerService extends Service {
    public SpeakerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("wangsc",intent.getStringExtra("msg"));
        _Utils.speaker(getApplicationContext(),intent.getStringExtra("msg"));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
