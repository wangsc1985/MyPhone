package com.wang17.myphone.widget

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.content.ContextCompat.getSystemService
import android.telephony.SmsMessage
import android.util.Log
import android.widget.RemoteViews
import com.wang17.myphone.MainActivity
import com.wang17.myphone.R
import com.wang17.myphone.activity.ToDoActivity
import com.wang17.myphone.model.BankBill
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.StockInfo
import com.wang17.myphone.model.database.PhoneMessage
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.structure.SmsStatus
import com.wang17.myphone.structure.SmsType
import com.wang17.myphone.util.*
import com.wang17.myphone.util._SinaStockUtils.OnLoadStockInfoListListener
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.*

/**
 * Implementation of App Widget functionality.
 */
class MyWidgetProvider : AppWidgetProvider() {
    private var mContext: Context? = null

    /**
     * 每次窗口小部件被更新都调用一次该方法，或者每次添加小部件时。
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val dataContext = DataContext(context)
        dataContext.editSetting(Setting.KEYS.latest_widget_update_time, DateTime().toTimeString())
//        if (dataContext.getSetting(Setting.KEYS.is_widget_update_noice, true).getBoolean()) {
//            _SoundUtils.play(context, R.raw.hcz)
//        }
        mContext = context
        isStockList = false
        try {
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_timer)
            val intentRootClick = Intent(ACTION_CLICK_ROOT)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intentRootClick, PendingIntent.FLAG_UPDATE_CURRENT)
            remoteViews.setOnClickPendingIntent(R.id.layout_root, pendingIntent)
            val intentTextClick = Intent(ACTION_CLICK_TEXT)
            val pendingIntent2 = PendingIntent.getBroadcast(context, 1, intentTextClick, PendingIntent.FLAG_UPDATE_CURRENT)
            remoteViews.setOnClickPendingIntent(R.id.textView_markDay, pendingIntent2)
            val layoutLeftClick = Intent(ACTION_CLICK_LAYOUT_LEFT)
            val pendingIntent3 = PendingIntent.getBroadcast(context, 2, layoutLeftClick, PendingIntent.FLAG_UPDATE_CURRENT)
            remoteViews.setOnClickPendingIntent(R.id.layout_left, pendingIntent3)
            val layoutRightClick = Intent(ACTION_CLICK_LAYOUT_RIGHT)
            val pendingIntent4 = PendingIntent.getBroadcast(context, 3, layoutRightClick, PendingIntent.FLAG_UPDATE_CURRENT)
            remoteViews.setOnClickPendingIntent(R.id.layout_right, pendingIntent4)

            setWidgetAlertColor(remoteViews)

            //region listView 更新
            val adapterIntent = Intent(context, MyWidgetRemoteViewsService::class.java)
            remoteViews.setRemoteAdapter(R.id.listview_todo, adapterIntent)
            updateListviewBroadcast(context) // 发送listview更新广播。ACTION_UPDATE_LISTVIEW

            //region 点击事件配置
            // 设置响应 “GridView(gridview)” 的intent模板
            // 说明：“集合控件(如GridView、ListView、StackView等)”中包含很多子元素，如GridView包含很多格子。
            //     它们不能像普通的按钮一样通过 setOnClickPendingIntent 设置点击事件，必须先通过两步。
            //        (01) 通过 setPendingIntentTemplate 设置 “intent模板”，这是比不可少的！
            //        (02) 然后在处理该“集合控件”的RemoteViewsFactory类的getViewAt()接口中 通过 setOnClickFillInIntent 设置“集合控件的某一项的数据”
            val listIntent = Intent(ACTION_CLICK_LIST_ITEM)
            val pendingIntent5 = PendingIntent.getBroadcast(context, 4, listIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            remoteViews.setPendingIntentTemplate(R.id.listview_todo, pendingIntent5)
            //endregion

            //endregion
            setSexDays(context, remoteViews) // 设置行房天数。
            setBankBalance(context, remoteViews)

            dataContext.addLog("widget", "widget update", "")
//            ComponentName myComponentName = new ComponentName(context, MyWidgetProvider.class);
            appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
        } catch (e: Exception) {
            _Utils.printException(context, e)
        }
    }

    /**
     * 设置行房天数。
     *
     * @param context
     * @param remoteViews
     */
    @Throws(Exception::class)
    private fun setSexDays(context: Context, remoteViews: RemoteViews) {
        val dataContext = DataContext(context)
        // 计时计日
        var text = context.resources.getString(R.string.widget_text)
        var progress = 0
        var progressMax = 24
        val id = UUID.fromString(dataContext.getSetting(Setting.KEYS.mark_day_focused, _Session.UUID_NULL).string)
        val lastMarkDay = dataContext.getLastMarkDay(id)
        val dayItem = dataContext.getDayItem(id)
        if (lastMarkDay != null) {
            val have = ((System.currentTimeMillis() - lastMarkDay.dateTime.timeInMillis) / 3600000).toInt()
            var day = have / 24
            val hour = have % 24
            if (dayItem != null) {
                when (dayItem.summary) {
                    "天" -> {
                        day = DateTime.dayOffset(lastMarkDay.dateTime, DateTime())
                        progressMax = dayItem.targetInHour / 24
                        progress = day
                    }
                    "当天" -> {
                        day = DateTime.dayOffset(lastMarkDay.dateTime, DateTime()) + 1
                        progressMax = dayItem.targetInHour / 24
                        progress = day
                    }
                    else -> progress = hour
                }
                text = day.toString() + ""
            }
        }
        remoteViews.setTextViewText(R.id.textView_markDay, text)
        markday_color=context.resources.getColor(R.color.month_text_color)
        remoteViews.setTextColor(R.id.textView_markDay, markday_color)
        remoteViews.setProgressBar(R.id.progressBar, progressMax, progress, false)
    }

    /**
     * 从数据库读取所有短信，解析出余额信息。
     *
     * @param context
     * @param remoteViews
     */
    private fun setBankBalance(context: Context, remoteViews: RemoteViews) {
        try {
            // 余额
//            val sms = DataContext(context).getPhoneMessages("95599")
//            var bankBill: BankBill?
//            var balanceStr: String? = null
//            var moneyStr: String? = null
//            var dateTimeStr: String? = null
//            val format = DecimalFormat("#,##0.00")
//            for (model in sms) {
//                bankBill = ParseCreditCard.parseABC(model.body)
//                if (bankBill != null) {
//                    balanceStr = format.format(bankBill.balance)
//                    moneyStr = format.format(bankBill.money)
//                    val offset = DateTime.dayOffset(bankBill.dateTime, DateTime())
//                    dateTimeStr = if (offset == 0) {
//                        bankBill.dateTime.toShortTimeString()
//                    } else if (offset == 1) {
//                        "昨天"
//                    } else if (offset == 2) {
//                        "前天"
//                    } else {
//                        bankBill.dateTime.toShortDateString1()
//                    }
//                }
//            }
//            if (balanceStr != null) {
//                remoteViews.setTextViewText(R.id.textView_money, moneyStr)
            var dataContext = DataContext(context)
            var date = DateTime(dataContext.getSetting(Setting.KEYS.bank1_date_millis, 0).long)
            var offset = DateTime.dayOffset(date, DateTime())
            var dateTimeStr = if (offset == 0) {
                date.toShortTimeString()
            } else if (offset == 1) {
                "昨天"
            } else if (offset == 2) {
                "前天"
            } else {
                date.toShortDateString1()
            }
            remoteViews.setTextViewText(R.id.textView_balance1, dataContext.getSetting(Setting.KEYS.bank1_balance, -1).string)
//            remoteViews.setTextViewText(R.id.textView_date1, dateTimeStr)


            date = DateTime(dataContext.getSetting(Setting.KEYS.bank2_date_millis, 0).long)
            offset = DateTime.dayOffset(date, DateTime())
            dateTimeStr = if (offset == 0) {
                date.toShortTimeString()
            } else if (offset == 1) {
                "昨天"
            } else if (offset == 2) {
                "前天"
            } else {
                date.toShortDateString1()
            }
            remoteViews.setTextViewText(R.id.textView_balance2, dataContext.getSetting(Setting.KEYS.bank2_balance, -1).string)
//            remoteViews.setTextViewText(R.id.textView_date2, dateTimeStr)
//            }
        } catch (e: Exception) {
            _Utils.printException(context, e)
            Log.e("wangsc", e.message!!)
        }
    }

    private fun e(log: Any?) {
        Log.e("wangsc", log.toString())
    }

    /**
     * 发送listview更新广播。
     *
     * @param context
     */
    private fun updateListviewBroadcast(context: Context) {
        val intentListViewUpdate = Intent(ACTION_UPDATE_LISTVIEW)
        intentListViewUpdate.component = ComponentName(context, MyWidgetProvider::class.java)
        context.sendBroadcast(intentListViewUpdate)
    }

    /**
     * 当小部件大小改变时
     */
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    /**
     * 每删除一次窗口小部件就调用一次
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
    }

    /**
     * 仅仅当我们创建第一个widget时才会启动服务，因为onEnabled()只会在第一个widget被创建时才执行。
     *
     * @param context
     */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateListviewBroadcast(context)
    }

    /**
     * 当最后一个该窗口小部件删除时调用该方法
     */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }

    /**
     * 接收窗口小部件发送的广播
     */
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val action = intent.action
            val dataContext = DataContext(context)
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_timer)
            val myComponentName = ComponentName(context, MyWidgetProvider::class.java)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            when (action) {
                "android.media.VOLUME_CHANGED_ACTION" -> {
                    val audio = context.getSystemService(Service.AUDIO_SERVICE) as AudioManager
                   val volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                    dataContext.editSetting(Setting.KEYS.buddha_volume,volume)
                }
                ACTION_CLICK_LAYOUT_LEFT -> {
                    dataContext.addLog("widget", "widget ACTION_CLICK_LAYOUT_LEFT", "")
                    setWidgetAlertColor(remoteViews)

                    //region 获取股票列表

                    // 更新listview
//                    remoteViews.setTextViewText(R.id.textView_date, "——")
                    remoteViews.setTextViewText(R.id.textView_balance1, "——")
                    remoteViews.setTextViewText(R.id.textView_balance2, "——")
                    appWidgetManager.updateAppWidget(myComponentName, remoteViews)

                    // 更新上证指数
                    setSZindex(context, remoteViews, myComponentName, appWidgetManager)


                    val isAllowWidgetListStock = dataContext.getSetting(Setting.KEYS.is_allow_widget_list_stock, true).boolean

                    val is_list_stock = dataContext.getSetting(Setting.KEYS.is_widget_list_stock, true).boolean
                    if (isAllowWidgetListStock) {
                        val positions = dataContext.getPositions(if (is_list_stock) 0 else 1)
                        if (positions.size > 0) {
                            isStockList = true
                        } else {
                            isStockList = false
                        }
                        _SinaStockUtils.getStockInfoList(positions, object : OnLoadStockInfoListListener {
                            override fun onLoadFinished(infoList: List<StockInfo>, totalProfit: BigDecimal, averageProfit: BigDecimal, time: String) {
                                try {
                                    if (stockInfoList.size == 0) {
                                        return
                                    }
                                    if (is_list_stock) {
                                        remoteViews.setTextViewText(R.id.textView_markDay, DecimalFormat("0.00").format(averageProfit * 100.toBigDecimal()))
                                    } else {
                                        remoteViews.setTextViewText(R.id.textView_markDay, time.substring(0, 2) + ":" + time.substring(2, 4))
                                    }
                                    if (totalProfit > 0.toBigDecimal()) {
                                        markday_color = context.resources.getColor(R.color.month_text_color)
                                    } else if (totalProfit == 0.0.toBigDecimal()) {
                                        markday_color = Color.BLACK
                                    } else {
                                        markday_color = context.resources.getColor(R.color.DARK_GREEN)
                                    }
                                    remoteViews.setTextColor(R.id.textView_markDay, markday_color)
                                    appWidgetManager.updateAppWidget(myComponentName, remoteViews)
                                    Companion.stockInfoList = stockInfoList
                                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetManager.getAppWidgetIds(myComponentName), R.id.listview_todo)
                                } catch (e: Exception) {
                                    _Utils.printException(context, e)
                                    Log.e("wangsc", e.message!!)
                                }
                            }
                        })
                    } else {
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetManager.getAppWidgetIds(myComponentName), R.id.listview_todo)
                    }
                }
                ACTION_CLICK_LAYOUT_RIGHT -> {
                    dataContext.addLog("widget", "widget ACTION_CLICK_LAYOUT_RIGHT", "")
                    val mainIntent = Intent(context, MainActivity::class.java)
                    mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(mainIntent)
                }
                ACTION_UPDATE_ALL -> {
                    dataContext.addLog("widget", "widget ACTION_UPDATE_ALL", "")
                    //相当于获得所有本程序创建的appwidget
                    var text = context.resources.getString(R.string.widget_text)
                    var progress = 0
                    val lastMarkDay = dataContext.getLastMarkDay(UUID.fromString(dataContext.getSetting(Setting.KEYS.mark_day_focused, _Session.UUID_NULL).string))
                    if (lastMarkDay != null) {
                        val have = ((DateTime().timeInMillis - lastMarkDay.dateTime.timeInMillis) / 3600000).toInt()
                        val day = have / 24
                        val hour = have % 24
                        text = day.toString() + ""
                        progress = hour
                    }
                    remoteViews.setTextViewText(R.id.textView_markDay, text)

                    markday_color = context.resources.getColor(R.color.month_text_color)
                    remoteViews.setTextColor(R.id.textView_markDay, markday_color)
                    remoteViews.setProgressBar(R.id.progressBar, 24, progress, false)
                    appWidgetManager.updateAppWidget(myComponentName, remoteViews)
                }
                ACTION_CLICK_ROOT -> {
                    dataContext.addLog("widget", "widget ACTION_CLICK_ROOT", "")
                    val mIntent = Intent(context, MainActivity::class.java)
                    mIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(mIntent)
                }
                Intent.ACTION_USER_PRESENT -> {
                    dataContext.addLog("widget", "widget ACTION_USER_PRESENT", "")
                }
                ACTION_CLICK_TEXT -> try {
                    dataContext.addLog("widget", "widget ACTION_CLICK_TEXT", "")

                    // 如果显示markday数据
//                    remoteViews.setTextViewText(R.id.textView_date, "——")
                    remoteViews.setTextViewText(R.id.textView_balance1, "——")
                    remoteViews.setTextViewText(R.id.textView_balance2, "——")
                    appWidgetManager.updateAppWidget(myComponentName, remoteViews)
                    // SZindex
//                        setSZindex(context, remoteViews, myComponentName, appWidgetManager);
                    setSexDays(context, remoteViews)
                    setBankBalance(context, remoteViews)
                    appWidgetManager.updateAppWidget(myComponentName, remoteViews)

                    // 更新 todo
                    isStockList = false
                    setWidgetAlertColor(remoteViews)
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetManager.getAppWidgetIds(myComponentName), R.id.listview_todo)
                } catch (e: Exception) {
                    _Utils.printException(context, e)
                    Log.e("wangsc", e.message!!)
                }
                ACTION_UPDATE_LISTVIEW -> try {
                    dataContext.addLog("widget", "widget ACTION_UPDATE_LISTVIEW", "")

                    setWidgetAlertColor(remoteViews)
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetManager.getAppWidgetIds(myComponentName), R.id.listview_todo)
                } catch (e: Exception) {
                    _Utils.printException(context, e)
                }
                ACTION_CLICK_LIST_ITEM ->
                    /**
                     * 点击列表事件
                     */
                    // TODO: 2020/8/15 这个地方也要判断
//                    if (isStockList == true) {
//                        if (dataContext.getSetting(Setting.KEYS.is_widget_list_stock, true).boolean) {
//                            val smsIntent = Intent(context, StockPositionActivity::class.java)
//                            context.startActivity(smsIntent)
//                        } else {
//                            val smsIntent = Intent(context, FuturePositionActivity::class.java)
//                            context.startActivity(smsIntent)
//                        }
//                    } else
                {
                    dataContext.addLog("widget", "widget ACTION_CLICK_LIST_ITEM", "")
                    val smsIntent = Intent(context, ToDoActivity::class.java)
                    context.startActivity(smsIntent)
                }
                "android.provider.Telephony.SMS_RECEIVED" -> {
                    dataContext.addLog("widget", "widget android.provider.Telephony.SMS_RECEIVED", "")
                    val objects = intent.extras!!["pdus"] as Array<Any>?
                    e("收到短信" + objects!!.size)
                    for (obj in objects) {
                        val bytes = obj as ByteArray
                        val message = SmsMessage.createFromPdu(bytes)
                        val number = message.originatingAddress
                        val content = message.messageBody
                        val dateTime = DateTime(message.timestampMillis)
                        e(number)
                        e(content)
                        e(dateTime.timeInMillis)
                        dataContext.addLog("新信息", number, dateTime.timeInMillis.toString() + "")
                        when (number) {
                            "95599" -> {
                                var bankBill: BankBill?
                                var balanceStr: String? = null
                                var moneyStr: String? = null
                                var dateTimeStr: String? = null
                                val format = DecimalFormat("#,##0.00")


                                bankBill = ParseCreditCard.parseABC(context, content)
                                if (bankBill != null) {
                                    balanceStr = format.format(bankBill.balance)
//                                    moneyStr = format.format(bankBill.money)
//                                    val offset = DateTime.dayOffset(bankBill.dateTime, DateTime())
//                                    dateTimeStr = if (offset == 0) {
//                                        bankBill.dateTime.toShortTimeString()
//                                    } else if (offset == 1) {
//                                        "昨天"
//                                    } else if (offset == 2) {
//                                        "前天"
//                                    } else {
//                                        bankBill.dateTime.toShortDateString1()
//                                    }
                                }
                                if (balanceStr != null) {
//                                _Utils.zhendong3(context);
                                    _SoundUtils.play(context, R.raw.maopao)
//                                    remoteViews.setTextViewText(R.id.textView_money, moneyStr)
                                    remoteViews.setTextViewText(R.id.textView_balance1, balanceStr)
//                                    remoteViews.setTextViewText(R.id.textView_date1, dateTimeStr)
                                    appWidgetManager.updateAppWidget(myComponentName, remoteViews)
                                    dataContext.editSetting(Setting.KEYS.bank1_balance, balanceStr)
//                                    dataContext.editSetting(Setting.KEYS.bank1_date_millis, bankBill.dateTime.timeInMillis)
                                }
                            }
                            "95588" -> {
                                var bankBill: BankBill?
                                var balanceStr: String? = null
                                var moneyStr: String? = null
                                var dateTimeStr: String? = null
                                val format = DecimalFormat("#,##0.00")

                                /**
                                 * 解析ICBC
                                 */
                                bankBill = ParseCreditCard.parseICBC(context, content)
                                if (bankBill != null) {
                                    balanceStr = format.format(bankBill.balance)
//                                    moneyStr = format.format(bankBill.money)
//                                    val offset = DateTime.dayOffset(bankBill.dateTime, DateTime())
//                                    dateTimeStr = if (offset == 0) {
//                                        bankBill.dateTime.toShortTimeString()
//                                    } else if (offset == 1) {
//                                        "昨天"
//                                    } else if (offset == 2) {
//                                        "前天"
//                                    } else {
//                                        bankBill.dateTime.toShortDateString1()
//                                    }
                                }
                                if (balanceStr != null) {
//                                _Utils.zhendong3(context);
                                    _SoundUtils.play(context, R.raw.maopao)
//                                    remoteViews.setTextViewText(R.id.textView_money, moneyStr)
                                    remoteViews.setTextViewText(R.id.textView_balance2, balanceStr)
//                                    remoteViews.setTextViewText(R.id.textView_date2, dateTimeStr)
                                    appWidgetManager.updateAppWidget(myComponentName, remoteViews)
                                    dataContext.editSetting(Setting.KEYS.bank2_balance, balanceStr)
//                                    dataContext.editSetting(Setting.KEYS.bank2_date_millis, bankBill.dateTime.timeInMillis)
                                }
                            }
                        }
                        //                        if (content.contains("银行")|content.contains("元")|content.contains("期货")|content.contains("账单")) {
                        val pm = PhoneMessage()
                        pm.address = number
                        pm.body = content
                        pm.createTime = dateTime
                        pm.type = SmsType.接收到
                        pm.status = SmsStatus.接受
                        dataContext.addPhoneMessage(pm)
                        //                        }
                    }
                }
            }
        } catch (e: Exception) {
            _Utils.printException(context, e)
            Log.e("wangsc", e.message!!)
        }
        super.onReceive(context, intent)
    }

    private fun setWidgetAlertColor(remoteViews: RemoteViews) {
        //region 警戒余额颜色
        remoteViews.setTextColor(R.id.textView_balance1, Color.WHITE)
        remoteViews.setTextColor(R.id.textView_balance2, Color.WHITE)
        remoteViews.setTextColor(R.id.textView_markDay, Color.WHITE)
        //endregion
    }

    @Throws(Exception::class)
    private fun setSZindex(context: Context, remoteViews: RemoteViews, myComponentName: ComponentName, appWidgetManager: AppWidgetManager) {
        val info = StockInfo()
        info.code = "000001"
        info.exchange = "sh"
        _SinaStockUtils.getStockInfo(info, object : _SinaStockUtils.OnLoadStockInfoListener {
            override fun onLoadFinished() {

                try {
                    remoteViews.setTextViewText(R.id.textView_balance2, DecimalFormat("0.00").format(info.price))
                    remoteViews.setTextViewText(R.id.textView_balance1, DecimalFormat("0.00%").format(info.increase))
                    appWidgetManager.updateAppWidget(myComponentName, remoteViews)
                } catch (e: Exception) {
                    _Utils.printException(context, e)
                }
            }
        })
    }

    companion object {
        const val ACTION_UPDATE_ALL = "com.wang17.widget.UPDATE_ALL"
        const val ACTION_UPDATE_LISTVIEW = "com.wang17.widget.UPDATE_LISTVIEW"
        const val ACTION_CLICK_LIST_ITEM = "com.wang17.widget.CLICK_LISTVIEW_ITEM"
        const val ACTION_CLICK_ROOT = "com.wang17.widget.CLICK_ROOT"
        const val ACTION_CLICK_TEXT = "com.wang17.widget.CLICK_TEXT"
        const val ACTION_CLICK_LAYOUT_LEFT = "com.wang17.widget.CLICK_LAYOUT_LEFT"
        const val ACTION_CLICK_LAYOUT_RIGHT = "com.wang17.widget.CLICK_LAYOUT_RIGHT"
        var isStockList = false
        var stockInfoList: List<StockInfo> = ArrayList()
        var markday_color= Color.BLACK
    }
}