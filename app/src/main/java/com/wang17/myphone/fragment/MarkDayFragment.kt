package com.wang17.myphone.fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.wang17.myphone.R
import com.wang17.myphone.activity.MarkDayRecordActivity
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.database.DayItem
import com.wang17.myphone.database.MarkDay
import com.wang17.myphone.database.Setting
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.DateTime.Companion.dayOffset
import com.wang17.myphone.model.DateTime.Companion.toSpanString
import com.wang17.myphone.util._CloudUtils.saveSetting
import com.wang17.myphone.util._Session
import com.wang17.myphone.util._Utils.printException
import kotlinx.android.synthetic.main.fragment_mark_day.*
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class MarkDayFragment : Fragment() {
    private lateinit var listItemDatas: MutableList<ListItemData>
    private lateinit var dataContext: DataContext
    private lateinit var listAdapter: DayItemListAdapter
    private var markdayFocusedId: UUID? = null

    internal inner class ListItemData {
        var dayItem: DayItem? = null
        var havePassedInHour = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            dataContext = DataContext(context)
            listItemDatas = ArrayList()
            markdayFocusedId = UUID.fromString(dataContext.getSetting(Setting.KEYS.mark_day_focused, _Session.UUID_NULL).string)
            listAdapter = DayItemListAdapter()
        } catch (e: Exception) {
            Log.e(_TAG, e.message!!)
            throw e
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_mark_day, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        floating_add.setOnClickListener { addDayItemDialog() }
        lv_markday.setAdapter(listAdapter)
        lv_markday.setOnItemClickListener { parent, view, position, id ->
            var dialog = AlertDialog.Builder(context!!).setItems(arrayOf("进入", "编辑", "删除", "主项")) { dialog, which ->
                var listItemData = listItemDatas[position]
                when (which) {
                    0 -> {
                        val intent = Intent(activity, MarkDayRecordActivity::class.java)
                        intent.putExtra("id", listItemData.dayItem!!.id.toString())
                        startActivityForResult(intent, 0)
                    }
                    1 -> editDayItemDialog(listItemData.dayItem)
                    2 -> android.support.v7.app.AlertDialog.Builder(context!!).setMessage("确定要删除此项目及其所有记录吗？").setNegativeButton("确定") { dialog, which ->
                        dataContext.deleteDayItem(listItemData.dayItem!!.id)
                        dataContext.deleteMarkDay(listItemData.dayItem!!.id)
                        listDatas
                        listAdapter!!.notifyDataSetChanged()
                    }.setPositiveButton("取消") { dialog, which -> dialog.dismiss() }.show()
                    3 -> {
                        dataContext.editSetting(Setting.KEYS.mark_day_focused, listItemData.dayItem!!.id)
                        markdayFocusedId = listItemData.dayItem!!.id
                        listAdapter!!.notifyDataSetChanged()
                    }
                    4 -> addDayItemDialog()
                }
            }.create()
//            dialog.window?.attributes?.y=view.top
            dialog.show()
        }
        lv_markday.setOnItemLongClickListener { parent, view, position, id ->

            setDateTimeDialog(DateTime(), listItemDatas[position].dayItem)
            true
        }
    }
    override fun onStart() {
        listDatas
        listAdapter!!.notifyDataSetChanged()
        super.onStart()
    }

    private val listDatas: Unit
        private get() {
            listItemDatas.clear()
            val dayItems = dataContext.getDayItems(true)
            for (i in dayItems.indices) {
                val did: ListItemData = ListItemData()
                did.dayItem = dayItems[i]
                val lastMarkDay = dataContext.getLastMarkDay(did.dayItem!!.id)
                if (lastMarkDay == null) {
                    did.havePassedInHour = 0
                } else {
                    did.havePassedInHour = ((DateTime().timeInMillis - lastMarkDay.dateTime.timeInMillis) / 3600000).toInt()
                    when (did.dayItem!!.summary) {
                        "天" -> did.havePassedInHour = dayOffset(lastMarkDay.dateTime, DateTime()) * 24
                        "当天" -> did.havePassedInHour = (dayOffset(lastMarkDay.dateTime, DateTime()) + 1) * 24
                    }
                }
                listItemDatas.add(did)
            }
        }

    fun editDayItemDialog(dayItem: DayItem?) {
        try {
            val view = View.inflate(context, R.layout.inflate_add_day_item, null)
            val dialog = AlertDialog.Builder(context).setView(view).create()
            dialog.setTitle("编辑项目")
            val editName = view.findViewById<EditText>(R.id.edit_name)
            val editTarget = view.findViewById<EditText>(R.id.edit_targetInDay)
            val editSummary = view.findViewById<EditText>(R.id.edit_summary)
            val buttonAdd = view.findViewById<Button>(R.id.button_add)
            val buttonCancel = view.findViewById<Button>(R.id.button_cancel)
            editName.setText(dayItem!!.name)
            editSummary.setText(dayItem.summary)
            editTarget.setText((dayItem.targetInHour / 24).toString())
            editName.setSelectAllOnFocus(true)
            editSummary.setSelectAllOnFocus(true)
            editTarget.setSelectAllOnFocus(true)
            buttonAdd.text = "编辑"
            buttonAdd.setOnClickListener {
                val name = editName.text.toString()
                val targetInHour = editTarget.text.toString().toInt() * 24
                val summary = editSummary.text.toString()
                dayItem.name = name
                dayItem.targetInHour = targetInHour
                dayItem.summary = summary
                dataContext = DataContext(context)
                dataContext.editDayItem(dayItem)
                listDatas
                listAdapter!!.notifyDataSetChanged()
                dialog.dismiss()
            }
            buttonCancel.setOnClickListener { dialog.dismiss() }
            dialog.show()
        } catch (e: Exception) {
            printException(context, e)
        }
    }

    fun addDayItemDialog() {
        val view = View.inflate(context, R.layout.inflate_add_day_item, null)
        val dialog = AlertDialog.Builder(context).setView(view).create()
        dialog.setTitle("添加项目")
        val editName = view.findViewById<EditText>(R.id.edit_name)
        val editTarget = view.findViewById<EditText>(R.id.edit_targetInDay)
        val editSummary = view.findViewById<EditText>(R.id.edit_summary)
        val buttonAdd = view.findViewById<Button>(R.id.button_add)
        val buttonCancel = view.findViewById<Button>(R.id.button_cancel)
        buttonAdd.setOnClickListener {
            val name = editName.text.toString()
            val targetInHour = editTarget.text.toString().toInt() * 24
            val summary = editSummary.text.toString()
            val item = DayItem(name, summary, targetInHour)
            val dataContext = DataContext(context)
            dataContext.addDayItem(item)
            listDatas
            listAdapter!!.notifyDataSetChanged()
            dialog.dismiss()
        }
        buttonCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    protected inner class DayItemListAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return listItemDatas.size
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            var convertView = convertView
            try {
                convertView = View.inflate(context, R.layout.inflate_list_item_mark_day, null)
                val listItemData = listItemDatas[position]
                val layoutRoot: ConstraintLayout = convertView.findViewById(R.id.layout_root)
                val imageView_mark = convertView.findViewById<ImageView>(R.id.imageView_mark)
                val textView_name = convertView.findViewById<TextView>(R.id.tv_name)
                val textView_interval = convertView.findViewById<TextView>(R.id.textView_interval)
                val textView_remaider = convertView.findViewById<TextView>(R.id.textView_remaider)
                val textView_target = convertView.findViewById<TextView>(R.id.textView_target)
                val textView_targetDate = convertView.findViewById<TextView>(R.id.textView_targetDate)
                val progressBar = convertView.findViewById<ProgressBar>(R.id.progressBar)
                progressBar.max = listItemData.dayItem!!.targetInHour
                textView_name.text = listItemData.dayItem!!.name
                var havePassedStr = toSpanString(listItemData.havePassedInHour.toLong() * 3600000, 4, 3)
                var remainderStr = toSpanString((listItemData.dayItem!!.targetInHour - listItemData.havePassedInHour).toLong() * 3600000, 4, 3)
                val targetStr = toSpanString(listItemData.dayItem!!.targetInHour.toLong() * 3600000, 4, 3)
                var targetDate = DateTime(DateTime().timeInMillis + (listItemData.dayItem!!.targetInHour - listItemData.havePassedInHour).toLong() * 3600000)
                when (listItemData.dayItem!!.summary) {
                    "周" ->{
                        havePassedStr = "${listItemData.havePassedInHour / 24/7}周 ${listItemData.havePassedInHour / 24%7}天"
                        val remainder = listItemData.dayItem!!.targetInHour / 24 - listItemData.havePassedInHour / 24
                        remainderStr = (if (remainder < 0) "+" + remainder * -1 else remainder).toString() + "天"
                        targetDate = DateTime(System.currentTimeMillis() + (listItemData.dayItem!!.targetInHour - listItemData.havePassedInHour).toLong() * 3600000)
                    }
                    "天" -> {
                        havePassedStr = "${listItemData.havePassedInHour / 24}天"
                        val remainder = listItemData.dayItem!!.targetInHour / 24 - listItemData.havePassedInHour / 24
                        remainderStr = (if (remainder < 0) "+" + remainder * -1 else remainder).toString() + "天"
                        targetDate = DateTime(System.currentTimeMillis() + (listItemData.dayItem!!.targetInHour - listItemData.havePassedInHour).toLong() * 3600000)
                    }
                    "当天" -> {
                        havePassedStr = "${listItemData.havePassedInHour / 24}天"
                        var remainder = listItemData.dayItem!!.targetInHour / 24 - listItemData.havePassedInHour / 24
                        remainderStr = (if (remainder < 0) "+" + remainder * -1 else remainder).toString() + "天"
                        targetDate = DateTime(System.currentTimeMillis() + (listItemData.dayItem!!.targetInHour - listItemData.havePassedInHour).toLong() * 3600000)
                    }
                }
                textView_interval.text = havePassedStr
                textView_remaider.text = remainderStr
                textView_target.text = "（ $targetStr ）"
                textView_targetDate.text = targetDate.toShortDateString()
                progressBar.progress = listItemData.havePassedInHour
                if (listItemData.dayItem!!.id == markdayFocusedId) {
                    imageView_mark.visibility = View.VISIBLE
                } else {
                    imageView_mark.visibility = View.INVISIBLE
                }
            } catch (e: Exception) {
                printException(context, e)
            }
            return convertView
        }
    }

    fun setDateTimeDialog(dateTime: DateTime, item: DayItem?) {
        try {
            val view = View.inflate(context, R.layout.inflate_dialog_date_picker, null)
            val dialog = AlertDialog.Builder(context).setView(view).create()
            dialog.setTitle("设定最近一次" + item!!.name + "时间")
            val year = dateTime.year
            val month = dateTime.month
            val maxDay = dateTime.getActualMaximum(Calendar.DAY_OF_MONTH)
            val day = dateTime.day
            val hour = dateTime.hour
            val yearNumbers = arrayOfNulls<String>(3)
            for (i in year - 2..year) {
                yearNumbers[i - year + 2] = i.toString() + "年"
            }
            val monthNumbers = arrayOfNulls<String>(12)
            for (i in 0..11) {
                monthNumbers[i] ="${i + 1 }月"
            }
            val dayNumbers = arrayOfNulls<String>(31)
            for (i in 0..30) {
                dayNumbers[i] =  "${i + 1}日"
            }
            val hourNumbers = arrayOfNulls<String>(24)
            for (i in 0..23) {
                hourNumbers[i] = i.toString() + "点"
            }
            val npYear = view.findViewById<View>(R.id.npYear) as NumberPicker
            val npMonth = view.findViewById<View>(R.id.npMonth) as NumberPicker
            val npDay = view.findViewById<View>(R.id.npDay) as NumberPicker
            val npHour = view.findViewById<View>(R.id.npHour) as NumberPicker
            npYear.minValue = year - 2
            npYear.maxValue = year
            npYear.value = year
            npYear.displayedValues = yearNumbers
            npYear.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
            npMonth.minValue = 1
            npMonth.maxValue = 12
            npMonth.displayedValues = monthNumbers
            npMonth.value = month + 1
            npMonth.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
            npDay.minValue = 1
            npDay.maxValue = 31
            npDay.displayedValues = dayNumbers
            npDay.value = day
            npDay.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
            npHour.minValue = 0
            npHour.maxValue = 23
            npHour.displayedValues = hourNumbers
            npHour.value = hour
            npHour.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定") { dialog, which ->
                try {
                    val y = npYear.value
                    val m = npMonth.value - 1
                    val d = npDay.value
                    val h = npHour.value
                    val selectedDateTime = DateTime(y, m, d, h, 0, 0)
                    val markDay = MarkDay(selectedDateTime, item.id, "")
                    dataContext.addMarkDay(markDay)
                    if (item.name == "戒期") {
                        saveSetting(context!!, dataContext.getSetting(Setting.KEYS.wx_request_code, "0000").string, Setting.KEYS.wx_sex_date.toString(), selectedDateTime.timeInMillis) { code, result ->
                            when (code) {
                                0 -> e(result)
                                1 -> e(result)
                                -1 -> e(result)
                                -2 -> e(result)
                            }
                            Looper.prepare()
                            Toast.makeText(context, "更新完毕", Toast.LENGTH_LONG).show()
                            Looper.loop()
                        }
                    }
                    listDatas
                    listAdapter!!.notifyDataSetChanged()
                    dialog.dismiss()
                } catch (e: Exception) {
                    printException(context, e)
                }
            }
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消") { dialog, which ->
                try {
                    dialog.dismiss()
                } catch (e: Exception) {
                    printException(context, e)
                }
            }
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun e(log: Any) {
        Log.e("wangsc", log.toString())
    }

    override fun startActivityForResult(intent: Intent, requestCode: Int) {
        listDatas
        listAdapter!!.notifyDataSetChanged()
        super.startActivityForResult(intent, requestCode)
    }

    companion object {
        private const val _TAG = "wangsc"
    }
}