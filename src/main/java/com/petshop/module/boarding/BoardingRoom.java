package com.petshop.module.boarding;

import java.time.LocalDateTime;

/** 笼位（v1.1） */
public class BoardingRoom {

    private Long id;
    private String code;        // R01/R02/C01…（C 开头为猫位）
    private String roomType;    // S/M/L/XL/CAT
    private Integer dailyPriceCent;
    private String note;
    private boolean isActive;
    private LocalDateTime createdAt;

    /** 在住数量（看板展示用） */
    private Long inUseCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public Integer getDailyPriceCent() { return dailyPriceCent; }
    public void setDailyPriceCent(Integer dailyPriceCent) { this.dailyPriceCent = dailyPriceCent; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getInUseCount() { return inUseCount; }
    public void setInUseCount(Long inUseCount) { this.inUseCount = inUseCount; }
}
