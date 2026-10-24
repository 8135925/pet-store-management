package com.petshop.module.staff;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface CommissionMapper {

    // ---------------- 员工 ----------------

    int insertStaff(Staff s);

    int updateStaff(Staff s);

    int setStaffActive(@Param("id") Long id, @Param("active") boolean active);

    Staff findStaffById(@Param("id") Long id);

    List<Staff> findAllStaff();

    List<Staff> findActiveStaff();

    /** sys_user.staff_id：登录账号关联的员工（开单默认服务人员） */
    Long findStaffIdByUserId(@Param("userId") Long userId);

    // ---------------- 规则 ----------------

    int insertRule(CommissionRule r);

    int updateRule(CommissionRule r);

    int setRuleActive(@Param("id") Long id, @Param("active") boolean active);

    CommissionRule findRuleById(@Param("id") Long id);

    List<CommissionRule> findAllRules();

    /** 结账日生效的规则（Service 里再按优先级匹配） */
    List<CommissionRule> findEffectiveRules(@Param("date") LocalDate date);

    // ---------------- 提成记录 ----------------

    int insertRecord(CommissionRecord r);

    List<CommissionRecord> findPositiveByBill(@Param("billId") Long billId);

    List<CommissionRecord> listRecords(@Param("staffId") Long staffId,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    long countRecords(@Param("staffId") Long staffId,
                      @Param("from") LocalDate from,
                      @Param("to") LocalDate to);

    /** 按员工汇总未结算提成（生成结算单用） */
    List<Map<String, Object>> groupUnsettled(@Param("from") LocalDate from,
                                             @Param("to") LocalDate to);

    int markRecordsSettled(@Param("settleId") Long settleId,
                           @Param("staffId") Long staffId,
                           @Param("from") LocalDate from,
                           @Param("to") LocalDate to);

    // ---------------- 结算单 ----------------

    int insertSettle(CommissionSettle s);

    CommissionSettle findSettleById(@Param("id") Long id);

    CommissionSettle findSettleByStaffMonth(@Param("staffId") Long staffId,
                                            @Param("month") String month);

    List<CommissionSettle> listSettles(@Param("month") String month);

    int confirmSettle(@Param("id") Long id, @Param("operator") String operator);

    // ---------------- 报表 ----------------

    /** 员工业绩：明细金额（排除卡抵扣行）与提成 */
    List<Map<String, Object>> staffPerformance(@Param("from") LocalDate from,
                                               @Param("to") LocalDate to);
}
