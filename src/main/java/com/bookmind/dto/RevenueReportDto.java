package com.bookmind.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RevenueReportDto {
    private String periodType;       // "DAY", "MONTH", "YEAR"
    private String periodTitle;      // e.g. "Doanh thu 7 ngày gần nhất", "Doanh thu năm 2026"
    private String startDate;        // dd/MM/yyyy
    private String endDate;          // dd/MM/yyyy
    private String filterInfo;       // e.g. "Từ 28/09/2026 đến 04/10/2026"

    private BigDecimal totalRevenue;       // Thực thu (DA_GIAO)
    private BigDecimal totalGrossRevenue;  // Tổng giá trị đặt hàng
    private long totalOrders;              // Tổng số đơn
    private long completedOrders;          // Đơn thành công
    private long canceledOrders;           // Đơn đã hủy
    private long pendingOrders;            // Đơn đang xử lý / đang giao
    private double successRate;            // % hoàn thành
    private BigDecimal averageOrderValue;  // Doanh thu trung bình / đơn thành công

    private List<RevenueDataPointDto> dataPoints = new ArrayList<>();
    private List<TopBookDto> topBooks = new ArrayList<>();

    public RevenueReportDto() {
    }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public String getPeriodTitle() {
        return periodTitle;
    }

    public void setPeriodTitle(String periodTitle) {
        this.periodTitle = periodTitle;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getFilterInfo() {
        return filterInfo;
    }

    public void setFilterInfo(String filterInfo) {
        this.filterInfo = filterInfo;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getTotalGrossRevenue() {
        return totalGrossRevenue;
    }

    public void setTotalGrossRevenue(BigDecimal totalGrossRevenue) {
        this.totalGrossRevenue = totalGrossRevenue;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(long completedOrders) {
        this.completedOrders = completedOrders;
    }

    public long getCanceledOrders() {
        return canceledOrders;
    }

    public void setCanceledOrders(long canceledOrders) {
        this.canceledOrders = canceledOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public BigDecimal getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(BigDecimal averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }

    public List<RevenueDataPointDto> getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(List<RevenueDataPointDto> dataPoints) {
        this.dataPoints = dataPoints;
    }

    public List<TopBookDto> getTopBooks() {
        return topBooks;
    }

    public void setTopBooks(List<TopBookDto> topBooks) {
        this.topBooks = topBooks;
    }
}
