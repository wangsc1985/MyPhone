package com.wang17.myphone.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.activity.AddCardActivity;
import com.wang17.myphone.activity.CardRecordActivity;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util.ParseCreditCard;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.model.database.BillRecord;
import com.wang17.myphone.model.database.CreditCard;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.structure.CardType;
import com.wang17.myphone.structure.RepayType;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.wang17.myphone.structure.CardType.储蓄卡;
import static com.wang17.myphone.structure.CardType.微粒贷;

/**
 * A simple {@link Fragment} subclass.
 */
@Deprecated
public class CardFragment extends Fragment {

    private static final int TO_CARD_RECORD_ACTIVITY = 0;
    private static final int TO_ADD_CARD_ACTIVITY = 1;
    private List<CreditCardForView> cardList;
    private ListView listView_cards;
    private View view;
    private DataContext dataContext;
    private CardListViewAdapter cardListViewAdapter;
    private Handler uiThreadHandler = null;
    private boolean changed;
    private static final int WARN_DAYS = 7;

    // 视图变量
    FloatingActionButton button_add;

    public CardFragment(){
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            dataContext = new DataContext(getActivity());
            uiThreadHandler = new Handler();
//            if (dataContext.getCreditCard().size() == 0) {
//                reCreatedata();
//            }
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            view = inflater.inflate(R.layout.fragment_card, container, false);
            //
            button_add = (FloatingActionButton) view.findViewById(R.id.button_add);
            button_add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(new Intent(getActivity(), AddCardActivity.class), TO_ADD_CARD_ACTIVITY);
                }
            });

            //
            listView_cards = (ListView) view.findViewById(R.id.listView_cards);
            listView_cards.setDividerHeight(0);
            listView_cards.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            // 长按卡片，进入设置界面
            listView_cards.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    CreditCardForView card = cardList.get(position);
                    switch (card.cardType) {
                        case 储蓄卡:
//                        break;
                        case 微粒贷:
                            reCreatedata();
                            break;
                        default:
                            sanPhoneMessageAsyn();
                    }
                    return true;
                }
            });
            // 点击卡片，进入详细界面
            listView_cards.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(getActivity(), CardRecordActivity.class);
                    intent.putExtra("cardNumber", cardList.get(position).cardNumber);
                    startActivityForResult(intent, TO_CARD_RECORD_ACTIVITY);
                }
            });
            //
            loadList();
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
        return view;
    }

    private void reCreatedata() {

        final ProgressDialog progressDialog = ProgressDialog.show(getContext(), "", "正在重建数据...", true, false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 删除所有卡片
                    dataContext.deleteCreditRecord();
                    // 重新加入常用卡片
                    dataContext.addCreditCard(new CreditCard("交通银行", "王世起", "5229646408387086", CardType.白金信用卡, 51000, 8, 26, RepayType.相对账单日, 1000, false, false, 0));
                    dataContext.addCreditCard(new CreditCard("中信银行", "王世起", "6226880092286103", CardType.白金信用卡, 19000, 18, 18, RepayType.相对账单日, 480, false, false, 19000));
                    dataContext.addCreditCard(new CreditCard("工商银行", "王世起", "4270300055600174", CardType.普通信用卡, 10000, 17, 21, RepayType.每月固定日, 0, false, false, 0));
                    dataContext.addCreditCard(new CreditCard("农业银行", "王世起", "6228481200216173919", 储蓄卡, 0, 0, 10000, RepayType.相对账单日, 0, false, false, 1000000));

                    // 删除卡片流水
                    dataContext.deleteBillRecord();
                    // 重新加入部分流水
                    dataContext.addBillRecord(new BillRecord("5229646408387086", new DateTime(2016, 8, 21), -2942.31, 47000, -1));
                    dataContext.addBillRecord(new BillRecord("4270300055600174", new DateTime(2016, 9, 20), -10000, 0, -1));
                    // 删除短信
                    dataContext.deletePhoneMessage();
                    dataContext.deleteBillStatement();

                    uiThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                loadList();
                                progressDialog.dismiss();
                            } catch (Exception e) {
                                _Utils.printException(getContext(), e);
                            }
                        }
                    });
                } catch (Exception e) {
                    _Utils.printException(getContext(), e);
                }
            }
        }).start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == TO_CARD_RECORD_ACTIVITY) {
                if (CardRecordActivity.isChanged) {
                    loadList();
                }
            }
            if (requestCode == TO_ADD_CARD_ACTIVITY) {
                if (AddCardActivity.isChanged) {
                    loadList();
                }
            }
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
    }

    private void loadList() {

        try {
            List<CreditCard> xxxList = dataContext.getCreditCard();
            this.cardList = new ArrayList<>();
            for (CreditCard card : xxxList) {
                CreditCardForView ccfv = new CreditCardForView(card);
                if (ccfv.cardType == CardType.微粒贷) {
                    if (!ccfv.currentBill.equals("0.00"))
                        cardList.add(ccfv);
                } else
                    cardList.add(ccfv);
            }
            Collections.sort(cardList, new SortByDateTimeASC());
            cardListViewAdapter = new CardListViewAdapter(getContext(), cardList);
            listView_cards.setAdapter(cardListViewAdapter);
//        cardListViewAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
    }

    /**
     * 适配器 - 信用卡列表
     */
    public class CardListViewAdapter extends BaseAdapter {

        private Context context;
        private List<CreditCardForView> cardList;

        public CardListViewAdapter(Context context, List<CreditCardForView> cardList) {
            this.context = context;
            this.cardList = cardList;
        }

        @Override
        public int getCount() {
            return cardList.size();
        }

        @Override
        public Object getItem(int position) {
            return cardList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                try {
                    CreditCardForView card = cardList.get(position);
                    if (card.cardType == 微粒贷) {
                        //region Description
                        convertView = View.inflate(context, R.layout.inflate_list_item_card_wld, null);
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
                        //endregion
                    }
                } catch (Exception e) {
                    _Utils.printException(getContext(), e);
                }
            }
            return null;
        }
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
            DecimalFormat format = new DecimalFormat("#,##0.00");

            try {
                this.cardName = card.getCardName();
                this.cardNumber = card.getCardNumber();
                this.cardType = card.getCardType();
                this.bankName = card.getBankName();
                this.balance = format.format(card.getBalance());

                DateTime today = DateTime.getToday();
                if (card.getCardType() == 微粒贷) {
                    List<BillRecord> billRecordList1 = dataContext.getBillRecords(card.getCardNumber(), true);
                    double money = 0, fee = 0; // 借款总金额、当前利息
                    int days = 0; // 所有借款项最大借款天数
                    DateTime dt = DateTime.getToday();
                    for (BillRecord billRecord : billRecordList1) {
                        money += billRecord.getBalance();
                        this.repayDate = billRecord.getDateTime().toShortDateString();
                        DateTime dateTime = billRecord.getDateTime();
                        //
                        if (dateTime.getTimeInMillis() < dt.getTimeInMillis()) {
                            dt = dateTime;
                        }
                        //
                        DateTime xxxDateTime = new DateTime(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay());
                        int xxxdays = (int) ((today.getTimeInMillis() - xxxDateTime.getTimeInMillis()) / 3600 / 1000 / 24); // 当前借款项借款天数
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
                    this.forSort = -1;
                }
            } catch (Exception e) {
                _Utils.printException(getContext(), e);
            }
        }
    }

    class SortByDateTimeASC implements Comparator {
        public int compare(Object o1, Object o2) {
            CreditCardForView br1 = (CreditCardForView) o1;
            CreditCardForView br2 = (CreditCardForView) o2;
            return br1.forSort - br2.forSort;
        }

    }

    private void sanPhoneMessageAsyn() {
        try {
            final ProgressDialog progressDialog = ProgressDialog.show(getContext(), "", "正在扫描短信...", true, false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        changed = ParseCreditCard.scanPhoneMessage(getContext());
                        uiThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (changed) {
                                        loadList();
                                    }
                                    progressDialog.dismiss();
                                } catch (Exception e) {
                                    _Utils.printException(getContext(), e);
                                }
                            }
                        });
                    } catch (Exception e) {
                        _Utils.printException(getContext(), e);
                    }
                }
            }).start();
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
    }
//
//    public static boolean scanPhoneMessage(Context context) {
//        try {
//            boolean changed = false;
//            List<String> bodys = new ArrayList<String>();
//            SmsHelper smsHelper = new SmsHelper(context);
//            List<PhoneMessage> sms = smsHelper.getSMSRecrod(SmsType.接收到, "银行");
//            for (PhoneMessage model : sms) {
//                BillRecord billRecord = null;
//                String body = model.getBody();
//                if (body.contains("交通银行")) {
//                    billRecord = jiaoTongCreditXiaoFei(context, model);
//                }
//                if (body.contains("农业银行")) {
//                    billRecord = nongYeDepositXiaoFei(context, model);
//                }
//                if (body.contains("微众银行")) {
//                    billRecord = weiLiDai(context, model);
//                }
//                if (billRecord != null) {
//                    DataContext dataContext = new DataContext(context);
//                    PhoneMessage phoneMessage = dataContext.getPhoneMessage(model.getId());
//                    if (phoneMessage == null) {
//                        dataContext.addPhoneMessage(model);
//                        smsHelper.delSms(model, true);
//                        dataContext.addBillRecord(billRecord);
//                        CreditCard creditCard = dataContext.getCreditCard(billRecord.getCardNumber());
//                        creditCard.setBalance(billRecord.getBalance());
//                        dataContext.updateCreditCard(creditCard);
//                        changed = true;
//                    }
//                }
//            }
//            return changed;
//        } catch (Exception e) {
//            _Utils.printException(e);
//        }
//        return false;
//    }
//
//    /**
//     * 您尾号7086交行信用卡24日14时03分成功消费人民币39.60元，当前可用额度为人民币104.29元。使用官方APP“买单吧” cc.bankcomm.com/appzzs ，秒查账单，分期、还款方便又安全！【交通银行】
//     *
//     * @param phoneMessage
//     * @return
//     */
//    private static BillRecord jiaoTongCreditXiaoFei(Context context, PhoneMessage phoneMessage) {
//        String msg = phoneMessage.getBody();
//        //  2016/10/24 条件判断：1、关键字匹配；
//        if (msg.contains("您尾号") && msg.contains("交行信用卡") && msg.contains("成功消费人民币") && msg.contains("当前可用额度为人民币")
//                && msg.contains("使用官方APP“买单吧” cc.bankcomm.com/appzzs ，秒查账单，分期、还款方便又安全！【交通银行】")) {
//
//            String msgNumber = regString(msg, "您尾号", "交行信用卡");
//
//            String sss = regString(msg, "交行信用卡", "日").trim();
//            if (sss.startsWith("0"))
//                sss = sss.substring(1);
//            int day = Integer.parseInt(sss);
//
//            sss = regString(msg, "日", "时").trim();
//            if (sss.startsWith("0"))
//                sss = sss.substring(1);
//            int hour = Integer.parseInt(sss);
//
//            sss = regString(msg, "时", "分").trim();
//            if (sss.startsWith("0"))
//                sss = sss.substring(1);
//            int min = Integer.parseInt(sss);
//
//            DateTime dateTime = new DateTime(phoneMessage.getCreateTime().getYear(), phoneMessage.getCreateTime().getMonth(), day, hour, min, 0);
//            double money = Double.parseDouble(regString(msg, "成功消费人民币", "元").trim());
//            double balance = Double.parseDouble(regString(msg, "当前可用额度为人民币", "元").trim());
//
//            String cardNumber = msgNumber;
//            DataContext dataContext = new DataContext(context);
//            CreditCard creditCard = dataContext.getCreditCard(msgNumber);
//            if (creditCard == null) {
//                creditCard = new CreditCard("交通银行", "未知", msgNumber, CardType.普通信用卡, 0, 0, 20, 0, true, false, balance);
//                dataContext.addCreditCard(creditCard);
//            } else {
//                cardNumber = creditCard.getCardNumber();
//            }
//            return new BillRecord(cardNumber, dateTime, money, balance, phoneMessage.getId());
//
//        }
//        return null;
//    }
//
//    /**
//     * 【中国农业银行】您尾号3919的农行账户于10月21日14时58分完成一笔转存交易，金额为9935.00，余额9935.23。
//     *
//     * @param phoneMessage
//     * @return
//     */
//    private static BillRecord nongYeDepositXiaoFei(Context context, PhoneMessage phoneMessage) {
//        String msg = phoneMessage.getBody();
//        //  2016/10/24 条件判断：1、关键字匹配；
//        if (msg.contains("【中国农业银行】您尾号") && msg.contains("的农行账户于") && msg.contains("金额为") && msg.contains("余额")) {
//
//            String msgNumber = regString(msg, "您尾号", "的农行账户");
//
//            String sss = regString(msg, "的农行账户于", "月").trim();
//            if (sss.startsWith("0"))
//                sss = sss.substring(1);
//            int month = Integer.parseInt(sss);
//
//            sss = regString(msg, "月", "日").trim();
//            if (sss.startsWith("0"))
//                sss = sss.substring(1);
//            int day = Integer.parseInt(sss);
//
//            sss = regString(msg, "日", "时").trim();
//            if (sss.startsWith("0"))
//                sss = sss.substring(1);
//            int hour = Integer.parseInt(sss);
//
//            sss = regString(msg, "时", "分").trim();
//            if (sss.startsWith("0"))
//                sss = sss.substring(1);
//            int min = Integer.parseInt(sss);
//
//            DateTime dateTime = new DateTime(phoneMessage.getCreateTime().getYear(), month - 1, day, hour, min, 0);
//            double money = Double.parseDouble(regString(msg, "金额为", "，余额").trim());
//            double balance = Double.parseDouble(regString(msg, "余额", "。").trim());
//
//            String cardNumber = msgNumber;
//            DataContext dataContext = new DataContext(context);
//            CreditCard creditCard = dataContext.getCreditCard(msgNumber);
//            if (creditCard == null) {
//                creditCard = new CreditCard("农业银行", "未知", msgNumber, 储蓄卡, 0, 0, 1000, 0, true, false, balance);
//                dataContext.addCreditCard(creditCard);
//            } else {
//                cardNumber = creditCard.getCardNumber();
//            }
//            return new BillRecord(cardNumber, dateTime, money, balance, phoneMessage.getId());
//        }
//        return null;
//    }
//
//    private static BillRecord weiLiDai(Context context, PhoneMessage phoneMessage) {
////        借款成功通知：您好，您在手机微信申请的微粒贷  5000.00  元已成功发放，收款卡   农业银行(3919)，   预计3分钟内到账。【微众银行】
//
//        String msg = phoneMessage.getBody();
//        //  2016/10/24 条件判断：1、关键字匹配；
//        if (msg.contains("借款成功通知：您好，您在手机微信申请的微粒贷") && msg.contains("元已成功发放，收款卡") && msg.contains("预计3分钟内到账。【微众银行】")) {
//
//            DateTime dateTime = phoneMessage.getCreateTime();
//            double money = Double.parseDouble(regString(msg, "手机微信申请的微粒贷", "元已成功发放"));
//
//            String cardNumber = "645708679";
//            DataContext dataContext = new DataContext(context);
//            CreditCard creditCard = dataContext.getCreditCard(cardNumber);
//            if (creditCard == null) {
//                creditCard = new CreditCard("微众银行", "未知", cardNumber, 微粒贷, 26000, 0, -1, 0, true, false, 0);
//                dataContext.addCreditCard(creditCard);
//            } else {
//                cardNumber = creditCard.getCardNumber();
//            }
//
//            return new BillRecord(cardNumber, dateTime, money, money, phoneMessage.getId());
//        }
//        return null;
//    }
//
//    private static String regString(String msg, String startReg, String endReg) {
//        int start = msg.indexOf(startReg) + startReg.length();
//        String sss = msg.substring(start);
//        int end = sss.indexOf(endReg);
//        return sss.substring(0, end);
//    }

}
