package com.wang17.myphone.event;

/**
 * Created by 阿弥陀佛 on 2016/10/13.
 */

public class TotalEvent {
    private int totalResult;
    public TotalEvent(int totalResult){
        this.totalResult = totalResult;
    }

    public int getTotalResult() {
        return totalResult;
    }
}
