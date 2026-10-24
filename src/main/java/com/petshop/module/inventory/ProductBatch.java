package com.petshop.module.inventory;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 商品批次：库存的最小可追溯单位。 */
public class ProductBatch {

    private Long id;
    private Long productId;
    private Long purchaseItemId;
    private String batchNo;
    private LocalDate expireOn;
    private int qtyIn;
    private int qtyLeft;
    private int unitCostCent;
    private String source;      // PURCHASE / OPENING / STOCK_TAKE / RETURN
    private String status;      // NORMAL / EXHAUSTED / EXPIRED / VOID
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;

    /** 展示用 */
    private String productName;
    private String productSpec;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getPurchaseItemId() { return purchaseItemId; }
    public void setPurchaseItemId(Long purchaseItemId) { this.purchaseItemId = purchaseItemId; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public LocalDate getExpireOn() { return expireOn; }
    public void setExpireOn(LocalDate expireOn) { this.expireOn = expireOn; }
    public int getQtyIn() { return qtyIn; }
    public void setQtyIn(int qtyIn) { this.qtyIn = qtyIn; }
    public int getQtyLeft() { return qtyLeft; }
    public void setQtyLeft(int qtyLeft) { this.qtyLeft = qtyLeft; }
    public int getUnitCostCent() { return unitCostCent; }
    public void setUnitCostCent(int unitCostCent) { this.unitCostCent = unitCostCent; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductSpec() { return productSpec; }
    public void setProductSpec(String productSpec) { this.productSpec = productSpec; }

    public boolean isExpired() {
        return expireOn != null && expireOn.isBefore(LocalDate.now());
    }

    public boolean isExpiringSoon(int days) {
        if (expireOn == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !expireOn.isBefore(today) && !expireOn.isAfter(today.plusDays(days));
    }
}
