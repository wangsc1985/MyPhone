package com.wang17.myphone.service

import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.wang17.myphone.R
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.database.Setting
import com.wang17.myphone.e
import com.wang17.myphone.eventbus.EventBusMessage
import com.wang17.myphone.eventbus.FromMuyuServiceDestory
import com.wang17.myphone.eventbus.FromMuyuServiceTimer
import com.wang17.myphone.util._NotificationUtils
import com.wang17.myphone.util._Utils
import org.greenrobot.eventbus.EventBus
import java.util.*

class MuyuService : Service() {

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

    var buddhaType = 11
    var pitch = 1.0f
    var speed = 1.0f
    var circleSecond = 600

    var muyu_period=1000L
    var yq_period=1000L

    lateinit var guSound: SoundPool
    lateinit var muyuSound:SoundPool

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
            circleSecond = (dc.getSetting(Setting.KEYS.muyu_period,666).int*1.080).toInt()

            guSound = SoundPool(100, AudioManager.STREAM_MUSIC, 0)
            guSound.load(this, R.raw.yq, 1)
            muyuSound = SoundPool(100, AudioManager.STREAM_MUSIC, 0)
            muyuSound.load(this, R.raw.muyu, 1)
            val setting = dc.getSetting(Setting.KEYS.buddha_duration)
            setting?.let {
                savedDuration = setting.long
            }
            muyu_period = dc.getSetting(Setting.KEYS.muyu_period,1000).long
            yq_period = dc.getSetting(Setting.KEYS.yq_period,1000).long
            val volume = dc.getSetting(Setting.KEYS.念佛最佳音量)
            volume?.let {
                setMediaVolume(it.int)
            }
            mAm = getSystemService(AUDIO_SERVICE) as AudioManager
            startTimeInMillis = System.currentTimeMillis()
            val musicName = dc.getSetting(Setting.KEYS.buddha_music_name)
            if (musicName == null)
                return

            startBackgroundMediaPlayer()
            startTimer()
            showFloatingWindow()
            //
            val filter = IntentFilter()
            filter.addAction(ACTION_BUDDHA_PLAYE)
            filter.addAction(ACTION_BUDDHA_PAUSE)
            filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            registerReceiver(buddhaReceiver, filter)
        } catch (e: Exception) {
            e("buddha service onCreate" + e.message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBackgroundMediaPlayer()
        stopTimer()

        _NotificationUtils.closeNotification(applicationContext, NOTIFICATION_ID)

        //
        mAm.abandonAudioFocus(afChangeListener)
        unregisterReceiver(buddhaReceiver)
        e("------------- 销毁完毕 ---------------")
        EventBus.getDefault().post(EventBusMessage.getInstance(FromMuyuServiceDestory(), "buddha service destroyed"))
        //region 悬浮窗
        windowManager.removeView(floatingWindowView)
    }

    var prvCount = 0
    var timerRuning = true
    var timer: Timer? = null
    fun startTimer() {
        timer?.cancel()
        timer = null
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                if (timerRuning) {
                    muyuSound.play(1,1.0f,1.0f,0,0,1.0f)

//                    e("缓存duration : ${savedDuration/1000}秒  此段duration : ${(System.currentTimeMillis() - startTimeInMillis)/1000}秒   此段起始时间 : ${DateTime(startTimeInMillis).toTimeString()}")
                    val duration = savedDuration + System.currentTimeMillis() - startTimeInMillis
                    val second = duration % 60000 / 1000
                    val miniteT = duration / 60000
                    val minite = miniteT % 60
                    val hour = miniteT / 60
                    notificationCount = (duration / 1000 / circleSecond).toInt()
                    notificationTime = "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}"
                    EventBus.getDefault().post(EventBusMessage.getInstance(FromMuyuServiceTimer(), duration.toString()))
                    tv_duration?.setText(notificationTime)
                    if (prvCount < notificationCount && dc.getSetting(Setting.KEYS.is念佛引罄间隔提醒, true).boolean) {
                        guSound.play(1, 1.0f, 1.0f, 0, 0, 1.0f)
                        prvCount = notificationCount
                        if (notificationCount % 2 == 0) {
                            Thread.sleep(yq_period)
                            guSound.play(1, 1.0f, 1.0f, 0, 0, 1.0f)
                        }
                    }
//                    val now = System.currentTimeMillis()
//                    if(dc.getSetting(Setting.KEYS.is监控木鱼间隔有效性,true).boolean){
//                        dc.addRunLog("木鱼","${now-prvTimeInMillin}","")
//                    }
//                    prvTimeInMillin = now
                }
                sendNotification(notificationCount, notificationTime)
            }
        }, 0, muyu_period)
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

    private var mBackgroundPlayer: MediaPlayer?=null

    @RequiresApi(Build.VERSION_CODES.M)
    fun startBackgroundMediaPlayer() {
        try {
            requestFocus()
            chantBuddhaStart()

            mBackgroundPlayer = MediaPlayer.create(applicationContext, R.raw.second_60)
            mBackgroundPlayer?.setVolume(0.01f, 0.01f)
            mBackgroundPlayer?.setLooping(true)
            mBackgroundPlayer?.start()
        } catch (e: Exception) {
            e("start media player error : " + e.message!!)
            _Utils.printException(applicationContext, e)
        }
    }

    fun stopBackgroundMediaPlayer() {
        mBackgroundPlayer?.stop()
        chantBuddhaStop()
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
                chantBuddhaRestart()
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
                chantBuddhaPause()
                floatingWinButState(true)
                sendNotification(notificationCount, notificationTime)
            }
            /**
             * 短暂性丢失焦点并作降音处理
             */
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                e("暂时失去焦点并降音")
                chantBuddhaPause()
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
            startTimeInMillis = System.currentTimeMillis()
            dc.editSetting(Setting.KEYS.buddha_startime, startTimeInMillis)
        } catch (e: Exception) {
            dc.addRunLog("err", "pause or stop", e.message)
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
            dc.addRunLog("err", "pause or stop", e.message)
        }
    }

    fun chantBuddhaStart() {
        reOrStartData()
        restartTimer()
    }

    fun chantBuddhaRestart() {
        reOrStartData()
        restartTimer()
    }

    fun chantBuddhaPause() {
        pauseTimer()
        pauseOrStopData()
    }

    fun chantBuddhaStop() {
        pauseTimer()
        pauseOrStopData()
    }

    private fun sendNotification(count: Int, time: String) {
        _NotificationUtils.sendNotification(NOTIFICATION_ID, applicationContext, R.layout.notification_nf) { remoteViews ->
            remoteViews.setTextViewText(R.id.tv_count, if (buddhaType > 9 ) count.toString() else "")
            remoteViews.setTextViewText(R.id.tv_time, time)
            if (timerRuning) {
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

    var tv_duration: TextView? = null

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

            layoutParams.width = dc.getSetting(Setting.KEYS.buddha_float_window_size, 200).int
            layoutParams.height = layoutParams.width + layoutParams.width / 10
            layoutParams.x = dc.getSetting(Setting.KEYS.buddha_float_window_x, 300).int
            layoutParams.y = dc.getSetting(Setting.KEYS.buddha_float_window_y, 300).int
            //endregion

            if (Settings.canDrawOverlays(this)) {
                floatingWindowView = View.inflate(this, R.layout.inflate_buddha_floating_window, null)
                val iv_control = floatingWindowView?.findViewById<ImageView>(R.id.iv_control)
                tv_duration = floatingWindowView?.findViewById(R.id.tv_duration)
                iv_control?.setOnClickListener {
                    if (Math.abs(changeX) < 10 && Math.abs(changeY) < 10) {
                        if (timerRuning) {
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

    var changeX = 0
    var changeY = 0
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
                    startX = x
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
//                    e("x : ${layoutParams.x}   y : ${layoutParams.y}")
                    windowManager.updateViewLayout(floatingWindowView, layoutParams)
                }
                MotionEvent.ACTION_UP -> {
                    changeX = event.rawX.toInt() - startX
                    changeY = event.rawY.toInt() - startY
                    if (Math.abs(changeX) > 10 || Math.abs(changeY) > 10) {
                        dc.editSetting(Setting.KEYS.buddha_float_window_x, layoutParams.x)
                        dc.editSetting(Setting.KEYS.buddha_float_window_y, layoutParams.y)
                    }
                }
                else -> {
                }
            }
            return false
        }
    }
    //endregion

    //region 接收器
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
                        BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                            val adapter = BluetoothAdapter.getDefaultAdapter()
                            e("接收到蓝牙广播")
                            if (BluetoothProfile.STATE_DISCONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET)) {
                                e("蓝牙耳机断开")
                                chantBuddhaPause()
                                floatingWinButState(true)
                                mAm.abandonAudioFocus(afChangeListener)
                            }
                        }
                        AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
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
    //endregion

    companion object {
        val ACTION_BUDDHA_PAUSE = "com.wang17.myphone.buddha.pause"
        val ACTION_BUDDHA_PLAYE = "com.wang17.myphone.buddha.play"
    }
}