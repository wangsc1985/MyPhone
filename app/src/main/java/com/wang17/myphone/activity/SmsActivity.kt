package com.wang17.myphone.activity

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.wang17.myphone.R
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.database.PhoneMessage
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.util._Utils.printException
import com.wang17.myphone.widget.MyWidgetProvider
import kotlinx.android.synthetic.main.activity_sms.*
import kotlinx.android.synthetic.main.widget_timer.*
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class SmsActivity : AppCompatActivity() {
    class GroupItem(
            var dateTime: DateTime,
            var number: String,
            var name: String,
            var body: String
    )

    class ChildItem(
            var id:UUID,
            var number: String,
            var dateTime: DateTime,
            var body: String
    )

    private var groupList: MutableList<GroupItem> = ArrayList()
    private var childListList: MutableList<MutableList<ChildItem>> = ArrayList()
    private var smsList: List<PhoneMessage> = ArrayList()
    private var adapter: SmsExpandableListAdapter = SmsExpandableListAdapter()
    private lateinit var dc: DataContext
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms)

        // 添加异常捕捉
        try {
//            val cursor = contentResolver.query(Uri.parse("content://sms"), arrayOf("_id", "address", "read", "body", "date"), null, null, "date desc")
//            if (cursor != null) {
//                var body = ""
//                while (cursor.moveToNext()) {
//                    body = cursor.getString(cursor.getColumnIndex("body")) // 在这里获取短信信息
//                    val smsdate = cursor.getString(cursor.getColumnIndex("date")).toLong()
//                    e(body)
//                }
//            }
            dc = DataContext(this)
            loadSmsData()
            listView_sms.setGroupIndicator(null)
            listView_sms.setAdapter(adapter)
            listView_sms.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
                AlertDialog.Builder(this).setMessage("确认删除?").setPositiveButton("删除", DialogInterface.OnClickListener { dialog, which ->
                    val ss = childListList[groupPosition][childPosition]
                    dc.deletePhoneMessage(ss.id)
                    loadSmsData()
                    adapter.notifyDataSetChanged()
                }).show()
                true
            }

            fab_clean.setOnClickListener {
                val removeList = smsList.filter {
                    m-> !m.body.contains("银行")&&!m.body.contains("余额")
                        &&!m.body.contains("金额")&&!m.body.contains("还款")
                        &&!m.body.contains("借款")&&!m.body.contains("贷款")
                        &&!m.body.contains("转账")&&!m.body.contains("快递")
                }
                dc.deletePhoneMessage(removeList)
                loadSmsData()
                adapter.notifyDataSetChanged()
            }
            fab_clean.setOnLongClickListener {
                var smsMap:MutableMap<String,PhoneMessage> = HashMap()
                smsList.forEach {smsInList->
                    val smsInMap = smsMap.get(smsInList.address)
                    if(smsInMap==null){
                        smsMap.put(smsInList.address,smsInList)
                    }else{
                        if(smsInMap.createTime.timeInMillis-smsInList.createTime.timeInMillis<2000){
                            smsInMap.createTime = smsInList.createTime
                            smsInMap.body = smsInList.body+smsInMap.body
                            dc.editPhoneMessage(smsInMap)
                            dc.deletePhoneMessage(smsInList.id)
                        }else{
                            smsMap[smsInList.address] = smsInList
                        }
                    }
                }
                loadSmsData()
                adapter.notifyDataSetChanged()
                AlertDialog.Builder(this).setMessage("整理完毕！").show()

                true
            }

            fab_clean_history.setOnClickListener {
                dc.deletePhoneMessageFrom(System.currentTimeMillis()-1000*60*60*24*20L)
                loadSmsData()
                adapter.notifyDataSetChanged()
                AlertDialog.Builder(this).setMessage("删除完毕！").show()
            }
            fab_express.setOnClickListener {
                val removeList = smsList.filter {
                    m-> m.body.contains("快递")
                }
                dc.deletePhoneMessage(removeList)
                loadSmsData()
                adapter.notifyDataSetChanged()
                AlertDialog.Builder(this).setMessage("删除完毕！").show()
            }
            //            }
        } catch (ex: Exception) {
            printException(this, ex)
            Log.e("wangsc", ex.message!!)
        }
    }

    fun findName(body: String): String? {
        var matcher = Pattern.compile("(?<=【).{1,10}(?=】)").matcher(body)
        if (matcher.find())
            return matcher.group().trim()

        matcher = Pattern.compile("(?<=\\[).{1,10}(?=\\])").matcher(body)
        if (matcher.find())
            return matcher.group().trim()

        return null
    }

    fun loadSmsData() {
        groupList.clear()
        childListList.clear()
        smsList = dc.phoneMessages
        smsList.forEach { sms ->
            val group = groupList.firstOrNull { m -> m.number == sms.address }
            var index = groupList.indexOfFirst { m -> m.number == sms.address }
            if (group == null) {
                groupList.add(GroupItem(sms.createTime, sms.address, findName(sms.body) ?: sms.address, sms.body))
                var childList = ArrayList<ChildItem>()
                childList.add(ChildItem(sms.id,sms.address,sms.createTime, sms.body))
                childListList.add(childList)
            } else {
                if ( sms.createTime.timeInMillis>group.dateTime.timeInMillis) {
                    group.dateTime = sms.createTime
                    group.body = sms.body
                    findName(sms.body)?.let {
                        group.name=it
                    }
                }
                childListList.get(index).add(ChildItem(sms.id,sms.address,sms.createTime, sms.body))
            }
        }
    }

    private fun e(log: Any) {
        Log.e("wangsc", log.toString())
    }

    internal inner class SmsModel(var number: String, var body: String, var smsDate: DateTime)

    private var preDay = 0
    private val mHashMap = HashMap<Int, View?>()

    protected inner class SmsExpandableListAdapter : BaseExpandableListAdapter() {
        override fun getGroupCount(): Int {
            return groupList.size
        }

        override fun getChildrenCount(groupPosition: Int): Int {
            return childListList[groupPosition].size
        }

        override fun getGroup(groupPosition: Int): Any {
            return groupList.get(groupPosition)
        }

        override fun getChild(groupPosition: Int, childPosition: Int): Any {
            return childListList.get(groupPosition).get(childPosition)
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

        override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View? {
            var view = convertView
            try {
                view = View.inflate(this@SmsActivity, R.layout.inflate_list_sms_group, null)
                val tvDateTime = view.findViewById<TextView>(R.id.tv_dateTime)
                val tvNumber = view.findViewById<TextView>(R.id.tv_number)
                val tvBody = view.findViewById<TextView>(R.id.tv_body)
                val groupItem = groupList.get(groupPosition)

                val today = DateTime.today
                val yestoday = today.addDays(-1)

                if (groupItem.dateTime.timeInMillis < yestoday.timeInMillis) {
                    tvDateTime.text = groupItem.dateTime.toShortDateString1()
                } else if (groupItem.dateTime.timeInMillis < today.timeInMillis) {
                    tvDateTime.text = "昨天"
                } else if (groupItem.dateTime.timeInMillis >= today.timeInMillis) {
                    tvDateTime.text = groupItem.dateTime.toShortTimeString()
                }
                tvNumber.text = groupItem.name
                tvBody.text = groupItem.body
            } catch (ex: Exception) {
            }
            return view
        }

        override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View? {
            var view = convertView
            try {
                view = View.inflate(this@SmsActivity, R.layout.inflate_list_sms_child, null)
                val tvDateTime = view.findViewById<TextView>(R.id.tv_dateTime)
                val tvBody = view.findViewById<TextView>(R.id.tv_body)
                val item = childListList.get(groupPosition).get(childPosition)

                val today = DateTime.today
                val yestoday = today.addDays(-1)

                if (item.dateTime.timeInMillis < yestoday.timeInMillis) {
                    tvDateTime.text = item.dateTime.toShortDateString1()
                } else if (item.dateTime.timeInMillis < today.timeInMillis) {
                    tvDateTime.text = "昨天"
                } else if (item.dateTime.timeInMillis >= today.timeInMillis) {
                    tvDateTime.text = item.dateTime.toShortTimeString()
                }
                tvBody.text = item.body
            } catch (ex: Exception) {
            }
            return view
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
            return true
        }

    }

    protected inner class SmsListdAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return smsList!!.size
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
                convertView = View.inflate(this@SmsActivity, R.layout.inflate_list_item_sms, null)
                if (mHashMap[position] == null) {
                    mHashMap[position] = convertView
                    val sms = smsList!![position]
                    val layoutDate = convertView.findViewById<LinearLayout>(R.id.layout_date)
                    val root = convertView.findViewById<LinearLayout>(R.id.linear_root)
                    val textViewDate = convertView.findViewById<TextView>(R.id.tv_att_date)
                    val textViewDateTime = convertView.findViewById<TextView>(R.id.tv_dateTime)
                    val textViewBody = convertView.findViewById<TextView>(R.id.tv_body)
                    val textViewNumber = convertView.findViewById<TextView>(R.id.tv_number)
                    root.setOnLongClickListener {
                        AlertDialog.Builder(this@SmsActivity).setMessage("要删除所有短信信息吗？").setPositiveButton("是") { dialog, which ->
                            val dataContext = DataContext(this@SmsActivity)
                            dataContext.deletePhoneMessage()
                            smsList = dataContext.phoneMessages
                            adapter!!.notifyDataSetChanged()
                        }.show()
                        true
                    }
                    if (sms.createTime.day != preDay) {
                        textViewDate.text = sms.createTime.toShortDateString()
                        layoutDate.visibility = View.VISIBLE
                        preDay = sms.createTime.day
                    } else {
                        layoutDate.visibility = View.GONE
                    }
                    textViewDateTime.text = sms.createTime.toLongDateTimeString()
                    textViewBody.text = sms.body
                    textViewNumber.text = sms.address
                    val matcher = Pattern.compile("-?\\d+\\.\\d+").matcher(sms.body)
                    if (matcher.find()) {
                        val money = matcher.group().toDouble()
                        if (Math.abs(money) > 100) {
                            textViewBody.setTextColor(Color.BLACK)
                        }
                    }
                } else {
                    convertView = mHashMap[position]!!
                }
            } catch (e: Exception) {
                printException(this@SmsActivity, e)
            }
            return convertView
        }
    }

    private fun delTodoDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_del_todo, null)
        AlertDialog.Builder(this)
                .setView(view).setCancelable(false).setPositiveButton("OK") { dialog, which ->
                    try {
                        val editTextTitle = view.findViewById<EditText>(R.id.editText_title)
                        val editTextSummary = view.findViewById<EditText>(R.id.editText_summary)
                        val bankName = editTextTitle.text.toString()
                        val cardNumber = editTextSummary.text.toString()
                        val dataContext = DataContext(this@SmsActivity)
                        dataContext.deleteBankToDo(bankName, cardNumber)

                        // 发送更新广播
                        val intent = Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW)
                        sendBroadcast(intent)
                    } catch (e: Exception) {
                        printException(this@SmsActivity, e)
                    }
                }.setNegativeButton("CANCEL", null).create().show()
    }
}