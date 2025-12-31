package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.ar.ARAgingFilterDto;
import com.lianhua.erp.dto.ar.ARAgingOrderDetailDto;
import com.lianhua.erp.dto.ar.ARAgingSummaryDto;
import com.lianhua.erp.service.ARAgingService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ar")
@RequiredArgsConstructor
@Tag(name = "應收帳款紀錄", description = "應收帳款 API")
public class ARAgingController {

    private final ARAgingService agingService;

    /**
     * ⭐ AR Aging Summary（分頁 + 搜尋）
     * - React-Admin List 使用
     * - 支援客戶名稱 / 帳齡區間 / 是否未收款
     */
    @PageableAsQueryParam
    @GetMapping
    public ResponseEntity<?> getAgingPaged(
            @ParameterObject ARAgingFilterDto filter,
            @ParameterObject Pageable pageable
    ) {
        Page<ARAgingSummaryDto> page =
                agingService.getAgingSummary(filter, pageable);

        // React-Admin dataProvider 解析：
        // - content → data
        // - totalElements → total
        return ResponseEntity.ok(
                Map.of(
                        "content", page.getContent(),
                        "totalElements", page.getTotalElements(),
                        "size", page.getSize(),
                        "number", page.getNumber(),
                        "totalPages", page.getTotalPages()
                )
        );
    }

    /**
     * ⭐ AR Aging Summary（不分頁）
     * - 匯出 / 報表專用
     */
    @GetMapping("/summary")
    public ResponseEntity<List<ARAgingSummaryDto>> getAllSummary() {
        return ResponseEntity.ok(
                agingService.getAgingSummaryAll()
        );
    }

    /**
     * ⭐ AR Detail
     * - 單一客戶逐筆未收款訂單明細
     */
    @GetMapping("/{customerId}/orders")
    public ResponseEntity<List<ARAgingOrderDetailDto>> getCustomerOrders(
            @PathVariable Long customerId
    ) {
        return ResponseEntity.ok(
                agingService.getCustomerOrders(customerId)
        );
    }
}

