package com.wang17.myphone.widget

import android.content.Context
import android.content.Intent
import android.content.res.Resources.NotFoundException
import android.graphics.Color
import android.util.Log
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.wang17.myphone.R
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.Lunar
import com.wang17.myphone.model.SolarTerm
import com.wang17.myphone.database.Setting
import com.wang17.myphone.util.*
import org.json.JSONException
import java.io.DataInputStream
import java.io.EOFException
import java.text.DecimalFormat
import java.util.*
import android.os.Handler
import com.wang17.myphone.database.DataContext
import kotlin.collections.ArrayList

class MyWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return MyWidgetRemoteViewsFactory(this.applicationContext, intent)
    }

    inner class ToDo {
        constructor() {
            color1 = Color.WHITE
            color2 = Color.WHITE
            color3 = Color.WHITE
            isAlert = false
        }

        constructor(header: String, title: String, summary: String, color1: Int, color2: Int, color3: Int, isAlert: Boolean) {
            this.header = header
            this.title = title
            this.summary = summary
            this.color1 = color1
            this.color2 = color2
            this.color3 = color3
            this.isAlert = isAlert
        }

        constructor(header: String, title: String, summary: String, color: Int, isAlert: Boolean) {
            this.header = header
            this.title = title
            this.summary = summary
            color1 = color
            color2 = color
            color3 = color
            this.isAlert = isAlert
        }

        constructor(header: String, title: String, summary: String, color: Int, isAlert: Boolean, rawId: Int) {
            this.header = header
            this.title = title
            this.summary = summary
            color1 = color
            color2 = color
            color3 = color
            this.isAlert = isAlert
            this.rawId = rawId
        }


        var header: String
        var title: String
        var summary: String
        var color1: Int
        var color2: Int
        var color3: Int
        var isAlert: Boolean
        var rawId: Int

        init {
            header = ""
            title = ""
            summary = ""
            rawId = R.raw.ding
        }
    }

    /**
     * onCreate 当第一次创建appwidget时会调用它。
     * onDataSetChanged 只要appwidget被更新就会调用。
     * getCount返回光标中的记录数。（在我们的例子中，就是需要在应用程序小部件中显示的任务项目的数量）
     * getViewAt处理所有的处理工作。它返回一个RemoteViews对象，在我们的例子中是单个列表项。
     * getViewTypeCount返回ListView 中views的类型个数，在我们的例子中，我们在每个ListView项目中都是相同的视图类型，所以我们return 1在那里。
     */
    inner class MyWidgetRemoteViewsFactory(applicationContext: Context?, intent: Intent?) : RemoteViewsFactory {

        private lateinit var mContext: Context
        private lateinit var dc: DataContext

        private var solarTermMap: TreeMap<DateTime, SolarTerm>
        private var mToDoList: MutableList<ToDo>
        private var uiHandler = Handler()

        private var factoryCount = 0
        private var createCount = 0

        init {
            mToDoList = ArrayList()
            solarTermMap = loadJavaSolarTerms(R.raw.solar_java_50)
        }

        override fun onCreate() {
            createCount++
        }

        private fun e(msg: String) {
            Log.e("wangsc", msg)
        }

        override fun onDataSetChanged() {
            try {
                dc = DataContext(mContext)
                if (MyWidgetProvider.isStockList) {
                    var time = ""
                    for (info in MyWidgetProvider.stockInfoList) {
                        var color: Int
                        var color1: Int
                        time = info.time
                        color1 = if (info.type == -1) {
                            Color.GREEN
                        } else {
                            Color.RED
                        }
                        color = if (info.increase > 0.toBigDecimal()) {
                            Color.RED
                        } else if (info.increase == 0.0.toBigDecimal()) {
                            Color.WHITE
                        } else {
                            Color.GREEN
                        }
                        val isListStock = dc.getSetting(Setting.KEYS.is_widget_list_stock, true).boolean
                        val format = if (isListStock) "0.00%" else "0"
                        mToDoList.add(ToDo(info.name, DecimalFormat("0.00").format(info.price), DecimalFormat(format).format(info.increase), color1, color, color, false))
                    }
                    mToDoList.add(ToDo("          ", "          ", time, Color.WHITE, false))
                } else {
                    setToDos()
                }

                //region 余额警戒
                if (dc.getSetting(Setting.KEYS.is账户超额提醒, true).boolean) {
                    var balanceStr = dc.getSetting(Setting.KEYS.balanceABC, 0).string.replace(",","")
                    var balanceLowABC = dc.getSetting(Setting.KEYS.balanceLowABC,100).int
                    var balanceLowICBC = dc.getSetting(Setting.KEYS.balanceLowICBC,50).int
                    var balance =balanceStr.toDouble()
                    if (balance > 3000) {
                        mToDoList.add(ToDo("          ", "ABC","          ",  WARNING3_COLOR, true, 0))
                    }else if(balance<balanceLowABC){
                        mToDoList.add(ToDo("          ", "ABC","          ",  WARNING2_COLOR, true, 0))
                    }
                    balanceStr = dc.getSetting(Setting.KEYS.balanceICBC, 0).string.replace(",","")
                    balance =balanceStr.toDouble()
                    if (balance > 2000) {
                        mToDoList.add(ToDo("          ", "ICBC","          ",  WARNING3_COLOR, true, 0))
                    }else if(balance<balanceLowICBC){
                        mToDoList.add(ToDo("          ", "ICBC","          ",  WARNING2_COLOR, true, 0))
                    }
                }
                //endregion

                //region 新信息
                val newMsg = dc.getSetting(Setting.KEYS.wx_new_msg)
                newMsg?.let {
                    mToDoList.add(ToDo("          ", "          ", it.string, WARNING3_COLOR, true, 0))
                }


                //endregion
            } catch (e: JSONException) {
                _Utils.printException(mContext, e)
                e(e.message!!)
            } catch (e: Exception) {
                _Utils.printException(mContext, e)
                e(e.message!!)
            }
        }

        @Throws(Exception::class)
        private fun setToDos() {
            mToDoList.clear()

            val now = DateTime()
            val lunar = Lunar(now)
            mToDoList.add(ToDo("          ", "${now.toShortDateString1()}   ${lunar.monthStr}${lunar.dayStr}   ${lunar.hourStr}时", "          ", Color.WHITE, false))


            val bankToDos = dc.bankToDosMoney
            /**
             * BankToDo
             */
            for (bankToDo in bankToDos) {
                if (bankToDo.money == -1.0) {
                    continue
                }
                val toDo = ToDo()
                val now = DateTime()
                val dayOffset = DateTime.dayOffset(now, bankToDo.dateTime)
                if (dayOffset > dc.getSetting(Setting.KEYS.几天内待办显示, 3).int && bankToDo.money != 0.0) continue
                if (dayOffset < 0) {
                    toDo.header = "+" + -dayOffset
                    toDo.color1 = Color.RED
                    toDo.color2 = Color.RED
                    toDo.color3 = Color.RED
                    toDo.isAlert = true
                } else {
                    when (dayOffset) {
                        0 -> {
                            toDo.header = "今"
                            toDo.color1 = WARNING3_COLOR
                            toDo.color2 = WARNING3_COLOR
                            toDo.color3 = WARNING3_COLOR
                            toDo.isAlert = true
                        }
                        1 -> {
                            toDo.header = "明"
                            toDo.color1 = WARNING2_COLOR
                            toDo.color2 = WARNING2_COLOR
                            toDo.color3 = WARNING2_COLOR
                        }
                        2 -> {
                            toDo.header = "后"
                            toDo.color1 = WARNING1_COLOR
                            toDo.color2 = WARNING1_COLOR
                            toDo.color3 = WARNING1_COLOR
                        }
                        else -> toDo.header = dayOffset.toString() + ""
                    }
                }
                if (bankToDo.money == 0.0) {
                    toDo.color1 = NORMAL_COLOR
                    toDo.color2 = NORMAL_COLOR
                    toDo.color3 = NORMAL_COLOR
                    toDo.isAlert = false
                }
                var formatter = DecimalFormat("#,##0")
                if (bankToDo.money % 1 != 0.0) {
                    formatter = DecimalFormat("#,##0.00")
                }
                toDo.title = bankToDo.bankName
                toDo.summary = formatter.format(bankToDo.money)

                //
                mToDoList.add(toDo)
            }
            /**
             * 账单日
             */
            val today = DateTime.today
//            when (today.day) {
//                9 ->                     // 交行 放款日
//                    mToDoList.add(ToDo("放款", "COMM",
//                            "7086", WARNING3_COLOR, dc.getSetting(Setting.KEYS.is_make_loan_alert, false).boolean))
//                18 ->                     // 工行 放款日
//                    mToDoList.add(ToDo("放款", "ICB",
//                            "0174", WARNING3_COLOR, dc.getSetting(Setting.KEYS.is_make_loan_alert, false).boolean))
//            }

            /**
             * 农历
             */
            val lunar0 = Lunar(today)
            val lunar1 = Lunar(today.addDays(1))
            val lunar2 = Lunar(today.addDays(2))
//            if (lunar0.day == 1) {
//                mToDoList.add(ToDo("今天", "初一", "戒", RELIGIOUS_WARNING_COLOR, false))
//            } else if (lunar0.day == 15) {
//                mToDoList.add(ToDo("今天", "十五", "戒", RELIGIOUS_WARNING_COLOR, false))
//            }

            //region 上坟提醒
            /**
             * 忌日
             */
            if (lunar0.month == 2) {
                when (lunar0.day) {
                    10 -> {
                        mToDoList.add(ToDo("后天", "忌日", "上坟", WARNING1_COLOR, false))
                    }
                    11 -> {
                        mToDoList.add(ToDo("明天", "忌日", "上坟", WARNING2_COLOR, false))
                    }
                    12 -> {
                        mToDoList.add(ToDo("今天", "忌日", "上坟", WARNING3_COLOR, false))
                    }
                }
            }

            /**
             * 清明节
             */
            val qmMap = solarTermMap.filter { m -> m.value == SolarTerm.清明 && m.key.year == today.year }
            val qmDate = qmMap.keys.firstOrNull()
            qmDate?.let {
                when (it.get(Calendar.DAY_OF_YEAR) - today.get(Calendar.DAY_OF_YEAR)) {
                    0 -> {
                        mToDoList.add(ToDo("今天", "清明节", "上坟", WARNING1_COLOR, false))
                    }
                    1 -> {
                        mToDoList.add(ToDo("明天", "清明节", "上坟", WARNING2_COLOR, false))
                    }
                    2 -> {
                        mToDoList.add(ToDo("后天", "清明节", "上坟", WARNING3_COLOR, false))
                    }
                    else -> {

                    }
                }
            }

            /**
             * 鬼节
             */
            if (lunar0.month == 7) {
                when (lunar0.day) {
                    13 -> {
                        mToDoList.add(ToDo("后天", "中元节", "上坟", WARNING1_COLOR, false))
                    }
                    14 -> {
                        mToDoList.add(ToDo("明天", "中元节", "上坟", WARNING2_COLOR, false))
                    }
                    15 -> {
                        mToDoList.add(ToDo("今天", "中元节", "上坟", WARNING3_COLOR, false))
                    }
                }
            }

            /**
             * 十月初一
             */
            if (lunar2.month == 10 && lunar2.day == 1) {
                mToDoList.add(ToDo("后天", "十月初一", "上坟", WARNING1_COLOR, false))
            } else if (lunar1.month == 10 && lunar1.day == 1) {
                mToDoList.add(ToDo("明天", "十月初一", "上坟", WARNING2_COLOR, false))
            } else if (lunar0.month == 10 && lunar0.day == 1) {
                mToDoList.add(ToDo("今天", "十月初一", "上坟", WARNING3_COLOR, false))
            }
            //endregion

            //region 戒期
            /**
             * 戒期
             */
            val latestDay = dc.getSetting(Setting.KEYS.上一次几点保存的小部件戒期信息, 0).int
            if (today.day != latestDay) {

                /**
                 * 备份数据库
                 */
                BackupTask(applicationContext).execute(BackupTask.COMMAND_BACKUP)

                var list_religious = ""
                val religiou = Religious(today.year, today.month, today.day, solarTermMap, null)
                val religiousDays = religiou.religiousDays
                val remarks = religiou.remarks
//                if ((religiousDays.size > 0 || remarks.size > 0)&&mToDoList.size>1) {
//                    mToDoList.add(ToDo("", "", "", Color.WHITE, false))
//                }
                if (religiousDays.size > 0) {
                    for ((key, value) in religiousDays) {
                        if (key.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                            val religious = value.split("\n".toRegex()).toTypedArray()
                            for (str in religious) {
                                var color: Int
                                color = if (findReligiousKeyWord(str) == 1) {
                                    RELIGIOUS_WARNING_COLOR
                                } else {
                                    NORMAL_COLOR
                                }
                                mToDoList.add(ToDo("", str, "", color, false))
                                list_religious += "\n$str"
                            }
                        }
                    }
                }
                if (remarks.size > 0) {
                    for ((key, value) in remarks) {
                        if (key.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                            val rs = value.split("\n").toTypedArray()
                            for (str in rs) {
                                mToDoList.add(ToDo("", str, "", Color.WHITE, false))
                                list_religious += "\n$str"
                            }
                        }
                    }
                }

                if (list_religious.isEmpty()) {
                    list_religious = dc.getSetting(Setting.KEYS.wisdom, "南无阿弥陀佛").string
                    mToDoList.add(ToDo(list_religious, "", "", Color.WHITE, false))
                }

                //
                dc.editSetting(Setting.KEYS.上一次几点保存的小部件戒期信息, today.day)
                dc.editSetting(Setting.KEYS.小部件戒期信息, list_religious)
            } else {
                val religious = dc.getSetting(Setting.KEYS.小部件戒期信息, "").string.split("\n").toTypedArray()
                for (str in religious) {
                    var color: Int
                    color = if (findReligiousKeyWord(str) == 1) {
                        RELIGIOUS_WARNING_COLOR
                    } else {
                        NORMAL_COLOR
                    }
                    mToDoList.add(ToDo("", str, "", color, false))
                }
            }
            //endregion
        }

        override fun onDestroy() {}
        override fun getCount(): Int {
            return mToDoList.size
        }

        override fun getViewAt(position: Int): RemoteViews {
            val remoteViews = RemoteViews(mContext.packageName, R.layout.widget_timer_list_item)
            try {
                val toDo = mToDoList[position]
                e("${toDo.title}   ${toDo.summary}")
                remoteViews.setTextViewText(R.id.textView_days, toDo.header)
                remoteViews.setTextViewText(R.id.tv_name, toDo.title)
                remoteViews.setTextViewText(R.id.textView_money, toDo.summary)
                remoteViews.setTextColor(R.id.textView_days, toDo.color1)
                remoteViews.setTextColor(R.id.tv_name, toDo.color2)
                remoteViews.setTextColor(R.id.textView_money, toDo.color3)
                if (position == 0)
                    remoteViews.setTextViewTextSize(R.id.tv_name, COMPLEX_UNIT_DIP, 10f)
                val fillInIntent = Intent()
                fillInIntent.putExtra("position", position)
                remoteViews.setOnClickFillInIntent(R.id.linear_root, fillInIntent)
                if (toDo.header.isEmpty()) {
                    remoteViews.setViewVisibility(R.id.textView_days, View.GONE)
                } else {
                    remoteViews.setViewVisibility(R.id.textView_days, View.VISIBLE)
                }
                if (toDo.title.isEmpty()) {
                    remoteViews.setViewVisibility(R.id.tv_name, View.GONE)
                } else {
                    remoteViews.setViewVisibility(R.id.tv_name, View.VISIBLE)
                }
                if (toDo.summary.isEmpty()) {
                    remoteViews.setViewVisibility(R.id.textView_money, View.GONE)
                } else {
                    remoteViews.setViewVisibility(R.id.textView_money, View.VISIBLE)
                }
                val now = DateTime()

                if (toDo.isAlert ) {
//                    if(toDo.rawId==0){
//                        _Utils.zhendong(applicationContext,200)
//                    }else{
                        _Utils.zhendong(applicationContext,100)
//                        if(now.hour != dc.getSetting(Setting.KEYS.pre_alert_hour, 0).int&& now.hour >= 7)
//                        _SoundUtils.mediaPlay(applicationContext, toDo.rawId)
//                    }
                }
            } catch (e: Exception) {
                _Utils.printException(mContext, e)
                Log.e("wangsc", e.message!!)
            }
            return remoteViews
        }

        override fun getLoadingView(): RemoteViews? {
            return null
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun hasStableIds(): Boolean {
            return false
        }

        val WARNING3_COLOR = Color.RED
        val WARNING2_COLOR = Color.YELLOW
        val WARNING1_COLOR = Color.GREEN
        val NORMAL_COLOR = Color.WHITE
        val RELIGIOUS_WARNING_COLOR = Color.RED
//        companion object {
//        }

        init {
            try {
                mContext = getApplicationContext()
                /**
                 * 初始化声音
                 * mSoundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
                 * mSoundPool.load(mContext, R.raw.ding, 1);
                 * mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                 * @Override public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                 * isSoundLoaded = true;
                 * }
                 * });
                 */
                factoryCount++

                //
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 读取JAVA结构的二进制节气数据文件。
     *
     * @param resId 待读取的JAVA二进制文件。
     * @return
     */
    @Throws(Exception::class)
    private fun loadJavaSolarTerms(resId: Int): TreeMap<DateTime, SolarTerm> {
        val result = TreeMap<DateTime, SolarTerm>()
        try {
            val dis = DataInputStream(resources.openRawResource(resId))
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
        } catch (e: NotFoundException) {
            Log.i("wangsc", e.message!!)
        } catch (e: Exception) {
            Log.i("wangsc", e.message!!)
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

    private fun findReligiousKeyWord(religious: String): Int {
        return if (religious.contains("俱亡")
                || religious.contains("奇祸")
                || religious.contains("夺纪")
                || religious.contains("大祸")
                || religious.contains("促寿")
                || religious.contains("恶疾")
                || religious.contains("大凶")
                || religious.contains("绝嗣")
                || religious.contains("男死")
                || religious.contains("女死")
                || religious.contains("血死")
                || religious.contains("一年内死")
                || religious.contains("危疾")
                || religious.contains("水厄")
                || religious.contains("贫夭")
                || religious.contains("暴亡")
                || religious.contains("失瘏夭胎")
                || religious.contains("损寿子带疾")
                || religious.contains("阴错日")
                || religious.contains("十恶大败日")
                || religious.contains("一年内亡")
                || religious.contains("必得急疾")
                || religious.contains("生子五官四肢不全。父母有灾")
                || religious.contains("减寿五年")
                || religious.contains("十斋日")
                || religious.contains("恶胎")) {
            1
        } else 0
    }
}