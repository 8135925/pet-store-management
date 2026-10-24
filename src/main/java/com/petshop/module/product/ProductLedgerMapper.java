package com.petshop.module.product;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductLedgerMapper {

    int insert(ProductLedger l);

    List<ProductLedger> findByProduct(@Param("productId") Long productId);

    List<ProductLedger> findRecent(@Param("limit") int limit);

    /** 反结账用：某单某商品的出库台账（按 id 升序） */
    List<ProductLedger> findSaleByBillAndProduct(@Param("billId") Long billId,
                                                 @Param("productId") Long productId);
}
