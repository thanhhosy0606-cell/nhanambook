package com.bookmind.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TopBookDto {
    private Long maSach;
    private String tenSach;
    private String hinhAnh;
    private String tenLoai;
    private long soLuongDaBan;
    private BigDecimal tongDoanhThu;

    public TopBookDto() {
    }

    public Long getMaSach() {
        return maSach;
    }

    public void setMaSach(Long maSach) {
        this.maSach = maSach;
    }

    public String getTenSach() {
        return tenSach;
    }

    public void setTenSach(String tenSach) {
        this.tenSach = tenSach;
    }

    public String getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(String hinhAnh) {
        this.hinhAnh = hinhAnh;
    }

    public String getTenLoai() {
        return tenLoai;
    }

    public void setTenLoai(String tenLoai) {
        this.tenLoai = tenLoai;
    }

    public long getSoLuongDaBan() {
        return soLuongDaBan;
    }

    public void setSoLuongDaBan(long soLuongDaBan) {
        this.soLuongDaBan = soLuongDaBan;
    }

    public BigDecimal getTongDoanhThu() {
        return tongDoanhThu;
    }

    public void setTongDoanhThu(BigDecimal tongDoanhThu) {
        this.tongDoanhThu = tongDoanhThu;
    }
}
