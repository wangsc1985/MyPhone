package com.wang17.myphone.model


class Lottery {
    var period:Int
    var redArr:MutableList<Int>
    var blueArr:MutableList<Int>
    var multiple:Int
    var type:Int

    constructor(){
        period=0
        redArr=ArrayList()
        blueArr=ArrayList()
        multiple=1
        type=1
    }
    constructor(period: Int, arr: MutableList<Int>, multiple: Int, type: Int) {
        this.period = period
        when(type){
            1->{
                this.redArr = arrayListOf(arr[0],arr[1],arr[2],arr[3],arr[4])
                this.blueArr = arrayListOf(arr[5],arr[6])
            }
            else->{
                this.redArr = arrayListOf(arr[0],arr[1],arr[2],arr[3],arr[4],arr[5])
                this.blueArr = arrayListOf(arr[6])
            }
        }
        this.multiple = multiple
        this.type = type
    }
}