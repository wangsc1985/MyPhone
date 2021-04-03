package com.wang17.myphone.database;

import android.telephony.SmsMessage;

import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.structure.SmsStatus;
import com.wang17.myphone.structure.SmsType;

/**
 * Created by 阿弥陀佛 on 2016/10/24.
 */
public class PhoneMessage {

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

    private int id;
    private int threadId;
    private String phoneNumber; //接收方
    private String address; // 发送方
    private String body;
    private SmsType type;
    private SmsStatus status;// 值为-1，说明此数据已删除。
    private DateTime createTime;
    private long updateTime=1;
    private long syncTime=1;

    public PhoneMessage(){
    }

    public PhoneMessage(SmsMessage sms){
        this.id=-1;
        this.threadId = -1;
        this.phoneNumber = "新信息自动转换";
        this.address= sms.getOriginatingAddress();
        this.body = sms.getMessageBody();
        this.type = SmsType.新信息;
        this.address= sms.getOriginatingAddress();
        this.body = sms.getMessageBody();
        this.status = SmsStatus.接受;
        this.createTime = new DateTime(sms.getTimestampMillis());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    /**
     * 收件人地址
     * @return
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * 收件人地址
     * @param phoneNumber
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * 发件人地址
     * @return
     */
    public String getAddress() {
        return address;
    }

    /**
     * 发件人地址
     * @param address
     */
    public void setAddress(String address) {
        this.address = address;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public DateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(DateTime createTime) {
        this.createTime = createTime;
    }

    public SmsType getType() {
        return type;
    }

    public void setType(SmsType type) {
        this.type = type;
    }

    public SmsStatus getStatus() {
        return status;
    }

    public void setStatus(SmsStatus status) {
        this.status = status;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public long getSyncTime() {
        return syncTime;
    }

    public void setSyncTime(long syncTime) {
        this.syncTime = syncTime;
    }
}
