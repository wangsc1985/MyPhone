package com.wang17.myphone.fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CalendarView
import android.widget.EditText
import android.widget.TextView
import com.wang17.myphone.R
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.database.Loan
import com.wang17.myphone.database.LoanRecord
import com.wang17.myphone.e
import com.wang17.myphone.model.DateTime
import kotlinx.android.synthetic.main.fragment_loan.*
import java.lang.Exception
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class LoanFragment : Fragment() {

    var loanList: MutableList<Loan> = ArrayList()
    var adapter = LoanListAdapter()
    lateinit var dc: DataContext

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dc = DataContext(context)

        loadLoanData()
        e("+++++++++++++++++"+loanList.size)
        loanList.forEach {
            e(it.name)
        }

        lv_loan.adapter = adapter
        lv_loan.setOnItemClickListener { parent, view, position, id ->
            val loan = loanList.get(position)
            AlertDialog.Builder(context!!).setItems(arrayOf("还款","删除","详单")){ dialogInterface: DialogInterface, i: Int ->
                when(i){
                    0->{
                        val view = View.inflate(context, R.layout.inflate_dialog_add_loan_record, null)
                        val cvDate = view.findViewById<CalendarView>(R.id.cv_date)
                        cvDate.date = loan.date.timeInMillis
                        val etSum = view.findViewById<EditText>(R.id.et_sum)
                        cvDate.setOnDateChangeListener { view, year, month, dayOfMonth -> view.date = DateTime(year, month, dayOfMonth).timeInMillis }
                        AlertDialog.Builder(context).setView(view).setPositiveButton("还款") { dialog, which ->
                            val date = DateTime(cvDate.date)

                            val loanRecord = LoanRecord(UUID.randomUUID(),date,etSum.text.toString().toBigDecimal()*-1.toBigDecimal(),0.toBigDecimal(),loan.id)
                            val days = (loanRecord.date.timeInMillis - loan.date.timeInMillis) / (1000 * 60 * 60 * 24)
                            loanRecord.interest = loanRecord.sum.setScale(10) / -10000.toBigDecimal() * loan.rate * days.toBigDecimal()
                            loan.sum+=loanRecord.sum
                            loan.interest += loanRecord.interest
                            dc.addLoanRecord(loanRecord)
                            dc.editLoan(loan)

                            loadLoanData()
                            adapter.notifyDataSetChanged()
                        }.show()
                    }
                    1->{
                        dc.deleteLoanRecords(loanList.get(position).id)
                        dc.deleteLoan(loanList.get(position).id)
                    }
                    2->{
                        val loanRecordList = dc.getLoanRecordList(loan.id)
                        var values = arrayOfNulls<String>(loanRecordList.size)
                        for (i in loanRecordList.indices){
                            values[i]="${loanRecordList[i].date.toShortDateString()}  ${format.format(loanRecordList[i].sum)}  ${format.format(loanRecordList[i].interest)}"
                        }
                        AlertDialog.Builder(context!!).setItems(values){ dialogInterface: DialogInterface, i: Int ->
                            AlertDialog.Builder(context!!).setMessage("删除当前还款记录？").setPositiveButton("确定"){ dialogInterface: DialogInterface, ii: Int ->
                                val loanRecord = loanRecordList[i]
                                loan.sum-=loanRecord.sum
                                loan.interest -=loanRecord.interest
                                dc.editLoan(loan)
                                dc.deleteLoanRecord(loanRecord.id)

                                loadLoanData()
                                adapter.notifyDataSetChanged()
                            }.show()
                        }.show()
                    }
                }
                loadLoanData()
                adapter.notifyDataSetChanged()
            }.show()
        }
        lv_loan.setOnItemLongClickListener { parent, view, position, id ->
            var et = EditText(context!!)
            val loan = loanList.get(position)
            et.setText(loan.rate.toString())
            AlertDialog.Builder(context!!).setView(et).setPositiveButton("修改"){dialog,which->
                loan.rate = et.text.toString().toBigDecimal()
                dc.editLoan(loan)

                loadLoanData()
                adapter.notifyDataSetChanged()
            }.show()
            true
        }
//        fab_add.setOnClickListener {
//            val view = View.inflate(context, R.layout.inflate_dialog_add_loan, null)
//            val etName = view.findViewById<EditText>(R.id.et_name)
//            val cvDate = view.findViewById<CalendarView>(R.id.cv_date)
//            val etSum = view.findViewById<EditText>(R.id.et_sum)
//            val etRate = view.findViewById<EditText>(R.id.et_rate)
//            cvDate.setOnDateChangeListener { view, year, month, dayOfMonth -> view.date = DateTime(year, month, dayOfMonth).timeInMillis }
//            AlertDialog.Builder(context).setView(view).setPositiveButton("添加") { dialog, which ->
//                val date = DateTime(cvDate.date)
//                e(date.toShortDateString())
//
//                val loan = Loan(UUID.randomUUID(),etName.text.toString(),date,etSum.text.toString().toBigDecimal(),etRate.text.toString().toBigDecimal(),0.toBigDecimal())
//                val loanRecord = LoanRecord(UUID.randomUUID(),date,etSum.text.toString().toBigDecimal(),0.toBigDecimal(),loan.id)
//
//                dc.addLoan(loan)
//                dc.addLoanRecord(loanRecord)
//
//                loadLoanData()
//                adapter.notifyDataSetChanged()
//            }.show()
//        }
    }

    private fun loadLoanData() {
        loanList = dc.loanList
        var money1 = 0.toBigDecimal()
        var money2=0.toBigDecimal()
        loanList.forEach {loan->
            money1 += loan.interest
            val days = (DateTime.today.timeInMillis - loan.date.timeInMillis) / (1000 * 60 * 60 * 24)
            money2 += loan.sum.setScale(10) / 10000.toBigDecimal() * loan.rate * days.toBigDecimal()
        }

        tv_money.text="结息：${format.format(money1)}  在途：${format.format(money2)}  总计：${format.format(money1+money2)}"
    }

    inner class LoanListAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return loanList.size
        }

        override fun getItem(position: Int): Any {
            return loanList.get(position)
        }

        override fun getItemId(position: Int): Long {
            return 0L
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            var convertView = convertView
            try {
                convertView = View.inflate(context!!,R.layout.inflate_list_item_loan,null)
                convertView?.apply {
                    val loan = loanList.get(position)
                    val tvName = findViewById<TextView>(R.id.tv_name)
                    val tvDate = findViewById<TextView>(R.id.tv_date)
                    val tvSum = findViewById<TextView>(R.id.tv_sum)
                    val tvInterest = findViewById<TextView>(R.id.tv_interest)
                    val tvInterestRunning = findViewById<TextView>(R.id.tv_interest_running)
                    val tvDays = findViewById<TextView>(R.id.tv_days)
                    val tvRate = findViewById<TextView>(R.id.tv_rate)

                    tvName.text = loan.name
                    tvSum.text =  "${DecimalFormat("#,##0.00").format(loan.sum.toFloat()/10000)}万"
                    tvDate.text = loan.date.toShortDateString()
                    tvRate.text = "${loan.rate}/万/日"
                    val days = (DateTime.today.timeInMillis - loan.date.timeInMillis) / (1000 * 60 * 60 * 24)
                    tvDays.text = "${days/30}月${days%30}天"
                    tvInterest.text = "结息：${format.format(loan.interest)}"
                    tvInterestRunning.text="在途：${format.format(loan.sum.setScale(10) / 10000.toBigDecimal() * loan.rate * days.toBigDecimal())} "
                    e("${loan.name}  ${loan.rate}  ${loan.sum}  ${loan.date.toShortDateString()} ${loan.sum.setScale(10) / 10000.toBigDecimal()* loan.rate* days.toBigDecimal()}")
                    if(loan.sum>0.toBigDecimal()){
//                        tvName.setTextColor(Color.RED)
                        tvSum.setTextColor(Color.RED)
//                        tvDate.setTextColor(Color.RED)
//                        tvDays.setTextColor(Color.RED)
//                        tvInterest.setTextColor(Color.RED)
                    }else{
//                        tvName.setTextColor(Color.BLACK)
                        tvSum.setTextColor(Color.BLACK)
//                        tvDate.setTextColor(Color.BLACK)
//                        tvDays.setTextColor(Color.BLACK)
//                        tvInterest.setTextColor(Color.BLACK)
                    }
                }
            } catch (e: Exception) {
                e(e.message!!)
            }
            return convertView
        }

    }

    val format2 = DecimalFormat("#,##0.00")
    val format = DecimalFormat("#,##0")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_loan, container, false)
    }
}