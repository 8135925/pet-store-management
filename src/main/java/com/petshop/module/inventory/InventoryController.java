package com.petshop.module.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petshop.common.BizException;
import com.petshop.common.ErrorCode;
import com.petshop.common.Money;
import com.petshop.common.SessionUser;
import com.petshop.module.audit.AuditService;
import com.petshop.module.product.ProductMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
public class InventoryController {

    private final InventoryService service;
    private final ProductMapper productMapper;
    private final AuditService audit;
    private final ObjectMapper objectMapper;

    public InventoryController(InventoryService service, ProductMapper productMapper,
                               AuditService audit, ObjectMapper objectMapper) {
        this.service = service;
        this.productMapper = productMapper;
        this.audit = audit;
        this.objectMapper = objectMapper;
    }

    private SessionUser user(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }

    // ==================== 供应商 ====================

    @GetMapping("/suppliers")
    public String suppliers(Model model) {
        model.addAttribute("items", service.suppliers());
        return "inventory/supplier-list";
    }

    @PostMapping("/suppliers/save")
    public String saveSupplier(@RequestParam(required = false) Long id,
                               @RequestParam String name,
                               @RequestParam(required = false) String contact,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String address,
                               @RequestParam(required = false) String mainCategory,
                               @RequestParam(required = false) String remark,
                               HttpSession session, HttpServletRequest request,
                               RedirectAttributes ra) {
        Long sid = service.saveSupplier(id, name, contact, phone, address, mainCategory, remark);
        audit.log(id == null ? "SUPPLIER_CREATE" : "SUPPLIER_UPDATE", "SUPPLIER", sid,
                null, "name=" + name, user(session), request.getRemoteAddr());
        ra.addFlashAttribute("okMsg", "供应商已保存");
        return "redirect:/suppliers";
    }

    @PostMapping("/suppliers/{id}/toggle")
    public String toggleSupplier(@PathVariable Long id, @RequestParam boolean active,
                                 RedirectAttributes ra) {
        service.toggleSupplier(id, active);
        ra.addFlashAttribute("okMsg", active ? "供应商已启用" : "供应商已停用");
        return "redirect:/suppliers";
    }

    // ==================== 采购单 ====================

    @GetMapping("/purchases")
    public String purchases(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String from,
                            @RequestParam(required = false) String to,
                            @RequestParam(defaultValue = "1") int page,
                            Model model) {
        java.util.Map<String, Object> r = service.searchPurchases(keyword, status, from, to, page, 20);
        model.addAttribute("rows", r.get("rows"));
        model.addAttribute("total", r.get("total"));
        model.addAttribute("page", r.get("page"));
        model.addAttribute("pages", r.get("pages"));
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "inventory/purchase-list";
    }

    @GetMapping("/purchases/new")
    public String newPurchase(Model model) {
        model.addAttribute("suppliers", service.activeSuppliers());
        model.addAttribute("products", productMapper.findActive());
        model.addAttribute("purchase", null);
        model.addAttribute("itemsJson", "[]");
        return "inventory/purchase-form";
    }

    @GetMapping("/purchases/{id}/edit")
    public String editPurchase(@PathVariable Long id, Model model) {
        PurchaseOrder p = service.purchase(id);
        if (!"DRAFT".equals(p.getStatus())) {
            throw new BizException(ErrorCode.PURCHASE_ALREADY_RECEIVED, "已入库的采购单不能修改");
        }
        model.addAttribute("suppliers", service.activeSuppliers());
        model.addAttribute("products", productMapper.findActive());
        model.addAttribute("purchase", p);
        try {
            model.addAttribute("itemsJson", objectMapper.writeValueAsString(p.getItems()));
        } catch (Exception e) {
            model.addAttribute("itemsJson", "[]");
        }
        return "inventory/purchase-form";
    }

    @PostMapping("/purchases/save")
    public String savePurchase(@RequestParam(required = false) Long id,
                               @RequestParam Long supplierId,
                               @RequestParam(required = false) String remark,
                               @RequestParam String itemsJson,
                               HttpSession session, RedirectAttributes ra) {
        List<PurchaseItem> items;
        try {
            items = objectMapper.readValue(itemsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PurchaseItem.class));
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "采购明细格式不正确");
        }
        Long pid = service.savePurchase(id, supplierId, remark, items, user(session).getDisplayName());
        ra.addFlashAttribute("okMsg", "采购单已保存（草稿）");
        return "redirect:/purchases/" + pid;
    }

    @GetMapping("/purchases/{id}")
    public String purchaseDetail(@PathVariable Long id, Model model) {
        model.addAttribute("purchase", service.purchase(id));
        return "inventory/purchase-detail";
    }

    @PostMapping("/purchases/{id}/receive")
    public String receive(@PathVariable Long id, HttpSession session,
                          HttpServletRequest request, RedirectAttributes ra) {
        service.receive(id, user(session).getDisplayName());
        audit.log("PURCHASE_RECEIVE", "PURCHASE", id, null, "status=RECEIVED",
                user(session), request.getRemoteAddr());
        ra.addFlashAttribute("okMsg", "入库完成，批次与库存已更新");
        return "redirect:/purchases/" + id;
    }

    @PostMapping("/purchases/{id}/cancel")
    public String cancelPurchase(@PathVariable Long id, HttpSession session,
                                 HttpServletRequest request, RedirectAttributes ra) {
        service.cancelPurchase(id, user(session).getDisplayName());
        audit.log("PURCHASE_CANCEL", "PURCHASE", id, null, "status=CANCELLED",
                user(session), request.getRemoteAddr());
        ra.addFlashAttribute("okMsg", "采购单已作废");
        return "redirect:/purchases";
    }

    // ==================== 批次 ====================

    @GetMapping("/batches")
    public String batches(@RequestParam(required = false) Long productId,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) Integer expiring,
                          Model model) {
        model.addAttribute("rows", service.batches(productId, status, expiring));
        model.addAttribute("products", productMapper.findAll());
        model.addAttribute("productId", productId);
        model.addAttribute("status", status);
        model.addAttribute("expiring", expiring);
        return "inventory/batch-list";
    }

    @PostMapping("/batches/{id}/void")
    public String voidBatch(@PathVariable Long id,
                            @RequestParam int qty,
                            @RequestParam String reason,
                            HttpSession session, HttpServletRequest request,
                            RedirectAttributes ra) {
        service.voidBatch(id, qty, reason, user(session).getDisplayName());
        audit.log("BATCH_VOID", "BATCH", id, null, "qty=" + qty + ",reason=" + reason,
                user(session), request.getRemoteAddr());
        ra.addFlashAttribute("okMsg", "批次已报损，库存与流水已同步");
        return "redirect:/batches";
    }

    // ==================== 盘点 ====================

    @GetMapping("/stock-takes")
    public String stockTakes(@RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "1") int page,
                             Model model) {
        java.util.Map<String, Object> r = service.searchStockTakes(status, page, 20);
        model.addAttribute("rows", r.get("rows"));
        model.addAttribute("total", r.get("total"));
        model.addAttribute("page", r.get("page"));
        model.addAttribute("pages", r.get("pages"));
        model.addAttribute("status", status);
        model.addAttribute("products", productMapper.findAll());
        return "inventory/take-list";
    }

    @PostMapping("/stock-takes")
    public String createStockTake(@RequestParam(defaultValue = "ALL") String scope,
                                  @RequestParam(required = false) String scopeValue,
                                  @RequestParam(required = false) String remark,
                                  HttpSession session, RedirectAttributes ra) {
        Long id = service.createStockTake(scope, scopeValue, user(session).getDisplayName(), remark);
        ra.addFlashAttribute("okMsg", "盘点单已创建，请录入实盘数量");
        return "redirect:/stock-takes/" + id;
    }

    @GetMapping("/stock-takes/{id}")
    public String stockTakeDetail(@PathVariable Long id, Model model) {
        model.addAttribute("take", service.stockTake(id));
        model.addAttribute("categories", categories());
        return "inventory/take-detail";
    }

    @PostMapping("/stock-takes/{id}/save")
    public String saveCounts(@PathVariable Long id,
                             @RequestParam java.util.Map<String, String> params,
                             RedirectAttributes ra) {
        java.util.Map<Long, String> counted = new java.util.HashMap<Long, String>();
        java.util.Map<Long, String> reasons = new java.util.HashMap<Long, String>();
        for (java.util.Map.Entry<String, String> e : params.entrySet()) {
            if (e.getKey().startsWith("counted_")) {
                counted.put(Long.parseLong(e.getKey().substring("counted_".length())), e.getValue());
            } else if (e.getKey().startsWith("reason_")) {
                reasons.put(Long.parseLong(e.getKey().substring("reason_".length())), e.getValue());
            }
        }
        service.saveCounts(id, counted, reasons);
        ra.addFlashAttribute("okMsg", "实盘数量已暂存");
        return "redirect:/stock-takes/" + id;
    }

    @PostMapping("/stock-takes/{id}/confirm")
    public String confirmStockTake(@PathVariable Long id, HttpSession session,
                                   HttpServletRequest request, RedirectAttributes ra) {
        service.confirmStockTake(id, user(session).getDisplayName());
        audit.log("STOCK_TAKE_CONFIRM", "STOCK_TAKE", id, null, "status=CONFIRMED",
                user(session), request.getRemoteAddr());
        ra.addFlashAttribute("okMsg", "盘点已确认，差异已生成调整台账");
        return "redirect:/stock-takes/" + id;
    }

    @PostMapping("/stock-takes/{id}/cancel")
    public String cancelStockTake(@PathVariable Long id, HttpSession session,
                                  HttpServletRequest request, RedirectAttributes ra) {
        service.cancelStockTake(id);
        audit.log("STOCK_TAKE_CANCEL", "STOCK_TAKE", id, null, "status=CANCELLED",
                user(session), request.getRemoteAddr());
        ra.addFlashAttribute("okMsg", "盘点单已作废");
        return "redirect:/stock-takes";
    }

    private List<String[]> categories() {
        List<String[]> list = new ArrayList<String[]>();
        list.add(new String[]{"MAIN_FOOD", "主粮"});
        list.add(new String[]{"SNACK", "零食"});
        list.add(new String[]{"SUPPLIES", "用品"});
        list.add(new String[]{"HEALTH", "保健"});
        list.add(new String[]{"OTHER", "其他"});
        return list;
    }

    /** 金额展示辅助（模板用） */
    public String yuan(int cent) {
        return Money.yuan(cent);
    }
}
