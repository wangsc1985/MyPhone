package com.wang17.myphone.database;

import com.wang17.myphone.model.DateTime;

import java.util.UUID;

/**
 * Created by 阿弥陀佛 on 2016/10/21.
 */
@Deprecated
public class BillRecord {
    private UUID id;
    private int smsId;
    private String cardNumber;
    private DateTime dateTime;
    private double money;
    private String summary;
    private String country;
    private String location;
    private String currency;
    private double balance;

    public BillRecord(UUID id) {
        this.id = id;
    }

    public BillRecord(String cardNumber, DateTime dateTime, double money, double balance, int smsId) {
        this.id = UUID.randomUUID();
        this.smsId = smsId;
        this.cardNumber = cardNumber;
        this.dateTime = dateTime;
        this.money = money;
        this.summary = "消费";
        this.country = "china";
        this.location = "";
        this.currency = "RMB";
        this.balance = balance;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getSmsId() {
        return smsId;
    }

    public void setSmsId(int smsId) {
        this.smsId = smsId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
