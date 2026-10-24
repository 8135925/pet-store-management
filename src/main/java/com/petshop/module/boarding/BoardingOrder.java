package com.petshop.module.boarding;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 寄养单（v1.1） */
public class BoardingOrder {

    private Long id;
    private String orderNo;
    private Long customerId;
    private Long petId;
    private Long roomId;
    private LocalDateTime startAt;        // 入住时间
    private LocalDate plannedEndOn;       // 预计离店日
    private LocalDateTime endAt;          // 实际退住时间
    private Integer days;                 // 计费晚数
    private Integer unitPriceCent;
    private Integer depositCent;
    private String status;                // BOOKED/CHECKED_IN/CHECKED_OUT/CANCELLED
    private Long billId;
    private String remark;
    private LocalDateTime createdAt;
    private String createdBy;

    /** 关联展示 */
    private String petName;
    private String species;
    private String breed;
    private String ownerName;
    private String ownerPhone;
    private String roomCode;
    private String roomType;
    private String billNo;

    /** 已住晚数（看板实时展示：入住日到今天） */
    public long nightsSoFar() {
        LocalDate start = startAt.toLocalDate();
        LocalDate end = endAt != null ? endAt.toLocalDate()
                : (status != null && "CHECKED_IN".equals(status) ? LocalDate.now() : plannedEndOn);
        long n = java.time.temporal.ChronoUnit.DAYS.between(start, end);
        return Math.max(n, 1);
    }

    /** 是否超期未接（在住且预离店日早于今天） */
    public boolean isOverdue() {
        return "CHECKED_IN".equals(status) && plannedEndOn.isBefore(LocalDate.now());
    }

    /** 超期天数（配合 isOverdue 展示） */
    public long overdueDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(plannedEndOn, LocalDate.now());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDate getPlannedEndOn() { return plannedEndOn; }
    public void setPlannedEndOn(LocalDate plannedEndOn) { this.plannedEndOn = plannedEndOn; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public Integer getUnitPriceCent() { return unitPriceCent; }
    public void setUnitPriceCent(Integer unitPriceCent) { this.unitPriceCent = unitPriceCent; }
    public Integer getDepositCent() { return depositCent; }
    public void setDepositCent(Integer depositCent) { this.depositCent = depositCent; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }
}
