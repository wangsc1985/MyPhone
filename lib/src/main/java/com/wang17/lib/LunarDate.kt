package com.wang17.lib

import java.util.*

/**
 * Created by 阿弥陀佛 on 2015/6/24.
 * 只存储农历月和农历日的类
 */
class LunarDate {
    companion object {
        var Months: MutableList<String>
        private val str = arrayOf("", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
        var Days: MutableList<String>

        init {
            Months = ArrayList()
            Months.add("正月")
            Months.add("二月")
            Months.add("三月")
            Months.add("四月")
            Months.add("五月")
            Months.add("六月")
            Months.add("七月")
            Months.add("八月")
            Months.add("九月")
            Months.add("十月")
            Months.add("十一月")
            Months.add("十二月")

            Days = ArrayList()
            for (i in 1..10) {
                Days.add("初" + str[i])
            }
            for (i in 1..9) {
                Days.add("十" + str[i])
            }
            Days.add("二十")
            for (i in 1..9) {
                Days.add("廿" + str[i])
            }
            Days.add("三十")
        }

    }

    var month: Int
        private set
    var day: Int
        private set

    constructor(lunarMonth: String, lunarDay: String) {
        month = Months!!.indexOf(lunarMonth) + 1
        day = Days!!.indexOf(lunarDay) + 1
    }

    constructor(lunarMonth: Int, lunarDay: Int) {
        month = lunarMonth
        day = lunarDay
    }

    override fun equals(obj: Any?): Boolean {
        val md = obj as LunarDate?
        return md!!.month == month && md.day == day
    }

    override fun hashCode(): Int {
        return String.concat(DateTime.format(month)+ DateTime.format(day)).hashCode()
    }

    override fun toString(): String {
        return Months[month - 1] + Days[day - 1]
    }
}