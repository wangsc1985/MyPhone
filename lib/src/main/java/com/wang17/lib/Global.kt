package com.wang17.lib

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

val scale = 8
val roundMode = BigDecimal.ROUND_DOWN
fun Int.toDecimal():BigDecimal{
    return this.toBigDecimal().setScale(scale,roundMode)
}

fun Double.toDecimal():BigDecimal{
    return this.toBigDecimal().setScale(scale,roundMode)
}
fun Float.toDecimal():BigDecimal{
    return this.toBigDecimal().setScale(scale,roundMode)
}
fun Long.toDecimal():BigDecimal{
    return this.toBigDecimal().setScale(scale,roundMode)
}
fun String.toDecimal():BigDecimal{
    return this.toBigDecimal().setScale(scale,roundMode)
}

fun String.Companion.concat(vararg args:Any):String{
    var sb=StringBuffer()
    for(str in args){
        sb.append(str.toString())
    }
    return sb.toString()
}
