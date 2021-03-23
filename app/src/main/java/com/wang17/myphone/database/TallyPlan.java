package com.wang17.myphone.database;

import java.util.UUID;

/**
 * Created by Administrator on 2017/11/29.
 */

public class TallyPlan{
    private UUID id;
    private long interval;
    private String item;
    private String summary;

    public TallyPlan() {
        this.id = UUID.randomUUID();
    }

    public TallyPlan(UUID id) {
        this.id = id;
    }

    public TallyPlan(long interval, String item, String summary) {
        this();
        this.interval = interval;
        this.item = item;
        this.summary = summary;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
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
