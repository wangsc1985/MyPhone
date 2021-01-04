package com.wang17.myphone.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.wang17.myphone.R
import com.wang17.myphone.activity.*
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.e
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.service.TimerByMediaLoopService
import com.wang17.myphone.util.*
import com.wang17.myphone.view._Button
import kotlinx.android.synthetic.main.fragment_button.*
import java.io.*
import java.util.concurrent.CountDownLatch

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
        var btn = _Button(context!!, "备份")
        btn.setOnClickListener {
            AlertDialog.Builder(context!!).setMessage("确认要备份数据吗？").setPositiveButton("确定") { dialog, which ->
                BackupTask(context).execute(BackupTask.COMMAND_BACKUP)
            }.setNegativeButton("取消", null).show()
        }
        layout_flexbox.addView(btn)
        //endregion

        //region 恢复按钮
        btn = _Button(context!!, "恢复")
        btn.setOnClickListener {
            AlertDialog.Builder(context!!).setMessage("确认要恢复数据吗？").setPositiveButton("确定") { dialog, which ->
                BackupTask(context).execute(BackupTask.COMMAND_RESTORE)
            }.setNegativeButton("取消", null).show()
        }
        layout_flexbox.addView(btn)
        //endregion

        //region ST按钮
        btn = _Button(context!!, "ST")
        btn.setOnClickListener {
            context!!.startActivity(Intent(context, StockPositionActivity::class.java))
            val mDataContext = DataContext(context)
            mDataContext.editSetting(Setting.KEYS.quick, 1)
        }
        layout_flexbox.addView(btn)
        //endregion

        //region FU按钮
        btn = _Button(context!!, "FU")
        btn.setOnClickListener {
            context!!.startActivity(Intent(context, FuturePositionActivity::class.java))
            val mDataContext = DataContext(context)
            mDataContext.editSetting(Setting.KEYS.quick, 3)
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

        //region Fund
        btn = _Button(context!!, "Fund")
        btn.setOnClickListener {
            startActivity(Intent(context, FundMonitorActivity::class.java))
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
        btn = _Button(context!!, "历史")
        btn.setOnClickListener {
            try {
                context!!.startActivity(Intent(context, StockPositionHistoryActivity::class.java))
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        btn.setOnLongClickListener {
            context!!.startActivity(Intent(context, SmsActivity::class.java))
            true
        }
        layout_flexbox.addView(btn)
        //endregion

        //region 云库
        btn = _Button(context!!, "云库")
        btn.setOnClickListener {
            _CloudUtils.getNewMsg(context!!,object:CloudCallback{
                override fun excute(code: Int, msg: Any?) {
                    Looper.prepare()
                            AlertDialog.Builder(context!!).setMessage(msg.toString()).show()
                    Looper.loop()
                    }
                })
        }
        layout_flexbox.addView(btn)
        //endregion
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_button, container, false)
    }

}