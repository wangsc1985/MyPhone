package com.wang17.myphone.structure;

/**
 * Created by 阿弥陀佛 on 2016/10/24.
 */
//    * status => 状态 -1接收，0 complete, 64 pending, 128 failed
public enum SmsStatus {
    接收(-1), 发送完成(0), 等待发送(64), 发送失败(128);

    private int value;

    SmsStatus(int value) {
        this.value = value;
    }

    public static SmsStatus fromInt(int value) {
        switch (value) {
            case -1:
                return 接收;
            case 0:
                return 发送完成;
            case 64:
                return 等待发送;
            case 128:
                return 发送失败;
            default:
                return null;
        }
    }

    public int toInt() {
        return this.value;
    }
}
