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
