package com.wang17.myphone.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView.OnGroupExpandListener
import android.widget.ImageView
import android.widget.TextView
import com.wang17.myphone.R
import com.wang17.myphone.dao.DataContext
import com.wang17.myphone.format
import com.wang17.myphone.fragment.ActionBarFragment.OnActionFragmentBackListener
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.DateTime.Companion.toSpanString2
import com.wang17.myphone.model.DateTime.Companion.today
import com.wang17.myphone.util._String
import com.wang17.myphone.util._Utils.printException
import kotlinx.android.synthetic.main.activity_buddha.*
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class BuddhaActivity : AppCompatActivity(), OnActionFragmentBackListener {

    // 类变量
    private lateinit var dataContext: DataContext
    private lateinit var recordListdAdapter: BaseExpandableListAdapter

    // 值变量
    private lateinit var groupList: MutableList<GroupInfo>
    private lateinit var childListList: MutableList<List<ChildInfo>>
    var year = 0
    var duration: Long = 0
    var count = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_buddha)
            dataContext = DataContext(this)
            //            tallyRecords = dataContext.getRecords(DateTime.getToday().getYear());
            year = today.year
            val iv_prev = findViewById<ImageView>(R.id.iv_prev)
            val iv_next = findViewById<ImageView>(R.id.iv_next)
            iv_prev.setOnClickListener {
                year--
                reflushData(year)
                recordListdAdapter.notifyDataSetChanged()
            }
            iv_next.setOnClickListener {
                year++
                reflushData(year)
                recordListdAdapter.notifyDataSetChanged()
            }
            reflushData(year)
            listView_records.setGroupIndicator(null)
            recordListdAdapter = BuddhaListAdapter()
            listView_records.setAdapter(recordListdAdapter)
            // 默认展开第一组
            listView_records.expandGroup(0)
            // 单组展开，不能多组同时展开。
            listView_records.setOnGroupExpandListener(object : OnGroupExpandListener {
                override fun onGroupExpand(groupPosition: Int) {
                    run {
                        var i = 0
                        val count = listView_records.expandableListAdapter.groupCount
                        while (i < count) {
                            if (groupPosition != i) { // 关闭其他分组
                                listView_records.collapseGroup(i)
                            }
                            i++
                        }
                    }
                }
            })
            listView_records.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
                val start = childListList[groupPosition][childPosition].start
                val intent = Intent(this@BuddhaActivity, BuddhaDetailActivity::class.java)
                intent.putExtra("start", start)
                startActivityForResult(intent, TO_TALLAY_RECORD_DETAIL_ACTIVITY)
                true
            }
        } catch (e: Exception) {
            printException(this, e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == TO_TALLAY_RECORD_DETAIL_ACTIVITY) {
            if (BuddhaDetailActivity.isChanged) {
//                tallyRecords = dataContext.getRecords(DateTime.getToday().getYear());
                reflushData(year)
                recordListdAdapter.notifyDataSetChanged()
                isChanged = true
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun reflushData(year: Int) {
        try {
            count = 0
            duration = 0L
            groupList = ArrayList()
            childListList = ArrayList()
            var month = -1
            var day = -1
            val isToday = true
            var groupNode = GroupInfo()
            var childNode = ChildInfo()
            var childList: MutableList<ChildInfo> = ArrayList()
            val first = dataContext.firstBuddha
            val latest = dataContext.latestBuddha
            if (first == null || latest == null) {
                return
            }
            var end = DateTime(year, 0, 1)
            var date = DateTime(year, 11, 31)
            if (end.timeInMillis < first.startTime.timeInMillis) {
                end = first.startTime
            }
            if (date.timeInMillis > latest.startTime.timeInMillis) {
                date = latest.startTime
            }
            Log.e("wangsc", "start : " + end.toLongDateTimeString() + " end : " + date.toLongDateTimeString())
            while (date.year == end.year && date[Calendar.DAY_OF_YEAR] >= end[Calendar.DAY_OF_YEAR]) {
                val records = dataContext.getBuddhas(date)
                //                isToday=false;
                if (date.month == month) {
                    day = date.day
                    childNode = ChildInfo()
                    childNode.day = day
                    childNode.start = date.timeInMillis
                    var no=0
                    for (record in records) {
                        groupNode.duration += record.duration
                        groupNode.count += record.count
                        childNode.duration += record.duration
                        childNode.count += record.count
                        if (record.summary.isNotEmpty()) {
                            no++
                            childNode.item += "${if(no>1) "\n" else ""}${record.summary}"
                        }
                        duration += record.duration
                        count += record.count
                    }
                    childList.add(childNode)
                } else {
                    month = date.month
                    groupNode = GroupInfo()
                    groupNode.month = month + 1

                    //
                    childList = ArrayList()
                    day = date.day
                    childNode = ChildInfo()
                    childNode.day = day
                    childNode.start = date.timeInMillis
                    var no=0
                    for (record in records) {
                        groupNode.duration += record.duration
                        groupNode.count += record.count
                        childNode.duration += record.duration
                        childNode.count += record.count
                        if (record.summary.isNotEmpty()) {
                            no++
                            childNode.item += "${if(no>1) "\n" else ""}${record.summary}"
                        }
                        duration += record.duration
                        count += record.count
                    }
                    childList.add(childNode)
                    groupList.add(groupNode)
                    childListList.add(childList)
                }
                date = date.addDays(-1)
            }
            tv_year.text = "${year}年   ${duration / 60000 / 60}小时"
//            tv_year.text = "${year}年   ${duration / 60000 / 60}小时   ${DecimalFormat("0.00").format(count.toDouble() / 10000)}万"
        } catch (e: Exception) {
            printException(this@BuddhaActivity, e)
        }
    }

    internal inner class BuddhaListAdapter : BaseExpandableListAdapter() {
        override fun getGroupCount(): Int {
            return groupList.size
        }

        override fun getGroup(groupPosition: Int): Any {
            return groupList[groupPosition]
        }


        override fun getChildrenCount(groupPosition: Int): Int {
            return childListList[groupPosition].size
        }

        override fun getChild(groupPosition: Int, childPosition: Int): Any {
            return childListList[groupPosition][childPosition]
        }

        override fun getGroupId(groupPosition: Int): Long {
            return groupPosition.toLong()
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long {
            return childPosition.toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View? {
            var convertView = convertView
            try {
                convertView = View.inflate(this@BuddhaActivity, R.layout.inflate_list_item_buddha_group, null)
                val tvDate = convertView.findViewById<TextView>(R.id.tv_att_date)
                val tvDuration = convertView.findViewById<TextView>(R.id.textView_monthDuration)
                val tvTap = convertView.findViewById<TextView>(R.id.textView_monthTap)
                val tvCount = convertView.findViewById<TextView>(R.id.textView_monthCount)
                val groupInfo = groupList[groupPosition]
                tvDate.text = _String.format(groupInfo.month) + "月"
                tvDuration.text = (groupInfo.duration / 60000 / 60).toString() + "小时"
//                textView_tap.text = (DecimalFormat("0").format(info.count.toFloat()/1080)) + "圈"
                tvCount.text = DecimalFormat("0.00").format(groupInfo.count.toDouble() / 10000) + " 万"
                if (groupInfo.duration == 0L) {
                    tvDuration.text = "--"
                }
                if (groupInfo.count == 0) {
                    tvCount.text = ""
                }
                if (groupInfo.count == 0) {
                    tvTap.text = ""
                }
            } catch (e: Exception) {
                printException(this@BuddhaActivity, e)
            }
            return convertView
        }

        override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View? {
            var convertView = convertView
            try {
                convertView = View.inflate(this@BuddhaActivity, R.layout.inflate_list_item_buddha_child, null)
                val tvDate = convertView.findViewById<TextView>(R.id.tv_att_date)
                val tvDuration = convertView.findViewById<TextView>(R.id.textView_monthDuration)
                val tvTap = convertView.findViewById<TextView>(R.id.textView_monthTap)
                val tvCount = convertView.findViewById<TextView>(R.id.textView_monthCount)
                val tvItem = convertView.findViewById<TextView>(R.id.tv_item)
//                val root = convertView.findViewById<ConstraintLayout>(R.id.layout_root)
                val childInfo = childListList[groupPosition][childPosition]
                if (childInfo.item.isNotEmpty())
                    tvItem.text = childInfo.item
                else
                    tvItem.visibility = View.GONE
                tvDate.text = _String.format(childInfo.day)
//                textView_tap.text =  (DecimalFormat("0").format(childInfo.count.toFloat()/1080))+ "圈"
                tvDuration.text = toSpanString2(childInfo.duration)
                tvCount.text = (childInfo.count.toDouble() / 1000).format(2) + " 千"
                if (childInfo.duration == 0L) {
                    tvDuration.text = "--"
                }
                if (childInfo.count == 0) {
                    tvCount.text = ""
                }
                if (childInfo.count == 0) {
                    tvTap.text = ""
                }
//                if(childInfo.count>=15000){
//                    tvCount.setTextColor(Color.RED)
//                    tvDuration.setTextColor(Color.RED)
////                    root.setBackgroundColor(Color.RED)
//                }else
                    if(childInfo.count<10000){
                    tvCount.setTextColor(Color.BLUE)
                    tvDuration.setTextColor(Color.BLUE)
//                    root.setBackgroundColor(Color.BLUE)
                }
            } catch (e: Exception) {
                printException(this@BuddhaActivity, e)
            }
            return convertView
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
            return true
        }
    }

    internal inner class GroupInfo {
        var month = 0
        var duration: Long = 0
        var count = 0
    }

    internal inner class ChildInfo {
        var day = 0
        var start: Long = 0
        var duration: Long = 0
        var count = 0
        var item = ""
    }

    override fun onBackButtonClickListener() {
        finish()
    }

    companion object {
        private const val TO_TALLAY_RECORD_DETAIL_ACTIVITY = 54
        var isChanged = false
    }
}