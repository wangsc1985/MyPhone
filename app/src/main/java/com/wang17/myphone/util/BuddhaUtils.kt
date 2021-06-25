package com.wang17.myphone.util

import android.content.Context
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.database.BuddhaRecord
import com.wang17.myphone.dao.DataContext
import java.util.*
import kotlin.collections.ArrayList

object BuddhaUtils {
    /**
     * 整合从制定日期后的数据，日期是从startTIme当前起始算起，建议将添加记录前的最后一个buddha记录的日期传递进来
     */
    fun integrate(context: Context, startTime: DateTime): MutableList<BuddhaRecord> {
        val dc = DataContext(context)
        val buddhaList = dc.getBuddhaListStartDate(startTime)
        val removeList: MutableList<BuddhaRecord> = ArrayList()
        var tmp: BuddhaRecord? = null
        buddhaList.forEach { buddha ->
            if (tmp != null) {
                if (buddha.startTime.get(Calendar.DAY_OF_YEAR) != tmp!!.startTime.get(Calendar.DAY_OF_YEAR)) {
                    dc.editBuddha(tmp)
                    tmp = buddha
                } else {
                    if (buddha.startTime.hour == tmp!!.startTime.hour) {
                        tmp!!.duration += buddha.duration
                        tmp!!.count += buddha.count
                        removeList.add(buddha)
                    } else {
                        dc.editBuddha(tmp)
                        tmp = buddha
                    }
                }
            } else {
                tmp = buddha
            }
        }
        tmp?.let {
            dc.editBuddha(tmp)
        }
        buddhaList.removeAll(removeList)
        dc.deleteBuddhaList(removeList)
        return buddhaList
    }

    fun integrate1(latestBuddha: BuddhaRecord?, newBuddhaList: MutableList<BuddhaRecord>): MutableList<BuddhaRecord> {
        val removeList: MutableList<BuddhaRecord> = ArrayList()
        var tmp: BuddhaRecord? = null
        val integrateBuddhaList: MutableList<BuddhaRecord> = ArrayList()
        if (latestBuddha != null){
            integrateBuddhaList.add(latestBuddha)
        }
        integrateBuddhaList.addAll(newBuddhaList)
        integrateBuddhaList.forEach { buddha ->
            if (tmp != null) {
                if (buddha.startTime.get(Calendar.DAY_OF_YEAR) != tmp!!.startTime.get(Calendar.DAY_OF_YEAR)) {
                    tmp = buddha
                } else {
                    if (buddha.startTime.hour == tmp!!.startTime.hour) {
                        tmp!!.duration += buddha.duration
                        tmp!!.count += buddha.count
                        removeList.add(buddha)
                    } else {
                        tmp = buddha
                    }
                }
            } else {
                tmp = buddha
            }
        }
        integrateBuddhaList.removeAll(removeList)
        return integrateBuddhaList
    }
    fun integrate(latestBuddha: BuddhaRecord?, newBuddhaList: MutableList<BuddhaRecord>) {
        val removeList: MutableList<BuddhaRecord> = ArrayList()
        var tmp: BuddhaRecord? = latestBuddha
        newBuddhaList.forEach { buddha ->
            if (tmp != null) {
                if (buddha.startTime.get(Calendar.DAY_OF_YEAR) != tmp!!.startTime.get(Calendar.DAY_OF_YEAR)) {
                    tmp = buddha
                } else {
                    if (buddha.startTime.hour == tmp!!.startTime.hour) {
                        tmp!!.duration += buddha.duration
                        tmp!!.count += buddha.count
                        removeList.add(buddha)
                    } else {
                        tmp = buddha
                    }
                }
            } else {
                tmp = buddha
            }
        }
        newBuddhaList.removeAll(removeList)
    }
}