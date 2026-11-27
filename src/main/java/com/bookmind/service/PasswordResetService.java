package com.bookmind.service;

import com.bookmind.entity.TaiKhoan;
import com.bookmind.repository.TaiKhoanRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PasswordResetService(TaiKhoanRepository taiKhoanRepository, PasswordEncoder passwordEncoder) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public static final int MAX_OTP_ATTEMPTS = 5;
    public static final long RESEND_COOLDOWN_SECONDS = 60;

    public record ResetToken(String email, String otpCode, LocalDateTime expiresAt,
                             LocalDateTime createdAt, java.util.concurrent.atomic.AtomicInteger attempts) {
        public ResetToken(String email, String otpCode, LocalDateTime expiresAt) {
            this(email, otpCode, expiresAt, LocalDateTime.now(), new java.util.concurrent.atomic.AtomicInteger(0));
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    private final Map<String, ResetToken> tokenStore = new ConcurrentHashMap<>();

    public boolean isMailConfigured() {
        return mailUsername != null && !mailUsername.isBlank()
                && !mailUsername.contains("your-email@gmail.com");
    }

    public record SendOtpResult(boolean emailSent, String otp, String message) {}

    public SendOtpResult sendResetCode(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ email.");
        }
        String cleanEmail = email.trim().toLowerCase();

        // Chống spam gửi mail: giới hạn 60 giây giữa các lần gửi lại OTP
        ResetToken existing = tokenStore.get(cleanEmail);
        if (existing != null && !existing.isExpired()
                && existing.createdAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Vui lòng đợi 60 giây trước khi yêu cầu gửi lại mã OTP mới!");
        }

        TaiKhoan tk = taiKhoanRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ email '" + cleanEmail + "' chưa được đăng ký trong hệ thống!"));

        if ("KHOA".equalsIgnoreCase(tk.getTrangThai())) {
            throw new IllegalStateException("Tài khoản của bạn đang bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        // Tạo mã OTP 6 chữ số ngẫu nhiên an toàn
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        tokenStore.put(cleanEmail, new ResetToken(cleanEmail, otp, LocalDateTime.now().plusMinutes(10)));
        log.info("Generated password reset OTP for {}: {} (valid 10 mins)", cleanEmail, otp);

        if (!isMailConfigured() || mailSender == null) {
            log.warn("Gmail SMTP is not configured in application.properties! Fallback demo OTP: {}", otp);
            return new SendOtpResult(false, otp,
                    "Mã xác thực OTP của bạn là: " + otp + " (Chế độ thử nghiệm do chưa cấu hình tài khoản Gmail gửi thư của Nhà Sách).");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailUsername, "Nhã Nam BookStore");
            helper.setTo(cleanEmail);
            helper.setSubject("[Nhã Nam BookStore] Mã xác nhận đặt lại mật khẩu của bạn: " + otp);

            String html = """
                <div style="max-width: 580px; margin: 0 auto; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #ffffff; border-radius: 16px; overflow: hidden; border: 1px solid #e2e8f0; box-shadow: 0 4px 20px rgba(0,0,0,0.05);">
                    <div style="background: linear-gradient(135deg, #006a31 0%, #0d8a43 100%); padding: 32px 24px; text-align: center; color: #ffffff;">
                        <h2 style="margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px;">NHÃ NAM BOOKSTORE</h2>
                        <p style="margin: 8px 0 0; font-size: 14px; opacity: 0.9;">Yêu cầu khôi phục mật khẩu tài khoản</p>
                    </div>
                    <div style="padding: 32px 28px; color: #334155; line-height: 1.6;">
                        <p style="font-size: 15px; margin-top: 0;">Xin chào <strong>{{USER_NAME}}</strong>,</p>
                        <p style="font-size: 15px;">Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản liên kết với email này. Vui lòng sử dụng mã xác thực bên dưới để hoàn tất:</p>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <div style="display: inline-block; background-color: #f0fdf4; border: 2px dashed #16a34a; border-radius: 12px; padding: 16px 36px;">
                                <span style="font-family: 'Consolas', monospace; font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #006a31;">{{OTP_CODE}}</span>
                            </div>
                            <p style="margin: 10px 0 0; font-size: 13px; color: #64748b;">Mã xác thực có hiệu lực trong <strong>10 phút</strong></p>
                        </div>

                        <p style="font-size: 14px; color: #64748b;">Nếu bạn không gửi yêu cầu này, vui lòng bỏ qua email. Mật khẩu hiện tại của bạn vẫn được an toàn 100%.</p>
                        
                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 28px 0;">
                        <p style="font-size: 12px; color: #94a3b8; text-align: center; margin-bottom: 0;">Đây là email tự động từ hệ thống Nhã Nam BookStore. Vui lòng không trả lời thư này.</p>
                    </div>
                </div>
                """
                .replace("{{USER_NAME}}", tk.getTenDN() != null ? tk.getTenDN() : cleanEmail)
                .replace("{{OTP_CODE}}", otp);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Password reset email sent successfully to {}", cleanEmail);
            return new SendOtpResult(true, otp,
                    "Mã xác thực gồm 6 chữ số đã được gửi đến email " + cleanEmail + ". Vui lòng kiểm tra hộp thư đến (hoặc thư mục Spam)!");
        } catch (Exception e) {
            log.error("Failed to send reset email to {}: {}", cleanEmail, e.getMessage(), e);
            return new SendOtpResult(false, otp,
                    "Gửi thư qua Gmail không thành công (" + e.getMessage() + "). Mã xác nhận OTP tạm thời của bạn là: " + otp);
        }
    }

    public boolean verifyOtp(String email, String otp) {
        if (email == null || otp == null) return false;
        String cleanEmail = email.trim().toLowerCase();
        ResetToken token = tokenStore.get(cleanEmail);
        if (token == null || token.isExpired()) {
            return false;
        }
        if (token.otpCode().equals(otp.trim())) {
            return true;
        }
        int failedCount = token.attempts().incrementAndGet();
        if (failedCount >= MAX_OTP_ATTEMPTS) {
            tokenStore.remove(cleanEmail);
            log.warn("OTP invalidated for {} after {} failed attempts (brute-force protection)", cleanEmail, failedCount);
        }
        return false;
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword, String confirmPassword) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Xác nhận mật khẩu mới không trùng khớp.");
        }

        String cleanEmail = email.trim().toLowerCase();
        boolean hadToken = tokenStore.containsKey(cleanEmail);
        if (!verifyOtp(cleanEmail, otp)) {
            if (hadToken && !tokenStore.containsKey(cleanEmail)) {
                throw new IllegalArgumentException("Bạn đã nhập sai quá " + MAX_OTP_ATTEMPTS + " lần. Mã OTP đã bị hủy, vui lòng yêu cầu gửi lại mã mới!");
            }
            throw new IllegalArgumentException("Mã xác thực không chính xác hoặc đã hết hạn (10 phút). Vui lòng gửi lại mã mới!");
        }

        TaiKhoan tk = taiKhoanRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email này."));

        tk.setMatKhau(passwordEncoder.encode(newPassword));
        if ("GOOGLE".equalsIgnoreCase(tk.getPtDangNhap()) || "FACEBOOK".equalsIgnoreCase(tk.getPtDangNhap())) {
            tk.setPtDangNhap("LOCAL"); // Khách đã đặt mật khẩu riêng
        }
        taiKhoanRepository.save(tk);

        // Hủy OTP sau khi sử dụng thành công
        tokenStore.remove(cleanEmail);
        log.info("Password reset successfully for account {}", cleanEmail);
    }
}
