package com.bookmind.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "LOAISACH")
@Getter
@Setter
public class LoaiSach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaLoai")
    private Long maLoai;

    @Column(name = "TenLoai", nullable = false, unique = true, length = 100)
    private String tenLoai;

    @Column(name = "HinhAnh", length = 500)
    private String hinhAnh;

    @Column(name = "MoTa", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = "HOAT_DONG"; // HOAT_DONG, NGUNG

    @Column(name = "NgayTao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "NgayCapNhat", insertable = false, updatable = false)
    private LocalDateTime ngayCapNhat;

    @OneToMany(mappedBy = "loaiSach", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Sach> danhSachSach = new ArrayList<>();

    public LoaiSach() {
    }

    public Long getMaLoai() {
        return maLoai;
    }

    public void setMaLoai(Long maLoai) {
        this.maLoai = maLoai;
    }

    public String getTenLoai() {
        return tenLoai;
    }

    public void setTenLoai(String tenLoai) {
        this.tenLoai = tenLoai;
    }

    public String getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(String hinhAnh) {
        this.hinhAnh = hinhAnh;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
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

    public List<Sach> getDanhSachSach() {
        return danhSachSach;
    }

    public void setDanhSachSach(List<Sach> danhSachSach) {
        this.danhSachSach = danhSachSach;
    }
}
