package com.bookmind.security;

import com.bookmind.entity.KhachHang;
import com.bookmind.entity.TaiKhoan;
import com.bookmind.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_USER = "CURRENT_USER";
    public static final String SESSION_CUSTOMER = "CURRENT_CUSTOMER";

    private final AuthService authService;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        HttpSession session = request.getSession();
        TaiKhoan user = (TaiKhoan) session.getAttribute(SESSION_USER);

        // Đảm bảo mọi tài khoản đã đăng nhập (kể cả ADMIN khi về trang bán hàng)
        // đều có đầy đủ hồ sơ Khách hàng và Giỏ hàng để mua sắm như khách thông thường
        if (user != null && session.getAttribute(SESSION_CUSTOMER) == null) {
            KhachHang kh = authService.getOrCreateKhachHangForTaiKhoan(user);
            if (kh != null) {
                session.setAttribute(SESSION_CUSTOMER, kh);
            }
        }

        if (uri.startsWith("/admin")) {
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getVaiTro())) {
                response.sendRedirect("/login?error=admin_required&redirect=" + uri);
                return false;
            }
        } else if (uri.startsWith("/shop/checkout") || uri.startsWith("/shop/my-orders") || uri.startsWith("/shop/order") || uri.startsWith("/shop/profile")) {
            if (user == null) {
                String safeRedirect = uri;
                if (uri.contains("/cancel-and-return-to-cart") || uri.contains("/confirm-payment") || uri.contains("/change-payment-method")) {
                    safeRedirect = uri.replaceAll("/(cancel-and-return-to-cart|confirm-payment|change-payment-method).*", "");
                }
                response.sendRedirect("/login?error=login_required&redirect=" + safeRedirect);
                return false;
            }
        }

        return true;
    }
}
