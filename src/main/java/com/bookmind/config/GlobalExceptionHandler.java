package com.bookmind.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public String handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                           HttpServletRequest request,
                                           RedirectAttributes redirectAttributes) {
        String uri = request.getRequestURI();
        log.warn("Method not allowed on URI: {} (method: {})", uri, request.getMethod());

        if (uri.contains("/shop/order/")) {
            try {
                String sub = uri.substring(uri.indexOf("/shop/order/") + "/shop/order/".length());
                String orderIdStr = sub.replaceAll("/.*", "");
                if (orderIdStr.matches("\\d+")) {
                    return "redirect:/shop/order/" + orderIdStr;
                }
            } catch (Exception ignored) {}
            return "redirect:/shop/my-orders";
        }

        if (uri.startsWith("/admin")) {
            return "redirect:/admin";
        }

        return "redirect:/";
    }
}