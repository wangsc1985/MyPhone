package com.wang17.myphone.view

import android.content.Context
import android.support.v7.widget.AppCompatButton
import android.widget.LinearLayout

class _Button : AppCompatButton {
    constructor(context: Context, text: String) : super(context) {
        this.text = text
        //        this.setBackground(getResources().getDrawable(R.drawable.btn_bg));
        val margin = 0
        var layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(margin, margin, margin, margin)
        layoutParams = layoutParams
    }

    constructor(context: Context, text: String, colorId: Int, margin: Int) : super(context) {
        this.text = text
        setBackgroundColor(resources.getColor(colorId))
        var layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(margin, margin, margin, margin)
        layoutParams = layoutParams
    }
}