package com.wang17.lib

import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.DateTimeException
import java.util.*
import kotlin.collections.ArrayList

class MyClass {
    class Person{
        var name=""
        var age=18
        var sex =1

        constructor(name: String, age: Int) {
            this.name = name
            this.age = age
        }
    }
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            for(i in 1..10){
                println(i)
            }
        }

        private fun testApplys(){
           var pp =  Person("赵五",33).apply {
                name="张三"
            }.apply {
                sex=0
            }

            println("${pp.name}  ${pp.age}  ${pp.sex}")


            pp =  Person("赵五",33).also {
                it.name="张三"
            }.also {
                it.sex=0
            }

            println("${pp.name}  ${pp.age}  ${pp.sex}")

        }

        private fun testListFind(){
            var person:MutableList<Person> = ArrayList()
            person.add(Person("赵五",33))
            person.add(Person("郑七",33))
            person.add(Person("张三",35))
            person.add(Person("李四",33))
            person.add(Person("王二",30))
            person.add(Person("吴六",23))
            person.add(Person("孙八",33))

            println("------------------ filter test --------------------")
            val aa = person.filter {
                it.age==33
            }
            aa.forEach {
                println("name : ${it.name} , age : ${it.age}")
            }

            println("------------------ takeWhile test --------------------")
            val bb = person.takeWhile {
                it.age==33
            }
            bb.forEach {
                println("name : ${it.name} , age : ${it.age}")
            }
        }

        private fun testBigDecimal() {
            println("----------------------------------------------------------------------------------------------------------------------------------------------------")
            println("在BigDecimal数值类型进行除法计算时，为了避免精度丢失，要预估除法结果小数位数，并将被除数设置decimal小数位数。")
            println("否则的话，被除数和除数都是整数，进行decimal数据转换时，会自动转换为精度为0的decimal类型，计算结果也会自动按照整数相除处理，处理的结果也是没有小数的四舍五入值。")
            println()
            var aaa = 1.000000001.toBigDecimal() / 3.toBigDecimal()
            println(aaa)
            aaa = 0.00001.toBigDecimal() + 1.01.toBigDecimal() / 3.toBigDecimal()
            println(aaa)
            println()

            println("----------------------------------------------------------------------------------------------------------------------------------------------------")
            println("用double.toBigDecimal()得到的BigDecimal数值，不存在精度丢失，但是小数位数只会精确到最后一个非0数字。")
            println()
            aaa = 1.000010.toBigDecimal()
            println(aaa - 1.toBigDecimal())
            println()

            /**
             *
             *
             */
            println("----------------------------------------------------------------------------------------------------------------------------------------------------")
            println("用BigDecimal(str:String)构造方法创建的BigDecimal数值，不会有精度的丢失")
            println("用BigDecimal(str:Double)构造方法创建的BigDecimal数值，有精度的丢失")
            println()
            aaa = BigDecimal("1.324003563")
            println(aaa - 1.toBigDecimal())
            aaa = BigDecimal(1.324003563)
            println(aaa - 1.toBigDecimal())
            println()

            println("----------------------------------------------------------------------------------------------------------------------------------------------------")
            println("自定义decimal带默认精度的转换方法")
            println()
            aaa = 1.toDecimal()
            println(aaa)
        }

        fun abc(callBack: CallBack) {
            callBack.execute()
        }

        fun getDltHttp(){
            _OkHttpUtil.getRequest("https://www.xinti.com/prizedetail/dlt.html") { html ->
                try {
                    var cc = html.substring(html.indexOf("<span class=\"iballs\">"))
                    cc = cc.substring(0, cc.indexOf("</span>"))
                    println(cc)
                } catch (e: Exception) {
                    println(e.message)
                }
            }
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
