package com.wang17.myphone.database

import com.wang17.myphone.model.DateTime
import java.math.BigDecimal
import java.util.*

data class Statement(
                              var date: DateTime,
                              var fund: BigDecimal,
                              var profit: BigDecimal,
){
    var id: UUID=UUID.randomUUID()
}
