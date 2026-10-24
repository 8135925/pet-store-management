package com.petshop.config;

import com.petshop.common.BusinessDate;
import com.petshop.common.Money;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

/** 模板全局工具对象：页面里用 ${ui.yuan(x)}、${ui.age(pet.birthday)} 等。 */
@ControllerAdvice
public class UiHelper {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final com.petshop.module.system.ConfigService configService;

    public UiHelper(com.petshop.module.system.ConfigService configService) {
        this.configService = configService;
    }

    @ModelAttribute("ui")
    public UiHelper ui() {
        return this;
    }

    @ModelAttribute("storeName")
    public String storeName() {
        return configService.storeName();
    }

    /** 分 -> "128.00" */
    public String yuan(Integer cent) {
        if (cent == null) {
            return "0.00";
        }
        return Money.yuan(cent);
    }

    /** 分 -> "¥128.00" */
    public String money(Integer cent) {
        return "¥" + yuan(cent);
    }

    /** 宠物年龄：X岁Y个月 / Y个月 / — */
    public String age(LocalDate birthday) {
        if (birthday == null) {
            return "—";
        }
        LocalDate today = LocalDate.now();
        if (birthday.isAfter(today)) {
            return "—";
        }
        Period p = Period.between(birthday, today);
        if (p.getYears() > 0) {
            return p.getYears() + "岁" + p.getMonths() + "个月";
        }
        if (p.getMonths() > 0) {
            return p.getMonths() + "个月";
        }
        return p.getDays() + "天";
    }

    /** 体重克 -> kg 展示 */
    public String kg(Integer weightG) {
        if (weightG == null || weightG <= 0) {
            return "—";
        }
        if (weightG % 1000 == 0) {
            return (weightG / 1000) + "kg";
        }
        return String.format("%.1fkg", weightG / 1000.0);
    }

    /** 体型档位中文名 */
    public String tier(String t) {
        if (t == null) {
            return "—";
        }
        switch (t) {
            case "S": return "小型";
            case "M": return "中型";
            case "L": return "大型";
            case "XL": return "巨型";
            default: return t;
        }
    }

    public String species(String s) {
        if (s == null) return "—";
        switch (s) {
            case "DOG": return "狗";
            case "CAT": return "猫";
            default: return "其他";
        }
    }

    public String gender(String g) {
        if (g == null) return "—";
        switch (g) {
            case "MALE": return "公";
            case "FEMALE": return "母";
            default: return "未知";
        }
    }

    public String eventType(String t) {
        if (t == null) return "—";
        switch (t) {
            case "VACCINE": return "疫苗";
            case "RABIES": return "狂犬";
            case "DEWORM": return "体内驱虫";
            case "OUT_DEWORM": return "体外驱虫";
            case "CHECKUP": return "体检";
            default: return t;
        }
    }

    /** 寄养单状态 */
    public String boardingStatus(String s) {
        if (s == null) return "—";
        switch (s) {
            case "BOOKED": return "已订未住";
            case "CHECKED_IN": return "在住";
            case "CHECKED_OUT": return "已退住";
            case "CANCELLED": return "已取消";
            default: return s;
        }
    }

    /** 预约状态 */
    public String apptStatus(String s) {
        if (s == null) return "—";
        switch (s) {
            case "BOOKED": return "已预约";
            case "ARRIVED": return "已到店";
            case "DONE": return "已完成";
            case "CANCELLED": return "已取消";
            case "NO_SHOW": return "未到";
            default: return s;
        }
    }

    /** 明细类型（v1.1 含寄养） */
    public String itemType(String t) {
        if (t == null) return "—";
        switch (t) {
            case "SERVICE": return "服务";
            case "PRODUCT": return "商品";
            case "BOARDING": return "寄养";
            default: return t;
        }
    }

    public String payMethod(String p) {
        if (p == null) return "—";
        switch (p) {
            case "CASH": return "现金";
            case "WECHAT": return "微信";
            case "ALIPAY": return "支付宝";
            case "BANK_CARD": return "银行卡";
            case "OTHER": return "其他";
            default: return p;
        }
    }

    public String cardStatus(String s) {
        if (s == null) return "—";
        switch (s) {
            case "ACTIVE": return "可用";
            case "EXPIRED": return "已过期";
            case "VOID": return "已作废";
            default: return s;
        }
    }

    public String contactStatus(String s) {
        if (s == null) return "待处理";
        switch (s) {
            case "CONTACTED": return "已联系";
            case "BOOKED": return "已预约";
            case "DONE": return "已完成";
            case "IGNORED": return "暂不联系";
            default: return s;
        }
    }

    public String ledgerType(String t) {
        if (t == null) return "—";
        switch (t) {
            case "IN": return "采购入库";
            case "SALE": return "销售出库";
            case "RETURN": return "退货入库";
            case "CANCEL": return "反结账回滚";
            case "ADJUST": return "盘点调整";
            default: return t;
        }
    }

    public String billType(String t) {
        return "CARD".equals(t) ? "售卡单" : "销售单";
    }

    /** 已逾期 X 天 */
    public long overdueDays(LocalDate due) {
        if (due == null) return 0;
        long d = java.time.temporal.ChronoUnit.DAYS.between(due, LocalDate.now());
        return Math.max(d, 0);
    }

    public String dt(LocalDateTime t) {
        return t == null ? "—" : t.format(DATETIME);
    }

    public String date(LocalDateTime t) {
        return t == null ? "—" : t.format(DATE);
    }

    public String d(LocalDate t) {
        return t == null ? "—" : t.format(DATE);
    }

    public LocalDate today() {
        return LocalDate.now();
    }
}
