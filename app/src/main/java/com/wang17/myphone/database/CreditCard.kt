package com.wang17.myphone.database;

import com.wang17.myphone.structure.CardType;
import com.wang17.myphone.structure.RepayType;

import java.util.UUID;

/**
 //annual 'ai niu
 // adj.每年的；年度的；一年生的
 // n.年刊；一年生植物
 * Created by 阿弥陀佛 on 2016/10/20.
 */

public class CreditCard {
    //  2016/10/20 卡片类型、信用额度、未出账单金额、本期应还款、账单日、还款日、年费、隐藏卡片（管理此卡，但不在首页显示）、设为黑名单（不管理此卡任何信息）
    private UUID id;
    private String bankName;
    private String cardName;
    private String cardNumber;
    private CardType cardType; // 卡片类型
    private double creditLine; // 信用额度
    private int billDay; // 账单日
    private int repayDay; // 还款日
    private RepayType repayType; // 每月固定日或者相对账单日。
    private double annualFee; // 年费
    private boolean isVisible; // 是否显示
    private boolean blackList; // 黑名单
    private double balance;// 余额/剩余额度

    public CreditCard(UUID id) {
        this.id = id;
    }

    public CreditCard(String bankName, String cardName, String cardNumber, CardType cardType, double creditLine, int billDay, int repayDay, RepayType repayType, int annualFee, boolean isVisible, boolean blackList, double balance) {
        this.id = UUID.randomUUID();
        this.bankName = bankName;
        this.cardName = cardName;
        this.cardNumber = cardNumber;
        this.cardType = cardType;
        this.creditLine = creditLine;
        this.billDay = billDay;
        this.repayDay = repayDay;
        this.repayType = repayType;
        this.annualFee = annualFee;
        this.isVisible = isVisible;
        this.blackList = blackList;
        this.balance = balance;
    }

    public UUID getId() {
        return id;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public CardType getCardType() {
        return cardType;
    }

    public void setCardType(CardType cardType) {
        this.cardType = cardType;
    }

    public double getCreditLine() {
        return creditLine;
    }

    public void setCreditLine(double creditLine) {
        this.creditLine = creditLine;
    }

    public int getBillDay() {
        return billDay;
    }

    public void setBillDay(int billDay) {
        this.billDay = billDay;
    }

    public int getRepayDay() {
        return repayDay;
    }

    public void setRepayDay(int repayDay) {
        this.repayDay = repayDay;
    }

    public RepayType getRepayType() {
        return repayType;
    }

    public void setRepayType(RepayType repayType) {
        this.repayType = repayType;
    }

    public double getAnnualFee() {
        return annualFee;
    }

    public void setAnnualFee(double annualFee) {
        this.annualFee = annualFee;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public boolean isBlackList() {
        return blackList;
    }

    public void setBlackList(boolean blackList) {
        this.blackList = blackList;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
