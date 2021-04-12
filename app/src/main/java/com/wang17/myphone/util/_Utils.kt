package com.wang17.myphone.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.SoundPool
import android.net.ConnectivityManager
import android.os.Build
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.Process
import android.os.Vibrator
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.speech.tts.TextToSpeech
import android.support.v7.app.AlertDialog
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log
import android.view.View
import android.widget.Toast
import com.ta.utdid2.android.utils.AESUtils
import com.wang17.myphone.MainActivity
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.receiver.LockReceiver
import com.wang17.myphone.service.MyAccessbilityService
import com.wangsc.commons.codec.digest.DigestUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileReader
import java.util.*

/**
 * Created by 阿弥陀佛 on 2016/10/18.
 */
object _Utils {

    /**
     * 隐藏虚拟按键，并且全屏
     */
    @JvmStatic
    fun hideBottomUIMenu(activity: Activity) {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            val v = activity.window.decorView
            v.systemUiVisibility = View.GONE
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            val decorView = activity.window.decorView
            val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN)
            decorView.systemUiVisibility = uiOptions
        }
    }

    fun isAppRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packageName = "com.wangsc.lovehome"
        val appProcesses = activityManager.runningAppProcesses
        if (appProcesses == null) {
            Log.e("wangsc", "null")
            return false
        }
        for (appProcess in appProcesses) {
            Log.e("wangsc", appProcess.processName)
            if (appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }

    /**
     * 模拟点击HOME按钮
     *
     * @param context
     */
    @JvmStatic
    fun clickHomeButton(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private var textToSpeech: TextToSpeech? = null //创建自带语音对象

    @JvmStatic
    fun speaker(context: Context?, msg: String?) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech!!.setPitch(1.0f) //方法用来控制音调
                textToSpeech!!.setSpeechRate(1.2f) //用来控制语速

                //判断是否支持下面语言
                val result = textToSpeech!!.setLanguage(Locale.CHINA)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(context, "SIMPLIFIED_CHINESE数据丢失或不支持", Toast.LENGTH_SHORT).show()
                } else {
                    textToSpeech!!.speak(msg, TextToSpeech.QUEUE_FLUSH, null) //输入中文，若不支持的设备则不会读出来
                }
            }
        }
    }

    fun speaker(context: Context?, msg: String?, pitch: Float) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech!!.setPitch(pitch) //方法用来控制音调
                textToSpeech!!.setSpeechRate(1.2f) //用来控制语速

                //判断是否支持下面语言
                val result = textToSpeech!!.setLanguage(Locale.CHINA)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(context, "SIMPLIFIED_CHINESE数据丢失或不支持", Toast.LENGTH_SHORT).show()
                } else {
                    textToSpeech!!.speak(msg, TextToSpeech.QUEUE_FLUSH, null) //输入中文，若不支持的设备则不会读出来
                }
            }
        }
    }

    fun speaker(context: Context?, msg: String?, pitch: Float, speech: Float) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech!!.setPitch(pitch) //方法用来控制音调
                textToSpeech!!.setSpeechRate(speech) //用来控制语速

                //判断是否支持下面语言
                val result = textToSpeech!!.setLanguage(Locale.CHINA)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(context, "SIMPLIFIED_CHINESE数据丢失或不支持", Toast.LENGTH_SHORT).show()
                } else {
                    textToSpeech!!.speak(msg, TextToSpeech.QUEUE_FLUSH, null) //输入中文，若不支持的设备则不会读出来
                }
            }
        }
    }

    private var soundpool: SoundPool? = null
    private val soundmap: MutableMap<Int, Int> = HashMap()
    /**
     * 检测辅助功能是否开启<br></br>
     * 方 法 名：isAccessibilitySettingsOn <br></br>
     * 创 建 人 <br></br>
     * 创建时间：2016-6-22 下午2:29:24 <br></br>
     * 修 改 人： <br></br>
     * 修改日期： <br></br>
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
    fun getTargetInMillis(birthday: DateTime): Long {
        val now = DateTime()
        var age = now.year - birthday.year + 1
        if (now.month < birthday.month) {
            age -= 1
        }
        var day = 100.0
        if (age < 18) {
            day = -1.0
        } else if (age >= 18 && age < 20) {
            day = 3.0
        } else if (age >= 20 && age < 30) {
            day = 4 + (age - 20) * 0.4
        } else if (age >= 30 && age < 40) {
            day = 8 + (age - 30) * 0.8
        } else if (age >= 40 && age < 50) {
            day = 16 + (age - 40) * 0.5
        } else if (age >= 50 && age < 60) {
            day = 21 + (age - 50) * 0.9
        }
        return (day * 24 * 3600000).toLong()
    }

    @JvmStatic
    fun getTargetInHours(birthday: DateTime): Int {
        return (getTargetInMillis(birthday) / 3600000).toInt()
    }

    @JvmStatic
    fun printException(context: Context?, e: Exception) {
        try {
            val msg = saveException(context, e)
            AlertDialog.Builder(context!!).setMessage(msg).setCancelable(false).setPositiveButton("知道了", null).show()
        } catch (e1: Exception) {
        }
    }

    @JvmStatic
    fun saveException(context: Context?, e: Exception): String {
        val msg = getExceptionStr(e)
        DataContext(context).addRunLog("err", "运行错误", msg)
        return msg
    }

    fun getExceptionStr(e: Exception): String {
        var msg = ""
        try {
            if (e.stackTrace.size == 0) return ""
            for (ste in e.stackTrace) {
                if (ste.className.contains(MainActivity::class.java.getPackage().name)) {
                    msg += """
                        类名：
                        ${ste.className}
                        方法名：
                        ${ste.methodName}
                        行号：${ste.lineNumber}
                        错误信息：
                        ${e.message}
                        
                        """.trimIndent()
                }
            }
        } catch (exception: Exception) {
        }
        return msg
    }

    @JvmStatic
    fun e(log: Any) {
        Log.e("wangsc", log.toString())
    }

    @JvmStatic
    fun getFilesWithSuffix(path: String?, suffix: String?): Array<String> {
        val file = File(path)
        val files = file.list { dir, name -> if (name != null && name.endsWith(suffix!!)) true else false }
        return files ?: arrayOf()
    }

    @JvmStatic
    fun getMD5(filePath: String): String {
        return DigestUtils.md5Hex(FileInputStream(filePath))

//        var bi: BigInteger? = null;
//        try {
//            var buffer = ByteArray(8192) { i -> i.toByte() }
//            var len = 0
//            var md = MessageDigest.getInstance("MD5");
//            var f = File(path)
//            var fis = FileInputStream(f)
//
//            while (len != -1) {
//                md.update(buffer, 0, len)
//                len = fis.read(buffer)
//            }
//            fis.close();
//            var b = md.digest()
//            bi = BigInteger(1, b);
//        } catch (e: Exception) {
//        }
//        e("md5 time span : ${System.currentTimeMillis()-a}ms")
//        return bi!!.toString(16)
    }

    var policyManager: DevicePolicyManager? = null
    var componentName: ComponentName? = null
    fun LockScreen(context: Context) {
        policyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(context, LockReceiver::class.java)
        if (policyManager!!.isAdminActive(componentName!!)) {
            Log.e("wangsc", "判断是否有权限(激活了设备管理器)")
            //判断是否有权限(激活了设备管理器)
            policyManager!!.lockNow()
            // 直接锁屏
            Process.killProcess(Process.myPid())
        } else {
            Log.e("wangsc", "激活设备管理器")
            activeManager(context)
            //激活设备管理器获取权限
        }
    }

    // 解除绑定
    fun Bind(context: Context) {
        if (componentName != null) {
            policyManager!!.removeActiveAdmin(componentName!!)
            activeManager(context)
        }
    }

    private fun activeManager(context: Context) {
        //使用隐式意图调用系统方法来激活指定的设备管理器
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "一键锁屏")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Log.e("wangsc", "激活了设备管理器")
    }

    /**
     * 获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
     *
     *
     * PARTIAL_WAKE_LOCK :保持CPU 运转，屏幕和键盘灯是关闭的。
     * SCREEN_DIM_WAKE_LOCK ：保持CPU 运转，允许保持屏幕显示但有可能是灰的，关闭键盘灯
     * SCREEN_BRIGHT_WAKE_LOCK ：保持CPU 运转，保持屏幕高亮显示，关闭键盘灯
     * FULL_WAKE_LOCK ：保持CPU 运转，保持屏幕高亮显示，键盘灯也保持亮度
     *
     * @param context
     */
    @JvmStatic
    fun acquireWakeLock(context: Context, PowerManager: Int): WakeLock? {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(PowerManager, context.javaClass.canonicalName)
            if (null != wakeLock) {
                wakeLock.acquire()
                Log.e("wangsc", "锁定唤醒锁: $wakeLock")
                return wakeLock
                //                    addRunLog2File(context, "", "锁定唤醒锁。");
            }
        } catch (e: Exception) {
            printException(context, e)
        }
        return null
    }

    /**
     * 释放设备电源锁
     */
    @JvmStatic
    fun releaseWakeLock(context: Context?, wakeLock: WakeLock?) {
        var wakeLock = wakeLock
        try {
            Log.e("wangsc", "解除唤醒锁: $wakeLock")
            if (null != wakeLock && wakeLock.isHeld) {
                wakeLock.release()
                wakeLock = null
                //                addRunLog2File(context, "", "解除唤醒锁。");
            }
        } catch (e: Exception) {
            printException(context, e)
        }
    }

    /**
     * 检查service是否还在允许。
     * 例：Utils.isServiceRunning(context!!, BuddhaService.javaClass.canonicalName)
     *
     * @param context
     * @param serverPackageName
     * @return
     */
    @JvmStatic
    fun isServiceRunning(context: Context, serverPackageName: String): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serverPackageName == service.service.className) {
                return true
            }
        }
        return false
    }

    /**
     * 获取所有程序的包名信息
     *
     * @param application
     * @return
     */
    @JvmStatic
    fun getAppInfos(application: Application): List<String> {
        val pm = application.packageManager
        val packgeInfos = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES)
        val appInfos: MutableList<String> = ArrayList()
        /* 获取应用程序的名称，不是包名，而是清单文件中的labelname
            String str_name = packageInfo.applicationInfo.loadLabel(pm).toString();
            appInfo.setAppName(str_name);
         */for (packgeInfo in packgeInfos) {
//            String appName = packgeInfo.applicationInfo.loadLabel(pm).toString();
            val packageName = packgeInfo.packageName
            //            Drawable drawable = packgeInfo.applicationInfo.loadIcon(pm);
//            AppInfo appInfo = new AppInfo(appName, packageName, drawable);
//            appInfos.add(appInfo);
            appInfos.add(packageName)
        }
        return appInfos
    }

    /**
     * 判断耳机是否连接。
     *
     * @return
     */
    val isHeadsetExists: Boolean
        get() {
            val buffer = CharArray(1024)
            var newState = 0
            try {
                val file = FileReader("/sys/class/switch/h2w/state")
                val len = file.read(buffer, 0, 1024)
                newState = Integer.valueOf(String(buffer, 0, len).trim { it <= ' ' })
            } catch (e: FileNotFoundException) {
                Log.e("FMTest", "This kernel does not have wired headset support")
            } catch (e: Exception) {
                Log.e("FMTest", "", e)
            }
            return newState != 0
        }

    /**
     * 判断辅助功能是否开启。
     *
     * @param mContext
     * @return
     */
    @JvmStatic
    fun isAccessibilitySettingsOn(mContext: Context): Boolean {
        var accessibilityEnabled = 0
        val service = mContext.packageName + "/" + MyAccessbilityService::class.java.canonicalName
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.applicationContext.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED)
            Log.v(AESUtils.TAG, "accessibilityEnabled = $accessibilityEnabled")
        } catch (e: SettingNotFoundException) {
            Log.e(AESUtils.TAG, "Error finding setting, default accessibility to not found: "
                    + e.message)
        }
        val mStringColonSplitter = SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            Log.v(AESUtils.TAG, "***ACCESSIBILITY IS ENABLED*** -----------------")
            val settingValue = Settings.Secure.getString(
                    mContext.applicationContext.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()
                    Log.v(AESUtils.TAG, "-------------- > accessibilityService :: $accessibilityService $service")
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        Log.v(AESUtils.TAG, "We've found the correct setting - accessibility is switched on!")
                        return true
                    }
                }
            }
        } else {
            Log.v(AESUtils.TAG, "***ACCESSIBILITY IS DISABLED***")
        }
        return false
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    fun px2dip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun isNetworkConnected(context: Context?): Boolean {
        if (context != null) {
            val mConnectivityManager = context
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mNetworkInfo = mConnectivityManager.activeNetworkInfo
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable
            }
        }
        return false
    }

    var mWakeLock: WakeLock? = null

    @SuppressLint("InvalidWakeLockTag")
    fun screenOn(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, "bright")
        mWakeLock?.acquire(120000)
    }

    fun screenOff(context: Context) {
        val mDevicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        mDevicePolicyManager.lockNow()
    }

    fun zhendong50(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(50)
    }
    fun zhendong100(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(100)
    }

    @JvmStatic
    fun zhendong500(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(500)
    }

    fun zhendong3(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val patter = longArrayOf(100, 100, 100, 100, 100, 100) // 数组的a[0]表示静止的时间，a[1]代表的是震动的时间，然后数组的a[2]表示静止的时间，a[3]代表的是震动的时间……依次类推下去
        vibrator.vibrate(patter, -1) // 第二个参数是循环的位置，-1是不循环。
    }

    fun zhendong6(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val patter = longArrayOf(100, 100, 100, 100, 100, 100, 1000, 100, 100, 100, 100, 100, 100) // 数组的a[0]表示静止的时间，a[1]代表的是震动的时间，然后数组的a[2]表示静止的时间，a[3]代表的是震动的时间……依次类推下去
        vibrator.vibrate(patter, -1) // 第二个参数是循环的位置，-1是不循环。
    }
}