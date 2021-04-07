package com.wang17.myphone.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.AudioManager
import android.media.SoundPool
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import com.wang17.myphone.R
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.model.StockInfo
import com.wang17.myphone.database.Position
import com.wang17.myphone.database.Setting
import com.wang17.myphone.util._SinaStockUtils.OnLoadStockInfoListListener
import com.wang17.myphone.util._SinaStockUtils.getStockInfoList
import com.wang17.myphone.util._Utils
import kotlinx.android.synthetic.main.activity_fund_monitor.*
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.*

class FundMonitorActivity : AppCompatActivity() {
    private lateinit var positions: List<Position>
    private lateinit var mDataContext: DataContext
    private var preClickTime: Long = 0
    private lateinit var mainReciver: MainReciver
    private var isSoundLoaded = false
    private lateinit var mSoundPool: SoundPool
    private var soundId = 0
    private var preBatteryTime: Long = 0

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
        setContentView(R.layout.activity_fund_monitor)
        mDataContext = DataContext(this)
        preClickTime = System.currentTimeMillis()
        positions = mDataContext.getPositions(0)
        /**
         * 初始化电量记录
         */
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        mDataContext.editSetting(Setting.KEYS.battery, battery)
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
        /**
         * 初始化声音
         */
        mSoundPool = SoundPool(10, AudioManager.STREAM_SYSTEM, 5)
        soundId = mSoundPool.load(this@FundMonitorActivity, R.raw.clock, 1)
        mSoundPool.setOnLoadCompleteListener { soundPool, sampleId, status -> isSoundLoaded = true }
    }

    override fun onResume() {
        super.onResume()
        _Utils.hideBottomUIMenu(this)
        startTimer()
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
        // 解除监控开关屏
        if (mainReciver != null) unregisterReceiver(mainReciver)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun stopTimer() {
        timer?.cancel()
        timer?.purge()
        timer = null
    }

    private var timer: Timer? = null
    private var preAverageProfit = 0.toBigDecimal()
    private fun e(log: Any) {
        Log.e("wangsc", log.toString())
    }

    private fun startTimer() {
        try {
            timer = Timer()
            timer?.schedule(object : TimerTask() {
                @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                override fun run() {
                    runOnUiThread {
                        reflushBatteryNumber()
                        try {
                            getStockInfoList(positions, object : OnLoadStockInfoListListener {
                                override fun onLoadFinished(stockInfoList: List<StockInfo>, totalProfit: BigDecimal, averageProfit: BigDecimal, time: String) {
                                    try {
                                        val size = stockInfoList.size
                                        e("$size , $totalProfit , $averageProfit")
                                        if (stockInfoList.size == 0) {
                                            return
                                        }
                                        runOnUiThread {
                                            textView_totalProfit.text = DecimalFormat("0.00").format(averageProfit * 100.toBigDecimal())
                                            textView.text = time
                                            if (averageProfit > 0.toBigDecimal()) {
                                                textView_totalProfit.setTextColor(resources.getColor(R.color.month_text_color))
                                            } else if (averageProfit == 0.toBigDecimal()) {
                                                textView_totalProfit.setTextColor(Color.WHITE)
                                            } else {
                                                textView_totalProfit.setTextColor(resources.getColor(R.color.DARK_GREEN))
                                            }
                                        }
                                        var speakMsg = ""
                                        //region 股票平均盈利
                                        val msgS = DecimalFormat("0.00").format((averageProfit * 100.toBigDecimal()))
                                        if (Math.abs((averageProfit - preAverageProfit).toDouble()) * 100 > 1.0 / size) {
                                            preAverageProfit = averageProfit
                                            speakMsg += msgS
                                        }
                                        if (!speakMsg.isEmpty() && mDataContext.getSetting(Setting.KEYS.is_stock_broadcast, true).boolean) {
                                            var pitch = 1.0f
                                            var speech = 1.2f
                                            if (averageProfit < 0.toBigDecimal()) {
                                                pitch = 0.1f
                                                speech = 0.8f
                                            }
                                            _Utils.speaker(applicationContext, speakMsg, pitch, speech)
                                        }
                                    } catch (e: Exception) {
                                        _Utils.printException(this@FundMonitorActivity, e)
                                        Log.e("wangsc", e.message?:"")
                                    }
                                }
                            })
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }, 0, 10000)
        } catch (e: Exception) {
            _Utils.printException(this, e)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun reflushBatteryNumber() {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        textView_battery.text = "$battery%"
    }

    private fun parseItem(code: String): String {
        val item = StringBuffer()
        for (i in 0 until code.length) {
            val c = code[i]
            if (!Character.isDigit(c)) {
                item.append(c)
            } else {
                break
            }
        }
        return item.toString()
    }

    private fun playknock() {
        if (!isSoundLoaded) {
            mSoundPool.setOnLoadCompleteListener { soundPool, sampleId, status ->
                isSoundLoaded = true
                soundPool.play(soundId, 0.6f, 0.6f, 0, 0, 1f)
            }
        } else {
            mSoundPool.play(soundId, 0.6f, 0.6f, 0, 0, 1f)
        }
    }

    inner class MainReciver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON -> {
                    val pm = context.getSystemService(POWER_SERVICE) as PowerManager
                    if (!pm.isScreenOn) {
                        _Utils.speaker(this@FundMonitorActivity.applicationContext, mDataContext.getSetting(Setting.KEYS.speaker_screen_off, "关").string)
                    }
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra("level", 0)
                    val dataLevel = mDataContext.getSetting(Setting.KEYS.battery, 0).int
                    if (dataLevel != level) {
                        val span = System.currentTimeMillis() - if (preBatteryTime == 0L) System.currentTimeMillis() else preBatteryTime
                        preBatteryTime = System.currentTimeMillis()
                        mDataContext.editSetting(Setting.KEYS.battery, level)
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
                            AudioManager.FLAG_SHOW_UI)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    audio.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_SHOW_UI)
                    return true
                }
                else -> {
                }
            }
        } catch (e: Exception) {
            _Utils.printException(this@FundMonitorActivity, e)
        }
        return super.onKeyDown(keyCode, event)
    }
}