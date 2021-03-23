package com.wang17.myphone.model;

import com.wang17.myphone.database.BillRecord;

import java.util.List;

/**
 * Created by 阿弥陀佛 on 2016/10/27.
 */

public class CreditCardResult{
    public DateTime repayDateNext;
    public DateTime startBillDateCurrent;
    public DateTime endBillDateCurrent;
    public DateTime startBillDateNext;
    public DateTime endBillDateNext;
    public DateTime billDateCurrent;
    public double consumeTotalCurrent;
    public double consumeTotalNext;
    public double repayTotalNext;
    public List<BillRecord> billRecordListCurrent;
    public List<BillRecord> billRecordListFuture;
}
