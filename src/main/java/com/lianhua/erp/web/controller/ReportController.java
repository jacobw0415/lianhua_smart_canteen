package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.report.ProfitReportDto;
import com.lianhua.erp.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 報表控制器
 * 提供 REST API 讓前端或 BI 系統查詢報表。
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "報表模組", description = "提供損益與營運統計報表 API")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "月損益報表", description = "依會計期間彙總銷售、採購、費用及淨利")
    @GetMapping("/monthly-profit")
    public ResponseEntity<List<ProfitReportDto>> getMonthlyProfitReport() {
        return ResponseEntity.ok(reportService.getMonthlyProfitReport());
    }
}
