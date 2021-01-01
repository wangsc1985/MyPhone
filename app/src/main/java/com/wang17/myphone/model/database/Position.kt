package com.wang17.myphone.model.database

import java.util.*

class Position {
    var id: UUID
    var code: String
    var name: String
    var cost:Double
    var type:Int // 0：股票；1：期货多单；-1：期货空单
    var amount:Int
    var exchange: String
    var profit:Double

    init {
        id = UUID.randomUUID()
        code=""
        name=""
        cost=0.0
        type=0
        amount=0
        exchange=""
        profit=0.0
    }
}