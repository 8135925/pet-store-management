package com.petshop.module.system;

import com.petshop.common.BizException;
import com.petshop.common.ErrorCode;
import com.petshop.common.SessionUser;
import com.petshop.config.AppProps;
import com.petshop.config.RestoreBootstrap;
import com.petshop.module.audit.AuditService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 运维：审计日志、数据体检、备份恢复（FR-M16-01~03）。 */
@Controller
public class OpsController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AuditService auditService;
    private final BackupService backupService;
    private final AppProps props;
    private final JdbcTemplate jdbc;

    public OpsController(AuditService auditService, BackupService backupService,
                         AppProps props, JdbcTemplate jdbc) {
        this.auditService = auditService;
        this.backupService = backupService;
        this.props = props;
        this.jdbc = jdbc;
    }

    private SessionUser user(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }

    // ==================== 审计 ====================

    @GetMapping("/system/audit")
    public String audit(@RequestParam(required = false) String action,
                        @RequestParam(required = false) String operator,
                        @RequestParam(required = false) String from,
                        @RequestParam(required = false) String to,
                        @RequestParam(defaultValue = "1") int page,
                        Model model) {
        int p = Math.max(page, 1);
        int size = 50;
        LocalDate f = parseDate(from);
        LocalDate t = parseDate(to);
        long total = auditService.count(action, operator, f, t);
        long offset = (long) (p - 1) * size;
        List<Map<String, Object>> rows = total == 0 ? new ArrayList<Map<String, Object>>()
                : auditService.search(action, operator, f, t, (int) Math.min(offset, Integer.MAX_VALUE), size);
        model.addAttribute("rows", rows);
        model.addAttribute("total", total);
        model.addAttribute("page", p);
        model.addAttribute("pages", (total + size - 1) / size);
        model.addAttribute("action", action);
        model.addAttribute("operator", operator);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("actions", ACTIONS);
        return "system/audit";
    }

    private static final Map<String, String> ACTIONS = new LinkedHashMap<String, String>();

    static {
        ACTIONS.put("BILL_CANCEL", "反结账");
        ACTIONS.put("PRICE_CHANGE", "改价");
        ACTIONS.put("CARD_VOID", "作废卡");
        ACTIONS.put("CARD_EXTEND", "卡延期");
        ACTIONS.put("PURCHASE_RECEIVE", "采购入库");
        ACTIONS.put("PURCHASE_CANCEL", "采购作废");
        ACTIONS.put("BATCH_VOID", "批次报损");
        ACTIONS.put("STOCK_TAKE_CONFIRM", "盘点确认");
        ACTIONS.put("STOCK_TAKE_CANCEL", "盘点作废");
        ACTIONS.put("COMMISSION_CONFIRM", "提成确认");
        ACTIONS.put("SUPPLIER_CREATE", "新增供应商");
        ACTIONS.put("SUPPLIER_UPDATE", "修改供应商");
        ACTIONS.put("BACKUP_RESTORE", "备份恢复");
        ACTIONS.put("DATA_EXPORT", "数据导出");
    }

    // ==================== 数据体检 ====================

    @GetMapping("/system/health")
    public String health(Model model) {
        List<Map<String, Object>> checks = new ArrayList<Map<String, Object>>();
        checks.add(check("库存结存 vs 流水求和",
                "SELECT p.id, p.name, p.stock_qty, IFNULL(SUM(l.qty_change),0) AS ledger_sum "
                        + "FROM product p LEFT JOIN product_ledger l ON l.product_id = p.id "
                        + "GROUP BY p.id HAVING p.stock_qty <> IFNULL(SUM(l.qty_change),0)"));
        checks.add(check("库存结存 vs 批次剩余",
                "SELECT p.id, p.name, p.stock_qty, IFNULL((SELECT SUM(qty_left) FROM product_batch b "
                        + "WHERE b.product_id = p.id AND b.status <> 'VOID'),0) AS batch_sum "
                        + "FROM product p "
                        + "WHERE p.stock_qty <> IFNULL((SELECT SUM(qty_left) FROM product_batch b "
                        + "WHERE b.product_id = p.id AND b.status <> 'VOID'),0)"));
        checks.add(check("单据优惠 vs 明细分摊",
                "SELECT b.id, b.bill_no, b.discount_cent, IFNULL(SUM(i.discount_cent),0) AS item_sum "
                        + "FROM bill b LEFT JOIN bill_item i ON i.bill_id = b.id "
                        + "WHERE b.status = 'SETTLED' "
                        + "GROUP BY b.id HAVING b.discount_cent <> IFNULL(SUM(i.discount_cent),0)"));
        checks.add(check("卡次数 vs 核销流水",
                "SELECT c.id, c.card_no, c.used_paid, c.used_gift, "
                        + "SUM(CASE WHEN l.from_gift=0 THEN l.qty ELSE 0 END) AS log_paid, "
                        + "SUM(CASE WHEN l.from_gift=1 THEN l.qty ELSE 0 END) AS log_gift "
                        + "FROM customer_card c LEFT JOIN card_consume_log l ON l.card_id = c.id "
                        + "GROUP BY c.id HAVING c.used_paid <> IFNULL(log_paid,0) "
                        + "OR c.used_gift <> IFNULL(log_gift,0)"));
        checks.add(check("已退住寄养单缺少结算单",
                "SELECT o.id, o.order_no, o.status FROM boarding_order o "
                        + "WHERE o.status = 'CHECKED_OUT' AND o.bill_id IS NULL"));
        // 仅在「确实启用过提成」时检查，否则没配规则的门店会满屏误报
        checks.add(check("有服务人员但未生成提成的明细（未启用提成时忽略）",
                "SELECT i.id, i.bill_id, i.name_snapshot, i.staff_id FROM bill_item i "
                        + "JOIN bill b ON b.id = i.bill_id "
                        + "WHERE b.status = 'SETTLED' AND i.staff_id IS NOT NULL AND i.paid_by_card = 0 "
                        + "AND EXISTS (SELECT 1 FROM commission_record) "
                        + "AND NOT EXISTS (SELECT 1 FROM commission_record c WHERE c.bill_item_id = i.id) "
                        + "LIMIT 200"));
        model.addAttribute("checks", checks);
        model.addAttribute("abnormal", countAbnormal(checks));
        return "system/health";
    }

    private Map<String, Object> check(String name, String sql) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", name);
        // 固定放一个 error 键：模板里 c.error 在键缺失时会抛 SpEL 异常
        m.put("error", null);
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(sql);
            m.put("rows", rows);
            m.put("count", rows.size());
        } catch (Exception e) {
            m.put("rows", new ArrayList<Map<String, Object>>());
            m.put("count", -1);
            m.put("error", e.getMessage());
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    private int countAbnormal(List<Map<String, Object>> checks) {
        int n = 0;
        for (Map<String, Object> c : checks) {
            Object cnt = c.get("count");
            if (cnt instanceof Number && ((Number) cnt).intValue() > 0) {
                n++;
            }
        }
        return n;
    }

    // ==================== 备份恢复 ====================

    /** 恢复准备就绪后的只读维护页（拦截器只放行本页与取消操作） */
    @GetMapping("/maintenance")
    public String maintenance(Model model) {
        model.addAttribute("pending", Files.isRegularFile(RestoreBootstrap.marker()));
        return "maintenance";
    }

    @GetMapping("/system/restore")
    public String restore(Model model) {
        model.addAttribute("backups", backupService.listBackups());
        model.addAttribute("pending", Files.isRegularFile(RestoreBootstrap.marker()));
        model.addAttribute("maintenance", RestoreBootstrap.isMaintenance());
        return "system/restore";
    }

    @PostMapping("/system/restore/prepare")
    public String prepareRestore(@RequestParam(required = false) String fileName,
                                 @RequestParam(required = false) MultipartFile file,
                                 HttpSession session, HttpServletRequest request,
                                 RedirectAttributes ra) {
        Path source = null;
        try {
            if (file != null && !file.isEmpty()) {
                String original = file.getOriginalFilename();
                if (original == null || !original.toLowerCase().endsWith(".db")) {
                    throw new BizException(ErrorCode.RESTORE_FILE_INVALID, "只支持 .db 备份文件");
                }
                Path dir = Paths.get(props.getDataDir(), "restore");
                Files.createDirectories(dir);
                source = dir.resolve("upload-" + LocalDateTime.now().format(TS) + ".db");
                Files.copy(file.getInputStream(), source, StandardCopyOption.REPLACE_EXISTING);
            } else if (fileName != null && !fileName.trim().isEmpty()) {
                Path base = Paths.get(props.getBackupDir()).toAbsolutePath().normalize();
                Path p = base.resolve(fileName.trim()).normalize();
                if (!p.startsWith(base)) {
                    throw new BizException(ErrorCode.RESTORE_FILE_INVALID, "备份文件路径不合法");
                }
                if (!Files.isRegularFile(p)) {
                    throw new BizException(ErrorCode.RESTORE_FILE_INVALID, "备份文件不存在");
                }
                source = p;
            } else {
                throw new BizException(ErrorCode.RESTORE_FILE_INVALID, "请选择备份文件");
            }

            validateDb(source);
            // 先备份当前库，再写标记
            String pre = backupService.manualBackup();
            Path marker = RestoreBootstrap.marker();
            Files.createDirectories(marker.getParent());
            Files.write(marker, source.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8));
            RestoreBootstrap.enterMaintenance();

            auditService.log("BACKUP_RESTORE", "SYSTEM", null,
                    null, "prepare " + source.getFileName() + ", pre-backup " + pre,
                    user(session), request.getRemoteAddr());
            ra.addFlashAttribute("okMsg", "已校验通过并准备就绪，请重启服务完成替换（当前库已自动备份）");
            return "redirect:/maintenance";
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.RESTORE_FILE_INVALID, "恢复准备失败：" + e.getMessage());
        }
    }

    @PostMapping("/system/restore/cancel")
    public String cancelRestore(HttpSession session, HttpServletRequest request,
                                RedirectAttributes ra) throws Exception {
        Files.deleteIfExists(RestoreBootstrap.marker());
        RestoreBootstrap.clearMaintenance();
        auditService.log("BACKUP_RESTORE", "SYSTEM", null, "cancel", null,
                user(session), request.getRemoteAddr());
        ra.addFlashAttribute("okMsg", "已取消恢复准备，系统恢复正常使用");
        return "redirect:/system/restore";
    }

    /** 校验：完整性 + 必需表 + 至少一个管理员 */
    private void validateDb(Path db) throws Exception {
        String url = "jdbc:sqlite:" + db.toAbsolutePath().toString().replace("\\", "/");
        try (Connection c = DriverManager.getConnection(url);
             Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("PRAGMA integrity_check")) {
                if (!rs.next() || !"ok".equalsIgnoreCase(rs.getString(1))) {
                    throw new BizException(ErrorCode.RESTORE_FILE_INVALID, "备份文件完整性校验失败");
                }
            }
            for (String table : new String[]{"bill", "customer", "product", "sys_user", "product_batch"}) {
                try (ResultSet rs = st.executeQuery(
                        "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='" + table + "'")) {
                    if (!rs.next() || rs.getInt(1) == 0) {
                        throw new BizException(ErrorCode.RESTORE_FILE_INVALID,
                                "备份文件缺少必需表：" + table);
                    }
                }
            }
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM sys_user WHERE role='ADMIN' AND is_active=1")) {
                if (!rs.next() || rs.getInt(1) == 0) {
                    throw new BizException(ErrorCode.RESTORE_FILE_INVALID, "备份文件中没有可用的管理员账号");
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.RESTORE_FILE_INVALID, "无法读取备份文件");
        }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "日期格式不正确");
        }
    }
}
