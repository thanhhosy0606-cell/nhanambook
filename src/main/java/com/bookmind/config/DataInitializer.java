package com.bookmind.config;

import com.bookmind.entity.*;
import com.bookmind.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final LoaiSachRepository loaiSachRepository;
    private final SachRepository sachRepository;
    private final MaGiamGiaRepository maGiamGiaRepository;
    private final PtthThanhToanRepository ptthThanhToanRepository;
    private final GioHangRepository gioHangRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Autowired
    public DataInitializer(TaiKhoanRepository taiKhoanRepository,
                           KhachHangRepository khachHangRepository,
                           LoaiSachRepository loaiSachRepository,
                           SachRepository sachRepository,
                           MaGiamGiaRepository maGiamGiaRepository,
                           PtthThanhToanRepository ptthThanhToanRepository,
                           GioHangRepository gioHangRepository,
                           PasswordEncoder passwordEncoder) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.khachHangRepository = khachHangRepository;
        this.loaiSachRepository = loaiSachRepository;
        this.sachRepository = sachRepository;
        this.maGiamGiaRepository = maGiamGiaRepository;
        this.ptthThanhToanRepository = ptthThanhToanRepository;
        this.gioHangRepository = gioHangRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initAdminAccount();
        initPaymentMethods();
        initCoupons();
        initSampleDataIfEmpty();
    }

    private void initCoupons() {
        // Chỉ khởi tạo mã giảm giá mặc định khi bảng MAGIAMGIA còn trống,
        // không ghi đè hoặc tự tạo lại các mã mà Admin đã chỉnh sửa / xóa trên Web.
        if (maGiamGiaRepository.count() == 0) {
            createOrUpdateCoupon("NHANAM10", "Giảm 10% đơn từ 200K", new BigDecimal("10.00"), new BigDecimal("40000"), new BigDecimal("200000"), 150);
            createOrUpdateCoupon("FREESHIP", "Miễn phí vận chuyển toàn quốc", null, new BigDecimal("30000"), new BigDecimal("150000"), 300);
            createOrUpdateCoupon("BANMOI20", "Tặng 20K cho bạn đọc mới", null, new BigDecimal("20000"), new BigDecimal("99000"), 500);
            createOrUpdateCoupon("NHANAM50", "Giảm 50K cho đơn từ 500K", null, new BigDecimal("50000"), new BigDecimal("500000"), 80);
            createOrUpdateCoupon("THIEUNHI15", "Giảm 15% sách Thiếu nhi", new BigDecimal("15.00"), new BigDecimal("35000"), new BigDecimal("120000"), 100);
            log.info("Initialized default promotional coupons");
        }
    }

    private void createOrUpdateCoupon(String code, String name, BigDecimal pct, BigDecimal maxDiscount, BigDecimal minOrder, int qty) {
        MaGiamGia m = maGiamGiaRepository.findByMaCodeIgnoreCase(code).orElse(null);
        if (m == null) {
            m = new MaGiamGia();
            m.setMaCode(code);
        }
        m.setTenMa(name);
        m.setPhanTramGiam(pct);
        m.setSoTienGiamToiDa(maxDiscount);
        m.setGiaTriDonToiThieu(minOrder);
        m.setSoLuong(qty);
        m.setTrangThai("HOAT_DONG");
        m.setNgayBatDau(LocalDateTime.now().minusDays(1));
        m.setNgayKetThuc(LocalDateTime.now().plusMonths(6));
        maGiamGiaRepository.save(m);
    }

    private void initAdminAccount() {
        TaiKhoan admin = taiKhoanRepository.findByTenDN("admin").orElseGet(() -> {
            TaiKhoan newAdmin = new TaiKhoan();
            newAdmin.setEmail("admin@bookmind.vn");
            newAdmin.setTenDN("admin");
            newAdmin.setMatKhau(passwordEncoder.encode("admin123"));
            newAdmin.setPtDangNhap("LOCAL");
            newAdmin.setVaiTro("ADMIN");
            newAdmin.setTrangThai("HOAT_DONG");
            log.info("Initialized default Admin account: admin / admin123");
            return taiKhoanRepository.save(newAdmin);
        });

        // Đảm bảo tài khoản Admin có hồ sơ Khách hàng và Giỏ hàng (TAIKHOAN.MaKH -> KHACHHANG.MaKH theo ERD)
        if (admin.getKhachHang() == null) {
            String adminPhone = "0900000001";
            if (khachHangRepository.existsBySdt(adminPhone)) {
                adminPhone = "090" + String.format("%07d", admin.getMaTK() != null ? admin.getMaTK() : 999999);
            }
            KhachHang khAdmin = new KhachHang();
            khAdmin.setHoTen("Quản trị viên");
            khAdmin.setSdt(adminPhone);
            khAdmin.setGioiTinh(true);
            khAdmin = khachHangRepository.save(khAdmin);

            admin.setKhachHang(khAdmin);
            taiKhoanRepository.save(admin);

            if (gioHangRepository.findByKhachHang(khAdmin).isEmpty()) {
                GioHang gh = new GioHang();
                gh.setKhachHang(khAdmin);
                gioHangRepository.save(gh);
            }
            log.info("Initialized KhachHang profile & GioHang for Admin account");
        }
    }

    private void initPaymentMethods() {
        if (ptthThanhToanRepository.count() == 0) {
            PtthThanhToan cod = new PtthThanhToan();
            cod.setTenPTTT("Tiền mặt");
            cod.setMoTa("Thanh toán khi nhận hàng (COD)");
            cod.setTrangThai("HOAT_DONG");
            ptthThanhToanRepository.save(cod);

            PtthThanhToan momo = new PtthThanhToan();
            momo.setTenPTTT("MoMo");
            momo.setMoTa("Thanh toán trực tuyến qua Cổng MoMo (QR & App)");
            momo.setTrangThai("HOAT_DONG");
            ptthThanhToanRepository.save(momo);

            PtthThanhToan vietqr = new PtthThanhToan();
            vietqr.setTenPTTT("VietQR");
            vietqr.setMoTa("Thanh toán chuyển khoản ngân hàng bằng mã VietQR (PayOS)");
            vietqr.setTrangThai("HOAT_DONG");
            ptthThanhToanRepository.save(vietqr);

            log.info("Initialized default payment methods: Tiền mặt, MoMo, VietQR");
        } else {
            // Ngừng kích hoạt VNPay theo yêu cầu gỡ bỏ VNPay
            ptthThanhToanRepository.findAll().stream()
                    .filter(p -> "VNPay".equalsIgnoreCase(p.getTenPTTT()))
                    .forEach(p -> {
                        if (!"NGUNG".equals(p.getTrangThai())) {
                            p.setTrangThai("NGUNG");
                            ptthThanhToanRepository.save(p);
                            log.info("Deactivated VNPay payment method");
                        }
                    });
        }
    }

    private LoaiSach createCategory(String tenLoai, String moTa) {
        LoaiSach l = new LoaiSach();
        l.setTenLoai(tenLoai);
        l.setMoTa(moTa);
        l.setTrangThai("HOAT_DONG");
        return loaiSachRepository.save(l);
    }

    private void createSampleBook(LoaiSach loaiSach, String tenSach, String tacGia, String isbn,
                                  int soLuong, String moTa, String giaBan, String hinhAnh,
                                  String nxb, int namXb, int soTrang) {
        Sach s = new Sach();
        s.setLoaiSach(loaiSach);
        s.setTenSach(tenSach);
        s.setTacGia(tacGia);
        s.setIsbn(isbn);
        s.setSoLuong(soLuong);
        s.setMoTa(moTa);
        s.setGiaBan(new BigDecimal(giaBan));
        s.setHinhAnh(hinhAnh);
        s.setNxb(nxb);
        s.setNamXb(namXb);
        s.setSoTrang(soTrang);
        s.setTrangThai("DANG_BAN");
        sachRepository.save(s);
    }

    private void initSampleDataIfEmpty() {
        if (loaiSachRepository.count() == 0) {
            LoaiSach l1 = createCategory("Công nghệ thông tin", "Sách lập trình, kiến trúc hệ thống, AI và dữ liệu");
            LoaiSach l2 = createCategory("Kinh tế & Kinh doanh", "Sách quản trị, đầu tư, tài chính và khởi nghiệp");
            LoaiSach l3 = createCategory("Văn học & Nghệ thuật", "Tiểu thuyết, truyện ngắn, tác phẩm kinh điển");
            LoaiSach l4 = createCategory("Tâm lý & Kỹ năng sống", "Sách phát triển bản thân, tư duy tích cực, thói quen");

            createSampleBook(l1, "Clean Code: Mã Sạch và Nghệ Thuật Lập Trình", "Robert C. Martin", "978-0132350884",
                    50, "Cuốn sách kinh điển giúp bạn viết code dễ đọc, dễ bảo trì và đạt chuẩn kiến trúc phần mềm chuyên nghiệp.",
                    "215000", "https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=600&auto=format&fit=crop&q=80",
                    "NXB Thông Tin & Truyền Thông", 2023, 464);

            createSampleBook(l1, "Thiết Kế Hệ Thống Quy Mô Lớn (System Design)", "Alex Xu", "978-1736049112",
                    40, "Cẩm nang toàn diện về thiết kế hệ thống phân tán, microservices, caching và xử lý hàng triệu request.",
                    "280000", "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80",
                    "NXB Dân Trí", 2024, 320);

            createSampleBook(l2, "Tâm Lý Học Về Tiền (The Psychology of Money)", "Morgan Housel", "978-0857197689",
                    65, "Những bài học vượt thời gian về sự giàu có, lòng tham và hạnh phúc thông qua góc nhìn tâm lý con người.",
                    "165000", "https://images.unsplash.com/photo-1592496431122-2349e0fbc666?w=600&auto=format&fit=crop&q=80",
                    "NXB Trẻ", 2022, 256);

            createSampleBook(l4, "Atomic Habits - Thay Đổi Tí Hon Hiệu Quả Bất Ngờ", "James Clear", "978-0735211292",
                    80, "Phương pháp khoa học đã được kiểm chứng để xây dựng thói quen tốt và phá bỏ những thói quen xấu mỗi ngày.",
                    "189000", "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80",
                    "NXB Thế Giới", 2023, 336);

            createSampleBook(l3, "Nhà Giả Kim (The Alchemist)", "Paulo Coelho", "978-0062315007",
                    100, "Kiệt tác văn học truyền cảm hứng sống và theo đuổi ước mơ của hàng chục triệu độc giả trên toàn cầu.",
                    "99000", "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=600&auto=format&fit=crop&q=80",
                    "NXB Hội Nhà Văn", 2023, 228);

            createOrUpdateCoupon("BOOKMIND10", "Giảm 10% đơn hàng từ 200.000đ", new BigDecimal("10.00"), new BigDecimal("50000"), new BigDecimal("200000"), 100);
            createOrUpdateCoupon("FREESHIP", "Miễn phí vận chuyển 30.000đ", null, new BigDecimal("30000"), new BigDecimal("150000"), 200);

            log.info("Initialized sample categories, books, and coupons");
        }
    }
}
