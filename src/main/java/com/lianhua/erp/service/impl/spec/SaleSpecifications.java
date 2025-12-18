package com.lianhua.erp.service.impl.spec;

import com.lianhua.erp.domain.Sale;
import com.lianhua.erp.dto.sale.SaleSearchRequestDto;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class SaleSpecifications {

    /** ----------------------------------------------------------
     * ⭐ 主方法：依照搜尋條件動態組合 Specification
     * ---------------------------------------------------------- */
    public static Specification<Sale> build(SaleSearchRequestDto req) {
        Specification<Sale> spec = Specification.allOf();

        spec = spec.and(byProductName(req));
        spec = spec.and(byPayMethod(req));
        spec = spec.and(byDateRange(req));

        return spec;
    }

    /** ----------------------------------------------------------
     * 1. productName（模糊搜尋，join product.name）
     * ---------------------------------------------------------- */
    private static Specification<Sale> byProductName(SaleSearchRequestDto req) {
        if (isEmpty(req.getProductName())) return null;

        String keyword = "%" + req.getProductName().trim() + "%";

        return (root, query, cb) -> {
            // 🔧 避免 join 造成重複筆數
            query.distinct(true);

            return cb.like(
                    root.join("product").get("name"),
                    keyword
            );
        };
    }

    /** ----------------------------------------------------------
     * 2. payMethod（精準）
     * ---------------------------------------------------------- */
    private static Specification<Sale> byPayMethod(SaleSearchRequestDto req) {
        if (isEmpty(req.getPayMethod())) return null;

        return (root, query, cb) ->
                cb.equal(root.get("payMethod"), req.getPayMethod());
    }

    /** ----------------------------------------------------------
     * 3. 銷售日期區間（saleDate from ～ to）
     * ---------------------------------------------------------- */
    private static Specification<Sale> byDateRange(SaleSearchRequestDto req) {
        Specification<Sale> spec = Specification.allOf();

        // 起：saleDate >= from
        if (req.getSaleDateFrom() != null) {
            LocalDate from = req.getSaleDateFrom();
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("saleDate"), from)
            );
        }

        // 迄：saleDate <= to
        if (req.getSaleDateTo() != null) {
            LocalDate to = req.getSaleDateTo();
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("saleDate"), to)
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
