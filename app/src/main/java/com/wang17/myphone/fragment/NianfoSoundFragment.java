package com.wang17.myphone.fragment;


import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.database.Setting;
import com.wang17.myphone.service.NianfoService;

import static android.content.Context.ALARM_SERVICE;
import static android.os.PowerManager.SCREEN_DIM_WAKE_LOCK;

/**
 * A simple {@link Fragment} subclass.
 */

@Deprecated
public class NianfoSoundFragment extends Fragment {

    //region 视图变量
    private View view;
    private Button button_start;
    private RelativeLayout layout_image;
    private Switch aSwitch_screen;
    //endregion
    //region 类变量
    private Vibrator vibrator;
    private DataContext dataContext;
    //endregion
    //region 值变量
    private int vibrateTime = 100; // 手机震动时间
    private static boolean isReading;
    private static int interval;
    //endregion

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            dataContext = new DataContext(getContext());
            // 震动变量
            vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

            interval = Integer.parseInt(dataContext.getSetting(Setting.KEYS.nianfo_intervalInMillis, 60).getString());
            super.onCreate(savedInstanceState);
        } catch (NumberFormatException e) {
            _Utils.printException(getContext(), e);
        }
    }

    private static PowerManager.WakeLock wakeLock;
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
//            EventBus.getDefault().register(this);
            // 加载视图
            view = inflater.inflate(R.layout.fragment_nian_fo, container, false);

            aSwitch_screen = (Switch) view.findViewById(R.id.switch_screen);
            aSwitch_screen.setChecked(Boolean.parseBoolean(dataContext.getSetting(Setting.KEYS.nianfo_screen_light, false).getString()));
            aSwitch_screen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        wakeLock=_Utils.acquireWakeLock(getContext(), SCREEN_DIM_WAKE_LOCK);
                        dataContext.editSetting(Setting.KEYS.nianfo_screen_light, isChecked);

                        try {
                            Intent intent = new Intent(_Session.POWER_MANAGER_ACTION);
                            PendingIntent pi = PendingIntent.getBroadcast(getContext(), 0, intent, 0);
                            AlarmManager am = (AlarmManager) getContext().getSystemService(ALARM_SERVICE);
                            am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 25 * 60000, pi);
                        } catch (Exception e) {
                            _Utils.printException(getContext(), e);
                        }
                        Log.i("wangsc", "screen light by click...");

                    } else {
                        _Utils.releaseWakeLock(getContext(),wakeLock);
                        dataContext.editSetting(Setting.KEYS.nianfo_screen_light, isChecked);
                        Log.i("wangsc", "screen non-light by click...");
                    }
                }
            });

            layout_image = (RelativeLayout) view.findViewById(R.id.layout_image);
            layout_image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    over();
                }
            });
            layout_image.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return true;
                }
            });

            button_start = (Button) view.findViewById(R.id.button_start);
            button_start.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    start();
                    return true;
                }
            });
            button_start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSetIntervalDialog();
                }
            });
            button_start.setText(DateTime.toSpanString(interval * 1000, 3, 1));
            return view;
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
            return view;
        }
    }

    private void start() {
        try {
            //　保存状态
            isReading = true;
            dataContext.editSetting(Setting.KEYS.nianfo_isReading, isReading);

            vibrator.vibrate(vibrateTime);// 震动
            layout_image.setVisibility(View.VISIBLE);// 图片显示

            // 启动服务
            Intent intent = new Intent(getContext(), NianfoService.class);
            intent.putExtra("timer", interval * 1000);
            getActivity().startService(intent);

        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
    }

    private void over() {
        try {
            //　保存状态
            isReading = false;
            dataContext.editSetting(Setting.KEYS.nianfo_isReading, isReading);

            vibrator.vibrate(vibrateTime);// 震动
            layout_image.setVisibility(View.INVISIBLE);// 图片隐藏
            getActivity().stopService(new Intent(getContext(), NianfoService.class));// 停止服务
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
    }

    private void showSetIntervalDialog() {
        try {
            SetIntervalDialog dialog = new SetIntervalDialog();
            dialog.show();
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
    }

    @Override
    public void onResume() {
        try {
            isReading = Boolean.parseBoolean(dataContext.getSetting(Setting.KEYS.nianfo_isReading, "false").getString());
            if (isReading) {
                layout_image.setVisibility(View.VISIBLE);
            } else {
                layout_image.setVisibility(View.INVISIBLE);
            }
        } catch (NumberFormatException e) {
            _Utils.printException(getContext(), e);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public class SetIntervalDialog {
        private Dialog dialog;

        public SetIntervalDialog() {
            try {
                dialog = new Dialog(getActivity());
                dialog.setContentView(R.layout.inflate_dialog_interval_picker);
                dialog.setTitle("设定默认计划时间");

                String[] secondNumbers = new String[12];
                for (int i = 0; i < 12; i++) {
                    secondNumbers[i] = i * 5 + "";
                }
                int xxxMinite = interval / 60;
                int xxxSecond = interval % 60;
                ((TextView) dialog.findViewById(R.id.textView_hour)).setText("分");
                ((TextView) dialog.findViewById(R.id.textView_min)).setText("秒");
                final NumberPicker number_minite = (NumberPicker) dialog.findViewById(R.id.number_hour);
                final NumberPicker number_second = (NumberPicker) dialog.findViewById(R.id.number_min);
                Button btnOK = (Button) dialog.findViewById(R.id.btnOK);
                Button btnCancle = (Button) dialog.findViewById(R.id.btnCancel);
                number_minite.setMinValue(0);
                number_minite.setMaxValue(99);
                number_minite.setValue(xxxMinite);
                number_minite.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
                number_second.setMinValue(0);
                number_second.setMaxValue(11);
                number_second.setDisplayedValues(secondNumbers);
                number_second.setValue(xxxSecond / 5);
                number_second.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 禁止对话框打开后数字选择框被选中
                number_minite.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        try {
                            if (newVal == 0 && number_second.getValue() == 0) {
                                number_second.setValue(6);
                            }
                        } catch (Exception e) {
                            _Utils.printException(getContext(), e);
                        }
                    }
                });
                number_second.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        try {
                            if (newVal == 0 && number_minite.getValue() == 0) {
                                number_minite.setValue(1);
                            }
                        } catch (Exception e) {
                            _Utils.printException(getContext(), e);
                        }
                    }
                });
                btnOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            int minite = number_minite.getValue();
                            int second = number_second.getValue() * 5;
                            interval = minite * 60 + second;
//                            textView_interval.setText(DateTime.toSpanString(isFitSpeed * 1000, 3, 1));
                            dataContext.editSetting(Setting.KEYS.nianfo_intervalInMillis, interval);
                            button_start.setText(DateTime.toSpanString(interval * 1000, 3, 1));
                            dialog.dismiss();
                        } catch (Exception e) {
                            _Utils.printException(getContext(), e);
                        }
                    }
                });
                btnCancle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            dialog.dismiss();
                        } catch (Exception e) {
                            _Utils.printException(getContext(), e);
                        }
                    }
                });
            } catch (Exception e) {
                _Utils.printException(getContext(), e);
            }
        }

        public void show() {
            try {
                dialog.show();
            } catch (Exception e) {
                _Utils.printException(getContext(), e);
            }
        }
    }


}
