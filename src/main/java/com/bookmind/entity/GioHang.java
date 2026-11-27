package com.bookmind.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "GIOHANG")
@Getter
@Setter
public class GioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaGioHang")
    private Long maGioHang;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaKH", nullable = false, unique = true)
    private KhachHang khachHang;

    @Column(name = "NgayTao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "NgayCapNhat", insertable = false, updatable = false)
    private LocalDateTime ngayCapNhat;

    @OneToMany(mappedBy = "gioHang", fetch = FetchType.LAZY)
    private List<ChiTietGioHang> chiTietList = new ArrayList<>();

    public GioHang() {
    }

    public Long getMaGioHang() {
        return maGioHang;
    }

    public void setMaGioHang(Long maGioHang) {
        this.maGioHang = maGioHang;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
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

    public List<ChiTietGioHang> getChiTietList() {
        return chiTietList;
    }

    public void setChiTietList(List<ChiTietGioHang> chiTietList) {
        this.chiTietList = chiTietList;
    }
}
