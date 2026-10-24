package com.petshop.module.boarding;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BoardingLogMapper {

    List<BoardingLog> findByOrder(@Param("orderId") Long orderId);

    int insert(BoardingLog l);
}
