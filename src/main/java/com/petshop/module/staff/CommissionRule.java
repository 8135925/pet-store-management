package com.petshop.module.staff;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CommissionRule {

    private Long id;
    private String scope;          // SERVICE / PRODUCT / BOARDING / CARD / ALL
    private Long refId;            // SERVICE→service_item.id；PRODUCT→product.id
    private String staffRole;      // 限定岗位，null=不限
    private int ratioBp;           // 万分比，1500 = 15%
    private int fixedCent;         // 固定提成（分/件 或 分/单）
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean isActive;
    private String remark;
    private LocalDateTime createdAt;

    /** 展示用 */
    private String refName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public String getStaffRole() { return staffRole; }
    public void setStaffRole(String staffRole) { this.staffRole = staffRole; }
    public int getRatioBp() { return ratioBp; }
    public void setRatioBp(int ratioBp) { this.ratioBp = ratioBp; }
    public int getFixedCent() { return fixedCent; }
    public void setFixedCent(int fixedCent) { this.fixedCent = fixedCent; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean active) { isActive = active; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getRefName() { return refName; }
    public void setRefName(String refName) { this.refName = refName; }

    public String getScopeLabel() {
        if ("SERVICE".equals(scope)) return "服务";
        if ("PRODUCT".equals(scope)) return "商品";
        if ("BOARDING".equals(scope)) return "寄养";
        if ("CARD".equals(scope)) return "售卡";
        return "全部";
    }

    /** 15.00% 或 "3.00 元/件" */
    public String getRateLabel() {
        StringBuilder sb = new StringBuilder();
        if (ratioBp > 0) {
            sb.append(String.format("%.2f%%", ratioBp / 100.0));
        }
        if (fixedCent > 0) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append(String.format("%.2f 元", fixedCent / 100.0));
        }
        return sb.length() == 0 ? "—" : sb.toString();
    }
}
