package com.petshop.module.staff;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CommissionRecord {

    private Long id;
    private Long staffId;
    private Long billId;
    private Long billItemId;
    private String bizType;       // SERVICE / PRODUCT / BOARDING / CARD
    private int baseCent;
    private int ratioBp;
    private int fixedCent;
    private int amountCent;       // 可为负（反结账冲销）
    private LocalDate businessDate;
    private Long settleId;
    private Long reverseOfId;
    private String remark;
    private LocalDateTime createdAt;

    /** 展示用 */
    private String staffName;
    private String billNo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getBillItemId() { return billItemId; }
    public void setBillItemId(Long billItemId) { this.billItemId = billItemId; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public int getBaseCent() { return baseCent; }
    public void setBaseCent(int baseCent) { this.baseCent = baseCent; }
    public int getRatioBp() { return ratioBp; }
    public void setRatioBp(int ratioBp) { this.ratioBp = ratioBp; }
    public int getFixedCent() { return fixedCent; }
    public void setFixedCent(int fixedCent) { this.fixedCent = fixedCent; }
    public int getAmountCent() { return amountCent; }
    public void setAmountCent(int amountCent) { this.amountCent = amountCent; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
    public Long getSettleId() { return settleId; }
    public void setSettleId(Long settleId) { this.settleId = settleId; }
    public Long getReverseOfId() { return reverseOfId; }
    public void setReverseOfId(Long reverseOfId) { this.reverseOfId = reverseOfId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }

    public String getBizLabel() {
        if ("SERVICE".equals(bizType)) return "服务";
        if ("PRODUCT".equals(bizType)) return "商品";
        if ("BOARDING".equals(bizType)) return "寄养";
        if ("CARD".equals(bizType)) return "售卡";
        return bizType;
    }
}
