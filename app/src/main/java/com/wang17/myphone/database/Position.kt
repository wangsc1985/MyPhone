package com.wang17.myphone.database

import java.math.BigDecimal
import java.util.*

class Position {
    var id: UUID
    var code: String
    var name: String
    var cost:BigDecimal
    var type:Int // 0：股票；1：期货多单；-1：期货空单
    var amount:Int
    var exchange: String
    var profit:BigDecimal

    init {
        id = UUID.randomUUID()
        code=""
        name=""
        cost=0.toBigDecimal()
        type=0
        amount=0
        exchange=""
        profit=0.toBigDecimal()
    }
}