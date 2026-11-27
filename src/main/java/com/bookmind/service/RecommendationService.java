package com.bookmind.service;

import com.bookmind.entity.DanhGia;
import com.bookmind.entity.KhachHang;
import com.bookmind.entity.Sach;
import com.bookmind.repository.ChiTietDonHangRepository;
import com.bookmind.repository.DanhGiaRepository;
import com.bookmind.repository.SachRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Hệ thống Gợi ý Sách Thông minh (BookMind AI Recommendation Engine):
 * Kết hợp Collaborative Filtering ("Người mua sách này cũng mua" từ CHITIETDONHANG & DANHGIA)
 * và Content-Based Filtering (Cùng tác giả, cùng thể loại, điểm đánh giá cao).
 * Không thay đổi bất kỳ bảng hay cột nào trong CSDL.
 */
@Service
public class RecommendationService {

    private final SachRepository sachRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final DanhGiaRepository danhGiaRepository;
    private final SachService sachService;

    public static class RecommendedBookItem {
        private final Sach sach;
        private final String reason;
        private final long soldCount;

        public RecommendedBookItem(Sach sach, String reason, long soldCount) {
            this.sach = sach;
            this.reason = reason;
            this.soldCount = soldCount;
        }

        public Sach getSach() {
            return sach;
        }

        public String getReason() {
            return reason;
        }

        public long getSoldCount() {
            return soldCount;
        }
    }

    @Autowired
    public RecommendationService(SachRepository sachRepository,
                                 ChiTietDonHangRepository chiTietDonHangRepository,
                                 DanhGiaRepository danhGiaRepository,
                                 SachService sachService) {
        this.sachRepository = sachRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.danhGiaRepository = danhGiaRepository;
        this.sachService = sachService;
    }

    /**
     * 1. Gợi ý "Người mua sách này cũng mua" (Collaborative Filtering từ CHITIETDONHANG + DANHGIA)
     */
    public List<RecommendedBookItem> getAlsoBoughtRecommendations(Long maSach, int limit) {
        Sach current = sachRepository.findById(maSach).orElse(null);
        if (current == null) return Collections.emptyList();

        Map<Long, Long> soldMap = sachService.getSoldQuantitiesMap();
        List<RecommendedBookItem> results = new ArrayList<>();
        Set<Long> addedIds = new LinkedHashSet<>();
        addedIds.add(maSach);

        // Bước 1: Tìm các sách được mua chung trong cùng đơn hàng (CHITIETDONHANG)
        List<Object[]> coPurchased = chiTietDonHangRepository.findAlsoBoughtBookIds(maSach);
        for (Object[] row : coPurchased) {
            if (results.size() >= limit) break;
            Long otherId = ((Number) row[0]).longValue();
            long sharedOrders = ((Number) row[1]).longValue();
            if (!addedIds.contains(otherId)) {
                sachRepository.findById(otherId).ifPresent(s -> {
                    if ("DANG_BAN".equalsIgnoreCase(s.getTrangThai()) && s.getSoLuong() != null && s.getSoLuong() > 0) {
                        results.add(new RecommendedBookItem(
                                s,
                                "Mua cùng trong " + sharedOrders + " đơn hàng",
                                soldMap.getOrDefault(s.getMaSach(), 0L)
                        ));
                        addedIds.add(s.getMaSach());
                    }
                });
            }
        }

        // Bước 2: Tìm các sách được những độc giả đã đánh giá cao cuốn sách này (>= 4 sao) yêu thích trong DANHGIA
        if (results.size() < limit) {
            List<DanhGia> currentReviews = danhGiaRepository.findBySach_MaSachAndTrangThaiOrderByNgayDanhGiaDesc(maSach, "HIEN_THI");
            Set<Long> fanCustomerIds = new HashSet<>();
            for (DanhGia dg : currentReviews) {
                if (dg.getSoSao() != null && dg.getSoSao() >= 4 && dg.getKhachHang() != null) {
                    fanCustomerIds.add(dg.getKhachHang().getMaKH());
                }
            }
            for (Long fanId : fanCustomerIds) {
                if (results.size() >= limit) break;
                List<Long> fanBoughtIds = chiTietDonHangRepository.findPurchasedBookIdsByCustomer(fanId);
                for (Long bId : fanBoughtIds) {
                    if (results.size() >= limit) break;
                    if (!addedIds.contains(bId)) {
                        sachRepository.findById(bId).ifPresent(s -> {
                            if ("DANG_BAN".equalsIgnoreCase(s.getTrangThai()) && s.getSoLuong() != null && s.getSoLuong() > 0) {
                                results.add(new RecommendedBookItem(
                                        s,
                                        "Độc giả yêu thích sách này cũng mua",
                                        soldMap.getOrDefault(s.getMaSach(), 0L)
                                ));
                                addedIds.add(s.getMaSach());
                            }
                        });
                    }
                }
            }
        }

        // Bước 3: Bổ sung các sách bán chạy / đánh giá cao cùng thể loại nếu dữ liệu mua chung chưa đủ limit
        if (results.size() < limit) {
            List<Sach> candidates = sachRepository.findByTrangThai("DANG_BAN").stream()
                    .filter(s -> !addedIds.contains(s.getMaSach()) && s.getSoLuong() != null && s.getSoLuong() > 0)
                    .sorted((a, b) -> Double.compare(
                            computeAiScore(b, current, soldMap),
                            computeAiScore(a, current, soldMap)
                    ))
                    .toList();

            for (Sach s : candidates) {
                if (results.size() >= limit) break;
                long sold = soldMap.getOrDefault(s.getMaSach(), 0L);
                String reason;
                if (current.getTacGia() != null && current.getTacGia().equalsIgnoreCase(s.getTacGia())) {
                    reason = "Cùng tác giả " + s.getTacGia();
                } else if (s.getSoLuotDanhGia() > 0 && s.getDiemDanhGia() >= 4.5) {
                    reason = "Đánh giá cao (" + s.getDiemDanhGia() + "★)";
                } else if (sold > 0) {
                    reason = "Bán chạy (" + sold + " cuốn đã bán)";
                } else {
                    reason = "Cùng chủ đề " + (s.getLoaiSach() != null ? s.getLoaiSach().getTenLoai() : "");
                }
                results.add(new RecommendedBookItem(s, reason, sold));
                addedIds.add(s.getMaSach());
            }
        }

        return results;
    }

    /**
     * 2. Gợi ý theo Cùng Tác giả & Cùng Thể loại (Content-Based Filtering) trên trang Chi tiết sách
     */
    public List<RecommendedBookItem> getRelatedByAuthorAndCategory(Long maSach, int limit) {
        Sach current = sachRepository.findById(maSach).orElse(null);
        if (current == null) return Collections.emptyList();

        Map<Long, Long> soldMap = sachService.getSoldQuantitiesMap();
        Long currentLoai = current.getLoaiSach() != null ? current.getLoaiSach().getMaLoai() : null;
        String currentAuthor = current.getTacGia() != null ? current.getTacGia().trim() : "";

        return sachRepository.findByTrangThai("DANG_BAN").stream()
                .filter(s -> !s.getMaSach().equals(maSach) && s.getSoLuong() != null && s.getSoLuong() > 0)
                .filter(s -> {
                    boolean sameAuthor = !currentAuthor.isEmpty() && s.getTacGia() != null && s.getTacGia().trim().equalsIgnoreCase(currentAuthor);
                    boolean sameCat = currentLoai != null && s.getLoaiSach() != null && currentLoai.equals(s.getLoaiSach().getMaLoai());
                    return sameAuthor || sameCat;
                })
                .sorted((a, b) -> Double.compare(
                        computeAiScore(b, current, soldMap),
                        computeAiScore(a, current, soldMap)
                ))
                .limit(limit)
                .map(s -> {
                    boolean sameAuthor = !currentAuthor.isEmpty() && s.getTacGia() != null && s.getTacGia().trim().equalsIgnoreCase(currentAuthor);
                    String reason = sameAuthor
                            ? "Cùng tác giả " + s.getTacGia()
                            : "Cùng thể loại " + (s.getLoaiSach() != null ? s.getLoaiSach().getTenLoai() : "");
                    return new RecommendedBookItem(s, reason, soldMap.getOrDefault(s.getMaSach(), 0L));
                })
                .toList();
    }

    /**
     * 3. Gợi ý cá nhân hóa trên Trang chủ ("Gợi ý AI dành riêng cho bạn"):
     * Phân tích lịch sử mua hàng (CHITIETDONHANG) của khách hàng đang đăng nhập (hoặc xu hướng chung nếu chưa đăng nhập).
     */
    public List<RecommendedBookItem> getPersonalizedRecommendations(KhachHang customer, int limit) {
        Map<Long, Long> soldMap = sachService.getSoldQuantitiesMap();
        List<RecommendedBookItem> results = new ArrayList<>();
        Set<Long> excludedIds = new HashSet<>();

        Set<Long> favoriteCategories = new LinkedHashSet<>();
        Set<String> favoriteAuthors = new LinkedHashSet<>();

        if (customer != null && customer.getMaKH() != null) {
            List<Long> purchasedIds = chiTietDonHangRepository.findPurchasedBookIdsByCustomer(customer.getMaKH());
            excludedIds.addAll(purchasedIds);

            for (Long bookId : purchasedIds) {
                sachRepository.findById(bookId).ifPresent(s -> {
                    if (s.getLoaiSach() != null) favoriteCategories.add(s.getLoaiSach().getMaLoai());
                    if (s.getTacGia() != null && !s.getTacGia().isBlank()) favoriteAuthors.add(s.getTacGia().trim().toLowerCase());
                });

                // Lấy sách mua chung từ CHITIETDONHANG với những sách khách này từng mua
                List<Object[]> alsoBought = chiTietDonHangRepository.findAlsoBoughtBookIds(bookId);
                for (Object[] row : alsoBought) {
                    if (results.size() >= limit) break;
                    Long candidateId = ((Number) row[0]).longValue();
                    if (!excludedIds.contains(candidateId)) {
                        sachRepository.findById(candidateId).ifPresent(s -> {
                            if ("DANG_BAN".equalsIgnoreCase(s.getTrangThai()) && s.getSoLuong() != null && s.getSoLuong() > 0) {
                                results.add(new RecommendedBookItem(
                                        s,
                                        "Gợi ý từ lịch sử mua hàng của bạn",
                                        soldMap.getOrDefault(s.getMaSach(), 0L)
                                ));
                                excludedIds.add(s.getMaSach());
                            }
                        });
                    }
                }
            }
        }

        if (results.size() < limit) {
            List<Sach> activeBooks = sachRepository.findByTrangThai("DANG_BAN").stream()
                    .filter(s -> !excludedIds.contains(s.getMaSach()) && s.getSoLuong() != null && s.getSoLuong() > 0)
                    .sorted((a, b) -> {
                        double scoreA = computePersonalizedScore(a, favoriteCategories, favoriteAuthors, soldMap);
                        double scoreB = computePersonalizedScore(b, favoriteCategories, favoriteAuthors, soldMap);
                        return Double.compare(scoreB, scoreA);
                    })
                    .toList();

            for (Sach s : activeBooks) {
                if (results.size() >= limit) break;
                long sold = soldMap.getOrDefault(s.getMaSach(), 0L);
                String reason;
                if (s.getTacGia() != null && favoriteAuthors.contains(s.getTacGia().trim().toLowerCase())) {
                    reason = "Tác giả bạn yêu thích: " + s.getTacGia();
                } else if (s.getLoaiSach() != null && favoriteCategories.contains(s.getLoaiSach().getMaLoai())) {
                    reason = "Hợp gu thể loại: " + s.getLoaiSach().getTenLoai();
                } else if (s.getSoLuotDanhGia() > 0 && s.getDiemDanhGia() >= 4.5) {
                    reason = "Độc giả đánh giá " + s.getDiemDanhGia() + "★";
                } else if (sold > 0) {
                    reason = "Xu hướng (" + sold + " cuốn đã bán)";
                } else {
                    reason = "AI đề xuất theo chủ đề nổi bật";
                }
                results.add(new RecommendedBookItem(s, reason, sold));
                excludedIds.add(s.getMaSach());
            }
        }

        return results;
    }

    private double computeAiScore(Sach candidate, Sach reference, Map<Long, Long> soldMap) {
        double score = 0.0;
        if (reference != null) {
            if (reference.getTacGia() != null && candidate.getTacGia() != null
                    && reference.getTacGia().trim().equalsIgnoreCase(candidate.getTacGia().trim())) {
                score += 50.0;
            }
            if (reference.getLoaiSach() != null && candidate.getLoaiSach() != null
                    && Objects.equals(reference.getLoaiSach().getMaLoai(), candidate.getLoaiSach().getMaLoai())) {
                score += 30.0;
            }
        }
        score += candidate.getDiemDanhGia() * 10.0;
        score += candidate.getSoLuotDanhGia() * 4.0;
        score += soldMap.getOrDefault(candidate.getMaSach(), 0L) * 6.0;
        return score;
    }

    private double computePersonalizedScore(Sach candidate, Set<Long> favCats, Set<String> favAuthors, Map<Long, Long> soldMap) {
        double score = 0.0;
        if (candidate.getTacGia() != null && favAuthors.contains(candidate.getTacGia().trim().toLowerCase())) {
            score += 45.0;
        }
        if (candidate.getLoaiSach() != null && favCats.contains(candidate.getLoaiSach().getMaLoai())) {
            score += 30.0;
        }
        score += candidate.getDiemDanhGia() * 12.0;
        score += candidate.getSoLuotDanhGia() * 5.0;
        score += soldMap.getOrDefault(candidate.getMaSach(), 0L) * 8.0;
        return score;
    }
}
