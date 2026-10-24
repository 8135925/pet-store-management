package com.petshop.module.crm;

import java.time.LocalDateTime;

/** 宠物标签 / 客户标签共用（两张表字段一致）。 */
public class Tag {

    private Long id;
    private Long ownerId;      // pet_id 或 customer_id
    private String tagCode;
    private String note;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getTagCode() { return tagCode; }
    public void setTagCode(String tagCode) { this.tagCode = tagCode; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getLabel() {
        return TagLabels.petLabel(tagCode);
    }
}
