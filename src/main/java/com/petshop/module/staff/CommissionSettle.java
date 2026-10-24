package com.petshop.module.staff;

import java.time.LocalDateTime;

public class CommissionSettle {

    private Long id;
    private String settleNo;
    private String month;         // 'yyyy-MM'
    private Long staffId;
    private int totalCent;
    private int recordCount;
    private String status;        // DRAFT / CONFIRMED
    private LocalDateTime confirmedAt;
    private String operator;
    private String remark;
    private LocalDateTime createdAt;

    /** 展示用 */
    private String staffName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSettleNo() { return settleNo; }
    public void setSettleNo(String settleNo) { this.settleNo = settleNo; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public int getTotalCent() { return totalCent; }
    public void setTotalCent(int totalCent) { this.totalCent = totalCent; }
    public int getRecordCount() { return recordCount; }
    public void setRecordCount(int recordCount) { this.recordCount = recordCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
}
