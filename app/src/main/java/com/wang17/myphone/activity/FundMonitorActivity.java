package com.wang17.myphone.activity;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.model.Commodity;
import com.wang17.myphone.model.StockInfo;
import com.wang17.myphone.model.database.Setting;
import com.wang17.myphone.model.database.Position;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.util._SinaStockUtils;
import com.wang17.myphone.util._String;
import com.wang17.myphone.util._Utils;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class FundMonitorActivity extends AppCompatActivity {

    private TextView textView, textViewTotalProfit, textViewBattery;

    private List<Position> positions;
    private DataContext mDataContext;

    private int clickCount;
    private long preClickTime;
    private MainReciver mainReciver;

    private boolean isSoundLoaded;
    private SoundPool mSoundPool;
    private int soundId;

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

        setContentView(R.layout.activity_fund_monitor);

        textView = findViewById(R.id.textView);
        textViewTotalProfit = findViewById(R.id.textView_totalProfit);
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

        positions = mDataContext.getPositions(0);


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
         * 初始化声音
         */
        mSoundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
        soundId = mSoundPool.load(FundMonitorActivity.this, R.raw.clock, 1);
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                isSoundLoaded = true;
            }
        });


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
    protected void onPause() {
        super.onPause();
        stopTimer();
        // 解除监控开关屏
        if (mainReciver != null)
            unregisterReceiver(mainReciver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        _Utils.releaseWakeLock(this);
//        stopService(new Intent(this, MusicService.class));

    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    private Timer timer;
    private double preAverageProfit;

    private void e(Object log){
        Log.e("wangsc",log.toString());
    }
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
                            try {
                                _SinaStockUtils.getStockInfoList(positions, new _SinaStockUtils.OnLoadStockInfoListListener() {
                                    @Override
                                    public void onLoadFinished(@NotNull List<? extends StockInfo> stockInfoList, double totalProfit, double averageProfit, @NotNull String time) {

                                        try {
                                            int size = stockInfoList.size();
                                            e(size+" , "+totalProfit+" , "+averageProfit);
                                            if (stockInfoList.size() == 0) {
                                                return;
                                            }
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    textViewTotalProfit.setText(new DecimalFormat("0.00").format(averageProfit*100));
                                                    textView.setText(time);

                                                    if (averageProfit > 0) {
                                                        textViewTotalProfit.setTextColor(getResources().getColor(R.color.month_text_color));
                                                    } else if (averageProfit == 0) {
                                                        textViewTotalProfit.setTextColor( Color.WHITE);
                                                    } else {
                                                        textViewTotalProfit.setTextColor(getResources().getColor(R.color.DARK_GREEN));
                                                    }
                                                }
                                            });

                                            String speakMsg = "";
                                            //region 股票平均盈利

                                            String msgS = new DecimalFormat("0.00").format(averageProfit * 100);
                                            Log.e("wangsc", "averageTotalProfitS: " + msgS);

                                            if (Math.abs(averageProfit - preAverageProfit) * 100 > 1.0 / size) {
                                                preAverageProfit = averageProfit;
                                                speakMsg += msgS;
                                            }


                                            if (!speakMsg.isEmpty() && mDataContext.getSetting(Setting.KEYS.is_stock_broadcast, true).getBoolean()) {
                                                float pitch = 1.0f, speech = 1.2f;
                                                if (averageProfit < 0) {
                                                    pitch = 0.1f;
                                                    speech = 0.8f;
                                                }
                                                _Utils.speaker(getApplicationContext(), speakMsg, pitch, speech);
                                            }
                                        } catch (Exception e) {
                                            _Utils.printException(FundMonitorActivity.this, e);
                                            Log.e("wangsc", e.getMessage());
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    });
                }
            }, 0, 10000);
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

    private String mTime;

    private boolean isFirst = true;

    public Commodity findCommodity(String code) {
        for (Commodity commodity : _Session.commoditys) {
            if (commodity.item.toLowerCase().equals(parseItem(code).toLowerCase())) {
                return commodity;
            }
        }
        return null;
    }

    public String getFormat(Commodity commodity) {
        if (commodity != null) {
            double i = 1;
            int fc = 0;
            while (commodity.cose / i < 1) {
                i /= 10;
                fc++;
            }
            String format = "0";
            for (int j = 0; j < fc; j++) {
                if (j == 0)
                    format += ".0";
                else
                    format += "0";
            }
            return format;
        }
        return "0";
    }

    private String parseItem(String code) {
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

    private void playknock() {
        //region test
//        long now = System.currentTimeMillis();
//        long span = (now - preTime) / 1000;
//        preTime = now;
//        if (span < 3600)
//            _Utils.speaker(StockDarkRunActivity.this, span+"");
        //endregion
        if (!isSoundLoaded) {
            mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    isSoundLoaded = true;
                    soundPool.play(soundId, 0.6f, 0.6f, 0, 0, 1);
                }
            });
        } else {
            mSoundPool.play(soundId, 0.6f, 0.6f, 0, 0, 1);
        }
    }

    private int loadStockCount = 0;
    private double preAverageTotalProfit = 0;


    public class MainReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_OFF:
                case Intent.ACTION_SCREEN_ON:
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    if (!pm.isScreenOn()) {
                        _Utils.speaker(FundMonitorActivity.this.getApplicationContext(), mDataContext.getSetting(Setting.KEYS.speaker_screen_off, "关").getString());
                    }
                    break;
                case Intent.ACTION_BATTERY_CHANGED:
                    int level = intent.getIntExtra("level", 0);
                    int dataLevel = mDataContext.getSetting(Setting.KEYS.battery, 0).getInt();
                    if (dataLevel != level) {
                        long span = System.currentTimeMillis() - (preBatteryTime == 0 ? System.currentTimeMillis() : preBatteryTime);
                        mDataContext.addLog("battery", _String.concat("{", level, "%}{", span / 60000, "分", span % 60000 / 1000, "秒}",""),"");
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
            _Utils.printException(FundMonitorActivity.this, e);
        }
        return super.onKeyDown(keyCode, event);
    }

}
