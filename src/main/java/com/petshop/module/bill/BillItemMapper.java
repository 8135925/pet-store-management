package com.petshop.module.bill;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BillItemMapper {

    List<BillItem> findByBill(@Param("billId") Long billId);

    List<BillItem> findProductsByBill(@Param("billId") Long billId);

    int insert(BillItem i);
}
