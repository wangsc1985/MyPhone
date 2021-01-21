package com.wang17.myphone.receiver

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import com.wang17.myphone.R
import com.wang17.myphone.activity.AlarmWindowActivity
import com.wang17.myphone.activity.FundMonitorActivity
import com.wang17.myphone.activity.FuturePositionActivity
import com.wang17.myphone.activity.StockPositionActivity
import com.wang17.myphone.event.NianfoOverEvent
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.model.database.TallyRecord
import com.wang17.myphone.service.NianfoMusicService
import com.wang17.myphone.service.SpeakerService
import com.wang17.myphone.util.BackupTask
import com.wang17.myphone.util.DataContext
import com.wang17.myphone.util._LogUtils.log2file
import com.wang17.myphone.util._Session
import com.wang17.myphone.util._Session.ALERT_VOLUMN
import com.wang17.myphone.util._Utils.printException
import org.greenrobot.eventbus.EventBus
import java.lang.Boolean
import java.util.*

/**
 * Created by 阿弥陀佛 on 2016/10/23.
 */
class AlarmReceiver : BroadcastReceiver() {
    private var mDataContext: DataContext? = null
    override fun onReceive(context: Context, argIntent: Intent) {
        val now = DateTime()
        try {
            mDataContext = DataContext(context)
            when (argIntent.action) {
                _Session.ACTION_ALARM_NIANFO_OVER -> {
                    log2file("念佛闹钟响铃")
                    Log.e("wangsc", "ALARM_NIANFO_OVER")
                    saveSection(context)
                    EventBus.getDefault().post(NianfoOverEvent())
                    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1)
                    /**
                     * 有对话框
                     */
                    if (Boolean.parseBoolean(mDataContext!!.getSetting(Setting.KEYS.tally_isShowDialog, "true").string)) {
                        val intent = Intent(context, AlarmWindowActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } else {
                        val intent1 = Intent(context, SpeakerService::class.java)
                        intent1.putExtra("msg", mDataContext!!.getSetting(Setting.KEYS.nianfo_over_speaker_msg, "富饶").string)
                        context.startService(intent1)

//                        SoundPool soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
//                        soundPool.load(context, R.raw.ling3, 1);
//                        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
//                            @Override
//                            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
//                                int streamId = soundPool.play(1, ALERT_VOLUMN, ALERT_VOLUMN, 0, 0, 1);
//                                try {
//                                    float add = (1 - ALERT_VOLUMN) / 2;
//                                    float vol = ALERT_VOLUMN;
//                                    for (int i = 0; i < 2; i++) {
//                                        Thread.sleep(1000);
//
//                                        vol += add;
//                                        soundPool.setVolume(streamId, vol, vol);
//                                    }
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        });
                        /**
                         * 停止念佛音乐服务
                         */
                        if (mDataContext!!.getSetting(Setting.KEYS.tally_music_is_playing, false).boolean) {
                            context.stopService(Intent(context, NianfoMusicService::class.java))
                        }
                    }
                }
                _Session.ACTION_ALARM_TRADE -> {
                    Log.e("wangsc", "ALARM_TRADE")
                    val soundPool = SoundPool(10, AudioManager.STREAM_SYSTEM, 5)
                    soundPool.load(context, R.raw.ling3, 1)
                    soundPool.setOnLoadCompleteListener { soundPool, sampleId, status ->
                        val streamId = soundPool.play(1, ALERT_VOLUMN, ALERT_VOLUMN, 0, 0, 1f)
                        try {
                            val add: Float = (1 - ALERT_VOLUMN) / 2
                            var vol: Float = ALERT_VOLUMN
                            var i = 0
                            while (i < 1) {
                                Thread.sleep(500)
                                vol += add
                                soundPool.setVolume(streamId, vol, vol)
                                i++
                            }
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                    val clock = DateTime(now.year, now.month, now.day, 8, 50, 0)
                    if (now.timeInMillis > clock.timeInMillis) {
                        clock[Calendar.HOUR] = 11
                        clock[Calendar.MINUTE] = 27
                    }
                    if (now.timeInMillis > clock.timeInMillis) {
                        clock[Calendar.HOUR] = 13
                        clock[Calendar.MINUTE] = 27
                    }
                    if (now.timeInMillis > clock.timeInMillis) {
                        clock[Calendar.HOUR] = 14
                        clock[Calendar.MINUTE] = 55
                    }
                    if (now.timeInMillis > clock.timeInMillis) {
                        clock[Calendar.HOUR] = 20
                        clock[Calendar.MINUTE] = 50
                    }
                    if (now.timeInMillis > clock.timeInMillis) {
                        if (clock[Calendar.DAY_OF_WEEK] == Calendar.FRIDAY) clock.addDays(3) else clock.addDays(1)
                        clock[Calendar.HOUR] = 8
                        clock[Calendar.MINUTE] = 50
                    }
                    val intent = Intent(_Session.ACTION_ALARM_TRADE)
                    val pi = PendingIntent.getBroadcast(context, ALARM_TRADE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    if (Build.VERSION.SDK_INT >= 19) {
                        am.setExact(AlarmManager.RTC_WAKEUP, clock.timeInMillis, pi)
                    } else {
                        am[AlarmManager.RTC_WAKEUP, clock.timeInMillis] = pi
                    }
                }
                _Session.ACTION_ALARM_STOCK_SCREEN -> {
                    wakeScreen(context)
                    var intent = Intent(context, FundMonitorActivity::class.java)
                    when (mDataContext!!.getSetting(Setting.KEYS.quick, 1).int) {
                        1 -> intent = Intent(context, StockPositionActivity::class.java)
                        3 -> intent = Intent(context, FuturePositionActivity::class.java)
                        4 -> intent = Intent(context, FundMonitorActivity::class.java)
                    }
                    context.startActivity(intent)
                }
            }
        } catch (e: Exception) {
            printException(context, e)
        }
    }

    companion object {
        const val ALARM_NIANFO_OVER = 406
        const val ALARM__STOCK_SCREEN = 564
        const val ALARM_NIANFO_BEFORE_OVER = 916
        const val ALARM_TRADE = 134
        var mWakeLock: WakeLock? = null
        @SuppressLint("InvalidWakeLockTag")
        fun wakeScreen(context: Context) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, "bright")
            mWakeLock?.acquire()
        }

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