package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.report.ARSummaryReportDto;
import com.lianhua.erp.dto.report.ARSummaryReportQueryDto;
import com.lianhua.erp.service.ARSummaryReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports/ar_summary")
@RequiredArgsConstructor
@Tag(name = "應收帳款總表", description = "應收帳款總表報表 API - 依會計期間彙總應收、已收、未收")
@PreAuthorize("hasAuthority('ar:view')")
public class ARSummaryReportController {

    private final ARSummaryReportService summaryService;

    /**
     * 📊 應收帳款總表
     *
     * 支援查詢方式：
     * 1. 多個月份：periods=2025-10,2025-11
     * 2. 單一月份：period=2025-10
     * 3. 截止日期：endDate=2025-12-31
     */
    @GetMapping
    @Operation(summary = "查詢應收帳款總表", description = """
        依會計期間彙總應收、已收、未收。

        查詢方式：
        - 多個月份：periods=2025-10,2025-11（優先）
        - 單一月份：period=2025-10
        - 截止日期：endDate=2025-12-31

        回傳內容：
        - accountingPeriod：期間
        - totalReceivable：應收總額
        - totalReceived：已收金額
        - totalOutstanding：未收餘額
        """)
    public ResponseEntity<List<ARSummaryReportDto>> getSummary(
            @ParameterObject ARSummaryReportQueryDto query,
            @RequestParam(required = false) String periods) {

        // 支援逗號分隔 periods
        if (periods != null && !periods.isBlank()
                && (query.getPeriods() == null || query.getPeriods().isEmpty())) {
            List<String> parsed = List.of(periods.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
            query.setPeriods(parsed);
        }

        List<String> periodsList = query.getPeriodsList();
        List<ARSummaryReportDto> result;

        if (periodsList != null && periodsList.size() > 1) {
            result = summaryService.generateSummary(periodsList);
        } else if (periodsList != null && periodsList.size() == 1) {
            result = summaryService.generateSummary(periodsList.get(0), query.getEndDate());
        } else {
            result = summaryService.generateSummary(query.getPeriod(), query.getEndDate());
        }

        return ResponseEntity.ok(result);
    }
}
