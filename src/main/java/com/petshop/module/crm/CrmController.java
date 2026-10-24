package com.petshop.module.crm;

import com.petshop.common.SessionUser;
import com.petshop.module.customer.Customer;
import com.petshop.module.customer.CustomerMapper;
import com.petshop.module.customer.Pet;
import com.petshop.module.customer.PetMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class CrmController {

    private final CrmService service;
    private final CustomerMapper customerMapper;
    private final PetMapper petMapper;

    public CrmController(CrmService service, CustomerMapper customerMapper, PetMapper petMapper) {
        this.service = service;
        this.customerMapper = customerMapper;
        this.petMapper = petMapper;
    }

    private SessionUser user(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }

    @GetMapping("/follow-ups")
    public String followUps(Model model) {
        model.addAttribute("tasks", service.pending());
        model.addAttribute("pendingCount", service.pendingCount());
        return "crm/follow-ups";
    }

    @PostMapping("/follow-ups/{id}/done")
    public String done(@PathVariable Long id,
                       @RequestParam(defaultValue = "PHONE") String channel,
                       @RequestParam(defaultValue = "CONNECTED") String outcome,
                       @RequestParam String content,
                       @RequestParam(required = false) String nextFollowOn,
                       HttpSession session, RedirectAttributes ra) {
        service.completeTask(id, channel, outcome, content, nextFollowOn, user(session));
        ra.addFlashAttribute("okMsg", "已完成回访，触达记录已保存");
        return "redirect:/follow-ups";
    }

    @PostMapping("/follow-ups/{id}/skip")
    public String skip(@PathVariable Long id, @RequestParam(required = false) String reason,
                       HttpSession session, RedirectAttributes ra) {
        service.skipTask(id, reason, user(session));
        ra.addFlashAttribute("okMsg", "已跳过该待办");
        return "redirect:/follow-ups";
    }

    @PostMapping("/follow-ups/{id}/postpone")
    public String postpone(@PathVariable Long id, @RequestParam String dueOn,
                           RedirectAttributes ra) {
        service.postponeTask(id, dueOn);
        ra.addFlashAttribute("okMsg", "已改期");
        return "redirect:/follow-ups";
    }

    @PostMapping("/follow-ups/manual")
    public String manual(@RequestParam Long customerId,
                         @RequestParam(required = false) Long petId,
                         @RequestParam String dueOn,
                         @RequestParam(required = false) String note,
                         HttpSession session, RedirectAttributes ra) {
        service.createManual(customerId, petId, dueOn, note, user(session));
        ra.addFlashAttribute("okMsg", "手工待办已创建");
        return "redirect:/follow-ups";
    }

    @PostMapping("/follow-ups/generate")
    public String generate(RedirectAttributes ra) {
        int n = service.generateDaily();
        ra.addFlashAttribute("okMsg", "已生成 " + n + " 条待办");
        return "redirect:/follow-ups";
    }

    @GetMapping("/recall")
    public String recall(Model model) {
        model.addAttribute("rows", service.recallList());
        return "crm/recall";
    }

    @PostMapping("/recall/generate")
    public String generateRecall(HttpSession session, RedirectAttributes ra) {
        int n = service.generateRecallTasks(user(session));
        ra.addFlashAttribute("okMsg", "已生成 " + n + " 条召回任务");
        return "redirect:/follow-ups";
    }

    @GetMapping("/customers/{id}/360")
    public String customer360(@PathVariable Long id, Model model) {
        Customer c = customerMapper.findById(id);
        model.addAttribute("customer", c);
        model.addAttribute("d", service.customer360(id));
        model.addAttribute("petTagDict", TagLabels.PET_TAGS);
        model.addAttribute("customerTagDict", TagLabels.CUSTOMER_TAGS);
        return "crm/customer360";
    }

    @PostMapping("/customers/{id}/tags")
    public String addCustomerTag(@PathVariable Long id,
                                 @RequestParam String tagCode,
                                 @RequestParam(required = false) String note) {
        service.addCustomerTag(id, tagCode, note);
        return "redirect:/customers/" + id + "/360";
    }

    @PostMapping("/customers/{id}/tags/remove")
    public String removeCustomerTag(@PathVariable Long id, @RequestParam String tagCode) {
        service.removeCustomerTag(id, tagCode);
        return "redirect:/customers/" + id + "/360";
    }

    @PostMapping("/pets/{id}/tags")
    public String addPetTag(@PathVariable Long id,
                            @RequestParam String tagCode,
                            @RequestParam(required = false) String note,
                            @RequestParam Long customerId) {
        service.addPetTag(id, tagCode, note);
        return "redirect:/customers/" + customerId + "/360";
    }

    @PostMapping("/pets/{id}/tags/remove")
    public String removePetTag(@PathVariable Long id, @RequestParam String tagCode,
                               @RequestParam Long customerId) {
        service.removePetTag(id, tagCode);
        return "redirect:/customers/" + customerId + "/360";
    }

    /** 客户详情页直接展示待办（预留） */
    @GetMapping("/customers/{id}/pets")
    public String pets(@PathVariable Long id, Model model) {
        List<Pet> pets = petMapper.findByOwner(id);
        model.addAttribute("pets", pets);
        return "redirect:/customers/" + id + "/360";
    }
}
