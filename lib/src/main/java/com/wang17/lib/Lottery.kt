package com.wang17.lib


class Lottery {
    var period:Int
    var redArr:MutableList<Int>
    var blueArr:MutableList<Int>
    var multiple:Int
    var type:Int

    constructor(){
        period=0
        redArr=ArrayList<Int>()
        blueArr=ArrayList<Int>()
        multiple=1
        type=1
    }
    constructor(period: Int, redArr: MutableList<Int>, blueArr: MutableList<Int>, multiple: Int, type: Int) {
        this.period = period
        this.redArr = redArr
        this.blueArr = blueArr
        this.multiple = multiple
        this.type = type
    }
}