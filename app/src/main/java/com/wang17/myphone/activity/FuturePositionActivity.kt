package com.wang17.myphone.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemLongClickListener
import com.wang17.myphone.R
import com.wang17.myphone.model.Commodity
import com.wang17.myphone.model.database.Position
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.util.*
import com.wang17.myphone.util._LogUtils.log2file
import kotlinx.android.synthetic.main.activity_attention_future.*
import okhttp3.Request
import okhttp3.Response
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.*

class FuturePositionActivity : AppCompatActivity() {
    private lateinit var adapter: StockListdAdapter
    private lateinit var mDataContext: DataContext
    private lateinit var positions: List<Position>

    //    private StockInfo info;
    private lateinit var infoList: MutableList<StockInfo>
    private var isSoundLoaded = false
    private lateinit var mSoundPool: SoundPool
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attention_future)
        infoList = ArrayList()
        mDataContext = DataContext(this)
        positions = mDataContext!!.getPositions(1)
        Log.e("wangsc", " stock size: " + positions.size)
        adapter = StockListdAdapter()
        listView_stocks.setAdapter(adapter)
        listView_stocks.setOnItemLongClickListener(OnItemLongClickListener { parent, view, position, id ->
            eidtStockDialog(positions.get(position))
            true
        })
        actionButton_home.setOnClickListener(View.OnClickListener { finish() })
        actionButton_home.setOnLongClickListener(OnLongClickListener {
            addStockDialog()
            true
        })
        /**
         * 初始化声音
         */
        mSoundPool = SoundPool(10, AudioManager.STREAM_SYSTEM, 5)
        mSoundPool!!.load(this@FuturePositionActivity, R.raw.clock, 1)
        mSoundPool!!.setOnLoadCompleteListener { soundPool, sampleId, status -> isSoundLoaded = true }
    }

    fun addStockDialog() {
        val view = View.inflate(this, R.layout.inflate_dialog_add_futures, null)
        val dialog = AlertDialog.Builder(this).create()
        dialog.setView(view)
        dialog.setTitle("合约信息")
        val editTextCode = view.findViewById<EditText>(R.id.editText_code)
        val editTextCost = view.findViewById<EditText>(R.id.editText_cost)
        val editTextAmount = view.findViewById<EditText>(R.id.editText_amount)
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "多单") { dialog, which ->
            try {
                val info: StockInfo = StockInfo()
                info.code = editTextCode.text.toString().toUpperCase()
                info.amount = editTextAmount.text.toString().toInt()
                fillStockInfo(info, object : OnLoadFinishedListener {
                    override fun onLoadFinished() {
                        if (info.name!!.isEmpty()) {
                            Snackbar.make(editTextCode, "合约代码不存在", Snackbar.LENGTH_SHORT).show()
                            return
                        }
                        val position = Position()
                        position.name = info.name!!
                        position.cost = BigDecimal(editTextCost.text.toString())
                        position.code = info.code!!
                        position.type = 1
                        position.amount = info.amount
                        mDataContext!!.addPosition(position)
                        positions = mDataContext!!.getPositions(1)
                        infoList!!.clear()
                        adapter!!.notifyDataSetChanged()
                        Log.e("wangsc", "添加合约成功")
                        dialog.dismiss()
                    }
                })
            } catch (e: NumberFormatException) {
                Snackbar.make(editTextCode, "成本价必须为数字", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                _Utils.printException(this@FuturePositionActivity, e)
            }
        }
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "空单") { dialog, which ->
            try {
                val info: StockInfo = StockInfo()
                info.code = editTextCode.text.toString().toUpperCase()
                info.amount = editTextAmount.text.toString().toInt()
                fillStockInfo(info, object : OnLoadFinishedListener {
                    override fun onLoadFinished() {
                        if (info.name!!.isEmpty()) {
                            Snackbar.make(editTextCode, "合约代码不存在", Snackbar.LENGTH_SHORT).show()
                            return
                        }
                        val position = Position()
                        position.name = info.name!!
                        position.cost = BigDecimal(editTextCost.text.toString())
                        position.code = info.code!!
                        position.type = -1
                        position.amount = info.amount
                        mDataContext!!.addPosition(position)
                        positions = mDataContext!!.getPositions(1)
                        infoList!!.clear()
                        adapter!!.notifyDataSetChanged()
                        Log.e("wangsc", "添加合约成功")
                        dialog.dismiss()
                    }
                })
            } catch (e: NumberFormatException) {
                Snackbar.make(editTextCode, "成本价必须为数字", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                _Utils.printException(this@FuturePositionActivity, e)
            }
        }
        dialog.show()
    }

    fun eidtStockDialog(position: Position) {
        val view = View.inflate(this, R.layout.inflate_dialog_add_futures, null)
        val dialog = AlertDialog.Builder(this).create()
        dialog.setView(view)
        dialog.setTitle("合约信息")
        val editTextCode = view.findViewById<EditText>(R.id.editText_code)
        val editTextCost = view.findViewById<EditText>(R.id.editText_cost)
        val editTextAmount = view.findViewById<EditText>(R.id.editText_amount)
        editTextCode.setText(position.code)
        editTextCost.setText(position.cost.toString() + "")
        editTextCode.isEnabled = false
        editTextAmount.setText(position.amount.toString())
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "删除") { dialog, which ->
            android.support.v7.app.AlertDialog.Builder(this@FuturePositionActivity).setMessage("确认要删除当前合约吗？").setPositiveButton("是") { dialog, which ->
                mDataContext!!.deletePosition(position.id)
                positions = mDataContext!!.getPositions(1)
                infoList!!.clear()
                adapter!!.notifyDataSetChanged()
            }.setNegativeButton("否", null).show()
        }
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "更新") { dialog, which ->
            try {
                val info: StockInfo = StockInfo()
                info.code = editTextCode.text.toString()
                info.amount = editTextAmount.text.toString().toInt()
                fillStockInfo(info, object : OnLoadFinishedListener {
                    override fun onLoadFinished() {
                        if (info.name!!.isEmpty()) {
                            Snackbar.make(editTextCode, "合约代码不存在", Snackbar.LENGTH_SHORT).show()
                            return
                        }
                        position.name = info.name!!
                        position.cost = BigDecimal(editTextCost.text.toString())
                        position.code = info.code!!
                        position.amount = info.amount
                        mDataContext!!.editPosition(position)
                        positions = mDataContext!!.getPositions(1)
                        infoList!!.clear()
                        adapter!!.notifyDataSetChanged()
                        Log.e("wangsc", "添加合约成功")
                        dialog.dismiss()
                    }
                })
            } catch (e: NumberFormatException) {
                Snackbar.make(editTextCode, "成本价必须为数字", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                _Utils.printException(this@FuturePositionActivity, e)
            }
        }
        dialog.show()
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
                    runOnUiThread { fillStockInfoList(infoList) }
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

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            var convertView = convertView
            try {
                val viewHolder: ViewHolder
                val info: StockInfo = StockInfo()
                val stock = positions!![position]
                Log.e("wangsc", "attention future activity adapter stock name: " + stock.name)
                if (convertView == null) {
                    convertView = View.inflate(this@FuturePositionActivity, R.layout.inflate_list_item_stock, null)
                    viewHolder = ViewHolder()
                    viewHolder.textViewName = convertView.findViewById(R.id.textView_name)
                    viewHolder.textViewCode = convertView.findViewById(R.id.textView_code)
                    viewHolder.textViewIncrease = convertView.findViewById(R.id.textView_increase)
                    viewHolder.textViewProfit = convertView.findViewById(R.id.textView_profit)
                    convertView.tag = viewHolder
                    info.name = stock.name
                    info.code = stock.code
                    info.cost = stock.cost
                    info.amount = stock.amount
                    info.viewHolder = viewHolder
                    info.type = stock.type
                    infoList!!.add(info)
                } else {
                    viewHolder = convertView.tag as ViewHolder
                }
                Log.e("wangsc", "++++++++++++++++++++++++++ " + info.name)
                viewHolder.textViewName!!.text = stock.name
                viewHolder.textViewCode!!.text = stock.code
                viewHolder.textViewIncrease!!.text = DecimalFormat("0.00%").format(0)
                viewHolder.textViewProfit!!.text = DecimalFormat("0.00").format(0)
                if (stock.type == 1) {
                    viewHolder.textViewCode!!.setTextColor(Color.RED)
                } else {
                    viewHolder.textViewCode!!.setTextColor(Color.GREEN)
                }
            } catch (e: Exception) {
                _Utils.printException(this@FuturePositionActivity, e)
            }
            return convertView
        }
    }

    internal inner class ViewHolder {
        //条目的布局文件中有什么组件，这里就定义有什么属性
        var textViewName: TextView? = null
        var textViewCode: TextView? = null
        var textViewIncrease: TextView? = null
        var textViewProfit: TextView? = null
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
                val url = "https://hq.sinajs.cn/list=" + info.code
                Log.e("wangsc", "code: " + info.code + "" + url)
                val client = _OkHttpUtil.client
                val request = Request.Builder().url(url).build()
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
                        val open = result[2].toBigDecimal()
                        info.name = result[0]
                        info.price = result[8].toBigDecimal()
                        info.increase = (info.price!! - open) / open
                        info.time = result[1]
                        runOnUiThread { onLoadFinishedListener?.onLoadFinished() }
                    } catch (e: Exception) {
                        _Utils.printException(this@FuturePositionActivity, e)
                        return@Runnable
                    }
                } else {
                    log2file("获取数据失败...")
                }
            } catch (e: Exception) {
                _Utils.printException(this@FuturePositionActivity, e)
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
                val totalProfit = arrayOf(0.0.toBigDecimal())
                Log.e("wangsc", "attention future activity fillStockInfoList infolist size: " + infoList!!.size)
                for (info1 in infoList) {
                    val url = "https://hq.sinajs.cn/list=" + info1.code
                    val client = _OkHttpUtil.client
                    val request  = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        try {
                            val sss = response.body!!.string()
                            val result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",".toRegex()).toTypedArray()
                            val open = result[2].toBigDecimal()
                            info1.name = result[0]
                            info1.price = result[8].toBigDecimal()
                            info1.increase = (info1.price!! - open) / open
                            info1.time = result[1]
                            mTime = info1.time
                            Log.e("wangsc", "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee$mTime")
                            val profitPoints = (info1.type.toBigDecimal() * (info1.price!! - info1.cost!!))
                            val commodity = findCommodity(info1.code)
                            totalProfit[0] += profitPoints * (info1.amount * commodity!!.unit).toBigDecimal()
                            Log.e("wangsc", "code: " + info1.code + " , profitPoints: " + totalProfit[0] + " , cose: " + commodity.cose + " , unit: " + commodity.unit)
                            val ff = getFormat(commodity)
                            //                            if (ff.length() > format.length()) {
//                                format = ff;
//                            }
                            runOnUiThread {
                                info1.viewHolder!!.textViewIncrease!!.text = DecimalFormat(ff).format(info1.price)
                                if (info1.increase!! > 0.toBigDecimal()) {
                                    info1.viewHolder!!.textViewIncrease!!.setTextColor(Color.RED)
                                } else if (info1.increase!! == 0.toBigDecimal()) {
                                    info1.viewHolder!!.textViewIncrease!!.setTextColor(Color.WHITE)
                                } else {
                                    info1.viewHolder!!.textViewIncrease!!.setTextColor(Color.GREEN)
                                }
                                info1.viewHolder!!.textViewProfit!!.text = DecimalFormat(ff).format(profitPoints)
                                if (profitPoints > 0.toBigDecimal()) {
                                    info1.viewHolder!!.textViewProfit!!.setTextColor(Color.RED)
                                } else if (profitPoints == 0.0.toBigDecimal()) {
                                    info1.viewHolder!!.textViewProfit!!.setTextColor(Color.WHITE)
                                } else {
                                    info1.viewHolder!!.textViewProfit!!.setTextColor(Color.GREEN)
                                }
                                textView_time!!.text = info1.time!!.substring(0, 2) + ":" + info1.time!!.substring(2, 4) + ":" + info1.time!!.substring(4, 6)
                            }
                        } catch (e: Exception) {
                            return@Runnable
                        }
                    } else {
                        log2file("获取数据失败...")
                        return@Runnable
                    }
                }
                runOnUiThread(Runnable {
                    val averageTotalProfit:BigDecimal
                    if (infoList.size == 0) return@Runnable
                    averageTotalProfit = totalProfit[0]

                    if (Math.abs((averageTotalProfit - preAverageTotalProfit).toDouble()) > 20 / infoList.size) {
                        preAverageTotalProfit = averageTotalProfit
                        var msg = DecimalFormat("0").format(averageTotalProfit)
                        if (averageTotalProfit > 0.toBigDecimal()) {
                            msg = "加$msg"
                        }
                        //                                if (!isFirst) {
//                                    _Utils.speaker(AttentionFutureActivity.this.getApplicationContext(), msg);
//                                }
                    }
                    isFirst = false
                    Log.e("wangsc", "count: $loadStockCount")
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
                    textView_totalProfit!!.text = DecimalFormat("#0,000").format(averageTotalProfit)
                    if (averageTotalProfit > 0.toBigDecimal()) {
                        textView_totalProfit!!.setTextColor(Color.RED)
                    } else if (averageTotalProfit == 0.0.toBigDecimal()) {
                        textView_totalProfit!!.setTextColor(Color.WHITE)
                    } else {
                        textView_totalProfit!!.setTextColor(Color.GREEN)
                    }
                    if (null != mTime && mTime != preTime) {
                        _AnimationUtils.heartBeat(actionButton_home)
                        preTime = mTime
                    }
                })
            } catch (e: Exception) {
//                    e.printStackTrace();
//                    _Utils.printException(getApplicationContext(), e);
            }
        }).start()
    }

    private var isFirst = true
    private var preTime: String? = null
    private var mTime: String? = null
    fun findCommodity(code: String?): Commodity? {
        for (commodity in _Session.commoditys) {
            if (commodity.item.toLowerCase() == parseItem(code).toLowerCase()) {
                return commodity
            }
        }
        return null
    }

    fun getFormat(commodity: Commodity?): String {
        if (commodity != null) {
            var i = 1.0
            var fc = 0
            while (commodity.cose / i < 1) {
                i /= 10.0
                fc++
            }
            var format = "0"
            for (j in 0 until fc) {
                format += if (j == 0) ".0" else "0"
            }
            return format
        }
        return "0"
    }

    private fun parseItem(code: String?): String {
        val item = StringBuffer()
        for (i in 0 until code!!.length) {
            val c = code[i]
            if (!Character.isDigit(c)) {
                item.append(c)
            } else {
                break
            }
        }
        return item.toString()
    }

    private var preAverageTotalProfit = 0.0.toBigDecimal()

    internal inner class StockInfo {
        var time: String? = null
        var name: String? = null
        var code: String? = null
        var cost: BigDecimal? = null
        var price: BigDecimal? = null
        var increase: BigDecimal? = null
        var amount = 0
        var type = 0
        var viewHolder: ViewHolder? = null
    }

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
            _Utils.printException(this@FuturePositionActivity, e)
        }
        return super.onKeyDown(keyCode, event)
    }
}