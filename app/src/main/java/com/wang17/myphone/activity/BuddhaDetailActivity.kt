package com.wang17.myphone.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import com.wang17.myphone.R
import com.wang17.myphone.activity.BuddhaDetailActivity
import com.wang17.myphone.fragment.ActionBarFragment.OnActionFragmentBackListener
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.DateTime.Companion.toSpanString
import com.wang17.myphone.model.database.BuddhaRecord
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.util.DataContext
import com.wang17.myphone.util._Utils.printException
import kotlinx.android.synthetic.main.activity_buddha_detail.*
import java.text.DecimalFormat

class BuddhaDetailActivity : AppCompatActivity(), OnActionFragmentBackListener {
    private var start: Long = 0
    private lateinit var dataContext: DataContext
    private lateinit var records: List<BuddhaRecord>
    private lateinit var recordListdAdapter: RecordListdAdapter

    var duration=0L
    var count = 0
    var number =0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buddha_detail)
        dataContext = DataContext(this@BuddhaDetailActivity)
        start = intent.getLongExtra("start", 0)
        refreshData()
        recordListdAdapter = RecordListdAdapter()
        listView_records.adapter = recordListdAdapter
        listView_records.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val tallyRecord = records.get(position)
            val hour = (tallyRecord.duration / 1000 / 60 / 60).toInt()
            val min = (tallyRecord.duration / 1000 / 60).toInt()
            showIntervalDialog(hour, min, tallyRecord)
        }
        listView_records.onItemLongClickListener = OnItemLongClickListener { parent, view, position, id ->
            AlertDialog.Builder(this@BuddhaDetailActivity).setMessage("确认要删除当前记录吗？").setPositiveButton("确定") { dialog, which ->
                val tallyRecord = records.get(position)
                dataContext.deleteRecord(tallyRecord.id)
                isChanged = true
                //
                refreshData()
                recordListdAdapter.notifyDataSetChanged()
            }.setNegativeButton("取消", null).show()
            true
        }
    }

    private fun refreshData() {
        records = dataContext.getBuddhas(DateTime(start))
        duration = 0L
        count = 0
        number = 0
        records.forEach {
            duration += it.duration
            count += it.count
            number += it.count * 1080
        }
        tv_info.text="${DateTime(start).toShortDateString()}   ${count}圈   ${DateTime.toSpanString2(duration)}   ${DecimalFormat("#,##0").format(number)}"
    }

    override fun onBackButtonClickListener() {
        finish()
    }

    protected inner class RecordListdAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return records.size
        }

        override fun getItem(position: Int): Any?{
            return null
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View?{
            var convertView = convertView
            val index = position
            try {
                convertView = View.inflate(this@BuddhaDetailActivity, R.layout.inflate_list_item_buddha_child, null)
                val buddha = records[position]
                val tv_date = convertView.findViewById<View>(R.id.textView_date) as TextView
                val tv_item = convertView.findViewById<View>(R.id.textView_item) as TextView
                val tv_duration = convertView.findViewById<View>(R.id.textView_monthDuration) as TextView
                val tv_count = convertView.findViewById<View>(R.id.textView_monthCount) as TextView
                val tv_number = convertView.findViewById<View>(R.id.textView_monthNumber) as TextView
                tv_date.text = "" + buddha.startTime.hourStr + "点" + buddha.startTime.miniteStr + "分"
                tv_item.text = if (buddha.summary == null || buddha.summary.isEmpty()) "默认" else buddha.summary
                tv_duration.text = "" + toSpanString(buddha.duration, 3, 2)
                tv_count.text = buddha.count.toString() + "圈"
                tv_number.text = DecimalFormat("#,##0").format((buddha.count * 1080).toLong())
            } catch (e: Exception) {
                printException(this@BuddhaDetailActivity, e)
            }
            return convertView
        }
    }

    fun showIntervalDialog(xxxHour: Int, xxxMin: Int, tallyRecord: BuddhaRecord) {
        val view = View.inflate(this, R.layout.inflate_dialog_edit_tally_record, null)
        val dialog = AlertDialog.Builder(this).create()
        dialog.setView(view)
        dialog.setTitle("设定念佛时间")
        val hourNumbers = arrayOfNulls<String>(100)
        for (i in 0..99) {
            hourNumbers[i] = i.toString() + "小时"
        }
        val minNumbers = arrayOfNulls<String>(60)
        for (i in 0..59) {
            minNumbers[i] = i.toString() + "分钟"
        }
        val number_hour = view.findViewById<View>(R.id.number_hour) as NumberPicker
        val number_min = view.findViewById<View>(R.id.number_min) as NumberPicker
        val editTextItem = view.findViewById<View>(R.id.editText_item) as EditText
        number_hour.minValue = 0
        number_hour.displayedValues = hourNumbers
        number_hour.maxValue = 99
        number_hour.value = xxxHour
        number_hour.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
        number_min.minValue = 0
        number_min.displayedValues = minNumbers
        number_min.maxValue = 59
        number_min.value = xxxMin
        number_min.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
        editTextItem.setText(if (tallyRecord.summary == null || tallyRecord.summary.isEmpty()) dataContext.getSetting(Setting.KEYS.tally_record_item_text, "").string else tallyRecord.summary)
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定") { dialog, which ->
            try {
                val hour = number_hour.value
                val min = number_min.value
                val interval = hour * 60000 * 60 + min * 60000
                if (interval == 0) {
                    AlertDialog.Builder(this@BuddhaDetailActivity).setMessage("确认要删除当前记录吗？").setPositiveButton("确定") { dialog, which ->
                        dataContext.deleteRecord(tallyRecord.id)
                        isChanged = true
                        //
                        refreshData()
                        recordListdAdapter.notifyDataSetChanged()
                    }.setNegativeButton("取消", null).show()
                } else {
                    tallyRecord.duration = interval.toLong()
                    tallyRecord.summary = editTextItem.text.toString()
                    dataContext.editBuddha(tallyRecord)
                    isChanged = true
                    //
                    refreshData()
                    recordListdAdapter.notifyDataSetChanged()
                }
                dialog.dismiss()
            } catch (e: Exception) {
                printException(this@BuddhaDetailActivity, e)
            }
        }
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消") { dialog, which -> dialog.dismiss() }
        dialog.show()
    }

    companion object {
        @JvmField
        var isChanged = false
    }
}