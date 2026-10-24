package com.petshop.module.crm;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ContactLog {

    private Long id;
    private Long customerId;
    private Long petId;
    private Long taskId;
    private String channel;      // PHONE / WECHAT / VISIT / SMS_COPY
    private String result;       // CONNECTED / NO_ANSWER / REFUSED / BOOKED
    private String content;
    private LocalDate nextFollowOn;
    private String operator;
    private LocalDateTime createdAt;

    /** 展示用 */
    private String customerName;
    private String petName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDate getNextFollowOn() { return nextFollowOn; }
    public void setNextFollowOn(LocalDate nextFollowOn) { this.nextFollowOn = nextFollowOn; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getChannelLabel() {
        if ("PHONE".equals(channel)) return "电话";
        if ("WECHAT".equals(channel)) return "微信";
        if ("VISIT".equals(channel)) return "到店";
        return "短信";
    }

    public String getResultLabel() {
        if ("CONNECTED".equals(result)) return "已接通";
        if ("NO_ANSWER".equals(result)) return "未接";
        if ("REFUSED".equals(result)) return "明确拒绝";
        if ("BOOKED".equals(result)) return "已预约";
        return result;
    }
}
