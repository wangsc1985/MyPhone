package com.wang17.myphone.database;

import java.util.UUID;

/**
 * Created by Administrator on 2017/3/22.
 */

public class PlayList {
    private UUID id;
    private String name;
    private UUID currentSongId;
    private int currentSongPosition;
    private boolean isDefault;
    private long updateTime=1;
    private long syncTime=1;
    private int status=1;// 值为-1，说明此数据已删除。

    public PlayList(UUID id){
        this.id = id;
    }

    public PlayList(String name, UUID currentSongId, int currentSongPosition) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.currentSongId = currentSongId;
        this.currentSongPosition = currentSongPosition;
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

    public UUID getCurrentSongId() {
        return currentSongId;
    }

    public void setCurrentSongId(UUID currentSongId) {
        this.currentSongId = currentSongId;
    }

    public int getCurrentSongPosition() {
        return currentSongPosition;
    }

    public void setCurrentSongPosition(int currentSongPosition) {
        this.currentSongPosition = currentSongPosition;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public long getSyncTime() {
        return syncTime;
    }

    public void setSyncTime(long syncTime) {
        this.syncTime = syncTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
