package com.wang17.myphone.fragment

import android.annotation.SuppressLint
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat.getSystemService
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.wang17.myphone.R
import com.wang17.myphone.activity.*
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.e
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.database.BuddhaRecord
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.util.*
import com.wang17.myphone.view._Button
import kotlinx.android.synthetic.main.fragment_button.*
import okhttp3.internal.wait
import java.lang.StringBuilder
import java.sql.BatchUpdateException
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.collections.ArrayList


/**
 * A simple [Fragment] subclass.
 */
class ButtonFragment : Fragment() {
    private lateinit var numberSpeaker: NumberSpeaker
    private lateinit var dataContext: DataContext
    var uiHandler = Handler()

    @SuppressLint("ServiceCast")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataContext = DataContext(context)
        numberSpeaker = NumberSpeaker(context)


        var btn = _Button(context!!, "设置")
        btn.setOnClickListener {
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
        }
        layout_flexbox.addView(btn)

        kotlin.run {
            btn = _Button(context!!, "buddha")
            btn.setOnClickListener {
                AlertDialog.Builder(context!!).setItems(arrayOf("整合", "上传", "详单",), DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        0 -> {
                            val dc = DataContext(context)
                            val buddhaList = dc.getBuddhas("11")
                            val removeList: MutableList<BuddhaRecord> = ArrayList()
                            var tmp: BuddhaRecord? = null
                            buddhaList.forEach { buddha ->
                                if (tmp != null) {
                                    if (buddha.startTime.get(Calendar.DAY_OF_YEAR) != tmp!!.startTime.get(Calendar.DAY_OF_YEAR)) {
                                        dc.editBuddha(tmp)
                                        tmp = buddha
                                    } else {
                                        if (buddha.startTime.hour == tmp!!.startTime.hour) {
                                            tmp!!.duration += buddha.duration
                                            tmp!!.count += buddha.count
                                            removeList.add(buddha)
                                        } else {
                                            dc.editBuddha(tmp)
                                            tmp = buddha
                                        }
                                    }
                                } else {
                                    tmp = buddha
                                }
                            }
                            tmp?.let {
                                dc.editBuddha(tmp)
                            }
                            dc.deleteBuddhaList(removeList)
                            AlertDialog.Builder(context!!).setMessage("整合完毕！").show()
                        }
                        1 -> {
                            Thread {
                                val buddhaList = dataContext.allBuddhas
                                e("buddha size : ${buddhaList.size}")

                                val uploadList: MutableList<BuddhaRecord> = ArrayList()
                                for (i in buddhaList.indices) {
                                    uploadList.add(buddhaList[i])
                                    if (i % 200 == 0 && i != 0) {
                                        var latch = CountDownLatch(1)
                                        _CloudUtils.addBuddhaList(context!!, uploadList, CloudCallback { code, result ->
                                            e(result)
                                            uiHandler.post {
                                                AlertDialog.Builder(context!!).setMessage("当前索引：${i}  网络反馈：${result.toString()}").setCancelable(false).setPositiveButton("继续", DialogInterface.OnClickListener { dialog, which ->
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
                        2 -> {
                            startActivity(Intent(context!!, BuddhaActivity::class.java))
                        }
                    }
                }).show()
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

                        }
                        1 -> {

                        }
                    }
                }.show()
            }
            btn.setOnLongClickListener {
                AlertDialog.Builder(context!!).setItems(arrayOf("大乐透", "双色球")) { dialog, which ->
                    when (which) {
                        0 -> {

                        }
                        1 -> {

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
                AlertDialog.Builder(context!!).setItems(arrayOf("stock", "futrue", "history", "fund"), DialogInterface.OnClickListener { dialog, which ->
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
            btn = _Button(context!!, "intr")
            btn.setOnClickListener {
                var msg = interest(dataContext.getSetting(Setting.KEYS.interest, "60,14;60,7;60,7").string)
                AlertDialog.Builder(context!!).setMessage(msg).show()
            }
            layout_flexbox.addView(btn)
        }
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_button, container, false)
    }

}