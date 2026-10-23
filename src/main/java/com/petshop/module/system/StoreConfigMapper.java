package com.petshop.module.system;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface StoreConfigMapper {

    String findValue(@Param("k") String k);

    List<Map<String, String>> findAll();

    int upsert(@Param("k") String k, @Param("v") String v);
}
