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

        // ======================================================
        // ⭐ 狀態過濾（精確 + 模糊 + 字首推測）
        // ======================================================
        if (StringUtils.hasText(req.getStatus())) {
            PaymentRecordStatus targetStatus = resolveStatus(req.getStatus());

            if (targetStatus != null) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("status"), targetStatus)
                );
            }
        } else {
            // 預設排除已作廢（除非明確指定）
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

    /* ======================================================
     * ⭐ 狀態解析核心（重點）
     * ====================================================== */
    private static PaymentRecordStatus resolveStatus(String input) {
        if (!StringUtils.hasText(input)) return null;

        String normalized = input.trim().toLowerCase();

        // ---------- 1️⃣ 先嘗試 enum 精確匹配 ----------
        try {
            return PaymentRecordStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ignore) {
            // 繼續做模糊與字首解析
        }

        // ---------- 2️⃣ 中文 / 英文模糊關鍵字 ----------
        if (containsAny(normalized, "作廢", "作废", "void", "voided")) {
            return PaymentRecordStatus.VOIDED;
        }

        if (containsAny(normalized, "有效", "正常", "生效", "active")) {
            return PaymentRecordStatus.ACTIVE;
        }

        // ---------- 3️⃣ 字首推測（ACT / VOI / VO）----------
        if (normalized.startsWith("act")) {
            return PaymentRecordStatus.ACTIVE;
        }

        if (normalized.startsWith("voi") || normalized.startsWith("vo")) {
            return PaymentRecordStatus.VOIDED;
        }

        // 無法解析 → 不加 status 條件
        return null;
    }

    private static boolean containsAny(String src, String... keywords) {
        for (String k : keywords) {
            if (src.contains(k)) return true;
        }
        return false;
    }

    /* ======================================================
     * 其餘條件（原封不動）
     * ====================================================== */
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
