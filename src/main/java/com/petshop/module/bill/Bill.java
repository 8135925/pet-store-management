package com.petshop.module.bill;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class Bill {

    private Long id;
    private String billNo;
    private String billType;      // SALE / CARD
    private Long customerId;
    private Long petId;
    private String status;        // OPEN / SETTLED / CANCELLED
    private int totalCent;
    private int discountCent;
    private int receivableCent;
    private int receivedCent;
    private String payMethod;
    private LocalDate businessDate;
    private LocalDateTime settledAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private String requestId;
    private String operator;
    private String remark;
    private LocalDateTime createdAt;

    /** 关联展示 */
    private String customerName;
    private String customerPhone;
    private String petName;
    private List<BillItem> items;
    private List<CardConsumeLogRef> consumeLogs;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }
    public String getBillType() { return billType; }
    public void setBillType(String billType) { this.billType = billType; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalCent() { return totalCent; }
    public void setTotalCent(int totalCent) { this.totalCent = totalCent; }
    public int getDiscountCent() { return discountCent; }
    public void setDiscountCent(int discountCent) { this.discountCent = discountCent; }
    public int getReceivableCent() { return receivableCent; }
    public void setReceivableCent(int receivableCent) { this.receivableCent = receivableCent; }
    public int getReceivedCent() { return receivedCent; }
    public void setReceivedCent(int receivedCent) { this.receivedCent = receivedCent; }
    public String getPayMethod() { return payMethod; }
    public void setPayMethod(String payMethod) { this.payMethod = payMethod; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public List<BillItem> getItems() { return items; }
    public void setItems(List<BillItem> items) { this.items = items; }
    public List<CardConsumeLogRef> getConsumeLogs() { return consumeLogs; }
    public void setConsumeLogs(List<CardConsumeLogRef> consumeLogs) { this.consumeLogs = consumeLogs; }

    /** 卡详情页流水引用（避免直接依赖 card 模块造成循环） */
    public static class CardConsumeLogRef {
        private Long cardId;
        private String cardNo;
        private boolean fromGift;
        private int qty;
        private String operator;
        private LocalDateTime createdAt;

        public Long getCardId() { return cardId; }
        public void setCardId(Long cardId) { this.cardId = cardId; }
        public String getCardNo() { return cardNo; }
        public void setCardNo(String cardNo) { this.cardNo = cardNo; }
        public boolean getFromGift() { return fromGift; }
        public void setFromGift(boolean fromGift) { this.fromGift = fromGift; }
        public int getQty() { return qty; }
        public void setQty(int qty) { this.qty = qty; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
