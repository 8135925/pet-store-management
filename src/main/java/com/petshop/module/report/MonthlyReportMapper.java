package com.petshop.module.report;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MonthlyReportMapper {

    /** 按月逐日汇总：售卡/商品/服务三列 + 单量 */
    List<Map<String, Object>> monthly(@Param("from") String from, @Param("to") String to);

    /** 按月逐日商品成本快照（毛利用，v1.1 起有值） */
    List<Map<String, Object>> monthlyCost(@Param("from") String from, @Param("to") String to);
}
