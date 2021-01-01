package com.wang17.myphone.event;

import com.wang17.myphone.structure.TimerStatus;

/**
 * Created by 阿弥陀佛 on 2016/10/14.
 */

public class TimerStatusEvent {
    TimerStatus timerStatus;
    public TimerStatusEvent(TimerStatus timerStatus){
        this.timerStatus = timerStatus;
    }

    public TimerStatus getTimerStatus() {
        return timerStatus;
    }
}
