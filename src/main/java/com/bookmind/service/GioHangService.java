package com.bookmind.service;

import com.bookmind.entity.*;
import com.bookmind.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GioHangService {

    private final GioHangRepository gioHangRepository;
    private final ChiTietGioHangRepository chiTietGioHangRepository;
    private final SachRepository sachRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public GioHangService(GioHangRepository gioHangRepository,
                          ChiTietGioHangRepository chiTietGioHangRepository,
                          SachRepository sachRepository) {
        this.gioHangRepository = gioHangRepository;
        this.chiTietGioHangRepository = chiTietGioHangRepository;
        this.sachRepository = sachRepository;
    }

    public GioHang getOrCreateCart(KhachHang khachHang) {
        return gioHangRepository.findByKhachHang_MaKH(khachHang.getMaKH())
                .orElseGet(() -> {
                    GioHang gh = new GioHang();
                    gh.setKhachHang(khachHang);
                    return gioHangRepository.save(gh);
                });
    }

    @Transactional
    public List<ChiTietGioHang> getCartItems(KhachHang khachHang) {
        GioHang cart = getOrCreateCart(khachHang);
        List<ChiTietGioHang> items = chiTietGioHangRepository.findByGioHang_MaGioHang(cart.getMaGioHang());
        // Tự động đồng bộ giá bán mới nhất nếu Admin đã chỉnh sửa giá sách
        for (ChiTietGioHang item : items) {
            if (item.getSach() != null && item.getSach().getGiaBan() != null) {
                if (item.getDonGia() == null || item.getDonGia().compareTo(item.getSach().getGiaBan()) != 0) {
                    item.setDonGia(item.getSach().getGiaBan());
                    chiTietGioHangRepository.save(item);
                }
            }
        }
        return items;
    }

    public int getCartItemCount(KhachHang khachHang) {
        if (khachHang == null || khachHang.getMaKH() == null) {
            return 0;
        }
        try {
            return chiTietGioHangRepository.sumQuantityByMaKH(khachHang.getMaKH());
        } catch (Exception e) {
            return 0;
        }
    }

    public BigDecimal calculateSubtotal(List<ChiTietGioHang> items) {
        return items.stream()
                .map(item -> item.getDonGia().multiply(BigDecimal.valueOf(item.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void addToCart(KhachHang khachHang, Long maSach, int soLuong) {
        if (soLuong <= 0) soLuong = 1;
        GioHang cart = getOrCreateCart(khachHang);
        Sach sach = sachRepository.findById(maSach)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách: " + maSach));

        if (sach.getSoLuong() < soLuong) {
            throw new RuntimeException("Số lượng tồn kho không đủ (" + sach.getSoLuong() + " cuốn còn lại)");
        }

        ChiTietGioHangId id = new ChiTietGioHangId(cart.getMaGioHang(), sach.getMaSach());
        ChiTietGioHang item = chiTietGioHangRepository.findById(id)
                .orElse(null);

        if (item != null) {
            int newQuantity = item.getSoLuong() + soLuong;
            if (newQuantity > sach.getSoLuong()) {
                throw new RuntimeException("Số lượng trong giỏ vượt quá tồn kho hiện có.");
            }
            item.setSoLuong(newQuantity);
            item.setDonGia(sach.getGiaBan());
            chiTietGioHangRepository.save(item);
        } else {
            ChiTietGioHang newItem = new ChiTietGioHang();
            newItem.setId(id);
            newItem.setGioHang(cart);
            newItem.setSach(sach);
            newItem.setSoLuong(soLuong);
            newItem.setDonGia(sach.getGiaBan());
            chiTietGioHangRepository.save(newItem);
        }
    }

    @Transactional
    public void updateQuantity(KhachHang khachHang, Long maSach, int soLuong) {
        GioHang cart = getOrCreateCart(khachHang);
        ChiTietGioHangId id = new ChiTietGioHangId(cart.getMaGioHang(), maSach);
        if (soLuong <= 0) {
            chiTietGioHangRepository.deleteById(id);
        } else {
            ChiTietGioHang item = chiTietGioHangRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong giỏ hàng"));
            Sach sach = item.getSach();
            if (soLuong > sach.getSoLuong()) {
                throw new RuntimeException("Kho chỉ còn " + sach.getSoLuong() + " cuốn");
            }
            item.setSoLuong(soLuong);
            chiTietGioHangRepository.save(item);
        }
    }

    @Transactional
    public void removeFromCart(KhachHang khachHang, Long maSach) {
        GioHang cart = getOrCreateCart(khachHang);
        ChiTietGioHangId id = new ChiTietGioHangId(cart.getMaGioHang(), maSach);
        chiTietGioHangRepository.deleteById(id);
    }

    @Transactional
    public void clearCart(KhachHang khachHang) {
        GioHang cart = getOrCreateCart(khachHang);
        chiTietGioHangRepository.deleteByMaGioHang(cart.getMaGioHang());
    }

    /** Lấy các dòng giỏ hàng theo danh sách mã sách được chọn. Danh sách rỗng/null = toàn bộ giỏ. */
    public List<ChiTietGioHang> getSelectedItems(KhachHang khachHang, List<Long> maSachList) {
        List<ChiTietGioHang> all = getCartItems(khachHang);
        if (maSachList == null || maSachList.isEmpty()) {
            return all;
        }
        return all.stream()
                .filter(item -> maSachList.contains(item.getSach().getMaSach()))
                .toList();
    }

    /** Chỉ xóa khỏi giỏ những sách đã được đặt mua, giữ lại phần còn lại. */
    @Transactional
    public void removeItems(KhachHang khachHang, List<ChiTietGioHang> items) {
        GioHang cart = getOrCreateCart(khachHang);
        for (ChiTietGioHang item : items) {
            chiTietGioHangRepository.deleteById(new ChiTietGioHangId(cart.getMaGioHang(), item.getSach().getMaSach()));
        }
    }
}
