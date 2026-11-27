package com.bookmind.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "DANHGIA", uniqueConstraints = {
    @UniqueConstraint(name = "UQ_DANHGIA_DH_SACH", columnNames = {"MaDH", "MaSach"})
})
@Getter
@Setter
public class DanhGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDanhGia")
    private Long maDanhGia;

    @Column(name = "SoSao", nullable = false)
    private Integer soSao;

    @Column(name = "NoiDung", columnDefinition = "TEXT")
    private String noiDung;

    @Column(name = "NgayDanhGia")
    private LocalDateTime ngayDanhGia;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = "HIEN_THI"; // HIEN_THI, AN

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDH", nullable = false)
    private DonHang donHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSach", nullable = false)
    private Sach sach;

    public DanhGia() {
    }

    @PrePersist
    public void prePersist() {
        if (this.ngayDanhGia == null) {
            this.ngayDanhGia = LocalDateTime.now();
        }
        if (this.trangThai == null) {
            this.trangThai = "HIEN_THI";
        }
    }

    @Transient
    public KhachHang getKhachHang() {
        return donHang != null ? donHang.getKhachHang() : null;
    }

    public Long getMaDanhGia() { return maDanhGia; }
    public void setMaDanhGia(Long maDanhGia) { this.maDanhGia = maDanhGia; }
    public DonHang getDonHang() { return donHang; }
    public void setDonHang(DonHang donHang) { this.donHang = donHang; }
    public Sach getSach() { return sach; }
    public void setSach(Sach sach) { this.sach = sach; }
    public Integer getSoSao() { return soSao; }
    public void setSoSao(Integer soSao) { this.soSao = soSao; }
    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String noiDung) { this.noiDung = noiDung; }
    public LocalDateTime getNgayDanhGia() { return ngayDanhGia; }
    public void setNgayDanhGia(LocalDateTime ngayDanhGia) { this.ngayDanhGia = ngayDanhGia; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
}
