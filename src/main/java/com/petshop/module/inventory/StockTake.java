package com.petshop.module.inventory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockTake {

    private Long id;
    private String takeNo;
    private String scope;           // ALL / CATEGORY / SELECTED
    private String scopeValue;
    private String status;          // COUNTING / CONFIRMED / CANCELLED
    private int itemCount;
    private int diffCount;
    private int diffAmountCent;
    private LocalDateTime startedAt;
    private LocalDateTime confirmedAt;
    private String operator;
    private String remark;
    private LocalDateTime createdAt;

    private List<StockTakeItem> items = new ArrayList<StockTakeItem>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTakeNo() { return takeNo; }
    public void setTakeNo(String takeNo) { this.takeNo = takeNo; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getScopeValue() { return scopeValue; }
    public void setScopeValue(String scopeValue) { this.scopeValue = scopeValue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    public int getDiffCount() { return diffCount; }
    public void setDiffCount(int diffCount) { this.diffCount = diffCount; }
    public int getDiffAmountCent() { return diffAmountCent; }
    public void setDiffAmountCent(int diffAmountCent) { this.diffAmountCent = diffAmountCent; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<StockTakeItem> getItems() { return items; }
    public void setItems(List<StockTakeItem> items) { this.items = items; }
}
