package com.bookmind.repository;

import com.bookmind.entity.DiaChi;
import com.bookmind.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiaChiRepository extends JpaRepository<DiaChi, Long> {

    @Query("SELECT d FROM DiaChi d WHERE d.khachHang.maKH = :maKH")
    List<DiaChi> findByKhachHang_MaKH(@Param("maKH") Long maKH);

    @Query("SELECT d FROM DiaChi d WHERE d.khachHang.maKH = :maKH AND d.macDinh = true")
    Optional<DiaChi> findByKhachHang_MaKHAndMacDinhTrue(@Param("maKH") Long maKH);

    @Query("SELECT d FROM DiaChi d WHERE d.maDiaChi = :maDiaChi AND d.khachHang.maKH = :maKH")
    Optional<DiaChi> findByMaDiaChiAndKhachHang_MaKH(@Param("maDiaChi") Long maDiaChi, @Param("maKH") Long maKH);

    List<DiaChi> findByKhachHang(KhachHang khachHang);
}
