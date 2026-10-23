package com.petshop.module.svc;

import com.petshop.common.Money;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/services")
public class ServiceItemController {

    private final ServiceItemMapper mapper;

    public ServiceItemController(ServiceItemMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", mapper.findAll());
        return "svc/list";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name,
                       @RequestParam(defaultValue = "60") int durationMin,
                       @RequestParam(defaultValue = "0") String priceS,
                       @RequestParam(defaultValue = "0") String priceM,
                       @RequestParam(defaultValue = "0") String priceL,
                       @RequestParam(defaultValue = "0") String priceXL,
                       @RequestParam(defaultValue = "0") int sortNo,
                       RedirectAttributes ra) {
        ServiceItem s = id == null ? new ServiceItem() : mapper.findById(id);
        if (s == null) {
            s = new ServiceItem();
        }
        s.setName(name);
        s.setDurationMin(Math.max(1, durationMin));
        s.setPriceSCent(Money.toCent(priceS));
        s.setPriceMCent(Money.toCent(priceM));
        s.setPriceLCent(Money.toCent(priceL));
        s.setPriceXLCent(Money.toCent(priceXL));
        s.setSortNo(sortNo);
        s.setIsActive(true);
        if (id == null) {
            mapper.insert(s);
        } else {
            mapper.update(s);
        }
        ra.addFlashAttribute("okMsg", "服务项目已保存");
        return "redirect:/services";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes ra) {
        mapper.setActive(id, active);
        ra.addFlashAttribute("okMsg", active ? "服务已上架" : "服务已下架");
        return "redirect:/services";
    }
}
