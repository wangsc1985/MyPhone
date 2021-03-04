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
import com.wang17.myphone.e
import com.wang17.myphone.fragment.ActionBarFragment.OnActionFragmentBackListener
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.DateTime.Companion.toSpanString
import com.wang17.myphone.model.database.BuddhaRecord
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.util.DataContext
import com.wang17.myphone.util._CloudUtils
import com.wang17.myphone.util._Utils.printException
import kotlinx.android.synthetic.main.activity_buddha_detail.*
import java.text.DecimalFormat

class BuddhaDetailActivity : AppCompatActivity(), OnActionFragmentBackListener {
    private var start: Long = 0
    private lateinit var dataContext: DataContext
    private lateinit var buddhaList: List<BuddhaRecord>
    private lateinit var recordListdAdapter: RecordListdAdapter

    var duration=0L
    var tap = 0
    var number =0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buddha_detail)
        dataContext = DataContext(this@BuddhaDetailActivity)
        start = intent.getLongExtra("start", 0)
        refreshData()
        recordListdAdapter = RecordListdAdapter()
        listView_records.adapter = recordListdAdapter
        listView_records.onItemLongClickListener = OnItemLongClickListener { parent, view, position, id ->
            AlertDialog.Builder(this@BuddhaDetailActivity).setMessage("确认要删除当前记录吗？").setPositiveButton("确定") { dialog, which ->

                dataContext.deleteBuddha(buddhaList.get(position).id)

                _CloudUtils.delBuddha(this, buddhaList.get(position)) { code, result ->
                    when (code) {
                        0 -> {
                            runOnUiThread {
                                AlertDialog.Builder(this).setMessage(result.toString()).show()
                                isChanged = true
                                //
                                refreshData()
                                recordListdAdapter.notifyDataSetChanged()
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
        tap = 0
        number = 0
        buddhaList.forEach {
            duration += it.duration
            tap += it.tap
            number += it.tap * it.tapCount
        }
        tv_info.text="${DateTime(start).toShortDateString()}   ${tap}圈   ${DateTime.toSpanString2(duration)}   ${DecimalFormat("#,##0").format(number)}"
    }

    override fun onBackButtonClickListener() {
        finish()
    }

    protected inner class RecordListdAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return buddhaList.size
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
                val buddha = buddhaList[position]
                val tv_date = convertView.findViewById<View>(R.id.textView_date) as TextView
                val tv_item = convertView.findViewById<View>(R.id.textView_item) as TextView
                val tv_duration = convertView.findViewById<View>(R.id.textView_monthDuration) as TextView
                val tv_tap = convertView.findViewById<View>(R.id.textView_monthTap) as TextView
                val tv_number = convertView.findViewById<View>(R.id.textView_monthNumber) as TextView
                tv_date.text = "" + buddha.startTime.hourStr + "点" + buddha.startTime.miniteStr + "分"
                tv_item.text = if (buddha.summary == null || buddha.summary.isEmpty()) "默认" else buddha.summary
                tv_duration.text = "" + toSpanString(buddha.duration, 3, 2)
                tv_tap.text = buddha.tap.toString() + "圈"
                tv_number.text = DecimalFormat("#,##0").format((buddha.tap * buddha.tapCount).toLong())
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