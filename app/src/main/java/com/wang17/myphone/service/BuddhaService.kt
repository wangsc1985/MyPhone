package com.wang17.myphone.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.IBinder
import com.wang17.myphone.MainActivity
import com.wang17.myphone.R
import com.wang17.myphone.e
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.util.DataContext
import com.wang17.myphone.util._NotificationUtils
import com.wang17.myphone.util._Session
import com.wang17.myphone.util._Utils
import java.io.File
import java.util.*

class BuddhaService : Service() {

    private val NOTIFICATION_ID: Int = 12345
    private lateinit var dc: DataContext
    var startTimeInMillis: Long = 0


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
        e("buddha service onStartCommand")
    }

    override fun onCreate() {
        super.onCreate()
        e("buddha service onCreate")

        try {
            dc = DataContext(applicationContext)
            mAm = getSystemService(AUDIO_SERVICE) as AudioManager
            startTimeInMillis = System.currentTimeMillis()
            val buddhaSpeed = dc.getSetting(Setting.KEYS.buddha_speed, 2).int
            e("buddha speed : $buddhaSpeed")
            startMediaPlayer(buddhaSpeed)
            startTimer()
        } catch (e: Exception) {
            e("buddha service onCreate" + e.message)
        }
    }

    override fun onDestroy() {
        e("buddha service onDestroy")
        super.onDestroy()
        stopMediaPlayer()
        stopTimer()
        _NotificationUtils.closeNotification(applicationContext, NOTIFICATION_ID)
    }

    lateinit var timer: Timer
    fun startTimer() {
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val currentTimeMillis = System.currentTimeMillis()
                val miniteT = (currentTimeMillis - startTimeInMillis) / 1000 / 60
                val minite = miniteT % 60
                val hour = miniteT / 60
                var count = (miniteT / 12).toInt()
                var time = "$hour:${if (minite < 10) "0" + minite else minite}"
                sendNotification(count, time)
            }
        }, 0, 1000)
    }

    fun stopTimer() {
        timer.cancel()
    }


    private lateinit var mPlayer: MediaPlayer
    fun startMediaPlayer(velocity: Int) {
        try {
            requestFocus()
            mPlayer = MediaPlayer()
            val url = File(_Session.ROOT_DIR, "追顶念佛$velocity.mp3")
            mPlayer.reset() //把各项参数恢复到初始状态
            mPlayer.setDataSource(url.path)
            mPlayer.prepare() //进行缓冲
            mPlayer.setVolume(1f, 1f)
            mPlayer.setLooping(true)
            mPlayer.start()
            reOrStart()
        } catch (e: Exception) {
            e("start media player error : " + e.message!!)
            _Utils.printException(applicationContext, e)
        }
    }

    fun stopMediaPlayer() {
        mPlayer.stop()
        pauseOrStop()
    }

    private var mAm: AudioManager? = null
    private fun requestFocus(): Boolean {
        val result = try {
            mAm!!.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        } catch (e: Exception) {
            e(e.message!!)
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    var afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when(focusChange){
            /**
             * 当其他应用申请焦点之后又释放焦点会触发此回调
             * 可重新播放音乐
             */
            AudioManager.AUDIOFOCUS_GAIN->{
                e("永远获取焦点")
                mPlayer.start()
                mPlayer.setVolume(1f,1f)
                reOrStart()
            }
            /**
             * 长时间丢失焦点,当其他应用申请的焦点为AUDIOFOCUS_GAIN时，
             * 会触发此回调事件，例如播放QQ音乐，网易云音乐等
             * 通常需要暂停音乐播放，若没有暂停播放就会出现和其他音乐同时输出声音
             */
            AudioManager.AUDIOFOCUS_LOSS->{
                e("永远失去焦点")
                mPlayer.stop()
//                stopService(Intent(applicationContext,BuddhaService::class.java))
                stopSelf()
            }
            /**
             * 短暂性丢失焦点，当其他应用申请AUDIOFOCUS_GAIN_TRANSIENT或AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE时，
             * 会触发此回调事件，例如播放短视频，拨打电话等。
             * 通常需要暂停音乐播放
             */
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT->{
                e("暂时失去焦点")
                mPlayer.pause()
                pauseOrStop()
            }
            /**
             * 短暂性丢失焦点并作降音处理
             */
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK->{
                e("暂时失去焦点并降音")
                mPlayer.setVolume(0.2f,0.2f)
            }
        }
    }

    /**
     * 开始或重新开始要做的：
     * 1、记录当前section的startime
     */
    fun reOrStart() {
        e("start or restart")
        dc.editSetting(Setting.KEYS.buddha_startime, System.currentTimeMillis())
    }

    /**
     * 停止或暂停是要做的：
     * 1、将当前section的duration累加入数据库中的总的duration
     * 2、删除section的startime
     */
    fun pauseOrStop() {
        e("pause or stop")
        val now = System.currentTimeMillis()

        // 计算当前section的duration
        val settingStartTime = dc.getSetting(Setting.KEYS.buddha_startime)
        settingStartTime?.let {
            val duration = now - it.long

            // 获取之前的duration，并将两次的duration重新录入数据库
            val setting = dc.getSetting(Setting.KEYS.buddha_duration)
            dc.editSetting(Setting.KEYS.buddha_duration, duration + (setting?.long ?: 0L))

            // 删除startime
            dc.deleteSetting(Setting.KEYS.buddha_startime)
        }
    }

    // TODO: 2020/11/20 悬浮窗
    private fun sendNotification(count: Int, time: String) {
        _NotificationUtils.sendNotification(NOTIFICATION_ID, applicationContext, R.layout.notification_nf) { remoteViews ->
            remoteViews.setTextViewText(R.id.tv_count, count.toString())
            remoteViews.setTextViewText(R.id.tv_time, time)
            if (mPlayer.isPlaying) {
                remoteViews.setImageViewResource(R.id.image_control, R.drawable.pause)

                val intentRootClick = Intent("com.wang17.myphone.buddha.pause")
                val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intentRootClick, PendingIntent.FLAG_UPDATE_CURRENT)
                remoteViews.setOnClickPendingIntent(R.id.image_control, pendingIntent)
            }
            else {
                remoteViews.setImageViewResource(R.id.image_control, R.drawable.play)

                val intentRootClick = Intent("com.wang17.myphone.buddha.play")
                val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intentRootClick, PendingIntent.FLAG_UPDATE_CURRENT)
                remoteViews.setOnClickPendingIntent(R.id.image_control, pendingIntent)
            }
        }
    }
}