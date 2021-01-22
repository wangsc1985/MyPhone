package com.wang17.myphone.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.fragment.ActionBarFragment;
import com.wang17.myphone.model.database.BuddhaRecord;
import com.wang17.myphone.model.database.Setting;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.model.DateTime;

import java.util.List;

public class BuddhaDetailActivity extends AppCompatActivity implements ActionBarFragment.OnActionFragmentBackListener {

    private ListView listView_records;
    public static boolean isChanged = false;
    private long start;

    private DataContext dataContext;
    private List<BuddhaRecord> records;
    private RecordListdAdapter recordListdAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buddha_detail);

        dataContext = new DataContext(BuddhaDetailActivity.this);
        listView_records = (ListView) findViewById(R.id.listView_records);

        start = getIntent().getLongExtra("start", 0);
        records = dataContext.getBuddhas(new DateTime(start));
        recordListdAdapter = new RecordListdAdapter();
        listView_records.setAdapter(recordListdAdapter);
        listView_records.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BuddhaRecord tallyRecord = records.get(position);
                int hour =(int)(tallyRecord.getDuration() / 1000 / 60 / 60);
                int min = (int)(tallyRecord.getDuration() / 1000 / 60);
                showIntervalDialog(hour, min, tallyRecord);
            }
        });
        listView_records.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(BuddhaDetailActivity.this).setMessage("确认要删除当前记录吗？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BuddhaRecord tallyRecord = records.get(position);
                        dataContext.deleteRecord(tallyRecord.getId());
                        isChanged = true;
                        //
                        records = dataContext.getBuddhas(new DateTime(start));
                        recordListdAdapter.notifyDataSetChanged();
                    }
                }).setNegativeButton("取消", null).show();
                return true;
            }
        });
    }

    @Override
    public void onBackButtonClickListener() {
        this.finish();
    }


    protected class RecordListdAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return records.size();
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
            final int index = position;
            try {
                convertView = View.inflate(BuddhaDetailActivity.this, R.layout.inflate_list_item_buddha_child, null);
                final BuddhaRecord tallyRecord = records.get(position);
                TextView textView_start = (TextView) convertView.findViewById(R.id.textView_date);
                textView_start.setText("" + tallyRecord.getStartTime().getHourStr() + "点" + tallyRecord.getStartTime().getMiniteStr() + "分");
                TextView textView_interval = (TextView) convertView.findViewById(R.id.textView_monthDuration);
                textView_interval.setText("" + DateTime.toSpanString(tallyRecord.getDuration(), 3, 2));
                TextView textViewItem = (TextView) convertView.findViewById(R.id.textView_item);
                textViewItem.setText((tallyRecord.getSummary() == null || tallyRecord.getSummary().isEmpty()) ? "默认" : tallyRecord.getSummary());
            } catch (Exception e) {
                _Utils.printException(BuddhaDetailActivity.this, e);
            }
            return convertView;
        }
    }


    public void showIntervalDialog(int xxxHour, int xxxMin, final BuddhaRecord tallyRecord) {
        View view = View.inflate(this, R.layout.inflate_dialog_edit_tally_record, null);
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(view);
        dialog.setTitle("设定念佛时间");


        String[] hourNumbers = new String[100];
        for (int i = 0; i < 100; i++) {
            hourNumbers[i] = i + "小时";
        }
        String[] minNumbers = new String[60];
        for (int i = 0; i < 60; i++) {
            minNumbers[i] = i + "分钟";
        }
        final NumberPicker number_hour = (NumberPicker) view.findViewById(R.id.number_hour);
        final NumberPicker number_min = (NumberPicker) view.findViewById(R.id.number_min);
        final EditText editTextItem = (EditText)view.findViewById(R.id.editText_item);
        number_hour.setMinValue(0);
        number_hour.setDisplayedValues(hourNumbers);
        number_hour.setMaxValue(99);
        number_hour.setValue(xxxHour);
        number_hour.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
        number_min.setMinValue(0);
        number_min.setDisplayedValues(minNumbers);
        number_min.setMaxValue(59);
        number_min.setValue(xxxMin);
        number_min.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
        editTextItem.setText((tallyRecord.getSummary()==null||tallyRecord.getSummary().isEmpty())?dataContext.getSetting(Setting.KEYS.tally_record_item_text,"").getString():tallyRecord.getSummary());


        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    int hour = number_hour.getValue();
                    int min = number_min.getValue();
                    int interval = hour * 60000 * 60 + min * 60000;
                    if (interval == 0) {
                        new AlertDialog.Builder(BuddhaDetailActivity.this).setMessage("确认要删除当前记录吗？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dataContext.deleteRecord(tallyRecord.getId());
                                isChanged = true;
                                //
                                records = dataContext.getBuddhas(new DateTime(start));
                                recordListdAdapter.notifyDataSetChanged();
                            }
                        }).setNegativeButton("取消", null).show();
                    } else {
                        tallyRecord.setDuration(interval);
                        tallyRecord.setSummary(editTextItem.getText().toString());
                        dataContext.editBuddha(tallyRecord);
                        isChanged = true;
                        //
                        records = dataContext.getBuddhas(new DateTime(start));
                        recordListdAdapter.notifyDataSetChanged();
                    }
                    dialog.dismiss();
                } catch (Exception e) {
                    _Utils.printException(BuddhaDetailActivity.this, e);
                }
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
