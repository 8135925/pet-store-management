package com.petshop.module.reminder;

import com.petshop.common.BizException;
import com.petshop.common.ErrorCode;
import com.petshop.common.SessionUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class ReminderController {

    private final PetEventMapper eventMapper;

    public ReminderController(PetEventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    /** 三档提醒清单：all=全部未处理 / today / 7d / 30d */
    @GetMapping("/reminders")
    public String list(@RequestParam(defaultValue = "all") String tier, Model model) {
        LocalDate today = LocalDate.now();
        LocalDate start = null;
        LocalDate end;
        switch (tier) {
            case "today":
                start = today;
                end = today;
                break;
            case "7d":
                end = today.plusDays(7);
                break;
            case "30d":
                end = today.plusDays(30);
                break;
            default:
                // 全部未处理（含逾期），近一年范围
                end = today.plusYears(1);
                tier = "all";
        }
        List<PetEvent> rows = eventMapper.findPending(end, start);
        model.addAttribute("items", rows);
        model.addAttribute("tier", tier);
        model.addAttribute("today", today);
        return "reminder/list";
    }

    /** 保存免疫记录（建在客户详情页） */
    @PostMapping("/reminders/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam Long petId,
                       @RequestParam String eventType,
                       @RequestParam String happenedOn,
                       @RequestParam(required = false) String nextDueOn,
                       @RequestParam(required = false) String hospital,
                       @RequestParam(required = false) String remark,
                       @RequestParam Long ownerId,
                       RedirectAttributes ra) {
        PetEvent e = id == null ? new PetEvent() : eventMapper.findById(id);
        if (e == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "记录不存在");
        }
        e.setPetId(petId);
        e.setEventType(eventType);
        try {
            e.setHappenedOn(LocalDate.parse(happenedOn.trim()));
            if (nextDueOn != null && !nextDueOn.trim().isEmpty()) {
                e.setNextDueOn(LocalDate.parse(nextDueOn.trim()));
            } else {
                e.setNextDueOn(e.getHappenedOn().plusDays(PetEvent.defaultInterval(eventType)));
            }
        } catch (Exception ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, "日期格式不正确");
        }
        if (e.getNextDueOn().isBefore(e.getHappenedOn())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "下次到期日不能早于发生日期");
        }
        e.setHospital(hospital);
        e.setRemark(remark);
        if (id == null) {
            e.setCreatedAt(LocalDateTime.now());
            eventMapper.insert(e);
        } else {
            eventMapper.update(e);
        }
        ra.addFlashAttribute("okMsg", "免疫记录已保存");
        return "redirect:/customers/" + ownerId;
    }

    /** 联系标记：已电话/已预约/已完成/暂不联系 */
    @PostMapping("/reminders/{id}/mark")
    public String mark(@PathVariable Long id, @RequestParam String status,
                       HttpSession session, RedirectAttributes ra) {
        SessionUser user = (SessionUser) session.getAttribute("sessionUser");
        if (!"CONTACTED".equals(status) && !"BOOKED".equals(status)
                && !"DONE".equals(status) && !"IGNORED".equals(status)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "标记类型不正确");
        }
        eventMapper.markContact(id, status, LocalDateTime.now(), user.getDisplayName());
        ra.addFlashAttribute("okMsg", "已标记");
        return "redirect:/reminders";
    }
}
