package com.petshop.module.audit;

import com.petshop.common.SessionUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 操作审计。显式调用而非 AOP：动作点可控、失败不拖累主流程。
 * 写审计失败只记日志，不影响业务事务（审计是旁路）。
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;

    public AuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(String action, String targetType, Long targetId,
                    String beforeJson, String afterJson, SessionUser user, String ip) {
        try {
            jdbc.update("INSERT INTO audit_log(action, target_type, target_id, before_json, after_json, "
                            + "operator, operator_id, ip, created_at) VALUES(?,?,?,?,?,?,?,?,?)",
                    action, targetType, targetId, cut(beforeJson), cut(afterJson),
                    user == null ? null : user.getDisplayName(),
                    user == null ? null : user.getId(),
                    ip, LocalDateTime.now().format(TS));
        } catch (Exception e) {
            log.warn("审计写入失败 action={} target={}#{}", action, targetType, targetId, e);
        }
    }

    public List<Map<String, Object>> search(String action, String operator,
                                            LocalDate from, LocalDate to, int offset, int size) {
        StringBuilder sql = new StringBuilder("SELECT * FROM audit_log WHERE 1=1");
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        if (action != null && !action.trim().isEmpty()) {
            sql.append(" AND action = ?");
            args.add(action.trim());
        }
        if (operator != null && !operator.trim().isEmpty()) {
            sql.append(" AND operator LIKE ?");
            args.add("%" + operator.trim() + "%");
        }
        if (from != null) {
            sql.append(" AND date(created_at) >= ?");
            args.add(from.toString());
        }
        if (to != null) {
            sql.append(" AND date(created_at) <= ?");
            args.add(to.toString());
        }
        sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        args.add(size);
        args.add(offset);
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public long count(String action, String operator, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM audit_log WHERE 1=1");
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        if (action != null && !action.trim().isEmpty()) {
            sql.append(" AND action = ?");
            args.add(action.trim());
        }
        if (operator != null && !operator.trim().isEmpty()) {
            sql.append(" AND operator LIKE ?");
            args.add("%" + operator.trim() + "%");
        }
        if (from != null) {
            sql.append(" AND date(created_at) >= ?");
            args.add(from.toString());
        }
        if (to != null) {
            sql.append(" AND date(created_at) <= ?");
            args.add(to.toString());
        }
        Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return n == null ? 0 : n;
    }

    /** 审计详情里不保留超长 JSON */
    private String cut(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= 1000 ? s : s.substring(0, 1000);
    }
}
