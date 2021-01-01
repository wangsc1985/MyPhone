package com.wang17.myphone.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.GridView

/**
 * Created by �����ӷ� on 2015/6/23.
 */
class UserGridView : GridView {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defstyle: Int) : super(context, attrs, defstyle) {}

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val expandspec = MeasureSpec.makeMeasureSpec(
                Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, expandspec)
    }
}