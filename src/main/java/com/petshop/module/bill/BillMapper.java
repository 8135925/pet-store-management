package com.petshop.module.bill;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface BillMapper {

    Bill findById(@Param("id") Long id);

    Bill findByRequestId(@Param("requestId") String requestId);

    /** 单据查询：日期区间/客户/宠物/单号/状态 */
    List<Bill> search(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,
                      @Param("customerId") Long customerId, @Param("petId") Long petId,
                      @Param("keyword") String keyword, @Param("status") String status,
                      @Param("offset") int offset, @Param("limit") int limit);

    long countSearch(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,
                     @Param("customerId") Long customerId, @Param("petId") Long petId,
                     @Param("keyword") String keyword, @Param("status") String status);

    List<Bill> findRecentByCustomer(@Param("customerId") Long customerId, @Param("limit") int limit);

    List<Bill> findSettledByDate(@Param("date") LocalDate date);

    long countByNoPrefix(@Param("prefix") String prefix);

    int insert(Bill b);

    int settle(@Param("id") Long id, @Param("receivedCent") int receivedCent,
               @Param("payMethod") String payMethod, @Param("settledAt") java.time.LocalDateTime settledAt);

    int cancel(@Param("id") Long id, @Param("reason") String reason,
               @Param("cancelledAt") java.time.LocalDateTime cancelledAt);
}
