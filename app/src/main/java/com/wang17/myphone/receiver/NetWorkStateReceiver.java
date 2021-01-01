package com.wang17.myphone.receiver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.util.Log;

import com.wang17.myphone.util.AMapUtil;
import com.wang17.myphone.util._Utils;

/**
 * Created by Carson_Ho on 16/10/31.
 */
public class NetWorkStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            //检测API是不是小于23，因为到了API23之后getNetworkInfo(int networkType)方法被弃用
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo wifiNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                NetworkInfo dataNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                if(wifiNetworkInfo.isConnected() ){
                    Log.e("wangsc","WIFI连接，记录定位。");
                    AMapUtil.getCurrentLocation(context,"WIFI连接");
                }else if(!wifiNetworkInfo.isConnected() ){
                    Log.e("wangsc","WIFI断开，记录定位。");
                    AMapUtil.getCurrentLocation(context,"WIFI断开");
                }
    //            if (wifiNetworkInfo.isConnected() && dataNetworkInfo.isConnected()) {
    //                Toast.makeText(context, "WIFI已连接,移动数据已连接", Toast.LENGTH_LONG).show();
    //            } else if (wifiNetworkInfo.isConnected() && !dataNetworkInfo.isConnected()) {
    //                Toast.makeText(context, "WIFI已连接,移动数据已断开", Toast.LENGTH_LONG).show();
    //            } else if (!wifiNetworkInfo.isConnected() && dataNetworkInfo.isConnected()) {
    //                Toast.makeText(context, "WIFI已断开,移动数据已连接", Toast.LENGTH_LONG).show();
    //            } else {
    //                Toast.makeText(context, "WIFI已断开,移动数据已断开", Toast.LENGTH_LONG).show();
    //            }
            }else {

                ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                Network[] networks = connMgr.getAllNetworks();
                //用于存放网络连接信息
                StringBuilder sb = new StringBuilder();
                //通过循环将网络信息逐个取出来
                for (Network network : networks){
                    //获取ConnectivityManager对象对应的NetworkInfo对象
                    NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
                    if(networkInfo.getState()== NetworkInfo.State.CONNECTED){
                        Log.e("wangsc",networkInfo.getSubtypeName()+"连接成功。");
                        AMapUtil.getCurrentLocation(context,networkInfo.getSubtypeName()+"连接成功。");
                    }
                }
            }
        } catch (Exception e) {
            _Utils.printException(context,e);
        }
    }
}