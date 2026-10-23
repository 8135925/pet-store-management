package com.petshop.module.card;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CardLogMapper {

    int insertConsume(CardConsumeLog log);

    List<CardConsumeLog> findConsumeByCard(@Param("cardId") Long cardId);

    List<CardConsumeLog> findConsumeByBill(@Param("billId") Long billId);

    int insertExtend(CardExtendLog log);

    List<CardExtendLog> findExtendByCard(@Param("cardId") Long cardId);
}
