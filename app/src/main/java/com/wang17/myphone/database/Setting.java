package com.wang17.myphone.database;

import com.wang17.myphone.model.DateTime;

import org.jetbrains.annotations.Nullable;

/**
 * Created by 阿弥陀佛 on 2015/6/30.
 */
public class Setting{

    private String name;
    private String value;
    private int level;

    public Setting(String name, String value,int level){
        this.name = name;
        this.value = value;
        this.level=level;
    }


    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getString() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }


    public Boolean getBoolean(){
        return Boolean.parseBoolean(value);
    }
    public int getInt(){
        return Integer.parseInt(value);
    }
    public long getLong(){
        return Long.parseLong(value);
    }
    public DateTime getDateTime(){
        return new DateTime(getLong());
    }
    public float getFloat(){
        return Float.parseFloat(value);
    }
    public double getDouble(){
        return Double.parseDouble(value);
    }

    public enum KEYS{
        tmp_selected,
        tmp_tt,is保持念佛服务,buddha_prv_count,
        is开启整点报时,
        top_title,bottom_title,
        buddha_service_running,
        几圈后自动结束念佛, is念佛自动暂停,
        muyu_duration,muyu_startime,muyu_stoptime,muyu_type,
        is填充图表区域, 图表开始日期,
        sms_last_time,
        is查看念佛记录需要验证,
        buddha_float_window_x, buddha_float_window_y,buddha_float_window_width, buddha_float_window_size, is显示念佛悬浮窗,
         彩票, is念佛整圈响引罄, is账户超额提醒,

        balanceABC, balanceICBC,balanceLowABC,balanceLowICBC,

        buddha_duration,buddha_startime,buddha_stoptime,
        念佛最佳音量,
        muyu_period,yq_period,muyu_count,

        main_start_page_index, 戒期wisdom,
        地图搜索半径_米, 地图搜索周期_天,足迹展示哪一年,

        buddha_music_name,

        is_print_state_else, is_print_content_else, is_print_other_all, is_print_ifClassName,

        map_animateLong, map_location_isAutoChangeGear, map_location_gear, map_location_is_opened, map_search_radius_toilet, map_search_radius_gas, map_location_clock_alarm_is_open,
        地图位置显示模式,
        is显示所有MarkDay, MarkDay主项, 顶栏标题,
        上一次几点保存的小部件戒期信息, 今天小部件戒期信息, alarm_window_msg, battery,

        is语音播报价格大幅变动,
        设置黑名单,
        显示几天内的待办,
        wx_new_msg,
    }
    //endregion
}
