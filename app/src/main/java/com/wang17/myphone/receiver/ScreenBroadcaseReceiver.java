package com.wang17.myphone.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.amap.api.services.route.DistanceItem;
import com.amap.api.services.route.DistanceResult;
import com.wang17.myphone.R;
import com.wang17.myphone.callback.CloudCallback;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.model.database.Location;
import com.wang17.myphone.model.database.Setting;
import com.wang17.myphone.util.AMapUtil;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._LogUtils;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.util._CloudUtils;
import com.wang17.myphone.util._SoundUtils;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.widget.MyWidgetProvider;

import java.util.Calendar;
import java.util.List;

public class ScreenBroadcaseReceiver extends BroadcastReceiver {

    private static long preDateTime;
    private static final String _TAG = "wangsc";

    private void e(Object msg){
        Log.e("wangsc",msg.toString());
    }
    @Override
    public void onReceive(final Context context, Intent intent) {

        switch (intent.getAction()) {
            case Intent.ACTION_USER_PRESENT:

                try {
                    long now = System.currentTimeMillis();
                    if (now - preDateTime >= 10000) {
                        try {
                            /**
                             * 记录location
                             */
                            final DataContext dataContext = new DataContext(context);
                            final Location oldLocation = dataContext.getLatestLocatio(_Session.UUID_NULL);
                            if (oldLocation != null) {
                                long t1 = System.currentTimeMillis();
                                AMapUtil.getCurrentLocation(context, "屏幕解锁", new AMapUtil.LocationCallBack() {
                                    @Override
                                    public void OnLocationedListener(final Location newLocation) {
                                        long t2 = System.currentTimeMillis();
                                        dataContext.addLog("ScreenBroadcase-Span","获取位置用时："+(t2-t1)+"毫秒","");
                                        AMapUtil.getDistants(context, oldLocation, newLocation, new AMapUtil.DistanceSearchCallback() {
                                            @Override
                                            public void OnDistanceSearchListener(DistanceResult distanceResult) {
                                                long t3 = System.currentTimeMillis();
                                                dataContext.addLog("ScreenBroadcase-Span","获取位置距离用时："+(t3-t2)+"毫秒","");
                                                List<DistanceItem> distanceItems = distanceResult.getDistanceResults();
                                                float distance = 0;
                                                for (DistanceItem item : distanceItems) {
                                                    distance += item.getDistance();
                                                }
                                                DateTime oldDate = new DateTime(oldLocation.Time);
                                                DateTime newDate = new DateTime(newLocation.Time);
                                                Log.e("wangsc", "distance: " + distance + " , oldDay: " + oldDate.getDay() + " , newDay: " + newDate.getDay());

    //                                        if (distance > 50 || oldDate.getDay() != newDate.getDay())
                                                dataContext.addLocation(newLocation);

                                                // 记录到云数据库
                                                String pwd = dataContext.getSetting(Setting.KEYS.wx_request_code,"0000").getString();
                                                long t4 = System.currentTimeMillis();
                                                _CloudUtils.addLocation(context, pwd, newLocation.Latitude, newLocation.Longitude, newLocation.Address, new CloudCallback() {
                                                    @Override
                                                    public void excute(int code, Object result) {
                                                        long t5 = System.currentTimeMillis();
                                                        dataContext.addLog("ScreenBroadcase-Span","云添加位置用时："+(t5-t4)+"毫秒","");
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }

                            if(dataContext.getSetting(Setting.KEYS.bank_balance,-1).getDouble()>3000){
                                _Utils.zhendong2(context);
                                _SoundUtils.play(context, R.raw.maopao);
                            }
                            /**
                             * 刷新小部件期股信息
                             */
                            DateTime time = new DateTime();
                            // 9:00 - 11:30     13:30 - 15:00
                            DateTime time1 = new DateTime(9, 0);
                            DateTime time2 = new DateTime(11, 30);
                            DateTime time3 = new DateTime(13, 30);
                            DateTime time4 = new DateTime(15, 0);
                            DateTime time5 = new DateTime(21, 0);
                            DateTime time6 = new DateTime(23, 0);

                            boolean is_widget_list_stock=dataContext.getSetting(Setting.KEYS.is_widget_list_stock,false).getBoolean();
                            if(is_widget_list_stock==true){
                                time1 = new DateTime(9, 30);
                                time3 = new DateTime(13, 0);
                            }

                            int day = time.get(Calendar.DAY_OF_WEEK);

                            if (dataContext.getSetting(Setting.KEYS.is_flush_widget_when_screen_on, false).getBoolean() == false)
                                return;
                            if (day == 1 || day == 7) return;

                            /**
                             * 或者小于time1，或者大于time6，或者大于time4小于time5，或者大于time2小于time3。直接返回。
                             */
                            if(is_widget_list_stock){
                                if (time.getTimeInMillis() < time1.getTimeInMillis()
                                        || (time.getTimeInMillis() > time4.getTimeInMillis())
                                        || (time.getTimeInMillis() > time2.getTimeInMillis()&& time.getTimeInMillis() < time3.getTimeInMillis()))
                                    return;
                            }else{
                                if (time.getTimeInMillis() < time1.getTimeInMillis()
                                        || time.getTimeInMillis()>time6.getTimeInMillis()
                                        || (time.getTimeInMillis() > time4.getTimeInMillis()&&time.getTimeInMillis()<time5.getTimeInMillis())
                                        || (time.getTimeInMillis() > time2.getTimeInMillis()
                                        && time.getTimeInMillis() < time3.getTimeInMillis()))
                                    return;
                            }

                            context.sendBroadcast(new Intent(MyWidgetProvider.ACTION_CLICK_LAYOUT_LEFT));



                        }catch (Exception e){
                            new DataContext(context).addLog("err","运行错误",e.getMessage());
                        }
                        finally {
                            preDateTime = now;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


                break;
            case Intent.ACTION_SCREEN_ON:
                Log.e(_TAG, "屏幕开启");
                /**
                 * 必须是动态注册才管用。
                 */
                break;
            case Intent.ACTION_SCREEN_OFF:
                Log.e(_TAG, "屏幕关闭");
                /**
                 * 必须是动态注册才管用。
                 */
                break;
        }
    }
}
