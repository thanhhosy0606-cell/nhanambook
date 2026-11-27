package com.bookmind.repository;

import com.bookmind.entity.MaGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaGiamGiaRepository extends JpaRepository<MaGiamGia, Long> {
    Optional<MaGiamGia> findByMaCodeIgnoreCase(String maCode);
    List<MaGiamGia> findByTrangThai(String trangThai);
    boolean existsByMaCodeIgnoreCase(String maCode);

    @Modifying
    @Query("UPDATE MaGiamGia m SET m.soLuong = m.soLuong - 1 WHERE m.maMaGiamGia = :id AND m.soLuong > 0")
    int deductCouponQuantityAtomic(@Param("id") Long id);
}
