package com.wang17.myphone.database;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.wang17.myphone.structure.RepayType;

/**
 * Created by 阿弥陀佛 on 2015/11/18.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int VERSION = 52;
    private static final String DATABASE_NAME = "mp.db";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建数据库后，对数据库的操作
        try {
            db.execSQL("create table if not exists setting("
                    + "name TEXT PRIMARY KEY,"
                    + "value TEXT,"
                    + "level INT NOT NULL DEFAULT 100)");
            db.execSQL("create table if not exists record("
                    + "id TEXT PRIMARY KEY,"
                    + "start LONG,"
                    + "interval INTEGER,"
                    + "item TEXT,"
                    + "summary TEXT)");
            db.execSQL("create table if not exists markDay("
                    + "id TEXT PRIMARY KEY,"
                    + "dateTime LONG,"
                    + "item TEXT NOT NULL DEFAULT '',"
                    + "summary TEXT NOT NULL DEFAULT '')");
            db.execSQL("create table if not exists creditCard("
                    + "id TEXT PRIMARY KEY,"
                    + "cardName TEXT,"
                    + "cardNumber TEXT,"
                    + "bankName TEXT,"
                    + "billDay INTEGER,"
                    + "repayDay INTEGER,"
                    + "annualFee REAL,"
                    + "creditLine REAL,"
                    + "cardType TEXT,"
                    + "balance REAL,"
                    + "isBlackList TEXT,"
                    + "isVisible TEXT,"
                    + "repayType TEXT)");
            db.execSQL("create table if not exists billRecord("
                    + "id TEXT PRIMARY KEY,"
                    + "dateTime LONG,"
                    + "cardNumber TEXT,"
                    + "country TEXT,"
                    + "currency TEXT,"
                    + "location TEXT,"
                    + "money REAL,"
                    + "summary TEXT,"
                    + "balance REAL,"
                    + "smsId int)");
            db.execSQL("create table if not exists sms("
                    + "id int PRIMARY KEY,"
                    + "threadId int,"
                    + "userName TEXT,"
                    + "phoneNumber TEXT,"
                    + "address TEXT,"
                    + "body TEXT,"
                    + "type int,"
                    + "status int,"
                    + "read int)");
            db.execSQL("create table if not exists billStatement("
                    + "id TEXT PRIMARY KEY,"
                    + "cardNumber TEXT,"
                    + "startDate LONG,"
                    + "endDate LONG,"
                    + "repayDate LONG,"
                    + "bill REAL,"
                    + "billRemain REAL)");
            db.execSQL("create table if not exists playList("
                    + "id TEXT PRIMARY KEY,"
                    + "name TEXT,"
                    + "currentSongId TEXT,"
                    + "currentSongPosition int,"
                    + "isDefault TEXT)");
//            db.execSQL("insert into playList values('" + _Session.UUID_NULL.toString() + "','默认列表','" + _Session.UUID_NULL.toString() + "',0)");
            db.execSQL("create table if not exists song("
                    + "id TEXT PRIMARY KEY,"
                    + "playListId TEXT,"
                    + "fileName TEXT,"
                    + "title TEXT,"
                    + "duration int,"
                    + "singer TEXT,"
                    + "album TEXT,"
                    + "year TEXT,"
                    + "type TEXT,"
                    + "size TEXT,"
                    + "fileUrl TEXT)");

            db.execSQL("create table if not exists location("
                    + "Id TEXT PRIMARY KEY,"
                    + "UserId TEXT,"
                    + "LocationType INT,"
                    + "Longitude REAL,"
                    + "Latitude REAL,"
                    + "Accuracy REAL,"
                    + "Provider TEXT,"
                    + "Speed REAL,"
                    + "Bearing REAL,"
                    + "Satellites INT,"
                    + "Country TEXT,"
                    + "Province TEXT,"
                    + "City TEXT,"
                    + "CityCode TEXT,"
                    + "District TEXT,"
                    + "AdCode TEXT,"
                    + "Address TEXT,"
                    + "PoiName TEXT,"
                    + "Time LONG,"
                    + "Summary TEXT)");
            db.execSQL("create table if not exists dayItem("
                    + "id TEXT PRIMARY KEY,"
                    + "name TEXT,"
                    + "summary TEXT,"
                    + "targetInHour INT)");
            db.execSQL("insert into dayItem values('00000000-0000-0000-0000-000000000000','持戒记录','',48,1,1,1)");
            db.execSQL("create table if not exists tallyPlan("
                    + "id TEXT PRIMARY KEY,"
                    + "interval LONG,"
                    + "item TEXT,"
                    + "summary TEXT)");
            db.execSQL("create table if not exists bankToDo("
                    + "id TEXT PRIMARY KEY,"
                    + "dateTime LONG,"
                    + "bankName TEXT,"
                    + "cardNumber TEXT,"
                    + "money REAL)");
            db.execSQL("create table if not exists stock("
                    + "id TEXT PRIMARY KEY,"
                    + "code TEXT,"
                    + "name TEXT,"
                    + "cost REAL,"
                    + "type INT,"
                    + "amount REAL,"
                    + "exchange TEXT,"
                    + "profit REAL)");
            db.execSQL("create table if not exists trade("
                    + "id TEXT PRIMARY KEY,"
                    + "dateTime LONG,"
                    + "code TEXT,"
                    + "name TEXT,"
                    + "price REAL,"
                    + "amount INT,"
                    + "type REAL,"
                    + "tag INT,"
                    + "hold INT)");
            db.execSQL("create table if not exists position("
                    + "id TEXT PRIMARY KEY,"
                    + "code TEXT,"
                    + "name TEXT,"
                    + "cost REAL,"
                    + "type INT,"
                    + "amount REAL,"
                    + "exchange TEXT,"
                    + "profit REAL)");
            db.execSQL("create table if not exists wisdom("
                    + "id TEXT PRIMARY KEY,"
                    + "content TEXT,"
                    + "rowNo TEXT)");
            db.execSQL("create table if not exists buddha("
                    + "id TEXT PRIMARY KEY,"
                    + "startTime LONG,"
                    + "duration LONG,"
                    + "count INT,"
                    + "type INT,"
                    + "summary TEXT)");
            db.execSQL("create table if not exists buddhaConfig("
                    + "id TEXT PRIMARY KEY,"
                    + "name TEXT,"
                    + "size LONG,"
                    + "md5 TEXT,"
                    + "pitch REAL,"
                    + "speed REAL,"
                    + "type INT,"
                    + "duration INT)");
            db.execSQL("create table if not exists loan("
                    + "id TEXT PRIMARY KEY,"
                    + "name TEXT,"
                    + "date LONG,"
                    + "sum TEXT,"
                    + "rate TEXT)");
        } catch (SQLException e) {
            Log.e("wangsc", e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 更改数据库版本的操作
        try {
            switch (oldVersion) {
                case 35:
                    db.execSQL("create table if not exists trade("
                            + "id TEXT PRIMARY KEY,"
                            + "dateTime LONG,"
                            + "code TEXT,"
                            + "name TEXT,"
                            + "price REAL,"
                            + "amount INT,"
                            + "type REAL)");
                case 36:
                    db.execSQL("alter table stock add profit REAL ");
                case 37:
                    db.execSQL("create table if not exists position("
                            + "id TEXT PRIMARY KEY,"
                            + "code TEXT,"
                            + "name TEXT,"
                            + "cost REAL,"
                            + "type INT,"
                            + "amount REAL,"
                            + "exchange TEXT,"
                            + "profit REAL)");
                    db.execSQL("insert into position select * from stock");

                case 38:
                    db.execSQL("drop table stock");
                case 39:
                    db.execSQL("create table if not exists wisdom("
                            + "id TEXT PRIMARY KEY,"
                            + "content TEXT,"
                            + "rowNo TEXT)");
                case 42:
                    db.execSQL("drop table buddha");
                    db.execSQL("create table if not exists buddha("
                            + "id TEXT PRIMARY KEY,"
                            + "startTime LONG,"
                            + "duration LONG,"
                            + "count INT,"
                            + "type INT,"
                            + "summary TEXT)");
                    db.execSQL("insert into buddha(id,startTime, duration) select id,start,interval from record");
                    db.execSQL("update buddha set type=1,count=0");
                case 43:
                    db.execSQL("update buddha set summary='计时念佛'");
                case 44:
                    db.execSQL("update buddha set summary='计时念佛'");
                case 45:
                    db.execSQL("create table if not exists buddhaConfig("
                            + "id TEXT PRIMARY KEY,"
                            + "name TEXT,"
                            + "size LONG,"
                            + "md5 TEXT,"
                            + "pitch REAL,"
                            + "speed REAL,"
                            + "type INT,"
                            + "duration INT)");
                case 46:
                    db.execSQL("update buddha set summary='' where summary='计时念佛' or  summary='计数念佛' or summary='耳听念佛' or summary='计时计数念佛'");

                case 47:
                    db.execSQL("create table if not exists buddhaConfig("
                            + "id TEXT PRIMARY KEY,"
                            + "name TEXT,"
                            + "size LONG,"
                            + "md5 TEXT,"
                            + "pitch REAL,"
                            + "speed REAL,"
                            + "type INT,"
                            + "duration INT)");
                    db.execSQL("insert into buddhaConfig select * from buddhaFile");
                    db.execSQL("drop table buddhaFile");

                case 50:
                    db.execSQL("create table if not exists loan("
                            + "id TEXT PRIMARY KEY,"
                            + "name TEXT,"
                            + "date LONG,"
                            + "sum TEXT,"
                            + "rate TEXT)");
                case 51:
                    db.execSQL("alter table trade add hold INT ");
            }
        } catch (SQLException e) {
            Log.e("wangsc", e.getMessage());
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // 每次成功打开数据库后首先被执行
    }


}
