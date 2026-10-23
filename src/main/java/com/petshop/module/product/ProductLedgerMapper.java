package com.petshop.module.product;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductLedgerMapper {

    int insert(ProductLedger l);

    List<ProductLedger> findByProduct(@Param("productId") Long productId);

    List<ProductLedger> findRecent(@Param("limit") int limit);
}
