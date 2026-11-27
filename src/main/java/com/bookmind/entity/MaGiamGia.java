package com.bookmind.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "MAGIAMGIA")
@Getter
@Setter
public class MaGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaGiamGia")
    private Long maMaGiamGia;

    @Column(name = "MaCode", nullable = false, unique = true, length = 50)
    private String maCode;

    @Column(name = "TenChuongTrinh", nullable = false, length = 100)
    private String tenMa;

    @Column(name = "PhanTramGiam", precision = 5, scale = 2)
    private BigDecimal phanTramGiam;

    @Column(name = "SoTienGiamToiDa", precision = 15, scale = 2)
    private BigDecimal soTienGiamToiDa;

    @Column(name = "GiaTriDonToiThieu", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaTriDonToiThieu = BigDecimal.ZERO;

    @Column(name = "NgayBatDau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "NgayKetThuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "SoLuong", nullable = false)
    private Integer soLuong = 0;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = "HOAT_DONG"; // HOAT_DONG, NGUNG

    public MaGiamGia() {
    }

    public Long getMaMaGiamGia() {
        return maMaGiamGia;
    }

    public void setMaMaGiamGia(Long maMaGiamGia) {
        this.maMaGiamGia = maMaGiamGia;
    }

    public String getMaCode() {
        return maCode;
    }

    public void setMaCode(String maCode) {
        this.maCode = maCode;
    }

    public String getTenMa() {
        return tenMa;
    }

    public void setTenMa(String tenMa) {
        this.tenMa = tenMa;
    }

    public BigDecimal getPhanTramGiam() {
        return phanTramGiam;
    }

    public void setPhanTramGiam(BigDecimal phanTramGiam) {
        this.phanTramGiam = phanTramGiam;
    }

    public BigDecimal getSoTienGiamToiDa() {
        return soTienGiamToiDa;
    }

    public void setSoTienGiamToiDa(BigDecimal soTienGiamToiDa) {
        this.soTienGiamToiDa = soTienGiamToiDa;
    }

    public BigDecimal getGiaTriDonToiThieu() {
        return giaTriDonToiThieu;
    }

    public void setGiaTriDonToiThieu(BigDecimal giaTriDonToiThieu) {
        this.giaTriDonToiThieu = giaTriDonToiThieu;
    }

    public LocalDateTime getNgayBatDau() {
        return ngayBatDau;
    }

    public void setNgayBatDau(LocalDateTime ngayBatDau) {
        this.ngayBatDau = ngayBatDau;
    }

    public LocalDateTime getNgayKetThuc() {
        return ngayKetThuc;
    }

    public void setNgayKetThuc(LocalDateTime ngayKetThuc) {
        this.ngayKetThuc = ngayKetThuc;
    }

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer soLuong) {
        this.soLuong = soLuong;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}
