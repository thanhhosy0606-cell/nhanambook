package com.bookmind.controller;

import com.bookmind.entity.ChiTietGioHang;
import com.bookmind.entity.KhachHang;
import com.bookmind.security.AuthInterceptor;
import com.bookmind.service.GioHangService;
import com.bookmind.service.SavedCouponService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class CartController {

    private final GioHangService gioHangService;
    private final SavedCouponService savedCouponService;

    @org.springframework.beans.factory.annotation.Autowired
    public CartController(GioHangService gioHangService, SavedCouponService savedCouponService) {
        this.gioHangService = gioHangService;
        this.savedCouponService = savedCouponService;
    }

    @GetMapping("/shop/cart")
    public String viewCart(HttpSession session, Model model) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        if (currentCustomer == null) {
            return "redirect:/login?error=login_required&redirect=/shop/cart";
        }

        List<ChiTietGioHang> items = gioHangService.getCartItems(currentCustomer);
        boolean hasDiscontinuedItems = items.stream()
                .anyMatch(i -> "NGUNG_BAN".equalsIgnoreCase(i.getSach().getTrangThai()));
        BigDecimal subtotal = items.stream()
                .filter(i -> !"NGUNG_BAN".equalsIgnoreCase(i.getSach().getTrangThai()))
                .map(i -> i.getDonGia().multiply(BigDecimal.valueOf(i.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("items", items);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("hasDiscontinuedItems", hasDiscontinuedItems);
        model.addAttribute("savedCoupons", savedCouponService.getSavedUsableCoupons(session));
        return "shop/cart";
    }

    @PostMapping("/shop/cart/add")
    public String addToCart(@RequestParam("maSach") Long maSach,
                            @RequestParam(value = "soLuong", defaultValue = "1") int soLuong,
                            @RequestParam(value = "action", defaultValue = "cart") String action,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        if (currentCustomer == null) {
            return "redirect:/login?error=login_required&redirect=/shop/book/" + maSach;
        }

        try {
            gioHangService.addToCart(currentCustomer, maSach, soLuong);
            session.removeAttribute(com.bookmind.config.GlobalModelAdvice.SESSION_CART_COUNT);
            if ("buy_now".equalsIgnoreCase(action)) {
                return "redirect:/shop/checkout?items=" + maSach;
            }
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sách vào giỏ hàng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/shop/book/" + maSach;
        }
        return "redirect:/shop/cart";
    }

    @PostMapping("/shop/cart/update")
    public String updateQuantity(@RequestParam("maSach") Long maSach,
                                 @RequestParam("soLuong") int soLuong,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        if (currentCustomer == null) {
            return "redirect:/login?error=login_required&redirect=/shop/cart";
        }

        try {
            gioHangService.updateQuantity(currentCustomer, maSach, soLuong);
            session.removeAttribute(com.bookmind.config.GlobalModelAdvice.SESSION_CART_COUNT);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật giỏ hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/shop/cart";
    }

    @PostMapping("/shop/cart/remove")
    public String removeItem(@RequestParam("maSach") Long maSach,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        if (currentCustomer == null) {
            return "redirect:/login?error=login_required&redirect=/shop/cart";
        }

        try {
            gioHangService.removeFromCart(currentCustomer, maSach);
            session.removeAttribute(com.bookmind.config.GlobalModelAdvice.SESSION_CART_COUNT);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/shop/cart";
    }
}
