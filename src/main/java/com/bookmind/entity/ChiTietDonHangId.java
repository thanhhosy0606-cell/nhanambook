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
public class ChiTietDonHangId implements Serializable {

    @Column(name = "MaDH")
    private Long maDH;

    @Column(name = "MaSach")
    private Long maSach;

    public ChiTietDonHangId() {
    }

    public ChiTietDonHangId(Long maDH, Long maSach) {
        this.maDH = maDH;
        this.maSach = maSach;
    }

    public Long getMaDH() {
        return maDH;
    }

    public void setMaDH(Long maDH) {
        this.maDH = maDH;
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
        ChiTietDonHangId that = (ChiTietDonHangId) o;
        return Objects.equals(maDH, that.maDH) && Objects.equals(maSach, that.maSach);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maDH, maSach);
    }
}
