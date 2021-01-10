package com.wang17.lib

import java.util.*
import java.util.regex.Pattern

class MyClass {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                var body = "您尾号0655卡10月30日15:33手机银行支出(跨行汇款)10元，余额1,048元，对方户名：王世超，对方账户尾号：3919"
                body = body.replace(",", "")
                val pattern = Pattern.compile("(您尾号0655).+(余额).*")
                var matcher = pattern.matcher(body)
                if (matcher.find()) {
                    // 尾号
                    matcher = Pattern.compile("(?<=尾号)[0-9]+").matcher(body)
                    matcher.find()
                    val cardNumber = matcher.group()
                    println("cardNumber : $cardNumber")
                    //月
                    matcher = Pattern.compile("[0-9]{1,2}(?=月)").matcher(body)
                    matcher.find()
                    val month = matcher.group().toInt() - 1
                    println("month : $month")
                    // 日
                    matcher = Pattern.compile("[0-9]{1,2}(?=日)").matcher(body)
                    matcher.find()
                    val day = matcher.group().toInt()
                    println("day : $day")
                    // 时
                    matcher = Pattern.compile("(?<=日)[0-9]{1,2}(?=:)").matcher(body)
                    matcher.find()
                    val hour = matcher.group().toInt()
                    println("hour : $hour")
                    // 分
                    matcher = Pattern.compile("(?<=:)[0-9]{1,2}").matcher(body)
                    matcher.find()
                    val minite = matcher.group().toInt()
                    println("minite : $minite")
                    // 金额
                    matcher = Pattern.compile("[0-9]+(\\.[0-9]{0,2})?(?=元)").matcher(body)
                    matcher.find()
                    val money = matcher.group().toDouble()
                    println("money : $money")
                    // 余额
                    matcher = Pattern.compile("(?<=余额)[0-9]+(\\.[0-9]{0,2})?(?=元)").matcher(body)
                    matcher.find()
                    val balance = matcher.group().toString()
                    println("balance : $balance")
                }
            } catch (e: Exception) {
            }
        }
    }
}