package com.petshop.module.system;

import com.petshop.common.BizException;
import com.petshop.common.ErrorCode;
import com.petshop.common.SessionUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    /** 连续失败达到该次数后锁定账号一段时间，防在线暴力猜密码 */
    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MILLIS = 10 * 60 * 1000L;
    private static final int MAX_TRACKED_USERS = 2000;

    private final SysUserMapper mapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    /** username -> 失败计数（内存态，重启后清零；单店场景足够） */
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<String, Attempt>();

    public UserService(SysUserMapper mapper) {
        this.mapper = mapper;
    }

    private static final class Attempt {
        private int failures;
        private long lockedUntil;
    }

    public SessionUser login(String username, String password) {
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        String key = username.trim();
        long nowMs = System.currentTimeMillis();
        Attempt attempt = attempts.get(key);
        if (attempt != null && attempt.lockedUntil > nowMs) {
            throw new BizException(ErrorCode.LOGIN_LOCKED);
        }
        SysUser u = mapper.findByUsername(key);
        // 统一提示，不区分「用户不存在」与「密码错误」
        if (u == null || !u.getIsActive() || !encoder.matches(password, u.getPasswordHash())) {
            recordFailure(key, nowMs);
            log.warn("登录失败 username={}", key);
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        attempts.remove(key);
        mapper.updateLoginTime(u.getId(), LocalDateTime.now());
        return new SessionUser(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole());
    }

    private void recordFailure(String key, long nowMs) {
        if (attempts.size() > MAX_TRACKED_USERS) {
            attempts.entrySet().removeIf(e -> e.getValue().lockedUntil > 0
                    && e.getValue().lockedUntil <= nowMs);
        }
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        synchronized (attempt) {
            attempt.failures++;
            if (attempt.failures >= MAX_FAILURES) {
                attempt.lockedUntil = nowMs + LOCK_MILLIS;
                attempt.failures = 0;
                log.warn("账号因连续登录失败被临时锁定 username={}", key);
            }
        }
    }

    public List<SysUser> findAll() {
        return mapper.findAll();
    }

    public void create(String username, String password, String displayName, String role) {
        if (username == null || username.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            throw new BizException(ErrorCode.PARAM_INVALID, "密码不能少于 6 位");
        }
        if (mapper.findByUsername(username.trim()) != null) {
            throw new BizException(ErrorCode.USER_EXISTS);
        }
        SysUser u = new SysUser();
        u.setUsername(username.trim());
        u.setPasswordHash(encoder.encode(password));
        u.setDisplayName((displayName == null || displayName.trim().isEmpty()) ? username.trim() : displayName.trim());
        u.setRole("ADMIN".equals(role) ? "ADMIN" : "STAFF");
        u.setIsActive(true);
        u.setCreatedAt(LocalDateTime.now());
        try {
            mapper.insert(u);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BizException(ErrorCode.USER_EXISTS);
        }
    }

    public void update(Long id, String displayName, String role, boolean active, SessionUser current) {
        SysUser u = mapper.findById(id);
        if (u == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用户不存在");
        }
        if (id.equals(current.getId()) && (!active || !"ADMIN".equals(role))) {
            throw new BizException(ErrorCode.USER_DELETE_SELF);
        }
        if (u.getIsActive() && "ADMIN".equals(u.getRole()) && (!active || !"ADMIN".equals(role))
                && mapper.countAdminActive(id) == 0) {
            throw new BizException(ErrorCode.USER_LAST_ADMIN);
        }
        u.setDisplayName(displayName == null || displayName.trim().isEmpty() ? u.getUsername() : displayName.trim());
        u.setRole("ADMIN".equals(role) ? "ADMIN" : "STAFF");
        u.setIsActive(active);
        mapper.update(u);
    }

    public void resetPassword(Long id, String password) {
        if (password == null || password.length() < 6) {
            throw new BizException(ErrorCode.PARAM_INVALID, "密码不能少于 6 位");
        }
        if (mapper.findById(id) == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用户不存在");
        }
        mapper.updatePassword(id, encoder.encode(password));
    }

    /**
     * 删除用户（仅限店员账号）。管理员一律不可删除：当前登录者必是管理员（拦截器保证），
     * 所以该规则同时天然防住了「删除自己」和「删光管理员」。
     */
    public void delete(Long id, SessionUser current) {
        SysUser u = mapper.findById(id);
        if (u == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用户不存在");
        }
        if ("ADMIN".equals(u.getRole())) {
            throw new BizException(ErrorCode.USER_DELETE_ADMIN);
        }
        if (current != null && id.equals(current.getId())) {
            throw new BizException(ErrorCode.USER_DELETE_SELF);
        }
        mapper.deleteById(id);
        log.info("用户已删除 id={} username={}（由 {} 执行）", id, u.getUsername(),
                current == null ? "-" : current.getUsername());
    }

    /** 供 DataInitializer 首次启动创建 admin */
    public void initAdmin(String username, String rawPassword) {
        if (mapper.count() > 0) {
            return;
        }
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setDisplayName("店主");
        u.setRole("ADMIN");
        u.setIsActive(true);
        u.setCreatedAt(LocalDateTime.now());
        mapper.insert(u);
        // 不在日志里输出明文口令：日志会被收集、转发、留存
        log.warn("已创建默认管理员账号 '{}'，初始密码取自 app.default-admin-password，请登录后立即修改", username);
    }
}
