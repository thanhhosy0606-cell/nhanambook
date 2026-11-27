package com.bookmind.repository;

import com.bookmind.entity.ChiTietGioHang;
import com.bookmind.entity.ChiTietGioHangId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChiTietGioHangRepository extends JpaRepository<ChiTietGioHang, ChiTietGioHangId> {
    List<ChiTietGioHang> findByGioHang_MaGioHang(Long maGioHang);
    Optional<ChiTietGioHang> findByGioHang_MaGioHangAndSach_MaSach(Long maGioHang, Long maSach);
    long countBySach_MaSach(Long maSach);

    @Modifying
    @Query("DELETE FROM ChiTietGioHang c WHERE c.gioHang.maGioHang = :maGioHang")
    void deleteByMaGioHang(@Param("maGioHang") Long maGioHang);

    @Modifying
    @Query("DELETE FROM ChiTietGioHang c WHERE c.sach.maSach = :maSach")
    void deleteByMaSach(@Param("maSach") Long maSach);

    @Query("SELECT COALESCE(SUM(ct.soLuong), 0) FROM ChiTietGioHang ct WHERE ct.gioHang.khachHang.maKH = :maKH")
    int sumQuantityByMaKH(@Param("maKH") Long maKH);
}
