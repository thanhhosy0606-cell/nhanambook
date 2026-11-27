package com.bookmind.repository;

import com.bookmind.entity.ChiTietDonHang;
import com.bookmind.entity.ChiTietDonHangId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, ChiTietDonHangId> {
    List<ChiTietDonHang> findByDonHang_MaDH(Long maDH);
    long countBySach_MaSach(Long maSach);

    @Query("SELECT COUNT(ct) > 0 FROM ChiTietDonHang ct WHERE ct.donHang.khachHang.maKH = :maKH AND ct.sach.maSach = :maSach AND ct.donHang.trangThai = 'DA_GIAO'")
    boolean hasPurchasedBook(@Param("maKH") Long maKH, @Param("maSach") Long maSach);

    @Query("SELECT ct.donHang FROM ChiTietDonHang ct WHERE ct.donHang.khachHang.maKH = :maKH AND ct.sach.maSach = :maSach AND ct.donHang.trangThai = 'DA_GIAO' ORDER BY ct.donHang.maDH DESC")
    List<com.bookmind.entity.DonHang> findDeliveredOrdersForBook(@Param("maKH") Long maKH, @Param("maSach") Long maSach);

    @Query("SELECT ct.sach.maSach, SUM(ct.soLuong) FROM ChiTietDonHang ct " +
           "WHERE ct.donHang.trangThai <> 'DA_HUY' " +
           "GROUP BY ct.sach.maSach " +
           "ORDER BY SUM(ct.soLuong) DESC")
    List<Object[]> findTopSellingBooksStats();

    @Query("SELECT ct.sach.maSach, SUM(ct.soLuong) FROM ChiTietDonHang ct " +
           "WHERE ct.donHang.trangThai <> 'DA_HUY' AND ct.sach.loaiSach.maLoai = :maLoai " +
           "GROUP BY ct.sach.maSach " +
           "ORDER BY SUM(ct.soLuong) DESC")
    List<Object[]> findTopSellingBooksByCategory(@Param("maLoai") Long maLoai);

    @Query("SELECT ct2.sach.maSach, COUNT(DISTINCT ct2.donHang.maDH), SUM(ct2.soLuong) " +
           "FROM ChiTietDonHang ct1 JOIN ChiTietDonHang ct2 ON ct1.donHang.maDH = ct2.donHang.maDH " +
           "WHERE ct1.sach.maSach = :maSach AND ct2.sach.maSach <> :maSach " +
           "AND ct1.donHang.trangThai <> 'DA_HUY' AND ct2.sach.trangThai = 'DANG_BAN' " +
           "GROUP BY ct2.sach.maSach " +
           "ORDER BY COUNT(DISTINCT ct2.donHang.maDH) DESC, SUM(ct2.soLuong) DESC")
    List<Object[]> findAlsoBoughtBookIds(@Param("maSach") Long maSach);

    @Query("SELECT DISTINCT ct.sach.maSach FROM ChiTietDonHang ct " +
           "WHERE ct.donHang.khachHang.maKH = :maKH AND ct.donHang.trangThai <> 'DA_HUY'")
    List<Long> findPurchasedBookIdsByCustomer(@Param("maKH") Long maKH);
}
