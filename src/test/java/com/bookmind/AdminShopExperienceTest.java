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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminShopExperienceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private SachRepository sachRepository;

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("Admin account has an associated KhachHang and GioHang profile")
    void testAdminHasKhachHangProfile() {
        TaiKhoan admin = taiKhoanRepository.findByTenDN("admin").orElseThrow();
        assertThat(admin.getVaiTro()).isEqualTo("ADMIN");

        KhachHang khAdmin = authService.getOrCreateKhachHangForTaiKhoan(admin);
        assertThat(khAdmin).isNotNull();
        assertThat(khAdmin.getTaiKhoan().getMaTK()).isEqualTo(admin.getMaTK());
        assertThat(khAdmin.getHoTen()).isEqualTo("Quản trị viên");
    }

    @Test
    @DisplayName("Admin can view shopping cart page without being bounced to /admin")
    void testAdminCanViewCart() throws Exception {
        TaiKhoan admin = taiKhoanRepository.findByTenDN("admin").orElseThrow();
        KhachHang khAdmin = authService.getOrCreateKhachHangForTaiKhoan(admin);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_USER, admin);
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, khAdmin);

        mockMvc.perform(get("/shop/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/cart"))
                .andExpect(model().attributeExists("items", "subtotal"));
    }

    @Test
    @DisplayName("Admin can add book to cart like a customer")
    void testAdminCanAddToCart() throws Exception {
        TaiKhoan admin = taiKhoanRepository.findByTenDN("admin").orElseThrow();
        KhachHang khAdmin = authService.getOrCreateKhachHangForTaiKhoan(admin);
        Sach book = sachRepository.findAll().stream().findFirst().orElseThrow();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_USER, admin);
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, khAdmin);

        mockMvc.perform(post("/shop/cart/add")
                        .param("maSach", book.getMaSach().toString())
                        .param("soLuong", "1")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Admin can view 'My Orders' page without being bounced to /admin")
    void testAdminCanViewMyOrders() throws Exception {
        TaiKhoan admin = taiKhoanRepository.findByTenDN("admin").orElseThrow();
        KhachHang khAdmin = authService.getOrCreateKhachHangForTaiKhoan(admin);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_USER, admin);
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, khAdmin);

        mockMvc.perform(get("/shop/my-orders").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/my-orders"))
                .andExpect(model().attributeExists("orderPage"));
    }

    @Test
    @DisplayName("AuthInterceptor auto-populates KhachHang profile if admin session only had CURRENT_USER")
    void testAuthInterceptorAutoPopulatesCustomerForAdmin() throws Exception {
        TaiKhoan admin = taiKhoanRepository.findByTenDN("admin").orElseThrow();

        // Giả lập session của admin chỉ có CURRENT_USER mà chưa có CURRENT_CUSTOMER
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_USER, admin);

        // Truy cập /shop/cart, AuthInterceptor phải tự động kích hoạt tạo/lấy KhachHang
        mockMvc.perform(get("/shop/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/cart"));

        assertThat(session.getAttribute(AuthInterceptor.SESSION_CUSTOMER)).isNotNull();
    }
}
