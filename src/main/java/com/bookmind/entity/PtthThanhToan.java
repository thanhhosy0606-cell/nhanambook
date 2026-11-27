package com.bookmind.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "PTTHANHTOAN")
@Getter
@Setter
public class PtthThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaPTTT")
    private Long maPTTT;

    @Column(name = "TenPTTT", nullable = false, unique = true, length = 50)
    private String tenPTTT;

    @Column(name = "MoTa", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = "HOAT_DONG"; // HOAT_DONG, NGUNG

    public Long getMaPTTT() { return maPTTT; }
    public void setMaPTTT(Long maPTTT) { this.maPTTT = maPTTT; }
    public String getTenPTTT() { return tenPTTT; }
    public void setTenPTTT(String tenPTTT) { this.tenPTTT = tenPTTT; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
}
