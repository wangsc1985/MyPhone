package com.wang17.lib

import java.text.DecimalFormat
import java.util.regex.Pattern

class MyClass {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            _OkHttpUtil.getRequest("https://www.xinti.com/prizedetail/dlt.html", HttpCallback { html ->
                try {
                    var cc = html.substring(html.indexOf("<span class=\"iballs\">"))
                   cc=  cc.substring(0,cc.indexOf("</span>"))
                    println(cc)
//                    println(html)
//                    var matcher = Pattern.compile("(?<=iballs).*(?=</ul>)").matcher(html)
//                    var matcher = Pattern.compile("(?<=<ul class=\"notice-list\">).*").matcher(html)
//                    matcher.find()
//                    val list = matcher.group().toString()
//                    println(list)
                } catch (e: Exception) {
                    println(e.message)
                }
            })
        }

        fun abc(callBack: CallBack) {
            callBack.execute()
        }

        /**
         * 大乐透
         */
        fun dlt() {
            val bonus = arrayOf(
                    arrayOf(5, 2, "一", 5000000),
                    arrayOf(5, 1, "二", 100000),
                    arrayOf(5, 0, "三", 10000),
                    arrayOf(4, 2, "四", 3000),
                    arrayOf(4, 1, "五", 300),
                    arrayOf(3, 2, "六", 200),
                    arrayOf(4, 0, "七", 100),
                    arrayOf(3, 1, "八", 15), arrayOf(2, 2, "八", 15),
                    arrayOf(3, 0, "九", 5), arrayOf(1, 2, "九", 5), arrayOf(2, 1, "九", 5), arrayOf(0, 2, "九", 5))
            val winLottery = arrayOf(intArrayOf(25, 28, 4, 35, 22), intArrayOf(4, 5))
            val winRedArr = winLottery[0]
            val winBlueArr = winLottery[1]

            val lotteryArr = arrayOf(
                    arrayOf(intArrayOf(25, 28, 4, 35, 22), intArrayOf(4, 10), 1),
                    arrayOf(intArrayOf(3, 12, 15, 23, 30), intArrayOf(11, 12), 1),
                    arrayOf(intArrayOf(4, 9, 10, 31, 33), intArrayOf(5, 12), 1),
                    arrayOf(intArrayOf(4, 6, 11, 18, 35), intArrayOf(6, 12), 1),
                    arrayOf(intArrayOf(4, 8, 10, 12, 35), intArrayOf(9, 12), 1))
            lotteryArr.forEach { lottery ->
                val redArr = lottery[0] as IntArray
                val blueArr = lottery[1] as IntArray
                val multiple = lottery[2] as Int

                var redWinCount = 0
                var blueWinCount = 0
                var redWinStr = StringBuffer()
                var blueWinStr = StringBuffer()
                redArr.forEach { num ->
                    if (winRedArr.contains(num)) {
                        redWinStr.append("$num ")
                        redWinCount++
                    }
                }
                redWinStr.append("+ ")
                blueArr.forEach { num ->
                    if (winBlueArr.contains(num)) {
                        blueWinStr.append("$num ")
                        blueWinCount++
                    }
                }
                bonus.forEach {
                    val rc = it[0] as Int
                    val bc = it[1] as Int
                    if (rc == redWinCount && bc == blueWinCount) {
                        println("${redWinStr}${blueWinStr}")
                        println("${it[2]}等奖 奖金${DecimalFormat("#,###").format((it[3] as Int) * multiple)}元")
                    }
                }
            }
        }

        /**
         * 双色球
         */
        fun ssq() {
            val bonus = arrayOf(
                    arrayOf(6, 1, "一", 5000000),
                    arrayOf(6, 0, "二", 100000),
                    arrayOf(5, 1, "三", 3000),
                    arrayOf(5, 0, "四", 200), arrayOf(4, 1, "四", 200),
                    arrayOf(4, 0, "五", 10), arrayOf(3, 1, "五", 10),
                    arrayOf(2, 1, "六", 5), arrayOf(1, 1, "六", 5), arrayOf(0, 1, "六", 5))
            val winLottery = arrayOf(intArrayOf(7, 9, 14, 26, 30, 31), 4)
            val winRedArr = winLottery[0] as IntArray
            val winBlue = winLottery[1] as Int
            var totalWin = 0
            var totalAward = 0

            val lotteryArr = arrayOf(
                    arrayOf(intArrayOf(3, 8, 12, 15, 23, 30), 11, 1),
                    arrayOf(intArrayOf(4, 8, 9, 10, 21, 31), 6, 1),
                    arrayOf(intArrayOf(4, 8, 6, 11, 18, 31), 10, 1),
                    arrayOf(intArrayOf(4, 8, 10, 12, 22, 30), 7, 1))
            lotteryArr.forEach { lottery ->
                val redArr = lottery[0] as IntArray
                val blue = lottery[1] as Int
                val multiple = lottery[2] as Int

                var redWinCount = 0
                var blueWinCount = 0
                var redWinStr = StringBuffer()
                var blueWinStr = StringBuffer()
                redArr.forEach { num ->
                    if (winRedArr.contains(num)) {
                        redWinStr.append("$num ")
                        redWinCount++
                    }
                }
                redWinStr.append("+ ")
                if (winBlue == blue) {
                    blueWinStr.append("$blue ")
                    blueWinCount++
                }


                bonus.forEach {
                    val rc = it[0] as Int
                    val bc = it[1] as Int
                    if (rc == redWinCount && bc == blueWinCount) {
                        val award = (it[3] as Int) * multiple
                        totalWin++
                        totalAward += award
                        println("${redWinStr}${blueWinStr}")
                        println("${it[2]}等奖 奖金${DecimalFormat("#,###").format(award)}元")
                    }
                }
            }
            println("中奖${totalWin}注，奖金${totalAward}元")
        }
    }
}
