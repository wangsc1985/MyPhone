package com.wang17.myphone.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import com.wang17.myphone.R
import com.wang17.myphone.dao.DataContext
import com.wang17.myphone.database.Location
import com.wang17.myphone.database.Setting
import com.wang17.myphone.event.LocationIsAutomaticEvent
import com.wang17.myphone.util._Session
import com.wang17.myphone.util._Utils.e
import com.wang17.myphone.util._Utils.getFilesWithSuffix
import com.wang17.myphone.util._Utils.printException
import kotlinx.android.synthetic.main.fragment_setting.*
import org.greenrobot.eventbus.EventBus
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class SettingFragment : Fragment() {

    // 值变量
    private var mDataContext: DataContext? = null
    private var settings: MutableList<Setting>? = null
    private var setAdapter: SettingListdAdapter? = null
    private var uiHandler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uiHandler = Handler()
        mDataContext = DataContext(context)
        settings = mDataContext!!.settings
        setAdapter = SettingListdAdapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            listView_setting.setOnItemClickListener { parent, view, position, id ->
                val setting = settings!![position]
                val isInBlackList = ignoreWords!!.contains(setting.name)
                AlertDialog.Builder(context!!).setItems(arrayOf("编辑", "删除", if (setting.level == 1) "取消关注" else "关注", if (isInBlackList) "拉白" else "拉黑", "刷新")) { dialog, which ->
                    when (which) {
                        0 -> editSettingItem(position)
                        1 -> AlertDialog.Builder(context!!).setMessage("要删除当前设置吗？").setPositiveButton("是") { dialog, which ->
                            mDataContext!!.deleteSetting(settings!![position].name)
                            settings = mDataContext!!.settings
                            setAdapter!!.notifyDataSetChanged()
                        }.setNegativeButton("否") { dialog, which ->
                            settings = mDataContext!!.settings
                            setAdapter!!.notifyDataSetChanged()
                        }.show()
                        2 -> {
                            mDataContext!!.editSettingLevel(settings!![position].name, if (setting.level == 1) 100 else 1)
                            settings = mDataContext!!.settings
                            setAdapter!!.notifyDataSetChanged()
                        }
                        3 -> {
                            if (isInBlackList) removeFromBlackList(setting.name) else add2BlackList(setting.name)
                            settings = mDataContext!!.settings
                            setAdapter!!.notifyDataSetChanged()
                        }
                        4 -> {
                            settings = mDataContext!!.settings
                            setAdapter!!.notifyDataSetChanged()
                        }
                    }
                }.show()
            }
            listView_setting.setOnItemLongClickListener(OnItemLongClickListener { parent, view, position, id ->
                editSettingItem(position)
                true
            })
            listView_setting.setAdapter(setAdapter)
        } catch (e: Exception) {
            printException(context, e)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onStart() {
        super.onStart()
        settings = mDataContext!!.settings
        setAdapter!!.notifyDataSetChanged()
    }

    var locationList: List<Location>? = null
    var index = 0


    private fun editSettingItem(position: Int) {
        val setting = settings!![position]
        if (ignoreWords!!.contains(setting.name)) {
            return
        }
        if (setting.string == "false" || setting.string == "true") {
            AlertDialog.Builder(context!!).setTitle(setting.name).setPositiveButton("是") { dialog, which ->
                mDataContext!!.editSetting(setting.name, true)
                settings = mDataContext!!.settings
                setAdapter!!.notifyDataSetChanged()
                if (setting.name == Setting.KEYS.map_location_isAutoChangeGear.toString()) {
                    EventBus.getDefault().post(LocationIsAutomaticEvent(true))
                }
            }.setNegativeButton("否") { dialog, which ->
                mDataContext!!.editSetting(setting.name, false)
                settings = mDataContext!!.settings
                setAdapter!!.notifyDataSetChanged()
                if (setting.name == Setting.KEYS.map_location_isAutoChangeGear.toString()) {
                    EventBus.getDefault().post(LocationIsAutomaticEvent(false))
                }
            }.show()
        } else {
            val v = View.inflate(context, R.layout.inflate_editbox, null)
            val input = v.findViewById<EditText>(R.id.et_value)
            input.setText(setting.string)
            AlertDialog.Builder(context!!).setTitle(setting.name).setView(v).setPositiveButton("修改") { dialog, which ->
                if (input.text.toString() != setting.string) {
                    mDataContext!!.editSetting(setting.name, input.text)
                    settings = mDataContext!!.settings
                    setAdapter!!.notifyDataSetChanged()
                }
            }.show()
        }
    }
    private val blackList: List<String>
        private get() {
            val blackList = mDataContext!!.getSetting(Setting.KEYS.设置黑名单, "black_list,").string
            val list = blackList.split(",".toRegex()).toTypedArray()
            val result: MutableList<String> = ArrayList()
            for (i in list.indices) {
                result.add(list[i])
            }
            return result
        }

    private fun add2BlackList(key: String) {
        var key = key
        key += ","
        val blackList = mDataContext!!.getSetting(Setting.KEYS.设置黑名单, "black_list,").string
        if (!blackList.contains(key)) {
            mDataContext!!.editSetting(Setting.KEYS.设置黑名单, blackList + key)
        }
    }

    private fun removeFromBlackList(key: String) {
        var key: String? = key
        key += ","
        val blackList = mDataContext!!.getSetting(Setting.KEYS.设置黑名单, "black_list,").string
        if (blackList.contains(key!!)) {
            mDataContext!!.editSetting(Setting.KEYS.设置黑名单, blackList.replace(key, ""))
        }
    }

    private var ignoreWords: List<String>? = null

    protected inner class SettingListdAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return settings!!.size
        }

        override fun getItem(position: Int): Any {
            return settings!![position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            var convertView = convertView
            try {
                convertView = View.inflate(context, R.layout.inflate_setting, null)
                val set = settings!![position]
                val root = convertView.findViewById<LinearLayout>(R.id.layout_root)
                val textViewKey = convertView.findViewById<TextView>(R.id.tv_dateTime)
                val textViewValue = convertView.findViewById<TextView>(R.id.textView11)
                val textView2 = convertView.findViewById<TextView>(R.id.tv_body)
                textViewKey.text = set.name
                textViewValue.text = set.string
                textView2.visibility = View.GONE
                if (ignoreWords!!.contains(set.name)) {
                    textViewKey.setTextColor(ContextCompat.getColor(context!!, R.color.color_gray))
                    textViewValue.setTextColor(ContextCompat.getColor(context!!, R.color.color_gray))
                }
                if (set.level == 1) {
//                    textViewKey.setTextColor(Color.RED)
                    root.setBackgroundColor(resources.getColor(R.color.floating_window_background,null))
                }
            } catch (e: Exception) {
                printException(context, e)
            }
            return convertView
        }

        override fun notifyDataSetChanged() {
            ignoreWords = blackList
            //
            val ignoreSettings: MutableList<Setting> = ArrayList()
            if (settings != null) {
                val iterator = settings!!.iterator()
                while (iterator.hasNext()) {
                    val setting = iterator.next()
                    if (ignoreWords!!.contains(setting.name)) {
                        ignoreSettings.add(setting)
                        iterator.remove()
                    }
                }
            }
            settings!!.addAll(ignoreSettings)
            super.notifyDataSetChanged()
        }
    }
}