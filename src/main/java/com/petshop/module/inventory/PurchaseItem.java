package com.petshop.module.inventory;

public class PurchaseItem {

    private Long id;
    private Long purchaseId;
    private Long productId;
    private int qty;
    private int unitCostCent;
    private int amountCent;
    private String batchNo;
    private String expireOn;        // 'yyyy-MM-dd'，可空
    private String remark;

    /** 展示用 */
    private String productName;
    private String productSpec;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Long purchaseId) { this.purchaseId = purchaseId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public int getUnitCostCent() { return unitCostCent; }
    public void setUnitCostCent(int unitCostCent) { this.unitCostCent = unitCostCent; }
    public int getAmountCent() { return amountCent; }
    public void setAmountCent(int amountCent) { this.amountCent = amountCent; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getExpireOn() { return expireOn; }
    public void setExpireOn(String expireOn) { this.expireOn = expireOn; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductSpec() { return productSpec; }
    public void setProductSpec(String productSpec) { this.productSpec = productSpec; }
}
