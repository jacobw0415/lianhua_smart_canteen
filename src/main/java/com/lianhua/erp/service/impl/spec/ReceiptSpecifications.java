package com.lianhua.erp.service.impl.spec;

import com.lianhua.erp.domain.Receipt;
import com.lianhua.erp.domain.ReceiptStatus;
import com.lianhua.erp.dto.receipt.ReceiptSearchRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReceiptSpecifications {

    /**
     * 主方法：依照搜尋條件動態組合 Specification
     * 支援方法名稱：bySearchRequest（新）或 build（向後相容）
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

            // 使用 distinct 避免 join 造成重複筆數
            query.distinct(true);

            // 確保關聯資料被載入（用於映射 orderNo 和 customerName）
            if (!query.getResultType().equals(Long.class) && !query.getResultType().equals(long.class)) {
                root.fetch("order", jakarta.persistence.criteria.JoinType.LEFT)
                        .fetch("customer", jakarta.persistence.criteria.JoinType.LEFT);
            }

            /* =====================================================
             * id（主鍵｜精確）
             * ===================================================== */
            if (request.getId() != null) {
                predicates.add(cb.equal(root.get("id"), request.getId()));
            }

            /* =====================================================
             * orderNo（訂單編號｜模糊搜尋）
             * JOIN order.orderNo
             * ===================================================== */
            if (StringUtils.hasText(request.getOrderNo())) {
                predicates.add(
                        cb.like(
                                cb.upper(root.join("order").get("orderNo")),
                                "%" + request.getOrderNo().trim().toUpperCase() + "%"
                        )
                );
            }

            /* =====================================================
             * customerName（客戶名稱｜模糊搜尋）
             * JOIN order.customer.name
             * ===================================================== */
            if (StringUtils.hasText(request.getCustomerName())) {
                predicates.add(
                        cb.like(
                                cb.lower(
                                        root.join("order")
                                                .join("customer")
                                                .get("name")
                                ),
                                "%" + request.getCustomerName().trim().toLowerCase() + "%"
                        )
                );
            }

            /* =====================================================
             * method（收款方式｜精確，需轉換為 enum）
             * ===================================================== */
            if (StringUtils.hasText(request.getMethod())) {
                try {
                    Receipt.PaymentMethod method = Receipt.PaymentMethod.valueOf(request.getMethod().toUpperCase());
                    predicates.add(cb.equal(root.get("method"), method));
                } catch (IllegalArgumentException e) {
                    // 如果傳入的 method 值無效，忽略此條件
                }
            }

            /* =====================================================
             * status（狀態：有效 / 作廢｜精確，需轉換為 enum）
             * 優先使用 status 參數，如果沒有則使用 includeVoided（向後相容）
             * ===================================================== */
            if (StringUtils.hasText(request.getStatus())) {
                try {
                    ReceiptStatus status = ReceiptStatus.valueOf(request.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    // 如果傳入的 status 值無效，忽略此條件
                }
            } else {
                // 向後相容：使用 includeVoided 參數
                // 預設排除已作廢的收款（除非明確要求包含）
                if (!Boolean.TRUE.equals(request.getIncludeVoided())) {
                    predicates.add(cb.equal(root.get("status"), ReceiptStatus.ACTIVE));
                }
            }

            /* =====================================================
             * accountingPeriod（會計期間｜精確）
             * ===================================================== */
            if (StringUtils.hasText(request.getAccountingPeriod())) {
                predicates.add(cb.equal(root.get("accountingPeriod"), request.getAccountingPeriod()));
            }

            /* =====================================================
             * receivedDate（收款日期）範圍
             * 支援 receivedDateFrom/receivedDateTo（LocalDate）或 fromDate/toDate（String）
             * ===================================================== */
            LocalDate fromDate = null;
            LocalDate toDate = null;

            // 優先使用 LocalDate 欄位
            if (request.getReceivedDateFrom() != null) {
                fromDate = request.getReceivedDateFrom();
            } else if (StringUtils.hasText(request.getFromDate())) {
                try {
                    fromDate = LocalDate.parse(request.getFromDate());
                } catch (Exception e) {
                    // 日期格式錯誤，忽略
                }
            }

            if (request.getReceivedDateTo() != null) {
                toDate = request.getReceivedDateTo();
            } else if (StringUtils.hasText(request.getToDate())) {
                try {
                    toDate = LocalDate.parse(request.getToDate());
                } catch (Exception e) {
                    // 日期格式錯誤，忽略
                }
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
