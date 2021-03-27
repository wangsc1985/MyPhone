package com.wang17.myphone.fragment

import android.annotation.SuppressLint
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import com.alibaba.fastjson.JSONArray
import com.wang17.myphone.R
import com.wang17.myphone.activity.*
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.e
import com.wang17.myphone.model.Lottery
import com.wang17.myphone.database.BuddhaRecord
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.database.Setting
import com.wang17.myphone.service.BuddhaService
import com.wang17.myphone.service.StockService
import com.wang17.myphone.util.*
import com.wang17.myphone.view._Button
import kotlinx.android.synthetic.main.fragment_button.*
import java.text.DecimalFormat
import java.util.concurrent.CountDownLatch
import kotlin.collections.ArrayList


/**
 * A simple [Fragment] subclass.
 */
class ButtonFragment : Fragment() {
    private lateinit var dataContext: DataContext
    var uiHandler = Handler()

    @SuppressLint("ServiceCast")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataContext = DataContext(context)


        var btn = _Button(context!!, "设置")
        btn.setOnClickListener {
            startActivity(Intent(context!!, SettingActivity::class.java))
        }
        btn.setOnLongClickListener {
            AlertDialog.Builder(context!!).setItems(arrayOf("本地", "云端"), DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    0 -> {
                        startActivity(Intent(context!!, SettingActivity::class.java))
                    }
                    1 -> {
                        _CloudUtils.getSettingList(context!!, dataContext.getSetting(Setting.KEYS.wx_request_code, "0088").string, CloudCallback { code, result ->
                            uiHandler.post {
                                AlertDialog.Builder(context!!).setMessage(result.toString()).show()
                            }
                        })
                    }
                }
            }).show()
            true
        }
        layout_flexbox.addView(btn)
        kotlin.run {
            btn = _Button(context!!, "念佛")
            btn.setOnClickListener {
                startActivity(Intent(context!!, BuddhaActivity::class.java))
//                AlertDialog.Builder(context!!).setItems(arrayOf("详单","木鱼"), DialogInterface.OnClickListener { dialog, which ->
//                    when (which) {
//                        0 -> {
//                            startActivity(Intent(context!!, BuddhaActivity::class.java))
//                        }
//                        1->{
//                            startActivity(Intent(context!!, KnockerActivity::class.java))
//                        }
//                    }
//                }).show()
            }
            btn.setOnLongClickListener {
                AlertDialog.Builder(context!!).setItems(arrayOf("整合", "is run","木鱼"), DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        /**
                         * 整合念佛记录
                         */
                        0 -> {
                            val dc = DataContext(context)
                            val buddhaList = dc.allBuddhas
                            val removeList: MutableList<BuddhaRecord> = ArrayList()
                            var preBuddhaRecord: BuddhaRecord? = null
                            var count = 0
                            buddhaList.forEach { buddha ->
                                if (preBuddhaRecord != null) {
                                    if (buddha.startTime.timeInMillis - (preBuddhaRecord!!.startTime.timeInMillis + preBuddhaRecord!!.duration) < 60000 * 5) {
                                        preBuddhaRecord!!.duration += buddha.duration
                                        preBuddhaRecord!!.count += buddha.count
                                        removeList.add(buddha)
                                        count++
                                    } else {
                                        dc.editBuddha(preBuddhaRecord)
                                        preBuddhaRecord = buddha
                                    }
                                } else {
                                    preBuddhaRecord = buddha
                                }
                            }
                            preBuddhaRecord?.let {
                                dc.editBuddha(preBuddhaRecord)
                            }
                            dc.deleteBuddhaList(removeList)
                            AlertDialog.Builder(context!!).setMessage("整合完毕！共整合${count}条记录").show()
                        }
                        /**
                         * 判断念佛服务是否在运行
                         */
                        1 -> {
                            AlertDialog.Builder(context!!).setMessage("BuddhaService运行：${_Utils.isServiceRunning(context!!,BuddhaService::class.qualifiedName!!)}").show()

                        }
                        2->{
                            startActivity(Intent(context!!, KnockerActivity::class.java))
                        }
                        /**
                         * 向云端上传念佛记录
                         */
                        100 -> {
                            Thread {
                                val buddhaList = dataContext.allBuddhas
                                e("buddha size : ${buddhaList.size}")

                                val uploadList: MutableList<BuddhaRecord> = ArrayList()
                                for (i in buddhaList.indices) {
                                    uploadList.add(buddhaList[i])
                                    if (i % 100 == 0 && i != 0) {
                                        var latch = CountDownLatch(1)
                                        _CloudUtils.addBuddhaList(context!!, uploadList, CloudCallback { code, result ->
                                            e(result)
                                            uiHandler.post {
                                                AlertDialog.Builder(context!!).setMessage("当前索引：${i} 网络反馈：code ${code} result ${result.toString()}").setCancelable(false).setPositiveButton("继续", DialogInterface.OnClickListener { dialog, which ->
                                                    uploadList.clear()
                                                    latch.countDown()
                                                }).show()
                                            }
                                        })
                                        latch.await()
                                    }
                                }
                                _CloudUtils.addBuddhaList(context!!, uploadList, CloudCallback { code, result ->
                                    uiHandler.post {
                                        AlertDialog.Builder(context!!).setMessage(result.toString()).setCancelable(false).setPositiveButton("上传完毕", null).show()
                                    }
                                })

                            }.start()

                        }
                    }
                }).show()
                true
            }
            layout_flexbox.addView(btn)
        }
        kotlin.run {

            btn = _Button(context!!, "数据")
            btn.setOnClickListener {
                AlertDialog.Builder(context!!).setItems(arrayOf("备份", "恢复"), DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        0 -> {
                            BackupTask(context).execute(BackupTask.COMMAND_BACKUP)
                        }
                        1 -> {
                            BackupTask(context).execute(BackupTask.COMMAND_RESTORE)
                        }
                    }
                }).show()
            }
            layout_flexbox.addView(btn)
        }
        kotlin.run {
            btn = _Button(context!!, "彩票")
            btn.setOnClickListener {
                AlertDialog.Builder(context!!).setItems(arrayOf("大乐透", "双色球")) { dialog, which ->
                    when (which) {
                        0 -> {
                            redeemLottery(1)
                        }
                        1 -> {
                            redeemLottery(2)
                        }
                    }
                }.show()
            }
            btn.setOnLongClickListener {
                AlertDialog.Builder(context!!).setItems(arrayOf("大乐透", "双色球")) { dialog, which ->
                    when (which) {
                        0 -> {
                            addLottery(1)
                        }
                        1 -> {
                            addLottery(2)
                        }
                    }
                }.show()
                true
            }
            layout_flexbox.addView(btn)

        }

        kotlin.run {
            btn = _Button(context!!, "足迹")
            btn.setOnClickListener {
                try {
                    startActivity(Intent(context, LocationListActivity::class.java))
                } catch (e: Exception) {
                    _Utils.printException(context, e)
                }
            }
            layout_flexbox.addView(btn)
        }

        kotlin.run {
            btn = _Button(context!!, "trade")
            btn.setOnClickListener {
                AlertDialog.Builder(context!!).setItems(arrayOf("stock", "futrue", "history", "fund","is run"), DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        0 -> {
                            context!!.startActivity(Intent(context, StockPositionActivity::class.java))
                            val mDataContext = DataContext(context)
                            mDataContext.editSetting(Setting.KEYS.quick, 1)
                        }
                        1 -> {
                            context!!.startActivity(Intent(context, FuturePositionActivity::class.java))
                            val mDataContext = DataContext(context)
                            mDataContext.editSetting(Setting.KEYS.quick, 3)
                        }
                        2 -> {
                            startActivity(Intent(context, StockPositionHistoryActivity::class.java))
                        }
                        3 -> {
                            startActivity(Intent(context, FundMonitorActivity::class.java))
                        }
                        4->{
                            AlertDialog.Builder(context!!).setMessage("StockService运行状态：${_Utils.isServiceRunning(context!!, StockService::class.qualifiedName!!)}").show()
                        }
                    }
                }).show()


            }
            layout_flexbox.addView(btn)
        }

        kotlin.run {
            btn = _Button(context!!, "todo")
            btn.setOnClickListener {
                activity!!.startActivity(Intent(context, ToDoActivity::class.java))
            }
            layout_flexbox.addView(btn)
        }

        kotlin.run {
            btn = _Button(context!!, "日志")
            btn.setOnClickListener {
                try {
                    context!!.startActivity(Intent(context, RunLogActivity::class.java))
                } catch (e: Exception) {
                    _Utils.printException(context, e)
                }
            }
            layout_flexbox.addView(btn)
        }

        kotlin.run {
            btn = _Button(context!!, "sms")
            btn.setOnClickListener {
                val view = View.inflate(context, R.layout.inflate_dialog_pwd, null)
                val et = view.findViewById<EditText>(R.id.et_pwd)
                et.requestFocus()
                val imm = et.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(et, 0)
                var dialog = AlertDialog.Builder(context!!).setTitle("提示").setView(view).setNegativeButton("确定") { dialog, which ->
                    if (et.text.toString() == "6639") {
                        context!!.startActivity(Intent(context, SmsActivity::class.java))
                    } else {
                        AlertDialog.Builder(context!!).setMessage(et.text.toString()).show()
                    }
                }.show()

                et.setOnEditorActionListener { v, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        if (et.text.toString() == "6639") {
                            context!!.startActivity(Intent(context, SmsActivity::class.java))
                        } else {
                            AlertDialog.Builder(context!!).setMessage(et.text.toString()).show()
                        }
                        dialog.dismiss()
                    }
                    true
                }
                et.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val text = s ?: ""

                        if (text.length == 4) {
                            if (et.text.toString() == "6639") {
                                context!!.startActivity(Intent(context, SmsActivity::class.java))
                            } else {
                                AlertDialog.Builder(context!!).setMessage(et.text.toString()).show()
                            }
                            dialog.dismiss()
                        }
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    }
                })
            }
            layout_flexbox.addView(btn)
        }

        kotlin.run {

            var xiaoKnockSound = SoundPool(100, AudioManager.STREAM_MUSIC,0)
            xiaoKnockSound.load(context,R.raw.yq,1)
            btn = _Button(context!!, "测试")
            btn.setOnClickListener {
                xiaoKnockSound.play(1,1.0f,1.0f,0,0,1.0f)
            }
            layout_flexbox.addView(btn)
        }
//        kotlin.run {
//            btn = _Button(context!!, "intr")
//            btn.setOnClickListener {
//                var msg = interest(dataContext.getSetting(Setting.KEYS.interest, "60,14;60,7;60,7").string)
//                AlertDialog.Builder(context!!).setMessage(msg).show()
//            }
//            layout_flexbox.addView(btn)
//        }
    }

    fun redeemLottery(type:Int){
        val view = View.inflate(context!!, R.layout.dialog_lottery, null)
        var etP = view.findViewById<EditText>(R.id.et_period)
        var etM = view.findViewById<EditText>(R.id.et_multiple)
        etM.isEnabled=false
        var et1 = view.findViewById<EditText>(R.id.et_1)
        var et2 = view.findViewById<EditText>(R.id.et_2)
        var et3 = view.findViewById<EditText>(R.id.et_3)
        var et4 = view.findViewById<EditText>(R.id.et_4)
        var et5 = view.findViewById<EditText>(R.id.et_5)
        var et6 = view.findViewById<EditText>(R.id.et_6)
        var et7 = view.findViewById<EditText>(R.id.et_7)
        var tvInfo = view.findViewById<TextView>(R.id.tvInfo)
        etP.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus){
                etP.selectAll()
            }
        }
        etP.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etM.requestFocus()
                etM.selectAll()
            }
            true
        }
        etM.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                et1.requestFocus()
                et1.selectAll()
            }
            true
        }
        et1.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                et2.requestFocus()
                et2.selectAll()
            }
            true
        }
        et1.addTextChangedListener(object:TextWatcher{
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==2){
                    et2.requestFocus()
                    et2.selectAll()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
        et2.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                et3.requestFocus()
                et3.selectAll()
            }

            true
        }
        et2.addTextChangedListener(object:TextWatcher{
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==2){
                    et3.requestFocus()
                    et3.selectAll()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
        et3.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                et4.requestFocus()
                et4.selectAll()
            }
            true
        }
        et3.addTextChangedListener(object:TextWatcher{
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==2){
                    et4.requestFocus()
                    et4.selectAll()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
        et4.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                et5.requestFocus()
                et5.selectAll()
            }
            true
        }
        et4.addTextChangedListener(object:TextWatcher{
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==2){
                    et5.requestFocus()
                    et5.selectAll()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
        et5.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                et6.requestFocus()
                et6.selectAll()
            }
            true
        }
        et5.addTextChangedListener(object:TextWatcher{
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==2){
                    et6.requestFocus()
                    et6.selectAll()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
        et6.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                et7.requestFocus()
                et7.selectAll()
            }
            true
        }
        et6.addTextChangedListener(object:TextWatcher{
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==2){
                    et7.requestFocus()
                    et7.selectAll()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })

        val lots = LotteryUtil.fromJSONArray(dataContext.getSetting(Setting.KEYS.lotterys,"[]").string)
        val period = if(lots.size>0) lots[0].period else 0
        etP.setText("${period}")
        etP.isEnabled=false
        et7.addTextChangedListener(object:TextWatcher{
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==2){
                    val winLottery = Lottery(period,
                            arrayListOf(et1.text.toString().toInt(), et2.text.toString().toInt(), et3.text.toString().toInt(), et4.text.toString().toInt(), et5.text.toString().toInt(),et6.text.toString().toInt(), et7.text.toString().toInt()),
                            0, type)
                    when(type)
                    {
                        1->{
                            tvInfo.setText(LotteryUtil.dlt(lots,winLottery))
                        }
                        2->{
                            tvInfo.setText(LotteryUtil.ssq(lots,winLottery))
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
        AlertDialog.Builder(context!!).setView(view).setCancelable(false).setPositiveButton("确定", null).show()
    }

    fun addLottery(type: Int) {
        val view = View.inflate(context!!, R.layout.dialog_lottery, null)
        var etP = view.findViewById<EditText>(R.id.et_period)
        var etM = view.findViewById<EditText>(R.id.et_multiple)
        var et1 = view.findViewById<EditText>(R.id.et_1)
        var et2 = view.findViewById<EditText>(R.id.et_2)
        var et3 = view.findViewById<EditText>(R.id.et_3)
        var et4 = view.findViewById<EditText>(R.id.et_4)
        var et5 = view.findViewById<EditText>(R.id.et_5)
        var et6 = view.findViewById<EditText>(R.id.et_6)
        var et7 = view.findViewById<EditText>(R.id.et_7)
        var tvInfo = view.findViewById<TextView>(R.id.tvInfo)
        etP.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus){
                etP.selectAll()
            }
        }
        etP.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etM.requestFocus()
                etM.selectAll()
            }
            true
        }
        etM.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                et1.requestFocus()
                et1.selectAll()
            }
            true
        }
        et1.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                et2.requestFocus()
                et2.selectAll()
            }
            true
        }
        et1.addTextChangedListener(object:TextWatcher{
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==2){
                    et2.requestFocus()
                    et2.selectAll()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
        et2.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                et3.requestFocus()
                et3.selectAll()
            }

            true
        }
        et2.addTextChangedListener(object:TextWatcher{
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==2){
                    et3.requestFocus()
                    et3.selectAll()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
        et3.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                et4.requestFocus()
                et4.selectAll()
            }
            true
        }
        et3.addTextChangedListener(object:TextWatcher{
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==2){
                    et4.requestFocus()
                    et4.selectAll()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
        et4.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                et5.requestFocus()
                et5.selectAll()
            }
            true
        }
        et4.addTextChangedListener(object:TextWatcher{
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==2){
                    et5.requestFocus()
                    et5.selectAll()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
        et5.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                et6.requestFocus()
                et6.selectAll()
            }
            true
        }
        et5.addTextChangedListener(object:TextWatcher{
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==2){
                    et6.requestFocus()
                    et6.selectAll()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
        et6.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                et7.requestFocus()
                et7.selectAll()
            }
            true
        }
        et6.addTextChangedListener(object:TextWatcher{
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==2){
                    et7.requestFocus()
                    et7.selectAll()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })

        var lots = ArrayList<Lottery>()
        et7.addTextChangedListener(object:TextWatcher{
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==2){
                    val lot = Lottery(etP.text.toString().toInt(),
                            arrayListOf(et1.text.toString().toInt(), et2.text.toString().toInt(), et3.text.toString().toInt(), et4.text.toString().toInt(), et5.text.toString().toInt(),et6.text.toString().toInt(), et7.text.toString().toInt()),
                            etM.text.toString().toInt(), type)
                    lots.add(lot)
                    et1.setText("")
                    et2.setText("")
                    et3.setText("")
                    et4.setText("")
                    et5.setText("")
                    et6.setText("")
                    et7.setText("")
                    et1.requestFocus()
                    var ss = StringBuffer()
                    lots.forEach { lot ->
                        if (type == 1)
                            ss.appendLine("${lot.period}期  ${lot.redArr[0]}  ${lot.redArr[1]}  ${lot.redArr[2]}  ${lot.redArr[3]}  ${lot.redArr[4]} + ${lot.blueArr[0]}  ${lot.blueArr[1]}  ${lot.multiple}倍")
                        else
                            ss.appendLine("${lot.period}期  ${lot.redArr[0]}  ${lot.redArr[1]}  ${lot.redArr[2]}  ${lot.redArr[3]}  ${lot.redArr[4]}  ${lot.redArr[5]} + ${lot.blueArr[0]}  ${lot.multiple}倍")
                    }
                    tvInfo.setText(ss.toString())

                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })

        et7.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
            }
            true
        }

        AlertDialog.Builder(context!!).setView(view).setCancelable(false).setPositiveButton("确定", DialogInterface.OnClickListener { dialog, which ->
            dataContext.editSetting(Setting.KEYS.lotterys, JSONArray.toJSON(lots).toString())
        }).show()
    }

    fun interest(data: String): String {
        var result = StringBuffer()
        var totalInterest = 0f
        val rate = floatArrayOf(
                0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 2.80f,
                2.81f, 2.81f, 2.82f, 2.82f, 2.83f, 2.83f, 2.84f, 2.84f, 2.85f,
                2.86f, 2.86f, 2.87f, 2.87f, 2.88f, 2.88f, 2.89f, 2.89f, 2.90f,
                2.91f, 2.91f, 2.92f, 2.92f, 2.93f, 2.93f, 2.94f, 2.94f, 2.95f,
                2.96f, 2.96f, 2.97f, 2.97f, 2.98f, 2.98f, 2.99f, 2.99f, 3.00f
        )
        var objArr = data.split(";")
        for (i in objArr.indices) {
            val obj = objArr[i].split(",")
            val wan = obj[0].toInt()
            val days = obj[1].toInt()
            var interest = rate[days] * 100 / 365 * days * wan
            totalInterest += interest
            result.append("利率： \t${DecimalFormat("0.00").format(rate[days])} \t存期：\t${if (days < 10) "0" + days else days.toString()}天 \t 金额： \t${wan}万 \t 利息： \n${DecimalFormat("0.00").format(interest)}元\n\n")
        }

        result.append("\n")
        result.append("累计利息： \t${DecimalFormat("0.00").format(totalInterest)}元")
        return result.toString()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_button, container, false)
    }

}