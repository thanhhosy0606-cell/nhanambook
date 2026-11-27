package com.bookmind.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "SACH")
@Getter
@Setter
public class Sach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaSach")
    private Long maSach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaLoai", nullable = false)
    private LoaiSach loaiSach;

    @Column(name = "TenSach", nullable = false)
    private String tenSach;

    @Column(name = "TacGia", nullable = false)
    private String tacGia;

    @Column(name = "ISBN", length = 20, unique = true)
    private String isbn;

    @Column(name = "SoLuong", nullable = false)
    private Integer soLuong = 0;

    @Column(name = "MoTa", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "GiaBan", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaBan;

    @Column(name = "HinhAnh", length = 500)
    private String hinhAnh;

    @Column(name = "NXB")
    private String nxb;

    @Column(name = "NamXB")
    private Integer namXb;

    @Column(name = "SoTrang")
    private Integer soTrang;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = "DANG_BAN"; // DANG_BAN, HET_HANG, NGUNG_BAN

    @Column(name = "NgayTao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "NgayCapNhat", insertable = false, updatable = false)
    private LocalDateTime ngayCapNhat;

    @org.hibernate.annotations.BatchSize(size = 50)
    @OneToMany(mappedBy = "sach", fetch = FetchType.LAZY)
    private java.util.List<DanhGia> danhSachDanhGia;

    public int getSoLuotDanhGia() {
        if (danhSachDanhGia == null) return 0;
        return (int) danhSachDanhGia.stream()
                .filter(d -> "HIEN_THI".equalsIgnoreCase(d.getTrangThai()))
                .count();
    }

    public double getDiemDanhGia() {
        if (danhSachDanhGia == null) return 0.0;
        java.util.OptionalDouble avg = danhSachDanhGia.stream()
                .filter(d -> "HIEN_THI".equalsIgnoreCase(d.getTrangThai()) && d.getSoSao() != null)
                .mapToInt(DanhGia::getSoSao)
                .average();
        if (avg.isEmpty()) return 0.0;
        return Math.round(avg.getAsDouble() * 10.0) / 10.0;
    }

    public int getSoSaoLamTron() {
        double diem = getDiemDanhGia();
        if (diem <= 0.0) return 0;
        return (int) Math.round(diem);
    }

    public Long getMaSach() { return maSach; }
    public void setMaSach(Long maSach) { this.maSach = maSach; }
    public LoaiSach getLoaiSach() { return loaiSach; }
    public void setLoaiSach(LoaiSach loaiSach) { this.loaiSach = loaiSach; }
    public String getTenSach() { return tenSach; }
    public void setTenSach(String tenSach) { this.tenSach = tenSach; }
    public String getTacGia() { return tacGia; }
    public void setTacGia(String tacGia) { this.tacGia = tacGia; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public BigDecimal getGiaBan() { return giaBan; }
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }
    public String getHinhAnh() { return hinhAnh; }
    public void setHinhAnh(String hinhAnh) { this.hinhAnh = hinhAnh; }
    public String getNxb() { return nxb; }
    public void setNxb(String nxb) { this.nxb = nxb; }
    public Integer getNamXb() { return namXb; }
    public void setNamXb(Integer namXb) { this.namXb = namXb; }
    public Integer getSoTrang() { return soTrang; }
    public void setSoTrang(Integer soTrang) { this.soTrang = soTrang; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
    public LocalDateTime getNgayCapNhat() { return ngayCapNhat; }
    public void setNgayCapNhat(LocalDateTime ngayCapNhat) { this.ngayCapNhat = ngayCapNhat; }
    public java.util.List<DanhGia> getDanhSachDanhGia() { return danhSachDanhGia; }
    public void setDanhSachDanhGia(java.util.List<DanhGia> danhSachDanhGia) { this.danhSachDanhGia = danhSachDanhGia; }
}
