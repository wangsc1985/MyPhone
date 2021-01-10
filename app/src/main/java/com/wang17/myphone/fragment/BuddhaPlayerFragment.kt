package com.wang17.myphone.fragment

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.design.widget.Snackbar
import com.wang17.myphone.e
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.alibaba.fastjson.JSON
import com.wang17.myphone.R
import com.wang17.myphone.callback.CloudCallback
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
        val miniteS = if (minite == 0L) "0" else {
            if (minite < 10) "0${minite}" else "${minite}"
        }

        val hour1 = totalDayDuration / (60000 * 60)
        val minite1 = totalDayDuration % (60000 * 60) / 60000
        val hourS1 = "${hour1}:"
        val miniteS1 = if (minite1 == 0L) "0" else {
            if (minite1 < 10) "0${minite1}" else "${minite1}"
        }
        uiHandler.post {
            tv_dayTotal.setText("${hourS}${miniteS}  ${formatter.format(totalMonthCount * 1080)}")
            tv_monthTotal.setText("${hourS1}${miniteS1}  ${formatter.format(totalDayCount * 1080)}")
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

        iv_buddha.setOnLongClickListener {
            val now = DateTime()
            val dialogView = View.inflate(context,R.layout.inflate_dialog_add_buddha,null)
            val cv_date = dialogView.findViewById<CalendarView>(R.id.cv_date)
//            val et_hour = dialogView.findViewById<EditText>(R.id.et_hour)
            val et_count = dialogView.findViewById<EditText>(R.id.et_count)
            val iv_minus = dialogView.findViewById<ImageView>(R.id.iv_minus)
            val iv_add = dialogView.findViewById<ImageView>(R.id.iv_add)
            cv_date.date = now.timeInMillis
//            et_hour.setText(now.hour)
            et_count.setText("1")
            cv_date.setOnDateChangeListener { view, year, month, dayOfMonth ->
                val now = DateTime()
                now.set(Calendar.YEAR,year)
                now.set(Calendar.MONTH,month)
                now.set(Calendar.DAY_OF_MONTH,dayOfMonth)
                view.date = now.timeInMillis
            }
            iv_add.setOnClickListener {
                et_count.setText((et_count.text.toString().toInt()+1).toString())
            }
            iv_minus.setOnClickListener {
                val count = et_count.text.toString().toInt()-1
                et_count.setText(if(count<1) "1" else count.toString())
            }

            AlertDialog.Builder(context).setView(dialogView).setPositiveButton("添加", DialogInterface.OnClickListener { dialog, which ->
                /**
                 * string phone, long startTime ,long duration , int count, string summary, int type
                 */
                val count = et_count.text.toString().toInt()
                val startTime = DateTime(cv_date.date)
                startTime.add(Calendar.MINUTE,-12*count)
                e("date : ${startTime.toLongDateTimeString()}")
                val duration = 12*60000*count.toLong()
                var dc = DataContext(context)
                dc.addBuddha(BuddhaRecord(startTime,duration,count,11,"计时计数念佛"))
                refreshTotalView()

                _CloudUtils.addBuddha(context!!,startTime,duration,count,"计时计数念佛",11, CloudCallback { code, result ->
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
                })
            }).show()
            true
        }

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

                val intent = Intent(context, BuddhaPlayerService::class.java)
                if (dataContext.getSetting(Setting.KEYS.is_mnf, false).boolean == false) {
                    intent.putExtra("velocity", 1)
                    context!!.startService(intent)
                    animatorSuofang(btn_mAnimator)
                    dataContext.editSetting(Setting.KEYS.is_mnf, true)
                    dataContext.editSetting(Setting.KEYS.is_knf, false)
                    dataContext.editSetting(Setting.KEYS.is_znf, false)
                } else {
                    context!!.stopService(intent)
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

                val intent = Intent(context, BuddhaPlayerService::class.java)
                if (dataContext.getSetting(Setting.KEYS.is_knf, false).boolean == false) {
                    intent.putExtra("velocity", 3)
                    context!!.startService(intent)
                    animatorSuofang(btn_kAnimator)
                    dataContext.editSetting(Setting.KEYS.is_knf, true)
                    dataContext.editSetting(Setting.KEYS.is_mnf, false)
                    dataContext.editSetting(Setting.KEYS.is_znf, false)
                } else {
                    context!!.stopService(intent)
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

                val intent = Intent(context, BuddhaPlayerService::class.java)
                if (dataContext.getSetting(Setting.KEYS.is_znf, false).boolean == false) {
                    intent.putExtra("velocity", 2)
                    context!!.startService(intent)
                    animatorSuofang(btn_zAnimator)
                    dataContext.editSetting(Setting.KEYS.is_znf, true)
                    dataContext.editSetting(Setting.KEYS.is_mnf, false)
                    dataContext.editSetting(Setting.KEYS.is_znf, false)
                } else {
                    context!!.stopService(intent)
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