package com.wang17.myphone.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.wang17.myphone.R
import com.wang17.myphone.dao.DataContext
import com.wang17.myphone.database.PhoneMessage
import com.wang17.myphone.database.Setting
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.util._Utils
import kotlinx.android.synthetic.main.fragment_sms.*

class SmsDialogFragment: DialogFragment() {
    private var smsList: List<PhoneMessage> = ArrayList()
    private var adapter: SmsListdAdapter = SmsListdAdapter()
    private lateinit var dc: DataContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dc = DataContext(context)
        loadSmsData()
    }

    fun loadSmsData() {
        smsList = dc.getPhoneMessages(dc.getSetting(Setting.KEYS.sms_last_time,  DateTime.today.timeInMillis).long)
//        val dateTime = DateTime(dc.getSetting(Setting.KEYS.sms_last_time, DateTime.today.timeInMillis).long)
//        smsList =  SmsHelper.getSMS(context, SmsType.接收到,dateTime)
        dc.editSetting(Setting.KEYS.sms_last_time, System.currentTimeMillis())
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sms,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lv_sms?.setAdapter(adapter)
        lv_sms?.setOnItemLongClickListener { parent, view, position, id ->
            AlertDialog.Builder(context!!).setMessage("确认删除?").setPositiveButton("删除", DialogInterface.OnClickListener { dialog, which ->
                val ss = smsList[position]
                dc.deletePhoneMessage(ss.id)
                loadSmsData()
                adapter.notifyDataSetChanged()
            }).show()
            true
        }
    }

    private var preDay = 0
    private val mHashMap = HashMap<Int, View?>()
    protected inner class SmsListdAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return smsList!!.size
        }

        override fun getItem(position: Int): Any? {
            return smsList!!.get(position)
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            var convertView = convertView
            try {
                convertView = View.inflate(context, R.layout.inflate_list_item_sms, null)
                if (mHashMap[position] == null) {
                    mHashMap[position] = convertView
                    val sms = smsList!![position]
                    val layoutDate = convertView.findViewById<LinearLayout>(R.id.layout_date)
                    val textViewDate = convertView.findViewById<TextView>(R.id.tv_att_date)
                    val textViewDateTime = convertView.findViewById<TextView>(R.id.tv_dateTime)
                    val textViewBody = convertView.findViewById<TextView>(R.id.tv_body)
                    val textViewNumber = convertView.findViewById<TextView>(R.id.tv_number)
                    if (sms.createTime.day != preDay) {

                        val today = DateTime.today
                        val yestoday = today.addDays(-1)
                        if (sms.createTime.timeInMillis < yestoday.timeInMillis) {
                            textViewDate.text = sms.createTime.toShortDateString1()
                        } else if (sms.createTime.timeInMillis < today.timeInMillis) {
                            textViewDate.text = "昨天"
                        } else if (sms.createTime.timeInMillis >= today.timeInMillis) {
                            textViewDate.text = "今天"
                        }

                        layoutDate.visibility = View.VISIBLE
                        preDay = sms.createTime.day
                    } else {
                        layoutDate.visibility = View.GONE
                    }
                    textViewDateTime.text = sms.createTime.toLongDateTimeString()
                    textViewBody.text = sms.body
                    textViewNumber.text = sms.address
//                    val matcher = Pattern.compile("-?\\d+\\.\\d+").matcher(sms.body)
//                    if (matcher.find()) {
//                        val money = matcher.group().toDouble()
//                        if (Math.abs(money) > 100) {
//                            textViewBody.setTextColor(Color.BLACK)
//                        }
//                    }
                } else {
                    convertView = mHashMap[position]!!
                }

            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
            return convertView
        }
    }

}