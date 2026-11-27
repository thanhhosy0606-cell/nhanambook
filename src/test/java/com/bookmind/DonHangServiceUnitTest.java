package com.bookmind;

import com.bookmind.dto.CheckoutRequest;
import com.bookmind.entity.*;
import com.bookmind.repository.*;
import com.bookmind.service.DonHangService;
import com.bookmind.service.GioHangService;
import com.bookmind.service.MaGiamGiaService;
import com.bookmind.service.ThanhToanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test độc lập dùng JUnit 5 + Mockito (không phụ thuộc MySQL)
 * kiểm thử các nghiệp vụ cốt lõi của phân hệ Đặt hàng & Đơn hàng:
 * 1. Tính phí vận chuyển động theo khu vực / ngưỡng miễn phí / mã FREESHIP.
 * 2. Tự động hủy đơn quá hạn thanh toán (@Scheduled) kèm hoàn tồn kho & lượt dùng mã giảm giá.
 * 3. Chống âm kho khi đặt hàng đồng thời (Atomic Stock Update).
 */
@ExtendWith(MockitoExtension.class)
class DonHangServiceUnitTest {

    @Mock
    private DonHangRepository donHangRepository;
    @Mock
    private ChiTietDonHangRepository chiTietDonHangRepository;
    @Mock
    private GioHangService gioHangService;
    @Mock
    private SachRepository sachRepository;
    @Mock
    private DiaChiRepository diaChiRepository;
    @Mock
    private MaGiamGiaRepository maGiamGiaRepository;
    @Mock
    private MaGiamGiaService maGiamGiaService;
    @Mock
    private ThanhToanService thanhToanService;
    @Mock
    private ThanhToanRepository thanhToanRepository;

    @InjectMocks
    private DonHangService donHangService;

    @Test
    @DisplayName("Phí vận chuyển: Nội thành Hà Nội / TP.HCM = 15.000đ khi đơn < 300.000đ")
    void calculateShippingFee_metroHanoiAndHcm_returns15k() {
        BigDecimal subtotal = new BigDecimal("180000");

        assertThat(donHangService.calculateShippingFee(subtotal, "Số 10 Cầu Giấy, Hà Nội", null))
                .isEqualByComparingTo("15000");
        assertThat(donHangService.calculateShippingFee(subtotal, "123 Lê Lợi, Quận 1, TP. Hồ Chí Minh", null))
                .isEqualByComparingTo("15000");
    }

    @Test
    @DisplayName("Phí vận chuyển: Đà Nẵng / Hải Phòng / Cần Thơ & tỉnh lân cận = 25.000đ")
    void calculateShippingFee_standardRegion_returns25k() {
        BigDecimal subtotal = new BigDecimal("150000");

        assertThat(donHangService.calculateShippingFee(subtotal, "Quận Hải Châu, Đà Nẵng", null))
                .isEqualByComparingTo("25000");
        assertThat(donHangService.calculateShippingFee(subtotal, "Quận Ngô Quyền, Hải Phòng", null))
                .isEqualByComparingTo("25000");
        assertThat(donHangService.calculateShippingFee(subtotal, "TP. Bắc Ninh, Tỉnh Bắc Ninh", null))
                .isEqualByComparingTo("25000");
    }

    @Test
    @DisplayName("Phí vận chuyển: Tuyến tỉnh xa = 35.000đ")
    void calculateShippingFee_remoteRegion_returns35k() {
        BigDecimal subtotal = new BigDecimal("200000");

        assertThat(donHangService.calculateShippingFee(subtotal, "TP. Buôn Ma Thuột, Đắk Lắk", null))
                .isEqualByComparingTo("35000");
    }

    @Test
    @DisplayName("Phí vận chuyển: Miễn phí (0đ) khi tổng tạm tính >= 300.000đ hoặc dùng mã FREESHIP")
    void calculateShippingFee_freeThresholdOrFreeshipCoupon_returnsZero() {
        // Đơn >= 300.000đ ở tỉnh xa vẫn miễn phí ship
        assertThat(donHangService.calculateShippingFee(new BigDecimal("300000"), "TP. Pleiku, Gia Lai", null))
                .isEqualByComparingTo(BigDecimal.ZERO);

        // Đơn < 300.000đ nhưng áp dụng mã FREESHIP
        MaGiamGia freeship = new MaGiamGia();
        freeship.setMaCode("FREESHIP");
        freeship.setTenMa("Miễn phí vận chuyển toàn quốc");

        assertThat(donHangService.calculateShippingFee(new BigDecimal("120000"), "TP. Pleiku, Gia Lai", freeship))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Scheduler tự hủy đơn quá hạn: Chuyển DA_HUY, hoàn tồn kho sách, hoàn lượt mã giảm giá và đánh dấu thanh toán THAT_BAI")
    void autoCancelExpiredUnpaidOrders_restoresStockAndCouponAndMarksPaymentFailed() {
        // Arrange
        MaGiamGia coupon = new MaGiamGia();
        coupon.setMaMaGiamGia(10L);
        coupon.setMaCode("NHANAM20");
        coupon.setSoLuong(5);

        DonHang expiredOrder = new DonHang();
        expiredOrder.setMaDH(99L);
        expiredOrder.setTrangThai("CHO_XAC_NHAN");
        expiredOrder.setMaGiamGia(coupon);
        expiredOrder.setThoiGianHetHanTT(LocalDateTime.now().minusMinutes(5));

        Sach book = new Sach();
        book.setMaSach(7L);
        book.setTenSach("Nhà Giả Kim");
        book.setSoLuong(0);
        book.setTrangThai("HET_HANG");

        ChiTietDonHang item = new ChiTietDonHang();
        item.setDonHang(expiredOrder);
        item.setSach(book);
        item.setSoLuong(2);
        item.setDonGia(new BigDecimal("85000"));

        ThanhToan pendingPayment = new ThanhToan();
        pendingPayment.setMaThanhToan(500L);
        pendingPayment.setDonHang(expiredOrder);
        pendingPayment.setTrangThai("CHO_THANH_TOAN");

        when(donHangRepository.findExpiredOnlineUnpaidOrders(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(expiredOrder));
        when(donHangRepository.findById(99L)).thenReturn(Optional.of(expiredOrder));
        when(chiTietDonHangRepository.findByDonHang_MaDH(99L)).thenReturn(List.of(item));
        when(thanhToanRepository.findByDonHang_MaDH(99L)).thenReturn(List.of(pendingPayment));

        // Act
        int canceledCount = donHangService.autoCancelExpiredUnpaidOrders();

        // Assert
        assertThat(canceledCount).isEqualTo(1);
        assertThat(expiredOrder.getTrangThai()).isEqualTo("DA_HUY");
        assertThat(book.getSoLuong()).isEqualTo(2);
        assertThat(book.getTrangThai()).isEqualTo("DANG_BAN");
        assertThat(coupon.getSoLuong()).isEqualTo(6);
        assertThat(pendingPayment.getTrangThai()).isEqualTo("THAT_BAI");

        verify(sachRepository).save(book);
        verify(maGiamGiaRepository).save(coupon);
        verify(thanhToanRepository).save(pendingPayment);
        verify(donHangRepository).save(expiredOrder);
    }

    @Test
    @DisplayName("Đặt hàng đồng thời (Race Condition): Ném lỗi và dừng tạo đơn khi deductStockAtomic trả về 0")
    void createOrder_whenConcurrentStockExhausted_throwsException() {
        KhachHang kh = new KhachHang();
        kh.setMaKH(1L);
        kh.setHoTen("Nguyễn Văn A");

        Sach book = new Sach();
        book.setMaSach(15L);
        book.setTenSach("Hoàng Tử Bé");
        book.setTrangThai("DANG_BAN");
        book.setSoLuong(1); // Lúc đọc còn 1 cuốn
        book.setGiaBan(new BigDecimal("75000"));

        ChiTietGioHang cartItem = new ChiTietGioHang();
        cartItem.setSach(book);
        cartItem.setSoLuong(1);
        cartItem.setDonGia(new BigDecimal("75000"));

        CheckoutRequest req = new CheckoutRequest();
        req.setHoTenNguoiNhan("Nguyễn Văn A");
        req.setSdtNguoiNhan("0912345678");
        req.setDiaChiNhan("Hà Nội");
        req.setMaPTTT(1L);
        req.setSelectedItems(List.of(15L));

        PtthThanhToan cod = new PtthThanhToan();
        cod.setMaPTTT(1L);
        cod.setTenPTTT("Tiền mặt");

        when(gioHangService.getSelectedItems(eq(kh), anyList())).thenReturn(List.of(cartItem));
        when(gioHangService.calculateSubtotal(anyList())).thenReturn(new BigDecimal("75000"));
        when(diaChiRepository.findByKhachHang_MaKH(1L)).thenReturn(List.of());
        when(diaChiRepository.save(any(DiaChi.class))).thenAnswer(inv -> inv.getArgument(0));
        when(thanhToanService.getMethodById(1L)).thenReturn(cod);
        when(thanhToanService.isCodMethod("Tiền mặt")).thenReturn(true);
        when(donHangRepository.save(any(DonHang.class))).thenAnswer(inv -> {
            DonHang dh = inv.getArgument(0);
            dh.setMaDH(101L);
            return dh;
        });

        // Giả lập khách hàng khác vừa mua mất cuốn cuối cùng ngay trước câu UPDATE nguyên tử
        when(sachRepository.deductStockAtomic(15L, 1)).thenReturn(0);

        assertThatThrownBy(() -> donHangService.createOrder(kh, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("vừa hết hàng hoặc không đủ số lượng tồn kho");

        verify(chiTietDonHangRepository, never()).save(any());
        verify(thanhToanService, never()).createPayment(any(), any(), any());
    }
}
