package com.wang17.myphone.model.database

import com.wang17.myphone.model.DateTime
import java.util.*

class BuddhaRecord {
    var id:UUID

    /**
     * 开始时间
     */
    var startTime:DateTime

    /**
     * 念佛总时长
     */
    var duration:Long

    /**
     * 总共念了几圈
     */
    var count:Int

//    /**
//     * 单圈数量
//     */
//    var countTap:Int

    /**
     * 1：计时念佛 11：计时计数念佛
     */
    var type:Int

    /**
     * 描述
     */
    var summary:String

    constructor(id:UUID,startTime: DateTime, duration: Long, count: Int, type: Int, summary: String) {
        this.id = id
        this.startTime = startTime
        this.duration = duration
        this.count = count
        this.type = type
        this.summary = summary
    }

    constructor(startTime: DateTime, duration: Long, count: Int, type: Int, summary: String) {
        this.id = UUID.randomUUID()
        this.startTime = startTime
        this.duration = duration
        this.count = count
        this.type = type
        this.summary = summary
    }
//
//    override fun equals(other: Any?): Boolean {
//        val tt = other as BuddhaRecord
//        return this.startTime==tt.startTime
//    }
}