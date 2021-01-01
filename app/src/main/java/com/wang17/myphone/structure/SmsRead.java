package com.wang17.myphone.structure;

/**
 * Created by 阿弥陀佛 on 2016/10/24.
 */
//    * read => 是否阅读 0未读， 1已读
public enum SmsRead {
    未读(0), 已读(1);

    private int value;

    SmsRead(int value) {
        this.value = value;
    }

    public static SmsRead fromInt(int value) {
        switch (value) {
            case 0:
                return 未读;
            case 1:
                return 已读;
            default:
                return null;
        }
    }

    public int toInt() {
        return this.value;
    }
}
