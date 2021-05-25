package com.wang17.myphone.fragment

import android.app.ProgressDialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.wang17.myphone.R
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.database.Position
import com.wang17.myphone.database.Statement
import com.wang17.myphone.database.Trade
import com.wang17.myphone.e
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.Stock
import com.wang17.myphone.toMyDecimal
import com.wang17.myphone.util.TradeUtils
import com.wang17.myphone.util._SinaStockUtils
import kotlinx.android.synthetic.main.fragment_dialog_chart.*
import lecho.lib.hellocharts.model.*
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList


class ChartFragment : Fragment() {
    private val hasAxes = true
    private val hasAxesNames = true // 横竖行的名字

    private val hasLines = true
    private val hasPoints = false
    private val shape = ValueShape.CIRCLE
    private val isFilled = false
    private val hasLabels = false // 是否显示点的数据

    private val isCubic = false
    private val hasLabelForSelected = false
    private val pointsHaveDifferentColor = false

    private var data: LineChartData? = null
    private val numberOfLines = 1 // 只显示一行数据

    private val maxNumberOfLines = 1 // 如果为4则表示最多显示4行，1表示只有一行数据

    private val numberOfPoints = 10 // 每行数据有多少个点

    private lateinit var runHandler: Handler

    // 存储数据
    var randomNumbersTab = Array(maxNumberOfLines) { FloatArray(numberOfPoints) }

    private lateinit var dc: DataContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dc = DataContext(context)
        progressDialog = ProgressDialog(context)
        activity!!.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dialog_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runHandler = Handler()
        generateData()
        fab_load.setOnLongClickListener {
//            Thread{
            checkUpdateChartData()
            generateData()
//            }.start()
            true
        }
        button2.setOnLongClickListener {
            dc.deleteAllStatements()
            true
        }
    }

    var axisXValues: MutableList<AxisValue> = ArrayList()
    private fun generateData() {

        val format = DecimalFormat("#,##0.00")
        val statements = dc.statements
        if (statements.size == 0)
            return

        val firstStatement = statements.first()
        val lastStatement = statements.last()

        val whiteColor = Color.BLACK
        val axisColor = resources.getColor(R.color.black_overlay, null)
//        generateValues()
        val values: MutableList<PointValue> = ArrayList()
        axisXValues.clear()
        var cc = 0.toBigDecimal()
        val fund = statements.last().fund
        var count = 0
        var aa = statements.size / 20


        for (i in statements.indices) {
            if (statements[i].profit.compareTo(0.toBigDecimal()) != 0) {
                cc += statements[i].profit * 100.toBigDecimal() / fund
                e("${statements[i].date.toShortDateString()}   ${format.format(statements[i].profit * 100.toBigDecimal() / fund)}   ${format.format(cc)}")
                values.add(PointValue((++count).toFloat(), cc.toFloat()))
//                if(count%aa==0)
//                axisXValues.add(AxisValue(count.toFloat(),statements[i].date.toShortDateString().toCharArray()))
            }
        }
        val line = Line(values)
        //line.setColor(ChartUtils.COLORS[i]); // 多条数据时选择这个即可
        line.setColor(whiteColor) // 定制线条颜色
        line.setShape(shape)
        line.setCubic(false)
        line.setFilled(isFilled)
        line.setHasLabels(false)
        line.setHasLabelsOnlyForSelected(true)
        line.setHasLines(hasLines)
        line.setHasPoints(false)
        line.strokeWidth = 1
        if (pointsHaveDifferentColor) {
            //多条数据时选择这个即可
            //line.setPointColor(ChartUtils.COLORS[(i + 1) % ChartUtils.COLORS.length]);
            line.setPointColor(whiteColor)
        }

        val lines: MutableList<Line> = ArrayList<Line>()
        lines.add(line)

        var data = LineChartData(lines)
        if (hasAxes) {
            val axisX = Axis()
            val axisY = Axis().setHasLines(true)
            if (hasAxesNames) {
                axisX.setName("${firstStatement.date.toShortDateString()} - ${lastStatement.date.toShortDateString()}")
                axisX.setTextColor(whiteColor)
                axisX.setLineColor(axisColor)
                axisY.setName("")
                axisY.setTextColor(whiteColor)
                axisY.setLineColor(axisColor)
            }
            data.setAxisXBottom(axisX)
            data.setAxisYLeft(axisY)
        } else {
            data.setAxisXBottom(null)
            data.setAxisYLeft(null)
        }
        data.setBaseValue(Float.NEGATIVE_INFINITY)
//        runHandler.post{
        chart.setLineChartData(data)
//        }
    }

    lateinit var progressDialog: ProgressDialog
    private fun checkUpdateChartData() {

        val now = DateTime()
        var trades = dc.trades.reversed()
        if (trades.size == 0)
            return
        val firstTrade: Trade = trades.first()

        var chartDate = firstTrade.dateTime
        val ss = dc.statements
        if (ss.size > 0) {
            chartDate = ss.last().date.addDays(1).date
            trades = dc.getTrades(chartDate).reversed()
        }

        if ((chartDate.timeInMillis < now.date.timeInMillis || (chartDate.isSameDay(now) && now.hour > 15))
            && now.get(Calendar.DAY_OF_WEEK) != 6 && now.get(Calendar.DAY_OF_WEEK) != 0 ) {

            val positions: MutableList<Position> = ArrayList()
            val statements: MutableList<Statement> = ArrayList()

            val dateList = _SinaStockUtils.getStockHistory(firstTrade.code, chartDate)
            val stocks: MutableList<Stock> = ArrayList()

            var prvProfit = 0.toBigDecimal()
            var reset = true
            dateList.forEach { dt ->
                e(dt.date.toShortDateString())
                /**
                 * 1、买入、卖出、股息收入、股税支出
                 *      更新持仓成本和持仓量。
                 *
                 * 2、持仓
                 *      根据持仓价和当天收盘价，计算盈利，并更新资金。
                 *
                 * 3、结算信息：当前资金、盈利、
                 *
                 */

                var profit = 0.toBigDecimal()
                var fund = 0.toBigDecimal()

                val tds = trades.filter { it.dateTime.isSameDay(dt.date) }
                tds.forEach { td ->
                    when (td.type) {
                        1 -> { //买入
                            var position = positions.firstOrNull { it.code.trim() == td.code.trim() }
                            if (position != null) {
                                // 成本总资金
                                val costFund = position.cost * (position.amount * 100).toBigDecimal()
                                // 成交总资金
                                val tradeFund = td.price * (td.amount * 100).toBigDecimal()
                                // 佣金
                                val commission = TradeUtils.commission(td)
                                // 过户费
                                val transferFee = TradeUtils.transferFee(td)
                                // 印花税
                                val tax = TradeUtils.tax(td)

                                val amount = position.amount + td.amount
                                val cost = (costFund + tradeFund + commission + transferFee + tax) / (amount * 100).toBigDecimal()

                                position.amount = amount
                                position.cost = cost

                            } else {
                                // 成交总资金
                                val tradeFund = td.price * (td.amount * 100).toBigDecimal()
                                // 佣金
                                val commission = TradeUtils.commission(td)
                                // 过户费
                                val transferFee = TradeUtils.transferFee(td)
                                // 印花税
                                val tax = TradeUtils.tax(td)

                                val cost = (tradeFund + commission + transferFee + tax) / (td.amount * 100).toBigDecimal()

                                position = Position(td.code, td.name, cost, 0, td.amount, "", 0.toBigDecimal())
                                positions.add(position)
                            }

                        }
                        -1 -> { //卖出

                            var position = positions.firstOrNull { it.code.trim() == td.code.trim() }
                            if (position != null) {
                                // 成本总资金
                                val costFund = position.cost * (position.amount * 100).toBigDecimal()
                                // 成交总资金
                                val tradeFund = td.price * (td.amount * 100).toBigDecimal()
                                // 佣金
                                val commission = TradeUtils.commission(td)
                                // 过户费
                                val transferFee = TradeUtils.transferFee(td)
                                // 印花税
                                val tax = TradeUtils.tax(td)

                                val amount = position.amount - td.amount
                                if (amount == 0) {
                                    positions.remove(position)
                                } else {
                                    val cost = (costFund - tradeFund + commission + transferFee + tax) / (amount * 100).toBigDecimal()
                                    position.amount = amount
                                    position.cost = cost
                                }
                                profit += (td.price - position.cost) * (td.amount * 100).toBigDecimal() - commission - transferFee - tax
                            }
                        }
                        2 -> { //股息

                            var position = positions.firstOrNull { it.code.trim() == td.code.trim() }
                            if (position != null) {
                                var cost = (position.cost * (position.amount * 100).toMyDecimal() - td.price) / (position.amount * 100).toBigDecimal()
                                position.cost = cost
                            }

                            profit += td.price

                        }
                        -2 -> { //股息税
                            profit -= td.price
                        }
                    }
                }

//            e("position size : ${positions.size}")
//            e("-----------------------${dt.date.toShortDateString()} ${positions.size}--------------------------")
                if (positions.size > 0) {
                    positions.forEach { p ->
//                    e("${p.code} ${p.name} ${p.amount} ${format.format(p.cost)}")
                        var stock = stocks.firstOrNull { stock -> stock.code.trim() == p.code.trim() && stock.date.isSameDay(dt.date) }
                        if (stock == null) {
                            stocks.addAll(_SinaStockUtils.getStockHistory(p.code, dt.date))
                            stock = stocks.firstOrNull { stock -> stock.code.trim() == p.code.trim() && stock.date.isSameDay(dt.date) }
                        }

                        stock?.let {
                            fund += it.price * (p.amount * 100).toBigDecimal()
                            profit += (it.price - p.cost) * (p.amount * 100).toBigDecimal()
//                    e("${it.code}  ${format.format(it.price)}  ${format.format(p.cost)} ${p.amount} ${format.format((it.price - p.cost) * (p.amount * 100).toBigDecimal())}")
                        }
                    }

                    if (reset)
                        statements.add(Statement(dt.date, fund, profit))
                    else
                        statements.add(Statement(dt.date, fund, profit - prvProfit))

                    prvProfit = profit
                    reset = false
                } else {
                    statements.add(Statement(dt.date, fund, 0.toBigDecimal()))
                    reset = true
                }
            }
            e("生成图表用时：${System.currentTimeMillis() - now.timeInMillis}")
            dc.addStatements(statements)
            Toast.makeText(context, "更新数据用时：${System.currentTimeMillis() - - now.timeInMillis}毫秒", Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(context, "没有新数据", Toast.LENGTH_LONG).show()
        }

    }

}