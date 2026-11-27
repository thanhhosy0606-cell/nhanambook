package com.bookmind.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "DONHANG")
@Getter
@Setter
public class DonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDH")
    private Long maDH;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaKH", nullable = false)
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaGiamGia")
    private MaGiamGia maGiamGia;

    @Column(name = "NgayDat", insertable = false, updatable = false)
    private LocalDateTime ngayDat;

    @Column(name = "GiaTamTinh", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaTamTinh = BigDecimal.ZERO;

    @Column(name = "GiamGia", nullable = false, precision = 15, scale = 2)
    private BigDecimal giamGia = BigDecimal.ZERO;

    @Column(name = "PhiVC", nullable = false, precision = 15, scale = 2)
    private BigDecimal phiVC = BigDecimal.ZERO;

    @Column(name = "TongTien", nullable = false, precision = 15, scale = 2)
    private BigDecimal tongTien = BigDecimal.ZERO;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = "CHO_XAC_NHAN"; // CHO_XAC_NHAN, DA_XAC_NHAN, DANG_GIAO, DA_GIAO, DA_HUY

    @Column(name = "GhiChu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "NgayCapNhat", insertable = false, updatable = false)
    private LocalDateTime ngayCapNhat;

    @Column(name = "ThoiGianHetHanTT")
    private LocalDateTime thoiGianHetHanTT;

    @OneToMany(mappedBy = "donHang", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChiTietDonHang> chiTietList = new ArrayList<>();

    @OneToMany(mappedBy = "donHang", cascade = CascadeType.ALL)
    private List<ThanhToan> thanhToanList = new ArrayList<>();

    private static final String RECEIVER_PREFIX = "[RECEIVER:";

    public DonHang() {
    }

    /**
     * Mã hóa snapshot thông tin người nhận vào đầu cột GhiChu (giữ nguyên 100% ERD bảng DONHANG).
     */
    public static String encodeReceiverSnapshot(String hoTen, String sdt, String diaChi, String userNote) {
        String safeName = sanitizeToken(hoTen);
        String safePhone = sanitizeToken(sdt);
        String safeAddr = sanitizeToken(diaChi);
        String header = RECEIVER_PREFIX + safeName + "|" + safePhone + "|" + safeAddr + "]";
        if (userNote != null && !userNote.trim().isEmpty()) {
            return header + "\n" + userNote.trim();
        }
        return header;
    }

    private static String sanitizeToken(String s) {
        if (s == null) return "";
        return s.trim().replace("|", "/").replace("]", ")").replace("\r", " ").replace("\n", " ");
    }

    @Transient
    private String[] parseReceiverSnapshot() {
        if (ghiChu == null || !ghiChu.startsWith(RECEIVER_PREFIX)) {
            return null;
        }
        int endIdx = ghiChu.indexOf(']');
        if (endIdx <= RECEIVER_PREFIX.length()) {
            return null;
        }
        String payload = ghiChu.substring(RECEIVER_PREFIX.length(), endIdx);
        String[] parts = payload.split("\\|", -1);
        return parts.length == 3 ? parts : null;
    }

    @Transient
    public String getGhiChuHienThi() {
        if (ghiChu == null) return "";
        if (ghiChu.startsWith(RECEIVER_PREFIX)) {
            int endIdx = ghiChu.indexOf(']');
            if (endIdx >= 0) {
                String rest = ghiChu.substring(endIdx + 1).trim();
                if (rest.startsWith("|")) {
                    rest = rest.substring(1).trim();
                }
                return rest;
            }
        }
        return ghiChu;
    }

    @Transient
    private DiaChi resolveDefaultAddress() {
        if (khachHang == null || khachHang.getDanhSachDiaChi() == null || khachHang.getDanhSachDiaChi().isEmpty()) {
            return null;
        }
        return khachHang.getDanhSachDiaChi().stream()
                .filter(d -> Boolean.TRUE.equals(d.getMacDinh()))
                .findFirst()
                .orElse(khachHang.getDanhSachDiaChi().get(0));
    }

    @Transient
    public String getHoTenNguoiNhan() {
        String[] snap = parseReceiverSnapshot();
        if (snap != null && !snap[0].isBlank()) {
            return snap[0];
        }
        DiaChi dc = resolveDefaultAddress();
        if (dc != null && dc.getHoTenNguoiNhan() != null && !dc.getHoTenNguoiNhan().isBlank()) {
            return dc.getHoTenNguoiNhan();
        }
        return khachHang != null && khachHang.getHoTen() != null ? khachHang.getHoTen() : "Khách hàng";
    }

    @Transient
    public String getSdtNguoiNhan() {
        String[] snap = parseReceiverSnapshot();
        if (snap != null && !snap[1].isBlank()) {
            return snap[1];
        }
        DiaChi dc = resolveDefaultAddress();
        if (dc != null && dc.getSdtNguoiNhan() != null && !dc.getSdtNguoiNhan().isBlank()) {
            return dc.getSdtNguoiNhan();
        }
        return khachHang != null && khachHang.getSdt() != null ? khachHang.getSdt() : "";
    }

    @Transient
    public String getDiaChiGiaoHang() {
        String[] snap = parseReceiverSnapshot();
        if (snap != null && !snap[2].isBlank()) {
            return snap[2];
        }
        DiaChi dc = resolveDefaultAddress();
        if (dc != null && dc.getDiaChiNhan() != null && !dc.getDiaChiNhan().isBlank()) {
            return dc.getDiaChiNhan();
        }
        return "Chưa cập nhật";
    }

    public Long getMaDH() { return maDH; }
    public void setMaDH(Long maDH) { this.maDH = maDH; }
    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang khachHang) { this.khachHang = khachHang; }
    public MaGiamGia getMaGiamGia() { return maGiamGia; }
    public void setMaGiamGia(MaGiamGia maGiamGia) { this.maGiamGia = maGiamGia; }
    public LocalDateTime getNgayDat() { return ngayDat; }
    public void setNgayDat(LocalDateTime ngayDat) { this.ngayDat = ngayDat; }
    public BigDecimal getGiaTamTinh() { return giaTamTinh; }
    public void setGiaTamTinh(BigDecimal giaTamTinh) { this.giaTamTinh = giaTamTinh; }
    public BigDecimal getGiamGia() { return giamGia; }
    public void setGiamGia(BigDecimal giamGia) { this.giamGia = giamGia; }
    public BigDecimal getPhiVC() { return phiVC; }
    public void setPhiVC(BigDecimal phiVC) { this.phiVC = phiVC; }
    public BigDecimal getTongTien() { return tongTien; }
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public LocalDateTime getNgayCapNhat() { return ngayCapNhat; }
    public void setNgayCapNhat(LocalDateTime ngayCapNhat) { this.ngayCapNhat = ngayCapNhat; }
    public LocalDateTime getThoiGianHetHanTT() { return thoiGianHetHanTT; }
    public void setThoiGianHetHanTT(LocalDateTime thoiGianHetHanTT) { this.thoiGianHetHanTT = thoiGianHetHanTT; }
    public List<ChiTietDonHang> getChiTietList() { return chiTietList; }
    public void setChiTietList(List<ChiTietDonHang> chiTietList) { this.chiTietList = chiTietList; }
    public List<ThanhToan> getThanhToanList() { return thanhToanList; }
    public void setThanhToanList(List<ThanhToan> thanhToanList) { this.thanhToanList = thanhToanList; }
}
