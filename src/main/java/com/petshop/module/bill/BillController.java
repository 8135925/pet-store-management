package com.petshop.module.bill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petshop.common.SessionUser;
import com.petshop.module.customer.Customer;
import com.petshop.module.customer.CustomerMapper;
import com.petshop.module.customer.Pet;
import com.petshop.module.customer.PetMapper;
import com.petshop.module.card.CustomerCard;
import com.petshop.module.card.CustomerCardMapper;
import com.petshop.module.card.CardTypeMapper;
import com.petshop.module.product.ProductMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class BillController {

    private final BillService service;
    private final CustomerMapper customerMapper;
    private final PetMapper petMapper;
    private final CustomerCardMapper cardMapper;
    private final CardTypeMapper cardTypeMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;
    private final com.petshop.module.appointment.AppointmentService appointmentService;

    public BillController(BillService service, CustomerMapper customerMapper, PetMapper petMapper,
                          CustomerCardMapper cardMapper, CardTypeMapper cardTypeMapper,
                          ServiceItemMapper serviceItemMapper,
                          ProductMapper productMapper, ObjectMapper objectMapper,
                          com.petshop.module.appointment.AppointmentService appointmentService) {
        this.service = service;
        this.customerMapper = customerMapper;
        this.petMapper = petMapper;
        this.cardMapper = cardMapper;
        this.cardTypeMapper = cardTypeMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.productMapper = productMapper;
        this.objectMapper = objectMapper;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/bills")
    public String list(@RequestParam(required = false) String dateFrom,
                       @RequestParam(required = false) String dateTo,
                       @RequestParam(required = false) Long customerId,
                       @RequestParam(required = false) Long petId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        LocalDate from = parseDate(dateFrom);
        LocalDate to = parseDate(dateTo);
        Map<String, Object> r = service.search(from, to, customerId, petId, keyword, status, page, 20);
        model.addAttribute("pr", r.get("pr"));
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("customerId", customerId);
        model.addAttribute("petId", petId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("pets", petMapper.listActiveWithOwner()); // v1.1 缺口②：宠物筛选
        return "bill/list";
    }

    @GetMapping("/bills/new")
    public String newBill(@RequestParam(required = false) Long customerId,
                          @RequestParam(required = false) Long appointmentId,
                          Model model, HttpSession session) {
        if (customerId == null) {
            // 未带客户参数（如侧边栏入口）：让用户先选客户
            model.addAttribute("keyword", "");
            model.addAttribute("customers", customerMapper.listActive());
            return "bill/pick-customer";
        }
        Customer c = customerMapper.findById(customerId);
        if (c == null) {
            throw new com.petshop.common.BizException(com.petshop.common.ErrorCode.PARAM_INVALID, "客户不存在");
        }
        // 每次打开结账页生成新的幂等 token
        String requestId = UUID.randomUUID().toString().replace("-", "");
        session.setAttribute("billRequestId", requestId);

        Map<String, Object> data = buildData(c);
        try {
            model.addAttribute("dataJson", objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        model.addAttribute("data", data);
        model.addAttribute("requestId", requestId);
        model.addAttribute("customer", c);
        // v1.1：预约到店转开单——预填服务明细
        if (appointmentId != null) {
            com.petshop.module.appointment.Appointment appt = appointmentService.detail(appointmentId);
            if (!"ARRIVED".equals(appt.getStatus())) {
                throw new com.petshop.common.BizException(com.petshop.common.ErrorCode.APPOINTMENT_STATE_INVALID,
                        "该预约不是「已到店」状态，不能转开单");
            }
            if (!customerId.equals(appt.getCustomerId())) {
                throw new com.petshop.common.BizException(com.petshop.common.ErrorCode.PARAM_INVALID,
                        "预约客户与开单客户不一致");
            }
            model.addAttribute("appointmentId", appointmentId);
            model.addAttribute("appointment", appt);
        }
        return "bill/new";
    }

    /** 开单页数据（给 Thymeleaf 与前端 JS 共用） */
    private Map<String, Object> buildData(Customer c) {
        Map<String, Object> data = new HashMap<>();
        data.put("customer", c);

        List<Map<String, Object>> pets = new java.util.ArrayList<>();
        for (Pet p : petMapper.findByOwner(c.getId())) {
            if (!p.getIsActive()) {
                continue;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("species", p.getSpecies());
            m.put("breed", p.getBreed());
            m.put("weightG", p.getWeightG());
            m.put("isActive", true);
            pets.add(m);
        }
        data.put("pets", pets);

        List<Map<String, Object>> svcs = new java.util.ArrayList<>();
        for (com.petshop.module.svc.ServiceItem s : serviceItemMapper.findActive()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("priceSCent", s.getPriceSCent());
            m.put("priceMCent", s.getPriceMCent());
            m.put("priceLCent", s.getPriceLCent());
            m.put("priceXLCent", s.getPriceXLCent());
            svcs.add(m);
        }
        data.put("services", svcs);

        List<Map<String, Object>> prods = new java.util.ArrayList<>();
        for (com.petshop.module.product.Product p : productMapper.findActive()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("barcode", p.getBarcode());
            m.put("salePriceCent", p.getSalePriceCent());
            m.put("stockQty", p.getStockQty());
            prods.add(m);
        }
        data.put("products", prods);

        List<Map<String, Object>> cards = new java.util.ArrayList<>();
        for (CustomerCard card : cardMapper.findUsableByCustomer(c.getId(), LocalDate.now())) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", card.getId());
            m.put("cardNo", card.getCardNo());
            m.put("paidQty", card.getPaidQty());
            m.put("giftQty", card.getGiftQty());
            m.put("usedPaid", card.getUsedPaid());
            m.put("usedGift", card.getUsedGift());
            m.put("serviceIds", card.getCardTypeId() == null ? ""
                    : cardTypeServiceIds(card.getCardTypeId()));
            cards.add(m);
        }
        data.put("cards", cards);
        return data;
    }

    private String cardTypeServiceIds(Long cardTypeId) {
        com.petshop.module.card.CardType t = cardTypeMapper.findById(cardTypeId);
        return t == null ? "" : t.getServiceIds();
    }

    /** 提交结账：itemsJson 由前端组装 */
    @PostMapping("/bills/settle")
    public String settle(@RequestParam Long customerId,
                         @RequestParam(required = false) Long petId,
                         @RequestParam String itemsJson,
                         @RequestParam(defaultValue = "0") String discountYuan,
                         @RequestParam String payMethod,
                         @RequestParam(required = false) String remark,
                         @RequestParam(required = false) String requestId,
                         @RequestParam(required = false) Long appointmentId,
                         HttpSession session, RedirectAttributes ra) {
        SessionUser user = (SessionUser) session.getAttribute("sessionUser");
        // 幂等键优先取表单隐藏域（双击重复提交时两次请求携带同一 token），回退到会话 token
        String token = (requestId == null || requestId.trim().isEmpty())
                ? (String) session.getAttribute("billRequestId") : requestId.trim();
        session.removeAttribute("billRequestId");

        BillForm form = new BillForm();
        try {
            List<BillForm.Item> items = objectMapper.readValue(itemsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, BillForm.Item.class));
            form.setItems(items);
        } catch (Exception e) {
            throw new com.petshop.common.BizException(com.petshop.common.ErrorCode.PARAM_INVALID, "明细数据格式不正确");
        }
        form.setCustomerId(customerId);
        form.setPetId(petId);
        form.setDiscountCent(com.petshop.common.Money.toCent(discountYuan));
        form.setPayMethod(payMethod);
        form.setRemark(remark);
        form.setRequestId(token);
        form.setAppointmentId(appointmentId);

        Long billId = service.settle(form, user);
        ra.addFlashAttribute("okMsg", appointmentId != null ? "结账成功，预约已完成" : "结账成功");
        return "redirect:/bills/" + billId;
    }

    @GetMapping("/bills/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("bill", service.detail(id));
        return "bill/detail";
    }

    /** 反结账（仅 ADMIN，拦截器已拦，服务端再校验） */
    @PostMapping("/bills/{id}/cancel")
    public String cancel(@PathVariable Long id, @RequestParam String reason,
                         HttpSession session, RedirectAttributes ra) {
        SessionUser user = (SessionUser) session.getAttribute("sessionUser");
        service.cancelSettle(id, reason, user);
        ra.addFlashAttribute("okMsg", "已反结账，次数与库存已回滚");
        return "redirect:/bills/" + id;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            throw new com.petshop.common.BizException(
                    com.petshop.common.ErrorCode.PARAM_INVALID, "日期格式不正确：" + s);
        }
    }
}
