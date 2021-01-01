package com.wang17.myphone.event;

/**
 * Created by 阿弥陀佛 on 2016/10/16.
 */

public class ExceptionEvent {
    private String message;
    public ExceptionEvent(String message){
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
