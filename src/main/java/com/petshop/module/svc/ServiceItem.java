package com.petshop.module.svc;

public class ServiceItem {

    private Long id;
    private String name;
    private int durationMin;
    private int priceSCent;
    private int priceMCent;
    private int priceLCent;
    private int priceXLCent;
    private int sortNo;
    private boolean isActive;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDurationMin() { return durationMin; }
    public void setDurationMin(int durationMin) { this.durationMin = durationMin; }
    public int getPriceSCent() { return priceSCent; }
    public void setPriceSCent(int priceSCent) { this.priceSCent = priceSCent; }
    public int getPriceMCent() { return priceMCent; }
    public void setPriceMCent(int priceMCent) { this.priceMCent = priceMCent; }
    public int getPriceLCent() { return priceLCent; }
    public void setPriceLCent(int priceLCent) { this.priceLCent = priceLCent; }
    public int getPriceXLCent() { return priceXLCent; }
    public void setPriceXLCent(int priceXLCent) { this.priceXLCent = priceXLCent; }
    public int getSortNo() { return sortNo; }
    public void setSortNo(int sortNo) { this.sortNo = sortNo; }
    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean active) { isActive = active; }

    public int priceOf(String tier) {
        if ("M".equals(tier)) return priceMCent;
        if ("L".equals(tier)) return priceLCent;
        if ("XL".equals(tier)) return priceXLCent;
        return priceSCent;
    }
}
