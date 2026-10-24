package com.petshop.module.appointment;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppointmentMapper {

    Appointment findById(@Param("id") Long id);

    List<Appointment> findByDate(@Param("date") String date);

    /** 该时段占用数（BOOKED + ARRIVED） */
    int countSlot(@Param("date") String date, @Param("slot") String slot);

    int insert(Appointment a);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int markDone(@Param("id") Long id, @Param("billId") Long billId);

    /** 结账事务内回写：仅 ARRIVED 状态且客户匹配时生效，返回影响行数 */
    int markDoneIfArrived(@Param("id") Long id, @Param("billId") Long billId,
                          @Param("customerId") Long customerId);

    /** 过期未处理（date < today 且仍 BOOKED） */
    List<Appointment> findStale(@Param("today") String today);
}
