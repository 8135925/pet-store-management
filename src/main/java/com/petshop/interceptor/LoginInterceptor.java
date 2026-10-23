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
