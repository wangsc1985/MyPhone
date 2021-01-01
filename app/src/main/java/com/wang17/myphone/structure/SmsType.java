package com.wang17.myphone.structure;

/**
 * Created by 阿弥陀佛 on 2016/10/24.
 */

//    * type => 类型 1是接收到的，2是已发出
public enum SmsType {
    新信息(0),接收到(1), 已发出(2);

    private int value;

    SmsType(int value) {
        this.value = value;
    }

    public static SmsType fromInt(int value) {
        switch (value) {
            case 0:
                return 新信息;
            case 1:
                return 接收到;
            case 2:
                return 已发出;
            default:
                return null;
        }
    }

    public int toInt() {
        return this.value;
    }
}