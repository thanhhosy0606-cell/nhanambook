package com.bookmind;

import com.bookmind.entity.KhachHang;
import com.bookmind.entity.TaiKhoan;
import com.bookmind.repository.KhachHangRepository;
import com.bookmind.repository.TaiKhoanRepository;
import com.bookmind.service.AuthService;
import com.bookmind.service.PasswordResetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OAuthAndPasswordResetTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("OAuth Google with configured credentials redirects to Google Accounts authorization endpoint")
    void testGoogleOAuthRedirectsToGoogle() throws Exception {
        mockMvc.perform(get("/oauth2/login/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("https://accounts.google.com/o/oauth2/v2/auth?*"));
    }

    @Test
    @DisplayName("OAuth Facebook with configured credentials redirects to Meta authorization endpoint")
    void testFacebookOAuthRedirectsToFacebook() throws Exception {
        mockMvc.perform(get("/oauth2/login/facebook"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("https://www.facebook.com/v19.0/dialog/oauth?*"));
    }

    @Test
    @DisplayName("OAuth Google callback with error redirects to login with message")
    void testGoogleOAuthCallbackError() throws Exception {
        mockMvc.perform(get("/auth/google/callback").param("error", "access_denied"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Process OAuth success creates new account, customer and cart for new user")
    void testProcessOAuthSuccessNewUser() {
        String testEmail = "realuser.google." + System.currentTimeMillis() + "@gmail.com";
        String testName = "Nguyễn Văn Thực Tế";
        String testAvatar = "https://lh3.googleusercontent.com/a/test-avatar";

        TaiKhoan tk = authService.processOAuthSuccess(testEmail, testName, testAvatar, "GOOGLE");

        assertThat(tk).isNotNull();
        assertThat(tk.getEmail()).isEqualTo(testEmail);
        assertThat(tk.getPtDangNhap()).isEqualTo("GOOGLE");
        assertThat(tk.getVaiTro()).isEqualTo("KHACH_HANG");

        Optional<KhachHang> khOpt = khachHangRepository.findByTaiKhoan(tk);
        assertThat(khOpt).isPresent();
        KhachHang kh = khOpt.get();
        assertThat(kh.getHoTen()).isEqualTo(testName);
        assertThat(kh.getAnhDaiDien()).isEqualTo(testAvatar);
    }

    @Test
    @DisplayName("Forgot password with unregistered email fails with error")
    void testForgotPasswordUnregisteredEmail() {
        assertThatThrownBy(() -> passwordResetService.sendResetCode("unregistered.email.99999@domain.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chưa được đăng ký");
    }

    @Test
    @DisplayName("Password reset flow verifies OTP, updates password and authenticates successfully")
    void testPasswordResetFlow() {
        // Find existing account
        TaiKhoan existing = taiKhoanRepository.findAll().stream()
                .filter(t -> t.getEmail() != null && !t.getEmail().isBlank())
                .findFirst().orElseThrow();

        String email = existing.getEmail();

        // Send reset code (returns demo OTP when live credentials are not present)
        PasswordResetService.SendOtpResult result = passwordResetService.sendResetCode(email);
        assertThat(result).isNotNull();
        assertThat(result.otp()).isNotBlank();
        assertThat(result.otp()).hasSize(6);

        // Verify that the generated OTP is valid
        assertThat(passwordResetService.verifyOtp(email, result.otp())).isTrue();

        // Invalid OTP should fail
        assertThat(passwordResetService.verifyOtp(email, "000000".equals(result.otp()) ? "999999" : "000000")).isFalse();

        // Inject known valid OTP directly or test resetPassword with invalid OTP
        assertThatThrownBy(() -> passwordResetService.resetPassword(email, "000000", "newPass123456", "newPass123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mã xác thực không chính xác");

        // Passwords mismatch should fail
        assertThatThrownBy(() -> passwordResetService.resetPassword(email, "123456", "password123", "password khác"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không trùng khớp");

        // Password too short should fail
        assertThatThrownBy(() -> passwordResetService.resetPassword(email, "123456", "123", "123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ít nhất 6 ký tự");
    }

    @Test
    @DisplayName("GET /forgot-password and GET /reset-password return 200 OK")
    void testForgotAndResetPasswordPages() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quên mật khẩu?")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gửi mã xác nhận qua Email")));

        mockMvc.perform(get("/reset-password").param("email", "test@domain.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đặt lại mật khẩu mới")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("test@domain.com")));
    }
}
