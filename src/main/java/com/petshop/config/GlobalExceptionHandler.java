package com.petshop.config;

import com.petshop.common.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.sqlite.SQLiteException;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

/** 全局异常：给用户看的永远是中文整句，错误码只进日志。 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public Object handleBiz(BizException e, HttpServletRequest req, RedirectAttributes ra) {
        log.warn("业务异常 code={} uri={} msg={}", e.getCode(), req.getRequestURI(), e.getMessage());
        String accept = req.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            ra.addFlashAttribute("errMsg", e.getMessage());
            return "redirect:" + referer(req);
        }
        if (isAjax(req)) {
            return "redirect:" + referer(req);
        }
        ra.addFlashAttribute("errMsg", e.getMessage());
        return "redirect:" + referer(req);
    }

    @ExceptionHandler({SQLiteException.class, SQLException.class})
    public Object handleDb(Exception e, HttpServletRequest req, RedirectAttributes ra) {
        log.error("数据库异常 uri={}", req.getRequestURI(), e);
        ra.addFlashAttribute("errMsg", "系统繁忙，请稍后重试；若持续出现请联系维护人员");
        return "redirect:" + referer(req);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleOther(Exception e, HttpServletRequest req) {
        log.error("系统异常 uri={}", req.getRequestURI(), e);
        req.setAttribute("errMsg", "系统出错，请稍后重试");
        req.setAttribute("uri", req.getRequestURI());
        return "error/500";
    }

    private boolean isAjax(HttpServletRequest req) {
        return "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
    }

    private String referer(HttpServletRequest req) {
        String ref = req.getHeader("Referer");
        if (ref == null || ref.trim().isEmpty()) {
            return "/";
        }
        // 只允许站内跳转
        if (ref.startsWith("http://") || ref.startsWith("https://")) {
            int idx = ref.indexOf('/', ref.indexOf("//") + 2);
            return idx < 0 ? "/" : ref.substring(idx);
        }
        return ref;
    }
}
