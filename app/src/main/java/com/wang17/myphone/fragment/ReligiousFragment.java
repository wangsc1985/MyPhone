package com.wang17.myphone.fragment;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.model.ReligiousCallBack;
import com.wang17.myphone.util._AnimationUtils;
import com.wang17.myphone.util.CalendarHelper;
import com.wang17.myphone.util.GanZhi;
import com.wang17.myphone.model.Lunar;
import com.wang17.myphone.util.Religious;
import com.wang17.myphone.util._String;
import com.wang17.myphone.model.CalendarItem;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.model.SolarTerm;

import java.io.DataInputStream;
import java.io.EOFException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A simple {@link Fragment} subclass.
 */
public class ReligiousFragment extends Fragment {


    // 视图变量
    private TextView textView_ganzhi, textViewMonth;
    private ImageView quickButton;
    private CalenderGridAdapter calendarAdapter;
    private LinearLayout layout_religious;
    private GridView userCalender;
    private ProgressBar progressBarLoading;
    // 类变量
    private ProgressDialog progressDialog;
    private Typeface fontHWZS, fontGF;
    private DateTime selectedDate;
    private Map<DateTime, View> calendarItemViewsMap;
    // 值变量
    private int calendarItemLength, preSelectedPosition, todayPosition, currentYear, currentMonth;
    private Map<Integer, CalendarItem> calendarItemsMap;
    private TreeMap<DateTime, SolarTerm> solarTermMap;
    private Map<DateTime, SolarTerm> currentMonthSolarTerms;
    private HashMap<DateTime, String> religiousDays, remarks;
    private Handler uiHandler;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            uiHandler = new Handler();

            //
            solarTermMap = loadJavaSolarTerms(R.raw.solar_java_50);

            //
            Log.i("wangsc", "MainActivity have loaded ... ");
        } catch (Exception ex) {
            Log.i("wangsc", ex.getMessage());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        try {
            View view = inflater.inflate(R.layout.fragment_religious, container, false);

            initializeComponent(view);


            return view;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();

        // 填充日历
        new Thread(new Runnable() {
            @Override
            public void run() {
                selectedDate = DateTime.getToday();
                currentYear = selectedDate.getYear();
                currentMonth = selectedDate.getMonth();
                refreshCalendar();
            }
        }).start();
    }

    /**
     * 方法 - 初始化所有变量
     */
    private void initializeComponent(View view) {
        try {

            //endregion
            calendarItemViewsMap = new HashMap<>();
            currentMonthSolarTerms = new HashMap<>();
            religiousDays = new HashMap<>();
            remarks = new HashMap<>();
            refreshCalendarTask = new RefreshCalendarTask();

            AssetManager mgr = getContext().getAssets();//得到AssetManager
            fontHWZS = Typeface.createFromAsset(mgr, "fonts/STZHONGS.TTF");
            fontGF = Typeface.createFromAsset(mgr, "fonts/GONGFANG.ttf");

            progressBarLoading = (ProgressBar) view.findViewById(R.id.progressBar_loading);

            // selectedDate
            selectedDate = DateTime.getToday();
            currentYear = selectedDate.getYear();
            currentMonth = selectedDate.getMonth();
            currentMonthSolarTerms = new HashMap<DateTime, SolarTerm>();

            textViewMonth = (TextView) view.findViewById(R.id.textView_month);
            textViewMonth.setTypeface(fontGF);
            textViewMonth.setText(selectedDate.getMonth() + 1 + "月");

            quickButton = (ImageView) view.findViewById(R.id.button_today);
            _AnimationUtils.setRorateAnimation(getContext(), quickButton, 7000);
            textViewMonth.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!CalendarHelper.isSameDate(selectedDate, DateTime.getToday())) {
                        try {
                            if (preSelectedPosition != -1) {
                                userCalender.getChildAt(preSelectedPosition).findViewById(R.id.calendarItem_cvIsSelected).setVisibility(View.INVISIBLE);
                            }
                            DateTime today = DateTime.getToday();
                            setSelectedDate(today.getYear(), today.getMonth(), today.getDay());
                        } catch (Exception e) {
                            Log.i("wangsc", e.getMessage());
                        }
                    } else {
                        try {
                            int selectedDay = selectedDate.getDay();
                            DateTime dateTime = new DateTime();
                            dateTime.set(currentYear, currentMonth, selectedDay);
                            dateTime = dateTime.addMonths(1);
                            setSelectedDate(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay());
                        } catch (Exception ex) {
                            Log.i("wangsc", ex.getMessage());
                        }
                    }
                }
            });

            textView_ganzhi = (TextView) view.findViewById(R.id.tvGanZhi);
            layout_religious = (LinearLayout) view.findViewById(R.id.linearReligious);

            // calendarAdapter
            calendarAdapter = new CalenderGridAdapter();
            calendarItemsMap = new HashMap<Integer, CalendarItem>();

            // btnCurrentMonth
//            btnCurrentMonth = (Button) view.findViewById(R.id.btnChangeMonth);
//            btnCurrentMonth.setOnClickListener(btnCurrentMonth_OnClickListener);

            // userCalender
            userCalender = (GridView) view.findViewById(R.id.userCalender);
            userCalender.setOnItemClickListener(userCalender_OnItemClickListener);
            userCalender.setOnItemSelectedListener(userCalender_OnItemSelectedListener);
            GridView calendarHeader = (GridView) view.findViewById(R.id.userCalenderHeader);
            calendarHeader.setAdapter(new CalenderHeaderGridAdapter()); // 添加星期标头

        } catch (Exception ex) {
            Log.i("wangsc", ex.getMessage());
        }
    }

    /**
     * 自定义日历标头适配器
     */
    protected class CalenderHeaderGridAdapter extends BaseAdapter {
        private String[] header = {"一", "二", "三", "四", "五", "六", "日"};

        @Override
        public int getCount() {
            return 7;
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
            TextView mTextView = new TextView(getContext());
            mTextView.setText(header[position]);
            mTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            mTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            mTextView.getPaint().setFakeBoldText(true);
            mTextView.setTypeface(Typeface.MONOSPACE);
            mTextView.setTextColor(Color.parseColor("#000000"));
            mTextView.setWidth(60);
            return mTextView;
        }
    }

    /**
     * 自定义日历适配器
     */
    protected class CalenderGridAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return calendarItemLength;
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
//            convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.inflat_calender_item, null);
            try {
                convertView = View.inflate(getContext(), R.layout.inflat_calender_item, null);
                TextView textViewYangLi = (TextView) convertView.findViewById(R.id.calenderItem_tv_YangLiDay);
                TextView textViewNongLi = (TextView) convertView.findViewById(R.id.calendarItem_tv_NongLiDay);
                ImageView imageIsToday = (ImageView) convertView.findViewById(R.id.calendarItem_cvIsToday);
                ImageView imageIsSelected = (ImageView) convertView.findViewById(R.id.calendarItem_cvIsSelected);
                if (calendarItemsMap.containsKey(position)) {
                    CalendarItem calendarItem = calendarItemsMap.get(position);
                    DateTime today = DateTime.getToday();
                    textViewYangLi.setText(calendarItem.getYangLi().get(DateTime.DAY_OF_MONTH) + "");

                    // 农历月初，字体设置。
                    if (calendarItem.getNongLi().getDay() == 1) {
                        if (calendarItem.getNongLi().isLeap())
                            textViewNongLi.setText("闰" + calendarItem.getNongLi().getMonthStr());
                        else
                            textViewNongLi.setText(calendarItem.getNongLi().getMonthStr());
                        textViewNongLi.setTextColor(Color.BLACK);
                        textViewNongLi.getPaint().setFakeBoldText(true);
                    } else {
                        textViewNongLi.setText(calendarItem.getNongLi().getDayStr());
                    }

                    // 今天
                    if (today.compareTo(calendarItem.getYangLi().getDate()) == 0) {
                        imageIsToday.setVisibility(View.VISIBLE);
                        textViewYangLi.setTextColor(Color.WHITE);
                        textViewNongLi.setTextColor(Color.WHITE);
                        todayPosition = position;
                    }

                    // 选中的日期
                    if (CalendarHelper.isSameDate(calendarItem.getYangLi(), selectedDate) && !CalendarHelper.isSameDate(calendarItem.getYangLi(), today)) {
                        imageIsSelected.setVisibility(View.VISIBLE);
                        preSelectedPosition = position;
                    }
                    calendarItemViewsMap.put(calendarItem.getYangLi(), convertView);
                } else {
                    textViewYangLi.setText("");
                    textViewNongLi.setText("");
                }
            } catch (Exception e) {
            }
            return convertView;
        }
    }

    private int findReligiousKeyWord(String religious) {
        if (religious.contains("俱亡")
                || religious.contains("奇祸")
                || religious.contains("夺纪")
                || religious.contains("大祸")
                || religious.contains("促寿")
                || religious.contains("恶疾")
                || religious.contains("大凶")
                || religious.contains("绝嗣")
                || religious.contains("男死")
                || religious.contains("女死")
                || religious.contains("血死")
                || religious.contains("一年内死")
                || religious.contains("危疾")
                || religious.contains("水厄")
                || religious.contains("贫夭")
                || religious.contains("暴亡")
                || religious.contains("失瘏夭胎")
                || religious.contains("损寿子带疾")
                || religious.contains("阴错日")
                || religious.contains("十恶大败日")
                || religious.contains("一年内亡")
                || religious.contains("必得急疾")
                || religious.contains("生子五官四肢不全。父母有灾")
                || religious.contains("减寿五年")
                || religious.contains("恶胎")) {
            return 1;
        }
        return 0;
    }


    /**
     * 更新信息栏（农历，干支，戒期信息），一定要在当前月份的日历界面载入完毕后在引用此方法。因为此方法数据调用calendarItemsMap，而calendarItemsMap是在形成当月数据时形成。
     *
     * @param seletedDateTime 当前选中的日期
     */
    private void refreshInfoLayout(DateTime seletedDateTime) {
        try {
            if (calendarItemsMap.size() == 0) return;

            CalendarItem calendarItem = null;
            for (Map.Entry<Integer, CalendarItem> entity : calendarItemsMap.entrySet()) {
                if (CalendarHelper.isSameDate(entity.getValue().getYangLi(), seletedDateTime)) {
                    calendarItem = entity.getValue();
                }
            }
            if (calendarItem == null) return;
//        yearMonth.setText(currentYear + "." + format(currentMonth + 1));
//        yangliBig.setText(seletedDateTime.getDay() + "");
            GanZhi gz = new GanZhi(calendarItem.getYangLi(), this.solarTermMap);
            textView_ganzhi.setText(_String.concat(gz.getTianGanYear(), gz.getDiZhiYear(), "年 ",
                    gz.getTianGanMonth(), gz.getDiZhiMonth(), "月 ",
                    gz.getTianGanDay(), gz.getDiZhiDay(), "日"));


            layout_religious.removeAllViews();

            boolean haveReligious = calendarItem.getReligious() != null;
            boolean haveRemarks = calendarItem.getRemarks() != null;
            if (haveReligious) {
                String[] religious = calendarItem.getReligious().split("\n");
                int i = 1;
                for (String str : religious) {
                    View view = View.inflate(getContext(), R.layout.inflate_targ_religious, null);

                    TextView tv = (TextView) view.findViewById(R.id.textView_religious);
                    tv.setText(str);
                    tv.getPaint().setFakeBoldText(true);
                    tv.setTypeface(fontHWZS);
                    if (findReligiousKeyWord(str) == 1) {
                        tv.setTextColor(getResources().getColor(R.color.month_text_color));
                    }

                    layout_religious.addView(view);
                }
            }
            if (haveRemarks) {
                String[] remarks = calendarItem.getRemarks().split("\n");
                int i = 1;

                for (String str : remarks) {
                    View view = View.inflate(getContext(), R.layout.inflate_targ_note, null);

                    TextView tv = (TextView) view.findViewById(R.id.textView_note);
                    tv.setText(str);
                    tv.getPaint().setFakeBoldText(true);
                    tv.setTypeface(fontHWZS);

                    layout_religious.addView(view);
                }
            }
        } catch (Exception e) {
            Log.i("wangsc", e.getMessage());
        }
    }

    /**
     * 刷新日历界面，使用此方法必须标明forAsynch变量。
     */
    private void refreshCalendar() {
        try {
            refreshCalendarTask.cancel(true);
            calendarItemsMap.clear();
            calendarItemViewsMap.clear();
            currentMonthSolarTerms.clear();
            religiousDays.clear();
            remarks.clear();

            int maxDayInMonth = 0;
            DateTime tmpToday = new DateTime(currentYear, currentMonth, 1);
            calendarItemLength = maxDayInMonth = tmpToday.getActualMaximum(DateTime.DAY_OF_MONTH);
            int day_week = tmpToday.get(DateTime.DAY_OF_WEEK) - 1;
            if (day_week == 0)
                day_week = 7;
            calendarItemLength += day_week - 1;

            // 得到填充日历控件所需要的数据
            boolean tag = false;
            for (int i = 1; i <= maxDayInMonth; i++) {
                int week = tmpToday.get(DateTime.WEEK_OF_MONTH);
                day_week = tmpToday.get(DateTime.DAY_OF_WEEK) - 1;
                if (day_week == 0) {
                    day_week = 7;
                    week--;
                    if (week == 0) {
                        tag = true;
                    }
                }
                if (tag) {
                    week++;
                }
                int key = (week - 1) * 7 + day_week - 1;
                DateTime newCalendar = new DateTime();
                newCalendar.setTimeInMillis(tmpToday.getTimeInMillis());
                CalendarItem item = new CalendarItem(newCalendar);
                calendarItemsMap.put(key, item);
                tmpToday.add(DateTime.DAY_OF_MONTH, 1);
            }
            // 填充日历控件
            todayPosition = -1;
            preSelectedPosition = -1;

            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        userCalender.setAdapter(calendarAdapter);
                        refreshCalendarTask = new RefreshCalendarTask();
                        refreshCalendarTask.execute();
                    } catch (Exception e) {
                    }
                }
            });
        } catch (NumberFormatException e) {
        }
    }

    int progress;
    private RefreshCalendarTask refreshCalendarTask;

    private class RefreshCalendarTask extends AsyncTask {

        /**
         * doInBackground方法内部执行后台任务,不可在此方法内修改UI
         *
         * @param params
         * @return
         */
        @Override
        protected Object doInBackground(Object[] params) {
            try {
                progress = 0;
                // 得到本月节气
                for (Map.Entry<DateTime, SolarTerm> entry : solarTermMap.entrySet()) {
                    if (entry.getKey().getYear() == currentYear && entry.getKey().getMonth() == currentMonth) {
                        currentMonthSolarTerms.put(entry.getKey(), entry.getValue());
                    }
                }

//                publishProgress(progress++);

                // 获得当月戒期信息
                try {
                    Religious religious = new Religious( currentYear, currentMonth, solarTermMap, new ReligiousCallBack() {
                        @Override
                        public void execute() {
//                            publishProgress(progress++);
                        }
                    });
                    religiousDays = religious.getReligiousDays();

//                    publishProgress(progress++);
                    remarks = religious.getRemarks();
                } catch (InterruptedException e) {
                } catch (Exception ex) {
                    religiousDays = new HashMap<>();
                    remarks = new HashMap<>();
                }
//                publishProgress(progress++);
            } catch (Exception e) {
            }
            return null;
        }

        /**
         * onPreExecute方法用于在执行后台任务前做一些UI操作
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
//            progressBarLoading.setVisibility(View.VISIBLE);
//            progressBarLoading.setProgress(0);
//            progressBarLoading.setMax(selectedDate.getActualMaximum(DateTime.DAY_OF_MONTH));
        }

        /**
         * onProgressUpdate方法用于更新进度信息
         *
         * @param values
         */
        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
//            progressBarLoading.setProgress((int) values[0]);
        }

        /**
         * 得到指定日期在日历中的position。
         *
         * @param dateTime
         * @return
         */
        private int dateTimeToPosition(DateTime dateTime, boolean tag) {
            int week = dateTime.get(DateTime.WEEK_OF_MONTH);
            int day_week = dateTime.get(DateTime.DAY_OF_WEEK) - 1;
            if (day_week == 0) {
                day_week = 7;
                week--;
                if (week == 0) {
                    tag = true;
                }
            }
            if (tag) {
                week++;
            }
            return (week - 1) * 7 + day_week - 1;
        }

        /**
         * onPostExecute方法用于在执行完后台任务后更新UI,显示结果
         *
         * @param o
         */
        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            try {
                Boolean tag = false;
                DateTime dateTime = new DateTime(selectedDate.getYear(), selectedDate.getMonth(), 1);
                if (dateTime.get(DateTime.DAY_OF_WEEK) - 1 == 0) {
                    tag = true;
                }

                for (Map.Entry<DateTime, View> entry : calendarItemViewsMap.entrySet()) {
                    CalendarItem calendarItem = calendarItemsMap.get(dateTimeToPosition(entry.getKey(), tag));
                    if (calendarItem != null) {
                        View convertView = entry.getValue();
                        TextView textViewNongLi = (TextView) convertView.findViewById(R.id.calendarItem_tv_NongLiDay);
                        ImageView imageIsUnReligious = (ImageView) convertView.findViewById(R.id.calendarItem_cvIsUnReligious);

                        // 节气
                        for (Map.Entry<DateTime, SolarTerm> termEntry : currentMonthSolarTerms.entrySet()) {
                            DateTime today = new DateTime(termEntry.getKey().getYear(), termEntry.getKey().getMonth(), termEntry.getKey().get(DateTime.DAY_OF_MONTH), 0, 0, 0);
                            if (CalendarHelper.isSameDate(today, calendarItem.getYangLi())) {
                                textViewNongLi.setText(termEntry.getValue().toString());
                                //                            textViewNongLi.setTextColor(Color.CYAN);
                                break;
                            }
                        }

                        //
                        DateTime currentDate = entry.getKey().getDate();
                        if (religiousDays.containsKey(currentDate)) {
                            calendarItem.setReligious(religiousDays.get(currentDate));
                        }
                        if (remarks.containsKey(currentDate)) {
                            calendarItem.setRemarks(remarks.get(currentDate));
                        }

                        // 非戒期日
                        if (calendarItem.getReligious() == null) {
                            imageIsUnReligious.setVisibility(View.VISIBLE);
                        }
                        // 戒期日，找到警示关键字。
                        else if (findReligiousKeyWord(calendarItem.getReligious()) == 1) {
                            textViewNongLi.setTextColor(getResources().getColor(R.color.month_text_color));
                        }
                        refreshInfoLayout(selectedDate);
                    }
                }
//                isHaveLoaded = true;
            } catch (Exception e) {
            }
        }

        /**
         * onCancelled方法用于在取消执行中的任务时更改UI
         */
        @Override
        protected void onCancelled() {
            super.onCancelled();

            progressBarLoading.setVisibility(View.VISIBLE);
            progressBarLoading.setProgress(0);
        }
    }

    /**
     * 事件 - 改变月份按钮
     */
    View.OnClickListener btnCurrentMonth_OnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                showMonthPickerDialog(currentYear, currentMonth);
            } catch (Exception ex) {
                Log.i("wangsc", ex.getMessage());
            }
        }
    };


    /**
     * 事件 - 点击日历某天
     */
    private AdapterView.OnItemClickListener userCalender_OnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            try {
                if (!calendarItemsMap.containsKey(position)) return;

                CalendarItem calendarItem = calendarItemsMap.get(position);
                CalendarItem preCalendarItem = calendarItemsMap.get(preSelectedPosition);
                if (todayPosition != -1) {
                    if (preSelectedPosition != -1 && preSelectedPosition != todayPosition) {
                        parent.getChildAt(preSelectedPosition).findViewById(R.id.calendarItem_cvIsSelected).setVisibility(View.INVISIBLE);
                    }
                    if (position != todayPosition) {
                        view.findViewById(R.id.calendarItem_cvIsSelected).setVisibility(View.VISIBLE);
                    }
                } else {
                    if (preSelectedPosition != -1) {
                        parent.getChildAt(preSelectedPosition).findViewById(R.id.calendarItem_cvIsSelected).setVisibility(View.INVISIBLE);
                    }
                    view.findViewById(R.id.calendarItem_cvIsSelected).setVisibility(View.VISIBLE);
                }
                preSelectedPosition = position;

                //
                setSelectedDate(calendarItem.getYangLi().getYear(), calendarItem.getYangLi().getMonth(), calendarItem.getYangLi().getDay());
            } catch (Exception e) {
                Log.i("wangsc", e.getMessage());
            }
        }
    };

    /**
     * 设置SelectedDate，并在修改该属性之后，重载自定义日历区域数据。
     *
     * @param year
     * @param month
     * @param day
     */
    public void setSelectedDate(int year, int month, int day) {
        try {
            boolean monthHasChanged = false;

            // 如果新选中日期与当前月份不再同一月份，则刷新日历。
            if (year != currentYear || month != currentMonth) {
                monthHasChanged = true;
            }
            this.selectedDate.set(year, month, day);

            // 判断是否刷新自定义日历区域
            if (monthHasChanged) {
                currentYear = year;
                currentMonth = month;
                refreshCalendar();
            }

            // “今”按钮是否显示
            textViewMonth.setText(selectedDate.getMonth() + 1 + "月");

            //
            refreshInfoLayout(selectedDate);
        } catch (Exception e) {
            Log.i("wangsc", e.getMessage());
        }
    }

    /**
     * 事件 - 选中日历某天
     */
    private AdapterView.OnItemSelectedListener userCalender_OnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };


    public void showMonthPickerDialog(int year, int month) {
        View view = View.inflate(getContext(), R.layout.inflate_date_picker_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(view).create();
        dialog.setTitle("选择月份");


        final NumberPicker npYear = (NumberPicker) view.findViewById(R.id.npYear);
        final NumberPicker npMonth = (NumberPicker) view.findViewById(R.id.npMonth);


        String[] yearValues = new String[Lunar.MaxYear - Lunar.MinYear + 1];
        for (int i = 0; i < yearValues.length; i++) {
            yearValues[i] = i + Lunar.MinYear + "年";
        }

        String[] monthValues = new String[12];
        for (int i = 0; i < monthValues.length; i++) {
            monthValues[i] = i + 1 + "月";
        }


        npYear.setMinValue(Lunar.MinYear);
        npYear.setMaxValue(Lunar.MaxYear);
        npYear.setDisplayedValues(yearValues);
        npYear.setValue(year);
        npYear.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
        npMonth.setMinValue(1);
        npMonth.setMaxValue(12);
        npMonth.setDisplayedValues(monthValues);
        npMonth.setValue(month + 1);
        npMonth.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中


        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "选择", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int year = npYear.getValue();
                int month = npMonth.getValue() - 1;
                DateTime dateTime = new DateTime();
                dateTime.set(year, month, 1);
                int maxDayOfMonth = dateTime.getActualMaximum(Calendar.DAY_OF_MONTH);
                int selectedDay = selectedDate.getDay();
                setSelectedDate(year, month, maxDayOfMonth < selectedDay ? maxDayOfMonth : selectedDay);
                //                    refreshCalendarWithDialog();
                dialog.dismiss();
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

    /**
     * 读取JAVA结构的二进制节气数据文件。
     *
     * @param resId 待读取的JAVA二进制文件。
     * @return
     */
    private TreeMap<DateTime, SolarTerm> loadJavaSolarTerms(int resId) throws Exception {
        TreeMap<DateTime, SolarTerm> result = new TreeMap<DateTime, SolarTerm>();
        try {
            DataInputStream dis = new DataInputStream(getResources().openRawResource(resId));

            long date = dis.readLong();
            int solar = dis.readInt();
            try {
                while (true) {
                    DateTime cal = new DateTime();
                    cal.setTimeInMillis(date);
                    SolarTerm solarTerm = SolarTerm.Int2SolarTerm(solar);
                    result.put(cal, solarTerm);
                    date = dis.readLong();
                    solar = dis.readInt();
                }
            } catch (EOFException ex) {
                dis.close();
            }
        } catch (Resources.NotFoundException e) {
            Log.i("wangsc", e.getMessage());
        } catch (Exception e) {
            Log.i("wangsc", e.getMessage());
        }
        // 按照KEY排序TreeMap
//        TreeMap<DateTime, SolarTerm> result = new TreeMap<DateTime, SolarTerm>(new Comparator<DateTime>() {
//            @Override
//            public int compare(DateTime lhs, DateTime rhs) {
//                return lhs.compareTo(rhs);
//            }
//        });
        return result;
    }
}
