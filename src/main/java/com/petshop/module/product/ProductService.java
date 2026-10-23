package com.petshop.module.product;

import com.petshop.common.BizException;
import com.petshop.common.ErrorCode;
import com.petshop.common.Money;
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

    public ProductService(ProductMapper productMapper, ProductLedgerMapper ledgerMapper, ConfigService configService) {
        this.productMapper = productMapper;
        this.ledgerMapper = ledgerMapper;
        this.configService = configService;
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
        if (name == null || name.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "商品名称不能为空");
        }
        Product p = id == null ? new Product() : findById(id);
        p.setName(name.trim());
        p.setBarcode(barcode == null || barcode.trim().isEmpty() ? null : barcode.trim());
        p.setSalePriceCent(Money.toCent(salePrice));
        p.setCostPriceCent(Money.toCent(costPrice));
        p.setSafetyQty(Math.max(0, safetyQty));
        p.setExpireOn(expireOn == null || expireOn.trim().isEmpty() ? null : LocalDate.parse(expireOn.trim()));
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
     * 手工出入库（采购入库/退货入库/盘点调整）。
     * 库存变更与流水同一事务（BR-04 / BR-07）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjust(Long productId, String bizType, int qty, String remark, String operator) {
        Product p = findById(productId);
        int change;
        switch (bizType) {
            case "IN":
            case "RETURN":
                change = Math.abs(qty);
                break;
            case "ADJUST":
                // 盘点调整：qty 为正负皆可的修正值
                change = qty;
                break;
            default:
                throw new BizException(ErrorCode.PARAM_INVALID, "不支持的出入库类型");
        }
        if (change == 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "变动数量不能为 0");
        }
        int before = p.getStockQty();
        int after;
        if (change > 0) {
            productMapper.addStock(productId, change);
            after = before + change;
        } else {
            int rows = productMapper.deductStock(productId, -change);
            if (rows == 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
            }
            after = before + change;
        }
        writeLedger(productId, bizType, change, before, after, "MANUAL", null, operator,
                remark == null || remark.trim().isEmpty() ? "手工调整" : remark.trim());
    }

    /** 结账扣库存（事务内调用）：不足抛 STOCK_NOT_ENOUGH */
    public void deductForSale(Long productId, int qty, Long billId, String operator) {
        Product p = productMapper.findById(productId);
        if (p == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "商品不存在");
        }
        int before = p.getStockQty();
        int rows = productMapper.deductStock(productId, qty);
        if (rows == 0) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        writeLedger(productId, "SALE", -qty, before, before - qty, "BILL", billId, operator, null);
    }

    /** 反结账回补库存（事务内调用） */
    public void refundForCancel(Long productId, int qty, Long billId, String operator) {
        Product p = productMapper.findById(productId);
        int before = p.getStockQty();
        productMapper.addStock(productId, qty);
        writeLedger(productId, "CANCEL", qty, before, before + qty, "BILL", billId, operator, "反结账回滚");
    }

    private void writeLedger(Long productId, String bizType, int qtyChange, int before, int after,
                             String refType, Long refId, String operator, String remark) {
        ProductLedger l = new ProductLedger();
        l.setProductId(productId);
        l.setBizType(bizType);
        l.setQtyChange(qtyChange);
        l.setBeforeQty(before);
        l.setAfterQty(after);
        l.setRefType(refType);
        l.setRefId(refId);
        l.setOperator(operator);
        l.setRemark(remark);
        l.setCreatedAt(LocalDateTime.now());
        ledgerMapper.insert(l);
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
