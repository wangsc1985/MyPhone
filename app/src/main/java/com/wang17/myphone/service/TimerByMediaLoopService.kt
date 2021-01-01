package com.wang17.myphone.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources.NotFoundException
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.IBinder
import android.os.PowerManager.WakeLock
import com.wang17.myphone.R
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.database.Position
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.util.DataContext
import com.wang17.myphone.util._NotificationUtils
import com.wang17.myphone.util._Utils
import java.text.DecimalFormat
import java.util.*

@Deprecated("")
/**
 * 用MediaPlayer的循环播放10秒音乐，占用后台进程，不让系统杀死此Service，并获取WakeLock电源锁。并且同时运行Timer， 并且发送Notification看看用这种方法能不能让Timer沾沾MediaPlayer的光，也不被杀死。
 *
 *
 * 测试结果：MediaPlayer运行正常，没有被杀死，但是TImer在运行一段时间后还是被杀死了。
 */
class TimerByMediaLoopService : Service() {
    private lateinit var mDataContext: DataContext
    private lateinit var mPlayer: MediaPlayer
    private val positions: List<Position>? = null
    private var isSoundLoaded = false
    private var mSoundPool: SoundPool? = null
    private var soundId = 0
    private var mainReciver: MainReciver? = null
    private var loadStockCount = 1
    private var loopCount = 1
    private var preAverageTotalProfitS = 0.0
    private val preAverageTotalProfitF = 0.0
    private var preTime: Long = 0
    var mTimeS: String? = null
    var mTimeF: String? = null
    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            mDataContext = DataContext(this)
            // TODO: 2019/4/26 test
            //region test
            /**
             * 初始化声音
             */
            mSoundPool = SoundPool(10, AudioManager.STREAM_MUSIC, 5)
            soundId = mSoundPool!!.load(applicationContext, R.raw.clock, 1)
            mSoundPool!!.setOnLoadCompleteListener { soundPool, sampleId, status -> isSoundLoaded = true }
            /**
             *
             */
            preAverageTotalProfitS = 0.0
            preTime = System.currentTimeMillis()
            startMediaPlayer()
            startTimer()
        } catch (e: Exception) {
            _Utils.printException(applicationContext, e)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        /**
         * 监听
         */
        mainReciver = MainReciver()
        val filter = IntentFilter()
        // 监控电量变化
        filter.addAction(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(mainReciver, filter)
    }

    lateinit var timer: Timer
    private fun startTimer() {
        if (timer != null) {
            return
        }
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                try {
                    if (++loopCount >= 10) {
                        val ctm = System.currentTimeMillis()
                        val span = DecimalFormat("#,000").format(ctm - preTime)
                        mDataContext!!.addLog("timer", "span", span + "秒")
                        preTime = ctm
                        loopCount = 0
                    }
                    sendNotification("LC", loopCount.toString() + "", "LSC", (++loadStockCount).toString(), "Time", DateTime().toTimeString())
                } catch (e: Exception) {
                }
            }
        }, 0, 10000)
    }

    private fun stopTimer() {
        timer.cancel()
    }

    override fun onDestroy() {
        stopTimer()
        stopMediaPlayer()
        // 解除监控
        if (mainReciver != null) unregisterReceiver(mainReciver)
        _NotificationUtils.closeNotification(applicationContext, NOTIFICATION_ID)
        super.onDestroy()
    }

    private fun stopMediaPlayer() {
        mPlayer.stop()
        mPlayer.release()
    }

    fun startMediaPlayer() {
        try {
            mPlayer = MediaPlayer.create(applicationContext, R.raw.second_60)
            mPlayer.setVolume(0.1f, 0.1f)
            mPlayer.setLooping(true)
            mPlayer.start()
        } catch (e: Exception) {
            _Utils.printException(applicationContext, e)
        }
    }

    private var preBatteryTime: Long = 0

    //region 通知
    private fun sendNotification(szPrice: String, szIncrease: String, sTime: String, sIncrease: String, fTime: String, fIncrease: String) {
        try {
            _NotificationUtils.sendNotification(NOTIFICATION_ID, applicationContext, R.layout.notification2) { remoteViews ->
                remoteViews.setTextViewText(R.id.textView_price_sz, szPrice)
                remoteViews.setTextViewText(R.id.textView_increase_sz, szIncrease)
                remoteViews.setTextColor(R.id.textView_increase_sz, if (szIncrease.contains("-")) resources.getColor(R.color.DARK_GREEN) else resources.getColor(R.color.month_text_color))
                remoteViews.setTextViewText(R.id.textView_time_s, sTime)
                remoteViews.setTextViewText(R.id.textView_increase_s, sIncrease)
                remoteViews.setTextColor(R.id.textView_increase_s, if (sIncrease.contains("-")) resources.getColor(R.color.DARK_GREEN) else resources.getColor(R.color.month_text_color))
                remoteViews.setTextViewText(R.id.textView_time_f, fTime)
                remoteViews.setTextViewText(R.id.textView_increase_f, fIncrease)
                remoteViews.setTextColor(R.id.textView_increase_f, if (fIncrease.contains("-")) resources.getColor(R.color.DARK_GREEN) else resources.getColor(R.color.month_text_color))
            }
        } catch (e: NotFoundException) {
            _Utils.printException(applicationContext, e)
        }
    }

    inner class MainReciver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                // TODO Auto-generated method stub
                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val level = intent.getIntExtra("level", 0)
                        val dataLevel = mDataContext!!.getSetting(Setting.KEYS.battery, 0).int
                        if (dataLevel != level) {
                            val span = System.currentTimeMillis() - if (preBatteryTime == 0L) System.currentTimeMillis() else preBatteryTime
                            mDataContext!!.addLog("电量", "{" + level + "%}{" + span / 60000 + "分" + span % 60000 / 1000 + "秒}", "")
                            preBatteryTime = System.currentTimeMillis()
                            mDataContext!!.editSetting(Setting.KEYS.battery, level)
                        }
                    }
                }
            } catch (e: Exception) {
                _Utils.printException(applicationContext, e)
            }
        }
    }

    companion object {
        private const val ERROR_CODE_5 = 589
        private val wakeLock: WakeLock? = null
        private const val ERROR_CODE_4 = 667

        //endregion
        private const val ERROR_CODE_1 = 628
        const val NOTIFICATION_ID = 100
        private const val ERROR_CODE = 807
    }
}