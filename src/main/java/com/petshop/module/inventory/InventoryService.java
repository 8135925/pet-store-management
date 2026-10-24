package com.petshop.module.inventory;

import com.petshop.common.BizException;
import com.petshop.common.ErrorCode;
import com.petshop.common.Money;
import com.petshop.module.product.Product;
import com.petshop.module.product.ProductLedger;
import com.petshop.module.product.ProductLedgerMapper;
import com.petshop.module.product.ProductMapper;
import com.petshop.module.system.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购 / 批次 / 盘点 的全部库存变动。
 *
 * <p>不变量（与 schema 自检一致）：</p>
 * <ol>
 *   <li>{@code product.stock_qty == SUM(product_ledger.qty_change)}</li>
 *   <li>{@code product.stock_qty == SUM(product_batch.qty_left)}（批次合计）</li>
 *   <li>同一事务内，台账 before/after 连续递推</li>
 * </ol>
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_PAGE_SIZE = 100;

    private final InventoryMapper mapper;
    private final ProductMapper productMapper;
    private final ProductLedgerMapper ledgerMapper;
    private final ConfigService configService;

    public InventoryService(InventoryMapper mapper, ProductMapper productMapper,
                            ProductLedgerMapper ledgerMapper, ConfigService configService) {
        this.mapper = mapper;
        this.productMapper = productMapper;
        this.ledgerMapper = ledgerMapper;
        this.configService = configService;
    }

    // ==================== 供应商 ====================

    public List<Supplier> suppliers() {
        return mapper.findAllSuppliers();
    }

    public List<Supplier> activeSuppliers() {
        return mapper.findActiveSuppliers();
    }

    public Supplier supplier(Long id) {
        Supplier s = mapper.findSupplierById(id);
        if (s == null) {
            throw new BizException(ErrorCode.SUPPLIER_NOT_FOUND);
        }
        return s;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveSupplier(Long id, String name, String contact, String phone,
                             String address, String mainCategory, String remark) {
        if (name == null || name.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "供应商名称不能为空");
        }
        String n = name.trim();
        if (mapper.countSupplierByName(n, id) > 0) {
            throw new BizException(ErrorCode.SUPPLIER_EXISTS);
        }
        if (id == null) {
            Supplier s = new Supplier();
            s.setName(n);
            s.setContact(contact);
            s.setPhone(phone);
            s.setAddress(address);
            s.setMainCategory(mainCategory);
            s.setRemark(remark);
            s.setIsActive(true);
            s.setCreatedAt(LocalDateTime.now());
            mapper.insertSupplier(s);
            return s.getId();
        }
        Supplier s = supplier(id);
        if (!n.equals(s.getName()) && mapper.countSupplierByName(n, id) > 0) {
            throw new BizException(ErrorCode.SUPPLIER_EXISTS);
        }
        s.setName(n);
        s.setContact(contact);
        s.setPhone(phone);
        s.setAddress(address);
        s.setMainCategory(mainCategory);
        s.setRemark(remark);
        mapper.updateSupplier(s);
        return id;
    }

    public void toggleSupplier(Long id, boolean active) {
        supplier(id);
        mapper.setSupplierActive(id, active);
    }

    // ==================== 采购单 ====================

    public PurchaseOrder purchase(Long id) {
        PurchaseOrder p = mapper.findPurchaseById(id);
        if (p == null) {
            throw new BizException(ErrorCode.PURCHASE_NOT_FOUND);
        }
        p.setItems(mapper.findPurchaseItems(id));
        return p;
    }

    /** 新建或编辑草稿（编辑时整单明细先删后插） */
    @Transactional(rollbackFor = Exception.class)
    public Long savePurchase(Long id, Long supplierId, String remark,
                             List<PurchaseItem> items, String operator) {
        if (supplierId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "必须选择供应商");
        }
        if (items == null || items.isEmpty()) {
            throw new BizException(ErrorCode.PURCHASE_EMPTY);
        }
        if (items.size() > 200) {
            throw new BizException(ErrorCode.PARAM_INVALID, "单次采购明细不能超过 200 行");
        }
        Supplier sup = supplier(supplierId);
        if (!sup.getIsActive()) {
            throw new BizException(ErrorCode.SUPPLIER_NOT_FOUND);
        }
        int total = 0;
        for (PurchaseItem it : items) {
            if (it.getProductId() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "采购明细必须选择商品");
            }
            if (it.getQty() <= 0 || it.getQty() > 1_000_000) {
                throw new BizException(ErrorCode.PARAM_INVALID, "采购数量不正确");
            }
            if (it.getUnitCostCent() < 0) {
                throw new BizException(ErrorCode.PARAM_INVALID, "进价不能为负数");
            }
            long amount = (long) it.getQty() * it.getUnitCostCent();
            if (amount > Integer.MAX_VALUE) {
                throw new BizException(ErrorCode.PARAM_INVALID, "采购金额超出上限");
            }
            it.setAmountCent((int) amount);
            total += it.getAmountCent();
            if (total < 0) {
                throw new BizException(ErrorCode.PARAM_INVALID, "采购金额超出上限");
            }
        }

        Long purchaseId;
        if (id == null) {
            PurchaseOrder p = new PurchaseOrder();
            p.setOrderNo(nextPurchaseNo());
            p.setSupplierId(supplierId);
            p.setStatus("DRAFT");
            p.setTotalCostCent(total);
            p.setItemCount(items.size());
            p.setRemark(remark);
            p.setCreatedAt(LocalDateTime.now());
            p.setCreatedBy(operator);
            mapper.insertPurchase(p);
            purchaseId = p.getId();
        } else {
            PurchaseOrder exist = purchase(id);
            if (!"DRAFT".equals(exist.getStatus())) {
                throw new BizException(ErrorCode.PURCHASE_ALREADY_RECEIVED, "已入库的采购单不能修改");
            }
            // 草稿编辑：明细整单替换（草稿无外部引用，安全）
            mapper.deletePurchaseItems(id);
            mapper.updatePurchaseDraft(id, supplierId, remark, total, items.size());
            purchaseId = id;
        }
        for (PurchaseItem it : items) {
            it.setPurchaseId(purchaseId);
            mapper.insertPurchaseItem(it);
        }
        return purchaseId;
    }

    /** 确认入库：批次 + 入库台账 + 总库存，单事务 */
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long purchaseId, String operator) {
        PurchaseOrder po = mapper.findPurchaseById(purchaseId);
        if (po == null) {
            throw new BizException(ErrorCode.PURCHASE_NOT_FOUND);
        }
        // 先原子占位，重复点确认不会重复入库
        int rows = mapper.receivePurchase(purchaseId, operator);
        if (rows == 0) {
            throw new BizException(ErrorCode.PURCHASE_ALREADY_RECEIVED);
        }
        List<PurchaseItem> items = mapper.findPurchaseItems(purchaseId);
        if (items.isEmpty()) {
            throw new BizException(ErrorCode.PURCHASE_EMPTY);
        }
        Map<Long, Integer> running = new HashMap<Long, Integer>();
        for (PurchaseItem it : items) {
            Product p = productMapper.findById(it.getProductId());
            if (p == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "商品不存在：id=" + it.getProductId());
            }
            ensureOpeningBatch(p);
            int before = running.containsKey(p.getId()) ? running.get(p.getId()) : p.getStockQty();
            productMapper.addStock(p.getId(), it.getQty());

            ProductBatch b = new ProductBatch();
            b.setProductId(p.getId());
            b.setPurchaseItemId(it.getId());
            b.setBatchNo(it.getBatchNo());
            b.setExpireOn(resolveExpire(it.getExpireOn(), p));
            b.setQtyIn(it.getQty());
            b.setQtyLeft(it.getQty());
            b.setUnitCostCent(it.getUnitCostCent());
            b.setSource("PURCHASE");
            b.setStatus("NORMAL");
            b.setReceivedAt(LocalDateTime.now());
            b.setCreatedAt(LocalDateTime.now());
            mapper.insertBatch(b);

            int after = before + it.getQty();
            writeLedger(p.getId(), "PURCHASE", it.getQty(), before, after, it.getUnitCostCent(),
                    b.getId(), "PURCHASE", purchaseId, operator, "采购入库 " + po.getOrderNo());
            running.put(p.getId(), after);
        }
        log.info("采购单 {} 入库完成，{} 行", po.getOrderNo(), items.size());
    }

    public void cancelPurchase(Long id, String operator) {
        PurchaseOrder p = mapper.findPurchaseById(id);
        if (p == null) {
            throw new BizException(ErrorCode.PURCHASE_NOT_FOUND);
        }
        int rows = mapper.cancelPurchase(id);
        if (rows == 0) {
            throw new BizException(ErrorCode.PURCHASE_ALREADY_RECEIVED, "只有草稿可以作废");
        }
    }

    public Map<String, Object> searchPurchases(String keyword, String status,
                                               String from, String to, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        long offset = (long) (p - 1) * s;
        String kw = keyword == null ? "" : keyword.trim();
        long total = mapper.countPurchases(kw, status, from, to);
        List<PurchaseOrder> rows = total == 0 ? new ArrayList<PurchaseOrder>()
                : mapper.searchPurchases(kw, status, from, to, (int) Math.min(offset, Integer.MAX_VALUE), s);
        Map<String, Object> r = new HashMap<String, Object>();
        r.put("total", total);
        r.put("page", p);
        r.put("pages", (total + s - 1) / s);
        r.put("rows", rows);
        return r;
    }

    // ==================== 批次 ====================

    public List<ProductBatch> batches(Long productId, String status, Integer expiringDays) {
        return mapper.findBatches(productId, status, expiringDays);
    }

    public ProductBatch batch(Long id) {
        ProductBatch b = mapper.findBatchById(id);
        if (b == null) {
            throw new BizException(ErrorCode.BATCH_NOT_FOUND);
        }
        return b;
    }

    /** 批次报损（过期/破损）：扣批次 + 扣总库存 + VOID 台账 */
    @Transactional(rollbackFor = Exception.class)
    public void voidBatch(Long batchId, int qty, String reason, String operator) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "报损原因必填");
        }
        ProductBatch b = batch(batchId);
        if (qty <= 0 || qty > b.getQtyLeft()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "报损数量必须在 1 到批次剩余之间");
        }
        Product p = productMapper.findById(b.getProductId());
        if (p == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "商品不存在");
        }
        if (mapper.deductBatch(batchId, qty) == 0) {
            throw new BizException(ErrorCode.BATCH_STOCK_NOT_ENOUGH);
        }
        if (b.getQtyLeft() - qty == 0) {
            mapper.setBatchStatus(batchId, "VOID");
        }
        if (productMapper.deductStock(p.getId(), qty) == 0) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        int after = currentStock(p.getId(), p.getStockQty() - qty);
        writeLedger(p.getId(), "VOID", -qty, after + qty, after, b.getUnitCostCent(),
                batchId, "MANUAL", null, operator, "批次报损：" + reason.trim());
    }

    // ==================== 销售出库 / 反结账 / 手工调整 ====================

    /**
     * 销售出库（FEFO，跨批次拆台账）。
     *
     * @return 整行来自单一批次时返回该批次 id，跨批次返回 null
     */
    public Long deductForSale(Long productId, int qty, Long billId, String operator) {
        if (qty <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "销售数量必须大于 0");
        }
        Product p = productMapper.findById(productId);
        if (p == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "商品不存在");
        }
        ensureOpeningBatch(p);
        if (productMapper.deductStock(productId, qty) == 0) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        int after = currentStock(productId, p.getStockQty() - qty);
        int before = after + qty;

        List<ProductBatch> available = mapper.findAvailableBatches(productId, LocalDate.now());
        List<BatchAllocator.BatchSlice> slices = new ArrayList<BatchAllocator.BatchSlice>();
        for (ProductBatch b : available) {
            slices.add(new BatchAllocator.BatchSlice(b.getId(), b.getQtyLeft(), b.getUnitCostCent()));
        }
        BatchAllocator.Result result = BatchAllocator.allocate(slices, qty);
        if (!result.isComplete()) {
            // 批次不足（常见于过期批次占库存 / 历史数据不一致），整单回滚，提示先盘点
            throw new BizException(ErrorCode.BATCH_STOCK_NOT_ENOUGH);
        }
        int ledgerBefore = before;
        Long singleBatch = result.getAllocations().size() == 1
                ? result.getAllocations().get(0).getBatchId() : null;
        for (BatchAllocator.Allocation a : result.getAllocations()) {
            if (mapper.deductBatch(a.getBatchId(), a.getQty()) == 0) {
                throw new BizException(ErrorCode.BATCH_STOCK_NOT_ENOUGH);
            }
            int change = -a.getQty();
            writeLedger(productId, "SALE", change, ledgerBefore, ledgerBefore + change,
                    a.getUnitCostCent(), a.getBatchId(), "BILL", billId, operator, null);
            ledgerBefore += change;
        }
        return singleBatch;
    }

    /** 反结账：按原出库台账逐批回补，保证批次合计等于总库存 */
    public void refundForCancel(Long productId, int qty, Long billId, String operator) {
        if (qty <= 0) {
            return;
        }
        Product p = productMapper.findById(productId);
        if (p == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "商品不存在");
        }
        productMapper.addStock(productId, qty);
        int after = currentStock(productId, p.getStockQty() + qty);
        int before = after - qty;
        int ledgerBefore = before;
        int remaining = qty;

        List<ProductLedger> sales = ledgerMapper.findSaleByBillAndProduct(billId, productId);
        for (ProductLedger l : sales) {
            if (remaining == 0) {
                break;
            }
            int sold = -l.getQtyChange();
            if (sold <= 0) {
                continue;
            }
            int take = Math.min(remaining, sold);
            boolean restored = l.getBatchId() != null && mapper.addBatchQty(l.getBatchId(), take) > 0;
            if (!restored) {
                createBatch(productId, take, l.getUnitCostCent(), "RETURN", null,
                        "反结账回补", LocalDate.now());
            }
            writeLedger(productId, "CANCEL", take, ledgerBefore, ledgerBefore + take,
                    l.getUnitCostCent(), l.getBatchId(), "BILL", billId, operator, "反结账回滚");
            ledgerBefore += take;
            remaining -= take;
        }
        if (remaining > 0) {
            // 历史数据：v1.1 台账没有批次，补一个 RETURN 批次承接
            createBatch(productId, remaining, p.getCostPriceCent(), "RETURN", null,
                    "反结账回补", LocalDate.now());
            writeLedger(productId, "CANCEL", remaining, ledgerBefore, ledgerBefore + remaining,
                    p.getCostPriceCent(), null, "BILL", billId, operator, "反结账回滚");
        }
    }

    /** 手工出入库（保留 v1.1 入口，改为按批次记账） */
    @Transactional(rollbackFor = Exception.class)
    public void adjust(Long productId, String bizType, int qty, String remark, String operator) {
        Product p = productMapper.findById(productId);
        if (p == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "商品不存在");
        }
        int change;
        String source;
        switch (bizType) {
            case "IN":
            case "RETURN":
                change = Math.abs(qty);
                source = "PURCHASE";
                break;
            case "ADJUST":
                change = qty;
                source = change > 0 ? "STOCK_TAKE" : "PURCHASE";
                break;
            case "VOID":
                change = -Math.abs(qty);
                source = "PURCHASE";
                break;
            default:
                throw new BizException(ErrorCode.PARAM_INVALID, "不支持的出入库类型");
        }
        if (change == 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "变动数量不能为 0");
        }
        if (Math.abs((long) change) > 1_000_000L) {
            throw new BizException(ErrorCode.PARAM_INVALID, "单次库存变动数量过大");
        }
        ensureOpeningBatch(p);
        String note = remark == null || remark.trim().isEmpty() ? "手工调整" : remark.trim();

        if (change > 0) {
            productMapper.addStock(productId, change);
            int after = currentStock(productId, p.getStockQty() + change);
            createBatch(productId, change, p.getCostPriceCent(), source, null, note, null);
            writeLedger(productId, bizType, change, after - change, after,
                    p.getCostPriceCent(), null, "MANUAL", null, operator, note);
        } else {
            int need = -change;
            if (productMapper.deductStock(productId, need) == 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
            }
            int after = currentStock(productId, p.getStockQty() - need);
            int ledgerBefore = after + need;
            List<ProductBatch> available = mapper.findAvailableBatches(productId, LocalDate.now());
            List<BatchAllocator.BatchSlice> slices = new ArrayList<BatchAllocator.BatchSlice>();
            for (ProductBatch b : available) {
                slices.add(new BatchAllocator.BatchSlice(b.getId(), b.getQtyLeft(), b.getUnitCostCent()));
            }
            BatchAllocator.Result result = BatchAllocator.allocate(slices, need);
            int remaining = need;
            for (BatchAllocator.Allocation a : result.getAllocations()) {
                if (mapper.deductBatch(a.getBatchId(), a.getQty()) == 0) {
                    throw new BizException(ErrorCode.BATCH_STOCK_NOT_ENOUGH);
                }
                writeLedger(productId, bizType, -a.getQty(), ledgerBefore, ledgerBefore - a.getQty(),
                        a.getUnitCostCent(), a.getBatchId(), "MANUAL", null, operator, note);
                ledgerBefore -= a.getQty();
                remaining -= a.getQty();
            }
            if (remaining > 0) {
                // 批次不足部分允许无批次台账（手工调整常见于历史数据修账），体检会提示
                writeLedger(productId, bizType, -remaining, ledgerBefore, ledgerBefore - remaining,
                        p.getCostPriceCent(), null, "MANUAL", null, operator, note + "（无批次）");
            }
        }
    }

    // ==================== 盘点 ====================

    @Transactional(rollbackFor = Exception.class)
    public Long createStockTake(String scope, String scopeValue, String operator, String remark) {
        String sc = scope == null || scope.trim().isEmpty() ? "ALL" : scope.trim();
        if (!"ALL".equals(sc) && !"CATEGORY".equals(sc) && !"SELECTED".equals(sc)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "盘点范围不正确");
        }
        if ("CATEGORY".equals(sc) && (scopeValue == null || scopeValue.trim().isEmpty())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请选择盘点分类");
        }
        if ("SELECTED".equals(sc) && (scopeValue == null || scopeValue.trim().isEmpty())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请选择盘点商品");
        }
        List<Product> products = mapper.findTakeScopeProducts(sc, scopeValue);
        if (products.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "该范围内没有商品");
        }
        for (Product p : products) {
            if (mapper.countOpenTakeByProduct(p.getId()) > 0) {
                throw new BizException(ErrorCode.STOCK_TAKE_DUPLICATE,
                        "商品「" + p.getName() + "」已存在未确认的盘点单");
            }
            ensureOpeningBatch(p);
        }

        StockTake t = new StockTake();
        t.setTakeNo(nextTakeNo());
        t.setScope(sc);
        t.setScopeValue(scopeValue);
        t.setStatus("COUNTING");
        t.setItemCount(products.size());
        t.setStartedAt(LocalDateTime.now());
        t.setOperator(operator);
        t.setRemark(remark);
        t.setCreatedAt(LocalDateTime.now());
        mapper.insertStockTake(t);

        for (Product p : products) {
            StockTakeItem item = new StockTakeItem();
            item.setTakeId(t.getId());
            item.setProductId(p.getId());
            item.setSystemQty((int) mapper.sumQtyLeft(p.getId()));
            item.setCostCent(p.getCostPriceCent());
            mapper.insertStockTakeItem(item);
        }
        log.info("盘点单 {} 创建，{} 个商品", t.getTakeNo(), products.size());
        return t.getId();
    }

    public StockTake stockTake(Long id) {
        StockTake t = mapper.findStockTakeById(id);
        if (t == null) {
            throw new BizException(ErrorCode.STOCK_TAKE_NOT_FOUND);
        }
        t.setItems(mapper.findStockTakeItems(id));
        return t;
    }

    public Map<String, Object> searchStockTakes(String status, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        long offset = (long) (p - 1) * s;
        long total = mapper.countStockTakes(status);
        List<StockTake> rows = total == 0 ? new ArrayList<StockTake>()
                : mapper.searchStockTakes(status, (int) Math.min(offset, Integer.MAX_VALUE), s);
        Map<String, Object> r = new HashMap<String, Object>();
        r.put("total", total);
        r.put("page", p);
        r.put("pages", (total + s - 1) / s);
        r.put("rows", rows);
        return r;
    }

    /** 录入实盘数（itemId -> 数量字符串，空表示未盘） */
    @Transactional(rollbackFor = Exception.class)
    public void saveCounts(Long takeId, Map<Long, String> countedByItem,
                           Map<Long, String> reasonByItem) {
        StockTake t = stockTake(takeId);
        if (!"COUNTING".equals(t.getStatus())) {
            throw new BizException(ErrorCode.STOCK_TAKE_ALREADY_CONFIRMED);
        }
        int counted = 0;
        for (StockTakeItem item : t.getItems()) {
            String raw = countedByItem == null ? null : countedByItem.get(item.getId());
            Integer qty = null;
            if (raw != null && !raw.trim().isEmpty()) {
                try {
                    qty = Integer.parseInt(raw.trim());
                } catch (NumberFormatException e) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "实盘数量必须是整数");
                }
                if (qty < 0 || qty > 1_000_000) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "实盘数量不正确");
                }
                counted++;
            }
            String reason = reasonByItem == null ? null : reasonByItem.get(item.getId());
            if (qty != null && qty != item.getSystemQty()
                    && (reason == null || reason.trim().isEmpty())) {
                throw new BizException(ErrorCode.PARAM_INVALID,
                        "「" + item.getProductName() + "」有差异，必须选择原因");
            }
            mapper.updateTakeItemCounted(item.getId(), qty,
                    reason == null ? null : reason.trim());
        }
        if (counted == 0) {
            throw new BizException(ErrorCode.STOCK_TAKE_EMPTY);
        }
    }

    /** 确认盘点：盘亏按 FEFO 扣批次、盘盈建 STOCK_TAKE 批次，全部写 TAKE 台账 */
    @Transactional(rollbackFor = Exception.class)
    public void confirmStockTake(Long takeId, String operator) {
        StockTake t = stockTake(takeId);
        if (!"COUNTING".equals(t.getStatus())) {
            throw new BizException(ErrorCode.STOCK_TAKE_ALREADY_CONFIRMED);
        }
        int diffCount = 0;
        int diffAmount = 0;
        int countedAny = 0;

        for (StockTakeItem item : t.getItems()) {
            if (item.getCountedQty() == null) {
                continue;
            }
            countedAny++;
            int diff = item.getCountedQty() - item.getSystemQty();
            mapper.updateTakeItemDiff(item.getId(), diff);
            if (diff == 0) {
                continue;
            }
            Product p = productMapper.findById(item.getProductId());
            if (p == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "商品不存在");
            }
            applyTakeDiff(t, p, item, diff, operator);
            diffCount++;
            diffAmount += diff * item.getCostCent();
        }
        if (countedAny == 0) {
            throw new BizException(ErrorCode.STOCK_TAKE_EMPTY);
        }
        int rows = mapper.confirmStockTake(takeId, diffCount, diffAmount, operator,
                LocalDateTime.now().format(TS));
        if (rows == 0) {
            throw new BizException(ErrorCode.STOCK_TAKE_ALREADY_CONFIRMED);
        }
        log.info("盘点单 {} 确认：{} 项差异，差异金额 {} 分", t.getTakeNo(), diffCount, diffAmount);
    }

    private void applyTakeDiff(StockTake t, Product p, StockTakeItem item, int diff, String operator) {
        if (diff > 0) {
            productMapper.addStock(p.getId(), diff);
            int after = currentStock(p.getId(), p.getStockQty() + diff);
            createBatch(p.getId(), diff, item.getCostCent(), "STOCK_TAKE", null,
                    "盘点盘盈 " + t.getTakeNo(), LocalDate.now());
            writeLedger(p.getId(), "TAKE", diff, after - diff, after, item.getCostCent(),
                    null, "STOCK_TAKE", t.getId(), operator, "盘点盘盈：" + item.getReason());
            return;
        }
        int need = -diff;
        if (productMapper.deductStock(p.getId(), need) == 0) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        int after = currentStock(p.getId(), p.getStockQty() - need);
        int ledgerBefore = after + need;
        List<ProductBatch> available = mapper.findAvailableBatches(p.getId(), LocalDate.now());
        List<BatchAllocator.BatchSlice> slices = new ArrayList<BatchAllocator.BatchSlice>();
        for (ProductBatch b : available) {
            slices.add(new BatchAllocator.BatchSlice(b.getId(), b.getQtyLeft(), b.getUnitCostCent()));
        }
        BatchAllocator.Result result = BatchAllocator.allocate(slices, need);
        int remaining = need;
        for (BatchAllocator.Allocation a : result.getAllocations()) {
            if (mapper.deductBatch(a.getBatchId(), a.getQty()) == 0) {
                throw new BizException(ErrorCode.BATCH_STOCK_NOT_ENOUGH);
            }
            writeLedger(p.getId(), "TAKE", -a.getQty(), ledgerBefore, ledgerBefore - a.getQty(),
                    a.getUnitCostCent(), a.getBatchId(), "STOCK_TAKE", t.getId(), operator,
                    "盘点盘亏：" + item.getReason());
            ledgerBefore -= a.getQty();
            remaining -= a.getQty();
        }
        if (remaining > 0) {
            writeLedger(p.getId(), "TAKE", -remaining, ledgerBefore, ledgerBefore - remaining,
                    item.getCostCent(), null, "STOCK_TAKE", t.getId(), operator,
                    "盘点盘亏（无批次）：" + item.getReason());
        }
    }

    public void cancelStockTake(Long id) {
        if (mapper.cancelStockTake(id) == 0) {
            throw new BizException(ErrorCode.STOCK_TAKE_ALREADY_CONFIRMED, "只有盘点中的单据可以作废");
        }
    }

    public List<StockTake> recentStockTakes(int limit) {
        return mapper.findRecentStockTakes(limit);
    }

    // ==================== 内部 ====================

    /**
     * 期初批次：v1.1 老数据只有 stock_qty，没有批次。
     * 首次发生库存变动（或盘点/建档）时为商品补一条 OPENING 批次，避免"有库存却卖不出去"。
     */
    private void ensureOpeningBatch(Product p) {
        if (p.getStockQty() <= 0 || mapper.countBatches(p.getId()) > 0) {
            return;
        }
        createBatch(p.getId(), p.getStockQty(), p.getCostPriceCent(), "OPENING", null,
                "期初库存", p.getExpireOn());
        log.info("商品 {} 补建期初批次 {} 件", p.getName(), p.getStockQty());
    }

    private void createBatch(Long productId, int qty, int unitCostCent, String source,
                             String batchNo, String remark, LocalDate expireOn) {
        ProductBatch b = new ProductBatch();
        b.setProductId(productId);
        b.setBatchNo(batchNo);
        b.setExpireOn(expireOn);
        b.setQtyIn(qty);
        b.setQtyLeft(qty);
        b.setUnitCostCent(Math.max(0, unitCostCent));
        b.setSource(source);
        b.setStatus("NORMAL");
        b.setReceivedAt(LocalDateTime.now());
        b.setCreatedAt(LocalDateTime.now());
        mapper.insertBatch(b);
    }

    private LocalDate resolveExpire(String raw, Product p) {
        if (raw != null && !raw.trim().isEmpty()) {
            try {
                return LocalDate.parse(raw.trim());
            } catch (Exception e) {
                throw new BizException(ErrorCode.PARAM_INVALID, "有效期格式不正确：" + raw);
            }
        }
        if (p.getShelfLifeDays() != null && p.getShelfLifeDays() > 0) {
            return LocalDate.now().plusDays(p.getShelfLifeDays());
        }
        return null;
    }

    private int currentStock(Long productId, int fallback) {
        Product fresh = productMapper.findById(productId);
        return fresh == null ? fallback : fresh.getStockQty();
    }

    private void writeLedger(Long productId, String bizType, int change, int before, int after,
                             int unitCostCent, Long batchId, String refType, Long refId,
                             String operator, String remark) {
        ProductLedger l = new ProductLedger();
        l.setProductId(productId);
        l.setBizType(bizType);
        l.setQtyChange(change);
        l.setBeforeQty(before);
        l.setAfterQty(after);
        l.setUnitCostCent(Math.max(0, unitCostCent));
        l.setBatchId(batchId);
        l.setRefType(refType);
        l.setRefId(refId);
        l.setOperator(operator);
        l.setRemark(remark);
        l.setCreatedAt(LocalDateTime.now());
        ledgerMapper.insert(l);
    }

    private String nextPurchaseNo() {
        String prefix = "P" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = mapper.countPurchaseByPrefix(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }

    private String nextTakeNo() {
        String prefix = "T" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = mapper.countTakeByPrefix(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }

}
