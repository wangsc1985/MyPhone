package com.wang17.myphone.receiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wang17.myphone.database.DataContext;
import com.wang17.myphone.util._LogUtils;

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

}
