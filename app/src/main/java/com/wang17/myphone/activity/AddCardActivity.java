package com.wang17.myphone.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.wang17.myphone.R;
import com.wang17.myphone.fragment.ActionBarFragment;
import com.wang17.myphone.util._Utils;

public class AddCardActivity extends AppCompatActivity  implements ActionBarFragment.OnActionFragmentBackListener {

    public static boolean isChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_add_card);
        } catch (Exception e) {
            _Utils.printException(this,e);
        }
    }

    @Override
    public void onBackButtonClickListener() {
        this.finish();
    }
}
