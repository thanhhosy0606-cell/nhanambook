package com.bookmind.repository;

import com.bookmind.entity.DanhGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DanhGiaRepository extends JpaRepository<DanhGia, Long> {
    @Query("SELECT dg FROM DanhGia dg JOIN FETCH dg.donHang dh JOIN FETCH dh.khachHang " +
           "WHERE dg.sach.maSach = :maSach AND dg.trangThai = :trangThai ORDER BY dg.ngayDanhGia DESC")
    List<DanhGia> findBySach_MaSachAndTrangThaiOrderByNgayDanhGiaDesc(@Param("maSach") Long maSach, @Param("trangThai") String trangThai);

    Optional<DanhGia> findFirstByDonHang_KhachHang_MaKHAndSach_MaSachOrderByMaDanhGiaDesc(Long maKH, Long maSach);

    default Optional<DanhGia> findByKhachHang_MaKHAndSach_MaSach(Long maKH, Long maSach) {
        return findFirstByDonHang_KhachHang_MaKHAndSach_MaSachOrderByMaDanhGiaDesc(maKH, maSach);
    }

    Optional<DanhGia> findByDonHang_MaDHAndSach_MaSach(Long maDH, Long maSach);

    long countBySach_MaSachAndTrangThai(Long maSach, String trangThai);

    @Modifying
    @Query("DELETE FROM DanhGia d WHERE d.donHang.khachHang.maKH = :maKH")
    void deleteByKhachHang_MaKH(@Param("maKH") Long maKH);

    void deleteBySach_MaSach(Long maSach);

    @Query("SELECT AVG(d.soSao) FROM DanhGia d WHERE d.sach.maSach = :maSach AND d.trangThai = :trangThai")
    Double getAverageRating(@Param("maSach") Long maSach, @Param("trangThai") String trangThai);
}
