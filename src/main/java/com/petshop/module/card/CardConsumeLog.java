package com.petshop.module.card;

import java.time.LocalDateTime;

/** 卡核销流水（只增不改） */
public class CardConsumeLog {

    private Long id;
    private Long cardId;
    private Long billId;
    private Long billItemId;
    private int qty;
    private boolean fromGift;
    private String operator;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getBillItemId() { return billItemId; }
    public void setBillItemId(Long billItemId) { this.billItemId = billItemId; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public boolean getFromGift() { return fromGift; }
    public void setFromGift(boolean fromGift) { this.fromGift = fromGift; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
