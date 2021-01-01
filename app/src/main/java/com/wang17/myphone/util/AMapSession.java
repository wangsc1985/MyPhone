package com.wang17.myphone.util;

import android.graphics.Color;

import com.amap.api.maps.model.LatLng;

/**
 * Created by Administrator on 2017/6/28.
 */

public class AMapSession {
    public static final int[] ALT_HEATMAP_GRADIENT_COLORS = {
            Color.argb(0, 0, 255, 255),
            Color.argb(255 / 3 * 2, 0, 255, 0),
            Color.rgb(125, 191, 0),
            Color.rgb(185, 71, 0),
            Color.rgb(255, 0, 0)
    };

    public static final float[] ALT_HEATMAP_GRADIENT_START_POINTS = { 0.0f,
            0.10f, 0.20f, 0.60f, 1.0f };
    /**
     * 一品尚都坐标
     */
    public static final LatLng POSITION_HOME = new LatLng(38.544783, 106.346331);
    /**
     * 银川市坐标
     */
    public static final LatLng POSITION_YINCHUAN = new LatLng(38.47, 106.27);
}
