package com.bookmind.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TAIKHOAN")
@Getter
@Setter
public class TaiKhoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTK")
    private Long maTK;

    @Column(name = "Email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "TenDN", unique = true, length = 50)
    private String tenDN;

    @Column(name = "MatKhau", length = 255)
    private String matKhau;

    @Column(name = "PTDangNhap", nullable = false, length = 30)
    private String ptDangNhap = "LOCAL"; // LOCAL, GOOGLE, FACEBOOK

    @Column(name = "VaiTro", nullable = false, length = 20)
    private String vaiTro = "KHACH_HANG"; // KHACH_HANG, ADMIN

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = "HOAT_DONG"; // HOAT_DONG, KHOA

    @Column(name = "NgayTao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "NgayCapNhat", insertable = false, updatable = false)
    private LocalDateTime ngayCapNhat;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "MaKH")
    private KhachHang khachHang;

    // Explicit Getters and Setters (prevents IDE/Lombok desync errors)
    public Long getMaTK() {
        return maTK;
    }

    public void setMaTK(Long maTK) {
        this.maTK = maTK;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTenDN() {
        return tenDN;
    }

    public void setTenDN(String tenDN) {
        this.tenDN = tenDN;
    }

    public String getMatKhau() {
        return matKhau;
    }

    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }

    public String getPtDangNhap() {
        return ptDangNhap;
    }

    public void setPtDangNhap(String ptDangNhap) {
        this.ptDangNhap = ptDangNhap;
    }

    public String getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(String vaiTro) {
        this.vaiTro = vaiTro;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    public LocalDateTime getNgayCapNhat() {
        return ngayCapNhat;
    }

    public void setNgayCapNhat(LocalDateTime ngayCapNhat) {
        this.ngayCapNhat = ngayCapNhat;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }
}
