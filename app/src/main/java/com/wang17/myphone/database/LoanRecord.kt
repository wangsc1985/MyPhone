package com.wang17.myphone.database

import com.wang17.myphone.model.DateTime
import java.math.BigDecimal
import java.util.*

/**
 * date：借贷日期；sum：借贷金额；loanId：归属于哪笔贷款
 */
class LoanRecord(
        var id: UUID,
        var date: DateTime,
        var sum: BigDecimal,
        var interest:BigDecimal,
        var loanId:UUID
)