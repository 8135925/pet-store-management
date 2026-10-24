package com.petshop.module.customer;

import com.petshop.common.PageResult;
import com.petshop.module.bill.Bill;
import com.petshop.module.bill.BillMapper;
import com.petshop.module.card.CustomerCard;
import com.petshop.module.card.CustomerCardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CustomerController {

    private final CustomerService service;
    private final CustomerCardMapper cardMapper;
    private final BillMapper billMapper;

    public CustomerController(CustomerService service, CustomerCardMapper cardMapper, BillMapper billMapper) {
        this.service = service;
        this.cardMapper = cardMapper;
        this.billMapper = billMapper;
    }

    @GetMapping("/customers")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Boolean includeInactive,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        PageResult<Customer> pr = service.search(keyword, Boolean.TRUE.equals(includeInactive), page, 20);
        model.addAttribute("pr", pr);
        model.addAttribute("keyword", keyword);
        model.addAttribute("includeInactive", Boolean.TRUE.equals(includeInactive));
        return "customer/list";
    }

    @GetMapping("/customers/new")
    public String form(Model model) {
        model.addAttribute("customer", new Customer());
        return "customer/form";
    }

    @GetMapping("/customers/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("customer", service.findById(id));
        return "customer/form";
    }

    @PostMapping("/customers/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name, @RequestParam String phone,
                       @RequestParam(required = false) String source,
                       @RequestParam(required = false) String remark,
                       RedirectAttributes ra) {
        if (id == null) {
            id = service.createCustomer(name, phone, source, remark);
            ra.addFlashAttribute("okMsg", "客户建档成功");
        } else {
            service.updateCustomer(id, name, phone, source, remark);
            ra.addFlashAttribute("okMsg", "客户资料已更新");
        }
        return "redirect:/customers/" + id;
    }

    @PostMapping("/customers/{id}/toggle")
    public String toggle(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes ra) {
        service.toggleCustomer(id, active);
        ra.addFlashAttribute("okMsg", active ? "客户已恢复" : "客户已停用");
        return "redirect:/customers/" + id;
    }

    /** 聚合详情页：宠物 / 持卡 / 近 10 单 / 待办提醒 */
    @GetMapping("/customers/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Customer c = service.findById(id);
        List<Pet> pets = service.petsOf(id);
        List<CustomerCard> cards = cardMapper.findByCustomer(id);
        List<Bill> bills = billMapper.findRecentByCustomer(id, 10);
        model.addAttribute("customer", c);
        model.addAttribute("pets", pets);
        model.addAttribute("cards", cards);
        model.addAttribute("bills", bills);
        model.addAttribute("reminders", service.remindersOfCustomer(id));
        return "customer/detail";
    }

    // ---------------- 宠物 ----------------

    @PostMapping("/pets/save")
    public String savePet(@RequestParam(required = false) Long id,
                          @RequestParam Long ownerId,
                          @RequestParam String name,
                          @RequestParam(defaultValue = "DOG") String species,
                          @RequestParam(required = false) String breed,
                          @RequestParam(defaultValue = "UNKNOWN") String gender,
                          @RequestParam(required = false) String birthday,
                          @RequestParam(required = false) String weightKg,
                          @RequestParam(defaultValue = "false") boolean neutered,
                          @RequestParam(required = false) String color,
                          @RequestParam(required = false) String habit,
                          RedirectAttributes ra) {
        service.savePet(id, ownerId, name, species, breed, gender, birthday, weightKg, neutered, color, habit);
        ra.addFlashAttribute("okMsg", id == null ? "宠物已建档" : "宠物资料已更新");
        return "redirect:/customers/" + ownerId;
    }

    @PostMapping("/pets/{id}/toggle")
    public String togglePet(@PathVariable Long id, @RequestParam boolean active,
                            @RequestParam Long ownerId, RedirectAttributes ra) {
        service.togglePet(id, active);
        ra.addFlashAttribute("okMsg", active ? "宠物已恢复在册" : "宠物已停用");
        return "redirect:/customers/" + ownerId;
    }

    @GetMapping("/pets")
    public String petList(@RequestParam(required = false) String keyword,
                          @RequestParam(defaultValue = "1") int page, Model model) {
        int p = Math.max(page, 1);
        String kw = keyword == null ? "" : keyword.trim();
        int size = 20;
        long total = petMapper.countSearch(kw);
        long offset = (long) (p - 1) * size;
        if (offset > Integer.MAX_VALUE) {
            offset = Integer.MAX_VALUE;
        }
        List<Pet> rows = total == 0
                ? java.util.Collections.<Pet>emptyList()
                : petMapper.search(kw, (int) offset, size);
        model.addAttribute("pr", new PageResult<>(rows, total, p, size));
        model.addAttribute("keyword", keyword);
        return "pet/list";
    }

    @Autowired
    private PetMapper petMapper;
}
