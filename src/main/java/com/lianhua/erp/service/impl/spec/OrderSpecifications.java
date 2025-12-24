package com.lianhua.erp.service.impl.spec;

import com.lianhua.erp.domain.Order;
import com.lianhua.erp.dto.order.OrderSearchRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class OrderSpecifications {

    public static Specification<Order> bySearchRequest(
            OrderSearchRequest request
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            /* =====================================================
             * id（主鍵｜精確）
             * ===================================================== */
            if (request.getId() != null) {
                predicates.add(
                        cb.equal(root.get("id"), request.getId())
                );
            }

            /* =====================================================
             * orderNo（訂單編號｜模糊搜尋） ✅【補上】
             * ===================================================== */
            if (StringUtils.hasText(request.getOrderNo())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("orderNo")),
                                "%" + request.getOrderNo().trim().toLowerCase() + "%"
                        )
                );
            }

            /* =====================================================
             * customerId（客戶 ID｜精確）
             * ===================================================== */
            if (request.getCustomerId() != null) {
                predicates.add(
                        cb.equal(
                                root.get("customer").get("id"),
                                request.getCustomerId()
                        )
                );
            }

            /* =====================================================
             * customerName（JOIN customer.name｜模糊搜尋）
             * ===================================================== */
            if (StringUtils.hasText(request.getCustomerName())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.join("customer").get("name")),
                                "%" + request.getCustomerName().trim().toLowerCase() + "%"
                        )
                );
            }

            /* =====================================================
             * note（備註｜模糊搜尋）
             * ===================================================== */
            if (StringUtils.hasText(request.getNote())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("note")),
                                "%" + request.getNote().trim().toLowerCase() + "%"
                        )
                );
            }

            /* =====================================================
             * orderDate 範圍
             * ===================================================== */
            if (request.getOrderDateFrom() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("orderDate"),
                                request.getOrderDateFrom()
                        )
                );
            }
            if (request.getOrderDateTo() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("orderDate"),
                                request.getOrderDateTo()
                        )
                );
            }

            /* =====================================================
             * deliveryDate 範圍
             * ===================================================== */
            if (request.getDeliveryDateFrom() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("deliveryDate"),
                                request.getDeliveryDateFrom()
                        )
                );
            }
            if (request.getDeliveryDateTo() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("deliveryDate"),
                                request.getDeliveryDateTo()
                        )
                );
            }

            /* =====================================================
             * orderStatus（訂單狀態｜精確）
             * ===================================================== */
            if (StringUtils.hasText(request.getOrderStatus())) {
                predicates.add(
                        cb.equal(
                                root.get("orderStatus"),
                                request.getOrderStatus()
                        )
                );
            }

            /* =====================================================
             * paymentStatus（付款狀態｜精確） ✅【補上】
             * ===================================================== */
            if (StringUtils.hasText(request.getPaymentStatus())) {
                predicates.add(
                        cb.equal(
                                root.get("paymentStatus"),
                                request.getPaymentStatus()
                        )
                );
            }

            /* =====================================================
             * accountingPeriod（會計期間｜精確）
             * ===================================================== */
            if (StringUtils.hasText(request.getAccountingPeriod())) {
                predicates.add(
                        cb.equal(
                                root.get("accountingPeriod"),
                                request.getAccountingPeriod()
                        )
                );
            }

            /* =====================================================
             * totalAmount 範圍
             * ===================================================== */
            if (request.getTotalAmountMin() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("totalAmount"),
                                request.getTotalAmountMin()
                        )
                );
            }
            if (request.getTotalAmountMax() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("totalAmount"),
                                request.getTotalAmountMax()
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
