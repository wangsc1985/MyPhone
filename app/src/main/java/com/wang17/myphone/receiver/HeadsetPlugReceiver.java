package com.wang17.myphone.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.database.TallyRecord;
import com.wang17.myphone.database.DataContext;
import com.wang17.myphone.database.Setting;
import com.wang17.myphone.util._LogUtils;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.util._Utils;

import static android.content.Context.ALARM_SERVICE;
import static com.wang17.myphone.receiver.AlarmReceiver.ALARM_NIANFO_BEFORE_OVER;
import static com.wang17.myphone.receiver.AlarmReceiver.ALARM_NIANFO_OVER;

public class HeadsetPlugReceiver extends BroadcastReceiver {
    private DataContext dataContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        dataContext = new DataContext(context);
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        switch (action) {
            case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (BluetoothProfile.STATE_DISCONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET)) {
                    _LogUtils.log2file( "蓝牙断开");
//                    if (dataContext.getSetting(Setting.KEYS.tally_music_is_playing, false).getBoolean()) {
//                        context.stopService(new Intent(context, NianfoMusicService.class));// 停止服务
//                    }
//
//                    if (dataContext.getSetting(Setting.KEYS.tally_endInMillis) != null) {
//                        stopAlarm(context);
//                        pauseNianfoTally(context);
//
//                        EventBus.getDefault().post(new NianfoPauseEvent());
//                    }
                } else if (BluetoothProfile.STATE_CONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET)) {
                    _LogUtils.log2file( "蓝牙连接");
                }
                break;
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                _LogUtils.log2file( "蓝牙设备:" + device.getName() + "已链接");
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                _LogUtils.log2file( "蓝牙设备:" + device.getName() + "已断开");
                break;
//            case Intent.ACTION_HEADSET_PLUG:
//                if (intent.hasExtra("state")) {
//                    switch (intent.getIntExtra("state", 0)){
//                        case 0:
//                            // 拔出耳机。
//                            _Utils.addRunLog2File(context, "拔出耳机");
//
//                            if (dataContext.getSetting(Setting.KEYS.tally_music_is_playing, false).getBoolean()) {
//                                context.stopService(new Intent(context, NianfoMusicService.class));// 停止服务
//                            }
//
//                            if (dataContext.getSetting(Setting.KEYS.tally_endInMillis) != null) {
//                                stopAlarm(context);
//                                pauseNianfoTally(context);
//
//                                EventBus.getDefault().post(new NianfoPauseEvent());
//                            }
//                            break;
//                        case 1:
//                            // 插入耳机。
//                            _Utils.addRunLog2File(context, "插入耳机");
//                            break;
//                    }
//                }
//                break;

        }
//        if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
//        } else if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
//        }
    }


    public void pauseNianfoTally(Context context) {
        try {

            // 向数据库中保存“已经完成的时间”
            long now = System.currentTimeMillis();
            long sectionStartInMillis = Long.parseLong(dataContext.getSetting(Setting.KEYS.tally_sectionStartMillis).getString());
            long endTimeInMillis = Long.parseLong(dataContext.getSetting(Setting.KEYS.tally_endInMillis).getString());
            dataContext.deleteSetting(Setting.KEYS.tally_sectionStartMillis);
            long sectionIntervalInMillis = now - sectionStartInMillis;
            if (sectionIntervalInMillis >= 60000) {
                this.saveSection(context, sectionStartInMillis, sectionIntervalInMillis);
            }

            // 向数据库中保存“剩余时间”
            dataContext.deleteSetting(Setting.KEYS.tally_endInMillis);
            long remainIntervalInMillis = endTimeInMillis - now;
            if (sectionIntervalInMillis < 60000) {
                remainIntervalInMillis = endTimeInMillis - sectionStartInMillis;
            }
            dataContext.addSetting(Setting.KEYS.tally_intervalInMillis, remainIntervalInMillis);

        } catch (NumberFormatException e) {
            _Utils.printException(context, e);
        }

    }

    private void saveSection(Context context, long partStartMillis, long partSpanMillis) {
        try {
            TallyRecord tallyRecord = new TallyRecord(new DateTime(partStartMillis), (int) partSpanMillis, dataContext.getSetting(Setting.KEYS.tally_record_item_text, "").getString());
            if (dataContext.getRecord(partStartMillis) == null)
                dataContext.addRecord(tallyRecord);
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void stopAlarm(Context context) {
        try {
            Intent intent = new Intent(_Session.NIAN_FO_TIMER);
            PendingIntent pi = PendingIntent.getBroadcast(context, ALARM_NIANFO_OVER, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            am.cancel(pi);
            pi = PendingIntent.getBroadcast(context, ALARM_NIANFO_BEFORE_OVER, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.cancel(pi);

        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
}
