package com.wang17.myphone.model;

import android.util.Log;

/**
 * Created by Administrator on 2017/3/22.
 */

public class ProgressEvent {
    private String time1;
    private String time2;
    private long progress;
    private long max;

    public ProgressEvent(long progress, long max) {
        this.progress = progress;
        this.max = max;
        try {
            time1 = DateTime.toSpanString(progress);
            time2 = DateTime.toSpanString(max);
        } catch (Exception e) {
            Log.e("wangsc", e.getMessage());
        }
    }

    public String getTime1() {
        return time1;
    }

    public String getTime2() {
        return time2;
    }

    public int getProgress() {
        return (int) (progress / 60000);
    }

    public int getMax() {
        return (int) (max / 60000);
    }
}
