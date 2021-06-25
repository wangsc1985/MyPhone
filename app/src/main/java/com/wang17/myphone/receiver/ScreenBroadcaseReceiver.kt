package com.wang17.myphone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.dao.DataContext
import com.wang17.myphone.database.Setting
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.service.BuddhaService
import com.wang17.myphone.util.*
import com.wang17.myphone.util._CloudUtils.addLocation
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
                                    addLocation(context, newLocation.Speed, newLocation.Latitude, newLocation.Longitude, newLocation.Address) { code, result ->
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
                                        0 -> {
                                            dc.deleteSetting(Setting.KEYS.wx_new_msg)
                                        }
                                        1 -> {
                                            dc.editSetting(Setting.KEYS.wx_new_msg, msg.toString())
                                        }
                                        else -> {

                                        }
                                    }
                                    context.sendBroadcast(Intent(MyWidgetProvider.ACTION_CLICK_TEXT))
                                }
                            })
                        }.start()

                        /**
                         * 启动buddhaservice
                         */
                        if (dc.getSetting(Setting.KEYS.is保持念佛服务, true).boolean) {
                            if (!_Utils.isServiceRunning(context, BuddhaService::class.qualifiedName!!)) {
                                dc.addRunLog("解锁","启动念佛服务","")
                                dc.editSetting(Setting.KEYS.buddha_service_running, true)
                                context.startService(Intent(context, BuddhaService::class.java))
                            }
                        }
                    } catch (e: Exception) {
                        _Utils.printException(context,e)
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