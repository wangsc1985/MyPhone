package com.wang17.myphone.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import com.wang17.myphone.MainActivity
import com.wang17.myphone.R
import com.wang17.myphone.event.NianfoPauseEvent
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.database.Setting
import com.wang17.myphone.database.TallyRecord
import com.wang17.myphone.receiver.AlarmReceiver
import com.wang17.myphone.receiver.HeadsetPlugReceiver
import com.wang17.myphone.util.*
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File

class NianfoMusicService : Service() {
    private lateinit var mDataContext: DataContext
    private lateinit var mPlayer: MediaPlayer
    private lateinit var timerPlayer:MediaPlayer

    private var startTimeMillis:Long = System.currentTimeMillis()

    private var todaySavedTotalMillis = 0
    private var mAm: AudioManager? = null
    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            play()
        } catch (e: Exception) {
            _Utils.printException(applicationContext, e)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        Log.e("wangsc", "nianfo music service on create...")
        wakeLock = _Utils.acquireWakeLock(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        Log.e("wangsc", "wakeLock:" + wakeLock)
        mDataContext = DataContext(applicationContext)
        mPlayer = MediaPlayer()
        timerPlayer = MediaPlayer.create(getApplicationContext(), R.raw.second_10)
        mAm = getSystemService(AUDIO_SERVICE) as AudioManager
        /**
         * 耳机监听
         */
        reciver = HeadsetPlugReceiver()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_HEADSET_PLUG)
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
//        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
//        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(reciver, filter)


        startTimeMillis = mDataContext.getSetting(Setting.KEYS.tally_sectionStartMillis, System.currentTimeMillis()).long
        todaySavedTotalMillis = getTodaySavedTotalInMillis()
    }

    private fun getTodaySavedTotalInMillis(): Int {
        try {
            val dataContext = DataContext(applicationContext)
            var totalResult = 0
            val tallyRecords = dataContext.getRecords(DateTime())
            for (rec in tallyRecords) {
                totalResult += rec.interval
            }
            return totalResult
        } catch (e: Exception) {
        }
        return 0
    }

    private var reciver: HeadsetPlugReceiver? = null
    override fun onDestroy() {
        _Utils.releaseWakeLock(applicationContext, wakeLock)

        mPlayer.stop()
        mPlayer.release()

        timerPlayer.stop()
        timerPlayer.release()

        mDataContext.editSetting(Setting.KEYS.tally_music_is_playing, false)
        mAm!!.abandonAudioFocus(afChangeListener)
        /**
         * 耳机监听
         */
        unregisterReceiver(reciver)
        super.onDestroy()
    }

    fun saveCurrentPosition(position: Int) {
        try {
            val str = mDataContext.getSetting(Setting.KEYS.media_player, "").string
            val fileName = mDataContext.getSetting(Setting.KEYS.buddha_music_name, "").string
            if (!str.isEmpty()) {
                val jsonArray = JSONArray(str)
                var retJsonObject: JSONObject? = null
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val file = jsonObject.getString("file")
                    if (file == fileName) {
                        retJsonObject = jsonObject
                        jsonObject.put("position", position)
                        break
                    }
                }
                /**
                 * 如果json字符串中没有这个文件。
                 */
                if (retJsonObject == null) {
                    retJsonObject = JSONObject()
                    retJsonObject.put("file", fileName)
                    retJsonObject.put("position", position)
                    retJsonObject.put("speed", "1.0")
                    retJsonObject.put("pitch", "1.0")
                    retJsonObject.put("volumn", "1.0")
                    jsonArray.put(retJsonObject)
                }
                mDataContext.editSetting(Setting.KEYS.media_player, jsonArray.toString())
            } else {
                /**
                 * 如果json字符串是空的。
                 */
                val jsonArray = JSONArray()
                val retJsonObject = JSONObject()
                retJsonObject.put("file", fileName)
                retJsonObject.put("position", position)
                retJsonObject.put("speed", "1.0")
                retJsonObject.put("pitch", "1.0")
                retJsonObject.put("volumn", "1.0")
                jsonArray.put(retJsonObject)
                mDataContext.editSetting(Setting.KEYS.media_player, jsonArray.toString())
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun play() {
        try {
            if (requestFocus()) {
                var volumn = mDataContext.getSetting(Setting.KEYS.media_player_volumn, 1.0f).float
                var speed = mDataContext.getSetting(Setting.KEYS.media_player_speed, 1.0f).float
                var pitch = mDataContext.getSetting(Setting.KEYS.media_player_pitch, 1.0f).float
                var position = 0
                val str = mDataContext.getSetting(Setting.KEYS.media_player, "").string
                val fileName = mDataContext.getSetting(Setting.KEYS.buddha_music_name, "").string
                Log.e("wangsc", "$fileName is playing...")
                if (!str.isEmpty()) {
                    val jsonArray = JSONArray(str)
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val file = jsonObject.getString("file")
                        if (file == fileName) {
                            speed = jsonObject.getString("speed").toFloat()
                            volumn = jsonObject.getString("volumn").toFloat()
                            pitch = jsonObject.getString("pitch").toFloat()
                            position = jsonObject.getString("position").toInt()
                            break
                        }
                    }
                }
                val url = File(_Session.ROOT_DIR, mDataContext.getSetting(Setting.KEYS.buddha_music_name, "").string)


                mPlayer.reset() //把各项参数恢复到初始状态
                mPlayer.setDataSource(url.path)
                mPlayer.prepare() //进行缓冲
//                mPlayer.seekTo(position)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mPlayer.playbackParams = mPlayer.playbackParams.setSpeed(speed)
                    mPlayer.playbackParams = mPlayer.playbackParams.setPitch(pitch)
                }
                mPlayer.setVolume(volumn, volumn)
                mPlayer.isLooping = true
                mPlayer.start()


                mPlayer.reset() //把各项参数恢复到初始状态
                mPlayer.prepare() //进行缓冲
                mPlayer.setVolume(0.01f, 0.01f)
                mPlayer.isLooping = false
                mPlayer.setOnCompletionListener(object:MediaPlayer.OnCompletionListener{
                    override fun onCompletion(mp: MediaPlayer?) {
                        val totalTime = todaySavedTotalMillis+(System.currentTimeMillis()-startTimeMillis)
                        val count = (totalTime/(60000*10)).toInt()
                        sendNotification(count,"${totalTime/(60000*60)}小时${totalTime%(60000*60)/60000}分钟")
                        mp!!.start()
                    }
                })
                mPlayer.start()

                val endTimeInMillis = mDataContext.getSetting(Setting.KEYS.tally_endInMillis).string.toLong()
                //                sendNotification(new DateTime(endTimeInMillis).toTimeString());
            }
            mDataContext.editSetting(Setting.KEYS.tally_music_is_playing, true)
        } catch (e: Exception) {
            _Utils.printException(applicationContext, e)
        }
    }

    fun pause() {
            mPlayer.pause()
            mDataContext.editSetting(Setting.KEYS.media_player_position, mPlayer.currentPosition)
            mDataContext.editSetting(Setting.KEYS.tally_music_is_playing, false)

        if (mDataContext.getSetting(Setting.KEYS.tally_endInMillis) != null) {
            stopAlarm(applicationContext)
            pauseNianfoTally(applicationContext)
            EventBus.getDefault().post(NianfoPauseEvent())
        }
        //        mAm.abandonAudioFocus(afChangeListener);
//        sendNotification("暂停");
    }

    fun resume() {
            mPlayer.start()

        mDataContext.editSetting(Setting.KEYS.tally_music_is_playing, true)
        restartNianfoTally()
        val endTimeInMillis = mDataContext.getSetting(Setting.KEYS.tally_endInMillis).string.toLong()
        //        sendNotification(new DateTime(endTimeInMillis).toTimeString());
    }

    fun stop() {
        try {

                mDataContext.editSetting(Setting.KEYS.media_player_position, mPlayer.currentPosition)
                mPlayer.stop()
                mPlayer.release()
            
            mDataContext.editSetting(Setting.KEYS.tally_music_is_playing, false)
            if (mDataContext.getSetting(Setting.KEYS.tally_endInMillis) != null) {
                stopAlarm(applicationContext)
                pauseNianfoTally(applicationContext)
                EventBus.getDefault().post(NianfoPauseEvent())
            }
            mAm!!.abandonAudioFocus(afChangeListener)
        } catch (e: IllegalStateException) {
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
//            todaySavedTotalInMillis = getTodaySavedTotalInMillis();

                // 重新设定闹钟
                startAlarm(applicationContext, endTimeInMillis)

                // 启动服务
                play()
                //                showNotification();
            }
        } catch (e: Exception) {
            _Utils.printException(applicationContext, e)
        }
    }

    fun pauseNianfoTally(context: Context) {
        try {
            /**
             * 删除 tally_endInMillis  目标时间
             * 删除 tally_sectionStartInMillis   开始时间
             *
             * 添加 tally_intervalInMillis    剩余时间长度
             */
            // 向数据库中保存“已经完成的时间”
            val now = System.currentTimeMillis()
            val sectionStartInMillis = mDataContext.getSetting(Setting.KEYS.tally_sectionStartMillis).string.toLong()
            val endTimeInMillis = mDataContext.getSetting(Setting.KEYS.tally_endInMillis).string.toLong()
            val sectionIntervalInMillis = now - sectionStartInMillis
            if (sectionIntervalInMillis >= 60000) {
                saveSection(context, sectionStartInMillis, sectionIntervalInMillis)
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
            _Utils.printException(context, e)
        }
    }

    private fun saveSection(context: Context, partStartMillis: Long, partSpanMillis: Long) {
        try {
            val tallyRecord = TallyRecord(DateTime(partStartMillis), partSpanMillis.toInt(), mDataContext.getSetting(Setting.KEYS.tally_record_item_text, "").string)
            if (mDataContext.getRecord(partStartMillis) == null) {
                mDataContext.addRecord(tallyRecord)
            }
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
    }


    fun startAlarm(context: Context, endTimeInMillis: Long) {
        val intent = Intent(_Session.ACTION_ALARM_NIANFO_OVER)
        // 念佛结束闹钟
        val pi = PendingIntent.getBroadcast(context, AlarmReceiver.ALARM_NIANFO_OVER, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val am = context.getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeInMillis, pi)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            am.setExact(AlarmManager.RTC_WAKEUP, endTimeInMillis, pi)
        } else {
            am[AlarmManager.RTC_WAKEUP, endTimeInMillis] = pi
        }
    }

    fun stopAlarm(context: Context) {
        try {
            val intent = Intent(_Session.NIAN_FO_TIMER)
            val pi = PendingIntent.getBroadcast(context, AlarmReceiver.ALARM_NIANFO_OVER, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val am = context.getSystemService(ALARM_SERVICE) as AlarmManager
            am.cancel(pi)
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
    }

    private fun changeplayerSpeed(speed: Float) {
        // this checks on API 23 and up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mPlayer.isPlaying) {
                mPlayer.playbackParams = mPlayer.playbackParams.setSpeed(speed)
            } else {
                mPlayer.playbackParams = mPlayer.playbackParams.setSpeed(speed)
                mPlayer.pause()
            }
        }
    }

    var afChangeListener = OnAudioFocusChangeListener { focusChange ->
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            // Pause playback
            Log.e("wangsc", "pause.................")
            pause()
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // Resume playback
            Log.e("wangsc", "resume.................")
            resume()
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            // Stop playback
            Log.e("wangsc", "stop.................")
            stop()
        }
    }

    private fun requestFocus(): Boolean {
        // Request audio focus for playback
        val result = mAm!!.requestAudioFocus(afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }


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

    companion object {
        val NOTIFICATION_ID: Int = 234
        private var wakeLock: WakeLock? = null
        const val notification_action_play = "com.wangsc.myphone.play"
        const val notification_action_pause = "com.wangsc.myphone.pause"
    }
}