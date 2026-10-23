package com.petshop.module.svc;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceItemMapper {

    ServiceItem findById(@Param("id") Long id);

    List<ServiceItem> findAll();

    List<ServiceItem> findActive();

    int insert(ServiceItem s);

    int update(ServiceItem s);

    int setActive(@Param("id") Long id, @Param("active") boolean active);
}
