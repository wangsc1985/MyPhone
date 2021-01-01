package com.wang17.myphone.model

class Commodity {

    // 合约
    @JvmField
    var item: String // 名称
    @JvmField
    var name: String // 交易单位  比如：1000克/手
    @JvmField
    var unit: Int // 最小变动价位 0.05元/克
    @JvmField
    var cose: Double

    init {
        item=""
        name=""
        unit=0
        cose=0.0
    }

    constructor(item:String,name:String,unit:Int,cose:Double){
        this.item = item
        this.name = name
        this.unit = unit
        this.cose = cose
    }
}