package com.wang17.myphone.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import com.wang17.myphone.R
import com.wang17.myphone.dao.DataContext
import com.wang17.myphone.database.Setting
import com.wang17.myphone.formatCNY
import com.wang17.myphone.util._Utils
import com.wang17.myphone.util._Utils.hideBottomUIMenu
import com.wang17.myphone.util._Utils.printException
import kotlinx.android.synthetic.main.activity_nianfo_dark_run.*
import java.util.*
import kotlin.jvm.Throws

class NianfoDarkRunActivity : AppCompatActivity() {
    private lateinit var dc: DataContext
    private lateinit var mainReciver: MainReciver
    private var preBatteryTime: Long = 0
    private  var muyu_period=1000L

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /**
         * 设置全屏的俩段代码必须在setContentView(R.layout.main) 之前，不然会报错。
         */
        //无title
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        //全屏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_nianfo_dark_run)
        dc = DataContext(this)
        startTime = System.currentTimeMillis()
        /**
         * 初始化电量记录
         */
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        dc.editSetting(Setting.KEYS.battery, battery)
        muyu_period = intent.getIntExtra("period",1000).toLong()
        /**
         * 监听
         */
        mainReciver = MainReciver()
        val filter = IntentFilter()
        // 监控开关屏
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        // 监控电量变化
        filter.addAction(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(mainReciver, filter)
    }

    override fun onResume() {
        super.onResume()
        /**
         * 隐藏虚拟按键
         */
        hideBottomUIMenu(this)
        startTimer()
    }

    override fun onDestroy() {
        super.onDestroy()

        // 解除监控开关屏
        if (mainReciver != null) unregisterReceiver(mainReciver)
        stopTimer()
    }

    private fun stopTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer!!.purge()
            timer = null
        }
    }

    private var timer: Timer? = null
    private var startTime = 0L
    private var count=0
    private fun startTimer() {
        if (timer != null) {
            return
        }
        try {
            timer = Timer()
            timer?.schedule(object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        reflushBatteryNumber()
                        _Utils.zhendong(this@NianfoDarkRunActivity,40)
                        var totalTime = (System.currentTimeMillis()-startTime)/1000
                        val minite = totalTime / 60
                        val second = totalTime % 60
                        val secondStr = if (second < 10) "0$second" else second.toString() + ""
                        count++
                        tv_duration.text = (if (minite > 0) minite.toString() + "分" else "") + (secondStr + "秒")
                        tv_count.text = "${(count/2).formatCNY()}"
                        if(count>=1080){
                            _Utils.zhendong(this@NianfoDarkRunActivity,1000)
                            finish()
                        }
                    }
                }
            }, 1000, muyu_period)
        } catch (e: Exception) {
            printException(this, e)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun reflushBatteryNumber() {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        textView_battery!!.text = "$battery%"
    }

    @Throws(Exception::class)
    fun durationToTimeString(duration: Long): String {
        val second = duration % 60000 / 1000
        val miniteT = duration / 60000
        val minite = miniteT % 60
        val hour = miniteT / 60
        return "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}"
    }

    inner class MainReciver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra("level", 0)
                    val dataLevel = dc.getSetting(Setting.KEYS.battery, 0).int
                    if (dataLevel != level) {
                        val span = System.currentTimeMillis() - if (preBatteryTime == 0L) System.currentTimeMillis() else preBatteryTime
                        preBatteryTime = System.currentTimeMillis()
                        dc.editSetting(Setting.KEYS.battery, level)
                        textView_battery!!.text = "$level%"
                        tv_span.text = "${durationToTimeString(span)}"
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        try {
            val audio = getSystemService(AUDIO_SERVICE) as AudioManager
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    audio.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    audio.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    return true
                }
                else -> {
                }
            }
        } catch (e: Exception) {
            printException(this@NianfoDarkRunActivity, e)
        }
        return super.onKeyDown(keyCode, event)
    }
}