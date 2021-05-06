package com.wang17.myphone.database

import java.util.*

class BuddhaConfig(

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
         * 计数类型 0听佛，1计时，10听佛计数，11念佛计数  (所有计数的类型，都是两位数。所有不计数的类型，都是单位数)
         */
        var type: Int,
        /**
         * 每圈时间（秒）
         */
        var circleSecond: Int
) {
        constructor(name: String, size: Long, md5: String, pitch: Float, speed: Float, type: Int, circleSecond: Int) : this(UUID.randomUUID(),name,size,md5,pitch,speed,type,circleSecond)
}