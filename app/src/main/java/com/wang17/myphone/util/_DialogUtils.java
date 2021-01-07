package com.wang17.myphone.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Switch;

import com.wang17.myphone.R;
import com.wang17.myphone.callback.MyCallback;
import com.wang17.myphone.model.database.BankToDo;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.model.database.Setting;
import com.wang17.myphone.widget.MyWidgetProvider;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.wang17.myphone.util._Utils.e;

public class _DialogUtils {

    private static final String TAG = "wangsc";

    /**
     * 新建todo项
     */
    public static void addTodoDialog(final Context context, final MyCallback callback) {
        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_todo, null);
        CalendarView calendarViewDate = view.findViewById(R.id.calendarView_date);
        EditText editTextTitle = view.findViewById(R.id.editText_title);
        EditText editTextSummery = view.findViewById(R.id.editText_summery);
        EditText editTextNumber = view.findViewById(R.id.editText_number);

        DateTime dateTime = new DateTime();
        calendarViewDate.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                dateTime.set(year,month,dayOfMonth);
            }
        });


        new AlertDialog.Builder(context)
                .setView(view).setCancelable(false).setPositiveButton("提交", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                try {

                    String bankName = editTextTitle.getText().toString();
                    double money = Double.parseDouble(editTextSummery.getText().toString());
                    String number = editTextNumber.getText().toString();

                    DataContext dataContext = new DataContext(context);

                    BankToDo bankToDo = new BankToDo(dateTime, bankName, number, money);
                    dataContext.addBankToDo(bankToDo);


                    // 发送更新广播
                    Intent intent = new Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW);
                    context.sendBroadcast(intent);

                    if (callback != null)
                        callback.callBack();

                } catch (Exception e) {
                    _Utils.printException(context, e);
                }
            }
        }).setNegativeButton("取消", null).create().show();

    }

    /**
     * 编辑todo项
     */
    public static void editTodoDialog(final Context context, final BankToDo bankToDo, final MyCallback callback) {
        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_todo, null);


        DataContext dataContext = new DataContext(context);
        DateTime dateTime = bankToDo.getDateTime();
        CalendarView calendarViewDate = view.findViewById(R.id.calendarView_date);
        EditText editTextTitle = view.findViewById(R.id.editText_title);
        EditText editTextSummery = view.findViewById(R.id.editText_summery);
        EditText editTextNumber = view.findViewById(R.id.editText_number);

        e("xxxxxxxxxxxxxxxxxxxxxxxx : "+bankToDo.getDateTime().toLongDateTimeString());
        calendarViewDate.setDate(bankToDo.getDateTime().getTimeInMillis());
        calendarViewDate.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                dateTime.set(year,month,dayOfMonth);
            }
        });
        editTextTitle.setText(bankToDo.getBankName());
        editTextTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    editTextTitle.selectAll();
                }
            }
        });
        String format= "#,##0.00";
        if(bankToDo.getMoney()%1==0){
            format= "#,##0";
        }
        editTextSummery.setText(new DecimalFormat(format).format(bankToDo.getMoney()));
        editTextSummery.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    editTextSummery.selectAll();
                }
            }
        });
        editTextNumber.setText(bankToDo.getCardNumber());
        editTextNumber.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    editTextNumber.selectAll();
                }
            }
        });
        new AlertDialog.Builder(context).setView(view).setCancelable(false).setPositiveButton("提交", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    String bankName = editTextTitle.getText().toString();
                    double money = Double.parseDouble(editTextSummery.getText().toString().replace(",",""));
                    String number = editTextNumber.getText().toString();

                    bankToDo.setBankName(bankName);
                    bankToDo.setDateTime(dateTime);
                    bankToDo.setMoney(money);
                    bankToDo.setCardNumber(number);
                    dataContext.editBankToDo(bankToDo);

                    // 发送更新广播
                    Intent intent = new Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW);
                    context.sendBroadcast(intent);

                    if (callback != null)
                        callback.callBack();
                } catch (Exception e) {
                    _Utils.printException(context, e);
                }
            }
        }).setNegativeButton("取消", null).create().show();

    }
}
