package com.bookmind.service;

import com.bookmind.entity.*;
import com.bookmind.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ThanhToanService {

    private final PtthThanhToanRepository ptthThanhToanRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final PayOSService payOSService;
    private final MoMoService moMoService;

    @org.springframework.beans.factory.annotation.Autowired
    public ThanhToanService(PtthThanhToanRepository ptthThanhToanRepository,
                            ThanhToanRepository thanhToanRepository,
                            PayOSService payOSService,
                            MoMoService moMoService) {
        this.ptthThanhToanRepository = ptthThanhToanRepository;
        this.thanhToanRepository = thanhToanRepository;
        this.payOSService = payOSService;
        this.moMoService = moMoService;
    }

    public List<PtthThanhToan> getAllActiveMethods() {
        return ptthThanhToanRepository.findByTrangThai("HOAT_DONG");
    }

    public PtthThanhToan getMethodById(Long id) {
        return ptthThanhToanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phương thức thanh toán: " + id));
    }

    @Transactional
    public ThanhToan createPayment(DonHang donHang, PtthThanhToan pttt, BigDecimal amount) {
        String code = "TXN-" + System.currentTimeMillis();
        String initialStatus = "CHO_THANH_TOAN";

        ThanhToan tt = new ThanhToan();
        tt.setDonHang(donHang);
        tt.setPtthThanhToan(pttt);
        tt.setSoTien(amount);
        tt.setMaGiaoDich(code);
        tt.setTrangThai(initialStatus);
        tt.setNoiDung("Thanh toan don hang #" + donHang.getMaDH());
        return thanhToanRepository.save(tt);
    }

    public boolean isVietQrMethod(String name) {
        if (name == null) return false;
        String clean = name.trim().toLowerCase();
        return clean.contains("vietqr") || clean.contains("chuyen khoan") || clean.contains("chuyển khoản")
                || clean.contains("bidv") || clean.contains("payos");
    }

    public boolean isMoMoMethod(String name) {
        if (name == null) return false;
        String clean = name.trim().toLowerCase();
        return clean.contains("momo");
    }

    public boolean isCodMethod(String name) {
        if (name == null) return false;
        String clean = name.trim().toLowerCase();
        return clean.contains("tien mat") || clean.contains("tiền mặt") || clean.contains("cod");
    }

    public String generatePaymentQrUrl(DonHang donHang, PtthThanhToan pttt) {
        if (pttt == null) {
            return null;
        }
        String name = pttt.getTenPTTT();
        // Tiền mặt COD thì không sinh QR
        if (isCodMethod(name)) {
            return null;
        }

        // 1. CỔNG THANH TOÁN VÍ MOMO RIÊNG BIỆT (Mã QR Ví MoMo gốc, không dùng VietQR)
        if (isMoMoMethod(name)) {
            return moMoService.getMoMoQrUrl(donHang);
        }

        // 2. VIETQR / BIDV: Cổng PayOS hoặc fallback Napas247 theo cấu hình
        return payOSService.getPayOsQrImageUrl(donHang);
    }

    public String getPayOsCheckoutUrl(DonHang donHang) {
        return payOSService.getPayOsCheckoutUrl(donHang);
    }

    public String getMoMoPayUrl(DonHang donHang) {
        return moMoService.getMoMoPayUrl(donHang);
    }

    public vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse getPayOsLink(DonHang donHang) {
        return payOSService.getPayOsLink(donHang);
    }

    @Transactional
    public void markPaymentSuccess(Long maThanhToan, String maGiaoDich) {
        ThanhToan tt = thanhToanRepository.findById(maThanhToan)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán: " + maThanhToan));
        tt.setTrangThai("THANH_CONG");
        tt.setThoiGianTT(LocalDateTime.now());
        if (maGiaoDich != null && !maGiaoDich.isBlank()) {
            tt.setMaGiaoDich(maGiaoDich);
        }
        thanhToanRepository.save(tt);
    }

    @Transactional
    public ThanhToan updatePaymentMethod(DonHang donHang, Long maPTTT) {
        PtthThanhToan pttt = getMethodById(maPTTT);
        List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDHOrderByMaThanhToanDesc(donHang.getMaDH());

        // Lưu lịch sử các lần thử thanh toán (quan hệ 1:N THANHTOAN -> DONHANG):
        // Đánh dấu giao dịch đang chờ cũ là THAT_BAI và tạo dòng giao dịch mới cho phương thức mới
        for (ThanhToan oldPayment : payments) {
            if ("CHO_THANH_TOAN".equalsIgnoreCase(oldPayment.getTrangThai())) {
                oldPayment.setTrangThai("THAT_BAI");
                oldPayment.setNoiDung("Khách hàng chuyển sang phương thức thanh toán khác (" + pttt.getTenPTTT() + ")");
                thanhToanRepository.save(oldPayment);
            }
        }
        return createPayment(donHang, pttt, donHang.getTongTien());
    }
}
