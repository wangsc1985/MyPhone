package com.wang17.myphone.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import android.widget.RemoteViews
import com.wang17.myphone.MainActivity
import com.wang17.myphone.R
import com.wang17.myphone.activity.ToDoActivity
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.database.PhoneMessage
import com.wang17.myphone.database.Setting
import com.wang17.myphone.model.BankBill
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.StockInfo
import com.wang17.myphone.structure.SmsStatus
import com.wang17.myphone.structure.SmsType
import com.wang17.myphone.util.*
import com.wang17.myphone.util._SinaStockUtils.OnLoadStockInfoListListener
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.*
import java.util.regex.Pattern


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
        markday_color = context.resources.getColor(R.color.month_text_color)
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
            var dataContext = DataContext(context)
//            var date = DateTime(dataContext.getSetting(Setting.KEYS.bank1_date_millis, 0).long)
//            var offset = DateTime.dayOffset(date, DateTime())
//            var dateTimeStr = if (offset == 0) {
//                date.toShortTimeString()
//            } else if (offset == 1) {
//                "昨天"
//            } else if (offset == 2) {
//                "前天"
//            } else {
//                date.toShortDateString1()
//            }
            remoteViews.setTextViewText(R.id.tv_balanceABC, dataContext.getSetting(Setting.KEYS.balanceABC, -1).string)

//            date = DateTime(dataContext.getSetting(Setting.KEYS.bank2_date_millis, 0).long)
//            offset = DateTime.dayOffset(date, DateTime())
//            dateTimeStr = if (offset == 0) {
//                date.toShortTimeString()
//            } else if (offset == 1) {
//                "昨天"
//            } else if (offset == 2) {
//                "前天"
//            } else {
//                date.toShortDateString1()
//            }
            remoteViews.setTextViewText(R.id.tv_balanceICBC, dataContext.getSetting(Setting.KEYS.balanceICBC, -1).string)
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
    @SuppressLint("ServiceCast")
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val action = intent.action
            val dc = DataContext(context)
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_timer)
            val myComponentName = ComponentName(context, MyWidgetProvider::class.java)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            when (action) {
                "android.media.VOLUME_CHANGED_ACTION" -> {
                    val setting = dc.getSetting(Setting.KEYS.buddha_startime)
                    setting?.let {
                        val audio = context.getSystemService(Service.AUDIO_SERVICE) as AudioManager
                        val volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                        dc.editSetting(Setting.KEYS.念佛最佳音量, volume)
                    }
                }
                ACTION_CLICK_LAYOUT_LEFT -> {
                    setWidgetAlertColor(remoteViews)

                    //region 获取股票列表

                    // 更新listview
//                    remoteViews.setTextViewText(R.id.textView_date, "——")
                    remoteViews.setTextViewText(R.id.tv_balanceABC, "——")
                    remoteViews.setTextViewText(R.id.tv_balanceICBC, "——")
                    appWidgetManager.updateAppWidget(myComponentName, remoteViews)

                    // 更新上证指数
                    setSZindex(context, remoteViews, myComponentName, appWidgetManager)


                    val isAllowWidgetListStock = dc.getSetting(Setting.KEYS.is小部件罗列股票, true).boolean

                    val is_list_stock = dc.getSetting(Setting.KEYS.is_widget_list_stock, true).boolean
                    if (isAllowWidgetListStock) {
                        val positions = dc.getPositions(if (is_list_stock) 0 else 1)
                        isStockList = positions.size > 0
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
                    val mainIntent = Intent(context, MainActivity::class.java)
                    mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(mainIntent)
                }
                ACTION_UPDATE_ALL -> {
                    //相当于获得所有本程序创建的appwidget
                    var text = context.resources.getString(R.string.widget_text)
                    var progress = 0
                    val lastMarkDay = dc.getLastMarkDay(UUID.fromString(dc.getSetting(Setting.KEYS.mark_day_focused, _Session.UUID_NULL).string))
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
                    val mIntent = Intent(context, MainActivity::class.java)
                    mIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(mIntent)
                }
                Intent.ACTION_USER_PRESENT -> {
                }
                ACTION_CLICK_TEXT -> try {
                    // 如果显示markday数据
//                    remoteViews.setTextViewText(R.id.textView_date, "——")
                    remoteViews.setTextViewText(R.id.tv_balanceABC, "——")
                    remoteViews.setTextViewText(R.id.tv_balanceICBC, "——")
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
                {
                    val smsIntent = Intent(context, ToDoActivity::class.java)
                    context.startActivity(smsIntent)
                }
                "android.provider.Telephony.SMS_RECEIVED" -> {
                    val objects = intent.extras!!["pdus"] as Array<Any>?
                    e("收到${objects!!.size}条短信")
                    if (objects == null)
                        return

                    var sms = PhoneMessage()
                    for (count in 0 until objects.size) {
                        val bytes = objects[count] as ByteArray
                        val message = SmsMessage.createFromPdu(bytes)
                        val number = message.originatingAddress
                        val content = message.messageBody
                        val dateTime = DateTime(message.timestampMillis)

                        try {
                            if (count == 0) {
                                sms.address = number ?: ""
                                sms.body = content
                                sms.createTime = dateTime
                                sms.type = SmsType.接收到
                                sms.status = SmsStatus.接收
                            } else {
                                sms.body += content
                                sms.createTime = dateTime
                            }

                        } catch (e: Exception) {
                            dc.addRunLog("拦截短信", e.message ?: "")
                        }
                        //                        }
                    }
                    /**
                     * 验证码
                     */
                    if (sms.body.contains("验证码") || sms.body.contains("动态密码")) {
                        _SoundUtils.play(context, R.raw.maopao)

                        var str = ""
                        while(true){
                            var matcher = Pattern.compile("(?<=验证码.{0,2})([0-9]{6})(?![0-9])").matcher(sms.body)
                            if (matcher.find()){
                                str = matcher.group()
                                break
                            }
                            matcher = Pattern.compile("(?<=验证码.{0,2})([0-9]{4})(?![0-9])").matcher(sms.body)
                            if (matcher.find()) {
                                str = matcher.group()
                                break
                            }
                            matcher = Pattern.compile("(?<![0-9])([0-9]{6})(?![0-9])").matcher(sms.body)
                            if (matcher.find()) {
                                str = matcher.group()
                                break
                            }
                            matcher = Pattern.compile("(?<![0-9])([0-9]{4})(?![0-9])").matcher(sms.body)
                            if (matcher.find()) {
                                str = matcher.group()
                                break
                            }
                        }
                        val clipManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("text", str)
                        clipManager.setPrimaryClip(clip)
                        _NotificationUtils.alertNotificationTop(context,str)
                    }

                    when (sms.address) {
                        "95599" -> {
                            var bankBill: BankBill?
                            var balanceStr: String? = null
                            val format = DecimalFormat("#,##0.00")

                            bankBill = ParseCreditCard.parseABC(context, sms.body)
                            if (bankBill != null) {
                                balanceStr = format.format(bankBill.balance)
                            }
                            if (balanceStr != null) {
                                _SoundUtils.play(context, R.raw.maopao)
                                remoteViews.setTextViewText(R.id.tv_balanceABC, balanceStr)
                                appWidgetManager.updateAppWidget(myComponentName, remoteViews)
                                dc.editSetting(Setting.KEYS.balanceABC, balanceStr)
                            }
                        }
                        "95588" -> {
                            var bankBill: BankBill?
                            var balanceStr: String? = null
                            val format = DecimalFormat("#,##0.00")

                            /**
                             * 解析ICBC
                             */
                            bankBill = ParseCreditCard.parseICBC(context, sms.body)
                            if (bankBill != null) {
                                balanceStr = format.format(bankBill.balance)
                            }
                            if (balanceStr != null) {
                                _SoundUtils.play(context, R.raw.maopao)
                                remoteViews.setTextViewText(R.id.tv_balanceICBC, balanceStr)
                                appWidgetManager.updateAppWidget(myComponentName, remoteViews)
                                dc.editSetting(Setting.KEYS.balanceICBC, balanceStr)
                            }
                        }
                    }

                    dc.addPhoneMessage(sms)
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
//        remoteViews.setTextColor(R.id.textView_balance1, Color.WHITE)
//        remoteViews.setTextColor(R.id.textView_balance2, Color.WHITE)
//        remoteViews.setTextColor(R.id.textView_markDay, Color.WHITE)
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
                    remoteViews.setTextViewText(R.id.tv_balanceICBC, DecimalFormat("0.00").format(info.price))
                    remoteViews.setTextViewText(R.id.tv_balanceABC, DecimalFormat("0.00%").format(info.increase))
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
        var markday_color = Color.BLACK
    }
}