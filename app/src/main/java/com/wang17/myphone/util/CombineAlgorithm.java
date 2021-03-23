package com.wang17.myphone.util;

/**
 * 文件名：CombineAlgorithm.java
 *
 * 版本信息：
 * 日期：2014-6-12
 * Copyright Corporation 2014
 * 版权所有
 *
 */


import com.wang17.myphone.database.BillRecord;
import com.wang17.myphone.model.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description:组合算法 从M个数中取出N个数，无顺序
 * 二维Object数组
 * @author :royoan
 * @since :2014-6-12 下午10:22:22
 * @version :0.0.1
 */
public class CombineAlgorithm {
    /* 原M个数据数组 */
    private List<BillRecord> src;

    /* 需要获取N个数 */
    private int nnnnnnn;

    private double money;
    private DateTime repayDate;
    private DataContext dataContext;

    public CombineAlgorithm(List<BillRecord> src, int getNum,double money,DateTime repayDate,DataContext dataContext) throws Exception {
        if (src == null)
            throw new Exception("原数组为空.");
        if (src.size() < getNum)
            throw new Exception("要取的数据比原数组个数还 大 .");
        this.src = src;
        nnnnnnn = getNum;
        this.money = money;
        this.repayDate = repayDate;
        this.dataContext = dataContext;
    }

    public boolean getResult(){
        BillRecord[] resultNode = new BillRecord[nnnnnnn];
        return combine(src, 0, 0, nnnnnnn, resultNode);
    }

    /**
     * <p>
     * 计算 C(m,nnnnnnn)个数 = (m!)/(nnnnnnn!*(m-nnnnnnn)!)
     * </p>
     * 从M个数中选N个数，函数返回有多少种选法 参数 m 必须大于等于 nnnnnnn m = 0; nnnnnnn = 0; retuan 1;
     *
     * @param m
     * @param n
     * @return
     * @since royoan 2014-6-13 下午8:25:33
     */
    public int combination(int m, int n) {
        if (m < n)
            return 0; // 如果总数小于取出的数，直接返回0

        int k = 1;
        int j = 1;
        // 该种算法约掉了分母的(m-nnnnnnn)!,这样分子相乘的个数就是有n个了
        for (int i = n; i >= 1; i--) {
            k = k * m;
            j = j * n;
            m--;
            n--;
        }
        return k / j;
    }

    /**
     * <p> 递归算法，把结果写到obj二维数组对象 </p>
     * @param src
     * @param srcIndex
     * @param i
     * @param n
     * @param tmp
     * @since royoan 2014-6-15 上午11:22:24
     */
    private boolean combine(List<BillRecord> src, int srcIndex, int i, int n, BillRecord[] tmp) {
        int j;
        for (j = srcIndex; j < src.size() - (n - 1); j++ ) {
            tmp[i] = src.get(j);
            if (n == 1) {
                double mmm = 0;
                double feee = 0;
                for(int index=0;index<tmp.length;index++){
                    BillRecord record = tmp[index];
                    double mm = record.getMoney();
                    mmm+=mm;
                    DateTime loanDate = new DateTime(record.getDateTime().getYear(), record.getDateTime().getMonth(), record.getDateTime().getDay());
                    long day = (repayDate.getTimeInMillis() - loanDate.getTimeInMillis()) / 1000 / 3600 / 24;
                    double fee = mm * 0.0005 * day;
                    feee+=fee;
                }
                if (mmm + feee + money == 0) {
                    for(int index=0;index<tmp.length;index++){
                        BillRecord record = tmp[index];
                        record.setBalance(0);
                        record.setSummary("已还清");
                        dataContext.updateBillRecord(record);
                    }
                    return true;
                }
            } else {
                n--;
                i++;
                combine(src, j+1, i, n, tmp);
                n++;
                i--;
            }
        }
        return false;
    }

    /**
     * 用法实例
     * @param args
     * @throws Exception
     * @since royoan 2014-6-15 下午8:21:05
     */
    public static void main(String[] args) throws Exception {
        List<BillRecord> a = new ArrayList<>();
//        CombineAlgorithm ca = new CombineAlgorithm(a, 3);
    }
}