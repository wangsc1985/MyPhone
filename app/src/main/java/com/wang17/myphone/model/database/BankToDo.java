package com.wang17.myphone.model.database;

import com.wang17.myphone.model.DateTime;

import java.util.UUID;

public class BankToDo{

    private UUID id;
    private DateTime dateTime;
    private String bankName;
    private String cardNumber;
    private double money;

    public BankToDo(DateTime dateTime, String bankName,String cardNumber, double money) {
        this.id = UUID.randomUUID();
        this.dateTime = dateTime;
        this.bankName = bankName;
        this.cardNumber = cardNumber;
        this.money = money;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
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

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }
}
