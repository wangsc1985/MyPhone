package com.wang17.myphone.database;

import java.util.UUID;

/**
 * Created by Administrator on 2017/10/11.
 */

public class DayItem {
    private UUID id;
    private String name;
    private String summary;
    private int targetInHour;

    public DayItem() {
        this.id = UUID.randomUUID();
    }

    public DayItem(UUID id) {
        this.id = id;
    }

    public DayItem(String name, String summary, int targetInHour) {
        this();
        this.name = name;
        this.summary = summary;
        this.targetInHour = targetInHour;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getTargetInHour() {
        return targetInHour;
    }

    public void setTargetInHour(int targetInHour) {
        this.targetInHour = targetInHour;
    }
}
