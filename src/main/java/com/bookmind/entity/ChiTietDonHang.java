package com.bookmind.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "CHITIETDONHANG")
@Getter
@Setter
public class ChiTietDonHang {

    @EmbeddedId
    private ChiTietDonHangId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("maDH")
    @JoinColumn(name = "MaDH", nullable = false)
    private DonHang donHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("maSach")
    @JoinColumn(name = "MaSach", nullable = false)
    private Sach sach;

    @Column(name = "SoLuong", nullable = false)
    private Integer soLuong;

    @Column(name = "DonGia", nullable = false, precision = 15, scale = 2)
    private BigDecimal donGia;

    public ChiTietDonHang() {
    }

    @Transient
    public BigDecimal getThanhTien() {
        if (donGia == null || soLuong == null) {
            return BigDecimal.ZERO;
        }
        return donGia.multiply(BigDecimal.valueOf(soLuong));
    }

    public ChiTietDonHangId getId() { return id; }
    public void setId(ChiTietDonHangId id) { this.id = id; }
    public DonHang getDonHang() { return donHang; }
    public void setDonHang(DonHang donHang) { this.donHang = donHang; }
    public Sach getSach() { return sach; }
    public void setSach(Sach sach) { this.sach = sach; }
    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
}
