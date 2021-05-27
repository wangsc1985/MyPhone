package com.wang17.myphone.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.CalendarView.OnDateChangeListener
import com.alibaba.fastjson.JSON
import com.wang17.myphone.*
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.database.*
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.StockInfo
import com.wang17.myphone.util.*
import com.wang17.myphone.util.TradeUtils.commission
import com.wang17.myphone.util.TradeUtils.tax
import com.wang17.myphone.util.TradeUtils.transferFee
import kotlinx.android.synthetic.main.activity_history_position.*
import okhttp3.Request
import okhttp3.Response
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class StockPositionHistoryActivity() : AppCompatActivity() {
    private lateinit var dc: DataContext
    private lateinit var positions: List<Position>
    private lateinit var wakeLock: PowerManager.WakeLock

    private var adapter: PositionListdAdapter = PositionListdAdapter()
    private var totalProfit = 0.toBigDecimal()

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
        _Utils.releaseWakeLock(this, wakeLock)
    }

    private val TO_ACTIVITY_TRADES = 1
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            TO_ACTIVITY_TRADES -> {
                e("从交易详单页面返回")
                if (resultCode == Activity.RESULT_OK) {
                    e("交易详单页面有内容修改")
                    positions = dc.allPositions
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_position)
        dc = DataContext(this)
        positions = dc.allPositions
        for (sp: Position in positions) {
            totalProfit += sp.profit
        }
        val format = DecimalFormat("#,##0")
        textView_profit.setText(format.format(totalProfit))
        fab_add.setOnClickListener {
            addStockDialog()
        }
        fab_refresh.setOnLongClickListener {
            AlertDialog.Builder(this@StockPositionHistoryActivity).setMessage("是否重新计算所有股票的收益？").setPositiveButton("是", DialogInterface.OnClickListener { dialog, which ->
                refreshData()
            }).show()
            true
        }
        fab_upload.setOnLongClickListener {
            AlertDialog.Builder(this@StockPositionHistoryActivity).setMessage("是否上传云端？").setPositiveButton("是") { dialog: DialogInterface, i: Int ->
                updatePositions()
            }.show()
            true
        }
        fab_chart.setOnClickListener {

            val statements = dc.getStatements(DateTime().addDays(-30)).reversed()
            if(statements.size>0){
                val last = statements[0]
                var arr = arrayOfNulls<String>(statements.size)
                for(i in statements.indices){
                    arr[i] = "${statements[i].date.toShortDateString1()}   ${DecimalFormat("#,##0").format(statements[i].profit)}  ${DecimalFormat("#,##0.00").format(statements[i].profit*100.toBigDecimal()/last.fund)}"
                }

                AlertDialog.Builder(this).setItems(arr,null).show()
            }
        }
        fab_chart.setOnLongClickListener {
            startActivity(Intent(this,ChartActivity::class.java))
            true
        }
        adapter = PositionListdAdapter()
        timer.schedule(object : TimerTask() {
            override fun run() {
                _SinaStockUtils.getStockInfoList(attTrades)
            }
        }, 0, 5000)
        listView_position.setAdapter(adapter)
        listView_position.setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View, index: Int, id: Long) {
                AlertDialog.Builder(this@StockPositionHistoryActivity).setItems(arrayOf("加仓", "减仓", "股息", "补税", "删除"), object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        val position = positions.get(index)
                        when (which) {
                            0 ->                                 // 加仓
                                buyMoreDialog(position)
                            1 ->                                 // 减仓
                                saleSomeDialog(position)
                            2 ->                                 // 股息
                                dividendDialog(position)
                            3 ->                                 // 补税
                                taxDialog(position)
                            4 ->                                 // 删除
                                AlertDialog.Builder(this@StockPositionHistoryActivity).setMessage("是否删除？").setPositiveButton("是", object : DialogInterface.OnClickListener {
                                    override fun onClick(dialog: DialogInterface, which: Int) {
                                        dc.deletePosition(positions.get(index).id)
                                        positions = dc.allPositions
                                        adapter.notifyDataSetChanged()
                                    }
                                }).setNegativeButton("否", null).show()
                            5 ->                                 // 重计成本
                                modifyCoseDialog(position)
                        }
                    }
                }).show()
            }
        })
        listView_position.setOnItemLongClickListener(object : OnItemLongClickListener {
            override fun onItemLongClick(parent: AdapterView<*>?, view: View, position: Int, id: Long): Boolean {
                val stock = positions.get(position)
                val intent = Intent(this@StockPositionHistoryActivity, TradesActivity::class.java)
                intent.putExtra("code", stock.code)
                startActivityForResult(intent, TO_ACTIVITY_TRADES)
                return true
            }
        })

        wakeLock = _Utils.acquireWakeLock(this, PowerManager.SCREEN_BRIGHT_WAKE_LOCK)!!
    }

    private fun refreshData() {
        totalProfit = 0.toBigDecimal()
        for (pos in positions) {
            val tradeList = dc.getTrades(pos.code)
            var cost = 0.toBigDecimal()
            var amount = 0
            var profit = 0.toBigDecimal()
            for (td in tradeList.reversed()) {
                // 成本总资金
                val costFund = cost * (amount * 100).toBigDecimal()
                // 成交总资金
                val tradeFund = td.price * (td.amount * 100).toBigDecimal()
                // 佣金
                val commission = commission(td)
                // 过户费
                val transferFee = transferFee(td)
                // 印花税
                val tax = tax(td)
                if (td.type == -2 || td.type == 2) { // 股息 息稅

                    // 计算成本
                    if (amount == 0) {
                        cost = 0.toBigDecimal()
                        // 计算盈利
                        profit += td.price
                    } else {
                        cost = (cost * (amount * 100).toMyDecimal() - td.price) / (amount * 100).toBigDecimal()
                    }
                    td.hold = amount
                } else {
                    /**
                     * 如果是平仓：计算盈利。
                     */
                    if (td.type == -1) {
                        profit += (td.price - cost) * (td.amount * 100).toBigDecimal() - commission - transferFee - tax
                    }
                    /**
                     * 计算成本
                     */
                    // 清仓
                    if (amount + td.type * td.amount == 0) {
                        dc.editTrade(td)
                        cost = 0.toBigDecimal()
                    } else {
                        cost = (costFund + td.type.toMyDecimal() * tradeFund + commission + transferFee + tax) / (amount * 100 + td.type * td.amount * 100).toBigDecimal()
                    }
                    amount = amount + td.type * td.amount
                    td.hold = amount
                }
                dc.editTrade(td)
            }
            totalProfit += profit
            pos.profit = profit
            pos.amount = amount
            pos.cost = cost
            dc.editPosition(pos)
        }
        val format = DecimalFormat("#,##0")
        textView_profit.setText(format.format(totalProfit))
        Toast.makeText(this@StockPositionHistoryActivity, "计算完毕", Toast.LENGTH_LONG).show()
        positions = dc.allPositions
        adapter.notifyDataSetChanged()
    }

    private fun updatePositions() {
        val json = JSON.toJSONString(positions)
        _CloudUtils.updatePositions(this, dc.getSetting(Setting.KEYS.wx_request_code).string, json, object : CloudCallback {
            override fun excute(code: Int, result: Any) {
                runOnUiThread(object : Runnable {
                    override fun run() {
                        when (code) {
                            0 -> Toast.makeText(this@StockPositionHistoryActivity, "更新完毕", Toast.LENGTH_LONG).show()
                            -1 -> Toast.makeText(this@StockPositionHistoryActivity, result.toString(), Toast.LENGTH_LONG).show()
                            -2 -> Toast.makeText(this@StockPositionHistoryActivity, result.toString(), Toast.LENGTH_LONG).show()
                        }
                    }
                })
            }
        })
    }

    /**
     * 补税
     *
     * @param position
     */
    private fun taxDialog(position: Position) {
        val view = View.inflate(this, R.layout.inflate_dialog_tax, null)
        val dialog = AlertDialog.Builder(this).create()
        dialog.setView(view)
        dialog.setCancelable(false)
        val calendarViewDate = view.findViewById<CalendarView>(R.id.calendarView_date)
        val historyTrades = dc.getTrades(position.code)
        if (historyTrades.size != 0) calendarViewDate.date = historyTrades.get(0).dateTime.timeInMillis
        calendarViewDate.setOnDateChangeListener(object : OnDateChangeListener {
            override fun onSelectedDayChange(view: CalendarView, year: Int, month: Int, dayOfMonth: Int) {
                view.date = DateTime(year, month, dayOfMonth).timeInMillis
            }
        })
        val editTextCode = view.findViewById<EditText>(R.id.editText_code)
        val editTextMoney = view.findViewById<EditText>(R.id.editText_money)
        editTextCode.setText(position.name)
        editTextCode.isEnabled = false
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                try {
                    val money = -1.toBigDecimal() * editTextMoney.text.toString().toBigDecimal()

                    //
                    val selectDate = DateTime(calendarViewDate.date)
                    val trdt = DateTime()
                    trdt[Calendar.YEAR] = selectDate[Calendar.YEAR]
                    trdt[Calendar.DAY_OF_YEAR] = selectDate[Calendar.DAY_OF_YEAR]

                    // 成本总资金
                    val costFund = position.cost * (position.amount * 100).toBigDecimal()
                    val amount = position.amount
                    var cost = 0.toBigDecimal()
                    if (amount > 0) {
                        cost = (costFund - money).setMyScale() / (amount * 100).toBigDecimal()
                        position.cost = cost
                        dc.editPosition(position)
                        positions = dc.allPositions
                        adapter.notifyDataSetChanged()
                    }

                    dc.addTrade(Trade(trdt, position.code, position.name, money, position.amount, -2, 0, cost, position.amount))
                    dialog.dismiss()
                } catch (e: NumberFormatException) {
                    Snackbar.make(editTextCode, "成本价必须为数字", Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    _Utils.printException(this@StockPositionHistoryActivity, e)
                }
            }
        })
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "取消", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.dismiss()
            }
        })
        dialog.show()
    }

    /**
     * 股息
     *
     * @param position
     */
    private fun dividendDialog(position: Position) {
        val view = View.inflate(this, R.layout.inflate_dialog_tax, null)
        val dialog = AlertDialog.Builder(this).create()
        dialog.setView(view)
        dialog.setCancelable(false)
        val calendarViewDate = view.findViewById<CalendarView>(R.id.calendarView_date)
        val historyTrades = dc.getTrades(position.code)
        if (historyTrades.size != 0) calendarViewDate.date = historyTrades.get(0).dateTime.timeInMillis
        calendarViewDate.setOnDateChangeListener(object : OnDateChangeListener {
            override fun onSelectedDayChange(view: CalendarView, year: Int, month: Int, dayOfMonth: Int) {
                view.date = DateTime(year, month, dayOfMonth).timeInMillis
            }
        })
        val editTextCode = view.findViewById<EditText>(R.id.editText_code)
        val editTextMoney = view.findViewById<EditText>(R.id.editText_money)
        editTextCode.setText(position.name)
        editTextCode.isEnabled = false
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                try {
                    val money = editTextMoney.text.toString().toBigDecimal()

                    //
                    val selectDate = DateTime(calendarViewDate.date)
                    val trdt = DateTime()
                    trdt[Calendar.YEAR] = selectDate[Calendar.YEAR]
                    trdt[Calendar.DAY_OF_YEAR] = selectDate[Calendar.DAY_OF_YEAR]

                    // 成本总资金
                    val costFund = position.cost * (position.amount * 100).toBigDecimal()
                    val amount = position.amount
                    val cost = (costFund - money).setMyScale() / (amount * 100).toBigDecimal()
                    position.cost = cost
                    dc.editPosition(position)

                    dc.addTrade(Trade(trdt, position.code, position.name, money, position.amount, 2, 0, cost, position.amount))

                    positions = dc.allPositions
                    adapter.notifyDataSetChanged()
                    dialog.dismiss()
                } catch (e: NumberFormatException) {
                    Snackbar.make(editTextCode, "成本价必须为数字", Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    _Utils.printException(this@StockPositionHistoryActivity, e)
                }
            }
        })
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "取消", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.dismiss()
            }
        })
        dialog.show()
    }

    /**
     * 加仓
     */
    fun buyMoreDialog(position: Position) {
        val view = View.inflate(this, R.layout.inflate_dialog_add_trade, null)
        val dialog = AlertDialog.Builder(this).create()
        dialog.setView(view)
        dialog.setCancelable(false)
        val calendarViewDate = view.findViewById<CalendarView>(R.id.calendarView_date)
//        val historyTrades = dataContext.getTrades(position.code)
//        if (historyTrades.size != 0)
//            calendarViewDate.date = historyTrades.get(0).dateTime.timeInMillis
        calendarViewDate.date = DateTime().timeInMillis
        calendarViewDate.setOnDateChangeListener(object : OnDateChangeListener {
            override fun onSelectedDayChange(view: CalendarView, year: Int, month: Int, dayOfMonth: Int) {
                view.date = DateTime(year, month, dayOfMonth).timeInMillis
            }
        })
        val editTextCode = view.findViewById<EditText>(R.id.editText_code)
        val editTextCost = view.findViewById<EditText>(R.id.editText_cost)
        val editTextAmount = view.findViewById<EditText>(R.id.editText_amount)
        editTextCode.setText(position.name)
        editTextCode.isEnabled = false
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "加仓", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                try {
                    val tradeAmount = editTextAmount.text.toString().toInt()
                    val tradePrice = editTextCost.text.toString().toBigDecimal()

                    //
                    val selectDate = DateTime(calendarViewDate.date)
                    val trdt = DateTime()
                    trdt[Calendar.YEAR] = selectDate[Calendar.YEAR]
                    trdt[Calendar.DAY_OF_YEAR] = selectDate[Calendar.DAY_OF_YEAR]

                    // 成本总资金
                    val costFund = position.cost * (position.amount * 100).toBigDecimal()
                    // 成交总资金
                    val tradeFund = tradePrice * (tradeAmount * 100).toBigDecimal()
                    // 佣金
                    val commission = commission(tradePrice, tradeAmount)
                    // 过户费
                    val transferFee = transferFee(tradePrice, tradeAmount)
                    val amount = position.amount + tradeAmount
                    val cost = (costFund + tradeFund + commission + transferFee).setMyScale() / (amount * 100).toBigDecimal()
                    position.cost = cost
                    position.amount = amount
                    dc.editPosition(position)

                    dc.addTrade(Trade(trdt, position.code, position.name, tradePrice, tradeAmount, 1, 0, cost, position.amount))

                    positions = dc.allPositions
                    adapter.notifyDataSetChanged()
                    dialog.dismiss()
                } catch (e: NumberFormatException) {
                    Snackbar.make(editTextCode, "成本价必须为数字", Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    _Utils.printException(this@StockPositionHistoryActivity, e)
                }
            }
        })
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "取消", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.dismiss()
            }
        })
        dialog.show()
    }

    /**
     * 减仓
     */
    fun saleSomeDialog(position: Position) {
        val view = View.inflate(this, R.layout.inflate_dialog_add_trade, null)
        val dialog = AlertDialog.Builder(this).create()
        dialog.setView(view)
        dialog.setCancelable(false)
//        val historyTrades = dataContext.getTrades(position.code)
//        if (historyTrades.size == 0) return
//        calendarViewDate.date = historyTrades.get(0).dateTime.timeInMillis
        val calendarViewDate = view.findViewById<CalendarView>(R.id.calendarView_date)
        calendarViewDate.date = DateTime().timeInMillis
        calendarViewDate.setOnDateChangeListener(object : OnDateChangeListener {
            override fun onSelectedDayChange(view: CalendarView, year: Int, month: Int, dayOfMonth: Int) {
                view.date = DateTime(year, month, dayOfMonth).timeInMillis
            }
        })
        val editTextCode = view.findViewById<EditText>(R.id.editText_code)
        val editTextCost = view.findViewById<EditText>(R.id.editText_cost)
        val editTextAmount = view.findViewById<EditText>(R.id.editText_amount)
        editTextCode.setText(position.name)
        editTextCode.isEnabled = false
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "减仓", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                try {
                    val tradeAmount = editTextAmount.text.toString().toInt()
                    val tradePrice = editTextCost.text.toString().toBigDecimal()

                    //
                    val selectDate = DateTime(calendarViewDate.date)
                    val trdt = DateTime()
                    trdt[Calendar.YEAR] = selectDate[Calendar.YEAR]
                    trdt[Calendar.DAY_OF_YEAR] = selectDate[Calendar.DAY_OF_YEAR]

                    // 成本总资金
                    val costFund = position.cost * (position.amount * 100).toBigDecimal()
                    // 成交总资金
                    val tradeFund = tradePrice * (tradeAmount * 100).toBigDecimal()
                    // 佣金
                    val commission = commission(tradePrice, tradeAmount)
                    // 过户费
                    val transferFee = transferFee(tradePrice, tradeAmount)
                    // 印花税
                    val tax = tax(-1, tradePrice, tradeAmount)
                    val profit = ((tradePrice - position.cost) * (tradeAmount * 100).toBigDecimal()) - commission - transferFee - tax
                    val amount = position.amount - tradeAmount
                    var cost: BigDecimal
                    if (amount == 0) {
                        cost = 0.toBigDecimal()
                    } else {
                        cost = ((costFund - tradeFund) + commission + transferFee + tax).setMyScale() / (amount * 100).toBigDecimal()
                    }
                    position.cost = cost
                    position.profit = position.profit + profit
                    position.amount = amount
                    dc.editPosition(position)

                    dc.addTrade(Trade(trdt, position.code, position.name, tradePrice, tradeAmount, -1, if (amount == 0) -1 else 0, cost, position.amount))


                    positions = dc.allPositions
                    adapter.notifyDataSetChanged()
                    dialog.dismiss()
                } catch (e: NumberFormatException) {
                    Snackbar.make(editTextCode, "成本价必须为数字", Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    _Utils.printException(this@StockPositionHistoryActivity, e)
                }
            }
        })
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "取消", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.dismiss()
            }
        })
        dialog.show()
    }

    /**
     * 修正成本价
     */
    fun modifyCoseDialog(position: Position) {
        val view = View.inflate(this, R.layout.inflate_dialog_stock_cost, null)
        val dialog = AlertDialog.Builder(this).create()
        dialog.setView(view)
        dialog.setCancelable(false)
        val editTextCost = view.findViewById<EditText>(R.id.editText_cost)
        val editTextCode = view.findViewById<EditText>(R.id.editText_code)
        editTextCode.setText(position.name)
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "修正", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                try {
                    val cost = editTextCost.text.toString().toBigDecimal()
                    position.cost = cost
                    dc.editPosition(position)
                    positions = dc.allPositions
                    adapter.notifyDataSetChanged()
                    dialog.dismiss()
                } catch (e: NumberFormatException) {
                    Snackbar.make(editTextCost, "成本价必须为数字", Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    _Utils.printException(this@StockPositionHistoryActivity, e)
                }
            }
        })
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "取消", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.dismiss()
            }
        })
        dialog.show()
    }

    fun addStockDialog() {
        val view = View.inflate(this, R.layout.inflate_dialog_add_stock, null)
        val dialog = AlertDialog.Builder(this).create()
        dialog.setView(view)
        dialog.setTitle("股票信息")
        dialog.setCancelable(false)
        val editTextCode = view.findViewById<EditText>(R.id.editText_code)
        editTextCode.inputType = InputType.TYPE_CLASS_NUMBER
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "取消", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.dismiss()
            }
        })
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "新增", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                try {
                    val info = StockInfo()
                    info.code = editTextCode.text.toString()
                    if (dc.getPosition(info.code) != null) return
                    info.exchange = if (info.code[0] == '6') "sh" else "sz"
                    fillStockInfo(info, object : StockPositionActivity.OnLoadFinishedListener {
                        override fun onLoadFinished() {
                            if (info.name.isEmpty()) {
                                Snackbar.make(editTextCode, "股票代码不存在", Snackbar.LENGTH_SHORT).show()
                                return
                            }
                            dc.addPosition(Position(info.code, info.name, 0.toBigDecimal(), 0, 0, info.exchange, 0.toBigDecimal()))
                            positions = dc.allPositions
                            adapter.notifyDataSetChanged()
                            dialog.dismiss()
                        }
                    })
                } catch (e: NumberFormatException) {
                    Snackbar.make(editTextCode, "成本价必须为数字", Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    _Utils.printException(this@StockPositionHistoryActivity, e)
                }
            }
        })
        dialog.show()
    }

    /**
     * 向info里面填充StockInfo值，在调用之前，需要info赋值code。
     *
     * @param info
     * @param onLoadFinishedListener
     */
    private fun fillStockInfo(info: StockInfo, onLoadFinishedListener: StockPositionActivity.OnLoadFinishedListener?) {

        //http://hq.sinajs.cn/list=sh601555
        //var hq_str_sh601555="东吴证券,11.290,11.380,11.160,11.350,11.050,11.160,11.170,61431561,687740501.000,3500,11.160,144700,11.150,98500,11.140,78500,11.130,99200,11.120,143700,11.170,99700,11.180,28700,11.190,41500,11.200,41500,11.210,2019-04-17,15:00:00,00";
        Thread(object : Runnable {
            override fun run() {
                try {
                    val url = "https://hq.sinajs.cn/list=" + info.exchange + info.code
                    val client = _OkHttpUtil.client
                    val request = Request.Builder().url(url).build()
                    var response: Response? = null
                    response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        try {
                            val sss = response.body!!.string()
                            val result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",".toRegex()).toTypedArray()
                            if (result.size == 0) {
                                return
                            }
                            if (result.size <= 1) return
                            val open = result[2].toBigDecimal()
                            info.name = result[0]
                            info.price = result[3].toBigDecimal()
                            info.increase = (info.price - open) / open
                            info.time = result[31]
                            runOnUiThread(object : Runnable {
                                override fun run() {
                                    onLoadFinishedListener?.onLoadFinished()
                                }
                            })
                        } catch (e: Exception) {
                            return
                        }
                    } else {
                        dc.addRunLog("StockPositionHistoryActivity", "获取数据失败...", "")
                    }
                } catch (e: Exception) {
                }
            }
        }).start()
    }

    protected inner class PositionListdAdapter() : BaseAdapter() {

        override fun getCount(): Int {
            return positions.size
        }

        override fun getItem(position: Int): Any {
            return positions.get(position)
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun notifyDataSetChanged() {
            super.notifyDataSetChanged()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var cView = convertView
            try {
                if (mHashMap.get(position) == null) {
                    cView = View.inflate(this@StockPositionHistoryActivity, R.layout.inflate_position, null)
                    mHashMap.put(position, cView)
                    val stock = positions[position]
                    val textViewCode = cView.findViewById<TextView>(R.id.textView_code)
                    val textViewName = cView.findViewById<TextView>(R.id.tv_name)
                    val textViewCost = cView.findViewById<TextView>(R.id.textView_cost)
                    val textViewAmount = cView.findViewById<TextView>(R.id.tv_amount)
                    val textViewProfit = cView.findViewById<TextView>(R.id.textView_profit)
                    val layout_trade = cView.findViewById<LinearLayout>(R.id.layout_trade)

                    var format = DecimalFormat("#,##0.00")
                    if (stock.amount > 0) {
                        layout_trade.visibility = View.VISIBLE
                        val trades = dc.getKeepTrades(stock.code)
                        e("trades size : " + trades.size)

                        trades.forEach {
                            val view = View.inflate(this@StockPositionHistoryActivity, R.layout.inflate_list_item_position_trade, null)
                            val tvAttDate = view.findViewById<TextView>(R.id.tv_att_date)
                            val tvAttType = view.findViewById<TextView>(R.id.tv_att_type)
                            val tvAttPrice = view.findViewById<TextView>(R.id.tv_att_price)
                            val tvAttAmount = view.findViewById<TextView>(R.id.tv_att_amount)
                            var tvAttIncrease = view.findViewById<TextView>(R.id.tv_att_increase)

                            tvAttDate.text = it.dateTime.toShortDateString1()
                            tvAttType.text = if (it.type == 1) "买入" else "卖出"
                            tvAttPrice.text = format.format(it.price)
                            tvAttAmount.text = (it.amount * 100).toString()
                            attTrades.add(AttTrades(it.code, it.price, it.amount, it.type, tvAttIncrease))

                            layout_trade.addView(view)
                        }
                    } else {
                        layout_trade.visibility = View.GONE
                        textViewCost.visibility = View.GONE
                        textViewAmount.visibility = View.GONE
                    }

                    textViewCode.text = stock.code
                    textViewName.text = stock.name
                    format = DecimalFormat("#,##0")
                    textViewProfit.text = format.format(stock.profit)
                    format = DecimalFormat("#,##0.000")
                    textViewCost.text = format.format(stock.cost)
                    format = DecimalFormat("#0")
                    textViewAmount.text = format.format(stock.amount * 100)
                } else {
                    cView = mHashMap.get(position)
                }
            } catch (e: Exception) {
                _Utils.printException(this@StockPositionHistoryActivity, e)
            }
            return cView!!
        }
    }

    private val mHashMap = HashMap<Int, View>()
    private var attTrades: MutableList<AttTrades>
    private var timer = Timer()

    data class AttTrades(
        var code: String,
        var price: BigDecimal,
        var amount: Int,
        var type: Int,
        var tvIncrease: TextView
    )

    init {
        attTrades = ArrayList()
    }
}