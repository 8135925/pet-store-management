package com.petshop.module.bill;

import java.util.List;

/** 开单表单（items 由前端 JS 组装成 JSON 提交） */
public class BillForm {

    private Long customerId;
    private Long petId;
    private List<Item> items;
    private Integer discountCent;
    private Integer receivedCent;
    private String payMethod;
    private String requestId;
    private String remark;
    /** v1.1：来源预约（到店转开单），结算成功后回写 DONE */
    private Long appointmentId;

    public static class Item {
        private String itemType;   // SERVICE / PRODUCT
        private Long refId;
        private Long petId;
        private String sizeTier;
        private Long cardId;       // 非空表示用卡抵扣
        private int qty;
        /** v1.2：本行服务人员，提成依据；为空表示不提成 */
        private Long staffId;

        public Long getStaffId() { return staffId; }
        public void setStaffId(Long staffId) { this.staffId = staffId; }

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        public Long getRefId() { return refId; }
        public void setRefId(Long refId) { this.refId = refId; }
        public Long getPetId() { return petId; }
        public void setPetId(Long petId) { this.petId = petId; }
        public String getSizeTier() { return sizeTier; }
        public void setSizeTier(String sizeTier) { this.sizeTier = sizeTier; }
        public Long getCardId() { return cardId; }
        public void setCardId(Long cardId) { this.cardId = cardId; }
        public int getQty() { return qty; }
        public void setQty(int qty) { this.qty = qty; }
    }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
    public Integer getDiscountCent() { return discountCent; }
    public void setDiscountCent(Integer discountCent) { this.discountCent = discountCent; }
    public Integer getReceivedCent() { return receivedCent; }
    public void setReceivedCent(Integer receivedCent) { this.receivedCent = receivedCent; }
    public String getPayMethod() { return payMethod; }
    public void setPayMethod(String payMethod) { this.payMethod = payMethod; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
}
