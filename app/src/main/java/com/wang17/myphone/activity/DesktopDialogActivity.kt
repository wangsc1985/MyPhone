package com.wang17.myphone.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import com.wang17.myphone.R
import kotlinx.android.synthetic.main.activity_desktop_dialog.*

class DesktopDialogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_desktop_dialog)
        tv_msg.text = intent.getStringExtra("msg")
    }
}