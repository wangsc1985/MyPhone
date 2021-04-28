package com.wang17.myphone.activity

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.wang17.myphone.R
import com.wang17.myphone.fragment.ActionBarFragment.OnActionFragmentBackListener
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.DateTime.Companion.today
import com.wang17.myphone.database.BillRecord
import com.wang17.myphone.database.CreditCard
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.structure.CardType
import com.wang17.myphone.structure.RepayType
import com.wang17.myphone.util.CreditCardHelper
import com.wang17.myphone.util._String
import com.wang17.myphone.util._Utils
import kotlinx.android.synthetic.main.activity_card_record.*
import java.text.DecimalFormat
import java.util.*

class CardRecordActivity : AppCompatActivity(), OnActionFragmentBackListener {
    //    private ProgressDialog progressDialog;
    //  类变量
    private lateinit var dataContext: DataContext
    private lateinit var records: MutableList<BillRecord>
    private lateinit var groupInfos: MutableList<GroupInfo>
    private lateinit var childInfos: MutableList<MutableList<BillRecord>>
    private lateinit var creditCard: CreditCard
    private lateinit var expandableListAdapter: BaseExpandableListAdapter

    //  值变量
    private var cardNumber: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_record)
        dataContext = DataContext(this)
        isChanged = false
        try {
            cardNumber = this.intent.getStringExtra("cardNumber")
            creditCard = dataContext.getCreditCard(cardNumber?:"0000")
            val listView_records = findViewById<View>(R.id.listView_records) as ExpandableListView

            // 填充信用卡Info
            val view = getCardInfo(creditCard.getCardType())
            header.addView(view)
            listData
            expandableListAdapter = CardRecordListAdapter()
            listView_records.setAdapter(expandableListAdapter)

            //设置item点击的监听器
            this.listView_records.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
                val record = childInfos[groupPosition][childPosition]
                if (creditCard.getCardType() == CardType.微粒贷) {
                    if (record.summary == "借款中") {
                        AlertDialog.Builder(this@CardRecordActivity).setMessage("确认已还清此借款？").setNegativeButton("确认") { dialog, which ->
                            record.balance = 0.0
                            record.summary = "已还清"
                            dataContext.updateBillRecord(record)
                            expandableListAdapter.notifyDataSetChanged()
                            header.removeAllViews()
                            header.addView(getCardInfo(creditCard.getCardType()))
                            isChanged = true
                            dialog.dismiss()
                        }.setPositiveButton("取消") { dialog, which -> dialog.dismiss() }.show()
                    }
                } else {
                    val billRecord = childInfos[groupPosition][childPosition]
                    var message: String? = "确认删除当前记录吗？"
                    AlertDialog.Builder(this@CardRecordActivity).setMessage(message).setNegativeButton("删除当前记录") { dialog, which ->
                        dataContext.deleteBillRecord(billRecord.id)
                        childInfos[groupPosition].removeAt(childPosition)
                        expandableListAdapter.notifyDataSetChanged()
                        header.removeAllViews()
                        header.addView(getCardInfo(creditCard.getCardType()))
                        isChanged = true
                        dialog.dismiss()
                    }.setPositiveButton("取消") { dialog, which -> dialog.dismiss() }.show()
                }
                false
            }
        } catch (e: Exception) {
            _Utils.printException(this, e)
        }
    }//
    //
    /**
     * 账单日：8号
     * 2016-01-05  >=2015-01-09   <2016-01-09
     * 2015-12-23  >=2015-01-09   <2016-01-09
     * 2016-02-23  >=2016-01-09   <2017-01-09
     */
    private val listData: Unit
        private get() {
            try {
                val today = today
                var startDateTime: DateTime? = null
                var endDateTime: DateTime? = null
                var year = today.year

                /**
                 * 账单日：8号
                 * 2016-01-05  >=2015-01-09   <2016-01-09
                 * 2015-12-23  >=2015-01-09   <2016-01-09
                 * 2016-02-23  >=2016-01-09   <2017-01-09
                 */
                val xxx = DateTime(today.year, 0, creditCard.billDay)
                xxx.add(Calendar.DAY_OF_MONTH, 1)
                if (today.timeInMillis < xxx.timeInMillis) {
                    year = today.year - 1
                }
                startDateTime = DateTime(year, 0, creditCard.billDay)
                endDateTime = DateTime(year + 1, 0, creditCard.billDay)
                startDateTime.add(Calendar.DAY_OF_MONTH, 1)
                endDateTime.add(Calendar.DAY_OF_MONTH, 1)
                records = dataContext.getBillRecords(cardNumber, startDateTime, endDateTime)
                childInfos = ArrayList()
                groupInfos = ArrayList()
                var month = today.month
                if (year != today.year) month = 11
                for (i in month downTo 0) {
                    startDateTime = DateTime(year, i, creditCard.billDay)
                    startDateTime.add(Calendar.DAY_OF_MONTH, 1)
                    endDateTime = if (i == 11) DateTime(year + 1, 0, creditCard.billDay) else DateTime(year, i + 1, creditCard.billDay)
                    endDateTime.add(Calendar.DAY_OF_MONTH, 1)
                    var money = 0.0
                    //
                    val billRecordList = dataContext.getBillRecords(cardNumber, startDateTime, endDateTime)
                    Collections.sort(billRecordList, SortByDateTimeDESC())
                    childInfos.add(billRecordList)
                    //
                    endDateTime.add(Calendar.DAY_OF_MONTH, -1)
                    for (re in billRecordList) {
                        money += re.money
                    }
                    groupInfos.add(GroupInfo(i + 1, startDateTime.monthStr + "." + startDateTime.dayStr + " - " + endDateTime.monthStr + "." + endDateTime.dayStr,
                            money, "消费"))
                }
                Collections.sort(groupInfos, SortByMonthDESC())
            } catch (e: Exception) {
                _Utils.printException(this@CardRecordActivity, e)
            }
        }

    private fun getCardInfo(cardType: CardType): View? {
        try {
            val card: CreditCardForView = CreditCardForView(creditCard)
            var convertView: View? = null
            return if (cardType == CardType.储蓄卡) {
                convertView = View.inflate(this, R.layout.inflate_card_deposit_header, null)
                val textView_name = convertView.findViewById<View>(R.id.tv_name) as TextView // 账户名
                val textView_number = convertView.findViewById<View>(R.id.tv_number) as TextView // 卡号
                val textView_cardType = convertView.findViewById<View>(R.id.textView_cardType) as TextView // 卡类型
                val textView_bankName = convertView.findViewById<View>(R.id.textView_bankName) as TextView // 银行
                val textView_monthConsume = convertView.findViewById<View>(R.id.textView_monthConsume) as TextView
                val textView_yearConsume = convertView.findViewById<View>(R.id.textView_yearConsume) as TextView
                val textView_balance = convertView.findViewById<View>(R.id.textView_balance) as TextView
                textView_name.text = card.cardName
                textView_number.text = _String.formatToCardNumber(card.cardNumber)
                textView_cardType.text = card.cardType.toString()
                textView_bankName.text = card.bankName
                textView_monthConsume.text = card.currentBill
                textView_yearConsume.text = card.futureBill
                textView_balance.text = card.balance
                convertView
            } else if (card.cardType == CardType.微粒贷) {
                convertView = View.inflate(this, R.layout.inflate_header_wld_card, null)
                val textView_name = convertView.findViewById<View>(R.id.tv_name) as TextView // 账户名
                val textView_number = convertView.findViewById<View>(R.id.tv_number) as TextView // 卡号
                val textView_cardType = convertView.findViewById<View>(R.id.textView_cardType) as TextView // 卡类型
                val textView_bankName = convertView.findViewById<View>(R.id.textView_bankName) as TextView // 银行
                val textView_date = convertView.findViewById<View>(R.id.tv_att_date) as TextView //
                val textView_money = convertView.findViewById<View>(R.id.textView_money) as TextView //
                val textView_days = convertView.findViewById<View>(R.id.textView_days) as TextView //
                val textView_fee = convertView.findViewById<View>(R.id.textView_fee) as TextView //
                textView_name.text = card.bankName
                textView_number.text = card.cardNumber
                textView_cardType.text = card.cardType.toString()
                textView_date.text = card.repayDate
                textView_bankName.text = card.bankName
                textView_money.text = card.currentBill
                textView_days.text = card.repayDays
                textView_fee.text = card.futureBill
                convertView
            } else {
                convertView = View.inflate(this, R.layout.inflate_card_credit_header, null)
                val textView_name = convertView.findViewById<View>(R.id.tv_name) as TextView // 账户名
                val textView_number = convertView.findViewById<View>(R.id.tv_number) as TextView // 卡号
                val textView_cardType = convertView.findViewById<View>(R.id.textView_cardType) as TextView // 卡类型
                val textView_bankName = convertView.findViewById<View>(R.id.textView_bankName) as TextView // 银行
                val textView_repayDays = convertView.findViewById<View>(R.id.textView_repayDays) as TextView // 还款天数
                val textView_repayDate = convertView.findViewById<View>(R.id.textView_repayDate) as TextView // 还款日期
                val textView_billDays = convertView.findViewById<View>(R.id.textView_billDays) as TextView // 账单天数
                val textView_billDate = convertView.findViewById<View>(R.id.textView_billDate) as TextView // 账单日期
                val textView_restLine = convertView.findViewById<View>(R.id.textView_restLine) as TextView // 剩余额度
                val textView_currentBill = convertView.findViewById<View>(R.id.textView_currentBill) as TextView // 本期账单
                val textView_futureBill = convertView.findViewById<View>(R.id.textView_futureBill) as TextView // 下期账单
                val textView_foiDays = convertView.findViewById<View>(R.id.textView_foiDays) as TextView //  免息天数
                val textView_maxFoiDays = convertView.findViewById<View>(R.id.textView_maxFoiDays) as TextView //  免息天数
                val imageView_edit = convertView.findViewById<View>(R.id.imageView_edit) as ImageView
                imageView_edit.setOnClickListener {
                    val intent = Intent(this@CardRecordActivity, EditCardActivity::class.java)
                    intent.putExtra("cardNumber", cardNumber)
                    startActivityForResult(intent, TO_EDIT_CARD)
                }
                textView_name.text = card.cardName
                textView_number.text = _String.formatToCardNumber(card.cardNumber)
                textView_cardType.text = card.cardType.toString()
                textView_bankName.text = card.bankName
                textView_repayDays.text = card.repayDays
                try {
                    if (card.repayDays!!.contains("超") || card.repayDays!!.toInt() <= WARN_DAYS) {
                        textView_repayDays.setTextColor(Color.RED)
                        textView_repayDate.setTextColor(Color.RED)
                        (convertView.findViewById<View>(R.id.textView002) as TextView).setTextColor(Color.RED)
                    }
                } catch (e: NumberFormatException) {
                }
                textView_repayDate.text = card.repayDate
                textView_billDays.text = card.billDays
                textView_billDate.text = card.billDate
                textView_restLine.text = card.balance
                textView_currentBill.text = card.currentBill
                textView_futureBill.text = card.futureBill
                textView_foiDays.text = card.foiDays
                textView_maxFoiDays.text = card.maxFoiDays
                convertView
            }
        } catch (e: Exception) {
            _Utils.printException(this, e)
        }
        return null
    }

    internal inner class CreditCardForView(card: CreditCard) {
        var forSort //
                = 0
        var cardName //
                : String? = null
        var cardNumber //
                : String? = null
        var cardType //
                : CardType? = null
        var bankName //
                : String? = null
        var repayDays: String? = null
        var repayDate: String? = null
        var billDays: String? = null
        var billDate: String? = null
        var balance // 如果是储蓄卡为余额，如果是信用卡为可用额度。
                : String? = null
        var currentBill // 如果是储蓄卡为当月消费，如果是信用卡为本期账单。
                : String? = null
        var futureBill // 如果是储蓄卡为本年消费，如果是信用卡为下期账单。
                : String? = null
        var foiDays: String? = null
        var maxFoiDays: String? = null

        init {
            try {
                val format = DecimalFormat("#,##0.00")
                cardName = card.cardName
                this.cardNumber = card.cardNumber
                cardType = card.cardType
                bankName = card.bankName
                balance = format.format(card.balance)
                var today = today
                if (card.cardType == CardType.储蓄卡) {
                    var startDateTime = DateTime(today.year, 1, 1)
                    var endDateTime = DateTime(today.year + 1, 1, 1)
                    val billRecordList1 = dataContext.getBillRecords(card.cardNumber, startDateTime, endDateTime)
                    var money1 = 0.0
                    for (billRecord in billRecordList1) {
                        money1 += billRecord.money
                    }
                    startDateTime = DateTime(today.year, today.month, 1)
                    endDateTime = startDateTime.addMonths(1)
                    val billRecordList2 = dataContext.getBillRecords(card.cardNumber, startDateTime, endDateTime)
                    var money2 = 0.0
                    for (billRecord in billRecordList2) {
                        money2 += billRecord.money
                    }
                    currentBill = format.format(money2)
                    futureBill = format.format(money1)
                    forSort = 10001
                } else if (card.cardType == CardType.微粒贷) {
                    val billRecordList = dataContext.getBillRecords(card.cardNumber, true)
                    var money = 0.0
                    var fee = 0.0
                    var days = 0
                    var dt = DateTime.today
                    for (billRecord in billRecordList) {
                        money += billRecord.balance
                        repayDate = billRecord.dateTime.toShortDateString()
                        val dateTime = billRecord.dateTime
                        //
                        if (dateTime.timeInMillis < dt.timeInMillis) {
                            dt = dateTime
                        }
                        //
                        val xxxDateTime = DateTime(dateTime.year, dateTime.month, dateTime.day)
                        var xxxdays = ((today.timeInMillis - xxxDateTime.timeInMillis) / 3600 / 1000 / 24).toInt()
                        if (xxxdays > days) days = xxxdays
                        if (xxxdays == 0) xxxdays = 1
                        //
                        fee += xxxdays * billRecord.balance * 0.0005
                    }
                    repayDate = dt.toShortDateString()
                    repayDays = days.toString() + ""
                    currentBill = format.format(money)
                    futureBill = format.format(fee)
                } else {
                    var xxxRepayDate: DateTime? = null
                    val result = CreditCardHelper.getCreditCardResult(this@CardRecordActivity, card)
                    // 已出剩余账单、未出剩余账单
                    val remanentCurrent = result.consumeTotalCurrent-result.repayTotalNext
                    val remanentFutue = result.consumeTotalNext

                    //
                    today = DateTime.today
                    if (remanentCurrent > 0) {
                        xxxRepayDate = DateTime(result.endBillDateCurrent.year, result.endBillDateCurrent.month, card.billDay)
                    } else if (remanentFutue > 0) {
                        xxxRepayDate = DateTime(result.endBillDateNext.year, result.endBillDateNext.month, card.billDay)
                    }
                    if (xxxRepayDate != null) {
                        if (card.repayType == RepayType.相对账单日) {
                            xxxRepayDate.add(Calendar.DAY_OF_MONTH, card.repayDay)
                        } else {
                            if (card.repayDay < card.billDay) xxxRepayDate.add(Calendar.MONTH, 1)
                            xxxRepayDate[Calendar.DAY_OF_MONTH] = card.repayDay
                        }
                    }

                    //
                    if (xxxRepayDate == null) {
                        repayDays = "--"
                        repayDate = "--"
                        forSort = 10000
                    } else {
                        if (xxxRepayDate.date == today) {
                            repayDays = "今"
                            forSort = 0
                        } else {
                            forSort = ((xxxRepayDate.timeInMillis - today.timeInMillis) / 60000 / 60 / 24).toInt()
                            if (forSort < 0) {
                                repayDays = "超" + forSort * -1
                            } else {
                                repayDays = forSort.toString() + ""
                            }
                        }
                        repayDate = xxxRepayDate.toShortDateString()
                    }
                    val a = (result.billDateCurrent.timeInMillis - DateTime.today.timeInMillis) / 60000 / 60 / 24
                    billDays = if (a == 0L) "今" else a.toString() + ""
                    billDate = result.billDateCurrent.toShortDateString()
                    currentBill = format.format((result.consumeTotalCurrent - result.repayTotalNext).toLong())
                    futureBill = format.format(result.consumeTotalNext)
                    foiDays = ((result.endBillDateNext.timeInMillis - DateTime.today.timeInMillis) / 60000 / 60 / 24 + card.repayDay).toString() + "天"
                    maxFoiDays = ((result.endBillDateNext.timeInMillis - result.startBillDateNext.timeInMillis + 1) / 60000 / 60 / 24 + card.repayDay).toString() + "天"
                }
            } catch (e: Exception) {
                _Utils.printException(this@CardRecordActivity, e)
            }
        }
    }

    internal inner class CardRecordListAdapter : BaseExpandableListAdapter() {
        override fun getGroupCount(): Int {
            return groupInfos.size
        }

        override fun getGroup(groupPosition: Int): Any {
            return groupInfos[groupPosition]
        }

        override fun getGroupId(groupPosition: Int): Long {
            return groupPosition.toLong()
        }

        override fun getChildrenCount(groupPosition: Int): Int {
            return childInfos[groupPosition].size
        }

        override fun getChild(groupPosition: Int, childPosition: Int): Any {
            return childInfos[groupPosition][childPosition]
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long {
            return childPosition.toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getGroupView(groupPosition: Int, isExpanded: Boolean,
                                  convertView: View?, parent: ViewGroup): View? {
            var convertView = convertView
            try {
                convertView = View.inflate(this@CardRecordActivity, R.layout.inflate_list_card_record_group, null)
                val textView_month = convertView.findViewById<View>(R.id.textView_month) as TextView
                val textView_dateSpan = convertView.findViewById<View>(R.id.textView_dateSpan) as TextView
                val textView_money = convertView.findViewById<View>(R.id.textView_money) as TextView
                val textView_item = convertView.findViewById<View>(R.id.tv_item) as TextView
                val info = groupInfos[groupPosition]
                textView_month.text = info.month.toString() + "月"
                textView_dateSpan.text = info.dateSpan
                val format = DecimalFormat("#,##0.00")
                textView_money.text = format.format(info.money)
                textView_item.text = info.item
            } catch (e: Exception) {
                _Utils.printException(this@CardRecordActivity, e)
            }
            return convertView
        }

        override fun getChildView(groupPosition: Int, childPosition: Int,
                                  isLastChild: Boolean, convertView: View?, parent: ViewGroup): View? {
            var convertView = convertView
            try {
                convertView = View.inflate(this@CardRecordActivity, R.layout.inflate_list_card_record_child, null)
                val textView_summary = convertView.findViewById<View>(R.id.textView_summary) as TextView
                val textView_use = convertView.findViewById<View>(R.id.textView_use) as TextView
                val textView_money = convertView.findViewById<View>(R.id.textView_money) as TextView
                val textView_time = convertView.findViewById<View>(R.id.textView_time) as TextView
                val billRecord = childInfos[groupPosition][childPosition]
                textView_summary.text = billRecord.summary
                textView_use.text = billRecord.currency
                val format = DecimalFormat("#,##0.00")
                textView_money.text = format.format(billRecord.money)
                textView_time.text = billRecord.dateTime.toShortDateString()
            } catch (e: Exception) {
                _Utils.printException(this@CardRecordActivity, e)
            }
            return convertView
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
            return true
        }
    }

    internal inner class SortByMonthDESC : Comparator<Any> {
        override fun compare(o1: Any, o2: Any): Int {
            val gi1 = o1 as GroupInfo
            val gi2 = o2 as GroupInfo
            return gi2.month - gi1.month
        }
    }

    internal inner class SortByDateTimeDESC : Comparator<Any> {
        override fun compare(o1: Any, o2: Any): Int {
            val br1 = o1 as BillRecord
            val br2 = o2 as BillRecord
            return (br2.dateTime.timeInMillis - br1.dateTime.timeInMillis).toInt()
        }
    }

    internal inner class GroupInfo(var month: Int, var dateSpan: String, var money: Double, var item: String)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            if (requestCode == TO_EDIT_CARD) {
                if (EditCardActivity.isChanged) {
                    isChanged = true
                    header.removeAllViews()
                    header.addView(getCardInfo(creditCard.cardType))
                }
            }
        } catch (e: Exception) {
            _Utils.printException(this@CardRecordActivity, e)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackButtonClickListener() {
        finish()
    }

    companion object {
        @JvmField
        var isChanged = false
        private const val WARN_DAYS = 7
        const val TO_EDIT_CARD = 1
    }
}