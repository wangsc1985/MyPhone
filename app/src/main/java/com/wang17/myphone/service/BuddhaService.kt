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
import com.wang17.myphone.dao.DataContext
import com.wang17.myphone.database.*
import com.wang17.myphone.e
import com.wang17.myphone.event.ResetTimeEvent
import com.wang17.myphone.eventbus.*
import com.wang17.myphone.format
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.ChannelName
import com.wang17.myphone.toBuddhaType
import com.wang17.myphone.util.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.jvm.Throws


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

    var isShowFloatWindow = false
    var isAutoPause = false

    var topTitle = ""
    var bottomTitle = ""
    var system_volumn = 10

    var setting_music_name:Setting? = null
    var cloudSaved = 0

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        kotlin.run aaa@{
            try {
                dc.addRunLog("BuddhaService", "启动念佛服务", "onStartCommand")
                dc.deleteRunLog("BuddhaService", DateTime.today.addDays(-1))
                topTitle = dc.getSetting(Setting.KEYS.top_title, "南无阿弥陀佛").string
                bottomTitle = dc.getSetting(Setting.KEYS.bottom_title, "一门深入 长时薰修").string

                loadDb()
//
                isShowFloatWindow = dc.getSetting(Setting.KEYS.is显示念佛悬浮窗, false).boolean
                isAutoPause = dc.getSetting(Setting.KEYS.is念佛自动暂停, false).boolean

                yqSound = SoundPool(100, AudioManager.STREAM_MUSIC, 0)
                yqSound.load(this, R.raw.yq, 1)

//                val set = dc.getSetting(Setting.KEYS.buddha_duration)
//                set?.let {
//                    setting_duration = set.long
//                }
                yq_period = dc.getSetting(Setting.KEYS.yq_period, 1000).long
                mAm = getSystemService(AUDIO_SERVICE) as AudioManager
                startTimeInMillis = System.currentTimeMillis()

                setting_music_name = dc.getSetting(Setting.KEYS.buddha_music_name)
                if (setting_music_name == null)
                    return@aaa

                loadBuddhaConfig()

                startBackgroundMediaPlayer()
                buddhaStart()

            } catch (e: Exception) {
                _Utils.printException(applicationContext,e)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @Throws(Exception::class)
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
        try {
            dc = DataContext(applicationContext)
//            dc.addRunLog("BuddhaService", "启动念佛服务", "onCreate")
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
            //
            EventBus.getDefault().register(this)

        } catch (e: Exception) {
            _Utils.printException(applicationContext,e)
        }
    }

    @Throws(Exception::class)
    fun setBuddhaVolume() {
        val volume = dc.getSetting(Setting.KEYS.念佛最佳音量)
        volume?.let {
            system_volumn = getMediaVolume()
            setMediaVolume(it.int)
        }
    }

    @Throws(Exception::class)
    private fun loadBuddhaConfig() {
        val musicName = dc.getSetting(Setting.KEYS.buddha_music_name)
        musicName?.let {
            val file = _Session.getFile(musicName.string)
            var bf = dc.getBuddhaConfig(musicName.string, file.length())
            if (bf == null) {
                bf = BuddhaConfig(musicName.string, file.length(), "md5", 1.0f, 1.0f, BuddhaType.计数念佛.toInt(), 600)
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
        try {
            dc.addRunLog("BuddhaService", "结束念佛服务", "onDestroy")
            dc.deleteSetting(Setting.KEYS.buddha_service_running)
            super.onDestroy()
            stopMediaPlayer()
            cancelTimer()
//        stopTimerMy()

            _NotificationUtils.closeNotification(applicationContext, ID)

            //
            mAm.abandonAudioFocus(afChangeListener)
            unregisterReceiver(buddhaReceiver)
            EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaServiceDestroy(), "buddha service destroyed"))
            //region 悬浮窗
            if (isShowFloatWindow) {
                windowManager.removeView(floatingWindowView)
            }
            EventBus.getDefault().unregister(this)

//            stopForeground(true)
//            dc.deleteSetting(Setting.KEYS.tmp_tt)
        } catch (e: Exception) {
            _Utils.printException(applicationContext,e)
        }
    }

    var prvCount = 0
    var isTimerRuning = true
    var timer: Timer? = null

    @Throws(Exception::class)
    fun durationToTimeString(duration: Long): String {
        val second = duration % 60000 / 1000
        val miniteT = duration / 60000
        val minite = miniteT % 60
        val hour = miniteT / 60
        return "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}"
    }

    var prvDayOfYear = DateTime().get(Calendar.DAY_OF_YEAR)
    var tellTimeHour = -1

    //    var tmpCC = -1L
    fun startTimer() {
        timer?.cancel()
        timer = null
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                try {
                    val now = DateTime()
                    dc.editSetting(Setting.KEYS.tmp_tt,now.timeInMillis)

                    if (now.minite == 0 && now.second < 5
                        && now.hour > 6 && now.hour < 24
                        && now.hour != tellTimeHour
                        && dc.getSetting(Setting.KEYS.is开启整点报时, true).boolean
                    ) {
                        _Utils.speaker(applicationContext, "${now.hour}点")
                        tellTimeHour = now.hour
                    }

                    val dayOfYear = now.get(Calendar.DAY_OF_YEAR)
                    if (isTimerRuning) {
                        val durationSection = setting_duration + System.currentTimeMillis() - startTimeInMillis
                        notificationCount = (durationSection / 1000 / circleSecond).toInt()
                        notificationTime = durationToTimeString(durationSection)
                        val durationDay = dbDuration + setting_duration + System.currentTimeMillis() - startTimeInMillis
                        notificationCountDay = dbCount + notificationCount
                        notificationTimeDay = durationToTimeString(durationDay)

                        EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaServiceTimer(), durationSection.toString()))
                        tv_duration?.setText(notificationTime)

                        if (prvCount < notificationCount) {
                            dc.editSetting(Setting.KEYS.buddha_prv_count,notificationCount)
                            if (dayOfYear != prvDayOfYear) {
                                checkSectionOnRunning()
                                _SoundUtils.play(applicationContext,R.raw.gu,_SoundUtils.SoundType.MUSIC)
                                dc.deleteRunLog("BuddhaService", DateTime.today.addDays(-1))
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
                                buddhaPause()
                                floatingWinButState(WinBtnState.点击播放)
                                mAm.abandonAudioFocus(afChangeListener)
                                dc.addRunLog("BuddhaService", "暂停念佛", "自动结束")
                                Thread {
                                    Thread.sleep(3000)
                                    setMediaVolume(system_volumn)
                                }.start()
                            }
                            if (notificationCount > dc.getSetting(Setting.KEYS.几圈后自动结束念佛, 12).int) {
                                stopSelf()
                            }
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
                    _Utils.printException(applicationContext,e)
                }
            }
        }, 0, 1000)
    }

    @Throws(Exception::class)
    fun restartTimer() {
        isTimerRuning = true
    }

    @Throws(Exception::class)
    fun pauseTimer() {
        isTimerRuning = false
    }

    @Throws(Exception::class)
    fun cancelTimer() {
        timer?.cancel()
    }

    @Throws(Exception::class)
    fun setMediaVolume(volume: Int) {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND or AudioManager.FLAG_SHOW_UI)
    }

    @Throws(Exception::class)
    fun getMediaVolume(): Int {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    private var mPlayer: MediaPlayer? = null
    private var mBackgroundPlayer: MediaPlayer? = null

    @RequiresApi(Build.VERSION_CODES.M)
    @Throws(Exception::class)
    fun startMediaPlayer(musicName: String) {
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
            mPlayer?.pause()
        }
    }

    @Throws(Exception::class)
    fun startBackgroundMediaPlayer() {
        mBackgroundPlayer = MediaPlayer.create(applicationContext, R.raw.second_60)
        mBackgroundPlayer?.setVolume(0.01f, 0.01f)
        mBackgroundPlayer?.setLooping(true)
        mBackgroundPlayer?.start()
    }

    fun stopMediaPlayer() {
        mBackgroundPlayer?.stop()
        buddhaStop()
        mPlayer?.stop()
    }

    private lateinit var mAm: AudioManager
    private fun requestFocus(): Boolean {
        val result = try {
            mAm.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        } catch (e: Exception) {
            _Utils.printException(applicationContext,e)
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    //region 声音焦点
    var afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->

        try {
            when (focusChange) {

                /**
                 * 当其他应用申请焦点之后又释放焦点会触发此回调
                 * 可重新播放音乐
                 */
                AudioManager.AUDIOFOCUS_GAIN -> {
                    e("获取永久焦点")
                    if (!AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        buddhaRestart()
                    } else {
                        AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = false
                    }
                    mPlayer?.setVolume(volume.toFloat(), volume.toFloat())
                    floatingWinButState(WinBtnState.点击暂停)
                    sendNotification(notificationCount, notificationCountDay, notificationTime, notificationTimeDay)
                }
                /**
                 * 长时间丢失焦点,当其他应用申请的焦点为AUDIOFOCUS_GAIN时，
                 * 会触发此回调事件，例如播放QQ音乐，网易云音乐等
                 * 通常需要暂停音乐播放，若没有暂停播放就会出现和其他音乐同时输出声音
                 */
                AudioManager.AUDIOFOCUS_LOSS -> {
                    e("永久失去焦点")
                    buddhaPause()
                    floatingWinButState(WinBtnState.点击播放)
                    sendNotification(notificationCount, notificationCountDay, notificationTime, notificationTimeDay)
                }
                /**
                 * 短暂性丢失焦点，当其他应用申请AUDIOFOCUS_GAIN_TRANSIENT或AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE时，
                 * 会触发此回调事件，例如播放短视频，拨打电话等。
                 * 通常需要暂停音乐播放
                 */
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    e("暂时失去焦点")
                    buddhaPause()
                    floatingWinButState(WinBtnState.点击播放)
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
        } catch (e: Exception) {
            _Utils.printException(applicationContext,e)
        }
    }
    //endregion

    var AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = false

    /**
     * 开始或重新开始要做的：
     * 1、记录当前section的startime
     */
    fun dataReOrStart() {
        try {

            topTitle = dc.getSetting(Setting.KEYS.top_title, "南无阿弥陀佛").string
            bottomTitle = dc.getSetting(Setting.KEYS.bottom_title, "一门深入 长时薰修").string
            cloudSaved = 0

            startTimeInMillis = System.currentTimeMillis()
            dc.editSetting(Setting.KEYS.buddha_startime, startTimeInMillis)

            checkSectionBeforeRestart()

        } catch (e: Exception) {
            _Utils.printException(applicationContext,e)
        }
    }

    private fun checkSectionOnPause() {
        try {
            val now = System.currentTimeMillis()

//        dc.addRunLog("checkSection", "进入check", "duration: ${durationToTimeString(setting_duration)}  stoptime: ${DateTime(setting_stoptime).toShortTimeString()}  count: ${setting_duration / 1000 / circleSecond}  ${(now - setting_stoptime)/60000}分钟")
            if (setting_stoptime > 0 && setting_duration / 1000 / circleSecond >= 1 && now - setting_stoptime > circleSecond * 1000) {
                cloudSaved++
                e("自动保存section${cloudSaved}")
                val startTime = DateTime(setting_stoptime - setting_duration)
                val tap = (setting_duration / 1000 / circleSecond).toInt()
                val durationWhole = circleSecond * tap * 1000.toLong()
                var buddha = BuddhaRecord(startTime, durationWhole, tap * 1080, buddhaType.toBuddhaType(), "")

                var durationOdd = setting_duration - durationWhole

                val latch = CountDownLatch(1)
                _CloudUtils.addBuddha(applicationContext!!, buddha) { code, result ->
                    when (code) {
                        0 -> {
                            dc.addRunLog("BuddhaService", "自动保存section，云储存buddha成功", "${code}   ${result}")
                            dc.addBuddha(buddha)
                            prvCount = 0
                            dc.editSetting(Setting.KEYS.buddha_prv_count,0)

                            setting_duration = durationOdd
                            notificationCount = 0
                            notificationTime = durationToTimeString(setting_duration)
                            dc.editSetting(Setting.KEYS.buddha_duration, setting_duration)

                            loadDb()

                            _SoundUtils.play(applicationContext,R.raw.gu,_SoundUtils.SoundType.MUSIC)
                            cloudSaved = 5
                            if(!dc.getSetting(Setting.KEYS.is保持念佛服务,true).boolean){
                                stopSelf()
                            }
                        }
                        else -> {
                            dc.addRunLog("err", "自动保存section，云储存buddha失败", "${code}   ${result}")
                            e("code : $code , result : $result")
                        }
                    }
                    latch.countDown()
                }
                latch.await()
            }
        } catch (e: Exception) {
            _Utils.printException(applicationContext,e)
        }
    }

    private fun checkSectionOnRunning() {
        try {
            val now = System.currentTimeMillis()

            val duration = setting_duration + now - startTimeInMillis
            notificationCount = (duration / 1000 / circleSecond).toInt()
            if (notificationCount >= 1) {

                val startTime = DateTime(now - duration)
                val tap = (duration / 1000 / circleSecond).toInt()
                val durationWhole = circleSecond * tap * 1000.toLong()
                var buddha = BuddhaRecord(startTime, durationWhole, tap * 1080, buddhaType.toBuddhaType(), "")

                var durationOdd = duration - durationWhole

                _CloudUtils.addBuddha(applicationContext!!, buddha) { code, result ->
                    when (code) {
                        0 -> {

                            dc.addRunLog("BuddhaService", "自动保存昨天section，云储存buddha成功", "${code}   ${result}")
                            dc.addBuddha(buddha)
                            startTimeInMillis = System.currentTimeMillis() - durationOdd
                            dc.editSetting(Setting.KEYS.buddha_startime, startTimeInMillis)

                            prvCount = 0
                            dc.editSetting(Setting.KEYS.buddha_prv_count,0)
                            loadDb()

                            _SoundUtils.play(applicationContext,R.raw.gu,_SoundUtils.SoundType.MUSIC)
                            Looper.prepare()
                            Toast.makeText(applicationContext, result.toString(), Toast.LENGTH_LONG).show()
                            Looper.loop()
                        }
                        else -> {
                            dc.addRunLog("err", "自动保存昨天section，云储存buddha失败", "${code}   ${result}")
                            e("code : $code , result : $result")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _Utils.printException(applicationContext,e)
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

                            dc.addRunLog("BuddhaService", "重启前保存section，云储存buddha成功", "${code}   ${result}")
                            dc.addBuddha(buddha)
                            startTimeInMillis = System.currentTimeMillis() - durationOdd
                            dc.editSetting(Setting.KEYS.buddha_startime, startTimeInMillis)

                            prvCount = 0
                            dc.editSetting(Setting.KEYS.buddha_prv_count,0)
                            loadDb()

                            _SoundUtils.play(applicationContext,R.raw.gu,_SoundUtils.SoundType.MUSIC)
                            Looper.prepare()
                            Toast.makeText(applicationContext, result.toString(), Toast.LENGTH_LONG).show()
                            Looper.loop()
                        }
                        else -> {
                            dc.addRunLog("err", "重启前保存section云储存buddha失败", "${code}   ${result}")
                            e("code : $code , result : $result")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _Utils.printException(applicationContext,e)
        }
    }

    /**
     * 停止或暂停是要做的：
     * 1、将当前section的duration累加入数据库中的总的duration
     * 2、删除section的startime
     */
    var setting_stoptime: Long = 0
    fun dataPauseOrStop() {
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
            _Utils.printException(applicationContext,e)
        }
    }

    @Throws(Exception::class)
    fun buddhaStart() {
        startMediaPlayer(setting_music_name!!.string)
        val set_running = dc.getSetting(Setting.KEYS.buddha_service_running)
        if (set_running != null) {
            prvCount = dc.getSetting(Setting.KEYS.buddha_prv_count,0).int
            val tt = ((System.currentTimeMillis()-dc.getSetting(Setting.KEYS.tmp_tt,System.currentTimeMillis()).long).toFloat()/1000).format(3)+"秒"
            _SoundUtils.play(applicationContext,R.raw.piu,_SoundUtils.SoundType.MUSIC)
            // 说明服务被异常结束，因为正常结束时，这个标志已经被删除了
            val set_startime = dc.getSetting(Setting.KEYS.buddha_startime)
            if (set_startime != null) {
                // 说明服务被异常结束时，程序正是念佛时。因为在正常结束时，这个标记已经被删除了。
                if (requestFocus()) {

                    dc.addRunLog("BuddhaService", "buddhaStart()", "服务被异常销毁${tt}后再次启动，并延续念佛状态")
                    //
                    mPlayer?.start()
                    startTimeInMillis = set_startime.long
                    //
                    restartTimer()
                    setBuddhaVolume()
                }
            } else {
                // 说明服务被异常结束时，程序正是暂停时。
                dc.addRunLog("BuddhaService", "buddhaStart()", "服务被异常销毁${tt}后再次启动，并继续保持暂停状态")
                //
                mPlayer?.pause()
                pauseTimer()
                //
                mAm.abandonAudioFocus(afChangeListener)
                floatingWinButState(WinBtnState.点击播放)
                //
                val durationSection = setting_duration
                notificationCount = (durationSection / 1000 / circleSecond).toInt()
                notificationTime = durationToTimeString(durationSection)
                val durationDay = dbDuration + setting_duration
                notificationCountDay = dbCount + notificationCount
                notificationTimeDay = durationToTimeString(durationDay)
            }

        } else {
            if (requestFocus()) {
                dc.editSetting(Setting.KEYS.buddha_prv_count,0)
                mPlayer?.start()
                dataReOrStart()
                restartTimer()
                setBuddhaVolume()
            }
        }
        //
        dc.getSetting(Setting.KEYS.buddha_duration)?.let {
            setting_duration = it.long
            dc.deleteSetting(Setting.KEYS.buddha_duration)
        }
        dc.getSetting(Setting.KEYS.buddha_stoptime)?.let {
            setting_stoptime = it.long
            dc.deleteSetting(Setting.KEYS.buddha_stoptime)
        }
        startTimer()
        showFloatingWindow()
        dc.editSetting(Setting.KEYS.buddha_service_running, true)
    }

    @Throws(Exception::class)
    fun buddhaRestart() {
        mPlayer?.start()
        dataReOrStart()
        restartTimer()
    }

    @Throws(Exception::class)
    fun buddhaPause() {
        pauseTimer()
        mPlayer?.pause()
        dataPauseOrStop()
    }

    @Throws(Exception::class)
    fun buddhaStop() {
        pauseTimer()
        mPlayer?.pause()
        dataPauseOrStop()
    }

    private fun sendNotification(count: Int, totalCount: Int, time: String, totalTime: String) {
        val notification = _NotificationUtils.sendNotification(applicationContext, ChannelName.老实念佛,ID, R.layout.notification_nf) { remoteViews ->
            try {
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
            } catch (e: Exception) {
                _Utils.printException(applicationContext,e)
            }
        }
//        startForeground(1, notification)
    }

    //region 消息处理-EventBus
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
            _Utils.printException(applicationContext,e)
        }
    }

    //endregion
    //region 悬浮窗
    @Throws(Exception::class)
    private fun floatingWinButState(sate: WinBtnState) {
        floatingWindowView?.let {
            val iv_control = it.findViewById<ImageView>(R.id.iv_control)

            if (sate == WinBtnState.点击播放) {
                iv_control.setImageResource(R.drawable.play)
            } else {
                iv_control.setImageResource(R.drawable.pause)
            }
        }
    }

    enum class WinBtnState {
        点击播放, 点击暂停
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
                            buddhaPause()
                            floatingWinButState(WinBtnState.点击播放)
                            mAm.abandonAudioFocus(afChangeListener)
                            dc.addRunLog("BuddhaService", "暂停念佛", "通知栏-暂停")
                            setMediaVolume(system_volumn)
                        } else {
                            if (requestFocus()) {
                                setBuddhaVolume()
                                buddhaRestart()
                                floatingWinButState(WinBtnState.点击暂停)
                                dc.addRunLog("BuddhaService", "开始念佛", "通知栏-开始")
                            }
                        }
                        sendNotification(notificationCount, notificationCountDay, notificationTime, notificationTimeDay)
                    }
                }
                iv_control?.setOnTouchListener(FloatingOnTouchListener())
                windowManager.addView(floatingWindowView, layoutParams)
            }
        } catch (e: Exception) {
            _Utils.printException(applicationContext,e)
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

    //region 接收器-Receiver
    inner class BuddhaReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                intent?.let {
                    when (it.action) {
                        ACTION_BUDDHA_VOLUME_ADD -> {
                            mPlayer?.let { player ->
                                if (player.isPlaying && volume.toFloat() < 1.0) {
                                    volume += "0.1".toBigDecimal()
                                    e("声音加" + volume)
                                    player.setVolume(volume.toFloat(), volume.toFloat())
                                    _SoundUtils.play(context!!, R.raw.bi, 1.0f, _SoundUtils.SoundType.MUSIC)
                                }
                            }
                        }
                        ACTION_BUDDHA_VOLUME_MINUS -> {
                            mPlayer?.let {player->
                                if (player.isPlaying && volume.toFloat() > 0.2) {
                                    volume -= "0.1".toBigDecimal()
                                    e("声音减" + volume)
                                    player.setVolume(volume.toFloat(), volume.toFloat())
                                    _SoundUtils.play(context!!, R.raw.bi, 1.0f,_SoundUtils.SoundType.MUSIC)
                                }
                            }
                        }
                        ACTION_BUDDHA_PLAYE -> {
                            if (requestFocus()) {
                                setBuddhaVolume()
                                buddhaRestart()
                                floatingWinButState(WinBtnState.点击暂停)
                                dc.addRunLog("BuddhaService", "开始念佛", "通知栏-开始")
                            }
                        }
                        ACTION_BUDDHA_PLAYE_AUTO -> {
                            isAutoPause = !isAutoPause
                            dc.editSetting(Setting.KEYS.is念佛自动暂停,isAutoPause)
                        }
                        ACTION_BUDDHA_PAUSE -> {
                            buddhaPause()
                            floatingWinButState(WinBtnState.点击播放)
                            mAm.abandonAudioFocus(afChangeListener)
                            dc.addRunLog("BuddhaService", "暂停念佛", "通知栏-暂停")
                            setMediaVolume(system_volumn)
                        }
                        BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                            val adapter = BluetoothAdapter.getDefaultAdapter()
                            e("接收到蓝牙广播")
                            if (BluetoothProfile.STATE_DISCONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET)) {
                                e("蓝牙耳机断开")
                                buddhaPause()
                                floatingWinButState(WinBtnState.点击播放)
                                mAm.abandonAudioFocus(afChangeListener)
                            }
                        }
                        AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                            buddhaPause()
                            floatingWinButState(WinBtnState.点击播放)
                            mAm.abandonAudioFocus(afChangeListener)
                        }
                    }

                    sendNotification(notificationCount, notificationCountDay, notificationTime, notificationTimeDay)
                }
            } catch (e: Exception) {
                _Utils.printException(applicationContext,e)
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
