/**
 * Coupon picker: tính điều kiện, sắp xếp tối ưu & gắn nhãn "Ưu tiên" cho mã giảm giá hời nhất theo đơn hàng.
 * Đồng bộ hoàn toàn với MaGiamGiaService.calculateDiscount và DonHangService.calculateShippingFee:
 * - Miễn phí vận chuyển (0đ) khi đơn hàng >= 300.000đ HOẶC dùng mã FREESHIP.
 * - Phí vận chuyển theo khu vực: Nội thành Hà Nội / TP.HCM (15.000đ), Tỉnh/TP tiêu chuẩn (25.000đ), Tuyến xa (35.000đ).
 */
(function (global) {
    const SHIPPING_FEE = 25000;
    const FREE_SHIPPING_THRESHOLD = 300000;
    const FEE_METRO = 15000;
    const FEE_STANDARD = 25000;
    const FEE_REMOTE = 35000;

    function formatVnd(n) {
        return Math.round(n).toLocaleString('vi-VN') + ' đ';
    }

    function stripVietnamese(str) {
        if (!str) return '';
        return str.toString().normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .replace(/đ/g, 'd').replace(/Đ/g, 'd')
            .toLowerCase().trim();
    }

    /**
     * Tính phí vận chuyển cơ bản theo giá trị đơn hàng & địa chỉ khu vực (chưa tính mã FREESHIP).
     */
    function calculateBaseShipping(subtotal, address) {
        if (subtotal >= FREE_SHIPPING_THRESHOLD) {
            return { fee: 0, regionLabel: 'Miễn phí (Đơn ≥ 300.000đ)' };
        }
        if (!address || !address.trim()) {
            return { fee: FEE_STANDARD, regionLabel: 'Tiêu chuẩn (Miễn phí đơn từ 300K)' };
        }
        const norm = stripVietnamese(address);
        if (norm.includes('ha noi') || norm.includes('hanoi') || /\bhn\b/.test(norm) ||
            norm.includes('ho chi minh') || norm.includes('tp.hcm') || norm.includes('tphcm') ||
            norm.includes('tp hcm') || norm.includes('sai gon') || norm.includes('saigon') || /\bhcm\b/.test(norm)) {
            return { fee: FEE_METRO, regionLabel: 'Nội thành HN / TP.HCM' };
        }
        const standardRegions = [
            'da nang', 'hai phong', 'can tho', 'binh duong', 'dong nai', 'long an',
            'ba ria', 'vung tau', 'bac ninh', 'hung yen', 'hai duong', 'vinh phuc',
            'ha nam', 'nam dinh', 'thai binh', 'quang ninh', 'thua thien hue', 'hue',
            'khanh hoa', 'nha trang', 'nghe an', 'thanh hoa', 'bac giang', 'thai nguyen',
            'phu tho', 'ninh binh', 'tien giang', 'ben tre', 'vinh long'
        ];
        for (const r of standardRegions) {
            if (norm.includes(r)) {
                return { fee: FEE_STANDARD, regionLabel: 'Khu vực Liên vùng' };
            }
        }
        return { fee: FEE_REMOTE, regionLabel: 'Tuyến tỉnh xa' };
    }

    function couponDiscount(input, subtotal, baseShippingFee) {
        const percent = parseFloat(input.dataset.percent || '0');
        const max = parseFloat(input.dataset.max || '0');
        const min = parseFloat(input.dataset.min || '0');
        const codeUpper = input.value.toUpperCase();
        const isFreeship = codeUpper === 'FREESHIP' || codeUpper.includes('SHIP');

        if (subtotal <= 0 || subtotal < min) {
            return {
                eligible: false,
                missing: Math.max(min - subtotal, 0),
                discount: 0,
                freeship: false,
                totalBenefit: 0,
                minOrder: min
            };
        }
        let discount = 0;
        if (percent > 0) {
            discount = subtotal * percent / 100;
            if (max > 0 && discount > max) discount = max;
        } else {
            discount = max;
        }
        discount = Math.min(discount, subtotal);
        // Lợi ích thực tế = tiền giảm trực tiếp + phí ship tiết kiệm được (nếu đơn chưa được freeship tự động)
        const shippingSaved = isFreeship ? baseShippingFee : 0;
        const totalBenefit = discount + shippingSaved;
        return {
            eligible: true,
            missing: 0,
            discount,
            freeship: isFreeship,
            totalBenefit,
            minOrder: min
        };
    }

    /**
     * Cập nhật trạng thái các mã theo tạm tính & địa chỉ nhận hàng hiện tại.
     */
    function refresh(root, subtotal, address) {
        const baseShipInfo = calculateBaseShipping(subtotal, address);
        const result = {
            code: '',
            discount: 0,
            freeship: baseShipInfo.fee === 0,
            shipping: baseShipInfo.fee,
            regionLabel: baseShipInfo.regionLabel
        };
        if (!root) return result;
        const none = root.querySelector('input[type=radio][value=""]');
        const evaluated = [];

        root.querySelectorAll('input[type=radio]').forEach(input => {
            if (!input.value) return;
            const info = couponDiscount(input, subtotal, baseShipInfo.fee);
            const label = input.closest('.coupon-option');
            evaluated.push({ input, label, info });
        });

        let bestItem = null;
        evaluated.forEach(item => {
            if (!item.info.eligible || item.info.totalBenefit <= 0) return;
            if (!bestItem) {
                bestItem = item;
            } else if (item.info.totalBenefit > bestItem.info.totalBenefit) {
                bestItem = item;
            } else if (item.info.totalBenefit === bestItem.info.totalBenefit && item.info.minOrder > bestItem.info.minOrder) {
                bestItem = item;
            }
        });

        evaluated.forEach(item => {
            const { input, label, info } = item;
            const cond = label.querySelector('.coupon-condition');
            const badge = label.querySelector('.coupon-priority-badge');
            const isBest = (bestItem === item);

            input.disabled = !info.eligible;
            label.classList.toggle('is-disabled', !info.eligible);
            label.classList.toggle('is-best', isBest);

            if (badge) {
                badge.classList.toggle('d-none', !isBest);
            }

            if (cond) {
                if (!cond.dataset.original) cond.dataset.original = cond.textContent;
                if (info.eligible) {
                    cond.textContent = cond.dataset.original + ' • Giảm ' + formatVnd(info.discount) + (info.freeship ? ' + miễn phí ship' : '');
                } else {
                    cond.textContent = subtotal > 0
                        ? 'Mua thêm ' + formatVnd(info.missing) + ' để dùng mã này'
                        : cond.dataset.original;
                }
                cond.classList.toggle('text-danger', !info.eligible && subtotal > 0);
            }

            if (!info.eligible && input.checked && none) {
                none.checked = true;
            }
            if (info.eligible && input.checked) {
                result.code = input.value;
                result.discount = info.discount;
                result.freeship = info.freeship || baseShipInfo.fee === 0;
                result.shipping = (info.freeship || baseShipInfo.fee === 0) ? 0 : baseShipInfo.fee;
                if (info.freeship && baseShipInfo.fee > 0) {
                    result.regionLabel = 'Miễn phí (Mã ' + input.value + ')';
                }
            }
        });

        if (evaluated.length > 1) {
            const container = evaluated[0].label.parentElement;
            const toggleBtn = container.querySelector('.coupon-toggle-btn');
            const sorted = [...evaluated].sort((a, b) => {
                if (a.info.eligible !== b.info.eligible) {
                    return a.info.eligible ? -1 : 1;
                }
                if (a.info.eligible) {
                    if (b.info.totalBenefit !== a.info.totalBenefit) {
                        return b.info.totalBenefit - a.info.totalBenefit;
                    }
                    return b.info.minOrder - a.info.minOrder;
                } else {
                    return a.info.missing - b.info.missing;
                }
            });

            sorted.forEach(item => {
                if (toggleBtn) {
                    container.insertBefore(item.label, toggleBtn);
                } else {
                    container.appendChild(item.label);
                }
            });
        }

        root.querySelectorAll('.coupon-option').forEach(l =>
            l.classList.toggle('is-selected', !!l.querySelector('input:checked')));
        updateToggleLabel(root);
        return result;
    }

    function updateToggleLabel(root) {
        const btn = root.querySelector('.coupon-toggle-btn');
        if (!btn) return;
        const coupons = Array.from(root.querySelectorAll('input[type=radio]')).filter(i => i.value);
        const usable = coupons.filter(i => !i.disabled).length;
        const collapsed = root.classList.contains('is-collapsed');
        btn.querySelector('.coupon-toggle-text').textContent = collapsed
            ? 'Xem tất cả ' + coupons.length + ' mã (' + usable + ' dùng được)'
            : 'Thu gọn';
        const icon = btn.querySelector('.coupon-toggle-icon');
        icon.classList.toggle('fa-chevron-down', collapsed);
        icon.classList.toggle('fa-chevron-up', !collapsed);
        btn.setAttribute('aria-expanded', String(!collapsed));
    }

    function initToggle() {
        document.querySelectorAll('.coupon-picker').forEach(root => {
            const btn = root.querySelector('.coupon-toggle-btn');
            if (!btn) return;
            btn.addEventListener('click', () => {
                root.classList.toggle('is-collapsed');
                updateToggleLabel(root);
            });
            updateToggleLabel(root);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initToggle);
    } else {
        initToggle();
    }

    global.CouponPicker = { refresh, formatVnd, calculateBaseShipping, SHIPPING_FEE, FREE_SHIPPING_THRESHOLD };
})(window);
