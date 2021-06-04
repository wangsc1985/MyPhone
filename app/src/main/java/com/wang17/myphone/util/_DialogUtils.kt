package com.wang17.myphone.util

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View.OnFocusChangeListener
import android.widget.CalendarView
import android.widget.EditText
import com.wang17.myphone.R
import com.wang17.myphone.activity.DesktopDialogActivity
import com.wang17.myphone.callback.MyCallback
import com.wang17.myphone.database.BankToDo
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.util._Utils.e
import com.wang17.myphone.util._Utils.printException
import com.wang17.myphone.widget.MyWidgetProvider
import java.text.DecimalFormat

object _DialogUtils {

    fun showDesktopDialog(context: Context, msg: String){
        val intent = Intent(context, DesktopDialogActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        intent.putExtra("msg", msg)
        context.startActivity(intent)
    }

    @JvmStatic
    fun showMessageBox(context: Context, msg: String){
        AlertDialog.Builder(context).setMessage(msg).show()
    }
    @JvmStatic
    fun showMessageBox(context: Context, msg: String, btnText: String){
        AlertDialog.Builder(context).setMessage(msg).setCancelable(false).setPositiveButton(btnText, null).show()
    }
    @JvmStatic
    fun showMessageBox(context: Context, msg: String, btnText: String, btnListener: DialogInterface.OnClickListener?){
        AlertDialog.Builder(context).setMessage(msg).setCancelable(false).setPositiveButton(btnText, btnListener).show()
    }
    /**
     * 新建todo项
     */
    @JvmStatic
    fun addTodoDialog(context: Context, callback: MyCallback?) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_todo, null)
        val cvDate = view.findViewById<CalendarView>(R.id.cv_date)
        val etBankName = view.findViewById<EditText>(R.id.et_bankName)
        val etMoney = view.findViewById<EditText>(R.id.et_money)
        val etSummary = view.findViewById<EditText>(R.id.et_summary)
        val dateTime = DateTime()
        cvDate.setOnDateChangeListener { view, year, month, dayOfMonth -> dateTime[year, month] = dayOfMonth }
        AlertDialog.Builder(context)
                .setView(view).setCancelable(false).setPositiveButton("提交") { dialog, which ->
                    try {
                        val bankName = etBankName.text.toString()
                        val money = etMoney.text.toString().toDouble()
                        val number = etSummary.text.toString()
                        val dataContext = DataContext(context)
                        val bankToDo = BankToDo(dateTime, bankName, number, money)
                        dataContext.addBankToDo(bankToDo)


                        // 发送更新广播
                        val intent = Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW)
                        context.sendBroadcast(intent)
                        callback?.execute()
                    } catch (e: Exception) {
                        printException(context, e)
                    }
                }.setNegativeButton("取消", null).create().show()
    }

    /**
     * 编辑todo项
     */
    @JvmStatic
    fun editTodoDialog(context: Context, bankToDo: BankToDo, callback: MyCallback?) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_todo, null)
        val dataContext = DataContext(context)
        val dateTime = bankToDo.dateTime
        val calendarViewDate = view.findViewById<CalendarView>(R.id.cv_date)
        val editTextTitle = view.findViewById<EditText>(R.id.et_bankName)
        val editTextSummery = view.findViewById<EditText>(R.id.et_money)
        val editTextNumber = view.findViewById<EditText>(R.id.et_summary)
        e("xxxxxxxxxxxxxxxxxxxxxxxx : " + bankToDo.dateTime.toLongDateTimeString())
        calendarViewDate.date = bankToDo.dateTime.timeInMillis
        calendarViewDate.setOnDateChangeListener { view, year, month, dayOfMonth -> dateTime[year, month] = dayOfMonth }
        editTextTitle.setText(bankToDo.bankName)
        editTextTitle.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                editTextTitle.selectAll()
            }
        }
        var format = "#,##0.00"
        if (bankToDo.money % 1 == 0.0) {
            format = "#,##0"
        }
        editTextSummery.setText(DecimalFormat(format).format(bankToDo.money))
        editTextSummery.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                editTextSummery.selectAll()
            }
        }
        editTextNumber.setText(bankToDo.summary)
        editTextNumber.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                editTextNumber.selectAll()
            }
        }
        AlertDialog.Builder(context).setView(view).setCancelable(false).setPositiveButton("提交") { dialog, which ->
            try {
                val bankName = editTextTitle.text.toString()
                val money = editTextSummery.text.toString().replace(",", "").toDouble()
                val number = editTextNumber.text.toString()
                bankToDo.bankName = bankName
                bankToDo.dateTime = dateTime
                bankToDo.money = money
                bankToDo.summary = number
                dataContext.editBankToDo(bankToDo)

                // 发送更新广播
                val intent = Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW)
                context.sendBroadcast(intent)
                callback?.execute()
            } catch (e: Exception) {
                printException(context, e)
            }
        }.setNegativeButton("取消", null).create().show()
    }
}