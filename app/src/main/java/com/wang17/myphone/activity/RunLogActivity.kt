package com.wang17.myphone.activity

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import com.wang17.myphone.R
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.database.RunLog
import com.wang17.myphone.util._Utils
import com.wang17.myphone.util._Utils.e
import kotlinx.android.synthetic.main.activity_runlog.*
import kotlinx.android.synthetic.main.fragment_to_do.*
import java.util.*

class RunLogActivity : FragmentActivity() {
    private lateinit var mDataContext: DataContext
    private var currentRunLogs: MutableList<RunLog> = ArrayList()
    private lateinit var allRunLogs:MutableList<RunLog>
    private lateinit var listAdapter: RunlogListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_runlog)
        try {
            mDataContext = DataContext(this)
            allRunLogs = mDataContext.runLogs
            currentRunLogs = allRunLogs

            listView_runlog.setOnItemClickListener(OnItemClickListener { parent, view, position, id ->
                AlertDialog.Builder(this@RunLogActivity).setMessage("要删除当前日志吗？").setNegativeButton("确定") { dialog, which ->
                    mDataContext.deleteRunLog(currentRunLogs.get(position).id)
                    val tag = spinner_logTag.selectedItem.toString()
                    currentRunLogs = mDataContext.getRunLogsByTag(tag)
                    listAdapter.notifyDataSetChanged()
                }.show()
            })
            listView_runlog.setOnItemLongClickListener(OnItemLongClickListener { parent, view, position, id ->
                AlertDialog.Builder(this@RunLogActivity).setMessage("要删除所有日志吗？").setNegativeButton("确定") { dialog, which ->
                    val tag = spinner_logTag.selectedItem.toString()
                    mDataContext.clearRunLogByTag(tag)
                    currentRunLogs = mDataContext.getRunLogsByTag(tag)
                    listAdapter.notifyDataSetChanged()
                }.show()
                true
            })

            initLocationGear()
            spinner_logTag.onItemSelectedListener = object:AdapterView.OnItemSelectedListener{
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val tag = spinner_logTag.selectedItem.toString()
                    currentRunLogs = mDataContext.getRunLogsByTag(tag)
                    e("xxxxxxxxxxxxxxxxxxxxxxxxxxx run log size : "+currentRunLogs.size)
                    listAdapter.notifyDataSetChanged()
                    e(1)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
            listAdapter = RunlogListAdapter()
            listView_runlog.setAdapter(listAdapter)
        } catch (e: Exception) {
            mDataContext.addRunLog("err","运行错误",e.message)
            e(e.message!!)
        }
    }

    private fun initLocationGear() {
        val list: MutableList<String> = ArrayList()
        val logs = mDataContext.runLogs
        for (i in 0 until logs.size) {
            val tag = logs[i].tag
            if(!list.contains(tag)){
                list.add(tag)
            }
        }
        if(list.size>0) {
            spinner_logTag.visibility = View.VISIBLE
            this.fillSpinner(spinner_logTag, list)
        }
            else{
            spinner_logTag.visibility = View.INVISIBLE
        }
    }

    private fun fillSpinner(spinner: Spinner, values: List<String?>) {
        val aspn: ArrayAdapter<String?> = ArrayAdapter<String?>(this, R.layout.inflate_spinner, values)
        aspn.setDropDownViewResource(R.layout.inflate_spinner_dropdown)
        spinner.adapter = aspn
    }

    protected inner class RunlogListAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return currentRunLogs.size
        }

        override fun getItem(position: Int): Any? {
            return currentRunLogs[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var cv = convertView
            cv = View.inflate(this@RunLogActivity, R.layout.inflate_runlog, null)
            try {
                val log = currentRunLogs[position]
                val textView_dateTime = cv.findViewById<TextView>(R.id.tv_dateTime)
                val textView_title = cv.findViewById<TextView>(R.id.textView_title)
                val textView_body = cv.findViewById<TextView>(R.id.tv_body)
                textView_dateTime.text = log.runTime.toLongDateTimeString()
                textView_title.text = log.item
                textView_body.text = log.message
            } catch (e: Exception) {
                _Utils.printException(this@RunLogActivity, e)
                Log.e("wangsc", e.message ?: "")
            }
            return cv
        }
    }
}