package com.wang17.myphone.receiver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._LogUtils;
import com.wang17.myphone.model.DateTime;

public class PhoneInReceiver extends BroadcastReceiver {

    private String number;
    private DataContext dataContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        //如果是来电
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
        dataContext = new DataContext(context);

        switch (tm.getCallState()) {
            case TelephonyManager.CALL_STATE_RINGING:
                // 响铃时
                number = intent.getStringExtra("incoming_number");
                dataContext.addLog("来电广播", "来电", "时间：" + new DateTime().toLongDateTimeString());
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                // 按接听按钮时
                dataContext.addLog("来电广播", "接听", "时间：" + new DateTime().toLongDateTimeString());
                break;

            case TelephonyManager.CALL_STATE_IDLE:
                // 按挂断按钮时
                dataContext.addLog("来电广播", "挂断", "时间：" + new DateTime().toLongDateTimeString());
                break;
        }
    }

}
