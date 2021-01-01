package com.wang17.myphone;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.amap.api.maps.TextureMapView;
import com.wang17.myphone.fragment.ActionBarFragment;
import com.wang17.myphone.fragment.ButtonFragment;
import com.wang17.myphone.fragment.MarkDayFragment;
import com.wang17.myphone.fragment.BuddhaPlayerFragment;
import com.wang17.myphone.fragment.SettingFragment;
import com.wang17.myphone.fragment.ReligiousFragment;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.model.database.Position;
import com.wang17.myphone.receiver.LocationRecordAlarmReceiver;
import com.wang17.myphone.receiver.NetWorkStateReceiver;
import com.wang17.myphone.service.FloatingWindowService;
import com.wang17.myphone.util.BackupTask;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._OkHttpUtil;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.model.database.Setting;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static com.wang17.myphone.receiver.AlarmReceiver.ALARM_TRADE;
import static com.wang17.myphone.util._Utils.e;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements BackupTask.OnFinishedListener, ActionBarFragment.OnActionFragmentSettingListener {

    private static final String _TAG = "wangsc";
    public static final int LOCATION_RECORD = 95874;
    private static final int REQUEST_CODE = 1;
    // 视图变量
    private ViewPager mViewPager;
    // 类变量

    // 值变量
    private ViewPagerAdapter mViewPagerAdapter;
    private List<Fragment> fragmentList;
    private TabLayout tabLayout_menu;
    private DataContext mDataContext;
    private boolean locationIsRunning;

    private Handler uiThreadHandler;


    @Override
    protected void onPause() {
        super.onPause();
        e("main activity on pause");
    }


    @Override
    protected void onStop() {
        super.onStop();
        e("main activity on stop");
    }

    @Override
    protected void onDestroy() {
//        unregisterReceiver(netWorkStateReceiver);
//        new BackupTask(this).execute(BackupTask.COMMAND_BACKUP);
//        receiver.unRegisterScreenActionReceiver(this);
        super.onDestroy();
        e("main activity on destory");
    }

    @Override
    protected void onStart() {
        super.onStart();
        e("main activity on start");
    }

    NetWorkStateReceiver netWorkStateReceiver;

    @Override
    protected void onResume() {
        super.onResume();
        e("main activity on resume");
    }


    private TextureMapView textureMapView;

    private Animation rorateAnimation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MainActivityPermissionsDispatcher.showUMAnalyticsWithCheck(this);
        uiThreadHandler = new Handler();

        FloatingActionButton btn = findViewById(R.id.floatingActionButton);
        btn.setOnLongClickListener(new View.OnLongClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public boolean onLongClick(View v) {
                if(FloatingWindowService.isStarted){
                    stopService(new Intent(MainActivity.this, FloatingWindowService.class));
                }else{
                    startFloatingButtonService();
                    _Utils.clickHomeButton(MainActivity.this);
                }
                return true;
            }
        });


        rorateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_animation);
        LinearInterpolator lin = new LinearInterpolator();
        rorateAnimation.setInterpolator(lin);
        //
        init(savedInstanceState);

        /**
         * 初始化声音
         */
        mSoundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
        soundId = mSoundPool.load(MainActivity.this, R.raw.clock, 1);
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                isSoundLoaded = true;
            }
        });

    }

    //region 设置悬浮窗权限
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startFloatingButtonService() {
        if (FloatingWindowService.isStarted) {
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        } else {
            startService(new Intent(MainActivity.this, FloatingWindowService.class));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                startService(new Intent(MainActivity.this, FloatingWindowService.class));
            }
        }
    }
    //endregion


    private void init(Bundle savedInstanceState) {
        try {
            mDataContext = new DataContext(this);

            fragmentList = new ArrayList<>();
            tabLayout_menu = (TabLayout) findViewById(R.id.tabLayout_menu);

            fragmentList.add(new ReligiousFragment());
            tabLayout_menu.addTab(tabLayout_menu.newTab().setText("宝鉴"));
            fragmentList.add(new MarkDayFragment());
            tabLayout_menu.addTab(tabLayout_menu.newTab().setText("MARK"));
            fragmentList.add(new BuddhaPlayerFragment());
            tabLayout_menu.addTab(tabLayout_menu.newTab().setText("念佛"));
            fragmentList.add(new ButtonFragment());
            tabLayout_menu.addTab(tabLayout_menu.newTab().setText("操作"));
            fragmentList.add(new SettingFragment());
            tabLayout_menu.addTab(tabLayout_menu.newTab().setText("设置"));


            tabLayout_menu.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    try {
                        mViewPager.setCurrentItem(tab.getPosition());
//                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_content, fragmentList.get(tab.getPosition())).commit();
                        mDataContext.editSetting(Setting.KEYS.main_start_page_index, tab.getPosition());
                    } catch (Exception e) {
                        _Utils.printException(MainActivity.this, e);
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });


            mViewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
            mViewPager = findViewById(R.id.viewPage_content);
            mViewPager.setOffscreenPageLimit(3);
            mViewPager.setAdapter(mViewPagerAdapter);
            mViewPager.addTouchables(null);
            mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    tabLayout_menu.getTabAt(position).select();
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    switch (state) {
                        case ViewPager.SCROLL_STATE_IDLE:
                            break;
                        case ViewPager.SCROLL_STATE_DRAGGING:
                            break;
                        case ViewPager.SCROLL_STATE_SETTLING:
                            break;
                        default:
                            break;
                    }
                }
            });

//            int position = Integer.parseInt(mDataContext.getSetting(Setting.KEYS.main_start_page_index, 0).getString());
//            if (position >= tabLayout_menu.getTabCount())
//                position = 0;
            tabLayout_menu.getTabAt(2).select();

        } catch (NumberFormatException e) {
            _Utils.printException(MainActivity.this, e);
        }
    }

    //        @Override
//    public void onSettingButtonClickListener() {
//            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
////        startActivity(new Intent(MainActivity.this, SettingActivity.class));
//    }
//
//    @Override
//    public void onSettingButtonLongClickListener() {
//        new BackupTask(this).execute(BackupTask.COMMAND_BACKUP);
//    }
//    @Override
//    public void onBackButtonClickListener() {
//    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            AudioManager audio = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    audio.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    audio.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                    return true;

                case KeyEvent.KEYCODE_BACK:
                    _Utils.clickHomeButton(this);
                default:
                    break;
            }
        } catch (Exception e) {
            _Utils.printException(MainActivity.this, e);
        }
        return super.onKeyDown(keyCode, event);
    }


    public static void startLocationRecordAlarm(Context context) {
        try {
            DateTime now = new DateTime();
            DateTime targetTime = now.addHours(1);
            targetTime.set(Calendar.MINUTE, 0);
            targetTime.set(Calendar.SECOND, 0);
            targetTime.set(Calendar.MILLISECOND, 0);

            Intent intent = new Intent(context, LocationRecordAlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, LOCATION_RECORD, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);

            if (Build.VERSION.SDK_INT >= 19) {
                am.setExact(AlarmManager.RTC_WAKEUP, targetTime.getTimeInMillis(), pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, targetTime.getTimeInMillis(), pi);
            }

        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public static void stopLocationRecordAlarm(Context context) {
        try {
            Intent intent = new Intent(context, LocationRecordAlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, LOCATION_RECORD, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            am.cancel(pi);
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }


    public static void startTradeAlarm(Context context) {
        try {
            DateTime now = new DateTime();
            DateTime clock = new DateTime(now.getYear(), now.getMonth(), now.getDay(), 8, 50, 0);
            if (now.getTimeInMillis() > clock.getTimeInMillis()) {
                clock.set(Calendar.HOUR_OF_DAY, 11);
                clock.set(Calendar.MINUTE, 27);
            }
            if (now.getTimeInMillis() > clock.getTimeInMillis()) {
                clock.set(Calendar.HOUR_OF_DAY, 13);
                clock.set(Calendar.MINUTE, 27);
            }
            if (now.getTimeInMillis() > clock.getTimeInMillis()) {
                clock.set(Calendar.HOUR_OF_DAY, 14);
                clock.set(Calendar.MINUTE, 55);
            }
            if (now.getTimeInMillis() > clock.getTimeInMillis()) {
                clock.set(Calendar.HOUR_OF_DAY, 20);
                clock.set(Calendar.MINUTE, 50);
            }
            if (now.getTimeInMillis() > clock.getTimeInMillis()) {
                if (clock.get(Calendar.DAY_OF_WEEK) == DateTime.FRIDAY)
                    clock.addDays(3);
                else
                    clock.addDays(1);
                clock.set(Calendar.HOUR_OF_DAY, 8);
                clock.set(Calendar.MINUTE, 50);
            }

            Log.e("wangsc", clock.toLongDateTimeString());

            Intent intent = new Intent(_Session.ACTION_ALARM_TRADE);
            PendingIntent pi = PendingIntent.getBroadcast(context, ALARM_TRADE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);

            if (Build.VERSION.SDK_INT >= 19) {
                am.setExact(AlarmManager.RTC_WAKEUP, clock.getTimeInMillis(), pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, clock.getTimeInMillis(), pi);
            }
            new DataContext(context).editSetting(Setting.KEYS.is_trade_alarm_open, true);

        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public static void stopTradeAlarm(Context context) {
        try {
            Intent intent = new Intent(_Session.ACTION_ALARM_TRADE);
            PendingIntent pi = PendingIntent.getBroadcast(context, ALARM_TRADE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            am.cancel(pi);
            new DataContext(context).editSetting(Setting.KEYS.is_trade_alarm_open, false);
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    @Override
    public void onFinished(int Result) {
        BackupTask.Finished(Result, tabLayout_menu);
    }

    @Override
    public void onSettingButtonClickListener() {

        /**
         * 检测是否为默认短信程序，如果不是，显示设置按钮。
         */
        final String myPackageName = getPackageName();
        if (!Telephony.Sms.getDefaultSmsPackage(this).equals(myPackageName)) {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, myPackageName);
            startActivity(intent);
        }
    }

    @Override
    public void onSettingButtonLongClickListener() {

    }


    public class ViewPagerAdapter extends FragmentPagerAdapter {

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }
    }

    //region 申请友盟统计所需要的权限。
    @NeedsPermission({
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    })
    void showUMAnalytics() {
        // 需要一个或多个权限，当这些权限全部通过后，执行此方法。
        //　调用统计接口，触发 MTA 并上传数据。
//        UMAnalyticsConfig(this, "583d7eede88bad552b00138c", "all", MobclickAgent.EScenarioType.E_UM_NORMAL);
//        init();

    }

    @OnShowRationale({
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    })
    void showRationaleForUMAnalytics(final PermissionRequest request) {
        //  解释了为什么需要此权限。这是权限第一次被拒绝之后显示的提示信息。
        // It passes in a PermissionRequest object which can be used to continue or abort the current permission request upon user input
        showRationaleDialog("软件部分功能需要此权限，禁止此权限不会影响软件正常使用，但获取此权限可以给开发者提供软件运行信息，便于软件的进一步开发与维护。", request);
//        MainActivity.this.finish();
    }

    @OnPermissionDenied({
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.READ_SMS,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION})
    void showDeniedForUMAnalytics() {
        //  如果用户拒绝权限，执行此方法
        MainActivity.this.finish();
    }

    @OnNeverAskAgain({
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.READ_SMS,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION})
    void showNeverAskForUMAnalytics() {
        //  如果用户对某一权限选择“再也不会问”，执行此方法
        new AlertDialog.Builder(this).setMessage("不会再询问此权限！").setPositiveButton("知道了", null).show();
        MainActivity.this.finish();
    }

    private void showRationaleDialog(String message, final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setPositiveButton("允许", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.cancel();
                        MainActivity.this.finish();
                    }
                })
                .setCancelable(false)
                .setMessage(message)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }
    //endregion


    //region scan stocks
    private List<Position> positions;
    private boolean isSoundLoaded;
    private SoundPool mSoundPool;
    private int soundId;

    private Timer timer;
    private long preTime;

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
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
                        @Override
                        public void run() {
                            // TODO: 2019/4/26 test
                            //region test
                            long now = System.currentTimeMillis();
                            long span = (now - preTime) / 1000;
                            preTime = now;
                            if (span < 3600)
                                _Utils.speaker(MainActivity.this.getApplicationContext(), span + "");
                            //endregion
                            fillStockInfoList();
                        }
                    });
                }
            }, 0, 5000);
        } catch (Exception e) {
            _Utils.printException(this, e);
        }
    }

    private void fillStockInfoList() {

        //http://hq.sinajs.cn/list=sh601555
        //var hq_str_sh601555="东吴证券,11.290,11.380,11.160,11.350,11.050,11.160,11.170,61431561,687740501.000,3500,11.160,144700,11.150,98500,11.140,78500,11.130,99200,11.120,143700,11.170,99700,11.180,28700,11.190,41500,11.200,41500,11.210,2019-04-17,15:00:00,00";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    double totalProfit = 0;
                    for (Position position : positions) {
                        String place = "sh";
                        if (position.getCode().substring(0, 2).equals("00")) {
                            place = "sz";
                            if (position.getCode().equals("000001")) {
                                place = "sh";
                            }
                        } else if (position.getCode().substring(0, 3).equals("399")) {
                            place = "sz";
                        }
                        String url = "https://hq.sinajs.cn/list=" + place + position.getCode();

                        OkHttpClient client = _OkHttpUtil.client;
                        Request request = new Request.Builder().url(url).build();
                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            String sss = response.body().string();
                            String[] result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",");

                            double price = Double.parseDouble(result[3]);
                            final double profit = (price - position.getCost()) / position.getCost();
                            totalProfit += profit;
                        }
                    }

                    double averageTotalProfit = totalProfit / positions.size();
                    if (Math.abs(averageTotalProfit - preAverageTotalProfit) * 100 > 1.0 / positions.size()) {
                        preAverageTotalProfit = averageTotalProfit;
                        String msg = new DecimalFormat("0.00").format(averageTotalProfit * 100);
                        if (averageTotalProfit > 0) {
                            msg = "加" + msg;
                        }
                        _Utils.speaker(MainActivity.this.getApplicationContext(), msg);
                    }


                    if (mDataContext.getSetting(Setting.KEYS.is_stock_load_noke, false).getBoolean() && ++loadStockCount >= 20) {
                        playknock();
                        loadStockCount = 0;
                    }

                } catch (IOException e) {
                    _Utils.printException(getApplicationContext(), e);
                }
            }
        }).start();
    }

    private void playknock() {
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


    //endregion


}
