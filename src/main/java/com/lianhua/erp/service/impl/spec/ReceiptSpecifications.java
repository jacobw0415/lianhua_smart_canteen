package com.lianhua.erp.service.impl.spec;

import com.lianhua.erp.domain.Receipt;
import com.lianhua.erp.domain.ReceiptStatus;
import com.lianhua.erp.dto.receipt.ReceiptSearchRequest;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReceiptSpecifications {

    /**
     * 主方法：依照搜尋條件動態組合 Specification
     */
    public static Specification<Receipt> bySearchRequest(ReceiptSearchRequest request) {
        return build(request);
    }

    /**
     * 向後相容的方法名稱
     */
    public static Specification<Receipt> build(ReceiptSearchRequest request) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // 使用 distinct 避免因多重 join 造成重複筆數
            query.distinct(true);

            // 1. ✅ 確保關聯資料被載入 (Fetch Join)
            // 注意：fetch 與之後的 join 類型必須一致 (統一使用 LEFT)
            if (!query.getResultType().equals(Long.class) && !query.getResultType().equals(long.class)) {
                root.fetch("order", JoinType.LEFT);
                // 如果 Receipt 直接關聯 Customer，才開啟下方這行；
                // 如果 Customer 是在 Order 底下，則不應直接 fetch("customer")
                // root.fetch("customer", JoinType.LEFT);
            }

            /* =====================================================
             * id (主鍵 | 精確)
             * ===================================================== */
            if (request.getId() != null) {
                predicates.add(cb.equal(root.get("id"), request.getId()));
            }

            /* =====================================================
             * orderNo (訂單編號 | 模糊搜尋)
             * 💡 關鍵修正：join 必須指定 JoinType.LEFT
             * ===================================================== */
            if (StringUtils.hasText(request.getOrderNo())) {
                predicates.add(
                        cb.like(
                                cb.upper(root.join("order", JoinType.LEFT).get("orderNo")),
                                "%" + request.getOrderNo().trim().toUpperCase() + "%"
                        )
                );
            }

            /* =====================================================
             * customerName (客戶名稱 | 模糊搜尋)
             * 💡 關鍵修正：連續 join 都必須指定 JoinType.LEFT
             * ===================================================== */
            if (StringUtils.hasText(request.getCustomerName())) {
                predicates.add(
                        cb.like(
                                cb.lower(
                                        root.join("order", JoinType.LEFT)
                                                .join("customer", JoinType.LEFT)
                                                .get("name")
                                ),
                                "%" + request.getCustomerName().trim().toLowerCase() + "%"
                        )
                );
            }

            /* =====================================================
             * method (收款方式)
             * ===================================================== */
            if (StringUtils.hasText(request.getMethod())) {
                try {
                    Receipt.PaymentMethod method = Receipt.PaymentMethod.valueOf(request.getMethod().toUpperCase());
                    predicates.add(cb.equal(root.get("method"), method));
                } catch (IllegalArgumentException e) {
                    // 無效 enum 值則忽略
                }
            }

            /* =====================================================
             * status (狀態：有效 / 作廢)
             * ===================================================== */
            if (StringUtils.hasText(request.getStatus())) {
                try {
                    ReceiptStatus status = ReceiptStatus.valueOf(request.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    // 無效則忽略
                }
            } else {
                // 預設排除已作廢 (除非明確 includeVoided)
                if (!Boolean.TRUE.equals(request.getIncludeVoided())) {
                    predicates.add(cb.equal(root.get("status"), ReceiptStatus.ACTIVE));
                }
            }

            /* =====================================================
             * accountingPeriod (會計期間)
             * ===================================================== */
            if (StringUtils.hasText(request.getAccountingPeriod())) {
                predicates.add(cb.equal(root.get("accountingPeriod"), request.getAccountingPeriod()));
            }

            /* =====================================================
             * receivedDate (收款日期範圍)
             * ===================================================== */
            LocalDate fromDate = null;
            LocalDate toDate = null;

            if (request.getReceivedDateFrom() != null) {
                fromDate = request.getReceivedDateFrom();
            } else if (StringUtils.hasText(request.getFromDate())) {
                try { fromDate = LocalDate.parse(request.getFromDate()); } catch (Exception ignored) {}
            }

            if (request.getReceivedDateTo() != null) {
                toDate = request.getReceivedDateTo();
            } else if (StringUtils.hasText(request.getToDate())) {
                try { toDate = LocalDate.parse(request.getToDate()); } catch (Exception ignored) {}
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("receivedDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("receivedDate"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}