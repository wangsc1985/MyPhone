package com.wang17.myphone.database

import java.util.*

class BuddhaFile(

        var id:UUID,
        /**
         * 文件名
         */
        var name: String,
        /**
         * 文件大小
         */
        var size: Long,
        /**
         * md5
         */
        var md5: String,
        /**
         * 音调
         */
        var pitch: Float,
        /**
         * 音速
         */
        var speed: Float,
        /**
         * 计数类型
         */
        var type: Int,
        /**
         * 每圈时间（秒）
         */
        var circleSecond: Int
) {
        constructor(name: String, size: Long, md5: String, pitch: Float, speed: Float, type: Int, circleSecond: Int) : this(UUID.randomUUID(),name,size,md5,pitch,speed,type,circleSecond)
}