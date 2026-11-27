package com.bookmind;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.bookmind.entity.DonHang;
import com.bookmind.entity.KhachHang;
import com.bookmind.entity.PtthThanhToan;
import com.bookmind.entity.TaiKhoan;
import com.bookmind.entity.ThanhToan;
import com.bookmind.repository.DonHangRepository;
import com.bookmind.repository.KhachHangRepository;
import com.bookmind.repository.PtthThanhToanRepository;
import com.bookmind.repository.SachRepository;
import com.bookmind.repository.TaiKhoanRepository;
import com.bookmind.repository.ThanhToanRepository;
import com.bookmind.security.AuthInterceptor;
import com.bookmind.service.GioHangService;
import com.bookmind.service.MoMoService;

@SpringBootTest(properties = {
        "momo.access-key=TEST_MOMO_ACCESS_KEY",
        "momo.secret-key=TEST_MOMO_SECRET_KEY"
})
@AutoConfigureMockMvc
public class OrderCancelAndChangePaymentTest {

    private static final String TEST_MOMO_ACCESS_KEY = "TEST_MOMO_ACCESS_KEY";
    private static final String TEST_MOMO_SECRET_KEY = "TEST_MOMO_SECRET_KEY";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private ThanhToanRepository thanhToanRepository;

    @Autowired
    private PtthThanhToanRepository ptthThanhToanRepository;

    @Autowired
    private SachRepository sachRepository;

    @Autowired
    private GioHangService gioHangService;

    @Autowired
    private MoMoService moMoService;

    @Test
    @DisplayName("PayOS cancel callback displays cancellation banner and payment methods")
    void testPayOsCancelCallback() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        TaiKhoan tk = taiKhoanRepository.findById(kh.getTaiKhoan().getMaTK()).orElseThrow();
        DonHang order = donHangRepository.findByKhachHang_MaKHOrderByNgayDatDesc(kh.getMaKH(), org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream().findFirst().orElseThrow();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);
        session.setAttribute(AuthInterceptor.SESSION_USER, tk);

        mockMvc.perform(get("/shop/order/" + order.getMaDH())
                        .param("cancel", "true")
                        .param("status", "CANCELLED")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isCancelled", true))
                .andExpect(model().attributeExists("paymentMethods"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("hủy giao dịch trên cổng PayOS")))
                .andExpect(content().string(org.hamcrest.Matchers.containsStringIgnoringCase("chọn phương thức thanh toán khác")));
    }

    @Test
    @DisplayName("Customer can change payment method to COD after cancelling PayOS")
    void testChangePaymentMethodToCOD() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        TaiKhoan tk = taiKhoanRepository.findById(kh.getTaiKhoan().getMaTK()).orElseThrow();
        DonHang order = donHangRepository.findByKhachHang_MaKHOrderByNgayDatDesc(kh.getMaKH(), org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream().findFirst().orElseThrow();

        PtthThanhToan codMethod = ptthThanhToanRepository.findAll().stream()
                .filter(p -> p.getTenPTTT() != null && (p.getTenPTTT().toLowerCase().contains("tiền mặt") || p.getTenPTTT().toLowerCase().contains("tien mat") || p.getTenPTTT().toUpperCase().contains("COD")))
                .findFirst().orElseGet(() -> ptthThanhToanRepository.findAll().get(0));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);
        session.setAttribute(AuthInterceptor.SESSION_USER, tk);

        mockMvc.perform(post("/shop/order/" + order.getMaDH() + "/change-payment-method")
                        .param("maPTTT", codMethod.getMaPTTT().toString())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/order/" + order.getMaDH()))
                .andExpect(flash().attributeExists("successMessage"));

        List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDHOrderByMaThanhToanDesc(order.getMaDH());
        assertThat(payments).isNotEmpty();
        assertThat(payments.get(0).getPtthThanhToan().getMaPTTT()).isEqualTo(codMethod.getMaPTTT());

        DonHang updatedOrder = donHangRepository.findById(order.getMaDH()).orElseThrow();
        assertThat(updatedOrder.getTrangThai()).isEqualTo("DA_XAC_NHAN");

        mockMvc.perform(get("/shop/order/" + order.getMaDH()).session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isCod", true))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thanh toán khi nhận hàng (COD)")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("payment-switch-item"))));
    }

    @Test
    @DisplayName("VNPay payment method is deactivated and not present in active list")
    void testVNPayIsDeactivatedAndNotActive() {
        List<PtthThanhToan> activeMethods = ptthThanhToanRepository.findByTrangThai("HOAT_DONG");
        boolean hasVNPay = activeMethods.stream().anyMatch(p -> "VNPay".equalsIgnoreCase(p.getTenPTTT()));
        assertThat(hasVNPay).isFalse();
    }

    @Test
    @DisplayName("Changing payment method to MoMo redirects to dedicated QR payment page")
    void testChangePaymentMethodToMoMo() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        TaiKhoan tk = taiKhoanRepository.findById(kh.getTaiKhoan().getMaTK()).orElseThrow();
        DonHang order = donHangRepository.findByKhachHang_MaKHOrderByNgayDatDesc(kh.getMaKH(), org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream().findFirst().orElseThrow();

        PtthThanhToan momoMethod = ptthThanhToanRepository.findAll().stream()
                .filter(p -> "MoMo".equalsIgnoreCase(p.getTenPTTT()))
                .findFirst().orElseThrow();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);
        session.setAttribute(AuthInterceptor.SESSION_USER, tk);

        mockMvc.perform(post("/shop/order/" + order.getMaDH() + "/change-payment-method")
                        .param("maPTTT", momoMethod.getMaPTTT().toString())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/order/" + order.getMaDH() + "/payment"));

        // /payment mở màn hình thanh toán riêng biệt cho MoMo
        mockMvc.perform(get("/shop/order/" + order.getMaDH() + "/payment")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/payment"))
                .andExpect(model().attributeExists("order", "payment", "paymentQrUrl", "isMoMo"));
    }

    @Test
    @DisplayName("Changing payment method to VietQR redirects to dedicated QR payment page")
    void testChangePaymentMethodToVietQR() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        TaiKhoan tk = taiKhoanRepository.findById(kh.getTaiKhoan().getMaTK()).orElseThrow();
        DonHang order = donHangRepository.findByKhachHang_MaKHOrderByNgayDatDesc(kh.getMaKH(), org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream().findFirst().orElseThrow();

        PtthThanhToan vietqrMethod = ptthThanhToanRepository.findAll().stream()
                .filter(p -> "VietQR".equalsIgnoreCase(p.getTenPTTT()))
                .findFirst().orElseThrow();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);
        session.setAttribute(AuthInterceptor.SESSION_USER, tk);

        mockMvc.perform(post("/shop/order/" + order.getMaDH() + "/change-payment-method")
                        .param("maPTTT", vietqrMethod.getMaPTTT().toString())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/order/" + order.getMaDH() + "/payment"));

        // /payment mở màn hình thanh toán riêng biệt cho VietQR
        mockMvc.perform(get("/shop/order/" + order.getMaDH() + "/payment")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/payment"))
                .andExpect(model().attributeExists("order", "payment", "paymentQrUrl", "isVietQr"));
    }

    @Test
    @DisplayName("MoMo IPN webhook verifies signature and updates order status")
    void testMoMoIpnSuccess() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        DonHang order = donHangRepository.findByKhachHang_MaKHOrderByNgayDatDesc(kh.getMaKH(), org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream().findFirst().orElseThrow();

        String orderId = "DH" + order.getMaDH() + "_123456";
        String requestId = "MOMO_REQ_" + order.getMaDH() + "_123456";
        String amount = order.getTongTien().toBigInteger().toString();
        String message = "Thành công";
        String orderInfo = "Thanh toan don hang #" + order.getMaDH();
        String orderType = "momo_wallet";
        String partnerCode = "MOMO";
        String payType = "qr";
        String responseTime = "1720000000000";
        String resultCode = "0";
        String transId = "9988776655";
        String extraData = String.valueOf(order.getMaDH());

        String rawSignature = "accessKey=" + TEST_MOMO_ACCESS_KEY + "&amount=" + amount +
                "&extraData=" + extraData +
                "&message=" + message +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&orderType=" + orderType +
                "&partnerCode=" + partnerCode +
                "&payType=" + payType +
                "&requestId=" + requestId +
                "&responseTime=" + responseTime +
                "&resultCode=" + resultCode +
                "&transId=" + transId;

        String signature = MoMoService.hmacSha256(rawSignature, TEST_MOMO_SECRET_KEY);

        String requestJson = String.format("{" +
                "\"partnerCode\":\"%s\"," +
                "\"orderId\":\"%s\"," +
                "\"requestId\":\"%s\"," +
                "\"amount\":%s," +
                "\"orderInfo\":\"%s\"," +
                "\"orderType\":\"%s\"," +
                "\"transId\":%s," +
                "\"resultCode\":%s," +
                "\"message\":\"%s\"," +
                "\"payType\":\"%s\"," +
                "\"responseTime\":%s," +
                "\"extraData\":\"%s\"," +
                "\"signature\":\"%s\"" +
                "}", partnerCode, orderId, requestId, amount, orderInfo, orderType, transId, resultCode, message, payType, responseTime, extraData, signature);

        mockMvc.perform(post("/api/payment/momo/ipn")
                        .contentType("application/json")
                        .content(requestJson))
                .andExpect(status().isNoContent());

        List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDHOrderByMaThanhToanDesc(order.getMaDH());
        assertThat(payments).isNotEmpty();
        assertThat(payments.get(0).getTrangThai()).isEqualTo("THANH_CONG");
        assertThat(payments.get(0).getMaGiaoDich()).isEqualTo(transId);
    }

    @Test
    @DisplayName("MoMo return URL handles customer redirect and verifies signature")
    void testMoMoReturnUrlSuccess() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        TaiKhoan tk = taiKhoanRepository.findById(kh.getTaiKhoan().getMaTK()).orElseThrow();
        DonHang order = donHangRepository.findByKhachHang_MaKHOrderByNgayDatDesc(kh.getMaKH(), org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream().findFirst().orElseThrow();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_CUSTOMER, kh);
        session.setAttribute(AuthInterceptor.SESSION_USER, tk);

        String orderId = "DH" + order.getMaDH() + "_987654";
        String requestId = "MOMO_REQ_" + order.getMaDH() + "_987654";
        String amount = order.getTongTien().toBigInteger().toString();
        String message = "Thành công";
        String orderInfo = "Thanh toan don hang #" + order.getMaDH();
        String orderType = "momo_wallet";
        String partnerCode = "MOMO";
        String payType = "qr";
        String responseTime = "1720000000000";
        String resultCode = "0";
        String transId = "8877665544";
        String extraData = String.valueOf(order.getMaDH());

        String rawSignature = "accessKey=" + TEST_MOMO_ACCESS_KEY + "&amount=" + amount +
                "&extraData=" + extraData +
                "&message=" + message +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&orderType=" + orderType +
                "&partnerCode=" + partnerCode +
                "&payType=" + payType +
                "&requestId=" + requestId +
                "&responseTime=" + responseTime +
                "&resultCode=" + resultCode +
                "&transId=" + transId;

        String signature = MoMoService.hmacSha256(rawSignature, TEST_MOMO_SECRET_KEY);

        mockMvc.perform(get("/shop/order/momo-return")
                        .param("partnerCode", partnerCode)
                        .param("orderId", orderId)
                        .param("requestId", requestId)
                        .param("amount", amount)
                        .param("orderInfo", orderInfo)
                        .param("orderType", orderType)
                        .param("transId", transId)
                        .param("resultCode", resultCode)
                        .param("message", message)
                        .param("payType", payType)
                        .param("responseTime", responseTime)
                        .param("extraData", extraData)
                        .param("signature", signature)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/order/" + order.getMaDH() + "/invoice"))
                .andExpect(flash().attributeExists("successMessage"));

        // Xác nhận màn hình hóa đơn hiển thị chuẩn xác
        mockMvc.perform(get("/shop/order/" + order.getMaDH() + "/invoice").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/invoice"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HÓA ĐƠN BÁN HÀNG")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ĐÃ THANH TOÁN")));
    }
}

