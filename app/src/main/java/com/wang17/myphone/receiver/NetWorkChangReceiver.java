package com.wang17.myphone.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.wang17.myphone.util.AMapUtil;
import com.wang17.myphone.util._LogUtils;
import com.wang17.myphone.util._Utils;

public class NetWorkChangReceiver extends BroadcastReceiver {
    /**
     * 获取连接类型
     *
     * @param type
     * @return
     */
    private String getConnectionType(int type) {
        String connType = "";
        if (type == ConnectivityManager.TYPE_MOBILE) {
            connType = "3G网络数据";
        } else if (type == ConnectivityManager.TYPE_WIFI) {
            connType = "WIFI网络";
        }
        return connType;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                // 监听wifi的打开与关闭，与wifi的连接无关

                WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifiMgr.getConnectionInfo();
                String ssid = info != null ? info.getSSID() : null;

                switch (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        Log.e("wangsc", " 接入" + ssid);
                        _LogUtils.log2file("接入" + ssid);
                        AMapUtil.getCurrentLocation(context, "接入" + ssid);
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        Log.e("wangsc", "断开" + ssid);
                        _LogUtils.log2file( "断开" + ssid);
                        AMapUtil.getCurrentLocation(context, "断开" + ssid);
                        break;
                }
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        // 监听网络连接，包括wifi和移动数据的打开和关闭,以及连接上可用的连接都会接到监听
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            //获取联网状态的NetworkInfo对象
            NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (info != null) {
                //如果当前的网络连接成功并且网络连接可用
                if (NetworkInfo.State.CONNECTED == info.getState() && info.isAvailable()) {
                    if (info.getType() == ConnectivityManager.TYPE_WIFI || info.getType() == ConnectivityManager.TYPE_MOBILE) {
                        _LogUtils.log2file( getConnectionType(info.getType()) + "连上");
                    }
                } else {
                    _LogUtils.log2file( getConnectionType(info.getType()) + "断开");
                }
            }
        }
    }
}
