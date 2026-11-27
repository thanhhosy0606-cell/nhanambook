package com.bookmind.controller;

import com.bookmind.dto.LoaiSachFormDto;
import com.bookmind.dto.MaGiamGiaFormDto;
import com.bookmind.dto.RevenueReportDto;
import com.bookmind.dto.SachFormDto;
import com.bookmind.entity.*;
import com.bookmind.repository.*;
import com.bookmind.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final SachService sachService;
    private final LoaiSachService loaiSachService;
    private final DonHangService donHangService;
    private final KhachHangService khachHangService;
    private final MaGiamGiaService maGiamGiaService;
    private final SachRepository sachRepository;
    private final KhachHangRepository khachHangRepository;
    private final DonHangRepository donHangRepository;
    private final RevenueService revenueService;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminController(SachService sachService,
                           LoaiSachService loaiSachService,
                           DonHangService donHangService,
                           KhachHangService khachHangService,
                           MaGiamGiaService maGiamGiaService,
                           SachRepository sachRepository,
                           KhachHangRepository khachHangRepository,
                           DonHangRepository donHangRepository,
                           RevenueService revenueService) {
        this.sachService = sachService;
        this.loaiSachService = loaiSachService;
        this.donHangService = donHangService;
        this.khachHangService = khachHangService;
        this.maGiamGiaService = maGiamGiaService;
        this.sachRepository = sachRepository;
        this.khachHangRepository = khachHangRepository;
        this.donHangRepository = donHangRepository;
        this.revenueService = revenueService;
    }

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        long totalBooks = sachRepository.count();
        long totalCustomers = khachHangRepository.count();
        long totalOrders = donHangRepository.count();

        // Tính tổng doanh thu đơn đã giao trực tiếp bằng SQL SUM (không tải toàn bộ DONHANG lên RAM)
        BigDecimal totalRevenue = donHangRepository.sumCompletedRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        Page<DonHang> recentOrders = donHangService.getAllOrdersAdmin("ALL", 0, 5);
        RevenueReportDto revenueReport = revenueService.getRevenueByDays(7);

        // 1. Thống kê Top 8 khách hàng trực tiếp bằng GROUP BY trên MySQL
        List<Object[]> topCustomerRows = donHangRepository.findTopCustomerStats(org.springframework.data.domain.PageRequest.of(0, 8));
        List<java.util.Map<String, Object>> customerStatsList = new java.util.ArrayList<>();
        for (Object[] row : topCustomerRows) {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("maKH", row[0]);
            m.put("hoTen", row[1]);
            m.put("sdt", row[2] != null ? row[2] : "Chưa cập nhật");
            m.put("email", row[3] != null ? row[3] : "");
            m.put("totalOrders", ((Number) row[4]).longValue());
            m.put("completedOrders", row[5] != null ? ((Number) row[5]).longValue() : 0L);
            m.put("canceledOrders", row[6] != null ? ((Number) row[6]).longValue() : 0L);
            m.put("totalSpent", row[7] instanceof BigDecimal bd ? bd : BigDecimal.ZERO);
            customerStatsList.add(m);
        }

        // 2. Thống kê hiệu quả mã giảm giá trực tiếp bằng GROUP BY trên MySQL
        List<MaGiamGia> allCoupons = maGiamGiaService.getAllForAdmin();
        java.util.Map<Long, java.util.Map<String, Object>> couponStatsMap = new java.util.LinkedHashMap<>();
        for (MaGiamGia mg : allCoupons) {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("maMaGiamGia", mg.getMaMaGiamGia());
            m.put("maCode", mg.getMaCode());
            m.put("tenMa", mg.getTenMa());
            m.put("trangThai", mg.getTrangThai());
            m.put("soLuongConLai", mg.getSoLuong() != null ? mg.getSoLuong() : 0);
            m.put("usedOrderCount", 0L);
            m.put("totalDiscountAmount", BigDecimal.ZERO);
            m.put("totalOrderRevenue", BigDecimal.ZERO);
            couponStatsMap.put(mg.getMaMaGiamGia(), m);
        }

        long totalCouponOrders = 0L;
        BigDecimal totalCouponDiscountGiven = BigDecimal.ZERO;
        BigDecimal totalCouponOrderRevenue = BigDecimal.ZERO;

        List<Object[]> couponUsageRows = donHangRepository.findCouponUsageStats();
        for (Object[] row : couponUsageRows) {
            Long couponId = row[0] != null ? ((Number) row[0]).longValue() : null;
            long used = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            BigDecimal disc = row[2] instanceof BigDecimal bd ? bd : BigDecimal.ZERO;
            BigDecimal rev = row[3] instanceof BigDecimal bd ? bd : BigDecimal.ZERO;

            totalCouponOrders += used;
            totalCouponDiscountGiven = totalCouponDiscountGiven.add(disc);
            totalCouponOrderRevenue = totalCouponOrderRevenue.add(rev);

            if (couponId != null && couponStatsMap.containsKey(couponId)) {
                java.util.Map<String, Object> stat = couponStatsMap.get(couponId);
                stat.put("usedOrderCount", used);
                stat.put("totalDiscountAmount", disc);
                stat.put("totalOrderRevenue", rev);
            }
        }

        List<java.util.Map<String, Object>> couponEffectivenessList = couponStatsMap.values().stream()
                .peek(m -> {
                    long used = (Long) m.get("usedOrderCount");
                    int remaining = (Integer) m.get("soLuongConLai");
                    long totalIssued = used + remaining;
                    double usageRate = totalIssued > 0 ? Math.round((used * 1000.0) / totalIssued) / 10.0 : 0.0;
                    m.put("usageRate", usageRate);
                })
                .sorted((a, b) -> {
                    int cmpUsed = Long.compare((Long) b.get("usedOrderCount"), (Long) a.get("usedOrderCount"));
                    if (cmpUsed != 0) return cmpUsed;
                    return ((BigDecimal) b.get("totalOrderRevenue")).compareTo((BigDecimal) a.get("totalOrderRevenue"));
                })
                .toList();

        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("recentOrders", recentOrders.getContent());
        model.addAttribute("revenueReport", revenueReport);
        model.addAttribute("customerStatsList", customerStatsList);
        model.addAttribute("couponEffectivenessList", couponEffectivenessList);
        model.addAttribute("totalCouponOrders", totalCouponOrders);
        model.addAttribute("totalCouponDiscountGiven", totalCouponDiscountGiven);
        model.addAttribute("totalCouponOrderRevenue", totalCouponOrderRevenue);
        model.addAttribute("activeNav", "dashboard");
        return "admin/dashboard";
    }

    // 0. Báo cáo & Thống kê doanh thu theo Ngày, Tháng, Năm
    @GetMapping("/revenue")
    public String revenueAnalytics(
            @RequestParam(value = "period", defaultValue = "day") String period,
            @RequestParam(value = "days", defaultValue = "7") int days,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "year", required = false) Integer year,
            Model model) {
        RevenueReportDto report = resolveRevenueReport(period, days, startDate, endDate, year);
        model.addAttribute("report", report);
        model.addAttribute("currentPeriod", period);
        model.addAttribute("currentDays", days);
        model.addAttribute("currentStartDate", startDate);
        model.addAttribute("currentEndDate", endDate);
        model.addAttribute("currentYear", year != null ? year : LocalDate.now().getYear());
        model.addAttribute("activeNav", "revenue");
        return "admin/revenue";
    }

    @GetMapping("/api/revenue")
    @ResponseBody
    public ResponseEntity<RevenueReportDto> getRevenueApi(
            @RequestParam(value = "period", defaultValue = "day") String period,
            @RequestParam(value = "days", defaultValue = "7") int days,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "year", required = false) Integer year) {
        RevenueReportDto report = resolveRevenueReport(period, days, startDate, endDate, year);
        return ResponseEntity.ok(report);
    }

    private RevenueReportDto resolveRevenueReport(String period, int days, String startDate, String endDate, Integer year) {
        if ("month".equalsIgnoreCase(period)) {
            int y = year != null ? year : LocalDate.now().getYear();
            return revenueService.getRevenueByMonths(y);
        } else if ("year".equalsIgnoreCase(period)) {
            int currentYear = LocalDate.now().getYear();
            return revenueService.getRevenueByYears(currentYear - 4, currentYear);
        } else {
            if (startDate != null && !startDate.isBlank() && endDate != null && !endDate.isBlank()) {
                try {
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    return revenueService.getRevenueByDateRange(start, end);
                } catch (Exception ignored) {
                }
            }
            return revenueService.getRevenueByDays(days > 0 ? days : 7);
        }
    }

    // 1. Quản lý sách
    @GetMapping("/books")
    public String manageBooks(@RequestParam(value = "keyword", required = false) String keyword,
                              @RequestParam(value = "maLoai", required = false) Long maLoai,
                              @RequestParam(value = "trangThai", defaultValue = "ALL") String trangThai,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "size", defaultValue = "10") int size,
                              Model model) {
        Page<Sach> bookPage = sachService.getBooksForAdmin(keyword, maLoai, trangThai, page, size);
        List<LoaiSach> categories = loaiSachService.getAllForAdmin();

        model.addAttribute("bookPage", bookPage);
        model.addAttribute("categories", categories);
        model.addAttribute("sachForm", new SachFormDto());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedLoai", maLoai);
        model.addAttribute("selectedTrangThai", trangThai);
        model.addAttribute("activeNav", "books");
        return "admin/books";
    }

    @PostMapping("/books/save")
    public String saveBook(@Valid @ModelAttribute("sachForm") SachFormDto form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu nhập sách chưa hợp lệ! Vui lòng kiểm tra lại các trường bắt buộc.");
            return "redirect:/admin/books";
        }
        try {
            boolean isEdit = form.getMaSach() != null;
            sachService.save(form);
            redirectAttributes.addFlashAttribute("successMessage", 
                    isEdit ? "Cập nhật thông tin sách thành công!" : "Thêm sách mới vào kho thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/books";
    }

    @PostMapping("/books/{id}/delete")
    public String deleteBook(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            String msg = sachService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", msg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/books";
    }

    @PostMapping("/books/{id}/toggle")
    public String toggleBookStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            sachService.toggleStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái sách!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/books";
    }

    // 2. Quản lý loại sách
    @GetMapping("/categories")
    public String manageCategories(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", defaultValue = "ALL") String status,
            Model model) {
        List<LoaiSach> categories = loaiSachService.searchForAdmin(keyword, status);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryForm", new LoaiSachFormDto());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activeNav", "categories");
        return "admin/categories";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@Valid @ModelAttribute("categoryForm") LoaiSachFormDto form,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên loại sách không được để trống!");
            return "redirect:/admin/categories";
        }
        try {
            boolean isEdit = form.getMaLoai() != null;
            loaiSachService.save(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    isEdit ? "Cập nhật loại sách thành công!" : "Thêm loại sách mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            loaiSachService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa loại sách thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/toggle")
    public String toggleCategoryStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            loaiSachService.toggleStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái loại sách!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // 3. Quản lý đơn hàng
    @GetMapping("/orders")
    public String manageOrders(@RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "status", defaultValue = "ALL") String status,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               Model model) {
        Page<DonHang> orderPage = donHangService.getAllOrdersAdmin(keyword, status, page, size);
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("unpaidTimeoutMinutes", donHangService.getUnpaidTimeoutMinutes());
        model.addAttribute("pendingUnpaidCount", donHangService.countPendingUnpaidOrders());
        model.addAttribute("expiredUnpaidCount", donHangService.countExpiredUnpaidOrders());
        model.addAttribute("lastAutoCancelCheckTime", donHangService.getLastAutoCancelCheckTime());
        model.addAttribute("lastAutoCancelCount", donHangService.getLastAutoCancelCount());
        model.addAttribute("activeNav", "orders");
        return "admin/orders";
    }

    @PostMapping("/orders/auto-cancel-now")
    public String triggerAutoCancelExpiredOrders(RedirectAttributes redirectAttributes) {
        int canceled = donHangService.autoCancelExpiredUnpaidOrders();
        if (canceled > 0) {
            redirectAttributes.addFlashAttribute("autoCancelResult",
                    "Đã tự động hủy " + canceled + " đơn hàng quá hạn thanh toán, hoàn lại tồn kho và số lượng mã giảm giá!");
        } else {
            redirectAttributes.addFlashAttribute("autoCancelResult",
                    "Đã quét xong: Hiện không có đơn hàng online nào quá hạn thanh toán (" + donHangService.getUnpaidTimeoutMinutes() + " phút).");
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable("id") Long id,
                                    @RequestParam("status") String status,
                                    RedirectAttributes redirectAttributes) {
        try {
            donHangService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng #" + id + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    // 4. Quản lý khách hàng
    @GetMapping("/customers")
    public String manageCustomers(@RequestParam(value = "keyword", required = false) String keyword,
                                  @RequestParam(value = "ptDangNhap", defaultValue = "ALL") String ptDangNhap,
                                  @RequestParam(value = "vaiTro", defaultValue = "ALL") String vaiTro,
                                  @RequestParam(value = "trangThai", defaultValue = "ALL") String trangThai,
                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                  @RequestParam(value = "size", defaultValue = "10") int size,
                                  Model model) {
        Page<KhachHang> customerPage = khachHangService.getAllForAdmin(keyword, ptDangNhap, vaiTro, trangThai, page, size);
        model.addAttribute("customerPage", customerPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedPtDangNhap", ptDangNhap);
        model.addAttribute("selectedVaiTro", vaiTro);
        model.addAttribute("selectedTrangThai", trangThai);
        model.addAttribute("activeNav", "customers");
        return "admin/customers";
    }

    @PostMapping("/customers/{id}/toggle-lock")
    public String toggleCustomerLock(@PathVariable("id") Long id,
                                     @RequestParam("maTK") Long maTK,
                                     RedirectAttributes redirectAttributes) {
        try {
            khachHangService.toggleLockAccount(maTK);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái tài khoản khách hàng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    @PostMapping("/customers/{id}/delete")
    public String deleteCustomer(@PathVariable("id") Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            khachHangService.deleteCustomer(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa khách hàng #" + id + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    // 5. Quản lý mã giảm giá
    @GetMapping("/coupons")
    public String manageCoupons(@RequestParam(value = "keyword", required = false) String keyword,
                                @RequestParam(value = "scope", defaultValue = "ALL") String scope,
                                @RequestParam(value = "status", defaultValue = "ALL") String status,
                                Model model) {
        List<MaGiamGia> coupons = maGiamGiaService.getCouponsForAdmin(keyword, scope, status);

        model.addAttribute("coupons", coupons);
        model.addAttribute("couponForm", new MaGiamGiaFormDto());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activeNav", "coupons");
        return "admin/coupons";
    }

    @PostMapping("/coupons/save")
    public String saveCoupon(@Valid @ModelAttribute("couponForm") MaGiamGiaFormDto form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin mã giảm giá chưa hợp lệ: " +
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/coupons";
        }
        try {
            boolean isEdit = form.getMaMaGiamGia() != null;
            maGiamGiaService.save(form);
            redirectAttributes.addFlashAttribute("successMessage", 
                    isEdit ? "Cập nhật mã giảm giá thành công!" : "Tạo mã giảm giá mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @PostMapping("/coupons/{id}/delete")
    public String deleteCoupon(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            maGiamGiaService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa mã giảm giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @PostMapping("/coupons/{id}/toggle")
    public String toggleCouponStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            maGiamGiaService.toggleStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái mã giảm giá!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/coupons";
    }
}
