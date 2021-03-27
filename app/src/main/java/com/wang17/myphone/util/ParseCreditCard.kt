package com.wang17.myphone.util

import android.content.Context
import android.util.Log
import com.wang17.myphone.database.*
import com.wang17.myphone.model.BankBill
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.structure.CardType
import com.wang17.myphone.structure.RepayType
import com.wang17.myphone.structure.SmsType
import java.util.*
import java.util.regex.Pattern

/**
 * Created by 阿弥陀佛 on 2016/10/27.
 */
object ParseCreditCard {
    private var changed = false
    @JvmStatic
    fun scanPhoneMessage(context: Context): Boolean {
        try {
            changed = false
            val smsHelper = SmsHelper(context)
            val sms = SmsHelper.getSMS(context, SmsType.接收到)
            for (model in sms) {
                var billRecord: BillRecord? = null
                val billStatement: BillStatement? = null
                val body = model.body
                if (billRecord == null) billRecord = weiLiDaiJK(context, model)
                if (billRecord == null) billRecord = weiLiDaiHK(context, model)
                if (billRecord == null) billRecord = jieBeiJK(context, model)


//                if (billRecord == null)
//                    billRecord = jiaoTongCreditRepay(context, model);
                if (billRecord != null) {
                    val dataContext = DataContext(context)
                    val phoneMessage = dataContext.getPhoneMessage(model.id)
                    if (phoneMessage == null) {
                        dataContext.addPhoneMessage(model)
                        //                        smsHelper.delSms(model, true);
                        dataContext.addBillRecord(billRecord)
                        val xxx = false
                        val bill = dataContext.getLastBillRecord(billRecord.cardNumber)
                        if (bill != null && bill.dateTime.timeInMillis <= billRecord.dateTime.timeInMillis) {
                            val creditCard = dataContext.getCreditCard(billRecord.cardNumber)
                            creditCard.balance = billRecord.balance
                            dataContext.updateCreditCard(creditCard)
                        }
                        changed = true
                    }
                }
                if (billStatement != null) {
                    val dataContext = DataContext(context)
                    val phoneMessage = dataContext.getPhoneMessage(model.id)
                    if (phoneMessage == null) {
                        dataContext.addPhoneMessage(model)
                        //                        smsHelper.delSms(model, true);
                        dataContext.addBillStatement(billStatement)
                        changed = true
                    }
                }
            }
            return changed
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
        return false
    }
    //region 微粒贷
    /**
     * 借款成功通知：您好，您在手机微信申请的微粒贷  5000.00  元已成功发放，收款卡   农业银行(3919)，   预计3分钟内到账。【微众银行】
     *
     * @param context
     * @param phoneMessage
     * @return
     */
    private fun weiLiDaiJK(context: Context, phoneMessage: PhoneMessage): BillRecord? {
//        借款成功通知：您好，您在手机微信申请的微粒贷  5000.00  元已成功发放，收款卡   农业银行(3919)，   预计3分钟内到账。【微众银行】
        try {
            val msg = phoneMessage.body
            //  2016/10/24 条件判断：1、关键字匹配；
            if (msg.contains("借款成功通知：您好，您在手机微信申请的微粒贷") && msg.contains("元已成功发放，收款卡") && msg.contains("预计3分钟内到账。【微众银行】")) {
                val dateTime = phoneMessage.createTime
                val money = regString(msg, "手机微信申请的微粒贷", "元已成功发放").toDouble()
                var cardNumber = "645708679"
                val dataContext = DataContext(context)
                var creditCard = dataContext.getCreditCard(cardNumber)
                if (creditCard == null) {
                    creditCard = CreditCard("微众银行", "未知", cardNumber, CardType.微粒贷, 26000.0, 0, -1, RepayType.相对账单日, 0, true, false, 0.0)
                    dataContext.addCreditCard(creditCard)
                } else {
                    cardNumber = creditCard.cardNumber
                }
                val billRecord = BillRecord(cardNumber, dateTime, money, money, phoneMessage.id)
                billRecord.summary = "借款中"
                return billRecord
            }
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
        return null
    }

    /**
     * 还款成功通知：您在手机微信申请的微粒贷已还本息共10010.00元，还款卡为农业银行（3919）。【微众银行】
     *
     * @param context
     * @param phoneMessage
     * @return
     */
    private fun weiLiDaiHK(context: Context, phoneMessage: PhoneMessage): BillRecord? {
        try {
            val lixi = 0.0005
            val msg = phoneMessage.body
            //  2016/10/24 条件判断：1、关键字匹配；
            if (msg.contains("还款成功通知：您在手机微信申请的微粒贷已还本息共") && msg.contains("元，还款卡为") && msg.contains("【微众银行】")) {
                val repayDate = DateTime(phoneMessage.createTime.year, phoneMessage.createTime.month, phoneMessage.createTime.day)
                val money = 0 - regString(msg, "申请的微粒贷已还本息共", "元，还款卡").toDouble()
                var cardNumber = "645708679"
                val dataContext = DataContext(context)
                var creditCard = dataContext.getCreditCard(cardNumber)
                if (creditCard == null) {
                    creditCard = CreditCard("微众银行", "未知", cardNumber, CardType.微粒贷, 26000.0, 0, -1, RepayType.相对账单日, 0, true, false, 0.0)
                    dataContext.addCreditCard(creditCard)
                } else {
                    cardNumber = creditCard.cardNumber
                }
                val records = dataContext.getBillRecords(cardNumber, true)
                for (record in records) {
                    val mmm = record.money
                    val loanDate = DateTime(record.dateTime.year, record.dateTime.month, record.dateTime.day)
                    val day = (repayDate.timeInMillis - loanDate.timeInMillis) / 1000 / 3600 / 24
                    val fee = mmm * lixi * day
                    if (mmm + fee + money == 0.0) {
                        record.balance = 0.0
                        record.summary = "已还清"
                        dataContext.updateBillRecord(record)
                        changed = true
                        continue
                    }
                }
                val billRecord = BillRecord(cardNumber, repayDate, money, money, phoneMessage.id)
                billRecord.summary = "还款"
                return billRecord
            }
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
        return null
    }
    //endregion
    //region 蚂蚁借呗
    /**
     * 报！蚂蚁借呗12000.00元借款已到中国农业银行尾号3919银行卡，请查收哦！【蚂蚁借呗】
     *
     * @param context
     * @param phoneMessage
     * @return
     */
    private fun jieBeiJK(context: Context, phoneMessage: PhoneMessage): BillRecord? {
        try {
            val msg = phoneMessage.body
            //  2016/10/24 条件判断：1、关键字匹配；
            if (msg.contains("报！蚂蚁借呗") && msg.contains("元借款已到") && msg.contains("银行卡，请查收哦！【蚂蚁借呗】")) {
                val dateTime = phoneMessage.createTime
                val money = regString(msg, "报！蚂蚁借呗", "元借款已到").toDouble()
                var cardNumber = "645708679@qq.com"
                val dataContext = DataContext(context)
                var creditCard = dataContext.getCreditCard(cardNumber)
                if (creditCard == null) {
                    creditCard = CreditCard("蚂蚁借呗", "未知", cardNumber, CardType.蚂蚁借呗, 12000.0, 0, -1, RepayType.相对账单日, 0, true, false, 0.0)
                    dataContext.addCreditCard(creditCard)
                } else {
                    cardNumber = creditCard.cardNumber
                }
                val billRecord = BillRecord(cardNumber, dateTime, money, money, phoneMessage.id)
                billRecord.summary = "借款中"
                return billRecord
            }
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
        return null
    }

    //endregion
    //region 辅助方法
    @Throws(Exception::class)
    private fun toInt(sss: String): Int {
        var sss = sss
        if (sss.startsWith("0")) sss = sss.substring(1)
        return sss.toInt()
    }

    @Throws(Exception::class)
    fun regString(msg: String, startReg: String, endReg: String?): String {
        val start = msg.indexOf(startReg) + startReg.length
        val sss = msg.substring(start)
        val end = sss.indexOf(endReg!!)
        return sss.substring(0, end).trim { it <= ' ' }
    }
    //endregion
    //region 银行卡
    /**
     * 【中国农业银行】您尾号3919的农行账户于10月21日14时58分完成一笔转存交易，金额为9935.00，余额9935.23。
     *
     * @param phoneMessage
     * @return
     */
    fun parseABC(context: Context?, phoneMessage: PhoneMessage): BillRecord? {
        try {
            val msg = phoneMessage.body
            //  2016/10/24 条件判断：1、关键字匹配；
            if (msg.contains("【中国农业银行】") && msg.contains("您尾号") && msg.contains("余额")) {
                var cardNumber = regString(msg, "您尾号", "账户")
                val dateTime = regNongBankDateString(msg)
                val money = regString(msg, "人民币", "，").toDouble()
                val balance = regString(msg, "余额", "。").toDouble()
                val dataContext = DataContext(context)
                var creditCard = dataContext.getCreditCard(cardNumber)
                if (creditCard == null) {
                    creditCard = CreditCard("农业银行", "未知", cardNumber, CardType.储蓄卡, 0.0, 0, 1000, RepayType.相对账单日, 0, true, false, balance)
                    dataContext.addCreditCard(creditCard)
                } else {
                    cardNumber = creditCard.cardNumber
                }
                val billRecord = BillRecord(cardNumber, dateTime, money, balance, phoneMessage.id)
                if (money > 0) {
                    billRecord.summary = "收入"
                }
                return billRecord
            }
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
        return null
    }

    fun parseICBC(context: Context, body: String): BankBill? {
//        您尾号0655卡 1月 9日15:38网上银行收入(银联入账)29,734.56元，余额116,134.60元。【工商银行】
//        您尾号0655卡 1月 9日18:22工商银行收入(他行汇入)    1,120元，余额     1,204元，对方户名：王世超，对方账户尾号：3919。
//        您尾号0655卡 1月 9日15:38网上银行收入(银联入账)29,734.56元，余额116,134.60元。【工商银行】
//        您尾号0655卡10月30日15:33手机银行支出(跨行汇款)       10元，余额  1,048.49元，对方户名：王世超，对方账户尾号：3919

        try {
            var body = body
            body = body.replace(",", "")
            val pattern = Pattern.compile("(您尾号0655).+(余额).*")
            var matcher = pattern.matcher(body)
            if (matcher.find()) {
                // 尾号
                matcher = Pattern.compile("(?<=尾号)[0-9]+").matcher(body)
                matcher.find()
                val cardNumber = matcher.group()
                //月
                matcher = Pattern.compile("[0-9]{1,2}(?=月)").matcher(body)
                matcher.find()
                val month = matcher.group().toInt() - 1
                // 日
                matcher = Pattern.compile("[0-9]{1,2}(?=日)").matcher(body)
                matcher.find()
                val day = matcher.group().toInt()
                // 时
                matcher = Pattern.compile("(?<=日)[0-9]{1,2}(?=:)").matcher(body)
                matcher.find()
                val hour = matcher.group().toInt()
                // 分
                matcher = Pattern.compile("(?<=:)[0-9]{1,2}").matcher(body)
                matcher.find()
                val minite = matcher.group().toInt()
                // 金额
                matcher = Pattern.compile("[0-9]+(\\.[0-9]{0,2})?(?=元)").matcher(body)
                matcher.find()
                val money = matcher.group().toDouble()
                // 余额
                matcher = Pattern.compile("(?<=余额)[0-9]+(\\.[0-9]{0,2})?(?=元)").matcher(body)
                matcher.find()
                val balance = matcher.group().toDouble()

                val dateTime = DateTime()
                dateTime[Calendar.MINUTE] = minite
                dateTime[Calendar.HOUR_OF_DAY] = hour
                dateTime[Calendar.MONTH] = month
                dateTime[Calendar.DAY_OF_MONTH] = day
                val bankBill = BankBill()
                bankBill.cardNumber = cardNumber
                bankBill.dateTime = dateTime
                bankBill.money = money
                bankBill.balance = balance
                return bankBill
            }
        } catch (e: Exception) {
            DataContext(context).addLog("解析错误","解析错误",e.message)
            return null
        }
        return null
    }

    fun parseABC(context: Context,body: String): BankBill? {

        try {//     【中国农业银行】您尾号3919的农行账户于10月21日14时58分完成一笔转存交易，金额为9935.00，余额9935.23。
//          【中国农业银行】您尾号3919账户04月16日13:34完成银证转帐交易人民币-30000.00，余额218.45。	1	1	1555392899887
            val pattern = Pattern.compile("(【中国农业银行】).+(尾号).+(人民币).+(余额).*")
            var matcher = pattern.matcher(body)
            if (matcher.find()) {
                matcher = Pattern.compile("[0-9]{1,2}(?=月)").matcher(body)
                matcher.find()
                val month = matcher.group().toInt() - 1
                matcher = Pattern.compile("[0-9]{1,2}(?=日)").matcher(body)
                matcher.find()
                val day = matcher.group().toInt()
                matcher = Pattern.compile("(?<=日)[0-9]{1,2}(?=:)").matcher(body)
                matcher.find()
                val hour = matcher.group().toInt()
                matcher = Pattern.compile("(?<=:)[0-9]{1,2}").matcher(body)
                matcher.find()
                val minite = matcher.group().toInt()
                matcher = Pattern.compile("(?<=尾号)[0-9]+").matcher(body)
                matcher.find()
                val cardNumber = matcher.group()
                matcher = Pattern.compile("(?<=人民币)-?[0-9]+(\\.[0-9]{0,2})?").matcher(body)
                matcher.find()
                val money = matcher.group().toDouble()
                matcher = Pattern.compile("(?<=余额)[0-9]+(\\.[0-9]{0,2})?").matcher(body)
                matcher.find()
                val balance = matcher.group().toDouble()
                val dateTime = DateTime()
                dateTime[Calendar.MINUTE] = minite
                dateTime[Calendar.HOUR_OF_DAY] = hour
                dateTime[Calendar.MONTH] = month
                dateTime[Calendar.DAY_OF_MONTH] = day
                val bankBill = BankBill()
                bankBill.cardNumber = cardNumber
                bankBill.dateTime = dateTime
                bankBill.money = money
                bankBill.balance = balance
                return bankBill
            }
        } catch (e: Exception) {
            DataContext(context).addLog("解析错误","解析错误",e.message)
            return null
        }
        return null
    }

    private fun e(log: Any) {
        Log.e("wangsc", log.toString())
    }

    @Throws(Exception::class)
    private fun regNongBankDateString(msg: String): DateTime {
        var index = msg.indexOf("月")
        index -= 2
        val month = toInt(msg.substring(index, index + 2)) - 1
        index += 3
        val day = toInt(msg.substring(index, index + 2))
        index += 3
        val hour = toInt(msg.substring(index, index + 2))
        index += 3
        val min = toInt(msg.substring(index, index + 2))
        val today = DateTime()
        var year = today.year
        if (month - 1 > today.month) {
            year--
        }
        return DateTime(year, month, day, hour, min, 0)
    } //endregion
}