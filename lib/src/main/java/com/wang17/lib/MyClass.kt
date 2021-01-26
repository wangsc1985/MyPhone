package com.wang17.lib

import java.io.*
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class MyClass {
    class Person{
        var name=""
        var age=18
        var sex =1
        var birthday:DateTime

        constructor(name: String, age: Int) {
            this.name = name
            this.age = age
            this.birthday = DateTime()
        }
        constructor(name: String, age: Int,birthday:DateTime) {
            this.name = name
            this.age = age
            this.birthday = birthday
        }
    }
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

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

        private fun religiousTest() {
            val today = DateTime()
            val cc = loadJavaSolarTerms()
            println(cc.size)
            var re = Religious(today.year, today.month, today.day, cc, null)
            var a = re.religiousDays
            var b = re.remarks

            a?.forEach {
                println("${it.key.toLongDateTimeString()}  ${it.value}")
            }

            println("==============================================")
            b?.forEach {
                println("${it.key.toLongDateTimeString()}  ${it.value}")
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
        /**
         * 读取JAVA结构的二进制节气数据文件。
         *
         * @param resId 待读取的JAVA二进制文件。
         * @return
         */
        @Throws(Exception::class)
        private fun loadJavaSolarTerms(): TreeMap<DateTime, SolarTerm> {
            val result = TreeMap<DateTime, SolarTerm>()
            try {
                val dis = DataInputStream(FileInputStream("d:\\solar_java_50.dat"))
                var date = dis.readLong()
                var solar = dis.readInt()
                try {
                    while (true) {
                        val cal = DateTime()
                        cal.timeInMillis = date
                        val solarTerm = SolarTerm.Int2SolarTerm(solar)
                        result[cal] = solarTerm
                        date = dis.readLong()
                        solar = dis.readInt()
                    }
                } catch (ex: EOFException) {
                    dis.close()
                }
            } catch (e: Exception) {
                println(e.message)
            }

            // 按照KEY排序TreeMap
//        TreeMap<DateTime, SolarTerm> result = new TreeMap<DateTime, SolarTerm>(new Comparator<DateTime>() {
//            @Override
//            public int compare(DateTime lhs, DateTime rhs) {
//                return lhs.compareTo(rhs);
//            }
//        });
            return result
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

    }
}
