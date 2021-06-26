package com.wang17.myphone.activity

import android.app.AlertDialog
import android.content.DialogInterface
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
import com.wang17.myphone.database.BuddhaType
import com.wang17.myphone.dao.DataContext
import com.wang17.myphone.eventbus.EventBusMessage
import com.wang17.myphone.eventbus.FromTotalCount
import com.wang17.myphone.format
import com.wang17.myphone.util._CloudUtils
import com.wang17.myphone.util._Utils
import com.wang17.myphone.util._Utils.printException
import kotlinx.android.synthetic.main.activity_buddha_detail.*
import kotlinx.android.synthetic.main.widget_timer.*
import org.greenrobot.eventbus.EventBus
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
                val spCount = view.findViewById<Spinner>(R.id.sp_count)
                val edit = view.findViewById<EditText>(R.id.et_content)
                edit.setText(buddha.summary)
                val aa = BuddhaType.values()
                val bb:MutableList<String> = ArrayList()
                aa.forEach {
                    bb.add(it.toString())
                }

                fillSpinner(spType,bb)
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
                    13->{
                        spType.setSelection(4)
                    }
                }
                spCount.setSelection(buddha.count/1080-1)
                val tapDuration = buddha.duration/(buddha.count/1080)
                AlertDialog.Builder(this).setView(view).setPositiveButton("修改") { dialog, which ->
                    val type = BuddhaType.valueOf(spType.selectedItem.toString()).toInt()
                    if(edit.text.toString()!=buddha.summary||type!=buddha.type||spCount.selectedItem.toString().toInt()!=buddha.count/1080){
                        if(edit.text.toString()!=buddha.summary){
                            buddha.summary = edit.text.toString()
                        }
                        if(type!=buddha.type){
                            buddha.type = type
                            if(buddha.type == BuddhaType.计时念佛.toInt()){
                                buddha.count=0
                            }
                        }
                        if(spCount.selectedItem.toString().toInt()!=buddha.count/1080){
                            buddha.count = spCount.selectedItem.toString().toInt()*1080
                            buddha.duration = buddha.count/1080*tapDuration
                        }
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
                                Toast.makeText(this,result.toString(),Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }.setNeutralButton("删除"){ dialog: DialogInterface, which: Int ->
                    dataContext.deleteBuddha(buddhaList.get(position).id)
                    _CloudUtils.delBuddha(this, buddhaList.get(position)) { code, result ->
                        when (code) {
                            0 -> {
                                runOnUiThread {
                                    isChanged = true
                                    //
                                    refreshData()
                                    recordListdAdapter.notifyDataSetChanged()
                                    Toast.makeText(this,result.toString(),Toast.LENGTH_LONG).show()
//                                AlertDialog.Builder(this).setMessage(result.toString()).show()
                                }
                            }
                            else -> {
                                e("code : $code , result : $result")
                            }
                        }
                    }
                }.show()
            } catch (e: Exception) {
                _Utils.printException(this,e)
            }
        }
        listView_records.onItemLongClickListener = OnItemLongClickListener { parent, view, position, id ->
            val buddha = buddhaList.get(position)
            if(buddha.type!=BuddhaType.计数念佛.toInt()){
                buddha.type = BuddhaType.计数念佛.toInt()
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
                        Toast.makeText(this,result.toString(),Toast.LENGTH_LONG).show()
                    }
                }
            }
            true
        }
    }

    fun fillSpinner(spinner: Spinner, values: MutableList<String>) {
        val adapter = ArrayAdapter(this, R.layout.inflate_spinner, values)
        adapter.setDropDownViewResource(R.layout.inflate_spinner_dropdown)
        spinner.adapter = adapter
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

        EventBus.getDefault().post(EventBusMessage.getInstance(FromTotalCount(),tap.toString()))
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
                val finger = convertView.findViewById<ImageView>(R.id.iv_finger)
                val tv_date = convertView.findViewById<TextView>(R.id.tv_att_date)
                val tv_item = convertView.findViewById<TextView>(R.id.tv_item)
                val tv_duration = convertView.findViewById<TextView>(R.id.textView_monthDuration)
                val tv_type = convertView.findViewById<TextView>(R.id.tv_type)
                val tv_number = convertView.findViewById<TextView>(R.id.textView_monthCount)
                tv_date.text = "" + buddha.startTime.hourStr + "点" + buddha.startTime.miniteStr + "分"
                if (buddha.summary.isNotEmpty())
                    tv_item.text = buddha.summary
                if(buddha.type==BuddhaType.计数念佛.toInt()){
                    finger.visibility = View.VISIBLE
                }else{
                    finger.visibility = View.GONE
                }
                tv_duration.text = "" + toSpanString(buddha.duration, 3, 2)
                tv_type.visibility=View.VISIBLE
                tv_type.text=BuddhaType.fromInt(buddha.type).toString()
                tv_number.text = if (buddha.count > 0) (buddha.count.toDouble()/1000).format(2)+" 千" else ""
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