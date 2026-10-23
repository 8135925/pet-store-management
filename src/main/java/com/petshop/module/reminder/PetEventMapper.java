package com.petshop.module.reminder;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PetEventMapper {

    PetEvent findById(@Param("id") Long id);

    List<PetEvent> findByPet(@Param("petId") Long petId);

    /** 到期提醒清单：未处理的，按到期日升序（逾期的天然在最前） */
    List<PetEvent> findPending(@Param("windowEnd") LocalDate windowEnd,
                               @Param("windowStart") LocalDate windowStart);

    List<PetEvent> findPendingByCustomer(@Param("customerId") Long customerId);

    int insert(PetEvent e);

    int update(PetEvent e);

    int markContact(@Param("id") Long id, @Param("status") String status,
                    @Param("contactedAt") java.time.LocalDateTime contactedAt,
                    @Param("contactedBy") String contactedBy);
}
