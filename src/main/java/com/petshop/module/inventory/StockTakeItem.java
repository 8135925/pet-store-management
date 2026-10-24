package com.petshop.module.inventory;

public class StockTakeItem {

    private Long id;
    private Long takeId;
    private Long productId;
    private int systemQty;
    private Integer countedQty;
    private Integer diffQty;
    private int costCent;
    private String reason;
    private String remark;

    /** 展示用 */
    private String productName;
    private String productSpec;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTakeId() { return takeId; }
    public void setTakeId(Long takeId) { this.takeId = takeId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public int getSystemQty() { return systemQty; }
    public void setSystemQty(int systemQty) { this.systemQty = systemQty; }
    public Integer getCountedQty() { return countedQty; }
    public void setCountedQty(Integer countedQty) { this.countedQty = countedQty; }
    public Integer getDiffQty() { return diffQty; }
    public void setDiffQty(Integer diffQty) { this.diffQty = diffQty; }
    public int getCostCent() { return costCent; }
    public void setCostCent(int costCent) { this.costCent = costCent; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductSpec() { return productSpec; }
    public void setProductSpec(String productSpec) { this.productSpec = productSpec; }

    /** 已录入实盘时的差异；未录入返回 null */
    public Integer getComputedDiff() {
        return countedQty == null ? null : countedQty - systemQty;
    }
}
