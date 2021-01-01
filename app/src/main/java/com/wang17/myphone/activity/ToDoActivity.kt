package com.wang17.myphone.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import com.wang17.myphone.R
import com.wang17.myphone.e
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.DateTime.Companion.dayOffset
import com.wang17.myphone.model.database.BankToDo
import com.wang17.myphone.util.DataContext
import com.wang17.myphone.util._DialogUtils
import com.wang17.myphone.util._Utils
import com.wang17.myphone.widget.MyWidgetProvider
import kotlinx.android.synthetic.main.fragment_to_do.*
import java.text.DecimalFormat
import java.util.*

class ToDoActivity : FragmentActivity() {
    private lateinit var mDataContext: DataContext
    private lateinit var mBankToDoList: List<BankToDo>
    private lateinit var listAdapter: TodoListAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //region activity启用这段代码
        setContentView(R.layout.fragment_to_do)
        mDataContext = DataContext(this)
        getBankTodos()
        listView_todo.setAdapter(listAdapter)
        val actionButton: FloatingActionButton = findViewById(R.id.floatingActionButtonAdd)
        actionButton.setOnClickListener { //  3/1 001 新建待办事项
            _DialogUtils.addTodoDialog(this@ToDoActivity) {
                getBankTodos()
                listAdapter.notifyDataSetChanged()
            }
        }
        actionButton.setOnLongClickListener {
            startActivity(Intent(this@ToDoActivity, SmsActivity::class.java))
            true
        }
        //endregion
    }

    private fun getBankTodos() {
        mBankToDoList = mDataContext.bankToDos
        e(mBankToDoList.size)
        mBankToDoList.forEach {
            e(it.money)
        }

        Collections.sort(mBankToDoList, Comparator { o1, o2 ->
//                if (o1.money == -1.0) {
//                    return@Comparator 1
//                }
//                if (o2.money == -1.0) {
//                    return@Comparator -1
//                }

//                return (int)(o1.getDateTime().getTimeInMillis()-o2.getDateTime().getTimeInMillis());
            if (o1.dateTime.timeInMillis < o2.dateTime.timeInMillis) {
                -1
            } else {
                1
            }
        })
        listAdapter = TodoListAdapter()
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
            convertView = View.inflate(this@ToDoActivity, R.layout.inflate_list_item_todo, null)
            try {
                val bankToDo = mBankToDoList[position]
                val now = DateTime()
                val dayOffset = dayOffset(now, bankToDo.dateTime)
                val textViewDate = convertView.findViewById<TextView>(R.id.textView_date)
                val textViewDays = convertView.findViewById<TextView>(R.id.textView_days)
                val textViewName = convertView.findViewById<TextView>(R.id.textView_name)
                val textViewMoney = convertView.findViewById<TextView>(R.id.textView_money)
                if (bankToDo.money == 0.0 || bankToDo.money == -1.0) {
                    textViewName.setTextColor(Color.GRAY)
                } else {
                    textViewName.setTextColor(Color.BLACK)
                }
                val layoutRoot: ConstraintLayout = convertView.findViewById(R.id.layout_root)
                layoutRoot.setOnLongClickListener { //  3/1 001 编辑选中项
                    _DialogUtils.editTodoDialog(this@ToDoActivity, bankToDo, null)
                    true
                }
                layoutRoot.setOnClickListener {
                    AlertDialog.Builder(this@ToDoActivity).setItems(arrayOf("编辑", "删除", "月+", "月-")) { dialog, which ->
                        when (which) {
                            0 ->                                         //  3/1 001 编辑选中项
                                _DialogUtils.editTodoDialog(this@ToDoActivity, bankToDo) { listAdapter!!.notifyDataSetChanged() }
                            1 ->
                                // 3/1 001 删除选中项
                                AlertDialog.Builder(this@ToDoActivity).setMessage("确定要删除此项目及其所有记录吗？").setNegativeButton("确定") { dialog, which ->
                                    mDataContext!!.deleteBankToDo(bankToDo.id)
                                    mBankToDoList = mDataContext!!.bankToDos
                                    listAdapter!!.notifyDataSetChanged()

                                    // 发送更新广播
                                    val intent = Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW)
                                    this@ToDoActivity.sendBroadcast(intent)
                                }.setPositiveButton("取消") { dialog, which -> dialog.dismiss() }.show()
                            2 -> AlertDialog.Builder(this@ToDoActivity).setMessage("是否将" + bankToDo.bankName + "目标日期切换至" + bankToDo.dateTime.addMonths(1).toShortDateString3() + "？").setPositiveButton("是") { dialog, which ->
                                bankToDo.dateTime = bankToDo.dateTime.addMonths(1)
                                mDataContext!!.editBankToDo(bankToDo)
                                mBankToDoList = mDataContext!!.bankToDos
                                listAdapter!!.notifyDataSetChanged()
                                // 发送更新广播
                                val intent = Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW)
                                this@ToDoActivity.sendBroadcast(intent)
                            }.setNegativeButton("否", null).show()
                            3 -> AlertDialog.Builder(this@ToDoActivity).setMessage("是否将" + bankToDo.bankName + "目标日期切换至" + bankToDo.dateTime.addMonths(-1).toShortDateString3() + "？").setPositiveButton("是") { dialog, which ->
                                bankToDo.dateTime = bankToDo.dateTime.addMonths(-1)
                                mDataContext!!.editBankToDo(bankToDo)
                                mBankToDoList = mDataContext!!.bankToDos
                                listAdapter!!.notifyDataSetChanged()
                                // 发送更新广播
                                val intent1 = Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW)
                                this@ToDoActivity.sendBroadcast(intent1)
                            }.setNegativeButton("否", null).show()
                        }
                    }.show()
                }
                val formatter = DecimalFormat("#,##0")
                textViewDate.text = bankToDo.dateTime.toShortDateString()
                if (dayOffset < 0) {
                    textViewDays.text = "+" + -dayOffset
                } else {
                    textViewDays.text = dayOffset.toString() + ""
                }
                textViewName.text = bankToDo.bankName
                textViewMoney.text = formatter.format(bankToDo.money)
                if (bankToDo.money == -1.0) {
                    textViewDays.text = ""
                    textViewMoney.text = ""
                }
            } catch (e: Exception) {
                _Utils.printException(this@ToDoActivity, e)
                Log.e("wangsc", e.message!!)
            }
            return convertView
        }
    }
}