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
import android.os.Looper
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.wang17.myphone.R
import com.wang17.myphone.database.*
import com.wang17.myphone.e
import com.wang17.myphone.event.ResetTimeEvent
import com.wang17.myphone.eventbus.*
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.toBuddhaType
import com.wang17.myphone.util._CloudUtils
import com.wang17.myphone.util._NotificationUtils
import com.wang17.myphone.util._Session
import com.wang17.myphone.util._Utils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.math.BigDecimal
import java.util.*


class BuddhaService : Service() {

    //region 悬浮窗
    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var floatingWindowView: View? = null

    //endregion
    private val ID: Int = 123456
    private lateinit var dc: DataContext
    val buddhaReceiver = BuddhaReceiver()

    var startTimeInMillis: Long = 0
    var savedDuration = 0L
    var notificationCount = 0
    var notificationTime = ""
    var notificationCountTotal = 0
    var notificationTimeTotal = ""
    var dbCount=0
    var dbDuration=0L

    var buddhaType = 11
    var pitch = 1.0f
    var speed = 1.0f
    var circleSecond = 600

    var yq_period = 1000L

    lateinit var guSound: SoundPool
    lateinit var souSound: SoundPool
    var targetTap = 12

    var isShowFloatWindow = false
    var isAutoPause = false

    var topTitle=""
    var bottomTitle=""

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
        e("buddha service onStartCommand")
    }

    fun loadDb(){
        dbCount=0
        dbDuration=0
        val ls = dc.getBuddhas(DateTime())
        ls.forEach {
            dbCount+=it.count
            dbDuration += it.duration
        }
        dbCount = dbCount/1080
    }
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate() {
        super.onCreate()
        e("buddha service onCreate")
        try {
            dc = DataContext(applicationContext)
            dc.deleteRunLog("BuddhaService", DateTime.today.addDays(-1))
            topTitle = dc.getSetting(Setting.KEYS.top_title,"").string
            bottomTitle = dc.getSetting(Setting.KEYS.bottom_title,"").string

            loadDb()

            isShowFloatWindow = dc.getSetting(Setting.KEYS.is显示念佛悬浮窗, false).boolean
            isAutoPause = dc.getSetting(Setting.KEYS.is念佛自动暂停, false).boolean
            targetTap = dc.getSetting(Setting.KEYS.念佛自动结束圈数, 12).int

            guSound = SoundPool(100, AudioManager.STREAM_MUSIC, 0)
            guSound.load(this, R.raw.yq, 1)
            souSound = SoundPool(100, AudioManager.STREAM_MUSIC, 0)
            souSound.load(this, R.raw.piu, 1)
            val setting = dc.getSetting(Setting.KEYS.buddha_duration)
            setting?.let {
                savedDuration = setting.long
            }
            yq_period = dc.getSetting(Setting.KEYS.yq_period, 1000).long
            val volume = dc.getSetting(Setting.KEYS.念佛最佳音量)
            volume?.let {
                setMediaVolume(it.int)
            }
            mAm = getSystemService(AUDIO_SERVICE) as AudioManager
            startTimeInMillis = System.currentTimeMillis()

            val musicName = dc.getSetting(Setting.KEYS.buddha_music_name)
            if (musicName == null)
                return

            loadBuddhaConfig()

            startMediaPlayer(musicName.string)
            startTimer()
            showFloatingWindow()
            //
            val filter = IntentFilter()
            filter.addAction(ACTION_BUDDHA_PLAYE)
            filter.addAction(ACTION_BUDDHA_PLAYE_AUTO)
            filter.addAction(ACTION_BUDDHA_PAUSE)
            filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            registerReceiver(buddhaReceiver, filter)

            EventBus.getDefault().register(this)
        } catch (e: Exception) {
            e("buddha service onCreate" + e.message)
        }
    }

    private fun loadBuddhaConfig() {
        val musicName = dc.getSetting(Setting.KEYS.buddha_music_name)
        musicName?.let {
            val file = _Session.getFile(musicName.string)
            var bf = dc.getBuddhaConfig(musicName.string, file.length())
            if (bf == null) {
                bf = BuddhaConfig(musicName.string, file.length(), "md5", 1.0f, 1.0f, 11, 600)
            }

            buddhaType = bf.type
            speed = bf.speed
            pitch = bf.pitch
            if (bf.type == 11 || bf.type == 10)
                circleSecond = (bf.circleSecond / bf.speed).toInt()
            else
                circleSecond = bf.circleSecond
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMediaPlayer()
        stopTimer()
//        stopTimerMy()

        _NotificationUtils.closeNotification(applicationContext, ID)

        //

        mAm.abandonAudioFocus(afChangeListener)
        unregisterReceiver(buddhaReceiver)
        e("------------- 销毁完毕 ---------------")
        EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaServiceDestroy(), "buddha service destroyed"))
        //region 悬浮窗
        if (isShowFloatWindow) {
            windowManager.removeView(floatingWindowView)
        }
        EventBus.getDefault().unregister(this)
    }

    var prvCount = 0
    var isTimerRuning = true
    var timer: Timer? = null

    fun longToTimeString(duration :Long):String{
        val second = duration % 60000 / 1000
        val miniteT = duration / 60000
        val minite = miniteT % 60
        val hour = miniteT / 60
        return "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}"
    }
    var preTime = 0L
    fun startTimer() {
        timer?.cancel()
        timer = null
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                if (isTimerRuning) {
//                    e("缓存duration : ${savedDuration/1000}秒  此段duration : ${(System.currentTimeMillis() - startTimeInMillis)/1000}秒   此段起始时间 : ${DateTime(startTimeInMillis).toTimeString()}")
                    val duration = savedDuration + System.currentTimeMillis() - startTimeInMillis
                    notificationCount = (duration / 1000 / circleSecond).toInt()
                    notificationTime = longToTimeString(duration)
                    e("db duration: ${longToTimeString(dbDuration)} ; save duration: ${longToTimeString(savedDuration)}  section duration:${longToTimeString(System.currentTimeMillis() - startTimeInMillis)}")
                    val duration1 = dbDuration + savedDuration + System.currentTimeMillis() - startTimeInMillis
                    notificationCountTotal = dbCount+notificationCount
                    notificationTimeTotal =  longToTimeString(duration1)

                    EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaServiceTimer(), duration.toString()))
                    tv_duration?.setText(notificationTime)

                    if (prvCount < notificationCount) {
                        if (dc.getSetting(Setting.KEYS.is念佛引罄间隔提醒, true).boolean) {
                            dc.addRunLog("BuddhaService", "引罄", "${DateTime().toTimeString()}  ${notificationTime} prv count : ${prvCount} ; notification count : ${notificationCount}")
                            guSound.play(1, 1.0f, 1.0f, 0, 0, 1.0f)
                            prvCount = notificationCount
                            if (notificationCount % 2 == 0) {
                                Thread.sleep(yq_period)
                                guSound.play(1, 1.0f, 1.0f, 0, 0, 1.0f)
                            }
                        }
                        if (isAutoPause) {
                            chantBuddhaPause()
                            floatingWinButState(true)
                            mAm.abandonAudioFocus(afChangeListener)
                            isAutoPause=false
                        }
                    }

                    if (notificationCount > targetTap) {
                        stopSelf()
                    }
                }
//                if (preTime > 0 && System.currentTimeMillis() - preTime > 10000) {
//                    dc.addRunLog("BuddhaService", "计时器超时", "${(System.currentTimeMillis() - preTime) / 1000}秒")
//                }
                preTime = System.currentTimeMillis()
                sendNotification(notificationCount,notificationCountTotal, notificationTime,notificationTimeTotal)
            }
        }, 0, 1000)
    }

    fun restartTimer() {
        isTimerRuning = true
    }

    fun pauseTimer() {
        isTimerRuning = false
    }

    fun stopTimer() {
        timer?.cancel()
    }

    fun setMediaVolume(volume: Int) {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND or AudioManager.FLAG_SHOW_UI)
    }

    private var mPlayer: MediaPlayer? = null
    private var mBackgroundPlayer: MediaPlayer? = null

    @RequiresApi(Build.VERSION_CODES.M)
    fun startMediaPlayer(musicName: String) {
        try {
            requestFocus()
            mPlayer = MediaPlayer()
            var url = File(_Session.ROOT_DIR, musicName)
            mPlayer?.reset() //把各项参数恢复到初始状态
            mPlayer?.setDataSource(url.path)
            mPlayer?.prepare() //进行缓冲
            mPlayer?.setVolume(volume.toFloat(), volume.toFloat())
            mPlayer?.setLooping(true)
            val params = mPlayer?.playbackParams
            params?.let {
                params.setPitch(pitch)
                params.setSpeed(speed)
                mPlayer?.playbackParams = params
            }
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

    fun stopMediaPlayer() {
        mBackgroundPlayer?.stop()
        chantBuddhaStop()
        mPlayer?.stop()
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
//                dc.addRunLog("BuddhaService","获取永久焦点","")
                e("获取永久焦点")
                if (!AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    chantBuddhaRestart()
                } else {
                    AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = false
                }
                mPlayer?.setVolume(1.0f, 1.0f)
                floatingWinButState(false)
                sendNotification(notificationCount,notificationCountTotal, notificationTime,notificationTimeTotal)
            }
            /**
             * 长时间丢失焦点,当其他应用申请的焦点为AUDIOFOCUS_GAIN时，
             * 会触发此回调事件，例如播放QQ音乐，网易云音乐等
             * 通常需要暂停音乐播放，若没有暂停播放就会出现和其他音乐同时输出声音
             */
            AudioManager.AUDIOFOCUS_LOSS -> {
//                dc.addRunLog("BuddhaService","永久失去焦点","")
                e("永久失去焦点")
                chantBuddhaPause()
                floatingWinButState(true)
                sendNotification(notificationCount,notificationCountTotal, notificationTime,notificationTimeTotal)
            }
            /**
             * 短暂性丢失焦点，当其他应用申请AUDIOFOCUS_GAIN_TRANSIENT或AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE时，
             * 会触发此回调事件，例如播放短视频，拨打电话等。
             * 通常需要暂停音乐播放
             */
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
//                dc.addRunLog("BuddhaService","暂时失去焦点","")
                e("暂时失去焦点")
                chantBuddhaPause()
                floatingWinButState(true)
                sendNotification(notificationCount,notificationCountTotal, notificationTime,notificationTimeTotal)
            }
            /**
             * 短暂性丢失焦点并作降音处理
             */
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
//                dc.addRunLog("BuddhaService","暂时失去焦点并降音","")
                e("暂时失去焦点并降音")
                mPlayer?.setVolume(0.3f, 0.3f)
                AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = true
//                pauseTimer()
//                pauseOrStopData()
            }
        }
    }

    var AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = false

    /**
     * 开始或重新开始要做的：
     * 1、记录当前section的startime
     */
    fun reOrStartData() {
        try {
//            e("------------------ start or restart -------------------------")

            startTimeInMillis = System.currentTimeMillis()
            dc.editSetting(Setting.KEYS.buddha_startime, startTimeInMillis)


            val settingStopTimeInMillis = dc.getSetting(Setting.KEYS.buddha_stoptime)
            val settingDuration = dc.getSetting(Setting.KEYS.buddha_duration)
            val now = System.currentTimeMillis()
            dc.addRunLog("BuddhaService", "开始念佛", settingStopTimeInMillis?.let { "上次暂停时间：${DateTime(it.long).toLongDateTimeString()}" +
                    "  上次念佛时长：${settingDuration?.let { settingDuration.long / 60000 }}分钟  距离上次暂停：${(now - it.long) / 60000}分钟" }?:""
            )
            if (settingStopTimeInMillis != null && settingDuration != null && now - settingStopTimeInMillis.long > 12 * 60000 && settingDuration.long / 1000 / circleSecond >= 1) {

                val startTime = DateTime(settingStopTimeInMillis.long - settingDuration.long)
                val tap = (settingDuration.long / 1000 / circleSecond).toInt()
                val durationWhole = circleSecond * tap * 1000.toLong()
                var buddha = BuddhaRecord(startTime, durationWhole, tap * 1080, buddhaType.toBuddhaType(), "")

                var durationOdd = settingDuration.long - durationWhole

                _CloudUtils.addBuddha(applicationContext!!, buddha) { code, result ->
                    when (code) {
                        0 -> {
                            dc.addRunLog("BuddhaService", "云储存buddha成功", "${code}   ${result}")
                            dc.addBuddha(buddha)
                            startTimeInMillis = System.currentTimeMillis() - durationOdd
                            savedDuration = 0
                            prvCount = 0
                            loadDb()
//                            dbDuration += durationWhole
//                            dbCount += tap

                            dc.deleteSetting(Setting.KEYS.buddha_duration)
                            dc.deleteSetting(Setting.KEYS.buddha_stoptime)
                            dc.editSetting(Setting.KEYS.buddha_startime, startTimeInMillis)
                            Looper.prepare()
                            Toast.makeText(applicationContext, result.toString(), Toast.LENGTH_LONG).show()
                            Looper.loop()
                        }
                        else -> {
                            dc.addRunLog("err", "云储存buddha失败", "${code}   ${result}")
                            e("code : $code , result : $result")
                        }
                    }
                }
            }

        } catch (e: Exception) {
            dc.addRunLog("err", "reOrStartData", e.message)
        }
    }

    /**
     * 停止或暂停是要做的：
     * 1、将当前section的duration累加入数据库中的总的duration
     * 2、删除section的startime
     */
    fun pauseOrStopData() {
        dc.addRunLog("BuddhaService", "暂停念佛", "")
        try {
//            e("---------------- pauseOrStopData --------------------")
            val now = System.currentTimeMillis()

            // 计算当前section的duration
            val settingStartTime = dc.getSetting(Setting.KEYS.buddha_startime)
            settingStartTime?.let {
                savedDuration += now - startTimeInMillis
                dc.editSetting(Setting.KEYS.buddha_duration, savedDuration)
                dc.editSetting(Setting.KEYS.buddha_stoptime, now)

                // 删除startime
                dc.deleteSetting(Setting.KEYS.buddha_startime)
            }
        } catch (e: Exception) {
            dc.addRunLog("err", "pauseOrStopData", e.message)
        }
    }

    fun chantBuddhaStart() {
        mPlayer?.start()
        reOrStartData()
        restartTimer()
    }

    fun chantBuddhaRestart() {
        mPlayer?.start()
        reOrStartData()
        restartTimer()
    }

    fun chantBuddhaPause() {
        pauseTimer()
        mPlayer?.pause()
        pauseOrStopData()
    }

    fun chantBuddhaStop() {
        pauseTimer()
        mPlayer?.pause()
        pauseOrStopData()
    }

    private fun sendNotification(count: Int, totalCount:Int, time: String,totalTime:String) {
        _NotificationUtils.sendNotification(ID, applicationContext, R.layout.notification_nf, _NotificationUtils.CHANNEL_BUDDHA) { remoteViews ->
            remoteViews.setTextViewText(R.id.tv_count, if (buddhaType > 9) "${count}" else "")
            remoteViews.setTextViewText(R.id.tv_time, time)
            remoteViews.setTextViewText(R.id.tv_countTotal, if (buddhaType > 9) "${totalCount}" else "")
            remoteViews.setTextViewText(R.id.tv_timeTotal, totalTime)
            remoteViews.setTextViewText(R.id.tv_topTitle,topTitle)
            remoteViews.setTextViewText(R.id.tv_bottomTitle,bottomTitle)
            if(totalCount==count){
                remoteViews.setViewVisibility(R.id.tv_countTotal,View.GONE)
                remoteViews.setViewVisibility(R.id.tv_timeTotal,View.GONE)
            }else{
                remoteViews.setViewVisibility(R.id.tv_countTotal,View.VISIBLE)
                remoteViews.setViewVisibility(R.id.tv_timeTotal,View.VISIBLE)
            }

            if (mPlayer?.isPlaying ?: false) {
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
            val intentRootClick1 = Intent(ACTION_BUDDHA_PLAYE_AUTO)
            val pendingIntent1 = PendingIntent.getBroadcast(applicationContext, 0, intentRootClick1, PendingIntent.FLAG_UPDATE_CURRENT)
            remoteViews.setOnClickPendingIntent(R.id.tv_count, pendingIntent1)
            remoteViews.setOnClickPendingIntent(R.id.tv_countTotal, pendingIntent1)
            if(isAutoPause){
                remoteViews.setTextColor(R.id.tv_count,resources.getColor(R.color.a,null))
                remoteViews.setTextColor(R.id.tv_time,resources.getColor(R.color.a,null))
                remoteViews.setTextColor(R.id.tv_countTotal,resources.getColor(R.color.a,null))
                remoteViews.setTextColor(R.id.tv_timeTotal,resources.getColor(R.color.a,null))
            }else{
                remoteViews.setTextColor(R.id.tv_count,resources.getColor(R.color.calendar_yl_color,null))
                remoteViews.setTextColor(R.id.tv_time,resources.getColor(R.color.calendar_yl_color,null))
                remoteViews.setTextColor(R.id.tv_countTotal,resources.getColor(R.color.calendar_yl_color,null))
                remoteViews.setTextColor(R.id.tv_timeTotal,resources.getColor(R.color.calendar_yl_color,null))
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
            if (!isShowFloatWindow) return
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
                        if (mPlayer?.isPlaying ?: false) {
                            chantBuddhaPause()
                            floatingWinButState(true)
                            mAm.abandonAudioFocus(afChangeListener)
                        } else {
                            if (requestFocus()) {
                                chantBuddhaRestart()
                                floatingWinButState(false)
                            }
                        }
                        sendNotification(notificationCount,notificationCountTotal, notificationTime,notificationTimeTotal)
                    }
                }
                iv_control?.setOnTouchListener(FloatingOnTouchListener())
                windowManager.addView(floatingWindowView, layoutParams)
            }
        } catch (e: Exception) {
            e("xxxxxxxxxxxxxxxxxxxxxxx ${e.message!!}")
        }
    }

    var volume: BigDecimal = "1.0".toBigDecimal()

    //region EventBus处理
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onGetMessage(msg: EventBusMessage) {
        try {
            when (msg.sender) {
                is FromBuddhaConfigUpdate -> {
                    loadBuddhaConfig()

                    var param = mPlayer?.playbackParams
                    param?.let {
                        e("pitch : ${msg.msg}")
                        param.pitch = pitch
                        param.speed = speed
                        mPlayer?.playbackParams = param
                    }
                }
                is ResetTimeEvent -> {
                    val duration = savedDuration + System.currentTimeMillis() - startTimeInMillis
                    val count = (duration / 1000 / circleSecond).toInt()
                    startTimeInMillis = System.currentTimeMillis() - (circleSecond * 1000 * count - savedDuration)
                    dc.editSetting(Setting.KEYS.buddha_startime, startTimeInMillis)
                }
                is FromBuddhaVolumeAdd -> {
                    volume += "0.1".toBigDecimal()
                    e("声音加" + volume)
                    mPlayer?.setVolume(volume.toFloat(), volume.toFloat())
                }
                is FromBuddhaVolumeMinus -> {
                    volume -= "0.1".toBigDecimal()
                    e("声音减" + volume)
                    mPlayer?.setVolume(volume.toFloat(), volume.toFloat())
                }
                is FromTotalCount->{
                    loadDb()
                }
            }
        } catch (e: Exception) {
            dc.addRunLog("BuddhaService", "EventBus", e.message)
        }
    }
    //endregion

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
                        ACTION_BUDDHA_PLAYE_AUTO -> {
                            if (!isTimerRuning) {
                                if (requestFocus()) {
                                    chantBuddhaRestart()
                                    floatingWinButState(false)
                                }
                            }
                            isAutoPause = !isAutoPause
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

                    sendNotification(notificationCount,notificationCountTotal, notificationTime,notificationTimeTotal)
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
        val ACTION_BUDDHA_PLAYE_AUTO = "com.wang17.myphone.buddha.play.auto"
    }
}
