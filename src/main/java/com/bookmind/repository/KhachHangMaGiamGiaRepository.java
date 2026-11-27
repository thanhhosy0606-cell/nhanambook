package com.bookmind.repository;

import com.bookmind.entity.KhachHangMaGiamGia;
import com.bookmind.entity.KhachHangMaGiamGiaId;
import com.bookmind.entity.MaGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhachHangMaGiamGiaRepository extends JpaRepository<KhachHangMaGiamGia, KhachHangMaGiamGiaId> {

    @Query("SELECT km.maGiamGia FROM KhachHangMaGiamGia km WHERE km.khachHang.maKH = :maKH ORDER BY km.maGiamGia.maMaGiamGia DESC")
    List<MaGiamGia> findSavedCouponsByMaKH(@Param("maKH") Long maKH);

    boolean existsById_MaKHAndId_MaGiamGia(Long maKH, Long maGiamGia);

    void deleteById_MaKHAndId_MaGiamGia(Long maKH, Long maGiamGia);

    void deleteById_MaKH(Long maKH);

    void deleteById_MaGiamGia(Long maGiamGia);
}
