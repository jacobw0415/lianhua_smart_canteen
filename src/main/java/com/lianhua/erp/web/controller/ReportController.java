package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.report.*;
import com.lianhua.erp.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 📊 報表控制器
 * 統一由 GlobalExceptionHandler 處理錯誤（不再使用 try-catch）
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "報表模組", description = "提供損益、現金流量、應收與應付帳齡、資產負債報表 API")
public class ReportController {

    private final ReportService reportService;
    private final CashFlowReportService cashFlowReportService;
    private final ARAgingReportService arAgingReportService;
    private final APAgingReportService apAgingReportService;
    private final BalanceSheetReportService balanceSheetReportService;

    // ============================================================
    // ✅ 共用日期驗證工具（若格式錯誤將丟出 IllegalArgumentException）
    // ============================================================
    private void validateDateRange(String startDate, String endDate) {
        if (startDate != null && endDate != null) {
            try {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                if (start.isAfter(end)) {
                    throw new IllegalArgumentException("起始日期不可晚於結束日期");
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("日期格式錯誤，請使用 yyyy-MM-dd 格式");
            }
        }
    }

    // ============================================================
    // 📘 月損益報表
    // ============================================================
    @Operation(
            summary = "月損益報表",
            description = "依會計期間或日期區間彙總銷售、採購、費用及淨利。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProfitReportDto.class)))),
            @ApiResponse(responseCode = "400", description = "輸入格式錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "查無資料",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping("/monthly-profit")
    public ResponseEntity<ApiResponseDto<List<ProfitReportDto>>> getMonthlyProfitReport(
            @Parameter(description = "會計期間（YYYY-MM）") @RequestParam(required = false) String period,
            @Parameter(description = "起始日期 (yyyy-MM-dd)") @RequestParam(required = false) String startDate,
            @Parameter(description = "結束日期 (yyyy-MM-dd)") @RequestParam(required = false) String endDate
    ) {
        validateDateRange(startDate, endDate);

        List<ProfitReportDto> list = reportService.getMonthlyProfitReport(period, startDate, endDate);
        if (list.isEmpty()) throw new EntityNotFoundException("查無損益報表資料");

        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }

    // ============================================================
    // 💰 現金流量報表
    // ============================================================
    @Operation(
            summary = "現金流量報表",
            description = "依日期區間或會計期間統計現金流入與流出。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CashFlowReportDto.class)))),
            @ApiResponse(responseCode = "400", description = "輸入格式錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "查無資料",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "業務邏輯錯誤",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping("/monthly-cashflow")
    public ResponseEntity<ApiResponseDto<List<CashFlowReportDto>>> getMonthlyCashFlowReport(
            @Parameter(description = "起始日期 (yyyy-MM-dd)") @RequestParam(required = false) String startDate,
            @Parameter(description = "結束日期 (yyyy-MM-dd)") @RequestParam(required = false) String endDate,
            @Parameter(description = "會計期間 (YYYY-MM)") @RequestParam(required = false) String period
    ) {
        validateDateRange(startDate, endDate);

        List<CashFlowReportDto> list = cashFlowReportService.generateCashFlow(period, startDate, endDate);
        if (list == null || list.isEmpty()) throw new EntityNotFoundException("查無現金流量報表資料");

        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }

    // ============================================================
    // 🧾 應收帳齡報表
    // ============================================================
    @Operation(
            summary = "應收帳齡報表",
            description = "統計客戶未收款與逾期天數。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功"),
            @ApiResponse(responseCode = "400", description = "輸入格式錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "查無資料",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping("/ar-aging")
    public ResponseEntity<ApiResponseDto<List<ARAgingReportDto>>> getArAgingReport(
            @Parameter(description = "客戶 ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "最小逾期天數") @RequestParam(required = false) Integer minOverdue,
            @Parameter(description = "會計期間（YYYY-MM）") @RequestParam(required = false) String period
    ) {
        if (minOverdue != null && minOverdue < 0)
            throw new IllegalArgumentException("逾期天數不得為負數");

        List<ARAgingReportDto> list = arAgingReportService.getAgingReceivables(customerId, minOverdue, period);
        if (list.isEmpty()) throw new EntityNotFoundException("查無應收帳齡資料");

        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }

    // ============================================================
    // 🧾 應付帳齡報表
    // ============================================================
    @Operation(
            summary = "應付帳齡報表",
            description = "統計供應商未付款與逾期天數。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功"),
            @ApiResponse(responseCode = "400", description = "輸入格式錯誤"),
            @ApiResponse(responseCode = "404", description = "查無資料"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping("/ap-aging")
    public ResponseEntity<ApiResponseDto<List<APAgingReportDto>>> getApAgingReport(
            @Parameter(description = "供應商 ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "最小逾期天數") @RequestParam(required = false) Integer minOverdue,
            @Parameter(description = "會計期間（YYYY-MM）") @RequestParam(required = false) String period
    ) {
        if (minOverdue != null && minOverdue < 0)
            throw new IllegalArgumentException("逾期天數不得為負數");

        List<APAgingReportDto> list = apAgingReportService.getAgingPayables(supplierId, minOverdue, period);
        if (list.isEmpty()) throw new EntityNotFoundException("查無應付帳齡資料");

        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }

    // ============================================================
    // 🧮 資產負債表
    // ============================================================
    @Operation(
            summary = "資產負債表",
            description = "依會計期間或日期區間統計資產、負債與權益狀況。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功"),
            @ApiResponse(responseCode = "400", description = "輸入格式錯誤"),
            @ApiResponse(responseCode = "404", description = "查無資料"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping("/balance-sheet")
    public ResponseEntity<ApiResponseDto<List<BalanceSheetReportDto>>> getBalanceSheetReport(
            @Parameter(description = "起始日期 (yyyy-MM-dd)") @RequestParam(required = false) String startDate,
            @Parameter(description = "結束日期 (yyyy-MM-dd)") @RequestParam(required = false) String endDate,
            @Parameter(description = "會計期間（YYYY-MM）") @RequestParam(required = false) String period
    ) {
        validateDateRange(startDate, endDate);

        List<BalanceSheetReportDto> list = balanceSheetReportService.generateBalanceSheet(period, startDate, endDate);
        if (list.isEmpty()) throw new EntityNotFoundException("查無資產負債表資料");

        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }
}
