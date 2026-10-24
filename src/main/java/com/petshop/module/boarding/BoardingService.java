package com.petshop.module.boarding;

import com.petshop.common.BizException;
import com.petshop.common.BusinessDate;
import com.petshop.common.ErrorCode;
import com.petshop.common.Money;
import com.petshop.common.SessionUser;
import com.petshop.module.bill.Bill;
import com.petshop.module.bill.BillItem;
import com.petshop.module.bill.BillItemMapper;
import com.petshop.module.bill.BillMapper;
import com.petshop.module.customer.Pet;
import com.petshop.module.customer.PetMapper;
import com.petshop.module.reminder.PetEvent;
import com.petshop.module.reminder.PetEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 寄养（v1.1）：疫苗拦截 BR-09、笼位互斥 BR-10、按晚计费 BR-11。 */
@Service
public class BoardingService {

    private static final Logger log = LoggerFactory.getLogger(BoardingService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TS_SEC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_PAGE_SIZE = 100;
    /** 单号并发撞唯一索引时的最大重试次数 */
    private static final int NO_RETRY = 3;

    private final BoardingRoomMapper roomMapper;
    private final BoardingOrderMapper orderMapper;
    private final BoardingLogMapper logMapper;
    private final PetMapper petMapper;
    private final PetEventMapper petEventMapper;
    private final BillMapper billMapper;
    private final BillItemMapper itemMapper;
    private final com.petshop.module.card.CardService cardService;

    public BoardingService(BoardingRoomMapper roomMapper, BoardingOrderMapper orderMapper,
                           BoardingLogMapper logMapper, PetMapper petMapper,
                           PetEventMapper petEventMapper,
                           BillMapper billMapper, BillItemMapper itemMapper,
                           com.petshop.module.card.CardService cardService) {
        this.roomMapper = roomMapper;
        this.orderMapper = orderMapper;
        this.logMapper = logMapper;
        this.petMapper = petMapper;
        this.petEventMapper = petEventMapper;
        this.billMapper = billMapper;
        this.itemMapper = itemMapper;
        this.cardService = cardService;
    }

    // ---------------- 笼位 ----------------

    public List<BoardingRoom> rooms() {
        return roomMapper.findAllWithUsage();
    }

    public List<BoardingRoom> activeRooms() {
        return roomMapper.findActive();
    }

    public void saveRoom(Long id, String code, String roomType, String priceYuan, String note) {
        if (code == null || code.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "笼位编号必填");
        }
        BoardingRoom r = id == null ? new BoardingRoom() : roomMapper.findById(id);
        if (r == null) {
            throw new BizException(ErrorCode.ROOM_NOT_FOUND);
        }
        int priceCent = Money.toCent(priceYuan);
        if (priceCent < 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "笼位价格不能为负数");
        }
        r.setCode(code.trim());
        r.setRoomType(roomType);
        r.setDailyPriceCent(priceCent);
        r.setNote(note);
        if (id == null) {
            r.setIsActive(true);
            r.setCreatedAt(LocalDateTime.now());
            try {
                roomMapper.insert(r);
            } catch (DuplicateKeyException e) {
                throw new BizException(ErrorCode.PARAM_INVALID, "笼位编号已存在：" + code);
            }
        } else {
            try {
                roomMapper.update(r);
            } catch (DuplicateKeyException e) {
                throw new BizException(ErrorCode.PARAM_INVALID, "笼位编号已存在：" + code);
            }
        }
    }

    public void toggleRoom(Long id, boolean active) {
        roomMapper.setActive(id, active);
    }

    // ---------------- 寄养单 ----------------

    /** 新建寄养单（BR-10 冲突检测；疫苗问题在入住时强拦，建单时写入提示） */
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long customerId, Long petId, Long roomId, String startAt,
                       String plannedEndOn, String unitPriceYuan, String depositYuan,
                       String remark, SessionUser user) {
        if (customerId == null || petId == null || roomId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "客户、宠物、笼位必填");
        }
        Pet pet = petMapper.findById(petId);
        if (pet == null || !pet.getIsActive()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "宠物不存在或已停用");
        }
        if (!customerId.equals(pet.getOwnerId())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "该宠物不属于所选客户");
        }
        BoardingRoom room = roomMapper.findById(roomId);
        if (room == null || !room.getIsActive()) {
            throw new BizException(ErrorCode.ROOM_NOT_FOUND);
        }
        LocalDateTime start;
        LocalDate endOn;
        try {
            start = LocalDateTime.parse(startAt.trim(), TS);
            endOn = LocalDate.parse(plannedEndOn.trim());
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "时间格式不正确");
        }
        if (!endOn.isAfter(start.toLocalDate())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "预离店日必须晚于入住日");
        }
        String startStr = start.format(TS_SEC);
        String endBound = endOn + " 23:59";
        int conflict = orderMapper.countConflict(roomId, startStr, endBound, null);
        if (conflict > 0) {
            throw new BizException(ErrorCode.ROOM_OCCUPIED);
        }
        // 疫苗预警（建单仅提示，入住强拦 BR-09）
        String vaccineMsg = vaccineProblem(petId, start.toLocalDate());

        int unitPriceCent = Money.toCent(unitPriceYuan);
        int depositCent = depositYuan == null || depositYuan.trim().isEmpty() ? 0 : Money.toCent(depositYuan);
        if (unitPriceCent < 0 || depositCent < 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "单价与押金不能为负数");
        }

        BoardingOrder o = new BoardingOrder();
        o.setCustomerId(customerId);
        o.setPetId(petId);
        o.setRoomId(roomId);
        o.setStartAt(start);
        o.setPlannedEndOn(endOn);
        o.setUnitPriceCent(unitPriceCent);
        o.setDepositCent(depositCent);
        o.setStatus("BOOKED");
        o.setRemark(remark);
        o.setCreatedAt(LocalDateTime.now());
        o.setCreatedBy(user.getDisplayName());
        insertWithNoRetry(o);
        log.info("新建寄养单 {} pet={} room={} by={} {}", o.getOrderNo(), pet.getName(), room.getCode(),
                user.getDisplayName(), vaccineMsg == null ? "" : "⚠ " + vaccineMsg);
        return o.getId();
    }

    /** 疫苗问题描述；无问题返回 null（BR-09） */
    private String vaccineProblem(Long petId, LocalDate startOn) {
        String due = latestVaccineDueOn(petId);
        if (due == null) {
            return "该宠物没有任何疫苗/狂犬记录，入住将被拒绝，请先补录疫苗记录";
        }
        if (LocalDate.parse(due).isBefore(startOn)) {
            return "该宠物疫苗将于 " + due + " 到期，早于入住日，入住将被拒绝";
        }
        return null;
    }

    /** 最近一条疫苗/狂犬的 next_due_on（yyyy-MM-dd）；无记录返回 null（BR-09） */
    public String latestVaccineDueOn(Long petId) {
        String latest = null;
        for (PetEvent e : petEventMapper.findByPet(petId)) {
            if ("VACCINE".equals(e.getEventType()) || "RABIES".equals(e.getEventType())) {
                if (e.getNextDueOn() != null && (latest == null || e.getNextDueOn().isAfter(LocalDate.parse(latest)))) {
                    latest = e.getNextDueOn().toString();
                }
            }
        }
        return latest;
    }

    public BoardingOrder detail(Long id) {
        BoardingOrder o = orderMapper.findById(id);
        if (o == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "寄养单不存在");
        }
        return o;
    }

    public List<BoardingLog> logsOf(Long orderId) {
        return logMapper.findByOrder(orderId);
    }

    /** 办理入住：BOOKED → CHECKED_IN，强拦疫苗问题（BR-09） */
    @Transactional(rollbackFor = Exception.class)
    public void checkin(Long id, SessionUser user) {
        BoardingOrder o = orderMapper.findById(id);
        if (o == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "寄养单不存在");
        }
        if (!"BOOKED".equals(o.getStatus())) {
            throw new BizException(ErrorCode.APPOINTMENT_STATE_INVALID, "该寄养单当前状态不能办理入住");
        }
        // BR-09 疫苗拦截（唯一强制点）
        String due = latestVaccineDueOn(o.getPetId());
        LocalDate today = LocalDate.now();
        if (due == null) {
            throw new BizException(ErrorCode.VACCINE_EXPIRED, "该宠物没有疫苗/狂犬记录，不能办理入住，请先补录疫苗记录");
        }
        if (LocalDate.parse(due).isBefore(today)) {
            throw new BizException(ErrorCode.VACCINE_EXPIRED,
                    "该宠物疫苗已于 " + due + " 过期，不能办理入住，请先补录疫苗记录");
        }
        // 笼位冲突复查（BOOKED 之后可能被别的单占了？同一笼位 BOOKED 也算占用，正常不会；防御性复查）
        String endBound = o.getPlannedEndOn() + " 23:59";
        if (orderMapper.countConflict(o.getRoomId(), o.getStartAt().format(TS_SEC), endBound, o.getId()) > 0) {
            throw new BizException(ErrorCode.ROOM_OCCUPIED);
        }
        int rows = orderMapper.markCheckedIn(id, LocalDateTime.now().format(TS_SEC));
        if (rows == 0) {
            throw new BizException(ErrorCode.APPOINTMENT_STATE_INVALID, "寄养单状态已变化，请刷新后重试");
        }
        log.info("寄养单 {} 办理入住 by={}", o.getOrderNo(), user.getDisplayName());
    }

    /** 修改预离店日（重跑冲突检测 BR-10） */
    @Transactional(rollbackFor = Exception.class)
    public void extend(Long id, String plannedEndOn, String note, SessionUser user) {
        BoardingOrder o = orderMapper.findById(id);
        if (o == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "寄养单不存在");
        }
        if (!"BOOKED".equals(o.getStatus()) && !"CHECKED_IN".equals(o.getStatus())) {
            throw new BizException(ErrorCode.APPOINTMENT_STATE_INVALID, "该寄养单当前状态不能修改预离店日");
        }
        LocalDate newEnd;
        try {
            newEnd = LocalDate.parse(plannedEndOn.trim());
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "日期格式不正确");
        }
        if (!newEnd.isAfter(o.getStartAt().toLocalDate())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "预离店日必须晚于入住日");
        }
        String endBound = newEnd + " 23:59";
        String startBound = o.getStartAt().format(TS_SEC);
        if (orderMapper.countConflict(o.getRoomId(), startBound, endBound, o.getId()) > 0) {
            throw new BizException(ErrorCode.ROOM_OCCUPIED);
        }
        String line = String.format("[%s %s 改预离店日 %s → %s] %s",
                LocalDateTime.now().format(TS), user.getDisplayName(),
                o.getPlannedEndOn(), newEnd, note == null ? "" : note);
        String newRemark = o.getRemark() == null || o.getRemark().trim().isEmpty()
                ? line : o.getRemark() + " " + line;
        int rows = orderMapper.extend(id, newEnd.toString(), newRemark);
        if (rows == 0) {
            throw new BizException(ErrorCode.APPOINTMENT_STATE_INVALID, "寄养单状态已变化，请刷新后重试");
        }
    }

    /** 取消（仅 BOOKED） */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        BoardingOrder o = orderMapper.findById(id);
        if (o == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "寄养单不存在");
        }
        if (!"BOOKED".equals(o.getStatus())) {
            throw new BizException(ErrorCode.APPOINTMENT_STATE_INVALID, "只有未入住的寄养单可以取消");
        }
        int rows = orderMapper.cancel(id);
        if (rows == 0) {
            throw new BizException(ErrorCode.APPOINTMENT_STATE_INVALID, "寄养单状态已变化，请刷新后重试");
        }
    }

    /** 每日喂养记录（BR-14 一天一条） */
    @Transactional(rollbackFor = Exception.class)
    public void addLog(Long orderId, String logDate, Integer fedCount, boolean walked,
                       String note, SessionUser user) {
        BoardingOrder o = orderMapper.findById(orderId);
        if (o == null || !"CHECKED_IN".equals(o.getStatus())) {
            throw new BizException(ErrorCode.BOARDING_NOT_IN);
        }
        LocalDate date;
        try {
            date = LocalDate.parse(logDate.trim());
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "日期格式不正确");
        }
        if (date.isBefore(o.getStartAt().toLocalDate())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "记录日期不能早于入住日");
        }
        BoardingLog l = new BoardingLog();
        l.setOrderId(orderId);
        l.setLogDate(date);
        l.setFedCount(fedCount == null || fedCount < 0 ? 0 : fedCount);
        l.setWalked(walked);
        l.setNote(note);
        l.setOperator(user.getDisplayName());
        l.setCreatedAt(LocalDateTime.now());
        try {
            logMapper.insert(l);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.PARAM_INVALID, date + " 已有记录，一天只能一条（BR-14）");
        }
    }

    /** 退住结算（F8）：按晚计费 → 生成 SALE 单（BOARDING 明细）→ 双向回写 */
    @Transactional(rollbackFor = Exception.class)
    public Long checkout(Long id, String payMethod, SessionUser user) {
        BoardingOrder o = orderMapper.findById(id);
        if (o == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "寄养单不存在");
        }
        if ("CHECKED_OUT".equals(o.getStatus())) {
            throw new BizException(ErrorCode.BOARDING_SETTLED);
        }
        if (!"CHECKED_IN".equals(o.getStatus())) {
            throw new BizException(ErrorCode.BOARDING_NOT_IN);
        }
        if (!isValidPayMethod(payMethod)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "收款方式不正确");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = BusinessDate.of(now);
        int days = (int) Math.max(1, ChronoUnit.DAYS.between(o.getStartAt().toLocalDate(), now.toLocalDate()));
        long amountLong = (long) o.getUnitPriceCent() * days;
        if (amountLong > Integer.MAX_VALUE) {
            throw new BizException(ErrorCode.PARAM_INVALID, "寄养金额超出上限，请核对单价与天数");
        }
        int amount = (int) amountLong;

        Bill b = new Bill();
        b.setBillType("SALE");
        b.setCustomerId(o.getCustomerId());
        b.setPetId(o.getPetId());
        b.setStatus("SETTLED");
        b.setTotalCent(amount);
        b.setDiscountCent(0);
        b.setReceivableCent(amount);
        b.setReceivedCent(amount);
        b.setPayMethod(payMethod.trim());
        b.setBusinessDate(today);
        b.setSettledAt(now);
        b.setRequestId("BO-" + o.getOrderNo());   // 幂等：一单只结算一次
        b.setOperator(user.getDisplayName());
        b.setRemark("寄养结算 " + o.getOrderNo());
        b.setCreatedAt(now);
        insertBoardingBill(b, today, o.getOrderNo());

        BillItem item = new BillItem();
        item.setBillId(b.getId());
        item.setItemType("BOARDING");
        item.setRefId(o.getId());
        item.setNameSnapshot("寄养·" + o.getPetName() + "·" + days + "晚·" + o.getRoomCode());
        item.setPetId(o.getPetId());
        item.setQty(days);
        item.setUnitPriceCent(o.getUnitPriceCent());
        item.setAmountCent(amount);
        item.setDiscountCent(0);
        item.setPaidByCard(false);
        item.setSortNo(0);
        itemMapper.insert(item);

        int rows = orderMapper.checkout(id, now.format(TS_SEC), days, b.getId());
        if (rows == 0) {
            throw new BizException(ErrorCode.BOARDING_SETTLED);
        }
        log.info("寄养单 {} 退住结算 {} 晚 ¥{} bill={}", o.getOrderNo(), days,
                Money.yuan(amount), b.getId());
        return b.getId();
    }

    // ---------------- 看板 / 查询 ----------------

    public Map<String, Object> board() {
        Map<String, Object> m = new HashMap<>();
        m.put("inHouse", orderMapper.findInHouse());
        m.put("todayArrivals", orderMapper.findTodayArrivals(LocalDate.now().toString()));
        m.put("todayDue", orderMapper.findTodayDue(LocalDate.now().toString()));
        return m;
    }

    public Map<String, Object> search(String keyword, String dateFrom, String dateTo, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);   // size<=0 会导致除零，必须夹紧
        long offset = (long) (p - 1) * s;
        if (offset > Integer.MAX_VALUE) {
            offset = Integer.MAX_VALUE;
        }
        List<BoardingOrder> rows = orderMapper.search(keyword, dateFrom, dateTo, (int) offset, s);
        long total = orderMapper.countSearch(keyword, dateFrom, dateTo);
        Map<String, Object> r = new HashMap<>();
        r.put("rows", rows);
        r.put("total", total);
        r.put("page", p);
        r.put("pages", (total + s - 1) / s);
        return r;
    }

    // ---------------- 内部 ----------------

    /** 生成寄养单号并插入；并发撞唯一索引时重新取号 */
    private void insertWithNoRetry(BoardingOrder o) {
        for (int attempt = 0; ; attempt++) {
            o.setOrderNo(nextNo());
            try {
                orderMapper.insert(o);
                return;
            } catch (DuplicateKeyException e) {
                if (attempt >= NO_RETRY - 1) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "单号生成冲突，请重试");
                }
                log.warn("寄养单号冲突，重新生成：{}", o.getOrderNo());
            }
        }
    }

    /** 插入寄养结算单：幂等键冲突 = 已结算；单号冲突 = 换号重试 */
    private void insertBoardingBill(Bill b, LocalDate today, String orderNo) {
        for (int attempt = 0; ; attempt++) {
            b.setBillNo(cardService.nextBillNo("S", today));
            try {
                billMapper.insert(b);
                return;
            } catch (DuplicateKeyException e) {
                if (billMapper.findByRequestId("BO-" + orderNo) != null) {
                    throw new BizException(ErrorCode.BOARDING_SETTLED);
                }
                if (attempt >= NO_RETRY - 1) {
                    throw e;
                }
                log.warn("结算单号冲突，重新生成：{}", b.getBillNo());
            }
        }
    }

    private String nextNo() {
        String prefix = "B" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = orderMapper.countByNoPrefix(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }

    private boolean isValidPayMethod(String pm) {
        return pm != null && (pm.equals("CASH") || pm.equals("WECHAT") || pm.equals("ALIPAY")
                || pm.equals("BANK_CARD") || pm.equals("OTHER"));
    }
}
