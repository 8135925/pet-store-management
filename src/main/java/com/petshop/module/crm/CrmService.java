package com.petshop.module.crm;

import com.petshop.common.BizException;
import com.petshop.common.BusinessDate;
import com.petshop.common.ErrorCode;
import com.petshop.common.SessionUser;
import com.petshop.module.customer.CustomerMapper;
import com.petshop.module.customer.Pet;
import com.petshop.module.customer.PetMapper;
import com.petshop.module.system.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户经营：标签、回访待办、触达记录、流失召回。
 *
 * <p>待办生成是幂等的（唯一索引 + existsTask），定时任务每天可安全重跑。</p>
 */
@Service
public class CrmService {

    private static final Logger log = LoggerFactory.getLogger(CrmService.class);
    private static final DateTimeFormatter MMDD = DateTimeFormatter.ofPattern("MM-dd");

    private final CrmMapper mapper;
    private final CustomerMapper customerMapper;
    private final PetMapper petMapper;
    private final ConfigService configService;

    public CrmService(CrmMapper mapper, CustomerMapper customerMapper,
                      PetMapper petMapper, ConfigService configService) {
        this.mapper = mapper;
        this.customerMapper = customerMapper;
        this.petMapper = petMapper;
        this.configService = configService;
    }

    // ==================== 标签 ====================

    public List<Tag> petTags(Long petId) {
        return mapper.findPetTags(petId);
    }

    public List<Tag> customerTags(Long customerId) {
        return mapper.findCustomerTags(customerId);
    }

    public void addPetTag(Long petId, String tagCode, String note) {
        if (!TagLabels.isValidPet(tagCode)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "宠物标签不正确");
        }
        try {
            Tag t = new Tag();
            t.setOwnerId(petId);
            t.setTagCode(tagCode);
            t.setNote(note);
            t.setCreatedAt(LocalDateTime.now());
            mapper.insertPetTag(t);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 重复打同一个标签忽略，不报错
        }
    }

    public void removePetTag(Long petId, String tagCode) {
        mapper.deletePetTag(petId, tagCode);
    }

    public void addCustomerTag(Long customerId, String tagCode, String note) {
        if (!TagLabels.isValidCustomer(tagCode)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "客户标签不正确");
        }
        try {
            Tag t = new Tag();
            t.setOwnerId(customerId);
            t.setTagCode(tagCode);
            t.setNote(note);
            t.setCreatedAt(LocalDateTime.now());
            mapper.insertCustomerTag(t);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 忽略重复
        }
    }

    public void removeCustomerTag(Long customerId, String tagCode) {
        mapper.deleteCustomerTag(customerId, tagCode);
    }

    // ==================== 待办 ====================

    public List<FollowUpTask> pending() {
        return mapper.findPending(BusinessDate.today());
    }

    public long pendingCount() {
        return mapper.countPending(BusinessDate.today());
    }

    public FollowUpTask task(Long id) {
        FollowUpTask t = mapper.findTaskById(id);
        if (t == null) {
            throw new BizException(ErrorCode.FOLLOW_UP_NOT_FOUND);
        }
        return t;
    }

    /** 完成任务：写触达记录 + 标记 DONE（同一事务） */
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, String channel, String outcome,
                             String content, String nextFollowOn, SessionUser user) {
        FollowUpTask t = task(taskId);
        if (!"PENDING".equals(t.getStatus())) {
            throw new BizException(ErrorCode.FOLLOW_UP_NOT_FOUND);
        }
        if (content == null || content.trim().length() < 2) {
            throw new BizException(ErrorCode.PARAM_INVALID, "沟通摘要至少写 2 个字");
        }
        String ch = isValidChannel(channel) ? channel : "PHONE";
        String oc = isValidOutcome(outcome) ? outcome : "CONNECTED";

        ContactLog log = new ContactLog();
        log.setCustomerId(t.getCustomerId());
        log.setPetId(t.getPetId());
        log.setTaskId(t.getId());
        log.setChannel(ch);
        log.setResult(oc);
        log.setContent(content.trim());
        if (nextFollowOn != null && !nextFollowOn.trim().isEmpty()) {
            try {
                log.setNextFollowOn(LocalDate.parse(nextFollowOn.trim()));
            } catch (Exception e) {
                throw new BizException(ErrorCode.PARAM_INVALID, "下次跟进日期格式不正确");
            }
        }
        log.setOperator(user.getDisplayName());
        log.setCreatedAt(LocalDateTime.now());
        mapper.insertContact(log);

        int rows = mapper.finishTask(taskId, "DONE", content.trim(), user.getDisplayName());
        if (rows == 0) {
            throw new BizException(ErrorCode.FOLLOW_UP_NOT_FOUND);
        }
    }

    public void skipTask(Long taskId, String reason, SessionUser user) {
        task(taskId);
        int rows = mapper.finishTask(taskId, "SKIPPED",
                reason == null || reason.trim().isEmpty() ? "客户明确拒绝" : reason.trim(),
                user.getDisplayName());
        if (rows == 0) {
            throw new BizException(ErrorCode.FOLLOW_UP_NOT_FOUND);
        }
    }

    public void postponeTask(Long taskId, String dueOn) {
        task(taskId);
        LocalDate d;
        try {
            d = LocalDate.parse(dueOn.trim());
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "日期格式不正确");
        }
        if (d.isBefore(LocalDate.now())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "改期不能早于今天");
        }
        mapper.postponeTask(taskId, d);
    }

    public void createManual(Long customerId, Long petId, String dueOn, String note, SessionUser user) {
        if (customerMapper.findById(customerId) == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "客户不存在");
        }
        LocalDate d = parseDate(dueOn, "跟进日期");
        if (d == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "跟进日期必填");
        }
        createTask(customerId, petId, "MANUAL", d, "MANUAL", null,
                note == null ? "手工待办" : note.trim());
    }

    // ==================== 自动生成（每日 04:10） ====================

    @Scheduled(cron = "0 10 4 * * ?")
    public void scheduledGenerate() {
        try {
            int n = generateDaily();
            log.info("每日待办生成完成：新增 {} 条", n);
        } catch (Exception e) {
            log.error("每日待办生成失败", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int generateDaily() {
        LocalDate today = BusinessDate.today();
        int created = 0;

        // 1) 服务后回访：N 天前有服务/寄养的客户与宠物
        int followDays = Math.max(1, configService.getInt("follow_up_days", 3));
        for (Map<String, Object> row : mapper.servicePetsForFollowUp(today.minusDays(followDays))) {
            created += createTask(((Number) row.get("customerId")).longValue(),
                    toLong(row.get("petId")), "SERVICE_FOLLOW_UP", today,
                    "BILL", null, "服务后 " + followDays + " 天回访");
        }

        // 2) 生日：提前 7 天（含 2/29 落到 2/28 的自然日算法）
        LocalDate target = today.plusDays(7);
        String mmdd = target.format(MMDD);
        for (Map<String, Object> row : mapper.petsWithBirthday(mmdd)) {
            created += createTask(((Number) row.get("customerId")).longValue(),
                    toLong(row.get("petId")), "BIRTHDAY", target,
                    "PET", toLong(row.get("petId")), "宠物生日关怀");
        }
        for (Map<String, Object> row : mapper.customersWithBirthday(mmdd)) {
            created += createTask(((Number) row.get("customerId")).longValue(),
                    null, "CUSTOMER_BIRTHDAY", target, "CUSTOMER", null, "客户生日关怀");
        }

        // 3) 超 60 天未处理自动失效
        mapper.expireOld(today.minusDays(60));
        return created;
    }

    private int createTask(Long customerId, Long petId, String type, LocalDate dueOn,
                           String sourceType, Long sourceId, String note) {
        if (customerId == null) {
            return 0;
        }
        if (mapper.existsTask(customerId, petId, type, dueOn) > 0) {
            return 0;
        }
        FollowUpTask t = new FollowUpTask();
        t.setCustomerId(customerId);
        t.setPetId(petId);
        t.setType(type);
        t.setDueOn(dueOn);
        t.setStatus("PENDING");
        t.setSourceType(sourceType);
        t.setSourceId(sourceId);
        t.setNote(note);
        t.setCreatedAt(LocalDateTime.now());
        mapper.insertTask(t);
        return 1;
    }

    // ==================== 流失召回 ====================

    public List<Map<String, Object>> recallList() {
        int days = Math.max(30, configService.getInt("recall_days", 90));
        return mapper.recallCustomers(BusinessDate.today(), days, 200);
    }

    @Transactional(rollbackFor = Exception.class)
    public int generateRecallTasks(SessionUser user) {
        List<Map<String, Object>> list = recallList();
        int created = 0;
        LocalDate today = BusinessDate.today();
        for (Map<String, Object> row : list) {
            created += createTask(((Number) row.get("customerId")).longValue(), null,
                    "RECALL", today, "RECALL", null, "超过 90 天未到店，电话召回");
        }
        return created;
    }

    // ==================== 客户 360 ====================

    public Map<String, Object> customer360(Long customerId) {
        if (customerMapper.findById(customerId) == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "客户不存在");
        }
        Map<String, Object> m = new HashMap<String, Object>();
        Map<String, Object> stats = mapper.customerStats(customerId);
        m.put("stats", stats);
        long spent = stats == null || stats.get("totalSpent") == null ? 0
                : ((Number) stats.get("totalSpent")).longValue();
        long saleCount = stats == null || stats.get("saleCount") == null ? 0
                : ((Number) stats.get("saleCount")).longValue();
        m.put("avgTicketCent", saleCount == 0 ? 0 : (int) (spent / saleCount));
        m.put("topServices", mapper.topServices(customerId));
        m.put("customerTags", mapper.findCustomerTags(customerId));
        m.put("contacts", mapper.findContacts(customerId, 20));

        List<Pet> pets = petMapper.findByOwner(customerId);
        m.put("pets", pets);
        Map<Long, List<Tag>> petTags = new LinkedHashMap<Long, List<Tag>>();
        for (Pet p : pets) {
            petTags.put(p.getId(), mapper.findPetTags(p.getId()));
        }
        m.put("petTags", petTags);
        return m;
    }

    public List<FollowUpTask> tasksOfCustomer(Long customerId) {
        return new ArrayList<FollowUpTask>();   // 预留：客户详情页暂不展示任务列表
    }

    // ==================== 内部 ====================

    private Long toLong(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    private boolean isValidChannel(String c) {
        return "PHONE".equals(c) || "WECHAT".equals(c) || "VISIT".equals(c) || "SMS_COPY".equals(c);
    }

    private boolean isValidOutcome(String o) {
        return "CONNECTED".equals(o) || "NO_ANSWER".equals(o)
                || "REFUSED".equals(o) || "BOOKED".equals(o);
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
