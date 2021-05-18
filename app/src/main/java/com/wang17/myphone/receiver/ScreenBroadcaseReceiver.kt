package com.wang17.myphone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import com.wang17.myphone.R
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.database.Setting
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.util.AMapUtil
import com.wang17.myphone.util._CloudUtils
import com.wang17.myphone.util._CloudUtils.addLocation
import com.wang17.myphone.util._Session
import com.wang17.myphone.util._SoundUtils
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

                        Thread {
                            /**
                             * 新消息提醒
                             */
                            _CloudUtils.getNewMsg(context, object : CloudCallback {
                                override fun excute(code: Int, msg: Any?) {
                                    when (code) {
                                        0->{
                                            dc.deleteSetting(Setting.KEYS.wx_new_msg)
                                        }
                                        1 -> {
                                            dc.editSetting(Setting.KEYS.wx_new_msg, msg.toString())
                                            _SoundUtils.play(context, R.raw.ding)
                                        }
                                        else->{

                                        }
                                    }
                                    context.sendBroadcast(Intent(MyWidgetProvider.ACTION_CLICK_TEXT))
                                }
                            })
                        }.start()
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