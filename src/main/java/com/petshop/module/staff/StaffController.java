package com.petshop.module.staff;

import com.petshop.common.BizException;
import com.petshop.common.ErrorCode;
import com.petshop.common.Money;
import com.petshop.common.SessionUser;
import com.petshop.module.audit.AuditService;
import com.petshop.module.product.ProductMapper;
import com.petshop.module.svc.ServiceItemMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

@Controller
public class StaffController {

    private final CommissionService service;
    private final ServiceItemMapper serviceItemMapper;
    private final ProductMapper productMapper;
    private final AuditService audit;

    public StaffController(CommissionService service, ServiceItemMapper serviceItemMapper,
                           ProductMapper productMapper, AuditService audit) {
        this.service = service;
        this.serviceItemMapper = serviceItemMapper;
        this.productMapper = productMapper;
        this.audit = audit;
    }

    private SessionUser user(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }

    @GetMapping("/staff")
    public String staff(Model model) {
        model.addAttribute("items", service.staffList());
        return "staff/list";
    }

    @PostMapping("/staff/save")
    public String saveStaff(@RequestParam(required = false) Long id,
                            @RequestParam String name,
                            @RequestParam String role,
                            @RequestParam(required = false) String phone,
                            @RequestParam(required = false) String hireOn,
                            @RequestParam(required = false) String leaveOn,
                            RedirectAttributes ra) {
        service.saveStaff(id, name, role, phone, hireOn, leaveOn);
        ra.addFlashAttribute("okMsg", "员工已保存");
        return "redirect:/staff";
    }

    @PostMapping("/staff/{id}/toggle")
    public String toggleStaff(@PathVariable Long id, @RequestParam boolean active,
                              RedirectAttributes ra) {
        service.toggleStaff(id, active);
        ra.addFlashAttribute("okMsg", active ? "员工已启用" : "员工已停用");
        return "redirect:/staff";
    }

    // ==================== 提成规则 ====================

    @GetMapping("/commission-rules")
    public String rules(Model model) {
        model.addAttribute("rules", service.rules());
        model.addAttribute("services", serviceItemMapper.findActive());
        model.addAttribute("products", productMapper.findActive());
        return "staff/rule-list";
    }

    @PostMapping("/commission-rules/save")
    public String saveRule(@RequestParam(required = false) Long id,
                           @RequestParam String scope,
                           @RequestParam(required = false) Long refId,
                           @RequestParam(required = false) String staffRole,
                           @RequestParam(defaultValue = "0") String ratio,
                           @RequestParam(defaultValue = "0") String fixedYuan,
                           @RequestParam String effectiveFrom,
                           @RequestParam(required = false) String effectiveTo,
                           @RequestParam(required = false) String remark,
                           RedirectAttributes ra) {
        int ratioBp;
        try {
            ratioBp = new BigDecimal(ratio == null || ratio.trim().isEmpty() ? "0" : ratio.trim())
                    .movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "提成比例格式不正确");
        }
        int fixedCent = Money.toCent(fixedYuan);
        service.saveRule(id, scope, refId,
                staffRole == null || staffRole.trim().isEmpty() ? null : staffRole.trim(),
                ratioBp, fixedCent, effectiveFrom, effectiveTo, remark);
        ra.addFlashAttribute("okMsg", "提成规则已保存");
        return "redirect:/commission-rules";
    }

    @PostMapping("/commission-rules/{id}/toggle")
    public String toggleRule(@PathVariable Long id, @RequestParam boolean active,
                             RedirectAttributes ra) {
        service.toggleRule(id, active);
        ra.addFlashAttribute("okMsg", active ? "规则已启用" : "规则已停用");
        return "redirect:/commission-rules";
    }

    // ==================== 提成明细 / 结算 ====================

    @GetMapping("/commissions")
    public String commissions(@RequestParam(required = false) Long staffId,
                              @RequestParam(required = false) String from,
                              @RequestParam(required = false) String to,
                              @RequestParam(defaultValue = "1") int page,
                              Model model) {
        LocalDate f = parseDate(from);
        LocalDate t = parseDate(to);
        java.util.Map<String, Object> r = service.records(staffId, f, t, page, 50);
        model.addAttribute("rows", r.get("rows"));
        model.addAttribute("total", r.get("total"));
        model.addAttribute("page", r.get("page"));
        model.addAttribute("pages", r.get("pages"));
        model.addAttribute("staffList", service.staffList());
        model.addAttribute("staffId", staffId);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("settles", service.settles(null));
        model.addAttribute("month", YearMonth.now().toString());
        return "staff/commission";
    }

    @PostMapping("/commissions/settle")
    public String generateSettle(@RequestParam String month, HttpSession session,
                                 RedirectAttributes ra) {
        int n = service.generateSettle(month, user(session).getDisplayName());
        ra.addFlashAttribute("okMsg", "已生成 " + n + " 张提成结算单（草稿），请核对后确认");
        return "redirect:/commissions";
    }

    @PostMapping("/commissions/settle/{id}/confirm")
    public String confirmSettle(@PathVariable Long id, HttpSession session,
                                HttpServletRequest request, RedirectAttributes ra) {
        service.confirmSettle(id, user(session).getDisplayName());
        audit.log("COMMISSION_CONFIRM", "COMMISSION", id, null, "status=CONFIRMED",
                user(session), request.getRemoteAddr());
        ra.addFlashAttribute("okMsg", "提成已确认，该月记录已锁定");
        return "redirect:/commissions";
    }

    // ==================== 业绩 ====================

    @GetMapping("/staff/performance")
    public String performance(@RequestParam(required = false) String month, Model model) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(month == null || month.trim().isEmpty()
                    ? YearMonth.now().toString() : month.trim());
        } catch (Exception e) {
            ym = YearMonth.now();
        }
        model.addAttribute("month", ym.toString());
        model.addAttribute("rows", service.performance(ym.atDay(1), ym.atEndOfMonth()));
        return "staff/performance";
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "日期格式不正确");
        }
    }
}
