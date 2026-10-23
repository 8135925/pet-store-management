package com.petshop.module.card;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 卡延期记录（只增不改） */
public class CardExtendLog {

    private Long id;
    private Long cardId;
    private LocalDate oldExpireOn;
    private LocalDate newExpireOn;
    private String reason;
    private String operator;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }
    public LocalDate getOldExpireOn() { return oldExpireOn; }
    public void setOldExpireOn(LocalDate oldExpireOn) { this.oldExpireOn = oldExpireOn; }
    public LocalDate getNewExpireOn() { return newExpireOn; }
    public void setNewExpireOn(LocalDate newExpireOn) { this.newExpireOn = newExpireOn; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
