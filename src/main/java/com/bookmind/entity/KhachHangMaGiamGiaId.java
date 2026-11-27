package com.bookmind.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class KhachHangMaGiamGiaId implements Serializable {

    @Column(name = "MaGiamGia")
    private Long maGiamGia;

    @Column(name = "MaKH")
    private Long maKH;

    public KhachHangMaGiamGiaId() {
    }

    public KhachHangMaGiamGiaId(Long maGiamGia, Long maKH) {
        this.maGiamGia = maGiamGia;
        this.maKH = maKH;
    }

    public Long getMaGiamGia() {
        return maGiamGia;
    }

    public void setMaGiamGia(Long maGiamGia) {
        this.maGiamGia = maGiamGia;
    }

    public Long getMaKH() {
        return maKH;
    }

    public void setMaKH(Long maKH) {
        this.maKH = maKH;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KhachHangMaGiamGiaId that = (KhachHangMaGiamGiaId) o;
        return Objects.equals(maGiamGia, that.maGiamGia) && Objects.equals(maKH, that.maKH);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maGiamGia, maKH);
    }
}
