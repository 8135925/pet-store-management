package com.petshop.module.card;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CustomerCardMapper {

    CustomerCard findById(@Param("id") Long id);

    List<CustomerCard> findByCustomer(@Param("customerId") Long customerId);

    /** 开单页：客户的可用卡（ACTIVE 且未过期且有余次） */
    List<CustomerCard> findUsableByCustomer(@Param("customerId") Long customerId, @Param("today") LocalDate today);

    /** 次卡余额表（全店） */
    List<CustomerCard> listBalance(@Param("keyword") String keyword,
                                   @Param("status") String status,
                                   @Param("orderByExpire") boolean orderByExpire);

    long countByNoPrefix(@Param("prefix") String prefix);

    int insert(CustomerCard c);

    int setStatus(@Param("id") Long id, @Param("status") String status);

    int updateExpire(@Param("id") Long id, @Param("expireOn") LocalDate expireOn, @Param("status") String status);

    int updateSourceBill(@Param("id") Long id, @Param("billId") Long billId);

    /** 核销扣次（带条件原子更新）。giftFirst=true 先扣赠次。返回影响行数。 */
    int deductGiftFirst(@Param("cardId") Long cardId, @Param("today") LocalDate today);

    int deductPaidFirst(@Param("cardId") Long cardId, @Param("today") LocalDate today);

    /** 反结账还原 */
    int refundGift(@Param("cardId") Long cardId, @Param("qty") int qty);

    int refundPaid(@Param("cardId") Long cardId, @Param("qty") int qty);
}
