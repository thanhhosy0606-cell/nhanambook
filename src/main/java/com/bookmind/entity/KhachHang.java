package com.bookmind.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "KHACHHANG")
@Getter
@Setter
public class KhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaKH")
    private Long maKH;

    @OneToOne(mappedBy = "khachHang", fetch = FetchType.LAZY)
    private TaiKhoan taiKhoan;

    @Column(name = "HoTen", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "SDT", unique = true, length = 15)
    private String sdt;

    @Column(name = "GioiTinh")
    private Boolean gioiTinh; // true = 1 (Nam), false = 0 (Nu)

    @Column(name = "AnhDaiDien", length = 500)
    private String anhDaiDien;

    @Column(name = "NgaySinh")
    private LocalDate ngaySinh;

    @OneToOne(mappedBy = "khachHang", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private GioHang gioHang;

    @OneToMany(mappedBy = "khachHang", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaChi> danhSachDiaChi = new ArrayList<>();

    public Long getMaKH() { return maKH; }
    public void setMaKH(Long maKH) { this.maKH = maKH; }

    public TaiKhoan getTaiKhoan() { return taiKhoan; }
    public void setTaiKhoan(TaiKhoan taiKhoan) { this.taiKhoan = taiKhoan; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getSdt() { return sdt; }
    public void setSdt(String sdt) { this.sdt = sdt; }

    public Boolean getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(Boolean gioiTinh) { this.gioiTinh = gioiTinh; }

    public String getAnhDaiDien() { return anhDaiDien; }
    public void setAnhDaiDien(String anhDaiDien) { this.anhDaiDien = anhDaiDien; }

    public LocalDate getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }

    public GioHang getGioHang() { return gioHang; }
    public void setGioHang(GioHang gioHang) { this.gioHang = gioHang; }

    public List<DiaChi> getDanhSachDiaChi() { return danhSachDiaChi; }
    public void setDanhSachDiaChi(List<DiaChi> danhSachDiaChi) { this.danhSachDiaChi = danhSachDiaChi; }
}
