package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.dashboard.*;
import com.lianhua.erp.dto.dashboard.analytics.*;
import com.lianhua.erp.dto.error.BadRequestResponse;
import com.lianhua.erp.dto.error.InternalServerErrorResponse;
import com.lianhua.erp.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "營運儀表板", description = "Dashboard Management API")
public class DashboardController {

    private final DashboardService service;

    /* =========================================================
     * 1~4. 基礎監控 API
     * ========================================================= */

    @GetMapping("/stats")
    @Operation(summary = "獲取核心 KPI 統計", description = "整合銷售、採購、費用、財務與現金流量指標。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得數據", content = @Content(schema = @Schema(implementation = DashboardStatsDto.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto<DashboardStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getDashboardStats()));
    }

    @GetMapping("/trends")
    @Operation(summary = "獲取營運趨勢圖數據", description = "取得零售營收與訂單收款對比趨勢。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得趨勢數據", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TrendPointDto.class)))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤", content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto<List<TrendPointDto>>> getTrends(
            @Parameter(description = "回溯天數", example = "30") @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getSalesTrendData(days)));
    }

    @GetMapping("/expenses/composition")
    @Operation(summary = "獲取本月支出結構比例", description = "用於前端圓餅圖分析成本佔比。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得支出結構數據", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ExpenseCompositionDto.class)))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto<List<ExpenseCompositionDto>>> getExpenseComposition() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getExpenseComposition()));
    }

    @GetMapping("/tasks")
    @Operation(summary = "獲取儀表板待辦與預警任務", description = "獲取即期應收帳款 (7D) 與未結案訂單明細。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得清單", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DashboardTaskDto.class)))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto<List<DashboardTaskDto>>> getTasks() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getPendingTasks()));
    }

    /* =========================================================
     * 5. 進階分析 API (v2.0 決策支援系列)
     * ========================================================= */


    @GetMapping("/analytics/accounts-aging")
    @Operation(summary = "獲取帳款帳齡分析", description = "分析 AR/AP 逾期風險分佈，用於信用控管。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得帳齡數據", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AccountAgingDto.class)))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponseDto<List<AccountAgingDto>>> getAgingAnalytics() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getAgingAnalytics()));
    }

    @GetMapping("/analytics/profit-loss-trend")
    @Operation(summary = "獲取損益四線走勢", description = "跨期間對比營收、毛利、費用與淨利變化趨勢。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得損益趨勢", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProfitLossPointDto.class)))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponseDto<List<ProfitLossPointDto>>> getProfitLossTrend(
            @Parameter(description = "回溯月數", example = "6") @RequestParam(defaultValue = "6") int months
    ) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getProfitLossTrend(months)));
    }

    @GetMapping("/analytics/order-funnel")
    @Operation(summary = "獲取訂單履約漏斗", description = "監控各階段訂單轉化效率與涉及金額。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得漏斗數據", content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderFunnelDto.class)))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto<List<OrderFunnelDto>>> getOrderFunnel(
            @Parameter(description = "會計期間 (YYYY-MM)", example = "2026-01") @RequestParam(required = false) String period
    ) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getOrderFunnel(period)));
    }
}