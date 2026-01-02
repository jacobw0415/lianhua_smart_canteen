package com.lianhua.erp.service.impl.spec;

import com.lianhua.erp.domain.ExpenseCategory;
import com.lianhua.erp.dto.expense.ExpenseCategorySearchRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ExpenseCategorySpecifications {

    /**
     * 構建費用類別搜尋 Specification
     */
    public static Specification<ExpenseCategory> build(ExpenseCategorySearchRequest search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 類別名稱（模糊搜尋）
            if (StringUtils.hasText(search.getName())) {
                String keyword = "%" + search.getName().trim() + "%";
                predicates.add(
                        cb.like(
                                cb.lower(root.get("name")),
                                keyword.toLowerCase()
                        )
                );
            }

            // 會計科目代碼（模糊搜尋）
            if (StringUtils.hasText(search.getAccountCode())) {
                String keyword = "%" + search.getAccountCode().trim() + "%";
                predicates.add(
                        cb.like(
                                cb.upper(root.get("accountCode")),
                                keyword.toUpperCase()
                        )
                );
            }

            // 是否啟用（精確搜尋）
            if (search.getActive() != null) {
                predicates.add(
                        cb.equal(root.get("active"), search.getActive())
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

