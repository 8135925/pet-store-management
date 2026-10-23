package com.petshop.module.report;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Map;

@Mapper
public interface ReportMapper {

    /** 单日营业汇总（三类收入分列 + 单量 + 核销次数） */
    Map<String, Object> dailySummary(@Param("date") LocalDate date);

    /** 今日看板数字 */
    Map<String, Object> dashboard(@Param("today") LocalDate today);
}
