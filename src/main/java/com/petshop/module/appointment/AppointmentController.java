package com.petshop.module.appointment;

import com.petshop.common.SessionUser;
import com.petshop.module.customer.CustomerMapper;
import com.petshop.module.customer.Pet;
import com.petshop.module.customer.PetMapper;
import com.petshop.module.svc.ServiceItemMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;

@Controller
public class AppointmentController {

    private final AppointmentService service;
    private final CustomerMapper customerMapper;
    private final PetMapper petMapper;
    private final ServiceItemMapper serviceItemMapper;

    public AppointmentController(AppointmentService service, CustomerMapper customerMapper,
                                 PetMapper petMapper, ServiceItemMapper serviceItemMapper) {
        this.service = service;
        this.customerMapper = customerMapper;
        this.petMapper = petMapper;
        this.serviceItemMapper = serviceItemMapper;
    }

    /** 预约看板（按日） */
    @GetMapping("/appointments")
    public String board(@RequestParam(required = false) String date, Model model) {
        String d = date == null || date.trim().isEmpty() ? LocalDate.now().toString() : date.trim();
        model.addAttribute("date", d);
        model.addAttribute("items", service.byDate(d));
        model.addAttribute("stale", service.stale());
        return "appointment/board";
    }

    @GetMapping("/appointments/new")
    public String createPage(@RequestParam(required = false) Long customerId, Model model) {
        model.addAttribute("customers", customerMapper.listActive());
        model.addAttribute("services", serviceItemMapper.findActive());
        model.addAttribute("customerId", customerId);
        if (customerId != null) {
            model.addAttribute("pets", petMapper.findByOwner(customerId));
        }
        return "appointment/form";
    }

    @PostMapping("/appointments/save")
    public String save(@RequestParam Long customerId, @RequestParam Long petId,
                       @RequestParam Long serviceId, @RequestParam String appointDate,
                       @RequestParam String slot, @RequestParam(required = false) String remark,
                       @RequestParam(required = false) boolean confirmBoarding,
                       HttpSession session, RedirectAttributes ra) {
        SessionUser user = (SessionUser) session.getAttribute("sessionUser");
        // BR-13：在店寄养强提示（前端已有二次确认，服务端仅记录备注）
        if (service.petInBoarding(petId) && !confirmBoarding) {
            ra.addFlashAttribute("errMsg", "该宠物正在寄养中，如确认要建到店预约请勾选「确认」后重试");
            return "redirect:/appointments/new?customerId=" + customerId;
        }
        String finalRemark = service.petInBoarding(petId) ? "（该宠物在店寄养中）" + (remark == null ? "" : remark) : remark;
        Long id = service.create(customerId, petId, serviceId, appointDate, slot, finalRemark, user);
        ra.addFlashAttribute("okMsg", "预约已创建");
        return "redirect:/appointments?date=" + LocalDate.parse(appointDate);
    }

    @PostMapping("/appointments/{id}/arrive")
    public String arrive(@PathVariable Long id, RedirectAttributes ra) {
        service.arrive(id);
        ra.addFlashAttribute("okMsg", "已标记到店，可前往开单");
        return "redirect:/appointments";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        service.cancel(id);
        ra.addFlashAttribute("okMsg", "预约已取消");
        return "redirect:/appointments";
    }

    @PostMapping("/appointments/{id}/noshow")
    public String noshow(@PathVariable Long id, RedirectAttributes ra) {
        service.noshow(id);
        ra.addFlashAttribute("okMsg", "已标记未到");
        return "redirect:/appointments";
    }
}
