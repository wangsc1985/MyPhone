package com.wang17.myphone.util

import android.os.Environment
import com.wang17.myphone.model.Commodity
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.util._Utils.e
import java.io.File
import java.util.*

/**
 * Created by 阿弥陀佛 on 2016/10/28.
 */
object _Session {
    const val ACTION_ALARM_NIANFO_OVER = "ACTION_ALARM_NIANFO_OVER"
    @JvmField
    val UUID_NULL = UUID.fromString("00000000-0000-0000-0000-000000000000")
    const val ALERT_VOLUMN = 0.2f
    @JvmField
    val BIRTHDAY = DateTime(1985, 11, 27)
    @JvmField
    var GEARS = intArrayOf(180, 120, 80, 60, 40)
    @JvmField
    var GEAR_NAMES = arrayOf("等待", "步行", "单车", "汽车", "高速")
    @JvmField
    var BUDDHA_MUSIC_NAME_ARR: Array<String>
    @JvmField
    val ROOT_DIR = File(Environment.getExternalStorageDirectory().toString() + "/0/myphone")
    @JvmField
    val BACKUP_DIR = File(Environment.getExternalStorageDirectory().toString() + "/0/myphone/backup")
    @JvmField
    var commoditys: MutableList<Commodity>

    /**
     * 根据文件名，获取当前文件的完整路径的File对象
     */
    @JvmStatic
    fun getFile(name:String):File{
        return File(ROOT_DIR,name)
    }
    init {
        try {
            /**
             * 加载念佛音乐列表
             */
            if (!BACKUP_DIR.exists()) {
                BACKUP_DIR.mkdirs()
            }
            BUDDHA_MUSIC_NAME_ARR = _Utils.getFilesWithSuffix(ROOT_DIR.path, ".mp3")
            Arrays.sort(BUDDHA_MUSIC_NAME_ARR)
            /**
             * unit*cose：一个点位多少钱/手。
             */
            commoditys = ArrayList()
            commoditys.add(Commodity("ts", "二债", 200, 0.005))
            commoditys.add(Commodity("tf", "五债", 100, 0.005))
            commoditys.add(Commodity("t", "十债", 10, 0.005))
            commoditys.add(Commodity("au", "金", 1000, 0.05))
            commoditys.add(Commodity("ag", "银", 15, 1.0))
            commoditys.add(Commodity("cu", "铜", 5, 10.0))
            commoditys.add(Commodity("al", "铝", 5, 5.0))
            commoditys.add(Commodity("zn", "锌", 5, 5.0))
            commoditys.add(Commodity("pb", "铅", 5, 5.0))
            commoditys.add(Commodity("ni", "镍", 1, 10.0))
            commoditys.add(Commodity("sn", "锡", 1, 10.0))
            commoditys.add(Commodity("j", "焦炭", 100, 0.5))
            commoditys.add(Commodity("jm", "焦煤", 60, 0.5))
            commoditys.add(Commodity("zc", "郑煤", 100, 0.2))
            commoditys.add(Commodity("i", "铁矿", 100, 0.5))
            commoditys.add(Commodity("wr", "线材", 10, 1.0))
            commoditys.add(Commodity("rb", "螺纹", 10, 1.0))
            commoditys.add(Commodity("hc", "热卷", 10, 1.0))
            commoditys.add(Commodity("sf", "硅铁", 5, 2.0))
            commoditys.add(Commodity("sm", "锰硅", 5, 2.0))
            commoditys.add(Commodity("fg", "玻璃", 20, 1.0))
            commoditys.add(Commodity("fu", "燃油", 10, 1.0))
            commoditys.add(Commodity("sc", "原油", 1000, 0.1))
            commoditys.add(Commodity("ru", "橡胶", 10, 5.0))
            commoditys.add(Commodity("l", "塑料", 5, 5.0))
            commoditys.add(Commodity("ta", "PTA", 5, 2.0))
            commoditys.add(Commodity("v", "PVC", 5, 5.0))
            commoditys.add(Commodity("pp", "PP", 5, 1.0))
            commoditys.add(Commodity("ma", "郑醇", 10, 1.0))
            commoditys.add(Commodity("bu", "沥青", 10, 2.0))
            commoditys.add(Commodity("a", "豆一", 10, 1.0))
            commoditys.add(Commodity("b", "豆二", 10, 1.0))
            commoditys.add(Commodity("c", "玉米", 10, 1.0))
            commoditys.add(Commodity("wh", "郑麦", 20, 1.0))
            commoditys.add(Commodity("ri", "早稻", 20, 1.0))
            commoditys.add(Commodity("lr", "晚稻", 20, 1.0))
            commoditys.add(Commodity("jr", "粳稻", 20, 1.0))
            commoditys.add(Commodity("rs", "菜籽", 10, 1.0))
            commoditys.add(Commodity("ml", "豆粕", 10, 1.0))
            commoditys.add(Commodity("rm", "菜粕", 10, 1.0))
            commoditys.add(Commodity("y", "豆油", 10, 2.0))
            commoditys.add(Commodity("oi", "郑油", 10, 1.0))
            commoditys.add(Commodity("p", "棕榈", 10, 2.0))
            commoditys.add(Commodity("cf", "郑棉", 5, 5.0))
            commoditys.add(Commodity("sr", "白糖", 10, 1.0))
            commoditys.add(Commodity("cy", "棉纱", 5, 5.0))
            commoditys.add(Commodity("jd", "鸡蛋", 5, 1.0))
            commoditys.add(Commodity("ap", "苹果", 10, 1.0))
            commoditys.add(Commodity("cs", "淀粉", 10, 1.0))
            commoditys.add(Commodity("fb", "纤板", 500, 0.05))
            commoditys.add(Commodity("bb", "胶板", 500, 0.05))
        } catch (e: Exception) {
            throw e
        }
    }
}