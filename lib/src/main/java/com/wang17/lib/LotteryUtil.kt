package com.wang17.lib

import java.text.DecimalFormat

object LotteryUtil {
    /**
     * 大乐透
     * 号码之间用“,”  号组之间用“;”
     */
    fun dlt(lotterys: String, winLottery: String) {
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

        /**
         * 本期中奖号码
         */
        val winLottery = winLottery.split(",")
        val winRedArr = intArrayOf(winLottery[0].toInt(), winLottery[1].toInt(), winLottery[2].toInt(), winLottery[3].toInt(), winLottery[4].toInt())
        val winBlueArr = intArrayOf(winLottery[5].toInt(), winLottery[6].toInt())

        var aaa = lotterys.split(";")
        var lotteryArr = arrayOfNulls<Any>(aaa.size)
        for (i in aaa.indices) {
            var bbb = aaa[i].split(",")
            var lottery = arrayOfNulls<Int>(bbb.size)
            for (j in bbb.indices) {
                lottery[j] = bbb[j].toInt()
            }
            lotteryArr[i] = lottery
        }

        /**
         * 购买的彩票
         */
//            val lotteryArr = arrayOf(
//                    arrayOf(intArrayOf(25, 28, 4, 35, 22), intArrayOf(4, 10), 1),
//                    arrayOf(intArrayOf(3, 12, 15, 23, 30), intArrayOf(11, 12), 1),
//                    arrayOf(intArrayOf(4, 9, 10, 31, 33), intArrayOf(5, 12), 1),
//                    arrayOf(intArrayOf(4, 6, 11, 18, 35), intArrayOf(6, 12), 1),
//                    arrayOf(intArrayOf(4, 8, 10, 12, 35), intArrayOf(9, 12), 1))
        lotteryArr.forEach { lottery ->
            var cc = lottery as Array<Int>
            val redArr = intArrayOf(cc[0], cc[1], cc[2], cc[3], cc[4])
            val blueArr = intArrayOf(cc[5], cc[6])
            val multiple = cc[7]

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
     * 大乐透
     */
    fun dlt(lotterys: List<Lottery>, winLottery: Lottery) {
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

        /**
         * 本期中奖号码
         */
        val winRedArr = winLottery.redArr
        val winBlueArr = winLottery.blueArr

        /**
         * 购买的彩票
         */
//            val lotteryArr = arrayOf(
//                    arrayOf(intArrayOf(25, 28, 4, 35, 22), intArrayOf(4, 10), 1),
//                    arrayOf(intArrayOf(3, 12, 15, 23, 30), intArrayOf(11, 12), 1),
//                    arrayOf(intArrayOf(4, 9, 10, 31, 33), intArrayOf(5, 12), 1),
//                    arrayOf(intArrayOf(4, 6, 11, 18, 35), intArrayOf(6, 12), 1),
//                    arrayOf(intArrayOf(4, 8, 10, 12, 35), intArrayOf(9, 12), 1))
        lotterys.forEach { lottery ->
            if (winLottery.period == lottery.period) {
                val redArr = lottery.redArr
                val blueArr = lottery.blueArr
                val multiple = lottery.multiple

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
    }

    /**
     * 双色球
     * 号码之间用“,”  号组之间用“;”
     */
    fun ssq(lotterys: String, winLottery: String) {
        val bonus = arrayOf(
                arrayOf(6, 1, "一", 5000000),
                arrayOf(6, 0, "二", 100000),
                arrayOf(5, 1, "三", 3000),
                arrayOf(5, 0, "四", 200), arrayOf(4, 1, "四", 200),
                arrayOf(4, 0, "五", 10), arrayOf(3, 1, "五", 10),
                arrayOf(2, 1, "六", 5), arrayOf(1, 1, "六", 5), arrayOf(0, 1, "六", 5))

        /**
         * 中奖号码
         */
        val winLottery = winLottery.split(",")
        val winRedArr = intArrayOf(winLottery[0].toInt(), winLottery[1].toInt(), winLottery[2].toInt(), winLottery[3].toInt(), winLottery[4].toInt(), winLottery[5].toInt())
        val winBlue = winLottery[6].toInt()
        var totalWin = 0
        var totalAward = 0

        /**
         * 购买号码
         */
        var aaa = lotterys.split(";")
        var lotteryArr = arrayOfNulls<Any>(aaa.size)
        for (i in aaa.indices) {
            var bbb = aaa[i].split(",")
            var lottery = arrayOfNulls<Int>(bbb.size)
            for (j in bbb.indices) {
                lottery[j] = bbb[j].toInt()
            }
            lotteryArr[i] = lottery
        }


        lotteryArr.forEach { lottery ->
            var cc = lottery as Array<Int>
            val redArr = intArrayOf(cc[0], cc[1], cc[2], cc[3], cc[4], cc[5])
            val blue = cc[6]
            val multiple = cc[7] as Int

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

    /**
     * 双色球
     */
    fun ssq(lotterys: List<Lottery>, winLottery: Lottery) {
        val bonus = arrayOf(
                arrayOf(6, 1, "一", 5000000),
                arrayOf(6, 0, "二", 100000),
                arrayOf(5, 1, "三", 3000),
                arrayOf(5, 0, "四", 200), arrayOf(4, 1, "四", 200),
                arrayOf(4, 0, "五", 10), arrayOf(3, 1, "五", 10),
                arrayOf(2, 1, "六", 5), arrayOf(1, 1, "六", 5), arrayOf(0, 1, "六", 5))

        /**
         * 中奖号码
         */
        val winRedArr = winLottery.redArr
        val winBlue = winLottery.blueArr
        var totalWin = 0
        var totalAward = 0

        /**
         * 购买号码
         */

        lotterys.forEach { lottery ->
            if (winLottery.period == lottery.period) {
                val redArr = lottery.redArr
                val blue = lottery.blueArr
                val multiple = lottery.multiple

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
        }
        println("中奖${totalWin}注，奖金${totalAward}元")
    }
}