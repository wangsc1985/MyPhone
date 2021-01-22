package com.wang17.myphone.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.fragment.ActionBarFragment;
import com.wang17.myphone.fragment.AddTallyFragment;
import com.wang17.myphone.model.database.BuddhaRecord;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.util._String;
import com.wang17.myphone.model.DateTime;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BuddhaActivity extends AppCompatActivity implements ActionBarFragment.OnActionFragmentBackListener {

    // 视图变量
    FloatingActionButton floatingAdd;
    // 类变量
    private DataContext dataContext;
    private BaseExpandableListAdapter recordListdAdapter;

    // 值变量
    private List<GroupInfo> groupList;
    private List<List<ChildInfo>> childListList;
    private static final int TO_TALLAY_RECORD_DETAIL_ACTIVITY = 54;
    public static boolean isChanged = false;
    int year;
    long duration;
    int count;
    int number;
    TextView tv_year;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_buddha);
            dataContext = new DataContext(this);
//            tallyRecords = dataContext.getRecords(DateTime.getToday().getYear());

            year = DateTime.getToday().getYear();
            tv_year = findViewById(R.id.tv_year);
            ImageView iv_prev = findViewById(R.id.iv_prev);
            ImageView iv_next = findViewById(R.id.iv_next);
            iv_prev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    year--;
                    reflushData(year);
                    recordListdAdapter.notifyDataSetChanged();
                }
            });
            iv_next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    year++;
                    reflushData(year);
                    recordListdAdapter.notifyDataSetChanged();
                }
            });

            reflushData(year);

            final ExpandableListView listView_records = (ExpandableListView) findViewById(R.id.listView_records);
            listView_records.setGroupIndicator(null);
            recordListdAdapter = new BuddhaListAdapter();
            listView_records.setAdapter(recordListdAdapter);
            // 默认展开第一组
            listView_records.expandGroup(0);
            // 单组展开，不能多组同时展开。
            listView_records.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {

                @Override
                public void onGroupExpand(int groupPosition) {
                    for (int i = 0, count = listView_records.getExpandableListAdapter().getGroupCount(); i < count; i++) {
                        if (groupPosition != i) {// 关闭其他分组
                            listView_records.collapseGroup(i);
                        }
                    }
                }
            });

            listView_records.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                    long start = childListList.get(groupPosition).get(childPosition).start;
                    Intent intent = new Intent(BuddhaActivity.this, BuddhaDetailActivity.class);
                    intent.putExtra("start", start);
                    startActivityForResult(intent, TO_TALLAY_RECORD_DETAIL_ACTIVITY);
                    return true;
                }
            });
            floatingAdd = (FloatingActionButton)findViewById(R.id.floating_add);
            floatingAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AddTallyFragment addTallyFragment = new AddTallyFragment();
                    addTallyFragment.setAfterAddRecordListener(new AddTallyFragment.AfterAddRecordListener() {
                        @Override
                        public void addRecode() {
//                    tallyRecords = dataContext.getRecords(DateTime.getToday().getYear());
                            reflushData(year);
                            recordListdAdapter.notifyDataSetChanged();
                            recordListdAdapter.notifyDataSetChanged();
                            isChanged = true;
                        }
                    });
                    addTallyFragment.show(getSupportFragmentManager(),"添加念佛记录");


                }
            });
        } catch (Exception e) {
            _Utils.printException(this, e);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TO_TALLAY_RECORD_DETAIL_ACTIVITY) {
            if (BuddhaDetailActivity.isChanged) {
//                tallyRecords = dataContext.getRecords(DateTime.getToday().getYear());
                reflushData(year);
                recordListdAdapter.notifyDataSetChanged();
                isChanged = true;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void reflushData(int year) {
        try {
            count=0;
            number=0;
            duration=0L;

            groupList = new ArrayList<>();
            childListList = new ArrayList<>();
            int month = -1;
            int day = -1;
            boolean isToday=true;
            GroupInfo groupNode = null;
            ChildInfo childNode = null;
            List<ChildInfo> childList = null;


            BuddhaRecord first = dataContext.getFirstBuddha();
            BuddhaRecord latest = dataContext.getLatestBuddha();
            if(first==null||latest==null){
                return;
            }
            DateTime end = new DateTime(year,0,1);
            DateTime date = new DateTime(year,11,31);
            if(end.getTimeInMillis()<first.getStartTime().getTimeInMillis()){
                end = first.getStartTime();
            }
            if(date.getTimeInMillis()>latest.getStartTime().getTimeInMillis()){
                date = latest.getStartTime();
            }
            Log.e("wangsc","start : "+end.toLongDateTimeString()+" end : "+date.toLongDateTimeString());

                while (date.getYear()==end.getYear()&& date.get(Calendar.DAY_OF_YEAR)>=end.get(Calendar.DAY_OF_YEAR)) {
                List<BuddhaRecord> records = dataContext.getBuddhas(date);
//                if(isToday&&records.size()==0){
//                    date = date.addDays(-1);
//                    continue;
//                }
                Log.e("wangsc","date day of year :"+date.get(Calendar.DAY_OF_YEAR) +" end day of year : "+end.get(Calendar.DAY_OF_YEAR));
//                isToday=false;

                if (date.getMonth() == month) {

                    day = date.getDay();
                    childNode = new ChildInfo();
                    childNode.day = day;
                    childNode.start = date.getTimeInMillis();

                    for (BuddhaRecord record : records) {
                        groupNode.duration += record.getDuration();
                        groupNode.count += record.getCount();
                        groupNode.number += record.getCount()*1080;
                        childNode.duration += record.getDuration();
                        childNode.count += record.getCount();
                        childNode.number += record.getCount()*1080;
                        duration+=record.getDuration();
                        count += record.getCount();
                        number += record.getCount()*1080;
                    }
                    childList.add(childNode);
                } else {
                    month = date.getMonth();
                    groupNode = new GroupInfo();
                    groupNode.month = month + 1;

                    //
                    childList = new ArrayList<>();
                    day = date.getDay();
                    childNode = new ChildInfo();
                    childNode.day = day;
                    childNode.start = date.getTimeInMillis();

                    for (BuddhaRecord record : records) {
                        groupNode.duration += record.getDuration();
                        groupNode.count += record.getCount();
                        groupNode.number += record.getCount()*1080;
                        childNode.duration += record.getDuration();
                        childNode.count += record.getCount();
                        childNode.number += record.getCount()*1080;
                        duration+=record.getDuration();
                        count += record.getCount();
                        number += record.getCount()*1080;
                    }
                    childList.add(childNode);


                    groupList.add(groupNode);
                    childListList.add(childList);
                }

                date = date.addDays(-1);
            }
            tv_year.setText(year+"年   " +count+"圈   "+ DateTime.toSpanString2(duration)+"   "+new DecimalFormat("0.00").format((double)number/10000)+"万");
        } catch (Exception e) {
            _Utils.printException(BuddhaActivity.this, e);
        }
    }

    class BuddhaListAdapter extends BaseExpandableListAdapter {
        @Override
        public int getGroupCount() {
            return groupList.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groupList.get(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return childListList.get(groupPosition).size();
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return childListList.get(groupPosition).get(childPosition);
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
                convertView = View.inflate(BuddhaActivity.this, R.layout.inflate_list_item_buddha_group, null);
                TextView textView_date =  convertView.findViewById(R.id.textView_date);
                TextView textView_duration = convertView.findViewById(R.id.textView_monthDuration);
                TextView textView_count =  convertView.findViewById(R.id.textView_monthCount);
                TextView textView_number = convertView.findViewById(R.id.textView_monthNumber);

                GroupInfo info = groupList.get(groupPosition);
                textView_date.setText(_String.format(info.month) + "月");
                textView_duration.setText(DateTime.toSpanString2(info.duration));
                textView_count.setText(info.count+"圈");
                textView_number.setText(new DecimalFormat("0.00").format((double)info.number/10000)+"万");

                if(info.duration==0){
                    textView_duration.setText("--");
                }
                if(info.number==0){
                    textView_number.setText("");
                }
                if(info.count==0){
                    textView_count.setText("");
                }
            } catch (Exception e) {
                _Utils.printException(BuddhaActivity.this, e);
            }
            return convertView;
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            try {
                convertView = View.inflate(BuddhaActivity.this, R.layout.inflate_list_item_buddha_child, null);
                TextView textView_date = (TextView) convertView.findViewById(R.id.textView_date);
                TextView textView_duration = (TextView) convertView.findViewById(R.id.textView_monthDuration);
                TextView textView_count = (TextView) convertView.findViewById(R.id.textView_monthCount);
                TextView textView_number = (TextView) convertView.findViewById(R.id.textView_monthNumber);

                final ChildInfo childInfo = childListList.get(groupPosition).get(childPosition);
                textView_date.setText(_String.format(childInfo.day));
                textView_count.setText(childInfo.count+"圈");
                textView_duration.setText(DateTime.toSpanString2(childInfo.duration));
                textView_number.setText(new DecimalFormat("#,##0").format(childInfo.number));
                TextView textViewItem =  convertView.findViewById(R.id.textView_item);
                textViewItem.setVisibility(View.GONE);
                if(childInfo.duration==0){
                    textView_duration.setText("--");
                }
                if(childInfo.number==0){
                    textView_number.setText("");
                }
                if(childInfo.count==0){
                    textView_count.setText("");
                }
            } catch (Exception e) {
                _Utils.printException(BuddhaActivity.this, e);
            }
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

    }


    class GroupInfo {
        public int month;
        public long number;
        public long duration;
        public int count;
    }

    class ChildInfo {
        public int day;
        public long start;
        public long number;
        public long duration;
        public int count;
    }

    @Override
    public void onBackButtonClickListener() {
        this.finish();
    }

    private void snackbar(String message) {
        try {
            Snackbar.make(floatingAdd, message, Snackbar.LENGTH_LONG).show();
        } catch (Exception e) {
            _Utils.printException(BuddhaActivity.this, e);
        }
    }
}
