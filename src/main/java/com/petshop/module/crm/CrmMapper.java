package com.petshop.module.crm;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface CrmMapper {

    // ---------------- 标签 ----------------

    int insertPetTag(Tag t);

    int deletePetTag(@Param("petId") Long petId, @Param("tagCode") String tagCode);

    List<Tag> findPetTags(@Param("petId") Long petId);

    int insertCustomerTag(Tag t);

    int deleteCustomerTag(@Param("customerId") Long customerId, @Param("tagCode") String tagCode);

    List<Tag> findCustomerTags(@Param("customerId") Long customerId);

    // ---------------- 待办 ----------------

    int insertTask(FollowUpTask t);

    int existsTask(@Param("customerId") Long customerId,
                   @Param("petId") Long petId,
                   @Param("type") String type,
                   @Param("dueOn") LocalDate dueOn);

    FollowUpTask findTaskById(@Param("id") Long id);

    List<FollowUpTask> findPending(@Param("date") LocalDate date);

    long countPending(@Param("date") LocalDate date);

    int finishTask(@Param("id") Long id,
                   @Param("status") String status,
                   @Param("result") String result,
                   @Param("operator") String operator);

    int postponeTask(@Param("id") Long id, @Param("dueOn") LocalDate dueOn);

    int expireOld(@Param("before") LocalDate before);

    /** 服务后回访来源：某营业日有服务/寄养明细的客户+宠物 */
    List<Map<String, Object>> servicePetsForFollowUp(@Param("date") LocalDate date);

    List<Map<String, Object>> petsWithBirthday(@Param("mmdd") String mmdd);

    List<Map<String, Object>> customersWithBirthday(@Param("mmdd") String mmdd);

    List<Map<String, Object>> recallCustomers(@Param("today") LocalDate today,
                                              @Param("days") int days,
                                              @Param("limit") int limit);

    // ---------------- 触达 ----------------

    int insertContact(ContactLog c);

    List<ContactLog> findContacts(@Param("customerId") Long customerId, @Param("limit") int limit);

    // ---------------- 客户 360 ----------------

    Map<String, Object> customerStats(@Param("customerId") Long customerId);

    List<Map<String, Object>> topServices(@Param("customerId") Long customerId);
}
