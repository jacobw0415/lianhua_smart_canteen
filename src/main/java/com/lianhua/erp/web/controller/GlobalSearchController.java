package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.globalSearch.GlobalSearchRequest;
import com.lianhua.erp.dto.globalSearch.GlobalSearchResponse;
import com.lianhua.erp.service.GlobalSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/global_search")
@RequiredArgsConstructor
@Tag(name = "Global Search", description = "ERP 全域搜尋服務 - 支援訂單、進貨、客戶跨模組查詢")
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping
    @PreAuthorize("hasAuthority('dashboard:view')")
    @Operation(summary = "全域搜尋", description = "輸入關鍵字即可在訂單編號、進貨單號、客戶名稱等欄位中進行模糊搜尋，並支援月份過濾")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "搜尋成功", content = @Content(schema = @Schema(implementation = GlobalSearchResponse.class))),
            @ApiResponse(responseCode = "401", description = "未授權", content = @Content),
            @ApiResponse(responseCode = "403", description = "權限不足", content = @Content),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content)
    })
    public GlobalSearchResponse search(
            @Parameter(description = "搜尋關鍵字", example = "永進", required = true)
            @RequestParam("keyword") String keyword,

            // ✅ 1. 新增接收 period 參數 (對應前端的會計期間)
            @Parameter(description = "會計期間 (月份過濾)，格式: YYYY-MM", example = "2026-01")
            @RequestParam(value = "period", required = false) String period,

            @Parameter(description = "搜尋範圍 (可選)，多個以逗號隔開", example = "orders,purchases")
            @RequestParam(value = "scopes", required = false) List<String> rawScopes,

            @Parameter(description = "回傳結果筆數限制 (預設 5)", example = "10")
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        log.info("GlobalSearch request: keyword={}, period={}, rawScopes={}", keyword, period, rawScopes);

        List<String> scopes = normalizeScopes(rawScopes);

        // ✅ 2. 將接收到的 period 設定進 Request DTO
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setKeyword(keyword);
        request.setPeriod(period); // 👈 關鍵：傳遞給 Service
        request.setScopes(scopes);
        request.setLimit(limit);

        return globalSearchService.search(request);
    }

    private List<String> normalizeScopes(List<String> rawScopes) {
        if (rawScopes == null || rawScopes.isEmpty()) {
            return Arrays.asList("orders", "purchases", "customers", "suppliers");
        }
        return rawScopes.stream()
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}