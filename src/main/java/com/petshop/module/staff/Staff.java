package com.petshop.module.staff;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 员工：与登录账号解耦，美容师可以没有系统账号。 */
public class Staff {

    private Long id;
    private String name;
    private String role;        // GROOMER / ASSISTANT / CASHIER / MANAGER
    private String phone;
    private LocalDate hireOn;
    private LocalDate leaveOn;
    private boolean isActive;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getHireOn() { return hireOn; }
    public void setHireOn(LocalDate hireOn) { this.hireOn = hireOn; }
    public LocalDate getLeaveOn() { return leaveOn; }
    public void setLeaveOn(LocalDate leaveOn) { this.leaveOn = leaveOn; }
    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getRoleLabel() {
        if ("GROOMER".equals(role)) return "美容师";
        if ("ASSISTANT".equals(role)) return "助理";
        if ("CASHIER".equals(role)) return "前台";
        if ("MANAGER".equals(role)) return "店长";
        return role;
    }
}
