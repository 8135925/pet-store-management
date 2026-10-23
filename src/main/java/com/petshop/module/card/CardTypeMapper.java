package com.petshop.module.card;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CardTypeMapper {

    CardType findById(@Param("id") Long id);

    List<CardType> findAll();

    List<CardType> findActive();

    int insert(CardType t);

    int update(CardType t);

    int setActive(@Param("id") Long id, @Param("active") boolean active);
}
