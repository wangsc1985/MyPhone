package com.wang17.myphone.database

import com.wang17.myphone.model.DateTime
import java.util.*

class BuddhaRecord {
    var id:UUID

    /**
     * 开始时间
     */
    var startTime:DateTime

    /**
     * 总时长
     */
    var duration:Long

    /**
     * 总数量
     */
    var count:Int

    /**
     * 0：耳听念佛 1：计时念佛 10:听佛念佛 11：计数念佛  13：散念  >=10计数 <10不计数
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
     * @param count
     * @param type
     * @param summary
     */
    constructor(id:UUID, startTime: DateTime, duration: Long, count:Int, type: BuddhaType, summary: String) {
        this.id = id
        this.startTime = startTime
        this.duration = duration
        this.type = type.toInt()
        this.count = if(this.type>=10) count else 0
        this.summary = summary
    }

    /**
     * @param startTime
     * @param duration
     * @param count
     * @param type
     * @param summary
     */
    constructor(startTime: DateTime, duration: Long, count:Int, type: BuddhaType, summary: String) {
        this.id = UUID.randomUUID()
        this.startTime = startTime
        this.duration = duration
        this.type = type.toInt()
        this.count = if(this.type>=10) count else 0
        this.summary = summary
    }
//
//    override fun equals(other: Any?): Boolean {
//        val tt = other as BuddhaRecord
//        return this.startTime==tt.startTime
//    }
}

enum class BuddhaType(var value:Int){
    听佛(0),
    计时念佛(1),
    听佛计数(10),
    计数念佛(11),
    散念(13);

    fun toInt():Int{
        return value
    }

    companion object{
        fun fromInt(value :Int):BuddhaType{
            when(value){
                0-> return 听佛
                1-> return 计时念佛
                10-> return 听佛计数
                11-> return 计数念佛
                else-> return 散念
            }
        }
    }
}