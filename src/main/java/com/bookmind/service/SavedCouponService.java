package com.bookmind.service;

import com.bookmind.entity.KhachHang;
import com.bookmind.entity.KhachHangMaGiamGia;
import com.bookmind.entity.MaGiamGia;
import com.bookmind.repository.KhachHangMaGiamGiaRepository;
import com.bookmind.repository.KhachHangRepository;
import com.bookmind.security.AuthInterceptor;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Quản lý "ví mã giảm giá" của người dùng:
 * - Khi đã đăng nhập: lưu trực tiếp vào bảng KH_MGG trong MySQL theo từng tài khoản (MaKH),
 *   đăng xuất và đăng nhập lại vẫn giữ nguyên các mã đã lưu.
 * - Khi chưa đăng nhập (khách vãng lai): lưu tạm trong HttpSession và tự động gộp vào KH_MGG khi đăng nhập.
 */
@Service
public class SavedCouponService {

    public static final String SESSION_SAVED_COUPONS = "SAVED_COUPONS";

    private final MaGiamGiaService maGiamGiaService;
    private final KhachHangMaGiamGiaRepository khachHangMaGiamGiaRepository;
    private final KhachHangRepository khachHangRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public SavedCouponService(MaGiamGiaService maGiamGiaService,
                              KhachHangMaGiamGiaRepository khachHangMaGiamGiaRepository,
                              KhachHangRepository khachHangRepository) {
        this.maGiamGiaService = maGiamGiaService;
        this.khachHangMaGiamGiaRepository = khachHangMaGiamGiaRepository;
        this.khachHangRepository = khachHangRepository;
    }

    private KhachHang getLoggedInCustomer(HttpSession session) {
        if (session == null) return null;
        Object attr = session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        return attr instanceof KhachHang kh && kh.getMaKH() != null ? kh : null;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getSessionCodes(HttpSession session) {
        if (session == null) return new LinkedHashSet<>();
        Object attr = session.getAttribute(SESSION_SAVED_COUPONS);
        if (attr instanceof Set<?>) {
            return (Set<String>) attr;
        }
        Set<String> codes = new LinkedHashSet<>();
        session.setAttribute(SESSION_SAVED_COUPONS, codes);
        return codes;
    }

    public Set<String> getSavedCodes(HttpSession session) {
        KhachHang customer = getLoggedInCustomer(session);
        if (customer != null) {
            return khachHangMaGiamGiaRepository.findSavedCouponsByMaKH(customer.getMaKH()).stream()
                    .map(c -> normalize(c.getMaCode()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return getSessionCodes(session);
    }

    public boolean isSaved(HttpSession session, String code) {
        return code != null && getSavedCodes(session).contains(normalize(code));
    }

    /** Lưu mã vào ví (bảng KH_MGG nếu đã đăng nhập, hoặc HttpSession nếu khách vãng lai). */
    @Transactional
    public void save(HttpSession session, String code) {
        Optional<MaGiamGia> couponOpt = maGiamGiaService.findByCode(code);
        if (couponOpt.isEmpty() || !isUsable(couponOpt.get())) {
            throw new IllegalArgumentException("Mã giảm giá không tồn tại hoặc đã hết hiệu lực");
        }
        MaGiamGia coupon = couponOpt.get();
        KhachHang customer = getLoggedInCustomer(session);
        if (customer != null) {
            if (!khachHangMaGiamGiaRepository.existsById_MaKHAndId_MaGiamGia(customer.getMaKH(), coupon.getMaMaGiamGia())) {
                KhachHang managedKh = khachHangRepository.getReferenceById(customer.getMaKH());
                khachHangMaGiamGiaRepository.save(new KhachHangMaGiamGia(coupon, managedKh));
            }
            return;
        }
        Set<String> codes = getSessionCodes(session);
        codes.add(normalize(code));
        session.setAttribute(SESSION_SAVED_COUPONS, codes);
    }

    /** Xóa mã khỏi ví (khi bỏ lưu hoặc đã sử dụng đặt hàng thành công). */
    @Transactional
    public void remove(HttpSession session, String code) {
        if (code == null) return;
        KhachHang customer = getLoggedInCustomer(session);
        if (customer != null) {
            maGiamGiaService.findByCode(code).ifPresent(coupon ->
                    khachHangMaGiamGiaRepository.deleteById_MaKHAndId_MaGiamGia(customer.getMaKH(), coupon.getMaMaGiamGia()));
        }
        Set<String> sessionCodes = getSessionCodes(session);
        if (sessionCodes.remove(normalize(code))) {
            session.setAttribute(SESSION_SAVED_COUPONS, sessionCodes);
        }
    }

    /** Gộp các mã khách vãng lai đã bấm lưu trong phiên vào bảng KH_MGG sau khi đăng nhập. */
    @Transactional
    public void mergeSessionCouponsToCustomer(HttpSession session, KhachHang customer) {
        if (session == null || customer == null || customer.getMaKH() == null) return;
        Object attr = session.getAttribute(SESSION_SAVED_COUPONS);
        if (!(attr instanceof Set<?> rawSet) || rawSet.isEmpty()) {
            return;
        }
        KhachHang managedKh = khachHangRepository.getReferenceById(customer.getMaKH());
        for (Object item : rawSet) {
            if (item instanceof String code) {
                maGiamGiaService.findByCode(code)
                        .filter(this::isUsable)
                        .ifPresent(coupon -> {
                            if (!khachHangMaGiamGiaRepository.existsById_MaKHAndId_MaGiamGia(customer.getMaKH(), coupon.getMaMaGiamGia())) {
                                khachHangMaGiamGiaRepository.save(new KhachHangMaGiamGia(coupon, managedKh));
                            }
                        });
            }
        }
        session.removeAttribute(SESSION_SAVED_COUPONS);
    }

    /** Danh sách mã đã lưu và vẫn còn dùng được (đang hoạt động, còn hạn, còn lượt). */
    public List<MaGiamGia> getSavedUsableCoupons(HttpSession session) {
        KhachHang customer = getLoggedInCustomer(session);
        if (customer != null) {
            return khachHangMaGiamGiaRepository.findSavedCouponsByMaKH(customer.getMaKH()).stream()
                    .filter(this::isUsable)
                    .toList();
        }
        Set<String> codes = getSessionCodes(session);
        if (codes.isEmpty()) return List.of();
        return maGiamGiaService.getAllActive().stream()
                .filter(c -> codes.contains(normalize(c.getMaCode())))
                .filter(this::isUsable)
                .toList();
    }

    private boolean isUsable(MaGiamGia c) {
        LocalDateTime now = LocalDateTime.now();
        return "HOAT_DONG".equalsIgnoreCase(c.getTrangThai())
                && c.getSoLuong() != null && c.getSoLuong() > 0
                && (c.getNgayBatDau() == null || !now.isBefore(c.getNgayBatDau()))
                && (c.getNgayKetThuc() == null || !now.isAfter(c.getNgayKetThuc()));
    }

    private String normalize(String code) {
        return code.trim().toUpperCase();
    }
}
