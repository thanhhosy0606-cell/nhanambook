package com.bookmind;

import com.bookmind.entity.*;
import com.bookmind.repository.*;
import com.bookmind.security.AuthInterceptor;
import com.bookmind.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CustomerProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private DiaChiRepository diaChiRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Accessing /shop/profile without authentication redirects to login")
    void testProfileRequiresLogin() throws Exception {
        mockMvc.perform(get("/shop/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=login_required&redirect=/shop/profile"));
    }

    @Test
    @DisplayName("Authenticated customer can view profile page")
    void testViewProfilePage() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        TaiKhoan tk = taiKhoanRepository.findById(kh.getTaiKhoan().getMaTK()).orElseThrow();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_USER, tk);
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);

        mockMvc.perform(get("/shop/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/profile"))
                .andExpect(model().attributeExists("user", "customer", "addresses", "orderCount"));
    }

    @Test
    @DisplayName("Customer can update profile info (hoTen, sdt, gioiTinh, ngaySinh)")
    void testUpdateProfileInfo() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        TaiKhoan tk = taiKhoanRepository.findById(kh.getTaiKhoan().getMaTK()).orElseThrow();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_USER, tk);
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);

        mockMvc.perform(post("/shop/profile")
                        .session(session)
                        .param("hoTen", "Nguyễn Văn Test")
                        .param("sdt", "0912345678")
                        .param("gioiTinh", "true")
                        .param("ngaySinh", "1995-10-20")
                        .param("anhDaiDien", "https://example.com/avatar-test.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/profile"))
                .andExpect(flash().attributeExists("successMessage"));

        KhachHang updated = khachHangRepository.findById(kh.getMaKH()).orElseThrow();
        assertThat(updated.getHoTen()).isEqualTo("Nguyễn Văn Test");
        assertThat(updated.getSdt()).isEqualTo("0912345678");
        assertThat(updated.getGioiTinh()).isTrue();
        assertThat(updated.getNgaySinh()).isEqualTo(LocalDate.of(1995, 10, 20));
        assertThat(updated.getAnhDaiDien()).isEqualTo("https://example.com/avatar-test.jpg");
    }

    @Test
    @DisplayName("Customer can add a new delivery address")
    void testAddDeliveryAddress() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        TaiKhoan tk = taiKhoanRepository.findById(kh.getTaiKhoan().getMaTK()).orElseThrow();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_USER, tk);
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);

        mockMvc.perform(post("/shop/profile/address/add")
                        .session(session)
                        .param("hoTenNguoiNhan", "Người nhận A")
                        .param("sdtNguoiNhan", "0988776655")
                        .param("diaChiNhan", "Số 123 Đường Sách, Q.1, TP.HCM")
                        .param("macDinh", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/profile"))
                .andExpect(flash().attributeExists("successMessage"));

        List<DiaChi> list = diaChiRepository.findByKhachHang_MaKH(kh.getMaKH());
        assertThat(list).anyMatch(d -> d.getDiaChiNhan().contains("Số 123 Đường Sách"));
    }
}
