package com.wang17.myphone.fragment;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONObject;

import com.wang17.myphone.R;
import com.wang17.myphone.event.LocationGearEvent;
import com.wang17.myphone.event.LocationIsAutomaticEvent;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.database.Location;
import com.wang17.myphone.database.Setting;
import com.wang17.myphone.service.NianfoMusicService;
import com.wang17.myphone.database.DataContext;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.util._Utils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFragment extends Fragment {

    // 视图变量
    private ListView listViewSetting;
    private SeekBar seekBarHeadset;
    private Spinner spinnerMusicNames, spinnerLocationGear;
    // 值变量
    private DataContext mDataContext;
    private List<Setting> settings;
    private SettingListdAdapter setAdapter;
    private Handler uiHandler;

    private void fillSpinner(Spinner spinner, List<String> values) {
        ArrayAdapter<String> aspn = new ArrayAdapter<>(getContext(), R.layout.inflate_spinner, values);
        aspn.setDropDownViewResource(R.layout.inflate_spinner_dropdown);
        spinner.setAdapter(aspn);
    }

    private void initMusicNames() {
        List<String> list = new ArrayList<String>();

        _Session.BUDDHA_MUSIC_NAME_ARR = _Utils.getFilesWithSuffix(_Session.ROOT_DIR.getPath(), ".mp3");
        if (_Session.BUDDHA_MUSIC_NAME_ARR.length == 0) {
            _Session.BUDDHA_MUSIC_NAME_ARR = new String[]{};
        }
        Arrays.sort(_Session.BUDDHA_MUSIC_NAME_ARR);

        for (int i = 0; i < _Session.BUDDHA_MUSIC_NAME_ARR.length; i++) {
            list.add(_Session.BUDDHA_MUSIC_NAME_ARR[i]);
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHandler = new Handler();
        mDataContext = new DataContext(getContext());
        settings = mDataContext.getSettings();
        setAdapter = new SettingListdAdapter();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            initView(view);
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        settings = mDataContext.getSettings();
        setAdapter.notifyDataSetChanged();

        initMusicNames();
        String databaseMusicName = mDataContext.getSetting(Setting.KEYS.buddha_music_name, "").getString();
        int num = spinnerMusicNames.getCount();
        for (int i = 0; i < num; i++) {
            if (spinnerMusicNames.getItemAtPosition(i).toString().equals(databaseMusicName)) {
                spinnerMusicNames.setSelection(i);
                break;
            }
        }
    }

    private Location mPrevLocation;
    List<Location> locationList;
    int index;

    private void initView(View view) {

        spinnerMusicNames = view.findViewById(R.id.spinner_musicName);
        spinnerMusicNames.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String name = spinnerMusicNames.getItemAtPosition(position).toString();
                        if (!name.equals(mDataContext.getSetting(Setting.KEYS.buddha_music_name, _Session.BUDDHA_MUSIC_NAME_ARR[0]).getString())) {
                            mDataContext.editSetting(Setting.KEYS.buddha_music_name, name);
                            if (mDataContext.getSetting(Setting.KEYS.tally_music_is_playing, false).getBoolean()) {
                                getContext().stopService(new Intent(getContext(), NianfoMusicService.class));
                                getContext().startService(new Intent(getContext(), NianfoMusicService.class));
                            }
                            settings = mDataContext.getSettings();
                            setAdapter.notifyDataSetChanged();
                            Log.e("wangsc", "setOnItemSelectedListener()事件启动。");
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                }
        );


        spinnerLocationGear = view.findViewById(R.id.spinner_locationGear);
        initLocationGear();
        spinnerLocationGear.setSelection(mDataContext.getSetting(Setting.KEYS.map_location_gear, 0).getInt());
        spinnerLocationGear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int gear = position;
                if (gear != mDataContext.getSetting(Setting.KEYS.map_location_gear, 0).getInt()) {
                    EventBus.getDefault().post(new LocationGearEvent(gear));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        listViewSetting = view.findViewById(R.id.listView_setting);
        listViewSetting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final Setting setting = settings.get(position);
                final boolean isInBlackList = ignoreWords.contains(setting.getName());
                new AlertDialog.Builder(getContext()).setItems(new String[]{"编辑", "删除", setting.getLevel() == 1 ? "取消关注" : "关注", isInBlackList ? "拉白" : "拉黑", "刷新"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                editSettingItem(position);
                                break;
                            case 1:
                                new AlertDialog.Builder(getContext()).setMessage("要删除当前设置吗？").setPositiveButton("是", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mDataContext.deleteSetting(settings.get(position).getName());
                                        settings = mDataContext.getSettings();
                                        setAdapter.notifyDataSetChanged();
                                    }
                                }).setNegativeButton("否", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        settings = mDataContext.getSettings();
                                        setAdapter.notifyDataSetChanged();
                                    }
                                }).show();
                                break;
                            case 2:
                                mDataContext.editSettingLevel(settings.get(position).getName(), setting.getLevel() == 1 ? 100 : 1);
                                settings = mDataContext.getSettings();
                                setAdapter.notifyDataSetChanged();
                                break;
                            case 3:
                                if (isInBlackList)
                                    removeFromBlackList(setting.getName());
                                else
                                    add2BlackList(setting.getName());
                                settings = mDataContext.getSettings();
                                setAdapter.notifyDataSetChanged();
                                break;
                            case 4:
                                settings = mDataContext.getSettings();
                                setAdapter.notifyDataSetChanged();
                                break;
                        }
                    }
                }).show();
            }
        });
        listViewSetting.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                editSettingItem(position);
                return true;
            }
        });
        listViewSetting.setAdapter(setAdapter);

        seekBarHeadset = (SeekBar) view.findViewById(R.id.seekBar_headset);
        int headset = mDataContext.getSetting(Setting.KEYS.headset_volume, 12).getInt();
        seekBarHeadset.setMax(15);
        seekBarHeadset.setProgress(headset);
        seekBarHeadset.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mDataContext.editSetting(Setting.KEYS.headset_volume, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void editSettingItem(int position) {
        Setting setting = settings.get(position);

        if (ignoreWords.contains(setting.getName())) {
            return;
        }
        if (setting.getString().equals("false") || setting.getString().equals("true")) {
            new AlertDialog.Builder(getContext()).setTitle(setting.getName()).setPositiveButton("是", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mDataContext.editSetting(setting.getName(), true);
                    settings = mDataContext.getSettings();
                    setAdapter.notifyDataSetChanged();

                    if (setting.getName().equals(Setting.KEYS.map_location_isAutoChangeGear.toString())) {
                        EventBus.getDefault().post(new LocationIsAutomaticEvent(true));
                    }
                }
            }).setNegativeButton("否", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mDataContext.editSetting(setting.getName(), false);
                    settings = mDataContext.getSettings();
                    setAdapter.notifyDataSetChanged();

                    if (setting.getName().equals(Setting.KEYS.map_location_isAutoChangeGear.toString())) {
                        EventBus.getDefault().post(new LocationIsAutomaticEvent(false));
                    }
                }
            }).show();
        } else {
            View v = View.inflate(getContext(), R.layout.inflate_editbox, null);
            final EditText input = v.findViewById(R.id.editText_value);
            input.setText(setting.getString());
            new AlertDialog.Builder(getContext()).setTitle(setting.getName()).setView(v).setPositiveButton("修改", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!input.getText().equals(setting.getString())) {

                        mDataContext.editSetting(setting.getName(), input.getText());
                        settings = mDataContext.getSettings();
                        setAdapter.notifyDataSetChanged();
                        try {
                            String str = mDataContext.getSetting(Setting.KEYS.media_player, "").getString();
                            String fileName = mDataContext.getSetting(Setting.KEYS.buddha_music_name, "").getString();
                            if (!str.isEmpty()) {
                                JSONArray jsonArray = new JSONArray(str);
                                JSONObject retJsonObject = null;

                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    String file = jsonObject.getString("file");
                                    if (file.equals(fileName)) {
                                        retJsonObject = jsonObject;
                                        switch (setting.getName()) {
                                            case "media_player_speed":
                                                jsonObject.put("speed", mDataContext.getSetting(Setting.KEYS.media_player_speed, "1.0").getString());
                                                break;
                                            case "media_player_pitch":
                                                jsonObject.put("pitch", mDataContext.getSetting(Setting.KEYS.media_player_pitch, "1.0").getString());
                                                break;

                                            case "media_player_volumn":
                                                jsonObject.put("volumn", mDataContext.getSetting(Setting.KEYS.media_player_volumn, "1.0").getString());
                                                break;
                                        }
                                        break;
                                    }
                                }
                                if (retJsonObject == null) {
                                    retJsonObject = new JSONObject();
                                    retJsonObject.put("file", fileName);
                                    retJsonObject.put("speed", mDataContext.getSetting(Setting.KEYS.media_player_speed, "1.0").getString());
                                    retJsonObject.put("pitch", mDataContext.getSetting(Setting.KEYS.media_player_pitch, "1.0").getString());
                                    retJsonObject.put("volumn", mDataContext.getSetting(Setting.KEYS.media_player_volumn, "1.0").getString());
                                    jsonArray.put(retJsonObject);
                                }
                                mDataContext.editSetting(Setting.KEYS.media_player, jsonArray.toString());
                            } else {
                                JSONArray jsonArray = new JSONArray();
                                JSONObject retJsonObject = new JSONObject();
                                retJsonObject.put("file", fileName);
                                retJsonObject.put("speed", mDataContext.getSetting(Setting.KEYS.media_player_speed, "1.0").getString());
                                retJsonObject.put("pitch", mDataContext.getSetting(Setting.KEYS.media_player_pitch, "1.0").getString());
                                retJsonObject.put("volumn", mDataContext.getSetting(Setting.KEYS.media_player_volumn, "1.0").getString());
                                jsonArray.put(retJsonObject);
                                mDataContext.editSetting(Setting.KEYS.media_player, jsonArray.toString());
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (mDataContext.getSetting(Setting.KEYS.tally_music_is_playing, false).getBoolean() == true) {
                            getActivity().stopService(new Intent(getContext(), NianfoMusicService.class));
                            getActivity().startService(new Intent(getContext(), NianfoMusicService.class));
                        }
                    }
                }
            }).show();
        }
    }


    private void parseSms() {
        Cursor cursor = null;
        // 添加异常捕捉
        try {
            cursor = this.getContext().getContentResolver().query(
                    Uri.parse("content://sms/inbox"),
                    new String[]{"_id", "address", "read", "body", "date"},
                    null, null, "date desc"); // datephone想要的短信号码
            if (cursor != null) { // 当接受到的新短信与想要的短信做相应判断
                String body = "";
                while (cursor.moveToNext()) {
                    body = cursor.getString(cursor.getColumnIndex("body"));// 在这里获取短信信息
                    long smsdate = Long.parseLong(cursor.getString(cursor.getColumnIndex("date")));
                    long nowdate = System.currentTimeMillis();
                    // 如果当前时间和短信时间间隔超过60秒,认为这条短信无效

                    // 规则  交行信用卡-账单  短信
                    Pattern pattern = Pattern.compile("(截至).+(，您尾号).+(信用卡).+(账单剩余应还款额).+(，在).+(前归还剩余最低还款额).+(不影响信用记录，如缴清勿理会。还款请点击买单吧APP cc.bankcomm.com/kfd02c ！【交通银行】)");
                    Matcher matcher = pattern.matcher(body);
                    if (matcher.find()) {
                        pattern = Pattern.compile("\\d+\\.*\\d*");
                        matcher = pattern.matcher(body);

                        List<String> group = new ArrayList<>();
                        while (matcher.find()) {
                            group.add(matcher.group());
                        }

                        String cardNumber = group.get(2);
                        double money = Double.parseDouble(group.get(7));
                        int month = Integer.parseInt(group.get(8));
                        int day = Integer.parseInt(group.get(9));

                        DateTime billDate = new DateTime(new DateTime(smsdate).getYear(), month - 1, day);
                        if (billDate.getTimeInMillis() - smsdate < 0) {
                            billDate.set(Calendar.YEAR, billDate.getYear() + 1);
                        }

//                        BankToDo bankToDo = new BankToDo(billDate, "交行" + cardNumber, money);

                        continue;
                    }


                    // 规则  交行信用卡-还款  短信


                    // 规则  浦发信用卡-账单  短信
                    // 规则  浦发信用卡-还款  短信

                    // 规则  工行信用卡-账单  短信
                    // 规则  工行信用卡-还款  短信

                    // 规则  微粒贷-账单  短信
                    // 规则  微粒贷-还款  短信

                    // 规则  招联-账单  短信
                    // 规则  招联-还款  短信

                    // 规则  借呗-账单  短信
                    // 规则  借呗-还款  短信

                    // 规则  花呗-账单  短信
                    // 规则  花呗-还款  短信

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
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
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivity(intent);
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

    private List<String> getBlackList() {
        String blackList = mDataContext.getSetting(Setting.KEYS.black_list, "black_list,").getString();
        String[] list = blackList.split(",");

        List<String> result = new ArrayList<>();
        for (int i = 0; i < list.length; i++) {
            result.add(list[i]);
        }
        return result;
    }

    private void add2BlackList(String key) {
        key += ",";
        String blackList = mDataContext.getSetting(Setting.KEYS.black_list, "black_list,").getString();
        if (!blackList.contains(key)) {
            mDataContext.editSetting(Setting.KEYS.black_list, blackList + key);
        }
    }

    private void removeFromBlackList(String key) {
        key += ",";
        String blackList = mDataContext.getSetting(Setting.KEYS.black_list, "black_list,").getString();
        if (blackList.contains(key)) {
            mDataContext.editSetting(Setting.KEYS.black_list, blackList.replace(key, ""));
        }
    }


    private List<String> keyWords;
    private List<String> ignoreWords;

    protected class SettingListdAdapter extends BaseAdapter {


        public SettingListdAdapter() {

            keyWords = new ArrayList<>();

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

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final int index = position;
            try {
                convertView = View.inflate(getContext(), R.layout.inflate_setting, null);
                final Setting set = settings.get(position);
                LinearLayout root = convertView.findViewById(R.id.layout_root);
                TextView textViewKey = convertView.findViewById(R.id.tv_dateTime);
                TextView textViewValue = convertView.findViewById(R.id.textView11);
                TextView textView2 = convertView.findViewById(R.id.tv_body);
                textViewKey.setText(set.getName());
                textViewValue.setText(set.getString());
                textView2.setVisibility(View.GONE);
                if (keyWords.contains(set.getName())) {
                    textViewKey.setTextColor(Color.RED);
                } else if (ignoreWords.contains(set.getName())) {
                    textViewKey.setTextColor(ContextCompat.getColor(getContext(), R.color.color_gray));
                    textViewValue.setTextColor(ContextCompat.getColor(getContext(), R.color.color_gray));
                }
                if (set.getLevel() == 1) {
                    root.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.range_overlay));
                }

                if (set.getName().equals(Setting.KEYS.media_player_pitch.toString())) {

                    String str = mDataContext.getSetting(Setting.KEYS.media_player, "").getString();
                    String fileName = mDataContext.getSetting(Setting.KEYS.buddha_music_name, "").getString();

                    if (!str.isEmpty()) {
                        JSONArray jsonArray = new JSONArray(str);

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String file = jsonObject.getString("file");
                            if (file.equals(fileName)) {
                                textViewValue.setText(jsonObject.getString("pitch"));
                                set.setValue(jsonObject.getString("pitch"));
                                break;
                            }
                        }
                    }
                } else if (set.getName().equals(Setting.KEYS.media_player_volumn.toString())) {

                    String str = mDataContext.getSetting(Setting.KEYS.media_player, "").getString();
                    String fileName = mDataContext.getSetting(Setting.KEYS.buddha_music_name, "").getString();

                    if (!str.isEmpty()) {
                        JSONArray jsonArray = new JSONArray(str);

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String file = jsonObject.getString("file");
                            if (file.equals(fileName)) {
                                textViewValue.setText(jsonObject.getString("volumn"));
                                set.setValue(jsonObject.getString("volumn"));
                                break;
                            }
                        }
                    }
                } else if (set.getName().equals(Setting.KEYS.media_player_speed.toString())) {

                    String str = mDataContext.getSetting(Setting.KEYS.media_player, "").getString();
                    String fileName = mDataContext.getSetting(Setting.KEYS.buddha_music_name, "").getString();

                    if (!str.isEmpty()) {
                        JSONArray jsonArray = new JSONArray(str);

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String file = jsonObject.getString("file");
                            if (file.equals(fileName)) {
                                textViewValue.setText(jsonObject.getString("speed"));
                                set.setValue(jsonObject.getString("speed"));
                                break;
                            }
                        }
                    }
                }

            } catch (Exception e) {
                _Utils.printException(getContext(), e);
            }
            return convertView;
        }

        @Override
        public void notifyDataSetChanged() {
            ignoreWords = getBlackList();
//
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
}
