package com.wang17.myphone.activity;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.model.StockInfo;
import com.wang17.myphone.model.ViewHolder;
import com.wang17.myphone.model.database.Setting;
import com.wang17.myphone.model.database.Position;
import com.wang17.myphone.util._AnimationUtils;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._LogUtils;
import com.wang17.myphone.util._OkHttpUtil;
import com.wang17.myphone.util._Utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class StockPositionActivity extends AppCompatActivity {

    private ListView listViewStock;
    private StockListdAdapter adapter;
    private DataContext mDataContext;
    private List<Position> positions;

    private FloatingActionButton actionButtonHome;
    private TextView textViewSzzsPrice, textViewSzzsIncrease, textViewSzczPrice, textViewSzczIncrease, textViewZxbzPrice, textViewZxbzIncrease, textViewTime, textViewTotalProfit;
    private LinearLayout layoutSzzs, layoutSzcz, layoutZxbz;

    //    private StockInfo info;
    private List<StockInfo> infoList;

    private boolean isSoundLoaded;
    private SoundPool mSoundPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attention_stock);

        infoList = new ArrayList<>();
        mDataContext = new DataContext(this);

        positions = mDataContext.getPositions(0);

        textViewTime = findViewById(R.id.textView_time);
        textViewTotalProfit = findViewById(R.id.textView_totalProfit);

        layoutSzcz = findViewById(R.id.llayoutSzcz);
        layoutSzzs = findViewById(R.id.llayoutSzzs);
        layoutZxbz = findViewById(R.id.llayoutZxbz);

        adapter = new StockListdAdapter();
        listViewStock = findViewById(R.id.listView_stocks);
        listViewStock.setAdapter(adapter);

        textViewSzzsPrice = findViewById(R.id.textViewSzzsPrice);
        textViewSzzsIncrease = findViewById(R.id.textViewSzzsIncrease);
        textViewSzczPrice = findViewById(R.id.textViewSzczPrice);
        textViewSzczIncrease = findViewById(R.id.textViewSzczIncrease);
        textViewZxbzPrice = findViewById(R.id.textViewZxbPrice);
        textViewZxbzIncrease = findViewById(R.id.textViewZxbIncrease);

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
                startActivity(new Intent(StockPositionActivity.this, StockPositionHistoryActivity.class));
                return true;
            }
        });
        /**
         * 初始化声音
         */
        mSoundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
        mSoundPool.load(StockPositionActivity.this, R.raw.clock, 1);
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                isSoundLoaded = true;
            }
        });
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
                            reflushSZZS();
                            reflushSZCZ();
                            reflushZXBZ();
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

    private void reflushSZZS() {
        final StockInfo info = new StockInfo();
        info.code = "000001";
        info.exchange = "sh";
        fillStockInfo(info, new OnLoadFinishedListener() {
            @Override
            public void onLoadFinished() {
                textViewTime.setText(info.time);
                textViewSzzsPrice.setText(new DecimalFormat("0.00").format(info.price));
                textViewSzzsIncrease.setText(new DecimalFormat("0.00%").format(info.increase));
                if (info.increase >= 0) {
                    layoutSzzs.setBackgroundColor(getResources().getColor(R.color.a));
                } else {
                    layoutSzzs.setBackgroundColor(getResources().getColor(R.color.DARK_GREEN));
                }
            }
        });
    }

    private void reflushSZCZ() {
        final StockInfo info = new StockInfo();
        info.code = "399001";
        info.exchange = "sz";
        fillStockInfo(info, new OnLoadFinishedListener() {
            @Override
            public void onLoadFinished() {
                textViewSzczPrice.setText(new DecimalFormat("0.00").format(info.price));
                textViewSzczIncrease.setText(new DecimalFormat("0.00%").format(info.increase));
                if (info.increase >= 0) {
                    layoutSzcz.setBackgroundColor(getResources().getColor(R.color.a));
                } else {
                    layoutSzcz.setBackgroundColor(getResources().getColor(R.color.DARK_GREEN));
                }
            }
        });
    }

    private void reflushZXBZ() {
        final StockInfo info = new StockInfo();
        info.code = "399005";
        info.exchange = "sz";
        fillStockInfo(info, new OnLoadFinishedListener() {
            @Override
            public void onLoadFinished() {
                textViewZxbzPrice.setText(new DecimalFormat("0.00").format(info.price));
                textViewZxbzIncrease.setText(new DecimalFormat("0.00%").format(info.increase));
                if (info.increase >= 0) {
                    layoutZxbz.setBackgroundColor(getResources().getColor(R.color.a));
                } else {
                    layoutZxbz.setBackgroundColor(getResources().getColor(R.color.DARK_GREEN));
                }
            }
        });
    }

    private void reflushDQS() {
//        http://hq.sinajs.cn/list=gb_msft （微软）
//        http://hq.sinajs.cn/list=gb_dji （ 道指）
//        http://hq.sinajs.cn/list=gb_ixic （纳斯达克）
//        http://hq.sinajs.cn/list=hkHSI (恒生指数）
    }

    private void reflushNSDK() {

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
                if (convertView == null) {
                    convertView = View.inflate(StockPositionActivity.this, R.layout.inflate_list_item_stock, null);
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
                    info.exchange = stock.getExchange();
                    info.viewHolder = viewHolder;
                    infoList.add(info);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }

                viewHolder.textViewName.setText(stock.getName());
                viewHolder.textViewCode.setText(stock.getCode());
                viewHolder.textViewIncrease.setText(new DecimalFormat("0.00%").format(0));
                viewHolder.textViewProfit.setText(new DecimalFormat("0.00%").format(0));

            } catch (Exception e) {
                _Utils.printException(StockPositionActivity.this, e);
            }
            return convertView;
        }
    }

    private void log(String msg) {
        Log.e("wangsc", msg);
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
                    String url = "https://hq.sinajs.cn/list=" + info.exchange + info.code;

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
                            info.price = Double.parseDouble(result[3]);
                            info.increase = (info.price - open) / open;
                            info.time = result[31];

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (onLoadFinishedListener != null)
                                        onLoadFinishedListener.onLoadFinished();
                                }
                            });
                        } catch (Exception e) {
                            return;
                        }
                    } else {
                        _LogUtils.log2file("获取数据失败...");
                    }

                } catch (Exception e) {

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
                    double totalAmount = 0;
                    if (infoList.size() <= 0)
                        return;
                    for (StockInfo stockInfo : infoList) {
                        final StockInfo info = stockInfo;
                        String url = "https://hq.sinajs.cn/list=" + info.exchange + info.code;

                        OkHttpClient client = _OkHttpUtil.client;
                        Request request = new Request.Builder().url(url).build();
                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            try {
                                String sss = response.body().string();
                                String[] result = sss.substring(sss.indexOf("\"")).replace("\"", "").split(",");

                                double open = Double.parseDouble(result[2]);
                                info.name = result[0];
                                info.price = Double.parseDouble(result[3]);
                                info.increase = (info.price - open) / open;
                                info.time = result[31];
                                mTime = info.time;

                                final double profit = (info.price - info.cost) / info.cost;
                                totalProfit[0] += profit * info.amount * info.cost * 100;
                                totalAmount += info.amount * info.cost * 100;

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        info.viewHolder.textViewIncrease.setText(new DecimalFormat("0.00%").format(info.increase));
                                        if (info.increase > 0) {
                                            info.viewHolder.textViewIncrease.setTextColor(Color.RED);
                                        } else if (info.increase == 0) {
                                            info.viewHolder.textViewIncrease.setTextColor(Color.WHITE);
                                        } else {
                                            info.viewHolder.textViewIncrease.setTextColor(Color.GREEN);
                                        }

                                        info.viewHolder.textViewProfit.setText(new DecimalFormat("0.00%").format(profit));
                                        if (profit > 0) {
                                            info.viewHolder.textViewProfit.setTextColor(Color.RED);
                                        } else if (profit == 0) {
                                            info.viewHolder.textViewProfit.setTextColor(Color.WHITE);
                                        } else {
                                            info.viewHolder.textViewProfit.setTextColor(Color.GREEN);
                                        }
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
                    final double finalTotalAmount = totalAmount;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            double averageTotalProfit;
                            if (infoList.size() == 0)
                                return;
                            averageTotalProfit = totalProfit[0] / finalTotalAmount;
                            if (Math.abs(averageTotalProfit - preAverageTotalProfit) * 100 > 0.3) {
                                preAverageTotalProfit = averageTotalProfit;
                                String msg = new DecimalFormat("0.00%").format(averageTotalProfit);
                                if (averageTotalProfit > 0) {
                                    msg = "加" + msg;
                                }
//                                if (!isFirst) {
//                                    _Utils.speaker(AttentionStockActivity.this.getApplicationContext(), msg);
//                                }
                            }
                            isFirst = false;

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

//                            _Utils.speaker(AttentionStockActivity.this,new DecimalFormat("0.00").format(averageProfit*100));
                            textViewTotalProfit.setText(new DecimalFormat("0.00%").format(averageTotalProfit));
                            if (averageTotalProfit > 0) {
                                textViewTotalProfit.setTextColor(Color.RED);
                            } else if (averageTotalProfit == 0) {
                                textViewTotalProfit.setTextColor(Color.WHITE);
                            } else {
                                textViewTotalProfit.setTextColor(Color.GREEN);
                            }

                            if (!mTime.equals(preTime)) {
                                _AnimationUtils.heartBeat(actionButtonHome);
                                preTime = mTime;
                            }
//                            AnimationUtils.setRorateAnimationOnce(AttentionStockActivity.this, actionButtonHome);
                        }
                    });

                } catch (Exception e) {
//                    e.printStackTrace();
//                    _Utils.printException(getApplicationContext(), e);
                }

            }
        }).start();
    }

    private String preTime, mTime;

    private boolean isFirst = true;

    //缩放
//    public ObjectAnimator scaleY;

//    public void stopSuofang() {
//        if (scaleY != null)
//            scaleY.cancel();
//    }

    private double preAverageTotalProfit = 0;

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
            _Utils.printException(StockPositionActivity.this, e);
        }
        return super.onKeyDown(keyCode, event);
    }
}
//A股股票&基金
//        http://hq.sinajs.cn/list=sh601006
//        http://hq.sinajs.cn/list=sh502007
//        A股指数
//        http://hq.sinajs.cn/list=s_sz399001
//        港股股票
//        http://hq.sinajs.cn/list=hk02333
//        http://hq.sinajs.cn/list=rt_hkCSCSHQ #沪港通资金流量
//        港股指数
//        http://hq.sinajs.cn/list=int_hangseng
//        http://hq.sinajs.cn/list=rt_hkHSI
//        http://hq.sinajs.cn/list=hkHSI,hkHSCEI,hkHSCCI #恒生指数，恒生国企指数，恒生红筹指数
//        美股股票&基金
//        http://hq.sinajs.cn/list=gb_amzn
//        http://hq.sinajs.cn/list=usr_amzn
//        http://hq.sinajs.cn/list=usr_russ
//        美股指数
//        http://hq.sinajs.cn/list=int_nasdaq
//        http://hq.sinajs.cn/list=gb_ixic #纳斯达克指数
//        http://hq.sinajs.cn/list=int_dji
//        http://hq.sinajs.cn/list=int_sp500
//        http://hq.sinajs.cn/list=int_ftse #伦敦指数
//        http://hq.sinajs.cn/list=int_bloombergeuropean500 #彭博欧洲500指数
//        http://hq.sinajs.cn/list=int_dax30,int_djstoxx50
//        外汇行情
//        http://hq.sinajs.cn/list=XAUUSD
//        http://hq.sinajs.cn/list=DINIW #美元指数
//        黄金&白银
//        http://hq.sinajs.cn/list=hf_XAU
//        http://hq.sinajs.cn/list=hf_XAG
//        http://hq.sinajs.cn/list=hf_GC #COMEX黄金
//        http://hq.sinajs.cn/list=hf_SI #COMEX白银
//        http://hq.sinajs.cn/list=hf_AUTD #黄金TD
//        http://hq.sinajs.cn/list=hf_AGTD #白银TD
//        http://hq.sinajs.cn/list=AU0 #黄金期货
//        http://hq.sinajs.cn/list=AG0 #白银期货
//        http://hq.sinajs.cn/list=hf_CL #NYMEX原油
//        期货
//        http://hq.sinajs.cn/list=CFF_LIST #金融期货合约
//        http://finance.sina.com.cn/iframe/futures_info_cff.js #商品与金融期货合约
//        http://hq.sinajs.cn/?list=CFF_RE_IF1705 #合约行情
//        期权合约的月份
//        http://stock.finance.sina.com.cn/futures/api/openapi.php/StockOptionService.getStockName
//        期权合约到期日
//        http://stock.finance.sina.com.cn/futures/api/openapi.php/StockOptionService.getRemainderDay?date=201705
//        看涨期权合约
//        http://hq.sinajs.cn/list=OP_UP_5100501705
//        看跌期权合约
//        http://hq.sinajs.cn/list=OP_DOWN_5100501705
//        期权行情
//        http://hq.sinajs.cn/list=CON_OP_10000869
//        http://hq.sinajs.cn/list=CON_ZL_10000869
//        http://hq.sinajs.cn/list=CON_SO_10000869
//        热门股票
//        http://finance.sina.com.cn/realstock/company/hotstock_daily_a.js
//        新股日历
//        http://vip.stock.finance.sina.com.cn/corp/view/iframe/vAK_NewStockIssueFrame_2015.php?num=10
//        定增列表
//        http://vip.stock.finance.sina.com.cn/corp/view/vAK_IncreaseStockIssueFrame_2015.php?num=10
//        基金公司
//        http://vip.stock.finance.sina.com.cn/fund_center/api/jsonp.php/var%20companyList=/NetValue_Service.getAllCompany