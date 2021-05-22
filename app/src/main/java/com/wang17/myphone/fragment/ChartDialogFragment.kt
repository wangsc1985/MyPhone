package com.wang17.myphone.fragment

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.wang17.myphone.R
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.database.Position
import com.wang17.myphone.database.Statement
import com.wang17.myphone.database.Trade
import com.wang17.myphone.e
import com.wang17.myphone.toMyDecimal
import com.wang17.myphone.util.TradeUtils
import com.wang17.myphone.util._SinaStockUtils
import kotlinx.android.synthetic.main.fragment_dialog_chart.*
import lecho.lib.hellocharts.model.*
import java.text.DecimalFormat


class ChartDialogFragment : Fragment() {
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

    // 存储数据
    var randomNumbersTab = Array(maxNumberOfLines) { FloatArray(numberOfPoints) }

    private lateinit var dc: DataContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dc = DataContext(context)
        activity!!.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dialog_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        generateData()
    }

    private fun generateData() {

        val statements = createChartData()


        val whiteColor = resources.getColor(R.color.black_overlay)
//        generateValues()
        val values: MutableList<PointValue> = ArrayList()
        var cc = 0.toBigDecimal()
        val fund = statements.last().fund
        for (i in statements.indices) {
            cc += statements[i].profit * 100.toBigDecimal() / fund
            values.add(PointValue(i.toFloat(), cc.toFloat()))
        }
        val line = Line(values)
        //line.setColor(ChartUtils.COLORS[i]); // 多条数据时选择这个即可
        line.setColor(whiteColor) // 定制线条颜色
        line.setShape(shape)
        line.setCubic(isCubic)
        line.setFilled(isFilled)
        line.setHasLabels(hasLabels)
        line.setHasLabelsOnlyForSelected(hasLabelForSelected)
        line.setHasLines(hasLines)
        line.setHasPoints(hasPoints)
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
            val axisY: Axis = Axis().setHasLines(true)
            if (hasAxesNames) {
                axisX.setName("最近10次考试成绩")
                axisY.setName("")
                axisX.setTextColor(whiteColor)
                axisY.setTextColor(whiteColor)
                axisY.setLineColor(whiteColor)
                axisX.setLineColor(whiteColor)
            }
            data.setAxisXBottom(axisX)
            data.setAxisYLeft(axisY)
        } else {
            data.setAxisXBottom(null)
            data.setAxisYLeft(null)
        }
        data.setBaseValue(Float.NEGATIVE_INFINITY)
        chart.setLineChartData(data)
    }

    private fun createChartData(): List<Statement> {
        val format = DecimalFormat("#,##0.00")
        val dt = System.currentTimeMillis()
        val allTrades = dc.trades.reversed()
        if (allTrades.size == 0)
            return ArrayList()
        val positions: MutableList<Position> = ArrayList()
        val statements: MutableList<Statement> = ArrayList()
        val firstTrade: Trade = allTrades.first()

        val dateList = _SinaStockUtils.getStockHistory(firstTrade.code, firstTrade.dateTime)
        val stocks = _SinaStockUtils.getStockHistory(firstTrade.code, firstTrade.dateTime)

        var prvProfit = 0.toBigDecimal()
        var reset = true
        dateList.forEach { dt ->
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

            val tds = allTrades.filter { it.dateTime.isSameDay(dt.date) }
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
        e("生成图表用时：${System.currentTimeMillis() - dt}")
        return statements
    }

}