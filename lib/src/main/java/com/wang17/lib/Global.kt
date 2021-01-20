package com.wang17.lib

import sun.rmi.runtime.Log
import java.math.BigDecimal
import java.util.*

val scale = 8
val roundMode = BigDecimal.ROUND_DOWN
fun Int.toDecimal():BigDecimal{
    return this.toBigDecimal().setScale(scale, roundMode)
}

fun Double.toDecimal():BigDecimal{
    return this.toBigDecimal().setScale(scale, roundMode)
}
fun Float.toDecimal():BigDecimal{
    return this.toBigDecimal().setScale(scale, roundMode)
}
fun Long.toDecimal():BigDecimal{
    return this.toBigDecimal().setScale(scale, roundMode)
}
fun String.toDecimal():BigDecimal{
    return this.toBigDecimal().setScale(scale, roundMode)
}

fun String.Companion.concat(vararg args: Any):String{
    var sb=StringBuffer()
    for(str in args){
        sb.append(str.toString())
    }
    return sb.toString()
}

/**
 * 时间字段，月、日、时、分、秒，小于10的，设置前缀‘0’。
 *
 * @param x
 * @return
 */
fun DateTime.Companion.format(x: Int): String {
    var s = "" + x
    if (s.length == 1) s = "0$s"
    return s
}

/**
 * 计算两个时间变量之间相隔的天数，计算精确到毫秒。
 *
 * @param calendar1
 * @param calendar2
 * @return
 */
fun DateTime.Companion.spanTotalDays(calendar1: Calendar, calendar2: Calendar): Long {
    return (calendar1.timeInMillis - calendar2.timeInMillis) / 1000 / 60 / 60 / 24
}

fun DateTime.Companion.spanTotalHours(calendar1: Calendar, calendar2: Calendar): Long {
    return (calendar1.timeInMillis - calendar2.timeInMillis) / 1000 / 60 / 60
}
