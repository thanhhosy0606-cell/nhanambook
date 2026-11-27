package com.bookmind.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "KH_MGG")
public class KhachHangMaGiamGia {

    @EmbeddedId
    private KhachHangMaGiamGiaId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("maGiamGia")
    @JoinColumn(name = "MaGiamGia", nullable = false)
    private MaGiamGia maGiamGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("maKH")
    @JoinColumn(name = "MaKH", nullable = false)
    private KhachHang khachHang;

    public KhachHangMaGiamGia() {
    }

    public KhachHangMaGiamGia(MaGiamGia maGiamGia, KhachHang khachHang) {
        this.id = new KhachHangMaGiamGiaId(maGiamGia.getMaMaGiamGia(), khachHang.getMaKH());
        this.maGiamGia = maGiamGia;
        this.khachHang = khachHang;
    }

    public KhachHangMaGiamGiaId getId() {
        return id;
    }

    public void setId(KhachHangMaGiamGiaId id) {
        this.id = id;
    }

    public MaGiamGia getMaGiamGia() {
        return maGiamGia;
    }

    public void setMaGiamGia(MaGiamGia maGiamGia) {
        this.maGiamGia = maGiamGia;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }
}
