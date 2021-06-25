package com.wang17.myphone.activity

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.wang17.myphone.R
import com.wang17.myphone.dao.DataContext
import com.wang17.myphone.database.PhoneMessage
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.util._Utils.printException
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
        var id: UUID,
        var number: String,
        var dateTime: DateTime,
        var body: String
    )

    private var groupList: MutableList<GroupItem> = ArrayList()
    private var childListList: MutableList<MutableList<ChildItem>> = ArrayList()
    private var smsList: List<PhoneMessage> = ArrayList()
    private var expandAdapter: SmsExpandableListAdapter = SmsExpandableListAdapter()
    private lateinit var dc: DataContext

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms)

        try {
            dc = DataContext(this)
            loadSmsData()

            elv_sms.setGroupIndicator(null)
            elv_sms.setAdapter(expandAdapter)

            elv_sms.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
                AlertDialog.Builder(this).setMessage("确认删除?").setPositiveButton("删除", DialogInterface.OnClickListener { dialog, which ->
                    val ss = childListList[groupPosition][childPosition]
                    dc.deletePhoneMessage(ss.id)
                    loadSmsData()
                    expandAdapter.notifyDataSetChanged()
                }).show()
                true
            }



            fab_clean.setOnClickListener {
                val removeList = smsList.filter { m ->
                    !m.body.contains("银行") && !m.body.contains("信用卡") && !m.body.contains("金额") && !m.body.contains("余额") && !m.body.contains("账号") && !m.body.contains("账户")
                            && !m.body.contains("转账") && !m.body.contains("转出") && !m.body.contains("转入") && !m.body.contains("账单")
                            && !m.body.contains("还款") && !m.body.contains("借款") && !m.body.contains("贷款")
                            && !m.body.contains("快递") && !m.body.contains("物流")
                            || m.body.contains("验证码") || m.body.contains("动态密码")
                }
                dc.deletePhoneMessage(removeList)
                loadSmsData()
                expandAdapter.notifyDataSetChanged()
                AlertDialog.Builder(this).setMessage("整理完毕！").show()
            }
            fab_clean.setOnLongClickListener {
                dc.deletePhoneMessage()
                loadSmsData()
                expandAdapter.notifyDataSetChanged()
                AlertDialog.Builder(this).setMessage("清空完毕！").show()

                true
            }

            fab_clean_history.setOnClickListener {
                dc.deletePhoneMessageFrom(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 20L)
                loadSmsData()
                expandAdapter.notifyDataSetChanged()
                AlertDialog.Builder(this).setMessage("删除完毕！").show()
            }
            fab_express.setOnClickListener {
                val removeList = smsList.filter { m ->
                    m.body.contains("快递")
                }
                dc.deletePhoneMessage(removeList)
                loadSmsData()
                expandAdapter.notifyDataSetChanged()
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
                childList.add(ChildItem(sms.id, sms.address, sms.createTime, sms.body))
                childListList.add(childList)
            } else {
//                if ( sms.createTime.timeInMillis>group.dateTime.timeInMillis) {
//                    group.dateTime = sms.createTime
//                    group.body = sms.body
//                }
                findName(sms.body)?.let {
                    group.name = it
                }
                childListList.get(index).add(ChildItem(sms.id, sms.address, sms.createTime, sms.body))
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

}