package com.bookmind.service;

import com.bookmind.entity.*;
import com.bookmind.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class KhachHangService {

    private final KhachHangRepository khachHangRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final DiaChiRepository diaChiRepository;
    private final DonHangRepository donHangRepository;
    private final GioHangRepository gioHangRepository;
    private final DanhGiaRepository danhGiaRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Autowired
    public KhachHangService(KhachHangRepository khachHangRepository,
                            TaiKhoanRepository taiKhoanRepository,
                            DiaChiRepository diaChiRepository,
                            DonHangRepository donHangRepository,
                            GioHangRepository gioHangRepository,
                            DanhGiaRepository danhGiaRepository,
                            PasswordEncoder passwordEncoder) {
        this.khachHangRepository = khachHangRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.diaChiRepository = diaChiRepository;
        this.donHangRepository = donHangRepository;
        this.gioHangRepository = gioHangRepository;
        this.danhGiaRepository = danhGiaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<KhachHang> getAllForAdmin(String keyword, String ptDangNhap, String vaiTro, String trangThai, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "maKH"));

        Specification<KhachHang> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            boolean isCountQuery = query.getResultType() == Long.class || query.getResultType() == long.class;
            @SuppressWarnings("unchecked")
            Join<KhachHang, TaiKhoan> tkJoin = !isCountQuery
                    ? (Join<KhachHang, TaiKhoan>) (Object) root.fetch("taiKhoan", JoinType.LEFT)
                    : root.join("taiKhoan", JoinType.LEFT);

            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                String cleanNum = keyword.trim().replace("#", "").trim();

                List<Predicate> orPreds = new ArrayList<>();
                orPreds.add(cb.like(cb.lower(root.get("hoTen")), kw));
                orPreds.add(cb.like(cb.lower(cb.coalesce(root.get("sdt"), "")), kw));
                orPreds.add(cb.like(cb.lower(tkJoin.get("email")), kw));
                orPreds.add(cb.like(cb.lower(cb.coalesce(tkJoin.get("tenDN"), "")), kw));

                if (!cleanNum.isEmpty()) {
                    orPreds.add(cb.like(root.get("maKH").as(String.class), "%" + cleanNum + "%"));
                }
                predicates.add(cb.or(orPreds.toArray(new Predicate[0])));
            }

            if (ptDangNhap != null && !ptDangNhap.trim().isEmpty() && !"ALL".equalsIgnoreCase(ptDangNhap)) {
                predicates.add(cb.equal(tkJoin.get("ptDangNhap"), ptDangNhap.trim()));
            }

            if (vaiTro != null && !vaiTro.trim().isEmpty() && !"ALL".equalsIgnoreCase(vaiTro)) {
                predicates.add(cb.equal(tkJoin.get("vaiTro"), vaiTro.trim()));
            }

            if (trangThai != null && !trangThai.trim().isEmpty() && !"ALL".equalsIgnoreCase(trangThai)) {
                predicates.add(cb.equal(tkJoin.get("trangThai"), trangThai.trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return khachHangRepository.findAll(spec, pageable);
    }

    public Page<KhachHang> getAllForAdmin(int page, int size) {
        return getAllForAdmin(null, null, null, null, page, size);
    }

    public KhachHang getById(Long maKH) {
        return khachHangRepository.findById(maKH)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng: " + maKH));
    }

    public long countOrders(Long maKH) {
        return donHangRepository.countByKhachHang_MaKH(maKH);
    }

    @Transactional
    public void toggleLockAccount(Long maTK) {
        TaiKhoan tk = taiKhoanRepository.findById(maTK)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + maTK));
        if ("ADMIN".equalsIgnoreCase(tk.getVaiTro())) {
            throw new RuntimeException("Không thể khóa tài khoản Quản trị viên (ADMIN)!");
        }
        if ("HOAT_DONG".equalsIgnoreCase(tk.getTrangThai())) {
            tk.setTrangThai("KHOA");
        } else {
            tk.setTrangThai("HOAT_DONG");
        }
        taiKhoanRepository.save(tk);
    }

    @Transactional
    public void deleteCustomer(Long maKH) {
        KhachHang kh = getById(maKH);
        TaiKhoan tk = kh.getTaiKhoan();

        if (tk != null && "ADMIN".equalsIgnoreCase(tk.getVaiTro())) {
            throw new RuntimeException("Không thể xóa tài khoản Quản trị viên (ADMIN)!");
        }

        long orderCount = donHangRepository.countByKhachHang_MaKH(maKH);
        if (orderCount > 0) {
            // Có đơn hàng -> Khóa tài khoản để bảo toàn dữ liệu lịch sử hóa đơn
            if (tk != null && "HOAT_DONG".equalsIgnoreCase(tk.getTrangThai())) {
                tk.setTrangThai("KHOA");
                taiKhoanRepository.save(tk);
            }
            throw new RuntimeException("Khách hàng #" + maKH + " (" + kh.getHoTen() + ") đã có " + orderCount + 
                    " đơn hàng trong hệ thống. Để bảo vệ dữ liệu lịch sử hóa đơn, không thể xóa khách hàng này. Hệ thống đã tự động khóa tài khoản!");
        }

        // 0 đơn hàng -> Tiến hành xóa an toàn (không có đơn hàng nên cũng không có đánh giá theo ERD)
        gioHangRepository.findByKhachHang(kh).ifPresent(gioHangRepository::delete);
        diaChiRepository.findByKhachHang_MaKH(maKH).forEach(diaChiRepository::delete);

        if (tk != null) {
            tk.setKhachHang(null);
            taiKhoanRepository.delete(tk);
        }
        khachHangRepository.delete(kh);
    }

    @Transactional
    public KhachHang updateProfile(Long maKH, String hoTen, String sdt, Boolean gioiTinh, LocalDate ngaySinh) {
        return updateProfile(maKH, hoTen, sdt, gioiTinh, ngaySinh, null);
    }

    @Transactional
    public KhachHang updateProfile(Long maKH, String hoTen, String sdt, Boolean gioiTinh, LocalDate ngaySinh, String anhDaiDien) {
        KhachHang kh = getById(maKH);
        if (hoTen == null || hoTen.trim().isEmpty()) {
            throw new RuntimeException("Họ và tên không được để trống!");
        }

        if (sdt != null && !sdt.trim().isEmpty()) {
            String cleanPhone = sdt.trim();
            Optional<KhachHang> existing = khachHangRepository.findBySdt(cleanPhone);
            if (existing.isPresent() && !existing.get().getMaKH().equals(maKH)) {
                throw new RuntimeException("Số điện thoại này đã được sử dụng bởi một tài khoản khác!");
            }
            kh.setSdt(cleanPhone);
        } else {
            kh.setSdt(null);
        }

        kh.setHoTen(hoTen.trim());
        kh.setGioiTinh(gioiTinh);
        kh.setNgaySinh(ngaySinh);
        if (anhDaiDien != null && !anhDaiDien.isBlank()) {
            kh.setAnhDaiDien(anhDaiDien.trim());
        }
        return khachHangRepository.save(kh);
    }

    @Transactional
    public void changePassword(Long maTK, String oldPassword, String newPassword, String confirmPassword) {
        TaiKhoan tk = taiKhoanRepository.findById(maTK)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + maTK));

        if (!"LOCAL".equalsIgnoreCase(tk.getPtDangNhap())) {
            throw new RuntimeException("Tài khoản đăng nhập qua mạng xã hội (Google/Facebook) không sử dụng mật khẩu trực tiếp!");
        }

        if (oldPassword == null || !passwordEncoder.matches(oldPassword, tk.getMatKhau())) {
            throw new RuntimeException("Mật khẩu hiện tại không chính xác!");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có tối thiểu 6 ký tự!");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Xác nhận mật khẩu mới không trùng khớp!");
        }

        tk.setMatKhau(passwordEncoder.encode(newPassword));
        taiKhoanRepository.save(tk);
    }

    public List<DiaChi> getAddresses(Long maKH) {
        return diaChiRepository.findByKhachHang_MaKH(maKH);
    }

    @Transactional
    public DiaChi addAddress(Long maKH, String hoTenNguoiNhan, String sdtNguoiNhan, String diaChiNhan, boolean macDinh) {
        KhachHang kh = getById(maKH);
        List<DiaChi> currentList = diaChiRepository.findByKhachHang_MaKH(maKH);

        boolean shouldBeDefault = macDinh || currentList.isEmpty();
        if (shouldBeDefault) {
            for (DiaChi d : currentList) {
                if (Boolean.TRUE.equals(d.getMacDinh())) {
                    d.setMacDinh(false);
                    diaChiRepository.save(d);
                }
            }
        }

        DiaChi diaChi = new DiaChi();
        diaChi.setKhachHang(kh);
        diaChi.setHoTenNguoiNhan(hoTenNguoiNhan != null ? hoTenNguoiNhan.trim() : kh.getHoTen());
        diaChi.setSdtNguoiNhan(sdtNguoiNhan != null ? sdtNguoiNhan.trim() : (kh.getSdt() != null ? kh.getSdt() : ""));
        diaChi.setDiaChiNhan(diaChiNhan.trim());
        diaChi.setMacDinh(shouldBeDefault);

        return diaChiRepository.save(diaChi);
    }

    @Transactional
    public void deleteAddress(Long maKH, Long maDiaChi) {
        DiaChi d = diaChiRepository.findByMaDiaChiAndKhachHang_MaKH(maDiaChi, maKH)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ!"));
        diaChiRepository.delete(d);

        if (Boolean.TRUE.equals(d.getMacDinh())) {
            List<DiaChi> remaining = diaChiRepository.findByKhachHang_MaKH(maKH);
            if (!remaining.isEmpty()) {
                remaining.get(0).setMacDinh(true);
                diaChiRepository.save(remaining.get(0));
            }
        }
    }

    @Transactional
    public void setDefaultAddress(Long maKH, Long maDiaChi) {
        List<DiaChi> list = diaChiRepository.findByKhachHang_MaKH(maKH);
        for (DiaChi d : list) {
            boolean isTarget = d.getMaDiaChi().equals(maDiaChi);
            d.setMacDinh(isTarget);
            diaChiRepository.save(d);
        }
    }
}
