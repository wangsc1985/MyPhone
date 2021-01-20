package com.wang17.lib

import java.lang.Exception
import java.util.*
import kotlin.Any
import kotlin.Int
import kotlin.String
import kotlin.Throws
import kotlin.run
import kotlin.toString

/**
 * Created by 阿弥陀佛 on 2015/6/24.
 */
class Religious {
    var religiousDays: HashMap<DateTime, String>? = null
        private set
    var remarks: HashMap<DateTime, String>? = null
        private set
    private var lunarReligiousDays: HashMap<LunarDate, String>? = null
    private var solarTermTreeMap: TreeMap<DateTime, SolarTerm>

    constructor(year: Int, month: Int, solarTermTreeMap: TreeMap<DateTime, SolarTerm>, callBack: ReligiousCallBack?) {
        this.solarTermTreeMap = solarTermTreeMap
        this.loadReligiousDays(year, month, callBack)
        callBack?.execute()
    }

    constructor(year: Int, month: Int, day: Int, solarTermTreeMap: TreeMap<DateTime, SolarTerm>, callBack: ReligiousCallBack?) {
        this.solarTermTreeMap = solarTermTreeMap
        this.loadReligiousDays(year, month, day, callBack)
        callBack?.execute()
    }

    private fun find(startDate: DateTime, endDate: DateTime, solarTerm: SolarTerm): Map.Entry<DateTime, SolarTerm>? {
        var solar: Map.Entry<DateTime, SolarTerm>? = null
        for (entry in solarTermTreeMap.entries) {
            if (entry.key.date.compareTo(startDate) >= 0) {
                if (entry.key.date.compareTo(endDate) > 0) break
                if (entry.value == solarTerm) solar = entry
            }
        }
        return solar
    }

    private fun find3Fu(year: Int, solarTerm: SolarTerm): Map.Entry<DateTime, SolarTerm>? {
        var solar: Map.Entry<DateTime, SolarTerm>? = null
        for (entry in solarTermTreeMap.entries) {
            if (entry.key.date.year == year && entry.value == solarTerm) {
                solar = entry
            }
        }
        return solar
    }

    /// <summary>
    /// 载入给定时间当月的所有干支戒期。干支戒期，依据天干地支订立，所以每年戒期的时间都是不一样的。
    /// </summary>
    /// <param name="Calendar"></param>
    @Throws(Exception::class)
    private fun loadReligiousDays(year: Int, month: Int, callBack: ReligiousCallBack?) {
        //
        loadLunarReligiousDays()
        religiousDays = HashMap<DateTime,String>()
        remarks = HashMap<DateTime,String>()

        //
        val startDate = DateTime(year, month, 1)
        val nextMonth = startDate.addMonths(1)
        val endDate = DateTime(nextMonth.year, nextMonth.month, 1).addDays(-1)
        var chufuStartDate = startDate
        var chufuEndDate = startDate
        var zhongfuStartDate = startDate
        var zhongfuEndDate = startDate
        var mofuStartDate = startDate
        var mofuEndDate = startDate


        /// ********二分日********
        /// 春分 雷将发声。犯者生子五官四肢不全。父母有灾。宜从惊蛰节禁起。戒过一月。
        /// 秋分 杀气浸盛。阳气日衰。宜从白露节禁起。戒过一月。
        /// 此二节之前三后三共七日。犯之必得危疾。尤宜切戒。
        var solar = find(startDate.addDays(-3), endDate.addDays(3), SolarTerm.春分)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-3).date, "春分前三日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-2).date, "春分前二日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-1).date, "春分前一日。犯之必得危疾。尤宜切戒。\n四离日（春分前一日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "春分日（二分日）。1、雷将发声。犯者生子五官四肢不全。父母有灾。2、犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(1).date, "春分后一日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(2).date, "春分后二日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(3).date, "春分后三日。犯之必得危疾。尤宜切戒。")
        }
        solar = find(startDate.addDays(-3), endDate.addDays(3), SolarTerm.秋分)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-3).date, "秋分前三日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-2).date, "秋分前二日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-1).date, "秋分前一日。犯之必得危疾。尤宜切戒。\n四离日（秋分前一日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "秋分日（二分日）。1、杀气浸盛。阳气日衰。2、犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(1).date, "秋分后一日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(2).date, "秋分后二日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(3).date, "秋分后三日。犯之必得危疾。尤宜切戒。")
        }
        callBack?.execute()

        /// *******二至日*********
        /// 夏至阴阳相争。死生分判之时。宜从芒种节禁起。戒过一月。
        /// 冬至阴阳相争。死生分判之时。宜从大雪节禁起。戒过一月。
        /// 此二节乃阴阳绝续之交。最宜禁忌。
        /// 此二至节之前三后三共七日。犯之必得急疾。尤宜切戒。
        solar = find(startDate.addDays(-3), endDate.addDays(3), SolarTerm.夏至)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-3).date, "夏至前三日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-2).date, "夏至前二日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-1).date, "夏至前一日。犯之必得急疾。尤宜切戒。\n四离日（夏至前一日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "夏至日（二至日）。1、阴阳相争。死生分判之时。2、此日乃阴阳绝续之交。最宜禁忌。3、犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(1).date, "夏至后一日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(2).date, "夏至后二日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(3).date, "夏至后三日。犯之必得急疾。尤宜切戒。")
        }
        callBack?.execute()
        solar = find(startDate.addDays(-36), endDate.addDays(3), SolarTerm.冬至)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-3).date, "冬至前三日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-2).date, "冬至前二日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-1).date, "冬至前一日。犯之必得急疾。尤宜切戒。\n四离日（冬至前一日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "冬至日（二至日）。1、阴阳相争。死生分判之时。2、此日乃阴阳绝续之交。最宜禁忌。3、犯之必得急疾。尤宜切戒。\n冬至半夜子时。犯之主在一年内亡。")
            AddReligiousDay(solar.key.addDays(1).date, "冬至后一日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(2).date, "冬至后二日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(3).date, "冬至后三日。犯之必得急疾。尤宜切戒。")

            /// 冬至半夜子时，后庚辛日。第三戌日。犯之皆主在一年内亡。
            run {
                var start = solar!!.key.date
                var touchGeng = false
                var touchXin = false
                var touchXu3 = false
                var count = 1
                while (!touchGeng || !touchXin || !touchXu3) {
                    start = start.addDays(1)
                    val ganzhi = GanZhi(start, this.solarTermTreeMap)
                    if (!touchGeng && ganzhi.tianGanDay == "庚") {
                        this.AddReligiousDay(start, "冬至后庚日。犯之主在一年内亡。")
                        touchGeng = true
                    }
                    if (!touchXin && ganzhi.tianGanDay == "辛") {
                        this.AddReligiousDay(start, "冬至后辛日。犯之主在一年内亡。")
                        touchXin = true
                    }
                    if (!touchXu3 && ganzhi.diZhiDay == "戌") {
                        if (count == 3) {
                            this.AddReligiousDay(start, "冬至后第三戌日。犯之主在一年内亡。")
                            touchXu3 = true
                        }
                        count++
                    }
                }
            }
        }
        callBack?.execute()


        /// 四立日，四绝日 犯之减寿五年。
        solar = find(startDate.addDays(-60), endDate.addDays(1), SolarTerm.立春)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-1).date, "立春日前一日（四绝日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "立春日(四立日)。犯之减寿五年。")

            /// 立春后的第五个戊日为春社日。犯之减寿五年。社日受胎者。毛发皆白。
            run {
                var start = solar!!.key.date
                var count = 1
                while (true) {
                    start = start.addDays(1)
                    val ganzhi = GanZhi(start, this.solarTermTreeMap)
                    if (ganzhi.tianGanDay == "戊") {
                        if (count == 5) {
                            this.AddReligiousDay(start, "春社日（立春后第五戊日）。犯之减寿五年。社日受胎者。毛发皆白。")
                            break
                        }
                        count++
                    }
                }
            }
        }
        callBack?.execute()
        solar = find(startDate, endDate.addDays(1), SolarTerm.立夏)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-1).date, "立夏日前一日（四绝日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "立夏日(四立日)。犯之减寿五年。")
        }
        callBack?.execute()
        solar = find(startDate.addDays(-60), endDate.addDays(1), SolarTerm.立秋)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-1).date, "立秋日前一日（四绝日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "立秋日(四立日)。犯之减寿五年。")

            /// 立秋后的第五个戊日为秋社日。犯之减寿五年。社日受胎者。毛发皆白。
            run {
                var start = solar!!.key.date
                var count = 1
                while (true) {
                    start = start.addDays(1)
                    val ganzhi = GanZhi(start, this.solarTermTreeMap)
                    if (ganzhi.tianGanDay == "戊") {
                        if (count == 5) {
                            this.AddReligiousDay(start, "秋社日（立秋后第五戊日）。犯之减寿五年。社日受胎者。毛发皆白。")
                            break
                        }
                        count++
                    }
                }
            }
        }
        callBack?.execute()
        solar = find(startDate, endDate.addDays(1), SolarTerm.立冬)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-1).date, "立冬日前一日（四绝日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "立冬日(四立日)。犯之减寿五年。")
        }
        callBack?.execute()

        /// 初伏：夏至后第三个庚日起到第四个庚日前一天的一段时间叫初伏，也叫头伏。犯之减寿一年。
        run {
            solar = find3Fu(year, SolarTerm.夏至)
            var start = solar!!.key.date
            var count = 1
            while (true) {
                start = start.addDays(1)
                val ganzhi = GanZhi(start, this.solarTermTreeMap)
                if (ganzhi.tianGanDay == "庚") {
                    if (count == 3) {
                        chufuStartDate = start
                    } else if (count == 4) {
                        zhongfuStartDate = start
                        chufuEndDate = start.addDays(-1)
                        break
                    }
                    count++
                }
            }
        }
        callBack?.execute()
        /// 中伏：夏至后第四个庚日起到立秋后第一个庚日前的一段时间叫中伏，也叫二伏。犯之减寿一年。
        /// 末伏：立秋后第一个庚日起到第二个庚日前一天的一段时间叫末伏，也叫终伏。犯之减寿一年。
        run {
            solar = find3Fu(year, SolarTerm.立秋)
            var start = solar!!.key.date
            var count = 1
            while (true) {
                start = start.addDays(1)
                val ganzhi = GanZhi(start, this.solarTermTreeMap)
                if (ganzhi.tianGanDay == "庚") {
                    if (count == 1) {
                        mofuStartDate = start
                        zhongfuEndDate = start.addDays(-1)
                    } else if (count == 2) {
                        mofuEndDate = start.addDays(-1)
                        break
                    }
                    count++
                }
            }
        }
        callBack?.execute()
        var day = startDate
        while (day.compareTo(endDate) <= 0) {
            val ganzhi = GanZhi(day, solarTermTreeMap)
            val lunar = Lunar(day)
            val chineseMonth = lunar.month
            val chineseDay = lunar.day

            //　大小月，大月30天，小月30天
            var maxDay = lunar.day
            var tempday = day.addDays(1)
            var tmpLunar = Lunar(tempday)
            while (tmpLunar.month == lunar.month) {
                maxDay = tmpLunar.day
                tempday = tempday.addDays(1)
                tmpLunar = Lunar(tempday)
            }


            /// 犯之减寿五年。（农历正月十五、七月十五、十月十五，为上中下三元）
            if (chineseDay == 15 && chineseMonth == 1) {
                AddReligiousDay(day, "农历正月十五（三元日）。犯之减寿五年。")
            } else if (chineseDay == 15 && chineseMonth == 7) {
                AddReligiousDay(day, "农历七月十五（三元日）。犯之减寿五年。")
            } else if (chineseDay == 15 && chineseMonth == 10) {
                AddReligiousDay(day, "农历十月十五（三元日）。犯之减寿五年。")
            }

            /// 三伏日
            if (day.compareTo(chufuEndDate.date) <= 0 && day.compareTo(chufuStartDate.date) >= 0) {
                AddReligiousDay(day, "初伏。犯之减寿一年。")
            } else if (day.compareTo(zhongfuEndDate.date) <= 0 && day.compareTo(zhongfuStartDate.date) >= 0) {
                AddReligiousDay(day, "中伏。犯之减寿一年。")
            } else if (day.compareTo(mofuEndDate.date) <= 0 && day.compareTo(mofuStartDate.date) >= 0) {
                AddReligiousDay(day, "末伏。犯之减寿一年。")
            }


            /// 毁败日：大月十八日。小月十七日。犯之得病。
            val tChineseDay = lunar.day
            if (tChineseDay == 17 || tChineseDay == 18) {
                if (maxDay == 30) {
                    if (tChineseDay == 18) {
                        AddReligiousDay(day, "毁败日。犯之得病。")
                    }
                } else {
                    if (tChineseDay == 17) {
                        AddReligiousDay(day, "毁败日。犯之得病。")
                    }
                }
            }

            /// 上弦为初七初八，下弦为二十二二十三。犯之减寿一年。
            if (chineseDay == 7 || chineseDay == 8) {
                AddReligiousDay(day, "上弦日。犯之减寿一年。")
            } else if (chineseDay == 22 || chineseDay == 23) {
                AddReligiousDay(day, "下弦日。犯之减寿一年。")
            }
            if (Lunar(day.addDays(1)).day == 1) {
                AddReligiousDay(day, "本月最后一天（晦日）。犯之减寿一年。")
            }

            /// 每月三辛日，犯之减寿一年。
            if (ganzhi.tianGanDay == "辛") {
                AddReligiousDay(day, "每月三辛日。犯之减寿一年。")
            } else if (ganzhi.tianGanDay == "甲" && ganzhi.diZhiDay == "子") {
                AddReligiousDay(day, "甲子日。犯之皆减寿一年。")
            } else if (ganzhi.tianGanDay == "庚" && ganzhi.diZhiDay == "申") {
                AddReligiousDay(day, "庚申日。犯之皆减寿一年。")
            } else if (ganzhi.tianGanDay == "丙" || ganzhi.tianGanDay == "丁") {
                AddReligiousDay(day, "丙丁日。天地仓开日。犯之皆得病。")
            }


            /// 十恶大败日
            /// 甲己年。三月戊戌日。七月癸亥日。十月丙申日。十一月丁亥日。
            /// 乙庚年。四月壬申日。九月乙巳日
            /// 丙辛年。三月辛巳日。九月庚辰日。十月甲辰日。
            /// 戊癸年。六月己丑日。
            /// 此皆大不吉之日。宜戒
            if (((ganzhi.tianGanYear == "甲" || ganzhi.tianGanYear == "己")
                            && (chineseMonth == 3 && ganzhi.tianGanDay == "戊" && ganzhi.diZhiDay == "戌" || chineseMonth == 7 && ganzhi.tianGanDay == "癸" && ganzhi.diZhiDay == "亥" || chineseMonth == 10 && ganzhi.tianGanDay == "丙" && ganzhi.diZhiDay == "申" || chineseMonth == 11 && ganzhi.tianGanDay == "丁" && ganzhi.diZhiDay == "亥")) ||
                    ((ganzhi.tianGanYear == "乙" || ganzhi.tianGanYear == "庚")
                            && (chineseMonth == 4 && ganzhi.tianGanDay == "壬" && ganzhi.diZhiDay == "申"
                            || chineseMonth == 9 && ganzhi.tianGanDay == "乙" && ganzhi.diZhiDay == "巳")) ||
                    ((ganzhi.tianGanYear == "丙" || ganzhi.tianGanYear == "辛")
                            && (chineseMonth == 3 && ganzhi.tianGanDay == "辛" && ganzhi.diZhiDay == "巳" || chineseMonth == 9 && ganzhi.tianGanDay == "庚" && ganzhi.diZhiDay == "辰" || chineseMonth == 10 && ganzhi.tianGanDay == "甲" && ganzhi.diZhiDay == "辰")) ||
                    ((ganzhi.tianGanYear == "戊" || ganzhi.tianGanYear == "癸")
                            && chineseMonth == 6 && ganzhi.tianGanDay == "己" && ganzhi.diZhiDay == "丑")) {
                AddReligiousDay(day, "十恶大败日。此皆大不吉之日。宜戒")
            }

            /// 阴错日
            /// 正月庚戌日 二月辛酉日 三月庚申日 四月丁未日
            /// 五月丙午日 六月丁巳日 七月甲辰日 八月乙卯日
            /// 九月甲寅日 十月癸丑日 十一月壬子日 十二月癸亥日
            /// 此阴不足之日。俱宜戒。
            if (chineseMonth == 1 && ganzhi.tianGanDay == "庚" && ganzhi.diZhiDay == "戌" || chineseMonth == 2 && ganzhi.tianGanDay == "辛" && ganzhi.diZhiDay == "酉" || chineseMonth == 3 && ganzhi.tianGanDay == "庚" && ganzhi.diZhiDay == "申" || chineseMonth == 4 && ganzhi.tianGanDay == "丁" && ganzhi.diZhiDay == "未" || chineseMonth == 5 && ganzhi.tianGanDay == "丙" && ganzhi.diZhiDay == "午" || chineseMonth == 6 && ganzhi.tianGanDay == "丁" && ganzhi.diZhiDay == "巳" || chineseMonth == 7 && ganzhi.tianGanDay == "甲" && ganzhi.diZhiDay == "辰" || chineseMonth == 8 && ganzhi.tianGanDay == "乙" && ganzhi.diZhiDay == "卯" || chineseMonth == 9 && ganzhi.tianGanDay == "甲" && ganzhi.diZhiDay == "寅" || chineseMonth == 10 && ganzhi.tianGanDay == "癸" && ganzhi.diZhiDay == "丑" || chineseMonth == 11 && ganzhi.tianGanDay == "壬" && ganzhi.diZhiDay == "子" || chineseMonth == 12 && ganzhi.tianGanDay == "癸" && ganzhi.diZhiDay == "亥") {
                AddReligiousDay(day, "阴错日。此阴不足之日。俱宜戒。")
            }

            // 五毒月
            if (chineseMonth == 5) {
                AddRemark(day, "注：农历五月俗称五毒月，按此月宜全戒为是。")
            }

            // 农历戒期
            if (lunarReligiousDays!!.containsKey(LunarDate(chineseMonth, chineseDay))) {
                AddReligiousDay(day, lunarReligiousDays!![LunarDate(chineseMonth, chineseDay)]?:"")
            }

            //region 个人相关斋戒日

            /// 祖先亡忌日。父母诞日、忌日。犯之皆减寿一年。
/*            for (MemorialDay memorialDay : memorialDays) {
                if (chineseMonth == memorialDay.getLunarDate().getMonth() && chineseDay == memorialDay.getLunarDate().getDay())
                    this.AddReligiousDay(day, memorialDay.getRelation().toString() + memorialDay.getType() + "。犯之减寿一年。");
            }*/
            // 太岁日。犯之皆减寿一年。
            if ("丑" == ganzhi.diZhiDay) {
                AddReligiousDay(day, "本人太岁日。犯之减寿一年。")
            }
            /// 己身夫妇本命诞日。犯之皆减寿。
            /*       if (!_String.IsNullOrEmpty(this.zodiac2) && this.ZodiacToDizhi(this.zodiac2).equals(ganzhi.getDiZhiDay())) {
                this.AddReligiousDay(day, "配偶太岁日。犯之减寿一年。");
            }*/

            //endregion

            //region 佛教斋戒日

            // 六斋日（每月）初八日、十四日、十五日、廿三日、廿九日、三十日（月小从廿八日起）
/*            if (swith_lzr) {
                if (maxDay == 30) {
                    if (chineseDay == 8 || chineseDay == 14 || chineseDay == 15 || chineseDay == 23 || chineseDay == 29 || chineseDay == 30) {
                        this.AddReligiousDay(day, "六斋日");
                    }
                } else {
                    if (chineseDay == 8 || chineseDay == 14 || chineseDay == 15 || chineseDay == 23 || chineseDay == 28 || chineseDay == 29) {
                        this.AddReligiousDay(day, "六斋日");
                    }
                }
            }*/

            // 十斋日（每月）初一日、初八日、十四日、十五日、十八日、廿三日、廿四日、廿八日、廿九日、三十日（月小从廿七日起）
//            if (swith_szr) {
            if (maxDay == 30) {
                if (chineseDay == 1 || chineseDay == 8 || chineseDay == 14 || chineseDay == 15 || chineseDay == 18 || chineseDay == 23 || chineseDay == 24 || chineseDay == 28 || chineseDay == 29 || chineseDay == 30) {
                    AddReligiousDay(day, "十斋日")
                }
            } else {
                if (chineseDay == 1 || chineseDay == 8 || chineseDay == 14 || chineseDay == 15 || chineseDay == 18 || chineseDay == 23 || chineseDay == 24 || chineseDay == 27 || chineseDay == 28 || chineseDay == 29) {
                    AddReligiousDay(day, "十斋日")
                }
            }
            //            }

            // 观音斋：（正月）初八日，（二月）初七日、初九日、十九日，（三月）初三日、初六日、十三日，
            // （四月）廿二日，（五月）初三日、十七日，（六月）十六日、十八日、十九日、廿三日，
            // （七月）十三日，（八月）十六日，（九月）十九日、廿三日，（十月）初二日，（十一月）十九日、廿四日，（十二月）廿五日。
//            if (swith_gyz) {
            var ggg = false
            when (chineseMonth) {
                1 -> if (chineseDay == 8) ggg = true
                2 -> when (chineseDay) {
                    7 -> ggg = true
                    9 -> ggg = true
                    19 -> ggg = true
                }
                3 -> when (chineseDay) {
                    3 -> ggg = true
                    6 -> ggg = true
                    13 -> ggg = true
                }
                4 -> if (chineseDay == 22) ggg = true
                5 -> when (chineseDay) {
                    3 -> ggg = true
                    17 -> ggg = true
                }
                6 -> when (chineseDay) {
                    16 -> ggg = true
                    18 -> ggg = true
                    19 -> ggg = true
                    23 -> ggg = true
                }
                7 -> if (chineseDay == 13) ggg = true
                8 -> if (chineseDay == 16) ggg = true
                9 -> when (chineseDay) {
                    19 -> ggg = true
                    23 -> ggg = true
                }
                10 -> if (chineseDay == 2) ggg = true
                11 -> when (chineseDay) {
                    19 -> ggg = true
                    24 -> ggg = true
                }
                12 -> if (chineseDay == 25) ggg = true
            }
            if (ggg) {
                AddReligiousDay(day, "观音斋")
            }
            //            }

            //endregion

            //
            day = day.addDays(1)
            callBack?.execute()
        }
    }

    @Throws(Exception::class)
    private fun loadReligiousDays(year: Int, month: Int, day: Int, callBack: ReligiousCallBack?) {
        //
        loadLunarReligiousDays()
        religiousDays = HashMap<DateTime,String>()
        remarks = HashMap<DateTime,String>()

        //
        val today = DateTime(year, month, day)
        var chufuStartDate = today
        var chufuEndDate = today
        var zhongfuStartDate = today
        var zhongfuEndDate = today
        var mofuStartDate = today
        var mofuEndDate = today

        /// ********二分日********
        /// 春分 雷将发声。犯者生子五官四肢不全。父母有灾。宜从惊蛰节禁起。戒过一月。
        /// 秋分 杀气浸盛。阳气日衰。宜从白露节禁起。戒过一月。
        /// 此二节之前三后三共七日。犯之必得危疾。尤宜切戒。
        var solar = find(today.addDays(-3), today.addDays(3), SolarTerm.春分)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-3).date, "春分前三日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-2).date, "春分前二日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-1).date, "春分前一日。犯之必得危疾。尤宜切戒。\n四离日（春分前一日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "春分日（二分日）。1、雷将发声。犯者生子五官四肢不全。父母有灾。2、犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(1).date, "春分后一日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(2).date, "春分后二日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(3).date, "春分后三日。犯之必得危疾。尤宜切戒。")
        }
        solar = find(today.addDays(-3), today.addDays(3), SolarTerm.秋分)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-3).date, "秋分前三日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-2).date, "秋分前二日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-1).date, "秋分前一日。犯之必得危疾。尤宜切戒。\n四离日（秋分前一日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "秋分日（二分日）。1、杀气浸盛。阳气日衰。2、犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(1).date, "秋分后一日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(2).date, "秋分后二日。犯之必得危疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(3).date, "秋分后三日。犯之必得危疾。尤宜切戒。")
        }
        callBack?.execute()

        /// *******二至日*********
        /// 夏至阴阳相争。死生分判之时。宜从芒种节禁起。戒过一月。
        /// 冬至阴阳相争。死生分判之时。宜从大雪节禁起。戒过一月。
        /// 此二节乃阴阳绝续之交。最宜禁忌。
        /// 此二至节之前三后三共七日。犯之必得急疾。尤宜切戒。
        solar = find(today.addDays(-3), today.addDays(3), SolarTerm.夏至)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-3).date, "夏至前三日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-2).date, "夏至前二日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-1).date, "夏至前一日。犯之必得急疾。尤宜切戒。\n四离日（夏至前一日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "夏至日（二至日）。1、阴阳相争。死生分判之时。2、此日乃阴阳绝续之交。最宜禁忌。3、犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(1).date, "夏至后一日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(2).date, "夏至后二日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(3).date, "夏至后三日。犯之必得急疾。尤宜切戒。")
        }
        callBack?.execute()
        solar = find(today.addDays(-48), today.addDays(3), SolarTerm.冬至)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-3).date, "冬至前三日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-2).date, "冬至前二日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(-1).date, "冬至前一日。犯之必得急疾。尤宜切戒。\n四离日（冬至前一日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "冬至日（二至日）。1、阴阳相争。死生分判之时。2、此日乃阴阳绝续之交。最宜禁忌。3、犯之必得急疾。尤宜切戒。\n冬至半夜子时。犯之主在一年内亡。")
            AddReligiousDay(solar.key.addDays(1).date, "冬至后一日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(2).date, "冬至后二日。犯之必得急疾。尤宜切戒。")
            AddReligiousDay(solar.key.addDays(3).date, "冬至后三日。犯之必得急疾。尤宜切戒。")

            /// 冬至半夜子时，后庚辛日。第三戌日。犯之皆主在一年内亡。
            run {
                var start = solar!!.key.date
                var touch庚 = false
                var touch辛 = false
                var touch戌3 = false
                var count = 1
                while (!touch庚 || !touch辛 || !touch戌3) {
                    start = start.addDays(1)
                    val ganzhi = GanZhi(start, this.solarTermTreeMap)
                    if (!touch庚 && ganzhi.tianGanDay == "庚") {
                        this.AddReligiousDay(start, "冬至后庚日。犯之主在一年内亡。")
                        touch庚 = true
                    }
                    if (!touch辛 && ganzhi.tianGanDay == "辛") {
                        this.AddReligiousDay(start, "冬至后辛日。犯之主在一年内亡。")
                        touch辛 = true
                    }
                    if (!touch戌3 && ganzhi.diZhiDay == "戌") {
                        if (count == 3) {
                            this.AddReligiousDay(start, "冬至后第三戌日。犯之主在一年内亡。")
                            touch戌3 = true
                        }
                        count++
                    }
                }
            }
        }
        callBack?.execute()


        /// 四立日，四绝日 犯之减寿五年。
        solar = find(today.addDays(-60), today.addDays(1), SolarTerm.立春)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-1).date, "立春日前一日（四绝日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "立春日(四立日)。犯之减寿五年。")

            /// 立春后的第五个戊日为春社日。犯之减寿五年。社日受胎者。毛发皆白。
            run {
                var start = solar!!.key.date
                var count = 1
                while (true) {
                    start = start.addDays(1)
                    val ganzhi = GanZhi(start, this.solarTermTreeMap)
                    if (ganzhi.tianGanDay == "戊") {
                        if (count == 5) {
                            this.AddReligiousDay(start, "春社日（立春后第五戊日）。犯之减寿五年。社日受胎者。毛发皆白。")
                            break
                        }
                        count++
                    }
                }
            }
        }
        callBack?.execute()
        solar = find(today, today.addDays(1), SolarTerm.立夏)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-1).date, "立夏日前一日（四绝日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "立夏日(四立日)。犯之减寿五年。")
        }
        callBack?.execute()
        solar = find(today.addDays(-60), today.addDays(1), SolarTerm.立秋)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-1).date, "立秋日前一日（四绝日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "立秋日(四立日)。犯之减寿五年。")

            /// 立秋后的第五个戊日为秋社日。犯之减寿五年。社日受胎者。毛发皆白。
            run {
                var start = solar!!.key.date
                var count = 1
                while (true) {
                    start = start.addDays(1)
                    val ganzhi = GanZhi(start, this.solarTermTreeMap)
                    if (ganzhi.tianGanDay == "戊") {
                        if (count == 5) {
                            this.AddReligiousDay(start, "秋社日（立秋后第五戊日）。犯之减寿五年。社日受胎者。毛发皆白。")
                            break
                        }
                        count++
                    }
                }
            }
        }
        callBack?.execute()
        solar = find(today, today.addDays(1), SolarTerm.立冬)
        if (solar != null) {
            AddReligiousDay(solar.key.addDays(-1).date, "立冬日前一日（四绝日）。犯之减寿五年。")
            AddReligiousDay(solar.key.date, "立冬日(四立日)。犯之减寿五年。")
        }
        callBack?.execute()

        /// 初伏：夏至后第三个庚日起到第四个庚日前一天的一段时间叫初伏，也叫头伏。犯之减寿一年。
        run {
            solar = find3Fu(year, SolarTerm.夏至)
            var start = solar!!.key.date
            var count = 1
            while (true) {
                start = start.addDays(1)
                val ganzhi = GanZhi(start, this.solarTermTreeMap)
                if (ganzhi.tianGanDay == "庚") {
                    if (count == 3) {
                        chufuStartDate = start
                    } else if (count == 4) {
                        zhongfuStartDate = start
                        chufuEndDate = start.addDays(-1)
                        break
                    }
                    count++
                }
            }
        }
        callBack?.execute()
        /// 中伏：夏至后第四个庚日起到立秋后第一个庚日前的一段时间叫中伏，也叫二伏。犯之减寿一年。
        /// 末伏：立秋后第一个庚日起到第二个庚日前一天的一段时间叫末伏，也叫终伏。犯之减寿一年。
        run {
            solar = find3Fu(year, SolarTerm.立秋)
            var start = solar!!.key.date
            var count = 1
            while (true) {
                start = start.addDays(1)
                val ganzhi = GanZhi(start, this.solarTermTreeMap)
                if (ganzhi.tianGanDay == "庚") {
                    if (count == 1) {
                        mofuStartDate = start
                        zhongfuEndDate = start.addDays(-1)
                    } else if (count == 2) {
                        mofuEndDate = start.addDays(-1)
                        break
                    }
                    count++
                }
            }
        }
        callBack?.execute()
        val ganzhi = GanZhi(today, solarTermTreeMap)
        val lunar = Lunar(today)
        val chineseMonth = lunar.month
        val chineseDay = lunar.day

        //　大小月，大月30天，小月30天
        var maxDay = lunar.day
        var tempday = today.addDays(1)
        var tmpLunar = Lunar(tempday)
        while (tmpLunar.month == lunar.month) {
            maxDay = tmpLunar.day
            tempday = tempday.addDays(1)
            tmpLunar = Lunar(tempday)
        }


        /// 犯之减寿五年。（农历正月十五、七月十五、十月十五，为上中下三元）
        if (chineseDay == 15 && chineseMonth == 1) {
            AddReligiousDay(today, "农历正月十五（三元日）。犯之减寿五年。")
        } else if (chineseDay == 15 && chineseMonth == 7) {
            AddReligiousDay(today, "农历七月十五（三元日）。犯之减寿五年。")
        } else if (chineseDay == 15 && chineseMonth == 10) {
            AddReligiousDay(today, "农历十月十五（三元日）。犯之减寿五年。")
        }

        /// 三伏日
        if (today.compareTo(chufuEndDate.date) <= 0 && today.compareTo(chufuStartDate.date) >= 0) {
            AddReligiousDay(today, "初伏。犯之减寿一年。")
        } else if (today.compareTo(zhongfuEndDate.date) <= 0 && today.compareTo(zhongfuStartDate.date) >= 0) {
            AddReligiousDay(today, "中伏。犯之减寿一年。")
        } else if (today.compareTo(mofuEndDate.date) <= 0 && today.compareTo(mofuStartDate.date) >= 0) {
            AddReligiousDay(today, "末伏。犯之减寿一年。")
        }


        /// 毁败日：大月十八日。小月十七日。犯之得病。
        val tChineseDay = lunar.day
        if (tChineseDay == 17 || tChineseDay == 18) {
            if (maxDay == 30) {
                if (tChineseDay == 18) {
                    AddReligiousDay(today, "毁败日。犯之得病。")
                }
            } else {
                if (tChineseDay == 17) {
                    AddReligiousDay(today, "毁败日。犯之得病。")
                }
            }
        }

        /// 上弦为初七初八，下弦为二十二二十三。犯之减寿一年。
        if (chineseDay == 7 || chineseDay == 8) {
            AddReligiousDay(today, "上弦日。犯之减寿一年。")
        } else if (chineseDay == 22 || chineseDay == 23) {
            AddReligiousDay(today, "下弦日。犯之减寿一年。")
        }
        if (Lunar(today.addDays(1)).day == 1) {
            AddReligiousDay(today, "本月最后一天（晦日）。犯之减寿一年。")
        }

        /// 每月三辛日，犯之减寿一年。
        if (ganzhi.tianGanDay == "辛") {
            AddReligiousDay(today, "每月三辛日。犯之减寿一年。")
        } else if (ganzhi.tianGanDay == "甲" && ganzhi.diZhiDay == "子") {
            AddReligiousDay(today, "甲子日。犯之皆减寿一年。")
        } else if (ganzhi.tianGanDay == "庚" && ganzhi.diZhiDay == "申") {
            AddReligiousDay(today, "庚申日。犯之皆减寿一年。")
        } else if (ganzhi.tianGanDay == "丙" || ganzhi.tianGanDay == "丁") {
            AddReligiousDay(today, "丙丁日。天地仓开日。犯之皆得病。")
        }


        /// 十恶大败日
        /// 甲己年。三月戊戌日。七月癸亥日。十月丙申日。十一月丁亥日。
        /// 乙庚年。四月壬申日。九月乙巳日
        /// 丙辛年。三月辛巳日。九月庚辰日。十月甲辰日。
        /// 戊癸年。六月己丑日。
        /// 此皆大不吉之日。宜戒
        if (((ganzhi.tianGanYear == "甲" || ganzhi.tianGanYear == "己")
                        && (chineseMonth == 3 && ganzhi.tianGanDay == "戊" && ganzhi.diZhiDay == "戌" || chineseMonth == 7 && ganzhi.tianGanDay == "癸" && ganzhi.diZhiDay == "亥" || chineseMonth == 10 && ganzhi.tianGanDay == "丙" && ganzhi.diZhiDay == "申" || chineseMonth == 11 && ganzhi.tianGanDay == "丁" && ganzhi.diZhiDay == "亥")) ||
                ((ganzhi.tianGanYear == "乙" || ganzhi.tianGanYear == "庚")
                        && (chineseMonth == 4 && ganzhi.tianGanDay == "壬" && ganzhi.diZhiDay == "申"
                        || chineseMonth == 9 && ganzhi.tianGanDay == "乙" && ganzhi.diZhiDay == "巳")) ||
                ((ganzhi.tianGanYear == "丙" || ganzhi.tianGanYear == "辛")
                        && (chineseMonth == 3 && ganzhi.tianGanDay == "辛" && ganzhi.diZhiDay == "巳" || chineseMonth == 9 && ganzhi.tianGanDay == "庚" && ganzhi.diZhiDay == "辰" || chineseMonth == 10 && ganzhi.tianGanDay == "甲" && ganzhi.diZhiDay == "辰")) ||
                ((ganzhi.tianGanYear == "戊" || ganzhi.tianGanYear == "癸")
                        && chineseMonth == 6 && ganzhi.tianGanDay == "己" && ganzhi.diZhiDay == "丑")) {
            AddReligiousDay(today, "十恶大败日。此皆大不吉之日。宜戒")
        }

        /// 阴错日
        /// 正月庚戌日 二月辛酉日 三月庚申日 四月丁未日
        /// 五月丙午日 六月丁巳日 七月甲辰日 八月乙卯日
        /// 九月甲寅日 十月癸丑日 十一月壬子日 十二月癸亥日
        /// 此阴不足之日。俱宜戒。
        if (chineseMonth == 1 && ganzhi.tianGanDay == "庚" && ganzhi.diZhiDay == "戌" || chineseMonth == 2 && ganzhi.tianGanDay == "辛" && ganzhi.diZhiDay == "酉" || chineseMonth == 3 && ganzhi.tianGanDay == "庚" && ganzhi.diZhiDay == "申" || chineseMonth == 4 && ganzhi.tianGanDay == "丁" && ganzhi.diZhiDay == "未" || chineseMonth == 5 && ganzhi.tianGanDay == "丙" && ganzhi.diZhiDay == "午" || chineseMonth == 6 && ganzhi.tianGanDay == "丁" && ganzhi.diZhiDay == "巳" || chineseMonth == 7 && ganzhi.tianGanDay == "甲" && ganzhi.diZhiDay == "辰" || chineseMonth == 8 && ganzhi.tianGanDay == "乙" && ganzhi.diZhiDay == "卯" || chineseMonth == 9 && ganzhi.tianGanDay == "甲" && ganzhi.diZhiDay == "寅" || chineseMonth == 10 && ganzhi.tianGanDay == "癸" && ganzhi.diZhiDay == "丑" || chineseMonth == 11 && ganzhi.tianGanDay == "壬" && ganzhi.diZhiDay == "子" || chineseMonth == 12 && ganzhi.tianGanDay == "癸" && ganzhi.diZhiDay == "亥") {
            AddReligiousDay(today, "阴错日。此阴不足之日。俱宜戒。")
        }

        // 五毒月
        if (chineseMonth == 5) {
            AddRemark(today, "注：农历五月俗称五毒月，按此月宜全戒为是。")
        }

        // 农历戒期
        if (lunarReligiousDays!!.containsKey(LunarDate(chineseMonth, chineseDay))) {
            AddReligiousDay(today, lunarReligiousDays!![LunarDate(chineseMonth, chineseDay)]?:"")
        }

        //region 个人相关斋戒日

        /// 祖先亡忌日。父母诞日、忌日。犯之皆减寿一年。
/*            for (MemorialDay memorialDay : memorialDays) {
                if (chineseMonth == memorialDay.getLunarDate().getMonth() && chineseDay == memorialDay.getLunarDate().getDay())
                    this.AddReligiousDay(day, memorialDay.getRelation().toString() + memorialDay.getType() + "。犯之减寿一年。");
            }*/
        // 太岁日。犯之皆减寿一年。
        if ("丑" == ganzhi.diZhiDay) {
            AddReligiousDay(today, "本人太岁日。犯之减寿一年。")
        }
        /// 己身夫妇本命诞日。犯之皆减寿。
        /*       if (!_String.IsNullOrEmpty(this.zodiac2) && this.ZodiacToDizhi(this.zodiac2).equals(ganzhi.getDiZhiDay())) {
                this.AddReligiousDay(day, "配偶太岁日。犯之减寿一年。");
            }*/

        //endregion

        //region 佛教斋戒日

        // 六斋日（每月）初八日、十四日、十五日、廿三日、廿九日、三十日（月小从廿八日起）
/*            if (swith_lzr) {
                if (maxDay == 30) {
                    if (chineseDay == 8 || chineseDay == 14 || chineseDay == 15 || chineseDay == 23 || chineseDay == 29 || chineseDay == 30) {
                        this.AddReligiousDay(day, "六斋日");
                    }
                } else {
                    if (chineseDay == 8 || chineseDay == 14 || chineseDay == 15 || chineseDay == 23 || chineseDay == 28 || chineseDay == 29) {
                        this.AddReligiousDay(day, "六斋日");
                    }
                }
            }*/

        // 十斋日（每月）初一日、初八日、十四日、十五日、十八日、廿三日、廿四日、廿八日、廿九日、三十日（月小从廿七日起）
//            if (swith_szr) {
        if (maxDay == 30) {
            if (chineseDay == 1 || chineseDay == 8 || chineseDay == 14 || chineseDay == 15 || chineseDay == 18 || chineseDay == 23 || chineseDay == 24 || chineseDay == 28 || chineseDay == 29 || chineseDay == 30) {
                AddReligiousDay(today, "十斋日")
            }
        } else {
            if (chineseDay == 1 || chineseDay == 8 || chineseDay == 14 || chineseDay == 15 || chineseDay == 18 || chineseDay == 23 || chineseDay == 24 || chineseDay == 27 || chineseDay == 28 || chineseDay == 29) {
                AddReligiousDay(today, "十斋日")
            }
        }
        //            }

        // 观音斋：（正月）初八日，（二月）初七日、初九日、十九日，（三月）初三日、初六日、十三日，
        // （四月）廿二日，（五月）初三日、十七日，（六月）十六日、十八日、十九日、廿三日，
        // （七月）十三日，（八月）十六日，（九月）十九日、廿三日，（十月）初二日，（十一月）十九日、廿四日，（十二月）廿五日。
//            if (swith_gyz) {
        var ggg = false
        when (chineseMonth) {
            1 -> if (chineseDay == 8) ggg = true
            2 -> when (chineseDay) {
                7 -> ggg = true
                9 -> ggg = true
                19 -> ggg = true
            }
            3 -> when (chineseDay) {
                3 -> ggg = true
                6 -> ggg = true
                13 -> ggg = true
            }
            4 -> if (chineseDay == 22) ggg = true
            5 -> when (chineseDay) {
                3 -> ggg = true
                17 -> ggg = true
            }
            6 -> when (chineseDay) {
                16 -> ggg = true
                18 -> ggg = true
                19 -> ggg = true
                23 -> ggg = true
            }
            7 -> if (chineseDay == 13) ggg = true
            8 -> if (chineseDay == 16) ggg = true
            9 -> when (chineseDay) {
                19 -> ggg = true
                23 -> ggg = true
            }
            10 -> if (chineseDay == 2) ggg = true
            11 -> when (chineseDay) {
                19 -> ggg = true
                24 -> ggg = true
            }
            12 -> if (chineseDay == 25) ggg = true
        }
        if (ggg) {
            AddReligiousDay(today, "观音斋")
        }
        //            }

        //endregion
        callBack?.execute()
    }

    private fun ZodiacToDizhi(zodiac: String): String {
        when (zodiac) {
            "鼠" -> return "子"
            "牛" -> return "丑"
            "虎" -> return "寅"
            "兔" -> return "卯"
            "龙" -> return "辰"
            "蛇" -> return "巳"
            "马" -> return "午"
            "羊" -> return "未"
            "猴" -> return "申"
            "鸡" -> return "酉"
            "狗" -> return "戌"
            "猪" -> return "亥"
        }
        return ""
    }

    /// <summary>
    /// 载入农历戒期。农历戒期，依据农历时间订立，所以每年戒期相对于农历来说，是固定不变的。
    /// </summary>
    private fun loadLunarReligiousDays() {
        lunarReligiousDays = HashMap()
        lunarReligiousDays!![LunarDate(1, 1)] = "天蜡。\n月朔。犯之削禄夺纪。\n玉帝校世人神气禄命。犯之夺纪。"
        lunarReligiousDays!![LunarDate(1, 3)] = "斗降、万神都会。犯之夺纪。"
        lunarReligiousDays!![LunarDate(1, 5)] = "五虚忌。"
        lunarReligiousDays!![LunarDate(1, 6)] = "六耗忌、雷斋日。犯之减寿。"
        lunarReligiousDays!![LunarDate(1, 7)] = "上会日。犯之损寿。"
        lunarReligiousDays!![LunarDate(1, 8)] = "五殿阎罗天子诞。犯之夺纪。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(1, 9)] = "玉皇上帝诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(1, 13)] = "杨公忌。"
        lunarReligiousDays!![LunarDate(1, 14)] = "三元降。犯之减寿。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(1, 15)] = "三元降。犯之减寿。\n月望。犯之减寿。\n上元神会。犯之夺纪。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(1, 16)] = "三元降。犯之减寿。"
        lunarReligiousDays!![LunarDate(1, 19)] = "长春真人诞。"
        lunarReligiousDays!![LunarDate(1, 23)] = "四天王巡行、三尸神奏事。"
        lunarReligiousDays!![LunarDate(1, 25)] = "月晦日。犯之减寿。\n天地仓开日。犯之损寿子带疾。"
        lunarReligiousDays!![LunarDate(1, 27)] = "斗降。犯之夺纪。"
        lunarReligiousDays!![LunarDate(1, 28)] = "人神在阴（宜先一日戒）。犯之得病。"
        lunarReligiousDays!![LunarDate(1, 29)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(1, 30)] = "月晦、司命奏事。犯之减寿。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(2, 1)] = "月朔。\n一殿秦广王诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(2, 2)] = "万神都会。犯之夺纪。\n福德土地正神诞。犯之得祸。"
        lunarReligiousDays!![LunarDate(2, 3)] = "斗降。\n文昌帝君诞。犯之削禄夺纪。"
        lunarReligiousDays!![LunarDate(2, 6)] = "雷斋日。犯之减寿。\n东岳帝君诞。"
        lunarReligiousDays!![LunarDate(2, 8)] = "释迦牟尼佛出日。犯之夺纪。\n宋帝王诞。\n张大帝诞。\n四天王巡行"
        lunarReligiousDays!![LunarDate(2, 11)] = "杨公忌。"
        lunarReligiousDays!![LunarDate(2, 14)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(2, 15)] = "释迦牟尼佛般涅槃。\n月望、太上老君诞。犯之削禄夺纪。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(2, 17)] = "东方杜将军诞。"
        lunarReligiousDays!![LunarDate(2, 18)] = "四殿五官王诞。\n至圣先师孔子讳辰。犯之削禄夺纪。"
        lunarReligiousDays!![LunarDate(2, 19)] = "观音大士诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(2, 21)] = "普贤菩萨诞。"
        lunarReligiousDays!![LunarDate(2, 23)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(2, 25)] = "月晦日。犯之减寿。"
        lunarReligiousDays!![LunarDate(2, 27)] = "斗降。犯之夺纪。"
        lunarReligiousDays!![LunarDate(2, 28)] = "人神在阴。犯之得病。"
        lunarReligiousDays!![LunarDate(2, 29)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(2, 30)] = "月晦、司命奏事。犯之减寿。\n四天王巡行（月小即戒廿九）。"
        lunarReligiousDays!![LunarDate(3, 1)] = "月朔。\n二殿楚江王诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(3, 3)] = "斗降。\n玄天上帝诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(3, 6)] = "雷斋日。犯之减寿。"
        lunarReligiousDays!![LunarDate(3, 8)] = "六殿卞城王诞。犯之夺纪。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(3, 9)] = "牛鬼神出。犯之产恶胎。\n杨公忌。"
        lunarReligiousDays!![LunarDate(3, 12)] = "中央五道诞。"
        lunarReligiousDays!![LunarDate(3, 14)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(3, 15)] = "月望、玄坛诞。犯之夺纪。\n昊天上帝诞。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(3, 16)] = "准提菩萨诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(3, 18)] = "中岳大帝诞、后土娘娘诞、三茅降。"
        lunarReligiousDays!![LunarDate(3, 20)] = "天地仓开日。犯之损寿。\n子孙娘娘诞。"
        lunarReligiousDays!![LunarDate(3, 23)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(3, 25)] = "月晦日。犯之减寿。"
        lunarReligiousDays!![LunarDate(3, 27)] = "斗降。\n七殿泰山王诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(3, 28)] = "人神在阴。犯之得病。\n苍颉至圣先师诞。犯之削禄夺纪。"
        lunarReligiousDays!![LunarDate(3, 29)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(3, 30)] = "月晦、司命奏事。犯之减寿。\n四天王巡行（月小即戒廿九）。"
        lunarReligiousDays!![LunarDate(4, 1)] = "月朔。\n八殿都市王诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(4, 3)] = "斗降。犯之夺纪。"
        lunarReligiousDays!![LunarDate(4, 4)] = "万神善化。犯之失瘏夭胎。\n文殊菩萨诞。"
        lunarReligiousDays!![LunarDate(4, 6)] = "雷斋日。犯之减寿。"
        lunarReligiousDays!![LunarDate(4, 7)] = "南斗北斗西斗同降。犯之减寿。\n杨公忌。" ////
        lunarReligiousDays!![LunarDate(4, 8)] = "释迦牟尼佛诞。犯之夺纪。\n万神善化。犯之失瘏夭胎。\n善恶童子降。犯之血死。\n九殿平等王诞。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(4, 14)] = "纯阳祖师诞。犯之减寿。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(4, 15)] = "月望、钟离祖师诞。犯之夺纪。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(4, 16)] = "天地仓开日。犯之损寿。"
        lunarReligiousDays!![LunarDate(4, 17)] = "十殿转轮王诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(4, 18)] = "天地仓开日。\n紫微大帝诞。犯之减寿。"
        lunarReligiousDays!![LunarDate(4, 20)] = "眼光圣母诞。"
        lunarReligiousDays!![LunarDate(4, 23)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(4, 25)] = "月晦。犯之减寿。"
        lunarReligiousDays!![LunarDate(4, 27)] = "斗降。犯之夺纪。"
        lunarReligiousDays!![LunarDate(4, 28)] = "人神在阴。犯之得病。"
        lunarReligiousDays!![LunarDate(4, 29)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(4, 30)] = "月晦、司命奏事。犯之减寿。\n四天王巡行（逢月小即戒廿九）。"
        lunarReligiousDays!![LunarDate(5, 1)] = "月朔、南极长生大帝诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(5, 3)] = "斗降。犯之夺纪。"
        lunarReligiousDays!![LunarDate(5, 5)] = "地腊。\n五帝校定人官爵。犯之削禄夺纪。\n九毒日。犯之夭亡奇祸不测。\n杨公忌。"
        lunarReligiousDays!![LunarDate(5, 6)] = "九毒日。犯之夭亡奇祸不测。\n雷斋日。"
        lunarReligiousDays!![LunarDate(5, 7)] = "九毒日。犯之夭亡奇祸不测。"
        lunarReligiousDays!![LunarDate(5, 8)] = "南方五道诞、四天王巡行。"
        lunarReligiousDays!![LunarDate(5, 11)] = "天仓开日。犯之损寿。\n天下都城隍诞。"
        lunarReligiousDays!![LunarDate(5, 12)] = "炳灵公诞。"
        lunarReligiousDays!![LunarDate(5, 13)] = "关圣降神。犯之削禄夺纪。"
        lunarReligiousDays!![LunarDate(5, 14)] = "四天王巡行。\n夜子时为天地交泰。犯之三年内夫妇俱亡。"
        lunarReligiousDays!![LunarDate(5, 15)] = "月望。犯之夺纪。\n九毒日。犯之夭亡奇祸不测。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(5, 16)] = "九毒日。\n天地元气造化万物之辰。\n三年内夫妇俱亡。"
        lunarReligiousDays!![LunarDate(5, 17)] = "九毒日。犯之夭亡奇祸不测。"
        lunarReligiousDays!![LunarDate(5, 18)] = "张天师诞。"
        lunarReligiousDays!![LunarDate(5, 22)] = "孝蛾神诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(5, 23)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(5, 25)] = "九毒日。犯之夭亡奇祸不测。\n月晦日。"
        lunarReligiousDays!![LunarDate(5, 26)] = "九毒日。犯之夭亡奇祸不测。"
        lunarReligiousDays!![LunarDate(5, 27)] = "九毒日。犯之夭亡奇祸不测。\n斗降。"
        lunarReligiousDays!![LunarDate(5, 28)] = "人神在阴。犯之得病。"
        lunarReligiousDays!![LunarDate(5, 29)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(5, 30)] = "月晦、司命奏事。犯之减寿。\n四天王巡行（月小即戒廿九）。"
        lunarReligiousDays!![LunarDate(6, 1)] = "月朔。犯之夺纪。"
        lunarReligiousDays!![LunarDate(6, 3)] = "斗降。犯之夺纪。\n杨公忌。"
        lunarReligiousDays!![LunarDate(6, 4)] = "南赡部洲转大法轮。犯之损寿。"
        lunarReligiousDays!![LunarDate(6, 6)] = "天仓开日。\n雷斋日。犯之损寿。"
        lunarReligiousDays!![LunarDate(6, 8)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(6, 10)] = "金粟如来诞。"
        lunarReligiousDays!![LunarDate(6, 13)] = "井泉龙王诞。"
        lunarReligiousDays!![LunarDate(6, 14)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(6, 15)] = "月望。犯之夺纪。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(6, 19)] = "观音大士涅槃（成道日）。犯之夺纪。"
        lunarReligiousDays!![LunarDate(6, 23)] = "南方火神诞。犯之遭回禄。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(6, 24)] = "雷祖诞。\n关帝诞。犯之削禄夺纪。"
        lunarReligiousDays!![LunarDate(6, 25)] = "月晦日。犯之减寿。"
        lunarReligiousDays!![LunarDate(6, 27)] = "斗降。犯之夺纪。"
        lunarReligiousDays!![LunarDate(6, 28)] = "人神在阴。犯之得病。"
        lunarReligiousDays!![LunarDate(6, 29)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(6, 30)] = "月晦。\n司命奏事。犯之减寿。\n四天王巡行（月小即戒廿九）。"
        lunarReligiousDays!![LunarDate(7, 1)] = "月晦、杨公忌。犯之夺纪。"
        lunarReligiousDays!![LunarDate(7, 3)] = "斗降。犯之夺纪。"
        lunarReligiousDays!![LunarDate(7, 5)] = "中会日损寿。\n一作初七。"
        lunarReligiousDays!![LunarDate(7, 6)] = "雷斋日减寿。"
        lunarReligiousDays!![LunarDate(7, 7)] = "道德腊。\n五帝校生人善恶。\n魁星诞削禄。犯之夺纪。"
        lunarReligiousDays!![LunarDate(7, 8)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(7, 10)] = "阴毒日大忌。"
        lunarReligiousDays!![LunarDate(7, 12)] = "长真谭真人诞。"
        lunarReligiousDays!![LunarDate(7, 13)] = "大势至菩萨诞。犯之减寿。"
        lunarReligiousDays!![LunarDate(7, 14)] = "三元降。犯之减寿。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(7, 15)] = "月望、三元降。\n地官校籍。犯之夺纪。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(7, 16)] = "三元降。犯之减寿。"
        lunarReligiousDays!![LunarDate(7, 18)] = "西王母诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(7, 19)] = "太岁诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(7, 22)] = "增福财神诞。犯之削禄夺纪。"
        lunarReligiousDays!![LunarDate(7, 23)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(7, 25)] = "月晦。犯之减寿。"
        lunarReligiousDays!![LunarDate(7, 27)] = "斗降。犯之夺纪。"
        lunarReligiousDays!![LunarDate(7, 28)] = "人神在阴。犯之得病。"
        lunarReligiousDays!![LunarDate(7, 29)] = "杨公忌、四天王巡行。"
        lunarReligiousDays!![LunarDate(7, 30)] = "地藏菩萨诞。犯之夺纪。\n月晦、司命奏事。犯之减寿。\n四天王巡行（月小即戒廿九）。"
        lunarReligiousDays!![LunarDate(8, 1)] = "月朔。犯之夺纪。\n许真君诞。"
        lunarReligiousDays!![LunarDate(8, 3)] = "斗降、北斗诞。犯之削禄夺纪。\n司命灶君诞。犯之遭回禄。"
        lunarReligiousDays!![LunarDate(8, 5)] = "雷声大帝诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(8, 6)] = "雷斋。犯之减寿。"
        lunarReligiousDays!![LunarDate(8, 8)] = "四天王巡行"
        lunarReligiousDays!![LunarDate(8, 10)] = "北斗大帝诞。"
        lunarReligiousDays!![LunarDate(8, 12)] = "西方五道诞。"
        lunarReligiousDays!![LunarDate(8, 14)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(8, 15)] = "月望。\n太阴朝元（宜焚香守夜）。犯之暴亡。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(8, 16)] = "天曹掠刷真君降。犯之贫夭。"
        lunarReligiousDays!![LunarDate(8, 18)] = "天人兴福之辰（宜斋戒，存想吉事）。"
        lunarReligiousDays!![LunarDate(8, 23)] = "四天王巡行。\n汉桓侯张显王诞。"
        lunarReligiousDays!![LunarDate(8, 24)] = "灶君夫人诞。"
        lunarReligiousDays!![LunarDate(8, 25)] = "月晦日。犯之减寿。"
        lunarReligiousDays!![LunarDate(8, 27)] = "斗降。\n至圣先师孔子诞。犯之削禄夺纪。\n杨公忌。"
        lunarReligiousDays!![LunarDate(8, 28)] = "人神在阴。犯之得病。\n四天会事。"
        lunarReligiousDays!![LunarDate(8, 29)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(8, 30)] = "月晦、司命奏事。犯之夺纪。\n诸神考校。犯之夺算。\n四天王巡行（月小即戒廿九）。"
        lunarReligiousDays!![LunarDate(9, 1)] = "月朔、南斗诞。犯之削禄夺纪。\n自初一至初九北斗九星降。犯之夺纪。（此九日俱宜斋戒）。"
        lunarReligiousDays!![LunarDate(9, 2)] = "自初一至初九北斗九星降。犯之夺纪。（此九日俱宜斋戒）。"
        lunarReligiousDays!![LunarDate(9, 3)] = "五瘟神诞。\n自初一至初九北斗九星降。犯之夺纪。（此九日俱宜斋戒）。"
        lunarReligiousDays!![LunarDate(9, 4)] = "自初一至初九北斗九星降。犯之夺纪。（此九日俱宜斋戒）。"
        lunarReligiousDays!![LunarDate(9, 5)] = "自初一至初九北斗九星降。犯之夺纪。（此九日俱宜斋戒）。"
        lunarReligiousDays!![LunarDate(9, 6)] = "自初一至初九北斗九星降。犯之夺纪。（此九日俱宜斋戒）。"
        lunarReligiousDays!![LunarDate(9, 7)] = "自初一至初九北斗九星降。犯之夺纪。（此九日俱宜斋戒）。"
        lunarReligiousDays!![LunarDate(9, 8)] = "四天王巡行。\n自初一至初九北斗九星降。犯之夺纪。（此九日俱宜斋戒）。"
        lunarReligiousDays!![LunarDate(9, 9)] = "斗母诞。削禄夺纪。\n酆都大帝诞、玄天上帝飞升。\n自初一至初九北斗九星降。犯之夺纪。（此九日俱宜斋戒）。"
        lunarReligiousDays!![LunarDate(9, 10)] = "斗母降。犯之夺纪。"
        lunarReligiousDays!![LunarDate(9, 11)] = "宜戒。"
        lunarReligiousDays!![LunarDate(9, 13)] = "孟婆尊神诞。"
        lunarReligiousDays!![LunarDate(9, 14)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(9, 15)] = "月望。犯之夺纪。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(9, 17)] = "金龙四大王诞。犯之水厄。"
        lunarReligiousDays!![LunarDate(9, 19)] = "日宫月宫会合。\n观世音菩萨出家日。犯之减寿。"
        lunarReligiousDays!![LunarDate(9, 23)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(9, 25)] = "月晦日。犯之减寿。\n杨公忌。"
        lunarReligiousDays!![LunarDate(9, 27)] = "斗降。犯之夺纪。"
        lunarReligiousDays!![LunarDate(9, 28)] = "人神在阴。犯之得病。"
        lunarReligiousDays!![LunarDate(9, 29)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(9, 30)] = "药师琉璃光佛诞危疾。\n月晦日 司命奏事。犯之减寿。\n四天王巡行（月小即戒廿九）。"
        lunarReligiousDays!![LunarDate(10, 1)] = "月晦、民岁腊。犯之夺纪。\n四天王降一年内死。"
        lunarReligiousDays!![LunarDate(10, 3)] = "斗降、三茅诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(10, 5)] = "下会日。犯之损寿。\n达摩祖师诞。"
        lunarReligiousDays!![LunarDate(10, 6)] = "天曹考察。犯之夺纪。"
        lunarReligiousDays!![LunarDate(10, 8)] = "佛涅槃日大忌色欲、四天王巡行。"
        lunarReligiousDays!![LunarDate(10, 10)] = "四天王降一年内死。"
        lunarReligiousDays!![LunarDate(10, 11)] = "宜戒。"
        lunarReligiousDays!![LunarDate(10, 14)] = "三元降减寿。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(10, 15)] = "月望、三元降。\n下元水府校籍。犯之夺纪。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(10, 16)] = "三元降。犯之减寿。"
        lunarReligiousDays!![LunarDate(10, 23)] = "杨公忌、四天王巡行。"
        lunarReligiousDays!![LunarDate(10, 25)] = "月晦日。犯之减寿。"
        lunarReligiousDays!![LunarDate(10, 27)] = "斗降。犯之夺纪。\n北极紫薇大帝降。"
        lunarReligiousDays!![LunarDate(10, 28)] = "人神在阴。犯之得病。"
        lunarReligiousDays!![LunarDate(10, 29)] = "四天王巡行"
        lunarReligiousDays!![LunarDate(10, 30)] = "月晦日、司命奏事。犯之减寿。\n四天王巡行（月小即戒廿九）。"
        lunarReligiousDays!![LunarDate(11, 1)] = "月朔。犯之夺纪。"
        lunarReligiousDays!![LunarDate(11, 3)] = "斗降。犯之夺纪。"
        lunarReligiousDays!![LunarDate(11, 4)] = "至圣先师孔子诞削禄。犯之夺纪。"
        lunarReligiousDays!![LunarDate(11, 6)] = "西岳大帝诞。犯之削禄夺纪。"
        lunarReligiousDays!![LunarDate(11, 8)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(11, 11)] = "天仓开日。\n太乙救苦天尊诞。犯之夺纪。"
        lunarReligiousDays!![LunarDate(11, 14)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(11, 15)] = "月望。\n四天王巡行。\n上半夜犯，男死。\n下半夜犯，女死。"
        lunarReligiousDays!![LunarDate(11, 17)] = "阿弥陀佛诞。"
        lunarReligiousDays!![LunarDate(11, 19)] = "太阳日宫诞奇祸。"
        lunarReligiousDays!![LunarDate(11, 21)] = "杨公忌。"
        lunarReligiousDays!![LunarDate(11, 23)] = "张仙诞。犯之绝嗣。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(11, 25)] = "掠刷大夫降。犯之大凶。\n月晦日。"
        lunarReligiousDays!![LunarDate(11, 26)] = "北方五道诞。"
        lunarReligiousDays!![LunarDate(11, 27)] = "斗降。犯之夺纪。"
        lunarReligiousDays!![LunarDate(11, 28)] = "人神在阴。犯之得病。"
        lunarReligiousDays!![LunarDate(11, 29)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(11, 30)] = "月晦、司命奏事。犯之减寿。\n四天王巡行（月小即戒廿九）。"
        lunarReligiousDays!![LunarDate(12, 1)] = "月朔。犯之夺纪。"
        lunarReligiousDays!![LunarDate(12, 3)] = "斗降。犯之夺纪。"
        lunarReligiousDays!![LunarDate(12, 6)] = "天仓开日。\n雷斋日。犯之减寿。"
        lunarReligiousDays!![LunarDate(12, 7)] = "掠刷大夫将。恶疾。" /////////////////
        lunarReligiousDays!![LunarDate(12, 8)] = "王侯腊。犯之夺纪。\n释迦如来成道日。\n四天王巡行。\n初旬内戊日。"
        lunarReligiousDays!![LunarDate(12, 12)] = "太素三元君朝真。"
        lunarReligiousDays!![LunarDate(12, 14)] = "四天王巡行。"
        lunarReligiousDays!![LunarDate(12, 15)] = "月望。犯之夺纪。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(12, 16)] = "南岳大帝诞。"
        lunarReligiousDays!![LunarDate(12, 19)] = "杨公忌。"
        lunarReligiousDays!![LunarDate(12, 20)] = "天地交道。犯之促寿。"
        lunarReligiousDays!![LunarDate(12, 21)] = "天猷上帝诞。"
        lunarReligiousDays!![LunarDate(12, 23)] = "五岳神降。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(12, 24)] = "司命朝天奏人善恶。犯之大祸。"
        lunarReligiousDays!![LunarDate(12, 25)] = "三清玉帝同降考察善恶。犯之奇祸。"
        lunarReligiousDays!![LunarDate(12, 27)] = "斗降。犯之夺纪。"
        lunarReligiousDays!![LunarDate(12, 28)] = "人神在阴。犯之得病。"
        lunarReligiousDays!![LunarDate(12, 29)] = "华严菩萨诞。\n四天王巡行。"
        lunarReligiousDays!![LunarDate(12, 30)] = "诸神下降，察访善恶。犯之男女俱亡。"
    }

    private fun AddReligiousDay(key: DateTime, value: String) {
        if (!religiousDays!!.containsKey(key))
            religiousDays!![key] = value
        else
            religiousDays!![key] = "${religiousDays!![key].toString()}\n${value}"
    }

    private fun AddRemark(key: DateTime, value: String) {
        if (!remarks!!.containsKey(key))
            remarks!![key] = value
        else
            remarks!![key] = "${remarks!![key].toString()}\n${value}"
    }
}