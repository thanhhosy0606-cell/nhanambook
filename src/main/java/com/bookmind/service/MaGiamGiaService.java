package com.bookmind.service;

import com.bookmind.dto.MaGiamGiaFormDto;
import com.bookmind.entity.ChiTietGioHang;
import com.bookmind.entity.MaGiamGia;
import com.bookmind.repository.DonHangRepository;
import com.bookmind.repository.MaGiamGiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MaGiamGiaService {

    private final MaGiamGiaRepository maGiamGiaRepository;
    private final DonHangRepository donHangRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public MaGiamGiaService(MaGiamGiaRepository maGiamGiaRepository,
                            DonHangRepository donHangRepository) {
        this.maGiamGiaRepository = maGiamGiaRepository;
        this.donHangRepository = donHangRepository;
    }

    public List<MaGiamGia> getAllActive() {
        LocalDateTime now = LocalDateTime.now();
        return maGiamGiaRepository.findByTrangThai("HOAT_DONG").stream()
                .filter(c -> c.getSoLuong() != null && c.getSoLuong() > 0)
                .filter(c -> c.getNgayBatDau() == null || !now.isBefore(c.getNgayBatDau()))
                .filter(c -> c.getNgayKetThuc() == null || !now.isAfter(c.getNgayKetThuc()))
                .toList();
    }

    public List<MaGiamGia> getAllForAdmin() {
        return maGiamGiaRepository.findAll();
    }

    public List<MaGiamGia> getCouponsForAdmin(String keyword, String loaiApDung, String trangThai) {
        List<MaGiamGia> list = maGiamGiaRepository.findAll();

        return list.stream()
                .filter(c -> {
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchCode = c.getMaCode() != null && c.getMaCode().toLowerCase().contains(kw);
                        boolean matchName = c.getTenMa() != null && c.getTenMa().toLowerCase().contains(kw);
                        if (!matchCode && !matchName) {
                            return false;
                        }
                    }
                    if (trangThai != null && !trangThai.trim().isEmpty() && !"ALL".equalsIgnoreCase(trangThai)) {
                        if (!trangThai.equalsIgnoreCase(c.getTrangThai())) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted((a, b) -> Long.compare(b.getMaMaGiamGia(), a.getMaMaGiamGia()))
                .toList();
    }

    public MaGiamGia getById(Long id) {
        return maGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã giảm giá: " + id));
    }

    public Optional<MaGiamGia> findByCode(String code) {
        if (code == null || code.trim().isEmpty()) return Optional.empty();
        return maGiamGiaRepository.findByMaCodeIgnoreCase(code.trim());
    }

    public BigDecimal calculateDiscount(MaGiamGia coupon, List<ChiTietGioHang> cartItems, BigDecimal subtotal) {
        if (coupon == null) return BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        if (!"HOAT_DONG".equalsIgnoreCase(coupon.getTrangThai())) {
            throw new RuntimeException("Mã giảm giá hiện không hoạt động");
        }
        if (now.isBefore(coupon.getNgayBatDau()) || now.isAfter(coupon.getNgayKetThuc())) {
            throw new RuntimeException("Mã giảm giá đã hết hạn hoặc chưa đến đợt áp dụng");
        }
        if (coupon.getSoLuong() <= 0) {
            throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng");
        }
        if (coupon.getGiaTriDonToiThieu() != null && subtotal.compareTo(coupon.getGiaTriDonToiThieu()) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu " + 
                    String.format("%,d", coupon.getGiaTriDonToiThieu().longValue()) + " đ để áp dụng mã này");
        }

        // Áp dụng trực tiếp trên tổng tiền tạm tính (subtotal) của toàn bộ đơn hàng (theo ERD bảng MAGIAMGIA)
        BigDecimal discount = BigDecimal.ZERO;
        if (coupon.getPhanTramGiam() != null && coupon.getPhanTramGiam().compareTo(BigDecimal.ZERO) > 0) {
            discount = subtotal.multiply(coupon.getPhanTramGiam()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getSoTienGiamToiDa() != null && discount.compareTo(coupon.getSoTienGiamToiDa()) > 0) {
                discount = coupon.getSoTienGiamToiDa();
            }
        } else if (coupon.getSoTienGiamToiDa() != null) {
            discount = coupon.getSoTienGiamToiDa();
        }

        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        return discount;
    }

    public BigDecimal calculateDiscount(MaGiamGia coupon, BigDecimal subtotal) {
        return calculateDiscount(coupon, null, subtotal);
    }

    @Transactional
    public MaGiamGia save(MaGiamGiaFormDto dto) {
        MaGiamGia mg;
        if (dto.getMaMaGiamGia() != null) {
            mg = getById(dto.getMaMaGiamGia());
            if (!mg.getMaCode().equalsIgnoreCase(dto.getMaCode().trim()) &&
                    maGiamGiaRepository.existsByMaCodeIgnoreCase(dto.getMaCode().trim())) {
                throw new RuntimeException("Mã code này đã tồn tại: " + dto.getMaCode());
            }
        } else {
            mg = new MaGiamGia();
            if (maGiamGiaRepository.existsByMaCodeIgnoreCase(dto.getMaCode().trim())) {
                throw new RuntimeException("Mã code này đã tồn tại: " + dto.getMaCode());
            }
        }

        if (dto.getNgayBatDau() != null && dto.getNgayKetThuc() != null
                && !dto.getNgayKetThuc().isAfter(dto.getNgayBatDau())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu!");
        }

        BigDecimal cleanPct = (dto.getPhanTramGiam() != null && dto.getPhanTramGiam().compareTo(BigDecimal.ZERO) > 0)
                ? dto.getPhanTramGiam()
                : null;

        mg.setMaCode(dto.getMaCode().toUpperCase().trim());
        mg.setTenMa(dto.getTenMa().trim());
        mg.setPhanTramGiam(cleanPct);
        mg.setSoTienGiamToiDa(dto.getSoTienGiamToiDa());
        mg.setGiaTriDonToiThieu(dto.getGiaTriDonToiThieu() != null ? dto.getGiaTriDonToiThieu() : BigDecimal.ZERO);
        mg.setNgayBatDau(dto.getNgayBatDau());
        mg.setNgayKetThuc(dto.getNgayKetThuc());
        mg.setSoLuong(dto.getSoLuong());
        if (dto.getTrangThai() != null) {
            mg.setTrangThai(dto.getTrangThai());
        }

        return maGiamGiaRepository.saveAndFlush(mg);
    }

    @Transactional
    public void delete(Long id) {
        MaGiamGia mg = getById(id);
        // Dùng 1 câu lệnh UPDATE trực tiếp trên DB thay vì tải toàn bộ DONHANG lên RAM
        donHangRepository.detachCouponFromOrders(id);
        maGiamGiaRepository.delete(mg);
        maGiamGiaRepository.flush();
    }

    @Transactional
    public void toggleStatus(Long id) {
        MaGiamGia mg = getById(id);
        if ("HOAT_DONG".equalsIgnoreCase(mg.getTrangThai())) {
            mg.setTrangThai("NGUNG");
        } else {
            mg.setTrangThai("HOAT_DONG");
        }
        maGiamGiaRepository.save(mg);
    }
}
