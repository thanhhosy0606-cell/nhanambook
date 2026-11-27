package com.bookmind.repository;

import com.bookmind.entity.PtthThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PtthThanhToanRepository extends JpaRepository<PtthThanhToan, Long> {
    List<PtthThanhToan> findByTrangThai(String trangThai);
    Optional<PtthThanhToan> findByTenPTTT(String tenPTTT);
}
