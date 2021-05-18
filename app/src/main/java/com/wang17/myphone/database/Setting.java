package com.wang17.myphone.database;

import com.wang17.myphone.model.DateTime;

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
        sms_last_time,
        is_authorize_buddha_record,
        buddha_float_window_x, buddha_float_window_y,buddha_float_window_width, buddha_float_window_size,
         彩票, is念佛引罄间隔提醒, is账户超额提醒,
        念佛自动结束时间_分钟,
        muyu_period,yq_period,
        balanceABC, balanceICBC,balanceLowABC,balanceLowICBC,
        buddha_duration,buddha_startime,buddha_stoptime, 念佛最佳音量,
        stock_reflush_time,interest,
        main_start_page_index,wisdom,
        地图搜索半径_米, 足迹展示年份,

        buddha_music_name,

        is_print_state_else, is_print_content_else, is_print_other_all, is_print_ifClassName,

        map_httpTimeOut, map_interval, map_isSensorEnable, map_isOnceLocationLatest, map_isOnceLocation, map_isGpsFirst, map_isNeedAddress, map_isLocationCacheEnable, map_animateLong,
        map_location_isAutoChangeGear, map_location_gear, map_location_is_opened, map_search_radius_toilet, map_search_radius_gas, map_location_clock_alarm_is_open,
        地图位置显示模式,
        web_index, is_auto_record, is_mark_day_show_all, mark_day_focused, 顶栏标题,
        is_stock_load_noke, location_search_days, speaker_screen_off,
        is_stocks_listener,
        上一次几点保存的小部件戒期信息, 小部件戒期信息, alarm_window_msg, is_widget_list_stock, battery, quick, nianfo_over_speaker_msg, media_player_speed,

        is_stock_broadcast,
        设置黑名单,
        几天内待办显示,
        wx_new_msg, wx_request_code, wx_sex_date,
        is小部件罗列股票
    }
    //endregion
}
