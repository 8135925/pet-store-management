package com.petshop.module.card;

import com.petshop.common.BizException;
import com.petshop.common.BusinessDate;
import com.petshop.common.ErrorCode;
import com.petshop.common.Money;
import com.petshop.common.SessionUser;
import com.petshop.module.system.ConfigService;
import com.petshop.module.bill.Bill;
import com.petshop.module.bill.BillMapper;
import com.petshop.module.customer.CustomerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class CardService {

    private static final Logger log = LoggerFactory.getLogger(CardService.class);

    private final CardTypeMapper typeMapper;
    private final CustomerCardMapper cardMapper;
    private final CardLogMapper logMapper;
    private final CustomerMapper customerMapper;
    private final BillMapper billMapper;
    private final ConfigService configService;

    public CardService(CardTypeMapper typeMapper, CustomerCardMapper cardMapper,
                       CardLogMapper logMapper, CustomerMapper customerMapper,
                       BillMapper billMapper, ConfigService configService) {
        this.typeMapper = typeMapper;
        this.cardMapper = cardMapper;
        this.logMapper = logMapper;
        this.customerMapper = customerMapper;
        this.billMapper = billMapper;
        this.configService = configService;
    }

    // ---------------- 卡类型 ----------------

    public CardType findType(Long id) {
        CardType t = typeMapper.findById(id);
        if (t == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "卡类型不存在");
        }
        return t;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveType(Long id, String name, String serviceIds, int paidQty, int giftQty,
                         String priceYuan, int validDays) {
        if (name == null || name.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "卡名称不能为空");
        }
        if (paidQty <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "付费次数必须大于 0");
        }
        if (giftQty < 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "赠送次数不能为负数");
        }
        int priceCent = Money.toCent(priceYuan);
        if (priceCent <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "售价必须大于 0");
        }
        if (validDays <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "有效天数必须大于 0");
        }
        CardType t = id == null ? new CardType() : typeMapper.findById(id);
        if (t == null) {
            t = new CardType();
        }
        t.setName(name.trim());
        t.setServiceIds(serviceIds == null ? "" : serviceIds.trim());
        t.setPaidQty(paidQty);
        t.setGiftQty(giftQty);
        t.setPriceCent(priceCent);
        t.setValidDays(validDays);
        t.setIsActive(true);
        if (id == null) {
            typeMapper.insert(t);
        } else {
            typeMapper.update(t);
        }
    }

    // ---------------- 售卡（F1） ----------------

    @Transactional(rollbackFor = Exception.class)
    public Long sell(Long customerId, Long cardTypeId, String receivedYuan, String payMethod, SessionUser user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate businessDate = BusinessDate.of(now);

        if (customerMapper.findById(customerId) == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "客户不存在");
        }
        CardType type = typeMapper.findById(cardTypeId);
        if (type == null || !type.getIsActive()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "卡类型不存在或已停售");
        }
        int receivedCent = Money.toCent(receivedYuan);
        if (receivedCent <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "实收金额必须大于 0");
        }

        // 1. 生成卡
        CustomerCard card = new CustomerCard();
        card.setCardNo(nextCardNo(LocalDate.now()));
        card.setCustomerId(customerId);
        card.setCardTypeId(type.getId());
        card.setPaidQty(type.getPaidQty());
        card.setGiftQty(type.getGiftQty());
        card.setUsedPaid(0);
        card.setUsedGift(0);
        card.setPaidAmountCent(receivedCent);
        card.setIssuedOn(LocalDate.now());
        card.setExpireOn(LocalDate.now().plusDays(type.getValidDays()));
        card.setStatus("ACTIVE");
        card.setRemark(null);
        card.setCreatedAt(now);
        cardMapper.insert(card);

        // 2. 售卡收款单（bill_type=CARD，不写明细）
        Bill b = new Bill();
        b.setBillNo(nextBillNo("Z", businessDate));
        b.setBillType("CARD");
        b.setCustomerId(customerId);
        b.setPetId(null);
        b.setStatus("SETTLED");
        b.setTotalCent(type.getPriceCent());
        b.setDiscountCent(0);
        b.setReceivableCent(receivedCent);
        b.setReceivedCent(receivedCent);
        b.setPayMethod(payMethod);
        b.setBusinessDate(businessDate);
        b.setSettledAt(now);
        b.setRequestId(null);
        b.setOperator(user.getDisplayName());
        b.setRemark("售卡：" + type.getName() + "，卡号 " + card.getCardNo());
        b.setCreatedAt(now);
        billMapper.insert(b);

        cardMapper.updateSourceBill(card.getId(), b.getId());
        return card.getId();
    }

    // ---------------- 核销扣次（F2，最关键的一段） ----------------

    /**
     * 原子扣次并写核销流水。调用方处于结账事务内。
     * @return fromGift：本次扣的是否赠送次数（写流水用，反结账据此还原）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deduct(Long cardId, Long billId, SessionUser user, LocalDateTime now) {
        LocalDate today = BusinessDate.of(now);
        CustomerCard before = cardMapper.findById(cardId);
        if (before == null) {
            throw new BizException(ErrorCode.CARD_NOT_FOUND);
        }
        boolean giftFirst = configService.giftFirst();
        int rows = giftFirst
                ? cardMapper.deductGiftFirst(cardId, today)
                : cardMapper.deductPaidFirst(cardId, today);
        if (rows == 0) {
            throw diagnose(before, today);
        }
        // 通过扣减前后对比判断扣的是哪一类（同一事务内可见）
        CustomerCard after = cardMapper.findById(cardId);
        boolean fromGift = after.getUsedGift() != before.getUsedGift();
        log.debug("核销 card={} bill={} fromGift={}", cardId, billId, fromGift);
        return fromGift;
    }

    private BizException diagnose(CustomerCard c, LocalDate today) {
        if ("VOID".equals(c.getStatus())) {
            return new BizException(ErrorCode.CARD_VOID);
        }
        if ("EXPIRED".equals(c.getStatus()) || c.getExpireOn().isBefore(today)) {
            return new BizException(ErrorCode.CARD_EXPIRED);
        }
        if (c.remain() <= 0) {
            return new BizException(ErrorCode.CARD_NO_BALANCE);
        }
        return new BizException(ErrorCode.SYSTEM_ERROR, "核销失败，请重试");
    }

    /** 核销流水（在 bill_item 拿到 id 后由 BillService 调用） */
    public void writeConsumeLog(Long cardId, Long billId, Long billItemId, boolean fromGift,
                                SessionUser user, LocalDateTime now) {
        CardConsumeLog l = new CardConsumeLog();
        l.setCardId(cardId);
        l.setBillId(billId);
        l.setBillItemId(billItemId);
        l.setQty(1);
        l.setFromGift(fromGift);
        l.setOperator(user.getDisplayName());
        l.setCreatedAt(now);
        logMapper.insertConsume(l);
    }

    // ---------------- 延期 / 作废（仅 ADMIN） ----------------

    @Transactional(rollbackFor = Exception.class)
    public void extend(Long cardId, String newExpire, String reason, SessionUser user) {
        CustomerCard card = mustFind(cardId);
        if ("VOID".equals(card.getStatus())) {
            throw new BizException(ErrorCode.CARD_VOID);
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "延期原因必填");
        }
        LocalDate newDate;
        try {
            newDate = LocalDate.parse(newExpire.trim());
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "到期日格式不正确");
        }
        if (!newDate.isAfter(card.getExpireOn())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "新到期日必须晚于当前到期日");
        }
        String status = newDate.isBefore(LocalDate.now()) ? "EXPIRED" : "ACTIVE";
        cardMapper.updateExpire(cardId, newDate, status);

        CardExtendLog l = new CardExtendLog();
        l.setCardId(cardId);
        l.setOldExpireOn(card.getExpireOn());
        l.setNewExpireOn(newDate);
        l.setReason(reason.trim());
        l.setOperator(user.getDisplayName());
        l.setCreatedAt(LocalDateTime.now());
        logMapper.insertExtend(l);
    }

    @Transactional(rollbackFor = Exception.class)
    public void voidCard(Long cardId, String reason, SessionUser user) {
        CustomerCard card = mustFind(cardId);
        if ("VOID".equals(card.getStatus())) {
            throw new BizException(ErrorCode.CARD_VOID, "该卡已是作废状态");
        }
        boolean consumed = card.getUsedPaid() > 0 || card.getUsedGift() > 0;
        if (consumed && (reason == null || reason.trim().isEmpty())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "该卡已有核销记录，作废必须填写原因");
        }
        cardMapper.setStatus(cardId, "VOID");
        log.info("卡作废 card={} by={} reason={}", card.getCardNo(), user.getDisplayName(), reason);
    }

    public CustomerCard mustFind(Long id) {
        CustomerCard c = cardMapper.findById(id);
        if (c == null) {
            throw new BizException(ErrorCode.CARD_NOT_FOUND);
        }
        return c;
    }

    // ---------------- 单号 ----------------

    private String nextCardNo(LocalDate date) {
        String prefix = "C" + date.format(BusinessDate.COMPACT);
        long seq = cardMapper.countByNoPrefix(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }

    public String nextBillNo(String typePrefix, LocalDate businessDate) {
        String prefix = typePrefix + businessDate.format(BusinessDate.COMPACT);
        long seq = billMapper.countByNoPrefix(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }
}
