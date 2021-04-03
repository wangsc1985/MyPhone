package com.wang17.myphone.database

import android.telephony.SmsMessage
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.structure.SmsStatus
import com.wang17.myphone.structure.SmsType
import java.util.*

/**
 * Created by 阿弥陀佛 on 2016/10/24.
 */
class PhoneMessage {
    /**
     * 获取所有短息记录
     * _id => 短消息序号 如100
     * thread_id => 对话的序号 如100
     * address => 发件人地址，手机号.如+8613811810000
     * person => 发件人，返回一个数字就是联系人列表里的序号，陌生人为null
     * date => 日期  long型。如1256539465022
     * protocol => 协议 0 SMS_RPOTO, 1 MMS_PROTO
     * read => 是否阅读 0未读， 1已读
     * status => 状态 -1接收，0 complete, 64 pending, 128 failed
     * type => 类型 1是接收到的，2是已发出
     * body => 短消息内容
     * service_center => 短信服务中心号码编号。如+8613800755500
     */
    var id = UUID.randomUUID()
    var address = "" // 发送方
    var body = ""
    var type = SmsType.接收到
    var status = SmsStatus.接收 // 值为-1，说明此数据已删除。
    var createTime = DateTime()

    constructor() {}
    constructor(sms: SmsMessage) {
        id = UUID.randomUUID()
        address = sms.originatingAddress?:""
        body = sms.messageBody
        type = SmsType.新信息
        address = sms.originatingAddress?:""
        body = sms.messageBody
        status = SmsStatus.接收
        createTime = DateTime(sms.timestampMillis)
    }
}