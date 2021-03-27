package com.wang17.myphone.fragment;


import android.app.Dialog;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.wang17.myphone.R;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.database.Setting;
import com.wang17.myphone.database.TallyRecord;
import com.wang17.myphone.database.DataContext;

import java.util.Calendar;

/**
 * A simple {@link Fragment} subclass.
 */
public class AddTallyFragment extends DialogFragment {

    public AfterAddRecordListener afterAddRecordListener;
    public void setAfterAddRecordListener(AfterAddRecordListener afterAddRecordListener) {
        this.afterAddRecordListener = afterAddRecordListener;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = View.inflate(getContext(), R.layout.inflate_add_tally, null);
        final AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
        dialog.setView(view);

        final EditText textTotal = (EditText) view.findViewById(R.id.text_total);
        final CalendarView date = (CalendarView) view.findViewById(R.id.calendar_date);
        final NumberPicker numberPickerHour = (NumberPicker)view.findViewById(R.id.numberPicker_Hour);
        final NumberPicker numberPickerMinite = (NumberPicker)view.findViewById(R.id.numberPicker_Minite);
        String[] hourNumbers = new String[100];
        for (int i = 0; i < 100; i++) {
            hourNumbers[i] = i + "点";
        }
        String[] minNumbers = new String[60];
        for (int i = 0; i < 60; i++) {
            minNumbers[i] = i + "分";
        }
        numberPickerHour.setMaxValue(23);
        numberPickerHour.setMinValue(0);
        numberPickerHour.setDisplayedValues(hourNumbers);
        numberPickerHour.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
        DateTime now = new DateTime();
        numberPickerHour.setValue(now.getHour());
        numberPickerMinite.setMinValue(0);
        numberPickerMinite.setMaxValue(59);
        numberPickerMinite.setDisplayedValues(minNumbers);
        numberPickerMinite.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中

        Button buttonOK = (Button) view.findViewById(R.id.button_ok);

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DateTime dateTime = new DateTime(date.getDate());
                DataContext dataContext = new DataContext(getContext());
                dateTime.set(Calendar.HOUR_OF_DAY,numberPickerHour.getValue());
                dateTime.set(Calendar.MINUTE,numberPickerMinite.getValue());
                TallyRecord tallyRecord = new TallyRecord(dateTime, Integer.parseInt(textTotal.getText().toString()) * 60 * 1000,dataContext.getSetting(Setting.KEYS.tally_record_item_text,"").getString());
                dataContext.addRecord(tallyRecord);
                textTotal.setSelection(0,textTotal.getText().length());
                Toast.makeText(getContext(), "添加记录成功！", Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                if(afterAddRecordListener!=null){
                    afterAddRecordListener.addRecode();
                }
            }
        });

        return dialog;
    }

    public interface AfterAddRecordListener{
        void addRecode();
    }
}
