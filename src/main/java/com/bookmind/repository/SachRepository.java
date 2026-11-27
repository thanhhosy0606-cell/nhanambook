package com.bookmind.repository;

import com.bookmind.entity.Sach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SachRepository extends JpaRepository<Sach, Long>, JpaSpecificationExecutor<Sach> {
    List<Sach> findByTrangThai(String trangThai);
    Page<Sach> findByTrangThai(String trangThai, Pageable pageable);

    List<Sach> findByLoaiSach_MaLoaiAndTrangThai(Long maLoai, String trangThai);
    Page<Sach> findByLoaiSach_MaLoaiAndTrangThai(Long maLoai, String trangThai, Pageable pageable);

    @Query("SELECT s FROM Sach s WHERE s.trangThai = :trangThai AND " +
           "(LOWER(s.tenSach) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.tacGia) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Sach> searchBooks(@Param("keyword") String keyword, @Param("trangThai") String trangThai, Pageable pageable);

    @Query("SELECT DISTINCT s.tacGia FROM Sach s WHERE s.trangThai = 'DANG_BAN' AND s.tacGia IS NOT NULL ORDER BY s.tacGia")
    List<String> findDistinctTacGia();

    long countByLoaiSach_MaLoai(Long maLoai);

    boolean existsByIsbn(String isbn);

    @Modifying
    @Query("UPDATE Sach s SET s.trangThai = CASE WHEN s.soLuong = :qty THEN 'HET_HANG' ELSE s.trangThai END, " +
           "s.soLuong = s.soLuong - :qty " +
           "WHERE s.maSach = :maSach AND s.soLuong >= :qty AND s.trangThai = 'DANG_BAN'")
    int deductStockAtomic(@Param("maSach") Long maSach, @Param("qty") int qty);
}
