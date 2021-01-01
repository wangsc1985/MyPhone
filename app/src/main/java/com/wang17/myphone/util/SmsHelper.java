package com.wang17.myphone.util;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;

import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.model.database.PhoneMessage;
import com.wang17.myphone.structure.SmsStatus;
import com.wang17.myphone.structure.SmsType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 阿弥陀佛 on 2016/10/24.
 */

public class SmsHelper {

    public static final Uri CONTENT_URI = Uri.parse("content://sms");
    private Context context;

    public SmsHelper(Context context) {
        this.context = context;
    }

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
    public List<PhoneMessage> getSMS(SmsType smsType, DateTime smsDate) {

        List<PhoneMessage> list = new ArrayList<>();
        try {
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.parse("content://sms");

            Cursor cursor = cr.query(uri, new String[]{"_id", "thread_id", "address", "date", "status", "type", "body"},
                    "type=? and date>?", new String[]{smsType.toInt() + "", smsDate.getTimeInMillis() + ""}, "date desc");
            if (cursor.moveToFirst()) {
                int id;
                int threadId;
                String address;
                DateTime date;
                int status;
                int type;
                String body;
                int idCloum = cursor.getColumnIndex("_id");
                int threadIdCloum = cursor.getColumnIndex("thread_id");
                int addressCloum = cursor.getColumnIndex("address");
                int dateCloum = cursor.getColumnIndex("date");
                int statusCloum = cursor.getColumnIndex("status");
                int typeCloum = cursor.getColumnIndex("type");
                int bodyCloum = cursor.getColumnIndex("body");
                do {
                    id = cursor.getInt(idCloum);
                    threadId = cursor.getInt(threadIdCloum);
                    address = cursor.getString(addressCloum);
                    date = new DateTime(Long.parseLong(cursor.getColumnName(dateCloum)));
                    status = cursor.getInt(statusCloum);
                    type = cursor.getInt(typeCloum);
                    body = cursor.getString(bodyCloum);

                    PhoneMessage user = new PhoneMessage();
                    user.setId(id);
                    user.setThreadId(threadId);
                    user.setAddress(address);
                    user.setBody(body);
                    user.setType(SmsType.fromInt(type));
                    user.setStatus(SmsStatus.fromInt(status));
                    user.setCreateTime(date);
                    list.add(user);
                } while (cursor.moveToNext());
            }
        } catch (NumberFormatException e) {
            _Utils.printException(context,e);
        }
        return list;
    }
    public static List<PhoneMessage> getSMS(Context context, SmsType smsType) {

        List<PhoneMessage> list = new ArrayList<>();
        try {
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.parse("content://sms");

            Cursor cursor = cr.query(uri, new String[]{"_id", "thread_id", "address", "date", "status", "type", "body"},
                    "type=?", new String[]{smsType.toInt() + ""}, "date desc");
            if (cursor.moveToFirst()) {
                int id;
                int threadId;
                String address;
                DateTime date;
                int status;
                int type;
                String body;
                int idCloum = cursor.getColumnIndex("_id");
                int threadIdCloum = cursor.getColumnIndex("thread_id");
                int addressCloum = cursor.getColumnIndex("address");
                int dateCloum = cursor.getColumnIndex("date");
                int statusCloum = cursor.getColumnIndex("status");
                int typeCloum = cursor.getColumnIndex("type");
                int bodyCloum = cursor.getColumnIndex("body");
                do {
                    id = cursor.getInt(idCloum);
                    threadId = cursor.getInt(threadIdCloum);
                    address = cursor.getString(addressCloum);
                    date = new DateTime(Long.parseLong(cursor.getColumnName(dateCloum)));
                    status = cursor.getInt(statusCloum);
                    type = cursor.getInt(typeCloum);
                    body = cursor.getString(bodyCloum);

                    PhoneMessage user = new PhoneMessage();
                    user.setId(id);
                    user.setThreadId(threadId);
                    user.setAddress(address);
                    user.setBody(body);
                    user.setType(SmsType.fromInt(type));
                    user.setStatus(SmsStatus.fromInt(status));
                    user.setCreateTime(date);
                    list.add(user);
                } while (cursor.moveToNext());
            }
        } catch (NumberFormatException e) {
            _Utils.printException(context,e);
        }
        return list;
    }

    /**
     * 逆序获取大于某一时间点，并且不包括这个时间点。
     *
     * date可以设置为null，相当于获取所有短信
     *
     * @param date
     * @return
     */
    public List<PhoneMessage> getSMS(DateTime date) {

        List<PhoneMessage> phoneMessageList = new ArrayList<>();
        try {
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.parse("content://sms");

            Cursor cursor;
            if(date==null) {
                cursor = cr.query(uri, new String[]{"_id", "thread_id", "address", "date", "status", "type", "body"},
                        null, null, "date desc");
            }else{
                cursor = cr.query(uri, new String[]{"_id", "thread_id", "address", "date", "status", "type", "body"},
                    "date>?", new String[]{date.getTimeInMillis() + ""}, "date desc");
            }
            if (cursor.moveToFirst()) {
                int id;
                int threadId;
                String address;
                DateTime xxxDate;
                int status;
                int type;
                String body;
                int idCloum = cursor.getColumnIndex("_id");
                int threadIdCloum = cursor.getColumnIndex("thread_id");
                int addressCloum = cursor.getColumnIndex("address");
                int dateCloum = cursor.getColumnIndex("date");
                int statusCloum = cursor.getColumnIndex("status");
                int typeCloum = cursor.getColumnIndex("type");
                int bodyCloum = cursor.getColumnIndex("body");
                do {
                    id = cursor.getInt(idCloum);
                    threadId = cursor.getInt(threadIdCloum);
                    address = cursor.getString(addressCloum);
                    xxxDate = new DateTime(cursor.getLong(dateCloum));
                    status = cursor.getInt(statusCloum);
                    type = cursor.getInt(typeCloum);
                    body = cursor.getString(bodyCloum);

                    PhoneMessage phoneMessage = new PhoneMessage();
                    phoneMessage.setId(id);
                    phoneMessage.setThreadId(threadId);
                    phoneMessage.setAddress(address);
                    phoneMessage.setBody(body);
                    phoneMessage.setType(SmsType.fromInt(type));
                    phoneMessage.setStatus(SmsStatus.fromInt(status));
                    phoneMessage.setCreateTime(xxxDate);
                    phoneMessageList.add(phoneMessage);
                } while (cursor.moveToNext());
            }
        } catch (NumberFormatException e) {
            _Utils.printException(context,e);
        }
        return phoneMessageList;
    }

    /**
     * 通过 信息类型 和 信息内容 查询手机信息
     * @param smsType
     * @param bodyContains
     * @return
     */
    public List<PhoneMessage> get(SmsType smsType, String bodyContains) {

        List<PhoneMessage> list = new ArrayList<>();
        try {
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.parse("content://sms");
            Cursor cursor = cr.query(uri, new String[]{"_id", "thread_id", "address", "date", "status", "type", "body"},
                    "type=? and body like ?", new String[]{smsType.toInt() + "", "%" + bodyContains + "%"}, "date ASC");
            if (cursor.moveToFirst()) {
                int id;
                int threadId;
                String address;
                DateTime date;
                int status;
                int type;
                String body;
                int idCloum = cursor.getColumnIndex("_id");
                int threadIdCloum = cursor.getColumnIndex("thread_id");
                int addressCloum = cursor.getColumnIndex("address");
                int dateCloum = cursor.getColumnIndex("date");
                int statusCloum = cursor.getColumnIndex("status");
                int typeCloum = cursor.getColumnIndex("type");
                int bodyCloum = cursor.getColumnIndex("body");
                do {
                    id = cursor.getInt(idCloum);
                    threadId = cursor.getInt(threadIdCloum);
                    address = cursor.getString(addressCloum);
                    date = new DateTime(cursor.getLong(dateCloum));
                    status = cursor.getInt(statusCloum);
                    type = cursor.getInt(typeCloum);
                    body = cursor.getString(bodyCloum);

                    PhoneMessage user = new PhoneMessage();
                    user.setId(id);
                    user.setThreadId(threadId);
                    user.setAddress(address);
                    user.setBody(body);
                    user.setType(SmsType.fromInt(type));
                    user.setStatus(SmsStatus.fromInt(status));
                    user.setCreateTime(date);
                    list.add(user);
                } while (cursor.moveToNext());
            }
        } catch (NumberFormatException e) {
            _Utils.printException(context,e);
        }
        return list;
    }

    /**
     * 通过 信息类型 和 对方号码 查询手机信息
     * @param phoneNumber
     * @return
     */
    public List<PhoneMessage> getSMSRecrodByAddress( String phoneNumber) {

        List<PhoneMessage> list = new ArrayList<>();
        try {
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.parse("content://sms");
            Cursor cursor = cr.query(uri, new String[]{"_id", "thread_id", "address", "date", "status", "type", "body"},
                    "address=?", new String[]{ phoneNumber}, "date ASC");
            if (cursor.moveToFirst()) {
                int id;
                int threadId;
                String address;
                DateTime date;
                int status;
                int type;
                String body;
                int idCloum = cursor.getColumnIndex("_id");
                int threadIdCloum = cursor.getColumnIndex("thread_id");
                int addressCloum = cursor.getColumnIndex("address");
                int dateCloum = cursor.getColumnIndex("date");
                int statusCloum = cursor.getColumnIndex("status");
                int typeCloum = cursor.getColumnIndex("type");
                int bodyCloum = cursor.getColumnIndex("body");
                do {
                    id = cursor.getInt(idCloum);
                    threadId = cursor.getInt(threadIdCloum);
                    address = cursor.getString(addressCloum);
                    date = new DateTime(cursor.getLong(dateCloum));
                    status = cursor.getInt(statusCloum);
                    type = cursor.getInt(typeCloum);
                    body = cursor.getString(bodyCloum);

                    PhoneMessage user = new PhoneMessage();
                    user.setId(id);
                    user.setThreadId(threadId);
                    user.setAddress(address);
                    user.setBody(body);
                    user.setType(SmsType.fromInt(type));
                    user.setStatus(SmsStatus.fromInt(status));
                    user.setCreateTime(date);
                    list.add(user);
                } while (cursor.moveToNext());
            }
        } catch (NumberFormatException e) {
            _Utils.printException(context,e);
        }
        return list;
    }

    public PhoneMessage getSMS(SmsType smsType, long dateTime, String body, String number) {

        try {
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.parse("content://sms");
            Cursor cursor = cr.query(uri, new String[]{"_id", "thread_id", "address", "date", "status", "type", "body"},
                    "type=? and date=? and body=? and address=?", new String[]{smsType.toInt() + "", dateTime + "", body, number}, null);
            if (cursor.moveToFirst()) {
                int id;
                int threadId;
                String address;
                DateTime date;
                int status;
                int type;
                String xxxBody;
                int idCloum = cursor.getColumnIndex("_id");
                int threadIdCloum = cursor.getColumnIndex("thread_id");
                int addressCloum = cursor.getColumnIndex("address");
                int dateCloum = cursor.getColumnIndex("date");
                int statusCloum = cursor.getColumnIndex("status");
                int typeCloum = cursor.getColumnIndex("type");
                int bodyCloum = cursor.getColumnIndex("body");

                id = cursor.getInt(idCloum);
                threadId = cursor.getInt(threadIdCloum);
                address = cursor.getString(addressCloum);
                date = new DateTime(cursor.getLong(dateCloum));
                status = cursor.getInt(statusCloum);
                type = cursor.getInt(typeCloum);
                xxxBody = cursor.getString(bodyCloum);

                PhoneMessage user = new PhoneMessage();
                user.setId(id);
                user.setThreadId(threadId);
                user.setAddress(address);
                user.setBody(xxxBody);
                user.setType(SmsType.fromInt(type));
                user.setStatus(SmsStatus.fromInt(status));
                user.setCreateTime(date);
                return user;
            }
        } catch (NumberFormatException e) {
            _Utils.printException(context,e);
        }
        return null;
    }

    /**
     * 发送短信息
     */
    public void sendSMS(PhoneMessage user) {
        try {
            SmsManager sms = SmsManager.getDefault();
            PendingIntent pi = PendingIntent.getActivity(context, 0, new Intent(), 0);
            if (user.getBody().length() > 70) {
                ArrayList<String> msgs = sms.divideMessage(user.getBody());
                for (String msg : msgs) {
                    sms.sendTextMessage(user.getPhoneNumber(), null, msg, pi, null);
                }
            } else {
                sms.sendTextMessage(user.getPhoneNumber(), null, user.getBody(), pi, null);
            }
        } catch (Exception e) {
            _Utils.printException(context,e);
        }
    }

    /**
     * 删除短信息
     * f ：  ture  单个删除
     * f ：  false 全部删除
     */
    public void delSms(PhoneMessage user, boolean f) {
        try {
            ContentResolver cr = context.getContentResolver();
            if (f == true) {
                Uri url = Uri.parse("content://sms");
                cr.delete(url, "_id = ?", new String[]{user.getId() + ""});
            } else {
                Uri url = Uri.parse("content://sms");
                cr.delete(url, null, null);
            }
        } catch (Exception e) {
            _Utils.printException(context,e);
        }
    }

    public void setToReaded(int id) {
        try {
            Uri uri = Uri.parse("content://sms");
            ContentResolver cr = context.getContentResolver();

            ContentValues values = new ContentValues();
            values.put("read", 1);
            cr.update(uri, values, "_id=?", new String[]{id+""});
        } catch (Exception e) {
            _Utils.printException(context,e);
        }
    }

    /**
     * 获取未读和已读短信数目
     * int i
     * i : 1接受 已读
     * i : 2 接受未读
     * i : 3 发送已读
     * i : 4 发送未读
     */
    public int getSMSCount(int i) {
        int smsCount = 0;
        try {
            Uri uri = Uri.parse("content://sms");
            ContentResolver cr = context.getContentResolver();
            if (i == 1) {
                Cursor cursor = cr.query(uri, null, "type =1 and read=1", null, null);
                smsCount = cursor.getCount();
            }
            if (i == 2) {
                Cursor cursor = cr.query(uri, null, "type =1 and read=0", null, null);
                smsCount = cursor.getCount();
            }
            if (i == 3) {
                Cursor cursor = cr.query(uri, null, "type =2 and read=1", null, null);
                smsCount = cursor.getCount();
            }
            if (i == 4) {
                Cursor cursor = cr.query(uri, null, "type =2 and read=0", null, null);
                smsCount = cursor.getCount();
            }
        } catch (Exception e) {
            _Utils.printException(context,e);
        }
        return smsCount;
    }

    //获取彩信 数目
    public int getSMSMCount(int i) {
        int smsMCount = 0;
        try {
            Uri uri = Uri.parse("content://mms/inbox");
            ContentResolver cr = context.getContentResolver();
            if (i == 1) {
                Cursor cursor = cr.query(uri, null, "type =1 and read=1", null, null);
                smsMCount = cursor.getCount();
            }
            if (i == 2) {
                Cursor cursor = cr.query(uri, null, "type =1 and read=0", null, null);
                smsMCount = cursor.getCount();

            }
        } catch (Exception e) {
            _Utils.printException(context,e);
        }
        return smsMCount;
    }
}
