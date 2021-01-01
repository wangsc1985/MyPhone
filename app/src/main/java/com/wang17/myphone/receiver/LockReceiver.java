package com.wang17.myphone.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LockReceiver extends DeviceAdminReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.e("wangsc","onreceiver");
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        Log.e("wangsc","激活使用");
        super.onEnabled(context, intent);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Log.e("wangsc","取消激活");
        super.onDisabled(context, intent);
    }
}
