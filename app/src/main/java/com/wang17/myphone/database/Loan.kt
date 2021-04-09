package com.wang17.myphone.database

import com.wang17.myphone.model.DateTime
import java.math.BigDecimal
import java.util.*

/**
 * 贷款类，name：贷款名称；dateTime：贷款日期；sum：借款金额；rate：贷款日利率 --元/万/天
 */
class Loan (
        var id: UUID,
        var name:String,
        var date:DateTime,
        var sum:BigDecimal,
        var rate:BigDecimal,
        )