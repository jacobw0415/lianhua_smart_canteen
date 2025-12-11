package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.ap.APAgingSummaryDto;
import com.lianhua.erp.dto.ap.APAgingPurchaseDetailDto;
import com.lianhua.erp.service.APAgingService;

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
public class APAgingController {

    private final APAgingService agingService;

    /**
     * ⭐ React-Admin List 用的標準分頁 API
     * 完全符合 dataProvider 解析格式：
     *   { "content": [...], "totalElements": 123 }
     */
    @PageableAsQueryParam
    @GetMapping
    public ResponseEntity<?> getAgingPaged(
            @ParameterObject Pageable pageable
    ) {
        Page<APAgingSummaryDto> page = agingService.getAgingSummary(pageable);

        // React-Admin dataProvider 會讀取：
        //   payload.content → data
        //   payload.totalElements → total
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
     * ⭐ 匯出 / 不分頁 Summary
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getAllSummary() {
        return ResponseEntity.ok(
                agingService.getAgingSummaryAll()
        );
    }

    /**
     * ⭐ AP Detail — 單一供應商未付款進貨明細
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
