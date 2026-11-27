package com.bookmind.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "DIACHI")
@Getter
@Setter
public class DiaChi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDiaChi")
    private Long maDiaChi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaKH", nullable = false)
    private KhachHang khachHang;

    @Column(name = "HoTenNguoiNhan", nullable = false, length = 100)
    private String hoTenNguoiNhan;

    @Column(name = "SDTNguoiNhan", nullable = false, length = 15)
    private String sdtNguoiNhan;

    @Column(name = "DiaChiNhan", nullable = false, length = 500)
    private String diaChiNhan;

    @Column(name = "MacDinh", nullable = false)
    private Boolean macDinh = false;

    @Column(name = "NgayTao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    public Long getMaDiaChi() { return maDiaChi; }
    public void setMaDiaChi(Long maDiaChi) { this.maDiaChi = maDiaChi; }
    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang khachHang) { this.khachHang = khachHang; }
    public String getHoTenNguoiNhan() { return hoTenNguoiNhan; }
    public void setHoTenNguoiNhan(String hoTenNguoiNhan) { this.hoTenNguoiNhan = hoTenNguoiNhan; }
    public String getSdtNguoiNhan() { return sdtNguoiNhan; }
    public void setSdtNguoiNhan(String sdtNguoiNhan) { this.sdtNguoiNhan = sdtNguoiNhan; }
    public String getDiaChiNhan() { return diaChiNhan; }
    public void setDiaChiNhan(String diaChiNhan) { this.diaChiNhan = diaChiNhan; }
    public Boolean getMacDinh() { return macDinh; }
    public void setMacDinh(Boolean macDinh) { this.macDinh = macDinh; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
}
