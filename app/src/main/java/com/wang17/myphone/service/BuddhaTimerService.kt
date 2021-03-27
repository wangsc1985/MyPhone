package com.wang17.myphone.service

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.wang17.myphone.R
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.event.NianfoOverEvent
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.database.Setting
import com.wang17.myphone.database.TallyRecord
import com.wang17.myphone.util.*
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.*


/**
 * 1、service
 * 2、timer
 * 3、media常开
 * 4、notification
 */

//@Deprecated("备用类", ReplaceWith(""),DeprecationLevel.HIDDEN)
class BuddhaTimerService : Service() {

    private var endTimeMillis: Long = 0
    private var startTimeMillis: Long = 0
    private var todaySavedTotalMillis = 0
    private var isSound = false

    private var timer = Timer()
    private var tag: Long = -1
    private var prevTag: Long = -1
    private var isRun = true

    private var mPlayer = MediaPlayer()
    private lateinit var mDataContext: DataContext
    private lateinit var myBroadcastReceiver: MyBroadcastReceiver


    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            mDataContext = DataContext(applicationContext)
            endTimeMillis = mDataContext.getSetting(Setting.KEYS.tally_endInMillis, 0).long
            startTimeMillis = mDataContext.getSetting(Setting.KEYS.tally_sectionStartMillis, 0).long
            todaySavedTotalMillis = getTodaySavedTotalMillis()
            isSound = intent.getBooleanExtra("isSound", false)

            startMediaPlayer()
            startTimer()

            myBroadcastReceiver = MyBroadcastReceiver()
            registerReceiver(myBroadcastReceiver, IntentFilter(ACTION_CLICK))
        } catch (e: Exception) {
            _Utils.printException(applicationContext, e)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun e(log: Any) {
        Log.e("wangsc", log.toString())
    }

    private fun getTodaySavedTotalMillis(): Int {
        try {
            var totalResult = 0
            val tallyRecords = mDataContext.getRecords(DateTime())
            for (rec in tallyRecords) {
                totalResult += rec.interval
            }
            return totalResult
        } catch (e: Exception) {
        }
        return 0
    }

    override fun onCreate() {
        super.onCreate()

    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        stopMediaPlayer()
        unregisterReceiver(myBroadcastReceiver);//销毁广播
        _NotificationUtils.closeNotification(applicationContext, NOTIFICATION_ID)
    }

    private fun startTimer() {

        timer.schedule(object : TimerTask() {
            override fun run() {
                try {
                    if (isRun) {
                        // 超时提醒
                        val span = endTimeMillis - System.currentTimeMillis()
                        tag = span / 1000 % 60
                        if (tag > prevTag) {
                            _NotificationUtils.sendNotification(NOTIFICATION_ID, applicationContext, R.layout.notification_nf) { remoteViews ->
                                val totalTime = todaySavedTotalMillis + (System.currentTimeMillis() - startTimeMillis)
                                val count = (totalTime / (60000 * 10)).toInt()
                                remoteViews.setTextViewText(R.id.tv_count, count.toString())
                                val hour = totalTime / (60000 * 60)
                                val minite = totalTime % (60000 * 60) / 60000
                                remoteViews.setTextViewText(R.id.tv_time, "${if (hour > 0) "${hour} : " else ""}${if(minite<0&&hour>0) "0$minite" else "$minite"}")

                                val contentIntent = PendingIntent.getActivity(applicationContext, 0, Intent(ACTION_CLICK), PendingIntent.FLAG_CANCEL_CURRENT)
                                remoteViews.setOnClickPendingIntent(R.id.image_control, contentIntent)
                                remoteViews.setImageViewResource(R.id.image_control,R.drawable.pause)
                            }
                        }
                        prevTag = tag
                        if (span <= 0) {
                            saveSection(applicationContext)
                            EventBus.getDefault().post(NianfoOverEvent())
                            _NotificationUtils.closeNotification(applicationContext, NOTIFICATION_ID)
                            /**
                             * 初始化声音
                             */
                            val mSoundPool = SoundPool(10, AudioManager.STREAM_MUSIC, 5)
                            mSoundPool.load(applicationContext, R.raw.clock, 1)
                            mSoundPool.setOnLoadCompleteListener { soundPool, sampleId, status -> mSoundPool.play(R.raw.ring, 1.0f, 1.0f, 0, 0, 0f) }
                            _Utils.speaker(applicationContext, "计时结束")
                            stopTimer()
                            stopMediaPlayer()
                        }
                    } else {
                        _NotificationUtils.sendNotification(NOTIFICATION_ID, applicationContext, R.layout.notification_nf) { remoteViews ->
//                            val totalTime = todaySavedTotalMillis
//                            val count = (totalTime / (60000 * 10)).toInt()
//                            remoteViews.setTextViewText(R.id.tv_count, count.toString())
//                            val hour = totalTime / (60000 * 60)
//                            val minite = totalTime % (60000 * 60) / 60000
//                            remoteViews.setTextViewText(R.id.tv_time, "${if (hour > 0) "${hour} : " else ""}${minite}")
//

                            val contentIntent = PendingIntent.getActivity(applicationContext, 0, Intent(ACTION_CLICK), PendingIntent.FLAG_CANCEL_CURRENT)
                            remoteViews.setOnClickPendingIntent(R.id.image_control, contentIntent)
                            remoteViews.setImageViewResource(R.id.image_control,R.drawable.play)
                        }
                    }
                } catch (e: Exception) {
                    _Utils.printException(applicationContext, e)
                }
            }
        }, 0, 1000)
    }

    inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Toast.makeText(context,"接收到广播",Toast.LENGTH_LONG).show()
            if (isRun) {
                pauseMediaPlayer()
                pauseNianfoTally()
                isRun = false
                _NotificationUtils.sendNotification(NOTIFICATION_ID, applicationContext, R.layout.notification_nf) { remoteViews ->
                    val intent = Intent(ACTION_CLICK)
                    val contentIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
                    remoteViews.setOnClickPendingIntent(R.id.image_control, contentIntent)
                    remoteViews.setImageViewResource(R.id.image_control,R.drawable.play)
                }
            } else {
                startMediaPlayer()
                restartNianfoTally()
                isRun = true
                _NotificationUtils.sendNotification(NOTIFICATION_ID, applicationContext, R.layout.notification_nf) { remoteViews ->
                    val intent = Intent(ACTION_CLICK)
                    val contentIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
                    remoteViews.setOnClickPendingIntent(R.id.image_control, contentIntent)
                    remoteViews.setImageViewResource(R.id.image_control,R.drawable.pause)
                }
            }
        }
    }

    private fun pauseMediaPlayer() {
        mPlayer.reset();
        mPlayer.setLooping(true)

        mPlayer = MediaPlayer.create(applicationContext, R.raw.second_10)
        mPlayer.setVolume(0.01f, 0.01f)

        mPlayer.prepare()
        mPlayer.start()
    }

    private fun stopTimer() {
        timer.cancel()
    }

    private fun stopMediaPlayer() {
        mPlayer.stop()
        mPlayer.release()
    }

    fun startMediaPlayer() {
        try {
            mPlayer.reset();
            mPlayer.setLooping(true)
            if (isSound) {
                val url = File(_Session.ROOT_DIR, mDataContext.getSetting(Setting.KEYS.buddha_music_name, "").string)
                mPlayer.setDataSource(url.path)
                mPlayer.setVolume(1f, 1f)
            } else {
                mPlayer = MediaPlayer.create(applicationContext, R.raw.second_10)
                mPlayer.setVolume(0.01f, 0.01f)
            }
            mPlayer.prepare()
            mPlayer.start()
        } catch (e: Exception) {
            _Utils.printException(applicationContext, e)
        }
    }

    private fun restartNianfoTally() {
        try {
            /**
             * 获取 tally_intervalInMillis 上次暂停时保存的，剩余的时间长度。
             * 删除 tally_intervalInMillis
             * 添加 tally_sectionStartInMillis
             * 添加 tally_endInMillis
             */
            // 删除
            val setting = mDataContext.getSetting(Setting.KEYS.tally_intervalInMillis)
            if (setting != null) {
                val interval = mDataContext.getSetting(Setting.KEYS.tally_intervalInMillis).string.toLong()
                mDataContext.deleteSetting(Setting.KEYS.tally_intervalInMillis)
                val sectionStartInMillis = System.currentTimeMillis()

                // 保存
                val endTimeInMillis = sectionStartInMillis + interval
                mDataContext.addSetting(Setting.KEYS.tally_endInMillis, endTimeInMillis)
                mDataContext.addSetting(Setting.KEYS.tally_sectionStartMillis, sectionStartInMillis)

                // 重新载入当天已入档的念佛总时间
                todaySavedTotalMillis = getTodaySavedTotalMillis()
            }
        } catch (e: Exception) {
            _Utils.printException(applicationContext, e)
        }
    }

    fun pauseNianfoTally() {
        try {
            /**
             * 删除 tally_endInMillis  目标时间
             * 删除 tally_sectionStartInMillis   开始时间
             *
             * 添加 tally_intervalInMillis    剩余时间长度
             */
            mDataContext.addLog("NianfoMusicService","pauseNianfoTally","")
            // 向数据库中保存“已经完成的时间”
            val now = System.currentTimeMillis()
            val sectionStartInMillis = mDataContext.getSetting(Setting.KEYS.tally_sectionStartMillis).string.toLong()
            val endTimeInMillis = mDataContext.getSetting(Setting.KEYS.tally_endInMillis).string.toLong()
            val sectionIntervalInMillis = now - sectionStartInMillis
            if (sectionIntervalInMillis >= 60000) {
                saveSection(sectionStartInMillis, sectionIntervalInMillis)
            }

            // 向数据库中保存“剩余时间”
            mDataContext.deleteSetting(Setting.KEYS.tally_sectionStartMillis)
            mDataContext.deleteSetting(Setting.KEYS.tally_endInMillis)
            var remainIntervalInMillis = endTimeInMillis - now
            if (sectionIntervalInMillis < 60000) {
                remainIntervalInMillis = endTimeInMillis - sectionStartInMillis
            }
            mDataContext.addSetting(Setting.KEYS.tally_intervalInMillis, remainIntervalInMillis)
        } catch (e: NumberFormatException) {
        }
    }

    private fun saveSection(partStartMillis: Long, partSpanMillis: Long) {
        try {
            mDataContext.addLog("NianfoMusicService","saveSection","")
            val tallyRecord = TallyRecord(DateTime(partStartMillis), partSpanMillis.toInt(), mDataContext.getSetting(Setting.KEYS.tally_record_item_text, "").string)
            if (mDataContext.getRecord(partStartMillis) == null) {
                mDataContext.addRecord(tallyRecord)
            }
        } catch (e: Exception) {
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 867
        private const val ACTION_CLICK = "com.wang17.myphone.CLICK_NF_NOTIFICATION"

        fun saveSection(context: Context?) {
            val mDataContext = DataContext(context)
            if (mDataContext.getSetting(Setting.KEYS.tally_endInMillis) != null) {
                //
                val sectionEndInMillis = mDataContext.getSetting(Setting.KEYS.tally_endInMillis).string.toLong()
                mDataContext.deleteSetting(Setting.KEYS.tally_endInMillis)
                val sectionStartInMillis = mDataContext.getSetting(Setting.KEYS.tally_sectionStartMillis).string.toLong()
                mDataContext.deleteSetting(Setting.KEYS.tally_sectionStartMillis)
                mDataContext.deleteSetting(Setting.KEYS.tally_manualOverTimeInMillis)
                val tallyRecord = TallyRecord(DateTime(sectionStartInMillis), (sectionEndInMillis - sectionStartInMillis).toInt(), mDataContext.getSetting(Setting.KEYS.tally_record_item_text, "").string)
                mDataContext.addRecord(tallyRecord)
                BackupTask(context).execute(BackupTask.COMMAND_BACKUP)
                mDataContext.editSetting(Setting.KEYS.tally_music_switch, false)
            }
        }
    }
}