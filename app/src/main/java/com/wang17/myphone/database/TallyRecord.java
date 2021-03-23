package com.wang17.myphone.database;

import com.wang17.myphone.model.DateTime;

import java.util.UUID;

/**
 * Created by 阿弥陀佛 on 2016/9/24.
 */
public class TallyRecord  {
    private UUID id;
    private DateTime start;
    private int interval;
    private String item;
    private String summary;

    public TallyRecord() {
        this.id = UUID.randomUUID();
    }
    public TallyRecord(UUID id){
        this.id = id;
    }
    public TallyRecord(DateTime start, int interval,String item){
        this.id = UUID.randomUUID();
        this.start =start;
        this.interval = interval;
        this.item = item;
    }


    public UUID getId() {
        return id;
    }

    public DateTime getStart() {
        return start;
    }

    public void setStart(DateTime start) {
        this.start = start;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
