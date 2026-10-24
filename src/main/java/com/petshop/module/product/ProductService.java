package com.petshop.module.product;

import com.petshop.common.BizException;
import com.petshop.common.ErrorCode;
import com.petshop.common.Money;
import com.petshop.module.inventory.InventoryService;
import com.petshop.module.system.ConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductLedgerMapper ledgerMapper;
    private final ConfigService configService;
    private final InventoryService inventoryService;

    public ProductService(ProductMapper productMapper, ProductLedgerMapper ledgerMapper,
                          ConfigService configService, InventoryService inventoryService) {
        this.productMapper = productMapper;
        this.ledgerMapper = ledgerMapper;
        this.configService = configService;
        this.inventoryService = inventoryService;
    }

    public List<Product> findAll() {
        return productMapper.findAll();
    }

    public List<Product> findActive() {
        return productMapper.findActive();
    }

    public Product findById(Long id) {
        Product p = productMapper.findById(id);
        if (p == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "商品不存在");
        }
        return p;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long save(Long id, String name, String barcode, String salePrice, String costPrice,
                     int safetyQty, String expireOn) {
        return save(id, name, barcode, salePrice, costPrice, safetyQty, expireOn, null, null, null, null);
    }

    /** v1.2：商品档案扩展（规格/分类/保质期/默认供应商） */
    @Transactional(rollbackFor = Exception.class)
    public Long save(Long id, String name, String barcode, String salePrice, String costPrice,
                     int safetyQty, String expireOn, String spec, String category,
                     Integer shelfLifeDays, Long defaultSupplierId) {
        if (name == null || name.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "商品名称不能为空");
        }
        Product p = id == null ? new Product() : findById(id);
        p.setName(name.trim());
        p.setBarcode(barcode == null || barcode.trim().isEmpty() ? null : barcode.trim());
        int saleCent = Money.toCent(salePrice);
        int costCent = Money.toCent(costPrice);
        if (saleCent < 0 || costCent < 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "售价与成本价不能为负数");
        }
        p.setSalePriceCent(saleCent);
        p.setCostPriceCent(costCent);
        p.setSafetyQty(Math.max(0, safetyQty));
        p.setExpireOn(expireOn == null || expireOn.trim().isEmpty() ? null : LocalDate.parse(expireOn.trim()));
        p.setSpec(spec == null || spec.trim().isEmpty() ? null : spec.trim());
        p.setCategory(category == null || category.trim().isEmpty() ? "OTHER" : category.trim());
        p.setShelfLifeDays(shelfLifeDays == null || shelfLifeDays <= 0 ? null : shelfLifeDays);
        p.setDefaultSupplierId(defaultSupplierId);
        p.setIsActive(true);
        if (id == null) {
            p.setStockQty(0);
            p.setCreatedAt(LocalDateTime.now());
            productMapper.insert(p);
        } else {
            productMapper.update(p);
        }
        return p.getId();
    }

    /**
     * 手工出入库：v1.2 起交给 InventoryService 按批次记账（BR-16）。
     * 保留此门面方法，避免 v1.1 的调用方与页面失效。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjust(Long productId, String bizType, int qty, String remark, String operator) {
        findById(productId);
        inventoryService.adjust(productId, bizType, qty, remark, operator);
    }

    /** 结账扣库存（FEFO）：返回整行来自单一批次时的批次 id，跨批次返回 null */
    public Long deductForSale(Long productId, int qty, Long billId, String operator) {
        return inventoryService.deductForSale(productId, qty, billId, operator);
    }

    /** 反结账回补库存（按原出库台账逐批回补） */
    public void refundForCancel(Long productId, int qty, Long billId, String operator) {
        inventoryService.refundForCancel(productId, qty, billId, operator);
    }

    public List<Product> expiring() {
        return productMapper.findExpiring(LocalDate.now(), 30);
    }

    public List<Product> lowStock() {
        return productMapper.findLowStock(configService.lowStockWarn());
    }

    @Transactional(rollbackFor = Exception.class)
    public void setActive(Long id, boolean active) {
        findById(id);
        productMapper.setActive(id, active);
    }

    public List<ProductLedger> ledgerOf(Long productId) {
        return ledgerMapper.findByProduct(productId);
    }
}
