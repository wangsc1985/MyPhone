package com.wang17.myphone.activity;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.wang17.myphone.MainActivity;
import com.wang17.myphone.R;
import com.wang17.myphone.event.LocationGearEvent;
import com.wang17.myphone.event.LocationIsAutomaticEvent;
import com.wang17.myphone.fragment.ActionBarFragment;
import com.wang17.myphone.fragment.WebDialogFragment;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.model.database.PhoneMessage;
import com.wang17.myphone.service.NianfoMusicService;
import com.wang17.myphone.util.BackupTask;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util.SmsHelper;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.model.database.RunLog;
import com.wang17.myphone.model.database.Setting;
import com.wang17.myphone.service.LocationService;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.Manifest.permission.WRITE_SETTINGS;
@Deprecated
public class SettingActivity extends AppCompatActivity implements ActionBarFragment.OnActionFragmentBackListener, BackupTask.OnFinishedListener {

    // 视图变量
    private Button button_1;
    private Button button_2;
    private Button button_3;
    private Button button_4;
    private Button button_5;
    private Button button_6;
    private Button button_7;
    private Button button_8;
    private ListView listViewRunLog, listViewSetting;
    private Switch aSwitch_1, aSwitch_2, aSwitch_4, aSwitch_3,aSwitchTradeAlarm;
    private SeekBar seekBarVolumn, seekBarHeadset;
    private Spinner spinnerMusicNames, spinnerLocationGear;
    // 值变量
    private DataContext dataContext;
    private List<RunLog> runLogs;
    private List<Setting> settings;
    private RunlogListdAdapter adapter;
    private SettingListdAdapter setAdapter;
    private Handler uiHandler;


    private void fillSpinner(Spinner spinner, List<String> values) {
        ArrayAdapter<String> aspn = new ArrayAdapter<>(SettingActivity.this, R.layout.inflate_spinner, values);
        aspn.setDropDownViewResource(R.layout.inflate_spinner_dropdown);
        spinner.setAdapter(aspn);
    }

    private void initMusicNames() {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < _Session.TALLY_MUSIC_NAMES.length; i++) {
            list.add(_Session.TALLY_MUSIC_NAMES[i]);
        }
        this.fillSpinner(spinnerMusicNames, list);
    }

    private void initLocationGear() {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < _Session.GEAR_NAMES.length; i++) {
            list.add(_Session.GEAR_NAMES[i]);
        }
        this.fillSpinner(spinnerLocationGear, list);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        uiHandler = new Handler();
        dataContext = new DataContext(this);
//        runLogs = dataContext.getRunLogsByLike("运行错误");
        runLogs = dataContext.getRunLogsByLike(new String[]{"定位","运行错误"});
        settings = dataContext.getSettings();

        spinnerMusicNames = (Spinner) findViewById(R.id.spinner_musicName);
        initMusicNames();
        String databaseMusicName = dataContext.getSetting(Setting.KEYS.tally_music_name, _Session.TALLY_MUSIC_NAMES[0]).getString();
        for (int i = 0; i < 4; i++) {
            if (spinnerMusicNames.getItemAtPosition(i).toString().equals(databaseMusicName)) {
                spinnerMusicNames.setSelection(i);
                break;
            }
        }
        spinnerMusicNames.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String name = spinnerMusicNames.getItemAtPosition(position).toString();
                        if (!name.equals(dataContext.getSetting(Setting.KEYS.tally_music_name, _Session.TALLY_MUSIC_NAMES[0]).getString())) {
                            dataContext.editSetting(Setting.KEYS.tally_music_name, name);
                            seekBarVolumn.setProgress((int) (dataContext.getSetting(spinnerMusicNames.getItemAtPosition(position), 0.3f).getFloat() * 100));
                            if (dataContext.getSetting(Setting.KEYS.tally_music_is_playing, false).getBoolean()) {
                                stopService(new Intent(SettingActivity.this, NianfoMusicService.class));
                                startService(new Intent(SettingActivity.this, NianfoMusicService.class));
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                }

        );

        aSwitchTradeAlarm = (Switch)findViewById(R.id.switch_tradeAlarm);
        aSwitchTradeAlarm.setChecked(dataContext.getSetting(Setting.KEYS.is_trade_alarm_open,true).getBoolean());
        aSwitchTradeAlarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    MainActivity.startTradeAlarm(SettingActivity.this);
                }else{
                    MainActivity.stopTradeAlarm(SettingActivity.this);
                }
            }
        });

        spinnerLocationGear = (Spinner) findViewById(R.id.spinner_locationGear);
        initLocationGear();
        spinnerLocationGear.setSelection(dataContext.getSetting(Setting.KEYS.map_location_gear, 0).getInt());
        spinnerLocationGear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int gear = position;
                if (gear != dataContext.getSetting(Setting.KEYS.map_location_gear, 0).getInt()) {
                    EventBus.getDefault().post(new LocationGearEvent(gear));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        final LinearLayout layoutOption = (LinearLayout) findViewById(R.id.layout_option);

        //
        listViewRunLog = (ListView) findViewById(R.id.listView_runlog);
        adapter = new RunlogListdAdapter();
        listViewRunLog.setAdapter(adapter);
        listViewRunLog.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(SettingActivity.this).setMessage("确认要删除当前日志吗？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dataContext.deleteRunLog(runLogs.get(position).getId());
                        runLogs = dataContext.getRunLogs();
                        adapter.notifyDataSetChanged();
                    }
                }).setNegativeButton("取消", null).show();
                return true;
            }
        });

        listViewSetting = (ListView) findViewById(R.id.listView_setting);
        setAdapter = new SettingListdAdapter();
        listViewSetting.setAdapter(setAdapter);
        listViewSetting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Setting setting = settings.get(position);
                if (ignoreWords.contains(setting.getName())) {
                    return;
                }
                if (setting.getString().equals("false") || setting.getString().equals("true")) {
                    new AlertDialog.Builder(SettingActivity.this).setTitle(setting.getName()).setPositiveButton("是", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dataContext.editSetting(setting.getName(), true);
                            settings = dataContext.getSettings();
                            setAdapter.notifyDataSetChanged();

                            if (setting.getName().equals(Setting.KEYS.map_location_isAutoChangeGear.toString())) {
                                EventBus.getDefault().post(new LocationIsAutomaticEvent(true));
                            }
                        }
                    }).setNegativeButton("否", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dataContext.editSetting(setting.getName(), false);
                            settings = dataContext.getSettings();
                            setAdapter.notifyDataSetChanged();

                            if (setting.getName().equals(Setting.KEYS.map_location_isAutoChangeGear.toString())) {
                                EventBus.getDefault().post(new LocationIsAutomaticEvent(false));
                            }
                        }
                    }).show();
                } else {
                    View v = View.inflate(SettingActivity.this, R.layout.inflate_editbox, null);
                    final EditText input = (EditText) v.findViewById(R.id.editText_value);
                    input.setText(setting.getString());
                    new AlertDialog.Builder(SettingActivity.this).setTitle(setting.getName()).setView(v).setPositiveButton("修改", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!input.getText().equals(setting.getString())) {
                                dataContext.editSetting(setting.getName(), input.getText());
                                settings = dataContext.getSettings();
                                setAdapter.notifyDataSetChanged();
                            }
                        }
                    }).show();
                }

            }
        });
        listViewSetting.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(SettingActivity.this).setMessage("确认要删除当前设置吗？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dataContext.deleteSetting(settings.get(position).getName());
                        settings = dataContext.getSettings();
                        setAdapter.notifyDataSetChanged();
                    }
                }).setNegativeButton("取消", null).show();
                return true;
            }
        });

        seekBarHeadset = (SeekBar) findViewById(R.id.seekBar_headset);
        int headset = dataContext.getSetting(Setting.KEYS.headset_volume, 12).getInt();
        seekBarHeadset.setMax(15);
        seekBarHeadset.setProgress(headset);
        seekBarHeadset.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                dataContext.editSetting(Setting.KEYS.headset_volume, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        ImageView imageViewHandle = (ImageView) findViewById(R.id.imageView_handle);
        imageViewHandle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (layoutOption.getVisibility() == View.GONE) {
                        layoutOption.setVisibility(View.VISIBLE);
//                        dataContext.editSetting(Setting.KEYS.option_layout_visibility, true);
                    } else {
                        layoutOption.setVisibility(View.GONE);
//                        dataContext.editSetting(Setting.KEYS.option_layout_visibility, false);
                    }
                } catch (Exception e) {
                    _Utils.printException(SettingActivity.this, e);
                }
            }
        });

        seekBarVolumn = (SeekBar) findViewById(R.id.seekBar_volumn);
        int volumn = (int) (dataContext.getSetting(dataContext.getSetting(Setting.KEYS.tally_music_name, _Session.TALLY_MUSIC_NAMES[0]).getString(), 0.3f).getFloat() * 100);
        seekBarVolumn.setMax(40);
        seekBarVolumn.setProgress(volumn);
        seekBarVolumn.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                dataContext.editSetting(dataContext.getSetting(Setting.KEYS.tally_music_name, _Session.TALLY_MUSIC_NAMES[0]).getString(), (float) progress / 100);
                if (dataContext.getSetting(Setting.KEYS.tally_music_is_playing, false).getBoolean()) {
                    stopService(new Intent(SettingActivity.this, NianfoMusicService.class));
                    startService(new Intent(SettingActivity.this, NianfoMusicService.class));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        button_1 = (Button) findViewById(R.id.button_1);
        button_1.setText("日志");
        button_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listViewSetting.setVisibility(View.INVISIBLE);
                listViewRunLog.setVisibility(View.VISIBLE);
                runLogs = dataContext.getRunLogs();
                Iterator<RunLog> iterator = runLogs.iterator();
//                while(iterator.hasNext()){
//                    RunLog runLog = iterator.next();
//                    if(runLog.getItem().contains("定位")){
//                        iterator.remove();
//                    }
//                }
                adapter.notifyDataSetChanged();
                
            }
        });
        button_1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dataContext.clearRunLog();
                runLogs.clear();
                adapter.notifyDataSetChanged();
                
                return true;
            }
        });

        button_2 = (Button) findViewById(R.id.button_2);
        button_2.setText("备份");
        button_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new BackupTask(SettingActivity.this).execute(BackupTask.COMMAND_BACKUP);
                
            }
        });
        button_2.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new BackupTask(SettingActivity.this).execute(BackupTask.COMMAND_RESTORE);
                
                return true;
            }
        });

        button_3 = (Button) findViewById(R.id.button_3);
        button_3.setText("配置");
        button_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listViewSetting.setVisibility(View.VISIBLE);
                listViewRunLog.setVisibility(View.INVISIBLE);
                settings = dataContext.getSettings();
                setAdapter.notifyDataSetChanged();
                
            }
        });
        button_4 = (Button) findViewById(R.id.button_4);
        button_4.setText("短信");
        button_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                startActivity(new Intent(SettingActivity.this, AttentionStockActivity.class));
                SmsHelper smsHelper = new SmsHelper(SettingActivity.this);
                 List<PhoneMessage> phoneMessageList = smsHelper.getSMS(new DateTime(2018,1,1));
                for(PhoneMessage phoneMessage :phoneMessageList){
                    dataContext.addPhoneMessage(phoneMessage);
                }

//                listViewSetting.setVisibility(View.INVISIBLE);
//                listViewRunLog.setVisibility(View.VISIBLE);
//                runLogs = dataContext.getRunLogsByLike("定位");
//                adapter.notifyDataSetChanged();
//                


//                dial(true);
/*                Intent intent = new Intent(SettingActivity.this, AlarmWindowActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                Snackbar.make(button_5, button_4.getText(), Snackbar.LENGTH_LONG).show();*/
            }
        });
        button_4.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {



/*                int headset = dataContext.getSetting(Setting.KEYS.headset_volume, 12).getInt();
                if (_Utils.isHeadsetExists() && dataContext.getSetting(Setting.KEYS.setting_autoSetVolume, true).getBoolean()) {
                    AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    int volume = (int) (mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 15 * headset);
                    if (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != volume) {
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
                    }
                }*/
                return true;
            }
        });
        //
        button_5 = (Button) findViewById(R.id.button_5);
        button_5.setText("违章");
        if (button_5.getText().toString().isEmpty()) {
            button_5.setVisibility(View.GONE);
        }
        button_5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebDialogFragment wdf = new WebDialogFragment();
                wdf.setCancelable(false);
                wdf.show(getSupportFragmentManager(), "违章查询");
                
            }
        });
        button_5.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dataContext.deleteRunLogByEquals("运行错误");
                runLogs.clear();
                adapter.notifyDataSetChanged();
                
                return true;
            }
        });
        button_6 = (Button) findViewById(R.id.button_6);
        button_6.setText("");
        if (button_6.getText().toString().isEmpty()) {
            button_6.setVisibility(View.GONE);
        }
        button_6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(SettingActivity.this, LocationService.class));
                Toast.makeText(SettingActivity.this, "已结束定位", Toast.LENGTH_SHORT).show();
            }
        });

        button_7 = (Button) findViewById(R.id.button_7);
        button_7.setText("");
        if (button_7.getText().toString().isEmpty()) {
            button_7.setVisibility(View.GONE);
        }
        button_7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
            }
        });
        button_7.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                
                return true;
            }
        });
        button_8 = (Button) findViewById(R.id.button_8);
        button_8.setText("定位");
        if (button_8.getText().toString().isEmpty()) {
            button_8.setVisibility(View.GONE);
        }
        button_8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingActivity.this,LocationListActivity.class));
            }
        });
        button_8.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
//                dataContext.clearLocations();
                
                return true;
            }
        });

    }

    /**
     * 拨打电话
     *
     * @return
     */
    private void dial(boolean isShowUI) {

        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:10086"));
        if (isShowUI) {
            intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:10086"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        if (ActivityCompat.checkSelfPermission(SettingActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivity(intent);
    }

    private void createDialog() {
        try {
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_alarm_finish, null);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(view).setCancelable(false).create();
            ImageView imageViewBuddha = (ImageView) view.findViewById(R.id.imageView_buddha);
            imageViewBuddha.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            TextView textViewTitle = (TextView) view.findViewById(R.id.textView_msg);
            // 用findViewById这种方式来查找视图中的元素，必须先要将此视图“填充”到该容器中。因为AlertDialog的setView方法不具有这种“填充”功能，
            // 所以先要将dialog_alarm_finish视图文件填充到一个View中，讲此View对象setView给AlertDialog，然后通过该View来查找视图文件中对应的元素。
            textViewTitle.setText("打开窗户，透透气。");
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startAlarm() {
        try {
            Intent intent = new Intent(_Session.NIAN_FO_TIMER);
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= 19) {
                am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000, pi);
            }


        } catch (Exception e) {
            _Utils.printException(this, e);
        }
    }

    private void stopAlarm() {
        try {
            Intent intent = new Intent(_Session.NIAN_FO_TIMER);
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            am.cancel(pi);
        } catch (Exception e) {
            _Utils.printException(this, e);
        }
    }

    /**
     * 设置手机飞行模式
     *
     * @param context
     * @param enabling true:设置为飞行模式 false:取消飞行模式
     */
    public void setAirplaneModeOn(Context context, boolean enabling) {
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                            Uri.parse("package:" + getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivityForResult(intent, 1);
                } else {
                    if (ContextCompat.checkSelfPermission(this, WRITE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{WRITE_SETTINGS}, 0);
                    }
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.AIRPLANE_MODE_ON, enabling ? 1 : 0);
                    Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                    intent.putExtra("state", enabling);
                    context.sendBroadcast(intent);
                }
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 判断手机是否是飞行模式
     *
     * @param context
     * @return
     */
    public boolean isAirplaneMode(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                            Uri.parse("package:" + getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivityForResult(intent, 1);
                } else {
                    if (ContextCompat.checkSelfPermission(this, WRITE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{WRITE_SETTINGS}, 0);
                    }
                    int isAirplaneMode = Settings.System.getInt(context.getContentResolver(),
                            Settings.System.AIRPLANE_MODE_ON, 0);
                    return (isAirplaneMode == 1) ? true : false;
                }
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return false;
    }

    private String test() {
        return null;
    }

    /**
     * 整点报时
     */
    private void initTimePrompt() {
        IntentFilter timeFilter = new IntentFilter();
        timeFilter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(mTimeReceiver, timeFilter);
    }

    private BroadcastReceiver mTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SoundPool soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
            soundPool.load(context, R.raw.ling, 1);
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    soundPool.play(1, 1, 1, 0, 0, 1);
                }
            });
        }
    };

    @Override
    public void onBackButtonClickListener() {
        this.finish();
    }

    @Override
    public void onFinished(int result) {
        BackupTask.Finished(result, button_5);
    }


    private List<String> keyWords;
    private List<String> ignoreWords;

    protected class SettingListdAdapter extends BaseAdapter {


        public SettingListdAdapter() {


            keyWords = new ArrayList<>();
            keyWords.add(Setting.KEYS.map_location_isAutoChangeGear.toString());
//            keyWords.add(Setting.KEYS.is_have_didi_order_running.toString());

            ignoreWords = new ArrayList<>();
            ignoreWords.add(Setting.KEYS.map_location_gear.toString());
            ignoreWords.add(Setting.KEYS.weixin_curr_account_id.toString());
            ignoreWords.add(Setting.KEYS.tally_manualOverTimeInMillis.toString());
            ignoreWords.add(Setting.KEYS.map_location_is_opened.toString());
            ignoreWords.add(Setting.KEYS.main_start_page_index.toString());
            ignoreWords.add(Setting.KEYS.tally_music_is_playing.toString());
            ignoreWords.add(Setting.KEYS.is_have_didi_order_running.toString());
            ignoreWords.add(Setting.KEYS.tally_dayTargetInMillis.toString());
            ignoreWords.add(Setting.KEYS.headset_volume.toString());
            ignoreWords.add(Setting.KEYS.tally_music_switch.toString());

        }

        @Override
        public int getCount() {
            return settings.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final int index = position;
            try {
                convertView = View.inflate(SettingActivity.this, R.layout.inflate_runlog, null);
                final Setting set = settings.get(position);
                TextView textViewKey = (TextView) convertView.findViewById(R.id.textView_dateTime);
                TextView textViewValue = (TextView) convertView.findViewById(R.id.textView11);
                TextView textView2 = (TextView) convertView.findViewById(R.id.textView_body);
                textViewKey.setText(set.getName());
                textViewValue.setText(set.getString());
                textView2.setVisibility(View.GONE);
                if (keyWords.contains(set.getName())) {
                    textViewKey.setTextColor(Color.RED);
                } else if (ignoreWords.contains(set.getName())) {
                    textViewKey.setTextColor(getResources().getColor(R.color.range_overlay, null));
                    textViewValue.setTextColor(getResources().getColor(R.color.range_overlay, null));
//                    convertView.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                _Utils.printException(SettingActivity.this, e);
            }
            return convertView;
        }

        @Override
        public void notifyDataSetChanged() {

            List<Setting> ignoreSettings = new ArrayList<>();

            if (settings != null) {
                Iterator<Setting> iterator = settings.iterator();
                while (iterator.hasNext()) {
                    Setting setting = iterator.next();
                    if (ignoreWords.contains(setting.getName())) {
                        ignoreSettings.add(setting);
                        iterator.remove();
                    }
                }
            }
            settings.addAll(ignoreSettings);
            super.notifyDataSetChanged();
        }
    }

    protected class RunlogListdAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return runLogs.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final int index = position;
            try {
                convertView = View.inflate(SettingActivity.this, R.layout.inflate_runlog, null);
                final RunLog log = runLogs.get(position);
                TextView textView1 = (TextView) convertView.findViewById(R.id.textView_dateTime);
                TextView textView11 = (TextView) convertView.findViewById(R.id.textView11);
                TextView textView2 = (TextView) convertView.findViewById(R.id.textView_body);
                textView1.setText(log.getItem());
                textView11.setText(log.getTag());
                textView2.setText(log.getMessage());
                if (log.getItem() != null && log.getItem().isEmpty())
                    textView1.setVisibility(View.GONE);
                if (log.getTag() != null && log.getTag().isEmpty())
                    textView11.setVisibility(View.GONE);
                if (log.getMessage() != null && log.getMessage().isEmpty())
                    textView2.setVisibility(View.GONE);
            } catch (Exception e) {
                _Utils.printException(SettingActivity.this, e);
            }
            return convertView;
        }
    }
}
