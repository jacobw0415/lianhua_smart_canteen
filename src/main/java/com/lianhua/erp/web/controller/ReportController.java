package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.InternalServerErrorResponse;
import com.lianhua.erp.dto.error.NotFoundResponse;
import com.lianhua.erp.dto.report.*;
import com.lianhua.erp.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 📊 報表控制器
 * 提供損益、現金流量與帳齡統計報表 API
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "報表模組", description = "提供損益、現金流量、應收與應付帳齡報表查詢 API")
public class ReportController {

    private final ReportService reportService;
    private final CashFlowReportService cashFlowReportservice;
    private final ARAgingReportService arAgingReportservice;
    private final APAgingReportService apAgingReportService;
    private final BalanceSheetReportService balanceSheetReportService;

    // ------------------------------------------------------
    //  月損益報表
    // ------------------------------------------------------
    @Operation(
            summary = "月損益報表",
            description = "依會計期間或日期區間彙總銷售、採購、費用及淨利。若未提供任何參數則查詢全部。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProfitReportDto.class)))),
            @ApiResponse(responseCode = "204", description = "查無資料"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping("/monthly-profit")
    public ResponseEntity<ApiResponseDto<List<ProfitReportDto>>> getMonthlyProfitReport(
            @Parameter(description = "會計期間（YYYY-MM，例如：2025-10）")
            @RequestParam(required = false) String period,

            @Parameter(description = "起始日期 (yyyy-MM-dd)")
            @RequestParam(required = false) String startDate,

            @Parameter(description = "結束日期 (yyyy-MM-dd)")
            @RequestParam(required = false) String endDate
    ) {
        List<ProfitReportDto> list = reportService.getMonthlyProfitReport(period, startDate, endDate);
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "查無損益報表資料"));
        }
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }

    // ------------------------------------------------------
    //  現金流量報表
    // ------------------------------------------------------
    @Operation(
            summary = "現金流量報表",
            description = "依日期區間、付款方式或會計期間統計現金流入與流出。未輸入條件則查詢全部。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CashFlowReportDto.class)))),
            @ApiResponse(responseCode = "204", description = "查無資料"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping("/monthly-cashflow")
    public ResponseEntity<ApiResponseDto<List<CashFlowReportDto>>> getMonthlyCashFlowReport(
            @Parameter(description = "起始日期 (yyyy-MM-dd)")
            @RequestParam(required = false) String startDate,

            @Parameter(description = "結束日期 (yyyy-MM-dd)")
            @RequestParam(required = false) String endDate,

            @Parameter(description = "付款方式 (CASH, TRANSFER, CARD, CHECK)")
            @RequestParam(required = false) String method,

            @Parameter(description = "會計期間 (YYYY-MM)")
            @RequestParam(required = false) String period
    ) {
        List<CashFlowReportDto> list = cashFlowReportservice.getCashFlowReport(startDate, endDate, method, period);
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "查無現金流量報表資料"));
        }
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }

    // ------------------------------------------------------
    //  應收帳齡報表
    // ------------------------------------------------------
    @Operation(
            summary = "應收帳齡報表",
            description = "統計客戶的未收款與逾期天數。若未提供條件則查詢全部客戶。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ARAgingReportDto.class)))),
            @ApiResponse(responseCode = "204", description = "查無資料"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping("/ar-aging")
    public ResponseEntity<ApiResponseDto<List<ARAgingReportDto>>> getArAgingReport(
            @Parameter(description = "客戶 ID（可選）")
            @RequestParam(required = false) Long customerId,

            @Parameter(description = "最小逾期天數（可選）")
            @RequestParam(required = false) Integer minOverdue,

            @Parameter(description = "會計期間（YYYY-MM）")
            @RequestParam(required = false) String period
    ) {
        List<ARAgingReportDto> list = arAgingReportservice.getAgingReceivables(customerId, minOverdue, period);
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "查無應收帳齡資料"));
        }
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }

    // ------------------------------------------------------
    //  應付帳齡報表
    // ------------------------------------------------------
    @Operation(
            summary = "應付帳齡報表",
            description = "統計供應商的未付款與逾期天數。若未提供條件則查詢全部供應商。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = APAgingReportDto.class)))),
            @ApiResponse(responseCode = "204", description = "查無資料"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping("/ap-aging")
    public ResponseEntity<ApiResponseDto<List<APAgingReportDto>>> getApAgingReport(
            @Parameter(description = "供應商 ID（可選）")
            @RequestParam(required = false) Long supplierId,

            @Parameter(description = "最小逾期天數（可選）")
            @RequestParam(required = false) Integer minOverdue,

            @Parameter(description = "會計期間（YYYY-MM）")
            @RequestParam(required = false) String period
    ) {
        List<APAgingReportDto> list = apAgingReportService.getAgingPayables(supplierId, minOverdue, period);
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "查無應付帳齡資料"));
        }
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }

    // ------------------------------------------------------
//  資產負債報表
// ------------------------------------------------------
    @Operation(
            summary = "資產負債表",
            description = "可依會計期間或日期區間統計公司截至期末的資產、負債與權益狀況"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BalanceSheetReportDto.class)))),
            @ApiResponse(responseCode = "204", description = "查無資料"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping("/balance-sheet")
    public ResponseEntity<ApiResponseDto<List<BalanceSheetReportDto>>> getBalanceSheetReport(
            @Parameter(description = "起始日期 (yyyy-MM-dd，可選)")
            @RequestParam(required = false) String startDate,

            @Parameter(description = "結束日期 (yyyy-MM-dd，可選)")
            @RequestParam(required = false) String endDate,

            @Parameter(description = "會計期間（YYYY-MM，可選）")
            @RequestParam(required = false) String period
    ) {
        List<BalanceSheetReportDto> list = balanceSheetReportService.generateBalanceSheet(period, startDate, endDate);
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponseDto.error(HttpStatus.NO_CONTENT.value(), "查無資產負債表資料"));
        }
        return ResponseEntity.ok(ApiResponseDto.ok(list));
    }

}
