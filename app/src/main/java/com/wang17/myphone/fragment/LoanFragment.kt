package com.wang17.myphone.fragment

import android.app.AlertDialog
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
        lv_loan.setOnItemLongClickListener { parent, view, position, id ->
            dc.deleteLoan(loanList.get(position).id)
            loadLoanData()
            adapter.notifyDataSetChanged()
            true
        }
        fab_add.setOnClickListener {
            val view = View.inflate(context, R.layout.inflate_dialog_add_loan, null)
            val etName = view.findViewById<EditText>(R.id.et_name)
            val cvDate = view.findViewById<CalendarView>(R.id.cv_date)
            val etSum = view.findViewById<EditText>(R.id.et_sum)
            val etRate = view.findViewById<EditText>(R.id.et_rate)
            cvDate.setOnDateChangeListener { view, year, month, dayOfMonth -> view.date = DateTime(year, month, dayOfMonth).timeInMillis }
            AlertDialog.Builder(context).setView(view).setPositiveButton("添加") { dialog, which ->
                val date = DateTime(cvDate.date)
                e(date.toShortDateString())

                val loan = Loan(UUID.randomUUID(),etName.text.toString(),date,etSum.text.toString().toBigDecimal(),etRate.text.toString().toBigDecimal())
                dc.addLoan(loan)

                loadLoanData()
                adapter.notifyDataSetChanged()
            }.show()


        }
    }

    private fun loadLoanData() {
        loanList = dc.loanList
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
                    val tvDays = findViewById<TextView>(R.id.tv_days)

                    tvName.text = loan.name
                    tvSum.text =  DecimalFormat("#,##0").format(loan.sum)
                    tvDate.text = loan.date.toShortDateString1()
                    val days = (DateTime.today.timeInMillis - loan.date.timeInMillis) / (1000 * 60 * 60 * 24)
                    tvDays.text = "${days/30}月${days%30}天"
                    tvInterest.text = DecimalFormat("#,##0.00").format(loan.sum.setScale(10) / 10000.toBigDecimal() * loan.rate * days.toBigDecimal())
                    e("${loan.name}  ${loan.rate}  ${loan.sum}  ${loan.date.toShortDateString()} ${loan.sum.setScale(10) / 10000.toBigDecimal()* loan.rate* days.toBigDecimal()}")
                }
            } catch (e: Exception) {
                e(e.message!!)
            }
            return convertView
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_loan, container, false)
    }
}