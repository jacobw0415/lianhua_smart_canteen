package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * 現金流量報表 DTO
 * 統計每月實際現金流入與流出。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "月現金流量報表回應 DTO（含 sales、receipts、payments、expenses）")
public class CashFlowReportDto {

    @Schema(description = "會計期間（YYYY-MM）", example = "2025-10")
    private String accountingPeriod;

    @Schema(description = "零售現金收入 (Sales)")
    private BigDecimal totalSales;

    @Schema(description = "訂單收款收入 (Receipts)")
    private BigDecimal totalReceipts;

    @Schema(description = "採購付款支出 (Payments)")
    private BigDecimal totalPayments;

    @Schema(description = "營運費用支出 (Expenses)")
    private BigDecimal totalExpenses;

    @Schema(description = "總流入金額 (Sales + Receipts)")
    private BigDecimal totalInflow;

    @Schema(description = "總流出金額 (Payments + Expenses)")
    private BigDecimal totalOutflow;

    @Schema(description = "本期淨現金流 (Inflow - Outflow)")
    private BigDecimal netCashFlow;
}
