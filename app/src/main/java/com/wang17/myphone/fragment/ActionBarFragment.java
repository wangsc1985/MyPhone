package com.wang17.myphone.fragment;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.database.MarkDay;
import com.wang17.myphone.database.Setting;
import com.wang17.myphone.database.DataContext;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.model.DateTime;

import java.util.Calendar;

public class ActionBarFragment extends Fragment {

    //   视图变量
    ImageView imageView_back, imageView_setting;
//    ImageView imageViewHelper;
//    TextView textViewHave, textViewLeave;
    TextView textViewTitle;
    ProgressBar progressBar;
    LinearLayout layoutReligious;


    //   类变量
    private OnActionFragmentBackListener backListener;
    private OnActionFragmentSettingListener settingListener;
    private OnActionFragmentProgressListener progressListener;
    private DataContext dataContext;

    //   值变量
    public static final int TO_LIST = 0;

    @Override
    public void onResume() {
        super.onResume();
        if (!_Utils.isAccessibilitySettingsOn(getContext())) {
            isShowHelperImage(true);
        } else {
            isShowHelperImage(false);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        //  2016/10/19 初始化类变量和值变量
        super.onCreate(savedInstanceState);
        try {
            dataContext = new DataContext(getActivity());
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            textViewTitle = view.findViewById(R.id.textView_title);
            textViewTitle.setText(dataContext.getSetting(Setting.KEYS.bar_title,"南无阿弥陀佛").getString());

            ConstraintLayout root = view.findViewById(R.id.layout_root);
            root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });
            root.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return true;
                }
            });

            //  2016/10/19 初始化视图
            imageView_setting =  view.findViewById(R.id.imageView_setting);
            imageView_setting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (settingListener != null)
                        settingListener.onSettingButtonClickListener();
                }
            });
            imageView_setting.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (settingListener != null) {
                        settingListener.onSettingButtonLongClickListener();
                    }
                    return true;
                }
            });


            imageView_back = view.findViewById(R.id.imageView_back);
            imageView_back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (backListener != null)
                        backListener.onBackButtonClickListener();
                }
            });

            //
            progressBar = view.findViewById(R.id.progressBar);
            layoutReligious = view.findViewById(R.id.layout_Religious);
            layoutReligious.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    setDateTimeDialog(new DateTime());
                    return true;
                }
            });

            if (settingListener == null) {
                imageView_setting.setVisibility(View.INVISIBLE);
            }
            if (backListener == null) {
                imageView_back.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_action_bar, container, false);
    }

    /**
     * 设置帮助图标显示与隐藏。
     *
     * @param isShow
     */
    public void isShowHelperImage(boolean isShow) {
        if (isShow) {
//            AnimationUtils.setFlickerAnimation(imageViewHelper);
        } else {
//            imageViewHelper.clearAnimation();
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            if (context instanceof OnActionFragmentBackListener) {
                backListener = (OnActionFragmentBackListener) context;
            }
            if (context instanceof OnActionFragmentSettingListener) {
                settingListener = (OnActionFragmentSettingListener) context;
            }
            if (context instanceof OnActionFragmentProgressListener) {
                progressListener = (OnActionFragmentProgressListener) context;
            }
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
    }

    /**
     * 显示持戒时间
     */
//    private void setTextViewSexualText() {
//        try {
//            MarkDay lastSexualDay = dataContext.getLastMarkDay();
//            if (lastSexualDay == null) {
//                textViewHave.setText("");
//                textViewLeave.setText("");
//            } else {
//                int have = (int) ((new DateTime().getTimeInMillis() - lastSexualDay.getDateTime().getTimeInMillis()) / 3600000);
//                int leave = (int) (_Utils.getTargetInMillis(_Session.BIRTHDAY) / 3600000) - have;
//                textViewHave.setText(DateTime.toSpanString1(have));
//                textViewLeave.setText(DateTime.toSpanString1(leave));
//                if (leave < 0) {
//                    leave *= -1;
//                    textViewLeave.setText("+" + DateTime.toSpanString1(leave));
//                }
//                progressBar.setMax((int) (_Utils.getTargetInMillis(_Session.BIRTHDAY) / 3600000));
//                progressBar.setProgress(have);
//            }
//        } catch (Exception e) {
//            _Utils.printException(getContext(), e);
//        }
//    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        try {
//            if (requestCode == TO_LIST) {
//                //            if(MarkDayRecordActivity.lastDateTimeChanged){
//                setTextViewSexualText();
//                //            }
//            }
//        } catch (Exception e) {
//            _Utils.printException(getContext(), e);
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    /**
     * 如果要使用返回按钮，需实现此接口
     */
    public interface OnActionFragmentBackListener {
        //  更新参数的类型和名字
        void onBackButtonClickListener();
    }

    /**
     * 如果要显示progressbar，需实现此接口
     */
    public interface OnActionFragmentProgressListener {
    }

    /**
     * 如果要使用设置按钮，需实现此接口
     */
    public interface OnActionFragmentSettingListener {
        void onSettingButtonClickListener();

        void onSettingButtonLongClickListener();
    }

    public void setDateTimeDialog(DateTime dateTime) {

        try {
            View view = View.inflate(getContext(), R.layout.inflate_dialog_date_picker, null);
            final AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(view).create();
            dialog.setTitle("设定时间");

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
                        MarkDay markDay = new MarkDay(selectedDateTime, _Session.UUID_NULL);
                        dataContext.addMarkDay(markDay);
//                        setTextViewSexualText();
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
//
//    public class AddSexualDayDialog {
//        private Dialog dialog;
//
//        public AddSexualDayDialog(DateTime dateTime) {
//            try {
//                dialog = new Dialog(getContext());
//                dialog.setContentView(R.layout.inflate_dialog_date_picker);
//                dialog.setTitle("设定时间");
//
//                int year = dateTime.getYear();
//                int month = dateTime.getMonth();
//                int maxDay = dateTime.getActualMaximum(Calendar.DAY_OF_MONTH);
//                int day = dateTime.getDay();
//                int hour = dateTime.getHour();
//
//                final NumberPicker npYear = (NumberPicker) dialog.findViewById(R.id.npYear);
//                final NumberPicker npMonth = (NumberPicker) dialog.findViewById(R.id.npMonth);
//                final NumberPicker npDay = (NumberPicker) dialog.findViewById(R.id.npDay);
//                final NumberPicker npHour = (NumberPicker) dialog.findViewById(R.id.npHour);
//                Button btnOK = (Button) dialog.findViewById(R.id.btnOK);
//                Button btnCancle = (Button) dialog.findViewById(R.id.btnCancel);
//                npYear.setMinValue(year - 2);
//                npYear.setMaxValue(year);
//                npYear.setValue(year);
//                npYear.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
//                npMonth.setMinValue(1);
//                npMonth.setMaxValue(12);
//                npMonth.setValue(month + 1);
//                npMonth.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
//                npDay.setMinValue(1);
//                npDay.setMaxValue(maxDay);
//                npDay.setValue(day);
//                npDay.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
//                npHour.setMinValue(0);
//                npHour.setMaxValue(23);
//                npHour.setValue(hour);
//                npHour.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
//                btnOK.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        try {
//                            int year = npYear.getString();
//                            int month = npMonth.getString() - 1;
//                            int day = npDay.getString();
//                            int hour = npHour.getString();
//                            DateTime selectedDateTime = new DateTime(year, month, day, hour, 0, 0);
//                            MarkDay sexualDay = new MarkDay(selectedDateTime);
//                            dataContext.addMarkDay(sexualDay);
//                            setTextViewSexualText();
//                            dialog.cancel();
//                        } catch (Exception e) {
//                            _Utils.printException(getContext(),e);
//                        }
//                    }
//                });
//                btnCancle.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        try {
//                            dialog.cancel();
//                        } catch (Exception e) {
//                            _Utils.printException(getContext(),e);
//                        }
//                    }
//                });
//            } catch (Exception e) {
//                _Utils.printException(getContext(),e);
//            }
//        }
//
//        public void show() {
//            try {
//                dialog.show();
//            } catch (Exception e) {
//                _Utils.printException(getContext(),e);
//            }
//        }
//    }
}
