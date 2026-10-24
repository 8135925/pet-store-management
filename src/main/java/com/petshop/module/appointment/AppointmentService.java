package com.petshop.module.appointment;

import com.petshop.common.BizException;
import com.petshop.common.ErrorCode;
import com.petshop.common.SessionUser;
import com.petshop.module.customer.Pet;
import com.petshop.module.customer.PetMapper;
import com.petshop.module.boarding.BoardingOrderMapper;
import com.petshop.module.system.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/** 预约（v1.1）：时段容量 BR、状态流转 */
@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private static final Pattern SLOT_PAT = Pattern.compile("^([01]\\d|2[01]):(00|30)$");
    private static final Pattern DATE_PAT = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private final AppointmentMapper mapper;
    private final PetMapper petMapper;
    private final BoardingOrderMapper boardingOrderMapper;
    private final ConfigService configService;

    public AppointmentService(AppointmentMapper mapper, PetMapper petMapper,
                              BoardingOrderMapper boardingOrderMapper, ConfigService configService) {
        this.mapper = mapper;
        this.petMapper = petMapper;
        this.boardingOrderMapper = boardingOrderMapper;
        this.configService = configService;
    }

    public Appointment detail(Long id) {
        Appointment a = mapper.findById(id);
        if (a == null) {
            throw new BizException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }
        return a;
    }

    public List<Appointment> byDate(String date) {
        return mapper.findByDate(date);
    }

    public List<Appointment> stale() {
        return mapper.findStale(LocalDate.now().toString());
    }

    /** 新建预约（时段容量 BR-13/FR-M9-02） */
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long customerId, Long petId, Long serviceId, String date,
                       String slot, String remark, SessionUser user) {
        if (customerId == null || petId == null || serviceId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "客户、宠物、服务项目必填");
        }
        if (date == null || !DATE_PAT.matcher(date.trim()).matches()
                || slot == null || !SLOT_PAT.matcher(slot.trim()).matches()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "预约日期或时段格式不正确（时段为 30 分钟粒度）");
        }
        LocalDate d = LocalDate.parse(date.trim());
        if (d.isBefore(LocalDate.now())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "预约日期不能早于今天");
        }
        Pet pet = petMapper.findById(petId);
        if (pet == null || !pet.getIsActive() || !customerId.equals(pet.getOwnerId())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "宠物不存在或不属于所选客户");
        }
        String s = slot.trim();
        int used = mapper.countSlot(d.toString(), s);
        int capacity = Math.max(configService.slotCapacity(), 1);
        if (used >= capacity) {
            throw new BizException(ErrorCode.SLOT_FULL, "该时段预约已满（" + used + "/" + capacity + "）");
        }
        Appointment a = new Appointment();
        a.setCustomerId(customerId);
        a.setPetId(petId);
        a.setServiceId(serviceId);
        a.setAppointDate(d);
        a.setSlot(s);
        a.setSizeTier(pet.sizeTier());
        a.setStatus("BOOKED");
        a.setRemark(remark);
        a.setCreatedAt(LocalDateTime.now());
        a.setCreatedBy(user.getDisplayName());
        mapper.insert(a);
        log.info("新建预约 id={} pet={} {} {} by={}", a.getId(), pet.getName(), date, s,
                user.getDisplayName());
        return a.getId();
    }

    /** BR-13：宠物是否在店寄养（前端强提示用） */
    public boolean petInBoarding(Long petId) {
        return boardingOrderMapper.countActiveByPet(petId) > 0;
    }

    private Appointment mustBooked(Long id) {
        Appointment a = mapper.findById(id);
        if (a == null) {
            throw new BizException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }
        if (!"BOOKED".equals(a.getStatus())) {
            throw new BizException(ErrorCode.APPOINTMENT_STATE_INVALID,
                    "该预约当前状态为「" + a.getStatus() + "」，不允许此操作");
        }
        return a;
    }

    @Transactional(rollbackFor = Exception.class)
    public void arrive(Long id) {
        mustBooked(id);
        requireMoved(mapper.updateStatus(id, "ARRIVED"));
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        mustBooked(id);
        requireMoved(mapper.updateStatus(id, "CANCELLED"));
    }

    @Transactional(rollbackFor = Exception.class)
    public void noshow(Long id) {
        mustBooked(id);
        requireMoved(mapper.updateStatus(id, "NO_SHOW"));
    }

    private void requireMoved(int rows) {
        if (rows == 0) {
            throw new BizException(ErrorCode.APPOINTMENT_STATE_INVALID, "预约状态已变化，请刷新后重试");
        }
    }

    /** 结账成功后回写（在 settle 事务内调用）：ARRIVED → DONE */
    @Transactional(rollbackFor = Exception.class)
    public void markDone(Long appointmentId, Long billId, Long customerId) {
        Appointment a = mapper.findById(appointmentId);
        if (a == null) {
            throw new BizException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }
        if (!"ARRIVED".equals(a.getStatus())) {
            throw new BizException(ErrorCode.APPOINTMENT_STATE_INVALID, "该预约不是「已到店」状态");
        }
        if (!a.getCustomerId().equals(customerId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "预约客户与账单客户不一致");
        }
        mapper.markDone(appointmentId, billId);
    }
}
