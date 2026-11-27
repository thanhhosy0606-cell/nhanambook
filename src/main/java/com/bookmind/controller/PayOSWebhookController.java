package com.bookmind.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookmind.entity.DonHang;
import com.bookmind.entity.ThanhToan;
import com.bookmind.repository.DonHangRepository;
import com.bookmind.repository.ThanhToanRepository;
import com.bookmind.service.MoMoService;
import com.bookmind.service.PayOSService;
import vn.payos.model.webhooks.WebhookData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/payment")
public class PayOSWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PayOSWebhookController.class);

    private final PayOSService payOSService;
    private final MoMoService moMoService;
    private final DonHangRepository donHangRepository;
    private final ThanhToanRepository thanhToanRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public PayOSWebhookController(PayOSService payOSService,
                                  MoMoService moMoService,
                                  DonHangRepository donHangRepository,
                                  ThanhToanRepository thanhToanRepository) {
        this.payOSService = payOSService;
        this.moMoService = moMoService;
        this.donHangRepository = donHangRepository;
        this.thanhToanRepository = thanhToanRepository;
    }

    /**
     * Webhook endpoint nhận thông báo tự động từ PayOS ngay khi khách hàng chuyển tiền thành công
     */
    @PostMapping("/payos/webhook")
    @Transactional
    public ResponseEntity<Map<String, Object>> handlePayOSWebhook(@RequestBody Object body) {
        log.info("Received PayOS webhook notification: {}", body);
        try {
            WebhookData webhookData = payOSService.verifyWebhook(body);
            if (webhookData == null) {
                log.warn("Invalid PayOS webhook signature or PayOS unconfigured");
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid signature"));
            }

            Long orderCode = webhookData.getOrderCode();
            String code = webhookData.getCode();
            log.info("PayOS Webhook valid! orderCode={}, amount={}, code={}", orderCode, webhookData.getAmount(), code);

            if ("00".equals(code) || "PAID".equalsIgnoreCase(webhookData.getDesc())) {
                Long orderId = payOSService.resolveOrderId(orderCode);
                DonHang order = orderId != null ? donHangRepository.findById(orderId).orElse(null) : null;
                if (order != null) {
                    List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDH(orderId);
                    for (ThanhToan tt : payments) {
                        tt.setTrangThai("THANH_CONG");
                        tt.setThoiGianTT(LocalDateTime.now());
                        tt.setMaGiaoDich(webhookData.getReference() != null ? webhookData.getReference() : "PAYOS-" + orderCode);
                        thanhToanRepository.save(tt);
                    }
                    if ("CHO_XAC_NHAN".equals(order.getTrangThai())) {
                        order.setTrangThai("DA_XAC_NHAN");
                        donHangRepository.save(order);
                    }
                    log.info("Order #{} automatically marked as PAID via PayOS webhook! (orderCode={})", orderId, orderCode);
                }
            }

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Error processing PayOS webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Webhook endpoint IPN nhận thông báo tự động từ Cổng MoMo API v2
     */
    @PostMapping("/momo/ipn")
    @Transactional
    public ResponseEntity<Map<String, Object>> handleMoMoIpn(@RequestBody Map<String, Object> body) {
        log.info("Received MoMo IPN notification: {}", body);
        try {
            java.util.Map<String, String> stringMap = new java.util.HashMap<>();
            for (Map.Entry<String, Object> entry : body.entrySet()) {
                stringMap.put(entry.getKey(), entry.getValue() != null ? String.valueOf(entry.getValue()) : "");
            }

            boolean isValid = moMoService.verifySignature(stringMap);
            if (!isValid) {
                log.warn("Invalid MoMo IPN signature");
                return ResponseEntity.badRequest().body(Map.of("resultCode", 99, "message", "Invalid signature"));
            }

            String orderIdStr = stringMap.get("orderId");
            Long orderId = moMoService.extractOrderId(orderIdStr);
            String resultCode = stringMap.get("resultCode");
            String transId = stringMap.get("transId");

            if ("0".equals(resultCode) && orderId != null) {
                DonHang order = donHangRepository.findById(orderId).orElse(null);
                if (order != null) {
                    List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDH(orderId);
                    for (ThanhToan tt : payments) {
                        tt.setTrangThai("THANH_CONG");
                        tt.setThoiGianTT(LocalDateTime.now());
                        tt.setMaGiaoDich(transId != null ? transId : "MOMO-" + orderId);
                        thanhToanRepository.save(tt);
                    }
                    if ("CHO_XAC_NHAN".equals(order.getTrangThai())) {
                        order.setTrangThai("DA_XAC_NHAN");
                        donHangRepository.save(order);
                    }
                    log.info("Order #{} automatically marked as PAID via MoMo IPN! transId={}", orderId, transId);
                }
            }

            return ResponseEntity.status(org.springframework.http.HttpStatus.NO_CONTENT).build();
        } catch (Exception e) {
            log.error("Error processing MoMo IPN: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("resultCode", 99, "message", e.getMessage()));
        }
    }

    /**
     * API kiểm tra trạng thái thanh toán real-time cho giao diện frontend order-detail.html
     */
    @GetMapping("/status/{orderId}")
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(@PathVariable("orderId") Long orderId) {
        DonHang order = donHangRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDHOrderByMaThanhToanDesc(orderId);
        boolean isPaid = false;
        String paymentStatus = "CHO_THANH_TOAN";
        if (!payments.isEmpty()) {
            ThanhToan tt = payments.get(0);
            paymentStatus = tt.getTrangThai();
            isPaid = "THANH_CONG".equalsIgnoreCase(paymentStatus);
        }

        // Tự động kiểm tra trực tiếp từ PayOS API hoặc MoMo Gateway API nếu DB chưa ghi nhận thành công
        if (!isPaid && payOSService.isConfigured() && payOSService.isPaymentPaid(orderId)) {
            isPaid = true;
            paymentStatus = "THANH_CONG";
            if (!payments.isEmpty()) {
                ThanhToan tt = payments.get(0);
                tt.setTrangThai("THANH_CONG");
                tt.setThoiGianTT(LocalDateTime.now());
                tt.setMaGiaoDich("PAYOS-" + orderId);
                thanhToanRepository.save(tt);
            }
            if ("CHO_XAC_NHAN".equals(order.getTrangThai())) {
                order.setTrangThai("DA_XAC_NHAN");
                donHangRepository.save(order);
            }
            log.info("Auto-verified PayOS payment for order #{} directly on payment page", orderId);
        } else if (!isPaid && moMoService.isPaymentPaid(orderId)) {
            isPaid = true;
            paymentStatus = "THANH_CONG";
            if (!payments.isEmpty()) {
                ThanhToan tt = payments.get(0);
                tt.setTrangThai("THANH_CONG");
                tt.setThoiGianTT(LocalDateTime.now());
                tt.setMaGiaoDich("MOMO-" + orderId);
                thanhToanRepository.save(tt);
            }
            if ("CHO_XAC_NHAN".equals(order.getTrangThai())) {
                order.setTrangThai("DA_XAC_NHAN");
                donHangRepository.save(order);
            }
            log.info("Auto-verified MoMo payment for order #{} directly via MoMo Query API", orderId);
        }

        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "paid", isPaid,
                "paymentStatus", paymentStatus,
                "orderStatus", order.getTrangThai()
        ));
    }

    /**
     * Endpoint xác nhận thanh toán từ Cổng Ví MoMo riêng biệt (có kiểm tra quyền sở hữu phiên đăng nhập & trạng thái đơn)
     */
    @PostMapping("/momo/confirm/{orderId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> confirmMoMoPayment(@PathVariable("orderId") Long orderId,
                                                                  jakarta.servlet.http.HttpSession session) {
        DonHang order = donHangRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Bảo mật: chỉ chủ đơn hàng đang đăng nhập hoặc ADMIN mới được xác nhận thanh toán đơn này
        com.bookmind.entity.KhachHang currentCustomer = session != null
                ? (com.bookmind.entity.KhachHang) session.getAttribute(com.bookmind.security.AuthInterceptor.SESSION_CUSTOMER)
                : null;
        com.bookmind.entity.TaiKhoan currentUser = session != null
                ? (com.bookmind.entity.TaiKhoan) session.getAttribute(com.bookmind.security.AuthInterceptor.SESSION_USER)
                : null;
        boolean isAdmin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getVaiTro());
        boolean isOwner = currentCustomer != null && order.getKhachHang() != null
                && currentCustomer.getMaKH().equals(order.getKhachHang().getMaKH());

        if (!isAdmin && !isOwner) {
            log.warn("Unauthorized MoMo confirm attempt for order #{}", orderId);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Bạn không có quyền xác nhận đơn hàng này"));
        }

        if (!"CHO_XAC_NHAN".equalsIgnoreCase(order.getTrangThai())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Đơn hàng không ở trạng thái chờ thanh toán"));
        }

        String transCode = "MOMO-" + orderId + "-" + (System.currentTimeMillis() % 1000000);
        List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDHOrderByMaThanhToanDesc(orderId);
        if (!payments.isEmpty()) {
            ThanhToan latest = payments.get(0);
            latest.setTrangThai("THANH_CONG");
            latest.setThoiGianTT(LocalDateTime.now());
            latest.setMaGiaoDich(transCode);
            thanhToanRepository.save(latest);
        }
        order.setTrangThai("DA_XAC_NHAN");
        donHangRepository.save(order);
        log.info("Order #{} confirmed paid via dedicated MoMo Wallet Gateway (transCode={})", orderId, transCode);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "paid", true,
                "orderId", orderId,
                "transCode", transCode
        ));
    }
}
