package com.wang17.myphone.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.wang17.myphone.R
import com.wang17.myphone.fragment.ActionBarFragment.OnActionFragmentBackListener
import com.wang17.myphone.fragment.SettingFragment

class SettingActivity : AppCompatActivity(), OnActionFragmentBackListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
    }

    override fun onBackButtonClickListener() {
        finish()
    }
}