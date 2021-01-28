package com.wang17.myphone.service

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import com.wang17.myphone.R
import com.wang17.myphone.circleMinite
import com.wang17.myphone.e
import com.wang17.myphone.eventbus.EventBusMessage
import com.wang17.myphone.eventbus.SenderBuddhaServiceOnDestroy
import com.wang17.myphone.eventbus.SenderTimerRuning
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.util.DataContext
import com.wang17.myphone.util._NotificationUtils
import com.wang17.myphone.util._Session
import com.wang17.myphone.util._Utils
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.*

class BuddhaService : Service() {

    //region 悬浮窗
    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var floatingWindowView: View? = null

    //endregion
    private val NOTIFICATION_ID: Int = 12345
    private lateinit var dc: DataContext
    val buddhaReceiver = BuddhaReceiver()

    var startTimeInMillis: Long = 0
    var savedDuration = 0L
    var notificationCount = 0
    var notificationTime = ""

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
        e("buddha service onStartCommand")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate() {
        super.onCreate()
        e("buddha service onCreate")
        try {
            dc = DataContext(applicationContext)
            dc.addLog("念佛", "=== buddha service 创建 ===", null)
            val setting = dc.getSetting(Setting.KEYS.buddha_duration)
            setting?.let {
                savedDuration = setting.long
            }
            val volume = dc.getSetting(Setting.KEYS.buddha_volume)
            volume?.let {
                setMediaVolume(it.int)
            }
            mAm = getSystemService(AUDIO_SERVICE) as AudioManager
            startTimeInMillis = System.currentTimeMillis()
            val buddhaSpeed = dc.getSetting(Setting.KEYS.buddha_speed, 2).int
            startMediaPlayer(buddhaSpeed)
            showFloatingWindow()
            startTimer()
            //
            val filter = IntentFilter()
            filter.addAction(ACTION_BUDDHA_PLAYE)
            filter.addAction(ACTION_BUDDHA_PAUSE)
            registerReceiver(buddhaReceiver, filter)
        } catch (e: Exception) {
            e("buddha service onCreate" + e.message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMediaPlayer()
        stopTimer()

        _NotificationUtils.closeNotification(applicationContext, NOTIFICATION_ID)

        //

        mAm.abandonAudioFocus(afChangeListener)
        unregisterReceiver(buddhaReceiver)
        e("------------- 销毁完毕 ---------------")
        EventBus.getDefault().post(EventBusMessage.getInstance(SenderBuddhaServiceOnDestroy(), "buddha service destroyed"))
        //region 悬浮窗
        windowManager.removeView(floatingWindowView)
        dc.addLog("念佛", "=== buddha service 销毁 ===", null)
    }

    var timerRuning = true
    var timer: Timer? = null
    fun startTimer() {
        timer?.cancel()
        timer = null
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                if (timerRuning) {
//                    e("缓存duration : ${savedDuration/1000}秒  此段duration : ${(System.currentTimeMillis() - startTimeInMillis)/1000}秒   此段起始时间 : ${DateTime(startTimeInMillis).toTimeString()}")
                    val duration = savedDuration + System.currentTimeMillis() - startTimeInMillis
                    val second = duration % 60000 / 1000
                    val miniteT = duration / 60000
                    val minite = miniteT % 60
                    val hour = miniteT / 60
                    notificationCount = (miniteT / circleMinite).toInt()
                    notificationTime = "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}"
                    EventBus.getDefault().post(EventBusMessage.getInstance(SenderTimerRuning(), duration.toString()))
                }
                sendNotification(notificationCount, notificationTime)
            }
        }, 0, 1000)
    }

    fun restartTimer() {
        timerRuning = true
    }

    fun pauseTimer() {
        timerRuning = false
    }

    fun stopTimer() {
        timer?.cancel()
    }


    /**
     * 设置多媒体音量
     * 这里我只写了多媒体和通话的音量调节，其他的只是参数不同，大家可仿照
     */
    fun setMediaVolume(volume: Int) {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND or AudioManager.FLAG_SHOW_UI)
    }

    private lateinit var mPlayer: MediaPlayer
    private lateinit var mBackgroundPlayer: MediaPlayer
    fun startMediaPlayer(velocity: Int) {
        try {
            requestFocus()
            mPlayer = MediaPlayer()
            var url = File(_Session.ROOT_DIR, "追顶念佛$velocity.mp3")
            mPlayer.reset() //把各项参数恢复到初始状态
            mPlayer.setDataSource(url.path)
            mPlayer.prepare() //进行缓冲
            mPlayer.setVolume(1f, 1f)
            mPlayer.setLooping(true)
            chantBuddhaStart()


            mBackgroundPlayer = MediaPlayer.create(applicationContext, R.raw.second_60)
            mBackgroundPlayer.setVolume(0.01f, 0.01f)
            mBackgroundPlayer.setLooping(true)
            mBackgroundPlayer.start()
        } catch (e: Exception) {
            e("start media player error : " + e.message!!)
            _Utils.printException(applicationContext, e)
        }
    }

    fun stopMediaPlayer() {
        mBackgroundPlayer.stop()
        chantBuddhaStop()
        mPlayer.stop()
    }

    private lateinit var mAm: AudioManager
    private fun requestFocus(): Boolean {
        val result = try {
            mAm.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        } catch (e: Exception) {
            e(e.message!!)
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    var afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            /**
             * 当其他应用申请焦点之后又释放焦点会触发此回调
             * 可重新播放音乐
             */
            AudioManager.AUDIOFOCUS_GAIN -> {
                e("获取永久焦点")
                dc.addLog("念佛", "获取永久焦点", null)
                chantBuddhaRestart()
                mPlayer.setVolume(1.0f, 1.0f)
                floatingWinButState(false)
                sendNotification(notificationCount, notificationTime)
            }
            /**
             * 长时间丢失焦点,当其他应用申请的焦点为AUDIOFOCUS_GAIN时，
             * 会触发此回调事件，例如播放QQ音乐，网易云音乐等
             * 通常需要暂停音乐播放，若没有暂停播放就会出现和其他音乐同时输出声音
             */
            AudioManager.AUDIOFOCUS_LOSS -> {
                e("永久失去焦点")
                dc.addLog("念佛", "永久失去焦点", null)
                chantBuddhaPause()
                floatingWinButState(true)
                sendNotification(notificationCount, notificationTime)
            }
            /**
             * 短暂性丢失焦点，当其他应用申请AUDIOFOCUS_GAIN_TRANSIENT或AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE时，
             * 会触发此回调事件，例如播放短视频，拨打电话等。
             * 通常需要暂停音乐播放
             */
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                e("暂时失去焦点")
                dc.addLog("念佛", "暂时失去焦点", null)
                chantBuddhaPause()
                floatingWinButState(true)
                sendNotification(notificationCount, notificationTime)
            }
            /**
             * 短暂性丢失焦点并作降音处理
             */
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                e("暂时失去焦点并降音")
                dc.addLog("念佛", "暂时失去焦点并降音", null)
                mPlayer.setVolume(0.3f, 0.3f)
                pauseTimer()
                pauseOrStopData()
            }
        }
    }

    /**
     * 开始或重新开始要做的：
     * 1、记录当前section的startime
     */
    fun reOrStartData() {
        try {
            e("------------------ start or restart -------------------------")
            dc.addLog("念佛", "start or restart", null)
            startTimeInMillis = System.currentTimeMillis()
            dc.editSetting(Setting.KEYS.buddha_startime, startTimeInMillis)
        } catch (e: Exception) {
            dc.addLog("err", "pause or stop", e.message)
        }
    }

    /**
     * 停止或暂停是要做的：
     * 1、将当前section的duration累加入数据库中的总的duration
     * 2、删除section的startime
     */
    fun pauseOrStopData() {
        try {
            e("---------------- pause or stop --------------------")
            dc.addLog("念佛", "pause or stop", null)
            val now = System.currentTimeMillis()

            // 计算当前section的duration
            val settingStartTime = dc.getSetting(Setting.KEYS.buddha_startime)
            settingStartTime?.let {
//                val setting = dc.getSetting(Setting.KEYS.buddha_duration)
//                savedDuration = it?.long ?: 0L
//                e("------------------ 缓存duration : ${savedDuration / 1000}秒  本段duration : ${(System.currentTimeMillis() - startTimeInMillis) / 1000}秒      此段起始时间 : ${DateTime(startTimeInMillis).toTimeString()} ------------------")
                savedDuration += now - startTimeInMillis
//                e("++++++++++++++++++ 缓存duration : ${savedDuration / 1000}秒")
                dc.editSetting(Setting.KEYS.buddha_duration, savedDuration)
                dc.editSetting(Setting.KEYS.buddha_stoptime, now)

                // 删除startime
                dc.deleteSetting(Setting.KEYS.buddha_startime)
            }
        } catch (e: Exception) {
            dc.addLog("err", "pause or stop", e.message)
        }
    }

    fun chantBuddhaStart() {
        mPlayer.start()
        reOrStartData()
        restartTimer()
    }

    fun chantBuddhaRestart() {
        mPlayer.start()
        reOrStartData()
        restartTimer()
    }

    fun chantBuddhaPause() {
        pauseTimer()
        mPlayer.pause()
        pauseOrStopData()
    }

    fun chantBuddhaStop() {
        pauseTimer()
        mPlayer.pause()
        pauseOrStopData()
    }

    // TODO: 2020/11/20 悬浮窗
    private fun sendNotification(count: Int, time: String) {
        _NotificationUtils.sendNotification(NOTIFICATION_ID, applicationContext, R.layout.notification_nf) { remoteViews ->
            remoteViews.setTextViewText(R.id.tv_count, count.toString())
            remoteViews.setTextViewText(R.id.tv_time, time)
            if (mPlayer.isPlaying) {
                remoteViews.setImageViewResource(R.id.image_control, R.drawable.pause)
                val intentRootClick = Intent(ACTION_BUDDHA_PAUSE)
                val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intentRootClick, PendingIntent.FLAG_UPDATE_CURRENT)
                remoteViews.setOnClickPendingIntent(R.id.image_control, pendingIntent)
            } else {
                remoteViews.setImageViewResource(R.id.image_control, R.drawable.play)
                val intentRootClick = Intent(ACTION_BUDDHA_PLAYE)
                val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intentRootClick, PendingIntent.FLAG_UPDATE_CURRENT)
                remoteViews.setOnClickPendingIntent(R.id.image_control, pendingIntent)
            }
        }
    }

    //region 悬浮窗
    private fun floatingWinButState(sate: Boolean) {
        floatingWindowView?.let {
            val iv_control = it.findViewById<ImageView>(R.id.iv_control)
            if (sate) {
                iv_control.setImageResource(R.drawable.play)
            } else {
                iv_control.setImageResource(R.drawable.pause)
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun showFloatingWindow() {

        try {
            //region 悬浮窗
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            layoutParams = WindowManager.LayoutParams()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
            }
            layoutParams.format = PixelFormat.RGBA_8888
            layoutParams.gravity = Gravity.CENTER
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

            layoutParams.width = 300
            layoutParams.height = 300
            layoutParams.x = 300
            layoutParams.y = 300
            //endregion

            if (Settings.canDrawOverlays(this)) {
                floatingWindowView = View.inflate(this, R.layout.inflate_buddha_floating_window, null)
                val iv_control = floatingWindowView?.findViewById<ImageView>(R.id.iv_control)
                iv_control?.setOnClickListener {
                    if(Math.abs(changeX)<10&&Math.abs(changeY)<10) {
                        if (mPlayer.isPlaying) {
                            chantBuddhaPause()
                            floatingWinButState(true)
                            mAm.abandonAudioFocus(afChangeListener)
                        } else {
                            if (requestFocus()) {
                                chantBuddhaRestart()
                                floatingWinButState(false)
                            }
                        }
                        sendNotification(notificationCount, notificationTime)
                    }
                }
                iv_control?.setOnTouchListener(FloatingOnTouchListener())
                windowManager.addView(floatingWindowView, layoutParams)
            }
        } catch (e: Exception) {
            e("xxxxxxxxxxxxxxxxxxxxxxx ${e.message!!}")
        }
    }

    var changeX=0
    var changeY=0
    var startX = 0
    var startY = 0
    inner class FloatingOnTouchListener : View.OnTouchListener {
        private var x = 0
        private var y = 0
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = event.rawX.toInt()
                    y = event.rawY.toInt()
                    startX =x
                    startY = y
                }
                MotionEvent.ACTION_MOVE -> {
                    val nowX = event.rawX.toInt()
                    val nowY = event.rawY.toInt()
                    val movedX = nowX - x
                    val movedY = nowY - y
                    x = nowX
                    y = nowY
                    layoutParams.x = layoutParams.x + movedX
                    layoutParams.y = layoutParams.y + movedY
                    windowManager.updateViewLayout(floatingWindowView, layoutParams)
                }
                MotionEvent.ACTION_UP->{
                    changeX=event.rawX.toInt()-startX
                    changeY=event.rawY.toInt()-startY
                }
                else -> {
                }
            }
            return false
        }
    }
    //endregion

    inner class BuddhaReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                intent?.let {
                    when (it.action) {
                        ACTION_BUDDHA_PLAYE -> {
                            if (requestFocus()) {
                                chantBuddhaRestart()
                                floatingWinButState(false)
                            }
                        }
                        ACTION_BUDDHA_PAUSE -> {
                            chantBuddhaPause()
                            floatingWinButState(true)
                            mAm.abandonAudioFocus(afChangeListener)
                        }
                    }

                    sendNotification(notificationCount, notificationTime)
                }
            } catch (e: Exception) {
                e("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee" + e.message ?: "")
            }
        }
    }

    // TODO: 2021/1/21  1、来微信记录被重置了

    companion object {
        val ACTION_BUDDHA_PAUSE = "com.wang17.myphone.buddha.pause"
        val ACTION_BUDDHA_PLAYE = "com.wang17.myphone.buddha.play"
    }
}