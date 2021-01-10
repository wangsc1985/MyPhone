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
import com.wang17.myphone.util.DataContext
import com.wang17.myphone.util._NotificationUtils
import com.wang17.myphone.util._Session
import com.wang17.myphone.util._Utils
import java.io.File
import java.util.*

//@Suppress("DEPRECATION")
/**
 *
 */
class BuddhaPlayerService : Service() {

    private val NOTIFICATION_ID: Int=12345
    private lateinit var mDataContext: DataContext
    var startTimeInMillis: Long = 0


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)

        e("buddha palyer service onStartCommand")
        val velocity = intent?.getIntExtra("velocity", 0)
        e("buddha palyer velocity ... ${velocity}")
        startTimeInMillis = System.currentTimeMillis()
        startMediaPlayer(velocity!!)
        startTimer()
    }

    override fun onCreate() {
        super.onCreate()
        e("buddha palyer service onCreate")
        mDataContext = DataContext(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMediaPlayer()
        stopTimer()
        _NotificationUtils.closeNotification(applicationContext,NOTIFICATION_ID)
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
            // TODO: 2020/11/20 源文件
            mPlayer = MediaPlayer()
            val url = File(_Session.ROOT_DIR, "追顶念佛$velocity.mp3")
            mPlayer.reset() //把各项参数恢复到初始状态
            mPlayer.setDataSource(url.path)
            mPlayer.prepare() //进行缓冲
            mPlayer.setVolume(1f, 1f)
            mPlayer.setLooping(true)
            mPlayer.start()
        } catch (e: Exception) {
            e(e.message!!)
            _Utils.printException(applicationContext, e)
        }
    }

    fun stopMediaPlayer() {
        mPlayer.stop()
    }

    private var mAm: AudioManager? = null
    private fun requestFocus(): Boolean {
        val result = mAm!!.requestAudioFocus(afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    var afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            // Pause playback
            mPlayer.pause()
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // Resume playback
            mPlayer.start()
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            // Stop playback
            mPlayer.stop()
        }
    }

    // TODO: 2020/11/20 悬浮窗

    // TODO: 2020/11/20 状态栏
    private fun sendNotification(count: Int, time: String) {
        _NotificationUtils.sendNotification(NOTIFICATION_ID, applicationContext, R.layout.notification_nf) { remoteViews ->
            remoteViews.setTextViewText(R.id.tv_count, count.toString())
            remoteViews.setTextViewText(R.id.tv_time, time)

            val intentRootClick = Intent(applicationContext, MainActivity::class.java)
            val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intentRootClick, PendingIntent.FLAG_UPDATE_CURRENT)
            remoteViews.setOnClickPendingIntent(R.id.tv_time, pendingIntent)
            remoteViews.setOnClickPendingIntent(R.id.tv_count, pendingIntent)
        }
    }
}