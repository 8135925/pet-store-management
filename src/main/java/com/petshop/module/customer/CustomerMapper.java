package com.petshop.module.customer;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerMapper {

    Customer findById(@Param("id") Long id);

    Customer findByPhone(@Param("phone") String phone);

    List<Customer> search(@Param("keyword") String keyword,
                          @Param("includeInactive") boolean includeInactive,
                          @Param("offset") int offset, @Param("limit") int limit);

    long countSearch(@Param("keyword") String keyword, @Param("includeInactive") boolean includeInactive);

    /** 供开单页选择（含停用过滤） */
    List<Customer> listActive();

    int insert(Customer c);

    int update(Customer c);

    int setActive(@Param("id") Long id, @Param("active") boolean active);

    /** 是否有历史单据（决定停用还是删除） */
    long countBills(@Param("customerId") Long customerId);
}
