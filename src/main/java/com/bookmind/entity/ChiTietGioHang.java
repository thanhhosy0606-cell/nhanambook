package com.bookmind.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "CHITIETGIOHANG")
@Getter
@Setter
public class ChiTietGioHang {

    @EmbeddedId
    private ChiTietGioHangId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("maGioHang")
    @JoinColumn(name = "MaGioHang", nullable = false)
    private GioHang gioHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("maSach")
    @JoinColumn(name = "MaSach", nullable = false)
    private Sach sach;

    @Column(name = "SoLuong", nullable = false)
    private Integer soLuong;

    @Column(name = "DonGia", nullable = false, precision = 15, scale = 2)
    private BigDecimal donGia;

    public BigDecimal getThanhTien() {
        if (donGia == null || soLuong == null) return BigDecimal.ZERO;
        return donGia.multiply(BigDecimal.valueOf(soLuong));
    }

    public ChiTietGioHangId getId() { return id; }
    public void setId(ChiTietGioHangId id) { this.id = id; }
    public GioHang getGioHang() { return gioHang; }
    public void setGioHang(GioHang gioHang) { this.gioHang = gioHang; }
    public Sach getSach() { return sach; }
    public void setSach(Sach sach) { this.sach = sach; }
    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
}
