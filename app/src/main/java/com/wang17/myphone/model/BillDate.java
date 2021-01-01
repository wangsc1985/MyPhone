package com.wang17.myphone.model;

/**
 * Created by 阿弥陀佛 on 2016/10/27.
 */

public class BillDate{
    private int month;
    private int day;

    public BillDate(int month, int day) {
        this.month = month;
        this.day = day;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }
}
