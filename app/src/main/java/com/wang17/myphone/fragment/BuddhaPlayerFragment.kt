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
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.alibaba.fastjson.JSON
import com.wang17.myphone.R
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.callback.DialogChoosenCallback
import com.wang17.myphone.e
import com.wang17.myphone.eventbus.EventBusMessage
import com.wang17.myphone.eventbus.SenderBuddhaServiceOnDestroy
import com.wang17.myphone.eventbus.SenderTimerRuning
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.database.BuddhaFile
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
    var isChangeConfig = false

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
            tv_monthTotal.setText("${hourS}${miniteS}  ${formatter2.format((totalMonthCount).toMyDecimal() / 10000.toBigDecimal())}万  ${formatter1.format(totalMonthCount.toFloat() / 1080 / now.day)}圈")
            tv_dayTotal.setText("${hourS1}${miniteS1}  ${formatter.format(totalDayCount)}  ${totalDayCount / 1080}圈")
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
            var count = (miniteT / getCurrentCircleDuration()).toInt()
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

        tv_buddha_name.text = dc.getSetting(Setting.KEYS.buddha_music_name, "阿弥陀佛.mp3").string

        tv_time.setOnLongClickListener {
            val setting = dc.getSetting(Setting.KEYS.buddha_duration)
            setting?.let {
                val duration = setting.long
                val tap = duration / getCurrentCircleDuration()
                val avgDuration = if (tap == 0L) duration else duration / tap
                AlertDialog.Builder(context).setMessage("缓存中存在念佛记录，是否存档？").setPositiveButton("存档") { dialog, which ->
                    buildBuddhaAndSave(tap.toInt(), duration, dc.getSetting(Setting.KEYS.buddha_stoptime, System.currentTimeMillis()).long,buddhaType()) { code, result ->
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
            }
            true
        }

        var wakeLock: PowerManager.WakeLock? = null
        tv_buddha_name.setOnLongClickListener {

            if (!_Utils.isRunService(context!!, BuddhaService::class.qualifiedName!!)) {
                wakeLock = _Utils.acquireWakeLock(context!!, PowerManager.SCREEN_BRIGHT_WAKE_LOCK)
                var msg = """
                愿将此功德：
                
                    回向给我今生今世及累生累世冤亲债主、历代宗亲、六亲眷属、一切护法，及一切有缘众生。
                    
                    回向给我父、母、兄、妹，以及他们的冤亲债主。
                    
                    回向给法界一切众生，愿所有众生，业障消除，善根增长，脱轮回苦，生极乐国。
                    
                    愿我念佛精进，愿心不退，早得三昧，速证菩提。
                    
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
                AlertDialog.Builder(context).setMessage(msg).setCancelable(false).setPositiveButton("圆满") { dialog, which ->
                    _Utils.releaseWakeLock(context!!, wakeLock)
                }.show()
            }

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

                val file = _Session.getFile(_Session.BUDDHA_MUSIC_NAME_ARR[which])
                val bf = dc.getBuddhaFile(_Session.BUDDHA_MUSIC_NAME_ARR[which], file.length())

                if (bf == null) {
                    dc.addBuddhaFile(BuddhaFile(_Session.BUDDHA_MUSIC_NAME_ARR[which], file.length(), _Utils.getMD5(file.path), 1.0f, 1.0f, 0, 10))
                }

                if (_Utils.isRunService(context!!, BuddhaService::class.qualifiedName!!)) {
                    isChangeConfig = true
                    context?.stopService(buddhaIntent)
                    context?.startService(buddhaIntent)
                }
            }.show()
        }

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

                items[index] = "${dayBuddhaList[index].startTime.toShortTimeString()}   ${hourS}:${miniteS}:${secondS}   ${dayBuddhaList[index].count / 1080}"
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
            var totalTap = 0
            while (start.year == today.year && start.get(Calendar.DAY_OF_YEAR) <= today.get(Calendar.DAY_OF_YEAR)) {
                var list = monthBuddhaList.filter {
                    val time = it.startTime
                    time.year == start.year && time.get(Calendar.DAY_OF_YEAR) == start.get(Calendar.DAY_OF_YEAR)
                }
                var duration = 0L
                var count = 0
                var tap = 0
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
                tap = count / 1080
                totalTap += tap
                sb.append("${time.toShortDateString1()} \t ${if (count > 0) "${hourS}:${miniteS} \t ${DecimalFormat("#,##0").format(count)} \t ${tap}" else ""}\n")

                totalDuration += duration
                start = start.addDays(1)
            }
            val hour = totalDuration / (60000 * 60)
            val minite = totalDuration % (60000 * 60) / 60000
            val hourS = "${hour}"
            val miniteS = if (minite < 10) "0${minite}" else "${minite}"
            val avgCount = totalTap.toBigDecimal().setScale(1) / today.day.toBigDecimal()
            sb.append("\n${hourS}:${miniteS} \t ${DecimalFormat("#,##0").format(totalTap)} \t ${totalTap} \t ${avgCount}\n")
            if (totalTap > 0)
                AlertDialog.Builder(context).setMessage(sb.toString()).show()
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

                buildBuddhaAndSave(0, tap*5*600000.toLong(), stoptimeInMillis,1) { code, result ->
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

                buildBuddhaAndSave(tap*1080, tap*avgDuration, stoptimeInMillis,11) { code, result ->
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

            val btnTo = view.findViewById<Button>(R.id.btn_to)
            btnTo.setOnClickListener {
                dialog.dismiss()
                addBuddhaRecordDialog()
            }
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
//            if (!_Utils.isRunService(context!!, BuddhaService::class.qualifiedName!!)) {
//                playBuddha()
//            }
            updateConfig()
            true
        }
        super.onViewCreated(view, savedInstanceState)
    }

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

                    buildBuddhaAndSave(count,tap*tapDuration, stoptimeInMillis,11) { code, result ->
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
                val bf = dc.getBuddhaFile(set.string, file.length())
                val view = View.inflate(context, R.layout.inflate_dialog_buddha_file, null)
                val etPitch = view.findViewById<EditText>(R.id.et_pitch)
                val etSpeed = view.findViewById<EditText>(R.id.et_speed)
                val spType = view.findViewById<Spinner>(R.id.sp_type)
                val etDuration = view.findViewById<EditText>(R.id.et_duration)
                val btnUpdate = view.findViewById<Button>(R.id.btnUpdate)
                //                        val btnClose = view.findViewById<Button>(R.id.btnClose)
                etPitch.setText(bf.pitch.toString())
                etSpeed.setText(bf.speed.toString())
                when (bf.type) {
                    0 -> {
                        spType.setSelection(0)
                    }
                    1 -> {
                        spType.setSelection(1)
                    }
                    11 -> {
                        spType.setSelection(2)
                    }
                }
                etDuration.setText(bf.duration.toString())
                etPitch.setOnEditorActionListener { v, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        bf.pitch = etPitch.text.toString().toFloat()
                        dc.editBuddhaFile(bf)
                        isChangeConfig = true
                        context?.stopService(buddhaIntent)
                        context?.startService(buddhaIntent)
                    }
                    true
                }
                etSpeed.setOnEditorActionListener { v, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        bf.speed = etSpeed.text.toString().toFloat()
                        dc.editBuddhaFile(bf)
                        isChangeConfig = true
                        context?.stopService(buddhaIntent)
                        context?.startService(buddhaIntent)
                    }
                    true
                }

                val dialog = AlertDialog.Builder(context).setTitle(set.string).setView(view).show()
                btnUpdate.setOnClickListener {
                    bf.duration = etDuration.text.toString().toInt()
                    bf.type = spType.selectedItem.toString().toInt()
                    bf.speed = etSpeed.text.toString().toFloat()
                    bf.pitch = etPitch.text.toString().toFloat()
                    dc.editBuddhaFile(bf)
                    isChangeConfig = true
                    if (_Utils.isRunService(context!!, BuddhaService::class.qualifiedName!!)) {
                        context?.stopService(buddhaIntent)
                        context?.startService(buddhaIntent)
                    } else {
                        dialog.dismiss()
                    }
                }

                //                        btnClose.setOnClickListener {
                //                            dialog.dismiss()
                //
                //                        dialog.setPositiveButton("关闭", DialogInterface.OnClickListener { dialog, which ->
                //                        })
            }
        }
    }

    private fun getCurrentCircleDuration(): Int {
        val setName = dc.getSetting(Setting.KEYS.buddha_music_name)
        var circleMinite = 10
        setName?.let {
            val file = _Session.getFile(setName.string)
            val bf = dc.getBuddhaFile(setName.string, file.length())
            bf?.let {
                circleMinite = bf.duration
            }
        }
        return circleMinite
    }

    private fun buddhaType():Int{
        var buddhaType = 11
        val durationSet = dc.getSetting(Setting.KEYS.buddha_duration)
        if (durationSet != null) {
            val set = dc.getSetting(Setting.KEYS.buddha_music_name)
            set?.let {
                val file = _Session.getFile(set.string)
                if (file.exists()) {
                    val bf = dc.getBuddhaFile(set.string, file.length())
                    buddhaType = bf.type
                }
            }
        }
        return buddhaType
    }

    private fun buildBuddhaAndSave( count: Int, duration: Long,stopTimeInMillis: Long,  buddhaType:Int,callback: CloudCallback) {

        val startTime = DateTime(stopTimeInMillis-duration)

        var buddha: BuddhaRecord? = null
        when (buddhaType) {
            11 -> {
                buddha = BuddhaRecord(startTime, duration, count, 11, "计时计数念佛")
            }
            1 -> {
                buddha = BuddhaRecord(startTime, duration, 0, 1, "计时念佛")
            }
            0 -> {
                buddha = BuddhaRecord(startTime, duration, 0, 0, "耳听念佛")
            }
        }

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

/*    private fun buildBuddhaListAndSave(tap: Int, tapCount: Int, stoptimeInMillis: Long, tapDuration: Long, callback: CloudCallback) {
        e("build buddha list and save")

        val durationSet = dc.getSetting(Setting.KEYS.buddha_duration)
        var buddhaType = 11
        if (durationSet != null) {
            val set = dc.getSetting(Setting.KEYS.buddha_music_name)
            set?.let {
                val file = _Session.getFile(set.string)
                if (file.exists()) {
                    val bf = dc.getBuddhaFile(set.string, file.length())
                    buddhaType = bf.type
                }
            }
        }
        var buddhaList: MutableList<BuddhaRecord> = ArrayList()
        var startTime: DateTime? = null
        var buddha: BuddhaRecord? = null
        for (index in tap downTo 1) {
            if (startTime == null || buddha == null) {
                startTime = DateTime(stoptimeInMillis)
                startTime.add(Calendar.MILLISECOND, (-1 * tapDuration * index).toInt())
                when (buddhaType) {
                    11 -> {
                        buddha = BuddhaRecord(startTime, tapDuration, tapCount, 11, "计时计数念佛")
                    }
                    1 -> {
                        buddha = BuddhaRecord(startTime, tapDuration, 0, 1, "计时念佛")
                    }
                    0 -> {
                        buddha = BuddhaRecord(startTime, tapDuration, 0, 0, "耳听念佛")
                    }
                }
            } else {
                var startTime = DateTime(stoptimeInMillis)
                startTime.add(Calendar.MILLISECOND, (-1 * tapDuration * index).toInt())
                if (startTime.get(Calendar.DAY_OF_YEAR) == startTime.get(Calendar.DAY_OF_YEAR) && startTime.hour == startTime.hour) {
                    buddha.duration += tapDuration
                    if (buddhaType == 11) {
                        buddha.count += tapCount
                    }
                } else {
                    buddhaList.add(buddha)
                    when (buddhaType) {
                        11 -> {
                            buddha = BuddhaRecord(startTime, tapDuration, tapCount, 11, "计时计数念佛")
                        }
                        1 -> {
                            buddha = BuddhaRecord(startTime, tapDuration, 0, 1, "计时念佛")
                        }
                        0 -> {
                            buddha = BuddhaRecord(startTime, tapDuration, 0, 0, "耳听念佛")
                        }
                    }
                    startTime = DateTime(startTime.timeInMillis)
                }
            }
        }
        buddha?.let {
            buddhaList.add(it)
        }


        _CloudUtils.addBuddhaList(context!!, buddhaList) { code, result ->
            when (code) {
                0 -> {
                    dc.addBuddhas(buddhaList)
                }
                else -> {
                    e("code : $code , result : $result")
                }
            }
            callback.excute(code, result)
        }
    }*/

/*    private fun createBuddhas2upload2save(tap: Int, stoptimeInMillis: Long, avgDuration: Long, callback: CloudCallback) {
        var newBuddhaList: MutableList<BuddhaRecord> = ArrayList()
        var lastBuddha: BuddhaRecord? = dc.latestBuddha
        if (tap == 0) {
            val startTime = DateTime(stoptimeInMillis)
            startTime.add(Calendar.MILLISECOND, (-1 * avgDuration).toInt())
            newBuddhaList.add(BuddhaRecord(startTime, avgDuration, 0, 11, "计时计数念佛"))
        } else {
            for (i in tap downTo 1) {
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
    }*/

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
        val setDuration = dc.getSetting(Setting.KEYS.buddha_duration)
        if (setDuration != null) {
            var duration = setDuration.long
            var tap = duration / (60000 * getCurrentCircleDuration())
            val second = duration % 60000 / 1000
            val hour = duration / (60000 * 60)
            val minite = duration % (60000 * 60) / 60000
            val hourS = "${hour}"
            val miniteS = if (minite < 10) "0${minite}" else "${minite}"
            val secondS = if (second < 10) "0${second}" else "${second}"

            val stoptimeInMillis = dc.getSetting(Setting.KEYS.buddha_stoptime, System.currentTimeMillis()).long
            val msg = "${hourS}:${miniteS}:${secondS} \t ${DateTime(stoptimeInMillis).toLongDateString3()}"
//                if(count>0){
            uiHandler.post {
                val dialog = AlertDialog.Builder(context).setMessage("缓存中存在念佛记录\n[ ${msg} ]\n是否存档？").setNegativeButton("存档", DialogInterface.OnClickListener { dialog, which ->
                    val avgDuration = if (tap > 0) duration / tap else duration
                    buildBuddhaAndSave(tap.toInt()*1080, duration, stoptimeInMillis,buddhaType()) { code, result ->
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
                if (!isChangeConfig) {
                    checkDuration(null)
                } else {
                    isChangeConfig = false
                }
            }
            is SenderTimerRuning -> {
                val duration = bus.msg.toLong()
                val second = duration % 60000 / 1000
                val miniteT = duration / 1000 / 60
                val minite = miniteT % 60
                val hour = miniteT / 60
                var count = (miniteT / getCurrentCircleDuration()).toInt()
                var time = "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}"
                tv_time.text = "$time  ${count}"
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