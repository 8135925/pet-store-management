package com.petshop.module.product;

import java.time.LocalDateTime;

/** 库存流水（只增不改） */
public class ProductLedger {

    private Long id;
    private Long productId;
    private String bizType;   // IN / SALE / RETURN / CANCEL / ADJUST
    private int qtyChange;
    private int beforeQty;
    private int afterQty;
    /** 出库成本单价快照（分）：仅 SALE 行有值，供毛利报表使用 */
    private int unitCostCent;
    /** v1.2：本次变动对应的商品批次；历史数据 / 手工调整为 null */
    private Long batchId;
    private String refType;   // BILL / MANUAL / PURCHASE
    private Long refId;
    private String operator;
    private String remark;
    private LocalDateTime createdAt;

    private String productName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public int getQtyChange() { return qtyChange; }
    public void setQtyChange(int qtyChange) { this.qtyChange = qtyChange; }
    public int getBeforeQty() { return beforeQty; }
    public void setBeforeQty(int beforeQty) { this.beforeQty = beforeQty; }
    public int getAfterQty() { return afterQty; }
    public void setAfterQty(int afterQty) { this.afterQty = afterQty; }
    public int getUnitCostCent() { return unitCostCent; }
    public void setUnitCostCent(int unitCostCent) { this.unitCostCent = unitCostCent; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getRefType() { return refType; }
    public void setRefType(String refType) { this.refType = refType; }
    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}
