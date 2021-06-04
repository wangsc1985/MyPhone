package com.wang17.myphone.database

import com.wang17.myphone.model.DateTime
import java.util.*

data class BankToDo(var dateTime: DateTime, var bankName: String, var summary: String, var money: Double) {
    var id: UUID = UUID.randomUUID()
}