package com.petshop.module.print;

import com.petshop.common.BizException;
import com.petshop.common.ErrorCode;
import com.petshop.module.bill.Bill;
import com.petshop.module.bill.BillService;
import com.petshop.module.card.CustomerCard;
import com.petshop.module.card.CustomerCardMapper;
import com.petshop.module.customer.Customer;
import com.petshop.module.customer.CustomerMapper;
import com.petshop.module.system.ConfigService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 58mm 小票：只读单据快照，不在模板里做二次计算（BR-24）。 */
@Controller
public class PrintController {

    private final BillService billService;
    private final CustomerMapper customerMapper;
    private final CustomerCardMapper cardMapper;
    private final ConfigService configService;
    private final JdbcTemplate jdbc;

    public PrintController(BillService billService, CustomerMapper customerMapper,
                           CustomerCardMapper cardMapper, ConfigService configService,
                           JdbcTemplate jdbc) {
        this.billService = billService;
        this.customerMapper = customerMapper;
        this.cardMapper = cardMapper;
        this.configService = configService;
        this.jdbc = jdbc;
    }

    @GetMapping("/print/bill/{id}")
    public String bill(@PathVariable Long id, Model model) {
        Bill b = billService.detail(id);
        Customer c = customerMapper.findById(b.getCustomerId());
        List<CustomerCard> cards = cardMapper.findUsableByCustomer(b.getCustomerId(), LocalDate.now());
        model.addAttribute("storeName", configService.storeName());
        model.addAttribute("kind", "bill");
        model.addAttribute("bill", b);
        model.addAttribute("customer", c);
        model.addAttribute("cards", cards);
        model.addAttribute("width", configService.getInt("receipt_width", 58));
        return "print/receipt";
    }

    @GetMapping("/print/card/{id}")
    public String card(@PathVariable Long id, Model model) {
        CustomerCard card = cardMapper.findById(id);
        if (card == null) {
            throw new BizException(ErrorCode.CARD_NOT_FOUND);
        }
        Customer c = customerMapper.findById(card.getCustomerId());
        String typeName = jdbc.query(
                "SELECT t.name FROM card_type t WHERE t.id = ?", rs -> rs.next() ? rs.getString(1) : null,
                card.getCardTypeId());
        model.addAttribute("storeName", configService.storeName());
        model.addAttribute("kind", "card");
        model.addAttribute("card", card);
        model.addAttribute("cardTypeName", typeName);
        model.addAttribute("customer", c);
        model.addAttribute("width", configService.getInt("receipt_width", 58));
        return "print/receipt";
    }

    @GetMapping("/print/boarding/{id}")
    public String boarding(@PathVariable Long id, Model model) {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT o.*, r.code AS room_code, p.name AS pet_name, c.name AS customer_name "
                        + "FROM boarding_order o "
                        + "JOIN boarding_room r ON r.id = o.room_id "
                        + "JOIN pet p ON p.id = o.pet_id "
                        + "JOIN customer c ON c.id = o.customer_id "
                        + "WHERE o.id = ?", id);
        model.addAttribute("storeName", configService.storeName());
        model.addAttribute("kind", "boarding");
        model.addAttribute("o", row);
        model.addAttribute("width", configService.getInt("receipt_width", 58));
        return "print/receipt";
    }
}
