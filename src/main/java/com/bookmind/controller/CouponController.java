package com.bookmind.controller;

import com.bookmind.service.SavedCouponService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** API lưu / bỏ lưu mã giảm giá vào ví của phiên người dùng. */
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final SavedCouponService savedCouponService;

    @org.springframework.beans.factory.annotation.Autowired
    public CouponController(SavedCouponService savedCouponService) {
        this.savedCouponService = savedCouponService;
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> save(@RequestParam("code") String code, HttpSession session) {
        try {
            savedCouponService.save(session, code);
            return ResponseEntity.ok(Map.of("saved", true, "code", code.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("saved", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/remove")
    public ResponseEntity<Map<String, Object>> remove(@RequestParam("code") String code, HttpSession session) {
        savedCouponService.remove(session, code);
        return ResponseEntity.ok(Map.of("saved", false, "code", code.trim().toUpperCase()));
    }
}
