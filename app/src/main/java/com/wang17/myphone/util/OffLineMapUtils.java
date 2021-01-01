package com.wang17.myphone.util;

import android.content.Context;
import android.os.Environment;

/**
 * Created by Administrator on 2017/7/22.
 */

public class OffLineMapUtils {
    /**
     * 获取map 缓存和读取目录
     */
    public static  String getSdCacheDir(Context context) {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            java.io.File fExternalStorageDirectory = Environment
                    .getExternalStorageDirectory();
            java.io.File autonaviDir = new java.io.File(
                    fExternalStorageDirectory, "amap");
            boolean result = false;
            if (!autonaviDir.exists()) {
                result = autonaviDir.mkdir();
            }
            java.io.File minimapDir = new java.io.File(autonaviDir,
                    "data");
            if (!minimapDir.exists()) {
                result = minimapDir.mkdir();
            }
            return minimapDir.toString() + "/";
        } else {
            return "";
        }
    }
}
