package com.bookmind.controller;

import com.bookmind.dto.LoginRequest;
import com.bookmind.dto.OAuthUserInfo;
import com.bookmind.dto.RegisterRequest;
import com.bookmind.entity.KhachHang;
import com.bookmind.entity.TaiKhoan;
import com.bookmind.security.AuthInterceptor;
import com.bookmind.service.AuthService;
import com.bookmind.service.OAuthService;
import com.bookmind.service.PasswordResetService;
import com.bookmind.service.SavedCouponService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final OAuthService oAuthService;
    private final PasswordResetService passwordResetService;
    private final SavedCouponService savedCouponService;

    @Autowired
    public AuthController(AuthService authService,
                          OAuthService oAuthService,
                          PasswordResetService passwordResetService,
                          SavedCouponService savedCouponService) {
        this.authService = authService;
        this.oAuthService = oAuthService;
        this.passwordResetService = passwordResetService;
        this.savedCouponService = savedCouponService;
    }

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "redirect", required = false) String redirect,
                                @RequestParam(value = "error", required = false) String error,
                                Model model, HttpSession session) {
        String safeRedirect = sanitizeRedirect(redirect);
        if (session.getAttribute(AuthInterceptor.SESSION_USER) != null) {
            TaiKhoan tk = (TaiKhoan) session.getAttribute(AuthInterceptor.SESSION_USER);
            if (safeRedirect != null) {
                return "redirect:" + safeRedirect;
            }
            if ("ADMIN".equalsIgnoreCase(tk.getVaiTro())) {
                return "redirect:/admin";
            }
            return "redirect:/";
        }
        model.addAttribute("loginRequest", new LoginRequest());
        model.addAttribute("redirect", safeRedirect);
        if ("login_required".equals(error)) {
            model.addAttribute("errorMessage", "Vui lòng đăng nhập để tiếp tục.");
        } else if ("admin_required".equals(error)) {
            model.addAttribute("errorMessage", "Khu vực quản trị yêu cầu quyền Quản lý (Admin).");
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String handleLogin(@ModelAttribute("loginRequest") LoginRequest req,
                              @RequestParam(value = "redirect", required = false) String redirect,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        String safeRedirect = sanitizeRedirect(redirect);
        try {
            Optional<TaiKhoan> opt = authService.authenticate(req.getUsernameOrEmail(), req.getPassword());
            if (opt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Sai thông tin đăng nhập hoặc mật khẩu");
                return "redirect:/login" + (safeRedirect != null ? "?redirect=" + safeRedirect : "");
            }

            TaiKhoan tk = opt.get();
            session.setAttribute(AuthInterceptor.SESSION_USER, tk);

            KhachHang kh = authService.getOrCreateKhachHangForTaiKhoan(tk);
            if (kh != null) {
                session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);
                savedCouponService.mergeSessionCouponsToCustomer(session, kh);
            }

            if (safeRedirect != null) {
                return "redirect:" + safeRedirect;
            }

            if ("ADMIN".equalsIgnoreCase(tk.getVaiTro())) {
                return "redirect:/admin";
            }

            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login" + (safeRedirect != null ? "?redirect=" + safeRedirect : "");
        }
    }

    private String sanitizeRedirect(String redirect) {
        if (redirect == null || redirect.isBlank() || redirect.startsWith("/login")) {
            return null;
        }
        if (redirect.contains("/cancel-and-return-to-cart") || redirect.contains("/confirm-payment") || redirect.contains("/change-payment-method")) {
            int idx = redirect.indexOf("/shop/order/");
            if (idx >= 0) {
                String sub = redirect.substring(idx + "/shop/order/".length());
                String orderId = sub.replaceAll("/.*", "");
                if (!orderId.isBlank() && orderId.matches("\\d+")) {
                    return "/shop/order/" + orderId;
                }
            }
            return "/shop/cart";
        }
        return redirect;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model, HttpSession session) {
        if (session.getAttribute(AuthInterceptor.SESSION_USER) != null) {
            return "redirect:/";
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest req,
                                 BindingResult bindingResult,
                                 HttpSession session,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            TaiKhoan tk = authService.register(req);
            session.setAttribute(AuthInterceptor.SESSION_USER, tk);
            authService.getKhachHangByTaiKhoan(tk).ifPresent(kh -> {
                session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);
                savedCouponService.mergeSessionCouponsToCustomer(session, kh);
            });

            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký tài khoản thành công!");
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }

    // ========================================================
    // OAUTH 2.0 GOOGLE & FACEBOOK LOGIN
    // ========================================================

    @GetMapping("/oauth2/login/{provider}")
    public String oauth2Login(@PathVariable("provider") String provider,
                              @RequestParam(value = "redirect", required = false) String redirect,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        String prov = provider.toLowerCase();
        if ("google".equals(prov)) {
            if (oAuthService.isGoogleConfigured()) {
                session.setAttribute("OAUTH_TARGET_REDIRECT", redirect);
                return "redirect:" + oAuthService.buildGoogleAuthUrl(redirect);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Chưa cấu hình Google Client ID / Secret trong hệ thống. Vui lòng thiết lập GOOGLE_CLIENT_ID và GOOGLE_CLIENT_SECRET trong application.properties theo hướng dẫn Google Cloud Console.");
                return "redirect:/login" + (redirect != null ? "?redirect=" + redirect : "");
            }
        } else if ("facebook".equals(prov)) {
            if (oAuthService.isFacebookConfigured()) {
                session.setAttribute("OAUTH_TARGET_REDIRECT", redirect);
                return "redirect:" + oAuthService.buildFacebookAuthUrl(redirect);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Chưa cấu hình Facebook App ID / Secret trong hệ thống. Vui lòng thiết lập FACEBOOK_APP_ID và FACEBOOK_APP_SECRET trong application.properties theo hướng dẫn Meta for Developers.");
                return "redirect:/login" + (redirect != null ? "?redirect=" + redirect : "");
            }
        }

        redirectAttributes.addFlashAttribute("errorMessage", "Nhà cung cấp xác thực không được hỗ trợ");
        return "redirect:/login";
    }

    @GetMapping("/auth/google/callback")
    public String googleCallback(@RequestParam(value = "code", required = false) String code,
                                 @RequestParam(value = "error", required = false) String error,
                                 @RequestParam(value = "state", required = false) String state,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (error != null || code == null || code.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã hủy đăng nhập Google hoặc xảy ra lỗi xác thực (" + error + ")");
            return "redirect:/login";
        }

        try {
            OAuthUserInfo userInfo = oAuthService.processGoogleCallback(code);
            TaiKhoan tk = authService.processOAuthSuccess(userInfo.getEmail(), userInfo.getName(), userInfo.getAvatarUrl(), "GOOGLE");
            session.setAttribute(AuthInterceptor.SESSION_USER, tk);
            authService.getKhachHangByTaiKhoan(tk).ifPresent(kh -> {
                session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);
                savedCouponService.mergeSessionCouponsToCustomer(session, kh);
            });

            redirectAttributes.addFlashAttribute("successMessage", "Đăng nhập thành công với tài khoản Google (" + userInfo.getEmail() + ")!");
            String targetRedirect = (String) session.getAttribute("OAUTH_TARGET_REDIRECT");
            session.removeAttribute("OAUTH_TARGET_REDIRECT");
            if (targetRedirect != null && !targetRedirect.isBlank() && !targetRedirect.startsWith("/login")) {
                return "redirect:" + targetRedirect;
            }
            if (state != null && !state.isBlank() && !state.startsWith("/login")) {
                return "redirect:" + state;
            }
            return "redirect:/";
        } catch (Exception e) {
            log.error("Error processing Google OAuth callback: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đăng nhập Google: " + e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/auth/facebook/callback")
    public String facebookCallback(@RequestParam(value = "code", required = false) String code,
                                   @RequestParam(value = "error", required = false) String error,
                                   @RequestParam(value = "state", required = false) String state,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        if (error != null || code == null || code.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã hủy đăng nhập Facebook hoặc xảy ra lỗi xác thực (" + error + ")");
            return "redirect:/login";
        }

        try {
            OAuthUserInfo userInfo = oAuthService.processFacebookCallback(code);
            TaiKhoan tk = authService.processOAuthSuccess(userInfo.getEmail(), userInfo.getName(), userInfo.getAvatarUrl(), "FACEBOOK");
            session.setAttribute(AuthInterceptor.SESSION_USER, tk);
            authService.getKhachHangByTaiKhoan(tk).ifPresent(kh -> {
                session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);
                savedCouponService.mergeSessionCouponsToCustomer(session, kh);
            });

            redirectAttributes.addFlashAttribute("successMessage", "Đăng nhập thành công với tài khoản Facebook (" + userInfo.getName() + ")!");
            String targetRedirect = (String) session.getAttribute("OAUTH_TARGET_REDIRECT");
            session.removeAttribute("OAUTH_TARGET_REDIRECT");
            if (targetRedirect != null && !targetRedirect.isBlank() && !targetRedirect.startsWith("/login")) {
                return "redirect:" + targetRedirect;
            }
            if (state != null && !state.isBlank() && !state.startsWith("/login")) {
                return "redirect:" + state;
            }
            return "redirect:/";
        } catch (Exception e) {
            log.error("Error processing Facebook OAuth callback: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đăng nhập Facebook: " + e.getMessage());
            return "redirect:/login";
        }
    }

    // ========================================================
    // PASSWORD RESET VIA REAL GMAIL EMAIL (QUÊN MẬT KHẨU)
    // ========================================================

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email,
                                       RedirectAttributes redirectAttributes) {
        try {
            PasswordResetService.SendOtpResult result = passwordResetService.sendResetCode(email);
            if (result.emailSent()) {
                redirectAttributes.addFlashAttribute("successMessage", result.message());
            } else {
                redirectAttributes.addFlashAttribute("warningMessage", result.message());
                redirectAttributes.addFlashAttribute("demoOtp", result.otp());
            }
            return "redirect:/reset-password?email=" + URLEncoder.encode(email.trim().toLowerCase(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(value = "email", required = false) String email,
                                        Model model) {
        model.addAttribute("email", email != null ? email : "");
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("email") String email,
                                      @RequestParam("otp") String otp,
                                      @RequestParam("newPassword") String newPassword,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.resetPassword(email, otp, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt lại mật khẩu thành công! Quý khách có thể đăng nhập ngay bằng mật khẩu mới.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reset-password?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}
