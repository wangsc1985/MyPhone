package com.wang17.myphone.receiver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.wang17.myphone.dao.DataContext;
import com.wang17.myphone.util._LogUtils;
import com.wang17.myphone.util._Utils;



public class SystemBroadcaseReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())){
                Log.e("wangsc", "网络状态更改了");
                _LogUtils.log2file("网络状态更改了","");


            }else if("android.intent.action.PHONE_STATE".equals(intent.getAction())){
                //如果是来电
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
                DataContext dataContext = new DataContext(context);
                switch (tm.getCallState()) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:// 按拨打按钮电话时。

/*                if (dataContext.getSetting(Setting.KEYS.music_isplaying) != null) {
                    dataContext.editSetting(Setting.KEYS.music_waiting, "");
                    context.stopService(new Intent(context, MusicPlayerService.class));
                }*/
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:// 按挂断按钮时
                        try {
//                if(dataContext.getSetting(Setting.KEYS.is_didi_order_running)!=null){
//                    Intent it = new Intent(Intent.ACTION_MAIN);
//                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    ComponentName cn = new ComponentName("com.sdu.didi.gsui", "com.sdu.didi.gsui.orderflow.orderrunning.OrderServingActivity");
//                    ComponentName cn = new ComponentName("com.sdu.didi.gsui", "com.sdu.didi.gsui.main.MainActivity");
//                    it.setComponent(cn);
//                    context.startActivity(it);
//                }
                        } catch (Exception e) {
                            dataContext.addRunLog("启动滴滴异常",e.getMessage());
                        }

/*                if (dataContext.getSetting(Setting.KEYS.music_waiting) != null) {
                    context.startService(new Intent(context, MusicPlayerService.class));
                    dataContext.clearSetting(Setting.KEYS.music_waiting);
                }*/
                        break;
                }


            }else if("android.intent.action.PHONE_STATE".equals(intent.getAction())){

            }


        } catch (Exception e) {
            _Utils.printException(context,e);
        }
    }
}
