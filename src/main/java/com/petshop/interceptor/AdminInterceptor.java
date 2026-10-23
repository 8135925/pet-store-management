package com.petshop.interceptor;

import com.petshop.common.BizException;
import com.petshop.common.ErrorCode;
import com.petshop.common.SessionUser;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** 仅 ADMIN 可访问：STAFF 访问管理接口返 403。服务端二次校验，不信任前端。 */
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("sessionUser");
        if (user == null) {
            throw new BizException(ErrorCode.LOGIN_REQUIRED);
        }
        if (!user.isAdmin()) {
            throw new BizException(ErrorCode.NO_PERMISSION);
        }
        return true;
    }
}
