package com.lianhua.erp.service.impl.spec;

import com.lianhua.erp.domain.ActivityAuditLog;
import com.lianhua.erp.dto.audit.ActivityAuditLogSearchRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 活動稽核查詢規格（Specification）。
 * <p>
 * 抽離於 Service，以便 list 與 export 共用相同條件。
 */
public class ActivityAuditLogSpecifications {

    public static Specification<ActivityAuditLog> bySearchRequest(ActivityAuditLogSearchRequest req) {
        return (root, query, cb) -> {
            if (req == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            // keyword 模糊搜尋：對多欄位做 OR，其他條件用 AND 疊加
            if (StringUtils.hasText(req.keyword())) {
                String pattern = "%" + req.keyword().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("requestPath")), pattern),
                        cb.like(cb.lower(root.get("queryString")), pattern),
                        cb.like(cb.lower(root.get("details")), pattern),
                        cb.like(cb.lower(root.get("action")), pattern),
                        cb.like(cb.lower(root.get("resourceType")), pattern),
                        cb.like(cb.lower(root.get("operatorUsername")), pattern),
                        cb.like(cb.lower(root.get("ipAddress")), pattern),
                        cb.like(cb.lower(root.get("userAgent")), pattern)
                ));
            }

            // operatorUsername（獨立模糊搜尋）：以 AND 條件疊加
            if (StringUtils.hasText(req.operatorUsername())) {
                String pattern = "%" + req.operatorUsername().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("operatorUsername")), pattern));
            }

            if (req.operatorId() != null) {
                predicates.add(cb.equal(root.get("operatorId"), req.operatorId()));
            }

            if (req.action() != null && !req.action().isBlank()) {
                predicates.add(cb.equal(
                        cb.upper(root.get("action")),
                        req.action().trim().toUpperCase(Locale.ROOT)
                ));
            }

            if (req.resourceType() != null && !req.resourceType().isBlank()) {
                predicates.add(cb.equal(
                        cb.upper(root.get("resourceType")),
                        req.resourceType().trim().toUpperCase(Locale.ROOT)
                ));
            }

            if (req.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), req.from()));
            }
            if (req.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), req.to()));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}

