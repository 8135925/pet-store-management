package com.petshop.module.system;

import com.petshop.common.BizException;
import com.petshop.common.SessionUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;

/** 登录/登出 + 用户管理 + 门店参数 + 备份。 */
@Controller
public class SystemController {

    private final UserService userService;
    private final ConfigService configService;
    private final BackupService backupService;

    public SystemController(UserService userService, ConfigService configService, BackupService backupService) {
        this.userService = userService;
        this.configService = configService;
        this.backupService = backupService;
    }

    // ---------------- 登录 / 登出 ----------------

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("sessionUser") != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        HttpServletRequest request, RedirectAttributes ra) {
        try {
            SessionUser user = userService.login(username, password);
            HttpSession session = request.getSession(true);
            // 防会话固定攻击：登录成功后更换 session id，旧 id 立即失效
            request.changeSessionId();
            session.setAttribute("sessionUser", user);
            return "redirect:/";
        } catch (BizException e) {
            ra.addFlashAttribute("errMsg", e.getMessage());
            ra.addFlashAttribute("username", username);
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ---------------- 用户管理（ADMIN，路径由拦截器保护） ----------------

    @GetMapping("/system/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAll());
        return "system/user-list";
    }

    @PostMapping("/system/users/save")
    public String saveUser(@RequestParam(required = false) Long id,
                           @RequestParam String username,
                           @RequestParam(required = false) String password,
                           @RequestParam String displayName,
                           @RequestParam String role,
                           @RequestParam(defaultValue = "false") boolean active,
                           RedirectAttributes ra) {
        if (id == null) {
            userService.create(username, password, displayName, role);
            ra.addFlashAttribute("okMsg", "用户已创建");
        } else {
            userService.update(id, displayName, role, active, currentUser());
            if (password != null && !password.isEmpty()) {
                userService.resetPassword(id, password);
            }
            ra.addFlashAttribute("okMsg", "用户已更新");
        }
        return "redirect:/system/users";
    }

    private SessionUser currentUser() {
        org.springframework.web.context.request.RequestAttributes ra =
                org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
        HttpSession session = ((org.springframework.web.context.request.ServletRequestAttributes) ra).getRequest().getSession(false);
        return session == null ? null : (SessionUser) session.getAttribute("sessionUser");
    }

    // ---------------- 门店参数（ADMIN） ----------------

    @GetMapping("/system/config")
    public String configPage(Model model) {
        model.addAttribute("storeName", configService.storeName());
        model.addAttribute("giftFirst", configService.giftFirst());
        model.addAttribute("expireWarnDays", configService.expireWarnDays());
        model.addAttribute("lowStockWarn", configService.lowStockWarn());
        return "system/config";
    }

    @PostMapping("/system/config")
    public String saveConfig(@RequestParam String storeName,
                             @RequestParam(defaultValue = "false") boolean giftFirst,
                             @RequestParam int expireWarnDays,
                             @RequestParam int lowStockWarn,
                             RedirectAttributes ra) {
        Map<String, String> kv = new LinkedHashMap<>();
        kv.put("store_name", storeName);
        kv.put("gift_first", String.valueOf(giftFirst));
        kv.put("expire_warn_days", String.valueOf(Math.max(1, expireWarnDays)));
        kv.put("low_stock_warn", String.valueOf(Math.max(0, lowStockWarn)));
        configService.save(kv);
        ra.addFlashAttribute("okMsg", "参数已保存");
        return "redirect:/system/config";
    }

    // ---------------- 备份（ADMIN） ----------------

    @GetMapping("/system/backup")
    public String backupPage(Model model) {
        model.addAttribute("backups", backupService.listBackups());
        model.addAttribute("dbPath", backupService.dbPath());
        return "system/backup";
    }

    @PostMapping("/system/backup")
    public String doBackup(RedirectAttributes ra) {
        String name = backupService.manualBackup();
        ra.addFlashAttribute("okMsg", "备份完成：" + name);
        return "redirect:/system/backup";
    }
}
