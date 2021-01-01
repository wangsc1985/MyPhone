package com.wang17.myphone.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wang17.myphone.util._LogUtils;

public class BootBroadcastReceiver extends BroadcastReceiver {

    private static final String ACTION_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN";
    private static final String ACTION_BOOT = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_BOOT)) {
            _LogUtils.log2file("开机");
        } else if (intent.getAction().equals(ACTION_SHUTDOWN)) {
            _LogUtils.log2file("关机");
        }
    }
}
