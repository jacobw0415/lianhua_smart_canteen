package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.report.APSummaryReportDto;
import com.lianhua.erp.dto.report.APSummaryReportQueryDto;
import com.lianhua.erp.service.APSummaryReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports/ap_summary")
@RequiredArgsConstructor
@Tag(name = "應付帳款總表", description = "應付帳款總表報表 API - 依會計期間彙總應付、已付、未付")
public class APSummaryReportController {

    private final APSummaryReportService summaryService;

    /**
     * 📊 應付帳款總表
     *
     * 支援查詢方式：
     * 1. 多個月份：periods=2026-01,2026-02
     * 2. 單一月份：period=2026-01
     * 3. 截止日期：endDate=2026-01-31
     */
    @GetMapping
    @Operation(summary = "查詢應付帳款總表", description = """
        依會計期間彙總應付、已付、未付。

        查詢方式：
        - 多個月份：periods=2026-01,2026-02（優先）
        - 單一月份：period=2026-01
        - 截止日期：endDate=2026-01-31

        回傳內容：
        - accountingPeriod：期間
        - totalPayable：應付總額
        - totalPaid：已付金額
        - totalOutstanding：未付餘額 (負債)
        """)
    public ResponseEntity<List<APSummaryReportDto>> getSummary(
            @ParameterObject APSummaryReportQueryDto query,
            @RequestParam(required = false) String periods) {

        // 支援逗號分隔 periods (補足 Spring 預設綁定可能漏掉的情況)
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
        List<APSummaryReportDto> result;

        // 邏輯路由：多月份 -> 單一月份(列表) -> 單一月份/截止日(欄位)
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