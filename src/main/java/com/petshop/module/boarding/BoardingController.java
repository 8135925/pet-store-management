package com.petshop.module.boarding;

import com.petshop.common.SessionUser;
import com.petshop.module.customer.CustomerMapper;
import com.petshop.module.customer.PetMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.Map;

@Controller
public class BoardingController {

    private final BoardingService service;
    private final CustomerMapper customerMapper;
    private final PetMapper petMapper;

    public BoardingController(BoardingService service, CustomerMapper customerMapper, PetMapper petMapper) {
        this.service = service;
        this.customerMapper = customerMapper;
        this.petMapper = petMapper;
    }

    /** 寄养看板：在住 / 今日入住 / 今日应离店 + 历史查询 */
    @GetMapping("/boarding")
    public String board(@RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String dateFrom,
                        @RequestParam(required = false) String dateTo,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "board") String view,
                        Model model) {
        Map<String, Object> b = service.board();
        model.addAttribute("inHouse", b.get("inHouse"));
        model.addAttribute("todayArrivals", b.get("todayArrivals"));
        model.addAttribute("todayDue", b.get("todayDue"));
        if ("history".equals(view)) {
            model.addAttribute("his", service.search(keyword, dateFrom, dateTo, page, 20));
        }
        model.addAttribute("view", view);
        model.addAttribute("keyword", keyword);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        return "boarding/board";
    }

    @GetMapping("/boarding/new")
    public String createPage(@RequestParam(required = false) Long customerId, Model model) {
        model.addAttribute("customers", customerMapper.listActive());
        model.addAttribute("rooms", service.activeRooms());
        model.addAttribute("customerId", customerId);
        if (customerId != null) {
            model.addAttribute("pets", petMapper.findByOwner(customerId));
        }
        return "boarding/form";
    }

    @PostMapping("/boarding/save")
    public String save(@RequestParam Long customerId, @RequestParam Long petId,
                       @RequestParam Long roomId, @RequestParam String startAt,
                       @RequestParam String plannedEndOn, @RequestParam String unitPriceYuan,
                       @RequestParam(required = false) String depositYuan,
                       @RequestParam(required = false) String remark,
                       HttpSession session, RedirectAttributes ra) {
        SessionUser user = (SessionUser) session.getAttribute("sessionUser");
        Long id = service.create(customerId, petId, roomId, startAt, plannedEndOn,
                unitPriceYuan, depositYuan, remark, user);
        ra.addFlashAttribute("okMsg", "寄养单已创建（入住时将校验疫苗）");
        return "redirect:/boarding/" + id;
    }

    @GetMapping("/boarding/{id}")
    public String detail(@PathVariable Long id, Model model) {
        BoardingOrder o = service.detail(id);
        model.addAttribute("o", o);
        model.addAttribute("logs", service.logsOf(id));
        model.addAttribute("today", LocalDate.now().toString());
        return "boarding/detail";
    }

    @PostMapping("/boarding/{id}/checkin")
    public String checkin(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        SessionUser user = (SessionUser) session.getAttribute("sessionUser");
        service.checkin(id, user);
        ra.addFlashAttribute("okMsg", "已办理入住");
        return "redirect:/boarding/" + id;
    }

    @PostMapping("/boarding/{id}/checkout")
    public String checkout(@PathVariable Long id, @RequestParam String payMethod,
                           HttpSession session, RedirectAttributes ra) {
        SessionUser user = (SessionUser) session.getAttribute("sessionUser");
        Long billId = service.checkout(id, payMethod, user);
        ra.addFlashAttribute("okMsg", "退住结算完成");
        return "redirect:/bills/" + billId;
    }

    @PostMapping("/boarding/{id}/extend")
    public String extend(@PathVariable Long id, @RequestParam String plannedEndOn,
                         @RequestParam(required = false) String note,
                         HttpSession session, RedirectAttributes ra) {
        SessionUser user = (SessionUser) session.getAttribute("sessionUser");
        service.extend(id, plannedEndOn, note, user);
        ra.addFlashAttribute("okMsg", "预离店日已修改");
        return "redirect:/boarding/" + id;
    }

    @PostMapping("/boarding/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        service.cancel(id);
        ra.addFlashAttribute("okMsg", "寄养单已取消");
        return "redirect:/boarding/" + id;
    }

    @PostMapping("/boarding/{id}/log")
    public String addLog(@PathVariable Long id, @RequestParam String logDate,
                         @RequestParam(defaultValue = "0") Integer fedCount,
                         @RequestParam(defaultValue = "false") boolean walked,
                         @RequestParam(required = false) String note,
                         HttpSession session, RedirectAttributes ra) {
        SessionUser user = (SessionUser) session.getAttribute("sessionUser");
        service.addLog(id, logDate, fedCount, walked, note, user);
        ra.addFlashAttribute("okMsg", "记录已保存");
        return "redirect:/boarding/" + id;
    }

    /** 笼位管理（ADMIN，拦截器保护） */
    @GetMapping("/boarding/rooms")
    public String rooms(Model model) {
        model.addAttribute("rooms", service.rooms());
        return "boarding/rooms";
    }

    @PostMapping("/boarding/rooms/save")
    public String saveRoom(@RequestParam(required = false) Long id, @RequestParam String code,
                           @RequestParam String roomType, @RequestParam(defaultValue = "0") String dailyPriceYuan,
                           @RequestParam(required = false) String note, RedirectAttributes ra) {
        service.saveRoom(id, code, roomType, dailyPriceYuan, note);
        ra.addFlashAttribute("okMsg", "笼位已保存");
        return "redirect:/boarding/rooms";
    }

    @PostMapping("/boarding/rooms/{id}/toggle")
    public String toggleRoom(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes ra) {
        service.toggleRoom(id, active);
        ra.addFlashAttribute("okMsg", active ? "笼位已启用" : "笼位已停用");
        return "redirect:/boarding/rooms";
    }
}
