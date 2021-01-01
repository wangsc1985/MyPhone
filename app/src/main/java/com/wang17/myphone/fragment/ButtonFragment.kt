package com.wang17.myphone.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.wang17.myphone.R
import com.wang17.myphone.activity.*
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.e
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.service.TimerByMediaLoopService
import com.wang17.myphone.util.*
import com.wang17.myphone.view._Button
import kotlinx.android.synthetic.main.fragment_button.*
import java.io.*

/**
 * A simple [Fragment] subclass.
 */
class ButtonFragment : Fragment() {
    private lateinit var numberSpeaker: NumberSpeaker
    private lateinit var dataContext: DataContext

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataContext = DataContext(context)
        numberSpeaker = NumberSpeaker(context)

        //region 备份按钮
        var btn = _Button(context!!, "备份")
        btn.setOnClickListener {
            AlertDialog.Builder(context!!).setMessage("确认要备份数据吗？").setPositiveButton("确定") { dialog, which ->
                BackupTask(context).execute(BackupTask.COMMAND_BACKUP)
            }.setNegativeButton("取消", null).show()
        }
        layout_flexbox.addView(btn)
        //endregion

        //region 恢复按钮
        btn = _Button(context!!, "恢复")
        btn.setOnClickListener {
            AlertDialog.Builder(context!!).setMessage("确认要恢复数据吗？").setPositiveButton("确定") { dialog, which ->
                BackupTask(context).execute(BackupTask.COMMAND_RESTORE)
            }.setNegativeButton("取消", null).show()
        }
        layout_flexbox.addView(btn)
        //endregion

        //region ST按钮
        btn = _Button(context!!, "ST")
        btn.setOnClickListener {
            context!!.startActivity(Intent(context, StockPositionActivity::class.java))
            val mDataContext = DataContext(context)
            mDataContext.editSetting(Setting.KEYS.quick, 1)
        }
        layout_flexbox.addView(btn)
        //endregion

        //region FU按钮
        btn = _Button(context!!, "FU")
        btn.setOnClickListener {
            context!!.startActivity(Intent(context, FuturePositionActivity::class.java))
            val mDataContext = DataContext(context)
            mDataContext.editSetting(Setting.KEYS.quick, 3)
        }
        layout_flexbox.addView(btn)
        //endregion

        //region 足迹
        btn = _Button(context!!, "足迹")
        btn.setOnClickListener {
            try {
                startActivity(Intent(context, LocationListActivity::class.java))
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        layout_flexbox.addView(btn)
        //endregion

        //region 地图
        btn = _Button(context!!, "地图")
        btn.setOnClickListener {
            try {
                startActivity(Intent(context, AmapActivity::class.java))
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        layout_flexbox.addView(btn)
        //endregion

        //region TODO界面
        btn = _Button(context!!, "TODO")
        btn.setOnClickListener {
            activity!!.startActivity(Intent(context, ToDoActivity::class.java))
        }
        layout_flexbox.addView(btn)
        //endregion

        //region 时间
        btn = _Button(context!!, "时间")
        btn.setOnClickListener {
            AlertDialog.Builder(context!!).setMessage("确定要重置计数器吗？").setPositiveButton("是") { dialog, which ->
                DataContext(context).editSetting(Setting.KEYS.wx_db_mark_date, System.currentTimeMillis())
            }.show()
        }
        layout_flexbox.addView(btn)
        //endregion

        //region 微小云库
        btn = _Button(context!!, "getNewMsg")
        btn.setOnClickListener {
                var start = System.currentTimeMillis()
                _CloudUtils.getNewMsg(context!!, object : CloudCallback {
                    override fun excute(code: Int, result: Any?) {
                    }
                })
            }
        layout_flexbox.addView(btn);
        //endregion

        //region 时间
//        btn = new _Button(getContext(), "getLocations");
//        btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                long start = System.currentTimeMillis();
//                _CloudUtils.getLocations(new CloudCallback() {
//                    @Override
//                    public void excute(int code, Object result) {
//                        long end = System.currentTimeMillis();
//                        _LogUtils.log2file("span.log", "getLocations用时：" + (end - start) + "毫秒", "");
//                        Looper.prepare();
//                        Toast.makeText(getContext(), "阿弥陀佛", Toast.LENGTH_SHORT).show();
//                        Looper.loop();
//                    }
//                });
//            }
//        });
//        flexboxLayout.addView(btn);
        //endregion

        //region Fund
        btn = _Button(context!!, "Fund")
        btn.setOnClickListener {
            startActivity(Intent(context, FundMonitorActivity::class.java))
        }
        layout_flexbox.addView(btn)
        //endregion

        //region 时间
//        btn = new _Button(getContext(), "getUser");
//        btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                long start = System.currentTimeMillis();
//                _CloudUtils.getUser("0088", new CloudCallback() {
//                    @Override
//                    public void excute(int code, Object result) {
//                        long end = System.currentTimeMillis();
//                        _LogUtils.log2file("span.log", "getUser用时：" + (end - start) + "毫秒", "");
//                        Looper.prepare();
//                        Toast.makeText(getContext(), "阿弥陀佛", Toast.LENGTH_SHORT).show();
//                        Looper.loop();
//                    }
//                });
//            }
//        });
//        flexboxLayout.addView(btn);
        //endregion

        //region 音乐占位Timer
        btn = _Button(context!!, "Timer");
        btn.setOnClickListener {
            context!!.startService(Intent(context!!, TimerByMediaLoopService::class.java))
            e("开始")
        };
        layout_flexbox.addView(btn);
        //endregion

        //region 日志
        btn = _Button(context!!, "日志")
        btn.setOnClickListener {
            try {
                context!!.startActivity(Intent(context, RunLogActivity::class.java))
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        layout_flexbox.addView(btn)
        //endregion

        //region 历史持仓
        btn = _Button(context!!, "历史")
        btn.setOnClickListener {
            try {
                context!!.startActivity(Intent(context, StockPositionHistoryActivity::class.java))
            } catch (e: Exception) {
                _Utils.printException(context, e)
            }
        }
        btn.setOnLongClickListener {
            context!!.startActivity(Intent(context, SmsActivity::class.java))
            true
        }
        layout_flexbox.addView(btn)
        //endregion

        //region 日志
//        btn = _Button(context!!, "音效")
//        btn.setOnClickListener {
//            _SoundUtils.play(context, R.raw.maopao)
//        }
//        layout_flexbox.addView(btn)
        //endregion

        //region 播报按钮
//        btn = new _Button(getContext(), "数字");
//        btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                DataContext dataContext = new DataContext(getContext());
//                numberSpeaker.readNumber(getContext(),12.86f);
//            }
//        });
//        flexboxLayout.addView(btn);
        //endregion

        //region 榛子云API发短信
//        btn = new _Button(getContext(), "API发短信");
//        btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                /**
//                 * 榛子短信api发送
//                 */
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        try {
//                            DataContext mDataContext = new DataContext(getContext());
//                            String number = mDataContext.getSetting(Setting.KEYS.sms_to_number, "18509513143").getString();
//                            OkHttpClient client =OkHttpClientUtil.getOkHttpClient();
//                            RequestBody requestBody = new FormBody.Builder()
//                                    .add("appId", "105574")
//                                    .add("appSecret", "a57124e6-98d0-4190-9564-4c3be70eb432")
//                                    .add("message", "api发送，验证码为: 1123")
//                                    .add("number", number).build();
//                            Request request = new Request.Builder().url("http://sms_developer.zhenzikj.com/sms/send.do").post(requestBody).build();
//                            Response response = client.newCall(request).execute();//发送请求
//                            String result = response.body().string();
//                            uiHandler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    new AlertDialog.Builder(getContext()).setTitle("发送短信完成").setMessage("result：" + result).show();
//                                }
//                            });
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }).start();
//            }
//        });
//        flexboxLayout.addView(btn);
        //endregion

        //region 榛子云JAR发短信
//        btn = new _Button(getContext(), "JAR发短信");
//        btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            DataContext mDataContext = new DataContext(getContext());
//                            String number = mDataContext.getSetting(Setting.KEYS.sms_to_number, "18509513143").getString();
//                            ZhenziSmsClient client = new ZhenziSmsClient("http://sms_developer.zhenzikj.com", "105574", "a57124e6-98d0-4190-9564-4c3be70eb432");
//                            Log.e(_TAG, "client: " + client);
//                            Map<String, String> params = new HashMap<String, String>();
//                            params.put("message", "jar发送，验证码为: 1123");
//                            params.put("number", number);
//                            String result = client.send(params);
//                            uiHandler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    new AlertDialog.Builder(getContext()).setTitle("发送短信完成").setMessage("result：" + result).show();
//                                }
//                            });
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            _Utils.printException(getContext(), e);
//                            _Utils.log2file("发送短信错误", e.getMessage());
//                        }
//                    }
//                }).start();
//            }
//        });
//        flexboxLayout.addView(btn);
        //endregion

        //region 榛子云剩余短信
//        btn = new _Button(getContext(), "剩余短信");
//        btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            /**
//                             * 榛子短信发送平台
//                             */
//                            ZhenziSmsClient client = new ZhenziSmsClient("http://sms_developer.zhenzikj.com", "105574", "a57124e6-98d0-4190-9564-4c3be70eb432");
//                            String result = client.balance();
//                            uiHandler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    try {
//                                        JSONObject jsonObject = new JSONObject(result);
//                                        new AlertDialog.Builder(getContext()).setMessage("剩余短信：" + jsonObject.getString("data") + " 条").show();
//                                    } catch (JSONException e) {
//                                        _Utils.log2file("错误代码：" + errorCode01, e.getMessage());
//                                    }
//                                }
//                            });
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            _Utils.printException(getContext(), e);
//                            _Utils.log2file("发送短信错误", e.getMessage());
//                        }
//                    }
//                }).start();
//            }
//        });
//        flexboxLayout.addView(btn);
        //endregion

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_button, container, false)
    }

}