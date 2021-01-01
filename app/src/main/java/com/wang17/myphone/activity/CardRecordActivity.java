package com.wang17.myphone.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.fragment.ActionBarFragment;
import com.wang17.myphone.util.CreditCardHelper;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.util._String;
import com.wang17.myphone.model.database.BillRecord;
import com.wang17.myphone.model.database.CreditCard;
import com.wang17.myphone.model.CreditCardResult;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.structure.CardType;
import com.wang17.myphone.structure.RepayType;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.wang17.myphone.structure.CardType.微粒贷;


public class CardRecordActivity extends AppCompatActivity implements ActionBarFragment.OnActionFragmentBackListener {


    //  视图变量
    RelativeLayout root;
    ExpandableListView expandableListView;
    LinearLayout layout_header;
    //    private ProgressDialog progressDialog;
    //  类变量
    private DataContext dataContext;
    private List<BillRecord> records;
    private List<GroupInfo> groupInfos;
    private List<List<BillRecord>> childInfos;
    private CreditCard creditCard;
    private BaseExpandableListAdapter expandableListAdapter;
    //  值变量
    private String cardNumber;
    public static boolean isChanged;
    private static final int WARN_DAYS = 7;
    public static final int TO_EDIT_CARD = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_record);
        dataContext = new DataContext(this);
        isChanged = false;

        try {

            cardNumber = this.getIntent().getStringExtra("cardNumber");
            creditCard = dataContext.getCreditCard(cardNumber);

            root = (RelativeLayout) findViewById(R.id.activity_card_record);
            ExpandableListView listView_records = (ExpandableListView) findViewById(R.id.listView_records);

            // 填充信用卡Info
            View view = getCardInfo(creditCard.getCardType());
            layout_header = (LinearLayout) findViewById(R.id.header);
            layout_header.addView(view);

            getListData();

            expandableListView = (ExpandableListView) findViewById(R.id.listView_records);
            expandableListAdapter = new CardRecordListAdapter();
            expandableListView.setAdapter(expandableListAdapter);

            //设置item点击的监听器
            expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, final int groupPosition, final int childPosition, long id) {
                    final BillRecord record = childInfos.get(groupPosition).get(childPosition);
                    if (creditCard.getCardType() == CardType.微粒贷) {
                        if (record.getSummary().equals("借款中")) {
                            new AlertDialog.Builder(CardRecordActivity.this).setMessage("确认已还清此借款？").setNegativeButton("确认", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    record.setBalance(0);
                                    record.setSummary("已还清");
                                    dataContext.updateBillRecord(record);
                                    expandableListAdapter.notifyDataSetChanged();
                                    layout_header.removeAllViews();
                                    layout_header.addView(CardRecordActivity.this.getCardInfo(creditCard.getCardType()));
                                    isChanged = true;
                                    dialog.dismiss();
                                }
                            }).setPositiveButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();
                        }
                    } else {
                        final BillRecord billRecord = childInfos.get(groupPosition).get(childPosition);
                        String message = "确认删除当前记录吗？";
                        if (billRecord.getSmsId() > 0)
                            message = dataContext.getPhoneMessage(billRecord.getSmsId()).getBody();
                        new AlertDialog.Builder(CardRecordActivity.this).setMessage(message).setNegativeButton("删除当前记录", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dataContext.deleteBillRecord(billRecord.getId());
                                if (billRecord.getSmsId() > 0)
                                    dataContext.deletePhoneMessage(billRecord.getSmsId());
                                childInfos.get(groupPosition).remove(childPosition);
                                expandableListAdapter.notifyDataSetChanged();
                                layout_header.removeAllViews();
                                layout_header.addView(CardRecordActivity.this.getCardInfo(creditCard.getCardType()));
                                isChanged = true;
                                dialog.dismiss();
                            }
                        }).setPositiveButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();

                    }
                    return false;
                }
            });
        } catch (Exception e) {
            _Utils.printException(this, e);
        }
    }

    private void getListData() {
        try {
            DateTime today = DateTime.getToday();
            DateTime startDateTime = null, endDateTime = null;
            int year = today.getYear();

            /**
             * 账单日：8号
             * 2016-01-05  >=2015-01-09   <2016-01-09
             * 2015-12-23  >=2015-01-09   <2016-01-09
             * 2016-02-23  >=2016-01-09   <2017-01-09
             */
            DateTime xxx = new DateTime(today.getYear(), 0, creditCard.getBillDay());
            xxx.add(Calendar.DAY_OF_MONTH, 1);
            if (today.getTimeInMillis() < xxx.getTimeInMillis()) {
                year = today.getYear() - 1;
            }
            startDateTime = new DateTime(year, 0, creditCard.getBillDay());
            endDateTime = new DateTime(year + 1, 0, creditCard.getBillDay());
            startDateTime.add(Calendar.DAY_OF_MONTH, 1);
            endDateTime.add(Calendar.DAY_OF_MONTH, 1);

            records = dataContext.getBillRecords(cardNumber, startDateTime, endDateTime);

            childInfos = new ArrayList<>();
            groupInfos = new ArrayList<>();
            int month = today.getMonth();
            if (year != today.getYear())
                month = 11;
            for (int i = month; i >= 0; i--) {
                startDateTime = new DateTime(year, i, creditCard.getBillDay());
                startDateTime.add(Calendar.DAY_OF_MONTH, 1);
                if (i == 11)
                    endDateTime = new DateTime(year + 1, 0, creditCard.getBillDay());
                else
                    endDateTime = new DateTime(year, i + 1, creditCard.getBillDay());
                endDateTime.add(Calendar.DAY_OF_MONTH, 1);
                double money = 0;
                //
                List<BillRecord> billRecordList = dataContext.getBillRecords(cardNumber, startDateTime, endDateTime);
                Collections.sort(billRecordList, new SortByDateTimeDESC());
                childInfos.add(billRecordList);
                //
                endDateTime.add(Calendar.DAY_OF_MONTH, -1);
                for (BillRecord re : billRecordList) {
                    money += re.getMoney();
                }
                groupInfos.add(new GroupInfo(i + 1, startDateTime.getMonthStr() + "." + startDateTime.getDayStr() + " - " + endDateTime.getMonthStr() + "." + endDateTime.getDayStr(), money, "消费"));
            }

            Collections.sort(groupInfos, new SortByMonthDESC());
        } catch (Exception e) {
            _Utils.printException(CardRecordActivity.this,e);
        }
    }

    private View getCardInfo(CardType cardType) {
        try {
            CreditCardForView card = new CreditCardForView(creditCard);
            View convertView = null;
            if (cardType == CardType.储蓄卡) {
                convertView = View.inflate(this, R.layout.inflate_card_deposit_header, null);
                TextView textView_name = (TextView) convertView.findViewById(R.id.textView_name); // 账户名
                TextView textView_number = (TextView) convertView.findViewById(R.id.textView_number); // 卡号
                TextView textView_cardType = (TextView) convertView.findViewById(R.id.textView_cardType); // 卡类型
                TextView textView_bankName = (TextView) convertView.findViewById(R.id.textView_bankName); // 银行

                TextView textView_monthConsume = (TextView) convertView.findViewById(R.id.textView_monthConsume);
                TextView textView_yearConsume = (TextView) convertView.findViewById(R.id.textView_yearConsume);
                TextView textView_balance = (TextView) convertView.findViewById(R.id.textView_balance);

                textView_name.setText(card.cardName);
                textView_number.setText(_String.formatToCardNumber(card.cardNumber));
                textView_cardType.setText(card.cardType.toString());
                textView_bankName.setText(card.bankName);

                textView_monthConsume.setText(card.currentBill);
                textView_yearConsume.setText(card.futureBill);
                textView_balance.setText(card.balance);
                return convertView;
            } else if (card.cardType == 微粒贷) {
                convertView = View.inflate(this, R.layout.inflate_header_wld_card, null);
                TextView textView_name = (TextView) convertView.findViewById(R.id.textView_name); // 账户名
                TextView textView_number = (TextView) convertView.findViewById(R.id.textView_number); // 卡号
                TextView textView_cardType = (TextView) convertView.findViewById(R.id.textView_cardType); // 卡类型
                TextView textView_bankName = (TextView) convertView.findViewById(R.id.textView_bankName); // 银行
                TextView textView_date = (TextView) convertView.findViewById(R.id.textView_date); //
                TextView textView_money = (TextView) convertView.findViewById(R.id.textView_money); //
                TextView textView_days = (TextView) convertView.findViewById(R.id.textView_days); //
                TextView textView_fee = (TextView) convertView.findViewById(R.id.textView_fee); //

                textView_name.setText(card.bankName);
                textView_number.setText(card.cardNumber);
                textView_cardType.setText(card.cardType.toString());
                textView_date.setText(card.repayDate);
                textView_bankName.setText(card.bankName);
                textView_money.setText(card.currentBill);
                textView_days.setText(card.repayDays);
                textView_fee.setText(card.futureBill);
                return convertView;
            } else {
                convertView = View.inflate(this, R.layout.inflate_card_credit_header, null);
                TextView textView_name = (TextView) convertView.findViewById(R.id.textView_name); // 账户名
                TextView textView_number = (TextView) convertView.findViewById(R.id.textView_number); // 卡号
                TextView textView_cardType = (TextView) convertView.findViewById(R.id.textView_cardType); // 卡类型
                TextView textView_bankName = (TextView) convertView.findViewById(R.id.textView_bankName); // 银行
                TextView textView_repayDays = (TextView) convertView.findViewById(R.id.textView_repayDays); // 还款天数
                TextView textView_repayDate = (TextView) convertView.findViewById(R.id.textView_repayDate); // 还款日期
                TextView textView_billDays = (TextView) convertView.findViewById(R.id.textView_billDays); // 账单天数
                TextView textView_billDate = (TextView) convertView.findViewById(R.id.textView_billDate); // 账单日期
                TextView textView_restLine = (TextView) convertView.findViewById(R.id.textView_restLine); // 剩余额度
                TextView textView_currentBill = (TextView) convertView.findViewById(R.id.textView_currentBill); // 本期账单
                TextView textView_futureBill = (TextView) convertView.findViewById(R.id.textView_futureBill); // 下期账单
                TextView textView_foiDays = (TextView) convertView.findViewById(R.id.textView_foiDays); //  免息天数
                TextView textView_maxFoiDays = (TextView) convertView.findViewById(R.id.textView_maxFoiDays); //  免息天数
                ImageView imageView_edit = (ImageView) convertView.findViewById(R.id.imageView_edit);
                imageView_edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(CardRecordActivity.this, EditCardActivity.class);
                        intent.putExtra("cardNumber", cardNumber);
                        startActivityForResult(intent, TO_EDIT_CARD);
                    }
                });

                textView_name.setText(card.cardName);
                textView_number.setText(_String.formatToCardNumber(card.cardNumber));
                textView_cardType.setText(card.cardType.toString());
                textView_bankName.setText(card.bankName);
                textView_repayDays.setText(card.repayDays);
                try {
                    if (card.repayDays.contains("超") || Integer.parseInt(card.repayDays) <= WARN_DAYS) {
                        textView_repayDays.setTextColor(Color.RED);
                        textView_repayDate.setTextColor(Color.RED);
                        ((TextView) convertView.findViewById(R.id.textView002)).setTextColor(Color.RED);
                    }
                } catch (NumberFormatException e) {
                }
                textView_repayDate.setText(card.repayDate);
                textView_billDays.setText(card.billDays);
                textView_billDate.setText(card.billDate);
                textView_restLine.setText(card.balance);
                textView_currentBill.setText(card.currentBill);
                textView_futureBill.setText(card.futureBill);
                textView_foiDays.setText(card.foiDays);
                textView_maxFoiDays.setText(card.maxFoiDays);
                return convertView;
            }
        } catch (Exception e) {
            _Utils.printException(this, e);
        }
        return null;
    }

    class CreditCardForView {
        public int forSort;//
        public String cardName;//
        public String cardNumber;//
        public CardType cardType;//
        public String bankName;//
        public String repayDays;
        public String repayDate;
        public String billDays;
        public String billDate;
        public String balance;// 如果是储蓄卡为余额，如果是信用卡为可用额度。
        public String currentBill;// 如果是储蓄卡为当月消费，如果是信用卡为本期账单。
        public String futureBill;// 如果是储蓄卡为本年消费，如果是信用卡为下期账单。
        public String foiDays;
        public String maxFoiDays;

        public CreditCardForView(CreditCard card) {
            try {
                DecimalFormat format = new DecimalFormat("#,##0.00");

                this.cardName = card.getCardName();
                this.cardNumber = card.getCardNumber();
                this.cardType = card.getCardType();
                this.bankName = card.getBankName();
                this.balance = format.format(card.getBalance());

                DateTime today = DateTime.getToday();
                if (card.getCardType() == CardType.储蓄卡) {
                    DateTime startDateTime = new DateTime(today.getYear(), 1, 1), endDateTime = new DateTime(today.getYear() + 1, 1, 1);
                    List<BillRecord> billRecordList1 = dataContext.getBillRecords(card.getCardNumber(), startDateTime, endDateTime);
                    double money1 = 0;
                    for (BillRecord billRecord : billRecordList1) {
                        money1 += billRecord.getMoney();
                    }
                    startDateTime = new DateTime(today.getYear(), today.getMonth(), 1);
                    endDateTime = startDateTime.addMonths(1);
                    List<BillRecord> billRecordList2 = dataContext.getBillRecords(card.getCardNumber(), startDateTime, endDateTime);
                    double money2 = 0;
                    for (BillRecord billRecord : billRecordList2) {
                        money2 += billRecord.getMoney();
                    }
                    this.currentBill = format.format(money2);
                    this.futureBill = format.format(money1);
                    this.forSort = 10001;
                } else if (card.getCardType() == 微粒贷) {
                    List<BillRecord> billRecordList = dataContext.getBillRecords(card.getCardNumber(), true);
                    double money = 0, fee = 0;
                    int days = 0;
                    DateTime dt = DateTime.getToday();
                    for (BillRecord billRecord : billRecordList) {
                        money += billRecord.getBalance();
                        this.repayDate = billRecord.getDateTime().toShortDateString();
                        DateTime dateTime = billRecord.getDateTime();
                        //
                        if (dateTime.getTimeInMillis() < dt.getTimeInMillis()) {
                            dt = dateTime;
                        }
                        //
                        DateTime xxxDateTime = new DateTime(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay());
                        int xxxdays = (int) ((today.getTimeInMillis() - xxxDateTime.getTimeInMillis()) / 3600 / 1000 / 24);
                        if (xxxdays > days)
                            days = xxxdays;
                        if (xxxdays == 0)
                            xxxdays = 1;
                        //
                        fee += xxxdays * billRecord.getBalance() * 0.0005;
                    }
                    this.repayDate = dt.toShortDateString();
                    this.repayDays = days + "";
                    this.currentBill = format.format(money);
                    this.futureBill = format.format(fee);
                } else {
                    DateTime xxxRepayDate = null;
                    CreditCardResult result = CreditCardHelper.getCreditCardResult(CardRecordActivity.this, card);
                    // 已出剩余账单、未出剩余账单
                    double remanentCurrent = result.consumeTotalCurrent - result.repayTotalNext;
                    double remanentFutue = result.consumeTotalNext;

                    //
                    today = DateTime.getToday();
                    if (remanentCurrent > 0) {
                        xxxRepayDate = new DateTime(result.endBillDateCurrent.getYear(), result.endBillDateCurrent.getMonth(), card.getBillDay());
                    } else if (remanentFutue > 0) {
                        xxxRepayDate = new DateTime(result.endBillDateNext.getYear(), result.endBillDateNext.getMonth(), card.getBillDay());
                    }
                    if (xxxRepayDate != null) {
                        if (card.getRepayType() == RepayType.相对账单日) {
                            xxxRepayDate.add(Calendar.DAY_OF_MONTH, card.getRepayDay());
                        } else {
                            if (card.getRepayDay() < card.getBillDay())
                                xxxRepayDate.add(Calendar.MONTH, 1);
                            xxxRepayDate.set(Calendar.DAY_OF_MONTH, card.getRepayDay());
                        }
                    }

                    //
                    if (xxxRepayDate == null) {
                        this.repayDays = "--";
                        this.repayDate = "--";
                        this.forSort = 10000;
                    } else {
                        if (xxxRepayDate.getDate().equals(today)) {
                            this.repayDays = "今";
                            this.forSort = 0;
                        } else {
                            this.forSort = (int) ((xxxRepayDate.getTimeInMillis() - today.getTimeInMillis()) / 60000 / 60 / 24);
                            if (this.forSort < 0) {
                                this.repayDays = "超" + (this.forSort * -1);
                            } else {
                                this.repayDays = this.forSort + "";
                            }
                        }
                        this.repayDate = xxxRepayDate.toShortDateString();
                    }

                    long a = (result.billDateCurrent.getTimeInMillis() - DateTime.getToday().getTimeInMillis()) / 60000 / 60 / 24;
                    this.billDays = a == 0 ? "今" : a + "";
                    this.billDate = result.billDateCurrent.toShortDateString();
                    this.currentBill = format.format(result.consumeTotalCurrent - result.repayTotalNext);
                    this.futureBill = format.format(result.consumeTotalNext);
                    this.foiDays = ((result.endBillDateNext.getTimeInMillis() - DateTime.getToday().getTimeInMillis()) / 60000 / 60 / 24 + card.getRepayDay()) + "天";
                    this.maxFoiDays = ((result.endBillDateNext.getTimeInMillis() - result.startBillDateNext.getTimeInMillis() + 1) / 60000 / 60 / 24 + card.getRepayDay()) + "天";
                }
            } catch (Exception e) {
                _Utils.printException(CardRecordActivity.this,e);
            }
        }
    }

    class CardRecordListAdapter extends BaseExpandableListAdapter {
        @Override
        public int getGroupCount() {
            return groupInfos.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groupInfos.get(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return childInfos.get(groupPosition).size();
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return childInfos.get(groupPosition).get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {

            try {
                convertView = View.inflate(CardRecordActivity.this, R.layout.inflate_list_card_record_group, null);
                TextView textView_month = (TextView) convertView.findViewById(R.id.textView_month);
                TextView textView_dateSpan = (TextView) convertView.findViewById(R.id.textView_dateSpan);
                TextView textView_money = (TextView) convertView.findViewById(R.id.textView_money);
                TextView textView_item = (TextView) convertView.findViewById(R.id.textView_item);

                GroupInfo info = groupInfos.get(groupPosition);
                textView_month.setText(info.month + "月");
                textView_dateSpan.setText(info.dateSpan);
                DecimalFormat format = new DecimalFormat("#,##0.00");
                textView_money.setText(format.format(info.money));
                textView_item.setText(info.item);
            } catch (Exception e) {
                _Utils.printException(CardRecordActivity.this, e);
            }
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            try {
                convertView = View.inflate(CardRecordActivity.this, R.layout.inflate_list_card_record_child, null);
                TextView textView_summary = (TextView) convertView.findViewById(R.id.textView_summary);
                TextView textView_use = (TextView) convertView.findViewById(R.id.textView_use);
                TextView textView_money = (TextView) convertView.findViewById(R.id.textView_money);
                TextView textView_time = (TextView) convertView.findViewById(R.id.textView_time);

                BillRecord billRecord = childInfos.get(groupPosition).get(childPosition);
                textView_summary.setText(billRecord.getSummary());
                textView_use.setText(billRecord.getCurrency());
                DecimalFormat format = new DecimalFormat("#,##0.00");
                textView_money.setText(format.format(billRecord.getMoney()));
                textView_time.setText(billRecord.getDateTime().toShortDateString());
            } catch (Exception e) {
                _Utils.printException(CardRecordActivity.this, e);
            }
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

    }

    class SortByMonthDESC implements Comparator {
        public int compare(Object o1, Object o2) {
            GroupInfo gi1 = (GroupInfo) o1;
            GroupInfo gi2 = (GroupInfo) o2;
            return gi2.month - gi1.month;
        }
    }

    class SortByDateTimeDESC implements Comparator {
        public int compare(Object o1, Object o2) {
            BillRecord br1 = (BillRecord) o1;
            BillRecord br2 = (BillRecord) o2;
            return (int) (br2.getDateTime().getTimeInMillis() - br1.getDateTime().getTimeInMillis());
        }

    }

    class GroupInfo {
        public int month;
        public String dateSpan;
        public double money;
        public String item;

        public GroupInfo(int month, String dateSpan, double money, String item) {
            this.month = month;
            this.dateSpan = dateSpan;
            this.money = money;
            this.item = item;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == TO_EDIT_CARD) {
                if (EditCardActivity.isChanged) {
                    isChanged = true;
                    layout_header.removeAllViews();
                    layout_header.addView(CardRecordActivity.this.getCardInfo(creditCard.getCardType()));
                }
            }
        } catch (Exception e) {
            _Utils.printException(CardRecordActivity.this,e);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackButtonClickListener() {
        this.finish();
    }
}
