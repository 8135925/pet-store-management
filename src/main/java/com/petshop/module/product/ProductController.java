package com.petshop.module.product;

import com.petshop.common.SessionUser;
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
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public String list(@RequestParam(required = false) String view, Model model) {
        model.addAttribute("items", service.findAll());
        model.addAttribute("view", view);
        model.addAttribute("expiringCount", service.expiring().size());
        model.addAttribute("lowStockCount", service.lowStock().size());
        if ("expiring".equals(view)) {
            model.addAttribute("filtered", service.expiring());
        } else if ("lowstock".equals(view)) {
            model.addAttribute("filtered", service.lowStock());
        }
        return "product/list";
    }

    @GetMapping("/products/new")
    public String form(Model model) {
        model.addAttribute("product", new Product());
        return "product/form";
    }

    @GetMapping("/products/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("product", service.findById(id));
        return "product/form";
    }

    @PostMapping("/products/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name,
                       @RequestParam(required = false) String barcode,
                       @RequestParam(defaultValue = "0") String salePrice,
                       @RequestParam(defaultValue = "0") String costPrice,
                       @RequestParam(defaultValue = "0") int safetyQty,
                       @RequestParam(required = false) String expireOn,
                       RedirectAttributes ra) {
        service.save(id, name, barcode, salePrice, costPrice, safetyQty, expireOn);
        ra.addFlashAttribute("okMsg", "商品已保存");
        return "redirect:/products";
    }

    @PostMapping("/products/{id}/toggle")
    public String toggle(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes ra) {
        service.setActive(id, active);
        ra.addFlashAttribute("okMsg", active ? "商品已恢复在售" : "商品已停售");
        return "redirect:/products";
    }

    /** 手工出入库（采购入库/退货入库/盘点调整） */
    @PostMapping("/products/{id}/stock")
    public String stock(@PathVariable Long id,
                        @RequestParam String bizType,
                        @RequestParam int qty,
                        @RequestParam(required = false) String remark,
                        HttpSession session, RedirectAttributes ra) {
        SessionUser user = (SessionUser) session.getAttribute("sessionUser");
        service.adjust(id, bizType, qty, remark, user.getDisplayName());
        ra.addFlashAttribute("okMsg", "库存已更新");
        return "redirect:/products/" + id + "/ledger";
    }

    @GetMapping("/products/{id}/ledger")
    public String ledger(@PathVariable Long id, Model model) {
        Product p = service.findById(id);
        model.addAttribute("product", p);
        model.addAttribute("logs", service.ledgerOf(id));
        return "product/ledger";
    }
}
