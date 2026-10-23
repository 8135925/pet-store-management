package com.petshop.module.reminder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 免疫/驱虫/体检记录 */
public class PetEvent {

    private Long id;
    private Long petId;
    private String eventType;    // VACCINE / RABIES / DEWORM / CHECKUP
    private LocalDate happenedOn;
    private LocalDate nextDueOn;
    private String hospital;
    private String remark;
    private String contactStatus; // NULL / CONTACTED / BOOKED / DONE / IGNORED
    private LocalDateTime contactedAt;
    private String contactedBy;
    private LocalDateTime createdAt;

    /** 关联展示 */
    private String petName;
    private String species;
    private String breed;
    private String ownerName;
    private String ownerPhone;
    private Long ownerId;

    /** 类型默认提醒间隔（天） */
    public static int defaultInterval(String type) {
        if ("VACCINE".equals(type) || "RABIES".equals(type)) return 365;
        if ("DEWORM".equals(type)) return 90;
        return 365;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public LocalDate getHappenedOn() { return happenedOn; }
    public void setHappenedOn(LocalDate happenedOn) { this.happenedOn = happenedOn; }
    public LocalDate getNextDueOn() { return nextDueOn; }
    public void setNextDueOn(LocalDate nextDueOn) { this.nextDueOn = nextDueOn; }
    public String getHospital() { return hospital; }
    public void setHospital(String hospital) { this.hospital = hospital; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getContactStatus() { return contactStatus; }
    public void setContactStatus(String contactStatus) { this.contactStatus = contactStatus; }
    public LocalDateTime getContactedAt() { return contactedAt; }
    public void setContactedAt(LocalDateTime contactedAt) { this.contactedAt = contactedAt; }
    public String getContactedBy() { return contactedBy; }
    public void setContactedBy(String contactedBy) { this.contactedBy = contactedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
}
