package com.bookmind.service;

import com.bookmind.dto.RevenueDataPointDto;
import com.bookmind.dto.RevenueReportDto;
import com.bookmind.dto.TopBookDto;
import com.bookmind.entity.ChiTietDonHang;
import com.bookmind.entity.DonHang;
import com.bookmind.entity.Sach;
import com.bookmind.repository.DonHangRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RevenueService {

    private final DonHangRepository donHangRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public RevenueService(DonHangRepository donHangRepository) {
        this.donHangRepository = donHangRepository;
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DAY_LABEL_FMT = DateTimeFormatter.ofPattern("dd/MM");

    /**
     * Báo cáo doanh thu theo N ngày gần nhất (mặc định 7 hoặc 30 ngày)
     */
    public RevenueReportDto getRevenueByDays(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        String title = "Báo cáo doanh thu " + days + " ngày gần nhất";
        return buildDailyReport(start, end, title);
    }

    /**
     * Báo cáo doanh thu theo khoảng ngày tùy chỉnh
     */
    public RevenueReportDto getRevenueByDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        String title = "Báo cáo doanh thu từ " + start.format(DATE_FMT) + " đến " + end.format(DATE_FMT);
        return buildDailyReport(start, end, title);
    }

    /**
     * Báo cáo doanh thu 12 tháng của năm chỉ định
     */
    public RevenueReportDto getRevenueByMonths(int year) {
        LocalDateTime startDt = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime endDt = LocalDateTime.of(year, 12, 31, 23, 59, 59);

        List<DonHang> orders = donHangRepository.findByNgayDatBetweenOrderByNgayDatAsc(startDt, endDt);

        List<RevenueDataPointDto> points = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            final int monthVal = m;
            List<DonHang> monthOrders = orders.stream()
                    .filter(o -> o.getNgayDat() != null && o.getNgayDat().getMonthValue() == monthVal)
                    .collect(Collectors.toList());

            points.add(createDataPoint("Thg " + monthVal, "Tháng " + monthVal + "/" + year, monthOrders));
        }

        return aggregateReport("MONTH", "Báo cáo doanh thu năm " + year + " (Theo 12 tháng)",
                "01/01/" + year, "31/12/" + year, "Cả năm " + year, points, orders);
    }

    /**
     * Báo cáo doanh thu theo từng năm (mặc định 5 năm gần nhất)
     */
    public RevenueReportDto getRevenueByYears(int startYear, int endYear) {
        if (startYear > endYear) {
            int tmp = startYear;
            startYear = endYear;
            endYear = tmp;
        }

        LocalDateTime startDt = LocalDateTime.of(startYear, 1, 1, 0, 0, 0);
        LocalDateTime endDt = LocalDateTime.of(endYear, 12, 31, 23, 59, 59);

        List<DonHang> orders = donHangRepository.findByNgayDatBetweenOrderByNgayDatAsc(startDt, endDt);

        List<RevenueDataPointDto> points = new ArrayList<>();
        for (int y = startYear; y <= endYear; y++) {
            final int currentYear = y;
            List<DonHang> yearOrders = orders.stream()
                    .filter(o -> o.getNgayDat() != null && o.getNgayDat().getYear() == currentYear)
                    .collect(Collectors.toList());

            points.add(createDataPoint(String.valueOf(currentYear), "Năm " + currentYear, yearOrders));
        }

        return aggregateReport("YEAR", "Báo cáo doanh thu các năm (" + startYear + " - " + endYear + ")",
                "01/01/" + startYear, "31/12/" + endYear, "Giai đoạn " + startYear + " - " + endYear, points, orders);
    }

    private RevenueReportDto buildDailyReport(LocalDate start, LocalDate end, String title) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);

        List<DonHang> orders = donHangRepository.findByNgayDatBetweenOrderByNgayDatAsc(startDt, endDt);

        List<RevenueDataPointDto> points = new ArrayList<>();
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            final LocalDate dateVal = cur;
            List<DonHang> dayOrders = orders.stream()
                    .filter(o -> o.getNgayDat() != null && o.getNgayDat().toLocalDate().isEqual(dateVal))
                    .collect(Collectors.toList());

            points.add(createDataPoint(cur.format(DAY_LABEL_FMT), cur.format(DATE_FMT), dayOrders));
            cur = cur.plusDays(1);
        }

        return aggregateReport("DAY", title, start.format(DATE_FMT), end.format(DATE_FMT),
                "Từ " + start.format(DATE_FMT) + " đến " + end.format(DATE_FMT), points, orders);
    }

    private RevenueDataPointDto createDataPoint(String label, String fullDate, List<DonHang> list) {
        BigDecimal completedRev = list.stream()
                .filter(o -> "DA_GIAO".equalsIgnoreCase(o.getTrangThai()))
                .map(DonHang::getTongTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossRev = list.stream()
                .filter(o -> !"DA_HUY".equalsIgnoreCase(o.getTrangThai()))
                .map(DonHang::getTongTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalCount = list.size();
        int completedCount = (int) list.stream().filter(o -> "DA_GIAO".equalsIgnoreCase(o.getTrangThai())).count();
        int canceledCount = (int) list.stream().filter(o -> "DA_HUY".equalsIgnoreCase(o.getTrangThai())).count();
        int pendingCount = totalCount - completedCount - canceledCount;

        RevenueDataPointDto pt = new RevenueDataPointDto();
        pt.setLabel(label);
        pt.setFullDate(fullDate);
        pt.setRevenue(completedRev);
        pt.setGrossRevenue(grossRev);
        pt.setOrderCount(totalCount);
        pt.setCompletedCount(completedCount);
        pt.setCanceledCount(canceledCount);
        pt.setPendingCount(pendingCount);
        return pt;
    }

    private RevenueReportDto aggregateReport(String periodType, String title, String startStr, String endStr,
                                             String filterInfo, List<RevenueDataPointDto> points, List<DonHang> allOrders) {
        BigDecimal totalRevenue = points.stream()
                .map(RevenueDataPointDto::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGross = points.stream()
                .map(RevenueDataPointDto::getGrossRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = points.stream().mapToLong(RevenueDataPointDto::getOrderCount).sum();
        long completedOrders = points.stream().mapToLong(RevenueDataPointDto::getCompletedCount).sum();
        long canceledOrders = points.stream().mapToLong(RevenueDataPointDto::getCanceledCount).sum();
        long pendingOrders = points.stream().mapToLong(RevenueDataPointDto::getPendingCount).sum();

        double successRate = totalOrders > 0 ? (double) completedOrders * 100.0 / totalOrders : 0.0;
        BigDecimal aov = completedOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(completedOrders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<TopBookDto> topBooks = extractTopBooks(allOrders);

        RevenueReportDto report = new RevenueReportDto();
        report.setPeriodType(periodType);
        report.setPeriodTitle(title);
        report.setStartDate(startStr);
        report.setEndDate(endStr);
        report.setFilterInfo(filterInfo);
        report.setTotalRevenue(totalRevenue);
        report.setTotalGrossRevenue(totalGross);
        report.setTotalOrders(totalOrders);
        report.setCompletedOrders(completedOrders);
        report.setCanceledOrders(canceledOrders);
        report.setPendingOrders(pendingOrders);
        report.setSuccessRate(Math.round(successRate * 10.0) / 10.0);
        report.setAverageOrderValue(aov);
        report.setDataPoints(points);
        report.setTopBooks(topBooks);
        return report;
    }

    private List<TopBookDto> extractTopBooks(List<DonHang> orders) {
        Map<Long, TopBookDto> map = new HashMap<>();

        for (DonHang d : orders) {
            if ("DA_HUY".equalsIgnoreCase(d.getTrangThai())) {
                continue;
            }
            if (d.getChiTietList() != null) {
                for (ChiTietDonHang c : d.getChiTietList()) {
                    Sach s = c.getSach();
                    if (s != null) {
                        Long bookId = s.getMaSach();
                        TopBookDto dto = map.computeIfAbsent(bookId, k -> {
                            TopBookDto tb = new TopBookDto();
                            tb.setMaSach(bookId);
                            tb.setTenSach(s.getTenSach());
                            tb.setHinhAnh(s.getHinhAnh());
                            tb.setTenLoai(s.getLoaiSach() != null ? s.getLoaiSach().getTenLoai() : "Khác");
                            tb.setSoLuongDaBan(0);
                            tb.setTongDoanhThu(BigDecimal.ZERO);
                            return tb;
                        });

                        dto.setSoLuongDaBan(dto.getSoLuongDaBan() + c.getSoLuong());
                        dto.setTongDoanhThu(dto.getTongDoanhThu().add(c.getThanhTien()));
                    }
                }
            }
        }

        return map.values().stream()
                .sorted((a, b) -> Long.compare(b.getSoLuongDaBan(), a.getSoLuongDaBan()))
                .limit(5)
                .collect(Collectors.toList());
    }
}
