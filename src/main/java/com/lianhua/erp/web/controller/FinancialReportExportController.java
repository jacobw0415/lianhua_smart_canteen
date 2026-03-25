package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.report.FinancialReportKey;
import com.lianhua.erp.dto.report.ReportExportQueryDto;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import com.lianhua.erp.security.ReportExportAuthorization;
import com.lianhua.erp.service.FinancialReportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/**
 * 五大財務報表統一匯出（與各報表 GET 使用相同查詢條件語意）。
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "財務報表匯出", description = "GET /api/reports/{reportKey}/export — xlsx / csv")
public class FinancialReportExportController {

    private final FinancialReportExportService financialReportExportService;
    private final ReportExportAuthorization reportExportAuthorization;

    @Operation(
            summary = "匯出財務報表",
            description = """
                    reportKey：balance_sheet | comprehensive_income_statement | cash_flow_reports | ar_summary | ap_summary

                    查詢參數與對應報表 GET 相同（period、periods、endDate、startDate 等）。
                    可額外傳逗號分隔 periods=2025-01,2025-02（與現有報表 Controller 行為一致）。

                    - format：xlsx（預設）或 csv
                    - scope：all（預設，匯出完整查詢結果）或 page（依 page/size 切片；0-based）
                    - sort：僅支援 accountingPeriod,asc|desc
                    - columns：選填，逗號分隔欄位鍵，順序即輸出欄序

                    權限：多數報表需 report:view；ar_summary 需 ar:view；ap_summary 需 ap:view。
                    """
    )
    @PageableAsQueryParam
    @GetMapping(value = "/{reportKey}/export", produces = {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv; charset=UTF-8"
    })
    public ResponseEntity<byte[]> export(
            @PathVariable String reportKey,
            @ParameterObject ReportExportQueryDto query,
            @RequestParam(required = false) String periods,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String columns,
            @ParameterObject Pageable pageable
    ) {
        FinancialReportKey key = FinancialReportKey.fromPath(reportKey);
        reportExportAuthorization.requirePermission(key);

        String resolvedScope = (scope == null || scope.isBlank()) ? "all" : scope;
        ExportPayload payload = financialReportExportService.export(
                key,
                query,
                periods,
                ExportFormat.fromQueryParam(format),
                ExportScope.fromQueryParam(resolvedScope),
                pageable,
                columns
        );

        ContentDisposition disposition = ContentDisposition.builder("attachment")
                .filename(payload.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(payload.mediaType()))
                .body(payload.data());
    }
}
