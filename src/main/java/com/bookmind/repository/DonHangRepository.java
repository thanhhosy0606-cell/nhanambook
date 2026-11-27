package com.bookmind.repository;

import com.bookmind.entity.DonHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonHangRepository extends JpaRepository<DonHang, Long>, JpaSpecificationExecutor<DonHang> {
    List<DonHang> findByKhachHang_MaKHOrderByNgayDatDesc(Long maKH);
    Page<DonHang> findByKhachHang_MaKHOrderByNgayDatDesc(Long maKH, Pageable pageable);
    Optional<DonHang> findByMaDHAndKhachHang_MaKH(Long maDH, Long maKH);
    long countByKhachHang_MaKH(Long maKH);
    long countByMaGiamGia_MaMaGiamGia(Long maMaGiamGia);

    Page<DonHang> findAllByOrderByNgayDatDesc(Pageable pageable);
    Page<DonHang> findByTrangThaiOrderByNgayDatDesc(String trangThai, Pageable pageable);

    List<DonHang> findByNgayDatBetweenOrderByNgayDatAsc(LocalDateTime start, LocalDateTime end);
    List<DonHang> findAllByOrderByNgayDatAsc();
    List<DonHang> findByTrangThaiAndNgayDatBefore(String trangThai, LocalDateTime cutoffTime);

    @Query("SELECT COALESCE(SUM(d.tongTien), 0) FROM DonHang d WHERE d.trangThai = 'DA_GIAO'")
    BigDecimal sumCompletedRevenue();

    @Query("SELECT kh.maKH, kh.hoTen, kh.sdt, tk.email, " +
           "COUNT(d), " +
           "SUM(CASE WHEN d.trangThai = 'DA_GIAO' THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN d.trangThai = 'DA_HUY' THEN 1L ELSE 0L END), " +
           "COALESCE(SUM(CASE WHEN d.trangThai <> 'DA_HUY' THEN d.tongTien ELSE 0 END), 0) " +
           "FROM DonHang d JOIN d.khachHang kh LEFT JOIN kh.taiKhoan tk " +
           "GROUP BY kh.maKH, kh.hoTen, kh.sdt, tk.email " +
           "ORDER BY COALESCE(SUM(CASE WHEN d.trangThai <> 'DA_HUY' THEN d.tongTien ELSE 0 END), 0) DESC, COUNT(d) DESC")
    List<Object[]> findTopCustomerStats(Pageable pageable);

    @Query("SELECT d.maGiamGia.maMaGiamGia, COUNT(d), COALESCE(SUM(d.giamGia), 0), COALESCE(SUM(d.tongTien), 0) " +
           "FROM DonHang d WHERE d.trangThai <> 'DA_HUY' AND d.maGiamGia IS NOT NULL " +
           "GROUP BY d.maGiamGia.maMaGiamGia")
    List<Object[]> findCouponUsageStats();

    @Query("SELECT COUNT(d) FROM DonHang d WHERE d.trangThai = 'CHO_XAC_NHAN' " +
           "AND NOT EXISTS (SELECT tt1 FROM ThanhToan tt1 WHERE tt1.donHang = d AND UPPER(tt1.trangThai) = 'THANH_CONG') " +
           "AND NOT EXISTS (SELECT tt2 FROM ThanhToan tt2 JOIN tt2.ptthThanhToan pt WHERE tt2.donHang = d " +
           "AND (LOWER(pt.tenPTTT) LIKE '%tien mat%' OR LOWER(pt.tenPTTT) LIKE '%tiền mặt%' OR LOWER(pt.tenPTTT) LIKE '%cod%'))")
    long countPendingOnlineUnpaidOrders();

    @Query("SELECT COUNT(d) FROM DonHang d WHERE d.trangThai = 'CHO_XAC_NHAN' " +
           "AND ((d.thoiGianHetHanTT IS NOT NULL AND d.thoiGianHetHanTT <= :now) " +
           "     OR (d.thoiGianHetHanTT IS NULL AND d.ngayDat <= :fallbackCutoff)) " +
           "AND NOT EXISTS (SELECT tt1 FROM ThanhToan tt1 WHERE tt1.donHang = d AND UPPER(tt1.trangThai) = 'THANH_CONG') " +
           "AND NOT EXISTS (SELECT tt2 FROM ThanhToan tt2 JOIN tt2.ptthThanhToan pt WHERE tt2.donHang = d " +
           "AND (LOWER(pt.tenPTTT) LIKE '%tien mat%' OR LOWER(pt.tenPTTT) LIKE '%tiền mặt%' OR LOWER(pt.tenPTTT) LIKE '%cod%'))")
    long countExpiredOnlineUnpaidOrders(@Param("now") LocalDateTime now, @Param("fallbackCutoff") LocalDateTime fallbackCutoff);

    @Query("SELECT d FROM DonHang d WHERE d.trangThai = 'CHO_XAC_NHAN' " +
           "AND ((d.thoiGianHetHanTT IS NOT NULL AND d.thoiGianHetHanTT <= :now) " +
           "     OR (d.thoiGianHetHanTT IS NULL AND d.ngayDat <= :fallbackCutoff)) " +
           "AND NOT EXISTS (SELECT tt1 FROM ThanhToan tt1 WHERE tt1.donHang = d AND UPPER(tt1.trangThai) = 'THANH_CONG') " +
           "AND NOT EXISTS (SELECT tt2 FROM ThanhToan tt2 JOIN tt2.ptthThanhToan pt WHERE tt2.donHang = d " +
           "AND (LOWER(pt.tenPTTT) LIKE '%tien mat%' OR LOWER(pt.tenPTTT) LIKE '%tiền mặt%' OR LOWER(pt.tenPTTT) LIKE '%cod%'))")
    List<DonHang> findExpiredOnlineUnpaidOrders(@Param("now") LocalDateTime now, @Param("fallbackCutoff") LocalDateTime fallbackCutoff);

    @Modifying
    @Query("UPDATE DonHang d SET d.maGiamGia = null WHERE d.maGiamGia.maMaGiamGia = :couponId")
    void detachCouponFromOrders(@Param("couponId") Long couponId);
}
