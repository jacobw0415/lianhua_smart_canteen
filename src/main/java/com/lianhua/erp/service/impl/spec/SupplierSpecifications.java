package com.lianhua.erp.service.impl.spec;

import com.lianhua.erp.domain.Supplier;
import com.lianhua.erp.dto.supplier.SupplierSearchRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SupplierSpecifications {

    /**
     * 依照搜尋請求動態建置過濾條件 (AND 邏輯)
     */
    public static Specification<Supplier> bySearchRequest(SupplierSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(req.getSupplierName())) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + req.getSupplierName().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(req.getContact())) {
                predicates.add(cb.like(
                        cb.lower(root.get("contact")),
                        "%" + req.getContact().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(req.getPhone())) {
                predicates.add(cb.like(
                        cb.lower(root.get("phone")),
                        "%" + req.getPhone().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(req.getBillingCycle())) {
                predicates.add(cb.equal(
                        cb.lower(root.get("billingCycle")),
                        req.getBillingCycle().toLowerCase()
                ));
            }

            if (StringUtils.hasText(req.getNote())) {
                predicates.add(cb.like(
                        cb.lower(root.get("note")),
                        "%" + req.getNote().toLowerCase() + "%"
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 專供全域搜尋使用的 OR 邏輯
     */
    public static Specification<Supplier> globalSearch(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return null;

            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("contact")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern)
            );
        };
    }
}