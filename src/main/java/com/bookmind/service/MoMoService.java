package com.bookmind.service;

import com.bookmind.dto.MoMoPaymentResponse;
import com.bookmind.entity.DonHang;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MoMoService {

    private static final Logger log = LoggerFactory.getLogger(MoMoService.class);

    @Value("${momo.partner-code:MOMO}")
    private String partnerCode;

    @Value("${momo.access-key:}")
    private String accessKey;

    @Value("${momo.secret-key:}")
    private String secretKey;

    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String endpoint;

    @Value("${momo.redirect-url:http://localhost:8080/shop/order/momo-return}")
    private String redirectUrl;

    @Value("${momo.ipn-url:http://localhost:8080/api/payment/momo/ipn}")
    private String ipnUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Cache kết quả tạo payment link theo mã đơn hàng
    private final Map<Long, MoMoPaymentResponse> cache = new ConcurrentHashMap<>();

    /**
     * Tạo yêu cầu thanh toán sang MoMo Gateway API v2 (captureWallet)
     */
    public MoMoPaymentResponse createPayment(DonHang donHang) {
        if (cache.containsKey(donHang.getMaDH())) {
            return cache.get(donHang.getMaDH());
        }

        long amount = donHang.getTongTien().longValue();
        String requestId = "MOMO_REQ_" + donHang.getMaDH() + "_" + System.currentTimeMillis();
        String orderId = "DH" + donHang.getMaDH() + "_" + System.currentTimeMillis();

        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            log.info("MoMo Gateway API keys are not configured. Using fallback MoMo Wallet QR.");
            MoMoPaymentResponse fallback = generateFallbackResponse(donHang, amount, orderId, requestId);
            cache.put(donHang.getMaDH(), fallback);
            return fallback;
        }

        String orderInfo = "Thanh toan don hang #" + donHang.getMaDH();
        String extraData = "";
        String requestType = "captureWallet";

        // Tạo raw signature theo chuẩn MoMo v2
        String rawSignature = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + redirectUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        String signature = hmacSha256(rawSignature, secretKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("partnerName", "BookMind Bookstore");
        requestBody.put("storeId", "BookMindStore");
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", redirectUrl);
        requestBody.put("ipnUrl", ipnUrl);
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", requestType);
        requestBody.put("signature", signature);

        try {
            String jsonPayload = objectMapper.writeValueAsString(requestBody);
            log.info("Sending MoMo payment request to {}: {}", endpoint, jsonPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            log.info("MoMo API response code {}: {}", response.statusCode(), response.body());

            if (response.statusCode() == 200) {
                MoMoPaymentResponse res = objectMapper.readValue(response.body(), MoMoPaymentResponse.class);
                if (res.getResultCode() != null && res.getResultCode() == 0 && res.getPayUrl() != null) {
                    cache.put(donHang.getMaDH(), res);
                    return res;
                }
                log.warn("MoMo returned non-zero result code: {} - {}", res.getResultCode(), res.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to call MoMo Gateway API: {}", e.getMessage());
        }

        // Fallback: nếu sandbox offline hoặc timeout, tạo fallback QR & payUrl nội bộ
        MoMoPaymentResponse fallback = generateFallbackResponse(donHang, amount, orderId, requestId);
        cache.put(donHang.getMaDH(), fallback);
        return fallback;
    }

    @Value("${momo.wallet.phone:}")
    private String walletPhone;

    @Value("${momo.wallet.name:NHA NAM BOOKSTORE}")
    private String walletName;

    private MoMoPaymentResponse generateFallbackResponse(DonHang donHang, long amount, String orderId, String requestId) {
        String memo = "DH" + donHang.getMaDH();
        // Sinh mã QR chuẩn định dạng riêng của Ví điện tử MoMo (P2P Transfer QR format, không dùng VietQR)
        String rawMoMo = String.format("2|99|%s|%s||0|0|%d|%s|transfer_myqr",
                walletPhone, walletName, amount, memo);
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&ecc=H&margin=8&color=a50064&bgcolor=ffffff&data="
                + URLEncoder.encode(rawMoMo, StandardCharsets.UTF_8);

        MoMoPaymentResponse res = new MoMoPaymentResponse();
        res.setPartnerCode(partnerCode);
        res.setOrderId(orderId);
        res.setRequestId(requestId);
        res.setAmount(amount);
        res.setResultCode(0);
        res.setMessage("MoMo Dedicated Wallet QR");
        res.setPayUrl(null);
        res.setQrCodeUrl(qrUrl);
        res.setDeeplink("momo://?action=payWithApp&isScanQR=true");
        return res;
    }

    public String getMoMoPayUrl(DonHang donHang) {
        MoMoPaymentResponse res = createPayment(donHang);
        return res != null ? res.getPayUrl() : null;
    }

    public String getMoMoQrUrl(DonHang donHang) {
        MoMoPaymentResponse res = createPayment(donHang);
        if (res != null) {
            if (res.getQrCodeUrl() != null && res.getQrCodeUrl().startsWith("http")) {
                return res.getQrCodeUrl();
            }
            if (res.getQrCodeUrl() != null && !res.getQrCodeUrl().isBlank()) {
                return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&ecc=H&margin=8&color=a50064&bgcolor=ffffff&data=" +
                        URLEncoder.encode(res.getQrCodeUrl(), StandardCharsets.UTF_8);
            }
            if (res.getPayUrl() != null && !res.getPayUrl().isBlank()) {
                return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&ecc=H&margin=8&color=a50064&bgcolor=ffffff&data=" +
                        URLEncoder.encode(res.getPayUrl(), StandardCharsets.UTF_8);
            }
        }
        long amount = donHang.getTongTien().longValue();
        String rawMoMo = String.format("2|99|%s|%s||0|0|%d|DH%d|transfer_myqr",
                walletPhone, walletName, amount, donHang.getMaDH());
        return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&ecc=H&margin=8&color=a50064&bgcolor=ffffff&data=" +
                URLEncoder.encode(rawMoMo, StandardCharsets.UTF_8);
    }

    /**
     * Truy vấn trực tiếp trạng thái giao dịch từ MoMo Gateway API v2 (/v2/gateway/api/query)
     */
    public boolean isPaymentPaid(Long maDH) {
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            return false;
        }
        MoMoPaymentResponse cached = cache.get(maDH);
        if (cached == null || cached.getOrderId() == null || cached.getPayUrl() == null) {
            return false;
        }
        try {
            String queryEndpoint = endpoint.replace("/create", "/query");
            String reqId = "MOMO_QRY_" + maDH + "_" + System.currentTimeMillis();
            String ordId = cached.getOrderId();

            String rawSig = "accessKey=" + accessKey +
                    "&orderId=" + ordId +
                    "&partnerCode=" + partnerCode +
                    "&requestId=" + reqId;
            String sig = hmacSha256(rawSig, secretKey);

            Map<String, Object> body = new HashMap<>();
            body.put("partnerCode", partnerCode);
            body.put("requestId", reqId);
            body.put("orderId", ordId);
            body.put("lang", "vi");
            body.put("signature", sig);

            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(queryEndpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                MoMoPaymentResponse res = objectMapper.readValue(response.body(), MoMoPaymentResponse.class);
                return res.getResultCode() != null && res.getResultCode() == 0;
            }
        } catch (Exception e) {
            log.debug("MoMo status query check: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Xác thực chữ ký số của MoMo trả về từ Return URL hoặc Webhook IPN
     */
    public boolean verifySignature(Map<String, String> params) {
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            return false;
        }
        try {
            String signature = params.get("signature");
            if (signature == null || signature.isBlank()) {
                return false;
            }

            String rawSignature = "accessKey=" + accessKey +
                    "&amount=" + params.getOrDefault("amount", "") +
                    "&extraData=" + params.getOrDefault("extraData", "") +
                    "&message=" + params.getOrDefault("message", "") +
                    "&orderId=" + params.getOrDefault("orderId", "") +
                    "&orderInfo=" + params.getOrDefault("orderInfo", "") +
                    "&orderType=" + params.getOrDefault("orderType", "") +
                    "&partnerCode=" + params.getOrDefault("partnerCode", "") +
                    "&payType=" + params.getOrDefault("payType", "") +
                    "&requestId=" + params.getOrDefault("requestId", "") +
                    "&responseTime=" + params.getOrDefault("responseTime", "") +
                    "&resultCode=" + params.getOrDefault("resultCode", "") +
                    "&transId=" + params.getOrDefault("transId", "");

            String expectedSignature = hmacSha256(rawSignature, secretKey);
            return expectedSignature.equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("MoMo verifySignature error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Trích xuất mã đơn hàng thực tế (Long) từ orderId của MoMo (ví dụ: DH123_172... hoặc DH123)
     */
    public Long extractOrderId(String momoOrderId) {
        if (momoOrderId == null) return null;
        String clean = momoOrderId;
        if (clean.startsWith("DH")) {
            clean = clean.substring(2);
        }
        if (clean.contains("_")) {
            clean = clean.substring(0, clean.indexOf("_"));
        }
        try {
            return Long.parseLong(clean.trim());
        } catch (Exception e) {
            log.warn("Cannot extract order id from MoMo orderId: {}", momoOrderId);
            return null;
        }
    }

    /**
     * Thuật toán mã hóa HMAC-SHA256
     */
    public static String hmacSha256(String data, String key) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKeySpec);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating HMAC-SHA256", e);
        }
    }
}
