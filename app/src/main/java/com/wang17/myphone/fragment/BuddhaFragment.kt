package com.wang17.myphone.fragment

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.alibaba.fastjson.JSON
import com.wang17.myphone.*
import com.wang17.myphone.activity.BuddhaActivity
import com.wang17.myphone.activity.BuddhaChartActivity
import com.wang17.myphone.activity.BuddhaDetailActivity
import com.wang17.myphone.activity.NianfoDarkRunActivity
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.callback.DialogChoosenCallback
import com.wang17.myphone.dao.DataContext
import com.wang17.myphone.database.*
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.event.ResetTimeEvent
import com.wang17.myphone.eventbus.*
import com.wang17.myphone.service.BuddhaService
import com.wang17.myphone.service.MuyuService
import com.wang17.myphone.service.StockService
import com.wang17.myphone.util.*
import com.wang17.myphone.util._Utils.getFilesWithSuffix
import kotlinx.android.synthetic.main.fragment_buddha.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class BuddhaFragment : Fragment() {

    lateinit var abtn_stockAnimator: AnimatorSuofangView
    lateinit var btn_buddha_animator: AnimatorSuofangView
    lateinit var fab_muyu_animator: AnimatorSuofangView
    var uiHandler = Handler()
    lateinit var dc: DataContext
    var isFromConfigChanged = false

    override fun onResume() {
        super.onResume()

        try {
            refreshTotalView()
            val size = dc.getPhoneMessages(dc.getSetting(Setting.KEYS.sms_last_time, DateTime.today.timeInMillis).long).size
            if (size > 0) {
                uiHandler.post {
                    tv_msg.visibility = View.VISIBLE
                }
                _Utils.zhendong(context!!, 100)
                EventBus.getDefault().post(EventBusMessage.getInstance(ChangeFragmentTab(), "2"))
            } else {
                uiHandler.post {
                    tv_msg.visibility = View.GONE
                }
            }

            uiHandler.post {
                tv_msg.setText(size.toString())
            }


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
                if (code == 0) {
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
                            Toast.makeText(context, "新增${buddhaS.size}条记录", Toast.LENGTH_LONG).show()
                        }
                    }
                    refreshTotalView()
                }
            }
        } catch (e: Exception) {
            _Utils.printException(context, e)
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

        EventBus.getDefault().post(EventBusMessage.getInstance(FromTotalCount(), (totalDayCount / 1080).toString()))

        var setting = dc.getSetting(Setting.KEYS.buddha_duration)
        if(setting!=null){
            val duration = setting.long
            val second = duration % 60000 / 1000
            val miniteT = duration / 60000
            val minite = miniteT % 60
            val hour = miniteT / 60
            var tap = (duration / 1000 / circleSecond).toInt()
            tv_time.text = "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}  $tap"
        }else{
            setting = dc.getSetting(Setting.KEYS.buddha_startime)
            setting?.let{
                val duration = System.currentTimeMillis() - setting.long
                val second = duration % 60000 / 1000
                val miniteT = duration / 60000
                val minite = miniteT % 60
                val hour = miniteT / 60
                var tap = (duration / 1000 / circleSecond).toInt()
                tv_time.text = "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}  $tap"
            }
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

        tv_msg.setOnClickListener {
            _FingerUtils.showFingerPrintDialog(activity!!) {
                val dialog = SmsDialogFragment()
                dialog.show(activity!!.supportFragmentManager, "sms")
                tv_msg.visibility = View.GONE
            }
        }

        iv_speed_add.setOnClickListener {
            try {
                val bf = getBuddhaConfig()
                bf.speed = (bf.speed + 0.01).format(2).toFloat()
                dc.editBuddhaConfig(bf)

                loadBuddhaName()
                EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaConfigUpdate(), ""))
                _Utils.zhendong70(context!!)
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        iv_speed_minus.setOnClickListener {
            try {
                val bf = getBuddhaConfig()
                bf.speed = (bf.speed - 0.01).format(2).toFloat()
                dc.editBuddhaConfig(bf)

                loadBuddhaName()
                EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaConfigUpdate(), ""))
                _Utils.zhendong70(context!!)
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        iv_pitch_add.setOnClickListener {
            try {
                val bf = getBuddhaConfig()
                bf.pitch = (bf.pitch + 0.01).format(2).toFloat()
                dc.editBuddhaConfig(bf)

                loadBuddhaName()
                EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaConfigUpdate(), ""))
                _Utils.zhendong70(context!!)
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        iv_pitch_minus.setOnClickListener {
            try {
                val bf = getBuddhaConfig()
                bf.pitch = (bf.pitch - 0.01).format(2).toFloat()
                dc.editBuddhaConfig(bf)

                loadBuddhaName()
                EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaConfigUpdate(), ""))
                _Utils.zhendong70(context!!)
            } catch (e: Exception) {
                _Utils.printException(context, e)
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
                                        Toast.makeText(context, result.toString(), Toast.LENGTH_LONG).show()
//                                        AlertDialog.Builder(context).setMessage(result.toString()).show()
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
                        dc.addBuddhaConfig(BuddhaConfig(_Session.BUDDHA_MUSIC_NAME_ARR[which], file.length(), "md5", 1.0f, 1.0f, BuddhaType.计数念佛.toInt(), 600))
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
                _Utils.printException(context, e)
            }
        }

        layout_dayTotal.setOnClickListener {
            if (dc.getSetting(Setting.KEYS.is查看念佛记录需要验证, false).boolean) {
                _FingerUtils.showFingerPrintDialog(activity!!) {
                    val intent = Intent(context, BuddhaDetailActivity::class.java)
                    intent.putExtra("start", System.currentTimeMillis())
                    startActivity(intent)
                }
            } else {
                val intent = Intent(context, BuddhaDetailActivity::class.java)
                intent.putExtra("start", System.currentTimeMillis())
                startActivity(intent)
            }
        }

        layout_monthTotal.setOnClickListener {
            if (dc.getSetting(Setting.KEYS.is查看念佛记录需要验证, false).boolean) {
                _FingerUtils.showFingerPrintDialog(activity!!) {
                    startActivity(Intent(context, BuddhaActivity::class.java))
                }
            } else {
                startActivity(Intent(context, BuddhaActivity::class.java))
            }
        }

        layout_monthTotal.setOnLongClickListener {
            startActivity(Intent(context, BuddhaChartActivity::class.java))
            true
        }

        iv_buddha.setOnLongClickListener {
            var count = 0
            val view = View.inflate(context, R.layout.inflate_dialog_add_buddha, null)
            val etCount = view.findViewById<EditText>(R.id.et_count)
            val ivAdd = view.findViewById<ImageView>(R.id.iv_add)
            val ivMinus = view.findViewById<ImageView>(R.id.iv_minus)
            val spType = view.findViewById<Spinner>(R.id.sp_type)


            val aa = BuddhaType.values()
            val bb: MutableList<String> = ArrayList()
            aa.forEach {
                bb.add(it.toString())
            }
            fillSpinner(spType, bb)
            spType.setSelection(dc.getSetting(Setting.KEYS.tmp_selected, 3).int, true)

            spType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    e("--------------")
                    dc.editSetting(Setting.KEYS.tmp_selected, spType.selectedItemPosition)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }


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
                etCount.setText((++count).toString())
            }
            ivMinus.setOnClickListener {
                if (count > 1) {
                    etCount.setText((--count).toString())
                }
            }
            val btnOk = view.findViewById<Button>(R.id.btn_ok)
            val dialog = AlertDialog.Builder(context).setView(view).show()

            btnOk.setOnClickListener {
                dialog.dismiss()
                val type = BuddhaType.valueOf(spType.selectedItem.toString())
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

                buildBuddhaAndSave(tap * 1080, tap * avgDuration, stoptimeInMillis, type) { code, result ->
                    if (code == 0) {
                        uiHandler.post {
                            refreshTotalView()
                        }
                    }
                    uiHandler.post {
                        Toast.makeText(context, result.toString(), Toast.LENGTH_LONG).show()
//                        AlertDialog.Builder(context!!).setMessage(result.toString()).show()
                    }
                }
            }
            true
        }


        fab_stock.setOnClickListener {
            //region 在StockReportService里面执行
            if (!_Utils.isServiceRunning(context!!, StockService::class.qualifiedName!!)) {
                context!!.startService(Intent(context, StockService::class.java))
                animatorSuofang(abtn_stockAnimator)
            }
            //endregion
        }

        fab_stock.setOnLongClickListener { //region 在StockReportService里面执行
            if (_Utils.isServiceRunning(context!!, StockService::class.qualifiedName!!)) {
                context!!.stopService(Intent(context, StockService::class.java))
                stopAnimatorSuofang(abtn_stockAnimator)
            } else {
                context!!.startService(Intent(context, StockService::class.java))
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
                dc.deleteSetting(Setting.KEYS.muyu_duration)
                dc.deleteSetting(Setting.KEYS.muyu_startime)
                dc.deleteSetting(Setting.KEYS.muyu_stoptime)
            } else {
                AlertDialog.Builder(context).setItems(arrayOf("木鱼", "木鱼+引擎", "震动"), DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        0, 1 -> {
                            val intent = Intent(context!!, MuyuService::class.java)
                            intent.putExtra("type", which)
                            context?.startService(intent)
                        }
                        2 -> {
                            val intent = Intent(context!!, NianfoDarkRunActivity::class.java)
                            intent.putExtra("period", dc.getSetting(Setting.KEYS.muyu_period,1000).int)
                            context?.startActivity(intent)
                        }
                    }
                    animatorSuofang(fab_muyu_animator)
                }).show()
            }
        }
        fab_muyu.setOnLongClickListener {
            val view = View.inflate(context!!, R.layout.inflate_editbox, null)
            val et = view.findViewById<EditText>(R.id.et_value)
            et.inputType = InputType.TYPE_CLASS_NUMBER
            et.setText(dc.getSetting(Setting.KEYS.muyu_period, 666).string)
            AlertDialog.Builder(context!!).setView(view).setTitle("设置木鱼间隔时间(ms/m)").setPositiveButton("确定") { dialogInterface: DialogInterface, i: Int ->
                var cc = et.text.toString().toDouble()
                if (cc < 300.0) {
                    cc = cc * 60000 / 1080
                }
                dc.editSetting(Setting.KEYS.muyu_period, cc.toInt())
                muyuCircleSecond = cc.toInt()
                if (_Utils.isServiceRunning(context!!, MuyuService::class.qualifiedName!!)) {
                    isFromConfigChanged = true
                    context!!.stopService(Intent(context!!, MuyuService::class.java))

                    AlertDialog.Builder(context).setItems(arrayOf("木鱼", "木鱼+引擎", "震动"), DialogInterface.OnClickListener { dialog, which ->
                        when (which) {
                            0, 1 -> {
                                val intent = Intent(context!!, MuyuService::class.java)
                                intent.putExtra("type", which)
                                context?.startService(intent)
                            }
                            2 -> {
                                val intent = Intent(context!!, NianfoDarkRunActivity::class.java)
                                intent.putExtra("period", dc.getSetting(Setting.KEYS.muyu_period,1000).int)
                                context?.startActivity(intent)
                            }
                        }
                        animatorSuofang(fab_muyu_animator)
                    }).show()
                }
            }.show()

            true
        }

        fab_buddha.setOnClickListener {
            isLongClickBuddhaButton = false
            startOrStopBuddha()
        }

        fab_buddha.setOnLongClickListener {
            isLongClickBuddhaButton = true
            startOrStopBuddha()
            true
        }

//        fab_list.setOnClickListener {
//
//            if (dc.getSetting(Setting.KEYS.is查看念佛记录需要验证, false).boolean) {
//                _FingerUtils.showFingerPrintDialog(activity!!) {
//                    val intent = Intent(context, BuddhaDetailActivity::class.java)
//                    intent.putExtra("start", System.currentTimeMillis())
//                    startActivity(intent)
//                }
//            } else {
//                val intent = Intent(context, BuddhaDetailActivity::class.java)
//                intent.putExtra("start", System.currentTimeMillis())
//                startActivity(intent)
//            }
//        }
//        fab_list.setOnLongClickListener {
//
//            if (dc.getSetting(Setting.KEYS.is查看念佛记录需要验证, false).boolean) {
//                _FingerUtils.showFingerPrintDialog(activity!!) {
//                    startActivity(Intent(context, BuddhaActivity::class.java))
//                }
//            } else {
//                startActivity(Intent(context, BuddhaActivity::class.java))
//            }
//            true
//        }
        super.onViewCreated(view, savedInstanceState)
    }

    fun fillSpinner(spinner: Spinner, values: MutableList<String>) {
        val adapter = ArrayAdapter(context!!, R.layout.inflate_spinner, values)
        adapter.setDropDownViewResource(R.layout.inflate_spinner_dropdown)
        spinner.adapter = adapter
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
        tv_speed.text = "速${bf.speed.toFloat().format(2)}度"
        tv_pitch.text = "音${bf.pitch.toFloat().format(2)}调"
    }

    fun getBuddhaConfig(): BuddhaConfig {
        val musicName = dc.getSetting(Setting.KEYS.buddha_music_name, "阿弥陀佛.mp3")

        val file = _Session.getFile(musicName.string)
        var bf = dc.getBuddhaConfig(musicName.string, file.length())
        if (bf == null) {
            bf = BuddhaConfig(musicName.string, file.length(), "md5", 1.0f, 1.0f, BuddhaType.计数念佛.toInt(), 600)
        }
        return bf
    }

    var isLongClickBuddhaButton = false
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
                            Toast.makeText(context, result.toString(), Toast.LENGTH_LONG).show()
//                            AlertDialog.Builder(context!!).setMessage(result.toString()).show()
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
                val spType = view.findViewById<Spinner>(R.id.sp_type)
                val etDuration = view.findViewById<EditText>(R.id.et_duration)
                etDuration.setText(bf.circleSecond.toString())
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

                AlertDialog.Builder(context).setTitle(set.string).setView(view).setPositiveButton("更新", DialogInterface.OnClickListener { dialog, which ->

                    bf.circleSecond = etDuration.text.toString().toInt()
                    bf.type = spType.selectedItem.toString().toInt()
                    dc.editBuddhaConfig(bf)
                    isFromConfigChanged = true
                    circleSecond = getCurrentCircleSecond()  // 必须在dc.editBuddhaConfig(bf)之后

                    loadBuddhaName()
                    dialog.dismiss()

                    var list = ArrayList<BuddhaConfig>()
                    var bfs = dc.allBuddhaConfig
                    bfs.forEach {
                        if (!(_Session.BUDDHA_MUSIC_NAME_ARR.contains(it.name) && _Session.getFile(it.name).length() == it.size)) {
                            e(it.name)
                            list.add(it)
                        }
                    }
                    dc.deleteBuddhaConfigList(list)


                    EventBus.getDefault().post(EventBusMessage.getInstance(FromBuddhaConfigUpdate(), ""))
                }).show()
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
                if (bf.type > 9) {
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

        var buddha = BuddhaRecord(startTime, duration, count, buddhaType, "")

        _CloudUtils.addBuddha(context!!, buddha) { code, result ->
            when (code) {
                0 -> {
                    dc.addBuddha(buddha)
                }
                else -> {
                    e("code : $code , result : $result")
                }
            }
            callback.excute(code, result)
        }
    }

    lateinit var buddhaIntent: Intent
    private fun startOrStopBuddha() {
        try {
            if (!_Utils.isServiceRunning(context!!, BuddhaService::class.qualifiedName!!)) {
                checkDuration(BuddhaSource.点击开始按钮) { code, result ->
                    if (code == 0) {
                        dc.editSetting(Setting.KEYS.is显示念佛悬浮窗, sw_float_window.isChecked)
                        dc.editSetting(Setting.KEYS.is念佛自动暂停, isLongClickBuddhaButton)
                        context?.startService(buddhaIntent)
                        animatorSuofang(btn_buddha_animator)
                    } else {
                        uiHandler.post {
                            Toast.makeText(context, result.toString(), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                context?.stopService(buddhaIntent)
                stopAnimatorSuofang(btn_buddha_animator)
            }
        } catch (e: Exception) {
            e("BuddhaFragment.startOrStopBuddha  " + e.message!!)
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
                    if (source == BuddhaSource.念佛服务结束 && isLongClickBuddhaButton) {
                        buildBuddhaAndSave(tap.toInt() * 1080, duration, stoptimeInMillis, buddhaType.toBuddhaType()) { code, result ->
                            if (code == 0) {
                                dc.deleteSetting(Setting.KEYS.buddha_duration)
                                dc.deleteSetting(Setting.KEYS.buddha_stoptime)
                                refreshTotalView()
                                uiHandler.post {
                                    tv_time.text = ""
                                    Toast.makeText(context, result.toString(), Toast.LENGTH_LONG).show()
//                                    AlertDialog.Builder(context).setMessage(result.toString()).show()
                                }
                            }
                            callback?.excute(code, result)
                        }
                        return@post
                    }

                    if (dc.getSetting(Setting.KEYS.is暂存处理对话框, true).boolean) {
                        val dialog = AlertDialog.Builder(context).setMessage("缓存中存在念佛记录\n[ ${msg} ]\n是否存档？")
                        dialog.setNegativeButton("存档") { dialog, which ->
                            buildBuddhaAndSave(tap.toInt() * 1080, duration, stoptimeInMillis, buddhaType.toBuddhaType()) { code, result ->
                                if (code == 0) {
                                    dc.deleteSetting(Setting.KEYS.buddha_duration)
                                    dc.deleteSetting(Setting.KEYS.buddha_stoptime)
                                    refreshTotalView()
                                    uiHandler.post {
                                        tv_time.text = ""
                                        Toast.makeText(context, result.toString(), Toast.LENGTH_LONG).show()
//                                    AlertDialog.Builder(context).setMessage(result.toString()).show()
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
                        callback?.excute(0, "继续使用")
                    }

                } else {
                    dc.deleteSetting(Setting.KEYS.buddha_duration)
                    dc.deleteSetting(Setting.KEYS.buddha_stoptime)
                    refreshTotalView()
                    uiHandler.post {
                        tv_time.text = ""
                    }
                    callback?.excute(0, "重置完毕！")
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
            is FromBuddhaAutoCloud -> {
                val duration = bus.msg.toLong()
                val second = duration % 60000 / 1000
                val miniteT = duration / 1000 / 60
                val minite = miniteT % 60
                val hour = miniteT / 60
                var count = (duration / 1000 / circleSecond).toInt()
                var time = "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}"
                tv_time.text = "$time  ${count}"
            }
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