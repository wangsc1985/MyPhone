package com.wang17.myphone.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager.WakeLock
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.widget.TextView
import com.wang17.myphone.R
import com.wang17.myphone.activity.StockPositionActivity
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.model.Commodity
import com.wang17.myphone.model.database.Position
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.util.*
import com.wang17.myphone.util._Utils.e
import okhttp3.Request
import java.io.IOException
import java.text.DecimalFormat
import java.util.concurrent.CountDownLatch

/**
 * 股票信息获取服务
 * 打开Notification通知  MediaPlayer循环播放（10秒音乐播放完毕执行任务）
 */
class StockService : Service() {

    //region 悬浮窗
    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var floatingWindowView: View
    //endregion


    private lateinit var mDataContext: DataContext
    private lateinit var mPlayer: MediaPlayer
    private lateinit var positions: MutableList<Position>
    private lateinit var mSoundPool: SoundPool

    private var mainReciver: MainReciver = MainReciver()
    private var isSoundLoaded = false
    private var soundId = 0
    private var loadStockCount = 1
    private var preAverageTotalProfitS = 0.0
    private var preAverageTotalProfitF = 0.0
    private var preTime: Long = 0
    var mTimeS: String = ""
    var mTimeF: String = ""


    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            mDataContext = DataContext(this)
            // TODO: 2019/4/26 test
            //region test
            /**
             * 初始化声音
             */
            mSoundPool = SoundPool(10, AudioManager.STREAM_MUSIC, 5)
            soundId = mSoundPool.load(applicationContext, R.raw.clock, 1)
            mSoundPool.setOnLoadCompleteListener { soundPool, sampleId, status -> isSoundLoaded = true }
            /**
             *
             */
            preAverageTotalProfitS = 0.0
            positions = mDataContext.positions
//            positions = getPositionsFromCloud()
            preTime = System.currentTimeMillis()
            e("position size : ${positions.size}")
            if (positions.size > 0)
                startMediaPlay()

            //region 悬浮窗
            showFloatingWindow()
            //endregion
        } catch (e: Exception) {
            _Utils.printException(applicationContext, e)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun getPositionsFromCloud(): MutableList<Position> {
        val latch = CountDownLatch(1)
        _CloudUtils.getPositions(this, "0088", object : CloudCallback {
            override fun excute(code: Int, result: Any) {
                when (code) {
                    0 -> {
                        positions = result as MutableList<Position>
                    }
                    -1 -> {
                        e(result.toString())
                    }
                    -2 -> {
                        e(result.toString())
                    }
                }
                latch.countDown()
            }
        })
        latch.await()
        return positions
    }

    override fun onCreate() {
        super.onCreate()
        Log.e("wangsc", "stock timer music service on create...")
        try {
//            wakeLock = _Utils.acquireWakeLock(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
//            Log.e("wangsc", "wakeLock:" + wakeLock)
            /**
             * 监听
             */
            val filter = IntentFilter()
            // 监控电量变化
            filter.addAction(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(mainReciver, filter)
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        try {
            //endregion
//            _Utils.releaseWakeLock(applicationContext, wakeLock)
            mPlayer.stop()
            mPlayer.release()
            // 解除监控
            if (mainReciver != null) unregisterReceiver(mainReciver)
            (applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
            //region 悬浮窗
            windowManager.removeView(floatingWindowView)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun showFloatingWindow() {

        //region 悬浮窗
        FloatingWindowService.isStarted = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        layoutParams = WindowManager.LayoutParams()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
        }
        layoutParams.format = PixelFormat.RGBA_8888
        layoutParams.gravity = Gravity.CENTER
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        layoutParams.width = 350
        layoutParams.height = 100
        layoutParams.x = 300
        layoutParams.y = 300
        //endregion

        if (Settings.canDrawOverlays(this)) {
            floatingWindowView = View.inflate(this, R.layout.inflate_floating_window, null)
            val zjIndex = floatingWindowView.findViewById<TextView>(R.id.tv_zj)
            val szIndex = floatingWindowView.findViewById<TextView>(R.id.tv_sz)

            zjIndex.text = "0.0"
            szIndex.text = "0.0"

            floatingWindowView.setOnTouchListener(FloatingOnTouchListener())
            windowManager.addView(floatingWindowView, layoutParams)
        }
    }

    inner class FloatingOnTouchListener : OnTouchListener {
        private var x = 0
        private var y = 0
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = event.rawX.toInt()
                    y = event.rawY.toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    val nowX = event.rawX.toInt()
                    val nowY = event.rawY.toInt()
                    val movedX = nowX - x
                    val movedY = nowY - y
                    x = nowX
                    y = nowY
                    layoutParams.x = layoutParams.x + movedX
                    layoutParams.y = layoutParams.y + movedY
                    windowManager.updateViewLayout(view, layoutParams)
                }
                else -> {
                }
            }
            return false
        }
    }

    fun startMediaPlay() {
        try {
            mPlayer = MediaPlayer.create(applicationContext, R.raw.second_10)
            mPlayer.setVolume(0.01f, 0.01f)
            mPlayer.setLooping(false)
            mPlayer.setOnCompletionListener(OnCompletionListener {
                try {
                    if (++loadStockCount >= 10 && mDataContext.getSetting(Setting.KEYS.is_stock_load_noke, false).boolean) {
                        playknock()
                        loadStockCount = 1
                    }
                    fillStockInfoList()
                    mPlayer.start()
                } catch (e: IOException) {
                    _Utils.printException(applicationContext, e)
                }
            })
            mPlayer.start()
        } catch (e: Exception) {
            _Utils.printException(applicationContext, e)
        }
    }

    private fun playknock() {
        try {
            if (!isSoundLoaded) {
                mSoundPool.setOnLoadCompleteListener { soundPool, sampleId, status ->
                    isSoundLoaded = true
                    soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
                }
            } else {
                mSoundPool.play(soundId, 1f, 1f, 0, 0, 1f)
            }
        } catch (e: Exception) {
        }
    }

    private var sizeS = 0
    private var sizeF = 0

    private fun fillStockInfoList() {

        //http://hq.sinajs.cn/list=sh601555
        //var hq_str_sh601555="东吴证券,11.290,11.380,11.160,11.350,11.050,11.160,11.170,61431561,687740501.000,3500,11.160,144700,11.150,98500,11.140,78500,11.130,99200,11.120,143700,11.170,99700,11.180,28700,11.190,41500,11.200,41500,11.210,2019-04-17,15:00:00,00";
        Thread(Runnable {
            try {
                var totalProfitS = 0.0
                var totalAmountS = 0.0
                var totalProfitF = 0.0
                sizeS = 0
                sizeF = sizeS
                if (positions.size <= 0) return@Runnable
                for (position in positions) {
                    if (position.type == 0) {
                        /**
                         * 股票
                         */
                        sizeS++
                        val urlS = "https://hq.sinajs.cn/list=" + position.exchange + position.code
                        e("url : $urlS")
                        val clientS = _OkHttpUtil.client
                        val requestS = Request.Builder().url(urlS).build()
                        val responseS = clientS.newCall(requestS).execute()
                        if (responseS.isSuccessful) {
                            try {
                                val sss = responseS.body!!.string()
                                e("数据：$sss")
                                val result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",".toRegex()).toTypedArray()
                                val price = result[3].toDouble()
                                val profit = (price - position.cost) / position.cost
                                mTimeS = result[31]
                                totalProfitS += profit * position.amount * position.cost * 100
                                totalAmountS += position.amount * position.cost * 100
                            } catch (e: Exception) {
                                Log.e("wangsc", "数据错误...")
                                mDataContext.addLog("StockMediaTimerServic", "数据错误...", "")
                                return@Runnable
                            }
                        } else {
                            Log.e("wangsc", "数据错误...")
                            mDataContext.addLog("StockMediaTimerServic", "数据错误...", "")
                        }
                    } else {
                        /**
                         * 期货
                         */
                        sizeF++
                        val url = "https://hq.sinajs.cn/list=" + position.code
                        e("url : $url")
                        val client = _OkHttpUtil.client
                        val request = Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            try {
                                val sss = response.body!!.string()
                                e("数据：$sss")
                                val result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",".toRegex()).toTypedArray()
                                val price = result[8].toDouble()
                                val profit = position.type * (price - position.cost)
                                mTimeF = result[1]
                                mTimeF = mTimeF.substring(0, 2) + ":" + mTimeF.substring(2, 4) + ":" + mTimeF.substring(4, 6)
                                val commodity = findCommodity(position.code)
                                totalProfitF += profit * position.amount * commodity!!.unit
                            } catch (e: Exception) {
                                Log.e("wangsc", "数据错误...")
                                mDataContext.addLog("StockMediaTimerServic", "数据错误...", "")
                                return@Runnable
                            }
                        } else {
                            Log.e("wangsc", "获取数据失败...")
                            mDataContext.addLog("StockMediaTimerServic", "获取数据失败...", "")
                        }
                    }
                }

                //region 上证指数
                val urlS = "https://hq.sinajs.cn/list=sh000001"
                var szPrice = 0.0
                var szIncrease = 0.0
                val clientS = _OkHttpUtil.client
                val requestS = Request.Builder().url(urlS).build()
                val responseS = clientS.newCall(requestS).execute()
                if (responseS.isSuccessful) {
                    try {
                        val sss = responseS.body!!.string()
                        val result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",".toRegex()).toTypedArray()
                        szPrice = result[3].toDouble()
                        val open = result[2].toDouble()
                        szIncrease = (szPrice - open) / open
                        Log.e("wangsc", "open: $open , price: $szPrice , increase: $szIncrease")
                        mTimeS = result[31]
                    } catch (e: Exception) {
                        Log.e("wangsc", "数据错误...")
                        mDataContext.addLog("StockMediaTimerServic", "数据错误...", "")
                        return@Runnable
                    }
                } else {
                    Log.e("wangsc", "数据错误...")
                    mDataContext.addLog("StockMediaTimerServic", "数据错误...", "")
                    return@Runnable
                }
                //endregion
                var speakMsg = ""
                //region 股票平均盈利
                var averageTotalProfitS = 0.0
                var msgS = ""
                if (sizeS > 0) {
                    averageTotalProfitS = totalProfitS / totalAmountS
                    msgS = DecimalFormat("0.00").format(averageTotalProfitS * 100)
                    Log.e("wangsc", "averageTotalProfitS: $msgS")
                    if (Math.abs(averageTotalProfitS - preAverageTotalProfitS) * 100 > 1.0 / sizeS) {
                        preAverageTotalProfitS = averageTotalProfitS
                        //                            if (averageTotalProfitS > 0) {
//                                msgS = "A" + msgS;
//                            }
                        speakMsg += msgS
                    }
                }
                //endregion


                //region 期货平均盈利
                var averageTotalProfitF = 0.0
                var msgF = ""
                if (sizeF > 0) {
                    averageTotalProfitF = totalProfitF / sizeF
                    msgF = DecimalFormat("0").format(averageTotalProfitF)
                    Log.e("wangsc", "averageTotalProfit: $msgF")
                    if (Math.abs(averageTotalProfitF - preAverageTotalProfitF) > 20 / sizeF) {
                        preAverageTotalProfitF = averageTotalProfitF
                        //                            if (averageTotalProfitF > 0) {
//                                msgF = "C" + msgF;
//                            }
//                            speakMsg += msgF;
                    }
                }
                //endregion
                if (!speakMsg.isEmpty() && mDataContext.getSetting(Setting.KEYS.is_stock_broadcast, true).boolean) {
                    var pitch = 1.0f
                    var speech = 1.2f
                    if (averageTotalProfitS < 0) {
                        pitch = 0.1f
                        speech = 0.8f
                    }
                    _Utils.speaker(applicationContext, speakMsg, pitch, speech)
                }
                sendNotification(DecimalFormat("0.00").format(szPrice),
                        DecimalFormat("0.00").format(szIncrease * 100),
                        mTimeS, DecimalFormat("0.00").format(averageTotalProfitS * 100),
                        mTimeF, DecimalFormat("#,###").format(averageTotalProfitF))

                uiHandler.post {
                    val zjIndex = floatingWindowView.findViewById<TextView>(R.id.tv_zj)
                    val szIndex = floatingWindowView.findViewById<TextView>(R.id.tv_sz)
//
                    zjIndex.setText(DecimalFormat("0.00").format(averageTotalProfitS * 100))
                    if (averageTotalProfitS > 0) {
                        zjIndex.setTextColor(resources.getColor(R.color.month_text_color))
                    } else if (averageTotalProfitS == 0.0) {
                        zjIndex.setTextColor(Color.WHITE)
                    } else {
                        zjIndex.setTextColor(resources.getColor(R.color.DARK_GREEN))
                    }

                    szIndex.setText(DecimalFormat("0.00").format(szIncrease * 100))

                    if (szIncrease > 0) {
                        szIndex.setTextColor(resources.getColor(R.color.month_text_color))
                    } else if (szIncrease == 0.0) {
                        szIndex.setTextColor(Color.WHITE)
                    } else {
                        szIndex.setTextColor(resources.getColor(R.color.DARK_GREEN))
                    }
                }
            } catch (e: Exception) {
                _Utils.printException(applicationContext, e)
            }
        }).start()
    }

    var uiHandler = Handler()
    private var preBatteryTime: Long = 0

    //region 通知
    private fun sendNotification(szPrice: String, szIncrease: String, sTime: String?, sIncrease: String, fTime: String?, fIncrease: String) {
        _NotificationUtils.sendNotification(NOTIFICATION_ID, applicationContext, R.layout.notification2) { remoteViews ->
            try {
                remoteViews.setTextViewText(R.id.textView_price_sz, szPrice)
                remoteViews.setTextViewText(R.id.textView_increase_sz, szIncrease)
                remoteViews.setTextColor(R.id.textView_increase_sz, if (szIncrease.contains("-")) resources.getColor(R.color.DARK_GREEN) else resources.getColor(R.color.month_text_color))
                remoteViews.setTextViewText(R.id.textView_time_s, sTime)
                remoteViews.setTextViewText(R.id.textView_increase_s, sIncrease)
                remoteViews.setTextColor(R.id.textView_increase_s, if (sIncrease.contains("-")) resources.getColor(R.color.DARK_GREEN) else resources.getColor(R.color.month_text_color))
                remoteViews.setTextViewText(R.id.textView_time_f, fTime)
                remoteViews.setTextViewText(R.id.textView_increase_f, fIncrease)
                remoteViews.setTextColor(R.id.textView_increase_f, if (fIncrease.contains("-")) resources.getColor(R.color.DARK_GREEN) else resources.getColor(R.color.month_text_color))
                val intentS = Intent(applicationContext, StockPositionActivity::class.java)
                val pendingIntentS = PendingIntent.getActivity(applicationContext, 0, intentS, PendingIntent.FLAG_UPDATE_CURRENT)
                remoteViews.setOnClickPendingIntent(R.id.layout_sz, pendingIntentS)
                remoteViews.setOnClickPendingIntent(R.id.layout_s, pendingIntentS)
                val intentF = Intent(applicationContext, StockPositionActivity::class.java)
                val pendingIntentF = PendingIntent.getActivity(applicationContext, 0, intentF, PendingIntent.FLAG_UPDATE_CURRENT)
                remoteViews.setOnClickPendingIntent(R.id.layout_f, pendingIntentF)
                if (sizeF == 0) {
                    remoteViews.setViewVisibility(R.id.layout_f, View.GONE)
                } else {
                    remoteViews.setViewVisibility(R.id.layout_f, View.VISIBLE)
                }
                if (sizeS == 0) {
                    remoteViews.setViewVisibility(R.id.layout_s, View.GONE)
                } else {
                    remoteViews.setViewVisibility(R.id.layout_s, View.VISIBLE)
                }
            } catch (e: Exception) {
            }
        }
    }

    inner class MainReciver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                // TODO Auto-generated method stub
                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val level = intent.getIntExtra("level", 0)
                        val dataLevel = mDataContext.getSetting(Setting.KEYS.battery, 0).int
                        if (dataLevel != level) {
                            val span = System.currentTimeMillis() - if (preBatteryTime == 0L) System.currentTimeMillis() else preBatteryTime
                            mDataContext.addLog("battery", _String.concat("{", level, "%}{", span / 60000, "分", span % 60000 / 1000, "秒}", ""), "")
                            preBatteryTime = System.currentTimeMillis()
                            mDataContext.editSetting(Setting.KEYS.battery, level)
                        }
                    }
                }
            } catch (e: Exception) {
                _Utils.printException(applicationContext, e)
            }
        }
    }

    companion object {
        private var wakeLock: WakeLock? = null

        @JvmStatic
        fun findCommodity(code: String): Commodity? {
            try {
                for (commodity in _Session.commoditys) {
                    if (commodity.item.toLowerCase() == parseItem(code).toLowerCase()) {
                        return commodity
                    }
                }
            } catch (e: Exception) {
            }
            return null
        }

        private fun parseItem(code: String): String {
            val item = StringBuffer()
            try {
                for (i in 0 until code.length) {
                    val c = code[i]
                    if (!Character.isDigit(c)) {
                        item.append(c)
                    } else {
                        break
                    }
                }
            } catch (e: Exception) {
            }
            return item.toString()
        }

        //endregion
        const val NOTIFICATION_ID = 100
    }
}