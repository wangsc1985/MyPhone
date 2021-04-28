package com.wang17.myphone.activity

import android.app.AlertDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemLongClickListener
import com.wang17.myphone.R
import com.wang17.myphone.e
import com.wang17.myphone.fragment.ActionBarFragment.OnActionFragmentBackListener
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.DateTime.Companion.toSpanString
import com.wang17.myphone.database.BuddhaRecord
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.util._CloudUtils
import com.wang17.myphone.util._Utils.printException
import kotlinx.android.synthetic.main.activity_buddha_detail.*
import kotlinx.android.synthetic.main.widget_timer.*
import java.text.DecimalFormat

class BuddhaDetailActivity : AppCompatActivity(), OnActionFragmentBackListener {
    private var start: Long = 0
    private lateinit var dataContext: DataContext
    private lateinit var buddhaList: List<BuddhaRecord>
    private lateinit var recordListdAdapter: RecordListdAdapter

    var duration = 0L
    var count = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buddha_detail)
        dataContext = DataContext(this)
        start = intent.getLongExtra("start", 0)
        refreshData()
        recordListdAdapter = RecordListdAdapter()
        listView_records.adapter = recordListdAdapter
        listView_records.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            try {
                val view = View.inflate(this, R.layout.inflate_dialog_editbox, null)
                val buddha = buddhaList.get(position)
                val spType = view.findViewById<Spinner>(R.id.sp_type)
                val edit = view.findViewById<EditText>(R.id.et_content)
                edit.setText(buddha.summary)
                when (buddha.type) {
                    0 -> {
                        spType.setSelection(0)
                    }
                    1 -> {
                        spType.setSelection(1)
                    }
                    10->{
                        spType.setSelection(2)
                    }
                    11 -> {
                        spType.setSelection(3)
                    }
                }
                AlertDialog.Builder(this).setView(view).setPositiveButton("修改") { dialog, which ->
                    val type = spType.selectedItem.toString().toInt()
                    if(edit.text.toString()!=buddha.summary||type!=buddha.type){
                        buddha.type = type
                        buddha.summary = edit.text.toString()
                        _CloudUtils.editBuddha(this, buddha) { code, result ->
                            when (code) {
                                0 -> {
                                    dataContext.editBuddha(buddha)
                                    isChanged = true
                                    runOnUiThread {
                                        refreshData()
                                        recordListdAdapter.notifyDataSetChanged()
                                    }
                                }
                            }
                            runOnUiThread {
                                AlertDialog.Builder(this).setMessage(result.toString()).show()
                            }
                        }
                    }
                }.show()
            } catch (e: Exception) {
                dataContext.addRunLog("err", "修改buddha错误", e.message)
            }
        }
        listView_records.onItemLongClickListener = OnItemLongClickListener { parent, view, position, id ->
            AlertDialog.Builder(this).setMessage("确认要删除当前记录吗？").setPositiveButton("确定") { dialog, which ->
                dataContext.deleteBuddha(buddhaList.get(position).id)
                _CloudUtils.delBuddha(this, buddhaList.get(position)) { code, result ->
                    when (code) {
                        0 -> {
                            runOnUiThread {
                                isChanged = true
                                //
                                refreshData()
                                recordListdAdapter.notifyDataSetChanged()
                                AlertDialog.Builder(this).setMessage(result.toString()).show()
                            }
                        }
                        else -> {
                            e("code : $code , result : $result")
                        }
                    }
                }


            }.setNegativeButton("取消", null).show()
            true
        }
    }

    private fun refreshData() {
        buddhaList = dataContext.getBuddhas(DateTime(start))
        duration = 0L
        count = 0
        buddhaList.forEach {
            duration += it.duration
            count += it.count
        }
        val tap = count.toFloat() / 1080
        tv_info.text = "${DateTime(start).toShortDateString()}   ${DateTime.toSpanString2(duration)}   ${if (count > 0) DecimalFormat("#,##0").format(count) else ""}"
    }

    override fun onBackButtonClickListener() {
        finish()
    }

    protected inner class RecordListdAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return buddhaList.size
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            var convertView = convertView
            val index = position
            try {
                convertView = View.inflate(this@BuddhaDetailActivity, R.layout.inflate_list_item_buddha_child, null)
                val buddha = buddhaList[position]
                val tv_date = convertView.findViewById<TextView>(R.id.tv_att_date)
                val tv_item = convertView.findViewById<TextView>(R.id.tv_item)
                val tv_duration = convertView.findViewById<TextView>(R.id.textView_monthDuration)
                val tv_type = convertView.findViewById<TextView>(R.id.tv_type)
                val tv_number = convertView.findViewById<TextView>(R.id.textView_monthCount)
                tv_date.text = "" + buddha.startTime.hourStr + "点" + buddha.startTime.miniteStr + "分"
//                when(buddha.type){
//                    1->{
//                        tv_item.text = "计时念佛"
//                    }
//                    11->{
//                        tv_item.text="计数念佛"
//                    }
//                }
                if (buddha.summary.isNotEmpty())
                    tv_item.text = buddha.summary
//                else
//                    tv_item.visibility = View.GONE
                tv_duration.text = "" + toSpanString(buddha.duration, 3, 2)
                tv_type.visibility=View.VISIBLE
                tv_type.text = buddha.type.toString()
//                val tap = buddha.count/1080
                tv_number.text = if (buddha.count > 0) DecimalFormat("#,##0").format(buddha.count) else ""
            } catch (e: Exception) {
                printException(this@BuddhaDetailActivity, e)
            }
            return convertView
        }
    }

    companion object {
        @JvmField
        var isChanged = false
    }
}