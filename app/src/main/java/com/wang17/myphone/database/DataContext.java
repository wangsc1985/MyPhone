package com.wang17.myphone.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.structure.CardType;
import com.wang17.myphone.structure.RepayType;
import com.wang17.myphone.structure.SmsStatus;
import com.wang17.myphone.structure.SmsType;
import com.wang17.myphone.util._Utils;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * Created by 阿弥陀佛 on 2015/11/18.
 */
public class DataContext {

    private DatabaseHelper dbHelper;
    private Context context;

    public DataContext(Context context) {
        this.context = context;
        dbHelper = new DatabaseHelper(context);
    }


    //region Loan
    public void addLoan(Loan model) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", model.getId().toString());
            values.put("name", model.getName());
            values.put("date", model.getDate().getTimeInMillis());
            values.put("sum", model.getSum().toString());
            values.put("rate", model.getRate().toString());

            //调用方法插入数据
            db.insert("loan", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }


    public List<Loan> getLoanList() {
        List<Loan> result = new ArrayList<Loan>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("loan", null, null,null, null, null, "date asc");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                Loan model = new Loan(UUID.fromString(cursor.getString(0)),
                        cursor.getString(1),
                        new DateTime(cursor.getLong(2)),
                        new BigDecimal(cursor.getString(3)),
                        new BigDecimal(cursor.getString(4)));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }


    public void deleteLoan(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("loan", "id=?", new String[]{id.toString()});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
    //endregion


    //region BuddhaConfig

    /**
     * 增加一条记录
     *
     * @param model 记录对象
     */
    public void addBuddhaConfig(BuddhaConfig model) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", model.getId().toString());
            values.put("name", model.getName());
            values.put("size", model.getSize());
            values.put("md5", model.getMd5());
            values.put("pitch", model.getPitch());
            values.put("speed", model.getSpeed());
            values.put("type", model.getType());
            values.put("duration", model.getCircleSecond());

            //调用方法插入数据
            db.insert("buddhaConfig", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public List<BuddhaConfig> getBuddhaConfigList() {
        List<BuddhaConfig> result = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("buddhaConfig", null, null, null, null, null, null);
            //判断游标是否为空
            if (cursor.moveToNext()) {
                BuddhaConfig model = new BuddhaConfig(UUID.fromString(cursor.getString(0)),
                        cursor.getString(1),
                        cursor.getLong(2),
                        cursor.getString(3),
                        cursor.getFloat(4),
                        cursor.getFloat(5),
                        cursor.getInt(6),
                        cursor.getInt(7));
//                e(model.getName());
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }


    public BuddhaConfig getBuddhaConfig(String name, long size) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("buddhaConfig", null, "name = ? and size = ?", new String[]{name, size + ""}, null, null, null);
            //判断游标是否为空
            if (cursor.moveToNext()) {
                BuddhaConfig model = new BuddhaConfig(UUID.fromString(cursor.getString(0)),
                        cursor.getString(1),
                        cursor.getLong(2),
                        cursor.getString(3),
                        cursor.getFloat(4),
                        cursor.getFloat(5),
                        cursor.getInt(6),
                        cursor.getInt(7));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    public void editBuddhaConfig(BuddhaConfig model) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            //使用update方法更新表中的数据
            ContentValues values = new ContentValues();
            values.put("name", model.getName());
            values.put("size", model.getSize());
            values.put("md5", model.getMd5());
            values.put("pitch", model.getPitch());
            values.put("speed", model.getSpeed());
            values.put("type", model.getType());
            values.put("duration", model.getCircleSecond());
            //调用方法插入数据
            db.update("buddhaConfig", values, "id=?", new String[]{model.getId().toString()});
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deleteBuddhaConfigList(List<BuddhaConfig> list) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            for (int i = 0; i < list.size(); i++) {
                db.delete("buddhaConfig", "id=?", new String[]{list.get(i).getId().toString()});
            }
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
    //endregion


    //region BuddhaRecord

    /**
     * 增加一条记录
     *
     * @param model 记录对象
     */
    public void addBuddha(BuddhaRecord model) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", model.getId().toString());
            values.put("startTime", model.getStartTime().getTimeInMillis());
            values.put("duration", model.getDuration());
            values.put("count", model.getCount());
            values.put("type", model.getType());
            values.put("summary", model.getSummary());

            //调用方法插入数据
            db.insert("buddha", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 增加一条记录
     *
     * @param models 记录对象
     */
    public void addBuddhas(List<BuddhaRecord> models) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            for (BuddhaRecord model : models) {
                //使用insert方法向表中插入数据
                ContentValues values = new ContentValues();
                values.put("id", model.getId().toString());
                values.put("startTime", model.getStartTime().getTimeInMillis());
                values.put("duration", model.getDuration());
                values.put("count", model.getCount());
                values.put("type", model.getType());
                values.put("summary", model.getSummary());
                //调用方法插入数据
                db.insert("buddha", "id", values);
            }
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 得到一条record
     *
     * @param id 记录ID
     * @return
     */
    public BuddhaRecord getBuddha(UUID id) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("buddha", null, "id=?", new String[]{id.toString()}, null, null, null);
            //判断游标是否为空
            if (cursor.moveToNext()) {
                BuddhaRecord model = new BuddhaRecord(id,
                        new DateTime(cursor.getLong(1)),
                        cursor.getLong(2),
                        cursor.getInt(3),
                        cursor.getInt(4),
                        cursor.getString(5));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    public void clearBuddhas() {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            db.delete("buddha", "type=?", new String[]{"11"});
            //判断游标是否为空
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public BuddhaRecord getLatestBuddha() {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("buddha", null, "type!=0", null, null, null, "startTime desc");
            //判断游标是否为空
            if (cursor.moveToNext()) {
                BuddhaRecord model = new BuddhaRecord(UUID.fromString(cursor.getString(0)),
                        new DateTime(cursor.getLong(1)),
                        cursor.getLong(2),
                        cursor.getInt(3),
                        cursor.getInt(4),
                        cursor.getString(5));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    public BuddhaRecord getFirstBuddha() {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("buddha", null, null, null, null, null, "startTime asc");
            //判断游标是否为空
            if (cursor.moveToNext()) {
                BuddhaRecord model = new BuddhaRecord(UUID.fromString(cursor.getString(0)),
                        new DateTime(cursor.getLong(1)),
                        cursor.getLong(2),
                        cursor.getInt(3),
                        cursor.getInt(4),
                        cursor.getString(5));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    /**
     * 获取某一天的record。
     *
     * @param date
     * @return
     */
    public List<BuddhaRecord> getBuddhas(DateTime date) {

        List<BuddhaRecord> result = new ArrayList<BuddhaRecord>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            DateTime startDate = new DateTime(date.getYear(), date.getMonth(), date.getDay());
            DateTime endDate = startDate.addDays(1);
            //查询获得游标
            Cursor cursor = db.query("buddha", null, "startTime>=? and startTime<?", new String[]{startDate.getTimeInMillis() + "", endDate.getTimeInMillis() + ""}, null, null, "startTime asc");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BuddhaRecord model = new BuddhaRecord(UUID.fromString(cursor.getString(0)),
                        new DateTime(cursor.getLong(1)),
                        cursor.getLong(2),
                        cursor.getInt(3),
                        cursor.getInt(4),
                        cursor.getString(5));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public List<BuddhaRecord> getBuddhaListStartDate(DateTime startDate) {

        List<BuddhaRecord> result = new ArrayList<BuddhaRecord>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            startDate = startDate.getDate();
            //查询获得游标
            Cursor cursor = db.query("buddha", null, "startTime>=?", new String[]{startDate.getTimeInMillis() + ""}, null, null, "startTime asc");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BuddhaRecord model = new BuddhaRecord(UUID.fromString(cursor.getString(0)),
                        new DateTime(cursor.getLong(1)),
                        cursor.getLong(2),
                        cursor.getInt(3),
                        cursor.getInt(4),
                        cursor.getString(5));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    /**
     * 获取某一时刻的record。
     *
     * @param timeInmillis
     * @return
     */
    public BuddhaRecord getBuddha(long timeInmillis) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("buddha", null, "startTime=?", new String[]{timeInmillis + ""}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BuddhaRecord model = new BuddhaRecord(UUID.fromString(cursor.getString(0)),
                        new DateTime(cursor.getLong(1)),
                        cursor.getLong(2),
                        cursor.getInt(3),
                        cursor.getInt(4),
                        cursor.getString(5));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    public List<BuddhaRecord> getAllBuddhas() {
        List<BuddhaRecord> result = new ArrayList<BuddhaRecord>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("buddha", null, null, null, null, null, "startTime asc");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BuddhaRecord model = new BuddhaRecord(UUID.fromString(cursor.getString(0)),
                        new DateTime(cursor.getLong(1)),
                        cursor.getLong(2),
                        cursor.getInt(3),
                        cursor.getInt(4),
                        cursor.getString(5));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    /**
     * 得到所有record
     *
     * @return
     */
    public List<BuddhaRecord> getBuddhas(int year) {
        List<BuddhaRecord> result = new ArrayList<BuddhaRecord>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            DateTime startDate = new DateTime(year, 0, 1);
            DateTime endDate = new DateTime(year + 1, 0, 1);
            //查询获得游标
            Cursor cursor = db.query("buddha", null, "startTime>=? AND startTime<?", new String[]{startDate.getTimeInMillis() + "", endDate.getTimeInMillis() + ""}, null, null, "startTime asc");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BuddhaRecord model = new BuddhaRecord(UUID.fromString(cursor.getString(0)),
                        new DateTime(cursor.getLong(1)),
                        cursor.getLong(2),
                        cursor.getInt(3),
                        cursor.getInt(4),
                        cursor.getString(5));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public List<BuddhaRecord> getBuddhas(String type) {
        List<BuddhaRecord> result = new ArrayList<BuddhaRecord>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("buddha", null, "type=?", new String[]{type}, null, null, "startTime asc");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BuddhaRecord model = new BuddhaRecord(UUID.fromString(cursor.getString(0)),
                        new DateTime(cursor.getLong(1)),
                        cursor.getLong(2),
                        cursor.getInt(3),
                        cursor.getInt(4),
                        cursor.getString(5));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public List<BuddhaRecord> getBuddhas(int year, int month) {
        List<BuddhaRecord> result = new ArrayList<BuddhaRecord>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            DateTime startDate = new DateTime(year, month, 1);
            DateTime endDate = new DateTime(year, month, 1);
            endDate = endDate.addMonths(1);
            //查询获得游标
            Cursor cursor = db.query("buddha", null, "startTime>=? AND startTime<?", new String[]{startDate.getTimeInMillis() + "", endDate.getTimeInMillis() + ""}, null, null, "startTime asc");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BuddhaRecord model = new BuddhaRecord(UUID.fromString(cursor.getString(0)),
                        new DateTime(cursor.getLong(1)),
                        cursor.getLong(2),
                        cursor.getInt(3),
                        cursor.getInt(4),
                        cursor.getString(5));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public void editBuddha(BuddhaRecord model) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            //使用update方法更新表中的数据
            ContentValues values = new ContentValues();
            values.put("startTime", model.getStartTime().getTimeInMillis());
            values.put("duration", model.getDuration());
            values.put("count", model.getCount());
            values.put("type", model.getType());
            values.put("summary", model.getSummary());

            db.update("buddha", values, "id=?", new String[]{model.getId().toString()});
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 删除指定的record
     *
     * @param id
     */
    public void deleteBuddha(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("buddha", "id=?", new String[]{id.toString()});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deleteBuddhaList(List<BuddhaRecord> buddhaList) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            for (int i = 0; i < buddhaList.size(); i++) {
                db.delete("buddha", "id=?", new String[]{buddhaList.get(i).getId().toString()});
            }
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
    //endregion

    //region Stock


    public void addPosition(Position position) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", position.getId().toString());
            values.put("code", position.getCode());
            values.put("name", position.getName());
            values.put("cost", position.getCost().toString());
            values.put("type", position.getType());
            values.put("amount", position.getAmount());
            values.put("exchange", position.getExchange());
            values.put("profit", position.getProfit().toString());
            //调用方法插入数据
            db.insert("position", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void editPosition(Position position) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("code", position.getCode());
            values.put("name", position.getName());
            values.put("cost", position.getCost().toString());
            values.put("type", position.getType());
            values.put("amount", position.getAmount());
            values.put("exchange", position.getExchange());
            values.put("profit", position.getProfit().toString());

            if (db.update("position", values, "id=?", new String[]{position.getId().toString()}) == 0) {
                this.addPosition(position);
            }

            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public Position getPosition(String code) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("position", null, "code = ?", new String[]{code}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                Position model = new Position();
                model.setId(UUID.fromString(cursor.getString(0)));
                model.setCode(cursor.getString(1));
                model.setName(cursor.getString(2));
                model.setCost(new BigDecimal(cursor.getString(3)));
                model.setType(cursor.getInt(4));
                model.setAmount(cursor.getInt(5));
                model.setExchange(cursor.getString(6));
                model.setProfit(new BigDecimal(cursor.getString(7)));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    public List<Position> getAllPositions() {
        List<Position> result = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("position", null, "type=0", null, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                Position model = new Position();
                model.setId(UUID.fromString(cursor.getString(0)));
                model.setCode(cursor.getString(1));
                model.setName(cursor.getString(2));
                model.setCost(new BigDecimal(cursor.getString(3)));
                model.setType(cursor.getInt(4));
                model.setAmount(cursor.getInt(5));
                model.setExchange(cursor.getString(6));
                model.setProfit(new BigDecimal(cursor.getString(7)));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public List<Position> getPositions() {
        List<Position> result = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("position", null, "amount != 0", null, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                Position model = new Position();
                model.setId(UUID.fromString(cursor.getString(0)));
                model.setCode(cursor.getString(1));
                model.setName(cursor.getString(2));
                model.setCost(new BigDecimal(cursor.getString(3)));
                model.setType(cursor.getInt(4));
                model.setAmount(cursor.getInt(5));
                model.setExchange(cursor.getString(6));
                model.setProfit(new BigDecimal(cursor.getString(7)));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public List<Position> getPositions(int type) {
        List<Position> result = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = null;
            if (type == 0) {
                cursor = db.query("position", null, "type=0 and amount != 0", null, null, null, null);
            } else {
                cursor = db.query("position", null, "type!=0 and amount != 0", null, null, null, null);
            }
            //判断游标是否为空
            while (cursor.moveToNext()) {
                Position model = new Position();
                model.setId(UUID.fromString(cursor.getString(0)));
                model.setCode(cursor.getString(1));
                model.setName(cursor.getString(2));
                model.setCost(new BigDecimal(cursor.getString(3)));
                model.setType(cursor.getInt(4));
                model.setAmount(cursor.getInt(5));
                model.setExchange(cursor.getString(6));
                model.setProfit(new BigDecimal(cursor.getString(7)));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public void deletePosition(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("position", "id=?", new String[]{id.toString()});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    //endregion

    //region BankToDo


    public void addBankToDo(BankToDo bankToDo) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", bankToDo.getId().toString());
            values.put("dateTime", bankToDo.getDateTime().getTimeInMillis());
            values.put("bankName", bankToDo.getBankName());
            values.put("cardNumber", bankToDo.getCardNumber());
            values.put("money", bankToDo.getMoney());
            //调用方法插入数据
            db.insert("bankToDo", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }


    public void editBankToDo(BankToDo bankToDo) {

        e("修改banktodo");
        //获取数据库对象
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        //使用update方法更新表中的数据
        ContentValues values = new ContentValues();
        values.put("dateTime", bankToDo.getDateTime().getTimeInMillis());
        values.put("bankName", bankToDo.getBankName());
        values.put("cardNumber", bankToDo.getCardNumber());
        values.put("money", bankToDo.getMoney());


        if (db.update("bankToDo", values, "id=?", new String[]{bankToDo.getId().toString()}) == 0) {
            this.addBankToDo(bankToDo);
        }
        db.close();
    }

    public BankToDo getBankToDo(String bankName, String cardNumber) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("bankToDo", null, "bankName like ? AND cardNumber like ?", new String[]{bankName, cardNumber}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BankToDo model = new BankToDo(new DateTime(cursor.getLong(1)), cursor.getString(2), cursor.getString(3), cursor.getDouble(4));
                model.setId(UUID.fromString(cursor.getString(0)));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    public List<BankToDo> getBankToDos() {
        List<BankToDo> result = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("bankToDo", null, null, null, null, null, "dateTime ASC");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BankToDo model = new BankToDo(new DateTime(cursor.getLong(1)), cursor.getString(2), cursor.getString(3), cursor.getDouble(4));
                model.setId(UUID.fromString(cursor.getString(0)));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public void deleteBankToDo(String bankName, String cardNumber) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            if (cardNumber == null)
                db.delete("bankToDo", "bankName like ? ", new String[]{bankName});
            else
                db.delete("bankToDo", "bankName like ? AND cardNumber like ?", new String[]{bankName, cardNumber});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deleteBankToDo(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("bankToDo", "id=?", new String[]{id.toString()});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deleteAllBankToDo() {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("bankToDo", null, null);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    //endregion

    //region Trade


    public void addTrade(Trade model) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", model.getId().toString());
            values.put("dateTime", model.getDateTime().getTimeInMillis());
            values.put("code", model.getCode());
            values.put("name", model.getName());
            values.put("price", model.getPrice().toString());
            values.put("amount", model.getAmount());
            values.put("type", model.getType());
            values.put("tag", model.getTag());
            values.put("hold", model.getHold());
            //调用方法插入数据
            db.insert("trade", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }


    public Trade getTrade(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("trade", null, "id = ?", new String[]{id.toString()}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                Trade model = new Trade();
                model.setId(UUID.fromString(cursor.getString(0)));
                model.setDateTime(new DateTime(cursor.getLong(1)));
                model.setCode(cursor.getString(2));
                model.setName(cursor.getString(3));
                model.setPrice(new BigDecimal(cursor.getString(4)));
                model.setAmount(cursor.getInt(5));
                model.setType(cursor.getInt(6));
                model.setTag(cursor.getInt(7));
                model.setHold(cursor.getInt(8));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    public List<Trade> getTrades(String code) {
        List<Trade> result = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("trade", null, "code = ?", new String[]{code}, null, null, "dateTime DESC");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                Trade model = new Trade();
                model.setId(UUID.fromString(cursor.getString(0)));
                model.setDateTime(new DateTime(cursor.getLong(1)));
                model.setCode(cursor.getString(2));
                model.setName(cursor.getString(3));
                model.setPrice(new BigDecimal(cursor.getString(4)));
                model.setAmount(cursor.getInt(5));
                model.setType(cursor.getInt(6));
                model.setTag(cursor.getInt(7));
                model.setHold(cursor.getInt(8));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }


    public List<Trade> getKeepTrades(String code) {
        List<Trade> result = new ArrayList<>();
        DateTime date = null;
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("trade", null, "code = ? and tag = ?", new String[]{code, "1"}, null, null, "dateTime DESC");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                Trade model = new Trade();
                model.setId(UUID.fromString(cursor.getString(0)));
                model.setDateTime(new DateTime(cursor.getLong(1)));
                model.setCode(cursor.getString(2));
                model.setName(cursor.getString(3));
                model.setPrice(new BigDecimal(cursor.getString(4)));
                model.setAmount(cursor.getInt(5));
                model.setType(cursor.getInt(6));
                model.setTag(cursor.getInt(7));
                model.setHold(cursor.getInt(8));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public List<Trade> getLatestTrades(String code) {
        List<Trade> result = new ArrayList<>();
        DateTime date = null;
        int type = -100;
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("trade", null, "code = ?", new String[]{code}, null, null, "dateTime DESC");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                Trade model = new Trade();
                model.setId(UUID.fromString(cursor.getString(0)));
                DateTime dt = new DateTime(cursor.getLong(1));
                int t = cursor.getInt(6);
                if (date != null && date.get(Calendar.DAY_OF_YEAR) != dt.get(Calendar.DAY_OF_YEAR)) {
                    break;
                }
                if (type != -100 && type != t) {
                    break;
                }
                date = dt;
                type = t;
                model.setDateTime(dt);
                model.setCode(cursor.getString(2));
                model.setName(cursor.getString(3));
                model.setPrice(new BigDecimal(cursor.getString(4)));
                model.setAmount(cursor.getInt(5));
                model.setType(t);
                model.setTag(cursor.getInt(7));
                model.setHold(cursor.getInt(8));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public List<Trade> getTrades() {
        List<Trade> result = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("trade", null, null, null, null, null, "dateTime DESC");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                Trade model = new Trade();
                model.setId(UUID.fromString(cursor.getString(0)));
                model.setDateTime(new DateTime(cursor.getLong(1)));
                model.setCode(cursor.getString(2));
                model.setName(cursor.getString(3));
                model.setPrice(new BigDecimal(cursor.getString(4)));
                model.setAmount(cursor.getInt(5));
                model.setType(cursor.getInt(6));
                model.setTag(cursor.getInt(7));
                model.setHold(cursor.getInt(8));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public void editTrade(Trade model) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("dateTime", model.getDateTime().getTimeInMillis());
            values.put("code", model.getCode());
            values.put("name", model.getName());
            values.put("price", model.getPrice().toString());
            values.put("amount", model.getAmount());
            values.put("type", model.getType());
            values.put("tag", model.getTag());
            values.put("hold", model.getHold());
            //调用方法插入数据
            db.update("trade", values, "id=?", new String[]{model.getId().toString()});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deleteTrade(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("trade", "id=?", new String[]{id.toString()});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    //endregion

    //region TallyPlan

    /**
     * 增加一条SexualDay
     *
     * @param model 记录对象
     */
    public void addTallyPlan(TallyPlan model) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", model.getId().toString());
            values.put("interval", model.getInterval());
            values.put("item", model.getItem());
            values.put("summary", model.getSummary());

            //调用方法插入数据
            db.insert("tallyPlan", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 得到所有SexualDay
     *
     * @return
     */
    public List<TallyPlan> getTallyPlans(boolean isTimeAsc) {

        List<TallyPlan> result = new ArrayList<TallyPlan>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("tallyPlan", null, null, null, null, null, isTimeAsc ? "interval ASC" : null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                TallyPlan model = new TallyPlan(UUID.fromString(cursor.getString(0)));
                model.setInterval(cursor.getLong(1));
                model.setItem(cursor.getString(2));
                model.setSummary(cursor.getString(3));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    /**
     * 得到所有SexualDay
     *
     * @return
     */
    public void updateTallyPlan(TallyPlan model) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            //使用update方法更新表中的数据
            ContentValues values = new ContentValues();
            values.put("interval", model.getInterval());
            values.put("item", model.getItem());
            values.put("summary", model.getSummary());

            db.update("tallyPlan", values, "id=?", new String[]{model.getId().toString()});
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 删除指定的record
     *
     * @param id
     */
    public void deleteTallyPlan(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("tallyPlan", "id=?", new String[]{id.toString()});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
    //endregion

    //region Location

    public void addLocation(Location model) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("Id", model.Id.toString());
            values.put("UserId", model.UserId.toString());
            values.put("LocationType", model.LocationType);
            values.put("Longitude", model.Longitude);
            values.put("Latitude", model.Latitude);
            values.put("Accuracy", model.Accuracy);
            values.put("Provider", model.Provider);
            values.put("Speed", model.Speed);
            values.put("Bearing", model.Bearing);
            values.put("Satellites", model.Satellites);
            values.put("Country", model.Country);
            values.put("Province", model.Province);
            values.put("City", model.City);
            values.put("CityCode", model.CityCode);
            values.put("District", model.District);
            values.put("AdCode", model.AdCode);
            values.put("Address", model.Address);
            values.put("PoiName", model.PoiName);
            values.put("Time", model.Time);
            values.put("Summary", model.Summary);
            //调用方法插入数据
            db.insert("location", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public List<Location> getLocatios(UUID UserId, boolean isTimeDesc) {
        List<Location> result = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("location", null, "UserId=?", new String[]{UserId.toString() + ""}, null, null, isTimeDesc ? "time DESC" : "time ASC");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                Location model = new Location();
                model.Id = UUID.fromString(cursor.getString(0));
                model.UserId = UUID.fromString(cursor.getString(1));
                model.LocationType = cursor.getInt(2);
                model.Longitude = cursor.getDouble(3);
                model.Latitude = cursor.getDouble(4);
                model.Accuracy = cursor.getFloat(5);
                model.Provider = cursor.getString(6);
                model.Speed = cursor.getFloat(7);
                model.Bearing = cursor.getFloat(8);
                model.Satellites = cursor.getInt(9);
                model.Country = cursor.getString(10);
                model.Province = cursor.getString(11);
                model.City = cursor.getString(12);
                model.CityCode = cursor.getString(13);
                model.District = cursor.getString(14);
                model.AdCode = cursor.getString(15);
                model.Address = cursor.getString(16);
                model.PoiName = cursor.getString(17);
                model.Time = cursor.getLong(18);
                model.Summary = cursor.getString(19);
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public Location getLatestLocatio(UUID UserId) {
        Location model = new Location();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("location", null, "UserId=?", new String[]{UserId.toString() + ""}, null, null, "time DESC");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                model.Id = UUID.fromString(cursor.getString(0));
                model.UserId = UUID.fromString(cursor.getString(1));
                model.LocationType = cursor.getInt(2);
                model.Longitude = cursor.getDouble(3);
                model.Latitude = cursor.getDouble(4);
                model.Accuracy = cursor.getFloat(5);
                model.Provider = cursor.getString(6);
                model.Speed = cursor.getFloat(7);
                model.Bearing = cursor.getFloat(8);
                model.Satellites = cursor.getInt(9);
                model.Country = cursor.getString(10);
                model.Province = cursor.getString(11);
                model.City = cursor.getString(12);
                model.CityCode = cursor.getString(13);
                model.District = cursor.getString(14);
                model.AdCode = cursor.getString(15);
                model.Address = cursor.getString(16);
                model.PoiName = cursor.getString(17);
                model.Time = cursor.getLong(18);
                model.Summary = cursor.getString(19);
                break;
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return model;
    }

    public List<Location> getLocatiosByYear(UUID UserId, int year, boolean isTimeDesc) {
        List<Location> result = new ArrayList<>();
        try {
            DateTime start = new DateTime(year, 0, 1);
            DateTime end = new DateTime(year + 1, 0, 1);
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("location", null, "UserId=? AND Time>? AND Time<?", new String[]{UserId.toString() + "", start.getTimeInMillis() + "", end.getTimeInMillis() + ""}, null, null, isTimeDesc ? "time DESC" : null);
            while (cursor.moveToNext()) {
                Location model = new Location();
                model.Id = UUID.fromString(cursor.getString(0));
                model.UserId = UUID.fromString(cursor.getString(1));
                model.LocationType = cursor.getInt(2);
                model.Longitude = cursor.getDouble(3);
                model.Latitude = cursor.getDouble(4);
                model.Accuracy = cursor.getFloat(5);
                model.Provider = cursor.getString(6);
                model.Speed = cursor.getFloat(7);
                model.Bearing = cursor.getFloat(8);
                model.Satellites = cursor.getInt(9);
                model.Country = cursor.getString(10);
                model.Province = cursor.getString(11);
                model.City = cursor.getString(12);
                model.CityCode = cursor.getString(13);
                model.District = cursor.getString(14);
                model.AdCode = cursor.getString(15);
                model.Address = cursor.getString(16);
                model.PoiName = cursor.getString(17);
                model.Time = cursor.getLong(18);
                model.Summary = cursor.getString(19);
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public List<Location> getLocatiosByDayspan(UUID UserId, int daySpan, boolean isTimeDesc) {
        List<Location> result = new ArrayList<>();
        try {
            long now = System.currentTimeMillis();
            long pre = new DateTime().addDays(-daySpan).getTimeInMillis();
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("location", null, "UserId=? AND Time>? AND Time<?", new String[]{UserId.toString() + "", pre + "", now + ""}, null, null, isTimeDesc ? "time DESC" : null);
            while (cursor.moveToNext()) {
                Location model = new Location();
                model.Id = UUID.fromString(cursor.getString(0));
                model.UserId = UUID.fromString(cursor.getString(1));
                model.LocationType = cursor.getInt(2);
                model.Longitude = cursor.getDouble(3);
                model.Latitude = cursor.getDouble(4);
                model.Accuracy = cursor.getFloat(5);
                model.Provider = cursor.getString(6);
                model.Speed = cursor.getFloat(7);
                model.Bearing = cursor.getFloat(8);
                model.Satellites = cursor.getInt(9);
                model.Country = cursor.getString(10);
                model.Province = cursor.getString(11);
                model.City = cursor.getString(12);
                model.CityCode = cursor.getString(13);
                model.District = cursor.getString(14);
                model.AdCode = cursor.getString(15);
                model.Address = cursor.getString(16);
                model.PoiName = cursor.getString(17);
                model.Time = cursor.getLong(18);
                model.Summary = cursor.getString(19);
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public List<Location> getTodayLocatios(UUID UserId) {
        List<Location> result = new ArrayList<>();
        DateTime today = new DateTime();
        DateTime start = new DateTime(today.getYear(), today.getMonth(), today.getDay());
        DateTime end = start.addDays(1);
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("location", null, "UserId=? AND Time>? AND Time<?", new String[]{UserId.toString(), start.getTimeInMillis() + "", end.getTimeInMillis() + ""}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                Location model = new Location();
                model.Id = UUID.fromString(cursor.getString(0));
                model.UserId = UUID.fromString(cursor.getString(1));
                model.LocationType = cursor.getInt(2);
                model.Longitude = cursor.getDouble(3);
                model.Latitude = cursor.getDouble(4);
                model.Accuracy = cursor.getFloat(5);
                model.Provider = cursor.getString(6);
                model.Speed = cursor.getFloat(7);
                model.Bearing = cursor.getFloat(8);
                model.Satellites = cursor.getInt(9);
                model.Country = cursor.getString(10);
                model.Province = cursor.getString(11);
                model.City = cursor.getString(12);
                model.CityCode = cursor.getString(13);
                model.District = cursor.getString(14);
                model.AdCode = cursor.getString(15);
                model.Address = cursor.getString(16);
                model.PoiName = cursor.getString(17);
                model.Time = cursor.getLong(18);
                model.Summary = cursor.getString(19);
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public void clearLocations(int accuracy) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("location", "accuracy>?", new String[]{accuracy + ""});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deleteLocation(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("location", "id=?", new String[]{id + ""});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    //endregion

    //region Song

    public void addSong(Song song) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", song.getId().toString());
            values.put("playListId", song.getPlayListId().toString());
            values.put("fileName", song.getFileName());
            values.put("title", song.getTitle());
            values.put("duration", song.getDuration());
            values.put("singer", song.getSinger());
            values.put("album", song.getAlbum());
            values.put("year", song.getYear());
            values.put("type", song.getType());
            values.put("size", song.getSize());
            values.put("fileUrl", song.getFileUrl());
            //调用方法插入数据
            db.insert("song", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void addSongs(List<Song> songList) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            for (Song song : songList) {
                //使用insert方法向表中插入数据
                ContentValues values = new ContentValues();
                values.put("id", song.getId().toString());
                values.put("playListId", song.getPlayListId().toString());
                values.put("fileName", song.getFileName());
                values.put("title", song.getTitle());
                values.put("duration", song.getDuration());
                values.put("singer", song.getSinger());
                values.put("album", song.getAlbum());
                values.put("year", song.getYear());
                values.put("type", song.getType());
                values.put("size", song.getSize());
                values.put("fileUrl", song.getFileUrl());
                //调用方法插入数据
                db.insert("song", "id", values);
            }

            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }


    public Song getSong(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("song", null, "id=?", new String[]{id.toString() + ""}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                Song model = new Song(UUID.fromString(cursor.getString(0)));
                model.setPlayListId(UUID.fromString(cursor.getString(1)));
                model.setFileName(cursor.getString(2));
                model.setTitle(cursor.getString(3));
                model.setDuration(cursor.getInt(4));
                model.setSinger(cursor.getString(5));
                model.setAlbum(cursor.getString(6));
                model.setYear(cursor.getString(7));
                model.setType(cursor.getString(8));
                model.setSize(cursor.getString(9));
                model.setFileUrl(cursor.getString(10));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    public List<Song> getSongs(UUID playListId) {
        List<Song> result = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("song", null, "playListId=?", new String[]{playListId.toString() + ""}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                Song model = new Song(UUID.fromString(cursor.getString(0)));
                model.setPlayListId(UUID.fromString(cursor.getString(1)));
                model.setFileName(cursor.getString(2));
                model.setTitle(cursor.getString(3));
                model.setDuration(cursor.getInt(4));
                model.setSinger(cursor.getString(5));
                model.setAlbum(cursor.getString(6));
                model.setYear(cursor.getString(7));
                model.setType(cursor.getString(8));
                model.setSize(cursor.getString(9));
                model.setFileUrl(cursor.getString(10));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public void deleteSongs(UUID playListId) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("song", "playListId=?", new String[]{playListId.toString() + ""});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
    //endregion

    //region PlayList
    public void addPlayList(PlayList playList) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", playList.getId().toString());
            values.put("name", playList.getName());
            values.put("currentSongId", playList.getCurrentSongId().toString());
            values.put("currentSongPosition", playList.getCurrentSongPosition());
            values.put("isDefault", playList.isDefault());
            //调用方法插入数据
            db.insert("playList", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public PlayList getPlayList(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("playList", null, "id=?", new String[]{id.toString() + ""}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                PlayList model = new PlayList(UUID.fromString(cursor.getString(0)));
                model.setName(cursor.getString(1));
                model.setCurrentSongId(UUID.fromString(cursor.getString(2)));
                model.setCurrentSongPosition((cursor.getInt(3)));
                model.setDefault(Boolean.parseBoolean(cursor.getString(4)));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    public List<PlayList> getPlayLists() {
        List<PlayList> result = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("playList", null, null, null, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                PlayList model = new PlayList(UUID.fromString(cursor.getString(0)));
                model.setName(cursor.getString(1));
                model.setCurrentSongId(UUID.fromString(cursor.getString(2)));
                model.setCurrentSongPosition((cursor.getInt(3)));
                model.setDefault(Boolean.parseBoolean(cursor.getString(4)));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public void updatePlayList(PlayList playList) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            //使用update方法更新表中的数据
            ContentValues values = new ContentValues();
            values.put("id", playList.getId().toString());
            values.put("name", playList.getName());
            values.put("currentSongId", playList.getCurrentSongId().toString());
            values.put("currentSongPosition", playList.getCurrentSongPosition());
            values.put("isDefault", playList.isDefault());

            db.update("playList", values, "id=?", new String[]{playList.getId().toString()});
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
    //endregion

    //region BillStatement

    public void addBillStatement(BillStatement billStatement) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", billStatement.getId().toString());
            values.put("cardNumber", billStatement.getCardNumber());
            values.put("startDate", billStatement.getStartDate().getTimeInMillis());
            values.put("endDate", billStatement.getEndDate().getTimeInMillis());
            values.put("repayDate", billStatement.getRepayDate().getTimeInMillis());
            values.put("bill", billStatement.getBill());
            values.put("billRemain", billStatement.getBillRemain());

            //调用方法插入数据
            db.insert("billStatement", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void updateBillStatement(BillStatement billStatement) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            //使用update方法更新表中的数据
            ContentValues values = new ContentValues();
            values.put("cardNumber", billStatement.getCardNumber());
            values.put("startDate", billStatement.getStartDate().getTimeInMillis());
            values.put("endDate", billStatement.getEndDate().getTimeInMillis());
            values.put("repayDate", billStatement.getRepayDate().getTimeInMillis());
            values.put("bill", billStatement.getBill());
            values.put("billRemain", billStatement.getBillRemain());

            db.update("billStatement", values, "id=?", new String[]{billStatement.getId().toString()});
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public BillStatement getBillStatement(DateTime startDate) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("billStatement", null, "startDate=?", new String[]{startDate.getTimeInMillis() + ""}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BillStatement model = new BillStatement(UUID.fromString(cursor.getString(0)));
                model.setCardNumber(cursor.getString(1));
                model.setStartDate(new DateTime(cursor.getLong(2)));
                model.setEndDate(new DateTime(cursor.getLong(3)));
                model.setRepayDate(new DateTime(cursor.getLong(4)));
                model.setBill(cursor.getDouble(5));
                model.setBillRemain(cursor.getDouble(6));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    public void deleteBillStatement() {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("billStatement", null, null);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
    //endregion

    //region sms

    /**
     * 增加一条记录
     *
     * @param phoneMessage 记录对象
     */
    public void addPhoneMessage(PhoneMessage phoneMessage) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", phoneMessage.getId().toString());
            values.put("address", phoneMessage.getAddress());
            values.put("body", phoneMessage.getBody());
            values.put("type", phoneMessage.getType().toInt());
            values.put("status", phoneMessage.getStatus().toInt());
            values.put("createTime", phoneMessage.getCreateTime().getTimeInMillis());

            //调用方法插入数据
            db.insert("sms", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void editPhoneMessage(PhoneMessage phoneMessage) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            //使用update方法更新表中的数据
            ContentValues values = new ContentValues();
//            values.put("id",phoneMessage.getId().toString());
            values.put("address", phoneMessage.getAddress());
            values.put("body", phoneMessage.getBody());
            values.put("type", phoneMessage.getType().toInt());
            values.put("status", phoneMessage.getStatus().toInt());
            values.put("createTime", phoneMessage.getCreateTime().getTimeInMillis());
            //调用方法插入数据
            db.update("sms", values, "id=?", new String[]{phoneMessage.getId().toString()});
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public List<PhoneMessage> getPhoneMessages(String number) {
        //获取数据库对象
        SQLiteDatabase db = null;
        //查询获得游标
        Cursor cursor = null;

        try {
            List<PhoneMessage> result = new ArrayList<>();
            //获取数据库对象
            db = dbHelper.getReadableDatabase();
            //查询获得游标
            cursor = db.query("sms", null, "address=?", new String[]{number + ""}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                PhoneMessage model = new PhoneMessage();
                model.setId(UUID.fromString(cursor.getString(0)));
                model.setAddress(cursor.getString(1));
                model.setBody(cursor.getString(2));
                model.setType(SmsType.fromInt(cursor.getInt(3)));
                model.setStatus(SmsStatus.fromInt(cursor.getInt(4)));
                model.setCreateTime(new DateTime(cursor.getLong(5)));
                result.add(model);
            }
            return result;
        } catch (Exception e) {
            _Utils.printException(context, e);
        } finally {
            if (cursor != null)
                cursor.close();
            if (db != null)
                db.close();
        }
        return null;
    }
    public PhoneMessage getLastPhoneMessages(String number) {
        //获取数据库对象
        SQLiteDatabase db = null;
        //查询获得游标
        Cursor cursor = null;

        try {
            //获取数据库对象
            db = dbHelper.getReadableDatabase();
            //查询获得游标
            cursor = db.query("sms", null, "address=?", new String[]{number + ""}, null, null, "createTime desc");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                PhoneMessage model = new PhoneMessage();
                model.setId(UUID.fromString(cursor.getString(0)));
                model.setAddress(cursor.getString(1));
                model.setBody(cursor.getString(2));
                model.setType(SmsType.fromInt(cursor.getInt(3)));
                model.setStatus(SmsStatus.fromInt(cursor.getInt(4)));
                model.setCreateTime(new DateTime(cursor.getLong(5)));
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        } finally {
            if (cursor != null)
                cursor.close();
            if (db != null)
                db.close();
        }
        return null;
    }

    public List<PhoneMessage> getPhoneMessages() {
        //获取数据库对象
        SQLiteDatabase db = null;
        //查询获得游标
        Cursor cursor = null;

        try {
            List<PhoneMessage> result = new ArrayList<>();
            //获取数据库对象
            db = dbHelper.getReadableDatabase();
            //查询获得游标
            cursor = db.query("sms", null, null, null, null, null, "createTime desc");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                PhoneMessage model = new PhoneMessage();
                model.setId(UUID.fromString(cursor.getString(0)));
                model.setAddress(cursor.getString(1));
                model.setBody(cursor.getString(2));
                model.setType(SmsType.fromInt(cursor.getInt(3)));
                model.setStatus(SmsStatus.fromInt(cursor.getInt(4)));
                model.setCreateTime(new DateTime(cursor.getLong(5)));
                result.add(model);
            }
            return result;
        } catch (Exception e) {
            _Utils.printException(context, e);
        } finally {
            if (cursor != null)
                cursor.close();
            if (db != null)
                db.close();
        }
        return null;
    }

    private void e(Object log) {
        Log.e("wangsc", log.toString());
    }


    public void deletePhoneMessage() {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("sms", null, null);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deletePhoneMessage(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("sms", "id=?", new String[]{id.toString()});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deletePhoneMessage(List<PhoneMessage> list) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            for (int i = 0; i < list.size(); i++) {
                db.delete("sms", "id=?", new String[]{list.get(i).getId().toString()});
            }
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deletePhoneMessageFrom(long timeInMillis) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("sms", "createTime < ?", new String[]{timeInMillis + ""});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
    //endregion

    //region DayItem
    public void addDayItem(DayItem dayItem) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", dayItem.getId().toString());
            values.put("name", dayItem.getName().toString());
            values.put("summary", dayItem.getSummary());
            values.put("targetInHour", dayItem.getTargetInHour());

            //调用方法插入数据
            db.insert("dayItem", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 得到所有SexualDay
     *
     * @return
     */
    public DayItem getDayItem(UUID id) {

        List<MarkDay> result = new ArrayList<MarkDay>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("dayItem", null, "id = ?", new String[]{id.toString()}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                DayItem model = new DayItem(UUID.fromString(cursor.getString(0)));
                model.setName(cursor.getString(1));
                model.setSummary(cursor.getString(2));
                model.setTargetInHour(cursor.getInt(3));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    public List<DayItem> getDayItems(boolean... isNameASC) {
        List<DayItem> result = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("dayItem", null, null, null, null, null, null);
            if (isNameASC.length == 1) {
                cursor = db.query("dayItem", null, null, null, null, null, isNameASC[0] ? "name ASC" : null);
            }
            //判断游标是否为空
            while (cursor.moveToNext()) {
                DayItem model = new DayItem(UUID.fromString(cursor.getString(0)));
                model.setName(cursor.getString(1));
                model.setSummary(cursor.getString(2));
                model.setTargetInHour(cursor.getInt(3));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }


    /**
     * 得到所有SexualDay
     *
     * @return
     */
    public void editDayItem(DayItem dayItem) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            //使用update方法更新表中的数据
            ContentValues values = new ContentValues();
            values.put("name", dayItem.getName().toString());
            values.put("summary", dayItem.getSummary());
            values.put("targetInHour", dayItem.getTargetInHour());

            db.update("dayItem", values, "id=?", new String[]{dayItem.getId().toString()});
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 删除指定的record
     *
     * @param id
     */
    public void deleteDayItem(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("dayItem", "id=?", new String[]{id.toString()});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
    //endregion

    //region MarkDay


    /**
     * 增加一条SexualDay
     *
     * @param markDay 记录对象
     */
    public void addMarkDay(MarkDay markDay) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", markDay.getId().toString());
            values.put("dateTime", markDay.getDateTime().getTimeInMillis());
            values.put("item", markDay.getItem().toString());
            values.put("summary", markDay.getSummary());

            //调用方法插入数据
            db.insert("markDay", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 得到所有SexualDay
     *
     * @return
     */
    public MarkDay getLastMarkDay(UUID itemId) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("markDay", null, "item like ?", new String[]{itemId.toString()}, null, null, "DateTime  DESC");
            //判断游标是否为空

            if (cursor.moveToNext()) {
                MarkDay model = new MarkDay(UUID.fromString(cursor.getString(0)));
                model.setDateTime(new DateTime(cursor.getLong(1)));
                model.setItem(UUID.fromString(cursor.getString(2)));
                model.setSummary(cursor.getString(3));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    /**
     * 得到所有SexualDay
     *
     * @return
     */
    public List<MarkDay> getMarkDays(UUID item, boolean isTimeDesc) {

        List<MarkDay> result = new ArrayList<MarkDay>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("markDay", null, "item like ?", new String[]{item.toString()}, null, null, isTimeDesc ? "DateTime DESC" : null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                MarkDay model = new MarkDay(UUID.fromString(cursor.getString(0)));
                model.setDateTime(new DateTime(cursor.getLong(1)));
                model.setItem(UUID.fromString(cursor.getString(2)));
                model.setSummary(cursor.getString(3));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    /**
     * 得到所有SexualDay
     *
     * @return
     */
    public void editMarkDay(MarkDay markDay) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            //使用update方法更新表中的数据
            ContentValues values = new ContentValues();
            values.put("dateTime", markDay.getDateTime().getTimeInMillis());
            values.put("item", markDay.getItem().toString());
            values.put("summary", markDay.getSummary());

            db.update("markDay", values, "id=?", new String[]{markDay.getId().toString()});
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }


    /**
     * 得到所有SexualDay
     *
     * @return
     */
    public void hideMarkDay(UUID item, DateTime dateTime) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.execSQL("update markDay set summary='hide' where item='" + item + "' and dateTime<=" + dateTime.getTimeInMillis());
            Log.e("wangsc", "update markDay set summary='hide' where item='" + item + "' and dateTime<=" + dateTime.getTimeInMillis());
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 删除指定的record
     *
     * @param itemId
     */
    public void deleteMarkDayBy(UUID itemId) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("markDay", "item=?", new String[]{itemId.toString()});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 删除指定的record
     *
     * @param id
     */
    public void deleteMarkDay(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("markDay", "id=?", new String[]{id.toString()});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
    //endregion

    //region CreditCard

    /**
     * 增加一条记录
     *
     * @param creditCard 记录对象
     */
    public void addCreditCard(CreditCard creditCard) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", creditCard.getId().toString());
            values.put("cardName", creditCard.getCardName());
            values.put("cardNumber", creditCard.getCardNumber());
            values.put("bankName", creditCard.getBankName());
            values.put("billDay", creditCard.getBillDay());
            values.put("repayDay", creditCard.getRepayDay());
            values.put("annualFee", creditCard.getAnnualFee());
            values.put("creditLine", creditCard.getCreditLine());
            values.put("cardType", creditCard.getCardType().toString());
            values.put("balance", creditCard.getBalance());
            values.put("isBlackList", creditCard.isBlackList() + "");
            values.put("isVisible", creditCard.isVisible() + "");
            values.put("repayType", creditCard.getRepayType().toString() + "");

            //调用方法插入数据
            db.insert("creditCard", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public List<CreditCard> getCreditCard() {
        List<CreditCard> creditCardList = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("creditCard", null, null, null, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                CreditCard model = new CreditCard(UUID.fromString(cursor.getString(0)));
                model.setCardName(cursor.getString(1));
                model.setCardNumber(cursor.getString(2));
                model.setBankName(cursor.getString(3));
                model.setBillDay(cursor.getInt(4));
                model.setRepayDay(cursor.getInt(5));
                model.setAnnualFee(cursor.getDouble(6));
                model.setCreditLine(cursor.getDouble(7));
                model.setCardType(CardType.valueOf(cursor.getString(8)));
                model.setBalance(cursor.getDouble(9));
                model.setBlackList(Boolean.parseBoolean(cursor.getString(10)));
                model.setVisible(Boolean.parseBoolean(cursor.getString(11)));
                model.setRepayType(RepayType.valueOf(cursor.getString(12)));
                creditCardList.add(model);
            }
            cursor.close();
            db.close();
        } catch (IllegalArgumentException e) {
            _Utils.printException(context, e);
        }
        return creditCardList;
    }

    public CreditCard getCreditCard(String cardNumber) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("creditCard", null, "cardNumber like ?", new String[]{"%" + cardNumber}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                CreditCard model = new CreditCard(UUID.fromString(cursor.getString(0)));
                model.setCardName(cursor.getString(1));
                model.setCardNumber(cursor.getString(2));
                model.setBankName(cursor.getString(3));
                model.setBillDay(cursor.getInt(4));
                model.setRepayDay(cursor.getInt(5));
                model.setAnnualFee(cursor.getDouble(6));
                model.setCreditLine(cursor.getDouble(7));
                model.setCardType(CardType.valueOf(cursor.getString(8)));
                model.setBalance(cursor.getDouble(9));
                model.setBlackList(Boolean.parseBoolean(cursor.getString(10)));
                model.setVisible(Boolean.parseBoolean(cursor.getString(11)));
                model.setRepayType(RepayType.valueOf(cursor.getString(12)));
                cursor.close();
                db.close();
                return model;
            }
        } catch (IllegalArgumentException e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    public void updateCreditCard(CreditCard creditCard) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            //使用update方法更新表中的数据
            ContentValues values = new ContentValues();
            values.put("id", creditCard.getId().toString());
            values.put("cardName", creditCard.getCardName());
            values.put("cardNumber", creditCard.getCardNumber());
            values.put("bankName", creditCard.getBankName());
            values.put("billDay", creditCard.getBillDay());
            values.put("repayDay", creditCard.getRepayDay());
            values.put("annualFee", creditCard.getAnnualFee());
            values.put("creditLine", creditCard.getCreditLine());
            values.put("cardType", creditCard.getCardType().toString());
            values.put("balance", creditCard.getBalance());
            values.put("isBlackList", creditCard.isBlackList() + "");
            values.put("isVisible", creditCard.isVisible() + "");
            values.put("repayType", creditCard.getRepayType().toString() + "");

            db.update("creditCard", values, "id=?", new String[]{creditCard.getId().toString()});
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 删除指定的record
     *
     * @param cardNumber
     */
    public void deleteCreditRecord(String cardNumber) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("creditCard", "cardNumber=?", new String[]{cardNumber});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deleteCreditRecord() {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("creditCard", null, null);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
    //endregion

    //region BillRecord

    /**
     * 增加一条记录
     *
     * @param billRecord 记录对象
     */
    public void addBillRecord(BillRecord billRecord) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", billRecord.getId().toString());
            values.put("dateTime", billRecord.getDateTime().getTimeInMillis());
            values.put("cardNumber", billRecord.getCardNumber());
            values.put("country", billRecord.getCountry());
            values.put("currency", billRecord.getCurrency());
            values.put("location", billRecord.getLocation());
            values.put("money", billRecord.getMoney());
            values.put("summary", billRecord.getSummary());
            values.put("balance", billRecord.getBalance());
            values.put("smsId", billRecord.getSmsId());

            //调用方法插入数据
            db.insert("billRecord", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public BillRecord getBillRecord(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("billRecord", null, "id=?", new String[]{id.toString()}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BillRecord model = new BillRecord(UUID.fromString(cursor.getString(0)));
                model.setDateTime(new DateTime(cursor.getLong(1)));
                model.setCardNumber(cursor.getString(2));
                model.setCountry(cursor.getString(3));
                model.setCurrency(cursor.getString(4));
                model.setLocation(cursor.getString(5));
                model.setMoney(cursor.getDouble(6));
                model.setSummary(cursor.getString(7));
                model.setBalance(cursor.getDouble(8));
                model.setSmsId(cursor.getInt(9));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    public List<BillRecord> getBillRecords(String cardNumber) {
        List<BillRecord> result = new ArrayList<BillRecord>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("billRecord", null, "cardNumber=?",
                    new String[]{cardNumber}, null, null, "dateTime asc");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BillRecord model = new BillRecord(UUID.fromString(cursor.getString(0)));
                model.setDateTime(new DateTime(cursor.getLong(1)));
                model.setCardNumber(cursor.getString(2));
                model.setCountry(cursor.getString(3));
                model.setCurrency(cursor.getString(4));
                model.setLocation(cursor.getString(5));
                model.setMoney(cursor.getDouble(6));
                model.setSummary(cursor.getString(7));
                model.setBalance(cursor.getDouble(8));
                model.setSmsId(cursor.getInt(9));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public BillRecord getLastBillRecord(String cardNumber) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("billRecord", null, "cardNumber=?",
                    new String[]{cardNumber}, null, null, "dateTime desc");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BillRecord model = new BillRecord(UUID.fromString(cursor.getString(0)));
                model.setDateTime(new DateTime(cursor.getLong(1)));
                model.setCardNumber(cursor.getString(2));
                model.setCountry(cursor.getString(3));
                model.setCurrency(cursor.getString(4));
                model.setLocation(cursor.getString(5));
                model.setMoney(cursor.getDouble(6));
                model.setSummary(cursor.getString(7));
                model.setBalance(cursor.getDouble(8));
                model.setSmsId(cursor.getInt(9));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    public List<BillRecord> getBillRecords(String cardNumber, DateTime startDateTime, DateTime endDateTime) {
        List<BillRecord> result = new ArrayList<BillRecord>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("billRecord", null, "cardNumber=? and dateTime>=? and dateTime<?",
                    new String[]{cardNumber, startDateTime.getTimeInMillis() + "", endDateTime.getTimeInMillis() + ""}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BillRecord model = new BillRecord(UUID.fromString(cursor.getString(0)));
                model.setDateTime(new DateTime(cursor.getLong(1)));
                model.setCardNumber(cursor.getString(2));
                model.setCountry(cursor.getString(3));
                model.setCurrency(cursor.getString(4));
                model.setLocation(cursor.getString(5));
                model.setMoney(cursor.getDouble(6));
                model.setSummary(cursor.getString(7));
                model.setBalance(cursor.getDouble(8));
                model.setSmsId(cursor.getInt(9));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    /**
     * 微粒贷专用方法，isDebt=true，未还清借款，isDebt=false，已还清的借款。
     *
     * @param cardNumber
     * @param isDebt
     * @return
     */
    public List<BillRecord> getBillRecords(String cardNumber, boolean isDebt) {
        List<BillRecord> result = new ArrayList<BillRecord>();
        try {
            // 获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            // 查询获得游标
            /**
             * 借款记录
             *      money > 0
             *
             *      已还清：balance = 0
             *      未还清：balance = money
             *
             * 还款记录
             *      money < 0
             *
             *      balance = 0
             */

            Cursor cursor = null;
            if (isDebt) {
                cursor = db.query("billRecord", null, "cardNumber=? and money>0 and balance!=0",
                        new String[]{cardNumber}, null, null, null);
            } else {
                cursor = db.query("billRecord", null, "cardNumber=? and money>0 and balance==0",
                        new String[]{cardNumber}, null, null, null);
            }
            //判断游标是否为空
            while (cursor.moveToNext()) {
                BillRecord model = new BillRecord(UUID.fromString(cursor.getString(0)));
                model.setDateTime(new DateTime(cursor.getLong(1)));
                model.setCardNumber(cursor.getString(2));
                model.setCountry(cursor.getString(3));
                model.setCurrency(cursor.getString(4));
                model.setLocation(cursor.getString(5));
                model.setMoney(cursor.getDouble(6));
                model.setSummary(cursor.getString(7));
                model.setBalance(cursor.getDouble(8));
                model.setSmsId(cursor.getInt(9));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    /**
     * 删除指定的record
     *
     * @param cardNumber
     */
    public void deleteBillRecord(String cardNumber) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("billRecord", "cardNumber=?", new String[]{cardNumber});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deleteBillRecord() {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("billRecord", null, null);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deleteBillRecord(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("billRecord", "id=?", new String[]{id.toString()});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void updateBillRecord(BillRecord billRecord) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            //使用update方法更新表中的数据
            ContentValues values = new ContentValues();
            values.put("id", billRecord.getId().toString());
            values.put("dateTime", billRecord.getDateTime().getTimeInMillis());
            values.put("cardNumber", billRecord.getCardNumber());
            values.put("country", billRecord.getCountry());
            values.put("currency", billRecord.getCurrency());
            values.put("location", billRecord.getLocation());
            values.put("money", billRecord.getMoney());
            values.put("summary", billRecord.getSummary());
            values.put("balance", billRecord.getBalance());
            values.put("smsId", billRecord.getSmsId());

            db.update("billRecord", values, "id=?", new String[]{billRecord.getId().toString()});
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    //endregion

    //region TallyRecord

    /**
     * 增加一条记录
     *
     * @param tallyRecord 记录对象
     */
    public void addRecord(TallyRecord tallyRecord) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", tallyRecord.getId().toString());
            values.put("start", tallyRecord.getStart().getTimeInMillis());
            values.put("interval", tallyRecord.getInterval());
            values.put("item", tallyRecord.getItem());
            values.put("summary", tallyRecord.getSummary());

            //调用方法插入数据
            db.insert("record", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 得到一条record
     *
     * @param id 记录ID
     * @return
     */
    public TallyRecord getRecord(UUID id) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("record", null, "id=?", new String[]{id.toString()}, null, null, null);
            //判断游标是否为空
            if (cursor.moveToNext()) {
                TallyRecord model = new TallyRecord(id);
                model.setStart(new DateTime(cursor.getLong(1)));
                model.setInterval(cursor.getInt(2));
                model.setItem(cursor.getString(3));
                model.setSummary(cursor.getString(4));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    /**
     * 获取某一天的record。
     *
     * @param date
     * @return
     */
    public List<TallyRecord> getRecords(DateTime date) {

        List<TallyRecord> result = new ArrayList<TallyRecord>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            DateTime startDate = new DateTime(date.getYear(), date.getMonth(), date.getDay());
            DateTime endDate = (DateTime) (startDate.clone());
            endDate = endDate.addDays(1);
            //查询获得游标
            Cursor cursor = db.query("record", null, "start>=? and start<?", new String[]{startDate.getTimeInMillis() + "", endDate.getTimeInMillis() + ""}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                TallyRecord model = new TallyRecord(UUID.fromString(cursor.getString(0)));
                model.setStart(new DateTime(cursor.getLong(1)));
                model.setInterval(cursor.getInt(2));
                model.setItem(cursor.getString(3));
                model.setSummary(cursor.getString(4));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    /**
     * 获取某一时刻的record。
     *
     * @param timeInmillis
     * @return
     */
    public TallyRecord getRecord(long timeInmillis) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("record", null, "start=?", new String[]{timeInmillis + ""}, null, null, null);
            //判断游标是否为空
            while (cursor.moveToNext()) {
                TallyRecord model = new TallyRecord(UUID.fromString(cursor.getString(0)));
                model.setStart(new DateTime(cursor.getLong(1)));
                model.setInterval(cursor.getInt(2));
                model.setItem(cursor.getString(3));
                model.setSummary(cursor.getString(4));
                cursor.close();
                db.close();
                return model;
            }
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return null;
    }

    /**
     * 得到所有record
     *
     * @return
     */
    public List<TallyRecord> getRecords(int year) {

        List<TallyRecord> result = new ArrayList<TallyRecord>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            DateTime startDate = new DateTime(year, 0, 1);
            DateTime endDate = new DateTime(year + 1, 0, 1);
            //查询获得游标
            Cursor cursor = db.query("record", null, "start>=? AND start<?", new String[]{startDate.getTimeInMillis() + "", endDate.getTimeInMillis() + ""}, null, null, "start desc");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                TallyRecord model = new TallyRecord(UUID.fromString(cursor.getString(0)));
                model.setStart(new DateTime(cursor.getLong(1)));
                model.setInterval(cursor.getInt(2));
                model.setItem(cursor.getString(3));
                model.setSummary(cursor.getString(4));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public void editRecord(TallyRecord tallyRecord) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            //使用update方法更新表中的数据
            ContentValues values = new ContentValues();
            values.put("start", tallyRecord.getStart().getTimeInMillis());
            values.put("interval", tallyRecord.getInterval());
            values.put("item", tallyRecord.getItem());
            values.put("summary", tallyRecord.getSummary());

            db.update("record", values, "id=?", new String[]{tallyRecord.getId().toString()});
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    /**
     * 删除指定的record
     *
     * @param id
     */
    public void deleteRecord(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("record", "id=?", new String[]{id.toString()});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
    //endregion

    //region RunLog
    public List<RunLog> getRunLogs() {
        List<RunLog> result = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("runLog", null, null, null, null, null, "runTime DESC");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                RunLog model = new RunLog(UUID.fromString(cursor.getString(0)));
                model.setRunTime(new DateTime(cursor.getLong(1)));
                model.setTag(cursor.getString(2));
                model.setItem(cursor.getString(3));
                model.setMessage(cursor.getString(4));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    @NotNull
    public List<RunLog> getRunLogsByTag(String tag) {
        List<RunLog> result = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("runLog", null, "tag like ?", new String[]{tag}, null, null, "runTime DESC");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                RunLog model = new RunLog(UUID.fromString(cursor.getString(0)));
                model.setRunTime(new DateTime(cursor.getLong(1)));
                model.setTag(cursor.getString(2));
                model.setItem(cursor.getString(3));
                model.setMessage(cursor.getString(4));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public List<RunLog> getRunLogsByEquals(String item) {
        List<RunLog> result = new ArrayList<>();
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("runLog", null, "item like ?", new String[]{item}, null, null, "runTime DESC");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                RunLog model = new RunLog(UUID.fromString(cursor.getString(0)));
                model.setRunTime(new DateTime(cursor.getLong(1)));
                model.setTag(cursor.getString(2));
                model.setItem(cursor.getString(3));
                model.setMessage(cursor.getString(4));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public List<RunLog> getRunLogsByLike(String[] itemLike) {
        List<RunLog> result = new ArrayList<>();
        String where = "";
        for (int i = 0; i < itemLike.length; i++) {
            where += " item like  ? ";
            if (i < itemLike.length - 1) {
                where += "OR";
            }
        }
        String[] whereArg = new String[itemLike.length];
        for (int i = 0; i < itemLike.length; i++) {
            whereArg[i] = "%" + itemLike[i] + "%";
        }

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //查询获得游标
            Cursor cursor = db.query("runLog", null, where, whereArg, null, null, "runTime DESC");
            //判断游标是否为空
            while (cursor.moveToNext()) {
                RunLog model = new RunLog(UUID.fromString(cursor.getString(0)));
                model.setRunTime(new DateTime(cursor.getLong(1)));
                model.setTag(cursor.getString(2));
                model.setItem(cursor.getString(3));
                model.setMessage(cursor.getString(4));
                result.add(model);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
        return result;
    }

    public void addRunLog(String tag, String item, String message) {
        RunLog runLog = new RunLog(UUID.randomUUID());
        runLog.setTag(tag);
        runLog.setItem(item);
        runLog.setMessage(message);
        runLog.setRunTime(new DateTime());
        addRunLog(runLog);
    }

    public void addRunLog(RunLog runLog) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", runLog.getId().toString());
            values.put("runTime", runLog.getRunTime().getTimeInMillis());
            values.put("tag", runLog.getTag());
            values.put("item", runLog.getItem());
            values.put("message", runLog.getMessage());
            //调用方法插入数据
            db.insert("runLog", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void addRunLog(String item, String message) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            //使用insert方法向表中插入数据
            ContentValues values = new ContentValues();
            values.put("id", UUID.randomUUID().toString());
            values.put("runTime", System.currentTimeMillis());
            values.put("tag", "default");
            values.put("item", item);
            values.put("message", message);
            //调用方法插入数据
            db.insert("runLog", "id", values);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void updateRunLog(RunLog runLog) {

        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            //使用update方法更新表中的数据
            ContentValues values = new ContentValues();
            values.put("runTime", runLog.getRunTime().getTimeInMillis());
            values.put("tag", runLog.getTag());
            values.put("item", runLog.getItem());
            values.put("message", runLog.getMessage());

            db.update("runLog", values, "id=?", new String[]{runLog.getId().toString()});
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void clearRunLog() {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("runLog", null, null);
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deleteRunLogByTag(String tag) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("runLog", "tag like ?", new String[]{tag});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deleteRunLogByEquals(String item) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("runLog", "item like ?", new String[]{"%" + item + "%"});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deleteRunLogByLike(String itemLike) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("runLog", "item like ?", new String[]{"%" + itemLike + "%"});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }

    public void deleteRunLog(UUID id) {
        try {
            //获取数据库对象
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("runLog", "id = ?", new String[]{id.toString()});
            //关闭SQLiteDatabase对象
            db.close();
        } catch (Exception e) {
            _Utils.printException(context, e);
        }
    }
    //endregion

    //region Setting
    public Setting getSetting(Object name) {

        //获取数据库对象
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        //查询获得游标
        Cursor cursor = db.query("setting", null, "name=?", new String[]{name.toString()}, null, null, null);
        //判断游标是否为空
        while (cursor.moveToNext()) {
            Setting setting = new Setting(name.toString(), cursor.getString(1), cursor.getInt(2));
            cursor.close();
            db.close();
            return setting;
        }
        return null;
    }

    public Setting getSetting(Object name, Object defaultValue) {
        Setting setting = getSetting(name);
        if (setting == null) {
            this.addSetting(name, defaultValue);
            setting = new Setting(name.toString(), defaultValue.toString(), 100);
            return setting;
        }
        return setting;
    }

    /**
     * 修改制定key配置，如果不存在则创建。
     *
     * @param name
     * @param value
     */
    public void editSetting(Object name, Object value) {
        //获取数据库对象
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        //使用update方法更新表中的数据
        ContentValues values = new ContentValues();
        values.put("value", value.toString());
        if (db.update("setting", values, "name=?", new String[]{name.toString()}) == 0) {
            this.addSetting(name, value.toString());
        }
        db.close();
    }

    public void editSettingLevel(Object name, int level) {
        //获取数据库对象
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        //使用update方法更新表中的数据
        ContentValues values = new ContentValues();
        values.put("level", level + "");
        db.update("setting", values, "name=?", new String[]{name.toString()});
        db.close();
    }

    public void deleteSetting(Object name) {
        //获取数据库对象
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("setting", "name=?", new String[]{name.toString()});
//        String sql = "DELETE FROM setting WHERE userId="+userId.toString()+" AND name="+name;
//        addLog(new Log(sql,userId),db);
        //关闭SQLiteDatabase对象
        db.close();
    }

    public void deleteSetting(String name) {
        //获取数据库对象
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("setting", "name=?", new String[]{name});
//        String sql = "DELETE FROM setting WHERE userId="+userId.toString()+" AND name="+name;
//        addLog(new Log(sql,userId),db);
        //关闭SQLiteDatabase对象
        db.close();
    }

    public void addSetting(Object name, Object value) {
        //获取数据库对象
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        //使用insert方法向表中插入数据
        ContentValues values = new ContentValues();
        values.put("name", name.toString());
        values.put("value", value.toString());
        //调用方法插入数据
        db.insert("setting", "name", values);
        //关闭SQLiteDatabase对象
        db.close();
    }

    public List<Setting> getSettings() {
        List<Setting> result = new ArrayList<>();
        //获取数据库对象
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        //查询获得游标
        Cursor cursor = db.query("setting", null, null, null, null, null, "level,name");
        //判断游标是否为空
        while (cursor.moveToNext()) {
            Setting setting = new Setting(cursor.getString(0), cursor.getString(1), cursor.getInt(2));
            result.add(setting);
        }
        cursor.close();
        db.close();
        return result;
    }

    public void clearSetting() {
        //获取数据库对象
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("setting", null, null);
        //        String sql = "DELETE FROM setting WHERE userId="+userId.toString()+" AND key="+key;
//        addLog(new Log(sql,userId),db);
        //关闭SQLiteDatabase对象
        db.close();
    }

    //endregion
}
