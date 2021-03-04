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
    var tap:Int

    /**
     * 单圈数量
     */
    var tapCount:Int

    /**
     * 1：计时念佛 11：计时计数念佛
     */
    var type:Int

    /**
     * 描述
     */
    var summary:String

    /**
     * @param id
     * @param startTime
     * @param duration
     * @param tap
     * @param tapCount
     * @param type
     * @param summary
     */
    constructor(id:UUID, startTime: DateTime, duration: Long, tap: Int,tapCount:Int, type: Int, summary: String) {
        this.id = id
        this.startTime = startTime
        this.duration = duration
        this.tap = tap
        this.tapCount = tapCount
        this.type = type
        this.summary = summary
    }

    /**
     * @param startTime
     * @param duration
     * @param tap
     * @param tapCount
     * @param type
     * @param summary
     */
    constructor(startTime: DateTime, duration: Long, tap: Int,tapCount:Int, type: Int, summary: String) {
        this.id = UUID.randomUUID()
        this.startTime = startTime
        this.duration = duration
        this.tap = tap
        this.tapCount = tapCount
        this.type = type
        this.summary = summary
    }
//
//    override fun equals(other: Any?): Boolean {
//        val tt = other as BuddhaRecord
//        return this.startTime==tt.startTime
//    }
}