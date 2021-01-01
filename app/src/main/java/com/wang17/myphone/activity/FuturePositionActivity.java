package com.wang17.myphone.activity;

import android.app.Service;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.model.Commodity;
import com.wang17.myphone.model.database.Setting;
import com.wang17.myphone.model.database.Position;
import com.wang17.myphone.util._AnimationUtils;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._LogUtils;
import com.wang17.myphone.util._OkHttpUtil;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.util._Utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FuturePositionActivity extends AppCompatActivity {

    private ListView listViewStock;
    private StockListdAdapter adapter;
    private DataContext mDataContext;
    private List<Position> positions;

    private FloatingActionButton actionButtonHome;
    private TextView textViewTime, textViewTotalProfit;

    //    private StockInfo info;
    private List<StockInfo> infoList;

    private boolean isSoundLoaded;
    private SoundPool mSoundPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attention_future);

        infoList = new ArrayList<>();
        mDataContext = new DataContext(this);

        positions = mDataContext.getPositions(1);
        Log.e("wangsc", " stock size: " + positions.size());

        textViewTime = findViewById(R.id.textView_time);
        textViewTotalProfit = findViewById(R.id.textView_totalProfit);

        adapter = new StockListdAdapter();
        listViewStock = findViewById(R.id.listView_stocks);
        listViewStock.setAdapter(adapter);
        listViewStock.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                eidtStockDialog(positions.get(position));
                return true;
            }
        });

        actionButtonHome = findViewById(R.id.actionButton_home);
        actionButtonHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        actionButtonHome.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                addStockDialog();
                return true;
            }
        });

        /**
         * 初始化声音
         */
        mSoundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
        mSoundPool.load(FuturePositionActivity.this, R.raw.clock, 1);
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                isSoundLoaded = true;
            }
        });
    }

    public void addStockDialog() {
        View view = View.inflate(this, R.layout.inflate_dialog_add_futures, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this).create();
        dialog.setView(view);
        dialog.setTitle("合约信息");

        final EditText editTextCode = view.findViewById(R.id.editText_code);
        final EditText editTextCost = view.findViewById(R.id.editText_cost);
        final EditText editTextAmount = view.findViewById(R.id.editText_amount);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "多单", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                try {
                    final StockInfo info = new StockInfo();
                    info.code = editTextCode.getText().toString().toUpperCase();
                    info.amount = Integer.parseInt(editTextAmount.getText().toString());

                    fillStockInfo(info, new OnLoadFinishedListener() {
                        @Override
                        public void onLoadFinished() {
                            if (info.name.isEmpty()) {
                                Snackbar.make(editTextCode, "合约代码不存在", Snackbar.LENGTH_SHORT).show();
                                return;
                            }

                            Position position = new Position();
                            position.setName(info.name);
                            position.setCost(Double.parseDouble(editTextCost.getText().toString()));
                            position.setCode(info.code);
                            position.setType(1);
                            position.setAmount(info.amount);
                            mDataContext.addPosition(position);

                            positions = mDataContext.getPositions(1);
                            infoList.clear();
                            adapter.notifyDataSetChanged();
                            Log.e("wangsc", "添加合约成功");
                            dialog.dismiss();
                        }
                    });
                } catch (NumberFormatException e) {
                    Snackbar.make(editTextCode, "成本价必须为数字", Snackbar.LENGTH_SHORT).show();
                } catch (Exception e) {
                    _Utils.printException(FuturePositionActivity.this, e);
                }
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "空单", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                try {
                    final StockInfo info = new StockInfo();
                    info.code = editTextCode.getText().toString().toUpperCase();
                    info.amount = Integer.parseInt(editTextAmount.getText().toString());

                    fillStockInfo(info, new OnLoadFinishedListener() {
                        @Override
                        public void onLoadFinished() {
                            if (info.name.isEmpty()) {
                                Snackbar.make(editTextCode, "合约代码不存在", Snackbar.LENGTH_SHORT).show();
                                return;
                            }

                            Position position = new Position();
                            position.setName(info.name);
                            position.setCost(Double.parseDouble(editTextCost.getText().toString()));
                            position.setCode(info.code);
                            position.setType(-1);
                            position.setAmount(info.amount);
                            mDataContext.addPosition(position);

                            positions = mDataContext.getPositions(1);
                            infoList.clear();
                            adapter.notifyDataSetChanged();
                            Log.e("wangsc", "添加合约成功");
                            dialog.dismiss();
                        }
                    });
                } catch (NumberFormatException e) {
                    Snackbar.make(editTextCode, "成本价必须为数字", Snackbar.LENGTH_SHORT).show();
                } catch (Exception e) {
                    _Utils.printException(FuturePositionActivity.this, e);
                }
            }
        });
        dialog.show();
    }

    public void eidtStockDialog(final Position position) {
        View view = View.inflate(this, R.layout.inflate_dialog_add_futures, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this).create();
        dialog.setView(view);
        dialog.setTitle("合约信息");

        final EditText editTextCode = view.findViewById(R.id.editText_code);
        final EditText editTextCost = view.findViewById(R.id.editText_cost);
        final EditText editTextAmount = view.findViewById(R.id.editText_amount);
        editTextCode.setText(position.getCode());
        editTextCost.setText(position.getCost() + "");
        editTextCode.setEnabled(false);
        editTextAmount.setText((int) position.getAmount() + "");
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new AlertDialog.Builder(FuturePositionActivity.this).setMessage("确认要删除当前合约吗？").setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDataContext.deletePosition(position.getId());
                        positions = mDataContext.getPositions(1);
                        infoList.clear();
                        adapter.notifyDataSetChanged();
                    }
                }).setNegativeButton("否", null).show();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                try {
                    final StockInfo info = new StockInfo();
                    info.code = editTextCode.getText().toString();
                    info.amount = Integer.parseInt(editTextAmount.getText().toString());

                    fillStockInfo(info, new OnLoadFinishedListener() {
                        @Override
                        public void onLoadFinished() {
                            if (info.name.isEmpty()) {
                                Snackbar.make(editTextCode, "合约代码不存在", Snackbar.LENGTH_SHORT).show();
                                return;
                            }

                            position.setName(info.name);
                            position.setCost(Double.parseDouble(editTextCost.getText().toString()));
                            position.setCode(info.code);
                            position.setAmount(info.amount);
                            mDataContext.editPosition(position);

                            positions = mDataContext.getPositions(1);
                            infoList.clear();
                            adapter.notifyDataSetChanged();
                            Log.e("wangsc", "添加合约成功");
                            dialog.dismiss();
                        }
                    });
                } catch (NumberFormatException e) {
                    Snackbar.make(editTextCode, "成本价必须为数字", Snackbar.LENGTH_SHORT).show();
                } catch (Exception e) {
                    _Utils.printException(FuturePositionActivity.this, e);
                }

            }
        });
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("wangsc", "onResume...");
        startTimer();
//        _Utils.acquireWakeLock(this, PowerManager.SCREEN_BRIGHT_WAKE_LOCK);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        _Utils.releaseWakeLock(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
        Log.e("wangsc", "onPause...");
    }


    private Timer timer;

    private void startTimer() {
        if (timer != null) {
            return;
        }
        try {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fillStockInfoList(infoList);
                        }
                    });
                }
            }, 0, 5000);
        } catch (Exception e) {
            _Utils.printException(this, e);
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    public interface OnLoadFinishedListener {
        void onLoadFinished();
    }

    protected class StockListdAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return positions.size();
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
            try {
                ViewHolder viewHolder;
                StockInfo info = new StockInfo();
                Position stock = positions.get(position);
                Log.e("wangsc", "attention future activity adapter stock name: " + stock.getName());
                if (convertView == null) {
                    convertView = View.inflate(FuturePositionActivity.this, R.layout.inflate_list_item_stock, null);
                    viewHolder = new ViewHolder();
                    viewHolder.textViewName = convertView.findViewById(R.id.textView_name);
                    viewHolder.textViewCode = convertView.findViewById(R.id.textView_code);
                    viewHolder.textViewIncrease = convertView.findViewById(R.id.textView_increase);
                    viewHolder.textViewProfit = convertView.findViewById(R.id.textView_profit);
                    convertView.setTag(viewHolder);
                    info.name = stock.getName();
                    info.code = stock.getCode();
                    info.cost = stock.getCost();
                    info.amount = stock.getAmount();
                    info.viewHolder = viewHolder;
                    info.type=stock.getType();
                    infoList.add(info);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }

                Log.e("wangsc", "++++++++++++++++++++++++++ " + info.name);

                viewHolder.textViewName.setText(stock.getName());
                viewHolder.textViewCode.setText(stock.getCode());
                viewHolder.textViewIncrease.setText(new DecimalFormat("0.00%").format(0));
                viewHolder.textViewProfit.setText(new DecimalFormat("0.00").format(0));
                if(stock.getType()==1){
                    viewHolder.textViewCode.setTextColor(Color.RED);
                }else{
                    viewHolder.textViewCode.setTextColor(Color.GREEN);
                }

            } catch (Exception e) {
                _Utils.printException(FuturePositionActivity.this, e);
            }
            return convertView;
        }
    }

    class ViewHolder {
        //条目的布局文件中有什么组件，这里就定义有什么属性
        TextView textViewName;
        TextView textViewCode;
        TextView textViewIncrease;
        TextView textViewProfit;
    }

    /**
     * 向info里面填充StockInfo值，在调用之前，需要info赋值code。
     *
     * @param info
     * @param onLoadFinishedListener
     */
    private void fillStockInfo(final StockInfo info, final OnLoadFinishedListener onLoadFinishedListener) {

        //http://hq.sinajs.cn/list=sh601555
        //var hq_str_sh601555="东吴证券,11.290,11.380,11.160,11.350,11.050,11.160,11.170,61431561,687740501.000,3500,11.160,144700,11.150,98500,11.140,78500,11.130,99200,11.120,143700,11.170,99700,11.180,28700,11.190,41500,11.200,41500,11.210,2019-04-17,15:00:00,00";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "https://hq.sinajs.cn/list=" + info.code;
                    Log.e("wangsc", "code: " + info.code + "" + url);

                    OkHttpClient client = _OkHttpUtil.client;
                    Request request = new Request.Builder().url(url).build();
                    Response response = null;
                    response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        try {
                            String sss = response.body().string();
                            String[] result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",");
                            if (result.length == 0) {
                                return;
                            }

                            if (result.length <= 1)
                                return;
                            double open = Double.parseDouble(result[2]);
                            info.name = result[0];
                            info.price = Double.parseDouble(result[8]);
                            info.increase = (info.price - open) / open;
                            info.time = result[1];

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (onLoadFinishedListener != null)
                                        onLoadFinishedListener.onLoadFinished();
                                }
                            });
                        } catch (Exception e) {
                            _Utils.printException(FuturePositionActivity.this, e);
                            return;
                        }
                    } else {
                        _LogUtils.log2file("获取数据失败...");
                    }

                } catch (Exception e) {
                    _Utils.printException(FuturePositionActivity.this, e);
//                    e.printStackTrace();
                }

            }
        }).start();
    }

    private int loadStockCount = 0;

    private void fillStockInfoList(final List<StockInfo> infoList) {

        //http://hq.sinajs.cn/list=sh601555
        //var hq_str_sh601555="东吴证券,11.290,11.380,11.160,11.350,11.050,11.160,11.170,61431561,687740501.000,3500,11.160,144700,11.150,98500,11.140,78500,11.130,99200,11.120,143700,11.170,99700,11.180,28700,11.190,41500,11.200,41500,11.210,2019-04-17,15:00:00,00";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final double[] totalProfit = {0};

                    Log.e("wangsc", "attention future activity fillStockInfoList infolist size: " + infoList.size());
                    for (final StockInfo info1 : infoList) {
                        final StockInfo info = info1;

                        String url = "https://hq.sinajs.cn/list=" + info.code;

                        OkHttpClient client = _OkHttpUtil.client;
                        Request request = new Request.Builder().url(url).build();
                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            try {
                                String sss = response.body().string();
                                String[] result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",");

                                double open = Double.parseDouble(result[2]);
                                info.name = result[0];
                                info.price = Double.parseDouble(result[8]);
                                info.increase = (info.price - open) / open;
                                info.time = result[1];
                                mTime = info.time;
                                Log.e("wangsc", "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee" + mTime);

                                double profitPoints = info.type * (info.price - info.cost);

                                Commodity commodity = findCommodity(info.code);
                                totalProfit[0] += profitPoints * info.amount * commodity.unit;

                                Log.e("wangsc", "code: " + info.code + " , profitPoints: " + totalProfit[0] + " , cose: " + commodity.cose + " , unit: " + commodity.unit);
                                final String ff = getFormat(commodity);
//                            if (ff.length() > format.length()) {
//                                format = ff;
//                            }

                                final double finalProfitPoints = profitPoints;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        info.viewHolder.textViewIncrease.setText(new DecimalFormat(ff).format(info.price));
                                        if (info.increase > 0) {
                                            info.viewHolder.textViewIncrease.setTextColor(Color.RED);
                                        } else if (info.increase == 0) {
                                            info.viewHolder.textViewIncrease.setTextColor(Color.WHITE);
                                        } else {
                                            info.viewHolder.textViewIncrease.setTextColor(Color.GREEN);
                                        }

                                        info.viewHolder.textViewProfit.setText(new DecimalFormat(ff).format(finalProfitPoints));
                                        if (finalProfitPoints > 0) {
                                            info.viewHolder.textViewProfit.setTextColor(Color.RED);
                                        } else if (finalProfitPoints == 0) {
                                            info.viewHolder.textViewProfit.setTextColor(Color.WHITE);
                                        } else {
                                            info.viewHolder.textViewProfit.setTextColor(Color.GREEN);
                                        }
                                        textViewTime.setText(info.time.substring(0, 2) + ":" + info.time.substring(2, 4) + ":" + info.time.substring(4, 6));
                                    }
                                });


                            } catch (Exception e) {
                                return;
                            }
                        } else {
                            _LogUtils.log2file("获取数据失败...");
                            return;
                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            double averageTotalProfit;
                            if (infoList.size() == 0)
                                return;
                            averageTotalProfit = totalProfit[0];
                            Log.e("wangsc", Math.abs(averageTotalProfit - preAverageTotalProfit) * 100 + "");
                            if (Math.abs(averageTotalProfit - preAverageTotalProfit) > 20 / infoList.size()) {
                                preAverageTotalProfit = averageTotalProfit;
                                String msg = new DecimalFormat("0").format(averageTotalProfit);
                                if (averageTotalProfit > 0) {
                                    msg = "加" + msg;
                                }
//                                if (!isFirst) {
//                                    _Utils.speaker(AttentionFutureActivity.this.getApplicationContext(), msg);
//                                }
                            }
                            isFirst = false;

                            Log.e("wangsc", "count: " + loadStockCount);
                            if (mDataContext.getSetting(Setting.KEYS.is_stock_load_noke, false).getBoolean() && ++loadStockCount >= 20) {

                                if (!isSoundLoaded) {
                                    mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                                        @Override
                                        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                                            isSoundLoaded = true;
                                            soundPool.play(1, 0.6f, 0.6f, 0, 0, 1);
                                        }
                                    });
                                } else {
                                    mSoundPool.play(1, 0.6f, 0.6f, 0, 0, 1);
                                }
                                loadStockCount = 0;
                            }

                            textViewTotalProfit.setText(new DecimalFormat("#0,000").format(averageTotalProfit));
                            if (averageTotalProfit > 0) {
                                textViewTotalProfit.setTextColor(Color.RED);
                            } else if (averageTotalProfit == 0) {
                                textViewTotalProfit.setTextColor(Color.WHITE);
                            } else {
                                textViewTotalProfit.setTextColor(Color.GREEN);
                            }

                            if (null != mTime && !mTime.equals(preTime)) {
                                _AnimationUtils.heartBeat(actionButtonHome);
                                preTime = mTime;
                            }
                        }
                    });

                } catch (Exception e) {
//                    e.printStackTrace();
//                    _Utils.printException(getApplicationContext(), e);
                }

            }
        }).start();
    }

    private boolean isFirst = true;

    private String preTime, mTime;

    public Commodity findCommodity(String code) {
        for (Commodity commodity : _Session.commoditys) {
            if (commodity.item.toLowerCase().equals(parseItem(code).toLowerCase())) {
                return commodity;
            }
        }
        return null;
    }

    public String getFormat(Commodity commodity) {
        if (commodity != null) {
            double i = 1;
            int fc = 0;
            while (commodity.cose / i < 1) {
                i /= 10;
                fc++;
            }
            String format = "0";
            for (int j = 0; j < fc; j++) {
                if (j == 0)
                    format += ".0";
                else
                    format += "0";
            }
            return format;
        }
        return "0";
    }

    private String parseItem(String code) {
        StringBuffer item = new StringBuffer();
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (!Character.isDigit(c)) {
                item.append(c);
            } else {
                break;
            }
        }
        return item.toString();
    }

    private double preAverageTotalProfit = 0;


    class StockInfo {
        public String time;
        public String name;
        public String code;
        public double cost;
        public double price;
        public double increase;
        public int amount;
        public int type;
        ViewHolder viewHolder;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            AudioManager audio = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    audio.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_SHOW_UI);
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    audio.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_SHOW_UI);
                    return true;
                default:
                    break;
            }
        } catch (Exception e) {
            _Utils.printException(FuturePositionActivity.this, e);
        }
        return super.onKeyDown(keyCode, event);
    }
}
