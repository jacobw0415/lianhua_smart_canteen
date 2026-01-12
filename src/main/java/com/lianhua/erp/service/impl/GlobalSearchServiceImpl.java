package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Order;
import com.lianhua.erp.domain.OrderCustomer;
import com.lianhua.erp.domain.Purchase;
import com.lianhua.erp.domain.Supplier;
import com.lianhua.erp.dto.globalSearch.GlobalSearchItemDto;
import com.lianhua.erp.dto.globalSearch.GlobalSearchRequest;
import com.lianhua.erp.dto.globalSearch.GlobalSearchResponse;
import com.lianhua.erp.repository.OrderCustomerRepository;
import com.lianhua.erp.repository.OrderRepository;
import com.lianhua.erp.repository.PurchaseRepository;
import com.lianhua.erp.repository.SupplierRepository;
import com.lianhua.erp.service.GlobalSearchService;
import com.lianhua.erp.service.impl.spec.SupplierSpecifications;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GlobalSearchServiceImpl implements GlobalSearchService {

    private final OrderRepository orderRepository;
    private final PurchaseRepository purchaseRepository;
    private final OrderCustomerRepository orderCustomerRepository;
    private final SupplierRepository supplierRepository;

    @Override
    public GlobalSearchResponse search(GlobalSearchRequest request) {
        validateRequest(request);

        String keyword = request.getKeyword().trim();
        // 取得會計期間參數 (由前端傳入，例如 "2026-01")
        String period = request.getPeriod();
        int limit = request.getLimit() != null ? request.getLimit() : 5;
        List<String> scopes = request.getScopes();

        List<GlobalSearchItemDto> results = new ArrayList<>();

        // 1. 搜尋訂單 (Orders) - 支援月份過濾
        if (scopes.contains("orders")) {
            results.addAll(fetchOrders(keyword, period, limit));
        }

        // 2. 搜尋進貨單 (Purchases) - 支援月份過濾
        if (scopes.contains("purchases")) {
            results.addAll(fetchPurchases(keyword, period, limit));
        }

        // 3. 搜尋客戶 (Customers) - 通常主檔不分月份
        if (scopes.contains("customers")) {
            results.addAll(fetchCustomers(keyword, limit));
        }

        if (scopes.contains("suppliers")) {
            results.addAll(fetchSuppliers(keyword, limit));
        }

        log.info("全域搜尋完成, 關鍵字: [{}], 月份: [{}], 總結果數: {}", keyword, period, results.size());
        return new GlobalSearchResponse(results);
    }

    private void validateRequest(GlobalSearchRequest request) {
        if (!StringUtils.hasText(request.getKeyword())) {
            throw new IllegalArgumentException("搜尋關鍵字不能為空");
        }
        if (request.getScopes() == null || request.getScopes().isEmpty()) {
            throw new IllegalArgumentException("搜尋範圍 (scopes) 不能為空");
        }
    }

    // --------------------------------------------------
    // 🔍 訂單搜尋：(OR 關鍵字) AND (會計期間)
    // --------------------------------------------------
    private List<GlobalSearchItemDto> fetchOrders(String keyword, String period, int limit) {
        String pattern = "%" + keyword.toLowerCase() + "%";

        Specification<Order> spec = (root, query, cb) -> {
            // 關鍵字部分 (OR)
            Predicate keywordMatch = cb.or(
                    cb.like(cb.lower(root.get("orderNo")), pattern),
                    cb.like(cb.lower(root.join("customer").get("name")), pattern),
                    cb.like(cb.lower(root.get("note")), pattern)
            );

            // 如果有提供月份，則加上 AND 條件
            if (StringUtils.hasText(period)) {
                Predicate periodMatch = cb.equal(root.get("accountingPeriod"), period);
                return cb.and(keywordMatch, periodMatch);
            }

            return keywordMatch;
        };

        return orderRepository.findAll(spec, PageRequest.of(0, limit))
                .map(order -> GlobalSearchItemDto.builder()
                        .type("訂單")
                        .id(order.getId())
                        .title(order.getOrderNo())
                        .subtitle(order.getCustomer() != null ? order.getCustomer().getName() : "無客戶")
                        .status(order.getOrderStatus() != null ? order.getOrderStatus().name() : "")
                        .route("/orders/" + order.getId())
                        .build())
                .getContent();
    }

    // --------------------------------------------------
    // 🔍 進貨搜尋：(OR 關鍵字) AND (會計期間)
    // --------------------------------------------------
    private List<GlobalSearchItemDto> fetchPurchases(String keyword, String period, int limit) {
        String pattern = "%" + keyword.toLowerCase() + "%";

        Specification<Purchase> spec = (root, query, cb) -> {
            query.distinct(true);

            // 關鍵字部分 (OR)
            Predicate keywordMatch = cb.or(
                    cb.like(cb.lower(root.get("purchaseNo")), pattern),
                    cb.like(cb.lower(root.get("supplier").get("name")), pattern),
                    cb.like(cb.lower(root.join("items").get("item")), pattern)
            );

            // 如果有提供月份，則加上 AND 條件
            if (StringUtils.hasText(period)) {
                Predicate periodMatch = cb.equal(root.get("accountingPeriod"), period);
                return cb.and(keywordMatch, periodMatch);
            }

            return keywordMatch;
        };

        return purchaseRepository.findAll(spec, PageRequest.of(0, limit))
                .map(p -> GlobalSearchItemDto.builder()
                        .type("進貨")
                        .id(p.getId())
                        .title(p.getPurchaseNo())
                        .subtitle(p.getSupplier() != null ? p.getSupplier().getName() : "")
                        .status(p.getStatus() != null ? p.getStatus().name() : "")
                        .route("/purchases/" + p.getId())
                        .build())
                .getContent();
    }

    // --------------------------------------------------
    // 🔍 客戶搜尋：維持純關鍵字 OR 邏輯 (主檔不分月份)
    // --------------------------------------------------
    private List<GlobalSearchItemDto> fetchCustomers(String keyword, int limit) {
        String pattern = "%" + keyword.toLowerCase() + "%";

        Specification<OrderCustomer> spec = (root, query, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("phone")), pattern),
                        cb.like(cb.lower(root.get("contactPerson")), pattern)
                );

        return orderCustomerRepository.findAll(spec, PageRequest.of(0, limit))
                .map(c -> GlobalSearchItemDto.builder()
                        .type("客戶")
                        .id(c.getId())
                        .title(c.getName())
                        .subtitle("聯絡人: " + c.getContactPerson() + " / " + c.getPhone())
                        .route("/order_customers/" + c.getId())
                        .build())
                .getContent();
    }

    // --------------------------------------------------
    // 🔍 供應商搜尋：維持純關鍵字 OR 邏輯 (主檔不分月份)
    // --------------------------------------------------
    private List<GlobalSearchItemDto> fetchSuppliers(String keyword, int limit) {
        // 使用您抽離出的 SupplierSpecifications 確保邏輯一致
        return supplierRepository.findAll(
                        SupplierSpecifications.globalSearch(keyword),
                        PageRequest.of(0, limit)
                )
                .map(s -> GlobalSearchItemDto.builder()
                        .type("供應商")
                        .id(s.getId())
                        .title(s.getName()) // 顯示供應商名稱
                        .subtitle("聯絡人: " + s.getContact() + " / " + s.getPhone())
                        // ✅ 修正路由：必須加上 ID，否則點擊會跳轉到 Not Found
                        .route("/suppliers/" + s.getId())
                        .build())
                .getContent();
    }
}