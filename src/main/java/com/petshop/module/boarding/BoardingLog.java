package com.petshop.module.boarding;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 寄养每日记录（一天一条，只增不改 BR-14） */
public class BoardingLog {

    private Long id;
    private Long orderId;
    private LocalDate logDate;
    private Integer fedCount;
    private boolean walked;
    private String note;
    private String operator;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }
    public Integer getFedCount() { return fedCount; }
    public void setFedCount(Integer fedCount) { this.fedCount = fedCount; }
    public boolean isWalked() { return walked; }
    public void setWalked(boolean walked) { this.walked = walked; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
