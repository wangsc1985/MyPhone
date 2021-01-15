package com.wang17.myphone.activity

import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.wang17.myphone.R
import com.wang17.myphone.e
import com.wang17.myphone.model.StockInfo
import com.wang17.myphone.model.ViewHolder
import com.wang17.myphone.model.database.Position
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.util.*
import com.wang17.myphone.util.TradeUtils.commission
import com.wang17.myphone.util.TradeUtils.tax
import com.wang17.myphone.util.TradeUtils.transferFee
import com.wang17.myphone.util._LogUtils.log2file
import kotlinx.android.synthetic.main.activity_attention_stock.*
import okhttp3.Request
import okhttp3.Response
import java.text.DecimalFormat
import java.util.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class StockPositionActivity : AppCompatActivity() {
    private var adapter: StockListdAdapter? = null
    private var mDataContext: DataContext? = null
    private var positions: List<Position>? = null
    private var textViewTime: TextView? = null
    private var textViewTotalProfit: TextView? = null

    //    private StockInfo info;
    private var infoList: MutableList<StockInfo>? = null
    private var isSoundLoaded = false
    private var mSoundPool: SoundPool? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attention_stock)
        infoList = ArrayList()
        mDataContext = DataContext(this)
        positions = mDataContext!!.getPositions(0)
        textViewTime = findViewById(R.id.textView_time)
        textViewTotalProfit = findViewById(R.id.textView_totalProfit)
        adapter = StockListdAdapter()
        listView_stocks.setAdapter(adapter)
        actionButton_home.setOnClickListener(View.OnClickListener { finish() })
        actionButton_home.setOnLongClickListener(OnLongClickListener {
            startActivity(Intent(this@StockPositionActivity, StockPositionHistoryActivity::class.java))
            true
        })
        /**
         * 初始化声音
         */
        mSoundPool = SoundPool(10, AudioManager.STREAM_SYSTEM, 5)
        mSoundPool!!.load(this@StockPositionActivity, R.raw.clock, 1)
        mSoundPool!!.setOnLoadCompleteListener { soundPool, sampleId, status -> isSoundLoaded = true }
    }

    override fun onResume() {
        super.onResume()
        Log.e("wangsc", "onResume...")
        startTimer()
        //        _Utils.acquireWakeLock(this, PowerManager.SCREEN_BRIGHT_WAKE_LOCK);
    }

    override fun onDestroy() {
        super.onDestroy()
        //        _Utils.releaseWakeLock(this);
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
        Log.e("wangsc", "onPause...")
    }

    private var timer: Timer? = null
    private fun startTimer() {
        if (timer != null) {
            return
        }
        try {
            timer = Timer()
            timer!!.schedule(object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        reflushSZZS()
                        reflushSZCZ()
                        reflushZXBZ()
                        fillStockInfoList(infoList)
                    }
                }
            }, 0, 5000)
        } catch (e: Exception) {
            _Utils.printException(this, e)
        }
    }

    private fun stopTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer!!.purge()
            timer = null
        }
    }

    interface OnLoadFinishedListener {
        fun onLoadFinished()
    }

    private fun reflushSZZS() {
        val info = StockInfo()
        info.code = "000001"
        info.exchange = "sh"
        fillStockInfo(info, object : OnLoadFinishedListener {
            override fun onLoadFinished() {
                textViewTime!!.text = info.time
                textViewSzzsPrice!!.text = DecimalFormat("0.00").format(info.price)
                textViewSzzsIncrease!!.text = DecimalFormat("0.00%").format(info.increase)
                if (info.increase >= 0) {
                    llayoutSzzs!!.setBackgroundColor(resources.getColor(R.color.a))
                } else {
                    llayoutSzzs!!.setBackgroundColor(resources.getColor(R.color.DARK_GREEN))
                }
            }
        })
    }

    private fun reflushSZCZ() {
        val info = StockInfo()
        info.code = "399001"
        info.exchange = "sz"
        fillStockInfo(info, object : OnLoadFinishedListener {
            override fun onLoadFinished() {
                textViewSzczPrice!!.text = DecimalFormat("0.00").format(info.price)
                textViewSzczIncrease!!.text = DecimalFormat("0.00%").format(info.increase)
                if (info.increase >= 0) {
                    llayoutSzcz!!.setBackgroundColor(resources.getColor(R.color.a))
                } else {
                    llayoutSzcz!!.setBackgroundColor(resources.getColor(R.color.DARK_GREEN))
                }
            }
        })
    }

    private fun reflushZXBZ() {
        val info = StockInfo()
        info.code = "399005"
        info.exchange = "sz"
        fillStockInfo(info, object : OnLoadFinishedListener {
            override fun onLoadFinished() {
                textViewZxbPrice!!.text = DecimalFormat("0.00").format(info.price)
                textViewZxbIncrease!!.text = DecimalFormat("0.00%").format(info.increase)
                if (info.increase >= 0) {
                    llayoutZxbz!!.setBackgroundColor(resources.getColor(R.color.a))
                } else {
                    llayoutZxbz!!.setBackgroundColor(resources.getColor(R.color.DARK_GREEN))
                }
            }
        })
    }

    private fun reflushDQS() {
//        http://hq.sinajs.cn/list=gb_msft （微软）
//        http://hq.sinajs.cn/list=gb_dji （ 道指）
//        http://hq.sinajs.cn/list=gb_ixic （纳斯达克）
//        http://hq.sinajs.cn/list=hkHSI (恒生指数）
    }

    private fun reflushNSDK() {}
    protected inner class StockListdAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return positions!!.size
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int,convertView: View?, parent: ViewGroup): View? {
            var cv = convertView
            try {
                val viewHolder: ViewHolder
                val info = StockInfo()
                val stock = positions!![position]
                if (cv == null) {
                    cv = View.inflate(this@StockPositionActivity, R.layout.inflate_list_item_stock, null)
                    viewHolder = ViewHolder()
                    viewHolder.textViewName = cv.findViewById(R.id.textView_name)
                    viewHolder.textViewCode = cv.findViewById(R.id.textView_code)
                    viewHolder.textViewIncrease = cv.findViewById(R.id.textView_increase)
                    viewHolder.textViewPrice = cv.findViewById(R.id.textView_price)
                    viewHolder.textViewProfit = cv.findViewById(R.id.textView_profit)
                    viewHolder.textViewCost = cv.findViewById(R.id.textView_cost)
                    cv.tag = viewHolder
                    info.name = stock.name
                    info.code = stock.code
                    info.cost = stock.cost
                    info.amount = stock.amount
                    info.exchange = stock.exchange
                    info.viewHolder = viewHolder
                    infoList!!.add(info)
                } else {
                    viewHolder = cv.tag as ViewHolder
                }
                viewHolder.textViewName.text = stock.name
                viewHolder.textViewCode.text = stock.code
                viewHolder.textViewIncrease.text = DecimalFormat("0.00%").format(0)
                viewHolder.textViewPrice.text = "0.00"
                viewHolder.textViewProfit.text = DecimalFormat("0.00%").format(0)
                viewHolder.textViewCost.text = DecimalFormat("0.000").format(0)
            } catch (e: Exception) {
                _Utils.printException(this@StockPositionActivity, e)
            }
            return cv
        }
    }

    /**
     * 向info里面填充StockInfo值，在调用之前，需要info赋值code。
     *
     * @param info
     * @param onLoadFinishedListener
     */
    private fun fillStockInfo(info: StockInfo, onLoadFinishedListener: OnLoadFinishedListener?) {

        //http://hq.sinajs.cn/list=sh601555
        //var hq_str_sh601555="东吴证券,11.290,11.380,11.160,11.350,11.050,11.160,11.170,61431561,687740501.000,3500,11.160,144700,11.150,98500,11.140,78500,11.130,99200,11.120,143700,11.170,99700,11.180,28700,11.190,41500,11.200,41500,11.210,2019-04-17,15:00:00,00";
        Thread(Runnable {
            try {
                val url = "https://hq.sinajs.cn/list=" + info.exchange + info.code
                val client = _OkHttpUtil.client
                val request: Request = Request.Builder().url(url).build()
                var response: Response? = null
                response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    try {
                        val sss = response.body!!.string()
                        val result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",".toRegex()).toTypedArray()
                        if (result.size == 0) {
                            return@Runnable
                        }
                        if (result.size <= 1) return@Runnable
                        val open = result[2].toDouble()
                        info.name = result[0]
                        info.price = result[3].toDouble()
                        info.increase = (info.price - open) / open
                        info.time = result[31]
                        runOnUiThread { onLoadFinishedListener?.onLoadFinished() }
                    } catch (e: Exception) {
                        return@Runnable
                    }
                } else {
                    log2file("获取数据失败...")
                }
            } catch (e: Exception) {

//                    e.printStackTrace();
            }
        }).start()
    }

    private var loadStockCount = 0
    private fun fillStockInfoList(infoList: List<StockInfo>?) {

        //http://hq.sinajs.cn/list=sh601555
        //var hq_str_sh601555="东吴证券,11.290,11.380,11.160,11.350,11.050,11.160,11.170,61431561,687740501.000,3500,11.160,144700,11.150,98500,11.140,78500,11.130,99200,11.120,143700,11.170,99700,11.180,28700,11.190,41500,11.200,41500,11.210,2019-04-17,15:00:00,00";
        Thread(Runnable {
            try {
                val totalProfit = doubleArrayOf(0.0)
                var totalAmount = 0.0
                if (infoList!!.size <= 0) return@Runnable
                for (stockInfo in infoList) {
                    val url = "https://hq.sinajs.cn/list=" + stockInfo.exchange + stockInfo.code
                    val client = _OkHttpUtil.client
                    val request: Request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        try {
                            val sss = response.body!!.string()
                            val result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",".toRegex()).toTypedArray()
                            val open = result[2].toDouble()
                            stockInfo.name = result[0]
                            stockInfo.price = result[3].toDouble()
                            stockInfo.increase = (stockInfo.price - open) / open
                            stockInfo.time = result[31]
                            mTime = stockInfo.time

                            val fee = commission(stockInfo.price,stockInfo.amount)+ tax(-1,stockInfo.price,stockInfo.amount)+ transferFee(stockInfo.price,stockInfo.amount)
                            val increase = ((stockInfo.price - stockInfo.cost)*stockInfo.amount*100 -fee )/ (stockInfo.cost*stockInfo.amount*100)
                            totalProfit[0] += increase * stockInfo.amount * stockInfo.cost * 100
                            totalAmount += stockInfo.amount * stockInfo.cost * 100
                            runOnUiThread {
                                stockInfo.viewHolder.textViewIncrease.text = DecimalFormat("0.00%").format(stockInfo.increase)
                                stockInfo.viewHolder.textViewPrice.text = DecimalFormat("0.00").format(stockInfo.price)
                                if (stockInfo.increase > 0) {
                                    stockInfo.viewHolder.textViewIncrease.setTextColor(Color.RED)
                                    stockInfo.viewHolder.textViewPrice.setTextColor(Color.RED)
                                } else if (stockInfo.increase == 0.0) {
                                    stockInfo.viewHolder.textViewIncrease.setTextColor(Color.WHITE)
                                    stockInfo.viewHolder.textViewPrice.setTextColor(Color.WHITE)
                                } else {
                                    stockInfo.viewHolder.textViewIncrease.setTextColor(Color.GREEN)
                                    stockInfo.viewHolder.textViewPrice.setTextColor(Color.GREEN)
                                }
                                stockInfo.viewHolder.textViewProfit.text = DecimalFormat("0.00%").format(increase)
                                if (increase > 0) {
                                    stockInfo.viewHolder.textViewProfit.setTextColor(Color.RED)
                                } else if (increase == 0.0) {
                                    stockInfo.viewHolder.textViewProfit.setTextColor(Color.WHITE)
                                } else {
                                    stockInfo.viewHolder.textViewProfit.setTextColor(Color.GREEN)
                                }
                                stockInfo.viewHolder.textViewCost.text = DecimalFormat("0.000").format(stockInfo.cost)
                                if (increase > 0) {
                                    stockInfo.viewHolder.textViewCost.setTextColor(Color.RED)
                                } else if (increase == 0.0) {
                                    stockInfo.viewHolder.textViewCost.setTextColor(Color.WHITE)
                                } else {
                                    stockInfo.viewHolder.textViewCost.setTextColor(Color.GREEN)
                                }
                            }
                        } catch (e: Exception) {
                            return@Runnable
                        }
                    } else {
                        log2file("获取数据失败...")
                        return@Runnable
                    }
                }
                val finalTotalAmount = totalAmount
                runOnUiThread(Runnable {
                    val averageTotalProfit: Double
                    if (infoList.size == 0) return@Runnable
                    averageTotalProfit = totalProfit[0] / finalTotalAmount
                    if (Math.abs(averageTotalProfit - preAverageTotalProfit) * 100 > 0.3) {
                        preAverageTotalProfit = averageTotalProfit
                        var msg = DecimalFormat("0.00%").format(averageTotalProfit)
                        if (averageTotalProfit > 0) {
                            msg = "加$msg"
                        }
                        //                                if (!isFirst) {
//                                    _Utils.speaker(AttentionStockActivity.this.getApplicationContext(), msg);
//                                }
                    }
                    isFirst = false
                    if (mDataContext!!.getSetting(Setting.KEYS.is_stock_load_noke, false).boolean && ++loadStockCount >= 20) {
                        if (!isSoundLoaded) {
                            mSoundPool!!.setOnLoadCompleteListener { soundPool, sampleId, status ->
                                isSoundLoaded = true
                                soundPool.play(1, 0.6f, 0.6f, 0, 0, 1f)
                            }
                        } else {
                            mSoundPool!!.play(1, 0.6f, 0.6f, 0, 0, 1f)
                        }
                        loadStockCount = 0
                    }

//                            _Utils.speaker(AttentionStockActivity.this,new DecimalFormat("0.00").format(averageProfit*100));
                    textViewTotalProfit!!.text = DecimalFormat("0.00%").format(averageTotalProfit)
                    if (averageTotalProfit > 0) {
                        textViewTotalProfit!!.setTextColor(Color.RED)
                    } else if (averageTotalProfit == 0.0) {
                        textViewTotalProfit!!.setTextColor(Color.WHITE)
                    } else {
                        textViewTotalProfit!!.setTextColor(Color.GREEN)
                    }
                    if (mTime != preTime) {
                        _AnimationUtils.heartBeat(actionButton_home)
                        preTime = mTime
                    }
                    //                            AnimationUtils.setRorateAnimationOnce(AttentionStockActivity.this, actionButtonHome);
                })
            } catch (e: Exception) {
//                    e.printStackTrace();
//                    _Utils.printException(getApplicationContext(), e);
            }
        }).start()
    }

    private var preTime: String? = null
    private var mTime: String? = null
    private var isFirst = true

    //缩放
    //    public ObjectAnimator scaleY;
    //    public void stopSuofang() {
    //        if (scaleY != null)
    //            scaleY.cancel();
    //    }
    private var preAverageTotalProfit = 0.0
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        try {
            val audio = getSystemService(AUDIO_SERVICE) as AudioManager
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    audio.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_SHOW_UI)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    audio.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_SHOW_UI)
                    return true
                }
                else -> {
                }
            }
        } catch (e: Exception) {
            _Utils.printException(this@StockPositionActivity, e)
        }
        return super.onKeyDown(keyCode, event)
    }
}