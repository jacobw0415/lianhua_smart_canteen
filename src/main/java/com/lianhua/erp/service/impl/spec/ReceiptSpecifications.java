package com.lianhua.erp.service.impl.spec;

import com.lianhua.erp.domain.Receipt;
import com.lianhua.erp.dto.receipt.ReceiptSearchRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class ReceiptSpecifications {

    /**
     * ----------------------------------------------------------
     * ⭐ 主方法：依照搜尋條件動態組合 Specification
     * ----------------------------------------------------------
     */
    public static Specification<Receipt> build(ReceiptSearchRequest req) {

        Specification<Receipt> spec = Specification.allOf();

        // 確保關聯資料被載入（用於映射 orderNo 和 customerName）
        spec = spec.and(fetchOrderAndCustomer());

        spec = spec.and(byCustomerName(req));
        spec = spec.and(byOrderNo(req));
        spec = spec.and(byMethod(req));
        spec = spec.and(byAccountingPeriod(req));
        spec = spec.and(byDateRange(req));

        return spec;
    }

    /**
     * 確保載入 Order 和 Customer 關聯（用於映射到 DTO）
     */
    private static Specification<Receipt> fetchOrderAndCustomer() {
        return (root, query, cb) -> {
            // 使用 fetch join 確保關聯資料被載入
            if (!query.getResultType().equals(Long.class) && !query.getResultType().equals(long.class)) {
                jakarta.persistence.criteria.Fetch<Receipt, com.lianhua.erp.domain.Order> orderFetch = root
                        .fetch("order", jakarta.persistence.criteria.JoinType.LEFT);
                orderFetch.fetch("customer", jakarta.persistence.criteria.JoinType.LEFT);
            }
            return null; // 這是一個 fetch join，不添加額外的條件
        };
    }

    private static Specification<Receipt> byCustomerName(ReceiptSearchRequest req) {
        if (isEmpty(req.getCustomerName()))
            return null;

        String keyword = "%" + req.getCustomerName().trim() + "%";

        return (root, query, cb) -> cb.like(
                root.join("order").join("customer").get("name"),
                keyword);
    }

    private static Specification<Receipt> byOrderNo(ReceiptSearchRequest req) {
        if (isEmpty(req.getOrderNo()))
            return null;

        String keyword = "%" + req.getOrderNo().trim() + "%";

        return (root, query, cb) -> cb.like(root.join("order").get("orderNo"), keyword);
    }

    private static Specification<Receipt> byMethod(ReceiptSearchRequest req) {
        if (isEmpty(req.getMethod()))
            return null;

        try {
            Receipt.PaymentMethod method = Receipt.PaymentMethod.valueOf(req.getMethod());
            return (root, query, cb) -> cb.equal(root.get("method"), method);
        } catch (IllegalArgumentException e) {
            // 如果傳入的 method 值無效，返回 null（不加入搜尋條件）
            return null;
        }
    }

    private static Specification<Receipt> byAccountingPeriod(ReceiptSearchRequest req) {
        if (isEmpty(req.getAccountingPeriod()))
            return null;

        return (root, query, cb) -> cb.equal(root.get("accountingPeriod"), req.getAccountingPeriod());
    }

    private static Specification<Receipt> byDateRange(ReceiptSearchRequest req) {
        Specification<Receipt> spec = Specification.allOf();

        if (!isEmpty(req.getFromDate())) {
            LocalDate from = LocalDate.parse(req.getFromDate());
            spec = spec.and(
                    (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("receivedDate"), from));
        }

        if (!isEmpty(req.getToDate())) {
            LocalDate to = LocalDate.parse(req.getToDate());
            spec = spec.and(
                    (root, query, cb) -> cb.lessThanOrEqualTo(root.get("receivedDate"), to));
        }

        return spec;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
