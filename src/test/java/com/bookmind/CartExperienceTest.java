package com.bookmind;

import com.bookmind.entity.*;
import com.bookmind.repository.*;
import com.bookmind.security.AuthInterceptor;
import com.bookmind.service.GioHangService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CartExperienceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SachRepository sachRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private GioHangService gioHangService;

    @Test
    @DisplayName("Thêm 2 sách khác nhau vào giỏ hàng và cập nhật số lượng thành công")
    void testAddMultipleBooksAndUpdateQuantity() throws Exception {
        List<Sach> books = sachRepository.findAll();
        assertThat(books.size()).isGreaterThanOrEqualTo(2);
        Sach book1 = books.get(0);
        Sach book2 = books.get(1);

        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);
        session.setAttribute(AuthInterceptor.SESSION_USER, kh.getTaiKhoan());

        // 1. Thêm cuốn sách 1 với số lượng 1
        mockMvc.perform(post("/shop/cart/add")
                        .session(session)
                        .param("maSach", book1.getMaSach().toString())
                        .param("soLuong", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/cart"));

        // 2. Thêm cuốn sách 2 với số lượng 2
        mockMvc.perform(post("/shop/cart/add")
                        .session(session)
                        .param("maSach", book2.getMaSach().toString())
                        .param("soLuong", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/cart"));

        // 3. Kiểm tra xem giỏ hàng có 2 cuốn và subtotal đúng
        List<ChiTietGioHang> items = gioHangService.getCartItems(kh);
        assertThat(items).hasSize(2);

        BigDecimal expectedSubtotal = book1.getGiaBan().multiply(BigDecimal.valueOf(1))
                .add(book2.getGiaBan().multiply(BigDecimal.valueOf(2)));
        BigDecimal subtotal = gioHangService.calculateSubtotal(items);
        assertThat(subtotal).isEqualByComparingTo(expectedSubtotal);

        // 4. Cập nhật số lượng cuốn sách 1 lên 6
        mockMvc.perform(post("/shop/cart/update")
                        .session(session)
                        .param("maSach", book1.getMaSach().toString())
                        .param("soLuong", "6"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/cart"));

        items = gioHangService.getCartItems(kh);
        ChiTietGioHang item1 = items.stream()
                .filter(i -> i.getSach().getMaSach().equals(book1.getMaSach()))
                .findFirst().orElseThrow();
        assertThat(item1.getSoLuong()).isEqualTo(6);

        BigDecimal expectedSubtotalAfterUpdate = book1.getGiaBan().multiply(BigDecimal.valueOf(6))
                .add(book2.getGiaBan().multiply(BigDecimal.valueOf(2)));
        assertThat(gioHangService.calculateSubtotal(items)).isEqualByComparingTo(expectedSubtotalAfterUpdate);
    }
}
