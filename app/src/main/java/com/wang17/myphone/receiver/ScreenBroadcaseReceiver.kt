package com.wang17.myphone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.database.Setting
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.util.AMapUtil
import com.wang17.myphone.util._CloudUtils.addLocation
import com.wang17.myphone.util._Session
import com.wang17.myphone.widget.MyWidgetProvider

class ScreenBroadcaseReceiver : BroadcastReceiver() {
    private fun e(msg: Any) {
        Log.e("wangsc", msg.toString())
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_USER_PRESENT -> try {
                val now = System.currentTimeMillis()
                if (now - preDateTime >= 60000) {
                    try {
                        /**
                         * 记录location
                         */
                        val dc = DataContext(context)
                        val oldLocation = dc.getLatestLocatio(_Session.UUID_NULL)
                        if (oldLocation != null) {
                            val t1 = System.currentTimeMillis()
                            AMapUtil.getCurrentLocation(context, "屏幕解锁") { newLocation ->
                                val t2 = System.currentTimeMillis()
                                val distance = AMapUtils.calculateLineDistance(LatLng(oldLocation.Latitude, oldLocation.Longitude), LatLng(newLocation.Latitude, newLocation.Longitude))
                                val oldDate = DateTime(oldLocation.Time)
                                val newDate = DateTime(newLocation.Time)
                                Log.e("wangsc", "speed: ${newLocation.Speed} , distance: ${distance} , oldDay: ${oldDate.day} , newDay: ${newDate.day}")
                                if (distance > 50 || oldDate.day != newDate.day) {
                                    dc.addLocation(newLocation)

                                    // 记录到云数据库
                                    val pwd = dc.getSetting(Setting.KEYS.wx_request_code, "0088").string
                                    val t4 = System.currentTimeMillis()
                                    addLocation(context, pwd,newLocation.Speed, newLocation.Latitude, newLocation.Longitude, newLocation.Address) { code, result ->
                                        val t5 = System.currentTimeMillis()
                                    }
                                }
                            }
                        }
                        //endregion
                        /**
                         * 刷新小部件期股信息
                         */
//                            DateTime time = new DateTime();
//                            // 9:00 - 11:30     13:30 - 15:00
//                            DateTime time1 = new DateTime(9, 0);
//                            DateTime time2 = new DateTime(11, 30);
//                            DateTime time3 = new DateTime(13, 30);
//                            DateTime time4 = new DateTime(15, 0);
//                            DateTime time5 = new DateTime(21, 0);
//                            DateTime time6 = new DateTime(23, 0);
//
//                            boolean is_widget_list_stock=dataContext.getSetting(Setting.KEYS.is_widget_list_stock,false).getBoolean();
//                            if(is_widget_list_stock==true){
//                                time1 = new DateTime(9, 30);
//                                time3 = new DateTime(13, 0);
//                            }
//
//                            int day = time.get(Calendar.DAY_OF_WEEK);
//
//                            if (dataContext.getSetting(Setting.KEYS.is_flush_widget_when_screen_on, false).getBoolean() == false)
//                                return;
//                            if (day == 1 || day == 7) return;
//
//                            /**
//                             * 或者小于time1，或者大于time6，或者大于time4小于time5，或者大于time2小于time3。直接返回。
//                             */
//                            if(is_widget_list_stock){
//                                if (time.getTimeInMillis() < time1.getTimeInMillis()
//                                        || (time.getTimeInMillis() > time4.getTimeInMillis())
//                                        || (time.getTimeInMillis() > time2.getTimeInMillis()&& time.getTimeInMillis() < time3.getTimeInMillis()))
//                                    return;
//                            }else{
//                                if (time.getTimeInMillis() < time1.getTimeInMillis()
//                                        || time.getTimeInMillis()>time6.getTimeInMillis()
//                                        || (time.getTimeInMillis() > time4.getTimeInMillis()&&time.getTimeInMillis()<time5.getTimeInMillis())
//                                        || (time.getTimeInMillis() > time2.getTimeInMillis()
//                                        && time.getTimeInMillis() < time3.getTimeInMillis()))
//                                    return;
//                            }
                        context.sendBroadcast(Intent(MyWidgetProvider.ACTION_CLICK_TEXT))
                    } catch (e: Exception) {
                        DataContext(context).addRunLog("err", "运行错误", e.message)
                    } finally {
                        preDateTime = now
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Intent.ACTION_SCREEN_ON -> Log.e(_TAG, "屏幕开启")
            Intent.ACTION_SCREEN_OFF -> Log.e(_TAG, "屏幕关闭")
        }
    }

    companion object {
        private var preDateTime: Long = 0
        private const val _TAG = "wangsc"
    }
}