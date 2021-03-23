package com.wang17.myphone.util;

import android.content.Context;

import com.wang17.myphone.database.BillRecord;
import com.wang17.myphone.database.BillStatement;
import com.wang17.myphone.database.CreditCard;
import com.wang17.myphone.model.CreditCardResult;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.structure.RepayType;

import java.util.Calendar;
import java.util.List;

/**
 * Created by 阿弥陀佛 on 2016/10/27.
 */

public class CreditCardHelper {

    public static CreditCardResult getCreditCardResult(Context context, CreditCard card) {
        CreditCardResult result = new CreditCardResult();
        try {
            DataContext dataContext = new DataContext(context);
            DateTime today = DateTime.getToday();
            DateTime xxxRepayDate = null;
            if (today.getDay() <= card.getBillDay()) {
                // 例如：账单日8号，当天10月3号。 已出账：s1=08.09 d1=09.09  未出账：s2=09.09 d2=10.09  >=s  <d
                result.billDateCurrent = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());
                result.endBillDateNext = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());
                result.endBillDateNext.add(Calendar.DAY_OF_MONTH, 1);//10.09
                today.add(Calendar.MONTH, -1); // 09.03
                result.startBillDateNext = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); // 09.08
                result.startBillDateNext.add(Calendar.DAY_OF_MONTH, 1);
                result.endBillDateCurrent = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());// 09.08
                result.endBillDateCurrent.add(Calendar.DAY_OF_MONTH, 1);// 09.09
                today.add(Calendar.MONTH, -1);// 08.03
                result.startBillDateCurrent = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());// 08.08
                result.startBillDateCurrent.add(Calendar.DAY_OF_MONTH, 1);// 08.09

            } else {
                // 例如：账单日8号，当天9月23号。已出账：s1=08.09 d1=09.09  未出账：s2=09.09 d2=10.09  >=s  <d
                result.startBillDateNext = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); // 09.08
                result.startBillDateNext.add(Calendar.DAY_OF_MONTH, 1);
                result.endBillDateCurrent = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); // 09.08
                result.endBillDateCurrent.add(Calendar.DAY_OF_MONTH, 1); // 09.09
                today.add(Calendar.MONTH, 1); // 10.23
                result.billDateCurrent = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());
                result.endBillDateNext = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());
                result.endBillDateNext.add(Calendar.DAY_OF_MONTH, 1); // 10.09
                today.add(Calendar.MONTH, -2); // 08.23
                result.startBillDateCurrent = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); //08.08
                result.startBillDateCurrent.add(Calendar.DAY_OF_MONTH, 1);
            }
            //
            result.repayDateNext = result.endBillDateCurrent.addDays(0); //09.09
            if (card.getRepayType() == RepayType.相对账单日) {
                result.repayDateNext = result.endBillDateCurrent.addDays(card.getRepayDay()); //10.02
            } else {
                if (card.getRepayDay() < card.getBillDay())
                    result.repayDateNext.add(Calendar.MONTH, 1); // 10.09
                result.repayDateNext.set(Calendar.DAY_OF_MONTH, card.getRepayDay()); // 10.01
                result.repayDateNext.add(Calendar.DAY_OF_MONTH,1); //10.02
            }
            //
            String start = result.startBillDateCurrent.toLongDateTimeString();
            String end   = result.endBillDateCurrent.toLongDateTimeString();
            start = start;
            end = end;
            result.billRecordListCurrent = dataContext.getBillRecords(card.getCardNumber(), result.startBillDateCurrent, result.endBillDateCurrent);
            for (BillRecord billRecord : result.billRecordListCurrent) {
                double money = billRecord.getMoney();
                if (money < 0)
                    result.consumeTotalCurrent -= money;
            }
            // 核对账单
//        BillStatement billStatement = CreditCardHelper.getCurrentBillStatement(context, card);
//        if (billStatement != null && result.consumeTotalCurrent != billStatement.getBill()) {
//            Toast.makeText(context, "账单已更正", Toast.LENGTH_SHORT).show();
//            result.consumeTotalCurrent = billStatement.getBill();
//        }
            String start2 = result.startBillDateNext.toLongDateTimeString();
            String end2   = result.endBillDateNext.toLongDateTimeString();
            start2 = start2;
            end2 = end2;
            result.billRecordListFuture = dataContext.getBillRecords(card.getCardNumber(), result.startBillDateNext, result.endBillDateNext);
            for (BillRecord billRecord : result.billRecordListFuture) {
                double money = billRecord.getMoney();
                if (money > 0 && billRecord.getDateTime().getTimeInMillis() < result.repayDateNext.getTimeInMillis())
                    result.repayTotalNext += money;
                else
                    result.consumeTotalNext -= money;
            }
        } catch (Exception e) {
            _Utils.printException(context,e);
        }
        return result;
    }

    /**
     * 测试还款日的小方法
     *
     * @param context
     * @param card
     */
    public static void getXXXs(Context context, CreditCard card) {
        try {
            DataContext dataContext = new DataContext(context);
            CreditCardResult result = new CreditCardResult();
            DateTime today = new DateTime(2015, 0, 20);
            for (int i = 0; i < 30; i++) {
                if (today.getDay() <= card.getBillDay()) {
                    // 例如：账单日8号，当天10月3号。 已出账：s1=08.09 d1=09.09  未出账：s2=09.09 d2=10.09  >=s  <d
                    result.billDateCurrent = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());
                    today.add(Calendar.MONTH, -1); // 09.03
                    result.endBillDateCurrent = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());// 09.08
                    result.endBillDateCurrent.add(Calendar.DAY_OF_MONTH, 1);// 09.09
                } else {
                    // 例如：账单日8号，当天9月23号。已出账：s1=08.09 d1=09.09  未出账：s2=09.09 d2=10.09  >=s  <d
                    result.endBillDateCurrent = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); // 09.08
                    result.endBillDateCurrent.add(Calendar.DAY_OF_MONTH, 1); // 09.09
                }
                DateTime repayDate = new DateTime(result.endBillDateCurrent.getYear(), result.endBillDateCurrent.getMonth(), result.endBillDateCurrent.getDay());
                repayDate.add(Calendar.DAY_OF_MONTH, card.getRepayDay() - 1);
                today.add(Calendar.MONTH, 1);
            }
        } catch (Exception e) {
            _Utils.printException(context,e);
        }

    }

    public static BillStatement getCurrentBillStatement(Context context, CreditCard card) {
        DateTime startDate = null, endDateTime = null;
        DataContext dataContext = null;
        try {
            DateTime today = DateTime.getToday();
            dataContext = new DataContext(context);
            if (today.getDay() <= card.getBillDay()) {
                // 例如：账单日8号，当天10月3号。 已出账：s1=08.09 d1=09.09  未出账：s2=09.09 d2=10.09  >=s  <d
                today.add(Calendar.MONTH, -1); // 09.03
                endDateTime = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());
                endDateTime.add(Calendar.DAY_OF_MONTH, 1);// 09.09
                today.add(Calendar.MONTH, -1);// 08.03
                startDate = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());// 08.08
                startDate.add(Calendar.DAY_OF_MONTH, 1);

            } else {
                // 例如：账单日8号，当天9月23号。已出账：s1=08.09 d1=09.09  未出账：s2=09.09 d2=10.09  >=s  <d
                endDateTime = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); // 09.08
                endDateTime.add(Calendar.DAY_OF_MONTH, 1); // 09.09
                today.add(Calendar.MONTH, 1); // 10.23
                today.add(Calendar.MONTH, -2); // 08.23
                startDate = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); //08.08
                startDate.add(Calendar.DAY_OF_MONTH, 1);
            }
        } catch (Exception e) {
            _Utils.printException(context,e);
        }
        return dataContext.getBillStatement(startDate);
    }

    public static double getCurrentBill(Context context, CreditCard card) {
        double money = 0;
        try {
            DateTime today = DateTime.getToday();
            DataContext dataContext = new DataContext(context);
            DateTime startDateTime = null, endDateTime = null;
            if (today.getDay() <= card.getBillDay()) {
                // 例如：账单日8号，当天10月3号。 已出账：s1=08.09 d1=09.09  未出账：s2=09.09 d2=10.09  >=s  <d
                today.add(Calendar.MONTH, -1); // 09.03
                endDateTime = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());
                endDateTime.add(Calendar.DAY_OF_MONTH, 1);// 09.09
                today.add(Calendar.MONTH, -1);// 08.03
                startDateTime = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());// 08.08
                startDateTime.add(Calendar.DAY_OF_MONTH, 1);

            } else {
                // 例如：账单日8号，当天9月23号。已出账：s1=08.09 d1=09.09  未出账：s2=09.09 d2=10.09  >=s  <d
                endDateTime = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); // 09.08
                endDateTime.add(Calendar.DAY_OF_MONTH, 1); // 09.09
                today.add(Calendar.MONTH, 1); // 10.23
                today.add(Calendar.MONTH, -2); // 08.23
                startDateTime = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); //08.08
                startDateTime.add(Calendar.DAY_OF_MONTH, 1);
            }

            List<BillRecord> billRecordList1 = dataContext.getBillRecords(card.getCardNumber(), startDateTime, endDateTime);
            for (BillRecord billRecord : billRecordList1) {
                money += billRecord.getMoney();
            }
        } catch (Exception e) {
            _Utils.printException(context,e);
        }
        return money;
    }

    public static DateTime getCurrentRepayDate(Context context, CreditCard card) {
        DateTime repayDate = null;
        try {
            DateTime today = DateTime.getToday();
            DateTime startDateTime = null, endDateTime = null;
            if (today.getDay() <= card.getBillDay()) {
                // 例如：账单日8号，当天10月3号。 已出账：s1=08.09 d1=09.09
                today.add(Calendar.MONTH, -1); // 09.03
                endDateTime = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());// 09.08
            } else {
                // 例如：账单日8号，当天9月23号。已出账：s1=08.09 d1=09.09
                endDateTime = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); // 09.08
            }

            repayDate = new DateTime(endDateTime.getYear(), endDateTime.getMonth(), endDateTime.getDay());
            if (card.getRepayType() == RepayType.相对账单日) {
                repayDate.add(Calendar.DAY_OF_MONTH, card.getRepayDay());
            } else {
                if (card.getRepayDay() < card.getBillDay()) {
                    repayDate.add(Calendar.MONTH, 1);
                    repayDate.set(Calendar.DAY_OF_MONTH, card.getRepayDay());
                }
                repayDate.set(Calendar.DAY_OF_MONTH, card.getRepayDay());

            }
        } catch (Exception e) {
            _Utils.printException(context,e);
        }
        return repayDate;
    }

    public static double getFutrueBill(Context context, CreditCard card) {
        double money = 0;
        try {
            DateTime today = DateTime.getToday();
            DataContext dataContext = new DataContext(context);
            DateTime startDateTime = null, endDateTime = null;
            if (today.getDay() <= card.getBillDay()) {
                // 例如：账单日8号，当天10月3号。 已出账：s1=08.09 d1=09.09  未出账：s2=09.09 d2=10.09  >=s  <d
                endDateTime = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());
                endDateTime.add(Calendar.DAY_OF_MONTH, 1);//10.09
                today.add(Calendar.MONTH, -1); // 09.03
                startDateTime = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); // 09.08
                startDateTime.add(Calendar.DAY_OF_MONTH, 1);

            } else {
                // 例如：账单日8号，当天9月23号。已出账：s1=08.09 d1=09.09  未出账：s2=09.09 d2=10.09  >=s  <d
                startDateTime = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); // 09.08
                startDateTime.add(Calendar.DAY_OF_MONTH, 1);
                today.add(Calendar.MONTH, 1); // 10.23
                endDateTime = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());
                endDateTime.add(Calendar.DAY_OF_MONTH, 1); // 10.09
            }
            List<BillRecord> billRecordList2 = dataContext.getBillRecords(card.getCardNumber(), startDateTime, endDateTime);
            for (BillRecord billRecord : billRecordList2) {
                money += billRecord.getMoney();
            }
        } catch (Exception e) {
            _Utils.printException(context,e);
        }
        return money;
    }

    public static void get2BillList(Context context, CreditCard card) {
        try {
            DateTime today = DateTime.getToday();
            DataContext dataContext = new DataContext(context);
            DateTime startDateTime1 = null, endDateTime1 = null, startDateTime2 = null, endDateTime2 = null;
            if (today.getDay() <= card.getBillDay()) {
                // 例如：账单日8号，当天10月3号。 已出账：s1=08.09 d1=09.09  未出账：s2=09.09 d2=10.09  >=s  <d
                endDateTime2 = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());
                endDateTime2.add(Calendar.DAY_OF_MONTH, 1);//10.09
                today.add(Calendar.MONTH, -1); // 09.03
                startDateTime2 = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); // 09.08
                startDateTime2.add(Calendar.DAY_OF_MONTH, 1);
                endDateTime1 = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());
                endDateTime1.add(Calendar.DAY_OF_MONTH, 1);// 09.09
                today.add(Calendar.MONTH, -1);// 08.03
                startDateTime1 = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());// 08.08
                startDateTime1.add(Calendar.DAY_OF_MONTH, 1);

            } else {
                // 例如：账单日8号，当天9月23号。已出账：s1=08.09 d1=09.09  未出账：s2=09.09 d2=10.09  >=s  <d
                startDateTime2 = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); // 09.08
                startDateTime2.add(Calendar.DAY_OF_MONTH, 1);
                endDateTime1 = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); // 09.08
                endDateTime1.add(Calendar.DAY_OF_MONTH, 1); // 09.09
                today.add(Calendar.MONTH, 1); // 10.23
                endDateTime2 = new DateTime(today.getYear(), today.getMonth(), card.getBillDay());
                endDateTime2.add(Calendar.DAY_OF_MONTH, 1); // 10.09
                today.add(Calendar.MONTH, -2); // 08.23
                startDateTime1 = new DateTime(today.getYear(), today.getMonth(), card.getBillDay()); //08.08
                startDateTime1.add(Calendar.DAY_OF_MONTH, 1);
            }

            List<BillRecord> billRecordList1 = dataContext.getBillRecords(card.getCardNumber(), startDateTime1, endDateTime1);
            double money1 = 0;
            for (BillRecord billRecord : billRecordList1) {
                money1 += billRecord.getMoney();
            }
            List<BillRecord> billRecordList2 = dataContext.getBillRecords(card.getCardNumber(), startDateTime2, endDateTime2);
            double money2 = 0;
            for (BillRecord billRecord : billRecordList2) {
                money2 += billRecord.getMoney();
            }
        } catch (Exception e) {
            _Utils.printException(context,e);
        }
    }
}
