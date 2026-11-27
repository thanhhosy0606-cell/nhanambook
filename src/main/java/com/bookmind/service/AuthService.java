package com.bookmind.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmind.dto.RegisterRequest;
import com.bookmind.entity.GioHang;
import com.bookmind.entity.KhachHang;
import com.bookmind.entity.TaiKhoan;
import com.bookmind.repository.GioHangRepository;
import com.bookmind.repository.KhachHangRepository;
import com.bookmind.repository.TaiKhoanRepository;

@Service
public class AuthService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final GioHangRepository gioHangRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(TaiKhoanRepository taiKhoanRepository,
                       KhachHangRepository khachHangRepository,
                       GioHangRepository gioHangRepository,
                       PasswordEncoder passwordEncoder) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.khachHangRepository = khachHangRepository;
        this.gioHangRepository = gioHangRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<TaiKhoan> authenticate(String usernameOrEmail, String password) {
        Optional<TaiKhoan> opt = taiKhoanRepository.findByTenDNOrEmail(usernameOrEmail, usernameOrEmail);
        if (opt.isPresent()) {
            TaiKhoan tk = opt.get();
            if ("KHOA".equalsIgnoreCase(tk.getTrangThai())) {
                throw new RuntimeException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
            }
            if (tk.getMatKhau() != null && passwordEncoder.matches(password, tk.getMatKhau())) {
                return Optional.of(tk);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public TaiKhoan register(RegisterRequest req) {
        if (taiKhoanRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }
        if (taiKhoanRepository.existsByTenDN(req.getTenDN())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }
        if (req.getSdt() != null && !req.getSdt().isBlank() && khachHangRepository.existsBySdt(req.getSdt())) {
            throw new RuntimeException("Số điện thoại đã được sử dụng");
        }

        KhachHang kh = new KhachHang();
        kh.setHoTen(req.getHoTen());
        kh.setSdt(req.getSdt());
        kh.setGioiTinh(true);
        kh = khachHangRepository.save(kh);

        TaiKhoan tk = new TaiKhoan();
        tk.setKhachHang(kh);
        tk.setEmail(req.getEmail());
        tk.setTenDN(req.getTenDN());
        tk.setMatKhau(passwordEncoder.encode(req.getMatKhau()));
        tk.setPtDangNhap("LOCAL");
        tk.setVaiTro("KHACH_HANG");
        tk.setTrangThai("HOAT_DONG");
        tk = taiKhoanRepository.save(tk);

        // Tạo giỏ hàng gắn với khách hàng
        GioHang gh = new GioHang();
        gh.setKhachHang(kh);
        gioHangRepository.save(gh);

        return tk;
    }

    public Optional<KhachHang> getKhachHangByTaiKhoan(TaiKhoan taiKhoan) {
        if (taiKhoan == null) return Optional.empty();
        if (taiKhoan.getKhachHang() != null && taiKhoan.getKhachHang().getMaKH() != null) {
            return khachHangRepository.findById(taiKhoan.getKhachHang().getMaKH());
        }
        return khachHangRepository.findByTaiKhoan(taiKhoan);
    }

    @Transactional
    public KhachHang getOrCreateKhachHangForTaiKhoan(TaiKhoan taiKhoan) {
        if (taiKhoan == null) {
            return null;
        }
        TaiKhoan managedTk = taiKhoan.getMaTK() != null
                ? taiKhoanRepository.findById(taiKhoan.getMaTK()).orElse(taiKhoan)
                : taiKhoan;
        if (managedTk.getKhachHang() != null && managedTk.getKhachHang().getMaKH() != null) {
            return khachHangRepository.findById(managedTk.getKhachHang().getMaKH())
                    .orElse(managedTk.getKhachHang());
        }
        return khachHangRepository.findByTaiKhoan(managedTk).orElseGet(() -> {
            String hoTen = "ADMIN".equalsIgnoreCase(managedTk.getVaiTro()) ? "Quản trị viên" : (managedTk.getTenDN() != null ? managedTk.getTenDN() : "Khách hàng");
            String defaultSdt = "ADMIN".equalsIgnoreCase(managedTk.getVaiTro()) ? "0900000001" : null;
            if (defaultSdt != null && khachHangRepository.existsBySdt(defaultSdt)) {
                defaultSdt = "090" + String.format("%07d", managedTk.getMaTK() != null ? managedTk.getMaTK() : 999999);
            }

            KhachHang kh = new KhachHang();
            kh.setHoTen(hoTen);
            kh.setSdt(defaultSdt);
            kh.setGioiTinh(true);
            kh = khachHangRepository.save(kh);

            managedTk.setKhachHang(kh);
            taiKhoanRepository.save(managedTk);

            if (gioHangRepository.findByKhachHang(kh).isEmpty()) {
                GioHang gh = new GioHang();
                gh.setKhachHang(kh);
                gioHangRepository.save(gh);
            }
            return kh;
        });
    }

    @Transactional
    public TaiKhoan processOAuthSuccess(String email, String name, String avatarUrl, String provider) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Không thể xác thực: Không nhận được địa chỉ email từ " + provider);
        }
        String cleanEmail = email.trim().toLowerCase();
        Optional<TaiKhoan> opt = taiKhoanRepository.findByEmail(cleanEmail);

        TaiKhoan tk;
        if (opt.isPresent()) {
            tk = opt.get();
            if ("KHOA".equalsIgnoreCase(tk.getTrangThai())) {
                throw new RuntimeException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
            }
            if ("LOCAL".equalsIgnoreCase(tk.getPtDangNhap()) || tk.getPtDangNhap() == null) {
                tk.setPtDangNhap(provider.toUpperCase());
                tk = taiKhoanRepository.save(tk);
            }
        } else {
            String baseUsername = cleanEmail.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
            if (baseUsername.isBlank()) baseUsername = provider.toLowerCase() + "_user";
            String username = baseUsername;
            int count = 1;
            while (taiKhoanRepository.existsByTenDN(username)) {
                username = baseUsername + count++;
            }

            tk = new TaiKhoan();
            tk.setEmail(cleanEmail);
            tk.setTenDN(username);
            tk.setPtDangNhap(provider.toUpperCase());
            tk.setVaiTro("KHACH_HANG");
            tk.setTrangThai("HOAT_DONG");
            tk = taiKhoanRepository.save(tk);
        }

        KhachHang kh = getOrCreateKhachHangForTaiKhoan(tk);
        boolean updatedKh = false;
        if (name != null && !name.isBlank() && (kh.getHoTen() == null || kh.getHoTen().equals(tk.getTenDN()) || kh.getHoTen().equals("Khách hàng"))) {
            kh.setHoTen(name.trim());
            updatedKh = true;
        }
        if (avatarUrl != null && !avatarUrl.isBlank() && (kh.getAnhDaiDien() == null || kh.getAnhDaiDien().isBlank())) {
            kh.setAnhDaiDien(avatarUrl.trim());
            updatedKh = true;
        }
        if (updatedKh) {
            khachHangRepository.save(kh);
        }

        return tk;
    }
}
