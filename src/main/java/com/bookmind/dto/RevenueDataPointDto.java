package com.bookmind.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RevenueDataPointDto {
    private String label;          // e.g. "04/10", "Tháng 10/2026", "Năm 2026"
    private String fullDate;       // e.g. "2026-10-04" or "Tháng 10, 2026"
    private BigDecimal revenue;    // Doanh thu đơn thành công (DA_GIAO)
    private BigDecimal grossRevenue; // Tổng doanh số (trừ DA_HUY)
    private int orderCount;        // Tổng số đơn
    private int completedCount;    // Số đơn hoàn thành (DA_GIAO)
    private int canceledCount;     // Số đơn bị hủy (DA_HUY)
    private int pendingCount;      // Số đơn đang xử lý / đang giao

    public RevenueDataPointDto() {
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getFullDate() {
        return fullDate;
    }

    public void setFullDate(String fullDate) {
        this.fullDate = fullDate;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getGrossRevenue() {
        return grossRevenue;
    }

    public void setGrossRevenue(BigDecimal grossRevenue) {
        this.grossRevenue = grossRevenue;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getCanceledCount() {
        return canceledCount;
    }

    public void setCanceledCount(int canceledCount) {
        this.canceledCount = canceledCount;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }
}
