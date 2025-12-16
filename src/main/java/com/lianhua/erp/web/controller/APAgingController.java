package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.ap.APAgingFilterDto;
import com.lianhua.erp.dto.ap.APAgingSummaryDto;
import com.lianhua.erp.dto.ap.APAgingPurchaseDetailDto;
import com.lianhua.erp.service.APAgingService;

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
@RequestMapping("/api/ap")
@RequiredArgsConstructor
@Tag(name = "應付帳款紀錄", description = "應付帳款 API")
public class APAgingController {

    private final APAgingService agingService;

    /**
     * ⭐ AP Aging Summary（分頁 + 搜尋）
     * - React-Admin List 使用
     * - 支援供應商名稱 / 帳齡區間 / 是否未付款
     */
    @PageableAsQueryParam
    @GetMapping
    public ResponseEntity<?> getAgingPaged(
            @ParameterObject APAgingFilterDto filter,
            @ParameterObject Pageable pageable
    ) {
        Page<APAgingSummaryDto> page =
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
     * ⭐ AP Aging Summary（不分頁）
     * - 匯出 / 報表專用
     */
    @GetMapping("/summary")
    public ResponseEntity<List<APAgingSummaryDto>> getAllSummary() {
        return ResponseEntity.ok(
                agingService.getAgingSummaryAll()
        );
    }

    /**
     * ⭐ AP Detail
     * - 單一供應商逐筆未付款進貨明細
     */
    @GetMapping("/{supplierId}/purchases")
    public ResponseEntity<List<APAgingPurchaseDetailDto>> getSupplierPurchases(
            @PathVariable Long supplierId
    ) {
        return ResponseEntity.ok(
                agingService.getSupplierPurchases(supplierId)
        );
    }
}