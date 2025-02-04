package com.wang17.myphone.util

import android.graphics.Color
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.wang17.myphone.activity.StockPositionHistoryActivity.AttTrades
import com.wang17.myphone.model.StockInfo
import com.wang17.myphone.database.Position
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.Stock
import com.wang17.myphone.service.StockService.Companion.findCommodity
import com.wang17.myphone.setMyScale
import com.wang17.myphone.toMyDecimal
import okhttp3.Request
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.CountDownLatch

object _SinaStockUtils {
    fun getStockInfoList(attTrades: List<AttTrades>) {
        Thread(Runnable {
            try {
                val format2 = DecimalFormat("#,##0.00")
                val format = DecimalFormat("#,##0")
                for ((code,tradePrice,tradeAmount,type,tvIncrease) in attTrades) {
                    val url = "https://hq.sinajs.cn/list=" + (if (code.startsWith("6")) "sh" else "sz") + code
                    val client = _OkHttpUtil.client
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body!!.string()
                        val result = body.substring(body.indexOf("\"")).replace("\"", "").split(",".toRegex()).toTypedArray()
                        val newPrice = result[3].toBigDecimal()
                        var newCommission = 0.toBigDecimal()
                        var newTransferFee=0.toBigDecimal()
                        var newFax=0.toBigDecimal()
                        var tradeCommission = 0.toBigDecimal()
                        var tradeTransferFee=0.toBigDecimal()
                        var tradeFax=0.toBigDecimal()

                        var increase=0.toMyDecimal()
                        var profit = 0.toMyDecimal()
                        when(type){
                            // 关注的是 买入的交易
                            1->{
                                tradeCommission = TradeUtils.commission(tradePrice,tradeAmount)
                                tradeTransferFee = TradeUtils.transferFee(tradePrice,tradeAmount)
                                newCommission = TradeUtils.commission(newPrice,tradeAmount)
                                newTransferFee = TradeUtils.transferFee(newPrice,tradeAmount)
                                newFax = TradeUtils.tax(-1,newPrice,tradeAmount)

                                profit = (newPrice-tradePrice)*tradeAmount.toMyDecimal()*100.toMyDecimal() - tradeCommission -tradeTransferFee-tradeFax -newCommission-newTransferFee-newFax
                                increase =profit/(tradePrice*tradeAmount.toBigDecimal()*100.toBigDecimal())
                            }
                            // 关注的是 卖出的交易
                            -1->{
                                tradeCommission = TradeUtils.commission(tradePrice,tradeAmount)
                                tradeTransferFee = TradeUtils.transferFee(tradePrice,tradeAmount)
                                tradeFax = TradeUtils.tax(-1,tradePrice,tradeAmount)
                                newCommission = TradeUtils.commission(newPrice,tradeAmount)
                                newTransferFee = TradeUtils.transferFee(newPrice,tradeAmount)

                                profit = (newPrice-tradePrice)*tradeAmount.toMyDecimal()*100.toMyDecimal() - tradeCommission -tradeTransferFee-tradeFax -newCommission-newTransferFee-newFax
                                increase =profit/(tradePrice*tradeAmount.toBigDecimal()*100.toBigDecimal())
                            }
                        }
                        tvIncrease.text = "${format2.format(Math.abs((newPrice-tradePrice).toDouble()))}  ${format2.format(increase*100.toBigDecimal())}%  ${format.format(profit)}"
                        if (increase>0.toBigDecimal()) {
                            tvIncrease.setTextColor(Color.RED)
                        } else if (increase<0.toBigDecimal()) {
                            tvIncrease.setTextColor(Color.CYAN)
                        } else {
                            tvIncrease.setTextColor(Color.WHITE)
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
            var totalProfit = 0.toBigDecimal()
            var totalCostFund = 0.toBigDecimal()
            var time = ""
            val stockInfoList: MutableList<StockInfo> = ArrayList()
            try {
                for (position in positions) {
                    val url = "https://hq.sinajs.cn/list=" + position.exchange + position.code
                    val client = _OkHttpUtil.client
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body!!.string()
                        val result = body.substring(body.indexOf("\"")).replace("\"", "").split(",".toRegex()).toTypedArray()
                        val info = StockInfo()
                        var profit = 0.toBigDecimal()
                        var increase=0.toBigDecimal()
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
                    totalProfit.setMyScale() / totalCostFund
                } else {
                    totalProfit
                }
                if (onLoadStockInfoListListener != null) {
                    if (positions.size != 0) onLoadStockInfoListListener.onLoadFinished(stockInfoList, totalProfit, totalAverageIncrease, time) else onLoadStockInfoListListener.onLoadFinished(stockInfoList, 0.toBigDecimal(), 0.toBigDecimal(), time)
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


    fun getStockHistory(code:String,startDate:DateTime):MutableList<Stock>{
        val today = DateTime()
        val days = (today.date.timeInMillis-startDate.date.timeInMillis)/(60000*60*24)
        var result = ArrayList<Stock>()
        // （参数：股票编号、分钟间隔（5、15、30、60、240）、均值（5、10、15、20、25）、查询个数点（最大值242））
//      http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=sh600123&scale=240&ma=5&datalen=3000
        val url = "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=${if(code.startsWith("6")) "sh" else "sz"}${code}&scale=240&ma=5&datalen=${days}"
        val countDownLatch = CountDownLatch(1)
        _OkHttpUtil.getRequest(url){httpCode,html->
            if(httpCode==_OkHttpUtil.HttpCode200){
                val arr = JSONArray.parse(html) as JSONArray
                arr.forEach {
                    val obj = JSONObject.parse(it.toString()) as JSONObject
                    val dateStr = obj["day"].toString()
                    val close = obj["close"].toString().toBigDecimal()

                    val dateArr = dateStr.split("-")
                    var date = DateTime(dateArr[0].toInt(),dateArr[1].toInt()-1,dateArr[2].toInt())
                    if(date.timeInMillis>=startDate.timeInMillis){
                        result.add(Stock(code,date,close))
                    }
                }
            }
            countDownLatch.countDown()
        }
        countDownLatch.await()
        return result
    }
}