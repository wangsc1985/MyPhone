package com.wang17.myphone.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.database.PhoneMessage;
import com.wang17.myphone.database.DataContext;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.widget.MyWidgetProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsActivity extends AppCompatActivity {

    private List<PhoneMessage> mSmsList;
    private SmsListdAdapter adapter;
    private ListView mSmsListView;
    private FloatingActionButton actionButtonAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        mSmsList = new ArrayList<>();
        adapter = new SmsListdAdapter();
        mSmsListView = findViewById(R.id.listView_sms);

        // 添加异常捕捉
        try {
            Cursor cursor = getContentResolver().query(
                    Uri.parse("content://sms"),
                    new String[]{"_id", "address", "read", "body", "date"},null, null, "date desc"); // datephone想要的短信号码

            if (cursor != null) { // 当接受到的新短信与想要的短信做相应判断

                String body = "";
                while (cursor.moveToNext()) {
                    body = cursor.getString(cursor.getColumnIndex("body"));// 在这里获取短信信息
                    long smsdate = Long.parseLong(cursor.getString(cursor.getColumnIndex("date")));
                    e(body);
                }

            }
            DataContext dataContext = new DataContext(this);
            mSmsList = dataContext.getPhoneMessages();
            mSmsListView.setAdapter(adapter);
//            }
        } catch (Exception ex) {
            _Utils.printException(this, ex);
            Log.e("wangsc", ex.getMessage());
        }
    }

    private void e(Object log) {
        Log.e("wangsc",log.toString());
    }

    class SmsModel {
        String number;
        String body;
        DateTime smsDate;

        public SmsModel(String number, String body, DateTime smsDate) {
            this.number = number;
            this.body = body;
            this.smsDate = smsDate;
        }
    }

    private int preDay;
    private HashMap<Integer, View> mHashMap = new HashMap<>();

    protected class SmsListdAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mSmsList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            try {

                convertView = View.inflate(SmsActivity.this, R.layout.inflate_list_item_sms, null);

                if (mHashMap.get(position) == null) {
                    mHashMap.put(position, convertView);

                    PhoneMessage sms = mSmsList.get(position);
                    LinearLayout layoutDate = convertView.findViewById(R.id.layout_date);
                    LinearLayout root = convertView.findViewById(R.id.linearLayout_root);
                    TextView textViewDate = convertView.findViewById(R.id.tv_att_date);
                    TextView textViewDateTime = convertView.findViewById(R.id.textView_dateTime);
                    TextView textViewBody = convertView.findViewById(R.id.textView_body);
                    TextView textViewNumber = convertView.findViewById(R.id.textView_number);
                    root.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {

                            new AlertDialog.Builder(SmsActivity.this).setMessage("要删除所有短信信息吗？").setPositiveButton("是", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    DataContext dataContext = new DataContext(SmsActivity.this);
                                    dataContext.deletePhoneMessage();
                                    mSmsList = dataContext.getPhoneMessages();
                                    adapter.notifyDataSetChanged();
                                }
                            }).show();
                            return true;
                        }
                    });
                    if (sms.getCreateTime().getDay() != preDay) {
                        textViewDate.setText(sms.getCreateTime().toShortDateString());
                        layoutDate.setVisibility(View.VISIBLE);
                        preDay = sms.getCreateTime().getDay();
                    } else {
                        layoutDate.setVisibility(View.GONE);
                    }
                    textViewDateTime.setText(sms.getCreateTime().toLongDateTimeString());
                    textViewBody.setText(sms.getBody());
                    textViewNumber.setText(sms.getAddress());

                    Matcher matcher = Pattern.compile("-?\\d+\\.\\d+").matcher(sms.getBody());
                    if (matcher.find()) {
                        double money = Double.parseDouble(matcher.group());
                        if (Math.abs(money) > 100) {
                            textViewBody.setTextColor(Color.BLACK);
                        }
                    }

                } else {
                    convertView = mHashMap.get(position);
                }
            } catch (Exception e) {
                _Utils.printException(SmsActivity.this, e);
            }
            return convertView;
        }
    }


    private void delTodoDialog() {
        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_del_todo, null);

        new AlertDialog.Builder(this)
                .setView(view).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                try {
                    EditText editTextTitle = view.findViewById(R.id.editText_title);
                    EditText editTextSummary = view.findViewById(R.id.editText_summary);

                    String bankName = editTextTitle.getText().toString();
                    String cardNumber = editTextSummary.getText().toString();
                    DataContext dataContext = new DataContext(SmsActivity.this);

                    dataContext.deleteBankToDo(bankName, cardNumber);

                    // 发送更新广播
                    Intent intent = new Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW);
                    sendBroadcast(intent);

                } catch (Exception e) {
                    _Utils.printException(SmsActivity.this, e);
                }
            }
        }).setNegativeButton("CANCEL", null).create().show();
    }

}
