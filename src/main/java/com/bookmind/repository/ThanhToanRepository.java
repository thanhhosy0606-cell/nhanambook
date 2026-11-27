package com.bookmind.repository;

import com.bookmind.entity.ThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThanhToanRepository extends JpaRepository<ThanhToan, Long> {
    List<ThanhToan> findByDonHang_MaDH(Long maDH);
    List<ThanhToan> findByDonHang_MaDHOrderByMaThanhToanDesc(Long maDH);
    Optional<ThanhToan> findByMaGiaoDich(String maGiaoDich);
}
