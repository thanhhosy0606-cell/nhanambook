package com.bookmind.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
public class ChiTietGioHangId implements Serializable {

    @Column(name = "MaGioHang")
    private Long maGioHang;

    @Column(name = "MaSach")
    private Long maSach;

    public ChiTietGioHangId() {
    }

    public ChiTietGioHangId(Long maGioHang, Long maSach) {
        this.maGioHang = maGioHang;
        this.maSach = maSach;
    }

    public Long getMaGioHang() {
        return maGioHang;
    }

    public void setMaGioHang(Long maGioHang) {
        this.maGioHang = maGioHang;
    }

    public Long getMaSach() {
        return maSach;
    }

    public void setMaSach(Long maSach) {
        this.maSach = maSach;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChiTietGioHangId that = (ChiTietGioHangId) o;
        return Objects.equals(maGioHang, that.maGioHang) && Objects.equals(maSach, that.maSach);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maGioHang, maSach);
    }
}
