package com.wang17.myphone.activity;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.wang17.myphone.MainActivity;
import com.wang17.myphone.R;
import com.wang17.myphone.event.LocationGearEvent;
import com.wang17.myphone.event.LocationIsAutomaticEvent;
import com.wang17.myphone.fragment.ActionBarFragment;
import com.wang17.myphone.fragment.SettingFragment;
import com.wang17.myphone.fragment.WebDialogFragment;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.model.database.PhoneMessage;
import com.wang17.myphone.service.NianfoMusicService;
import com.wang17.myphone.util.BackupTask;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util.SmsHelper;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.model.database.RunLog;
import com.wang17.myphone.model.database.Setting;
import com.wang17.myphone.service.LocationService;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.Manifest.permission.WRITE_SETTINGS;

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
