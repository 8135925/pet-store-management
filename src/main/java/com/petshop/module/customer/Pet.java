package com.petshop.module.customer;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Pet {

    private Long id;
    private Long ownerId;
    private String name;
    private String species;      // DOG / CAT / OTHER
    private String breed;
    private String gender;       // MALE / FEMALE / UNKNOWN
    private LocalDate birthday;
    private Integer weightG;
    private boolean isNeutered;
    private String color;
    private String habit;
    private boolean isActive;
    private LocalDateTime createdAt;

    /** 关联展示字段 */
    private String ownerName;
    private String ownerPhone;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
    public Integer getWeightG() { return weightG; }
    public void setWeightG(Integer weightG) { this.weightG = weightG; }
    public boolean getIsNeutered() { return isNeutered; }
    public void setIsNeutered(boolean neutered) { isNeutered = neutered; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getHabit() { return habit; }
    public void setHabit(String habit) { this.habit = habit; }
    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }

    /** 体型档位：按体重自动匹配（边界取大档） */
    public String sizeTier() {
        int w = weightG == null ? 0 : weightG;
        if (w <= 0) {
            return "S"; // 未录体重按小型
        }
        if (w < 10000) return "S";
        if (w < 25000) return "M";
        if (w < 40000) return "L";
        return "XL";
    }
}
