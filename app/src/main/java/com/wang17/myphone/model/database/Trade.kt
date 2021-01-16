package com.wang17.myphone.model.database

import com.wang17.myphone.model.DateTime
import java.math.BigDecimal
import java.util.*

class Trade {
    var id: UUID
    var dateTime: DateTime
    var code: String
    var name: String
    var price:BigDecimal
    var amount:Int
    var type:Int // 1是买，-1是卖，2 股息 ，-2 息税
    var tag:Int // 0：普通   1：关注    -1：清仓
    var cost:BigDecimal


    init {
        id = UUID.randomUUID()
        dateTime = DateTime.today
        code =""
        name = ""
        price = BigDecimal(0.0).setScale(3,BigDecimal.ROUND_HALF_UP)
        amount=0
        type=1
        tag = 0
        cost = BigDecimal(0.0).setScale(2,BigDecimal.ROUND_HALF_UP)
    }
}