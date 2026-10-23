package com.petshop.module.card;

import java.util.ArrayList;
import java.util.List;

public class CardType {

    private Long id;
    private String name;
    private String serviceIds;   // 适用服务 id 列表，逗号分隔
    private int paidQty;
    private int giftQty;
    private int priceCent;
    private int validDays;
    private boolean isActive;

    /** 适用服务名称（展示用） */
    private String serviceNames;

    public List<Long> serviceIdList() {
        List<Long> ids = new ArrayList<>();
        if (serviceIds != null) {
            for (String s : serviceIds.split(",")) {
                if (!s.trim().isEmpty()) {
                    try {
                        ids.add(Long.parseLong(s.trim()));
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
        return ids;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getServiceIds() { return serviceIds; }
    public void setServiceIds(String serviceIds) { this.serviceIds = serviceIds; }
    public int getPaidQty() { return paidQty; }
    public void setPaidQty(int paidQty) { this.paidQty = paidQty; }
    public int getGiftQty() { return giftQty; }
    public void setGiftQty(int giftQty) { this.giftQty = giftQty; }
    public int getPriceCent() { return priceCent; }
    public void setPriceCent(int priceCent) { this.priceCent = priceCent; }
    public int getValidDays() { return validDays; }
    public void setValidDays(int validDays) { this.validDays = validDays; }
    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean active) { isActive = active; }
    public String getServiceNames() { return serviceNames; }
    public void setServiceNames(String serviceNames) { this.serviceNames = serviceNames; }
}
