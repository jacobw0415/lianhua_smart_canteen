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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    public ResponseEntity<ApiResponseDto<DashboardStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getDashboardStats()));
    }

    @GetMapping("/trends")
    @Operation(summary = "獲取營運趨勢圖數據", description = "取得零售營收與訂單收款對比趨勢。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得趨勢數據", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TrendPointDto.class)))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤", content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })

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

    public ResponseEntity<ApiResponseDto<List<ExpenseCompositionDto>>> getExpenseComposition() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getExpenseComposition()));
    }

    @GetMapping("/tasks")
    @Operation(summary = "獲取儀表板待辦與預警任務", description = "獲取即期應收帳款 (7D) 與未結案訂單明細。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得清單", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DashboardTaskDto.class)))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })

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

    public ResponseEntity<ApiResponseDto<List<AccountAgingDto>>> getAgingAnalytics() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getAgingAnalytics()));
    }

    @GetMapping("/analytics/profit-loss-trend")
    @Operation(summary = "獲取損益四線走勢", description = "跨期間對比營收、毛利、費用與淨利變化趨勢。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得損益趨勢", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProfitLossPointDto.class)))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })

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

    public ResponseEntity<ApiResponseDto<List<OrderFunnelDto>>> getOrderFunnel(
            @Parameter(description = "會計期間 (YYYY-MM)", example = "2026-01") @RequestParam(required = false) String period
    ) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getOrderFunnel(period)));
    }

    /* =========================================================
     * 6. 深度財務與獲利決策
     * ========================================================= */

    @GetMapping("/analytics/break-even")
    @Operation(summary = "獲取損益平衡分析", description = "分析當月累計營收何時超越固定成本門檻。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得數據", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BreakEvenPointDto.class)))),
            @ApiResponse(responseCode = "400", description = "會計期間格式錯誤", content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "500", description = "系統錯誤", content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })

    public ResponseEntity<ApiResponseDto<List<BreakEvenPointDto>>> getBreakEven(
            @Parameter(description = "會計期間 (YYYY-MM)", example = "2026-01") @RequestParam String period
    ) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getBreakEvenAnalysis(period)));
    }

    @GetMapping("/analytics/liquidity")
    @Operation(summary = "獲取流動性指標", description = "包含流動比率、速動比率等財務健康度。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得財務指標", content = @Content(schema = @Schema(implementation = LiquidityDto.class)))
    })

    public ResponseEntity<ApiResponseDto<LiquidityDto>> getLiquidity() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getLiquidityAnalytics()));
    }

        @GetMapping("/analytics/cashflow-forecast")
        @Operation(summary = "獲取未來 30 天現金流預測", description = "結合應收應付到期日預估資金水位。")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "成功取得預測數據", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CashflowForecastDto.class))))
        })
        public ResponseEntity<ApiResponseDto<List<CashflowForecastDto>>> getCashflowForecast(
                @Parameter(description = "基準日 (YYYY-MM-DD)")
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
                @Parameter(description = "天數") @RequestParam(defaultValue = "30") int days
        ) {
            LocalDate effectiveDate = (baseDate != null) ? baseDate : LocalDate.now();
            return ResponseEntity.ok(ApiResponseDto.ok(service.getCashflowForecast(effectiveDate, days)));
        }

    @GetMapping("/analytics/product-pareto")
    @Operation(summary = "獲取商品獲利 Pareto 分析", description = "識別貢獻公司 80% 獲利的品項。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductParetoDto.class)))),
            @ApiResponse(responseCode = "400", description = "日期格式錯誤")
    })

    public ResponseEntity<ApiResponseDto<List<ProductParetoDto>>> getProductPareto(
            @Parameter(description = "開始日期", example = "2026-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "結束日期", example = "2026-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getProductParetoAnalysis(start, end)));
    }

    @GetMapping("/analytics/supplier-concentration")
    @Operation(summary = "獲取供應商採購集中度", description = "評估採購額佔比與風險。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SupplierConcentrationDto.class))))
    })

    public ResponseEntity<ApiResponseDto<List<SupplierConcentrationDto>>> getSupplierConcentration(
            @Parameter(description = "開始日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "結束日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getSupplierConcentration(start, end)));
    }

    @GetMapping("/analytics/customer-retention")
    @Operation(summary = "獲取客戶回購與沉睡分析", description = "監控批發客戶流失風險。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CustomerRetentionDto.class))))
    })

    public ResponseEntity<ApiResponseDto<List<CustomerRetentionDto>>> getCustomerRetention() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getCustomerRetention()));
    }

    /**
     * [圖表 7] 獲取採購結構分析 (依進貨項目)
     * 識別特定期間內各品項的採購金額分布與佔比。
     */
    @GetMapping("/analytics/purchase-structure")
    @Operation(summary = "獲取採購結構分析", description = "依據進貨項目 (item) 聚合採購金額，排除已作廢單據。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得採購結構數據",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PurchaseStructureDto.class)))),
            @ApiResponse(responseCode = "400", description = "請求日期格式錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })

    public ResponseEntity<ApiResponseDto<List<PurchaseStructureDto>>> getPurchaseStructure(
            @Parameter(description = "開始日期 (YYYY-MM-DD)", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "結束日期 (YYYY-MM-DD)", example = "2026-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        // 調用 Service 層獲取聚合後的採購品項數據
        return ResponseEntity.ok(ApiResponseDto.ok(service.getPurchaseStructureByItem(start, end)));
    }

    /**
     * [圖表 9] 獲取客戶採購集中度分析 (Customer Concentration)
     * 分析指定期間內各 B2B 客戶的訂單總額及其佔全體營收的比例，用於識別關鍵客戶風險。
     */
    @GetMapping("/analytics/customer-concentration")
    @Operation(summary = "獲取客戶採購集中度分析", description = "計算各客戶在指定期間內的訂單總額及其佔全體營收的佔比，排除已作廢單據。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得客戶集中度數據",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CustomerConcentrationDto.class)))),
            @ApiResponse(responseCode = "400", description = "請求日期格式錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })

    public ResponseEntity<ApiResponseDto<List<CustomerConcentrationDto>>> getCustomerConcentration(
            @Parameter(description = "開始日期 (YYYY-MM-DD)", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "結束日期 (YYYY-MM-DD)", example = "2026-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        // 調用 Service 實作，處理客戶營收貢獻度之聚合分析
        return ResponseEntity.ok(ApiResponseDto.ok(service.getCustomerConcentration(start, end)));
    }

}