package com.bookmind.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmind.entity.LoaiSach;

@Repository
public interface LoaiSachRepository extends JpaRepository<LoaiSach, Long> {
    List<LoaiSach> findByTrangThai(String trangThai);
    Optional<LoaiSach> findByTenLoaiIgnoreCase(String tenLoai);
    boolean existsByTenLoai(String tenLoai);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM LoaiSach c WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(c.tenLoai) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.moTa) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:trangThai IS NULL OR :trangThai = '' OR :trangThai = 'ALL' OR c.trangThai = :trangThai) " +
           "ORDER BY c.maLoai ASC")
    List<LoaiSach> searchCategories(@org.springframework.data.repository.query.Param("keyword") String keyword,
                                    @org.springframework.data.repository.query.Param("trangThai") String trangThai);
}
