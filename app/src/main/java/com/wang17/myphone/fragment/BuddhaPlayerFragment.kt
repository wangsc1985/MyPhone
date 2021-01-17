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
import com.wang17.myphone.service.BuddhaPlayerService
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
    lateinit var btn_kAnimator: AnimatorSuofangView
    lateinit var btn_mAnimator: AnimatorSuofangView
    lateinit var btn_zAnimator: AnimatorSuofangView
    var uiHandler = Handler()

    override fun onResume() {
        super.onResume()

        val dataContext = DataContext(context)
        val buddha =  dataContext.getLatestBuddha()
        var time = DateTime(1970,1,1)
        if(buddha!=null){
            time = buddha.startTime
        }
        _CloudUtils.loadBuddha(context!!,time) { code, result ->
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
                        buddhaS.add(BuddhaRecord(DateTime(startTime),duration,count,type,summary))
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
            if(hour==0L) "0" else "00"
        } else {
            if (minite < 10) "0${minite}" else "${minite}"
        }

        val hour1 = totalDayDuration / (60000 * 60)
        val minite1 = totalDayDuration % (60000 * 60) / 60000
        val hourS1 = "${hour1}:"
        val miniteS1 = if (minite1 == 0L) {
            if(hour1==0L) "0" else "00"
        } else {
            if (minite1 < 10) "0${minite1}" else "${minite1}"
        }
        uiHandler.post {
            tv_monthTotal.setText("${hourS}${miniteS}  ${formatter.format(totalMonthCount * 1080)}  ${DecimalFormat("0.0").format(totalMonthCount.toBigDecimal().setScale(1)/now.day.toBigDecimal())}")
            tv_dayTotal.setText("${hourS1}${miniteS1}  ${formatter.format(totalDayCount * 1080)}  ${totalDayCount}")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val dataContext = DataContext(context)
        abtn_stockAnimator = AnimatorSuofangView(abtn_stock)
        btn_kAnimator = AnimatorSuofangView(btn_k)
        btn_mAnimator = AnimatorSuofangView(btn_m)
        btn_zAnimator = AnimatorSuofangView(btn_z)
        if (dataContext.getSetting(Setting.KEYS.is_stocks_listener, false).boolean == true) {
            animatorSuofang(abtn_stockAnimator)
        }
        if (dataContext.getSetting(Setting.KEYS.is_mnf, false).boolean == true) {
            animatorSuofang(btn_mAnimator)
        }
        if (dataContext.getSetting(Setting.KEYS.is_knf, false).boolean == true) {
            animatorSuofang(btn_kAnimator)
        }
        if (dataContext.getSetting(Setting.KEYS.is_znf, false).boolean == true) {
            animatorSuofang(btn_zAnimator)
        }

        var totalCount = 0
        layout_dayTotal.setOnClickListener {
            val dayBuddhaList = dataContext.getBuddhas(DateTime())
            var items = arrayOfNulls<String>(dayBuddhaList.size)
            for(index in dayBuddhaList.indices){
                val hour1 = dayBuddhaList[index].duration / (60000 * 60)
                val minite1 = dayBuddhaList[index].duration % (60000 * 60) / 60000
                val hourS1 = "${hour1}:"
                val miniteS1 = if (minite1 == 0L) {if(hour1==0L) "0" else "00"} else {
                    if (minite1 < 10) "0${minite1}" else "${minite1}"
                }
                totalCount+=dayBuddhaList[index].count
                items[index] = "${dayBuddhaList[index].startTime.toShortTimeString()}   ${hourS1}${miniteS1}   ${dayBuddhaList[index].count}"
            }
            if(totalCount>0)
            AlertDialog.Builder(context).setItems(items, DialogInterface.OnClickListener { dialog, index ->
                AlertDialog.Builder(context)
                        .setMessage("是否要删除【${dayBuddhaList[index].startTime.toLongDateTimeString()}】的记录?")
                        .setNegativeButton("是", DialogInterface.OnClickListener { dialog, which ->
                            var dc = DataContext(context)
                            dc.deleteBuddha(dayBuddhaList[index].id)
                            refreshTotalView()

                            _CloudUtils.delBuddha(context!!,dayBuddhaList[index]) { code, result ->
                                when(code){
                                    0->{
                                        uiHandler.post {
                                            Toast.makeText(context,result.toString(),Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    else->{
                                        e("code : $code , result : $result")
                                    }
                                }
                            }
                }).show()
            }).show()
        }
        layout_monthTotal.setOnLongClickListener {

            val today = DateTime.today
            val monthBuddhaList = dataContext.getBuddhas(today.year,today.month)
            var start = DateTime(today.year,today.month,1)
            var sb = StringBuilder()
            var totalDuration=0L
            var totalCount = 0
            while (start.year==today.year&&start.get(Calendar.DAY_OF_YEAR)<=today.get(Calendar.DAY_OF_YEAR)){
                var list = monthBuddhaList.filter {
                    val time = it.startTime
                    time.year==start.year&&time.get(Calendar.DAY_OF_YEAR)==start.get(Calendar.DAY_OF_YEAR)
                }
                var duration = 0L
                var count =0
                var time = start

                list.forEach {
                    time = it.startTime
                    count += it.count
                    duration+=it.duration
                    e("time : ${time.toLongDateTimeString()}")
                }

                val hour = duration / (60000 * 60)
                val minite = duration % (60000 * 60) / 60000
                val hourS = "${hour}:"
                val miniteS = if (minite == 0L) {if(hour==0L) "0" else "00"} else {
                    if (minite < 10) "0${minite}" else "${minite}"
                }
                sb.append("${time.toShortDateString1()} \t ${if(count>0) "${hourS}${miniteS} \t ${DecimalFormat("#,000").format(count*1080)} \t ${count}" else ""}\n")

                totalCount+=count
                totalDuration+=duration
                start = start.addDays(1)
            }
            val hour = totalDuration / (60000 * 60)
            val minite = totalDuration % (60000 * 60) / 60000
            val hourS = "${hour}:"
            val miniteS = if (minite == 0L) {if(hour==0L) "0" else "00"} else {
                if (minite < 10) "0${minite}" else "${minite}"
            }
            val avgCount = totalCount.toBigDecimal().setScale(1)/today.day.toBigDecimal()
            sb.append("\n${hourS}${miniteS} \t ${DecimalFormat("#,000").format(totalCount*1080)} \t ${totalCount} \t ${avgCount}\n")
            if(totalCount>0)
                AlertDialog.Builder(context).setMessage(sb.toString()).show()
            true
        }

        iv_buddha.setOnLongClickListener {
            val list = arrayOf("1*1080","2*1080","3*1080","4*1080","5*1080","6*1080","7*1080","8*1080","9*1080","10*1080")
            AlertDialog.Builder(context).setItems(list, DialogInterface.OnClickListener { dialog, which ->
                var count = 0
                when(which){
                    0->{ count=1 }
                    1->{ count=2 }
                    2->{ count=3 }
                    3->{ count=4 }
                    4->{ count=5 }
                    5->{ count=6 }
                    6->{ count=7 }
                    7->{ count=8 }
                    8->{ count=9 }
                    9->{ count=10 }
                }
                /**
                 * string phone, long startTime ,long duration , int count, string summary, int type
                 */
                var dc = DataContext(context)
                val latestBuddha =  dc.latestBuddha
                var duration = (10*60000*count).toLong()
                if(latestBuddha!=null){
                    duration = latestBuddha.duration/latestBuddha.count
                }

                var buddhaList:MutableList<BuddhaRecord> = ArrayList()
                for(i in 1..count){
                    val startTime = DateTime()
                    startTime.add(Calendar.MILLISECOND,(-1*duration*i).toInt())
                    e("start time : ${startTime.toLongDateTimeString()}")
                    buddhaList.add(BuddhaRecord(startTime,duration,1,11,"计时计数念佛"))
                }
                dc.addBuddhas(buddhaList)
                refreshTotalView()

                _CloudUtils.addBuddhaList(context!!,buddhaList) { code, result ->
                    when(code){
                        0->{
                            uiHandler.post {
                                Toast.makeText(context,result.toString(),Toast.LENGTH_LONG).show()
                            }
                        }
                        else->{
                            e("code : $code , result : $result")
                        }
                    }
                }
            }).show()
            true
        }

//        iv_buddha.setOnClickListener {
//            val now = DateTime()
//            val dialogView = View.inflate(context,R.layout.inflate_dialog_add_buddha,null)
//            val cv_date = dialogView.findViewById<CalendarView>(R.id.cv_date)
//            val et_count = dialogView.findViewById<EditText>(R.id.et_count)
//            val iv_minus = dialogView.findViewById<ImageView>(R.id.iv_minus)
//            val iv_add = dialogView.findViewById<ImageView>(R.id.iv_add)
//            cv_date.date = now.timeInMillis
//            et_count.setText("1")
//            cv_date.setOnDateChangeListener { view, year, month, dayOfMonth ->
//                val now = DateTime()
//                now.set(Calendar.YEAR,year)
//                now.set(Calendar.MONTH,month)
//                now.set(Calendar.DAY_OF_MONTH,dayOfMonth)
//                view.date = now.timeInMillis
//            }
//            iv_add.setOnClickListener {
//                et_count.setText((et_count.text.toString().toInt()+1).toString())
//            }
//            iv_minus.setOnClickListener {
//                val count = et_count.text.toString().toInt()-1
//                et_count.setText(if(count<1) "1" else count.toString())
//            }
//
//
//            AlertDialog.Builder(context).setView(dialogView).setPositiveButton("添加", DialogInterface.OnClickListener { dialog, which ->
//                /**
//                 * string phone, long startTime ,long duration , int count, string summary, int type
//                 **/
//                var dc = DataContext(context)
//                val bb =  dc.latestBuddha
//                val count = et_count.text.toString().toInt()
//                val startTime = DateTime(cv_date.date)
//                var duration = (10*60000*count).toLong()
//                if(bb!=null){
//                    duration = bb.duration/bb.count*count
//                }
//                startTime.add(Calendar.MILLISECOND,(-1*duration).toInt())
//                e("date : ${startTime.toLongDateTimeString()}")
//                val buddha = BuddhaRecord(startTime,duration,count,11,"计时计数念佛")
//                dc.addBuddha(buddha)
//                refreshTotalView()
//
//                _CloudUtils.addBuddha(context!!,buddha, CloudCallback { code, result ->
//                    when(code){
//                        0->{
//                            uiHandler.post {
//                                Toast.makeText(context,result.toString(),Toast.LENGTH_LONG).show()
//                            }
//                        }
//                        else->{
//                            e("code : $code , result : $result")
//                        }
//                    }
//                })
//            }).show()
//            true
//        }

        abtn_stock.setOnClickListener {
            //region 在StockReportService里面执行
            if (dataContext.getSetting(Setting.KEYS.is_stocks_listener, false).boolean == false) {
                context!!.startService(Intent(context, StockService::class.java))
                dataContext.editSetting(Setting.KEYS.is_stocks_listener, true)
                animatorSuofang(abtn_stockAnimator)
            }
            //endregion
        }
        abtn_stock.setOnLongClickListener { //region 在StockReportService里面执行
            if (dataContext.getSetting(Setting.KEYS.is_stocks_listener, false).boolean == true) {
                context!!.stopService(Intent(context, StockService::class.java))
                dataContext.editSetting(Setting.KEYS.is_stocks_listener, false)
                stopAnimatorSuofang(abtn_stockAnimator)
            } else {
                context!!.startService(Intent(context, StockService::class.java))
                dataContext.editSetting(Setting.KEYS.is_stocks_listener, true)
                animatorSuofang(abtn_stockAnimator)
                //
                _Utils.clickHomeButton(context)
            }
            //endregion
            true
        }

        btn_m.setOnClickListener {
            try {
                stopAnimatorSuofang(btn_kAnimator)
                stopAnimatorSuofang(btn_mAnimator)
                stopAnimatorSuofang(btn_zAnimator)

                var intent = Intent(context,BuddhaPlayerService::class.java)
                if (dataContext.getSetting(Setting.KEYS.is_mnf, false).boolean == false) {
                    e("开启")
                    intent.putExtra("velocity", 1)
                    context?.startService(intent)
                    animatorSuofang(btn_mAnimator)
                    dataContext.editSetting(Setting.KEYS.is_mnf, true)
                    dataContext.editSetting(Setting.KEYS.is_knf, false)
                    dataContext.editSetting(Setting.KEYS.is_znf, false)
                } else {
                    e("关闭")
                    context?.stopService(intent)
                    dataContext.editSetting(Setting.KEYS.is_mnf, false)
                    stopAnimatorSuofang(btn_mAnimator)
                }
            } catch (e: Exception) {
                e(e.message!!)
            }
        }
        btn_k.setOnClickListener {
            try {
                stopAnimatorSuofang(btn_kAnimator)
                stopAnimatorSuofang(btn_mAnimator)
                stopAnimatorSuofang(btn_zAnimator)
                animatorSuofang(btn_kAnimator)

                var intent = Intent(context,BuddhaPlayerService::class.java)
                if (dataContext.getSetting(Setting.KEYS.is_knf, false).boolean == false) {
                    e("开启")
                    intent.putExtra("velocity", 3)
                    context?.startService(intent)
                    animatorSuofang(btn_kAnimator)
                    dataContext.editSetting(Setting.KEYS.is_knf, true)
                    dataContext.editSetting(Setting.KEYS.is_mnf, false)
                    dataContext.editSetting(Setting.KEYS.is_znf, false)
                } else {
                    e("关闭")
                    context?.stopService(intent)
                    dataContext.editSetting(Setting.KEYS.is_knf, false)
                    stopAnimatorSuofang(btn_kAnimator)
                }
            } catch (e: Exception) {
                e(e.message!!)
            }
        }
        btn_z.setOnClickListener {
            try {
                stopAnimatorSuofang(btn_kAnimator)
                stopAnimatorSuofang(btn_mAnimator)
                stopAnimatorSuofang(btn_zAnimator)
                animatorSuofang(btn_zAnimator)

                var intent = Intent(context,BuddhaPlayerService::class.java)
                if (dataContext.getSetting(Setting.KEYS.is_znf, false).boolean == false) {
                    e("开启")
                    intent.putExtra("velocity", 2)
                    context?.startService(intent)
                    animatorSuofang(btn_zAnimator)
                    dataContext.editSetting(Setting.KEYS.is_znf, true)
                    dataContext.editSetting(Setting.KEYS.is_mnf, false)
                    dataContext.editSetting(Setting.KEYS.is_knf, false)
                } else {
                    e("关闭")
                    context?.stopService(intent)
                    dataContext.editSetting(Setting.KEYS.is_znf, false)
                    stopAnimatorSuofang(btn_zAnimator)
                }
            } catch (e: Exception) {
                e(e.message!!)
            }
        }
        super.onViewCreated(view, savedInstanceState)
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