package com.wang17.myphone.model.database;

import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.util._Session;

import java.util.UUID;

/**
 * Created by 阿弥陀佛 on 2016/10/17.
 */

public class MarkDay{
    private UUID id;
    private DateTime dateTime;
    private UUID item;
    private String summary;

    public MarkDay() {
        this.id = UUID.randomUUID();
    }

    public MarkDay(UUID id) {
        this.id = id;
    }

    public MarkDay(DateTime dateTime,UUID itemId) {
        this();
        this.dateTime = dateTime;
        this.item = itemId;
        this.summary = "";
    }

    public MarkDay(DateTime dateTime, UUID itemId, String summary) {
        this();
        this.dateTime = dateTime;
        this.item = itemId;
        this.summary = summary;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public UUID getItem() {
        return item;
    }

    public void setItem(UUID item) {
        this.item = item;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
