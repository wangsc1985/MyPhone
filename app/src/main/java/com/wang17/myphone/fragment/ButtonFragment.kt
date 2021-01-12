package com.wang17.myphone.fragment

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import com.wang17.myphone.R
import com.wang17.myphone.activity.*
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.util.BackupTask
import com.wang17.myphone.util.DataContext
import com.wang17.myphone.util.NumberSpeaker
import com.wang17.myphone.util._Utils
import com.wang17.myphone.view._Button
import kotlinx.android.synthetic.main.fragment_button.*
import java.text.DecimalFormat


/**
 * A simple [Fragment] subclass.
 */
class ButtonFragment : Fragment() {
    private lateinit var numberSpeaker: NumberSpeaker
    private lateinit var dataContext: DataContext

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataContext = DataContext(context)
        numberSpeaker = NumberSpeaker(context)

        //region 备份按钮
        var btn = _Button(context!!, "数据")
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
        //endregion

        //region ST按钮
        btn = _Button(context!!, "TRADE")
        btn.setOnClickListener {
            AlertDialog.Builder(context!!).setItems(arrayOf("ST", "FT", "HIS", "FUN"), DialogInterface.OnClickListener { dialog, which ->
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
        //endregion


        //region 足迹
        btn = _Button(context!!, "足迹")
        btn.setOnClickListener {
            try {
                startActivity(Intent(context, LocationListActivity::class.java))
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        layout_flexbox.addView(btn)
        //endregion


        //region TODO界面
        btn = _Button(context!!, "TODO")
        btn.setOnClickListener {
            activity!!.startActivity(Intent(context, ToDoActivity::class.java))
        }
        layout_flexbox.addView(btn)
        //endregion


        //region 日志
        btn = _Button(context!!, "日志")
        btn.setOnClickListener {
            try {
                context!!.startActivity(Intent(context, RunLogActivity::class.java))
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        layout_flexbox.addView(btn)
        //endregion

        //region 历史持仓
        btn = _Button(context!!, "SMS")
        btn.setOnClickListener {
            val view = View.inflate(context,R.layout.inflate_dialog_pwd,null)
            val et = view.findViewById<EditText>(R.id.et_pwd)
            AlertDialog.Builder(context!!).setTitle("提示").setView(view).setNegativeButton("确定") { dialog, which ->
                if(et.text.toString()=="6639"){
                    context!!.startActivity(Intent(context, SmsActivity::class.java))
                }else{
                    AlertDialog.Builder(context!!).setMessage(et.text.toString()).show()
                }
            }.show()
        }
        layout_flexbox.addView(btn)
        //endregion

        //region 云库
        btn = _Button(context!!, "INTR")
        btn.setOnClickListener {
            var msg = interest(dataContext.getSetting(Setting.KEYS.interest, "60,14;60,7;60,7").string)
            AlertDialog.Builder(context!!).setMessage(msg).show()
        }
        layout_flexbox.addView(btn)
        //endregion
    }

    fun interest(data: String):String{
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
            result.append("利率： \t${rate[days]} \t存期：\t${days}天 \t 金额： \t${wan}万 \t 本期利息： \t${DecimalFormat("0.00").format(interest)}元\n")
        }
        result.append("累计利息： \t${DecimalFormat("0.00").format(totalInterest)}元")
        return result.toString()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_button, container, false)
    }

}