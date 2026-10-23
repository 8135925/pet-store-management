package com.petshop.module.product;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    Product findById(@Param("id") Long id);

    List<Product> findAll();

    List<Product> findActive();

    /** 预警：临期（n 天内到期，含已逾期） */
    List<Product> findExpiring(@Param("today") java.time.LocalDate today, @Param("days") int days);

    /** 预警：低库存 */
    List<Product> findLowStock(@Param("globalWarn") int globalWarn);

    long countLowStock(@Param("globalWarn") int globalWarn);

    long countExpiring(@Param("today") java.time.LocalDate today, @Param("days") int days);

    int insert(Product p);

    int update(Product p);

    int setActive(@Param("id") Long id, @Param("active") boolean active);

    /** 条件扣库存：库存不足影响 0 行（设计文档 §7.3） */
    int deductStock(@Param("id") Long id, @Param("qty") int qty);

    /** 反结账回补 */
    int addStock(@Param("id") Long id, @Param("qty") int qty);
}
