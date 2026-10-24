package com.petshop.module.inventory;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface InventoryMapper {

    // ---------------- 供应商 ----------------

    int insertSupplier(Supplier s);

    int updateSupplier(Supplier s);

    int setSupplierActive(@Param("id") Long id, @Param("active") boolean active);

    Supplier findSupplierById(@Param("id") Long id);

    List<Supplier> findAllSuppliers();

    List<Supplier> findActiveSuppliers();

    int countSupplierByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    // ---------------- 采购单 ----------------

    int insertPurchase(PurchaseOrder p);

    int insertPurchaseItem(PurchaseItem i);

    int updatePurchaseTotal(@Param("id") Long id,
                            @Param("totalCostCent") int totalCostCent,
                            @Param("itemCount") int itemCount);

    /** 草稿编辑：供应商、备注、金额一起更新 */
    int updatePurchaseDraft(@Param("id") Long id,
                            @Param("supplierId") Long supplierId,
                            @Param("remark") String remark,
                            @Param("totalCostCent") int totalCostCent,
                            @Param("itemCount") int itemCount);

    PurchaseOrder findPurchaseById(@Param("id") Long id);

    List<PurchaseItem> findPurchaseItems(@Param("purchaseId") Long purchaseId);

    List<PurchaseOrder> searchPurchases(@Param("keyword") String keyword,
                                        @Param("status") String status,
                                        @Param("from") String from,
                                        @Param("to") String to,
                                        @Param("offset") int offset,
                                        @Param("size") int size);

    long countPurchases(@Param("keyword") String keyword,
                        @Param("status") String status,
                        @Param("from") String from,
                        @Param("to") String to);

    int receivePurchase(@Param("id") Long id, @Param("operator") String operator);

    int cancelPurchase(@Param("id") Long id);

    int deletePurchaseItems(@Param("purchaseId") Long purchaseId);

    long countPurchaseByPrefix(@Param("prefix") String prefix);

    // ---------------- 批次 ----------------

    int insertBatch(ProductBatch b);

    ProductBatch findBatchById(@Param("id") Long id);

    /** FEFO 可用批次：未过期、有剩余、状态正常 */
    List<ProductBatch> findAvailableBatches(@Param("productId") Long productId,
                                            @Param("today") LocalDate today);

    List<ProductBatch> findBatches(@Param("productId") Long productId,
                                   @Param("status") String status,
                                   @Param("expiringDays") Integer expiringDays);

    int countBatches(@Param("productId") Long productId);

    long sumQtyLeft(@Param("productId") Long productId);

    int deductBatch(@Param("id") Long id, @Param("qty") int qty);

    int addBatchQty(@Param("id") Long id, @Param("qty") int qty);

    int setBatchStatus(@Param("id") Long id, @Param("status") String status);

    // ---------------- 盘点 ----------------

    int insertStockTake(StockTake t);

    int insertStockTakeItem(StockTakeItem i);

    StockTake findStockTakeById(@Param("id") Long id);

    List<StockTakeItem> findStockTakeItems(@Param("takeId") Long takeId);

    List<StockTake> searchStockTakes(@Param("status") String status,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    long countStockTakes(@Param("status") String status);

    int countOpenTakeByProduct(@Param("productId") Long productId);

    int updateTakeItemCounted(@Param("id") Long id,
                              @Param("countedQty") Integer countedQty,
                              @Param("reason") String reason);

    int updateTakeItemDiff(@Param("id") Long id, @Param("diffQty") int diffQty);

    int confirmStockTake(@Param("id") Long id,
                         @Param("diffCount") int diffCount,
                         @Param("diffAmountCent") int diffAmountCent,
                         @Param("operator") String operator,
                         @Param("confirmedAt") String confirmedAt);

    int cancelStockTake(@Param("id") Long id);

    /** 盘点范围内的商品（ALL / CATEGORY / SELECTED） */
    List<com.petshop.module.product.Product> findTakeScopeProducts(@Param("scope") String scope,
                                                                  @Param("scopeValue") String scopeValue);

    List<StockTake> findRecentStockTakes(@Param("limit") int limit);

    long countTakeByPrefix(@Param("prefix") String prefix);
}
