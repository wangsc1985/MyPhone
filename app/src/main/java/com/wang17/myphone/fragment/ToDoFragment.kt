package com.wang17.myphone.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.wang17.myphone.R
import com.wang17.myphone.activity.SmsActivity
import com.wang17.myphone.activity.ToDoActivity
import com.wang17.myphone.database.BankToDo
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.util._DialogUtils
import com.wang17.myphone.util._Utils
import com.wang17.myphone.widget.MyWidgetProvider
import kotlinx.android.synthetic.main.fragment_to_do.*
import java.text.DecimalFormat
import java.util.*
import kotlin.Comparator

class ToDoFragment : Fragment() {
    private lateinit var dc: DataContext
    private var mBankToDoList: MutableList<BankToDo> = ArrayList()
    private var listAdapter: TodoListAdapter = TodoListAdapter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //region activity启用这段代码
        dc = DataContext(context!!)
        getBankTodos()
        listView_todo.setAdapter(listAdapter)
        val actionButton: FloatingActionButton = view.findViewById(R.id.fab_add)
        actionButton.setOnClickListener { //  3/1 001 新建待办事项
            _DialogUtils.addTodoDialog(context!!) {
                getBankTodos()
                listAdapter.notifyDataSetChanged()
            }
        }
        actionButton.setOnLongClickListener {
            startActivity(Intent(context!!, SmsActivity::class.java))
            true
        }
        //endregion
    }

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?,  savedInstanceState: Bundle? ): View? {
//        listView = view.findViewById(R.id.listView_todo);
//        listView.setAdapter(listAdapter);
        return inflater.inflate(R.layout.fragment_to_do, container, false)
    }


    private fun getBankTodos() {
        mBankToDoList.clear()
        mBankToDoList.addAll(dc.bankToDosMoney)

        Collections.sort(mBankToDoList, Comparator { o1, o2 ->
            if (o1.dateTime.timeInMillis < o2.dateTime.timeInMillis) {
                -1
            } else {
                1
            }
        })
        mBankToDoList.addAll(dc.bankToDosNoMoney)
    }

    inner class TodoListAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return mBankToDoList.size
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            convertView = View.inflate(context!!, R.layout.inflate_list_item_todo, null)
            try {
                val bankToDo = mBankToDoList[position]
                val now = DateTime()
                val dayOffset = DateTime.dayOffset(now, bankToDo.dateTime)
                val textViewDate = convertView.findViewById<TextView>(R.id.tv_att_date)
                val textViewDays = convertView.findViewById<TextView>(R.id.textView_days)
                val textViewName = convertView.findViewById<TextView>(R.id.tv_name)
                val textViewMoney = convertView.findViewById<TextView>(R.id.textView_money)
                if (bankToDo.money <= 0.0) {
                    textViewName.setTextColor(Color.GRAY)
                } else {
                    textViewName.setTextColor(Color.BLACK)
                }
                val layoutRoot: ConstraintLayout = convertView.findViewById(R.id.layout_root)
                layoutRoot.setOnLongClickListener { //  3/1 001 编辑选中项
                    _DialogUtils.editTodoDialog(context!!, bankToDo, null)
                    true
                }
                layoutRoot.setOnClickListener {

                    AlertDialog.Builder(context!!).setTitle(bankToDo.bankName).setItems(arrayOf("编辑", "删除", "月+", "月-",if(bankToDo.money<=0.0) "启用" else "停用")) { dialog, which ->
                        when (which) {
                            0 ->                                         //  3/1 001 编辑选中项
                                _DialogUtils.editTodoDialog(context!!, bankToDo) {
                                    getBankTodos()
                                    listAdapter!!.notifyDataSetChanged()
                                }
                            1 ->
                                // 3/1 001 删除选中项
                                AlertDialog.Builder(context!!).setMessage("确定要删除此项目及其所有记录吗？").setNegativeButton("确定") { dialog, which ->
                                    dc.deleteBankToDo(bankToDo.id)
                                    getBankTodos()
                                    listAdapter!!.notifyDataSetChanged()

                                    // 发送更新广播
                                    val intent = Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW)
                                    context!!.sendBroadcast(intent)
                                }.setPositiveButton("取消") { dialog, which -> dialog.dismiss() }.show()
                            2 -> AlertDialog.Builder(context!!).setMessage("是否将【" + bankToDo.bankName + "】目标日期切换至" + bankToDo.dateTime.addMonths(1).toShortDateString3() + "？").setPositiveButton("是") { dialog, which ->
                                bankToDo.dateTime = bankToDo.dateTime.addMonths(1)
                                dc.editBankToDo(bankToDo)
                                getBankTodos()
                                listAdapter!!.notifyDataSetChanged()
                                // 发送更新广播
                                val intent = Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW)
                                context!!.sendBroadcast(intent)
                            }.setNegativeButton("否", null).show()
                            3 -> AlertDialog.Builder(context!!).setMessage("是否将【" + bankToDo.bankName + "】目标日期切换至" + bankToDo.dateTime.addMonths(-1).toShortDateString3() + "？").setPositiveButton("是") { dialog, which ->
                                bankToDo.dateTime = bankToDo.dateTime.addMonths(-1)
                                dc.editBankToDo(bankToDo)
                                getBankTodos()
                                listAdapter!!.notifyDataSetChanged()
                                // 发送更新广播
                                val intent1 = Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW)
                                context!!.sendBroadcast(intent1)
                            }.setNegativeButton("否", null).show()
                            4->{
                                if(bankToDo.money<=0){
                                    bankToDo.money=100.0
                                }else{
                                    bankToDo.money=0.0
                                }
                                dc.editBankToDo(bankToDo)
                                getBankTodos()
                                listAdapter!!.notifyDataSetChanged()
                            }
                        }
                    }.show()
                }
                val formatter = DecimalFormat("#,##0")
                textViewDate.text = bankToDo.dateTime.toShortDateString1()
                if (dayOffset < 0) {
                    textViewDays.text = "+" + -dayOffset
                } else {
                    textViewDays.text = dayOffset.toString() + ""
                }
                textViewName.text = bankToDo.bankName
                textViewMoney.text = formatter.format(bankToDo.money)
                if (bankToDo.money <=0) {
                    textViewDate.text = ""
                    textViewDays.text = ""
                    textViewMoney.text = ""
                }
            } catch (e: Exception) {
                _Utils.printException(context!!, e)
                Log.e("wangsc", e.message!!)
            }
            return convertView
        }
    }
}