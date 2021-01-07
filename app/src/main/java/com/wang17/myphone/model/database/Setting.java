package com.wang17.myphone.model.database;

import com.wang17.myphone.model.DateTime;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

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
        is_mnf,is_knf,is_znf,stock_reflush_time,buddha_load_time,
        main_start_page_index,wisdom,map_search_radius,
        nianfo_intervalInMillis, nianfo_endInMillis, nianfo_palyId, nianfo_isReading,nianfo_screen_light,

        tally_dayTargetInMillis, tally_manualOverTimeInMillis, tally_sectionStartMillis, tally_endInMillis, tally_intervalInMillis,
        tally_music_is_playing,tally_music_switch,tally_isShowDialog, tally_music_name,

        music_current_list_id,music_isplaying, is_keep_cpu_runing,

        weixin_curr_account_id, is_have_didi_order_running,headset_volume,

        is_print_state_else, is_print_content_else, is_print_other_all, is_print_ifClassName,

        map_httpTimeOut, map_interval, map_isSensorEnable, map_isOnceLocationLatest, map_isOnceLocation, map_isGpsFirst, map_isNeedAddress, map_isLocationCacheEnable, map_animateLong,
        map_is_default_toilet, map_location_isAutoChangeGear, map_location_gear, map_location_is_opened, map_search_radius_toilet, map_search_radius_gas, map_location_clock_alarm_is_open,
        map_mylocation_type, web_index, is_auto_record, is_mark_day_show_all, is_trade_alarm_open, is_allow_action_recents, mark_day_focused, tally_record_item_text, bar_title,
        bank_bill_warning_days, bank_card, is_stock_load_noke, location_search_days, speaker_screen_off,
        is_stocks_listener,
        list_religious_day, list_religious, alarm_window_msg, is_widget_list_stock, battery, quick, latest_widget_update_time, nianfo_over_speaker_msg, media_player_speed,
        media_player_position, media_player_pitch,

        media_player_volumn,media_player, bmob_file_url,SyncTime,wx_sex_date,bank_balance, bank_date_millis,is_widget_update_noice,
        is_stock_broadcast, // 记录是不是语音报送盈利
        is_flush_widget_when_screen_on, is_make_loan_alert, black_list, tally_once_minute, ignore_todo, sms_to_number, pre_alert_hour,
        location_search_month, speaker_pitch, speaker_speech, todo_visible_dayoffset, wx_db_mark_date, wx_new_msg_count, wx_request_code, is_allow_widget_list_stock,tv_ip,clock_ip,
        bank1_balance,bank1_date_millis,bank2_balance,bank2_date_millis
    }
    //endregion
}
