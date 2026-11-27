package com.bookmind.repository;

import com.bookmind.entity.GioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GioHangRepository extends JpaRepository<GioHang, Long> {
    Optional<GioHang> findByKhachHang_MaKH(Long maKH);
    Optional<GioHang> findByKhachHang(com.bookmind.entity.KhachHang khachHang);
}
