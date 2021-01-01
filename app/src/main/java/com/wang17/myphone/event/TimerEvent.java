package com.wang17.myphone.event;

/**
 * Created by 阿弥陀佛 on 2016/10/12.
 */

public class TimerEvent {
    private long endTimeMillis;

    public TimerEvent(long endTimeMillis){
        this.endTimeMillis = endTimeMillis;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }
}
