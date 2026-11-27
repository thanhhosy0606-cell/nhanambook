package com.bookmind.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CheckoutRequest {
    @NotBlank(message = "Họ tên người nhận không được để trống")
    private String hoTenNguoiNhan;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String sdtNguoiNhan;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    private String diaChiNhan;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán")
    private Long maPTTT;

    private String maGiamGiaCode;

    private String ghiChu;

    /** Mã sách được tích chọn trong giỏ hàng. Rỗng = thanh toán toàn bộ giỏ. */
    private List<Long> selectedItems = new ArrayList<>();

    public String getHoTenNguoiNhan() { return hoTenNguoiNhan; }
    public void setHoTenNguoiNhan(String hoTenNguoiNhan) { this.hoTenNguoiNhan = hoTenNguoiNhan; }
    public String getSdtNguoiNhan() { return sdtNguoiNhan; }
    public void setSdtNguoiNhan(String sdtNguoiNhan) { this.sdtNguoiNhan = sdtNguoiNhan; }
    public String getDiaChiNhan() { return diaChiNhan; }
    public void setDiaChiNhan(String diaChiNhan) { this.diaChiNhan = diaChiNhan; }
    public Long getMaPTTT() { return maPTTT; }
    public void setMaPTTT(Long maPTTT) { this.maPTTT = maPTTT; }
    public String getMaGiamGiaCode() { return maGiamGiaCode; }
    public void setMaGiamGiaCode(String maGiamGiaCode) { this.maGiamGiaCode = maGiamGiaCode; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public List<Long> getSelectedItems() { return selectedItems; }
    public void setSelectedItems(List<Long> selectedItems) { this.selectedItems = selectedItems; }
}
