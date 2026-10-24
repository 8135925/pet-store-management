package com.petshop.module.bill;

public class BillItem {

    private Long id;
    private Long billId;
    private String itemType;      // SERVICE / PRODUCT
    private Long refId;
    private String nameSnapshot;
    private String sizeTier;
    private Long petId;
    private int qty;
    private int unitPriceCent;
    private int amountCent;
    private int discountCent;
    private boolean paidByCard;
    private Long cardId;
    private int sortNo;
    /** v1.2：本行实际服务人员（提成依据） */
    private Long staffId;
    /** v1.2：整行来自单一批次时冗余记录；跨批次为 null，明细看台账 */
    private Long batchId;

    /** 关联展示 */
    private String petName;
    private String cardNo;
    private String staffName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public String getNameSnapshot() { return nameSnapshot; }
    public void setNameSnapshot(String nameSnapshot) { this.nameSnapshot = nameSnapshot; }
    public String getSizeTier() { return sizeTier; }
    public void setSizeTier(String sizeTier) { this.sizeTier = sizeTier; }
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public int getUnitPriceCent() { return unitPriceCent; }
    public void setUnitPriceCent(int unitPriceCent) { this.unitPriceCent = unitPriceCent; }
    public int getAmountCent() { return amountCent; }
    public void setAmountCent(int amountCent) { this.amountCent = amountCent; }
    public int getDiscountCent() { return discountCent; }
    public void setDiscountCent(int discountCent) { this.discountCent = discountCent; }
    public boolean getPaidByCard() { return paidByCard; }
    public void setPaidByCard(boolean paidByCard) { this.paidByCard = paidByCard; }
    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }
    public int getSortNo() { return sortNo; }
    public void setSortNo(int sortNo) { this.sortNo = sortNo; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public String getCardNo() { return cardNo; }
    public void setCardNo(String cardNo) { this.cardNo = cardNo; }
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
}
