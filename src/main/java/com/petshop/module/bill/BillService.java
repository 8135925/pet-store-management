package com.petshop.module.bill;

import com.petshop.common.BizException;
import com.petshop.common.BusinessDate;
import com.petshop.common.ErrorCode;
import com.petshop.common.Money;
import com.petshop.common.SessionUser;
import com.petshop.module.system.ConfigService;
import com.petshop.module.card.CardConsumeLog;
import com.petshop.module.card.CardService;
import com.petshop.module.card.CustomerCardMapper;
import com.petshop.module.customer.Pet;
import com.petshop.module.customer.PetMapper;
import com.petshop.module.product.Product;
import com.petshop.module.product.ProductMapper;
import com.petshop.module.product.ProductService;
import com.petshop.module.svc.ServiceItem;
import com.petshop.module.svc.ServiceItemMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 结账 / 反结账：金额重算 + 折扣分摊 + 卡核销 + 库存扣减，全部在单事务内。 */
@Service
public class BillService {

    private static final Logger log = LoggerFactory.getLogger(BillService.class);

    /** 单行数量上限：防止 int 金额溢出与误输入 */
    private static final int MAX_LINE_QTY = 9999;
    /** 单据明细行数上限：防止超大批量请求拖垮服务 */
    private static final int MAX_LINE_COUNT = 200;
    /** 分页每页上限 */
    private static final int MAX_PAGE_SIZE = 100;

    private final BillMapper billMapper;
    private final BillItemMapper itemMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final ProductMapper productMapper;
    private final PetMapper petMapper;
    private final CardService cardService;
    private final CustomerCardMapper cardMapper;
    private final com.petshop.module.card.CardLogMapper cardLogMapper;
    private final ProductService productService;
    private final ConfigService configService;
    private final com.petshop.module.appointment.AppointmentMapper appointmentMapper;
    private final com.petshop.module.customer.CustomerMapper customerMapper;
    private final com.petshop.module.staff.CommissionService commissionService;

    public BillService(BillMapper billMapper, BillItemMapper itemMapper,
                       ServiceItemMapper serviceItemMapper, ProductMapper productMapper,
                       PetMapper petMapper, CardService cardService, CustomerCardMapper cardMapper,
                       com.petshop.module.card.CardLogMapper cardLogMapper,
                       ProductService productService, ConfigService configService,
                       com.petshop.module.appointment.AppointmentMapper appointmentMapper,
                       com.petshop.module.customer.CustomerMapper customerMapper,
                       com.petshop.module.staff.CommissionService commissionService) {
        this.billMapper = billMapper;
        this.itemMapper = itemMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.productMapper = productMapper;
        this.petMapper = petMapper;
        this.cardService = cardService;
        this.cardMapper = cardMapper;
        this.cardLogMapper = cardLogMapper;
        this.productService = productService;
        this.configService = configService;
        this.appointmentMapper = appointmentMapper;
        this.customerMapper = customerMapper;
        this.commissionService = commissionService;
    }

    // ---------------- 结账（F4 主流程） ----------------

    @Transactional(rollbackFor = Exception.class)
    public Long settle(BillForm form, SessionUser user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate businessDate = BusinessDate.of(now);

        // 1. 幂等：requestId 已存在 → 直接返回已有单（统一 trim，避免带空格绕过唯一索引）
        String requestId = form.getRequestId() == null ? null : form.getRequestId().trim();
        if (requestId != null && requestId.isEmpty()) {
            requestId = null;
        }
        if (requestId != null) {
            Bill exists = billMapper.findByRequestId(requestId);
            if (exists != null) {
                log.info("重复提交被幂等拦截 requestId={}", requestId);
                return exists.getId();
            }
        }

        // 2. 基本校验
        if (form.getCustomerId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "必须选择客户");
        }
        if (customerMapper.findById(form.getCustomerId()) == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "客户不存在");
        }
        List<BillForm.Item> items = form.getItems();
        if (items == null || items.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "单据明细不能为空");
        }
        if (items.size() > MAX_LINE_COUNT) {
            throw new BizException(ErrorCode.PARAM_INVALID, "单据明细行数不能超过 " + MAX_LINE_COUNT);
        }
        String payMethod = form.getPayMethod();
        if (payMethod == null || payMethod.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "必须选择收款方式");
        }
        if (!isValidPayMethod(payMethod)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "收款方式不正确");
        }

        // 3. 逐条重算明细金额（不信任前端金额）
        int n = items.size();
        String[] nameSnapshots = new String[n];
        int[] unitPrices = new int[n];
        int[] amounts = new int[n];          // qty × 单价（优惠前）
        String[] tiers = new String[n];
        boolean[] cardUsed = new boolean[n];
        int total = 0;
        boolean hasService = false;

        for (int i = 0; i < n; i++) {
            BillForm.Item f = items.get(i);
            int qty = f.getQty() <= 0 ? 1 : f.getQty();
            if (qty > MAX_LINE_QTY) {
                throw new BizException(ErrorCode.PARAM_INVALID, "单行数量不能超过 " + MAX_LINE_QTY);
            }
            if ("SERVICE".equals(f.getItemType())) {
                hasService = true;
                if (f.getRefId() == null) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "服务项目不正确");
                }
                if (f.getPetId() == null) {
                    throw new BizException(ErrorCode.PET_REQUIRED);
                }
                Pet pet = petMapper.findById(f.getPetId());
                if (pet == null) {
                    throw new BizException(ErrorCode.PET_REQUIRED);
                }
                // 服务必须记在实际主人名下，防止把别人宠物的服务挂到本单
                if (!form.getCustomerId().equals(pet.getOwnerId())) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "所选宠物不属于该客户");
                }
                ServiceItem s = serviceItemMapper.findById(f.getRefId());
                if (s == null) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "服务项目不存在");
                }
                // 体型档位：前端可手工改档，否则按体重自动匹配
                String tier = (f.getSizeTier() == null || f.getSizeTier().trim().isEmpty())
                        ? pet.sizeTier() : f.getSizeTier().trim();
                if (!"S".equals(tier) && !"M".equals(tier) && !"L".equals(tier) && !"XL".equals(tier)) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "体型档位不正确");
                }
                tiers[i] = tier;
                unitPrices[i] = s.priceOf(tier);
                nameSnapshots[i] = s.getName(); // BR-06 名称快照
                if (f.getCardId() != null) {
                    if (qty != 1) {
                        throw new BizException(ErrorCode.PARAM_INVALID, "使用次卡抵扣的明细数量必须为 1");
                    }
                    // 校验卡适用该服务（卡类型未配置适用服务 = 不限）
                    com.petshop.module.card.CustomerCard card =
                            cardService.mustFind(f.getCardId());
                    if (card.getCardTypeId() != null) {
                        List<Long> applicable = cardService.findType(card.getCardTypeId()).serviceIdList();
                        if (!applicable.isEmpty() && !applicable.contains(s.getId())) {
                            throw new BizException(ErrorCode.CARD_NOT_MATCH_SERVICE);
                        }
                    }
                    cardUsed[i] = true;
                }
            } else if ("PRODUCT".equals(f.getItemType())) {
                if (f.getRefId() == null) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "商品不正确");
                }
                Product p = productMapper.findById(f.getRefId());
                if (p == null || !p.getIsActive()) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "商品不存在或已停售");
                }
                unitPrices[i] = p.getSalePriceCent();
                nameSnapshots[i] = p.getName();
                tiers[i] = null;
                cardUsed[i] = false;
            } else {
                throw new BizException(ErrorCode.PARAM_INVALID, "明细类型不正确");
            }
            long lineAmount = (long) unitPrices[i] * qty;
            if (lineAmount > Integer.MAX_VALUE) {
                throw new BizException(ErrorCode.PARAM_INVALID, "单行金额超出上限");
            }
            amounts[i] = (int) lineAmount;
            if (total > Integer.MAX_VALUE - amounts[i]) {
                throw new BizException(ErrorCode.PARAM_INVALID, "单据金额超出上限");
            }
            total += amounts[i];
        }

        // 4. 折扣：只用非卡抵扣明细做基数分摊（卡抵扣明细金额记 0）
        int discount = form.getDiscountCent() == null ? 0 : Math.max(0, form.getDiscountCent());
        List<Integer> baseIdx = new ArrayList<>();
        int[] baseAmounts;
        for (int i = 0; i < n; i++) {
            if (!cardUsed[i] && amounts[i] > 0) {
                baseIdx.add(i);
            }
        }
        baseAmounts = new int[baseIdx.size()];
        int baseSum = 0;
        for (int k = 0; k < baseIdx.size(); k++) {
            baseAmounts[k] = amounts[baseIdx.get(k)];
            baseSum += baseAmounts[k];
        }
        if (discount > baseSum) {
            throw new BizException(ErrorCode.DISCOUNT_EXCEED_TOTAL);
        }
        int[] alloc = Money.allocate(discount, baseAmounts);
        int[] itemDiscount = new int[n];
        for (int k = 0; k < baseIdx.size(); k++) {
            itemDiscount[baseIdx.get(k)] = alloc[k];
        }

        // 5. 应收 = Σ(明细折后金额)，卡抵扣明细金额为 0（钱在售卡当天已收，不再计应收）
        int receivable = 0;
        for (int i = 0; i < n; i++) {
            if (cardUsed[i]) {
                continue;
            }
            receivable += amounts[i] - itemDiscount[i];
        }

        // 6. 生成单号，先插主单占位（撞单号 / 撞幂等键分别处理）
        Bill b = new Bill();
        b.setBillType("SALE");
        b.setCustomerId(form.getCustomerId());
        b.setPetId(form.getPetId());
        b.setStatus("OPEN");
        b.setTotalCent(total);
        b.setDiscountCent(discount);
        b.setReceivableCent(receivable);
        b.setReceivedCent(receivable);
        b.setPayMethod(payMethod.trim());
        b.setBusinessDate(businessDate);
        b.setRequestId(requestId);
        b.setOperator(user.getDisplayName());
        b.setRemark(form.getRemark());
        b.setCreatedAt(now);
        Long duplicatedBillId = insertBillWithRetry(b, businessDate, requestId);
        if (duplicatedBillId != null) {
            return duplicatedBillId;
        }

        // 7. 逐条处理明细：核销 / 扣库存 / 写明细
        for (int i = 0; i < n; i++) {
            BillForm.Item f = items.get(i);
            int qty = f.getQty() <= 0 ? 1 : f.getQty();

            BillItem item = new BillItem();
            item.setBillId(b.getId());
            item.setItemType(f.getItemType());
            item.setRefId(f.getRefId());
            item.setNameSnapshot(nameSnapshots[i]);
            item.setSizeTier(tiers[i]);
            item.setPetId(f.getPetId());
            item.setQty(qty);
            item.setUnitPriceCent(unitPrices[i]);
            item.setSortNo(i);
            item.setCardId(f.getCardId());
            item.setStaffId(f.getStaffId());

            if (cardUsed[i]) {
                // a) 次卡抵扣：扣次 → 明细金额记 0（BR-01/BR-02/BR-07）
                boolean fromGift = cardService.deduct(f.getCardId(), b.getId(), user, now);
                item.setPaidByCard(true);
                item.setAmountCent(0);
                item.setDiscountCent(0);
                itemMapper.insert(item);
                cardService.writeConsumeLog(f.getCardId(), b.getId(), item.getId(), fromGift, user, now);
            } else {
                if ("PRODUCT".equals(f.getItemType())) {
                    // b) 商品：FEFO 扣批次 + 流水（BR-07 / BR-15）
                    item.setBatchId(productService.deductForSale(
                            f.getRefId(), qty, b.getId(), user.getDisplayName()));
                }
                item.setPaidByCard(false);
                item.setAmountCent(amounts[i] - itemDiscount[i]);
                item.setDiscountCent(itemDiscount[i]);
                itemMapper.insert(item);
            }
        }

        // 8. 结账（带状态条件，防止重复结账）
        int settled = billMapper.settle(b.getId(), receivable, payMethod.trim(), now);
        if (settled == 0) {
            throw new BizException(ErrorCode.BILL_ALREADY_SETTLED);
        }

        // 9. 提成：按明细服务人员与规则生成记录（与结账同事务，BR-19/BR-20）
        commissionService.recordForBill(b, itemMapper.findByBill(b.getId()), user);

        // 10. 来源预约回写（v1.1 F9）：ARRIVED → DONE，与结账同一事务
        if (form.getAppointmentId() != null) {
            int rows = appointmentMapper.markDoneIfArrived(form.getAppointmentId(), b.getId(), form.getCustomerId());
            if (rows == 0) {
                throw new BizException(ErrorCode.APPOINTMENT_STATE_INVALID, "来源预约状态已变化，请刷新后重新开单");
            }
        }

        log.info("结账完成 billNo={} total={} discount={} receivable={} by={}",
                b.getBillNo(), total, discount, receivable, user.getDisplayName());
        return b.getId();
    }

    // ---------------- 反结账（F5） ----------------

    @Transactional(rollbackFor = Exception.class)
    public void cancelSettle(Long billId, String reason, SessionUser user) {
        if (!user.isAdmin()) {
            throw new BizException(ErrorCode.NO_PERMISSION);
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "反结账原因必填");
        }
        Bill b = billMapper.findById(billId);
        if (b == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        if (!"SALE".equals(b.getBillType())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "售卡收款单不支持反结账");
        }
        if (!"SETTLED".equals(b.getStatus())) {
            throw new BizException(ErrorCode.BILL_ALREADY_SETTLED);
        }
        LocalDate today = BusinessDate.of(LocalDateTime.now());
        if (!today.equals(b.getBusinessDate())) {
            throw new BizException(ErrorCode.BILL_NOT_TODAY);
        }

        // 1. 先原子置为 CANCELLED：并发双击时只有一个事务能成功，避免重复回补库存/次数
        int rows = billMapper.cancel(billId, reason.trim(), LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ErrorCode.BILL_ALREADY_CANCELLED);
        }

        // 2. 还原核销次数：按流水 from_gift 精确还原（不能按当前开关重算）
        List<CardConsumeLog> logs = cardLogMapper.findConsumeByBill(billId);
        for (CardConsumeLog l : logs) {
            if (l.getFromGift()) {
                cardMapper.refundGift(l.getCardId(), l.getQty());
            } else {
                cardMapper.refundPaid(l.getCardId(), l.getQty());
            }
        }

        // 3. 回补库存 + 反向流水（按原出库台账逐批回补）
        List<BillItem> productItems = itemMapper.findProductsByBill(billId);
        for (BillItem i : productItems) {
            productService.refundForCancel(i.getRefId(), i.getQty(), billId, user.getDisplayName());
        }

        // 4. 提成冲销：写负数记录，不删除原记录（BR-21）
        commissionService.reverseForBill(b, user);

        log.info("反结账 billNo={} by={} reason={}", b.getBillNo(), user.getDisplayName(), reason);
    }

    // ---------------- 查询 ----------------

    public Bill detail(Long id) {
        Bill b = billMapper.findById(id);
        if (b == null) {
            throw new BizException(ErrorCode.BILL_NOT_FOUND);
        }
        b.setItems(itemMapper.findByBill(id));
        return b;
    }

    public Map<String, Object> search(LocalDate from, LocalDate to, Long customerId, Long petId,
                                      String keyword, String status, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        long offset = (long) (p - 1) * s;
        if (offset > Integer.MAX_VALUE) {
            offset = Integer.MAX_VALUE;
        }
        String kw = keyword == null ? "" : keyword.trim();
        long total = billMapper.countSearch(from, to, customerId, petId, kw, status);
        List<Bill> rows = total == 0
                ? new ArrayList<Bill>()
                : billMapper.search(from, to, customerId, petId, kw, status, (int) offset, s);
        Map<String, Object> m = new HashMap<>();
        m.put("pr", new com.petshop.common.PageResult<>(rows, total, p, s));
        return m;
    }

    /**
     * 插入主单；单号并发撞唯一索引时换号重试。
     * @return 非 null 表示命中幂等键，已存在同一请求的单据 id
     */
    private Long insertBillWithRetry(Bill b, LocalDate businessDate, String requestId) {
        for (int attempt = 0; ; attempt++) {
            b.setBillNo(cardService.nextBillNo("S", businessDate));
            try {
                billMapper.insert(b);
                return null;
            } catch (DuplicateKeyException e) {
                if (requestId != null) {
                    Bill exists = billMapper.findByRequestId(requestId);
                    if (exists != null) {
                        log.info("并发重复提交命中幂等键 requestId={}", requestId);
                        return exists.getId();
                    }
                }
                if (attempt >= 2) {
                    throw e;
                }
                log.warn("单号冲突，重新生成：{}", b.getBillNo());
            }
        }
    }

    private boolean isValidPayMethod(String pm) {
        return "CASH".equals(pm) || "WECHAT".equals(pm) || "ALIPAY".equals(pm)
                || "BANK_CARD".equals(pm) || "OTHER".equals(pm);
    }
}
