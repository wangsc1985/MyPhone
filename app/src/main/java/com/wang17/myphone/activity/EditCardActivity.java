package com.wang17.myphone.activity;

import android.app.Dialog;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.util.CreditCardHelper;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.database.CreditCard;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.structure.CardType;
import com.wang17.myphone.structure.RepayType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EditCardActivity extends AppCompatActivity {
    // 视图变量
    private Spinner spinner_cardType;
    private EditText editText_bankName;
    private EditText editText_accountName;
    private EditText editText_accountNumber;
    private EditText editText_creditLine;
    private TextView textView_billday;
    private TextView textView_repayDate;
    private RadioGroup radioGroup_repayType;
    // 值变量
    private CreditCard creditCard;
    private DataContext dataContext;
    public static boolean isChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_edit_card);


            dataContext = new DataContext(this);
            creditCard = dataContext.getCreditCard(getIntent().getStringExtra("cardNumber"));
            init();
        } catch (Exception e) {
            _Utils.printException(EditCardActivity.this,e);
        }
    }

    private void init() {
        try {
            spinner_cardType = (Spinner) findViewById(R.id.spinner_cardtype);
            List<String> values = new ArrayList<>();
            for (CardType cardType : CardType.values()) {
                values.add(cardType.toString());
            }
            fillSpinner(spinner_cardType, values);
            spinner_cardType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    creditCard.setCardType(CardType.valueOf(spinner_cardType.getItemAtPosition(position).toString()));
                    dataContext.updateCreditCard(creditCard);
                    isChanged();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });


            editText_bankName = (EditText) findViewById(R.id.editText_bankName);
            editText_bankName.setText(creditCard.getBankName());
            editText_bankName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    creditCard.setBankName(s.toString());
                    dataContext.updateCreditCard(creditCard);
                    isChanged();
                }
            });

            editText_accountName = (EditText) findViewById(R.id.editText_accountName);
            editText_accountName.setText(creditCard.getCardName());
            editText_accountName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    creditCard.setCardName(s.toString());
                    dataContext.updateCreditCard(creditCard);
                    isChanged();
                }
            });

            editText_accountNumber = (EditText) findViewById(R.id.editText_accountNumber);
            editText_accountNumber.setText(creditCard.getCardNumber());
            editText_accountNumber.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    creditCard.setCardNumber(s.toString());
                    dataContext.updateCreditCard(creditCard);
                    isChanged();
                }
            });

//            DecimalFormat format = new DecimalFormat("#,##0.00");
            editText_creditLine = (EditText) findViewById(R.id.editText_creditLine);
            editText_creditLine.setText(creditCard.getCreditLine() + "");
            editText_creditLine.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    creditCard.setCreditLine(Double.parseDouble(s.toString()));
                    dataContext.updateCreditCard(creditCard);
                    isChanged();
                }
            });

            textView_billday = (TextView) findViewById(R.id.textView_billday);
            textView_billday.setText(creditCard.getBillDay() + "日");

            textView_repayDate = (TextView) findViewById(R.id.textView_repayDate);
            textView_repayDate.setText(CreditCardHelper.getCurrentRepayDate(EditCardActivity.this, creditCard).toShortDateString());
            textView_repayDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SetRepayDateDialog dialog = new SetRepayDateDialog();
                    dialog.show();
                }
            });

            radioGroup_repayType = (RadioGroup) findViewById(R.id.radioGroup_repayType);
            RadioButton radioButton_a = (RadioButton) findViewById(R.id.radioButton_a);
            RadioButton radioButton_b = (RadioButton) findViewById(R.id.radioButton_b);
            radioButton_a.setText(RepayType.每月固定日.toString());
            radioButton_b.setText(RepayType.相对账单日.toString());
            if (creditCard.getRepayType() == RepayType.每月固定日) {
                radioButton_a.setChecked(true);
                radioButton_b.setChecked(false);
            }
            radioGroup_repayType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    creditCard.setRepayType(RepayType.valueOf(radioButton.getText().toString()));
                    dataContext.updateCreditCard(creditCard);
                    isChanged();
                }
            });

        } catch (Exception e) {
            _Utils.printException(this,e);
        }
    }


    private void fillSpinner(Spinner spinner, List<String> values) {
        try {
            ArrayAdapter<String> aspn = new ArrayAdapter<String>(EditCardActivity.this, R.layout.inflate_spinner, values);
            aspn.setDropDownViewResource(R.layout.inflate_spinner_dropdown);
            spinner.setAdapter(aspn);
        } catch (Exception e) {
            _Utils.printException(EditCardActivity.this,e);
        }
    }

    private void isChanged() {
        try {
            isChanged = true;
            Snackbar.make(spinner_cardType, "设置已保存。", Snackbar.LENGTH_LONG).show();
        } catch (Exception e) {
            _Utils.printException(EditCardActivity.this,e);
        }
    }


    public class SetRepayDateDialog {
        private Dialog dialog;
        private boolean isChanged = false;

        public SetRepayDateDialog() {
            try {
                dialog = new Dialog(EditCardActivity.this);
                dialog.setContentView(R.layout.inflate_dialog_repaydate_picker);
                dialog.setTitle("请任意设置一期的还款日，系统自动推导出所有还款日。");

                final NumberPicker numberPicker_year = (NumberPicker) dialog.findViewById(R.id.number_year);
                final NumberPicker numberPicker_month = (NumberPicker) dialog.findViewById(R.id.number_month);
                final NumberPicker numberPicker_day = (NumberPicker) dialog.findViewById(R.id.number_day);
                Button btnOK = (Button) dialog.findViewById(R.id.btnOK);
                Button btnCancle = (Button) dialog.findViewById(R.id.btnCancel);

                DateTime today = DateTime.getToday();
                DateTime currentRepayDate = CreditCardHelper.getCurrentRepayDate(EditCardActivity.this, creditCard);

                numberPicker_year.setMinValue(today.getYear() - 1);
                numberPicker_year.setMaxValue(today.getYear());
                numberPicker_year.setValue(currentRepayDate.getYear());
                numberPicker_year.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
                numberPicker_month.setMinValue(1);
                numberPicker_month.setMaxValue(12);
                numberPicker_month.setValue(currentRepayDate.getMonth() + 1);
                numberPicker_month.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
                numberPicker_day.setMinValue(1);
                numberPicker_day.setMaxValue(28);
                numberPicker_day.setValue(currentRepayDate.getDay());
                numberPicker_day.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中

                numberPicker_month.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        isChanged = true;
                    }
                });
                numberPicker_day.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        isChanged = true;
                    }
                });

                btnOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isChanged) {
                            int year = numberPicker_year.getValue();
                            int month = numberPicker_month.getValue() - 1;
                            int day = numberPicker_day.getValue();
                            DateTime repayDate = new DateTime(year, month, day);
                            DateTime billDate = repayDate.addMonths(-1);
                            billDate.set(Calendar.DAY_OF_MONTH, EditCardActivity.this.creditCard.getBillDay());
                            if (EditCardActivity.this.creditCard.getRepayType() == RepayType.相对账单日) {
                                EditCardActivity.this.creditCard.setRepayDay((int) ((repayDate.getDate().getTimeInMillis() - billDate.getDate().getTimeInMillis()) / 1000 / 3600 / 24));
                            } else {
                                EditCardActivity.this.creditCard.setRepayDay(day);
                            }
                            dataContext.updateCreditCard(EditCardActivity.this.creditCard);
                            textView_repayDate.setText(CreditCardHelper.getCurrentRepayDate(EditCardActivity.this, creditCard).toShortDateString());
                            isChanged();
                        }
                        dialog.dismiss();
                    }
                });
                btnCancle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
            } catch (Exception e) {
                _Utils.printException(EditCardActivity.this,e);
            }
        }

        public void show() {
            try {
                dialog.show();
            } catch (Exception e) {
                _Utils.printException(EditCardActivity.this,e);
            }
        }
    }
}
