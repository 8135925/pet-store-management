package com.petshop.interceptor;

import com.petshop.common.SessionUser;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** 未登录访问任何页面跳 /login。 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 备份恢复准备就绪 → 只读维护页，阻止继续写入旧库
        if (com.petshop.config.RestoreBootstrap.isMaintenance()) {
            String uri = request.getRequestURI();
            // 取消恢复也必须放行，否则维护页上无法退回
            if (!uri.endsWith("/maintenance") && !uri.endsWith("/login")
                    && !uri.endsWith("/system/restore/cancel")
                    && !uri.contains("/css/") && !uri.contains("/js/")
                    && !uri.contains("/vendor/")) {
                response.sendRedirect(request.getContextPath() + "/maintenance");
                return false;
            }
        }
        // 维护页本身不需要登录，避免会话过期把用户锁在重定向里
        if (request.getRequestURI().endsWith("/maintenance")) {
            return true;
        }
        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("sessionUser");
        if (user == null) {
            String uri = request.getRequestURI();
            if ("GET".equalsIgnoreCase(request.getMethod()) && !uri.contains("/login")) {
                response.sendRedirect(request.getContextPath() + "/login");
            } else {
                response.sendError(403, "请先登录");
            }
            return false;
        }
        return true;
    }
}
