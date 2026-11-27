package com.bookmind.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "THANHTOAN")
@Getter
@Setter
public class ThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaThanhToan")
    private Long maThanhToan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDH", nullable = false)
    private DonHang donHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaPTTT", nullable = false)
    private PtthThanhToan ptthThanhToan;

    @Column(name = "SoTien", nullable = false, precision = 15, scale = 2)
    private BigDecimal soTien;

    @Column(name = "MaGiaoDich", length = 150)
    private String maGiaoDich;

    @Column(name = "ThoiGianTT")
    private LocalDateTime thoiGianTT;

    @Column(name = "TrangThai", nullable = false, length = 30)
    private String trangThai = "CHO_THANH_TOAN"; // CHO_THANH_TOAN, THANH_CONG, THAT_BAI, DA_HOAN_TIEN

    @Column(name = "NoiDung", columnDefinition = "TEXT")
    private String noiDung;

    public Long getMaThanhToan() { return maThanhToan; }
    public void setMaThanhToan(Long maThanhToan) { this.maThanhToan = maThanhToan; }
    public DonHang getDonHang() { return donHang; }
    public void setDonHang(DonHang donHang) { this.donHang = donHang; }
    public PtthThanhToan getPtthThanhToan() { return ptthThanhToan; }
    public void setPtthThanhToan(PtthThanhToan ptthThanhToan) { this.ptthThanhToan = ptthThanhToan; }
    public BigDecimal getSoTien() { return soTien; }
    public void setSoTien(BigDecimal soTien) { this.soTien = soTien; }
    public String getMaGiaoDich() { return maGiaoDich; }
    public void setMaGiaoDich(String maGiaoDich) { this.maGiaoDich = maGiaoDich; }
    public LocalDateTime getThoiGianTT() { return thoiGianTT; }
    public void setThoiGianTT(LocalDateTime thoiGianTT) { this.thoiGianTT = thoiGianTT; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String noiDung) { this.noiDung = noiDung; }
}
