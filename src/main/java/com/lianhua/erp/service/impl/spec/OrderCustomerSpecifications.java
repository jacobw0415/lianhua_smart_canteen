package com.lianhua.erp.service.impl.spec;
import com.lianhua.erp.domain.OrderCustomer;
import com.lianhua.erp.dto.orderCustomer.OrderCustomerRequestDto;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class OrderCustomerSpecifications {

    public static Specification<OrderCustomer> bySearchRequest(
            OrderCustomerRequestDto request
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // ===== 客戶名稱（模糊搜尋）=====
            if (StringUtils.hasText(request.getName())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("name")),
                                "%" + request.getName().trim().toLowerCase() + "%"
                        )
                );
            }

            // ===== 聯絡人（模糊搜尋）=====
            if (StringUtils.hasText(request.getContactPerson())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("contactPerson")),
                                "%" + request.getContactPerson().trim().toLowerCase() + "%"
                        )
                );
            }

            // ===== 電話（模糊搜尋）=====
            if (StringUtils.hasText(request.getPhone())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("phone")),
                                "%" + request.getPhone().trim().toLowerCase() + "%"
                        )
                );
            }

            // ===== 地址（模糊搜尋）=====
            if (StringUtils.hasText(request.getAddress())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("address")),
                                "%" + request.getAddress().trim().toLowerCase() + "%"
                        )
                );
            }

            // ===== 結帳週期（模糊搜尋）=====
            if (StringUtils.hasText(request.getBillingCycle())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("billingCycle")),
                                "%" + request.getBillingCycle().trim().toLowerCase() + "%"
                        )
                );
            }

            // ===== 備註（模糊搜尋）=====
            if (StringUtils.hasText(request.getNote())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("note")),
                                "%" + request.getNote().trim().toLowerCase() + "%"
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

