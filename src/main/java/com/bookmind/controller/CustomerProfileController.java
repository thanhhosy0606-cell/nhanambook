package com.bookmind.controller;

import com.bookmind.entity.DiaChi;
import com.bookmind.entity.KhachHang;
import com.bookmind.entity.TaiKhoan;
import com.bookmind.repository.DonHangRepository;
import com.bookmind.security.AuthInterceptor;
import com.bookmind.service.AuthService;
import com.bookmind.service.KhachHangService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class CustomerProfileController {

    private static final Logger log = LoggerFactory.getLogger(CustomerProfileController.class);

    private final KhachHangService khachHangService;
    private final DonHangRepository donHangRepository;
    private final AuthService authService;

    @Autowired
    public CustomerProfileController(KhachHangService khachHangService,
                                     DonHangRepository donHangRepository,
                                     AuthService authService) {
        this.khachHangService = khachHangService;
        this.donHangRepository = donHangRepository;
        this.authService = authService;
    }

    private KhachHang resolveCustomer(HttpSession session) {
        KhachHang kh = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        if (kh == null) {
            TaiKhoan user = (TaiKhoan) session.getAttribute(AuthInterceptor.SESSION_USER);
            if (user != null) {
                kh = authService.getOrCreateKhachHangForTaiKhoan(user);
                session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);
            }
        }
        return kh;
    }

    @GetMapping({"/shop/profile", "/profile"})
    public String showProfile(HttpSession session, Model model) {
        TaiKhoan user = (TaiKhoan) session.getAttribute(AuthInterceptor.SESSION_USER);
        if (user == null) {
            return "redirect:/login?error=login_required&redirect=/shop/profile";
        }

        KhachHang customer = resolveCustomer(session);
        // Refresh customer from DB to make sure we show latest state
        if (customer != null && customer.getMaKH() != null) {
            customer = khachHangService.getById(customer.getMaKH());
            session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, customer);
        }

        List<DiaChi> addresses = customer != null ? khachHangService.getAddresses(customer.getMaKH()) : List.of();
        long orderCount = customer != null ? donHangRepository.countByKhachHang_MaKH(customer.getMaKH()) : 0;

        model.addAttribute("user", user);
        model.addAttribute("customer", customer);
        model.addAttribute("addresses", addresses);
        model.addAttribute("orderCount", orderCount);

        return "shop/profile";
    }

    @PostMapping("/shop/profile")
    public String updateProfile(@RequestParam("hoTen") String hoTen,
                                @RequestParam(value = "sdt", required = false) String sdt,
                                @RequestParam(value = "gioiTinh", required = false) Boolean gioiTinh,
                                @RequestParam(value = "ngaySinh", required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngaySinh,
                                @RequestParam(value = "anhDaiDien", required = false) String anhDaiDien,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        TaiKhoan user = (TaiKhoan) session.getAttribute(AuthInterceptor.SESSION_USER);
        if (user == null) {
            return "redirect:/login?error=login_required&redirect=/shop/profile";
        }

        KhachHang customer = resolveCustomer(session);
        if (customer == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy hồ sơ khách hàng để cập nhật!");
            return "redirect:/shop/profile";
        }

        try {
            KhachHang updated = khachHangService.updateProfile(
                    customer.getMaKH(), hoTen, sdt, gioiTinh, ngaySinh, anhDaiDien
            );
            session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, updated);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin cá nhân thành công!");
        } catch (Exception e) {
            log.error("Update profile error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/shop/profile";
    }

    @PostMapping("/shop/profile/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        TaiKhoan user = (TaiKhoan) session.getAttribute(AuthInterceptor.SESSION_USER);
        if (user == null) {
            return "redirect:/login?error=login_required&redirect=/shop/profile";
        }

        try {
            khachHangService.changePassword(user.getMaTK(), oldPassword, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            log.error("Change password error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/shop/profile";
    }

    @PostMapping("/shop/profile/address/add")
    public String addAddress(@RequestParam("hoTenNguoiNhan") String hoTenNguoiNhan,
                             @RequestParam("sdtNguoiNhan") String sdtNguoiNhan,
                             @RequestParam("diaChiNhan") String diaChiNhan,
                             @RequestParam(value = "macDinh", defaultValue = "false") boolean macDinh,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        KhachHang customer = resolveCustomer(session);
        if (customer == null) {
            return "redirect:/login?error=login_required&redirect=/shop/profile";
        }

        try {
            khachHangService.addAddress(customer.getMaKH(), hoTenNguoiNhan, sdtNguoiNhan, diaChiNhan, macDinh);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm địa chỉ giao hàng mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/shop/profile";
    }

    @PostMapping("/shop/profile/address/delete/{id}")
    public String deleteAddress(@PathVariable("id") Long id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        KhachHang customer = resolveCustomer(session);
        if (customer == null) {
            return "redirect:/login?error=login_required&redirect=/shop/profile";
        }

        try {
            khachHangService.deleteAddress(customer.getMaKH(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa địa chỉ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/shop/profile";
    }

    @PostMapping("/shop/profile/address/default/{id}")
    public String setDefaultAddress(@PathVariable("id") Long id,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        KhachHang customer = resolveCustomer(session);
        if (customer == null) {
            return "redirect:/login?error=login_required&redirect=/shop/profile";
        }

        try {
            khachHangService.setDefaultAddress(customer.getMaKH(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đặt làm địa chỉ mặc định!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/shop/profile";
    }
}
