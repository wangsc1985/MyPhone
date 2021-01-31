package com.wang17.myphone.fragment

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alibaba.fastjson.JSON
import com.wang17.myphone.R
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.callback.DialogChoosenCallback
import com.wang17.myphone.circleMinite
import com.wang17.myphone.e
import com.wang17.myphone.eventbus.EventBusMessage
import com.wang17.myphone.eventbus.SenderBuddhaServiceOnDestroy
import com.wang17.myphone.eventbus.SenderTimerRuning
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.database.BuddhaRecord
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.service.BuddhaService
import com.wang17.myphone.service.StockService
import com.wang17.myphone.toMyDecimal
import com.wang17.myphone.util.*
import com.wang17.myphone.util._Utils.getFilesWithSuffix
import kotlinx.android.synthetic.main.fragment_player.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.DecimalFormat
import java.util.*

class BuddhaPlayerFragment : Fragment() {

    lateinit var abtn_stockAnimator: AnimatorSuofangView
    lateinit var btn_buddha_animator: AnimatorSuofangView
    var uiHandler = Handler()
    lateinit var dc: DataContext
    var isChangeSpeed = false

    override fun onResume() {
        super.onResume()

        if (_Utils.isRunService(context!!, StockService::class.qualifiedName!!)) {
            animatorSuofang(abtn_stockAnimator)
        } else {
            stopAnimatorSuofang(abtn_stockAnimator)
        }
        if (_Utils.isRunService(context!!, BuddhaService::class.qualifiedName!!)) {
            animatorSuofang(btn_buddha_animator)
        } else {
            stopAnimatorSuofang(btn_buddha_animator)
        }


        val buddha = dc.getLatestBuddha()
        var time = DateTime(1970, 1, 1)
        if (buddha != null) {
            time = buddha.startTime
        }
        _CloudUtils.loadBuddha(context!!, time) { code, result ->
            try {
                if (code == 0) {
                    e(result)
                    val jsonArray = JSON.parseArray(result.toString())
                    var buddhaS = ArrayList<BuddhaRecord>()
                    for (i in jsonArray.indices) {
                        val startTime = _JsonUtils.getValueByKey(jsonArray[i], "startTime").toLong()
                        val duration = _JsonUtils.getValueByKey(jsonArray[i], "duration").toLong()
                        val count = _JsonUtils.getValueByKey(jsonArray[i], "count").toInt()
                        val summary = _JsonUtils.getValueByKey(jsonArray[i], "summary")
                        val type = _JsonUtils.getValueByKey(jsonArray[i], "type").toInt()
                        buddhaS.add(BuddhaRecord(DateTime(startTime), duration, count, type, summary))
                    }

                    dc.addBuddhas(buddhaS)

                    uiHandler.post {
                        if (buddhaS.size > 0) {
                            AlertDialog.Builder(context).setMessage("新增${buddhaS.size}条记录").show()
                        }
                    }
                    refreshTotalView()
                }
            } catch (e: Exception) {
                e(e.message!!)
            }
        }
    }

    private fun refreshTotalView() {
        val now = DateTime()
        var totalDayDuration: Long = 0
        var totalDayCount = 0
        var totalMonthDuration: Long = 0
        var totalMonthCount = 0
        val monthBuddhas = dc.getBuddhas(now.year, now.month)
        monthBuddhas.forEach {
            totalMonthCount += it.count
            totalMonthDuration += it.duration
        }
        val dayBuddhas = dc.getBuddhas(now)
        dayBuddhas.forEach {
            totalDayCount += it.count
            totalDayDuration += it.duration
        }

        e("month buddha size : ${monthBuddhas.size} , day buddha size : ${dayBuddhas.size}")
        val hour = totalMonthDuration / (60000 * 60)
        val minite = totalMonthDuration % (60000 * 60) / 60000
        val hourS = "${hour}:"
        val miniteS = if (minite == 0L) {
            if (hour == 0L) "0" else "00"
        } else {
            if (minite < 10) "0${minite}" else "${minite}"
        }

        val hour1 = totalDayDuration / (60000 * 60)
        val minite1 = totalDayDuration % (60000 * 60) / 60000
        val hourS1 = "${hour1}:"
        val miniteS1 = if (minite1 == 0L) {
            if (hour1 == 0L) "0" else "00"
        } else {
            if (minite1 < 10) "0${minite1}" else "${minite1}"
        }
        val formatter = DecimalFormat("#,##0")
        val formatter1 = DecimalFormat("0.0")
        val formatter2 = DecimalFormat("0.00")
        uiHandler.post {
            tv_monthTotal.setText("${hourS}${miniteS}  ${formatter2.format((totalMonthCount * 1080).toMyDecimal() / 10000.toBigDecimal())}万  ${formatter1.format(totalMonthCount.toMyDecimal() / now.day.toBigDecimal())}圈")
            tv_dayTotal.setText("${hourS1}${miniteS1}  ${formatter.format(totalDayCount * 1080)}  ${totalDayCount}圈")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        buddhaIntent = Intent(context, BuddhaService::class.java)
        abtn_stockAnimator = AnimatorSuofangView(abtn_stock)
        btn_buddha_animator = AnimatorSuofangView(abtn_buddha)

        val setting = dc.getSetting(Setting.KEYS.buddha_duration)
        setting?.let {
            val duration = it.long
            val second = duration % 60000 / 1000
            val miniteT = duration / 60000
            val minite = miniteT % 60
            val hour = miniteT / 60
            var count = (miniteT / circleMinite).toInt()
            tv_time.text = "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second} \t $count \t "
        }

//        val buddhaSpeed = dc.getSetting(Setting.KEYS.buddha_speed, 2).int
//        when (buddhaSpeed) {
//            1 -> {
//                btn_speed.text = "慢"
//            }
//            2 -> {
//                btn_speed.text = "快"
//            }
//        }
        val buddhaName = dc.getSetting(Setting.KEYS.buddha_music_name, "追顶念佛2.mp3").string
        tv_buddha_name.text = buddhaName

        tv_time.setOnLongClickListener {
            AlertDialog.Builder(context).setMessage("请选择念佛记录处理方式").setPositiveButton("保存", DialogInterface.OnClickListener { dialog, which ->
                val setting = dc.getSetting(Setting.KEYS.buddha_duration)
                setting?.let {
                }
            }).setNegativeButton("清空", DialogInterface.OnClickListener { dialog, which ->
                dc.deleteSetting(Setting.KEYS.buddha_duration)
                dc.deleteSetting(Setting.KEYS.buddha_stoptime)
                tv_time.text = ""
            }).show()
            true
        }

        var wakeLock: PowerManager.WakeLock? = null
        tv_buddha_name.setOnLongClickListener {
            wakeLock = _Utils.acquireWakeLock(context!!, PowerManager.SCREEN_BRIGHT_WAKE_LOCK)
            var msg = """
                愿将此功德：
                
                    回向给我今生今世及累生累世冤亲债主、历代宗亲、六亲眷属、一切护法，及一切有缘众生。
                    
                    回向给我父、母、兄、妹，以及他们的冤亲债主。
                    
                    回向给法界一切众生。
                    
                    愿我一心向道，念佛精进，早得三昧，速证菩提。
                    
                    祈求观音菩萨，慈悲加持，入我梦中，除我病苦。
                    
                    愿我身心健康，无病无灾。财富丰足，无忧无虑。家庭和睦，无烦无恼。所有恶习，尽除无余。所有冤孽，勿近我身。
                    
                    往昔所造诸恶业，皆由无始贪嗔痴。
                    从身语意之所生，一切我今皆忏悔。
                    
                    愿我临遇命终时，尽除一切诸障碍。
                    面见彼佛阿弥陀，既得往生安乐刹。
                    
                    弟子惟照，现是生死凡夫，罪障深重，轮回六道，苦不可言。
                    今遇知识，得闻弥陀名号，本愿功德，一心称念，求愿往生。
                    愿佛慈悲不舍，哀怜摄受。
            """.trimIndent()
            AlertDialog.Builder(context).setMessage(msg).setCancelable(false).setPositiveButton("圆满", DialogInterface.OnClickListener { dialog, which ->
                _Utils.releaseWakeLock(context!!, wakeLock)
            }).show()
            true
        }
        tv_buddha_name.setOnClickListener {
            _Session.BUDDHA_MUSIC_NAME_ARR = getFilesWithSuffix(_Session.ROOT_DIR.path, ".mp3")
            if (_Session.BUDDHA_MUSIC_NAME_ARR.size == 0) {
                _Session.BUDDHA_MUSIC_NAME_ARR = arrayOf()
            }
            Arrays.sort(_Session.BUDDHA_MUSIC_NAME_ARR)

            AlertDialog.Builder(context).setItems(_Session.BUDDHA_MUSIC_NAME_ARR) { dialog, which ->
                dc.editSetting(Setting.KEYS.buddha_music_name, _Session.BUDDHA_MUSIC_NAME_ARR[which])
                tv_buddha_name.text = _Session.BUDDHA_MUSIC_NAME_ARR[which]
//                when (which) {
//                    0 -> {
//                        btn_speed.text = "慢"
//                    }
//                    1 -> {
//                        btn_speed.text = "快"
//                    }
//                }
//                dc.editSetting(Setting.KEYS.buddha_speed, which + 1)
                if (_Utils.isRunService(context!!, BuddhaService::class.qualifiedName!!)) {
                    isChangeSpeed = true
                    context?.stopService(buddhaIntent)
                    context?.startService(buddhaIntent)
                }
            }.show()
        }

        var totalCount = 0
        layout_dayTotal.setOnClickListener {
            val dayBuddhaList = dc.getBuddhas(DateTime())
            var items = arrayOfNulls<String>(dayBuddhaList.size)
            for (index in dayBuddhaList.indices) {
                val second = dayBuddhaList[index].duration % 60000 / 1000
                val hour = dayBuddhaList[index].duration / (60000 * 60)
                val minite = dayBuddhaList[index].duration % (60000 * 60) / 60000
                val hourS = "${hour}"
                val miniteS = if (minite < 10) "0${minite}" else "${minite}"
                val secondS = if (second < 10) "0${second}" else "${second}"

                totalCount += dayBuddhaList[index].count
                items[index] = "${dayBuddhaList[index].startTime.toShortTimeString()}   ${hourS}:${miniteS}:${secondS}   ${dayBuddhaList[index].count}"
            }
            if (dayBuddhaList.size > 0)
                AlertDialog.Builder(context).setItems(items, DialogInterface.OnClickListener { dialog, index ->
                    AlertDialog.Builder(context)
                            .setMessage("是否要删除【${dayBuddhaList[index].startTime.toLongDateTimeString()}】的记录?")
                            .setNegativeButton("是", DialogInterface.OnClickListener { dialog, which ->
                                dc.deleteBuddha(dayBuddhaList[index].id)
                                refreshTotalView()

                                _CloudUtils.delBuddha(context!!, dayBuddhaList[index]) { code, result ->
                                    when (code) {
                                        0 -> {
                                            uiHandler.post {
                                                AlertDialog.Builder(context).setMessage(result.toString()).show()
                                            }
                                        }
                                        else -> {
                                            e("code : $code , result : $result")
                                        }
                                    }
                                }
                            }).show()
                }).show()
        }

        layout_monthTotal.setOnClickListener {
            val today = DateTime.today
            val monthBuddhaList = dc.getBuddhas(today.year, today.month)
            var start = DateTime(today.year, today.month, 1)
            var sb = StringBuilder()
            var totalDuration = 0L
            var totalCount = 0
            while (start.year == today.year && start.get(Calendar.DAY_OF_YEAR) <= today.get(Calendar.DAY_OF_YEAR)) {
                var list = monthBuddhaList.filter {
                    val time = it.startTime
                    time.year == start.year && time.get(Calendar.DAY_OF_YEAR) == start.get(Calendar.DAY_OF_YEAR)
                }
                var duration = 0L
                var count = 0
                var time = start

                list.forEach {
                    time = it.startTime
                    count += it.count
                    duration += it.duration
                    e("time : ${time.toLongDateTimeString()}")
                }

                val hour = duration / (60000 * 60)
                val minite = duration % (60000 * 60) / 60000
                val hourS = "${hour}"
                val miniteS = if (minite == 0L) {
                    if (hour == 0L) "0" else "00"
                } else {
                    if (minite < 10) "0${minite}" else "${minite}"
                }
                sb.append("${time.toShortDateString1()} \t ${if (count > 0) "${hourS}:${miniteS} \t ${DecimalFormat("#,##0").format(count * 1080)} \t ${count}" else ""}\n")

                totalCount += count
                totalDuration += duration
                start = start.addDays(1)
            }
            val hour = totalDuration / (60000 * 60)
            val minite = totalDuration % (60000 * 60) / 60000
            val hourS = "${hour}"
            val miniteS = if (minite < 10) "0${minite}" else "${minite}"
            val avgCount = totalCount.toBigDecimal().setScale(1) / today.day.toBigDecimal()
            sb.append("\n${hourS}:${miniteS} \t ${DecimalFormat("#,##0").format(totalCount * 1080)} \t ${totalCount} \t ${avgCount}\n")
            if (totalCount > 0)
                AlertDialog.Builder(context).setMessage(sb.toString()).show()
        }

        iv_buddha.setOnLongClickListener {
            val list = arrayOf("1 x 1080", "2 x 1080", "3 x 1080", "4 x 1080", "5 x 1080", "6 x 1080", "7 x 1080", "8 x 1080", "9 x 1080", "10 x 1080")
            AlertDialog.Builder(context).setItems(list, DialogInterface.OnClickListener { dialog, which ->
                var count = 0
                when (which) {
                    0 -> {
                        count = 1
                    }
                    1 -> {
                        count = 2
                    }
                    2 -> {
                        count = 3
                    }
                    3 -> {
                        count = 4
                    }
                    4 -> {
                        count = 5
                    }
                    5 -> {
                        count = 6
                    }
                    6 -> {
                        count = 7
                    }
                    7 -> {
                        count = 8
                    }
                    8 -> {
                        count = 9
                    }
                    9 -> {
                        count = 10
                    }
                }
                AlertDialog.Builder(context).setMessage("新增 $count 圈？").setNegativeButton("是", DialogInterface.OnClickListener { dialog, which ->
                    /**
                     * string phone, long startTime ,long duration , int count, string summary, int type
                     */

                    val latestBuddha = dc.latestBuddha
                    var avgDuration = (10 * 60000).toLong()
                    if (latestBuddha != null) {
                        avgDuration = latestBuddha.duration / latestBuddha.count
                    }
                    var stoptimeInMillis = System.currentTimeMillis()

                    createBuddhas2upload2save(count, stoptimeInMillis, avgDuration, CloudCallback { code, result ->
                        if (code == 0) {
                            uiHandler.post {
                                refreshTotalView()
                            }
                        }
                        uiHandler.post {
                            AlertDialog.Builder(context!!).setMessage(result.toString()).show()
                        }
                    })
                }).show()
            }).show()
            true
        }

        abtn_stock.setOnClickListener {
            //region 在StockReportService里面执行
            if (!_Utils.isRunService(context!!, StockService::class.qualifiedName!!)) {
                context!!.startService(Intent(context, StockService::class.java))
                dc.editSetting(Setting.KEYS.is_stocks_listener, true)
                animatorSuofang(abtn_stockAnimator)
            }
            //endregion
        }
        abtn_stock.setOnLongClickListener { //region 在StockReportService里面执行
            if (_Utils.isRunService(context!!, StockService::class.qualifiedName!!)) {
                context!!.stopService(Intent(context, StockService::class.java))
                dc.editSetting(Setting.KEYS.is_stocks_listener, false)
                stopAnimatorSuofang(abtn_stockAnimator)
            } else {
                context!!.startService(Intent(context, StockService::class.java))
                dc.editSetting(Setting.KEYS.is_stocks_listener, true)
                animatorSuofang(abtn_stockAnimator)
                //
                _Utils.clickHomeButton(context!!)
            }
            //endregion
            true
        }

        abtn_buddha.setOnClickListener {
            playBuddha()
        }
        abtn_buddha.setOnLongClickListener {
            if (!_Utils.isRunService(context!!, BuddhaService::class.qualifiedName!!)) {
                playBuddha()
            }
            true
        }
        super.onViewCreated(view, savedInstanceState)
    }

    private fun createBuddhas2upload2save(count: Int, stoptimeInMillis: Long, avgDuration: Long, callback: CloudCallback) {
        var newBuddhaList: MutableList<BuddhaRecord> = ArrayList()
        var lastBuddha: BuddhaRecord? = dc.latestBuddha
        if (count == 0) {
            val startTime = DateTime(stoptimeInMillis)
            startTime.add(Calendar.MILLISECOND, (-1 * avgDuration).toInt())
            newBuddhaList.add(BuddhaRecord(startTime, avgDuration, 0, 11, "计时计数念佛"))
        } else {
            for (i in count downTo 1) {
                val startTime = DateTime(stoptimeInMillis)
                startTime.add(Calendar.MILLISECOND, (-1 * avgDuration * i).toInt())
                e("start time : ${startTime.toLongDateTimeString()}")
                newBuddhaList.add(BuddhaRecord(startTime, avgDuration, 1, 11, "计时计数念佛"))
            }
        }

        // 整合
        BuddhaUtils.integrate(lastBuddha, newBuddhaList)

        _CloudUtils.addIntegrateBuddhas(context!!, lastBuddha, newBuddhaList) { code, result ->
            when (code) {
                0 -> {
                    lastBuddha?.let {
                        dc.editBuddha(lastBuddha)
                    }
                    dc.addBuddhas(newBuddhaList)
                    callback.excute(code, result)
                }
                else -> {
                    e("code : $code , result : $result")
                    callback.excute(code, result)
                }
            }
        }
    }

    lateinit var buddhaIntent: Intent
    private fun playBuddha() {
        try {
            if (!_Utils.isRunService(context!!, BuddhaService::class.qualifiedName!!)) {
                checkDuration { code, result ->
                    if (code == 0) {
                        context?.startService(buddhaIntent)
                        animatorSuofang(btn_buddha_animator)
                    } else {
                        uiHandler.post {
                            AlertDialog.Builder(context).setMessage(result.toString()).show()
                        }
                    }
                }
            } else {
                context?.stopService(buddhaIntent)
                stopAnimatorSuofang(btn_buddha_animator)
            }
        } catch (e: Exception) {
            e(e.message!!)
        }
    }

    /**
     * 检查数据库中是否有未保存的duration记录
     */
    fun checkDuration(callback: DialogChoosenCallback?) {
//        Thread{
        val setting = dc.getSetting(Setting.KEYS.buddha_duration)
        if (setting != null) {
            var duration = setting.long
            var count = duration / 600000
            val second = duration % 60000 / 1000
            val hour = duration / (60000 * 60)
            val minite = duration % (60000 * 60) / 60000
            val hourS = "${hour}"
            val miniteS = if (minite < 10) "0${minite}" else "${minite}"
            val secondS = if (second < 10) "0${second}" else "${second}"

            val stoptimeInMillis = dc.getSetting(Setting.KEYS.buddha_stoptime, System.currentTimeMillis()).long
            val msg = "${"${hourS}:${miniteS}:${secondS} \t ${DateTime(stoptimeInMillis).toLongDateString3()} \t ${count}"}"
//                if(count>0){
            uiHandler.post {
                val dialog = AlertDialog.Builder(context).setMessage("缓存中存在念佛记录\n[ ${msg} ]\n是否存档？").setNegativeButton("存档", DialogInterface.OnClickListener { dialog, which ->
                    val avgDuration = if (count > 0) duration / count else duration
                    createBuddhas2upload2save(count.toInt(), stoptimeInMillis, avgDuration, CloudCallback { code, result ->
                        if (code == 0) {
                            dc.deleteSetting(Setting.KEYS.buddha_duration)
                            dc.deleteSetting(Setting.KEYS.buddha_stoptime)
                            refreshTotalView()
                            uiHandler.post {
                                tv_time.text = ""
                                AlertDialog.Builder(context).setMessage(result.toString()).show()
                            }
                        }
                        callback?.excute(code, result)
                    })
                }).setNeutralButton("丢弃", DialogInterface.OnClickListener { dialog, which ->
                    dc.deleteSetting(Setting.KEYS.buddha_duration)
                    dc.deleteSetting(Setting.KEYS.buddha_stoptime)
                    tv_time.text = ""
                    callback?.excute(0, "丢弃完毕！")
                })

                if (callback != null) {
                    dialog.setPositiveButton("使用", DialogInterface.OnClickListener { dialog, which ->
                        callback?.excute(0, "")
                    }).setCancelable(false)
                }
                dialog.show()
            }
        } else {
            callback?.excute(0, "缓存中记录为空！")
        }
//        }.start()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        EventBus.getDefault().register(this)
        dc = DataContext(context)
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGetMessage(bus: EventBusMessage) {
        when (bus.sender) {
            is SenderBuddhaServiceOnDestroy -> {
                e("buddha player fragment 接收到销毁消息")
                if (!isChangeSpeed) {
                    checkDuration(null)
                } else {
                    isChangeSpeed = false
                }
            }
            is SenderTimerRuning -> {
                val duration = bus.msg.toLong()
                val second = duration % 60000 / 1000
                val miniteT = duration / 1000 / 60
                val minite = miniteT % 60
                val hour = miniteT / 60
                var count = (miniteT / circleMinite).toInt()
                var time = "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}"
                tv_time.text = "$time \t $count"
            }
        }
    }

    //region 动画
    fun animatorSuofang(view: AnimatorSuofangView) {
        if (view.scaleX == null)
            view.scaleX = ObjectAnimator.ofFloat(view.targetView, "scaleX", 1f, 0.7f, 1f)
        view.scaleX!!.repeatCount = -1
        view.scaleX!!.duration = 600
        view.scaleX!!.start()
        if (view.scaleY == null)
            view.scaleY = ObjectAnimator.ofFloat(view.targetView, "scaleY", 1f, 0.7f, 1f)
        view.scaleY!!.repeatCount = -1
        view.scaleY!!.duration = 600
        view.scaleY!!.start()
    }

    fun stopAnimatorSuofang(view: AnimatorSuofangView) {
        if (view.targetView != null) {
            view.scaleY?.repeatCount = 0
            view.scaleX?.repeatCount = 0
        }
    }

    class AnimatorSuofangView {
        var scaleY: ObjectAnimator? = null
        var scaleX: ObjectAnimator? = null
        var targetView: View

        constructor(view: View) {
            this.targetView = view
        }
    }
    //endregion

}