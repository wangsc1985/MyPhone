package com.wang17.myphone.database

import com.wang17.myphone.model.DateTime
import java.math.BigDecimal
import java.util.*

class Trade (
    /**
     * 交易时间
     */
    var dateTime: DateTime,
    /**
     * 股票代码
     */
    var code: String,
    /**
     * 股票名称
     */
    var name: String,
    /**
     * 交易价格
     */
    var price:BigDecimal,
    /**
     * 交易手数 1手=100股
     */
    var amount:Int,
    /**
     * 1是买，-1是卖，2 股息 ，-2 息税
     */
    var type:Int,
    /**
     *  0：普通   1：关注    -1：清仓
     */
    var tag:Int,
    /**
     * 成本
     */
    var cost:BigDecimal,

    /**
     * 持有
     */
    var hold:Int
){
    var id: UUID=UUID.randomUUID()
}