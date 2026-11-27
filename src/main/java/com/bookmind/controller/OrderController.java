package com.bookmind.controller;

import com.bookmind.dto.CheckoutRequest;
import com.bookmind.dto.DanhGiaRequest;
import com.bookmind.entity.*;
import com.bookmind.repository.ChiTietDonHangRepository;
import com.bookmind.repository.DiaChiRepository;
import com.bookmind.repository.ThanhToanRepository;
import com.bookmind.security.AuthInterceptor;
import com.bookmind.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    private final DonHangService donHangService;
    private final GioHangService gioHangService;
    private final ThanhToanService thanhToanService;
    private final DiaChiRepository diaChiRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final SavedCouponService savedCouponService;
    private final MoMoService moMoService;
    private final PayOSService payOSService;
    private final DanhGiaService danhGiaService;

    @Autowired
    public OrderController(DonHangService donHangService,
                           GioHangService gioHangService,
                           ThanhToanService thanhToanService,
                           DiaChiRepository diaChiRepository,
                           ThanhToanRepository thanhToanRepository,
                           ChiTietDonHangRepository chiTietDonHangRepository,
                           SavedCouponService savedCouponService,
                           MoMoService moMoService,
                           PayOSService payOSService,
                           DanhGiaService danhGiaService) {
        this.donHangService = donHangService;
        this.gioHangService = gioHangService;
        this.thanhToanService = thanhToanService;
        this.diaChiRepository = diaChiRepository;
        this.thanhToanRepository = thanhToanRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.savedCouponService = savedCouponService;
        this.moMoService = moMoService;
        this.payOSService = payOSService;
        this.danhGiaService = danhGiaService;
    }

    @GetMapping("/shop/checkout")
    public String showCheckoutPage(@RequestParam(value = "items", required = false) List<Long> selectedItems,
                                   @RequestParam(value = "coupon", required = false) String couponCode,
                                   HttpSession session, Model model) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        if (currentCustomer == null) {
            return "redirect:/login?error=login_required&redirect=/shop/cart";
        }

        List<ChiTietGioHang> cartItems = gioHangService.getSelectedItems(currentCustomer, selectedItems);
        if (cartItems.isEmpty()) {
            return "redirect:/shop/cart";
        }

        List<PtthThanhToan> paymentMethods = thanhToanService.getAllActiveMethods();
        List<DiaChi> savedAddresses = diaChiRepository.findByKhachHang_MaKH(currentCustomer.getMaKH());

        CheckoutRequest form = new CheckoutRequest();
        form.setHoTenNguoiNhan(currentCustomer.getHoTen());
        form.setSdtNguoiNhan(currentCustomer.getSdt() != null ? currentCustomer.getSdt() : "");
        if (!savedAddresses.isEmpty()) {
            DiaChi defaultAddr = savedAddresses.stream()
                    .filter(d -> Boolean.TRUE.equals(d.getMacDinh()))
                    .findFirst()
                    .orElse(savedAddresses.get(0));
            if (defaultAddr.getHoTenNguoiNhan() != null && !defaultAddr.getHoTenNguoiNhan().isBlank()) {
                form.setHoTenNguoiNhan(defaultAddr.getHoTenNguoiNhan());
            }
            if (defaultAddr.getSdtNguoiNhan() != null && !defaultAddr.getSdtNguoiNhan().isBlank()) {
                form.setSdtNguoiNhan(defaultAddr.getSdtNguoiNhan());
            }
            form.setDiaChiNhan(defaultAddr.getDiaChiNhan());
        }
        if (!paymentMethods.isEmpty()) {
            PtthThanhToan defaultPt = paymentMethods.stream()
                    .filter(p -> "VietQR".equalsIgnoreCase(p.getTenPTTT()))
                    .findFirst()
                    .orElse(paymentMethods.get(0));
            form.setMaPTTT(defaultPt.getMaPTTT());
        }
        form.setSelectedItems(cartItems.stream().map(it -> it.getSach().getMaSach()).toList());
        if (couponCode != null && savedCouponService.isSaved(session, couponCode)) {
            form.setMaGiamGiaCode(couponCode.trim().toUpperCase());
        }

        model.addAttribute("checkoutRequest", form);
        populateCheckoutModel(model, session, cartItems, paymentMethods, savedAddresses);
        return "shop/checkout";
    }

    @PostMapping("/shop/checkout")
    public String processCheckout(@Valid @ModelAttribute("checkoutRequest") CheckoutRequest req,
                                  BindingResult bindingResult,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        if (currentCustomer == null) {
            return "redirect:/login?error=login_required&redirect=/shop/cart";
        }

        if (bindingResult.hasErrors()) {
            List<ChiTietGioHang> cartItems = gioHangService.getSelectedItems(currentCustomer, req.getSelectedItems());
            populateCheckoutModel(model, session, cartItems, thanhToanService.getAllActiveMethods(),
                    diaChiRepository.findByKhachHang_MaKH(currentCustomer.getMaKH()));
            return "shop/checkout";
        }

        String code = req.getMaGiamGiaCode();
        boolean hasCoupon = code != null && !code.trim().isEmpty();
        try {
            // Chỉ cho phép dùng mã đã được người dùng bấm "Lưu mã"
            if (hasCoupon && !savedCouponService.isSaved(session, code)) {
                throw new RuntimeException("Mã giảm giá '" + code.trim().toUpperCase() + "' chưa được lưu vào ví của bạn");
            }
            DonHang donHang = donHangService.createOrder(currentCustomer, req);
            session.removeAttribute(com.bookmind.config.GlobalModelAdvice.SESSION_CART_COUNT);
            if (hasCoupon) {
                savedCouponService.remove(session, code); // mã đã dùng thì rời khỏi ví
            }
            PtthThanhToan pttt = thanhToanService.getMethodById(req.getMaPTTT());
            boolean isOnline = pttt != null && (thanhToanService.isVietQrMethod(pttt.getTenPTTT()) || thanhToanService.isMoMoMethod(pttt.getTenPTTT()));
            if (isOnline) {
                return "redirect:/shop/order/" + donHang.getMaDH() + "/payment";
            }
            redirectAttributes.addFlashAttribute("successMessage", "Đặt hàng thành công! Mã đơn hàng: #" + donHang.getMaDH());
            return "redirect:/shop/order/" + donHang.getMaDH();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            StringBuilder url = new StringBuilder("redirect:/shop/checkout?");
            if (req.getSelectedItems() != null) {
                req.getSelectedItems().forEach(id -> url.append("items=").append(id).append('&'));
            }
            return url.toString();
        }
    }

    private void populateCheckoutModel(Model model, HttpSession session, List<ChiTietGioHang> cartItems,
                                        List<PtthThanhToan> paymentMethods, List<DiaChi> savedAddresses) {
        BigDecimal subtotal = gioHangService.calculateSubtotal(cartItems);
        model.addAttribute("items", cartItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("paymentMethods", paymentMethods);
        model.addAttribute("savedAddresses", savedAddresses);
        model.addAttribute("savedCoupons", savedCouponService.getSavedUsableCoupons(session));
    }

    @GetMapping("/shop/my-orders")
    public String myOrders(@RequestParam(value = "page", defaultValue = "0") int page,
                           @RequestParam(value = "size", defaultValue = "10") int size,
                           HttpSession session,
                           Model model) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        if (currentCustomer == null) {
            return "redirect:/login?error=login_required&redirect=/shop/my-orders";
        }

        Page<DonHang> orderPage = donHangService.getCustomerOrders(currentCustomer, page, size);
        model.addAttribute("orderPage", orderPage);
        return "shop/my-orders";
    }

    @GetMapping("/shop/order/{id}")
    public String orderDetail(@PathVariable("id") Long id,
                              @RequestParam(value = "code", required = false) String payOsCode,
                              @RequestParam(value = "status", required = false) String payOsStatus,
                              @RequestParam(value = "cancel", required = false) String payOsCancel,
                              HttpSession session,
                              Model model) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        DonHang order = donHangService.getOrderById(id);

        // Bảo vệ: Khách hàng chỉ xem đơn của mình (nếu không phải Admin)
        TaiKhoan user = (TaiKhoan) session.getAttribute(AuthInterceptor.SESSION_USER);
        if (user != null && !"ADMIN".equalsIgnoreCase(user.getVaiTro())) {
            if (currentCustomer == null || !order.getKhachHang().getMaKH().equals(currentCustomer.getMaKH())) {
                return "redirect:/shop/my-orders";
            }
        }

        // Tự động xác nhận thực tế khi PayOS chuyển hướng khách hàng về sau khi thanh toán thành công
        // Luôn xác minh thực tế với máy chủ PayOS nếu PayOS đã cấu hình (tránh giả mạo query string)
        if (("00".equals(payOsCode) || "PAID".equalsIgnoreCase(payOsStatus)) && !"true".equalsIgnoreCase(payOsCancel)) {
            if (!payOSService.isConfigured() || payOSService.isPaymentPaid(id)) {
                List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDHOrderByMaThanhToanDesc(id);
                if (!payments.isEmpty()) {
                    thanhToanService.markPaymentSuccess(payments.get(0).getMaThanhToan(), "PAYOS-" + id);
                }
                if ("CHO_XAC_NHAN".equals(order.getTrangThai())) {
                    donHangService.updateOrderStatus(id, "DA_XAC_NHAN");
                }
                return "redirect:/shop/order/" + id + "/invoice";
            }
        }

        List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDHOrderByMaThanhToanDesc(id);
        ThanhToan currentPayment = payments.isEmpty() ? null : payments.get(0);

        // Đồng bộ thanh toán nếu đơn hàng đã giao thành công
        if (currentPayment != null && !"THANH_CONG".equalsIgnoreCase(currentPayment.getTrangThai())
                && "DA_GIAO".equalsIgnoreCase(order.getTrangThai())) {
            currentPayment.setTrangThai("THANH_CONG");
            if (currentPayment.getThoiGianTT() == null) {
                currentPayment.setThoiGianTT(java.time.LocalDateTime.now());
            }
            thanhToanRepository.save(currentPayment);
        }

        String payOsCheckoutUrl = null;
        String momoPayUrl = null;
        String paymentQrUrl = null;
        if (currentPayment != null) {
            payOsCheckoutUrl = thanhToanService.getPayOsCheckoutUrl(order);
            momoPayUrl = thanhToanService.getMoMoPayUrl(order);
            if (currentPayment.getPtthThanhToan() != null) {
                paymentQrUrl = thanhToanService.generatePaymentQrUrl(order, currentPayment.getPtthThanhToan());
            }
        }

        boolean isCancelled = "true".equalsIgnoreCase(payOsCancel) || "CANCELLED".equalsIgnoreCase(payOsStatus);
        String ptttName = currentPayment != null && currentPayment.getPtthThanhToan() != null 
                ? currentPayment.getPtthThanhToan().getTenPTTT() : "";
        boolean isVietQr = thanhToanService.isVietQrMethod(ptttName);
        boolean isMoMo = thanhToanService.isMoMoMethod(ptttName);
        boolean isCod = thanhToanService.isCodMethod(ptttName);

        model.addAttribute("order", order);
        model.addAttribute("payment", currentPayment);
        model.addAttribute("payOsCheckoutUrl", payOsCheckoutUrl);
        model.addAttribute("momoPayUrl", momoPayUrl);
        model.addAttribute("paymentQrUrl", paymentQrUrl);
        model.addAttribute("isCancelled", isCancelled);
        model.addAttribute("isVietQr", isVietQr);
        model.addAttribute("isMoMo", isMoMo);
        model.addAttribute("isCod", isCod);
        model.addAttribute("paymentMethods", thanhToanService.getAllActiveMethods());

        // Lấy danh sách đánh giá của khách hàng cho từng cuốn sách trong đơn hàng này
        Map<Long, DanhGia> userRatingsMap = new HashMap<>();
        if (order != null && order.getKhachHang() != null && order.getChiTietList() != null) {
            for (ChiTietDonHang ct : order.getChiTietList()) {
                if (ct != null && ct.getSach() != null) {
                    danhGiaService.getUserRatingForBook(order.getKhachHang(), ct.getSach().getMaSach())
                            .ifPresent(dg -> userRatingsMap.put(ct.getSach().getMaSach(), dg));
                }
            }
        }
        model.addAttribute("userRatingsMap", userRatingsMap);

        return "shop/order-detail";
    }

    /**
     * Khách hàng đánh giá sách trực tiếp từ màn hình Chi tiết đơn hàng
     */
    @PostMapping("/shop/order/{orderId}/rate-book/{bookId}")
    public String rateBookFromOrder(@PathVariable("orderId") Long orderId,
                                    @PathVariable("bookId") Long bookId,
                                    @RequestParam("soSao") Integer soSao,
                                    @RequestParam("noiDung") String noiDung,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        if (currentCustomer == null) {
            return "redirect:/login?error=login_required&redirect=/shop/order/" + orderId;
        }

        DonHang order = donHangService.getOrderById(orderId);
        if (order == null || !"DA_GIAO".equalsIgnoreCase(order.getTrangThai())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ những đơn hàng đã giao thành công mới được đánh giá!");
            return "redirect:/shop/order/" + orderId;
        }

        if (soSao == null || soSao < 1 || soSao > 5) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số sao đánh giá phải từ 1 đến 5 sao!");
            return "redirect:/shop/order/" + orderId;
        }
        if (noiDung == null || noiDung.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập cảm nhận/nhận xét về cuốn sách!");
            return "redirect:/shop/order/" + orderId;
        }

        try {
            DanhGiaRequest req = new DanhGiaRequest();
            req.setMaSach(bookId);
            req.setSoSao(soSao);
            req.setNoiDung(noiDung.trim());
            danhGiaService.submitRating(currentCustomer, orderId, req);
            redirectAttributes.addFlashAttribute("successMessage", "Đánh giá của bạn về cuốn sách đã được lưu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/shop/order/" + orderId;
    }

    /**
     * Endpoint Return URL MoMo chuyển hướng khách hàng về sau khi thanh toán hoặc hủy
     */
    @GetMapping("/shop/order/momo-return")
    public String momoReturn(@RequestParam Map<String, String> allParams,
                             RedirectAttributes redirectAttributes) {
        String orderIdStr = allParams.get("orderId");
        Long orderId = moMoService.extractOrderId(orderIdStr);
        String resultCode = allParams.get("resultCode");
        String message = allParams.get("message");
        String transId = allParams.get("transId");

        if (orderId == null) {
            return "redirect:/shop/my-orders";
        }

        boolean isValid = moMoService.verifySignature(allParams);
        if ("0".equals(resultCode) && isValid) {
            List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDHOrderByMaThanhToanDesc(orderId);
            if (!payments.isEmpty()) {
                thanhToanService.markPaymentSuccess(payments.get(0).getMaThanhToan(), transId != null ? transId : "MOMO-" + orderId);
            }
            DonHang order = donHangService.getOrderById(orderId);
            if ("CHO_XAC_NHAN".equals(order.getTrangThai())) {
                donHangService.updateOrderStatus(orderId, "DA_XAC_NHAN");
            }
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thanh toán thành công qua Cổng MoMo! Đơn hàng #" + orderId + " đã được xác nhận thực tế.");
            return "redirect:/shop/order/" + orderId + "/invoice";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Giao dịch qua MoMo chưa hoàn tất hoặc bạn đã hủy (" + (message != null ? message : "Chưa hoàn tất") + ").");
            return "redirect:/shop/order/" + orderId + "?cancel=true&status=CANCELLED";
        }
    }

    @PostMapping("/shop/order/{id}/change-payment-method")
    public String changePaymentMethod(@PathVariable("id") Long id,
                                      @RequestParam("maPTTT") Long maPTTT,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        DonHang order = donHangService.getOrderById(id);
        TaiKhoan user = (TaiKhoan) session.getAttribute(AuthInterceptor.SESSION_USER);
        if (user != null && !"ADMIN".equalsIgnoreCase(user.getVaiTro())) {
            if (currentCustomer == null || !order.getKhachHang().getMaKH().equals(currentCustomer.getMaKH())) {
                return "redirect:/shop/my-orders";
            }
        }

        try {
            PtthThanhToan newMethod = thanhToanService.getMethodById(maPTTT);
            thanhToanService.updatePaymentMethod(order, maPTTT);

            String methodName = newMethod.getTenPTTT();
            boolean isOnline = thanhToanService.isVietQrMethod(methodName) || thanhToanService.isMoMoMethod(methodName);
            donHangService.updatePaymentExpiration(id, isOnline);

            // Đổi phương thức thành công
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã đổi sang phương thức: " + methodName + " thành công!");

            if (isOnline) {
                // Chuyển thẳng sang trang quét mã QR thanh toán độc lập
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã đổi sang phương thức: " + methodName + " thành công! Vui lòng quét mã QR để thanh toán.");
                return "redirect:/shop/order/" + id + "/payment";
            } else {
                // Phương thức tiền mặt (COD): tự động xác nhận đơn hàng thành công, không cần quét QR hay xác minh
                donHangService.updateOrderStatus(id, "DA_XAC_NHAN");

                redirectAttributes.addFlashAttribute("successMessage",
                        "Thanh toán bằng Tiền mặt (COD) thành công! Đơn hàng #" + id + " đã được xác nhận.");
                return "redirect:/shop/order/" + id;
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể đổi phương thức: " + e.getMessage());
            return "redirect:/shop/order/" + id;
        }
    }

    @GetMapping("/shop/order/{id}/change-payment-method")
    public String handleGetChangePaymentMethod(@PathVariable("id") Long id) {
        return "redirect:/shop/order/" + id;
    }

    @GetMapping("/shop/order/{id}/payment")
    public String showOrderPaymentPage(@PathVariable("id") Long id,
                                       HttpSession session,
                                       Model model) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        DonHang order = donHangService.getOrderById(id);

        TaiKhoan user = (TaiKhoan) session.getAttribute(AuthInterceptor.SESSION_USER);
        if (user != null && !"ADMIN".equalsIgnoreCase(user.getVaiTro())) {
            if (currentCustomer == null || !order.getKhachHang().getMaKH().equals(currentCustomer.getMaKH())) {
                return "redirect:/shop/my-orders";
            }
        }

        List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDHOrderByMaThanhToanDesc(id);
        ThanhToan currentPayment = payments.isEmpty() ? null : payments.get(0);

        // Nếu đã thanh toán thành công rồi hoặc đơn hàng đã giao, chuyển thẳng sang màn hình hóa đơn
        if ((currentPayment != null && "THANH_CONG".equalsIgnoreCase(currentPayment.getTrangThai()))
                || "DA_GIAO".equalsIgnoreCase(order.getTrangThai())) {
            return "redirect:/shop/order/" + id + "/invoice";
        }

        String ptttName = currentPayment != null && currentPayment.getPtthThanhToan() != null 
                ? currentPayment.getPtthThanhToan().getTenPTTT() : "";
        boolean isVietQr = thanhToanService.isVietQrMethod(ptttName);
        boolean isMoMo = thanhToanService.isMoMoMethod(ptttName);
        boolean isCod = thanhToanService.isCodMethod(ptttName);

        // Đơn hàng COD thì không có mã QR, chuyển thẳng về trang chi tiết đơn
        if (isCod) {
            return "redirect:/shop/order/" + id;
        }

        String paymentQrUrl = null;
        String momoPayUrl = null;
        if (currentPayment != null && currentPayment.getPtthThanhToan() != null) {
            paymentQrUrl = thanhToanService.generatePaymentQrUrl(order, currentPayment.getPtthThanhToan());
            if (isMoMo) {
                momoPayUrl = thanhToanService.getMoMoPayUrl(order);
            }
        }

        model.addAttribute("order", order);
        model.addAttribute("payment", currentPayment);
        model.addAttribute("paymentQrUrl", paymentQrUrl);
        model.addAttribute("momoPayUrl", momoPayUrl);
        model.addAttribute("isVietQr", isVietQr);
        model.addAttribute("isMoMo", isMoMo);
        model.addAttribute("paymentMethods", thanhToanService.getAllActiveMethods());
        return "shop/payment";
    }

    @GetMapping("/shop/order/{id}/invoice")
    public String showOrderInvoice(@PathVariable("id") Long id,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        DonHang order = donHangService.getOrderById(id);

        TaiKhoan user = (TaiKhoan) session.getAttribute(AuthInterceptor.SESSION_USER);
        if (user != null && !"ADMIN".equalsIgnoreCase(user.getVaiTro())) {
            if (currentCustomer == null || !order.getKhachHang().getMaKH().equals(currentCustomer.getMaKH())) {
                return "redirect:/shop/my-orders";
            }
        }

        // Đơn hàng đã hủy thì không có hóa đơn
        if ("DA_HUY".equalsIgnoreCase(order.getTrangThai())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đơn hàng #" + id + " đã bị hủy nên không có hóa đơn.");
            return "redirect:/shop/order/" + id;
        }

        List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDHOrderByMaThanhToanDesc(id);
        ThanhToan currentPayment = payments.isEmpty() ? null : payments.get(0);

        if (currentPayment != null && !"THANH_CONG".equalsIgnoreCase(currentPayment.getTrangThai())
                && ("DA_GIAO".equalsIgnoreCase(order.getTrangThai())
                    || "DA_XAC_NHAN".equalsIgnoreCase(order.getTrangThai())
                    || "DANG_GIAO".equalsIgnoreCase(order.getTrangThai()))) {
            currentPayment.setTrangThai("THANH_CONG");
            if (currentPayment.getThoiGianTT() == null) {
                currentPayment.setThoiGianTT(java.time.LocalDateTime.now());
            }
            thanhToanRepository.save(currentPayment);
        }

        String ptttName = currentPayment != null && currentPayment.getPtthThanhToan() != null 
                ? currentPayment.getPtthThanhToan().getTenPTTT() : "";
        boolean isVietQr = thanhToanService.isVietQrMethod(ptttName);
        boolean isMoMo = thanhToanService.isMoMoMethod(ptttName);
        boolean isCod = thanhToanService.isCodMethod(ptttName);

        List<ChiTietDonHang> items = chiTietDonHangRepository.findByDonHang_MaDH(id);

        model.addAttribute("order", order);
        model.addAttribute("items", items);
        model.addAttribute("payment", currentPayment);
        model.addAttribute("isVietQr", isVietQr);
        model.addAttribute("isMoMo", isMoMo);
        model.addAttribute("isCod", isCod);
        return "shop/invoice";
    }

    @RequestMapping(value = "/shop/order/{id}/cancel-and-return-to-cart", method = {RequestMethod.GET, RequestMethod.POST})
    public String cancelAndReturnToCart(@PathVariable("id") Long id,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        KhachHang currentCustomer = (KhachHang) session.getAttribute(AuthInterceptor.SESSION_CUSTOMER);
        if (currentCustomer == null) {
            return "redirect:/login?redirect=/shop/order/" + id;
        }
        DonHang order = donHangService.getOrderById(id);
        if (!order.getKhachHang().getMaKH().equals(currentCustomer.getMaKH())) {
            return "redirect:/shop/my-orders";
        }

        try {
            if ("CHO_XAC_NHAN".equals(order.getTrangThai())) {
                List<ChiTietDonHang> items = chiTietDonHangRepository.findByDonHang_MaDH(id);
                // Hủy đơn hàng và tự động hoàn lại tồn kho sách
                donHangService.updateOrderStatus(id, "DA_HUY");

                // Thêm lại các sách vào giỏ hàng của khách
                StringBuilder redirectUrl = new StringBuilder("/shop/checkout?");
                for (ChiTietDonHang item : items) {
                    try {
                        gioHangService.addToCart(currentCustomer, item.getSach().getMaSach(), item.getSoLuong());
                        redirectUrl.append("items=").append(item.getSach().getMaSach()).append('&');
                    } catch (Exception ignored) {}
                }
                session.removeAttribute(com.bookmind.config.GlobalModelAdvice.SESSION_CART_COUNT);
                redirectAttributes.addFlashAttribute("infoMessage", "Đã hủy đơn hàng #" + id + " và đưa sách trở lại giỏ hàng. Bạn có thể chọn phương thức thanh toán khác!");
                return "redirect:" + redirectUrl.toString();
            } else if ("DA_HUY".equals(order.getTrangThai())) {
                return "redirect:/shop/cart";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy đơn: " + e.getMessage());
        }
        return "redirect:/shop/order/" + id;
    }

    @GetMapping("/shop/order/{id}/confirm-payment")
    public String handleGetConfirmPayment(@PathVariable("id") Long id) {
        return "redirect:/shop/order/" + id + "/payment";
    }

    @PostMapping("/shop/order/{id}/confirm-payment")
    public String confirmPayment(@PathVariable("id") Long id,
                                 @RequestParam(value = "maGiaoDich", required = false) String maGiaoDich,
                                 RedirectAttributes redirectAttributes) {
        try {
            DonHang order = donHangService.getOrderById(id);
            List<ThanhToan> payments = thanhToanRepository.findByDonHang_MaDHOrderByMaThanhToanDesc(id);
            if (payments.isEmpty() || payments.get(0) == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin giao dịch thanh toán cho đơn hàng #" + id);
                return "redirect:/shop/order/" + id;
            }
            ThanhToan currentPayment = payments.get(0);
            String ptttName = currentPayment.getPtthThanhToan() != null 
                    ? currentPayment.getPtthThanhToan().getTenPTTT() : "";

            if (thanhToanService.isVietQrMethod(ptttName)) {
                // Kiểm tra thực tế xem tiền đã vào tài khoản BIDV qua cổng PayOS chưa
                boolean isPaid = payOSService.isConfigured() && payOSService.isPaymentPaid(id);
                if (isPaid) {
                    thanhToanService.markPaymentSuccess(currentPayment.getMaThanhToan(), "PAYOS-" + id);
                    if ("CHO_XAC_NHAN".equals(order.getTrangThai())) {
                        donHangService.updateOrderStatus(id, "DA_XAC_NHAN");
                    }
                    redirectAttributes.addFlashAttribute("successMessage", "Xác nhận đã thanh toán thành công qua VietQR BIDV!");
                    return "redirect:/shop/order/" + id;
                } else if (maGiaoDich != null && !maGiaoDich.trim().isEmpty()) {
                    thanhToanService.markPaymentSuccess(currentPayment.getMaThanhToan(), maGiaoDich.trim());
                    if ("CHO_XAC_NHAN".equals(order.getTrangThai())) {
                        donHangService.updateOrderStatus(id, "DA_XAC_NHAN");
                    }
                    redirectAttributes.addFlashAttribute("successMessage", 
                            "Đã ghi nhận thông tin chuyển khoản BIDV (mã GD: " + maGiaoDich.trim() + ") thành công! Đơn hàng #" + id + " đã được xác nhận.");
                    return "redirect:/shop/order/" + id;
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                            "Hệ thống chưa ghi nhận giao dịch chuyển khoản cho đơn hàng #" + id + ". Quý khách vui lòng quét mã QR chuyển khoản và chờ trong giây lát (hoặc nhập Mã tham chiếu ngân hàng vào ô trên).");
                    return "redirect:/shop/order/" + id;
                }
            } else if (thanhToanService.isMoMoMethod(ptttName)) {
                if (maGiaoDich != null && !maGiaoDich.trim().isEmpty()) {
                    thanhToanService.markPaymentSuccess(currentPayment.getMaThanhToan(), maGiaoDich.trim());
                    if ("CHO_XAC_NHAN".equals(order.getTrangThai())) {
                        donHangService.updateOrderStatus(id, "DA_XAC_NHAN");
                    }
                    redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận thanh toán MoMo mã #" + maGiaoDich.trim() + " thành công!");
                    return "redirect:/shop/order/" + id;
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                            "Vui lòng quét mã QR chuyển tiền qua ví MoMo và nhập Mã giao dịch để hoàn tất.");
                    return "redirect:/shop/order/" + id;
                }
            } else {
                redirectAttributes.addFlashAttribute("infoMessage", 
                        "Đơn hàng chọn hình thức Thanh toán khi nhận hàng (COD). Quý khách sẽ thanh toán tiền mặt cho shipper khi nhận sách.");
                return "redirect:/shop/order/" + id;
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xác nhận thanh toán: " + e.getMessage());
            return "redirect:/shop/order/" + id;
        }
    }
}
