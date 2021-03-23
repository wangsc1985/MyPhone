package com.wang17.myphone.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.wang17.myphone.R;
import com.wang17.myphone.fragment.ActionBarFragment;
import com.wang17.myphone.fragment.SettingFragment;

public class SettingActivity extends AppCompatActivity implements ActionBarFragment.OnActionFragmentBackListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_content, new SettingFragment()).commit();
    }

    @Override
    public void onBackButtonClickListener() {
        this.finish();
    }
}
