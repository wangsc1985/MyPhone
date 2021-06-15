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
import com.wang17.myphone.model.ChannelName
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
import java.util.concurrent.CountDownLatch


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
    var setting_duration = 0L
    var notificationCount = 0
    var notificationTime = ""
    var notificationCountDay = 0
    var notificationTimeDay = ""
    var dbCount = 0
    var dbDuration = 0L

    var buddhaType = 11
    var pitch = 1.0f
    var speed = 1.0f
    var circleSecond = 600

    var yq_period = 1000L

    lateinit var yqSound: SoundPool
    lateinit var guSound: SoundPool
    var targetTap = 12

    var isShowFloatWindow = false
    var isAutoPause = false

    var topTitle = ""
    var bottomTitle = ""

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
        e("buddha service onStartCommand")
    }

    fun loadDb() {
        dbCount = 0
        dbDuration = 0
        val ls = dc.getBuddhas(DateTime())
        ls.forEach {
            dbCount += it.count
            dbDuration += it.duration
        }
        dbCount = dbCount / 1080
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate() {
        super.onCreate()
        e("buddha service onCreate")
        try {
            dc = DataContext(applicationContext)
            dc.deleteRunLog("BuddhaService", DateTime.today.addDays(-1))

            loadDb()

            isShowFloatWindow = dc.getSetting(Setting.KEYS.is显示念佛悬浮窗, false).boolean
            isAutoPause = dc.getSetting(Setting.KEYS.is念佛自动暂停, false).boolean
            targetTap = dc.getSetting(Setting.KEYS.几圈后自动结束念佛, 12).int

            yqSound = SoundPool(100, AudioManager.STREAM_MUSIC, 0)
            yqSound.load(this, R.raw.yq, 1)
            guSound = SoundPool(100, AudioManager.STREAM_MUSIC, 0)
            guSound.load(this, R.raw.gu, 1)
            val setting = dc.getSetting(Setting.KEYS.buddha_duration)
            setting?.let {
                setting_duration = setting.long
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
            filter.addAction(ACTION_BUDDHA_VOLUME_ADD)
            filter.addAction(ACTION_BUDDHA_VOLUME_MINUS)
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

    fun durationToTimeString(duration: Long): String {
        val second = duration % 60000 / 1000
        val miniteT = duration / 60000
        val minite = miniteT % 60
        val hour = miniteT / 60
        return "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}"
    }

    var prvDayOfYear = DateTime().get(Calendar.DAY_OF_YEAR)
    var tellTimeHour = -1
    fun startTimer() {
        timer?.cancel()
        timer = null
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                try {
                    val now = DateTime()
//                    e("${now.toTimeString()}")

                    if (now.minite == 0 && now.second < 5
                        && now.hour > 6 && now.hour < 24
                        && now.hour != tellTimeHour
                        && dc.getSetting(Setting.KEYS.is开启整点报时, true).boolean
                    ) {
                        _Utils.speaker(applicationContext, "${now.hour}点")
                        tellTimeHour = now.hour
                    }

//                    e("${notificationCount}  ${notificationTime}  ${notificationCountDay}  ${notificationTimeDay}  ${now.toTimeString()}")
                    val dayOfYear = now.get(Calendar.DAY_OF_YEAR)
                    if (isTimerRuning) {
                        //                    e("缓存duration : ${savedDuration/1000}秒  此段duration : ${(System.currentTimeMillis() - startTimeInMillis)/1000}秒   此段起始时间 : ${DateTime(startTimeInMillis).toTimeString()}")
                        val durationSection = setting_duration + System.currentTimeMillis() - startTimeInMillis
                        notificationCount = (durationSection / 1000 / circleSecond).toInt()
                        notificationTime = durationToTimeString(durationSection)
                        //                    e("db duration: ${durationToTimeString(dbDuration)} ; save duration: ${durationToTimeString(setting_duration)}  section duration:${durationToTimeString(System.currentTimeMillis() - startTimeInMillis)}")
                        val durationDay = dbDuration + setting_duration + System.currentTimeMillis() - startTimeInMillis
                        notificationCountDay = dbCount + notificationCount
                        notificationTimeDay = durationToTimeString(durationDay)

                        EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaServiceTimer(), durationSection.toString()))
                        tv_duration?.setText(notificationTime)

                        if (prvCount < notificationCount) {
                            if (dayOfYear != prvDayOfYear) {
                                checkSectionOnRunning()
                                guSound.play(1, 1.0f, 1.0f, 0, 0, 1.0f)
                                prvDayOfYear = dayOfYear
                            }

                            if (dc.getSetting(Setting.KEYS.is念佛整圈响引罄, true).boolean) {
                                dc.addRunLog("BuddhaService", "引罄", "${DateTime().toTimeString()}  ${notificationTime} prv count : ${prvCount} ; notification count : ${notificationCount}")
                                yqSound.play(1, 1.0f, 1.0f, 0, 0, 1.0f)
                                prvCount = notificationCount
                                if (notificationCount % 2 == 0) {
                                    Thread.sleep(yq_period)
                                    yqSound.play(1, 1.0f, 1.0f, 0, 0, 1.0f)
                                }
                            }
                            if (isAutoPause) {
                                chantBuddhaPause()
                                floatingWinButState(true)
                                mAm.abandonAudioFocus(afChangeListener)
                                dc.addRunLog("BuddhaService", "暂停念佛", "")
                            }
                        }

                        if (notificationCount > targetTap) {
                            stopSelf()
                        }

                    } else {
                        if (now.second == 1) {
                            if (dayOfYear != prvDayOfYear) {
                                if (setting_duration / 1000 / circleSecond >= 1) {
                                    checkSectionOnPause()
                                } else {
                                    loadDb()
                                }
                                val durationSection = setting_duration
                                notificationCount = (durationSection / 1000 / circleSecond).toInt()
                                notificationTime = durationToTimeString(durationSection)
                                val durationDay = dbDuration + setting_duration
                                notificationCountDay = dbCount + notificationCount
                                notificationTimeDay = durationToTimeString(durationDay)

                                prvDayOfYear = dayOfYear

                            } else if (cloudSaved < 5) {
                                checkSectionOnPause()
                            }
                        }
                    }

                    sendNotification(notificationCount, notificationCountDay, notificationTime, notificationTimeDay)
                } catch (e: Exception) {
                    dc.addRunLog("BuddhaService", "timer err", e.message)
                }
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
            dc.addRunLog("BuddhaService", "开始念佛", "")


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
            e("BuddhaService.requestFocus" + e.message!!)
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    //region 声音焦点
    var afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {

            /**
             * 当其他应用申请焦点之后又释放焦点会触发此回调
             * 可重新播放音乐
             */
            AudioManager.AUDIOFOCUS_GAIN -> {
                e("获取永久焦点")
                if (!AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    chantBuddhaRestart()
                } else {
                    AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = false
                }
                mPlayer?.setVolume(volume.toFloat(), volume.toFloat())
                floatingWinButState(false)
                sendNotification(notificationCount, notificationCountDay, notificationTime, notificationTimeDay)
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
                sendNotification(notificationCount, notificationCountDay, notificationTime, notificationTimeDay)
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
                sendNotification(notificationCount, notificationCountDay, notificationTime, notificationTimeDay)
            }
            /**
             * 短暂性丢失焦点并作降音处理
             */
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                e("暂时失去焦点并降音")
                mPlayer?.setVolume(0.3f, 0.3f)
                AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = true
//                pauseTimer()
//                pauseOrStopData()
            }
        }
    }
    //endregion

    var AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = false

    /**
     * 开始或重新开始要做的：
     * 1、记录当前section的startime
     */
    fun reOrStartData() {
        try {

            topTitle = dc.getSetting(Setting.KEYS.top_title, "南无阿弥陀佛").string
            bottomTitle = dc.getSetting(Setting.KEYS.bottom_title, "一门深入 长时薰修").string
            cloudSaved = 0

            startTimeInMillis = System.currentTimeMillis()
            dc.editSetting(Setting.KEYS.buddha_startime, startTimeInMillis)

            checkSectionBeforeRestart()

        } catch (e: Exception) {
            dc.addRunLog("err", "reOrStartData", e.message)
        }
    }

    var cloudSaved = 0
    private fun checkSectionOnPause() {
        try {
            val now = System.currentTimeMillis()

//        dc.addRunLog("checkSection", "进入check", "duration: ${durationToTimeString(setting_duration)}  stoptime: ${DateTime(setting_stoptime).toShortTimeString()}  count: ${setting_duration / 1000 / circleSecond}  ${(now - setting_stoptime)/60000}分钟")
            if (setting_stoptime > 0 && setting_duration / 1000 / circleSecond >= 1 && now - setting_stoptime > circleSecond * 1000) {
                cloudSaved++
                e("自动保存section${cloudSaved}")
                dc.addRunLog("BuddhaService", "自动保存section", "")
                val startTime = DateTime(setting_stoptime - setting_duration)
                val tap = (setting_duration / 1000 / circleSecond).toInt()
                val durationWhole = circleSecond * tap * 1000.toLong()
                var buddha = BuddhaRecord(startTime, durationWhole, tap * 1080, buddhaType.toBuddhaType(), "")

                var durationOdd = setting_duration - durationWhole

                val latch = CountDownLatch(1)
                _CloudUtils.addBuddha(applicationContext!!, buddha) { code, result ->
                    when (code) {
                        0 -> {
                            dc.addRunLog("BuddhaService", "云储存buddha成功", "${code}   ${result}")
                            dc.addBuddha(buddha)
                            prvCount = 0

                            setting_duration = durationOdd
                            notificationCount = 0
                            notificationTime = durationToTimeString(setting_duration)
                            dc.editSetting(Setting.KEYS.buddha_duration, setting_duration)

                            loadDb()

                            guSound.play(1, 1.0f, 1.0f, 0, 0, 1.0f)
                            cloudSaved = 5
                        }
                        else -> {
                            dc.addRunLog("err", "云储存buddha失败", "${code}   ${result}")
                            e("code : $code , result : $result")
                        }
                    }
                    latch.countDown()
                }
                latch.await()
            }
        } catch (e: Exception) {
            dc.addRunLog("err", "BuddhaService.checkSection", e.message)
        }
    }

    private fun checkSectionOnRunning() {
        try {
            val now = System.currentTimeMillis()

            val duration = setting_duration + now - startTimeInMillis
            notificationCount = (duration / 1000 / circleSecond).toInt()
            if (notificationCount >= 1) {

                dc.addRunLog("BuddhaService", "自动保存昨天section", "")
                val startTime = DateTime(now - duration)
                val tap = (duration / 1000 / circleSecond).toInt()
                val durationWhole = circleSecond * tap * 1000.toLong()
                var buddha = BuddhaRecord(startTime, durationWhole, tap * 1080, buddhaType.toBuddhaType(), "")

                var durationOdd = duration - durationWhole

                _CloudUtils.addBuddha(applicationContext!!, buddha) { code, result ->
                    when (code) {
                        0 -> {

                            dc.addRunLog("BuddhaService", "云储存buddha成功", "${code}   ${result}")
                            dc.addBuddha(buddha)
                            startTimeInMillis = System.currentTimeMillis() - durationOdd
                            dc.editSetting(Setting.KEYS.buddha_startime, startTimeInMillis)

                            prvCount = 0
                            loadDb()

                            guSound.play(1, 1.0f, 1.0f, 0, 0, 1.0f)
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
            dc.addRunLog("err", "BuddhaService.saveSection", e.message)
        }
    }

    private fun checkSectionBeforeRestart() {
        try {
            val now = System.currentTimeMillis()

            if (setting_stoptime > 0 && (setting_duration / 1000 / circleSecond) >= 1 && (now - setting_stoptime) > circleSecond * 1000) {

                dc.addRunLog("BuddhaService", "重启前保存section", "")
                val duration = setting_duration + now - startTimeInMillis
                notificationCount = (duration / 1000 / circleSecond).toInt()
                val startTime = DateTime(now - duration)
                val tap = (duration / 1000 / circleSecond).toInt()
                val durationWhole = circleSecond * tap * 1000.toLong()
                var buddha = BuddhaRecord(startTime, durationWhole, tap * 1080, buddhaType.toBuddhaType(), "")

                var durationOdd = duration - durationWhole

                _CloudUtils.addBuddha(applicationContext!!, buddha) { code, result ->
                    when (code) {
                        0 -> {

                            dc.addRunLog("BuddhaService", "云储存buddha成功", "${code}   ${result}")
                            dc.addBuddha(buddha)
                            startTimeInMillis = System.currentTimeMillis() - durationOdd
                            dc.editSetting(Setting.KEYS.buddha_startime, startTimeInMillis)

                            prvCount = 0
                            loadDb()

                            guSound.play(1, 1.0f, 1.0f, 0, 0, 1.0f)
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
            dc.addRunLog("err", "BuddhaService.saveSection", e.message)
        }
    }

    /**
     * 停止或暂停是要做的：
     * 1、将当前section的duration累加入数据库中的总的duration
     * 2、删除section的startime
     */
    var setting_stoptime: Long = 0
    fun pauseOrStopData() {
        try {
//            e("---------------- pauseOrStopData --------------------")
            val now = System.currentTimeMillis()

            // 计算当前section的duration
            val settingStartTime = dc.getSetting(Setting.KEYS.buddha_startime)
            settingStartTime?.let {
                setting_duration += now - startTimeInMillis
                setting_stoptime = now
                dc.editSetting(Setting.KEYS.buddha_duration, setting_duration)
                dc.editSetting(Setting.KEYS.buddha_stoptime, setting_stoptime)

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

    private fun sendNotification(count: Int, totalCount: Int, time: String, totalTime: String) {
        _NotificationUtils.sendNotification(applicationContext, ChannelName.老实念佛, ID, R.layout.notification_nf) { remoteViews ->
            remoteViews.setTextViewText(R.id.tv_count, if (buddhaType > 9) "${count}" else "")
            remoteViews.setTextViewText(R.id.tv_time, time)
            remoteViews.setTextViewText(R.id.tv_countTotal, if (buddhaType > 9) "${totalCount}" else "")
            remoteViews.setTextViewText(R.id.tv_timeTotal, totalTime)
            remoteViews.setTextViewText(R.id.tv_topTitle, topTitle)
            remoteViews.setTextViewText(R.id.tv_bottomTitle, bottomTitle)

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
            remoteViews.setOnClickPendingIntent(R.id.tv_time, pendingIntent1)
            remoteViews.setOnClickPendingIntent(R.id.tv_count, pendingIntent1)

            val intentRootClick2 = Intent(ACTION_BUDDHA_VOLUME_MINUS)
            val pendingIntent2 = PendingIntent.getBroadcast(applicationContext, 0, intentRootClick2, PendingIntent.FLAG_UPDATE_CURRENT)
            remoteViews.setOnClickPendingIntent(R.id.tv_timeTotal, pendingIntent2)

            val intentRootClick3 = Intent(ACTION_BUDDHA_VOLUME_ADD)
            val pendingIntent3 = PendingIntent.getBroadcast(applicationContext, 0, intentRootClick3, PendingIntent.FLAG_UPDATE_CURRENT)
            remoteViews.setOnClickPendingIntent(R.id.tv_countTotal, pendingIntent3)
            val autoPauseColor = resources.getColor(R.color.a, null)
            val normalColor = resources.getColor(R.color.cardview_dark_background, null)
            if (isAutoPause) {
                remoteViews.setTextColor(R.id.tv_count, autoPauseColor)
                remoteViews.setTextColor(R.id.tv_time, autoPauseColor)
                remoteViews.setTextColor(R.id.tv_countTotal, autoPauseColor)
                remoteViews.setTextColor(R.id.tv_timeTotal, autoPauseColor)
            } else {
                remoteViews.setTextColor(R.id.tv_count, normalColor)
                remoteViews.setTextColor(R.id.tv_time, normalColor)
                remoteViews.setTextColor(R.id.tv_countTotal, normalColor)
                remoteViews.setTextColor(R.id.tv_timeTotal, normalColor)
            }
        }
    }

    //region 消息处理
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onGetMessage(msg: EventBusMessage) {
        try {
            when (msg.sender) {
                is FromBuddhaConfigUpdate -> {
                    loadBuddhaConfig()

                    if (mPlayer?.isPlaying ?: false) {
                        var param = mPlayer?.playbackParams
                        param?.let {
                            e("pitch : ${msg.msg}")
                            param.pitch = pitch
                            param.speed = speed
                            mPlayer?.playbackParams = param
                        }
                    }
                }
                is ResetTimeEvent -> {
                    val duration = setting_duration + System.currentTimeMillis() - startTimeInMillis
                    val count = (duration / 1000 / circleSecond).toInt()
                    startTimeInMillis = System.currentTimeMillis() - (circleSecond * 1000 * count - setting_duration)
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
                is FromTotalCount -> {
                    loadDb()
                    val durationSection = setting_duration
                    notificationCount = (durationSection / 1000 / circleSecond).toInt()
                    notificationTime = durationToTimeString(durationSection)
                    val durationDay = dbDuration + setting_duration
                    notificationCountDay = dbCount + notificationCount
                    notificationTimeDay = durationToTimeString(durationDay)
                }
            }
        } catch (e: Exception) {
            dc.addRunLog("BuddhaService", "EventBus", e.message)
        }
    }

    //endregion
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
                            dc.addRunLog("BuddhaService", "暂停念佛", "")
                        } else {
                            if (requestFocus()) {
                                chantBuddhaRestart()
                                floatingWinButState(false)
                                dc.addRunLog("BuddhaService", "开始念佛", "")
                            }
                        }
                        sendNotification(notificationCount, notificationCountDay, notificationTime, notificationTimeDay)
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
                        ACTION_BUDDHA_VOLUME_ADD -> {
                            if (volume.toFloat() < 1.0) {
                                volume += "0.1".toBigDecimal()
                                e("声音加" + volume)
                                mPlayer?.setVolume(volume.toFloat(), volume.toFloat())
                                _Utils.zhendong70(context!!)
//                            Toast.makeText(context,volume.setScale(1,BigDecimal.ROUND_DOWN).toString(),Toast.LENGTH_SHORT).show()
                            }
                        }
                        ACTION_BUDDHA_VOLUME_MINUS -> {
                            if (volume.toFloat() > 0.2) {
                                volume -= "0.1".toBigDecimal()
                                e("声音减" + volume)
                                mPlayer?.setVolume(volume.toFloat(), volume.toFloat())
                                _Utils.zhendong70(context!!)
//                            Toast.makeText(context,volume.setScale(1,BigDecimal.ROUND_DOWN).toString(),Toast.LENGTH_SHORT).show()
                            }
                        }
                        ACTION_BUDDHA_PLAYE -> {
                            if (requestFocus()) {
                                chantBuddhaRestart()
                                floatingWinButState(false)
                                dc.addRunLog("BuddhaService", "开始念佛", "")
                            }
                        }
                        ACTION_BUDDHA_PLAYE_AUTO -> {
                            isAutoPause = !isAutoPause
                        }
                        ACTION_BUDDHA_PAUSE -> {
                            chantBuddhaPause()
                            floatingWinButState(true)
                            mAm.abandonAudioFocus(afChangeListener)
                            dc.addRunLog("BuddhaService", "暂停念佛", "")
                        }
                        BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                            val adapter = BluetoothAdapter.getDefaultAdapter()
                            e("接收到蓝牙广播")
                            if (BluetoothProfile.STATE_DISCONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET)) {
                                e("蓝牙耳机断开")
                                chantBuddhaPause()
                                floatingWinButState(true)
                                mAm.abandonAudioFocus(afChangeListener)
                                dc.addRunLog("BuddhaService", "耳机断开1", "")
                            }
                        }
                        AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                            chantBuddhaPause()
                            floatingWinButState(true)
                            mAm.abandonAudioFocus(afChangeListener)
                            dc.addRunLog("BuddhaService", "耳机断开2", "")
                        }
                    }

                    sendNotification(notificationCount, notificationCountDay, notificationTime, notificationTimeDay)
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
        val ACTION_BUDDHA_VOLUME_ADD = "com.wang17.myphone.buddha.vulume.add"
        val ACTION_BUDDHA_VOLUME_MINUS = "com.wang17.myphone.buddha.vulume.minus"
    }
}
