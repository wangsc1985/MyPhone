package com.wang17.myphone.event;

/**
 * Created by 阿弥陀佛 on 2016/10/16.
 */

public class ReadingStatusEvent {

    private boolean isReading;

    public ReadingStatusEvent(boolean isReading){
        this.isReading = isReading;
    }

    public boolean isReading() {
        return isReading;
    }
}
