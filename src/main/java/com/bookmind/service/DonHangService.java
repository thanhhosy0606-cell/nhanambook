package com.bookmind.service;

import com.bookmind.dto.CheckoutRequest;
import com.bookmind.entity.*;
import com.bookmind.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DonHangService {

    private static final Logger log = LoggerFactory.getLogger(DonHangService.class);

    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("300000");
    public static final BigDecimal FEE_METRO = new BigDecimal("15000");      // Nội thành Hà Nội, TP.HCM
    public static final BigDecimal FEE_STANDARD = new BigDecimal("25000");   // Đà Nẵng, Hải Phòng, Cần Thơ & tỉnh lân cận
    public static final BigDecimal FEE_REMOTE = new BigDecimal("35000");     // Tuyến tỉnh xa

    private final DonHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final GioHangService gioHangService;
    private final SachRepository sachRepository;
    private final DiaChiRepository diaChiRepository;
    private final MaGiamGiaRepository maGiamGiaRepository;
    private final MaGiamGiaService maGiamGiaService;
    private final ThanhToanService thanhToanService;
    private final ThanhToanRepository thanhToanRepository;

    @Value("${order.unpaid.timeout-minutes:15}")
    private int unpaidTimeoutMinutes = 15;

    private volatile LocalDateTime lastAutoCancelCheckTime;
    private volatile int lastAutoCancelCount = 0;

    public int getUnpaidTimeoutMinutes() {
        return unpaidTimeoutMinutes;
    }

    public LocalDateTime getLastAutoCancelCheckTime() {
        return lastAutoCancelCheckTime;
    }

    public int getLastAutoCancelCount() {
        return lastAutoCancelCount;
    }

    public long countPendingUnpaidOrders() {
        return donHangRepository.countPendingOnlineUnpaidOrders();
    }

    public long countExpiredUnpaidOrders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fallbackCutoff = now.minusMinutes(unpaidTimeoutMinutes);
        return donHangRepository.countExpiredOnlineUnpaidOrders(now, fallbackCutoff);
    }

    @Transactional
    public void updatePaymentExpiration(Long maDH, boolean isOnline) {
        DonHang dh = getOrderById(maDH);
        dh.setThoiGianHetHanTT(isOnline ? LocalDateTime.now().plusMinutes(unpaidTimeoutMinutes) : null);
        donHangRepository.save(dh);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DonHangService(DonHangRepository donHangRepository,
                          ChiTietDonHangRepository chiTietDonHangRepository,
                          GioHangService gioHangService,
                          SachRepository sachRepository,
                          DiaChiRepository diaChiRepository,
                          MaGiamGiaRepository maGiamGiaRepository,
                          MaGiamGiaService maGiamGiaService,
                          ThanhToanService thanhToanService,
                          ThanhToanRepository thanhToanRepository) {
        this.donHangRepository = donHangRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.gioHangService = gioHangService;
        this.sachRepository = sachRepository;
        this.diaChiRepository = diaChiRepository;
        this.maGiamGiaRepository = maGiamGiaRepository;
        this.maGiamGiaService = maGiamGiaService;
        this.thanhToanService = thanhToanService;
        this.thanhToanRepository = thanhToanRepository;
    }

    /**
     * Tính phí vận chuyển động theo khu vực địa chỉ nhận hàng và theo giá trị đơn hàng:
     * - Miễn phí (0đ) nếu: dùng mã FREESHIP (hoặc mã miễn phí vận chuyển) HOẶC đơn hàng có tạm tính >= 300.000đ.
     * - Nội thành Hà Nội / TP.HCM: 15.000đ
     * - Đà Nẵng, Hải Phòng, Cần Thơ & các tỉnh lân cận: 25.000đ
     * - Các tỉnh/thành phố khác (tuyến xa): 35.000đ
     */
    public BigDecimal calculateShippingFee(BigDecimal subtotal, String diaChiNhan, MaGiamGia coupon) {
        if (coupon != null) {
            String code = coupon.getMaCode() != null ? coupon.getMaCode().trim().toUpperCase() : "";
            String name = coupon.getTenMa() != null ? coupon.getTenMa().toLowerCase() : "";
            if ("FREESHIP".equals(code) || code.contains("SHIP") || name.contains("vận chuyển") || name.contains("freeship")) {
                return BigDecimal.ZERO;
            }
        }
        if (subtotal != null && subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }
        if (diaChiNhan == null || diaChiNhan.trim().isEmpty()) {
            return FEE_STANDARD;
        }
        String norm = normalizeVietnamese(diaChiNhan);

        // Khu vực 1: Nội thành Hà Nội & TP. Hồ Chí Minh -> 15.000đ
        if (norm.contains("ha noi") || norm.contains("hanoi") || norm.matches(".*\\bhn\\b.*")
                || norm.contains("ho chi minh") || norm.contains("tp.hcm") || norm.contains("tphcm")
                || norm.contains("tp hcm") || norm.contains("sai gon") || norm.contains("saigon")
                || norm.matches(".*\\bhcm\\b.*")) {
            return FEE_METRO;
        }

        // Khu vực 2: Thành phố trực thuộc TW & các tỉnh lân cận -> 25.000đ
        String[] standardRegions = {
                "da nang", "hai phong", "can tho", "binh duong", "dong nai", "long an",
                "ba ria", "vung tau", "bac ninh", "hung yen", "hai duong", "vinh phuc",
                "ha nam", "nam dinh", "thai binh", "quang ninh", "thua thien hue", "hue",
                "khanh hoa", "nha trang", "nghe an", "thanh hoa", "bac giang", "thai nguyen",
                "phu tho", "ninh binh", "tien giang", "ben tre", "vinh long"
        };
        for (String region : standardRegions) {
            if (norm.contains(region)) {
                return FEE_STANDARD;
            }
        }

        // Khu vực 3: Các tỉnh thành khác (tuyến xa) -> 35.000đ
        return FEE_REMOTE;
    }

    private String normalizeVietnamese(String str) {
        if (str == null) return "";
        String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
        return temp.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase().trim();
    }

    @Transactional
    public DonHang createOrder(KhachHang khachHang, CheckoutRequest request) {
        List<ChiTietGioHang> cartItems = gioHangService.getSelectedItems(khachHang, request.getSelectedItems());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất một cuốn sách để đặt hàng!");
        }

        // 1. Kiểm tra trạng thái bán & tồn kho
        for (ChiTietGioHang item : cartItems) {
            Sach s = item.getSach();
            if ("NGUNG_BAN".equalsIgnoreCase(s.getTrangThai())) {
                throw new RuntimeException("Sách '" + s.getTenSach() + "' đã ngừng bán, vui lòng xóa khỏi giỏ hàng trước khi đặt hàng!");
            }
            if (s.getSoLuong() < item.getSoLuong()) {
                throw new RuntimeException("Sách '" + s.getTenSach() + "' không đủ số lượng trong kho (" + s.getSoLuong() + " cuốn còn lại)");
            }
        }

        // 2. Lưu / Cập nhật địa chỉ nhận hàng
        DiaChi diaChi = diaChiRepository.findByKhachHang_MaKH(khachHang.getMaKH()).stream()
                .filter(d -> d.getDiaChiNhan().equalsIgnoreCase(request.getDiaChiNhan().trim()))
                .findFirst()
                .orElseGet(() -> {
                    DiaChi d = new DiaChi();
                    d.setKhachHang(khachHang);
                    d.setHoTenNguoiNhan(request.getHoTenNguoiNhan().trim());
                    d.setSdtNguoiNhan(request.getSdtNguoiNhan().trim());
                    d.setDiaChiNhan(request.getDiaChiNhan().trim());
                    d.setMacDinh(false);
                    return diaChiRepository.save(d);
                });

        // 3. Tính tiền & Phí vận chuyển theo khu vực / giá trị đơn
        BigDecimal giaTamTinh = gioHangService.calculateSubtotal(cartItems);
        BigDecimal giamGia = BigDecimal.ZERO;
        MaGiamGia coupon = null;

        if (request.getMaGiamGiaCode() != null && !request.getMaGiamGiaCode().trim().isEmpty()) {
            Optional<MaGiamGia> couponOpt = maGiamGiaService.findByCode(request.getMaGiamGiaCode());
            if (couponOpt.isPresent()) {
                coupon = couponOpt.get();
                giamGia = maGiamGiaService.calculateDiscount(coupon, cartItems, giaTamTinh);
                // Trừ số lượng mã giảm giá nguyên tử (Atomic Update chống Race Condition)
                int updatedCoupon = maGiamGiaRepository.deductCouponQuantityAtomic(coupon.getMaMaGiamGia());
                if (updatedCoupon == 0) {
                    throw new RuntimeException("Mã giảm giá '" + coupon.getMaCode() + "' vừa hết lượt sử dụng!");
                }
            }
        }

        BigDecimal phiVC = calculateShippingFee(giaTamTinh, request.getDiaChiNhan(), coupon);

        BigDecimal tongTien = giaTamTinh.subtract(giamGia).add(phiVC);
        if (tongTien.compareTo(BigDecimal.ZERO) < 0) {
            tongTien = BigDecimal.ZERO;
        }

        // 4. Tạo Đơn hàng (Tiền mặt COD tự động xác nhận thành công ngay, chỉ MoMo/VietQR mới chờ xác minh)
        PtthThanhToan pttt = thanhToanService.getMethodById(request.getMaPTTT());
        boolean isCod = pttt != null && thanhToanService.isCodMethod(pttt.getTenPTTT());
        String initialOrderStatus = isCod ? "DA_XAC_NHAN" : "CHO_XAC_NHAN";

        DonHang donHang = new DonHang();
        donHang.setKhachHang(khachHang);
        donHang.setMaGiamGia(coupon);
        donHang.setGiaTamTinh(giaTamTinh);
        donHang.setGiamGia(giamGia);
        donHang.setPhiVC(phiVC);
        donHang.setTongTien(tongTien);
        donHang.setTrangThai(initialOrderStatus);
        donHang.setGhiChu(DonHang.encodeReceiverSnapshot(
                request.getHoTenNguoiNhan(),
                request.getSdtNguoiNhan(),
                request.getDiaChiNhan(),
                request.getGhiChu()));
        if (!isCod) {
            donHang.setThoiGianHetHanTT(LocalDateTime.now().plusMinutes(unpaidTimeoutMinutes));
        }
        donHang = donHangRepository.save(donHang);

        // 5. Lưu Chi tiết đơn hàng và trừ tồn kho nguyên tử (Atomic Update chống Race Condition)
        for (ChiTietGioHang item : cartItems) {
            Sach s = item.getSach();
            int updatedStock = sachRepository.deductStockAtomic(s.getMaSach(), item.getSoLuong());
            if (updatedStock == 0) {
                throw new RuntimeException("Sách '" + s.getTenSach() + "' vừa hết hàng hoặc không đủ số lượng tồn kho!");
            }

            ChiTietDonHang ctdh = new ChiTietDonHang();
            ctdh.setId(new ChiTietDonHangId(donHang.getMaDH(), s.getMaSach()));
            ctdh.setDonHang(donHang);
            ctdh.setSach(s);
            ctdh.setSoLuong(item.getSoLuong());
            ctdh.setDonGia(item.getDonGia());
            chiTietDonHangRepository.save(ctdh);
        }

        // 6. Tạo bản ghi thanh toán
        thanhToanService.createPayment(donHang, pttt, tongTien);

        // 7. Chỉ xóa khỏi giỏ những sách đã đặt, giữ lại sách chưa chọn
        gioHangService.removeItems(khachHang, cartItems);

        return donHang;
    }

    public Page<DonHang> getCustomerOrders(KhachHang khachHang, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return donHangRepository.findByKhachHang_MaKHOrderByNgayDatDesc(khachHang.getMaKH(), pageable);
    }

    public DonHang getOrderById(Long maDH) {
        return donHangRepository.findById(maDH)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + maDH));
    }

    public Page<DonHang> getAllOrdersAdmin(String keyword, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "maDH"));

        Specification<DonHang> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
                predicates.add(cb.equal(root.get("trangThai"), status.trim()));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                String cleanNum = keyword.trim().replace("#", "").trim();

                Join<DonHang, KhachHang> khachHangJoin = root.join("khachHang", JoinType.LEFT);
                Join<KhachHang, TaiKhoan> taiKhoanJoin = khachHangJoin.join("taiKhoan", JoinType.LEFT);

                List<Predicate> orPreds = new ArrayList<>();
                orPreds.add(cb.like(cb.lower(khachHangJoin.get("hoTen")), kw));
                orPreds.add(cb.like(cb.lower(cb.coalesce(khachHangJoin.get("sdt"), "")), kw));
                orPreds.add(cb.like(cb.lower(taiKhoanJoin.get("email")), kw));
                orPreds.add(cb.like(cb.lower(cb.coalesce(taiKhoanJoin.get("tenDN"), "")), kw));
                orPreds.add(cb.like(cb.lower(cb.coalesce(root.get("ghiChu"), "")), kw));

                if (!cleanNum.isEmpty()) {
                    orPreds.add(cb.like(root.get("maDH").as(String.class), "%" + cleanNum + "%"));
                }

                predicates.add(cb.or(orPreds.toArray(Predicate[]::new)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        return donHangRepository.findAll(spec, pageable);
    }

    public Page<DonHang> getAllOrdersAdmin(String status, int page, int size) {
        return getAllOrdersAdmin(null, status, page, size);
    }

    @Transactional
    public void updateOrderStatus(Long maDH, String status) {
        DonHang dh = getOrderById(maDH);
        String oldStatus = dh.getTrangThai();
        if (oldStatus != null && oldStatus.equalsIgnoreCase(status)) {
            return;
        }
        dh.setTrangThai(status);

        if ("DA_HUY".equalsIgnoreCase(status) && !"DA_HUY".equalsIgnoreCase(oldStatus)) {
            // 1. Hoàn lại số lượng tồn kho cho từng đầu sách trong đơn
            List<ChiTietDonHang> items = chiTietDonHangRepository.findByDonHang_MaDH(maDH);
            for (ChiTietDonHang item : items) {
                Sach s = item.getSach();
                if (s != null) {
                    int currentStock = s.getSoLuong() != null ? s.getSoLuong() : 0;
                    s.setSoLuong(currentStock + item.getSoLuong());
                    if ("HET_HANG".equalsIgnoreCase(s.getTrangThai()) && s.getSoLuong() > 0) {
                        s.setTrangThai("DANG_BAN");
                    }
                    sachRepository.save(s);
                }
            }

            // 2. Hoàn lại số lượng mã giảm giá nếu đơn hàng có áp dụng mã
            MaGiamGia coupon = dh.getMaGiamGia();
            if (coupon != null) {
                int currentCouponQty = coupon.getSoLuong() != null ? coupon.getSoLuong() : 0;
                coupon.setSoLuong(currentCouponQty + 1);
                maGiamGiaRepository.save(coupon);
            }

            // 3. Chuyển các giao dịch đang chờ thanh toán sang THAT_BAI
            List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDH(maDH);
            for (ThanhToan tt : payments) {
                if ("CHO_THANH_TOAN".equalsIgnoreCase(tt.getTrangThai())) {
                    tt.setTrangThai("THAT_BAI");
                    thanhToanRepository.save(tt);
                }
            }
        } else if ("DA_GIAO".equalsIgnoreCase(status)) {
            // Khi đơn hàng đã giao thành công thì thanh toán hoàn tất
            List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDH(maDH);
            for (ThanhToan tt : payments) {
                if (!"THANH_CONG".equalsIgnoreCase(tt.getTrangThai())) {
                    tt.setTrangThai("THANH_CONG");
                    if (tt.getThoiGianTT() == null) {
                        tt.setThoiGianTT(LocalDateTime.now());
                    }
                    thanhToanRepository.save(tt);
                }
            }
        }
        donHangRepository.save(dh);
    }

    /**
     * Scheduler tự động hủy các đơn hàng thanh toán trực tuyến (CHO_XAC_NHAN) chưa thanh toán
     * sau N phút (mặc định 15 phút), đồng thời hoàn trả tồn kho sách và hoàn số lượng mã giảm giá.
     */
    @Scheduled(fixedRateString = "${order.unpaid.check-interval-ms:60000}")
    @Transactional
    public int autoCancelExpiredUnpaidOrders() {
        LocalDateTime now = LocalDateTime.now();
        this.lastAutoCancelCheckTime = now;
        LocalDateTime fallbackCutoff = now.minusMinutes(unpaidTimeoutMinutes);
        List<DonHang> expiredOrders = donHangRepository.findExpiredOnlineUnpaidOrders(now, fallbackCutoff);
        int canceledCount = 0;

        for (DonHang dh : expiredOrders) {
            String note = dh.getGhiChu() != null && !dh.getGhiChu().isBlank() ? dh.getGhiChu() + "\n" : "";
            dh.setGhiChu(note + "[Tự động hủy do quá hạn thanh toán " + unpaidTimeoutMinutes + " phút]");
            updateOrderStatus(dh.getMaDH(), "DA_HUY");
            canceledCount++;
            log.info("Auto-cancelled expired unpaid order #{} (expired at {}), restored stock & coupon quantity.",
                    dh.getMaDH(), dh.getThoiGianHetHanTT() != null ? dh.getThoiGianHetHanTT() : dh.getNgayDat());
        }
        this.lastAutoCancelCount = canceledCount;
        return canceledCount;
    }
}
