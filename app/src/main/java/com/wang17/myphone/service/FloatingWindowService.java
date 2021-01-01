package com.wang17.myphone.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.wang17.myphone.R;
import com.wang17.myphone.model.DateTime;

import java.util.Timer;
import java.util.TimerTask;

import kotlin.concurrent.TimersKt;

/**
 * Created by dongzhong on 2018/5/30.
 */

public class FloatingWindowService extends Service {
    public static boolean isStarted = false;
    Handler uiHandler = new Handler();

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    private Button button;

    @Override
    public void onCreate() {
        super.onCreate();
        isStarted = true;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        layoutParams.width = 250;
        layoutParams.height = 170;
        layoutParams.x = 300;
        layoutParams.y = 300;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showFloatingWindow();
        startTime();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        windowManager.removeView(button);
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
            button = new Button(getApplicationContext());
            button.setText("阿弥陀佛");
            button.setBackgroundResource(R.drawable.btn_shape);
            button.setTextColor(Color.RED);
            button.setTextSize(20);
//            button.setBackgroundColor(Color.GRAY);
            windowManager.addView(button, layoutParams);

            button.setOnTouchListener(new FloatingOnTouchListener());
        }
    }

    Timer timer =null;
    private void startTime(){
        if(timer ==null){
            timer = new  Timer();
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        button.setText("abc\n"+new DateTime().toShortTimeString());
                    }
                });
            }
        },0,1000);
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    windowManager.updateViewLayout(view, layoutParams);
                    break;
                default:
                    break;
            }
            return false;
        }
    }
}
