package com.wang17.myphone.activity;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.database.Setting;
import com.wang17.myphone.database.Position;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._LogUtils;
import com.wang17.myphone.util._Utils;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NianfoDarkRunActivity extends AppCompatActivity {

    private TextView textViewTotal, textViewBattery;

    private List<Position> positions;
    private DataContext mDataContext;

    private int clickCount;
    private long preClickTime;
    private MainReciver mainReciver;

    private long preBatteryTime;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * 设置全屏的俩段代码必须在setContentView(R.layout.main) 之前，不然会报错。
         */
        //无title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_nianfo_dark_run);

        textViewTotal = findViewById(R.id.textView_monthDuration);
        textViewBattery = findViewById(R.id.textView_battery);

//        FrameLayout root = (FrameLayout) findViewById(R.id.root);
//        root.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                long ctm = System.currentTimeMillis();
//                if (ctm - preClickTime < 1000) {
//                    if (++clickCount >= 4) {
//                        FutrueDarkRunActivity.this.finish();
//                    }
//                } else {
//                    clickCount = 0;
//                }
//                preClickTime = ctm;
//                Log.e("wangsc", clickCount + "");
//            }
//        });

        mDataContext = new DataContext(this);
        preClickTime = System.currentTimeMillis();

        positions = mDataContext.getPositions(1);


        /**
         * 初始化电量记录
         */
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        mDataContext.editSetting(Setting.KEYS.battery, battery);
        /**
         * 监听
         */
        mainReciver = new MainReciver();
        IntentFilter filter = new IntentFilter();
        // 监控开关屏
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        // 监控电量变化
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mainReciver, filter);

        /**
         *
         */
//        startService(new Intent(this, MusicService.class));

    }

    @Override
    protected void onResume() {
        super.onResume();
        /**
         * 隐藏虚拟按键
         */
        _Utils.hideBottomUIMenu(this);
        /**
         *
         */
        startTimer();

//     PARTIAL_WAKE_LOCK :保持CPU 运转，屏幕和键盘灯是关闭的。
//     SCREEN_DIM_WAKE_LOCK ：保持CPU 运转，允许保持屏幕显示但有可能是灰的，关闭键盘灯
//     SCREEN_BRIGHT_WAKE_LOCK ：保持CPU 运转，保持屏幕高亮显示，关闭键盘灯
//        _Utils.acquireWakeLock(this,PARTIAL_WAKE_LOCK);
//        _Utils.acquireWakeLock(this,SCREEN_DIM_WAKE_LOCK);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 解除监控开关屏
        if (mainReciver != null)
            unregisterReceiver(mainReciver);
        stopTimer();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    private Timer timer;
    private int totalTime = 0;

    private void startTimer() {
        if (timer != null) {
            return;
        }
        try {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void run() {
                            reflushBatteryNumber();
                            totalTime++;
                            int minite = totalTime / 60, second = totalTime % 60;
                            String secondStr = second < 10 ? "0" + second : second + "";
                            textViewTotal.setText((minite > 0 ? minite + "分" : "") + (secondStr + "秒"));
                            if (totalTime % 600 == 0) {
                                _Utils.speaker(NianfoDarkRunActivity.this, totalTime / 600 + "千");
                                _LogUtils.log2file(totalTime / 600 + "千");
                            }
                        }
                    });
                }
            }, 1000, 1000);
        } catch (Exception e) {
            _Utils.printException(this, e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void reflushBatteryNumber() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        textViewBattery.setText(battery + "%");
    }


    public class MainReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_OFF:
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    if (!pm.isScreenOn()) {
                        _Utils.speaker(NianfoDarkRunActivity.this, "屏幕关闭");
                    }
                case Intent.ACTION_SCREEN_ON:
                    pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    if (pm.isScreenOn()) {
                        _Utils.speaker(NianfoDarkRunActivity.this, "屏幕开启");
                    }
                    break;
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
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            AudioManager audio = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    audio.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_SHOW_UI);
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    audio.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_SHOW_UI);
                    return true;
                default:
                    break;
            }
        } catch (Exception e) {
            _Utils.printException(NianfoDarkRunActivity.this, e);
        }
        return super.onKeyDown(keyCode, event);
    }


}
