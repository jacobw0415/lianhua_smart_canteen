package com.lianhua.erp.service.impl.spec;

import com.lianhua.erp.domain.Payment;
import com.lianhua.erp.dto.payment.PaymentSearchRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class PaymentSpecifications {

    /** ----------------------------------------------------------
     * ⭐ 主方法：依照搜尋條件動態組合 Specification
     * ---------------------------------------------------------- */
    public static Specification<Payment> build(PaymentSearchRequest req) {

        Specification<Payment> spec = Specification.allOf();

        // ⭐ 預設排除已作廢的付款（除非明確要求包含）
        // 注意：此功能需要 Payment 實體有 status 欄位（PaymentStatus enum: ACTIVE, VOIDED）
        if (!Boolean.TRUE.equals(req.getIncludeVoided())) {
            // TODO: 當 Payment 實體添加 status 欄位後，取消註解以下邏輯
            // spec = spec.and((root, query, cb) -> 
            //     cb.equal(root.get("status"), com.lianhua.erp.domain.PaymentStatus.ACTIVE)
            // );
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
