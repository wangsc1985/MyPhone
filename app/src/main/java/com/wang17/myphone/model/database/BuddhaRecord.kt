package com.wang17.myphone.model.database

import com.wang17.myphone.model.DateTime
import java.util.*

class BuddhaRecord {
    var id:UUID
    var startTime:DateTime
    var duration:Long
    var count:Int
    var type:Int
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
}