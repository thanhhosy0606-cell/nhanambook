package com.bookmind;

import com.bookmind.entity.*;
import com.bookmind.repository.*;
import com.bookmind.security.AuthInterceptor;
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
public class BookReviewTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SachRepository sachRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private DanhGiaRepository danhGiaRepository;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;

    @Autowired
    private DiaChiRepository diaChiRepository;

    @Autowired
    private com.bookmind.service.DanhGiaService danhGiaService;

    private DonHang createPurchase(KhachHang kh, Sach sach) {
        DonHang dh = new DonHang();
        dh.setKhachHang(kh);
        dh.setNgayDat(java.time.LocalDateTime.now());
        dh.setTongTien(sach.getGiaBan());
        dh.setGiaTamTinh(sach.getGiaBan());
        dh.setPhiVC(java.math.BigDecimal.ZERO);
        dh.setGiamGia(java.math.BigDecimal.ZERO);
        dh.setTrangThai("DA_GIAO");
        dh = donHangRepository.save(dh);

        ChiTietDonHang ct = new ChiTietDonHang();
        ct.setId(new ChiTietDonHangId(dh.getMaDH(), sach.getMaSach()));
        ct.setDonHang(dh);
        ct.setSach(sach);
        ct.setSoLuong(1);
        ct.setDonGia(sach.getGiaBan());
        ct = chiTietDonHangRepository.saveAndFlush(ct);
        dh.getChiTietList().add(ct);
        return dh;
    }

    private Sach pickUnreviewedBook(KhachHang kh) {
        return sachRepository.findAll().stream()
                .filter(s -> danhGiaRepository.findByKhachHang_MaKHAndSach_MaSach(kh.getMaKH(), s.getMaSach()).isEmpty())
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("Submit review without login redirects to login page")
    void testSubmitReviewWithoutLogin() throws Exception {
        Sach sach = sachRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(post("/shop/book/" + sach.getMaSach() + "/rate")
                        .param("soSao", "5")
                        .param("noiDung", "Tuyệt vời"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=login_required&redirect=/shop/book/" + sach.getMaSach()));
    }

    @Test
    @DisplayName("Submit review successfully without maSach in request body (maSach from URL path)")
    void testSubmitReviewSuccessWithoutMaSachInBody() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Sach sach = pickUnreviewedBook(kh);
        createPurchase(kh, sach);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);

        mockMvc.perform(post("/shop/book/" + sach.getMaSach() + "/rate")
                        .session(session)
                        .param("soSao", "5")
                        .param("noiDung", "Sách rất hay và giao nhanh"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/book/" + sach.getMaSach()))
                .andExpect(flash().attributeExists("successMessage"));

        var reviewOpt = danhGiaRepository.findByKhachHang_MaKHAndSach_MaSach(kh.getMaKH(), sach.getMaSach());
        assertThat(reviewOpt).isPresent();
        assertThat(reviewOpt.get().getSoSao()).isEqualTo(5);
        assertThat(reviewOpt.get().getNoiDung()).isEqualTo("Sách rất hay và giao nhanh");

        // Verify another user can see this review in the list
        var reviews = danhGiaService.getRatingsForBook(sach.getMaSach());
        assertThat(reviews).anyMatch(r -> r.getNoiDung().equals("Sách rất hay và giao nhanh"));
    }

    @Test
    @DisplayName("Submit review with invalid soSao triggers validation error instead of 400 Whitelabel")
    void testSubmitReviewInvalidStars() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Sach sach = pickUnreviewedBook(kh);
        createPurchase(kh, sach);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);

        mockMvc.perform(post("/shop/book/" + sach.getMaSach() + "/rate")
                        .session(session)
                        .param("soSao", "10")
                        .param("noiDung", "Không hợp lệ"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/book/" + sach.getMaSach()))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Customer who has NOT purchased the book cannot review")
    void testCustomerWithoutPurchaseCannotReview() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Sach sach = sachRepository.findAll().stream()
                .filter(s -> !chiTietDonHangRepository.hasPurchasedBook(kh.getMaKH(), s.getMaSach()))
                .findFirst()
                .orElseThrow();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);

        mockMvc.perform(post("/shop/book/" + sach.getMaSach() + "/rate")
                        .session(session)
                        .param("soSao", "5")
                        .param("noiDung", "Thử đánh giá khi chưa mua"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/book/" + sach.getMaSach()))
                .andExpect(flash().attribute("errorMessage", "Chỉ những đơn hàng đã giao thành công mới được đánh giá!"));
    }

    @Test
    @DisplayName("Customer can rate book directly under the book on order-detail page")
    void testRateBookDirectlyFromOrder() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Sach sach = pickUnreviewedBook(kh);
        DonHang order = createPurchase(kh, sach);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);
        session.setAttribute(AuthInterceptor.SESSION_USER, kh.getTaiKhoan());

        // Submit rating from order
        mockMvc.perform(post("/shop/order/" + order.getMaDH() + "/rate-book/" + sach.getMaSach())
                        .session(session)
                        .param("soSao", "5")
                        .param("noiDung", "Đánh giá từ chi tiết đơn hàng"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/order/" + order.getMaDH()))
                .andExpect(flash().attributeExists("successMessage"));

        // Verify review exists in DB
        var reviewOpt = danhGiaRepository.findByKhachHang_MaKHAndSach_MaSach(kh.getMaKH(), sach.getMaSach());
        assertThat(reviewOpt).isPresent();
        assertThat(reviewOpt.get().getSoSao()).isEqualTo(5);
        assertThat(reviewOpt.get().getNoiDung()).isEqualTo("Đánh giá từ chi tiết đơn hàng");

        // Verify order detail page reflects the rating in userRatingsMap
        mockMvc.perform(get("/shop/order/" + order.getMaDH()).session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("userRatingsMap"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bạn đã đánh giá")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đánh giá từ chi tiết đơn hàng")));
    }

    @Test
    @DisplayName("Customer can only review a book once (second attempt is rejected)")
    void testCustomerCanOnlyReviewOnce() throws Exception {
        KhachHang kh1 = khachHangRepository.findAll().get(0);
        Sach sach = pickUnreviewedBook(kh1);
        createPurchase(kh1, sach);

        MockHttpSession sessionUser1 = new MockHttpSession();
        sessionUser1.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh1);
        sessionUser1.setAttribute(AuthInterceptor.SESSION_USER, kh1.getTaiKhoan());

        // 1. Submit review lần đầu -> Thành công
        mockMvc.perform(post("/shop/book/" + sach.getMaSach() + "/rate")
                        .session(sessionUser1)
                        .param("soSao", "4")
                        .param("noiDung", "Nhận xét ban đầu"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        // 2. Kiểm tra DB đã lưu đúng và có ngayDanhGia không null
        var reviewOpt = danhGiaRepository.findByKhachHang_MaKHAndSach_MaSach(kh1.getMaKH(), sach.getMaSach());
        assertThat(reviewOpt).isPresent();
        assertThat(reviewOpt.get().getNgayDanhGia()).isNotNull();

        // 3. Submit review lần 2 -> Bị từ chối vì mỗi sách chỉ được đánh giá 1 lần
        mockMvc.perform(post("/shop/book/" + sach.getMaSach() + "/rate")
                        .session(sessionUser1)
                        .param("soSao", "5")
                        .param("noiDung", "Thử đánh giá lần hai"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "Bạn đã đánh giá cuốn sách này rồi. Mỗi cuốn sách chỉ được đánh giá 1 lần!"));

        // 4. Nội dung và số sao trong DB không bị thay đổi
        var originalReview = danhGiaRepository.findByKhachHang_MaKHAndSach_MaSach(kh1.getMaKH(), sach.getMaSach()).orElseThrow();
        assertThat(originalReview.getSoSao()).isEqualTo(4);
        assertThat(originalReview.getNoiDung()).isEqualTo("Nhận xét ban đầu");
    }
}
