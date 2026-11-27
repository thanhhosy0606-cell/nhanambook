package com.bookmind.service;

import com.bookmind.entity.DonHang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PayOSService {

    private static final Logger log = LoggerFactory.getLogger(PayOSService.class);

    @Autowired(required = false)
    private PayOS payOS;

    @Value("${payos.return-url:http://localhost:8080/shop/order/}")
    private String returnUrlBase;

    @Value("${payos.cancel-url:http://localhost:8080/shop/order/}")
    private String cancelUrlBase;

    private final Map<Long, CreatePaymentLinkResponse> cache = new ConcurrentHashMap<>();
    private final Map<Long, Long> orderCodeToOrderId = new ConcurrentHashMap<>();

    public boolean isConfigured() {
        return payOS != null;
    }

    public long calculateOrderCode(DonHang donHang) {
        int timeSuffix = donHang.getNgayDat() != null 
                ? donHang.getNgayDat().toLocalTime().toSecondOfDay() 
                : 0;
        long orderCode = Math.abs(donHang.getMaDH() * 100000L + (timeSuffix % 100000L));
        if (orderCode <= 0 || orderCode > 9007199254740991L) {
            orderCode = Math.abs(donHang.getMaDH() * 100000L + 12345L);
        }
        return orderCode;
    }

    public Long resolveOrderId(Long orderCode) {
        if (orderCode == null) return null;
        Long cached = orderCodeToOrderId.get(orderCode);
        if (cached != null) return cached;
        if (orderCode < 100000L) return orderCode;
        return orderCode / 100000L;
    }

    public CreatePaymentLinkResponse createPaymentLink(DonHang donHang) {
        if (cache.containsKey(donHang.getMaDH())) {
            return cache.get(donHang.getMaDH());
        }

        if (payOS == null) {
            log.info("PayOS is not configured yet. Fallback VietQR will be used.");
            return null;
        }

        try {
            long orderCode = calculateOrderCode(donHang);
            orderCodeToOrderId.put(orderCode, donHang.getMaDH());

            long amount = donHang.getTongTien().longValue();
            String desc = "DH" + donHang.getMaDH();
            if (desc.length() > 25) {
                desc = desc.substring(0, 25);
            }

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amount)
                    .description(desc)
                    .returnUrl(returnUrlBase + donHang.getMaDH())
                    .cancelUrl(cancelUrlBase + donHang.getMaDH())
                    .buyerName(donHang.getHoTenNguoiNhan())
                    .buyerPhone(donHang.getSdtNguoiNhan())
                    .buyerAddress(donHang.getDiaChiGiaoHang())
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);
            cache.put(donHang.getMaDH(), response);
            log.info("PayOS payment link created for order #{} with orderCode {}, checkoutUrl: {}", 
                    donHang.getMaDH(), orderCode, response.getCheckoutUrl());
            return response;
        } catch (Exception e) {
            log.warn("PayOS createPaymentLink note for order #{}: {}", donHang.getMaDH(), e.getMessage());
            return null;
        }
    }

    public CreatePaymentLinkResponse getPayOsLink(DonHang donHang) {
        return createPaymentLink(donHang);
    }

    public String getPayOsQrImageUrl(DonHang donHang) {
        CreatePaymentLinkResponse link = createPaymentLink(donHang);
        if (link != null) {
            // Ưu tiên 1: Tạo ảnh VietQR chuẩn từ thông tin BIN và Số tài khoản thực tế PayOS trả về
            if (link.getBin() != null && !link.getBin().isBlank() && link.getAccountNumber() != null && !link.getAccountNumber().isBlank()) {
                try {
                    String memo = URLEncoder.encode(link.getDescription() != null ? link.getDescription() : ("DH" + donHang.getMaDH()), StandardCharsets.UTF_8);
                    String accName = link.getAccountName() != null ? URLEncoder.encode(link.getAccountName(), StandardCharsets.UTF_8) : "";
                    return String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                            link.getBin(), link.getAccountNumber(), link.getAmount(), memo, accName);
                } catch (Exception e) {
                    log.error("Failed to format VietQR image URL: {}", e.getMessage());
                }
            }
            // Ưu tiên 2: Sinh QR từ chuỗi EMVCo VietQR của PayOS
            if (link.getQrCode() != null && !link.getQrCode().isBlank()) {
                return "https://api.qrserver.com/v1/create-qr-code/?size=280x280&data="
                        + URLEncoder.encode(link.getQrCode(), StandardCharsets.UTF_8);
            }
        }

        // Ưu tiên 3: Fallback VietQR chuẩn Napas theo cấu hình tài khoản thụ hưởng
        long amount = donHang.getTongTien().longValue();
        String memo = URLEncoder.encode("DH" + donHang.getMaDH(), StandardCharsets.UTF_8);
        String accName = URLEncoder.encode(fallbackAccountName, StandardCharsets.UTF_8);
        return String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                fallbackBankCode, fallbackAccountNumber, amount, memo, accName);
    }

    @org.springframework.beans.factory.annotation.Value("${vietqr.bank.code:BIDV}")
    private String fallbackBankCode;

    @org.springframework.beans.factory.annotation.Value("${vietqr.bank.account-number:0000000000}")
    private String fallbackAccountNumber;

    @org.springframework.beans.factory.annotation.Value("${vietqr.bank.account-name:NHA NAM BOOKSTORE}")
    private String fallbackAccountName;

    public String getPayOsCheckoutUrl(DonHang donHang) {
        CreatePaymentLinkResponse link = createPaymentLink(donHang);
        return link != null ? link.getCheckoutUrl() : null;
    }

    public WebhookData verifyWebhook(Object webhookBody) {
        if (payOS == null) {
            return null;
        }
        try {
            return payOS.webhooks().verify(webhookBody);
        } catch (Exception e) {
            log.error("PayOS webhook verification failed: {}", e.getMessage());
            return null;
        }
    }

    public boolean isPaymentPaid(Long orderId) {
        if (payOS == null || orderId == null) {
            return false;
        }
        try {
            CreatePaymentLinkResponse cached = cache.get(orderId);
            if (cached == null || cached.getOrderCode() <= 0) {
                return false;
            }
            long codeToCheck = cached.getOrderCode();
            vn.payos.model.v2.paymentRequests.PaymentLink link = payOS.paymentRequests().get(codeToCheck);
            if (link != null && link.getStatus() != null && "PAID".equalsIgnoreCase(String.valueOf(link.getStatus()))) {
                log.info("PayOS payment status for order #{} (orderCode {}): PAID", orderId, codeToCheck);
                return true;
            }
        } catch (Exception e) {
            log.debug("Error checking PayOS status for order #{}: {}", orderId, e.getMessage());
        }
        return false;
    }
}
