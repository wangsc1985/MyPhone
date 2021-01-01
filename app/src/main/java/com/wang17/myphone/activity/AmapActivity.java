package com.wang17.myphone.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.wang17.myphone.R;
import com.wang17.myphone.fragment.AmapFragment;
public class AmapActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amap);

//        Intent intent = getIntent();
//        double latitude = intent.getDoubleExtra("Latitude",0);
//        double longitude = intent.getDoubleExtra("Longitude",0);
//        Log.e("wangsc","latitude: "+latitude);
//        LatLng latLng =null;
//        if(latitude!=0&&longitude!=0){
//            latLng =new LatLng(latitude,longitude);
//        }
//
        AmapFragment fragment = new AmapFragment();
        android.support.v4.app.FragmentManager fragmentManager =getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction transaction = fragmentManager. beginTransaction();
        transaction.replace(R.id.fragment, fragment);
        transaction.commit();
    }
}
