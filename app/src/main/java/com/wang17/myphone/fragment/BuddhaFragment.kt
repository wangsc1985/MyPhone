package com.wang17.myphone.fragment

import android.animation.ObjectAnimator
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources.NotFoundException
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.BaseAdapter
import android.widget.NumberPicker
import android.widget.TextView
import com.wang17.myphone.R
import com.wang17.myphone.activity.BuddhaActivity
import com.wang17.myphone.activity.BuddhaDetailActivity
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.event.NianfoOverEvent
import com.wang17.myphone.event.NianfoPauseEvent
import com.wang17.myphone.event.NianfoResumeEvent
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.DateTime.Companion.toSpanString
import com.wang17.myphone.database.Setting
import com.wang17.myphone.database.TallyPlan
import com.wang17.myphone.database.TallyRecord
import com.wang17.myphone.service.BuddhaTimerService
import com.wang17.myphone.service.StockService
import com.wang17.myphone.structure.TimerDisplayStatus
import com.wang17.myphone.util.*
import kotlinx.android.synthetic.main.fragment_buddha.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class BuddhaFragment : Fragment() {
    //region 视图变量
    private var timerDisplayStatus: TimerDisplayStatus? = null

    //endregion
    private var adapter = TallyPlanListAdapter()
    private lateinit var tallyPlanList: List<TallyPlan>

    //region 值变量
    private val vibrateTime = 100 // 震动时间
    private var defaultTargetInMillis = 0
    private var manualOverTimeInMillis = 0
    private var endTimeInMillis: Long = 0
    private var sectionStartInMillis: Long = 0
    private var todaySavedTotalInMillis = 0
    private var timer: Timer? = null
    private var onceMinte = 0



    //endregion
    //region 类变量
    private lateinit var vibrator: Vibrator
    private var uiThreadHandler: Handler? = null

    fun setTimerDisplayStatus(timerDisplayStatus: TimerDisplayStatus?) {
        try {
            this.timerDisplayStatus = timerDisplayStatus
            uiThreadHandler!!.post {
                try {
                    when (this@BuddhaFragment.timerDisplayStatus) {
                        TimerDisplayStatus.停止状态 -> {
                            textView_timer!!.clearAnimation()
                            textView_timer!!.setTextColor(ContextCompat.getColor(context!!, R.color.timerStopColor))
                        }
                        TimerDisplayStatus.运行状态 -> {
                            textView_timer!!.clearAnimation()
                            textView_timer!!.setTextColor(ContextCompat.getColor(context!!, R.color.timerRunningColor))
                        }
                        TimerDisplayStatus.暂停状态 -> {
                            _AnimationUtils.setFlickerAnimation(textView_timer, 500)
                            textView_timer!!.setTextColor(ContextCompat.getColor(context!!, R.color.timerRunningColor))
                        }
                    }
                } catch (e: NotFoundException) {
                    _Utils.printException(context, e)
                } catch (e: Exception) {
                    _Utils.printException(context, e)
                }
            }
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            val dataContext = DataContext(context)
            tallyPlanList = dataContext.getTallyPlans(true)
            uiThreadHandler = Handler()
            onceMinte = dataContext.getSetting(Setting.KEYS.tally_once_minute, 10).int
            EventBus.getDefault().register(this)
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dataContext = DataContext(context)

        initializeView()
        if (dataContext.getSetting(Setting.KEYS.is_stocks_listener, false).boolean == true) {
            _AnimationUtils.setRorateAnimation(context, abtn_stock, 3000)
        }
        abtn_stock.setOnClickListener {
            //region 在StockReportService里面执行
            if (dataContext.getSetting(Setting.KEYS.is_stocks_listener, false).boolean == false) {
                context!!.startService(Intent(context, StockService::class.java))
                dataContext.editSetting(Setting.KEYS.is_stocks_listener, true)
                animatorSuofang(abtn_stock)
            }
            //endregion
        }
        abtn_stock.setOnLongClickListener { //region 在StockReportService里面执行
            if (dataContext.getSetting(Setting.KEYS.is_stocks_listener, false).boolean == true) {
                context!!.stopService(Intent(context, StockService::class.java))
                dataContext.editSetting(Setting.KEYS.is_stocks_listener, false)
                stopAnimatorSuofang()
            } else {
                context!!.startService(Intent(context, StockService::class.java))
                dataContext.editSetting(Setting.KEYS.is_stocks_listener, true)
                animatorSuofang(abtn_stock)
                //
                _Utils.clickHomeButton(context!!)
            }
            //endregion
            true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_buddha, container, false)
        return view
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        try {
            initViewData()
            super.onResume()
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        stopTimer()
        //        stopMediaPlayer();
        super.onDestroy()
    }

    /**
     * 初始化数值
     */
    private fun initViewData() {
        try {
            val dataContext = DataContext(context)
            var setting = dataContext.getSetting(Setting.KEYS.tally_endInMillis)
            if (setting != null) { // 说明正处于运行状态

                // “暂停状态”有“时间间隔”，没有“目标时间”；
                // “终止状态/停止状态”状态：没有“时间间隔”，没有“目标时间”；
                // “运行状态”：没有“时间间隔”，有“目标时间”；
                endTimeInMillis = setting.string.toLong()
                sectionStartInMillis = dataContext.getSetting(Setting.KEYS.tally_sectionStartMillis).string.toLong()
                if (endTimeInMillis < System.currentTimeMillis()) {
                    AlertDialog.Builder(context!!)
                            .setMessage("上一次计时[ " + DateTime(sectionStartInMillis).toTimeString() + " - " + DateTime(endTimeInMillis).toTimeString() + " ]没有正常结束，需要将计时存入数据库吗?")
                            .setPositiveButton("是") { dialog, which -> //
                                val tallyRecord = TallyRecord(DateTime(sectionStartInMillis), (endTimeInMillis - sectionStartInMillis).toInt(), dataContext.getSetting(Setting.KEYS.tally_record_item_text, "").string)
                                dataContext.addRecord(tallyRecord)
                                //
                                dataContext.deleteSetting(Setting.KEYS.tally_sectionStartMillis)
                                dataContext.deleteSetting(Setting.KEYS.tally_endInMillis)
                                (context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1)
                                BackupTask(context).execute(BackupTask.COMMAND_BACKUP)
                            }.setNegativeButton("否") { dialog, which -> //
                                dataContext.deleteSetting(Setting.KEYS.tally_sectionStartMillis)
                                dataContext.deleteSetting(Setting.KEYS.tally_endInMillis)
                                (context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1)
                            }.show()
                } else {
                    startTimer()
                    setTimerDisplayStatus(TimerDisplayStatus.运行状态)
                }
            }
            setting = dataContext.getSetting(Setting.KEYS.tally_intervalInMillis)
            if (setting != null) { // 暂停状态
                val interval = setting.string.toLong()
                setTimerView(interval)
                setTimerDisplayStatus(TimerDisplayStatus.暂停状态)

                // 计数器
                val onceMinute = dataContext.getSetting(Setting.KEYS.tally_once_minute, 10).int
                val count = getTodaySavedTotalInMillis() / 60000 / onceMinute
                val progress = getTodaySavedTotalInMillis() / 1000 % (onceMinute * 60)
                textView_count!!.text = count.toString() + ""
                circle_percent!!.setProgress(progress)
                //                Log.e(_ATAG, "暂停状态：" + count);
            }
            defaultTargetInMillis = dataContext.getSetting(Setting.KEYS.tally_dayTargetInMillis, 60000 * 60).string.toInt()
            manualOverTimeInMillis = dataContext.getSetting(Setting.KEYS.tally_manualOverTimeInMillis, 0).string.toInt()
            if (timerDisplayStatus != TimerDisplayStatus.停止状态) {
                listView_tallyPlan!!.visibility = View.INVISIBLE
                if (timerDisplayStatus == TimerDisplayStatus.运行状态) {
                    setTimerView(endTimeInMillis - System.currentTimeMillis())
                    setFinishTimeView()

                    // 计数器
                    setting = dataContext.getSetting(Setting.KEYS.tally_sectionStartMillis)
                    val onceMinute = dataContext.getSetting(Setting.KEYS.tally_once_minute, 10).int
                    if (setting != null) {
                        val count = ((System.currentTimeMillis() - setting.long) / 60000 / onceMinute).toInt()
                        val progress = ((System.currentTimeMillis() - setting.long) / 1000 % (onceMinute * 60)).toInt()
                        textView_count!!.text = count.toString() + ""
                        circle_percent!!.setProgress(progress)
                        //                        Log.e(_ATAG, "运行状态：" + count);
                    }
                }
            } else {
//                listViewTallayPlan.setVisibility(View.VISIBLE);
                setTimerView(defaultTargetInMillis.toLong())


                // 计数器
                val onceMinute = dataContext.getSetting(Setting.KEYS.tally_once_minute, 10).int
                val count = getTodaySavedTotalInMillis() / 60000 / onceMinute
                val progress = getTodaySavedTotalInMillis() / 1000 % (onceMinute * 60)
                textView_count!!.text = count.toString() + ""
                circle_percent!!.setProgress(progress)
                //                Log.e(_ATAG, "停止状态：" + count);
            }
            todaySavedTotalInMillis = getTodaySavedTotalInMillis()
            setTotalResultTextView(todaySavedTotalInMillis.toLong())
            if (timerDisplayStatus == TimerDisplayStatus.运行状态) {
                setTotalResultTextView(todaySavedTotalInMillis + (System.currentTimeMillis() - sectionStartInMillis))
                val now = System.currentTimeMillis()
                val onceMinute = dataContext.getSetting(Setting.KEYS.tally_once_minute, 10).int
                val count = ((getTodaySavedTotalInMillis() + (now - sectionStartInMillis)) / 60000 / onceMinute).toInt()
                val progress = ((getTodaySavedTotalInMillis() + (now - sectionStartInMillis)) / 1000 % (onceMinute * 60)).toInt()
                textView_count!!.text = count.toString() + ""
                circle_percent!!.setProgress(progress)
            }
        } catch (e: NumberFormatException) {
            _Utils.printException(context, e)
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
    }

    /**
     * 初始化视图变量，并设置事件。
     */
    private fun initializeView() {
        try {
            val dataContext = DataContext(context)
            vibrator = activity!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator


            circle_percent!!.setMax(onceMinte * 60)

            listView_tallyPlan!!.adapter = adapter
            listView_tallyPlan!!.onItemLongClickListener = OnItemLongClickListener { parent, view, position, id ->
                AlertDialog.Builder(context!!).setItems(arrayOf("添加", "删除", "返回")) { dialog, which ->
                    when (which) {
                        0 -> {
                            addTallayPlanDialog(1, 0)
                            dialog.dismiss()
                        }
                        1 -> {
                            val model = tallyPlanList!![position]
                            dataContext.deleteTallyPlan(model.id)
                            tallyPlanList = dataContext.getTallyPlans(true)
                            adapter!!.notifyDataSetChanged()
                            dialog.dismiss()
                        }
                        2 -> listView_tallyPlan!!.visibility = View.INVISIBLE
                    }
                }.create().show()
                true
            }
            listView_tallyPlan!!.onItemClickListener = OnItemClickListener { parent, view, position, id ->
                val model = tallyPlanList!![position]
                defaultTargetInMillis = model.interval.toInt()
                dataContext.editSetting(Setting.KEYS.tally_dayTargetInMillis, defaultTargetInMillis)
                startBuddha()
            }

            if (dataContext.getSetting(Setting.KEYS.tally_music_switch, false).boolean == true) {
                switch_music!!.isChecked = true
            }
            switch_music!!.setOnCheckedChangeListener { buttonView, isChecked ->
                dataContext.editSetting(Setting.KEYS.tally_music_switch, isChecked)
                if (isChecked && timerDisplayStatus == TimerDisplayStatus.运行状态) {
                    startMusic()
                }
                if (!isChecked && timerDisplayStatus == TimerDisplayStatus.运行状态) {
                    stopMusic()
                }
            }

            layout_root!!.setOnLongClickListener {
                if (timerDisplayStatus == TimerDisplayStatus.停止状态) {
                    if (listView_tallyPlan!!.visibility == View.VISIBLE) listView_tallyPlan!!.visibility = View.INVISIBLE else listView_tallyPlan!!.visibility = View.VISIBLE
                } else {
                    showManualOverTimeDialog(0, 10)
                }
                true
            }


            layout_dayTotal!!.setOnClickListener {
                try {
                    val intent = Intent(activity, BuddhaDetailActivity::class.java)
                    intent.putExtra("start", System.currentTimeMillis())
                    startActivityForResult(intent, TO_TALLAY_RECORD_DETAIL_ACTIVITY)
                } catch (e: Exception) {
                    _Utils.printException(context, e)
                }
            }
            layout_dayTotal!!.setOnLongClickListener {
                try {
                    startActivityForResult(Intent(activity, BuddhaActivity::class.java), TO_TALLAY_RECORD_ACTIVITY)
                } catch (e: Exception) {
                    _Utils.printException(context, e)
                }
                true
            }

            timerDisplayStatus = TimerDisplayStatus.停止状态
            textView_timer!!.setOnClickListener {
                try {
                    when (timerDisplayStatus) {
                        TimerDisplayStatus.停止状态 -> startBuddha()
                        TimerDisplayStatus.运行状态 -> pauseBuddha()
                        TimerDisplayStatus.暂停状态 -> restartBuddha()
                        else -> {
                        }
                    }
                } catch (e: Exception) {
                    _Utils.printException(context, e)
                }
            }
            textView_timer!!.setOnLongClickListener {
                try {
                    if (timerDisplayStatus != TimerDisplayStatus.停止状态) {
                        stopBuddha()
                    } else {
                        showIntervalDialog(defaultTargetInMillis / 60000 / 60, defaultTargetInMillis / 60000 % 60)
                    }
                } catch (e: Exception) {
                    _Utils.printException(context, e)
                }
                true
            }
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
    }

    /**
     * 在念佛进行时，延长念佛目标时间。
     *
     * @param minites
     */
    private fun manualOverTimeInMin(minites: Int) {
        val dataContext = DataContext(context)
        // 修改程序中结束时间变量，及数据库中的结束时间
        if (endTimeInMillis + minites * 60000 - System.currentTimeMillis() > 60000) {
            endTimeInMillis += minites * 60000.toLong()
            dataContext.editSetting(Setting.KEYS.tally_endInMillis, endTimeInMillis)
            manualOverTimeInMillis += minites * 60000
            dataContext.editSetting(Setting.KEYS.tally_manualOverTimeInMillis, manualOverTimeInMillis)

            // 更新界面相关文本空间
            setTimerView(endTimeInMillis - System.currentTimeMillis())
        }
    }

    private fun setTotalResultTextView(totalMillis: Long) {
        try {
            uiThreadHandler!!.post {
                try {
                    textView_monthDuration!!.text = toSpanString(totalMillis, 3, 2)
                } catch (e: Exception) {
                    _Utils.printException(context, e)
                }
            }
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
    }

    private fun setTimerView(countDownIntervalInMillis: Long) {
        try {
            var stime = ""
            var shour = ""
            var sminute = ""
            var ssecond = ""
            var hour = 0
            var minute = 0
            var second = 0
            if (countDownIntervalInMillis == 0L) {
                stime = "00 : 00 : 00"
            } else {
                hour = (countDownIntervalInMillis / (1000 * 60 * 60)).toInt()
                minute = (countDownIntervalInMillis % (1000 * 60 * 60) / (60 * 1000)).toInt()
                second = (countDownIntervalInMillis % (60 * 1000) / 1000).toInt()
                shour = "" + hour
                sminute = "" + minute
                ssecond = "" + second
                if (hour <= 9) {
                    shour = "0$hour"
                }
                if (minute <= 9) {
                    sminute = "0$minute"
                }
                if (second <= 9) {
                    ssecond = "0$second"
                }
                stime = "$shour : $sminute : $ssecond"
            }
            val finalStime = stime
            uiThreadHandler!!.post { textView_timer!!.text = finalStime }
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
    }

    private fun getTodaySavedTotalInMillis(): Int {
        try {
            val dataContext = DataContext(context)
            var totalResult = 0
            val tallyRecords = dataContext.getRecords(DateTime())
            for (rec in tallyRecords) {
                totalResult += rec.interval
            }
            return totalResult
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
        return 0
    }

    private fun startBuddha() {
        try {
            val dataContext = DataContext(context)
            listView_tallyPlan!!.visibility = View.INVISIBLE
            // 检查系统音量
            val mAudioManager = activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)
            val current = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
//            if ((float) current / max < 0.8) {
//                new AlertDialog.Builder(getContext()).setMessage("当前系统音量太小，建议增大系统音量（音量键调节）。").setPositiveButton("确定", null).show();
//                return;
//            }

            // 向数据库中保存“时间节开始”
            sectionStartInMillis = System.currentTimeMillis()
            dataContext.addSetting(Setting.KEYS.tally_sectionStartMillis, sectionStartInMillis)

            // 向数据库中保存“任务完成时时间”
            endTimeInMillis = sectionStartInMillis + defaultTargetInMillis
            dataContext.addSetting(Setting.KEYS.tally_endInMillis, endTimeInMillis)

            // 更新当天总计
            todaySavedTotalInMillis = getTodaySavedTotalInMillis()

            // 开始倒计时文字更新
            startTimer()


            // 更新念佛计时状态
            vibrator!!.vibrate(vibrateTime.toLong())
            setTimerDisplayStatus(TimerDisplayStatus.运行状态)
            if (switch_music!!.isChecked) {
                startMusic()
            }
            startAlarm()
            //            showNotification();
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
    }

    private fun pauseBuddha() {
        val dataContext = DataContext(context)
        try {
            val dataContext = DataContext(context)
            vibrator!!.vibrate(vibrateTime.toLong())

            // 向数据库中保存“已经完成的时间”
            val now = System.currentTimeMillis()
            sectionStartInMillis = dataContext.getSetting(Setting.KEYS.tally_sectionStartMillis).string.toLong()
            dataContext.deleteSetting(Setting.KEYS.tally_sectionStartMillis)
            val sectionIntervalInMillis = now - sectionStartInMillis
            if (sectionIntervalInMillis >= 60000) {
                saveSection(sectionStartInMillis, sectionIntervalInMillis)
            }

            // 向数据库中保存“剩余时间”
            dataContext.deleteSetting(Setting.KEYS.tally_endInMillis)
            var remainIntervalInMillis = endTimeInMillis - now
            if (sectionIntervalInMillis < 60000) {
                remainIntervalInMillis = endTimeInMillis - sectionStartInMillis
            }
            dataContext.addSetting(Setting.KEYS.tally_intervalInMillis, remainIntervalInMillis)
            setTimerView(remainIntervalInMillis)


            // 更新当天总计
            todaySavedTotalInMillis = getTodaySavedTotalInMillis()

            // 停止倒计时文字更新
            stopTimer()

            // 更新念佛计时状态
            setTimerDisplayStatus(TimerDisplayStatus.暂停状态)
            if (switch_music!!.isChecked) {
                stopMusic()
            }
            stopAlarm()
        } catch (e: NumberFormatException) {
            _Utils.printException(context, e)
        }
    }

    private fun restartBuddha() {
        val dataContext = DataContext(context)
        try {
            vibrator!!.vibrate(vibrateTime.toLong())
            val dataContext = DataContext(context)
            val setting = dataContext.getSetting(Setting.KEYS.tally_intervalInMillis)
            if (setting != null) {
                // 删除
                val interval = setting.string.toLong()
                dataContext.deleteSetting(Setting.KEYS.tally_intervalInMillis)
                sectionStartInMillis = System.currentTimeMillis()

                // 保存
                endTimeInMillis = sectionStartInMillis + interval
                dataContext.addSetting(Setting.KEYS.tally_endInMillis, endTimeInMillis)
                dataContext.addSetting(Setting.KEYS.tally_sectionStartMillis, sectionStartInMillis)

                // 重新载入当天已入档的念佛总时间
                todaySavedTotalInMillis = getTodaySavedTotalInMillis()
                startTimer()
                setTimerDisplayStatus(TimerDisplayStatus.运行状态)
                if (switch_music!!.isChecked) {
                    startMusic()
                }
                startAlarm()
            }
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
    }

    private fun stopBuddha() {
        val dataContext = DataContext(context)
        try {
//            listViewTallayPlan.setVisibility(View.VISIBLE);
            val dataContext = DataContext(context)
            dataContext.editSetting(Setting.KEYS.tally_music_switch, false)
            if (timerDisplayStatus == TimerDisplayStatus.停止状态)
                return
            vibrator!!.vibrate(vibrateTime.toLong())
            if (dataContext.getSetting(Setting.KEYS.tally_endInMillis) != null) {
                // 说明此时倒计时正在运行。
                dataContext.deleteSetting(Setting.KEYS.tally_endInMillis)
                sectionStartInMillis = dataContext.getSetting(Setting.KEYS.tally_sectionStartMillis).string.toLong()
                dataContext.deleteSetting(Setting.KEYS.tally_sectionStartMillis)
                val interval = (System.currentTimeMillis() - sectionStartInMillis) / 60000 * 60000
                if (interval > 0) {
                    saveSection(sectionStartInMillis, interval)
                }
            } else {
                // 因为程序开始已经把停止状态过滤，所以说明此时倒计时只能是暂停状态。
                dataContext.deleteSetting(Setting.KEYS.tally_intervalInMillis)
            }
            manualOverTimeInMillis = 0
            dataContext.deleteSetting(Setting.KEYS.tally_manualOverTimeInMillis)
            setTimerView(defaultTargetInMillis.toLong())
            stopTimer()
            _NotificationUtils.closeNotification(context, NOTIFICATION_ID)
            setTimerDisplayStatus(TimerDisplayStatus.停止状态)
            if (switch_music!!.isChecked) {
                stopMusic()
            }
            stopAlarm()
            setAutoCoundProgress()
        } catch (e: NumberFormatException) {
            _Utils.printException(context, e)
        }
    }

    private fun startMusic() {
        // 启动服务
//        activity!!.startService(Intent(context, NianfoMusicService::class.java))
    }

    private fun stopMusic() {
//        activity!!.stopService(Intent(context, NianfoMusicService::class.java)) // 停止服务
//        val manager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        manager.cancel(NOTIFICATION_FLAG)
    }

    //region 动画
    var scaleY: ObjectAnimator? = null
    var scaleX: ObjectAnimator? = null
    fun animatorSuofang(myView: View?) {
        if (scaleY == null) scaleY = ObjectAnimator.ofFloat(myView, "scaleY", 1f, 0.7f, 1f)
        scaleY!!.repeatCount = -1
        scaleY!!.duration = 600
        scaleY!!.start()
        if (scaleX == null) scaleX = ObjectAnimator.ofFloat(myView, "scaleX", 1f, 0.7f, 1f)
        scaleX!!.repeatCount = -1
        scaleX!!.duration = 600
        scaleX!!.start()
    }

    fun stopAnimatorSuofang() {
        scaleY!!.repeatCount = 0
        scaleX!!.repeatCount = 0
    }
    //endregion
    private fun startTimer() {
        if (timer != null) {
            return
        }
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                try {
                    val now = System.currentTimeMillis()
                    /**
                     * 自动计数部分
                     * 每10分钟记一次数，count为次数，progress是当前还剩多少时间走完10分钟。
                     */
                    setAutoCoundProgress()
                    /**
                     * 计时部分
                     */
                    var span = endTimeInMillis - now
                    setTimerView(span)
                    setTotalResultTextView(todaySavedTotalInMillis + (now - sectionStartInMillis))
                    /**
                     * 计时结束
                     */
                    if (span <= 0) {
                        manualOverTimeInMillis = 0
                        span = defaultTargetInMillis.toLong()
                        setTimerDisplayStatus(TimerDisplayStatus.停止状态)
                        clearFinishTimeView()

                        //
                        stopBuddha()
                    }
                } catch (e: Exception) {
                    _Utils.printException(context, e)
                }
            }
        }, 0, 1000)
    }

    private fun setAutoCoundProgress() {
        val now = System.currentTimeMillis()
        val count = ((getTodaySavedTotalInMillis() + (now - sectionStartInMillis)) / 60000 / onceMinte).toInt()
        val progress = ((getTodaySavedTotalInMillis() + (now - sectionStartInMillis)) / 1000 % (onceMinte * 60)).toInt()
        uiThreadHandler!!.post {
            textView_count!!.text = count.toString() + ""
            circle_percent!!.setProgress(progress)
        }
    }

    private fun stopTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    private fun startAlarm() {
        val intent = Intent(context, BuddhaTimerService::class.java)
        intent.putExtra("isSound", switch_music.isChecked)
        context!!.startService(intent)
    }

    private fun stopAlarm() {
        context!!.stopService(Intent(context, BuddhaTimerService::class.java))
    }

    /**
     * 清除结束时间文本
     */
    private fun clearFinishTimeView() {
        uiThreadHandler!!.post { // 清除屏幕上结束时间
            textView_finishTime!!.text = ""
        }
    }

    /**
     * 显示任务终止时时间。
     *
     * @throws Exception
     */
    private fun setFinishTimeView() {
        try {
            val finishTime = DateTime(endTimeInMillis)
            val sectionTargetInMillis = endTimeInMillis - sectionStartInMillis
            val dayTargetInMillis = sectionTargetInMillis + todaySavedTotalInMillis
            val finishText = (toSpanString(dayTargetInMillis, 4, 2)
                    + "   " + finishTime.hourStr + ":" + finishTime.miniteStr + ":" + finishTime.secondStr)
            //                    + "   " + DateTime.toSpanString(dayTargetInMillis, 4, 2);
            textView_finishTime!!.text = finishText
            //            textViewHaveH.setVisibility(View.INVISIBLE);
//            textViewHaveM.setVisibility(View.INVISIBLE);
//            progressBarHave.setVisibility(View.INVISIBLE);
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
    }

    private fun saveSection(partStartMillis: Long, partSpanMillis: Long) {
        try {
            val dataContext = DataContext(context)
            val tallyRecord = TallyRecord(DateTime(partStartMillis), partSpanMillis.toInt(), dataContext.getSetting(Setting.KEYS.tally_record_item_text, "").string)
            if (dataContext.getRecord(partStartMillis) == null) dataContext.addRecord(tallyRecord)
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        try {
            todaySavedTotalInMillis = getTodaySavedTotalInMillis()
            if (timerDisplayStatus == TimerDisplayStatus.运行状态) setTotalResultTextView(todaySavedTotalInMillis + (System.currentTimeMillis() - sectionStartInMillis)) else setTotalResultTextView(todaySavedTotalInMillis.toLong())
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun showManualOverTimeDialog(xxxHour: Int, xxxMin: Int) {
        val view = View.inflate(context, R.layout.inflate_dialog_interval_picker, null)
        val dialog = AlertDialog.Builder(context!!).create()
        dialog.setView(view)
        dialog.setTitle("延时设定")
        val hourNumbers = arrayOfNulls<String>(100)
        for (i in 0..99) {
            hourNumbers[i] = i.toString() + "小时"
        }
        val minNumbers = arrayOfNulls<String>(12)
        for (i in 0..11) {
            minNumbers[i] = (i * 5).toString() + "分钟"
        }
        val number_hour = view.findViewById<View>(R.id.number_hour) as NumberPicker
        val number_min = view.findViewById<View>(R.id.number_min) as NumberPicker
        number_hour.minValue = 0
        number_hour.displayedValues = hourNumbers
        number_hour.maxValue = 99
        number_hour.value = xxxHour
        number_hour.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
        number_min.minValue = 0
        number_min.displayedValues = minNumbers
        number_min.maxValue = 11
        number_min.value = xxxMin / 5
        number_min.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
        number_hour.setOnValueChangedListener { picker, oldVal, newVal ->
            try {
                if (newVal == 0 && number_min.value == 0) {
                    number_min.value = 6
                }
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        number_min.setOnValueChangedListener { picker, oldVal, newVal ->
            try {
                if (newVal == 0 && number_hour.value == 0) {
                    number_hour.value = 1
                }
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "减少") { dialog, which ->
            try {
                val hour = number_hour.value
                val min = number_min.value
                var interval = hour * 60000 * 60 + min * 5 * 60000
                interval *= -1
                manualOverTimeInMin(interval / 60000)
                dialog.dismiss()
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "取消") { dialog, which -> dialog.dismiss() }
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "增加") { dialog, which ->
            try {
                val hour = number_hour.value
                val min = number_min.value
                val interval = hour * 60000 * 60 + min * 5 * 60000
                manualOverTimeInMin(interval / 60000)
                dialog.dismiss()
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        dialog.show()
    }

    fun showIntervalDialog(xxxHour: Int, xxxMin: Int) {
        val view = View.inflate(context, R.layout.inflate_dialog_interval_picker, null)
        val dialog = AlertDialog.Builder(context!!).create()
        dialog.setView(view)
        dialog.setTitle("设定念佛时间")
        val hourNumbers = arrayOfNulls<String>(100)
        for (i in 0..99) {
            hourNumbers[i] = i.toString() + "小时"
        }
        val minNumbers = arrayOfNulls<String>(12)
        for (i in 0..11) {
            minNumbers[i] = (i * 5).toString() + "分钟"
        }
        val number_hour = view.findViewById<View>(R.id.number_hour) as NumberPicker
        val number_min = view.findViewById<View>(R.id.number_min) as NumberPicker
        number_hour.minValue = 0
        number_hour.displayedValues = hourNumbers
        number_hour.maxValue = 99
        number_hour.value = xxxHour
        number_hour.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
        number_min.minValue = 0
        number_min.displayedValues = minNumbers
        number_min.maxValue = 11
        number_min.value = xxxMin / 5
        number_min.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
        number_hour.setOnValueChangedListener { picker, oldVal, newVal ->
            try {
                if (newVal == 0 && number_min.value == 0) {
                    number_min.value = 6
                }
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        number_min.setOnValueChangedListener { picker, oldVal, newVal ->
            try {
                if (newVal == 0 && number_hour.value == 0) {
                    number_hour.value = 1
                }
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定") { dialog, which ->
            try {
                val dataContext = DataContext(context)
                val hour = number_hour.value
                val min = number_min.value
                defaultTargetInMillis = hour * 60000 * 60 + min * 5 * 60000
                setTimerView(defaultTargetInMillis.toLong())
                dataContext.editSetting(Setting.KEYS.tally_dayTargetInMillis, defaultTargetInMillis)
                dialog.dismiss()
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消") { dialog, which -> dialog.dismiss() }
        dialog.show()
    }

    fun addTallayPlanDialog(xxxHour: Int, xxxMin: Int) {
        val view = View.inflate(context, R.layout.inflate_dialog_interval_picker, null)
        val dialog = AlertDialog.Builder(context!!).create()
        dialog.setView(view)
        dialog.setTitle("添加计划")
        val hourNumbers = arrayOfNulls<String>(100)
        for (i in 0..99) {
            hourNumbers[i] = i.toString() + "小时"
        }
        val minNumbers = arrayOfNulls<String>(12)
        for (i in 0..11) {
            minNumbers[i] = (i * 5).toString() + "分钟"
        }
        val number_hour = view.findViewById<View>(R.id.number_hour) as NumberPicker
        val number_min = view.findViewById<View>(R.id.number_min) as NumberPicker
        number_hour.minValue = 0
        number_hour.displayedValues = hourNumbers
        number_hour.maxValue = 99
        number_hour.value = xxxHour
        number_hour.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
        number_min.minValue = 0
        number_min.displayedValues = minNumbers
        number_min.maxValue = 11
        number_min.value = xxxMin / 5
        number_min.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
        number_hour.setOnValueChangedListener { picker, oldVal, newVal ->
            try {
                if (newVal == 0 && number_min.value == 0) {
                    number_min.value = 6
                }
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        number_min.setOnValueChangedListener { picker, oldVal, newVal ->
            try {
                if (newVal == 0 && number_hour.value == 0) {
                    number_hour.value = 1
                }
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "添加") { dialog, which ->
            try {
                val dataContext = DataContext(context)
                val hour = number_hour.value
                val min = number_min.value
                val interval = hour * 60000 * 60 + min * 5 * 60000.toLong()
                val item = "阿弥陀佛"
                val summary = ""
                val model = TallyPlan(interval, item, summary)
                dataContext.addTallyPlan(model)
                tallyPlanList = dataContext.getTallyPlans(true)
                adapter!!.notifyDataSetChanged()
                dialog.dismiss()
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消") { dialog, which -> dialog.dismiss() }
        dialog.show()
    }

    //region EventBus事件处理
    @Subscribe
    fun onEventMainThread(event: NianfoPauseEvent?) {
        try {
            // 停止倒计时文字更新
            stopTimer()

            // 更新念佛计时状态
            setTimerDisplayStatus(TimerDisplayStatus.暂停状态)
        } catch (e: NumberFormatException) {
            _Utils.printException(context, e)
        }
    }

    @Subscribe
    fun onEventMainThread(event: NianfoOverEvent?) {
        try {
        } catch (e: NumberFormatException) {
            _Utils.printException(context, e)
        }
    }

    @Subscribe
    fun onEventMainThread(event: NianfoResumeEvent?) {
        startTimer()
    }

    //endregion
    //缩放

    protected inner class TallyPlanListAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return tallyPlanList.size
        }

        override fun getItem(position: Int): Any {
            return tallyPlanList.get(position)
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            view = View.inflate(context, R.layout.inflate_list_item_tally_plan, null)
            try {
                val listItem = tallyPlanList[position]
                val textViewInterval = view.findViewById<View>(R.id.textView_interval) as TextView
                val textViewItem = view.findViewById<View>(R.id.textView_item) as TextView
                val textViewSummary = view.findViewById<View>(R.id.textView_summary) as TextView
                textViewItem.text = listItem.item
                textViewSummary.text = listItem.summary
                val interval = listItem.interval
                val hour = (interval / 3600 / 1000).toInt()
                val minite = (interval % 3600000 / 60 / 1000).toInt()
                val second = (interval % 60000 / 1000).toInt()
                val sHour = if (hour < 10) "0$hour" else "" + hour
                val sMinite = if (minite < 10) "0$minite" else "" + minite
                val sSecond = if (second < 10) "0$second" else "" + second
                textViewInterval.text = "$sHour:$sMinite:$sSecond"
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
            return view
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 145
        private const val TO_TALLAY_RECORD_ACTIVITY = 0
        private const val TO_TALLAY_RECORD_DETAIL_ACTIVITY = 1
        const val NOTIFICATION_FLAG = 605
    }
}