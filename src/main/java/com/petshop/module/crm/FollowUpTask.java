package com.petshop.module.crm;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FollowUpTask {

    private Long id;
    private Long customerId;
    private Long petId;
    private String type;       // SERVICE_FOLLOW_UP / BIRTHDAY / CUSTOMER_BIRTHDAY / RECALL / MANUAL
    private LocalDate dueOn;
    private String status;     // PENDING / DONE / SKIPPED / EXPIRED
    private String sourceType;
    private Long sourceId;
    private String note;
    private String result;
    private String operator;
    private LocalDateTime doneAt;
    private LocalDateTime createdAt;

    /** 展示用 */
    private String customerName;
    private String customerPhone;
    private String petName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDate getDueOn() { return dueOn; }
    public void setDueOn(LocalDate dueOn) { this.dueOn = dueOn; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getDoneAt() { return doneAt; }
    public void setDoneAt(LocalDateTime doneAt) { this.doneAt = doneAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getTypeLabel() {
        if ("SERVICE_FOLLOW_UP".equals(type)) return "服务回访";
        if ("BIRTHDAY".equals(type)) return "宠物生日";
        if ("CUSTOMER_BIRTHDAY".equals(type)) return "客户生日";
        if ("RECALL".equals(type)) return "流失召回";
        return "手工待办";
    }

    public boolean isOverdue() {
        return "PENDING".equals(status) && dueOn != null && dueOn.isBefore(LocalDate.now());
    }
}
