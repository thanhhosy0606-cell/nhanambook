package com.bookmind.service;

import com.bookmind.dto.SachFormDto;
import com.bookmind.entity.DanhGia;
import com.bookmind.entity.LoaiSach;
import com.bookmind.entity.Sach;
import com.bookmind.repository.ChiTietDonHangRepository;
import com.bookmind.repository.ChiTietGioHangRepository;
import com.bookmind.repository.DanhGiaRepository;
import com.bookmind.repository.LoaiSachRepository;
import com.bookmind.repository.SachRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SachService {

    private final SachRepository sachRepository;
    private final LoaiSachRepository loaiSachRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final ChiTietGioHangRepository chiTietGioHangRepository;
    private final DanhGiaRepository danhGiaRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public SachService(SachRepository sachRepository,
                       LoaiSachRepository loaiSachRepository,
                       ChiTietDonHangRepository chiTietDonHangRepository,
                       ChiTietGioHangRepository chiTietGioHangRepository,
                       DanhGiaRepository danhGiaRepository) {
        this.sachRepository = sachRepository;
        this.loaiSachRepository = loaiSachRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.chiTietGioHangRepository = chiTietGioHangRepository;
        this.danhGiaRepository = danhGiaRepository;
    }

    public Page<Sach> getBooksForShop(Long maLoai, String keyword, int page, int size) {
        return getFilteredBooks(maLoai, keyword, null, null, null, null, null, page, size);
    }

    public Page<Sach> getFilteredBooks(Long maLoai, String keyword, String tacGia,
                                       BigDecimal minPrice, BigDecimal maxPrice, Integer rating,
                                       String sort, int page, int size) {
        Sort sortObj = Sort.by("maSach").descending();
        if ("price_asc".equalsIgnoreCase(sort)) {
            sortObj = Sort.by("giaBan").ascending();
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            sortObj = Sort.by("giaBan").descending();
        } else if ("name_asc".equalsIgnoreCase(sort)) {
            sortObj = Sort.by("tenSach").ascending();
        }
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Specification<Sach> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("trangThai"), "DANG_BAN"));

            if (maLoai != null) {
                predicates.add(cb.equal(root.get("loaiSach").get("maLoai"), maLoai));
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("tenSach")), kw),
                        cb.like(cb.lower(root.get("tacGia")), kw)
                ));
            }
            if (tacGia != null && !tacGia.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("tacGia"), tacGia.trim()));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("giaBan"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("giaBan"), maxPrice));
            }
            if (rating != null && rating >= 1 && rating <= 5) {
                Subquery<Long> ratingSubquery = query.subquery(Long.class);
                Root<DanhGia> dgRoot = ratingSubquery.from(DanhGia.class);
                ratingSubquery.select(dgRoot.get("sach").get("maSach"));
                ratingSubquery.where(cb.equal(dgRoot.get("trangThai"), "HIEN_THI"));
                ratingSubquery.groupBy(dgRoot.get("sach").get("maSach"));

                double minAvg = rating - 0.5;
                double maxAvg = rating + 0.5;
                if (rating == 5) {
                    ratingSubquery.having(cb.greaterThanOrEqualTo(cb.avg(dgRoot.get("soSao")), minAvg));
                } else {
                    ratingSubquery.having(cb.and(
                            cb.greaterThanOrEqualTo(cb.avg(dgRoot.get("soSao")), minAvg),
                            cb.lessThan(cb.avg(dgRoot.get("soSao")), maxAvg)
                    ));
                }
                predicates.add(root.get("maSach").in(ratingSubquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return sachRepository.findAll(spec, pageable);
    }

    public List<String> getTopAuthors(int limit) {
        return sachRepository.findDistinctTacGia().stream().limit(limit).toList();
    }

    /**
     * Bản đồ số lượng đã bán thực tế của từng mã sách (tính từ CHITIETDONHANG các đơn không bị hủy).
     */
    public Map<Long, Long> getSoldQuantitiesMap() {
        Map<Long, Long> map = new HashMap<>();
        try {
            List<Object[]> rows = chiTietDonHangRepository.findTopSellingBooksStats();
            for (Object[] row : rows) {
                if (row[0] != null && row[1] != null) {
                    Long maSach = ((Number) row[0]).longValue();
                    Long totalSold = ((Number) row[1]).longValue();
                    map.put(maSach, totalSold);
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    /**
     * Sách Bán Chạy & Nổi Bật thực tế:
     * Ưu tiên xếp theo tổng số lượng đã bán thực tế từ CHITIETDONHANG (đơn không hủy),
     * sau đó bổ sung các sách có điểm đánh giá cao từ DANHGIA để đủ số lượng hiển thị.
     */
    public List<Sach> getTopActiveBooks(int limit) {
        List<Sach> result = new ArrayList<>();
        Set<Long> addedIds = new LinkedHashSet<>();

        List<Object[]> topSoldRows = chiTietDonHangRepository.findTopSellingBooksStats();
        for (Object[] row : topSoldRows) {
            if (result.size() >= limit) break;
            Long maSach = ((Number) row[0]).longValue();
            sachRepository.findById(maSach).ifPresent(s -> {
                if ("DANG_BAN".equalsIgnoreCase(s.getTrangThai()) && s.getSoLuong() != null && s.getSoLuong() > 0) {
                    result.add(s);
                    addedIds.add(s.getMaSach());
                }
            });
        }

        if (result.size() < limit) {
            List<Sach> activeBooks = sachRepository.findByTrangThai("DANG_BAN").stream()
                    .filter(s -> !addedIds.contains(s.getMaSach()) && s.getSoLuong() != null && s.getSoLuong() > 0)
                    .sorted((a, b) -> {
                        int cmpRating = Double.compare(b.getDiemDanhGia(), a.getDiemDanhGia());
                        if (cmpRating != 0) return cmpRating;
                        int cmpReviews = Integer.compare(b.getSoLuotDanhGia(), a.getSoLuotDanhGia());
                        if (cmpReviews != 0) return cmpReviews;
                        return Long.compare(b.getMaSach(), a.getMaSach());
                    })
                    .toList();
            for (Sach s : activeBooks) {
                if (result.size() >= limit) break;
                result.add(s);
                addedIds.add(s.getMaSach());
            }
        }
        return result;
    }

    /**
     * Flash Sale Hàng Ngày:
     * Ưu tiên các đầu sách vừa có lượt mua thực tế từ CHITIETDONHANG vừa còn tồn kho dồi dào để kích cầu,
     * kết hợp các đầu sách có mức giá ưu đãi và đánh giá tốt.
     */
    public List<Sach> getFlashSaleBooks(int limit) {
        Map<Long, Long> soldMap = getSoldQuantitiesMap();
        List<Sach> activeBooks = sachRepository.findByTrangThai("DANG_BAN").stream()
                .filter(s -> s.getSoLuong() != null && s.getSoLuong() > 0)
                .sorted((a, b) -> {
                    long soldA = soldMap.getOrDefault(a.getMaSach(), 0L);
                    long soldB = soldMap.getOrDefault(b.getMaSach(), 0L);
                    // Ưu tiên sách đã có lượt mua thực tế trong CHITIETDONHANG + tồn kho tốt
                    double scoreA = soldA * 15.0 + a.getDiemDanhGia() * 5.0 + (a.getSoLuong() != null ? Math.min(a.getSoLuong(), 100) * 0.2 : 0)
                            - (a.getGiaBan() != null ? a.getGiaBan().doubleValue() / 20000.0 : 0);
                    double scoreB = soldB * 15.0 + b.getDiemDanhGia() * 5.0 + (b.getSoLuong() != null ? Math.min(b.getSoLuong(), 100) * 0.2 : 0)
                            - (b.getGiaBan() != null ? b.getGiaBan().doubleValue() / 20000.0 : 0);
                    return Double.compare(scoreB, scoreA);
                })
                .limit(limit)
                .toList();
        return activeBooks;
    }

    public List<Sach> getNewReleases(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Order.desc("namXb"), Sort.Order.desc("maSach")));
        return sachRepository.findByTrangThai("DANG_BAN", pageable).getContent();
    }

    /**
     * Best Seller Cho Bé (danh mục Thiếu nhi - mã loại 7):
     * Tính từ tổng số lượng bán thực tế trong CHITIETDONHANG của danh mục 7,
     * nếu chưa đủ limit thì bù thêm sách thiếu nhi có điểm đánh giá cao nhất.
     */
    public List<Sach> getBestSellersForKids(int limit) {
        List<Sach> result = new ArrayList<>();
        Set<Long> addedIds = new LinkedHashSet<>();

        List<Object[]> soldKids = chiTietDonHangRepository.findTopSellingBooksByCategory(7L);
        for (Object[] row : soldKids) {
            if (result.size() >= limit) break;
            Long maSach = ((Number) row[0]).longValue();
            sachRepository.findById(maSach).ifPresent(s -> {
                if ("DANG_BAN".equalsIgnoreCase(s.getTrangThai())) {
                    result.add(s);
                    addedIds.add(s.getMaSach());
                }
            });
        }

        if (result.size() < limit) {
            List<Sach> kidsBooks = sachRepository.findByLoaiSach_MaLoaiAndTrangThai(7L, "DANG_BAN").stream()
                    .filter(s -> !addedIds.contains(s.getMaSach()))
                    .sorted((a, b) -> {
                        int cmpRating = Double.compare(b.getDiemDanhGia(), a.getDiemDanhGia());
                        if (cmpRating != 0) return cmpRating;
                        return Integer.compare(b.getSoLuotDanhGia(), a.getSoLuotDanhGia());
                    })
                    .toList();
            for (Sach s : kidsBooks) {
                if (result.size() >= limit) break;
                result.add(s);
                addedIds.add(s.getMaSach());
            }
        }

        if (result.isEmpty()) {
            return getTopActiveBooks(limit);
        }
        return result;
    }

    public Sach getSachById(Long maSach) {
        return sachRepository.findById(maSach)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách mã: " + maSach));
    }

    public Page<Sach> getAllForAdmin(int page, int size) {
        return getBooksForAdmin(null, null, null, page, size);
    }

    public Page<Sach> getBooksForAdmin(String keyword, Long maLoai, String trangThai, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("maSach").descending());

        Specification<Sach> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("tenSach")), kw),
                        cb.like(cb.lower(root.get("tacGia")), kw),
                        cb.like(cb.lower(cb.coalesce(root.get("isbn"), "")), kw)
                ));
            }

            if (maLoai != null && maLoai > 0) {
                predicates.add(cb.equal(root.get("loaiSach").get("maLoai"), maLoai));
            }

            if (trangThai != null && !trangThai.trim().isEmpty() && !"ALL".equalsIgnoreCase(trangThai)) {
                predicates.add(cb.equal(root.get("trangThai"), trangThai.trim()));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return sachRepository.findAll(spec, pageable);
    }

    public long getCartCount(Long maSach) {
        return chiTietGioHangRepository.countBySach_MaSach(maSach);
    }

    public long getOrderCount(Long maSach) {
        return chiTietDonHangRepository.countBySach_MaSach(maSach);
    }

    @Transactional
    public String delete(Long maSach) {
        Sach sach = getSachById(maSach);
        long orderCount = chiTietDonHangRepository.countBySach_MaSach(maSach);

        if (orderCount > 0) {
            sach.setTrangThai("NGUNG_BAN");
            sachRepository.saveAndFlush(sach);
            return "Sách '" + sach.getTenSach() + "' đã phát sinh trong " + orderCount + " đơn hàng nên đã được chuyển sang trạng thái Ngừng bán trong CSDL.";
        } else {
            chiTietGioHangRepository.deleteByMaSach(maSach);
            danhGiaRepository.deleteBySach_MaSach(maSach);
            sachRepository.delete(sach);
            sachRepository.flush();
            return "Đã xóa hoàn toàn cuốn sách '" + sach.getTenSach() + "' khỏi cơ sở dữ liệu MySQL!";
        }
    }

    @Transactional
    public Sach save(SachFormDto dto) {
        LoaiSach loai = loaiSachRepository.findById(dto.getMaLoai())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại sách: " + dto.getMaLoai()));

        Sach sach;
        if (dto.getMaSach() != null) {
            sach = getSachById(dto.getMaSach());
        } else {
            sach = new Sach();
        }

        String cleanIsbn = (dto.getIsbn() != null && !dto.getIsbn().trim().isEmpty()) ? dto.getIsbn().trim() : null;

        sach.setLoaiSach(loai);
        sach.setTenSach(dto.getTenSach() != null ? dto.getTenSach().trim() : "");
        sach.setTacGia(dto.getTacGia() != null ? dto.getTacGia().trim() : "");
        sach.setIsbn(cleanIsbn);
        sach.setSoLuong(dto.getSoLuong());
        sach.setMoTa(dto.getMoTa());
        sach.setGiaBan(dto.getGiaBan());
        sach.setHinhAnh(dto.getHinhAnh() != null && !dto.getHinhAnh().trim().isEmpty() ? dto.getHinhAnh().trim() : null);
        sach.setNxb(dto.getNxb());
        sach.setNamXb(dto.getNamXb());
        sach.setSoTrang(dto.getSoTrang());
        if (dto.getTrangThai() != null) {
            sach.setTrangThai(dto.getTrangThai());
        }
        return sachRepository.save(sach);
    }

    @Transactional
    public void toggleStatus(Long maSach) {
        Sach sach = getSachById(maSach);
        if ("DANG_BAN".equalsIgnoreCase(sach.getTrangThai())) {
            sach.setTrangThai("NGUNG_BAN");
        } else {
            sach.setTrangThai("DANG_BAN");
        }
        sachRepository.save(sach);
    }
}
