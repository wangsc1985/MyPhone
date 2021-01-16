package com.wang17.myphone.util

import android.graphics.Color
import com.wang17.myphone.activity.StockPositionHistoryActivity.AttTrades
import com.wang17.myphone.model.StockInfo
import com.wang17.myphone.model.database.Position
import com.wang17.myphone.service.StockService.Companion.findCommodity
import okhttp3.Request
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.*

object _SinaStockUtils {
    fun getStockInfoList(attTrades: List<AttTrades>) {
        Thread(Runnable {
            try {
                val format = DecimalFormat("#,##0.00")
                for ((code, tradePrice, type, tvPrice2) in attTrades) {
                    val url = "https://hq.sinajs.cn/list=" + (if (code.startsWith("6")) "sh" else "sz") + code
                    val client = _OkHttpUtil.client
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body!!.string()
                        val result = body.substring(body.indexOf("\"")).replace("\"", "").split(",".toRegex()).toTypedArray()
                        val newPrice = result[3].toBigDecimal()
                        tvPrice2.text = "${format.format(Math.abs((newPrice-tradePrice).toDouble()))}  ${format.format(Math.abs((newPrice-tradePrice).toDouble()).toBigDecimal()*100.toBigDecimal()/tradePrice)}%"
                        if (type.toBigDecimal() * (tradePrice - newPrice) < 0.toBigDecimal()) {
                            tvPrice2.setTextColor(Color.RED)
                        } else if (type.toBigDecimal() * (tradePrice - newPrice) > 0.toBigDecimal()) {
                            tvPrice2.setTextColor(Color.CYAN)
                        } else {
                            tvPrice2.setTextColor(Color.WHITE)
                        }
                    } else {
                        _LogUtils.log2file("获取数据失败...")
                        return@Runnable
                    }
                }
            } catch (e: Exception) {
                _Utils.getExceptionStr(e)
            }
        }).start()
    }

    @Throws(Exception::class)
    fun getStockInfoList(positionCodes: List<String>, onLoadStockInfoListListener: OnLoadStockInfoListListener1?) {
        Thread(Runnable {
            try {
                val map: MutableMap<String, Double> = HashMap()
                for (code in positionCodes) {
                    val url = "https://hq.sinajs.cn/list=" + (if (code.startsWith("6")) "sh" else "sz") + code
                    val client = _OkHttpUtil.client
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body!!.string()
                        val result = body.substring(body.indexOf("\"")).replace("\"", "").split(",".toRegex()).toTypedArray()
                        map[code] = result[3].toDouble()
                    } else {
                        _LogUtils.log2file("获取数据失败...")
                        return@Runnable
                    }
                }
                onLoadStockInfoListListener?.onLoadFinished(map)
            } catch (e: Exception) {
                _Utils.getExceptionStr(e)
            }
        }).start()
    }

    @JvmStatic
    @Throws(Exception::class)
    fun getStockInfoList(positions: List<Position>, onLoadStockInfoListListener: OnLoadStockInfoListListener?) {
        Thread(Runnable {
            var isStock = true
            var totalProfit = 0.0.toBigDecimal()
            var totalCostFund = 0.0.toBigDecimal()
            var time = ""
            val stockInfoList: MutableList<StockInfo> = ArrayList()
            try {
                for (position in positions) {
//                        final Stock stock = stock;
                    val url = "https://hq.sinajs.cn/list=" + position.exchange + position.code
                    val client = _OkHttpUtil.client
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body!!.string()
                        val result = body.substring(body.indexOf("\"")).replace("\"", "").split(",".toRegex()).toTypedArray()
                        val info = StockInfo()
                        var profit = 0.0.toBigDecimal()
                        var increase=0.0.toBigDecimal()
                        if (position.type == 0) {
                            /**
                             * 股票列表
                             */
                            isStock = true
                            val open = result[2].toDouble()
                            info.name = result[0]
                            info.price = result[3].toBigDecimal()

                            val fee = TradeUtils.commission(info.price, position.amount) + TradeUtils.tax(-1, info.price, position.amount) + TradeUtils.transferFee(info.price, position.amount)
                            info.increase =(info.price - position.cost) / position.cost
                            info.time = result[31]
                            profit = (info.price - position.cost)*(position.amount*100).toBigDecimal() -fee
                            increase=profit/ (position.cost*(position.amount*100).toBigDecimal())
                            totalProfit += profit
                           totalCostFund += position.cost * (position.amount  * 100).toBigDecimal()
                        } else {
                            /**
                             * 期货列表
                             */
                            isStock = false
                            val open = result[2].toBigDecimal()
                            info.name = result[0]
                            info.price = result[8].toBigDecimal()
                            val yesterdayClose = result[5].toBigDecimal()
                            info.increase = info.price - yesterdayClose
                            info.time = result[1]
                            val commodity = findCommodity(position.code)
                            profit =(info.price - position.cost) * (position.type *  position.amount * commodity!!.unit).toBigDecimal()
                            totalProfit += profit
                        }
                        time = info.time
                        info.type = position.type
                        info.code = position.code
                        info.cost = position.cost
                        info.exchange = position.exchange
                        info.amount = position.amount
                        stockInfoList.add(info)
                    } else {
                        _LogUtils.log2file("获取数据失败...")
                        return@Runnable
                    }
                }
                var totalAverageIncrease = if (isStock) {
                    totalProfit / totalCostFund
                } else {
                    totalProfit
                }
                if (onLoadStockInfoListListener != null) {
                    if (positions.size != 0) onLoadStockInfoListListener.onLoadFinished(stockInfoList, totalProfit, totalAverageIncrease, time) else onLoadStockInfoListListener.onLoadFinished(stockInfoList, 0.0.toBigDecimal(), 0.0.toBigDecimal(), time)
                }
            } catch (e: Exception) {
                _Utils.getExceptionStr(e)
            }
        }).start()
    }

    @Throws(Exception::class)
    fun getStockInfo(info: StockInfo, onLoadStockInfoListener: OnLoadStockInfoListener) {
        Thread {
            var time = ""
            try {
                val url = "https://hq.sinajs.cn/list=" + info.exchange + info.code
                val client = _OkHttpUtil.client
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body!!.string()
                    val result = body.substring(body.indexOf("\"")).replace("\"", "").split(",".toRegex()).toTypedArray()
                    val open = result[2].toBigDecimal()
                    info.name = result[0]
                    info.price = result[3].toBigDecimal()
                    info.increase = (info.price - open) / open
                    info.time = result[31]
                }
                onLoadStockInfoListener.onLoadFinished()
                //                    return stockInfoList;
            } catch (e: Exception) {
                _Utils.getExceptionStr(e)
            }
        }.start()
    }

    interface OnLoadStockInfoListener {
        fun onLoadFinished()
    }

    interface OnLoadStockInfoListListener {
        fun onLoadFinished(infoList: List<StockInfo>, totalProfit: BigDecimal, averageProfit: BigDecimal, time: String)
    }

    interface OnLoadStockInfoListListener1 {
        fun onLoadFinished(map: Map<String, Double>)
    }
}