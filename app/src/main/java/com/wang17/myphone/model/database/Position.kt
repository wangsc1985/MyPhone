package com.wang17.myphone.model.database

import com.wang17.myphone.decimal2
import com.wang17.myphone.decimal3
import com.wang17.myphone.roundMode
import java.lang.reflect.Field
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
        cost=BigDecimal(0.0).setScale(decimal3, roundMode)
        type=0
        amount=0
        exchange=""
        profit=BigDecimal(0.0).setScale(decimal2, roundMode)
    }
}