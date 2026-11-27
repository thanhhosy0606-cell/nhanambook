package com.bookmind.config;

import com.bookmind.entity.KhachHang;
import com.bookmind.entity.TaiKhoan;
import com.bookmind.security.AuthInterceptor;
import com.bookmind.service.AuthService;
import com.bookmind.service.GioHangService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    public static final String SESSION_CART_COUNT = "SESSION_CART_COUNT";

    private final GioHangService gioHangService;
    private final AuthService authService;

    public GlobalModelAdvice(GioHangService gioHangService, AuthService authService) {
        this.gioHangService = gioHangService;
        this.authService = authService;
    }

    @ModelAttribute("cartCount")
    public int getCartCount(HttpServletRequest request, HttpSession session) {
        if (session == null) {
            return 0;
        }
        String uri = request != null ? request.getRequestURI() : "";
        if (uri.startsWith("/admin") || uri.startsWith("/api/")) {
            return 0;
        }
        Object cachedCount = session.getAttribute(SESSION_CART_COUNT);
        if (cachedCount instanceof Integer count) {
            return count;
        }

        KhachHang customer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        if (customer == null) {
            TaiKhoan user = (TaiKhoan) session.getAttribute(AuthInterceptor.SESSION_USER);
            if (user != null) {
                try {
                    customer = authService.getOrCreateKhachHangForTaiKhoan(user);
                    if (customer != null) {
                        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, customer);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (customer == null) {
            return 0;
        }
        int count = gioHangService.getCartItemCount(customer);
        session.setAttribute(SESSION_CART_COUNT, count);
        return count;
    }
}
