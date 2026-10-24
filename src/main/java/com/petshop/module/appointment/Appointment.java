package com.petshop.module.appointment;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 预约（v1.1）：纯日程记录，转开单才收款 */
public class Appointment {

    private Long id;
    private Long customerId;
    private Long petId;
    private Long serviceId;
    private LocalDate appointDate;
    private String slot;          // 'HH:mm'
    private String sizeTier;
    private String status;        // BOOKED/ARRIVED/DONE/CANCELLED/NO_SHOW
    private Long billId;
    private String remark;
    private LocalDateTime createdAt;
    private String createdBy;

    /** 关联展示 */
    private String petName;
    private String breed;
    private String ownerName;
    private String ownerPhone;
    private String serviceName;
    private String billNo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public LocalDate getAppointDate() { return appointDate; }
    public void setAppointDate(LocalDate appointDate) { this.appointDate = appointDate; }
    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }
    public String getSizeTier() { return sizeTier; }
    public void setSizeTier(String sizeTier) { this.sizeTier = sizeTier; }
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
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }
}
