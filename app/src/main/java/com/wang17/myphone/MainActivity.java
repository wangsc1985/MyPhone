package com.wang17.myphone;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Telephony;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.amap.api.maps.TextureMapView;
import com.wang17.myphone.eventbus.ChangeFragmentTab;
import com.wang17.myphone.eventbus.EventBusMessage;
import com.wang17.myphone.fragment.ActionBarFragment;
import com.wang17.myphone.fragment.OperationFragment;
import com.wang17.myphone.fragment.MarkDayFragment;
import com.wang17.myphone.fragment.BuddhaFragment;
import com.wang17.myphone.fragment.ReligiousFragment;
import com.wang17.myphone.receiver.NetWorkStateReceiver;
import com.wang17.myphone.util.BackupTask;
import com.wang17.myphone.dao.DataContext;
import com.wang17.myphone.util._DialogUtils;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.database.Setting;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BackupTask.OnFinishedListener, ActionBarFragment.OnActionFragmentSettingListener {

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

    private Handler uiHandler;


    @Override
    protected void onPause() {
        super.onPause();
//        e("main activity on pause");
    }


    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
//        e("main activity on stop");
    }

    @Override
    protected void onDestroy() {
//        unregisterReceiver(netWorkStateReceiver);
//        new BackupTask(this).execute(BackupTask.COMMAND_BACKUP);
//        receiver.unRegisterScreenActionReceiver(this);
        super.onDestroy();
//        e("main activity on destory");
    }

    @Override
    protected void onStart() {
        super.onStart();
//        e("main activity on start");
    }

    NetWorkStateReceiver netWorkStateReceiver;

    @Override
    protected void onResume() {
        super.onResume();
//        e("main activity on resume");
    }

    private TextureMapView textureMapView;

    private Animation rorateAnimation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        uiHandler = new Handler();
        EventBus.getDefault().register(this);

        rorateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_animation);
        LinearInterpolator lin = new LinearInterpolator();
        rorateAnimation.setInterpolator(lin);
        //
        init(savedInstanceState);
    }

    //region 设置悬浮窗权限
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startFloatingButtonService() {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (!Settings.canDrawOverlays(this)) {
                _DialogUtils.showMessageBox(this,"授权失败","确定");
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                _DialogUtils.showMessageBox(this,"授权成功");
            }
        }
    }
    //endregion

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGetMessage( EventBusMessage bus) {
        if(bus.getSender() instanceof ChangeFragmentTab){
            tabLayout_menu.getTabAt(Integer.parseInt(bus.getMsg())).select();
        }
    }

    private void init(Bundle savedInstanceState) {
        try {
            mDataContext = new DataContext(this);

            fragmentList = new ArrayList<>();
            tabLayout_menu = findViewById(R.id.tabLayout_menu);

            fragmentList.add(new ReligiousFragment());
            tabLayout_menu.addTab(tabLayout_menu.newTab().setText("宝鉴"));
            fragmentList.add(new MarkDayFragment());
            tabLayout_menu.addTab(tabLayout_menu.newTab().setText("MARK"));
            fragmentList.add(new BuddhaFragment());
            tabLayout_menu.addTab(tabLayout_menu.newTab().setText("念佛"));
            fragmentList.add(new OperationFragment());
            tabLayout_menu.addTab(tabLayout_menu.newTab().setText("操作"));


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
            tabLayout_menu.getTabAt(2).select();
        } catch (NumberFormatException e) {
            _Utils.printException(MainActivity.this, e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            AudioManager audio = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
//                    EventBus.getDefault().post(EventBusMessage.getInstance(new FromBuddhaVolumeAdd(), ""));
                    audio.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
//                    EventBus.getDefault().post(EventBusMessage.getInstance(new FromBuddhaVolumeMinus(), ""));
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

    @Override
    public void onFinished(int Result) {
        BackupTask.Finished(Result, this);
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

    //endregion

    //region scan stocks
    private SoundPool mSoundPool;
    //endregion


}
