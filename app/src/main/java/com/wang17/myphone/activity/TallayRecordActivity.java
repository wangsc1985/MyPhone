package com.wang17.myphone.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.fragment.ActionBarFragment;
import com.wang17.myphone.fragment.AddTallyFragment;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.util._String;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.model.database.TallyRecord;

import java.util.ArrayList;
import java.util.List;

public class TallayRecordActivity extends AppCompatActivity implements ActionBarFragment.OnActionFragmentBackListener {

    // 视图变量
    RelativeLayout root;
    FloatingActionButton floatingAdd;
    // 类变量
    private DataContext dataContext;
    private List<TallyRecord> tallyRecords;
    private BaseExpandableListAdapter recordListdAdapter;

    // 值变量
    private List<GroupInfo> groupList;
    private List<List<ChildInfo>> childListList;
    private static final int TO_TALLAY_RECORD_DETAIL_ACTIVITY = 54;
    public static boolean isChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_tallay_record);
            dataContext = new DataContext(this);
            root = (RelativeLayout) findViewById(R.id.activity_tallay_record);
//            tallyRecords = dataContext.getRecords(DateTime.getToday().getYear());

            reflushData();

            final ExpandableListView listView_records = (ExpandableListView) findViewById(R.id.listView_records);
            recordListdAdapter = new TallyRecordListAdapter();
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
                    Intent intent = new Intent(TallayRecordActivity.this, TallayRecordDetailActivity.class);
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
                            reflushData();
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
            if (TallayRecordDetailActivity.isChanged) {
//                tallyRecords = dataContext.getRecords(DateTime.getToday().getYear());
                reflushData();
                recordListdAdapter.notifyDataSetChanged();
                isChanged = true;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void reflushData() {
        try {
            groupList = new ArrayList<>();
            childListList = new ArrayList<>();
            int month = -1;
            int day = -1;
            boolean isToday=true;
            GroupInfo groupNode = null;
            ChildInfo childNode = null;
            List<ChildInfo> childList = null;


            DateTime date = DateTime.getToday();
            DateTime end = date.addMonths(-2);
            while (( date.getTimeInMillis()-end.getTimeInMillis()) / 3600000 / 24 >= 0) {
                List<TallyRecord> records = dataContext.getRecords(date);
                if(isToday&&records.size()==0){
                    date = date.addDays(-1);
                    continue;
                }
                isToday=false;

                if (date.getMonth() == month) {

                    day = date.getDay();
                    childNode = new ChildInfo();
                    childNode.day = day;
                    childNode.start = date.getTimeInMillis();

                    for (TallyRecord record : records) {
                        groupNode.total += record.getInterval();
                        childNode.total += record.getInterval();
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

                    for (TallyRecord record : records) {
                        groupNode.total += record.getInterval();
                        childNode.total += record.getInterval();
                    }
                    childList.add(childNode);


                    groupList.add(groupNode);
                    childListList.add(childList);
                }

                date = date.addDays(-1);
            }

//
//            for (TallyRecord record : tallyRecords) {
//                if (record.getStart().getMonth() == month) {
//                    groupNode.total += record.getInterval();
//
//                    //
//                    if (record.getStart().getDay() == day) {
//                        childNode.total += record.getInterval();
//                    } else {
//                        day = record.getStart().getDay();
//                        childNode = new ChildInfo();
//                        childNode.day = day;
//                        childNode.start = record.getStart().getTimeInMillis();
//                        childNode.total += record.getInterval();
//                        childList.add(childNode);
//                    }
//                } else {
//                    month = record.getStart().getMonth();
//                    groupNode = new GroupInfo();
//                    groupNode.month = record.getStart().getMonth() + 1;
//                    groupNode.total += record.getInterval();
//                    groupList.add(groupNode);
//
//                    //
//                    childList = new ArrayList<>();
//                    day = record.getStart().getDay();
//                    childNode = new ChildInfo();
//                    childNode.day = day;
//                    childNode.start = record.getStart().getTimeInMillis();
//                    childNode.total += record.getInterval();
//                    childList.add(childNode);
//                    childListList.add(childList);
//                }
//            }
        } catch (Exception e) {
            _Utils.printException(TallayRecordActivity.this, e);
        }
    }

/*    private void reflushData() {
        groupList = new ArrayList<>();
        childListList = new ArrayList<>();
        DateTime date = new DateTime(2000, 0, 1);
        GroupInfo groupInfo = null;
        List<TallyRecord> child = null;
        for (TallyRecord record : tallyRecords) {
            if (record.getStart().getDate().getTimeInMillis() == date.getTimeInMillis()) {
                groupInfo.total += record.getInterval();
                child.add(record);
            } else {
                date = record.getStart().getDate();
                groupInfo = new GroupInfo();
                child = new ArrayList<>();
                groupInfo.date = date;
                groupInfo.total += record.getInterval();
                child.add(record);
                groupList.add(groupInfo);
                childListList.add(child);
            }
        }
    }*/


    class TallyRecordListAdapter extends BaseExpandableListAdapter {
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
                convertView = View.inflate(TallayRecordActivity.this, R.layout.inflate_list_item_tally_record_group, null);
                TextView textView_date = (TextView) convertView.findViewById(R.id.textView_date);
                TextView textView_total = (TextView) convertView.findViewById(R.id.textView_monthTotal);

                GroupInfo info = groupList.get(groupPosition);
                textView_date.setText(_String.format(info.month) + "月");
                textView_total.setText(DateTime.toSpanString(info.total, 3, 2));
            } catch (Exception e) {
                _Utils.printException(TallayRecordActivity.this, e);
            }
            return convertView;
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            try {
                convertView = View.inflate(TallayRecordActivity.this, R.layout.inflate_list_item_tally_record_child, null);
                TextView textView_date = (TextView) convertView.findViewById(R.id.textView_date);
                TextView textView_total = (TextView) convertView.findViewById(R.id.textView_monthTotal);

                final ChildInfo tallyRecord = childListList.get(groupPosition).get(childPosition);
                textView_date.setText(_String.format(tallyRecord.day));
                textView_total.setText(DateTime.toSpanString(tallyRecord.total, 3, 2));
                TextView textViewItem = (TextView) convertView.findViewById(R.id.textView_item);
                textViewItem.setVisibility(View.GONE);
                if(tallyRecord.total==0){
                    textView_total.setText("--");
                }
            } catch (Exception e) {
                _Utils.printException(TallayRecordActivity.this, e);
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
        public long total;
    }

    class ChildInfo {
        public int day;
        public long start;
        public long total;
    }

    @Override
    public void onBackButtonClickListener() {
        this.finish();
    }

    private void snackbar(String message) {
        try {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        } catch (Exception e) {
            _Utils.printException(TallayRecordActivity.this, e);
        }
    }
}
