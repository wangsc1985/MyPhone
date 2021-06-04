package com.wang17.myphone.database

import com.wang17.myphone.structure.CardType
import com.wang17.myphone.structure.RepayType
import java.math.BigDecimal
import java.util.*

data class CreditCard(
    var bankName: String,
    var userName: String,
    var number: String,
    /**
      * 信用额度
      */
     var quota:BigDecimal,
    /**
      * 剩余额度
      */
     var balance:BigDecimal,
    /**
     * 账单日
     */
    var billDay:Int,
    /**
     * 还款日 0代表是非固定还款日，非0代表是固定还款日
     */
    var repayDay:Int,
    /**
     * 年费
     */
    var annualFee:BigDecimal,
    /**
     * 是否显示
     */
    var isVisible:Boolean,
){
    var id: UUID=UUID.randomUUID()
}