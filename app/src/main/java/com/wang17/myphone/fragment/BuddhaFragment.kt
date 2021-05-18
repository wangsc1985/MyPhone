package com.wang17.myphone.fragment

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.support.v4.app.Fragment
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.alibaba.fastjson.JSON
import com.wang17.myphone.R
import com.wang17.myphone.activity.BuddhaActivity
import com.wang17.myphone.activity.BuddhaDetailActivity
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.callback.DialogChoosenCallback
import com.wang17.myphone.database.*
import com.wang17.myphone.e
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.event.ResetTimeEvent
import com.wang17.myphone.eventbus.*
import com.wang17.myphone.service.BuddhaService
import com.wang17.myphone.service.MuyuService
import com.wang17.myphone.service.StockService
import com.wang17.myphone.toBuddhaType
import com.wang17.myphone.toMyDecimal
import com.wang17.myphone.util.*
import com.wang17.myphone.util._Utils.getFilesWithSuffix
import kotlinx.android.synthetic.main.fragment_buddha.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.DecimalFormat
import java.util.*

class BuddhaFragment : Fragment() {

    lateinit var abtn_stockAnimator: AnimatorSuofangView
    lateinit var btn_buddha_animator: AnimatorSuofangView
    lateinit var fab_muyu_animator: AnimatorSuofangView
    var uiHandler = Handler()
    lateinit var dc: DataContext
    var isFromConfigChanged = false

    val format = DecimalFormat("0.00")

    override fun onResume() {
        super.onResume()

        val size = dc.getPhoneMessages(dc.getSetting(Setting.KEYS.sms_last_time,0).long).size
        if(size>0){
            tv_msg.visibility=View.VISIBLE
            _Utils.zhendong2(context!!,100)
        }else{
            tv_msg.visibility=View.GONE
        }
        tv_msg.setText(size.toString())


        if (_Utils.isServiceRunning(context!!, StockService::class.qualifiedName!!)) {
            animatorSuofang(abtn_stockAnimator)
        } else {
            stopAnimatorSuofang(abtn_stockAnimator)
        }
        if (_Utils.isServiceRunning(context!!, BuddhaService::class.qualifiedName!!)) {
            animatorSuofang(btn_buddha_animator)
        } else {
            stopAnimatorSuofang(btn_buddha_animator)
        }

        if (_Utils.isServiceRunning(context!!, MuyuService::class.qualifiedName!!)) {
            animatorSuofang(fab_muyu_animator)
        } else {
            stopAnimatorSuofang(fab_muyu_animator)
        }

        loadBuddhaName()

        val buddha = dc.latestBuddha
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
                        buddhaS.add(BuddhaRecord(DateTime(startTime), duration, count, type.toBuddhaType(), summary))
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
            tv_monthTotal.setText("${hourS}${miniteS}  ${formatter2.format((totalMonthCount).toMyDecimal() / 10000.toBigDecimal())}万  ${formatter1.format(totalMonthCount.toFloat() / 1080 / now.day)}圈")
            tv_dayTotal.setText("${hourS1}${miniteS1}  ${formatter.format(totalDayCount)}  ${totalDayCount / 1080}圈")
        }
    }

    var circleSecond = 600
    var muyuCircleSecond = 600
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        buddhaIntent = Intent(context, BuddhaService::class.java)
        abtn_stockAnimator = AnimatorSuofangView(fab_stock)
        btn_buddha_animator = AnimatorSuofangView(fab_buddha)
        fab_muyu_animator = AnimatorSuofangView(fab_muyu)

        dc = DataContext(context)
        circleSecond = getCurrentCircleSecond()
        muyuCircleSecond = (dc.getSetting(Setting.KEYS.muyu_period, 666).int * 1.080).toInt()

        val setting = dc.getSetting(Setting.KEYS.buddha_duration)
        setting?.let {
            val duration = it.long
            val second = duration % 60000 / 1000
            val miniteT = duration / 60000
            val minite = miniteT % 60
            val hour = miniteT / 60
            var tap = (duration / 1000 / circleSecond).toInt()
            tv_time.text = "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second} \t $tap \t "
        }

        iv_volume_add.setOnClickListener {
            try {
                EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaVolumeAdd(), ""))
                _Utils.zhendong70(context!!)
            } catch (e: Exception) {

            }
        }

        iv_volume_minus.setOnClickListener {
            try {
                EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaVolumeMinus(), ""))
                _Utils.zhendong70(context!!)
            } catch (e: Exception) {

            }
        }

        iv_speed_add.setOnClickListener {
            try {
                val bf = getBuddhaConfig()
                bf.speed = format.format(bf.speed + 0.01).toFloat()
//            bf.speed = ( bf.speed.toBigDecimal().setScale(2,BigDecimal.ROUND_HALF_UP) + 0.01.toBigDecimal()).toFloat()
                dc.editBuddhaConfig(bf)

                loadBuddhaName()
                EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaConfigUpdate(), ""))
                _Utils.zhendong70(context!!)
            } catch (e: Exception) {
                dc.addRunLog("BuddhaPlayer", "加速", e.message)
            }
        }
        iv_speed_minus.setOnClickListener {
            try {
                val bf = getBuddhaConfig()
                bf.speed = format.format(bf.speed - 0.01).toFloat()
//            bf.speed = ( bf.speed.toBigDecimal().setScale(2,BigDecimal.ROUND_HALF_UP) - 0.01.toBigDecimal()).toFloat()
                dc.editBuddhaConfig(bf)

                loadBuddhaName()
                EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaConfigUpdate(), ""))
                _Utils.zhendong70(context!!)
            } catch (e: Exception) {
                dc.addRunLog("BuddhaPlayer", "减速", e.message)
            }
        }
        iv_pitch_add.setOnClickListener {
            try {
                val bf = getBuddhaConfig()
                bf.pitch = format.format(bf.pitch + 0.01).toFloat()
                dc.editBuddhaConfig(bf)

                loadBuddhaName()
                EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaConfigUpdate(), ""))
                _Utils.zhendong70(context!!)
            } catch (e: Exception) {
                dc.addRunLog("BuddhaPlayer", "升调", e.message)
            }
        }
        iv_pitch_minus.setOnClickListener {
            try {
                val bf = getBuddhaConfig()
                bf.pitch = format.format(bf.pitch - 0.01).toFloat()
                dc.editBuddhaConfig(bf)

                loadBuddhaName()
                EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaConfigUpdate(), ""))
                _Utils.zhendong70(context!!)
            } catch (e: Exception) {
                dc.addRunLog("BuddhaPlayer", "降调", e.message)
            }
        }

        loadBuddhaName()

        tv_time.setOnLongClickListener {

            if (_Utils.isServiceRunning(context!!, BuddhaService::class.qualifiedName!!) || _Utils.isServiceRunning(context!!, MuyuService::class.qualifiedName!!)) {
                AlertDialog.Builder(context).setMessage("现在对齐时间线？").setPositiveButton("确定") { dialogInterface: DialogInterface, i: Int ->
                    EventBus.getDefault().post(EventBusMessage.getInstance(ResetTimeEvent(), ""))
                }.show()
            } else {
                val setting = dc.getSetting(Setting.KEYS.buddha_duration)
                setting?.let {
                    var duration = setting.long
                    val tap = duration / 1000 / circleSecond

                    val buddhaType = buddhaType()
                    if (buddhaType < 10) {
                        duration = (duration / 60000) * 60000
                    }
                    if (duration > 0) {
                        AlertDialog.Builder(context).setMessage("缓存中存在念佛记录，是否存档？").setPositiveButton("存档") { dialog, which ->
                            buildBuddhaAndSave(tap.toInt() * 1080, duration, dc.getSetting(Setting.KEYS.buddha_stoptime, System.currentTimeMillis()).long, buddhaType.toBuddhaType()) { code, result ->
                                if (code == 0) {
                                    dc.deleteSetting(Setting.KEYS.buddha_duration)
                                    dc.deleteSetting(Setting.KEYS.buddha_stoptime)
                                    refreshTotalView()
                                    uiHandler.post {
                                        tv_time.text = ""
                                        AlertDialog.Builder(context).setMessage(result.toString()).show()
                                    }
                                }
                            }
                        }.setNegativeButton("清空", DialogInterface.OnClickListener { dialog, which ->
                            dc.deleteSetting(Setting.KEYS.buddha_duration)
                            dc.deleteSetting(Setting.KEYS.buddha_stoptime)
                            tv_time.text = ""
                        }).show()
                    } else {
                        dc.deleteSetting(Setting.KEYS.buddha_duration)
                        dc.deleteSetting(Setting.KEYS.buddha_stoptime)
                        refreshTotalView()
                        uiHandler.post {
                            tv_time.text = ""
                        }
                    }
                }
            }

            true
        }

        tv_buddha_name.setOnLongClickListener {
            updateConfig()
            true
        }
        tv_buddha_name.setOnClickListener {
            try {
                _Session.BUDDHA_MUSIC_NAME_ARR = getFilesWithSuffix(_Session.ROOT_DIR.path, ".mp3")
                if (_Session.BUDDHA_MUSIC_NAME_ARR.size == 0) {
                    _Session.BUDDHA_MUSIC_NAME_ARR = arrayOf()
                }
                Arrays.sort(_Session.BUDDHA_MUSIC_NAME_ARR)

                AlertDialog.Builder(context).setItems(_Session.BUDDHA_MUSIC_NAME_ARR) { dialog, which ->
                    dc.editSetting(Setting.KEYS.buddha_music_name, _Session.BUDDHA_MUSIC_NAME_ARR[which])
                    loadBuddhaName()

                    val file = _Session.getFile(_Session.BUDDHA_MUSIC_NAME_ARR[which])
                    val bf = dc.getBuddhaConfig(_Session.BUDDHA_MUSIC_NAME_ARR[which], file.length())

                    if (bf == null) {
                        dc.addBuddhaConfig(BuddhaConfig(_Session.BUDDHA_MUSIC_NAME_ARR[which], file.length(), "md5", 1.0f, 1.0f, 11, 600))
                        updateConfig()
                    } else {
                        circleSecond = getCurrentCircleSecond()
                        if (_Utils.isServiceRunning(context!!, BuddhaService::class.qualifiedName!!) && dc.getSetting(Setting.KEYS.buddha_startime) != null) {
                            isFromConfigChanged = true
                            context?.stopService(buddhaIntent)
                            context?.startService(buddhaIntent)
                        }
                    }

                }.show()
            } catch (e: Exception) {
                dc.addRunLog("err", "切换music", e.message)
            }
        }

        layout_dayTotal.setOnClickListener {
            if(dc.getSetting(Setting.KEYS.is_authorize_buddha_record,false).boolean){
                _FingerUtils.showFingerPrintDialog(activity!!) {
                    val intent = Intent(context, BuddhaDetailActivity::class.java)
                    intent.putExtra("start", System.currentTimeMillis())
                    startActivity(intent)
                }
            }else{
                val intent = Intent(context, BuddhaDetailActivity::class.java)
                intent.putExtra("start", System.currentTimeMillis())
                startActivity(intent)
            }
        }

        layout_monthTotal.setOnClickListener {
            if(dc.getSetting(Setting.KEYS.is_authorize_buddha_record,false).boolean){
                _FingerUtils.showFingerPrintDialog(activity!!) {
                    startActivity(Intent(context, BuddhaActivity::class.java))
                }
            }else{
                startActivity(Intent(context, BuddhaActivity::class.java))
            }
        }

        iv_buddha.setOnLongClickListener {
            val view = View.inflate(context, R.layout.inflate_dialog_add_buddha, null)
            val etCount = view.findViewById<EditText>(R.id.et_count)
            val ivAdd = view.findViewById<ImageView>(R.id.iv_add)
            val ivMinus = view.findViewById<ImageView>(R.id.iv_minus)

            val now = DateTime()
            val hourNumbers = arrayOfNulls<String>(24)
            for (i in 0..23) {
                hourNumbers[i] = i.toString() + "点"
            }
            val minNumbers = arrayOfNulls<String>(60)
            for (i in 0..59) {
                minNumbers[i] = i.toString() + "分"
            }
            val number_hour = view.findViewById<View>(R.id.number_hour) as NumberPicker
            val number_min = view.findViewById<View>(R.id.number_min) as NumberPicker
            number_hour.minValue = 0
            number_hour.displayedValues = hourNumbers
            number_hour.maxValue = 23
            number_hour.value = now.hour
            number_hour.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
            number_min.minValue = 0
            number_min.displayedValues = minNumbers
            number_min.maxValue = 59
            number_min.value = now.minite
            number_min.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
            ivAdd.setOnClickListener {
                etCount.setText((etCount.text.toString().toInt() + 1).toString())
            }
            ivMinus.setOnClickListener {
                if (etCount.text.toString().toInt() > 1)
                    etCount.setText((etCount.text.toString().toInt() - 1).toString())
            }
            val btnCount = view.findViewById<Button>(R.id.btn_count)
            val btnTime = view.findViewById<Button>(R.id.btn_time)
            val btn13 = view.findViewById<Button>(R.id.btn_13)
            val dialog = AlertDialog.Builder(context).setView(view).show()

            btnTime.setOnClickListener {
                dialog.dismiss()
                val tap = etCount.text.toString().toInt()
                val hour = number_hour.value
                val min = number_min.value
                var time = DateTime()
                time.set(Calendar.HOUR_OF_DAY, hour)
                time.set(Calendar.MINUTE, min)
                var stoptimeInMillis = time.timeInMillis

                buildBuddhaAndSave(0, tap * 5 * 60000.toLong(), stoptimeInMillis, BuddhaType.计时念佛) { code, result ->
                    if (code == 0) {
                        uiHandler.post {
                            refreshTotalView()
                        }
                    }
                    uiHandler.post {
                        AlertDialog.Builder(context!!).setMessage(result.toString()).show()
                    }
                }
            }

            btn13.setOnClickListener {
                dialog.dismiss()
                val tap = etCount.text.toString().toInt()
                val latestBuddha = dc.latestBuddha
                val hour = number_hour.value
                val min = number_min.value
                var avgDuration = (10 * 60000).toLong()
                if (latestBuddha != null) {
                    avgDuration = (latestBuddha.duration / (latestBuddha.count.toFloat() / 1080)).toLong()
                }
                var time = DateTime()
                time.set(Calendar.HOUR_OF_DAY, hour)
                time.set(Calendar.MINUTE, min)
                var stoptimeInMillis = time.timeInMillis

                buildBuddhaAndSave(tap * 1080, tap * avgDuration, stoptimeInMillis, BuddhaType.散念) { code, result ->
                    if (code == 0) {
                        uiHandler.post {
                            refreshTotalView()
                        }
                    }
                    uiHandler.post {
                        AlertDialog.Builder(context!!).setMessage(result.toString()).show()
                    }
                }
            }

            btnCount.setOnClickListener {
                dialog.dismiss()
                val tap = etCount.text.toString().toInt()
                val latestBuddha = dc.latestBuddha
                val hour = number_hour.value
                val min = number_min.value
                var avgDuration = (10 * 60000).toLong()
                if (latestBuddha != null) {
                    avgDuration = (latestBuddha.duration / (latestBuddha.count.toFloat() / 1080)).toLong()
                }
                var time = DateTime()
                time.set(Calendar.HOUR_OF_DAY, hour)
                time.set(Calendar.MINUTE, min)
                var stoptimeInMillis = time.timeInMillis

                buildBuddhaAndSave(tap * 1080, tap * avgDuration, stoptimeInMillis, BuddhaType.计数念佛) { code, result ->
                    if (code == 0) {
                        uiHandler.post {
                            refreshTotalView()
                        }
                    }
                    uiHandler.post {
                        AlertDialog.Builder(context!!).setMessage(result.toString()).show()
                    }
                }
            }

//            val ivTo = view.findViewById<ImageView>(R.id.iv_to)
            val ivTo = view.findViewById<Button>(R.id.btn_to)
            ivTo.setOnClickListener {
                dialog.dismiss()
                addBuddhaRecordDialog()
            }
            true
        }


        fab_stock.setOnClickListener {
            //region 在StockReportService里面执行
            if (!_Utils.isServiceRunning(context!!, StockService::class.qualifiedName!!)) {
                context!!.startService(Intent(context, StockService::class.java))
                dc.editSetting(Setting.KEYS.is_stocks_listener, true)
                animatorSuofang(abtn_stockAnimator)
            }
            //endregion
        }

        fab_stock.setOnLongClickListener { //region 在StockReportService里面执行
            if (_Utils.isServiceRunning(context!!, StockService::class.qualifiedName!!)) {
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

        fab_muyu.setOnClickListener {
            if (_Utils.isServiceRunning(context!!, MuyuService::class.qualifiedName!!)) {
                context?.stopService(Intent(context!!, MuyuService::class.java))
                stopAnimatorSuofang(fab_muyu_animator)
            } else {
                context?.startService(Intent(context!!, MuyuService::class.java))
                animatorSuofang(fab_muyu_animator)
            }
        }
        fab_muyu.setOnLongClickListener {
            val view = View.inflate(context!!, R.layout.inflate_editbox, null)
            val et = view.findViewById<EditText>(R.id.et_value)
            et.inputType = InputType.TYPE_CLASS_NUMBER
            et.setText(dc.getSetting(Setting.KEYS.muyu_period, 666).string)
            AlertDialog.Builder(context!!).setView(view).setTitle("设置木鱼间隔时间").setPositiveButton("确定") { dialogInterface: DialogInterface, i: Int ->
                dc.editSetting(Setting.KEYS.muyu_period, et.text.toString())
                muyuCircleSecond = (dc.getSetting(Setting.KEYS.muyu_period, 666).int * 1.08).toInt()
                if (_Utils.isServiceRunning(context!!, MuyuService::class.qualifiedName!!)) {
                    isFromConfigChanged = true
                    context!!.stopService(Intent(context!!, MuyuService::class.java))
                    context!!.startService(Intent(context!!, MuyuService::class.java))
                }
            }.show()

//            if (!_Utils.isServiceRunning(context!!, BuddhaService::class.qualifiedName!!)) {
//                wakeLock = _Utils.acquireWakeLock(context!!, PowerManager.SCREEN_BRIGHT_WAKE_LOCK)
//                var msg = """
//                愿将这些功德：
//
//                    回向给我今生今世及累生累世冤亲债主、历代宗亲、六亲眷属、一切护法，及一切有缘众生。
//
//                    回向给我父、母、子、女、兄、妹，以及他们的冤亲债主。
//
//                    回向给法界一切众生，愿所有众生，业障消除，善根增长，脱轮回苦，生极乐国。
//
//                    愿我念佛不断，精进不退，早得三昧，速证菩提。
//
//                    愿我身心健康，无病无灾。财富丰足，无忧无虑。家庭和睦，无烦无恼。所有恶习，尽除无余。所有冤孽，勿近我身。
//
//                    往昔所造诸恶业，皆由无始贪嗔痴。
//                    从身语意之所生，一切我今皆忏悔。
//
//                    愿我临遇命终时，尽除一切诸障碍。
//                    面见彼佛阿弥陀，既得往生安乐刹。
//
//                    弟子惟照，现是生死凡夫，罪障深重，轮回六道，苦不可言。
//                    今遇知识，得闻弥陀名号，本愿功德，一心称念，求愿往生。
//                    愿佛慈悲不舍，哀怜摄受。
//            """.trimIndent()
//                AlertDialog.Builder(context).setMessage(msg).setCancelable(false).setPositiveButton("圆满") { dialog, which ->
//                    _Utils.releaseWakeLock(context!!, wakeLock)
//                }.show()
//            }

            true
        }

        fab_buddha.setOnClickListener {
            autoSave = false
            startOrStopBuddha()
        }

        fab_buddha.setOnLongClickListener {
            autoSave = true
            startOrStopBuddha()
            true
        }
        super.onViewCreated(view, savedInstanceState)
    }

    private fun loadBuddhaName() {
        val musicName = dc.getSetting(Setting.KEYS.buddha_music_name, "阿弥陀佛.mp3")
        val bf = getBuddhaConfig()
        if (bf.type == 11 || bf.type == 10) {
            circleSecond = (bf.circleSecond / bf.speed).toInt()
        } else {
            circleSecond = bf.circleSecond
        }

        tv_buddha_name.text = "${musicName.string.replace(".mp3", "")}  ${circleSecond}秒"
        tv_speed.text = "速${format.format(bf.speed)}度"
        tv_pitch.text = "音${format.format(bf.pitch)}调"
    }

    fun getBuddhaConfig(): BuddhaConfig {
        val musicName = dc.getSetting(Setting.KEYS.buddha_music_name, "阿弥陀佛.mp3")

        val file = _Session.getFile(musicName.string)
        var bf = dc.getBuddhaConfig(musicName.string, file.length())
        if (bf == null) {
            bf = BuddhaConfig(musicName.string, file.length(), "md5", 1.0f, 1.0f, 11, 600)
        }
        return bf
    }

    var autoSave = false
    fun addBuddhaRecordDialog() {
        val view = View.inflate(context, R.layout.inflate_dialog_add_buddha1, null)
        val cvDate = view.findViewById<CalendarView>(R.id.cv_date)

        val now = DateTime(System.currentTimeMillis())
        var year = now.year
        var month = now.month
        var day = now.day
        cvDate.date = now.timeInMillis
        val etNum = view.findViewById<EditText>(R.id.et_count)
        cvDate.setOnDateChangeListener { view, y, m, d ->
            year = y
            month = m
            day = d
        }

        val hourNumbers = arrayOfNulls<String>(24)
        for (i in 0..23) {
            hourNumbers[i] = i.toString() + "点"
        }
        val minNumbers = arrayOfNulls<String>(60)
        for (i in 0..59) {
            minNumbers[i] = i.toString() + "分"
        }
        val number_hour = view.findViewById<View>(R.id.number_hour) as NumberPicker
        val number_min = view.findViewById<View>(R.id.number_min) as NumberPicker
        number_hour.minValue = 0
        number_hour.displayedValues = hourNumbers
        number_hour.maxValue = 23
        number_hour.value = 9
        number_hour.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
        number_hour.value = 21
        number_min.minValue = 0
        number_min.displayedValues = minNumbers
        number_min.maxValue = 59
        number_min.value = 0
        number_min.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS // 禁止对话框打开后数字选择框被选中
        val btn = view.findViewById<Button>(R.id.btn_ok)
        val dialog = AlertDialog.Builder(context).setView(view).setCancelable(false).show()
        btn.setOnClickListener {
            dialog.dismiss()
        }
        etNum.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val hour = number_hour.value
                val min = number_min.value
                var tapDuration = (55000).toLong()

                var time = DateTime(year, month, day)
                time.set(Calendar.HOUR_OF_DAY, hour)
                time.set(Calendar.MINUTE, min)
                var stoptimeInMillis = time.timeInMillis

                if (etNum.text.toString().isEmpty()) {
                    time = time.addDays(1)
                    cvDate.date = time.timeInMillis
                    year = time.year
                    month = time.month
                    day = time.day
                } else {
                    val count = etNum.text.toString().toInt()
                    val tapCount = 100
                    val tap = count / tapCount

                    buildBuddhaAndSave(count, tap * tapDuration, stoptimeInMillis, BuddhaType.计数念佛) { code, result ->
                        if (code == 0) {
                            uiHandler.post {
                                refreshTotalView()
                                time = time.addDays(1)
                                cvDate.date = time.timeInMillis
                                year = time.year
                                month = time.month
                                day = time.day
                                etNum.setText("")
                            }
                        }
                        uiHandler.post {
                            AlertDialog.Builder(context!!).setMessage(result.toString()).show()
                        }
                    }
                }
            }
            true
        }
    }

    private fun updateConfig() {
        val set = dc.getSetting(Setting.KEYS.buddha_music_name)
        set?.let {
            val file = _Session.getFile(set.string)
            if (file.exists()) {
                val bf = dc.getBuddhaConfig(set.string, file.length())
                val view = View.inflate(context, R.layout.inflate_dialog_buddha_config, null)
                val etPitch = view.findViewById<EditText>(R.id.et_pitch)
                val etSpeed = view.findViewById<EditText>(R.id.et_speed)
                val ivPitchMinus = view.findViewById<ImageView>(R.id.iv_pitch_minus)
                val ivPitchAdd = view.findViewById<ImageView>(R.id.iv_pitch_add)
                val ivSpeedMinus = view.findViewById<ImageView>(R.id.iv_speed_minus)
                val ivSpeedAdd = view.findViewById<ImageView>(R.id.iv_speed_add)
                val spType = view.findViewById<Spinner>(R.id.sp_type)
                val etDuration = view.findViewById<EditText>(R.id.et_duration)
                val btnUpdate = view.findViewById<Button>(R.id.btnUpdate)
                etPitch.setText(bf.pitch.toString())
                etSpeed.setText(bf.speed.toString())
                when (bf.type) {
                    0 -> {
                        spType.setSelection(0)
                    }
                    1 -> {
                        spType.setSelection(1)
                    }
                    10 -> {
                        spType.setSelection(2)
                    }
                    11 -> {
                        spType.setSelection(3)
                    }
                }
                val format = DecimalFormat("0.00")
                ivPitchAdd.setOnClickListener {
                    etPitch.setText(format.format(etPitch.text.toString().toFloat() + 0.01))
                    bf.pitch = etPitch.text.toString().toFloat()
                    dc.editBuddhaConfig(bf)

                    loadBuddhaName()
                    EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaConfigUpdate(), ""))
                }
                ivPitchMinus.setOnClickListener {
                    etPitch.setText(format.format(etPitch.text.toString().toFloat() - 0.01))
                    bf.pitch = etPitch.text.toString().toFloat()
                    dc.editBuddhaConfig(bf)

                    loadBuddhaName()
                    EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaConfigUpdate(), ""))
                }
                ivSpeedAdd.setOnClickListener {
                    etSpeed.setText(format.format(etSpeed.text.toString().toFloat() + 0.01))
                    bf.speed = etSpeed.text.toString().toFloat()
                    dc.editBuddhaConfig(bf)

                    loadBuddhaName()
                    EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaConfigUpdate(), ""))
                }
                ivSpeedMinus.setOnClickListener {
                    etSpeed.setText(format.format(etSpeed.text.toString().toFloat() - 0.01))
                    bf.speed = etSpeed.text.toString().toFloat()
                    dc.editBuddhaConfig(bf)

                    loadBuddhaName()
                    EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaConfigUpdate(), ""))
                }
                etDuration.setText(bf.circleSecond.toString())

                val dialog = AlertDialog.Builder(context).setTitle(set.string).setView(view).show()
                btnUpdate.setOnClickListener {
                    bf.circleSecond = etDuration.text.toString().toInt()
                    bf.type = spType.selectedItem.toString().toInt()
                    bf.speed = etSpeed.text.toString().toFloat()
                    bf.pitch = etPitch.text.toString().toFloat()
                    dc.editBuddhaConfig(bf)
                    isFromConfigChanged = true
                    circleSecond = getCurrentCircleSecond()  // 必须在dc.editBuddhaConfig(bf)之后
                    if (_Utils.isServiceRunning(context!!, BuddhaService::class.qualifiedName!!) && dc.getSetting(Setting.KEYS.buddha_startime) != null) {
                        context?.stopService(buddhaIntent)
                        context?.startService(buddhaIntent)
                    } else {
                        dialog.dismiss()
                    }

                    var list = ArrayList<BuddhaConfig>()
                    var bfs = dc.buddhaConfigList
                    e("buddha files size : ${bfs.size}  remove list size : ${list.size}")
                    bfs.forEach {
                        if (!_Session.BUDDHA_MUSIC_NAME_ARR.contains(it.name)) {
                            e(it.name)
                            list.add(it)
                        }
                    }

                    dc.deleteBuddhaConfigList(list)
                }
            }
        }
    }

    private fun getCurrentCircleSecond(): Int {
        val setName = dc.getSetting(Setting.KEYS.buddha_music_name)
        var circleSecond = 600
        setName?.let {
            val file = _Session.getFile(setName.string)
            val bf = dc.getBuddhaConfig(setName.string, file.length())
            bf?.let {
                if (bf.type == 11 || bf.type == 10) {
                    circleSecond = (bf.circleSecond / bf.speed).toInt()
                } else {
                    circleSecond = bf.circleSecond
                }
            }
        }
        return circleSecond
    }

    private fun buddhaType(): Int {
        var buddhaType = 11
        val durationSet = dc.getSetting(Setting.KEYS.buddha_duration)
        if (durationSet != null) {
            val set = dc.getSetting(Setting.KEYS.buddha_music_name)
            set?.let {
                val file = _Session.getFile(set.string)
                if (file.exists()) {
                    val bf = dc.getBuddhaConfig(set.string, file.length())
                    buddhaType = bf.type
                }
            }
        }
        return buddhaType
    }

    private fun buildBuddhaAndSave(count: Int, duration: Long, stopTimeInMillis: Long, buddhaType: BuddhaType, callback: CloudCallback) {
        val startTime = DateTime(stopTimeInMillis - duration)

        var buddha: BuddhaRecord? = null

        buddha = BuddhaRecord(startTime, duration, count, buddhaType, "")

//        when (buddhaType) {
//            10 -> {
//            }
//            11 -> {
//                buddha = BuddhaRecord(startTime, duration, count, 11, "")
//            }
//            1 -> {
//            }
//            0 -> {
//                buddha = BuddhaRecord(startTime, duration, 0, 0, "")
//            }
//        }

        buddha?.let {
            _CloudUtils.addBuddha(context!!, it) { code, result ->
                when (code) {
                    0 -> {
                        dc.addBuddha(it)
                    }
                    else -> {
                        e("code : $code , result : $result")
                    }
                }
                callback.excute(code, result)
            }
        }
    }

    lateinit var buddhaIntent: Intent
    private fun startOrStopBuddha() {
        try {
            if (!_Utils.isServiceRunning(context!!, BuddhaService::class.qualifiedName!!)) {
                checkDuration(BuddhaSource.点击开始按钮) { code, result ->
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

    enum class BuddhaSource {
        念佛服务结束, 木鱼服务结束, 点击开始按钮
    }

    /**
     * 检查数据库中是否有未保存的duration记录
     * 这个方法在两种情况下会调用：
     *  1、启动念佛
     *  2、念佛结束
     */
    fun checkDuration(source: BuddhaSource, callback: DialogChoosenCallback?) {
        val setDuration = dc.getSetting(Setting.KEYS.buddha_duration)
        var buddhaType = buddhaType()
        if (setDuration != null) {
            var duration = setDuration.long
            var tap = duration / 1000 / circleSecond
            if (source == BuddhaSource.木鱼服务结束) {
                buddhaType = 11
                var tap = duration / 1000 / muyuCircleSecond
            }
            val second = duration % 60000 / 1000
            val hour = duration / (60000 * 60)
            val minite = duration % (60000 * 60) / 60000
            val hourS = "${hour}"
            val miniteS = if (minite < 10) "0${minite}" else "${minite}"
            val secondS = if (second < 10) "0${second}" else "${second}"

            val stoptimeInMillis = dc.getSetting(Setting.KEYS.buddha_stoptime, System.currentTimeMillis()).long
            val msg = "${hourS}:${miniteS}:${secondS} \t ${DateTime(stoptimeInMillis).toLongDateString3()}"

            uiHandler.post {
                if (buddhaType < 10) {
                    duration = (duration / 60000) * 60000
                }
                if (duration > 60000) {
                    if (source == BuddhaSource.念佛服务结束 && autoSave) {
                        buildBuddhaAndSave(tap.toInt() * 1080, duration, stoptimeInMillis, buddhaType.toBuddhaType()) { code, result ->
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
                        }
                        return@post
                    }
                    val dialog = AlertDialog.Builder(context).setMessage("缓存中存在念佛记录\n[ ${msg} ]\n是否存档？")
                    dialog.setNegativeButton("存档") { dialog, which ->
                        buildBuddhaAndSave(tap.toInt() * 1080, duration, stoptimeInMillis, buddhaType.toBuddhaType()) { code, result ->
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
                        }
                    }
                    dialog.setNeutralButton("丢弃") { dialog, which ->
                        dc.deleteSetting(Setting.KEYS.buddha_duration)
                        dc.deleteSetting(Setting.KEYS.buddha_stoptime)
                        tv_time.text = ""
                        callback?.excute(0, "丢弃完毕！")
                    }
                    if (source == BuddhaSource.点击开始按钮) {
                        dialog.setPositiveButton("使用") { dialog, which ->
                            callback?.excute(0, "")
                        }.setCancelable(false)
                    }
                    dialog.show()

                } else {
                    dc.deleteSetting(Setting.KEYS.buddha_duration)
                    dc.deleteSetting(Setting.KEYS.buddha_stoptime)
                    refreshTotalView()
                    uiHandler.post {
                        tv_time.text = ""
                    }
                }
            }
        } else {
            callback?.excute(0, "缓存中记录为空！")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        EventBus.getDefault().register(this)
        return inflater.inflate(R.layout.fragment_buddha, container, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGetMessage(bus: EventBusMessage) {
        when (bus.sender) {
            is FromBuddhaServiceDestroy -> {
                if (isFromConfigChanged) {
                    isFromConfigChanged = false
                } else {
                    checkDuration(BuddhaSource.念佛服务结束, null)
                }
            }
            is FromBuddhaServiceTimer -> {
                val duration = bus.msg.toLong()
                val second = duration % 60000 / 1000
                val miniteT = duration / 1000 / 60
                val minite = miniteT % 60
                val hour = miniteT / 60
                var count = (duration / 1000 / circleSecond).toInt()
                var time = "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}"
                tv_time.text = "$time  ${count}"
            }
            is FromMuyuServiceTimer -> {
                val duration = bus.msg.toLong()
                val second = duration % 60000 / 1000
                val miniteT = duration / 1000 / 60
                val minite = miniteT % 60
                val hour = miniteT / 60
                var count = (duration / 1000 / muyuCircleSecond).toInt()
                var time = "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}"
                tv_time.text = "$time  ${count}"
            }
            is FromMuyuServiceDestory -> {
                if (isFromConfigChanged) {
                    isFromConfigChanged = false
                } else {
                    checkDuration(BuddhaSource.木鱼服务结束, null)
                }
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