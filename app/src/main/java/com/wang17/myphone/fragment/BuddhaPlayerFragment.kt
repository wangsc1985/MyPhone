package com.wang17.myphone.fragment

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.wang17.myphone.e
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.alibaba.fastjson.JSON
import com.wang17.myphone.R
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.database.BuddhaRecord
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.service.BuddhaService
import com.wang17.myphone.service.StockService
import com.wang17.myphone.util.DataContext
import com.wang17.myphone.util._CloudUtils
import com.wang17.myphone.util._JsonUtils
import com.wang17.myphone.util._Utils
import kotlinx.android.synthetic.main.fragment_player.*
import kotlinx.android.synthetic.main.fragment_player.abtn_stock
import java.text.DecimalFormat
import java.util.*

class BuddhaPlayerFragment : Fragment() {

    lateinit var abtn_stockAnimator: AnimatorSuofangView
    lateinit var btn_buddha_animator: AnimatorSuofangView
    var uiHandler = Handler()

    override fun onResume() {
        super.onResume()

        if (_Utils.isRunService(context!!, StockService::class.qualifiedName!!)) {
            animatorSuofang(abtn_stockAnimator)
        }else{
            stopAnimatorSuofang(abtn_stockAnimator)
        }
        if (_Utils.isRunService(context!!, BuddhaService::class.qualifiedName!!)) {
            animatorSuofang(btn_buddha_animator)
        }else{
            stopAnimatorSuofang(btn_buddha_animator)
        }


        val dataContext = DataContext(context)
        val buddha = dataContext.getLatestBuddha()
        var time = DateTime(1970, 1, 1)
        if (buddha != null) {
            time = buddha.startTime
        }
        _CloudUtils.loadBuddha(context!!, time) { code, result ->
            try {
                if (code == 0) {
                    e(result)
                    val jsonArray = JSON.parseArray(result.toString())
                    var buddhaS = ArrayList<BuddhaRecord>()
                    for (i in jsonArray.indices) {
                        val startTime = _JsonUtils.getValueByKey(jsonArray[i], "startTime").toLong()
                        val duration = _JsonUtils.getValueByKey(jsonArray[i], "duration").toLong()
                        val count = _JsonUtils.getValueByKey(jsonArray[i], "count").toInt()
                        val summary = _JsonUtils.getValueByKey(jsonArray[i], "summary")
                        val type = _JsonUtils.getValueByKey(jsonArray[i], "type").toInt()
                        buddhaS.add(BuddhaRecord(DateTime(startTime), duration, count, type, summary))
                    }

                    dataContext.addBuddhas(buddhaS)

                    uiHandler.post {
                        if (buddhaS.size > 0) {
                            AlertDialog.Builder(context).setMessage("新增${buddhaS.size}条记录").show()
                        }
                    }
                    refreshTotalView()
                }
            } catch (e: Exception) {
                e(e.message!!)
            }
        }
    }

    private fun refreshTotalView() {
        val dataContext = DataContext(context)
        val now = DateTime()
        var totalDayDuration: Long = 0
        var totalDayCount = 0
        var totalMonthDuration: Long = 0
        var totalMonthCount = 0
        val monthBuddhas = dataContext.getBuddhas(now.year, now.month)
        monthBuddhas.forEach {
            totalMonthCount += it.count
            totalMonthDuration += it.duration
        }
        val dayBuddhas = dataContext.getBuddhas(now)
        dayBuddhas.forEach {
            totalDayCount += it.count
            totalDayDuration += it.duration
        }

        e("month buddha size : ${monthBuddhas.size} , day buddha size : ${dayBuddhas.size}")
        val hour = totalMonthDuration / (60000 * 60)
        val minite = totalMonthDuration % (60000 * 60) / 60000
        val hourS = "${hour}:"
        val formatter = DecimalFormat("#,##0")
        val miniteS = if (minite == 0L) {
            if (hour == 0L) "0" else "00"
        } else {
            if (minite < 10) "0${minite}" else "${minite}"
        }

        val hour1 = totalDayDuration / (60000 * 60)
        val minite1 = totalDayDuration % (60000 * 60) / 60000
        val hourS1 = "${hour1}:"
        val miniteS1 = if (minite1 == 0L) {
            if (hour1 == 0L) "0" else "00"
        } else {
            if (minite1 < 10) "0${minite1}" else "${minite1}"
        }
        uiHandler.post {
            tv_monthTotal.setText("${hourS}${miniteS}  ${formatter.format(totalMonthCount * 1080)}  ${DecimalFormat("0.0").format(totalMonthCount.toBigDecimal().setScale(1) / now.day.toBigDecimal())}")
            tv_dayTotal.setText("${hourS1}${miniteS1}  ${formatter.format(totalDayCount * 1080)}  ${totalDayCount}")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val dataContext = DataContext(context)
        buddhaIntent = Intent(context, BuddhaService::class.java)
        abtn_stockAnimator = AnimatorSuofangView(abtn_stock)
        btn_buddha_animator = AnimatorSuofangView(abtn_buddha)
        val buddhaSpeed = dataContext.getSetting(Setting.KEYS.buddha_speed, 2).int
        when (buddhaSpeed) {
            1 -> {
                btn_speed.text = "慢"
            }
            2 -> {
                btn_speed.text = "快"
            }
        }

        var totalCount = 0
        layout_dayTotal.setOnClickListener {
            val dayBuddhaList = dataContext.getBuddhas(DateTime())
            var items = arrayOfNulls<String>(dayBuddhaList.size)
            for (index in dayBuddhaList.indices) {
                val hour1 = dayBuddhaList[index].duration / (60000 * 60)
                val minite1 = dayBuddhaList[index].duration % (60000 * 60) / 60000
                val hourS1 = "${hour1}:"
                val miniteS1 = if (minite1 == 0L) {
                    if (hour1 == 0L) "0" else "00"
                } else {
                    if (minite1 < 10) "0${minite1}" else "${minite1}"
                }
                totalCount += dayBuddhaList[index].count
                items[index] = "${dayBuddhaList[index].startTime.toShortTimeString()}   ${hourS1}${miniteS1}   ${dayBuddhaList[index].count}"
            }
            if (totalCount > 0)
                AlertDialog.Builder(context).setItems(items, DialogInterface.OnClickListener { dialog, index ->
                    AlertDialog.Builder(context)
                            .setMessage("是否要删除【${dayBuddhaList[index].startTime.toLongDateTimeString()}】的记录?")
                            .setNegativeButton("是", DialogInterface.OnClickListener { dialog, which ->
                                var dc = DataContext(context)
                                dc.deleteBuddha(dayBuddhaList[index].id)
                                refreshTotalView()

                                _CloudUtils.delBuddha(context!!, dayBuddhaList[index]) { code, result ->
                                    when (code) {
                                        0 -> {
                                            uiHandler.post {
                                                Toast.makeText(context, result.toString(), Toast.LENGTH_LONG).show()
                                            }
                                        }
                                        else -> {
                                            e("code : $code , result : $result")
                                        }
                                    }
                                }
                            }).show()
                }).show()
        }

        layout_monthTotal.setOnClickListener {
            val today = DateTime.today
            val monthBuddhaList = dataContext.getBuddhas(today.year, today.month)
            var start = DateTime(today.year, today.month, 1)
            var sb = StringBuilder()
            var totalDuration = 0L
            var totalCount = 0
            while (start.year == today.year && start.get(Calendar.DAY_OF_YEAR) <= today.get(Calendar.DAY_OF_YEAR)) {
                var list = monthBuddhaList.filter {
                    val time = it.startTime
                    time.year == start.year && time.get(Calendar.DAY_OF_YEAR) == start.get(Calendar.DAY_OF_YEAR)
                }
                var duration = 0L
                var count = 0
                var time = start

                list.forEach {
                    time = it.startTime
                    count += it.count
                    duration += it.duration
                    e("time : ${time.toLongDateTimeString()}")
                }

                val hour = duration / (60000 * 60)
                val minite = duration % (60000 * 60) / 60000
                val hourS = "${hour}:"
                val miniteS = if (minite == 0L) {
                    if (hour == 0L) "0" else "00"
                } else {
                    if (minite < 10) "0${minite}" else "${minite}"
                }
                sb.append("${time.toShortDateString1()} \t ${if (count > 0) "${hourS}${miniteS} \t ${DecimalFormat("#,000").format(count * 1080)} \t ${count}" else ""}\n")

                totalCount += count
                totalDuration += duration
                start = start.addDays(1)
            }
            val hour = totalDuration / (60000 * 60)
            val minite = totalDuration % (60000 * 60) / 60000
            val hourS = "${hour}:"
            val miniteS = if (minite == 0L) {
                if (hour == 0L) "0" else "00"
            } else {
                if (minite < 10) "0${minite}" else "${minite}"
            }
            val avgCount = totalCount.toBigDecimal().setScale(1) / today.day.toBigDecimal()
            sb.append("\n${hourS}${miniteS} \t ${DecimalFormat("#,000").format(totalCount * 1080)} \t ${totalCount} \t ${avgCount}\n")
            if (totalCount > 0)
                AlertDialog.Builder(context).setMessage(sb.toString()).show()
        }

        iv_buddha.setOnLongClickListener {
            val list = arrayOf("1 x 1080", "2 x 1080", "3 x 1080", "4 x 1080", "5 x 1080", "6 x 1080", "7 x 1080", "8 x 1080", "9 x 1080", "10 x 1080")
            AlertDialog.Builder(context).setItems(list, DialogInterface.OnClickListener { dialog, which ->
                var count = 0
                when (which) {
                    0 -> {
                        count = 1
                    }
                    1 -> {
                        count = 2
                    }
                    2 -> {
                        count = 3
                    }
                    3 -> {
                        count = 4
                    }
                    4 -> {
                        count = 5
                    }
                    5 -> {
                        count = 6
                    }
                    6 -> {
                        count = 7
                    }
                    7 -> {
                        count = 8
                    }
                    8 -> {
                        count = 9
                    }
                    9 -> {
                        count = 10
                    }
                }
                AlertDialog.Builder(context).setMessage("新增 $count 圈？").setNegativeButton("是", DialogInterface.OnClickListener { dialog, which ->
                    /**
                     * string phone, long startTime ,long duration , int count, string summary, int type
                     */
                    var dc = DataContext(context)
                    val latestBuddha = dc.latestBuddha
                    var duration = (10 * 60000 * count).toLong()
                    if (latestBuddha != null) {
                        duration = latestBuddha.duration / latestBuddha.count
                    }

                    var buddhaList: MutableList<BuddhaRecord> = ArrayList()
                    for (i in 1..count) {
                        val startTime = DateTime()
                        startTime.add(Calendar.MILLISECOND, (-1 * duration * i).toInt())
                        e("start time : ${startTime.toLongDateTimeString()}")
                        buddhaList.add(BuddhaRecord(startTime, duration, 1, 11, "计时计数念佛"))
                    }
                    dc.addBuddhas(buddhaList)
                    refreshTotalView()

                    _CloudUtils.addBuddhaList(context!!, buddhaList) { code, result ->
                        when (code) {
                            0 -> {
                                uiHandler.post {
                                    Toast.makeText(context, result.toString(), Toast.LENGTH_LONG).show()
                                }
                            }
                            else -> {
                                e("code : $code , result : $result")
                            }
                        }
                    }
                }).show()
            }).show()
            true
        }

        abtn_stock.setOnClickListener {
            //region 在StockReportService里面执行
            if (!_Utils.isRunService(context!!, StockService::class.qualifiedName!!)) {
                context!!.startService(Intent(context, StockService::class.java))
                dataContext.editSetting(Setting.KEYS.is_stocks_listener, true)
                animatorSuofang(abtn_stockAnimator)
            }
            //endregion
        }
        abtn_stock.setOnLongClickListener { //region 在StockReportService里面执行
            if (_Utils.isRunService(context!!, StockService::class.qualifiedName!!)) {
                context!!.stopService(Intent(context, StockService::class.java))
                dataContext.editSetting(Setting.KEYS.is_stocks_listener, false)
                stopAnimatorSuofang(abtn_stockAnimator)
            } else {
                context!!.startService(Intent(context, StockService::class.java))
                dataContext.editSetting(Setting.KEYS.is_stocks_listener, true)
                animatorSuofang(abtn_stockAnimator)
                //
                _Utils.clickHomeButton(context!!)
            }
            //endregion
            true
        }

        abtn_buddha.setOnClickListener {
            playBuddha()
        }
        btn_speed.setOnClickListener {
            AlertDialog.Builder(context).setItems(arrayOf("慢", "快"), DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    0 -> {
                        btn_speed.text = "慢"
                    }
                    1 -> {
                        btn_speed.text = "快"
                    }
                }
                dataContext.editSetting(Setting.KEYS.buddha_speed, which + 1)
                if (_Utils.isRunService(context!!, BuddhaService::class.qualifiedName!!)) {
                    context?.stopService(buddhaIntent)
                    context?.startService(buddhaIntent)
                }
            }).show()
        }
        super.onViewCreated(view, savedInstanceState)
    }

    lateinit var buddhaIntent: Intent
    private fun playBuddha() {
        try {
            val dc = DataContext(context)
            if (!_Utils.isRunService(context!!, BuddhaService::class.qualifiedName!!)) {
                e("开启")
                context?.startService(buddhaIntent)
                animatorSuofang(btn_buddha_animator)
            } else {
                e("关闭")
                context?.stopService(buddhaIntent)
                stopAnimatorSuofang(btn_buddha_animator)

                val setting = dc.getSetting(Setting.KEYS.buddha_duration)
                setting?.let {
                    val duration = it.long
                    val count = duration/600000
                    val hour = duration / (60000 * 60)
                    val minite = duration % (60000 * 60) / 60000
                    val hourS = "${hour}:"
                    val miniteS = if (minite == 0L) {
                        if (hour == 0L) "0" else "00"
                    } else {
                        if (minite < 10) "0${minite}" else "${minite}"
                    }
                    val msg = "${if (count > 0) "${hourS}${miniteS} \t ${DecimalFormat("#,000").format(duration)} \t ${count}" else ""}\n"
                    // TODO: 2021/1/20 asdf
                    AlertDialog.Builder(context).setMessage("有未保存的念佛记录【${msg}】，是否保存？").setNegativeButton("保存", DialogInterface.OnClickListener { dialog, which ->
                        /**
                         * string phone, long startTime ,long duration , int count, string summary, int type
                         */
                        val avgDuration = duration/count
                        var buddhaList: MutableList<BuddhaRecord> = ArrayList()
                        for (i in 1..count) {
                            val startTime = DateTime()
                            startTime.add(Calendar.MILLISECOND, (-1 * avgDuration * i).toInt())
                            buddhaList.add(BuddhaRecord(startTime, avgDuration, 1, 11, "计时计数念佛"))
                        }
                        dc.addBuddhas(buddhaList)
                        refreshTotalView()

                        _CloudUtils.addBuddhaList(context!!, buddhaList) { code, result ->
                            when (code) {
                                0 -> {
                                    uiHandler.post {
                                        Toast.makeText(context, result.toString(), Toast.LENGTH_LONG).show()
                                    }
                                }
                                else -> {
                                    e("code : $code , result : $result")
                                }
                            }
                        }
                    }).setPositiveButton("丢弃", DialogInterface.OnClickListener { dialog, which ->
                        dc.deleteSetting(Setting.KEYS.buddha_duration)
                    }).show()
                }
            }
        } catch (e: Exception) {
            e(e.message!!)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    //region 动画
    fun animatorSuofang(view: AnimatorSuofangView) {
        if (view.scaleX == null)
            view.scaleX = ObjectAnimator.ofFloat(view.targetView, "scaleX", 1f, 0.7f, 1f)
        view.scaleX!!.repeatCount = -1
        view.scaleX!!.duration = 600
        view.scaleX!!.start()
        if (view.scaleY == null)
            view.scaleY = ObjectAnimator.ofFloat(view.targetView, "scaleY", 1f, 0.7f, 1f)
        view.scaleY!!.repeatCount = -1
        view.scaleY!!.duration = 600
        view.scaleY!!.start()
    }

    fun stopAnimatorSuofang(view: AnimatorSuofangView) {
        if (view.targetView != null) {
            view.scaleY?.repeatCount = 0
            view.scaleX?.repeatCount = 0
        }
    }

    class AnimatorSuofangView {
        var scaleY: ObjectAnimator? = null
        var scaleX: ObjectAnimator? = null
        var targetView: View

        constructor(view: View) {
            this.targetView = view
        }
    }
    //endregion
}