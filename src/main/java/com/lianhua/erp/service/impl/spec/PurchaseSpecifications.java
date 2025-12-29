package com.lianhua.erp.service.impl.spec;

import com.lianhua.erp.domain.Purchase;
import com.lianhua.erp.dto.purchase.PurchaseSearchRequest;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class PurchaseSpecifications {

    /** ----------------------------------------------------------
     * ⭐ 主方法：依照搜尋條件動態組合 Specification
     * ---------------------------------------------------------- */
    public static Specification<Purchase> build(PurchaseSearchRequest req) {
        Specification<Purchase> spec = Specification.allOf();

        spec = spec.and(bySupplierId(req));
        spec = spec.and(bySupplierName(req));
        spec = spec.and(byItem(req));
        spec = spec.and(byStatus(req));
        spec = spec.and(byAccountingPeriod(req));
        spec = spec.and(byPurchaseNo(req));
        spec = spec.and(byDateRange(req));

        return spec;
    }

    /** ----------------------------------------------------------
     * 1. supplierId（精準）
     * ---------------------------------------------------------- */
    private static Specification<Purchase> bySupplierId(PurchaseSearchRequest req) {
        if (req.getSupplierId() == null) return null;

        return (root, query, cb) ->
                cb.equal(root.get("supplier").get("id"), req.getSupplierId());
    }

    /** ----------------------------------------------------------
     * 2. supplierName（模糊搜尋）
     * ---------------------------------------------------------- */
    private static Specification<Purchase> bySupplierName(PurchaseSearchRequest req) {
        if (isEmpty(req.getSupplierName())) return null;

        String keyword = "%" + req.getSupplierName().trim() + "%";

        return (root, query, cb) ->
                cb.like(root.get("supplier").get("name"), keyword);
    }

    /** ----------------------------------------------------------
     * 3. item（模糊搜尋）
     * ---------------------------------------------------------- */
    private static Specification<Purchase> byItem(PurchaseSearchRequest req) {
        if (isEmpty(req.getItem())) return null;

        String keyword = "%" + req.getItem().trim() + "%";

        return (root, query, cb) ->
                cb.like(root.get("item"), keyword);
    }

    /** ----------------------------------------------------------
     * 4. status（精準）
     * ---------------------------------------------------------- */
    private static Specification<Purchase> byStatus(PurchaseSearchRequest req) {
        if (isEmpty(req.getStatus())) return null;

        return (root, query, cb) ->
                cb.equal(root.get("status"), req.getStatus());
    }

    /** ----------------------------------------------------------
     * 5. accountingPeriod（YYYY-MM，精準）
     * ---------------------------------------------------------- */
    private static Specification<Purchase> byAccountingPeriod(PurchaseSearchRequest req) {
        if (isEmpty(req.getAccountingPeriod())) return null;

        return (root, query, cb) ->
                cb.equal(root.get("accountingPeriod"), req.getAccountingPeriod());
    }

    /** ----------------------------------------------------------
     * 6. purchaseNo（進貨單編號，模糊搜尋）
     * ---------------------------------------------------------- */
    private static Specification<Purchase> byPurchaseNo(PurchaseSearchRequest req) {
        if (isEmpty(req.getPurchaseNo())) return null;

        String keyword = "%" + req.getPurchaseNo().trim() + "%";

        return (root, query, cb) ->
                cb.like(root.get("purchaseNo"), keyword);
    }

    /** ----------------------------------------------------------
     * 7. 日期區間（fromDate ～ toDate）
     * ---------------------------------------------------------- */
    private static Specification<Purchase> byDateRange(PurchaseSearchRequest req) {
        Specification<Purchase> spec = Specification.allOf();

        // 起：purchaseDate >= from
        if (!isEmpty(req.getFromDate())) {
            LocalDate from = LocalDate.parse(req.getFromDate());
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("purchaseDate"), from)
            );
        }

        // 迄：purchaseDate <= to
        if (!isEmpty(req.getToDate())) {
            LocalDate to = LocalDate.parse(req.getToDate());
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("purchaseDate"), to)
            );
        }

        return spec;
    }

    /** ----------------------------------------------------------
     * 工具函式：避免 Null / 空白異常
     * ---------------------------------------------------------- */
    private static boolean isEmpty(String str) {
        return (str == null || str.trim().isEmpty());
    }
}
