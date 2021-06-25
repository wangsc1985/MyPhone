package com.wang17.myphone.util

import android.content.Context
import com.wang17.myphone.dao.DataContext
import com.wang17.myphone.model.BankBill
import com.wang17.myphone.model.DateTime
import java.util.*
import java.util.regex.Pattern

/**
 * Created by 阿弥陀佛 on 2016/10/27.
 */
object ParseCreditCard {

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
            _Utils.printException(context,e)
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
            _Utils.printException(context,e)
            return null
        }
        return null
    }
}