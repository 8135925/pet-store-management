package com.petshop.module.card;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CustomerCard {

    private Long id;
    private String cardNo;
    private Long customerId;
    private Long cardTypeId;
    private int paidQty;
    private int giftQty;
    private int usedPaid;
    private int usedGift;
    private int paidAmountCent;
    private LocalDate issuedOn;
    private LocalDate expireOn;
    private String status;       // ACTIVE / EXPIRED / VOID
    private Long sourceBillId;
    private String remark;
    private LocalDateTime createdAt;

    /** 关联展示 */
    private String customerName;
    private String customerPhone;
    private String cardTypeName;

    /** 剩余次数：实时计算，不落库 */
    public int remain() {
        return paidQty - usedPaid + giftQty - usedGift;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCardNo() { return cardNo; }
    public void setCardNo(String cardNo) { this.cardNo = cardNo; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getCardTypeId() { return cardTypeId; }
    public void setCardTypeId(Long cardTypeId) { this.cardTypeId = cardTypeId; }
    public int getPaidQty() { return paidQty; }
    public void setPaidQty(int paidQty) { this.paidQty = paidQty; }
    public int getGiftQty() { return giftQty; }
    public void setGiftQty(int giftQty) { this.giftQty = giftQty; }
    public int getUsedPaid() { return usedPaid; }
    public void setUsedPaid(int usedPaid) { this.usedPaid = usedPaid; }
    public int getUsedGift() { return usedGift; }
    public void setUsedGift(int usedGift) { this.usedGift = usedGift; }
    public int getPaidAmountCent() { return paidAmountCent; }
    public void setPaidAmountCent(int paidAmountCent) { this.paidAmountCent = paidAmountCent; }
    public LocalDate getIssuedOn() { return issuedOn; }
    public void setIssuedOn(LocalDate issuedOn) { this.issuedOn = issuedOn; }
    public LocalDate getExpireOn() { return expireOn; }
    public void setExpireOn(LocalDate expireOn) { this.expireOn = expireOn; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getSourceBillId() { return sourceBillId; }
    public void setSourceBillId(Long sourceBillId) { this.sourceBillId = sourceBillId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCardTypeName() { return cardTypeName; }
    public void setCardTypeName(String cardTypeName) { this.cardTypeName = cardTypeName; }
}
