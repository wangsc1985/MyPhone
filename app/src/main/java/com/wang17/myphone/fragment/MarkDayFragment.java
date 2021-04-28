package com.wang17.myphone.fragment;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wang17.myphone.R;
import com.wang17.myphone.activity.MarkDayRecordActivity;
import com.wang17.myphone.callback.CloudCallback;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.database.DayItem;
import com.wang17.myphone.database.MarkDay;
import com.wang17.myphone.database.Setting;
import com.wang17.myphone.database.DataContext;
import com.wang17.myphone.util._CloudUtils;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.util._Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * A simple {@link Fragment} subclass.
 */
public class MarkDayFragment extends Fragment {

    private static final String _TAG = "wangsc";

    private Button buttonAdd;
    private View view;
    private ListView listView;
    private List<ListItemData> listItemDatas;
    private DataContext dataContext;
    private DayItemListAdapter listAdapter;
    private UUID markdayFocusedId;

//    @Override
//    protected void lazyLoad() {
//
//        try {
//            if (!isPrepared || !isVisible || isHaveLoaded) {
//                return;
//            }
//
//            listView.setAdapter(listAdapter);
//
//
//            isHaveLoaded = true;
//        } catch (Exception e) {
//            Log.e("wangsc", e.getMessage());
//        }
//    }

    class ListItemData {
        public DayItem dayItem;
        public int havePassedInHour;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            dataContext = new DataContext(getContext());
            listItemDatas = new ArrayList<>();
            markdayFocusedId = UUID.fromString(dataContext.getSetting(Setting.KEYS.mark_day_focused, _Session.UUID_NULL).getString());
            listAdapter = new DayItemListAdapter();
        } catch (Exception e) {
            Log.e(_TAG,e.getMessage());
            throw e ;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_sexual_day, container, false);

        listView = view.findViewById(R.id.listView_sexualDays);


        FloatingActionButton actionButton =  view.findViewById(R.id.floating_add);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addDayItemDialog();
            }
        });

        listView.setAdapter(listAdapter);
//        isPrepared = true;
//        lazyLoad();
        return view;
    }

    @Override
    public void onStart() {
        getListDatas();
        listAdapter.notifyDataSetChanged();
        super.onStart();
    }

    private void getListDatas() {
        listItemDatas.clear();
        List<DayItem> dayItems = dataContext.getDayItems(true);
        for (int i = 0; i < dayItems.size(); i++) {

            ListItemData did = new ListItemData();
            did.dayItem = dayItems.get(i);

            MarkDay lastMarkDay = dataContext.getLastMarkDay(did.dayItem.getId());

            if (lastMarkDay == null) {
                did.havePassedInHour = 0;
            } else {
                did.havePassedInHour = (int) ((new DateTime().getTimeInMillis() - lastMarkDay.getDateTime().getTimeInMillis()) / 3600000);
                switch (did.dayItem.getSummary()){

                    case "天":
                        did.havePassedInHour = DateTime.dayOffset(lastMarkDay.getDateTime(), new DateTime())*24;
                        break;
                    case "当天":
                        did.havePassedInHour = (DateTime.dayOffset(lastMarkDay.getDateTime(), new DateTime())+1)*24;
                        break;
                }
            }
            listItemDatas.add(did);
        }
    }

    public void editDayItemDialog(final DayItem dayItem) {

        try {
            View view = View.inflate(getContext(), R.layout.inflate_add_day_item, null);
            final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext()).setView(view).create();
            dialog.setTitle("编辑项目");


            final EditText editName =  view.findViewById(R.id.edit_name);
            final EditText editTarget =  view.findViewById(R.id.edit_targetInDay);
            final EditText editSummary =  view.findViewById(R.id.edit_summary);
            Button buttonAdd =  view.findViewById(R.id.button_add);
            Button buttonCancel =  view.findViewById(R.id.button_cancel);

            editName.setText(dayItem.getName());
            editSummary.setText(dayItem.getSummary());
            editTarget.setText(dayItem.getTargetInHour() / 24 + "");
            editName.setSelectAllOnFocus(true);
            editSummary.setSelectAllOnFocus(true);
            editTarget.setSelectAllOnFocus(true);

            buttonAdd.setText("编辑");
            buttonAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String name = editName.getText().toString();
                    int targetInHour = Integer.parseInt(editTarget.getText().toString()) * 24;
                    String summary = editSummary.getText().toString();

                    dayItem.setName(name);
                    dayItem.setTargetInHour(targetInHour);
                    dayItem.setSummary(summary);
                    dataContext = new DataContext(getContext());
                    dataContext.editDayItem(dayItem);

                    getListDatas();
                    listAdapter.notifyDataSetChanged();

                    dialog.dismiss();
                }
            });
            buttonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });


            dialog.show();
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }


    }

    public void addDayItemDialog() {

        View view = View.inflate(getContext(), R.layout.inflate_add_day_item, null);
        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext()).setView(view).create();
        dialog.setTitle("添加项目");


        final EditText editName =  view.findViewById(R.id.edit_name);
        final EditText editTarget =  view.findViewById(R.id.edit_targetInDay);
        final EditText editSummary =  view.findViewById(R.id.edit_summary);
        Button buttonAdd =  view.findViewById(R.id.button_add);
        Button buttonCancel =  view.findViewById(R.id.button_cancel);

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editName.getText().toString();
                int targetInHour = Integer.parseInt(editTarget.getText().toString()) * 24;
                String summary = editSummary.getText().toString();

                DayItem item = new DayItem(name, summary, targetInHour);
                DataContext dataContext = new DataContext(getContext());
                dataContext.addDayItem(item);

                getListDatas();
                listAdapter.notifyDataSetChanged();

                dialog.dismiss();
            }
        });
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });


        dialog.show();


    }

    protected class DayItemListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return listItemDatas.size();
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
        public View getView(final int position, View convertView, ViewGroup parent) {
            try {
                convertView = View.inflate(getContext(), R.layout.inflate_list_item_mark_day, null);

                final ListItemData listItemData = listItemDatas.get(position);
                final ConstraintLayout layoutRoot =  convertView.findViewById(R.id.layout_root);
                ImageView imageView_mark =  convertView.findViewById(R.id.imageView_mark);
                TextView textView_name =  convertView.findViewById(R.id.tv_name);
                TextView textView_interval =  convertView.findViewById(R.id.textView_interval);
                TextView textView_remaider = convertView.findViewById(R.id.textView_remaider);
                TextView textView_target =  convertView.findViewById(R.id.textView_target);
                TextView textView_targetDate =  convertView.findViewById(R.id.textView_targetDate);
                ProgressBar progressBar =  convertView.findViewById(R.id.progressBar);
                progressBar.setMax(listItemData.dayItem.getTargetInHour());
                textView_name.setText(listItemData.dayItem.getName());
                String havePassedStr = DateTime.toSpanString((long) listItemData.havePassedInHour * 3600000, 4, 3);
                String remainderStr = DateTime.toSpanString((long) (listItemData.dayItem.getTargetInHour() - listItemData.havePassedInHour) * 3600000, 4, 3);
                String targetStr = DateTime.toSpanString((long) listItemData.dayItem.getTargetInHour() * 3600000, 4, 3);

                DateTime targetDate = new DateTime(new DateTime().getTimeInMillis() + (long) (listItemData.dayItem.getTargetInHour() - listItemData.havePassedInHour) * 3600000);



                switch (listItemData.dayItem.getSummary()){

                    case "天":
                        havePassedStr = listItemData.havePassedInHour/24 + "天";
                        int remainder = listItemData.dayItem.getTargetInHour() / 24 - listItemData.havePassedInHour/24;
                        remainderStr = (remainder < 0 ? "+" + (remainder * -1) : remainder) + "天";
                        targetDate = new DateTime(System.currentTimeMillis() + (long) (listItemData.dayItem.getTargetInHour() - listItemData.havePassedInHour) * 3600000);
                        break;
                    case "当天":
                        havePassedStr = listItemData.havePassedInHour/24 + "天";
                        remainder = listItemData.dayItem.getTargetInHour() / 24 - listItemData.havePassedInHour/24;
                        remainderStr = (remainder < 0 ? "+" + (remainder * -1) : remainder) + "天";
                        targetDate = new DateTime(System.currentTimeMillis() + (long) (listItemData.dayItem.getTargetInHour() - listItemData.havePassedInHour) * 3600000);
                        break;
                }


                textView_interval.setText(havePassedStr);
                textView_remaider.setText(remainderStr);
                textView_target.setText("（ " + targetStr + " ）");
                textView_targetDate.setText(targetDate.toShortDateString());
                progressBar.setProgress(listItemData.havePassedInHour);
                if (listItemData.dayItem.getId().equals(markdayFocusedId)) {
                    imageView_mark.setVisibility(View.VISIBLE);
                } else {
                    imageView_mark.setVisibility(View.INVISIBLE);
                }
                layoutRoot.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(getContext()).setItems(new String[]{"进入", "编辑", "删除", "主项"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        Intent intent = new Intent(getActivity(), MarkDayRecordActivity.class);
                                        intent.putExtra("id", listItemData.dayItem.getId().toString());
                                        startActivityForResult(intent, 0);
                                        break;
                                    case 1:
                                        editDayItemDialog(listItemData.dayItem);
                                        break;
                                    case 2:
                                        new AlertDialog.Builder(getContext()).setMessage("确定要删除此项目及其所有记录吗？").setNegativeButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dataContext.deleteDayItem(listItemData.dayItem.getId());
                                                dataContext.deleteMarkDay(listItemData.dayItem.getId());
                                                getListDatas();
                                                listAdapter.notifyDataSetChanged();
                                            }
                                        }).setPositiveButton("取消", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        }).show();

                                        break;
                                    case 3:
                                        dataContext.editSetting(Setting.KEYS.mark_day_focused, listItemData.dayItem.getId());
                                        markdayFocusedId = listItemData.dayItem.getId();
                                        listAdapter.notifyDataSetChanged();
                                        break;
                                    case 4:
                                        addDayItemDialog();
                                        break;
                                }
                            }
                        }).show();

                    }
                });
                layoutRoot.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        setDateTimeDialog(new DateTime(), listItemData.dayItem);
                        return true;
                    }
                });
            } catch (Exception e) {
                _Utils.printException(getContext(), e);
            }
            return convertView;
        }
    }

    public void setDateTimeDialog(DateTime dateTime, final DayItem item) {

        try {
            View view = View.inflate(getContext(), R.layout.inflate_dialog_date_picker, null);
            final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext()).setView(view).create();
            dialog.setTitle("设定最近一次" + item.getName() + "时间");

            final int year = dateTime.getYear();
            int month = dateTime.getMonth();
            int maxDay = dateTime.getActualMaximum(Calendar.DAY_OF_MONTH);
            int day = dateTime.getDay();
            int hour = dateTime.getHour();

            String[] yearNumbers = new String[3];
            for (int i = year - 2; i <= year; i++) {
                yearNumbers[i - year + 2] = i + "年";
            }
            String[] monthNumbers = new String[12];
            for (int i = 0; i < 12; i++) {
                monthNumbers[i] = i + 1 + "月";
            }
            String[] dayNumbers = new String[31];
            for (int i = 0; i < 31; i++) {
                dayNumbers[i] = i + 1 + "日";
            }
            String[] hourNumbers = new String[24];
            for (int i = 0; i < 24; i++) {
                hourNumbers[i] = i + "点";
            }
            final NumberPicker npYear = (NumberPicker) view.findViewById(R.id.npYear);
            final NumberPicker npMonth = (NumberPicker) view.findViewById(R.id.npMonth);
            final NumberPicker npDay = (NumberPicker) view.findViewById(R.id.npDay);
            final NumberPicker npHour = (NumberPicker) view.findViewById(R.id.npHour);
            npYear.setMinValue(year - 2);
            npYear.setMaxValue(year);
            npYear.setValue(year);
            npYear.setDisplayedValues(yearNumbers);
            npYear.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
            npMonth.setMinValue(1);
            npMonth.setMaxValue(12);
            npMonth.setDisplayedValues(monthNumbers);
            npMonth.setValue(month + 1);
            npMonth.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
            npDay.setMinValue(1);
            npDay.setMaxValue(31);
            npDay.setDisplayedValues(dayNumbers);
            npDay.setValue(day);
            npDay.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
            npHour.setMinValue(0);
            npHour.setMaxValue(23);
            npHour.setDisplayedValues(hourNumbers);
            npHour.setValue(hour);
            npHour.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中

            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        int y = npYear.getValue();
                        int m = npMonth.getValue() - 1;
                        int d = npDay.getValue();
                        int h = npHour.getValue();
                        DateTime selectedDateTime = new DateTime(y, m, d, h, 0, 0);
                        MarkDay markDay = new MarkDay(selectedDateTime, item.getId(), "");
                        dataContext.addMarkDay(markDay);


                        if(item.getName().equals("戒期")){
                            _CloudUtils.saveSetting(getContext(), dataContext.getSetting(Setting.KEYS.wx_request_code, "0000").getString(), Setting.KEYS.wx_sex_date.toString(), selectedDateTime.getTimeInMillis(), new CloudCallback() {
                                @Override
                                public void excute(int code, Object result) {
                                    switch (code){
                                        case 0:
                                            e(result);
                                            break;
                                        case 1:
                                            e(result);
                                            break;
                                        case -1:
                                            e(result);
                                            break;
                                        case -2:
                                            e(result);
                                            break;
                                    }
                                    Looper.prepare();
                                    Toast.makeText(getContext(),"更新完毕",Toast.LENGTH_LONG).show();
                                    Looper.loop();
                                }
                            });
                        }


                        getListDatas();
                        listAdapter.notifyDataSetChanged();

                        dialog.dismiss();
                    } catch (Exception e) {
                        _Utils.printException(getContext(), e);
                    }
                }
            });
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        dialog.dismiss();
                    } catch (Exception e) {
                        _Utils.printException(getContext(), e);
                    }
                }
            });
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void e(Object log) {
        Log.e("wangsc",log.toString());
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        getListDatas();
        listAdapter.notifyDataSetChanged();
        super.startActivityForResult(intent, requestCode);
    }
}
