package com.bookmind.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bookmind.dto.DanhGiaRequest;
import com.bookmind.entity.DanhGia;
import com.bookmind.entity.KhachHang;
import com.bookmind.entity.LoaiSach;
import com.bookmind.entity.MaGiamGia;
import com.bookmind.entity.Sach;
import com.bookmind.security.AuthInterceptor;
import com.bookmind.service.DanhGiaService;
import com.bookmind.service.LoaiSachService;
import com.bookmind.service.MaGiamGiaService;
import com.bookmind.service.RecommendationService;
import com.bookmind.service.SachService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import com.bookmind.service.SavedCouponService;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class ShopController {

    private final SachService sachService;
    private final LoaiSachService loaiSachService;
    private final DanhGiaService danhGiaService;
    private final MaGiamGiaService maGiamGiaService;
    private final SavedCouponService savedCouponService;
    private final RecommendationService recommendationService;

    @Autowired
    public ShopController(SachService sachService,
                          LoaiSachService loaiSachService,
                          DanhGiaService danhGiaService,
                          MaGiamGiaService maGiamGiaService,
                          SavedCouponService savedCouponService,
                          RecommendationService recommendationService) {
        this.sachService = sachService;
        this.loaiSachService = loaiSachService;
        this.danhGiaService = danhGiaService;
        this.maGiamGiaService = maGiamGiaService;
        this.savedCouponService = savedCouponService;
        this.recommendationService = recommendationService;
    }

    @GetMapping({"/", "/shop"})
    public String index(Model model, HttpSession session) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        List<LoaiSach> categories = loaiSachService.getAllActive();
        List<Sach> flashSaleBooks = sachService.getFlashSaleBooks(6);
        List<Sach> newReleases = sachService.getNewReleases(8);
        List<Sach> kidsBestSellers = sachService.getBestSellersForKids(8);
        List<Sach> featuredBooks = sachService.getTopActiveBooks(8);
        List<MaGiamGia> coupons = maGiamGiaService.getAllActive();
        java.util.Map<Long, Long> soldQuantitiesMap = sachService.getSoldQuantitiesMap();
        List<RecommendationService.RecommendedBookItem> aiRecommendations =
                recommendationService.getPersonalizedRecommendations(currentCustomer, 4);

        model.addAttribute("categories", categories);
        model.addAttribute("flashSaleBooks", flashSaleBooks);
        model.addAttribute("newReleases", newReleases);
        model.addAttribute("kidsBestSellers", kidsBestSellers);
        model.addAttribute("featuredBooks", featuredBooks);
        model.addAttribute("coupons", coupons);
        model.addAttribute("savedCouponCodes", savedCouponService.getSavedCodes(session));
        model.addAttribute("soldQuantitiesMap", soldQuantitiesMap);
        model.addAttribute("aiRecommendations", aiRecommendations);
        return "shop/index";
    }

    @GetMapping("/api/books/suggest")
    @ResponseBody
    public List<java.util.Map<String, Object>> suggestBooks(@RequestParam(value = "keyword", required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        Page<Sach> bookPage = sachService.getBooksForShop(null, keyword.trim(), 0, 5);
        return bookPage.getContent().stream().map(s -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("maSach", s.getMaSach());
            map.put("tenSach", s.getTenSach());
            map.put("tacGia", s.getTacGia());
            map.put("giaBan", s.getGiaBan());
            map.put("hinhAnh", s.getHinhAnh());
            return map;
        }).toList();
    }

    @GetMapping("/shop/books")
    public String listBooks(@RequestParam(value = "maLoai", required = false) Long maLoai,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "tacGia", required = false) String tacGia,
                            @RequestParam(value = "minPrice", required = false) java.math.BigDecimal minPrice,
                            @RequestParam(value = "maxPrice", required = false) java.math.BigDecimal maxPrice,
                            @RequestParam(value = "rating", required = false) Integer rating,
                            @RequestParam(value = "sort", required = false) String sort,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "12") int size,
                            Model model) {
        Page<Sach> bookPage = sachService.getFilteredBooks(maLoai, keyword, tacGia, minPrice, maxPrice, rating, sort, page, size);
        List<LoaiSach> categories = loaiSachService.getAllActive();
        List<String> topAuthors = sachService.getTopAuthors(150);

        model.addAttribute("bookPage", bookPage);
        model.addAttribute("categories", categories);
        model.addAttribute("topAuthors", topAuthors);
        model.addAttribute("currentLoai", maLoai);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedTacGia", tacGia);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("selectedRating", rating);
        model.addAttribute("selectedSort", sort);

        if (maLoai != null) {
            try {
                model.addAttribute("selectedCategory", loaiSachService.getById(maLoai));
            } catch (Exception ignored) {}
        }
        return "shop/book-list";
    }

    @GetMapping("/shop/book/{id}")
    public String bookDetail(@PathVariable("id") Long id, Model model, HttpSession session) {
        Sach sach = sachService.getSachById(id);
        List<DanhGia> reviews = danhGiaService.getRatingsForBook(id);
        Double avgRating = danhGiaService.getAverageRating(id);
        long totalReviews = danhGiaService.countRatings(id);

        model.addAttribute("sach", sach);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("totalReviews", totalReviews);
        model.addAttribute("soldCount", sachService.getSoldQuantitiesMap().getOrDefault(id, 0L));
        model.addAttribute("alsoBoughtBooks", recommendationService.getAlsoBoughtRecommendations(id, 4));
        model.addAttribute("relatedBooks", recommendationService.getRelatedByAuthorAndCategory(id, 4));

        // Check if user has already reviewed
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        DanhGiaRequest reviewForm = new DanhGiaRequest();
        reviewForm.setMaSach(id);
        reviewForm.setSoSao(5);

        if (currentCustomer != null) {
            danhGiaService.getUserRatingForBook(currentCustomer, id)
                    .ifPresent(existingRating -> {
                        model.addAttribute("userReview", existingRating);
                        reviewForm.setSoSao(existingRating.getSoSao());
                        reviewForm.setNoiDung(existingRating.getNoiDung());
                    });
        }

        model.addAttribute("reviewForm", reviewForm);

        return "shop/book-detail";
    }

    @PostMapping("/shop/book/{id}/rate")
    public String submitReview(@PathVariable("id") Long id,
                               @Valid @ModelAttribute("reviewForm") DanhGiaRequest form,
                               BindingResult bindingResult,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        if (currentCustomer == null) {
            return "redirect:/login?error=login_required&redirect=/shop/book/" + id;
        }

        form.setMaSach(id);

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg != null ? errorMsg : "Thông tin đánh giá không hợp lệ!");
            return "redirect:/shop/book/" + id;
        }

        try {
            danhGiaService.submitRating(currentCustomer, form);
            redirectAttributes.addFlashAttribute("successMessage", "Cảm ơn bạn đã gửi đánh giá cuốn sách này!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/shop/book/" + id;
    }
}
