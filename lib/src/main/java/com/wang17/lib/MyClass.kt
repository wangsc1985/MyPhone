package com.wang17.lib

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import java.io.*
import java.math.BigDecimal
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
        fun parseJSONArray(jsonStr:String):ArrayList<Lottery>{
            val result = ArrayList<Lottery>()
            val arr = JSONArray.parse(jsonStr) as JSONArray
            arr.forEach {
                val lotJson = it as JSONObject
                val period = lotJson.get("period").toString().toInt()
                val redA = lotJson.get("redArr").toString().replace("[","").replace("]","").split(",")
                val redArr = ArrayList<Int>()
                redA.forEach {
                    redArr.add(it.toInt())
                }
                val blueA = lotJson.get("blueArr").toString().replace("[","").replace("]","").split(",")
                val blueArr = ArrayList<Int>()
                blueA.forEach {
                    blueArr.add(it.toInt())
                }
                val multiple = lotJson.get("multiple").toString().toInt()
                val type = lotJson.get("type").toString().toInt()
                result.add(Lottery(period,redArr,blueArr,multiple,type))
            }
            return result
        }

        fun getStockHistory(code:String){
            val url = "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=${if(code.startsWith("6")) "sh" else "sz"}${code}&scale=240&ma=5&datalen=4023"
            _OkHttpUtil.getRequest(url){html->
                println(html)
                val arr = JSONArray.parse(html) as JSONArray
                var count=0
                arr.forEach {
                    val obj = JSONObject.parse(it.toString()) as JSONObject
                    val dateStr = obj["day"].toString()
                    val close = obj["close"].toString().toBigDecimal()

                    val aa = dateStr.split(" ")
                    val dateArr = aa[0].split("-")
                    var date = DateTime(dateArr[0].toInt(),dateArr[1].toInt()-1,dateArr[2].toInt())
                    if(aa.size>1){
                        val timeArr = aa[1].split(":")
                        date = DateTime(dateArr[0].toInt(),dateArr[1].toInt()-1,dateArr[2].toInt(),timeArr[0].toInt(),timeArr[1].toInt(),timeArr[2].toInt())
                    }

                    if(aa.size>1){
                        if(date.hour==15){
                            count++
                            println("${count} : ${code}  ${date.toShortDateString()}  ${close}")
                        }
                    }else{
                        count++
                        println("${count} : ${code}  ${date.toShortDateString()}  ${close}")
                    }
                }
            }
        }
        @JvmStatic
        fun main(args: Array<String>) {

            println((666*1.08).toInt())


//            val date1 = DateTime(2020,1,23,7,2,5)
//            val date2 = DateTime(2020,1,24,15,2,5)
//            println(date1.date.timeInMillis)
//            println((date2.date.timeInMillis-date1.date.timeInMillis)/(1000*60*60*24))

//            val body="【交通银行信用卡】送你1次抽奖机会！9月20日登录交通银行信用卡周周刷活动主页即可获得！百发百中送您积分、刷卡金或者金砖哦，戳 cc."
//            var matcher = Pattern.compile("(?<=【).{1,10}(?=】)").matcher(body)
//            if (matcher.find())
//                println(matcher.group().trim())
//
//
//            val a:String?=null
//            val b:String?="asdfasdfasdf"
//            println(b?:"ccc")

//
//            var match = Pattern.compile("[0-9]{6}").matcher("【去哪儿网】验证码: 7782，十分钟后失效，请勿向任何人提供验证码")
//            if (match.find()) {
//                println(match.group())
//            } else {
//                match = Pattern.compile("[0-9]{4}").matcher("【去哪儿网】验证码: 7782，十分钟后失效，请勿向任何人提供验证码")
//                if (match.find()) {
//                    println(match.group())
//                }
//            }


//            val lotterys = ArrayList<Lottery>()
//            lotterys.add(Lottery(21025, arrayListOf(3,4,22,23,28,1), arrayListOf(1,4),1,1))
//            lotterys.add(Lottery(21025, arrayListOf(17,24,28,30,33), arrayListOf(3,12),1,1))
//            lotterys.add(Lottery(21025, arrayListOf(13,20,22,32,35), arrayListOf(1,10),1,1))
//            lotterys.add(Lottery(21025, arrayListOf(3,10,16,27,33), arrayListOf(3,4),1,1))
//            lotterys.add(Lottery(21025, arrayListOf(10,13,24,31,35), arrayListOf(2,11),1,1))
//            val winLottery= Lottery(21025, arrayListOf(17,18,23,30,31), arrayListOf(10,12),0,1)
//
//            val jsonStr = JSONArray.toJSON(lotterys).toString()
//            println(jsonStr)
//
//           val arr =  parseJSONArray(jsonStr)
//            arr.forEach {
//                println(it.redArr[1])
//            }



//            LotteryUtil.dlt(lotterys,winLottery)

//            var content="勿泄露验证码！验证码646777，5分钟内输入有效。您尾号7086的信用卡正在进行【财付通公司】快捷支付-签约。【交通银行】"
//            var matcher = Pattern.compile("(?<=验证码).{0,2}[0-9]{4,8}").matcher(content)
//            matcher.find()
//            content = matcher.group()
//            println(content)
//            matcher = Pattern.compile("[0-9]{4,8}").matcher(content)
//            matcher.find()
//            println(matcher.group())

//            println(3120/300*300)
//
//            _OkHttpUtil.getRequest("https://weizhao1985.lofter.com/post/4c3be773_1cbccf784", HttpCallback {html ->
//                try {
//                    var html=  html.replace("\r","").replace("\n","")
//                    println(html)
//                    var matcher = Pattern.compile("(?<=寿康宝鉴日历版本z)[0-9]*(?=</p>)").matcher(html)
//                    matcher.find()
//                    val version = matcher.group().trim()
//                    println(version)
//                } catch (e: Exception) {
//                    println(e.message)
//                }
//            })
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

//        fun getDltHttp(){
//            _OkHttpUtil.getRequest("https://www.xinti.com/prizedetail/dlt.html") { html ->
//                try {
//                    var cc = html.substring(html.indexOf("<span class=\"iballs\">"))
//                    cc = cc.substring(0, cc.indexOf("</span>"))
//                    println(cc)
//                } catch (e: Exception) {
//                    println(e.message)
//                }
//            }
//        }

    }
}
