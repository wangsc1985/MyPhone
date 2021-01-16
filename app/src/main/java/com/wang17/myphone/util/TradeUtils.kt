package com.wang17.myphone.util

import com.wang17.myphone.model.database.Trade
import java.math.BigDecimal

object TradeUtils {

    /**
     * 印花税  卖出时千1
     * type price amount
     * @param money
     */
    fun tax(trade: Trade): BigDecimal {
        if (trade.type == 1) {
            return 0.toBigDecimal().setScale(6,BigDecimal.ROUND_HALF_UP)
        } else {
            var money = trade.price * (trade.amount * 100).toBigDecimal()
            return (money / 1000.toBigDecimal()).setScale(6,BigDecimal.ROUND_HALF_UP)
        }
    }
    fun tax(type:Int, price: BigDecimal, amount:Int): BigDecimal {
        if (type == 1) {
            return 0.toBigDecimal().setScale(6,BigDecimal.ROUND_HALF_UP)
        } else {
            var money = price * (amount * 100).toBigDecimal()
            return (money / 1000.toBigDecimal()).setScale(6,BigDecimal.ROUND_HALF_UP)
        }
    }

    /**
     * 佣金  万3  最低5元
     * price amount
     */
    fun commission(trade: Trade): BigDecimal {
        var money = trade.price * (trade.amount * 100).toBigDecimal()
        money = money * (3 / 10000).toBigDecimal().setScale(6,BigDecimal.ROUND_HALF_UP)
        return if (money < 5.toBigDecimal()) 5.0.toBigDecimal() else money
    }
    fun commission(price:BigDecimal,amount:Int): BigDecimal {
        var money = price * (amount * 100).toBigDecimal()
        money = money * (3 / 10000).toBigDecimal().setScale(6,BigDecimal.ROUND_HALF_UP)
        return if (money < 5.toBigDecimal()) 5.0.toBigDecimal() else money
    }


    /**
     * 过户费   十万2
     * price amount
     */
    fun transferFee(trade: Trade): BigDecimal {
        var money = trade.price * (trade.amount * 100).toBigDecimal()
        return money * (2 / 100000).toBigDecimal().setScale(6,BigDecimal.ROUND_HALF_UP)
    }
    fun transferFee(price:BigDecimal,amount:Int): BigDecimal {
        var money = price *( amount * 100).toBigDecimal()
        return money * (2/ 100000).toBigDecimal().setScale(6,BigDecimal.ROUND_HALF_UP)
    }
}