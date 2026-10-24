package com.petshop.module.report;

import com.petshop.common.BusinessDate;
import com.petshop.module.appointment.Appointment;
import com.petshop.module.appointment.AppointmentMapper;
import com.petshop.module.bill.Bill;
import com.petshop.module.bill.BillMapper;
import com.petshop.module.boarding.BoardingOrder;
import com.petshop.module.boarding.BoardingOrderMapper;
import com.petshop.module.product.Product;
import com.petshop.module.product.ProductService;
import com.petshop.module.reminder.PetEvent;
import com.petshop.module.reminder.PetEventMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReportController {

    private final ReportService reportService;
    private final BillMapper billMapper;
    private final PetEventMapper eventMapper;
    private final ProductService productService;
    private final BoardingOrderMapper boardingOrderMapper;
    private final AppointmentMapper appointmentMapper;
    private final MonthlyReportMapper monthlyMapper;

    public ReportController(ReportService reportService, BillMapper billMapper,
                            PetEventMapper eventMapper, ProductService productService,
                            BoardingOrderMapper boardingOrderMapper,
                            AppointmentMapper appointmentMapper, MonthlyReportMapper monthlyMapper) {
        this.reportService = reportService;
        this.billMapper = billMapper;
        this.eventMapper = eventMapper;
        this.productService = productService;
        this.boardingOrderMapper = boardingOrderMapper;
        this.appointmentMapper = appointmentMapper;
        this.monthlyMapper = monthlyMapper;
    }

    /** 首页看板：v1.0 六数字 + v1.1 在店数 / 今日预约数 */
    @GetMapping("/")
    public String dashboard(Model model) {
        Map<String, Object> d = reportService.dashboard();
        model.addAttribute("d", d);
        LocalDate today = LocalDate.now();
        // 待办提醒（前 8 条）
        List<PetEvent> reminders = eventMapper.findPending(today.plusDays(30), null);
        if (reminders.size() > 8) {
            reminders = reminders.subList(0, 8);
        }
        model.addAttribute("reminders", reminders);
        model.addAttribute("today", today);
        // v1.1：在店宠物 / 今日预约
        List<BoardingOrder> inHouse = boardingOrderMapper.findInHouse();
        model.addAttribute("inHouse", inHouse);
        model.addAttribute("inHouseCount", inHouse.size());
        model.addAttribute("todayAppointments", appointmentMapper.findByDate(today.toString()));
        return "dashboard";
    }

    /** 日营业汇总（三类收入分列） */
    @GetMapping("/reports/daily")
    public String daily(@RequestParam(required = false) String date, Model model) {
        LocalDate d = BusinessDate.today();
        if (date != null && !date.trim().isEmpty()) {
            try {
                d = LocalDate.parse(date.trim());
            } catch (Exception e) {
                throw new com.petshop.common.BizException(
                        com.petshop.common.ErrorCode.PARAM_INVALID, "日期格式不正确：" + date);
            }
        }
        Map<String, Object> sum = reportService.dailySummary(d);
        List<Bill> bills = billMapper.findSettledByDate(d);
        model.addAttribute("date", d);
        model.addAttribute("sum", sum);
        model.addAttribute("bills", bills);
        return "report/daily";
    }

    /** 月度营业汇总 + 商品毛利（v1.1 FR-M10-02/03） */
    @GetMapping("/reports/monthly")
    public String monthly(@RequestParam(required = false) String month, Model model) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(month == null || month.trim().isEmpty()
                    ? YearMonth.now().toString() : month.trim());
        } catch (Exception e) {
            ym = YearMonth.now();
        }
        String from = ym.atDay(1).format(DateTimeFormatter.ISO_DATE);
        String to = ym.atEndOfMonth().format(DateTimeFormatter.ISO_DATE);

        List<Map<String, Object>> rows = monthlyMapper.monthly(from, to);
        Map<String, Map<String, Object>> costByDate = new HashMap<>();
        for (Map<String, Object> c : monthlyMapper.monthlyCost(from, to)) {
            costByDate.put(String.valueOf(c.get("businessDate")), c);
        }
        long sumCard = 0, sumProduct = 0, sumService = 0, sumCost = 0, sumCount = 0;
        for (Map<String, Object> r : rows) {
            Map<String, Object> c = costByDate.get(String.valueOf(r.get("businessDate")));
            long cost = c == null || c.get("productCost") == null ? 0 : ((Number) c.get("productCost")).longValue();
            r.put("productCost", cost);
            long card = ((Number) r.get("cardIncome")).longValue();
            long prod = ((Number) r.get("productIncome")).longValue();
            long svc = ((Number) r.get("serviceIncome")).longValue();
            r.put("dayIncome", card + prod + svc);
            r.put("profit", prod - cost);
            sumCard += card;
            sumProduct += prod;
            sumService += svc;
            sumCost += cost;
            sumCount += ((Number) r.get("billCount")).longValue();
        }
        model.addAttribute("month", ym.toString());
        model.addAttribute("rows", rows);
        model.addAttribute("sumCard", sumCard);
        model.addAttribute("sumProduct", sumProduct);
        model.addAttribute("sumService", sumService);
        model.addAttribute("sumCost", sumCost);
        model.addAttribute("sumIncome", sumCard + sumProduct + sumService);
        model.addAttribute("sumProfit", sumProduct - sumCost);
        model.addAttribute("sumCount", sumCount);
        return "report/monthly";
    }

    /** 次卡余额表入口复用 /cards（M6-03）；本控制器补充库存预警页跳转 */
    @ModelAttribute("expireWarnDays")
    public int expireWarnDays() {
        return reportService.expireWarnDays();
    }

    @GetMapping("/reports/stock")
    public String stock(Model model) {
        List<Product> expiring = productService.expiring();
        List<Product> lowStock = productService.lowStock();
        model.addAttribute("expiring", expiring);
        model.addAttribute("lowStock", lowStock);
        return "report/stock";
    }
}
