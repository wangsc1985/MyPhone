package com.wang17.myphone.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.wang17.myphone.MainActivity;
import com.wang17.myphone.R;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.receiver.LockReceiver;
import com.wang17.myphone.service.MyAccessbilityService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.content.Context.ACTIVITY_SERVICE;
import static com.ta.utdid2.android.utils.AESUtils.TAG;

/**
 * Created by 阿弥陀佛 on 2016/10/18.
 */

public class _Utils {

    private static final String _TAG = "wangsc";

    /**
     * 隐藏虚拟按键，并且全屏
     */
    public static void hideBottomUIMenu(Activity activity) {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = activity.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = activity.getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    public static boolean isAppRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = "com.wangsc.lovehome";

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            Log.e("wangsc", "null");
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            Log.e("wangsc", appProcess.processName);
            if (appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 模拟点击HOME按钮
     *
     * @param context
     */
    public static void clickHomeButton(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static TextToSpeech textToSpeech = null;//创建自带语音对象

    public static void speaker(final Context context, final String msg) {
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setPitch(1.0f);//方法用来控制音调
                    textToSpeech.setSpeechRate(1.2f);//用来控制语速

                    //判断是否支持下面语言
                    int result = textToSpeech.setLanguage(Locale.CHINA);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(context, "SIMPLIFIED_CHINESE数据丢失或不支持", Toast.LENGTH_SHORT).show();
                    } else {
                        textToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null);//输入中文，若不支持的设备则不会读出来
                    }
                }
            }
        });
    }

    public static void speaker(final Context context, final String msg, float pitch) {
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setPitch(pitch);//方法用来控制音调
                    textToSpeech.setSpeechRate(1.2f);//用来控制语速

                    //判断是否支持下面语言
                    int result = textToSpeech.setLanguage(Locale.CHINA);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(context, "SIMPLIFIED_CHINESE数据丢失或不支持", Toast.LENGTH_SHORT).show();
                    } else {
                        textToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null);//输入中文，若不支持的设备则不会读出来
                    }
                }
            }
        });
    }

    public static void speaker(final Context context, final String msg, float pitch, float speech) {
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setPitch(pitch);//方法用来控制音调
                    textToSpeech.setSpeechRate(speech);//用来控制语速

                    //判断是否支持下面语言
                    int result = textToSpeech.setLanguage(Locale.CHINA);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(context, "SIMPLIFIED_CHINESE数据丢失或不支持", Toast.LENGTH_SHORT).show();
                    } else {
                        textToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null);//输入中文，若不支持的设备则不会读出来
                    }
                }
            }
        });
    }


    private static SoundPool soundpool;
    private static Map<Integer, Integer> soundmap = new HashMap<Integer, Integer>();

    public static void readNumber(Context context, float num) {
        try {
            boolean isMan = true;
            if (num < 0)
                isMan = false;

            NumToWord numToWord = new NumToWord();
            String number = numToWord.cvt(num + "", true);

            Log.e(_TAG, number);
            if (Build.VERSION.SDK_INT > 21) {
                SoundPool.Builder builder = new SoundPool.Builder();
                //传入音频数量
                builder.setMaxStreams(5);
                //AudioAttributes是一个封装音频各种属性的方法
                AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
                //设置音频流的合适的属性
                attrBuilder.setLegacyStreamType(AudioManager.STREAM_SYSTEM);//STREAM_MUSIC
                //加载一个AudioAttributes
                builder.setAudioAttributes(attrBuilder.build());
                soundpool = builder.build();
            } else {
                soundpool = new SoundPool(5, AudioManager.STREAM_SYSTEM, 0);
            }
            //
            if (isMan) {
                soundmap.put(1, soundpool.load(context, R.raw.s10, 1));
                soundmap.put(2, soundpool.load(context, R.raw.s100, 1));
                soundmap.put(3, soundpool.load(context, R.raw.s1000, 1));
                soundmap.put(4, soundpool.load(context, R.raw.s10000, 1));
                soundmap.put(5, soundpool.load(context, R.raw.s0, 1));
                soundmap.put(6, soundpool.load(context, R.raw.s1, 1));
                soundmap.put(7, soundpool.load(context, R.raw.s2, 1));
                soundmap.put(8, soundpool.load(context, R.raw.s3, 1));
                soundmap.put(9, soundpool.load(context, R.raw.s4, 1));
                soundmap.put(10, soundpool.load(context, R.raw.s5, 1));
                soundmap.put(11, soundpool.load(context, R.raw.s6, 1));
                soundmap.put(12, soundpool.load(context, R.raw.s7, 1));
                soundmap.put(13, soundpool.load(context, R.raw.s8, 1));
                soundmap.put(14, soundpool.load(context, R.raw.s9, 1));
                soundmap.put(15, soundpool.load(context, R.raw.sd, 1));
            } else {
                soundmap.put(1, soundpool.load(context, R.raw.s_10, 1));
                soundmap.put(2, soundpool.load(context, R.raw.s_100, 1));
                soundmap.put(3, soundpool.load(context, R.raw.s_1000, 1));
                soundmap.put(4, soundpool.load(context, R.raw.s_10000, 1));
                soundmap.put(5, soundpool.load(context, R.raw.s_0, 1));
                soundmap.put(6, soundpool.load(context, R.raw.s_1, 1));
                soundmap.put(7, soundpool.load(context, R.raw.s_2, 1));
                soundmap.put(8, soundpool.load(context, R.raw.s_3, 1));
                soundmap.put(9, soundpool.load(context, R.raw.s_4, 1));
                soundmap.put(10, soundpool.load(context, R.raw.s_5, 1));
                soundmap.put(11, soundpool.load(context, R.raw.s_6, 1));
                soundmap.put(12, soundpool.load(context, R.raw.s_7, 1));
                soundmap.put(13, soundpool.load(context, R.raw.s_8, 1));
                soundmap.put(14, soundpool.load(context, R.raw.s_9, 1));
                soundmap.put(15, soundpool.load(context, R.raw.s_d, 1));
            }
            try {
Thread.sleep(1000);
                    for (int i = 0; i < number.length(); i++) {
                            switch (number.charAt(i)) {
                                case '十':
                                    soundpool.play(soundmap.get(1), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                                case '百':
                                    soundpool.play(soundmap.get(2), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                                case '千':
                                    soundpool.play(soundmap.get(3), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                                case '万':
                                    soundpool.play(soundmap.get(4), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                                case '零':
                                    soundpool.play(soundmap.get(5), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                                case '一':
                                    soundpool.play(soundmap.get(6), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                                case '二':
                                    soundpool.play(soundmap.get(7), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                                case '三':
                                    soundpool.play(soundmap.get(8), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                                case '四':
                                    soundpool.play(soundmap.get(9), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                                case '五':
                                    soundpool.play(soundmap.get(10), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                                case '六':
                                    soundpool.play(soundmap.get(11), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                                case '七':
                                    soundpool.play(soundmap.get(12), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                                case '八':
                                    soundpool.play(soundmap.get(13), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                                case '九':
                                    soundpool.play(soundmap.get(14), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                                case '点':
                                    soundpool.play(soundmap.get(15), 1, 1, 0, 0, 1);
                                    soundpool.wait();
                                    break;
                            }
                    }
            } catch (InterruptedException e) {
            }
        } catch (IllegalArgumentException e) {
            Log.e(_TAG, e.getMessage());
            e.printStackTrace();
        } finally {
//            soundpool.release();
        }
    }


    /**
     * 检测辅助功能是否开启<br>
     * 方 法 名：isAccessibilitySettingsOn <br>
     * 创 建 人 <br>
     * 创建时间：2016-6-22 下午2:29:24 <br>
     * 修 改 人： <br>
     * 修改日期： <br>
     * @param mContext
     * @return boolean
     */
//    private boolean isAccessibilitySettingsOn(Context mContext) {
//        int accessibilityEnabled = 0;
//        // TestService为对应的服务
//        final String service = mContext.getPackageName() + "/" + TestService.class.getCanonicalName();
//        Log.i(TAG, "service:" + service);
//        // com.z.buildingaccessibilityservices/android.accessibilityservice.AccessibilityService
//        try {
//            accessibilityEnabled = Settings.Secure.getInt(mContext.getApplicationContext().getContentResolver(),
//                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
//            Log.v(TAG, "accessibilityEnabled = " + accessibilityEnabled);
//        } catch (Settings.SettingNotFoundException e) {
//            Log.e(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
//        }
//        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
//
//        if (accessibilityEnabled == 1) {
//            Log.v(TAG, "***ACCESSIBILITY IS ENABLED*** -----------------");
//            String settingValue = Settings.Secure.getString(mContext.getApplicationContext().getContentResolver(),
//                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
//            // com.z.buildingaccessibilityservices/com.z.buildingaccessibilityservices.TestService
//            if (settingValue != null) {
//                mStringColonSplitter.setString(settingValue);
//                while (mStringColonSplitter.hasNext()) {
//                    String accessibilityService = mStringColonSplitter.next();
//
//                    Log.v(TAG, "-------------- > accessibilityService :: " + accessibilityService + " " + service);
//                    if (accessibilityService.equalsIgnoreCase(service)) {
//                        Log.v(TAG, "We've found the correct setting - accessibility is switched on!");
//                        return true;
//                    }
//                }
//            }
//        } else {
//            Log.v(TAG, "***ACCESSIBILITY IS DISABLED***");
//        }
//        return false;
//    }
//

    /**
     * 行房节欲期
     *
     * @param birthday
     * @return
     */
    public static long getTargetInMillis(DateTime birthday) {
        DateTime now = new DateTime();
        int age = (now.getYear() - birthday.getYear()) + 1;
        if (now.getMonth() < birthday.getMonth()) {
            age -= 1;
        }
        double day = 100;
        if (age < 18) {
            day = -1;
        } else if (age >= 18 && age < 20) {
            day = 3;
        } else if (age >= 20 && age < 30) {
            day = 4 + (age - 20) * 0.4;
        } else if (age >= 30 && age < 40) {
            day = 8 + (age - 30) * 0.8;
        } else if (age >= 40 && age < 50) {
            day = 16 + (age - 40) * 0.5;
        } else if (age >= 50 && age < 60) {
            day = 21 + (age - 50) * 0.9;
        }
        return (long) (day * 24 * 3600000);
    }

    public static int getTargetInHours(DateTime birthday) {
        return (int) (getTargetInMillis(birthday) / 3600000);
    }

    public static void printException(Context context, Exception e) {
        try {
            String msg = saveException(context,e);
            new AlertDialog.Builder(context).setMessage(msg).setCancelable(false).setPositiveButton("知道了", null).show();
        } catch (Exception e1) {
        }
    }

    public static String saveException(Context context, Exception e) {
        String msg = getExceptionStr(e);
        new DataContext(context).addLog("err","运行错误",msg);
        return msg;
    }

    public static String getExceptionStr(Exception e) {
        String msg = "";
        try {
            if (e.getStackTrace().length == 0)
                return "";
            for (StackTraceElement ste : e.getStackTrace()) {
                if (ste.getClassName().contains(MainActivity.class.getPackage().getName())) {
                    msg += "类名：\n" + ste.getClassName()
                            + "\n方法名：\n" + ste.getMethodName()
                            + "\n行号：" + ste.getLineNumber()
                            + "\n错误信息：\n" + e.getMessage() + "\n";
                }
            }
        } catch (Exception exception) {
        }
        return msg;
    }

    public static void e(Object log){
        Log.e("wangsc",log.toString());
    }

    public static String[] getFilesWithSuffix(String path, final String suffix) {
        File file = new File(path);
        String[] files = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name != null && name.endsWith(suffix))
                    return true;
                else
                    return false;
            }
        });
        return files == null ? new String[]{} : files;
    }

    public static DevicePolicyManager policyManager;
    public static ComponentName componentName;

    public static void LockScreen(Context context) {
        policyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(context, LockReceiver.class);
        if (policyManager.isAdminActive(componentName)) {
            Log.e("wangsc", "判断是否有权限(激活了设备管理器)");
            //判断是否有权限(激活了设备管理器)
            policyManager.lockNow();
            // 直接锁屏
            android.os.Process.killProcess(android.os.Process.myPid());
        } else {
            Log.e("wangsc", "激活设备管理器");
            activeManager(context);
            //激活设备管理器获取权限
        }
    }

    // 解除绑定
    public static void Bind(Context context) {
        if (componentName != null) {
            policyManager.removeActiveAdmin(componentName);
            activeManager(context);
        }
    }

    private static void activeManager(Context context) {
        //使用隐式意图调用系统方法来激活指定的设备管理器
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "一键锁屏");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        Log.e("wangsc", "激活了设备管理器");
    }

    /**
     * 获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
     * <p>
     * PARTIAL_WAKE_LOCK :保持CPU 运转，屏幕和键盘灯是关闭的。
     * SCREEN_DIM_WAKE_LOCK ：保持CPU 运转，允许保持屏幕显示但有可能是灰的，关闭键盘灯
     * SCREEN_BRIGHT_WAKE_LOCK ：保持CPU 运转，保持屏幕高亮显示，关闭键盘灯
     * FULL_WAKE_LOCK ：保持CPU 运转，保持屏幕高亮显示，键盘灯也保持亮度
     *
     * @param context
     */
    public static PowerManager.WakeLock acquireWakeLock(Context context, int PowerManager) {
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager, context.getClass().getCanonicalName());
            if (null != wakeLock) {
                wakeLock.acquire();
                Log.e("wangsc", "锁定唤醒锁: " + wakeLock);
                return wakeLock;
//                    addRunLog2File(context, "", "锁定唤醒锁。");
            }
        } catch (Exception e) {
            printException(context, e);
        }
        return null;
    }

    /**
     * 释放设备电源锁
     */
    public static void releaseWakeLock(Context context, PowerManager.WakeLock wakeLock) {
        try {

            Log.e("wangsc", "解除唤醒锁: " + wakeLock);
            if (null != wakeLock && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
//                addRunLog2File(context, "", "解除唤醒锁。");
            }
        } catch (Exception e) {
            printException(context, e);
        }
    }

    /**
     * 检查service是否还在允许。
     *
     * @param context
     * @param serverPackageName
     * @return
     */
    public static boolean isServiceRunning(Context context, String serverPackageName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serverPackageName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有程序的包名信息
     *
     * @param application
     * @return
     */
    public static List<String> getAppInfos(Application application) {
        PackageManager pm = application.getPackageManager();
        List<PackageInfo> packgeInfos = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        List<String> appInfos = new ArrayList<String>();
        /* 获取应用程序的名称，不是包名，而是清单文件中的labelname
            String str_name = packageInfo.applicationInfo.loadLabel(pm).toString();
            appInfo.setAppName(str_name);
         */
        for (PackageInfo packgeInfo : packgeInfos) {
//            String appName = packgeInfo.applicationInfo.loadLabel(pm).toString();
            String packageName = packgeInfo.packageName;
//            Drawable drawable = packgeInfo.applicationInfo.loadIcon(pm);
//            AppInfo appInfo = new AppInfo(appName, packageName, drawable);
//            appInfos.add(appInfo);
            appInfos.add(packageName);
        }
        return appInfos;
    }

    /**
     * 判断耳机是否连接。
     *
     * @return
     */
    public static boolean isHeadsetExists() {
        char[] buffer = new char[1024];

        int newState = 0;

        try {
            FileReader file = new FileReader("/sys/class/switch/h2w/state");
            int len = file.read(buffer, 0, 1024);
            newState = Integer.valueOf((new String(buffer, 0, len)).trim());
        } catch (FileNotFoundException e) {
            Log.e("FMTest", "This kernel does not have wired headset support");
        } catch (Exception e) {
            Log.e("FMTest", "", e);
        }
        return newState != 0;
    }

    /**
     * 判断辅助功能是否开启。
     *
     * @param mContext
     * @return
     */
    public static boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = mContext.getPackageName() + "/" + MyAccessbilityService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.v(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error finding setting, default accessibility to not found: "
                    + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            Log.v(TAG, "***ACCESSIBILITY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    Log.v(TAG, "-------------- > accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        Log.v(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            Log.v(TAG, "***ACCESSIBILITY IS DISABLED***");
        }

        return false;
    }


    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }


    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    public static PowerManager.WakeLock mWakeLock;

    @SuppressLint("InvalidWakeLockTag")
    public static void screenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "bright");
        mWakeLock.acquire(120000);
    }

    public static void screenOff(Context context) {
        DevicePolicyManager mDevicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mDevicePolicyManager.lockNow();
    }

    public static void zhendong1(Context context){
        Vibrator vibrator = (Vibrator)context.getSystemService(context.VIBRATOR_SERVICE);
        vibrator.vibrate(1000);
    }

    public static void zhendong2(Context context){
        Vibrator vibrator = (Vibrator)context.getSystemService(context.VIBRATOR_SERVICE);
        vibrator.vibrate(500);
    }

    public static void zhendong3(Context context){
        Vibrator vibrator = (Vibrator)context.getSystemService(context.VIBRATOR_SERVICE);
        long[] patter = {100, 100, 100,100,100,100}; // 数组的a[0]表示静止的时间，a[1]代表的是震动的时间，然后数组的a[2]表示静止的时间，a[3]代表的是震动的时间……依次类推下去
        vibrator.vibrate(patter, -1); // 第二个参数是循环的位置，-1是不循环。


    }
   public static void zhendong6(Context context){
        Vibrator vibrator = (Vibrator)context.getSystemService(context.VIBRATOR_SERVICE);
        long[] patter = {100, 100, 100,100,100,100,1000,100, 100, 100,100,100,100}; // 数组的a[0]表示静止的时间，a[1]代表的是震动的时间，然后数组的a[2]表示静止的时间，a[3]代表的是震动的时间……依次类推下去
        vibrator.vibrate(patter, -1); // 第二个参数是循环的位置，-1是不循环。
    }
}
