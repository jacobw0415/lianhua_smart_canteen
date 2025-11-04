package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * 月損益報表 DTO
 * 區分零售銷售（Sales）與批發訂單（Orders）收入，
 * 並統計採購、費用與淨利。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "月損益報表回應 DTO（含零售與訂單收入）")
public class ProfitReportDto {

    @Schema(description = "會計期間（YYYY-MM）", example = "2025-10")
    private String accountingPeriod;

    @Schema(description = "零售銷售金額（Sales 表）", example = "45800.00")
    private BigDecimal totalSales;

    @Schema(description = "訂單銷售金額（Orders 表）", example = "78200.00")
    private BigDecimal totalOrders;

    @Schema(description = "總收入（totalSales + totalOrders）", example = "124000.00")
    private BigDecimal totalRevenue;

    @Schema(description = "總採購成本", example = "73500.00")
    private BigDecimal totalPurchase;

    @Schema(description = "總營業費用", example = "18400.00")
    private BigDecimal totalExpense;

    @Schema(description = "本期淨利（totalRevenue - totalPurchase - totalExpense）", example = "32100.00")
    private BigDecimal netProfit;
}
