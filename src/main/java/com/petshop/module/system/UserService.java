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

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final SysUserMapper mapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    public UserService(SysUserMapper mapper) {
        this.mapper = mapper;
    }

    public SessionUser login(String username, String password) {
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        SysUser u = mapper.findByUsername(username.trim());
        // 统一提示，不区分「用户不存在」与「密码错误」
        if (u == null || !u.getIsActive() || !encoder.matches(password, u.getPasswordHash())) {
            log.warn("登录失败 username={}", username);
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        mapper.updateLoginTime(u.getId(), LocalDateTime.now());
        return new SessionUser(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole());
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
        mapper.insert(u);
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
        mapper.updatePassword(id, encoder.encode(password));
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
        log.info("已创建默认管理员账号 admin，初始密码 {}，请登录后尽快修改", rawPassword);
    }
}
