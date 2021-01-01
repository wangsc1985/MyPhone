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
import com.wang17.myphone.model.database.Setting;
import com.wang17.myphone.model.database.Position;
import com.wang17.myphone.util._AnimationUtils;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._LogUtils;
import com.wang17.myphone.util._OkHttpUtil;
import com.wang17.myphone.util._String;
import com.wang17.myphone.util._Utils;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
@Deprecated
public class StockDarkRunActivity extends AppCompatActivity {

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

        setContentView(R.layout.activity_dark_run_stock);


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
//                        StockDarkRunActivity.this.finish();
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
        soundId = mSoundPool.load(StockDarkRunActivity.this, R.raw.clock, 1);
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
        startTimer();
        /**
         * 隐藏虚拟按键
         */
        _Utils.hideBottomUIMenu(this);
        Log.e("wangsc", "on resume()...");
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

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    private Timer timer;
    private String preTime;

    private void startTimer() {
        if (timer != null) {
            return;
        }
        try {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
                            int battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                            textViewBattery.setText(battery + "%");

                            fillStockInfoList();
                        }
                    });
                }
            }, 0, 5000);
        } catch (Exception e) {
            _Utils.printException(this, e);
        }
    }

    private String mTime;

    private void fillStockInfoList() {

        //http://hq.sinajs.cn/list=sh601555
        //var hq_str_sh601555="东吴证券,11.290,11.380,11.160,11.350,11.050,11.160,11.170,61431561,687740501.000,3500,11.160,144700,11.150,98500,11.140,78500,11.130,99200,11.120,143700,11.170,99700,11.180,28700,11.190,41500,11.200,41500,11.210,2019-04-17,15:00:00,00";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    double totalProfit = 0;
                    double totalAmount = 0;
                    if (positions.size() <= 0)
                        return;
                    for (Position position : positions) {
                        String url = "https://hq.sinajs.cn/list=" + position.getExchange() + position.getCode();

                        OkHttpClient client = _OkHttpUtil.client;
                        Request request = new Request.Builder().url(url).build();
                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            String sss = response.body().string();
                            String[] result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",");

                            double price = Double.parseDouble(result[3]);
                            final double profit = (price - position.getCost()) / position.getCost();
                            mTime = result[31];
                            totalProfit += profit * position.getAmount() * position.getCost() * 100;
                            totalAmount += position.getAmount() * position.getCost() * 100;
                        } else {
                            _LogUtils.log2file("获取数据失败...");
                            return;
                        }
                    }

                    final double averageTotalProfit = totalProfit / totalAmount;
                    if (Math.abs(averageTotalProfit - preAverageTotalProfit) * 100 > 1.0 / positions.size()) {
                        preAverageTotalProfit = averageTotalProfit;
                        String msg = new DecimalFormat("0.00").format(averageTotalProfit * 100);
                        if (averageTotalProfit > 0) {
                            msg = "加" + msg;
                        }
                        if (!isFirst) {
                            _Utils.speaker(StockDarkRunActivity.this.getApplicationContext(), msg);
                        }
                    }
                    isFirst = false;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textViewTotalProfit.setText(new DecimalFormat("0.00").format(averageTotalProfit*100));
                            if(averageTotalProfit>0){
                                textViewTotalProfit.setTextColor(Color.RED);
                            }else if(averageTotalProfit<0){
                                textViewTotalProfit.setTextColor(Color.GREEN);
                            }else{
                                textViewTotalProfit.setTextColor(Color.WHITE);
                            }
                            textView.setText(mTime);
                            if (!mTime.equals(preTime)) {
                                _AnimationUtils.heartBeat(textViewTotalProfit);
                                preTime = mTime;
                            }
                        }
                    });


                    if (mDataContext.getSetting(Setting.KEYS.is_stock_load_noke, false).getBoolean() && ++loadStockCount >= 20) {
                        playknock();
                        loadStockCount = 0;
                    }

                } catch (Exception e) {
//                    e.printStackTrace();
//                    _Utils.printException(getApplicationContext(), e);
                }
            }
        }).start();
    }

    private boolean isFirst = true;


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
                        _Utils.speaker(StockDarkRunActivity.this.getApplicationContext(), mDataContext.getSetting(Setting.KEYS.speaker_screen_off, "关").getString());
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
            _Utils.printException(StockDarkRunActivity.this, e);
        }
        return super.onKeyDown(keyCode, event);
    }
}
