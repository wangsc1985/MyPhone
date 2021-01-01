package com.wang17.myphone.event;

/**
 * Created by 阿弥陀佛 on 2016/10/16.
 */

public class VolumnChangedEvent {
    private float volumn;
    public VolumnChangedEvent(float volumn){
        this.volumn = volumn;
    }

    public float getVolumn() {
        return volumn;
    }
}
