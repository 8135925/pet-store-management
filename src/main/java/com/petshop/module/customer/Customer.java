package com.petshop.module.customer;

import java.time.LocalDateTime;

public class Customer {

    private Long id;
    private String name;
    private String phone;
    private String source;
    private String remark;
    private boolean isActive;
    private LocalDateTime createdAt;

    /** 列表页附带：名下宠物数 */
    private Long petCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getPetCount() { return petCount; }
    public void setPetCount(Long petCount) { this.petCount = petCount; }
}
