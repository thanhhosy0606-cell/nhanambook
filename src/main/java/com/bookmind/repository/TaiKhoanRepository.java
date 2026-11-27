package com.bookmind.repository;

import com.bookmind.entity.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Long> {
    Optional<TaiKhoan> findByEmail(String email);
    Optional<TaiKhoan> findByTenDN(String tenDN);
    Optional<TaiKhoan> findByTenDNOrEmail(String tenDN, String email);
    boolean existsByEmail(String email);
    boolean existsByTenDN(String tenDN);
}
