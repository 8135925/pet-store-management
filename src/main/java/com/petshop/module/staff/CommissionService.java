package com.petshop.module.staff;

import com.petshop.common.BizException;
import com.petshop.common.BusinessDate;
import com.petshop.common.ErrorCode;
import com.petshop.common.SessionUser;
import com.petshop.module.bill.Bill;
import com.petshop.module.bill.BillItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工与提成。
 *
 * <p>提成记录只增不改：反结账写负数冲销（reverse_of_id 指向原记录），
 * 结算确认后打上 settle_id 锁定（BR-21/BR-22）。</p>
 */
@Service
public class CommissionService {

    private static final Logger log = LoggerFactory.getLogger(CommissionService.class);
    private static final int MAX_PAGE_SIZE = 200;

    private final CommissionMapper mapper;

    public CommissionService(CommissionMapper mapper) {
        this.mapper = mapper;
    }

    // ==================== 员工 ====================

    public List<Staff> staffList() {
        return mapper.findAllStaff();
    }

    public List<Staff> activeStaff() {
        return mapper.findActiveStaff();
    }

    public Staff staff(Long id) {
        Staff s = mapper.findStaffById(id);
        if (s == null) {
            throw new BizException(ErrorCode.STAFF_NOT_FOUND);
        }
        return s;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveStaff(Long id, String name, String role, String phone,
                          String hireOn, String leaveOn) {
        if (name == null || name.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "员工姓名不能为空");
        }
        if (!isValidRole(role)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "员工岗位不正确");
        }
        if (id == null) {
            Staff s = new Staff();
            s.setName(name.trim());
            s.setRole(role);
            s.setPhone(phone);
            s.setHireOn(parseDate(hireOn, "入职日期"));
            s.setLeaveOn(parseDate(leaveOn, "离职日期"));
            s.setIsActive(true);
            s.setCreatedAt(LocalDateTime.now());
            mapper.insertStaff(s);
            return s.getId();
        }
        Staff s = staff(id);
        s.setName(name.trim());
        s.setRole(role);
        s.setPhone(phone);
        s.setHireOn(parseDate(hireOn, "入职日期"));
        s.setLeaveOn(parseDate(leaveOn, "离职日期"));
        mapper.updateStaff(s);
        return id;
    }

    public void toggleStaff(Long id, boolean active) {
        staff(id);
        mapper.setStaffActive(id, active);
    }

    private boolean isValidRole(String role) {
        return "GROOMER".equals(role) || "ASSISTANT".equals(role)
                || "CASHIER".equals(role) || "MANAGER".equals(role);
    }

    // ==================== 规则 ====================

    public List<CommissionRule> rules() {
        return mapper.findAllRules();
    }

    public CommissionRule rule(Long id) {
        CommissionRule r = mapper.findRuleById(id);
        if (r == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "提成规则不存在");
        }
        return r;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveRule(Long id, String scope, Long refId, String staffRole,
                         int ratioBp, int fixedCent, String effectiveFrom, String effectiveTo,
                         String remark) {
        if (!isValidScope(scope)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "提成范围不正确");
        }
        if (ratioBp < 0 || ratioBp > 10000) {
            throw new BizException(ErrorCode.PARAM_INVALID, "提成比例必须在 0%~100% 之间");
        }
        if (fixedCent < 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "固定提成不能为负数");
        }
        if (ratioBp == 0 && fixedCent == 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "比例与固定提成至少填一项");
        }
        LocalDate from = parseDate(effectiveFrom, "生效日期");
        if (from == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "生效日期必填");
        }
        LocalDate to = parseDate(effectiveTo, "失效日期");
        if (to != null && to.isBefore(from)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "失效日期不能早于生效日期");
        }
        if (("SERVICE".equals(scope) || "PRODUCT".equals(scope)) && refId == null) {
            // 不指定项目 = 该类型全部；允许，但提示语义
            refId = null;
        }
        if ("CARD".equals(scope) || "BOARDING".equals(scope) || "ALL".equals(scope)) {
            refId = null;
        }

        if (id == null) {
            CommissionRule r = new CommissionRule();
            r.setScope(scope);
            r.setRefId(refId);
            r.setStaffRole(staffRole);
            r.setRatioBp(ratioBp);
            r.setFixedCent(fixedCent);
            r.setEffectiveFrom(from);
            r.setEffectiveTo(to);
            r.setIsActive(true);
            r.setRemark(remark);
            r.setCreatedAt(LocalDateTime.now());
            mapper.insertRule(r);
            return r.getId();
        }
        CommissionRule r = rule(id);
        r.setScope(scope);
        r.setRefId(refId);
        r.setStaffRole(staffRole);
        r.setRatioBp(ratioBp);
        r.setFixedCent(fixedCent);
        r.setEffectiveFrom(from);
        r.setEffectiveTo(to);
        r.setRemark(remark);
        mapper.updateRule(r);
        return id;
    }

    public void toggleRule(Long id, boolean active) {
        rule(id);
        mapper.setRuleActive(id, active);
    }

    private boolean isValidScope(String scope) {
        return "SERVICE".equals(scope) || "PRODUCT".equals(scope)
                || "BOARDING".equals(scope) || "CARD".equals(scope) || "ALL".equals(scope);
    }

    // ==================== 结账生成 / 反结账冲销 ====================

    /**
     * 结账时生成提成（与结账同事务）。
     * 卡抵扣行不提成（BR-19）；未指定服务人员且账号未关联员工时跳过。
     */
    public void recordForBill(Bill bill, List<BillItem> items, SessionUser user) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<CommissionRule> rules = mapper.findEffectiveRules(bill.getBusinessDate());
        if (rules.isEmpty()) {
            return;
        }
        Long defaultStaffId = user == null ? null : mapper.findStaffIdByUserId(user.getId());
        Map<Long, Staff> staffCache = new HashMap<Long, Staff>();

        for (BillItem item : items) {
            if (item.getPaidByCard()) {
                continue;
            }
            Long staffId = item.getStaffId() != null ? item.getStaffId() : defaultStaffId;
            if (staffId == null) {
                continue;
            }
            Staff staff = staffCache.get(staffId);
            if (staff == null) {
                staff = mapper.findStaffById(staffId);
                staffCache.put(staffId, staff);
            }
            if (staff == null || !staff.getIsActive()) {
                continue;
            }
            CommissionRule rule = CommissionCalculator.pick(rules,
                    item.getItemType(), item.getRefId(), staff.getRole(), bill.getBusinessDate());
            if (rule == null) {
                continue;
            }
            int base = item.getAmountCent();
            int amount = CommissionCalculator.calc(base, rule.getRatioBp(), rule.getFixedCent());
            if (amount == 0) {
                continue;
            }
            CommissionRecord r = new CommissionRecord();
            r.setStaffId(staffId);
            r.setBillId(bill.getId());
            r.setBillItemId(item.getId());
            r.setBizType(item.getItemType());
            r.setBaseCent(base);
            r.setRatioBp(rule.getRatioBp());
            r.setFixedCent(rule.getFixedCent());
            r.setAmountCent(amount);
            r.setBusinessDate(bill.getBusinessDate());
            r.setRemark(rule.getRemark());
            r.setCreatedAt(LocalDateTime.now());
            mapper.insertRecord(r);
        }
    }

    /** 售卡提成：整单一条，基数取实收（BR-19） */
    public void recordForCardBill(Bill bill, Long staffId, SessionUser user) {
        Long sid = staffId != null ? staffId : (user == null ? null : mapper.findStaffIdByUserId(user.getId()));
        if (sid == null) {
            return;
        }
        Staff staff = mapper.findStaffById(sid);
        if (staff == null || !staff.getIsActive()) {
            return;
        }
        List<CommissionRule> rules = mapper.findEffectiveRules(bill.getBusinessDate());
        CommissionRule rule = CommissionCalculator.pick(rules, "CARD", null,
                staff.getRole(), bill.getBusinessDate());
        if (rule == null) {
            return;
        }
        int base = bill.getReceivedCent();
        int amount = CommissionCalculator.calc(base, rule.getRatioBp(), rule.getFixedCent());
        if (amount == 0) {
            return;
        }
        CommissionRecord r = new CommissionRecord();
        r.setStaffId(sid);
        r.setBillId(bill.getId());
        r.setBillItemId(null);
        r.setBizType("CARD");
        r.setBaseCent(base);
        r.setRatioBp(rule.getRatioBp());
        r.setFixedCent(rule.getFixedCent());
        r.setAmountCent(amount);
        r.setBusinessDate(bill.getBusinessDate());
        r.setRemark("售卡提成");
        r.setCreatedAt(LocalDateTime.now());
        mapper.insertRecord(r);
    }

    /** 反结账冲销：写负数记录，保留原记录 */
    public void reverseForBill(Bill bill, SessionUser user) {
        List<CommissionRecord> positives = mapper.findPositiveByBill(bill.getId());
        if (positives.isEmpty()) {
            return;
        }
        LocalDate today = BusinessDate.today();
        for (CommissionRecord src : positives) {
            CommissionRecord r = new CommissionRecord();
            r.setStaffId(src.getStaffId());
            r.setBillId(src.getBillId());
            r.setBillItemId(src.getBillItemId());
            r.setBizType(src.getBizType());
            r.setBaseCent(-src.getBaseCent());
            r.setRatioBp(src.getRatioBp());
            r.setFixedCent(0);
            r.setAmountCent(-src.getAmountCent());
            r.setBusinessDate(today);
            r.setReverseOfId(src.getId());
            r.setRemark("反结账冲销 " + bill.getBillNo());
            r.setCreatedAt(LocalDateTime.now());
            mapper.insertRecord(r);
        }
        log.info("反结账冲销提成 {} 条 billNo={}", positives.size(), bill.getBillNo());
    }

    // ==================== 查询与结算 ====================

    public Map<String, Object> records(Long staffId, LocalDate from, LocalDate to, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        long offset = (long) (p - 1) * s;
        long total = mapper.countRecords(staffId, from, to);
        List<CommissionRecord> rows = total == 0 ? new ArrayList<CommissionRecord>()
                : mapper.listRecords(staffId, from, to, (int) Math.min(offset, Integer.MAX_VALUE), s);
        Map<String, Object> r = new HashMap<String, Object>();
        r.put("total", total);
        r.put("page", p);
        r.put("pages", (total + s - 1) / s);
        r.put("rows", rows);
        return r;
    }

    public List<CommissionSettle> settles(String month) {
        return mapper.listSettles(month);
    }

    public CommissionSettle settle(Long id) {
        CommissionSettle s = mapper.findSettleById(id);
        if (s == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "提成结算单不存在");
        }
        return s;
    }

    /** 生成某月结算单（草稿）：按员工汇总未结算记录 */
    @Transactional(rollbackFor = Exception.class)
    public int generateSettle(String month, String operator) {
        YearMonth ym = parseMonth(month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        List<Map<String, Object>> groups = mapper.groupUnsettled(from, to);
        int created = 0;
        for (Map<String, Object> g : groups) {
            Long staffId = ((Number) g.get("staffId")).longValue();
            int total = ((Number) g.get("total")).intValue();
            int cnt = ((Number) g.get("cnt")).intValue();
            if (mapper.findSettleByStaffMonth(staffId, ym.toString()) != null) {
                continue;
            }
            CommissionSettle s = new CommissionSettle();
            s.setSettleNo("M" + ym.toString().replace("-", "") + String.format("%03d", staffId));
            s.setMonth(ym.toString());
            s.setStaffId(staffId);
            s.setTotalCent(total);
            s.setRecordCount(cnt);
            s.setStatus("DRAFT");
            s.setOperator(operator);
            s.setCreatedAt(LocalDateTime.now());
            mapper.insertSettle(s);
            created++;
        }
        if (created == 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "该月没有可结算的提成记录");
        }
        return created;
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmSettle(Long id, String operator) {
        CommissionSettle s = settle(id);
        if (!"DRAFT".equals(s.getStatus())) {
            throw new BizException(ErrorCode.COMMISSION_ALREADY_SETTLED);
        }
        YearMonth ym = parseMonth(s.getMonth());
        int rows = mapper.confirmSettle(id, operator);
        if (rows == 0) {
            throw new BizException(ErrorCode.COMMISSION_ALREADY_SETTLED);
        }
        mapper.markRecordsSettled(id, s.getStaffId(), ym.atDay(1), ym.atEndOfMonth());
        log.info("提成结算确认 settleNo={} month={} total={}", s.getSettleNo(), s.getMonth(), s.getTotalCent());
    }

    public List<Map<String, Object>> performance(LocalDate from, LocalDate to) {
        return mapper.staffPerformance(from, to);
    }

    // ==================== 内部 ====================

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month == null || month.trim().isEmpty()
                    ? YearMonth.now().toString() : month.trim());
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "月份格式不正确");
        }
    }

    private LocalDate parseDate(String s, String field) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, field + "格式不正确");
        }
    }
}
