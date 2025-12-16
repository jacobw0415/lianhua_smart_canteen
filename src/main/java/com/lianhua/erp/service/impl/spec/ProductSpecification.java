package com.lianhua.erp.service.impl.spec;

import com.lianhua.erp.domain.Product;
import com.lianhua.erp.domain.ProductCategory;
import com.lianhua.erp.dto.product.ProductSearchRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {
    
    public static Specification<Product> build(ProductSearchRequest search) {
        
        return (root, query, cb) -> {
            
            List<Predicate> predicates = new ArrayList<>();
            
            /* =========================
             * 商品名稱（LIKE）
             * ========================= */
            if (StringUtils.hasText(search.getName())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("name")),
                                "%" + search.getName().trim().toLowerCase() + "%"
                        )
                );
            }
            
            /* =========================
             * 是否啟用（=）
             * ========================= */
            if (search.getActive() != null) {
                predicates.add(
                        cb.equal(root.get("active"), search.getActive())
                );
            }
            
            /* =========================
             * 分類 ID（=）
             * ========================= */
            if (search.getCategoryId() != null) {
                Join<Product, ProductCategory> categoryJoin =
                        root.join("category", JoinType.LEFT);
                
                predicates.add(
                        cb.equal(categoryJoin.get("id"), search.getCategoryId())
                );
            }
            
            /* =========================
             * ⭐ 分類代碼（LIKE）
             * ========================= */
            if (StringUtils.hasText(search.getCategoryCode())) {
                Join<Product, ProductCategory> categoryJoin =
                        root.join("category", JoinType.LEFT);
                
                predicates.add(
                        cb.like(
                                cb.upper(categoryJoin.get("code")),
                                "%" + search.getCategoryCode().trim().toUpperCase() + "%"
                        )
                );
            }
            
            /* =========================
             * 價格區間（BETWEEN）
             * ========================= */
            if (search.getUnitPriceMin() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("unitPrice"),
                                search.getUnitPriceMin()
                        )
                );
            }
            
            if (search.getUnitPriceMax() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("unitPrice"),
                                search.getUnitPriceMax()
                        )
                );
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

