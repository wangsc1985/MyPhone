package com.wang17.myphone.eventbus

class EventBusMessage private constructor(val sender:Any,val msg: String) {
    companion object {
        @JvmStatic
        fun getInstance(sender: Any,msg: String): EventBusMessage {
            return EventBusMessage(sender,msg)
        }
    }
}