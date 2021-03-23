package com.wang17.myphone.database;

import com.wang17.myphone.model.BillDate;
import com.wang17.myphone.model.DateTime;

import java.util.UUID;

/**
 * Created by 阿弥陀佛 on 2016/10/27.
 */
@Deprecated
public class BillStatement{
    private UUID id;
    private String cardNumber;
    private DateTime startDate;
    private DateTime endDate;
    private DateTime repayDate;
    private double bill;
    private double billRemain;

    public BillStatement(UUID id) {
        this.id = id;
    }

    public BillStatement(String cardNumber, BillDate startDate, BillDate endDate, BillDate repayDate, double bill, double billRemain) {
        this.id = UUID.randomUUID();
        DateTime today = DateTime.getToday();
        this.cardNumber = cardNumber;
        if (startDate.getMonth() > today.getMonth())
            this.startDate = new DateTime(today.getYear() - 1, startDate.getMonth()-1, startDate.getDay());
        else
            this.startDate = new DateTime(today.getYear(), startDate.getMonth()-1, startDate.getDay());
        if (endDate.getMonth() > today.getMonth())
            this.endDate = new DateTime(today.getYear() - 1, endDate.getMonth()-1, endDate.getDay());
        else
            this.endDate = new DateTime(today.getYear(), endDate.getMonth()-1, endDate.getDay());
        if (repayDate.getMonth() < today.getMonth())
            this.repayDate = new DateTime(today.getYear() + 1, repayDate.getMonth()-1, repayDate.getDay());
        else
            this.repayDate = new DateTime(today.getYear(), repayDate.getMonth()-1, repayDate.getDay());
        this.bill = bill;
        this.billRemain = billRemain;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public DateTime getRepayDate() {
        return repayDate;
    }

    public void setRepayDate(DateTime repayDate) {
        this.repayDate = repayDate;
    }

    public double getBill() {
        return bill;
    }

    public void setBill(double bill) {
        this.bill = bill;
    }

    public double getBillRemain() {
        return billRemain;
    }

    public void setBillRemain(double billRemain) {
        this.billRemain = billRemain;
    }

    public DateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(DateTime startDate) {
        this.startDate = startDate;
    }

    public DateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(DateTime endDate) {
        this.endDate = endDate;
    }
}
