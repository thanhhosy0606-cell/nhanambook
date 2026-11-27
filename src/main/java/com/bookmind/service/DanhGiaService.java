package com.bookmind.service;

import com.bookmind.dto.DanhGiaRequest;
import com.bookmind.entity.*;
import com.bookmind.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DanhGiaService {

    private final DanhGiaRepository danhGiaRepository;
    private final SachRepository sachRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public DanhGiaService(DanhGiaRepository danhGiaRepository,
                          SachRepository sachRepository,
                          ChiTietDonHangRepository chiTietDonHangRepository) {
        this.danhGiaRepository = danhGiaRepository;
        this.sachRepository = sachRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
    }

    public List<DanhGia> getRatingsForBook(Long maSach) {
        return danhGiaRepository.findBySach_MaSachAndTrangThaiOrderByNgayDanhGiaDesc(maSach, "HIEN_THI");
    }

    public Double getAverageRating(Long maSach) {
        Double avg = danhGiaRepository.getAverageRating(maSach, "HIEN_THI");
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 5.0;
    }

    public long countRatings(Long maSach) {
        return danhGiaRepository.countBySach_MaSachAndTrangThai(maSach, "HIEN_THI");
    }

    public Optional<DanhGia> getUserRatingForBook(KhachHang khachHang, Long maSach) {
        return danhGiaRepository.findByKhachHang_MaKHAndSach_MaSach(khachHang.getMaKH(), maSach);
    }

    public boolean hasPurchasedBook(Long maKH, Long maSach) {
        if (maKH == null || maSach == null) return false;
        return chiTietDonHangRepository.hasPurchasedBook(maKH, maSach);
    }

    @Transactional
    public DanhGia submitRating(KhachHang khachHang, DanhGiaRequest req) {
        return submitRating(khachHang, null, req);
    }

    @Transactional
    public DanhGia submitRating(KhachHang khachHang, Long maDH, DanhGiaRequest req) {
        List<DonHang> deliveredOrders = chiTietDonHangRepository.findDeliveredOrdersForBook(khachHang.getMaKH(), req.getMaSach());
        if (deliveredOrders.isEmpty()) {
            throw new IllegalArgumentException("Chỉ những đơn hàng đã giao thành công mới được đánh giá!");
        }

        DonHang targetOrder = deliveredOrders.get(0);
        if (maDH != null) {
            targetOrder = deliveredOrders.stream()
                    .filter(dh -> maDH.equals(dh.getMaDH()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Đơn hàng #" + maDH + " chưa giao thành công hoặc không chứa cuốn sách này!"));
        }

        Sach sach = sachRepository.findById(req.getMaSach())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách: " + req.getMaSach()));

        Optional<DanhGia> existingOpt = danhGiaRepository.findByKhachHang_MaKHAndSach_MaSach(khachHang.getMaKH(), req.getMaSach());
        if (existingOpt.isPresent()) {
            throw new IllegalArgumentException("Bạn đã đánh giá cuốn sách này rồi. Mỗi cuốn sách chỉ được đánh giá 1 lần!");
        }

        DanhGia dg = new DanhGia();
        dg.setDonHang(targetOrder);
        dg.setSach(sach);
        dg.setSoSao(req.getSoSao());
        dg.setNoiDung(req.getNoiDung());
        dg.setNgayDanhGia(LocalDateTime.now());
        dg.setTrangThai("HIEN_THI");
        return danhGiaRepository.save(dg);
    }
}
