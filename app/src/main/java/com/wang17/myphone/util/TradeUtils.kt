package com.wang17.myphone.util

import com.wang17.myphone.model.database.Trade

object TradeUtils {

    /**
     * 印花税  卖出时千1
     * type price amount
     * @param money
     */
    fun tax(trade: Trade): Double {
        if (trade.type == 1) {
            return 0.0
        } else {
            var money = trade.price * trade.amount * 100
            return money / 1000
        }
    }
    fun tax(type:Int,price:Double,amount:Int): Double {
        if (type == 1) {
            return 0.0
        } else {
            var money = price * amount * 100
            return money / 1000
        }
    }

    /**
     * 佣金  万3  最低5元
     * price amount
     */
    fun commission(trade: Trade): Double {
        var money = trade.price * trade.amount * 100
        money = money * 3 / 10000
        return if (money < 5) 5.0 else money
    }
    fun commission(price:Double,amount:Int): Double {
        var money = price * amount * 100
        money = money * 3 / 10000
        return if (money < 5) 5.0 else money
    }


    /**
     * 过户费   十万2
     * price amount
     */
    fun transferFee(trade: Trade): Double {
        var money = trade.price * trade.amount * 100
        return money * 2 / 100000
    }
    fun transferFee(price:Double,amount:Int): Double {
        var money = price * amount * 100
        return money * 2 / 100000
    }
}