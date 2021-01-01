package com.wang17.myphone.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.wang17.myphone.R
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.database.Trade
import com.wang17.myphone.util.DataContext
import com.wang17.myphone.util._Utils
import kotlinx.android.synthetic.main.activity_trades.*
import java.text.DecimalFormat
import java.util.*

class TradesActivity : AppCompatActivity() {

    private var dataContext: DataContext? = null
    private lateinit var trades: List<Trade>
    private var adapter: TradesListdAdapter? = null
    private var code: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trades)
        dataContext = DataContext(this)
        code = intent.getStringExtra("code")
        trades = dataContext!!.getTrades(code)

        var cost = 0.0
        var amount = 0
        var profit = 0.0
        for (i in trades.indices.reversed()) {
            val td = trades[i]
            // 成本总资金
            val costFund = cost * amount * 100
            // 成交总资金
            val tradeFund = td.price * td.amount * 100
            // 佣金
            val commission = commission(tradeFund)
            // 过户费
            val transferFee = transferFee(tradeFund)
            // 印花税
            val tax: Double = (if (td.type == 1) 0.0 else tax(tradeFund))

            if (td.type == -2 || td.type == 2) {

                // 计算成本
                if (amount == 0) {
                    cost = 0.0
                    // 计算盈利
                    profit += td.price
                } else {
                    cost = (cost * amount * 100 - td.price) / (amount * 100)
                }
            } else {
                /**
                 * 计算盈利
                 */
                if (td.type == -1) {
                    profit += ((td.price - cost) * td.amount * 100) - commission - transferFee - tax
                }
                /**
                 * 计算成本
                 */
                // 清仓
                if (amount + td.type * td.amount == 0) {
                    cost = 0.0
                } else {
                    cost = (costFund + (td.type * tradeFund) + commission + transferFee + tax) / (amount * 100 + td.type * td.amount * 100)
                }
                amount = amount + td.type * td.amount
            }
            td.cost = cost
        }

        //
        adapter = TradesListdAdapter()
        listView_trades.setAdapter(adapter)
        listView_trades.setOnItemClickListener(OnItemClickListener { parent, view, position, id ->
            editTradeDialog(trades.get(position))
            //                new AlertDialog.Builder(TradesActivity.this).setMessage("是否删除？").setPositiveButton("是", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                    }
//                }).setNegativeButton("否", null).show();
        })
    }

    fun editTradeDialog(trade: Trade) {
        val view = View.inflate(this, R.layout.inflate_dialog_add_trade, null)
        val dialog = AlertDialog.Builder(this).create()
        dialog.setView(view)
        val calendarViewDate = view.findViewById<CalendarView>(R.id.calendarView_date)
        val editTextCode = view.findViewById<EditText>(R.id.editText_code)
        val editTextCost = view.findViewById<EditText>(R.id.editText_cost)
        val editTextAmount = view.findViewById<EditText>(R.id.editText_amount)
        val switchAtt = view.findViewById<Switch>(R.id.switch_att)
        calendarViewDate.date = trade.dateTime.timeInMillis
        calendarViewDate.setOnDateChangeListener { view, year, month, dayOfMonth -> view.date = DateTime(year, month, dayOfMonth).timeInMillis }
        editTextCode.setText(trade.name)
        editTextCode.isEnabled = false
        editTextCost.setText(trade.price.toString() + "")
        editTextAmount.setText(trade.amount.toString() + "")
        switchAtt.isChecked = trade.tag == 1
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "修改") { dialog, which ->
            try {
                val tradeAmount = editTextAmount.text.toString().toInt()
                val tradePrice = editTextCost.text.toString().toDouble()

                //
                val selectDate = DateTime(calendarViewDate.date)
                val trdt = DateTime()
                trdt[Calendar.YEAR] = selectDate[Calendar.YEAR]
                trdt[Calendar.DAY_OF_YEAR] = selectDate[Calendar.DAY_OF_YEAR]
                trade.price = tradePrice
                trade.dateTime = trdt
                trade.amount = tradeAmount
                trade.tag = if (switchAtt.isChecked) 1 else 0
                dataContext!!.editTrade(trade)
                trades = dataContext!!.getTrades(code)
                //                    for(Trade t : trades){
//                        e(t.getName()+"  "+ t.getDateTime().toLongDateTimeString());
//                    }
                setResult(RESULT_OK)
                adapter!!.notifyDataSetChanged()
                dialog.dismiss()
            } catch (e: NumberFormatException) {
                Snackbar.make(editTextCode, "成本价必须为数字", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                _Utils.printException(this@TradesActivity, e)
            }
        }
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "删除") { dialog, which ->
            val position = dataContext!!.getPosition(trade.code)
            position.amount = position.amount - trade.amount
            dataContext!!.editPosition(position)
            dataContext!!.deleteTrade(trade.id)
            trades = dataContext!!.getTrades(code)
            adapter!!.notifyDataSetChanged()
            dialog.dismiss()
            setResult(RESULT_OK)
        }
        dialog.show()
    }

    private fun e(log: Any) {
        Log.e("wangsc", log.toString())
    }

    /**
     * 印花税  卖出时千1
     *
     * @param money
     */
    private fun tax(money: Double): Double {
        return money / 1000
    }

    /**
     * 佣金  万3  最低5元
     */
    private fun commission(money: Double): Double {
        var money = money
        money = money * 3 / 10000
        return if (money < 5) 5.0 else money
    }

    /**
     * 过户费   十万2
     */
    private fun transferFee(money: Double): Double {
        return money * 2 / 100000
    }

    protected inner class TradesListdAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return trades!!.size
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
                convertView = View.inflate(this@TradesActivity, R.layout.inflate_trade, null)
                val trade = trades!![position]
                val textViewDate = convertView.findViewById<TextView>(R.id.textView_dateTime)
                val textViewName = convertView.findViewById<TextView>(R.id.textView_name)
                val textViewPrice = convertView.findViewById<TextView>(R.id.textView_price)
                val textViewAmount = convertView.findViewById<TextView>(R.id.textView_amount)
                val textViewType = convertView.findViewById<TextView>(R.id.textView_type)
                textViewDate.text = trade.dateTime.toShortDateString()
                textViewName.text = trade.name
                var format = DecimalFormat("#0.00")
                textViewPrice.text = "${format.format(trade.price)}(${format.format(trade.cost)})"
                format = DecimalFormat("#0")
                var type = "买入"
                var amount: String? = "-"
                when (trade.type) {
                    1 -> {
                        type = "买入"
                        amount = format.format((trade.amount * 100).toLong())
                    }
                    -1 -> {
                        amount = format.format((trade.amount * 100).toLong())
                        type = "卖出"
                    }
                    2 -> type = "股息"
                    -2 -> type = "息税"
                }
                textViewType.text = type
                textViewAmount.text = amount
                if (trade.tag == 1) {
                    textViewName.setTextColor(Color.RED)
                } else if (trade.tag == -1) {
                    textViewName.setTextColor(Color.BLUE)
                }
            } catch (e: Exception) {
                _Utils.printException(this@TradesActivity, e)
            }
            return convertView
        }
    }
}