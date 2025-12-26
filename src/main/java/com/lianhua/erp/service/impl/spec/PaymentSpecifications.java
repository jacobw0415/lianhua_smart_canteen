package com.lianhua.erp.service.impl.spec;

import com.lianhua.erp.domain.Payment;
import com.lianhua.erp.domain.PaymentRecordStatus;
import com.lianhua.erp.dto.payment.PaymentSearchRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

public class PaymentSpecifications {

    /** ----------------------------------------------------------
     * ⭐ 主方法：依照搜尋條件動態組合 Specification
     * ---------------------------------------------------------- */
    public static Specification<Payment> build(PaymentSearchRequest req) {

        Specification<Payment> spec = Specification.allOf();

        // ⭐ 狀態過濾
        if (StringUtils.hasText(req.getStatus())) {
            try {
                PaymentRecordStatus status = PaymentRecordStatus.valueOf(req.getStatus().toUpperCase());
                spec = spec.and((root, query, cb) -> 
                    cb.equal(root.get("status"), status)
                );
            } catch (IllegalArgumentException e) {
                // 忽略無效的狀態值
            }
        } else {
            // 預設排除已作廢的付款（除非明確要求包含）
            if (!Boolean.TRUE.equals(req.getIncludeVoided())) {
                spec = spec.and((root, query, cb) -> 
                    cb.equal(root.get("status"), PaymentRecordStatus.ACTIVE)
                );
            }
        }

        spec = spec.and(bySupplierName(req));
        spec = spec.and(byItem(req));
        spec = spec.and(byMethod(req));
        spec = spec.and(byAccountingPeriod(req));
        spec = spec.and(byDateRange(req));

        return spec;
    }

    private static Specification<Payment> bySupplierName(PaymentSearchRequest req) {
        if (isEmpty(req.getSupplierName())) return null;

        String keyword = "%" + req.getSupplierName().trim() + "%";

        return (root, query, cb) ->
                cb.like(
                        root.join("purchase").join("supplier").get("name"),
                        keyword
                );
    }

    private static Specification<Payment> byItem(PaymentSearchRequest req) {
        if (isEmpty(req.getItem())) return null;

        String keyword = "%" + req.getItem().trim() + "%";

        return (root, query, cb) ->
                cb.like(root.join("purchase").get("item"), keyword);
    }

    private static Specification<Payment> byMethod(PaymentSearchRequest req) {
        if (isEmpty(req.getMethod())) return null;

        return (root, query, cb) ->
                cb.equal(root.get("method"), req.getMethod());
    }

    private static Specification<Payment> byAccountingPeriod(PaymentSearchRequest req) {
        if (isEmpty(req.getAccountingPeriod())) return null;

        return (root, query, cb) ->
                cb.equal(root.get("accountingPeriod"), req.getAccountingPeriod());
    }

    private static Specification<Payment> byDateRange(PaymentSearchRequest req) {
        Specification<Payment> spec = Specification.allOf();

        if (!isEmpty(req.getFromDate())) {
            LocalDate from = LocalDate.parse(req.getFromDate());
            spec = spec.and(
                    (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("payDate"), from)
            );
        }

        if (!isEmpty(req.getToDate())) {
            LocalDate to = LocalDate.parse(req.getToDate());
            spec = spec.and(
                    (root, query, cb) -> cb.lessThanOrEqualTo(root.get("payDate"), to)
            );
        }

        return spec;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
