package com.wang17.myphone.fragment

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.wang17.myphone.R
import com.wang17.myphone.database.*
import com.wang17.myphone.e
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.Stock
import com.wang17.myphone.toMyDecimal
import com.wang17.myphone.util.TradeUtils
import com.wang17.myphone.util._SinaStockUtils
import kotlinx.android.synthetic.main.fragment_chart.*
import lecho.lib.hellocharts.model.*
import okhttp3.internal.format
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList


class ChartFragment : Fragment() {
    private val hasAxes = true
    private val hasAxesNames = true // 横竖行的名字

    private val hasLines = true
    private val hasPoints = false
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
    val format = DecimalFormat("#,##0.00")

    // 存储数据
    var randomNumbersTab = Array(maxNumberOfLines) { FloatArray(numberOfPoints) }

    private lateinit var dc: DataContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dc = DataContext(context)
        activity!!.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runHandler = Handler()
        generateChart()
        checkChartData()
    }

    var axisXValues: MutableList<AxisValue> = ArrayList()
    private fun generateChart() {
        val statements = dc.getStatements(DateTime(dc.getSetting(Setting.KEYS.chart_start_year,2020).int,0,1))
        if (statements.size == 0)
            return

        val firstStatement = statements.first()
        val lastStatement = statements.last()

        val lineColor =  resources.getColor(R.color.timerStopColor, null)
        val axisColor = resources.getColor(R.color.timerPauseColor, null)
        val values: MutableList<PointValue> = ArrayList()
        axisXValues.clear()
        var totalProfit = 0.toBigDecimal()
        val fund = statements.last().fund
        var count = 0
        var prvMonth = -1
        var prvYear = -1

        for (i in statements.indices) {
            val statement = statements[i]
            if (statement.profit.compareTo(0.toBigDecimal()) != 0) {
                totalProfit += statement.profit * 100.toBigDecimal() / fund
                values.add(PointValue((++count).toFloat(), totalProfit.toFloat()))
                if (statement.date.month != prvMonth) {
                    var str = ""
                    if (statement.date.year != prvYear) {
                        if (prvYear != -1)
                            str = "${statement.date.year.toString().substring(2, 4)}年"
                        else
                            str = ""
                        prvYear = statement.date.year
                    } else {
                        str = "${statement.date.monthStr}月"
                    }
                    axisXValues.add(AxisValue(count.toFloat(), str.toCharArray()))
                    prvMonth = statement.date.month
                }
            }
        }

        val line = Line(values)
        //line.setColor(ChartUtils.COLORS[i]); // 多条数据时选择这个即可
        line.setColor(lineColor) // 定制线条颜色
        line.setShape(ValueShape.CIRCLE)//折线图上每个数据点的形状  这里是圆形 （有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.DIAMOND）
        line.setCubic(false) //曲线是否平滑，即是曲线还是折线
        line.setFilled(dc.getSetting(Setting.KEYS.is_fill_line_area,true).boolean)//是否填充曲线的面积
        line.setHasLabels(false)//曲线的数据坐标是否加上备注
        line.setHasLabelsOnlyForSelected(true)//点击数据坐标提示数据（设置了这个line.setHasLabels(true);就无效）
        line.setHasLines(true)//是否用线显示。如果为false 则没有曲线只有点显示
        line.setHasPoints(false)//是否显示圆点 如果为false 则没有原点只有点显示（每个数据点都是个大的圆点）
        line.strokeWidth = 1
        if (pointsHaveDifferentColor) {
            //多条数据时选择这个即可
            //line.setPointColor(ChartUtils.COLORS[(i + 1) % ChartUtils.COLORS.length]);
            line.setPointColor(lineColor)
        }

        val lines: MutableList<Line> = ArrayList<Line>()
        lines.add(line)

        var data = LineChartData(lines)
        if (hasAxes) {
            val axisX = Axis().setValues(axisXValues).setHasLines(true)
            val axisY = Axis().setHasLines(true)
            if (hasAxesNames) {
                axisX.setName("${firstStatement.date.toShortDateString()} 至 ${lastStatement.date.toShortDateString()}  今日：${format.format(lastStatement.profit*100.toBigDecimal()/lastStatement.fund)}%  总计：${format.format(totalProfit)}%")
                axisX.setTextColor(axisColor)
                axisX.setLineColor(axisColor)
                axisX.setMaxLabelChars(7); //最多几个X轴坐标，意思就是你的缩放让X轴上数据的个数7<=x<=mAxisXValues.length
                axisY.setName("")
                axisY.setTextColor(axisColor)
                axisY.setLineColor(axisColor)
            }
            data.setAxisXBottom(axisX)
            data.setAxisYLeft(axisY)
            data.setAxisYRight(axisY)
        } else {
            data.setAxisXBottom(null)
            data.setAxisYLeft(null)
        }
        data.setBaseValue(Float.NEGATIVE_INFINITY)
//        runHandler.post{
        chart.setLineChartData(data)
//        }
    }

    private fun checkChartData() {
        val ss = dc.lastStatement

        ss?.let {
            val now = DateTime()
            val chartDate = it.date.addDays(1).date
            if ((chartDate.timeInMillis < now.date.timeInMillis || (chartDate.isSameDay(now) && now.hour > 15))
                && now.get(Calendar.DAY_OF_WEEK) != 6 && now.get(Calendar.DAY_OF_WEEK) != 0
            ) {
                loadChartData()
                generateChart()
            }
        }
    }

    private fun loadChartData() {

        val format = DecimalFormat("#,##0.00")
        val now = DateTime()
        var positions: MutableList<Position> = ArrayList()
        val statements: MutableList<Statement> = ArrayList()
        var trades = dc.trades.reversed()
        if (trades.size == 0)
            return
        val firstTrade: Trade = trades.first()

        var prvProfit = 0.toBigDecimal()
//        var reset = true
        var chartDate = firstTrade.dateTime
        val ss = dc.statements
        if (ss.size > 0) {
            chartDate = ss.last().date.addDays(1).date
            trades = dc.getTrades(chartDate).reversed()
            positions = dc.positions
            prvProfit = ss.last().totalProfit
//            reset = positions.size == 0
        }

        val dateList = _SinaStockUtils.getStockHistory(firstTrade.code, chartDate)
        val stocks: MutableList<Stock> = ArrayList()

        dateList.forEach { dt ->

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
                        e("${it.code}  ${format.format(it.price)}  ${format.format(p.cost)} ${p.amount} ${format.format((it.price - p.cost) * (p.amount * 100).toBigDecimal())}")
                    }
                }

//                if (reset)
//                    statements.add(Statement(dt.date, fund, profit, profit))
//                else
                    statements.add(Statement(dt.date, fund, profit - prvProfit, profit))

                prvProfit = profit
//                reset = false
            } else {
                statements.add(Statement(dt.date, 0.toBigDecimal(), 0.toBigDecimal(), 0.toBigDecimal()))
                prvProfit = 0.toBigDecimal()
//                reset = true
            }
        }
        e("生成图表用时：${System.currentTimeMillis() - now.timeInMillis}")
        dc.addStatements(statements)
        Toast.makeText(context, "更新数据用时：${System.currentTimeMillis() - -now.timeInMillis}毫秒", Toast.LENGTH_LONG).show()
    }

}