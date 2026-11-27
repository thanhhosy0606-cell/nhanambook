package com.bookmind.repository;

import com.bookmind.entity.KhachHang;
import com.bookmind.entity.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Long>, JpaSpecificationExecutor<KhachHang> {
    Optional<KhachHang> findByTaiKhoan(TaiKhoan taiKhoan);
    Optional<KhachHang> findByTaiKhoan_MaTK(Long maTK);
    Optional<KhachHang> findBySdt(String sdt);
    boolean existsBySdt(String sdt);
}
