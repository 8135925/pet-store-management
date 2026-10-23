package com.petshop.module.report;

import com.petshop.common.BusinessDate;
import com.petshop.module.bill.Bill;
import com.petshop.module.bill.BillMapper;
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
import java.util.List;
import java.util.Map;

@Controller
public class ReportController {

    private final ReportService reportService;
    private final BillMapper billMapper;
    private final PetEventMapper eventMapper;
    private final ProductService productService;

    public ReportController(ReportService reportService, BillMapper billMapper,
                            PetEventMapper eventMapper, ProductService productService) {
        this.reportService = reportService;
        this.billMapper = billMapper;
        this.eventMapper = eventMapper;
        this.productService = productService;
    }

    /** 首页看板：6 个数字 */
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
        return "dashboard";
    }

    /** 日营业汇总（三类收入分列） */
    @GetMapping("/reports/daily")
    public String daily(@RequestParam(required = false) String date, Model model) {
        LocalDate d = (date == null || date.trim().isEmpty())
                ? BusinessDate.today() : LocalDate.parse(date.trim());
        Map<String, Object> sum = reportService.dailySummary(d);
        List<Bill> bills = billMapper.findSettledByDate(d);
        model.addAttribute("date", d);
        model.addAttribute("sum", sum);
        model.addAttribute("bills", bills);
        return "report/daily";
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
