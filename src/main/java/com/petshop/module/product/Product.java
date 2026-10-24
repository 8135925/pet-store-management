package com.petshop.module.product;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Product {

    private Long id;
    private String name;
    private String barcode;
    private int salePriceCent;
    private int costPriceCent;
    private int stockQty;
    private int safetyQty;
    private LocalDate expireOn;
    private boolean isActive;
    private LocalDateTime createdAt;
    /** v1.2：规格，如 2kg / 500ml */
    private String spec;
    /** v1.2：分类 MAIN_FOOD / SNACK / SUPPLIES / HEALTH / OTHER */
    private String category;
    /** v1.2：保质期天数，入库时可按生产日期推算效期 */
    private Integer shelfLifeDays;
    /** v1.2：默认供应商 */
    private Long defaultSupplierId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public int getSalePriceCent() { return salePriceCent; }
    public void setSalePriceCent(int salePriceCent) { this.salePriceCent = salePriceCent; }
    public int getCostPriceCent() { return costPriceCent; }
    public void setCostPriceCent(int costPriceCent) { this.costPriceCent = costPriceCent; }
    public int getStockQty() { return stockQty; }
    public void setStockQty(int stockQty) { this.stockQty = stockQty; }
    public int getSafetyQty() { return safetyQty; }
    public void setSafetyQty(int safetyQty) { this.safetyQty = safetyQty; }
    public LocalDate getExpireOn() { return expireOn; }
    public void setExpireOn(LocalDate expireOn) { this.expireOn = expireOn; }
    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getShelfLifeDays() { return shelfLifeDays; }
    public void setShelfLifeDays(Integer shelfLifeDays) { this.shelfLifeDays = shelfLifeDays; }
    public Long getDefaultSupplierId() { return defaultSupplierId; }
    public void setDefaultSupplierId(Long defaultSupplierId) { this.defaultSupplierId = defaultSupplierId; }

    public boolean isLowStock(int globalWarn) {
        return safetyQty > 0 ? stockQty <= safetyQty : stockQty <= globalWarn;
    }

    public boolean isExpiringSoon(int days) {
        if (expireOn == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !expireOn.isBefore(today) && !expireOn.isAfter(today.plusDays(days));
    }

    public boolean isExpired() {
        return expireOn != null && expireOn.isBefore(LocalDate.now());
    }
}
