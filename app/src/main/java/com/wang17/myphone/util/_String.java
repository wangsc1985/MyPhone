package com.wang17.myphone.util;

import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by 阿弥陀佛 on 2015/6/23.
 */
public class _String {
    public static String concat(Object... strings) {
        StringBuilder sb = new StringBuilder();
        for (Object str : strings) {
            if (str != null)
                sb.append(str.toString());
        }
        return sb.toString();
    }

    public static boolean IsNullOrEmpty(String str) {
        if (str == null || str.length() == 0)
            return true;
        else
            return false;
    }

    /**
     * 根据timeInMillis格式化时间，默认格式"yyyy-MM-dd HH:mm:ss"。
     * @param timeInMillis
     * @param strPattern
     * @return
     */
    public  static String formatUTC(long timeInMillis, String strPattern) {
        SimpleDateFormat sdf = null;
        if (TextUtils.isEmpty(strPattern)) {
            strPattern = "yyyy-MM-dd HH:mm:ss";
        }
        if (sdf == null) {
            try {
                sdf = new SimpleDateFormat(strPattern, Locale.CHINA);
            } catch (Throwable e) {
            }
        } else {
            sdf.applyPattern(strPattern);
        }
        return sdf == null ? "NULL" : sdf.format(timeInMillis);
    }

    /**
     * 时间字段，月、日、时、分、秒，小于10的，设置前缀‘0’。
     *
     * @param x
     * @return
     */
    public static String format(int x) {
        String s = "" + x;
        if (s.length() == 1)
            s = "0" + s;
        return s;
    }

    public static String formatToCardNumber(String number){
        String num = "";
        int index = number.length() - 1;
        for (int i = 0; i < number.length(); i++) {
            index = number.length() - 1 - i;
            if (i % 4 == 0 && i != 0) {
                num = " " + num;
            }
            num = number.charAt(index) + num;
        }
        return num;
    }
}
