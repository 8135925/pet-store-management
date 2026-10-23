package com.petshop.module.card;

import com.petshop.common.SessionUser;
import com.petshop.module.customer.Customer;
import com.petshop.module.customer.CustomerMapper;
import com.petshop.module.svc.ServiceItem;
import com.petshop.module.svc.ServiceItemMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class CardController {

    private final CardService service;
    private final CardTypeMapper typeMapper;
    private final CustomerCardMapper cardMapper;
    private final CardLogMapper logMapper;
    private final CustomerMapper customerMapper;
    private final ServiceItemMapper serviceItemMapper;

    public CardController(CardService service, CardTypeMapper typeMapper, CustomerCardMapper cardMapper,
                          CardLogMapper logMapper, CustomerMapper customerMapper,
                          ServiceItemMapper serviceItemMapper) {
        this.service = service;
        this.typeMapper = typeMapper;
        this.cardMapper = cardMapper;
        this.logMapper = logMapper;
        this.customerMapper = customerMapper;
        this.serviceItemMapper = serviceItemMapper;
    }

    // ---------------- 卡类型维护（ADMIN） ----------------

    @GetMapping("/card-types")
    public String types(Model model) {
        model.addAttribute("types", typeMapper.findAll());
        model.addAttribute("services", serviceItemMapper.findAll());
        return "card/type-list";
    }

    @PostMapping("/card-types/save")
    public String saveType(@RequestParam(required = false) Long id,
                           @RequestParam String name,
                           @RequestParam(required = false) List<Long> serviceIds,
                           @RequestParam int paidQty,
                           @RequestParam(defaultValue = "0") int giftQty,
                           @RequestParam String priceYuan,
                           @RequestParam(defaultValue = "365") int validDays,
                           RedirectAttributes ra) {
        StringBuilder sb = new StringBuilder();
        if (serviceIds != null) {
            for (Long sid : serviceIds) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(sid);
            }
        }
        service.saveType(id, name, sb.toString(), paidQty, giftQty, priceYuan, validDays);
        ra.addFlashAttribute("okMsg", "卡类型已保存");
        return "redirect:/card-types";
    }

    @PostMapping("/card-types/{id}/toggle")
    public String toggleType(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes ra) {
        typeMapper.setActive(id, active);
        ra.addFlashAttribute("okMsg", active ? "卡类型已上架" : "卡类型已停售");
        return "redirect:/card-types";
    }

    // ---------------- 售卡 ----------------

    @GetMapping("/cards/sell")
    public String sellPage(@RequestParam(required = false) Long customerId, Model model) {
        model.addAttribute("customers", customerMapper.listActive());
        model.addAttribute("types", typeMapper.findActive());
        model.addAttribute("services", serviceItemMapper.findActive());
        model.addAttribute("customerId", customerId);
        return "card/sell";
    }

    @PostMapping("/cards/sell")
    public String sell(@RequestParam Long customerId, @RequestParam Long cardTypeId,
                       @RequestParam String receivedYuan, @RequestParam String payMethod,
                       HttpSession session, RedirectAttributes ra) {
        SessionUser user = (SessionUser) session.getAttribute("sessionUser");
        Long cardId = service.sell(customerId, cardTypeId, receivedYuan, payMethod, user);
        ra.addFlashAttribute("okMsg", "售卡成功");
        return "redirect:/cards/" + cardId;
    }

    // ---------------- 卡列表 / 详情 ----------------

    @GetMapping("/cards")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String orderByExpire,
                       Model model) {
        boolean byExpire = "1".equals(orderByExpire);
        model.addAttribute("cards", cardMapper.listBalance(keyword, status, byExpire));
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("orderByExpire", orderByExpire);
        return "card/list";
    }

    @GetMapping("/cards/{id}")
    public String detail(@PathVariable Long id, Model model) {
        CustomerCard c = service.mustFind(id);
        model.addAttribute("card", c);
        model.addAttribute("consumeLogs", logMapper.findConsumeByCard(id));
        model.addAttribute("extendLogs", logMapper.findExtendByCard(id));
        return "card/detail";
    }

    // ---------------- 延期 / 作废（ADMIN） ----------------

    @PostMapping("/cards/{id}/extend")
    public String extend(@PathVariable Long id, @RequestParam String newExpireOn,
                         @RequestParam String reason, HttpSession session, RedirectAttributes ra) {
        service.extend(id, newExpireOn, reason, user(session));
        ra.addFlashAttribute("okMsg", "延期成功");
        return "redirect:/cards/" + id;
    }

    @PostMapping("/cards/{id}/void")
    public String voidCard(@PathVariable Long id, @RequestParam(required = false) String reason,
                           HttpSession session, RedirectAttributes ra) {
        service.voidCard(id, reason, user(session));
        ra.addFlashAttribute("okMsg", "卡已作废");
        return "redirect:/cards/" + id;
    }

    private SessionUser user(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }
}
